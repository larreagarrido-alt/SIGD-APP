package pe.net.ws.kpiweb.features.kpi.remote;

import com.google.gson.annotations.SerializedName;

/** Snapshot de una evaluación KPI entregada por la API de WebSite+. */
public final class KpiServerAssignment {
    @SerializedName("id_evaluacion") private long evaluationId;
    @SerializedName("id_kpi") private long kpiId;
    @SerializedName("kpi_codigo") private String kpiCode;
    @SerializedName("kpi_nombre") private String name;
    @SerializedName("kpi_descripcion") private String description;
    @SerializedName("categoria_nombre") private String category;
    @SerializedName("tipo_metrica") private String metricType;
    @SerializedName("unidad_medida") private String unit;
    @SerializedName("periodo_tipo") private String periodType;
    @SerializedName("periodo_clave") private String periodKey;
    @SerializedName("fecha_limite") private String dueAt;
    @SerializedName("estado") private String status;
    @SerializedName("meta_snapshot") private double target;
    @SerializedName("peso_snapshot") private double weight;
    @SerializedName("puntos_base_snapshot") private int basePoints;
    @SerializedName("valor_reportado") private Double reportedValue;
    @SerializedName("porcentaje_cumplimiento") private Double compliance;
    @SerializedName("puntos_otorgados") private Integer awardedPoints;

    public long getEvaluationId() { return evaluationId; }
    public long getKpiId() { return kpiId; }
    public String getKpiCode() { return kpiCode; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getMetricType() { return metricType; }
    public String getUnit() { return unit; }
    public String getPeriodType() { return periodType; }
    public String getPeriodKey() { return periodKey; }
    public String getDueAt() { return dueAt; }
    public String getStatus() { return status; }
    public double getTarget() { return target; }
    public double getWeight() { return weight; }
    public int getBasePoints() { return basePoints; }
    public Double getReportedValue() { return reportedValue; }
    public Double getCompliance() { return compliance; }
    public Integer getAwardedPoints() { return awardedPoints; }
}
