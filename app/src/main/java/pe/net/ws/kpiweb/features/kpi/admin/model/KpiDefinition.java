package pe.net.ws.kpiweb.features.kpi.admin.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import pe.net.ws.kpiweb.features.kpi.engine.KpiMeasurementType;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;

/**
 * Definición completa y versionable de un indicador KPI configurado por Gerencia.
 * Cumple con los 15 campos requeridos por la directriz administrativa del SIGD 2026.
 */
public class KpiDefinition implements Serializable {

    private String id;
    private int version = 1;
    private String name;
    private String description;
    private String objective;
    private String area;
    private String role;
    private String responsible;
    private KpiMeasurementType measurementType = KpiMeasurementType.CANTIDAD;
    private String unit = "unidades";
    private double targetValue = 10.0;
    private String frequency = "Diaria"; // Diaria, Semanal, Mensual
    private double weight = 20.0; // 0% a 100%
    private double baseline = 0.0;
    private String informationSource = "Reporte diario de actividades";
    private String startDate = "2026-10-01";
    private KpiPolarity polarity = KpiPolarity.MAYOR_ES_MEJOR;
    private boolean active = true;
    private long createdAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    public KpiDefinition() {}

    public KpiDefinition(String id, String name, String area, String role,
                         KpiMeasurementType measurementType, String unit,
                         double targetValue, double weight, KpiPolarity polarity) {
        this.id = id;
        this.name = name;
        this.area = area;
        this.role = role;
        this.measurementType = measurementType;
        this.unit = unit;
        this.targetValue = targetValue;
        this.weight = weight;
        this.polarity = polarity;
        this.active = true;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public KpiMeasurementType getMeasurementType() { return measurementType; }
    public void setMeasurementType(KpiMeasurementType measurementType) { this.measurementType = measurementType; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getBaseline() { return baseline; }
    public void setBaseline(double baseline) { this.baseline = baseline; }

    public String getInformationSource() { return informationSource; }
    public void setInformationSource(String informationSource) { this.informationSource = informationSource; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public KpiPolarity getPolarity() { return polarity; }
    public void setPolarity(KpiPolarity polarity) { this.polarity = polarity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("version", version);
            json.put("name", name);
            json.put("description", description);
            json.put("objective", objective);
            json.put("area", area);
            json.put("role", role);
            json.put("responsible", responsible);
            json.put("measurementType", measurementType != null ? measurementType.name() : KpiMeasurementType.CANTIDAD.name());
            json.put("unit", unit);
            json.put("targetValue", targetValue);
            json.put("frequency", frequency);
            json.put("weight", weight);
            json.put("baseline", baseline);
            json.put("informationSource", informationSource);
            json.put("startDate", startDate);
            json.put("polarity", polarity != null ? polarity.name() : KpiPolarity.MAYOR_ES_MEJOR.name());
            json.put("active", active);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiDefinition fromJson(JSONObject json) {
        if (json == null) return null;
        KpiDefinition k = new KpiDefinition();
        k.setId(json.optString("id", ""));
        k.setVersion(json.optInt("version", 1));
        k.setName(json.optString("name", ""));
        k.setDescription(json.optString("description", ""));
        k.setObjective(json.optString("objective", ""));
        k.setArea(json.optString("area", ""));
        k.setRole(json.optString("role", ""));
        k.setResponsible(json.optString("responsible", ""));
        try {
            k.setMeasurementType(KpiMeasurementType.valueOf(json.optString("measurementType", KpiMeasurementType.CANTIDAD.name())));
        } catch (Exception ignored) {
            k.setMeasurementType(KpiMeasurementType.CANTIDAD);
        }
        k.setUnit(json.optString("unit", "unidades"));
        k.setTargetValue(json.optDouble("targetValue", 10.0));
        k.setFrequency(json.optString("frequency", "Diaria"));
        k.setWeight(json.optDouble("weight", 20.0));
        k.setBaseline(json.optDouble("baseline", 0.0));
        k.setInformationSource(json.optString("informationSource", ""));
        k.setStartDate(json.optString("startDate", ""));
        try {
            k.setPolarity(KpiPolarity.valueOf(json.optString("polarity", KpiPolarity.MAYOR_ES_MEJOR.name())));
        } catch (Exception ignored) {
            k.setPolarity(KpiPolarity.MAYOR_ES_MEJOR);
        }
        k.setActive(json.optBoolean("active", true));
        k.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
        k.setUpdatedAt(json.optLong("updatedAt", System.currentTimeMillis()));
        return k;
    }
}
