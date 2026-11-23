package pe.net.ws.kpiweb.features.kpi.accountant.model;

/**
 * Registro diario de producción del cargo CONTADOR (SIGD 2026).
 * Estructura oficial (Sección 1 - Control Diario de Producción):
 * - Día (Lunes a Sábado)
 * - Modalidad / Actividad Principal
 * - Meta Diaria (por turno)
 * - Registros Contables
 * - Conciliaciones Bancarias
 * - Comprobantes Procesados
 * - Total Actividades (Registros + Conciliaciones + Comprobantes)
 * - % Cumplimiento
 * - Observaciones
 */
public class KpiAccountantDayRecord {

    private String date;
    private String dayName;
    private String dayShort;
    private String mainActivity;
    private String dailyGoalLabel;
    private int dailyExpectedTotal;

    private int registrosCount;
    private int conciliacionesCount;
    private int comprobantesCount;
    private String observations;

    private boolean registered;
    private boolean locked;

    public KpiAccountantDayRecord(
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
        this.dailyExpectedTotal = dailyExpectedTotal > 0 ? dailyExpectedTotal : 12;
        this.registrosCount = 0;
        this.conciliacionesCount = 0;
        this.comprobantesCount = 0;
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

    public int getRegistrosCount() {
        return registrosCount;
    }

    public void setRegistrosCount(int registrosCount) {
        this.registrosCount = Math.max(0, registrosCount);
    }

    public int getConciliacionesCount() {
        return conciliacionesCount;
    }

    public void setConciliacionesCount(int conciliacionesCount) {
        this.conciliacionesCount = Math.max(0, conciliacionesCount);
    }

    public int getComprobantesCount() {
        return comprobantesCount;
    }

    public void setComprobantesCount(int comprobantesCount) {
        this.comprobantesCount = Math.max(0, comprobantesCount);
    }

    public int getTotalActivities() {
        return registrosCount + conciliacionesCount + comprobantesCount;
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

