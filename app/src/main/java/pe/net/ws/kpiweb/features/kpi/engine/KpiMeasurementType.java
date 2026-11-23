package pe.net.ws.kpiweb.features.kpi.engine;

/**
 * Tipos de medición admitidos para los indicadores KPI según el SIGD 2026.
 */
public enum KpiMeasurementType {
    CANTIDAD("Cantidad", "Unidades numéricas fijadas como meta"),
    PORCENTAJE("Porcentaje", "Proporción de actividades realizadas vs programadas"),
    TIEMPO("Tiempo", "Minutos u horas dedicadas contra un umbral límite"),
    PROMEDIO("Promedio", "Media aritmética de múltiples registros o evaluaciones"),
    CALIFICACION("Calificación", "Puntaje cualitativo o escala de desempeño (1 a 20 / 1 a 5)"),
    CUMPLIMIENTO_ACTIVIDADES("Cumplimiento de actividades", "Conteo de hitos entregados en cronograma");

    private final String displayName;
    private final String description;

    KpiMeasurementType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
