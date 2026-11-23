package pe.net.ws.kpiweb.features.kpi.engine;

import java.util.List;

/**
 * Motor centralizado y reutilizable para el cálculo matemático y evaluación de KPIs
 * conforme a los lineamientos del Sistema Integral de Gestión del Desempeño (SIGD) 2026.
 * 
 * Reglas de seguridad matemática:
 * 1. Protección estricta contra divisiones entre cero (ArithmeticException / NaN).
 * 2. Diferenciación de polaridad (Mayor es mejor vs. Menor es mejor).
 * 3. Cálculos ponderados según el peso asignado (0% a 100%).
 * 4. No limita arbitrariamente a 100% si las metas contemplan sobrecumplimiento positivo.
 * 5. Ejecución determinista libre de código arbitrario.
 */
public class KpiCalculationEngine {

    public static class EvaluationResult {
        public final double actualValue;
        public final double targetValue;
        public final double rawCompliancePercentage;
        public final double weightedScore;
        public final String performanceLevel;
        public final String levelDescription;
        public final int colorHex;

        public EvaluationResult(double actualValue, double targetValue,
                                double rawCompliancePercentage, double weightedScore,
                                String performanceLevel, String levelDescription, int colorHex) {
            this.actualValue = actualValue;
            this.targetValue = targetValue;
            this.rawCompliancePercentage = roundTwoDecimals(rawCompliancePercentage);
            this.weightedScore = roundTwoDecimals(weightedScore);
            this.performanceLevel = performanceLevel;
            this.levelDescription = levelDescription;
            this.colorHex = colorHex;
        }
    }

    /**
     * Calcula el porcentaje de cumplimiento de un indicador individual.
     */
    public static double calculateCompliance(double actual, double target,
                                             KpiMeasurementType type, KpiPolarity polarity) {
        if (polarity == null) polarity = KpiPolarity.MAYOR_ES_MEJOR;
        if (type == null) type = KpiMeasurementType.CANTIDAD;

        // Protección contra división por cero
        if (target <= 0.0) {
            if (actual == 0.0) return 100.0;
            return (polarity == KpiPolarity.MAYOR_ES_MEJOR) ? 100.0 : 0.0;
        }

        double compliance;
        if (polarity == KpiPolarity.MAYOR_ES_MEJOR) {
            // Regla directa: Más es mejor (ej. ventas, piezas producidas)
            compliance = Math.max(0.0, (actual / target) * 100.0);
        } else {
            // Regla inversa: Menos es mejor (ej. errores de producción, tiempos de demora)
            if (actual <= target) {
                compliance = 100.0;
            } else {
                double excess = actual - target;
                compliance = Math.max(0.0, (1.0 - (excess / target)) * 100.0);
            }
        }

        return roundTwoDecimals(compliance);
    }

    /**
     * Aplica la ponderación (%) sobre el cumplimiento obtenido.
     */
    public static double calculateWeightedScore(double compliancePercentage, double weightPercentage) {
        if (weightPercentage <= 0.0) return 0.0;
        return roundTwoDecimals(compliancePercentage * (weightPercentage / 100.0));
    }

    /**
     * Evalúa integralmente un indicador devolviendo el resultado desglosado.
     */
    public static EvaluationResult evaluateIndicator(double actual, double target,
                                                     double weight, KpiMeasurementType type,
                                                     KpiPolarity polarity) {
        double compliance = calculateCompliance(actual, target, type, polarity);
        double weighted = calculateWeightedScore(compliance, weight);
        String level = getLevelFromPercentage(compliance);
        String desc = getLevelDescription(level);
        int color = getLevelColor(level);

        return new EvaluationResult(actual, target, compliance, weighted, level, desc, color);
    }

    /**
     * Suma los puntajes ponderados de una lista de indicadores activos.
     */
    public static double calculateTotalWeightedScore(List<Double> weightedScores) {
        if (weightedScores == null || weightedScores.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Double score : weightedScores) {
            if (score != null) {
                sum += score;
            }
        }
        return roundTwoDecimals(sum);
    }

    /**
     * Clasifica el nivel de desempeño según el baremo institucional oficial del SIGD 2026.
     * Escala unificada con los módulos oficiales:
     * - Excelente: >= 90%
     * - Muy Bueno: >= 80%
     * - Bueno:     >= 60%
     * - Regular:   >= 40%
     * - Deficiente: < 40%
     */
    public static String getLevelFromPercentage(double percentage) {
        if (percentage >= 90.0) {
            return "EXCELENTE";
        } else if (percentage >= 80.0) {
            return "MUY BUENO";
        } else if (percentage >= 60.0) {
            return "BUENO";
        } else if (percentage >= 40.0) {
            return "REGULAR";
        } else {
            return "DEFICIENTE";
        }
    }

    public static String getLevelDescription(String level) {
        if (level == null) return "Sin evaluación";
        switch (level) {
            case "EXCELENTE":
                return "Superó ampliamente las metas e indicadores establecidos.";
            case "MUY BUENO":
                return "Cumplió con solvencia las metas requeridas para su puesto.";
            case "BUENO":
                return "Alcanzó el estándar mínimo operativo esperado.";
            case "REGULAR":
                return "Rendimiento por debajo del umbral, requiere plan de mejora.";
            case "DEFICIENTE":
                return "Incumplimiento crítico de los objetivos asignados.";
            default:
                return "Nivel en proceso de cálculo.";
        }
    }

    public static int getLevelColor(String level) {
        if (level == null) return 0xFF64748B;
        switch (level) {
            case "EXCELENTE":
                return 0xFF10B981; // Verde esmeralda
            case "MUY BUENO":
                return 0xFF0284C7; // Azul corporativo
            case "BUENO":
                return 0xFF6366F1; // Índigo
            case "REGULAR":
                return 0xFFF59E0B; // Ámbar / Naranja
            case "DEFICIENTE":
                return 0xFFEF4444; // Rojo alerta
            default:
                return 0xFF64748B;
        }
    }

    public static double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
