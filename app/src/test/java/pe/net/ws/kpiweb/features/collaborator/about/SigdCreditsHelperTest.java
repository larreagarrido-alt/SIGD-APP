package pe.net.ws.kpiweb.features.collaborator.about;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SigdCreditsHelperTest {

    @Test
    public void constants_matchOfficialSigdIdentityAndWebsiteCredits() {
        assertEquals("SIGD", SigdCreditsHelper.APP_NAME);
        assertEquals("Sistema Integral de Gestión de Desempeño", SigdCreditsHelper.APP_DESCRIPTIVE_NAME);
        assertEquals("Website", SigdCreditsHelper.DEVELOPER_NAME);
        assertEquals("2026", SigdCreditsHelper.COPYRIGHT_YEAR);
        assertEquals("2026-jadiel", SigdCreditsHelper.CREDIT_SIGNATURE);
        assertEquals("© 2026 WEBSITE · Todos los derechos reservados", SigdCreditsHelper.COPYRIGHT_LINE);
    }

    @Test
    public void formatInstalledVersion_usesRealMetadataAndNeverSignature() {
        String formatted = SigdCreditsHelper.formatInstalledVersion("1.0-face-self-service-20261022", 6L);
        assertEquals("1.0-face-self-service-20261022 (Build 6)", formatted);
        assertFalse(formatted.contains(SigdCreditsHelper.CREDIT_SIGNATURE));
    }

    @Test
    public void formatInstalledVersion_handlesMissingMetadataSafely() {
        assertEquals("1.2.0", SigdCreditsHelper.formatInstalledVersion("1.2.0", 0L));
        assertEquals("Build 4", SigdCreditsHelper.formatInstalledVersion("", 4L));
        assertEquals("No disponible", SigdCreditsHelper.formatInstalledVersion("", 0L));
    }
}
