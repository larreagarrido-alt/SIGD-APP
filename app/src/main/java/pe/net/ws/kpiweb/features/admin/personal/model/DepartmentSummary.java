package pe.net.ws.kpiweb.features.admin.personal.model;

/**
 * Resumen cuantitativo por área o departamento.
 */
public class DepartmentSummary {
    private final String departmentName;
    private final int totalCount;
    private final int activeCount;

    public DepartmentSummary(String departmentName, int totalCount, int activeCount) {
        this.departmentName = departmentName;
        this.totalCount = totalCount;
        this.activeCount = activeCount;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public String getChipLabel() {
        return departmentName + " (" + totalCount + ")";
    }
}
