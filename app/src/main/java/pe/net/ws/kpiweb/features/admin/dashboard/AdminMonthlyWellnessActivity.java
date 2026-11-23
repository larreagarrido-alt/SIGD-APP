package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ExecutionStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.TrendPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Módulo A — Evaluación mensual (Administrador — SIGD).
 * Muestra participación agregada, resultados del período y evolución mensual respetando
 * los filtros reales (período, agencia y campaña) sin exponer respuestas psicológicas individuales.
 */
public final class AdminMonthlyWellnessActivity extends AppCompatActivity {

    private final Calendar month = Calendar.getInstance();
    private final List<Call<ApiResponse<JsonObject>>> activeCalls = new ArrayList<>();

    private TextView statusBanner;
    private TextView tvPeriod;
    private TextView tvAgency;
    private TextView tvCampaign;
    private View agencyFilterChip;
    private View campaignFilterChip;
    private TextView statEligible;
    private TextView statCompleted;
    private TextView statParticipation;
    private TextView statParticipationNote;
    private CircularProgressIndicator participationRing;
    private LinearLayout resultsContainer;
    private LinearLayout trendContainer;
    private LinearLayout plansContainer;
    private LinearLayout assistantQuickQuestions;
    private ImageView refreshIcon;

    private int selectedAgencyId;
    private long selectedCampaignId;
    private JsonArray agencyRows = new JsonArray();
    private JsonArray campaignRows = new JsonArray();

    private String token;
    private int userId;

