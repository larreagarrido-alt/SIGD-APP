package pe.net.ws.kpiweb.features.kpi.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Checks the original 13 model shapes retain real nested fields across JSON recovery. */
@RunWith(Parameterized.class)
public class KpiFormPayloadRoundTripTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> cases() {
        String week = "{\"weekId\":\"fixture-week\",\"days\":[{\"date\":\"12/11/2026\",\"observations\":\"nested observation\"}]}";
        return Arrays.asList(new Object[][] {
            {"VENTAS", pe.net.ws.kpiweb.features.kpi.model.KpiSalesDay.class,
                "{\"dayIndex\":2,\"observaciones\":\"visita observada\",\"llamadas\":3}"},
            {"DISENADOR", pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport.class,
                "{\"weekId\":\"fixture-week\",\"generalObservations\":\"observación\",\"days\":[{\"date\":\"12/11/2026\",\"observations\":\"detalle\"}]}"},
            {"DISENADOR_JUNIOR", pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorDayRecord.class,
                "{\"date\":\"12/11/2026\",\"supervisorObservation\":\"revisado\",\"activities\":[{\"clientProject\":\"proyecto sintético\"}]}"},
            {"DISENADOR_SENIOR", pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorWeekReport.class,
                "{\"weekId\":\"fixture-week\",\"achievements\":\"logro\",\"days\":[{\"date\":\"12/11/2026\",\"activitiesDescription\":\"detalle\"}]}"},
            {"GERENCIA", pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerWeekReport.class,
                "{\"weekId\":\"fixture-week\",\"days\":[{\"date\":\"12/11/2026\",\"ownObservations\":\"seguimiento\"}]}"},
            {"PROGRAMACION", pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport.class,
                "{\"fecha\":\"12/11/2026\",\"firmaPracticante\":\"firma sintética\",\"actividades\":[{\"proyecto\":\"KPIWeb\",\"commitHash\":\"fixture\"}]}"},
            {"ADMINISTRACION", pe.net.ws.kpiweb.features.kpi.administrator.model.KpiAdministratorWeekReport.class, week},
            {"ASISTENCIA_ADMIN", pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantWeekReport.class, week},
            {"CONTABILIDAD", pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantWeekReport.class, week},
            {"COMMUNITY", pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityWeekReport.class, week},
            {"FOTOGRAFIA", pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerWeekReport.class, week},
            {"TRAFFICKER", pe.net.ws.kpiweb.features.kpi.trafficker.model.KpiTraffickerWeekReport.class, week},
            {"VIDEO", pe.net.ws.kpiweb.features.kpi.videographer.model.KpiVideographerWeekReport.class, week},
        });
    }

    private final String type;
    private final Class<?> model;
    private final String json;
    public KpiFormPayloadRoundTripTest(String type, Class<?> model, String json) {
        this.type = type; this.model = model; this.json = json;
    }

    @Test public void retainedFieldsSurviveModelDeserialization() {
        Gson gson = new Gson();
        JsonObject input = JsonParser.parseString(json).getAsJsonObject();
        Object restored = gson.fromJson(input, model);
        JsonObject output = gson.toJsonTree(restored).getAsJsonObject();
        for (String field : input.keySet()) {
            JsonElement expected = input.get(field);
            JsonElement actual = output.get(field);
            assertTrue(type + " lost " + field, actual != null);
            if (expected.isJsonArray()) {
                assertTrue(type + " lost nested rows", actual.isJsonArray() && actual.getAsJsonArray().size() == expected.getAsJsonArray().size());
                JsonObject expectedRow = expected.getAsJsonArray().get(0).getAsJsonObject();
                JsonObject actualRow = actual.getAsJsonArray().get(0).getAsJsonObject();
                for (String nested : expectedRow.keySet()) assertEquals(type + " lost nested " + nested, expectedRow.get(nested), actualRow.get(nested));
            } else assertEquals(type + " changed " + field, expected, actual);
        }
    }
}
