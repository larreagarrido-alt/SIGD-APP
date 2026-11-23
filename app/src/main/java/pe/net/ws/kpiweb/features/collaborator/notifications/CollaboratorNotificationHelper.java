package pe.net.ws.kpiweb.features.collaborator.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.settings.data.SettingsPreferences;

/**
 * Gestor de recordatorios y notificaciones locales de prueba para el Colaborador.
 * Respeta las preferencias configuradas en SettingsPreferences (alertas maestras,
 * recordatorios de KPI y avisos generales) y el permiso POST_NOTIFICATIONS en Android 13+.
 */
public final class CollaboratorNotificationHelper {

    public static final String CHANNEL_ID_REMINDERS = "website_collab_kpi_reminders";
    private static final String CHANNEL_NAME = "Recordatorios de KPI y Avisos WEBSITE";
    private static final String CHANNEL_DESC = "Alertas locales de entregas de KPI, evaluaciones y avisos de WEBSITE";

    private CollaboratorNotificationHelper() {
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Verifica si los recordatorios locales están permitidos según los interruptores de Configuración.
     */
    public static boolean areKpiRemindersAllowedBySettings(Context context) {
        SettingsPreferences prefs = SettingsPreferences.getInstance(context);
        return prefs.isMasterNotifEnabled() && prefs.isKpiNotifEnabled();
    }

    /**
     * Dispara una notificación local del sistema Android para probar recordatorios de KPI.
     *
     * @return true si la notificación del sistema fue emitida, false si falta permiso o está desactivada.
     */
    public static boolean triggerSystemReminderNotification(Context context, String title, String message) {
        if (!areKpiRemindersAllowedBySettings(context)) {
            return false;
        }
        if (!hasNotificationPermission(context)) {
            return false;
        }

        ensureNotificationChannel(context);

        Intent intent = new Intent(context, CollaboratorNotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2026, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
                .setSmallIcon(R.drawable.collab_ic_nav_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify((int) (System.currentTimeMillis() & 0xFFFFFFF), builder.build());
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
