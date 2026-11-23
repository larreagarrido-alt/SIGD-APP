package pe.net.ws.kpiweb.features.collaborator.rewards;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Matrix;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Shared, read-only viewer for published prize photos and the admin preview. */
public final class RewardImageViewer {
    private RewardImageViewer() { }

    public static void show(Activity activity, String imagePath, String bearer, Drawable preview) {
        show(activity, imagePath, bearer, preview, null);
    }

    public static void show(Activity activity, String imagePath, String bearer,
                            Drawable preview, Uri selectedImage) {
        if ((imagePath == null || imagePath.isEmpty()) && preview == null && selectedImage == null) return;
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(0xFF0B0618);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ZoomImage image = new ZoomImage(activity);
        image.setContentDescription("Imagen del premio ampliada. Pellizca o toca dos veces para ampliar");
        root.addView(image, new FrameLayout.LayoutParams(-1, -1));
        if (preview != null) image.setImageDrawable(preview.getConstantState() == null
                ? preview : preview.getConstantState().newDrawable().mutate());
        if (selectedImage != null) {
            RewardImageLoader.loadLocal(activity, image, selectedImage, 4096);
        } else if (imagePath != null && !imagePath.isEmpty()) {
            RewardImageLoader.load(activity, image, null, imagePath, bearer, 4096);
        }
        TextView close = new TextView(activity);
        close.setText("✕  Cerrar");
        close.setTextColor(0xFFFFFFFF);
        close.setTextSize(16);
        close.setGravity(Gravity.CENTER);
        close.setBackgroundColor(0xCC261343);
        int pad = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        close.setPadding(pad, pad / 2, pad, pad / 2);
        close.setContentDescription("Cerrar imagen ampliada");
        close.setOnClickListener(v -> root.animate().alpha(0f).setDuration(140)
                .withEndAction(dialog::dismiss).start());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        closeParams.setMargins(pad, pad, pad, pad);
        root.addView(close, closeParams);
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        dialog.show();
        ViewCompat.requestApplyInsets(root);
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(160).start();
        if (window != null) window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
    }

    private static final class ZoomImage extends androidx.appcompat.widget.AppCompatImageView {
        private final Matrix matrix = new Matrix();
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;
        private float scale = 1f;
        private float lastX, lastY;
        private boolean dragging;

        ZoomImage(Activity activity) {
            super(activity);
            setScaleType(ImageView.ScaleType.MATRIX);
            scaleDetector = new ScaleGestureDetector(activity, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    zoom(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                    return true;
                }
            });
            gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDoubleTap(MotionEvent event) {
                    if (scale > 1.01f) fit();
                    else zoom(2.5f, event.getX(), event.getY());
                    return true;
                }
            });
        }

        @Override public void setImageDrawable(Drawable drawable) {
            super.setImageDrawable(drawable);
            post(this::fit);
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            post(this::fit);
        }

        private void fit() {
            Drawable drawable = getDrawable();
            if (drawable == null || getWidth() == 0 || getHeight() == 0
                    || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) return;
            matrix.reset();
            float ratio = Math.min(1f, Math.min((float) getWidth() / drawable.getIntrinsicWidth(),
                    (float) getHeight() / drawable.getIntrinsicHeight()));
            matrix.postScale(ratio, ratio);
            matrix.postTranslate((getWidth() - drawable.getIntrinsicWidth() * ratio) / 2f,
                    (getHeight() - drawable.getIntrinsicHeight() * ratio) / 2f);
            scale = 1f;
            setImageMatrix(matrix);
        }

        private void zoom(float factor, float x, float y) {
            float next = Math.max(1f, Math.min(5f, scale * factor));
            matrix.postScale(next / scale, next / scale, x, y);
            scale = next;
            constrain();
        }

        private void constrain() {
            Drawable drawable = getDrawable();
            if (drawable == null) return;
            android.graphics.RectF bounds = new android.graphics.RectF(0, 0,
                    drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(bounds);
            float dx = bounds.width() <= getWidth() ? getWidth() / 2f - bounds.centerX()
                    : bounds.left > 0 ? -bounds.left : bounds.right < getWidth() ? getWidth() - bounds.right : 0;
            float dy = bounds.height() <= getHeight() ? getHeight() / 2f - bounds.centerY()
                    : bounds.top > 0 ? -bounds.top : bounds.bottom < getHeight() ? getHeight() - bounds.bottom : 0;
            matrix.postTranslate(dx, dy);
            setImageMatrix(matrix);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            if (event.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    lastX = event.getX(); lastY = event.getY(); dragging = true;
                } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && dragging && scale > 1f) {
                    matrix.postTranslate(event.getX() - lastX, event.getY() - lastY);
                    lastX = event.getX(); lastY = event.getY(); constrain();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    dragging = false; performClick();
                }
            }
            return true;
        }

        @Override public boolean performClick() { return super.performClick(); }
    }
}
