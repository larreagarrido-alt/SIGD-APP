package pe.net.ws.kpiweb.features.collaborator.wellness;

import org.junit.Test;

import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessRecommendationVisualResolver.VisualKind;
import pe.net.ws.kpiweb.features.collaborator.wellness.WellnessRecommendationVisualResolver.VisualStyle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class WellnessRecommendationVisualResolverTest {

    @Test
    public void resolve_mapsRealEngineTopicIdsToDistinctVisualKinds() {
        VisualStyle rest = WellnessRecommendationVisualResolver.resolve("rest", "trabajo y descanso",
                "Protege momentos de descanso", "Las pausas ayudan.");
        VisualStyle learning = WellnessRecommendationVisualResolver.resolve("motivation", "aprendizaje y reconocimiento",
                "Conversa sobre oportunidades de aprendizaje", "Oportunidades de desarrollo.");
        VisualStyle tasks = WellnessRecommendationVisualResolver.resolve("organization", "responsabilidades",
                "Conserva la claridad de tareas", "Funciones claras.");
        VisualStyle comm = WellnessRecommendationVisualResolver.resolve("communication", "comunicación",
                "Mantén la comunicación útil", "Comunicación abierta.");
        VisualStyle team = WellnessRecommendationVisualResolver.resolve("support", "apoyo del equipo",
                "Busca apoyo en el equipo", "Apoyo de colegas.");
        VisualStyle stress = WellnessRecommendationVisualResolver.resolve("overall", "experiencia general",
                "Dedica un momento a cuidarte", "Habilidades frente al estrés.");
        VisualStyle env = WellnessRecommendationVisualResolver.resolve("environment", "ambiente y recursos",
                "Haz visible un recurso que falta", "El entorno importa.");

        assertEquals(VisualKind.REST_SELF_CARE, rest.kind);
        assertEquals("Autocuidado", rest.categoryLabel);

        assertEquals(VisualKind.LEARNING_GROWTH, learning.kind);
        assertEquals("Desarrollo", learning.categoryLabel);

        assertEquals(VisualKind.TASKS_ORGANIZATION, tasks.kind);
        assertEquals("Organización", tasks.categoryLabel);

        assertEquals(VisualKind.COMMUNICATION, comm.kind);
        assertEquals("Comunicación", comm.categoryLabel);

        assertEquals(VisualKind.TEAMWORK, team.kind);
        assertEquals("Trabajo en equipo", team.categoryLabel);

        assertEquals(VisualKind.STRESS_EMOTIONAL, stress.kind);
        assertEquals("Bienestar emocional", stress.categoryLabel);

        assertEquals(VisualKind.ENVIRONMENT_GENERAL, env.kind);
        assertEquals("Entorno laboral", env.categoryLabel);

        assertNotEquals(rest.iconRes, learning.iconRes);
        assertNotEquals(learning.iconRes, tasks.iconRes);
        assertNotEquals(tasks.iconRes, team.iconRes);
        assertNotEquals(team.iconRes, stress.iconRes);
    }

    @Test
    public void resolve_fallsBackToKeywordsAndGeneralIconGracefully() {
        VisualStyle recognition = WellnessRecommendationVisualResolver.resolve("", "",
                "Valora el reconocimiento de tu esfuerzo", "Reconocer logros motiva.");
        assertEquals(VisualKind.RECOGNITION, recognition.kind);
        assertEquals("Reconocimiento", recognition.categoryLabel);

        VisualStyle fallback = WellnessRecommendationVisualResolver.resolve("", "",
                "Sugerencia general", "Contenido informativo.");
        assertEquals(VisualKind.ENVIRONMENT_GENERAL, fallback.kind);
        assertEquals("Bienestar laboral", fallback.categoryLabel);
    }
}
