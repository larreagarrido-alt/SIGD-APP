package pe.net.ws.kpiweb.features.admin.personal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Anillo circular de rendimiento estilo dark premium.
 * Muestra el porcentaje y subtítulo centrado.
 * Si el puntaje es negativo (-1), indica estado pendiente de evaluación.
 */
public class CircularProgressView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textSubtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private int progress = 94; // 0 - 100 o -1 para pendiente
    private String subtitle = "de avance";
    private float strokeWidthPx = 28f;

    public CircularProgressView(Context context) {
        super(context);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        strokeWidthPx = 10f * density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidthPx);
        trackPaint.setColor(0xFF251733);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidthPx);
        progressPaint.setColor(0xFF8738F5);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textValuePaint.setColor(0xFFF5EDFF);
        textValuePaint.setTextSize(26f * density);
        textValuePaint.setTextAlign(Paint.Align.CENTER);
        textValuePaint.setFakeBoldText(true);

        textSubtitlePaint.setColor(0xFFB9A7D0);
        textSubtitlePaint.setTextSize(11f * density);
        textSubtitlePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(int score, String sub) {
        this.progress = score;
        if (sub != null) {
            this.subtitle = sub;
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = strokeWidthPx / 2f + 4f;
        oval.set(pad, pad, w - pad, h - pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - strokeWidthPx / 2f - 4f;

        if (radius <= 0) return;

        // 1. Pista de fondo
        canvas.drawCircle(cx, cy, radius, trackPaint);

        // 2. Arco de progreso
        if (progress >= 0) {
            float sweepAngle = 360f * (Math.min(100, Math.max(0, progress)) / 100f);
            canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint);

            // Textos centrados
            String valStr = progress + "%";
            canvas.drawText(valStr, cx, cy + (textValuePaint.getTextSize() * 0.15f), textValuePaint);
            canvas.drawText(subtitle, cx, cy + (textValuePaint.getTextSize() * 0.75f), textSubtitlePaint);
        } else {
            // Estado pendiente
            canvas.drawText("--", cx, cy + (textValuePaint.getTextSize() * 0.15f), textValuePaint);
            canvas.drawText("Pendiente", cx, cy + (textValuePaint.getTextSize() * 0.75f), textSubtitlePaint);
        }
    }
}
