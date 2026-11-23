package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Reviewed links and original, short app wording. No third-party text is reproduced. */
public final class WellnessKnowledgeCatalog {
    public static final class Source {
        public final String id;
        public final String institution;
        public final String title;
        public final String published;
        public final String url;
        public final String topic;
        public final String supports;
        public final String reviewed;
        public final String useNote;

        Source(String id, String institution, String title, String published, String url,
               String topic, String supports, String reviewed, String useNote) {
            this.id = id;
            this.institution = institution;
            this.title = title;
            this.published = published;
            this.url = url;
            this.topic = topic;
            this.supports = supports;
            this.reviewed = reviewed;
            this.useNote = useNote;
        }
    }

    public static final class Entry {
        public final String topicId;
        public final String title;
        public final String explanation;
        public final String action;
        public final Source source;

        Entry(String topicId, String title, String explanation, String action, Source source) {
            this.topicId = topicId;
            this.title = title;
            this.explanation = explanation;
            this.action = action;
            this.source = source;
        }
    }

    private static final String REVIEWED = "2026-11-11";
    private static final Source WHO = new Source("who-work", "OMS", "Mental health at work",
            "2026-09-15", "https://www.who.int/news-room/fact-sheets/detail/mental-health-at-work",
            "Salud mental laboral", "Carga, apoyo, comunicación, funciones claras y condiciones de trabajo", REVIEWED,
            "Solo enlace y redacción propia. No reproducir texto, imágenes ni logotipo; revisar permisos antes de reutilizar material.");
    private static final Source PAHO = new Source("paho-stress", "OPS", "En tiempos de estrés, haz lo que importa: una guía ilustrada",
            "2022", "https://www.paho.org/es/documentos/tiempos-estres-haz-lo-que-importa-guia-ilustrada",
            "Manejo del estrés", "Habilidades prácticas de autocuidado en momentos de estrés", REVIEWED,
            "Solo enlace y redacción propia. La publicación CC BY-NC-SA 3.0 IGO limita usos comerciales y exige atribución.");
    private static final Source NIOSH = new Source("niosh-twh", "NIOSH/CDC", "Total Worker Health® Frequently Asked Questions",
            "2024-05-31", "https://www.cdc.gov/niosh/twh/faq/index.html",
            "Bienestar del trabajador", "Participación de trabajadores, entorno saludable y control de las condiciones laborales", REVIEWED,
            "Solo enlace y redacción propia. CDC pide atribución y no suponer respaldo institucional; no usar logotipo.");
    private static final Source MINSA = new Source("minsa-stress", "MINSA Perú",
            "Reconocer a tiempo las señales de estrés laboral ayuda a cuidar la salud mental",
            "2026-04-21", "https://www.gob.pe/institucion/minsa/noticias/1381793-reconocer-a-tiempo-las-senales-de-estres-laboral-ayuda-a-cuidar-la-salud-mental",
            "Prevención del estrés laboral", "Pausas, descanso, comunicación y búsqueda de apoyo", REVIEWED,
            "Solo enlace y redacción propia; no reproducir material gráfico ni atribuir respaldo a MINSA.");

    private static final List<Source> SOURCES = Collections.unmodifiableList(Arrays.asList(WHO, PAHO, NIOSH, MINSA));

    private WellnessKnowledgeCatalog() {}

    public static List<Source> sources() { return SOURCES; }

    /** A topic with no vetted entry is omitted, never filled with invented advice. */
    public static Entry find(String topicId, boolean difficulty) {
        switch (topicId) {
            case "workload": return new Entry(topicId,
                    difficulty ? "Revisa la carga de tareas" : "Conserva lo que facilita tu carga",
                    "La cantidad de trabajo y el margen para organizarlo influyen en el bienestar laboral.",
                    difficulty ? "Plantea a tu equipo qué plazo o distribución de tareas necesita revisión."
                            : "Identifica qué forma de distribuir tareas te funcionó este mes.", WHO);
            case "communication": return new Entry(topicId,
                    difficulty ? "Aclara una necesidad concreta" : "Mantén la comunicación útil",
                    "La comunicación abierta ayuda a revisar condiciones y necesidades de trabajo.",
                    difficulty ? "Elige un momento para plantear una duda y acordar el siguiente paso."
                            : "Reconoce qué conversación o acuerdo te resultó útil.", WHO);
            case "support": return new Entry(topicId,
                    difficulty ? "Busca apoyo en el equipo" : "Cuida tus vínculos de apoyo",
                    "El apoyo de colegas y responsables forma parte de un entorno laboral saludable.",
                    difficulty ? "Pide ayuda para una tarea o situación específica a alguien de confianza."
                            : "Mantén el contacto con las personas que te apoyaron.", WHO);
            case "environment": return new Entry(topicId,
                    difficulty ? "Haz visible un recurso que falta" : "Conserva condiciones favorables",
                    "El entorno y la participación de quienes trabajan importan para su bienestar.",
                    difficulty ? "Describe un recurso o condición que facilitaría tu trabajo por el canal apropiado."
                            : "Identifica qué recurso o condición te ayudó este mes.", NIOSH);
            case "rest": return new Entry(topicId,
                    difficulty ? "Protege momentos de descanso" : "Mantén tus pausas útiles",
                    "Las pausas y el respeto al descanso son medidas de autocuidado laboral.",
                    difficulty ? "Busca una pausa breve posible y conversa si tu horario impide descansar."
                            : "Conserva una pausa o rutina de descanso que te hizo bien.", MINSA);
            case "motivation": return new Entry(topicId,
                    difficulty ? "Conversa sobre oportunidades de aprendizaje" : "Reconoce lo que te motiva",
                    "Las oportunidades de desarrollo forman parte de una experiencia laboral saludable.",
                    difficulty ? "Pregunta por una oportunidad concreta de aprender o recibir orientación."
                            : "Anota una oportunidad de aprendizaje que valoraste.", WHO);
            case "organization": return new Entry(topicId,
                    difficulty ? "Aclara responsabilidades" : "Conserva la claridad de tareas",
                    "Las funciones poco claras pueden dificultar la experiencia de trabajo.",
                    difficulty ? "Confirma con tu equipo la prioridad y el responsable de una tarea."
                            : "Conserva el acuerdo que te dio claridad este mes.", WHO);
            case "overall": return new Entry(topicId,
                    difficulty ? "Dedica un momento a cuidarte" : "Reconoce lo que te hizo bien",
                    "Las habilidades prácticas de autocuidado pueden ayudar a afrontar momentos de estrés.",
                    difficulty ? "Prueba unos minutos de una práctica de la guía y considera pedir apoyo si el malestar continúa."
                            : "Identifica una práctica cotidiana que quieras conservar.", PAHO);
            default: return null;
        }
    }
}
