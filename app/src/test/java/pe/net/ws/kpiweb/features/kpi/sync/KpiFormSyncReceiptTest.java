package pe.net.ws.kpiweb.features.kpi.sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class KpiFormSyncReceiptTest {
    private final JsonObject sent = JsonParser.parseString("{\"tipo_formulario\":\"PROGRAMACION\",\"registro_uid\":\"fixture-1\",\"revision\":7,\"payload\":{\"fecha\":\"2026-11-12\",\"actividades\":[{\"observacion\":\"original\"}]}}").getAsJsonObject();

    @Test public void onlyExactWorkerReceiptAcknowledgesWrite() {
        assertTrue(KpiFormSyncManager.matchesReceipt(json("{\"id_trabajador\":4,\"accepted\":[{\"tipo_formulario\":\"PROGRAMACION\",\"registro_uid\":\"fixture-1\",\"revision\":7}]}"),sent,4));
        assertFalse(KpiFormSyncManager.matchesReceipt(json("{\"id_trabajador\":5,\"accepted\":[{\"tipo_formulario\":\"PROGRAMACION\",\"registro_uid\":\"fixture-1\",\"revision\":7}]}"),sent,4));
    }

    @Test public void readBackMustContainSameNestedContentAndRevision() {
        JsonObject data = json("{\"id_trabajador\":4,\"registros\":[{\"tipo_formulario\":\"PROGRAMACION\",\"registro_uid\":\"fixture-1\",\"revision\":7,\"payload\":{\"fecha\":\"2026-11-12\",\"actividades\":[{\"observacion\":\"original\"}]}}]}");
        assertTrue(KpiFormSyncManager.matchesRead(data,sent,4));
        assertFalse(KpiFormSyncManager.matchesRead(data,sent,5));
        data.getAsJsonArray("registros").get(0).getAsJsonObject().getAsJsonObject("payload")
                .getAsJsonArray("actividades").get(0).getAsJsonObject().addProperty("observacion","changed");
        assertFalse(KpiFormSyncManager.matchesRead(data,sent,4));
    }

    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
}
