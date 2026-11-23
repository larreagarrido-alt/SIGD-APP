package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.AdminNoticeItem;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.AdminTarget;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.FilterTab;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.VisualKind;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.VisualMeta;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.EmptyCopy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminNotificationsUiHelperTest {

    @Test
    public void parsesFiltersAndCountsUnreadWithoutMixingOperationalForms() {
        JsonArray arr = new JsonArray();

        JsonObject n1 = new JsonObject();
        n1.addProperty("id_notificacion", 101);
        n1.addProperty("tipo", "KPI_VENCIDO");
        n1.addProperty("titulo", "KPI vencido en área comercial");
        n1.addProperty("mensaje", "Revisar indicador vencido.");
        n1.addProperty("creado_en", "2026-11-21 09:30:00");
        n1.addProperty("leida", 0);
        arr.add(n1);

        JsonObject n2 = new JsonObject();
        n2.addProperty("id_notificacion", 102);
        n2.addProperty("tipo", "AVISO_GENERAL");
        n2.addProperty("titulo", "Actualización de sistema");
        n2.addProperty("mensaje", "Sin pendientes adicionales.");
        n2.addProperty("creado_en", "2026-11-20 18:10:00");
        n2.addProperty("leida", 1);
        arr.add(n2);

        List<AdminNoticeItem> parsed = AdminNotificationsUiHelper.parseNotifications(arr);
        assertEquals(2, parsed.size());
        assertEquals(1, AdminNotificationsUiHelper.countUnread(parsed));
        assertEquals("1 pendiente", AdminNotificationsUiHelper.formatHeaderPendingStatus(1));
        assertEquals("Sin pendientes", AdminNotificationsUiHelper.formatHeaderPendingStatus(0));
        assertEquals("0 notificaciones", AdminNotificationsUiHelper.formatSectionCount(0));
        assertEquals("2 notificaciones", AdminNotificationsUiHelper.formatSectionCount(parsed.size()));

        assertEquals(2, AdminNotificationsUiHelper.filterNotices(parsed, FilterTab.TODOS).size());
        assertEquals(1, AdminNotificationsUiHelper.filterNotices(parsed, FilterTab.NO_LEIDOS).size());
        assertEquals(1, AdminNotificationsUiHelper.filterNotices(parsed, FilterTab.IMPORTANTES).size());
    }

    @Test
    public void resolvesAdminEmptyStatesConsistentlyWithCollaboratorStyle() {
        EmptyCopy allEmpty = AdminNotificationsUiHelper.resolveEmptyCopy(FilterTab.TODOS, 0);
        assertEquals("¡Todo al día!", allEmpty.title);
        assertEquals("No hay avisos pendientes", allEmpty.statusChip);
        assertTrue(allEmpty.positiveTone);

        EmptyCopy unreadEmpty = AdminNotificationsUiHelper.resolveEmptyCopy(FilterTab.NO_LEIDOS, 3);
        assertEquals("No tienes avisos sin leer", unreadEmpty.title);
        assertTrue(unreadEmpty.positiveTone);

        EmptyCopy importantEmpty = AdminNotificationsUiHelper.resolveEmptyCopy(FilterTab.IMPORTANTES, 0);
        assertEquals("No tienes avisos importantes por ahora", importantEmpty.title);
        assertFalse(importantEmpty.positiveTone);
    }

    @Test
    public void resolvesAdminIconsAndTargetsHonestly() {
        VisualMeta form = AdminNotificationsUiHelper.resolveVisualMeta("FORMULARIO_RECIBIDO");
        assertEquals(VisualKind.FILE_CHECK, form.iconKind);
        assertEquals(AdminTarget.PERSONAL_REPORTS, form.target);

        VisualMeta approval = AdminNotificationsUiHelper.resolveVisualMeta("APROBACION_CIERRE");
        assertEquals(VisualKind.SHIELD_CHECK, approval.iconKind);
        assertEquals(AdminTarget.KPI_OFFICIAL, approval.target);
        assertTrue(approval.important);

        VisualMeta general = AdminNotificationsUiHelper.resolveVisualMeta("AVISO_GENERAL");
        assertEquals(VisualKind.BELL, general.iconKind);
        assertEquals(AdminTarget.NONE, general.target);
        assertFalse(general.important);
    }
}
