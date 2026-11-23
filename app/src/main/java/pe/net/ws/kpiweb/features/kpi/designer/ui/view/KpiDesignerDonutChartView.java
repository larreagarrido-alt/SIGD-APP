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

import pe.net.ws.kpiweb.R;

/**
 * Gráfico de anillo (Donut) nativo en Canvas 2D para la distribución de actividades
 * en el detalle del reporte semanal del Diseñador Gráfico.
 * Muestra el número central de actividades y arcos para:
 * - Diseños gráficos (#E11D48)
 * - Videos (#0284C7)
 * - Sesiones fotográficas (#10B981)
 */
public class KpiDesignerDonutChartView extends View {

    private final Paint arcPaintDesigns = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaintVideos = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaintPhotos = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint textCountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int designsCount = 17;
    private int videosCount = 5;
    private int photosCount = 1;

    public KpiDesignerDonutChartView(Context context) {
        super(context);
        init();
    }

    public KpiDesignerDonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KpiDesignerDonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float strokeWidth = dpToPx(16);

        arcPaintDesigns.setColor(Color.parseColor("#E11D48")); // Fucsia
        arcPaintDesigns.setStyle(Paint.Style.STROKE);
        arcPaintDesigns.setStrokeWidth(strokeWidth);
        arcPaintDesigns.setStrokeCap(Paint.Cap.ROUND);

        arcPaintVideos.setColor(Color.parseColor("#0284C7"));  // Azul
        arcPaintVideos.setStyle(Paint.Style.STROKE);
        arcPaintVideos.setStrokeWidth(strokeWidth);
        arcPaintVideos.setStrokeCap(Paint.Cap.ROUND);

        arcPaintPhotos.setColor(Color.parseColor("#10B981"));  // Verde
        arcPaintPhotos.setStyle(Paint.Style.STROKE);
        arcPaintPhotos.setStrokeWidth(strokeWidth);
        arcPaintPhotos.setStrokeCap(Paint.Cap.ROUND);

        textCountPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_primary));
        textCountPaint.setTextSize(spToPx(24));
        textCountPaint.setTextAlign(Paint.Align.CENTER);
        textCountPaint.setFakeBoldText(true);

        textLabelPaint.setColor(ContextCompat.getColor(getContext(), R.color.kpi_text_secondary));
        textLabelPaint.setTextSize(spToPx(11));
        textLabelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDistribution(int designs, int videos, int photos) {
        this.designsCount = designs;
        this.videosCount = videos;
        this.photosCount = photos;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int size = Math.min(width, height);

        float padding = dpToPx(12);
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = (size / 2f) - padding;

        if (radius <= 0) return;

        RectF oval = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        int total = designsCount + videosCount + photosCount;
        if (total <= 0) {
            total = 1;
        }

        float sweepDesigns = (designsCount / (float) total) * 360f;
        float sweepVideos = (videosCount / (float) total) * 360f;
        float sweepPhotos = (photosCount / (float) total) * 360f;

        float gap = dpToPx(3);
        float startAngle = -90f;

        // Diseños
        if (designsCount > 0) {
            canvas.drawArc(oval, startAngle + (gap / 2), Math.max(1, sweepDesigns - gap), false, arcPaintDesigns);
        }
        startAngle += sweepDesigns;

        // Videos
        if (videosCount > 0) {
            canvas.drawArc(oval, startAngle + (gap / 2), Math.max(1, sweepVideos - gap), false, arcPaintVideos);
        }
        startAngle += sweepVideos;

        // Fotos
        if (photosCount > 0) {
            canvas.drawArc(oval, startAngle + (gap / 2), Math.max(1, sweepPhotos - gap), false, arcPaintPhotos);
        }

        // Texto central
        int displayTotal = designsCount + videosCount + photosCount;
        canvas.drawText(String.valueOf(displayTotal), centerX, centerY + dpToPx(4), textCountPaint);
        canvas.drawText("actividades", centerX, centerY + dpToPx(18), textLabelPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
