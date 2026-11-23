package pe.net.ws.kpiweb.features.kpi.community.data;

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

import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityDayRecord;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityWeekReport;

/**
 * Gestor de almacenamiento persistente para el cargo de COMMUNITY MANAGER (SIGD 2026).
 * Conectado al flujo de Colaborador Ejecutivo y a la hoja oficial de producción:
 * - Persiste los registros diarios de Publicaciones, Interacciones atendidas y Otras actividades.
 * - Calcula automáticamente el total de actividades diario/semanal y el nivel de productividad.
 * - Sincroniza en tiempo real con el Centro de Reportes Semanales (KpiReportsHubFragment).
 */
public class KpiCommunityStorageManager {

    private static final String PREF_NAME = "kpi_community_storage_v1";
    private static final String KEY_WEEKS_DATA = "kpi_community_weeks_json";
    private static final String KEY_FULL_WEEKS = "key_full_weeks_json";
    private static final String KEY_CURRENT_WEEK_ID = "kpi_community_current_week_id";

    public static final String[] DAY_NAMES = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    };

    public static final String[] DAY_SHORTS = {
            "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"
    };

    public static final String[] DEFAULT_ACTIVITIES = {
            "Gestión de redes\n(Programación y publicación)",
            "Atención a la comunidad\n(Mensajes y comentarios)",
            "Seguimiento de redes\n(Interacción y alcance)",
            "Gestión de publicaciones\n(Según calendario)",
            "Revisión de métricas\n(Alcance e interacción)",
            "Reporte semanal\n(Resumen y avances)"
    };

    public static final String[] DEFAULT_GOAL_LABELS = {
            "1 - 2 publicaciones",
            "≥ 10 interacciones",
            "≥ 10 interacciones",
            "1 - 2 publicaciones",
            "1 análisis de resultados",
            "1 reporte de avances"
    };

    public static final int[] DEFAULT_DAILY_EXPECTED_TOTALS = {
            11, 14, 13, 12, 9, 6
    };

    private static volatile KpiCommunityStorageManager instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Map<String, KpiCommunityWeekReport> weekStore = new LinkedHashMap<>();

    private KpiCommunityStorageManager(Context context) {
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

    public static synchronized KpiCommunityStorageManager getInstance(Context context) {
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiCommunityStorageManager(context);
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
            List<KpiCommunityDayRecord> cleanDays = new ArrayList<>();
            String startStr = "";
            String endStr = "";

            for (int i = 0; i < 6; i++) {
                Calendar dCal = (Calendar) monday.clone();
                dCal.add(Calendar.DAY_OF_MONTH, i);
                String dateStr = df.format(dCal.getTime());
                if (i == 0) startStr = dateStr;
                if (i == 5) endStr = dateStr;

                KpiCommunityDayRecord rec = new KpiCommunityDayRecord(
                        dateStr,
                        DAY_NAMES[i],
                        DAY_SHORTS[i],
                        DEFAULT_ACTIVITIES[i],
                        DEFAULT_GOAL_LABELS[i],
                        DEFAULT_DAILY_EXPECTED_TOTALS[i]
                );
                rec.setPublicacionesCount(0);
                rec.setInteraccionesCount(0);
                rec.setOtrasActividadesCount(0);
                rec.setObservations("");
                rec.setRegistered(false);
                rec.setLocked(false);
                cleanDays.add(rec);
            }

            KpiCommunityWeekReport activeReport = new KpiCommunityWeekReport(
                    realWeekId, weekNum, "Del " + startStr + " al " + endStr
            );
            activeReport.setDays(cleanDays);
            activeReport.setStatus("En curso");
            activeReport.setMorningShift(true);
            activeReport.setOtherActivitiesChecked(false);
            activeReport.setGeneralObservations(
                    ""
            );

            Map<String, KpiCommunityWeekReport> reordered = new LinkedHashMap<>();
            reordered.put(realWeekId, activeReport);
            reordered.putAll(weekStore);
            weekStore.clear();
            weekStore.putAll(reordered);
        }

        prefs.edit().putString(KEY_CURRENT_WEEK_ID, realWeekId).apply();
    }

    private void seedOfficialWeek37() {
        String weekId = "Semana N.º 37";
        KpiCommunityWeekReport week37 = new KpiCommunityWeekReport(
                weekId, 37, "Del 04/09/2026 al 10/09/2026"
        );
        week37.setStatus("Completado");
        week37.setMorningShift(true);
        week37.setOtherActivitiesChecked(false);
        week37.setGeneralObservations(
                "Se cumplió con la gestión de redes sociales, la publicación de 9 contenidos, la atención de 50 interacciones y la entrega del reporte semanal, alcanzando un 98% de cumplimiento general."
        );

        String[] dates = {
                "04/09/2026", "05/09/2026", "06/09/2026",
                "07/09/2026", "08/09/2026", "09/09/2026"
        };
        int[] publicaciones = {2, 2, 2, 2, 1, 0};
        int[] interacciones = {8, 11, 10, 9, 7, 5};
        int[] otras = {1, 1, 1, 1, 1, 1};
        String[] obs = {
                "Publicación de contenidos en Facebook e Instagram.",
                "Respuesta a mensajes y consultas.",
                "Monitoreo de interacciones y alcance.",
                "Publicación de contenidos programados.",
                "Revisión del rendimiento de la semana.",
                "Elaboración del reporte semanal."
        };

        List<KpiCommunityDayRecord> days = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            KpiCommunityDayRecord rec = new KpiCommunityDayRecord(
                    dates[i],
                    DAY_NAMES[i],
                    DAY_SHORTS[i],
                    DEFAULT_ACTIVITIES[i],
                    DEFAULT_GOAL_LABELS[i],
                    DEFAULT_DAILY_EXPECTED_TOTALS[i]
            );
            rec.setPublicacionesCount(publicaciones[i]);
            rec.setInteraccionesCount(interacciones[i]);
            rec.setOtrasActividadesCount(otras[i]);
            rec.setObservations(obs[i]);
            rec.setRegistered(true);
            rec.setLocked(true);
            days.add(rec);
        }

        week37.setDays(days);
        weekStore.put(weekId, week37);
        prefs.edit().putString(KEY_CURRENT_WEEK_ID, weekId).apply();
    }

    public synchronized KpiCommunityWeekReport getCurrentWeekReport() {
        String currentId = prefs.getString(KEY_CURRENT_WEEK_ID, null);
        if (currentId != null && weekStore.containsKey(currentId)) {
            return weekStore.get(currentId);
        }
        if (!weekStore.isEmpty()) {
            return weekStore.values().iterator().next();
        }
        ensureCurrentCalendarWeek();
        return weekStore.values().iterator().next();
    }

    public synchronized KpiCommunityWeekReport getWeekReport(String weekId) {
        if (weekId == null) return getCurrentWeekReport();
        for (Map.Entry<String, KpiCommunityWeekReport> entry : weekStore.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(weekId)
                    || entry.getKey().contains(weekId)
                    || weekId.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return getCurrentWeekReport();
    }

    public synchronized List<String> getWeekList() {
        return new ArrayList<>(weekStore.keySet());
    }

    public synchronized void saveDayRecord(
            int dayIndex,
            int publicaciones,
            int interacciones,
            int otrasActividades,
            String observations
    ) {
        KpiCommunityWeekReport currentWeek = getCurrentWeekReport();
        if (currentWeek != null && currentWeek.getDays() != null && dayIndex >= 0 && dayIndex < currentWeek.getDays().size()) {
            KpiCommunityDayRecord rec = currentWeek.getDays().get(dayIndex);
            rec.setPublicacionesCount(publicaciones);
            rec.setInteraccionesCount(interacciones);
            rec.setOtrasActividadesCount(otrasActividades);
            rec.setObservations(observations);
            rec.setRegistered(true);
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    public synchronized void saveSaturdayClosing(
            boolean act1,
            boolean act2,
            boolean act3,
            boolean act4,
            boolean act5,
            boolean act6,
            boolean otherActivities,
            String generalObservations
    ) {
        KpiCommunityWeekReport currentWeek = getCurrentWeekReport();
        if (currentWeek != null) {
            currentWeek.setAction1GestionarRedes(act1);
            currentWeek.setAction2ProgramarContenidos(act2);
            currentWeek.setAction3AtenderMensajes(act3);
            currentWeek.setAction4SeguimientoMetricas(act4);
            currentWeek.setAction5ElaborarReporte(act5);
            currentWeek.setAction6CoordinarPresencia(act6);
            currentWeek.setOtherActivitiesChecked(otherActivities);
            currentWeek.setGeneralObservations(generalObservations);
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    public synchronized void saveShift(boolean morningShift) {
        KpiCommunityWeekReport currentWeek = getCurrentWeekReport();
        if (currentWeek != null) {
            currentWeek.setMorningShift(morningShift);
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    public int getTodayDayIndex() {
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            default: return 0;
        }
    }

    private synchronized void saveToPrefs() {
        try {
            JSONArray arr = new JSONArray();
            for (KpiCommunityWeekReport rep : weekStore.values()) {
                JSONObject obj = new JSONObject();
                obj.put("weekId", rep.getWeekId());
                obj.put("weekNumber", rep.getWeekNumber());
                obj.put("dateRange", rep.getDateRange());
                obj.put("status", rep.getStatus());
                obj.put("morningShift", rep.isMorningShift());
                obj.put("action1GestionarRedes", rep.isAction1GestionarRedes());
                obj.put("action2ProgramarContenidos", rep.isAction2ProgramarContenidos());
                obj.put("action3AtenderMensajes", rep.isAction3AtenderMensajes());
                obj.put("action4SeguimientoMetricas", rep.isAction4SeguimientoMetricas());
                obj.put("action5ElaborarReporte", rep.isAction5ElaborarReporte());
                obj.put("action6CoordinarPresencia", rep.isAction6CoordinarPresencia());
                obj.put("otherActivitiesChecked", rep.isOtherActivitiesChecked());
                obj.put("generalObservations", rep.getGeneralObservations());

                JSONArray daysArr = new JSONArray();
                for (KpiCommunityDayRecord d : rep.getDays()) {
                    JSONObject dObj = new JSONObject();
                    dObj.put("date", d.getDate());
                    dObj.put("dayName", d.getDayName());
                    dObj.put("dayShort", d.getDayShort());
                    dObj.put("mainActivity", d.getMainActivity());
                    dObj.put("dailyGoalLabel", d.getDailyGoalLabel());
                    dObj.put("dailyExpectedTotal", d.getDailyExpectedTotal());
                    dObj.put("publicacionesCount", d.getPublicacionesCount());
                    dObj.put("interaccionesCount", d.getInteraccionesCount());
                    dObj.put("otrasActividadesCount", d.getOtrasActividadesCount());
                    dObj.put("observations", d.getObservations());
                    dObj.put("registered", d.isRegistered());
                    dObj.put("locked", d.isLocked());
                    daysArr.put(dObj);
                }
                obj.put("days", daysArr);
                arr.put(obj);
            }
            prefs.edit().putString(KEY_WEEKS_DATA, arr.toString())
                    .putString(KEY_FULL_WEEKS, new com.google.gson.Gson().toJson(weekStore)).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        String fullJson = prefs.getString(KEY_FULL_WEEKS, null);
        if (fullJson != null) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KpiCommunityWeekReport>>() {}.getType();
                Map<String, KpiCommunityWeekReport> full = new com.google.gson.Gson().fromJson(fullJson, type);
                if (full != null) { weekStore.clear(); weekStore.putAll(full); return; }
            } catch (RuntimeException ignored) { /* Preserve the legacy readable format. */ }
        }
        String jsonStr = prefs.getString(KEY_WEEKS_DATA, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) return;

        try {
            JSONArray arr = new JSONArray(jsonStr);
            weekStore.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String weekId = obj.optString("weekId");
                int weekNum = obj.optInt("weekNumber");
                String dateRange = obj.optString("dateRange");

                KpiCommunityWeekReport rep = new KpiCommunityWeekReport(weekId, weekNum, dateRange);
                rep.setStatus(obj.optString("status", "En curso"));
                rep.setMorningShift(obj.optBoolean("morningShift", true));
                rep.setAction1GestionarRedes(obj.optBoolean("action1GestionarRedes", true));
                rep.setAction2ProgramarContenidos(obj.optBoolean("action2ProgramarContenidos", true));
                rep.setAction3AtenderMensajes(obj.optBoolean("action3AtenderMensajes", true));
                rep.setAction4SeguimientoMetricas(obj.optBoolean("action4SeguimientoMetricas", true));
                rep.setAction5ElaborarReporte(obj.optBoolean("action5ElaborarReporte", true));
                rep.setAction6CoordinarPresencia(obj.optBoolean("action6CoordinarPresencia", true));
                rep.setOtherActivitiesChecked(obj.optBoolean("otherActivitiesChecked", false));
                rep.setGeneralObservations(obj.optString("generalObservations", ""));

                JSONArray daysArr = obj.optJSONArray("days");
                List<KpiCommunityDayRecord> days = new ArrayList<>();
                if (daysArr != null) {
                    for (int j = 0; j < daysArr.length(); j++) {
                        JSONObject dObj = daysArr.getJSONObject(j);
                        KpiCommunityDayRecord d = new KpiCommunityDayRecord(
                                dObj.optString("date"),
                                dObj.optString("dayName"),
                                dObj.optString("dayShort"),
                                dObj.optString("mainActivity"),
                                dObj.optString("dailyGoalLabel"),
                                dObj.optInt("dailyExpectedTotal", 1)
                        );
                        d.setPublicacionesCount(dObj.optInt("publicacionesCount", 0));
                        d.setInteraccionesCount(dObj.optInt("interaccionesCount", 0));
                        d.setOtrasActividadesCount(dObj.optInt("otrasActividadesCount", 0));
                        d.setObservations(dObj.optString("observations", ""));
                        d.setRegistered(dObj.optBoolean("registered", false));
                        d.setLocked(dObj.optBoolean("locked", false));
                        days.add(d);
                    }
                }
                rep.setDays(days);
                weekStore.put(weekId, rep);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void syncCurrentWeek() {
        var week = getCurrentWeekReport();
        if (week != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                appContext, "COMMUNITY", week.getWeekId(), week);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiCommunityWeekReport report = new com.google.gson.Gson().fromJson(payload, KpiCommunityWeekReport.class);
        if (report == null || report.getWeekId() == null || report.getWeekId().isEmpty()) return false;
        KpiCommunityWeekReport local = weekStore.get(report.getWeekId());
        if (local != null && local.getDays() != null) for (KpiCommunityDayRecord day : local.getDays())
            if (day.isRegistered() && !allowReplace) return false;
        weekStore.put(report.getWeekId(), report);
        saveToPrefs();
        return true;
    }

}

