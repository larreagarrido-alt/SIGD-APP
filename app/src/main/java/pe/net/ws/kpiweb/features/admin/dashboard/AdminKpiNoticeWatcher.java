package pe.net.ws.kpiweb.features.admin.dashboard;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Shows new server notices while the administrator dashboard is active. */
public final class AdminKpiNoticeWatcher {
    private static final String CHANNEL = "sigd_admin_kpi_submissions";
    private static final long INTERVAL_MS = 30_000L;
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable repeat = this::poll;
    private Call<ApiResponse<JsonObject>> request;
    private boolean running;

    public AdminKpiNoticeWatcher(Context context) { this.context = context.getApplicationContext(); }

    public void start() {
        if (running) return;
        running = true;
        poll();
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(repeat);
        if (request != null) request.cancel();
        request = null;
    }

    private void next() {
        if (running) handler.postDelayed(repeat, INTERVAL_MS);
    }

    private void poll() {
        if (!running) return;
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0 || session.getUserId() <= 0) {
            next();
            return;
        }
        final int userId = session.getUserId();
        final String token = session.getAccessToken();
        request = ApiClient.getService().getOwnNotifications(session.getBearerToken());
        request.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call,
                                             Response<ApiResponse<JsonObject>> response) {
                if (!current(call, userId, token)) return;
                ApiResponse<JsonObject> envelope = response.body();
                JsonObject data = envelope == null ? null : envelope.getData();
                if (response.isSuccessful() && envelope != null && envelope.isSuccessful()
                        && data != null && data.has("id_trabajador")
                        && data.get("id_trabajador").getAsInt() == new SessionManager(context).getWorkerId()
                        && data.has("notificaciones") && data.get("notificaciones").isJsonArray()) {
                    consume(userId, data.getAsJsonArray("notificaciones"));
                }
                next();
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (current(call, userId, token)) next();
            }
        });
    }

    private boolean current(Call<?> call, int userId, String token) {
        SessionManager session = new SessionManager(context);
        return running && !call.isCanceled() && session.getUserId() == userId
                && token.equals(session.getAccessToken());
    }

    private void consume(int userId, JsonArray rows) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("admin_notice_cursor", Context.MODE_PRIVATE);
        String key = "user_" + userId;
        long previous = prefs.getLong(key, -1L);
        long newest = previous;
        int newKpi = 0;
        java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT);
        dayFormat.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
        String today = dayFormat.format(new java.util.Date());
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            if (!row.has("id_notificacion") || row.get("id_notificacion").isJsonNull()) continue;
            long id = row.get("id_notificacion").getAsLong();
            newest = Math.max(newest, id);
            boolean newNotice = previous >= 0 ? id > previous
                    : row.has("creado_en") && !row.get("creado_en").isJsonNull()
                    && row.get("creado_en").getAsString().startsWith(today)
                    && row.has("leida") && !row.get("leida").isJsonNull()
                    && row.get("leida").getAsInt() == 0;
            if (newNotice && row.has("referencia_tipo")
                    && !row.get("referencia_tipo").isJsonNull()
                    && "kpi_formulario".equals(row.get("referencia_tipo").getAsString())) newKpi++;
        }
        if (newKpi > 0 && !canNotify()) return;
        if (newest > previous) prefs.edit().putLong(key, newest).apply();
        if (newKpi > 0) notifyKpi(newKpi);
    }

    private boolean canNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private void notifyKpi(int count) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "KPI recibidos",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            NotificationManager system = context.getSystemService(NotificationManager.class);
            if (system != null) system.createNotificationChannel(channel);
        }
        Class<?> destination = new SessionManager(context).getRoleId() == 1
                ? AdminNotificationsActivity.class : CollaboratorNotificationsActivity.class;
        Intent target = new Intent(context, destination);
        PendingIntent pending = PendingIntent.getActivity(context, 2026, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String message = count == 1 ? "Se recibió un reporte KPI para revisión."
                : "Se recibieron " + count + " reportes KPI para revisión.";
        NotificationCompat.Builder notice = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.collab_ic_nav_bell)
                .setContentTitle("Nuevo KPI recibido")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setContentIntent(pending)
                .setAutoCancel(true);
        try { manager.notify(2026, notice.build()); } catch (SecurityException ignored) { }
    }
}
