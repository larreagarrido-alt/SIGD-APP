package pe.net.ws.kpiweb.features.admin.dashboard;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.junit.Test;

public class AdminDashboardHeaderFormatterTest {

    private final Locale localeEs = new Locale("es", "ES");

    @Test
    public void extractFirstName_returnsOnlyFirstWordForFullName() {
        assertEquals("Manuel", AdminDashboardHeaderFormatter.extractFirstName(
                "Manuel Eduardo Castillo Medrano", "Cuenta"));
        assertEquals("Luz", AdminDashboardHeaderFormatter.extractFirstName(
                "  Luz   Camila  ", "Cuenta"));
    }

    @Test
    public void extractFirstName_returnsFallbackForBlankInput() {
        assertEquals("Cuenta", AdminDashboardHeaderFormatter.extractFirstName("", "Cuenta"));
        assertEquals("Cuenta", AdminDashboardHeaderFormatter.extractFirstName("   ", "Cuenta"));
        assertEquals("Cuenta", AdminDashboardHeaderFormatter.extractFirstName(null, "Cuenta"));
    }

    @Test
    public void extractInitials_returnsTwoInitialsWhenMultipleWordsExist() {
        assertEquals("ME", AdminDashboardHeaderFormatter.extractInitials(
                "Manuel Eduardo Castillo Medrano", localeEs));
        assertEquals("M", AdminDashboardHeaderFormatter.extractInitials("manuel", localeEs));
        assertEquals("?", AdminDashboardHeaderFormatter.extractInitials("  ", localeEs));
    }
}
