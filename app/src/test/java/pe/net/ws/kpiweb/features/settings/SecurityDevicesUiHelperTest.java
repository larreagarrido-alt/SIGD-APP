package pe.net.ws.kpiweb.features.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecurityDevicesUiHelperTest {

    @Test
    public void formatDeviceModel_combinesAndAvoidsDuplicatedManufacturer() {
        assertEquals("Samsung SM-S918B", SecurityDevicesUiHelper.formatDeviceModel("samsung", "SM-S918B"));
        assertEquals("Google Pixel 8 Pro", SecurityDevicesUiHelper.formatDeviceModel("Google", "Google Pixel 8 Pro"));
        assertEquals("Motorola moto g84", SecurityDevicesUiHelper.formatDeviceModel("motorola", "moto g84"));
        assertEquals("Dispositivo Android", SecurityDevicesUiHelper.formatDeviceModel("", ""));
    }

    @Test
    public void buildSnapshot_detectsCurrentLinkedDeviceWithFingerprintAndFace() {
        JsonArray android = new JsonArray();
        JsonObject current = new JsonObject();
        current.addProperty("id_acceso", 14);
        current.addProperty("nombre", "Pixel 8");
        current.addProperty("huella_habilitada", 1);
        android.add(current);

        JsonArray faces = new JsonArray();
        JsonObject face = new JsonObject();
        face.addProperty("id_acceso_android", 14);
        faces.add(face);

        SecurityDevicesUiHelper.SecuritySnapshot snapshot = SecurityDevicesUiHelper.buildSnapshot(
                android, faces, 14, true, false, "Google Pixel 8",
                SecurityDevicesUiHelper.HardwareStatus.AVAILABLE,
                SecurityDevicesUiHelper.HardwareStatus.AVAILABLE);

        assertEquals(SecurityDevicesUiHelper.LinkState.LINKED, snapshot.linkState);
        assertEquals("Vinculado", snapshot.linkBadgeLabel);
        assertEquals(SecurityDevicesUiHelper.BiometricState.ACTIVE, snapshot.fingerprintState);
        assertEquals("Vinculada", snapshot.fingerprintLabel);
        assertEquals(SecurityDevicesUiHelper.BiometricState.ACTIVE, snapshot.faceState);
        assertEquals("Vinculado", snapshot.faceLabel);
        assertEquals("Pixel 8 • #14", snapshot.deviceDisplayName);
        assertEquals(1, snapshot.linkedDevices.size());
        assertTrue(snapshot.linkedDevices.get(0).isCurrentDevice);
    }

    @Test
    public void buildSnapshot_handlesDisabledFingerprintAndRetainedFace() {
        JsonArray android = new JsonArray();
        JsonObject current = new JsonObject();
        current.addProperty("id_acceso", 22);
        current.addProperty("nombre", "Galaxy A54");
        current.addProperty("huella_habilitada", 0);
        android.add(current);

        JsonArray faces = new JsonArray();
        JsonObject face = new JsonObject();
        face.addProperty("id_acceso_android", 22);
        faces.add(face);

        SecurityDevicesUiHelper.SecuritySnapshot snapshot = SecurityDevicesUiHelper.buildSnapshot(
                android, faces, 22, true, true, "Samsung Galaxy A54",
                SecurityDevicesUiHelper.HardwareStatus.AVAILABLE,
                SecurityDevicesUiHelper.HardwareStatus.AVAILABLE);

        assertEquals(SecurityDevicesUiHelper.LinkState.LINKED, snapshot.linkState);
        assertEquals(SecurityDevicesUiHelper.BiometricState.DISABLED, snapshot.fingerprintState);
        assertEquals("Desactivada", snapshot.fingerprintLabel);
        assertEquals(SecurityDevicesUiHelper.BiometricState.ACTIVE, snapshot.faceState);
        assertEquals("Vinculado", snapshot.faceLabel);
    }

    @Test
    public void buildSnapshot_hidesStaleInactiveBindingOfReplacedRegistration() {
        JsonArray android = new JsonArray();
        JsonObject stale = new JsonObject();
        stale.addProperty("id_acceso", 1);
        stale.addProperty("nombre", "motorola moto g75 5G");
        stale.addProperty("huella_habilitada", 0);
        android.add(stale);
        JsonObject current = new JsonObject();
        current.addProperty("id_acceso", 6);
        current.addProperty("nombre", "motorola moto g75 5G");
        current.addProperty("huella_habilitada", 1);
        android.add(current);
        JsonArray faces = new JsonArray();
        JsonObject face = new JsonObject();
        face.addProperty("id_acceso_android", 6);
        faces.add(face);

        SecurityDevicesUiHelper.SecuritySnapshot snapshot = SecurityDevicesUiHelper.buildSnapshot(
                android, faces, 6, true, false, "Motorola moto g75 5G",
                SecurityDevicesUiHelper.HardwareStatus.AVAILABLE,
                SecurityDevicesUiHelper.HardwareStatus.AVAILABLE);

        assertEquals(1, snapshot.linkedDevices.size());
        assertEquals(6, snapshot.linkedDevices.get(0).accessId);
        assertEquals(1, snapshot.totalDevices);
    }

    @Test
    public void buildSnapshot_distinguishesAvailableNotConfiguredAndUnavailableHardware() {
        JsonArray android = new JsonArray();
        JsonObject other = new JsonObject();
        other.addProperty("id_acceso", 5);
        other.addProperty("nombre", "Xiaomi 13");
        other.addProperty("huella_habilitada", 1);
        android.add(other);

        SecurityDevicesUiHelper.SecuritySnapshot snapshot = SecurityDevicesUiHelper.buildSnapshot(
                android, new JsonArray(), 0, false, false, "Samsung Galaxy S24",
                SecurityDevicesUiHelper.HardwareStatus.NOT_CONFIGURED,
                SecurityDevicesUiHelper.HardwareStatus.UNAVAILABLE);

        assertEquals(SecurityDevicesUiHelper.LinkState.UNLINKED, snapshot.linkState);
        assertEquals("No vinculado aquí", snapshot.linkBadgeLabel);
        assertEquals(SecurityDevicesUiHelper.BiometricState.NOT_CONFIGURED, snapshot.fingerprintState);
        assertEquals("Sin configurar", snapshot.fingerprintLabel);
        assertEquals(SecurityDevicesUiHelper.BiometricState.UNAVAILABLE, snapshot.faceState);
        assertEquals("No disponible", snapshot.faceLabel);
        assertEquals("Samsung Galaxy S24", snapshot.deviceDisplayName);
        assertEquals(1, snapshot.linkedDevices.size());
        assertFalse(snapshot.linkedDevices.get(0).isCurrentDevice);
    }
}
