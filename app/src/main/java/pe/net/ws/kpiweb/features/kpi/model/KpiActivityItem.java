package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;

/**
 * Modelo para representar una actividad individual dentro del registro diario de producción.
 */
public class KpiActivityItem implements Serializable {
    private String id;
    private String numero;
    private String proyecto;
    private String tipo;
    private String descripcion;
    private String horaInicio;
    private String horaFin;
    private String estado; // "Entregado" o "Pendiente"
    private int bugs;
    private String commitHash;

    public KpiActivityItem() {
        this.estado = "Entregado";
        this.bugs = 0;
    }

    public KpiActivityItem(String id, String numero, String proyecto, String tipo,
                           String descripcion, String horaInicio, String horaFin,
                           String estado, int bugs, String commitHash) {
        this.id = id;
        this.numero = numero;
        this.proyecto = proyecto;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = estado;
        this.bugs = bugs;
        this.commitHash = commitHash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getProyecto() {
        return proyecto;
    }

    public void setProyecto(String proyecto) {
        this.proyecto = proyecto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public int getBugs() {
        return bugs;
    }

    public void setBugs(int bugs) {
        this.bugs = bugs;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public boolean isEntregado() {
        return "Entregado".equalsIgnoreCase(estado);
    }

    public boolean isPendiente() {
        return "Pendiente".equalsIgnoreCase(estado);
    }

    public boolean isSalidaCampo() {
        return "Salida a campo".equalsIgnoreCase(estado) 
                || "Justificado".equalsIgnoreCase(estado)
                || (estado != null && estado.toLowerCase().contains("campo"));
    }
}
