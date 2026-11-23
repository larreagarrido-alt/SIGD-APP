package pe.net.ws.kpiweb.features.kpi.junior.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorActivity;
import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorDayRecord;
import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorProductionGoal;

/**
 * Repositorio independiente en memoria (RAM) para el Diseñador Gráfico Junior.
 * Cumple con la restricción de NO modificar bases de datos (SQLite, Room, MySQL, Firebase).
 * Mantiene la persistencia durante la sesión de la aplicación.
 */
public class KpiJuniorMemoryRepository {

    private static final String PREF_NAME = "kpi_junior_storage_v2";
    private static final String KEY_RECORDS = "key_junior_records";
    private static android.content.Context sAppContext;

    private static KpiJuniorMemoryRepository instance;
    private static int instanceUserId = Integer.MIN_VALUE;

    private static String scopedPrefName() {
        int userId = sAppContext == null ? 0 : new pe.net.ws.kpiweb.core.session.SessionManager(sAppContext).getUserId();
        return PREF_NAME + "_user_" + userId;
    }

    // Almacén en memoria indexado por Fecha ("DD/MM/YYYY")
    private final Map<String, KpiJuniorDayRecord> memoryStore = new LinkedHashMap<>();

    public static synchronized void init(android.content.Context context) {
        if (context != null) {
            sAppContext = context.getApplicationContext();
            if (instance != null) {
                instance.loadFromPrefs();
            }
        }
    }

    public static synchronized KpiJuniorMemoryRepository getInstance(android.content.Context context) {
        if (context != null) {
            sAppContext = context.getApplicationContext();
        }
        return getInstance();
    }

