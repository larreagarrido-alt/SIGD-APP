package pe.net.ws.kpiweb.features.auth;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class RetryAfterTest {
    @Test public void secondsAreTakenOnlyFromServerHeader() {
        assertEquals(90_000,RetryAfter.delayMillis("90",0));
        assertEquals(-1,RetryAfter.delayMillis(null,0));
        assertEquals(-1,RetryAfter.delayMillis("sometime",0));
    }
    @Test public void httpDateUsesProvidedCurrentTime() {
        assertEquals(60_000,RetryAfter.delayMillis("Wed, 21 Oct 2015 07:28:00 GMT",1445412420000L));
        assertEquals(0,RetryAfter.delayMillis("Wed, 21 Oct 2015 07:28:00 GMT",1445412480000L));
    }
}
