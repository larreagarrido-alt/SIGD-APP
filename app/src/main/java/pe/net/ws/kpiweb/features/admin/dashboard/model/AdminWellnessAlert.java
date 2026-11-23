package pe.net.ws.kpiweb.features.admin.dashboard.model;

/**
 * Modelo para las alertas de bienestar del equipo.
 */
public class AdminWellnessAlert {
    private final int alertCount;
    private final String title;
    private final String description;
    private final String actionLabel;

    public AdminWellnessAlert(int alertCount, String title, String description, String actionLabel) {
        this.alertCount = alertCount;
        this.title = title;
        this.description = description;
        this.actionLabel = actionLabel;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getActionLabel() {
        return actionLabel;
    }
}
