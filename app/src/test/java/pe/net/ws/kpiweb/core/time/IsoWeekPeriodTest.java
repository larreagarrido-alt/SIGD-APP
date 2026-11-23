package pe.net.ws.kpiweb.core.time;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.junit.Test;

public class IsoWeekPeriodTest {
    @Test public void usesIsoWeekYearAtCalendarBoundary() {
        Calendar date = new GregorianCalendar(TimeZone.getTimeZone("America/Lima"));
        date.clear();
        date.set(2027, Calendar.JANUARY, 1);
        assertEquals("2026-W53", IsoWeekPeriod.format(date));
    }
}
