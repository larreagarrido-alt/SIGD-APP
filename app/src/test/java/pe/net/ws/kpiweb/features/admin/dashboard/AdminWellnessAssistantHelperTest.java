package pe.net.ws.kpiweb.features.admin.dashboard;

import org.junit.Test;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.AssistantRole;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.BlockType;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.ResponseBlock;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessAssistantHelper.SuggestedQuestion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminWellnessAssistantHelperTest {

    @Test
    public void providesFiveInteractiveAdminSuggestedQuestions() {
        List<SuggestedQuestion> items = AdminWellnessAssistantHelper.adminSuggestedQuestions();
        assertEquals(5, items.size());
        assertEquals("¿Cómo mejorar la comunicación del equipo?", items.get(0).question);
        assertEquals("¿Cómo puedo interpretar las tendencias generales de bienestar?", items.get(4).question);
    }

    @Test
    public void structuresHeadingsBulletsAndParagraphsWithoutAlteringMeaning() {
        String serverText = "Acciones sugeridas:\n- Programar pausas activas breves\n2. Revisar distribución de tareas\nMantén canales abiertos de escucha.";
        List<ResponseBlock> blocks = AdminWellnessAssistantHelper.formatStructuredResponse(serverText);
        assertEquals(4, blocks.size());
        assertEquals(BlockType.HEADING, blocks.get(0).type);
        assertEquals("Acciones sugeridas", blocks.get(0).text);
        assertEquals(BlockType.BULLET, blocks.get(1).type);
        assertEquals("Programar pausas activas breves", blocks.get(1).text);
        assertEquals(BlockType.BULLET, blocks.get(2).type);
        assertEquals("Revisar distribución de tareas", blocks.get(2).text);
        assertEquals(BlockType.PARAGRAPH, blocks.get(3).type);
        assertEquals("Mantén canales abiertos de escucha.", blocks.get(3).text);
    }

    @Test
    public void detectsUrgentEmotionalSupportCuesAndDifferentiatesRoles() {
        assertTrue(AdminWellnessAssistantHelper.containsUrgentSupportCue("Un colaborador atraviesa una crisis de pánico"));
        assertTrue(AdminWellnessAssistantHelper.containsUrgentSupportCue("Necesito ayuda urgente por una emergencia emocional"));
        assertFalse(AdminWellnessAssistantHelper.containsUrgentSupportCue("¿Cómo organizar una pausa activa?"));
        assertFalse(AssistantRole.ADMIN.subtitle.equals(AssistantRole.COLLABORATOR.subtitle));
    }
}
