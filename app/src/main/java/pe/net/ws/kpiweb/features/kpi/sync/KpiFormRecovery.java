package pe.net.ws.kpiweb.features.kpi.sync;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Reads every page of authorized reports and restores the original repositories. */
public final class KpiFormRecovery {
    public interface Completion { void finished(boolean success, int restored); }

    private KpiFormRecovery() {}

    public static void refresh(Context context, Completion completion) {
        Context app = context.getApplicationContext();
        SessionManager session = new SessionManager(app);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0) {
            if (completion != null) completion.finished(false, 0);
            return;
        }
        fetch(app, session.getUserId(), session.getWorkerId(), session.getAccessToken(), 0, 0, completion);
    }

    private static void fetch(Context app, int user, int worker, String token, long after,
                              int restored, Completion completion) {
        ApiClient.getService().getKpiFormsPage("Bearer " + token, "", after, 100)
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        SessionManager now = new SessionManager(app);
                        if (!now.isLoggedIn() || now.getUserId() != user || now.getWorkerId() != worker
                                || !token.equals(now.getAccessToken())) { finish(false, restored, completion); return; }
                        if (response.code() == 401) {
                            now.clearActiveSession();
                            finish(false, restored, completion);
                            return;
                        }
                        ApiResponse<JsonObject> envelope = response.body();
                        JsonObject data = envelope == null ? null : envelope.getData();
                        if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                                || data == null || !data.has("id_trabajador")
                                || data.get("id_trabajador").getAsInt() != worker
                                || !data.has("registros") || !data.get("registros").isJsonArray()) {
                            finish(false, restored, completion); return;
                        }
                        JsonArray rows = data.getAsJsonArray("registros");
                        int count = restored;
                        for (JsonElement element : rows) {
                            try {
                                if (element.isJsonObject() && restore(app, element.getAsJsonObject())) count++;
                            } catch (RuntimeException ignored) { /* One invalid row must not erase valid local work. */ }
                        }
                        long next = data.has("next_after_id") ? data.get("next_after_id").getAsLong() : after;
                        boolean more = data.has("has_more") && data.get("has_more").getAsBoolean();
                        if (more && rows.size() > 0 && next > after) fetch(app, user, worker, token, next, count, completion);
                        else finish(!more, count, completion);
                    }
                    @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable failure) {
                        finish(false, restored, completion);
                    }
                });
    }

    private static boolean restore(Context app, JsonObject row) {
        if (!row.has("tipo_formulario") || !row.has("registro_uid") || !row.has("payload")
                || !row.get("payload").isJsonObject()) return false;
        String type = row.get("tipo_formulario").getAsString();
        String uid = row.get("registro_uid").getAsString();
        int cargo = row.has("id_cargo") ? row.get("id_cargo").getAsInt() : 0;
        if (cargo > 0) KpiFormSyncManager.rememberSourceCargo(app, type, uid, cargo);
        if (cargo != new SessionManager(app).getCargoId()) return false;
        if (KpiFormSyncManager.hasPending(app, type, uid)) return false;
        if (!row.has("revision")) return false;
        long revision = row.get("revision").getAsLong();
        long known = KpiFormSyncManager.confirmedRevision(app, type, uid);
        if (known >= revision) return false;
        boolean allowReplace = known > 0;
        JsonObject payload = row.getAsJsonObject("payload");
        boolean applied;
        switch (type) {
            case "VENTAS": applied = pe.net.ws.kpiweb.features.kpi.data.KpiSalesWeekManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "DISENADOR": applied = pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "DISENADOR_JUNIOR": applied = pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "DISENADOR_SENIOR": applied = pe.net.ws.kpiweb.features.kpi.senior.data.KpiSeniorStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "GERENCIA": applied = pe.net.ws.kpiweb.features.kpi.manager.data.KpiGeneralManagerStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "PROGRAMACION": applied = pe.net.ws.kpiweb.features.kpi.data.KpiProgrammerRepository.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "ADMINISTRACION": applied = pe.net.ws.kpiweb.features.kpi.administrator.data.KpiAdministratorStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "ASISTENCIA_ADMIN": applied = pe.net.ws.kpiweb.features.kpi.assistant.data.KpiAdminAssistantStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "CONTABILIDAD": applied = pe.net.ws.kpiweb.features.kpi.accountant.data.KpiAccountantStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "COMMUNITY": applied = pe.net.ws.kpiweb.features.kpi.community.data.KpiCommunityStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "FOTOGRAFIA": applied = pe.net.ws.kpiweb.features.kpi.photographer.data.KpiPhotographerStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "TRAFFICKER": applied = pe.net.ws.kpiweb.features.kpi.trafficker.data.KpiTraffickerStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            case "VIDEO": applied = pe.net.ws.kpiweb.features.kpi.videographer.data.KpiVideographerStorageManager.getInstance(app).restoreFromServer(payload, allowReplace); break;
            default: return false;
        }
        if (applied) KpiFormSyncManager.rememberConfirmed(app, type, uid, revision);
        return applied;
    }

    private static void finish(boolean success, int restored, Completion completion) {
        if (completion != null) completion.finished(success, restored);
    }
}
