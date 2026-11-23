package pe.net.ws.kpiweb.features.collaborator.common;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import pe.net.ws.kpiweb.R;

/**
 * Helper visual adaptable al Modo Claro y Modo Oscuro para mostrar avisos internos,
 * tarjetas emergentes (banners con botones de acción como "Ver KPI", "Ver ranking",
 * "Realizar evaluación", "Cerrar") y diálogos modales personalizados con la identidad
 * morada corporativa de WEBSITE, reemplazando los Toasts y AlertDialogs básicos de Android.
 */
public final class CollaboratorBannerHelper {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private CollaboratorBannerHelper() {
    }

    /**
     * Muestra un banner interno flotante adaptable al tema activo (Claro u Oscuro).
     */
    public static void showBanner(Activity activity, String title, String subtitle) {
        showBanner(activity, title, subtitle, R.drawable.collab_ic_website_logo);
    }

    public static void showSuccess(Activity activity, String message) {
        showBanner(activity, "¡Listo!", message, R.drawable.collab_ic_website_logo);
    }

    public static void showInfo(Activity activity, String title, String message) {
        showBanner(activity, title, message, R.drawable.collab_ic_website_logo);
    }

    public static void showBanner(Activity activity, String title, String subtitle, int iconRes) {
        showBannerWithAction(activity, title, subtitle, iconRes, "Cerrar", null);
    }

