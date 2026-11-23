package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.AssistantRole;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.BlockType;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.ResponseBlock;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.SuggestedQuestion;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Asistente de bienestar — Chat de orientación general para el Colaborador.
 * - Reutiliza el logo oficial original de Website (collab_img_website_logo) sin W genérica.
 * - Responde dinámicamente a Modo Claro y Modo Oscuro de SIGD.
 * - Conserva consultas en vivo a actividades publicadas y orientación local transparente.
 */
public final class CollaboratorWellnessAssistantActivity extends AppCompatActivity {

    private Call<ApiResponse<JsonObject>> pendingActivitiesCall;

    private ScrollView scrollView;
    private LinearLayout messagesContainer;
    private LinearLayout suggestionsContainer;
    private EditText inputField;
    private View sendBtn;
    private View loadingBubble;

    private boolean busy;
    private boolean lastSenderWasAssistant;
    private String token;
    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_wellness_assistant);
        CollaboratorThemeHelper.setupSystemBars(this);

        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        scrollView = findViewById(R.id.collab_assistant_scroll);
        messagesContainer = findViewById(R.id.collab_assistant_messages);
        suggestionsContainer = findViewById(R.id.collab_assistant_suggestions);
        inputField = findViewById(R.id.collab_assistant_input);
        sendBtn = findViewById(R.id.collab_assistant_btn_send);

        findViewById(R.id.collab_assistant_btn_back).setOnClickListener(v -> finish());
        sendBtn.setOnClickListener(v -> sendTypedQuestion());
        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTypedQuestion();
                return true;
            }
            return false;
        });

        addAssistantMessage(AssistantRole.COLLABORATOR.welcomeMessage, false);
        renderSuggestedQuestions();
        CollaboratorThemeHelper.applyReadableTypeface(findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void renderSuggestedQuestions() {
        suggestionsContainer.removeAllViews();
        int textPrimary = ContextCompat.getColor(this, R.color.collab_text_primary);
        int accentIcon = ContextCompat.getColor(this, R.color.collab_purple_primary);
        int chevronTint = ContextCompat.getColor(this, R.color.collab_text_muted);

        List<SuggestedQuestion> questions = CollaboratorWellnessAssistantHelper.collaboratorSuggestedQuestions();
        for (int i = 0; i < questions.size(); i++) {
            SuggestedQuestion item = questions.get(i);
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setBackgroundResource(R.drawable.admin_bg_wellness_card);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setPadding(dp(12), dp(11), dp(12), dp(11));

            ImageView icon = new ImageView(this);
            icon.setImageResource(iconForQuestion(item.id));
            icon.setImageTintList(ColorStateList.valueOf(accentIcon));
            chip.addView(icon, new LinearLayout.LayoutParams(dp(17), dp(17)));

            TextView lbl = new TextView(this);
            lbl.setText(item.question);
            lbl.setTextColor(textPrimary);
            lbl.setTextSize(12.5f);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lLp.setMarginStart(dp(10));
            lLp.setMarginEnd(dp(8));
            chip.addView(lbl, lLp);

            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.admin_ic_chevron_right);
            arrow.setImageTintList(ColorStateList.valueOf(chevronTint));
            chip.addView(arrow, new LinearLayout.LayoutParams(dp(15), dp(15)));

            chip.setOnClickListener(v -> handleQuestion(item.question));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(8);
            suggestionsContainer.addView(chip, lp);
        }
        CollaboratorThemeHelper.applyReadableTypeface(suggestionsContainer);
    }

    @DrawableRes
    private static int iconForQuestion(String id) {
        if ("time".equals(id)) return R.drawable.collaborator_ic_clock;
        if ("workload".equals(id)) return R.drawable.collab_ic_notif_clipboard_check;
        if ("communication".equals(id)) return R.drawable.collab_ic_notif_heart_handshake;
        if ("rest".equals(id)) return R.drawable.collab_ic_heart_pulse;
        return R.drawable.admin_ic_calendar_heart;
    }

    private void sendTypedQuestion() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) {
            inputField.setError("Escribe una consulta");
            return;
        }
        inputField.setText("");
        handleQuestion(text);
    }

    private void handleQuestion(String question) {
        if (busy) return;
        addUserMessage(question);
        boolean urgent = CollaboratorWellnessAssistantHelper.containsUrgentCue(question);

        if (CollaboratorWellnessAssistantHelper.isActivitiesAvailabilityQuery(question)) {
            queryAuthorizedActivities(urgent);
            return;
        }

        setBusy(true);
        showLoadingBubble("Preparando orientación…");
        scrollView.postDelayed(() -> {
            if (isFinishing()) return;
            hideLoadingBubble();
            setBusy(false);
            String response = CollaboratorWellnessAssistantHelper.localGuidanceForQuestion(question);
            addAssistantMessage(response, urgent);
        }, 180L);
    }

    private void queryAuthorizedActivities(boolean urgent) {
        setBusy(true);
        showLoadingBubble("Consultando actividades publicadas…");
        if (pendingActivitiesCall != null) pendingActivitiesCall.cancel();
        pendingActivitiesCall = ApiClient.getService().getOwnWellnessActivities(token);
        pendingActivitiesCall.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                pendingActivitiesCall = null;
                hideLoadingBubble();
                setBusy(false);
                if (!sameAccount() || isFinishing()) return;

                ApiResponse<JsonObject> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccessful() && body.getData() != null) {
                    JsonObject data = body.getData();
                    JsonArray rows = data.has("actividades") && data.get("actividades").isJsonArray()
                            ? data.getAsJsonArray("actividades")
                            : (data.has("planes") && data.get("planes").isJsonArray()
                            ? data.getAsJsonArray("planes") : new JsonArray());
                    List<String> lines = new ArrayList<>();
                    for (JsonElement el : rows) {
                        if (!el.isJsonObject()) continue;
                        JsonObject item = el.getAsJsonObject();
                        boolean pubFlag = item.has("publicada") && !item.get("publicada").isJsonNull()
                                && item.get("publicada").getAsBoolean();
                        PublicationStatus pub = AdminWellnessActivityHelper.normalizePublicationStatus(
                                cleanString(item, "estado_publicacion"), pubFlag);
                        VisibilityScope vis = AdminWellnessActivityHelper.normalizeVisibilityScope(
                                cleanString(item, "visibilidad"));
                        if (!CollaboratorWellnessAssistantHelper.isActivityVisibleForCollaborator(
                                pub, vis, cleanString(item, "estado"))) {
                            continue;
                        }
                        String schedule = AdminWellnessActivityHelper.formatScheduleDisplay(
                                cleanString(item, "fecha_programada"),
                                cleanString(item, "hora_inicio"),
                                cleanString(item, "hora_fin"));
                        lines.add("- " + cleanString(item, "nombre") + " (" + schedule + ")");
                    }
                    if (lines.isEmpty()) {
                        addAssistantMessage(
                                "Actividades disponibles:\nPor el momento no tienes actividades ni eventos publicados para tu cuenta.",
                                urgent);
                    } else {
                        StringBuilder sb = new StringBuilder("Actividades publicadas para ti:\n");
                        for (String l : lines) sb.append(l).append("\n");
                        sb.append("Puedes ver todos los detalles en la sección Actividades y eventos.");
                        addAssistantMessage(sb.toString().trim(), urgent);
                    }
                } else {
                    String msg = response.code() == 404
                            ? "Actividades disponibles:\nEl servidor aún no habilita el listado de actividades publicadas (GET bienestar/actividades). No se muestran eventos ficticios."
                            : "No se pudo consultar tus actividades publicadas (HTTP " + response.code() + ").";
                    addAssistantMessage(msg, urgent);
                }
                if (response.errorBody() != null) response.errorBody().close();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                pendingActivitiesCall = null;
                hideLoadingBubble();
                setBusy(false);
                if (sameAccount() && !isFinishing()) {
                    addAssistantMessage("Sin conexión con el servidor para consultar actividades disponibles.", urgent);
                }
            }
        });
    }

    private void addUserMessage(String text) {
        lastSenderWasAssistant = false;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(13.5f);
        bubble.setLineSpacing(dp(2), 1f);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadii(new float[]{
                dp(16), dp(16), dp(16), dp(16), dp(4), dp(4), dp(16), dp(16)
        });
        bg.setColor(ContextCompat.getColor(this, R.color.collab_purple_primary));
        bubble.setBackground(bg);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setMaxWidth(dp(280));
        row.addView(bubble);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        messagesContainer.addView(row, lp);
        CollaboratorThemeHelper.applyReadableTypeface(row);
        scrollToBottom();
    }

    private void addAssistantMessage(String rawText, boolean includeUrgentNotice) {
        int cardBg = ContextCompat.getColor(this, R.color.collab_bg_surface);
        int cardStroke = ContextCompat.getColor(this, R.color.collab_border);
        int textPrimary = ContextCompat.getColor(this, R.color.collab_text_primary);
        int headingColor = ContextCompat.getColor(this, R.color.collab_purple_accent);
        int bulletColor = ContextCompat.getColor(this, R.color.collab_status_completed);

        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);

        // Logo oficial original de Website (sin W genérica y sin repetir en mensajes consecutivos)
        if (!lastSenderWasAssistant) {
            LinearLayout brandRow = new LinearLayout(this);
            brandRow.setOrientation(LinearLayout.HORIZONTAL);
            brandRow.setGravity(Gravity.CENTER_VERTICAL);

            ImageView avatar = new ImageView(this);
            avatar.setImageResource(R.drawable.collab_img_website_logo);
            avatar.setAdjustViewBounds(true);
            avatar.setScaleType(ImageView.ScaleType.FIT_START);
            avatar.setContentDescription("Website");
            brandRow.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(22)));

            LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            brLp.bottomMargin = dp(5);
            brLp.setMarginStart(dp(2));
            group.addView(brandRow, brLp);
        }
        lastSenderWasAssistant = true;

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadii(new float[]{
                dp(4), dp(4), dp(16), dp(16), dp(16), dp(16), dp(16), dp(16)
        });
        bg.setColor(cardBg);
        bg.setStroke(dp(1), cardStroke);
        bubble.setBackground(bg);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(12));

        List<ResponseBlock> blocks = AdminWellnessAssistantHelper.formatStructuredResponse(rawText);
        for (int i = 0; i < blocks.size(); i++) {
            ResponseBlock block = blocks.get(i);
            if (block.type == BlockType.HEADING) {
                TextView h = new TextView(this);
                h.setText(block.text);
                h.setTextColor(headingColor);
                h.setTextSize(13f);
                h.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
                LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) hLp.topMargin = dp(8);
                bubble.addView(h, hLp);
            } else if (block.type == BlockType.BULLET) {
                LinearLayout bulletRow = new LinearLayout(this);
                bulletRow.setOrientation(LinearLayout.HORIZONTAL);
                TextView dot = new TextView(this);
                dot.setText("•");
                dot.setTextColor(bulletColor);
                dot.setTextSize(14f);
                dot.setTypeface(null, Typeface.BOLD);
                bulletRow.addView(dot);

                TextView body = new TextView(this);
                body.setText(block.text);
                body.setTextColor(textPrimary);
                body.setTextSize(13f);
                body.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                bLp.setMarginStart(dp(7));
                bulletRow.addView(body, bLp);

                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.topMargin = dp(4);
                bubble.addView(bulletRow, rLp);
            } else {
                TextView p = new TextView(this);
                p.setText(block.text);
                p.setTextColor(textPrimary);
                p.setTextSize(13f);
                p.setLineSpacing(dp(2), 1f);
                LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) pLp.topMargin = dp(6);
                bubble.addView(p, pLp);
            }
        }

        if (includeUrgentNotice) {
            TextView urgentTv = new TextView(this);
            urgentTv.setText(AdminWellnessAssistantHelper.URGENT_SUPPORT_NOTICE);
            urgentTv.setTextColor(bulletColor);
            urgentTv.setTextSize(11.5f);
            urgentTv.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
            urgentTv.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            uLp.topMargin = dp(8);
            bubble.addView(urgentTv, uLp);
        }

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.setMarginEnd(dp(18));
        group.addView(bubble, bLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        messagesContainer.addView(group, lp);
        CollaboratorThemeHelper.applyReadableTypeface(group);
        scrollToBottom();
    }

    private void showLoadingBubble(String labelText) {
        hideLoadingBubble();
        int secondaryText = ContextCompat.getColor(this, R.color.collab_text_secondary);
        int accentColor = ContextCompat.getColor(this, R.color.collab_purple_primary);

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
        pill.setPadding(dp(12), dp(9), dp(12), dp(9));

        ProgressBar pb = new ProgressBar(this);
        pb.setIndeterminateTintList(ColorStateList.valueOf(accentColor));
        pill.addView(pb, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView lbl = new TextView(this);
        lbl.setText(labelText);
        lbl.setTextColor(secondaryText);
        lbl.setTextSize(12f);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMarginStart(dp(8));
        pill.addView(lbl, tLp);

        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = dp(10);
        messagesContainer.addView(pill, rLp);
        loadingBubble = pill;
        scrollToBottom();
    }

    private void hideLoadingBubble() {
        if (loadingBubble != null) {
            messagesContainer.removeView(loadingBubble);
            loadingBubble = null;
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        sendBtn.setEnabled(!value);
        sendBtn.setAlpha(value ? 0.6f : 1f);
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private boolean sameAccount() {
        SessionManager s = new SessionManager(this);
        return s.isLoggedIn() && s.getUserId() == userId && token.equals(s.getBearerToken());
    }

    @Override
    protected void onStop() {
        if (pendingActivitiesCall != null) {
            pendingActivitiesCall.cancel();
            pendingActivitiesCall = null;
        }
        super.onStop();
    }

    private static String cleanString(JsonObject o, String key) {
        if (o == null || !o.has(key)) return "";
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return "";
        String val = e.getAsString().trim();
        return "—".equals(val) ? "" : val;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
