package pe.net.ws.kpiweb.features.auth;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.TextureView;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.LoginRequest;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Arrays;
import javax.crypto.Cipher;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnFingerprint;
    private MaterialButton btnCamera;
    private CompoundButton rememberBiometric;
    private TextView errorView;
    private TextView biometricHint;
    private TextView loadingView;
    private View loadingRow;
    private SessionManager sessionManager;
    private AndroidDeviceStore biometricStore;
    private AndroidBiometricCoordinator androidBiometrics;
    private Call<?> pendingCall;
    private boolean busy;
    private boolean passwordRateLimited, fingerprintRateLimited, faceRateLimited;
    private final Handler rateLimitHandler = new Handler(Looper.getMainLooper());
    private enum LoginMethod { PASSWORD, FINGERPRINT, FACE }
    private boolean updatingBiometricSelection;
    private boolean biometricKeyUnavailable;
    private int biometricKeyCheckGeneration;
    private final java.util.concurrent.ExecutorService biometricKeyChecks=java.util.concurrent.Executors.newSingleThreadExecutor();
    private long faceTapStarted;



    private FaceIdentityCoordinator faceCoordinator;
    private pe.net.ws.kpiweb.core.session.FaceDeviceStore faceDeviceStore;
    private final java.util.concurrent.ExecutorService faceCrypto=java.util.concurrent.Executors.newSingleThreadExecutor();
    private int facePreflight;
    private androidx.appcompat.app.AlertDialog faceScanDialog;
    private FaceOverlayView faceOverlayView;
    private TextView tvFaceStatus;
    private TextView tvFacePrompt;
    private ProgressBar pbFaceProgress;
    private SigdSplashController splashController;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    clearError();
                    checkAndStartFaceRecognition();
                } else {
                    showError(R.string.auth_face_permission_denied);
                }
            });

    private final ActivityResultLauncher<Intent> biometricSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                // Settings may return RESULT_CANCELED even when enrollment succeeded.
                // Registering a system fingerprint never authorizes an application account.
                if (BiometricManager.from(this).canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                    clearError();
                    updateBiometricStatus();
                    Toast.makeText(this, R.string.auth_biometric_registered, Toast.LENGTH_LONG).show();
                } else {
                    showError(R.string.auth_biometric_not_enrolled);
                }
            });

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull android.content.Context context, Void input) {
                    return super.createIntent(context, input)
                            .putExtra("android.intent.extras.CAMERA_FACING", 1)
                            .putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
                }
            }, bitmap -> {
                if (bitmap != null) showCameraPreview(bitmap);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sessionManager = new SessionManager(this);
        biometricStore = new AndroidDeviceStore(this);
        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnFingerprint = findViewById(R.id.btn_fingerprint);
        btnCamera = findViewById(R.id.btn_camera);
        rememberBiometric = findViewById(R.id.cb_biometric);
        View biometricRow = findViewById(R.id.biometric_toggle_row);
        if (biometricRow != null) {
            biometricRow.setOnClickListener(v -> {
                if (rememberBiometric.isEnabled()) rememberBiometric.toggle();
            });
        }
        adaptBiometricButtonsLayout();
        errorView = findViewById(R.id.tv_auth_error);
        biometricHint = findViewById(R.id.tv_biometric_hint);
        loadingView = findViewById(R.id.tv_auth_loading);
        loadingRow = findViewById(R.id.auth_loading_row);
        View root = findViewById(R.id.login_root);
        View scrollView = findViewById(R.id.login_scroll);
        splashController = new SigdSplashController(root);
        boolean launchSplash = savedInstanceState == null
                && Intent.ACTION_MAIN.equals(getIntent().getAction())
                && !getIntent().getBooleanExtra("forget_android_access", false);
        if (!splashController.playIfEligible(launchSplash)) {
            View card = findViewById(R.id.card_login);
            card.setAlpha(0f);
            card.setTranslationY(18 * getResources().getDisplayMetrics().density);
            card.animate().alpha(1f).translationY(0f).setDuration(260).start();
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            if (scrollView != null) {
                scrollView.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            } else {
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            }
            if (splashController != null) {
                splashController.applyBottomInset(bars.bottom);
            }
            return insets;
        });
        new WindowInsetsControllerCompat(getWindow(), root).setAppearanceLightStatusBars(false);
        new WindowInsetsControllerCompat(getWindow(), root).setAppearanceLightNavigationBars(false);
        ViewCompat.requestApplyInsets(root);

        androidBiometrics = new AndroidBiometricCoordinator(this, new AndroidBiometricCoordinator.Listener() {
            public void loading(boolean value) { setLoading(value); }
            public void error(int message) { updateBiometricStatus(); showError(message); }
            public void rateLimited(String retryAfter) { updateBiometricStatus(); showRateLimit(LoginMethod.FINGERPRINT,retryAfter); }
            public void linked() {
                updateBiometricStatus();
                new MaterialAlertDialogBuilder(LoginActivity.this)
                        .setTitle(R.string.auth_biometric_saved_title)
                        .setMessage(getString(R.string.auth_biometric_saved_message, biometricStore.username()))
                        .setCancelable(false)
                        .setPositiveButton(R.string.auth_understood, (dialog, which) -> goToMain()).show();
            }
            public void alreadyLinked() {
                updateBiometricStatus();
                goToMain();
            }
            public void forgotten() {
                rememberBiometric.setChecked(false);
                getIntent().removeExtra("forget_android_access");
                updateBiometricStatus(); clearError();
                Toast.makeText(LoginActivity.this, R.string.auth_android_unlinked, Toast.LENGTH_LONG).show();
            }
            public void authenticated(LoginResponseData profile, LoginResponseData.AuthTokens tokens) {
                saveSession(profile, tokens); goToMain();
            }
        }, savedInstanceState, getIntent().getBooleanExtra("forget_android_access", false));
        if (biometricStore.hasCredential()) etUsername.setText(biometricStore.username());
        updateBiometricStatus();
        etUsername.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (tilUsername != null && tilUsername.getError() != null) {
                    tilUsername.setError(null);
                    tilUsername.setErrorEnabled(false);
                }
                if(biometricStore.hasCredential())updateBiometricStatus();
            }
            public void afterTextChanged(Editable s) {}
        });
        etPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                if (tilPassword != null && tilPassword.getError() != null) {
                    tilPassword.setError(null);
                    tilPassword.setErrorEnabled(false);
                }
            }
            public void afterTextChanged(Editable s) {}
        });
        btnLogin.setOnClickListener(view -> performLogin());
        btnFingerprint.setOnClickListener(view -> unlockWithBiometrics());
        faceDeviceStore=new pe.net.ws.kpiweb.core.session.FaceDeviceStore(this);
        if(biometricStore.hasCredential() && faceDeviceStore.matches(biometricStore))
            root.post(prepareFaceModels);
        btnCamera.setOnClickListener(view -> {faceTapStarted=FaceTiming.now();checkAndStartFaceRecognition();});
        findViewById(R.id.btn_help).setOnClickListener(view -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.auth_help_title)
                .setMessage(R.string.auth_help_message)
                .setPositiveButton(R.string.auth_understood, null).show());
        etPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin();
                return true;
            }
            return false;
        });
        rememberBiometric.setOnCheckedChangeListener((button, checked) -> {
            if(updatingBiometricSelection)return;
            if (checked && !biometricsAvailable()) button.setChecked(false);
        });
        if (savedInstanceState == null && getIntent().getBooleanExtra("forget_android_access", false)) {
            btnFingerprint.post(() -> { if (!inactive()) unlockWithBiometrics(); });
        }
    }

    private void adaptBiometricButtonsLayout() {
        LinearLayout actions = findViewById(R.id.biometric_actions);
        if (actions == null) return;
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        if (screenWidthDp > 0 && screenWidthDp < 320) {
            actions.setOrientation(LinearLayout.VERTICAL);
            actions.setWeightSum(0f);
            int gap = Math.round(10 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnFingerprint.setLayoutParams(fp);
            btnFingerprint.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.topMargin = gap;
            btnCamera.setLayoutParams(cp);
            btnCamera.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        }
    }

    private void performLogin() {
        if (passwordRateLimited) return;
        if (busy) return;
        clearError();
        if (tilUsername != null) { tilUsername.setError(null); tilUsername.setErrorEnabled(false); }
        if (tilPassword != null) { tilPassword.setError(null); tilPassword.setErrorEnabled(false); }
        String username = textOf(etUsername).trim();
        String password = textOf(etPassword);
        if (username.isEmpty()) {
            if (tilUsername != null) {
                tilUsername.setErrorEnabled(true);
                tilUsername.setError(getString(R.string.auth_enter_username));
            } else {
                etUsername.setError(getString(R.string.auth_enter_username));
            }
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            if (tilPassword != null) {
                tilPassword.setErrorEnabled(true);
                tilPassword.setError(getString(R.string.auth_enter_password));
            } else {
                etPassword.setError(getString(R.string.auth_enter_password));
            }
            etPassword.requestFocus();
            return;
        }
        loadingView.setText(R.string.auth_loading_login);
        setLoading(true);
        long requestStarted=FaceTiming.now();
        Call<ApiResponse<LoginResponseData>> call = ApiClient.getService().login(new LoginRequest(username, password));
        pendingCall = call;
        call.enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponseData>> source,
                                   @NonNull Response<ApiResponse<LoginResponseData>> response) {
                if (inactive()) return;
                FaceTiming.log(LoginActivity.this,"password_server",requestStarted);
                LoginResponseData data = response.body() != null ? response.body().getData() : null;
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || data == null || data.getUser() == null || !validTokens(data.getAuth())) {
                    setLoading(false);
                    showResponseError(response);
                    return;
                }
                saveSession(data, data.getAuth());
                etPassword.setText(null);
                // A password login may use another account, but never rebind biometrics implicitly.
                if (biometricStore.hasCredential()
                        && !biometricStore.username().equals(data.getUser().getUsername())) {
                    goToMain();
                    return;
                }
                boolean linked=BiometricLoginPolicy.linkedFor(biometricStore,data.getUser().getUsername(),
                        !biometricKeyUnavailable,systemBiometricsReady());
                if (linked) {
                    goToMain();
                } else if (BiometricLoginPolicy.needsRegistration(biometricStore,data.getUser().getUsername(),
                        rememberBiometric.isChecked(),linked)) {
                    if (!biometricsAvailable()) { setLoading(false); return; }
                    androidBiometrics.register(data.getUser().getUsername());
                } else {
                    goToMain();
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponseData>> source, @NonNull Throwable error) {
                if (inactive() || source.isCanceled()) return;
                FaceTiming.log(LoginActivity.this,"password_server",requestStarted);
                setLoading(false);
                showError(R.string.auth_connection_error);
            }
        });
    }

    private void unlockWithBiometrics() {
        if (fingerprintRateLimited) return;
        if (busy) return;
        clearError();
        if (biometricKeyUnavailable) {showError(R.string.auth_biometric_key_unavailable);return;}
        if (!biometricsAvailable()) return;
        if (!biometricStore.hasCredential()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.auth_biometric_link_title)
                    .setMessage(R.string.auth_biometric_link_message)
                    .setNegativeButton(R.string.auth_cancel, null)
                    .setPositiveButton(R.string.auth_biometric_link, (dialog, which) -> {
                        rememberBiometric.setChecked(true);
                        biometricHint.setText(R.string.auth_biometric_link_next);
                        performLogin();
                    }).show();
            return;
        }
        if (biometricStore.isFingerprintDisabled()) {
            showError(R.string.auth_fingerprint_disabled); return;
        }
        String entered = textOf(etUsername).trim();
        if (!entered.isEmpty() && !entered.equals(biometricStore.username())) {
            showError(R.string.auth_biometric_other_account); return;
        }
        androidBiometrics.unlock();
    }
    private void saveSession(LoginResponseData data, LoginResponseData.AuthTokens tokens) {
        long started=FaceTiming.now();
        LoginResponseData.WorkerInfo worker = data.getWorker();
        String fullName = worker != null && worker.getNombreCompleto() != null
                ? worker.getNombreCompleto() : data.getUser().getUsername();
        sessionManager.saveSession(tokens.getAccessToken(), tokens.getRefreshToken(),
                data.getUser().getIdUsuario(), data.getUser().getUsername(), data.getUser().getIdRol(),
                worker != null ? worker.getIdTrabajador() : 0, fullName);
        FaceTiming.log(this,"session_save",started);
    }

    private boolean biometricsAvailable() {
        int status = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        if (status == BiometricManager.BIOMETRIC_SUCCESS) return true;
        if (status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            showError(R.string.auth_biometric_not_enrolled);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.auth_biometric_register_title)
                    .setMessage(R.string.auth_biometric_register_message)
                    .setNegativeButton(R.string.auth_cancel, null)
                    .setPositiveButton(R.string.auth_biometric_register, (dialog, which) -> openBiometricSettings())
                    .show();
        }
        else if (status == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) showError(R.string.auth_biometric_no_hardware);
        else showError(R.string.auth_biometric_unavailable);
        return false;
    }

    private boolean systemBiometricsReady() {
        return BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG)==BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void updateBiometricStatus() {
        if(!biometricStore.hasCredential())biometricKeyUnavailable=false;
        String entered=textOf(etUsername).trim();
        boolean same=BiometricLoginPolicy.sameAccount(biometricStore,entered);
        boolean linked=BiometricLoginPolicy.linkedFor(biometricStore,entered,
                !biometricKeyUnavailable,systemBiometricsReady());
        updatingBiometricSelection=true;
        rememberBiometric.setChecked(linked);
        updatingBiometricSelection=false;
        rememberBiometric.setEnabled(!busy && !biometricKeyUnavailable && !biometricStore.isFingerprintDisabled()
                && !biometricStore.isEnabled() && (!biometricStore.hasCredential() || same));
        if(biometricKeyUnavailable && biometricStore.hasCredential()) {
            biometricHint.setText(R.string.auth_biometric_key_unavailable);
        } else if(biometricStore.hasCredential() && !same) {
            biometricHint.setText(R.string.auth_biometric_other_account);
        } else if (linked) {
            biometricHint.setText(R.string.auth_biometric_username);
        } else if (biometricStore.isFingerprintDisabled()) {
            biometricHint.setText(R.string.auth_fingerprint_disabled);
        } else if (biometricStore.hasCredential()) {
            biometricHint.setText(R.string.auth_android_pending);
        } else if (systemBiometricsReady()) {
            biometricHint.setText(R.string.auth_biometric_ready_unlinked);
        } else {
            biometricHint.setText(R.string.auth_biometric_opt_in);
        }
    }

    private void openBiometricSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                    .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            intent = new Intent(Settings.ACTION_FINGERPRINT_ENROLL);
        } else {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        }
        try {
            biometricSettingsLauncher.launch(intent);
        } catch (ActivityNotFoundException | SecurityException error) {
            try {
                biometricSettingsLauncher.launch(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                showError(R.string.auth_biometric_settings_missing);
            }
        }
    }

    private void openCamera() {
        try {
            cameraLauncher.launch(null);
        } catch (ActivityNotFoundException | SecurityException error) {
            showError(R.string.auth_camera_missing);
        }
    }

    private void showCameraPreview(Bitmap bitmap) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, 0);
        ImageView image = new ImageView(this);
        image.setContentDescription(getString(R.string.auth_camera_preview_description));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageBitmap(bitmap);
        content.addView(image, new LinearLayout.LayoutParams(-1, Math.round(220 * getResources().getDisplayMetrics().density)));
        TextView notice = new TextView(this);
        notice.setText(R.string.auth_camera_preview);
        notice.setPadding(0, padding / 2, 0, 0);
        content.addView(notice);
        new MaterialAlertDialogBuilder(this).setTitle(R.string.auth_camera_preview_title)
                .setView(content).setPositiveButton(R.string.auth_understood, null)
                .setOnDismissListener(dialog -> { image.setImageDrawable(null); bitmap.recycle(); })
                .show();
    }

    private void showResponseError(Response<?> response) {
        String code = "";
        if (response.errorBody() != null) {
            try {
                ApiResponse<?> error = new com.google.gson.Gson().fromJson(response.errorBody().string(), ApiResponse.class);
                if (error != null && error.getErrorCode() != null) code = error.getErrorCode();
            } catch (Exception ignored) { }
        }
        if (response.code() == 429) showRateLimit(LoginMethod.PASSWORD,response.headers().get("Retry-After"));
        else if (code.equals("ACCOUNT_INACTIVE")) showError(R.string.auth_inactive);
        else if (code.equals("DEVICE_NOT_AUTHORIZED")) showError(R.string.auth_device_denied);
        else if (response.code() == 401) showError(R.string.auth_invalid_credentials);
        else showError(R.string.auth_server_error);
    }

    private void setLoading(boolean loading) {
        busy = loading;
        loadingRow.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading && !passwordRateLimited);
        btnFingerprint.setEnabled(!loading && !fingerprintRateLimited);
        btnCamera.setEnabled(!loading && !faceRateLimited);
        if(loading)rememberBiometric.setEnabled(false);
        else rememberBiometric.setEnabled(!biometricKeyUnavailable && !biometricStore.isFingerprintDisabled() && !biometricStore.isEnabled()
                && (!biometricStore.hasCredential()
                    || BiometricLoginPolicy.sameAccount(biometricStore,textOf(etUsername).trim())));
        etUsername.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void clearError() { errorView.setVisibility(View.GONE); }
    private void showError(int message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
        errorView.setAlpha(0f);
        errorView.animate().alpha(1f).setDuration(180).start();
    }
    private void showRateLimit(LoginMethod method,String retryAfter) {
        long delay=RetryAfter.delayMillis(retryAfter,System.currentTimeMillis());
        // Without a server deadline, Android must not add an indefinite lock of its own.
        if(delay>0){
            if(method==LoginMethod.PASSWORD)passwordRateLimited=true;
            else if(method==LoginMethod.FINGERPRINT)fingerprintRateLimited=true;
            else faceRateLimited=true;
        }
        String message=getString(R.string.auth_rate_limit)+" " + (delay>0
                ?getString(R.string.auth_rate_limit_retry_after,delay/1000+(delay%1000==0?0:1))
                :getString(R.string.auth_rate_limit_unknown));
        errorView.setText(message);errorView.setVisibility(View.VISIBLE);
        setLoading(busy);
        if(delay>0)rateLimitHandler.postDelayed(()->{
            if(inactive())return;
            if(method==LoginMethod.PASSWORD)passwordRateLimited=false;
            else if(method==LoginMethod.FINGERPRINT)fingerprintRateLimited=false;
            else faceRateLimited=false;
            setLoading(busy);
            if(message.contentEquals(errorView.getText()))clearError();
        },delay);
    }
    private boolean inactive() { return isFinishing() || isDestroyed(); }
    private static String textOf(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString(); }
    private static boolean validTokens(LoginResponseData.AuthTokens tokens) {
        return tokens != null && tokens.getAccessToken() != null && tokens.getAccessToken().matches("[a-fA-F0-9]{64}")
                && tokens.getRefreshToken() != null && tokens.getRefreshToken().matches("[a-fA-F0-9]{64}");
    }

    private void goToMain() {
        goToMain(0);
    }
    private void goToMain(long faceStartedNanos) {
        long started=FaceTiming.now();
        AccountNavigation.afterAuthentication(this,faceStartedNanos);
        FaceTiming.log(this,"navigation_dispatch",started);
    }

    private FaceCameraManager faceCameraManager;
    private LocalFaceExtractor localFaceExtractor;
    private android.webkit.WebView localFaceExtractorView;
    private android.webkit.WebView preparedExtractorView;
    private LocalFaceExtractor preparedExtractor;
    private LocalPadAnalyzer preparedPad;
    private final Runnable prepareFaceModels=this::prepareFaceModels;

    private void prepareFaceModels() {
        if(inactive() || biometricStore==null || faceDeviceStore==null
                || !biometricStore.hasCredential() || !faceDeviceStore.matches(biometricStore)
                || localFaceExtractor!=null)return;
        if(preparedExtractor!=null){
            if(!preparedExtractor.hasFailed())return;
            releasePreparedExtractor();
        }
        FrameLayout root=findViewById(R.id.login_root);
        android.webkit.WebView view=null;
        try {
            FaceExtractorPool.Entry pooled=FaceExtractorPool.take(this);
            view=pooled!=null?pooled.view:new android.webkit.WebView(new android.content.MutableContextWrapper(this));
            view.setAlpha(.01f);
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            root.addView(view,new FrameLayout.LayoutParams(dp(1),dp(1)));
            preparedExtractor=pooled!=null?pooled.extractor:new LocalFaceExtractor(this,view);
            preparedExtractorView=view;
        } catch(RuntimeException unavailable) {
            if(view!=null){root.removeView(view);view.destroy();}
        }
        if(preparedPad==null){preparedPad=new LocalPadAnalyzer(this);preparedPad.prepare();}
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}

    private void releasePreparedExtractor() {
        if(preparedExtractor!=null)FaceExtractorPool.offer(this,preparedExtractorView,preparedExtractor);
        else if(preparedExtractorView!=null)preparedExtractorView.destroy();
        preparedExtractor=null;preparedExtractorView=null;
    }

    private void releasePreparedModels() {
        releasePreparedExtractor();
        if(preparedPad!=null){preparedPad.close();preparedPad=null;}
    }

    private void checkAndStartFaceRecognition() {
        if (faceRateLimited) return;
        if (busy) return;
        clearError();
        String username = textOf(etUsername).trim();
        if (username.isEmpty() && (!biometricStore.hasCredential() || biometricStore.username().isEmpty())) {
            etUsername.setError(getString(R.string.auth_enter_username));
            etUsername.requestFocus();
            return;
        }

        if(!username.isEmpty()&&!username.equals(biometricStore.username())){showError(R.string.auth_android_phone_already_linked);return;}
        if(!biometricStore.hasCredential()||!faceDeviceStore.matches(biometricStore)){showError(R.string.auth_face_key_needed);return;}
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        final int run=++facePreflight;loadingView.setText(R.string.auth_loading_face);setLoading(true);
        long preflightStarted=FaceTiming.now();
        faceCrypto.execute(()->{
            try{faceDeviceStore.checkDirectAccess();runOnUiThread(()->{if(run==facePreflight&&!inactive()){FaceTiming.log(this,"key_preflight",preflightStarted);setLoading(false);startFaceRecognitionFlow();}});}
            catch(Exception problem){runOnUiThread(()->{
                if(run!=facePreflight||inactive())return;setLoading(false);
                showError(R.string.auth_face_pilot_key_required);
            });}
        });
    }

    private void startFaceRecognitionFlow() {
        if (inactive()) return;
        String username = textOf(etUsername).trim();
        if (username.isEmpty() && biometricStore.hasCredential()) {
            username = biometricStore.username();
        }

        View dialogView;
        try {dialogView=getLayoutInflater().inflate(R.layout.dialog_face_scan,null);}
        catch(RuntimeException unavailable) {showError(R.string.auth_face_extractor_unavailable);return;}
        TextureView cameraTexture = dialogView.findViewById(R.id.face_camera_texture);
        faceOverlayView = dialogView.findViewById(R.id.face_overlay_view);
        faceOverlayView.setMinimalGuidance(true);
        tvFaceStatus = dialogView.findViewById(R.id.tv_face_status);
        tvFacePrompt = dialogView.findViewById(R.id.tv_face_liveness_prompt);
        pbFaceProgress = dialogView.findViewById(R.id.pb_face_progress);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_face_cancel);
        MaterialButton btnCapture = dialogView.findViewById(R.id.btn_face_capture);

        android.webkit.WebView extractorView=dialogView.findViewById(R.id.face_extractor_webview);
        try {
            if(preparedExtractor==null){
                FaceExtractorPool.Entry pooled=FaceExtractorPool.take(this);
                if(pooled!=null){preparedExtractor=pooled.extractor;preparedExtractorView=pooled.view;}
            }
            if(preparedExtractor!=null && preparedExtractorView!=null && !preparedExtractor.hasFailed()){
                ViewGroup former=(ViewGroup)preparedExtractorView.getParent();
                if(former!=null)former.removeView(preparedExtractorView);
                ViewGroup destination=(ViewGroup)extractorView.getParent();
                int position=destination.indexOfChild(extractorView);
                destination.removeView(extractorView);
                extractorView.destroy();
                destination.addView(preparedExtractorView,position,new FrameLayout.LayoutParams(dp(1),dp(1)));
                extractorView=preparedExtractorView;
                localFaceExtractor=preparedExtractor;
                preparedExtractor=null;preparedExtractorView=null;
            } else {
                releasePreparedExtractor();
                ViewGroup destination=(ViewGroup)extractorView.getParent();
                int position=destination.indexOfChild(extractorView);
                ViewGroup.LayoutParams layout=extractorView.getLayoutParams();
                destination.removeView(extractorView);
                extractorView.destroy();
                extractorView=new android.webkit.WebView(new android.content.MutableContextWrapper(this));
                extractorView.setAlpha(.01f);
                extractorView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                destination.addView(extractorView,position,layout);
                localFaceExtractor=new LocalFaceExtractor(this,extractorView);
            }
            localFaceExtractorView=extractorView;
        }
        catch(RuntimeException unavailable) {
            releasePreparedExtractor();showError(R.string.auth_face_extractor_unavailable);return;
        }

        if (cameraTexture != null) {
            faceCameraManager = new FaceCameraManager(this, cameraTexture, new FaceCameraManager.QualityListener() {
                @Override
                public void onFaceCountChanged(int faceCount) {
                    if (faceOverlayView != null) {
                        faceOverlayView.setScanning(faceCount > 0);
                    }
                }

                @Override
                public void onIlluminationChanged(boolean adequate) {}
                @Override public void onLandmarks(float[] points) {
                    if(faceOverlayView!=null)faceOverlayView.setLandmarks(points);
                }
                @Override public void onPadResult(double real,double spoof,double millis){
                    if(tvFacePrompt!=null)tvFacePrompt.setText(R.string.auth_face_pad_local_checking);
                }
                @Override public void onPreviewReady() {
                    if(pbFaceProgress!=null)pbFaceProgress.setVisibility(View.GONE);
                    if(tvFaceStatus!=null)tvFaceStatus.setText(R.string.auth_face_loading_models);
                }
                @Override public void onExtractorReady() {
                    if(tvFaceStatus!=null)tvFaceStatus.setText(R.string.auth_face_detecting);
                }
                @Override public void onActionChanged(String action) {
                    if(tvFaceStatus!=null)tvFaceStatus.setText("CENTER".equals(action)?R.string.auth_face_position_guide:R.string.auth_face_follow_guide);
                    if(tvFacePrompt!=null)tvFacePrompt.setText("BLINK".equals(action)?R.string.auth_face_liveness_blink:
                        "REPOSITION".equals(action)?R.string.auth_face_reposition:
                        "OPEN_EYES".equals(action)?R.string.auth_face_open_eyes:
                        "TURN_LEFT".equals(action)?R.string.auth_face_turn_left:
                        "TURN_RIGHT".equals(action)?R.string.auth_face_turn_right:
                        "RETURN_CENTER".equals(action)?R.string.auth_face_return_center:R.string.auth_face_position_guide);
                }
            });
            faceCameraManager.setLoginAttemptStarted(faceTapStarted);
            faceCameraManager.setExtractor(localFaceExtractor);
            faceCameraManager.enableLocalPad(preparedPad);
            preparedPad=null;
        }

        faceScanDialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .setOnCancelListener(d -> {
                    if (faceCoordinator != null) faceCoordinator.cancel();
                })
                .setOnDismissListener(d -> {
                    if(faceCoordinator!=null)faceCoordinator.cancel();
                    if(localFaceExtractor!=null) {
                        FaceExtractorPool.offer(this,localFaceExtractorView,localFaceExtractor);
                        localFaceExtractor=null;localFaceExtractorView=null;
                    }
                    if (faceCameraManager != null) {
                        faceCameraManager.closeCamera();
                        faceCameraManager = null;
                    }
                })
                .create();

        if (faceScanDialog.getWindow() != null) {
            faceScanDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> {
            if (faceCoordinator != null) faceCoordinator.cancel();
            faceScanDialog.dismiss();
        });

        if (btnCapture != null) {
            btnCapture.setVisibility(View.GONE);
            btnCapture.setText(R.string.auth_face_retry);
            final String targetUsername=username;
            btnCapture.setOnClickListener(v -> {
                btnCapture.setVisibility(View.GONE);
                faceScanDialog.dismiss();faceTapStarted=FaceTiming.now();checkAndStartFaceRecognition();
            });
            faceCameraManager.setExtractor(localFaceExtractor);
        }

        faceScanDialog.show();
        FaceTiming.log(this,"tap_to_dialog",faceTapStarted);
        // Open the camera and load both local models while the challenge is in flight.
        faceCameraManager.startPreview();
        tvFacePrompt.setText(R.string.auth_face_identity_notice);
        faceCoordinator=new FaceIdentityCoordinator(ApiClient.getFaceDeviceService(),faceDeviceStore.id(),biometricStore.uid(),biometricStore.username(),faceDeviceStore::sign,
            new FaceIdentityCoordinator.Capture(){
                public void start(java.util.function.Consumer<String> payload,java.util.function.Consumer<Integer> error){
                    if(faceCameraManager==null){error.accept(R.string.auth_face_extractor_unavailable);return;}
                    faceCameraManager.startPreview();faceCameraManager.captureForLogin(new FaceCameraManager.LoginCallback(){
                        public void onDescriptor(String descriptor){try{payload.accept(faceCameraManager.loginPayload(descriptor));}catch(Exception invalid){error.accept(R.string.auth_face_extractor_unavailable);}}
                        public void onError(FaceAuthCoordinator.CaptureResult.Status status){error.accept(status==FaceAuthCoordinator.CaptureResult.Status.MULTIPLE_FACES?R.string.auth_face_multiple_faces:
                            status==FaceAuthCoordinator.CaptureResult.Status.SPOOF_DETECTED?R.string.auth_face_pad_local_rejected:
                            status==FaceAuthCoordinator.CaptureResult.Status.LOW_QUALITY?R.string.auth_face_low_quality:R.string.auth_face_extractor_unavailable);}
                    });
                }
                public void stop(){if(faceCameraManager!=null)faceCameraManager.stopPreview();}
            },new FaceIdentityCoordinator.Listener(){
                public void timing(String stage,long started){FaceTiming.log(LoginActivity.this,stage,started);}
                public void loading(boolean value){setLoading(value);if(pbFaceProgress!=null)pbFaceProgress.setVisibility(value?View.VISIBLE:View.GONE);}
                public void status(int resource){tvFaceStatus.setText(resource);}
                public void matched(String account){tvFaceStatus.setText(getString(R.string.auth_face_matched_blocked,account));tvFacePrompt.setText(R.string.auth_face_identity_notice);btnCancel.setText(R.string.auth_face_close);btnCapture.setVisibility(View.GONE);}
                public void authenticated(LoginResponseData profile,LoginResponseData.AuthTokens tokens){
                    if(inactive())return;saveSession(profile,tokens);
                    if(faceScanDialog!=null)faceScanDialog.dismiss();goToMain(faceTapStarted);faceTapStarted=0;
                }
                public void error(int resource){
                    if(resource==R.string.auth_rate_limit){
                        if(faceScanDialog!=null)faceScanDialog.dismiss();
                        showRateLimit(LoginMethod.FACE,null);
                        return;
                    }
                    tvFaceStatus.setText(resource);tvFacePrompt.setText(R.string.auth_face_identity_notice);
                    btnCapture.setVisibility(View.VISIBLE);
                }
                public void rateLimited(String retryAfter){
                    if(faceScanDialog!=null)faceScanDialog.dismiss();
                    showRateLimit(LoginMethod.FACE,retryAfter);
                }
            },faceCrypto,ContextCompat.getMainExecutor(this));
        faceCoordinator.start();
    }

    @Override protected void onResume() {
        super.onResume();
        if(biometricStore!=null && faceDeviceStore!=null && biometricStore.hasCredential()
                && faceDeviceStore.matches(biometricStore))
            findViewById(R.id.login_root).post(prepareFaceModels);
        if(biometricStore==null || !biometricStore.isEnabled())return;
        final int check=++biometricKeyCheckGeneration;
        if(!systemBiometricsReady()){
            biometricKeyUnavailable=true;
            updateBiometricStatus();
            return;
        }
        biometricKeyChecks.execute(()->{
            boolean usable;
            try{biometricStore.signingOperation();usable=true;}
            catch(GeneralSecurityException | RuntimeException invalid){usable=false;}
            final boolean keyUsable=usable;
            runOnUiThread(()->{
                if(check!=biometricKeyCheckGeneration || inactive() || !biometricStore.isEnabled())return;
                biometricKeyUnavailable=!keyUsable;
                updateBiometricStatus();
            });
        });
    }

    @Override protected void onStop() {
        ++biometricKeyCheckGeneration;
        findViewById(R.id.login_root).removeCallbacks(prepareFaceModels);
        ++facePreflight;
        if(faceCoordinator!=null)faceCoordinator.cancel();
        if(faceScanDialog!=null && faceScanDialog.isShowing())faceScanDialog.dismiss();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        androidBiometrics.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (splashController != null) splashController.cancel();
        rateLimitHandler.removeCallbacksAndMessages(null);
        if (pendingCall != null) pendingCall.cancel();
        if (androidBiometrics != null) androidBiometrics.destroy();
        if (faceCoordinator != null) faceCoordinator.cancel();
        faceCrypto.shutdown();
        biometricKeyChecks.shutdown();
        releasePreparedModels();
        if (faceCameraManager != null) {
            faceCameraManager.closeCamera();
            faceCameraManager = null;
        }
        if (faceScanDialog != null && faceScanDialog.isShowing()) faceScanDialog.dismiss();
        if(localFaceExtractor!=null) {
            FaceExtractorPool.offer(this,localFaceExtractorView,localFaceExtractor);
            localFaceExtractor=null;localFaceExtractorView=null;
        }
        super.onDestroy();
    }
}
