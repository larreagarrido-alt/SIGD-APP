package pe.net.ws.kpiweb.features.account.model;

import java.io.Serializable;

/**
 * Modelo completo del perfil de usuario (Administrador o Colaborador).
 * Los campos del servidor (ID, Rol, Departamento) son de solo lectura.
 * Los campos editables autorizados (Nombre para mostrar / Teléfono) se gestionan con validación.
 */
public class UserProfile implements Serializable {

    private final String employeeId;
    private String fullName;
    private String email;
    private String phone;
    private final String jobTitle;
    private final String department;
    private final UserRole role;
    private final String hireDate;
    private String avatarInitials;
    private final String accountStatus;

    public UserProfile(String employeeId, String fullName, String email, String phone,
                       String jobTitle, String department, UserRole role,
                       String hireDate, String avatarInitials, String accountStatus) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.jobTitle = jobTitle;
        this.department = department;
        this.role = role;
        this.hireDate = hireDate;
        this.avatarInitials = avatarInitials;
        this.accountStatus = accountStatus;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        updateInitials();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public UserRole getRole() {
        return role;
    }

    public String getHireDate() {
        return hireDate != null && !hireDate.trim().isEmpty() ? hireDate : "Fecha de ingreso no registrada";
    }

    public String getHireDateRaw() {
        return hireDate;
    }

    public String getAvatarInitials() {
        return avatarInitials;
    }

    public String getAccountStatus() {
        return accountStatus != null ? accountStatus : "Activo";
    }

    private void updateInitials() {
        if (fullName == null || fullName.trim().isEmpty()) {
            this.avatarInitials = "WS";
            return;
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            this.avatarInitials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            this.avatarInitials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
    }
}
