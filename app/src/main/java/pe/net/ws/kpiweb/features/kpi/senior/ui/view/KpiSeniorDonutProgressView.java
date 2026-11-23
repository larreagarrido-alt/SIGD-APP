package pe.net.ws.kpiweb.features.kpi.senior.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import pe.net.ws.kpiweb.R;

/**
 * Vista Canvas 2D nativa que renderiza el anillo circular de porcentaje de cumplimiento
 * para el reporte semanal del Diseñador Gráfico Senior.
 */
public class KpiSeniorDonutProgressView extends View {

    private float progress = 68.15f; // Porcentaje de 0 a 100
    private Paint trackPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private RectF arcBounds;
    private float strokeWidth = 24f;

    public KpiSeniorDonutProgressView(Context context) {
        super(context);
        init();
    }

    public KpiSeniorDonutProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KpiSeniorDonutProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_border));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#06B6D4")); // Color cian de la ficha

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_primary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        arcBounds = new RectF();
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(100f, progress));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = strokeWidth + 8f;
        arcBounds.set(padding, padding, w - padding, h - padding);
        textPaint.setTextSize(Math.min(w, h) * 0.20f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Dibujar pista base de 360 grados
        canvas.drawOval(arcBounds, trackPaint);

        // 2. Dibujar arco de progreso (comenzando arriba a -90 grados)
        float sweepAngle = (progress / 100f) * 360f;
        canvas.drawArc(arcBounds, -90, sweepAngle, false, progressPaint);

        // 3. Dibujar porcentaje de texto centrado
        String label = String.format(Locale.US, "%.2f%%", progress);
        float yPos = (getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(label, getWidth() / 2f, yPos, textPaint);
    }
}
