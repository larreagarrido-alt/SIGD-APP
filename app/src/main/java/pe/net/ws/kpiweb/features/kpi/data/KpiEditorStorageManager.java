package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.community.data.KpiCommunityStorageManager;

/**
 * Gestor de persistencia y lógica de negocio para el Modo Editor de Reportes KPI.
 * Soporta:
 * 1. Modificación Global por Cargo (cuando [En específico] = "Todos"):
 *    - Modifica las metas base, actividades esperadas y ponderaciones del cargo.
 * 2. Modificación Individual por Colaborador:
 *    - Modifica los valores registrados, avances diarios y observaciones de ese colaborador.
 */
public class KpiEditorStorageManager {

    private static final String PREF_NAME = "kpi_editor_reports_storage_v1";
    private static final String PREF_GLOBAL_GOALS = "kpi_editor_global_cargo_goals_";
    private static final String PREF_INDIVIDUAL_RECORDS = "kpi_editor_indiv_record_";

    public static final String[] DAYS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    public static class DayEntry {
        public String dayName;
        public String activityTitle;
        public int expectedGoal;
        public int actualValue;
        public String observation;

        public DayEntry(String dayName, String activityTitle, int expectedGoal, int actualValue, String observation) {
            this.dayName = dayName;
            this.activityTitle = activityTitle != null ? activityTitle : "Actividades de producción";
            this.expectedGoal = Math.max(0, expectedGoal);
            this.actualValue = Math.max(0, actualValue);
            this.observation = observation != null ? observation : "";
        }

        public int getPercentage() {
            if (expectedGoal <= 0) return 100;
            return Math.min(100, Math.round((actualValue * 100.0f) / expectedGoal));
        }
    }

    public static class ReportData {
        public String cargo;
        public String collaborator;
        public boolean isAllMode;
        public String areaName;
        public String supervisorName;
        public String immediateBoss;
        public String generalObservation;
        public List<DayEntry> days = new ArrayList<>();

        public int getTotalExpected() {
            int sum = 0;
            for (DayEntry d : days) sum += d.expectedGoal;
            return sum;
        }

        public int getTotalActual() {
            int sum = 0;
            for (DayEntry d : days) sum += d.actualValue;
            return sum;
        }

        public int getOverallPercentage() {
            int exp = getTotalExpected();
            if (exp <= 0) return 100;
            return Math.min(100, Math.round((getTotalActual() * 100.0f) / exp));
        }

        public String getKpiLevelName() {
            int pct = getOverallPercentage();
            if (pct >= 90) return "EXCELENTE";
            if (pct >= 80) return "MUY BUENO";
            if (pct >= 60) return "BUENO";
            if (pct >= 40) return "REGULAR";
            return "DEFICIENTE";
        }

        public int getKpiLevelColor() {
            int pct = getOverallPercentage();
            if (pct >= 90) return 0xFF10B981; // Verde
            if (pct >= 80) return 0xFF3B82F6; // Azul
            if (pct >= 60) return 0xFFF59E0B; // Ámbar
            if (pct >= 40) return 0xFFF97316; // Naranja
            return 0xFFEF4444; // Rojo
        }
    }

    private static volatile KpiEditorStorageManager instance;
    private final SharedPreferences prefs;
    private final Context appContext;

    private KpiEditorStorageManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized KpiEditorStorageManager getInstance(Context context) {
        if (instance == null) {
            instance = new KpiEditorStorageManager(context);
        }
        return instance;
    }

    /**
     * Obtiene el reporte para el cargo y colaborador especificados.
     * Si collaborator es nulo, vacío o "Todos", retorna el modo global del cargo.
     */
    public synchronized ReportData getReport(String cargo, String collaborator) {
        String cleanCargo = cargo != null ? cargo.trim() : "Diseñador Gráfico";
        boolean isAll = collaborator == null || collaborator.trim().isEmpty() || collaborator.equalsIgnoreCase("Todos") || collaborator.toLowerCase(Locale.ROOT).contains("todos");
        String cleanCollab = isAll ? "Todos" : collaborator.trim();

        ReportData report = new ReportData();
        report.cargo = cleanCargo;
        report.collaborator = cleanCollab;
        report.isAllMode = isAll;

        resolveMetadata(cleanCargo, cleanCollab, report);
        loadDaysData(cleanCargo, cleanCollab, isAll, report);

        return report;
    }

