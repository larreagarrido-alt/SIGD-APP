package pe.net.ws.kpiweb.features.admin.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.text.Normalizer;
import java.util.Locale;

/** Pure formatting and local search matching helpers for the Official KPI Management screen. */
final class AdminKpiOfficialFormatter {
    private static final Locale ES_LOCALE = new Locale("es", "PE");

    private AdminKpiOfficialFormatter() {
    }

    static String formatFormType(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Área sin especificar";
        String cleaned = raw.trim().replace('_', ' ').replaceAll("\\s+", " ");
        String lower = cleaned.toLowerCase(ES_LOCALE);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    static String formatWorkerLabel(@Nullable String workerName, int workerId) {
        if (workerName != null && !workerName.trim().isEmpty()) {
            return workerName.trim();
        }
        return workerId > 0 ? "Trabajador " + workerId : "Trabajador sin ID";
    }

    static String formatEvaluationsCount(int count) {
        int safe = Math.max(0, count);
        return safe == 1 ? "1 evaluación" : safe + " evaluaciones";
    }

    static boolean matchesReportQuery(@Nullable JsonObject row, @Nullable String rawQuery) {
        if (row == null) return false;
        if (rawQuery == null || rawQuery.trim().isEmpty()) return true;
        String needle = normalize(rawQuery);
        if (needle.isEmpty()) return true;

        int id = safeInt(row, "id_detalle");
        int workerId = safeInt(row, "id_trabajador");
        int evals = safeInt(row, "evaluaciones_vinculadas");
        String formType = safeString(row, "tipo_formulario");
        String workerName = safeString(row, "nombre_completo");

        String searchable = normalize(
                "reporte #" + id + " reporte " + id + " #" + id + " " + id + " "
                        + formType + " " + formatFormType(formType) + " "
                        + workerName + " trabajador " + workerId + " " + workerId + " "
                        + formatEvaluationsCount(evals));
        return searchable.contains(needle);
    }

    static String normalize(@Nullable String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value.trim().toLowerCase(ES_LOCALE), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    static String safeString(@Nullable JsonObject row, String key) {
        return row != null && row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : "";
    }

    static int safeInt(@Nullable JsonObject row, String key) {
        try {
            return row != null && row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
