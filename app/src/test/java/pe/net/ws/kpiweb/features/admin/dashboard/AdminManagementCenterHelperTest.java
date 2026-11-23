package pe.net.ws.kpiweb.features.admin.dashboard;

import org.junit.Test;

import java.util.List;

import pe.net.ws.kpiweb.features.admin.dashboard.AdminManagementCenterHelper.ModuleId;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminManagementCenterHelper.ModuleItem;

import static org.junit.Assert.assertEquals;

public class AdminManagementCenterHelperTest {

    @Test
    public void keepsReportsInBottomNavigationAndBothEditorsInManagement() {
        List<ModuleItem> modules = AdminManagementCenterHelper.buildAdminModules();
        assertEquals(5, modules.size());
        assertEquals(ModuleId.KPI_SHEET_EDITOR, modules.get(0).id);
        assertEquals(ModuleId.SCHEDULE_EDITOR, modules.get(1).id);

        ModuleItem ranking = modules.get(2);
        assertEquals(ModuleId.RANKING, ranking.id);
        assertEquals("Ranking", ranking.title);
        assertEquals("Clasificación del equipo", ranking.description);
        assertEquals(0xFFFFC75A, ranking.iconColorHex);
        assertEquals(0xFF4B3419, ranking.iconBgHex);

        ModuleItem rewards = modules.get(3);
        assertEquals(ModuleId.REWARDS, rewards.id);
        assertEquals("Premios", rewards.title);
        assertEquals("Catálogo y recompensas", rewards.description);
        assertEquals(0xFFFFAA70, rewards.iconColorHex);
        assertEquals(0xFF4B2B23, rewards.iconBgHex);

        ModuleItem wellness = modules.get(4);
        assertEquals(ModuleId.WELLNESS, wellness.id);
        assertEquals("Bienestar", wellness.title);
        assertEquals("Seguimiento autorizado", wellness.description);
        assertEquals(0xFFFF819D, wellness.iconColorHex);
        assertEquals(0xFF4C2338, wellness.iconBgHex);
    }

    @Test
    public void formatsDynamicModuleCountProperly() {
        assertEquals("0 módulos", AdminManagementCenterHelper.formatModuleCount(0));
        assertEquals("1 módulo", AdminManagementCenterHelper.formatModuleCount(1));
        assertEquals("4 módulos", AdminManagementCenterHelper.formatModuleCount(4));
    }
}
