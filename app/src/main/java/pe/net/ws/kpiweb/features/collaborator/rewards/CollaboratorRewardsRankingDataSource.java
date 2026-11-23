package pe.net.ws.kpiweb.features.collaborator.rewards;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;

/**
 * Fuente única de verdad (Single Source of Truth) para Puntos, Rangos,
 * Reconocimientos Institucionales, Historial de Puntos y Ranking Competitivo
 * del módulo Colaborador de WEBSITE.
 *
 * Reglas de negocio obligatorias:
 * 1. Coherencia 100% idéntica entre Dashboard, Premios y Beneficios, y Ranking.
 * 2. LOS PUNTOS NO SE CANJEAN. No existe gasto, descuento ni compra con puntos.
 * 3. Las evaluaciones de Bienestar jamás afectan puntos, premios ni ranking.
 */
public final class CollaboratorRewardsRankingDataSource {

    public enum PeriodFilter {
        CURRENT_MONTH("Octubre 2026 (Mes actual)"),
        CURRENT_WEEK("Semana actual (23–29 Oct)");

        private final String displayLabel;

        PeriodFilter(String displayLabel) {
            this.displayLabel = displayLabel;
        }

        public String getDisplayLabel() {
            return displayLabel;
        }
    }

    public enum RecognitionStatus {
        ACHIEVED("Alcanzado"),
        AVAILABLE("Disponible como reconocimiento"),
        PENDING("Pendiente");

        private final String label;

        RecognitionStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static final class CollaboratorScoreSnapshot {
        public final String collabId;
        public final String fullName;
        public final String roleAndArea;
        public final String initials;
        public final int totalPoints;
        public final int rankPosition;
        public final String tierName;
        public final String shortTierName;
        public final int nextTierTargetPoints;
        public final String nextTierName;
        public final int pointsNeededForNextTier;
        public final int tierProgressPercent;
        public final String periodVariationText;
        public final String rankVariationText;
        public final String pointsDiffToPrecedingRank;
        public final int climbToPrecedingPercent;
        public final String comparisonSummaryText;
        public final int weeklyGainedPoints;
        public final String weeklyRankVariationText;
        public final String latestAchievementSummary;

        public CollaboratorScoreSnapshot(
                String collabId,
                String fullName,
                String roleAndArea,
                String initials,
                int totalPoints,
                int rankPosition,
                String tierName,
                String shortTierName,
                int nextTierTargetPoints,
                String nextTierName,
                int pointsNeededForNextTier,
                int tierProgressPercent,
                String periodVariationText,
                String rankVariationText,
                String pointsDiffToPrecedingRank,
                int climbToPrecedingPercent,
                String comparisonSummaryText,
                int weeklyGainedPoints,
                String weeklyRankVariationText,
                String latestAchievementSummary) {
            this.collabId = collabId;
            this.fullName = fullName;
            this.roleAndArea = roleAndArea;
            this.initials = initials;
            this.totalPoints = totalPoints;
            this.rankPosition = rankPosition;
            this.tierName = tierName;
            this.shortTierName = shortTierName;
            this.nextTierTargetPoints = nextTierTargetPoints;
            this.nextTierName = nextTierName;
            this.pointsNeededForNextTier = pointsNeededForNextTier;
            this.tierProgressPercent = tierProgressPercent;
            this.periodVariationText = periodVariationText;
            this.rankVariationText = rankVariationText;
            this.pointsDiffToPrecedingRank = pointsDiffToPrecedingRank;
            this.climbToPrecedingPercent = climbToPrecedingPercent;
            this.comparisonSummaryText = comparisonSummaryText;
            this.weeklyGainedPoints = weeklyGainedPoints;
            this.weeklyRankVariationText = weeklyRankVariationText;
            this.latestAchievementSummary = latestAchievementSummary;
        }

        public String getFormattedTotalPoints() {
            return formatNumber(totalPoints) + " pts";
        }

        public String getFormattedTotalPointsNumberOnly() {
            return formatNumber(totalPoints);
        }
    }

    public static final class RecognitionCardItem {
        public final String id;
        public final String title;
        public final String description;
        public final String conditionsRequired;
        public final int requiredPoints;
        public final String requiredTier;
        public final int illustrationRes;
        public final RecognitionStatus status;
        public final String recognitionDateText;

