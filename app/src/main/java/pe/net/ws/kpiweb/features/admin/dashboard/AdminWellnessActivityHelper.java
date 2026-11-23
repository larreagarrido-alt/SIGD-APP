package pe.net.ws.kpiweb.features.admin.dashboard;

import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio, catálogos, validaciones y control de visibilidad para
 * Actividades de bienestar (Evaluación mensual -> Plan de mejora organizacional).
 * Separa estrictamente estado de ejecución vs. estado de publicación y no confunde
 * categorías mensuales con las seis dimensiones de Ryff.
 */
public final class AdminWellnessActivityHelper {

    public enum ExecutionStatus {
        PENDIENTE("PENDIENTE", "Pendiente"),
        EN_PROCESO("EN_PROCESO", "En proceso"),
        COMPLETADA("COMPLETADA", "Completada");

        public final String apiValue;
        public final String displayName;

        ExecutionStatus(String apiValue, String displayName) {
            this.apiValue = apiValue;
            this.displayName = displayName;
        }
    }

    public enum PublicationStatus {
        BORRADOR("BORRADOR", "Borrador"),
        PUBLICADA("PUBLICADA", "Publicada");

        public final String apiValue;
        public final String displayName;

        PublicationStatus(String apiValue, String displayName) {
            this.apiValue = apiValue;
            this.displayName = displayName;
        }
    }

    public enum VisibilityScope {
        SOLO_ADMIN("SOLO_ADMIN", "Solo administración"),
        TODO_EQUIPO("TODO_EQUIPO", "Todo el equipo elegible"),
        AGENCIA_CAMPANA("AGENCIA_CAMPANA", "Agencias o campañas seleccionadas"),
        COLABORADORES("COLABORADORES", "Colaboradores específicos");

        public final String apiValue;
        public final String displayName;

        VisibilityScope(String apiValue, String displayName) {
            this.apiValue = apiValue;
            this.displayName = displayName;
        }
    }

    public static final class MonthlyCategory {
        public final String code;
        public final String label;

        public MonthlyCategory(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    public static final class FormInput {
        public final String name;
        public final String objective;
        public final String categoryCode;
        public final String responsible;
        public final String scheduledDate;
        public final String startTime;
        public final String endTime;
        public final ExecutionStatus executionStatus;
        public final VisibilityScope visibilityScope;
        public final int selectedAgenciesCount;
        public final int selectedCampaignsCount;
        public final int selectedCollaboratorsCount;

        public FormInput(String name, String objective, String categoryCode, String responsible,
                         String scheduledDate, String startTime, String endTime,
                         ExecutionStatus executionStatus, VisibilityScope visibilityScope,
                         int selectedAgenciesCount, int selectedCampaignsCount,
                         int selectedCollaboratorsCount) {
            this.name = name != null ? name.trim() : "";
            this.objective = objective != null ? objective.trim() : "";
            this.categoryCode = categoryCode != null ? categoryCode.trim() : "";
            this.responsible = responsible != null ? responsible.trim() : "";
            this.scheduledDate = scheduledDate != null ? scheduledDate.trim() : "";
            this.startTime = startTime != null ? startTime.trim() : "";
            this.endTime = endTime != null ? endTime.trim() : "";
            this.executionStatus = executionStatus != null ? executionStatus : ExecutionStatus.PENDIENTE;
            this.visibilityScope = visibilityScope != null ? visibilityScope : VisibilityScope.SOLO_ADMIN;
            this.selectedAgenciesCount = Math.max(0, selectedAgenciesCount);
            this.selectedCampaignsCount = Math.max(0, selectedCampaignsCount);
            this.selectedCollaboratorsCount = Math.max(0, selectedCollaboratorsCount);
        }
    }

    public static final class ValidationResult {
        public final boolean valid;
        public final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage != null ? errorMessage : "";
        }
    }

    private AdminWellnessActivityHelper() {}

