package pe.net.ws.kpiweb.features.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure UI-state formatter for the Security & Devices screen.
 * Combines real server biometric bindings with real OS hardware availability
 * so no state is ever fabricated or guessed.
 */
public final class SecurityDevicesUiHelper {

    public enum LinkState {
        LINKED,
        UNLINKED
    }

    public enum BiometricState {
        ACTIVE,
        DISABLED,
        AVAILABLE,
        NOT_CONFIGURED,
        UNAVAILABLE
    }

    public enum HardwareStatus {
        AVAILABLE,
        NOT_CONFIGURED,
        UNAVAILABLE
    }

    public static final class DeviceBindingItem {
        public final int accessId;
        public final String name;
        public final boolean fingerprintActive;
        public final boolean faceActive;
        public final boolean isCurrentDevice;

        public DeviceBindingItem(int accessId, String name, boolean fingerprintActive,
                                 boolean faceActive, boolean isCurrentDevice) {
            this.accessId = accessId;
            this.name = name;
            this.fingerprintActive = fingerprintActive;
            this.faceActive = faceActive;
            this.isCurrentDevice = isCurrentDevice;
        }
    }

    public static final class SecuritySnapshot {
        public final String deviceDisplayName;
        public final LinkState linkState;
        public final String linkBadgeLabel;
        public final BiometricState fingerprintState;
        public final String fingerprintLabel;
        public final BiometricState faceState;
        public final String faceLabel;
        public final String summaryMessage;
        public final List<DeviceBindingItem> linkedDevices;
        public final int totalDevices;
        public final int totalFaces;

        public SecuritySnapshot(String deviceDisplayName, LinkState linkState, String linkBadgeLabel,
                                BiometricState fingerprintState, String fingerprintLabel,
                                BiometricState faceState, String faceLabel, String summaryMessage,
                                List<DeviceBindingItem> linkedDevices, int totalDevices, int totalFaces) {
            this.deviceDisplayName = deviceDisplayName;
            this.linkState = linkState;
            this.linkBadgeLabel = linkBadgeLabel;
            this.fingerprintState = fingerprintState;
            this.fingerprintLabel = fingerprintLabel;
            this.faceState = faceState;
            this.faceLabel = faceLabel;
            this.summaryMessage = summaryMessage;
            this.linkedDevices = Collections.unmodifiableList(linkedDevices);
            this.totalDevices = totalDevices;
            this.totalFaces = totalFaces;
        }
    }

    private SecurityDevicesUiHelper() { }

    /** Builds a clean human-readable model label from Android Build manufacturer & model. */
    public static String formatDeviceModel(String manufacturer, String model) {
        String cleanMan = manufacturer == null ? "" : manufacturer.trim();
        String cleanMod = model == null ? "" : model.trim();
        if (cleanMan.isEmpty() && cleanMod.isEmpty()) {
            return "Dispositivo Android";
        }
        if (cleanMan.isEmpty()) {
            return capitalize(cleanMod);
        }
        if (cleanMod.isEmpty()) {
            return capitalize(cleanMan);
        }
        if (cleanMod.toLowerCase(Locale.ROOT).startsWith(cleanMan.toLowerCase(Locale.ROOT))) {
            return capitalize(cleanMod);
        }
        return capitalize(cleanMan) + " " + cleanMod;
    }

    /** Convenience overload assuming available OS hardware when hardware status is omitted. */
    public static SecuritySnapshot buildSnapshot(JsonArray androidDevices, JsonArray faceBindings,
                                                 int localAccessId, boolean localUsernameMatches,
                                                 boolean localFingerprintDisabled,
                                                 String fallbackDeviceModel) {
        return buildSnapshot(androidDevices, faceBindings, localAccessId, localUsernameMatches,
                localFingerprintDisabled, fallbackDeviceModel,
                HardwareStatus.AVAILABLE, HardwareStatus.AVAILABLE);
    }

    /**
     * Parses server arrays ("android" and "rostro") together with local device metadata
     * and real OS hardware availability into a structured snapshot for the "Dispositivo actual" card.
     */
    public static SecuritySnapshot buildSnapshot(JsonArray androidDevices, JsonArray faceBindings,
                                                 int localAccessId, boolean localUsernameMatches,
                                                 boolean localFingerprintDisabled,
                                                 String fallbackDeviceModel,
                                                 HardwareStatus fingerprintHardware,
                                                 HardwareStatus faceHardware) {
        String baseModel = (fallbackDeviceModel == null || fallbackDeviceModel.trim().isEmpty())
                ? "Dispositivo Android"
                : fallbackDeviceModel.trim();
        HardwareStatus safeFpHw = fingerprintHardware == null ? HardwareStatus.UNAVAILABLE : fingerprintHardware;
        HardwareStatus safeFaceHw = faceHardware == null ? HardwareStatus.UNAVAILABLE : faceHardware;

        Set<Integer> faceAccessIds = new HashSet<>();
        int faceCount = 0;
        if (faceBindings != null) {
            faceCount = faceBindings.size();
            for (int i = 0; i < faceBindings.size(); i++) {
                JsonElement el = faceBindings.get(i);
                if (el != null && el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("id_acceso_android") && !obj.get("id_acceso_android").isJsonNull()) {
                        faceAccessIds.add(obj.get("id_acceso_android").getAsInt());
                    }
                }
            }
        }

