package pe.net.ws.kpiweb.navigation;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import pe.net.ws.kpiweb.AccountActivity;
import pe.net.ws.kpiweb.MainActivity;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;

/** Fixture activities describe routing only; they are not application dashboards. */
public class DashboardRouterTest {
    private static class RealModule extends Activity {}
    private static class OtherModule extends Activity {}
    private static class AccountSubclass extends AccountActivity {}

    @Test public void mergedModulesRequireBackendPermissions() {
        assertTrue(DashboardRouter.destinations().size() == 2);
        assertNull(DashboardRouter.resolve(profile(7, 2, "[\"auth.perfil\"]")));
        assertSame(DashboardRouter.destinations().get(0).activity(),
            DashboardRouter.resolve(profile(7, 2, "[\"dashboard.admin\"]")).activity());
        assertSame(DashboardRouter.destinations().get(1).activity(),
            DashboardRouter.resolve(profile(7, 2, "[\"kpi.ver\"]")).activity());
    }

    @Test public void activeWorkerWithoutKpiAssignmentOpensPersonalHome() {
        LoginResponseData worker = new Gson().fromJson("{\"user\":{\"id_usuario\":7,\"id_rol\":5},"
            + "\"worker\":{\"id_trabajador\":12,\"estado\":\"Activo\",\"id_cargo\":4,"
            + "\"cargo_nombre\":\"Programador Web\"},\"permissions\":[]}", LoginResponseData.class);
        assertSame(pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity.class,
            DashboardRouter.resolveHome(worker).activity());
        assertTrue(worker.getWorker().getIdCargo() == 4);
    }

    @Test public void personalHomeNeverReplacesAuthorizedAdminOrAdmitsInactiveWorkers() {
        LoginResponseData admin = new Gson().fromJson("{\"user\":{\"id_usuario\":7,\"id_rol\":1},"
            + "\"worker\":{\"id_trabajador\":12,\"estado\":\"Activo\"},"
            + "\"permissions\":[\"dashboard.admin\"]}", LoginResponseData.class);
        assertSame(pe.net.ws.kpiweb.features.admin.dashboard.AdminDashboardActivity.class,
            DashboardRouter.resolveHome(admin).activity());
        LoginResponseData inactive = new Gson().fromJson("{\"user\":{\"id_usuario\":7},"
            + "\"worker\":{\"id_trabajador\":12,\"estado\":\"Inactivo\"},\"permissions\":[]}",
            LoginResponseData.class);
        assertNull(DashboardRouter.resolveHome(inactive));
        assertNull(DashboardRouter.resolveHome(profile(7, 5, "[]")));
    }

    @Test public void allSixSyntheticRolesRouteByEffectivePermissionNotRoleNumber() {
        for (int role = 1; role <= 6; role++) {
            String adminGrant = role % 2 == 0 ? "[\"dashboard.admin\"]" : "[]";
            LoginResponseData profile = new Gson().fromJson("{\"user\":{\"id_usuario\":" + role
                + ",\"id_rol\":" + role + "},\"worker\":{\"id_trabajador\":" + role
                + ",\"estado\":\"Activo\"},\"permissions\":" + adminGrant + "}", LoginResponseData.class);
            assertSame(role % 2 == 0
                    ? pe.net.ws.kpiweb.features.admin.dashboard.AdminDashboardActivity.class
                    : pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity.class,
                DashboardRouter.resolveHome(profile).activity());
        }
    }

    @Test public void twelveRealCargoIdsDoNotGrantAdministration() {
        int[] cargos = {1, 2, 3, 4, 5, 6, 7, 13, 14, 15, 16, 17};
        for (int cargo : cargos) {
            LoginResponseData profile = new Gson().fromJson("{\"user\":{\"id_usuario\":7,\"id_rol\":5},"
                + "\"worker\":{\"id_trabajador\":12,\"id_cargo\":" + cargo
                + ",\"estado\":\"Activo\"},\"permissions\":[]}", LoginResponseData.class);
            assertSame(pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity.class,
                DashboardRouter.resolveHome(profile).activity());
        }
    }

    @Test public void managementRoleWithoutDashboardGrantNeverFallsIntoCollaboratorHome() {
        for (String role : new String[]{"Administrador", "Gerencia", "Jefe de Agencia", "Supervisor"}) {
            LoginResponseData missing = new Gson().fromJson("{\"user\":{\"id_usuario\":7,"
                + "\"rol_nombre\":\"" + role + "\"},\"worker\":{\"id_trabajador\":12,"
                + "\"estado\":\"Activo\"},\"permissions\":[]}", LoginResponseData.class);
            assertTrue(DashboardRouter.requiresAdministrativeGrant(missing));
            assertNull(DashboardRouter.resolveHome(missing));
            LoginResponseData granted = new Gson().fromJson("{\"user\":{\"id_usuario\":7,"
                + "\"rol_nombre\":\"" + role + "\"},\"worker\":{\"id_trabajador\":12,"
                + "\"estado\":\"Activo\"},\"permissions\":[\"dashboard.admin\"]}",
                LoginResponseData.class);
            assertSame(pe.net.ws.kpiweb.features.admin.dashboard.AdminDashboardActivity.class,
                DashboardRouter.resolveHome(granted).activity());
        }
    }

