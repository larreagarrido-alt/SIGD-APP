package pe.net.ws.kpiweb.features.kpi.community.model;

/**
 * Registro diario de producción del cargo COMMUNITY MANAGER (SIGD 2026).
 * Estructura oficial (Sección 1 - Control Diario de Producción):
 * - Día (Lunes a Sábado)
 * - Modalidad / Actividad Principal
 * - Meta Diaria (por turno)
 * - Publicaciones realizadas (Post, Carrusel o Historia)
 * - Interacciones atendidas (Mensajes, comentarios, consultas y respuestas)
 * - Otras actividades
 * - Total Actividades (Sumatoria de publicaciones, interacciones y otras actividades)
 * - % Cumplimiento
 * - Observaciones
 */
public class KpiCommunityDayRecord {

    private String date;
    private String dayName;
    private String dayShort;
    private String mainActivity;
    private String dailyGoalLabel;
    private int dailyExpectedTotal;

    private int publicacionesCount;
    private int interaccionesCount;
    private int otrasActividadesCount;
    private String observations;

    private boolean registered;
    private boolean locked;

    public KpiCommunityDayRecord(
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
        this.publicacionesCount = 0;
        this.interaccionesCount = 0;
        this.otrasActividadesCount = 0;
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
        this.dailyExpectedTotal = dailyExpectedTotal > 0 ? dailyExpectedTotal : 1;
    }

    public int getPublicacionesCount() {
        return publicacionesCount;
    }

    public void setPublicacionesCount(int publicacionesCount) {
        this.publicacionesCount = Math.max(0, publicacionesCount);
    }

    public int getInteraccionesCount() {
        return interaccionesCount;
    }

    public void setInteraccionesCount(int interaccionesCount) {
        this.interaccionesCount = Math.max(0, interaccionesCount);
    }

    public int getOtrasActividadesCount() {
        return otrasActividadesCount;
    }

    public void setOtrasActividadesCount(int otrasActividadesCount) {
        this.otrasActividadesCount = Math.max(0, otrasActividadesCount);
    }

    public int getTotalActivities() {
        return publicacionesCount + interaccionesCount + otrasActividadesCount;
    }

    public int getCompliancePercentage() {
        if (!registered && getTotalActivities() == 0) {
            return 0;
        }
        if (dailyExpectedTotal <= 0) return 100;
        int pct = Math.round((getTotalActivities() * 100f) / dailyExpectedTotal);
        return Math.min(100, Math.max(0, pct));
    }

    public String getObservations() {
        return observations != null ? observations : "";
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

