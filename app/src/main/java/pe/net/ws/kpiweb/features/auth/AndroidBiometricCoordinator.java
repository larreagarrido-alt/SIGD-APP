package pe.net.ws.kpiweb.features.auth;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.network.ApiService;
import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Server-registered, per-installation key; no refresh-token shortcut or fingerprint export. */
final class AndroidBiometricCoordinator {
    interface Listener {
        void loading(boolean value);
        void error(int message);
        default void rateLimited(String retryAfter) { error(R.string.auth_rate_limit); }
        void linked();
        void alreadyLinked();
        void forgotten();
        void authenticated(LoginResponseData profile, LoginResponseData.AuthTokens tokens);
    }

    interface PromptBridge {
        void authenticate(AndroidAccessChallenge challenge, boolean registration, Signature signature, PromptCallback callback);
        void cancel();
    }

    interface PromptCallback {
        void onSucceeded(Signature signature);
        void onError(int code, CharSequence message);
        void onFailed();
    }

    interface InactiveChecker {
        boolean isInactive();
    }

    private final Listener listener;
    private final AndroidDeviceStore store;
    private final SessionManager session;
    private final ApiService api;
    private final PromptBridge promptBridge;
    private final InactiveChecker inactiveChecker;
    private Call<?> call;
    private AndroidAccessChallenge pending;
    private boolean registering;
    private boolean forget;

    AndroidBiometricCoordinator(AppCompatActivity activity, Listener listener, Bundle state, boolean forget) {
        this(
                listener,
                new AndroidDeviceStore(activity),
                new SessionManager(activity),
                ApiClient.getService(),
                new AndroidBiometricPromptBridge(activity),
                () -> activity.isFinishing() || activity.isDestroyed(),
                state,
                forget
        );
    }

    AndroidBiometricCoordinator(
            Listener listener,
            AndroidDeviceStore store,
            SessionManager session,
            ApiService api,
            PromptBridge promptBridge,
            @Nullable InactiveChecker inactiveChecker,
            @Nullable Bundle state,
            boolean forget
    ) {
        this.listener = listener;
        this.store = store;
        this.session = session;
        this.api = api;
        this.promptBridge = promptBridge;
        this.inactiveChecker = inactiveChecker != null ? inactiveChecker : () -> false;
        this.forget = forget;
        if (state != null) {
            String challengeJson = state.getString("android_challenge");
            if (challengeJson != null) {
                pending = new Gson().fromJson(challengeJson, AndroidAccessChallenge.class);
            }
            registering = state.getBoolean("android_registering");
        }
        if (pending != null) listener.loading(true);
    }

    void saveState(Bundle state) {
        state.putString("android_challenge", pending == null ? null : new Gson().toJson(pending));
        state.putBoolean("android_registering", registering);
    }

    void destroy() {
        if (call != null) call.cancel();
        if (promptBridge != null) promptBridge.cancel();
    }

    /**
     * Called only right after a successful password login (fresh PASSWORD session).
     * A key already stored on this installation is never overwritten blindly: the server decides
     * whether it is still ACTIVE. It is released only when the server reports it can never be used
     * again (missing, revoked or still pending), and only then a new registration starts.
     */
    void register(String username) {
        if (!store.hasCredential()) { begin(username); return; }
        if (!store.username().isEmpty() && !store.username().equals(username)) {
            fail(R.string.auth_android_phone_already_linked);
            return;
        }
        listener.loading(true);
        send(api.challengeAndroid(identity()), challenge -> {
            // The server confirms this installation has an ACTIVE record in MySQL.
            if (!store.username().equals(username)) {
                fail(R.string.auth_android_phone_already_linked);
                return;
            }

            // Verify local private key possession:
            try {
                store.signingOperation();
            } catch (GeneralSecurityException e) {
                // Keystore key is missing or invalidated; cannot authenticate.
                fail(R.string.auth_biometric_key_unavailable);
                return;
            }

            if (store.isEnabled()) {
                // Link was already confirmed and private key is verified present.
                try {
                    store.save(store.id(), username, true);
                } catch (GeneralSecurityException error) {
                    fail(R.string.auth_biometric_key_unavailable);
                    return;
                }
                listener.loading(false);
                listener.alreadyLinked();
            } else {
                // State recovery (e.g. previous save failure or dropped network after server activated).
                // Receiving challenge from server does NOT prove private key possession or grant access;
                // we must complete biometric confirmation with prompt + signature!
                showPrompt(challenge, false);
            }
        }, code -> {
            if (!isDeadLink(code)) return false;
            if (!store.clear()) {
                fail(R.string.auth_biometric_key_unavailable);
                return true;
            }
            begin(username);
            return true;
        });
    }

