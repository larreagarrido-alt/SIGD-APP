package pe.net.ws.kpiweb.features.kpi.assistant.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantDayRecord;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantWeekReport;

/**
 * Gestor de almacenamiento persistente para el cargo de ASISTENTE ADMINISTRATIVO (SIGD 2026).
 * Conectado al flujo de Colaborador Ejecutivo y a los usuarios reales de la base de datos:
 * - Persiste los registros diarios de Gestiones Realizadas, Documentos Registrados y Coordinaciones Internas.
 * - Calcula automáticamente el total de actividades diario/semanal (meta ≥ 72 actividades) y el nivel KPI.
 * - Sincroniza en tiempo real con el Centro de Reportes Semanales (KpiReportsHubFragment).
 */
public class KpiAdminAssistantStorageManager {

    private static final String PREF_NAME = "kpi_admin_assistant_storage_v1";
    private static final String KEY_WEEKS_DATA = "kpi_asst_weeks_json";
    private static final String KEY_FULL_WEEKS = "key_full_weeks_json";
    private static final String KEY_CURRENT_WEEK_ID = "kpi_asst_current_week_id";

    public static final String[] DAY_NAMES = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    };

    public static final String[] DAY_SHORTS = {
            "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"
    };

    public static final String[] DEFAULT_ACTIVITIES = {
            "Gestión de documentos\n(Registro y archivo)",
            "Coordinación de actividades\n(Áreas y equipo)",
            "Seguimiento de requerimientos\n(Compras / Servicios)",
            "Apoyo en logística\n(Recursos / Materiales)",
            "Control y actualización\n(Formatos / Registros)",
            "Reporte y consolidación\n(Resultados semanales)"
    };

    public static final String[] DEFAULT_GOAL_LABELS = {
            "≥ 10 registros",
            "≥ 4 coordinaciones",
            "≥ 5 seguimientos",
            "≥ 3 gestiones",
            "≥ 5 actualizaciones",
            "≥ 1 informe\n(semanal)"
    };

    public static final int[] DEFAULT_DAILY_EXPECTED_TOTALS = {
            23, 11, 11, 9, 14, 4
    };

    private static volatile KpiAdminAssistantStorageManager instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Map<String, KpiAdminAssistantWeekReport> weekStore = new LinkedHashMap<>();

    private KpiAdminAssistantStorageManager(Context context) {
        this.appContext = context.getApplicationContext();
        pe.net.ws.kpiweb.core.session.SessionManager session = new pe.net.ws.kpiweb.core.session.SessionManager(context);
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME + "_user_" + session.getUserId(), Context.MODE_PRIVATE);
        loadFromPrefs();
        if (weekStore.isEmpty() && session.getWorkerId() <= 0) {
            seedOfficialWeek37();
        }
        ensureCurrentCalendarWeek();
        saveToPrefs();
    }

    public static synchronized KpiAdminAssistantStorageManager getInstance(Context context) {
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiAdminAssistantStorageManager(context);
            instanceUserId = userId;
        }
        return instance;
    }

    private synchronized void ensureCurrentCalendarWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int year = cal.get(Calendar.YEAR);
        int weekNum = cal.get(Calendar.WEEK_OF_YEAR);
        String realWeekId = "Semana N.º " + weekNum + " (" + year + ")";

        if (!weekStore.containsKey(realWeekId)) {
            Calendar monday = (Calendar) cal.clone();
            int dow = monday.get(Calendar.DAY_OF_WEEK);
            int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
            monday.add(Calendar.DAY_OF_MONTH, -diff);

            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            List<KpiAdminAssistantDayRecord> cleanDays = new ArrayList<>();
            String startStr = "";
            String endStr = "";

            for (int i = 0; i < 6; i++) {
                Calendar dCal = (Calendar) monday.clone();
                dCal.add(Calendar.DAY_OF_MONTH, i);
                String dateStr = df.format(dCal.getTime());
                if (i == 0) startStr = dateStr;
                if (i == 5) endStr = dateStr;

                KpiAdminAssistantDayRecord rec = new KpiAdminAssistantDayRecord(
                        dateStr,
                        DAY_NAMES[i],
                        DAY_SHORTS[i],
                        DEFAULT_ACTIVITIES[i],
                        DEFAULT_GOAL_LABELS[i],
                        DEFAULT_DAILY_EXPECTED_TOTALS[i]
                );
                rec.setGestionesCount(0);
                rec.setDocumentosCount(0);
                rec.setCoordinacionesCount(0);
                rec.setObservations("");
                rec.setRegistered(false);
                rec.setLocked(false);
                cleanDays.add(rec);
            }

            KpiAdminAssistantWeekReport activeReport = new KpiAdminAssistantWeekReport(
                    realWeekId, weekNum, "Del " + startStr + " al " + endStr
            );
            activeReport.setDays(cleanDays);
            activeReport.setStatus("En curso");
            activeReport.setMorningShift(true);
            activeReport.setOtherActivitiesChecked(false);
            activeReport.setGeneralObservations(
                    ""
            );

            Map<String, KpiAdminAssistantWeekReport> reordered = new LinkedHashMap<>();
            reordered.put(realWeekId, activeReport);
            reordered.putAll(weekStore);
            weekStore.clear();
            weekStore.putAll(reordered);
        }

        prefs.edit().putString(KEY_CURRENT_WEEK_ID, realWeekId).apply();
    }

    private void seedOfficialWeek37() {
        String weekId = "Semana N.º 37";
        KpiAdminAssistantWeekReport week37 = new KpiAdminAssistantWeekReport(
                weekId, 37, "Del 04/09/2026 al 10/09/2026"
        );
        week37.setStatus("Completado");
        week37.setMorningShift(true);
        week37.setOtherActivitiesChecked(false);
        week37.setGeneralObservations(
                "Se cumplió con la gestión administrativa, el registro de documentos y la coordinación de actividades, alcanzando el 100% de la meta semanal."
        );

        String[] dates = {
                "04/09/2026", "05/09/2026", "06/09/2026",
                "07/09/2026", "08/09/2026", "09/09/2026"
        };
        int[] gestiones = {12, 5, 6, 3, 7, 1};
        int[] documentos = {8, 2, 3, 4, 5, 2};
        int[] coordinaciones = {3, 4, 2, 2, 2, 1};
        String[] obs = {
                "Registro de solicitudes y archivado digital.",
                "Coordinación de reuniones internas.",
                "Seguimiento de compras y servicios.",
                "Gestión de materiales para el equipo.",
                "Actualización de reportes y formatos.",
                "Elaboración del reporte semanal de gestión."
        };

        List<KpiAdminAssistantDayRecord> days = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            KpiAdminAssistantDayRecord rec = new KpiAdminAssistantDayRecord(
                    dates[i],
                    DAY_NAMES[i],
                    DAY_SHORTS[i],
                    DEFAULT_ACTIVITIES[i],
                    DEFAULT_GOAL_LABELS[i],
                    DEFAULT_DAILY_EXPECTED_TOTALS[i]
            );
            rec.setGestionesCount(gestiones[i]);
            rec.setDocumentosCount(documentos[i]);
            rec.setCoordinacionesCount(coordinaciones[i]);
            rec.setObservations(obs[i]);
            rec.setRegistered(true);
            rec.setLocked(true);
            days.add(rec);
        }
        week37.setDays(days);
        weekStore.put(weekId, week37);
    }

    public int getTodayDayIndex() {
        Calendar now = Calendar.getInstance();
        switch (now.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            default: return 5;
        }
    }

    public synchronized boolean areAllCurrentWeekDaysLocked() {
        KpiAdminAssistantWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null || week.getDays().size() < 6) return false;
        for (KpiAdminAssistantDayRecord d : week.getDays()) {
            if (!d.isLocked()) return false;
        }
        return true;
    }

    public synchronized int getNextUnlockedDayIndexAfter(int fromIndex) {
        KpiAdminAssistantWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null) return -1;
        List<KpiAdminAssistantDayRecord> list = week.getDays();
        for (int i = fromIndex + 1; i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        for (int i = 0; i < fromIndex && i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        return -1;
    }

    public synchronized KpiAdminAssistantWeekReport getCurrentWeek() {
        ensureCurrentCalendarWeek();
        String currentId = prefs.getString(KEY_CURRENT_WEEK_ID, "");
        KpiAdminAssistantWeekReport report = weekStore.get(currentId);
        if (report == null && !weekStore.isEmpty()) {
            report = weekStore.values().iterator().next();
        }
        return report;
    }

    public synchronized KpiAdminAssistantWeekReport getWeekById(String weekId) {
        return weekStore.get(weekId);
    }

    public synchronized List<KpiAdminAssistantWeekReport> getAllWeeks() {
        return new ArrayList<>(weekStore.values());
    }

    public synchronized void saveDayRecord(KpiAdminAssistantDayRecord record) {
        if (record == null) return;
        KpiAdminAssistantWeekReport current = getCurrentWeek();
        if (current == null || current.getDays() == null) return;
        List<KpiAdminAssistantDayRecord> days = current.getDays();
        for (int i = 0; i < days.size(); i++) {
            KpiAdminAssistantDayRecord existing = days.get(i);
            if (existing.getDayName().equalsIgnoreCase(record.getDayName())
                    || (existing.getDate() != null && existing.getDate().equals(record.getDate()))) {
                days.set(i, record);
                break;
            }
        }
        saveToPrefs();
            syncCurrentWeek();
    }

    public synchronized void saveWeekClosingData(
            boolean morningShift,
            boolean otherActivitiesChecked,
            String generalObservations
    ) {
        KpiAdminAssistantWeekReport current = getCurrentWeek();
        if (current == null) return;
        current.setMorningShift(morningShift);
        current.setOtherActivitiesChecked(otherActivitiesChecked);
        if (generalObservations != null) {
            current.setGeneralObservations(generalObservations.trim());
        }
        saveToPrefs();
            syncCurrentWeek();
    }

    public synchronized void markCurrentWeekSubmitted() {
        KpiAdminAssistantWeekReport current = getCurrentWeek();
        if (current != null) {
            current.setStatus("Enviado ✓");
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    private void loadFromPrefs() {
        String fullJson = prefs.getString(KEY_FULL_WEEKS, null);
        if (fullJson != null) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KpiAdminAssistantWeekReport>>() {}.getType();
                Map<String, KpiAdminAssistantWeekReport> full = new com.google.gson.Gson().fromJson(fullJson, type);
                if (full != null) { weekStore.clear(); weekStore.putAll(full); return; }
            } catch (RuntimeException ignored) { /* Preserve the legacy readable format. */ }
        }
        weekStore.clear();
        String json = prefs.getString(KEY_WEEKS_DATA, null);
        if (json == null || json.trim().isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject wObj = arr.getJSONObject(i);
                KpiAdminAssistantWeekReport week = new KpiAdminAssistantWeekReport(
                        wObj.optString("weekId", ""),
                        wObj.optInt("weekNumber", 37),
                        wObj.optString("dateRange", "")
                );
                week.setStatus(wObj.optString("status", "En curso"));
                week.setMorningShift(wObj.optBoolean("morningShift", true));
                week.setOtherActivitiesChecked(wObj.optBoolean("otherActivitiesChecked", false));
                week.setGeneralObservations(wObj.optString("generalObservations", ""));

                JSONArray dArr = wObj.optJSONArray("days");
                List<KpiAdminAssistantDayRecord> dList = new ArrayList<>();
                if (dArr != null) {
                    for (int j = 0; j < dArr.length(); j++) {
                        JSONObject dObj = dArr.getJSONObject(j);
                        KpiAdminAssistantDayRecord d = new KpiAdminAssistantDayRecord(
                                dObj.optString("date", ""),
                                dObj.optString("dayName", DAY_NAMES[Math.min(j, 5)]),
                                dObj.optString("dayShort", DAY_SHORTS[Math.min(j, 5)]),
                                dObj.optString("mainActivity", DEFAULT_ACTIVITIES[Math.min(j, 5)]),
                                dObj.optString("dailyGoalLabel", DEFAULT_GOAL_LABELS[Math.min(j, 5)]),
                                dObj.optInt("dailyExpectedTotal", DEFAULT_DAILY_EXPECTED_TOTALS[Math.min(j, 5)])
                        );
                        d.setGestionesCount(dObj.optInt("gestionesCount", 0));
                        d.setDocumentosCount(dObj.optInt("documentosCount", 0));
                        d.setCoordinacionesCount(dObj.optInt("coordinacionesCount", 0));
                        d.setObservations(dObj.optString("observations", ""));
                        d.setRegistered(dObj.optBoolean("registered", false));
                        d.setLocked(dObj.optBoolean("locked", false));
                        dList.add(d);
                    }
                }
                week.setDays(dList);
                weekStore.put(week.getWeekId(), week);
            }
        } catch (JSONException ignored) {
        }
    }

    private void saveToPrefs() {
        try {
            JSONArray arr = new JSONArray();
            for (KpiAdminAssistantWeekReport week : weekStore.values()) {
                JSONObject wObj = new JSONObject();
                wObj.put("weekId", week.getWeekId());
                wObj.put("weekNumber", week.getWeekNumber());
                wObj.put("dateRange", week.getDateRange());
                wObj.put("status", week.getStatus());
                wObj.put("morningShift", week.isMorningShift());
                wObj.put("otherActivitiesChecked", week.isOtherActivitiesChecked());
                wObj.put("generalObservations", week.getGeneralObservations());

                JSONArray dArr = new JSONArray();
                for (KpiAdminAssistantDayRecord d : week.getDays()) {
                    JSONObject dObj = new JSONObject();
                    dObj.put("date", d.getDate());
                    dObj.put("dayName", d.getDayName());
                    dObj.put("dayShort", d.getDayShort());
                    dObj.put("mainActivity", d.getMainActivity());
                    dObj.put("dailyGoalLabel", d.getDailyGoalLabel());
                    dObj.put("dailyExpectedTotal", d.getDailyExpectedTotal());
                    dObj.put("gestionesCount", d.getGestionesCount());
                    dObj.put("documentosCount", d.getDocumentosCount());
                    dObj.put("coordinacionesCount", d.getCoordinacionesCount());
                    dObj.put("observations", d.getObservations());
                    dObj.put("registered", d.isRegistered());
                    dObj.put("locked", d.isLocked());
                    dArr.put(dObj);
                }
                wObj.put("days", dArr);
                arr.put(wObj);
            }
            prefs.edit().putString(KEY_WEEKS_DATA, arr.toString())
                    .putString(KEY_FULL_WEEKS, new com.google.gson.Gson().toJson(weekStore)).apply();
        } catch (JSONException ignored) {
        }
    }
    private void syncCurrentWeek() {
        var week = getCurrentWeek();
        if (week != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                appContext, "ASISTENCIA_ADMIN", week.getWeekId(), week);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiAdminAssistantWeekReport report = new com.google.gson.Gson().fromJson(payload, KpiAdminAssistantWeekReport.class);
        if (report == null || report.getWeekId() == null || report.getWeekId().isEmpty()) return false;
        KpiAdminAssistantWeekReport local = weekStore.get(report.getWeekId());
        if (local != null && local.getDays() != null) for (KpiAdminAssistantDayRecord day : local.getDays())
            if (day.isRegistered() && !allowReplace) return false;
        weekStore.put(report.getWeekId(), report);
        saveToPrefs();
        return true;
    }

}

