package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessRecommendationVisualResolver.VisualStyle;

/**
 * Infla y estiliza cada tarjeta independiente de recomendación de Bienestar Psicológico.
 * Aplica tipografía legible sin cursiva, ícono individual con contenedor pastel suave
 * y animación de aparición discreta respetando la preferencia de reducir movimiento.
 */
final class WellnessRecommendationCardBinder {

    private static final long ENTRANCE_DURATION_MS = 180L;

    private WellnessRecommendationCardBinder() {}

    static View inflateAndBind(ViewGroup parent,
                               WellnessRecommendationRules.Recommendation recommendation,
                               String descriptionText,
                               int index) {
        Context context = parent.getContext();
        View card = LayoutInflater.from(context)
                .inflate(R.layout.collaborator_item_wellness_recommendation, parent, false);

        VisualStyle style = WellnessRecommendationVisualResolver.resolve(recommendation);
        float density = context.getResources().getDisplayMetrics().density;
        boolean darkMode = (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int effectiveAccent = darkMode
                ? style.accentColor
                : ColorUtils.blendARGB(style.accentColor, 0xFF241532, 0.42f);

        View iconBox = card.findViewById(R.id.wellness_rec_icon_box);
        ImageView ivIcon = card.findViewById(R.id.wellness_rec_icon);
        TextView tvTitle = card.findViewById(R.id.wellness_rec_title);
        TextView tvCategory = card.findViewById(R.id.wellness_rec_category);
        TextView tvDescription = card.findViewById(R.id.wellness_rec_description);

        int r = Color.red(effectiveAccent);
        int g = Color.green(effectiveAccent);
        int b = Color.blue(effectiveAccent);

        if (iconBox != null) {
            GradientDrawable boxBg = new GradientDrawable();
            boxBg.setShape(GradientDrawable.RECTANGLE);
            boxBg.setCornerRadius(13f * density);
            boxBg.setColor(Color.argb(darkMode ? 38 : 28, r, g, b));
            boxBg.setStroke(Math.max(1, Math.round(1f * density)), Color.argb(darkMode ? 72 : 58, r, g, b));
            iconBox.setBackground(boxBg);
        }

        if (ivIcon != null) {
            ivIcon.setImageResource(style.iconRes);
            ivIcon.setColorFilter(effectiveAccent);
        }

        if (tvTitle != null) {
            tvTitle.setText(recommendation.title);
            tvTitle.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        }

        if (tvCategory != null) {
            if (style.categoryLabel != null && !style.categoryLabel.isEmpty()) {
                tvCategory.setText(style.categoryLabel);
                tvCategory.setTextColor(effectiveAccent);
                tvCategory.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
                GradientDrawable pillBg = new GradientDrawable();
                pillBg.setShape(GradientDrawable.RECTANGLE);
                pillBg.setCornerRadius(99f * density);
                pillBg.setColor(Color.argb(darkMode ? 30 : 24, r, g, b));
                tvCategory.setBackground(pillBg);
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }
        }

        if (tvDescription != null) {
            tvDescription.setText(descriptionText);
            tvDescription.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.NORMAL));
        }

        parent.addView(card);

        if (WellnessOptionCardBinder.animationsEnabled(context)) {
            card.setAlpha(0f);
            card.setTranslationY(8f * density);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(Math.min(index * 40L, 200L))
                    .setDuration(ENTRANCE_DURATION_MS)
                    .start();
        }

        return card;
    }

    static LinearLayout extraSlot(View card) {
        return card.findViewById(R.id.wellness_rec_extra_slot);
    }
}
