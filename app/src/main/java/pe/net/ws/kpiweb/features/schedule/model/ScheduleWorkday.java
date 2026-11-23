package pe.net.ws.kpiweb.features.schedule.model;

/** Jornada personal de la semana actual, leída de Asistencias. */
public final class ScheduleWorkday {
    public String fecha;
    public boolean manana;
    public boolean tarde;
    public String estado;
    public String motivo;

    public boolean hasShift() {
        return manana || tarde;
    }
}
