package pe.net.ws.kpiweb.features.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;
import pe.net.ws.kpiweb.core.session.FaceDeviceStore;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.FaceEnrollmentActivity;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;
import pe.net.ws.kpiweb.features.auth.models.LoginRequest;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.settings.data.SettingsPreferences;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla compartida "Seguridad y dispositivos" para Administrador y Colaborador.
 * Utiliza el tema oscuro morado SIGD y conserva todos los flujos seguros de consulta,
 * verificación de contraseña, recuperación biométrica y revocación en el servidor.
 */
public final class SecurityDevicesActivity extends AppCompatActivity {

    private static final int COLOR_BG_MAIN = Color.parseColor("#11091F");
    private static final int COLOR_CARD = Color.parseColor("#251630");
    private static final int COLOR_SURFACE = Color.parseColor("#302044");
    private static final int COLOR_VIOLET = Color.parseColor("#8B30E0");
    private static final int COLOR_LAVENDER = Color.parseColor("#D8B4FE");
    private static final int COLOR_MINT = Color.parseColor("#57D6A0");
    private static final int COLOR_CORAL = Color.parseColor("#FF819D");
    private static final int COLOR_BORDER = Color.parseColor("#674182");

    private enum FeedbackTone {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private SessionManager session;
    private TextView status;
    private View statusCard;
    private ImageView statusIcon;
    private ProgressBar progressBar;

    private TextView tvDeviceModel;
    private View linkPill;
    private ImageView ivLinkIcon;
    private TextView tvLinkStatus;
    private ImageView ivFingerprintIcon;
    private TextView tvFingerprintState;
    private ImageView ivFaceIcon;
    private TextView tvFaceState;
    private TextView tvLastSync;
    private View devicesListWrapper;
    private LinearLayout devicesContainer;

