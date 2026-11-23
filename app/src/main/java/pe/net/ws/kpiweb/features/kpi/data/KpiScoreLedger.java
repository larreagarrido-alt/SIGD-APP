package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import pe.net.ws.kpiweb.features.kpi.engine.KpiCalculationEngine;

/**
 * Gestor centralizado y acumulador de puntuaciones laborales (Ledger).
 * Aplica las reglas oficiales del SIGD 2026:
 * - Excelente (>=90%): 20 puntos
 * - Muy Bueno (>=80%): 17 puntos
 * - Bueno     (>=60%): 14 puntos
 * - Regular   (>=40%): 12 puntos
 * - Deficiente (<40%): 10 puntos
 * 
 * Protegido contra duplicación: Una jornada diaria solo otorga puntos una sola vez por colaborador.
 */
public class KpiScoreLedger implements KpiScoreProvider {

    private static volatile KpiScoreLedger instance;
    private final Context appContext;

    private KpiScoreLedger(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized KpiScoreLedger getInstance(Context context) {
        if (instance == null) {
            instance = new KpiScoreLedger(context);
        }
        return instance;
    }

    @Override
    public int getTotalPointsForCollaborator(String employeeId) {
        List<DayScore> history = getScoreHistory(employeeId);
        int total = 0;
        for (DayScore s : history) {
            total += s.pointsEarned;
        }
        return total;
    }

    @Override
    public int getWeeklyPoints(String employeeId) {
        List<DayScore> history = getScoreHistory(employeeId);
        int weekly = 0;
        int limit = Math.min(6, history.size()); // Últimos 6 días hábiles
        for (int i = 0; i < limit; i++) {
            weekly += history.get(i).pointsEarned;
        }
        return weekly;
    }

    @Override
    public List<DayScore> getScoreHistory(String employeeId) {
        List<DayScore> result = new ArrayList<>();
        List<KpiRecordEntry> allEntries = KpiCustomRepository.getInstance(appContext).getAllRecordEntries();

        // Agrupar por Fecha + Puesto para no duplicar puntos
        Map<String, List<KpiRecordEntry>> dayGroups = new HashMap<>();
        for (KpiRecordEntry e : allEntries) {
            if (employeeId == null || employeeId.isEmpty() || employeeId.equalsIgnoreCase(e.getEmployeeId()) || "COLAB-ACTUAL".equalsIgnoreCase(e.getEmployeeId())) {
                String groupKey = e.getDate() + "_" + e.getRole();
                if (!dayGroups.containsKey(groupKey)) {
                    dayGroups.put(groupKey, new ArrayList<>());
                }
                dayGroups.get(groupKey).add(e);
            }
        }

        Set<String> processedKeys = new HashSet<>();
        for (Map.Entry<String, List<KpiRecordEntry>> group : dayGroups.entrySet()) {
            if (processedKeys.contains(group.getKey())) continue;
            processedKeys.add(group.getKey());

            List<KpiRecordEntry> list = group.getValue();
            if (list.isEmpty()) continue;

            String date = list.get(0).getDate();
            String role = list.get(0).getRole();

            // Si fue jornada en campo justificada: 20 puntos automáticos
            boolean anyFieldWork = false;
            double sumWeighted = 0.0;
            for (KpiRecordEntry e : list) {
                if (e.isFieldWork()) anyFieldWork = true;
                sumWeighted += e.getComputedWeightedScore();
            }

            int points;
            String level;
            double finalCompliance;

            if (anyFieldWork) {
                finalCompliance = 100.0;
                points = 20;
                level = "EXCELENTE";
            } else {
                finalCompliance = Math.round(sumWeighted * 100.0) / 100.0;
                level = KpiCalculationEngine.getLevelFromPercentage(finalCompliance);
                points = mapLevelToPoints(level);
            }

            result.add(new DayScore(date, role, points, finalCompliance, level));
        }

        return result;
    }

    public static int mapLevelToPoints(String level) {
        if (level == null) return 10;
        switch (level) {
            case "EXCELENTE":
                return 20;
            case "MUY BUENO":
                return 17;
            case "BUENO":
                return 14;
            case "REGULAR":
                return 12;
            case "DEFICIENTE":
            default:
                return 10;
        }
    }
}
