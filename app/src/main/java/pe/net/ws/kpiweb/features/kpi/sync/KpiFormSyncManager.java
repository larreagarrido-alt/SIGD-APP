package pe.net.ws.kpiweb.features.kpi.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Durable account-scoped outbox for complete KPI form snapshots. */
public final class KpiFormSyncManager {
    private static final Gson GSON = new Gson();
    private static KpiFormSyncManager instance;
    private final Context app;
    private final SharedPreferences prefs;
    private final int userId;
    private final int workerId;
    private boolean busy;

    private KpiFormSyncManager(Context context, SessionManager session) {
        app = context.getApplicationContext();
        userId = session.getUserId();
        workerId = session.getWorkerId();
        prefs = app.getSharedPreferences(outboxName(userId, workerId), Context.MODE_PRIVATE);
    }

    private static synchronized KpiFormSyncManager forSession(Context context, SessionManager session) {
        if (instance == null || instance.userId != session.getUserId() || instance.workerId != session.getWorkerId())
            instance = new KpiFormSyncManager(context, session);
        return instance;
    }

    public static void enqueue(Context context, String type, String localKey, Object report) {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0
                || localKey == null || localKey.isEmpty() || report == null) return;
        forSession(context, session).save(type, localKey, report);
    }

    public static void retry(Context context) {
        SessionManager session = new SessionManager(context);
        if (session.isLoggedIn() && session.getUserId() > 0 && session.getWorkerId() > 0)
            forSession(context, session).sendNext();
    }

    public static boolean hasPending(Context context, String type, String uid) {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0) return false;
        SharedPreferences outbox = context.getApplicationContext().getSharedPreferences(
                outboxName(session.getUserId(), session.getWorkerId()), Context.MODE_PRIVATE);
        return outbox.contains("pending:" + type + ":" + uid);
    }

    public static long confirmedRevision(Context context, String type, String uid) {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0) return 0;
        return context.getApplicationContext().getSharedPreferences(
                outboxName(session.getUserId(), session.getWorkerId()), Context.MODE_PRIVATE)
                .getLong("confirmed:" + type + ":" + uid, 0);
    }

    public static void rememberConfirmed(Context context, String type, String uid, long revision) {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0) return;
        SharedPreferences outbox = context.getApplicationContext().getSharedPreferences(
                outboxName(session.getUserId(), session.getWorkerId()), Context.MODE_PRIVATE);
        String key = "confirmed:" + type + ":" + uid;
        String nextKey = "revision:" + type + ":" + uid;
        if (revision > outbox.getLong(key, 0) || revision > outbox.getLong(nextKey, 0))
            outbox.edit().putLong(key, Math.max(revision, outbox.getLong(key, 0)))
                    .putLong(nextKey, Math.max(revision, outbox.getLong(nextKey, 0))).commit();
    }

    public static void rememberSourceCargo(Context context, String type, String uid, int cargoId) {
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0
                || cargoId <= 0) return;
        context.getApplicationContext().getSharedPreferences(
                outboxName(session.getUserId(), session.getWorkerId()), Context.MODE_PRIVATE)
                .edit().putInt("source_cargo:" + type + ":" + uid, cargoId).apply();
    }

    private static String outboxName(int userId, int workerId) {
        return "kpi_form_outbox_user_" + userId + "_worker_" + workerId;
    }

    private void save(String type, String localKey, Object report) {
        JsonElement payload = GSON.toJsonTree(report);
        if (!payload.isJsonObject()) return;
        int cargoId = new SessionManager(app).getCargoId();
        String legacyUid = UUID.nameUUIDFromBytes((type + ":" + localKey).getBytes(StandardCharsets.UTF_8)).toString();
        String uid = cargoId > 0 && prefs.getInt("source_cargo:" + type + ":" + legacyUid, 0) != cargoId
                ? UUID.nameUUIDFromBytes((type + ":cargo_" + cargoId + ":" + localKey)
                    .getBytes(StandardCharsets.UTF_8)).toString()
                : legacyUid;
        String key = type + ":" + uid;
        long revision = Math.max(System.currentTimeMillis(), prefs.getLong("revision:" + key, 0L) + 1L);
        JsonObject record = new JsonObject();
        record.addProperty("tipo_formulario", type);
        record.addProperty("registro_uid", uid);
        record.addProperty("revision", revision);
        record.add("payload", payload);
        // Commit the outbox before any network attempt; never discard it on a failed call.
        if (prefs.edit().putLong("revision:" + key, revision)
                .remove("blocked:" + key)
                .putString("pending:" + key, record.toString()).commit()) sendNext();
    }

    private synchronized void sendNext() {
        if (busy) return;
        String key = null, raw = null;
        for (java.util.Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("pending:") && entry.getValue() instanceof String
                    && !prefs.getBoolean("blocked:" + entry.getKey().substring("pending:".length()), false)) {
                key = entry.getKey(); raw = (String) entry.getValue(); break;
            }
        }
        if (key == null) return;
        SessionManager session = new SessionManager(app);
        if (!session.isLoggedIn() || session.getUserId() != userId || session.getWorkerId() != workerId) return;
        final String pendingKey = key, snapshot = raw, token = session.getAccessToken();
        final JsonObject record;
        try { record = JsonParser.parseString(snapshot).getAsJsonObject(); }
        catch (RuntimeException invalid) { return; }
        JsonArray records = new JsonArray(); records.add(record);
        JsonObject batch = new JsonObject(); batch.add("records", records);
        busy = true;
        ApiClient.getService().getAllowedKpiForms(session.getBearerToken())
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                        if (!new SessionManager(app).getAccessToken().equals(token)) { done(false); return; }
                        ApiResponse<JsonObject> envelope = response.body();
                        JsonObject data = envelope == null ? null : envelope.getData();
                        if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                                || data == null || !data.has("id_trabajador")
                                || data.get("id_trabajador").getAsInt() != workerId
                                || !data.has("tipos_formulario") || !data.get("tipos_formulario").isJsonArray()) {
                            if (response.errorBody() != null) response.errorBody().close();
                            notifyIfCurrent(token, "No se pudo comprobar el cargo; el reporte sigue en este teléfono.");
                            done(false); return;
                        }
                        String type = record.get("tipo_formulario").getAsString();
                        boolean allowed = false;
                        for (JsonElement item : data.getAsJsonArray("tipos_formulario")) {
                            if (item.isJsonPrimitive() && type.equals(item.getAsString())) { allowed = true; break; }
                        }
                        if (!allowed) {
                            prefs.edit().putBoolean("blocked:" + pendingKey.substring("pending:".length()), true).commit();
                            notifyIfCurrent(token, "Esta ficha no corresponde al cargo de tu cuenta. Quedó guardada en el teléfono; abre Mis KPI para usar tu formulario autorizado.");
                            done(true); return;
                        }
                        postSnapshot(pendingKey, snapshot, token, record, batch);
                    }
                    @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                        notifyIfCurrent(token, "Sin conexión para comprobar el cargo. El reporte sigue en el teléfono.");
                        done(false);
                    }
                });
    }

    private void postSnapshot(String pendingKey, String snapshot, String token, JsonObject record, JsonObject batch) {
        ApiClient.getService().syncKpiForms("Bearer " + token, batch)
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                        String errorCode = readErrorCode(response);
                        JsonObject data = response.body() == null ? null : response.body().getData();
                        if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                                || data == null || !matchesReceipt(data, record, workerId)) {
                            if ("FORM_NOT_ASSIGNED".equals(errorCode) || "CARGO_CHANGED".equals(errorCode)) {
                                prefs.edit().putBoolean("blocked:" + pendingKey.substring("pending:".length()), true).commit();
                                notifyIfCurrent(token, "El formulario no corresponde al cargo de tu cuenta. El registro quedó en el teléfono.");
                                done(true); return;
                            }
                            notifyIfCurrent(token, response.code() == 401
                                    ? "Sesión vencida; el reporte sigue guardado en este teléfono."
                                    : "El servidor no confirmó el reporte (HTTP " + response.code()
                                    + (errorCode.isEmpty() ? "" : ", " + errorCode) + "). Sigue pendiente en este teléfono.");
                            done(false); return;
                        }
                        ApiClient.getService().getKpiForm("Bearer " + token,
                                record.get("tipo_formulario").getAsString(), record.get("registro_uid").getAsString())
                                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                                    @Override public void onResponse(Call<ApiResponse<JsonObject>> read, Response<ApiResponse<JsonObject>> result) {
                                        if (result.errorBody() != null) result.errorBody().close();
                                        SessionManager current = new SessionManager(app);
                                        JsonObject body = result.body() == null ? null : result.body().getData();
                                        boolean verified = result.isSuccessful() && result.body() != null && result.body().isSuccessful()
                                                && current.getUserId() == userId && current.getWorkerId() == workerId
                                                && token.equals(current.getAccessToken()) && matchesRead(body, record, workerId)
                                                && snapshot.equals(prefs.getString(pendingKey, null));
                                        if (verified) {
                                            prefs.edit().remove(pendingKey)
                                                    .putLong("confirmed:" + pendingKey.substring("pending:".length()),
                                                            record.get("revision").getAsLong()).commit();
                                        }
                                        notifyIfCurrent(token, verified
                                                ? "Reporte KPI confirmado y comprobado en el servidor."
                                                : "El reporte sigue pendiente: no se pudo confirmar su lectura en el servidor.");
                                        done(verified);
                                    }
                                    @Override public void onFailure(Call<ApiResponse<JsonObject>> read, Throwable failure) {
                                        notifyIfCurrent(token, "El reporte sigue pendiente: falta comprobar su lectura en el servidor.");
                                        done(false);
                                    }
                                });
                    }
                    @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable failure) {
                        notifyIfCurrent(token, "Sin conexión: el reporte sigue guardado en este teléfono.");
                        done(false);
                    }
                });
    }

    private void notifyIfCurrent(String token, String message) {
        SessionManager current = new SessionManager(app);
        if (current.isLoggedIn() && current.getUserId() == userId && current.getWorkerId() == workerId
                && token.equals(current.getAccessToken()))
            Toast.makeText(app, message, Toast.LENGTH_LONG).show();
    }

    private static String readErrorCode(Response<?> response) {
        if (response.errorBody() == null) return "";
        try {
            JsonObject body = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
            return body.has("error_code") && body.get("error_code").isJsonPrimitive()
                    ? body.get("error_code").getAsString() : "";
        } catch (Exception ignored) {
            return "";
        } finally {
            response.errorBody().close();
        }
    }

    private synchronized void done(boolean confirmed) { busy = false; if (confirmed) sendNext(); }

    static boolean matchesReceipt(JsonObject data, JsonObject sent, int workerId) {
        if (!data.has("id_trabajador") || data.get("id_trabajador").getAsInt() != workerId
                || !data.has("accepted") || !data.get("accepted").isJsonArray()
                || data.getAsJsonArray("accepted").size() != 1) return false;
        JsonObject item = data.getAsJsonArray("accepted").get(0).getAsJsonObject();
        return sameIdentity(item, sent);
    }

    static boolean matchesRead(JsonObject data, JsonObject sent, int workerId) {
        if (data == null || !data.has("id_trabajador") || data.get("id_trabajador").getAsInt() != workerId
                || !data.has("registros") || !data.get("registros").isJsonArray()) return false;
        for (JsonElement item : data.getAsJsonArray("registros")) {
            if (item.isJsonObject() && sameIdentity(item.getAsJsonObject(), sent)
                    && item.getAsJsonObject().has("payload")
                    && item.getAsJsonObject().get("payload").equals(sent.get("payload"))) return true;
        }
        return false;
    }

    private static boolean sameIdentity(JsonObject actual, JsonObject sent) {
        return actual.has("tipo_formulario") && actual.has("registro_uid") && actual.has("revision")
                && actual.get("tipo_formulario").equals(sent.get("tipo_formulario"))
                && actual.get("registro_uid").equals(sent.get("registro_uid"))
                && actual.get("revision").equals(sent.get("revision"));
    }
}
