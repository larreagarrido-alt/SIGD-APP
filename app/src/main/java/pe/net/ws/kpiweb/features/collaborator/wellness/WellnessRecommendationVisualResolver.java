package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.text.Normalizer;
import java.util.Locale;

import pe.net.ws.kpiweb.R;

/**
 * Resuelve únicamente la presentación visual (ícono, color de acento suave y etiqueta de categoría)
 * de cada recomendación real de Bienestar Psicológico sin modificar su contenido ni cálculo.
 */
public final class WellnessRecommendationVisualResolver {

    public enum VisualKind {
        REST_SELF_CARE,
        RECOGNITION,
        LEARNING_GROWTH,
        TASKS_ORGANIZATION,
        COMMUNICATION,
        TEAMWORK,
        STRESS_EMOTIONAL,
        ENVIRONMENT_GENERAL
    }

    public static final class VisualStyle {
        public final VisualKind kind;
        public final int iconRes;
        public final String categoryLabel;
        public final int accentColor;

        public VisualStyle(VisualKind kind, int iconRes, String categoryLabel, int accentColor) {
            this.kind = kind;
            this.iconRes = iconRes;
            this.categoryLabel = categoryLabel;
            this.accentColor = accentColor;
        }
    }

    private static final int COLOR_LAVENDER = 0xFFDAB4FF;
    private static final int COLOR_SOFT_GOLD = 0xFFF4C184;
    private static final int COLOR_MINT_GREEN = 0xFF92E4C4;
    private static final int COLOR_SOFT_BLUE = 0xFF9BCBFF;
    private static final int COLOR_TURQUOISE = 0xFF7EDDD4;
    private static final int COLOR_PASTEL_ROSE = 0xFFEDA9CC;

    private WellnessRecommendationVisualResolver() {}

    public static VisualStyle resolve(WellnessRecommendationRules.Recommendation recommendation) {
        if (recommendation == null) {
            return styleFor(VisualKind.ENVIRONMENT_GENERAL, "Bienestar laboral");
        }
        return resolve(recommendation.id, recommendation.topic, recommendation.title, recommendation.detail);
    }

    public static VisualStyle resolve(String topicId, String topicName, String title, String detail) {
        String id = normalize(topicId);
        String text = normalize((topicName == null ? "" : topicName) + " "
                + (title == null ? "" : title) + " "
                + (detail == null ? "" : detail));

        // 1. Coincidencia prioritaria por identificador real del motor de recomendaciones
        if ("rest".equals(id)) {
            return styleFor(VisualKind.REST_SELF_CARE, "Autocuidado");
        }
        if ("communication".equals(id)) {
            return styleFor(VisualKind.COMMUNICATION, "Comunicación");
        }
        if ("support".equals(id)) {
            return styleFor(VisualKind.TEAMWORK, "Trabajo en equipo");
        }
        if ("organization".equals(id) || "workload".equals(id)) {
            return styleFor(VisualKind.TASKS_ORGANIZATION, "Organización");
        }
        if ("overall".equals(id)) {
            return styleFor(VisualKind.STRESS_EMOTIONAL, "Bienestar emocional");
        }
        if ("motivation".equals(id)) {
            if (containsAny(text, "reconocimiento", "esfuerzo", "logro")
                    && !containsAny(text, "aprendizaje", "aprender", "desarrollo")) {
                return styleFor(VisualKind.RECOGNITION, "Reconocimiento");
            }
            return styleFor(VisualKind.LEARNING_GROWTH, "Desarrollo");
        }
        if ("environment".equals(id)) {
            return styleFor(VisualKind.ENVIRONMENT_GENERAL, "Entorno laboral");
        }

        // 2. Respaldo por palabras clave del contenido real cuando no hay ID de tema
        if (containsAny(text, "descanso", "pausa", "autocuidado", "dormir", "sueno")) {
            return styleFor(VisualKind.REST_SELF_CARE, "Autocuidado");
        }
        if (containsAny(text, "reconocimiento", "esfuerzo", "logro", "valorar")) {
            return styleFor(VisualKind.RECOGNITION, "Reconocimiento");
        }
        if (containsAny(text, "aprendizaje", "aprender", "desarrollo", "crecimiento", "motiva")) {
            return styleFor(VisualKind.LEARNING_GROWTH, "Desarrollo");
        }
        if (containsAny(text, "tarea", "carga", "claridad", "responsabilidad", "organiza", "prioridad")) {
            return styleFor(VisualKind.TASKS_ORGANIZATION, "Organización");
        }
        if (containsAny(text, "comunicacion", "conversacion", "aclarar", "dialogo", "duda")) {
            return styleFor(VisualKind.COMMUNICATION, "Comunicación");
        }
        if (containsAny(text, "equipo", "apoyo", "colega", "companero", "vinculo")) {
            return styleFor(VisualKind.TEAMWORK, "Trabajo en equipo");
        }
        if (containsAny(text, "estres", "emocional", "cuidarte", "malestar", "relajacion")) {
            return styleFor(VisualKind.STRESS_EMOTIONAL, "Bienestar emocional");
        }

        return styleFor(VisualKind.ENVIRONMENT_GENERAL, "Bienestar laboral");
    }

    private static VisualStyle styleFor(VisualKind kind, String categoryLabel) {
        switch (kind) {
            case REST_SELF_CARE:
                return new VisualStyle(kind, R.drawable.collab_ic_rec_moon_star, categoryLabel, COLOR_LAVENDER);
            case RECOGNITION:
                return new VisualStyle(kind, R.drawable.collab_ic_notif_award, categoryLabel, COLOR_SOFT_GOLD);
            case LEARNING_GROWTH:
                return new VisualStyle(kind, R.drawable.collab_ic_rec_graduation_cap, categoryLabel, COLOR_MINT_GREEN);
            case TASKS_ORGANIZATION:
                return new VisualStyle(kind, R.drawable.collab_ic_rec_clipboard_list, categoryLabel, COLOR_SOFT_BLUE);
            case COMMUNICATION:
                return new VisualStyle(kind, R.drawable.collab_ic_rec_messages_square, categoryLabel, COLOR_LAVENDER);
            case TEAMWORK:
                return new VisualStyle(kind, R.drawable.collab_ic_rec_users_round, categoryLabel, COLOR_TURQUOISE);
            case STRESS_EMOTIONAL:
                return new VisualStyle(kind, R.drawable.collab_ic_rec_brain, categoryLabel, COLOR_PASTEL_ROSE);
            case ENVIRONMENT_GENERAL:
            default:
                return new VisualStyle(VisualKind.ENVIRONMENT_GENERAL, R.drawable.collab_ic_rec_sparkles, categoryLabel, COLOR_LAVENDER);
        }
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
