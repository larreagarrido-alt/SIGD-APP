package pe.net.ws.kpiweb.features.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.network.ApiService;
import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.FaceChallengeResponse;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Coordinator for local facial capture evaluation. Session issuance remains blocked.
 * Uses local temporal landmark checks and a V011-scoped server challenge.
 * These checks are not trusted PAD evidence. This coordinator cannot emit sessions.
 */
public class FaceAuthCoordinator {

    public interface Listener {
        void onStatusChanged(int messageResId, boolean isScanning);
        void onLivenessPrompt(String instruction);
        void onAuthenticated(LoginResponseData data);
        void onCaptureEvaluated(String message);
        void onError(int messageResId);
        void onLoading(boolean loading);
    }

    public interface CameraBridge {
        boolean isCameraAvailable();
        boolean hasCameraPermission();
        void startPreview();
        void configureChallenge(FaceChallengeResponse challenge);
        void captureAndProcess(CaptureCallback callback);
        void stopPreview();
    }

    public interface CaptureCallback {
        void onResult(CaptureResult result);
    }

    public static class CaptureResult {
        public enum Status {
            SUCCESS,
            NO_FACE,
            MULTIPLE_FACES,
            LOW_QUALITY,
            SPOOF_DETECTED,
            CONTINUITY_LOST,
            CANCELLED,
            TIMEOUT,
            EXTRACTOR_UNAVAILABLE
        }

        public final Status status;
        public final String facePayload;
        public final boolean livenessVerified;

        public CaptureResult(Status status, @Nullable String facePayload, boolean livenessVerified) {
            this.status = status;
            this.facePayload = facePayload;
            this.livenessVerified = livenessVerified;
        }

        public static CaptureResult success(String facePayload) {
            return new CaptureResult(Status.SUCCESS, facePayload, true);
        }

        public static CaptureResult error(Status status) {
            return new CaptureResult(status, null, false);
        }
    }

    public interface InactiveChecker {
        boolean isInactive();
    }

    private final Listener listener;
    private final CameraBridge cameraBridge;
    private final AndroidDeviceStore store;
    private final ApiService api;
    private final InactiveChecker inactiveChecker;

    private Call<?> pendingCall;
    private boolean inProgress = false;
    private int generation;
    private FaceChallengeResponse activeChallenge;

    public FaceAuthCoordinator(
            @NonNull Listener listener,
            @NonNull CameraBridge cameraBridge,
            @NonNull AndroidDeviceStore store,
            @NonNull SessionManager session,
            @NonNull ApiService api,
            @Nullable InactiveChecker inactiveChecker
    ) {
        this.listener = listener;
        this.cameraBridge = cameraBridge;
        this.store = store;
        this.api = api;
        this.inactiveChecker = inactiveChecker != null ? inactiveChecker : () -> false;
    }

    private boolean inactive() {
        return inactiveChecker.isInactive();
    }

    public void startFaceAuth(@Nullable String username) { start(username, false); }

    /** Explicit offline evaluation. It never verifies account identity or requests a session. */
    public void startLocalEvaluation(@Nullable String username) { start(username, true); }

