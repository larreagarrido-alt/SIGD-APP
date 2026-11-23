package pe.net.ws.kpiweb.features.admin.dashboard;

import org.junit.Test;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ExecutionStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.FormInput;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.MonthlyCategory;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ValidationResult;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminWellnessActivityHelperTest {

    @Test
    public void exposesMonthlyCategoriesDistinctFromRyffDimensions() {
        List<MonthlyCategory> categories = AdminWellnessActivityHelper.monthlyCategories();
        assertEquals(8, categories.size());
        assertEquals("Bienestar general", AdminWellnessActivityHelper.formatCategoryLabel("GENERAL"));
        assertEquals("Carga de tareas", AdminWellnessActivityHelper.formatCategoryLabel("CARGA_DE_TAREAS"));
        assertEquals("Trabajo y descanso", AdminWellnessActivityHelper.formatCategoryLabel("trabajo y descanso"));
    }

    @Test
    public void separatesExecutionStatusFromPublicationStatus() {
        FormInput draft = new FormInput(
                "Taller de manejo del estrés",
                "Brindar técnicas prácticas de respiración y pausas",
                "TRABAJO_Y_DESCANSO",
                "Ana López",
                "2026-11-28",
                "10:00",
                "11:30",
                ExecutionStatus.PENDIENTE,
                VisibilityScope.SOLO_ADMIN,
                0, 0, 0
        );
        assertTrue(AdminWellnessActivityHelper.validate(draft, false).valid);

        ValidationResult publishAdminOnly = AdminWellnessActivityHelper.validate(draft, true);
        assertFalse(publishAdminOnly.valid);

        FormInput publishable = new FormInput(
                "Taller de manejo del estrés",
                "Brindar técnicas prácticas de respiración y pausas",
                "TRABAJO_Y_DESCANSO",
                "Ana López",
                "2026-11-28",
                "10:00",
                "11:30",
                ExecutionStatus.PENDIENTE,
                VisibilityScope.TODO_EQUIPO,
                0, 0, 0
        );
        assertTrue(AdminWellnessActivityHelper.validate(publishable, true).valid);
    }

    @Test
    public void validatesTimeRangesAndSpecificRecipientsBeforePublishing() {
        FormInput badTime = new FormInput(
                "Pausa activa", "Reducir fatiga", "GENERAL", "Bruno Díaz",
                "2026-11-29", "15:00", "14:00",
                ExecutionStatus.PENDIENTE, VisibilityScope.TODO_EQUIPO, 0, 0, 0
        );
        assertFalse(AdminWellnessActivityHelper.validate(badTime, true).valid);

        FormInput missingCollaborators = new FormInput(
                "Pausa activa", "Reducir fatiga", "GENERAL", "Bruno Díaz",
                "2026-11-29", "15:00", "15:30",
                ExecutionStatus.PENDIENTE, VisibilityScope.COLABORADORES, 0, 0, 0
        );
        assertFalse(AdminWellnessActivityHelper.validate(missingCollaborators, true).valid);

        FormInput validCollaborators = new FormInput(
                "Pausa activa", "Reducir fatiga", "GENERAL", "Bruno Díaz",
                "2026-11-29", "15:00", "15:30",
                ExecutionStatus.EN_PROCESO, VisibilityScope.COLABORADORES, 0, 0, 3
        );
        assertTrue(AdminWellnessActivityHelper.validate(validCollaborators, true).valid);
        assertEquals("3 colaboradores", AdminWellnessActivityHelper.formatAudienceSummary(
                VisibilityScope.COLABORADORES, 0, 0, 3));
    }

    @Test
    public void collaboratorOnlySeesPublishedNonAdminNonCanceledActivities() {
        assertTrue(AdminWellnessActivityHelper.isVisibleToCollaborator(
                PublicationStatus.PUBLICADA, VisibilityScope.TODO_EQUIPO, "PENDIENTE"));
        assertTrue(AdminWellnessActivityHelper.isVisibleToCollaborator(
                PublicationStatus.PUBLICADA, VisibilityScope.COLABORADORES, "EN_PROCESO"));
        assertFalse(AdminWellnessActivityHelper.isVisibleToCollaborator(
                PublicationStatus.BORRADOR, VisibilityScope.TODO_EQUIPO, "PENDIENTE"));
        assertFalse(AdminWellnessActivityHelper.isVisibleToCollaborator(
                PublicationStatus.PUBLICADA, VisibilityScope.SOLO_ADMIN, "PENDIENTE"));
        assertFalse(AdminWellnessActivityHelper.isVisibleToCollaborator(
                PublicationStatus.PUBLICADA, VisibilityScope.TODO_EQUIPO, "CANCELADA"));
        assertEquals("28/11/2026 · 10:00 - 11:30",
                AdminWellnessActivityHelper.formatScheduleDisplay("2026-11-28", "10:00", "11:30"));
    }
}
