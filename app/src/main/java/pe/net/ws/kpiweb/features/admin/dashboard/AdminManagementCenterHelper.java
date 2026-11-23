package pe.net.ws.kpiweb.features.admin.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fuente única de datos para las herramientas administrativas del Centro de gestión.
 * Excluye los accesos que ya viven en la barra inferior (Inicio/Dashboard, Personal,
 * Reportes y Notificaciones) y mantiene separación estricta respecto a "Mi espacio".
 */
public final class AdminManagementCenterHelper {

    public enum ModuleId {
        KPI_MANAGEMENT,
        KPI_SHEET_EDITOR,
        SCHEDULE_EDITOR,
        RANKING,
        REWARDS,
        WELLNESS
    }

    public static final class ModuleItem {
        public final ModuleId id;
        public final String title;
        public final String description;
        public final int iconColorHex;
        public final int iconBgHex;

        public ModuleItem(ModuleId id, String title, String description, int iconColorHex, int iconBgHex) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.iconColorHex = iconColorHex;
            this.iconBgHex = iconBgHex;
        }
    }

    private AdminManagementCenterHelper() {}

    public static List<ModuleItem> buildAdminModules() {
        List<ModuleItem> list = new ArrayList<>(5);
        list.add(new ModuleItem(ModuleId.KPI_SHEET_EDITOR, "Editor de fichas KPI", "Edita el diseño y las metas visibles por cargo",
                0xFFC4A0FF, 0xFF3A2458));
        list.add(new ModuleItem(ModuleId.SCHEDULE_EDITOR, "Editor de horarios", "Edita y publica el cronograma por cargo",
                0xFF9CD8FF, 0xFF1C4055));
        list.add(new ModuleItem(ModuleId.RANKING, "Ranking", "Clasificación del equipo",
                0xFFFFC75A, 0xFF4B3419));
        list.add(new ModuleItem(ModuleId.REWARDS, "Premios", "Catálogo y recompensas",
                0xFFFFAA70, 0xFF4B2B23));
        list.add(new ModuleItem(ModuleId.WELLNESS, "Bienestar", "Seguimiento autorizado",
                0xFFFF819D, 0xFF4C2338));
        return Collections.unmodifiableList(list);
    }

    public static String formatModuleCount(int count) {
        int safe = Math.max(0, count);
        return safe == 1 ? "1 módulo" : safe + " módulos";
    }
}
