package pe.net.ws.kpiweb.core.network;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.*;
import pe.net.ws.kpiweb.features.auth.models.FaceDeviceResponse;
public interface FaceDeviceApi {
    @POST("auth/face/device/status") Call<ApiResponse<FaceDeviceResponse>> status(@Header("Authorization") String token,@Body Map<String,Object> body);
    @POST("auth/face/device/revoke-challenge") Call<ApiResponse<FaceDeviceResponse>> revokeChallenge(@Header("Authorization") String token,@Body Map<String,Object> body);
    @POST("auth/face/device/revoke") Call<ApiResponse<FaceDeviceResponse>> revoke(@Header("Authorization") String token,@Body Map<String,Object> body);
    @POST("auth/face/device/register") Call<ApiResponse<FaceDeviceResponse>> register(@Header("Authorization") String token,@Body Map<String,Object> body);
    @POST("auth/face/device/confirm") Call<ApiResponse<FaceDeviceResponse>> confirm(@Header("Authorization") String token,@Body Map<String,Object> body);
    @POST("auth/face/device/challenge") Call<ApiResponse<FaceDeviceResponse>> challenge(@Body Map<String,Object> body);
    @POST("auth/face/device/verify") Call<ApiResponse<FaceDeviceResponse>> verify(@Body Map<String,Object> body);
}
