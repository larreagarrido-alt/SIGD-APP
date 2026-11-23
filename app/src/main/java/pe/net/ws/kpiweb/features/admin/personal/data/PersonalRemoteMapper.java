package pe.net.ws.kpiweb.features.admin.personal.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;

/** Maps only fields actually supplied by the Personal API. */
public final class PersonalRemoteMapper {
    private PersonalRemoteMapper() {}

    private static String value(JsonObject row, String key) {
        JsonElement element = row.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    public static EmployeeItem employee(JsonObject row) {
        String name = value(row, "nombre_completo").trim();
        String[] words = name.split("\\s+");
        String initials = name.isEmpty() ? "?" : words[0].substring(0, 1).toUpperCase()
                + (words.length > 1 ? words[words.length - 1].substring(0, 1).toUpperCase() : "");
        String state = value(row, "condicion_operativa");
        String leave = value(row, "permiso_hoy_tipo");
        EmployeeItem.WorkStatus status;
        if ("INACTIVO".equalsIgnoreCase(state) || "DESVINCULADO".equalsIgnoreCase(state))
            status = EmployeeItem.WorkStatus.INACTIVO;
        else if ("DE_VACACIONES_O_PERMISO".equalsIgnoreCase(state))
            status = "Vacaciones".equalsIgnoreCase(leave) ? EmployeeItem.WorkStatus.VACACIONES
                    : EmployeeItem.WorkStatus.LICENCIA;
        else if ("OPERATIVO".equalsIgnoreCase(state)) status = EmployeeItem.WorkStatus.ACTIVO;
        else status = EmployeeItem.WorkStatus.SIN_DATOS;
        EmployeeItem employee = new EmployeeItem(value(row, "id_trabajador"), name,
                value(row, "cargo_nombre"), value(row, "agencia_nombre"), status,
                value(row, "email"), value(row, "telefono"), initials, 0xFF5B57BA,
                -1, 0, 0, 0, value(row, "fecha_inicio"), value(row, "agencia_nombre"),
                "", "", "", "Sin evaluación registrada", "", "", 0, 0, 0,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        employee.setPhotoUrl(value(row, "foto_perfil"));
        return employee;
    }
}
