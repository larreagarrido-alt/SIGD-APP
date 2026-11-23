package pe.net.ws.kpiweb.features.kpi.senior.model;

import java.io.Serializable;

/**
 * Representa uno de los seis indicadores clave del cargo (KPI) para el Diseñador Gráfico Senior.
 */
public class KpiSeniorIndicator implements Serializable {
    private String name;
    private int weightPercentage; // 25, 25, 20, 15, 10, 5
    private float compliancePercentage;
    private boolean isPending;

    public KpiSeniorIndicator(String name, int weightPercentage, float compliancePercentage, boolean isPending) {
        this.name = name;
        this.weightPercentage = weightPercentage;
        this.compliancePercentage = compliancePercentage;
        this.isPending = isPending;
    }

    public String getName() {
        return name;
    }

    public int getWeightPercentage() {
        return weightPercentage;
    }

    public float getCompliancePercentage() {
        return compliancePercentage;
    }

    public void setCompliancePercentage(float compliancePercentage) {
        this.compliancePercentage = compliancePercentage;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public float getWeightedValue() {
        if (isPending) return 0f;
        return (weightPercentage * compliancePercentage) / 100f;
    }
}
