package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleLevel;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleProfile;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Datos del cargo CEO – Gerencia. Fuente única, lista para ser reemplazada por datos editables. */
public final class ScheduleData {

    public static final String[] DAYS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};
    public static final String[] DAYS_FULL =
            {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    private static final int LUN = 0, MAR = 1, MIE = 2, JUE = 3, VIE = 4, SAB = 5;
    private static final int[] ALL = {LUN, MAR, MIE, JUE, VIE, SAB};
    private static final int[] LV = {LUN, MAR, MIE, JUE, VIE};

    private ScheduleData() { }

    /** Los 13 cargos oficiales, en el orden en que se muestran en los selectores. */
    public static List<ScheduleProfile> defaultProfiles() {
        List<ScheduleProfile> p = new ArrayList<>();

        ScheduleProfile ceo = new ScheduleProfile(
                DefaultIds.GERENTE, "Gerente General",
                "CEO – Gerencia · Planificación · Supervisión · Estrategia · Resultados",
                buildSlots(), "KPIs", buildKpis(), buildLevels(),
                "La disciplina de hoy, construye los resultados de mañana.",
                new String[]{"Planificar", "Supervisar", "Estrategia", "Resultados"},
                "Disciplina hoy, grandes resultados mañana");
        ceo.factory = true;
        p.add(ceo);

        p.add(factory(DefaultIds.ADMIN, "Administrador",
                "Administrador · Planificación · Supervisión · Control · Resultados",
                AdministratorScheduleData.slots(), AdministratorScheduleData.goals(),
                new String[]{"Planificación", "Supervisión", "Control", "Resultados"}));
        p.add(factory(DefaultIds.ADMIN_ASSISTANT, "Asistente Administrativo",
                "Asistente Administrativo · Planificación · Supervisión · Control · Resultados",
                AdminAssistantScheduleData.slots(), AdminAssistantScheduleData.goals(),
                new String[]{"Planificación", "Supervisión", "Control", "Resultados"}));
        p.add(factory(DefaultIds.ACCOUNTANT, "Contador",
                "Contador · Contabilidad · Conciliación · Reportes · Tributos · Validación",
                AccountantScheduleData.slots(), AccountantScheduleData.goals(),
                new String[]{"Contabilidad", "Conciliación", "Reportes", "Tributos", "Validación"}));
        p.add(factory(DefaultIds.PHOTOGRAPHER, "Fotógrafo",
                "Fotógrafo · Producción · Cobertura · Edición · Branding · Respaldo",
                PhotographerScheduleData.slots(), PhotographerScheduleData.goals(),
                new String[]{"Producción", "Cobertura", "Edición", "Branding", "Respaldo"}));
        p.add(factory(DefaultIds.TRAFFICKER, "Trafficker Digital",
                "Trafficker Digital · Objetivos · Estrategia · Áreas · Recursos · SIGD",
                TraffickerScheduleData.slots(), TraffickerScheduleData.goals(),
                new String[]{"Objetivos", "Estrategia", "Áreas", "Recursos", "SIGD"}));
        p.add(factory(DefaultIds.VIDEOGRAPHER, "Videógrafo",
                "Videógrafo · Grabación · Edición · Reels · Postproducción · Entrega",
                VideographerScheduleData.slots(), VideographerScheduleData.goals(),
                new String[]{"Grabación", "Edición", "Reels", "Postproducción", "Entrega"}));

        p.addAll(GeneratedScheduleData.all());
        return p;
    }

    private static ScheduleProfile factory(String id, String role, String subtitle, List<Slot> slots,
            List<ScheduleKpi> goals, String[] pillars) {
        ScheduleProfile pr = new ScheduleProfile(id, role, subtitle, slots, "Metas", goals, null,
                null, pillars, "");
        pr.factory = true;
        return pr;
    }

    /** Plantilla en blanco con las franjas estándar de la jornada (para "Nuevo horario"). */
    public static ScheduleProfile blankProfile(String id, String roleName) {
        String[][] ranges = {
                {"8:00 - 8:30", "N"}, {"8:30 - 9:30", "N"}, {"9:30 - 10:30", "N"},
                {"10:30 - 12:00", "N"}, {"12:00 - 12:30", "N"}, {"12:30 - 13:00", "N"},
                {"13:00 - 13:30", "N"}, {"13:30 - 15:30", "B"}, {"15:30 - 16:30", "N"},
                {"16:30 - 17:30", "N"}, {"17:30 - 18:00", "N"}, {"18:00 - 18:30", "E"}};
        List<Slot> slots = new ArrayList<>();
        for (String[] r : ranges) {
            Slot s = new Slot();
            s.applyLabel(r[0]);
            s.kind = "B".equals(r[1]) ? Slot.Kind.BREAK : "E".equals(r[1]) ? Slot.Kind.EXIT : Slot.Kind.NORMAL;
            if (s.kind == Slot.Kind.EXIT) s.endMin = s.endMin + 1;
            if (s.kind == Slot.Kind.BREAK) s.days("BREAK", ALL);
            slots.add(s);
        }
        ScheduleProfile p = new ScheduleProfile(id, roleName, roleName, slots, "Metas",
                new ArrayList<ScheduleKpi>(), null, null, new String[0], "");
        return p;
    }
    private static int m(int h, int min) {
        return h * 60 + min;
    }

    public static List<Slot> buildSlots() {
        List<Slot> s = new ArrayList<>();

        s.add(new Slot("8:00 - 8:05", m(8, 0), m(8, 5), Kind.ENTRY)
                .days("Supervisión de entrada", ALL));

        s.add(new Slot("8:06 - 8:29", m(8, 6), m(8, 29), Kind.NORMAL)
                .day(LUN, "Compromisos, incidencias, asuntos prioritarios de la empresa")
                .days("Revisión de agenda, KPIs y pendientes", MAR, JUE)
                .days("Revisión de resultados y prioridades", MIE, VIE)
                .day(SAB, "Preparación de reunión y resultados semanales"));

        s.add(new Slot("8:30 - 8:59", m(8, 30), m(8, 59), Kind.NORMAL)
                .days("Reunión con jefaturas", LUN, SAB)
                .day(MAR, "Supervisión operativa de áreas y seguimiento de responsables")
                .day(MIE, "Seguimiento operativo y reuniones con clientes / prospectos")
                .day(JUE, "Supervisión de producción y marketing")
                .day(VIE, "Evaluación de ventas y cobranzas"));

        s.add(new Slot("9:00 - 10:29", m(9, 0), m(10, 29), Kind.NORMAL)
                .day(LUN, "Revisión de KPIs y planificación semanal")
                .day(MAR, "Supervisión operativa de áreas y seguimiento de responsables")
                .day(MIE, "Seguimiento operativo y reuniones con clientes / prospectos")
                .day(JUE, "Supervisión de producción y marketing")
                .day(VIE, "Evaluación de ventas y cobranzas")
                .day(SAB, "Preparación de reunión y resultados semanales"));

        s.add(new Slot("10:30 - 12:29", m(10, 30), m(12, 29), Kind.NORMAL)
                .days("Trabajo en el campo · Visita de prospectos", LUN, MIE, SAB)
                .days("Coordinación interna, proyectos y clientes", MAR, JUE)
                .day(VIE, "Visitas a clientes actuales · Supervisión de calidad de servicios, "
                        + "encuestas, reportes de segmentación, etc."));

        s.add(new Slot("12:30 - 12:59", m(12, 30), m(12, 59), Kind.NORMAL)
                .days("Consolidación de incidencias y pendientes", ALL));

        s.add(new Slot("13:00 - 13:14", m(13, 0), m(13, 14), Kind.NORMAL)
                .days("Supervisión de reporte comercial y creativo", ALL));

        s.add(new Slot("13:15 - 13:29", m(13, 15), m(13, 29), Kind.NORMAL)
                .days("Llenado de reporte ejecutivo de producción", ALL));

        s.add(new Slot("13:30", m(13, 30), m(13, 44), Kind.EXIT)
                .days("Supervisión de salida", ALL));

        s.add(new Slot("13:45 - 15:29", m(13, 45), m(15, 29), Kind.BREAK)
                .days("BREAK", LV));
        s.add(new Slot("13:45 - 15:29", m(13, 45), m(15, 29), Kind.OFF)
                .day(SAB, "NO LABORABLE"));

        s.add(new Slot("15:30 - 16:49", m(15, 30), m(16, 49), Kind.NORMAL)
                .days("Trabajo comercial y seguimiento de proyectos", LV));

        s.add(new Slot("17:00 - 18:29", m(17, 0), m(18, 29), Kind.NORMAL)
                .days("Trabajo en el campo · Visita de prospectos", LV));

        s.add(new Slot("18:30", m(18, 30), m(18, 59), Kind.EXIT)
                .days("Supervisión de salida", LV));

        return s;
    }

    public static List<ScheduleKpi> buildKpis() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Cumplimiento de objetivos comerciales", "≥ 90%", true));
        k.add(new ScheduleKpi("Generación de oportunidades (prospectos)", "≥ 20", true));
        k.add(new ScheduleKpi("Conversión de ventas", "≥ 30%", true));
        k.add(new ScheduleKpi("Cumplimiento de proyectos / entregas", "≥ 95%", true));
        k.add(new ScheduleKpi("Satisfacción de clientes (NPS)", "≥ 8/10", false));
        k.add(new ScheduleKpi("Rentabilidad / crecimiento", "Según meta", false));
        k.add(new ScheduleKpi("Supervisión de áreas", "100%", true));
        return k;
    }

    public static List<ScheduleLevel> buildLevels() {
        List<ScheduleLevel> l = new ArrayList<>();
        l.add(new ScheduleLevel("95% - 100%", "Excelente", "#16A34A",
                "Supera los objetivos. Desempeño sobresaliente."));
        l.add(new ScheduleLevel("90% - 94%", "Muy Bueno", "#2563EB",
                "Cumple con los objetivos de forma sólida."));
        l.add(new ScheduleLevel("80% - 89%", "Bueno", "#EAB308",
                "Cumple con los objetivos con oportunidades de mejora."));
        l.add(new ScheduleLevel("70% - 79%", "Regular", "#F97316",
                "Cumplimiento parcial. Requiere seguimiento."));
        l.add(new ScheduleLevel("< 70%", "Deficiente", "#DC2626",
                "No cumple con los objetivos. Requiere plan de acción."));
        return l;
    }
}
