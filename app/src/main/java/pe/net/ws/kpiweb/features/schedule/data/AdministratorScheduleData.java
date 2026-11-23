package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Horario del cargo Administrador (Lun–Sáb, 8:00 a 18:30). */
final class AdministratorScheduleData {

    private AdministratorScheduleData() { }

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
                "Revisión de agenda, pendientes y recursos",
                "Revisión de agenda y avances de equipos",
                "Revisión de requerimientos y stock",
                "Revisión de pendientes entre áreas",
                "Revisión de KPIs y resultados previos",
                "Revisión de pendientes de cierre semanal"));

        s.add(six(new Slot("9:30 - 10:30", m(9, 30), m(10, 29), Kind.NORMAL),
                "Planificación semanal", "Supervisión de equipos", "Gestión de recursos",
                "Coordinación interáreas", "Control de indicadores", "Reporte de gestión"));

        s.add(six(new Slot("10:30 - 12:00", m(10, 30), m(11, 59), Kind.NORMAL),
                "Organización de tareas y asignación de responsables",
                "Evaluación de avances y observaciones",
                "Coordinación de abastecimiento o reposición",
                "Seguimiento de acuerdos y compromisos",
                "Análisis de cumplimiento de metas",
                "Elaboración del informe semanal"));

        s.add(six(new Slot("12:00 - 12:30", m(12, 0), m(12, 29), Kind.NORMAL),
                "Coordinación interna con responsables",
                "Reunión breve de seguimiento",
                "Verificación de disponibilidad de recursos",
                "Revisión de ejecución por áreas",
                "Validación de indicadores clave",
                "Revisión final del reporte"));

        s.add(new Slot("12:30 - 13:00", m(12, 30), m(12, 59), Kind.NORMAL)
                .days("Atención de incidencias y pendientes", all));

        s.add(six(new Slot("13:00 - 13:30", m(13, 0), m(13, 29), Kind.NORMAL),
                "Reporte parcial de actividades", "Reporte parcial de supervisión",
                "Reporte parcial de gestión", "Reporte parcial de coordinación",
                "Reporte parcial de indicadores", "Consolidación de resultados"));

        s.add(new Slot("13:30 - 15:30", m(13, 30), m(15, 29), Kind.BREAK)
                .days("BREAK", all));

        s.add(six(new Slot("15:30 - 16:30", m(15, 30), m(16, 29), Kind.NORMAL),
                "Seguimiento al plan de trabajo",
                "Seguimiento a observaciones de equipos",
                "Seguimiento a solicitudes y recursos",
                "Seguimiento a coordinaciones realizadas",
                "Seguimiento a acciones correctivas",
                "Presentación o entrega del informe"));

        s.add(six(new Slot("16:30 - 17:30", m(16, 30), m(17, 29), Kind.NORMAL),
                "Verificación del cumplimiento del día",
                "Verificación de cumplimiento de áreas",
                "Validación del uso de recursos",
                "Verificación de cumplimiento de acuerdos",
                "Verificación del avance de KPIs",
                "Evaluación de resultados y mejoras"));

        s.add(new Slot("17:30 - 18:00", m(17, 30), m(17, 59), Kind.NORMAL)
                .days("Llenado de reporte", 0, 1, 2, 3, 4)
                .day(5, "Llenado de reporte semanal"));

        s.add(new Slot("18:00 - 18:30", m(18, 0), m(18, 30), Kind.EXIT)
                .days("Supervisión de salida", all));

        return s;
    }

    /** Metas principales por día. */
    static List<ScheduleKpi> goals() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Planificación semanal", "≥ 1 plan de trabajo", true, "Lunes"));
        k.add(new ScheduleKpi("Supervisión de equipos", "≥ 2 revisiones", true, "Martes"));
        k.add(new ScheduleKpi("Gestión de recursos", "≥ 2 gestiones", true, "Miércoles"));
        k.add(new ScheduleKpi("Coordinación interáreas", "≥ 2 coordinaciones", true, "Jueves"));
        k.add(new ScheduleKpi("Control de indicadores", "1 revisión de KPIs", true, "Viernes"));
        k.add(new ScheduleKpi("Reporte de gestión", "1 informe semanal", true, "Sábado"));
        k.add(new ScheduleKpi("Total semanal", "6", false, "actividades principales"));
        return k;
    }
}
