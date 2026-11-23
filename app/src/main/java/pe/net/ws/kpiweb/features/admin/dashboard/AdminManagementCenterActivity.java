package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminManagementCenterHelper.ModuleId;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminManagementCenterHelper.ModuleItem;

/**
 * Centro de Gestión del Administrador en formato lista vertical premium.
 * Agrupa las herramientas administrativas; Reportes permanece en la barra inferior.
 */
public final class AdminManagementCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_management_center);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        findViewById(R.id.mgmt_center_btn_back).setOnClickListener(v -> finish());

        List<ModuleItem> modules = AdminManagementCenterHelper.buildAdminModules();
        TextView tvCount = findViewById(R.id.mgmt_center_modules_count);
        if (tvCount != null) {
            tvCount.setText(AdminManagementCenterHelper.formatModuleCount(modules.size()));
        }

        LinearLayout listContainer = findViewById(R.id.mgmt_center_list_container);
        renderModuleList(listContainer, modules);

        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    private void renderModuleList(LinearLayout container, List<ModuleItem> modules) {
        if (container == null) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;
        int strokeWidthPx = Math.max(1, Math.round(1f * density));

        for (int i = 0; i < modules.size(); i++) {
            ModuleItem item = modules.get(i);
            View row = inflater.inflate(R.layout.admin_item_management_module, container, false);
            if (i == modules.size() - 1 && row.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) row.getLayoutParams()).bottomMargin = 0;
            }
            FrameLayout iconBox = row.findViewById(R.id.mgmt_row_icon_box);
            ImageView iconView = row.findViewById(R.id.mgmt_row_icon);
            TextView titleView = row.findViewById(R.id.mgmt_row_title);
            TextView descView = row.findViewById(R.id.mgmt_row_desc);

            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setShape(GradientDrawable.RECTANGLE);
            badgeBg.setCornerRadius(14f * density);
            badgeBg.setColor(item.iconBgHex);
            badgeBg.setStroke(strokeWidthPx, (item.iconColorHex & 0x00FFFFFF) | 0x3D000000);
            iconBox.setBackground(badgeBg);

            iconView.setImageResource(resolveIcon(item.id));
            ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(item.iconColorHex));

            titleView.setText(item.title);
            descView.setText(item.description);
            row.setContentDescription(item.title + ". " + item.description);

            attachPressFeedback(row);
            row.setOnClickListener(v -> openModule(item.id));
            container.addView(row);
        }
    }

    @DrawableRes
    private int resolveIcon(ModuleId id) {
        switch (id) {
            case KPI_MANAGEMENT:
                return R.drawable.admin_ic_sliders_horizontal;
            case KPI_SHEET_EDITOR:
                return R.drawable.admin_ic_sliders_horizontal;
            case SCHEDULE_EDITOR:
                return R.drawable.sch_ic_schedule;
            case RANKING:
                return R.drawable.admin_ic_trophy_lucide;
            case REWARDS:
                return R.drawable.collab_ic_gift;
            case WELLNESS:
            default:
                return R.drawable.collab_ic_heart_pulse;
        }
    }

    private void openModule(ModuleId id) {
        switch (id) {
            case KPI_MANAGEMENT:
                startActivity(new Intent(this, AdminKpiOfficialActivity.class));
                break;
            case KPI_SHEET_EDITOR:
                startActivity(new Intent(this,
                        pe.net.ws.kpiweb.features.kpi.ui.KpiReportsEditorActivity.class));
                break;
            case SCHEDULE_EDITOR:
                Intent schedule = new Intent(this,
                        pe.net.ws.kpiweb.features.schedule.ui.ScheduleActivity.class);
                schedule.putExtra(pe.net.ws.kpiweb.features.schedule.ui.ScheduleActivity.EXTRA_EDITOR, true);
                startActivity(schedule);
                break;
            case RANKING:
                startActivity(new Intent(this, AdminRankingActivity.class));
                break;
            case REWARDS:
                startActivity(new Intent(this, AdminRewardsActivity.class));
                break;
            case WELLNESS:
                startActivity(new Intent(this, AdminWellnessActivity.class));
                break;
        }
    }

    private void attachPressFeedback(View view) {
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
