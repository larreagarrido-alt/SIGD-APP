package pe.net.ws.kpiweb.features.admin.dashboard;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignment;

/**
 * Utilidades puras para el Menú del Administrador y la vista personal "Mis evaluaciones".
 * Calcula métricas estrictamente individuales de desempeño (sin ranking ni premios canjeables).
 */
public final class AdminMenuFormatter {

    public static final class PersonalEvalSummary {
        public final int total;
        public final int approved;
        public final int pending;
        public final String scoreDisplay;
        public final boolean hasScore;

        public PersonalEvalSummary(int total, int approved, int pending, String scoreDisplay, boolean hasScore) {
            this.total = total;
            this.approved = approved;
            this.pending = pending;
            this.scoreDisplay = scoreDisplay;
            this.hasScore = hasScore;
        }
    }

    private AdminMenuFormatter() {}

    /** Devuelve solo el primer nombre para mantener compacta la tarjeta de perfil del menú. */
    public static String extractFirstName(String fullName) {
        if (fullName == null) return "Administrador";
        String clean = fullName.trim();
        if (clean.isEmpty()) return "Administrador";
        String[] parts = clean.split("\\s+");
        return parts.length > 0 && !parts[0].isEmpty() ? parts[0] : "Administrador";
    }

    public static String resolveRoleLabel(String rawRole) {
        if (rawRole == null || rawRole.trim().isEmpty()) return "Administrador";
        return rawRole.trim();
    }

    /**
     * Resume las evaluaciones personales del administrador autenticado.
     * La puntuación refleja únicamente el cumplimiento individual (ponderado por peso cuando existe),
     * separada de rankings competitivos o puntos canjeables.
     */
    public static PersonalEvalSummary computePersonalEvaluationSummary(List<KpiServerAssignment> rows) {
        if (rows == null || rows.isEmpty()) {
            return new PersonalEvalSummary(0, 0, 0, "—", false);
        }
        int total = 0;
        int approved = 0;
        int pending = 0;
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        double simpleSum = 0.0;
        int scoredCount = 0;

        for (KpiServerAssignment item : rows) {
            if (item == null) continue;
            total++;
            String status = item.getStatus() == null ? "" : item.getStatus().trim().toLowerCase(Locale.ROOT);
            if ("aprobado".equals(status)) {
                approved++;
            } else if (!"cancelado".equals(status) && !"no exigible".equals(status)) {
                pending++;
            }
            if (item.getCompliance() != null) {
                double pct = item.getCompliance();
                double weight = item.getWeight();
                if (weight > 0) {
                    weightedSum += pct * weight;
                    weightTotal += weight;
                }
                simpleSum += pct;
                scoredCount++;
            }
        }

        if (scoredCount == 0) {
            return new PersonalEvalSummary(total, approved, pending, "—", false);
        }
        double score = weightTotal > 0 ? (weightedSum / weightTotal) : (simpleSum / scoredCount);
        String formatted = String.format(Locale.US, "%.1f%%", score);
        return new PersonalEvalSummary(total, approved, pending, formatted, true);
    }
}
