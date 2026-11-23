package pe.net.ws.kpiweb.features.kpi.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pe.net.ws.kpiweb.core.session.SessionManager;

/** Restores local reminders after Android removes alarms on a reboot or app update. */
public final class KpiReminderBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        SessionManager session = new SessionManager(context);
        if (session.isLoggedIn() && session.getWorkerId() > 0
                && (session.getRoleId() == 5 || session.getRoleId() == 6)) {
            KpiReminderScheduler.scheduleAllReminders(context);
        }
    }
}
