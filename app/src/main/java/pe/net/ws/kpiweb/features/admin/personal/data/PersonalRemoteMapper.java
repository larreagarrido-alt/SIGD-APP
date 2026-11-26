package pe.net.ws.kpiweb.features.admin.personal.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeKpiItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EvaluationHistoryItem;

/** Maps only fields actually supplied by the Personal API. */
public final class PersonalRemoteMapper {
    private PersonalRemoteMapper() {}

    private static String value(JsonObject row, String key) {
        JsonElement element = row.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static int count(JsonObject row, String key) {
        try { return Integer.parseInt(value(row, key)); }
        catch (NumberFormatException ignored) { return 0; }
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

    /** Maps only approved server results; a received form is never shown as an approved KPI. */
    public static EmployeeItem detail(JsonObject data) {
        JsonObject row=data.getAsJsonObject("empleado");
        EmployeeItem base=employee(row);
        JsonObject summary=data.has("kpi_resumen") && data.get("kpi_resumen").isJsonObject()
                ? data.getAsJsonObject("kpi_resumen") : new JsonObject();
        int approved=count(summary,"evaluaciones_aprobadas");
        int received=count(summary,"formularios_recibidos");
        int awaiting=count(summary,"sin_revisar");
        String percent=value(summary,"porcentaje");
        int score=-1;
        if (!percent.isEmpty()) {
            try { score=(int)Math.round(Double.parseDouble(percent)); }
            catch (NumberFormatException ignored) { }
        }
        List<EvaluationHistoryItem> history=new ArrayList<>();
        JsonArray historyRows=data.getAsJsonArray("kpi_historial");
        if (historyRows!=null) for (JsonElement element:historyRows) if (element.isJsonObject()) {
            JsonObject item=element.getAsJsonObject();
            try { history.add(new EvaluationHistoryItem(value(item,"periodo"),
                    (int)Math.round(Double.parseDouble(value(item,"porcentaje"))),"Aprobado")); }
            catch (NumberFormatException ignored) { }
        }
        List<EmployeeKpiItem> indicators=new ArrayList<>();
        JsonArray indicatorRows=data.getAsJsonArray("kpi_indicadores");
        if (indicatorRows!=null) for (JsonElement element:indicatorRows) if (element.isJsonObject()) {
            JsonObject item=element.getAsJsonObject();
            String actual=value(item,"porcentaje_actual");
            int progress=0;
            try { progress=(int)Math.round(Double.parseDouble(actual)); }
            catch (NumberFormatException ignored) { }
            indicators.add(new EmployeeKpiItem(value(item,"id_kpi"),value(item,"nombre"),
                    value(item,"descripcion"),value(item,"periodicidad"),
                    value(item,"meta_esperada")+" "+value(item,"unidad_medida"),
                    actual.isEmpty()?"Sin evaluación aprobada este mes":actual+" % aprobado",
                    "%",progress,!actual.isEmpty(),actual.isEmpty()
                    ? EmployeeKpiItem.KpiStatus.PENDIENTE : EmployeeKpiItem.KpiStatus.CUMPLIDO));
        }
        String current=score<0 ? "Esperando evaluación del administrador" : "Resultado aprobado este mes";
        String description=received+" formularios recibidos · "+awaiting+" por revisar";
        EmployeeItem detail=new EmployeeItem(base.getId(),base.getName(),base.getRole(),
                base.getDepartment(),base.getWorkStatus(),base.getEmail(),base.getPhone(),
                base.getAvatarInitials(),base.getAvatarColorHex(),score,0,0,0,
                value(row,"fecha_inicio"),base.getLocation(),value(row,"tipo_trabajador"),
                "","",current,description,approved+" evaluaciones aprobadas",
                count(summary,"indicadores_asignados"),approved,awaiting,
                history,indicators,Collections.emptyList());
        detail.setPhotoUrl(base.getPhotoUrl());
        return detail;
    }
}
