package pe.net.ws.kpiweb.features.kpi.manager.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Modelo diario para el cargo de GERENTE GENERAL (SIGD 2026).
 * Encapsula los dos bloques diarios de dirección:
 * 1. Control Diario de Gestión (Propia): Indicador/Actividad principal, Meta del día,
 *    Seguimiento realizado, Cantidad numérica clave, Resultado (%) y Observaciones.
 * 2. Supervisión de las Áreas de la Empresa: Área supervisada, Cumplimiento (%),
 *    Principales observaciones y Acciones realizadas.
 */
public class KpiGeneralManagerDayRecord implements Serializable {

    private String date;          // Ej. "04/09/2026"
    private String dayName;       // "Lunes", "Martes", etc.
    private String dayShort;      // "LUN", "MAR", etc.

    // Bloque 1: Control diario de gestión (propia)
    private String mainIndicator = "";
    private String dailyGoalText = "";
    private String trackingPerformed = "";
    private int priorityCount = 0;      // Ej. Lunes: 3 indicadores revisados, Martes: 2 asuntos resueltos
    private int ownCompliancePct = 0; // 0 a 100%; nunca asumir cumplimiento
    private String ownObservations = "";

    // Bloque 2: Supervisión de las áreas de la empresa
    private String supervisedArea = "";
    private int areaCompliancePct = 0; // 0 a 100%; nunca asumir cumplimiento
    private String areaObservations = "";
    private String areaActions = "";

    // Estados de control y firma biométrica
    private boolean isLocked = false;
    private boolean isRegistered = false;

    public KpiGeneralManagerDayRecord() {
    }

    public KpiGeneralManagerDayRecord(String date, String dayName, String dayShort,
                                      String mainIndicator, String dailyGoalText,
                                      String supervisedArea) {
        this.date = date;
        this.dayName = dayName;
        this.dayShort = dayShort;
        this.mainIndicator = mainIndicator;
        this.dailyGoalText = dailyGoalText;
        this.supervisedArea = supervisedArea;
    }

    public String getDate() {
        return date != null ? date : "";
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayName() {
        return dayName != null ? dayName : "";
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public String getDayShort() {
        return dayShort != null ? dayShort : "";
    }

    public void setDayShort(String dayShort) {
        this.dayShort = dayShort;
    }

    public String getMainIndicator() {
        return mainIndicator != null ? mainIndicator : "";
    }

    public void setMainIndicator(String mainIndicator) {
        this.mainIndicator = mainIndicator;
    }

    public String getDailyGoalText() {
        return dailyGoalText != null ? dailyGoalText : "";
    }

    public void setDailyGoalText(String dailyGoalText) {
        this.dailyGoalText = dailyGoalText;
    }

    public String getTrackingPerformed() {
        return trackingPerformed != null ? trackingPerformed : "";
    }

    public void setTrackingPerformed(String trackingPerformed) {
        this.trackingPerformed = trackingPerformed;
    }

    public int getPriorityCount() {
        return priorityCount;
    }

    public void setPriorityCount(int priorityCount) {
        this.priorityCount = Math.max(0, priorityCount);
    }

    public int getOwnCompliancePct() {
        return ownCompliancePct;
    }

    public void setOwnCompliancePct(int ownCompliancePct) {
        this.ownCompliancePct = Math.max(0, Math.min(100, ownCompliancePct));
    }

    public String getOwnObservations() {
        return ownObservations != null ? ownObservations : "";
    }

    public void setOwnObservations(String ownObservations) {
        this.ownObservations = ownObservations;
    }

    public String getSupervisedArea() {
        return supervisedArea != null ? supervisedArea : "";
    }

    public void setSupervisedArea(String supervisedArea) {
        this.supervisedArea = supervisedArea;
    }

    public int getAreaCompliancePct() {
        return areaCompliancePct;
    }

    public void setAreaCompliancePct(int areaCompliancePct) {
        this.areaCompliancePct = Math.max(0, Math.min(100, areaCompliancePct));
    }

    public String getAreaObservations() {
        return areaObservations != null ? areaObservations : "";
    }

    public void setAreaObservations(String areaObservations) {
        this.areaObservations = areaObservations;
    }

    public String getAreaActions() {
        return areaActions != null ? areaActions : "";
    }

    public void setAreaActions(String areaActions) {
        this.areaActions = areaActions;
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

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("date", getDate());
            json.put("dayName", getDayName());
            json.put("dayShort", getDayShort());
            json.put("mainIndicator", getMainIndicator());
            json.put("dailyGoalText", getDailyGoalText());
            json.put("trackingPerformed", getTrackingPerformed());
            json.put("priorityCount", priorityCount);
            json.put("ownCompliancePct", ownCompliancePct);
            json.put("ownObservations", getOwnObservations());
            json.put("supervisedArea", getSupervisedArea());
            json.put("areaCompliancePct", areaCompliancePct);
            json.put("areaObservations", getAreaObservations());
            json.put("areaActions", getAreaActions());
            json.put("isLocked", isLocked);
            json.put("isRegistered", isRegistered);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiGeneralManagerDayRecord fromJson(JSONObject json) {
        if (json == null) return null;
        KpiGeneralManagerDayRecord rec = new KpiGeneralManagerDayRecord();
        rec.setDate(json.optString("date", ""));
        rec.setDayName(json.optString("dayName", ""));
        rec.setDayShort(json.optString("dayShort", ""));
        rec.setMainIndicator(json.optString("mainIndicator", ""));
        rec.setDailyGoalText(json.optString("dailyGoalText", ""));
        rec.setTrackingPerformed(json.optString("trackingPerformed", ""));
        rec.setPriorityCount(json.optInt("priorityCount", 0));
        rec.setOwnCompliancePct(json.optInt("ownCompliancePct", 0));
        rec.setOwnObservations(json.optString("ownObservations", ""));
        rec.setSupervisedArea(json.optString("supervisedArea", ""));
        rec.setAreaCompliancePct(json.optInt("areaCompliancePct", 0));
        rec.setAreaObservations(json.optString("areaObservations", ""));
        rec.setAreaActions(json.optString("areaActions", ""));
        rec.setLocked(json.optBoolean("isLocked", false));
        rec.setRegistered(json.optBoolean("isRegistered", false));
        return rec;
    }
}

