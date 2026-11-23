package pe.net.ws.kpiweb.features.auth;

import static org.junit.Assert.*;
import com.google.gson.Gson;
import java.util.*;
import java.util.function.Consumer;
import org.junit.Test;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.*;
import pe.net.ws.kpiweb.core.session.FaceDeviceStore;
import pe.net.ws.kpiweb.features.auth.models.FaceDeviceResponse;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.Call;

/** Protocol/lifecycle tests. These do not claim optical PAD or hardware-key coverage. */
public class FaceIdentityCoordinatorTest {
    static class Api implements FaceDeviceApi {
        public Call<ApiResponse<FaceDeviceResponse>> status(String t,Map<String,Object>b){throw new AssertionError("No status mutation from login");}
        public Call<ApiResponse<FaceDeviceResponse>> revokeChallenge(String t,Map<String,Object>b){throw new AssertionError("No revocation from login");}
        public Call<ApiResponse<FaceDeviceResponse>> revoke(String t,Map<String,Object>b){throw new AssertionError("No revocation from login");}
        FaceDeviceResponse challenge=new FaceDeviceResponse(), result=new FaceDeviceResponse();
        Map<String,Object> proof; boolean unavailable, limited; int verifications;
        Api(){challenge.facial_self_service_authorized=true;challenge.challengeId=17;challenge.nonce="synthetic-nonce";challenge.message="WEBSITEPLUS_AUTH:LOGIN:17:nonce";
            result.identity_match=true;result.device_signature_valid=true;
            result.user=new FaceDeviceResponse.User();result.user.username="fixture";}
        public Call<ApiResponse<FaceDeviceResponse>> challenge(Map<String,Object> body){return unavailable?TestCall.failure(new java.io.IOException()):limited?TestCall.error(429,"{}","90"):TestCall.success(ApiResponse.success(challenge));}
        public Call<ApiResponse<FaceDeviceResponse>> verify(Map<String,Object> body){verifications++;proof=body;return TestCall.success(ApiResponse.success(result));}
        public Call<ApiResponse<FaceDeviceResponse>> register(String t,Map<String,Object>b){throw new AssertionError("No registration from login");}
        public Call<ApiResponse<FaceDeviceResponse>> confirm(String t,Map<String,Object>b){throw new AssertionError("No confirmation from login");}
    }
    static class Camera implements FaceIdentityCoordinator.Capture {
        Consumer<String> success;boolean stopped;
        public void start(Consumer<String> p,Consumer<Integer> e){success=p;}
        public void stop(){stopped=true;}
    }
    static class Events implements FaceIdentityCoordinator.Listener {
        String matched,retryAfter;int error;boolean loading,authenticated;
        public void loading(boolean b){loading=b;}public void status(int r){}
        public void matched(String user){matched=user;}public void error(int r){error=r;}
        public void rateLimited(String header){retryAfter=header;error(R.string.auth_rate_limit);}
        public void authenticated(LoginResponseData p,LoginResponseData.AuthTokens t){authenticated=true;}
    }
    final Api api=new Api();final Camera camera=new Camera();final Events events=new Events();
    @Test public void pilotNotAuthorizedNeverOpensCamera(){api.challenge.facial_self_service_authorized=false;subject.start();assertNull(camera.success);assertEquals(R.string.auth_face_access_not_authorized,events.error);assertEquals(0,signatures);}
    String signed;int signatures;
    final FaceIdentityCoordinator subject=new FaceIdentityCoordinator(api,3,"synthetic-uid","fixture",message->{signed=message;signatures++;return "signature";},camera,events,Runnable::run,Runnable::run);
    private String payload(){double[] v=new double[128];Arrays.fill(v,.05);return new Gson().toJson(v);}
    @Test public void coincidenceBindsExactPayloadAndDoesNotBecomeLogin()throws Exception{
        subject.start();String raw=payload();camera.success.accept(raw);
        assertEquals(FaceDeviceStore.proofMessage(api.challenge.message,raw),signed);
        assertEquals(raw,api.proof.get("face_payload"));assertEquals(17,api.proof.get("challenge_id"));
        assertEquals("fixture",events.matched);assertFalse(events.loading);assertTrue(camera.stopped);
    }
    @Test public void foreignAccountResponseRejected(){subject.start();api.result.user.username="other";camera.success.accept(payload());assertNull(events.matched);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void unexpectedTokenResponseRejected(){subject.start();api.result.auth=new LoginResponseData.AuthTokens();camera.success.accept(payload());assertNull(events.matched);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void unexpectedSessionFlagRejected(){subject.start();api.result.session_issued=true;camera.success.accept(payload());assertNull(events.matched);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void clientCannotAcceptUnimplementedLiveness(){subject.start();api.result.liveness_verified=true;camera.success.accept(payload());assertNull(events.matched);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void malformedCaptureNeverSigns(){subject.start();camera.success.accept("{bad json");assertEquals(0,signatures);assertNull(api.proof);assertEquals(R.string.auth_face_extractor_unavailable,events.error);}
    @Test public void shortCaptureNeverSigns(){subject.start();camera.success.accept("[1,2]");assertEquals(0,signatures);assertNull(api.proof);}
    @Test public void nonNumericCaptureNeverSigns(){subject.start();camera.success.accept(payload().replaceFirst("0.05","\"0.05\""));assertEquals(0,signatures);}
    @Test public void cancellationIgnoresLateCapture(){subject.start();Consumer<String> late=camera.success;subject.cancel();late.accept(payload());assertNull(api.proof);assertEquals(0,signatures);assertNull(events.matched);}
    @Test public void wrongPurposeNeverOpensCamera(){api.challenge.message="WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:x";subject.start();assertNull(camera.success);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void offlineNeverCapturesOrSigns(){api.unavailable=true;subject.start();assertNull(camera.success);assertEquals(R.string.auth_connection_error,events.error);assertEquals(0,signatures);}
    @Test public void serverRetryAfterIsForwardedWithoutOpeningCamera(){
        api.limited=true;subject.start();assertNull(camera.success);assertEquals("90",events.retryAfter);
        assertEquals(R.string.auth_rate_limit,events.error);assertEquals(0,api.verifications);
    }
    @Test public void cancelledQueuedCryptoNeverUsesProtectedKey(){
        List<Runnable> queued=new ArrayList<>();
        FaceIdentityCoordinator delayed=new FaceIdentityCoordinator(api,3,"synthetic-uid","fixture",message->{signatures++;return "signature";},camera,events,queued::add,Runnable::run);
        delayed.start();camera.success.accept(payload());assertEquals(1,queued.size());
        delayed.cancel();queued.get(0).run();
        assertEquals(0,signatures);assertNull(api.proof);assertNull(events.matched);
    }
    @Test public void repeatedCaptureCallbackSubmitsOnlyOneVerification(){
        List<Runnable> queued=new ArrayList<>();
        FaceIdentityCoordinator delayed=new FaceIdentityCoordinator(api,3,"synthetic-uid","fixture",message->{signatures++;return "signature";},camera,events,queued::add,Runnable::run);
        delayed.start();String raw=payload();camera.success.accept(raw);camera.success.accept(raw);
        assertEquals(1,queued.size());queued.get(0).run();
        assertEquals(1,signatures);assertEquals(1,api.verifications);
    }
    private String screened(){PadTemporalGate gate=new PadTemporalGate();for(int i=0;i<5;i++)gate.observe(i*250,.99,.01);return gate.signedCapture(payload());}
    private void loginResponse(){
        api.result=new Gson().fromJson("{\"identity_match\":true,\"device_signature_valid\":true,\"facial_self_service_authorized\":true,\"local_pad_accepted\":true,\"session_issued\":true,\"security_profile\":\"SELF_SERVICE_LOCAL_PAD\",\"status\":\"AUTHENTICATED_SELF_SERVICE_LOCAL_PAD\",\"user\":{\"id_usuario\":1,\"username\":\"fixture\"},\"profile\":{\"user\":{\"id_usuario\":1,\"username\":\"fixture\",\"id_rol\":5}},\"auth\":{\"access_token\":\""+String.join("",Collections.nCopies(64,"a"))+"\",\"refresh_token\":\""+String.join("",Collections.nCopies(64,"b"))+"\"}}",FaceDeviceResponse.class);
    }
    @Test public void localPadProofSignsEvidenceAndAcceptsBoundSession()throws Exception{
        loginResponse();subject.start();String raw=screened();camera.success.accept(raw);
        assertEquals(FaceDeviceStore.proofMessage(api.challenge.message,raw),signed);assertTrue(events.authenticated);assertFalse(events.loading);
    }
    @Test public void localPadCannotLoginWithAnotherProfilesId(){loginResponse();api.result.profile=new Gson().fromJson("{\"user\":{\"id_usuario\":2,\"username\":\"fixture\"}}",LoginResponseData.class);subject.start();camera.success.accept(screened());assertFalse(events.authenticated);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void differentFaceCannotIssueSession(){loginResponse();api.result.identity_match=false;subject.start();camera.success.accept(screened());assertFalse(events.authenticated);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void rejectedLocalPadCannotIssueSession(){loginResponse();api.result.local_pad_accepted=false;subject.start();camera.success.accept(screened());assertFalse(events.authenticated);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void invalidTokenCannotEnterAccount(){loginResponse();api.result.auth=new LoginResponseData.AuthTokens();subject.start();camera.success.accept(screened());assertFalse(events.authenticated);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void unscopedLegacySessionNeverEnters(){loginResponse();api.result.security_profile="LOCAL_PAD";subject.start();camera.success.accept(screened());assertFalse(events.authenticated);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void missingPilotAuthorizationNeverEnters(){loginResponse();api.result.facial_self_service_authorized=false;subject.start();camera.success.accept(screened());assertFalse(events.authenticated);assertEquals(R.string.auth_server_error,events.error);}
    @Test public void cancelledQueuedLoginNeverEnters(){loginResponse();subject.start();Consumer<String> late=camera.success;subject.cancel();late.accept(screened());assertFalse(events.authenticated);assertEquals(0,signatures);}
}
