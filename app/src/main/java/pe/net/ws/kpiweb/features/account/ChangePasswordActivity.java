package pe.net.ws.kpiweb.features.account;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.account.data.AccountRepository;
import pe.net.ws.kpiweb.features.account.validation.AccountValidator;
import pe.net.ws.kpiweb.features.account.validation.ValidationResult;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;

/**
 * Pantalla para solicitud de cambio de contraseña adaptable a Modo Claro y Modo Oscuro.
 * Cumple estrictamente con la regla de seguridad:
 * No almacena contraseñas localmente ni finge un cambio exitoso en base de datos.
 * Informa el estado pendiente de conexión con el servicio seguro del servidor del integrador.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputLayout layoutCurrent;
    private TextInputEditText etCurrent;

    private TextInputLayout layoutNew;
    private TextInputEditText etNew;

    private TextInputLayout layoutConfirm;
    private TextInputEditText etConfirm;

    private ProgressBar progressBar;
    private MaterialButton btnSubmit;
    private MaterialButton btnCancel;
    private View btnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_activity_change_password);

        CollaboratorThemeHelper.setupSystemBars(this);
        bindViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void bindViews() {
        btnBack = findViewById(R.id.account_pwd_btn_back);
        layoutCurrent = findViewById(R.id.account_pwd_layout_current);
        etCurrent = findViewById(R.id.account_pwd_et_current);
        layoutNew = findViewById(R.id.account_pwd_layout_new);
        etNew = findViewById(R.id.account_pwd_et_new);
        layoutConfirm = findViewById(R.id.account_pwd_layout_confirm);
        etConfirm = findViewById(R.id.account_pwd_et_confirm);
        progressBar = findViewById(R.id.account_pwd_progress);
        btnSubmit = findViewById(R.id.account_pwd_btn_submit);
        btnCancel = findViewById(R.id.account_pwd_btn_cancel);
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        // Configuración estricta de Mostrar/Ocultar contraseña:
        // Configuración de visibilidad mediante controlador de única fuente de verdad:
        // Estado inicial: contraseña oculta con puntos, acción disponible: "Mostrar contraseña" (ojo abierto).
        // Al pulsar Mostrar: texto en claro, acción cambia a "Ocultar contraseña" (ojo tachado).
        // Al pulsar Ocultar: texto con puntos, acción cambia a "Mostrar contraseña" (ojo abierto).
        new PasswordFieldController(layoutCurrent, etCurrent);
        new PasswordFieldController(layoutNew, etNew);
        new PasswordFieldController(layoutConfirm, etConfirm);

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> attemptPasswordChange());
        }
    }

    /**
     * Controlador de visibilidad de contraseña con única fuente de verdad booleana.
     * Evita consultas frágiles a instanceof sobre getTransformationMethod()
     * y garantiza que el estado visual, el ícono, la descripción accesible
     * y la selección del cursor permanezcan 100% sincronizados en ambos sentidos.
     */
    private static class PasswordFieldController {
        private final TextInputLayout layout;
        private final TextInputEditText editText;
        private boolean isVisible = false; // Fuente de verdad absoluta

        PasswordFieldController(TextInputLayout layout, TextInputEditText editText) {
            this.layout = layout;
            this.editText = editText;
            if (this.layout != null && this.editText != null) {
                applyState();
                this.layout.setEndIconOnClickListener(v -> toggle());
            }
        }

        private void toggle() {
            isVisible = !isVisible;
            applyState();
        }

        private void applyState() {
            int cursor = editText.getSelectionEnd();
            android.graphics.Typeface typeface = editText.getTypeface();

            if (isVisible) {
                // Contraseña VISIBLE en texto legible
                editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                layout.setEndIconDrawable(R.drawable.collab_ic_eye_off);
                layout.setEndIconContentDescription("Ocultar contraseña");
            } else {
                // Contraseña OCULTA mediante puntos
                editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                layout.setEndIconDrawable(R.drawable.collab_ic_eye);
                layout.setEndIconContentDescription("Mostrar contraseña");
            }

            if (typeface != null) {
                editText.setTypeface(typeface);
            }
            if (cursor >= 0 && editText.getText() != null) {
                editText.setSelection(Math.min(cursor, editText.getText().length()));
            }
        }
    }

    private void attemptPasswordChange() {
        String currentPwd = etCurrent != null && etCurrent.getText() != null ? etCurrent.getText().toString() : "";
        String newPwd = etNew != null && etNew.getText() != null ? etNew.getText().toString() : "";
        String confirmPwd = etConfirm != null && etConfirm.getText() != null ? etConfirm.getText().toString() : "";

        // 1. Validar Contraseña Actual
        ValidationResult currentResult = AccountValidator.validateCurrentPassword(currentPwd);
        if (!currentResult.isValid()) {
            if (layoutCurrent != null) layoutCurrent.setError(currentResult.getErrorMessage());
            CollaboratorBannerHelper.showBanner(
                    this,
                    "Revisa tu contraseña actual",
                    currentResult.getErrorMessage(),
                    R.drawable.account_ic_lock
            );
            return;
        } else {
            if (layoutCurrent != null) layoutCurrent.setError(null);
        }

        // 2. Validar Nueva Contraseña
        ValidationResult newResult = AccountValidator.validateNewPassword(newPwd, currentPwd);
        if (!newResult.isValid()) {
            if (layoutNew != null) layoutNew.setError(newResult.getErrorMessage());
            CollaboratorBannerHelper.showBanner(
                    this,
                    "Requisitos de nueva contraseña",
                    newResult.getErrorMessage(),
                    R.drawable.account_ic_lock
            );
            return;
        } else {
            if (layoutNew != null) layoutNew.setError(null);
        }

        // 3. Validar Confirmación
        ValidationResult confirmResult = AccountValidator.validatePasswordConfirmation(newPwd, confirmPwd);
        if (!confirmResult.isValid()) {
            if (layoutConfirm != null) layoutConfirm.setError(confirmResult.getErrorMessage());
            CollaboratorBannerHelper.showBanner(
                    this,
                    "Confirmación no coincide",
                    confirmResult.getErrorMessage(),
                    R.drawable.account_ic_lock
            );
            return;
        } else {
            if (layoutConfirm != null) layoutConfirm.setError(null);
        }

        // 4. Estado de Carga
        setLoadingState(true);

        // 5. Envío al Repositorio (Sin almacenar contraseñas en SharedPreferences ni memoria persistente)
        AccountRepository.getInstance().requestPasswordChange(currentPwd, newPwd, new AccountRepository.ChangePasswordCallback() {
            @Override
            public void onPendingIntegration(String message) {
                setLoadingState(false);
                showPendingIntegrationDialog(message);
            }

            @Override
            public void onError(String errorMessage) {
                setLoadingState(false);
                CollaboratorBannerHelper.showBanner(
                        ChangePasswordActivity.this,
                        "Aviso de seguridad",
                        errorMessage,
                        R.drawable.account_ic_lock
                );
            }
        });
    }

    private void setLoadingState(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnSubmit != null) btnSubmit.setEnabled(!loading);
        if (btnCancel != null) btnCancel.setEnabled(!loading);
        if (etCurrent != null) etCurrent.setEnabled(!loading);
        if (etNew != null) etNew.setEnabled(!loading);
        if (etConfirm != null) etConfirm.setEnabled(!loading);
    }

    private void showPendingIntegrationDialog(String message) {
        CollaboratorBannerHelper.showPremiumModal(
                this,
                "SEGURIDAD DE CREDENCIALES • WEBSITE",
                "Validación de formato completada",
                "Las reglas y requisitos de tu nueva contraseña se validaron correctamente.\n\n"
                        + message
                        + "\n\nPor seguridad institucional, ninguna contraseña ha sido almacenada localmente en este dispositivo.",
                "Entendido",
                this::finish,
                "Seguir editando"
        );
    }
}