    /**
     * Categorías reales alineadas con los ejes del cuestionario mensual (WellnessResponseAnalyzer).
     */
    public static List<MonthlyCategory> monthlyCategories() {
        List<MonthlyCategory> list = new ArrayList<>(8);
        list.add(new MonthlyCategory("GENERAL", "Bienestar general"));
        list.add(new MonthlyCategory("CARGA_DE_TAREAS", "Carga de tareas"));
        list.add(new MonthlyCategory("COMUNICACION", "Comunicación"));
        list.add(new MonthlyCategory("APOYO_DEL_EQUIPO", "Apoyo del equipo"));
        list.add(new MonthlyCategory("AMBIENTE_Y_RECURSOS", "Ambiente y recursos"));
        list.add(new MonthlyCategory("TRABAJO_Y_DESCANSO", "Trabajo y descanso"));
        list.add(new MonthlyCategory("APRENDIZAJE_Y_RECONOCIMIENTO", "Aprendizaje y reconocimiento"));
        list.add(new MonthlyCategory("CLARIDAD_DE_RESPONSABILIDADES", "Claridad de responsabilidades"));
        return Collections.unmodifiableList(list);
    }

    public static String formatCategoryLabel(@Nullable String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty() || "—".equals(rawCode.trim())) {
            return "Bienestar general";
        }
        String norm = normalizeKey(rawCode);
        for (MonthlyCategory cat : monthlyCategories()) {
            if (normalizeKey(cat.code).equals(norm) || normalizeKey(cat.label).equals(norm)) {
                return cat.label;
            }
        }
        String cleaned = rawCode.trim().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    public static ExecutionStatus normalizeExecutionStatus(@Nullable String raw) {
        if (raw == null) return ExecutionStatus.PENDIENTE;
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("EN_PROCESO".equals(upper) || "EN PROCESO".equals(upper) || "ACTIVA".equals(upper)) {
            return ExecutionStatus.EN_PROCESO;
        }
        if ("COMPLETADA".equals(upper) || "FINALIZADA".equals(upper) || "REALIZADA".equals(upper)) {
            return ExecutionStatus.COMPLETADA;
        }
        return ExecutionStatus.PENDIENTE;
    }

    public static PublicationStatus normalizePublicationStatus(@Nullable String raw, boolean publishedFlag) {
        if (publishedFlag) return PublicationStatus.PUBLICADA;
        if (raw == null) return PublicationStatus.BORRADOR;
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("PUBLICADA".equals(upper) || "PUBLICADO".equals(upper) || "PUBLISHED".equals(upper)) {
            return PublicationStatus.PUBLICADA;
        }
        return PublicationStatus.BORRADOR;
    }

    public static VisibilityScope normalizeVisibilityScope(@Nullable String raw) {
        if (raw == null) return VisibilityScope.SOLO_ADMIN;
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("TODO_EQUIPO".equals(upper) || "TODOS".equals(upper) || "EQUIPO".equals(upper)) {
            return VisibilityScope.TODO_EQUIPO;
        }
        if ("AGENCIA_CAMPANA".equals(upper) || "AGENCIA".equals(upper) || "CAMPANA".equals(upper)) {
            return VisibilityScope.AGENCIA_CAMPANA;
        }
        if ("COLABORADORES".equals(upper) || "ESPECIFICOS".equals(upper)) {
            return VisibilityScope.COLABORADORES;
        }
        return VisibilityScope.SOLO_ADMIN;
    }

    /**
     * Valida el formulario según si la acción es Guardar borrador (publish=false)
     * o Publicar actividad (publish=true).
     */
    public static ValidationResult validate(FormInput input, boolean publish) {
        if (input == null) {
            return new ValidationResult(false, "Completa los datos de la actividad.");
        }
        if (input.name.isEmpty()) {
            return new ValidationResult(false, "Ingresa el nombre de la actividad.");
        }
        if (input.objective.isEmpty()) {
            return new ValidationResult(false, "Describe el objetivo de la actividad.");
        }
        if (input.responsible.isEmpty()) {
            return new ValidationResult(false, "Selecciona o indica un responsable para la actividad.");
        }
        if (input.scheduledDate.isEmpty() || !input.scheduledDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return new ValidationResult(false, "Selecciona una fecha válida para la actividad.");
        }
        if (!input.startTime.isEmpty() && !input.endTime.isEmpty()) {
            int startMinutes = parseMinutesOfDay(input.startTime);
            int endMinutes = parseMinutesOfDay(input.endTime);
            if (startMinutes < 0 || endMinutes < 0) {
                return new ValidationResult(false, "El formato de horario no es válido.");
            }
            if (endMinutes <= startMinutes) {
                return new ValidationResult(false, "La hora de finalización debe ser posterior a la hora de inicio.");
            }
        } else if (!input.startTime.isEmpty() ^ !input.endTime.isEmpty()) {
            return new ValidationResult(false, "Completa tanto la hora de inicio como la hora de finalización, o deja ambas vacías.");
        }

        if (publish) {
            if (input.visibilityScope == VisibilityScope.SOLO_ADMIN) {
                return new ValidationResult(false,
                        "Para publicar a colaboradores, selecciona 'Todo el equipo elegible', 'Agencias o campañas' o 'Colaboradores específicos'.");
            }
            if (input.visibilityScope == VisibilityScope.AGENCIA_CAMPANA
                    && input.selectedAgenciesCount == 0 && input.selectedCampaignsCount == 0) {
                return new ValidationResult(false, "Selecciona al menos una agencia o campaña destinataria.");
            }
            if (input.visibilityScope == VisibilityScope.COLABORADORES
                    && input.selectedCollaboratorsCount == 0) {
                return new ValidationResult(false, "Selecciona al menos un colaborador destinatario.");
            }
        }
        return new ValidationResult(true, "");
    }

