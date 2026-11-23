package pe.net.ws.kpiweb.features.admin.dashboard;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Lógica pura, formateo y validaciones del módulo Bienestar organizacional (SIGD).
 * Libre de dependencias de UI para facilitar pruebas unitarias determinísticas.
 */
public final class AdminWellnessHelper {

    public enum CampaignStatus {
        ACTIVA,
        PROGRAMADA,
        FINALIZADA,
        BORRADOR,
        CANCELADA
    }

    public static final class TrendPoint {
        public final String monthKey;
        public final boolean valid;
        public final float percentage;

        public TrendPoint(String monthKey, boolean valid, float percentage) {
            this.monthKey = monthKey != null ? monthKey : "";
            this.valid = valid;
            this.percentage = percentage;
        }
    }

    public static final class RyffDimensionInfo {
        public final String key;
        public final String displayName;
        public final int accentColorHex;
        public final int badgeBgHex;

        public RyffDimensionInfo(String key, String displayName, int accentColorHex, int badgeBgHex) {
            this.key = key;
            this.displayName = displayName;
            this.accentColorHex = accentColorHex;
            this.badgeBgHex = badgeBgHex;
        }
    }

    public static final class CampaignRecord {
        public final long id;
        public final String title;
        public final String description;
        public final String audience;
        public final String startAt;
        public final String endAt;
        public final CampaignStatus status;
        public final int invitedCount;
        public final int completedCount;
        public final boolean hasInvitationCounts;

        public CampaignRecord(long id, String title, String description, String audience,
                              String startAt, String endAt, CampaignStatus status,
                              int invitedCount, int completedCount, boolean hasInvitationCounts) {
            this.id = id;
            this.title = title != null ? title : "";
            this.description = description != null ? description : "";
            this.audience = audience != null ? audience : "";
            this.startAt = startAt != null ? startAt : "";
            this.endAt = endAt != null ? endAt : "";
            this.status = status != null ? status : CampaignStatus.BORRADOR;
            this.invitedCount = Math.max(0, invitedCount);
            this.completedCount = Math.max(0, completedCount);
            this.hasInvitationCounts = hasInvitationCounts;
        }
    }

    public static final class CampaignOperationalStats {
        public final int totalApplications;
        public final int activeOrPending;
        public final int completedEvaluations;
        public final int finishedOrExpired;
        public final boolean usesInvitationCounts;

        public CampaignOperationalStats(int totalApplications, int activeOrPending,
                                        int completedEvaluations, int finishedOrExpired,
                                        boolean usesInvitationCounts) {
            this.totalApplications = totalApplications;
            this.activeOrPending = activeOrPending;
            this.completedEvaluations = completedEvaluations;
            this.finishedOrExpired = finishedOrExpired;
            this.usesInvitationCounts = usesInvitationCounts;
        }
    }

    public static final class RangeValidationResult {
        public final boolean valid;
        public final String errorMessage;
        public final String projectedStatus;
        public final String durationSummary;

        public RangeValidationResult(boolean valid, String errorMessage,
                                     String projectedStatus, String durationSummary) {
            this.valid = valid;
            this.errorMessage = errorMessage != null ? errorMessage : "";
            this.projectedStatus = projectedStatus != null ? projectedStatus : "";
            this.durationSummary = durationSummary != null ? durationSummary : "";
        }
    }

    private static final String[] MONTHS_ES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private static final String[] MONTHS_SHORT_ES = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private AdminWellnessHelper() {}

    public static String formatMonthDisplay(@Nullable String yyyyMm) {
        if (yyyyMm == null || yyyyMm.length() < 7 || yyyyMm.charAt(4) != '-') {
            return yyyyMm == null ? "—" : yyyyMm;
        }
        try {
            int year = Integer.parseInt(yyyyMm.substring(0, 4));
            int month = Integer.parseInt(yyyyMm.substring(5, 7));
            if (month >= 1 && month <= 12) {
                return MONTHS_ES[month - 1] + " " + year;
            }
        } catch (NumberFormatException ignored) {
        }
        return yyyyMm;
    }

