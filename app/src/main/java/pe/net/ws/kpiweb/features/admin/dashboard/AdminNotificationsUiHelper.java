package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.EmptyCopy;

/**
 * Lógica pura de presentación y clasificación para el Centro de Notificaciones del Administrador.
 * Mantiene estricta separación entre avisos personales del servidor y métricas del resumen operativo.
 */
public final class AdminNotificationsUiHelper {

    public enum FilterTab {
        TODOS,
        NO_LEIDOS,
        IMPORTANTES
    }

    public enum VisualKind {
        FILE_CHECK,
        CLIPBOARD_CHECK,
        CALENDAR_CLOCK,
        ALARM_CLOCK,
        SHIELD_CHECK,
        USERS_ROUND,
        GIFT,
        BELL
    }

    public enum AdminTarget {
        NONE(""),
        PERSONAL_REPORTS("Ver reportes"),
        KPI_OFFICIAL("Ir a KPI oficial"),
        REWARDS("Ver premios"),
        PERSONAL("Ver personal");

        public final String actionLabel;

        AdminTarget(String actionLabel) {
            this.actionLabel = actionLabel;
        }
    }

    public static final class VisualMeta {
        public final VisualKind iconKind;
        public final String badgeLabel;
        public final AdminTarget target;
        public final boolean important;

        public VisualMeta(VisualKind iconKind, String badgeLabel, AdminTarget target, boolean important) {
            this.iconKind = iconKind;
            this.badgeLabel = badgeLabel;
            this.target = target;
            this.important = important;
        }
    }

    public static final class AdminNoticeItem {
        public final long id;
        public final String tipo;
        public final String titulo;
        public final String mensaje;
        public final String referenciaTipo;
        public final long referenciaId;
        public final String creadoEn;
        public final long sortTimestamp;
        public boolean leida;

        public AdminNoticeItem(long id, String tipo, String titulo, String mensaje,
                               String referenciaTipo, long referenciaId,
                               String creadoEn, long sortTimestamp, boolean leida) {
            this.id = id;
            this.tipo = tipo;
            this.titulo = titulo;
            this.mensaje = mensaje;
            this.referenciaTipo = referenciaTipo;
            this.referenciaId = referenciaId;
            this.creadoEn = creadoEn;
            this.sortTimestamp = sortTimestamp;
            this.leida = leida;
        }
    }

    private AdminNotificationsUiHelper() {}

    public static String formatHeaderPendingStatus(int unreadCount) {
        return CollaboratorNotificationsUiHelper.formatHeaderPendingStatus(unreadCount);
    }

    public static String formatSectionCount(int visibleCount) {
        return CollaboratorNotificationsUiHelper.formatSectionCount(visibleCount);
    }

    public static int countUnread(List<AdminNoticeItem> items) {
        if (items == null) return 0;
        int unread = 0;
        for (AdminNoticeItem item : items) {
            if (item != null && !item.leida) unread++;
        }
        return unread;
    }

    public static boolean isImportant(AdminNoticeItem item) {
        if (item == null) return false;
        return resolveVisualMeta(item.tipo).important;
    }

    public static List<AdminNoticeItem> filterNotices(List<AdminNoticeItem> items, FilterTab filter) {
        List<AdminNoticeItem> out = new ArrayList<>();
        if (items == null) return out;
        FilterTab active = filter == null ? FilterTab.TODOS : filter;
        for (AdminNoticeItem item : items) {
            if (item == null) continue;
            if (active == FilterTab.TODOS) {
                out.add(item);
            } else if (active == FilterTab.NO_LEIDOS) {
                if (!item.leida) out.add(item);
            } else if (active == FilterTab.IMPORTANTES) {
                if (isImportant(item)) out.add(item);
            }
        }
        return out;
    }

    public static EmptyCopy resolveEmptyCopy(FilterTab filter, int totalUnfilteredCount) {
        FilterTab active = filter == null ? FilterTab.TODOS : filter;
        if (active == FilterTab.NO_LEIDOS) {
            return new EmptyCopy(
                    "No tienes avisos sin leer",
                    totalUnfilteredCount > 0
                            ? "Ya revisaste tus avisos recientes. Puedes consultarlos en la pestaña Todos."
                            : "No hay avisos pendientes por revisar en tu cuenta administrativa.",
                    "Bandeja al día",
                    true
            );
        }
        if (active == FilterTab.IMPORTANTES) {
            return new EmptyCopy(
                    "No tienes avisos importantes por ahora",
                    "Cuando haya alertas prioritarias de KPI, aprobaciones o cierres, aparecerán aquí.",
                    "Sin alertas prioritarias",
                    false
            );
        }
        return new EmptyCopy(
                "¡Todo al día!",
                "No tienes notificaciones administrativas por ahora. Cuando haya novedades, aparecerán aquí.",
                "No hay avisos pendientes",
                true
        );
    }

