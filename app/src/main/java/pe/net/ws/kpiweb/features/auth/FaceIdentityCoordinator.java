package pe.net.ws.kpiweb.features.auth;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import com.google.gson.JsonParser;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.*;
import pe.net.ws.kpiweb.core.session.FaceDeviceStore;
import pe.net.ws.kpiweb.features.auth.models.FaceDeviceResponse;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.*;

/** Account/device verification. LOCAL_PAD login is explicitly weaker than server-evaluated PAD. */
public final class FaceIdentityCoordinator {
    public interface Signer {String sign(String message)throws Exception;}
    public interface Capture {void start(Consumer<String> payload,Consumer<Integer> error);void stop();}
    public interface Listener {void loading(boolean value);void status(int resource);void matched(String username);void error(int resource);
        default void timing(String stage,long started) {}
        default void rateLimited(String retryAfter) {error(R.string.auth_rate_limit);}
        default void authenticated(LoginResponseData profile,LoginResponseData.AuthTokens tokens) {error(R.string.auth_server_error);}}
    private final FaceDeviceApi api;private final long id;private final String uid,username;
    private final Signer signer;private final Capture capture;private final Listener listener;private final Executor crypto,main;
    private Call<?> pending;private volatile int generation;private volatile boolean running;
    public FaceIdentityCoordinator(FaceDeviceApi api,long id,String uid,String username,Signer signer,Capture capture,Listener listener,Executor crypto,Executor main){
        this.api=api;this.id=id;this.uid=uid;this.username=username;this.signer=signer;this.capture=capture;this.listener=listener;this.crypto=crypto;this.main=main;
    }
    private Map<String,Object> identity(){Map<String,Object> body=new HashMap<>();body.put("id_clave",id);body.put("device_uid",uid);return body;}
    public void start(){
        if(running)return;running=true;int run=++generation;listener.loading(true);listener.status(R.string.auth_face_preparing);
        send(api.challenge(identity()),run,"challenge_server",challenge->{
            if(!challenge.facial_self_service_authorized){fail(R.string.auth_face_access_not_authorized);return;}
            if(challenge.challengeId<=0||challenge.message==null||challenge.nonce==null||!challenge.message.startsWith("WEBSITEPLUS_AUTH:LOGIN:")){fail(R.string.auth_server_error);return;}
            listener.status(R.string.auth_face_capturing_identity);
            long captureStarted=FaceTiming.now();
            java.util.concurrent.atomic.AtomicBoolean captureDelivered=new java.util.concurrent.atomic.AtomicBoolean();
            capture.start(payload->{
                if(!current(run)||!captureDelivered.compareAndSet(false,true))return;
                listener.timing("local_capture",captureStarted);capture.stop();listener.status(R.string.auth_face_checking_identity);
                long verificationStarted=FaceTiming.now();
                crypto.execute(()->{
                    if(!current(run))return;
                    try {
                        // Enforce dimensions/types before performing a protected signing operation.
                        com.google.gson.JsonElement parsed=JsonParser.parseString(payload);
                        boolean local=parsed.isJsonObject();
                        com.google.gson.JsonArray v;
                        if(local){
                            com.google.gson.JsonObject obj=parsed.getAsJsonObject();
                            if(obj.get("version").getAsInt()!=2)throw new IllegalArgumentException();
                            com.google.gson.JsonObject pad=obj.getAsJsonObject("pad");
                            if(!"MiniFASNetV1SE+V2-min".equals(pad.get("model").getAsString())||pad.get("samples").getAsInt()<5
                                ||pad.get("duration_ms").getAsLong()<800||pad.get("real_min").getAsDouble()<.95)throw new IllegalArgumentException();
                            v=obj.getAsJsonArray("descriptor");
                        }else v=parsed.getAsJsonArray();
                        if(v.size()!=128)throw new IllegalArgumentException();
                        for(com.google.gson.JsonElement n:v)if(!n.isJsonPrimitive()||!n.getAsJsonPrimitive().isNumber()||!Double.isFinite(n.getAsDouble()))throw new IllegalArgumentException();
                        String signature=signer.sign(FaceDeviceStore.proofMessage(challenge.message,payload));
                        main.execute(()->{
                            if(!current(run))return;Map<String,Object> proof=identity();proof.put("challenge_id",challenge.challengeId);proof.put("nonce",challenge.nonce);
                            proof.put("firma_base64",signature);proof.put("face_payload",payload);
                            listener.timing("local_signature",verificationStarted);
                            send(api.verify(proof),run,"verify_server",result->{
                                if(!result.identity_match||!result.device_signature_valid||result.liveness_verified
                                    ||result.user==null||!username.equals(result.user.username)){fail(R.string.auth_server_error);return;}
                                if(result.session_issued){
                                    if(!local||!result.facial_self_service_authorized||!result.local_pad_accepted||!"SELF_SERVICE_LOCAL_PAD".equals(result.security_profile)||!"AUTHENTICATED_SELF_SERVICE_LOCAL_PAD".equals(result.status)
                                        ||result.profile==null||result.profile.getUser()==null||result.profile.getUser().getIdUsuario()<=0
                                        ||result.profile.getUser().getIdUsuario()!=result.user.id_usuario||!username.equals(result.profile.getUser().getUsername())
                                        ||result.auth==null||result.auth.getAccessToken()==null||!result.auth.getAccessToken().matches("[a-fA-F0-9]{64}")
                                        ||result.auth.getRefreshToken()==null||!result.auth.getRefreshToken().matches("[a-fA-F0-9]{64}")){fail(R.string.auth_server_error);return;}
                                    running=false;listener.loading(false);listener.authenticated(result.profile,result.auth);return;
                                }
                                if(result.auth!=null){fail(R.string.auth_server_error);return;}
                                running=false;listener.loading(false);listener.matched(result.user.username);
                            });
                        });
                    }catch(RuntimeException malformed){main.execute(()->{if(current(run))fail(R.string.auth_face_extractor_unavailable);});}
                    catch(Exception locked){main.execute(()->{if(current(run))fail(R.string.auth_face_recent_unlock);});}
                });
            },error->{if(current(run))fail(error);});
        });
    }
    private boolean current(int run){return running&&generation==run;}
    private void send(Call<ApiResponse<FaceDeviceResponse>> call,int run,String stage,Consumer<FaceDeviceResponse> success){
        long started=FaceTiming.now();
        pending=call;call.enqueue(new Callback<ApiResponse<FaceDeviceResponse>>(){
            public void onResponse(Call<ApiResponse<FaceDeviceResponse>> c,Response<ApiResponse<FaceDeviceResponse>> r){
                if(!current(run)||c.isCanceled())return;
                listener.timing(stage,started);
                if(r.code()==429){running=false;capture.stop();listener.loading(false);listener.rateLimited(r.headers().get("Retry-After"));return;}
                if(r.isSuccessful()&&r.body()!=null&&r.body().isSuccessful()&&r.body().getData()!=null){success.accept(r.body().getData());return;}
                String code="";try{if(r.errorBody()!=null){String raw=r.errorBody().string();if(raw.length()<8192)code=JsonParser.parseString(raw).getAsJsonObject().get("error_code").getAsString();}}catch(Exception ignored){}
                fail("FACE_MISMATCH".equals(code)?R.string.auth_face_identity_mismatch:
                    "FACE_PROFILE_PENDING".equals(code)?R.string.auth_face_profile_pending:
                    "FACE_ENROLLMENT_REQUIRED".equals(code)?R.string.auth_face_no_record:
                    "FACE_KEY_NOT_AUTHORIZED".equals(code)?R.string.auth_face_key_needed:
                    r.code()==404||r.code()==503?R.string.auth_face_update_required:R.string.auth_face_enroll_failed);
            }
            public void onFailure(Call<ApiResponse<FaceDeviceResponse>> c,Throwable error){if(current(run)&&!c.isCanceled()){listener.timing(stage,started);fail(R.string.auth_connection_error);}}
        });
    }
    private void fail(int resource){running=false;capture.stop();listener.loading(false);listener.error(resource);}
    public void cancel(){++generation;running=false;if(pending!=null)pending.cancel();capture.stop();listener.loading(false);}
}
