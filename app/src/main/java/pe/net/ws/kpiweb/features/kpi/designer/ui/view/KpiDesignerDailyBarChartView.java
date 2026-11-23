package pe.net.ws.kpiweb.features.kpi.designer.ui.view;

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
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;

/**
 * Gráfico nativo en Canvas 2D de producción diaria para el Diseñador Gráfico.
 * Muestra 3 barras agrupadas por día:
 * - Diseños (Fucsia / Rosa: #E11D48)
 * - Videos (Azul: #0284C7)
 * - Sesiones Fotográficas (Verde: #10B981)
 */
public class KpiDesignerDailyBarChartView extends View {

    public static class DayProduction {
        public final String dayShort; // "Lun\n04/09"
        public final int designs;
        public final int videos;
        public final int photos;

        public DayProduction(String dayShort, int designs, int videos, int photos) {
            this.dayShort = dayShort;
            this.designs = designs;
            this.videos = videos;
            this.photos = photos;
        }
    }

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint designsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint videosPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint photosPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<DayProduction> dataList = new ArrayList<>();
    private int maxScale = 8;

    public KpiDesignerDailyBarChartView(Context context) {
        super(context);
        init();
    }

    public KpiDesignerDailyBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KpiDesignerDailyBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_border));
        gridPaint.setStrokeWidth(dpToPx(1));

        axisTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_secondary));
        axisTextPaint.setTextSize(spToPx(10));
        axisTextPaint.setTextAlign(Paint.Align.RIGHT);

        designsPaint.setColor(Color.parseColor("#E11D48")); // Fucsia/Rosa
        videosPaint.setColor(Color.parseColor("#0284C7"));  // Azul
        photosPaint.setColor(Color.parseColor("#10B981"));  // Verde esmeralda

        barTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_primary));
        barTextPaint.setTextSize(spToPx(10));
        barTextPaint.setTextAlign(Paint.Align.CENTER);
        barTextPaint.setFakeBoldText(true);

        if (!new pe.net.ws.kpiweb.core.session.SessionManager(getContext()).isLoggedIn())
            setDefaultMockData();
    }

    public void setDefaultMockData() {
        dataList.clear();
        dataList.add(new DayProduction("Lun\n04/09", 4, 2, 0));
        dataList.add(new DayProduction("Mar\n05/09", 6, 0, 0));
        dataList.add(new DayProduction("Mié\n06/09", 3, 1, 0));
        dataList.add(new DayProduction("Jue\n07/09", 6, 0, 0));
        dataList.add(new DayProduction("Vie\n08/09", 3, 2, 0));
        dataList.add(new DayProduction("Sáb\n09/09", 0, 0, 1));
        invalidate();
    }

    public void setDaysFromRecords(List<KpiDesignerDayRecord> records) {
        dataList.clear();
        String[] daysFull = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};
        String[] daysShort = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"};

        for (int i = 0; i < 6; i++) {
            String targetDay = daysFull[i];
            KpiDesignerDayRecord match = null;

            if (records != null) {
                for (KpiDesignerDayRecord r : records) {
                    if (r.getDayOfWeek() != null &&
                            (r.getDayOfWeek().equalsIgnoreCase(targetDay) ||
                             r.getDayOfWeek().replace("É", "E").replace("Á", "A").equalsIgnoreCase(targetDay.replace("É", "E").replace("Á", "A")))) {
                        match = r;
                        break;
                    }
                }
            }

            if (match != null) {
                String shortDate = match.getDate() != null && match.getDate().length() >= 5 ?
                        match.getDate().substring(0, 5) : "";
                dataList.add(new DayProduction(daysShort[i] + "\n" + shortDate,
                        match.getDesignsCompleted(), match.getVideosCompleted(), match.getPhotosCompleted()));
            } else {
                dataList.add(new DayProduction(daysShort[i] + "\n--/--", 0, 0, 0));
            }
        }

        recalculateScale();
        invalidate();
    }

    private void recalculateScale() {
        int highest = 6;
        for (DayProduction d : dataList) {
            if (d.designs > highest) highest = d.designs;
            if (d.videos > highest) highest = d.videos;
            if (d.photos > highest) highest = d.photos;
        }
        maxScale = ((highest + 1) / 2) * 2;
        if (maxScale < 8) maxScale = 8;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float leftMargin = dpToPx(28);
        float rightMargin = dpToPx(12);
        float topMargin = dpToPx(22);
        float bottomMargin = dpToPx(38);

        float chartWidth = width - leftMargin - rightMargin;
        float chartHeight = height - topMargin - bottomMargin;

        if (chartWidth <= 0 || chartHeight <= 0) return;

        // 1. Líneas de guía horizontales (0, 2, 4, 6, 8)
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            float y = topMargin + (chartHeight * (steps - i) / (float) steps);
            canvas.drawLine(leftMargin, y, width - rightMargin, y, gridPaint);

            int val = (maxScale * i) / steps;
            axisTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(val), leftMargin - dpToPx(4), y + dpToPx(3), axisTextPaint);
        }

        if (dataList.isEmpty()) return;

        // 2. Dibujar barras agrupadas por día
        int dayCount = dataList.size();
        float groupWidth = chartWidth / dayCount;
        float barWidth = dpToPx(8);
        float barSpacing = dpToPx(2);
        float cornerRadius = dpToPx(2.5f);

        for (int i = 0; i < dayCount; i++) {
            DayProduction d = dataList.get(i);
            float groupCenterX = leftMargin + (i * groupWidth) + (groupWidth / 2f);
            float groundY = topMargin + chartHeight;

            // Barra 1: Diseños (Izquierda)
            float dLeft = groupCenterX - (barWidth * 1.5f) - barSpacing;
            float dRight = dLeft + barWidth;
            float dHeight = (d.designs / (float) maxScale) * chartHeight;
            float dTop = groundY - dHeight;

            if (d.designs > 0) {
                RectF dRect = new RectF(dLeft, dTop, dRight, groundY);
                canvas.drawRoundRect(dRect, cornerRadius, cornerRadius, designsPaint);
                canvas.drawText(String.valueOf(d.designs), (dLeft + dRight) / 2f, dTop - dpToPx(3), barTextPaint);
            }

            // Barra 2: Videos (Centro)
            float vLeft = groupCenterX - (barWidth / 2f);
            float vRight = vLeft + barWidth;
            float vHeight = (d.videos / (float) maxScale) * chartHeight;
            float vTop = groundY - vHeight;

            if (d.videos > 0) {
                RectF vRect = new RectF(vLeft, vTop, vRight, groundY);
                canvas.drawRoundRect(vRect, cornerRadius, cornerRadius, videosPaint);
                canvas.drawText(String.valueOf(d.videos), (vLeft + vRight) / 2f, vTop - dpToPx(3), barTextPaint);
            }

            // Barra 3: Fotos (Derecha)
            float pLeft = groupCenterX + (barWidth / 2f) + barSpacing;
            float pRight = pLeft + barWidth;
            float pHeight = (d.photos / (float) maxScale) * chartHeight;
            float pTop = groundY - pHeight;

            if (d.photos > 0) {
                RectF pRect = new RectF(pLeft, pTop, pRight, groundY);
                canvas.drawRoundRect(pRect, cornerRadius, cornerRadius, photosPaint);
                canvas.drawText(String.valueOf(d.photos), (pLeft + pRight) / 2f, pTop - dpToPx(3), barTextPaint);
            }

            // Etiqueta del día en el eje X
            String[] lines = d.dayShort.split("\n");
            float textY = groundY + dpToPx(13);
            axisTextPaint.setTextAlign(Paint.Align.CENTER);
            if (lines.length > 0) {
                canvas.drawText(lines[0], groupCenterX, textY, axisTextPaint);
            }
            if (lines.length > 1) {
                canvas.drawText(lines[1], groupCenterX, textY + dpToPx(11), axisTextPaint);
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