    @Test public void effectiveManagementPermissionAlsoBlocksPersonalFallback() {
        LoginResponseData manager = new Gson().fromJson("{\"user\":{\"id_usuario\":7,"
            + "\"rol_nombre\":\"Colaborador\"},\"worker\":{\"id_trabajador\":12,"
            + "\"estado\":\"Activo\"},\"permissions\":[\"kpi.administrar\"]}",
            LoginResponseData.class);
        assertNull(DashboardRouter.resolveHome(manager));
        LoginResponseData managerWithKpi = new Gson().fromJson("{\"user\":{\"id_usuario\":7,"
            + "\"rol_nombre\":\"Administrador\"},\"worker\":{\"id_trabajador\":12,"
            + "\"estado\":\"Activo\"},\"permissions\":[\"kpi.ver\"]}",
            LoginResponseData.class);
        assertNull(DashboardRouter.resolveHome(managerWithKpi));
    }

    @Test public void selectsRegisteredDestinationWithEffectivePermission() {
        DashboardRouter.Destination admin = destination(RealModule.class, "dashboard.admin");
        assertSame(admin, DashboardRouter.resolve(profile(7, 2, "[\"dashboard.admin\"]"),
            Collections.singletonList(admin)));
    }

    @Test public void skipsUnauthorizedDestination() {
        DashboardRouter.Destination admin = destination(OtherModule.class, "dashboard.admin");
        DashboardRouter.Destination kpi = destination(RealModule.class, "kpi.ver");
        assertSame(kpi, DashboardRouter.resolve(profile(7, 2, "[\"kpi.ver\"]"), Arrays.asList(admin, kpi)));
        assertNull(DashboardRouter.resolve(profile(7, 2, "[\"auth.perfil\"]"), Arrays.asList(admin, kpi)));
    }

    @Test public void roleDoesNotReplacePermission() {
        DashboardRouter.Destination restricted = destination(RealModule.class, "dashboard.admin", 2);
        assertNull(DashboardRouter.resolve(profile(7, 2, "[\"auth.perfil\"]"),
            Collections.singletonList(restricted)));
    }

    @Test public void roleRestrictionUsesBackendProfile() {
        DashboardRouter.Destination restricted = destination(RealModule.class, "kpi.ver", 3);
        assertNull(DashboardRouter.resolve(profile(7, 2, "[\"kpi.ver\"]"),
            Collections.singletonList(restricted)));
        assertSame(restricted, DashboardRouter.resolve(profile(7, 3, "[\"kpi.ver\"]"),
            Collections.singletonList(restricted)));
    }

    @Test public void emptyPermissionsCannotRoute() {
        assertNull(DashboardRouter.resolve(profile(7, 2, "[]"),
            Collections.singletonList(destination(RealModule.class, "kpi.ver"))));
    }

    @Test public void missingPermissionsCannotRoute() {
        assertNull(DashboardRouter.resolve(profile(7, 2, null),
            Collections.singletonList(destination(RealModule.class, "kpi.ver"))));
    }

    @Test public void missingOrInvalidIdentityCannotRoute() {
        for (LoginResponseData invalid : new LoginResponseData[] {
                null, new LoginResponseData(), profile(0, 2, "[\"kpi.ver\"]"),
                profile(-1, 2, "[\"kpi.ver\"]")}) {
            assertNull(DashboardRouter.resolve(invalid,
                Collections.singletonList(destination(RealModule.class, "kpi.ver"))));
        }
    }

    @Test public void respectsRegisteredOrderForSameAndDifferentPermissions() {
        DashboardRouter.Destination first = destination(RealModule.class, "kpi.ver");
        DashboardRouter.Destination second = destination(OtherModule.class, "kpi.ver");
        assertSame(first, DashboardRouter.resolve(profile(7, 2, "[\"kpi.ver\"]"), Arrays.asList(first, second)));
        DashboardRouter.Destination admin = destination(OtherModule.class, "dashboard.admin");
        LoginResponseData both = profile(7, 2, "[\"kpi.ver\",\"dashboard.admin\"]");
        assertSame(admin, DashboardRouter.resolve(both, Arrays.asList(admin, first)));
        assertSame(first, DashboardRouter.resolve(both, Arrays.asList(first, admin)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void accountCannotBeRegisteredAsDashboard() {
        destination(AccountActivity.class, "auth.perfil");
    }

    @Test(expected = IllegalArgumentException.class)
    public void entryCannotBeRegisteredAsDashboard() {
        destination(MainActivity.class, "auth.perfil");
    }

    @Test(expected = IllegalArgumentException.class)
    public void accountSubclassCannotBeRegisteredAsDashboard() {
        destination(AccountSubclass.class, "auth.perfil");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPermissionCannotBeRegistered() {
        destination(RealModule.class, "");
    }

    private static DashboardRouter.Destination destination(Class<? extends Activity> activity,
            String permission, Integer... roles) {
        return new DashboardRouter.Destination(activity, permission, new HashSet<>(Arrays.asList(roles)));
    }

    private static LoginResponseData profile(int id, int role, String permissions) {
        return new Gson().fromJson("{\"user\":{\"id_usuario\":" + id + ",\"id_rol\":" + role
            + "},\"permissions\":" + (permissions == null ? "null" : permissions) + "}", LoginResponseData.class);
    }
}
