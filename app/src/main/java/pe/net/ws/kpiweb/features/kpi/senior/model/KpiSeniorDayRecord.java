package pe.net.ws.kpiweb.features.kpi.senior.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Modelo que encapsula los dos bloques del formulario diario del Diseñador Gráfico Senior:
 * 1. Mi producción diaria (Diseños, Videos, Sesiones, Otras, Descripción).
 * 2. Supervisión de mi equipo de diseño (Senior supervisados, Junior supervisados, % Cumplimiento, Observaciones, Acciones).
 */
public class KpiSeniorDayRecord implements Serializable {

    private String date; // Formato "DD/MM/YYYY" ej. "07/09/2026"
    private String dayName; // "Lunes", "Martes", etc.
    private String dayShort; // "Lun", "Mar", etc.

    // Bloque 1: Mi producción diaria
    private int designsCount = 0;
    private int videosCount = 0;
    private int photoSessionsCount = 0;
    private int otherActivitiesCount = 0;
    private String activitiesDescription = "";

    // Bloque 2: Supervisión de mi equipo de diseño
    private int supervisedSeniorCount = 0;
    private int supervisedJuniorCount = 0;
    private int teamCompliancePercentage = 0; // 0 a 100
    private String supervisionObservations = "";
    private String supervisionActions = "";

    // Estados
    private boolean isLocked = false;
    private boolean isRegistered = false;
    private int dailyGoal = 4; // Meta sugerida del día para calcular porcentaje diario

    public KpiSeniorDayRecord() {
    }

    public KpiSeniorDayRecord(String date, String dayName, String dayShort) {
        this.date = date;
        this.dayName = dayName;
        this.dayShort = dayShort;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public String getDayShort() {
        return dayShort;
    }

    public void setDayShort(String dayShort) {
        this.dayShort = dayShort;
    }

    public int getDesignsCount() {
        return designsCount;
    }

    public void setDesignsCount(int designsCount) {
        this.designsCount = Math.max(0, designsCount);
    }

    public int getVideosCount() {
        return videosCount;
    }

    public void setVideosCount(int videosCount) {
        this.videosCount = Math.max(0, videosCount);
    }

    public int getPhotoSessionsCount() {
        return photoSessionsCount;
    }

    public void setPhotoSessionsCount(int photoSessionsCount) {
        this.photoSessionsCount = Math.max(0, photoSessionsCount);
    }

    public int getOtherActivitiesCount() {
        return otherActivitiesCount;
    }

    public void setOtherActivitiesCount(int otherActivitiesCount) {
        this.otherActivitiesCount = Math.max(0, otherActivitiesCount);
    }

    public String getActivitiesDescription() {
        return activitiesDescription != null ? activitiesDescription : "";
    }

    public void setActivitiesDescription(String activitiesDescription) {
        this.activitiesDescription = activitiesDescription;
    }

    public int getSupervisedSeniorCount() {
        return supervisedSeniorCount;
    }

    public void setSupervisedSeniorCount(int supervisedSeniorCount) {
        this.supervisedSeniorCount = Math.max(0, supervisedSeniorCount);
    }

    public int getSupervisedJuniorCount() {
        return supervisedJuniorCount;
    }

    public void setSupervisedJuniorCount(int supervisedJuniorCount) {
        this.supervisedJuniorCount = Math.max(0, supervisedJuniorCount);
    }

    public int getTeamCompliancePercentage() {
        return teamCompliancePercentage;
    }

    public void setTeamCompliancePercentage(int teamCompliancePercentage) {
        this.teamCompliancePercentage = Math.max(0, Math.min(100, teamCompliancePercentage));
    }

    public String getSupervisionObservations() {
        return supervisionObservations != null ? supervisionObservations : "";
    }

    public void setSupervisionObservations(String supervisionObservations) {
        this.supervisionObservations = supervisionObservations;
    }

    public String getSupervisionActions() {
        return supervisionActions != null ? supervisionActions : "";
    }

    public void setSupervisionActions(String supervisionActions) {
        this.supervisionActions = supervisionActions;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }

    public void setDailyGoal(int dailyGoal) {
        this.dailyGoal = dailyGoal;
    }

    /**
     * Calcula la producción total del día sumando los 4 tipos de actividades.
     */
    public int getTotalProduction() {
        return designsCount + videosCount + photoSessionsCount + otherActivitiesCount;
    }

    /**
     * Retorna el porcentaje de cumplimiento del día según su meta diaria.
     */
    public int getDailyCompliancePercentage() {
        if (dailyGoal <= 0) return 0;
        int pct = Math.round((getTotalProduction() * 100f) / dailyGoal);
        return Math.min(100, pct);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("date", date);
            json.put("dayName", dayName);
            json.put("dayShort", dayShort);
            json.put("designsCount", designsCount);
            json.put("videosCount", videosCount);
            json.put("photoSessionsCount", photoSessionsCount);
            json.put("otherActivitiesCount", otherActivitiesCount);
            json.put("activitiesDescription", activitiesDescription);
            json.put("supervisedSeniorCount", supervisedSeniorCount);
            json.put("supervisedJuniorCount", supervisedJuniorCount);
            json.put("teamCompliancePercentage", teamCompliancePercentage);
            json.put("supervisionObservations", supervisionObservations);
            json.put("supervisionActions", supervisionActions);
            json.put("isLocked", isLocked);
            json.put("isRegistered", isRegistered);
            json.put("dailyGoal", dailyGoal);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiSeniorDayRecord fromJson(JSONObject json) {
        if (json == null) return null;
        KpiSeniorDayRecord record = new KpiSeniorDayRecord();
        record.date = json.optString("date", "");
        record.dayName = json.optString("dayName", "");
        record.dayShort = json.optString("dayShort", "");
        record.designsCount = json.optInt("designsCount", 0);
        record.videosCount = json.optInt("videosCount", 0);
        record.photoSessionsCount = json.optInt("photoSessionsCount", 0);
        record.otherActivitiesCount = json.optInt("otherActivitiesCount", 0);
        record.activitiesDescription = json.optString("activitiesDescription", "");
        record.supervisedSeniorCount = json.optInt("supervisedSeniorCount", 0);
        record.supervisedJuniorCount = json.optInt("supervisedJuniorCount", 0);
        record.teamCompliancePercentage = json.optInt("teamCompliancePercentage", 0);
        record.supervisionObservations = json.optString("supervisionObservations", "");
        record.supervisionActions = json.optString("supervisionActions", "");
        record.isLocked = json.optBoolean("isLocked", false);
        record.isRegistered = json.optBoolean("isRegistered", false);
        record.dailyGoal = json.optInt("dailyGoal", 4);
        return record;
    }
}
