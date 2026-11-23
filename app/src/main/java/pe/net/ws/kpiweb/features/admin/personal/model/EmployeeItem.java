package pe.net.ws.kpiweb.features.admin.personal.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modelo representativo de un colaborador en el módulo Personal.
 * Preparado para desacoplamiento e integración con API externa y datos de KPI.
 * Cumple con la política de no mostrar información médica ni datos privados sensibles.
 */
public class EmployeeItem implements Serializable {

    public enum WorkStatus {
        ACTIVO("Activo"),
        VACACIONES("De vacaciones"),
        CAPACITACION("En capacitación"),
        LICENCIA("Con licencia"),
        INACTIVO("Inactivo"),
        SIN_DATOS("Estado no disponible");

        private final String displayName;

        WorkStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String id;
    private final String name;
    private final String role;
    private final String department;
    private final WorkStatus workStatus;
    private final String email;
    private final String phone;
    private final String avatarInitials;
    private String photoUrl;
    private final int avatarColorHex;
    private final int kpiSummaryScore;
    private final int assignedActivities;
    private final int completedActivities;
    private final int pendingActivities;
    private final String hireDate;
    private final String location;
    private final String contractType;
    private final String workSchedule;
    private final String workModality;
    private final String performanceSummaryStatus;
    private final String performanceComparisonText;
    private final String evaluationsCountText;
    private final int kpiAssignedCount;
    private final int kpiCompletedCount;
    private final int kpiPendingCount;
    private final List<EvaluationHistoryItem> evaluationHistory;
    private final List<EmployeeKpiItem> kpis;
    private final List<EmployeeActivityItem> activities;

    public EmployeeItem(String id, String name, String role, String department,
                        WorkStatus workStatus, String email, String phone,
                        String avatarInitials, int avatarColorHex,
                        int kpiSummaryScore, int assignedActivities,
                        int completedActivities, int pendingActivities) {
        this(id, name, role, department, workStatus, email, phone, avatarInitials,
             avatarColorHex, kpiSummaryScore, assignedActivities, completedActivities,
             pendingActivities, "01/02/2023", "Sede Central - San Isidro", "Plazo Indeterminado",
             "08:30 - 17:30", "Híbrida (3x2)", "Desempeño Sobresaliente",
             "+8 pp frente al período anterior", "12 de 14 aprobadas",
             4, 3, 1, null, null, null);
    }

    public EmployeeItem(String id, String name, String role, String department,
                        WorkStatus workStatus, String email, String phone,
                        String avatarInitials, int avatarColorHex,
                        int kpiSummaryScore, int assignedActivities,
                        int completedActivities, int pendingActivities,
                        String hireDate, String location, String contractType,
                        List<EmployeeActivityItem> activities) {
        this(id, name, role, department, workStatus, email, phone, avatarInitials,
             avatarColorHex, kpiSummaryScore, assignedActivities, completedActivities,
             pendingActivities, hireDate, location, contractType,
             "08:30 - 17:30", "Híbrida (3x2)", "Desempeño Sobresaliente",
             "+8 pp frente al período anterior", "12 de 14 aprobadas",
             4, 3, 1, null, null, activities);
    }

    public EmployeeItem(String id, String name, String role, String department,
                        WorkStatus workStatus, String email, String phone,
                        String avatarInitials, int avatarColorHex,
                        int kpiSummaryScore, int assignedActivities,
                        int completedActivities, int pendingActivities,
                        String hireDate, String location, String contractType,
                        String workSchedule, String workModality,
                        String performanceSummaryStatus, String performanceComparisonText,
                        String evaluationsCountText, int kpiAssignedCount,
                        int kpiCompletedCount, int kpiPendingCount,
                        List<EvaluationHistoryItem> evaluationHistory,
                        List<EmployeeKpiItem> kpis,
                        List<EmployeeActivityItem> activities) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.department = department;
        this.workStatus = workStatus;
        this.email = email;
        this.phone = phone;
        this.avatarInitials = avatarInitials;
        this.avatarColorHex = avatarColorHex;
        this.kpiSummaryScore = kpiSummaryScore;
        this.assignedActivities = assignedActivities;
        this.completedActivities = completedActivities;
        this.pendingActivities = pendingActivities;
        this.hireDate = hireDate != null ? hireDate : "15/01/2022";
        this.location = location != null ? location : "Sede Central - San Isidro";
        this.contractType = contractType != null ? contractType : "Plazo Indeterminado";
        this.workSchedule = workSchedule != null ? workSchedule : "08:30 - 17:30";
        this.workModality = workModality != null ? workModality : "Híbrida (3x2)";
        this.performanceSummaryStatus = performanceSummaryStatus != null ? performanceSummaryStatus : "Excelente";
        this.performanceComparisonText = performanceComparisonText != null ? performanceComparisonText : "+5 pp frente al período anterior";
        this.evaluationsCountText = evaluationsCountText != null ? evaluationsCountText : "10 de 12 evaluaciones";
        this.kpiAssignedCount = kpiAssignedCount;
        this.kpiCompletedCount = kpiCompletedCount;
        this.kpiPendingCount = kpiPendingCount;
        this.evaluationHistory = evaluationHistory != null ? new ArrayList<>(evaluationHistory) : new ArrayList<>();
        this.kpis = kpis != null ? new ArrayList<>(kpis) : new ArrayList<>();
        this.activities = activities != null ? new ArrayList<>(activities) : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getDepartment() {
        return department;
    }

    public WorkStatus getWorkStatus() {
        return workStatus;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarInitials() {
        return avatarInitials;
    }

    public String getPhotoUrl() { return photoUrl; }

    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getAvatarColorHex() {
        return avatarColorHex;
    }

    public int getKpiSummaryScore() {
        return kpiSummaryScore;
    }

    public int getAssignedActivities() {
        return assignedActivities;
    }

    public int getCompletedActivities() {
        return completedActivities;
    }

    public int getPendingActivities() {
        return pendingActivities;
    }

    public String getHireDate() {
        return hireDate;
    }

    public String getLocation() {
        return location;
    }

    public String getContractType() {
        return contractType;
    }

    public String getWorkSchedule() {
        return workSchedule;
    }

    public String getWorkModality() {
        return workModality;
    }

    public String getPerformanceSummaryStatus() {
        return performanceSummaryStatus;
    }

    public String getPerformanceComparisonText() {
        return performanceComparisonText;
    }

    public String getEvaluationsCountText() {
        return evaluationsCountText;
    }

    public int getKpiAssignedCount() {
        return kpiAssignedCount;
    }

    public int getKpiCompletedCount() {
        return kpiCompletedCount;
    }

    public int getKpiPendingCount() {
        return kpiPendingCount;
    }

    public List<EvaluationHistoryItem> getEvaluationHistory() {
        return Collections.unmodifiableList(evaluationHistory);
    }

    public List<EmployeeKpiItem> getKpis() {
        return Collections.unmodifiableList(kpis);
    }

    public List<EmployeeActivityItem> getActivities() {
        return Collections.unmodifiableList(activities);
    }
}
