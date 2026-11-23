package pe.net.ws.kpiweb.features.collaborator.attendance;

/**
 * Resumen consolidado mensual de asistencias del colaborador.
 * Provee métricas de puntualidad y porcentaje de cumplimiento mensual.
 */
public class AttendanceSummary {

    private final String monthName;
    private final int totalWorkDays;
    private final int attendedDays;
    private final int tardinessDays;
    private final int absenceDays;
    private final int justifiedDays;
    private final int attendancePercentage;

    public AttendanceSummary(String monthName, int totalWorkDays, int attendedDays,
                             int tardinessDays, int absenceDays, int justifiedDays,
                             int attendancePercentage) {
        this.monthName = monthName;
        this.totalWorkDays = totalWorkDays;
        this.attendedDays = attendedDays;
        this.tardinessDays = tardinessDays;
        this.absenceDays = absenceDays;
        this.justifiedDays = justifiedDays;
        this.attendancePercentage = attendancePercentage;
    }

    public String getMonthName() {
        return monthName;
    }

    public int getTotalWorkDays() {
        return totalWorkDays;
    }

    public int getAttendedDays() {
        return attendedDays;
    }

    public int getTardinessDays() {
        return tardinessDays;
    }

    public int getAbsenceDays() {
        return absenceDays;
    }

    public int getJustifiedDays() {
        return justifiedDays;
    }

    public int getAttendancePercentage() {
        return attendancePercentage;
    }

    public String getFormattedPercentage() {
        return attendancePercentage + "%";
    }
}

