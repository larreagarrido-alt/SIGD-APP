package pe.net.ws.kpiweb.features.collaborator.dashboard.model;

/**
 * Modelo para la próxima actividad y elementos de actividad reciente.
 */
public class CollaboratorActivityItem {

    public enum ActivityType {
        ENTREGA,
        OBJETIVO,
        EVALUACION,
        ANUNCIO,
        REVISION
    }

    private final String id;
    private final String title;
    private final String category;
    private final String timeLabel;
    private final String statusLabel;
    private final String actionButtonLabel;
    private final ActivityType type;

    public CollaboratorActivityItem(String id, String title, String category,
                                  String timeLabel, String statusLabel,
                                  String actionButtonLabel, ActivityType type) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.timeLabel = timeLabel;
        this.statusLabel = statusLabel;
        this.actionButtonLabel = actionButtonLabel;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public String getActionButtonLabel() {
        return actionButtonLabel;
    }

    public ActivityType getType() {
        return type;
    }
}
