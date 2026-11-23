package pe.net.ws.kpiweb.features.collaborator.dashboard.model;

/**
 * Modelo para la tarjeta Hero de rendimiento personal del colaborador.
 * El porcentaje de avance puede ser null si aún está "Pendiente de evaluación".
 */
public class CollaboratorPerformanceSummary {

    private final String periodLabel;
    private final String headline;
    private final String comparisonTrend;
    private final Integer scorePercentage; // Nullable
    private final String summaryFooter;

    public CollaboratorPerformanceSummary(String periodLabel, String headline,
                                         String comparisonTrend, Integer scorePercentage,
                                         String summaryFooter) {
        this.periodLabel = periodLabel;
        this.headline = headline;
        this.comparisonTrend = comparisonTrend;
        this.scorePercentage = scorePercentage;
        this.summaryFooter = summaryFooter;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public String getHeadline() {
        return headline;
    }

    public String getComparisonTrend() {
        return comparisonTrend;
    }

    public Integer getScorePercentage() {
        return scorePercentage;
    }

    public boolean hasScore() {
        return scorePercentage != null;
    }

    public String getSummaryFooter() {
        return summaryFooter;
    }
}
