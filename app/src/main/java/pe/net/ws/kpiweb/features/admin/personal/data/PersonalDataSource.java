package pe.net.ws.kpiweb.features.admin.personal.data;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;

/**
 * Contrato de fuente de datos para el módulo Personal.
 */
public interface PersonalDataSource {
    List<EmployeeItem> getAllEmployees();
    EmployeeItem getEmployeeById(String id);
    List<DepartmentSummary> getDepartmentSummaries();
    List<EmployeeItem> filterEmployees(String query, String department, EmployeeItem.WorkStatus status);
    int getTotalCount();
    int getActiveCount();
    int getVacationCount();
    int getTrainingAndLeaveCount();
}
