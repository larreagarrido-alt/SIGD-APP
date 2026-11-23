package pe.net.ws.kpiweb.features.kpi.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport;

public class KpiProgrammerTemplateTest {
    @Test public void keepsOriginalFormDefinitionsWithoutDemoResponses() {
        KpiDailyReport report = KpiMockDataFactory.createBlankProgrammerTemplate();
        assertEquals(6, report.getMetas().size());
        assertEquals(8, report.getCriteriosEvaluacion().size());
        assertTrue(report.getCriteriosEvaluacion().values().stream().noneMatch(Boolean::booleanValue));
        assertTrue(report.getActividades().isEmpty());
        assertNull(report.getPracticante());
        assertNull(report.getSupervisor());
        assertNull(report.getFirmaPracticante());
        assertNull(report.getFirmaSupervisor());
        assertNull(report.getObservacionSupervisor());
        assertFalse(report.isHuboDificultad());
        assertEquals(0, report.getPuntaje());
    }
}
