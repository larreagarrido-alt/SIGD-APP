package pe.net.ws.kpiweb.features.collaborator.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.NotificationType;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.TargetModule;
import pe.net.ws.kpiweb.core.session.SessionManager;

/**
 * Repositorio local de notificaciones del colaborador.
 * Proporciona notificaciones de prueba realistas diferenciadas por colaborador:
 * - Nuevo KPI asignado
 * - KPI próximo a vencer
 * - KPI vencido
 * - Evaluación actualizada
 * - Nuevo anuncio o actividad
 */
public class CollaboratorNotificationRepository {

    private static final String PREFS_NAME = "website_collab_notifications_prefs_v3";
    private static final String KEY_READ_PREFIX = "notif_read_";
    private static final String KEY_DISCARDED_PREFIX = "notif_discarded_";

    private static CollaboratorNotificationRepository instance;
    private final SharedPreferences prefs;
    private final SessionManager session;
    private final List<CollaboratorNotificationItem> allNotifications = new ArrayList<>();
    private final List<CollaboratorNotificationItem> remoteNotifications = new ArrayList<>();
    private int remoteWorkerId;
    private int dynamicCounter = 1;

    private CollaboratorNotificationRepository(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.session = new SessionManager(context);
        seedNotifications();
    }

    public static synchronized CollaboratorNotificationRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CollaboratorNotificationRepository(context);
        }
        return instance;
    }

    private void seedNotifications() {
        allNotifications.clear();

        // ============================================================
        // 1. LUCÍA GÓMEZ (collab_01 - Diseño y Producto)
        // ============================================================
        addSeed(new CollaboratorNotificationItem(
                "notif_l1", "collab_01", NotificationType.NUEVO_KPI,
                "Nuevo KPI asignado: Pruebas de usabilidad documentadas",
                "Se asignó el indicador 'Pruebas de usabilidad documentadas' (meta: 5 sesiones) en tu plan de Diseño y Producto.",
                "Hoy • 10:15 AM", 1730120100000L, TargetModule.MIS_KPI, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_l2", "collab_01", NotificationType.KPI_PROXIMO,
                "KPI próximo a vencer: Prototipo de onboarding",
                "Tu entregable 'Entregables UI/UX del sprint' (12/14 diseños) vence hoy a las 18:00. Registra tu avance a tiempo.",
                "Hoy • 09:40 AM", 1730118000000L, TargetModule.MIS_KPI, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_l3", "collab_01", NotificationType.RECORDATORIO_EVALUACION,
                "Evaluación actualizada: 94% de cumplimiento",
                "Tu indicador 'Calidad de Design System' fue validado con 96%, elevando tu promedio mensual a 94% y sumando +120 pts (Total: 1,240 pts • Puesto #3).",
                "Hoy • 08:30 AM", 1730113800000L, TargetModule.DASHBOARD, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_l4", "collab_01", NotificationType.PREMIO_DESBLOQUEADO,
                "Reconocimiento alcanzado: Cumplimiento de objetivos",
                "Con tus 1,240 puntos acumulados en Rango Oro tienes habilitado el reconocimiento 'Colaborador destacado del mes'.",
                "Ayer • 06:20 PM", 1730062800000L, TargetModule.PREMIOS, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_l5", "collab_01", NotificationType.KPI_VENCIDO,
                "Aviso preventivo de KPI vencido (0 vencidos actualmente)",
                "Recuerda que mantener tus 5 KPI sin vencimientos al cierre del mes te otorga el reconocimiento por constancia.",
                "Ayer • 04:10 PM", 1730055000000L, TargetModule.MIS_KPI, true
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_l6", "collab_01", NotificationType.AVISO_GENERAL,
                "Nuevo anuncio: Actualización de fechas de evaluación",
                "La coordinación de Diseño y Producto publicó el cronograma de cierre de indicadores del mes de Octubre 2026.",
                "25 Oct • 10:00 AM", 1729868400000L, TargetModule.BIENESTAR, true
        ));

        // ============================================================
        // 2. CARLOS MENDOZA (collab_02 - Área Comercial)
        // ============================================================
        addSeed(new CollaboratorNotificationItem(
                "notif_c1", "collab_02", NotificationType.KPI_VENCIDO,
                "KPI vencido: Reactivación de cartera inactiva",
                "El plazo para 'Reactivación de cartera inactiva' (3/8 cuentas) venció ayer. Registra tus gestiones pendientes en Mis KPI.",
                "Hoy • 10:05 AM", 1730119500000L, TargetModule.MIS_KPI, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_c2", "collab_02", NotificationType.KPI_PROXIMO,
                "KPI próximo a vencer: Cuota de ventas corporativas",
                "Tienes programada la reunión de cierre con Grupo Nexus mañana a las 10:30 AM para completar tu meta comercial.",
                "Hoy • 08:45 AM", 1730114700000L, TargetModule.MIS_KPI, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_c3", "collab_02", NotificationType.NUEVO_KPI,
                "Nuevo KPI asignado: Renovación de cuentas clave Q4",
                "Se habilitó la meta de retención corporativa en tu panel comercial del trimestre.",
                "Ayer • 04:15 PM", 1730055300000L, TargetModule.MIS_KPI, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_c4", "collab_02", NotificationType.RECORDATORIO_EVALUACION,
                "Evaluación actualizada: Líder #1 del ranking (1,620 pts)",
                "Mantienes la posición #1 del ranking general en Rango Platino tras el cierre del contrato anual TechCorp.",
                "26 Oct • 09:30 AM", 1729953000000L, TargetModule.RANKING_PUNTOS, true
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_c5", "collab_02", NotificationType.AVISO_GENERAL,
                "Nuevo anuncio: Comité comercial de cierre mensual",
                "Revisa tus reconocimientos alcanzados y la agenda de presentación comercial del viernes.",
                "24 Oct • 05:00 PM", 1729807200000L, TargetModule.PREMIOS, true
        ));

        // ============================================================
        // 3. ELENA ROJAS (collab_03 - Atención al Cliente)
        // ============================================================
        addSeed(new CollaboratorNotificationItem(
                "notif_e1", "collab_03", NotificationType.RECORDATORIO_EVALUACION,
                "Evaluación actualizada: En proceso de consolidación",
                "Tu evaluación mensual se encuentra en consolidación mientras se validan tus métricas CSAT (4.9/5.0).",
                "Hoy • 10:15 AM", 1730120100000L, TargetModule.DASHBOARD, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_e2", "collab_03", NotificationType.NUEVO_KPI,
                "Nuevo KPI asignado: Documentación en base de conocimiento",
                "Tienes asignada la meta de 10 guías operativas en Atención al Cliente (avance actual: 6/10).",
                "Hoy • 09:10 AM", 1730116200000L, TargetModule.MIS_KPI, false
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_e3", "collab_03", NotificationType.KPI_PROXIMO,
                "KPI próximo a vencer: Tickets de soporte resueltos",
                "Llevas 142/150 tickets resueltos. El corte semanal cierra este jueves a las 15:00.",
                "Ayer • 03:40 PM", 1730053200000L, TargetModule.MIS_KPI, true
        ));
        addSeed(new CollaboratorNotificationItem(
                "notif_e4", "collab_03", NotificationType.AVISO_GENERAL,
                "Nuevo anuncio: Reconocimiento al compromiso disponible",
                "Con tus 890 puntos en Rango Plata has cumplido los requisitos del 'Premio al compromiso'.",
                "Ayer • 10:20 AM", 1730034000000L, TargetModule.PREMIOS, true
        ));
    }

    private void addSeed(CollaboratorNotificationItem item) {
        String key = KEY_READ_PREFIX + item.getId();
        if (prefs.contains(key)) {
            item.setRead(prefs.getBoolean(key, item.isRead()));
        }
        allNotifications.add(item);
    }

    public boolean isDiscarded(String notificationId) {
        return prefs.getBoolean(KEY_DISCARDED_PREFIX + notificationId, false);
    }

    public synchronized void replaceRemoteNotifications(int workerId, List<CollaboratorNotificationItem> items) {
        if (!session.isLoggedIn() || workerId <= 0 || workerId != session.getWorkerId()) return;
        remoteWorkerId = workerId;
        remoteNotifications.clear();
        remoteNotifications.addAll(items);
    }

    private boolean realWorker(String collaboratorId) {
        return session.isLoggedIn() && session.getWorkerId() > 0
                && String.valueOf(session.getWorkerId()).equals(collaboratorId);
    }

    private List<CollaboratorNotificationItem> source(String collaboratorId) {
        return realWorker(collaboratorId) && remoteWorkerId == session.getWorkerId()
                ? remoteNotifications : realWorker(collaboratorId) ? Collections.emptyList() : allNotifications;
    }

    public void discardNotificationLocally(String notificationId) {
        prefs.edit().putBoolean(KEY_DISCARDED_PREFIX + notificationId, true).apply();
    }

    public void restoreNotificationLocally(String notificationId) {
        prefs.edit().putBoolean(KEY_DISCARDED_PREFIX + notificationId, false).apply();
    }

    public List<CollaboratorNotificationItem> getNotificationsForCollaborator(String collaboratorId, String categoryFilter) {
        List<CollaboratorNotificationItem> result = new ArrayList<>();
        for (CollaboratorNotificationItem item : source(collaboratorId)) {
            if (item.getCollaboratorId().equals(collaboratorId) && (realWorker(collaboratorId) || !isDiscarded(item.getId()))) {
                if (categoryFilter == null || "TODAS".equalsIgnoreCase(categoryFilter)) {
                    result.add(item);
                } else if ("NO_LEIDAS".equalsIgnoreCase(categoryFilter)) {
                    if (!item.isRead()) {
                        result.add(item);
                    }
                } else if ("IMPORTANTES".equalsIgnoreCase(categoryFilter)) {
                    if (item.getType() == NotificationType.KPI_VENCIDO
                            || item.getType() == NotificationType.KPI_PROXIMO
                            || item.getType() == NotificationType.CAMBIO_ESTADO) result.add(item);
                } else if (item.getType().getCategoryKey().equalsIgnoreCase(categoryFilter)) {
                    result.add(item);
                }
            }
        }
        Collections.sort(result, (a, b) -> Long.compare(b.getSortTimestamp(), a.getSortTimestamp()));
        return result;
    }

    public int getUnreadCount(String collaboratorId) {
        int count = 0;
        for (CollaboratorNotificationItem item : source(collaboratorId)) {
            if (item.getCollaboratorId().equals(collaboratorId) && !item.isRead()
                    && (realWorker(collaboratorId) || !isDiscarded(item.getId()))) {
                count++;
            }
        }
        return count;
    }

    public void markAsRead(String notificationId) {
        setReadState(notificationId, true);
    }

    public void setReadState(String notificationId, boolean read) {
        List<CollaboratorNotificationItem> candidates = new ArrayList<>(allNotifications);
        candidates.addAll(remoteNotifications);
        for (CollaboratorNotificationItem item : candidates) {
            if (item.getId().equals(notificationId)) {
                item.setRead(read);
                prefs.edit().putBoolean(KEY_READ_PREFIX + notificationId, read).apply();
                break;
            }
        }
    }

    public void markAllAsRead(String collaboratorId) {
        SharedPreferences.Editor editor = prefs.edit();
        for (CollaboratorNotificationItem item : source(collaboratorId)) {
            if (item.getCollaboratorId().equals(collaboratorId) && !item.isRead()) {
                item.setRead(true);
                editor.putBoolean(KEY_READ_PREFIX + item.getId(), true);
            }
        }
        editor.apply();
    }

    public CollaboratorNotificationItem addLocalTestReminder(String collaboratorId, String collaboratorFirstName) {
        String id = "notif_test_" + System.currentTimeMillis() + "_" + (dynamicCounter++);
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        CollaboratorNotificationItem item = new CollaboratorNotificationItem(
                id,
                collaboratorId,
                NotificationType.KPI_PROXIMO,
                "KPI próximo a vencer: Revisión de indicadores activos",
                "Hola " + collaboratorFirstName + ", recuerda verificar tus entregas del mes y tu progreso en WEBSITE.",
                "Ahora • " + timeStr,
                System.currentTimeMillis(),
                TargetModule.MIS_KPI,
                false
        );
        allNotifications.add(0, item);
        prefs.edit().putBoolean(KEY_READ_PREFIX + id, false).apply();
        return item;
    }
}