    public static String formatAudienceSummary(VisibilityScope scope, int agenciesCount,
                                               int campaignsCount, int collaboratorsCount) {
        switch (scope) {
            case TODO_EQUIPO:
                return "Todo el equipo elegible";
            case AGENCIA_CAMPANA: {
                List<String> parts = new ArrayList<>();
                if (agenciesCount > 0) {
                    parts.add(agenciesCount + (agenciesCount == 1 ? " agencia" : " agencias"));
                }
                if (campaignsCount > 0) {
                    parts.add(campaignsCount + (campaignsCount == 1 ? " campaña" : " campañas"));
                }
                return parts.isEmpty() ? "Agencias / campañas" : String.join(" · ", parts);
            }
            case COLABORADORES:
                return collaboratorsCount + (collaboratorsCount == 1 ? " colaborador" : " colaboradores");
            case SOLO_ADMIN:
            default:
                return "Solo administración";
        }
    }

    public static String formatScheduleDisplay(@Nullable String dateYyyyMmDd,
                                               @Nullable String startTime,
                                               @Nullable String endTime) {
        String datePart = formatDateDisplay(dateYyyyMmDd);
        String start = startTime != null ? startTime.trim() : "";
        String end = endTime != null ? endTime.trim() : "";
        if (!start.isEmpty() && !end.isEmpty() && !"—".equals(start) && !"—".equals(end)) {
            return datePart + " · " + start + " - " + end;
        }
        return datePart;
    }

    public static String formatDateDisplay(@Nullable String dateYyyyMmDd) {
        if (dateYyyyMmDd == null || dateYyyyMmDd.trim().isEmpty() || "—".equals(dateYyyyMmDd.trim())) {
            return "Sin fecha";
        }
        String raw = dateYyyyMmDd.trim();
        if (raw.length() >= 10 && raw.charAt(4) == '-' && raw.charAt(7) == '-') {
            return raw.substring(8, 10) + "/" + raw.substring(5, 7) + "/" + raw.substring(0, 4);
        }
        return raw;
    }

    /**
     * Regla estricta de visibilidad para el Colaborador:
     * Solo se muestran actividades publicadas y dirigidas a colaboradores (nunca borradores ni internas).
     */
    public static boolean isVisibleToCollaborator(@Nullable PublicationStatus publicationStatus,
                                                  @Nullable VisibilityScope visibilityScope,
                                                  @Nullable String rawExecutionStatus) {
        if (publicationStatus != PublicationStatus.PUBLICADA) {
            return false;
        }
        if (visibilityScope == VisibilityScope.SOLO_ADMIN) {
            return false;
        }
        if (rawExecutionStatus != null) {
            String upper = rawExecutionStatus.trim().toUpperCase(Locale.ROOT);
            if ("CANCELADA".equals(upper) || "ELIMINADA".equals(upper)) {
                return false;
            }
        }
        return true;
    }

    private static int parseMinutesOfDay(String hhMm) {
        String[] parts = hhMm.split(":");
        if (parts.length < 2) return -1;
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String normalizeKey(String raw) {
        return Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
