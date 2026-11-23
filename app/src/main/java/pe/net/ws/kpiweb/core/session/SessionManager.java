package pe.net.ws.kpiweb.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {
    private static final String PREF_NAME = "wsp_session_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROL_ID = "rol_id";
    private static final String KEY_WORKER_ID = "worker_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_CARGO_NAME = "cargo_name";
    private static final String KEY_CARGO_ID = "cargo_id";
    private static final String KEY_ROL_NAME = "rol_name";
    private static final String KEY_AGENCIA_NAME = "agencia_name";
    private static final String KEY_WORKER_TIPO = "worker_tipo";
    private static final String KEY_WORKER_EMAIL = "worker_email";
    private static final String KEY_WORKER_PHOTO = "worker_photo";
    private static final String KEY_WORKER_STATUS = "worker_status";
    private static final String KEY_POINTS_BALANCE = "points_balance";
    private static final String KEY_DNI = "user_dni";

    private final SharedPreferences prefs;
    private final Context appContext;

    public SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public SessionManager(SharedPreferences prefs) {
        this.appContext = null;
        this.prefs = prefs;
    }

    public void saveSession(String accessToken, String refreshToken, int userId, String username, int roleId, int workerId, String fullName) {
        saveSession(accessToken, refreshToken, userId, username, roleId, workerId, fullName, "", "", "", "");
    }

    public void saveSession(String accessToken, String refreshToken, int userId, String username, int roleId, int workerId,
                            String fullName, String cargoName, String roleName) {
        saveSession(accessToken, refreshToken, userId, username, roleId, workerId, fullName, cargoName, roleName, "", "");
    }

    public void saveSession(String accessToken, String refreshToken, int userId, String username, int roleId, int workerId,
                            String fullName, String cargoName, String roleName, String agenciaName, String workerTipo) {
        boolean sameWorker = workerId > 0 && workerId == prefs.getInt(KEY_WORKER_ID, 0);
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                // Quick access stores refresh tokens only in BiometricTokenStore, encrypted.
                .remove(KEY_REFRESH_TOKEN)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username)
                .putInt(KEY_ROL_ID, roleId)
                .putInt(KEY_WORKER_ID, workerId)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_CARGO_NAME, cargoName != null ? cargoName : "")
                .putString(KEY_ROL_NAME, roleName != null ? roleName : "")
                .putString(KEY_AGENCIA_NAME, agenciaName != null ? agenciaName : "")
                .putString(KEY_WORKER_TIPO, workerTipo != null ? workerTipo : "")
                .remove(KEY_POINTS_BALANCE);
        if (!sameWorker) editor.remove(KEY_WORKER_EMAIL).remove(KEY_WORKER_PHOTO).remove(KEY_WORKER_STATUS).remove(KEY_DNI).remove(KEY_CARGO_ID);
        editor.apply();
    }

    public void saveWorkerContact(String email, String photo, String status) {
        prefs.edit().putString(KEY_WORKER_EMAIL, email == null ? "" : email.trim())
                .putString(KEY_WORKER_PHOTO, photo == null ? "" : photo.trim())
                .putString(KEY_WORKER_STATUS, status == null ? "" : status.trim()).apply();
    }

    /** Refreshes display fields only after /auth/me confirms the same signed-in worker. */
    public void updateVerifiedWorkerDetails(int workerId, String fullName, String cargo,
                                            String agency, String tipo, String email,
                                            String photo, String status) {
        if (!isLoggedIn() || workerId <= 0 || workerId != getWorkerId()) return;
        prefs.edit()
                .putString(KEY_FULL_NAME, fullName == null ? "" : fullName.trim())
                .putString(KEY_CARGO_NAME, cargo == null ? "" : cargo.trim())
                .putString(KEY_AGENCIA_NAME, agency == null ? "" : agency.trim())
                .putString(KEY_WORKER_TIPO, tipo == null ? "" : tipo.trim())
                .putString(KEY_WORKER_EMAIL, email == null ? "" : email.trim())
                .putString(KEY_WORKER_PHOTO, photo == null ? "" : photo.trim())
                .putString(KEY_WORKER_STATUS, status == null ? "" : status.trim())
                .apply();
    }

    /** The KPI endpoint reads these fields from the current Asistencias worker row. */
    public void updateVerifiedKpiLaborFields(int workerId, int cargoId, String cargo, String tipo) {
        if (!isLoggedIn() || workerId <= 0 || workerId != getWorkerId()
                || cargoId <= 0
                || cargo == null || cargo.trim().isEmpty()
                || !("Practicante".equals(tipo) || "Colaborador".equals(tipo))) return;
        prefs.edit().putInt(KEY_CARGO_ID, cargoId).putString(KEY_CARGO_NAME, cargo.trim())
                .putString(KEY_WORKER_TIPO, tipo).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getString(KEY_ACCESS_TOKEN, null) != null;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getBearerToken() {
        String token = getAccessToken();
        return token.isEmpty() ? "" : "Bearer " + token;
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "");
    }

    public int getRoleId() {
        return prefs.getInt(KEY_ROL_ID, 0);
    }

    public int getWorkerId() {
        return prefs.getInt(KEY_WORKER_ID, 0);
    }

    public String getCargoName() {
        return prefs.getString(KEY_CARGO_NAME, "");
    }

    public int getCargoId() {
        return prefs.getInt(KEY_CARGO_ID, 0);
    }

    public String getRolName() {
        return prefs.getString(KEY_ROL_NAME, "");
    }

    public String getAgenciaName() {
        return prefs.getString(KEY_AGENCIA_NAME, "");
    }

    public String getWorkerTipo() {
        return prefs.getString(KEY_WORKER_TIPO, "");
    }

    public String getWorkerEmail() {
        return prefs.getString(KEY_WORKER_EMAIL, "");
    }

    public String getWorkerPhoto() {
        return prefs.getString(KEY_WORKER_PHOTO, "");
    }

    public String getWorkerStatus() {
        return prefs.getString(KEY_WORKER_STATUS, "");
    }

    public String getDni() {
        return prefs.getString(KEY_DNI, "");
    }

    /** Store only the DNI returned for the worker verified by /auth/me. */
    public void saveVerifiedDni(int workerId, String dni) {
        if (isLoggedIn() && workerId > 0 && workerId == getWorkerId()) {
            prefs.edit().putString(KEY_DNI, dni == null ? "" : dni.trim()).apply();
        }
    }

    public void savePointsBalance(int points) {
        prefs.edit().putInt(KEY_POINTS_BALANCE, points).apply();
    }

    public Integer getPointsBalance() {
        return prefs.contains(KEY_POINTS_BALANCE) ? prefs.getInt(KEY_POINTS_BALANCE, 0) : null;
    }

    public void clear() {
        clearActiveSession();
        if (appContext != null) {
            new BiometricTokenStore(appContext).clear();
            new AndroidDeviceStore(appContext).clear();
        }
    }

    /** Leave the signed-in screen while retaining the encrypted, biometric-protected quick access. */
    @android.annotation.SuppressLint("ApplySharedPref")
    public void clearActiveSession() {
        // commit() is used intentionally to guarantee synchronous eviction before navigating away.
        prefs.edit().clear().commit();
    }
}
