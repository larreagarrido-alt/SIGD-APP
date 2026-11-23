package pe.net.ws.kpiweb.features.kpi.admin.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Registro de captura diaria de un colaborador para un KPI dinámico.
 * Congela la versión de la meta (kpiVersionUsed) para garantizar
 * que ediciones futuras de Gerencia no alteren los resultados históricos.
 */
public class KpiRecordEntry implements Serializable {

    private String recordId;
    private String kpiId;
    private int kpiVersionUsed;
    private String kpiName;
    private String role;
    private String employeeId;
    private String date; // YYYY-MM-DD o DD/MM/YYYY
    private double actualValue;
    private double targetValueUsed;
    private double weightUsed;
    private double computedCompliance;
    private double computedWeightedScore;
    private boolean biometricVerified;
    private String period = "Diaria";
    private String deliveryStatus = "Entregado"; // Entregado, Aprobado, Observado
    private String supervisorObservation = "";
    private boolean isFieldWork = false;
    private String fieldWorkReason = "";
    private String syncStatus = "PENDING"; // PENDING, SYNCED
    private long timestamp;

    public KpiRecordEntry() {}

    public KpiRecordEntry(String recordId, String kpiId, int kpiVersionUsed, String kpiName,
                          String role, String employeeId, String date, double actualValue,
                          double targetValueUsed, double weightUsed, double computedCompliance,
                          double computedWeightedScore, boolean biometricVerified) {
        this.recordId = recordId;
        this.kpiId = kpiId;
        this.kpiVersionUsed = kpiVersionUsed;
        this.kpiName = kpiName;
        this.role = role;
        this.employeeId = employeeId;
        this.date = date;
        this.actualValue = actualValue;
        this.targetValueUsed = targetValueUsed;
        this.weightUsed = weightUsed;
        this.computedCompliance = computedCompliance;
        this.computedWeightedScore = computedWeightedScore;
        this.biometricVerified = biometricVerified;
        this.period = "Diaria";
        this.deliveryStatus = "Entregado";
        this.supervisorObservation = "";
        this.isFieldWork = false;
        this.fieldWorkReason = "";
        this.syncStatus = "PENDING";
        this.timestamp = System.currentTimeMillis();
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getKpiId() { return kpiId; }
    public void setKpiId(String kpiId) { this.kpiId = kpiId; }

    public int getKpiVersionUsed() { return kpiVersionUsed; }
    public void setKpiVersionUsed(int kpiVersionUsed) { this.kpiVersionUsed = kpiVersionUsed; }

    public String getKpiName() { return kpiName; }
    public void setKpiName(String kpiName) { this.kpiName = kpiName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getActualValue() { return actualValue; }
    public void setActualValue(double actualValue) { this.actualValue = actualValue; }

    public double getTargetValueUsed() { return targetValueUsed; }
    public void setTargetValueUsed(double targetValueUsed) { this.targetValueUsed = targetValueUsed; }

    public double getWeightUsed() { return weightUsed; }
    public void setWeightUsed(double weightUsed) { this.weightUsed = weightUsed; }

    public double getComputedCompliance() { return computedCompliance; }
    public void setComputedCompliance(double computedCompliance) { this.computedCompliance = computedCompliance; }

    public double getComputedWeightedScore() { return computedWeightedScore; }
    public void setComputedWeightedScore(double computedWeightedScore) { this.computedWeightedScore = computedWeightedScore; }

    public boolean isBiometricVerified() { return biometricVerified; }
    public void setBiometricVerified(boolean biometricVerified) { this.biometricVerified = biometricVerified; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public String getSupervisorObservation() { return supervisorObservation; }
    public void setSupervisorObservation(String supervisorObservation) { this.supervisorObservation = supervisorObservation; }

    public boolean isFieldWork() { return isFieldWork; }
    public void setFieldWork(boolean fieldWork) { isFieldWork = fieldWork; }

    public String getFieldWorkReason() { return fieldWorkReason; }
    public void setFieldWorkReason(String fieldWorkReason) { this.fieldWorkReason = fieldWorkReason; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("recordId", recordId);
            json.put("kpiId", kpiId);
            json.put("kpiVersionUsed", kpiVersionUsed);
            json.put("kpiName", kpiName);
            json.put("role", role);
            json.put("employeeId", employeeId);
            json.put("date", date);
            json.put("actualValue", actualValue);
            json.put("targetValueUsed", targetValueUsed);
            json.put("weightUsed", weightUsed);
            json.put("computedCompliance", computedCompliance);
            json.put("computedWeightedScore", computedWeightedScore);
            json.put("biometricVerified", biometricVerified);
            json.put("period", period);
            json.put("deliveryStatus", deliveryStatus);
            json.put("supervisorObservation", supervisorObservation);
            json.put("isFieldWork", isFieldWork);
            json.put("fieldWorkReason", fieldWorkReason);
            json.put("syncStatus", syncStatus);
            json.put("timestamp", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiRecordEntry fromJson(JSONObject json) {
        if (json == null) return null;
        KpiRecordEntry e = new KpiRecordEntry();
        e.setRecordId(json.optString("recordId", ""));
        e.setKpiId(json.optString("kpiId", ""));
        e.setKpiVersionUsed(json.optInt("kpiVersionUsed", 1));
        e.setKpiName(json.optString("kpiName", ""));
        e.setRole(json.optString("role", ""));
        e.setEmployeeId(json.optString("employeeId", ""));
        e.setDate(json.optString("date", ""));
        e.setActualValue(json.optDouble("actualValue", 0.0));
        e.setTargetValueUsed(json.optDouble("targetValueUsed", 0.0));
        e.setWeightUsed(json.optDouble("weightUsed", 0.0));
        e.setComputedCompliance(json.optDouble("computedCompliance", 0.0));
        e.setComputedWeightedScore(json.optDouble("computedWeightedScore", 0.0));
        e.setBiometricVerified(json.optBoolean("biometricVerified", false));
        e.setPeriod(json.optString("period", "Diaria"));
        e.setDeliveryStatus(json.optString("deliveryStatus", "Entregado"));
        e.setSupervisorObservation(json.optString("supervisorObservation", ""));
        e.setFieldWork(json.optBoolean("isFieldWork", false));
        e.setFieldWorkReason(json.optString("fieldWorkReason", ""));
        e.setSyncStatus(json.optString("syncStatus", "PENDING"));
        e.setTimestamp(json.optLong("timestamp", System.currentTimeMillis()));
        return e;
    }
}
