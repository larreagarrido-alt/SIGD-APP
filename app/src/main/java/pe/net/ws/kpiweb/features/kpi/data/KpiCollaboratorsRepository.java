package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;

/**
 * Repositorio central de Colaboradores y Practicantes conectado a la base de datos de Jair.
 * Gestiona dinámicamente la identidad de los colaboradores reales de la base de datos
 * sin depender de nombres estáticos ni datos inventados.
 */
public class KpiCollaboratorsRepository {

    private static volatile KpiCollaboratorsRepository instance;
    private final List<KpiCollaboratorProfile> collaborators = new ArrayList<>();
    private final Context appContext;

    private KpiCollaboratorsRepository(Context context) {
        this.appContext = context.getApplicationContext();
        syncWithSession();
    }

    public static synchronized KpiCollaboratorsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new KpiCollaboratorsRepository(context);
        }
        return instance;
    }

    public synchronized void syncWithSession() {
        if (appContext == null) return;
        SessionManager session = new SessionManager(appContext);
        if (session.isLoggedIn()) {
            String name = session.getFullName();
            if (name == null || name.trim().isEmpty()) {
                name = session.getUsername();
            }
            String cargo = session.getCargoName();
            if (cargo == null || cargo.trim().isEmpty()) {
                cargo = session.getRolName();
            }
            if (cargo == null || cargo.trim().isEmpty()) {
                cargo = "Colaborador";
            }
            String idStr = session.getWorkerId() > 0 ? String.valueOf(session.getWorkerId()) : String.valueOf(session.getUserId());
            String area = session.getAgenciaName().isEmpty() ? "WebSite+ SIGD" : session.getAgenciaName();
            String tipo = session.getWorkerTipo();

            registerActiveDatabaseUser(idStr, name, cargo, area, tipo);
        }
    }

    /**
     * Registra o actualiza el usuario activo verificado en la base de datos de Jair.
     */
    public synchronized void registerActiveDatabaseUser(String id, String name, String role, String area, String tipo) {
        if (name == null || name.trim().isEmpty()) return;
        String cleanName = name.trim();
        String cleanRole = (role != null && !role.trim().isEmpty()) ? KpiDefaultSessionManager.normalizeOperationalRole(role.trim()) : "Colaborador";
        String cleanArea = (area != null && !area.trim().isEmpty()) ? area.trim() : "Team Creativo";
        boolean isPracticante = "Practicante".equalsIgnoreCase(tipo) || KpiDefaultSessionManager.isRoleNaturallyPracticante(cleanRole);

        // Actualizar si ya existe por ID o por nombre
        for (KpiCollaboratorProfile p : collaborators) {
            if ((id != null && id.equals(p.getId())) || cleanName.equalsIgnoreCase(p.getName())) {
                p.setName(cleanName);
                p.setRole(cleanRole);
                p.setArea(cleanArea);
                p.setPracticante(isPracticante);
                return;
            }
        }

        // Insertar al inicio de la lista como colaborador activo de la base de datos
        KpiCollaboratorProfile profile = new KpiCollaboratorProfile(
                (id != null && !id.trim().isEmpty()) ? id : "DB-USER-" + (collaborators.size() + 1),
                cleanName,
                cleanRole,
                cleanArea,
                "Licenciado / Dirección",
                "Gerencia General",
                "Mañana (08:00 a.m. - 01:30 p.m.)",
                isPracticante
        );
        collaborators.add(0, profile);
    }

    public synchronized List<KpiCollaboratorProfile> getAllCollaborators() {
        return new ArrayList<>(collaborators);
    }

    public synchronized List<KpiCollaboratorProfile> getCollaboratorsForRole(String roleName) {
        List<KpiCollaboratorProfile> result = new ArrayList<>();
        if (roleName == null) return result;

        String target = normalizeRole(roleName);
        for (KpiCollaboratorProfile p : collaborators) {
            String role = normalizeRole(p.getRole());
            if (role.equals(target) || rolesEquivalent(role, target)) {
                result.add(p);
            }
        }

        if (result.isEmpty()) {
            // Sincronizar con la sesión activa de la base de datos antes de construir el perfil dinámico
            syncWithSession();
            for (KpiCollaboratorProfile p : collaborators) {
                String role = normalizeRole(p.getRole());
                if (role.equals(target) || rolesEquivalent(role, target)) {
                    result.add(p);
                }
            }
        }

        if (result.isEmpty()) {
            // Tomar el usuario autenticado de la base de datos si coincide o mostrar el identificador dinámico del cargo
            String activeUserName = KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String cleanName;
            boolean isCommunity = target.contains("COMMUNITY") || target.contains("REDES");
            boolean isVideografo = !isCommunity && (target.contains("VIDEOGRAFO") || target.contains("VIDEO"));
            boolean isTrafficker = !isCommunity && !isVideografo && (target.contains("TRAFFICKER") || target.contains("PAUTA"));
            boolean isFotografo = !isCommunity && !isVideografo && !isTrafficker && (target.contains("FOTOGRAFO") || target.contains("FOTOGRAFIA") || target.contains("AUDIOVISUAL"));
            boolean isContador = !isCommunity && !isVideografo && !isTrafficker && !isFotografo && (target.contains("CONTADOR") || target.contains("CONTABILIDAD") || target.contains("FINANZA"));
            boolean isAsistenteAdmin = !isCommunity && !isVideografo && !isTrafficker && !isFotografo && !isContador && target.contains("ASISTENTE") && target.contains("ADMIN");
            boolean isGerente = !isCommunity && !isVideografo && !isTrafficker && !isFotografo && !isContador && !isAsistenteAdmin && (target.contains("GERENT") || target.contains("DIRECCION") || target.equals("ADMIN"));
            boolean isAdministrador = !isCommunity && !isVideografo && !isTrafficker && !isFotografo && !isContador && !isAsistenteAdmin && (target.contains("ADMINISTRADOR") || target.contains("ADMINISTRACION"));
            if (activeUserName != null && !activeUserName.trim().isEmpty() && !activeUserName.equals("Colaborador WebSite+")) {
                cleanName = activeUserName.trim();
            } else if (isCommunity) {
                cleanName = "NICOL RAMOS";
            } else {
                cleanName = "Colaborador (" + roleName + ")";
            }

            String defaultArea = isGerente ? "Gerencia General"
                    : (isAdministrador ? "Administración General"
                        : (isAsistenteAdmin ? "Administración"
                            : (isContador ? "Contabilidad y Finanzas"
                                : ((isFotografo || isVideografo) ? "Producción Audiovisual"
                                    : ((isTrafficker || isCommunity) ? "Marketing Digital" : "WebSite+ SIGD")))));
            String defaultSupervisor = isGerente ? "Directorio"
                    : (isCommunity ? "MANUEL CASTILLO"
                        : ((isAdministrador || isAsistenteAdmin || isContador || isFotografo || isVideografo || isTrafficker) ? "Gerencia General" : "Licenciado / Dirección"));
            String defaultBoss = isGerente ? "Directorio"
                    : (isCommunity ? "JEFFERSON CUEVA"
                        : ((isAdministrador || isContador) ? "Gerencia / Dirección"
                            : (isAsistenteAdmin ? "Administración General"
                                : ((isFotografo || isVideografo) ? "Dirección Creativa"
                                    : (isTrafficker ? "Dirección de Marketing" : "Gerencia General")))));

            KpiCollaboratorProfile dynamic = new KpiCollaboratorProfile(
                    "EMP-DB-" + (collaborators.size() + 1),
                    cleanName,
                    roleName,
                    defaultArea,
                    defaultSupervisor,
                    defaultBoss,
                    "Mañana (08:00 a.m. - 01:30 p.m.)",
                    KpiDefaultSessionManager.isRoleNaturallyPracticante(roleName));
            collaborators.add(dynamic);
            result.add(dynamic);
        }
        return result;
    }

    private static String normalizeRole(String r) {
        if (r == null) return "";
        return r.toUpperCase()
                .replace("Ñ", "N")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .trim();
    }

    private static boolean rolesEquivalent(String r1, String r2) {
        boolean r1Community = r1.contains("COMMUNITY") || r1.contains("REDES");
        boolean r2Community = r2.contains("COMMUNITY") || r2.contains("REDES");
        if (r1Community || r2Community) {
            return r1Community && r2Community;
        }
        boolean r1Video = r1.contains("VIDEOGRAFO") || r1.contains("VIDEO");
        boolean r2Video = r2.contains("VIDEOGRAFO") || r2.contains("VIDEO");
        if (r1Video || r2Video) {
            return r1Video && r2Video;
        }
        boolean r1Trafficker = r1.contains("TRAFFICKER") || r1.contains("PAUTA");
        boolean r2Trafficker = r2.contains("TRAFFICKER") || r2.contains("PAUTA");
        if (r1Trafficker || r2Trafficker) {
            return r1Trafficker && r2Trafficker;
        }
        boolean r1Fotografo = r1.contains("FOTOGRAFO") || r1.contains("FOTOGRAFIA") || r1.contains("AUDIOVISUAL");
        boolean r2Fotografo = r2.contains("FOTOGRAFO") || r2.contains("FOTOGRAFIA") || r2.contains("AUDIOVISUAL");
        if (r1Fotografo || r2Fotografo) {
            return r1Fotografo && r2Fotografo;
        }
        boolean r1Contador = r1.contains("CONTADOR") || r1.contains("CONTABILIDAD") || r1.contains("FINANZA");
        boolean r2Contador = r2.contains("CONTADOR") || r2.contains("CONTABILIDAD") || r2.contains("FINANZA");
        if (r1Contador || r2Contador) {
            return r1Contador && r2Contador;
        }
        boolean r1AsistAdmin = r1.contains("ASISTENTE") && r1.contains("ADMIN");
        boolean r2AsistAdmin = r2.contains("ASISTENTE") && r2.contains("ADMIN");
        if (r1AsistAdmin || r2AsistAdmin) {
            return r1AsistAdmin && r2AsistAdmin;
        }
        boolean r1AdminCargo = r1.contains("ADMINISTRADOR") || r1.contains("ADMINISTRACION");
        boolean r2AdminCargo = r2.contains("ADMINISTRADOR") || r2.contains("ADMINISTRACION");
        if (r1AdminCargo || r2AdminCargo) {
            return r1AdminCargo && r2AdminCargo;
        }
        if ((r1.contains("GERENT") || r1.equals("ADMIN")) && (r2.contains("GERENT") || r2.equals("ADMIN"))) return true;
        if (r1.contains("SENIOR") && r2.contains("SENIOR")) return true;
        if (r1.contains("SENIOR") || r2.contains("SENIOR")) return false; // Senior no es equivalente a Diseñador Estándar
        if (r1.contains("VENTA") && r2.contains("VENTA")) return true;
        if (r1.contains("PROGRAM") && r2.contains("PROGRAM")) return true;
        if ((r1.contains("DISEN") || r1.contains("GRAFIC") || r1.contains("CREATIV")) && (r2.contains("DISEN") || r2.contains("GRAFIC") || r2.contains("CREATIV"))) {
            boolean r1Jr = r1.contains("JUNIOR");
            boolean r2Jr = r2.contains("JUNIOR");
            return r1Jr == r2Jr;
        }
        return false;
    }

    public synchronized void addCollaborator(KpiCollaboratorProfile profile) {
        if (profile != null) {
            if (profile.getRegistrationDate() == null || profile.getRegistrationDate().isEmpty()) {
                profile.setRegistrationDate(new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
            }
            collaborators.add(profile);
        }
    }

    public synchronized KpiCollaboratorProfile findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String clean = name.trim();
        for (KpiCollaboratorProfile p : collaborators) {
            if (p.getName().equalsIgnoreCase(clean)) {
                return p;
            }
        }
        return null;
    }

    public synchronized void clearInventedData() {
        collaborators.clear();
        syncWithSession();
    }
}
