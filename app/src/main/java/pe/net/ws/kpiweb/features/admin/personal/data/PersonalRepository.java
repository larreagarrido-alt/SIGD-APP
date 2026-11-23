package pe.net.ws.kpiweb.features.admin.personal.data;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;

/**
 * Repositorio centralizado para el módulo Personal.
 * Implementa el patrón Repository desacoplando las Activities y los layouts XML
 * de los orígenes de datos específicos.
 *
 * La sesión administrativa usa un snapshot vacío hasta recibir datos autorizados de la API.
 */
public class PersonalRepository {

    private static PersonalRepository instance;
    private PersonalDataSource dataSource;
    private LivePersonalDataSource liveDataSource;

    public static synchronized PersonalRepository getInstance() {
        if (instance == null) {
            instance = new PersonalRepository();
        }
        return instance;
    }

    public PersonalRepository() {
        liveDataSource = new LivePersonalDataSource();
        dataSource = liveDataSource;
    }

    /**
     * Permite inyectar una fuente alternativa para pruebas aisladas.
     */
    public synchronized void setDataSource(PersonalDataSource newDataSource) {
        if (newDataSource != null) {
            this.dataSource = newDataSource;
        }
    }

    public synchronized void useLiveData() {
        liveDataSource = new LivePersonalDataSource();
        dataSource = liveDataSource;
    }

    public synchronized void replaceLiveEmployees(List<EmployeeItem> employees) {
        if (liveDataSource != null) liveDataSource.replace(employees);
    }

    public List<EmployeeItem> getAllEmployees() {
        return dataSource.getAllEmployees();
    }

    public EmployeeItem getEmployeeById(String id) {
        return dataSource.getEmployeeById(id);
    }

    public int getTotalCount() {
        return dataSource.getTotalCount();
    }

        public int getActiveCount() {
        return dataSource.getActiveCount();
    }

    public int getVacationCount() {
        return dataSource.getVacationCount();
    }

    public int getTrainingAndLeaveCount() {
        return dataSource.getTrainingAndLeaveCount();
    }

    public List<DepartmentSummary> getDepartmentSummaries() {
        return dataSource.getDepartmentSummaries();
    }

    public List<EmployeeItem> filter(String query, String department, EmployeeItem.WorkStatus status) {
        return dataSource.filterEmployees(query, department, status);
    }
}
