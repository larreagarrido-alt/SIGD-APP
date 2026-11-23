package pe.net.ws.kpiweb.features.admin.dashboard.model;

/**
 * Modelo para cada colaborador en el listado de actividades del período.
 */
public class AdminEmployeeKpiItem {
    public enum StatusType {
        ON_TRACK,
        PENDING,
        AT_RISK
    }

    private final String name;
    private final String role;
    private final int assigned;
    private final int delivered;
    private final int pending;
    private final String statusLabel;
    private final StatusType statusType;

    public AdminEmployeeKpiItem(String name, String role, int assigned, int delivered, int pending, String statusLabel, StatusType statusType) {
        this.name = name;
        this.role = role;
        this.assigned = assigned;
        this.delivered = delivered;
        this.pending = pending;
        this.statusLabel = statusLabel;
        this.statusType = statusType;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
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

    public String getStatusLabel() {
        return statusLabel;
    }

    public StatusType getStatusType() {
        return statusType;
    }
}
