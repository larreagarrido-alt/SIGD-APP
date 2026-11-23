package pe.net.ws.kpiweb.features.kpi.community.model;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;

public class KpiCommunityWeekReportTest {
    private static KpiCommunityWeekReport withActivities(int activities) {
        KpiCommunityDayRecord day = new KpiCommunityDayRecord("", "", "", "", "", 1);
        day.setOtrasActividadesCount(activities);
        KpiCommunityWeekReport report = new KpiCommunityWeekReport("", 1, "");
        report.setDays(Collections.singletonList(day));
        return report;
    }

    @Test public void classifiesByPercentageInsteadOfOldActivityThreshold() {
        KpiCommunityWeekReport below = withActivities(58);
        assertEquals(89, below.getProductivityPercentage());
        assertEquals("MUY BUENO", below.getProductivityLevel());

        KpiCommunityWeekReport above = withActivities(59);
        assertEquals(91, above.getProductivityPercentage());
        assertEquals("EXCELENTE", above.getProductivityLevel());

        assertEquals("DEFICIENTE", withActivities(0).getProductivityLevel());
    }
}
