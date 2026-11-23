package pe.net.ws.kpiweb.features.admin.dashboard;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formats real server ranking period keys and filter identifiers into readable Spanish labels. */
final class AdminRankingPeriodFormatter {
    private static final String[] MONTHS_ES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(\\d{4})-(0[1-9]|1[0-2])$");
    private static final Pattern QUARTER_PATTERN = Pattern.compile("^(\\d{4})-[QT]([1-4])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEK_PATTERN = Pattern.compile("^(\\d{4})-[WS](0?[1-9]|[1-4]\\d|5[0-3])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})$");

    private AdminRankingPeriodFormatter() {
    }

    static String formatPeriodKey(@Nullable String key, @Nullable String periodType) {
        if (key == null || key.trim().isEmpty()) {
            String freq = frequencyLabel(periodType).toLowerCase(Locale.ROOT);
            return "Sin cierre " + freq;
        }
        String trimmed = key.trim();
        Matcher monthMatcher = MONTH_PATTERN.matcher(trimmed);
        if (monthMatcher.matches()) {
            String year = monthMatcher.group(1);
            int month = Integer.parseInt(monthMatcher.group(2));
            return MONTHS_ES[month - 1] + " " + year;
        }
        Matcher quarterMatcher = QUARTER_PATTERN.matcher(trimmed);
        if (quarterMatcher.matches()) {
            String year = quarterMatcher.group(1);
            String quarter = quarterMatcher.group(2);
            return quarter + ".º trimestre " + year;
        }
        Matcher weekMatcher = WEEK_PATTERN.matcher(trimmed);
        if (weekMatcher.matches()) {
            String year = weekMatcher.group(1);
            int week = Integer.parseInt(weekMatcher.group(2));
            return "Semana " + week + " · " + year;
        }
        Matcher yearMatcher = YEAR_PATTERN.matcher(trimmed);
        if (yearMatcher.matches()) {
            return "Año " + yearMatcher.group(1);
        }
        return trimmed;
    }

    static String frequencyLabel(@Nullable String periodType) {
        if (periodType == null) return "Mensual";
        switch (periodType.trim().toUpperCase(Locale.ROOT)) {
            case "SEMANAL":
                return "Semanal";
            case "TRIMESTRAL":
                return "Trimestral";
            case "ANUAL":
                return "Anual";
            case "MENSUAL":
            default:
                return "Mensual";
        }
    }

    static String personnelLabel(@Nullable String typeFilter) {
        if ("Colaborador".equalsIgnoreCase(typeFilter)) return "Colaboradores";
        if ("Practicante".equalsIgnoreCase(typeFilter)) return "Practicantes";
        return "Todos";
    }

    static String latestClosedSubtitle(@Nullable String periodType) {
        return "Último cierre " + frequencyLabel(periodType).toLowerCase(Locale.ROOT) + " oficial disponible";
    }
}
