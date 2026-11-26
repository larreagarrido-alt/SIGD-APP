package pe.net.ws.kpiweb.core.network;

import android.content.Context;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import pe.net.ws.kpiweb.core.session.SessionManager;

/** Refreshes an expired access token once and retries the original authorized request. */
final class SessionAuthenticator implements Authenticator {
    private static final Object ROTATION_LOCK = new Object();
    private final Context app;
    private final OkHttpClient refreshClient = new OkHttpClient();

    SessionAuthenticator(Context context) { app = context.getApplicationContext(); }

    @Override public Request authenticate(Route route, Response response) throws IOException {
        if (response.priorResponse() != null) return null;
        String header = response.request().header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        String expired = header.substring(7);
        synchronized (ROTATION_LOCK) {
            SessionManager session = new SessionManager(app);
            if (!session.isLoggedIn()) return null;
            String current = session.getAccessToken();
            if (!expired.equals(current)) return session.wasPreviousAccess(expired)
                    ? response.request().newBuilder().header("Authorization", "Bearer " + current).build()
                    : null;
            String refresh = session.getRefreshToken();
            if (refresh.isEmpty()) return null;
            JsonObject body = new JsonObject();
            body.addProperty("refresh_token", refresh);
            Request request = new Request.Builder().url(ApiClient.BASE_URL + "auth/refresh")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json"))).build();
            try (Response rotated = refreshClient.newCall(request).execute()) {
                if (!rotated.isSuccessful() || rotated.body() == null) return null;
                JsonObject json = JsonParser.parseString(rotated.body().string()).getAsJsonObject();
                if (!json.has("success") || !json.get("success").getAsBoolean()
                        || !json.has("data") || !json.get("data").isJsonObject()) return null;
                JsonObject data = json.getAsJsonObject("data");
                String access = data.get("access_token").getAsString();
                String nextRefresh = data.get("refresh_token").getAsString();
                if (!session.rotateTokens(expired, access, nextRefresh)) return null;
                return response.request().newBuilder().header("Authorization", "Bearer " + access).build();
            } catch (RuntimeException invalidResponse) { return null; }
        }
    }
}
