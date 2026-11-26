package pe.net.ws.kpiweb.features.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import pe.net.ws.kpiweb.R;

/**
 * Orchestrates the dynamic SIGD splash entrance and its seamless handoff into the Login screen.
 * Respects system animation-scale accessibility settings and cleans up all animators on destroy.
 */
final class SigdSplashController {

    private static final long HOLD_DURATION_MS = 1480L;
    private static final long MIN_TAP_SKIP_MS = 520L;

    private final View overlay;
    private final SigdSplashBackdropView backdrop;
    private final View halo;
    private final View splashLogo;
    private final View tagline;
    private final View subtitle;
    private final View loaderTrack;
    private final View loaderBeam;
    private final View footer;
    private final View loginScroll;
    private final View loginCard;
    private final View targetLoginLogo;
    private final float density;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AnimatorSet introSet;
    private AnimatorSet exitSet;
    private ObjectAnimator haloBreathX;
    private ObjectAnimator haloBreathY;
    private ObjectAnimator beamAnimator;
    private long startedAtMs;
    private boolean dismissed;

    SigdSplashController(View root) {
        this.overlay = root.findViewById(R.id.auth_splash_overlay);
        this.backdrop = root.findViewById(R.id.auth_splash_backdrop);
        this.halo = root.findViewById(R.id.auth_splash_halo);
        this.splashLogo = root.findViewById(R.id.auth_splash_logo);
        this.tagline = root.findViewById(R.id.auth_splash_tagline);
        this.subtitle = root.findViewById(R.id.auth_splash_subtitle);
        this.loaderTrack = root.findViewById(R.id.auth_splash_loader_track);
        this.loaderBeam = root.findViewById(R.id.auth_splash_loader_beam);
        this.footer = root.findViewById(R.id.auth_splash_footer);
        this.loginScroll = root.findViewById(R.id.login_scroll);
        this.loginCard = root.findViewById(R.id.card_login);
        this.targetLoginLogo = root.findViewById(R.id.iv_auth_logo);
        this.density = root.getResources().getDisplayMetrics().density;
    }

