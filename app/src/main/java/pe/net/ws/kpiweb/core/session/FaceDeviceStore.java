package pe.net.ws.kpiweb.core.session;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.nio.charset.StandardCharsets;

/** Separate hardware device key for the scoped pilot. This key does not attest PAD. V011 is untouched. */
public final class FaceDeviceStore {
    private final Context context;
    private final SharedPreferences prefs;
    private final String alias;
    public FaceDeviceStore(Context context){this(context,context.getSharedPreferences("wsp_face_device",Context.MODE_PRIVATE),"wsp_face_pilot_v1");}
    public FaceDeviceStore(Context context,SharedPreferences prefs,String alias){
        if(!alias.startsWith("wsp_face_"))throw new IllegalArgumentException("Separate face alias required");
        this.context=context;this.prefs=prefs;this.alias=alias;
    }
    private KeyStore keys() throws GeneralSecurityException {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");try{ks.load(null);}catch(Exception e){throw new GeneralSecurityException(e);}return ks;
    }
    private String activeAlias(){return prefs.getString("key_alias",alias);}
    private PrivateKey checkedKey(String selected) throws GeneralSecurityException {
        PrivateKey key=(PrivateKey)keys().getKey(selected,null);if(key==null)throw new GeneralSecurityException("Face key missing");
        KeyInfo info=KeyFactory.getInstance("EC","AndroidKeyStore").getKeySpec(key,KeyInfo.class);
        boolean hardware;
        if(Build.VERSION.SDK_INT>=31){int level=info.getSecurityLevel();hardware=level==KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT||level==KeyProperties.SECURITY_LEVEL_STRONGBOX;}
        else hardware=info.isInsideSecureHardware();
        if(!hardware)throw new GeneralSecurityException("Hardware key required");
        if(info.isUserAuthenticationRequired())throw new GeneralSecurityException("Authorize a new direct-pilot facial key from account security");
        return key;
    }
    public String prepare() throws GeneralSecurityException {return prepare(false);}
    /** Rotation must be explicitly requested in the authenticated security screen. Old active key survives until confirm. */
    public String prepare(boolean rotate) throws GeneralSecurityException {
        KeyguardManager lock=(KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        if(lock==null||!lock.isDeviceSecure())throw new GeneralSecurityException("Secure screen lock required");
        String selected=prefs.getString("candidate_alias",activeAlias());
        if((rotate||prefs.getBoolean("needs_rotation",false))&&!prefs.contains("candidate_alias")){selected=alias+"_"+java.util.UUID.randomUUID();
            if(!prefs.edit().putString("candidate_alias",selected).commit())throw new GeneralSecurityException("Cannot persist rotation candidate");}
        KeyStore ks=keys();
        if(!ks.containsAlias(selected)){
            KeyGenParameterSpec.Builder spec=new KeyGenParameterSpec.Builder(selected,KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256).setUserAuthenticationRequired(false);
            // Android 12-14 have documented unlocked-device key bugs; enforce lock state below on every sign.
            if(Build.VERSION.SDK_INT>=35)spec.setUnlockedDeviceRequired(true);
            KeyPairGenerator generator=KeyPairGenerator.getInstance("EC","AndroidKeyStore");generator.initialize(spec.build());generator.generateKeyPair();
        }
        checkedKey(selected);byte[] der=keys().getCertificate(selected).getPublicKey().getEncoded();
        return "-----BEGIN PUBLIC KEY-----\n"+AndroidDeviceStore.encodeBase64(der)+"\n-----END PUBLIC KEY-----\n";
    }
    public String sign(String message) throws GeneralSecurityException {
        requireUnlockedDevice();
        Signature op=Signature.getInstance("SHA256withECDSA");op.initSign(checkedKey(activeAlias()));op.update(message.getBytes(StandardCharsets.UTF_8));return AndroidDeviceStore.encodeBase64(op.sign());
    }
    public static String proofMessage(String challenge,String payload) throws GeneralSecurityException {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();
        for(byte b:hash)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return challenge+"\nFACE_PAYLOAD_SHA256:"+hex;
    }
    private void requireUnlockedDevice() throws GeneralSecurityException {
        KeyguardManager lock=(KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE);
        if(lock==null||!lock.isDeviceSecure()||lock.isDeviceLocked()||lock.isKeyguardLocked())throw new GeneralSecurityException("Unlocked secure device required");
    }
    public void checkDirectAccess() throws GeneralSecurityException {requireUnlockedDevice();checkedKey(activeAlias());}
    /** Compatibility for isolated test tools; this never prompts for a credential. */
    public void checkRecentUnlock() throws GeneralSecurityException {checkDirectAccess();}
    public long id(){return prefs.getLong("id",0);}
    /** Clear the facial binding and its separate key after confirmed server revocation. V011 is untouched. */
    public void clearBinding() throws GeneralSecurityException {
        String active=activeAlias();
        String candidate=prefs.getString("candidate_alias",null);
        if(!prefs.edit().remove("id").remove("android_id").remove("username")
                .remove("key_alias").remove("candidate_alias").putBoolean("needs_rotation",true).commit())
            throw new GeneralSecurityException("Cannot clear facial binding");
        KeyStore store=keys();
        if(store.containsAlias(active))store.deleteEntry(active);
        if(candidate!=null&&!candidate.equals(active)&&store.containsAlias(candidate))store.deleteEntry(candidate);
    }
    public boolean matches(AndroidDeviceStore guardian){return id()>0&&prefs.getInt("android_id",0)==guardian.id()&&prefs.getString("username","").equals(guardian.username());}
    public void save(long id,AndroidDeviceStore guardian) throws GeneralSecurityException {
        if(id<=0||!prefs.edit().putLong("id",id).putInt("android_id",guardian.id()).putString("username",guardian.username())
            .putString("key_alias",prefs.getString("candidate_alias",activeAlias())).remove("candidate_alias").remove("needs_rotation").commit())throw new GeneralSecurityException("Cannot save face binding");
    }
}
