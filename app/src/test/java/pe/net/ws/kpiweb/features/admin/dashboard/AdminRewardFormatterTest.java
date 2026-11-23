package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.JsonObject;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminRewardFormatterTest {

    @Test
    public void formatsAndValidatesBackendDateTimes() {
        String formatted = AdminRewardFormatter.formatBackendDateTime(2026, 11, 19, 9, 5);
        assertEquals("2026-11-19 09:05:00", formatted);
        assertTrue(AdminRewardFormatter.isValidBackendDateTime(formatted));
        assertFalse(AdminRewardFormatter.isValidBackendDateTime("2026-13-01 09:00:00"));
        assertFalse(AdminRewardFormatter.isValidBackendDateTime(""));

        assertEquals("19 nov 2026", AdminRewardFormatter.formatDisplayDate(formatted));
        assertEquals("09:05", AdminRewardFormatter.formatDisplayTime(formatted));
        assertEquals("19 nov 2026 · 09:05", AdminRewardFormatter.formatCompactDateTime(formatted));
        assertArrayEquals(new int[]{2026, 11, 19, 9, 5}, AdminRewardFormatter.parseDateTimeParts(formatted));

        assertTrue(AdminRewardFormatter.isEndAfterStart("2026-11-19 09:00:00", "2026-11-20 18:00:00"));
        assertFalse(AdminRewardFormatter.isEndAfterStart("2026-11-20 18:00:00", "2026-11-19 09:00:00"));
        assertFalse(AdminRewardFormatter.isEndAfterStart("2026-11-19 09:00:00", "2026-11-19 09:00:00"));
    }

    @Test
    public void matchesTitleAndDescriptionIgnoringAccents() {
        JsonObject item = new JsonObject();
        item.addProperty("titulo", "Bono de innovación");
        item.addProperty("descripcion", "Reconocimiento especial para el equipo destacado");

        assertTrue(AdminRewardFormatter.matchesSearch(item, ""));
        assertTrue(AdminRewardFormatter.matchesSearch(item, "innovacion"));
        assertTrue(AdminRewardFormatter.matchesSearch(item, "DESTACADO"));
        assertFalse(AdminRewardFormatter.matchesSearch(item, "vacaciones"));
    }

    @Test
    public void formatsStateAndFilterLabels() {
        assertEquals("Activos", AdminRewardFormatter.formatFilterLabel("ACTIVO"));
        assertEquals("Programados", AdminRewardFormatter.formatFilterLabel("PROGRAMADO"));
        assertEquals("Vencidos", AdminRewardFormatter.formatFilterLabel("VENCIDO"));
        assertEquals("Borradores", AdminRewardFormatter.formatFilterLabel("BORRADOR"));
        assertEquals("Desactivados", AdminRewardFormatter.formatFilterLabel("DESACTIVADO"));
        assertEquals("Activo", AdminRewardFormatter.formatStateLabel("ACTIVO"));
        assertEquals("Total: 0", AdminRewardFormatter.formatTotalBadge(0));
        assertEquals("Total: 5", AdminRewardFormatter.formatTotalBadge(5));
        assertEquals("0 premios", AdminRewardFormatter.formatRewardsCount(0, 0, false));
        assertEquals("1 premio", AdminRewardFormatter.formatRewardsCount(1, 1, false));
        assertEquals("4 premios", AdminRewardFormatter.formatRewardsCount(4, 4, false));
        assertEquals("2 de 5", AdminRewardFormatter.formatRewardsCount(2, 5, true));
    }
}
