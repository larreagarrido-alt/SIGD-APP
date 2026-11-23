package pe.net.ws.kpiweb.features.kpi.model;

import java.io.Serializable;

/**
 * Modelo para las metas diarias de producción (Sección 02).
 */
public class KpiProductionGoal implements Serializable {
    private String categoria;
    private String metaTexto;
    private String tipoIcono; // "dev", "bug", "improve", "layout", "api", "maint"
    private int colorResId;
    private int colorBgResId;

    public KpiProductionGoal(String categoria, String metaTexto, String tipoIcono, int colorResId, int colorBgResId) {
        this.categoria = categoria;
        this.metaTexto = metaTexto;
        this.tipoIcono = tipoIcono;
        this.colorResId = colorResId;
        this.colorBgResId = colorBgResId;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMetaTexto() {
        return metaTexto;
    }

    public void setMetaTexto(String metaTexto) {
        this.metaTexto = metaTexto;
    }

    public String getTipoIcono() {
        return tipoIcono;
    }

    public void setTipoIcono(String tipoIcono) {
        this.tipoIcono = tipoIcono;
    }

    public int getColorResId() {
        return colorResId;
    }

    public void setColorResId(int colorResId) {
        this.colorResId = colorResId;
    }

    public int getColorBgResId() {
        return colorBgResId;
    }

    public void setColorBgResId(int colorBgResId) {
        this.colorBgResId = colorBgResId;
    }
}
