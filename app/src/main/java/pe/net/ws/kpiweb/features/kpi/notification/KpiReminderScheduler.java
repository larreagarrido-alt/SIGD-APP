package pe.net.ws.kpiweb.features.kpi.notification;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;

import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;

/**
 * Programador automático de alarmas y notificaciones del módulo KPI:
 * 1. 2:00 PM (14:00 hrs): Turno Mañana.
 * 2. 6:40 PM (18:40 hrs): Turno Tarde.
 * 3. 8:00 PM Diario (20:00 hrs): Cierre diario.
 * 4. Sábado 8:00 PM (20:00 hrs): Verificación de turnos pendientes de la semana con sonido doble.
 */
public final class KpiReminderScheduler {

    private static final int ALARM_REQUEST_CODE_2PM = 14000;
    private static final int ALARM_REQUEST_CODE_640PM = 18400;
    private static final int ALARM_REQUEST_CODE_8PM = 20000;
    private static final int ALARM_REQUEST_CODE_SATURDAY_8PM = 20007;

    private KpiReminderScheduler() {}

    /**
     * Solicita permiso de notificaciones en Android 13+ si aún no ha sido concedido.
     */
    public static void requestNotificationPermissionIfNeeded(Activity activity) {
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1008
                );
            }
        }
    }

    /**
     * Programa todas las alarmas requeridas para los Practicantes y Colaboradores.
     */
    public static void scheduleAllReminders(Context context) {
        if (context == null) return;
        schedule2PmReminder(context);
        schedule640PmReminder(context);
        scheduleDaily8PmReminder(context);
        scheduleSaturday8PmReminder(context);
    }

    /**
     * Alarma 1: Todos los días a las 2:00 PM (14:00 hrs) - Turno Mañana
     */
    public static void schedule2PmReminder(Context context) {
        scheduleAlarmAtTime(context, 14, 0, ALARM_REQUEST_CODE_2PM,
                KpiDailyReminderReceiver.ACTION_REMINDER_2PM, "2PM");
    }

    /**
     * Alarma 2: Todos los días a las 6:40 PM (18:40 hrs) - Turno Tarde
     */
    public static void schedule640PmReminder(Context context) {
        scheduleAlarmAtTime(context, 18, 40, ALARM_REQUEST_CODE_640PM,
                KpiDailyReminderReceiver.ACTION_REMINDER_640PM, "640PM");
    }

    /**
     * Alarma 3: Todos los días a las 8:00 PM (20:00 hrs) - Cierre diario
     */
    public static void scheduleDaily8PmReminder(Context context) {
        scheduleAlarmAtTime(context, 20, 0, ALARM_REQUEST_CODE_8PM,
                KpiDailyReminderReceiver.ACTION_REMINDER_8PM, "8PM");
    }

    /**
     * Alarma 4: Sábados a las 8:00 PM (20:00 hrs) - Si faltan turnos, suena DOBLE.
     */
    public static void scheduleSaturday8PmReminder(Context context) {
        if (context == null) return;
        KpiDailyReminderReceiver.createNotificationChannel(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, KpiDailyReminderReceiver.class);
        intent.setAction(KpiDailyReminderReceiver.ACTION_REMINDER_SATURDAY_8PM);
        intent.putExtra(KpiDailyReminderReceiver.EXTRA_ALARM_TYPE, "SATURDAY_8PM");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE_SATURDAY_8PM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar target = Calendar.getInstance();
        target.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        target.set(Calendar.HOUR_OF_DAY, 20); // 8:00 PM
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.getTimeInMillis() <= System.currentTimeMillis()) {
            target.add(Calendar.WEEK_OF_YEAR, 1);
        }

        setExactAlarm(alarmManager, target.getTimeInMillis(), pendingIntent);
    }

    private static void scheduleAlarmAtTime(Context context, int hour24, int minute,
                                            int requestCode, String action, String alarmType) {
        if (context == null) return;
        KpiDailyReminderReceiver.createNotificationChannel(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, KpiDailyReminderReceiver.class);
        intent.setAction(action);
        intent.putExtra(KpiDailyReminderReceiver.EXTRA_ALARM_TYPE, alarmType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour24);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.getTimeInMillis() <= System.currentTimeMillis()) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        setExactAlarm(alarmManager, target.getTimeInMillis(), pendingIntent);
    }

    private static void setExactAlarm(AlarmManager alarmManager, long triggerAtMillis, PendingIntent pendingIntent) {
        try {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } catch (SecurityException e) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
    }

    // =========================================================================
    // DISPARADORES INMEDIATOS PARA PRUEBA EN VIVO
    // =========================================================================

    public static void triggerInstant2PmReminder(Context context) {
        if (context == null) return;
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        KpiDailyReminderReceiver.show2PmMorningNotification(context, activeRole);
        checkFallbackAudio(context);
        Toast.makeText(context, "Notificación 2:00 PM (Turno Mañana) enviada con sonido", Toast.LENGTH_LONG).show();
    }

    public static void triggerInstant640PmReminder(Context context) {
        if (context == null) return;
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        KpiDailyReminderReceiver.show640PmAfternoonNotification(context, activeRole);
        checkFallbackAudio(context);
        Toast.makeText(context, "Notificación 6:40 PM (Turno Tarde) enviada con sonido", Toast.LENGTH_LONG).show();
    }

    public static void triggerInstantSaturday8PmDoubleReminder(Context context) {
        if (context == null) return;
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        int pending = KpiDailyReminderReceiver.countPendingKpiShifts(context, activeRole);
        KpiDailyReminderReceiver.showSaturdayClosingNotification(context, activeRole, Math.max(3, pending), true);
        Toast.makeText(context, "Notificación Sábado 8:00 PM con DOBLE TIMBRE enviada", Toast.LENGTH_LONG).show();
    }

    public static void triggerInstantReminderCheck(Context context) {
        if (context == null) return;
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        int pending = KpiDailyReminderReceiver.countPendingKpiShifts(context, activeRole);
        boolean isDouble = pending > 2;
        boolean isWeekSubmitted = KpiDesignerMemoryRepository.getInstance(context)
                .isCurrentWeekFinalSubmitted(activeRole);

        KpiDailyReminderReceiver.showDaily8PmCheckNotification(
                context,
                activeRole,
                pending,
                isDouble,
                isWeekSubmitted
        );

        checkFallbackAudio(context);

        if (pending > 2) {
            Toast.makeText(context,
                    "Notificación 8:00 PM: ¡DOBLE TIMBRE activado por tener " + pending + " KPIs sin hacer!",
                    Toast.LENGTH_LONG).show();
        } else if (pending > 0) {
            Toast.makeText(context,
                    "Notificación 8:00 PM: Tienes " + pending + " turno(s) pendiente(s)",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context,
                    "Notificación 8:00 PM enviada (Todos los KPIs al día)",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static void checkFallbackAudio(Context context) {
        boolean hasNotifPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean ringerNormal = am != null && am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
        if (!hasNotifPerm || !ringerNormal) {
            KpiSoundHelper.playReminder8PmTone(context);
        }
    }
}
