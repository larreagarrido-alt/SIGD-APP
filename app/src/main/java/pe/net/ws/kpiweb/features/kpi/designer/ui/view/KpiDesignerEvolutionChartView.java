package pe.net.ws.kpiweb.features.kpi.designer.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerHistorySummary;

/**
 * Gráfico nativo en Canvas 2D de curva de evolución de cumplimiento semanal.
 * Muestra el progreso de las últimas 8 semanas (S1..S8) con degradado morado suave
 * y puntos destacados.
 */
public class KpiDesignerEvolutionChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<KpiDesignerHistorySummary.EvolutionPoint> dataPoints = new ArrayList<>();

    public KpiDesignerEvolutionChartView(Context context) {
        super(context);
        init();
    }

    public KpiDesignerEvolutionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KpiDesignerEvolutionChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_border));
        gridPaint.setStrokeWidth(dpToPx(1));

        axisTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_secondary));
        axisTextPaint.setTextSize(spToPx(10));
        axisTextPaint.setTextAlign(Paint.Align.RIGHT);

        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_primary)); // Morado Website+
        linePaint.setStrokeWidth(dpToPx(2.5f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        areaPaint.setStyle(Paint.Style.FILL);

        pointPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_primary));
        pointPaint.setStyle(Paint.Style.FILL);

        pointInnerPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_card_bg));
        pointInnerPaint.setStyle(Paint.Style.FILL);

        if (!new pe.net.ws.kpiweb.core.session.SessionManager(getContext()).isLoggedIn())
            setDefaultData();
    }

    public void setDefaultData() {
        dataPoints.clear();
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S1", 50));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S2", 75));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S3", 75));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S4", 88));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S5", 85));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S6", 85));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S7", 92));
        dataPoints.add(new KpiDesignerHistorySummary.EvolutionPoint("S8", 100));
        invalidate();
    }

    public void setDataPoints(List<KpiDesignerHistorySummary.EvolutionPoint> points) {
        dataPoints.clear();
        if (points != null) {
            dataPoints.addAll(points);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float leftMargin = dpToPx(34);
        float rightMargin = dpToPx(16);
        float topMargin = dpToPx(16);
        float bottomMargin = dpToPx(26);

        float chartWidth = width - leftMargin - rightMargin;
        float chartHeight = height - topMargin - bottomMargin;

        if (chartWidth <= 0 || chartHeight <= 0) return;

        // 1. Líneas de guía y etiquetas en Y (0%, 25%, 50%, 75%, 100%)
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            float y = topMargin + (chartHeight * (steps - i) / (float) steps);
            canvas.drawLine(leftMargin, y, width - rightMargin, y, gridPaint);

            int val = (100 * i) / steps;
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(val + "%", leftMargin - dpToPx(4), y + dpToPx(3), axisTextPaint);
        }

        if (dataPoints.size() < 2) return;

        // 2. Construir trayectorias de línea y área sombreada
        Path linePath = new Path();
        Path areaPath = new Path();

        float stepX = chartWidth / (float) (dataPoints.size() - 1);
        float groundY = topMargin + chartHeight;

        float[] pointXs = new float[dataPoints.size()];
        float[] pointYs = new float[dataPoints.size()];

        for (int i = 0; i < dataPoints.size(); i++) {
            KpiDesignerHistorySummary.EvolutionPoint p = dataPoints.get(i);
            float x = leftMargin + (i * stepX);
            float clampedPercent = Math.max(0, Math.min(100, p.percentage));
            float y = groundY - ((clampedPercent / 100f) * chartHeight);

            pointXs[i] = x;
            pointYs[i] = y;

            if (i == 0) {
                linePath.moveTo(x, y);
                areaPath.moveTo(x, groundY);
                areaPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                areaPath.lineTo(x, y);
            }
        }

        areaPath.lineTo(pointXs[pointXs.length - 1], groundY);
        areaPath.close();

        // Aplicar degradado al área
        LinearGradient gradient = new LinearGradient(0, topMargin, 0, groundY,
                Color.parseColor("#40C7D2FE"), Color.parseColor("#05C7D2FE"), Shader.TileMode.CLAMP);
        areaPaint.setShader(gradient);
        canvas.drawPath(areaPath, areaPaint);

        // Dibujar línea principal
        canvas.drawPath(linePath, linePaint);

        // 3. Dibujar puntos destacados y etiquetas en eje X
        for (int i = 0; i < dataPoints.size(); i++) {
            float x = pointXs[i];
            float y = pointYs[i];

            // Círculo exterior morado
            canvas.drawCircle(x, y, dpToPx(4), pointPaint);
            // Centro blanco
            canvas.drawCircle(x, y, dpToPx(2), pointInnerPaint);

            // Etiqueta eje X (S1, S2...)
            axisTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(dataPoints.get(i).label, x, groundY + dpToPx(14), axisTextPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
