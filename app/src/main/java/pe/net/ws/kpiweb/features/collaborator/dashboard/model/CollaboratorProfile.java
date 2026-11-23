package pe.net.ws.kpiweb.features.collaborator.dashboard.model;

import java.util.List;

/**
 * Entidad completa del colaborador autenticado.
 */
public class CollaboratorProfile {

    private final String id;
    private final String firstName;
    private final String fullName;
    private final String jobTitle;
    private final String department;
    private final String avatarInitials;
    private final CollaboratorPerformanceSummary performanceSummary;
    private final CollaboratorPersonalStats stats;
    private final CollaboratorActivityItem nextActivity; // Nullable
    private final List<CollaboratorActivityItem> recentActivities;
    private final List<CollaboratorKpiItem> kpis;

    public CollaboratorProfile(String id, String firstName, String fullName,
                               String jobTitle, String department, String avatarInitials,
                               CollaboratorPerformanceSummary performanceSummary,
                               CollaboratorPersonalStats stats,
                               CollaboratorActivityItem nextActivity,
                               List<CollaboratorActivityItem> recentActivities,
                               List<CollaboratorKpiItem> kpis) {
        this.id = id;
        this.firstName = firstName;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.department = department;
        this.avatarInitials = avatarInitials;
        this.performanceSummary = performanceSummary;
        this.stats = stats;
        this.nextActivity = nextActivity;
        this.recentActivities = recentActivities;
        this.kpis = kpis;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public String getAvatarInitials() {
        return avatarInitials;
    }

    public CollaboratorPerformanceSummary getPerformanceSummary() {
        return performanceSummary;
    }

    public CollaboratorPersonalStats getStats() {
        return stats;
    }

    public CollaboratorActivityItem getNextActivity() {
        return nextActivity;
    }

    public List<CollaboratorActivityItem> getRecentActivities() {
        return recentActivities;
    }

    public List<CollaboratorKpiItem> getKpis() {
        return kpis;
    }

    public int getAssignedKpisCount() {
        return kpis != null ? kpis.size() : 0;
    }

    public int getCompletedKpisCount() {
        if (kpis == null) return 0;
        int count = 0;
        for (CollaboratorKpiItem item : kpis) {
            if (item.getStatus() == CollaboratorKpiItem.Status.COMPLETADO) {
                count++;
            }
        }
        return count;
    }

    public int getPendingKpisCount() {
        if (kpis == null) return 0;
        int count = 0;
        for (CollaboratorKpiItem item : kpis) {
            if (item.getStatus() != CollaboratorKpiItem.Status.COMPLETADO) {
                count++;
            }
        }
        return count;
    }

    public int getOverdueKpisCount() {
        if (kpis == null) return 0;
        int count = 0;
        for (CollaboratorKpiItem item : kpis) {
            if (item.getStatus() == CollaboratorKpiItem.Status.VENCIDO) {
                count++;
            }
        }
        return count;
    }

    public String getNextDeliveryDateDisplay() {
        if (nextActivity != null && nextActivity.getTimeLabel() != null && !nextActivity.getTimeLabel().isEmpty()) {
            return nextActivity.getTimeLabel();
        }
        if (kpis != null) {
            for (CollaboratorKpiItem item : kpis) {
                if (item.getStatus() != CollaboratorKpiItem.Status.COMPLETADO && item.getDueDate() != null) {
                    return item.getDueDate();
                }
            }
        }
        return id != null && id.matches("[0-9]+") ? "Por consultar" : "Sin entregas próximas";
    }
}