        public RecognitionCardItem(
                String id,
                String title,
                String description,
                String conditionsRequired,
                int requiredPoints,
                String requiredTier,
                int illustrationRes,
                RecognitionStatus status,
                String recognitionDateText) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.conditionsRequired = conditionsRequired;
            this.requiredPoints = requiredPoints;
            this.requiredTier = requiredTier;
            this.illustrationRes = illustrationRes;
            this.status = status;
            this.recognitionDateText = recognitionDateText;
        }
    }

    public static final class PointsHistoryEntry {
        public final String dateText;
        public final String reasonTitle;
        public final String kpiContextSubtitle;
        public final int pointsAdded;
        public final int accumulatedTotalAfterOp;

        public PointsHistoryEntry(
                String dateText,
                String reasonTitle,
                String kpiContextSubtitle,
                int pointsAdded,
                int accumulatedTotalAfterOp) {
            this.dateText = dateText;
            this.reasonTitle = reasonTitle;
            this.kpiContextSubtitle = kpiContextSubtitle;
            this.pointsAdded = pointsAdded;
            this.accumulatedTotalAfterOp = accumulatedTotalAfterOp;
        }
    }

    public static final class LeaderboardEntry {
        public final int rank;
        public final String collabId;
        public final String fullName;
        public final String initials;
        public final String departmentAndTier;
        public final int totalPoints;
        public final int weeklyPoints;
        public final String trendBadge;

        public LeaderboardEntry(
                int rank,
                String collabId,
                String fullName,
                String initials,
                String departmentAndTier,
                int totalPoints,
                int weeklyPoints,
                String trendBadge) {
            this.rank = rank;
            this.collabId = collabId;
            this.fullName = fullName;
            this.initials = initials;
            this.departmentAndTier = departmentAndTier;
            this.totalPoints = totalPoints;
            this.weeklyPoints = weeklyPoints;
            this.trendBadge = trendBadge;
        }
    }

    private CollaboratorRewardsRankingDataSource() {
    }

