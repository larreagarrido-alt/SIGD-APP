package pe.net.ws.kpiweb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;

import org.junit.Test;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessRecommendationRules;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessPersonalizationEngine;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessKnowledgeCatalog;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessResponseAnalyzer;

public class WellnessRecommendationRulesTest {
    @Test public void difficultWorkloadGetsRelevantSuggestionWithoutClinicalScore() {
        int[] answers = {2, 5, 2, 2, 2, 2, 2, 2, 2, 2};
        WellnessRecommendationRules.Result result = WellnessRecommendationRules.forMonthlyAnswers(answers);
        assertEquals("carga de tareas", result.recommendations.get(0).topic);
        assertTrue(result.attention.contains("carga de tareas"));
        assertTrue(result.positives.contains("comunicación"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompleteAnswersDoNotCreateResult() {
        WellnessRecommendationRules.forMonthlyAnswers(new int[]{1, 2});
    }

    @Test public void positiveAnswersAcknowledgeStrengthsWithoutInventingProblems() {
        WellnessRecommendationRules.Result result = WellnessRecommendationRules.forMonthlyAnswers(
                new int[]{1, 1, 2, 1, 2, 1, 2, 1, 2, 1});
        assertTrue(result.summary.contains("positiva"));
        assertTrue(result.attention.contains("No señalaste dificultades"));
        assertEquals(4, result.recommendations.size());
        assertTrue(result.recommendations.get(0).reason.contains("favorable"));
    }

    @Test public void communicationDifficultyAndMixedAnswersChangeSummaryAndPriority() {
        WellnessRecommendationRules.Result result = WellnessRecommendationRules.forMonthlyAnswers(
                new int[]{2, 3, 2, 5, 2, 2, 2, 3, 3, 3});
        assertEquals("comunicación", result.recommendations.get(0).topic);
        assertTrue(result.summary.contains("comunicación"));
        assertTrue(result.positives.contains("trabajo y descanso"));
    }

    @Test public void feedbackChangesFutureTieOrderButCannotHideSevereDifficulty() {
        int[] answers = {3, 5, 3, 4, 3, 3, 3, 3, 3, 3};
        Map<String, Integer> preferences = new HashMap<>();
        preferences.put("communication", WellnessPersonalizationEngine.updatedWeight(0, true));
        preferences.put("workload", WellnessPersonalizationEngine.updatedWeight(0, false));
        WellnessRecommendationRules.Result result = WellnessRecommendationRules.forMonthlyAnswers(answers, preferences);
        assertEquals("workload", result.recommendations.get(0).id);

        int[] ties = {3, 4, 3, 4, 3, 3, 3, 3, 3, 3};
        WellnessRecommendationRules.Result before = WellnessRecommendationRules.forMonthlyAnswers(ties);
        WellnessRecommendationRules.Result after = WellnessRecommendationRules.forMonthlyAnswers(ties, preferences);
        assertEquals("workload", before.recommendations.get(0).id);
        assertEquals("communication", after.recommendations.get(0).id);
        assertEquals("workload", WellnessRecommendationRules.forMonthlyAnswers(ties, Collections.emptyMap())
                .recommendations.get(0).id);
        assertFalse(after.recommendations.get(0).reason.isEmpty());
    }

    @Test public void feedbackWeightIsBounded() {
        assertEquals(2, WellnessPersonalizationEngine.updatedWeight(2, true));
        assertEquals(-2, WellnessPersonalizationEngine.updatedWeight(-2, false));
    }

    @Test public void allRecommendationsHaveVettedSourceAndPracticalAction() {
        int[][] examples = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {3, 5, 3, 2, 2, 4, 3, 3, 3, 4},
                {2, 3, 3, 5, 4, 2, 2, 3, 3, 3}
        };
        for (int[] answers : examples) {
            WellnessRecommendationRules.Result result = WellnessRecommendationRules.forMonthlyAnswers(answers);
            assertTrue(result.recommendations.size() >= 3 && result.recommendations.size() <= 5);
            for (WellnessRecommendationRules.Recommendation recommendation : result.recommendations) {
                assertFalse(recommendation.action.isEmpty());
                assertFalse(recommendation.reason.isEmpty());
                assertTrue(WellnessKnowledgeCatalog.sources().contains(recommendation.source));
            }
        }
        assertEquals(4, WellnessKnowledgeCatalog.sources().size());
        for (WellnessKnowledgeCatalog.Source source : WellnessKnowledgeCatalog.sources()) {
            URI uri = URI.create(source.url);
            assertEquals("https", uri.getScheme());
            assertTrue(uri.getHost().equals("www.who.int") || uri.getHost().equals("www.paho.org")
                    || uri.getHost().equals("www.cdc.gov") || uri.getHost().equals("www.gob.pe"));
            assertFalse(source.published.isEmpty());
            assertFalse(source.reviewed.isEmpty());
            assertFalse(source.supports.isEmpty());
        }
    }

    @Test public void professionalSupportSuggestionRequiresTwoConsecutiveDifficultMonths() {
        assertTrue(WellnessResponseAnalyzer.shouldSuggestProfessionalSupport(4, 5));
        assertFalse(WellnessResponseAnalyzer.shouldSuggestProfessionalSupport(4, 0));
        assertFalse(WellnessResponseAnalyzer.shouldSuggestProfessionalSupport(4, 2));
    }
}
