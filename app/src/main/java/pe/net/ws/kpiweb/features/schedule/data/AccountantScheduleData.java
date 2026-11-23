package pe.net.ws.kpiweb.features.schedule.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import pe.net.ws.kpiweb.features.schedule.model.Slot.Kind;

/** Horario del cargo Contador (Lun–Sáb, 8:00 a 18:30). */
final class AccountantScheduleData {

    private AccountantScheduleData() { }

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
                "Revisión de agenda y comprobantes",
                "Revisión de agenda y estados de cuenta",
                "Revisión de agenda y cartera",
                "Revisión de agenda y reportes",
                "Revisión de agenda tributaria",
                "Revisión de pendientes contables"));

        s.add(six(new Slot("8:30 - 9:30", m(8, 30), m(9, 29), Kind.NORMAL),
                "Clasificación de ingresos y egresos",
                "Revisión de movimientos bancarios",
                "Revisión de cuentas pendientes",
                "Recopilación de información financiera",
                "Revisión de cronograma SUNAT",
                "Consolidación de información semanal"));

        s.add(six(new Slot("9:30 - 10:30", m(9, 30), m(10, 29), Kind.NORMAL),
                "Registro de compras y ventas",
                "Conciliación bancaria",
                "Control de cuentas por pagar y cobrar",
                "Elaboración de reportes financieros",
                "Declaraciones y obligaciones tributarias",
                "Cierre y validación de información contable"));

        s.add(six(new Slot("10:30 - 12:00", m(10, 30), m(11, 59), Kind.NORMAL),
                "Procesamiento de comprobantes contables",
                "Verificación de cargos y abonos",
                "Registro de vencimientos y abonos",
                "Análisis de ingresos y egresos",
                "Verificación de comprobantes tributarios",
                "Verificación de libros y registros"));

        s.add(six(new Slot("12:00 - 12:30", m(12, 0), m(12, 29), Kind.NORMAL),
                "Verificación de documentos recibidos",
                "Cruce de cuentas y saldos",
                "Seguimiento a cuentas pendientes",
                "Consolidación de resultados contables",
                "Validación de tributos y retenciones",
                "Revisión final de saldos"));

        s.add(six(new Slot("12:30 - 13:00", m(12, 30), m(12, 59), Kind.NORMAL),
                "Atención de observaciones y pendientes",
                "Corrección de diferencias",
                "Coordinación de cobros y pagos",
                "Revisión de indicadores financieros",
                "Corrección de inconsistencias",
                "Respaldo de documentos contables"));

        s.add(six(new Slot("13:00 - 13:30", m(13, 0), m(13, 29), Kind.NORMAL),
                "Actualización de registros contables",
                "Registro de conciliación",
                "Actualización de estados de cuenta",
                "Actualización de cuadros de control",
                "Actualización de declaración",
                "Organización de evidencias"));

        s.add(new Slot("13:30 - 15:30", m(13, 30), m(15, 29), Kind.BREAK)
                .days("BREAK", all));

        s.add(six(new Slot("15:30 - 16:30", m(15, 30), m(16, 29), Kind.NORMAL),
                "Seguimiento de comprobantes pendientes",
                "Seguimiento de movimientos observados",
                "Seguimiento de cuentas observadas",
                "Validación del reporte mensual / interno",
                "Seguimiento de envío y constancias",
                "Elaboración del cierre semanal"));

        s.add(six(new Slot("16:30 - 17:30", m(16, 30), m(17, 29), Kind.NORMAL),
                "Validación de registros realizados",
                "Validación de saldos conciliados",
                "Validación de registros y saldos",
                "Ajustes y observaciones del informe",
                "Revisión de observaciones tributarias",
                "Validación final y observaciones"));

        s.add(six(new Slot("17:30 - 18:00", m(17, 30), m(17, 59), Kind.NORMAL),
                "Reporte de actividades contables",
                "Reporte de conciliación",
                "Reporte de cuentas por cobrar y pagar",
                "Entrega del reporte financiero",
                "Reporte de cumplimiento tributario",
                "Entrega del cierre semanal"));

        s.add(new Slot("18:00 - 18:30", m(18, 0), m(18, 30), Kind.EXIT)
                .days("Cierre de actividades", all));

        return s;
    }

    static List<ScheduleKpi> goals() {
        List<ScheduleKpi> k = new ArrayList<>();
        k.add(new ScheduleKpi("Registro de compras y ventas (Ingresos / Egresos)", "≥ 20 comprobantes", true, "Lunes"));
        k.add(new ScheduleKpi("Conciliación bancaria (Cuentas y movimientos)", "1 conciliación", true, "Martes"));
        k.add(new ScheduleKpi("Control de cuentas por pagar y por cobrar", "≥ 10 registros", true, "Miércoles"));
        k.add(new ScheduleKpi("Elaboración de reportes financieros (Mensual / Interno)", "1 reporte", true, "Jueves"));
        k.add(new ScheduleKpi("Declaraciones y obligaciones tributarias (SUNAT)", "1 declaración (según cronograma)", true, "Viernes"));
        k.add(new ScheduleKpi("Cierre y validación de información contable", "1 validación (semanal)", true, "Sábado"));
        k.add(new ScheduleKpi("Total semanal", "6", false, "actividades principales"));
        return k;
    }
}
