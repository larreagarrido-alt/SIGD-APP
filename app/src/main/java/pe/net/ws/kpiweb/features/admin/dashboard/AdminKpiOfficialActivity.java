package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Administrative KPI official management workflow (Centro de gestión -> Gestión de KPI).
 * Preserves official definitions, approvals, assignments, report review, comparison rules, and monthly closures.
 */
public final class AdminKpiOfficialActivity extends AppCompatActivity {
    public static final String EXTRA_REPORTS_CENTER = "reports_center";
    private static final int REPORT_PREVIEW_LIMIT = 3;

    private TextView periodValue, periodBadge, periodDetail, closeCardStatus;
    private ProgressBar periodLoading;
    private View feedbackBanner;
    private TextView feedbackText;
    private ImageView refreshIcon;

    private LinearLayout definitionsList;
    private View definitionsEmpty;

    private TextView reportsCount, reportsEmptyTitle, reportsEmptySubtitle, reportsToggleText;
    private View reportsSearchContainer, reportsSearchClear, reportsEmpty, reportsToggleBtn;
    private ImageView reportsSearchIcon, reportsToggleIcon;
    private EditText reportsSearch;
    private RecyclerView reportsRecycler;
    private AdminKpiReportAdapter reportAdapter;

    private final List<JsonObject> allReports = new ArrayList<>();
    private boolean reportsLoaded = false;
    private boolean showingAllReports = false;
    private boolean closingMonth = false;
    private boolean reportsCenter = false;

    private Call<ApiResponse<JsonObject>> definitionsCall;
    private Call<ApiResponse<JsonObject>> reportsCall;
    private Call<ApiResponse<JsonObject>> periodsCall;
    private Call<ApiResponse<JsonObject>> closeCall;
    private AlertDialog closeConfirmDialog;

    private String token;
    private int user;

    private final ActivityResultLauncher<Intent> configureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    String msg = result.getData() != null
                            ? result.getData().getStringExtra(AdminKpiConfigureActivity.EXTRA_RESULT_MESSAGE)
                            : null;
                    showFeedback(msg != null && !msg.isEmpty()
                            ? msg
                            : "Indicador guardado como pendiente. Apruébalo después de verificar sus reglas.");
                    refresh();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        user = session.getUserId();

        setContentView(R.layout.admin_activity_kpi_official);
        AdminBottomNavigation.attach(this, 1);
        getWindow().setStatusBarColor(getColor(R.color.admin_bg_main));
        getWindow().setNavigationBarColor(getColor(R.color.admin_bg_main));

        periodValue = findViewById(R.id.kpi_period_value);
        periodBadge = findViewById(R.id.kpi_period_badge);
        periodDetail = findViewById(R.id.kpi_period_detail);
        periodLoading = findViewById(R.id.kpi_period_loading);
        closeCardStatus = findViewById(R.id.kpi_close_card_status);

        feedbackBanner = findViewById(R.id.kpi_feedback_banner);
        feedbackText = findViewById(R.id.kpi_feedback_text);
        refreshIcon = findViewById(R.id.kpi_official_refresh_icon);

        definitionsList = findViewById(R.id.kpi_definitions_list);
        definitionsEmpty = findViewById(R.id.kpi_definitions_empty);

        reportsCount = findViewById(R.id.kpi_reports_count);
        reportsSearchContainer = findViewById(R.id.kpi_reports_search_container);
        reportsSearchIcon = findViewById(R.id.kpi_reports_search_icon);
        reportsSearch = findViewById(R.id.kpi_reports_search);
        reportsSearchClear = findViewById(R.id.kpi_reports_search_clear);
        reportsEmpty = findViewById(R.id.kpi_reports_empty);
        reportsEmptyTitle = findViewById(R.id.kpi_reports_empty_title);
        reportsEmptySubtitle = findViewById(R.id.kpi_reports_empty_subtitle);
        reportsRecycler = findViewById(R.id.kpi_reports_recycler);
        reportsToggleBtn = findViewById(R.id.kpi_reports_toggle_btn);
        reportsToggleText = findViewById(R.id.kpi_reports_toggle_text);
        reportsToggleIcon = findViewById(R.id.kpi_reports_toggle_icon);

