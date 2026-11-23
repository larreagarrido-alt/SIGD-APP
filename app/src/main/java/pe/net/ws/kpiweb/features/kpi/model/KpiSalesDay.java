package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;

/**
 * Modelo de datos para el registro comercial diario de un Asesor de Ventas.
 */
public class KpiSalesDay implements Serializable {

    private int dayIndex; // 0=LUN, 1=MAR, 2=MIÉ, 3=JUE, 4=VIE, 5=SÁB
    private String dayName;
    private String shortName;
    private int dayNumber;
    private String fullDateFormatted;
    private boolean saved;
    private String savedAt;
    private String observaciones;

    // Métricas del día
    private int llamadas;
    private int visitas;
    private int cotizaciones;
    private int seguimiento;
    private int prospectos;
    private int clientes;

    public KpiSalesDay(int dayIndex, String dayName, String shortName, int dayNumber, String fullDateFormatted) {
        this.dayIndex = dayIndex;
        this.dayName = dayName;
        this.shortName = shortName;
        this.dayNumber = dayNumber;
        this.fullDateFormatted = fullDateFormatted;
        this.saved = false;
        this.savedAt = "";
        this.observaciones = "";
        this.llamadas = 0;
        this.visitas = 0;
        this.cotizaciones = 0;
        this.seguimiento = 0;
        this.prospectos = 0;
        this.clientes = 0;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public String getDayName() {
        return dayName;
    }

    public String getShortName() {
        return shortName;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getFullDateFormatted() {
        return fullDateFormatted;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public String getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(String savedAt) {
        this.savedAt = savedAt;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public int getLlamadas() {
        return llamadas;
    }

    public void setLlamadas(int llamadas) {
        this.llamadas = Math.max(0, llamadas);
    }

    public int getVisitas() {
        return visitas;
    }

    public void setVisitas(int visitas) {
        this.visitas = Math.max(0, visitas);
    }

    public int getCotizaciones() {
        return cotizaciones;
    }

    public void setCotizaciones(int cotizaciones) {
        this.cotizaciones = Math.max(0, cotizaciones);
    }

    public int getSeguimiento() {
        return seguimiento;
    }

    public void setSeguimiento(int seguimiento) {
        this.seguimiento = Math.max(0, seguimiento);
    }

    public int getProspectos() {
        return prospectos;
    }

    public void setProspectos(int prospectos) {
        this.prospectos = Math.max(0, prospectos);
    }

    public int getClientes() {
        return clientes;
    }

    public void setClientes(int clientes) {
        this.clientes = Math.max(0, clientes);
    }

    public int getTotalActividadesDia() {
        return llamadas + visitas + cotizaciones + seguimiento + prospectos + clientes;
    }
}
