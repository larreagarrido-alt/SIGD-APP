package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;

/**
 * Componente reutilizable de tarjetas interactivas para Bienestar Psicológico.
 * Muestra caritas vectoriales 3D (52dp) para escalas emocionales o íconos
 * adaptados para otras escalas, con borde violeta luminoso (#C66BFF), check
 * circular y animaciones discretas (180-220 ms) respetando reducir movimiento.
 */
final class WellnessOptionCardBinder {

    private static final long ANIM_DURATION_MS = 210L;

    static final class OptionItem {
        final int optionId;
        final String label;
        final String subtitle;

        OptionItem(int optionId, String label, String subtitle) {
            this.optionId = optionId;
            this.label = label == null ? "" : label;
            this.subtitle = subtitle == null ? "" : subtitle.trim();
        }
    }

    interface OnOptionSelectedListener {
        void onOptionSelected(int optionId);
    }

    private static final class BoundCard {
        final OptionItem item;
        final String resolvedSubtitle;
        final WellnessOptionVisualResolver.OptionVisual visual;
        final View root;
        final FrameLayout iconBox;
        final ImageView iconView;
        final TextView badgeText;
        final TextView labelView;
        final TextView subtitleView;
        final FrameLayout checkRing;
        final ImageView checkIcon;

        BoundCard(OptionItem item, String resolvedSubtitle,
                  WellnessOptionVisualResolver.OptionVisual visual, View root) {
            this.item = item;
            this.resolvedSubtitle = resolvedSubtitle;
            this.visual = visual;
            this.root = root;
            this.iconBox = root.findViewById(R.id.collab_wellness_option_icon_box);
            this.iconView = root.findViewById(R.id.collab_wellness_option_icon);
            this.badgeText = root.findViewById(R.id.collab_wellness_option_badge_text);
            this.labelView = root.findViewById(R.id.collab_wellness_option_label);
            this.subtitleView = root.findViewById(R.id.collab_wellness_option_subtitle);
            this.checkRing = root.findViewById(R.id.collab_wellness_option_check_ring);
            this.checkIcon = root.findViewById(R.id.collab_wellness_option_check_icon);
        }
    }

    private final Context context;
    private final List<BoundCard> boundCards = new ArrayList<>();
    private int currentSelectedId;

    private WellnessOptionCardBinder(Context context, int initialSelectedId) {
        this.context = context;
        this.currentSelectedId = initialSelectedId;
    }

    static WellnessOptionCardBinder bind(
            LinearLayout container,
            List<OptionItem> items,
            int selectedOptionId,
            OnOptionSelectedListener listener
    ) {
        Context context = container.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        container.removeAllViews();

        List<String> labels = new ArrayList<>(items.size());
        for (OptionItem item : items) {
            labels.add(item.label);
        }
        WellnessOptionVisualResolver.ScaleType scale = WellnessOptionVisualResolver.detectScale(labels);
        boolean hasAnySelection = hasSelection(items, selectedOptionId);

        WellnessOptionCardBinder binder = new WellnessOptionCardBinder(context, selectedOptionId);
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < items.size(); i++) {
            OptionItem item = items.get(i);
            WellnessOptionVisualResolver.OptionVisual visual =
                    WellnessOptionVisualResolver.resolve(item.label, scale);
            String resolvedSubtitle = !item.subtitle.isEmpty() ? item.subtitle : visual.defaultSubtitle;

            View cardView = inflater.inflate(R.layout.collaborator_item_wellness_option_card, container, false);
            BoundCard bound = new BoundCard(item, resolvedSubtitle, visual, cardView);

            bound.labelView.setText(item.label);
            if (!resolvedSubtitle.isEmpty()) {
                bound.subtitleView.setText(resolvedSubtitle);
                bound.subtitleView.setVisibility(View.VISIBLE);
            } else {
                bound.subtitleView.setVisibility(View.GONE);
            }

            if (visual.iconRes != 0) {
                int boxSize = Math.round((visual.isIllustratedFace() ? 56f : 42f) * density);
                int iconSize = Math.round((visual.isIllustratedFace() ? 52f : 26f) * density);
                LinearLayout.LayoutParams boxLp = (LinearLayout.LayoutParams) bound.iconBox.getLayoutParams();
                boxLp.width = boxSize;
                boxLp.height = boxSize;
                bound.iconBox.setLayoutParams(boxLp);
                FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
                bound.iconView.setLayoutParams(iconLp);
                bound.iconView.setImageResource(visual.iconRes);
                bound.iconView.setVisibility(View.VISIBLE);
                bound.badgeText.setVisibility(View.GONE);
            } else {
                int boxSize = Math.round(40f * density);
                LinearLayout.LayoutParams boxLp = (LinearLayout.LayoutParams) bound.iconBox.getLayoutParams();
                boxLp.width = boxSize;
                boxLp.height = boxSize;
                bound.iconBox.setLayoutParams(boxLp);
                bound.iconView.setVisibility(View.GONE);
                bound.badgeText.setText(String.format(Locale.ROOT, "%d", i + 1));
                bound.badgeText.setVisibility(View.VISIBLE);
            }

            boolean isSelected = (item.optionId == selectedOptionId);
            binder.applyVisualState(bound, isSelected, hasAnySelection, false);

            cardView.setOnClickListener(v -> {
                if (binder.currentSelectedId == item.optionId) {
                    return;
                }
                binder.currentSelectedId = item.optionId;
                for (BoundCard other : binder.boundCards) {
                    boolean nowSelected = (other.item.optionId == item.optionId);
                    binder.applyVisualState(other, nowSelected, true, true);
                }
                if (listener != null) {
                    listener.onOptionSelected(item.optionId);
                }
            });

            binder.boundCards.add(bound);
            container.addView(cardView);
        }