        reportsCenter = getIntent().getBooleanExtra(EXTRA_REPORTS_CENTER, false);
        showingAllReports = reportsCenter;
        reportAdapter = new AdminKpiReportAdapter(this::openReceivedReport);
        reportsRecycler.setLayoutManager(new LinearLayoutManager(this));
        reportsRecycler.setItemAnimator(null);
        reportsRecycler.setAdapter(reportAdapter);

        findViewById(R.id.kpi_official_back).setOnClickListener(v -> finish());
        findViewById(R.id.kpi_official_refresh).setOnClickListener(v -> {
            if (refreshIcon != null) {
                refreshIcon.animate().rotationBy(360f).setDuration(420L).start();
            }
            refresh();
        });
        findViewById(R.id.kpi_feedback_dismiss).setOnClickListener(v -> feedbackBanner.setVisibility(View.GONE));

        findViewById(R.id.kpi_card_configure).setOnClickListener(v -> newDefinition());
        findViewById(R.id.kpi_card_worker_sheets).setOnClickListener(v -> {
            Intent intent = new Intent(this, pe.net.ws.kpiweb.features.admin.personal.AdminPersonalActivity.class);
            intent.putExtra(pe.net.ws.kpiweb.features.admin.personal.AdminPersonalActivity.EXTRA_OPEN_REPORTS, true);
            startActivity(intent);
        });

        findViewById(R.id.kpi_action_approve_policy).setOnClickListener(v -> newPolicy());
        findViewById(R.id.kpi_action_close_month).setOnClickListener(v -> closeMonth());

        reportsToggleBtn.setOnClickListener(v -> {
            showingAllReports = !showingAllReports;
            renderReports();
        });

