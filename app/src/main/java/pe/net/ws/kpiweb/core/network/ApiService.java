package pe.net.ws.kpiweb.core.network;

import pe.net.ws.kpiweb.features.auth.models.LoginRequest;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import java.util.Map;
import com.google.gson.JsonObject;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignments;
import pe.net.ws.kpiweb.features.kpi.sync.KpiSyncReceipt;

public interface ApiService {
    @GET("asistencias/mias")
    Call<ApiResponse<JsonObject>> getOwnAttendance(@Header("Authorization") String bearer,
                                                    @Query("mes") String month);
    @GET("bienestar/admin/resumen")
    Call<ApiResponse<JsonObject>> getAdminWellnessOverview(@Header("Authorization") String bearer,
                                                           @Query("mes") String month,
                                                           @Query("id_campana") long campaignId,
                                                           @Query("id_agencia") int agencyId);
    @GET("bienestar/admin/campanas")
    Call<ApiResponse<JsonObject>> getAdminWellnessCampaigns(@Header("Authorization") String bearer);
    @POST("bienestar/admin/campanas")
    Call<ApiResponse<JsonObject>> createAdminWellnessCampaign(@Header("Authorization") String bearer,
                                                              @Body JsonObject body);
    @PUT("bienestar/admin/campanas/{id}")
    Call<ApiResponse<JsonObject>> updateAdminWellnessCampaign(@Header("Authorization") String bearer,
                                                              @Path("id") long id,@Body JsonObject body);
    @GET("bienestar/admin/cuestionario")
    Call<ApiResponse<JsonObject>> getAdminRyffQuestionnaire(@Header("Authorization") String bearer);
    @GET("bienestar/admin/planes")
    Call<ApiResponse<JsonObject>> getAdminWellnessPlans(@Header("Authorization") String bearer);
    @POST("bienestar/admin/planes")
    Call<ApiResponse<JsonObject>> createAdminWellnessPlan(@Header("Authorization") String bearer,
                                                          @Body JsonObject body);
    @PUT("bienestar/admin/planes/{id}")
    Call<ApiResponse<JsonObject>> updateAdminWellnessPlan(@Header("Authorization") String bearer,
                                                          @Path("id") long id,@Body JsonObject body);
    @POST("bienestar/admin/asistente")
    Call<ApiResponse<JsonObject>> askAdminWellnessAssistant(@Header("Authorization") String bearer,
                                                            @Body JsonObject body);
    @GET("ranking")
    Call<ApiResponse<JsonObject>> getLatestRanking(@Header("Authorization") String bearerToken,
                                                    @Query("periodo_tipo") String periodType);
    @GET("ranking")
    Call<ApiResponse<JsonObject>> getRankingMonth(@Header("Authorization") String bearerToken,
                                                 @Query("periodo_tipo") String periodType,
                                                 @Query("periodo_clave") String month);
    @GET("ranking/{workerId}")
    Call<ApiResponse<JsonObject>> getRankingDetail(@Header("Authorization") String bearerToken,
                                                   @Path("workerId") int workerId,
                                                   @Query("periodo_tipo") String periodType);
    @GET("ranking/periodos")
    Call<ApiResponse<JsonObject>> getRankingPeriods(@Header("Authorization") String bearerToken);
    @GET("kpi/oficial/definiciones")
    Call<ApiResponse<JsonObject>> getOfficialKpiDefinitions(@Header("Authorization") String bearerToken);
    @POST("kpi/oficial/definiciones")
    Call<ApiResponse<JsonObject>> createOfficialKpiDefinition(@Header("Authorization") String bearerToken,
                                                              @Body JsonObject body);
    @POST("kpi/oficial/definiciones/{id}/aprobar")
    Call<ApiResponse<JsonObject>> approveOfficialKpiDefinition(@Header("Authorization") String bearerToken,
                                                               @Path("id") int id, @Body JsonObject body);
    @POST("kpi/oficial/asignaciones")
    Call<ApiResponse<JsonObject>> assignOfficialKpi(@Header("Authorization") String bearerToken,
                                                    @Body JsonObject body);
    @GET("kpi/oficial/reportes")
    Call<ApiResponse<JsonObject>> getOfficialKpiReports(@Header("Authorization") String bearerToken);
    @POST("kpi/oficial/reportes/{id}/evaluar")
    Call<ApiResponse<JsonObject>> reviewOfficialKpiReport(@Header("Authorization") String bearerToken,
                                                          @Path("id") int id, @Body JsonObject body);
    @POST("kpi/evaluaciones/{id}/calificar")
    Call<ApiResponse<JsonObject>> approveOfficialKpiEvaluation(@Header("Authorization") String bearerToken,
                                                               @Path("id") int id, @Body JsonObject body);
    @POST("ranking/politicas")
    Call<ApiResponse<JsonObject>> approveRankingPolicy(@Header("Authorization") String bearerToken,
                                                       @Body JsonObject body);
    @POST("ranking/cierres")
    Call<ApiResponse<JsonObject>> closeRankingMonth(@Header("Authorization") String bearerToken,
                                                   @Body JsonObject body);
    @GET("bienestar/tests")
    Call<ApiResponse<JsonObject>> getOfficialWellnessTests(@Header("Authorization") String bearerToken);
    @GET("bienestar/campanas")
    Call<ApiResponse<JsonObject>> getOwnWellnessCampaigns(@Header("Authorization") String bearerToken);
    @GET("bienestar/campanas/{id}")
    Call<ApiResponse<JsonObject>> getOwnWellnessCampaign(@Header("Authorization") String bearerToken,
                                                         @Path("id") long campaignId);
    @GET("bienestar/actividades")
    Call<ApiResponse<JsonObject>> getOwnWellnessActivities(@Header("Authorization") String bearerToken);
    @GET("bienestar/mis-respuestas")
    Call<ApiResponse<JsonObject>> getOwnWellnessHistory(@Header("Authorization") String bearerToken);
    @GET("bienestar/mis-respuestas/{id}")
    Call<ApiResponse<JsonObject>> getOwnWellnessDetail(@Header("Authorization") String bearerToken,
                                                       @Path("id") long evaluationId);
    @POST("bienestar/respuestas")
    Call<ApiResponse<JsonObject>> submitWellnessAnswers(@Header("Authorization") String bearerToken,
                                                        @Body Map<String, Object> body);
    @GET("auth/seguridad/dispositivos")
    Call<ApiResponse<JsonObject>> getSecurityDevices(@Header("Authorization") String bearerToken);
    @POST("auth/seguridad/revocar")
    Call<ApiResponse<JsonObject>> revokeOwnBiometrics(@Header("Authorization") String bearerToken,
                                                       @Body Map<String, Object> body);
    @POST("auth/seguridad/huella/desafio")
    Call<ApiResponse<AndroidAccessChallenge>> beginFingerprintRecovery(@Header("Authorization") String bearerToken,
                                                                        @Body Map<String, Object> body);
    @POST("auth/seguridad/huella/confirmar")
    Call<ApiResponse<JsonObject>> confirmFingerprintRecovery(@Header("Authorization") String bearerToken,
                                                               @Body Map<String, Object> body);
    @GET("notificaciones")
    Call<ApiResponse<JsonObject>> getOwnNotifications(@Header("Authorization") String bearerToken);
    @POST("notificaciones/{id}/lectura")
    Call<ApiResponse<JsonObject>> setNotificationRead(@Header("Authorization") String bearerToken,
                                                       @Path("id") long id, @Body Map<String, Object> body);
    @POST("notificaciones/marcar-todas")
    Call<ApiResponse<JsonObject>> markAllNotificationsRead(@Header("Authorization") String bearerToken,
                                                            @Body Map<String, Object> body);
    @POST("kpi/registros/sync")
    Call<ApiResponse<KpiSyncReceipt>> syncKpiRecords(@Header("Authorization") String bearerToken,
                                                      @Body Map<String, Object> batch);
    @POST("kpi/formularios/sync")
    Call<ApiResponse<JsonObject>> syncKpiForms(@Header("Authorization") String bearerToken,
                                               @Body JsonObject batch);
    @GET("kpi/formularios")
    Call<ApiResponse<JsonObject>> getKpiForms(@Header("Authorization") String bearerToken,
                                              @Query("tipo_formulario") String formType);
    @GET("kpi/formularios/permitidos")
    Call<ApiResponse<JsonObject>> getAllowedKpiForms(@Header("Authorization") String bearerToken);
    @GET("kpi/formularios")
    Call<ApiResponse<JsonObject>> getKpiFormsPage(@Header("Authorization") String bearerToken,
                                                  @Query("tipo_formulario") String formType,
                                                  @Query("after_id") long afterId,
                                                  @Query("limit") int limit);
    @GET("kpi/formularios")
    Call<ApiResponse<JsonObject>> getKpiForm(@Header("Authorization") String bearerToken,
                                             @Query("tipo_formulario") String formType,
                                             @Query("registro_uid") String recordUid);
    @GET("kpi/formularios")
    Call<ApiResponse<JsonObject>> getKpiFormsWorkerPage(@Header("Authorization") String bearerToken,
                                                        @Query("id_trabajador") int workerId,
                                                        @Query("after_id") long afterId,
                                                        @Query("limit") int limit);
    @GET("kpi/configuracion/borradores")
    Call<ApiResponse<JsonObject>> getKpiDrafts(@Header("Authorization") String bearerToken);
    @POST("kpi/configuracion/borradores")
    Call<ApiResponse<JsonObject>> saveKpiDraft(@Header("Authorization") String bearerToken,
                                               @Body JsonObject draft);
    @GET("personal")
    Call<ApiResponse<JsonObject>> getPersonal(@Header("Authorization") String bearerToken,
                                               @Query("limit") int limit, @Query("offset") int offset);