    private void resolveMetadata(String cargo, String collab, ReportData report) {
        String upper = cargo.toUpperCase(Locale.ROOT);
        if (upper.contains("COMMUNITY")) {
            report.areaName = "MARKETING DIGITAL";
            report.supervisorName = "MANUEL CASTILLO";
            report.immediateBoss = "JEFFERSON CUEVA";
            report.generalObservation = "Se cumplió con la gestión de redes sociales, la publicación de contenidos, la atención de interacciones y la entrega del reporte semanal.";
        } else if (upper.contains("DISEN") || upper.contains("GRAFIC")) {
            report.areaName = "DISEÑO Y MULTIMEDIA";
            report.supervisorName = "DIRECTOR CREATIVO";
            report.immediateBoss = "GERENCIA GENERAL";
            report.generalObservation = "Entrega de artes finales, piezas gráficas comerciales y adaptación de formatos publicitarios según cronograma.";
        } else if (upper.contains("CONTADOR") || upper.contains("CONTAB")) {
            report.areaName = "CONTABILIDAD Y FINANZAS";
            report.supervisorName = "GERENCIA GENERAL";
            report.immediateBoss = "DIRECTORIO";
            report.generalObservation = "Control tributario, conciliaciones bancarias y emisión de estados financieros en los plazos previstos.";
        } else if (upper.contains("VIDE")) {
            report.areaName = "PRODUCCIÓN AUDIOVISUAL";
            report.supervisorName = "DIRECTOR AUDIOVISUAL";
            report.immediateBoss = "GERENCIA GENERAL";
            report.generalObservation = "Producción de material audiovisual, grabación de piezas comerciales y edición de reels semanales.";
        } else {
            report.areaName = "WEBSITE+ SIGD";
            report.supervisorName = "SUPERVISOR DE ÁREA";
            report.immediateBoss = "GERENCIA GENERAL";
            report.generalObservation = "Cumplimiento del plan de trabajo operativo semanal de acuerdo a los estándares del área.";
        }
    }

