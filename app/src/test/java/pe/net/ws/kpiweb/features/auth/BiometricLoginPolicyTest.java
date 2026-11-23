package pe.net.ws.kpiweb.features.auth;

import static org.junit.Assert.*;

import org.junit.Test;
import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;
import pe.net.ws.kpiweb.core.session.SessionManager;

public class BiometricLoginPolicyTest {
    @Test public void confirmedChoiceSurvivesRestartWithoutCreatingSession() throws Exception {
        MockSharedPreferences credentials=new MockSharedPreferences();
        AndroidDeviceStore.SoftwareEcCrypto key=new AndroidDeviceStore.SoftwareEcCrypto();
        AndroidDeviceStore first=new AndroidDeviceStore(credentials,key);
        first.generateKey();
        first.save(41,"worker_a",true);
        AndroidDeviceStore reopened=new AndroidDeviceStore(credentials,key);
        assertTrue(BiometricLoginPolicy.linkedFor(reopened,"worker_a",true,true));
        assertFalse(BiometricLoginPolicy.needsRegistration(reopened,"worker_a",true,true));
        assertFalse(new SessionManager(new MockSharedPreferences()).isLoggedIn());
    }

    @Test public void pendingOtherAccountAndInvalidKeyNeverLookLinked() throws Exception {
        AndroidDeviceStore store=new AndroidDeviceStore(new MockSharedPreferences(),new AndroidDeviceStore.SoftwareEcCrypto());
        store.save(42,"worker_a",false);
        assertFalse(BiometricLoginPolicy.linkedFor(store,"worker_a",true,true));
        assertTrue(BiometricLoginPolicy.needsRegistration(store,"worker_a",true,false));
        store.save(42,"worker_a",true);
        assertFalse(BiometricLoginPolicy.linkedFor(store,"worker_b",true,true));
        assertFalse(BiometricLoginPolicy.needsRegistration(store,"worker_b",true,false));
        assertFalse(BiometricLoginPolicy.linkedFor(store,"worker_a",false,true));
        assertFalse(BiometricLoginPolicy.linkedFor(store,"worker_a",true,false));
        assertTrue(store.clear());
        assertFalse(store.isEnabled());
    }

    @Test public void disabledFingerprintDoesNotRegisterAgainOrBlockGuardian() throws Exception {
        AndroidDeviceStore store=new AndroidDeviceStore(new MockSharedPreferences(),new AndroidDeviceStore.SoftwareEcCrypto());
        store.generateKey();
        store.save(43,"worker_a",true);
        assertTrue(store.disableBiometricLogin());
        assertTrue(store.hasCredential());
        assertFalse(BiometricLoginPolicy.linkedFor(store,"worker_a",true,true));
        assertFalse(BiometricLoginPolicy.needsRegistration(store,"worker_a",true,false));
    }
}