    private void start(@Nullable String username, boolean localOnly) {
        if (inactive() || inProgress) return;
        final int run=++generation;

        // 1. Verificar disponibilidad de cámara en hardware
        if (!cameraBridge.isCameraAvailable()) {
            listener.onError(R.string.auth_camera_missing);
            return;
        }

        // 2. Verificar permisos de cámara
        if (!cameraBridge.hasCameraPermission()) {
            listener.onError(R.string.auth_face_permission_denied);
            return;
        }

        // Si no se proporcionó username explícito, intentar usar el usuario vinculado
        String targetUser = (username != null && !username.trim().isEmpty())
                ? username.trim()
                : (store.hasCredential() ? store.username() : "");

        if (targetUser.isEmpty()) {
            listener.onError(R.string.auth_enter_username);
            return;
        }

        // Verificar consistencia de dispositivo V011: celular no puede autenticar otra cuenta
        if (store.hasCredential() && !store.username().isEmpty() && !store.username().equals(targetUser)) {
            listener.onError(R.string.auth_android_phone_already_linked);
            return;
        }

        if (localOnly && !store.hasCredential()) {
            listener.onError(R.string.auth_android_link_missing);
            return;
        }
        inProgress = true;
        listener.onLoading(true);
        listener.onStatusChanged(R.string.auth_face_preparing, false);

        if (localOnly) {
            FaceChallengeResponse local = new FaceChallengeResponse();
            java.util.List<String> actions=new java.util.ArrayList<>(java.util.Arrays.asList("BLINK","TURN_LEFT","TURN_RIGHT"));
            java.util.Collections.shuffle(actions,new java.security.SecureRandom());
            local.actions=actions.toArray(new String[0]);
            // This sequence is NOT a server-issued authentication challenge.
            local.guide="Prueba local de captura. No comprueba tu identidad ni inicia sesión.";
            captureForChallenge(targetUser,run,local);
            return;
        }

        // 3. Solicitar desafío de liveness al backend (anti-replay nonce)
        Map<String, Object> challengeReq = new HashMap<>();
        challengeReq.put("username", targetUser);
        challengeReq.put("device_uid", store.uid());

        Call<ApiResponse<FaceChallengeResponse>> call = api.challengeFace(challengeReq);
        this.pendingCall = call;

        call.enqueue(new Callback<ApiResponse<FaceChallengeResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<FaceChallengeResponse>> call,
                                   @NonNull Response<ApiResponse<FaceChallengeResponse>> response) {
                if (inactive() || run!=generation || !inProgress) return;

                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()) {
                    handleChallengeError(response);
                    return;
                }

                activeChallenge = response.body().getData();
                if (activeChallenge == null || activeChallenge.nonce == null || activeChallenge.actions == null || activeChallenge.actions.length != 3) {
                    inProgress=false;
                    listener.onLoading(false);
                    listener.onError(R.string.auth_server_error);
                    return;
                }

                captureForChallenge(targetUser,run,activeChallenge);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<FaceChallengeResponse>> call, @NonNull Throwable t) {
                if (inactive() || run!=generation || !inProgress) return;
                inProgress=false;
                listener.onLoading(false);
                listener.onError(R.string.auth_connection_error);
            }
        });
    }

    private void captureForChallenge(String targetUser,int run,FaceChallengeResponse challenge) {
        activeChallenge=challenge;
        listener.onLivenessPrompt(challenge.guide != null ? challenge.guide : "Sigue las instrucciones del visor");
        cameraBridge.configureChallenge(challenge);
        cameraBridge.startPreview();
        cameraBridge.captureAndProcess(result -> {
            if(inactive() || run!=generation || !inProgress)return;
            cameraBridge.stopPreview();
            processCaptureResult(targetUser,result);
        });
    }

    private void processCaptureResult(String targetUser, CaptureResult result) {
        inProgress=false;
        if (result == null || result.status == CaptureResult.Status.CANCELLED) {
            listener.onLoading(false);
            listener.onError(R.string.auth_face_cancelled);
            return;
        }

        switch (result.status) {
            case CONTINUITY_LOST:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_continuity_lost);
                return;
            case TIMEOUT:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_timeout);
                return;
            case NO_FACE:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_no_face_detected);
                return;
            case MULTIPLE_FACES:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_multiple_faces);
                return;
            case LOW_QUALITY:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_low_quality);
                return;
            case SPOOF_DETECTED:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_spoof_detected);
                return;
            case EXTRACTOR_UNAVAILABLE:
                listener.onLoading(false);
                listener.onError(R.string.auth_face_extractor_unavailable);
                return;
            case SUCCESS:
                if (!result.livenessVerified || result.facePayload == null
                        || result.facePayload.trim().isEmpty()
                        || "[]".equals(result.facePayload.trim())) {
                    listener.onLoading(false);
                    listener.onError(R.string.auth_face_extractor_unavailable);
                    return;
                }
                break;
        }

        // No private key access here: V011 requires a BiometricPrompt CryptoObject.
        // Local gestures and an embedding are not remotely verifiable PAD evidence.
        // Until that trust boundary is solved this flow cannot send a verification or save tokens.
        try {
            com.google.gson.JsonArray vector = com.google.gson.JsonParser.parseString(result.facePayload).getAsJsonArray();
            if (vector.size()!=128) throw new IllegalArgumentException("Invalid dimension");
            for(com.google.gson.JsonElement value:vector) {
                if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
                        || !Double.isFinite(value.getAsDouble())) throw new IllegalArgumentException("Invalid value");
            }
        } catch(RuntimeException invalid) {
            inProgress=false;listener.onLoading(false);
            listener.onError(R.string.auth_face_extractor_unavailable);return;
        }
        inProgress=false;
        listener.onLoading(false);
        listener.onStatusChanged(R.string.auth_face_capture_evaluated,false);
        listener.onCaptureEvaluated(null);
    }

    private void handleChallengeError(Response<?> response) {
        listener.onLoading(false);
        inProgress = false;
        String errorCode = extractErrorCode(response);

        if (response.code() == 429 || "TOO_MANY_ATTEMPTS".equals(errorCode)) {
            listener.onError(R.string.auth_rate_limit);
        } else if ("WORKER_WITHOUT_FACE_RECORD".equals(errorCode)) {
            listener.onError(R.string.auth_face_no_record);
        } else if ("ACCOUNT_INACTIVE".equals(errorCode)) {
            listener.onError(R.string.auth_inactive);
        } else if ("ACCOUNT_ALREADY_LINKED".equals(errorCode)) {
            listener.onError(R.string.auth_android_account_already_linked);
        } else if ("DEVICE_NOT_FOUND".equals(errorCode)) {
            listener.onError(R.string.auth_android_link_missing);
        } else if ("DEVICE_NOT_AUTHORIZED".equals(errorCode)) {
            listener.onError(R.string.auth_android_phone_already_linked);
        } else if ("REGISTRATION_INCOMPLETE".equals(errorCode)) {
            listener.onError(R.string.auth_biometric_link_cancelled);
        } else if (response.code() == 404 || "USER_NOT_FOUND".equals(errorCode)) {
            listener.onError(R.string.auth_invalid_credentials);
        } else {
            listener.onError(R.string.auth_server_error);
        }
    }

    private void handleVerifyError(Response<?> response) {
        String errorCode = extractErrorCode(response);

        if ("WORKER_WITHOUT_FACE_RECORD".equals(errorCode)) {
            listener.onError(R.string.auth_face_no_record);
        } else if ("FACE_MISMATCH".equals(errorCode)) {
            listener.onError(R.string.auth_face_mismatch);
        } else if ("INVALID_SIGNATURE".equals(errorCode)) {
            listener.onError(R.string.auth_android_signature_rejected);
        } else if ("SPOOF_DETECTED".equals(errorCode)) {
            listener.onError(R.string.auth_face_spoof_detected);
        } else if ("ACCOUNT_INACTIVE".equals(errorCode)) {
            listener.onError(R.string.auth_inactive);
        } else if ("ACCOUNT_ALREADY_LINKED".equals(errorCode)) {
            listener.onError(R.string.auth_android_account_already_linked);
        } else if ("DEVICE_NOT_FOUND".equals(errorCode)) {
            listener.onError(R.string.auth_android_link_missing);
        } else if ("DEVICE_NOT_AUTHORIZED".equals(errorCode)) {
            listener.onError(R.string.auth_android_phone_already_linked);
        } else if (response.code() == 429 || "TOO_MANY_ATTEMPTS".equals(errorCode)) {
            listener.onError(R.string.auth_rate_limit);
        } else if ("CHALLENGE_EXPIRED_OR_REUSED".equals(errorCode)) {
            listener.onError(R.string.auth_android_signature_rejected);
        } else {
            listener.onError(R.string.auth_server_error);
        }
    }

    private String extractErrorCode(Response<?> response) {
        if (response.errorBody() == null) return "";
        try {
            ApiResponse<?> error = new Gson().fromJson(response.errorBody().string(), ApiResponse.class);
            return error != null && error.getErrorCode() != null ? error.getErrorCode() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    public void cancel() {
        generation++;
        if (pendingCall != null) {
            pendingCall.cancel();
            pendingCall = null;
        }
        cameraBridge.stopPreview();
        inProgress = false;
        listener.onLoading(false);
        listener.onError(R.string.auth_face_cancelled);
    }

    public void destroy() {
        generation++;
        if (pendingCall != null) {
            pendingCall.cancel();
            pendingCall = null;
        }
        cameraBridge.stopPreview();
        inProgress = false;
    }
}
