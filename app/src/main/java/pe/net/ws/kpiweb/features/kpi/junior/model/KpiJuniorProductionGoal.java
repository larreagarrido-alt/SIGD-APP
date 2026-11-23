package pe.net.ws.kpiweb.features.kpi.junior.model;

import java.io.Serializable;

/**
 * Modalidad de meta diaria de producción para el Diseñador Gráfico Junior:
 * 1. DISEÑO GRÁFICO: 4 posts diarios.
 * 2. PRODUCCIÓN AUDIOVISUAL: 2 videos diarios.
 * 3. PRODUCCIÓN MIXTA: 1 video y 2 posts diarios.
 */
public enum KpiJuniorProductionGoal implements Serializable {
    DISENO_GRAFICO("Diseño gráfico", 4, 0, 4, "4 piezas gráficas para redes sociales"),
    PRODUCCION_AUDIOVISUAL("Producción audiovisual", 0, 2, 2, "2 videos cortos para redes sociales"),
    PRODUCCION_MIXTA("Producción mixta", 2, 1, 3, "1 video corto para redes y 2 piezas gráficas");

    private final String title;
    private final int targetPosts;
    private final int targetVideos;
    private final int targetTotalPieces;
    private final String description;

    KpiJuniorProductionGoal(String title, int targetPosts, int targetVideos, int targetTotalPieces, String description) {
        this.title = title;
        this.targetPosts = targetPosts;
        this.targetVideos = targetVideos;
        this.targetTotalPieces = targetTotalPieces;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public int getTargetPosts() {
        return targetPosts;
    }

    public int getTargetVideos() {
        return targetVideos;
    }

    public int getTargetTotalPieces() {
        return targetTotalPieces;
    }

    public String getDescription() {
        return description;
    }
}
