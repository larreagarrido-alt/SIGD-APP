package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleProfile;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/**
 * Horarios PROPUESTOS para los cargos que aún no tienen horario impreso. Las metas salen de las
 * hojas oficiales de KPI de cada cargo (metas, rutinas y criterios); las franjas por día son una
 * propuesta editable desde el Editor de Horarios.
 */
final class GeneratedScheduleData {

    private static final int[] ALL = {0, 1, 2, 3, 4, 5};

    private GeneratedScheduleData() { }

    private static int m(int h, int min) {
        return h * 60 + min;
    }

    private static Slot slot(String label, Kind kind, String uniform) {
        Slot s = new Slot();
        s.kind = kind;
        s.applyLabel(label);
        if (kind == Kind.EXIT) {
            s.endMin = s.endMin + 1; // la última franja incluye su minuto final
        }
        s.days(uniform, ALL);
        return s;
    }

    private static Slot six(String label, String lun, String mar, String mie, String jue,
            String vie, String sab) {
        Slot s = slot(label, Kind.NORMAL, "");
        s.day(0, lun).day(1, mar).day(2, mie).day(3, jue).day(4, vie).day(5, sab);
        return s;
    }

    private static ScheduleProfile profile(String id, String role, String subtitle, List<Slot> slots,
            String tab, List<ScheduleKpi> goals, String[] pillars) {
        ScheduleProfile p = new ScheduleProfile(id, role, subtitle, slots, tab, goals, null, null,
                pillars, "");
        p.factory = true;
        p.generated = true;
        return p;
    }

    static List<ScheduleProfile> all() {
        List<ScheduleProfile> l = new ArrayList<>();
        l.add(communityManager());
        l.add(salesAdvisor());
        l.add(graphicDesigner());
        l.add(seniorDesigner());
        l.add(programmerJunior());
        l.add(designerJunior());
        return l;
    }

    // ------------------------------------------------------------------ Community Manager
    private static ScheduleProfile communityManager() {
        List<Slot> s = new ArrayList<>();
        s.add(slot("8:00 - 8:30", Kind.NORMAL, "Revisión de agenda y calendario de contenidos"));
        s.add(six("8:30 - 9:30",
                "Programación de publicaciones del día", "Revisión de mensajes y comentarios",
                "Monitoreo de interacciones y alcance", "Revisión del calendario de contenidos",
                "Recopilación de métricas de la semana", "Consolidación de resultados de la semana"));
        s.add(six("9:30 - 10:30",
                "Publicación en Facebook e Instagram", "Respuesta a mensajes y consultas",
                "Seguimiento de interacción en redes", "Publicación de contenidos programados",
                "Análisis de alcance e interacción", "Elaboración del reporte semanal"));
        s.add(six("10:30 - 12:00",
                "Gestión de redes (programación y publicación)",
                "Atención a la comunidad (mensajes y comentarios)",
                "Seguimiento de redes (interacción y alcance)",
                "Gestión de publicaciones (según calendario)",
                "Revisión de métricas (alcance e interacción)",
                "Reporte semanal (resumen y avances)"));
        s.add(slot("12:00 - 13:00", Kind.NORMAL, "Interacción con la comunidad"));
        s.add(slot("13:00 - 13:30", Kind.NORMAL, "Registro de publicaciones e interacciones"));
        s.add(slot("13:30 - 15:30", Kind.BREAK, "BREAK"));
        s.add(slot("15:30 - 16:30", Kind.NORMAL, "Preparación y programación de contenidos"));
        s.add(slot("16:30 - 17:30", Kind.NORMAL, "Respuesta a pendientes de la comunidad"));
        s.add(slot("17:30 - 18:00", Kind.NORMAL, "Reporte de actividades del día"));
        s.add(slot("18:00 - 18:30", Kind.EXIT, "Cierre de actividades"));

        List<ScheduleKpi> g = new ArrayList<>();
        g.add(new ScheduleKpi("Gestión de redes (Programación y publicación)", "1 - 2 publicaciones", true, "Lunes"));
        g.add(new ScheduleKpi("Atención a la comunidad (Mensajes y comentarios)", "≥ 10 interacciones", true, "Martes"));
        g.add(new ScheduleKpi("Seguimiento de redes (Interacción y alcance)", "Monitoreo diario", true, "Miércoles"));
        g.add(new ScheduleKpi("Gestión de publicaciones (Según calendario)", "Contenidos programados", true, "Jueves"));
        g.add(new ScheduleKpi("Revisión de métricas (Alcance e interacción)", "1 análisis", true, "Viernes"));
        g.add(new ScheduleKpi("Reporte semanal (Resumen y avances)", "1 reporte", true, "Sábado"));
        g.add(new ScheduleKpi("Total semanal", "≥ 10 pub / ≥ 50 int", false, "Semanal"));
        return profile(DefaultIds.COMMUNITY, "Community Manager",
                "Community Manager · Gestiona redes · Publica · Interactúa · Analiza resultados",
                s, "Metas", g,
                new String[]{"Gestiona redes", "Publica", "Interactúa", "Analiza resultados"});
    }