    public static void showBannerWithAction(Activity activity,
                                            String title,
                                            String subtitle,
                                            int iconRes,
                                            String actionLabel,
                                            Runnable onAction) {
        if (activity == null || activity.isFinishing()) return;

        View rootView = activity.findViewById(android.R.id.content);
        if (!(rootView instanceof ViewGroup)) return;
        ViewGroup contentParent = (ViewGroup) rootView;

        // Eliminar banner previo si existe
        View existing = contentParent.findViewWithTag("collab_custom_floating_banner");
        if (existing != null) {
            contentParent.removeView(existing);
        }

        float density = activity.getResources().getDisplayMetrics().density;
        int pad14 = (int) (14 * density);
        int pad12 = (int) (12 * density);
        int margin16 = (int) (16 * density);
        int marginTop = (int) (50 * density);

        int surfaceColor = activity.getColor(R.color.collab_bg_surface);
        int surfaceSecColor = activity.getColor(R.color.collab_bg_surface_secondary);
        int borderColor = activity.getColor(R.color.collab_purple_primary);
        int textPrimaryColor = activity.getColor(R.color.collab_text_primary);
        int textSecondaryColor = activity.getColor(R.color.collab_text_secondary);
        int accentColor = activity.getColor(R.color.collab_purple_accent);

        LinearLayout banner = new LinearLayout(activity);
        banner.setTag("collab_custom_floating_banner");
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(pad14, pad12, pad14, pad12);
        banner.setElevation(20 * density);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{surfaceColor, surfaceSecColor}
        );
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) (1.8f * density), borderColor);
        banner.setBackground(bg);

        // Contenedor de icono
        FrameLayout iconBox = new FrameLayout(activity);
        int boxSize = (int) (40 * density);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(boxSize, boxSize);
        iconBox.setLayoutParams(boxLp);
        iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);

        ImageView iv = new ImageView(activity);
        int iconSize = (int) (22 * density);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
        iv.setLayoutParams(ivLp);
        iv.setImageResource(iconRes);
        if (iconRes != R.drawable.collab_ic_website_logo) {
            iv.setColorFilter(accentColor);
        }
        iconBox.addView(iv);
        banner.addView(iconBox);

        // Columna de texto
        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textLp.setMarginStart((int) (12 * density));
        textLp.setMarginEnd((int) (8 * density));
        textCol.setLayoutParams(textLp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(textPrimaryColor);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvTitle.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(activity);
            tvSub.setText(subtitle);
            tvSub.setTextColor(textSecondaryColor);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = (int) (2 * density);
            tvSub.setLayoutParams(subLp);
            tvSub.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.NORMAL));
            textCol.addView(tvSub);
        }

        TextView swipeHint = new TextView(activity);
        swipeHint.setText("Desliza para cerrar");
        swipeHint.setTextColor(accentColor);
        swipeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        swipeHint.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.NORMAL));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = (int) (4 * density);
        swipeHint.setLayoutParams(hintLp);
        textCol.addView(swipeHint);

        banner.addView(textCol);

        // Botón de acción o cierre
        TextView tvAction = new TextView(activity);
        tvAction.setText(actionLabel != null && !actionLabel.isEmpty() ? actionLabel : "Cerrar");
        tvAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvAction.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        tvAction.setGravity(Gravity.CENTER);
        if (onAction != null) {
            tvAction.setBackgroundResource(R.drawable.collab_bg_chip_selected);
            tvAction.setTextColor(Color.WHITE);
            tvAction.setPadding((int) (12 * density), (int) (7 * density), (int) (12 * density), (int) (7 * density));
            tvAction.setOnClickListener(v -> {
                contentParent.removeView(banner);
                onAction.run();
            });
        } else {
            tvAction.setBackgroundResource(R.drawable.collab_bg_pill_purple);
            tvAction.setTextColor(accentColor);
            tvAction.setPadding((int) (10 * density), (int) (6 * density), (int) (10 * density), (int) (6 * density));
            tvAction.setOnClickListener(v -> contentParent.removeView(banner));
        }
        banner.addView(tvAction);

        View.OnTouchListener dismissOnSwipe = new View.OnTouchListener() {
            private float downX;
            private float downY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > 64 * density && Math.abs(dx) > Math.abs(dy)) {
                            banner.animate().translationX(Math.signum(dx) * banner.getWidth())
                                    .alpha(0f).setDuration(180)
                                    .withEndAction(() -> {
                                        if (banner.getParent() instanceof ViewGroup) {
                                            ((ViewGroup) banner.getParent()).removeView(banner);
                                        }
                                    }).start();
                        }
                        return true;
                    default:
                        return true;
                }
            }
        };
        banner.setOnTouchListener(dismissOnSwipe);
        textCol.setOnTouchListener(dismissOnSwipe);
        iconBox.setOnTouchListener(dismissOnSwipe);
        swipeHint.setOnTouchListener(dismissOnSwipe);

        FrameLayout.LayoutParams rootLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootLp.gravity = Gravity.TOP;
        rootLp.setMargins(margin16, marginTop, margin16, margin16);

        banner.setAlpha(0f);
        banner.setTranslationY(-30 * density);
        contentParent.addView(banner, rootLp);

        banner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240)
                .start();

        MAIN_HANDLER.postDelayed(() -> {
            if (activity.isFinishing()) return;
            if (banner.getParent() != null) {
                banner.animate()
                        .alpha(0f)
                        .translationY(-20 * density)
                        .setDuration(200)
                        .withEndAction(() -> {
                            if (banner.getParent() instanceof ViewGroup) {
                                ((ViewGroup) banner.getParent()).removeView(banner);
                            }
                        })
                        .start();
            }
        }, 3800);
    }

    /**
     * Muestra un diálogo modal con tarjeta adaptable al Modo Claro y Modo Oscuro en lugar del AlertDialog estándar.
     */
    public static void showPremiumModal(Activity activity,
                                        String badgeText,
                                        String title,
                                        String message,
                                        String primaryButtonText,
                                        Runnable onPrimaryAction,
                                        String secondaryButtonText) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        float density = activity.getResources().getDisplayMetrics().density;
        int pad20 = (int) (20 * density);

        int surfaceColor = activity.getColor(R.color.collab_bg_surface);
        int surfaceSecColor = activity.getColor(R.color.collab_bg_surface_secondary);
        int borderColor = activity.getColor(R.color.collab_purple_primary);
        int textPrimaryColor = activity.getColor(R.color.collab_text_primary);
        int textSecondaryColor = activity.getColor(R.color.collab_text_secondary);
        int accentColor = activity.getColor(R.color.collab_purple_accent);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad20, pad20, pad20, pad20);

        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{surfaceColor, surfaceSecColor}
        );
        cardBg.setCornerRadius(22 * density);
        cardBg.setStroke((int) (1.8f * density), borderColor);
        card.setBackground(cardBg);

        if (badgeText != null && !badgeText.isEmpty()) {
            TextView tvBadge = new TextView(activity);
            tvBadge.setText(badgeText.toUpperCase());
            tvBadge.setTextColor(accentColor);
            tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
            tvBadge.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            badgeLp.bottomMargin = (int) (10 * density);
            tvBadge.setLayoutParams(badgeLp);
            card.addView(tvBadge);
        }

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(textPrimaryColor);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(tvTitle);

        TextView tvMessage = new TextView(activity);
        tvMessage.setText(message);
        tvMessage.setTextColor(textSecondaryColor);
        tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvMessage.setLineSpacing(3 * density, 1f);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        msgLp.topMargin = (int) (10 * density);
        msgLp.bottomMargin = (int) (18 * density);
        tvMessage.setLayoutParams(msgLp);
        card.addView(tvMessage);

        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        if (secondaryButtonText != null && !secondaryButtonText.isEmpty()) {
            TextView btnSec = new TextView(activity);
            btnSec.setText(secondaryButtonText);
            btnSec.setTextColor(textPrimaryColor);
            btnSec.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            btnSec.setTypeface(Typeface.DEFAULT_BOLD);
            btnSec.setGravity(Gravity.CENTER);
            btnSec.setBackgroundResource(R.drawable.collab_bg_chip_unselected);
            btnSec.setPadding((int) (16 * density), (int) (10 * density), (int) (16 * density), (int) (10 * density));
            btnSec.setOnClickListener(v -> dialog.dismiss());
            btnRow.addView(btnSec);
        }

        if (primaryButtonText != null && !primaryButtonText.isEmpty()) {
            TextView btnPri = new TextView(activity);
            btnPri.setText(primaryButtonText);
            btnPri.setTextColor(Color.WHITE);
            btnPri.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
            btnPri.setTypeface(Typeface.DEFAULT_BOLD);
            btnPri.setGravity(Gravity.CENTER);
            btnPri.setBackgroundResource(R.drawable.collab_bg_chip_selected);
            btnPri.setPadding((int) (18 * density), (int) (10 * density), (int) (18 * density), (int) (10 * density));
            LinearLayout.LayoutParams priLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            priLp.setMarginStart((int) (10 * density));
            btnPri.setLayoutParams(priLp);
            btnPri.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPrimaryAction != null) {
                    onPrimaryAction.run();
                }
            });
            btnRow.addView(btnPri);
        }

        card.addView(btnRow);
        dialog.setContentView(card);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.show();
    }
}
