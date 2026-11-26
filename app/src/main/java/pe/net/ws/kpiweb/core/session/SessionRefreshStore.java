package pe.net.ws.kpiweb.core.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Device-bound encrypted refresh token for a normal signed-in session. */
final class SessionRefreshStore {
    private static final String ALIAS = "sigd.session.refresh.v1";
    private static final String PREF = "sigd_session_refresh_v1";
    private static final String VALUE = "token";
    private final SharedPreferences prefs;

    SessionRefreshStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    boolean save(String token) {
        if (token == null || !token.matches("[a-fA-F0-9]{64}")) return false;
        try {
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            if (!store.containsAlias(ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256).build());
                generator.generateKey();
            }
            SecretKey key = (SecretKey) store.getKey(ALIAS, null);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] ciphertext = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] packed = new byte[cipher.getIV().length + ciphertext.length];
            System.arraycopy(cipher.getIV(), 0, packed, 0, cipher.getIV().length);
            System.arraycopy(ciphertext, 0, packed, cipher.getIV().length, ciphertext.length);
            return prefs.edit().putString(VALUE, Base64.encodeToString(packed, Base64.NO_WRAP)).commit();
        } catch (Exception error) { return false; }
    }

    String read() {
        try {
            byte[] packed = Base64.decode(prefs.getString(VALUE, ""), Base64.NO_WRAP);
            if (packed.length < 29) return "";
            KeyStore store = KeyStore.getInstance("AndroidKeyStore");
            store.load(null);
            SecretKey key = (SecretKey) store.getKey(ALIAS, null);
            if (key == null) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, packed, 0, 12));
            String token = new String(cipher.doFinal(packed, 12, packed.length - 12), StandardCharsets.UTF_8);
            return token.matches("[a-fA-F0-9]{64}") ? token : "";
        } catch (Exception error) { return ""; }
    }

    void clear() { prefs.edit().remove(VALUE).commit(); }
}
