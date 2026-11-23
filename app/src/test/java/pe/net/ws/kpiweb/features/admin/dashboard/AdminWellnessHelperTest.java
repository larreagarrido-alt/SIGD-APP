package pe.net.ws.kpiweb.features.admin.dashboard;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.CampaignOperationalStats;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.CampaignRecord;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.CampaignStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.RangeValidationResult;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.RyffDimensionInfo;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessHelper.TrendPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminWellnessHelperTest {

    @Test
    public void formatsParticipationOnlyWithValidDenominatorOrAuthorizedServerValue() {
        assertEquals("75 %", AdminWellnessHelper.formatParticipationPercent(null, 15, 20));
        assertEquals("66.7 %", AdminWellnessHelper.formatParticipationPercent(null, 2, 3));
        assertEquals("82.5 %", AdminWellnessHelper.formatParticipationPercent(82.5, 0, 0));
        assertEquals("—", AdminWellnessHelper.formatParticipationPercent(null, 0, 0));
        assertEquals("—", AdminWellnessHelper.formatParticipationPercent(null, 3, 0));
    }

    @Test
    public void requiresAtLeastTwoValidMonthsForComparableTrends() {
        assertFalse(AdminWellnessHelper.hasComparableTrendData(Collections.emptyList()));
        assertFalse(AdminWellnessHelper.hasComparableTrendData(Arrays.asList(
                new TrendPoint("2026-10", true, 50f),
                new TrendPoint("2026-11", false, 0f)
        )));
        assertTrue(AdminWellnessHelper.hasComparableTrendData(Arrays.asList(
                new TrendPoint("2026-10", true, 50f),
                new TrendPoint("2026-11", true, 65f)
        )));
    }

    @Test
    public void exposesAllSixCanonicalRyffDimensionsInOrder() {
        List<RyffDimensionInfo> dims = AdminWellnessHelper.canonicalRyffDimensions();
        assertEquals(6, dims.size());
        assertEquals("Autoaceptación", dims.get(0).displayName);
        assertEquals("Relaciones positivas", dims.get(1).displayName);
        assertEquals("Autonomía", dims.get(2).displayName);
        assertEquals("Dominio del entorno", dims.get(3).displayName);
        assertEquals("Propósito en la vida", dims.get(4).displayName);
        assertEquals("Crecimiento personal", dims.get(5).displayName);
        assertEquals("Dominio del entorno", AdminWellnessHelper.formatRyffDimensionName("DOMINIO_DEL_ENTORNO"));
        assertEquals("Propósito en la vida", AdminWellnessHelper.formatRyffDimensionName("proposito en la vida"));
    }

    @Test
    public void validatesVigenciaRangesAndProjectsInitialStatus() {
        long now = 1_700_000_000_000L;
        RangeValidationResult missing = AdminWellnessHelper.validateVigenciaRange(0L, now + 3600_000L, now);
        assertFalse(missing.valid);

        RangeValidationResult inverted = AdminWellnessHelper.validateVigenciaRange(now + 7200_000L, now + 3600_000L, now);
        assertFalse(inverted.valid);

        RangeValidationResult future = AdminWellnessHelper.validateVigenciaRange(now + 3600_000L, now + 3600_000L + 3 * 86400_000L, now);
        assertTrue(future.valid);
        assertEquals("Programada", future.projectedStatus);
        assertEquals("3 días", future.durationSummary);

        RangeValidationResult active = AdminWellnessHelper.validateVigenciaRange(now - 60_000L, now + 5 * 3600_000L, now);
        assertTrue(active.valid);
        assertEquals("Activa", active.projectedStatus);
    }

    @Test
    public void computesRealOperationalStatsWithoutConflatingCompletedAndSent() {
        List<CampaignRecord> rows = Arrays.asList(
                new CampaignRecord(1, "Ryff Q4", "", "COLABORADOR", "2026-11-01", "2026-11-15",
                        CampaignStatus.ACTIVA, 10, 4, true),
                new CampaignRecord(2, "Ryff Q3", "", "AMBOS", "2026-08-01", "2026-08-15",
                        CampaignStatus.FINALIZADA, 12, 11, true)
        );
        CampaignOperationalStats stats = AdminWellnessHelper.computeCampaignStats(rows);
        assertTrue(stats.usesInvitationCounts);
        assertEquals(22, stats.totalApplications);
        assertEquals(1, stats.activeOrPending);
        assertEquals(15, stats.completedEvaluations);
        assertEquals(1, stats.finishedOrExpired);
    }

    @Test
    public void derivesAudienceAndMatchesCollaboratorSearchAccurately() {
        assertEquals("COLABORADOR", AdminWellnessHelper.deriveAudienceFromRoles(Arrays.asList("Analista", "QA")));
        assertEquals("PRACTICANTE", AdminWellnessHelper.deriveAudienceFromRoles(Collections.singletonList("Practicante Preprofesional")));
        assertEquals("AMBOS", AdminWellnessHelper.deriveAudienceFromRoles(Arrays.asList("Analista", "Practicante")));

        assertTrue(AdminWellnessHelper.matchesCollaboratorQuery("Ángel Torres", "Analista", "Agencia Lima", "angel"));
        assertTrue(AdminWellnessHelper.matchesCollaboratorQuery("Ángel Torres", "Analista", "Agencia Lima", "lima"));
        assertFalse(AdminWellnessHelper.matchesCollaboratorQuery("Ángel Torres", "Analista", "Agencia Lima", "piura"));
        assertEquals("Noviembre 2026", AdminWellnessHelper.formatMonthDisplay("2026-11"));
        assertEquals("Nov 26", AdminWellnessHelper.formatMonthShort("2026-11"));
        assertEquals("2026-11-22 10:00:00",
                AdminWellnessHelper.formatServerDateTime(1795341600000L, TimeZone.getTimeZone("UTC")));
    }
}
