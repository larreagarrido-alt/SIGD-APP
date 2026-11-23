package pe.net.ws.kpiweb.features.kpi.junior.model;

import java.io.Serializable;

/**
 * Representa una actividad registrada dentro de la ficha KPI del Diseñador Gráfico Junior.
 */
public class KpiJuniorActivity implements Serializable {

    public enum Type {
        POST("Post"),
        VIDEO("Video");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Status {
        PENDIENTE("Pendiente"),
        ENTREGADO("Entregado");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String id;
    private int orderNumber; // 1, 2, 3...
    private String clientProject;
    private String activityPiece;
    private Type type;
    private String startTime; // "09:00"
    private String endTime;   // "10:00"
    private Status status;
    private int correctionsCount;

    public KpiJuniorActivity() {
        this.type = Type.POST;
        this.status = Status.PENDIENTE;
        this.correctionsCount = 0;
        this.startTime = "";
        this.endTime = "";
    }

    public KpiJuniorActivity(int orderNumber, String clientProject, String activityPiece, Type type, String startTime, String endTime, Status status, int correctionsCount) {
        this.orderNumber = orderNumber;
        this.clientProject = clientProject;
        this.activityPiece = activityPiece;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.correctionsCount = correctionsCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getClientProject() {
        return clientProject;
    }

    public void setClientProject(String clientProject) {
        this.clientProject = clientProject;
    }

    public String getActivityPiece() {
        return activityPiece;
    }

    public void setActivityPiece(String activityPiece) {
        this.activityPiece = activityPiece;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getCorrectionsCount() {
        return correctionsCount;
    }

    public void setCorrectionsCount(int correctionsCount) {
        this.correctionsCount = correctionsCount;
    }

    public org.json.JSONObject toJson() {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("id", id);
            json.put("orderNumber", orderNumber);
            json.put("clientProject", clientProject);
            json.put("activityPiece", activityPiece);
            json.put("type", type != null ? type.name() : Type.POST.name());
            json.put("startTime", startTime);
            json.put("endTime", endTime);
            json.put("status", status != null ? status.name() : Status.PENDIENTE.name());
            json.put("correctionsCount", correctionsCount);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiJuniorActivity fromJson(org.json.JSONObject json) {
        if (json == null) return null;
        KpiJuniorActivity a = new KpiJuniorActivity();
        a.setId(json.optString("id", ""));
        a.setOrderNumber(json.optInt("orderNumber", 1));
        a.setClientProject(json.optString("clientProject", ""));
        a.setActivityPiece(json.optString("activityPiece", ""));
        try {
            a.setType(Type.valueOf(json.optString("type", Type.POST.name())));
        } catch (Exception ignored) {
            a.setType(Type.POST);
        }
        a.setStartTime(json.optString("startTime", ""));
        a.setEndTime(json.optString("endTime", ""));
        try {
            a.setStatus(Status.valueOf(json.optString("status", Status.PENDIENTE.name())));
        } catch (Exception ignored) {
            a.setStatus(Status.PENDIENTE);
        }
        a.setCorrectionsCount(json.optInt("correctionsCount", 0));
        return a;
    }
}
