package pe.net.ws.kpiweb.features.kpi.model;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Local explanation of the same day/category rule used by the official server scorer. */
public final class KpiProgrammerDailyScore {
    private static final String[] TYPES = {"Desarrollo", "Corrección", "Mejora",
            "Maquetación", "Integración / API", "Mantenimiento Web"};

    private KpiProgrammerDailyScore() { }

    public static Result calculate(int dayIndex, int goal, List<KpiActivityItem> activities) {
        if (dayIndex < 0 || dayIndex >= TYPES.length || goal <= 0)
            return new Result("", 0, 0, 0);
        int matching = 0, other = 0;
        if (activities != null) for (KpiActivityItem item : activities) {
            if (item == null || !item.isEntregado()) continue;
            if (matches(dayIndex, item.getTipo())) matching++;
            else other++;
        }
        return new Result(TYPES[dayIndex], goal, matching, other);
    }

    private static boolean matches(int dayIndex, String type) {
        String value = normalize(type);
        String required = normalize(TYPES[dayIndex]);
        return value.equals(required)
                || dayIndex == 3 && value.contains("maquetaci")
                || dayIndex == 4 && value.contains("api")
                || dayIndex == 5 && value.contains("mantenim");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.trim().toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static final class Result {
        public final String requiredType;
        public final int goal;
        public final int matching;
        public final int other;

        Result(String requiredType, int goal, int matching, int other) {
            this.requiredType = requiredType;
            this.goal = goal;
            this.matching = matching;
            this.other = other;
        }

        public int percent() {
            return goal <= 0 ? 0 : Math.min(150,
                    (int) Math.round(matching * 100.0 / goal));
        }
    }
}
