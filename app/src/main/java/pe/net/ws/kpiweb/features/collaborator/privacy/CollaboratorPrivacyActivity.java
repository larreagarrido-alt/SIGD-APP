package pe.net.ws.kpiweb.features.collaborator.privacy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationHelper;

/**
 * Pantalla de Privacidad y Permisos del Colaborador (WEBSITE).
 * Permite:
 * 1. Ver el estado real de los permisos utilizados por WEBSITE.
 * 2. Abrir la configuración de permisos de la aplicación en el sistema Android.
 * 3. Consultar información transparente sobre el almacenamiento local de preferencias.
 * 4. Consultar el estado de la Política de Privacidad institucional sin inventar textos legales.
 */
public class CollaboratorPrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_privacy);

        CollaboratorThemeHelper.setupSystemBars(this);

        View btnBack = findViewById(R.id.collab_privacy_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnSysSettings = findViewById(R.id.collab_privacy_btn_system_settings);
        if (btnSysSettings != null) {
            btnSysSettings.setOnClickListener(v -> openSystemAppDetailsSettings());
        }

        View btnPolicy = findViewById(R.id.collab_privacy_btn_policy);
        if (btnPolicy != null) {
            btnPolicy.setOnClickListener(v -> showPrivacyPolicyStatusDialog());
        }

        refreshPermissionStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
        refreshPermissionStatus();
    }

    private void refreshPermissionStatus() {
        TextView tvNotifPerm = findViewById(R.id.collab_privacy_tv_notif_perm_status);
        if (tvNotifPerm != null) {
            boolean granted = CollaboratorNotificationHelper.hasNotificationPermission(this);
            if (granted) {
                tvNotifPerm.setText("Concedido");
                tvNotifPerm.setBackgroundResource(R.drawable.collab_bg_pill_green);
                tvNotifPerm.setTextColor(getColor(R.color.collab_status_completed));
            } else {
                tvNotifPerm.setText("No concedido");
                tvNotifPerm.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                tvNotifPerm.setTextColor(getColor(R.color.collab_status_pending));
            }
        }

    }

    private void openSystemAppDetailsSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la pantalla de permisos del sistema", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrivacyPolicyStatusDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Política de Privacidad • WEBSITE")
                .setMessage("El documento oficial de Política de Privacidad y Tratamiento de Datos de WEBSITE se encuentra pendiente de publicación por el área legal e institucional.\n\n"
                        + "Mientras tanto, la aplicación opera únicamente con preferencias locales en tu dispositivo sin recopilar datos sensibles no autorizados.")
                .setPositiveButton("Entendido", null)
                .show();
    }
}
