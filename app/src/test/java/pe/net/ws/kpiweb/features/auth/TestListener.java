package pe.net.ws.kpiweb.features.auth;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;

public class TestListener implements AndroidBiometricCoordinator.Listener {
    public boolean loading = false;
    public Integer lastErrorMessage = null;
    public String retryAfter;
    public int errorCallCount = 0;
    public boolean linkedCalled = false;
    public boolean alreadyLinkedCalled = false;
    public boolean forgottenCalled = false;
    public LoginResponseData authenticatedProfile = null;
    public LoginResponseData.AuthTokens authenticatedTokens = null;

    @Override
    public void loading(boolean value) {
        this.loading = value;
    }

    @Override
    public void error(int message) {
        this.lastErrorMessage = message;
        this.errorCallCount++;
    }
    public void rateLimited(String header) {retryAfter=header;error(R.string.auth_rate_limit);}

    @Override
    public void linked() {
        this.linkedCalled = true;
    }

    @Override
    public void alreadyLinked() {
        this.alreadyLinkedCalled = true;
    }

    @Override
    public void forgotten() {
        this.forgottenCalled = true;
    }

    @Override
    public void authenticated(LoginResponseData profile, LoginResponseData.AuthTokens tokens) {
        this.authenticatedProfile = profile;
        this.authenticatedTokens = tokens;
    }

    public void reset() {
        loading = false;
        lastErrorMessage = null;
        retryAfter = null;
        errorCallCount = 0;
        linkedCalled = false;
        alreadyLinkedCalled = false;
        forgottenCalled = false;
        authenticatedProfile = null;
        authenticatedTokens = null;
    }
}
