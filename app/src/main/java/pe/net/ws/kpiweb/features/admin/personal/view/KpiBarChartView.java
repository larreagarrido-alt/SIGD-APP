package pe.net.ws.kpiweb.features.admin.personal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.admin.personal.model.EvaluationHistoryItem;

/**
 * Gráfico de barras mensual para el historial de indicadores KPI.
 * Barras con esquinas redondeadas y valores porcentuales superiores.
 */
public class KpiBarChartView extends View {

    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private final List<EvaluationHistoryItem> dataList = new ArrayList<>();

    public KpiBarChartView(Context context) {
        super(context);
        init();
    }

    public KpiBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KpiBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        baselinePaint.setColor(0xFF2A1A3C);
        baselinePaint.setStrokeWidth(1.5f * density);

        barPaint.setStyle(Paint.Style.FILL);

        valueTextPaint.setColor(0xFFF5EDFF);
        valueTextPaint.setTextSize(11f * density);
        valueTextPaint.setTextAlign(Paint.Align.CENTER);
        valueTextPaint.setFakeBoldText(true);

        labelTextPaint.setColor(0xFFB9A7D0);
        labelTextPaint.setTextSize(11f * density);
        labelTextPaint.setTextAlign(Paint.Align.CENTER);

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
            canvas.drawText("Aún no hay evaluaciones disponibles", w / 2f, h / 2f, emptyTextPaint);
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float padTop = 24f * density;
        float padBottom = 26f * density;
        float padSide = 20f * density;

        float chartH = h - padTop - padBottom;
        float chartW = w - (padSide * 2f);
        float baselineY = padTop + chartH;

        canvas.drawLine(padSide, baselineY, w - padSide, baselineY, baselinePaint);

        int count = dataList.size();
        float slotW = chartW / count;
        float barW = Math.min(28f * density, slotW * 0.5f);

        for (int i = 0; i < count; i++) {
            float cx = padSide + (i * slotW) + (slotW / 2f);
            int score = Math.max(0, Math.min(100, dataList.get(i).getScore()));
            float barHeight = chartH * (score / 100f);
            float topY = baselineY - barHeight;

            barRect.set(cx - barW / 2f, topY, cx + barW / 2f, baselineY);

            barPaint.setShader(new LinearGradient(cx, topY, cx, baselineY,
                    0xFFA855F7, 0xFF8738F5, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(barRect, 8f * density, 8f * density, barPaint);

            // Valor del puntaje
            canvas.drawText(score + "%", cx, topY - 6f * density, valueTextPaint);

            // Mes
            canvas.drawText(dataList.get(i).getPeriodLabel(), cx, h - 6f * density, labelTextPaint);
        }
    }
}
