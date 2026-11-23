package pe.net.ws.kpiweb.features.admin.dashboard;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class AdminDashboardPeriodTest {

    @Test
    public void formatsSameMonthWeekCleanlyWithoutTechnicalCodes() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/Lima"));
        cal.set(2026, Calendar.NOVEMBER, 18, 10, 0, 0);

        assertEquals("2026-W47", AdminDashboardPeriod.fromSelectedDate(cal));
        assertEquals("Semana 47", AdminDashboardPeriod.formatWeekNumberLabel(cal));
        assertEquals("16–22 de noviembre de 2026", AdminDashboardPeriod.formatWeekRangeLong(cal));
        assertEquals("Semana 47 · 16–22 nov. 2026", AdminDashboardPeriod.formatWeekSummarySubtitle(cal));
    }

    @Test
    public void handlesWeeksCrossingMonthsAndYearsAccurately() {
        Calendar crossMonth = new GregorianCalendar(TimeZone.getTimeZone("America/Lima"));
        crossMonth.set(2026, Calendar.NOVEMBER, 30, 10, 0, 0);
        assertEquals("30 nov. – 6 dic. 2026", AdminDashboardPeriod.formatWeekRangeLong(crossMonth));

        Calendar crossYear = new GregorianCalendar(TimeZone.getTimeZone("America/Lima"));
        crossYear.set(2025, Calendar.DECEMBER, 31, 10, 0, 0);
        assertEquals("29 dic. 2025 – 4 ene. 2026", AdminDashboardPeriod.formatWeekRangeLong(crossYear));
    }
}
