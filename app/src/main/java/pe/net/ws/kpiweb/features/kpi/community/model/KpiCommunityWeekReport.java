package pe.net.ws.kpiweb.features.kpi.community.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Reporte semanal consolidado del cargo COMMUNITY MANAGER (SIGD 2026).
 * Consolida los 6 días de producción (Lunes a Sábado), el resumen semanal de 4 indicadores,
 * las 6 acciones realizadas (Sección 3) y las Observaciones Generales (Sección 5).
 */
public class KpiCommunityWeekReport {

    private String weekId;
    private int weekNumber;
    private String dateRange;
    private String status;
    private boolean morningShift = true;

    // Acciones del Community Manager (Sección 3)
    private boolean action1GestionarRedes = true;
    private boolean action2ProgramarContenidos = true;
    private boolean action3AtenderMensajes = true;
    private boolean action4SeguimientoMetricas = true;
    private boolean action5ElaborarReporte = true;
    private boolean action6CoordinarPresencia = true;
    private boolean otherActivitiesChecked = false;

    private String generalObservations;
    private List<KpiCommunityDayRecord> days = new ArrayList<>();

    public KpiCommunityWeekReport(String weekId, int weekNumber, String dateRange) {
        this.weekId = weekId != null ? weekId : "";
        this.weekNumber = weekNumber;
        this.dateRange = dateRange != null ? dateRange : "";
        this.status = "En curso";
        this.morningShift = true;
        this.action1GestionarRedes = true;
        this.action2ProgramarContenidos = true;
        this.action3AtenderMensajes = true;
        this.action4SeguimientoMetricas = true;
        this.action5ElaborarReporte = true;
        this.action6CoordinarPresencia = true;
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

    public boolean isAction1GestionarRedes() {
        return action1GestionarRedes;
    }

    public void setAction1GestionarRedes(boolean action1GestionarRedes) {
        this.action1GestionarRedes = action1GestionarRedes;
    }

    public boolean isAction2ProgramarContenidos() {
        return action2ProgramarContenidos;
    }

    public void setAction2ProgramarContenidos(boolean action2ProgramarContenidos) {
        this.action2ProgramarContenidos = action2ProgramarContenidos;
    }

    public boolean isAction3AtenderMensajes() {
        return action3AtenderMensajes;
    }

    public void setAction3AtenderMensajes(boolean action3AtenderMensajes) {
        this.action3AtenderMensajes = action3AtenderMensajes;
    }

    public boolean isAction4SeguimientoMetricas() {
        return action4SeguimientoMetricas;
    }

    public void setAction4SeguimientoMetricas(boolean action4SeguimientoMetricas) {
        this.action4SeguimientoMetricas = action4SeguimientoMetricas;
    }

    public boolean isAction5ElaborarReporte() {
        return action5ElaborarReporte;
    }

    public void setAction5ElaborarReporte(boolean action5ElaborarReporte) {
        this.action5ElaborarReporte = action5ElaborarReporte;
    }

    public boolean isAction6CoordinarPresencia() {
        return action6CoordinarPresencia;
    }

    public void setAction6CoordinarPresencia(boolean action6CoordinarPresencia) {
        this.action6CoordinarPresencia = action6CoordinarPresencia;
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

    public List<KpiCommunityDayRecord> getDays() {
        return days;
    }

    public void setDays(List<KpiCommunityDayRecord> days) {
        this.days = days != null ? days : new ArrayList<>();
    }

    // Totales calculados
    public int getTotalPublicaciones() {
        int sum = 0;
        for (KpiCommunityDayRecord d : days) {
            sum += d.getPublicacionesCount();
        }
        return sum;
    }

    public int getTotalInteracciones() {
        int sum = 0;
        for (KpiCommunityDayRecord d : days) {
            sum += d.getInteraccionesCount();
        }
        return sum;
    }

    public int getTotalOtrasActividades() {
        int sum = 0;
        for (KpiCommunityDayRecord d : days) {
            sum += d.getOtrasActividadesCount();
        }
        return sum;
    }

    public int getTotalActivities() {
        return getTotalPublicaciones() + getTotalInteracciones() + getTotalOtrasActividades();
    }

    public int getPublicacionesCompliance() {
        int target = 10;
        int total = getTotalPublicaciones();
        return Math.min(100, Math.round((total * 100f) / target));
    }

    public int getInteraccionesCompliance() {
        int target = 50;
        int total = getTotalInteracciones();
        return Math.min(100, Math.round((total * 100f) / target));
    }

    public int getMetricasCompliance() {
        return 100;
    }

    public int getReporteCompliance() {
        return 100;
    }

    public int getGeneralCompliancePercentage() {
        int c1 = getPublicacionesCompliance();
        int c2 = getInteraccionesCompliance();
        int c3 = getMetricasCompliance();
        int c4 = getReporteCompliance();
        return Math.round((c1 + c2 + c3 + c4) / 4f);
    }

    public String getProductivityLevel() {
        int percentage = getProductivityPercentage();
        if (percentage >= 90) {
            return "EXCELENTE";
        } else if (percentage >= 80) {
            return "MUY BUENO";
        } else if (percentage >= 60) {
            return "BUENO";
        } else if (percentage >= 40) {
            return "REGULAR";
        } else {
            return "DEFICIENTE";
        }
    }

    public String getProductivityDescription() {
        int percentage = getProductivityPercentage();
        if (percentage >= 90) {
            return "Cumple y supera el objetivo semanal.";
        } else if (percentage >= 80) {
            return "Cumple y está por llegar al objetivo.";
        } else if (percentage >= 60) {
            return "Cumple con la mitad recomendada.";
        } else if (percentage >= 40) {
            return "Baja actividad respecto a la meta.";
        } else {
            return "No cumple con las expectativas del área.";
        }
    }

    public int getProductivityPercentage() {
        return Math.min(100, Math.round(getTotalActivities() * 100f / 65f));
    }
}

