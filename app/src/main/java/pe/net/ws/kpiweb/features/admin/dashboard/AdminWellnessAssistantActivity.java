package pe.net.ws.kpiweb.features.admin.dashboard;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.AssistantRole;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.BlockType;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.ResponseBlock;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.SuggestedQuestion;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Interfaz conversacional premium del Asistente SIGD (Orientación organizacional — Administrador).
 * Conserva el mecanismo real POST bienestar/admin/asistente, reutiliza el logo oficial transparente
 * SIGD by website y protege estrictamente la privacidad individual.
 */
public final class AdminWellnessAssistantActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_QUESTION = "extra_initial_question";

    private final List<Call<ApiResponse<JsonObject>>> activeCalls = new ArrayList<>();

    private ScrollView scrollView;
    private LinearLayout messagesContainer;
    private LinearLayout suggestionsList;
    private EditText inputField;
    private View sendBtn;
    private View loadingBubble;

    private boolean waitingResponse;
    private String token;
    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_wellness_assistant);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        scrollView = findViewById(R.id.assistant_scroll);
        messagesContainer = findViewById(R.id.assistant_messages_container);
        suggestionsList = findViewById(R.id.assistant_suggestions_list);
        inputField = findViewById(R.id.assistant_input);
        sendBtn = findViewById(R.id.assistant_btn_send);

        findViewById(R.id.assistant_btn_back).setOnClickListener(v -> finish());
        sendBtn.setOnClickListener(v -> sendTypedQuestion());
        inputField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTypedQuestion();
                return true;
            }
            return false;
        });

        // Mensaje de bienvenida del Asistente SIGD
        addAssistantMessage(AssistantRole.ADMIN.welcomeMessage, null, false);
        renderSuggestedQuestions();

        AdminTypography.apply(this, findViewById(android.R.id.content));

        String initial = getIntent().getStringExtra(EXTRA_INITIAL_QUESTION);
        if (initial != null && !initial.trim().isEmpty()) {
            sendQuestion(initial.trim());
        }
    }

    private void renderSuggestedQuestions() {
        suggestionsList.removeAllViews();
        List<SuggestedQuestion> items = AdminWellnessAssistantHelper.adminSuggestedQuestions();
        for (int i = 0; i < items.size(); i++) {
            SuggestedQuestion item = items.get(i);
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setBackgroundResource(R.drawable.admin_bg_wellness_card);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setPadding(dp(12), dp(10), dp(12), dp(10));

            ImageView icon = new ImageView(this);
            icon.setImageResource(iconForSuggestion(item.id));
            icon.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
            chip.addView(icon, new LinearLayout.LayoutParams(dp(17), dp(17)));

            TextView label = new TextView(this);
            label.setText(item.question);
            label.setTextColor(getColor(R.color.admin_text_primary));
            label.setTextSize(12.5f);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lLp.setMarginStart(dp(10));
            lLp.setMarginEnd(dp(8));
            chip.addView(label, lLp);

            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.admin_ic_chevron_right);
            arrow.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_text_muted)));
            chip.addView(arrow, new LinearLayout.LayoutParams(dp(15), dp(15)));

            chip.setOnClickListener(v -> sendQuestion(item.question));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(8);
            suggestionsList.addView(chip, lp);
        }
        AdminTypography.apply(this, suggestionsList);
    }

    @DrawableRes
    private static int iconForSuggestion(String id) {
        if ("communication".equals(id)) return R.drawable.collab_ic_notif_heart_handshake;
        if ("workload".equals(id)) return R.drawable.admin_ic_calendar_heart;
        if ("activity".equals(id)) return R.drawable.collab_ic_notif_clipboard_check;
        if ("rest".equals(id)) return R.drawable.collaborator_ic_clock;
        return R.drawable.admin_ic_chart_line;
    }

    private void sendTypedQuestion() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) {
            inputField.setError("Escribe una consulta");
            return;
        }
        inputField.setText("");
        sendQuestion(text);
    }

    private void sendQuestion(String question) {
        if (waitingResponse) return;
        addUserMessage(question);
        boolean urgentCue = AdminWellnessAssistantHelper.containsUrgentSupportCue(question);

        waitingResponse = true;
        sendBtn.setEnabled(false);
        sendBtn.setAlpha(0.6f);
        showLoadingBubble();

        JsonObject body = new JsonObject();
        body.addProperty("pregunta", question);
        Call<ApiResponse<JsonObject>> call = ApiClient.getService().askAdminWellnessAssistant(token, body);
        activeCalls.add(call);
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                activeCalls.remove(c);
                hideLoadingBubble();
                waitingResponse = false;
                sendBtn.setEnabled(true);
                sendBtn.setAlpha(1f);
                if (!sameAccount() || isFinishing()) return;

                ApiResponse<JsonObject> resp = response.body();
                if (response.isSuccessful() && resp != null && resp.isSuccessful() && resp.getData() != null) {
                    JsonObject data = resp.getData();
                    String answer = cleanString(data, "respuesta");
                    String notice = cleanString(data, "aviso");
                    if (answer.isEmpty()) {
                        addErrorBubble("El servidor devolvió una respuesta vacía.", question);
                    } else {
                        addAssistantMessage(answer, notice, urgentCue);
                    }
                } else {
                    String msg = response.code() == 403
                            ? "Tu cuenta no tiene permiso para usar el asistente administrativo."
                            : response.code() == 401
                            ? "Sesión vencida. Vuelve a iniciar sesión."
                            : "No se pudo obtener respuesta del servidor (HTTP " + response.code() + ").";
                    addErrorBubble(msg, question);
                }
                if (response.errorBody() != null) response.errorBody().close();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                activeCalls.remove(c);
                hideLoadingBubble();
                waitingResponse = false;
                sendBtn.setEnabled(true);
                sendBtn.setAlpha(1f);
                if (sameAccount() && !isFinishing()) {
                    addErrorBubble("Sin conexión con el servidor. Verifica tu red e inténtalo de nuevo.", question);
                }
            }
        });
    }

    private void addUserMessage(String text) {
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
        bg.setColor(0xFF8B30E0);
        bubble.setBackground(bg);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setMaxWidth(dp(280));
        row.addView(bubble);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        messagesContainer.addView(row, lp);
        AdminTypography.apply(this, row);
        scrollToBottom();
    }

    private void addAssistantMessage(String rawText, @Nullable String serverNotice, boolean includeUrgentNotice) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);

        // Avatar oficial transparente SIGD by website
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.auth_sigd_logo_transparent);
        avatar.setAdjustViewBounds(true);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatar.setContentDescription("Asistente SIGD");
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(28), dp(30));
        avLp.topMargin = dp(2);
        row.addView(avatar, avLp);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadii(new float[]{
                dp(4), dp(4), dp(16), dp(16), dp(16), dp(16), dp(16), dp(16)
        });
        bg.setColor(getColor(R.color.admin_card_bg));
        bg.setStroke(dp(1), getColor(R.color.admin_border));
        bubble.setBackground(bg);
        bubble.setPadding(dp(13), dp(11), dp(13), dp(11));

        int headingColor = getColor(R.color.admin_purple_accent);
        int textPrimary = getColor(R.color.admin_text_primary);
        int textSecondary = getColor(R.color.admin_text_secondary);

        List<ResponseBlock> blocks = AdminWellnessAssistantHelper.formatStructuredResponse(rawText);
        for (int i = 0; i < blocks.size(); i++) {
            ResponseBlock block = blocks.get(i);
            if (block.type == BlockType.HEADING) {
                TextView h = new TextView(this);
                h.setText(block.text);
                h.setTextColor(headingColor);
                h.setTextSize(13f);
                h.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) hLp.topMargin = dp(8);
                bubble.addView(h, hLp);
            } else if (block.type == BlockType.BULLET) {
                LinearLayout bulletRow = new LinearLayout(this);
                bulletRow.setOrientation(LinearLayout.HORIZONTAL);
                TextView dot = new TextView(this);
                dot.setText("•");
                dot.setTextColor(headingColor);
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

        if (serverNotice != null && !serverNotice.trim().isEmpty()) {
            TextView noticeTv = new TextView(this);
            noticeTv.setText(serverNotice);
            noticeTv.setTextColor(textSecondary);
            noticeTv.setTextSize(11.5f);
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nLp.topMargin = dp(8);
            bubble.addView(noticeTv, nLp);
        }

        if (includeUrgentNotice) {
            TextView urgentTv = new TextView(this);
            urgentTv.setText(AdminWellnessAssistantHelper.URGENT_SUPPORT_NOTICE);
            urgentTv.setTextColor(getColor(R.color.admin_status_delivered));
            urgentTv.setTextSize(11.5f);
            urgentTv.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
            urgentTv.setPadding(dp(10), dp(8), dp(10), dp(8));
            LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            uLp.topMargin = dp(8);
            bubble.addView(urgentTv, uLp);
        }

        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bLp.setMarginStart(dp(8));
        bLp.setMarginEnd(dp(18));
        row.addView(bubble, bLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        messagesContainer.addView(row, lp);
        AdminTypography.apply(this, row);
        scrollToBottom();
    }

    private void showLoadingBubble() {
        hideLoadingBubble();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.auth_sigd_logo_transparent);
        avatar.setAdjustViewBounds(true);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(avatar, new LinearLayout.LayoutParams(dp(28), dp(30)));

        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
        pill.setPadding(dp(12), dp(9), dp(12), dp(9));

        ProgressBar pb = new ProgressBar(this);
        pb.setIndeterminateTintList(ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
        pill.addView(pb, new LinearLayout.LayoutParams(dp(16), dp(16)));

        TextView lbl = new TextView(this);
        lbl.setText("Consultando orientación…");
        lbl.setTextColor(getColor(R.color.admin_text_secondary));
        lbl.setTextSize(12f);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMarginStart(dp(8));
        pill.addView(lbl, tLp);

        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.setMarginStart(dp(8));
        row.addView(pill, pLp);

        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = dp(10);
        messagesContainer.addView(row, rLp);
        loadingBubble = row;
        scrollToBottom();
    }

    private void hideLoadingBubble() {
        if (loadingBubble != null) {
            messagesContainer.removeView(loadingBubble);
            loadingBubble = null;
        }
    }

    private void addErrorBubble(String message, String retryQuestion) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.admin_bg_wellness_stat_card);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView msg = new TextView(this);
        msg.setText(message);
        msg.setTextColor(getColor(R.color.admin_status_overdue));
        msg.setTextSize(12.5f);
        box.addView(msg);

        TextView retry = new TextView(this);
        retry.setText("Reintentar consulta");
        retry.setTextColor(getColor(R.color.admin_text_primary));
        retry.setTextSize(12f);
        retry.setTypeface(null, Typeface.BOLD);
        retry.setBackgroundResource(R.drawable.admin_bg_wellness_secondary_btn);
        retry.setPadding(dp(12), dp(7), dp(12), dp(7));
        retry.setOnClickListener(v -> sendQuestion(retryQuestion));
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = dp(8);
        box.addView(retry, rLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        messagesContainer.addView(box, lp);
        AdminTypography.apply(this, box);
        scrollToBottom();
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
        for (Call<ApiResponse<JsonObject>> c : activeCalls) c.cancel();
        activeCalls.clear();
        super.onStop();
    }

    private static String cleanString(JsonObject o, String key) {
        if (o == null || !o.has(key)) return "";
        JsonElement el = o.get(key);
        if (el == null || el.isJsonNull()) return "";
        String val = el.getAsString().trim();
        return "—".equals(val) ? "" : val;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
