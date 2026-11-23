package pe.net.ws.kpiweb.features.collaborator.dashboard.data;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorActivityItem;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorKpiItem;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorPerformanceSummary;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorPersonalStats;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;

/**
 * Fuente de datos ficticios determinista para el Colaborador.
 * Proporciona 3 perfiles de prueba independientes para verificar el comportamiento
 * adaptativo de la interfaz según el cargo (Diseño, Ventas, Atención al cliente).
 */
public class CollaboratorMockDataSource {

    public enum DevProfileIndex {
        DISENO,
        VENTAS,
        ATENCION_CLIENTE
    }

    public CollaboratorProfile getProfile(DevProfileIndex index) {
        switch (index) {
            case DISENO:
                return createDesignProfile();
            case VENTAS:
                return createSalesProfile();
            case ATENCION_CLIENTE:
                return createSupportProfile();
            default:
                return createDesignProfile();
        }
    }

    private CollaboratorProfile createDesignProfile() {
        // 1. Rendimiento Hero (Figma inicio_colaborador.png)
        CollaboratorPerformanceSummary hero = new CollaboratorPerformanceSummary(
                "OCTUBRE 2026",
                "Tu esfuerzo\nmarca la diferencia.",
                "Vas +8 pp frente a septiembre.",
                94,
                "Entregas del mes • 12 de 14"
        );

        // 2. Cuatro Estadísticas Personales (Lucía: 1,240 pts -> Rango Oro, Ranking #3)
        CollaboratorPersonalStats stats = new CollaboratorPersonalStats(
                "1,240", "↑ +180 esta semana",
                "#3", "↑ +2 posiciones",
                "12/14", "86% completado", 86,
                "95%", "Excelente"
        );

        // 3. Próxima Entrega
        CollaboratorActivityItem nextActivity = new CollaboratorActivityItem(
                "act_101",
                "Prototipo de onboarding",
                "Diseño de producto • KPI de entregas",
                "Hoy • 18:00",
                "En curso",
                "Preparar entrega",
                CollaboratorActivityItem.ActivityType.ENTREGA
        );

        // 4. Actividad Reciente
        List<CollaboratorActivityItem> recent = new ArrayList<>();
        recent.add(new CollaboratorActivityItem(
                "rec_1", "KPI entregado", "Prototipo de onboarding móvil",
                "Hace 2 horas", "Completado", null, CollaboratorActivityItem.ActivityType.ENTREGA
        ));
        recent.add(new CollaboratorActivityItem(
                "rec_2", "Nuevo anuncio", "Actualización de fechas de evaluaciones",
                "Ayer, 16:30", "Informativo", null, CollaboratorActivityItem.ActivityType.ANUNCIO
        ));
        recent.add(new CollaboratorActivityItem(
                "rec_3", "Puntos acumulados", "+120 pts por entrega puntual de wireframes",
                "14 de octubre", "Acreditado", null, CollaboratorActivityItem.ActivityType.EVALUACION
        ));

        // 5. KPIs del Diseñador (5 asignados: 3 completados, 2 pendientes/en curso)
        List<CollaboratorKpiItem> kpis = new ArrayList<>();
        kpis.add(new CollaboratorKpiItem("kpi_1", "Entregables UI/UX del sprint", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 12, 14, "diseños", CollaboratorKpiItem.Status.EN_CURSO, "Hoy • 18:00"));
        kpis.add(new CollaboratorKpiItem("kpi_2", "Calidad de Design System", CollaboratorKpiItem.MeasurementType.PORCENTAJE, 96, 95, "%", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_3", "Entregas puntuales en Figma", CollaboratorKpiItem.MeasurementType.PORCENTAJE, 100, 100, "%", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_4", "Pruebas de usabilidad documentadas", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 3, 5, "sesiones", CollaboratorKpiItem.Status.PENDIENTE, "29 Oct • 17:00"));
        kpis.add(new CollaboratorKpiItem("kpi_5", "Auditoría de contraste y accesibilidad", CollaboratorKpiItem.MeasurementType.PORCENTAJE, 100, 100, "%", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));

        return new CollaboratorProfile(
                "collab_01",
                "Lucía",
                "Lucía Gómez",
                "Product Designer • UI/UX",
                "Diseño y Producto",
                "LG",
                hero,
                stats,
                nextActivity,
                recent,
                kpis
        );
    }

