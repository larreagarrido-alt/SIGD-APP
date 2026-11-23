package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;

/**
 * Analiza las alternativas reales de una pregunta de bienestar y asigna
 * ilustraciones vectoriales 3D o íconos de escala acordes a su contenido
 * (ánimo/satisfacción, frecuencia, acuerdo o texto simple), sin forzar
 * caritas cuando no corresponden ni alterar el texto o puntaje original.
 */
final class WellnessOptionVisualResolver {

    enum ScaleType {
        MOOD_EXPRESSION,
        FREQUENCY,
        AGREEMENT,
        YES_NO,
        PLAIN
    }

    static final class OptionVisual {
        final ScaleType scaleType;
        final int iconRes;
        final int accentColorRes;
        final String defaultSubtitle;

        OptionVisual(ScaleType scaleType, int iconRes, int accentColorRes, String defaultSubtitle) {
            this.scaleType = scaleType;
            this.iconRes = iconRes;
            this.accentColorRes = accentColorRes;
            this.defaultSubtitle = defaultSubtitle == null ? "" : defaultSubtitle;
        }

        boolean isIllustratedFace() {
            return scaleType == ScaleType.MOOD_EXPRESSION && iconRes != 0;
        }
    }

    private WellnessOptionVisualResolver() {
    }

    static ScaleType detectScale(List<String> optionLabels) {
        if (optionLabels == null || optionLabels.isEmpty()) {
            return ScaleType.PLAIN;
        }
        int moodMatches = 0;
        int frequencyMatches = 0;
        int agreementMatches = 0;
        int yesNoMatches = 0;

        for (String raw : optionLabels) {
            String key = normalize(raw);
            if (matchMoodLevel(key) > 0)
                moodMatches++;
            if (matchFrequencyLevel(key) > 0)
                frequencyMatches++;
            if (matchAgreementLevel(key) > 0)
                agreementMatches++;
            if ("si".equals(key) || "no".equals(key))
                yesNoMatches++;
        }

        int total = optionLabels.size();
        if (total == 2 && yesNoMatches == 2) {
            return ScaleType.YES_NO;
        }
        if (frequencyMatches >= Math.max(2, total - 1)) {
            return ScaleType.FREQUENCY;
        }
        if (agreementMatches >= Math.max(2, total - 1)) {
            return ScaleType.AGREEMENT;
        }
        if (moodMatches >= Math.max(2, total - 1)) {
            return ScaleType.MOOD_EXPRESSION;
        }
        return ScaleType.PLAIN;
    }

    static OptionVisual resolve(String optionLabel, ScaleType questionScale) {
        String key = normalize(optionLabel);
        switch (questionScale) {
            case MOOD_EXPRESSION:
                return moodVisualForLevel(matchMoodLevel(key));
            case FREQUENCY:
                return frequencyVisualForLevel(matchFrequencyLevel(key));
            case AGREEMENT:
                return agreementVisualForLevel(matchAgreementLevel(key));
            case YES_NO:
                if ("si".equals(key)) {
                    return new OptionVisual(ScaleType.YES_NO, R.drawable.collab_ic_scale_agree_5,
                            R.color.collab_mood_very_good, "");
                }
                if ("no".equals(key)) {
                    return new OptionVisual(ScaleType.YES_NO, R.drawable.collab_ic_scale_agree_1,
                            R.color.collab_mood_very_bad, "");
                }
                return plainVisual();
            case PLAIN:
            default:
                return plainVisual();
        }
    }

    static OptionVisual resolveSingle(String optionLabel) {
        String key = normalize(optionLabel);
        int mood = matchMoodLevel(key);
        if (mood > 0)
            return moodVisualForLevel(mood);
        int freq = matchFrequencyLevel(key);
        if (freq > 0)
            return frequencyVisualForLevel(freq);
        int agree = matchAgreementLevel(key);
        if (agree > 0)
            return agreementVisualForLevel(agree);
        if ("si".equals(key)) {
            return new OptionVisual(ScaleType.YES_NO, R.drawable.collab_ic_scale_agree_5, R.color.collab_mood_very_good,
                    "");
        }
        if ("no".equals(key)) {
            return new OptionVisual(ScaleType.YES_NO, R.drawable.collab_ic_scale_agree_1, R.color.collab_mood_very_bad,
                    "");
        }
        return plainVisual();
    }

    private static OptionVisual moodVisualForLevel(int level) {
        switch (level) {
            case 5:
                return new OptionVisual(ScaleType.MOOD_EXPRESSION, R.drawable.collab_ic_mood_very_good,
                        R.color.collab_mood_very_good, "Energía positiva y óptimo ánimo");
            case 4:
                return new OptionVisual(ScaleType.MOOD_EXPRESSION, R.drawable.collab_ic_mood_good,
                        R.color.collab_mood_good, "Tranquilo y con buen ritmo");
            case 3:
                return new OptionVisual(ScaleType.MOOD_EXPRESSION, R.drawable.collab_ic_mood_neutral,
                        R.color.collab_mood_neutral, "Estable y equilibrado");
            case 2:
                return new OptionVisual(ScaleType.MOOD_EXPRESSION, R.drawable.collab_ic_mood_bad,
                        R.color.collab_mood_bad, "Algo de cansancio o tensión");
            case 1:
                return new OptionVisual(ScaleType.MOOD_EXPRESSION, R.drawable.collab_ic_mood_very_bad,
                        R.color.collab_mood_very_bad, "Sobrecarga; necesito pausa o apoyo");
            default:
                return plainVisual();
        }
    }

