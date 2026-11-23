package pe.net.ws.kpiweb.features.auth;

import com.google.gson.Gson;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.Before;
import org.junit.Test;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;
import pe.net.ws.kpiweb.features.auth.models.LoginRequest;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;

import static org.junit.Assert.*;

public class AndroidBiometricAuthTest {

    private final Gson gson = new Gson();
    private MockSharedPreferences mockPrefs;
    private MockSharedPreferences mockSessionPrefs;
    private AndroidDeviceStore store;
    private SessionManager session;
    private TestApiService api;
    private TestPromptBridge promptBridge;
    private TestListener listener;
    private AndroidDeviceStore.SoftwareEcCrypto testCrypto;
    private AndroidBiometricCoordinator coordinator;

    @Before
    public void setUp() {
        mockPrefs = new MockSharedPreferences();
        mockSessionPrefs = new MockSharedPreferences();
        testCrypto = new AndroidDeviceStore.SoftwareEcCrypto();
        store = new AndroidDeviceStore(mockPrefs, testCrypto);
        session = new SessionManager(mockSessionPrefs);
        api = new TestApiService();
        promptBridge = new TestPromptBridge();
        listener = new TestListener();
        coordinator = new AndroidBiometricCoordinator(
                listener,
                store,
                session,
                api,
                promptBridge,
                () -> false,
                null,
                false
        );
    }

