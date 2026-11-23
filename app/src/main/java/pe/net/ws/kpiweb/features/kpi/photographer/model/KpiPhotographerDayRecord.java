package pe.net.ws.kpiweb.features.kpi.photographer.model;

/**
 * Registro diario de producción del cargo FOTÓGRAFO (SIGD 2026).
 * Estructura oficial (Sección 1 - Control Diario de Producción):
 * - Día (Lunes a Sábado)
 * - Modalidad / Actividad Principal
 * - Meta Diaria (por turno)
 * - Sesiones Realizadas
 * - Fotos Capturadas
 * - Fotos Editadas
 * - Total Actividades (Bloques cumplidos: Sesiones + Captura + Edición)
 * - % Cumplimiento
 * - Observaciones
 */
public class KpiPhotographerDayRecord {

    private String date;
    private String dayName;
    private String dayShort;
    private String mainActivity;
    private String dailyGoalLabel;
    private int dailyExpectedTotal;

    private int sesionesCount;
    private int fotosCapturadasCount;
    private int fotosEditadasCount;
    private String observations;

    private boolean registered;
    private boolean locked;

    public KpiPhotographerDayRecord(
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
        this.dailyExpectedTotal = dailyExpectedTotal > 0 ? dailyExpectedTotal : 3;
        this.sesionesCount = 0;
        this.fotosCapturadasCount = 0;
        this.fotosEditadasCount = 0;
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

    public int getSesionesCount() {
        return sesionesCount;
    }

    public void setSesionesCount(int sesionesCount) {
        this.sesionesCount = Math.max(0, sesionesCount);
    }

    public int getFotosCapturadasCount() {
        return fotosCapturadasCount;
    }

    public void setFotosCapturadasCount(int fotosCapturadasCount) {
        this.fotosCapturadasCount = Math.max(0, fotosCapturadasCount);
    }

    public int getFotosEditadasCount() {
        return fotosEditadasCount;
    }

    public void setFotosEditadasCount(int fotosEditadasCount) {
        this.fotosEditadasCount = Math.max(0, fotosEditadasCount);
    }

    /**
     * Calcula el Total de Actividades del día según el formato oficial SIGD de Fotógrafo:
     * Sesiones realizadas + bloque de Fotos Capturadas + bloque de Fotos Editadas
     * (Ej. Lunes a Viernes: 1 + 1 + 1 = 3; Sábado: 0 + 1 + 1 = 2; Total Semanal = 17).
     */
    public int getTotalActivities() {
        int s = sesionesCount;
        int capBlock = fotosCapturadasCount > 0 ? 1 : 0;
        int editBlock = fotosEditadasCount > 0 ? 1 : 0;
        return s + capBlock + editBlock;
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

