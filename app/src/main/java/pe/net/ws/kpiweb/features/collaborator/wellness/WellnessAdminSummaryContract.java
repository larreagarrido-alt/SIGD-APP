package pe.net.ws.kpiweb.features.collaborator.wellness;

import java.util.Collections;
import java.util.List;

/**
 * Contrato de datos y política de privacidad para el Resumen Administrativo Agregado
 * de Bienestar Ocupacional (WEBSITE).
 *
 * Reglas institucionales obligatorias:
 * 1. Privacidad y anonimato: Nunca exponer respuestas individuales identificables
 *    de un colaborador específico hacia paneles administrativos si la política lo restringe.
 * 2. Umbral mínimo de anonimato (k-anonymity): No generar ni mostrar reportes agregados
 *    cuando el grupo o rango consultado tenga menos de {@link #MIN_PARTICIPANTS_FOR_REPORT}
 *    participantes, evitando la identificación indirecta de una persona.
 * 3. Independencia estricta: Las respuestas del test de bienestar NUNCA afectan KPI,
 *    premios, puntos ni ranking competitivo.
 */
public final class WellnessAdminSummaryContract {

    /**
     * Cantidad mínima de colaboradores evaluados en un período o departamento
     * para habilitar la visualización estadística agregada sin comprometer el anonimato.
     */
    public static final int MIN_PARTICIPANTS_FOR_REPORT = 5;

    /**
     * Garantía explícita de que el módulo de Bienestar no altera puntos ni KPI.
     */
    public static final boolean AFFECTS_KPI_REWARDS_OR_RANKING = false;

    private WellnessAdminSummaryContract() {
    }

    public static final class AggregatedPeriodSummary {
        private final String periodLabel;
        private final String modalityType; // "SEMANAL" | "MENSUAL"
        private final int completedEvaluationsCount;
        private final int totalEligibleCollaborators;
        private final float participationRatePercent;
        private final boolean privacyThresholdMet;
        private final String generalWellnessTrend;
        private final List<DimensionTrendItem> anonymizedDimensionTrends;

        public AggregatedPeriodSummary(
                String periodLabel,
                String modalityType,
                int completedEvaluationsCount,
                int totalEligibleCollaborators,
                String generalWellnessTrend,
                List<DimensionTrendItem> anonymizedDimensionTrends) {
            this.periodLabel = periodLabel;
            this.modalityType = modalityType;
            this.completedEvaluationsCount = completedEvaluationsCount;
            this.totalEligibleCollaborators = Math.max(1, totalEligibleCollaborators);
            this.participationRatePercent = (completedEvaluationsCount * 100f) / this.totalEligibleCollaborators;
            this.privacyThresholdMet = completedEvaluationsCount >= MIN_PARTICIPANTS_FOR_REPORT;
            this.generalWellnessTrend = this.privacyThresholdMet
                    ? generalWellnessTrend
                    : "Datos protegidos por umbral mínimo de confidencialidad (< "
                            + MIN_PARTICIPANTS_FOR_REPORT + " registros)";
            this.anonymizedDimensionTrends = this.privacyThresholdMet && anonymizedDimensionTrends != null
                    ? Collections.unmodifiableList(anonymizedDimensionTrends)
                    : Collections.emptyList();
        }

        public String getPeriodLabel() {
            return periodLabel;
        }

        public String getModalityType() {
            return modalityType;
        }

        public int getCompletedEvaluationsCount() {
            return completedEvaluationsCount;
        }

        public int getTotalEligibleCollaborators() {
            return totalEligibleCollaborators;
        }

        public float getParticipationRatePercent() {
            return participationRatePercent;
        }

        public boolean isPrivacyThresholdMet() {
            return privacyThresholdMet;
        }

        public String getGeneralWellnessTrend() {
            return generalWellnessTrend;
        }

        public List<DimensionTrendItem> getAnonymizedDimensionTrends() {
            return anonymizedDimensionTrends;
        }
    }

    public static final class DimensionTrendItem {
        private final String dimensionName;
        private final String predominantMoodLabel;
        private final int positiveSharePercent;

        public DimensionTrendItem(String dimensionName, String predominantMoodLabel, int positiveSharePercent) {
            this.dimensionName = dimensionName;
            this.predominantMoodLabel = predominantMoodLabel;
            this.positiveSharePercent = positiveSharePercent;
        }

        public String getDimensionName() {
            return dimensionName;
        }

        public String getPredominantMoodLabel() {
            return predominantMoodLabel;
        }

        public int getPositiveSharePercent() {
            return positiveSharePercent;
        }
    }
}

