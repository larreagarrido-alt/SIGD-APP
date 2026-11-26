package pe.net.ws.kpiweb.features.kpi.model;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import org.junit.Test;

public class KpiProgrammerDailyScoreTest {
    @Test public void dailyGoalCountsOnlyDeliveredTasksOfRequiredType() {
        KpiActivityItem correction = new KpiActivityItem();
        correction.setTipo("Corrección");
        correction.setEstado("Entregado");
        correction.setBugs(9);
        KpiActivityItem unrelated = new KpiActivityItem();
        unrelated.setTipo("Desarrollo");
        unrelated.setEstado("Entregado");
        KpiActivityItem pending = new KpiActivityItem();
        pending.setTipo("Corrección");
        pending.setEstado("Pendiente");
        KpiProgrammerDailyScore.Result result = KpiProgrammerDailyScore.calculate(
                1, 2, Arrays.asList(correction, unrelated, pending));
        assertEquals(1, result.matching);
        assertEquals(1, result.other);
        assertEquals(50, result.percent());
    }
}
