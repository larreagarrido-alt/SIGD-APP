package pe.net.ws.kpiweb.features.admin.dashboard;

import java.util.Locale;

/**
 * Formatea los datos compactos del perfil y saludo del encabezado administrativo
 * sin alterar la información real proveniente de la sesión o el servidor.
 */
final class AdminDashboardHeaderFormatter {

    private AdminDashboardHeaderFormatter() {
    }

    static String extractFirstName(String rawName, String fallback) {
        if (rawName == null) {
            return fallback;
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        String[] parts = trimmed.split("\\s+");
        return parts.length > 0 && !parts[0].isEmpty() ? parts[0] : fallback;
    }

    static String extractInitials(String rawName, Locale locale) {
        if (rawName == null) {
            return "?";
        }
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        String[] parts = trimmed.split("\\s+");
        Locale safeLocale = locale == null ? Locale.getDefault() : locale;
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(safeLocale);
        }
        return parts[0].substring(0, 1).toUpperCase(safeLocale);
    }
}
