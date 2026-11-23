package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import pe.net.ws.kpiweb.R;

/**
 * Pantalla principal de Bienestar organizacional (Administrador — SIGD).
 * Separa claramente en dos apartados independientes:
 * 1) Evaluación mensual (participación y tendencias agregadas)
 * 2) Evaluación de Ryff (aplicaciones, vigencia, seguimiento y resultados autorizados)
 */
public final class AdminWellnessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_wellness);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        findViewById(R.id.wellness_hub_btn_back).setOnClickListener(v -> finish());

        applyIconContainer(findViewById(R.id.wellness_hub_monthly_icon_box), 0xFF382252, 0xFFD8B4FE);
        applyIconContainer(findViewById(R.id.wellness_hub_ryff_icon_box), 0xFF163F37, 0xFF57D6A0);

        View monthlyCard = findViewById(R.id.wellness_hub_card_monthly);
        attachPressFeedback(monthlyCard);
        monthlyCard.setOnClickListener(v ->
                startActivity(new Intent(this, AdminMonthlyWellnessActivity.class)));

        View ryffCard = findViewById(R.id.wellness_hub_card_ryff);
        attachPressFeedback(ryffCard);
        ryffCard.setOnClickListener(v ->
                startActivity(new Intent(this, AdminRyffActivity.class)));

        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    private void applyIconContainer(@Nullable View target, int bgColor, int accentColor) {
        if (target == null) return;
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(14f * density);
        bg.setColor(bgColor);
        bg.setStroke(Math.max(1, Math.round(1f * density)), (accentColor & 0x00FFFFFF) | 0x45000000);
        target.setBackground(bg);
    }

    private void attachPressFeedback(@Nullable View view) {
        if (view == null || isReduceMotionEnabled()) return;
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(105L).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(125L).start();
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
}
