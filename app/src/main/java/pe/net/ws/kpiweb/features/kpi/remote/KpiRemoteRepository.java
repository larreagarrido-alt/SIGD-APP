package pe.net.ws.kpiweb.features.kpi.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.core.time.IsoWeekPeriod;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.engine.KpiMeasurementType;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Puente API -> lógica KPI existente. La API es la fuente de asignaciones y metas;
 * SharedPreferences conserva el último snapshot únicamente como respaldo sin conexión.
 */
public final class KpiRemoteRepository {
    private static final String PREFS = "kpi_server_cache";
    private static final String KEY_ASSIGNMENTS = "current_assignments";

    public interface RefreshCallback {
        void onLoaded(int count);
        void onUnavailable(String message, boolean hasCachedData);
    }

    private final Context appContext;
    private final SessionManager session;
    private final SharedPreferences cache;
    private final int ownerUserId;
    private final int ownerWorkerId;
    private final Gson gson = new Gson();

    public KpiRemoteRepository(Context context) {
        appContext = context.getApplicationContext();
        session = new SessionManager(appContext);
        ownerUserId = session.getUserId();
        ownerWorkerId = session.getWorkerId();
        cache = appContext.getSharedPreferences(PREFS + "_user_" + ownerUserId, Context.MODE_PRIVATE);
    }

    public void refreshCurrentPeriod(LoginResponseData profile, RefreshCallback callback) {
        if (!sameOwner() || !session.isLoggedIn()) {
            callback.onUnavailable("La sesión cambió. Vuelve a iniciar sesión.", false);
            return;
        }
        String period = IsoWeekPeriod.currentInLima();
        final String token = session.getAccessToken();
        ApiClient.getService().getMisKpis(session.getBearerToken(), period)
                .enqueue(new Callback<ApiResponse<KpiServerAssignments>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<KpiServerAssignments>> call,
                                           Response<ApiResponse<KpiServerAssignments>> response) {
                        if (!sameOwner() || !token.equals(session.getAccessToken())) return;
                        if (response.code() == 401) {
                            session.clearActiveSession();
                            callback.onUnavailable("La sesión venció. Vuelve a iniciar sesión.", false);
                            return;
                        }
                        if (response.code() == 403) {
                            cache.edit().remove(KEY_ASSIGNMENTS).apply();
                            callback.onUnavailable("Esta cuenta no tiene permiso para consultar KPI.", false);
                            return;
                        }
                        ApiResponse<KpiServerAssignments> envelope = response.body();
                        if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                                || envelope.getData() == null) {
                            unavailable(callback, envelope != null ? envelope.getMessage() : "No se pudieron consultar los KPI");
                            return;
                        }
                        KpiServerAssignments data = envelope.getData();
                        if (data.getWorkerId() != session.getWorkerId()) {
                            unavailable(callback, "La respuesta no corresponde al trabajador autenticado");
                            return;
                        }
                        cache.edit().putString(KEY_ASSIGNMENTS, gson.toJson(data)).apply();
                        importDefinitions(profile, data.getAssignments());
                        callback.onLoaded(data.getAssignments().size());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<KpiServerAssignments>> call, Throwable error) {
                        if (!sameOwner() || !token.equals(session.getAccessToken())) return;
                        unavailable(callback, "Sin conexión con WebSite+");
                    }
                });
    }

    public KpiServerAssignments getCachedAssignments() {
        if (!sameOwner() || !session.isLoggedIn()) return null;
        String json = cache.getString(KEY_ASSIGNMENTS, null);
        if (json == null || json.trim().isEmpty()) return null;
        try {
            KpiServerAssignments value = gson.fromJson(json, KpiServerAssignments.class);
            return value != null && value.getWorkerId() == session.getWorkerId() ? value : null;
        }
        catch (RuntimeException ignored) { return null; }
    }

    private boolean sameOwner() {
        return ownerUserId > 0 && ownerWorkerId > 0 && ownerUserId == session.getUserId()
                && ownerWorkerId == session.getWorkerId();
    }

    private void unavailable(RefreshCallback callback, String message) {
        callback.onUnavailable(message, getCachedAssignments() != null);
    }

    private void importDefinitions(LoginResponseData profile, List<KpiServerAssignment> assignments) {
        String role = profile != null && profile.getWorker() != null
                ? safe(profile.getWorker().getCargoNombre(), profile.getUser() != null ? profile.getUser().getRolNombre() : "")
                : "";
        KpiCustomRepository repository = KpiCustomRepository.getInstance(appContext);
        for (KpiServerAssignment assignment : assignments) {
            String code = safe(assignment.getKpiCode(), "DB-KPI-" + assignment.getKpiId());
            KpiDefinition definition = new KpiDefinition(
                    code,
                    safe(assignment.getName(), code),
                    safe(assignment.getCategory(), "KPI WebSite+"),
                    role,
                    measurementType(assignment.getMetricType()),
                    safe(assignment.getUnit(), "unidades"),
                    assignment.getTarget(),
                    normalizeWeight(assignment.getWeight()),
                    KpiPolarity.MAYOR_ES_MEJOR);
            definition.setDescription(safe(assignment.getDescription(), "Asignación obtenida desde WebSite+"));
            definition.setFrequency(safe(assignment.getPeriodType(), "SEMANAL"));
            definition.setInformationSource("Base de datos WebSite+ · " + safe(assignment.getPeriodKey(), "periodo actual"));
            definition.setActive(!isTerminal(assignment.getStatus()));
            repository.saveOrUpdateKpi(definition);
        }
    }

    private static KpiMeasurementType measurementType(String value) {
        if (value == null) return KpiMeasurementType.CANTIDAD;
        switch (value.toUpperCase(Locale.ROOT)) {
            case "PORCENTAJE": return KpiMeasurementType.PORCENTAJE;
            case "ESCALA_1_5": return KpiMeasurementType.CALIFICACION;
            case "BOOLEANO": return KpiMeasurementType.CUMPLIMIENTO_ACTIVIDADES;
            default: return KpiMeasurementType.CANTIDAD;
        }
    }

    private static double normalizeWeight(double value) {
        return value > 0 && value <= 1 ? value * 100.0 : value;
    }

    private static boolean isTerminal(String status) {
        if (status == null) return false;
        return status.equalsIgnoreCase("Cancelado") || status.equalsIgnoreCase("No exigible");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
