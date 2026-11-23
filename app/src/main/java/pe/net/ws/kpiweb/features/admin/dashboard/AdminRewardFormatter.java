package pe.net.ws.kpiweb.features.admin.dashboard;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure formatting, validation, and local search utilities for reward publications. */
final class AdminRewardFormatter {
    private static final Locale ES_LOCALE = new Locale("es", "PE");
    private static final String[] SHORT_MONTHS = {
            "ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic"
    };
    private static final Pattern BACKEND_DT = Pattern.compile(
            "^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]) ([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)$");

    private AdminRewardFormatter() {
    }

    static String formatTotalBadge(int total) {
        return "Total: " + Math.max(0, total);
    }

    static String formatRewardsCount(int shown, int total, boolean filtered) {
        int safeShown = Math.max(0, shown);
        int safeTotal = Math.max(0, total);
        if (filtered) {
            return safeShown + " de " + safeTotal;
        }
        return safeTotal == 1 ? "1 premio" : safeTotal + " premios";
    }

    static String formatFilterLabel(@Nullable String state) {
        if (state == null) return "Todos";
        switch (state.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVO": return "Activos";
            case "PROGRAMADO": return "Programados";
            case "VENCIDO": return "Vencidos";
            case "BORRADOR": return "Borradores";
            case "DESACTIVADO": return "Desactivados";
            case "TODOS":
            default: return "Todos";
        }
    }

    static String formatStateLabel(@Nullable String state) {
        if (state == null || state.trim().isEmpty()) return "Sin estado";
        switch (state.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVO": return "Activo";
            case "PROGRAMADO": return "Programado";
            case "VENCIDO": return "Vencido";
            case "BORRADOR": return "Borrador";
            case "DESACTIVADO": return "Desactivado";
            default:
                String s = state.trim().toLowerCase(ES_LOCALE);
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    static boolean isValidBackendDateTime(@Nullable String value) {
        return value != null && BACKEND_DT.matcher(value.trim()).matches();
    }

    static boolean isEndAfterStart(@Nullable String start, @Nullable String end) {
        if (!isValidBackendDateTime(start) || !isValidBackendDateTime(end)) return false;
        return end.trim().compareTo(start.trim()) > 0;
    }

    static String formatBackendDateTime(int year, int monthOneBased, int day, int hour, int minute) {
        return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d:00",
                year, monthOneBased, day, hour, minute);
    }

    static String formatDisplayDate(@Nullable String backendDateTime) {
        if (backendDateTime == null || backendDateTime.trim().isEmpty()) return "";
        Matcher m = BACKEND_DT.matcher(backendDateTime.trim());
        if (!m.matches()) return backendDateTime.trim();
        int year = Integer.parseInt(m.group(1));
        int month = Integer.parseInt(m.group(2));
        int day = Integer.parseInt(m.group(3));
        return day + " " + SHORT_MONTHS[month - 1] + " " + year;
    }

    static String formatDisplayTime(@Nullable String backendDateTime) {
        if (backendDateTime == null || backendDateTime.trim().isEmpty()) return "";
        Matcher m = BACKEND_DT.matcher(backendDateTime.trim());
        if (!m.matches()) return "";
        return m.group(4) + ":" + m.group(5);
    }

    static String formatCompactDateTime(@Nullable String backendDateTime) {
        if (backendDateTime == null || backendDateTime.trim().isEmpty()) return "Sin definir";
        String date = formatDisplayDate(backendDateTime);
        String time = formatDisplayTime(backendDateTime);
        return time.isEmpty() ? date : date + " · " + time;
    }

    @Nullable
    static int[] parseDateTimeParts(@Nullable String backendDateTime) {
        if (backendDateTime == null) return null;
        Matcher m = BACKEND_DT.matcher(backendDateTime.trim());
        if (!m.matches()) return null;
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)),
                Integer.parseInt(m.group(4)),
                Integer.parseInt(m.group(5))
        };
    }

    static boolean matchesSearch(@Nullable JsonObject row, @Nullable String rawQuery) {
        if (row == null) return false;
        if (rawQuery == null || rawQuery.trim().isEmpty()) return true;
        String needle = normalize(rawQuery);
        if (needle.isEmpty()) return true;
        String haystack = normalize(safeString(row, "titulo") + " " + safeString(row, "descripcion"));
        return haystack.contains(needle);
    }

    static String normalize(@Nullable String input) {
        if (input == null) return "";
        String decomposed = Normalizer.normalize(input.trim().toLowerCase(ES_LOCALE), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    static String safeString(@Nullable JsonObject row, String key) {
        return row != null && row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : "";
    }

    static int safeInt(@Nullable JsonObject row, String key) {
        try {
            return Integer.parseInt(safeString(row, key));
        } catch (Exception e) {
            return 0;
        }
    }
}
