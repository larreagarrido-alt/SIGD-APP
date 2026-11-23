package pe.net.ws.kpiweb.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.Request;
import okio.Timeout;
import org.junit.Test;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionEntryCoordinatorTest {
    private static final Gson JSON = new Gson();

    @Test public void validProfileIsVerified() {
        Fixture f = new Fixture();
        LoginResponseData current = profile(7, "kpi.ver");
        f.call.respond(200, current);
        assertEvent(f, "verified");
        assertSame(current, f.listener.profile);
        assertNotReissued(f.call);
    }

    @Test public void expiredSessionRequiresLogin() {
        Fixture f = new Fixture();
        f.call.respond(401, null);
        assertEvent(f, "unauthorized");
    }

    @Test public void forbiddenReportsAccessDenied() {
        Fixture f = new Fixture();
        f.call.respond(403, null);
        assertEvent(f, "forbidden");
    }

    @Test public void connectionFailureAllowsRetry() {
        Fixture f = new Fixture();
        f.call.fail();
        assertEvent(f, "unavailable");
    }

    @Test public void wrongIdentityCannotEnter() {
        Fixture f = new Fixture();
        f.call.respond(200, profile(8, "kpi.ver"));
        assertEvent(f, "unauthorized");
    }

    @Test public void malformedIdentityCannotEnter() {
        for (LoginResponseData invalid : new LoginResponseData[] {
                new LoginResponseData(), profile(0, "kpi.ver"), profile(-1, "kpi.ver")}) {
            Fixture f = new Fixture();
            f.call.respond(200, invalid);
            assertEvent(f, "unauthorized");
        }
    }

    @Test public void missingPermissionsCannotEnter() {
        Fixture f = new Fixture();
        f.call.respond(200, profile(7, null));
        assertEvent(f, "unavailable");
    }

    @Test public void accountChangedDuringRequestIgnoresResponse() {
        Fixture f = new Fixture();
        f.current = false;
        f.call.respond(200, profile(7, "kpi.ver"));
        f.call.fail();
        assertNoEvent(f);
    }

    @Test public void cancellationIgnoresLateSuccessAndFailureWithoutReissuing() {
        Fixture f = new Fixture();
        f.coordinator.cancel();
        f.call.respond(200, profile(7, "kpi.ver"));
        f.call.fail();
        assertTrue(f.call.canceled);
        assertEquals(1, f.call.cancelCount);
        assertNoEvent(f);
        assertNotReissued(f.call);
    }

    private static void assertEvent(Fixture f, String event) {
        assertEquals(event, f.listener.event);
        assertEquals(1, f.listener.notifications);
        if (!"verified".equals(event)) assertNull(f.listener.profile);
    }

    private static void assertNoEvent(Fixture f) {
        assertEquals("", f.listener.event);
        assertEquals(0, f.listener.notifications);
        assertNull(f.listener.profile);
    }

    private static void assertNotReissued(FakeCall call) {
        assertEquals(1, call.enqueueCount);
        assertEquals(0, call.executeCount);
        assertEquals(0, call.cloneCount);
    }

    private static LoginResponseData profile(int id, String permission) {
        String permissions = permission == null ? "null" : "[\"" + permission + "\"]";
        return JSON.fromJson("{\"user\":{\"id_usuario\":" + id + ",\"id_rol\":2},\"permissions\":" + permissions + "}", LoginResponseData.class);
    }

    private static final class Fixture {
        boolean current = true;
        final TestListener listener = new TestListener();
        final FakeCall call = new FakeCall();
        final SessionEntryCoordinator coordinator = new SessionEntryCoordinator(listener);
        Fixture() { coordinator.verify(call, 7, () -> current); }
    }

    private static final class TestListener implements SessionEntryCoordinator.Listener {
        String event = "";
        LoginResponseData profile;
        int notifications;
        @Override public void verified(LoginResponseData value) {
            profile = value; event = "verified"; notifications++;
        }
        @Override public void unauthorized() { event = "unauthorized"; notifications++; }
        @Override public void forbidden() { event = "forbidden"; notifications++; }
        @Override public void unavailable() { event = "unavailable"; notifications++; }
    }

    private static final class FakeCall implements Call<ApiResponse<LoginResponseData>> {
        Callback<ApiResponse<LoginResponseData>> callback;
        boolean canceled;
        int enqueueCount;
        int executeCount;
        int cloneCount;
        int cancelCount;
        // Deliver callbacks even after cancellation to exercise the coordinator's guards.
        void respond(int code, LoginResponseData data) {
            Response<ApiResponse<LoginResponseData>> response = code == 200
                ? Response.success(ApiResponse.success(data))
                : Response.error(code, okhttp3.ResponseBody.create(null, new byte[0]));
            callback.onResponse(this, response);
        }
        void fail() { callback.onFailure(this, new IOException("offline")); }
        public void enqueue(Callback<ApiResponse<LoginResponseData>> callback) {
            enqueueCount++; this.callback = callback;
        }
        public Response<ApiResponse<LoginResponseData>> execute() {
            executeCount++; throw new UnsupportedOperationException();
        }
        public void cancel() { cancelCount++; canceled = true; }
        public boolean isCanceled() { return canceled; }
        public boolean isExecuted() { return callback != null; }
        public Call<ApiResponse<LoginResponseData>> clone() { cloneCount++; return new FakeCall(); }
        public Request request() { return new Request.Builder().url("https://example.test/auth/me").build(); }
        public Timeout timeout() { return new Timeout(); }
    }
}