    private static String valid64Hex(char fill) {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < 64; i++) sb.append(fill);
        return sb.toString();
    }

    private LoginResponseData createProfile(int idUsuario, String username) {
        String json = "{\"user\":{\"id_usuario\":" + idUsuario + ",\"username\":\"" + username + "\"}}";
        return gson.fromJson(json, LoginResponseData.class);
    }

    private LoginResponseData.AuthTokens createTokens(char fill) {
        String hex = valid64Hex(fill);
        String json = "{\"access_token\":\"" + hex + "\",\"refresh_token\":\"" + hex + "\",\"expires_in\":3600}";
        return gson.fromJson(json, LoginResponseData.AuthTokens.class);
    }

    // =========================================================================
    // 15 ESCENARIOS OBLIGATORIOS DEL COORDINADOR
    // =========================================================================

    /**
     * 1. Primera vinculación sin credenciales locales.
     */
    @Test
    public void test01_PrimeraVinculacionSinCredencialesLocales() {
        assertFalse("Debe iniciar sin credenciales", store.hasCredential());

        AndroidAccessChallenge regChallenge = new AndroidAccessChallenge();
        regChallenge.accessId = 101;
        regChallenge.challengeId = 2001;
        regChallenge.message = "CHALLENGE_REG_101";

        api.registerAndroidCalls.add(TestCall.success(ApiResponse.success(regChallenge)));
        api.confirmAndroidCalls.add(TestCall.success(ApiResponse.success(new Object())));

        promptBridge.setAction(TestPromptBridge.Action.SUCCEED);

        coordinator.register("jair_test");

        assertTrue("listener.linked() debe haberse llamado", listener.linkedCalled);
        assertNull("No debe haber error", listener.lastErrorMessage);
        assertTrue("La vinculación debe quedar confirmada localmente", store.isEnabled());
        assertEquals(101, store.id());
        assertEquals("jair_test", store.username());
        assertNotNull("api.lastRegisterBody debe tener la clave pública", api.lastRegisterBody.get("clave_publica"));
        assertNotNull("api.lastConfirmBody debe tener la firma", api.lastConfirmBody.get("firma_base64"));
    }

    /**
     * 2. Vinculación antigua que no existe en V011: DEVICE_NOT_FOUND.
     */
    @Test
    public void test02_VinculacionAntiguaNoExisteEnV011_DeviceNotFound() throws GeneralSecurityException {
        String originalUid = store.uid();
        store.save(999, "jair_test", false);
        assertTrue(store.hasCredential());

        // 1. challengeAndroid devuelve 401 DEVICE_NOT_FOUND
        String errorJson = "{\"status\":\"error\",\"code\":401,\"error_code\":\"DEVICE_NOT_FOUND\",\"message\":\"No existe\"}";
        api.challengeAndroidCalls.add(TestCall.error(401, errorJson));

        // 2. El coordinador purga y comienza un nuevo registro limpio
        AndroidAccessChallenge newChallenge = new AndroidAccessChallenge();
        newChallenge.accessId = 1002;
        newChallenge.challengeId = 3001;
        newChallenge.message = "CHALLENGE_NEW_1002";

        api.registerAndroidCalls.add(TestCall.success(ApiResponse.success(newChallenge)));
        api.confirmAndroidCalls.add(TestCall.success(ApiResponse.success(new Object())));

        coordinator.register("jair_test");

        assertTrue("Debe completar la nueva vinculación", listener.linkedCalled);
        assertEquals(1002, store.id());
        assertTrue("Debe quedar habilitado tras confirmación", store.isEnabled());
        assertEquals("El UID del hardware debe mantenerse constante", originalUid, store.uid());
    }

    /**
     * 3. Vinculación revocada: DEVICE_REVOKED.
     */
    @Test
    public void test03_VinculacionRevocada_DeviceRevoked() throws GeneralSecurityException {
        store.save(555, "jair_test", true);

        String errorJson = "{\"status\":\"error\",\"code\":401,\"error_code\":\"DEVICE_REVOKED\",\"message\":\"Revocado\"}";
        api.challengeAndroidCalls.add(TestCall.error(401, errorJson));

        AndroidAccessChallenge newChallenge = new AndroidAccessChallenge();
        newChallenge.accessId = 1003;
        newChallenge.challengeId = 3002;
        newChallenge.message = "CHALLENGE_NEW_1003";

        api.registerAndroidCalls.add(TestCall.success(ApiResponse.success(newChallenge)));
        api.confirmAndroidCalls.add(TestCall.success(ApiResponse.success(new Object())));

        coordinator.register("jair_test");

        assertTrue("Debe reemplazar la clave revocada con un registro nuevo", listener.linkedCalled);
        assertEquals(1003, store.id());
        assertTrue(store.isEnabled());
    }

    /**
     * 4. Registro incompleto en el servidor: REGISTRATION_INCOMPLETE.
     */
    @Test
    public void test04_RegistroIncompleto_RegistrationIncomplete() throws GeneralSecurityException {
        store.save(666, "jair_test", false);

        String errorJson = "{\"status\":\"error\",\"code\":401,\"error_code\":\"REGISTRATION_INCOMPLETE\",\"message\":\"Incompleto\"}";
        api.challengeAndroidCalls.add(TestCall.error(401, errorJson));

        AndroidAccessChallenge newChallenge = new AndroidAccessChallenge();
        newChallenge.accessId = 1004;
        newChallenge.challengeId = 3003;
        newChallenge.message = "CHALLENGE_NEW_1004";

        api.registerAndroidCalls.add(TestCall.success(ApiResponse.success(newChallenge)));
        api.confirmAndroidCalls.add(TestCall.success(ApiResponse.success(new Object())));

        coordinator.register("jair_test");

        assertTrue(listener.linkedCalled);
        assertEquals(1004, store.id());
        assertTrue(store.isEnabled());
    }

    /**
     * 5. Vinculación activa y válida (desbloqueo biométrico normal y revalidación en login).
     */
    @Test
    public void test05_VinculacionActivaYValida_UnlockYRegister() throws GeneralSecurityException {
        store.generateKey();
        store.save(777, "jair_test", true);

        // Caso 5A: Desbloqueo biométrico
        AndroidAccessChallenge loginChallenge = new AndroidAccessChallenge();
        loginChallenge.accessId = 777;
        loginChallenge.challengeId = 4001;
        loginChallenge.message = "WEBSITEPLUS_AUTH:LOGIN:1:nonce:NONE:2026-10-16 12:00:00";

        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(loginChallenge)));

        LoginResponseData.AuthTokens tokens = createTokens('a');
        api.loginAndroidCalls.add(TestCall.success(ApiResponse.success(tokens)));

        LoginResponseData profile = createProfile(1, "jair_test");
        api.getProfileCalls.add(TestCall.success(ApiResponse.success(profile)));

        coordinator.unlock();

        assertNotNull("listener.authenticatedProfile no debe ser nulo", listener.authenticatedProfile);
        assertEquals("jair_test", listener.authenticatedProfile.getUser().getUsername());
        assertEquals(tokens.getAccessToken(), listener.authenticatedTokens.getAccessToken());

        // Caso 5B: register() con cuenta ya vinculada y confirmada
        listener.reset();
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(loginChallenge)));

        coordinator.register("jair_test");

        assertTrue("listener.alreadyLinked() debe llamarse sin duplicar en servidor", listener.alreadyLinkedCalled);
        assertNull(listener.lastErrorMessage);
    }

    /**
     * 6. Error al guardar confirmed=true simulando la secuencia real:
     * 1. El usuario inicia una vinculación nueva.
     * 2. Android guarda correctamente la información local pendiente (confirmed=false).
     * 3. El servidor recibe y confirma correctamente la firma biométrica.
     * 4. El servidor marca el dispositivo como ACTIVO.
     * 5. Android intenta guardar confirmed=true.
     * 6. El guardado local falla específicamente en ese momento.
     * 7. El coordinador detecta el error.
     * 8. La aplicación no afirma falsamente que el estado local quedó guardado.
     * 9. Un reintento puede recuperar el vínculo autorizado, demostrando la posesión
     *    de la clave privada mediante una nueva firma válida.
     * 10. No se generan registros duplicados ni se revoca la vinculación legítima.
     */
    @Test
    public void test06_ErrorAlGuardarConfirmedTrue_SecuenciaReal_YRecuperacionConFirma() throws GeneralSecurityException {
        // 1. El usuario inicia una vinculación nueva
        AndroidAccessChallenge regChallenge = new AndroidAccessChallenge();
        regChallenge.accessId = 888;
        regChallenge.challengeId = 5001;
        regChallenge.message = "CHALLENGE_REG_888";

        api.registerAndroidCalls.add(TestCall.success(ApiResponse.success(regChallenge)));
        api.confirmAndroidCalls.add(TestCall.success(ApiResponse.success(new Object())));
        promptBridge.setAction(TestPromptBridge.Action.SUCCEED);

        // Activamos fallo selectivo ÚNICAMENTE cuando se intente guardar confirmed=true (Paso 5 y 6).
        // El guardado inicial de confirmed=false (Paso 2) se ejecuta y persiste correctamente.
        mockPrefs.setFailOnlyOnConfirmedTrue(true);

        coordinator.register("jair_test");

        // Paso 2 verificado: los datos pendientes se guardaron
        assertEquals(888, store.id());
        assertEquals("jair_test", store.username());
        assertTrue(store.hasCredential());

        // Pasos 3 y 4 verificados: el servidor recibió registro y confirmación de firma
        assertNotNull("api.lastRegisterBody debe existir", api.lastRegisterBody);
        assertNotNull("api.lastConfirmBody debe tener firma", api.lastConfirmBody);

        // Pasos 7 y 8 verificados: el coordinador detectó el fallo al persistir confirmed=true
        assertFalse("NO debe llamar a listener.linked() si falló el guardado local de confirmed=true", listener.linkedCalled);
        assertFalse("NO debe llamar a listener.alreadyLinked() si falló el guardado local de confirmed=true", listener.alreadyLinkedCalled);
        assertEquals("Debe reportar error seguro al usuario",
                (Integer) R.string.auth_biometric_key_unavailable, listener.lastErrorMessage);
        assertFalse("El almacén no debe figurar habilitado", store.isEnabled());

        // Paso 9: Reintento de recuperación demostrando posesión de clave con nueva firma válida
        listener.reset();
        mockPrefs.setFailOnlyOnConfirmedTrue(false);

        AndroidAccessChallenge recoveryChallenge = new AndroidAccessChallenge();
        recoveryChallenge.accessId = 888;
        recoveryChallenge.challengeId = 5002;
        recoveryChallenge.message = "CHALLENGE_RECOVERY_888";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(recoveryChallenge)));

        LoginResponseData.AuthTokens tokens = createTokens('f');
        api.loginAndroidCalls.add(TestCall.success(ApiResponse.success(tokens)));
        LoginResponseData profile = createProfile(1, "jair_test");
        api.getProfileCalls.add(TestCall.success(ApiResponse.success(profile)));

        coordinator.register("jair_test");

        // Paso 10 verificado: Recuperación exitosa con firma, sin duplicar registro ni revocar
        assertNotNull("Debe completar la autenticación recuperando el vínculo", listener.authenticatedProfile);
        assertEquals("jair_test", listener.authenticatedProfile.getUser().getUsername());
        assertTrue("La vinculación ahora sí debe quedar confirmada localmente", store.isEnabled());
        assertEquals(888, store.id());
        assertEquals("No debe haber llamadas adicionales a registerAndroid (solo 1 original)", 1, api.registerAndroidCount);
        assertEquals("No debe haber llamadas a revokeAndroid", 0, api.revokeAndroidCount);
    }

    /**
     * 7. Pérdida de conexión después de activar el dispositivo en el servidor (sanación con firma).
     */
    @Test
    public void test07_PerdidaDeConexionDespuesDeActivarEnServidor_SanaConFirma() throws GeneralSecurityException {
        // Estado local pendiente (confirmed=false), pero en MySQL el servidor ya lo tiene en ACTIVO
        store.generateKey();
        store.save(901, "jair_test", false);
        assertFalse("Inicialmente confirmed es false", store.isEnabled());

        // El servidor devuelve challenge (HTTP 200) porque la fila está ACTIVA
        AndroidAccessChallenge challenge = new AndroidAccessChallenge();
        challenge.accessId = 901;
        challenge.challengeId = 6001;
        challenge.message = "CHALLENGE_ACTIVO_901";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        // Al sanar el estado pendiente, el coordinador exige firma con el sensor biométrico y valida con el servidor
        LoginResponseData.AuthTokens tokens = createTokens('1');
        api.loginAndroidCalls.add(TestCall.success(ApiResponse.success(tokens)));

        LoginResponseData profile = createProfile(1, "jair_test");
        api.getProfileCalls.add(TestCall.success(ApiResponse.success(profile)));

        coordinator.register("jair_test");

        assertTrue("El prompt biométrico debió haberse presentado para verificar posesión de clave",
                promptBridge.getPromptCallCount() > 0);
        assertNotNull("Debe autenticarse y confirmar la posesión de la clave", listener.authenticatedProfile);
        assertTrue("El estado local debe haber quedado sanado a confirmed=true", store.isEnabled());
        assertEquals(901, store.id());
    }

    /**
     * 8. Error de red durante el desafío.
     */
    @Test
    public void test08_ErrorDeRedDuranteElDesafio_ConservaCredencial() throws GeneralSecurityException {
        store.generateKey();
        store.save(902, "jair_test", true);

        // Simulamos desconexión de red / timeout
        api.challengeAndroidCalls.add(TestCall.failure(new IOException("Network timeout")));

        coordinator.unlock();

        assertEquals((Integer) R.string.auth_connection_error, listener.lastErrorMessage);
        assertTrue("Un error transitorio de red NUNCA debe borrar la credencial local", store.hasCredential());
        assertTrue("La credencial debe seguir activa", store.isEnabled());
        assertEquals(902, store.id());
    }

    /**
     * 9. Firma criptográfica incorrecta o inválida.
     */
    @Test
    public void test09_FirmaCriptograficaIncorrecta_RechazadaPorServidor() throws GeneralSecurityException {
        store.generateKey();
        store.save(903, "jair_test", true);

        AndroidAccessChallenge challenge = new AndroidAccessChallenge();
        challenge.accessId = 903;
        challenge.challengeId = 7001;
        challenge.message = "CHALLENGE_903";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        // El backend responde INVALID_SIGNATURE
        String errorJson = "{\"status\":\"error\",\"code\":401,\"error_code\":\"INVALID_SIGNATURE\",\"message\":\"Firma invalida\"}";
        api.loginAndroidCalls.add(TestCall.error(401, errorJson));

        coordinator.unlock();

        assertEquals((Integer) R.string.auth_android_signature_rejected, listener.lastErrorMessage);
        assertNull("No debe conceder sesión", listener.authenticatedProfile);
    }

    /**
     * 10. Desafío caducado o reutilizado.
     */
    @Test
    public void test10_DesafioCaducadoOReutilizado_Rechazado() throws GeneralSecurityException {
        store.generateKey();
        store.save(904, "jair_test", true);

        AndroidAccessChallenge challenge = new AndroidAccessChallenge();
        challenge.accessId = 904;
        challenge.challengeId = 7002;
        challenge.message = "CHALLENGE_EXPIRED";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        // El backend responde 401 INVALID_SIGNATURE por desafío expirado o reutilizado
        String errorJson = "{\"status\":\"error\",\"code\":401,\"error_code\":\"INVALID_SIGNATURE\",\"message\":\"Comprobacion vencida o utilizada\"}";
        api.loginAndroidCalls.add(TestCall.error(401, errorJson));

        coordinator.unlock();

        assertEquals((Integer) R.string.auth_android_signature_rejected, listener.lastErrorMessage);
        assertNull(listener.authenticatedProfile);
    }

    /**
     * 11. Intento de vincular una cuenta que ya tiene otro celular activo: HTTP 409 ACCOUNT_ALREADY_LINKED.
     */
    @Test
    public void test11_CuentaConOtroCelularActivo_Http409() {
        String errorJson = "{\"status\":\"error\",\"code\":409,\"error_code\":\"ACCOUNT_ALREADY_LINKED\",\"message\":\"Ya vinculado\"}";
        api.registerAndroidCalls.add(TestCall.error(409, errorJson));

        coordinator.register("jair_test");

        assertEquals((Integer) R.string.auth_android_account_already_linked, listener.lastErrorMessage);
        assertFalse("No debe guardarse vinculación local", store.hasCredential());
    }

    /**
     * 12. Cancelación de la ventana de huella.
     */
    @Test
    public void test12_CancelacionDeLaVentanaDeHuella() throws GeneralSecurityException {
        store.generateKey();
        store.save(905, "jair_test", true);

        AndroidAccessChallenge challenge = new AndroidAccessChallenge();
        challenge.accessId = 905;
        challenge.challengeId = 8001;
        challenge.message = "CHALLENGE_905";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        // Usuario cancela el diálogo de huella
        promptBridge.setAction(TestPromptBridge.Action.CANCEL);

        coordinator.unlock();

        assertEquals("Debe reportar cancelación de biometría",
                (Integer) R.string.auth_biometric_cancelled, listener.lastErrorMessage);
        assertNull("No debe conceder acceso", listener.authenticatedProfile);
        assertEquals("No debe haber enviado loginAndroid", 0, api.loginAndroidCalls.size());
    }

    /**
     * 13. Inicio de sesión biométrico después de cerrar sesión normalmente.
     */
    @Test
    public void test13_InicioDeSesionBiometricoDespuesDeCerrarSesion() throws GeneralSecurityException {
        // Estado: usuario cerró sesión en SessionManager pero mantiene vinculación
        store.generateKey();
        store.save(906, "jair_test", true);
        session.clearActiveSession();
        assertFalse("La sesión activa debe estar cerrada", session.isLoggedIn());

        AndroidAccessChallenge challenge = new AndroidAccessChallenge();
        challenge.accessId = 906;
        challenge.challengeId = 9001;
        challenge.message = "CHALLENGE_906";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        LoginResponseData.AuthTokens tokens = createTokens('c');
        api.loginAndroidCalls.add(TestCall.success(ApiResponse.success(tokens)));

        LoginResponseData profile = createProfile(1, "jair_test");
        api.getProfileCalls.add(TestCall.success(ApiResponse.success(profile)));

        coordinator.unlock();

        assertNotNull("Debe autenticarse exitosamente", listener.authenticatedProfile);
        assertEquals("jair_test", listener.authenticatedProfile.getUser().getUsername());
    }

    /**
     * 14. Recuperación sin borrar una clave o vinculación legítima (errores de servidor transitorios 500, 429).
     */
    @Test
    public void test14_RecuperacionSinBorrarClaveLegitima() throws GeneralSecurityException {
        store.generateKey();
        store.save(907, "jair_test", true);

        // Error transitorio 500
        api.challengeAndroidCalls.add(TestCall.error(500, "{\"status\":\"error\",\"code\":500,\"message\":\"Error interno\"}"));

        coordinator.unlock();

        assertEquals((Integer) R.string.auth_server_error, listener.lastErrorMessage);
        assertTrue("NUNCA debe borrar credencial por error 500", store.hasCredential());
        assertTrue(store.isEnabled());
        assertEquals(907, store.id());

        // Error transitorio 429 (Rate limiting)
        listener.reset();
        api.challengeAndroidCalls.add(TestCall.error(429, "{\"status\":\"error\",\"code\":429,\"message\":\"Espera un minuto\"}", "90"));

        coordinator.unlock();

        assertEquals((Integer) R.string.auth_rate_limit, listener.lastErrorMessage);
        assertEquals("90", listener.retryAfter);
        assertTrue("NUNCA debe borrar credencial por rate limiting 429", store.hasCredential());
        assertTrue(store.isEnabled());
    }

    /**
     * 15. Verificación de que NUNCA se concede acceso biométrico solamente por un estado local confirmed=true.
     */
    @Test
    public void test15_NuncaSeConcedeAccesoBiometricoSolamentePorConfirmedTrue() throws GeneralSecurityException {
        // Almacén local figura con confirmed=true
        store.generateKey();
        store.save(908, "jair_test", true);
        assertTrue("Almacén local figura confirmed=true", store.isEnabled());

        // El servidor rechaza el desafío (ej. 401 UNAUTHORIZED)
        api.challengeAndroidCalls.add(TestCall.error(401, "{\"status\":\"error\",\"code\":401,\"error_code\":\"UNAUTHORIZED\"}"));

        coordinator.unlock();

        assertNull("JAMÁS debe conceder acceso biométrico por tener confirmed=true local",
                listener.authenticatedProfile);
        assertNull("JAMÁS debe recibir tokens sin firma verificada en servidor",
                listener.authenticatedTokens);
        assertEquals((Integer) R.string.auth_android_session_rejected, listener.lastErrorMessage);
    }

    /**
     * 16. Verificación de fallo cuando la clave en Keystore fue invalidada o eliminada.
     */
    @Test
    public void test16_ClaveKeystoreInvalidaOMissing_NoSeConcedeAcceso() throws GeneralSecurityException {
        // Almacén con un CryptoProvider que falla (simulando KeyPermanentlyInvalidatedException)
        AndroidDeviceStore.CryptoProvider failingCrypto = new AndroidDeviceStore.CryptoProvider() {
            @Override public String generateKey() throws GeneralSecurityException { throw new GeneralSecurityException("Key invalidated"); }
            @Override public java.security.Signature signingOperation() throws GeneralSecurityException { throw new GeneralSecurityException("Key invalidated"); }
            @Override public void deleteKey() {}
        };
        AndroidDeviceStore failingStore = new AndroidDeviceStore(mockPrefs, failingCrypto);
        failingStore.save(909, "jair_test", true);

        AndroidBiometricCoordinator failingCoordinator = new AndroidBiometricCoordinator(
                listener, failingStore, session, api, promptBridge, () -> false, null, false);

        AndroidAccessChallenge challenge = new AndroidAccessChallenge();
        challenge.accessId = 909;
        challenge.challengeId = 9002;
        challenge.message = "CHALLENGE_909";
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        // Intento de register con clave invalidada en Keystore
        failingCoordinator.register("jair_test");

        assertFalse("JAMÁS debe considerar alreadyLinked si la clave de Keystore está invalidada o perdida",
                listener.alreadyLinkedCalled);
        assertEquals((Integer) R.string.auth_biometric_key_unavailable, listener.lastErrorMessage);

        // Intento de unlock con clave invalidada en Keystore
        listener.reset();
        api.challengeAndroidCalls.add(TestCall.success(ApiResponse.success(challenge)));

        failingCoordinator.unlock();

        assertNull("JAMÁS debe autenticar si la clave de Keystore está invalidada", listener.authenticatedProfile);
        assertEquals((Integer) R.string.auth_biometric_key_unavailable, listener.lastErrorMessage);
    }

    /**
     * 17. Fallo al limpiar preferencias en AndroidDeviceStore.clear():
     * Si prefs.commit() falla, NO debe eliminarse la clave criptográfica del Keystore
     * para evitar un estado huérfano inconsistente.
     */
    @Test
    public void test17_FalloAlLimpiarPreferencias_NoEliminaClaveKeystore() throws GeneralSecurityException {
        store.generateKey();
        store.save(777, "jair_test", true);
        assertTrue(store.hasCredential());
        assertTrue(store.isEnabled());
        assertNotNull("La clave debe existir antes de clear()", testCrypto.getKeyPair());

        // Simulamos fallo en SharedPreferences al remover/limpiar
        mockPrefs.setFailOnClear(true);

        boolean cleared = store.clear();

        assertFalse("store.clear() debe retornar false cuando falla commit()", cleared);
        assertTrue("store.hasCredential() debe preservarse para no quedar en estado corrupto", store.hasCredential());
        assertEquals(777, store.id());
        assertNotNull("La clave criptográfica NO debe borrarse si la persistencia falló", testCrypto.getKeyPair());
    }

    /**
     * 18. Dead link con fallo de limpieza local en el coordinador:
     * Si el servidor indica que el vínculo expiró (ej. DEVICE_NOT_FOUND) pero el borrado local
     * falla, el coordinador debe detenerse de forma segura sin registrar datos sobre almacenamiento sucio.
     */
    @Test
    public void test18_DeadLinkConFalloDeLimpiezaLocal_DetieneFlujoSinRegistroSucio() throws GeneralSecurityException {
        store.generateKey();
        store.save(666, "jair_test", true);

        // Servidor reporta que el dispositivo ya no existe (DEVICE_NOT_FOUND)
        String errorJson = "{\"status\":\"error\",\"code\":401,\"error_code\":\"DEVICE_NOT_FOUND\",\"message\":\"No existe\"}";
        api.challengeAndroidCalls.add(TestCall.error(401, errorJson));

        // Borrado local falla
        mockPrefs.setFailOnClear(true);

        coordinator.register("jair_test");

        assertEquals("Debe reportar fallo seguro al usuario",
                (Integer) R.string.auth_biometric_key_unavailable, listener.lastErrorMessage);
        assertFalse("NO debe llamar a linked()", listener.linkedCalled);
        assertTrue("NO debe llamar a api.registerAndroid si la limpieza falló", api.registerAndroidCalls.isEmpty());
        assertTrue("La credencial previa sigue intacta para no quedar huérfana", store.hasCredential());
    }

    /**
     * 19. Verificación de que el constructor por defecto de AndroidDeviceStore utiliza
     * AndroidKeyStoreCrypto y NO cae silenciosamente en SoftwareEcCrypto.
     */
    @Test
    public void test19_DefaultConstructorUsesAndroidKeyStoreCryptoNotSoftware() {
        // En entorno de pruebas JVM de escritorio (sin AndroidKeystore del OS),
        // el constructor por defecto debe intentar usar AndroidKeyStore y fallar
        // con GeneralSecurityException (o excepción derivada de KeyStore),
        // NUNCA generar claves silenciosas con SoftwareEcCrypto.
        AndroidDeviceStore defaultStore = new AndroidDeviceStore(mockPrefs);

        try {
            defaultStore.generateKey();
            fail("El constructor por defecto no debe generar claves de software en silencio");
        } catch (Exception expected) {
            // Se espera fallo de seguridad porque Keystore no está presente en la JVM host,
            // demostrando que NO hay fallback silencioso a SoftwareEcCrypto.
            assertTrue(expected instanceof GeneralSecurityException ||
                       expected.getCause() instanceof GeneralSecurityException ||
                       expected.getClass().getName().contains("KeyStore") ||
                       expected.getClass().getName().contains("Security"));
        }
    }

    // =========================================================================
    // PRUEBAS DE INFRAESTRUCTURA Y FORMATOS
    // =========================================================================

    @Test
    public void testDeviceStoreInitialState() {
        assertFalse("El almacén no debe tener credenciales al iniciar", store.hasCredential());
        assertFalse("El almacén no debe estar habilitado al iniciar", store.isEnabled());
        assertEquals(0, store.id());
        assertEquals("", store.username());
        assertNotNull(store.uid());
        assertFalse(store.uid().isEmpty());
    }

    @Test
    public void disabledFingerprintKeepsGuardianForFaceAndCanBeReenabled() throws GeneralSecurityException {
        String installation = store.uid();
        store.generateKey();
        store.save(71, "existing_user", true);
        assertTrue(store.disableBiometricLogin());
        assertFalse(store.isEnabled());
        assertTrue(store.isFingerprintDisabled());
        assertTrue(store.hasCredential());
        assertEquals(71, store.id());
        assertEquals("existing_user", store.username());
        assertEquals(installation, store.uid());
        assertNotNull(store.signingOperation());
        store.save(71, "existing_user", true);
        assertTrue(store.isEnabled());
        assertFalse(store.isFingerprintDisabled());
    }

    @Test
    public void testCoordinatorDeadLinkClassification() {
        assertTrue(AndroidBiometricCoordinator.isDeadLink("DEVICE_NOT_FOUND"));
        assertTrue(AndroidBiometricCoordinator.isDeadLink("DEVICE_REVOKED"));
        assertTrue(AndroidBiometricCoordinator.isDeadLink("REGISTRATION_INCOMPLETE"));

        assertFalse(AndroidBiometricCoordinator.isDeadLink("ACCOUNT_ALREADY_LINKED"));
        assertFalse(AndroidBiometricCoordinator.isDeadLink("DEVICE_NOT_AUTHORIZED"));
        assertFalse(AndroidBiometricCoordinator.isDeadLink("INVALID_SIGNATURE"));
        assertFalse(AndroidBiometricCoordinator.isDeadLink("TOO_MANY_ATTEMPTS"));
        assertFalse(AndroidBiometricCoordinator.isDeadLink("UNAUTHORIZED"));
        assertFalse(AndroidBiometricCoordinator.isDeadLink(null));
    }

    @Test
    public void testChallengeDeserialization() {
        String json = "{\"id_acceso\":42,\"challenge_id\":105,\"message\":\"WEBSITEPLUS_AUTH:LOGIN:1:nonce:NONE:2026-10-16 12:00:00\"}";
        AndroidAccessChallenge challenge = gson.fromJson(json, AndroidAccessChallenge.class);

        assertNotNull(challenge);
        assertEquals(42, challenge.accessId);
        assertEquals(105, challenge.challengeId);
        assertEquals("WEBSITEPLUS_AUTH:LOGIN:1:nonce:NONE:2026-10-16 12:00:00", challenge.message);
    }

    @Test
    public void testTokenValidationRules() {
        String valid64Hex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        assertTrue(valid64Hex.matches("[a-fA-F0-9]{64}"));

        String shortToken = "0123456789abcdef";
        assertFalse(shortToken.matches("[a-fA-F0-9]{64}"));

        String invalidChars = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdeg";
        assertFalse(invalidChars.matches("[a-fA-F0-9]{64}"));
    }

    @Test
    public void testLoginRequestSerialization() {
        LoginRequest req = new LoginRequest("test_user", "SecretPass123!");
        String json = gson.toJson(req);

        assertTrue(json.contains("\"username\":\"test_user\""));
        assertTrue(json.contains("\"password\":\"SecretPass123!\""));
    }
}
