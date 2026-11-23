package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.JsonObject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminKpiOfficialFormatterTest {

    @Test
    public void formatsFormTypeWorkerAndEvaluations() {
        assertEquals("Programación", AdminKpiOfficialFormatter.formatFormType("PROGRAMACIÓN"));
        assertEquals("Ventas campo", AdminKpiOfficialFormatter.formatFormType("VENTAS_CAMPO"));
        assertEquals("Área sin especificar", AdminKpiOfficialFormatter.formatFormType("  "));

        assertEquals("Trabajador 12", AdminKpiOfficialFormatter.formatWorkerLabel("", 12));
        assertEquals("Ana Torres", AdminKpiOfficialFormatter.formatWorkerLabel("Ana Torres", 12));

        assertEquals("0 evaluaciones", AdminKpiOfficialFormatter.formatEvaluationsCount(0));
        assertEquals("1 evaluación", AdminKpiOfficialFormatter.formatEvaluationsCount(1));
        assertEquals("4 evaluaciones", AdminKpiOfficialFormatter.formatEvaluationsCount(4));
    }

    @Test
    public void matchesReportLocallyByIdAreaOrWorker() {
        JsonObject report = new JsonObject();
        report.addProperty("id_detalle", 10);
        report.addProperty("tipo_formulario", "PROGRAMACIÓN");
        report.addProperty("id_trabajador", 12);
        report.addProperty("evaluaciones_vinculadas", 0);

        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, ""));
        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, "#10"));
        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, "reporte 10"));
        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, "programacion"));
        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, "PROGRAMACIÓN"));
        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, "trabajador 12"));
        assertTrue(AdminKpiOfficialFormatter.matchesReportQuery(report, "12"));
        assertFalse(AdminKpiOfficialFormatter.matchesReportQuery(report, "ventas"));
        assertFalse(AdminKpiOfficialFormatter.matchesReportQuery(report, "trabajador 99"));
    }
}
