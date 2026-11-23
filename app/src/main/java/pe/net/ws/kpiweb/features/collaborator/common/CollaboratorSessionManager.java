package pe.net.ws.kpiweb.features.collaborator.common;

import android.content.Context;
import android.content.SharedPreferences;

import pe.net.ws.kpiweb.features.account.data.AccountRepository;
import pe.net.ws.kpiweb.features.collaborator.dashboard.data.CollaboratorMockDataSource;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorPerformanceSummary;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorPersonalStats;
import pe.net.ws.kpiweb.core.session.SessionManager;
import java.util.Collections;

/**
 * Gestor de sesión activa del Colaborador.
 * Persiste el perfil seleccionado (Lucía Gómez, Carlos Mendoza o Elena Rojas)
 * para mantener coherencia dinámica entre Dashboard, Mis KPI, Notificaciones,
 * Menú y Mi Cuenta, y limpia el estado al cerrar sesión.
 */
public final class CollaboratorSessionManager {

    private static final String PREFS_NAME = "website_collaborator_session";
    private static final String KEY_ACTIVE_PROFILE_INDEX = "active_profile_index";

    private static CollaboratorSessionManager instance;
    private final SharedPreferences prefs;
    private final CollaboratorMockDataSource dataSource;
    private final Context appContext;

    private CollaboratorSessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        // Retira las conversaciones locales de la función eliminada en instalaciones anteriores.
        this.appContext.getSharedPreferences("website_collab_ai_threads_prefs_v3", Context.MODE_PRIVATE)
                .edit().clear().apply();
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dataSource = new CollaboratorMockDataSource();
    }

    public static synchronized CollaboratorSessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new CollaboratorSessionManager(context);
        }
        return instance;
    }

    public CollaboratorMockDataSource.DevProfileIndex getActiveProfileIndex() {
        String saved = prefs.getString(KEY_ACTIVE_PROFILE_INDEX, CollaboratorMockDataSource.DevProfileIndex.DISENO.name());
        try {
            return CollaboratorMockDataSource.DevProfileIndex.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return CollaboratorMockDataSource.DevProfileIndex.DISENO;
        }
    }

    public void setActiveProfileIndex(CollaboratorMockDataSource.DevProfileIndex index) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_INDEX, index.name()).apply();
        CollaboratorProfile profile = dataSource.getProfile(index);
        AccountRepository.getInstance().syncWithCollaboratorProfile(profile);
    }

    public CollaboratorProfile cycleNextProfile() {
        CollaboratorMockDataSource.DevProfileIndex current = getActiveProfileIndex();
        CollaboratorMockDataSource.DevProfileIndex next;
        switch (current) {
            case DISENO:
                next = CollaboratorMockDataSource.DevProfileIndex.VENTAS;
                break;
            case VENTAS:
                next = CollaboratorMockDataSource.DevProfileIndex.ATENCION_CLIENTE;
                break;
            default:
                next = CollaboratorMockDataSource.DevProfileIndex.DISENO;
                break;
        }
        setActiveProfileIndex(next);
        return dataSource.getProfile(next);
    }

    public CollaboratorProfile getActiveProfile() {
        SessionManager session = new SessionManager(appContext);
        // Una cuenta autenticada sin trabajador vinculado tampoco debe mostrar perfiles ficticios.
        if (session.isLoggedIn()) {
            String fullName = session.getFullName().trim();
            if (fullName.isEmpty()) fullName = session.getUsername();
            String[] parts = fullName.split("\\s+");
            String firstName = parts.length == 0 ? fullName : parts[0];
            String initials = firstName.isEmpty() ? "?" : firstName.substring(0, 1).toUpperCase();
            if (parts.length > 1) initials += parts[parts.length - 1].substring(0, 1).toUpperCase();
            Integer points = session.getPointsBalance();
            CollaboratorProfile profile = new CollaboratorProfile(
                    String.valueOf(session.getWorkerId()), firstName, fullName,
                    session.getCargoName(), session.getAgenciaName(), initials,
                    new CollaboratorPerformanceSummary("", "Tu información laboral", "Indicadores por consultar", null,
                            "Aún no se han cargado evaluaciones reales"),
                    new CollaboratorPersonalStats(points == null ? "—" : String.valueOf(points),
                            points == null ? "Sin datos" : "Saldo confirmado", "—", "Sin datos", "—", "Sin datos", 0,
                            "—", "Sin datos"), null, Collections.emptyList(), Collections.emptyList());
            AccountRepository.getInstance().syncWithCollaboratorProfile(profile);
            return profile;
        }
        CollaboratorMockDataSource.DevProfileIndex index = getActiveProfileIndex();
        CollaboratorProfile profile = dataSource.getProfile(index);
        AccountRepository.getInstance().syncWithCollaboratorProfile(profile);
        return profile;
    }

    /**
     * Limpia el estado de sesión activa del colaborador al cerrar sesión.
     */
    public void clearSession() {
        prefs.edit().clear().apply();
        CollaboratorProfile defaultProfile = dataSource.getProfile(CollaboratorMockDataSource.DevProfileIndex.DISENO);
        AccountRepository.getInstance().syncWithCollaboratorProfile(defaultProfile);
    }
}
