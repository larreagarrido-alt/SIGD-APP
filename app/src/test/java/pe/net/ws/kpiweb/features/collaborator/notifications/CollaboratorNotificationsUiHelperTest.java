package pe.net.ws.kpiweb.features.collaborator.notifications;

import org.junit.Test;

import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.NotificationType;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.EmptyCopy;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.IconKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollaboratorNotificationsUiHelperTest {

    @Test
    public void formatHeaderPendingStatus_handlesZeroSingularAndPlural() {
        assertEquals("Sin pendientes", CollaboratorNotificationsUiHelper.formatHeaderPendingStatus(0));
        assertEquals("1 pendiente", CollaboratorNotificationsUiHelper.formatHeaderPendingStatus(1));
        assertEquals("4 pendientes", CollaboratorNotificationsUiHelper.formatHeaderPendingStatus(4));
    }

    @Test
    public void formatSectionCount_handlesZeroSingularAndPlural() {
        assertEquals("0 notificaciones", CollaboratorNotificationsUiHelper.formatSectionCount(0));
        assertEquals("1 notificación", CollaboratorNotificationsUiHelper.formatSectionCount(1));
        assertEquals("12 notificaciones", CollaboratorNotificationsUiHelper.formatSectionCount(12));
    }

    @Test
    public void resolveEmptyCopy_distinguishesAllUnreadAndImportantFilters() {
        EmptyCopy all = CollaboratorNotificationsUiHelper.resolveEmptyCopy("TODAS", 0);
        assertEquals("¡Todo al día!", all.title);
        assertEquals("No hay avisos pendientes", all.statusChip);
        assertTrue(all.positiveTone);

        EmptyCopy unread = CollaboratorNotificationsUiHelper.resolveEmptyCopy("NO_LEIDAS", 3);
        assertEquals("No tienes notificaciones sin leer", unread.title);
        assertTrue(unread.positiveTone);

        EmptyCopy important = CollaboratorNotificationsUiHelper.resolveEmptyCopy("IMPORTANTES", 2);
        assertEquals("No tienes avisos importantes por ahora", important.title);
        assertFalse(important.positiveTone);
    }

    @Test
    public void resolveIconKind_mapsNotificationTypesToDistinctIcons() {
        assertEquals(IconKind.CLIPBOARD_CHECK, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.NUEVO_KPI));
        assertEquals(IconKind.CLIPBOARD_CHECK, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.RECORDATORIO_EVALUACION));
        assertEquals(IconKind.CALENDAR_CLOCK, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.KPI_PROXIMO));
        assertEquals(IconKind.ALARM_CLOCK, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.KPI_VENCIDO));
        assertEquals(IconKind.GIFT, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.PREMIO_DESBLOQUEADO));
        assertEquals(IconKind.AWARD, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.ACTIVIDAD_COMPLETADA));
        assertEquals(IconKind.HEART_HANDSHAKE, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.RECORDATORIO_BIENESTAR));
        assertEquals(IconKind.MEGAPHONE, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.CAMBIO_ESTADO));
        assertEquals(IconKind.BELL, CollaboratorNotificationsUiHelper.resolveIconKind(NotificationType.AVISO_GENERAL));
    }
}
