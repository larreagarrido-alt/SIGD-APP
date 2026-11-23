package pe.net.ws.kpiweb.features.admin.dashboard;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AdminRankingPeriodFormatterTest {

    @Test
    public void formatsMonthlyPeriodKeysInSpanish() {
        assertEquals("Enero 2026", AdminRankingPeriodFormatter.formatPeriodKey("2026-01", "MENSUAL"));
        assertEquals("Noviembre 2026", AdminRankingPeriodFormatter.formatPeriodKey("2026-11", "MENSUAL"));
        assertEquals("Diciembre 2025", AdminRankingPeriodFormatter.formatPeriodKey("2025-12", "MENSUAL"));
    }

    @Test
    public void formatsQuarterlyAnnualAndWeeklyKeys() {
        assertEquals("4.º trimestre 2026", AdminRankingPeriodFormatter.formatPeriodKey("2026-Q4", "TRIMESTRAL"));
        assertEquals("2.º trimestre 2026", AdminRankingPeriodFormatter.formatPeriodKey("2026-T2", "TRIMESTRAL"));
        assertEquals("Año 2026", AdminRankingPeriodFormatter.formatPeriodKey("2026", "ANUAL"));
        assertEquals("Semana 47 · 2026", AdminRankingPeriodFormatter.formatPeriodKey("2026-W47", "SEMANAL"));
    }

    @Test
    public void formatsEmptyOrUnknownKeysGracefully() {
        assertEquals("Sin cierre mensual", AdminRankingPeriodFormatter.formatPeriodKey("", "MENSUAL"));
        assertEquals("Sin cierre trimestral", AdminRankingPeriodFormatter.formatPeriodKey(null, "TRIMESTRAL"));
        assertEquals("Sin cierre anual", AdminRankingPeriodFormatter.formatPeriodKey("   ", "ANUAL"));
        assertEquals("2026-CUSTOM", AdminRankingPeriodFormatter.formatPeriodKey("2026-CUSTOM", "MENSUAL"));
    }

    @Test
    public void mapsFrequencyAndPersonnelLabels() {
        assertEquals("Semanal", AdminRankingPeriodFormatter.frequencyLabel("SEMANAL"));
        assertEquals("Mensual", AdminRankingPeriodFormatter.frequencyLabel("MENSUAL"));
        assertEquals("Trimestral", AdminRankingPeriodFormatter.frequencyLabel("TRIMESTRAL"));
        assertEquals("Anual", AdminRankingPeriodFormatter.frequencyLabel("ANUAL"));

        assertEquals("Todos", AdminRankingPeriodFormatter.personnelLabel("Todos"));
        assertEquals("Colaboradores", AdminRankingPeriodFormatter.personnelLabel("Colaborador"));
        assertEquals("Practicantes", AdminRankingPeriodFormatter.personnelLabel("Practicante"));
    }
}
