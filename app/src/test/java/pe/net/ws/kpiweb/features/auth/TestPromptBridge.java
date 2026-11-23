package pe.net.ws.kpiweb.features.auth;

import java.security.Signature;
import pe.net.ws.kpiweb.features.auth.models.AndroidAccessChallenge;

public class TestPromptBridge implements AndroidBiometricCoordinator.PromptBridge {
    public enum Action {
        SUCCEED,
        CANCEL,
        FAILED
    }

    private Action action = Action.SUCCEED;
    private AndroidAccessChallenge lastChallenge;
    private boolean lastRegistration;
    private Signature lastSignature;
    private int promptCallCount = 0;
    private boolean cancelCalled = false;

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public void authenticate(AndroidAccessChallenge challenge, boolean registration, Signature signature, AndroidBiometricCoordinator.PromptCallback callback) {
        promptCallCount++;
        this.lastChallenge = challenge;
        this.lastRegistration = registration;
        this.lastSignature = signature;

        if (action == Action.SUCCEED) {
            callback.onSucceeded(signature);
        } else if (action == Action.CANCEL) {
            callback.onError(13, "Biometric prompt cancelled"); // ERROR_USER_CANCELED = 13
        } else if (action == Action.FAILED) {
            callback.onFailed();
        }
    }

    @Override
    public void cancel() {
        this.cancelCalled = true;
    }

    public int getPromptCallCount() { return promptCallCount; }
    public AndroidAccessChallenge getLastChallenge() { return lastChallenge; }
    public boolean isLastRegistration() { return lastRegistration; }
    public Signature getLastSignature() { return lastSignature; }
    public boolean isCancelCalled() { return cancelCalled; }
}
