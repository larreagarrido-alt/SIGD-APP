package pe.net.ws.kpiweb.features.kpi.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import pe.net.ws.kpiweb.R;

/**
 * Adaptador transversal de paleta visual:
 * - Modo Claro: Variantes de Morado Encendido / Medio Fucsia (#A21CAF / #C026D3).
 * - Modo Oscuro: Variantes de Morado con Azul (#818CF8 / #2563EB sobre #0B1120).
 */
public final class KpiThemeContrastApplier {

    private KpiThemeContrastApplier() {}

    public static void apply(View root) {
        if (root == null) {
            return;
        }
        boolean darkMode = KpiThemeManager.isDarkMode(root.getContext());
        applyRecursively(root, darkMode);
    }

    private static void applyRecursively(View view, boolean darkMode) {
        Context context = view.getContext();
        adaptBackground(context, view, darkMode);

        if (view instanceof TextView) {
            adaptText(context, (TextView) view, darkMode);
        }
        if (view instanceof ImageView) {
            adaptImageTint(context, (ImageView) view, darkMode);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyRecursively(group.getChildAt(index), darkMode);
            }
        }
    }

    private static void adaptBackground(Context context, View view, boolean darkMode) {
        Drawable background = view.getBackground();
        if (!(background instanceof ColorDrawable)) return;

        int color = ((ColorDrawable) background).getColor();
        if (darkMode) {
            if (matches(color, 0xFFF8FAFC) || matches(color, 0xFFFAF5FF)) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.kpi_light_bg));
            } else if (matches(color, 0xFFFFFFFF)) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.kpi_card_bg));
            } else if (matches(color, 0xFFF1F5F9) || matches(color, 0xFFE2E8F0)
                    || matches(color, 0xFFF5F3FF) || matches(color, 0xFFEDE9FE)
                    || matches(color, 0xFFEEF2FF) || matches(color, 0xFFEFF6FF)
                    || matches(color, 0xFFF0FDF4) || matches(color, 0xFFFFFBEB)
                    || matches(color, 0xFFFDF2F8)) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.kpi_surface_variant));
            } else if (isLegacyPurple(color)) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.kpi_btn_save_start));
            }
        } else {
            if (matches(color, 0xFFF8FAFC)) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.kpi_light_bg));
            } else if (isLegacyPurple(color)) {
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.kpi_primary));
            }
        }
    }

    private static void adaptText(Context context, TextView textView, boolean darkMode) {
        int textColor = textView.getCurrentTextColor();
        if (isLegacyPurple(textColor)) {
            textView.setTextColor(ContextCompat.getColor(context, R.color.kpi_primary));
        } else if (darkMode) {
            if (isPrimaryNeutral(textColor)) {
                textView.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_primary));
            } else if (isSecondaryNeutral(textColor)) {
                textView.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_secondary));
            }
        }

        if (darkMode && textView.getHintTextColors() != null) {
            int hintColor = textView.getHintTextColors().getDefaultColor();
            if (isPrimaryNeutral(hintColor) || isSecondaryNeutral(hintColor)) {
                textView.setHintTextColor(ContextCompat.getColor(context, R.color.kpi_gray_light));
            }
        }
    }

    private static void adaptImageTint(Context context, ImageView imageView, boolean darkMode) {
        ColorStateList tint = imageView.getImageTintList();
        if (tint == null) return;

        int color = tint.getDefaultColor();
        if (isLegacyPurple(color)) {
            imageView.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.kpi_primary)));
        } else if (darkMode) {
            if (isPrimaryNeutral(color)) {
                imageView.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.kpi_text_primary)));
            } else if (isSecondaryNeutral(color)) {
                imageView.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.kpi_text_secondary)));
            }
        }
    }

    private static boolean isLegacyPurple(int color) {
        return matches(color, 0xFF5B21B6)
                || matches(color, 0xFF4C1D95)
                || matches(color, 0xFF6D28D9)
                || matches(color, 0xFF7C3AED);
    }

    private static boolean isPrimaryNeutral(int color) {
        return matches(color, 0xFF0F172A)
                || matches(color, 0xFF1E293B)
                || matches(color, 0xFF334155);
    }

    private static boolean isSecondaryNeutral(int color) {
        return matches(color, 0xFF475569)
                || matches(color, 0xFF64748B)
                || matches(color, 0xFF94A3B8);
    }

    private static boolean matches(int first, int second) {
        return first == second;
    }
}
