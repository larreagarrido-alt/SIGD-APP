package pe.net.ws.kpiweb.features.kpi.junior.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;

/**
 * Vista personalizada nativa que dibuja el gráfico de barras agrupadas
 * de producción semanal (Posts en Morado y Videos en Azul) para cada día de la semana.
 */
public class KpiJuniorWeeklyChartView extends View {

    public static class DayData {
        public final String dayLabel; // "Lun\n12/10"
        public final int posts;
        public final int videos;

        public DayData(String dayLabel, int posts, int videos) {
            this.dayLabel = dayLabel;
            this.posts = posts;
            this.videos = videos;
        }
    }

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint postPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint videoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<DayData> dataList = new ArrayList<>();
    private int maxScale = 10;

    public KpiJuniorWeeklyChartView(Context context) {
        super(context);
        init();
    }

    public KpiJuniorWeeklyChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KpiJuniorWeeklyChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_border));
        gridPaint.setStrokeWidth(dpToPx(1));

        axisTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_secondary));
        axisTextPaint.setTextSize(spToPx(10));
        axisTextPaint.setTextAlign(Paint.Align.RIGHT);

        postPaint.setColor(Color.parseColor("#8B5CF6")); // Morado
        videoPaint.setColor(Color.parseColor("#3B82F6")); // Azul

        barTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_primary));
        barTextPaint.setTextSize(spToPx(11));
        barTextPaint.setTextAlign(Paint.Align.CENTER);
        barTextPaint.setFakeBoldText(true);

        // Demo charts must not appear as a real worker's production history.
        if (!new pe.net.ws.kpiweb.core.session.SessionManager(getContext()).isLoggedIn())
            setDefaultData();
    }

    public void setDefaultData() {
        dataList.clear();
        dataList.add(new DayData("Lun\n12/10", 4, 2));
        dataList.add(new DayData("Mar\n13/10", 6, 2));
        dataList.add(new DayData("Mié\n14/10", 5, 1));
        dataList.add(new DayData("Jue\n15/10", 7, 3));
        dataList.add(new DayData("Vie\n16/10", 3, 2));
        dataList.add(new DayData("Sáb\n17/10", 4, 1));
        dataList.add(new DayData("Dom\n18/10", 3, 1));
        invalidate();
    }

    public void setData(List<DayData> list) {
        dataList.clear();
        if (list != null) {
            dataList.addAll(list);
            int highest = 10;
            for (DayData d : list) {
                if (d.posts > highest) highest = d.posts;
                if (d.videos > highest) highest = d.videos;
            }
            maxScale = ((highest + 1) / 2) * 2;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float leftMargin = dpToPx(32);
        float rightMargin = dpToPx(12);
        float topMargin = dpToPx(24);
        float bottomMargin = dpToPx(40);

        float chartWidth = width - leftMargin - rightMargin;
        float chartHeight = height - topMargin - bottomMargin;

        if (chartWidth <= 0 || chartHeight <= 0) return;

        // 1. Dibujar líneas guía horizontales (0, 2, 4, 6, 8, 10)
        int steps = 5; // 0, 2, 4, 6, 8, 10
        for (int i = 0; i <= steps; i++) {
            float y = topMargin + (chartHeight * (steps - i) / (float) steps);
            canvas.drawLine(leftMargin, y, width - rightMargin, y, gridPaint);

            int val = (maxScale * i) / steps;
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(val), leftMargin - dpToPx(6), y + dpToPx(3), axisTextPaint);
        }

        if (dataList.isEmpty()) return;

        // 2. Dibujar barras agrupadas por día
        int dayCount = dataList.size();
        float groupWidth = chartWidth / dayCount;
        float barWidth = dpToPx(10);
        float barSpacing = dpToPx(3);
        float cornerRadius = dpToPx(3);

        for (int i = 0; i < dayCount; i++) {
            DayData data = dataList.get(i);
            float groupCenterX = leftMargin + (i * groupWidth) + (groupWidth / 2f);

            // Posición barra posts (izquierda)
            float postLeft = groupCenterX - barWidth - (barSpacing / 2f);
            float postRight = postLeft + barWidth;
            float postHeight = (data.posts / (float) maxScale) * chartHeight;
            float postTop = topMargin + chartHeight - postHeight;
            float groundY = topMargin + chartHeight;

            // Dibujar barra de Posts
            if (postHeight > 0) {
                RectF postRect = new RectF(postLeft, postTop, postRight, groundY);
                canvas.drawRoundRect(postRect, cornerRadius, cornerRadius, postPaint);
                canvas.drawText(String.valueOf(data.posts), (postLeft + postRight) / 2f, postTop - dpToPx(4), barTextPaint);
            }

            // Posición barra videos (derecha)
            float videoLeft = groupCenterX + (barSpacing / 2f);
            float videoRight = videoLeft + barWidth;
            float videoHeight = (data.videos / (float) maxScale) * chartHeight;
            float videoTop = topMargin + chartHeight - videoHeight;

            // Dibujar barra de Videos
            if (videoHeight > 0) {
                RectF videoRect = new RectF(videoLeft, videoTop, videoRight, groundY);
                canvas.drawRoundRect(videoRect, cornerRadius, cornerRadius, videoPaint);
                canvas.drawText(String.valueOf(data.videos), (videoLeft + videoRight) / 2f, videoTop - dpToPx(4), barTextPaint);
            }

            // Etiqueta del día en el eje X
            String[] lines = data.dayLabel.split("\n");
            float textY = groundY + dpToPx(14);
            axisTextPaint.setTextAlign(Paint.Align.CENTER);
            if (lines.length > 0) {
                canvas.drawText(lines[0], groupCenterX, textY, axisTextPaint);
            }
            if (lines.length > 1) {
                canvas.drawText(lines[1], groupCenterX, textY + dpToPx(12), axisTextPaint);
            }
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
