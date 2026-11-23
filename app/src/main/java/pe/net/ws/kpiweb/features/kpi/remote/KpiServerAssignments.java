package pe.net.ws.kpiweb.features.kpi.remote;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.List;

/** Respuesta de GET /kpi/mis-kpis. */
public final class KpiServerAssignments {
    @SerializedName("id_trabajador") private int workerId;
    @SerializedName("periodo") private String period;
    @SerializedName("total") private int total;
    @SerializedName("evaluaciones") private List<KpiServerAssignment> assignments;

    public int getWorkerId() { return workerId; }
    public String getPeriod() { return period; }
    public int getTotal() { return total; }
    public List<KpiServerAssignment> getAssignments() {
        return assignments == null ? Collections.emptyList() : assignments;
    }
}
