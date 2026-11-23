package pe.net.ws.kpiweb.features.admin.personal;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;

/**
 * Lógica pura de estadísticas, ordenamiento seguro y etiquetas del módulo Personal (Administrador).
 * Opera únicamente sobre los colaboradores reales entregados por el servidor.
 */
public final class AdminPersonalUiHelper {

    public enum SortOption {
        ORIGINAL("Ordenar por", "Orden original"),
        NAME_ASC("Nombre", "Nombre (A–Z)"),
        ROLE_ASC("Cargo", "Cargo (A–Z)"),
        STATUS("Estado", "Estado laboral");

        public final String buttonLabel;
        public final String sheetLabel;

        SortOption(String buttonLabel, String sheetLabel) {
            this.buttonLabel = buttonLabel;
            this.sheetLabel = sheetLabel;
        }
    }

    public static final class Stats {
        public final int total;
        public final int active;
        public final int inactive;
        public final int leave;

        public Stats(int total, int active, int inactive, int leave) {
            this.total = total;
            this.active = active;
            this.inactive = inactive;
            this.leave = leave;
        }
    }

    private static final int[] AVATAR_PALETTE = new int[]{
            0xFF7C3AED,
            0xFF8B30E0,
            0xFF6D28D9,
            0xFF6366F1,
            0xFF9333EA,
            0xFF5B48C8
    };

    private AdminPersonalUiHelper() {}

    public static Stats computeStats(List<EmployeeItem> employees) {
        if (employees == null || employees.isEmpty()) {
            return new Stats(0, 0, 0, 0);
        }
        int active = 0;
        int inactive = 0;
        int leave = 0;
        for (EmployeeItem item : employees) {
            if (item == null || item.getWorkStatus() == null) continue;
            switch (item.getWorkStatus()) {
                case ACTIVO:
                    active++;
                    break;
                case INACTIVO:
                    inactive++;
                    break;
                case LICENCIA:
                    leave++;
                    break;
                default:
                    break;
            }
        }
        return new Stats(employees.size(), active, inactive, leave);
    }

    /**
     * Cuenta colaboradores por agencia respetando el conjunto filtrado por búsqueda y estado
     * (sin aplicar el filtro de agencia) para mostrar contadores dinámicos en cada chip.
     */
    public static Map<String, Integer> countByAgency(List<EmployeeItem> employees) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int total = employees == null ? 0 : employees.size();
        counts.put("todos", total);
        if (employees == null) return counts;
        for (EmployeeItem item : employees) {
            if (item == null || item.getDepartment() == null) continue;
            String key = item.getDepartment().trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) continue;
            Integer prev = counts.get(key);
            counts.put(key, prev == null ? 1 : prev + 1);
        }
        return counts;
    }

    public static int agencyCountFor(Map<String, Integer> agencyCounts, String departmentName) {
        if (agencyCounts == null || departmentName == null) return 0;
        String key = departmentName.trim().toLowerCase(Locale.ROOT);
        Integer value = agencyCounts.get(key);
        return value != null ? value : 0;
    }

    public static String formatAgencyDisplayName(String departmentName) {
        if (departmentName == null || departmentName.trim().isEmpty()) return "Todas";
        return "Todos".equalsIgnoreCase(departmentName.trim()) ? "Todas" : departmentName.trim();
    }

    public static List<EmployeeItem> sortEmployees(List<EmployeeItem> source, SortOption option) {
        List<EmployeeItem> copy = source == null ? new ArrayList<>() : new ArrayList<>(source);
        if (option == null || option == SortOption.ORIGINAL || copy.size() <= 1) {
            return copy;
        }
        Comparator<EmployeeItem> comparator;
        switch (option) {
            case NAME_ASC:
                comparator = (a, b) -> normalized(a.getName()).compareTo(normalized(b.getName()));
                break;
            case ROLE_ASC:
                comparator = (a, b) -> {
                    int cmp = normalized(a.getRole()).compareTo(normalized(b.getRole()));
                    return cmp != 0 ? cmp : normalized(a.getName()).compareTo(normalized(b.getName()));
                };
                break;
            case STATUS:
                comparator = (a, b) -> {
                    int cmp = Integer.compare(statusRank(a.getWorkStatus()), statusRank(b.getWorkStatus()));
                    return cmp != 0 ? cmp : normalized(a.getName()).compareTo(normalized(b.getName()));
                };
                break;
            default:
                return copy;
        }
        Collections.sort(copy, comparator);
        return copy;
    }

    public static String formatResultsCount(int showing, int total, boolean filtered) {
        int safeShowing = Math.max(0, showing);
        int safeTotal = Math.max(0, total);
        if (!filtered || safeShowing == safeTotal) {
            return safeShowing == 1 ? "1 colaborador" : safeShowing + " colaboradores";
        }
        return safeShowing + " de " + safeTotal + " colaboradores";
    }

    public static String formatAgencyChipLabel(DepartmentSummary summary) {
        if (summary == null) return "Todas";
        String name = summary.getDepartmentName();
        String display = "Todos".equalsIgnoreCase(name) ? "Todas" : name;
        return display + " (" + Math.max(0, summary.getTotalCount()) + ")";
    }

    public static int avatarColorFor(String name, String id) {
        String key = (name != null && !name.trim().isEmpty()) ? name.trim() : (id == null ? "" : id);
        int hash = Math.abs(key.hashCode());
        return AVATAR_PALETTE[hash % AVATAR_PALETTE.length];
    }

    private static int statusRank(EmployeeItem.WorkStatus status) {
        if (status == null) return 99;
        switch (status) {
            case ACTIVO:
                return 0;
            case LICENCIA:
            case CAPACITACION:
                return 1;
            case VACACIONES:
                return 2;
            case INACTIVO:
                return 3;
            default:
                return 4;
        }
    }

    private static String normalized(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
