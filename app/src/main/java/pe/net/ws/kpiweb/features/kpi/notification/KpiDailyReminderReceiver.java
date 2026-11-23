package pe.net.ws.kpiweb.features.kpi.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import pe.net.ws.kpiweb.MainActivity;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository;

/**
 * Receptor automático de recordatorios de KPI para Practicantes y Colaboradores:
 * 1. 2:00 PM (14:00 hrs): Avisa que está listo para llenar el KPI del Turno Mañana.
 * 2. 6:40 PM (18:40 hrs): Avisa sobre el KPI del Turno Tarde.
 * 3. 8:00 PM Diario (20:00 hrs): Recordatorio general de días pendientes.
 * 4. Sábado 8:00 PM (20:00 hrs): Si no ha llenado ninguno de los dos turnos o tiene pendientes,
 *    suena DOBLE con una sola notificación.
 */
public class KpiDailyReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_REMINDER_2PM = "pe.net.ws.kpiweb.ACTION_REMINDER_2PM";
    public static final String ACTION_REMINDER_640PM = "pe.net.ws.kpiweb.ACTION_REMINDER_640PM";
    public static final String ACTION_REMINDER_8PM = "pe.net.ws.kpiweb.ACTION_REMINDER_8PM";
    public static final String ACTION_REMINDER_SATURDAY_8PM = "pe.net.ws.kpiweb.ACTION_REMINDER_SATURDAY_8PM";

    private static final String LEGACY_CHANNEL_ID = "kpi_daily_8pm_channel";
    public static final String CHANNEL_ID = "kpi_daily_8pm_custom_tone_v2";

    public static final int NOTIFICATION_ID_2PM = 202614;
    public static final int NOTIFICATION_ID_640PM = 202618;
    public static final int NOTIFICATION_ID = 202608;
    public static final int NOTIFICATION_ID_SATURDAY = 202620;

    public static final String EXTRA_FORCE_SHOW = "extra_force_show";
    public static final String EXTRA_ALARM_TYPE = "extra_alarm_type";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        boolean forceShow = intent != null && intent.getBooleanExtra(EXTRA_FORCE_SHOW, false);
        if (!forceShow) {
            pe.net.ws.kpiweb.core.session.SessionManager session =
                    new pe.net.ws.kpiweb.core.session.SessionManager(context);
            int worker = session.getWorkerId();
            if (!session.isLoggedIn() || worker <= 0) {
                reschedule(context, intent == null ? null : intent.getAction(),
                        intent == null ? null : intent.getStringExtra(EXTRA_ALARM_TYPE));
                return;
            }
            String token = session.getAccessToken();
            PendingResult pending = goAsync();
            pe.net.ws.kpiweb.core.network.ApiClient.getService()
                    .getAllowedKpiForms(session.getBearerToken())
                    .enqueue(new retrofit2.Callback<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>>() {
                        @Override public void onResponse(retrofit2.Call<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>> call,
                                retrofit2.Response<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>> response) {
                            try {
                                if (response.errorBody() != null) response.errorBody().close();
                                pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject> body = response.body();
                                com.google.gson.JsonObject data = body == null ? null : body.getData();
                                pe.net.ws.kpiweb.core.session.SessionManager current =
                                        new pe.net.ws.kpiweb.core.session.SessionManager(context);
                                if (response.isSuccessful() && body != null && body.isSuccessful()
                                        && data != null && data.has("calendario_laboral")
                                        && data.get("calendario_laboral").isJsonObject()
                                        && data.has("turnos_kpi_pendientes")
                                        && data.get("turnos_kpi_pendientes").isJsonPrimitive()
                                        && current.isLoggedIn() && current.getWorkerId() == worker
                                        && token.equals(current.getAccessToken())
                                        && pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.getInstance(context)
                                        .applyServerCalendar(new org.json.JSONObject(
                                                data.getAsJsonObject("calendario_laboral").toString()), worker)) {
                                    int pendingCount=data.get("turnos_kpi_pendientes").getAsInt();
                                    if (pendingCount<0) throw new IllegalArgumentException("KPI pending count");
                                    dispatchConfirmedReminder(context, intent, pendingCount);
                                    return;
                                }
                                reschedule(context, intent == null ? null : intent.getAction(),
                                        intent == null ? null : intent.getStringExtra(EXTRA_ALARM_TYPE));
                            } catch (Exception invalid) {
                                reschedule(context, intent == null ? null : intent.getAction(),
                                        intent == null ? null : intent.getStringExtra(EXTRA_ALARM_TYPE));
                            } finally { pending.finish(); }
                        }
                        @Override public void onFailure(retrofit2.Call<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>> call,
                                Throwable error) {
                            try { reschedule(context, intent == null ? null : intent.getAction(),
                                    intent == null ? null : intent.getStringExtra(EXTRA_ALARM_TYPE)); }
                            finally { pending.finish(); }
                        }
                    });
            return;
        }
        dispatchConfirmedReminder(context, intent, 0);
    }

    private void dispatchConfirmedReminder(Context context, Intent intent, int serverPendingCount) {

        KpiDesignerMemoryRepository.init(context);
        KpiJuniorMemoryRepository.init(context);
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        if (activeRole == null || activeRole.trim().isEmpty()) {
            activeRole = "equipo";
        }

        String action = intent != null ? intent.getAction() : null;
        String alarmType = intent != null ? intent.getStringExtra(EXTRA_ALARM_TYPE) : null;
        boolean forceShow = intent != null && intent.getBooleanExtra(EXTRA_FORCE_SHOW, false);
        pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository calendar =
                pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.getInstance(context);
        if (!hasScheduledShiftThisWeek(calendar)) {
            reschedule(context, action, alarmType);
            return;
        }
        pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.AttendanceDay today =
                calendar.getDayAttendance(Calendar.getInstance(TimeZone.getTimeZone("America/Lima")));
        if (today.onApprovedLeave) {
            reschedule(context, action, alarmType);
            return;
        }

        if (ACTION_REMINDER_2PM.equals(action) || "2PM".equals(alarmType)) {
            if (today.morningAttended) show2PmMorningNotification(context, activeRole);
            KpiReminderScheduler.schedule2PmReminder(context);
            return;
        }

        if (ACTION_REMINDER_640PM.equals(action) || "640PM".equals(alarmType)) {
            if (today.afternoonAttended) show640PmAfternoonNotification(context, activeRole);
            KpiReminderScheduler.schedule640PmReminder(context);
            return;
        }

        if (ACTION_REMINDER_SATURDAY_8PM.equals(action) || "SATURDAY_8PM".equals(alarmType)) {
            int pendingCount = serverPendingCount;
            boolean isDouble = (pendingCount > 2) || forceShow;
            if (pendingCount > 0 || forceShow) {
                showSaturdayClosingNotification(context, activeRole, pendingCount, isDouble);
            }
            KpiReminderScheduler.scheduleSaturday8PmReminder(context);
            return;
        }

        // Caso por defecto: Recordatorio diario 8:00 PM
        int pendingCount = serverPendingCount;
        boolean isDouble = (pendingCount > 2);
        boolean isWeekSubmitted = pendingCount == 0;

        if (forceShow || pendingCount > 0 || !isWeekSubmitted) {
            showDaily8PmCheckNotification(context, activeRole, pendingCount, isDouble, isWeekSubmitted);
        }

        KpiReminderScheduler.scheduleDaily8PmReminder(context);
    }

    private static boolean hasScheduledShiftThisWeek(pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository calendar) {
        for (int day=0; day<6; day++) {
            pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.AttendanceDay row=calendar.getDayAttendance(day);
            if (row.morningAttended || row.afternoonAttended) return true;
        }
        return false;
    }

    private static void reschedule(Context context, String action, String alarmType) {
        if (ACTION_REMINDER_2PM.equals(action) || "2PM".equals(alarmType)) KpiReminderScheduler.schedule2PmReminder(context);
        else if (ACTION_REMINDER_640PM.equals(action) || "640PM".equals(alarmType)) KpiReminderScheduler.schedule640PmReminder(context);
        else if (ACTION_REMINDER_SATURDAY_8PM.equals(action) || "SATURDAY_8PM".equals(alarmType)) KpiReminderScheduler.scheduleSaturday8PmReminder(context);
        else KpiReminderScheduler.scheduleDaily8PmReminder(context);
    }

    /**
     * 1. Notificación de las 2:00 PM (Turno Mañana)
     */
    public static void show2PmMorningNotification(Context context, String roleName) {
        String title = "¡Son las 2:00 PM! Listo para llenar tu KPI de la Mañana";
        String body = "Hola (" + roleName + "), tu Turno Mañana ha concluido. Ingresa a la app para registrar tus actividades.";
        dispatchNotification(context, NOTIFICATION_ID_2PM, title, body, false);
    }

    /**
     * 2. Notificación de las 6:40 PM (Turno Tarde)
     */
    public static void show640PmAfternoonNotification(Context context, String roleName) {
        String title = "¡Son las 6:40 PM! Listo para llenar tu KPI de la Tarde";
        String body = "Hola (" + roleName + "), tu Turno Tarde ha concluido. Ingresa a la app para registrar tus avances.";
        dispatchNotification(context, NOTIFICATION_ID_640PM, title, body, false);
    }

    /**
     * 3. Notificación de Sábado 8:00 PM: Cierre definitivo antes del envío al Licenciado.
     * Suena DOBLE TIMBRE si hay más de 2 KPIs no hechos.
     */
    public static void showSaturdayClosingNotification(Context context, String roleName, int pendingCount, boolean isDoubleSound) {
        String title = isDoubleSound
                ? "¡Alerta Crítica! Sábado 8:00 PM (" + pendingCount + " KPIs sin hacer)"
                : "¡Sábado 8:00 PM! Cierre Semanal para el Licenciado";
        String body = "Atención (" + roleName + "): Tienes " + pendingCount
                + " turnos exigibles sin ficha confirmada por el servidor. Abre Mis KPI para completarlos.";
        dispatchNotification(context, NOTIFICATION_ID_SATURDAY, title, body, isDoubleSound);
    }

    /**
     * 4. Notificación Diaria 8:00 PM (Lunes a Viernes).
     * Suena DOBLE TIMBRE si acumula más de 2 KPIs no hechos.
     */
    public static void showDaily8PmCheckNotification(Context context,
                                                     String roleName,
                                                     int pendingCount,
                                                     boolean isDoubleSound,
                                                     boolean isWeekSubmitted) {
        String title;
        String body;
        if (pendingCount > 2) {
            title = "⚠️ ¡Doble Alerta 8:00 PM! (" + pendingCount + " KPIs sin hacer)";
            body = "Hola (" + roleName + "), acumulas " + pendingCount + " turnos atrasados. ¡Complétalos hoy para no retrasar tu reporte semanal!";
        } else if (pendingCount > 0) {
            title = "¡Son las 8:00 PM! Tienes " + pendingCount + " turno(s) pendiente(s)";
            body = "Hola (" + roleName + "), aún tienes turnos del día sin registrar en tu hoja de producción.";
        } else if (!isWeekSubmitted) {
            title = "¡Recordatorio 8:00 PM! Falta enviar tu Reporte Final";
            body = "Tus avances diarios de " + roleName + " están registrados, pero falta enviar el consolidado.";
        } else {
            title = "¡KPIs al día a las 8:00 PM!";
            body = "Excelente trabajo en " + roleName + ". Todos tus KPIs están completos y verificados.";
        }

        dispatchNotification(context, NOTIFICATION_ID, title, body, isDoubleSound);
    }

    /**
     * Cuenta el número real de turnos o días pendientes sin registrar hasta el día de hoy.
     */
    public static int countPendingKpiShifts(Context context, String roleName) {
        if (context == null) return 0;
        int pending = 0;
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int todayIdx = 0;
        switch (dow) {
            case Calendar.MONDAY: todayIdx = 0; break;
            case Calendar.TUESDAY: todayIdx = 1; break;
            case Calendar.WEDNESDAY: todayIdx = 2; break;
            case Calendar.THURSDAY: todayIdx = 3; break;
            case Calendar.FRIDAY: todayIdx = 4; break;
            case Calendar.SATURDAY: todayIdx = 5; break;
            default: todayIdx = 5; break;
        }

        pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository attRepo =
                pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.getInstance(context);

        String upper = roleName != null ? roleName.toUpperCase(java.util.Locale.ROOT) : "";
        if (upper.contains("PROGRAM")) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("kpi_weekly_prog_prefs_v1", Context.MODE_PRIVATE);
            for (int i = 0; i <= todayIdx; i++) {
                pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(i);
                if (att != null) {
                    if (att.morningAttended && !prefs.getBoolean("day_" + i + "_shift_0_sent", prefs.getBoolean("day_" + i + "_sent", false))) {
                        pending++;
                    }
                    if (att.afternoonAttended && !prefs.getBoolean("day_" + i + "_shift_1_sent", false)) {
                        pending++;
                    }
                }
            }
        } else if (upper.contains("JUNIOR") || upper.contains("DISEN") || upper.contains("DISEÑO") || upper.contains("PRACTICANTE")) {
            // Evaluamos contra KpiJuniorMemoryRepository
            java.text.SimpleDateFormat dfShort = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US);
            Calendar tempCal = Calendar.getInstance();
            tempCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            for (int i = 0; i <= todayIdx; i++) {
                pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(i);
                String dStr = dfShort.format(tempCal.getTime());
                if (att != null) {
                    if (att.morningAttended) {
                        pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorDayRecord mr =
                                pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository.getInstance(context).getShiftRecord(dStr, "MANANA");
                        if (mr == null || !mr.isBiometricVerified()) {
                            pending++;
                        }
                    }
                    if (att.afternoonAttended) {
                        pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorDayRecord ar =
                                pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository.getInstance(context).getShiftRecord(dStr, "TARDE");
                        if (ar == null || !ar.isBiometricVerified()) {
                            pending++;
                        }
                    }
                }
                tempCal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else {
            List<String> missingDays = KpiDesignerMemoryRepository.getInstance(context)
                    .getMissingDaysUpToToday(roleName);
            pending = missingDays != null ? missingDays.size() : 0;
        }

        return pending;
    }

    private static void dispatchNotification(Context context, int notificationId,
                                             String title, String body, boolean isDoubleSound) {
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Si no hay permiso concedido en Android 13+, reproducir sonido directamente
                if (isDoubleSound) {
                    KpiSoundHelper.playDoubleReminderTone(context);
                } else {
                    KpiSoundHelper.playReminder8PmTone(context);
                }
                return;
            }
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri customToneUri = KpiSoundHelper.getReminder8PmSoundUri(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.kpi_ic_target)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(customToneUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }

        // Si es el caso de Sábado 8:00 PM (sonido doble), reproducir el segundo tono mediante MediaPlayer
        // para garantizar que suene doble en una sola notificación.
        if (isDoubleSound) {
            KpiSoundHelper.playDoubleReminderTone(context);
        }
    }

    private static boolean checkAnyShiftPending(Context context, String roleName) {
        List<String> missingDays = KpiDesignerMemoryRepository.getInstance(context)
                .getMissingDaysUpToToday(roleName);
        boolean isWeekSubmitted = KpiDesignerMemoryRepository.getInstance(context)
                .isCurrentWeekFinalSubmitted(roleName);
        return !missingDays.isEmpty() || !isWeekSubmitted;
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID);

            Uri customToneUri = KpiSoundHelper.getReminder8PmSoundUri(context);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios KPI (Mañana 2pm, Tarde 6:40pm, Noche 8pm)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisa automáticamente en los horarios clave de registro de KPI (2:00 PM, 6:40 PM y 8:00 PM) con tono personalizado.");
            channel.setSound(customToneUri, audioAttributes);
            manager.createNotificationChannel(channel);
        }
    }
}
