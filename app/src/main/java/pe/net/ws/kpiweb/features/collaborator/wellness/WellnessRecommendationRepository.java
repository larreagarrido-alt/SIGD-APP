package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** App-private, account-scoped generic preferences; no answers or messages stored here. */
public final class WellnessRecommendationRepository {
    private static final String[] TOPICS = {"workload", "communication", "support", "environment",
            "rest", "motivation", "organization", "overall"};
    private final SharedPreferences prefs;
    private final String scope;

    public WellnessRecommendationRepository(SharedPreferences prefs, int workerId) {
        this.prefs = prefs;
        this.scope = workerId > 0 ? "wellness_feedback_v1_" + workerId + "_" : null;
    }

    public boolean isEnabled() {
        return scope != null && prefs.getBoolean(scope + "enabled", false);
    }

    public void setEnabled(boolean enabled) {
        if (scope != null) prefs.edit().putBoolean(scope + "enabled", enabled).apply();
    }

    public Map<String, Integer> preferences() {
        if (!isEnabled()) return Collections.emptyMap();
        Map<String, Integer> result = new HashMap<>();
        for (String topic : TOPICS) result.put(topic, prefs.getInt(scope + topic, 0));
        return result;
    }

    public boolean rate(String monthKey, String topic, boolean helpful) {
        if (!isEnabled() || !isKnownTopic(topic) || monthKey == null || !monthKey.matches("\\d{6}")) return false;
        String voteKey = scope + "vote_" + monthKey + "_" + topic;
        int vote = helpful ? 1 : -1;
        int oldVote = prefs.getInt(voteKey, 0);
        if (oldVote == vote) return false;
        int current = prefs.getInt(scope + topic, 0);
        int next = oldVote == 0 ? WellnessPersonalizationEngine.updatedWeight(current, helpful)
                : Math.max(-2, Math.min(2, current - oldVote + vote));
        prefs.edit().putInt(scope + topic, next).putInt(voteKey, vote).apply();
        return true;
    }

    public int vote(String monthKey, String topic) {
        if (scope == null || monthKey == null || !monthKey.matches("\\d{6}") || !isKnownTopic(topic)) return 0;
        return prefs.getInt(scope + "vote_" + monthKey + "_" + topic, 0);
    }

    public void clear() {
        if (scope == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet())
            if (key.startsWith(scope) && !key.equals(scope + "enabled")) editor.remove(key);
        editor.apply();
    }

    private boolean isKnownTopic(String topic) {
        for (String known : TOPICS) if (known.equals(topic)) return true;
        return false;
    }
}
