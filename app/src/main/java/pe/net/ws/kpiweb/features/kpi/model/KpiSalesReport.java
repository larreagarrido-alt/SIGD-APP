package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo para el reporte diario de un Asesor de Ventas.
 */
public class KpiSalesReport implements Serializable {
    private String id;
    private String fecha;
    private String asesor;
    private String supervisor;
    private String turno;
    private String rol;

    private List<KpiProductionGoal> metas;
    private List<KpiSalesActivityItem> gestiones;

    // Resumen comercial
    private int gestionesAsignadas;
    private int gestionesConcretadas;
    private int gestionesPendientes;
    private int visitasCampoJustificadas;
    private int prospectosContactados;
    private int cotizacionesEmitidas;
    private int ventasCerradas;
    private double totalCotizado;
    private double totalVendido;
    private int cumplimientoMeta;

    // Salida a campo / Visitas a clientes
    private boolean esSalidaCampo;
    private String motivoSalidaCampo;

    // Criterios y dificultades
    private Map<String, Boolean> criteriosEvaluacion;
    private boolean huboDificultad;
    private String detalleDificultad;

    // Observaciones y firmas
    private String observacionSupervisor;
    private String firmaAsesor;
    private String firmaSupervisor;

    private String nivelProductividad;
    private int puntaje;

    public KpiSalesReport() {
        this.metas = new ArrayList<>();
        this.gestiones = new ArrayList<>();
        this.criteriosEvaluacion = new LinkedHashMap<>();
    }

    public void recalcularResumen() {
        this.gestionesAsignadas = gestiones.size();
        this.gestionesConcretadas = 0;
        this.gestionesPendientes = 0;
        this.visitasCampoJustificadas = 0;
        this.prospectosContactados = 0;
        this.cotizacionesEmitidas = 0;
        this.ventasCerradas = 0;
        this.totalCotizado = 0.0;
        this.totalVendido = 0.0;

        for (KpiSalesActivityItem item : gestiones) {
            if (item.isSalidaCampo()) {
                this.visitasCampoJustificadas++;
            } else if (item.isConcretado()) {
                this.gestionesConcretadas++;
            } else {
                this.gestionesPendientes++;
            }

            String tipo = item.getTipoGestion();
            if ("Prospección".equalsIgnoreCase(tipo)) {
                this.prospectosContactados++;
            } else if ("Cotización".equalsIgnoreCase(tipo)) {
                this.cotizacionesEmitidas++;
                this.totalCotizado += item.getMonto();
            } else if ("Cierre de venta".equalsIgnoreCase(tipo)) {
                this.ventasCerradas++;
                this.totalVendido += item.getMonto();
            }
        }

        // Si se activó jornada completa en terreno
        if (this.esSalidaCampo) {
            this.cumplimientoMeta = 100;
            this.nivelProductividad = "EXCELENTE";
            this.puntaje = 20;
            return;
        }

        if (this.gestionesAsignadas > 0) {
            int efectivas = this.gestionesConcretadas + this.visitasCampoJustificadas;
            this.cumplimientoMeta = (int) Math.round(((double) efectivas / this.gestionesAsignadas) * 100);
        } else {
            this.cumplimientoMeta = 100;
        }

        if (cumplimientoMeta >= 90) {
            this.nivelProductividad = "EXCELENTE";
            this.puntaje = 20;
        } else if (cumplimientoMeta >= 80) {
            this.nivelProductividad = "MUY BUENO";
            this.puntaje = 17;
        } else if (cumplimientoMeta >= 60) {
            this.nivelProductividad = "BUENO";
            this.puntaje = 14;
        } else if (cumplimientoMeta >= 40) {
            this.nivelProductividad = "REGULAR";
            this.puntaje = 12;
        } else {
            this.nivelProductividad = "DEFICIENTE";
            this.puntaje = 10;
        }
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getAsesor() { return asesor; }
    public void setAsesor(String asesor) { this.asesor = asesor; }

    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public List<KpiProductionGoal> getMetas() { return metas; }
    public void setMetas(List<KpiProductionGoal> metas) { this.metas = metas; }

    public List<KpiSalesActivityItem> getGestiones() { return gestiones; }
    public void setGestiones(List<KpiSalesActivityItem> gestiones) {
        this.gestiones = gestiones;
        recalcularResumen();
    }

    public int getGestionesAsignadas() { return gestionesAsignadas; }
    public int getGestionesConcretadas() { return gestionesConcretadas; }
    public int getGestionesPendientes() { return gestionesPendientes; }
    public int getVisitasCampoJustificadas() { return visitasCampoJustificadas; }
    public int getProspectosContactados() { return prospectosContactados; }
    public int getCotizacionesEmitidas() { return cotizacionesEmitidas; }
    public int getVentasCerradas() { return ventasCerradas; }
    public double getTotalCotizado() { return totalCotizado; }
    public double getTotalVendido() { return totalVendido; }
    public int getCumplimientoMeta() { return cumplimientoMeta; }

    public boolean isEsSalidaCampo() { return esSalidaCampo; }
    public void setEsSalidaCampo(boolean esSalidaCampo) { this.esSalidaCampo = esSalidaCampo; }

    public String getMotivoSalidaCampo() { return motivoSalidaCampo; }
    public void setMotivoSalidaCampo(String motivoSalidaCampo) { this.motivoSalidaCampo = motivoSalidaCampo; }

    public Map<String, Boolean> getCriteriosEvaluacion() { return criteriosEvaluacion; }
    public void setCriteriosEvaluacion(Map<String, Boolean> criteriosEvaluacion) { this.criteriosEvaluacion = criteriosEvaluacion; }

    public boolean isHuboDificultad() { return huboDificultad; }
    public void setHuboDificultad(boolean huboDificultad) { this.huboDificultad = huboDificultad; }

    public String getDetalleDificultad() { return detalleDificultad; }
    public void setDetalleDificultad(String detalleDificultad) { this.detalleDificultad = detalleDificultad; }

    public String getObservacionSupervisor() { return observacionSupervisor; }
    public void setObservacionSupervisor(String observacionSupervisor) { this.observacionSupervisor = observacionSupervisor; }

    public String getFirmaAsesor() { return firmaAsesor; }
    public void setFirmaAsesor(String firmaAsesor) { this.firmaAsesor = firmaAsesor; }

    public String getFirmaSupervisor() { return firmaSupervisor; }
    public void setFirmaSupervisor(String firmaSupervisor) { this.firmaSupervisor = firmaSupervisor; }

    public String getNivelProductividad() { return nivelProductividad; }
    public void setNivelProductividad(String nivelProductividad) { this.nivelProductividad = nivelProductividad; }

    public int getPuntaje() { return puntaje; }
    public void setPuntaje(int puntaje) { this.puntaje = puntaje; }
}
