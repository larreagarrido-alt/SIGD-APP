package pe.net.ws.kpiweb.features.collaborator.common;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity;
import pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorModulesMenuActivity;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationRepository;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsActivity;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessActivity;

/**
 * Controlador reutilizable para la Barra de Navegación Inferior del Colaborador.
 * Orden: INICIO | BIENESTAR | KPI | NOTIFICACIONES | MENÚ
 */
public final class CollaboratorBottomNavHelper {

    public enum NavTab {
        HOME,
        WELLNESS,
        KPI,
        NOTIFICATIONS,
        MENU
    }

    private CollaboratorBottomNavHelper() {
    }

    public static void setup(AppCompatActivity activity, NavTab activeTab) {
        View navHome = activity.findViewById(R.id.collab_nav_home);
        View navWellness = activity.findViewById(R.id.collab_nav_wellness);
        View navKpi = activity.findViewById(R.id.collab_nav_kpi);
        View navNotifs = activity.findViewById(R.id.collab_nav_notifs);
        View navMenu = activity.findViewById(R.id.collab_nav_menu);

        if (navHome == null) {
            return;
        }

        applyTabState(activity, activeTab,
                R.id.collab_nav_ind_home, R.id.collab_nav_ic_home, R.id.collab_nav_tv_home, NavTab.HOME);
        applyTabState(activity, activeTab,
                R.id.collab_nav_ind_wellness, R.id.collab_nav_ic_wellness, R.id.collab_nav_tv_wellness, NavTab.WELLNESS);
        applyKpiCenterState(activity, activeTab);
        applyTabState(activity, activeTab,
                R.id.collab_nav_ind_notifs, R.id.collab_nav_ic_notifs, R.id.collab_nav_tv_notifs, NavTab.NOTIFICATIONS);
        applyTabState(activity, activeTab,
                R.id.collab_nav_ind_menu, R.id.collab_nav_ic_menu, R.id.collab_nav_tv_menu, NavTab.MENU);

        updateUnreadBadge(activity);

        navHome.setOnClickListener(v -> {
            if (activeTab != NavTab.HOME) {
                navigateTo(activity, CollaboratorDashboardActivity.class);
            }
        });

        if (navWellness != null) {
            navWellness.setOnClickListener(v -> {
                if (activeTab != NavTab.WELLNESS) {
                    navigateTo(activity, CollaboratorWellnessActivity.class);
                }
            });
        }

        if (navKpi != null) {
            navKpi.setOnClickListener(v -> {
                if (activeTab != NavTab.KPI) {
                    CollaboratorKpiIntegrationActivity.openKpiSection(activity);
                }
            });
        }

        if (navNotifs != null) {
            navNotifs.setOnClickListener(v -> {
                if (activeTab != NavTab.NOTIFICATIONS) {
                    navigateTo(activity, CollaboratorNotificationsActivity.class);
                }
            });
        }

        if (navMenu != null) {
            navMenu.setOnClickListener(v -> {
                if (activeTab != NavTab.MENU) {
                    navigateTo(activity, CollaboratorModulesMenuActivity.class);
                }
            });
        }
    }

    public static void updateUnreadBadge(AppCompatActivity activity) {
        TextView badge = activity.findViewById(R.id.collab_nav_notif_badge);
        if (badge == null) {
            return;
        }
        CollaboratorProfile profile = CollaboratorSessionManager.getInstance(activity).getActiveProfile();
        int unread = CollaboratorNotificationRepository.getInstance(activity)
                .getUnreadCount(profile.getId());
        if (unread > 0) {
            badge.setText(unread > 9 ? "9+" : String.valueOf(unread));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private static void applyTabState(AppCompatActivity activity, NavTab activeTab,
                                      int indicatorId, int iconId, int labelId, NavTab tab) {
        boolean isActive = (activeTab == tab);
        int activeColor = activity.getColor(R.color.collab_nav_active);
        int inactiveColor = activity.getColor(R.color.collab_nav_inactive);
        int targetColor = isActive ? activeColor : inactiveColor;

        View indicator = activity.findViewById(indicatorId);
        if (indicator != null) {
            indicator.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
        }

        ImageView icon = activity.findViewById(iconId);
        if (icon != null) {
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(targetColor));
        }

        TextView label = activity.findViewById(labelId);
        if (label != null) {
            label.setTextColor(targetColor);
            label.setTypeface(CollaboratorThemeHelper.readableTypeface(
                    isActive ? Typeface.BOLD : Typeface.NORMAL));
        }
    }

    private static void applyKpiCenterState(AppCompatActivity activity, NavTab activeTab) {
        boolean isActive = (activeTab == NavTab.KPI);
        View indicator = activity.findViewById(R.id.collab_nav_ind_kpi);
        if (indicator != null) {
            indicator.setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
        }
        TextView label = activity.findViewById(R.id.collab_nav_tv_kpi);
        if (label != null) {
            int color = isActive
                    ? activity.getColor(R.color.collab_nav_active)
                    : activity.getColor(R.color.collab_purple_accent);
            label.setTextColor(color);
            label.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        }
    }

    private static void navigateTo(AppCompatActivity current, Class<?> targetClass) {
        Intent intent = new Intent(current, targetClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        current.startActivity(intent);
        current.overridePendingTransition(0, 0);
    }
}
