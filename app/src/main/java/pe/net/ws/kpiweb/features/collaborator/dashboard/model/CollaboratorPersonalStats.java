package pe.net.ws.kpiweb.features.collaborator.dashboard.model;

/**
 * Modelo para las 4 tarjetas estadísticas del colaborador:
 * Mis puntos, Mi ranking, KPI entregados y Mis asistencias.
 */
public class CollaboratorPersonalStats {

    private final String pointsDisplay;
    private final String pointsTrend;
    private final String rankingDisplay;
    private final String rankingTrend;
    private final String kpiDeliveredDisplay;
    private final String kpiDeliveredSub;
    private final int kpiProgressPercent;
    private final String attendanceDisplay;
    private final String attendanceBadge;

    public CollaboratorPersonalStats(String pointsDisplay, String pointsTrend,
                                   String rankingDisplay, String rankingTrend,
                                   String kpiDeliveredDisplay, String kpiDeliveredSub,
                                   int kpiProgressPercent,
                                   String attendanceDisplay, String attendanceBadge) {
        this.pointsDisplay = pointsDisplay;
        this.pointsTrend = pointsTrend;
        this.rankingDisplay = rankingDisplay;
        this.rankingTrend = rankingTrend;
        this.kpiDeliveredDisplay = kpiDeliveredDisplay;
        this.kpiDeliveredSub = kpiDeliveredSub;
        this.kpiProgressPercent = kpiProgressPercent;
        this.attendanceDisplay = attendanceDisplay;
        this.attendanceBadge = attendanceBadge;
    }

    public String getPointsDisplay() {
        return pointsDisplay;
    }

    public String getPointsTrend() {
        return pointsTrend;
    }

    public String getRankingDisplay() {
        return rankingDisplay;
    }

    public String getRankingTrend() {
        return rankingTrend;
    }

    public String getKpiDeliveredDisplay() {
        return kpiDeliveredDisplay;
    }

    public String getKpiDeliveredSub() {
        return kpiDeliveredSub;
    }

    public int getKpiProgressPercent() {
        return kpiProgressPercent;
    }

    public String getAttendanceDisplay() {
        return attendanceDisplay;
    }

    public String getAttendanceBadge() {
        return attendanceBadge;
    }
}
