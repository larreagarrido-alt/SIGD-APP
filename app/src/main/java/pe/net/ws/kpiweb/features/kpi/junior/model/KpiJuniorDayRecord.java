package pe.net.ws.kpiweb.features.kpi.junior.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro diario consolidado del Diseñador Gráfico Junior.
 */
public class KpiJuniorDayRecord implements Serializable {

    private String date; // "16/10/2026"
    private String userName; // "Oso"
    private String supervisor; // "Carlos Ruiz"
    private String shift; // "Mañana", "Tarde", "Completo"
    private KpiJuniorProductionGoal goal; // Modalidad de meta

    private List<KpiJuniorActivity> activities = new ArrayList<>();

    // Sección 5: Evaluación rápida (true = Cumple, false = No cumple, null = sin marcar)
    private Boolean evalGoalMet;
    private Boolean evalDeliveredOnTime;
    private Boolean evalBriefingMet;
    private Boolean evalQualityAdequate;
    private Boolean evalFilesOrganized;
    private Boolean evalCommunication;

    // Sección 6: Incidencias
    private boolean hasIncident;
    private String incidentDetail;

    // Sección 7: Observaciones
    private String supervisorObservation;

    // Métricas calculadas
    private int postsCompleted;
    private int videosCompleted;
    private int totalPieces;
    private int targetPieces;
    private int deliveredPieces;
    private int pendingPieces;
    private int distributePieces;
    private int totalCorrections;
    private int goalCompliancePercentage;

    // Sección 8: Nivel de productividad
    private int totalScore; // sobre 100
    private double finalGrade; // sobre 20
    private String productivityLevel; // "Excelente", "Muy bueno", etc.

    // Verificación biométrica
    private boolean biometricVerified;
    private long verificationTimestamp;

    public KpiJuniorDayRecord() {
        this.goal = KpiJuniorProductionGoal.DISENO_GRAFICO;
        this.evalGoalMet = true;
        this.evalDeliveredOnTime = true;
        this.evalBriefingMet = true;
        this.evalQualityAdequate = true;
        this.evalFilesOrganized = true;
        this.evalCommunication = true;
        this.hasIncident = false;
        this.incidentDetail = "";
        this.supervisorObservation = "";
    }

    /**
     * Recalcula automáticamente todas las métricas de producción y notas
     * en función de las actividades actuales y la meta seleccionada.
     */
    public void recalculateMetrics() {
        int posts = 0;
        int videos = 0;
        int delivered = 0;
        int pending = 0;
        int corrections = 0;

        for (KpiJuniorActivity act : activities) {
            if (act.getType() == KpiJuniorActivity.Type.POST) {
                posts++;
            } else if (act.getType() == KpiJuniorActivity.Type.VIDEO) {
                videos++;
            }

            if (act.getStatus() == KpiJuniorActivity.Status.ENTREGADO) {
                delivered++;
            } else {
                pending++;
            }

            corrections += act.getCorrectionsCount();
        }

        this.postsCompleted = posts;
        this.videosCompleted = videos;
        this.totalPieces = posts + videos;
        this.deliveredPieces = delivered;
        this.pendingPieces = pending;
        this.totalCorrections = corrections;

        int target = (goal != null) ? goal.getTargetTotalPieces() : 4;
        this.targetPieces = target;
        this.distributePieces = Math.max(0, target - totalPieces);

        if (target > 0) {
            this.goalCompliancePercentage = (int) Math.round(((double) deliveredPieces / (double) target) * 100.0);
        } else {
            this.goalCompliancePercentage = 0;
        }

        // Cálculo automático de los 6 criterios de evaluación rápida en función de las actividades registradas
        this.evalGoalMet = (target > 0 && deliveredPieces >= target);
        this.evalDeliveredOnTime = (deliveredPieces > 0 && pendingPieces == 0);
        if (goal == KpiJuniorProductionGoal.PRODUCCION_AUDIOVISUAL) {
            this.evalBriefingMet = (videosCompleted > 0);
        } else if (goal == KpiJuniorProductionGoal.DISENO_GRAFICO) {
            this.evalBriefingMet = (postsCompleted > 0);
        } else {
            this.evalBriefingMet = (postsCompleted > 0 || videosCompleted > 0);
        }
        this.evalQualityAdequate = (deliveredPieces > 0 && corrections <= 2);
        this.evalFilesOrganized = (deliveredPieces > 0);
        this.evalCommunication = !hasIncident;

        // Calificación sobre 100 y 20
        // Ponderación base: cumplimiento de meta (hasta 80 pts) + evaluación rápida (hasta 20 pts)
        int evalCount = 0;
        if (Boolean.TRUE.equals(evalGoalMet)) evalCount++;
        if (Boolean.TRUE.equals(evalDeliveredOnTime)) evalCount++;
        if (Boolean.TRUE.equals(evalBriefingMet)) evalCount++;
        if (Boolean.TRUE.equals(evalQualityAdequate)) evalCount++;
        if (Boolean.TRUE.equals(evalFilesOrganized)) evalCount++;
        if (Boolean.TRUE.equals(evalCommunication)) evalCount++;

        double evalRatio = evalCount / 6.0;
        double complianceRatio = Math.min(1.0, (double) goalCompliancePercentage / 100.0);

        int score = (int) Math.round((complianceRatio * 75.0) + (evalRatio * 25.0));
        // Penalización ligera por correcciones excesivas
        if (corrections > 3) {
            score = Math.max(0, score - (corrections - 3) * 2);
        }
        this.totalScore = Math.min(100, Math.max(0, score));
        this.finalGrade = Math.round((totalScore / 5.0) * 10.0) / 10.0;

        // Determinar nivel de productividad según los 5 rangos oficiales SIGD
        if (totalScore >= 90) {
            this.productivityLevel = "Excelente";
        } else if (totalScore >= 80) {
            this.productivityLevel = "Muy bueno";
        } else if (totalScore >= 60) {
            this.productivityLevel = "Bueno";
        } else if (totalScore >= 40) {
            this.productivityLevel = "Regular";
        } else {
            this.productivityLevel = "Deficiente";
        }
    }

