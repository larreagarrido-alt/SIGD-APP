package pe.net.ws.kpiweb.features.kpi.data;

import java.util.List;

/**
 * Contrato de integración oficial para la pantalla de Ranking y Premios (desarrollada por compañera).
 * Expone únicamente datos limpios de puntajes y cumplimiento sin interferir en sus vistas.
 */
public interface KpiScoreProvider {

    class DayScore {
        public final String date;
        public final String role;
        public final int pointsEarned;
        public final double compliancePercentage;
        public final String performanceLevel;

        public DayScore(String date, String role, int pointsEarned,
                        double compliancePercentage, String performanceLevel) {
            this.date = date;
            this.role = role;
            this.pointsEarned = pointsEarned;
            this.compliancePercentage = compliancePercentage;
            this.performanceLevel = performanceLevel;
        }
    }

    int getTotalPointsForCollaborator(String employeeId);

    int getWeeklyPoints(String employeeId);

    List<DayScore> getScoreHistory(String employeeId);
}
