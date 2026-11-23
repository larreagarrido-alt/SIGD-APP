package pe.net.ws.kpiweb.features.kpi.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.model.KpiActivityItem;
import pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport;
import pe.net.ws.kpiweb.features.kpi.model.KpiProductionGoal;
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesActivityItem;
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesReport;

/**
 * Generador de datos simulados (Mock Data) idénticos al prototipo visual aprobado.
 */
public class KpiMockDataFactory {

    /** Original programmer form template without any example worker, activity or score. */
    public static KpiDailyReport createBlankProgrammerTemplate() {
        KpiDailyReport report = new KpiDailyReport();
        report.setMetas(programmerGoals());
        report.setCriteriosEvaluacion(programmerCriteria(false));
        return report;
    }

    private static List<KpiProductionGoal> programmerGoals() {
        List<KpiProductionGoal> metas = new ArrayList<>();
        metas.add(new KpiProductionGoal("Desarrollo de funcionalidad", "1 funcionalidad", "dev", R.color.kpi_cat_desarrollo, R.color.kpi_cat_desarrollo_bg));
        metas.add(new KpiProductionGoal("Corrección de errores", "2 a 4 incidencias", "bug", R.color.kpi_cat_correccion, R.color.kpi_cat_correccion_bg));
        metas.add(new KpiProductionGoal("Mejora / Cambio pequeños", "2 a 4 tareas", "improve", R.color.kpi_cat_mejora, R.color.kpi_cat_mejora_bg));
        metas.add(new KpiProductionGoal("Maquetación / Frontend", "1 módulo o sección", "layout", R.color.kpi_cat_maquetacion, R.color.kpi_cat_maquetacion_bg));
        metas.add(new KpiProductionGoal("Integración / API", "1 integración / avance", "api", R.color.kpi_cat_api, R.color.kpi_cat_api_bg));
        metas.add(new KpiProductionGoal("Mantenimiento Web", "2 a 4 tareas", "maint", R.color.kpi_cat_mantenimiento, R.color.kpi_cat_mantenimiento_bg));
        return metas;
    }

    private static Map<String, Boolean> programmerCriteria(boolean checked) {
        Map<String, Boolean> criterios = new LinkedHashMap<>();
        criterios.put("Cumplí las tareas asignadas", checked);
        criterios.put("Entregué dentro del tiempo", checked);
        criterios.put("Código funcional", checked);
        criterios.put("Código ordenado y entendible", checked);
        criterios.put("Realicé pruebas", checked);
        criterios.put("Utilicé Git correctamente", checked);
        criterios.put("Documenté los cambios", checked);
        criterios.put("Comuniqué avances o dificultades", checked);
        return criterios;
    }

