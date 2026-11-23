package pe.net.ws.kpiweb.features.admin.dashboard.model;

/**
 * Modelo para las barras de rendimiento histórico mensual del equipo.
 */
public class AdminMonthlyPerformance {
    private final String monthLabel;
    private final int percentage;

    public AdminMonthlyPerformance(String monthLabel, int percentage) {
        this.monthLabel = monthLabel;
        this.percentage = percentage;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public int getPercentage() {
        return percentage;
    }
}
