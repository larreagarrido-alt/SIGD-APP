package pe.net.ws.kpiweb.features.admin.personal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.admin.personal.model.EvaluationHistoryItem;

/**
 * Gráfico de líneas moderno con relleno degradado y puntos con brillo.
 * Diseñado según los lineamientos dark premium de WEBSITE.
 */
public class PerformanceLineChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textScorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<EvaluationHistoryItem> dataList = new ArrayList<>();
    private final Path pathLine = new Path();
    private final Path pathFill = new Path();

    public PerformanceLineChartView(Context context) {
        super(context);
        init();
    }

    public PerformanceLineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PerformanceLineChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        gridPaint.setColor(0xFF2A1A3C);
        gridPaint.setStrokeWidth(1.2f * density);
        gridPaint.setStyle(Paint.Style.STROKE);

        linePaint.setColor(0xFFBB75FF);
        linePaint.setStrokeWidth(3f * density);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setPathEffect(new CornerPathEffect(16f * density));

        fillPaint.setStyle(Paint.Style.FILL);

        dotOuterPaint.setColor(0xFF8738F5);
        dotOuterPaint.setStyle(Paint.Style.FILL);

        dotInnerPaint.setColor(0xFFFFFFFF);
        dotInnerPaint.setStyle(Paint.Style.FILL);

        textScorePaint.setColor(0xFFF5EDFF);
        textScorePaint.setTextSize(12f * density);
        textScorePaint.setTextAlign(Paint.Align.CENTER);
        textScorePaint.setFakeBoldText(true);

        textLabelPaint.setColor(0xFFB9A7D0);
        textLabelPaint.setTextSize(12f * density);
        textLabelPaint.setTextAlign(Paint.Align.CENTER);

        emptyTextPaint.setColor(0xFF836F9C);
        emptyTextPaint.setTextSize(13f * density);
        emptyTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<EvaluationHistoryItem> items) {
        dataList.clear();
        if (items != null) {
            dataList.addAll(items);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) return;

        if (dataList.isEmpty()) {
            canvas.drawText("Aún no hay evaluaciones registradas", w / 2f, h / 2f, emptyTextPaint);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float padTop = 26f * density;
        float padBottom = 26f * density;
        float padLeft = 24f * density;
        float padRight = 24f * density;

        float chartH = h - padTop - padBottom;
        float chartW = w - padLeft - padRight;

        // 1. Líneas de guía horizontales
        for (int i = 0; i <= 3; i++) {
            float y = padTop + (chartH * (i / 3f));
            canvas.drawLine(padLeft, y, w - padRight, y, gridPaint);
        }

        int count = dataList.size();
        float stepX = count > 1 ? chartW / (count - 1) : chartW / 2f;

        float[] pointX = new float[count];
        float[] pointY = new float[count];

        pathLine.reset();
        pathFill.reset();

        for (int i = 0; i < count; i++) {
            float x = count > 1 ? (padLeft + i * stepX) : (w / 2f);
            int score = Math.max(0, Math.min(100, dataList.get(i).getScore()));
            // Escala entre 60% y 100% para mejor visualización dinámica
            float ratio = (score - 60f) / 40f;
            ratio = Math.max(0.1f, Math.min(1.0f, ratio));
            float y = padTop + chartH * (1.0f - ratio);

            pointX[i] = x;
            pointY[i] = y;

            if (i == 0) {
                pathLine.moveTo(x, y);
                pathFill.moveTo(x, padTop + chartH);
                pathFill.lineTo(x, y);
            } else {
                pathLine.lineTo(x, y);
                pathFill.lineTo(x, y);
            }
        }

        if (count > 0) {
            pathFill.lineTo(pointX[count - 1], padTop + chartH);
            pathFill.close();

            // Sombra / Gradiente bajo la línea
            fillPaint.setShader(new LinearGradient(0, padTop, 0, padTop + chartH,
                    0x408738F5, 0x008738F5, Shader.TileMode.CLAMP));
            canvas.drawPath(pathFill, fillPaint);
            canvas.drawPath(pathLine, linePaint);
        }

        // 2. Puntos y etiquetas
        for (int i = 0; i < count; i++) {
            float x = pointX[i];
            float y = pointY[i];

            // Anillo exterior y punto central
            canvas.drawCircle(x, y, 7f * density, dotOuterPaint);
            canvas.drawCircle(x, y, 3.5f * density, dotInnerPaint);

            // Valor del puntaje arriba
            String scoreStr = dataList.get(i).getScore() + "%";
            canvas.drawText(scoreStr, x, y - 10f * density, textScorePaint);

            // Etiqueta del período abajo
            String period = dataList.get(i).getPeriodLabel();
            canvas.drawText(period, x, h - 6f * density, textLabelPaint);
        }
    }
}
