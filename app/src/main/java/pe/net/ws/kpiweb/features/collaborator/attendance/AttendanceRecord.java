package pe.net.ws.kpiweb.features.collaborator.attendance;

/**
 * Modelo individual de registro diario de asistencia.
 * Representa la información requerida para la integración con el sistema anterior:
 * fecha, hora de entrada, hora de salida, condición y justificación si existiese.
 */
public class AttendanceRecord {

    public enum AttendanceStatus {
        ASISTENCIA,
        TARDANZA,
        FALTA,
        JUSTIFICADA
    }

    private final String id;
    private final String dateDisplay;
    private final String dayOfWeek;
    private final String checkInTime;
    private final String checkOutTime;
    private final AttendanceStatus status;
    private final String justificationNote;

    public AttendanceRecord(String id, String dateDisplay, String dayOfWeek,
                            String checkInTime, String checkOutTime,
                            AttendanceStatus status, String justificationNote) {
        this.id = id;
        this.dateDisplay = dateDisplay;
        this.dayOfWeek = dayOfWeek;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.justificationNote = justificationNote;
    }

    public String getId() {
        return id;
    }

    public String getDateDisplay() {
        return dateDisplay;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public String getJustificationNote() {
        return justificationNote;
    }

    public boolean hasJustification() {
        return justificationNote != null && !justificationNote.trim().isEmpty();
    }
}

