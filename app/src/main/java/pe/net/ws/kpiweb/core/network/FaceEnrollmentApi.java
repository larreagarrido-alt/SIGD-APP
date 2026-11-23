package pe.net.ws.kpiweb.core.network;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;
import pe.net.ws.kpiweb.features.auth.models.FaceEnrollmentStatus;
public interface FaceEnrollmentApi {
    @POST("auth/face/enrollment/status") Call<ApiResponse<FaceEnrollmentStatus>> status(@Header("Authorization") String bearer,@Body Map<String,Object> body);
    @POST("auth/face/enrollment/challenge") Call<ApiResponse<AndroidAccessChallenge>> challenge(@Header("Authorization") String bearer,@Body Map<String,Object> body);
    @POST("auth/face/enrollment/confirm") Call<ApiResponse<FaceEnrollmentStatus>> confirm(@Header("Authorization") String bearer,@Body Map<String,Object> body);
}
