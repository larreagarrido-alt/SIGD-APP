package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.util.Map;

/** Replaceable local inference boundary. A future validated model can implement it. */
public interface WellnessRecommendationProvider {
    WellnessRecommendationRules.Result recommend(WellnessResponseAnalyzer.Analysis analysis,
                                                 Map<String, Integer> preferences);
}
