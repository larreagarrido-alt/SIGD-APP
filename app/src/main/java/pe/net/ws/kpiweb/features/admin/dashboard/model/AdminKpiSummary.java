package pe.net.ws.kpiweb.features.admin.dashboard.model;

/**
 * Resumen de los 4 indicadores KPI principales.
 * Cumple la regla de consistencia: assigned = delivered + pending + overdue
 */
public class AdminKpiSummary {
    private final int assigned;
    private final int delivered;
    private final int pending;
    private final int overdue;

    public AdminKpiSummary(int assigned, int delivered, int pending, int overdue) {
        this.assigned = assigned;
        this.delivered = delivered;
        this.pending = pending;
        this.overdue = overdue;
    }

    public int getAssigned() {
        return assigned;
    }

    public int getDelivered() {
        return delivered;
    }

    public int getPending() {
        return pending;
    }

    public int getOverdue() {
        return overdue;
    }
}
