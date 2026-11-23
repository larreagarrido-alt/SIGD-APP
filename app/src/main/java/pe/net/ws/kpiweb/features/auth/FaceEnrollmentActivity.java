package pe.net.ws.kpiweb.features.auth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.*;
import pe.net.ws.kpiweb.core.session.*;
import pe.net.ws.kpiweb.features.auth.models.*;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Authenticated enrollment only. Never issues or saves a facial login session. */
public final class FaceEnrollmentActivity extends AppCompatActivity {
    private static final int COLOR_BG_MAIN = Color.parseColor("#11091F");
    private static final int COLOR_LAVENDER = Color.parseColor("#D8B4FE");
    private static final int COLOR_MINT = Color.parseColor("#57D6A0");
    private static final int COLOR_CORAL = Color.parseColor("#FF819D");

    private SessionManager session;
    private AndroidDeviceStore store;
    private FaceEnrollmentApi api;
    private TextView status;
    private TextView statusTitle;
    private TextView statePill;
    private ImageView statusIcon;
    private MaterialButton start;
    private FaceOverlayView overlay;
    private FaceCameraManager camera;
    private LocalFaceExtractor extractor;
    private Call<?> pending;
    private BiometricPrompt prompt;
    private boolean active,needsStatus=true;
    private String profile;
    private FaceDeviceStore faceStore;
    private MaterialButton prepareKey;
    private MaterialButton disableFace;
    private FaceRegistrationAttempt registrationAttempt;
    private final java.util.concurrent.ExecutorService keyWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> cameraPermission=registerForActivityResult(
        new ActivityResultContracts.RequestPermission(),granted->{
            if(!active)return;
            if(granted)capture();else error(R.string.auth_face_permission_denied);
        });

