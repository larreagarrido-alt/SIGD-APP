package pe.net.ws.kpiweb.features.schedule.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Contenedor con zoom (pellizco, doble toque) y desplazamiento libre en ambos ejes para su único hijo.
 * Los toques simples siguen llegando a las celdas del hijo (edición directa sobre la ficha).
 */
public class ZoomPanLayout extends FrameLayout {

    // La ficha tiene 1036 dp; en teléfonos de 360 dp debe poder ajustarse completa.
    private static final float MIN_SCALE = 0.2f;
    private static final float MAX_SCALE = 3.5f;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float scale = 1f;
    private float tx = 0f;
    private float ty = 0f;
    private float lastX;
    private float lastY;
    private int activePointer = -1;
    private boolean dragging = false;
    private boolean scaling = false;
    private final int touchSlop;
    private boolean initialFitDone = false;

    public ZoomPanLayout(Context context) {
        this(context, null);
    }

    public ZoomPanLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector d) {
                scaling = true;
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector d) {
                zoomAround(scale * d.getScaleFactor(), d.getFocusX(), d.getFocusY());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector d) {
                scaling = false;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zoomAround(scale < 1.4f ? 2f : 1f, e.getX(), e.getY());
                return true;
            }
        });
        setClipChildren(true);
    }

    private View content() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View c = content();
        if (c != null) {
            // El hijo mide libremente (su tamaño natural); el escalado se aplica con matriz de vista.
            measureChild(c, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View c = content();
        if (c == null) return;
        c.layout(0, 0, c.getMeasuredWidth(), c.getMeasuredHeight());
        c.setPivotX(0f);
        c.setPivotY(0f);
        if (!initialFitDone && c.getMeasuredWidth() > 0 && getWidth() > 0) {
            fitToWidth();
            initialFitDone = true;
        } else {
            apply();
        }
    }

    /** Ajusta la ficha al ancho disponible (vista panorámica completa). */
    public void fitToWidth() {
        View c = content();
        if (c == null || c.getMeasuredWidth() == 0 || getWidth() == 0) return;
        scale = clamp(Math.min(1f, (float) getWidth() / c.getMeasuredWidth()));
        tx = 0f;
        ty = 0f;
        apply();
    }

    /** Muestra la hoja completa conservando su proporción. */
    public void fitToPage() {
        View c = content();
        if (c == null || c.getMeasuredWidth() == 0 || c.getMeasuredHeight() == 0
                || getWidth() == 0 || getHeight() == 0) return;
        scale = clamp(Math.min((float) getWidth() / c.getMeasuredWidth(),
                (float) getHeight() / c.getMeasuredHeight()));
        tx = 0f;
        ty = 0f;
        apply();
    }

    public void zoomBy(float factor) {
        zoomAround(scale * factor, getWidth() / 2f, getHeight() / 2f);
    }

    public void resetZoom() {
        fitToWidth();
    }

    public float getScale() {
        return scale;
    }

    private float clamp(float s) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
    }

    private void zoomAround(float target, float fx, float fy) {
        float ns = clamp(target);
        float ratio = ns / scale;
        tx = fx - (fx - tx) * ratio;
        ty = fy - (fy - ty) * ratio;
        scale = ns;
        apply();
    }

    private void apply() {
        View c = content();
        if (c == null) return;
        float cw = c.getMeasuredWidth() * scale;
        float ch = c.getMeasuredHeight() * scale;
        float minX = Math.min(0f, getWidth() - cw);
        float minY = Math.min(0f, getHeight() - ch);
        tx = cw <= getWidth() ? 0f : Math.max(minX, Math.min(0f, tx));
        ty = ch <= getHeight() ? 0f : Math.max(minY, Math.min(0f, ty));
        c.setScaleX(scale);
        c.setScaleY(scale);
        c.setTranslationX(tx);
        c.setTranslationY(ty);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();
                activePointer = ev.getPointerId(0);
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (ev.getPointerCount() > 1 || scaling) return true;
                if (!dragging && (Math.abs(ev.getX() - lastX) > touchSlop || Math.abs(ev.getY() - lastY) > touchSlop)) {
                    dragging = true;
                    lastX = ev.getX();
                    lastY = ev.getY();
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);
        gestureDetector.onTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();
                activePointer = ev.getPointerId(0);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (scaling || ev.getPointerCount() > 1) {
                    return true;
                }
                int idx = ev.findPointerIndex(activePointer);
                if (idx < 0) return true;
                float x = ev.getX(idx);
                float y = ev.getY(idx);
                tx += x - lastX;
                ty += y - lastY;
                lastX = x;
                lastY = y;
                apply();
                return true;
            case MotionEvent.ACTION_POINTER_UP: {
                int upIdx = ev.getActionIndex();
                if (ev.getPointerId(upIdx) == activePointer) {
                    int newIdx = upIdx == 0 ? 1 : 0;
                    activePointer = ev.getPointerId(newIdx);
                    lastX = ev.getX(newIdx);
                    lastY = ev.getY(newIdx);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointer = -1;
                dragging = false;
                return true;
            default:
                return true;
        }
    }
}
