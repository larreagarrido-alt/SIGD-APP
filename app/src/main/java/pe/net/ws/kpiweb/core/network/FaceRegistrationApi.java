package pe.net.ws.kpiweb.core.network;

import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Streaming;

/** Raw bodies preserve the actual HTTP status even when a proxy returns invalid JSON. */
public interface FaceRegistrationApi {
    @Streaming @POST("auth/face/device/register") Call<ResponseBody> register(@Header("Authorization") String token, @Body Map<String,Object> body);
    @Streaming @POST("auth/face/device/confirm") Call<ResponseBody> confirm(@Header("Authorization") String token, @Body Map<String,Object> body);
}
