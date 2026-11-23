package pe.net.ws.kpiweb.features.collaborator.wellness;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.List;

import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessEvolutionHelper.EvaluationRecord;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessEvolutionHelper.StatusTone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WellnessEvolutionHelperTest {

    @Test
    public void formatPeriodKey_handlesMonthlyWeeklyAndCampaignFormats() {
        assertEquals("Noviembre 2026", WellnessEvolutionHelper.formatPeriodKey("2026-11"));
        assertEquals("Enero 2026", WellnessEvolutionHelper.formatPeriodKey("2026-01"));
        assertEquals("Semana 47 · 2026", WellnessEvolutionHelper.formatPeriodKey("2026-W47"));
        assertEquals("Campaña #3", WellnessEvolutionHelper.formatPeriodKey("C3"));
        assertEquals("Período vigente", WellnessEvolutionHelper.formatPeriodKey(""));
    }

    @Test
    public void formatStatusAndDate_neverInventsMissingDate() {
        assertEquals("Completada", WellnessEvolutionHelper.formatStatusLabel("COMPLETADA"));
        assertEquals(StatusTone.COMPLETED, WellnessEvolutionHelper.resolveStatusTone("COMPLETADA"));
        assertEquals("En proceso", WellnessEvolutionHelper.formatStatusLabel("EN_PROCESO"));
        assertEquals(StatusTone.IN_PROGRESS, WellnessEvolutionHelper.resolveStatusTone("EN_PROCESO"));
        assertEquals("Pendiente", WellnessEvolutionHelper.formatStatusLabel("PENDIENTE"));
        assertEquals(StatusTone.PENDING, WellnessEvolutionHelper.resolveStatusTone("PENDIENTE"));

        assertEquals("", WellnessEvolutionHelper.formatCompletedDate(null));
        assertEquals("", WellnessEvolutionHelper.formatCompletedDate(""));
        assertTrue(WellnessEvolutionHelper.formatCompletedDate("2026-11-20 15:30:00").startsWith("Realizada el 20"));
    }

    @Test
    public void parseAndSelectFeatured_prefersMonthlyEvaluation() {
        JsonObject weekly = new JsonObject();
        weekly.addProperty("id_evaluacion_bienestar", 10);
        weekly.addProperty("id_test", 2);
        weekly.addProperty("titulo", "Evaluación semanal");
        weekly.addProperty("periodo_clave", "2026-W47");
        weekly.addProperty("estado", "COMPLETADA");

        JsonObject monthly = new JsonObject();
        monthly.addProperty("id_evaluacion_bienestar", 11);
        monthly.addProperty("id_test", 1);
        monthly.addProperty("titulo", "Evaluación mensual");
        monthly.addProperty("periodo_clave", "2026-11");
        monthly.addProperty("estado", "COMPLETADA");
        monthly.addProperty("fecha_completada", "2026-11-18 10:00:00");

        JsonArray arr = new JsonArray();
        arr.add(weekly);
        arr.add(monthly);

        List<EvaluationRecord> parsed = WellnessEvolutionHelper.parseHistory(arr);
        assertEquals(2, parsed.size());
        assertFalse(parsed.get(0).hasCompletedDate());
        assertTrue(parsed.get(1).hasCompletedDate());

        EvaluationRecord featured = WellnessEvolutionHelper.selectFeatured(parsed, 1);
        assertNotNull(featured);
        assertEquals(11, featured.evaluationId);
        assertEquals("Noviembre 2026", featured.periodDisplay);
        assertTrue(featured.canOpenDetail());

        assertNull(WellnessEvolutionHelper.selectFeatured(WellnessEvolutionHelper.parseHistory(new JsonArray()), 1));
    }
}
