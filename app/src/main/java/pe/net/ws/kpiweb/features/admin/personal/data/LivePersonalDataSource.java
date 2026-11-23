package pe.net.ws.kpiweb.features.admin.personal.data;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;

/** In-memory snapshot of workers returned by the authenticated Personal API. */
public final class LivePersonalDataSource implements PersonalDataSource {
    private final List<EmployeeItem> employees = new ArrayList<>();

    public void replace(List<EmployeeItem> incoming) {
        employees.clear();
        // The API can return more than one account for the same worker.
        Map<String, EmployeeItem> byWorker = new LinkedHashMap<>();
        for (EmployeeItem item : incoming) {
            if (item != null && item.getId() != null && !item.getId().isEmpty())
                byWorker.putIfAbsent(item.getId(), item);
        }
        employees.addAll(byWorker.values());
    }

    @Override public List<EmployeeItem> getAllEmployees() { return new ArrayList<>(employees); }
    @Override public EmployeeItem getEmployeeById(String id) {
        for (EmployeeItem item : employees) if (item.getId().equals(id)) return item;
        return null;
    }
    @Override public int getTotalCount() { return employees.size(); }
    @Override public int getActiveCount() {
        int count = 0;
        for (EmployeeItem item : employees) if (item.getWorkStatus() == EmployeeItem.WorkStatus.ACTIVO) count++;
        return count;
    }
    @Override public int getVacationCount() {
        int count = 0;
        for (EmployeeItem item : employees) if (item.getWorkStatus() == EmployeeItem.WorkStatus.VACACIONES) count++;
        return count;
    }
    @Override public int getTrainingAndLeaveCount() {
        int count = 0;
        for (EmployeeItem item : employees) if (item.getWorkStatus() == EmployeeItem.WorkStatus.LICENCIA) count++;
        return count;
    }
    @Override public List<DepartmentSummary> getDepartmentSummaries() {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (EmployeeItem item : employees) {
            if (item.getDepartment().isEmpty()) continue;
            int[] count = counts.computeIfAbsent(item.getDepartment(), ignored -> new int[2]);
            count[0]++;
            if (item.getWorkStatus() == EmployeeItem.WorkStatus.ACTIVO) count[1]++;
        }
        List<DepartmentSummary> result = new ArrayList<>();
        result.add(new DepartmentSummary("Todos", employees.size(), getActiveCount()));
        for (Map.Entry<String, int[]> entry : counts.entrySet())
            result.add(new DepartmentSummary(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        return result;
    }
    private static String normalized(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT);
    }
    @Override public List<EmployeeItem> filterEmployees(String query, String department, EmployeeItem.WorkStatus status) {
        List<EmployeeItem> result = new ArrayList<>();
        String q = normalized(query);
        for (EmployeeItem item : employees) {
            if (department != null && !department.equalsIgnoreCase("Todos")
                    && !department.equalsIgnoreCase(item.getDepartment())) continue;
            if (status != null && status != item.getWorkStatus()) continue;
            if (!q.isEmpty() && !normalized(item.getName()).contains(q)
                    && !normalized(item.getRole()).contains(q) && !normalized(item.getId()).contains(q)) continue;
            result.add(item);
        }
        return result;
    }
}