    public static String formatMonthShort(@Nullable String yyyyMm) {
        if (yyyyMm == null || yyyyMm.length() < 7 || yyyyMm.charAt(4) != '-') {
            return yyyyMm == null ? "—" : yyyyMm;
        }
        try {
            int month = Integer.parseInt(yyyyMm.substring(5, 7));
            String shortYear = yyyyMm.substring(2, 4);
            if (month >= 1 && month <= 12) {
                return MONTHS_SHORT_ES[month - 1] + " " + shortYear;
            }
        } catch (NumberFormatException ignored) {
        }
        return yyyyMm;
    }

    /**
     * Formatea el porcentaje real de participación.
     * Prioriza el valor autorizado del servidor y solo calcula (completados / elegibles) * 100
     * cuando el servidor no envía el porcentaje y existe un denominador estrictamente válido (> 0).
     */
    public static String formatParticipationPercent(@Nullable Double serverPercent, int completed, int eligible) {
        Double value = null;
        if (serverPercent != null && !Double.isNaN(serverPercent) && !Double.isInfinite(serverPercent)) {
            value = Math.max(0.0, Math.min(100.0, serverPercent));
        } else if (eligible > 0 && completed >= 0) {
            double computed = (completed * 100.0) / eligible;
            value = Math.max(0.0, Math.min(100.0, computed));
        }
        if (value == null) return "—";
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return Math.round(value) + " %";
        }
        return String.format(Locale.US, "%.1f %%", value);
    }

    /**
     * Exige al menos dos puntos mensuales válidos y comparables para graficar tendencias.
     */
    public static boolean hasComparableTrendData(@Nullable List<TrendPoint> points) {
        if (points == null) return false;
        int validCount = 0;
        for (TrendPoint point : points) {
            if (point != null && point.valid) {
                validCount++;
                if (validCount >= 2) return true;
            }
        }
        return false;
    }

    public static List<RyffDimensionInfo> canonicalRyffDimensions() {
        List<RyffDimensionInfo> list = new ArrayList<>(6);
        list.add(new RyffDimensionInfo("autoaceptacion", "Autoaceptación", 0xFFD8B4FE, 0xFF382252));
        list.add(new RyffDimensionInfo("relaciones_positivas", "Relaciones positivas", 0xFF57D6A0, 0xFF174637));
        list.add(new RyffDimensionInfo("autonomia", "Autonomía", 0xFF8DE1D5, 0xFF163D44));
        list.add(new RyffDimensionInfo("dominio_del_entorno", "Dominio del entorno", 0xFFFFC75A, 0xFF4B3419));
        list.add(new RyffDimensionInfo("proposito_en_la_vida", "Propósito en la vida", 0xFFFFAA70, 0xFF4B2B23));
        list.add(new RyffDimensionInfo("crecimiento_personal", "Crecimiento personal", 0xFFFF819D, 0xFF4C2338));
        return Collections.unmodifiableList(list);
    }

    public static String normalizeDimensionKey(@Nullable String raw) {
        if (raw == null) return "";
        String norm = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (norm.contains("autoacept")) return "autoaceptacion";
        if (norm.contains("relacion")) return "relaciones_positivas";
        if (norm.contains("autonom")) return "autonomia";
        if (norm.contains("dominio") || norm.contains("entorno")) return "dominio_del_entorno";
        if (norm.contains("proposito")) return "proposito_en_la_vida";
        if (norm.contains("crecimiento")) return "crecimiento_personal";
        return norm;
    }

    public static String formatRyffDimensionName(@Nullable String raw) {
        String key = normalizeDimensionKey(raw);
        for (RyffDimensionInfo info : canonicalRyffDimensions()) {
            if (info.key.equals(key)) return info.displayName;
        }
        if (raw == null || raw.trim().isEmpty()) return "General";
        String cleaned = raw.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    public static CampaignStatus normalizeCampaignStatus(@Nullable String raw) {
        if (raw == null) return CampaignStatus.BORRADOR;
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVA".equals(upper) || "PUBLICADA".equals(upper) || "VIGENTE".equals(upper) || "EN_CURSO".equals(upper)) {
            return CampaignStatus.ACTIVA;
        }
        if ("PROGRAMADA".equals(upper) || "PENDIENTE".equals(upper)) {
            return CampaignStatus.PROGRAMADA;
        }
        if ("FINALIZADA".equals(upper) || "CERRADA".equals(upper) || "VENCIDA".equals(upper) || "EXPIRADA".equals(upper)) {
            return CampaignStatus.FINALIZADA;
        }
        if ("CANCELADA".equals(upper) || "DESACTIVADA".equals(upper)) {
            return CampaignStatus.CANCELADA;
        }
        return CampaignStatus.BORRADOR;
    }

    public static String formatCampaignStatus(CampaignStatus status) {
        switch (status) {
            case ACTIVA:
                return "Activa";
            case PROGRAMADA:
                return "Programada";
            case FINALIZADA:
                return "Finalizada";
            case CANCELADA:
                return "Cancelada";
            case BORRADOR:
            default:
                return "Borrador";
        }
    }

    public static CampaignOperationalStats computeCampaignStats(@Nullable List<CampaignRecord> campaigns) {
        if (campaigns == null || campaigns.isEmpty()) {
            return new CampaignOperationalStats(0, 0, 0, 0, false);
        }
        boolean hasInvitations = false;
        int totalInvited = 0;
        int totalCompleted = 0;
        int pendingOrActive = 0;
        int expiredOrFinished = 0;

        for (CampaignRecord item : campaigns) {
            if (item == null) continue;
            if (item.hasInvitationCounts) {
                hasInvitations = true;
                totalInvited += item.invitedCount;
                totalCompleted += item.completedCount;
            } else {
                totalCompleted += item.completedCount;
            }
            if (item.status == CampaignStatus.ACTIVA || item.status == CampaignStatus.PROGRAMADA) {
                pendingOrActive++;
            } else if (item.status == CampaignStatus.FINALIZADA || item.status == CampaignStatus.CANCELADA) {
                expiredOrFinished++;
            }
        }
        int totalMetric = hasInvitations ? totalInvited : campaigns.size();
        return new CampaignOperationalStats(totalMetric, pendingOrActive, totalCompleted, expiredOrFinished, hasInvitations);
    }

    public static RangeValidationResult validateVigenciaRange(long startMillis, long endMillis, long nowMillis) {
        if (startMillis <= 0L || endMillis <= 0L) {
            return new RangeValidationResult(false, "Selecciona fecha y hora de inicio y vencimiento.", "", "");
        }
        if (endMillis <= startMillis) {
            return new RangeValidationResult(false, "La fecha de vencimiento debe ser posterior al inicio.", "", "");
        }
        long diffMinutes = Math.max(1L, (endMillis - startMillis) / 60000L);
        String duration;
        if (diffMinutes < 60L) {
            duration = diffMinutes + (diffMinutes == 1L ? " minuto" : " minutos");
        } else if (diffMinutes < 24L * 60L) {
            long hours = diffMinutes / 60L;
            duration = hours + (hours == 1L ? " hora" : " horas");
        } else {
            long days = diffMinutes / (24L * 60L);
            duration = days + (days == 1L ? " día" : " días");
        }
        String projected = startMillis > nowMillis ? "Programada" : "Activa";
        return new RangeValidationResult(true, "", projected, duration);
    }

    public static String formatServerDateTime(long epochMillis, TimeZone zone) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        fmt.setTimeZone(zone != null ? zone : TimeZone.getDefault());
        return fmt.format(new Date(epochMillis));
    }

    public static String formatDisplayDateTime(long epochMillis, TimeZone zone) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.US);
        fmt.setTimeZone(zone != null ? zone : TimeZone.getDefault());
        return fmt.format(new Date(epochMillis));
    }

    public static String deriveAudienceFromRoles(@Nullable List<String> roles) {
        if (roles == null || roles.isEmpty()) return "AMBOS";
        boolean hasIntern = false;
        boolean hasCollaborator = false;
        for (String role : roles) {
            String normalized = normalizeSearchText(role);
            if (normalized.contains("practicante") || normalized.contains("pasante") || normalized.contains("intern")) {
                hasIntern = true;
            } else {
                hasCollaborator = true;
            }
        }
        if (hasIntern && !hasCollaborator) return "PRACTICANTE";
        if (hasCollaborator && !hasIntern) return "COLABORADOR";
        return "AMBOS";
    }

    public static boolean matchesCollaboratorQuery(@Nullable String name, @Nullable String role,
                                                   @Nullable String agency, @Nullable String query) {
        String q = normalizeSearchText(query);
        if (q.isEmpty()) return true;
        return normalizeSearchText(name).contains(q)
                || normalizeSearchText(role).contains(q)
                || normalizeSearchText(agency).contains(q);
    }

    public static String normalizeSearchText(@Nullable String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