    public static KpiDailyReport createMockReport() {
        KpiDailyReport report = new KpiDailyReport();
        report.setId("REP-2025-0524");
        
        // 01 DATOS GENERALES
        report.setFecha("24/05/2025");
        String authUser = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        boolean isPracticante = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String prefix = isPracticante ? "Practicante: " : "Colaborador: ";
        String nameToUse = (authUser != null && !authUser.isEmpty() && !authUser.equals("Colaborador WebSite+"))
                ? prefix + authUser
                : (isPracticante ? "Practicante" : "Colaborador");
        report.setPracticante(nameToUse);
        report.setSupervisor("Ing. Jair Valdera (Supervisor de TI)");
        report.setTurno("Tarde (3:30 pm - 6:30 pm)");
        report.setRol(isPracticante ? "PRACTICANTE DE SOFTWARE Y PROGRAMACIÓN WEB" : "COLABORADOR DE SOFTWARE Y PROGRAMACIÓN WEB");

        // 02 METAS DIARIAS DE PRODUCCIÓN
        report.setMetas(programmerGoals());

        // 03 REGISTRO DE ACTIVIDADES (Vacías para mostrar el placeholder de descripción)
        List<KpiActivityItem> actividades = new ArrayList<>();
        actividades.add(new KpiActivityItem(
                "ACT-01", "01", "Webpuntos", "Desarrollo",
                "",
                "03:30 pm", "04:15 pm", "Entregado", 0, "a3b2c3d"
        ));
        actividades.add(new KpiActivityItem(
                "ACT-02", "02", "Website Corporativo", "Corrección",
                "",
                "04:20 pm", "05:00 pm", "Entregado", 1, "b7c1d92"
        ));
        actividades.add(new KpiActivityItem(
                "ACT-03", "03", "Tienda Online", "Maquetación",
                "",
                "05:05 pm", "05:45 pm", "Entregado", 0, "c9f4e21"
        ));
        actividades.add(new KpiActivityItem(
                "ACT-04", "04", "Panel Administrativo", "Integración / API",
                "",
                "05:50 pm", "06:15 pm", "Entregado", 0, "d1a8b64"
        ));
        actividades.add(new KpiActivityItem(
                "ACT-05", "05", "Comisión externa / Terreno", "Salida a campo",
                "Soporte técnico y coordinación presencial en sede externa",
                "06:00 pm", "06:30 pm", "Salida a campo", 0, "CAMPO-ADMIN"
        ));
        report.setActividades(actividades);

        // 05 EVALUACIÓN RÁPIDA DEL DÍA
        report.setCriteriosEvaluacion(programmerCriteria(true));

        // 06 INCIDENCIAS DEL DÍA
        report.setHuboDificultad(true);
        report.setDetalleDificultad("La API de WhatsApp presentó lentitud temporal que retrasó la integración aprox. 15 minutos.");

        // 07 OBSERVACIÓN DEL SUPERVISOR Y FIRMAS
        report.setObservacionSupervisor("Buen trabajo Luis, cumpliste con todas las tareas asignadas y la calidad del código fue muy buena. Continúa así.");
        report.setFirmaPracticante("Luis Fernando Pérez Díaz");
        report.setFirmaSupervisor("María José Sánchez");

        // 08 NIVEL DE PRODUCTIVIDAD
        report.setNivelProductividad("EXCELENTE");
        report.setPuntaje(20);

        return report;
    }

    public static List<KpiDailyReport> createMockReportList() {
        List<KpiDailyReport> list = new ArrayList<>();
        list.add(createMockReport());

        // Reporte de ayer
        KpiDailyReport repAyer = new KpiDailyReport();
        repAyer.setId("REP-2025-0523");
        repAyer.setFecha("23/05/2025");
        repAyer.setPracticante("Luis Fernando Pérez Díaz");
        repAyer.setSupervisor("María José Sánchez");
        repAyer.setTurno("Mañana (8:00 am - 1:30 pm)");
        repAyer.setRol("PRACTICANTE DE SOFTWARE Y PROGRAMACIÓN WEB");

        List<KpiActivityItem> actAyer = new ArrayList<>();
        actAyer.add(new KpiActivityItem("ACT-10", "01", "Webpuntos", "Desarrollo", "Diseño inicial del layout", "08:00 am", "11:00 am", "Entregado", 0, "f2a1b90"));
        actAyer.add(new KpiActivityItem("ACT-11", "02", "Tienda Online", "Maquetación", "Header responsive", "11:15 am", "01:30 pm", "Entregado", 0, "d4c8e11"));
        repAyer.setActividades(actAyer);
        repAyer.setNivelProductividad("EXCELENTE");
        repAyer.setPuntaje(20);
        list.add(repAyer);
        return list;
    }

