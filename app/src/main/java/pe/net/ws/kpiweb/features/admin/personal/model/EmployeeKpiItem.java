package pe.net.ws.kpiweb.features.admin.personal.model;

import java.io.Serializable;

/**
 * Modelo representativo de un indicador KPI asignado a un colaborador.
 * Capa de presentación de solo lectura con datos ficticios para pruebas.
 * No contiene fórmulas oficiales ni lógica de evaluación del servidor.
 */
public class EmployeeKpiItem implements Serializable {

    public enum KpiStatus {
        CUMPLIDO("Cumplido", 0xFF10B981, 0x2610B981),
        EN_PROGRESO("En proceso", 0xFFF59E0B, 0x26F59E0B),
        EN_RIESGO("En riesgo", 0xFFEF4444, 0x26EF4444),
        PENDIENTE("Pendiente", 0xFFBB75FF, 0x268738F5);

        private final String displayName;
        private final int colorHex;
        private final int bgTintHex;

        KpiStatus(String displayName, int colorHex, int bgTintHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
            this.bgTintHex = bgTintHex;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColorHex() {
            return colorHex;
        }

        public int getBgTintHex() {
            return bgTintHex;
        }
    }

    private final String id;
    private final String name;
    private final String description;
    private final String measurementType; // "Porcentaje", "Tiempo", "Conteo", "Moneda", "Puntos"
    private final String targetValue;     // ej. "95%", "24 hrs", "15 casos", "S/ 50,000"
    private final String actualValue;     // ej. "97%", "18 hrs", "14 casos", "S/ 52,400"
    private final String unit;            // "%", "hrs", "tickets", "pts"
    private final int progressPercent;    // 0 - 100 para la barra visual (si aplica)
    private final boolean showProgressBar;
    private final KpiStatus status;

    public EmployeeKpiItem(String id, String name, String description, String measurementType,
                           String targetValue, String actualValue, String unit,
                           int progressPercent, boolean showProgressBar, KpiStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.measurementType = measurementType;
        this.targetValue = targetValue;
        this.actualValue = actualValue;
        this.unit = unit;
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.showProgressBar = showProgressBar;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMeasurementType() {
        return measurementType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public String getUnit() {
        return unit;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public boolean isShowProgressBar() {
        return showProgressBar;
    }

    public KpiStatus getStatus() {
        return status;
    }
}
