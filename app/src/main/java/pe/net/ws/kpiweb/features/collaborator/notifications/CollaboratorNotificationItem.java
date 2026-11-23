package pe.net.ws.kpiweb.features.collaborator.notifications;

/**
 * Modelo de una notificación del colaborador en WEBSITE.
 * Soporta todos los tipos de avisos corporativos: KPI pendiente, KPI vencido,
 * entrega próxima, puntos ganados, premio desbloqueado, recordatorio de test de bienestar,
 * actualización de evaluación y mensaje del sistema.
 */
public class CollaboratorNotificationItem {

    public enum NotificationType {
        NUEVO_KPI("KPI", "KPI pendiente"),
        KPI_PROXIMO("KPI", "Entrega próxima"),
        KPI_VENCIDO("KPI", "KPI vencido"),
        ACTIVIDAD_COMPLETADA("ACTIVIDADES", "Puntos ganados"),
        PREMIO_DESBLOQUEADO("ACTIVIDADES", "Premio desbloqueado"),
        RECORDATORIO_BIENESTAR("GENERAL", "Test de bienestar"),
        RECORDATORIO_EVALUACION("EVALUACION", "Actualización de evaluación"),
        CAMBIO_ESTADO("ACTIVIDADES", "Cambio de estado"),
        AVISO_GENERAL("GENERAL", "Mensaje del sistema");

        private final String categoryKey;
        private final String label;

        NotificationType(String categoryKey, String label) {
            this.categoryKey = categoryKey;
            this.label = label;
        }

        public String getCategoryKey() {
            return categoryKey;
        }

        public String getLabel() {
            return label;
        }
    }

    public enum TargetModule {
        MIS_KPI("Ir a Mis KPI"),
        PREMIOS("Ir a Premios"),
        RANKING_PUNTOS("Ver Ranking y Puntos"),
        BIENESTAR("Ir a Test de Bienestar"),
        DASHBOARD("Ir a Mi Inicio"),
        CONFIGURACION("Ir a Configuración"),
        MI_CUENTA("Ir a Mi Cuenta");

        private final String actionLabel;

        TargetModule(String actionLabel) {
            this.actionLabel = actionLabel;
        }

        public String getActionLabel() {
            return actionLabel;
        }
    }

    private final String id;
    private final String collaboratorId;
    private final NotificationType type;
    private final String title;
    private final String message;
    private final String dateTimeDisplay;
    private final long sortTimestamp;
    private final TargetModule targetModule;
    private boolean isRead;

    public CollaboratorNotificationItem(String id, String collaboratorId, NotificationType type,
                                        String title, String message, String dateTimeDisplay,
                                        long sortTimestamp, TargetModule targetModule, boolean isRead) {
        this.id = id;
        this.collaboratorId = collaboratorId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.dateTimeDisplay = dateTimeDisplay;
        this.sortTimestamp = sortTimestamp;
        this.targetModule = targetModule;
        this.isRead = isRead;
    }

    public String getId() {
        return id;
    }

    public String getCollaboratorId() {
        return collaboratorId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getDateTimeDisplay() {
        return dateTimeDisplay;
    }

    public long getSortTimestamp() {
        return sortTimestamp;
    }

    public TargetModule getTargetModule() {
        return targetModule;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