    public static KpiSalesReport createSalesMockReport() {
        KpiSalesReport report = new KpiSalesReport();
        report.setId("REP-VEN-2025-0524");
        report.setFecha("24/05/2025");
        String salesAuthUser = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        boolean isSalesPracticante = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String salesPrefix = isSalesPracticante ? "Practicante: " : "Colaborador: ";
        String salesName = (salesAuthUser != null && !salesAuthUser.isEmpty() && !salesAuthUser.equals("Colaborador WebSite+"))
                ? salesPrefix + salesAuthUser : (isSalesPracticante ? "Practicante Comercial" : "Asesor Comercial");
        report.setAsesor(salesName);
        report.setSupervisor("Ing. Jair Valdera (Supervisor de TI)");
        report.setTurno("Tarde (3:30 pm - 6:30 pm)");
        report.setRol(isSalesPracticante ? "PRACTICANTE DE VENTAS Y PROSPECCIÓN WEB" : "ASESOR DE VENTAS Y PROSPECCIÓN WEB");

        // Metas comerciales
        List<KpiProductionGoal> metas = new ArrayList<>();
        metas.add(new KpiProductionGoal("Prospección en frío", "15 prospectos", "dev", R.color.kpi_cat_desarrollo, R.color.kpi_cat_desarrollo_bg));
        metas.add(new KpiProductionGoal("Llamadas de seguimiento", "8 llamadas", "api", R.color.kpi_cat_api, R.color.kpi_cat_api_bg));
        metas.add(new KpiProductionGoal("Reuniones / Demos web", "2 a 3 citas", "improve", R.color.kpi_cat_mejora, R.color.kpi_cat_mejora_bg));
        metas.add(new KpiProductionGoal("Cotizaciones enviadas", "2 propuestas", "layout", R.color.kpi_cat_maquetacion, R.color.kpi_cat_maquetacion_bg));
        metas.add(new KpiProductionGoal("Cierres de venta", "1 plan web", "maint", R.color.kpi_cat_mantenimiento, R.color.kpi_cat_mantenimiento_bg));
        report.setMetas(metas);

        // Gestiones comerciales
        List<KpiSalesActivityItem> gestiones = new ArrayList<>();
        gestiones.add(new KpiSalesActivityItem("GES-01", "01", "Restaurante El Pacífico", "Prospección", "Contacto inicial con administrador vía WhatsApp y envío de brochure", "03:30 pm", "04:10 pm", "Concretado", 0.0));
        gestiones.add(new KpiSalesActivityItem("GES-02", "02", "Clínica San Juan", "Cotización", "Elaboración y envío de propuesta para portal de reservas médicas", "04:15 pm", "05:00 pm", "Concretado", 1850.0));
        gestiones.add(new KpiSalesActivityItem("GES-03", "03", "Boutique Glamour", "Cierre de venta", "Firma de contrato y recepción de adelanto para tienda virtual", "05:05 pm", "05:45 pm", "Concretado", 2400.0));
        gestiones.add(new KpiSalesActivityItem("GES-04", "04", "Inmobiliaria Moderna", "Salida a campo", "Reunión presencial en sus oficinas para presentación de demo comercial", "05:50 pm", "06:30 pm", "Salida a campo", 3500.0));
        report.setGestiones(gestiones);

        // Criterios
        Map<String, Boolean> criterios = new LinkedHashMap<>();
        criterios.put("Cumplí meta de llamadas/contactos", true);
        criterios.put("Presenté portafolio de WebSite+", true);
        criterios.put("Registré información en CRM", true);
        criterios.put("Envié cotizaciones formales", true);
        criterios.put("Hice seguimiento a propuestas", true);
        criterios.put("Identifiqué necesidades del cliente", true);
        report.setCriteriosEvaluacion(criterios);

        report.setHuboDificultad(false);
        report.setDetalleDificultad("Sin inconvenientes en la jornada comercial.");
        report.setObservacionSupervisor("Excelente desempeño en la jornada comercial.");
        report.setFirmaAsesor(salesAuthUser != null && !salesAuthUser.isEmpty() && !salesAuthUser.equals("Colaborador WebSite+") ? salesAuthUser : salesName);
        report.setFirmaSupervisor("Carlos Ramírez");
        report.setNivelProductividad("EXCELENTE");
        report.setPuntaje(20);

        return report;
    }
}
