package pe.net.ws.kpiweb.features.kpi.trafficker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Reporte semanal consolidado del cargo TRAFFICKER DIGITAL (SIGD 2026).
 * Consolida los 6 días de producción (Lunes a Sábado), el resumen semanal de indicadores,
 * las acciones realizadas (Sección 3) y las Observaciones Generales (Sección 5).
 */
public class KpiTraffickerWeekReport {

    private String weekId;
    private int weekNumber;
    private String dateRange;
    private String status;
    private boolean morningShift = true;
    private boolean otherActivitiesChecked = false;
    private String generalObservations;
    private List<KpiTraffickerDayRecord> days = new ArrayList<>();

    public KpiTraffickerWeekReport(String weekId, int weekNumber, String dateRange) {
        this.weekId = weekId != null ? weekId : "";
        this.weekNumber = weekNumber;
        this.dateRange = dateRange != null ? dateRange : "";
        this.status = "En curso";
        this.morningShift = true;
        this.otherActivitiesChecked = false;
        this.generalObservations = "";
    }

    public String getWeekId() {
        return weekId;
    }

    public void setWeekId(String weekId) {
        this.weekId = weekId != null ? weekId : "";
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange != null ? dateRange : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "En curso";
    }

    public boolean isMorningShift() {
        return morningShift;
    }

    public void setMorningShift(boolean morningShift) {
        this.morningShift = morningShift;
    }

    public boolean isOtherActivitiesChecked() {
        return otherActivitiesChecked;
    }

    public void setOtherActivitiesChecked(boolean otherActivitiesChecked) {
        this.otherActivitiesChecked = otherActivitiesChecked;
    }

    public String getGeneralObservations() {
        return generalObservations != null ? generalObservations : "";
    }

    public void setGeneralObservations(String generalObservations) {
        this.generalObservations = generalObservations != null ? generalObservations : "";
    }

    public List<KpiTraffickerDayRecord> getDays() {
        return days;
    }

    public void setDays(List<KpiTraffickerDayRecord> days) {
        this.days = days != null ? days : new ArrayList<>();
    }
}

