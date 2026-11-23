package pe.net.ws.kpiweb.features.collaborator.about;

import android.content.pm.PackageInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;

/**
 * Pantalla compartida "Acerca de SIGD" para Administrador y Colaborador.
 * Responde dinámicamente al tema claro y oscuro de SIGD.
 */
public class CollaboratorAboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_about);

        CollaboratorThemeHelper.setupSystemBars(this);
        styleAccentBadges();

        View btnBack = findViewById(R.id.collab_about_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvYear = findViewById(R.id.collab_about_tv_creation_year);
        if (tvYear != null) {
            tvYear.setText(SigdCreditsHelper.COPYRIGHT_YEAR);
        }

        TextView tvSignature = findViewById(R.id.collab_about_tv_credit_signature);
        if (tvSignature != null) {
            tvSignature.setText(SigdCreditsHelper.CREDIT_SIGNATURE);
        }

        TextView tvCopyright = findViewById(R.id.collab_about_tv_copyright);
        if (tvCopyright != null) {
            tvCopyright.setText(SigdCreditsHelper.COPYRIGHT_LINE);
        }

        bindAppVersion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void styleAccentBadges() {
        int purpleBadgeBg = ContextCompat.getColor(this, R.color.admin_dash_badge_purple_bg);
        applyBadge(findViewById(R.id.collab_about_purpose_badge), purpleBadgeBg, 12);
        applyBadge(findViewById(R.id.collab_about_tv_creation_year), purpleBadgeBg, 99);
        applyBadge(findViewById(R.id.collab_about_tv_credit_signature), purpleBadgeBg, 99);
    }

    private void applyBadge(View view, int color, int radiusDp) {
        if (view == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(Math.round(radiusDp * getResources().getDisplayMetrics().density));
        view.setBackground(bg);
    }

    private void bindAppVersion() {
        TextView tvVersion = findViewById(R.id.collab_about_tv_version);
        if (tvVersion == null) return;

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName != null ? pInfo.versionName : "";
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? pInfo.getLongVersionCode()
                    : pInfo.versionCode;
            tvVersion.setText(SigdCreditsHelper.formatInstalledVersion(versionName, versionCode));
        } catch (Exception e) {
            tvVersion.setText(SigdCreditsHelper.formatInstalledVersion("", 0L));
        }
    }
}
