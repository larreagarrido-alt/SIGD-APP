package pe.net.ws.kpiweb.features.kpi.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contenedor con Zoom Interactivo (Pinch-to-Zoom con 2 dedos + Desplazamiento 2D libre)
 * diseñado exclusivamente para el lienzo de la Hoja Oficial en el Editor Maestro de KPIs.
 *
 * - Permite acercar la hoja desde 1.0x (100%) hasta 3.0x (300%).
 * - Conserva intacta la detección de toques simples (clicks) sobre cada celda editable,
 *   transformando automáticamente las coordenadas mediante la matriz nativa de ViewGroup.
 */
public class KpiZoomableSheetLayout extends FrameLayout {

    public interface OnZoomChangeListener {
        void onZoomChanged(float scaleFactor, int zoomPercent);
    }

    private static final float MIN_SCALE = 0.2f;
    private static final float MAX_SCALE = 3.0f;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector doubleTapDetector;
    private final int touchSlop;

    private float scaleFactor = 1.0f;
    private float tranX = 0f;
    private float tranY = 0f;

    private float downX = 0f;
    private float downY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private boolean isDragging = false;

    private OnZoomChangeListener zoomChangeListener;

    public KpiZoomableSheetLayout(@NonNull Context context) {
        this(context, null);
    }