    @GET("personal/{id}")
    Call<ApiResponse<JsonObject>> getPersonalDetail(@Header("Authorization") String bearerToken,
                                                     @retrofit2.http.Path("id") int workerId);
    @POST("auth/biometria/registro")
    Call<ApiResponse<AndroidAccessChallenge>> registerAndroid(@Header("Authorization") String bearer, @Body Map<String, Object> body);
    @POST("auth/biometria/confirmar")
    Call<ApiResponse<Object>> confirmAndroid(@Header("Authorization") String bearer, @Body Map<String, Object> body);
    @POST("auth/biometria/desafio")
    Call<ApiResponse<AndroidAccessChallenge>> challengeAndroid(@Body Map<String, Object> body);
    @POST("auth/biometria/login")
    Call<ApiResponse<LoginResponseData.AuthTokens>> loginAndroid(@Body Map<String, Object> body);
    @POST("auth/biometria/revocar")
    Call<ApiResponse<Object>> revokeAndroid(@Header("Authorization") String bearer, @Body Map<String, Object> body);
    @POST("auth/logout")
    Call<ApiResponse<Object>> logout(@Header("Authorization") String bearer, @Body Map<String, Object> body);

    @POST("auth/login")
    Call<ApiResponse<LoginResponseData>> login(@Body LoginRequest request);

