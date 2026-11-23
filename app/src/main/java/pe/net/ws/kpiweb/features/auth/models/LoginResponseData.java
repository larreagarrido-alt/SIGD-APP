package pe.net.ws.kpiweb.features.auth.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LoginResponseData {
    @SerializedName("auth")
    private AuthTokens auth;

    @SerializedName("user")
    private UserInfo user;

    @SerializedName("worker")
    private WorkerInfo worker;

    @SerializedName("permissions")
    private List<String> permissions;

    @SerializedName("verified")
    private boolean verified;

    @SerializedName("session_issued")
    private boolean sessionIssued;

    @SerializedName("mode")
    private String mode;

    @SerializedName("message")
    private String message;

    public AuthTokens getAuth() { return auth; }
    public UserInfo getUser() { return user; }
    public WorkerInfo getWorker() { return worker; }
    public List<String> getPermissions() { return permissions; }
    public boolean isVerified() { return verified; }
    public boolean isSessionIssued() { return sessionIssued; }
    public String getMode() { return mode; }
    public String getMessage() { return message; }

    public static class AuthTokens {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("refresh_token")
        private String refreshToken;

        @SerializedName("expires_in")
        private int expiresIn;

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public int getExpiresIn() { return expiresIn; }
    }

    public static class UserInfo {
        @SerializedName("id_usuario")
        private int idUsuario;

        @SerializedName("username")
        private String username;

        @SerializedName("id_rol")
        private int idRol;

        @SerializedName("rol_nombre")
        private String rolNombre;

        @SerializedName("debe_cambiar_password")
        private boolean debeCambiarPassword;

        public int getIdUsuario() { return idUsuario; }
        public String getUsername() { return username; }
        public int getIdRol() { return idRol; }
        public String getRolNombre() { return rolNombre; }
        public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    }

    public static class WorkerInfo {
        @SerializedName("id_trabajador")
        private int idTrabajador;

        @SerializedName("dni")
        private String dni;

        @SerializedName("nombres")
        private String nombres;

        @SerializedName("apellidos")
        private String apellidos;

        @SerializedName("nombre_completo")
        private String nombreCompleto;

        @SerializedName("tipo")
        private String tipo;

        @SerializedName("estado")
        private String estado;

        @SerializedName("cargo_nombre")
        private String cargoNombre;

        @SerializedName("id_cargo")
        private Integer idCargo;

        @SerializedName("id_agencia")
        private Integer idAgencia;

        @SerializedName("agencia_nombre")
        private String agenciaNombre;

        @SerializedName("email")
        private String email;

        @SerializedName("telefono")
        private String telefono;

        @SerializedName("fecha_inicio")
        private String fechaInicio;

        @SerializedName("foto_perfil")
        private String fotoPerfil;

        @SerializedName("saldo_puntos")
        private Integer saldoPuntos;

        public int getIdTrabajador() { return idTrabajador; }
        public String getDni() { return dni; }
        public String getNombreCompleto() { return nombreCompleto; }
        public String getTipo() { return tipo; }
        public String getEstado() { return estado; }
        public String getCargoNombre() { return cargoNombre; }
        public Integer getIdCargo() { return idCargo; }
        public Integer getIdAgencia() { return idAgencia; }
        public String getAgenciaNombre() { return agenciaNombre; }
        public String getEmail() { return email; }
        public String getTelefono() { return telefono; }
        public String getFechaInicio() { return fechaInicio; }
        public String getFotoPerfil() { return fotoPerfil; }
        public Integer getSaldoPuntos() { return saldoPuntos; }
    }
}
