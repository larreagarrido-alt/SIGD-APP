package pe.net.ws.kpiweb.features.kpi.designer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modelo de reporte semanal completo para el Diseñador Gráfico.
 * Contiene las 5 secciones oficiales de la ficha:
 * 1. Control diario de producción (Lunes a Sábado)
 * 2. Resumen semanal de producción (Metas fijas: 18 diseños, 6 videos, 1 sesión = 25 total)
 * 3. Acciones que realiza el Diseñador Gráfico (9 casillas seleccionables, única sección editable)
 * 4. Evaluación KPI - Productividad (Excelente, Muy bueno, Bueno, Regular, Deficiente)
 * 5. Observaciones generales
 */
public class KpiDesignerWeekReport implements Serializable {

    public static final int WEEKLY_GOAL_DESIGNS = 18;
    public static final int WEEKLY_GOAL_VIDEOS = 6;
    public static final int WEEKLY_GOAL_PHOTOS = 1;
    public static final int WEEKLY_GOAL_TOTAL = 25;

    public static final String[] ACTION_TITLES = {
            "Producción de diseños gráficos (posts, banners, piezas digitales, etc.)",
            "Edición y montaje de videos.",
            "Diseño de piezas para campañas y redes sociales.",
            "Adaptación de piezas a diferentes formatos.",
            "Preparación de material gráfico para la web.",
            "Edición y retoque de fotografías (cuando corresponda).",
            "Organización y entrega de archivos finales.",
            "Cumplimiento de las metas diarias de producción.",
            "Otras actividades asignadas."
    };

    private String weekId; // "Semana 5"
    private int weekNumber; // 5
    private String dateRange; // "08/09/2026 - 13/09/2026"
    private String dateRangeFull; // "Del 08 al 13 de septiembre de 2026"
    private String status; // "Finalizado" o "En progreso"

    private List<KpiDesignerDayRecord> days = new ArrayList<>();

    // Totales calculados
    private int totalDesigns;
    private int totalVideos;
    private int totalPhotos;
    private int totalActivities;
    private int totalCompliancePercentage;
    private String kpiLevel; // "EXCELENTE", "MUY BUENO", "BUENO", "REGULAR", "DEFICIENTE"
    private String kpiLevelDescription;

    // Sección 3: Acciones (9 casillas)
    private boolean[] actionsPerformed = new boolean[9];
    private boolean actionsBiometricVerified;

    // Sección 5: Observaciones generales
    private String generalObservations;

    public KpiDesignerWeekReport() {
        // Inicializar acciones por defecto (las 8 primeras activadas según imagen)
        for (int i = 0; i < 8; i++) {
            actionsPerformed[i] = true;
        }
        actionsPerformed[8] = false;
    }

    public KpiDesignerWeekReport(String weekId, int weekNumber, String dateRange, String dateRangeFull, String status) {
        this();
        this.weekId = weekId;
        this.weekNumber = weekNumber;
        this.dateRange = dateRange;
        this.dateRangeFull = dateRangeFull;
        this.status = status;
    }

    /**
     * Recalcula automáticamente todas las métricas de la semana a partir de sus días.
     */
    public void recalculateMetrics() {
        int sumDesigns = 0;
        int sumVideos = 0;
        int sumPhotos = 0;

        for (KpiDesignerDayRecord d : days) {
            sumDesigns += d.getDesignsCompleted();
            sumVideos += d.getVideosCompleted();
            sumPhotos += d.getPhotosCompleted();
        }

        this.totalDesigns = sumDesigns;
        this.totalVideos = sumVideos;
        this.totalPhotos = sumPhotos;
        this.totalActivities = sumDesigns + sumVideos + sumPhotos;

        if (WEEKLY_GOAL_TOTAL > 0) {
            this.totalCompliancePercentage = (int) Math.round(((double) totalActivities / WEEKLY_GOAL_TOTAL) * 100.0);
        } else {
            this.totalCompliancePercentage = 0;
        }

        // Determinar Nivel KPI según escala oficial SIGD
        if (totalCompliancePercentage >= 90) {
            this.kpiLevel = "EXCELENTE";
            this.kpiLevelDescription = "Cumple y/o supera el objetivo.";
        } else if (totalCompliancePercentage >= 80) {
            this.kpiLevel = "MUY BUENO";
            this.kpiLevelDescription = "Cumple y está por llegar al objetivo.";
        } else if (totalCompliancePercentage >= 60) {
            this.kpiLevel = "BUENO";
            this.kpiLevelDescription = "Cumple con la mitad recomendada.";
        } else if (totalCompliancePercentage >= 40) {
            this.kpiLevel = "REGULAR";
            this.kpiLevelDescription = "Baja expectativa con la empresa.";
        } else {
            this.kpiLevel = "DEFICIENTE";
            this.kpiLevelDescription = "No cumple con las expectativas.";
        }
    }

