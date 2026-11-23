package pe.net.ws.kpiweb.core.session;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

/** One installation, one account. Only a public key leaves Android Keystore. */
public final class AndroidDeviceStore {
    private static final String ALIAS = "websiteplus.android.login.v1";

    public interface CryptoProvider {
        String generateKey() throws GeneralSecurityException;
        Signature signingOperation() throws GeneralSecurityException;
        void deleteKey();
    }

    private final SharedPreferences prefs;
    private final CryptoProvider crypto;

    public AndroidDeviceStore(Context context) {
        this(context.getSharedPreferences("wsp_android_credential", Context.MODE_PRIVATE), new AndroidKeyStoreCrypto());
    }

    public AndroidDeviceStore(SharedPreferences prefs) {
        this(prefs, new AndroidKeyStoreCrypto());
    }

    public AndroidDeviceStore(SharedPreferences prefs, CryptoProvider crypto) {
        this.prefs = prefs;
        this.crypto = crypto != null ? crypto : new AndroidKeyStoreCrypto();
    }

    public static String encodeBase64(byte[] data) {
        try {
            return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
        } catch (Throwable e) {
            try {
                Class<?> b64 = Class.forName("java.util.Base64");
                Object encoder = b64.getMethod("getEncoder").invoke(null);
                return (String) encoder.getClass().getMethod("encodeToString", byte[].class).invoke(encoder, data);
            } catch (Exception ex) {
                throw new IllegalStateException("Base64 encoding not supported", ex);
            }
        }
    }

    public String uid() {
        String value = prefs.getString("uid", "");
        if (value.isEmpty()) {
            value = UUID.randomUUID().toString();
            if (!prefs.edit().putString("uid", value).commit()) throw new IllegalStateException("Cannot save installation");
        }
        return value;
    }

    public int id() { return prefs.getInt("id", 0); }
    public String username() { return prefs.getString("username", ""); }
    public boolean hasCredential() { return id() > 0; }
    public boolean isEnabled() { return hasCredential() && prefs.getBoolean("confirmed", false); }
    public boolean isFingerprintDisabled() { return hasCredential() && prefs.getBoolean("fingerprint_disabled", false); }

    /** Disables fingerprint login locally while retaining the guardian needed by a facial key. */
    public boolean disableBiometricLogin() {
        return prefs.edit().putBoolean("confirmed", false).putBoolean("fingerprint_disabled", true).commit();
    }

    public void save(int id, String username, boolean confirmed) throws GeneralSecurityException {
        if (id <= 0 || username == null || username.isEmpty() || !prefs.edit().putInt("id", id)
                .putString("username", username).putBoolean("confirmed", confirmed)
                .putBoolean("fingerprint_disabled", false).commit()) {
            throw new GeneralSecurityException("Cannot save device credential");
        }
    }

    public String generateKey() throws GeneralSecurityException {
        if (hasCredential()) throw new GeneralSecurityException("Existing credential must be released first");
        return crypto.generateKey();
    }

    public Signature signingOperation() throws GeneralSecurityException {
        return crypto.signingOperation();
    }

    @SuppressLint("ApplySharedPref")
    public boolean clear() {
        // Step 1: Remove credentials from SharedPreferences with synchronous commit.
        // We preserve "uid" so the hardware installation identity remains intact.
        boolean prefsCleared = prefs.edit().remove("id").remove("username").remove("confirmed")
                .remove("fingerprint_disabled").commit();
        if (!prefsCleared) {
            // SharedPreferences commit failed. Do NOT delete the cryptographic key to avoid leaving
            // the store in an inconsistent orphan state (prefs referencing a non-existent hardware key).
            return false;
        }
        // Step 2: Only after persistent references are safely wiped, delete the hardware key from Keystore.
        crypto.deleteKey();
        return true;
    }

    private static final class AndroidKeyStoreCrypto implements CryptoProvider {
        @Override
        public String generateKey() throws GeneralSecurityException {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            try { store.load(null); } catch (java.io.IOException e) { throw new GeneralSecurityException(e); }
            store.deleteEntry(ALIAS);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
            KeyGenParameterSpec.Builder spec = new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true);
            if (Build.VERSION.SDK_INT >= 30) spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG);
            else spec.setUserAuthenticationValidityDurationSeconds(-1);
            generator.initialize(spec.build());
            byte[] der = generator.generateKeyPair().getPublic().getEncoded();
            return "-----BEGIN PUBLIC KEY-----\n" + encodeBase64(der) + "\n-----END PUBLIC KEY-----\n";
        }

        @Override
        public Signature signingOperation() throws GeneralSecurityException {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            try { store.load(null); } catch (java.io.IOException e) { throw new GeneralSecurityException(e); }
            PrivateKey key = (PrivateKey) store.getKey(ALIAS, null);
            if (key == null) throw new GeneralSecurityException("Device key missing");
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(key);
            return signature;
        }

        @Override
        public void deleteKey() {
            try {
                KeyStore store = KeyStore.getInstance("AndroidKeyStore");
                store.load(null);
                store.deleteEntry(ALIAS);
            } catch (Exception ignored) { }
        }
    }

    public static final class SoftwareEcCrypto implements CryptoProvider {
        private KeyPair keyPair;

        public SoftwareEcCrypto() { }

        public SoftwareEcCrypto(KeyPair keyPair) {
            this.keyPair = keyPair;
        }

        @Override
        public String generateKey() throws GeneralSecurityException {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            keyPair = generator.generateKeyPair();
            byte[] der = keyPair.getPublic().getEncoded();
            return "-----BEGIN PUBLIC KEY-----\n" + encodeBase64(der) + "\n-----END PUBLIC KEY-----\n";
        }

        @Override
        public Signature signingOperation() throws GeneralSecurityException {
            if (keyPair == null || keyPair.getPrivate() == null) {
                throw new GeneralSecurityException("Device key missing");
            }
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(keyPair.getPrivate());
            return signature;
        }

        @Override
        public void deleteKey() {
            keyPair = null;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }
    }
}
