package pe.net.ws.kpiweb.features.collaborator.ranking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Construye una vista estructurada y honesta del progreso real del colaborador
 * a partir de las respuestas oficiales de {@code /premios/mis-puntos} y {@code /ranking}.
 * Nunca inventa niveles, umbrales, porcentajes sin denominador ni puestos ficticios.
 */
final class CollaboratorProgressSnapshot {

    enum EvalWindow {
        WEEK,
        MONTH
    }

    static final class LevelNode {
        final int number;
        final String name;
        final int requiredPoints;
        final boolean completed;
        final boolean current;

        LevelNode(int number, String name, int requiredPoints, boolean completed, boolean current) {
            this.number = number;
            this.name = name;
            this.requiredPoints = requiredPoints;
            this.completed = completed;
            this.current = current;
        }
    }

    static final class RankingRow {
        final int workerId;
        final int position;
        final String fullName;
        final String initials;
        final String roleOrArea;
        final String medal;
        final int periodPoints;
        final boolean currentUser;

        RankingRow(int workerId, int position, String fullName, String initials,
                   String roleOrArea, String medal, int periodPoints, boolean currentUser) {
            this.workerId = workerId;
            this.position = position;
            this.fullName = fullName;
            this.initials = initials;
            this.roleOrArea = roleOrArea;
            this.medal = medal;
            this.periodPoints = periodPoints;
            this.currentUser = currentUser;
        }
    }

    final int totalPoints;
    final Integer rankPosition;
    final int totalParticipants;
    final Integer topPercentile;
    final String medal;
    final String closedPeriodKey;
    final String visibilityScope;

    // Niveles reales si el backend los entrega (vacío cuando aún no existe catálogo)
    final List<LevelNode> levels;
    final LevelNode currentLevel;
    final LevelNode nextLevel;

    // Métricas de evaluación del período seleccionado (Semana / Mes)
    final EvalWindow evalWindow;
    final Integer progressPercent;
    final int activitiesCompleted;
    final int pointsEarnedInPeriod;
    final Double teamAverageCompliance;
    final boolean hasPeriodData;

    // Ranking cerrado permitido por el servidor
    final List<RankingRow> rankingRows;

    private CollaboratorProgressSnapshot(
            int totalPoints,
            Integer rankPosition,
            int totalParticipants,
            Integer topPercentile,
            String medal,
            String closedPeriodKey,
            String visibilityScope,
            List<LevelNode> levels,
            LevelNode currentLevel,
            LevelNode nextLevel,
            EvalWindow evalWindow,
            Integer progressPercent,
            int activitiesCompleted,
            int pointsEarnedInPeriod,
            Double teamAverageCompliance,
            boolean hasPeriodData,
            List<RankingRow> rankingRows
    ) {
        this.totalPoints = totalPoints;
        this.rankPosition = rankPosition;
        this.totalParticipants = totalParticipants;
        this.topPercentile = topPercentile;
        this.medal = medal;
        this.closedPeriodKey = closedPeriodKey;
        this.visibilityScope = visibilityScope;
        this.levels = levels;
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.evalWindow = evalWindow;
        this.progressPercent = progressPercent;
        this.activitiesCompleted = activitiesCompleted;
        this.pointsEarnedInPeriod = pointsEarnedInPeriod;
        this.teamAverageCompliance = teamAverageCompliance;
        this.hasPeriodData = hasPeriodData;
        this.rankingRows = rankingRows;
    }

