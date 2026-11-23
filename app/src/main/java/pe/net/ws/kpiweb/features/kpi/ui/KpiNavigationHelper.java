package pe.net.ws.kpiweb.features.kpi.ui;

import androidx.fragment.app.Fragment;

import pe.net.ws.kpiweb.features.kpi.accountant.ui.KpiAccountantContainerFragment;
import pe.net.ws.kpiweb.features.kpi.admin.ui.KpiAdminManageListFragment;
import pe.net.ws.kpiweb.features.kpi.admin.ui.KpiDynamicCaptureFragment;
import pe.net.ws.kpiweb.features.kpi.administrator.ui.KpiAdministratorContainerFragment;
import pe.net.ws.kpiweb.features.kpi.assistant.ui.KpiAdminAssistantContainerFragment;
import pe.net.ws.kpiweb.features.kpi.designer.ui.KpiDesignerContainerFragment;
import pe.net.ws.kpiweb.features.kpi.junior.ui.KpiJuniorContainerFragment;
import pe.net.ws.kpiweb.features.kpi.manager.ui.KpiGeneralManagerContainerFragment;
import pe.net.ws.kpiweb.features.kpi.photographer.ui.KpiPhotographerContainerFragment;
import pe.net.ws.kpiweb.features.kpi.senior.ui.KpiSeniorContainerFragment;
import pe.net.ws.kpiweb.features.kpi.trafficker.ui.KpiTraffickerContainerFragment;
import pe.net.ws.kpiweb.features.kpi.videographer.ui.KpiVideographerContainerFragment;

/**
 * Enrutador centralizado e independiente por cargo (listo para separar por ramas):
 * - Programador Junior -> KpiFormFragment (8 secciones técnicas propias)
 * - Diseñador Gráfico Junior -> KpiJuniorContainerFragment (piezas, nota vigesimal /20 y gráfico propio)
 * - Asesor de Ventas -> KpiSalesContainerFragment (6 metas comerciales + Hoja de Rutas con lugares y horas)
 * - Diseñador Gráfico -> KpiDesignerContainerFragment (modalidades A/B/C y gráficos propios)
 * - Diseñador Gráfico Senior -> KpiSeniorContainerFragment (Producción propia + Supervisión de equipo)
 * - Nuevos cargos del Licenciado -> KpiDynamicCaptureFragment
 */
public class KpiNavigationHelper {

    public static final String ROL_PROGRAMADOR = "PROGRAMADOR";
    public static final String ROL_ASESOR_VENTAS = "ASESOR_VENTAS";
    public static final String ROL_DISENADOR = "DISENADOR";
    public static final String ROL_DISENADOR_JUNIOR = "DISENADOR_JUNIOR";
    public static final String ROL_DISENADOR_SENIOR = "DISENADOR_SENIOR";
    public static final String ROL_JEFE_AGENCIA = "JEFE_AGENCIA";

    public static Fragment obtenerFragmentoPorRol(String rol) {
        if (rol == null || rol.trim().isEmpty()) {
            return KpiRoleSelectorFragment.newInstance();
        }

        String normalized = rol.toUpperCase().trim()
                .replace("Ñ", "N")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");

        // 1A-00. Cargo específico: Videógrafo / Producción de Video
        if (normalized.contains("VIDEOGRAFO") || normalized.contains("VIDEO")) {
            return KpiVideographerContainerFragment.newInstance();
        }

        // 1A-0. Cargo específico: Trafficker Digital / Pauta Digital
        if (normalized.contains("TRAFFICKER") || normalized.contains("PAUTA")) {
            return KpiTraffickerContainerFragment.newInstance();
        }

        // 1A. Cargo específico: Fotógrafo / Producción Audiovisual
        if (normalized.contains("FOTOGRAFO") || normalized.contains("FOTOGRAFIA") || normalized.contains("AUDIOVISUAL")) {
            return KpiPhotographerContainerFragment.newInstance();
        }

        // 1B. Cargo específico: Contador / Contabilidad y Finanzas
        if (normalized.contains("CONTADOR") || normalized.contains("CONTABILIDAD") || normalized.contains("FINANZA")) {
            return KpiAccountantContainerFragment.newInstance();
        }

        // 1B. Cargo específico: Asistente Administrativo
        if (normalized.contains("ASISTENTE") && normalized.contains("ADMIN")) {
            return KpiAdminAssistantContainerFragment.newInstance();
        }

        // 1B. Cargo específico: Administrador / Administración General
        if (normalized.contains("ADMINISTRADOR") || normalized.contains("ADMINISTRACION")) {
            return KpiAdministratorContainerFragment.newInstance();
        }

        // 1C. Cargo específico: Gerente General / Gerencia General
        if (normalized.contains("GERENTE") || normalized.contains("GERENCIA")) {
            return KpiGeneralManagerContainerFragment.newInstance();
        }

        // 1D. Dirección / Licenciado / Jefe / Admin General -> Selector de Roles
        if (normalized.contains("ADMIN") || normalized.contains("DIRECCION")
                || normalized.contains("LICENCIADO") || normalized.contains("JEFE") || normalized.equals(ROL_JEFE_AGENCIA)) {
            return KpiRoleSelectorFragment.newInstance();
        }

        // 2. Asesor de Ventas / Área Comercial
        if (normalized.contains("VENTA") || normalized.contains("COMERCIAL") || normalized.contains("ASESOR") || normalized.equals(ROL_ASESOR_VENTAS)) {
            return KpiSalesContainerFragment.newInstance();
        }

        // 3. Diseñador Gráfico Senior (Producción 24 + Supervisión Sr/Jr)
        if (normalized.contains("SENIOR") || normalized.equals(ROL_DISENADOR_SENIOR)) {
            return KpiSeniorContainerFragment.newInstance();
        }

        // 4. Diseñador Gráfico Junior (Practicante)
        if ((normalized.contains("DISEN") || normalized.contains("DISENO")) && normalized.contains("JUNIOR")) {
            return KpiJuniorContainerFragment.newInstance();
        }

        // 5. Diseñador Gráfico (Estándar / Modalidades A/B/C, 25 actividades, incluye Creativa/Diseño)
        if (normalized.contains("DISEN") || normalized.contains("DISENO") || normalized.contains("GRAFIC") || normalized.contains("CREATIV") || normalized.equals(ROL_DISENADOR)) {
            return KpiDesignerContainerFragment.newInstanceForRole("Diseñador Gráfico", false);
        }

        // 6. Programador Junior / Desarrollo TI
        if (normalized.contains("PROGRAM") || normalized.contains("DESARROLL") || normalized.contains("DEV") || normalized.contains("SISTEMA") || normalized.equals(ROL_PROGRAMADOR)) {
            return KpiFormFragment.newInstance();
        }

        // Fallback seguro: Selector de roles para evitar imponer el KPI de programador
        return KpiRoleSelectorFragment.newInstance();
    }
}
