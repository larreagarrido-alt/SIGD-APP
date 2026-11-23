package pe.net.ws.kpiweb.features.kpi.manager.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo del Reporte Semanal de Gestión del GERENTE GENERAL (SIGD 2026).
 * Consolida:
 * - Sección 1: Control diario de gestión (propia)
 * - Sección 2: Supervisión de las áreas de la empresa
 * - Sección 3: Resumen semanal de gestión (propia)
 * - Sección 4: Indicadores clave del cargo (KPI) con pesos (25%, 25%, 20%, 15%, 15%)
 * - Secciones 5, 6 y 7: Logros de la semana, Dificultades / Incidencias y Plan de mejora
 */
public class KpiGeneralManagerWeekReport implements Serializable {

    private String weekId;       // Ej. "Semana N.º 37"
    private int weekNumber;      // Ej. 37
    private String dateRange;    // Ej. "Del 04/09/2026 al 10/09/2026"
    private String status;       // "En curso" o "Enviado"

    private List<KpiGeneralManagerDayRecord> days = new ArrayList<>();

    // Cierre semanal ejecutivo (Secciones 5, 6, 7)
    private String achievements = "";
    private String difficulties = "";
    private String improvementPlan = "";

    // Totales semanales de resumen (Sección 3)
    private int weeklyIndicatorsReviewed = 18;
    private int weeklyIssuesResolved = 11;

    public KpiGeneralManagerWeekReport() {
    }

    public KpiGeneralManagerWeekReport(String weekId, int weekNumber, String dateRange) {
        this.weekId = weekId;
        this.weekNumber = weekNumber;
        this.dateRange = dateRange;
        this.status = "En curso";
    }

    public String getWeekId() {
        return weekId != null ? weekId : "";
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
        return dateRange != null ? dateRange : "";
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public String getStatus() {
        return status != null ? status : "En curso";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<KpiGeneralManagerDayRecord> getDays() {
        return days;
    }

    public void setDays(List<KpiGeneralManagerDayRecord> days) {
        this.days = days != null ? days : new ArrayList<>();
    }

    public String getAchievements() {
        return achievements != null ? achievements : "";
    }

    public void setAchievements(String achievements) {
        this.achievements = achievements;
    }

    public String getDifficulties() {
        return difficulties != null ? difficulties : "";
    }

    public void setDifficulties(String difficulties) {
        this.difficulties = difficulties;
    }

    public String getImprovementPlan() {
        return improvementPlan != null ? improvementPlan : "";
    }

    public void setImprovementPlan(String improvementPlan) {
        this.improvementPlan = improvementPlan;
    }

    public int getWeeklyIndicatorsReviewed() {
        return weeklyIndicatorsReviewed;
    }

    public void setWeeklyIndicatorsReviewed(int weeklyIndicatorsReviewed) {
        this.weeklyIndicatorsReviewed = Math.max(0, weeklyIndicatorsReviewed);
    }

    public int getWeeklyIssuesResolved() {
        return weeklyIssuesResolved;
    }

    public void setWeeklyIssuesResolved(int weeklyIssuesResolved) {
        this.weeklyIssuesResolved = Math.max(0, weeklyIssuesResolved);
    }

    /**
     * Cantidad de días efectivamente registrados en la semana (0 a 6).
     */
    public int getRegisteredDaysCount() {
        if (days == null) return 0;
        int count = 0;
        for (KpiGeneralManagerDayRecord d : days) {
            if (d.isRegistered() || d.isLocked()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Promedio semanal de cumplimiento de gestión propia (Sección 1).
     */
    public int getAverageOwnCompliance() {
        if (days == null || days.isEmpty()) return 0;
        int sum = 0;
        int count = 0;
        for (KpiGeneralManagerDayRecord d : days) {
            if (d.isRegistered() || d.isLocked()) {
                sum += d.getOwnCompliancePct();
                count++;
            }
        }
        return count > 0 ? Math.round((float) sum / count) : 0;
    }

    /**
     * Promedio semanal de supervisión de áreas (Sección 2).
     */
    public int getAverageAreaSupervisionCompliance() {
        if (days == null || days.isEmpty()) return 0;
        int sum = 0;
        int count = 0;
        for (KpiGeneralManagerDayRecord d : days) {
            if (d.isRegistered() || d.isLocked()) {
                sum += d.getAreaCompliancePct();
                count++;
            }
        }
        return count > 0 ? Math.round((float) sum / count) : 0;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("weekId", getWeekId());
            json.put("weekNumber", weekNumber);
            json.put("dateRange", getDateRange());
            json.put("status", getStatus());
            json.put("achievements", getAchievements());
            json.put("difficulties", getDifficulties());
            json.put("improvementPlan", getImprovementPlan());
            json.put("weeklyIndicatorsReviewed", weeklyIndicatorsReviewed);
            json.put("weeklyIssuesResolved", weeklyIssuesResolved);

            JSONArray daysArr = new JSONArray();
            if (days != null) {
                for (KpiGeneralManagerDayRecord d : days) {
                    daysArr.put(d.toJson());
                }
            }
            json.put("days", daysArr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiGeneralManagerWeekReport fromJson(JSONObject json) {
        if (json == null) return null;
        KpiGeneralManagerWeekReport report = new KpiGeneralManagerWeekReport(
                json.optString("weekId", "Semana"),
                json.optInt("weekNumber", 37),
                json.optString("dateRange", "")
        );
        report.setStatus(json.optString("status", "En curso"));
        report.setAchievements(json.optString("achievements", ""));
        report.setDifficulties(json.optString("difficulties", ""));
        report.setImprovementPlan(json.optString("improvementPlan", ""));
        report.setWeeklyIndicatorsReviewed(json.optInt("weeklyIndicatorsReviewed", 18));
        report.setWeeklyIssuesResolved(json.optInt("weeklyIssuesResolved", 11));

        List<KpiGeneralManagerDayRecord> dayList = new ArrayList<>();
        JSONArray arr = json.optJSONArray("days");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                KpiGeneralManagerDayRecord rec = KpiGeneralManagerDayRecord.fromJson(arr.optJSONObject(i));
                if (rec != null) {
                    dayList.add(rec);
                }
            }
        }
        report.setDays(dayList);
        return report;
    }
}

