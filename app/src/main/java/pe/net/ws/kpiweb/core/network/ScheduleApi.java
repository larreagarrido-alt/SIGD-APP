package pe.net.ws.kpiweb.core.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PATCH;

public interface ScheduleApi {
    @GET("horarios/mi-horario")
    Call<ApiResponse<JsonObject>> mine(@Header("Authorization") String bearer);

    @GET("horarios")
    Call<ApiResponse<JsonArray>> all(@Header("Authorization") String bearer);

    @GET("horarios/cargos")
    Call<ApiResponse<JsonArray>> cargos(@Header("Authorization") String bearer);

    @GET("horarios/trabajadores")
    Call<ApiResponse<JsonArray>> workers(@Header("Authorization") String bearer);

    @GET("horarios/plantillas")
    Call<ApiResponse<JsonArray>> templates(@Header("Authorization") String bearer);

    @GET("horarios/alertas")
    Call<ApiResponse<JsonArray>> alerts(@Header("Authorization") String bearer);

    @PATCH("horarios/alertas/{id}/atender")
    Call<ApiResponse<JsonObject>> acknowledge(@Header("Authorization") String bearer, @Path("id") long id);

    @PUT("horarios/cargos/{id}")
    Call<ApiResponse<JsonObject>> save(@Header("Authorization") String bearer,
                                       @Path("id") int cargoId, @Body JsonObject body);

    @POST("horarios/cargos/{id}/plantillas/{template}")
    Call<ApiResponse<JsonObject>> assign(@Header("Authorization") String bearer,
                                         @Path("id") int cargoId, @Path("template") String templateId);

    @PUT("horarios/turnos/{worker}")
    Call<ApiResponse<JsonObject>> shift(@Header("Authorization") String bearer,
                                        @Path("worker") int workerId, @Body JsonObject body);
}
