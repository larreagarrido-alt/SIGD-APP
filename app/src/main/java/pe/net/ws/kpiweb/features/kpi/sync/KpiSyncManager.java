package pe.net.ws.kpiweb.features.kpi.sync;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Sends local KPI records to the API; only an exact receipt changes PENDING to SYNCED. */
public final class KpiSyncManager {
    public interface SyncCallback {
        void onSyncSuccess(int syncedCount);
        void onSyncError(String errorMessage);
    }

    private static volatile KpiSyncManager instance;
    private final Context appContext;
    private Call<ApiResponse<KpiSyncReceipt>> pendingRequest;

    private KpiSyncManager(Context context) { appContext = context.getApplicationContext(); }

    public static synchronized KpiSyncManager getInstance(Context context) {
        if (instance == null) instance = new KpiSyncManager(context);
        return instance;
    }

    public List<KpiRecordEntry> getPendingSyncEntries() {
        List<KpiRecordEntry> pending = new ArrayList<>();
        for (KpiRecordEntry entry : KpiCustomRepository.getInstance(appContext).getAllRecordEntries()) {
            if ("PENDING".equalsIgnoreCase(entry.getSyncStatus())) pending.add(entry);
        }
        return pending;
    }

    public int getPendingSyncCount() { return getPendingSyncEntries().size(); }

    /** Diagnostic snapshot; sending always uses a fresh snapshot from the active account. */
    public JSONObject buildBatchSyncPayload() {
        JSONObject body = new JSONObject();
        JSONArray records = new JSONArray();
        for (KpiRecordEntry entry : getPendingSyncEntries()) records.put(entry.toJson());
        try { body.put("records", records); } catch (org.json.JSONException ignored) { }
        return body;
    }

    public synchronized void syncPendingRecords(SyncCallback callback) {
        if (pendingRequest != null) {
            error(callback, "Ya se está sincronizando. Espera el resultado antes de reintentar.");
            return;
        }
        SessionManager session = new SessionManager(appContext);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) {
            error(callback, "Inicia sesión con una cuenta de trabajador para sincronizar.");
            return;
        }
        List<KpiRecordEntry> pending = getPendingSyncEntries();
        if (pending.isEmpty()) {
            if (callback != null) callback.onSyncSuccess(0);
            return;
        }
        int workerId = session.getWorkerId();
        String token = session.getAccessToken();
        Map<String, String> snapshots = new LinkedHashMap<>();
        JsonArray records = new JsonArray();
        // The local API accepts at most 50 records per transaction. Further records remain PENDING.
        for (KpiRecordEntry entry : pending.subList(0, Math.min(50, pending.size()))) {
            String id = entry.getRecordId();
            if (id == null || id.isEmpty() || snapshots.containsKey(id)
                    || !String.valueOf(workerId).equals(entry.getEmployeeId())) {
                error(callback, "Hay registros locales sin identidad o ID válido. No se enviaron.");
                return;
            }
            String json = entry.toJson().toString();
            snapshots.put(id, json);
            records.add(JsonParser.parseString(json));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("records", records);
        Call<ApiResponse<KpiSyncReceipt>> request =
                ApiClient.getService().syncKpiRecords(session.getBearerToken(), body);
        pendingRequest = request;
        request.enqueue(new Callback<ApiResponse<KpiSyncReceipt>>() {
            @Override public void onResponse(Call<ApiResponse<KpiSyncReceipt>> call,
                                             Response<ApiResponse<KpiSyncReceipt>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                synchronized (KpiSyncManager.this) {
                    if (pendingRequest != call) return;
                    pendingRequest = null;
                }
                if (!token.equals(new SessionManager(appContext).getAccessToken())) {
                    error(callback, "La sesión cambió durante la sincronización. Reintenta con la cuenta actual.");
                    return;
                }
                if (response.code() == 401) {
                    session.clearActiveSession();
                    error(callback, "La sesión venció. Vuelve a iniciar sesión.");
                    return;
                }
                if (response.code() == 403) {
                    error(callback, "Esta cuenta no tiene permiso para enviar registros KPI.");
                    return;
                }
                if (response.code() == 404) {
                    error(callback, "La API de sincronización aún no está disponible. Los registros siguen pendientes.");
                    return;
                }
                if (response.code() == 409 || response.code() == 422) {
                    error(callback, "El servidor rechazó un registro o su asignación KPI. Se conservan pendientes.");
                    return;
                }
                ApiResponse<KpiSyncReceipt> envelope = response.body();
                KpiSyncReceipt receipt = envelope == null ? null : envelope.getData();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || receipt == null || !exactReceipt(receipt, workerId, snapshots.keySet())) {
                    error(callback, "El servidor no confirmó todos los registros. Se conservan pendientes.");
                    return;
                }
                int synced = KpiCustomRepository.getInstance(appContext).markConfirmedEntries(snapshots);
                if (callback != null) callback.onSyncSuccess(synced);
            }
            @Override public void onFailure(Call<ApiResponse<KpiSyncReceipt>> call, Throwable failure) {
                synchronized (KpiSyncManager.this) {
                    if (pendingRequest != call) return;
                    pendingRequest = null;
                }
                error(callback, "Sin conexión con el servidor. Los registros siguen pendientes.");
            }
        });
    }

    static boolean exactReceipt(KpiSyncReceipt receipt, int workerId, Set<String> sentIds) {
        if (receipt.getWorkerId() != workerId || receipt.getAcceptedIds().size() != sentIds.size()) return false;
        Set<String> accepted = new HashSet<>(receipt.getAcceptedIds());
        return accepted.size() == sentIds.size() && accepted.equals(sentIds);
    }

    private static void error(SyncCallback callback, String message) {
        if (callback != null) callback.onSyncError(message);
    }
}
