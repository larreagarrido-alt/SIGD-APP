package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Horario del cargo Fotógrafo (Lun–Sáb, 8:00 a 18:30). */
final class PhotographerScheduleData {

    private PhotographerScheduleData() { }

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
                "Revisión de agenda y equipos",
                "Revisión de agenda y locaciones",
                "Revisión de agenda y requerimientos",
                "Revisión de agenda y material",
                "Revisión de agenda de branding",
                "Revisión de entregas y pendientes"));

        s.add(six(new Slot("8:30 - 9:30", m(8, 30), m(9, 29), Kind.NORMAL),
                "Planificación de la sesión de producción",
                "Coordinación de cobertura en oficina",
                "Coordinación de sesión de cliente",
                "Preparación del set y equipos",
                "Definición de conceptos (branding interno)",
                "Organización de archivos y entregas"));

        s.add(six(new Slot("9:30 - 10:30", m(9, 30), m(10, 29), Kind.NORMAL),
                "Montaje del set de fotografía",
                "Cobertura fotográfica de actividades",
                "Sesión fotográfica de cliente externo",
                "Toma de fotografías de producto/servicio",
                "Sesión creativa (branding / interna)",
                "Revisión y selección de fotografías"));

        s.add(six(new Slot("10:30 - 12:00", m(10, 30), m(11, 59), Kind.NORMAL),
                "Toma de fotografías (producción)",
                "Captura de fotos institucionales",
                "Toma de fotografías según requerimiento",
                "Edición y selección inicial de fotos",
                "Captura de material complementario",
                "Edición final de fotografías"));

        s.add(six(new Slot("12:00 - 12:30", m(12, 0), m(12, 29), Kind.NORMAL),
                "Respaldo de fotografías capturadas",
                "Organización de archivos por proyecto",
                "Respaldo y clasificación de fotos",
                "Revisión de la calidad del material",
                "Clasificación de fotos por campaña",
                "Validación de fotos para entrega"));

        s.add(six(new Slot("12:30 - 13:00", m(12, 30), m(12, 59), Kind.NORMAL),
                "Revisión de avances de la sesión",
                "Ajustes de iluminación y encuadre",
                "Selección preliminar de fotografías",
                "Corrección básica de imágenes",
                "Ajustes de color y estilo",
                "Preparación del paquete final"));

        s.add(six(new Slot("13:00 - 13:30", m(13, 0), m(13, 29), Kind.NORMAL),
                "Registro de la sesión y observaciones",
                "Coordinación de próximas coberturas",
                "Registro de entregables",
                "Documentación de la sesión",
                "Revisión de lineamientos de branding",
                "Cierre de pendientes de la semana"));

        s.add(new Slot("13:30 - 15:30", m(13, 30), m(15, 29), Kind.BREAK)
                .days("BREAK", all));

        s.add(six(new Slot("15:30 - 16:30", m(15, 30), m(16, 29), Kind.NORMAL),
                "Edición y postproducción",
                "Edición de fotografías capturadas",
                "Edición y retoque de la sesión",
                "Postproducción del material",
                "Edición del material de branding",
                "Entrega y respaldo en Drive"));

        s.add(six(new Slot("16:30 - 17:30", m(16, 30), m(17, 29), Kind.NORMAL),
                "Selección final de fotografías",
                "Revisión y ajustes de edición",
                "Optimización y exportación de fotos",
                "Selección y organización final",
                "Revisión y aprobación del material",
                "Validación del material entregado"));

        s.add(six(new Slot("17:30 - 18:00", m(17, 30), m(17, 59), Kind.NORMAL),
                "Preparación de entrega (Drive / Nube)",
                "Subida y respaldo del material",
                "Entrega del material al área solicitante",
                "Registro de entregables",
                "Cierre del material de la semana",
                "Informe de entregas y respaldo final"));

        s.add(new Slot("18:00 - 18:30", m(18, 0), m(18, 30), Kind.EXIT)
                .days("Cierre de actividades", all));

        return s;
    }

    static List<ScheduleKpi> goals() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Sesión de producto (Bants para campaña)", "≥ 1 sesión (≥ 45 fotos)", true, "Lunes"));
        k.add(new ScheduleKpi("Cobertura de redes (Concesión de oficina)", "≥ 1 sesión (≥ 20 fotos)", true, "Martes"));
        k.add(new ScheduleKpi("Sesión de cliente (Cliente externo)", "≥ 1 sesión (≥ 40 fotos)", true, "Miércoles"));
        k.add(new ScheduleKpi("Edición y redacción (Postproducción)", "≥ 30 fotos editadas", true, "Jueves"));
        k.add(new ScheduleKpi("Sesión creativa (Branding / Interna)", "≥ 1 sesión (≥ 30 fotos)", true, "Viernes"));
        k.add(new ScheduleKpi("Organización y respaldo (Archivo y entregas)", "≥ 1 entrega (≥ 20 fotos)", true, "Sábado"));
        k.add(new ScheduleKpi("Total semanal", "6", false, "actividades principales"));
        return k;
    }
}
