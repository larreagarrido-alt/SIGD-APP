package pe.net.ws.kpiweb.features.account.validation;

import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * Validador de lógica de negocio para los campos del perfil y cambio de contraseña.
 */
public class AccountValidator {

    // Formato de teléfono para Perú / LATAM: 9 dígitos, opcionalmente con prefijo +51 o espacios/guiones
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+?51[- ]?)?9\\d{8}$");

    /**
     * Valida el nombre completo.
     */
    public static ValidationResult validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return ValidationResult.error("El nombre completo no puede estar vacío");
        }
        String trimmed = fullName.trim();
        if (trimmed.length() < 3) {
            return ValidationResult.error("El nombre debe tener al menos 3 caracteres");
        }
        if (!trimmed.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
            return ValidationResult.error("El nombre solo debe contener letras");
        }
        return ValidationResult.success();
    }

    /**
     * Valida el formato del teléfono.
     */
    public static ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.error("El teléfono es requerido");
        }
        String cleanPhone = phone.replaceAll("[\\s-]", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches() && !cleanPhone.matches("^9\\d{8}$")) {
            return ValidationResult.error("Ingresa un número móvil válido de 9 dígitos (inicia con 9)");
        }
        return ValidationResult.success();
    }

    /**
     * Valida el correo electrónico.
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.error("El correo no puede estar vacío");
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return ValidationResult.error("Ingresa un correo electrónico corporativo válido");
        }
        return ValidationResult.success();
    }

    /**
     * Valida la contraseña actual.
     */
    public static ValidationResult validateCurrentPassword(String currentPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return ValidationResult.error("Debes ingresar tu contraseña actual");
        }
        return ValidationResult.success();
    }

    /**
     * Valida la nueva contraseña según requisitos de seguridad.
     */
    public static ValidationResult validateNewPassword(String newPassword, String currentPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            return ValidationResult.error("La nueva contraseña no puede estar vacía");
        }
        if (newPassword.length() < 8) {
            return ValidationResult.error("La nueva contraseña debe tener al menos 8 caracteres");
        }
        boolean hasDigit = false;
        boolean hasLetter = false;
        for (char c : newPassword.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            if (Character.isLetter(c)) hasLetter = true;
        }
        if (!hasDigit || !hasLetter) {
            return ValidationResult.error("Debe contener al menos una letra y un número");
        }
        if (currentPassword != null && newPassword.equals(currentPassword)) {
            return ValidationResult.error("La nueva contraseña debe ser diferente a la actual");
        }
        return ValidationResult.success();
    }

    /**
     * Comprueba la coincidencia de confirmación de contraseña.
     */
    public static ValidationResult validatePasswordConfirmation(String newPassword, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) {
            return ValidationResult.error("Por favor confirma tu nueva contraseña");
        }
        if (!confirmation.equals(newPassword)) {
            return ValidationResult.error("Las contraseñas no coinciden");
        }
        return ValidationResult.success();
    }
}