    private void begin(String username) {
        listener.loading(true);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("device_uid", store.uid());
            body.put("clave_publica", store.generateKey());
            String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "Android";
            String model = Build.MODEL != null ? Build.MODEL : "Device";
            body.put("nombre", manufacturer + " " + model);
            send(api.registerAndroid(session.getBearerToken(), body), challenge -> {
                if (challenge == null || challenge.accessId <= 0) {
                    fail(R.string.auth_server_error);
                    return;
                }
                // Pending metadata is stored before the prompt so a lost confirmation can be resolved later.
                try {
                    store.save(challenge.accessId, username, false);
                } catch (GeneralSecurityException error) {
                    fail(R.string.auth_biometric_key_unavailable);
                    return;
                }
                showPrompt(challenge, true);
            });
        } catch (GeneralSecurityException | IllegalStateException error) {
            fail(R.string.auth_biometric_key_unavailable);
        }
    }

    void unlock() {
        if (!store.hasCredential()) {
            fail(R.string.auth_android_link_missing);
            return;
        }
        listener.loading(true);
        send(api.challengeAndroid(identity()), challenge -> showPrompt(challenge, false));
    }

    private Map<String, Object> identity() {
        Map<String, Object> body = new HashMap<>();
        body.put("id_acceso", store.id());
        body.put("device_uid", store.uid());
        return body;
    }

    private void showPrompt(AndroidAccessChallenge challenge, boolean registration) {
        if (challenge == null || challenge.message == null || challenge.message.length() > 600 || challenge.challengeId <= 0) {
            fail(R.string.auth_server_error);
            return;
        }
        try {
            pending = challenge;
            registering = registration;
            Signature signature = store.signingOperation();
            promptBridge.authenticate(challenge, registration, signature, new PromptCallback() {
                @Override
                public void onSucceeded(Signature operation) {
                    if (inactive()) return;
                    if (pending == null) return; // A repeated prompt callback cannot submit the same challenge twice.
                    try {
                        if (operation == null || pending == null || pending.message == null) {
                            throw new GeneralSecurityException("Invalid signature operation or challenge");
                        }
                        operation.update(pending.message.getBytes(StandardCharsets.UTF_8));
                        Map<String, Object> body = identity();
                        body.put("challenge_id", pending.challengeId);
                        body.put("firma_base64", AndroidDeviceStore.encodeBase64(operation.sign()));
                        pending = null;
                        if (registering) confirm(body); else login(body);
                    } catch (GeneralSecurityException error) {
                        pending = null;
                        fail(R.string.auth_biometric_key_unavailable);
                    }
                }

                @Override
                public void onError(int code, CharSequence message) {
                    if (!inactive()) {
                        pending = null;
                        fail(registering ? R.string.auth_biometric_link_cancelled : R.string.auth_biometric_cancelled);
                    }
                }

                @Override
                public void onFailed() {
                    if (!inactive()) {
                        listener.error(R.string.auth_biometric_retry);
                    }
                }
            });
        } catch (GeneralSecurityException error) {
            pending = null;
            fail(R.string.auth_biometric_key_unavailable);
        }
    }

    private void confirm(Map<String, Object> body) {
        send(api.confirmAndroid(session.getBearerToken(), body), ignored -> {
            try {
                store.save(store.id(), store.username(), true);
                listener.loading(false);
                listener.linked();
            } catch (GeneralSecurityException error) {
                fail(R.string.auth_biometric_key_unavailable);
            }
        });
    }

    private void login(Map<String, Object> body) {
        send(api.loginAndroid(body), tokens -> {
            if (!validTokens(tokens)) {
                fail(R.string.auth_server_error);
                return;
            }
            try {
                store.save(store.id(), store.username(), true);
            } catch (GeneralSecurityException error) {
                fail(R.string.auth_biometric_key_unavailable);
                return;
            }
            if (forget) {
                send(api.revokeAndroid("Bearer " + tokens.getAccessToken(), identity()), ignored -> {
                    session.clear();
                    forget = false;
                    listener.loading(false);
                    listener.forgotten();
                });
            } else {
                send(api.getProfile("Bearer " + tokens.getAccessToken()), profile -> {
                    if (profile == null || profile.getUser() == null) {
                        fail(R.string.auth_server_error);
                        return;
                    }
                    try {
                        store.save(store.id(), profile.getUser().getUsername(), true);
                    } catch (GeneralSecurityException error) {
                        fail(R.string.auth_biometric_key_unavailable);
                        return;
                    }
                    listener.loading(false);
                    listener.authenticated(profile, tokens);
                });
            }
        });
    }

    /** Server states after which the stored key can never authenticate again. */
    static boolean isDeadLink(String code) {
        return code != null && (code.equals("DEVICE_NOT_FOUND") || code.equals("DEVICE_REVOKED") || code.equals("REGISTRATION_INCOMPLETE"));
    }

    private interface Success<T> { void accept(T value); }
    /** Returns true when the error was fully handled by the caller. */
    private interface Failure { boolean handle(String code); }
    private <T> void send(Call<ApiResponse<T>> request, Success<T> success) { send(request, success, null); }
    private <T> void send(Call<ApiResponse<T>> request, Success<T> success, Failure failure) {
        call = request;
        request.enqueue(new Callback<ApiResponse<T>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<T>> source, @NonNull Response<ApiResponse<T>> response) {
                if (inactive()) return;
                if (response.code() == 429) {
                    listener.loading(false);
                    listener.rateLimited(response.headers().get("Retry-After"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                    success.accept(response.body().getData());
                    return;
                }
                String code = "";
                try {
                    if (response.errorBody() != null) {
                        ApiResponse<?> error = new Gson().fromJson(response.errorBody().string(), ApiResponse.class);
                        if (error != null && error.getErrorCode() != null) code = error.getErrorCode();
                    }
                } catch (Exception ignored) { }
                if (failure != null && failure.handle(code)) return;
                if (isDeadLink(code)) {
                    if (!store.clear()) {
                        fail(R.string.auth_biometric_key_unavailable);
                        return;
                    }
                    fail(R.string.auth_android_link_missing);
                } else if (code.equals("ACCOUNT_ALREADY_LINKED")) {
                    fail(R.string.auth_android_account_already_linked);
                } else if (code.equals("FRESH_PASSWORD_REQUIRED")) {
                    fail(R.string.auth_android_fresh_password);
                } else if (code.equals("UNAUTHORIZED") || code.equals("INVALID_TOKEN")) {
                    fail(R.string.auth_android_session_rejected);
                } else if (code.equals("INVALID_SIGNATURE")) {
                    fail(R.string.auth_android_signature_rejected);
                } else if (response.code() == 404) {
                    fail(R.string.auth_android_server_update);
                } else if (response.code() == 401 || response.code() == 403) {
                    fail(R.string.auth_android_access_denied);
                } else {
                    fail(R.string.auth_server_error);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<T>> source, @NonNull Throwable error) {
                if (!inactive() && !source.isCanceled()) {
                    fail(R.string.auth_connection_error);
                }
            }
        });
    }

    private boolean validTokens(LoginResponseData.AuthTokens tokens) {
        return tokens != null && tokens.getAccessToken() != null && tokens.getAccessToken().matches("[a-fA-F0-9]{64}")
                && tokens.getRefreshToken() != null && tokens.getRefreshToken().matches("[a-fA-F0-9]{64}");
    }

    private void fail(int message) {
        listener.loading(false);
        listener.error(message);
    }

    private boolean inactive() {
        return inactiveChecker.isInactive();
    }

    private static final class AndroidBiometricPromptBridge implements PromptBridge {
        private final AppCompatActivity activity;
        private BiometricPrompt prompt;

        AndroidBiometricPromptBridge(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void authenticate(AndroidAccessChallenge challenge, boolean registration, Signature signature, PromptCallback callback) {
            prompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    Signature op = result.getCryptoObject() != null ? result.getCryptoObject().getSignature() : null;
                    callback.onSucceeded(op);
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    callback.onError(errorCode, errString);
                }

                @Override
                public void onAuthenticationFailed() {
                    callback.onFailed();
                }
            });

            BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(registration ? R.string.auth_biometric_enroll_title : R.string.auth_biometric_unlock_title))
                    .setSubtitle(activity.getString(R.string.auth_biometric_subtitle))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(activity.getString(R.string.auth_use_password))
                    .build();

            prompt.authenticate(info, new BiometricPrompt.CryptoObject(signature));
        }

        @Override
        public void cancel() {
            if (prompt != null) {
                prompt.cancelAuthentication();
            }
        }
    }
}
