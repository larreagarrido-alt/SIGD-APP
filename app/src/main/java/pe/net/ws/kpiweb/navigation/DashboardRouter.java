package pe.net.ws.kpiweb.navigation;

import android.app.Activity;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.text.Normalizer;
import java.util.Locale;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminDashboardActivity;
import pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity;

/** Integration point only. Permissions come from a fresh /auth/me, never intent extras or cached roles. */
public final class DashboardRouter {
    private DashboardRouter() {}

    public static List<Destination> destinations() {
        return java.util.Arrays.asList(
            new Destination(AdminDashboardActivity.class, "dashboard.admin", Collections.emptySet()),
            new Destination(CollaboratorDashboardActivity.class, "kpi.ver", Collections.emptySet())
        );
    }

    public static Destination resolve(LoginResponseData profile) {return resolve(profile,destinations());}

    public static Destination resolve(LoginResponseData profile,List<Destination> registered) {
        if(profile==null||profile.getUser()==null||profile.getUser().getIdUsuario()<=0
                ||profile.getPermissions()==null)return null;
        for(Destination destination:registered)if(destination.accepts(profile))return destination;
        return null;
    }

    /** A verified active worker may open the personal home even before KPI grants exist.
     * Every sensitive action remains authorized by its own backend endpoint. */
    public static Destination resolveHome(LoginResponseData profile) {
        if (profile != null && profile.getWorker() != null
                && !"Activo".equalsIgnoreCase(profile.getWorker().getEstado())) return null;
        Destination privileged = resolve(profile);
        if (privileged != null && privileged.activity() == AdminDashboardActivity.class) return privileged;
        if (requiresAdministrativeGrant(profile)) return null;
        if (privileged != null) return privileged;
        if (profile == null || profile.getUser() == null || profile.getUser().getIdUsuario() <= 0
                || profile.getPermissions() == null || profile.getWorker() == null
                || profile.getWorker().getIdTrabajador() <= 0
                || !"Activo".equalsIgnoreCase(profile.getWorker().getEstado())) return null;
        return new Destination(CollaboratorDashboardActivity.class, "auth.perfil", Collections.emptySet());
    }

    /** Classifies a missing grant for the UI only; never grants access by role name or ID. */
    public static boolean requiresAdministrativeGrant(LoginResponseData profile) {
        if (profile == null || profile.getUser() == null || profile.getPermissions() == null
                || profile.getPermissions().contains("dashboard.admin")) return false;
        String role = profile.getUser().getRolNombre();
        if (role != null) {
            String normalized = Normalizer.normalize(role.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
            if (normalized.equals("ADMINISTRADOR") || normalized.equals("GERENCIA")
                    || normalized.equals("JEFE DE AGENCIA") || normalized.equals("SUPERVISOR")) return true;
        }
        for (String code : new String[]{"kpi.administrar", "kpi.revisar", "empleados.administrar",
                "actividad.revisar", "bienestar.ver_agregado", "premios.administrar"}) {
            if (profile.getPermissions().contains(code)) return true;
        }
        return false;
    }

    public static final class Destination {
        private final Class<? extends Activity> activity;
        private final String permission;
        private final Set<Integer> roleIds;
        public Destination(Class<? extends Activity> activity,String permission,Set<Integer> roleIds) {
            if(activity==null||permission==null||!permission.matches("[a-z][a-z0-9_.]+")||roleIds==null)
                throw new IllegalArgumentException("A real activity and backend permission are required");
            if(pe.net.ws.kpiweb.MainActivity.class.isAssignableFrom(activity)||pe.net.ws.kpiweb.AccountActivity.class.isAssignableFrom(activity)
                    ||pe.net.ws.kpiweb.features.auth.LoginActivity.class.isAssignableFrom(activity)
                    ||pe.net.ws.kpiweb.features.auth.FaceEnrollmentActivity.class.isAssignableFrom(activity))
                throw new IllegalArgumentException("Authentication/account activities are not dashboards");
            for(Integer id:roleIds)if(id==null||id<=0)throw new IllegalArgumentException("Invalid backend role ID");
            this.activity=activity;this.permission=permission;
            this.roleIds=Collections.unmodifiableSet(new HashSet<>(roleIds));
        }
        public Class<? extends Activity> activity(){return activity;}
        private boolean accepts(LoginResponseData profile){
            return profile.getPermissions().contains(permission)
                &&(roleIds.isEmpty()||roleIds.contains(profile.getUser().getIdRol()));
        }
    }
}