    @GET("auth/me")
    Call<ApiResponse<LoginResponseData>> getProfile(@Header("Authorization") String bearerToken);

    @POST("auth/refresh")
    Call<ApiResponse<LoginResponseData.AuthTokens>> refresh(@Body Map<String, String> request);

    @GET("dashboard/resumen")
    Call<ApiResponse<JsonObject>> getDashboardResumen(
        @Header("Authorization") String bearerToken,
        @Query("periodo") String periodo
    );

    @GET("premios")
    Call<ApiResponse<JsonObject>> getPublishedRewards(@Header("Authorization") String bearerToken);

    @GET("premios/admin")
    Call<ApiResponse<JsonObject>> getAdminRewards(@Header("Authorization") String bearerToken);
    @GET("premios/{id}")
    Call<ApiResponse<JsonObject>> getRewardDetail(@Header("Authorization") String bearerToken, @Path("id") int id);

    @POST("premios/admin")
    Call<ApiResponse<JsonObject>> createReward(@Header("Authorization") String bearerToken,
                                               @Body JsonObject body);

    @PUT("premios/admin/{id}")
    Call<ApiResponse<JsonObject>> updateReward(@Header("Authorization") String bearerToken,
                                               @Path("id") int id, @Body JsonObject body);

    @DELETE("premios/admin/{id}")
    Call<ApiResponse<JsonObject>> deleteReward(@Header("Authorization") String bearerToken,
                                               @Path("id") int id);

    @Multipart
    @POST("premios/admin/{id}/imagen")
    Call<ApiResponse<JsonObject>> uploadRewardImage(@Header("Authorization") String bearerToken,
                                                    @Path("id") int id,
                                                    @Part MultipartBody.Part image);

    @GET("premios/mis-puntos")
    Call<ApiResponse<JsonObject>> getOwnPoints(@Header("Authorization") String bearerToken);

    @GET("kpi/mis-kpis")
    Call<ApiResponse<KpiServerAssignments>> getMisKpis(
        @Header("Authorization") String bearerToken,
        @Query("periodo") String periodo
    );

    @POST("auth/face/challenge")
    Call<ApiResponse<pe.net.ws.kpiweb.features.auth.models.FaceChallengeResponse>> challengeFace(@Body Map<String, Object> body);

    @POST("auth/face/verify")
    Call<ApiResponse<LoginResponseData>> verifyFace(@Body Map<String, Object> body);
}
