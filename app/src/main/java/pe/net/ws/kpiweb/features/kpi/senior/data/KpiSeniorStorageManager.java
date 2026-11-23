package pe.net.ws.kpiweb.features.kpi.senior.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorDayRecord;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorWeekReport;

/**
 * Gestor de almacenamiento temporal privado para el Diseñador Gráfico Senior.
 * Cumple estrictamente con:
 * 1. NO utilizar SQLite, Room, MySQL ni backend.
 * 2. Guardar ambos bloques de producción y supervisión de forma conjunta.
 * 3. Identificar registros por fecha y evitar duplicados.
 * 4. Persistir datos tras reiniciar la aplicación mediante SharedPreferences privado (JSON).
 * 5. Servir de fuente para reportes semanales e histórico de solo lectura.
 */
public class KpiSeniorStorageManager {

    private static final String PREF_NAME = "kpi_senior_storage_v3";
    private static final String KEY_WEEKS_DATA = "kpi_senior_weeks_json";
    private static final String KEY_FULL_WEEKS = "key_full_weeks_json";
    private static final String KEY_CURRENT_WEEK_ID = "kpi_senior_current_week_id";

    private static volatile KpiSeniorStorageManager instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final Context appContext;
    private final SharedPreferences prefs;

    // Almacén en memoria sincronizado con SharedPreferences
    private final Map<String, KpiSeniorWeekReport> weekStore = new LinkedHashMap<>();