        setupReportSearch();
        if (reportsCenter) {
            TextView heading = findViewById(R.id.kpi_official_heading);
            heading.setText("Reportes recibidos");
            ((TextView) findViewById(R.id.kpi_official_header_subtitle))
                    .setText("Consulta el estado y abre cada formulario");
            ((android.widget.ImageView) findViewById(R.id.kpi_official_header_icon))
                    .setImageResource(R.drawable.admin_ic_reports);
            findViewById(R.id.kpi_official_header_card)
                    .setBackgroundResource(R.drawable.admin_bg_kpi_reports_header);
            ((TextView)findViewById(R.id.kpi_reports_section_title)).setText("Formularios enviados por el equipo");
            int[] managementSections = { R.id.kpi_period_card, R.id.kpi_admin_section_title,
                    R.id.kpi_admin_cards, R.id.kpi_definitions_empty, R.id.kpi_definitions_list,
                    R.id.kpi_closure_heading, R.id.kpi_action_approve_policy,
                    R.id.kpi_action_close_month, R.id.kpi_closure_info_card };
            for (int sectionId : managementSections) findViewById(sectionId).setVisibility(View.GONE);
        }
        AdminTypography.apply(this, findViewById(android.R.id.content));
        refresh();
    }

    @Override
    protected void onStop() {
        cancelCalls();
        super.onStop();
    }

    private void cancelCalls() {
        if (definitionsCall != null) definitionsCall.cancel();
        if (reportsCall != null) reportsCall.cancel();
        if (periodsCall != null) periodsCall.cancel();
        if (closeCall != null) closeCall.cancel();
        definitionsCall = null;
        reportsCall = null;
        periodsCall = null;
        closeCall = null;
        closingMonth = false;
        if (closeConfirmDialog != null && closeConfirmDialog.isShowing()) {
            closeConfirmDialog.dismiss();
        }
    }

    private void setupReportSearch() {
        reportsSearchClear.setOnClickListener(v -> reportsSearch.setText(""));
        reportsSearch.setOnFocusChangeListener((v, hasFocus) -> {
            reportsSearchContainer.setActivated(hasFocus);
            ImageViewCompat.setImageTintList(reportsSearchIcon, ColorStateList.valueOf(
                    getColor(hasFocus ? R.color.admin_purple_accent : R.color.admin_text_muted)));
        });
        reportsSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasQuery = s != null && s.length() > 0;
                reportsSearchClear.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                renderReports();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private boolean sameAccount() {
        SessionManager current = new SessionManager(this);
        return current.isLoggedIn() && current.getUserId() == user && token.equals(current.getBearerToken());
    }

    private void showFeedback(String message) {
        feedbackText.setText(message);
        feedbackBanner.setVisibility(View.VISIBLE);
    }

    private void refresh() {
        cancelCalls();
        if (!reportsCenter) {
            loadPeriodState();
            definitionsCall = ApiClient.getService().getOfficialKpiDefinitions(token);
            run(definitionsCall, data -> {
                definitionsCall = null;
                renderDefinitions(data.getAsJsonArray("definiciones"));
            });
        }

        reportsCall = ApiClient.getService().getOfficialKpiReports(token);
        run(reportsCall, data -> {
            reportsCall = null;
            allReports.clear();
            JsonArray list = data.getAsJsonArray("reportes");
            if (list != null) {
                for (JsonElement el : list) {
                    if (el.isJsonObject()) allReports.add(el.getAsJsonObject());
                }
            }
            reportsLoaded = true;
            renderReports();
        });
    }

    private void loadPeriodState() {
        periodLoading.setVisibility(View.VISIBLE);
        periodBadge.setVisibility(View.GONE);
        if (closeCardStatus != null) closeCardStatus.setVisibility(View.GONE);
        periodValue.setText("Cargando…");
        periodDetail.setText("Consultando información del período…");

        if (!sameAccount()) {
            periodLoading.setVisibility(View.GONE);
            periodValue.setText("Sin datos");
            periodDetail.setText("La cuenta cambió. Vuelve a iniciar sesión.");
            return;
        }

        periodsCall = ApiClient.getService().getRankingPeriods(token);
        periodsCall.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != periodsCall || call.isCanceled() || !sameAccount() || isFinishing()) return;
                periodsCall = null;
                periodLoading.setVisibility(View.GONE);

                ApiResponse<JsonObject> result = response.body();
                if (!response.isSuccessful() || result == null || !result.isSuccessful() || result.getData() == null) {
                    periodValue.setText("Sin datos");
                    periodBadge.setVisibility(View.GONE);
                    periodDetail.setText(response.code() == 403
                            ? "Sin permiso para consultar cierres de ranking."
                            : "No hay información de cierre disponible.");
                    return;
                }

                JsonArray periods = result.getData().getAsJsonArray("periodos");
                if (periods == null || periods.size() == 0 || !periods.get(0).isJsonObject()) {
                    periodValue.setText("Sin datos");
                    periodBadge.setText("Sin cierre");
                    periodBadge.setTextColor(getColor(R.color.admin_text_secondary));
                    periodBadge.setVisibility(View.VISIBLE);
                    periodDetail.setText("Aún no existen períodos mensuales cerrados en el servidor.");
                    return;
                }

                JsonObject latest = periods.get(0).getAsJsonObject();
                String key = string(latest, "periodo_clave");
                int participants = number(latest, "participantes");
                int approvedEvals = number(latest, "evaluaciones_aprobadas");

                periodValue.setText(AdminRankingPeriodFormatter.formatPeriodKey(key, "MENSUAL"));
                periodBadge.setText("Cerrado");
                periodBadge.setTextColor(getColor(R.color.admin_status_delivered));
                periodBadge.setVisibility(View.VISIBLE);
                if (closeCardStatus != null) {
                    closeCardStatus.setText("Cerrado");
                    closeCardStatus.setVisibility(View.VISIBLE);
                }

                StringBuilder detail = new StringBuilder("Último cierre oficial");
                if (!key.isEmpty()) detail.append(" · ").append(key);
                if (participants > 0) {
                    detail.append(" · ").append(participants)
                            .append(participants == 1 ? " participante" : " participantes");
                }
                if (approvedEvals > 0) {
                    detail.append(" · ").append(approvedEvals)
                            .append(approvedEvals == 1 ? " evaluación" : " evaluaciones");
                }
                periodDetail.setText(detail.toString());
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call != periodsCall || call.isCanceled() || !sameAccount() || isFinishing()) return;
                periodsCall = null;
                periodLoading.setVisibility(View.GONE);
                periodValue.setText("Sin datos");
                periodBadge.setVisibility(View.GONE);
                periodDetail.setText("Sin conexión para consultar el estado del período.");
            }
        });
    }

    private void renderDefinitions(@Nullable JsonArray list) {
        definitionsList.removeAllViews();
        if (list == null || list.size() == 0) {
            definitionsEmpty.setVisibility(View.VISIBLE);
            return;
        }
        definitionsEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isJsonObject()) continue;
            JsonObject item = list.get(i).getAsJsonObject();
            int id = number(item, "id_kpi");
            String name = string(item, "nombre");
            String code = string(item, "codigo");
            int cargoId = number(item, "id_cargo_objetivo");
            String label = name + " · " + code + " · cargo " + cargoId;
            boolean pendingApproval = item.get("regla_aprobada_en") == null
                    || item.get("regla_aprobada_en").isJsonNull();

            View card = inflater.inflate(R.layout.admin_item_kpi_definition, definitionsList, false);
            TextView titleView = card.findViewById(R.id.kpi_def_title);
            TextView badgeView = card.findViewById(R.id.kpi_def_badge);
            TextView metaView = card.findViewById(R.id.kpi_def_meta);
            TextView actionView = card.findViewById(R.id.kpi_def_action);

            titleView.setText(name.isEmpty() ? "Indicador #" + id : name);
            metaView.setText((code.isEmpty() ? "KPI #" + id : code) + " · Cargo " + cargoId);

            if (pendingApproval) {
                badgeView.setText("Pendiente");
                badgeView.setTextColor(getColor(R.color.admin_status_pending));
                badgeView.setBackgroundResource(R.drawable.admin_bg_kpi_badge_pending);
                actionView.setText("Aprobar");
                actionView.setOnClickListener(v -> confirm(
                        "Aprobar indicador",
                        label,
                        () -> run(ApiClient.getService().approveOfficialKpiDefinition(token, id, new JsonObject()),
                                data -> {
                                    showFeedback("Indicador aprobado correctamente.");
                                    refresh();
                                })));
            } else {
                badgeView.setText("Aprobado");
                badgeView.setTextColor(getColor(R.color.admin_status_delivered));
                badgeView.setBackgroundResource(R.drawable.admin_bg_kpi_badge_approved);
                badgeView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.admin_ic_check, 0, 0, 0);
                badgeView.setCompoundDrawablePadding(4);
                android.graphics.drawable.Drawable check = badgeView.getCompoundDrawables()[0];
                if (check != null) check.setTint(getColor(R.color.admin_status_delivered));
                actionView.setVisibility(View.GONE);
            }

            AdminTypography.apply(this, card);
            definitionsList.addView(card);
        }
    }

    private void renderReports() {
        String query = reportsSearch != null ? reportsSearch.getText().toString().trim() : "";
        List<JsonObject> filtered = new ArrayList<>();
        for (JsonObject item : allReports) {
            if (AdminKpiOfficialFormatter.matchesReportQuery(item, query)) {
                filtered.add(item);
            }
        }

        if (query.isEmpty()) {
            reportsCount.setText(String.valueOf(allReports.size()));
        } else {
            reportsCount.setText(filtered.size() + " de " + allReports.size());
        }

        if (filtered.isEmpty()) {
            reportAdapter.submitList(new ArrayList<>());
            reportsRecycler.setVisibility(View.GONE);
            reportsToggleBtn.setVisibility(View.GONE);
            if (reportsLoaded) {
                reportsEmpty.setVisibility(View.VISIBLE);
                if (allReports.isEmpty()) {
                    reportsEmptyTitle.setText("Sin reportes disponibles");
                    reportsEmptySubtitle.setText("No hay formularios recibidos para revisar.");
                } else {
                    reportsEmptyTitle.setText("Sin coincidencias");
                    reportsEmptySubtitle.setText("No se encontraron reportes que coincidan con \"" + query + "\".");
                }
            }
            return;
        }

        reportsEmpty.setVisibility(View.GONE);
        reportsRecycler.setVisibility(View.VISIBLE);

        boolean canToggle = !reportsCenter && filtered.size() > REPORT_PREVIEW_LIMIT;
        List<JsonObject> visible = canToggle && !showingAllReports
                ? new ArrayList<>(filtered.subList(0, REPORT_PREVIEW_LIMIT))
                : filtered;
        reportAdapter.submitList(visible);

        if (canToggle) {
            reportsToggleBtn.setVisibility(View.VISIBLE);
            reportsToggleText.setText(showingAllReports ? "Ver menos reportes" : "Ver todos los reportes");
            reportsToggleIcon.setRotation(showingAllReports ? 180f : 0f);
            reportsToggleBtn.setContentDescription(showingAllReports
                    ? "Ver menos reportes"
                    : "Ver todos los reportes (" + filtered.size() + ")");
        } else {
            reportsToggleBtn.setVisibility(View.GONE);
        }
    }

    private void newDefinition() {
        configureLauncher.launch(new Intent(this, AdminKpiConfigureActivity.class));
    }

    private void review(int report) {
        android.widget.Toast.makeText(this, "Calculando el reporte…", android.widget.Toast.LENGTH_SHORT).show();
        JsonObject body = new JsonObject();
        body.addProperty("observacion", "");
        run(ApiClient.getService().reviewOfficialKpiReport(token, report, body), data -> {
                        int id = number(data, "id_evaluacion");
                        refresh();
                        AlertDialog dialog = new AlertDialog.Builder(this)
                                .setTitle("Evaluación #" + id + " preparada")
                                .setMessage("Resultado del servidor: " + string(data, "valor_calculado") + " "
                                        + string(data, "unidad_medida") + " / meta exigible "
                                        + string(data, "meta_efectiva") + " = "
                                        + string(data, "porcentaje_estimado") + " %. Si apruebas su validez: "
                                        + string(data, "puntos_si_aprueba") + " puntos. Revisa la ficha antes de aprobar.")
                                .setNegativeButton("Después", null)
                                .setNeutralButton("Observar", (d, which) -> observeEvaluation(id))
                                .setPositiveButton("Aprobar", (d, which) -> approveEvaluation(id))
                                .create();
                        styleDialog(dialog);
                        dialog.show();
                    });
    }

    private void openReceivedReport(int reportId) {
        for (JsonObject report : allReports) {
            if (number(report, "id_detalle") == reportId) {
                int worker = number(report, "id_trabajador");
                String estimated = string(report, "porcentaje_estimado");
                String percent = estimated.isEmpty()
                        ? string(report, "porcentaje_cumplimiento") : estimated;
                if (worker > 0) pe.net.ws.kpiweb.features.kpi.ui.KpiServerReportBrowser
                        .show(this, worker, reportId, percent, () -> decideReport(report));
                return;
            }
        }
    }

    private void decideReport(JsonObject report) {
        int evaluation = number(report, "id_evaluacion");
        if (evaluation <= 0) {
            review(number(report, "id_detalle"));
            return;
        }
        String state = string(report, "estado_evaluacion");
        if (!"Entregado".equals(state)) {
            new AlertDialog.Builder(this).setTitle("Evaluación #" + evaluation)
                    .setMessage("Estado: " + state + " · Resultado: "
                            + string(report, "porcentaje_cumplimiento") + " % · Puntos: "
                            + string(report, "puntos_otorgados"))
                    .setPositiveButton("Cerrar", null).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Decidir evaluación #" + evaluation)
                .setMessage("Ya se calculó este reporte. Aprueba si los datos son válidos; observa si requieren corrección.")
                .setNegativeButton("Después", null)
                .setNeutralButton("Observar", (d, which) -> observeEvaluation(evaluation))
                .setPositiveButton("Aprobar", (d, which) -> approveEvaluation(evaluation)).show();
    }

    private void observeEvaluation(int id) {
        form("Observar evaluación #" + id, new String[]{"Motivo para corregir el reporte"}, fields -> {
            String reason = fields[0].getText().toString().trim();
            if (reason.isEmpty()) {
                showFeedback("Escribe el motivo antes de observar el reporte.");
                return;
            }
            JsonObject body = new JsonObject();
            body.addProperty("estado", "Observado");
            body.addProperty("observaciones_supervisor", reason);
            confirm("Observar reporte", "Esta evaluación no acreditará puntos.",
                    () -> run(ApiClient.getService().approveOfficialKpiEvaluation(token, id, body), data -> {
                        showFeedback("Evaluación #" + id + " observada sin puntos.");
                        refresh();
                    }));
        });
    }

    private void approveEvaluation(int id) {
        form("Aprobar evaluación #" + id, new String[]{"Observaciones del supervisor"}, fields -> {
            JsonObject body = new JsonObject();
            body.addProperty("estado", "Aprobado");
            body.addProperty("observaciones_supervisor", fields[0].getText().toString().trim());
            confirm("Confirmar aprobación",
                    "La fórmula del indicador aprobado calculará el resultado y los puntos de esta evaluación.",
                    () -> run(ApiClient.getService().approveOfficialKpiEvaluation(token, id, body), data -> {
                        showFeedback("Evaluación #" + id + " aprobada.");
                        refresh();
                    }));
        });
    }

    private void newPolicy() {
        form("Aprobar comparación mensual",
                new String[]{"Mes YYYY-MM", "Justificación empresarial de comparación entre cargos"},
                fields -> {
                    JsonObject body = new JsonObject();
                    body.addProperty("periodo_clave", fields[0].getText().toString().trim());
                    body.addProperty("formula", "PROMEDIO_PONDERADO");
                    body.addProperty("comparacion_cargos_aprobada", true);
                    body.addProperty("justificacion", fields[1].getText().toString().trim());
                    confirm("Aprobar fórmula",
                            "Porcentaje = promedio de cumplimientos aprobados, ponderado por peso. Empates: puntos confirmados y luego ID. Confirma solo si WEBSITE aprobó comparar así los cargos.",
                            () -> run(ApiClient.getService().approveRankingPolicy(token, body),
                                    data -> showFeedback("Regla mensual aprobada en el servidor.")));
                });
    }

    private void closeMonth() {
        if (closingMonth) return;
        form("Cerrar ranking mensual", new String[]{"Mes concluido YYYY-MM"}, fields -> {
            String period = fields[0].getText().toString().trim();
            showCloseConfirmModal(period);
        });
    }

    private void showCloseConfirmModal(String period) {
        if (closeConfirmDialog != null && closeConfirmDialog.isShowing()) {
            closeConfirmDialog.dismiss();
        }
        View modalView = LayoutInflater.from(this).inflate(R.layout.admin_dialog_kpi_close_confirm, null);
        TextView periodChip = modalView.findViewById(R.id.kpi_close_modal_period);
        View cancelBtn = modalView.findViewById(R.id.kpi_close_modal_cancel);
        View confirmBtn = modalView.findViewById(R.id.kpi_close_modal_confirm);
        ProgressBar confirmProgress = modalView.findViewById(R.id.kpi_close_modal_progress);
        TextView confirmText = modalView.findViewById(R.id.kpi_close_modal_confirm_text);

        String formatted = AdminRankingPeriodFormatter.formatPeriodKey(period, "MENSUAL");
        periodChip.setText(formatted.equals(period) ? "Período " + period : formatted + " · " + period);
        AdminTypography.apply(this, modalView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(modalView)
                .setCancelable(true)
                .create();
        closeConfirmDialog = dialog;

        cancelBtn.setOnClickListener(v -> {
            if (!closingMonth) dialog.dismiss();
        });
        confirmBtn.setOnClickListener(v -> {
            if (closingMonth) return;
            if (!sameAccount()) {
                dialog.dismiss();
                showFeedback("La cuenta cambió. Vuelve a iniciar sesión.");
                return;
            }
            closingMonth = true;
            dialog.setCancelable(false);
            cancelBtn.setEnabled(false);
            cancelBtn.setAlpha(0.5f);
            confirmBtn.setEnabled(false);
            confirmProgress.setVisibility(View.VISIBLE);
            confirmText.setText("Cerrando…");

            JsonObject body = new JsonObject();
            body.addProperty("periodo_clave", period);
            closeCall = ApiClient.getService().closeRankingMonth(token, body);
            closeCall.enqueue(new Callback<ApiResponse<JsonObject>>() {
                @Override
                public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                    if (response.errorBody() != null) response.errorBody().close();
                    if (call != closeCall || call.isCanceled() || isFinishing()) return;
                    closeCall = null;
                    closingMonth = false;
                    if (dialog.isShowing()) dialog.dismiss();

                    ApiResponse<JsonObject> result = response.body();
                    if (response.isSuccessful() && result != null && result.isSuccessful() && result.getData() != null) {
                        showFeedback("Cierre mensual confirmado: " + number(result.getData(), "participantes") + " participantes.");
                        refresh();
                    } else {
                        String msg = result != null && result.getMessage() != null && !result.getMessage().trim().isEmpty()
                                ? result.getMessage().trim()
                                : (response.code() == 403 ? "Tu cuenta no tiene permiso para cerrar el mes."
                                : response.code() == 401 ? "Sesión vencida."
                                : "No se completó el cierre mensual (HTTP " + response.code() + "). Revisa las validaciones e intenta nuevamente.");
                        showFeedback(msg);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                    if (call != closeCall || call.isCanceled() || isFinishing()) return;
                    closeCall = null;
                    closingMonth = false;
                    if (dialog.isShowing()) dialog.dismiss();
                    showFeedback("Sin conexión. Verifica el estado del período antes de reintentar el cierre.");
                }
            });
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private interface Submit {
        void accept(EditText[] fields);
    }

    private void form(String title, String[] labels, Submit submit) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(getColor(R.color.admin_bg_drawer));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(12));
        scroll.addView(box);

        EditText[] fields = new EditText[labels.length];
        for (int i = 0; i < labels.length; i++) {
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextSize(12.5f);
            label.setTextColor(getColor(R.color.admin_text_secondary));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = i == 0 ? dp(4) : dp(12);
            lp.bottomMargin = dp(4);
            box.addView(label, lp);

            EditText field = new EditText(this);
            field.setTextColor(getColor(R.color.admin_text_primary));
            field.setHintTextColor(getColor(R.color.admin_text_muted));
            field.setTextSize(14f);
            field.setBackgroundResource(R.drawable.admin_bg_ranking_search);
            field.setPadding(dp(12), dp(10), dp(12), dp(10));
            field.setMinHeight(dp(44));
            field.setSingleLine(i != 2 && i != labels.length - 1);
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            box.addView(field, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            fields[i] = field;
        }
        AdminTypography.apply(this, box);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setOnShowListener(v -> {
            applyDialogColors(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(click -> {
                for (int i = 0; i < fields.length; i++) {
                    if (!labels[i].contains("(opcional)") && fields[i].getText().toString().trim().isEmpty()) {
                        fields[i].setError("Obligatorio");
                        return;
                    }
                }
                dialog.dismiss();
                submit.accept(fields);
            });
        });
        dialog.show();
    }

    private void confirm(String title, String message, Runnable action) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", (d, which) -> action.run())
                .create();
        styleDialog(dialog);
        dialog.show();
    }

    private void styleDialog(AlertDialog dialog) {
        dialog.setOnShowListener(d -> applyDialogColors(dialog));
    }

    private void applyDialogColors(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.admin_bg_drawer);
        }
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(getColor(R.color.admin_text_primary));
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextColor(getColor(R.color.admin_text_secondary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.admin_purple_accent));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.admin_text_secondary));
        }
    }

    private interface Done {
        void accept(JsonObject data);
    }

    private void run(Call<ApiResponse<JsonObject>> call, Done done) {
        if (!sameAccount()) {
            showFeedback("La cuenta cambió. Vuelve a iniciar sesión.");
            return;
        }
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> request, Response<ApiResponse<JsonObject>> response) {
                String serverError = null;
                if (response.errorBody() != null) {
                    try {
                        JsonObject error = com.google.gson.JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
                        if (error.has("message") && !error.get("message").isJsonNull())
                            serverError = error.get("message").getAsString();
                    } catch (Exception ignored) { /* The HTTP status remains available below. */ }
                }
                if (request.isCanceled() || !sameAccount() || isFinishing()) return;
                ApiResponse<JsonObject> result = response.body();
                if (response.isSuccessful() && result != null && result.isSuccessful() && result.getData() != null) {
                    done.accept(result.getData());
                } else {
                    String message = serverError != null && !serverError.trim().isEmpty() ? serverError
                            : response.code() == 403 ? "Tu cuenta no tiene permiso para esta operación."
                            : response.code() == 401 ? "Sesión vencida."
                            : "No se completó la operación (HTTP " + response.code() + "). Revisa los datos y vuelve a intentar.";
                    showFeedback(message);
                    new AlertDialog.Builder(AdminKpiOfficialActivity.this)
                            .setTitle("No se pudo calcular el reporte")
                            .setMessage(message)
                            .setPositiveButton("Entendido", null).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> request, Throwable error) {
                if (!request.isCanceled() && sameAccount() && !isFinishing()) {
                    showFeedback("Sin conexión. Vuelve a intentar sin repetir una aprobación hasta verificar su estado.");
                }
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String string(JsonObject row, String key) {
        return AdminKpiOfficialFormatter.safeString(row, key);
    }

    private static int number(JsonObject row, String key) {
        return AdminKpiOfficialFormatter.safeInt(row, key);
    }
}
