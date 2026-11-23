package pe.net.ws.kpiweb.features.auth.models;
public final class FaceDeviceResponse extends FaceChallengeResponse {
    public long id_clave;
    public boolean already_active,active,identity_match,device_signature_valid,liveness_verified,session_issued;
    public String reference_source,status;
    public LoginResponseData.AuthTokens auth;
    public LoginResponseData profile;
    public boolean local_pad_accepted;
    public boolean facial_self_service_authorized;
    public boolean facial_login_enabled,self_service_available,recent_password,revoked;
    public String security_profile;
    public User user;
    public static final class User {public int id_usuario;public String username;}
}