    public static VisualMeta resolveVisualMeta(String rawTipo) {
        String kind = rawTipo == null ? "" : rawTipo.trim().toUpperCase(Locale.ROOT);
        if (kind.equals("KPI_VENCIDO")) {
            return new VisualMeta(VisualKind.ALARM_CLOCK, "KPI vencido", AdminTarget.KPI_OFFICIAL, true);
        }
        if (kind.equals("KPI_POR_VENCER")) {
            return new VisualMeta(VisualKind.CALENDAR_CLOCK, "Recordatorio", AdminTarget.KPI_OFFICIAL, true);
        }
        if (kind.equals("KPI_REVISADO") || kind.contains("EVALUACION")) {
            return new VisualMeta(VisualKind.CLIPBOARD_CHECK, "Evaluación KPI", AdminTarget.PERSONAL_REPORTS, false);
        }
        if (kind.contains("FORMULARIO") || kind.contains("REPORTE")) {
            return new VisualMeta(VisualKind.FILE_CHECK, "Formulario KPI", AdminTarget.PERSONAL_REPORTS, false);
        }
        if (kind.contains("APROBACION") || kind.contains("CIERRE") || kind.contains("POLITICA")) {
            return new VisualMeta(VisualKind.SHIELD_CHECK, "Aprobación", AdminTarget.KPI_OFFICIAL, true);
        }
        if (kind.equals("PREMIO_DISPONIBLE") || kind.equals("CANJE_ACTUALIZADO") || kind.contains("PREMIO") || kind.contains("CANJE")) {
            return new VisualMeta(VisualKind.GIFT, "Premios", AdminTarget.REWARDS, false);
        }
        if (kind.contains("EQUIPO") || kind.contains("PERSONAL") || kind.contains("TRABAJADOR")) {
            return new VisualMeta(VisualKind.USERS_ROUND, "Equipo", AdminTarget.PERSONAL, false);
        }
        if (kind.contains("ALERTA") || kind.contains("URGENTE")) {
            return new VisualMeta(VisualKind.ALARM_CLOCK, "Importante", AdminTarget.NONE, true);
        }
        return new VisualMeta(VisualKind.BELL, "Aviso general", AdminTarget.NONE, false);
    }

    public static List<AdminNoticeItem> parseNotifications(JsonArray rows) {
        List<AdminNoticeItem> out = new ArrayList<>();
        if (rows == null) return out;
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            AdminNoticeItem item = parseNotice(element.getAsJsonObject());
            if (item != null) out.add(item);
        }
        return out;
    }

    public static AdminNoticeItem parseNotice(JsonObject row) {
        if (row == null) return null;
        try {
            if (!row.has("id_notificacion") || row.get("id_notificacion").isJsonNull()) return null;
            long id = row.get("id_notificacion").getAsLong();
            if (id <= 0) return null;
            String tipo = str(row, "tipo");
            String titulo = str(row, "titulo");
            String mensaje = str(row, "mensaje");
            String refTipo = str(row, "referencia_tipo");
            long refId = longVal(row, "referencia_id");
            String creadoEn = str(row, "creado_en");
            boolean leida = row.has("leida") && !row.get("leida").isJsonNull()
                    && row.get("leida").getAsInt() == 1;
            long timestamp = parseTimestamp(creadoEn);
            return new AdminNoticeItem(id, tipo, titulo, mensaje, refTipo, refId, creadoEn, timestamp, leida);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static long parseTimestamp(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return 0L;
        String clean = rawDate.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                Date parsed = new SimpleDateFormat(pattern, Locale.US).parse(clean);
                if (parsed != null) return parsed.getTime();
            } catch (Exception ignored) {
                // Try next format.
            }
        }
        return 0L;
    }

    private static String str(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString().trim() : "";
    }

    private static long longVal(JsonObject row, String key) {
        try {
            return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsLong() : 0L;
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
