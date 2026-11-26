package pe.net.ws.kpiweb;

import android.app.Activity;
import android.app.Application;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminKpiNoticeWatcher;

/** Polls server KPI notices while any SIGD screen is visible. */
public final class SigdApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private AdminKpiNoticeWatcher noticeWatcher;
    private int visibleActivities;

    @Override public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
        noticeWatcher = new AdminKpiNoticeWatcher(this);
        registerActivityLifecycleCallbacks(this);
    }

    @Override public void onActivityStarted(@NonNull Activity activity) {
        if (visibleActivities++ == 0) noticeWatcher.start();
    }

    @Override public void onActivityStopped(@NonNull Activity activity) {
        visibleActivities = Math.max(0, visibleActivities - 1);
        if (visibleActivities == 0) noticeWatcher.stop();
    }

    @Override public void onActivityCreated(@NonNull Activity activity, Bundle state) { }
    @Override public void onActivityResumed(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || !new SessionManager(this).isLoggedIn()
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) return;
        android.content.SharedPreferences prefs = getSharedPreferences("sigd_notification_permission", Context.MODE_PRIVATE);
        if (prefs.getBoolean("asked", false)) return;
        prefs.edit().putBoolean("asked", true).apply();
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 4103);
    }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle state) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}
