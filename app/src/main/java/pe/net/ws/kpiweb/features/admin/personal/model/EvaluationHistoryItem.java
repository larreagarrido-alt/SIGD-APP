package pe.net.ws.kpiweb.features.admin.personal.model;

import java.io.Serializable;

/**
 * Representa el resultado histórico ficticio de una evaluación en un período determinado.
 */
public class EvaluationHistoryItem implements Serializable {
    private final String periodLabel; // ej. "Jul", "Ago", "Set", "Oct"
    private final int score;          // 0 - 100 (%)
    private final String statusLabel; // "Excelente", "Bueno", etc.

    public EvaluationHistoryItem(String periodLabel, int score, String statusLabel) {
        this.periodLabel = periodLabel;
        this.score = score;
        this.statusLabel = statusLabel;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public int getScore() {
        return score;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}
