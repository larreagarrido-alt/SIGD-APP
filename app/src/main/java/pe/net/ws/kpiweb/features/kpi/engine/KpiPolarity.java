package pe.net.ws.kpiweb.features.kpi.engine;

/**
 * Sentido de la optimización del indicador:
 * - MAYOR_ES_MEJOR: A mayor valor alcanzado, mayor es el porcentaje de cumplimiento (ej. ventas, piezas diseñadas).
 * - MENOR_ES_MEJOR: A menor valor registrado, mejor es el desempeño (ej. bugs generados, tiempos de respuesta, incidencias).
 */
public enum KpiPolarity {
    MAYOR_ES_MEJOR("Mayor es mejor (Directo)", "Superar o igualar la meta genera mayor cumplimiento"),
    MENOR_ES_MEJOR("Menor es mejor (Inverso)", "Mantenerse por debajo del umbral genera mayor cumplimiento");

    private final String displayName;
    private final String description;

    KpiPolarity(String displayName, String description) {
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