        CollaboratorThemeHelper.applyReadableTypeface(container);
        for (BoundCard bound : binder.boundCards) {
            boolean isSelected = bound.item.optionId == selectedOptionId;
            bound.labelView.setTypeface(CollaboratorThemeHelper.readableTypeface(
                    isSelected ? Typeface.BOLD : Typeface.NORMAL));
        }
        return binder;
    }

    int getSelectedOptionId() {
        return currentSelectedId;
    }

    private static boolean hasSelection(List<OptionItem> items, int selectedId) {
        for (OptionItem item : items) {
            if (item.optionId == selectedId) return true;
        }
        return false;
    }

    private void applyVisualState(BoundCard bound, boolean selected, boolean hasAnySelection, boolean animate) {
        float density = context.getResources().getDisplayMetrics().density;
        int accent = ContextCompat.getColor(context, bound.visual.accentColorRes);
        int cardBg = ContextCompat.getColor(context,
                selected ? R.color.collab_wellness_card_selected_bg : R.color.collab_wellness_card_bg);
        int defaultBorder = ContextCompat.getColor(context, R.color.collab_wellness_card_border);
        int selectedBorder = ContextCompat.getColor(context, R.color.collab_wellness_selected_border);
        int selectedGlow = ContextCompat.getColor(context, R.color.collab_wellness_selected_glow);
        int violetPrimary = ContextCompat.getColor(context, R.color.collab_wellness_violet_primary);
        int checkTint = ContextCompat.getColor(context, R.color.collab_wellness_check_icon_tint);
        int ringColor = ContextCompat.getColor(context, R.color.collab_wellness_radio_ring);

        // Fondo de tarjeta con resplandor y borde violeta luminoso al seleccionar
        bound.root.setBackground(buildCardBackground(
                density, cardBg, defaultBorder, selectedBorder, selectedGlow, selected));

        // Halo decorativo muy sutil detrás de la carita 3D / ícono
        GradientDrawable iconBg = new GradientDrawable();
        if (bound.visual.isIllustratedFace()) {
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(withAlpha(accent, selected ? 44 : 20));
        } else {
            iconBg.setShape(GradientDrawable.RECTANGLE);
            iconBg.setCornerRadius(12f * density);
            iconBg.setColor(withAlpha(accent, selected ? 46 : 24));
        }
        bound.iconBox.setBackground(iconBg);

        if (bound.visual.iconRes == 0) {
            bound.badgeText.setTextColor(selected ? selectedBorder : ContextCompat.getColor(context, R.color.collab_purple_accent));
        }

        // Tipografía
        bound.labelView.setTextColor(ContextCompat.getColor(context, R.color.collab_text_primary));
        bound.labelView.setTypeface(CollaboratorThemeHelper.readableTypeface(
                selected ? Typeface.BOLD : Typeface.NORMAL));

        // Indicador circular derecho en violeta luminoso
        GradientDrawable checkBg = new GradientDrawable();
        checkBg.setShape(GradientDrawable.OVAL);
        if (selected) {
            checkBg.setColor(violetPrimary);
            checkBg.setStroke(Math.round(1.3f * density), selectedBorder);
            bound.checkRing.setBackground(checkBg);
            ImageViewCompat.setImageTintList(bound.checkIcon, ColorStateList.valueOf(checkTint));
            bound.checkIcon.setVisibility(View.VISIBLE);
        } else {
            checkBg.setColor(Color.TRANSPARENT);
            checkBg.setStroke(Math.round(1.6f * density), ringColor);
            bound.checkRing.setBackground(checkBg);
            bound.checkIcon.setVisibility(View.INVISIBLE);
        }

        // Accesibilidad para lectores de pantalla
        bound.root.setSelected(selected);
        String desc = bound.resolvedSubtitle.isEmpty()
                ? bound.item.label
                : bound.item.label + ". " + bound.resolvedSubtitle;
        bound.root.setContentDescription(desc);
        ViewCompat.setStateDescription(bound.root, selected ? "Seleccionada" : "Sin seleccionar");

        float targetCardAlpha = (!hasAnySelection || selected) ? 1f : 0.86f;
        float targetIconScale = selected ? 1.06f : 1f;
        boolean canAnimate = animate && animationsEnabled(context);
        View targetIcon = bound.visual.iconRes != 0 ? bound.iconView : bound.badgeText;

        targetIcon.animate().cancel();
        bound.checkIcon.animate().cancel();
        bound.root.animate().cancel();

        if (!canAnimate) {
            bound.root.setAlpha(targetCardAlpha);
            bound.root.setScaleX(1f);
            bound.root.setScaleY(1f);
            targetIcon.setScaleX(targetIconScale);
            targetIcon.setScaleY(targetIconScale);
            bound.checkIcon.setAlpha(selected ? 1f : 0f);
            bound.checkIcon.setScaleX(selected ? 1f : 0.7f);
            bound.checkIcon.setScaleY(selected ? 1f : 0.7f);
            return;
        }

        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        bound.root.animate()
                .alpha(targetCardAlpha)
                .setDuration(ANIM_DURATION_MS)
                .setInterpolator(interpolator)
                .start();

        if (selected) {
            bound.root.setScaleX(0.985f);
            bound.root.setScaleY(0.985f);
            bound.root.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIM_DURATION_MS)
                    .setInterpolator(interpolator)
                    .start();

            targetIcon.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(ANIM_DURATION_MS)
                    .setInterpolator(interpolator)
                    .start();

            bound.checkIcon.setAlpha(0f);
            bound.checkIcon.setScaleX(0.65f);
            bound.checkIcon.setScaleY(0.65f);
            bound.checkIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ANIM_DURATION_MS)
                    .setInterpolator(interpolator)
                    .start();
        } else {
            targetIcon.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180L)
                    .setInterpolator(interpolator)
                    .start();
        }
    }

    private static Drawable buildCardBackground(
            float density,
            int cardBg,
            int defaultBorder,
            int selectedBorder,
            int selectedGlow,
            boolean selected
    ) {
        if (!selected) {
            GradientDrawable normal = new GradientDrawable();
            normal.setShape(GradientDrawable.RECTANGLE);
            normal.setCornerRadius(18f * density);
            normal.setColor(cardBg);
            normal.setStroke(Math.round(1f * density), defaultBorder);
            return normal;
        }
        GradientDrawable outerGlow = new GradientDrawable();
        outerGlow.setShape(GradientDrawable.RECTANGLE);
        outerGlow.setCornerRadius(19f * density);
        outerGlow.setColor(Color.TRANSPARENT);
        outerGlow.setStroke(Math.round(3.2f * density), selectedGlow);

        GradientDrawable innerCard = new GradientDrawable();
        innerCard.setShape(GradientDrawable.RECTANGLE);
        innerCard.setCornerRadius(18f * density);
        innerCard.setColor(cardBg);
        innerCard.setStroke(Math.round(1.6f * density), selectedBorder);

        LayerDrawable layers = new LayerDrawable(new Drawable[]{outerGlow, innerCard});
        int inset = Math.round(1.4f * density);
        layers.setLayerInset(1, inset, inset, inset, inset);
        return layers;
    }

    static boolean animationsEnabled(Context context) {
        try {
            float scale = Settings.Global.getFloat(
                    context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f);
            return scale > 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
