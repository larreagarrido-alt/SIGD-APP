package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Horario del cargo Asistente Administrativo (Lun–Sáb, 8:00 a 18:30). */
final class AdminAssistantScheduleData {

    private AdminAssistantScheduleData() { }

    private static int m(int h, int min) {
        return h * 60 + min;
    }

    private static Slot six(Slot s, String lun, String mar, String mie, String jue, String vie,
            String sab) {
        return s.day(0, lun).day(1, mar).day(2, mie).day(3, jue).day(4, vie).day(5, sab);
    }

    static List<Slot> slots() {
        List<Slot> s = new ArrayList<>();
        int[] all = {0, 1, 2, 3, 4, 5};

        s.add(new Slot("8:00 - 8:30", m(8, 0), m(8, 29), Kind.ENTRY)
                .days("Supervisión de entrada", all));

        s.add(six(new Slot("8:30 - 9:30", m(8, 30), m(9, 29), Kind.NORMAL),
                "Revisión de agenda, pendientes y documentos",
                "Revisión de agenda de equipos",
                "Revisión de requerimientos",
                "Revisión de recursos necesarios",
                "Revisión de formatos y registros",
                "Revisión de resultados semanales"));

        s.add(six(new Slot("9:30 - 10:30", m(9, 30), m(10, 29), Kind.NORMAL),
                "Gestión de documentos", "Coordinación de actividades",
                "Seguimiento de compras y servicios", "Apoyo en logística",
                "Control y actualización", "Reporte y consolidación"));

        s.add(six(new Slot("10:30 - 12:00", m(10, 30), m(11, 59), Kind.NORMAL),
                "Registro y archivo digital", "Programación de reuniones internas",
                "Consulta de estado de solicitudes", "Gestión de materiales",
                "Actualización de reportes", "Recopilación de gestiones"));

        s.add(six(new Slot("12:00 - 12:30", m(12, 0), m(12, 29), Kind.NORMAL),
                "Verificación de documentos recibidos", "Seguimiento de acuerdos",
                "Coordinación con proveedores", "Control de entregas y solicitudes",
                "Verificación de información", "Revisión de coordinaciones"));

        s.add(six(new Slot("12:30 - 13:00", m(12, 30), m(12, 59), Kind.NORMAL),
                "Coordinación de pendientes", "Coordinación con responsables",
                "Registro de seguimientos", "Coordinación de abastecimiento",
                "Corrección de registros", "Elaboración del informe semanal"));

        s.add(six(new Slot("13:00 - 13:30", m(13, 0), m(13, 29), Kind.NORMAL),
                "Actualización de archivos", "Registro de compromisos",
                "Actualización de pendientes", "Registro de gestiones",
                "Consolidación de actualizaciones", "Validación de datos"));

        s.add(new Slot("13:30 - 15:30", m(13, 30), m(15, 29), Kind.BREAK)
                .days("BREAK", all));

        s.add(six(new Slot("15:30 - 16:30", m(15, 30), m(16, 29), Kind.NORMAL),
                "Organización de expedientes", "Seguimiento de coordinaciones",
                "Seguimiento a requerimientos", "Seguimiento de recursos",
                "Seguimiento de actualizaciones", "Redacción del informe de gestión"));

        s.add(six(new Slot("16:30 - 17:30", m(16, 30), m(17, 29), Kind.NORMAL),
                "Control de registros realizados", "Verificación de tareas asignadas",
                "Verificación de respuestas", "Verificación de materiales",
                "Validación de documentos", "Revisión de resultados y mejoras"));

        s.add(six(new Slot("17:30 - 18:00", m(17, 30), m(17, 59), Kind.NORMAL),
                "Reporte de actividades", "Reporte de coordinaciones", "Reporte de avances",
                "Reporte logístico", "Reporte de control", "Entrega del informe"));

        s.add(new Slot("18:00 - 18:30", m(18, 0), m(18, 30), Kind.EXIT)
                .days("Supervisión de salida", all));

        return s;
    }

    /** Metas principales por día. */
    static List<ScheduleKpi> goals() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Gestión de documentos (Registro y archivo)", "≥ 10 registros", true, "Lunes"));
        k.add(new ScheduleKpi("Coordinación de actividades (Áreas y equipo)", "≥ 4 coordinaciones", true, "Martes"));
        k.add(new ScheduleKpi("Seguimiento de requerimientos (Compras / Servicios)", "≥ 5 seguimientos", true, "Miércoles"));
        k.add(new ScheduleKpi("Apoyo en logística (Recursos / Materiales)", "≥ 3 gestiones", true, "Jueves"));
        k.add(new ScheduleKpi("Control y actualización (Formatos / Registros)", "≥ 5 actualizaciones", true, "Viernes"));
        k.add(new ScheduleKpi("Reporte y consolidación (Resultados semanales)", "≥ 1 informe (semanal)", true, "Sábado"));
        k.add(new ScheduleKpi("Total semanal", "6", false, "actividades principales"));
        return k;
    }
}