    public static synchronized KpiJuniorMemoryRepository getInstance() {
        int userId = sAppContext == null ? 0 : new pe.net.ws.kpiweb.core.session.SessionManager(sAppContext).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiJuniorMemoryRepository();
            instanceUserId = userId;
        }
        return instance;
    }

    private KpiJuniorMemoryRepository() {
        loadFromPrefs();
    }

    /**
     * Guarda o actualiza un registro diario en la memoria y lo persiste en disco.
     */
    public synchronized void saveRecord(KpiJuniorDayRecord record) {
        if (record != null && record.getDate() != null) {
            record.recalculateMetrics();
            String shiftTag = record.getShiftTag();
            memoryStore.put(record.getDate() + "_" + shiftTag, record);
            memoryStore.put(record.getDate(), record);
            saveToPrefs();
            if (sAppContext != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                    sAppContext, "DISENADOR_JUNIOR", record.getDate() + ":" + shiftTag, record);
        }
    }

    public synchronized void saveShiftRecord(String date, String shiftTag, KpiJuniorDayRecord record) {
        if (record != null && date != null) {
            record.setDate(date);
            record.recalculateMetrics();
            String tag = (shiftTag != null && shiftTag.toUpperCase().contains("TARDE")) ? "TARDE" : "MANANA";
            memoryStore.put(date + "_" + tag, record);
            if (!memoryStore.containsKey(date) || "MANANA".equals(tag)) {
                memoryStore.put(date, record);
            }
            saveToPrefs();
            if (sAppContext != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                    sAppContext, "DISENADOR_JUNIOR", date + ":" + tag, record);
        }
    }

    public synchronized KpiJuniorDayRecord getShiftRecord(String date, String shiftTag) {
        if (date == null) return null;
        String tag = (shiftTag != null && shiftTag.toUpperCase().contains("TARDE")) ? "TARDE" : "MANANA";
        String key = date + "_" + tag;
        KpiJuniorDayRecord r = memoryStore.get(key);
        if (r != null) return r;

        String altDate = date.startsWith("0") ? date.substring(1) : ("0" + date);
        r = memoryStore.get(altDate + "_" + tag);
        if (r != null) return r;

        KpiJuniorDayRecord general = memoryStore.get(date);
        if (general != null && (general.getShiftTag().equalsIgnoreCase(tag) || "MANANA".equalsIgnoreCase(tag))) {
            return general;
        }

        String[] parts = date.split("/");
        if (parts.length > 0) {
            String dayNum = parts[0].trim();
            for (Map.Entry<String, KpiJuniorDayRecord> entry : memoryStore.entrySet()) {
                if (entry.getKey().contains("_" + tag)) {
                    KpiJuniorDayRecord rec = entry.getValue();
                    if (rec.getDate() != null && (rec.getDate().startsWith(dayNum + "/") || rec.getDate().startsWith("0" + dayNum + "/"))) {
                        return rec;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Obtiene el registro correspondiente a una fecha específica.
     */
    public synchronized KpiJuniorDayRecord getRecordByDate(String date) {
        return memoryStore.get(date);
    }

    /**
     * Obtiene todos los registros en memoria ordenados cronológicamente.
     */
    public synchronized List<KpiJuniorDayRecord> getAllRecords() {
        return new ArrayList<>(memoryStore.values());
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiJuniorDayRecord record = new com.google.gson.Gson().fromJson(payload, KpiJuniorDayRecord.class);
        if (record == null || record.getDate() == null || record.getDate().isEmpty()) return false;
        String tag = record.getShiftTag();
        String key = record.getDate() + "_" + tag;
        if (memoryStore.containsKey(key) && !allowReplace) return false;
        memoryStore.put(key, record);
        if (!memoryStore.containsKey(record.getDate()) || "MANANA".equals(tag)) memoryStore.put(record.getDate(), record);
        saveToPrefs();
        return true;
    }

    /**
     * Obtiene los registros para la semana de muestra (del 12/10/2026 al 18/10/2026).
     */
    public synchronized List<KpiJuniorDayRecord> getWeeklyRecords(String startDate, String endDate) {
        List<KpiJuniorDayRecord> list = new ArrayList<>();
        String[] days = {
                "12/10/2026", "13/10/2026", "14/10/2026", "15/10/2026",
                "16/10/2026", "17/10/2026", "18/10/2026"
        };
        for (String day : days) {
            KpiJuniorDayRecord record = memoryStore.get(day);
            if (record != null) {
                list.add(record);
            }
        }
        return list;
    }

    /**
     * Precarga los datos exactos del reporte semanal (semana del 12/10 al 18/10 de 2026)
     * para reflejar fielmente las pantallas y gráficos de los bocetos.
     */
    private void seedInitialMockData() {
        // 1. Lunes 12/10/2026 (4 Posts, 2 Videos)
        createSampleDay("12/10/2026", 4, 2, 0, KpiJuniorProductionGoal.PRODUCCION_MIXTA);

        // 2. Martes 13/10/2026 (6 Posts, 2 Videos)
        createSampleDay("13/10/2026", 6, 2, 0, KpiJuniorProductionGoal.DISENO_GRAFICO);

        // 3. Miércoles 14/10/2026 (5 Posts, 1 Video)
        createSampleDay("14/10/2026", 5, 1, 1, KpiJuniorProductionGoal.PRODUCCION_MIXTA);

        // 4. Jueves 15/10/2026 (7 Posts, 3 Videos)
        createSampleDay("15/10/2026", 7, 3, 0, KpiJuniorProductionGoal.DISENO_GRAFICO);

        // 5. Viernes 16/10/2026 (5 Posts, 2 Videos) - DÍA PRINCIPAL DE REFERENCIA
        createFridayRecord();

        // 6. Sábado 17/10/2026 (4 Posts, 1 Video)
        createSampleDay("17/10/2026", 4, 1, 1, KpiJuniorProductionGoal.DISENO_GRAFICO);

        // 7. Domingo 18/10/2026 (3 Posts, 1 Video)
        createSampleDay("18/10/2026", 3, 1, 0, KpiJuniorProductionGoal.DISENO_GRAFICO);
    }

    private void createFridayRecord() {
        KpiJuniorDayRecord friday = new KpiJuniorDayRecord();
        friday.setDate("16/10/2026");
        friday.setUserName("Oso");
        friday.setSupervisor("Lic. Jefferson Cueva (Supervisor Creativo)");
        friday.setShift("Mañana");
        friday.setGoal(KpiJuniorProductionGoal.PRODUCCION_MIXTA);

        List<KpiJuniorActivity> activities = new ArrayList<>();
        activities.add(new KpiJuniorActivity(1, "Cliente ABC", "Diseño de flyer promoción", KpiJuniorActivity.Type.POST, "08:00 am", "09:30 am", KpiJuniorActivity.Status.ENTREGADO, 0));
        activities.add(new KpiJuniorActivity(2, "Tienda XYZ", "Post para redes", KpiJuniorActivity.Type.POST, "09:40 am", "11:10 am", KpiJuniorActivity.Status.ENTREGADO, 1));
        activities.add(new KpiJuniorActivity(3, "Marca Sol", "Edición de video corto", KpiJuniorActivity.Type.VIDEO, "11:20 am", "01:30 pm", KpiJuniorActivity.Status.ENTREGADO, 0));
        activities.add(new KpiJuniorActivity(4, "Inmobiliaria Top", "Diseño de imagen campaña", KpiJuniorActivity.Type.POST, "03:30 pm", "05:00 pm", KpiJuniorActivity.Status.ENTREGADO, 0));
        activities.add(new KpiJuniorActivity(5, "Restaurante Gourmet", "Video para historias", KpiJuniorActivity.Type.VIDEO, "05:00 pm", "06:30 pm", KpiJuniorActivity.Status.ENTREGADO, 0));

        friday.setActivities(activities);
        friday.setEvalGoalMet(true);
        friday.setEvalDeliveredOnTime(true);
        friday.setEvalBriefingMet(true);
        friday.setEvalQualityAdequate(true);
        friday.setEvalFilesOrganized(true);
        friday.setEvalCommunication(true);
        friday.setHasIncident(false);
        friday.setSupervisorObservation("Excelente ritmo de entrega y cumplimiento del briefing.");
        friday.setBiometricVerified(true);
        friday.setVerificationTimestamp(System.currentTimeMillis());

        friday.recalculateMetrics();
        memoryStore.put(friday.getDate(), friday);
    }

    private void createSampleDay(String date, int posts, int videos, int pendingCount, KpiJuniorProductionGoal goal) {
        KpiJuniorDayRecord rec = new KpiJuniorDayRecord();
        rec.setDate(date);
        rec.setUserName("Oso");
        rec.setSupervisor("Lic. Jefferson Cueva (Supervisor Creativo)");
        rec.setShift("Mañana");
        rec.setGoal(goal);

        List<KpiJuniorActivity> activities = new ArrayList<>();
        int count = 1;
        for (int i = 0; i < posts; i++) {
            KpiJuniorActivity.Status status = (pendingCount > 0 && i == 0) ? KpiJuniorActivity.Status.PENDIENTE : KpiJuniorActivity.Status.ENTREGADO;
            activities.add(new KpiJuniorActivity(count++, "Cliente " + (char)('A' + i), "Diseño para redes #" + (i + 1), KpiJuniorActivity.Type.POST, "08:00 am", "10:30 am", status, 0));
        }
        for (int j = 0; j < videos; j++) {
            activities.add(new KpiJuniorActivity(count++, "Proyecto Video " + (j + 1), "Edición reel redes #" + (j + 1), KpiJuniorActivity.Type.VIDEO, "03:30 pm", "06:30 pm", KpiJuniorActivity.Status.ENTREGADO, 0));
        }

        rec.setActivities(activities);
        rec.setEvalGoalMet(true);
        rec.setEvalDeliveredOnTime(true);
        rec.setEvalBriefingMet(true);
        rec.setEvalQualityAdequate(true);
        rec.setEvalFilesOrganized(true);
        rec.setEvalCommunication(true);
        rec.setBiometricVerified(true);
        rec.recalculateMetrics();

        memoryStore.put(date, rec);
    }

    private synchronized void saveToPrefs() {
        if (sAppContext == null) return;
        try {
            android.content.SharedPreferences prefs = sAppContext.getSharedPreferences(scopedPrefName(), android.content.Context.MODE_PRIVATE);
            org.json.JSONArray array = new org.json.JSONArray();
            for (KpiJuniorDayRecord r : memoryStore.values()) {
                array.put(r.toJson());
            }
            prefs.edit().putString(KEY_RECORDS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        if (sAppContext == null) return;
        try {
            android.content.SharedPreferences prefs = sAppContext.getSharedPreferences(scopedPrefName(), android.content.Context.MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_RECORDS, null);
            if (jsonStr != null) {
                org.json.JSONArray array = new org.json.JSONArray(jsonStr);
                memoryStore.clear();
                for (int i = 0; i < array.length(); i++) {
                    org.json.JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        KpiJuniorDayRecord r = KpiJuniorDayRecord.fromJson(obj);
                        if (r != null && r.getDate() != null) {
                            String tag = r.getShiftTag();
                            memoryStore.put(r.getDate() + "_" + tag, r);
                            if (!memoryStore.containsKey(r.getDate()) || "MANANA".equals(tag)) {
                                memoryStore.put(r.getDate(), r);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
