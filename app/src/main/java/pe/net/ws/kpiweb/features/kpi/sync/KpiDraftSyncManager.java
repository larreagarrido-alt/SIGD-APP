package pe.net.ws.kpiweb.features.kpi.sync;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Account-scoped outbox for private administrative drafts. Never publishes official KPI values. */
public final class KpiDraftSyncManager {
    private KpiDraftSyncManager() {}

    private static SharedPreferences prefs(Context app, int user) {
        return app.getSharedPreferences("kpi_admin_draft_outbox_user_" + user, Context.MODE_PRIVATE);
    }

    public static void enqueue(Context context, KpiDefinition draft) {
        Context app = context.getApplicationContext();
        SessionManager session = new SessionManager(app);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || draft == null || draft.getId() == null) return;
        try {
            JsonObject item = new JsonObject();
            item.addProperty("borrador_uid", draft.getId());
            item.addProperty("revision", Math.max(1, draft.getUpdatedAt()));
            item.add("payload", JsonParser.parseString(draft.toJson().toString()).getAsJsonObject());
            prefs(app, session.getUserId()).edit().putString("pending:" + draft.getId(), item.toString()).commit();
            retry(app);
        } catch (RuntimeException ignored) { /* Keep the local draft if serialization fails. */ }
    }

    public static void retry(Context context) {
        Context app = context.getApplicationContext();
        SessionManager session = new SessionManager(app);
        if (!session.isLoggedIn() || session.getUserId() <= 0) return;
        SharedPreferences outbox = prefs(app, session.getUserId());
        for (java.util.Map.Entry<String, ?> entry : outbox.getAll().entrySet()) {
            if (!entry.getKey().startsWith("pending:") || !(entry.getValue() instanceof String)) continue;
            String key = entry.getKey(), raw = (String) entry.getValue(), token = session.getAccessToken();
            int user = session.getUserId();
            JsonObject sent;
            try { sent = JsonParser.parseString(raw).getAsJsonObject(); }
            catch (RuntimeException malformed) { return; }
            ApiClient.getService().saveKpiDraft("Bearer " + token, sent).enqueue(new Callback<ApiResponse<JsonObject>>() {
                @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                    if (response.errorBody() != null) response.errorBody().close();
                    if (response.code() == 401) { new SessionManager(app).clearActiveSession(); return; }
                    JsonObject data = response.body() == null ? null : response.body().getData();
                    if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                            || data == null || !sameAccount(app, user, token)
                            || !data.has("id_usuario") || data.get("id_usuario").getAsInt() != user
                            || !data.has("borrador_uid") || !data.get("borrador_uid").equals(sent.get("borrador_uid"))
                            || !data.has("revision") || !data.get("revision").equals(sent.get("revision"))) return;
                    ApiClient.getService().getKpiDrafts("Bearer " + token).enqueue(new Callback<ApiResponse<JsonObject>>() {
                        @Override public void onResponse(Call<ApiResponse<JsonObject>> read, Response<ApiResponse<JsonObject>> result) {
                            if (result.errorBody() != null) result.errorBody().close();
                            JsonObject body = result.body() == null ? null : result.body().getData();
                            if (!result.isSuccessful() || result.body() == null || !result.body().isSuccessful()
                                    || !sameAccount(app, user, token) || !raw.equals(outbox.getString(key, null))
                                    || body == null || !body.has("id_usuario") || body.get("id_usuario").getAsInt() != user
                                    || !body.has("borradores") || !body.get("borradores").isJsonArray()) return;
                            for (JsonElement row : body.getAsJsonArray("borradores")) {
                                if (!row.isJsonObject()) continue;
                                JsonObject item = row.getAsJsonObject();
                                if (item.has("borrador_uid") && item.get("borrador_uid").equals(sent.get("borrador_uid"))
                                        && item.has("revision") && item.get("revision").equals(sent.get("revision"))
                                        && item.has("payload") && item.get("payload").equals(sent.get("payload"))) {
                                    outbox.edit().remove(key).commit();
                                    return;
                                }
                            }
                        }
                        @Override public void onFailure(Call<ApiResponse<JsonObject>> read, Throwable failure) {}
                    });
                }
                @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable failure) {}
            });
        }
    }

    public static void refresh(Context context, Runnable updated) {
        Context app = context.getApplicationContext();
        SessionManager session = new SessionManager(app);
        if (!session.isLoggedIn() || session.getUserId() <= 0) return;
        int user = session.getUserId(); String token = session.getAccessToken();
        ApiClient.getService().getKpiDrafts("Bearer " + token).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (response.code() == 401) { new SessionManager(app).clearActiveSession(); return; }
                JsonObject data = response.body() == null ? null : response.body().getData();
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || !sameAccount(app, user, token) || data == null || !data.has("id_usuario")
                        || data.get("id_usuario").getAsInt() != user || !data.has("borradores")
                        || !data.get("borradores").isJsonArray()) return;
                KpiCustomRepository repo = KpiCustomRepository.getInstance(app);
                for (JsonElement element : data.getAsJsonArray("borradores")) {
                    try {
                        JsonObject item = element.getAsJsonObject();
                        String uid = item.get("borrador_uid").getAsString();
                        if (prefs(app, user).contains("pending:" + uid)) continue;
                        KpiDefinition draft = KpiDefinition.fromJson(new JSONObject(item.getAsJsonObject("payload").toString()));
                        if (draft != null && uid.equals(draft.getId())) repo.importServerDraft(draft);
                    } catch (Exception ignored) { /* Skip invalid server draft without changing local data. */ }
                }
                if (updated != null) updated.run();
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable failure) {}
        });
    }

    private static boolean sameAccount(Context app, int user, String token) {
        SessionManager current = new SessionManager(app);
        return current.isLoggedIn() && current.getUserId() == user && token.equals(current.getAccessToken());
    }
}
