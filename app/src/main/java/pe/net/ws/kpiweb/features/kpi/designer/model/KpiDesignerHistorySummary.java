package pe.net.ws.kpiweb.features.kpi.designer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo para las estadísticas generales y evolución del Historial de Reportes.
 */
public class KpiDesignerHistorySummary implements Serializable {

    public static class EvolutionPoint implements Serializable {
        public final String label; // "S1", "S2", ...
        public final int percentage; // 50, 75, 80, ...

        public EvolutionPoint(String label, int percentage) {
            this.label = label;
            this.percentage = percentage;
        }
    }

    private int totalDesigns;
    private int totalVideos;
    private int totalPhotos;
    private int averageCompliance;
    private List<EvolutionPoint> evolutionPoints = new ArrayList<>();

    public KpiDesignerHistorySummary() {
    }

    public KpiDesignerHistorySummary(int totalDesigns, int totalVideos, int totalPhotos, int averageCompliance, List<EvolutionPoint> evolutionPoints) {
        this.totalDesigns = totalDesigns;
        this.totalVideos = totalVideos;
        this.totalPhotos = totalPhotos;
        this.averageCompliance = averageCompliance;
        this.evolutionPoints = evolutionPoints;
    }

    public int getTotalDesigns() { return totalDesigns; }
    public void setTotalDesigns(int totalDesigns) { this.totalDesigns = totalDesigns; }

    public int getTotalVideos() { return totalVideos; }
    public void setTotalVideos(int totalVideos) { this.totalVideos = totalVideos; }

    public int getTotalPhotos() { return totalPhotos; }
    public void setTotalPhotos(int totalPhotos) { this.totalPhotos = totalPhotos; }

    public int getAverageCompliance() { return averageCompliance; }
    public void setAverageCompliance(int averageCompliance) { this.averageCompliance = averageCompliance; }

    public List<EvolutionPoint> getEvolutionPoints() { return evolutionPoints; }
    public void setEvolutionPoints(List<EvolutionPoint> evolutionPoints) { this.evolutionPoints = evolutionPoints; }
}
