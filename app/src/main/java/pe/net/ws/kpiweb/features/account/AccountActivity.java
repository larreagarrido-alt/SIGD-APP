package pe.net.ws.kpiweb.features.account;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.account.data.AccountRepository;
import pe.net.ws.kpiweb.features.account.model.UserProfile;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla principal de "Mi cuenta" para Administrador y Colaborador.
 * Presenta el perfil profesional completo, iniciales de avatar dinámicas, datos de servidor no editables
 * y accesos seguros para edición de datos autorizados y cambio de contraseña.
 */
public class AccountActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ROLE = "extra_user_role";

    private UserRole currentRole = UserRole.ADMINISTRADOR;
    private UserProfile currentProfile;

    private View btnBack;
    private View btnSettings;
    private View btnChangePhoto;
    private TextView tvAvatar;
    private TextView tvFullName;
    private TextView tvPersonalName;
    private TextView tvJobTitle;
    private TextView tvLaborJob;
    private TextView tvRoleBadge;
    private TextView tvStatusBadge;

    private TextView tvEmail;
    private TextView tvPhone;
    private TextView tvDepartment;
    private TextView tvEmployeeId;
    private TextView tvHireDate;

    private View btnEditProfile;
    private View rowName;
    private View rowPhone;
    private View rowChangePassword;
    private TextView tvDataState;
    private Call<ApiResponse<LoginResponseData>> profileRequest;

    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    UserProfile updated = (UserProfile) result.getData().getSerializableExtra(EditProfileActivity.RESULT_UPDATED_PROFILE);
                    if (updated != null) {
                        this.currentProfile = updated;
                        populateViews(currentProfile);
                    } else {
                        loadProfile();
                    }
                    CollaboratorBannerHelper.showBanner(
                            this,
                            "Perfil actualizado",
                            "Tus datos de contacto se actualizaron correctamente en la sesión.",
                            R.drawable.collaborator_ic_check_circle
                    );
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        if (UserRole.COLABORADOR.name().equals(getIntent().getStringExtra(EXTRA_USER_ROLE))) {
            setTheme(R.style.Theme_KPIWeb_Collaborator);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_activity_main);

        setupWindowAppearance();
        parseIntentExtras();
        bindViews();
        setupListeners();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupWindowAppearance();
        refreshVerifiedProfile();
    }

    @Override
    protected void onStop() {
        if (profileRequest != null) profileRequest.cancel();
        profileRequest = null;
        super.onStop();
    }

    private void setupWindowAppearance() {
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
        btnBack = findViewById(R.id.account_btn_back);
        btnSettings = findViewById(R.id.account_btn_settings);
        btnChangePhoto = findViewById(R.id.account_btn_change_photo);

        tvAvatar = findViewById(R.id.account_tv_avatar);
        tvFullName = findViewById(R.id.account_tv_full_name);
        tvPersonalName = findViewById(R.id.account_tv_personal_name);
        tvJobTitle = findViewById(R.id.account_tv_job_title);
        tvLaborJob = findViewById(R.id.account_tv_labor_job);
        tvRoleBadge = findViewById(R.id.account_tv_role_badge);
        tvStatusBadge = findViewById(R.id.account_tv_status_badge);

        tvEmail = findViewById(R.id.account_tv_email);
        tvPhone = findViewById(R.id.account_tv_phone);
        tvDepartment = findViewById(R.id.account_tv_department);
        tvEmployeeId = findViewById(R.id.account_tv_employee_id);
        tvHireDate = findViewById(R.id.account_tv_hire_date);
        tvDataState = findViewById(R.id.account_tv_data_state);

        btnEditProfile = findViewById(R.id.account_btn_edit_profile);
        rowName = findViewById(R.id.account_row_name);
        rowPhone = findViewById(R.id.account_row_phone);
        rowChangePassword = findViewById(R.id.account_row_change_password);
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, pe.net.ws.kpiweb.features.settings.SettingsActivity.class);
                intent.putExtra(EXTRA_USER_ROLE, currentRole.name());
                startActivity(intent);
            });
        }

        findViewById(R.id.account_btn_face_security).setOnClickListener(v ->
                startActivity(new Intent(this, pe.net.ws.kpiweb.features.auth.FaceEnrollmentActivity.class)));

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v ->
                    CollaboratorBannerHelper.showBanner(
                            this,
                            "Fotografía corporativa",
                            "La carga de foto al servidor institucional estará disponible al conectar la API.",
                            R.drawable.account_ic_camera
                    )
            );
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> openEditProfile());
        }

        if (rowName != null) {
            rowName.setOnClickListener(v -> openEditProfile());
        }

        if (rowPhone != null) {
            rowPhone.setOnClickListener(v -> openEditProfile());
        }

        if (rowChangePassword != null) {
            rowChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChangePasswordActivity.class);
                startActivity(intent);
            });
        }
    }

    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra(EditProfileActivity.EXTRA_USER_ROLE, currentRole.name());
        editProfileLauncher.launch(intent);
    }

    private void loadProfile() {
        SessionManager session = new SessionManager(this);
        boolean verifiedAccount = session.isLoggedIn() && session.getUserId() > 0;
        boolean verifiedWorker = verifiedAccount && currentRole == UserRole.COLABORADOR;
        if (verifiedWorker) {
            CollaboratorSessionManager.getInstance(this).getActiveProfile();
        }
        currentProfile = verifiedAccount ? profileFromSession(session) : new UserProfile(
                "—", "Sin sesión", "", "", "", "", currentRole, "", "", "Por consultar");
        if (verifiedAccount) {
            // Editing is local-only today; do not present it as a saved server change.
            if (btnEditProfile != null) btnEditProfile.setVisibility(View.GONE);
            if (btnChangePhoto != null) btnChangePhoto.setVisibility(View.GONE);
            if (rowName != null) {
                rowName.setOnClickListener(null);
                rowName.setClickable(false);
                rowName.setFocusable(false);
            }
            if (rowPhone != null) {
                rowPhone.setOnClickListener(null);
                rowPhone.setClickable(false);
                rowPhone.setFocusable(false);
            }
            findViewById(R.id.account_iv_name_edit).setVisibility(View.GONE);
            findViewById(R.id.account_iv_phone_edit).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.account_tv_name_label)).setText("Nombre registrado");
            ((TextView) findViewById(R.id.account_tv_department_label)).setText("Agencia o área");
            ((TextView) findViewById(R.id.account_tv_employee_id_label)).setText(
                    session.getWorkerId() > 0 ? "ID de trabajador" : "ID de usuario");
            if (tvDataState != null) tvDataState.setVisibility(View.VISIBLE);
        }
        populateViews(currentProfile);
    }

    private UserProfile profileFromSession(SessionManager session) {
        String name = session.getFullName().trim();
        if (name.isEmpty()) name = session.getUsername().trim();
        String status = session.getWorkerId() > 0 ? session.getWorkerStatus() : "Por consultar";
        UserProfile profile = new UserProfile(
                String.valueOf(session.getWorkerId() > 0 ? session.getWorkerId() : session.getUserId()),
                name.isEmpty() ? "Nombre no registrado" : name,
                session.getWorkerEmail(), "", session.getCargoName(), session.getAgenciaName(),
                currentRole, "", "", status);
        profile.setFullName(profile.getFullName());
        return profile;
    }

    private void refreshVerifiedProfile() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() <= 0) return;
        if (profileRequest != null) profileRequest.cancel();
        final int expectedUserId = session.getUserId();
        final int expectedWorkerId = session.getWorkerId();
        final int expectedRoleId = session.getRoleId();
        final String expectedToken = session.getAccessToken();
        if (tvDataState != null) tvDataState.setText("Actualizando datos de tu cuenta…");
        profileRequest = ApiClient.getService().getProfile(session.getBearerToken());
        profileRequest.enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponseData>> call,
                                   Response<ApiResponse<LoginResponseData>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (!isCurrentRequest(call, expectedToken)) return;
                profileRequest = null;
                if (response.code() == 401) {
                    session.clearActiveSession();
                    AccountNavigation.login(AccountActivity.this, false);
                    return;
                }
                ApiResponse<LoginResponseData> envelope = response.body();
                LoginResponseData data = envelope == null ? null : envelope.getData();
                LoginResponseData.WorkerInfo worker = data == null ? null : data.getWorker();
                if (data != null && data.getUser() != null
                        && (data.getUser().getIdUsuario() != expectedUserId
                        || data.getUser().getIdRol() != expectedRoleId
                        || (expectedWorkerId > 0 && (worker == null || worker.getIdTrabajador() != expectedWorkerId))
                        || (expectedWorkerId == 0 && worker != null))) {
                    session.clearActiveSession();
                    AccountNavigation.login(AccountActivity.this, false);
                    return;
                }
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || data == null || data.getUser() == null) {
                    showProfileUnavailable();
                    return;
                }
                String fullName = worker == null ? session.getUsername() : worker.getNombreCompleto();
                if (fullName == null || fullName.trim().isEmpty()) fullName = session.getFullName();
                if (fullName == null || fullName.trim().isEmpty()) fullName = session.getUsername();
                if (worker != null) session.updateVerifiedWorkerDetails(expectedWorkerId, fullName,
                        worker.getCargoNombre(), worker.getAgenciaNombre(), worker.getTipo(),
                        worker.getEmail(), worker.getFotoPerfil(), worker.getEstado());
                currentProfile = new UserProfile(String.valueOf(expectedWorkerId > 0 ? expectedWorkerId : expectedUserId), fullName,
                        worker == null ? "" : worker.getEmail(), worker == null ? "" : worker.getTelefono(),
                        worker == null ? "" : worker.getCargoNombre(), worker == null ? "" : worker.getAgenciaNombre(),
                        currentRole, worker == null ? "" : worker.getFechaInicio(),
                        "", worker == null ? "Por consultar" : worker.getEstado());
                currentProfile.setFullName(fullName);
                populateViews(currentProfile);
                if (tvDataState != null) tvDataState.setText(worker == null
                        ? "El servidor solo proporciona usuario y rol de esta cuenta"
                        : "Datos actualizados desde WEBSITE");
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponseData>> call, Throwable error) {
                if (!isCurrentRequest(call, expectedToken)) return;
                profileRequest = null;
                showProfileUnavailable();
            }
        });
    }

    private boolean isCurrentRequest(Call<ApiResponse<LoginResponseData>> call, String expectedToken) {
        return !isFinishing() && !isDestroyed() && profileRequest == call && !call.isCanceled()
                && expectedToken.equals(new SessionManager(this).getAccessToken());
    }

    private void showProfileUnavailable() {
        if (tvDataState != null) tvDataState.setText("Sin conexión con el perfil · datos de la última sesión");
    }

    private void populateViews(UserProfile profile) {
        if (profile == null) return;

        if (tvAvatar != null) tvAvatar.setText(profile.getAvatarInitials());
        if (new SessionManager(this).isLoggedIn()) {
            CollaboratorProfilePhotoLoader.load(this, findViewById(R.id.account_iv_photo), tvAvatar,
                    new SessionManager(this).getWorkerPhoto());
        }
        if (tvFullName != null) tvFullName.setText(profile.getFullName());
        if (tvPersonalName != null) tvPersonalName.setText(profile.getFullName());
        if (tvJobTitle != null) tvJobTitle.setText(profile.getJobTitle());
        if (tvLaborJob != null) tvLaborJob.setText(profile.getJobTitle());

        if (tvRoleBadge != null) {
            String workerType = currentRole == UserRole.COLABORADOR
                    ? new SessionManager(this).getWorkerTipo() : "";
            tvRoleBadge.setText(workerType == null || workerType.trim().isEmpty()
                    ? profile.getRole().getDisplayName() : workerType.trim());
        }

        if (tvStatusBadge != null) {
            String status = currentRole == UserRole.COLABORADOR
                    ? new SessionManager(this).getWorkerStatus() : profile.getAccountStatus();
            tvStatusBadge.setText(status == null || status.trim().isEmpty()
                    ? "Estado por consultar" : "● " + status.trim());
        }

        if (tvEmail != null) tvEmail.setText(displayOrUnavailable(profile.getEmail()));
        boolean verifiedAccount = new SessionManager(this).isLoggedIn();
        if (tvPhone != null) tvPhone.setText(verifiedAccount
                ? displayServerField(profile.getPhone()) : displayOrUnavailable(profile.getPhone()));
        if (tvDepartment != null) tvDepartment.setText(displayOrUnavailable(profile.getDepartment()));
        if (tvEmployeeId != null) tvEmployeeId.setText(profile.getEmployeeId());
        if (tvHireDate != null) tvHireDate.setText(verifiedAccount
                ? displayServerField(profile.getHireDateRaw()) : profile.getHireDate());
    }

    private String displayOrUnavailable(String value) {
        return value == null || value.trim().isEmpty() ? "No registrado" : value.trim();
    }

    private String displayServerField(String value) {
        return value == null || value.trim().isEmpty()
                ? "No disponible" : value.trim();
    }
}
