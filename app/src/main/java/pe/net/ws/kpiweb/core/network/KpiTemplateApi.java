package pe.net.ws.kpiweb.core.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;

public interface KpiTemplateApi {
    @GET("kpi/plantillas")
    Call<ApiResponse<JsonObject>> read(@Header("Authorization") String bearer);

    @PUT("kpi/plantillas")
    Call<ApiResponse<JsonObject>> save(@Header("Authorization") String bearer, @Body JsonObject body);
}
