package pe.net.ws.kpiweb.features.account;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.account.data.AccountRepository;
import pe.net.ws.kpiweb.features.account.model.UserProfile;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.account.validation.AccountValidator;
import pe.net.ws.kpiweb.features.account.validation.ValidationResult;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;

/**
 * Pantalla para editar los campos autorizados del perfil (Nombre y Teléfono)
 * con soporte completo para Modo Claro y Modo Oscuro.
 */
public class EditProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ROLE = "extra_user_role";
    public static final String RESULT_UPDATED_PROFILE = "result_updated_profile";

    private UserRole currentRole = UserRole.ADMINISTRADOR;
    private UserProfile currentProfile;

    private TextInputLayout layoutName;
    private TextInputEditText etName;

    private TextInputLayout layoutPhone;
    private TextInputEditText etPhone;

    private TextInputEditText etEmail;
    private TextInputEditText etJob;

    private ProgressBar progressBar;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private View btnClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_activity_edit_profile);

        CollaboratorThemeHelper.setupSystemBars(this);
        parseIntentExtras();
        bindViews();
        loadProfileData();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void parseIntentExtras() {
        if (getIntent().hasExtra(EXTRA_USER_ROLE)) {
            String roleName = getIntent().getStringExtra(EXTRA_USER_ROLE);
            try {
                currentRole = UserRole.valueOf(roleName);
            } catch (Exception ignored) {
                currentRole = UserRole.ADMINISTRADOR;
            }
        }
    }

    private void bindViews() {
        btnClose = findViewById(R.id.account_edit_btn_close);
        layoutName = findViewById(R.id.account_edit_layout_name);
        etName = findViewById(R.id.account_edit_et_name);
        layoutPhone = findViewById(R.id.account_edit_layout_phone);
        etPhone = findViewById(R.id.account_edit_et_phone);
        etEmail = findViewById(R.id.account_edit_et_email);
        etJob = findViewById(R.id.account_edit_et_job);
        progressBar = findViewById(R.id.account_edit_progress);
        btnSave = findViewById(R.id.account_edit_btn_save);
        btnCancel = findViewById(R.id.account_edit_btn_cancel);
    }

    private void loadProfileData() {
        currentProfile = AccountRepository.getInstance().getProfile(currentRole);
        if (currentProfile != null) {
            if (etName != null) etName.setText(currentProfile.getFullName());
            if (etPhone != null) etPhone.setText(currentProfile.getPhone());
            if (etEmail != null) etEmail.setText(currentProfile.getEmail());
            if (etJob != null) {
                etJob.setText(currentProfile.getJobTitle() + " • " + currentProfile.getDepartment());
            }
        }
    }

    private void setupListeners() {
        if (btnClose != null) btnClose.setOnClickListener(v -> finish());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> attemptSave());
        }
    }

    private void attemptSave() {
        if (new SessionManager(this).isLoggedIn()) {
            CollaboratorBannerHelper.showBanner(this, "Edición no disponible",
                    "El servidor aún no ofrece una operación autorizada para guardar estos cambios.");
            return;
        }
        String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        // 1. Validaciones
        ValidationResult nameResult = AccountValidator.validateFullName(name);
        if (!nameResult.isValid()) {
            if (layoutName != null) layoutName.setError(nameResult.getErrorMessage());
            CollaboratorBannerHelper.showBanner(this, "Nombre inválido", nameResult.getErrorMessage());
            return;
        } else {
            if (layoutName != null) layoutName.setError(null);
        }

        ValidationResult phoneResult = AccountValidator.validatePhone(phone);
        if (!phoneResult.isValid()) {
            if (layoutPhone != null) layoutPhone.setError(phoneResult.getErrorMessage());
            CollaboratorBannerHelper.showBanner(this, "Teléfono inválido", phoneResult.getErrorMessage());
            return;
        } else {
            if (layoutPhone != null) layoutPhone.setError(null);
        }

        // 2. Estado de Carga
        setLoadingState(true);

        // 3. Actualización en Repositorio
        AccountRepository.getInstance().updateProfile(currentRole, name, phone, new AccountRepository.UpdateProfileCallback() {
            @Override
            public void onSuccess(UserProfile updatedProfile, String message) {
                setLoadingState(false);
                Intent resultIntent = new Intent();
                resultIntent.putExtra(RESULT_UPDATED_PROFILE, updatedProfile);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoadingState(false);
                CollaboratorBannerHelper.showBanner(EditProfileActivity.this, "Error al actualizar", errorMessage);
            }
        });
    }

    private void setLoadingState(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnSave != null) btnSave.setEnabled(!loading);
        if (btnCancel != null) btnCancel.setEnabled(!loading);
        if (etName != null) etName.setEnabled(!loading);
        if (etPhone != null) etPhone.setEnabled(!loading);
    }
}
