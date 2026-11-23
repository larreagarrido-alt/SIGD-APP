package pe.net.ws.kpiweb.features.kpi.senior.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte de una semana de trabajo del Diseñador Gráfico Senior (Lunes a Sábado).
 * Provee cálculos acumulados automáticos, promedio de supervisión de equipo y los 6 indicadores clave.
 */
public class KpiSeniorWeekReport implements Serializable {

    private String weekId; // Ej. "Semana N.º 37"
    private int weekNumber; // 37
    private String dateRange; // "Del 07/09/2026 al 12/09/2026"
    private String status = "En progreso"; // "Completada" o "En progreso"
    private int weeklyGoal = 24; // Meta oficial de producción semanal (10 post + 12 videos + 2 fotos = 24)
    private int goalDesigns = 10;
    private int goalVideos = 12;
    private int goalPhotoSessions = 2;

    private String collaboratorName = "";
    private String collaboratorRole = "Diseñador Gráfico Senior (Jefatura Área Creativa)";
    private String supervisorName = "Licenciado / Dirección";
    private String areaName = "Team Creativo";
    private String immediateBoss = "Gerencia General";
    private String shift = "Mañana (08:00 a.m. - 01:30 p.m.)";

    private String achievements = "• Todo una producción (68.15%)\n• 100% entrega en sesiones de producto.";
    private String difficulties = "• Ajustes adicionales de postproducción en videos largos.";
    private String improvementPlan = "• Optimización de tiempos en renderizado y estandarización de briefs.";

    private List<KpiSeniorDayRecord> days = new ArrayList<>();

    public KpiSeniorWeekReport() {
    }

    public KpiSeniorWeekReport(String weekId, int weekNumber, String dateRange, int weeklyGoal) {
        this.weekId = weekId;
        this.weekNumber = weekNumber;
        this.dateRange = dateRange;
        this.weeklyGoal = weeklyGoal;
    }

    public String getWeekId() {
        return weekId;
    }

