package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Deterministic ranking: explicit difficulties outrank preference feedback. */
public final class WellnessRecommendationEngine implements WellnessRecommendationProvider {
    @Override
    public WellnessRecommendationRules.Result recommend(WellnessResponseAnalyzer.Analysis analysis,
                                                         Map<String, Integer> preferences) {
        List<String> positives = new ArrayList<>();
        List<String> attention = new ArrayList<>();
        List<WellnessResponseAnalyzer.Topic> ranked = new ArrayList<>(analysis.topics);
        for (WellnessResponseAnalyzer.Topic topic : ranked) {
            if (topic.answer <= 2) positives.add(topic.name);
            if (topic.answer >= 4) attention.add(topic.name);
        }
        Map<String, Integer> weights = preferences == null ? Collections.emptyMap() : preferences;
        ranked.sort(Comparator.<WellnessResponseAnalyzer.Topic>comparingInt(
                topic -> priority(topic, weights)).reversed());
        List<WellnessRecommendationRules.Recommendation> suggestions = new ArrayList<>();
        for (WellnessResponseAnalyzer.Topic topic : ranked) {
            if (suggestions.size() == 4) break;
            WellnessKnowledgeCatalog.Entry entry = WellnessKnowledgeCatalog.find(topic.id, topic.answer >= 4);
            if (entry != null && entry.source != null)
                suggestions.add(new WellnessRecommendationRules.Recommendation(topic.id,
                        entry.title, entry.explanation, entry.action, topic.name, reason(topic), entry.source));
        }
        String summary;
        if (attention.isEmpty()) {
            summary = "En " + (analysis.overall <= 2 ? "tu experiencia general positiva" : "este mes")
                    + " señalaste varios aspectos favorables. Estas ideas buscan ayudarte a cuidarlos.";
        } else if (positives.isEmpty()) {
            summary = "Señalaste dificultades en " + attention.get(0)
                    + ". Puedes revisar estas sugerencias a tu ritmo y pedir apoyo si lo necesitas.";
        } else {
            summary = "Señalaste aspectos favorables en " + positives.get(0)
                    + " y dificultades en " + attention.get(0) + ". Estas sugerencias parten de esa combinación.";
        }
        return new WellnessRecommendationRules.Result(summary,
                positives.isEmpty() ? "No señalaste un aspecto claramente favorable entre los temas revisados."
                        : String.join(" · ", positives),
                attention.isEmpty() ? "No señalaste dificultades marcadas entre los temas revisados."
                        : String.join(" · ", attention), suggestions);
    }

    private int priority(WellnessResponseAnalyzer.Topic topic, Map<String, Integer> preferences) {
        int severity = topic.answer >= 4 ? topic.answer * 10 : topic.answer == 3 ? 18 : 8;
        int preference = Math.max(-2, Math.min(2, weights(topic.id, preferences)));
        return severity + preference * 2;
    }

    private int weights(String id, Map<String, Integer> preferences) {
        Integer weight = preferences.get(id);
        return weight == null ? 0 : weight;
    }

    private String reason(WellnessResponseAnalyzer.Topic topic) {
        String response = topic.answer <= 2 ? "favorable" : topic.answer == 3 ? "regular" : "difícil";
        return "Por tu respuesta " + response + " sobre " + topic.name + " este mes.";
    }
}
