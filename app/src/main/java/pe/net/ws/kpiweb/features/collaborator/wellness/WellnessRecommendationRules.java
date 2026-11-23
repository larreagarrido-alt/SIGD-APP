package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Local, non-clinical orientation from the ten original monthly questions. */
public final class WellnessRecommendationRules {
    public static final class Recommendation {
        public final String id;
        public final String title;
        public final String detail;
        public final String action;
        public final String topic;
        public final String reason;
        public final WellnessKnowledgeCatalog.Source source;

        Recommendation(String id, String title, String detail, String action, String topic,
                       String reason, WellnessKnowledgeCatalog.Source source) {
            this.id = id;
            this.title = title;
            this.detail = detail;
            this.action = action;
            this.topic = topic;
            this.reason = reason;
            this.source = source;
        }
    }

    public static final class Result {
        public final String summary;
        public final String positives;
        public final String attention;
        public final List<Recommendation> recommendations;

        Result(String summary, String positives, String attention, List<Recommendation> recommendations) {
            this.summary = summary;
            this.positives = positives;
            this.attention = attention;
            this.recommendations = Collections.unmodifiableList(recommendations);
        }
    }

    private WellnessRecommendationRules() {}

    public static Result forMonthlyAnswers(int[] answers) {
        return forMonthlyAnswers(answers, Collections.emptyMap());
    }

    public static Result forMonthlyAnswers(int[] answers, Map<String, Integer> preferences) {
        return new WellnessRecommendationEngine().recommend(
                new WellnessResponseAnalyzer().analyze(answers), preferences);
    }
}