    public KpiZoomableSheetLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KpiZoomableSheetLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClipChildren(true);
        setClipToPadding(true);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        doubleTapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(@NonNull MotionEvent event) {
                if (scaleFactor > 1.01f) resetZoom();
                else setZoomAnimatedOrDirect(2f);
                return true;
            }
        });

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                requestDisallowParentIntercept(true);
                return true;
            }

            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float prevScale = scaleFactor;
                float newScale = clamp(prevScale * detector.getScaleFactor(), MIN_SCALE, MAX_SCALE);
                if (Math.abs(newScale - prevScale) > 0.001f) {
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();
                    float ratio = newScale / prevScale;
                    tranX = focusX - (focusX - tranX) * ratio;
                    tranY = focusY - (focusY - tranY) * ratio;
                    scaleFactor = newScale;
                    clampTranslationsAndApply();
                    notifyZoomChanged();
                }
                return true;
            }
        });
    }

    public void setOnZoomChangeListener(@Nullable OnZoomChangeListener listener) {
        this.zoomChangeListener = listener;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public int getZoomPercent() {
        return Math.round(scaleFactor * 100f);
    }

    public void zoomIn() {
        setZoomAnimatedOrDirect(clamp(scaleFactor + 0.25f, MIN_SCALE, MAX_SCALE));
    }

    public void zoomOut() {
        setZoomAnimatedOrDirect(clamp(scaleFactor - 0.25f, MIN_SCALE, MAX_SCALE));
    }

    public void resetZoom() {
        scaleFactor = 1.0f;
        tranX = 0f;
        tranY = 0f;
        clampTranslationsAndApply();
        notifyZoomChanged();
    }

    /** Ajusta toda la hoja al alto visible del editor, manteniendo su proporción. */
    public void fitToPage() {
        View child = getTargetChild();
        ScrollView viewport = findParentScrollView();
        if (child == null || viewport == null || child.getHeight() == 0 || viewport.getHeight() == 0) return;
        scaleFactor = clamp(Math.min(1f, (float) viewport.getHeight() / child.getHeight()),
                MIN_SCALE, MAX_SCALE);
        tranX = 0f;
        tranY = 0f;
        clampTranslationsAndApply();
        notifyZoomChanged();
        viewport.smoothScrollTo(0, 0);
    }

    private void setZoomAnimatedOrDirect(float targetScale) {
        float prevScale = scaleFactor;
        if (Math.abs(targetScale - prevScale) < 0.001f) return;

        View child = getTargetChild();
        float focusX = child != null ? child.getWidth() * 0.5f : getWidth() * 0.5f;
        float focusY = child != null ? Math.min(getHeight() * 0.5f, child.getHeight() * 0.35f) : getHeight() * 0.5f;

        float ratio = targetScale / prevScale;
        tranX = focusX - (focusX - tranX) * ratio;
        tranY = focusY - (focusY - tranY) * ratio;
        scaleFactor = targetScale;
        if (Math.abs(scaleFactor - 1f) <= 0.01f) {
            scaleFactor = 1.0f;
            tranX = 0f;
            tranY = 0f;
        }
        clampTranslationsAndApply();
        notifyZoomChanged();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        doubleTapDetector.onTouchEvent(ev);
        if (ev.getPointerCount() > 1 || scaleFactor > 1.01f) {
            requestDisallowParentIntercept(true);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isDragging = false;
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            return false;
        }

        if (ev.getPointerCount() > 1 || scaleDetector.isInProgress()) {
            isDragging = true;
            requestDisallowParentIntercept(true);
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                downX = ev.getX(0);
                downY = ev.getY(0);
                lastTouchX = downX;
                lastTouchY = downY;
                activePointerId = ev.getPointerId(0);
                isDragging = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (scaleFactor > 1.01f) {
                    int idx = ev.findPointerIndex(activePointerId);
                    if (idx < 0) idx = 0;
                    float x = ev.getX(idx);
                    float y = ev.getY(idx);
                    float dist = (float) Math.hypot(x - downX, y - downY);
                    if (dist > touchSlop) {
                        isDragging = true;
                        lastTouchX = x;
                        lastTouchY = y;
                        requestDisallowParentIntercept(true);
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);

        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                downX = ev.getX(0);
                downY = ev.getY(0);
                lastTouchX = downX;
                lastTouchY = downY;
                activePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                requestDisallowParentIntercept(true);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!scaleDetector.isInProgress() && scaleFactor > 1.01f) {
                    int idx = ev.findPointerIndex(activePointerId);
                    if (idx < 0) {
                        activePointerId = ev.getPointerId(0);
                        idx = 0;
                    }
                    float x = ev.getX(idx);
                    float y = ev.getY(idx);
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;

                    if (!isDragging && Math.hypot(x - downX, y - downY) > touchSlop) {
                        isDragging = true;
                    }

                    if (isDragging) {
                        requestDisallowParentIntercept(true);
                        panBy(dx, dy);
                        lastTouchX = x;
                        lastTouchY = y;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = ev.getActionIndex();
                int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newIndex = (pointerIndex == 0) ? 1 : 0;
                    if (newIndex < ev.getPointerCount()) {
                        lastTouchX = ev.getX(newIndex);
                        lastTouchY = ev.getY(newIndex);
                        activePointerId = ev.getPointerId(newIndex);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isDragging = false;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }
        }
        return true;
    }

    private void panBy(float dx, float dy) {
        View child = getTargetChild();
        if (child == null) return;

        int w = child.getWidth();
        int h = child.getHeight();
        float minX = -w * (scaleFactor - 1f);
        float minY = -h * (scaleFactor - 1f);

        tranX = clamp(tranX + dx, minX, 0f);

        float candidateY = tranY + dy;
        float clampedY = clamp(candidateY, minY, 0f);
        float leftoverDy = candidateY - clampedY;
        tranY = clampedY;

        applyTransformToChild(child);

        // Si el usuario arrastra verticalmente más allá del borde del zoom actual,
        // desplazamos suavemente el ScrollView padre para recorrer toda la hoja de arriba a abajo.
        if (Math.abs(leftoverDy) > 0.5f) {
            ScrollView parentScroll = findParentScrollView();
            if (parentScroll != null) {
                parentScroll.scrollBy(0, Math.round(-leftoverDy));
            }
        }
    }

    private void clampTranslationsAndApply() {
        View child = getTargetChild();
        if (child == null) return;

        if (scaleFactor <= 1.005f) {
            tranX = 0f;
            tranY = 0f;
        } else {
            int w = child.getWidth();
            int h = child.getHeight();
            float minX = -w * (scaleFactor - 1f);
            float minY = -h * (scaleFactor - 1f);
            tranX = clamp(tranX, minX, 0f);
            tranY = clamp(tranY, minY, 0f);
        }
        applyTransformToChild(child);
    }

    private void applyTransformToChild(@NonNull View child) {
        child.setPivotX(0f);
        child.setPivotY(0f);
        child.setScaleX(scaleFactor);
        child.setScaleY(scaleFactor);
        child.setTranslationX(tranX);
        child.setTranslationY(tranY);
    }

    @Nullable
    private View getTargetChild() {
        return getChildCount() > 0 ? getChildAt(0) : null;
    }

    @Nullable
    private ScrollView findParentScrollView() {
        ViewParent p = getParent();
        while (p != null) {
            if (p instanceof ScrollView) {
                return (ScrollView) p;
            }
            p = p.getParent();
        }
        return null;
    }

    private void requestDisallowParentIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    private void notifyZoomChanged() {
        if (zoomChangeListener != null) {
            zoomChangeListener.onZoomChanged(scaleFactor, getZoomPercent());
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
