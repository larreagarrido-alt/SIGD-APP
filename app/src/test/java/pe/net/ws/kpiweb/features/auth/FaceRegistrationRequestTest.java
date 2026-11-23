package pe.net.ws.kpiweb.features.auth;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Test;
import pe.net.ws.kpiweb.features.auth.models.FaceDeviceResponse;
import static org.junit.Assert.*;

public class FaceRegistrationRequestTest {
    private static final class Result implements FaceRegistrationRequest.Listener {
        FaceRegistrationRequest.Diagnostic diagnostic;FaceDeviceResponse value;int callbacks;
        public void success(FaceDeviceResponse value,int http){this.value=value;callbacks++;assertEquals(200,http);}
        public void failure(FaceRegistrationRequest.Diagnostic diagnostic){this.diagnostic=diagnostic;callbacks++;}
    }
    private ResponseBody body(String json){return ResponseBody.create(MediaType.parse("application/json"),json);}
    @Test public void displaysRealRegisterHttpAndBackendCodeWithoutBackendMessage(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.error(403,"{\"error_code\":\"FRESH_PASSWORD_REQUIRED\",\"message\":\"private-message-token\"}"),FaceRegistrationRequest.Stage.REGISTER,r);
        assertEquals(403,r.diagnostic.http);assertEquals("FRESH_PASSWORD_REQUIRED",r.diagnostic.backendCode);
        assertFalse(r.diagnostic.uncertain);assertTrue(r.diagnostic.display().contains("inicio del registro"));
        assertFalse(r.diagnostic.display().contains("private-message-token"));assertEquals(1,r.callbacks);
    }
    @Test public void confirmSignatureRefusalIsNotReportedAsConnectionFailure(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.error(401,"{\"error_code\":\"INVALID_SIGNATURE\"}"),FaceRegistrationRequest.Stage.CONFIRM,r);
        assertEquals(401,r.diagnostic.http);assertEquals("INVALID_SIGNATURE",r.diagnostic.backendCode);
        assertEquals(FaceRegistrationRequest.Stage.CONFIRM,r.diagnostic.stage);assertFalse(r.diagnostic.uncertain);
    }
    @Test public void serverErrorRetainsCodeAndCannotBeConsideredRollbackProof(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.error(500,"{\"error_code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"SQL password secret\"}"),FaceRegistrationRequest.Stage.REGISTER,r);
        assertTrue(r.diagnostic.uncertain);assertEquals(500,r.diagnostic.http);assertFalse(r.diagnostic.display().contains("password secret"));
    }
    @Test public void connectionFailureDoesNotClaimNotSentAndDoesNotRetry(){
        Result r=new Result();TestCall<ResponseBody> call=TestCall.failure(new IOException("secret-token-signature-face"));
        FaceRegistrationRequest.enqueue(call,FaceRegistrationRequest.Stage.CONFIRM,r);
        assertEquals(0,r.diagnostic.http);assertEquals("CONEXION_SIN_RESPUESTA",r.diagnostic.localCode);
        assertTrue(r.diagnostic.uncertain);assertEquals(1,r.callbacks);assertFalse(r.diagnostic.display().contains("secret-token-signature-face"));
    }
    @Test public void htmlOnHttp200KeepsActualStatusAndUnknownOutcome(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.success(body("<html>private-proxy-body</html>")),FaceRegistrationRequest.Stage.CONFIRM,r);
        assertEquals(200,r.diagnostic.http);assertTrue(r.diagnostic.uncertain);assertEquals("RESPUESTA_NO_VALIDA",r.diagnostic.localCode);
        assertFalse(r.diagnostic.display().contains("private-proxy-body"));
    }
    @Test public void proxyHtml403IsNotTreatedAsConfirmedBackendRejection(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.error(403,"<html>blocked by proxy</html>"),FaceRegistrationRequest.Stage.REGISTER,r);
        assertEquals(403,r.diagnostic.http);assertEquals("",r.diagnostic.backendCode);assertTrue(r.diagnostic.uncertain);
    }
    @Test public void canceledRequestCannotDispatchCallbacksOrAutomaticRetries(){
        Result r=new Result();TestCall<ResponseBody> call=TestCall.failure(new IOException("fixture"));call.cancel();
        FaceRegistrationRequest.enqueue(call,FaceRegistrationRequest.Stage.REGISTER,r);assertEquals(0,r.callbacks);
    }
    @Test public void successfulBeginKeepsChallengeFieldsForExistingSignatureFlow(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.success(body("{\"status\":\"success\",\"data\":{\"id_clave\":17,\"challenge_id\":8,\"message\":\"WEBSITEPLUS_AUTH:REGISTRO_DISPOSITIVO:fixture\"}}")),FaceRegistrationRequest.Stage.REGISTER,r);
        assertEquals(17,r.value.id_clave);assertEquals(8,r.value.challengeId);assertNull(r.diagnostic);assertEquals(1,r.callbacks);
    }
    @Test public void oversizedOrUnsafeErrorCodeIsNeverDisplayed(){
        Result r=new Result();FaceRegistrationRequest.enqueue(TestCall.error(502,"{\"error_code\":\"Bearer private-token\"}"),FaceRegistrationRequest.Stage.REGISTER,r);
        assertEquals("",r.diagnostic.backendCode);assertFalse(r.diagnostic.display().contains("private-token"));
        Result large=new Result();FaceRegistrationRequest.enqueue(TestCall.error(500,"x".repeat(8193)),FaceRegistrationRequest.Stage.REGISTER,large);
        assertTrue(large.diagnostic.uncertain);assertEquals("",large.diagnostic.backendCode);
    }
    @Test public void unresolvedDispatchSurvivesActivityRecreationWithoutStoringSensitiveData(){
        MockSharedPreferences prefs=new MockSharedPreferences();FaceRegistrationAttempt attempt=new FaceRegistrationAttempt(prefs);
        assertTrue(attempt.dispatched(FaceRegistrationRequest.Stage.CONFIRM));
        FaceRegistrationAttempt recreated=new FaceRegistrationAttempt(prefs);assertTrue(recreated.unresolved());
        assertEquals(FaceRegistrationRequest.Stage.CONFIRM,recreated.diagnostic().stage);assertTrue(recreated.diagnostic().uncertain);
        assertEquals(4,prefs.getAll().size());assertFalse(prefs.contains("access_token"));assertFalse(prefs.contains("firma_base64"));assertFalse(prefs.contains("device_uid"));
    }
    @Test public void explicitKnownResponseCanResolveButNetworkErrorStaysPersisted(){
        FaceRegistrationAttempt attempt=new FaceRegistrationAttempt(new MockSharedPreferences());attempt.dispatched(FaceRegistrationRequest.Stage.REGISTER);
        assertTrue(attempt.record(new FaceRegistrationRequest.Diagnostic(FaceRegistrationRequest.Stage.REGISTER,500,"INTERNAL_SERVER_ERROR","",true)));
        assertEquals(500,attempt.diagnostic().http);assertTrue(attempt.unresolved());assertTrue(attempt.resolved());assertFalse(attempt.unresolved());
    }
}
