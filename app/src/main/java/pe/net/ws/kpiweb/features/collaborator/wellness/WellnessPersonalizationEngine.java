package pe.net.ws.kpiweb.features.collaborator.wellness;

/** Bounded preference for generic suggestion topics; does not learn from private answers. */
public final class WellnessPersonalizationEngine {
    private WellnessPersonalizationEngine() {}

    public static int updatedWeight(int current, boolean helpful) {
        return Math.max(-2, Math.min(2, current + (helpful ? 1 : -1)));
    }
}
