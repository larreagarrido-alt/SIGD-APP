package pe.net.ws.kpiweb.features.collaborator.ranking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.junit.Test;

public class CollaboratorProgressSnapshotTest {

    @Test
    public void handlesZeroDataHonestlyWithoutInventingLevelsOrPercentages() {
        JsonObject ownPoints = JsonParser.parseString(
                "{\"id_trabajador\":12,\"saldo_actual\":0,\"movimientos\":[],\"mis_canjes\":[]}"
        ).getAsJsonObject();

        CollaboratorProgressSnapshot snap = CollaboratorProgressSnapshot.fromServerData(
                ownPoints, null, 12, CollaboratorProgressSnapshot.EvalWindow.MONTH, 1795200000000L);

        assertEquals(0, snap.totalPoints);
        assertNull(snap.rankPosition);
        assertNull(snap.topPercentile);
        assertNull(snap.progressPercent);
        assertNull(snap.teamAverageCompliance);
        assertTrue(snap.levels.isEmpty());
        assertFalse(snap.hasPeriodData);
        assertTrue(snap.rankingRows.isEmpty());
    }

    @Test
    public void extractsRealClosedRankingMetricsAndTopPercentile() {
        JsonObject ownPoints = JsonParser.parseString(
                "{\"id_trabajador\":7,\"saldo_actual\":1240,\"movimientos\":["
                        + "{\"puntos\":120,\"creado_en\":\"2026-11-18 10:00:00\"},"
                        + "{\"puntos\":-50,\"creado_en\":\"2026-11-19 10:00:00\"}"
                        + "]}"
        ).getAsJsonObject();

        JsonObject closedRanking = JsonParser.parseString(
                "{\"periodo_clave\":\"2026-11\",\"visibilidad\":\"EMPRESA\",\"id_trabajador\":7,"
                        + "\"resumen\":{\"participantes\":10,\"promedio_cumplimiento\":84.5,\"evaluaciones_completadas\":25},"
                        + "\"ranking\":["
                        + "{\"id_trabajador\":1,\"puesto\":1,\"nombre_completo\":\"Ana Torres\",\"puntos_kpi_periodo\":400,\"porcentaje_cumplimiento\":95,\"kpis_asignados\":4,\"kpis_aprobados\":4,\"medalla\":\"ORO\"},"
                        + "{\"id_trabajador\":7,\"puesto\":2,\"nombre_completo\":\"Luz Castillo\",\"puntos_kpi_periodo\":320,\"porcentaje_cumplimiento\":88,\"kpis_asignados\":5,\"kpis_aprobados\":4,\"medalla\":\"PLATA\"}"
                        + "]}"
        ).getAsJsonObject();

        long nov20 = 1795132800000L; // 2026-11-20 UTC approx
        CollaboratorProgressSnapshot snap = CollaboratorProgressSnapshot.fromServerData(
                ownPoints, closedRanking, 7, CollaboratorProgressSnapshot.EvalWindow.MONTH, nov20);

        assertEquals(1240, snap.totalPoints);
        assertEquals(Integer.valueOf(2), snap.rankPosition);
        assertEquals(10, snap.totalParticipants);
        assertEquals(Integer.valueOf(20), snap.topPercentile);
        assertEquals("PLATA", snap.medal);
        assertEquals(Integer.valueOf(88), snap.progressPercent);
        assertEquals(4, snap.activitiesCompleted);
        assertEquals(120, snap.pointsEarnedInPeriod);
        assertEquals(84.5, snap.teamAverageCompliance, 0.01);
        assertEquals(2, snap.rankingRows.size());
        assertTrue(snap.rankingRows.get(1).currentUser);
        assertEquals("LC", snap.rankingRows.get(1).initials);
    }

    @Test
    public void parsesFutureServerLevelsCatalogWhenProvided() {
        JsonObject ownPoints = JsonParser.parseString(
                "{\"id_trabajador\":5,\"saldo_actual\":650,\"movimientos\":[],"
                        + "\"niveles\":["
                        + "{\"nivel\":1,\"nombre\":\"Inicio\",\"puntos_requeridos\":0},"
                        + "{\"nivel\":2,\"nombre\":\"Constante\",\"puntos_requeridos\":500},"
                        + "{\"nivel\":3,\"nombre\":\"Destacado\",\"puntos_requeridos\":1000}"
                        + "]}"
        ).getAsJsonObject();

        CollaboratorProgressSnapshot snap = CollaboratorProgressSnapshot.fromServerData(
                ownPoints, null, 5, CollaboratorProgressSnapshot.EvalWindow.WEEK, System.currentTimeMillis());

        assertEquals(3, snap.levels.size());
        assertEquals(2, snap.currentLevel.number);
        assertEquals("Constante", snap.currentLevel.name);
        assertEquals(3, snap.nextLevel.number);
        assertTrue(snap.levels.get(0).completed);
        assertTrue(snap.levels.get(1).current);
        assertFalse(snap.levels.get(2).completed);
    }
}
