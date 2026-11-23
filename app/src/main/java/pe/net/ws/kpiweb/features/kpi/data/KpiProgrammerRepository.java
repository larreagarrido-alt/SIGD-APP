package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.model.KpiActivityItem;
import pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport;

/**
 * Repositorio persistente local para los reportes diarios del Programador.
 * Utiliza SharedPreferences con serialización JSON privada para preservar
 * las incidencias y avances tras cerrar o reiniciar la aplicación.
 */
public class KpiProgrammerRepository {

    private static final String PREF_NAME = "kpi_programmer_storage_v1";
    private static final String KEY_REPORTS = "key_programmer_reports_json";
    private static final String KEY_FULL_REPORTS = "key_programmer_reports_full_json";

    private static volatile KpiProgrammerRepository instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Map<String, KpiDailyReport> reportsMap = new LinkedHashMap<>();

    private KpiProgrammerRepository(Context context) {
        this.appContext = context.getApplicationContext();
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        this.prefs = appContext.getSharedPreferences(PREF_NAME + "_user_" + userId, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized KpiProgrammerRepository getInstance(Context context) {
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiProgrammerRepository(context);
            instanceUserId = userId;
        }
        return instance;
    }

    public synchronized void saveReport(KpiDailyReport report) {
        if (report == null) return;
        String key = (report.getFecha() != null && !report.getFecha().isEmpty()) ? report.getFecha() : ("REPORT-" + System.currentTimeMillis());
        reportsMap.put(key, report);
        saveToPrefs();
        pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(appContext, "PROGRAMACION", key, report);
    }

    public synchronized KpiDailyReport getReportByDate(String date) {
        if (date == null) return null;
        return reportsMap.get(date);
    }

    public synchronized List<KpiDailyReport> getAllReports() {
        return new ArrayList<>(reportsMap.values());
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiDailyReport report = new com.google.gson.Gson().fromJson(payload, KpiDailyReport.class);
        if (report == null || report.getFecha() == null || report.getFecha().isEmpty()) return false;
        if (reportsMap.containsKey(report.getFecha()) && !allowReplace) return false;
        reportsMap.put(report.getFecha(), report);
        saveToPrefs();
        return true;
    }

    private synchronized void saveToPrefs() {
        try {
            JSONArray array = new JSONArray();
            for (KpiDailyReport r : reportsMap.values()) {
                array.put(reportToJson(r));
            }
            // Keep the legacy representation for compatibility; the full snapshot preserves
            // every model field and nested list through process death.
            prefs.edit().putString(KEY_REPORTS, array.toString())
                    .putString(KEY_FULL_REPORTS, new com.google.gson.Gson().toJson(reportsMap)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        String fullJson = prefs.getString(KEY_FULL_REPORTS, null);
        if (fullJson != null) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KpiDailyReport>>() {}.getType();
                Map<String, KpiDailyReport> full = new com.google.gson.Gson().fromJson(fullJson, type);
                if (full != null) { reportsMap.clear(); reportsMap.putAll(full); return; }
            } catch (RuntimeException ignored) { /* Preserve the older readable representation. */ }
        }
        String jsonStr = prefs.getString(KEY_REPORTS, null);
        if (jsonStr == null) return;
        try {
            JSONArray array = new JSONArray(jsonStr);
            reportsMap.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null) {
                    KpiDailyReport r = jsonToReport(obj);
                    if (r != null && r.getFecha() != null) {
                        reportsMap.put(r.getFecha(), r);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject reportToJson(KpiDailyReport r) {
        JSONObject json = new JSONObject();
        try {
            json.put("id", r.getId());
            json.put("fecha", r.getFecha());
            json.put("practicante", r.getPracticante());
            json.put("supervisor", r.getSupervisor());
            json.put("turno", r.getTurno());
            json.put("rol", r.getRol());
            json.put("tareasAsignadas", r.getTareasAsignadas());
            json.put("tareasCompletadas", r.getTareasCompletadas());
            json.put("tareasPendientes", r.getTareasPendientes());
            json.put("tareasJustificadasCampo", r.getTareasJustificadasCampo());
            json.put("funcionalidadesDesarrolladas", r.getFuncionalidadesDesarrolladas());
            json.put("incidenciasCorregidas", r.getIncidenciasCorregidas());
            json.put("bugsDetectados", r.getBugsDetectados());
            json.put("bugsGenerados", r.getBugsGenerados());
            json.put("tareasAprobadas", r.getTareasAprobadas());
            json.put("tareasRequierenCorreccion", r.getTareasRequierenCorreccion());
            json.put("cumplimientoMeta", r.getCumplimientoMeta());
            json.put("huboDificultad", r.isHuboDificultad());
            json.put("detalleDificultad", r.getDetalleDificultad());
            json.put("esSalidaCampo", r.isEsSalidaCampo());
            json.put("motivoSalidaCampo", r.getMotivoSalidaCampo());
            json.put("observacionSupervisor", r.getObservacionSupervisor());
            json.put("firmaPracticante", r.getFirmaPracticante());
            json.put("firmaSupervisor", r.getFirmaSupervisor());
            json.put("nivelProductividad", r.getNivelProductividad());
            json.put("puntaje", r.getPuntaje());

            JSONArray actArray = new JSONArray();
            if (r.getActividades() != null) {
                for (KpiActivityItem a : r.getActividades()) {
                    JSONObject actJson = new JSONObject();
                    actJson.put("id", a.getId());
                    actJson.put("numero", a.getNumero());
                    actJson.put("proyecto", a.getProyecto());
                    actJson.put("tipo", a.getTipo());
                    actJson.put("descripcion", a.getDescripcion());
                    actJson.put("horaInicio", a.getHoraInicio());
                    actJson.put("horaFin", a.getHoraFin());
                    actJson.put("estado", a.getEstado());
                    actJson.put("bugs", a.getBugs());
                    actJson.put("commitHash", a.getCommitHash());
                    actArray.put(actJson);
                }
            }
            json.put("actividades", actArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private KpiDailyReport jsonToReport(JSONObject json) {
        if (json == null) return null;
        KpiDailyReport r = new KpiDailyReport();
        r.setId(json.optString("id", ""));
        r.setFecha(json.optString("fecha", ""));
        r.setPracticante(json.optString("practicante", ""));
        r.setSupervisor(json.optString("supervisor", ""));
        r.setTurno(json.optString("turno", ""));
        r.setRol(json.optString("rol", ""));
        r.setTareasAsignadas(json.optInt("tareasAsignadas", 0));
        r.setTareasCompletadas(json.optInt("tareasCompletadas", 0));
        r.setTareasPendientes(json.optInt("tareasPendientes", 0));
        r.setTareasJustificadasCampo(json.optInt("tareasJustificadasCampo", 0));
        r.setFuncionalidadesDesarrolladas(json.optInt("funcionalidadesDesarrolladas", 0));
        r.setIncidenciasCorregidas(json.optInt("incidenciasCorregidas", 0));
        r.setBugsDetectados(json.optInt("bugsDetectados", 0));
        r.setBugsGenerados(json.optInt("bugsGenerados", 0));
        r.setTareasAprobadas(json.optInt("tareasAprobadas", 0));
        r.setTareasRequierenCorreccion(json.optInt("tareasRequierenCorreccion", 0));
        r.setCumplimientoMeta(json.optInt("cumplimientoMeta", 0));
        r.setHuboDificultad(json.optBoolean("huboDificultad", false));
        r.setDetalleDificultad(json.optString("detalleDificultad", ""));
        r.setEsSalidaCampo(json.optBoolean("esSalidaCampo", false));
        r.setMotivoSalidaCampo(json.optString("motivoSalidaCampo", ""));
        r.setObservacionSupervisor(json.optString("observacionSupervisor", ""));
        r.setFirmaPracticante(json.optString("firmaPracticante", ""));
        r.setFirmaSupervisor(json.optString("firmaSupervisor", ""));
        r.setNivelProductividad(json.optString("nivelProductividad", ""));
        r.setPuntaje(json.optInt("puntaje", 0));

        JSONArray actArray = json.optJSONArray("actividades");
        List<KpiActivityItem> actList = new ArrayList<>();
        if (actArray != null) {
            for (int i = 0; i < actArray.length(); i++) {
                JSONObject actJson = actArray.optJSONObject(i);
                if (actJson != null) {
                    KpiActivityItem a = new KpiActivityItem(
                            actJson.optString("id", ""),
                            actJson.optString("numero", ""),
                            actJson.optString("proyecto", ""),
                            actJson.optString("tipo", ""),
                            actJson.optString("descripcion", ""),
                            actJson.optString("horaInicio", ""),
                            actJson.optString("horaFin", ""),
                            actJson.optString("estado", "Entregado"),
                            actJson.optInt("bugs", 0),
                            actJson.optString("commitHash", "")
                    );
                    actList.add(a);
                }
            }
        }
        r.setActividades(actList);
        return r;
    }
}
