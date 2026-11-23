package pe.net.ws.kpiweb.features.kpi.engine;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pruebas unitarias automatizadas para verificar que el motor matemático
 * del SIGD 2026 es 100% determinista, seguro y libre de excepciones en casos de borde.
 */
public class KpiCalculationEngineTest {

    private static final double DELTA = 0.001;

    @Test
    public void testDivisionByZeroProtection() {
        // Meta 0 con valor 0 debe retornar 100% de cumplimiento
        double compliance = KpiCalculationEngine.calculateCompliance(0, 0, KpiMeasurementType.CANTIDAD, KpiPolarity.MAYOR_ES_MEJOR);
        assertEquals(100.0, compliance, DELTA);

        // Meta 0 con valor positivo en Mayor es Mejor
        double compPos = KpiCalculationEngine.calculateCompliance(5, 0, KpiMeasurementType.CANTIDAD, KpiPolarity.MAYOR_ES_MEJOR);
        assertEquals(100.0, compPos, DELTA);

        // Meta 0 con valor positivo en Menor es Mejor (ej. bugs)
        double compBugs = KpiCalculationEngine.calculateCompliance(3, 0, KpiMeasurementType.CANTIDAD, KpiPolarity.MENOR_ES_MEJOR);
        assertEquals(0.0, compBugs, DELTA);
    }

    @Test
    public void testNegativeActualClampedToZero() {
        // Un valor negativo registrado no debe producir cumplimiento negativo
        double compliance = KpiCalculationEngine.calculateCompliance(-5, 10, KpiMeasurementType.CANTIDAD, KpiPolarity.MAYOR_ES_MEJOR);
        assertEquals(0.0, compliance, DELTA);
    }

    @Test
    public void testPolarityMayorEsMejor() {
        // Cumplimiento exacto (10 de 10 = 100%)
        double exact = KpiCalculationEngine.calculateCompliance(10, 10, KpiMeasurementType.CANTIDAD, KpiPolarity.MAYOR_ES_MEJOR);
        assertEquals(100.0, exact, DELTA);

        // Cumplimiento parcial (5 de 10 = 50%)
        double half = KpiCalculationEngine.calculateCompliance(5, 10, KpiMeasurementType.CANTIDAD, KpiPolarity.MAYOR_ES_MEJOR);
        assertEquals(50.0, half, DELTA);

        // Sobrecumplimiento positivo (15 de 10 = 150%)
        double over = KpiCalculationEngine.calculateCompliance(15, 10, KpiMeasurementType.CANTIDAD, KpiPolarity.MAYOR_ES_MEJOR);
        assertEquals(150.0, over, DELTA);
    }

    @Test
    public void testPolarityMenorEsMejor() {
        // Meta: máximo 2 errores permitidos. Resultado: 0 errores -> 100%
        double zeroErrors = KpiCalculationEngine.calculateCompliance(0, 2, KpiMeasurementType.CANTIDAD, KpiPolarity.MENOR_ES_MEJOR);
        assertEquals(100.0, zeroErrors, DELTA);

        // Meta: 2 errores. Resultado: 2 errores -> 100%
        double allowedErrors = KpiCalculationEngine.calculateCompliance(2, 2, KpiMeasurementType.CANTIDAD, KpiPolarity.MENOR_ES_MEJOR);
        assertEquals(100.0, allowedErrors, DELTA);

        // Meta: 2 errores. Resultado: 3 errores (1 en exceso sobre 2 = 50% de penalización -> 50% cumplimiento)
        double excessErrors = KpiCalculationEngine.calculateCompliance(3, 2, KpiMeasurementType.CANTIDAD, KpiPolarity.MENOR_ES_MEJOR);
        assertEquals(50.0, excessErrors, DELTA);

        // Meta: 2 errores. Resultado: 4 errores -> 0%
        double maxExcess = KpiCalculationEngine.calculateCompliance(4, 2, KpiMeasurementType.CANTIDAD, KpiPolarity.MENOR_ES_MEJOR);
        assertEquals(0.0, maxExcess, DELTA);
    }

    @Test
    public void testWeightedScoreCalculation() {
        // Cumplimiento 100% con peso 40% = 40.0 pts
        double score1 = KpiCalculationEngine.calculateWeightedScore(100.0, 40.0);
        assertEquals(40.0, score1, DELTA);

        // Cumplimiento 80% con peso 35% = 28.0 pts
        double score2 = KpiCalculationEngine.calculateWeightedScore(80.0, 35.0);
        assertEquals(28.0, score2, DELTA);

        // Peso 0%
        double scoreZero = KpiCalculationEngine.calculateWeightedScore(100.0, 0.0);
        assertEquals(0.0, scoreZero, DELTA);
    }

    @Test
    public void testTotalWeightedScoreSum() {
        double total = KpiCalculationEngine.calculateTotalWeightedScore(Arrays.asList(40.0, 28.0, 25.0));
        assertEquals(93.0, total, DELTA);

        double emptyTotal = KpiCalculationEngine.calculateTotalWeightedScore(Collections.emptyList());
        assertEquals(0.0, emptyTotal, DELTA);
    }

    @Test
    public void testOfficialSigd2026PerformanceLevels() {
        assertEquals("EXCELENTE", KpiCalculationEngine.getLevelFromPercentage(120.0));
        assertEquals("EXCELENTE", KpiCalculationEngine.getLevelFromPercentage(90.0));

        assertEquals("MUY BUENO", KpiCalculationEngine.getLevelFromPercentage(89.9));
        assertEquals("MUY BUENO", KpiCalculationEngine.getLevelFromPercentage(80.0));

        assertEquals("BUENO", KpiCalculationEngine.getLevelFromPercentage(79.9));
        assertEquals("BUENO", KpiCalculationEngine.getLevelFromPercentage(60.0));

        assertEquals("REGULAR", KpiCalculationEngine.getLevelFromPercentage(59.9));
        assertEquals("REGULAR", KpiCalculationEngine.getLevelFromPercentage(40.0));

        assertEquals("DEFICIENTE", KpiCalculationEngine.getLevelFromPercentage(39.9));
        assertEquals("DEFICIENTE", KpiCalculationEngine.getLevelFromPercentage(0.0));
    }
}
