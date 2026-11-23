package pe.net.ws.kpiweb.features.account.data;

import android.os.Handler;
import android.os.Looper;

import pe.net.ws.kpiweb.features.account.model.UserProfile;
import pe.net.ws.kpiweb.features.account.model.UserRole;

/**
 * Repositorio de prueba para el módulo Mi cuenta.
 * Permite la gestión local de datos de perfil para Administrador y Colaborador
 * con separación explícita de campos locales frente a la futura API segura del servidor.
 */
public class AccountRepository {

    private static AccountRepository instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private UserProfile adminProfile;
    private UserProfile collaboratorProfile;

    public interface UpdateProfileCallback {
        void onSuccess(UserProfile updatedProfile, String message);
        void onError(String errorMessage);
    }

    public interface ChangePasswordCallback {
        void onPendingIntegration(String message);
        void onError(String errorMessage);
    }

    private AccountRepository() {
        initMockProfiles();
    }

    public static synchronized AccountRepository getInstance() {
        if (instance == null) {
            instance = new AccountRepository();
        }
        return instance;
    }

    private void initMockProfiles() {
        // Perfil Administrador por defecto
        adminProfile = new UserProfile(
                "EMP-ADM-001",
                "Ana Martínez",
                "ana.martinez@website.com",
                "987654321",
                "Directora de Operaciones",
                "Operaciones y Talento",
                UserRole.ADMINISTRADOR,
                "10 de enero de 2023",
                "AM",
                "Activo"
        );

        // Perfil Colaborador por defecto
        collaboratorProfile = new UserProfile(
                "EMP-COL-042",
                "Lucía Gómez",
                "lucia.gomez@website.com",
                "912345678",
                "Product Designer • UI/UX",
                "Diseño y Producto",
                UserRole.COLABORADOR,
                "15 de marzo de 2024",
                "LG",
                "Activo"
        );
    }

    public UserProfile getProfile(UserRole role) {
        if (role == UserRole.ADMINISTRADOR) {
            return adminProfile;
        }
        return collaboratorProfile;
    }

    /**
     * Sincroniza la cuenta activa del colaborador cuando se cambia entre los perfiles de demostración
     * (Lucía Gómez, Carlos Mendoza o Elena Rojas).
     */
    public void syncWithCollaboratorProfile(pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile activeCollab) {
        if (activeCollab == null) {
            return;
        }
        String id = activeCollab.getId();
        if (id.matches("[0-9]+")) {
            collaboratorProfile = new UserProfile(id, activeCollab.getFullName(), "", "",
                    activeCollab.getJobTitle(), activeCollab.getDepartment(), UserRole.COLABORADOR,
                    "", activeCollab.getAvatarInitials(), "Activo");
            return;
        }
        String empCode = "EMP-COL-042";
        String email = "lucia.gomez@website.com";
        String phone = "912345678";
        String hireDate = "15 de marzo de 2024";

        if ("collab_02".equals(id)) {
            empCode = "EMP-COL-087";
            email = "carlos.mendoza@website.com";
            phone = "945678123";
            hireDate = "02 de agosto de 2023";
        } else if ("collab_03".equals(id)) {
            empCode = "EMP-COL-114";
            email = "elena.rojas@website.com";
            phone = "978123456";
            hireDate = "20 de enero de 2025";
        }

        collaboratorProfile = new UserProfile(
                empCode,
                activeCollab.getFullName(),
                email,
                phone,
                activeCollab.getJobTitle(),
                activeCollab.getDepartment(),
                UserRole.COLABORADOR,
                hireDate,
                activeCollab.getAvatarInitials(),
                "Activo"
        );
    }

    /**
     * Actualiza localmente los campos permitidos (nombre y teléfono).
     * Simula llamada asíncrona con manejo de carga y confirmación explícita.
     */
    public void updateProfile(UserRole role, String newFullName, String newPhone, UpdateProfileCallback callback) {
        // Simular latencia de red de 600ms
        mainHandler.postDelayed(() -> {
            try {
                UserProfile profile = getProfile(role);
                profile.setFullName(newFullName);
                profile.setPhone(newPhone);
                String msg = "Información actualizada correctamente.";
                callback.onSuccess(profile, msg);
            } catch (Exception e) {
                callback.onError("Error al actualizar la información: " + e.getMessage());
            }
        }, 600);
    }

    /**
     * Solicita cambio de contraseña.
     * En conformidad con las directivas de seguridad, NO almacena contraseñas en memoria ni SharedPreferences
     * e informa que el servicio seguro del backend del integrador está pendiente.
     */
    public void requestPasswordChange(String currentPassword, String newPassword, ChangePasswordCallback callback) {
        mainHandler.postDelayed(() -> {
            // No guardamos la contraseña en ningún medio persistente o volátil
            callback.onPendingIntegration("Validación y cambio de contraseña pendientes de integración con el servicio seguro del servidor.");
        }, 700);
    }
}
