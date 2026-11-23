package pe.net.ws.kpiweb.features.collaborator.wellness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import pe.net.ws.kpiweb.R;

public class WellnessOptionVisualResolverTest {

    @Test
    public void detectsWeeklyAndMonthlyMoodScalesRegardlessOfOrder() {
        List<String> weekly = Arrays.asList("Muy bien", "Bien", "Neutral", "Mal", "Muy mal");
        List<String> monthly = Arrays.asList("Muy mal", "Mal", "Regular", "Bien", "Muy bien");

        assertEquals(WellnessOptionVisualResolver.ScaleType.MOOD_EXPRESSION,
                WellnessOptionVisualResolver.detectScale(weekly));
        assertEquals(WellnessOptionVisualResolver.ScaleType.MOOD_EXPRESSION,
                WellnessOptionVisualResolver.detectScale(monthly));

        WellnessOptionVisualResolver.OptionVisual muyBien = WellnessOptionVisualResolver.resolve(
                "Muy bien", WellnessOptionVisualResolver.ScaleType.MOOD_EXPRESSION);
        assertEquals(R.drawable.collab_ic_mood_very_good, muyBien.iconRes);
        assertEquals(R.color.collab_mood_very_good, muyBien.accentColorRes);
        org.junit.Assert.assertTrue(muyBien.isIllustratedFace());
        org.junit.Assert.assertFalse(muyBien.defaultSubtitle.isEmpty());

        WellnessOptionVisualResolver.OptionVisual regular = WellnessOptionVisualResolver.resolve(
                "Regular", WellnessOptionVisualResolver.ScaleType.MOOD_EXPRESSION);
        assertEquals(R.drawable.collab_ic_mood_neutral, regular.iconRes);
        assertEquals(R.color.collab_mood_neutral, regular.accentColorRes);

        WellnessOptionVisualResolver.OptionVisual muyMal = WellnessOptionVisualResolver.resolve(
                "Muy mal", WellnessOptionVisualResolver.ScaleType.MOOD_EXPRESSION);
        assertEquals(R.drawable.collab_ic_mood_very_bad, muyMal.iconRes);
        assertEquals(R.color.collab_mood_very_bad, muyMal.accentColorRes);
    }

    @Test
    public void detectsFrequencyAndAgreementWithoutForcingFaces() {
        List<String> freq = Arrays.asList("Siempre", "Casi siempre", "A veces", "Rara vez", "Nunca");
        assertEquals(WellnessOptionVisualResolver.ScaleType.FREQUENCY,
                WellnessOptionVisualResolver.detectScale(freq));
        WellnessOptionVisualResolver.OptionVisual siempre = WellnessOptionVisualResolver.resolve(
                "Siempre", WellnessOptionVisualResolver.ScaleType.FREQUENCY);
        assertEquals(R.drawable.collab_ic_scale_freq_5, siempre.iconRes);
        assertNotEquals(R.drawable.collab_ic_mood_very_good, siempre.iconRes);

        List<String> agreement = Arrays.asList(
                "Totalmente en desacuerdo", "En desacuerdo", "Neutral", "De acuerdo", "Totalmente de acuerdo");
        assertEquals(WellnessOptionVisualResolver.ScaleType.AGREEMENT,
                WellnessOptionVisualResolver.detectScale(agreement));
        WellnessOptionVisualResolver.OptionVisual totallyAgree = WellnessOptionVisualResolver.resolve(
                "Totalmente de acuerdo", WellnessOptionVisualResolver.ScaleType.AGREEMENT);
        assertEquals(R.drawable.collab_ic_scale_agree_5, totallyAgree.iconRes);
    }

    @Test
    public void keepsPlainScaleForNumericOrUnrelatedTextOptions() {
        List<String> numeric = Arrays.asList("1", "2", "3", "4", "5");
        assertEquals(WellnessOptionVisualResolver.ScaleType.PLAIN,
                WellnessOptionVisualResolver.detectScale(numeric));
        assertEquals(0, WellnessOptionVisualResolver.resolve(
                "3", WellnessOptionVisualResolver.ScaleType.PLAIN).iconRes);

        List<String> custom = Arrays.asList("Trabajo remoto", "Híbrido", "Presencial");
        assertEquals(WellnessOptionVisualResolver.ScaleType.PLAIN,
                WellnessOptionVisualResolver.detectScale(custom));
        assertEquals(0, WellnessOptionVisualResolver.resolve(
                "Híbrido", WellnessOptionVisualResolver.ScaleType.PLAIN).iconRes);
    }
}