    private void loadDaysData(String cargo, String collab, boolean isAll, ReportData report) {
        String key = isAll ? (PREF_GLOBAL_GOALS + normalizeKey(cargo))
                : (PREF_INDIVIDUAL_RECORDS + normalizeKey(cargo) + "_" + normalizeKey(collab));

        String rawJson = prefs.getString(key, null);
        if (rawJson != null) {
            try {
                JSONObject root = new JSONObject(rawJson);
                if (root.has("generalObservation")) {
                    report.generalObservation = root.optString("generalObservation", report.generalObservation);
                }
                JSONArray arr = root.optJSONArray("days");
                if (arr != null && arr.length() == 6) {
                    report.days.clear();
                    for (int i = 0; i < 6; i++) {
                        JSONObject o = arr.getJSONObject(i);
                        report.days.add(new DayEntry(
                                o.optString("dayName", DAYS[i]),
                                o.optString("activityTitle", getDefaultActivityTitle(cargo, i)),
                                o.optInt("expectedGoal", getDefaultExpected(cargo, i)),
                                o.optInt("actualValue", o.optInt("expectedGoal", getDefaultExpected(cargo, i))),
                                o.optString("observation", "Actividad completada conformemente.")
                        ));
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        // Si no hay datos guardados individuales, cargar los defaults o heredar del global de cargo
        int[] globalExpected = loadGlobalExpectedArray(cargo);
        report.days.clear();
        for (int i = 0; i < 6; i++) {
            int exp = globalExpected[i];
            int act = isAll ? exp : (int) Math.round(exp * 0.98); // 98% a 100% por defecto
            report.days.add(new DayEntry(
                    DAYS[i],
                    getDefaultActivityTitle(cargo, i),
                    exp,
                    act,
                    "Registro conforme de actividades diarias."
            ));
        }
    }

    private int[] loadGlobalExpectedArray(String cargo) {
        String key = PREF_GLOBAL_GOALS + normalizeKey(cargo);
        String rawJson = prefs.getString(key, null);
        int[] result = new int[6];
        for (int i = 0; i < 6; i++) {
            result[i] = getDefaultExpected(cargo, i);
        }
        if (rawJson != null) {
            try {
                JSONObject root = new JSONObject(rawJson);
                JSONArray arr = root.optJSONArray("days");
                if (arr != null && arr.length() == 6) {
                    for (int i = 0; i < 6; i++) {
                        result[i] = arr.getJSONObject(i).optInt("expectedGoal", result[i]);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * Guarda los cambios efectuados en el reporte.
     * Si isAllMode es true, actualiza las metas base de todo el cargo.
     */
    public synchronized void saveReport(ReportData report) {
        if (report == null || report.cargo == null) return;
        boolean isAll = report.isAllMode;
        String key = isAll ? (PREF_GLOBAL_GOALS + normalizeKey(report.cargo))
                : (PREF_INDIVIDUAL_RECORDS + normalizeKey(report.cargo) + "_" + normalizeKey(report.collaborator));

        try {
            JSONObject root = new JSONObject();
            root.put("cargo", report.cargo);
            root.put("collaborator", report.collaborator);
            root.put("generalObservation", report.generalObservation);

            JSONArray arr = new JSONArray();
            for (DayEntry d : report.days) {
                JSONObject o = new JSONObject();
                o.put("dayName", d.dayName);
                o.put("activityTitle", d.activityTitle);
                o.put("expectedGoal", d.expectedGoal);
                o.put("actualValue", d.actualValue);
                o.put("observation", d.observation);
                arr.put(o);
            }
            root.put("days", arr);

            prefs.edit().putString(key, root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized void updateDayExpected(ReportData report, int dayIndex, int newExpected) {
        if (report == null || dayIndex < 0 || dayIndex >= report.days.size()) return;
        DayEntry d = report.days.get(dayIndex);
        d.expectedGoal = Math.max(0, newExpected);
        saveReport(report);
    }

    public synchronized void updateDayActual(ReportData report, int dayIndex, int newActual) {
        if (report == null || dayIndex < 0 || dayIndex >= report.days.size()) return;
        DayEntry d = report.days.get(dayIndex);
        d.actualValue = Math.max(0, newActual);
        saveReport(report);
    }

    public synchronized void updateDayObservation(ReportData report, int dayIndex, String observation) {
        if (report == null || dayIndex < 0 || dayIndex >= report.days.size()) return;
        DayEntry d = report.days.get(dayIndex);
        d.observation = observation != null ? observation.trim() : "";
        saveReport(report);
    }

    public synchronized void updateGeneralObservation(ReportData report, String observation) {
        if (report == null) return;
        report.generalObservation = observation != null ? observation.trim() : "";
        saveReport(report);
    }

    private String getDefaultActivityTitle(String cargo, int dayIndex) {
        String upper = cargo.toUpperCase(Locale.ROOT);
        if (upper.contains("COMMUNITY")) {
            return KpiCommunityStorageManager.DEFAULT_ACTIVITIES[dayIndex].replace("\n", " - ");
        } else if (upper.contains("DISEN") || upper.contains("GRAFIC")) {
            String[] titles = {
                    "Diseño de piezas publicitarias principales",
                    "Adaptación de formatos y artes para redes",
                    "Retoque fotográfico y branding comercial",
                    "Diseño de catálogos y presentaciones",
                    "Control de calidad gráfica y revisiones",
                    "Organización de entregables y reporte semanal"
            };
            return titles[dayIndex];
        } else if (upper.contains("CONTADOR")) {
            String[] titles = {
                    "Registro de comprobantes y compras",
                    "Conciliaciones bancarias y arqueos",
                    "Cálculo de impuestos y provisiones",
                    "Emisión y validación de facturas electrónicas",
                    "Revisión de estados financieros preliminares",
                    "Reporte y archivo contable semanal"
            };
            return titles[dayIndex];
        } else {
            String[] titles = {
                    "Planificación y gestión de actividades",
                    "Ejecución de tareas operativas clave",
                    "Seguimiento y control de avance",
                    "Resolución de requerimientos técnicos",
                    "Revisión de calidad y verificación",
                    "Cierre semanal y consolidado de resultados"
            };
            return titles[dayIndex];
        }
    }

    private int getDefaultExpected(String cargo, int dayIndex) {
        String upper = cargo.toUpperCase(Locale.ROOT);
        if (upper.contains("COMMUNITY")) {
            return KpiCommunityStorageManager.DEFAULT_DAILY_EXPECTED_TOTALS[dayIndex]; // 11, 14, 13, 12, 9, 6
        } else if (upper.contains("DISEN") || upper.contains("GRAFIC")) {
            int[] exp = {4, 5, 4, 4, 3, 2};
            return exp[dayIndex];
        } else if (upper.contains("CONTADOR")) {
            int[] exp = {8, 10, 8, 8, 6, 4};
            return exp[dayIndex];
        } else {
            int[] exp = {5, 6, 5, 5, 4, 3};
            return exp[dayIndex];
        }
    }

    private String normalizeKey(String input) {
        if (input == null) return "general";
        return input.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
    }
}

