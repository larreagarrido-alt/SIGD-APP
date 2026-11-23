package pe.net.ws.kpiweb.features.auth;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import com.google.gson.JsonObject;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.network.ApiService;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;
import pe.net.ws.kpiweb.features.auth.models.LoginRequest;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignments;
import retrofit2.Call;
import okhttp3.MultipartBody;

public class TestApiService implements ApiService {
    @Override public Call<ApiResponse<JsonObject>> getOwnAttendance(String bearer,String month) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getAdminWellnessOverview(String bearer,String month,long campaignId,int agencyId) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getAdminWellnessCampaigns(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> createAdminWellnessCampaign(String bearer,JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> updateAdminWellnessCampaign(String bearer,long id,JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getAdminRyffQuestionnaire(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getAdminWellnessPlans(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> createAdminWellnessPlan(String bearer,JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> updateAdminWellnessPlan(String bearer,long id,JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> askAdminWellnessAssistant(String bearer,JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOwnWellnessCampaigns(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOwnWellnessCampaign(String bearer,long id) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOwnWellnessActivities(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getLatestRanking(String bearer, String periodType) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getRankingMonth(String bearer, String periodType, String month) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getRankingDetail(String bearer, int workerId, String periodType) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getRankingPeriods(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOfficialKpiDefinitions(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> createOfficialKpiDefinition(String bearer, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> approveOfficialKpiDefinition(String bearer, int id, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> assignOfficialKpi(String bearer, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOfficialKpiReports(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> reviewOfficialKpiReport(String bearer, int id, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> approveOfficialKpiEvaluation(String bearer, int id, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> approveRankingPolicy(String bearer, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> closeRankingMonth(String bearer, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getAdminRewards(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getRewardDetail(String bearer, int id) { return null; }
    @Override public Call<ApiResponse<JsonObject>> createReward(String bearer, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> updateReward(String bearer, int id, JsonObject body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> deleteReward(String bearer, int id) { return null; }
    @Override public Call<ApiResponse<JsonObject>> uploadRewardImage(String bearer, int id, MultipartBody.Part image) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOfficialWellnessTests(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOwnWellnessHistory(String bearer) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getOwnWellnessDetail(String bearer, long evaluationId) { return null; }
    @Override public Call<ApiResponse<JsonObject>> submitWellnessAnswers(String bearer, Map<String, Object> body) { return null; }
    @Override public Call<ApiResponse<JsonObject>> getSecurityDevices(String bearer) {
        throw new IllegalStateException("Unexpected security device request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> revokeOwnBiometrics(String bearer, Map<String, Object> body) {
        throw new IllegalStateException("Unexpected security device mutation in auth test");
    }
    @Override public Call<ApiResponse<AndroidAccessChallenge>> beginFingerprintRecovery(String bearer, Map<String, Object> body) {
        throw new IllegalStateException("Unexpected fingerprint recovery request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> confirmFingerprintRecovery(String bearer, Map<String, Object> body) {
        throw new IllegalStateException("Unexpected fingerprint recovery mutation in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getOwnNotifications(String bearer) {
        throw new IllegalStateException("Unexpected notifications request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> setNotificationRead(String bearer, long id, Map<String, Object> body) {
        throw new IllegalStateException("Unexpected notifications mutation in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> markAllNotificationsRead(String bearer, Map<String, Object> body) {
        throw new IllegalStateException("Unexpected notifications mutation in auth test");
    }
    @Override
    public Call<ApiResponse<pe.net.ws.kpiweb.features.kpi.sync.KpiSyncReceipt>> syncKpiRecords(
            String bearerToken, Map<String, Object> batch) {
        throw new IllegalStateException("Unexpected KPI sync request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> syncKpiForms(String bearerToken, JsonObject batch) {
        throw new IllegalStateException("Unexpected KPI form sync request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getKpiForms(String bearerToken, String formType) {
        throw new IllegalStateException("Unexpected KPI form read request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getAllowedKpiForms(String bearerToken) {
        throw new IllegalStateException("Unexpected KPI form mapping request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getKpiFormsPage(String bearerToken, String formType, long afterId, int limit) {
        throw new IllegalStateException("Unexpected KPI form page request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getKpiForm(String bearerToken, String formType, String recordUid) {
        throw new IllegalStateException("Unexpected KPI form receipt read in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getKpiFormsWorkerPage(String bearerToken, int workerId, long afterId, int limit) {
        throw new IllegalStateException("Unexpected worker KPI form request in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> getKpiDrafts(String bearerToken) {
        throw new IllegalStateException("Unexpected KPI draft read in auth test");
    }
    @Override public Call<ApiResponse<JsonObject>> saveKpiDraft(String bearerToken, JsonObject draft) {
        throw new IllegalStateException("Unexpected KPI draft write in auth test");
    }
    @Override
    public Call<ApiResponse<JsonObject>> getPersonal(String bearerToken, int limit, int offset) {
        throw new IllegalStateException("Unexpected Personal request in auth test");
    }

    @Override
    public Call<ApiResponse<JsonObject>> getPersonalDetail(String bearerToken, int workerId) {
        throw new IllegalStateException("Unexpected Personal detail request in auth test");
    }
    public final Queue<Call<ApiResponse<AndroidAccessChallenge>>> registerAndroidCalls = new LinkedList<>();
    public final Queue<Call<ApiResponse<Object>>> confirmAndroidCalls = new LinkedList<>();
    public final Queue<Call<ApiResponse<AndroidAccessChallenge>>> challengeAndroidCalls = new LinkedList<>();
    public final Queue<Call<ApiResponse<LoginResponseData.AuthTokens>>> loginAndroidCalls = new LinkedList<>();
    public final Queue<Call<ApiResponse<Object>>> revokeAndroidCalls = new LinkedList<>();
    public final Queue<Call<ApiResponse<LoginResponseData>>> getProfileCalls = new LinkedList<>();

    public String lastAuthHeader;
    public Map<String, Object> lastRegisterBody;
    public Map<String, Object> lastConfirmBody;
    public Map<String, Object> lastChallengeBody;
    public Map<String, Object> lastLoginBody;
    public Map<String, Object> lastRevokeBody;

    public int registerAndroidCount = 0;
    public int confirmAndroidCount = 0;
    public int challengeAndroidCount = 0;
    public int loginAndroidCount = 0;
    public int revokeAndroidCount = 0;

    @Override
    public Call<ApiResponse<AndroidAccessChallenge>> registerAndroid(String bearer, Map<String, Object> body) {
        this.registerAndroidCount++;
        this.lastAuthHeader = bearer;
        this.lastRegisterBody = body;
        return pollOrDefault(registerAndroidCalls, "registerAndroid");
    }

    @Override
    public Call<ApiResponse<Object>> confirmAndroid(String bearer, Map<String, Object> body) {
        this.confirmAndroidCount++;
        this.lastAuthHeader = bearer;
        this.lastConfirmBody = body;
        return pollOrDefault(confirmAndroidCalls, "confirmAndroid");
    }

    @Override
    public Call<ApiResponse<AndroidAccessChallenge>> challengeAndroid(Map<String, Object> body) {
        this.challengeAndroidCount++;
        this.lastChallengeBody = body;
        return pollOrDefault(challengeAndroidCalls, "challengeAndroid");
    }

    @Override
    public Call<ApiResponse<LoginResponseData.AuthTokens>> loginAndroid(Map<String, Object> body) {
        this.loginAndroidCount++;
        this.lastLoginBody = body;
        return pollOrDefault(loginAndroidCalls, "loginAndroid");
    }

    @Override
    public Call<ApiResponse<Object>> revokeAndroid(String bearer, Map<String, Object> body) {
        this.revokeAndroidCount++;
        this.lastAuthHeader = bearer;
        this.lastRevokeBody = body;
        return pollOrDefault(revokeAndroidCalls, "revokeAndroid");
    }

    @Override
    public Call<ApiResponse<LoginResponseData>> getProfile(String bearerToken) {
        this.lastAuthHeader = bearerToken;
        return pollOrDefault(getProfileCalls, "getProfile");
    }

    @Override
    public Call<ApiResponse<Object>> logout(String bearer, Map<String, Object> body) {
        return TestCall.success(ApiResponse.success(new Object()));
    }

    @Override
    public Call<ApiResponse<LoginResponseData>> login(LoginRequest request) {
        return TestCall.success(ApiResponse.success(new LoginResponseData()));
    }

    @Override
    public Call<ApiResponse<LoginResponseData.AuthTokens>> refresh(Map<String, String> request) {
        return TestCall.success(ApiResponse.success(new LoginResponseData.AuthTokens()));
    }

    @Override
    public Call<ApiResponse<com.google.gson.JsonObject>> getDashboardResumen(String bearerToken, String periodo) {
        return TestCall.success(ApiResponse.success(new com.google.gson.JsonObject()));
    }

    @Override
    public Call<ApiResponse<JsonObject>> getPublishedRewards(String bearerToken) {
        throw new IllegalStateException("Unexpected rewards request in auth test");
    }

    @Override
    public Call<ApiResponse<JsonObject>> getOwnPoints(String bearerToken) {
        throw new IllegalStateException("Unexpected points request in auth test");
    }

    @Override
    public Call<ApiResponse<KpiServerAssignments>> getMisKpis(String bearerToken, String periodo) {
        return TestCall.success(ApiResponse.success(new KpiServerAssignments()));
    }

    public final Queue<Call<ApiResponse<pe.net.ws.kpiweb.features.auth.models.FaceChallengeResponse>>> challengeFaceCalls = new LinkedList<>();
    public final Queue<Call<ApiResponse<LoginResponseData>>> verifyFaceCalls = new LinkedList<>();
    public Map<String, Object> lastFaceChallengeBody;
    public Map<String, Object> lastFaceVerifyBody;
    public int challengeFaceCount = 0;
    public int verifyFaceCount = 0;

    @Override
    public Call<ApiResponse<pe.net.ws.kpiweb.features.auth.models.FaceChallengeResponse>> challengeFace(Map<String, Object> body) {
        this.challengeFaceCount++;
        this.lastFaceChallengeBody = body;
        return pollOrDefault(challengeFaceCalls, "challengeFace");
    }

    @Override
    public Call<ApiResponse<LoginResponseData>> verifyFace(Map<String, Object> body) {
        this.verifyFaceCount++;
        this.lastFaceVerifyBody = body;
        return pollOrDefault(verifyFaceCalls, "verifyFace");
    }

    private <T> Call<ApiResponse<T>> pollOrDefault(Queue<Call<ApiResponse<T>>> queue, String method) {
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        throw new IllegalStateException("Unexpected call to " + method + "; queue is empty in test setup.");
    }
}
