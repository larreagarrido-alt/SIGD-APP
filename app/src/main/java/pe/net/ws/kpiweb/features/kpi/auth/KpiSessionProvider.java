package pe.net.ws.kpiweb.features.kpi.auth;

/**
 * Contrato de integración oficial con el sistema de Login y Sesión (desarrollado por Jair).
 * Desacopla por completo el módulo de KPIs para que cuando Jair libere la pantalla de login,
 * el módulo de KPI consuma directamente esta interfaz sin requerir selectores manuales.
 */
public interface KpiSessionProvider {

    String getAuthenticatedUserId();

    String getAuthenticatedUserName();

    String getAuthenticatedRole();

    boolean hasAdminPrivileges();

    boolean isAuthenticated();
}
