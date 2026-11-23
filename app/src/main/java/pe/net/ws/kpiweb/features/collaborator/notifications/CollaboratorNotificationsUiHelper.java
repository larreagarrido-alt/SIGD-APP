package pe.net.ws.kpiweb.features.collaborator.notifications;

import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.NotificationType;

/**
 * Lógica pura de presentación para el Centro de Notificaciones SIGD:
 * contadores reales, mensajes diferenciados por filtro y clasificación de íconos.
 */
public final class CollaboratorNotificationsUiHelper {

    public enum IconKind {
        CLIPBOARD_CHECK,
        CALENDAR_CLOCK,
        ALARM_CLOCK,
        GIFT,
        AWARD,
        HEART_HANDSHAKE,
        MEGAPHONE,
        BELL
    }

    public static final class EmptyCopy {
        public final String title;
        public final String description;
        public final String statusChip;
        public final boolean positiveTone;

        public EmptyCopy(String title, String description, String statusChip, boolean positiveTone) {
            this.title = title;
            this.description = description;
            this.statusChip = statusChip;
            this.positiveTone = positiveTone;
        }
    }

    private CollaboratorNotificationsUiHelper() {}

    public static String formatHeaderPendingStatus(int unreadCount) {
        if (unreadCount <= 0) return "Sin pendientes";
        return unreadCount == 1 ? "1 pendiente" : unreadCount + " pendientes";
    }

    public static String formatSectionCount(int visibleCount) {
        int safe = Math.max(0, visibleCount);
        return safe == 1 ? "1 notificación" : safe + " notificaciones";
    }

    public static EmptyCopy resolveEmptyCopy(String selectedCategory, int totalUnfilteredCount) {
        String cat = selectedCategory == null ? "TODAS" : selectedCategory;
        if ("NO_LEIDAS".equalsIgnoreCase(cat)) {
            return new EmptyCopy(
                    "No tienes notificaciones sin leer",
                    totalUnfilteredCount > 0
                            ? "Ya revisaste tus avisos recientes. Puedes consultarlos en la pestaña Todos."
                            : "No hay avisos pendientes por revisar en tu cuenta.",
                    "Bandeja al día",
                    true
            );
        }
        if ("IMPORTANTES".equalsIgnoreCase(cat)) {
            return new EmptyCopy(
                    "No tienes avisos importantes por ahora",
                    "Cuando haya vencimientos próximos o alertas prioritarias de KPI, aparecerán aquí.",
                    "Sin alertas prioritarias",
                    false
            );
        }
        return new EmptyCopy(
                "¡Todo al día!",
                "No tienes notificaciones por ahora. Cuando haya novedades, aparecerán aquí.",
                "No hay avisos pendientes",
                true
        );
    }

    public static IconKind resolveIconKind(NotificationType type) {
        if (type == null) return IconKind.BELL;
        switch (type) {
            case NUEVO_KPI:
            case RECORDATORIO_EVALUACION:
                return IconKind.CLIPBOARD_CHECK;
            case KPI_PROXIMO:
                return IconKind.CALENDAR_CLOCK;
            case KPI_VENCIDO:
                return IconKind.ALARM_CLOCK;
            case PREMIO_DESBLOQUEADO:
                return IconKind.GIFT;
            case ACTIVIDAD_COMPLETADA:
                return IconKind.AWARD;
            case RECORDATORIO_BIENESTAR:
                return IconKind.HEART_HANDSHAKE;
            case CAMBIO_ESTADO:
                return IconKind.MEGAPHONE;
            case AVISO_GENERAL:
            default:
                return IconKind.BELL;
        }
    }
}
