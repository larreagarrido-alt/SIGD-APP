package pe.net.ws.kpiweb.features.admin.personal;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonObject;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRemoteMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.personal.adapter.ActivityAdapter;
import pe.net.ws.kpiweb.features.admin.personal.adapter.KpiAdapter;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRepository;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeActivityItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeKpiItem;
import pe.net.ws.kpiweb.features.admin.personal.view.CircularProgressView;
import pe.net.ws.kpiweb.features.admin.personal.view.KpiBarChartView;
import pe.net.ws.kpiweb.features.admin.personal.view.PerformanceLineChartView;

/**
 * Pantalla de detalle individual del colaborador seleccionado.
 * Contiene tres pestañas: Resumen | Indicadores | Actividades.
 * Presenta datos del trabajador autenticado; KPI y actividades requieren integración propia.
 */
public class AdminEmployeeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EMPLOYEE_ID = "extra_employee_id";

    private PersonalRepository repository;
    private EmployeeItem employee;

    // Header del Perfil
    private TextView tvAvatar;
    private TextView tvName;
    private TextView tvRole;
    private TextView tvDeptCode;
    private TextView tvStatus;
    private TextView tvEmail;
    private TextView tvPhone;

    // Pestañas
    private TextView btnTabSummary;
    private TextView btnTabKpi;
    private TextView btnTabActivities;
    private LinearLayout layoutContentSummary;
    private LinearLayout layoutContentKpi;
    private LinearLayout layoutContentActivities;

    // Tab 1: Resumen
    private TextView tvHeroPeriod;
    private TextView tvHeroSubtitle;
    private TextView tvHeroStatus;
    private CircularProgressView cpvHeroScore;
    private TextView tvHeroEvaluations;
    private ProgressBar pbHeroEvaluations;
    private TextView tvStatAssigned;
    private TextView tvStatCompleted;
    private TextView tvStatPending;
    private PerformanceLineChartView chartPerformanceEvolution;
    private TextView tvLaborHireDate;
    private TextView tvLaborSchedule;
    private TextView tvLaborContract;
    private TextView tvLaborModality;

    // Tab 2: Indicadores
    private KpiAdapter kpiAdapter;
    private KpiBarChartView chartKpiHistory;
    private TextView chipKpiThisMonth;
    private TextView chipKpiThisWeek;
    private TextView chipKpiPrevMonth;
    private TextView chipKpiCustom;

    // Tab 3: Actividades
    private ActivityAdapter activityAdapter;
    private LinearLayout cardUpcoming;
    private TextView tvUpcomingTitle;
    private TextView tvUpcomingDeadline;
    private TextView tvActivitiesEmpty;
    private TextView chipActAll;
    private TextView chipActPending;
    private TextView chipActInProgress;
    private TextView chipActCompleted;
    private TextView chipActOverdue;
    private String currentActivityStatusFilter = "Todas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_employee_detail);

        repository = PersonalRepository.getInstance();
        String employeeId = getIntent().getStringExtra(EXTRA_EMPLOYEE_ID);

        if (employeeId != null) {
            employee = repository.getEmployeeById(employeeId);
        }

        if (employee == null) {
            Toast.makeText(this, "Colaborador no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupTopBar();
        bindEmployeeHeader();
        setupTabs();
        findViewById(R.id.personal_detail_server_reports_action).setOnClickListener(v -> {
            try {
                pe.net.ws.kpiweb.features.kpi.ui.KpiServerReportBrowser.show(this,
                        Integer.parseInt(employee.getId()));
            } catch (NumberFormatException ignored) { }
        });
        bindSummaryTab();
        bindKpiTab();
        bindActivitiesTab();
        AdminPersonalTypography.apply(findViewById(android.R.id.content));
        loadRealDetail(employeeId);
    }

    private void loadRealDetail(String employeeId) {
        String bearer = new SessionManager(this).getBearerToken();
        if (bearer.isEmpty()) return;
        int workerId;
        try { workerId = Integer.parseInt(employeeId); }
        catch (NumberFormatException error) { return; }
        ApiClient.getService().getPersonalDetail(bearer, workerId).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (isFinishing() || isDestroyed()) return;
                ApiResponse<JsonObject> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null
                        || !body.getData().has("empleado")) {
                    Toast.makeText(AdminEmployeeDetailActivity.this, "No se pudo consultar el detalle real", Toast.LENGTH_LONG).show();
                    return;
                }
                employee = PersonalRemoteMapper.employee(body.getData().getAsJsonObject("empleado"));
                bindEmployeeHeader();
                bindSummaryTab();
                bindKpiTab();
                bindActivitiesTab();
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!isFinishing() && !isDestroyed())
                    Toast.makeText(AdminEmployeeDetailActivity.this, "No hay conexión con el detalle de Personal", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initViews() {
        // Topbar
        ImageButton ibBack = findViewById(R.id.ib_detail_back);
        ibBack.setOnClickListener(v -> finish());

        // Header
        tvAvatar = findViewById(R.id.tv_detail_avatar);
        tvName = findViewById(R.id.tv_detail_name);
        tvRole = findViewById(R.id.tv_detail_role);
        tvDeptCode = findViewById(R.id.tv_detail_dept_code);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvEmail = findViewById(R.id.tv_detail_email);
        tvPhone = findViewById(R.id.tv_detail_phone);

        // Selector de Pestañas
        btnTabSummary = findViewById(R.id.btn_tab_summary);
        btnTabKpi = findViewById(R.id.btn_tab_kpi);
        btnTabActivities = findViewById(R.id.btn_tab_activities);
        layoutContentSummary = findViewById(R.id.layout_content_summary);
        layoutContentKpi = findViewById(R.id.layout_content_kpi);
        layoutContentActivities = findViewById(R.id.layout_content_activities);

        // Tab Resumen
        tvHeroPeriod = findViewById(R.id.tv_hero_period);
        tvHeroSubtitle = findViewById(R.id.tv_hero_subtitle);
        tvHeroStatus = findViewById(R.id.tv_hero_status);
        cpvHeroScore = findViewById(R.id.cpv_hero_score);
        tvHeroEvaluations = findViewById(R.id.tv_hero_evaluations);
        pbHeroEvaluations = findViewById(R.id.pb_hero_evaluations);
        tvStatAssigned = findViewById(R.id.tv_stat_assigned);
        tvStatCompleted = findViewById(R.id.tv_stat_completed);
        tvStatPending = findViewById(R.id.tv_stat_pending);
        chartPerformanceEvolution = findViewById(R.id.chart_performance_evolution);
        tvLaborHireDate = findViewById(R.id.tv_labor_hire_date);
        tvLaborSchedule = findViewById(R.id.tv_labor_schedule);
        tvLaborContract = findViewById(R.id.tv_labor_contract);
        tvLaborModality = findViewById(R.id.tv_labor_modality);

        // Tab Indicadores
        chartKpiHistory = findViewById(R.id.chart_kpi_bar_history);
        chipKpiThisMonth = findViewById(R.id.chip_kpi_this_month);
        chipKpiThisWeek = findViewById(R.id.chip_kpi_this_week);
        chipKpiPrevMonth = findViewById(R.id.chip_kpi_prev_month);
        chipKpiCustom = findViewById(R.id.chip_kpi_custom);

        // Tab Actividades
        cardUpcoming = findViewById(R.id.card_upcoming_activity);
        tvUpcomingTitle = findViewById(R.id.tv_upcoming_title);
        tvUpcomingDeadline = findViewById(R.id.tv_upcoming_deadline);
        tvActivitiesEmpty = findViewById(R.id.tv_activities_empty);
        chipActAll = findViewById(R.id.chip_act_all);
        chipActPending = findViewById(R.id.chip_act_pending);
        chipActInProgress = findViewById(R.id.chip_act_in_progress);
        chipActCompleted = findViewById(R.id.chip_act_completed);
        chipActOverdue = findViewById(R.id.chip_act_overdue);
    }

    private void setupTopBar() {
        // Nada adicional, ya configurado el click del back
    }

    private void bindEmployeeHeader() {
        tvName.setText(employee.getName());
        tvRole.setText(employee.getRole());
        tvDeptCode.setText(employee.getDepartment() + " • " + employee.getId());
        tvAvatar.setText(employee.getAvatarInitials());

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(employee.getAvatarColorHex());
        tvAvatar.setBackground(avatarBg);

        // Estado laboral
        EmployeeItem.WorkStatus ws = employee.getWorkStatus();
        tvStatus.setText(ws.getDisplayName());
        int textColor;
        int bgColor;
        switch (ws) {
            case ACTIVO:
                textColor = ContextCompat.getColor(this, R.color.personal_status_delivered);
                bgColor = ContextCompat.getColor(this, R.color.personal_status_delivered_bg);
                break;
            case VACACIONES:
                textColor = ContextCompat.getColor(this, R.color.personal_status_pending);
                bgColor = ContextCompat.getColor(this, R.color.personal_status_pending_bg);
                break;
            case CAPACITACION:
                textColor = ContextCompat.getColor(this, R.color.personal_cyan_brain);
                bgColor = ContextCompat.getColor(this, R.color.personal_cyan_brain_bg);
                break;
            case LICENCIA:
                textColor = ContextCompat.getColor(this, R.color.personal_status_leave);
                bgColor = ContextCompat.getColor(this, R.color.personal_status_leave_bg);
                break;
            case INACTIVO:
            case SIN_DATOS:
            default:
                textColor = ContextCompat.getColor(this, R.color.personal_text_secondary);
                bgColor = ContextCompat.getColor(this, R.color.personal_status_leave_bg);
                break;
        }
        tvStatus.setTextColor(textColor);
        tvStatus.setBackgroundTintList(ColorStateList.valueOf(bgColor));

        tvEmail.setText(employee.getEmail().isEmpty() ? "Correo no disponible" : employee.getEmail());
        tvPhone.setText(employee.getPhone().isEmpty() ? "Teléfono no disponible" : employee.getPhone());
    }

    private void setupTabs() {
        btnTabSummary.setOnClickListener(v -> selectTab(0));
        btnTabKpi.setOnClickListener(v -> selectTab(1));
        btnTabActivities.setOnClickListener(v -> selectTab(2));
        selectTab(0);
    }

    private void selectTab(int index) {
        // 0: Resumen, 1: Indicadores, 2: Actividades
        updateTabButton(btnTabSummary, index == 0);
        updateTabButton(btnTabKpi, index == 1);
        updateTabButton(btnTabActivities, index == 2);

        layoutContentSummary.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        layoutContentKpi.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        layoutContentActivities.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
    }

    private void updateTabButton(TextView btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.personal_bg_tab_selected);
            btn.setTextColor(ContextCompat.getColor(this, R.color.personal_text_primary));
            btn.setTypeface(AdminPersonalTypography.readable(Typeface.BOLD));
        } else {
            btn.setBackgroundResource(R.drawable.personal_bg_tab_unselected);
            btn.setTextColor(ContextCompat.getColor(this, R.color.personal_text_secondary));
            btn.setTypeface(AdminPersonalTypography.readable(Typeface.NORMAL));
        }
    }

    private void bindSummaryTab() {
        int score = employee.getKpiSummaryScore();
        tvHeroPeriod.setText(score >= 0 ? "Período consultado" : "KPI sin datos conectados");
        if (score >= 0) {
            cpvHeroScore.setProgress(score, "de avance");
            tvHeroStatus.setText(employee.getPerformanceSummaryStatus());
            tvHeroSubtitle.setText(employee.getPerformanceComparisonText());
            pbHeroEvaluations.setProgress(score);
        } else {
            cpvHeroScore.setProgress(-1, "Pendiente");
            tvHeroStatus.setText("Pendiente de evaluación");
            tvHeroSubtitle.setText("Sin evaluación registrada este período");
            pbHeroEvaluations.setProgress(0);
        }

        tvHeroEvaluations.setText(score >= 0 ? employee.getEvaluationsCountText()
                : "Evaluaciones no disponibles en esta vista");

        // Estadísticas personales
        tvStatAssigned.setText(score >= 0 ? String.valueOf(employee.getKpiAssignedCount()) : "—");
        tvStatCompleted.setText(score >= 0 ? String.valueOf(employee.getKpiCompletedCount()) : "—");
        tvStatPending.setText(score >= 0 ? String.valueOf(employee.getKpiPendingCount()) : "—");

        // Gráfico de líneas de evolución
        chartPerformanceEvolution.setData(employee.getEvaluationHistory());

        // Información laboral
        tvLaborHireDate.setText(available(employee.getHireDate()));
        tvLaborSchedule.setText(available(employee.getWorkSchedule()));
        tvLaborContract.setText(available(employee.getContractType()));
        tvLaborModality.setText(available(employee.getWorkModality()));
    }

    private String available(String value) {
        return value == null || value.trim().isEmpty() ? "No disponible" : value;
    }

    private void bindKpiTab() {
        RecyclerView rvKpi = findViewById(R.id.rv_kpi_list);
        rvKpi.setLayoutManager(new LinearLayoutManager(this));
        kpiAdapter = new KpiAdapter();
        rvKpi.setAdapter(kpiAdapter);

        kpiAdapter.submitList(employee.getKpis());
        chartKpiHistory.setData(employee.getEvaluationHistory());
        boolean hasKpis = !employee.getKpis().isEmpty();
        findViewById(R.id.personal_detail_kpi_periods).setVisibility(hasKpis ? View.VISIBLE : View.GONE);
        findViewById(R.id.rv_kpi_list).setVisibility(hasKpis ? View.VISIBLE : View.GONE);
        findViewById(R.id.personal_detail_kpi_empty).setVisibility(hasKpis ? View.GONE : View.VISIBLE);
        findViewById(R.id.personal_detail_kpi_history).setVisibility(
                employee.getEvaluationHistory().isEmpty() ? View.GONE : View.VISIBLE);

        // Selectores de período
        setupKpiPeriodChips();
    }

    private void setupKpiPeriodChips() {
        View.OnClickListener listener = v -> {
            updateKpiPeriodChipStyle(chipKpiThisMonth, v == chipKpiThisMonth);
            updateKpiPeriodChipStyle(chipKpiThisWeek, v == chipKpiThisWeek);
            updateKpiPeriodChipStyle(chipKpiPrevMonth, v == chipKpiPrevMonth);
            updateKpiPeriodChipStyle(chipKpiCustom, v == chipKpiCustom);

            if (v == chipKpiThisMonth) {
                kpiAdapter.submitList(employee.getKpis());
            } else if (v == chipKpiThisWeek) {
                // Filtro dinámico simulado para esta semana
                List<EmployeeKpiItem> weekList = new ArrayList<>();
                for (EmployeeKpiItem item : employee.getKpis()) {
                    if (item.getStatus() == EmployeeKpiItem.KpiStatus.CUMPLIDO) {
                        weekList.add(item);
                    }
                }
                kpiAdapter.submitList(weekList.isEmpty() ? employee.getKpis() : weekList);
            } else {
                kpiAdapter.submitList(employee.getKpis());
            }
        };

        chipKpiThisMonth.setOnClickListener(listener);
        chipKpiThisWeek.setOnClickListener(listener);
        chipKpiPrevMonth.setOnClickListener(listener);
        chipKpiCustom.setOnClickListener(listener);
    }

    private void updateKpiPeriodChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.personal_bg_dept_chip_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.personal_text_primary));
            chip.setTypeface(AdminPersonalTypography.readable(Typeface.BOLD));
        } else {
            chip.setBackgroundResource(R.drawable.personal_bg_dept_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.personal_text_secondary));
            chip.setTypeface(AdminPersonalTypography.readable(Typeface.NORMAL));
        }
    }

    private void bindActivitiesTab() {
        RecyclerView rvActivities = findViewById(R.id.rv_activities_list);
        rvActivities.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new ActivityAdapter();
        rvActivities.setAdapter(activityAdapter);

        List<EmployeeActivityItem> allActivities = employee.getActivities();
        activityAdapter.submitList(allActivities);

        // Próxima actividad destacada
        if (!allActivities.isEmpty()) {
            EmployeeActivityItem nextAct = allActivities.get(0);
            cardUpcoming.setVisibility(View.VISIBLE);
            tvUpcomingTitle.setText(nextAct.getTitle());
            tvUpcomingDeadline.setText("Fecha límite: " + nextAct.getDeadline() + " • " + nextAct.getCategory());
        } else {
            cardUpcoming.setVisibility(View.GONE);
        }

        setupActivityFilterChips();
        applyActivityFilter();
    }

    private void setupActivityFilterChips() {
        TextView[] chips = {chipActAll, chipActPending, chipActInProgress, chipActCompleted, chipActOverdue};
        String[] statuses = {"Todas", "Pendiente", "En progreso", "Completada", "Vencida"};

        for (int i = 0; i < chips.length; i++) {
            final String status = statuses[i];
            final TextView chip = chips[i];

            chip.setOnClickListener(v -> {
                currentActivityStatusFilter = status;
                for (int j = 0; j < chips.length; j++) {
                    updateKpiPeriodChipStyle(chips[j], chips[j] == chip);
                }
                applyActivityFilter();
            });
        }
    }

    private void applyActivityFilter() {
        List<EmployeeActivityItem> all = employee.getActivities();
        List<EmployeeActivityItem> filtered = new ArrayList<>();

        if ("Todas".equalsIgnoreCase(currentActivityStatusFilter)) {
            filtered.addAll(all);
        } else {
            for (EmployeeActivityItem item : all) {
                if (statusMatches(item.getStatus(), currentActivityStatusFilter)) {
                    filtered.add(item);
                }
            }
        }

        activityAdapter.submitList(filtered);

        if (filtered.isEmpty()) {
            tvActivitiesEmpty.setVisibility(View.VISIBLE);
        } else {
            tvActivitiesEmpty.setVisibility(View.GONE);
        }
    }

    private boolean statusMatches(String itemStatus, String filter) {
        if (itemStatus == null) return false;
        if ("Pendiente".equalsIgnoreCase(filter) && "Pendiente".equalsIgnoreCase(itemStatus)) return true;
        if ("En progreso".equalsIgnoreCase(filter) && "En progreso".equalsIgnoreCase(itemStatus)) return true;
        if ("Completada".equalsIgnoreCase(filter) && "Completada".equalsIgnoreCase(itemStatus)) return true;
        if ("Vencida".equalsIgnoreCase(filter) && "Vencida".equalsIgnoreCase(itemStatus)) return true;
        return false;
    }
}
