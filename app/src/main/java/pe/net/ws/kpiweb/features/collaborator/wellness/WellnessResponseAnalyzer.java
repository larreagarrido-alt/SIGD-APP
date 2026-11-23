package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Maps each question to a topic. 1 = very good; 5 = very bad. No clinical score. */
public final class WellnessResponseAnalyzer {
    public static boolean shouldSuggestProfessionalSupport(int currentOverall, int previousOverall) {
        return currentOverall >= 4 && currentOverall <= 5 && previousOverall >= 4 && previousOverall <= 5;
    }
    public static final class Topic {
        public final String id;
        public final String name;
        public final int answer;

        Topic(String id, String name, int answer) {
            this.id = id;
            this.name = name;
            this.answer = answer;
        }
    }

    public static final class Analysis {
        public final List<Topic> topics;
        public final int overall;

        Analysis(List<Topic> topics, int overall) {
            this.topics = Collections.unmodifiableList(topics);
            this.overall = overall;
        }
    }

    public Analysis analyze(int[] answers) {
        if (answers == null || answers.length != 10) throw new IllegalArgumentException("Ten answers required");
        for (int answer : answers) if (answer < 1 || answer > 5)
            throw new IllegalArgumentException("Each answer must be between 1 and 5");
        List<Topic> topics = new ArrayList<>();
        topics.add(new Topic("workload", "carga de tareas", answers[1]));
        topics.add(new Topic("communication", "comunicación", answers[3]));
        topics.add(new Topic("support", "apoyo del equipo", answers[4]));
        topics.add(new Topic("environment", "ambiente y recursos", Math.max(answers[0], answers[6])));
        topics.add(new Topic("rest", "trabajo y descanso", answers[5]));
        topics.add(new Topic("motivation", "aprendizaje y reconocimiento", Math.max(answers[7], answers[8])));
        topics.add(new Topic("organization", "claridad de responsabilidades", answers[2]));
        topics.add(new Topic("overall", "experiencia general", answers[9]));
        return new Analysis(topics, answers[9]);
    }
}
