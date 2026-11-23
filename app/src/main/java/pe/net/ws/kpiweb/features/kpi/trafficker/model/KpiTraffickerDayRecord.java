package pe.net.ws.kpiweb.features.kpi.trafficker.model;

/**
 * Registro diario de producción del cargo TRAFFICKER DIGITAL (SIGD 2026).
 * Estructura oficial (Sección 1 - Control Diario de Producción):
 * - Día (Lunes a Sábado)
 * - Modalidad / Actividad Principal
 * - Meta Diaria (por turno)
 * - Campañas Gestionadas
 * - Ajustes / Optimizaciones
 * - Reportes / Análisis
 * - Total Actividades (Sumatoria de campañas, ajustes y reportes)
 * - % Cumplimiento
 * - Observaciones
 */
public class KpiTraffickerDayRecord {

    private String date;
    private String dayName;
    private String dayShort;
    private String mainActivity;
    private String dailyGoalLabel;
    private int dailyExpectedTotal;

    private int campanasCount;
    private int ajustesCount;
    private int reportesCount;
    private String observations;

    private boolean registered;
    private boolean locked;

    public KpiTraffickerDayRecord(
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
        this.dailyExpectedTotal = dailyExpectedTotal > 0 ? dailyExpectedTotal : 1;
        this.campanasCount = 0;
        this.ajustesCount = 0;
        this.reportesCount = 0;
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

    public int getCampanasCount() {
        return campanasCount;
    }

    public void setCampanasCount(int campanasCount) {
        this.campanasCount = Math.max(0, campanasCount);
    }

    public int getAjustesCount() {
        return ajustesCount;
    }

    public void setAjustesCount(int ajustesCount) {
        this.ajustesCount = Math.max(0, ajustesCount);
    }

    public int getReportesCount() {
        return reportesCount;
    }

    public void setReportesCount(int reportesCount) {
        this.reportesCount = Math.max(0, reportesCount);
    }

    /**
     * Sumatoria de campañas gestionadas, ajustes/optimizaciones y reportes/análisis.
     */
    public int getTotalActivities() {
        return campanasCount + ajustesCount + reportesCount;
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

