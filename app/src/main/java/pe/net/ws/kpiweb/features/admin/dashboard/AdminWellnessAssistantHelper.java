package pe.net.ws.kpiweb.features.admin.dashboard;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Helper puro para el Asistente de orientación organizacional (Asistente SIGD).
 * Define preguntas frecuentes administrativas, estructura visual de respuestas reales del servidor,
 * diferenciación de rol y detección responsable de situaciones que requieren atención profesional.
 */
public final class AdminWellnessAssistantHelper {

    public enum AssistantRole {
        ADMIN(
                "Asistente SIGD",
                "Orientación para la gestión del bienestar",
                "Hola. Puedo brindarte orientaciones generales sobre bienestar organizacional, comunicación, carga de trabajo, pausas y actividades de mejora. No accedo a respuestas psicológicas individuales ni realizo diagnósticos."
        ),
        COLLABORATOR(
                "Asistente SIGD",
                "Orientación de bienestar laboral",
                "Hola. Puedo orientarte de forma general sobre tu bienestar laboral y las actividades disponibles."
        );

        public final String title;
        public final String subtitle;
        public final String welcomeMessage;

        AssistantRole(String title, String subtitle, String welcomeMessage) {
            this.title = title;
            this.subtitle = subtitle;
            this.welcomeMessage = welcomeMessage;
        }
    }

    public enum BlockType {
        HEADING,
        BULLET,
        PARAGRAPH
    }

    public static final class ResponseBlock {
        public final BlockType type;
        public final String text;

        public ResponseBlock(BlockType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    public static final class SuggestedQuestion {
        public final String id;
        public final String question;

        public SuggestedQuestion(String id, String question) {
            this.id = id;
            this.question = question;
        }
    }

    public static final String DISCLAIMER_TEXT =
            "Este asistente proporciona orientación general y no sustituye la atención de un profesional.";

    public static final String URGENT_SUPPORT_NOTICE =
            "Si alguien del equipo atraviesa una situación urgente o de riesgo emocional, prioriza la atención inmediata de un profesional de salud mental acreditado o los servicios de emergencia locales.";

    private AdminWellnessAssistantHelper() {}

    public static List<SuggestedQuestion> adminSuggestedQuestions() {
        List<SuggestedQuestion> list = new ArrayList<>(5);
        list.add(new SuggestedQuestion("communication", "¿Cómo mejorar la comunicación del equipo?"));
        list.add(new SuggestedQuestion("workload", "¿Qué actividades ayudan a reducir la sobrecarga laboral?"));
        list.add(new SuggestedQuestion("activity", "¿Cómo organizar una actividad de bienestar?"));
        list.add(new SuggestedQuestion("rest", "¿Qué acciones puedo implementar para mejorar el descanso?"));
        list.add(new SuggestedQuestion("trends", "¿Cómo puedo interpretar las tendencias generales de bienestar?"));
        return Collections.unmodifiableList(list);
    }

    /**
     * Organiza una respuesta real del servidor en bloques legibles (títulos breves, viñetas y párrafos)
     * sin alterar su contenido ni convertirla en un diagnóstico.
     */
    public static List<ResponseBlock> formatStructuredResponse(@Nullable String rawResponse) {
        List<ResponseBlock> blocks = new ArrayList<>();
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return blocks;
        }
        String normalized = rawResponse.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("- ") || line.startsWith("• ") || line.startsWith("* ")) {
                String item = line.substring(2).trim();
                if (!item.isEmpty()) blocks.add(new ResponseBlock(BlockType.BULLET, item));
            } else if (line.matches("^\\d+[.)]\\s+.+")) {
                String item = line.replaceFirst("^\\d+[.)]\\s+", "").trim();
                if (!item.isEmpty()) blocks.add(new ResponseBlock(BlockType.BULLET, item));
            } else if (line.endsWith(":") && line.length() <= 68) {
                blocks.add(new ResponseBlock(BlockType.HEADING, line.substring(0, line.length() - 1).trim()));
            } else {
                blocks.add(new ResponseBlock(BlockType.PARAGRAPH, line));
            }
        }
        return blocks;
    }

    /**
     * Detecta menciones a situaciones urgentes para acompañar con orientación responsable
     * sin inventar teléfonos ni contactos institucionales.
     */
    public static boolean containsUrgentSupportCue(@Nullable String question) {
        if (question == null || question.trim().isEmpty()) return false;
        String norm = Normalizer.normalize(question.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String[] cues = {
                "suicid", "autolesion", "crisis", "emergencia",
                "urgencia emocional", "panico", "ayuda urgente", "danarme", "quitarse la vida"
        };
        for (String cue : cues) {
            if (norm.contains(cue)) return true;
        }
        return false;
    }
}