    static CollaboratorProgressSnapshot fromServerData(
            JsonObject ownPoints,
            JsonObject closedRanking,
            int currentWorkerId,
            EvalWindow window,
            long nowMillis
    ) {
        int balance = intOrZero(ownPoints, "saldo_actual");

        // 1. Parsear movimientos reales dentro de la ventana de tiempo elegida
        long windowMs = window == EvalWindow.WEEK ? 7L * 86400000L : 31L * 86400000L;
        int earnedInWindow = 0;
        int positiveMovementsInWindow = 0;
        JsonArray movements = ownPoints != null && ownPoints.has("movimientos") && ownPoints.get("movimientos").isJsonArray()
                ? ownPoints.getAsJsonArray("movimientos") : new JsonArray();
        for (JsonElement element : movements) {
            if (!element.isJsonObject()) continue;
            JsonObject m = element.getAsJsonObject();
            long ts = parseTimestamp(stringOrEmpty(m, "creado_en"));
            if (ts > 0 && ts >= nowMillis - windowMs && ts <= nowMillis) {
                int delta = intOrZero(m, "puntos");
                if (delta > 0) {
                    earnedInWindow += delta;
                    positiveMovementsInWindow++;
                }
            }
        }

        // 2. Parsear ranking cerrado oficial si existe
        String periodKey = "";
        String visibility = "";
        int participants = 0;
        Double teamAvg = null;
        Integer ownRank = null;
        String ownMedal = "";
        Integer ownCompliancePct = null;
        int ownApprovedKpis = 0;
        int ownAssignedKpis = 0;
        int ownPeriodKpiPoints = 0;
        List<RankingRow> rows = new ArrayList<>();

        if (closedRanking != null) {
            periodKey = stringOrEmpty(closedRanking, "periodo_clave");
            visibility = stringOrEmpty(closedRanking, "visibilidad");
            if (closedRanking.has("resumen") && closedRanking.get("resumen").isJsonObject()) {
                JsonObject resumen = closedRanking.getAsJsonObject("resumen");
                participants = intOrZero(resumen, "participantes");
                teamAvg = doubleOrNull(resumen, "promedio_cumplimiento");
            }
            JsonArray arr = closedRanking.has("ranking") && closedRanking.get("ranking").isJsonArray()
                    ? closedRanking.getAsJsonArray("ranking") : new JsonArray();
            if (participants <= 0) {
                participants = arr.size();
            }
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject row = el.getAsJsonObject();
                int wid = intOrZero(row, "id_trabajador");
                int pos = intOrZero(row, "puesto");
                String fullName = stringOrEmpty(row, "nombre_completo");
                if (fullName.isEmpty()) fullName = "Colaborador";
                String cargo = stringOrEmpty(row, "cargo_nombre");
                String agency = stringOrEmpty(row, "agencia_nombre");
                String sub = !cargo.isEmpty() ? cargo : agency;
                String rowMedal = stringOrEmpty(row, "medalla");
                int pts = intOrZero(row, "puntos_kpi_periodo");
                boolean isSelf = wid == currentWorkerId;
                rows.add(new RankingRow(wid, pos, fullName, initialsOf(fullName), sub, rowMedal, pts, isSelf));
                if (isSelf) {
                    if (pos > 0) ownRank = pos;
                    ownMedal = rowMedal;
                    Double compliance = doubleOrNull(row, "porcentaje_cumplimiento");
                    if (compliance != null) {
                        ownCompliancePct = Math.max(0, Math.min(100, (int) Math.round(compliance)));
                    }
                    ownApprovedKpis = intOrZero(row, "kpis_aprobados");
                    ownAssignedKpis = intOrZero(row, "kpis_asignados");
                    ownPeriodKpiPoints = pts;
                }
            }
        }

        // Percentil solo cuando existen puesto y al menos 2 participantes reales
        Integer topPct = null;
        if (ownRank != null && ownRank > 0 && participants >= 2) {
            topPct = Math.max(1, Math.min(100, Math.round(ownRank * 100f / participants)));
        }

        // 3. Parsear catálogo real de niveles solo si el servidor lo proporciona
        List<LevelNode> parsedLevels = parseLevels(ownPoints, balance);
        LevelNode currentLvl = null;
        LevelNode nextLvl = null;
        for (int i = 0; i < parsedLevels.size(); i++) {
            LevelNode node = parsedLevels.get(i);
            if (node.current) {
                currentLvl = node;
                if (i + 1 < parsedLevels.size()) {
                    nextLvl = parsedLevels.get(i + 1);
                }
                break;
            }
        }

        // 4. Métricas del bloque "Evaluación de puntos" sin inventar porcentajes
        Integer progressPct = null;
        int activities = positiveMovementsInWindow;
        int earnedPoints = earnedInWindow;
        boolean hasData = positiveMovementsInWindow > 0 || earnedInWindow > 0;

