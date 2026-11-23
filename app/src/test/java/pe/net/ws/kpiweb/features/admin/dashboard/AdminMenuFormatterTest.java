package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminMenuFormatter.PersonalEvalSummary;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminMenuFormatterTest {

    private final Gson gson = new Gson();

    @Test
    public void extractsFirstNameCompactly() {
        assertEquals("Manuel", AdminMenuFormatter.extractFirstName("Manuel Eduardo Castillo Medrano"));
        assertEquals("Ana", AdminMenuFormatter.extractFirstName("  Ana   "));
        assertEquals("Administrador", AdminMenuFormatter.extractFirstName(""));
        assertEquals("Administrador", AdminMenuFormatter.extractFirstName(null));
        assertEquals("Administrador", AdminMenuFormatter.resolveRoleLabel(""));
        assertEquals("Administrador General", AdminMenuFormatter.resolveRoleLabel("Administrador General"));
    }

    @Test
    public void computesIndividualPerformanceScoreWithoutRankingOrRewards() {
        KpiServerAssignment a1 = gson.fromJson(
                "{\"id_evaluacion\":1,\"estado\":\"Aprobado\",\"peso_snapshot\":60,\"porcentaje_cumplimiento\":90.0}",
                KpiServerAssignment.class);
        KpiServerAssignment a2 = gson.fromJson(
                "{\"id_evaluacion\":2,\"estado\":\"Pendiente\",\"peso_snapshot\":40,\"porcentaje_cumplimiento\":80.0}",
                KpiServerAssignment.class);

        PersonalEvalSummary summary = AdminMenuFormatter.computePersonalEvaluationSummary(Arrays.asList(a1, a2));
        assertEquals(2, summary.total);
        assertEquals(1, summary.approved);
        assertEquals(1, summary.pending);
        assertTrue(summary.hasScore);
        assertEquals("86.0%", summary.scoreDisplay);
    }

    @Test
    public void handlesEmptyEvaluationsHonestly() {
        PersonalEvalSummary empty = AdminMenuFormatter.computePersonalEvaluationSummary(Collections.emptyList());
        assertEquals(0, empty.total);
        assertEquals(0, empty.approved);
        assertEquals(0, empty.pending);
        assertFalse(empty.hasScore);
        assertEquals("—", empty.scoreDisplay);
    }
}
