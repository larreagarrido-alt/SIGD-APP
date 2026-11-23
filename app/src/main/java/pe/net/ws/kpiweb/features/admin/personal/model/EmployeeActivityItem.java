package pe.net.ws.kpiweb.features.admin.personal.model;

import java.io.Serializable;

/**
 * Representa una actividad o tarea laboral asignada a un colaborador.
 * Modelo de solo lectura para información laboral autorizada y seguimiento administrativo.
 */
public class EmployeeActivityItem implements Serializable {

    private final String id;
    private final String title;
    private final String description;
    private final String category;
    private final String deadline;
    private final String status; // "Completada", "En progreso", "Pendiente", "Vencida"
    private final int progressPercent;
    private final String priority; // "Alta", "Media", "Normal"

    public EmployeeActivityItem(String id, String title, String category, String deadline, String status) {
        this(id, title, category, category, deadline, status,
                "Completada".equalsIgnoreCase(status) ? 100 : ("En progreso".equalsIgnoreCase(status) ? 60 : 0),
                "Normal");
    }

    public EmployeeActivityItem(String id, String title, String description, String category,
                                String deadline, String status, int progressPercent, String priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
        this.status = status;
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.priority = priority != null ? priority : "Normal";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getStatus() {
        return status;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public String getPriority() {
        return priority;
    }
}
