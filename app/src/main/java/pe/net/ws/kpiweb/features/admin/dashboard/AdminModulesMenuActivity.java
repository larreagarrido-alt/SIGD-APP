package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.account.AccountActivity;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.collaborator.about.CollaboratorAboutActivity;
import pe.net.ws.kpiweb.features.collaborator.about.SigdCreditsHelper;
import pe.net.ws.kpiweb.features.collaborator.attendance.CollaboratorAttendanceActivity;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.privacy.CollaboratorPrivacyActivity;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessActivity;
import pe.net.ws.kpiweb.features.help.HelpActivity;
import pe.net.ws.kpiweb.features.settings.SettingsActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Menú principal reorganizado del Administrador (SIGD by website).
 * Separa claramente:
 * 1. MI ESPACIO (funciones personales del administrador sin ranking ni premios).
 * 2. ADMINISTRACIÓN (acceso al Centro de gestión que agrupa los módulos existentes).
 * 3. SISTEMA Y CUENTA (perfil, configuración, privacidad, soporte y cierre de sesión).
 */
public class AdminModulesMenuActivity extends AppCompatActivity {
    private Call<ApiResponse<LoginResponseData>> profileRequest;
    private TextView accountName;
    private TextView accountRole;
    private TextView accountInitials;
    private ImageView accountPhoto;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_modules_menu);
        AdminBottomNavigation.attach(this, 4);

        setupWindowAppearance();
        setupHeaderActions();
        setupAccountCard();
        setupPersonalSpaceActions();
        setupAdministrationActions();
        setupSystemOptions();
        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupWindowAppearance();
        SigdCreditsHelper.bindMenuFooter(this, findViewById(android.R.id.content), false);
        showSessionAccount();
        refreshAccount();
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

    private void setupHeaderActions() {
        View btnClose = findViewById(R.id.admin_menu_btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void setupAccountCard() {
        accountName = findViewById(R.id.admin_menu_account_name);
        accountRole = findViewById(R.id.admin_menu_account_role);
        accountInitials = findViewById(R.id.admin_menu_account_initials);
        accountPhoto = findViewById(R.id.admin_menu_account_photo);
        bindAction(R.id.admin_menu_card_account, this::openAccount);
    }

    private void openAccount() {
        Intent intent = new Intent(this, AccountActivity.class);
        intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.ADMINISTRADOR.name());
        startActivity(intent);
    }

    private void showSessionAccount() {
        SessionManager session = new SessionManager(this);
        String name = session.isLoggedIn() ? session.getFullName() : "";
        if (name == null || name.trim().isEmpty()) name = session.getUsername();
        String role = session.isLoggedIn() ? session.getRolName() : "";
        renderAccount(name, role, session.isLoggedIn() ? session.getWorkerPhoto() : "");
    }

    private void renderAccount(String fullName, String role, String photo) {
        String safeFull = fullName == null ? "" : fullName.trim();
        accountName.setText(AdminMenuFormatter.extractFirstName(safeFull));
        accountRole.setText(AdminMenuFormatter.resolveRoleLabel(role));
        accountInitials.setText(AdminDashboardHeaderFormatter.extractInitials(
                safeFull.isEmpty() ? "AD" : safeFull, new Locale("es", "ES")));
        CollaboratorProfilePhotoLoader.load(this, accountPhoto, accountInitials, photo);
    }

    private void refreshAccount() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() <= 0) return;
        if (profileRequest != null) profileRequest.cancel();
        int expectedUser = session.getUserId();
        int expectedRole = session.getRoleId();
        int expectedWorker = session.getWorkerId();
        String expectedToken = session.getAccessToken();
        profileRequest = ApiClient.getService().getProfile(session.getBearerToken());
        profileRequest.enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginResponseData>> call,
                                   Response<ApiResponse<LoginResponseData>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != profileRequest || call.isCanceled() || isFinishing() || isDestroyed()
                        || !expectedToken.equals(new SessionManager(AdminModulesMenuActivity.this).getAccessToken())) {
                    return;
                }
                profileRequest = null;
                ApiResponse<LoginResponseData> envelope = response.body();
                LoginResponseData data = envelope == null ? null : envelope.getData();
                LoginResponseData.WorkerInfo worker = data == null ? null : data.getWorker();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || data == null || data.getUser() == null) {
                    if (response.code() == 401) accountRole.setText("Sesión vencida");
                    return;
                }
                if (data.getUser().getIdUsuario() != expectedUser
                        || data.getUser().getIdRol() != expectedRole
                        || (worker == null ? expectedWorker != 0 : worker.getIdTrabajador() != expectedWorker)) {
                    accountRole.setText("No se pudo verificar");
                    return;
                }
                String verifiedName = worker == null ? data.getUser().getUsername() : worker.getNombreCompleto();
                if (verifiedName == null || verifiedName.trim().isEmpty()) {
                    verifiedName = data.getUser().getUsername();
                }
                String photo = worker == null ? "" : worker.getFotoPerfil();
                if (worker != null) {
                    session.updateVerifiedWorkerDetails(expectedWorker, verifiedName,
                            worker.getCargoNombre(), worker.getAgenciaNombre(), worker.getTipo(),
                            worker.getEmail(), photo, worker.getEstado());
                }
                renderAccount(verifiedName, data.getUser().getRolNombre(), photo);
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginResponseData>> call, Throwable error) {
                if (call == profileRequest) profileRequest = null;
            }
        });
    }

    private void setupPersonalSpaceActions() {
        // A. Mis KPI (espacio personal de metas y avances del administrador, sin ranking ni premios)
        bindAction(R.id.admin_menu_my_kpi, () -> {
            Intent intent = new Intent(this, CollaboratorKpiIntegrationActivity.class);
            intent.putExtra(CollaboratorKpiIntegrationActivity.EXTRA_RETURN_ADMIN, true);
            startActivity(intent);
        });

        // B. Mi bienestar (reutiliza módulo oficial de bienestar personal con privacidad estricta)
        bindAction(R.id.admin_menu_my_wellness, () -> {
            Intent intent = new Intent(this, CollaboratorWellnessActivity.class);
            intent.putExtra(CollaboratorWellnessActivity.EXTRA_RETURN_ADMIN, true);
            startActivity(intent);
        });

        // C. Mi horario (reutiliza pantalla personal de jornada sin simular horarios ficticios)
        bindAction(R.id.admin_menu_my_schedule, () -> {
            startActivity(new Intent(this, pe.net.ws.kpiweb.features.schedule.ui.ScheduleActivity.class));
        });

        // D. Mis evaluaciones (resultados personales desde GET /kpi/mis-kpis sin ranking ni canjes)
        bindAction(R.id.admin_menu_my_evaluations, () ->
                startActivity(new Intent(this, AdminMyEvaluationsActivity.class)));
    }

    private void setupAdministrationActions() {
        bindAction(R.id.admin_menu_management_center, () ->
                startActivity(new Intent(this, AdminManagementCenterActivity.class)));
    }

    private void setupSystemOptions() {
        bindAction(R.id.admin_menu_opt_account, this::openAccount);

        bindAction(R.id.admin_menu_opt_settings, () -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.ADMINISTRADOR.name());
            startActivity(intent);
        });

        bindAction(R.id.admin_menu_opt_privacy, () ->
                startActivity(new Intent(this, CollaboratorPrivacyActivity.class)));

        bindAction(R.id.admin_menu_opt_help, () -> {
            Intent intent = new Intent(this, HelpActivity.class);
            intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.ADMINISTRADOR.name());
            startActivity(intent);
        });

        bindAction(R.id.admin_menu_opt_about, () ->
                startActivity(new Intent(this, CollaboratorAboutActivity.class)));

        bindAction(R.id.admin_menu_opt_logout, this::showLogoutConfirmationDialog);
        SigdCreditsHelper.bindMenuFooter(this, findViewById(android.R.id.content), false);
    }

    private void bindAction(int viewId, Runnable action) {
        View view = findViewById(viewId);
        if (view == null) return;
        attachPressFeedback(view);
        view.setOnClickListener(v -> action.run());
    }

    private void attachPressFeedback(View view) {
        if (isReduceMotionEnabled()) return;
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(110L).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(130L).start();
            }
            return false;
        });
    }

    private boolean isReduceMotionEnabled() {
        try {
            return Settings.Global.getFloat(
                    getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void showLogoutConfirmationDialog() {
        View content = getLayoutInflater().inflate(R.layout.admin_dialog_logout, null);
        AdminTypography.apply(this, content);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(content).create();
        content.findViewById(R.id.admin_logout_cancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.admin_logout_confirm).setOnClickListener(v -> {
            dialog.dismiss();
            pe.net.ws.kpiweb.navigation.AccountNavigation.signOutConfirmed(this);
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(420));
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.68f;
            window.setAttributes(params);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
