package pe.net.ws.kpiweb.features.auth;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Animated corporate-violet aurora, expanding biometric-style pulse rings and subtle 4-point
 * brand sparkles behind the SIGD splash logo.
 */
public final class SigdSplashBackdropView extends View {

    private static final class Particle {
        final float xFrac;
        final float yFrac;
        final float radiusDp;
        final float speed;
        final float phase;
        final boolean star;

        Particle(float xFrac, float yFrac, float radiusDp, float speed, float phase, boolean star) {
            this.xFrac = xFrac;
            this.yFrac = yFrac;
            this.radiusDp = radiusDp;
            this.speed = speed;
            this.phase = phase;
            this.star = star;
        }
    }

    // Deterministic starfield & dust layout (balanced around the central SIGD logo).
    private static final Particle[] PARTICLES = new Particle[] {
            new Particle(0.16f, 0.24f, 4.6f, 0.22f, 0.0f, true),
            new Particle(0.84f, 0.20f, 5.2f, 0.18f, 1.3f, true),
            new Particle(0.76f, 0.56f, 4.0f, 0.25f, 2.7f, true),
            new Particle(0.20f, 0.66f, 4.4f, 0.20f, 4.1f, true),
            new Particle(0.54f, 0.16f, 3.6f, 0.24f, 5.0f, true),
            new Particle(0.10f, 0.42f, 1.7f, 0.30f, 0.5f, false),
            new Particle(0.28f, 0.34f, 1.4f, 0.26f, 1.9f, false),
            new Particle(0.38f, 0.74f, 1.8f, 0.28f, 2.4f, false),
            new Particle(0.64f, 0.28f, 1.5f, 0.32f, 3.3f, false),
            new Particle(0.88f, 0.44f, 1.9f, 0.24f, 4.4f, false),
            new Particle(0.72f, 0.72f, 1.6f, 0.29f, 5.5f, false),
            new Particle(0.14f, 0.80f, 1.5f, 0.23f, 1.1f, false),
            new Particle(0.86f, 0.78f, 1.7f, 0.27f, 3.9f, false),
            new Particle(0.45f, 0.84f, 1.4f, 0.31f, 0.8f, false),
            new Particle(0.31f, 0.18f, 1.5f, 0.25f, 2.1f, false),
            new Particle(0.68f, 0.14f, 1.3f, 0.29f, 4.8f, false)
    };

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path starPath = new Path();

    private Shader baseGradient;
    private float density;
    private float loopPhase;
    private float introProgress;
    private float focalX = -1f;
    private float focalY = -1f;
    private ValueAnimator loopAnimator;
    private ValueAnimator introAnimator;

    public void setFocalPoint(float x, float y) {
        if (x > 0f && y > 0f) {
            this.focalX = x;
            this.focalY = y;
            invalidate();
        }
    }

    public SigdSplashBackdropView(Context context) {
        this(context, null);
    }

