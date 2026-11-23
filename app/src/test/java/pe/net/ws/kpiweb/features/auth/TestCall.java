package pe.net.ws.kpiweb.features.auth;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Synchronous in-memory Call implementation for testing Retrofit callbacks.
 */
public class TestCall<T> implements Call<T> {
    private final Response<T> response;
    private final Throwable failure;
    private boolean canceled = false;
    private boolean executed = false;

    public static <T> TestCall<T> success(T body) {
        return new TestCall<>(Response.success(body), null);
    }

    public static <T> TestCall<T> success(int code, T body) {
        return new TestCall<>(Response.success(code, body), null);
    }

    public static <T> TestCall<T> error(int code, String jsonError) {
        ResponseBody errorBody = ResponseBody.create(
                MediaType.parse("application/json"),
                jsonError != null ? jsonError : ""
        );
        return new TestCall<>(Response.error(code, errorBody), null);
    }

    public static <T> TestCall<T> error(int code, String jsonError, String retryAfter) {
        ResponseBody errorBody = ResponseBody.create(MediaType.parse("application/json"), jsonError);
        okhttp3.Response raw = new okhttp3.Response.Builder()
                .request(new Request.Builder().url("https://ws.net.pe/api/v1/").build())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(code).message("Test")
                .addHeader("Retry-After", retryAfter).build();
        return new TestCall<>(Response.error(errorBody, raw), null);
    }

    public static <T> TestCall<T> failure(Throwable t) {
        return new TestCall<>(null, t);
    }

    private TestCall(Response<T> response, Throwable failure) {
        this.response = response;
        this.failure = failure;
    }

    @Override
    public Response<T> execute() throws IOException {
        executed = true;
        if (failure != null) {
            if (failure instanceof IOException) throw (IOException) failure;
            throw new IOException(failure);
        }
        return response;
    }

    @Override
    public void enqueue(Callback<T> callback) {
        executed = true;
        if (canceled) return;
        if (failure != null) {
            callback.onFailure(this, failure);
        } else {
            callback.onResponse(this, response);
        }
    }

    @Override public boolean isExecuted() { return executed; }
    @Override public void cancel() { canceled = true; }
    @Override public boolean isCanceled() { return canceled; }
    @Override public Call<T> clone() { return new TestCall<>(response, failure); }
    @Override public Request request() { return new Request.Builder().url("https://ws.net.pe/api/v1/").build(); }
    @Override public Timeout timeout() { return Timeout.NONE; }
}
