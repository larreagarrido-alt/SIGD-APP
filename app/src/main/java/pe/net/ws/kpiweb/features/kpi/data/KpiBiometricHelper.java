package pe.net.ws.kpiweb.features.kpi.data;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executor;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.ui.KpiThemeManager;

/**
 * Gestor unificado de autenticación biométrica para todos los KPIs diarios:
 * - Muestra un panel previo de selección:
 * Tipo de biométrico
 * [ Huella dactilar ] | [ Facial ]
 * + Botón activable "Recordar mi elección" para no preguntar a cada rato.
 * - Ejecuta AndroidX BiometricPrompt adaptado al tipo seleccionado (Huella o
 * Facial).
 */
public class KpiBiometricHelper {

    public static final String TYPE_FINGERPRINT = "FINGERPRINT";
    public static final String TYPE_FACE = "FACE";

    private static final String PREF_NAME = "kpi_biometric_choice_prefs";
    private static final String KEY_REMEMBER = "remember_biometric_type";
    private static final String KEY_TYPE = "selected_biometric_type";

    public interface BiometricCallback {
        void onSuccess();

        void onError(int errorCode, String errorMessage);

        void onFailed();
    }

    public static class BiometricStatus {
        public final boolean isAvailable;
        public final String message;

        public BiometricStatus(boolean isAvailable, String message) {
            this.isAvailable = isAvailable;
            this.message = message;
        }
    }

