package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminMenuFormatter.PersonalEvalSummary;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignment;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignments;
import pe.net.ws.kpiweb.features.kpi.ui.KpiServerReportBrowser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla "Mis evaluaciones" dentro de "Mi espacio" del Administrador.
 * Consulta exclusivamente las evaluaciones personales del trabajador autenticado (GET /kpi/mis-kpis)
 * mostrando su puntuación individual de desempeño sin ranking ni puntos canjeables.
 */
public final class AdminMyEvaluationsActivity extends AppCompatActivity {

    private TextView scoreValue;
    private TextView statTotal;
    private TextView statApproved;
    private TextView statPending;
    private TextView periodLabel;
    private View statusCard;
    private View loadingSpinner;
    private TextView statusTitle;
    private TextView statusDetail;
    private View btnRetry;
    private LinearLayout listContainer;
    private ImageView refreshIcon;

    private Call<ApiResponse<KpiServerAssignments>> pendingCall;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_my_evaluations);
        getWindow().setStatusBarColor(getColor(R.color.admin_bg_main));
        getWindow().setNavigationBarColor(getColor(R.color.admin_bg_main));

        scoreValue = findViewById(R.id.my_evals_score_value);
        statTotal = findViewById(R.id.my_evals_stat_total);
        statApproved = findViewById(R.id.my_evals_stat_approved);
        statPending = findViewById(R.id.my_evals_stat_pending);
        periodLabel = findViewById(R.id.my_evals_period_label);
        statusCard = findViewById(R.id.my_evals_status_card);
        loadingSpinner = findViewById(R.id.my_evals_loading);
        statusTitle = findViewById(R.id.my_evals_status_title);
        statusDetail = findViewById(R.id.my_evals_status_detail);
        btnRetry = findViewById(R.id.my_evals_btn_retry);
        listContainer = findViewById(R.id.my_evals_list);
        refreshIcon = findViewById(R.id.my_evals_refresh_icon);

        findViewById(R.id.my_evals_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.my_evals_btn_refresh).setOnClickListener(v -> {
            if (refreshIcon != null) {
                refreshIcon.animate().rotationBy(360f).setDuration(360L).start();
            }
            loadEvaluations();
        });
        btnRetry.setOnClickListener(v -> loadEvaluations());

        findViewById(R.id.my_evals_btn_open_kpi).setOnClickListener(v -> {
            Intent intent = new Intent(this, CollaboratorKpiIntegrationActivity.class);
            intent.putExtra(CollaboratorKpiIntegrationActivity.EXTRA_RETURN_ADMIN, true);
            startActivity(intent);
        });

        findViewById(R.id.my_evals_btn_my_reports).setOnClickListener(v -> {
            SessionManager session = new SessionManager(this);
            if (!session.isLoggedIn() || session.getWorkerId() <= 0) {
                Toast.makeText(this, "No hay un trabajador vinculado en esta sesión.", Toast.LENGTH_SHORT).show();
                return;
            }
            KpiServerReportBrowser.show(this, session.getWorkerId());
        });

        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvaluations();
    }

    @Override
    protected void onStop() {
        if (pendingCall != null) pendingCall.cancel();
        pendingCall = null;
        super.onStop();
    }

    private void loadEvaluations() {
        if (pendingCall != null) pendingCall.cancel();
        listContainer.removeAllViews();
        showStatus(true, "Consultando tus evaluaciones…",
                "Obteniendo los resultados personales registrados en el servidor.", false);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) {
            bindSummary(new PersonalEvalSummary(0, 0, 0, "—", false));
            showStatus(false, "Trabajador no vinculado",
                    "Tu cuenta necesita un perfil de trabajador vinculado para consultar evaluaciones personales.", false);
            return;
        }

        final String expectedToken = session.getAccessToken();
        final int expectedWorker = session.getWorkerId();

        pendingCall = ApiClient.getService().getMisKpis(session.getBearerToken(), "");
        pendingCall.enqueue(new Callback<ApiResponse<KpiServerAssignments>>() {
            @Override
            public void onResponse(Call<ApiResponse<KpiServerAssignments>> call,
                                   Response<ApiResponse<KpiServerAssignments>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != pendingCall || call.isCanceled() || isFinishing() || isDestroyed()) return;
                pendingCall = null;

                SessionManager current = new SessionManager(AdminMyEvaluationsActivity.this);
                if (!current.isLoggedIn() || current.getWorkerId() != expectedWorker
                        || !expectedToken.equals(current.getAccessToken())) {
                    return;
                }

                if (response.code() == 401) {
                    bindSummary(new PersonalEvalSummary(0, 0, 0, "—", false));
                    showStatus(false, "Sesión vencida", "Vuelve a iniciar sesión para ver tus evaluaciones.", false);
                    return;
                }
                if (response.code() == 403) {
                    bindSummary(new PersonalEvalSummary(0, 0, 0, "—", false));
                    showStatus(false, "Sin permiso", "Tu cuenta no tiene permiso kpi.ver para consultar evaluaciones.", false);
                    return;
                }

                ApiResponse<KpiServerAssignments> envelope = response.body();
                KpiServerAssignments data = envelope == null ? null : envelope.getData();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful() || data == null) {
                    bindSummary(new PersonalEvalSummary(0, 0, 0, "—", false));
                    showStatus(false, "No se pudieron cargar",
                            "El servidor no devolvió tus evaluaciones (HTTP " + response.code() + ").", true);
                    return;
                }

                List<KpiServerAssignment> rows = data.getAssignments();
                PersonalEvalSummary summary = AdminMenuFormatter.computePersonalEvaluationSummary(rows);
                bindSummary(summary);
                renderEvaluations(rows);
            }

            @Override
            public void onFailure(Call<ApiResponse<KpiServerAssignments>> call, Throwable error) {
                if (call != pendingCall || call.isCanceled() || isFinishing() || isDestroyed()) return;
                pendingCall = null;
                bindSummary(new PersonalEvalSummary(0, 0, 0, "—", false));
                showStatus(false, "Sin conexión",
                        "No se pudo conectar con el servidor para consultar tus evaluaciones.", true);
            }
        });
    }

    private void bindSummary(PersonalEvalSummary summary) {
        scoreValue.setText(summary.scoreDisplay);
        statTotal.setText(String.valueOf(summary.total));
        statApproved.setText(String.valueOf(summary.approved));
        statPending.setText(String.valueOf(summary.pending));
        periodLabel.setText(summary.total > 0
                ? "Evaluación individual · " + summary.total + (summary.total == 1 ? " indicador" : " indicadores")
                : "Evaluación personal de desempeño");
    }

    private void renderEvaluations(List<KpiServerAssignment> rows) {
        listContainer.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            showStatus(false, "Aún no tienes evaluaciones registradas",
                    "Cuando se asignen o aprueben indicadores personales para tu cuenta, aparecerán aquí. Puedes registrar avances en Mis KPI.",
                    false);
            return;
        }
        statusCard.setVisibility(View.GONE);

        for (KpiServerAssignment item : rows) {
            if (item == null) continue;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.personal_bg_card_premium);
            card.setPadding(dp(15), dp(14), dp(15), dp(14));

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(10);
            card.setLayoutParams(cardLp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView title = new TextView(this);
            String name = item.getName() != null && !item.getName().trim().isEmpty()
                    ? item.getName().trim() : "Indicador #" + item.getKpiId();
            title.setText(name);
            title.setTextColor(getColor(R.color.admin_text_primary));
            title.setTextSize(14.5f);
            title.setTypeface(null, Typeface.BOLD);
            headerRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView badge = new TextView(this);
            String status = item.getStatus() != null && !item.getStatus().trim().isEmpty()
                    ? item.getStatus().trim() : "Sin estado";
            badge.setText(status);
            badge.setTextSize(11f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(dp(9), dp(3), dp(9), dp(3));
            styleStatusBadge(badge, status);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.setMarginStart(dp(8));
            headerRow.addView(badge, badgeLp);
            card.addView(headerRow);

            StringBuilder meta = new StringBuilder();
            if (item.getKpiCode() != null && !item.getKpiCode().trim().isEmpty()) {
                meta.append(item.getKpiCode().trim());
            }
            if (item.getPeriodKey() != null && !item.getPeriodKey().trim().isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(item.getPeriodKey().trim());
            }
            if (item.getCategory() != null && !item.getCategory().trim().isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(item.getCategory().trim());
            }
            if (meta.length() > 0) {
                TextView tvMeta = new TextView(this);
                tvMeta.setText(meta.toString());
                tvMeta.setTextColor(getColor(R.color.admin_text_secondary));
                tvMeta.setTextSize(11.5f);
                LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                metaLp.topMargin = dp(4);
                card.addView(tvMeta, metaLp);
            }

            String unit = item.getUnit() != null ? item.getUnit().trim() : "";
            String reported = item.getReportedValue() != null
                    ? formatNumber(item.getReportedValue()) + (unit.isEmpty() ? "" : " " + unit)
                    : "Sin registro";
            String target = formatNumber(item.getTarget()) + (unit.isEmpty() ? "" : " " + unit);
            String compliance = item.getCompliance() != null
                    ? String.format(Locale.US, "%.1f%% cumplimiento", item.getCompliance())
                    : "Pendiente de calificación";

            TextView tvResult = new TextView(this);
            tvResult.setText("Avance: " + reported + "  ·  Meta: " + target + "\n" + compliance);
            tvResult.setTextColor(getColor(R.color.admin_text_primary));
            tvResult.setTextSize(12.5f);
            tvResult.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            resLp.topMargin = dp(8);
            card.addView(tvResult, resLp);

            listContainer.addView(card);
        }
        AdminTypography.apply(this, listContainer);
    }

    private void styleStatusBadge(TextView badge, String rawStatus) {
        String s = rawStatus.toLowerCase(Locale.ROOT);
        if (s.contains("aprobado")) {
            badge.setBackgroundResource(R.drawable.personal_bg_status_active_pill);
            badge.setTextColor(getColor(R.color.admin_status_delivered));
        } else if (s.contains("vencido")) {
            badge.setBackgroundResource(R.drawable.personal_bg_status_inactive_pill);
            badge.setTextColor(getColor(R.color.admin_status_overdue));
        } else if (s.contains("entregado") || s.contains("progreso")) {
            badge.setBackgroundResource(R.drawable.personal_bg_status_leave_pill);
            badge.setTextColor(getColor(R.color.admin_cyan_brain));
        } else {
            badge.setBackgroundResource(R.drawable.personal_bg_chip_unselected_premium);
            badge.setTextColor(getColor(R.color.admin_status_pending));
        }
    }

    private void showStatus(boolean loading, String title, String detail, boolean canRetry) {
        statusCard.setVisibility(View.VISIBLE);
        loadingSpinner.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusTitle.setText(title);
        statusDetail.setText(detail);
        btnRetry.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private static String formatNumber(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
