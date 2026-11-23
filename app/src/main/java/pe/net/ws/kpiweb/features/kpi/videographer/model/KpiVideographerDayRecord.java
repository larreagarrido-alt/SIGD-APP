package pe.net.ws.kpiweb.features.kpi.videographer.model;

/**
 * Registro diario de producción del cargo VIDEÓGRAFO (SIGD 2026).
 * Estructura oficial (Sección 1 - Control Diario de Producción):
 * - Día (Lunes a Sábado)
 * - Modalidad / Actividad Principal
 * - Meta Diaria (por turno)
 * - Videos Grabados
 * - Videos Editados
 * - Ajustes / Correcciones
 * - Total Actividades (Sumatoria de grabaciones, ediciones y ajustes)
 * - % Cumplimiento
 * - Observaciones
 */
public class KpiVideographerDayRecord {

    private String date;
    private String dayName;
    private String dayShort;
    private String mainActivity;
    private String dailyGoalLabel;
    private int dailyExpectedTotal;

    private int videosGrabadosCount;
    private int videosEditadosCount;
    private int ajustesCorreccionesCount;
    private String observations;

    private boolean registered;
    private boolean locked;

    public KpiVideographerDayRecord(
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
        this.videosGrabadosCount = 0;
        this.videosEditadosCount = 0;
        this.ajustesCorreccionesCount = 0;
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

    public int getVideosGrabadosCount() {
        return videosGrabadosCount;
    }

    public void setVideosGrabadosCount(int videosGrabadosCount) {
        this.videosGrabadosCount = Math.max(0, videosGrabadosCount);
    }

    public int getVideosEditadosCount() {
        return videosEditadosCount;
    }

    public void setVideosEditadosCount(int videosEditadosCount) {
        this.videosEditadosCount = Math.max(0, videosEditadosCount);
    }

    public int getAjustesCorreccionesCount() {
        return ajustesCorreccionesCount;
    }

    public void setAjustesCorreccionesCount(int ajustesCorreccionesCount) {
        this.ajustesCorreccionesCount = Math.max(0, ajustesCorreccionesCount);
    }

    /**
     * Sumatoria de videos grabados, videos editados y ajustes/correcciones.
     */
    public int getTotalActivities() {
        return videosGrabadosCount + videosEditadosCount + ajustesCorreccionesCount;
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