    // Getters y Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getShiftTag() {
        if (shift != null && shift.toUpperCase().contains("TARDE")) {
            return "TARDE";
        }
        return "MANANA";
    }

    public KpiJuniorProductionGoal getGoal() {
        return goal;
    }

    public void setGoal(KpiJuniorProductionGoal goal) {
        this.goal = goal;
    }

    public List<KpiJuniorActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<KpiJuniorActivity> activities) {
        this.activities = activities;
    }

    public Boolean getEvalGoalMet() {
        return evalGoalMet;
    }

    public void setEvalGoalMet(Boolean evalGoalMet) {
        this.evalGoalMet = evalGoalMet;
    }

    public Boolean getEvalDeliveredOnTime() {
        return evalDeliveredOnTime;
    }

    public void setEvalDeliveredOnTime(Boolean evalDeliveredOnTime) {
        this.evalDeliveredOnTime = evalDeliveredOnTime;
    }

    public Boolean getEvalBriefingMet() {
        return evalBriefingMet;
    }

    public void setEvalBriefingMet(Boolean evalBriefingMet) {
        this.evalBriefingMet = evalBriefingMet;
    }

    public Boolean getEvalQualityAdequate() {
        return evalQualityAdequate;
    }

    public void setEvalQualityAdequate(Boolean evalQualityAdequate) {
        this.evalQualityAdequate = evalQualityAdequate;
    }

    public Boolean getEvalFilesOrganized() {
        return evalFilesOrganized;
    }

    public void setEvalFilesOrganized(Boolean evalFilesOrganized) {
        this.evalFilesOrganized = evalFilesOrganized;
    }

    public Boolean getEvalCommunication() {
        return evalCommunication;
    }

    public void setEvalCommunication(Boolean evalCommunication) {
        this.evalCommunication = evalCommunication;
    }

    public boolean isHasIncident() {
        return hasIncident;
    }

    public void setHasIncident(boolean hasIncident) {
        this.hasIncident = hasIncident;
    }

    public String getIncidentDetail() {
        return incidentDetail;
    }

    public void setIncidentDetail(String incidentDetail) {
        this.incidentDetail = incidentDetail;
    }

    public String getSupervisorObservation() {
        return supervisorObservation;
    }

    public void setSupervisorObservation(String supervisorObservation) {
        this.supervisorObservation = supervisorObservation;
    }

    public int getPostsCompleted() {
        return postsCompleted;
    }

    public int getVideosCompleted() {
        return videosCompleted;
    }

    public int getTotalPieces() {
        return totalPieces;
    }

    public int getTargetPieces() {
        return targetPieces;
    }

    public int getDeliveredPieces() {
        return deliveredPieces;
    }

    public int getPendingPieces() {
        return pendingPieces;
    }

    public int getDistributePieces() {
        return distributePieces;
    }

    public int getTotalCorrections() {
        return totalCorrections;
    }

    public int getGoalCompliancePercentage() {
        return goalCompliancePercentage;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public double getFinalGrade() {
        return finalGrade;
    }

    public String getProductivityLevel() {
        return productivityLevel;
    }

    public boolean isBiometricVerified() {
        return biometricVerified;
    }

    public void setBiometricVerified(boolean biometricVerified) {
        this.biometricVerified = biometricVerified;
    }

    public long getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(long verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    public org.json.JSONObject toJson() {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("date", date);
            json.put("userName", userName);
            json.put("supervisor", supervisor);
            json.put("shift", shift);
            json.put("goal", goal != null ? goal.name() : KpiJuniorProductionGoal.DISENO_GRAFICO.name());
            json.put("evalGoalMet", evalGoalMet != null ? evalGoalMet : false);
            json.put("evalDeliveredOnTime", evalDeliveredOnTime != null ? evalDeliveredOnTime : false);
            json.put("evalBriefingMet", evalBriefingMet != null ? evalBriefingMet : false);
            json.put("evalQualityAdequate", evalQualityAdequate != null ? evalQualityAdequate : false);
            json.put("evalFilesOrganized", evalFilesOrganized != null ? evalFilesOrganized : false);
            json.put("evalCommunication", evalCommunication != null ? evalCommunication : false);
            json.put("hasIncident", hasIncident);
            json.put("incidentDetail", incidentDetail);
            json.put("supervisorObservation", supervisorObservation);
            json.put("postsCompleted", postsCompleted);
            json.put("videosCompleted", videosCompleted);
            json.put("totalPieces", totalPieces);
            json.put("targetPieces", targetPieces);
            json.put("deliveredPieces", deliveredPieces);
            json.put("pendingPieces", pendingPieces);
            json.put("distributePieces", distributePieces);
            json.put("totalCorrections", totalCorrections);
            json.put("goalCompliancePercentage", goalCompliancePercentage);
            json.put("totalScore", totalScore);
            json.put("finalGrade", finalGrade);
            json.put("productivityLevel", productivityLevel);
            json.put("biometricVerified", biometricVerified);
            json.put("verificationTimestamp", verificationTimestamp);

            org.json.JSONArray actArray = new org.json.JSONArray();
            if (activities != null) {
                for (KpiJuniorActivity act : activities) {
                    actArray.put(act.toJson());
                }
            }
            json.put("activities", actArray);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiJuniorDayRecord fromJson(org.json.JSONObject json) {
        if (json == null) return null;
        KpiJuniorDayRecord r = new KpiJuniorDayRecord();
        r.setDate(json.optString("date", ""));
        r.setUserName(json.optString("userName", ""));
        r.setSupervisor(json.optString("supervisor", ""));
        r.setShift(json.optString("shift", ""));
        try {
            r.setGoal(KpiJuniorProductionGoal.valueOf(json.optString("goal", KpiJuniorProductionGoal.DISENO_GRAFICO.name())));
        } catch (Exception ignored) {
            r.setGoal(KpiJuniorProductionGoal.DISENO_GRAFICO);
        }
        r.setEvalGoalMet(json.optBoolean("evalGoalMet", false));
        r.setEvalDeliveredOnTime(json.optBoolean("evalDeliveredOnTime", false));
        r.setEvalBriefingMet(json.optBoolean("evalBriefingMet", false));
        r.setEvalQualityAdequate(json.optBoolean("evalQualityAdequate", false));
        r.setEvalFilesOrganized(json.optBoolean("evalFilesOrganized", false));
        r.setEvalCommunication(json.optBoolean("evalCommunication", false));
        r.setHasIncident(json.optBoolean("hasIncident", false));
        r.setIncidentDetail(json.optString("incidentDetail", ""));
        r.setSupervisorObservation(json.optString("supervisorObservation", ""));
        r.setBiometricVerified(json.optBoolean("biometricVerified", false));
        r.setVerificationTimestamp(json.optLong("verificationTimestamp", 0));

        org.json.JSONArray actArray = json.optJSONArray("activities");
        List<KpiJuniorActivity> actList = new ArrayList<>();
        if (actArray != null) {
            for (int i = 0; i < actArray.length(); i++) {
                org.json.JSONObject actObj = actArray.optJSONObject(i);
                if (actObj != null) {
                    KpiJuniorActivity a = KpiJuniorActivity.fromJson(actObj);
                    if (a != null) actList.add(a);
                }
            }
        }
        r.setActivities(actList);
        r.recalculateMetrics();
        return r;
    }
}
