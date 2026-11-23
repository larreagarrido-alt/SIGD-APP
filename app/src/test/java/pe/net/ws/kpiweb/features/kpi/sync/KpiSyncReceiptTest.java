package pe.net.ws.kpiweb.features.kpi.sync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;

public class KpiSyncReceiptTest {
    private final Gson gson = new Gson();

    @Test public void onlyExactWorkerAndIdsAreAccepted() {
        HashSet<String> sent = new HashSet<>(Arrays.asList("REC-1", "REC-2"));
        assertTrue(KpiSyncManager.exactReceipt(receipt(7, "REC-1", "REC-2"), 7, sent));
        assertFalse(KpiSyncManager.exactReceipt(receipt(8, "REC-1", "REC-2"), 7, sent));
        assertFalse(KpiSyncManager.exactReceipt(receipt(7, "REC-1"), 7, sent));
        assertFalse(KpiSyncManager.exactReceipt(receipt(7, "REC-1", "REC-1"), 7, sent));
    }

    private KpiSyncReceipt receipt(int worker, String... ids) {
        StringBuilder json = new StringBuilder("{\"id_trabajador\":").append(worker).append(",\"accepted_ids\":[");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) json.append(',');
            json.append('"').append(ids[i]).append('"');
        }
        return gson.fromJson(json.append("]}").toString(), KpiSyncReceipt.class);
    }
}