    public void setWeekId(String weekId) {
        this.weekId = weekId;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getStatus() {
        // Si los 6 días están registrados, marcar como Completada
        if (getRegisteredDaysCount() >= 6) {
            return "Completada";
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getWeeklyGoal() {
        return weeklyGoal;
    }

    public void setWeeklyGoal(int weeklyGoal) {
        this.weeklyGoal = weeklyGoal;
    }

    public int getGoalDesigns() {
        return goalDesigns;
    }

    public void setGoalDesigns(int goalDesigns) {
        this.goalDesigns = goalDesigns;
    }

    public int getGoalVideos() {
        return goalVideos;
    }

    public void setGoalVideos(int goalVideos) {
        this.goalVideos = goalVideos;
    }

    public int getGoalPhotoSessions() {
        return goalPhotoSessions;
    }

    public void setGoalPhotoSessions(int goalPhotoSessions) {
        this.goalPhotoSessions = goalPhotoSessions;
    }

    public String getCollaboratorName() {
        return collaboratorName;
    }

    public void setCollaboratorName(String collaboratorName) {
        this.collaboratorName = collaboratorName;
    }

    public String getCollaboratorRole() {
        return collaboratorRole;
    }

    public void setCollaboratorRole(String collaboratorRole) {
        this.collaboratorRole = collaboratorRole;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getImmediateBoss() {
        return immediateBoss;
    }

    public void setImmediateBoss(String immediateBoss) {
        this.immediateBoss = immediateBoss;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getAchievements() {
        return achievements;
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    public String getDifficulties() {
        return difficulties;
    }

    public void setDifficulties(String difficulties) {
        this.difficulties = difficulties;
    }

    public String getImprovementPlan() {
        return improvementPlan;
    }

    public void setImprovementPlan(String improvementPlan) {
        this.improvementPlan = improvementPlan;
    }

    public List<KpiSeniorDayRecord> getDays() {
        return days;
    }

    public void setDays(List<KpiSeniorDayRecord> days) {
        this.days = days;
    }

    public int getTotalDesigns() {
        int sum = 0;
        for (KpiSeniorDayRecord d : days) {
            if (d.isRegistered()) sum += d.getDesignsCount();
        }
        return sum;
    }

    public int getTotalVideos() {
        int sum = 0;
        for (KpiSeniorDayRecord d : days) {
            if (d.isRegistered()) sum += d.getVideosCount();
        }
        return sum;
    }

    public int getTotalPhotoSessions() {
        int sum = 0;
        for (KpiSeniorDayRecord d : days) {
            if (d.isRegistered()) sum += d.getPhotoSessionsCount();
        }
        return sum;
    }

    public int getTotalOtherActivities() {
        int sum = 0;
        for (KpiSeniorDayRecord d : days) {
            if (d.isRegistered()) sum += d.getOtherActivitiesCount();
        }
        return sum;
    }

    public int getTotalProduction() {
        return getTotalDesigns() + getTotalVideos() + getTotalPhotoSessions() + getTotalOtherActivities();
    }

    public float getCompliancePercentage() {
        if (weeklyGoal <= 0) return 0f;
        return (getTotalProduction() * 100f) / weeklyGoal;
    }

    public int getPendingProduction() {
        return Math.max(0, weeklyGoal - getTotalProduction());
    }

    public int getRegisteredDaysCount() {
        int count = 0;
        for (KpiSeniorDayRecord d : days) {
            if (d.isRegistered()) count++;
        }
        return count;
    }

    /**
     * Calcula la media aritmética de los porcentajes de cumplimiento de los días efectivamente registrados.
     * Los días sin registro NO se incluyen como ceros.
     */
    public float getAverageSupervisionCompliance() {
        int registeredCount = 0;
        float totalPercentage = 0f;
        for (KpiSeniorDayRecord d : days) {
            if (d.isRegistered()) {
                registeredCount++;
                totalPercentage += d.getTeamCompliancePercentage();
            }
        }
        if (registeredCount == 0) return 0f;
        return totalPercentage / registeredCount;
    }

    /**
     * Genera los 6 indicadores clave del cargo con sus pesos normativos.
     */
    public List<KpiSeniorIndicator> getIndicators() {
        List<KpiSeniorIndicator> list = new ArrayList<>();
        // 1. Calidad de producción (25%)
        list.add(new KpiSeniorIndicator("Calidad de producción", 25, 100.0f, false));

        // 2. Productividad (25%): basado en el porcentaje de cumplimiento sobre la meta
        float prodComp = Math.min(100f, getCompliancePercentage());
        list.add(new KpiSeniorIndicator("Productividad", 25, prodComp, false));

        // 3. Supervisión del equipo (20%): basado en el promedio de supervisión de equipo
        float supComp = getAverageSupervisionCompliance();
        list.add(new KpiSeniorIndicator("Supervisión del equipo", 20, supComp > 0 ? supComp : 90.0f, false));

        // 4. Cumplimiento de entregas (15%)
        list.add(new KpiSeniorIndicator("Cumplimiento de entregas", 15, 70.0f, false));

        // 5. Cero errores ortográficos (10%)
        list.add(new KpiSeniorIndicator("Cero errores ortográficos", 10, 90.0f, false));

        // 6. Organización y liderazgo (5%)
        list.add(new KpiSeniorIndicator("Organización y liderazgo", 5, 100.0f, false));

        return list;
    }

    /**
     * Calcula el promedio ponderado final de todos los indicadores si todos están definidos.
     */
    public float getGeneralWeightedScore() {
        List<KpiSeniorIndicator> indicators = getIndicators();
        float total = 0f;
        for (KpiSeniorIndicator ind : indicators) {
            if (ind.isPending()) return -1f; // Si hay alguno pendiente no se muestra el total
            total += ind.getWeightedValue();
        }
        return total;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("weekId", weekId);
            json.put("weekNumber", weekNumber);
            json.put("dateRange", dateRange);
            json.put("status", status);
            json.put("weeklyGoal", weeklyGoal);
            json.put("goalDesigns", goalDesigns);
            json.put("goalVideos", goalVideos);
            json.put("goalPhotoSessions", goalPhotoSessions);
            json.put("collaboratorName", collaboratorName);
            json.put("collaboratorRole", collaboratorRole);
            json.put("supervisorName", supervisorName);
            json.put("areaName", areaName);
            json.put("immediateBoss", immediateBoss);
            json.put("shift", shift);
            json.put("achievements", achievements);
            json.put("difficulties", difficulties);
            json.put("improvementPlan", improvementPlan);
            JSONArray daysArray = new JSONArray();
            for (KpiSeniorDayRecord d : days) {
                daysArray.put(d.toJson());
            }
            json.put("days", daysArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiSeniorWeekReport fromJson(JSONObject json) {
        if (json == null) return null;
        KpiSeniorWeekReport report = new KpiSeniorWeekReport();
        report.weekId = json.optString("weekId", "");
        report.weekNumber = json.optInt("weekNumber", 0);
        report.dateRange = json.optString("dateRange", "");
        report.status = json.optString("status", "En progreso");
        report.weeklyGoal = json.optInt("weeklyGoal", 24);
        report.goalDesigns = json.optInt("goalDesigns", 10);
        report.goalVideos = json.optInt("goalVideos", 12);
        report.goalPhotoSessions = json.optInt("goalPhotoSessions", 2);
        report.collaboratorName = json.optString("collaboratorName", "");
        if (report.collaboratorName.isEmpty()) {
            report.collaboratorName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        }
        report.collaboratorRole = json.optString("collaboratorRole", "Diseñador Gráfico Senior (Jefatura Área Creativa)");
        report.supervisorName = json.optString("supervisorName", "Licenciado / Dirección");
        report.areaName = json.optString("areaName", "Team Creativo");
        report.immediateBoss = json.optString("immediateBoss", "Gerencia General");
        report.shift = json.optString("shift", "Mañana (08:00 a.m. - 01:30 p.m.)");
        report.achievements = json.optString("achievements", "• Todo una producción (68.15%)\n• 100% entrega en sesiones de producto.");
        report.difficulties = json.optString("difficulties", "• Ajustes adicionales de postproducción en videos largos.");
        report.improvementPlan = json.optString("improvementPlan", "• Optimización de tiempos en renderizado y estandarización de briefs.");
        JSONArray daysArray = json.optJSONArray("days");
        if (daysArray != null) {
            for (int i = 0; i < daysArray.length(); i++) {
                JSONObject dayJson = daysArray.optJSONObject(i);
                if (dayJson != null) {
                    report.days.add(KpiSeniorDayRecord.fromJson(dayJson));
                }
            }
        }
        return report;
    }
}
