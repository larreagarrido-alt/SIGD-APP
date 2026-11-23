package pe.net.ws.kpiweb.features.admin.dashboard.model;

/**
 * Resumen del equipo con indicadores complementarios.
 */
public class AdminTeamSummary {
    private final int collaboratorsCount;
    private final int generalAveragePercentage;
    private final int testsCompleted;
    private final int testsTotal;
    private final int kpiOnTrackPercentage;

    public AdminTeamSummary(int collaboratorsCount, int generalAveragePercentage, int testsCompleted, int testsTotal, int kpiOnTrackPercentage) {
        this.collaboratorsCount = collaboratorsCount;
        this.generalAveragePercentage = generalAveragePercentage;
        this.testsCompleted = testsCompleted;
        this.testsTotal = testsTotal;
        this.kpiOnTrackPercentage = kpiOnTrackPercentage;
    }

    public int getCollaboratorsCount() {
        return collaboratorsCount;
    }

    public int getGeneralAveragePercentage() {
        return generalAveragePercentage;
    }

    public int getTestsCompleted() {
        return testsCompleted;
    }

    public int getTestsTotal() {
        return testsTotal;
    }

    public int getKpiOnTrackPercentage() {
        return kpiOnTrackPercentage;
    }
}
