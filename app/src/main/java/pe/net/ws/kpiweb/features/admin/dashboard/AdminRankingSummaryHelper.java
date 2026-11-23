package pe.net.ws.kpiweb.features.admin.dashboard;

import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

/**
 * Cálculo puro y veraz del resumen y estados del Ranking de desempeño (Administrador).
 * Nunca inventa cantidades ni confunde personal registrado con colaboradores evaluados.
 */
final class AdminRankingSummaryHelper {

    enum EmptyKind {
        NONE,
        NO_CLOSED_PERIOD,
        NO_EVALUATIONS,
        NO_SEARCH_RESULTS,
        NO_FILTER_RESULTS
    }

    static final class SummaryStats {
        final String collaboratorsText;
        final String evaluatedText;
        final String classifiedText;
        final String footerHighlight;
        final boolean hasData;

        SummaryStats(String collaboratorsText,
                     String evaluatedText,
                     String classifiedText,
                     String footerHighlight,
                     boolean hasData) {
            this.collaboratorsText = collaboratorsText;
            this.evaluatedText = evaluatedText;
            this.classifiedText = classifiedText;
            this.footerHighlight = footerHighlight;
            this.hasData = hasData;
        }
    }

    private AdminRankingSummaryHelper() {}

    static SummaryStats computeStats(boolean loaded,
                                     String loadedPeriodKey,
                                     List<JsonObject> visibleRows) {
        if (!loaded || loadedPeriodKey == null || loadedPeriodKey.trim().isEmpty()) {
            return new SummaryStats("—", "—", "—", "", false);
        }
        if (visibleRows == null || visibleRows.isEmpty()) {
            return new SummaryStats("0", "0", "0", "", false);
        }

        int totalParticipants = visibleRows.size();
        int evaluatedCount = 0;
        int classifiedCount = 0;
        int totalApprovedKpis = 0;
        float complianceSum = 0f;

        for (JsonObject row : visibleRows) {
            if (row == null) continue;
            int approved = intField(row, "kpis_aprobados");
            int rank = intField(row, "puesto");
            if (approved > 0) evaluatedCount++;
            if (rank > 0) classifiedCount++;
            totalApprovedKpis += approved;
            complianceSum += floatField(row, "porcentaje_cumplimiento");
        }

        float avg = complianceSum / totalParticipants;
        String footer = String.format(Locale.getDefault(),
                "Promedio de cumplimiento: %.1f%% · %d %s",
                avg,
                totalApprovedKpis,
                totalApprovedKpis == 1 ? "KPI aprobado" : "KPI aprobados");

        return new SummaryStats(
                String.valueOf(totalParticipants),
                String.valueOf(evaluatedCount),
                String.valueOf(classifiedCount),
                footer,
                true
        );
    }

    static EmptyKind resolveEmptyKind(int sourceCount,
                                      int visibleCount,
                                      String loadedPeriodKey,
                                      String query,
                                      boolean hasActiveFilters) {
        if (visibleCount > 0) return EmptyKind.NONE;
        if (sourceCount <= 0) {
            return (loadedPeriodKey == null || loadedPeriodKey.trim().isEmpty())
                    ? EmptyKind.NO_CLOSED_PERIOD
                    : EmptyKind.NO_EVALUATIONS;
        }
        if (query != null && !query.trim().isEmpty()) {
            return EmptyKind.NO_SEARCH_RESULTS;
        }
        return hasActiveFilters ? EmptyKind.NO_FILTER_RESULTS : EmptyKind.NO_EVALUATIONS;
    }

    private static int intField(JsonObject row, String key) {
        try {
            return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static float floatField(JsonObject row, String key) {
        try {
            return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsFloat() : 0f;
        } catch (RuntimeException ignored) {
            return 0f;
        }
    }
}
