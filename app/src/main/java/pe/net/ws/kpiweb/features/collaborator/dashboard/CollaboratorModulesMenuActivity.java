package pe.net.ws.kpiweb.features.collaborator.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import pe.net.ws.kpiweb.MainActivity;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.account.AccountActivity;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.collaborator.about.CollaboratorAboutActivity;
import pe.net.ws.kpiweb.features.collaborator.about.SigdCreditsHelper;
import pe.net.ws.kpiweb.features.collaborator.attendance.CollaboratorAttendanceActivity;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.privacy.CollaboratorPrivacyActivity;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorRankingPointsActivity;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsActivity;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessActivity;
import pe.net.ws.kpiweb.features.help.HelpActivity;
import pe.net.ws.kpiweb.features.settings.SettingsActivity;

/**
 * Menú del Colaborador (WEBSITE).
 * Presenta espacio preparado para el logo oficial de WEBSITE, tarjeta del
 * colaborador activo,
 * accesos a módulos personales y opciones de cuenta, configuración, privacidad,
 * ayuda,
 * acerca de WEBSITE y cierre de sesión con confirmación real.
 * No incluye opciones exclusivas de Administrador.
 */
public class CollaboratorModulesMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_modules_menu);

        CollaboratorThemeHelper.setupSystemBars(this);

        setupHeaderActions();
        bindActiveCollaboratorSummary();
        setupModuleGridActions();
        setupMenuOptions();
        SigdCreditsHelper.bindMenuFooter(this, findViewById(android.R.id.content), false);
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.MENU);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
        bindActiveCollaboratorSummary();
        SigdCreditsHelper.bindMenuFooter(this, findViewById(android.R.id.content), false);
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.MENU);
    }

    private void setupHeaderActions() {
        View btnClose = findViewById(R.id.collab_menu_btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    private void bindActiveCollaboratorSummary() {
        CollaboratorProfile profile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        if (profile == null)
            return;

        TextView tvAvatar = findViewById(R.id.collab_menu_tv_avatar);
        TextView tvName = findViewById(R.id.collab_menu_tv_user_name);
        TextView tvWorkerType = findViewById(R.id.collab_menu_tv_worker_type);
        TextView tvRole = findViewById(R.id.collab_menu_tv_user_role);

        if (tvAvatar != null)
            tvAvatar.setText(profile.getAvatarInitials());
        CollaboratorProfilePhotoLoader.load(this, findViewById(R.id.collab_menu_iv_photo), tvAvatar,
                new SessionManager(this).getWorkerPhoto());
        if (tvName != null)
            tvName.setText(profile.getFullName());
        if (tvWorkerType != null) {
            String workerType = new SessionManager(this).getWorkerTipo();
            tvWorkerType.setText(workerType == null || workerType.trim().isEmpty()
                    ? "Colaborador"
                    : workerType.trim());
        }
        if (tvRole != null) {
            String job = profile.getJobTitle() == null ? "" : profile.getJobTitle().trim();
            String area = profile.getDepartment() == null ? "" : profile.getDepartment().trim();
            String detail = job.isEmpty() ? area : area.isEmpty() ? job : job + " • " + area;
            if (detail.isEmpty()) {
                tvRole.setVisibility(View.GONE);
            } else {
                tvRole.setText(detail);
                tvRole.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupModuleGridActions() {
        bindClick(R.id.collab_menu_mod_schedule, () -> {
            startActivity(new Intent(this, pe.net.ws.kpiweb.features.schedule.ui.ScheduleActivity.class));
        });

        bindClick(R.id.collab_menu_mod_kpi, () -> CollaboratorKpiIntegrationActivity.openKpiSection(this));

        bindClick(R.id.collab_menu_mod_rewards,
                () -> startActivity(new Intent(this, CollaboratorRewardsActivity.class)));

        bindClick(R.id.collab_menu_mod_ranking,
                () -> startActivity(new Intent(this, CollaboratorRankingPointsActivity.class)));

        bindClick(R.id.collab_menu_mod_wellness,
                () -> startActivity(new Intent(this, CollaboratorWellnessActivity.class)));

        bindClick(R.id.collab_menu_mod_attendance, () -> {
            Intent intent = new Intent(this, CollaboratorAttendanceActivity.class);
            intent.putExtra(CollaboratorAttendanceActivity.EXTRA_SECTION_MODE,
                    CollaboratorAttendanceActivity.MODE_ATTENDANCE);
            startActivity(intent);
        });
    }

    private void setupMenuOptions() {
        Runnable openAccount = () -> {
            Intent intent = new Intent(this, AccountActivity.class);
            intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
            startActivity(intent);
        };

        bindClick(R.id.collab_menu_card_account, openAccount);
        bindClick(R.id.collab_menu_opt_account, openAccount);

        bindClick(R.id.collab_menu_opt_settings, () -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
            startActivity(intent);
        });

        bindClick(R.id.collab_menu_opt_privacy,
                () -> startActivity(new Intent(this, CollaboratorPrivacyActivity.class)));

        bindClick(R.id.collab_menu_opt_help, () -> {
            Intent intent = new Intent(this, HelpActivity.class);
            intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
            startActivity(intent);
        });

        bindClick(R.id.collab_menu_opt_about, () -> startActivity(new Intent(this, CollaboratorAboutActivity.class)));

        bindClick(R.id.collab_menu_opt_logout, this::showLogoutConfirmationDialog);
    }

    private void bindClick(int viewId, Runnable action) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> action.run());
        }
    }

    private void showLogoutConfirmationDialog() {
        CollaboratorBannerHelper.showPremiumModal(
                this,
                "SEGURIDAD DE SESIÓN • WEBSITE",
                getString(R.string.collab_logout_dialog_title),
                getString(R.string.collab_logout_dialog_msg),
                getString(R.string.collab_logout_dialog_confirm),
                () -> {
                    // Limpiar estado de sesión activa del colaborador
                    CollaboratorSessionManager.getInstance(this).clearSession();

                    pe.net.ws.kpiweb.navigation.AccountNavigation.signOutConfirmed(this);
                },
                getString(R.string.collab_logout_dialog_cancel));
    }
}