    public SigdSplashBackdropView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SigdSplashBackdropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.35f * density);
        particlePaint.setStyle(Paint.Style.FILL);
    }

    public void start() {
        stop();
        introAnimator = ValueAnimator.ofFloat(0f, 1f);
        introAnimator.setDuration(1550L);
        introAnimator.setInterpolator(new DecelerateInterpolator(1.4f));
        introAnimator.addUpdateListener(anim -> {
            introProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        introAnimator.start();

        loopAnimator = ValueAnimator.ofFloat(0f, 1f);
        loopAnimator.setDuration(5200L);
        loopAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loopAnimator.setInterpolator(new LinearInterpolator());
        loopAnimator.addUpdateListener(anim -> {
            loopPhase = (float) anim.getAnimatedValue();
            invalidate();
        });
        loopAnimator.start();
    }

    public void stop() {
        if (introAnimator != null) {
            introAnimator.cancel();
            introAnimator = null;
        }
        if (loopAnimator != null) {
            loopAnimator.cancel();
            loopAnimator = null;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        // Seamless match with @color/auth_background (#21083B) -> rich center -> @color/auth_background_bottom (#1B0631).
        baseGradient = new LinearGradient(
                0f, 0f, 0f, h,
                new int[] { 0xFF21083B, 0xFF2E0752, 0xFF1B0631 },
                new float[] { 0f, 0.48f, 1f },
                Shader.TileMode.CLAMP
        );
        basePaint.setShader(baseGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Deep nocturnal corporate violet base
        if (baseGradient != null) {
            canvas.drawRect(0f, 0f, w, h, basePaint);
        }

        double angle = loopPhase * Math.PI * 2.0;
        float cx = focalX > 0f ? focalX : w * 0.5f;
        float logoY = focalY > 0f ? focalY : h * 0.41f;

        // 2. Three living aurora light orbs drifting softly
        float orb1X = cx + (float) Math.cos(angle) * (w * 0.20f);
        float orb1Y = h * 0.24f + (float) Math.sin(angle * 0.8) * (h * 0.05f);
        drawOrb(canvas, orb1X, orb1Y, w * 0.62f, Color.argb(92, 168, 44, 240), Color.argb(0, 149, 23, 213));

        float orb2X = cx - (float) Math.cos(angle + 1.4) * (w * 0.24f);
        float orb2Y = h * 0.72f + (float) Math.sin(angle + 1.0) * (h * 0.06f);
        drawOrb(canvas, orb2X, orb2Y, w * 0.58f, Color.argb(74, 114, 9, 183), Color.argb(0, 33, 8, 59));

        float pulseScale = 0.85f + 0.22f * introProgress + 0.05f * (float) Math.sin(angle * 2.0);
        int centerAlpha = Math.round(108 * Math.min(1f, introProgress * 1.4f));
        drawOrb(canvas, cx, logoY, w * 0.44f * pulseScale,
                Color.argb(centerAlpha, 194, 92, 255), Color.argb(0, 114, 9, 183));

        // 3. Two staggered luminous expanding rings centered behind the SIGD mark
        drawPulseRing(canvas, cx, logoY, introProgress, 0.05f, 0.78f, 52f * density, 168f * density, 130);
        drawPulseRing(canvas, cx, logoY, introProgress, 0.22f, 0.96f, 56f * density, 198f * density, 95);

        // 4. Floating stardust & 4-point brand sparkles
        float fadeIn = Math.min(1f, introProgress * 1.8f);
        for (Particle p : PARTICLES) {
            float progressY = (p.yFrac - loopPhase * p.speed + 1f) % 1f;
            float px = w * p.xFrac + (float) Math.sin(angle + p.phase) * (8f * density);
            float py = h * progressY;
            float twinkle = 0.45f + 0.55f * (float) Math.sin(angle * 2.0 + p.phase);
            int alpha = Math.round(fadeIn * twinkle * (p.star ? 210 : 125));
            if (alpha <= 4) continue;
            if (p.star) {
                particlePaint.setColor(Color.argb(alpha, 245, 224, 255));
                drawFourPointStar(canvas, px, py, p.radiusDp * density * (0.82f + 0.28f * twinkle));
            } else {
                particlePaint.setColor(Color.argb(alpha, 214, 174, 255));
                canvas.drawCircle(px, py, p.radiusDp * density, particlePaint);
            }
        }
    }

    private void drawOrb(Canvas canvas, float cx, float cy, float radius, int startColor, int endColor) {
        if (radius <= 1f) return;
        orbPaint.setShader(new RadialGradient(cx, cy, radius, startColor, endColor, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, orbPaint);
    }

    private void drawPulseRing(Canvas canvas, float cx, float cy, float progress,
                               float startAt, float endAt, float minR, float maxR, int maxAlpha) {
        if (progress <= startAt || progress >= endAt) return;
        float t = (progress - startAt) / (endAt - startAt);
        float eased = 1f - (1f - t) * (1f - t) * (1f - t);
        float radius = minR + (maxR - minR) * eased;
        // Fade in quickly, then dissolve smoothly as the ring expands.
        float alphaFactor = t < 0.18f ? (t / 0.18f) : (1f - (t - 0.18f) / 0.82f);
        int alpha = Math.max(0, Math.min(255, Math.round(maxAlpha * alphaFactor * alphaFactor)));
        if (alpha <= 2) return;
        ringPaint.setColor(Color.argb(alpha, 232, 198, 255));
        canvas.drawCircle(cx, cy, radius, ringPaint);
    }

    private void drawFourPointStar(Canvas canvas, float cx, float cy, float r) {
        float inner = r * 0.24f;
        starPath.reset();
        starPath.moveTo(cx, cy - r);
        starPath.quadTo(cx + inner, cy - inner, cx + r, cy);
        starPath.quadTo(cx + inner, cy + inner, cx, cy + r);
        starPath.quadTo(cx - inner, cy + inner, cx - r, cy);
        starPath.quadTo(cx - inner, cy - inner, cx, cy - r);
        starPath.close();
        canvas.drawPath(starPath, particlePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