    public static String formatNumber(int value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    public static String resolveTierName(int points) {
        if (points >= 1500) return "Rango Platino";
        if (points >= 1000) return "Rango Oro";
        if (points >= 500) return "Rango Plata";
        return "Rango Bronce";
    }

    public static String resolveShortTierName(int points) {
        if (points >= 1500) return "Platino";
        if (points >= 1000) return "Oro";
        if (points >= 500) return "Plata";
        return "Bronce";
    }

    public static CollaboratorScoreSnapshot getSnapshotForCollaborator(String collabId) {
        if ("collab_02".equals(collabId)) {
            // Carlos Mendoza: 1,620 pts | #1 | Rango Platino
            return new CollaboratorScoreSnapshot(
                    "collab_02",
                    "Carlos Mendoza",
                    "Senior Sales Executive • Área Comercial",
                    "CM",
                    1620,
                    1,
                    "Rango Platino",
                    "Platino",
                    1500,
                    "Rango Platino Consolidado",
                    0,
                    100,
                    "↑ +240 pts frente al período anterior (Septiembre 2026)",
                    "Líder #1 por 2 semanas consecutivas",
                    "Ventaja de +295 pts sobre el puesto #2 (Valeria Ríos)",
                    100,
                    "Ocupas el puesto #1 del ranking general • Cumplimiento comercial: 84%",
                    240,
                    "↑ #1 en la semana actual (+240 pts)",
                    "Cierre corporativo anual TechCorp (+160 pts)"
            );
        } else if ("collab_03".equals(collabId)) {
            // Elena Rojas: 890 pts | #6 | Rango Plata
            return new CollaboratorScoreSnapshot(
                    "collab_03",
                    "Elena Rojas",
                    "Customer Success Specialist • Atención al Cliente",
                    "ER",
                    890,
                    6,
                    "Rango Plata",
                    "Plata",
                    1000,
                    "Rango Oro (1,000 pts)",
                    110,
                    89,
                    "↑ +110 pts frente al período anterior (Septiembre 2026)",
                    "↑ Subiste +1 posición frente al período anterior",
                    "A 90 pts del puesto #5 (Sofía Alarcón • 980 pts)",
                    82,
                    "Diferencia frente al puesto #5: 90 pts • SLA de atención resuelto: 94%",
                    110,
                    "↑ +1 posición esta semana (+110 pts)",
                    "Resolución de Ticket Crítico Nivel 3 en 8 min (+110 pts)"
            );
        } else {
            // Lucía Gómez: 1,240 pts | #3 | Rango Oro
            return new CollaboratorScoreSnapshot(
                    "collab_01",
                    "Lucía Gómez",
                    "Product Designer • Diseño y Producto",
                    "LG",
                    1240,
                    3,
                    "Rango Oro",
                    "Oro",
                    1500,
                    "Rango Platino (1,500 pts)",
                    260,
                    83,
                    "↑ +180 pts frente al período anterior (Septiembre 2026)",
                    "↑ Subiste +2 posiciones frente al período anterior",
                    "A 85 pts del puesto #2 (Valeria Ríos • 1,325 pts)",
                    88,
                    "Diferencia frente al puesto #2: 85 pts • Frente al puesto #1: 380 pts",
                    180,
                    "↑ +2 posiciones esta semana (+180 pts)",
                    "Entrega puntual: Prototipo de onboarding móvil (+120 pts)"
            );
        }
    }

    /**
     * Lista de los 5 reconocimientos institucionales con ilustraciones vectoriales.
     * Los estados (Alcanzado, Disponible como reconocimiento, Pendiente) y fechas
     * se calculan honestamente según el puntaje acumulado real del colaborador.
     */
    public static List<RecognitionCardItem> getRecognitionsForCollaborator(String collabId) {
        CollaboratorScoreSnapshot snap = getSnapshotForCollaborator(collabId);
        int pts = snap.totalPoints;

        List<RecognitionCardItem> items = new ArrayList<>();

        // 1. Reconocimiento por constancia (500 pts - Rango Plata)
        items.add(new RecognitionCardItem(
                "rec_constancy",
                "Reconocimiento por constancia",
                "Distinción otorgada por mantener entregas continuas y puntualidad sostenida en las actividades operativas del área.",
                "Condición: Alcanzar Rango Plata (500+ pts) y mantener asistencia ≥ 90% en el período.",
                500,
                "Rango Plata • 500 pts",
                R.drawable.collab_ill_reward_constancy,
                RecognitionStatus.ACHIEVED,
                "Reconocido el 05 Oct 2026"
        ));

        // 2. Premio al compromiso (850 pts - Rango Plata)
        RecognitionStatus commitmentStatus = "collab_03".equals(collabId)
                ? RecognitionStatus.AVAILABLE
                : RecognitionStatus.ACHIEVED;
        String commitmentDate = commitmentStatus == RecognitionStatus.ACHIEVED
                ? "Reconocido el 12 Oct 2026"
                : "Requisito cumplido (890 / 850 pts) • Disponible para emisión";

        items.add(new RecognitionCardItem(
                "rec_commitment",
                "Premio al compromiso",
                "Reconocimiento institucional al involucramiento proactivo con las metas del equipo y calidad en cada entregable validado.",
                "Condición: Acumular 850+ pts y registrar cumplimiento activo en los indicadores del trimestre.",
                850,
                "Rango Plata • 850 pts",
                R.drawable.collab_ill_reward_commitment,
                commitmentStatus,
                commitmentDate
        ));

        // 3. Reconocimiento por cumplimiento de objetivos (1,000 pts - Rango Oro)
        RecognitionStatus goalsStatus;
        String goalsDate;
        if (pts >= 1000) {
            goalsStatus = RecognitionStatus.ACHIEVED;
            goalsDate = "Reconocido el 18 Oct 2026";
        } else {
            goalsStatus = RecognitionStatus.PENDING;
            goalsDate = "Te faltan " + formatNumber(1000 - pts) + " pts para alcanzar este reconocimiento";
        }

        items.add(new RecognitionCardItem(
                "rec_goals",
                "Reconocimiento por cumplimiento de objetivos",
                "Certificación interna por superar las metas de KPI asignadas en el período con alto estándar técnico y operativo.",
                "Condición: Alcanzar Rango Oro (1,000+ pts) y superar el 85% de avance en indicadores clave.",
                1000,
                "Rango Oro • 1,000 pts",
                R.drawable.collab_ill_reward_goals,
                goalsStatus,
                goalsDate
        ));

        // 4. Colaborador destacado del mes (1,200 pts - Rango Oro)
        RecognitionStatus monthStarStatus;
        String monthStarDate;
        if ("collab_02".equals(collabId)) {
            monthStarStatus = RecognitionStatus.ACHIEVED;
            monthStarDate = "Reconocido el 24 Oct 2026";
        } else if (pts >= 1200) {
            monthStarStatus = RecognitionStatus.AVAILABLE;
            monthStarDate = "Puntaje alcanzado (1,240 / 1,200 pts) • Disponible en cierre mensual";
        } else {
            monthStarStatus = RecognitionStatus.PENDING;
            monthStarDate = "Te faltan " + formatNumber(1200 - pts) + " pts para habilitar este reconocimiento";
        }

        items.add(new RecognitionCardItem(
                "rec_month_star",
                "Colaborador destacado del mes",
                "Mención especial en el comité directivo de WEBSITE por figurar dentro del Top 3 de rendimiento mensual.",
                "Condición: Rango Oro (1,200+ pts) y ubicarse en el Top 3 del ranking mensual.",
                1200,
                "Rango Oro • 1,200 pts",
                R.drawable.collab_ill_reward_month_star,
                monthStarStatus,
                monthStarDate
        ));

        // 5. Reconocimiento al mejor desempeño (1,500 pts - Rango Platino)
        RecognitionStatus perfStatus;
        String perfDate;
        if (pts >= 1500) {
            perfStatus = RecognitionStatus.ACHIEVED;
            perfDate = "Reconocido el 28 Oct 2026";
        } else {
            perfStatus = RecognitionStatus.PENDING;
            perfDate = "Te faltan " + formatNumber(1500 - pts) + " pts para alcanzar Rango Platino";
        }

        items.add(new RecognitionCardItem(
                "rec_best_performance",
                "Reconocimiento al mejor desempeño",
                "Máxima distinción anual y trimestral de WEBSITE para líderes de excelencia en cumplimiento de KPI y liderazgo de resultados.",
                "Condición: Alcanzar Rango Platino (1,500+ pts) y liderar los indicadores estratégicos del área.",
                1500,
                "Rango Platino • 1,500 pts",
                R.drawable.collab_ill_reward_performance,
                perfStatus,
                perfDate
        ));

        return items;
    }

    /**
     * Historial cronológico de puntos obtenidos por el colaborador.
     * Incluye Fecha, Motivo del reconocimiento, Puntos sumados (+X pts) y
     * Total acumulado después de la operación. Sin canjes ni descuentos.
     */
    public static List<PointsHistoryEntry> getPointsHistoryForCollaborator(String collabId) {
        List<PointsHistoryEntry> list = new ArrayList<>();

        if ("collab_02".equals(collabId)) {
            // Carlos Mendoza -> Total actual: 1,620 pts
            list.add(new PointsHistoryEntry(
                    "28 Oct 2026",
                    "Cierre de contrato anual TechCorp",
                    "KPI de Cuota de ventas corporativas validado",
                    160,
                    1620
            ));
            list.add(new PointsHistoryEntry(
                    "24 Oct 2026",
                    "Meta completa de propuestas ejecutivas (18/18)",
                    "Cumplimiento al 100% antes del cierre semanal",
                    80,
                    1460
            ));
            list.add(new PointsHistoryEntry(
                    "19 Oct 2026",
                    "Renovación anticipada de cuentas clave (100%)",
                    "Reconocimiento comercial por retención B2B",
                    220,
                    1380
            ));
            list.add(new PointsHistoryEntry(
                    "11 Oct 2026",
                    "Meta de tasa de conversión de demos (30%)",
                    "Indicador trimestral superado sin observaciones",
                    260,
                    1160
            ));
            list.add(new PointsHistoryEntry(
                    "02 Oct 2026",
                    "Consolidado inicial de desempeño Q3",
                    "Base acumulada por cumplimiento de objetivos previos",
                    900,
                    900
            ));
        } else if ("collab_03".equals(collabId)) {
            // Elena Rojas -> Total actual: 890 pts
            list.add(new PointsHistoryEntry(
                    "28 Oct 2026",
                    "Resolución de Ticket Crítico Nivel 3 en 8 min",
                    "KPI de Tiempo medio de primera respuesta cumplido",
                    110,
                    890
            ));
            list.add(new PointsHistoryEntry(
                    "21 Oct 2026",
                    "Índice de satisfacción CSAT 4.9 / 5.0",
                    "Reconocimiento por excelencia en Atención al Cliente",
                    130,
                    780
            ));
            list.add(new PointsHistoryEntry(
                    "14 Oct 2026",
                    "Superación de meta quincenal de casos resueltos",
                    "75 tickets cerrados dentro del SLA operativo",
                    150,
                    650
            ));
            list.add(new PointsHistoryEntry(
                    "03 Oct 2026",
                    "Consolidado inicial de desempeño Q3",
                    "Base acumulada por constancia operativa",
                    500,
                    500
            ));
        } else {
            // Lucía Gómez -> Total actual: 1,240 pts
            list.add(new PointsHistoryEntry(
                    "28 Oct 2026",
                    "Entrega puntual: Prototipo de onboarding móvil",
                    "KPI de Entregables UI/UX del sprint validado",
                    120,
                    1240
            ));
            list.add(new PointsHistoryEntry(
                    "24 Oct 2026",
                    "Entregas puntuales en Figma al 100%",
                    "Reconocimiento semanal por cero retrasos de diseño",
                    60,
                    1120
            ));
            list.add(new PointsHistoryEntry(
                    "18 Oct 2026",
                    "Conformidad del 96% en Calidad de Design System",
                    "Superación de meta técnica (96% vs 95% requerido)",
                    200,
                    1060
            ));
            list.add(new PointsHistoryEntry(
                    "12 Oct 2026",
                    "Auditoría de contraste y accesibilidad al 100%",
                    "KPI completado sin observaciones de QA",
                    160,
                    860
            ));
            list.add(new PointsHistoryEntry(
                    "02 Oct 2026",
                    "Consolidado inicial de desempeño Q3",
                    "Base acumulada por cumplimiento de KPI previos",
                    700,
                    700
            ));
        }

        return list;
    }

    /**
     * Tabla de clasificación completa para el período seleccionado.
     */
    public static List<LeaderboardEntry> getLeaderboard(PeriodFilter period) {
        List<LeaderboardEntry> list = new ArrayList<>();
        if (period == PeriodFilter.CURRENT_WEEK) {
            list.add(new LeaderboardEntry(1, "collab_02", "Carlos Mendoza", "CM",
                    "Área Comercial • Rango Platino", 1620, 240, "↑ +240 pts esta semana"));
            list.add(new LeaderboardEntry(2, "collab_04", "Valeria Ríos", "VR",
                    "Diseño y Producto • Rango Oro", 1325, 195, "↑ +195 pts esta semana"));
            list.add(new LeaderboardEntry(3, "collab_01", "Lucía Gómez", "LG",
                    "Diseño y Producto • Rango Oro", 1240, 180, "↑ +180 pts esta semana"));
            list.add(new LeaderboardEntry(4, "collab_05", "Mateo Paredes", "MP",
                    "Tecnología e Innovación • Rango Oro", 1110, 140, "↑ +140 pts esta semana"));
            list.add(new LeaderboardEntry(5, "collab_06", "Sofía Alarcón", "SA",
                    "Marketing Digital • Rango Plata", 980, 115, "↑ +115 pts esta semana"));
            list.add(new LeaderboardEntry(6, "collab_03", "Elena Rojas", "ER",
                    "Atención al Cliente • Rango Plata", 890, 110, "↑ +110 pts esta semana"));
        } else {
            list.add(new LeaderboardEntry(1, "collab_02", "Carlos Mendoza", "CM",
                    "Área Comercial • Rango Platino", 1620, 240, "Líder #1 • 1,620 pts"));
            list.add(new LeaderboardEntry(2, "collab_04", "Valeria Ríos", "VR",
                    "Diseño y Producto • Rango Oro", 1325, 195, "Puesto #2 • 1,325 pts"));
            list.add(new LeaderboardEntry(3, "collab_01", "Lucía Gómez", "LG",
                    "Diseño y Producto • Rango Oro", 1240, 180, "↑ +2 puestos • 1,240 pts"));
            list.add(new LeaderboardEntry(4, "collab_05", "Mateo Paredes", "MP",
                    "Tecnología e Innovación • Rango Oro", 1110, 140, "Puesto #4 • 1,110 pts"));
            list.add(new LeaderboardEntry(5, "collab_06", "Sofía Alarcón", "SA",
                    "Marketing Digital • Rango Plata", 980, 115, "Puesto #5 • 980 pts"));
            list.add(new LeaderboardEntry(6, "collab_03", "Elena Rojas", "ER",
                    "Atención al Cliente • Rango Plata", 890, 110, "↑ +1 puesto • 890 pts"));
        }
        return list;
    }
}