    private Call<?> request;
    private boolean busy;
    private JsonArray devices;
    private JsonArray faces;
    private BiometricPrompt fingerprintPrompt;
    private String lastVerifiedLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        setTheme(R.style.Theme_KPIWeb_SecurityDark);
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            AccountNavigation.login(this, false);
            return;
        }
        setContentView(R.layout.activity_security_devices);
        setupDarkSystemBars();
        bindViews();
        styleStaticBadges();
        setupActions();
        loadStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupDarkSystemBars();
        if (status != null && request == null && !busy) {
            loadStatus();
        }
    }

    private void setupDarkSystemBars() {
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void bindViews() {
        status = findViewById(R.id.security_status);
        statusCard = findViewById(R.id.security_status_card);
        statusIcon = findViewById(R.id.security_status_icon);
        progressBar = findViewById(R.id.security_progress);

        tvDeviceModel = findViewById(R.id.security_device_model);
        linkPill = findViewById(R.id.security_link_pill);
        ivLinkIcon = findViewById(R.id.security_link_icon);
        tvLinkStatus = findViewById(R.id.security_link_status);
        ivFingerprintIcon = findViewById(R.id.security_fingerprint_icon);
        tvFingerprintState = findViewById(R.id.security_fingerprint_state);
        ivFaceIcon = findViewById(R.id.security_face_icon);
        tvFaceState = findViewById(R.id.security_face_state);
        tvLastSync = findViewById(R.id.security_last_sync);
        devicesListWrapper = findViewById(R.id.security_devices_list_wrapper);
        devicesContainer = findViewById(R.id.security_devices_container);

        if (tvDeviceModel != null) {
            tvDeviceModel.setText(SecurityDevicesUiHelper.formatDeviceModel(Build.MANUFACTURER, Build.MODEL));
        }
        applyLinkPillStyle(Color.parseColor("#338B30E0"), COLOR_BORDER, COLOR_LAVENDER);
    }

    private void styleStaticBadges() {
        applyRoundedBadge(findViewById(R.id.security_header_shield_badge), Color.parseColor("#338B30E0"), 12);
        applyRoundedBadge(findViewById(R.id.security_hero_phone_badge), Color.parseColor("#338B30E0"), 13);
        applyRoundedBadge(findViewById(R.id.security_refresh_badge), Color.parseColor("#33FFFFFF"), 12);
        applyRoundedBadge(findViewById(R.id.security_badge_revoke_face), Color.parseColor("#29FF819D"), 12);
        applyRoundedBadge(findViewById(R.id.security_badge_revoke_fp), Color.parseColor("#29FF819D"), 12);
        applyRoundedBadge(findViewById(R.id.security_badge_link_fp), Color.parseColor("#2457D6A0"), 12);
        applyRoundedBadge(findViewById(R.id.security_badge_link_face), Color.parseColor("#2E8B30E0"), 12);
        applyRoundedBadge(findViewById(R.id.security_badge_revoke_all), Color.parseColor("#38FF819D"), 12);
        applyRoundedBadge(findViewById(R.id.security_badge_info), Color.parseColor("#2E8B30E0"), 10);

        applyRoundedBadge(findViewById(R.id.security_tag_warn_face), Color.parseColor("#2BFF819D"), 99);
        applyRoundedBadge(findViewById(R.id.security_tag_warn_fp), Color.parseColor("#2BFF819D"), 99);
        applyRoundedBadge(findViewById(R.id.security_tag_critical), Color.parseColor("#3DFF819D"), 99);
    }

    private void setupActions() {
        View btnBack = findViewById(R.id.security_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        bindActionCard(R.id.security_refresh, this::loadStatus);
        bindActionCard(R.id.security_revoke_face, () -> chooseDevice("ROSTRO"));
        bindActionCard(R.id.security_revoke_android, () -> chooseDevice("HUELLA"));
        bindActionCard(R.id.security_revoke_both, () -> chooseDevice("AMBOS"));
        bindActionCard(R.id.security_link_face, () ->
                startActivity(new Intent(this, FaceEnrollmentActivity.class)));
        bindActionCard(R.id.security_link_android, () -> {
            AndroidDeviceStore local = new AndroidDeviceStore(this);
            if (local.isFingerprintDisabled() && usernameMatches(local)) {
                chooseDevice("RECUPERAR_HUELLA");
                return;
            }
            new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KPIWeb_SecurityDarkDialog)
                    .setTitle("Registrar una clave nueva")
                    .setMessage("Si reinstalaste la app, inicia con contraseña y registra una clave biométrica nueva. Si la clave de esta instalación todavía existe, se puede recuperar con contraseña reciente y una firma confirmada con biometría. Una clave revocada no se reactiva.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Ir al inicio de sesión", (d, w) -> AccountNavigation.signOut(this))
                    .show();
        });
    }

    private void bindActionCard(int viewId, Runnable action) {
        View card = findViewById(viewId);
        if (card == null) return;
        card.setOnClickListener(v -> action.run());
        if (!SettingsPreferences.getInstance(this).isReduceAnimations()) {
            card.setOnTouchListener((v, event) -> {
                int act = event.getActionMasked();
                if (act == MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(90L).start();
                } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120L).start();
                }
                return false;
            });
        }
    }

    private SecurityDevicesUiHelper.HardwareStatus detectFingerprintHardware() {
        int result = BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (result == BiometricManager.BIOMETRIC_SUCCESS) {
            return SecurityDevicesUiHelper.HardwareStatus.AVAILABLE;
        }
        if (result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            return SecurityDevicesUiHelper.HardwareStatus.NOT_CONFIGURED;
        }
        return SecurityDevicesUiHelper.HardwareStatus.UNAVAILABLE;
    }

    private SecurityDevicesUiHelper.HardwareStatus detectFaceHardware() {
        PackageManager pm = getPackageManager();
        boolean hasCamera = pm != null && (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY));
        return hasCamera
                ? SecurityDevicesUiHelper.HardwareStatus.AVAILABLE
                : SecurityDevicesUiHelper.HardwareStatus.UNAVAILABLE;
    }

    private void setBusyState(boolean isBusy) {
        busy = isBusy;
        if (progressBar != null) {
            progressBar.setVisibility(isBusy ? View.VISIBLE : View.GONE);
        }
    }

    private void setFeedback(String message, FeedbackTone tone) {
        if (status != null) {
            status.setText(message);
        }
        int bgColor;
        int strokeColor;
        int accentColor;
        int iconRes;
        switch (tone) {
            case SUCCESS:
                bgColor = ContextCompat.getColor(this, R.color.admin_status_delivered_bg);
                strokeColor = ContextCompat.getColor(this, R.color.sigd_sec_mint);
                accentColor = ContextCompat.getColor(this, R.color.sigd_sec_mint);
                iconRes = R.drawable.security_ic_shield_check;
                break;
            case WARNING:
                bgColor = ContextCompat.getColor(this, R.color.sigd_sec_warn_bg);
                strokeColor = ContextCompat.getColor(this, R.color.sigd_sec_warn_border);
                accentColor = ContextCompat.getColor(this, R.color.sigd_sec_coral);
                iconRes = R.drawable.admin_ic_alert_triangle;
                break;
            case ERROR:
                bgColor = ContextCompat.getColor(this, R.color.sigd_sec_crit_bg);
                strokeColor = ContextCompat.getColor(this, R.color.sigd_sec_crit_border);
                accentColor = ContextCompat.getColor(this, R.color.sigd_sec_coral);
                iconRes = R.drawable.admin_ic_alert_triangle;
                break;
            case INFO:
            default:
                bgColor = ContextCompat.getColor(this, R.color.admin_card_bg);
                strokeColor = ContextCompat.getColor(this, R.color.admin_border);
                accentColor = ContextCompat.getColor(this, R.color.admin_purple_accent);
                iconRes = R.drawable.admin_ic_info_circle;
                break;
        }
        if (statusCard != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bgColor);
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(1), strokeColor);
            statusCard.setBackground(bg);
        }
        if (statusIcon != null) {
            statusIcon.setImageResource(iconRes);
            statusIcon.setColorFilter(accentColor);
        }
    }

    private void renderSnapshot(SecurityDevicesUiHelper.SecuritySnapshot snapshot) {
        if (tvDeviceModel != null) {
            tvDeviceModel.setText(snapshot.deviceDisplayName);
        }
        if (tvLinkStatus != null) {
            tvLinkStatus.setText(snapshot.linkBadgeLabel);
        }
        if (snapshot.linkState == SecurityDevicesUiHelper.LinkState.LINKED) {
            applyLinkPillStyle(Color.parseColor("#2457D6A0"), Color.parseColor("#6657D6A0"), COLOR_MINT);
        } else {
            applyLinkPillStyle(Color.parseColor("#26FF819D"), Color.parseColor("#66FF819D"), COLOR_CORAL);
        }

        if (tvFingerprintState != null) {
            tvFingerprintState.setText(snapshot.fingerprintLabel);
            int fpColor = resolveBiometricColor(snapshot.fingerprintState);
            tvFingerprintState.setTextColor(fpColor);
            if (ivFingerprintIcon != null) {
                ivFingerprintIcon.setColorFilter(fpColor);
            }
        }

        if (tvFaceState != null) {
            tvFaceState.setText(snapshot.faceLabel);
            int faceColor = resolveBiometricColor(snapshot.faceState);
            tvFaceState.setTextColor(faceColor);
            if (ivFaceIcon != null) {
                ivFaceIcon.setColorFilter(faceColor);
            }
        }

        if (tvLastSync != null && lastVerifiedLabel != null) {
            tvLastSync.setText("Última verificación: " + lastVerifiedLabel);
        }

        renderLinkedDevicesList(snapshot.linkedDevices);
        setFeedback(snapshot.summaryMessage,
                snapshot.linkState == SecurityDevicesUiHelper.LinkState.LINKED
                        ? FeedbackTone.SUCCESS
                        : FeedbackTone.INFO);
    }

    private void renderLinkedDevicesList(java.util.List<SecurityDevicesUiHelper.DeviceBindingItem> items) {
        if (devicesListWrapper == null || devicesContainer == null) return;
        devicesContainer.removeAllViews();
        if (items == null || items.isEmpty()) {
            devicesListWrapper.setVisibility(View.GONE);
            return;
        }
        devicesListWrapper.setVisibility(View.VISIBLE);

        int surfaceColor = ContextCompat.getColor(this, R.color.admin_surface_secondary);
        int violetColor = ContextCompat.getColor(this, R.color.admin_purple_primary);
        int borderColor = ContextCompat.getColor(this, R.color.admin_border);
        int textPrimary = ContextCompat.getColor(this, R.color.admin_text_primary);
        int textSecondary = ContextCompat.getColor(this, R.color.admin_text_secondary);
        int mintColor = ContextCompat.getColor(this, R.color.sigd_sec_mint);
        int coralColor = ContextCompat.getColor(this, R.color.sigd_sec_coral);

        for (int i = 0; i < items.size(); i++) {
            SecurityDevicesUiHelper.DeviceBindingItem item = items.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(surfaceColor);
            rowBg.setCornerRadius(dp(12));
            rowBg.setStroke(dp(1), item.isCurrentDevice ? violetColor : borderColor);
            row.setBackground(rowBg);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(8);
            row.setLayoutParams(lp);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textCol.setLayoutParams(colLp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("#" + item.accessId + " • " + item.name
                    + (item.isCurrentDevice ? " (Este equipo)" : ""));
            tvTitle.setTextColor(textPrimary);
            tvTitle.setTextSize(13f);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(tvTitle);

            TextView tvMeta = new TextView(this);
            String fpTxt = item.fingerprintActive ? "Huella vinculada" : "Huella desactivada";
            String faceTxt = item.faceActive ? "Rostro vinculado" : "Sin rostro";
            tvMeta.setText(fpTxt + "  •  " + faceTxt);
            tvMeta.setTextColor(textSecondary);
            tvMeta.setTextSize(11.5f);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            metaLp.topMargin = dp(2);
            tvMeta.setLayoutParams(metaLp);
            textCol.addView(tvMeta);

            row.addView(textCol);

            TextView pill = new TextView(this);
            boolean active = item.fingerprintActive || item.faceActive;
            pill.setText(active ? "Activo" : "Inactivo");
            pill.setTextSize(11f);
            pill.setTypeface(null, android.graphics.Typeface.BOLD);
            pill.setTextColor(active ? mintColor : coralColor);
            pill.setPadding(dp(9), dp(4), dp(9), dp(4));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(active ? Color.parseColor("#2457D6A0") : Color.parseColor("#26FF819D"));
            pillBg.setCornerRadius(dp(99));
            pill.setBackground(pillBg);
            row.addView(pill);

            devicesContainer.addView(row);
        }
    }

    private int resolveBiometricColor(SecurityDevicesUiHelper.BiometricState state) {
        switch (state) {
            case ACTIVE:
                return ContextCompat.getColor(this, R.color.sigd_sec_mint);
            case DISABLED:
                return ContextCompat.getColor(this, R.color.sigd_sec_coral);
            case AVAILABLE:
                return ContextCompat.getColor(this, R.color.admin_text_primary);
            case NOT_CONFIGURED:
            case UNAVAILABLE:
            default:
                return ContextCompat.getColor(this, R.color.admin_text_secondary);
        }
    }

    private void applyLinkPillStyle(int fillColor, int strokeColor, int contentColor) {
        if (linkPill != null) {
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(fillColor);
            pill.setCornerRadius(dp(99));
            pill.setStroke(dp(1), strokeColor);
            linkPill.setBackground(pill);
        }
        if (ivLinkIcon != null) {
            ivLinkIcon.setColorFilter(contentColor);
        }
        if (tvLinkStatus != null) {
            tvLinkStatus.setTextColor(contentColor);
        }
    }

    private void applyRoundedBadge(View view, int color, int radiusDp) {
        if (view == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radiusDp));
        view.setBackground(bg);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void loadStatus() {
        if (busy) return;
        setBusyState(true);
        setFeedback("Consultando vínculos de tu cuenta…", FeedbackTone.INFO);
        if (tvLastSync != null && lastVerifiedLabel == null) {
            tvLastSync.setText("Última verificación: consultando servidor…");
        }
        final String token = session.getAccessToken();
        request = ApiClient.getService().getSecurityDevices(session.getBearerToken());
        @SuppressWarnings("unchecked")
        Call<ApiResponse<JsonObject>> call = (Call<ApiResponse<JsonObject>>) request;
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonObject>> source,
                                   @NonNull Response<ApiResponse<JsonObject>> response) {
                if (stale(source, token)) return;
                setBusyState(false);
                request = null;
                if (response.code() == 401) {
                    expired();
                    return;
                }
                ApiResponse<JsonObject> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful() || envelope.getData() == null) {
                    setFeedback(response.code() == 404 ? "La API de seguridad aún no está publicada."
                            : response.code() == 403 ? "Esta cuenta no tiene acceso a seguridad y dispositivos."
                            : "No se pudo consultar el servidor. Reintenta cuando tengas conexión.",
                            FeedbackTone.ERROR);
                    return;
                }
                JsonObject data = envelope.getData();
                try {
                    if (data.get("id_usuario").getAsInt() != session.getUserId()) {
                        throw new IllegalStateException();
                    }
                    JsonArray android = data.getAsJsonArray("android");
                    JsonArray face = data.getAsJsonArray("rostro");
                    devices = android;
                    faces = face;
                    lastVerifiedLabel = new SimpleDateFormat("'hoy' • HH:mm", Locale.getDefault()).format(new Date());

                    AndroidDeviceStore local = new AndroidDeviceStore(SecurityDevicesActivity.this);
                    String fallbackModel = SecurityDevicesUiHelper.formatDeviceModel(Build.MANUFACTURER, Build.MODEL);
                    SecurityDevicesUiHelper.SecuritySnapshot snapshot = SecurityDevicesUiHelper.buildSnapshot(
                            android,
                            face,
                            local.id(),
                            usernameMatches(local),
                            local.isFingerprintDisabled(),
                            fallbackModel,
                            detectFingerprintHardware(),
                            detectFaceHardware()
                    );
                    renderSnapshot(snapshot);
                } catch (RuntimeException invalid) {
                    setFeedback("La respuesta del servidor no corresponde a esta cuenta.", FeedbackTone.ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonObject>> source, @NonNull Throwable error) {
                if (stale(source, token)) return;
                setBusyState(false);
                request = null;
                setFeedback("Sin conexión con el servidor. Reintenta.", FeedbackTone.ERROR);
            }
        });
    }

    private void chooseDevice(String method) {
        if (busy) return;
        if (devices == null) {
            loadStatus();
            return;
        }
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int n = 0; n < devices.size(); n++) {
            JsonObject row = devices.get(n).getAsJsonObject();
            int id = row.get("id_acceso").getAsInt();
            boolean face = false;
            if (faces != null) {
                for (int f = 0; f < faces.size(); f++) {
                    if (faces.get(f).getAsJsonObject().get("id_acceso_android").getAsInt() == id) {
                        face = true;
                    }
                }
            }
            boolean fingerprint = row.get("huella_habilitada").getAsInt() == 1;
            if ("ROSTRO".equals(method) && !face || "HUELLA".equals(method) && !fingerprint
                    || "RECUPERAR_HUELLA".equals(method) && (fingerprint
                    || new AndroidDeviceStore(this).id() != id)) {
                continue;
            }
            ids.add(id);
            labels.add("#" + id + " " + row.get("nombre").getAsString());
        }
        if (ids.isEmpty()) {
            setFeedback("No hay un vínculo activo de ese método. Actualiza el estado.", FeedbackTone.WARNING);
            return;
        }
        if (ids.size() == 1) {
            confirmRevoke(method, ids.get(0));
            return;
        }
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KPIWeb_SecurityDarkDialog)
                .setTitle("Selecciona el dispositivo")
                .setItems(labels.toArray(new String[0]), (d, selected) -> confirmRevoke(method, ids.get(selected)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmRevoke(String method, int accessId) {
        if (busy) return;
        FrameLayout container = new FrameLayout(this);
        container.setPadding(dp(24), dp(12), dp(24), dp(4));

        EditText password = new EditText(this);
        password.setHint("Contraseña actual");
        password.setHintTextColor(ContextCompat.getColor(this, R.color.admin_text_muted));
        password.setTextColor(ContextCompat.getColor(this, R.color.admin_text_primary));
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(ContextCompat.getColor(this, R.color.admin_surface_secondary));
        inputBg.setCornerRadius(dp(12));
        inputBg.setStroke(dp(1), ContextCompat.getColor(this, R.color.admin_border));
        password.setBackground(inputBg);
        container.addView(password);

        String title;
        String message;
        String positiveLabel;
        if ("RECUPERAR_HUELLA".equals(method)) {
            title = "Recuperar huella";
            message = "Dispositivo #" + accessId + ". El servidor verificará tu contraseña antes de iniciar la recuperación biométrica.";
            positiveLabel = "Continuar";
        } else if ("AMBOS".equals(method)) {
            title = "Desvincular todos los métodos";
            message = "Dispositivo #" + accessId + ".\n\nEsta acción revocará por completo el dispositivo, su huella digital y su reconocimiento facial en el servidor. Si corresponde a este teléfono, se cerrará tu sesión actual y deberás iniciar sesión con contraseña para volver a registrar una clave.\n\nEscribe tu contraseña actual para confirmar.";
            positiveLabel = "Confirmar desvinculación";
        } else if ("ROSTRO".equals(method)) {
            title = "Desvincular rostro";
            message = "Dispositivo #" + accessId + ". Se eliminará únicamente el acceso facial vinculado a este equipo. Ingresa tu contraseña actual para confirmar.";
            positiveLabel = "Desvincular";
        } else {
            title = "Desvincular huella";
            message = "Dispositivo #" + accessId + ". Se desactivará solo la huella digital y se conservará el rostro vinculado. Ingresa tu contraseña actual para confirmar.";
            positiveLabel = "Desvincular";
        }

        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KPIWeb_SecurityDarkDialog)
                .setTitle(title)
                .setMessage(message)
                .setView(container)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(positiveLabel, (dialog, which) -> {
                    String entered = password.getText().toString();
                    password.getText().clear();
                    if (entered.isEmpty()) {
                        setFeedback("Escribe tu contraseña actual.", FeedbackTone.WARNING);
                        return;
                    }
                    reauthenticateAndRevoke(method, accessId, entered);
                })
                .show();
    }

    private void reauthenticateAndRevoke(String method, int accessId, String password) {
        setBusyState(true);
        setFeedback("Confirmando contraseña con el servidor…", FeedbackTone.INFO);
        final int userId = session.getUserId();
        final String username = session.getUsername();
        final String originalToken = session.getAccessToken();
        request = ApiClient.getService().login(new LoginRequest(username, password));
        @SuppressWarnings("unchecked")
        Call<ApiResponse<LoginResponseData>> call = (Call<ApiResponse<LoginResponseData>>) request;
        call.enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponseData>> source,
                                   @NonNull Response<ApiResponse<LoginResponseData>> response) {
                if (stale(source, originalToken)) return;
                request = null;
                LoginResponseData data = response.body() == null ? null : response.body().getData();
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || data == null || data.getUser() == null || data.getUser().getIdUsuario() != userId
                        || !username.equals(data.getUser().getUsername()) || data.getAuth() == null
                        || data.getAuth().getAccessToken() == null) {
                    setBusyState(false);
                    setFeedback(response.code() == 429 ? "Demasiados intentos. Espera el plazo indicado por el servidor."
                            : "Contraseña rechazada o cuenta distinta. No se cambió ningún vínculo.",
                            FeedbackTone.ERROR);
                    return;
                }
                if ("RECUPERAR_HUELLA".equals(method)) {
                    beginFingerprintRecovery(accessId, data.getAuth().getAccessToken(), originalToken);
                } else {
                    revoke(method, accessId, data.getAuth().getAccessToken(), originalToken);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponseData>> source, @NonNull Throwable error) {
                if (stale(source, originalToken)) return;
                setBusyState(false);
                request = null;
                setFeedback("Sin conexión. No se cambió ningún vínculo.", FeedbackTone.ERROR);
            }
        });
    }

    private void beginFingerprintRecovery(int accessId, String freshToken, String originalToken) {
        AndroidDeviceStore local = new AndroidDeviceStore(this);
        if (local.id() != accessId || !usernameMatches(local) || !local.isFingerprintDisabled()) {
            setBusyState(false);
            logoutTemporary(freshToken);
            setFeedback("La clave de este dispositivo no está disponible. Registra una nueva tras iniciar con contraseña.",
                    FeedbackTone.WARNING);
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("id_acceso", accessId);
        body.put("device_uid", local.uid());
        setFeedback("Solicitando desafío para recuperar la huella…", FeedbackTone.INFO);
        Call<ApiResponse<AndroidAccessChallenge>> call = ApiClient.getService()
                .beginFingerprintRecovery("Bearer " + freshToken, body);
        request = call;
        call.enqueue(new Callback<ApiResponse<AndroidAccessChallenge>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AndroidAccessChallenge>> source,
                                   @NonNull Response<ApiResponse<AndroidAccessChallenge>> response) {
                if (stale(source, originalToken)) return;
                request = null;
                AndroidAccessChallenge challenge = response.body() == null ? null : response.body().getData();
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || challenge == null || challenge.accessId != accessId || challenge.challengeId <= 0
                        || challenge.message == null || challenge.message.isEmpty()) {
                    setBusyState(false);
                    logoutTemporary(freshToken);
                    setFeedback("El servidor no autorizó la recuperación. La huella sigue desactivada.",
                            FeedbackTone.ERROR);
                    return;
                }
                promptFingerprintRecovery(local, challenge, freshToken, originalToken);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AndroidAccessChallenge>> source,
                                  @NonNull Throwable error) {
                if (stale(source, originalToken)) return;
                request = null;
                setBusyState(false);
                logoutTemporary(freshToken);
                setFeedback("Sin conexión. La huella sigue desactivada.", FeedbackTone.ERROR);
            }
        });
    }

    private void promptFingerprintRecovery(AndroidDeviceStore local, AndroidAccessChallenge challenge,
                                           String freshToken, String originalToken) {
        final Signature signature;
        try {
            signature = local.signingOperation();
        } catch (GeneralSecurityException unavailable) {
            setBusyState(false);
            logoutTemporary(freshToken);
            setFeedback("La clave biométrica anterior ya no existe. Inicia con contraseña y registra una clave nueva.",
                    FeedbackTone.WARNING);
            return;
        }
        setFeedback("Confirma la huella de este dispositivo…", FeedbackTone.INFO);
        fingerprintPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        if (isFinishing() || isDestroyed() || !originalToken.equals(session.getAccessToken())) return;
                        Signature approved = result.getCryptoObject() == null ? null : result.getCryptoObject().getSignature();
                        if (approved == null) {
                            setBusyState(false);
                            logoutTemporary(freshToken);
                            setFeedback("No se obtuvo una firma biométrica.", FeedbackTone.ERROR);
                            return;
                        }
                        try {
                            approved.update(challenge.message.getBytes(StandardCharsets.UTF_8));
                            confirmFingerprintRecovery(local, challenge,
                                    AndroidDeviceStore.encodeBase64(approved.sign()), freshToken, originalToken);
                        } catch (GeneralSecurityException invalid) {
                            setBusyState(false);
                            logoutTemporary(freshToken);
                            setFeedback("No se pudo firmar el desafío. La huella sigue desactivada.",
                                    FeedbackTone.ERROR);
                        }
                    }

                    @Override
                    public void onAuthenticationError(int code, @NonNull CharSequence message) {
                        setBusyState(false);
                        logoutTemporary(freshToken);
                        setFeedback("Confirmación biométrica cancelada. La huella sigue desactivada.",
                                FeedbackTone.WARNING);
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Recuperar huella")
                .setSubtitle("Confirma la clave de este dispositivo")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancelar")
                .build();
        fingerprintPrompt.authenticate(info, new BiometricPrompt.CryptoObject(signature));
    }

    private void confirmFingerprintRecovery(AndroidDeviceStore local, AndroidAccessChallenge challenge,
                                            String signature, String freshToken, String originalToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("id_acceso", challenge.accessId);
        body.put("device_uid", local.uid());
        body.put("challenge_id", challenge.challengeId);
        body.put("firma_base64", signature);
        setFeedback("Verificando la firma en el servidor…", FeedbackTone.INFO);
        Call<ApiResponse<JsonObject>> call = ApiClient.getService()
                .confirmFingerprintRecovery("Bearer " + freshToken, body);
        request = call;
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonObject>> source,
                                   @NonNull Response<ApiResponse<JsonObject>> response) {
                if (stale(source, originalToken)) return;
                request = null;
                setBusyState(false);
                logoutTemporary(freshToken);
                JsonObject result = response.body() == null ? null : response.body().getData();
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || result == null || !result.has("id_acceso") || result.get("id_acceso").getAsInt() != challenge.accessId
                        || !result.has("huella_habilitada") || !result.get("huella_habilitada").getAsBoolean()) {
                    setFeedback("El servidor no confirmó la recuperación. Comprueba el estado antes de reintentar.",
                            FeedbackTone.ERROR);
                    return;
                }
                try {
                    local.save(challenge.accessId, session.getUsername(), true);
                } catch (GeneralSecurityException failure) {
                    setFeedback("El servidor recuperó la huella, pero no se pudo guardar el estado local.",
                            FeedbackTone.WARNING);
                    return;
                }
                setFeedback("Huella recuperada en este dispositivo; el rostro se conservó.", FeedbackTone.SUCCESS);
                loadStatus();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonObject>> source,
                                  @NonNull Throwable error) {
                if (stale(source, originalToken)) return;
                request = null;
                setBusyState(false);
                logoutTemporary(freshToken);
                setFeedback("Sin confirmación de red. Comprueba el estado del servidor antes de reintentar; no se activó la huella local.",
                        FeedbackTone.ERROR);
            }
        });
    }

    private void revoke(String method, int accessId, String freshToken, String originalToken) {
        setFeedback("Revocando vínculo en el servidor…", FeedbackTone.INFO);
        Map<String, Object> body = new HashMap<>();
        body.put("id_acceso", accessId);
        body.put("metodo", method);
        request = ApiClient.getService().revokeOwnBiometrics("Bearer " + freshToken, body);
        @SuppressWarnings("unchecked")
        Call<ApiResponse<JsonObject>> call = (Call<ApiResponse<JsonObject>>) request;
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<JsonObject>> source,
                                   @NonNull Response<ApiResponse<JsonObject>> response) {
                if (stale(source, originalToken)) return;
                request = null;
                setBusyState(false);
                logoutTemporary(freshToken);
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()) {
                    setFeedback(response.code() == 404 ? "No hay un vínculo activo de ese método en tu cuenta."
                            : response.code() == 403 ? "El servidor requiere una contraseña reciente o denegó el acceso."
                            : "El servidor no confirmó la revocación. No se borraron claves locales.",
                            FeedbackTone.ERROR);
                    return;
                }
                JsonObject result = response.body().getData();
                if (result == null || !result.has("id_acceso") || result.get("id_acceso").getAsInt() != accessId) {
                    setFeedback("Respuesta inesperada del servidor. Actualiza el estado antes de continuar.",
                            FeedbackTone.WARNING);
                    loadStatus();
                    return;
                }
                boolean localDevice = false;
                try {
                    AndroidDeviceStore androidStore = new AndroidDeviceStore(SecurityDevicesActivity.this);
                    FaceDeviceStore faceStore = new FaceDeviceStore(SecurityDevicesActivity.this);
                    localDevice = androidStore.id() == accessId && usernameMatches(androidStore);
                    if (localDevice) {
                        if ("ROSTRO".equals(method) || "AMBOS".equals(method)) {
                            if (faceStore.matches(androidStore)) faceStore.clearBinding();
                        }
                        if ("HUELLA".equals(method) && !androidStore.disableBiometricLogin()) {
                            throw new GeneralSecurityException("No se pudo desactivar la huella local");
                        }
                        if ("AMBOS".equals(method) && !androidStore.clear()) {
                            throw new GeneralSecurityException("No se pudo limpiar la clave del dispositivo");
                        }
                    }
                } catch (GeneralSecurityException localFailure) {
                    setFeedback("Cambio confirmado en el servidor, pero la clave local no está disponible. Para recuperar este teléfono, desvincula ambos métodos del dispositivo anterior e inicia con contraseña para registrar una clave nueva.",
                            FeedbackTone.WARNING);
                    return;
                }
                if ("AMBOS".equals(method) && localDevice) {
                    session.clearActiveSession();
                    AccountNavigation.login(SecurityDevicesActivity.this, false);
                } else {
                    setFeedback("Cambio confirmado para el dispositivo #" + accessId + ".", FeedbackTone.SUCCESS);
                    loadStatus();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<JsonObject>> source, @NonNull Throwable error) {
                if (stale(source, originalToken)) return;
                request = null;
                setBusyState(false);
                logoutTemporary(freshToken);
                setFeedback("Sin conexión. Comprueba el estado del servidor antes de volver a intentar.",
                        FeedbackTone.ERROR);
            }
        });
    }

    private void logoutTemporary(String token) {
        ApiClient.getService().logout("Bearer " + token, Collections.emptyMap())
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                           @NonNull Response<ApiResponse<Object>> response) {
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable error) {
                    }
                });
    }

    private boolean usernameMatches(AndroidDeviceStore store) {
        return session.getUsername().equals(store.username());
    }

    private boolean stale(Call<?> source, String originalToken) {
        return isFinishing() || isDestroyed() || source != request
                || !originalToken.equals(session.getAccessToken());
    }

    private void expired() {
        session.clearActiveSession();
        AccountNavigation.login(this, false);
    }

    @Override
    protected void onDestroy() {
        if (request != null) request.cancel();
        if (fingerprintPrompt != null) fingerprintPrompt.cancelAuthentication();
        super.onDestroy();
    }
}
