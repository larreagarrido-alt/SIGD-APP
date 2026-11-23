package pe.net.ws.kpiweb.features.admin.dashboard;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.CampaignOperationalStats;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.CampaignRecord;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.CampaignStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.RangeValidationResult;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.RyffDimensionInfo;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRemoteMapper;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Módulo B — Evaluación de Ryff (Administrador — SIGD).
 * Organizado en tres pestañas reales:
 * 1) Aplicaciones (invitaciones, asistente de nueva aplicación en 3 pasos y seguimiento)
 * 2) Resultados (seis dimensiones de Ryff agregadas y autorizadas, protegiendo privacidad)
 * 3) Configuración (instrumento Ryff del servidor, vigencia y cierre/publicación autorizada)
 */
public final class AdminRyffActivity extends AppCompatActivity {

    private static final int TAB_APPLICATIONS = 0;
    private static final int TAB_RESULTS = 1;
    private static final int TAB_CONFIG = 2;

    private final Calendar resultsMonth = Calendar.getInstance();
    private final List<Call<ApiResponse<JsonObject>>> activeCalls = new ArrayList<>();
    private final List<CampaignRecord> campaigns = new ArrayList<>();
    private final Map<Long, JsonObject> rawCampaigns = new HashMap<>();

    private int activeTab = TAB_APPLICATIONS;
    private String selectedStatusFilter = "TODAS";
    private String searchQuery = "";

    private TextView tabApplications;
    private TextView tabResults;
    private TextView tabConfig;
    private View paneApplications;
    private View paneResults;
    private View paneConfig;
    private ScrollView scrollView;
    private TextView statusBanner;
    private ImageView refreshIcon;

    // Aplicaciones
    private TextView statSentLabel;
    private TextView statSent;
    private TextView statPending;
    private TextView statCompleted;
    private TextView statExpired;
    private EditText searchInput;
    private LinearLayout statusFiltersContainer;
    private LinearLayout applicationsList;

    // Resultados
    private TextView resultsPeriodTv;
    private LinearLayout dimensionsList;

    // Configuración
    private LinearLayout instrumentCard;
    private LinearLayout configCampaignsList;

