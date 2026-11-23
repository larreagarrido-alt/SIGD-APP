package pe.net.ws.kpiweb.features.collaborator.wellness;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OfficialWellnessScreenTest {
    @Test public void monthlyOptionsFollowDanisMeaningRatherThanDatabaseOrder() {
        assertEquals(1, OfficialWellnessScreen.monthlyScore("Muy bien"));
        assertEquals(2, OfficialWellnessScreen.monthlyScore("Bien"));
        assertEquals(3, OfficialWellnessScreen.monthlyScore("Regular"));
        assertEquals(4, OfficialWellnessScreen.monthlyScore("Mal"));
        assertEquals(5, OfficialWellnessScreen.monthlyScore("Muy mal"));
        assertEquals(0, OfficialWellnessScreen.monthlyScore("Otra respuesta"));
    }
}