    private final ActivityResultLauncher<Intent> activityFormLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        String msg = result.getData().getStringExtra(
                                AdminWellnessActivityFormActivity.EXTRA_RESULT_MESSAGE);
                        if (msg != null && !msg.trim().isEmpty()) {
                            showStatus(msg, false);
                        }
                    }
                    refresh();
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_wellness_monthly);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        bindViews();
        bindEvents();
        AdminTypography.apply(this, findViewById(android.R.id.content));
        refresh();
    }

    private void bindViews() {
        statusBanner = findViewById(R.id.monthly_status_banner);
        tvPeriod = findViewById(R.id.monthly_tv_period);
        tvAgency = findViewById(R.id.monthly_tv_agency);
        tvCampaign = findViewById(R.id.monthly_tv_campaign);
        agencyFilterChip = findViewById(R.id.monthly_filter_agency);
        campaignFilterChip = findViewById(R.id.monthly_filter_campaign);
        statEligible = findViewById(R.id.monthly_stat_eligible);
        statCompleted = findViewById(R.id.monthly_stat_completed);
        statParticipation = findViewById(R.id.monthly_stat_participation);
        statParticipationNote = findViewById(R.id.monthly_stat_participation_note);
        participationRing = findViewById(R.id.monthly_participation_ring);
        resultsContainer = findViewById(R.id.monthly_results_container);
        trendContainer = findViewById(R.id.monthly_trend_container);
        plansContainer = findViewById(R.id.monthly_plans_container);
        assistantQuickQuestions = findViewById(R.id.monthly_assistant_quick_questions);
        refreshIcon = findViewById(R.id.monthly_refresh_icon);

        styleIconBadge(findViewById(R.id.monthly_badge_eligible), 0xFF382252, 0xFFD8B4FE);
        styleIconBadge(findViewById(R.id.monthly_badge_completed), 0xFF174637, 0xFF57D6A0);
        styleIconBadge(findViewById(R.id.monthly_badge_participation), 0xFF1B3554, 0xFF8CCBFF);
        renderAssistantQuickQuestions();
    }

    private void styleIconBadge(@Nullable View target, int bgColor, int accentColor) {
        if (target == null) return;
        float d = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(11f * d);
        bg.setColor(bgColor);
        bg.setStroke(Math.max(1, Math.round(d)), (accentColor & 0x00FFFFFF) | 0x45000000);
        target.setBackground(bg);
    }

    private void bindEvents() {
        findViewById(R.id.monthly_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.monthly_btn_refresh).setOnClickListener(v -> {
            if (refreshIcon != null && !isReduceMotionEnabled()) {
                refreshIcon.animate().rotationBy(360f).setDuration(420L).start();
            }
            refresh();
        });
        findViewById(R.id.monthly_btn_prev_month).setOnClickListener(v -> shiftMonth(-1));
        findViewById(R.id.monthly_btn_next_month).setOnClickListener(v -> shiftMonth(1));
        View pickPeriodBtn = findViewById(R.id.monthly_btn_pick_period);
        if (pickPeriodBtn != null) pickPeriodBtn.setOnClickListener(v -> showMonthYearBottomSheet());
        agencyFilterChip.setOnClickListener(v -> chooseAgency());
        campaignFilterChip.setOnClickListener(v -> chooseCampaign());
        findViewById(R.id.monthly_btn_add_plan).setOnClickListener(v -> openPlanForm(null));
        findViewById(R.id.monthly_assistant_open_chat).setOnClickListener(v -> openAssistant(null));
        statusBanner.setOnClickListener(v -> refresh());
    }

    private void shiftMonth(int delta) {
        month.add(Calendar.MONTH, delta);
        selectedCampaignId = 0;
        tvCampaign.setText("Todas");
        syncFilterStates();
        refresh();
    }

    private String monthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(month.getTime());
    }

    private void syncFilterStates() {
        agencyFilterChip.setSelected(selectedAgencyId > 0);
        campaignFilterChip.setSelected(selectedCampaignId > 0);
    }

    private void refresh() {
        String key = monthKey();
        tvPeriod.setText(AdminWellnessHelper.formatMonthDisplay(key));
        syncFilterStates();
        showStatus("Consultando datos del período " + AdminWellnessHelper.formatMonthDisplay(key) + "…", false);

        request(ApiClient.getService().getAdminWellnessOverview(token, key, selectedCampaignId, selectedAgencyId), data -> {
            hideStatus();
            agencyRows = array(data, "agencias");
            int eligible = intValue(data, "participantes_elegibles", -1);
            int completed = intValue(data, "cuestionarios_completados", -1);
            Double serverPct = nullableDouble(data, "participacion_porcentaje");

            statEligible.setText(eligible >= 0 ? String.valueOf(eligible) : "—");
            statCompleted.setText(completed >= 0 ? String.valueOf(completed) : "—");
            String pctText = AdminWellnessHelper.formatParticipationPercent(serverPct, completed, eligible);
            statParticipation.setText(pctText);
            if ("—".equals(pctText)) {
                statParticipationNote.setText("Sin denominador válido o grupo insuficiente para el período");
                if (participationRing != null) {
                    participationRing.setVisibility(View.GONE);
                    participationRing.setProgressCompat(0, false);
                }
            } else {
                statParticipationNote.setText("Evaluaciones completadas sobre participantes elegibles");
                if (participationRing != null) {
                    double pctNum = serverPct != null
                            ? serverPct
                            : (eligible > 0 ? (completed * 100.0) / eligible : 0.0);
                    int clamped = (int) Math.max(0, Math.min(100, Math.round(pctNum)));
                    participationRing.setVisibility(View.VISIBLE);
                    participationRing.setProgressCompat(clamped, false);
                }
            }

            renderPeriodResults(data);
            renderMonthlyTrend(array(data, "tendencia_participacion"));
            AdminTypography.apply(this, findViewById(android.R.id.content));
        });

        request(ApiClient.getService().getAdminWellnessCampaigns(token), data ->
                campaignRows = array(data, "campanas"));

        request(ApiClient.getService().getAdminWellnessPlans(token), data -> {
            renderPlans(array(data, "planes"));
            AdminTypography.apply(this, plansContainer);
        });
    }

    private void renderPeriodResults(JsonObject data) {
        resultsContainer.removeAllViews();
        JsonArray agencyComparison = array(data, "comparacion_agencias");
        int completed = intValue(data, "cuestionarios_completados", 0);

        if (agencyComparison.size() == 0 && completed <= 0) {
            renderEmptyBlock(
                    resultsContainer,
                    R.drawable.admin_ic_calendar_heart,
                    "Aún no hay resultados para este período",
                    "Cuando existan participaciones válidas que cumplan el criterio de privacidad grupal, aparecerán aquí."
            );
            return;
        }

        if (agencyComparison.size() == 0) {
            renderEmptyBlock(
                    resultsContainer,
                    R.drawable.admin_ic_shield_check,
                    "Desglose grupal protegido",
                    "No hay agencias con al menos cinco participantes elegibles para mostrar comparaciones sin comprometer la privacidad."
            );
            return;
        }

        TextView subtitle = new TextView(this);
        subtitle.setText("Participación agregada por agencia (mínimo grupal autorizado)");
        subtitle.setTextColor(getColor(R.color.admin_text_secondary));
        subtitle.setTextSize(12f);
        resultsContainer.addView(subtitle);

        for (JsonElement element : agencyComparison) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String agencyName = stringValue(row, "agencia");
            Double pct = nullableDouble(row, "participacion_porcentaje");
            String formatted = AdminWellnessHelper.formatParticipationPercent(pct, -1, -1);
            addMetricBarRow(resultsContainer, agencyName, formatted,
                    pct != null ? pct.floatValue() : 0f, 100f, getColor(R.color.admin_purple_accent));
        }
    }

    private void renderMonthlyTrend(JsonArray trendRows) {
        trendContainer.removeAllViews();
        List<TrendPoint> points = new ArrayList<>();
        for (JsonElement element : trendRows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String m = stringValue(row, "mes");
            Double pct = nullableDouble(row, "participacion_porcentaje");
            points.add(new TrendPoint(m, pct != null, pct != null ? pct.floatValue() : 0f));
        }

        if (!AdminWellnessHelper.hasComparableTrendData(points)) {
            renderEmptyBlock(
                    trendContainer,
                    R.drawable.admin_ic_chart_line,
                    "Aún no hay información suficiente para visualizar tendencias",
                    "Se requieren al menos dos períodos mensuales con participación comparable para mostrar evolución histórica."
            );
            return;
        }

        TextView caption = new TextView(this);
        caption.setText("Participación mensual comparable · Toca una barra para ver el detalle");
        caption.setTextColor(getColor(R.color.admin_text_secondary));
        caption.setTextSize(12f);
        trendContainer.addView(caption);

        TextView trendCallout = new TextView(this);
        trendCallout.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
        trendCallout.setPadding(dp(11), dp(7), dp(11), dp(7));
        trendCallout.setTextColor(getColor(R.color.admin_purple_accent));
        trendCallout.setTextSize(11.5f);
        trendCallout.setVisibility(View.GONE);
        LinearLayout.LayoutParams calloutLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        calloutLp.topMargin = dp(8);
        trendContainer.addView(trendCallout, calloutLp);

        // Gráfico vertical de barras interactivo y legible
        LinearLayout chartRow = new LinearLayout(this);
        chartRow.setOrientation(LinearLayout.HORIZONTAL);
        chartRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(136));
        chartLp.topMargin = dp(12);
        trendContainer.addView(chartRow, chartLp);

        for (TrendPoint p : points) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            col.setClickable(true);
            col.setFocusable(true);
            col.setOnClickListener(v -> {
                trendCallout.setVisibility(View.VISIBLE);
                String monthDisplay = AdminWellnessHelper.formatMonthDisplay(p.monthKey);
                trendCallout.setText(p.valid
                        ? monthDisplay + " · " + Math.round(p.percentage) + " % de participación"
                        : monthDisplay + " · Grupo insuficiente por privacidad");
            });
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            colLp.setMarginStart(dp(3));
            colLp.setMarginEnd(dp(3));

            TextView val = new TextView(this);
            val.setText(p.valid ? Math.round(p.percentage) + "%" : "—");
            val.setTextColor(p.valid ? getColor(R.color.admin_purple_accent) : getColor(R.color.admin_text_muted));
            val.setTextSize(10.5f);
            val.setTypeface(null, Typeface.BOLD);
            col.addView(val);

            int maxBarHeight = dp(82);
            int barHeight = p.valid ? Math.max(dp(10), Math.round((Math.min(100f, Math.max(0f, p.percentage)) / 100f) * maxBarHeight)) : dp(8);
            View bar = new View(this);
            GradientDrawable barBg = new GradientDrawable();
            barBg.setCornerRadius(dp(6));
            barBg.setColor(p.valid ? getColor(R.color.admin_purple_primary) : getColor(R.color.admin_border));
            bar.setBackground(barBg);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(22), barHeight);
            barLp.topMargin = dp(4);
            col.addView(bar, barLp);

            TextView label = new TextView(this);
            label.setText(AdminWellnessHelper.formatMonthShort(p.monthKey));
            label.setTextColor(getColor(R.color.admin_text_secondary));
            label.setTextSize(10f);
            label.setMaxLines(1);
            LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lblLp.topMargin = dp(6);
            col.addView(label, lblLp);

            chartRow.addView(col, colLp);
        }
    }

    private void renderPlans(JsonArray rows) {
        plansContainer.removeAllViews();
        if (rows.size() == 0) {
            renderEmptyBlock(
                    plansContainer,
                    R.drawable.collab_ic_notif_heart_handshake,
                    "Aún no hay actividades de bienestar",
                    "Crea un taller, charla, pausa activa o acción de mejora con el botón + Nueva actividad."
            );
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            JsonElement el = rows.get(i);
            if (!el.isJsonObject()) continue;
            JsonObject item = el.getAsJsonObject();

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
            card.setPadding(dp(13), dp(12), dp(13), dp(12));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> openPlanForm(item));

            // Fila 1: Nombre + chevron editar
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView title = new TextView(this);
            title.setText(stringValue(item, "nombre"));
            title.setTextColor(getColor(R.color.admin_text_primary));
            title.setTextSize(14.5f);
            title.setTypeface(null, Typeface.BOLD);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            titleRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            ImageView chevron = new ImageView(this);
            chevron.setImageResource(R.drawable.admin_ic_chevron_right);
            chevron.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_text_muted)));
            titleRow.addView(chevron, new LinearLayout.LayoutParams(dp(16), dp(16)));
            card.addView(titleRow);

            // Objetivo resumido
            String objective = stringValue(item, "objetivo");
            if (!"—".equals(objective) && !objective.trim().isEmpty()) {
                TextView objTv = new TextView(this);
                objTv.setText(objective);
                objTv.setTextColor(getColor(R.color.admin_text_secondary));
                objTv.setTextSize(12f);
                objTv.setMaxLines(2);
                objTv.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                oLp.topMargin = dp(4);
                card.addView(objTv, oLp);
            }

            // Badges separados: Estado de ejecución + Estado de publicación
            ExecutionStatus execStatus = AdminWellnessActivityHelper.normalizeExecutionStatus(
                    stringValue(item, "estado"));
            boolean publishedFlag = item.has("publicada") && !item.get("publicada").isJsonNull()
                    && item.get("publicada").getAsBoolean();
            PublicationStatus pubStatus = AdminWellnessActivityHelper.normalizePublicationStatus(
                    stringValue(item, "estado_publicacion"), publishedFlag);

            LinearLayout badgesRow = new LinearLayout(this);
            badgesRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams bRowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bRowLp.topMargin = dp(8);
            badgesRow.addView(buildBadge(execStatus.displayName, executionBgColor(execStatus), executionTextColor(execStatus)));
            TextView pubBadge = buildBadge(pubStatus.displayName,
                    pubStatus == PublicationStatus.PUBLICADA ? getColor(R.color.admin_status_delivered_bg) : getColor(R.color.admin_status_assigned_bg),
                    pubStatus == PublicationStatus.PUBLICADA ? getColor(R.color.admin_status_delivered) : getColor(R.color.admin_purple_accent));
            LinearLayout.LayoutParams pubLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pubLp.setMarginStart(dp(6));
            badgesRow.addView(pubBadge, pubLp);
            card.addView(badgesRow, bRowLp);

            // Metadatos: Fecha/horario, Responsable, Área y Alcance/destinatarios
            String schedule = AdminWellnessActivityHelper.formatScheduleDisplay(
                    stringValue(item, "fecha_programada"),
                    stringValue(item, "hora_inicio"),
                    stringValue(item, "hora_fin"));
            String responsible = stringValue(item, "responsable");
            String category = AdminWellnessActivityHelper.formatCategoryLabel(stringValue(item, "dimension"));
            String scopeLabel = stringValue(item, "alcance_resumen");
            if ("—".equals(scopeLabel) || scopeLabel.trim().isEmpty()) {
                VisibilityScope scope = AdminWellnessActivityHelper.normalizeVisibilityScope(
                        stringValue(item, "visibilidad"));
                scopeLabel = pubStatus == PublicationStatus.PUBLICADA
                        ? scope.displayName : "Solo administración";
            }

            TextView meta = new TextView(this);
            meta.setText("Fecha: " + schedule + "\nResponsable: " + responsible
                    + " · " + category + "\nVisibilidad: " + scopeLabel);
            meta.setTextColor(getColor(R.color.admin_text_muted));
            meta.setTextSize(11.5f);
            meta.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            metaLp.topMargin = dp(7);
            card.addView(meta, metaLp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(10);
            plansContainer.addView(card, lp);
        }
    }

    private TextView buildBadge(String text, int bgColor, int txtColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(10.5f);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(txtColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(99));
        bg.setColor(bgColor);
        tv.setBackground(bg);
        tv.setPadding(dp(9), dp(3), dp(9), dp(3));
        return tv;
    }

    private int executionBgColor(ExecutionStatus status) {
        if (status == ExecutionStatus.COMPLETADA) return getColor(R.color.admin_status_delivered_bg);
        if (status == ExecutionStatus.EN_PROCESO) return getColor(R.color.admin_status_pending_bg);
        return getColor(R.color.admin_status_assigned_bg);
    }

    private int executionTextColor(ExecutionStatus status) {
        if (status == ExecutionStatus.COMPLETADA) return getColor(R.color.admin_status_delivered);
        if (status == ExecutionStatus.EN_PROCESO) return getColor(R.color.admin_status_pending);
        return getColor(R.color.admin_purple_accent);
    }

    private void chooseAgency() {
        List<String> names = new ArrayList<>();
        names.add("Todas las agencias");
        int selectedIndex = 0;
        for (int i = 0; i < agencyRows.size(); i++) {
            JsonObject row = agencyRows.get(i).getAsJsonObject();
            names.add(stringValue(row, "nombre"));
            if ((int) longValue(row, "id_agencia") == selectedAgencyId) {
                selectedIndex = i + 1;
            }
        }
        showPremiumSelectorSheet(
                "Seleccionar agencia",
                "Filtra la participación y resultados por sede real",
                R.drawable.admin_ic_building,
                names,
                selectedIndex,
                which -> {
                    selectedAgencyId = which == 0 ? 0
                            : (int) longValue(agencyRows.get(which - 1).getAsJsonObject(), "id_agencia");
                    tvAgency.setText(which == 0 ? "Todas" : names.get(which));
                    refresh();
                }
        );
    }

    private void chooseCampaign() {
        List<JsonObject> visible = new ArrayList<>();
        String key = monthKey();
        for (JsonElement el : campaignRows) {
            if (!el.isJsonObject()) continue;
            JsonObject row = el.getAsJsonObject();
            if (stringValue(row, "inicio_en").startsWith(key)) {
                visible.add(row);
            }
        }
        List<String> names = new ArrayList<>();
        names.add("Todas las campañas");
        int selectedIndex = 0;
        for (int i = 0; i < visible.size(); i++) {
            names.add(stringValue(visible.get(i), "titulo"));
            if (longValue(visible.get(i), "id_campana") == selectedCampaignId) {
                selectedIndex = i + 1;
            }
        }
        showPremiumSelectorSheet(
                "Seleccionar campaña",
                "Campañas registradas para " + AdminWellnessHelper.formatMonthDisplay(key),
                R.drawable.admin_ic_calendar_range,
                names,
                selectedIndex,
                which -> {
                    selectedCampaignId = which == 0 ? 0 : longValue(visible.get(which - 1), "id_campana");
                    tvCampaign.setText(which == 0 ? "Todas" : names.get(which));
                    refresh();
                }
        );
    }

    private void showPremiumSelectorSheet(String title, String subtitle, @DrawableRes int iconRes,
                                          List<String> options, int selectedIndex, Consumer<Integer> onPicked) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int sheetBgColor = getColor(R.color.admin_card_bg);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(sheetBgColor);
        rootBg.setCornerRadii(new float[]{dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0});
        root.setBackground(rootBg);
        root.setPadding(dp(18), dp(10), dp(18), dp(22));

        // Handle superior
        View handle = new View(this);
        GradientDrawable hBg = new GradientDrawable();
        hBg.setCornerRadius(dp(99));
        hBg.setColor(getColor(R.color.admin_border_light));
        handle.setBackground(hBg);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(dp(40), dp(4));
        hLp.gravity = Gravity.CENTER_HORIZONTAL;
        hLp.bottomMargin = dp(14);
        root.addView(handle, hLp);

        // Encabezado del sheet
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout iconBox = new FrameLayout(this);
        styleIconBadge(iconBox, getColor(R.color.admin_status_assigned_bg), getColor(R.color.admin_purple_accent));
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconRes);
        iconView.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
        iconBox.addView(iconView, new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
        head.addView(iconBox, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.setMarginStart(dp(11));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.admin_text_primary));
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        titleCol.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(getColor(R.color.admin_text_secondary));
        tvSub.setTextSize(11.5f);
        titleCol.addView(tvSub);
        head.addView(titleCol, tLp);
        root.addView(head);

        ScrollView scroll = new ScrollView(this);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(14);
        root.addView(scroll, sLp);

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            boolean isSelected = i == selectedIndex;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(dp(14));
            rowBg.setColor(getColor(isSelected ? R.color.admin_surface_tertiary : R.color.admin_surface_secondary));
            rowBg.setStroke(dp(1), getColor(isSelected ? R.color.admin_purple_primary : R.color.admin_border));
            row.setBackground(rowBg);
            row.setClickable(true);
            row.setFocusable(true);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));

            ImageView rowIcon = new ImageView(this);
            rowIcon.setImageResource(iconRes);
            rowIcon.setImageTintList(ColorStateList.valueOf(getColor(isSelected ? R.color.admin_purple_accent : R.color.admin_text_muted)));
            row.addView(rowIcon, new LinearLayout.LayoutParams(dp(17), dp(17)));

            TextView label = new TextView(this);
            label.setText(options.get(i));
            label.setTextColor(getColor(R.color.admin_text_primary));
            label.setTextSize(13.5f);
            label.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lLp.setMarginStart(dp(11));
            row.addView(label, lLp);

            if (isSelected) {
                ImageView check = new ImageView(this);
                check.setImageResource(R.drawable.admin_ic_check);
                check.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_status_delivered)));
                row.addView(check, new LinearLayout.LayoutParams(dp(18), dp(18)));
            }

            row.setOnClickListener(v -> {
                sheet.dismiss();
                onPicked.accept(index);
            });

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) rLp.topMargin = dp(8);
            list.addView(row, rLp);
        }

        AdminTypography.apply(this, root);
        sheet.setContentView(root);
        if (sheet.getWindow() != null) {
            sheet.getWindow().setNavigationBarColor(sheetBgColor);
        }
        sheet.show();
    }

    private void showMonthYearBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        final int[] chosenYear = {month.get(Calendar.YEAR)};
        final int currentMonth = month.get(Calendar.MONTH);
        final int currentYear = month.get(Calendar.YEAR);
        String[] monthsShort = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(getColor(R.color.admin_card_bg));
        rootBg.setCornerRadii(new float[]{dp(22), dp(22), dp(22), dp(22), 0, 0, 0, 0});
        root.setBackground(rootBg);
        root.setPadding(dp(18), dp(12), dp(18), dp(22));

        // Selector de año
        LinearLayout yearBar = new LinearLayout(this);
        yearBar.setOrientation(LinearLayout.HORIZONTAL);
        yearBar.setGravity(Gravity.CENTER_VERTICAL);
        yearBar.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
        yearBar.setPadding(dp(8), dp(6), dp(8), dp(6));

        ImageView prevYear = new ImageView(this);
        prevYear.setImageResource(R.drawable.admin_ic_chevron_left);
        prevYear.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
        prevYear.setPadding(dp(8), dp(8), dp(8), dp(8));
        yearBar.addView(prevYear, new LinearLayout.LayoutParams(dp(36), dp(36)));

        TextView tvYear = new TextView(this);
        tvYear.setText(String.valueOf(chosenYear[0]));
        tvYear.setTextColor(getColor(R.color.admin_text_primary));
        tvYear.setTextSize(15.5f);
        tvYear.setTypeface(null, Typeface.BOLD);
        tvYear.setGravity(Gravity.CENTER);
        yearBar.addView(tvYear, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView nextYear = new ImageView(this);
        nextYear.setImageResource(R.drawable.admin_ic_chevron_right);
        nextYear.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
        nextYear.setPadding(dp(8), dp(8), dp(8), dp(8));
        yearBar.addView(nextYear, new LinearLayout.LayoutParams(dp(36), dp(36)));
        root.addView(yearBar);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gLp.topMargin = dp(12);
        root.addView(grid, gLp);

        Runnable renderGrid = () -> {
            grid.removeAllViews();
            tvYear.setText(String.valueOf(chosenYear[0]));
            for (int r = 0; r < 4; r++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                for (int c = 0; c < 3; c++) {
                    final int mIndex = r * 3 + c;
                    boolean selected = chosenYear[0] == currentYear && mIndex == currentMonth;
                    TextView cell = new TextView(this);
                    cell.setText(monthsShort[mIndex]);
                    cell.setGravity(Gravity.CENTER);
                    cell.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
                    cell.setTextSize(13f);
                    cell.setTypeface(null, Typeface.BOLD);
                    cell.setBackgroundResource(R.drawable.admin_bg_wellness_chip);
                    cell.setSelected(selected);
                    cell.setPadding(dp(8), dp(11), dp(8), dp(11));
                    cell.setOnClickListener(v -> {
                        month.set(Calendar.YEAR, chosenYear[0]);
                        month.set(Calendar.MONTH, mIndex);
                        selectedCampaignId = 0;
                        tvCampaign.setText("Todas");
                        sheet.dismiss();
                        refresh();
                    });
                    LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    if (c > 0) cLp.setMarginStart(dp(8));
                    row.addView(cell, cLp);
                }
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (r > 0) rLp.topMargin = dp(8);
                grid.addView(row, rLp);
            }
            AdminTypography.apply(this, grid);
        };

        prevYear.setOnClickListener(v -> { chosenYear[0]--; renderGrid.run(); });
        nextYear.setOnClickListener(v -> { chosenYear[0]++; renderGrid.run(); });
        renderGrid.run();

        AdminTypography.apply(this, root);
        sheet.setContentView(root);
        sheet.show();
    }

    private void openPlanForm(@Nullable JsonObject current) {
        Intent intent = new Intent(this, AdminWellnessActivityFormActivity.class);
        if (current != null) {
            intent.putExtra(AdminWellnessActivityFormActivity.EXTRA_PLAN_JSON, current.toString());
        }
        if (agencyRows != null && agencyRows.size() > 0) {
            intent.putExtra(AdminWellnessActivityFormActivity.EXTRA_AGENCIES_JSON, agencyRows.toString());
        }
        if (campaignRows != null && campaignRows.size() > 0) {
            intent.putExtra(AdminWellnessActivityFormActivity.EXTRA_CAMPAIGNS_JSON, campaignRows.toString());
        }
        activityFormLauncher.launch(intent);
    }

    private void renderAssistantQuickQuestions() {
        if (assistantQuickQuestions == null) return;
        assistantQuickQuestions.removeAllViews();
        List<AdminWellnessAssistantHelper.SuggestedQuestion> items =
                AdminWellnessAssistantHelper.adminSuggestedQuestions();
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            AdminWellnessAssistantHelper.SuggestedQuestion item = items.get(i);
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setPadding(dp(11), dp(9), dp(11), dp(9));

            TextView lbl = new TextView(this);
            lbl.setText(item.question);
            lbl.setTextColor(getColor(R.color.admin_text_primary));
            lbl.setTextSize(12f);
            chip.addView(lbl, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.admin_ic_chevron_right);
            arrow.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
            chip.addView(arrow, new LinearLayout.LayoutParams(dp(14), dp(14)));

            chip.setOnClickListener(v -> openAssistant(item.question));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(7);
            assistantQuickQuestions.addView(chip, lp);
        }
    }

    private void openAssistant(@Nullable String initialQuestion) {
        Intent intent = new Intent(this, AdminWellnessAssistantActivity.class);
        if (initialQuestion != null && !initialQuestion.trim().isEmpty()) {
            intent.putExtra(AdminWellnessAssistantActivity.EXTRA_INITIAL_QUESTION, initialQuestion);
        }
        startActivity(intent);
    }

    private void addMetricBarRow(LinearLayout parent, String label, String valueText,
                                 float value, float max, int barColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(10);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(getColor(R.color.admin_text_primary));
        lbl.setTextSize(13f);
        lbl.setTypeface(null, Typeface.BOLD);
        head.addView(lbl, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView val = new TextView(this);
        val.setText(valueText);
        val.setTextColor(barColor);
        val.setTextSize(12.5f);
        val.setTypeface(null, Typeface.BOLD);
        head.addView(val);
        row.addView(head);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.round(max * 100));
        bar.setProgress(Math.max(0, Math.min(bar.getMax(), Math.round(value * 100))));
        bar.setProgressTintList(ColorStateList.valueOf(barColor));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(getColor(R.color.admin_border)));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        bLp.topMargin = dp(5);
        row.addView(bar, bLp);

        parent.addView(row, rowLp);
    }

    private void renderEmptyBlock(LinearLayout parent, int iconRes, String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(12), dp(18), dp(12), dp(18));

        FrameLayout iconWrap = new FrameLayout(this);
        styleIconBadge(iconWrap, getColor(R.color.admin_status_assigned_bg), getColor(R.color.admin_purple_accent));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
        iconWrap.addView(icon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        box.addView(iconWrap, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.admin_text_primary));
        tvTitle.setTextSize(14.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(11);
        box.addView(tvTitle, tLp);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(getColor(R.color.admin_text_secondary));
        tvSub.setTextSize(12f);
        tvSub.setLineSpacing(dp(2), 1f);
        tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(5);
        box.addView(tvSub, sLp);

        parent.addView(box);
    }

    private void request(Call<ApiResponse<JsonObject>> call, Consumer<JsonObject> done) {
        activeCalls.add(call);
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                activeCalls.remove(c);
                if (!sameAccount() || isFinishing()) return;
                ApiResponse<JsonObject> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccessful() && body.getData() != null) {
                    done.accept(body.getData());
                } else {
                    String msg = response.code() == 403
                            ? "Tu cuenta no tiene permiso para consultar Bienestar administrativo."
                            : response.code() == 401
                            ? "Sesión vencida. Vuelve a iniciar sesión."
                            : "No se pudo completar la consulta (HTTP " + response.code() + "). Toca para reintentar.";
                    showStatus(msg, true);
                }
                if (response.errorBody() != null) response.errorBody().close();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                activeCalls.remove(c);
                if (sameAccount() && !isFinishing()) {
                    showStatus("Sin conexión con el servidor. Toca aquí para reintentar.", true);
                }
            }
        });
    }

    private void showStatus(String message, boolean isError) {
        statusBanner.setText(message);
        statusBanner.setTextColor(getColor(isError ? R.color.admin_status_overdue : R.color.admin_text_secondary));
        statusBanner.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusBanner.setVisibility(View.GONE);
    }

    private boolean sameAccount() {
        SessionManager s = new SessionManager(this);
        return s.isLoggedIn() && s.getUserId() == userId && token.equals(s.getBearerToken());
    }

    @Override
    protected void onStop() {
        for (Call<ApiResponse<JsonObject>> c : activeCalls) c.cancel();
        activeCalls.clear();
        super.onStop();
    }

    private boolean isReduceMotionEnabled() {
        try {
            return Settings.Global.getFloat(
                    getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String stringValue(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? "—" : e.getAsString();
    }

    private static long longValue(JsonObject o, String key) {
        try {
            return o.get(key).getAsLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static int intValue(JsonObject o, String key, int fallback) {
        try {
            JsonElement e = o.get(key);
            return e == null || e.isJsonNull() ? fallback : e.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Nullable
    private static Double nullableDouble(JsonObject o, String key) {
        try {
            JsonElement e = o.get(key);
            if (e == null || e.isJsonNull()) return null;
            String s = e.getAsString().trim();
            if (s.isEmpty() || "—".equals(s)) return null;
            return Double.parseDouble(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
