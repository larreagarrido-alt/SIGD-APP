package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;

/**
 * Modelo de datos de un Colaborador o Practicante adaptable a base de datos.
 * Permite que cada cargo tenga múltiples personas asociadas.
 */
public class KpiCollaboratorProfile implements Serializable {

    private String id;
    private String name;
    private String role;
    private String area;
    private String supervisor;
    private String immediateBoss;
    private String shift;
    private boolean isPracticante;
    private String registrationDate;

    public KpiCollaboratorProfile() {
        this.registrationDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    public KpiCollaboratorProfile(String id, String name, String role, String area,
                                  String supervisor, String immediateBoss, String shift,
                                  boolean isPracticante) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.area = area;
        this.supervisor = supervisor;
        this.immediateBoss = immediateBoss;
        this.shift = shift;
        this.isPracticante = isPracticante;
        this.registrationDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    public String getRegistrationDate() {
        return registrationDate != null ? registrationDate : "";
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getId() {
        return id != null ? id : "";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role != null ? role : "";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getArea() {
        return area != null ? area : "";
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getSupervisor() {
        return supervisor != null ? supervisor : "";
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getImmediateBoss() {
        return immediateBoss != null ? immediateBoss : "";
    }

    public void setImmediateBoss(String immediateBoss) {
        this.immediateBoss = immediateBoss;
    }

    public String getShift() {
        return shift != null ? shift : "";
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public boolean isPracticante() {
        return isPracticante;
    }

    public void setPracticante(boolean practicante) {
        isPracticante = practicante;
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}

