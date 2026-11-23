package pe.net.ws.kpiweb.features.collaborator.wellness;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lógica pura de formateo y organización del historial real de "Mi evolución de bienestar".
 * No inventa puntuaciones clínicas, porcentajes ni fechas ficticias.
 */
public final class WellnessEvolutionHelper {

    public enum StatusTone {
        COMPLETED,
        IN_PROGRESS,
        PENDING
    }

    public static final class EvaluationRecord {
        public final int evaluationId;
        public final int testId;
        public final String title;
        public final String periodKey;
        public final String periodDisplay;
        public final String rawStatus;
        public final String statusDisplay;
        public final StatusTone statusTone;
        public final String completedAtDisplay;
        public final JsonObject rawRow;

        public EvaluationRecord(int evaluationId,
                                int testId,
                                String title,
                                String periodKey,
                                String periodDisplay,
                                String rawStatus,
                                String statusDisplay,
                                StatusTone statusTone,
                                String completedAtDisplay,
                                JsonObject rawRow) {
            this.evaluationId = evaluationId;
            this.testId = testId;
            this.title = title;
            this.periodKey = periodKey;
            this.periodDisplay = periodDisplay;
            this.rawStatus = rawStatus;
            this.statusDisplay = statusDisplay;
            this.statusTone = statusTone;
            this.completedAtDisplay = completedAtDisplay;
            this.rawRow = rawRow;
        }

        public boolean canOpenDetail() {
            return evaluationId > 0 && rawRow != null;
        }

        public boolean hasCompletedDate() {
            return completedAtDisplay != null && !completedAtDisplay.isEmpty();
        }
    }

    private static final String[] SPANISH_MONTHS = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private WellnessEvolutionHelper() {}

    public static List<EvaluationRecord> parseHistory(JsonArray rows) {
        List<EvaluationRecord> result = new ArrayList<>();
        if (rows == null) return result;
        for (JsonElement element : rows) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            int evalId = getInt(row, "id_evaluacion_bienestar", 0);
            int testId = getInt(row, "id_test", 0);
            String title = getString(row, "titulo");
            if (title.isEmpty()) title = "Evaluación de bienestar";
            String periodKey = getString(row, "periodo_clave");
            String rawStatus = getString(row, "estado");
            String completedRaw = getString(row, "fecha_completada");
            result.add(new EvaluationRecord(
                    evalId,
                    testId,
                    title,
                    periodKey,
                    formatPeriodKey(periodKey),
                    rawStatus,
                    formatStatusLabel(rawStatus),
                    resolveStatusTone(rawStatus),
                    formatCompletedDate(completedRaw),
                    row
            ));
        }
        return result;
    }

    /**
     * Selecciona la evaluación destacada para la tarjeta principal:
     * prioriza la evaluación mensual más reciente (si coincide monthlyTestId),
     * o la evaluación más reciente del historial si no hay mensual.
     */
    public static EvaluationRecord selectFeatured(List<EvaluationRecord> records, int monthlyTestId) {
        if (records == null || records.isEmpty()) return null;
        if (monthlyTestId > 0) {
            for (EvaluationRecord record : records) {
                if (record.testId == monthlyTestId) return record;
            }
        }
        return records.get(0);
    }

    /**
     * Evita mostrar duplicado en la línea de tiempo el registro que ya aparece
     * en la tarjeta destacada superior, conservando todos los demás períodos del historial.
     */
    public static List<EvaluationRecord> excludeFeatured(List<EvaluationRecord> records, EvaluationRecord featured) {
        List<EvaluationRecord> filtered = new ArrayList<>();
        if (records == null) return filtered;
        boolean removedOnce = false;
        for (EvaluationRecord rec : records) {
            if (rec == null) continue;
            if (!removedOnce && featured != null && isSameRecord(rec, featured)) {
                removedOnce = true;
                continue;
            }
            filtered.add(rec);
        }
        return filtered;
    }

    private static boolean isSameRecord(EvaluationRecord a, EvaluationRecord b) {
        if (a == b) return true;
        if (a.evaluationId > 0 && b.evaluationId > 0) {
            return a.evaluationId == b.evaluationId;
        }
        return a.testId == b.testId && a.periodKey.equals(b.periodKey);
    }

    public static String formatPeriodKey(String periodKey) {
        if (periodKey == null || periodKey.trim().isEmpty()) return "Período vigente";
        String clean = periodKey.trim();
        // Formato YYYY-MM
        if (clean.matches("\\d{4}-\\d{2}")) {
            try {
                int year = Integer.parseInt(clean.substring(0, 4));
                int month = Integer.parseInt(clean.substring(5, 7));
                if (month >= 1 && month <= 12) {
                    return SPANISH_MONTHS[month - 1] + " " + year;
                }
            } catch (NumberFormatException ignored) {
                return clean;
            }
        }
        // Formato YYYY-W##
        if (clean.matches("\\d{4}-W\\d{2}")) {
            String year = clean.substring(0, 4);
            String week = clean.substring(6, 8);
            return "Semana " + Integer.parseInt(week) + " · " + year;
        }
        // Formato campaña Ryff C#
        if (clean.matches("C\\d+")) {
            return "Campaña #" + clean.substring(1);
        }
        return clean;
    }

    public static String formatCompletedDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "";
        String clean = rawDate.trim();
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
                parser.setLenient(false);
                Date parsed = parser.parse(clean);
                if (parsed != null) {
                    SimpleDateFormat out = new SimpleDateFormat("d MMM yyyy", new Locale("es", "PE"));
                    return "Realizada el " + out.format(parsed);
                }
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        return "Realizada el " + clean;
    }

    public static String formatStatusLabel(String rawStatus) {
        if (rawStatus == null) return "Registrada";
        switch (rawStatus.trim().toUpperCase(Locale.ROOT)) {
            case "COMPLETADA":
                return "Completada";
            case "EN_PROCESO":
                return "En proceso";
            case "PENDIENTE":
                return "Pendiente";
            default:
                return rawStatus.trim().isEmpty() ? "Registrada" : rawStatus.trim();
        }
    }

    public static StatusTone resolveStatusTone(String rawStatus) {
        if (rawStatus == null) return StatusTone.PENDING;
        switch (rawStatus.trim().toUpperCase(Locale.ROOT)) {
            case "COMPLETADA":
                return StatusTone.COMPLETED;
            case "EN_PROCESO":
                return StatusTone.IN_PROGRESS;
            default:
                return StatusTone.PENDING;
        }
    }

    private static String getString(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString().trim() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