    public static BiometricStatus checkBiometricAvailability(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.BIOMETRIC_WEAK;
        int canAuth = biometricManager.canAuthenticate(authenticators);

        switch (canAuth) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return new BiometricStatus(true, "Biometría disponible");
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return new BiometricStatus(false,
                        "Este dispositivo no cuenta con sensor biométrico (huella dactilar o reconocimiento facial).");
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return new BiometricStatus(false,
                        "El sensor biométrico del dispositivo no está disponible temporalmente.");
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return new BiometricStatus(false,
                        "No tienes huella ni reconocimiento facial configurado en este celular.");
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                return new BiometricStatus(false,
                        "Se requiere una actualización de seguridad del sistema para usar la biometría.");
            default:
                return new BiometricStatus(false, "No fue posible verificar el sensor biométrico del dispositivo.");
        }
    }

    /**
     * Punto de entrada unificado para guardar cualquier KPI diario:
     * Si el usuario activó "Recordar", abre directamente el método elegido (Huella
     * o Facial).
     * Si no, muestra el panel "Tipo de biométrico: Huella dactilar | Facial" +
     * Switch "Recordar".
     */
    public static void authenticate(Fragment fragment, String dayTitle, BiometricCallback callback) {
        if (fragment == null || fragment.getContext() == null)
            return;
        Context context = fragment.requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        boolean remembered = prefs.getBoolean(KEY_REMEMBER, false);
        String savedType = prefs.getString(KEY_TYPE, TYPE_FINGERPRINT);

        if (remembered) {
            launchSelectedBiometric(fragment, dayTitle, savedType, true, callback);
        } else {
            showBiometricTypeSelectorDialog(fragment, dayTitle, savedType, callback);
        }
    }

    public static void clearRememberedPreference(Context context) {
        if (context == null)
            return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_REMEMBER, false)
                .apply();
    }

    private static void showBiometricTypeSelectorDialog(Fragment fragment,
            String dayTitle,
            String initialType,
            BiometricCallback callback) {
        Context context = fragment.requireContext();
        boolean isDark = KpiThemeManager.isDarkMode(context);

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int primaryColor = ContextCompat.getColor(context, R.color.kpi_primary);
        int textPrimary = ContextCompat.getColor(context, R.color.kpi_text_primary);
        int textSecondary = ContextCompat.getColor(context, R.color.kpi_text_secondary);
        int borderNeutral = ContextCompat.getColor(context, R.color.kpi_border);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 20);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(context, 22));
        cardBg.setColor(isDark ? Color.parseColor("#151E36") : Color.WHITE);
        cardBg.setStroke(dp(context, 1), borderNeutral);
        root.setBackground(cardBg);

        // Título centrado: Tipo de biométrico
        TextView tvTitle = new TextView(context);
        tvTitle.setText("Tipo de biométrico");
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(textPrimary);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText("Elige cómo validar: " + dayTitle);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(textSecondary);
        tvSub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(context, 4);
        subLp.bottomMargin = dp(context, 16);
        tvSub.setLayoutParams(subLp);
        root.addView(tvSub);

        // Fila horizontal: [ Huella dactilar ] | [ Facial ]
        LinearLayout optionsRow = new LinearLayout(context);
        optionsRow.setOrientation(LinearLayout.HORIZONTAL);
        optionsRow.setGravity(Gravity.CENTER_VERTICAL);

        final String[] selectedType = { TYPE_FACE.equals(initialType) ? TYPE_FACE : TYPE_FINGERPRINT };

        LinearLayout btnFingerprint = buildOptionBox(context, R.drawable.kpi_ic_fingerprint, "Huella dactilar",
                textPrimary);
        LinearLayout.LayoutParams fpLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fpLp.rightMargin = dp(context, 6);
        btnFingerprint.setLayoutParams(fpLp);

        View divider = new View(context);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(context, 1), dp(context, 64));
        divider.setLayoutParams(divLp);
        divider.setBackgroundColor(borderNeutral);

        LinearLayout btnFace = buildOptionBox(context, R.drawable.kpi_ic_camera_pro, "Facial", textPrimary);
        LinearLayout.LayoutParams faceLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        faceLp.leftMargin = dp(context, 6);
        btnFace.setLayoutParams(faceLp);

        optionsRow.addView(btnFingerprint);
        optionsRow.addView(divider);
        optionsRow.addView(btnFace);
        root.addView(optionsRow);

        // Fila activable: Recordar mi elección
        LinearLayout rememberRow = new LinearLayout(context);
        rememberRow.setOrientation(LinearLayout.HORIZONTAL);
        rememberRow.setGravity(Gravity.CENTER_VERTICAL);
        rememberRow.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        GradientDrawable remBg = new GradientDrawable();
        remBg.setCornerRadius(dp(context, 12));
        remBg.setColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#FAF5FF"));
        remBg.setStroke(dp(context, 1), borderNeutral);
        rememberRow.setBackground(remBg);
        LinearLayout.LayoutParams remLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        remLp.topMargin = dp(context, 14);
        rememberRow.setLayoutParams(remLp);

        LinearLayout remTexts = new LinearLayout(context);
        remTexts.setOrientation(LinearLayout.VERTICAL);
        remTexts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvRemTitle = new TextView(context);
        tvRemTitle.setText("Recordar tipo de biométrico");
        tvRemTitle.setTextSize(12.5f);
        tvRemTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvRemTitle.setTextColor(textPrimary);
        remTexts.addView(tvRemTitle);

        TextView tvRemDesc = new TextView(context);
        tvRemDesc.setText("Para no volver a preguntar cada vez que guardes");
        tvRemDesc.setTextSize(10.5f);
        tvRemDesc.setTextColor(textSecondary);
        remTexts.addView(tvRemDesc);

        SwitchMaterial swRemember = new SwitchMaterial(context);
        swRemember.setChecked(false);

        rememberRow.addView(remTexts);
        rememberRow.addView(swRemember);
        rememberRow.setOnClickListener(v -> swRemember.setChecked(!swRemember.isChecked()));
        root.addView(rememberRow);

        // Botones inferiores: Cancelar | Confirmar
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(context, 16);
        actionsRow.setLayoutParams(actLp);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cancelar");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextSize(13f);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setTextColor(textSecondary);
        btnCancel.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, dp(context, 46), 0.85f);
        cancelLp.rightMargin = dp(context, 8);
        btnCancel.setLayoutParams(cancelLp);
        actionsRow.addView(btnCancel);

        TextView btnContinue = new TextView(context);
        btnContinue.setGravity(Gravity.CENTER);
        btnContinue.setTextSize(13f);
        btnContinue.setTypeface(Typeface.DEFAULT_BOLD);
        btnContinue.setTextColor(Color.WHITE);
        btnContinue.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        LinearLayout.LayoutParams contLp = new LinearLayout.LayoutParams(0, dp(context, 46), 1.35f);
        btnContinue.setLayoutParams(contLp);
        actionsRow.addView(btnContinue);

        root.addView(actionsRow);

        Runnable refreshSelectionUi = () -> {
            boolean fpActive = TYPE_FINGERPRINT.equals(selectedType[0]);
            styleOptionBox(context, btnFingerprint, fpActive, primaryColor, borderNeutral, isDark);
            styleOptionBox(context, btnFace, !fpActive, primaryColor, borderNeutral, isDark);
            btnContinue.setText(fpActive ? "Validar con Huella" : "Activar Cámara Facial");
        };
        refreshSelectionUi.run();

        btnFingerprint.setOnClickListener(v -> {
            selectedType[0] = TYPE_FINGERPRINT;
            refreshSelectionUi.run();
        });

        btnFace.setOnClickListener(v -> {
            selectedType[0] = TYPE_FACE;
            refreshSelectionUi.run();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnContinue.setOnClickListener(v -> {
            boolean rememberNow = swRemember.isChecked();
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_TYPE, selectedType[0])
                    .putBoolean(KEY_REMEMBER, rememberNow)
                    .apply();
            dialog.dismiss();
            launchSelectedBiometric(fragment, dayTitle, selectedType[0], rememberNow, callback);
        });

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static LinearLayout buildOptionBox(Context context, int iconResId, String label, int textColor) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        int p = dp(context, 14);
        box.setPadding(dp(context, 8), p, dp(context, 8), p);
        box.setClickable(true);
        box.setFocusable(true);

        ImageView ivIcon = new ImageView(context);
        ivIcon.setImageResource(iconResId);
        ivIcon.setColorFilter(textColor);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32));
        ivIcon.setLayoutParams(icLp);
        box.addView(ivIcon);

        TextView tvLabel = new TextView(context);
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setTextColor(textColor);
        tvLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 6);
        tvLabel.setLayoutParams(lp);
        box.addView(tvLabel);

        return box;
    }

    private static void styleOptionBox(Context context,
            LinearLayout box,
            boolean selected,
            int primaryColor,
            int borderNeutral,
            boolean isDark) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(context, 16));
        if (selected) {
            bg.setColor(isDark ? Color.parseColor("#1E1B4B") : Color.parseColor("#FAE8FF"));
            bg.setStroke(dp(context, 2), primaryColor);
        } else {
            bg.setColor(isDark ? Color.parseColor("#0F172A") : Color.parseColor("#F8FAFC"));
            bg.setStroke(dp(context, 1), borderNeutral);
        }
        box.setBackground(bg);
    }

    private static void launchSelectedBiometric(Fragment fragment,
            String dayTitle,
            String biometricType,
            boolean isRemembered,
            BiometricCallback callback) {
        if (TYPE_FACE.equals(biometricType)) {
            launchFacialCameraBiometric(fragment, dayTitle, isRemembered, callback);
        } else {
            launchFingerprintBiometric(fragment, dayTitle, isRemembered, callback);
        }
    }

    /**
     * Enciende la cámara frontal real (Camera2 + TextureView) dentro de
     * KpiFacialCameraDialogFragment
     * para realizar el reconocimiento/validación facial en vivo.
     */
    private static void launchFacialCameraBiometric(Fragment fragment,
            String dayTitle,
            boolean isRemembered,
            BiometricCallback callback) {
        if (!fragment.isAdded())
            return;
        Context context = fragment.requireContext();

        KpiFacialCameraDialogFragment cameraDialog = KpiFacialCameraDialogFragment.newInstance(dayTitle, isRemembered);

        cameraDialog.setFacialResultListener(new KpiFacialCameraDialogFragment.FacialResultListener() {
            @Override
            public void onVerified() {
                callback.onSuccess();
            }

            @Override
            public void onSwitchToFingerprint() {
                if (!fragment.isAdded())
                    return;
                if (isRemembered) {
                    clearRememberedPreference(context);
                    showBiometricTypeSelectorDialog(fragment, dayTitle, TYPE_FINGERPRINT, callback);
                } else {
                    launchFingerprintBiometric(fragment, dayTitle, false, callback);
                }
            }

            @Override
            public void onCancelled() {
                // Cancelación voluntaria por el usuario: se mantiene en borrador sin bloquear
            }

            @Override
            public void onCameraUnavailable(String message) {
                callback.onError(-1, message);
            }
        });

        cameraDialog.show(fragment.getChildFragmentManager(), "kpi_facial_camera_dialog");
    }

    /**
     * Ejecuta el sensor nativo de Huella Dactilar del dispositivo mediante AndroidX
     * BiometricPrompt.
     */
    private static void launchFingerprintBiometric(Fragment fragment,
            String dayTitle,
            boolean isRemembered,
            BiometricCallback callback) {
        Context context = fragment.requireContext();
        BiometricStatus status = checkBiometricAvailability(context);

        if (!status.isAvailable) {
            callback.onError(-1, status.message);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(context);

        BiometricPrompt biometricPrompt = new BiometricPrompt(fragment, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (isRemembered && errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            clearRememberedPreference(context);
                            showBiometricTypeSelectorDialog(fragment, dayTitle, TYPE_FINGERPRINT, callback);
                            return;
                        }
                        callback.onError(errorCode, errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onFailed();
                    }
                });

        BiometricManager bm = BiometricManager.from(context);
        int allowedAuth;
        if (bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            allowedAuth = BiometricManager.Authenticators.BIOMETRIC_STRONG;
        } else {
            allowedAuth = BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.BIOMETRIC_WEAK;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Validación por Huella Dactilar")
                .setSubtitle("Guardar KPI: " + dayTitle)
                .setDescription("Coloca tu huella dactilar en el sensor para confirmar y guardar tu KPI.")
                .setNegativeButtonText(isRemembered ? "Cambiar método" : "Cancelar")
                .setAllowedAuthenticators(allowedAuth)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
