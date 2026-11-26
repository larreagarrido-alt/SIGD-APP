package pe.net.ws.kpiweb.features.admin.dashboard;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.gson.JsonObject;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.collaborator.attendance.CollaboratorAttendanceActivity;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import pe.net.ws.kpiweb.features.admin.dashboard.state.AdminDateFilterState;
import pe.net.ws.kpiweb.features.admin.dashboard.state.AdminFilterMode;

/**
 * Pantalla principal del Dashboard Administrativo para WEBSITE.
 * Integra diseño dark premium de Figma, navegación por fechas avanzada,
 * tarjeta hero con anillo de progreso, KPIs en 2 columnas y menú lateral.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private NestedScrollView nestedScrollView;

    // Encabezado y Saludo (Figma)
    private TextView tvGreetingTitle;
    private TextView tvGreetingSub;
    private View layoutProfileBadge;
    private TextView tvProfileName;
    private TextView tvProfileRole;
    private TextView tvProfileInitials;
    private ImageView ivProfilePhoto;

    // Controles de fecha (Conservados)
    private ImageButton btnDatePrev;
    private ImageButton btnDateNext;
    private TextView tvWeekTitle;
    private TextView tvSelectedDate;
    private View layoutDateSelector;
    private TextView btnTodayQuick;

    // Evolución de la semana (Gráfico interactivo)
    private View sectionWeeklyChart;
    private TextView tvWeeklyChartSubtitle;
    private TextView tvWeeklyChartCallout;
    private LinearLayout weeklyChartBars;

    // Selector de modo
    private TextView btnModeDay;
    private TextView btnModeWeek;
    private TextView btnModeMonth;
    private View chipCustom;
    private TextView tvCustomLabel;
    private ImageView icCustom;

    // Tarjeta Hero de Rendimiento General (Figma)
    private TextView tvHeroBadgePeriod;
    private CircularProgressIndicator heroCircularProgress;
    private TextView tvHeroProgressVal;
    private TextView tvHeroProgressFooter;
    private TextView tvHeroAction;
    private LinearProgressIndicator heroLinearProgress;

    // 4 Tarjetas Estadísticas Principales (Figma inicio_administrador.png)
    private TextView tvStatCollaboratorsVal;
    private TextView tvStatCollaboratorsSub;
    private TextView tvStatReportsVal;
    private TextView tvStatReportsSub;
    private TextView tvStatPendingVal;
    private TextView tvStatPendingSub;
    private TextView tvStatAttendanceVal;

    // Acciones de Resumen de hoy (Figma)
    private View btnReviewNow;
    private View cardActionTeam;
    private View cardActionReport;
    private View btnSeeAllToday;
    private View btnSeeAllAlerts;

    // Estado y Datos
    private AdminDateFilterState dateFilterState;
    private Call<ApiResponse<JsonObject>> dashboardRequest;
    private Call<ApiResponse<LoginResponseData>> profileRequest;
    private final Locale localeEs = new Locale("es", "ES");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_dashboard);

        dateFilterState = new AdminDateFilterState();
        dateFilterState.setMode(AdminFilterMode.SEMANA);

        setupWindowAppearance();
        initViews();
        setupNavigation();
        setupDateFilters();
        setupActions();
        AdminTypography.apply(this, findViewById(android.R.id.content));

        // Escuchar cambios de estado en el filtro de fechas
        dateFilterState.setOnFilterChangedListener(state -> updateDashboardData());

        // Carga inicial
        updateDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupWindowAppearance();
        bindAccountHeader();
        updateDashboardData();
    }

    @Override
    protected void onStop() {
        if (dashboardRequest != null) dashboardRequest.cancel();
        if (profileRequest != null) profileRequest.cancel();
        super.onStop();
    }

    private void setupWindowAppearance() {
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void initViews() {
        nestedScrollView = findViewById(R.id.admin_scroll_view);

        // Encabezado
        tvGreetingTitle = findViewById(R.id.admin_tv_greeting_title);
        tvGreetingSub = findViewById(R.id.admin_tv_greeting_sub);
        layoutProfileBadge = findViewById(R.id.admin_layout_profile_badge);
        tvProfileName = findViewById(R.id.admin_tv_profile_name);
        tvProfileRole = findViewById(R.id.admin_tv_profile_role);
        tvProfileInitials = findViewById(R.id.admin_tv_profile_initials);
        ivProfilePhoto = findViewById(R.id.admin_iv_profile_photo);

        // Header y fechas
        btnDatePrev = findViewById(R.id.admin_btn_date_prev);
        btnDateNext = findViewById(R.id.admin_btn_date_next);
        tvWeekTitle = findViewById(R.id.admin_tv_week_title);
        tvSelectedDate = findViewById(R.id.admin_tv_selected_date);
        layoutDateSelector = findViewById(R.id.admin_layout_date_picker_trigger);
        btnTodayQuick = findViewById(R.id.admin_btn_today_quick);

        sectionWeeklyChart = findViewById(R.id.admin_section_weekly_chart);
        tvWeeklyChartSubtitle = findViewById(R.id.admin_weekly_chart_subtitle);
        tvWeeklyChartCallout = findViewById(R.id.admin_weekly_chart_callout);
        weeklyChartBars = findViewById(R.id.admin_weekly_chart_bars);

        stylePersonalIconBadge(findViewById(R.id.admin_dash_quick_wellness_icon_box),
                ContextCompat.getColor(this, R.color.admin_dash_badge_red_bg),
                ContextCompat.getColor(this, R.color.sigd_sec_coral));
        stylePersonalIconBadge(findViewById(R.id.admin_dash_quick_kpi_icon_box),
                ContextCompat.getColor(this, R.color.admin_dash_badge_green_bg),
                ContextCompat.getColor(this, R.color.admin_dash_green));
        stylePersonalIconBadge(findViewById(R.id.admin_dash_quick_schedule_icon_box),
                ContextCompat.getColor(this, R.color.admin_dash_badge_cyan_bg),
                ContextCompat.getColor(this, R.color.admin_dash_cyan));

        // Modos
        btnModeDay = findViewById(R.id.admin_btn_mode_day);
        btnModeWeek = findViewById(R.id.admin_btn_mode_week);
        btnModeMonth = findViewById(R.id.admin_btn_mode_month);
        chipCustom = findViewById(R.id.admin_chip_filter_custom);
        if (chipCustom != null) {
            tvCustomLabel = chipCustom.findViewById(R.id.admin_tv_filter_custom_label);
            icCustom = chipCustom.findViewById(R.id.admin_ic_filter_custom);
        }

        // Tarjeta Hero
        tvHeroBadgePeriod = findViewById(R.id.admin_tv_hero_badge_period);
        heroCircularProgress = findViewById(R.id.admin_hero_circular_progress);
        tvHeroProgressVal = findViewById(R.id.admin_tv_hero_progress_val);
        tvHeroProgressFooter = findViewById(R.id.admin_tv_hero_progress_footer);
        tvHeroAction = findViewById(R.id.admin_btn_hero_action);
        heroLinearProgress = findViewById(R.id.admin_hero_linear_progress);

        // 4 Tarjetas Estadísticas (Figma inicio_administrador.png)
        tvStatCollaboratorsVal = findViewById(R.id.admin_tv_stat_collaborators_val);
        tvStatCollaboratorsSub = findViewById(R.id.admin_tv_stat_collaborators_sub);
        tvStatReportsVal = findViewById(R.id.admin_tv_stat_reports_val);
        tvStatReportsSub = findViewById(R.id.admin_tv_stat_reports_sub);
        tvStatPendingVal = findViewById(R.id.admin_tv_stat_pending_val);
        tvStatPendingSub = findViewById(R.id.admin_tv_stat_pending_sub);
        tvStatAttendanceVal = findViewById(R.id.admin_tv_stat_attendance_val);

        // Acciones Figma
        btnReviewNow = findViewById(R.id.admin_btn_review_now);
        cardActionTeam = findViewById(R.id.admin_card_action_team);
        cardActionReport = findViewById(R.id.admin_card_action_report);
        btnSeeAllToday = findViewById(R.id.admin_btn_see_all_today);
        btnSeeAllAlerts = findViewById(R.id.admin_btn_see_all_alerts);

    }

    private void setupNavigation() {
        // Barra de Navegación Inferior (5 pestañas oficiales)
        View tabHome = findViewById(R.id.admin_nav_tab_home);
        if (tabHome != null) {
            tabHome.setOnClickListener(v -> {
                if (nestedScrollView != null) {
                    nestedScrollView.smoothScrollTo(0, 0);
                }
            });
        }

        View tabStaff = findViewById(R.id.admin_nav_tab_staff);
        if (tabStaff != null) {
            tabStaff.setOnClickListener(v -> {
                Intent intent = new Intent(this, pe.net.ws.kpiweb.features.admin.personal.AdminPersonalActivity.class);
                startActivity(intent);
            });
        }

        View tabReports = findViewById(R.id.admin_nav_tab_reports);
        if (tabReports != null) {
            tabReports.setOnClickListener(v -> openWorkerReports());
        }

        View tabNotifs = findViewById(R.id.admin_nav_tab_notifs);
        if (tabNotifs != null) {
            tabNotifs.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminNotificationsActivity.class)));
        }

        View tabMenu = findViewById(R.id.admin_nav_tab_menu);
        if (tabMenu != null) {
            tabMenu.setOnClickListener(v -> openModulesMenu());
        }
    }

    private void openModulesMenu() {
        Intent intent = new Intent(this, AdminModulesMenuActivity.class);
        startActivity(intent);
    }

    private void setupDateFilters() {
        btnDatePrev.setOnClickListener(v -> dateFilterState.previous());
        btnDateNext.setOnClickListener(v -> dateFilterState.next());
        btnTodayQuick.setOnClickListener(v -> dateFilterState.applyThisWeek());
        btnTodayQuick.setText("Esta semana");

        layoutDateSelector.setOnClickListener(v -> showNativeDatePicker());

        // Modos
        btnModeDay.setOnClickListener(v -> dateFilterState.setMode(AdminFilterMode.DIA));
        btnModeWeek.setOnClickListener(v -> dateFilterState.setMode(AdminFilterMode.SEMANA));
        btnModeMonth.setOnClickListener(v -> dateFilterState.setMode(AdminFilterMode.MES));

        if (chipCustom != null) {
            chipCustom.setOnClickListener(v -> showCustomRangeDialog());
        }
    }

    private void setupActions() {
        if (layoutProfileBadge != null) {
            layoutProfileBadge.setOnClickListener(v -> openAccount());
        }

        btnReviewNow.setOnClickListener(v -> openWorkerReports());
        cardActionTeam.setOnClickListener(v -> {
            Intent intent = new Intent(this, pe.net.ws.kpiweb.features.admin.personal.AdminPersonalActivity.class);
            startActivity(intent);
        });
        cardActionReport.setOnClickListener(v -> openWorkerReports());

        // Mi espacio personal (reutiliza rutas reales sin Ranking ni Premios)
        View quickWellness = findViewById(R.id.admin_dash_quick_wellness);
        if (quickWellness != null) {
            quickWellness.setOnClickListener(v -> {
                Intent intent = new Intent(this, CollaboratorWellnessActivity.class);
                intent.putExtra(CollaboratorWellnessActivity.EXTRA_RETURN_ADMIN, true);
                startActivity(intent);
            });
        }
        View quickKpi = findViewById(R.id.admin_dash_quick_kpi);
        if (quickKpi != null) {
            quickKpi.setOnClickListener(v -> {
                Intent intent = new Intent(this, CollaboratorKpiIntegrationActivity.class);
                intent.putExtra(CollaboratorKpiIntegrationActivity.EXTRA_RETURN_ADMIN, true);
                startActivity(intent);
            });
        }
        View quickSchedule = findViewById(R.id.admin_dash_quick_schedule);
        if (quickSchedule != null) {
            quickSchedule.setOnClickListener(v -> startActivity(new Intent(this,
                    pe.net.ws.kpiweb.features.schedule.ui.ScheduleActivity.class)));
        }
    }

    private void stylePersonalIconBadge(View target, int bgColor, int accentColor) {
        if (target == null) return;
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(12f * d);
        bg.setColor(bgColor);
        bg.setStroke(Math.max(1, Math.round(d)), (accentColor & 0x00FFFFFF) | 0x45000000);
        target.setBackground(bg);
    }

    private void openWorkerReports() {
        Intent reports = new Intent(this, AdminKpiOfficialActivity.class);
        reports.putExtra(AdminKpiOfficialActivity.EXTRA_REPORTS_CENTER, true);
        startActivity(reports);
    }

    private void openAccount() {
        Intent intent = new Intent(this, pe.net.ws.kpiweb.features.account.AccountActivity.class);
        intent.putExtra(pe.net.ws.kpiweb.features.account.AccountActivity.EXTRA_USER_ROLE,
                pe.net.ws.kpiweb.features.account.model.UserRole.ADMINISTRADOR.name());
        startActivity(intent);
    }

    private void updateDashboardData() {
        Calendar ref = dateFilterState.getReferenceCalendar();

        // 1. Selector de semana moderno con número de semana y rango legible
        if (tvWeekTitle != null) {
            tvWeekTitle.setText(AdminDashboardPeriod.formatWeekNumberLabel(ref));
        }
        tvSelectedDate.setText(AdminDashboardPeriod.formatWeekRangeLong(ref));
        btnDatePrev.setEnabled(true);
        btnDatePrev.setAlpha(1.0f);
        boolean nextWeekAvailable = dateFilterState.getEndDate().before(Calendar.getInstance());
        btnDateNext.setEnabled(nextWeekAvailable);
        btnDateNext.setAlpha(nextWeekAvailable ? 1.0f : 0.35f);

        // 2. Subtítulo legible (sin códigos técnicos tipo 2026-W47 como título principal)
        String readableWeek = AdminDashboardPeriod.formatWeekSummarySubtitle(ref);
        tvGreetingSub.setText("Resumen semanal · " + readableWeek);
        tvHeroBadgePeriod.setText(readableWeek);

        // 4. Actualizar visual de modos de vista
        highlightMode(dateFilterState.getMode());

        // The live endpoint only understands a KPI week key. Never present its
        // totals as if they corresponded to a day, month or arbitrary range.
        if (dashboardRequest != null) dashboardRequest.cancel();
        clearLiveMetrics("Consultando el resumen", "Estamos comprobando los datos de " + AdminDashboardPeriod.formatWeekNumberLabel(ref).toLowerCase(localeEs) + ".", null, null);
        loadWeeklyDashboard(ref);
        return;
    }

    private void bindAccountHeader() {
        SessionManager session = new SessionManager(this);
        String name = session.isLoggedIn() ? session.getFullName() : "";
        if (name == null || name.trim().isEmpty()) name = session.getUsername();
        String firstName = AdminDashboardHeaderFormatter.extractFirstName(name, session.isLoggedIn() ? "Cuenta" : "Sin sesión");
        String role = session.isLoggedIn() ? session.getRolName() : "";
        if (role == null || role.trim().isEmpty()) role = session.isLoggedIn() ? "Administrador" : "Sin sesión";
        tvProfileName.setText(firstName);
        tvProfileRole.setText(role);
        tvGreetingTitle.setText(session.isLoggedIn() ? "Hola, " + firstName : "Hola");
        tvProfileInitials.setText(AdminDashboardHeaderFormatter.extractInitials(name, localeEs));
        CollaboratorProfilePhotoLoader.load(this, ivProfilePhoto, tvProfileInitials,
                session.isLoggedIn() ? session.getWorkerPhoto() : "");
        if (!session.isLoggedIn() || session.getUserId() <= 0) return;
        if (profileRequest != null) profileRequest.cancel();
        int expectedUser = session.getUserId();
        int expectedRole = session.getRoleId();
        int expectedWorker = session.getWorkerId();
        String expectedToken = session.getAccessToken();
        profileRequest = ApiClient.getService().getProfile(session.getBearerToken());
        profileRequest.enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponseData>> call,
                                   Response<ApiResponse<LoginResponseData>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != profileRequest || call.isCanceled() || isFinishing() || isDestroyed()
                        || !expectedToken.equals(new SessionManager(AdminDashboardActivity.this).getAccessToken())) return;
                profileRequest = null;
                ApiResponse<LoginResponseData> envelope = response.body();
                LoginResponseData data = envelope == null ? null : envelope.getData();
                LoginResponseData.WorkerInfo worker = data == null ? null : data.getWorker();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || data == null || data.getUser() == null
                        || data.getUser().getIdUsuario() != expectedUser
                        || data.getUser().getIdRol() != expectedRole
                        || (worker == null ? expectedWorker != 0 : worker.getIdTrabajador() != expectedWorker)) return;
                String verifiedName = worker == null ? data.getUser().getUsername() : worker.getNombreCompleto();
                if (verifiedName == null || verifiedName.trim().isEmpty()) verifiedName = data.getUser().getUsername();
                if (verifiedName == null || verifiedName.trim().isEmpty()) return;
                String verifiedFirstName = AdminDashboardHeaderFormatter.extractFirstName(verifiedName, "Cuenta");
                tvProfileName.setText(verifiedFirstName);
                tvGreetingTitle.setText("Hola, " + verifiedFirstName);
                String verifiedRole = data.getUser().getRolNombre();
                if (verifiedRole != null && !verifiedRole.trim().isEmpty()) {
                    tvProfileRole.setText(verifiedRole.trim());
                }
                tvProfileInitials.setText(AdminDashboardHeaderFormatter.extractInitials(verifiedName, localeEs));
                if (worker != null) {
                    session.updateVerifiedWorkerDetails(expectedWorker, verifiedName,
                            worker.getCargoNombre(), worker.getAgenciaNombre(), worker.getTipo(),
                            worker.getEmail(), worker.getFotoPerfil(), worker.getEstado());
                }
                CollaboratorProfilePhotoLoader.load(AdminDashboardActivity.this, ivProfilePhoto,
                        tvProfileInitials, worker == null ? "" : worker.getFotoPerfil());
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponseData>> call, Throwable error) {
                if (call == profileRequest) profileRequest = null;
            }
        });
    }

    private void clearLiveMetrics(String title, String detail, String action,
                                  View.OnClickListener onAction) {
        findViewById(R.id.admin_section_today).setVisibility(View.GONE);
        findViewById(R.id.admin_section_alerts).setVisibility(View.GONE);
        findViewById(R.id.admin_section_employees).setVisibility(View.GONE);
        View kpiSectionTitle = findViewById(R.id.admin_tv_kpi_section_title);
        if (kpiSectionTitle != null) kpiSectionTitle.setVisibility(View.GONE);
        findViewById(R.id.admin_stats_row_primary).setVisibility(View.GONE);
        findViewById(R.id.admin_stats_row_secondary).setVisibility(View.GONE);
        if (sectionWeeklyChart != null) sectionWeeklyChart.setVisibility(View.GONE);
        findViewById(R.id.admin_dashboard_footer).setVisibility(View.GONE);
        String readableWeek = AdminDashboardPeriod.formatWeekSummarySubtitle(dateFilterState.getReferenceCalendar());
        tvGreetingSub.setText("Resumen semanal · " + readableWeek);
        ((TextView) findViewById(R.id.admin_tv_hero_headline)).setText(title);
        ((TextView) findViewById(R.id.admin_tv_hero_subline)).setText(detail);
        ((TextView) findViewById(R.id.admin_tv_stat_collaborators_label)).setText("Personal registrado");
        ((TextView) findViewById(R.id.admin_tv_stat_reports_label)).setText("KPI entregados");
        ((TextView) findViewById(R.id.admin_tv_stat_pending_label)).setText("KPI pendientes");
        ((TextView) findViewById(R.id.admin_tv_stat_attendance_label)).setText("KPI vencidos");
        findViewById(R.id.admin_tv_stat_attendance_badge).setVisibility(View.GONE);
        tvStatCollaboratorsVal.setText("—");
        tvStatReportsVal.setText("—");
        tvStatPendingVal.setText("—");
        tvStatAttendanceVal.setText("—");
        tvStatAttendanceVal.setTextColor(ContextCompat.getColor(this, R.color.admin_text_primary));
        tvStatCollaboratorsSub.setText("Total del servidor");
        tvStatReportsSub.setText("Período seleccionado");
        tvStatPendingSub.setText("Período seleccionado");
        TextView overdueSub = findViewById(R.id.admin_tv_stat_overdue_sub);
        if (overdueSub != null) overdueSub.setText("Período seleccionado");
        findViewById(R.id.admin_hero_progress_ring).setVisibility(View.GONE);
        heroLinearProgress.setVisibility(View.GONE);
        heroCircularProgress.setProgressCompat(0, false);
        heroLinearProgress.setProgressCompat(0, false);
        tvHeroProgressVal.setText("");
        tvHeroProgressFooter.setVisibility(View.GONE);
        tvHeroAction.setVisibility(action == null ? View.GONE : View.VISIBLE);
        if (action != null) {
            tvHeroAction.setText(action);
            tvHeroAction.setOnClickListener(onAction);
        }
    }

    private void loadWeeklyDashboard(Calendar selectedDate) {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getBearerToken().isEmpty()) {
            clearLiveMetrics("Inicia sesión", "Accede para consultar el resumen semanal.",
                    "Ir al acceso", v -> startActivity(new Intent(this,
                            pe.net.ws.kpiweb.features.auth.LoginActivity.class)));
            return;
        }
        // The PHP endpoint uses date('Y-\\WW'): calendar year plus ISO week.
        // Use the selected date, not the Monday at a year boundary.
        String period = AdminDashboardPeriod.fromSelectedDate(selectedDate);
        String expectedToken = session.getAccessToken();
        dashboardRequest = ApiClient.getService().getDashboardResumen(session.getBearerToken(), period);
        dashboardRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != dashboardRequest || call.isCanceled() || isFinishing() || isDestroyed()
                        || !expectedToken.equals(new SessionManager(AdminDashboardActivity.this).getAccessToken())) return;
                dashboardRequest = null;
                ApiResponse<JsonObject> envelope = response.body();
                if (response.code() == 403) {
                    clearLiveMetrics("Acceso al resumen restringido",
                            "Esta cuenta aún no tiene acceso a las cifras semanales. Revisa sus permisos de administrador.",
                            "Ver mi cuenta", v -> openAccount());
                    return;
                }
                if (response.code() == 401) {
                    clearLiveMetrics("Sesión finalizada", "Ingresa de nuevo para consultar los datos.",
                            "Ir al acceso", v -> startActivity(new Intent(AdminDashboardActivity.this,
                                    pe.net.ws.kpiweb.features.auth.LoginActivity.class)));
                    return;
                }
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || envelope.getData() == null) {
                    clearLiveMetrics("Resumen no disponible", "El servidor no pudo entregar datos válidos.",
                            "Reintentar", v -> updateDashboardData());
                    return;
                }
                showLiveMetrics(envelope.getData(), period);
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call != dashboardRequest || call.isCanceled()) return;
                dashboardRequest = null;
                clearLiveMetrics("Sin conexión", "Comprueba la conexión e inténtalo nuevamente.",
                        "Reintentar", v -> updateDashboardData());
            }
        });
    }

    private void showLiveMetrics(JsonObject data, String requestedPeriod) {
        if (!data.has("periodo_consultado") || data.get("periodo_consultado").isJsonNull()
                || !requestedPeriod.equals(data.get("periodo_consultado").getAsString())) {
            clearLiveMetrics("Período no válido", "La respuesta no corresponde a la semana elegida.",
                    "Reintentar", v -> updateDashboardData());
            return;
        }
        JsonObject personal = objectOrNull(data, "personal");
        JsonObject kpi = objectOrNull(data, "kpi");
        if (personal == null || kpi == null) {
            clearLiveMetrics("Resumen incompleto", "Faltan cifras necesarias para mostrar esta semana.",
                    "Reintentar", v -> updateDashboardData());
            return;
        }
        String readableWeek = AdminDashboardPeriod.formatWeekSummarySubtitle(dateFilterState.getReferenceCalendar());
        tvGreetingSub.setText("Resumen semanal · " + readableWeek);
        View kpiSectionTitle = findViewById(R.id.admin_tv_kpi_section_title);
        if (kpiSectionTitle != null) kpiSectionTitle.setVisibility(View.VISIBLE);
        findViewById(R.id.admin_stats_row_primary).setVisibility(View.VISIBLE);
        findViewById(R.id.admin_stats_row_secondary).setVisibility(View.VISIBLE);
        findViewById(R.id.admin_dashboard_footer).setVisibility(View.VISIBLE);
        tvHeroProgressFooter.setVisibility(View.VISIBLE);
        tvHeroAction.setVisibility(View.VISIBLE);
        tvHeroAction.setText("Ver reportes recibidos");
        tvHeroAction.setOnClickListener(v -> openWorkerReports());
        ((TextView) findViewById(R.id.admin_tv_hero_subline)).setText(readableWeek + " · Datos verificados");
        tvHeroBadgePeriod.setText(readableWeek);
        tvStatCollaboratorsVal.setText(numberOrDash(personal, "total_empleados"));
        Integer rawReports = integerOrNull(data, "formularios_recibidos_total");
        ((TextView) findViewById(R.id.admin_tv_stat_reports_label)).setText("Formularios recibidos en total");
        tvStatReportsVal.setText(rawReports == null ? "—" : String.valueOf(rawReports));
        tvStatReportsSub.setText("Guardados en el servidor; sin evaluación");
        Integer pendingCount = integerOrNull(kpi, "kpis_pendientes");
        tvStatPendingVal.setText(pendingCount == null ? "—" : String.valueOf(pendingCount));
        Integer overdueCount = integerOrNull(kpi, "kpis_vencidos");
        tvStatAttendanceVal.setText(overdueCount == null ? "—" : String.valueOf(overdueCount));
        tvStatAttendanceVal.setTextColor(ContextCompat.getColor(this,
                overdueCount != null && overdueCount > 0 ? R.color.admin_dash_red : R.color.admin_text_primary));
        Integer total = integerOrNull(kpi, "kpis_totales");
        Integer delivered = integerOrNull(kpi, "kpis_entregados");
        Integer approved = integerOrNull(kpi, "kpis_aprobados");
        String rawReportLabel = rawReports == null ? "" : " · " + rawReports + " formularios recibidos en total";
        if (total == null || delivered == null || approved == null || total <= 0) {
            ((TextView) findViewById(R.id.admin_tv_hero_headline)).setText("Resumen semanal");
            findViewById(R.id.admin_hero_progress_ring).setVisibility(View.GONE);
            heroLinearProgress.setVisibility(View.GONE);
            tvHeroProgressFooter.setText("Sin KPI registrados en esta semana para calcular el avance" + rawReportLabel);
            renderWeeklyEvolutionChart(readableWeek, 0, 0, 0, 0);
            return;
        }
        ((TextView) findViewById(R.id.admin_tv_hero_headline)).setText("Resumen semanal");
        int completed = Math.min(total, Math.max(0, delivered + approved));
        int percent = Math.round(completed * 100f / total);
        findViewById(R.id.admin_hero_progress_ring).setVisibility(View.VISIBLE);
        heroLinearProgress.setVisibility(View.VISIBLE);
        heroCircularProgress.setProgressCompat(percent, false);
        heroLinearProgress.setProgressCompat(percent, false);
        tvHeroProgressVal.setText(percent + "%");
        tvHeroProgressFooter.setText("KPI cumplidos: " + completed + " de " + total + rawReportLabel);
        renderWeeklyEvolutionChart(
                readableWeek,
                total,
                completed,
                pendingCount != null ? Math.max(0, pendingCount) : 0,
                overdueCount != null ? Math.max(0, overdueCount) : 0
        );
    }

    private void renderWeeklyEvolutionChart(String readableWeek, int total, int completed, int pending, int overdue) {
        if (sectionWeeklyChart == null || weeklyChartBars == null) return;
        sectionWeeklyChart.setVisibility(View.VISIBLE);
        weeklyChartBars.removeAllViews();
        if (tvWeeklyChartSubtitle != null) {
            tvWeeklyChartSubtitle.setText(readableWeek + " · Toca una barra para ver el detalle");
        }
        int secondaryColor = ContextCompat.getColor(this, R.color.admin_text_secondary);
        int zeroBarColor = ContextCompat.getColor(this, R.color.admin_surface_tertiary);
        if (total <= 0) {
            if (tvWeeklyChartCallout != null) tvWeeklyChartCallout.setVisibility(View.GONE);
            TextView empty = new TextView(this);
            empty.setText("Aún no existen KPI registrados en " + readableWeek + " para graficar la evolución.");
            empty.setTextColor(secondaryColor);
            empty.setTextSize(12f);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(10), dp(14), dp(10), dp(14));
            weeklyChartBars.addView(empty);
            return;
        }

        if (tvWeeklyChartCallout != null) {
            tvWeeklyChartCallout.setVisibility(View.VISIBLE);
            tvWeeklyChartCallout.setText(readableWeek + " · Total: " + total + " KPI evaluables");
        }

        LinearLayout chartRow = new LinearLayout(this);
        chartRow.setOrientation(LinearLayout.HORIZONTAL);
        chartRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(132));
        weeklyChartBars.addView(chartRow, rowLp);

        String[] labels = {"Cumplidos", "Pendientes", "Vencidos"};
        int[] values = {completed, pending, overdue};
        int[] colors = {
                ContextCompat.getColor(this, R.color.admin_dash_green),
                ContextCompat.getColor(this, R.color.admin_dash_amber),
                ContextCompat.getColor(this, R.color.sigd_sec_coral)
        };
        int maxVal = Math.max(1, Math.max(completed, Math.max(pending, overdue)));

        for (int i = 0; i < labels.length; i++) {
            final String lbl = labels[i];
            final int val = values[i];
            final int pct = Math.round(val * 100f / total);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setClickable(true);
            col.setFocusable(true);
            col.setOnClickListener(v -> {
                if (tvWeeklyChartCallout != null) {
                    tvWeeklyChartCallout.setText(readableWeek + " · " + lbl + ": " + val
                            + " KPI (" + pct + "% del total)");
                }
            });
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            colLp.setMarginStart(dp(6));
            colLp.setMarginEnd(dp(6));

            TextView valTv = new TextView(this);
            valTv.setText(String.valueOf(val));
            valTv.setTextColor(colors[i]);
            valTv.setTextSize(12.5f);
            valTv.setTypeface(null, Typeface.BOLD);
            col.addView(valTv);

            int barH = val == 0 ? dp(8) : Math.max(dp(14), Math.round((val / (float) maxVal) * dp(78)));
            View bar = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(8));
            bg.setColor(val == 0 ? zeroBarColor : colors[i]);
            bar.setBackground(bg);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(34), barH);
            bLp.topMargin = dp(4);
            col.addView(bar, bLp);

            TextView lblTv = new TextView(this);
            lblTv.setText(lbl);
            lblTv.setTextColor(secondaryColor);
            lblTv.setTextSize(11f);
            lblTv.setMaxLines(1);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lLp.topMargin = dp(6);
            col.addView(lblTv, lLp);

            chartRow.addView(col, colLp);
        }
        AdminTypography.apply(this, weeklyChartBars);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static JsonObject objectOrNull(JsonObject data, String field) {
        return data.has(field) && data.get(field).isJsonObject() ? data.getAsJsonObject(field) : null;
    }

    private static Integer integerOrNull(JsonObject data, String field) {
        try {
            return data.has(field) && !data.get(field).isJsonNull() ? data.get(field).getAsInt() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String numberOrDash(JsonObject data, String field) {
        Integer value = integerOrNull(data, field);
        return value == null ? "—" : String.valueOf(value);
    }

    private void highlightMode(AdminFilterMode mode) {
        boolean isDay = (mode == AdminFilterMode.DIA);
        boolean isWeek = (mode == AdminFilterMode.SEMANA);
        boolean isMonth = (mode == AdminFilterMode.MES);
        boolean isCustom = (mode == AdminFilterMode.PERSONALIZADO);

        btnModeDay.setBackgroundResource(isDay ? R.drawable.admin_bg_segment_active : 0);
        btnModeDay.setTextColor(ContextCompat.getColor(this, isDay ? R.color.admin_text_primary : R.color.admin_text_secondary));
        btnModeDay.setTypeface(null, isDay ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        btnModeWeek.setBackgroundResource(isWeek ? R.drawable.admin_bg_segment_active : 0);
        btnModeWeek.setTextColor(ContextCompat.getColor(this, isWeek ? R.color.admin_text_primary : R.color.admin_text_secondary));
        btnModeWeek.setTypeface(null, isWeek ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        btnModeMonth.setBackgroundResource(isMonth ? R.drawable.admin_bg_segment_active : 0);
        btnModeMonth.setTextColor(ContextCompat.getColor(this, isMonth ? R.color.admin_text_primary : R.color.admin_text_secondary));
        btnModeMonth.setTypeface(null, isMonth ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        if (chipCustom != null) {
            chipCustom.setBackgroundResource(isCustom ? R.drawable.admin_bg_segment_active : R.drawable.admin_bg_date_picker_capsule);
            if (tvCustomLabel != null) {
                tvCustomLabel.setTextColor(ContextCompat.getColor(this, isCustom ? R.color.admin_text_primary : R.color.admin_text_secondary));
            }
            if (icCustom != null) {
                icCustom.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, isCustom ? R.color.admin_text_primary : R.color.admin_purple_accent)
                ));
            }
        }
        AdminTypography.apply(this, btnModeDay);
        AdminTypography.apply(this, btnModeWeek);
        AdminTypography.apply(this, btnModeMonth);
    }

    private void showNativeDatePicker() {
        AdminWeeklyCalendarDialog.show(
                this,
                dateFilterState.getReferenceCalendar(),
                (year, month, dayOfMonth) -> dateFilterState.applyWeekDate(year, month, dayOfMonth)
        );
    }

    private void showCustomRangeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.admin_dialog_date_range, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvStart = dialogView.findViewById(R.id.admin_tv_dialog_start_date);
        TextView tvEnd = dialogView.findViewById(R.id.admin_tv_dialog_end_date);
        TextView tvError = dialogView.findViewById(R.id.admin_tv_dialog_error);
        View btnApply = dialogView.findViewById(R.id.admin_btn_dialog_apply);
        View btnCancel = dialogView.findViewById(R.id.admin_btn_dialog_cancel);

        final Calendar selectedStart = Calendar.getInstance();
        final Calendar selectedEnd = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", localeEs);
        tvStart.setText(sdf.format(selectedStart.getTime()));
        tvEnd.setText(sdf.format(selectedEnd.getTime()));

        tvStart.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedStart.set(year, month, dayOfMonth);
                tvStart.setText(sdf.format(selectedStart.getTime()));
                tvError.setVisibility(View.GONE);
            }, selectedStart.get(Calendar.YEAR), selectedStart.get(Calendar.MONTH), selectedStart.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvEnd.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedEnd.set(year, month, dayOfMonth);
                tvEnd.setText(sdf.format(selectedEnd.getTime()));
                tvError.setVisibility(View.GONE);
            }, selectedEnd.get(Calendar.YEAR), selectedEnd.get(Calendar.MONTH), selectedEnd.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            if (selectedStart.after(selectedEnd)) {
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            dialog.dismiss();
            dateFilterState.applyCustomRange(selectedStart, selectedEnd);
        });

        dialog.show();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
