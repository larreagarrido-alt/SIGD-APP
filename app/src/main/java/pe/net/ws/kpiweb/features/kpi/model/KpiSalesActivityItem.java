package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;

/**
 * Modelo para representar una gestión individual de un Asesor de Ventas.
 */
public class KpiSalesActivityItem implements Serializable {
    private String id;
    private String numero;
    private String cliente;
    private String tipoGestion; // "Prospección", "Seguimiento", "Demo / Cita", "Cotización", "Cierre de venta", "Visita en terreno"
    private String detalle;
    private String horaInicio;
    private String horaFin;
    private String estado; // "Concretado", "En seguimiento", "Salida a campo"
    private double monto;

    public KpiSalesActivityItem() {
        this.estado = "Concretado";
        this.monto = 0.0;
    }

    public KpiSalesActivityItem(String id, String numero, String cliente, String tipoGestion,
                               String detalle, String horaInicio, String horaFin,
                               String estado, double monto) {
        this.id = id;
        this.numero = numero;
        this.cliente = cliente;
        this.tipoGestion = tipoGestion;
        this.detalle = detalle;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estado = estado;
        this.monto = monto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getTipoGestion() { return tipoGestion; }
    public void setTipoGestion(String tipoGestion) { this.tipoGestion = tipoGestion; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public boolean isConcretado() {
        return "Concretado".equalsIgnoreCase(estado) || "Cerrado".equalsIgnoreCase(estado);
    }

    public boolean isSalidaCampo() {
        return "Salida a campo".equalsIgnoreCase(estado)
                || "Visita en terreno".equalsIgnoreCase(estado)
                || (estado != null && estado.toLowerCase().contains("campo"));
    }
}
