package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ExecutionStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessAssistantHelper.ActivityDisplayState;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla Actividades y eventos (Bienestar Psicológico — Colaborador).
 * Consulta únicamente actividades reales publicadas por el Administrador (GET
 * bienestar/actividades),
 * ocultando estrictamente borradores, internas o canceladas.
 */
public final class CollaboratorWellnessActivitiesActivity extends AppCompatActivity {

    private final List<JsonObject> visibleActivities = new ArrayList<>();
    private Call<ApiResponse<JsonObject>> pendingCall;

    private LinearLayout filtersContainer;
    private LinearLayout listContainer;
    private ImageView refreshIcon;

    private String activeFilter = "TODAS";
    private String token;
    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_wellness_activities);
        CollaboratorThemeHelper.setupSystemBars(this);

        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        filtersContainer = findViewById(R.id.collab_act_filters);
        listContainer = findViewById(R.id.collab_act_list);
        refreshIcon = findViewById(R.id.collab_act_refresh_icon);

        findViewById(R.id.collab_act_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.collab_act_btn_refresh).setOnClickListener(v -> {
            if (refreshIcon != null && !isReduceMotionEnabled()) {
                refreshIcon.animate().rotationBy(360f).setDuration(420L).start();
            }
            loadActivities();
        });

        renderFilters();
        CollaboratorThemeHelper.applyReadableTypeface(findViewById(android.R.id.content));
        loadActivities();
    }

    private void renderFilters() {
        filtersContainer.removeAllViews();
        String[][] options = {
                { "TODAS", "Todas" },
                { "PROXIMA", "Próximas" },
                { "EN_CURSO", "En curso" },
                { "FINALIZADA", "Finalizadas" }
        };
        for (String[] pair : options) {
            String key = pair[0];
            String label = pair[1];
            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(12f);
            chip.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            chip.setBackgroundResource(R.drawable.admin_bg_wellness_chip);
            chip.setSelected(key.equals(activeFilter));
            chip.setTextColor(key.equals(activeFilter) ? Color.WHITE
                    : androidx.core.content.ContextCompat.getColor(this, R.color.collab_text_secondary));
            chip.setPadding(dp(13), dp(7), dp(13), dp(7));
            chip.setOnClickListener(v -> {
                activeFilter = key;
                renderFilters();
                renderActivities();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            filtersContainer.addView(chip, lp);
        }
    }

    private void loadActivities() {
        renderNotice(R.drawable.admin_ic_calendar_heart, 0xFF85E2C7,
                "Consultando actividades…",
                "Verificando talleres y eventos publicados para ti.");
        if (pendingCall != null)
            pendingCall.cancel();
        pendingCall = ApiClient.getService().getOwnWellnessActivities(token);
        pendingCall.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                pendingCall = null;
                if (!sameAccount() || isFinishing())
                    return;
                ApiResponse<JsonObject> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccessful() && body.getData() != null) {
                    JsonObject data = body.getData();
                    JsonArray rows = data.has("actividades") && data.get("actividades").isJsonArray()
                            ? data.getAsJsonArray("actividades")
                            : (data.has("planes") && data.get("planes").isJsonArray()
                                    ? data.getAsJsonArray("planes")
                                    : new JsonArray());
                    visibleActivities.clear();
                    for (JsonElement el : rows) {
                        if (!el.isJsonObject())
                            continue;
                        JsonObject item = el.getAsJsonObject();
                        boolean pubFlag = item.has("publicada") && !item.get("publicada").isJsonNull()
                                && item.get("publicada").getAsBoolean();
                        PublicationStatus pub = AdminWellnessActivityHelper.normalizePublicationStatus(
                                cleanString(item, "estado_publicacion"), pubFlag);
                        VisibilityScope vis = AdminWellnessActivityHelper.normalizeVisibilityScope(
                                cleanString(item, "visibilidad"));
                        if (CollaboratorWellnessAssistantHelper.isActivityVisibleForCollaborator(
                                pub, vis, cleanString(item, "estado"))) {
                            visibleActivities.add(item);
                        }
                    }
                    renderActivities();
                } else {
                    visibleActivities.clear();
                    if (response.code() == 404) {
                        renderNotice(R.drawable.admin_ic_calendar_heart,
                                androidx.core.content.ContextCompat.getColor(
                                        CollaboratorWellnessActivitiesActivity.this, R.color.collab_purple_accent),
                                "Sin actividades disponibles por ahora",
                                "Las actividades y eventos publicados por el Administrador aparecerán aquí en cuanto el servidor habilite el listado GET bienestar/actividades.");
                    } else if (response.code() == 401) {
                        renderNotice(R.drawable.admin_ic_shield_check,
                                androidx.core.content.ContextCompat.getColor(
                                        CollaboratorWellnessActivitiesActivity.this, R.color.collab_status_overdue),
                                "Sesión vencida", "Vuelve a iniciar sesión para consultar tus actividades.");
                    } else {
                        renderNotice(R.drawable.admin_ic_alert_triangle,
                                androidx.core.content.ContextCompat.getColor(
                                        CollaboratorWellnessActivitiesActivity.this, R.color.collab_status_overdue),
                                "No se pudieron cargar las actividades",
                                "Respuesta del servidor HTTP " + response.code()
                                        + ". Toca actualizar para reintentar.");
                    }
                }
                if (response.errorBody() != null)
                    response.errorBody().close();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                pendingCall = null;
                if (sameAccount() && !isFinishing()) {
                    renderNotice(R.drawable.admin_ic_alert_triangle,
                            androidx.core.content.ContextCompat.getColor(CollaboratorWellnessActivitiesActivity.this,
                                    R.color.collab_status_overdue),
                            "Sin conexión",
                            "No se pudo consultar el servidor. Toca actualizar cuando vuelva tu conexión.");
                }
            }
        });
    }

    private void renderActivities() {
        listContainer.removeAllViews();
        if (visibleActivities.isEmpty()) {
            renderNotice(R.drawable.admin_ic_calendar_heart,
                    androidx.core.content.ContextCompat.getColor(this, R.color.collab_status_completed),
                    "No hay actividades publicadas por ahora",
                    "Cuando el Administrador publique talleres, charlas o actividades para ti, aparecerán aquí.");
            return;
        }

        int shown = 0;
        for (JsonObject item : visibleActivities) {
            ExecutionStatus exec = AdminWellnessActivityHelper.normalizeExecutionStatus(
                    cleanString(item, "estado"));
            ActivityDisplayState state = CollaboratorWellnessAssistantHelper.classifyActivityState(exec);
            if (!"TODAS".equals(activeFilter) && !state.name().equals(activeFilter)) {
                continue;
            }
            shown++;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.admin_bg_wellness_surface);
            card.setPadding(dp(15), dp(14), dp(15), dp(14));

            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);

            int textPrimary = androidx.core.content.ContextCompat.getColor(this, R.color.collab_text_primary);
            int textSecondary = androidx.core.content.ContextCompat.getColor(this, R.color.collab_text_secondary);

            TextView title = new TextView(this);
            title.setText(cleanString(item, "nombre"));
            title.setTextColor(textPrimary);
            title.setTextSize(15f);
            title.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView badge = buildStateBadge(state);
            head.addView(badge);
            card.addView(head);

            String objective = cleanString(item, "objetivo");
            if (!objective.isEmpty()) {
                TextView tvObj = new TextView(this);
                tvObj.setText(objective);
                tvObj.setTextColor(textPrimary);
                tvObj.setTextSize(13f);
                tvObj.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                oLp.topMargin = dp(6);
                card.addView(tvObj, oLp);
            }

            String schedule = AdminWellnessActivityHelper.formatScheduleDisplay(
                    cleanString(item, "fecha_programada"),
                    cleanString(item, "hora_inicio"),
                    cleanString(item, "hora_fin"));
            String responsible = cleanString(item, "responsable");
            String area = AdminWellnessActivityHelper.formatCategoryLabel(cleanString(item, "dimension"));
            String notes = cleanString(item, "observaciones");

            StringBuilder meta = new StringBuilder();
            meta.append("Fecha y horario: ").append(schedule);
            meta.append("\nÁrea: ").append(area);
            if (!responsible.isEmpty()) {
                meta.append("\nResponsable: ").append(responsible);
            }
            if (!notes.isEmpty()) {
                meta.append("\nDetalles: ").append(notes);
            }

            TextView tvMeta = new TextView(this);
            tvMeta.setText(meta.toString());
            tvMeta.setTextColor(textSecondary);
            tvMeta.setTextSize(12f);
            tvMeta.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mLp.topMargin = dp(8);
            card.addView(tvMeta, mLp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(11);
            listContainer.addView(card, lp);
        }

        if (shown == 0) {
            renderNotice(R.drawable.admin_ic_search,
                    androidx.core.content.ContextCompat.getColor(this, R.color.collab_purple_accent),
                    "Sin actividades en este estado",
                    "Prueba seleccionando otro filtro para consultar todas las actividades publicadas.");
        } else {
            CollaboratorThemeHelper.applyReadableTypeface(listContainer);
        }
    }

    private TextView buildStateBadge(ActivityDisplayState state) {
        TextView badge = new TextView(this);
        badge.setText(state.label);
        badge.setTextSize(10.5f);
        badge.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        int bgColor;
        int txtColor;
        if (state == ActivityDisplayState.EN_CURSO) {
            bgColor = androidx.core.content.ContextCompat.getColor(this, R.color.collab_badge_green_bg);
            txtColor = androidx.core.content.ContextCompat.getColor(this, R.color.collab_status_completed);
        } else if (state == ActivityDisplayState.FINALIZADA) {
            bgColor = androidx.core.content.ContextCompat.getColor(this, R.color.collab_bg_surface_secondary);
            txtColor = androidx.core.content.ContextCompat.getColor(this, R.color.collab_text_secondary);
        } else {
            bgColor = androidx.core.content.ContextCompat.getColor(this, R.color.collab_badge_purple_bg);
            txtColor = androidx.core.content.ContextCompat.getColor(this, R.color.collab_purple_accent);
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(99));
        bg.setColor(bgColor);
        badge.setBackground(bg);
        badge.setTextColor(txtColor);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        return badge;
    }

    private void renderNotice(int iconRes, int tint, String title, String subtitle) {
        listContainer.removeAllViews();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackgroundResource(R.drawable.admin_bg_wellness_surface);
        card.setPadding(dp(20), dp(22), dp(20), dp(22));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(tint));
        card.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.collab_text_primary));
        t.setTextSize(15f);
        t.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(10);
        card.addView(t, tLp);

        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.collab_text_secondary));
        s.setTextSize(12.5f);
        s.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(5);
        card.addView(s, sLp);

        listContainer.addView(card);
        CollaboratorThemeHelper.applyReadableTypeface(listContainer);
    }

    private boolean sameAccount() {
        SessionManager s = new SessionManager(this);
        return s.isLoggedIn() && s.getUserId() == userId && token.equals(s.getBearerToken());
    }

    @Override
    protected void onStop() {
        if (pendingCall != null) {
            pendingCall.cancel();
            pendingCall = null;
        }
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

    private static String cleanString(JsonObject o, String key) {
        if (o == null || !o.has(key))
            return "";
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull())
            return "";
        String val = e.getAsString().trim();
        return "—".equals(val) ? "" : val;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
