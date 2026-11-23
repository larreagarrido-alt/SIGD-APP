package pe.net.ws.kpiweb;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonObject;
import java.util.Arrays;
import org.junit.Test;
import pe.net.ws.kpiweb.features.admin.personal.data.LivePersonalDataSource;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRemoteMapper;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;

public class AdminPersonalDataTest {
    private static EmployeeItem worker(String id, String state) {
        JsonObject row = new JsonObject();
        row.addProperty("id_trabajador", id);
        row.addProperty("nombre_completo", "Persona de prueba");
        row.addProperty("condicion_operativa", state);
        row.addProperty("cargo_nombre", "Diseño");
        row.addProperty("agencia_nombre", "Lima");
        return PersonalRemoteMapper.employee(row);
    }

    @Test public void unknownStatusDoesNotBecomeActive() {
        assertEquals(EmployeeItem.WorkStatus.SIN_DATOS, worker("12", "").getWorkStatus());
        assertEquals(EmployeeItem.WorkStatus.SIN_DATOS, worker("12", "ESTADO_NUEVO").getWorkStatus());
        assertEquals(EmployeeItem.WorkStatus.ACTIVO, worker("12", "OPERATIVO").getWorkStatus());
    }

    @Test public void multipleAccountsCountAsOneWorker() {
        LivePersonalDataSource source = new LivePersonalDataSource();
        source.replace(Arrays.asList(worker("12", "OPERATIVO"), worker("12", "OPERATIVO"),
                worker("13", "INACTIVO")));
        assertEquals(2, source.getTotalCount());
        assertEquals(1, source.getActiveCount());
        assertEquals(1, source.getDepartmentSummaries().get(1).getActiveCount());
    }
}
