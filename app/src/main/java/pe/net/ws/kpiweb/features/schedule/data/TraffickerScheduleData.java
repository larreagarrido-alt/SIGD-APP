package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Horario del cargo Trafficker Digital (Lun–Sáb, 8:00 a 18:30). */
final class TraffickerScheduleData {

    private TraffickerScheduleData() { }

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

        s.add(six(new Slot("8:00 - 8:30", m(8, 0), m(8, 29), Kind.NORMAL),
                "Revisión de agenda y KPIs",
                "Revisión de agenda y pendientes",
                "Revisión de agenda y áreas",
                "Revisión de agenda y recursos",
                "Revisión de agenda y SIGD",
                "Revisión de agenda semanal"));

        s.add(six(new Slot("8:30 - 9:30", m(8, 30), m(9, 29), Kind.NORMAL),
                "Análisis de indicadores prioritarios",
                "Definición de asuntos prioritarios",
                "Revisión de avances por área",
                "Revisión de presupuesto y gastos",
                "Verificación de incidencias",
                "Consolidación de avances"));

        s.add(six(new Slot("9:30 - 10:30", m(9, 30), m(10, 29), Kind.NORMAL),
                "Seguimiento de objetivos",
                "Gestión estratégica",
                "Seguimiento de áreas",
                "Control de recursos",
                "Cumplimiento del SIGD",
                "Revisión general y coordinación"));

        s.add(six(new Slot("10:30 - 12:00", m(10, 30), m(11, 59), Kind.NORMAL),
                "Evaluación de KPIs comerciales",
                "Resolución de asuntos priorizados",
                "Coordinación con áreas programadas",
                "Validación de compromisos presupuestarios",
                "Seguimiento de acciones pendientes",
                "Planificación de la siguiente semana"));

        s.add(six(new Slot("12:00 - 12:30", m(12, 0), m(12, 29), Kind.NORMAL),
                "Alineación de metas",
                "Seguimiento de decisiones estratégicas",
                "Revisión de resultados parciales",
                "Control de disponibilidad de recursos",
                "Revisión de cumplimiento interno",
                "Revisión de prioridades"));

        s.add(six(new Slot("12:30 - 13:00", m(12, 30), m(12, 59), Kind.NORMAL),
                "Registro de hallazgos y pendientes",
                "Registro de acuerdos",
                "Registro de observaciones por área",
                "Registro de gastos y control",
                "Registro de incidencias",
                "Registro de avances consolidados"));

        s.add(six(new Slot("13:00 - 13:30", m(13, 0), m(13, 29), Kind.NORMAL),
                "Reporte parcial de objetivos",
                "Reporte parcial estratégico",
                "Reporte parcial de áreas",
                "Reporte parcial de recursos",
                "Reporte parcial de cumplimiento",
                "Reporte parcial semanal"));

        s.add(new Slot("13:30 - 15:30", m(13, 30), m(15, 29), Kind.BREAK)
                .days("BREAK", all));

        s.add(six(new Slot("15:30 - 16:30", m(15, 30), m(16, 29), Kind.NORMAL),
                "Seguimiento a indicadores observados",
                "Ejecución de acciones estratégicas",
                "Seguimiento a compromisos de áreas",
                "Supervisión de uso de recursos",
                "Validación de acciones correctivas",
                "Organización del plan semanal"));

        s.add(six(new Slot("16:30 - 17:30", m(16, 30), m(17, 29), Kind.NORMAL),
                "Verificación del avance del día",
                "Validación de resultados estratégicos",
                "Verificación del cumplimiento por área",
                "Validación del control presupuestario",
                "Verificación del cumplimiento del SIGD",
                "Revisión final de avances"));

        s.add(six(new Slot("17:30 - 18:00", m(17, 30), m(17, 59), Kind.NORMAL),
                "Informe de indicadores",
                "Informe de gestión estratégica",
                "Informe de seguimiento de áreas",
                "Informe de control de recursos",
                "Informe de cumplimiento e incidencias",
                "Informe de planificación semanal"));

        s.add(new Slot("18:00 - 18:30", m(18, 0), m(18, 30), Kind.EXIT)
                .days("Cierre de actividades", all));

        return s;
    }

    static List<ScheduleKpi> goals() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Seguimiento de objetivos", "Revisar 2 - 3 indicadores prioritarios", true, "Lunes"));
        k.add(new ScheduleKpi("Gestión estratégica", "Resolver 1 - 2 asuntos prioritarios", true, "Martes"));
        k.add(new ScheduleKpi("Seguimiento de áreas", "Revisar avances de las áreas programadas", true, "Miércoles"));
        k.add(new ScheduleKpi("Control de recursos", "Verificar compromisos presupuestarios del día", true, "Jueves"));
        k.add(new ScheduleKpi("Cumplimiento del SIGD", "Revisar incidencias y acciones pendientes", true, "Viernes"));
        k.add(new ScheduleKpi("Revisión general y coordinación", "Consolidar avances y planificar la siguiente semana", true, "Sábado"));
        k.add(new ScheduleKpi("Total semanal", "6", false, "actividades principales"));
        return k;
    }
}
