package pe.net.ws.kpiweb.features.kpi.sync;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/** Server acknowledgement for an atomic KPI record batch. */
public final class KpiSyncReceipt {
    @SerializedName("id_trabajador") private int workerId;
    @SerializedName("accepted_ids") private List<String> acceptedIds;
    public int getWorkerId() { return workerId; }
    public List<String> getAcceptedIds() {
        return acceptedIds == null ? Collections.emptyList() : acceptedIds;
    }
}