    // ------------------------------------------------------------------ Asesor de Ventas
    private static ScheduleProfile salesAdvisor() {
        List<Slot> s = new ArrayList<>();
        s.add(slot("8:00 - 8:30", Kind.NORMAL, "Revisión de agenda, ruta y metas del día"));
        s.add(slot("8:30 - 9:30", Kind.NORMAL, "Preparación de contactos y material comercial"));
        String[] main = {"Llamadas comerciales", "Nuevas visitas a clientes y prospectos",
                "Elaboración y envío de cotizaciones", "Seguimiento a empresas en cotización",
                "Prospección de nuevos clientes", "Cierre de ventas y nuevos clientes"};
        s.add(six("9:30 - 10:30", main[0], main[1], main[2], main[3], main[4], main[5]));
        s.add(six("10:30 - 12:00", main[0], main[1], main[2], main[3], main[4], main[5]));
        s.add(slot("12:00 - 13:00", Kind.NORMAL, "Registro diario de metas y observaciones"));
        s.add(slot("13:00 - 13:30", Kind.NORMAL, "Actualización de hoja de ruta"));
        s.add(slot("13:30 - 15:30", Kind.BREAK, "BREAK"));
        s.add(slot("15:30 - 17:00", Kind.NORMAL, "Seguimiento comercial en campo"));
        s.add(slot("17:00 - 18:00", Kind.NORMAL, "Registro de metas diarias (llamadas, visitas, cotizaciones)"));
        s.add(slot("18:00 - 18:30", Kind.EXIT, "Cierre de actividades"));

        List<ScheduleKpi> g = new ArrayList<>();
        g.add(new ScheduleKpi("Llamadas comerciales", "100", true));
        g.add(new ScheduleKpi("Nuevas visitas", "60", true));
        g.add(new ScheduleKpi("Cotizaciones enviadas", "30", true));
        g.add(new ScheduleKpi("Empresas en seguimiento", "50", true));
        g.add(new ScheduleKpi("Nuevos prospectos", "15", true));
        g.add(new ScheduleKpi("Nuevos clientes", "3", true));
        return profile(DefaultIds.SALES, "Asesor de Ventas",
                "Asesor de Ventas · Llamadas · Visitas · Cotizaciones · Cierre",
                s, "Metas", g, new String[]{"Llamadas", "Visitas", "Cotizaciones", "Cierre"});
    }

    // ------------------------------------------------------------------ Diseñador Gráfico
    private static ScheduleProfile graphicDesigner() {
        String a = "Modalidad A: diseño gráfico";
        String b = "Modalidad B: audiovisual y diseño";
        List<Slot> s = new ArrayList<>();
        s.add(slot("8:00 - 8:30", Kind.NORMAL, "Revisión de briefs y prioridades del día"));
        s.add(six("8:30 - 10:30", a, b, a, b, a, "Sesión fotográfica (producción)"));
        s.add(six("10:30 - 12:30", a, b, a, b, a, "Sesión fotográfica (producción)"));
        s.add(slot("12:30 - 13:00", Kind.NORMAL, "Revisión y ajustes de piezas"));
        s.add(slot("13:00 - 13:30", Kind.EXIT, "Entrega y reporte de producción"));

        List<ScheduleKpi> g = new ArrayList<>();
        g.add(new ScheduleKpi("Modalidad A (Diseño gráfico)", "06 diseños", true, "Por turno"));
        g.add(new ScheduleKpi("Modalidad B (Audiovisual y diseño)", "02 videos + 02 diseños", true, "Por turno"));
        g.add(new ScheduleKpi("Sesión fotográfica (Producción)", "01 sesión", true, "Por turno"));
        g.add(new ScheduleKpi("Total semanal", "18 dis / 06 vid / 01 ses (25 act)", true));
        return profile(DefaultIds.DESIGNER, "Diseñador Gráfico",
                "Diseñador Gráfico · Planifica · Produce · Crea · Genera resultados",
                s, "Metas", g, new String[]{"Planifica", "Produce", "Crea", "Genera resultados"});
    }

