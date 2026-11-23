package pe.net.ws.kpiweb.core.time;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** API period key: ISO week year and ISO week number, e.g. 2026-W53. */
public final class IsoWeekPeriod {
    private IsoWeekPeriod() {}

    public static String currentInLima() {
        return format(Calendar.getInstance(TimeZone.getTimeZone("America/Lima")));
    }

    public static String format(Calendar selectedDate) {
        GregorianCalendar date = new GregorianCalendar(selectedDate.getTimeZone(), Locale.ROOT);
        date.setTimeInMillis(selectedDate.getTimeInMillis());
        date.setFirstDayOfWeek(Calendar.MONDAY);
        date.setMinimalDaysInFirstWeek(4);
        return String.format(Locale.ROOT, "%04d-W%02d", date.getWeekYear(), date.get(Calendar.WEEK_OF_YEAR));
    }
}
