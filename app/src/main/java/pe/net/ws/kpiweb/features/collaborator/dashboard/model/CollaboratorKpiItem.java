package pe.net.ws.kpiweb.features.collaborator.dashboard.model;

/**
 * Modelo genérico y reutilizable para cualquier tipo de indicador asignado al colaborador
 * (Diseño, Ventas, Soporte, etc.).
 */
public class CollaboratorKpiItem {

    public enum MeasurementType {
        ENTREGABLES,
        MONTO_FINANCIERO,
        PORCENTAJE,
        TIEMPO,
        PUNTUACION
    }

    public enum Status {
        COMPLETADO,
        EN_CURSO,
        PENDIENTE,
        VENCIDO
    }

    private final String id;
    private final String name;
    private final MeasurementType measurementType;
    private final double currentValue;
    private final double targetValue;
    private final String unit;
    private final Status status;
    private final String dueDate;

    public CollaboratorKpiItem(String id, String name, MeasurementType measurementType,
                               double currentValue, double targetValue, String unit,
                               Status status, String dueDate) {
        this.id = id;
        this.name = name;
        this.measurementType = measurementType;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
        this.unit = unit;
        this.status = status;
        this.dueDate = dueDate;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public String getUnit() {
        return unit;
    }

    public Status getStatus() {
        return status;
    }

    public String getDueDate() {
        return dueDate;
    }

    public int getProgressPercentage() {
        if (targetValue <= 0) return 0;
        int p = (int) Math.round((currentValue * 100.0) / targetValue);
        return Math.min(100, Math.max(0, p));
    }
}
