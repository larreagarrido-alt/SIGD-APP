package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminRankingSummaryHelper.EmptyKind;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminRankingSummaryHelper.SummaryStats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminRankingSummaryHelperTest {

    @Test
    public void computeStats_returnsDashesWhenNoClosedPeriodExists() {
        SummaryStats stats = AdminRankingSummaryHelper.computeStats(true, "", Collections.emptyList());
        assertEquals("—", stats.collaboratorsText);
        assertEquals("—", stats.evaluatedText);
        assertEquals("—", stats.classifiedText);
        assertFalse(stats.hasData);
    }

    @Test
    public void computeStats_distinguishesParticipantsEvaluatedAndClassified() {
        JsonObject r1 = new JsonObject();
        r1.addProperty("puesto", 1);
        r1.addProperty("kpis_aprobados", 4);
        r1.addProperty("porcentaje_cumplimiento", 95.0);

        JsonObject r2 = new JsonObject();
        r2.addProperty("puesto", 2);
        r2.addProperty("kpis_aprobados", 0);
        r2.addProperty("porcentaje_cumplimiento", 0.0);

        SummaryStats stats = AdminRankingSummaryHelper.computeStats(true, "2026-11", Arrays.asList(r1, r2));
        assertEquals("2", stats.collaboratorsText);
        assertEquals("1", stats.evaluatedText);
        assertEquals("2", stats.classifiedText);
        assertTrue(stats.hasData);
        assertTrue(stats.footerHighlight.contains("4 KPI aprobados"));
    }

    @Test
    public void resolveEmptyKind_differentiatesClosedPeriodSearchAndFilters() {
        assertEquals(EmptyKind.NO_CLOSED_PERIOD,
                AdminRankingSummaryHelper.resolveEmptyKind(0, 0, "", "", false));
        assertEquals(EmptyKind.NO_EVALUATIONS,
                AdminRankingSummaryHelper.resolveEmptyKind(0, 0, "2026-11", "", false));
        assertEquals(EmptyKind.NO_SEARCH_RESULTS,
                AdminRankingSummaryHelper.resolveEmptyKind(3, 0, "2026-11", "ana", false));
        assertEquals(EmptyKind.NO_FILTER_RESULTS,
                AdminRankingSummaryHelper.resolveEmptyKind(3, 0, "2026-11", "", true));
        assertEquals(EmptyKind.NONE,
                AdminRankingSummaryHelper.resolveEmptyKind(3, 2, "2026-11", "", false));
    }
}