    private JsonObject latestOverview;
    private JsonObject latestQuestionnaire;
    private String token;
    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_wellness_ryff);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        bindViews();
        bindEvents();
        selectTab(TAB_APPLICATIONS);
        renderStatusFilterChips();
        AdminTypography.apply(this, findViewById(android.R.id.content));
        refreshAll();
    }

    private void bindViews() {
        tabApplications = findViewById(R.id.ryff_tab_applications);
        tabResults = findViewById(R.id.ryff_tab_results);
        tabConfig = findViewById(R.id.ryff_tab_config);
        paneApplications = findViewById(R.id.ryff_pane_applications);
        paneResults = findViewById(R.id.ryff_pane_results);
        paneConfig = findViewById(R.id.ryff_pane_config);
        scrollView = findViewById(R.id.ryff_scroll);
        statusBanner = findViewById(R.id.ryff_status_banner);
        refreshIcon = findViewById(R.id.ryff_refresh_icon);

        statSentLabel = findViewById(R.id.ryff_stat_sent_label);
        statSent = findViewById(R.id.ryff_stat_sent);
        statPending = findViewById(R.id.ryff_stat_pending);
        statCompleted = findViewById(R.id.ryff_stat_completed);
        statExpired = findViewById(R.id.ryff_stat_expired);
        searchInput = findViewById(R.id.ryff_search_input);
        statusFiltersContainer = findViewById(R.id.ryff_status_filters);
        applicationsList = findViewById(R.id.ryff_applications_list);

        resultsPeriodTv = findViewById(R.id.ryff_results_tv_period);
        dimensionsList = findViewById(R.id.ryff_dimensions_list);

        instrumentCard = findViewById(R.id.ryff_instrument_card);
        configCampaignsList = findViewById(R.id.ryff_config_campaigns_list);
    }

    private void bindEvents() {
        findViewById(R.id.ryff_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.ryff_btn_refresh).setOnClickListener(v -> {
            if (refreshIcon != null && !isReduceMotionEnabled()) {
                refreshIcon.animate().rotationBy(360f).setDuration(420L).start();
            }
            refreshAll();
        });
        tabApplications.setOnClickListener(v -> selectTab(TAB_APPLICATIONS));
        tabResults.setOnClickListener(v -> selectTab(TAB_RESULTS));
        tabConfig.setOnClickListener(v -> selectTab(TAB_CONFIG));
        findViewById(R.id.ryff_btn_new_application).setOnClickListener(v -> openNewApplicationWizard());
        findViewById(R.id.ryff_results_prev_month).setOnClickListener(v -> {
            resultsMonth.add(Calendar.MONTH, -1);
            refreshResultsOnly();
        });
        findViewById(R.id.ryff_results_next_month).setOnClickListener(v -> {
            resultsMonth.add(Calendar.MONTH, 1);
            refreshResultsOnly();
        });
        statusBanner.setOnClickListener(v -> refreshAll());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim() : "";
                renderApplicationsList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void selectTab(int tab) {
        activeTab = tab;
        tabApplications.setSelected(tab == TAB_APPLICATIONS);
        tabResults.setSelected(tab == TAB_RESULTS);
        tabConfig.setSelected(tab == TAB_CONFIG);
        int inactiveTabColor = getColor(R.color.admin_text_secondary);
        tabApplications.setTextColor(tab == TAB_APPLICATIONS ? Color.WHITE : inactiveTabColor);
        tabResults.setTextColor(tab == TAB_RESULTS ? Color.WHITE : inactiveTabColor);
        tabConfig.setTextColor(tab == TAB_CONFIG ? Color.WHITE : inactiveTabColor);

        paneApplications.setVisibility(tab == TAB_APPLICATIONS ? View.VISIBLE : View.GONE);
        paneResults.setVisibility(tab == TAB_RESULTS ? View.VISIBLE : View.GONE);
        paneConfig.setVisibility(tab == TAB_CONFIG ? View.VISIBLE : View.GONE);
        scrollView.scrollTo(0, 0);
    }

    private String resultsMonthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(resultsMonth.getTime());
    }

    private void refreshAll() {
        showStatus("Consultando información de Ryff en el servidor…", false);
        refreshResultsOnly();

        request(ApiClient.getService().getAdminWellnessCampaigns(token), data -> {
            hideStatus();
            campaigns.clear();
            rawCampaigns.clear();
            JsonArray rows = array(data, "campanas");
            for (JsonElement el : rows) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                long id = longValue(obj, "id_campana");
                rawCampaigns.put(id, obj);
                String rawStatus = hasNonEmpty(obj, "estado_efectivo")
                        ? stringValue(obj, "estado_efectivo") : stringValue(obj, "estado");
                boolean hasInv = obj.has("invitados") && !obj.get("invitados").isJsonNull();
                int invited = intValue(obj, "invitados", 0);
                int completed = intValue(obj, "completados",
                        intValue(obj, "cuestionarios_completados", 0));
                campaigns.add(new CampaignRecord(
                        id,
                        stringValue(obj, "titulo"),
                        stringValue(obj, "descripcion"),
                        stringValue(obj, "audiencia"),
                        stringValue(obj, "inicio_en"),
                        stringValue(obj, "fin_en"),
                        AdminWellnessHelper.normalizeCampaignStatus(rawStatus),
                        invited,
                        completed,
                        hasInv
                ));
            }
            renderOperationalSummary();
            renderApplicationsList();
            renderConfigCampaigns();
            AdminTypography.apply(this, findViewById(android.R.id.content));
        });

        request(ApiClient.getService().getAdminRyffQuestionnaire(token), data -> {
            latestQuestionnaire = data;
            renderInstrumentCard();
            renderDimensions();
            AdminTypography.apply(this, instrumentCard);
        });
    }

    private void refreshResultsOnly() {
        String key = resultsMonthKey();
        resultsPeriodTv.setText(AdminWellnessHelper.formatMonthDisplay(key));
        request(ApiClient.getService().getAdminWellnessOverview(token, key, 0, 0), data -> {
            latestOverview = data;
            renderDimensions();
            AdminTypography.apply(this, dimensionsList);
        });
    }

    private void renderOperationalSummary() {
        CampaignOperationalStats stats = AdminWellnessHelper.computeCampaignStats(campaigns);
        statSentLabel.setText(stats.usesInvitationCounts ? "Invitaciones" : "Aplicaciones");
        statSent.setText(String.valueOf(stats.totalApplications));
        statPending.setText(String.valueOf(stats.activeOrPending));
        statCompleted.setText(String.valueOf(stats.completedEvaluations));
        statExpired.setText(String.valueOf(stats.finishedOrExpired));
    }

    private void renderStatusFilterChips() {
        statusFiltersContainer.removeAllViews();
        String[][] filters = {
                {"TODAS", "Todas"},
                {"ACTIVA", "Activas"},
                {"PROGRAMADA", "Programadas"},
                {"BORRADOR", "Borrador"},
                {"FINALIZADA", "Finalizadas"},
                {"CANCELADA", "Canceladas"}
        };
        for (String[] pair : filters) {
            String key = pair[0];
            String label = pair[1];
            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(12f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setBackgroundResource(R.drawable.admin_bg_wellness_chip);
            chip.setSelected(key.equals(selectedStatusFilter));
            chip.setTextColor(key.equals(selectedStatusFilter) ? Color.WHITE : getColor(R.color.admin_text_secondary));
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            chip.setOnClickListener(v -> {
                selectedStatusFilter = key;
                renderStatusFilterChips();
                renderApplicationsList();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            statusFiltersContainer.addView(chip, lp);
        }
    }

    private void renderApplicationsList() {
        applicationsList.removeAllViews();
        List<CampaignRecord> filtered = new ArrayList<>();
        String q = AdminWellnessHelper.normalizeSearchText(searchQuery);
        for (CampaignRecord item : campaigns) {
            if (!"TODAS".equals(selectedStatusFilter) && !item.status.name().equals(selectedStatusFilter)) {
                continue;
            }
            if (!q.isEmpty() && !AdminWellnessHelper.normalizeSearchText(item.title).contains(q)
                    && !AdminWellnessHelper.normalizeSearchText(item.description).contains(q)) {
                continue;
            }
            filtered.add(item);
        }

        if (campaigns.isEmpty()) {
            renderEmptyCard(
                    applicationsList,
                    R.drawable.collab_ic_rec_brain,
                    0xFF57D6A0,
                    "Todavía no hay aplicaciones de Ryff",
                    "Configura una aplicación y selecciona a los colaboradores autorizados"
            );
            return;
        }

        if (filtered.isEmpty()) {
            renderEmptyCard(
                    applicationsList,
                    R.drawable.admin_ic_search,
                    0xFFD8B4FE,
                    "Sin coincidencias",
                    "No hay aplicaciones que coincidan con el filtro o búsqueda actual."
            );
            return;
        }

        for (CampaignRecord item : filtered) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.admin_bg_wellness_card);
            card.setPadding(dp(14), dp(13), dp(14), dp(13));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> showCampaignActionsDialog(item));

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView title = new TextView(this);
            title.setText(item.title);
            title.setTextColor(getColor(R.color.admin_text_primary));
            title.setTextSize(14.5f);
            title.setTypeface(null, Typeface.BOLD);
            header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView badge = buildStatusBadge(item.status);
            header.addView(badge);
            card.addView(header);

            TextView range = new TextView(this);
            range.setText("Vigencia: " + item.startAt + "  →  " + item.endAt);
            range.setTextColor(getColor(R.color.admin_text_secondary));
            range.setTextSize(11.5f);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.topMargin = dp(6);
            card.addView(range, rLp);

            TextView meta = new TextView(this);
            StringBuilder metaText = new StringBuilder("#").append(item.id)
                    .append(" · Audiencia: ").append(item.audience);
            if (item.hasInvitationCounts) {
                metaText.append(" · Invitados: ").append(item.invitedCount)
                        .append(" · Completadas: ").append(item.completedCount);
            }
            meta.setText(metaText.toString());
            meta.setTextColor(getColor(R.color.admin_text_muted));
            meta.setTextSize(11f);
            LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mLp.topMargin = dp(4);
            card.addView(meta, mLp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            applicationsList.addView(card, lp);
        }
    }

    private void renderDimensions() {
        dimensionsList.removeAllViews();
        JsonArray serverDimensions = latestOverview != null ? array(latestOverview, "dimensiones") : new JsonArray();
        if (serverDimensions.size() == 0) {
            renderEmptyCard(
                    dimensionsList,
                    R.drawable.collab_ic_rec_brain,
                    0xFF8DE1D5,
                    "No hay resultados disponibles para mostrar",
                    "Los resultados agregados de las seis dimensiones aparecen cuando existen evaluaciones completadas de una versión Ryff validada que cumple los criterios de confidencialidad del servidor."
            );
            return;
        }

        float scaleMax = 5f;
        if (latestQuestionnaire != null && latestQuestionnaire.has("instrumento")
                && latestQuestionnaire.get("instrumento").isJsonObject()) {
            JsonObject inst = latestQuestionnaire.getAsJsonObject("instrumento");
            float parsed = floatValue(inst, "escala_max", 5f);
            if (parsed > 0f) scaleMax = parsed;
        }

        Map<String, JsonObject> byKey = new HashMap<>();
        for (JsonElement el : serverDimensions) {
            if (!el.isJsonObject()) continue;
            JsonObject row = el.getAsJsonObject();
            byKey.put(AdminWellnessHelper.normalizeDimensionKey(stringValue(row, "dimension")), row);
        }

        for (RyffDimensionInfo dim : AdminWellnessHelper.canonicalRyffDimensions()) {
            JsonObject row = byKey.get(dim.key);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.admin_bg_wellness_surface);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);

            FrameLayout iconBox = new FrameLayout(this);
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setCornerRadius(dp(10));
            iconBg.setColor(dim.badgeBgHex);
            iconBox.setBackground(iconBg);
            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.collab_ic_rec_brain);
            icon.setImageTintList(ColorStateList.valueOf(dim.accentColorHex));
            FrameLayout.LayoutParams iLp = new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER);
            iconBox.addView(icon, iLp);
            top.addView(iconBox, new LinearLayout.LayoutParams(dp(36), dp(36)));

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tcLp.setMarginStart(dp(10));

            TextView tvName = new TextView(this);
            tvName.setText(dim.displayName);
            tvName.setTextColor(getColor(R.color.admin_text_primary));
            tvName.setTextSize(14f);
            tvName.setTypeface(null, Typeface.BOLD);
            titleCol.addView(tvName);

            TextView tvParticipants = new TextView(this);
            if (row != null) {
                tvParticipants.setText("Participantes incluidos: " + stringValue(row, "participantes"));
            } else {
                tvParticipants.setText("Sin datos agregados suficientes en este período");
            }
            tvParticipants.setTextColor(getColor(R.color.admin_text_secondary));
            tvParticipants.setTextSize(11f);
            titleCol.addView(tvParticipants);
            top.addView(titleCol, tcLp);

            TextView tvAvg = new TextView(this);
            if (row != null) {
                tvAvg.setText(stringValue(row, "promedio") + " / " + Math.round(scaleMax));
            } else {
                tvAvg.setText("—");
            }
            boolean isDark = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            int dimAccent = isDark ? dim.accentColorHex
                    : androidx.core.graphics.ColorUtils.blendARGB(dim.accentColorHex, 0xFF241532, 0.42f);
            tvAvg.setTextColor(dimAccent);
            tvAvg.setTextSize(14f);
            tvAvg.setTypeface(null, Typeface.BOLD);
            top.addView(tvAvg);
            card.addView(top);

            if (row != null) {
                float avg = floatValue(row, "promedio", 0f);
                ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                bar.setMax(Math.round(scaleMax * 100));
                bar.setProgress(Math.max(0, Math.min(bar.getMax(), Math.round(avg * 100))));
                bar.setProgressTintList(ColorStateList.valueOf(dimAccent));
                bar.setProgressBackgroundTintList(ColorStateList.valueOf(getColor(R.color.admin_border)));
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
                bLp.topMargin = dp(8);
                card.addView(bar, bLp);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            dimensionsList.addView(card, lp);
        }
    }

    private void renderInstrumentCard() {
        instrumentCard.removeAllViews();
        JsonObject instrument = latestQuestionnaire != null && latestQuestionnaire.has("instrumento")
                && latestQuestionnaire.get("instrumento").isJsonObject()
                ? latestQuestionnaire.getAsJsonObject("instrumento") : null;

        if (instrument == null) {
            TextView title = new TextView(this);
            title.setText("Instrumento Ryff pendiente de validación");
            title.setTextColor(getColor(R.color.admin_text_primary));
            title.setTextSize(14.5f);
            title.setTypeface(null, Typeface.BOLD);
            instrumentCard.addView(title);

            TextView desc = new TextView(this);
            desc.setText("El servidor aún no reporta una versión Ryff validada. Las preguntas de demostración no sustituyen al instrumento oficial.");
            desc.setTextColor(getColor(R.color.admin_text_secondary));
            desc.setTextSize(12f);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dLp.topMargin = dp(5);
            instrumentCard.addView(desc, dLp);
            return;
        }

        TextView title = new TextView(this);
        title.setText(stringValue(instrument, "titulo"));
        title.setTextColor(getColor(R.color.admin_text_primary));
        title.setTextSize(15f);
        title.setTypeface(null, Typeface.BOLD);
        instrumentCard.addView(title);

        JsonArray questions = array(latestQuestionnaire, "preguntas");
        TextView details = new TextView(this);
        details.setText("Versión: " + stringValue(instrument, "version_instrumento")
                + " · Escala: " + stringValue(instrument, "escala_min") + " a " + stringValue(instrument, "escala_max")
                + "\nEstado: " + stringValue(latestQuestionnaire, "estado")
                + " · Ítems registrados: " + questions.size());
        details.setTextColor(getColor(R.color.admin_text_secondary));
        details.setTextSize(12f);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = dp(6);
        instrumentCard.addView(details, dLp);

        TextView viewBtn = new TextView(this);
        viewBtn.setText("Consultar preguntas del instrumento");
        viewBtn.setTextColor(getColor(R.color.admin_text_primary));
        viewBtn.setTextSize(12.5f);
        viewBtn.setTypeface(null, Typeface.BOLD);
        viewBtn.setGravity(Gravity.CENTER);
        viewBtn.setBackgroundResource(R.drawable.admin_bg_wellness_secondary_btn);
        viewBtn.setPadding(dp(14), dp(10), dp(14), dp(10));
        viewBtn.setOnClickListener(v -> showQuestionnaireDialog(instrument, questions));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = dp(12);
        instrumentCard.addView(viewBtn, bLp);
    }

    private void renderConfigCampaigns() {
        configCampaignsList.removeAllViews();
        if (campaigns.isEmpty()) {
            renderEmptyCard(
                    configCampaignsList,
                    R.drawable.admin_ic_settings_2,
                    getColor(R.color.admin_purple_accent),
                    "Sin aplicaciones configuradas",
                    "Crea una aplicación desde la pestaña Aplicaciones para administrar su vigencia y publicación."
            );
            return;
        }
        for (CampaignRecord item : campaigns) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.admin_bg_wellness_surface);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));

            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(this);
            title.setText(item.title);
            title.setTextColor(getColor(R.color.admin_text_primary));
            title.setTextSize(14f);
            title.setTypeface(null, Typeface.BOLD);
            head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            head.addView(buildStatusBadge(item.status));
            card.addView(head);

            TextView info = new TextView(this);
            info.setText("Inicio: " + item.startAt + "\nVencimiento: " + item.endAt + "\nDestinatarios: " + item.audience);
            info.setTextColor(getColor(R.color.admin_text_secondary));
            info.setTextSize(11.5f);
            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            iLp.topMargin = dp(6);
            card.addView(info, iLp);

            TextView action = new TextView(this);
            action.setText("Administrar vigencia / estado");
            action.setTextColor(getColor(R.color.admin_text_primary));
            action.setTextSize(12f);
            action.setTypeface(null, Typeface.BOLD);
            action.setGravity(Gravity.CENTER);
            action.setBackgroundResource(R.drawable.admin_bg_wellness_secondary_btn);
            action.setPadding(dp(12), dp(9), dp(12), dp(9));
            action.setOnClickListener(v -> showCampaignActionsDialog(item));
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            aLp.topMargin = dp(10);
            card.addView(action, aLp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            configCampaignsList.addView(card, lp);
        }
    }

    private void showCampaignActionsDialog(CampaignRecord item) {
        String[] options = {
                "Vincular test Ryff validado y publicar",
                "Desactivar aplicación",
                "Finalizar anticipadamente"
        };
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        EditText input = new EditText(this);
                        input.setHint("ID numérico del test Ryff validado");
                        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                        new AlertDialog.Builder(this)
                                .setTitle("Publicar aplicación Ryff")
                                .setMessage("Las evaluaciones completadas y los resultados históricos nunca se alteran.")
                                .setView(input)
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Publicar", (d, w) -> {
                                    try {
                                        int testId = Integer.parseInt(input.getText().toString().trim());
                                        JsonObject body = new JsonObject();
                                        body.addProperty("id_test", testId);
                                        body.addProperty("estado", "PUBLICADA");
                                        request(ApiClient.getService().updateAdminWellnessCampaign(token, item.id, body),
                                                data -> refreshAll());
                                    } catch (NumberFormatException ignored) {
                                        Toast.makeText(this, "Ingresa un ID de test numérico válido.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .show();
                    } else {
                        String nextState = which == 1 ? "DESACTIVADA" : "CERRADA";
                        String confirmMsg = which == 1
                                ? "La aplicación dejará de estar disponible para nuevas respuestas. Los resultados registrados se conservan."
                                : "La aplicación quedará finalizada anticipadamente sin alterar evaluaciones completadas.";
                        new AlertDialog.Builder(this)
                                .setTitle("Confirmar cambio de estado")
                                .setMessage(confirmMsg)
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Confirmar", (d, w) -> {
                                    JsonObject body = new JsonObject();
                                    body.addProperty("estado", nextState);
                                    request(ApiClient.getService().updateAdminWellnessCampaign(token, item.id, body),
                                            data -> refreshAll());
                                })
                                .show();
                    }
                })
                .show();
    }

    private void showQuestionnaireDialog(JsonObject instrument, JsonArray questions) {
        StringBuilder sb = new StringBuilder();
        sb.append(stringValue(instrument, "titulo"))
                .append(" · v").append(stringValue(instrument, "version_instrumento"))
                .append("\nEscala: ").append(stringValue(instrument, "escala_min"))
                .append(" a ").append(stringValue(instrument, "escala_max")).append("\n\n");
        for (JsonElement el : questions) {
            if (!el.isJsonObject()) continue;
            JsonObject q = el.getAsJsonObject();
            sb.append(stringValue(q, "orden")).append(". ").append(stringValue(q, "enunciado"))
                    .append("\nDimensión: ").append(AdminWellnessHelper.formatRyffDimensionName(stringValue(q, "dimension_ryff")))
                    .append("\n\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("Cuestionario Ryff del servidor")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    // =========================================================================
    // FORMULARIO MODERNO EN 3 PASOS: NUEVA APLICACIÓN DE RYFF
    // =========================================================================
    private void openNewApplicationWizard() {
        View view = LayoutInflater.from(this).inflate(R.layout.admin_dialog_ryff_wizard, null);
        TextView stepSubtitle = view.findViewById(R.id.wizard_step_subtitle);
        View bar1 = view.findViewById(R.id.wizard_bar_1);
        View bar2 = view.findViewById(R.id.wizard_bar_2);
        View bar3 = view.findViewById(R.id.wizard_bar_3);
        TextView errorText = view.findViewById(R.id.wizard_error_text);

        View step1 = view.findViewById(R.id.wizard_step_1);
        View step2 = view.findViewById(R.id.wizard_step_2);
        View step3 = view.findViewById(R.id.wizard_step_3);

        EditText inputTitle = view.findViewById(R.id.wizard_input_title);
        EditText inputDesc = view.findViewById(R.id.wizard_input_desc);
        EditText searchCollab = view.findViewById(R.id.wizard_search_collaborators);
        TextView selectedCountTv = view.findViewById(R.id.wizard_selected_count);
        TextView collabStatusTv = view.findViewById(R.id.wizard_collaborators_status);
        LinearLayout collabList = view.findViewById(R.id.wizard_collaborators_list);

        View pickStart = view.findViewById(R.id.wizard_pick_start);
        View pickEnd = view.findViewById(R.id.wizard_pick_end);
        TextView tvStart = view.findViewById(R.id.wizard_tv_start);
        TextView tvEnd = view.findViewById(R.id.wizard_tv_end);
        TextView tvTimezone = view.findViewById(R.id.wizard_tv_timezone);

        LinearLayout summaryCard = view.findViewById(R.id.wizard_summary_card);
        TextView btnPrev = view.findViewById(R.id.wizard_btn_prev);
        TextView btnNext = view.findViewById(R.id.wizard_btn_next);

        final List<EmployeeItem> allEmployees = new ArrayList<>();
        final Set<String> selectedIds = new LinkedHashSet<>();
        final int[] currentStep = {1};
        final long[] startMillis = {0L};
        final long[] endMillis = {0L};
        final boolean[] submitting = {false};
        final TimeZone tz = TimeZone.getDefault();

        tvTimezone.setText("Zona horaria aplicada: " + tz.getID()
                + " · La evaluación solo estará disponible dentro de este rango.");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        view.findViewById(R.id.wizard_btn_close).setOnClickListener(v -> dialog.dismiss());

        Runnable renderCollaborators = () -> {
            collabList.removeAllViews();
            String q = searchCollab.getText().toString().trim();
            int shown = 0;
            for (EmployeeItem emp : allEmployees) {
                if (!AdminWellnessHelper.matchesCollaboratorQuery(emp.getName(), emp.getRole(), emp.getDepartment(), q)) {
                    continue;
                }
                shown++;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
                row.setPadding(dp(10), dp(8), dp(10), dp(8));

                TextView avatar = new TextView(this);
                avatar.setText(emp.getAvatarInitials());
                avatar.setTextColor(Color.WHITE);
                avatar.setTextSize(11.5f);
                avatar.setTypeface(null, Typeface.BOLD);
                avatar.setGravity(Gravity.CENTER);
                GradientDrawable avBg = new GradientDrawable();
                avBg.setShape(GradientDrawable.OVAL);
                avBg.setColor(emp.getAvatarColorHex() != 0 ? emp.getAvatarColorHex() : 0xFF6B3FA0);
                avatar.setBackground(avBg);
                row.addView(avatar, new LinearLayout.LayoutParams(dp(34), dp(34)));

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                iLp.setMarginStart(dp(10));
                iLp.setMarginEnd(dp(8));

                TextView name = new TextView(this);
                name.setText(emp.getName());
                name.setTextColor(getColor(R.color.admin_text_primary));
                name.setTextSize(13f);
                name.setTypeface(null, Typeface.BOLD);
                info.addView(name);

                TextView meta = new TextView(this);
                meta.setText(emp.getRole() + " · " + emp.getDepartment());
                meta.setTextColor(getColor(R.color.admin_text_secondary));
                meta.setTextSize(11f);
                info.addView(meta);
                row.addView(info, iLp);

                CheckBox cb = new CheckBox(this);
                cb.setChecked(selectedIds.contains(emp.getId()));
                cb.setButtonTintList(ColorStateList.valueOf(0xFF8B30E0));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedIds.add(emp.getId());
                    else selectedIds.remove(emp.getId());
                    int count = selectedIds.size();
                    selectedCountTv.setText(count + (count == 1 ? " seleccionado" : " seleccionados"));
                });
                row.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));
                row.addView(cb);

                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.bottomMargin = dp(7);
                collabList.addView(row, rLp);
            }
            if (!allEmployees.isEmpty()) {
                collabStatusTv.setText(shown == 0
                        ? "No se encontraron colaboradores con ese criterio."
                        : "Mostrando " + shown + " de " + allEmployees.size() + " colaboradores elegibles.");
            }
        };

        // Cargar directorio real de colaboradores desde GET personal
        loadEligibleCollaborators(allEmployees, 0, () -> {
            if (allEmployees.isEmpty()) {
                collabStatusTv.setText("No hay colaboradores elegibles disponibles.");
            }
            renderCollaborators.run();
        }, errMsg -> collabStatusTv.setText(errMsg));

        searchCollab.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCollaborators.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        pickStart.setOnClickListener(v -> pickDateTime(startMillis[0], chosen -> {
            startMillis[0] = chosen;
            tvStart.setText(AdminWellnessHelper.formatDisplayDateTime(chosen, tz));
            errorText.setVisibility(View.GONE);
        }));

        pickEnd.setOnClickListener(v -> pickDateTime(endMillis[0] > 0 ? endMillis[0] : startMillis[0], chosen -> {
            endMillis[0] = chosen;
            tvEnd.setText(AdminWellnessHelper.formatDisplayDateTime(chosen, tz));
            errorText.setVisibility(View.GONE);
        }));

        Runnable syncStepUi = () -> {
            int s = currentStep[0];
            step1.setVisibility(s == 1 ? View.VISIBLE : View.GONE);
            step2.setVisibility(s == 2 ? View.VISIBLE : View.GONE);
            step3.setVisibility(s == 3 ? View.VISIBLE : View.GONE);
            errorText.setVisibility(View.GONE);

            bar1.setBackgroundColor(0xFF8B30E0);
            bar2.setBackgroundColor(s >= 2 ? 0xFF8B30E0 : 0xFF3B2454);
            bar3.setBackgroundColor(s >= 3 ? 0xFF8B30E0 : 0xFF3B2454);

            if (s == 1) {
                stepSubtitle.setText("Paso 1 de 3 · Seleccionar colaboradores");
                btnPrev.setText("Cancelar");
                btnNext.setText("Continuar");
            } else if (s == 2) {
                stepSubtitle.setText("Paso 2 de 3 · Configurar vigencia");
                btnPrev.setText("Atrás");
                btnNext.setText("Revisar");
            } else {
                stepSubtitle.setText("Paso 3 de 3 · Revisar y confirmar");
                btnPrev.setText("Atrás");
                btnNext.setText("Crear invitaciones");

                // Construir resumen real
                summaryCard.removeAllViews();
                RangeValidationResult range = AdminWellnessHelper.validateVigenciaRange(
                        startMillis[0], endMillis[0], System.currentTimeMillis());
                List<String> selectedNames = new ArrayList<>();
                List<String> selectedRoles = new ArrayList<>();
                for (EmployeeItem emp : allEmployees) {
                    if (selectedIds.contains(emp.getId())) {
                        selectedNames.add(emp.getName());
                        selectedRoles.add(emp.getRole());
                    }
                }
                String audience = AdminWellnessHelper.deriveAudienceFromRoles(selectedRoles);
                addSummaryLine(summaryCard, "Aplicación", inputTitle.getText().toString().trim());
                addSummaryLine(summaryCard, "Colaboradores seleccionados",
                        selectedIds.size() + (selectedIds.size() == 1 ? " colaborador" : " colaboradores")
                                + (selectedNames.isEmpty() ? "" : "\n" + previewNames(selectedNames)));
                addSummaryLine(summaryCard, "Inicio", AdminWellnessHelper.formatDisplayDateTime(startMillis[0], tz));
                addSummaryLine(summaryCard, "Vencimiento", AdminWellnessHelper.formatDisplayDateTime(endMillis[0], tz)
                        + " (" + range.durationSummary + ")");
                addSummaryLine(summaryCard, "Estado inicial proyectado", range.projectedStatus);
                addSummaryLine(summaryCard, "Audiencia de campaña", audience);
                addSummaryLine(summaryCard, "Autorización y privacidad",
                        "Evaluación voluntaria · Respuestas individuales confidenciales");
            }
        };

        btnPrev.setOnClickListener(v -> {
            if (submitting[0]) return;
            if (currentStep[0] == 1) {
                dialog.dismiss();
            } else {
                currentStep[0]--;
                syncStepUi.run();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (submitting[0]) return;
            if (currentStep[0] == 1) {
                String title = inputTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    showWizardError(errorText, "Ingresa un nombre para identificar la aplicación de Ryff.");
                    return;
                }
                if (selectedIds.isEmpty()) {
                    showWizardError(errorText, "Selecciona al menos un colaborador para continuar.");
                    return;
                }
                currentStep[0] = 2;
                syncStepUi.run();
            } else if (currentStep[0] == 2) {
                RangeValidationResult res = AdminWellnessHelper.validateVigenciaRange(
                        startMillis[0], endMillis[0], System.currentTimeMillis());
                if (!res.valid) {
                    showWizardError(errorText, res.errorMessage);
                    return;
                }
                currentStep[0] = 3;
                syncStepUi.run();
            } else {
                // Enviar solicitud real al servidor (nunca simular guardado exitoso)
                submitting[0] = true;
                btnNext.setEnabled(false);
                btnNext.setText("Enviando…");
                errorText.setVisibility(View.GONE);

                List<String> selectedRoles = new ArrayList<>();
                JsonArray workerIds = new JsonArray();
                for (EmployeeItem emp : allEmployees) {
                    if (selectedIds.contains(emp.getId())) {
                        selectedRoles.add(emp.getRole());
                        try {
                            workerIds.add(Integer.parseInt(emp.getId()));
                        } catch (NumberFormatException ignored) {
                            workerIds.add(emp.getId());
                        }
                    }
                }
                String desc = inputDesc.getText().toString().trim();
                if (desc.isEmpty()) {
                    desc = "Aplicación autorizada del cuestionario de bienestar psicológico de Ryff.";
                }
                JsonObject body = new JsonObject();
                body.addProperty("titulo", inputTitle.getText().toString().trim());
                body.addProperty("descripcion", desc);
                body.addProperty("audiencia", AdminWellnessHelper.deriveAudienceFromRoles(selectedRoles));
                body.addProperty("inicio_en", AdminWellnessHelper.formatServerDateTime(startMillis[0], tz));
                body.addProperty("fin_en", AdminWellnessHelper.formatServerDateTime(endMillis[0], tz));
                body.add("trabajadores_ids", workerIds);

                Call<ApiResponse<JsonObject>> call = ApiClient.getService().createAdminWellnessCampaign(token, body);
                activeCalls.add(call);
                call.enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                        activeCalls.remove(c);
                        submitting[0] = false;
                        btnNext.setEnabled(true);
                        btnNext.setText("Crear invitaciones");
                        if (!sameAccount() || isFinishing()) return;
                        ApiResponse<JsonObject> respBody = response.body();
                        if (response.isSuccessful() && respBody != null && respBody.isSuccessful()) {
                            dialog.dismiss();
                            showStatus("Aplicación registrada en el servidor. Recuerda vincular el test Ryff validado en Configuración para habilitarla.", false);
                            refreshAll();
                        } else {
                            String msg = response.code() == 403
                                    ? "Tu cuenta no tiene permiso para crear aplicaciones de Ryff."
                                    : "El servidor rechazó la solicitud (HTTP " + response.code() + "). No se generaron invitaciones.";
                            showWizardError(errorText, msg);
                        }
                        if (response.errorBody() != null) response.errorBody().close();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                        activeCalls.remove(c);
                        submitting[0] = false;
                        btnNext.setEnabled(true);
                        btnNext.setText("Crear invitaciones");
                        if (sameAccount() && !isFinishing()) {
                            showWizardError(errorText, "Sin conexión con el servidor. No se generaron invitaciones.");
                        }
                    }
                });
            }
        });

        syncStepUi.run();
        AdminTypography.apply(this, view);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void loadEligibleCollaborators(List<EmployeeItem> collector, int offset,
                                           Runnable onDone, Consumer<String> onError) {
        Call<ApiResponse<JsonObject>> call = ApiClient.getService().getPersonal(token, 100, offset);
        activeCalls.add(call);
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                activeCalls.remove(c);
                if (!sameAccount() || isFinishing()) return;
                ApiResponse<JsonObject> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    onError.accept(response.code() == 403
                            ? "Tu cuenta no tiene permiso para consultar el directorio de Personal."
                            : "No se pudo cargar el listado de colaboradores (HTTP " + response.code() + ").");
                    if (response.errorBody() != null) response.errorBody().close();
                    return;
                }
                JsonObject data = body.getData();
                JsonArray rows = array(data, "empleados");
                for (JsonElement el : rows) {
                    if (el.isJsonObject()) {
                        EmployeeItem item = PersonalRemoteMapper.employee(el.getAsJsonObject());
                        if (item.getWorkStatus() != EmployeeItem.WorkStatus.INACTIVO) {
                            collector.add(item);
                        }
                    }
                }
                JsonObject pagination = data.has("paginacion") && data.get("paginacion").isJsonObject()
                        ? data.getAsJsonObject("paginacion") : null;
                boolean hasMore = pagination != null && pagination.has("hay_mas")
                        && pagination.get("hay_mas").getAsBoolean() && rows.size() > 0 && offset < 2000;
                if (hasMore) {
                    loadEligibleCollaborators(collector, offset + rows.size(), onDone, onError);
                } else {
                    onDone.run();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                activeCalls.remove(c);
                if (sameAccount() && !isFinishing()) {
                    onError.accept("Sin conexión al cargar colaboradores.");
                }
            }
        });
    }

    private void pickDateTime(long initialMillis, Consumer<Long> onSelected) {
        Calendar cal = Calendar.getInstance();
        if (initialMillis > 0L) {
            cal.setTimeInMillis(initialMillis);
        }
        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(Calendar.YEAR, year);
            chosen.set(Calendar.MONTH, monthOfYear);
            chosen.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            new TimePickerDialog(this, (tView, hourOfDay, minute) -> {
                chosen.set(Calendar.HOUR_OF_DAY, hourOfDay);
                chosen.set(Calendar.MINUTE, minute);
                chosen.set(Calendar.SECOND, 0);
                chosen.set(Calendar.MILLISECOND, 0);
                onSelected.accept(chosen.getTimeInMillis());
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addSummaryLine(LinearLayout parent, String label, String value) {
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(getColor(R.color.admin_text_muted));
        lbl.setTextSize(11f);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (parent.getChildCount() > 0) lLp.topMargin = dp(8);
        parent.addView(lbl, lLp);

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(getColor(R.color.admin_text_primary));
        val.setTextSize(13f);
        val.setTypeface(null, Typeface.BOLD);
        parent.addView(val);
    }

    private static String previewNames(List<String> names) {
        int limit = Math.min(3, names.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        if (names.size() > limit) {
            sb.append(" y ").append(names.size() - limit).append(" más");
        }
        return sb.toString();
    }

    private void showWizardError(TextView errorView, String msg) {
        errorView.setText(msg);
        errorView.setVisibility(View.VISIBLE);
    }

    private TextView buildStatusBadge(CampaignStatus status) {
        TextView badge = new TextView(this);
        badge.setText(AdminWellnessHelper.formatCampaignStatus(status));
        badge.setTextSize(11f);
        badge.setTypeface(null, Typeface.BOLD);
        int bgColor;
        int txtColor;
        switch (status) {
            case ACTIVA:
                bgColor = getColor(R.color.admin_status_delivered_bg);
                txtColor = getColor(R.color.admin_status_delivered);
                break;
            case PROGRAMADA:
                bgColor = getColor(R.color.admin_status_assigned_bg);
                txtColor = getColor(R.color.admin_purple_accent);
                break;
            case FINALIZADA:
                bgColor = getColor(R.color.admin_status_pending_bg);
                txtColor = getColor(R.color.admin_status_pending);
                break;
            case CANCELADA:
                bgColor = getColor(R.color.admin_status_overdue_bg);
                txtColor = getColor(R.color.admin_status_overdue);
                break;
            case BORRADOR:
            default:
                bgColor = getColor(R.color.admin_surface_secondary);
                txtColor = getColor(R.color.admin_text_secondary);
                break;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(99));
        bg.setColor(bgColor);
        badge.setBackground(bg);
        badge.setTextColor(txtColor);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        return badge;
    }

    private void renderEmptyCard(LinearLayout parent, int iconRes, int tint, String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackgroundResource(R.drawable.admin_bg_wellness_surface);
        card.setPadding(dp(20), dp(22), dp(20), dp(22));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(tint));
        card.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(getColor(R.color.admin_text_primary));
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(10);
        card.addView(tvTitle, tLp);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(getColor(R.color.admin_text_secondary));
        tvSub.setTextSize(12.5f);
        tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(5);
        card.addView(tvSub, sLp);

        parent.addView(card);
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
                            ? "Tu cuenta no tiene permiso para gestionar Ryff."
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

    private void showStatus(String msg, boolean isError) {
        statusBanner.setText(msg);
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

    private static boolean hasNonEmpty(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() && !o.get(key).getAsString().trim().isEmpty();
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

    private static float floatValue(JsonObject o, String key, float fallback) {
        try {
            JsonElement e = o.get(key);
            return e == null || e.isJsonNull() ? fallback : e.getAsFloat();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