    // Cálculos individuales de porcentaje
    public double getDesignsPercentage() {
        return Math.round(((double) totalDesigns / WEEKLY_GOAL_DESIGNS) * 10000.0) / 100.0;
    }

    public double getVideosPercentage() {
        return Math.round(((double) totalVideos / WEEKLY_GOAL_VIDEOS) * 10000.0) / 100.0;
    }

    public double getPhotosPercentage() {
        return Math.round(((double) totalPhotos / WEEKLY_GOAL_PHOTOS) * 10000.0) / 100.0;
    }

    // Getters and Setters
    public String getWeekId() { return weekId; }
    public void setWeekId(String weekId) { this.weekId = weekId; }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }

    public String getDateRangeFull() { return dateRangeFull; }
    public void setDateRangeFull(String dateRangeFull) { this.dateRangeFull = dateRangeFull; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<KpiDesignerDayRecord> getDays() { return days; }
    public void setDays(List<KpiDesignerDayRecord> days) {
        this.days = days;
        recalculateMetrics();
    }

    public int getTotalDesigns() { return totalDesigns; }
    public int getTotalVideos() { return totalVideos; }
    public int getTotalPhotos() { return totalPhotos; }
    public int getTotalActivities() { return totalActivities; }
    public int getTotalCompliancePercentage() { return totalCompliancePercentage; }
    public String getKpiLevel() { return kpiLevel; }
    public String getKpiLevelDescription() { return kpiLevelDescription; }

    public boolean[] getActionsPerformed() { return actionsPerformed; }
    public void setActionsPerformed(boolean[] actionsPerformed) { this.actionsPerformed = actionsPerformed; }

    public boolean isActionsBiometricVerified() { return actionsBiometricVerified; }
    public void setActionsBiometricVerified(boolean actionsBiometricVerified) { this.actionsBiometricVerified = actionsBiometricVerified; }

    public String getGeneralObservations() { return generalObservations; }
    public void setGeneralObservations(String generalObservations) { this.generalObservations = generalObservations; }

    public org.json.JSONObject toJson() {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("weekId", weekId);
            json.put("weekNumber", weekNumber);
            json.put("dateRange", dateRange);
            json.put("dateRangeFull", dateRangeFull);
            json.put("status", status);
            json.put("totalDesigns", totalDesigns);
            json.put("totalVideos", totalVideos);
            json.put("totalPhotos", totalPhotos);
            json.put("totalActivities", totalActivities);
            json.put("totalCompliancePercentage", totalCompliancePercentage);
            json.put("kpiLevel", kpiLevel);
            json.put("kpiLevelDescription", kpiLevelDescription);
            json.put("generalObservations", generalObservations);
            json.put("actionsBiometricVerified", actionsBiometricVerified);

            org.json.JSONArray daysArray = new org.json.JSONArray();
            if (days != null) {
                for (KpiDesignerDayRecord day : days) {
                    daysArray.put(day.toJson());
                }
            }
            json.put("days", daysArray);

            org.json.JSONArray actionsArray = new org.json.JSONArray();
            if (actionsPerformed != null) {
                for (boolean b : actionsPerformed) {
                    actionsArray.put(b);
                }
            }
            json.put("actionsPerformed", actionsArray);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiDesignerWeekReport fromJson(org.json.JSONObject json) {
        if (json == null) return null;
        KpiDesignerWeekReport w = new KpiDesignerWeekReport();
        w.setWeekId(json.optString("weekId", ""));
        w.setWeekNumber(json.optInt("weekNumber", 1));
        w.setDateRange(json.optString("dateRange", ""));
        w.setDateRangeFull(json.optString("dateRangeFull", ""));
        w.setStatus(json.optString("status", "En progreso"));
        w.setGeneralObservations(json.optString("generalObservations", ""));
        w.setActionsBiometricVerified(json.optBoolean("actionsBiometricVerified", false));

        org.json.JSONArray daysArray = json.optJSONArray("days");
        List<KpiDesignerDayRecord> list = new ArrayList<>();
        if (daysArray != null) {
            for (int i = 0; i < daysArray.length(); i++) {
                org.json.JSONObject dayObj = daysArray.optJSONObject(i);
                if (dayObj != null) {
                    KpiDesignerDayRecord d = KpiDesignerDayRecord.fromJson(dayObj);
                    if (d != null) list.add(d);
                }
            }
        }
        w.setDays(list);

        org.json.JSONArray actionsArray = json.optJSONArray("actionsPerformed");
        if (actionsArray != null) {
            boolean[] actions = new boolean[9];
            for (int i = 0; i < Math.min(actions.length, actionsArray.length()); i++) {
                actions[i] = actionsArray.optBoolean(i, false);
            }
            w.setActionsPerformed(actions);
        }
        return w;
    }
}
