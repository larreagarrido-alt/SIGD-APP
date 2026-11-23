package pe.net.ws.kpiweb.features.kpi.photographer.data;

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

import pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerDayRecord;
import pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerWeekReport;

/**
 * Gestor de almacenamiento persistente para el cargo de FOTÓGRAFO (SIGD 2026).
 * Conectado al flujo de Colaborador Ejecutivo y a los usuarios reales de la base de datos:
 * - Persiste los registros diarios de Sesiones Realizadas, Fotos Capturadas y Fotos Editadas.
 * - Calcula automáticamente el total de actividades diario/semanal (meta ≥ 16 actividades) y el nivel KPI.
 * - Sincroniza en tiempo real con el Centro de Reportes Semanales (KpiReportsHubFragment).
 */
public class KpiPhotographerStorageManager {

    private static final String PREF_NAME = "kpi_photographer_storage_v1";
    private static final String KEY_WEEKS_DATA = "kpi_photog_weeks_json";
    private static final String KEY_FULL_WEEKS = "key_full_weeks_json";
    private static final String KEY_CURRENT_WEEK_ID = "kpi_photog_current_week_id";

    public static final String[] DAY_NAMES = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    };

    public static final String[] DAY_SHORTS = {
            "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"
    };

    public static final String[] DEFAULT_ACTIVITIES = {
            "Sesión de producto\n(Banners para campaña)",
            "Cobertura de redes\n(Contenido de oficina)",
            "Sesión de cliente\n(Cliente externo)",
            "Edición y revelado\n(Postproducción)",
            "Sesión creativa\n(Branding / Interna)",
            "Organización y respaldo\n(Archivo y entrega)"
    };

    public static final String[] DEFAULT_GOAL_LABELS = {
            "≥ 1 sesión\n(≥ 40 fotos)",
            "≥ 1 sesión\n(≥ 20 fotos)",
            "≥ 1 sesión\n(≥ 40 fotos)",
            "≥ 30 fotos\neditadas",
            "≥ 1 sesión\n(≥ 30 fotos)",
            "≥ 1 entrega\n(≥ 20 fotos)"
    };

    public static final int[] DEFAULT_DAILY_EXPECTED_TOTALS = {
            3, 3, 3, 3, 3, 2
    };

    private static volatile KpiPhotographerStorageManager instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Map<String, KpiPhotographerWeekReport> weekStore = new LinkedHashMap<>();

    private KpiPhotographerStorageManager(Context context) {
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

    public static synchronized KpiPhotographerStorageManager getInstance(Context context) {
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiPhotographerStorageManager(context);
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
            List<KpiPhotographerDayRecord> cleanDays = new ArrayList<>();
            String startStr = "";
            String endStr = "";

            for (int i = 0; i < 6; i++) {
                Calendar dCal = (Calendar) monday.clone();
                dCal.add(Calendar.DAY_OF_MONTH, i);
                String dateStr = df.format(dCal.getTime());
                if (i == 0) startStr = dateStr;
                if (i == 5) endStr = dateStr;

                KpiPhotographerDayRecord rec = new KpiPhotographerDayRecord(
                        dateStr,
                        DAY_NAMES[i],
                        DAY_SHORTS[i],
                        DEFAULT_ACTIVITIES[i],
                        DEFAULT_GOAL_LABELS[i],
                        DEFAULT_DAILY_EXPECTED_TOTALS[i]
                );
                rec.setSesionesCount(0);
                rec.setFotosCapturadasCount(0);
                rec.setFotosEditadasCount(0);
                rec.setObservations("");
                rec.setRegistered(false);
                rec.setLocked(false);
                cleanDays.add(rec);
            }

            KpiPhotographerWeekReport activeReport = new KpiPhotographerWeekReport(
                    realWeekId, weekNum, "Del " + startStr + " al " + endStr
            );
            activeReport.setDays(cleanDays);
            activeReport.setStatus("En curso");
            activeReport.setMorningShift(true);
            activeReport.setOtherActivitiesChecked(false);
            activeReport.setGeneralObservations(
                    ""
            );

            Map<String, KpiPhotographerWeekReport> reordered = new LinkedHashMap<>();
            reordered.put(realWeekId, activeReport);
            reordered.putAll(weekStore);
            weekStore.clear();
            weekStore.putAll(reordered);
        }

        prefs.edit().putString(KEY_CURRENT_WEEK_ID, realWeekId).apply();
    }

    private void seedOfficialWeek37() {
        String weekId = "Semana N.º 37";
        KpiPhotographerWeekReport week37 = new KpiPhotographerWeekReport(
                weekId, 37, "Del 04/09/2026 al 10/09/2026"
        );
        week37.setStatus("Completado");
        week37.setMorningShift(true);
        week37.setOtherActivitiesChecked(false);
        week37.setGeneralObservations(
                "Se cumplió con la mayoría de sesiones planificadas, se superó la meta de fotos capturadas y editadas, y se entregó el material en los plazos establecidos."
        );

        String[] dates = {
                "04/09/2026", "05/09/2026", "06/09/2026",
                "07/09/2026", "08/09/2026", "09/09/2026"
        };
        int[] sesiones = {1, 1, 1, 1, 1, 0};
        int[] capturadas = {42, 28, 48, 36, 37, 24};
        int[] editadas = {30, 20, 35, 36, 32, 24};
        String[] obs = {
                "Toma y edición campaña fotos en set.",
                "Fotos para redes sociales.",
                "Cobertura de cliente A.",
                "Edición de material pendiente.",
                "Sesión para branding interno.",
                "Respaldo y entrega en Drive."
        };

        List<KpiPhotographerDayRecord> days = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            KpiPhotographerDayRecord rec = new KpiPhotographerDayRecord(
                    dates[i],
                    DAY_NAMES[i],
                    DAY_SHORTS[i],
                    DEFAULT_ACTIVITIES[i],
                    DEFAULT_GOAL_LABELS[i],
                    DEFAULT_DAILY_EXPECTED_TOTALS[i]
            );
            rec.setSesionesCount(sesiones[i]);
            rec.setFotosCapturadasCount(capturadas[i]);
            rec.setFotosEditadasCount(editadas[i]);
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
        KpiPhotographerWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null || week.getDays().size() < 6) return false;
        for (KpiPhotographerDayRecord d : week.getDays()) {
            if (!d.isLocked()) return false;
        }
        return true;
    }

    public synchronized int getNextUnlockedDayIndexAfter(int fromIndex) {
        KpiPhotographerWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null) return -1;
        List<KpiPhotographerDayRecord> list = week.getDays();
        for (int i = fromIndex + 1; i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        for (int i = 0; i < fromIndex && i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        return -1;
    }

    public synchronized KpiPhotographerWeekReport getCurrentWeek() {
        ensureCurrentCalendarWeek();
        String currentId = prefs.getString(KEY_CURRENT_WEEK_ID, "");
        KpiPhotographerWeekReport report = weekStore.get(currentId);
        if (report == null && !weekStore.isEmpty()) {
            report = weekStore.values().iterator().next();
        }
        return report;
    }

    public synchronized KpiPhotographerWeekReport getWeekById(String weekId) {
        return weekStore.get(weekId);
    }

    public synchronized List<KpiPhotographerWeekReport> getAllWeeks() {
        return new ArrayList<>(weekStore.values());
    }

    public synchronized void saveDayRecord(KpiPhotographerDayRecord record) {
        if (record == null) return;
        KpiPhotographerWeekReport current = getCurrentWeek();
        if (current == null || current.getDays() == null) return;
        List<KpiPhotographerDayRecord> days = current.getDays();
        for (int i = 0; i < days.size(); i++) {
            KpiPhotographerDayRecord existing = days.get(i);
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
        KpiPhotographerWeekReport current = getCurrentWeek();
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
        KpiPhotographerWeekReport current = getCurrentWeek();
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
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KpiPhotographerWeekReport>>() {}.getType();
                Map<String, KpiPhotographerWeekReport> full = new com.google.gson.Gson().fromJson(fullJson, type);
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
                KpiPhotographerWeekReport week = new KpiPhotographerWeekReport(
                        wObj.optString("weekId", ""),
                        wObj.optInt("weekNumber", 37),
                        wObj.optString("dateRange", "")
                );
                week.setStatus(wObj.optString("status", "En curso"));
                week.setMorningShift(wObj.optBoolean("morningShift", true));
                week.setOtherActivitiesChecked(wObj.optBoolean("otherActivitiesChecked", false));
                week.setGeneralObservations(wObj.optString("generalObservations", ""));

                JSONArray dArr = wObj.optJSONArray("days");
                List<KpiPhotographerDayRecord> dList = new ArrayList<>();
                if (dArr != null) {
                    for (int j = 0; j < dArr.length(); j++) {
                        JSONObject dObj = dArr.getJSONObject(j);
                        KpiPhotographerDayRecord d = new KpiPhotographerDayRecord(
                                dObj.optString("date", ""),
                                dObj.optString("dayName", DAY_NAMES[Math.min(j, 5)]),
                                dObj.optString("dayShort", DAY_SHORTS[Math.min(j, 5)]),
                                dObj.optString("mainActivity", DEFAULT_ACTIVITIES[Math.min(j, 5)]),
                                dObj.optString("dailyGoalLabel", DEFAULT_GOAL_LABELS[Math.min(j, 5)]),
                                dObj.optInt("dailyExpectedTotal", DEFAULT_DAILY_EXPECTED_TOTALS[Math.min(j, 5)])
                        );
                        d.setSesionesCount(dObj.optInt("sesionesCount", 0));
                        d.setFotosCapturadasCount(dObj.optInt("fotosCapturadasCount", 0));
                        d.setFotosEditadasCount(dObj.optInt("fotosEditadasCount", 0));
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
            for (KpiPhotographerWeekReport week : weekStore.values()) {
                JSONObject wObj = new JSONObject();
                wObj.put("weekId", week.getWeekId());
                wObj.put("weekNumber", week.getWeekNumber());
                wObj.put("dateRange", week.getDateRange());
                wObj.put("status", week.getStatus());
                wObj.put("morningShift", week.isMorningShift());
                wObj.put("otherActivitiesChecked", week.isOtherActivitiesChecked());
                wObj.put("generalObservations", week.getGeneralObservations());

                JSONArray dArr = new JSONArray();
                for (KpiPhotographerDayRecord d : week.getDays()) {
                    JSONObject dObj = new JSONObject();
                    dObj.put("date", d.getDate());
                    dObj.put("dayName", d.getDayName());
                    dObj.put("dayShort", d.getDayShort());
                    dObj.put("mainActivity", d.getMainActivity());
                    dObj.put("dailyGoalLabel", d.getDailyGoalLabel());
                    dObj.put("dailyExpectedTotal", d.getDailyExpectedTotal());
                    dObj.put("sesionesCount", d.getSesionesCount());
                    dObj.put("fotosCapturadasCount", d.getFotosCapturadasCount());
                    dObj.put("fotosEditadasCount", d.getFotosEditadasCount());
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
                appContext, "FOTOGRAFIA", week.getWeekId(), week);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiPhotographerWeekReport report = new com.google.gson.Gson().fromJson(payload, KpiPhotographerWeekReport.class);
        if (report == null || report.getWeekId() == null || report.getWeekId().isEmpty()) return false;
        KpiPhotographerWeekReport local = weekStore.get(report.getWeekId());
        if (local != null && local.getDays() != null) for (KpiPhotographerDayRecord day : local.getDays())
            if (day.isRegistered() && !allowReplace) return false;
        weekStore.put(report.getWeekId(), report);
        saveToPrefs();
        return true;
    }

}

