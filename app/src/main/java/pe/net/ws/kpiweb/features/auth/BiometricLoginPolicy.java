package pe.net.ws.kpiweb.features.auth;

import pe.net.ws.kpiweb.core.session.AndroidDeviceStore;

/** Local UI/registration policy. A saved choice never creates an authenticated session. */
final class BiometricLoginPolicy {
    private BiometricLoginPolicy() {}

    static boolean sameAccount(AndroidDeviceStore store, String username) {
        return username != null && !username.isEmpty() && username.equals(store.username());
    }

    static boolean linkedFor(AndroidDeviceStore store, String username,
                             boolean keyUsable, boolean systemBiometricsReady) {
        return store.isEnabled() && sameAccount(store, username)
                && keyUsable && systemBiometricsReady;
    }

    static boolean needsRegistration(AndroidDeviceStore store, String username,
                                     boolean selected, boolean linked) {
        return selected && !linked && !store.isFingerprintDisabled()
                && (!store.hasCredential() || sameAccount(store, username));
    }
}