    private static OptionVisual frequencyVisualForLevel(int level) {
        switch (level) {
            case 5:
                return new OptionVisual(ScaleType.FREQUENCY, R.drawable.collab_ic_scale_freq_5,
                        R.color.collab_mood_very_good, "");
            case 4:
                return new OptionVisual(ScaleType.FREQUENCY, R.drawable.collab_ic_scale_freq_4,
                        R.color.collab_mood_good, "");
            case 3:
                return new OptionVisual(ScaleType.FREQUENCY, R.drawable.collab_ic_scale_freq_3,
                        R.color.collab_mood_neutral, "");
            case 2:
                return new OptionVisual(ScaleType.FREQUENCY, R.drawable.collab_ic_scale_freq_2, R.color.collab_mood_bad,
                        "");
            case 1:
                return new OptionVisual(ScaleType.FREQUENCY, R.drawable.collab_ic_scale_freq_1,
                        R.color.collab_mood_very_bad, "");
            default:
                return plainVisual();
        }
    }

    private static OptionVisual agreementVisualForLevel(int level) {
        switch (level) {
            case 5:
                return new OptionVisual(ScaleType.AGREEMENT, R.drawable.collab_ic_scale_agree_5,
                        R.color.collab_mood_very_good, "");
            case 4:
                return new OptionVisual(ScaleType.AGREEMENT, R.drawable.collab_ic_scale_agree_4,
                        R.color.collab_mood_good, "");
            case 3:
                return new OptionVisual(ScaleType.AGREEMENT, R.drawable.collab_ic_scale_agree_3,
                        R.color.collab_mood_neutral, "");
            case 2:
                return new OptionVisual(ScaleType.AGREEMENT, R.drawable.collab_ic_scale_agree_2,
                        R.color.collab_mood_bad, "");
            case 1:
                return new OptionVisual(ScaleType.AGREEMENT, R.drawable.collab_ic_scale_agree_1,
                        R.color.collab_mood_very_bad, "");
            default:
                return plainVisual();
        }
    }

    private static OptionVisual plainVisual() {
        return new OptionVisual(ScaleType.PLAIN, 0, R.color.collab_purple_accent, "");
    }

    private static int matchMoodLevel(String key) {
        if (key.isEmpty())
            return 0;
        if ("muy bien".equals(key) || "excelente".equals(key) || "muy satisfecho".equals(key)
                || "muy satisfecha".equals(key) || "muy satisfecho/a".equals(key)
                || "muy bueno".equals(key) || "muy buena".equals(key) || "muy alto".equals(key)) {
            return 5;
        }
        if ("bien".equals(key) || "bueno".equals(key) || "buena".equals(key)
                || "satisfecho".equals(key) || "satisfecha".equals(key)
                || "satisfecho/a".equals(key) || "alto".equals(key)) {
            return 4;
        }
        if ("neutral".equals(key) || "regular".equals(key) || "moderado".equals(key)
                || "normal".equals(key) || "intermedio".equals(key)) {
            return 3;
        }
        if ("mal".equals(key) || "malo".equals(key) || "mala".equals(key)
                || "insatisfecho".equals(key) || "insatisfecha".equals(key)
                || "insatisfecho/a".equals(key) || "poco satisfecho".equals(key)
                || "poco satisfecha".equals(key) || "bajo".equals(key)) {
            return 2;
        }
        if ("muy mal".equals(key) || "muy malo".equals(key) || "muy mala".equals(key)
                || "muy insatisfecho".equals(key) || "muy insatisfecha".equals(key)
                || "muy insatisfecho/a".equals(key) || "muy bajo".equals(key)
                || "pesimo".equals(key)) {
            return 1;
        }
        return 0;
    }

    private static int matchFrequencyLevel(String key) {
        if (key.isEmpty())
            return 0;
        if ("siempre".equals(key) || "todos los dias".equals(key) || "muy frecuentemente".equals(key)) {
            return 5;
        }
        if ("casi siempre".equals(key) || "frecuentemente".equals(key) || "a menudo".equals(key)
                || "con frecuencia".equals(key)) {
            return 4;
        }
        if ("a veces".equals(key) || "ocasionalmente".equals(key) || "algunas veces".equals(key)) {
            return 3;
        }
        if ("rara vez".equals(key) || "casi nunca".equals(key) || "pocas veces".equals(key)
                || "raramente".equals(key)) {
            return 2;
        }
        if ("nunca".equals(key) || "jamas".equals(key)) {
            return 1;
        }
        return 0;
    }

    private static int matchAgreementLevel(String key) {
        if (key.isEmpty())
            return 0;
        if ("totalmente de acuerdo".equals(key) || "muy de acuerdo".equals(key)
                || "completamente de acuerdo".equals(key)) {
            return 5;
        }
        if ("de acuerdo".equals(key) || "algo de acuerdo".equals(key)
                || "moderadamente de acuerdo".equals(key)) {
            return 4;
        }
        if ("neutral".equals(key) || "ni de acuerdo ni en desacuerdo".equals(key)
                || "indiferente".equals(key)) {
            return 3;
        }
        if ("en desacuerdo".equals(key) || "algo en desacuerdo".equals(key)
                || "moderadamente en desacuerdo".equals(key)) {
            return 2;
        }
        if ("totalmente en desacuerdo".equals(key) || "muy en desacuerdo".equals(key)
                || "completamente en desacuerdo".equals(key)) {
            return 1;
        }
        return 0;
    }

    static String normalize(String value) {
        if (value == null)
            return "";
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("\\s+", " ");
    }
}
