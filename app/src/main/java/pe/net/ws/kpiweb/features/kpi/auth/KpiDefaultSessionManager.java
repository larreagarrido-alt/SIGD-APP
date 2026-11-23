package pe.net.ws.kpiweb.features.kpi.auth;

/**
 * Gestor central de sesión y modo visual KPI (SIGD 2026).
 * Regla oficial de clasificación:
 * - ⚡ PRACTICANTES (Estilo Eléctrico / Energía):
 * 1. Programador Junior (Programador)
 * 2. Diseñador Gráfico Junior
 * + Cualquier nuevo Practicante agregado de otra área.
 * - 👔 COLABORADORES (Estilo Elegante Ejecutivo):
 * 1. Asesor de Ventas
 * 2. Diseñador Gráfico
 * 3. Diseñador Gráfico Senior
 * + Cualquier nuevo Colaborador agregado de otra área.
 */
public class KpiDefaultSessionManager implements KpiSessionProvider {

    private static volatile KpiDefaultSessionManager instance;

    private String userId = "EMP-2026-001";
    private String userName = "Colaborador WebSite+";
    private String userRole = "Diseñador Gráfico";
    private boolean practicanteMode = false;
    private boolean isAdmin = true;
    private boolean authenticated = true;

    private KpiDefaultSessionManager() {
    }

    public static synchronized KpiDefaultSessionManager getInstance() {
        if (instance == null) {
            instance = new KpiDefaultSessionManager();
        }
        return instance;
    }

    public void setSession(String userId, String userName, String userRole, boolean isAdmin) {
        setSession(userId, userName, userRole, null, isAdmin);
    }

    public void setSession(String userId, String userName, String userRole, String tipo, boolean isAdmin) {
        this.userId = userId;
        this.userName = userName;
        this.isAdmin = isAdmin;
        this.authenticated = true;
        if (userRole != null && !userRole.trim().isEmpty()) {
            this.userRole = normalizeOperationalRole(userRole.trim());
        }
        if (tipo != null && !tipo.trim().isEmpty()) {
            this.practicanteMode = tipo.trim().equalsIgnoreCase("Practicante");
        } else {
            this.practicanteMode = isRoleNaturallyPracticante(this.userRole);
        }
    }

    public String getWorkerTypeLabel() {
        return practicanteMode ? "Practicante" : "Colaborador";
    }

    public String getPrefixedUserName() {
        if (userName == null || userName.isEmpty() || userName.equals("Colaborador WebSite+")) {
            return getWorkerTypeLabel();
        }
        return getWorkerTypeLabel() + ": " + userName;
    }

    public String getPrefixedUserNameUppercase() {
        if (userName == null || userName.isEmpty() || userName.equals("Colaborador WebSite+")) {
            return getWorkerTypeLabel().toUpperCase(java.util.Locale.ROOT);
        }
        return getWorkerTypeLabel().toUpperCase(java.util.Locale.ROOT) + ": "
                + userName.toUpperCase(java.util.Locale.ROOT);
    }

    public static String normalizeOperationalRole(String rawRole) {
        if (rawRole == null || rawRole.trim().isEmpty()) {
            return "Diseñador Gráfico";
        }
        String clean = rawRole.trim();
        String upper = clean.toUpperCase(java.util.Locale.ROOT)
                .replace("Ñ", "N")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");

        if (upper.contains("VIDEOGRAFO") || upper.contains("VIDEO")) {
            return "Videógrafo";
        }
        if (upper.contains("TRAFFICKER") || upper.contains("PAUTA")) {
            return "Trafficker Digital";
        }
        if (upper.contains("FOTOGRAFO") || upper.contains("FOTOGRAFIA") || upper.contains("AUDIOVISUAL")) {
            return "Fotógrafo";
        }
        if (upper.contains("CONTADOR") || upper.contains("CONTABILIDAD") || upper.contains("FINANZA")) {
            return "Contador";
        }
        if (upper.contains("ASISTENTE") && upper.contains("ADMIN")) {
            return "Asistente Administrativo";
        }
        if (upper.contains("ADMINISTRADOR") || upper.contains("ADMINISTRACION")) {
            return "Administrador";
        }
        if (upper.contains("GERENTE") || upper.contains("GERENCIA") || upper.contains("DIRECTOR")
                || upper.equals("ADMIN")) {
            return "Gerente General";
        }
        // Excepción / Regla para Creativa/Diseño: donde aparezca, vincularla como
        // Diseñador Gráfico
        if (upper.contains("CREATIV")
                || (upper.contains("DISEN") && !upper.contains("SENIOR") && !upper.contains("JUNIOR"))) {
            return "Diseñador Gráfico";
        }
        if (upper.contains("SENIOR")) {
            return "Diseñador Gráfico Senior";
        }
        if (upper.contains("VENTA") || upper.contains("COMERCIAL") || upper.contains("ASESOR")) {
            return "Asesor de Ventas";
        }
        if (upper.contains("PROGRAM") || upper.contains("DEV") || upper.contains("DESARROLL")) {
            return "Programador Junior";
        }
        return clean;
    }

    public void setActiveRole(String userRole) {
        if (userRole != null && !userRole.trim().isEmpty()) {
            this.userRole = normalizeOperationalRole(userRole.trim());
            this.practicanteMode = isRoleNaturallyPracticante(this.userRole);
        }
    }

    public static boolean isRoleNaturallyPracticante(String role) {
        if (role == null)
            return false;
        String upper = role.toUpperCase();
        if (upper.contains("SENIOR"))
            return false;
        return upper.contains("JUNIOR")
                || upper.contains("PRACTICANTE")
                || upper.contains("PROGRAMADOR");
    }

    public void setActiveRoleAndMode(String userRole, boolean isPracticanteMode) {
        if (userRole != null && !userRole.trim().isEmpty()) {
            this.userRole = normalizeOperationalRole(userRole.trim());
        }
        this.practicanteMode = isPracticanteMode;
    }

    public boolean isPracticanteMode() {
        return practicanteMode;
    }

    public void setPracticanteMode(boolean practicanteMode) {
        this.practicanteMode = practicanteMode;
    }

    @Override
    public String getAuthenticatedUserId() {
        return userId;
    }

    @Override
    public String getAuthenticatedUserName() {
        return userName;
    }

    @Override
    public String getAuthenticatedRole() {
        return userRole;
    }

    @Override
    public boolean hasAdminPrivileges() {
        return isAdmin;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }
}
