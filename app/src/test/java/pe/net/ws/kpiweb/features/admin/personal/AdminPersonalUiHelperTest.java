package pe.net.ws.kpiweb.features.admin.personal;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalUiHelper.SortOption;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalUiHelper.Stats;
import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem.WorkStatus;

import static org.junit.Assert.assertEquals;

public class AdminPersonalUiHelperTest {

    private EmployeeItem sample(String id, String name, String role, String agency, WorkStatus status) {
        return new EmployeeItem(id, name, role, agency, status,
                "", "", "XX", 0xFF5B57BA, -1, 0, 0, 0);
    }

    @Test
    public void computesRealStatusCountsForFilterChips() {
        List<EmployeeItem> list = Arrays.asList(
                sample("1", "Ana López", "Diseñadora", "Agencia Lima", WorkStatus.ACTIVO),
                sample("2", "Bruno Díaz", "Analista", "Agencia Chiclayo", WorkStatus.ACTIVO),
                sample("3", "Carla Ruiz", "Desarrolladora", "Agencia Lima", WorkStatus.INACTIVO),
                sample("4", "Diego Pérez", "Soporte", "Agencia Chiclayo", WorkStatus.LICENCIA),
                sample("5", "Elena Soto", "Asistente", "Agencia Lima", WorkStatus.VACACIONES));
        Stats stats = AdminPersonalUiHelper.computeStats(list);
        assertEquals(5, stats.total);
        assertEquals(2, stats.active);
        assertEquals(1, stats.inactive);
        assertEquals(1, stats.leave);
    }

    @Test
    public void computesDynamicAgencyCountsWithoutZeroingAlternatives() {
        List<EmployeeItem> list = Arrays.asList(
                sample("1", "Ana López", "Diseñadora", "Agencia Lima", WorkStatus.ACTIVO),
                sample("2", "Bruno Díaz", "Analista", "Agencia Chiclayo", WorkStatus.ACTIVO),
                sample("3", "Carla Ruiz", "Desarrolladora", "Agencia Lima", WorkStatus.ACTIVO));
        Map<String, Integer> counts = AdminPersonalUiHelper.countByAgency(list);
        assertEquals(3, AdminPersonalUiHelper.agencyCountFor(counts, "Todos"));
        assertEquals(2, AdminPersonalUiHelper.agencyCountFor(counts, "Agencia Lima"));
        assertEquals(1, AdminPersonalUiHelper.agencyCountFor(counts, "Agencia Chiclayo"));
        assertEquals(0, AdminPersonalUiHelper.agencyCountFor(counts, "Agencia Piura"));
        assertEquals("Todas", AdminPersonalUiHelper.formatAgencyDisplayName("Todos"));
        assertEquals("Agencia Lima", AdminPersonalUiHelper.formatAgencyDisplayName("Agencia Lima"));
    }

    @Test
    public void sortsByNameRoleStatusWhilePreservingOriginalOrder() {
        EmployeeItem c = sample("3", "Carlos Vega", "Backend", "Agencia Lima", WorkStatus.INACTIVO);
        EmployeeItem a = sample("1", "Ángel Torres", "QA", "Agencia Lima", WorkStatus.ACTIVO);
        EmployeeItem b = sample("2", "Beatriz Paz", "Analista", "Agencia Lima", WorkStatus.LICENCIA);
        List<EmployeeItem> source = Arrays.asList(c, a, b);

        assertEquals("3", AdminPersonalUiHelper.sortEmployees(source, SortOption.ORIGINAL).get(0).getId());
        assertEquals("1", AdminPersonalUiHelper.sortEmployees(source, SortOption.NAME_ASC).get(0).getId());
        assertEquals("2", AdminPersonalUiHelper.sortEmployees(source, SortOption.ROLE_ASC).get(0).getId());

        List<EmployeeItem> byStatus = AdminPersonalUiHelper.sortEmployees(source, SortOption.STATUS);
        assertEquals(WorkStatus.ACTIVO, byStatus.get(0).getWorkStatus());
        assertEquals(WorkStatus.LICENCIA, byStatus.get(1).getWorkStatus());
        assertEquals(WorkStatus.INACTIVO, byStatus.get(2).getWorkStatus());
    }

    @Test
    public void formatsCountsAndAgencyLabelsAccurately() {
        assertEquals("16 colaboradores", AdminPersonalUiHelper.formatResultsCount(16, 16, false));
        assertEquals("1 colaborador", AdminPersonalUiHelper.formatResultsCount(1, 1, false));
        assertEquals("4 de 16 colaboradores", AdminPersonalUiHelper.formatResultsCount(4, 16, true));
        assertEquals("Todas (16)", AdminPersonalUiHelper.formatAgencyChipLabel(new DepartmentSummary("Todos", 16, 14)));
        assertEquals("Agencia Lima (10)",
                AdminPersonalUiHelper.formatAgencyChipLabel(new DepartmentSummary("Agencia Lima", 10, 9)));
    }
}
