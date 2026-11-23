package pe.net.ws.kpiweb.features.kpi.administrator.model;

/**
 * Registro diario de producción del cargo ADMINISTRADOR (SIGD 2026).
 * Estructura oficial (Sección 1 - Control Diario de Producción):
 * - Día (Lunes a Sábado)
 * - Modalidad / Actividad Principal
 * - Meta Diaria (por turno)
 * - Gestiones Realizadas
 * - Seguimientos Ejecutados
 * - Reuniones / Coordinaciones
 * - Total Actividades (Gestiones + Seguimientos + Reuniones)
 * - % Cumplimiento
 * - Observaciones
 */
public class KpiAdministratorDayRecord {

    private String date;
    private String dayName;
    private String dayShort;
    private String mainActivity;
    private String dailyGoalLabel;
    private int dailyExpectedTotal;

    private int gestionesCount;
    private int seguimientosCount;
    private int reunionesCount;
    private String observations;

    private boolean registered;
    private boolean locked;

    public KpiAdministratorDayRecord(
            String date,
            String dayName,
            String dayShort,
            String mainActivity,
            String dailyGoalLabel,
            int dailyExpectedTotal
    ) {
        this.date = date != null ? date : "";
        this.dayName = dayName != null ? dayName : "";
        this.dayShort = dayShort != null ? dayShort : "";
        this.mainActivity = mainActivity != null ? mainActivity : "";
        this.dailyGoalLabel = dailyGoalLabel != null ? dailyGoalLabel : "";
        this.dailyExpectedTotal = dailyExpectedTotal > 0 ? dailyExpectedTotal : 4;
        this.gestionesCount = 0;
        this.seguimientosCount = 0;
        this.reunionesCount = 0;
        this.observations = "";
        this.registered = false;
        this.locked = false;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date != null ? date : "";
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName != null ? dayName : "";
    }

    public String getDayShort() {
        return dayShort;
    }

    public void setDayShort(String dayShort) {
        this.dayShort = dayShort != null ? dayShort : "";
    }

    public String getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(String mainActivity) {
        this.mainActivity = mainActivity != null ? mainActivity : "";
    }

    public String getDailyGoalLabel() {
        return dailyGoalLabel;
    }

    public void setDailyGoalLabel(String dailyGoalLabel) {
        this.dailyGoalLabel = dailyGoalLabel != null ? dailyGoalLabel : "";
    }

    public int getDailyExpectedTotal() {
        return dailyExpectedTotal;
    }

    public void setDailyExpectedTotal(int dailyExpectedTotal) {
        this.dailyExpectedTotal = Math.max(1, dailyExpectedTotal);
    }

    public int getGestionesCount() {
        return gestionesCount;
    }

    public void setGestionesCount(int gestionesCount) {
        this.gestionesCount = Math.max(0, gestionesCount);
    }

    public int getSeguimientosCount() {
        return seguimientosCount;
    }

    public void setSeguimientosCount(int seguimientosCount) {
        this.seguimientosCount = Math.max(0, seguimientosCount);
    }

    public int getReunionesCount() {
        return reunionesCount;
    }

    public void setReunionesCount(int reunionesCount) {
        this.reunionesCount = Math.max(0, reunionesCount);
    }

    public int getTotalActivities() {
        return gestionesCount + seguimientosCount + reunionesCount;
    }

    public int getCompliancePercentage() {
        int total = getTotalActivities();
        if (dailyExpectedTotal <= 0) return total > 0 ? 100 : 0;
        return Math.min(100, Math.round((total * 100f) / dailyExpectedTotal));
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations != null ? observations : "";
    }

    public boolean isRegistered() {
        return registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}

