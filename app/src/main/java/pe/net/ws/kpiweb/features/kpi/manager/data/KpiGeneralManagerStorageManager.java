package pe.net.ws.kpiweb.features.kpi.manager.data;

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

import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerDayRecord;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerWeekReport;

/**
 * Gestor de almacenamiento persistente para el cargo de GERENTE GENERAL (SIGD 2026).
 * Conectado al flujo de Colaborador Ejecutivo y accesible por el rol ADMIN:
 * - Persiste los registros diarios de ambos bloques (Gestión Propia y Supervisión de Áreas).
 * - Sincroniza en tiempo real con el Centro de Reportes Semanales (KpiReportsHubFragment).
 * - Precarga la Semana N.º 37 de referencia oficial (Manuel Castillo) y mantiene activa
 *   la semana calendario actual de Lunes a Sábado.
 */
public class KpiGeneralManagerStorageManager {

    private static final String PREF_NAME = "kpi_general_manager_storage_v1";
    private static final String KEY_WEEKS_DATA = "kpi_gm_weeks_json";
    private static final String KEY_FULL_WEEKS = "key_full_weeks_json";
    private static final String KEY_CURRENT_WEEK_ID = "kpi_gm_current_week_id";

    public static final String[] DAY_NAMES = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
    };

    public static final String[] DAY_SHORTS = {
            "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"
    };

    public static final String[] DEFAULT_INDICATORS = {
            "Seguimiento de objetivos",
            "Gestión estratégica",
            "Seguimiento de áreas",
            "Control de recursos",
            "Cumplimiento del SIGD",
            "Revisión general y coordinación"
    };

    public static final String[] DEFAULT_GOALS = {
            "Revisar 2 – 3 indicadores prioritarios",
            "Resolver 1 – 2 asuntos prioritarios",
            "Revisar avances de las áreas programadas",
            "Verificar compromisos presupuestarios del día",
            "Revisar incidencias y acciones pendientes",
            "Consolidar avances y planificar la siguiente semana"
    };

    public static final String[] DEFAULT_TRACKING_HINTS = {
            "3 indicadores",
            "2 asuntos",
            "4 áreas",
            "Presupuesto y gastos",
            "0 incidencias",
            "Plan semanal"
    };

    public static final String[] DEFAULT_SUPERVISED_AREAS = {
            "Marketing / Diseño",
            "Producción Audiovisual",
            "Comercial / Ventas",
            "Administración",
            "Contabilidad",
            "Todas las áreas"
    };

    private static volatile KpiGeneralManagerStorageManager instance;
    private static int instanceUserId = Integer.MIN_VALUE;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Map<String, KpiGeneralManagerWeekReport> weekStore = new LinkedHashMap<>();

    private KpiGeneralManagerStorageManager(Context context) {
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

    public static synchronized KpiGeneralManagerStorageManager getInstance(Context context) {
        int userId = new pe.net.ws.kpiweb.core.session.SessionManager(context).getUserId();
        if (instance == null || instanceUserId != userId) {
            instance = new KpiGeneralManagerStorageManager(context);
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
            List<KpiGeneralManagerDayRecord> cleanDays = new ArrayList<>();
            String startStr = "";
            String endStr = "";

            for (int i = 0; i < 6; i++) {
                Calendar dCal = (Calendar) monday.clone();
                dCal.add(Calendar.DAY_OF_MONTH, i);
                String dateStr = df.format(dCal.getTime());
                if (i == 0) startStr = dateStr;
                if (i == 5) endStr = dateStr;

                KpiGeneralManagerDayRecord rec = new KpiGeneralManagerDayRecord(
                        dateStr,
                        DAY_NAMES[i],
                        DAY_SHORTS[i],
                        DEFAULT_INDICATORS[i],
                        DEFAULT_GOALS[i],
                        DEFAULT_SUPERVISED_AREAS[i]
                );
                rec.setTrackingPerformed("");
                rec.setPriorityCount(0);
                rec.setOwnCompliancePct(0);
                rec.setOwnObservations("");
                rec.setAreaCompliancePct(0);
                rec.setAreaObservations("");
                rec.setAreaActions("");
                rec.setRegistered(false);
                rec.setLocked(false);
                cleanDays.add(rec);
            }

            KpiGeneralManagerWeekReport activeReport = new KpiGeneralManagerWeekReport(
                    realWeekId, weekNum, "Del " + startStr + " al " + endStr
            );
            activeReport.setDays(cleanDays);
            activeReport.setStatus("En curso");
            activeReport.setWeeklyIndicatorsReviewed(18);
            activeReport.setWeeklyIssuesResolved(11);
            activeReport.setAchievements("• Cumplimiento del 100% de la gestión.\n• Avance de campañas digitales.\n• Coordinación efectiva entre áreas.\n• Cierre del plan semanal sin incidencias.");
            activeReport.setDifficulties("• Menor alcance en leads (miércoles).\n• Ajustes en el presupuesto de pauta.\n• Reprogramación de una reunión externa.");
            activeReport.setImprovementPlan("• Fortalecer la generación de leads.\n• Optimizar el uso del presupuesto.\n• Realizar seguimiento más cercano a Ventas.\n• Revisar nuevas oportunidades de campaña.");

            Map<String, KpiGeneralManagerWeekReport> reordered = new LinkedHashMap<>();
            reordered.put(realWeekId, activeReport);
            reordered.putAll(weekStore);
            weekStore.clear();
            weekStore.putAll(reordered);
        }

        prefs.edit().putString(KEY_CURRENT_WEEK_ID, realWeekId).apply();
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
        KpiGeneralManagerWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null || week.getDays().size() < 6) return false;
        for (KpiGeneralManagerDayRecord d : week.getDays()) {
            if (!d.isLocked()) return false;
        }
        return true;
    }

    public synchronized int getNextUnlockedDayIndexAfter(int fromIndex) {
        KpiGeneralManagerWeekReport week = getCurrentWeek();
        if (week == null || week.getDays() == null) return -1;
        List<KpiGeneralManagerDayRecord> list = week.getDays();
        for (int i = fromIndex + 1; i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        for (int i = 0; i < fromIndex && i < list.size(); i++) {
            if (!list.get(i).isLocked()) return i;
        }
        return -1;
    }

    public synchronized KpiGeneralManagerWeekReport getCurrentWeek() {
        ensureCurrentCalendarWeek();
        String currentId = prefs.getString(KEY_CURRENT_WEEK_ID, "");
        KpiGeneralManagerWeekReport report = weekStore.get(currentId);
        if (report == null && !weekStore.isEmpty()) {
            report = weekStore.values().iterator().next();
        }
        return report;
    }

    public synchronized KpiGeneralManagerWeekReport getWeekById(String weekId) {
        return weekStore.get(weekId);
    }

    public synchronized List<KpiGeneralManagerWeekReport> getAllWeeks() {
        return new ArrayList<>(weekStore.values());
    }

    public synchronized void saveDayRecord(KpiGeneralManagerDayRecord record) {
        if (record == null || record.getDate() == null) return;
        record.setLocked(true);
        record.setRegistered(true);

        KpiGeneralManagerWeekReport week = getCurrentWeek();
        if (week != null) {
            boolean found = false;
            for (int i = 0; i < week.getDays().size(); i++) {
                KpiGeneralManagerDayRecord d = week.getDays().get(i);
                if (d.getDate().equals(record.getDate()) || d.getDayName().equalsIgnoreCase(record.getDayName())) {
                    week.getDays().set(i, record);
                    found = true;
                    break;
                }
            }
            if (!found) {
                week.getDays().add(record);
            }
            if (areAllCurrentWeekDaysLocked()) {
                week.setStatus("Enviado");
            }
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    public synchronized void saveWeekSummaryNotes(String achievements, String difficulties, String improvementPlan,
                                                  int indicatorsReviewed, int issuesResolved) {
        KpiGeneralManagerWeekReport week = getCurrentWeek();
        if (week != null) {
            if (achievements != null) week.setAchievements(achievements);
            if (difficulties != null) week.setDifficulties(difficulties);
            if (improvementPlan != null) week.setImprovementPlan(improvementPlan);
            if (indicatorsReviewed > 0) week.setWeeklyIndicatorsReviewed(indicatorsReviewed);
            if (issuesResolved > 0) week.setWeeklyIssuesResolved(issuesResolved);
            saveToPrefs();
            syncCurrentWeek();
        }
    }

    public synchronized void saveToPrefs() {
        try {
            JSONArray array = new JSONArray();
            for (KpiGeneralManagerWeekReport w : weekStore.values()) {
                array.put(w.toJson());
            }
            prefs.edit().putString(KEY_WEEKS_DATA, array.toString())
                    .putString(KEY_FULL_WEEKS, new com.google.gson.Gson().toJson(weekStore)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        String fullJson = prefs.getString(KEY_FULL_WEEKS, null);
        if (fullJson != null) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, KpiGeneralManagerWeekReport>>() {}.getType();
                Map<String, KpiGeneralManagerWeekReport> full = new com.google.gson.Gson().fromJson(fullJson, type);
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
                        KpiGeneralManagerWeekReport report = KpiGeneralManagerWeekReport.fromJson(weekJson);
                        if (report != null && !report.getWeekId().isEmpty()) {
                            weekStore.put(report.getWeekId(), report);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void seedOfficialWeek37() {
        KpiGeneralManagerWeekReport w37 = new KpiGeneralManagerWeekReport(
                "Semana N.º 37", 37, "Del 04/09/2026 al 10/09/2026"
        );
        w37.setStatus("Enviado");
        w37.setWeeklyIndicatorsReviewed(18);
        w37.setWeeklyIssuesResolved(11);
        w37.setAchievements("• Cumplimiento del 100% de la gestión.\n• Avance de campañas digitales.\n• Coordinación efectiva entre áreas.\n• Cierre del plan semanal sin incidencias.");
        w37.setDifficulties("• Menor alcance en leads (miércoles).\n• Ajustes en el presupuesto de pauta.\n• Reprogramación de una reunión externa.");
        w37.setImprovementPlan("• Fortalecer la generación de leads.\n• Optimizar el uso del presupuesto.\n• Realizar seguimiento más cercano a Ventas.\n• Revisar nuevas oportunidades de campaña.");

        List<KpiGeneralManagerDayRecord> days37 = new ArrayList<>();

        String[] dates37 = {"04/09/2026", "05/09/2026", "07/09/2026", "08/09/2026", "09/09/2026", "10/09/2026"};
        String[] trackings37 = {
                "3 indicadores",
                "2 asuntos",
                "4 áreas",
                "Presupuesto y gastos",
                "0 incidencias",
                "Plan semanal"
        };
        int[] counts37 = {3, 2, 4, 1, 0, 1};
        int[] ownPct37 = {100, 100, 100, 100, 100, 100};
        String[] ownObs37 = {
                "Revisión y alineación de metas.",
                "Definición de acciones comerciales.",
                "Seguimiento a Marketing, Producción y Operaciones.",
                "Control de gastos y disponibilidades.",
                "Se mantuvo el cumplimiento del SIGD.",
                "Cierre de avances y definición de prioridades."
        };
        int[] areaPct37 = {100, 100, 95, 100, 100, 100};
        String[] areaObs37 = {
                "Avance de campañas.",
                "Cumplimiento de cronograma.",
                "Menor alcance en leads.",
                "Procesos en orden.",
                "Registros al día.",
                "Consolidación semanal."
        };
        String[] areaAct37 = {
                "Ajuste de contenidos.",
                "Revisión y validación.",
                "Refuerzo de pauta.",
                "Control de documentos.",
                "Validación de reportes.",
                "Reunión de cierre."
        };

        for (int i = 0; i < 6; i++) {
            KpiGeneralManagerDayRecord rec = new KpiGeneralManagerDayRecord(
                    dates37[i],
                    DAY_NAMES[i],
                    DAY_SHORTS[i],
                    DEFAULT_INDICATORS[i],
                    DEFAULT_GOALS[i],
                    DEFAULT_SUPERVISED_AREAS[i]
            );
            rec.setTrackingPerformed(trackings37[i]);
            rec.setPriorityCount(counts37[i]);
            rec.setOwnCompliancePct(ownPct37[i]);
            rec.setOwnObservations(ownObs37[i]);
            rec.setAreaCompliancePct(areaPct37[i]);
            rec.setAreaObservations(areaObs37[i]);
            rec.setAreaActions(areaAct37[i]);
            rec.setRegistered(true);
            rec.setLocked(true);
            days37.add(rec);
        }

        w37.setDays(days37);
        weekStore.put("Semana N.º 37", w37);
    }
    private void syncCurrentWeek() {
        var week = getCurrentWeek();
        if (week != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                appContext, "GERENCIA", week.getWeekId(), week);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiGeneralManagerWeekReport report = new com.google.gson.Gson().fromJson(payload, KpiGeneralManagerWeekReport.class);
        if (report == null || report.getWeekId() == null || report.getWeekId().isEmpty()) return false;
        KpiGeneralManagerWeekReport local = weekStore.get(report.getWeekId());
        if (local != null && local.getDays() != null) for (KpiGeneralManagerDayRecord day : local.getDays())
            if (day.isRegistered() && !allowReplace) return false;
        weekStore.put(report.getWeekId(), report);
        saveToPrefs();
        return true;
    }

}