        List<DeviceBindingItem> items = new ArrayList<>();
        DeviceBindingItem currentItem = null;
        if (androidDevices != null) {
            for (int i = 0; i < androidDevices.size(); i++) {
                JsonElement el = androidDevices.get(i);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject row = el.getAsJsonObject();
                int accessId = row.has("id_acceso") && !row.get("id_acceso").isJsonNull()
                        ? row.get("id_acceso").getAsInt() : 0;
                String name = row.has("nombre") && !row.get("nombre").isJsonNull()
                        ? row.get("nombre").getAsString().trim() : "";
                if (name.isEmpty()) {
                    name = "Dispositivo #" + accessId;
                }
                boolean fpActive = row.has("huella_habilitada") && !row.get("huella_habilitada").isJsonNull()
                        && row.get("huella_habilitada").getAsInt() == 1;
                boolean faceActive = faceAccessIds.contains(accessId);
                boolean isCurrent = localUsernameMatches && localAccessId > 0 && accessId == localAccessId;

                // Old bindings replaced by a newer registration (no fingerprint, no face) are
                // history, not linked devices. Keep only the current one so recovery still works.
                if (!fpActive && !faceActive && !isCurrent) continue;
                DeviceBindingItem item = new DeviceBindingItem(accessId, name, fpActive, faceActive, isCurrent);
                items.add(item);
                if (isCurrent) {
                    currentItem = item;
                }
            }
        }

        String displayName = baseModel;
        LinkState linkState;
        String linkBadgeLabel;
        BiometricState fpState;
        String fpLabel;
        BiometricState faceState;
        String faceLabel;

        if (currentItem != null) {
            displayName = currentItem.name + " • #" + currentItem.accessId;
            linkState = LinkState.LINKED;
            linkBadgeLabel = "Vinculado";
            if (currentItem.fingerprintActive) {
                fpState = BiometricState.ACTIVE;
                fpLabel = "Vinculada";
            } else {
                fpState = BiometricState.DISABLED;
                fpLabel = "Desactivada";
            }
            if (currentItem.faceActive) {
                faceState = BiometricState.ACTIVE;
                faceLabel = "Vinculado";
            } else {
                faceState = resolveUnlinkedState(safeFaceHw);
                faceLabel = resolveUnlinkedFaceLabel(safeFaceHw);
            }
        } else {
            linkState = LinkState.UNLINKED;
            linkBadgeLabel = items.isEmpty() ? "Sin vínculo" : "No vinculado aquí";
            if (localUsernameMatches && localAccessId > 0 && localFingerprintDisabled) {
                fpState = BiometricState.DISABLED;
                fpLabel = "Desactivada";
            } else {
                fpState = resolveUnlinkedState(safeFpHw);
                fpLabel = resolveUnlinkedFingerprintLabel(safeFpHw);
            }
            faceState = resolveUnlinkedState(safeFaceHw);
            faceLabel = resolveUnlinkedFaceLabel(safeFaceHw);
        }

        String summaryMessage = formatSummaryMessage(items.size(), faceCount, currentItem != null);
        return new SecuritySnapshot(
                displayName, linkState, linkBadgeLabel,
                fpState, fpLabel, faceState, faceLabel,
                summaryMessage, items, items.size(), faceCount
        );
    }

    private static BiometricState resolveUnlinkedState(HardwareStatus hardware) {
        switch (hardware) {
            case AVAILABLE:
                return BiometricState.AVAILABLE;
            case NOT_CONFIGURED:
                return BiometricState.NOT_CONFIGURED;
            case UNAVAILABLE:
            default:
                return BiometricState.UNAVAILABLE;
        }
    }

    private static String resolveUnlinkedFingerprintLabel(HardwareStatus hardware) {
        switch (hardware) {
            case AVAILABLE:
                return "Disponible";
            case NOT_CONFIGURED:
                return "Sin configurar";
            case UNAVAILABLE:
            default:
                return "No disponible";
        }
    }

    private static String resolveUnlinkedFaceLabel(HardwareStatus hardware) {
        switch (hardware) {
            case AVAILABLE:
                return "Disponible";
            case NOT_CONFIGURED:
                return "Sin configurar";
            case UNAVAILABLE:
            default:
                return "No disponible";
        }
    }

    private static String formatSummaryMessage(int deviceCount, int faceCount, boolean currentLinked) {
        if (deviceCount == 0 && faceCount == 0) {
            return "Tu cuenta aún no tiene dispositivos ni métodos biométricos vinculados.";
        }
        String devWord = deviceCount == 1 ? "1 dispositivo vinculado" : deviceCount + " dispositivos vinculados";
        String faceWord = faceCount == 1 ? "1 rostro vinculado" : faceCount + " rostros vinculados";
        if (currentLinked) {
            return "Cuenta sincronizada: " + devWord + " y " + faceWord + ".";
        }
        return "Tienes " + devWord + " (" + faceWord + ") en tu cuenta, pero este equipo aún no está vinculado.";
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        char first = value.charAt(0);
        if (Character.isUpperCase(first)) return value;
        return Character.toUpperCase(first) + value.substring(1);
    }
}