    @Override protected void onCreate(Bundle state){
        CollaboratorThemeHelper.applySavedTheme(this);
        setTheme(R.style.Theme_KPIWeb_SecurityDark);
        super.onCreate(state);session=new SessionManager(this);store=new AndroidDeviceStore(this);
        if(!session.isLoggedIn()){startActivity(new Intent(this,LoginActivity.class));finish();return;}
        try {setContentView(R.layout.activity_face_enrollment);}
        catch(RuntimeException unavailable){finish();return;}
        setupDarkSystemBars();
        status=findViewById(R.id.enroll_status);
        statusTitle=findViewById(R.id.enroll_status_title);
        statePill=findViewById(R.id.enroll_state_pill);
        statusIcon=findViewById(R.id.enroll_status_icon);
        start=findViewById(R.id.enroll_start);
        overlay=findViewById(R.id.enroll_overlay);
        styleStaticBadges();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.face_enroll_root),(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()|androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
        String accountLabel = session.getFullName() != null && !session.getFullName().trim().isEmpty()
                ? session.getFullName().trim() : session.getUsername();
        ((TextView)findViewById(R.id.face_enroll_account)).setText(getString(R.string.auth_face_enroll_account,accountLabel));
        api=ApiClient.getFaceEnrollmentService();
        faceStore=new FaceDeviceStore(this);prepareKey=findViewById(R.id.face_prepare_key);
        disableFace=findViewById(R.id.face_disable);
        disableFace.setOnClickListener(v->new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KPIWeb_SecurityDarkDialog)
            .setTitle(R.string.auth_face_disable).setMessage(R.string.auth_face_disable_explanation)
            .setNegativeButton(R.string.auth_cancel,null).setPositiveButton(R.string.auth_face_disable,(d,w)->disableFaceAccess()).show());
        registrationAttempt=new FaceRegistrationAttempt(getSharedPreferences("wsp_face_registration_attempt",MODE_PRIVATE));
        prepareKey.setOnClickListener(v->{
            if(faceStore.matches(store))new com.google.android.material.dialog.MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_KPIWeb_SecurityDarkDialog)
                .setTitle(R.string.auth_face_rotate_key).setMessage(R.string.auth_face_rotate_explanation)
                .setNegativeButton(R.string.auth_cancel,null).setPositiveButton(R.string.auth_face_rotate_key,(d,w)->prepareFaceKey(true)).show();
            else prepareFaceKey(false);
        });
        findViewById(R.id.enroll_back).setOnClickListener(v->finish());
        View btnBackTop = findViewById(R.id.enroll_btn_back_top);
        if (btnBackTop != null) btnBackTop.setOnClickListener(v -> finish());
        start.setOnClickListener(v->{
            if(needsStatus){loadStatus();return;}
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)cameraPermission.launch(Manifest.permission.CAMERA);
            else capture();
        });
    }

    private void setupDarkSystemBars() {
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void styleStaticBadges() {
        applyBadge(findViewById(R.id.enroll_hero_badge), Color.parseColor("#338B30E0"), 14);
        applyBadge(findViewById(R.id.enroll_req_badge_phone), Color.parseColor("#2E8B30E0"), 10);
        applyBadge(findViewById(R.id.enroll_req_badge_fp), Color.parseColor("#2457D6A0"), 10);
        applyBadge(findViewById(R.id.enroll_req_badge_lock), Color.parseColor("#2E8B30E0"), 10);
        updateVisualState("Verificando", "Validación facial", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.security_ic_shield_check);
    }

    private void applyBadge(View view, int color, int radiusDp) {
        if (view == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(Math.round(radiusDp * getResources().getDisplayMetrics().density));
        view.setBackground(bg);
    }

    private void updateVisualState(String pillLabel, String titleText, int accentColor, int pillBgColor, int iconRes) {
        if (statePill != null) {
            statePill.setText(pillLabel);
            statePill.setTextColor(accentColor);
            applyBadge(statePill, pillBgColor, 99);
        }
        if (statusTitle != null) {
            statusTitle.setText(titleText);
        }
        if (statusIcon != null) {
            statusIcon.setImageResource(iconRes);
            statusIcon.setColorFilter(accentColor);
        }
    }

    private void promotePrepareKeyAsPrimary(boolean primary) {
        if (prepareKey == null) return;
        if (primary) {
            prepareKey.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.face_btn_primary_bg));
            ColorStateList textColors = ContextCompat.getColorStateList(this, R.color.face_btn_primary_text);
            prepareKey.setTextColor(textColors);
            prepareKey.setIconTint(textColors);
            prepareKey.setStrokeWidth(0);
        } else {
            prepareKey.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.face_btn_secondary_bg));
            ColorStateList textColors = ContextCompat.getColorStateList(this, R.color.face_btn_secondary_text);
            prepareKey.setTextColor(textColors);
            prepareKey.setIconTint(textColors);
            prepareKey.setStrokeColor(ContextCompat.getColorStateList(this, R.color.face_btn_secondary_stroke));
            prepareKey.setStrokeWidth(Math.round(1.2f * getResources().getDisplayMetrics().density));
        }
    }

    @Override protected void onResume(){super.onResume();active=true;setupDarkSystemBars();if(status!=null)loadStatus();}
    private boolean linked(){return session.isLoggedIn()&&store.hasCredential()&&store.username().equals(session.getUsername());}
    private Map<String,Object> identity(){Map<String,Object> body=new HashMap<>();body.put("id_acceso",store.id());body.put("device_uid",store.uid());return body;}
    private void loadStatus(){
        if(registrationAttempt!=null&&registrationAttempt.unresolved()){showUncertainRegistration();return;}
        if(!linked()){
            status.setText(R.string.auth_face_enroll_link_required);
            start.setEnabled(false);
            prepareKey.setEnabled(false);
            promotePrepareKeyAsPrimary(false);
            updateVisualState("Requiere vínculo", "Configuración previa requerida", COLOR_CORAL, Color.parseColor("#2BFF819D"), R.drawable.security_ic_smartphone);
            return;
        }
        start.setEnabled(false);prepareKey.setEnabled(false);status.setText(R.string.auth_face_enroll_checking);
        updateVisualState("Verificando", "Consultando cuenta", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.security_ic_shield_check);
        send(api.status(session.getBearerToken(),identity()),value->{
            if(value.reference_available&&"ASISTENCIA".equals(value.reference_source)){
                status.setText(R.string.auth_face_attendance_available);start.setVisibility(android.view.View.GONE);
                promotePrepareKeyAsPrimary(true);
                ((TextView)findViewById(R.id.face_enroll_explanation)).setText(R.string.auth_face_attendance_available);
                prepareKey.setText(faceStore.matches(store)?R.string.auth_face_rotate_key:R.string.auth_face_prepare_key);
                prepareKey.setEnabled(value.recent_password);
                if(!value.recent_password){
                    status.setText(R.string.auth_face_enroll_fresh_required);
                    updateVisualState("Verificación", "Confirma tu contraseña", COLOR_CORAL, Color.parseColor("#2BFF819D"), R.drawable.collab_ic_lock);
                } else {
                    updateVisualState("Disponible", "Autorización facial", COLOR_MINT, Color.parseColor("#2457D6A0"), R.drawable.security_ic_scan_face);
                }
                loadFaceAccessStatus();
                return;
            }
            promotePrepareKeyAsPrimary(false);
            if(value.migration_v012_required){error(R.string.auth_face_update_required);return;}
            if(value.registered){
                status.setText(R.string.auth_face_enroll_saved);start.setEnabled(false);
                updateVisualState("Pendiente", "Rostro registrado", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.security_ic_shield_check);
                return;
            }
            if(!value.recent_password){error(R.string.auth_face_enroll_fresh_required);return;}
            needsStatus=false;status.setText(R.string.auth_face_enroll_ready);start.setText(R.string.auth_face_enroll_title);start.setEnabled(true);
            updateVisualState("Listo", "Registro facial disponible", COLOR_MINT, Color.parseColor("#2457D6A0"), R.drawable.security_ic_scan_face);
        });
    }
    private void loadFaceAccessStatus(){
        send(FaceRegistrationClient.getDeviceService().status(session.getBearerToken(),identity()),value->{
            boolean own=value.active&&faceStore.matches(store)&&faceStore.id()==value.id_clave;
            disableFace.setVisibility(own?android.view.View.VISIBLE:android.view.View.GONE);
            disableFace.setEnabled(own&&value.recent_password);
            prepareKey.setText(own?R.string.auth_face_rotate_key:R.string.auth_face_prepare_key);
            prepareKey.setEnabled(value.recent_password);
            if(own){
                status.setText(value.facial_login_enabled?R.string.auth_face_key_ready:R.string.auth_face_global_disabled);
                if (value.facial_login_enabled) {
                    updateVisualState("Habilitado", "Acceso facial activo", COLOR_MINT, Color.parseColor("#2457D6A0"), R.drawable.security_ic_shield_check);
                } else {
                    updateVisualState("Autorizado", "Función pendiente de empresa", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.collab_ic_lock);
                }
            } else {
                status.setText(R.string.auth_face_activation_ready);
                updateVisualState("Disponible", "Activar reconocimiento facial", COLOR_MINT, Color.parseColor("#2457D6A0"), R.drawable.security_ic_scan_face);
            }
            if(!value.recent_password)status.append("\n"+getString(R.string.auth_face_enroll_fresh_required));
        });
    }
    private void disableFaceAccess(){
        if(!active||pending!=null||!linked()||!faceStore.matches(store))return;
        disableFace.setEnabled(false);prepareKey.setEnabled(false);
        Map<String,Object> body=new HashMap<>();body.put("id_clave",faceStore.id());body.put("device_uid",store.uid());
        send(FaceRegistrationClient.getDeviceService().revokeChallenge(session.getBearerToken(),body),challenge->{
            if(challenge.challengeId<=0||challenge.message==null||!challenge.message.startsWith("WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:")){error(R.string.auth_face_enroll_failed);return;}
            try{
                Signature operation=store.signingOperation();
                prompt=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
                        if(!active)return;
                        try{
                            Signature op=result.getCryptoObject()==null?null:result.getCryptoObject().getSignature();
                            if(op==null)throw new java.security.GeneralSecurityException();
                            op.update(challenge.message.getBytes(StandardCharsets.UTF_8));
                            Map<String,Object> proof=new HashMap<>(body);proof.put("challenge_id",challenge.challengeId);proof.put("firma_base64",AndroidDeviceStore.encodeBase64(op.sign()));
                            send(FaceRegistrationClient.getDeviceService().revoke(session.getBearerToken(),proof),revoked->{
                                if(!revoked.revoked){error(R.string.auth_face_enroll_failed);return;}
                                try{
                                    faceStore.clearBinding();
                                    status.setText(R.string.auth_face_disabled);
                                    disableFace.setVisibility(android.view.View.GONE);
                                    prepareKey.setText(R.string.auth_face_prepare_key);
                                    prepareKey.setEnabled(true);
                                    updateVisualState("Desactivado", "Reconocimiento desactivado", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.collab_ic_lock);
                                }
                                catch(Exception failure){error(R.string.auth_face_enroll_failed);}
                            });
                        }catch(Exception failure){error(R.string.auth_face_enroll_failed);}
                    }
                    @Override public void onAuthenticationError(int code,CharSequence message){if(active)loadStatus();}
                });
                prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.auth_face_disable))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText(getString(R.string.auth_cancel)).build(),new BiometricPrompt.CryptoObject(operation));
            }catch(Exception failure){error(R.string.auth_face_enroll_link_required);}
        });
    }
    private void capture(){
        if(!active||!linked())return;
        start.setEnabled(false);profile=null;
        findViewById(R.id.enroll_camera_card).setVisibility(android.view.View.VISIBLE);
        updateVisualState("En captura", "Escaneo facial activo", COLOR_MINT, Color.parseColor("#2457D6A0"), R.drawable.security_ic_scan_face);
        try {
            if(extractor==null)extractor=new LocalFaceExtractor(this,(WebView)findViewById(R.id.enroll_engine));
            if(camera==null)camera=new FaceCameraManager(this,(TextureView)findViewById(R.id.enroll_camera),new FaceCameraManager.QualityListener(){
                public void onFaceCountChanged(int count){overlay.setScanning(count==1);}
                public void onIlluminationChanged(boolean good){}
                public void onLandmarks(float[] points){overlay.setLandmarks(points);}
                public void onEnrollmentProgress(int count){status.setText(getString(R.string.auth_face_enroll_progress,count));}
            });
            if(!camera.isCameraAvailable()){error(R.string.auth_camera_missing);return;}
            camera.setExtractor(extractor);status.setText(getString(R.string.auth_face_enroll_progress,0));camera.startPreview();
            camera.captureForEnrollment(new FaceCameraManager.EnrollmentCallback(){
                public void onSamples(String payload){if(!active)return;camera.stopPreview();profile=payload;authorizeProfile();}
                public void onError(FaceAuthCoordinator.CaptureResult.Status failure){
                    if(!active)return;camera.stopPreview();error(failure==FaceAuthCoordinator.CaptureResult.Status.MULTIPLE_FACES?R.string.auth_face_multiple_faces:
                        failure==FaceAuthCoordinator.CaptureResult.Status.LOW_QUALITY?R.string.auth_face_low_quality:R.string.auth_face_extractor_unavailable);
                }
            });
        }catch(RuntimeException unavailable){error(R.string.auth_face_extractor_unavailable);}
    }
    private void authorizeProfile(){
        if(profile==null||!linked()){error(R.string.auth_face_enroll_link_required);return;}
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(profile.getBytes(StandardCharsets.UTF_8));
            StringBuilder digest=new StringBuilder();for(byte b:hash)digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            Map<String,Object> body=identity();body.put("perfil_sha256",digest.toString());status.setText(R.string.auth_face_enroll_saving);
            updateVisualState("Guardando", "Autorizando perfil", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.security_ic_shield_check);
            send(api.challenge(session.getBearerToken(),body),this::sign);
        }catch(Exception failure){error(R.string.auth_face_enroll_failed);}
    }
    private void sign(AndroidAccessChallenge challenge){
        if(challenge.challengeId<=0||challenge.message==null||!challenge.message.startsWith("WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:")){error(R.string.auth_face_enroll_failed);return;}
        try {
            Signature operation=store.signingOperation();
            prompt=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
                    if(!active||profile==null)return;
                    try {
                        Signature signature=result.getCryptoObject()==null?null:result.getCryptoObject().getSignature();
                        if(signature==null)throw new java.security.GeneralSecurityException();
                        signature.update(challenge.message.getBytes(StandardCharsets.UTF_8));
                        Map<String,Object> body=identity();body.put("challenge_id",challenge.challengeId);
                        body.put("firma_base64",AndroidDeviceStore.encodeBase64(signature.sign()));body.put("perfil_json",profile);
                        send(api.confirm(session.getBearerToken(),body),value->{
                            profile=null;
                            if(!value.registered||!"PENDIENTE_VALIDACION".equals(value.estado)||value.facial_login_enabled){error(R.string.auth_face_enroll_failed);return;}
                            status.setText(R.string.auth_face_enroll_saved);start.setEnabled(false);
                            updateVisualState("Guardado", "Rostro registrado", COLOR_MINT, Color.parseColor("#2457D6A0"), R.drawable.security_ic_shield_check);
                        });
                    }catch(Exception failure){error(R.string.auth_face_enroll_failed);}
                }
                @Override public void onAuthenticationError(int code,CharSequence message){if(active)error(R.string.auth_face_enroll_failed);}
            });
            BiometricPrompt.PromptInfo info=new BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.auth_face_enroll_title))
                .setSubtitle(getString(R.string.auth_face_enroll_confirm)).setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(getString(R.string.auth_cancel)).build();
            prompt.authenticate(info,new BiometricPrompt.CryptoObject(operation));
        }catch(Exception unavailable){error(R.string.auth_face_enroll_link_required);}
    }
    private <T> void send(Call<ApiResponse<T>> call,Consumer<T> success){
        pending=call;call.enqueue(new Callback<ApiResponse<T>>(){
            public void onResponse(Call<ApiResponse<T>> call,Response<ApiResponse<T>> response){
                if(!active||call.isCanceled())return;pending=null;
                if(response.isSuccessful()&&response.body()!=null&&response.body().isSuccessful()&&response.body().getData()!=null){success.accept(response.body().getData());return;}
                String code="";
                try {if(response.errorBody()!=null){String text=response.errorBody().string();if(text.length()<8192)code=JsonParser.parseString(text).getAsJsonObject().get("error_code").getAsString();}}catch(Exception ignored){}
                if("FACE_ALREADY_REGISTERED".equals(code)||"FACE_ALREADY_IN_ATTENDANCE".equals(code)){profile=null;loadStatus();return;}
                error("FRESH_PASSWORD_REQUIRED".equals(code)?R.string.auth_face_enroll_fresh_required:
                    response.code()==404||response.code()==503?R.string.auth_face_update_required:R.string.auth_face_enroll_failed);
            }
            public void onFailure(Call<ApiResponse<T>> call,Throwable failure){if(active&&!call.isCanceled())error(R.string.auth_connection_error);}
        });
    }
    private void error(int resource){
        if(camera!=null)camera.closeCamera();if(overlay!=null)overlay.setScanning(false);
        profile=null;needsStatus=true;status.setText(resource);start.setText(R.string.auth_face_enroll_retry);start.setEnabled(linked());
        updateVisualState("Aviso", "Atención requerida", COLOR_CORAL, Color.parseColor("#2BFF819D"), R.drawable.admin_ic_alert_triangle);
    }
    private void prepareFaceKey(boolean rotate){
        if(registrationAttempt.unresolved()){showUncertainRegistration();return;}
        if(pending!=null||!linked())return;
        prepareKey.setEnabled(false);status.setText(R.string.auth_face_enroll_saving);
        updateVisualState("Procesando", "Preparando clave facial", COLOR_LAVENDER, Color.parseColor("#2E8B30E0"), R.drawable.security_ic_shield_check);
        keyWorker.execute(()->{
            try {
                String publicKey=faceStore.prepare(rotate);
                runOnUiThread(()->{
                    if(!active)return;Map<String,Object> body=identity();body.put("clave_publica",publicKey);
                    sendRegistration(FaceRegistrationClient.getService().register(session.getBearerToken(),body),FaceRegistrationRequest.Stage.REGISTER,(challenge,http)->{
                        if(challenge.already_active){saveFaceKey(challenge.id_clave,FaceRegistrationRequest.Stage.REGISTER,http);return;}
                        if(challenge.id_clave<=0||challenge.challengeId<=0||challenge.message==null||!challenge.message.startsWith("WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:")){
                            registrationFailure(new FaceRegistrationRequest.Diagnostic(FaceRegistrationRequest.Stage.REGISTER,http,"","RESPUESTA_NO_VALIDA",true));return;
                        }
                        if(!registrationAttempt.resolved()){showUncertainRegistration();return;}
                        authorizeFaceKey(challenge);
                    });
                });
            }catch(Exception unsupported){runOnUiThread(()->{if(active)localRegistrationError("No se pudo preparar la clave facial protegida. No se envió el registro al servidor.");});}
        });
    }
    private void saveFaceKey(long id,FaceRegistrationRequest.Stage stage,int http){
        try{faceStore.save(id,store);if(!registrationAttempt.resolved()){showUncertainRegistration();return;}prepareKey.setEnabled(false);loadFaceAccessStatus();}
        catch(Exception failure){registrationFailure(new FaceRegistrationRequest.Diagnostic(stage,http,"","GUARDADO_LOCAL_NO_CONFIRMADO",true));}
    }
    private void authorizeFaceKey(FaceDeviceResponse challenge){
        if(challenge.id_clave<=0||challenge.challengeId<=0||challenge.message==null||!challenge.message.startsWith("WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:")){error(R.string.auth_face_enroll_failed);return;}
        try {
            Signature operation=store.signingOperation();
            prompt=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){
                    if(!active)return;
                    try {
                        Signature op=result.getCryptoObject()==null?null:result.getCryptoObject().getSignature();if(op==null)throw new java.security.GeneralSecurityException();
                        op.update(challenge.message.getBytes(StandardCharsets.UTF_8));
                        Map<String,Object> body=new HashMap<>();body.put("id_clave",challenge.id_clave);body.put("device_uid",store.uid());
                        body.put("challenge_id",challenge.challengeId);body.put("firma_base64",AndroidDeviceStore.encodeBase64(op.sign()));
                        sendRegistration(FaceRegistrationClient.getService().confirm(session.getBearerToken(),body),FaceRegistrationRequest.Stage.CONFIRM,(confirmed,http)->{
                            if(confirmed.active&&!confirmed.session_issued&&confirmed.auth==null&&confirmed.id_clave==challenge.id_clave)saveFaceKey(confirmed.id_clave,FaceRegistrationRequest.Stage.CONFIRM,http);
                            else registrationFailure(new FaceRegistrationRequest.Diagnostic(FaceRegistrationRequest.Stage.CONFIRM,http,"","RESPUESTA_NO_VALIDA",true));
                        });
                    }catch(Exception failure){localRegistrationError("No se pudo preparar la firma local. No se envió la confirmación.");}
                }
                @Override public void onAuthenticationError(int code,CharSequence message){if(active)localRegistrationError("Autorización con huella cancelada o no disponible. No se envió la confirmación.");}
            });
            prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.auth_face_prepare_key))
                .setSubtitle(getString(R.string.auth_face_enroll_confirm)).setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButtonText(getString(R.string.auth_cancel)).build(),new BiometricPrompt.CryptoObject(operation));
        }catch(Exception failure){localRegistrationError("No se pudo abrir la autorización local con huella. No se envió la confirmación.");}
    }
    private interface RegistrationSuccess {void accept(FaceDeviceResponse response,int http);}
    private void sendRegistration(Call<okhttp3.ResponseBody> call,FaceRegistrationRequest.Stage stage,RegistrationSuccess success){
        if(registrationAttempt.unresolved()){showUncertainRegistration();return;}
        if(!registrationAttempt.dispatched(stage)){localRegistrationError("No se pudo guardar el estado local. No se envió la petición.");return;}
        pending=call;
        FaceRegistrationRequest.enqueue(call,stage,new FaceRegistrationRequest.Listener(){
            public void success(FaceDeviceResponse value,int http){
                runOnUiThread(()->{if(!active||call.isCanceled())return;pending=null;success.accept(value,http);});
            }
            public void failure(FaceRegistrationRequest.Diagnostic diagnostic){
                runOnUiThread(()->{if(!active||call.isCanceled())return;pending=null;registrationFailure(diagnostic);});
            }
        });
    }
    private void registrationFailure(FaceRegistrationRequest.Diagnostic diagnostic){
        if(diagnostic.uncertain){registrationAttempt.record(diagnostic);showUncertainRegistration();return;}
        if(!registrationAttempt.resolved()){showUncertainRegistration();return;}
        if(camera!=null)camera.closeCamera();if(overlay!=null)overlay.setScanning(false);
        profile=null;needsStatus=true;status.setText(diagnostic.display());
        start.setText(R.string.auth_face_enroll_retry);start.setEnabled(linked());prepareKey.setEnabled(false);
        updateVisualState("Aviso", "No se completó el registro", COLOR_CORAL, Color.parseColor("#2BFF819D"), R.drawable.admin_ic_alert_triangle);
    }
    private void showUncertainRegistration(){
        if(camera!=null)camera.closeCamera();if(overlay!=null)overlay.setScanning(false);
        status.setText(registrationAttempt.diagnostic().display());start.setEnabled(false);prepareKey.setEnabled(false);
        updateVisualState("Pendiente", "Estado por verificar", COLOR_CORAL, Color.parseColor("#2BFF819D"), R.drawable.admin_ic_alert_triangle);
    }
    private void localRegistrationError(String message){
        if(registrationAttempt.unresolved()){showUncertainRegistration();return;}
        error(R.string.auth_face_enroll_failed);status.setText(getString(R.string.auth_face_registration_local_diagnostic,message));
    }
    @Override protected void onStop(){
        active=false;profile=null;needsStatus=true;
        if(pending!=null)pending.cancel();if(prompt!=null)prompt.cancelAuthentication();
        if(camera!=null)camera.closeCamera();if(overlay!=null)overlay.setScanning(false);
        super.onStop();
    }
    @Override protected void onDestroy(){keyWorker.shutdown();if(extractor!=null)extractor.destroy();super.onDestroy();}
}
