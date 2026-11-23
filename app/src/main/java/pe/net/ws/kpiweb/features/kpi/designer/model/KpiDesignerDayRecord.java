package pe.net.ws.kpiweb.features.kpi.designer.model;

import java.io.Serializable;

/**
 * Modelo de registro diario de producción para el Diseñador Gráfico.
 * Reglas de producción:
 * - Lunes: Modalidad B (Audiovisual + Diseño) -> 2 videos + 2 diseños
 * - Martes: Modalidad A (Diseño gráfico) -> 6 diseños
 * - Miércoles: Modalidad B (Audiovisual + Diseño) -> 2 videos + 2 diseños
 * - Jueves: Modalidad A (Diseño gráfico) -> 6 diseños
 * - Viernes: Modalidad B (Audiovisual + Diseño) -> 2 videos + 2 diseños
 * - Sábado: Producción fotográfica -> 1 sesión
 */
public class KpiDesignerDayRecord implements Serializable {

    private String date; // "04/09/2026"
    private String dayOfWeek; // "LUNES", "MARTES", etc.
    private String modality; // "B - Audiovisual + Diseño", "A - Diseño gráfico", "Producción fotográfica"
    private String dailyGoalDescription; // "02 videos + 02 diseños", "06 diseños", "01 sesión"

    private int designsGoal;
    private int videosGoal;
    private int photosGoal;

    private int designsCompleted;
    private int videosCompleted;
    private int photosCompleted;

    private String observations;
    private boolean biometricVerified;
    private long verificationTimestamp;
    private boolean locked = false;

    public KpiDesignerDayRecord() {
    }

    public KpiDesignerDayRecord(String date, String dayOfWeek, String modality, String dailyGoalDescription,
                                int designsGoal, int videosGoal, int photosGoal,
                                int designsCompleted, int videosCompleted, int photosCompleted,
                                String observations, boolean biometricVerified) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.modality = modality;
        this.dailyGoalDescription = dailyGoalDescription;
        this.designsGoal = designsGoal;
        this.videosGoal = videosGoal;
        this.photosGoal = photosGoal;
        this.designsCompleted = designsCompleted;
        this.videosCompleted = videosCompleted;
        this.photosCompleted = photosCompleted;
        this.observations = observations;
        this.biometricVerified = biometricVerified;
        this.verificationTimestamp = System.currentTimeMillis();
    }

    public int getTotalCompleted() {
        return designsCompleted + videosCompleted + photosCompleted;
    }

    public int getTotalGoal() {
        return designsGoal + videosGoal + photosGoal;
    }

    public int getCompliancePercentage() {
        int target = getTotalGoal();
        if (target <= 0) return 100;
        return (int) Math.round(((double) getTotalCompleted() / target) * 100.0);
    }

    // Getters and Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getModality() { return modality; }
    public void setModality(String modality) { this.modality = modality; }

    public String getDailyGoalDescription() { return dailyGoalDescription; }
    public void setDailyGoalDescription(String dailyGoalDescription) { this.dailyGoalDescription = dailyGoalDescription; }

    public int getDesignsGoal() { return designsGoal; }
    public void setDesignsGoal(int designsGoal) { this.designsGoal = designsGoal; }

    public int getVideosGoal() { return videosGoal; }
    public void setVideosGoal(int videosGoal) { this.videosGoal = videosGoal; }

    public int getPhotosGoal() { return photosGoal; }
    public void setPhotosGoal(int photosGoal) { this.photosGoal = photosGoal; }

    public int getDesignsCompleted() { return designsCompleted; }
    public void setDesignsCompleted(int designsCompleted) { this.designsCompleted = designsCompleted; }

    public int getVideosCompleted() { return videosCompleted; }
    public void setVideosCompleted(int videosCompleted) { this.videosCompleted = videosCompleted; }

    public int getPhotosCompleted() { return photosCompleted; }
    public void setPhotosCompleted(int photosCompleted) { this.photosCompleted = photosCompleted; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public boolean isBiometricVerified() { return biometricVerified; }
    public void setBiometricVerified(boolean biometricVerified) { this.biometricVerified = biometricVerified; }

    public long getVerificationTimestamp() { return verificationTimestamp; }
    public void setVerificationTimestamp(long verificationTimestamp) { this.verificationTimestamp = verificationTimestamp; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public org.json.JSONObject toJson() {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("date", date);
            json.put("dayOfWeek", dayOfWeek);
            json.put("modality", modality);
            json.put("dailyGoalDescription", dailyGoalDescription);
            json.put("designsGoal", designsGoal);
            json.put("videosGoal", videosGoal);
            json.put("photosGoal", photosGoal);
            json.put("designsCompleted", designsCompleted);
            json.put("videosCompleted", videosCompleted);
            json.put("photosCompleted", photosCompleted);
            json.put("observations", observations);
            json.put("biometricVerified", biometricVerified);
            json.put("verificationTimestamp", verificationTimestamp);
            json.put("locked", locked);
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static KpiDesignerDayRecord fromJson(org.json.JSONObject json) {
        if (json == null) return null;
        KpiDesignerDayRecord d = new KpiDesignerDayRecord();
        d.setDate(json.optString("date", ""));
        d.setDayOfWeek(json.optString("dayOfWeek", ""));
        d.setModality(json.optString("modality", ""));
        d.setDailyGoalDescription(json.optString("dailyGoalDescription", ""));
        d.setDesignsGoal(json.optInt("designsGoal", 0));
        d.setVideosGoal(json.optInt("videosGoal", 0));
        d.setPhotosGoal(json.optInt("photosGoal", 0));
        d.setDesignsCompleted(json.optInt("designsCompleted", 0));
        d.setVideosCompleted(json.optInt("videosCompleted", 0));
        d.setPhotosCompleted(json.optInt("photosCompleted", 0));
        d.setObservations(json.optString("observations", ""));
        d.setBiometricVerified(json.optBoolean("biometricVerified", false));
        d.setVerificationTimestamp(json.optLong("verificationTimestamp", 0));
        d.setLocked(json.optBoolean("locked", false));
        return d;
    }
}