    static boolean animationsEnabled(Context context) {
        try {
            return Settings.Global.getFloat(
                    context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    void applyBottomInset(int bottomInsetPx) {
        if (footer == null) return;
        footer.setPadding(0, 0, 0, bottomInsetPx);
    }

    /** Plays the dynamic splash and hands off smoothly to the Login screen. Returns true if started. */
    boolean playIfEligible(boolean shouldPlay) {
        if (!shouldPlay || overlay == null || !animationsEnabled(overlay.getContext())) {
            if (overlay != null) overlay.setVisibility(View.GONE);
            return false;
        }
        dismissed = false;
        startedAtMs = System.currentTimeMillis();
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(1f);

        // Prepare login screen beneath for a silky reveal during handoff.
        if (loginScroll != null) {
            loginScroll.setAlpha(0f);
        }
        if (loginCard != null) {
            loginCard.setAlpha(0f);
            loginCard.setTranslationY(24f * density);
        }

        // Initial states for staggered splash choreography (starts visible & scaled like OS splash for zero blink).
        splashLogo.setAlpha(0.88f);
        splashLogo.setScaleX(0.70f);
        splashLogo.setScaleY(0.70f);
        splashLogo.setTranslationY(0f);

        halo.setAlpha(0f);
        halo.setScaleX(0.58f);
        halo.setScaleY(0.58f);

        tagline.setAlpha(0f);
        tagline.setTranslationY(16f * density);

        subtitle.setAlpha(0f);
        subtitle.setTranslationY(12f * density);

        loaderTrack.setAlpha(0f);
        footer.setAlpha(0f);
        footer.setTranslationY(8f * density);

        if (backdrop != null) {
            backdrop.post(() -> {
                if (dismissed || splashLogo.getHeight() <= 0) return;
                int[] bdLoc = new int[2];
                int[] logoLoc = new int[2];
                backdrop.getLocationInWindow(bdLoc);
                splashLogo.getLocationInWindow(logoLoc);
                backdrop.setFocalPoint(
                        (logoLoc[0] - bdLoc[0]) + splashLogo.getWidth() * 0.5f,
                        (logoLoc[1] - bdLoc[1]) + splashLogo.getHeight() * 0.5f
                );
            });
            backdrop.start();
        }

        // 1. Logo spring + luminous halo bloom
        AnimatorSet logoIn = new AnimatorSet();
        logoIn.playTogether(
                ObjectAnimator.ofFloat(splashLogo, View.ALPHA, 0.88f, 1f),
                ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, 0.70f, 1f),
                ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, 0.70f, 1f)
        );
        logoIn.setDuration(540L);
        logoIn.setInterpolator(new OvershootInterpolator(1.08f));

        AnimatorSet haloIn = new AnimatorSet();
        haloIn.playTogether(
                ObjectAnimator.ofFloat(halo, View.ALPHA, 0f, 0.92f),
                ObjectAnimator.ofFloat(halo, View.SCALE_X, 0.62f, 1.04f),
                ObjectAnimator.ofFloat(halo, View.SCALE_Y, 0.62f, 1.04f)
        );
        haloIn.setDuration(680L);
        haloIn.setInterpolator(new DecelerateInterpolator(1.6f));

        // 2. Tagline + subtitle + loader + footer staggered reveal
        AnimatorSet taglineIn = new AnimatorSet();
        taglineIn.playTogether(
                ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, 16f * density, 0f)
        );
        taglineIn.setStartDelay(240L);
        taglineIn.setDuration(420L);
        taglineIn.setInterpolator(new DecelerateInterpolator(1.8f));

        AnimatorSet subtitleIn = new AnimatorSet();
        subtitleIn.playTogether(
                ObjectAnimator.ofFloat(subtitle, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(subtitle, View.TRANSLATION_Y, 12f * density, 0f),
                ObjectAnimator.ofFloat(loaderTrack, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(footer, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(footer, View.TRANSLATION_Y, 8f * density, 0f)
        );
        subtitleIn.setStartDelay(360L);
        subtitleIn.setDuration(420L);
        subtitleIn.setInterpolator(new DecelerateInterpolator(1.8f));

        introSet = new AnimatorSet();
        introSet.playTogether(logoIn, haloIn, taglineIn, subtitleIn);
        introSet.start();

        // 3. Gentle halo breathing + glowing progress beam sweep
        haloBreathX = ObjectAnimator.ofFloat(halo, View.SCALE_X, 0.98f, 1.12f);
        haloBreathX.setStartDelay(650L);
        haloBreathX.setDuration(1100L);
        haloBreathX.setRepeatMode(ValueAnimator.REVERSE);
        haloBreathX.setRepeatCount(ValueAnimator.INFINITE);
        haloBreathX.setInterpolator(new AccelerateDecelerateInterpolator());
        haloBreathX.start();

        haloBreathY = ObjectAnimator.ofFloat(halo, View.SCALE_Y, 0.98f, 1.12f);
        haloBreathY.setStartDelay(650L);
        haloBreathY.setDuration(1100L);
        haloBreathY.setRepeatMode(ValueAnimator.REVERSE);
        haloBreathY.setRepeatCount(ValueAnimator.INFINITE);
        haloBreathY.setInterpolator(new AccelerateDecelerateInterpolator());
        haloBreathY.start();

        float startX = -56f * density;
        float endX = 124f * density;
        loaderBeam.setTranslationX(startX);
        beamAnimator = ObjectAnimator.ofFloat(loaderBeam, View.TRANSLATION_X, startX, endX);
        beamAnimator.setStartDelay(380L);
        beamAnimator.setDuration(920L);
        beamAnimator.setRepeatCount(ValueAnimator.INFINITE);
        beamAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        beamAnimator.start();

        // Allow optional gentle tap-to-continue once the entrance finishes.
        overlay.setOnClickListener(v -> {
            if (System.currentTimeMillis() - startedAtMs >= MIN_TAP_SKIP_MS) {
                transitionToLogin();
            }
        });

        handler.postDelayed(this::transitionToLogin, HOLD_DURATION_MS);
        return true;
    }

    private void transitionToLogin() {
        if (dismissed || overlay == null) return;
        dismissed = true;
        handler.removeCallbacksAndMessages(null);
        overlay.setOnClickListener(null);

        // Compute seamless upward lift toward the Login header logo when laid out.
        float logoDeltaY = -56f * density;
        float targetScale = 0.84f;
        if (targetLoginLogo != null && targetLoginLogo.getHeight() > 0 && splashLogo.getHeight() > 0) {
            int[] srcLoc = new int[2];
            int[] dstLoc = new int[2];
            splashLogo.getLocationInWindow(srcLoc);
            targetLoginLogo.getLocationInWindow(dstLoc);
            float srcCenterY = srcLoc[1] + splashLogo.getHeight() * 0.5f;
            float dstCenterY = dstLoc[1] + targetLoginLogo.getHeight() * 0.5f;
            logoDeltaY = dstCenterY - srcCenterY;
            targetScale = Math.max(0.72f, Math.min(1f,
                    (float) targetLoginLogo.getHeight() / (float) splashLogo.getHeight()));
        }

        AnimatorSet fadeTexts = new AnimatorSet();
        fadeTexts.playTogether(
                ObjectAnimator.ofFloat(tagline, View.ALPHA, tagline.getAlpha(), 0f),
                ObjectAnimator.ofFloat(subtitle, View.ALPHA, subtitle.getAlpha(), 0f),
                ObjectAnimator.ofFloat(loaderTrack, View.ALPHA, loaderTrack.getAlpha(), 0f),
                ObjectAnimator.ofFloat(footer, View.ALPHA, footer.getAlpha(), 0f),
                ObjectAnimator.ofFloat(halo, View.ALPHA, halo.getAlpha(), 0f)
        );
        fadeTexts.setDuration(220L);

        AnimatorSet liftLogoAndDissolve = new AnimatorSet();
        liftLogoAndDissolve.playTogether(
                ObjectAnimator.ofFloat(splashLogo, View.TRANSLATION_Y, splashLogo.getTranslationY(), logoDeltaY),
                ObjectAnimator.ofFloat(splashLogo, View.SCALE_X, splashLogo.getScaleX(), targetScale),
                ObjectAnimator.ofFloat(splashLogo, View.SCALE_Y, splashLogo.getScaleY(), targetScale),
                ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f)
        );
        liftLogoAndDissolve.setStartDelay(70L);
        liftLogoAndDissolve.setDuration(420L);
        liftLogoAndDissolve.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet revealLogin = new AnimatorSet();
        if (loginScroll != null && loginCard != null) {
            revealLogin.playTogether(
                    ObjectAnimator.ofFloat(loginScroll, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(loginCard, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(loginCard, View.TRANSLATION_Y, 24f * density, 0f)
            );
        } else if (loginScroll != null) {
            revealLogin.play(ObjectAnimator.ofFloat(loginScroll, View.ALPHA, 0f, 1f));
        }
        revealLogin.setStartDelay(110L);
        revealLogin.setDuration(420L);
        revealLogin.setInterpolator(new DecelerateInterpolator(1.8f));

        exitSet = new AnimatorSet();
        exitSet.playTogether(fadeTexts, liftLogoAndDissolve, revealLogin);
        exitSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishImmediately();
            }
        });
        exitSet.start();
    }

    private void finishImmediately() {
        stopLoopingAnimators();
        if (backdrop != null) backdrop.stop();
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
            overlay.setAlpha(1f);
        }
        if (loginScroll != null) loginScroll.setAlpha(1f);
        if (loginCard != null) {
            loginCard.setAlpha(1f);
            loginCard.setTranslationY(0f);
        }
    }

    private void stopLoopingAnimators() {
        if (haloBreathX != null) { haloBreathX.cancel(); haloBreathX = null; }
        if (haloBreathY != null) { haloBreathY.cancel(); haloBreathY = null; }
        if (beamAnimator != null) { beamAnimator.cancel(); beamAnimator = null; }
    }

    void cancel() {
        dismissed = true;
        handler.removeCallbacksAndMessages(null);
        if (introSet != null) { introSet.cancel(); introSet = null; }
        if (exitSet != null) { exitSet.cancel(); exitSet = null; }
        stopLoopingAnimators();
        if (backdrop != null) backdrop.stop();
    }
}