    private CollaboratorProfile createSalesProfile() {
        // 1. Rendimiento Hero Ventas
        CollaboratorPerformanceSummary hero = new CollaboratorPerformanceSummary(
                "OCTUBRE 2026",
                "Meta comercial\nen camino a superarse.",
                "Vas +14% vs. mes anterior.",
                84,
                "Ventas acumuladas • S/ 42.000 de S/ 50.000"
        );

        // 2. Cuatro Estadísticas Personales Ventas (Carlos: 1,620 pts -> Rango Platino, Ranking #1)
        CollaboratorPersonalStats stats = new CollaboratorPersonalStats(
                "1,620", "↑ +240 esta semana",
                "#1", "Líder del ranking",
                "15/18", "84% completado", 84,
                "98%", "Excelente"
        );

        // 3. Próxima Actividad Ventas
        CollaboratorActivityItem nextActivity = new CollaboratorActivityItem(
                "act_201",
                "Reunión de cierre con Grupo Nexus",
                "Comercial • Meta de conversión corporativa",
                "Mañana • 10:30",
                "Confirmada",
                "Ver mis objetivos",
                CollaboratorActivityItem.ActivityType.OBJETIVO
        );

        // 4. Actividad Reciente Ventas
        List<CollaboratorActivityItem> recent = new ArrayList<>();
        recent.add(new CollaboratorActivityItem(
                "rec_s1", "Venta concretada", "Cierre contrato anual TechCorp (S/ 18.000)",
                "Hace 3 horas", "Ganada", null, CollaboratorActivityItem.ActivityType.OBJETIVO
        ));
        recent.add(new CollaboratorActivityItem(
                "rec_s2", "Nuevo lead calificado", "Demo programada con Banco Sol",
                "Ayer, 11:00", "En seguimiento", null, CollaboratorActivityItem.ActivityType.OBJETIVO
        ));

        // 5. KPIs de Ventas (6 asignados: 3 completados, 2 en curso, 1 vencido)
        List<CollaboratorKpiItem> kpis = new ArrayList<>();
        kpis.add(new CollaboratorKpiItem("kpi_v1", "Cuota de ventas corporativas", CollaboratorKpiItem.MeasurementType.MONTO_FINANCIERO, 42000, 50000, "S/", CollaboratorKpiItem.Status.EN_CURSO, "Mañana • 10:30"));
        kpis.add(new CollaboratorKpiItem("kpi_v2", "Nuevos clientes B2B captados", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 12, 15, "clientes", CollaboratorKpiItem.Status.EN_CURSO, "30 Oct • 18:00"));
        kpis.add(new CollaboratorKpiItem("kpi_v3", "Tasa de conversión de demos", CollaboratorKpiItem.MeasurementType.PORCENTAJE, 30, 30, "%", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_v4", "Propuestas ejecutivas enviadas", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 18, 18, "propuestas", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_v5", "Renovación de cuentas clave", CollaboratorKpiItem.MeasurementType.PORCENTAJE, 100, 95, "%", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_v6", "Reactivación de cartera inactiva", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 3, 8, "cuentas", CollaboratorKpiItem.Status.VENCIDO, "Vencido ayer"));

        return new CollaboratorProfile(
                "collab_02",
                "Carlos",
                "Carlos Mendoza",
                "Senior Sales Executive",
                "Área Comercial",
                "CM",
                hero,
                stats,
                nextActivity,
                recent,
                kpis
        );
    }

    private CollaboratorProfile createSupportProfile() {
        // 1. Rendimiento Hero Soporte (Demuestra el caso "Pendiente de evaluación" sin datos inventados)
        CollaboratorPerformanceSummary hero = new CollaboratorPerformanceSummary(
                "OCTUBRE 2026",
                "Evaluación mensual\nen proceso de consolidación.",
                "Cálculo ponderado en curso.",
                null, // Score null -> "Pendiente de evaluación"
                "Casos resueltos del período • 142 de 150"
        );

        // 2. Cuatro Estadísticas Personales Soporte (Elena: 890 pts -> Rango Plata, Ranking #6)
        CollaboratorPersonalStats stats = new CollaboratorPersonalStats(
                "890", "↑ +110 esta semana",
                "#6", "↑ +1 posición",
                "142/150", "94% resueltos", 94,
                "92%", "Puntual"
        );

        // 3. Próxima Actividad Soporte
        CollaboratorActivityItem nextActivity = new CollaboratorActivityItem(
                "act_301",
                "Resolución de tickets críticos de soporte",
                "Operaciones • SLA de primera respuesta",
                "Jueves • 15:00",
                "Prioridad alta",
                "Consultar indicadores",
                CollaboratorActivityItem.ActivityType.REVISION
        );

        // 4. Actividad Reciente Soporte
        List<CollaboratorActivityItem> recent = new ArrayList<>();
        recent.add(new CollaboratorActivityItem(
                "rec_cs1", "Ticket Nivel 3 cerrado", "Incidente de sincronización resuelto",
                "Hace 45 min", "Resuelto", null, CollaboratorActivityItem.ActivityType.REVISION
        ));
        recent.add(new CollaboratorActivityItem(
                "rec_cs2", "Encuesta CSAT 5/5", "Excelente atención recibida por usuario",
                "Hoy, 09:15", "Calificado", null, CollaboratorActivityItem.ActivityType.EVALUACION
        ));

        // 5. KPIs de Soporte (4 asignados: 2 completados, 2 pendientes/en curso)
        List<CollaboratorKpiItem> kpis = new ArrayList<>();
        kpis.add(new CollaboratorKpiItem("kpi_c1", "Tickets de soporte resueltos", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 142, 150, "tickets", CollaboratorKpiItem.Status.EN_CURSO, "Jueves • 15:00"));
        kpis.add(new CollaboratorKpiItem("kpi_c2", "Tiempo medio de primera respuesta", CollaboratorKpiItem.MeasurementType.TIEMPO, 8, 10, "min", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_c3", "Índice de satisfacción CSAT", CollaboratorKpiItem.MeasurementType.PUNTUACION, 4.9, 5.0, "/5", CollaboratorKpiItem.Status.COMPLETADO, "Completado"));
        kpis.add(new CollaboratorKpiItem("kpi_c4", "Documentación en base de conocimiento", CollaboratorKpiItem.MeasurementType.ENTREGABLES, 6, 10, "guías", CollaboratorKpiItem.Status.PENDIENTE, "31 Oct • 17:00"));

        return new CollaboratorProfile(
                "collab_03",
                "Elena",
                "Elena Rojas",
                "Customer Success Specialist",
                "Atención al Cliente",
                "ER",
                hero,
                stats,
                nextActivity,
                recent,
                kpis
        );
    }
}
