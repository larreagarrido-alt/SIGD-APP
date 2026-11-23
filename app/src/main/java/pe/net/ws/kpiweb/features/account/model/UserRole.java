package pe.net.ws.kpiweb.features.account.model;

/**
 * Roles de usuario soportados en WEBSITE.
 */
public enum UserRole {
    ADMINISTRADOR("Administrador", "Gestión Integral y Supervisión"),
    COLABORADOR("Colaborador", "Crecimiento y Gestión Personal");

    private final String displayName;
    private final String subtitle;

    UserRole(String displayName, String subtitle) {
        this.displayName = displayName;
        this.subtitle = subtitle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
