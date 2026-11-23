package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;

/**
 * Modelo para las actividades de la Hoja de Ruta comercial (Visitas, llamadas y seguimientos).
 * Permite registro manual sin captura GPS ni fotos obligatorias.
 */
public class KpiRouteItem implements Serializable {

    private long id;
    private String activityType;    // "Visita presencial", "Llamada", "Seguimiento"
    private String businessName;    // Nombre del negocio
    private String contactPerson;   // Contacto
    private String phone;           // Teléfono
    private String manualAddress;   // Dirección manual (opcional)
    private String date;            // Fecha (YYYY-MM-DD o formato amigable)
    private String dayName;         // Lunes, Martes, Miércoles, Jueves, Viernes, Sábado
    private String startTime;       // Hora inicio (HH:mm)
    private String endTime;         // Hora fin (HH:mm)
    private String commercialResult;// Interesado, Venta Cerrada, En Seguimiento, No Interesado, etc.
    private String observations;    // Observaciones
    private long timestamp;

    public KpiRouteItem() {
        this.timestamp = System.currentTimeMillis();
    }

    public KpiRouteItem(long id, String activityType, String businessName, String contactPerson,
                        String phone, String manualAddress, String date, String dayName,
                        String startTime, String endTime, String commercialResult,
                        String observations, long timestamp) {
        this.id = id;
        this.activityType = activityType;
        this.businessName = businessName;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.manualAddress = manualAddress;
        this.date = date;
        this.dayName = dayName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.commercialResult = commercialResult;
        this.observations = observations;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getManualAddress() { return manualAddress; }
    public void setManualAddress(String manualAddress) { this.manualAddress = manualAddress; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getCommercialResult() { return commercialResult; }
    public void setCommercialResult(String commercialResult) { this.commercialResult = commercialResult; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
