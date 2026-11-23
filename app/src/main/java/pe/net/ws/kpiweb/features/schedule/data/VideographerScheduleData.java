package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Horario del cargo Videógrafo (Lun–Sáb, 8:00 a 18:30). */
final class VideographerScheduleData {

    private VideographerScheduleData() { }

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
                "Revisión de agenda y material",
                "Revisión de agenda y requerimientos",
                "Revisión de agenda y locación",
                "Revisión de agenda de edición",
                "Revisión de entregas"));

        s.add(six(new Slot("8:30 - 9:30", m(8, 30), m(9, 29), Kind.NORMAL),
                "Planificación de grabación",
                "Preparación de proyecto",
                "Planificación de reel",
                "Preparación de grabación cliente",
                "Preparación del proyecto final",
                "Organización de archivos"));

        s.add(six(new Slot("9:30 - 10:30", m(9, 30), m(10, 29), Kind.NORMAL),
                "Grabación de contenido",
                "Edición de video",
                "Producción de reel",
                "Video de cliente",
                "Edición y gráfica",
                "Entrega y respaldo"));

        s.add(six(new Slot("10:30 - 12:00", m(10, 30), m(11, 59), Kind.NORMAL),
                "Toma de video campaña redes",
                "Postproducción y montaje",
                "Grabación para redes sociales",
                "Grabación servicio / testimonio",
                "Color, audio y títulos",
                "Preparación de publicación"));

        s.add(six(new Slot("12:00 - 12:30", m(12, 0), m(12, 29), Kind.NORMAL),
                "Revisión de tomas",
                "Ajustes de audio",
                "Selección de clips",
                "Revisión de tomas clave",
                "Revisión de detalles",
                "Verificación de archivos"));

        s.add(six(new Slot("12:30 - 13:00", m(12, 30), m(12, 59), Kind.NORMAL),
                "Registro de material",
                "Corrección de cortes",
                "Edición breve de reel",
                "Corrección de encuadre",
                "Correcciones finales",
                "Carga y respaldo en Drive"));

        s.add(six(new Slot("13:00 - 13:30", m(13, 0), m(13, 29), Kind.NORMAL),
                "Reporte parcial de grabación",
                "Registro de avances",
                "Registro de material",
                "Registro de observaciones",
                "Registro de cambios",
                "Registro de entrega"));

        s.add(new Slot("13:30 - 15:30", m(13, 30), m(15, 29), Kind.BREAK)
                .days("BREAK", all));

        s.add(six(new Slot("15:30 - 16:30", m(15, 30), m(16, 29), Kind.NORMAL),
                "Selección inicial de clips",
                "Continuación de edición",
                "Edición y ritmo del reel",
                "Ajustes y correcciones",
                "Exportación del video",
                "Revisión de publicación final"));

        s.add(six(new Slot("16:30 - 17:30", m(16, 30), m(17, 29), Kind.NORMAL),
                "Organización de archivos",
                "Revisión de calidad",
                "Revisión final del reel",
                "Organización de clips finales",
                "Validación de versión final",
                "Consolidación semanal"));

        s.add(six(new Slot("17:30 - 18:00", m(17, 30), m(17, 59), Kind.NORMAL),
                "Respaldo de material",
                "Exportación preliminar",
                "Exportación y respaldo",
                "Respaldo de material",
                "Respaldo del proyecto",
                "Informe de entrega"));

        s.add(new Slot("18:00 - 18:30", m(18, 0), m(18, 30), Kind.EXIT)
                .days("Cierre de actividades", all));

        return s;
    }

    static List<ScheduleKpi> goals() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Grabación de contenido (Campaña redes)", "1 video (≥ 3 min)", true, "Lunes"));
        k.add(new ScheduleKpi("Edición de video (Postproducción)", "1 video (≥ 3 min)", true, "Martes"));
        k.add(new ScheduleKpi("Producción de reel (Redes sociales)", "1 reel (30 - 60 seg)", true, "Miércoles"));
        k.add(new ScheduleKpi("Video de cliente (Servicio / Testimonio)", "1 video (≥ 2 min)", true, "Jueves"));
        k.add(new ScheduleKpi("Edición y gráfica (Color, audio, títulos)", "1 video (≥ 2 min)", true, "Viernes"));
        k.add(new ScheduleKpi("Entrega y respaldo (Archivo y publicación)", "1 entrega (≥ 1 video)", true, "Sábado"));
        k.add(new ScheduleKpi("Total semanal", "6", false, "actividades principales"));
        return k;
    }
}
