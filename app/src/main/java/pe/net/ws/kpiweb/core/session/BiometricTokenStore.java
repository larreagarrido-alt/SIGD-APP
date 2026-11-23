package pe.net.ws.kpiweb.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/** Refresh token encrypted for one account on this device. Private key use requires biometrics. */
public final class BiometricTokenStore {
    private static final String ALIAS = "websiteplus.biometric.refresh.v1";
    private static final String PREFERENCES = "wsp_biometric_vault";
    private static final String TOKEN = "encrypted_refresh";
    private static final String USERNAME = "username";
    private static final OAEPParameterSpec OAEP = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
    private final SharedPreferences prefs;

    public BiometricTokenStore(Context context) {
        prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.contains(TOKEN);
    }

    public String getUsername() {
        return prefs.getString(USERNAME, "");
    }

    /** Called only after a successful password login and explicit opt-in. */
    public byte[] prepareEnrollment(String refreshToken) throws GeneralSecurityException {
        clear();
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(-1);
        }
        generator.initialize(builder.build());
        generator.generateKeyPair();
        return encrypt(refreshToken);
    }

    public byte[] encrypt(String refreshToken) throws GeneralSecurityException {
        if (refreshToken == null || !refreshToken.matches("[a-fA-F0-9]{64}")) {
            throw new GeneralSecurityException("Invalid refresh token");
        }
        KeyStore store = keyStore();
        if (store.getCertificate(ALIAS) == null) throw new GeneralSecurityException("Biometric key missing");
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        // Public-key encryption does not require a second prompt after rotating the refresh token.
        cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(store.getCertificate(ALIAS).getPublicKey().getEncoded())), OAEP);
        return cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
    }

    public Cipher decryptionCipher() throws GeneralSecurityException {
        KeyStore store = keyStore();
        PrivateKey key = (PrivateKey) store.getKey(ALIAS, null);
        if (key == null) throw new GeneralSecurityException("Biometric key missing");
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, OAEP);
        return cipher;
    }

    public byte[] encryptedToken() {
        return Base64.decode(prefs.getString(TOKEN, ""), Base64.NO_WRAP);
    }

    public void save(byte[] encrypted, String username) throws GeneralSecurityException {
        if (!prefs.edit().putString(TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(USERNAME, username).commit()) {
            throw new GeneralSecurityException("Cannot save biometric access");
        }
    }

    @android.annotation.SuppressLint("ApplySharedPref")
    public void clear() {
        // commit() is used intentionally to guarantee synchronous removal of biometric tokens.
        prefs.edit().clear().commit();
        try { keyStore().deleteEntry(ALIAS); } catch (GeneralSecurityException ignored) { }
    }

    private static KeyStore keyStore() throws GeneralSecurityException {
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            return store;
        } catch (java.io.IOException e) {
            throw new GeneralSecurityException(e);
        }
    }
}