        if (window == EvalWindow.MONTH && ownRank != null) {
            if (ownCompliancePct != null && ownAssignedKpis > 0) {
                progressPct = ownCompliancePct;
            }
            if (ownApprovedKpis > 0) {
                activities = ownApprovedKpis;
                hasData = true;
            }
            if (earnedPoints == 0 && ownPeriodKpiPoints > 0) {
                earnedPoints = ownPeriodKpiPoints;
                hasData = true;
            }
            if (progressPct != null) {
                hasData = true;
            }
        }

        return new CollaboratorProgressSnapshot(
                balance,
                ownRank,
                participants,
                topPct,
                ownMedal,
                periodKey,
                visibility,
                Collections.unmodifiableList(parsedLevels),
                currentLvl,
                nextLvl,
                window,
                progressPct,
                activities,
                earnedPoints,
                window == EvalWindow.MONTH ? teamAvg : null,
                hasData,
                Collections.unmodifiableList(rows)
        );
    }

    /**
     * Selecciona hasta 5 filas relevantes centradas en el usuario actual para la vista compacta,
     * o todas las filas cuando el usuario presiona "Ver ranking".
     */
    List<RankingRow> visibleRankingRows(boolean expanded) {
        if (expanded || rankingRows.size() <= 5) {
            return rankingRows;
        }
        int selfIndex = -1;
        for (int i = 0; i < rankingRows.size(); i++) {
            if (rankingRows.get(i).currentUser) {
                selfIndex = i;
                break;
            }
        }
        if (selfIndex < 0 || selfIndex < 3) {
            return rankingRows.subList(0, 5);
        }
        int start = Math.max(0, Math.min(selfIndex - 2, rankingRows.size() - 5));
        return rankingRows.subList(start, Math.min(rankingRows.size(), start + 5));
    }

    static String formatPoints(int value) {
        return NumberFormat.getIntegerInstance(new Locale("es", "PE")).format(value);
    }

    static String formatPeriodMonthLabel(String periodKey) {
        if (periodKey == null || !periodKey.matches("\\d{4}-\\d{2}")) {
            return "";
        }
        try {
            Date d = new SimpleDateFormat("yyyy-MM", Locale.US).parse(periodKey);
            if (d == null) return periodKey;
            String raw = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES")).format(d);
            return raw.isEmpty() ? periodKey : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        } catch (Exception ignored) {
            return periodKey;
        }
    }

    static String initialsOf(String name) {
        if (name == null) return "?";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(new Locale("es", "ES"));
        }
        return parts[0].substring(0, 1).toUpperCase(new Locale("es", "ES"));
    }

    private static List<LevelNode> parseLevels(JsonObject ownPoints, int balance) {
        if (ownPoints == null || !ownPoints.has("niveles") || !ownPoints.get("niveles").isJsonArray()) {
            return Collections.emptyList();
        }
        JsonArray arr = ownPoints.getAsJsonArray("niveles");
        List<LevelNode> raw = new ArrayList<>();
        int highestReachedIdx = -1;
        for (int i = 0; i < arr.size(); i++) {
            JsonElement el = arr.get(i);
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            int num = intOrZero(obj, "nivel");
            if (num <= 0) num = i + 1;
            String name = stringOrEmpty(obj, "nombre");
            if (name.isEmpty()) name = "Nivel " + num;
            int req = intOrZero(obj, "puntos_requeridos");
            if (balance >= req) {
                highestReachedIdx = raw.size();
            }
            raw.add(new LevelNode(num, name, req, false, false));
        }
        List<LevelNode> resolved = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            LevelNode n = raw.get(i);
            boolean isCurrent = i == highestReachedIdx;
            boolean isCompleted = highestReachedIdx >= 0 && i <= highestReachedIdx;
            resolved.add(new LevelNode(n.number, n.name, n.requiredPoints, isCompleted, isCurrent));
        }
        return resolved;
    }

    private static long parseTimestamp(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).parse(raw);
            return parsed == null ? 0 : parsed.getTime();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int intOrZero(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return 0;
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static Double doubleOrNull(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String stringOrEmpty(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
        try {
            return object.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
