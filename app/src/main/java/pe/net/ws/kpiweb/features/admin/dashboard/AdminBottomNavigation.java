package pe.net.ws.kpiweb.features.admin.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalActivity;

/** Keeps the five main admin destinations available on secondary screens. */
final class AdminBottomNavigation {
    private AdminBottomNavigation() {}

    static void attach(Activity activity, int activeTab) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        ViewGroup container = (ViewGroup) content;
        View page = container.getChildAt(0);
        if (page == null) return;
        int height = Math.round(68 * activity.getResources().getDisplayMetrics().density);
        // The page used to consume system insets while the overlaid bar did not.
        // Put both in one inset-aware column so the bar stays above Android's buttons.
        container.removeView(page);
        page.setFitsSystemWindows(false);
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setFitsSystemWindows(true);
        column.addView(page, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        View nav = activity.getLayoutInflater().inflate(R.layout.admin_layout_bottom_nav, column, false);
        column.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));
        container.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int[] tabs = {R.id.admin_nav_tab_staff, R.id.admin_nav_tab_reports,
                R.id.admin_nav_tab_home, R.id.admin_nav_tab_notifs, R.id.admin_nav_tab_menu};
        int[] indicators = {R.id.admin_nav_ind_staff, R.id.admin_nav_ind_reports,
                R.id.admin_nav_ind_home, R.id.admin_nav_ind_notifs, R.id.admin_nav_ind_menu};
        for (int i = 0; i < tabs.length; i++) {
            final int destination = i;
            View tab = nav.findViewById(tabs[i]);
            tab.setOnClickListener(v -> open(activity, destination));
            View indicator = nav.findViewById(indicators[i]);
            if (indicator != null) indicator.setVisibility(i == activeTab ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private static void open(Activity activity, int tab) {
        Class<?> target;
        switch (tab) {
            case 0: target = AdminPersonalActivity.class; break;
            case 1: target = AdminKpiOfficialActivity.class; break;
            case 3: target = AdminNotificationsActivity.class; break;
            case 4: target = AdminModulesMenuActivity.class; break;
            default: target = AdminDashboardActivity.class;
        }
        if (activity.getClass() == target) return;
        Intent intent = new Intent(activity, target);
        if (tab == 1) intent.putExtra(AdminKpiOfficialActivity.EXTRA_REPORTS_CENTER, true);
        activity.startActivity(intent);
    }
}