    private KpiSeniorStorageManager(Context context) {
        this.appContext = context.getApplicationContext();
        pe.net.ws.kpiweb.core.session.SessionManager session = new pe.net.ws.kpiweb.core.session.SessionManager(context);
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME + "_user_" + session.getUserId(), Context.MODE_PRIVATE);
        loadFromPrefs();
        if (weekStore.isEmpty() && session.getWorkerId() <= 0) {
            seedInitialHistoryData();
        }
        ensureCurrentCalendarWeek();
        saveToPrefs();
    }

    public static synchronized KpiSeniorStorageManager getInstance(Context context) {
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiSeniorStorageManager(context);
            instanceUserId = userId;
        }
        return instance;
    }

    private synchronized void ensureCurrentCalendarWeek() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setFirstDayOfWeek(java.util.Calendar.MONDAY);
        int year = cal.get(java.util.Calendar.YEAR);
        int weekNum = cal.get(java.util.Calendar.WEEK_OF_YEAR);
        String realWeekId = "Semana N.º " + weekNum + " (" + year + ")";

        if (!weekStore.containsKey(realWeekId)) {
            java.util.Calendar monday = (java.util.Calendar) cal.clone();
            int dow = monday.get(java.util.Calendar.DAY_OF_WEEK);
            int diff = (dow == java.util.Calendar.SUNDAY) ? 6 : (dow - java.util.Calendar.MONDAY);
            monday.add(java.util.Calendar.DAY_OF_MONTH, -diff);

            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String[] names = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
            String[] shorts = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
            int[] dailyGoals = {4, 4, 4, 4, 4, 2};

            List<KpiSeniorDayRecord> cleanDays = new ArrayList<>();
            String startStr = "";
            String endStr = "";
            for (int i = 0; i < 6; i++) {
                java.util.Calendar dCal = (java.util.Calendar) monday.clone();
                dCal.add(java.util.Calendar.DAY_OF_MONTH, i);
                String dateStr = df.format(dCal.getTime());
                if (i == 0) startStr = dateStr;
                if (i == 5) endStr = dateStr;

                KpiSeniorDayRecord rec = new KpiSeniorDayRecord(dateStr, names[i], shorts[i]);
                rec.setDesignsCount(0);
                rec.setVideosCount(0);
                rec.setPhotoSessionsCount(0);
                rec.setOtherActivitiesCount(0);
                rec.setActivitiesDescription("");
                rec.setSupervisedSeniorCount(0);
                rec.setSupervisedJuniorCount(0);
                rec.setTeamCompliancePercentage(0);
                rec.setSupervisionObservations("");
                rec.setSupervisionActions("");
                rec.setDailyGoal(dailyGoals[i]);
                rec.setRegistered(false);
                rec.setLocked(false);
                cleanDays.add(rec);
            }

            KpiSeniorWeekReport activeReport = new KpiSeniorWeekReport(
                    realWeekId, weekNum, "Del " + startStr + " al " + endStr, 22);
            activeReport.setDays(cleanDays);
            activeReport.setStatus("En curso");

            Map<String, KpiSeniorWeekReport> reordered = new LinkedHashMap<>();
            reordered.put(realWeekId, activeReport);
            reordered.putAll(weekStore);
            weekStore.clear();
            weekStore.putAll(reordered);
        }

        prefs.edit().putString(KEY_CURRENT_WEEK_ID, realWeekId).apply();
    }

    public int getTodayDayIndex() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        switch (now.get(java.util.Calendar.DAY_OF_WEEK)) {
            case java.util.Calendar.MONDAY: return 0;
            case java.util.Calendar.TUESDAY: return 1;
            case java.util.Calendar.WEDNESDAY: return 2;
            case java.util.Calendar.THURSDAY: return 3;
            case java.util.Calendar.FRIDAY: return 4;
            default: return 5;
        }
    }

    public synchronized boolean areAllCurrentWeekDaysLocked() {
        KpiSeniorWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null || week.getDays().size() < 6) return false;
        for (KpiSeniorDayRecord d : week.getDays()) {
            if (!d.isLocked()) return false;
        }
        return true;
    }

    public synchronized int getNextUnlockedDayIndexAfter(int fromIndex) {
        KpiSeniorWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null) return -1;
        List<KpiSeniorDayRecord> list = week.getDays();
        for (int i = fromIndex + 1; i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        for (int i = 0; i < fromIndex && i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        return -1;
    }

    /**
     * Retorna la semana actual activa del calendario en curso.
     */
    public synchronized KpiSeniorWeekReport getCurrentWeek() {
        ensureCurrentCalendarWeek();
        String currentId = prefs.getString(KEY_CURRENT_WEEK_ID, "");
        KpiSeniorWeekReport report = weekStore.get(currentId);
        if (report == null && !weekStore.isEmpty()) {
            report = weekStore.values().iterator().next();
        }
        return report;
    }

    public synchronized void setCurrentWeekId(String weekId) {
        if (weekStore.containsKey(weekId)) {
            prefs.edit().putString(KEY_CURRENT_WEEK_ID, weekId).apply();
        }
    }

    public synchronized KpiSeniorWeekReport getWeekById(String weekId) {
        return weekStore.get(weekId);
    }

    public synchronized List<KpiSeniorWeekReport> getAllWeeks() {
        return new ArrayList<>(weekStore.values());
    }

    /**
     * Guarda o actualiza el registro diario de ambos bloques, lo bloquea y recalcula la semana.
     */
    public synchronized void saveDayRecord(KpiSeniorDayRecord record) {
        if (record == null || record.getDate() == null) return;

        record.setLocked(true);
        record.setRegistered(true);

        // Buscar en qué semana encaja la fecha o insertar en la semana activa
        KpiSeniorWeekReport week = getCurrentWeek();
        if (week != null) {
            boolean found = false;
            for (int i = 0; i < week.getDays().size(); i++) {
                KpiSeniorDayRecord d = week.getDays().get(i);
                if (d.getDate().equals(record.getDate())) {
                    week.getDays().set(i, record);
                    found = true;
                    break;
                }
            }
            if (!found) {
                week.getDays().add(record);
            }
            // Guardar cambios persistentes
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    /**
     * Retorna el registro de un día según su fecha.
     */
    public synchronized KpiSeniorDayRecord getDayRecord(String date) {
        for (KpiSeniorWeekReport week : weekStore.values()) {
            for (KpiSeniorDayRecord day : week.getDays()) {
                if (day.getDate().equals(date)) {
                    return day;
                }
            }
        }
        return null;
    }

    /**
     * Comprueba si el día para la fecha dada ya está guardado y bloqueado.
     */
    public synchronized boolean isDayLocked(String date) {
        KpiSeniorDayRecord record = getDayRecord(date);
        return record != null && record.isLocked();
    }

    /**
     * Desbloquea un día para permitir nuevas pruebas de registro.
     */
    public synchronized void unlockDayRecord(String date) {
        KpiSeniorDayRecord record = getDayRecord(date);
        if (record != null) {
            record.setLocked(false);
            record.setRegistered(false);
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    /**
     * Formatea la fecha de hoy para mostrarla en la interfaz.
     */
    public static String getFormattedTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        String dateStr = sdf.format(new Date());
        return Character.toUpperCase(dateStr.charAt(0)) + dateStr.substring(1);
    }

    /**
     * Persiste todas las semanas en SharedPreferences.
     */
    public synchronized void saveToPrefs() {
        try {
            JSONArray array = new JSONArray();
            for (KpiSeniorWeekReport w : weekStore.values()) {
                array.put(w.toJson());
            }
            prefs.edit().putString(KEY_WEEKS_DATA, array.toString())
                    .putString(KEY_FULL_WEEKS, new com.google.gson.Gson().toJson(weekStore)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga las semanas desde SharedPreferences si existen.
     */
    private synchronized void loadFromPrefs() {
        String fullJson = prefs.getString(KEY_FULL_WEEKS, null);
        if (fullJson != null) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KpiSeniorWeekReport>>() {}.getType();
                Map<String, KpiSeniorWeekReport> full = new com.google.gson.Gson().fromJson(fullJson, type);
                if (full != null) { weekStore.clear(); weekStore.putAll(full); return; }
            } catch (RuntimeException ignored) { /* Preserve the legacy readable format. */ }
        }
        String jsonStr = prefs.getString(KEY_WEEKS_DATA, null);
        if (jsonStr != null) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                weekStore.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject weekJson = array.optJSONObject(i);
                    if (weekJson != null) {
                        KpiSeniorWeekReport report = KpiSeniorWeekReport.fromJson(weekJson);
                        if (report != null && report.getWeekId() != null) {
                            weekStore.put(report.getWeekId(), report);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Precarga los datos iniciales de las semanas 32 a 37 que coinciden fielmente con los diseños.
     */
    private void seedInitialHistoryData() {
        // SEMANA N.º 37 (Semana de referencia)
        KpiSeniorWeekReport w37 = new KpiSeniorWeekReport("Semana N.º 37", 37, "Del 07/09/2026 al 12/09/2026", 63);
        List<KpiSeniorDayRecord> days37 = new ArrayList<>();

        // Lunes 07/09/2026
        KpiSeniorDayRecord lun = new KpiSeniorDayRecord("07/09/2026", "Lunes", "Lun");
        lun.setDesignsCount(2);
        lun.setVideosCount(2);
        lun.setPhotoSessionsCount(0);
        lun.setOtherActivitiesCount(0);
        lun.setActivitiesDescription("Diseño de posts para redes sociales y edición de un reel promocional.");
        lun.setSupervisedSeniorCount(1);
        lun.setSupervisedJuniorCount(1);
        lun.setTeamCompliancePercentage(60);
        lun.setSupervisionObservations("Producciones en desarrollo, buen avance del equipo.");
        lun.setSupervisionActions("Revisión de piezas, coordinación y feedback.");
        lun.setRegistered(false);
        lun.setLocked(false);
        lun.setDailyGoal(4);
        days37.add(lun);

        // Martes 08/09/2026
        KpiSeniorDayRecord mar = new KpiSeniorDayRecord("08/09/2026", "Martes", "Mar");
        mar.setDesignsCount(1);
        mar.setVideosCount(0);
        mar.setPhotoSessionsCount(0);
        mar.setOtherActivitiesCount(0);
        mar.setActivitiesDescription("Diseño de banners de campaña.");
        mar.setSupervisedSeniorCount(1);
        mar.setSupervisedJuniorCount(1);
        mar.setTeamCompliancePercentage(25);
        mar.setSupervisionObservations("Producciones en ajuste.");
        mar.setSupervisionActions("Feedback de diseño.");
        mar.setRegistered(true);
        mar.setLocked(true);
        mar.setDailyGoal(2);
        days37.add(mar);

        // Miércoles 09/09/2026
        KpiSeniorDayRecord mie = new KpiSeniorDayRecord("09/09/2026", "Miércoles", "Mié");
        mie.setDesignsCount(2);
        mie.setVideosCount(2);
        mie.setPhotoSessionsCount(0);
        mie.setOtherActivitiesCount(0);
        mie.setActivitiesDescription("Edición y animación de reels institucionales.");
        mie.setSupervisedSeniorCount(1);
        mie.setSupervisedJuniorCount(1);
        mie.setTeamCompliancePercentage(50);
        mie.setSupervisionObservations("Producciones con buen ritmo.");
        mie.setSupervisionActions("Revisión técnica de piezas.");
        mie.setRegistered(true);
        mie.setLocked(true);
        mie.setDailyGoal(4);
        days37.add(mie);

        // Jueves 10/09/2026
        KpiSeniorDayRecord jue = new KpiSeniorDayRecord("10/09/2026", "Jueves", "Jue");
        jue.setDesignsCount(0);
        jue.setVideosCount(2);
        jue.setPhotoSessionsCount(1);
        jue.setOtherActivitiesCount(0);
        jue.setActivitiesDescription("Sesión fotográfica de producto y postproducción de video.");
        jue.setSupervisedSeniorCount(1);
        jue.setSupervisedJuniorCount(1);
        jue.setTeamCompliancePercentage(50);
        jue.setSupervisionObservations("Producciones en postproducción.");
        jue.setSupervisionActions("Ajuste de iluminación y color.");
        jue.setRegistered(true);
        jue.setLocked(true);
        jue.setDailyGoal(3);
        days37.add(jue);

        // Viernes 11/09/2026
        KpiSeniorDayRecord vie = new KpiSeniorDayRecord("11/09/2026", "Viernes", "Vie");
        vie.setDesignsCount(4);
        vie.setVideosCount(2);
        vie.setPhotoSessionsCount(0);
        vie.setOtherActivitiesCount(0);
        vie.setActivitiesDescription("Flyers para eventos del fin de semana.");
        vie.setSupervisedSeniorCount(1);
        vie.setSupervisedJuniorCount(1);
        vie.setTeamCompliancePercentage(50);
        vie.setSupervisionObservations("Producciones de cierre listas.");
        vie.setSupervisionActions("Aprobación final de publicaciones.");
        vie.setRegistered(true);
        vie.setLocked(true);
        vie.setDailyGoal(6);
        days37.add(vie);

        // Sábado 12/09/2026
        KpiSeniorDayRecord sab = new KpiSeniorDayRecord("12/09/2026", "Sábado", "Sáb");
        sab.setDesignsCount(0);
        sab.setVideosCount(0);
        sab.setPhotoSessionsCount(2);
        sab.setOtherActivitiesCount(0);
        sab.setActivitiesDescription("Cobertura y sesión en locación exterior.");
        sab.setSupervisedSeniorCount(1);
        sab.setSupervisedJuniorCount(1);
        sab.setTeamCompliancePercentage(100);
        sab.setSupervisionObservations("Producciones completadas con excelencia.");
        sab.setSupervisionActions("Cierre y entrega al cliente.");
        sab.setRegistered(true);
        sab.setLocked(true);
        sab.setDailyGoal(2);
        days37.add(sab);

        w37.setDays(days37);
        w37.setStatus("Completada");
        weekStore.put(w37.getWeekId(), w37);

        // SEMANAS HISTÓRICAS 36, 35, 34, 33, 32
        weekStore.put("Semana N.º 36", createHistoricalWeek("Semana N.º 36", 36, "Del 31/08/2026 al 05/09/2026", 75.00f, 6));
        weekStore.put("Semana N.º 35", createHistoricalWeek("Semana N.º 35", 35, "Del 24/08/2026 al 29/08/2026", 82.50f, 6));
        weekStore.put("Semana N.º 34", createHistoricalWeek("Semana N.º 34", 34, "Del 17/08/2026 al 22/08/2026", 90.00f, 6));
        weekStore.put("Semana N.º 33", createHistoricalWeek("Semana N.º 33", 33, "Del 10/08/2026 al 15/08/2026", 60.00f, 6));
        weekStore.put("Semana N.º 32", createHistoricalWeek("Semana N.º 32", 32, "Del 03/08/2026 al 08/08/2026", 78.33f, 6));
    }

    private KpiSeniorWeekReport createHistoricalWeek(String id, int num, String range, float complPct, int daysCount) {
        KpiSeniorWeekReport report = new KpiSeniorWeekReport(id, num, range, 60);
        List<KpiSeniorDayRecord> days = new ArrayList<>();
        String[] names = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        String[] shorts = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
        for (int i = 0; i < daysCount; i++) {
            KpiSeniorDayRecord d = new KpiSeniorDayRecord("0" + (i + 1) + "/08/2026", names[i], shorts[i]);
            d.setDesignsCount(3);
            d.setVideosCount(1);
            d.setPhotoSessionsCount(i == 5 ? 1 : 0);
            d.setOtherActivitiesCount(1);
            d.setSupervisedSeniorCount(1);
            d.setSupervisedJuniorCount(1);
            d.setTeamCompliancePercentage(Math.round(complPct));
            d.setRegistered(true);
            d.setLocked(true);
            days.add(d);
        }
        report.setDays(days);
        report.setStatus("Completada");
        return report;
    }
    private void syncCurrentWeek() {
        var week = getCurrentWeek();
        if (week != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                appContext, "DISENADOR_SENIOR", week.getWeekId(), week);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiSeniorWeekReport report = new com.google.gson.Gson().fromJson(payload, KpiSeniorWeekReport.class);
        if (report == null || report.getWeekId() == null || report.getWeekId().isEmpty()) return false;
        KpiSeniorWeekReport local = weekStore.get(report.getWeekId());
        if (local != null && local.getDays() != null) for (KpiSeniorDayRecord day : local.getDays())
            if (day.isRegistered() && !allowReplace) return false;
        weekStore.put(report.getWeekId(), report);
        saveToPrefs();
        return true;
    }

}
