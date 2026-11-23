package pe.net.ws.kpiweb.features.collaborator.wellness;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ExecutionStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.SuggestedQuestion;

/**
 * Reglas locales y utilidades del Bienestar del Colaborador:
 * - Clasificación de estados reales de Actividades y eventos.
 * - Preguntas frecuentes interactivas y orientación general local (sin IA simulada,
 *   sin diagnóstico clínico y sin leer respuestas psicológicas privadas).
 */
public final class CollaboratorWellnessAssistantHelper {

    public enum ActivityDisplayState {
        PROXIMA("Próxima"),
        EN_CURSO("En curso"),
        FINALIZADA("Finalizada");

        public final String label;

        ActivityDisplayState(String label) {
            this.label = label;
        }
    }

    private CollaboratorWellnessAssistantHelper() {}

    public static List<SuggestedQuestion> collaboratorSuggestedQuestions() {
        List<SuggestedQuestion> list = new ArrayList<>(5);
        list.add(new SuggestedQuestion("time", "¿Cómo organizar mejor mi tiempo?"));
        list.add(new SuggestedQuestion("workload", "¿Qué hacer si tengo mucha carga laboral?"));
        list.add(new SuggestedQuestion("communication", "¿Cómo mejorar la comunicación?"));
        list.add(new SuggestedQuestion("rest", "¿Cómo cuidar mis descansos?"));
        list.add(new SuggestedQuestion("activities", "¿Qué actividades de bienestar tengo disponibles?"));
        return Collections.unmodifiableList(list);
    }

    public static boolean isActivitiesAvailabilityQuery(@Nullable String question) {
        String norm = normalize(question);
        return norm.contains("actividad") || norm.contains("evento")
                || norm.contains("taller") || norm.contains("charla");
    }

    /**
     * Genera orientación general basada en reglas locales (sin IA externa ni acceso a respuestas privadas).
     */
    public static String localGuidanceForQuestion(@Nullable String question) {
        String norm = normalize(question);
        if (norm.contains("tiempo") || norm.contains("organizar") || norm.contains("priorid")) {
            return "Organización práctica de tu jornada:\n"
                    + "- Define al inicio del día tus 2 o 3 tareas prioritarias antes de revisar mensajes.\n"
                    + "- Agrupa actividades similares en bloques de enfoque de 40 a 50 minutos.\n"
                    + "- Reserva un cierre breve al final del turno para dejar claras las prioridades del día siguiente.\n"
                    + "Orientación general con reglas locales; no sustituye asesoría profesional.";
        }
        if (norm.contains("carga") || norm.contains("sobrecarga") || norm.contains("estres") || norm.contains("presion")) {
            return "Manejo de carga laboral:\n"
                    + "- Haz una lista visible de tus pendientes y distingue qué es urgente de lo que puede reprogramarse.\n"
                    + "- Conversa a tiempo con tu coordinación cuando necesites aclarar plazos o redistribuir tareas.\n"
                    + "- Realiza micro-pausas breves de respiración entre bloques intensos de trabajo.\n"
                    + "Orientación general con reglas locales; si el malestar persiste, busca apoyo profesional.";
        }
        if (norm.contains("comunicacion") || norm.contains("equipo") || norm.contains("companero")) {
            return "Comunicación clara y saludable:\n"
                    + "- Acuerda expectativas, responsables y fechas por un canal claro desde el inicio.\n"
                    + "- Expresa necesidades o bloqueos describiendo hechos concretos y propuestas de solución.\n"
                    + "- Confirma acuerdos brevemente por escrito para evitar malentendidos.\n"
                    + "Orientación general con reglas locales.";
        }
        if (norm.contains("descanso") || norm.contains("pausa") || norm.contains("cansancio") || norm.contains("sueno")) {
            return "Cuidado de tus descansos:\n"
                    + "- Haz pausas breves cada 60 a 90 minutos para estirar, hidratarte y descansar la vista.\n"
                    + "- Protege tus horarios fuera de jornada desconectando notificaciones laborales cuando corresponda.\n"
                    + "- Mantén rutinas regulares de descanso al cerrar tu día laboral.\n"
                    + "Orientación general con reglas locales.";
        }
        return "Orientación general de bienestar laboral:\n"
                + "- Prioriza tus tareas clave, toma pausas breves durante la jornada y comunica a tiempo cuando necesites apoyo.\n"
                + "- Puedes consultar tus actividades publicadas en la sección Actividades y eventos o revisar tus evaluaciones cuando lo desees.\n"
                + "Este asistente usa reglas locales de orientación general y no accede a tus respuestas psicológicas privadas.";
    }

    public static ActivityDisplayState classifyActivityState(@Nullable ExecutionStatus executionStatus) {
        if (executionStatus == ExecutionStatus.COMPLETADA) {
            return ActivityDisplayState.FINALIZADA;
        }
        if (executionStatus == ExecutionStatus.EN_PROCESO) {
            return ActivityDisplayState.EN_CURSO;
        }
        return ActivityDisplayState.PROXIMA;
    }

    public static boolean isActivityVisibleForCollaborator(@Nullable PublicationStatus pubStatus,
                                                           @Nullable VisibilityScope visibility,
                                                           @Nullable String rawExecStatus) {
        return AdminWellnessActivityHelper.isVisibleToCollaborator(pubStatus, visibility, rawExecStatus);
    }

    public static boolean containsUrgentCue(@Nullable String question) {
        return AdminWellnessAssistantHelper.containsUrgentSupportCue(question);
    }

    private static String normalize(@Nullable String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
