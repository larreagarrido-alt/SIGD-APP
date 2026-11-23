package pe.net.ws.kpiweb.features.admin.dashboard;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Clave de semana ISO aceptada por el API del Dashboard y formateo legible para humanos.
 * Respeta semanas ISO (lunes a domingo) incluso cuando cruzan meses o años.
 */
public final class AdminDashboardPeriod {

    private static final String[] MONTHS_FULL_ES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private static final String[] MONTHS_SHORT_ES = {
            "ene.", "feb.", "mar.", "abr.", "may.", "jun.",
            "jul.", "ago.", "sep.", "oct.", "nov.", "dic."
    };

    private AdminDashboardPeriod() { }

    public static String fromSelectedDate(Calendar selectedDate) {
        return pe.net.ws.kpiweb.core.time.IsoWeekPeriod.format(selectedDate);
    }

    public static int isoWeekNumber(Calendar selectedDate) {
        GregorianCalendar cal = toIsoCalendar(selectedDate);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static String formatWeekNumberLabel(Calendar selectedDate) {
        return "Semana " + isoWeekNumber(selectedDate);
    }

    public static String formatWeekRangeLong(Calendar selectedDate) {
        GregorianCalendar start = weekMonday(selectedDate);
        GregorianCalendar end = (GregorianCalendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        int d1 = start.get(Calendar.DAY_OF_MONTH);
        int m1 = start.get(Calendar.MONTH);
        int y1 = start.get(Calendar.YEAR);
        int d2 = end.get(Calendar.DAY_OF_MONTH);
        int m2 = end.get(Calendar.MONTH);
        int y2 = end.get(Calendar.YEAR);

        if (y1 == y2 && m1 == m2) {
            return d1 + "–" + d2 + " de " + MONTHS_FULL_ES[m1] + " de " + y1;
        }
        if (y1 == y2) {
            return d1 + " " + MONTHS_SHORT_ES[m1] + " – " + d2 + " " + MONTHS_SHORT_ES[m2] + " " + y1;
        }
        return d1 + " " + MONTHS_SHORT_ES[m1] + " " + y1 + " – " + d2 + " " + MONTHS_SHORT_ES[m2] + " " + y2;
    }

    public static String formatWeekSummarySubtitle(Calendar selectedDate) {
        GregorianCalendar start = weekMonday(selectedDate);
        GregorianCalendar end = (GregorianCalendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        int d1 = start.get(Calendar.DAY_OF_MONTH);
        int m1 = start.get(Calendar.MONTH);
        int y1 = start.get(Calendar.YEAR);
        int d2 = end.get(Calendar.DAY_OF_MONTH);
        int m2 = end.get(Calendar.MONTH);
        int y2 = end.get(Calendar.YEAR);

        String range;
        if (y1 == y2 && m1 == m2) {
            range = d1 + "–" + d2 + " " + MONTHS_SHORT_ES[m1] + " " + y1;
        } else if (y1 == y2) {
            range = d1 + " " + MONTHS_SHORT_ES[m1] + " – " + d2 + " " + MONTHS_SHORT_ES[m2] + " " + y1;
        } else {
            range = d1 + " " + MONTHS_SHORT_ES[m1] + " " + y1 + " – " + d2 + " " + MONTHS_SHORT_ES[m2] + " " + y2;
        }
        return formatWeekNumberLabel(selectedDate) + " · " + range;
    }

    private static GregorianCalendar toIsoCalendar(Calendar source) {
        GregorianCalendar cal = new GregorianCalendar(source.getTimeZone(), Locale.ROOT);
        cal.setTimeInMillis(source.getTimeInMillis());
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        return cal;
    }

    private static GregorianCalendar weekMonday(Calendar source) {
        GregorianCalendar cal = toIsoCalendar(source);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diffToMonday = dow == Calendar.SUNDAY ? -6 : (Calendar.MONDAY - dow);
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday);
        return cal;
    }
}