    // ------------------------------------------------------------------ Diseñador Senior
    private static ScheduleProfile seniorDesigner() {
        List<Slot> s = new ArrayList<>();
        s.add(slot("8:00 - 8:15", Kind.ENTRY, "Ingreso y organización"));
        s.add(slot("8:15 - 9:00", Kind.NORMAL, "Revisión de briefs"));
        s.add(slot("9:00 - 10:00", Kind.NORMAL, "Producción creativa"));
        s.add(slot("10:00 - 10:30", Kind.NORMAL, "Revisión y ajustes"));
        s.add(slot("10:30 - 12:00", Kind.NORMAL, "Producción creativa"));
        s.add(slot("12:00 - 13:00", Kind.NORMAL, "Supervisión de equipo"));
        s.add(slot("13:00 - 13:30", Kind.EXIT, "Entrega y reportes"));

        List<ScheduleKpi> g = new ArrayList<>();
        g.add(new ScheduleKpi("Producción propia (Audiovisual y diseño)", "4 vid + 2 dis", true, "Meta del día"));
        g.add(new ScheduleKpi("Producción propia (Audiovisual y diseño)", "2 vid + 1 dis", true, "Meta del día"));
        g.add(new ScheduleKpi("Producción propia (Diseño y sesión)", "2 dis + 1 ses", true, "Meta del día"));
        g.add(new ScheduleKpi("Supervisión de equipo", "Producción · Video llamada · Feedback", true));
        g.add(new ScheduleKpi("Indicadores clave (KPI)", "Calidad de producción · Productividad", true));
        return profile(DefaultIds.SENIOR, "Diseñador Senior",
                "Diseñador Senior · Produce · Supervisa · Coordina · Garantiza calidad",
                s, "Metas", g, new String[]{"Produce", "Supervisa", "Coordina", "Garantiza calidad"});
    }

    // ------------------------------------------------------------------ Programador Junior
    private static ScheduleProfile programmerJunior() {
        String[] main = {"Desarrollo de funcionalidad", "Corrección de incidencias", "Mejoras del sistema",
                "Desarrollo de interfaz (frontend)", "API e integraciones", "Mantenimiento y soporte"};
        List<Slot> s = new ArrayList<>();
        s.add(slot("8:00 - 8:30", Kind.NORMAL, "Ingreso y revisión de tareas asignadas"));
        s.add(six("8:30 - 10:30", main[0], main[1], main[2], main[3], main[4], main[5]));
        s.add(six("10:30 - 12:30", main[0], main[1], main[2], main[3], main[4], main[5]));
        s.add(slot("12:30 - 13:00", Kind.NORMAL, "Pruebas y verificación de lo desarrollado"));
        s.add(slot("13:00 - 13:30", Kind.EXIT, "Registro de actividades y reporte diario"));

        List<ScheduleKpi> g = new ArrayList<>();
        g.add(new ScheduleKpi("Desarrollo", "1 funcionalidad", true, "Meta diaria"));
        g.add(new ScheduleKpi("Corrección", "2 a 4 incidencias", true, "Meta diaria"));
        g.add(new ScheduleKpi("Mejoras", "2 a 4 tareas", true, "Meta diaria"));
        g.add(new ScheduleKpi("Frontend", "1 módulo", true, "Meta diaria"));
        g.add(new ScheduleKpi("API / Integración", "1 avance", true, "Meta diaria"));
        g.add(new ScheduleKpi("Mantenimiento", "2 a 4 tareas", true, "Meta diaria"));
        return profile(DefaultIds.PROGRAMMER, "Programador Junior",
                "Programador Junior · Desarrollo · Corrección · Mejoras · Integración",
                s, "Metas", g, new String[]{"Desarrollo", "Corrección", "Mejoras", "Integración"});
    }

    // ------------------------------------------------------------------ Diseñador Junior
    private static ScheduleProfile designerJunior() {
        String d = "Diseño gráfico (posts)";
        String v = "Audiovisual (videos cortos)";
        String x = "Mixta (video y posts)";
        List<Slot> s = new ArrayList<>();
        s.add(slot("8:00 - 8:30", Kind.NORMAL, "Ingreso y revisión de briefs"));
        s.add(six("8:30 - 10:30", d, v, x, d, v, x));
        s.add(six("10:30 - 12:30", d, v, x, d, v, x));
        s.add(slot("12:30 - 13:00", Kind.NORMAL, "Correcciones y ajustes de piezas"));
        s.add(slot("13:00 - 13:30", Kind.EXIT, "Registro de actividades y entrega"));

        List<ScheduleKpi> g = new ArrayList<>();
        g.add(new ScheduleKpi("Diseño gráfico (4 posts)", "4 piezas RRSS", true, "Meta diaria"));
        g.add(new ScheduleKpi("Audiovisual (2 videos)", "2 videos cortos", true, "Meta diaria"));
        g.add(new ScheduleKpi("Mixta (1 video + 2 posts)", "3 piezas totales", true, "Meta diaria"));
        return profile(DefaultIds.DESIGNER_JUNIOR, "Diseñador Junior",
                "Diseñador Junior · Diseña · Produce · Entrega · Mejora",
                s, "Metas", g, new String[]{"Diseña", "Produce", "Entrega", "Mejora"});
    }
}
