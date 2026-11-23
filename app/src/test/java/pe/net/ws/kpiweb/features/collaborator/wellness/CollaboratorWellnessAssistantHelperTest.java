package pe.net.ws.kpiweb.features.collaborator.wellness;

import org.junit.Test;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ExecutionStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.SuggestedQuestion;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessAssistantHelper.ActivityDisplayState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollaboratorWellnessAssistantHelperTest {

    @Test
    public void providesFiveInteractiveCollaboratorQuestions() {
        List<SuggestedQuestion> questions = CollaboratorWellnessAssistantHelper.collaboratorSuggestedQuestions();
        assertEquals(5, questions.size());
        assertEquals("¿Cómo organizar mejor mi tiempo?", questions.get(0).question);
        assertEquals("¿Qué actividades de bienestar tengo disponibles?", questions.get(4).question);
    }

    @Test
    public void detectsActivitiesQueryAndReturnsLocalNonClinicalGuidance() {
        assertTrue(CollaboratorWellnessAssistantHelper.isActivitiesAvailabilityQuery(
                "¿Qué actividades de bienestar tengo disponibles?"));
        assertFalse(CollaboratorWellnessAssistantHelper.isActivitiesAvailabilityQuery(
                "¿Cómo cuidar mis descansos?"));

        String timeGuide = CollaboratorWellnessAssistantHelper.localGuidanceForQuestion("¿Cómo organizar mejor mi tiempo?");
        assertTrue(timeGuide.contains("Organización práctica"));

        String restGuide = CollaboratorWellnessAssistantHelper.localGuidanceForQuestion("¿Cómo cuidar mis descansos?");
        assertTrue(restGuide.contains("Cuidado de tus descansos"));
    }

    @Test
    public void classifiesActivityStatesAndHidesDraftsOrAdminOnly() {
        assertEquals(ActivityDisplayState.PROXIMA,
                CollaboratorWellnessAssistantHelper.classifyActivityState(ExecutionStatus.PENDIENTE));
        assertEquals(ActivityDisplayState.EN_CURSO,
                CollaboratorWellnessAssistantHelper.classifyActivityState(ExecutionStatus.EN_PROCESO));
        assertEquals(ActivityDisplayState.FINALIZADA,
                CollaboratorWellnessAssistantHelper.classifyActivityState(ExecutionStatus.COMPLETADA));

        assertTrue(CollaboratorWellnessAssistantHelper.isActivityVisibleForCollaborator(
                PublicationStatus.PUBLICADA, VisibilityScope.TODO_EQUIPO, "PENDIENTE"));
        assertFalse(CollaboratorWellnessAssistantHelper.isActivityVisibleForCollaborator(
                PublicationStatus.BORRADOR, VisibilityScope.TODO_EQUIPO, "PENDIENTE"));
        assertFalse(CollaboratorWellnessAssistantHelper.isActivityVisibleForCollaborator(
                PublicationStatus.PUBLICADA, VisibilityScope.SOLO_ADMIN, "PENDIENTE"));
    }

    @Test
    public void excludesFeaturedRecordFromEvolutionTimelineWithoutDroppingOlderPeriods() {
        WellnessEvolutionHelper.EvaluationRecord nov = new WellnessEvolutionHelper.EvaluationRecord(
                101, 1, "Evaluación mensual", "2026-11", "Noviembre 2026",
                "COMPLETADA", "Completada", WellnessEvolutionHelper.StatusTone.COMPLETED, "20/11/2026", null);
        WellnessEvolutionHelper.EvaluationRecord oct = new WellnessEvolutionHelper.EvaluationRecord(
                98, 1, "Evaluación mensual", "2026-10", "Octubre 2026",
                "COMPLETADA", "Completada", WellnessEvolutionHelper.StatusTone.COMPLETED, "22/10/2026", null);

        List<WellnessEvolutionHelper.EvaluationRecord> remaining =
                WellnessEvolutionHelper.excludeFeatured(java.util.Arrays.asList(nov, oct), nov);
        assertEquals(1, remaining.size());
        assertEquals(98, remaining.get(0).evaluationId);

        List<WellnessEvolutionHelper.EvaluationRecord> singleOnly =
                WellnessEvolutionHelper.excludeFeatured(java.util.Collections.singletonList(nov), nov);
        assertTrue(singleOnly.isEmpty());
    }
}
