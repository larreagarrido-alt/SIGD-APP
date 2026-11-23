package pe.net.ws.kpiweb.features.admin.personal.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeActivityItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeKpiItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EvaluationHistoryItem;

/**
 * Fuente de datos ficticia para pruebas del módulo Personal y detalle de colaborador.
 * Contiene 18 colaboradores con información laboral completa, indicadores KPI asignados,
 * historial de rendimiento y actividades con estados semánticos.
 * Totalmente desacoplada de MySQL, API PHP y módulos KPI de otros integrantes.
 */
public class MockPersonalDataSource implements PersonalDataSource {

    private final List<EmployeeItem> mockEmployees = new ArrayList<>();

    public MockPersonalDataSource() {
        initMockData();
    }

    private void initMockData() {
        mockEmployees.clear();

        // 1. Tecnología e Innovación - Carlos Mendoza (Tech Lead)
        mockEmployees.add(new EmployeeItem(
                "EMP-1001", "Carlos Mendoza", "Tech Lead & Principal Architect", "Tecnología e Innovación",
                EmployeeItem.WorkStatus.ACTIVO, "c.mendoza@website.pe", "+51 987 654 321",
                "CM", 0xFF8738F5, 94, 18, 16, 2,
                "15/01/2021", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Híbrida (3x2)", "Desempeño Sobresaliente",
                "+8 pp frente al período anterior", "16 de 18 evaluaciones aprobadas",
                4, 3, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 88, "Bueno"),
                        new EvaluationHistoryItem("Ago", 91, "Sobresaliente"),
                        new EvaluationHistoryItem("Set", 89, "Bueno"),
                        new EvaluationHistoryItem("Oct", 94, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-001", "Disponibilidad del Core", "Uptime de microservicios críticos en producción", "Porcentaje", "99.5%", "99.9%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-002", "Calidad de Código y Cobertura", "SonarQube code coverage y reducción de duplicación", "Porcentaje", "85%", "91%", "%", 91, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-003", "Tiempo Medio de Resolución (MTTR)", "Resolución de incidencias críticas de arquitectura", "Tiempo", "2.0 hrs", "1.4 hrs", "hrs", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-004", "Velocidad de Despliegue de Sprint", "Cumplimiento del cronograma de entregables técnicos", "Conteo", "12 entregas", "10 entregas", "entregas", 83, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-101", "Revisión arquitectónica de Microservicios", "Auditoría de estabilidad y escalabilidad en Kubernetes", "Arquitectura", "24/10/2026", "En progreso", 70, "Alta"),
                        new EmployeeActivityItem("ACT-102", "Hardening de Gateway API y OAuth2", "Revisión de vulnerabilidades y políticas de tokens", "Seguridad", "20/10/2026", "Completada", 100, "Alta"),
                        new EmployeeActivityItem("ACT-103", "Documentación de contratos REST y OpenAPI", "Especificaciones para integración de nuevos clientes", "Documentación", "28/10/2026", "Pendiente", 0, "Normal"),
                        new EmployeeActivityItem("ACT-104", "Migración de base de datos a clúster multi-región", "Validación previa de replicación en staging", "Infraestructura", "15/10/2026", "Completada", 100, "Media")
                )
        ));

        // 2. Tecnología e Innovación - Ana Silva (Frontend Lead)
        mockEmployees.add(new EmployeeItem(
                "EMP-1002", "Ana Silva", "Senior Frontend Engineer", "Tecnología e Innovación",
                EmployeeItem.WorkStatus.ACTIVO, "a.silva@website.pe", "+51 988 123 456",
                "AS", 0xFFBB75FF, 92, 14, 13, 1,
                "01/06/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Remota", "Excelente",
                "+4 pp frente al período anterior", "13 de 14 entregas aprobadas",
                4, 3, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 86, "Bueno"),
                        new EvaluationHistoryItem("Ago", 89, "Bueno"),
                        new EvaluationHistoryItem("Set", 90, "Excelente"),
                        new EvaluationHistoryItem("Oct", 92, "Excelente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-005", "Core Web Vitals LCP", "Largest Contentful Paint inferior a 1.8 segundos", "Tiempo", "1.8s", "1.3s", "seg", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-006", "Consistencia UI / Design Tokens", "Alineación de componentes con el Design System", "Porcentaje", "95%", "97%", "%", 97, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-007", "Resolución de Bugs de Interfaz", "Tasa de cierre de defectos de accesibilidad y UI", "Conteo", "15 casos", "14 casos", "casos", 93, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-008", "Migración a Microfrontends", "Avance de integración del nuevo módulo de administración", "Porcentaje", "80%", "65%", "%", 65, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-105", "Implementación de Design Tokens en Web", "Sincronización de paleta morada dark premium", "Diseño Web", "25/10/2026", "En progreso", 80, "Alta"),
                        new EmployeeActivityItem("ACT-106", "Refactorización de accesibilidad WCAG AA", "Mejora de contrastes y lectores de pantalla", "Accesibilidad", "18/10/2026", "Completada", 100, "Media"),
                        new EmployeeActivityItem("ACT-107", "Optimización de bundle size en producción", "Tree-shaking y reducción de librerías JS pesadas", "Performance", "29/10/2026", "Pendiente", 0, "Normal")
                )
        ));

        // 3. Tecnología e Innovación - Javier Paredes (Backend Specialist)
        mockEmployees.add(new EmployeeItem(
                "EMP-1003", "Javier Paredes", "Backend Specialist", "Tecnología e Innovación",
                EmployeeItem.WorkStatus.VACACIONES, "j.paredes@website.pe", "+51 977 445 566",
                "JP", 0xFF6366F1, 88, 12, 11, 1,
                "15/03/2021", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Presencial", "Bueno",
                "+1 pp frente al período anterior", "11 de 12 aprobadas",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 85, "Bueno"),
                        new EvaluationHistoryItem("Ago", 87, "Bueno"),
                        new EvaluationHistoryItem("Set", 88, "Bueno"),
                        new EvaluationHistoryItem("Oct", 88, "Bueno")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-009", "Latencia p95 de Endpoints", "Tiempo de respuesta promedio menor a 150ms", "Tiempo", "150ms", "110ms", "ms", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-010", "Eficiencia de Queries MySQL", "Optimización de índices y planes de ejecución", "Porcentaje", "90%", "92%", "%", 92, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-011", "Implementación de Caché Redis", "Tasa de aciertos de caché por encima del 80%", "Porcentaje", "80%", "72%", "%", 72, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-108", "Ajuste de índices en base de datos transaccional", "Reducción de bloqueos en tablas maestras", "Base de Datos", "14/10/2026", "Completada", 100, "Alta"),
                        new EmployeeActivityItem("ACT-109", "Actualización de dependencias de seguridad Spring", "Parcheo de vulnerabilidades CVE", "Mantenimiento", "30/10/2026", "Pendiente", 0, "Media")
                )
        ));

        // 4. Tecnología e Innovación - Sofía Vega (Mobile Developer)
        mockEmployees.add(new EmployeeItem(
                "EMP-1004", "Sofía Vega", "Mobile Developer (Android)", "Tecnología e Innovación",
                EmployeeItem.WorkStatus.ACTIVO, "s.vega@website.pe", "+51 966 332 211",
                "SV", 0xFFA855F7, 95, 16, 15, 1,
                "01/10/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Híbrida (3x2)", "Sobresaliente",
                "+6 pp frente al período anterior", "15 de 16 entregas aprobadas",
                4, 4, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 89, "Bueno"),
                        new EvaluationHistoryItem("Ago", 92, "Sobresaliente"),
                        new EvaluationHistoryItem("Set", 94, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 95, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-012", "Crash-Free Users", "Sesiones estables en app Android superiores al 99.5%", "Porcentaje", "99.5%", "99.8%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-013", "Tiempo de Inicio en Frío", "Startup time optimizado menor a 1.2 segundos", "Tiempo", "1.2s", "0.9s", "seg", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-014", "Consumo Eficiente de Memoria", "Uso de RAM menor a 140MB en vistas complejas", "Conteo", "140 MB", "115 MB", "MB", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-015", "Entregables de UI Móvil", "Pantallas del módulo implementadas con fidelidad", "Porcentaje", "100%", "100%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-110", "Pulido visual del módulo Personal y navegación", "Alineación pixel-perfect con diseño dark premium", "UI Móvil", "23/10/2026", "En progreso", 90, "Alta"),
                        new EmployeeActivityItem("ACT-111", "Pruebas de compatibilidad en diferentes DPIs", "Verificación en pantallas HD, FHD y tablets", "Testing", "21/10/2026", "Completada", 100, "Media"),
                        new EmployeeActivityItem("ACT-112", "Optimización de layouts anidados y Overdraw", "Eliminación de capas redundantes de renderizado", "Performance", "19/10/2026", "Completada", 100, "Alta")
                )
        ));

        // 5. Tecnología e Innovación - Diego Morales (QA Automation)
        mockEmployees.add(new EmployeeItem(
                "EMP-1005", "Diego Morales", "QA Automation Engineer", "Tecnología e Innovación",
                EmployeeItem.WorkStatus.ACTIVO, "d.morales@website.pe", "+51 955 889 900",
                "DM", 0xFF06B6D4, 90, 11, 10, 1,
                "01/02/2023", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Remota", "Excelente",
                "+3 pp frente al período anterior", "10 de 11 suites aprobadas",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 84, "Bueno"),
                        new EvaluationHistoryItem("Ago", 86, "Bueno"),
                        new EvaluationHistoryItem("Set", 88, "Bueno"),
                        new EvaluationHistoryItem("Oct", 90, "Excelente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-016", "Cobertura de Tests E2E", "Automatización de flujos de usuario en regresión", "Porcentaje", "80%", "84%", "%", 84, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-017", "Defect Leakage a Producción", "Defectos no detectados menor al 3%", "Porcentaje", "< 3%", "1.2%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-018", "Tiempo de Ejecución de Pipeline QA", "Ejecución completa en CI/CD menor a 15 min", "Tiempo", "15 min", "18 min", "min", 75, true, EmployeeKpiItem.KpiStatus.EN_RIESGO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-113", "Automatización de suite de pruebas de Personal", "Validación de filtros y búsqueda por código", "Automatización", "26/10/2026", "En progreso", 50, "Alta"),
                        new EmployeeActivityItem("ACT-114", "Validación cruzada de reportes y exportación", "Verificación de consistencia numérica", "QA", "17/10/2026", "Completada", 100, "Media")
                )
        ));

        // 6. Tecnología e Innovación - Elena Ramos (DevOps)
        mockEmployees.add(new EmployeeItem(
                "EMP-1006", "Elena Ramos", "DevOps & Cloud Engineer", "Tecnología e Innovación",
                EmployeeItem.WorkStatus.CAPACITACION, "e.ramos@website.pe", "+51 944 112 233",
                "ER", 0xFF10B981, 87, 9, 8, 1,
                "15/07/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Híbrida (3x2)", "En capacitación",
                "0 pp frente al período anterior", "8 de 9 tareas completadas",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 85, "Bueno"),
                        new EvaluationHistoryItem("Ago", 87, "Bueno"),
                        new EvaluationHistoryItem("Set", 87, "Bueno"),
                        new EvaluationHistoryItem("Oct", 87, "Bueno")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-019", "Tiempo de Despliegue CI/CD", "Duración de pipeline de build y deploy en AWS", "Tiempo", "8 min", "6 min", "min", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-020", "Disponibilidad de Clústeres K8s", "Uptime de nodos y réplicas de contenedores", "Porcentaje", "99.9%", "99.95%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-021", "Certificación Kubernetes CKA", "Culminación del programa corporativo de formación", "Porcentaje", "100%", "75%", "%", 75, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-115", "Módulo 4: Seguridad y Políticas en Kubernetes", "Programa de capacitación avanzada de Cloud", "Formación", "27/10/2026", "En progreso", 75, "Media"),
                        new EmployeeActivityItem("ACT-116", "Automatización de backups diarios en S3", "Verificación de integridad de snapshots", "Cloud", "12/10/2026", "Completada", 100, "Alta")
                )
        ));

        // 7. Diseño de Producto - Lucía Torres (Lead Product Designer)
        mockEmployees.add(new EmployeeItem(
                "EMP-1007", "Lucía Torres", "Lead Product Designer", "Diseño de Producto",
                EmployeeItem.WorkStatus.ACTIVO, "l.torres@website.pe", "+51 933 556 677",
                "LT", 0xFFEC4899, 96, 17, 16, 1,
                "01/03/2021", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Híbrida (3x2)", "Desempeño Sobresaliente",
                "+8 pp frente a septiembre", "16 de 17 entregas aprobadas",
                4, 4, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 90, "Sobresaliente"),
                        new EvaluationHistoryItem("Ago", 93, "Sobresaliente"),
                        new EvaluationHistoryItem("Set", 92, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 96, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-022", "Calidad de Diseño UI/UX", "Aprobación de prototipos y consistencia visual", "Porcentaje", "95%", "98%", "%", 98, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-023", "Entregas de Prototipos a Tiempo", "Cumplimiento del cronograma de sprint de diseño", "Porcentaje", "90%", "95%", "%", 95, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-024", "Satisfacción de Usuarios (CSAT)", "Puntaje en pruebas de usabilidad y feedback", "Puntos", "4.8 / 5.0", "4.9 / 5.0", "pts", 98, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-025", "Adopción de Design System", "Cobertura de componentes en Figma y Mobile", "Porcentaje", "100%", "100%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-117", "Prototipo de Onboarding y Flujo Mobile", "Diseño interactivo de bienvenida y permisos", "Diseño UI", "24/10/2026", "En progreso", 85, "Alta"),
                        new EmployeeActivityItem("ACT-118", "Guía de accesibilidad y contraste WCAG", "Documentación para equipo de desarrollo frontend", "Design System", "18/10/2026", "Completada", 100, "Media"),
                        new EmployeeActivityItem("ACT-119", "Revisión de pantallas del Administrador", "Auditoría visual de consistencia en paleta morada", "Revisión UI", "21/10/2026", "Completada", 100, "Alta")
                )
        ));

        // 8. Diseño de Producto - Mateo Benítez (UX Researcher)
        mockEmployees.add(new EmployeeItem(
                "EMP-1008", "Mateo Benítez", "UX Researcher & Service Designer", "Diseño de Producto",
                EmployeeItem.WorkStatus.ACTIVO, "m.benitez@website.pe", "+51 922 778 899",
                "MB", 0xFFF43F5E, 91, 13, 12, 1,
                "15/09/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Remota", "Excelente",
                "+5 pp frente al período anterior", "12 de 13 estudios culminados",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 85, "Bueno"),
                        new EvaluationHistoryItem("Ago", 87, "Bueno"),
                        new EvaluationHistoryItem("Set", 89, "Bueno"),
                        new EvaluationHistoryItem("Oct", 91, "Excelente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-026", "Entrevistas en Profundidad", "Estudios cualitativos con administradores de área", "Conteo", "12 sesiones", "14 sesiones", "sesiones", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-027", "Tasa de Insights Accionables", "Recomendaciones implementadas en el roadmap", "Porcentaje", "75%", "82%", "%", 82, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-028", "Evaluación Heurística de Flujos", "Análisis de fricciones en el flujo de consulta", "Porcentaje", "90%", "85%", "%", 85, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-120", "Informe de hallazgos del módulo Colaboradores", "Consolidado de feedback de administradores", "Research", "25/10/2026", "En progreso", 60, "Alta"),
                        new EmployeeActivityItem("ACT-121", "Test de usabilidad de búsqueda y filtros rápidos", "Medición de tiempo para encontrar trabajadores", "Usabilidad", "19/10/2026", "Completada", 100, "Media")
                )
        ));

        // 9. Diseño de Producto - Camila Ríos (UI Designer)
        mockEmployees.add(new EmployeeItem(
                "EMP-1009", "Camila Ríos", "UI Designer & Brand Specialist", "Diseño de Producto",
                EmployeeItem.WorkStatus.VACACIONES, "c.rios@website.pe", "+51 911 334 455",
                "CR", 0xFF8B5CF6, 89, 10, 9, 1,
                "01/11/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Híbrida (3x2)", "Bueno",
                "+2 pp frente al período anterior", "9 de 10 entregas aprobadas",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 85, "Bueno"),
                        new EvaluationHistoryItem("Ago", 88, "Bueno"),
                        new EvaluationHistoryItem("Set", 89, "Bueno"),
                        new EvaluationHistoryItem("Oct", 89, "Bueno")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-029", "Iconografía y Activos Visuales", "Entrega de assets SVG optimizados para Android", "Porcentaje", "95%", "95%", "%", 95, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-030", "Especificaciones de Hand-off", "Documentación de medidas y estilos para Devs", "Porcentaje", "90%", "92%", "%", 92, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-031", "Revisión de Microinteracciones", "Animaciones y feedback visual de botones", "Porcentaje", "80%", "70%", "%", 70, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-122", "Diseño de iconos vectoriales para tabs y métricas", "Assets con paleta violeta y estados seleccionados", "UI Design", "16/10/2026", "Completada", 100, "Alta"),
                        new EmployeeActivityItem("ACT-123", "Revisión de badges de estado laboral", "Pills con bordes y tipografía legible", "Design System", "31/10/2026", "Pendiente", 0, "Normal")
                )
        ));

        // 10. Datos y Analítica - Rodrigo Herrera (Lead Data Engineer)
        mockEmployees.add(new EmployeeItem(
                "EMP-1010", "Rodrigo Herrera", "Data Engineering Lead", "Datos y Analítica",
                EmployeeItem.WorkStatus.ACTIVO, "r.herrera@website.pe", "+51 900 667 788",
                "RH", 0xFF14B8A6, 93, 15, 14, 1,
                "15/04/2021", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Remota", "Sobresaliente",
                "+7 pp frente al período anterior", "14 de 15 pipelines OK",
                4, 3, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 86, "Bueno"),
                        new EvaluationHistoryItem("Ago", 88, "Bueno"),
                        new EvaluationHistoryItem("Set", 91, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 93, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-032", "Disponibilidad del Data Warehouse", "Uptime de bases analíticas y vistas materializadas", "Porcentaje", "99.8%", "99.9%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-033", "Tiempo de Carga ETL", "Ejecución de procesos nocturnos antes de las 06:00", "Tiempo", "06:00 AM", "05:15 AM", "hrs", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-034", "Calidad e Integridad de Datos", "Tasa de registros con validaciones aprobadas", "Porcentaje", "98%", "99.2%", "%", 99, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-035", "Migración de Modelos a dbt", "Modernización de transformaciones analíticas", "Porcentaje", "70%", "60%", "%", 60, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-124", "Optimización de pipeline de consolidado de colaboradores", "Mejora de tiempos de respuesta en consultas", "ETL", "26/10/2026", "En progreso", 70, "Alta"),
                        new EmployeeActivityItem("ACT-125", "Validación de esquema relacional en Staging", "Verificación de claves y restricciones", "Data Quality", "17/10/2026", "Completada", 100, "Media")
                )
        ));

        // 11. Datos y Analítica - Andrés Salazar (BI Analyst)
        mockEmployees.add(new EmployeeItem(
                "EMP-1011", "Andrés Salazar", "Business Intelligence Analyst", "Datos y Analítica",
                EmployeeItem.WorkStatus.ACTIVO, "a.salazar@website.pe", "+51 989 223 344",
                "AS", 0xFF0EA5E9, 91, 13, 12, 1,
                "01/08/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Híbrida (3x2)", "Excelente",
                "+4 pp frente al período anterior", "12 de 13 tableros validados",
                3, 3, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 85, "Bueno"),
                        new EvaluationHistoryItem("Ago", 87, "Bueno"),
                        new EvaluationHistoryItem("Set", 89, "Bueno"),
                        new EvaluationHistoryItem("Oct", 91, "Excelente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-036", "Exactitud de Métricas Ejecutivas", "Conciliación de cifras entre sistemas y reportes", "Porcentaje", "99%", "99.5%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-037", "Tiempo de Respuesta a Consultas BI", "Resolución de requerimientos analíticos ad-hoc", "Tiempo", "24 hrs", "16 hrs", "hrs", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-038", "Adopción de Dashboards Administrativos", "Usuarios activos mensuales en tableros de gestión", "Porcentaje", "85%", "88%", "%", 88, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-126", "Modelado de métricas de desempeño histórico", "Definición de vistas para gráficos de evolución", "Modelado BI", "24/10/2026", "En progreso", 80, "Alta"),
                        new EmployeeActivityItem("ACT-127", "Automatización de reporte de cierre mensual", "Generación programada y envío a gerencias", "Reportes", "18/10/2026", "Completada", 100, "Media")
                )
        ));

        // 12. Datos y Analítica - Claudia Méndez (ML Engineer - Pendiente de evaluación)
        mockEmployees.add(new EmployeeItem(
                "EMP-1012", "Claudia Méndez", "Machine Learning Engineer", "Datos y Analítica",
                EmployeeItem.WorkStatus.ACTIVO, "c.mendez@website.pe", "+51 978 556 611",
                "CM", 0xFF64748B, -1, 6, 2, 4,
                "01/10/2026", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Remota", "Pendiente de evaluación",
                "Nuevo ingreso este período", "Evaluación inicial programada",
                2, 0, 2,
                Collections.emptyList(), // Sin historial previo para comprobar estado vacío
                Arrays.asList(
                        new EmployeeKpiItem("KPI-039", "Despliegue del Modelo Predictivo de Asistencia", "Pruebas de inferencia y validación de datasets", "Porcentaje", "90%", "Pendiente", "%", 0, false, EmployeeKpiItem.KpiStatus.PENDIENTE),
                        new EmployeeKpiItem("KPI-040", "Monitoreo de Drift en Modelos de Productividad", "Configuración de alertas ante desviaciones de datos", "Porcentaje", "95%", "Pendiente", "%", 0, false, EmployeeKpiItem.KpiStatus.PENDIENTE)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-128", "Configuración de entorno de inferencia con FastAPI", "Despliegue en clúster de prueba interno", "Machine Learning", "28/10/2026", "En progreso", 40, "Alta"),
                        new EmployeeActivityItem("ACT-129", "Revisión de datasets anonimizados para entrenamiento", "Cumplimiento de políticas de privacidad", "Data Governance", "22/10/2026", "Completada", 100, "Media")
                )
        ));

        // 13. Operaciones - Gabriel Morales (Scrum Master)
        mockEmployees.add(new EmployeeItem(
                "EMP-1013", "Gabriel Morales", "Agile Coach & Scrum Master", "Operaciones",
                EmployeeItem.WorkStatus.ACTIVO, "g.morales@website.pe", "+51 967 990 011",
                "GM", 0xFFEAB308, 95, 15, 14, 1,
                "10/04/2021", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Híbrida (3x2)", "Sobresaliente",
                "+5 pp frente al período anterior", "14 de 15 sprints cerrados",
                4, 4, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 89, "Bueno"),
                        new EvaluationHistoryItem("Ago", 91, "Sobresaliente"),
                        new EvaluationHistoryItem("Set", 93, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 95, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-041", "Cumplimiento del Sprint Goal", "Porcentaje de compromisos de squad completados", "Porcentaje", "85%", "92%", "%", 92, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-042", "Resolución de Impedimentos Críticos", "Tiempo de desbloqueo de tareas de desarrollo", "Tiempo", "12 hrs", "7 hrs", "hrs", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-043", "Madurez Ágil del Squad", "Nivel de autogestión y estimación en retrospectivas", "Puntos", "4.0 / 5.0", "4.6 / 5.0", "pts", 92, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-044", "Predecibilidad de Entrega (Velocity)", "Estabilidad en Story Points entregados por sprint", "Porcentaje", "90%", "94%", "%", 94, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-130", "Facilitación de Sprint Planning Q4", "Alineación de objetivos y backlog con Tech Leads", "Agilidad", "24/10/2026", "Completada", 100, "Alta"),
                        new EmployeeActivityItem("ACT-131", "Monitoreo de métricas de flujo y velocidad", "Consolidado de cycle time y lead time", "Operaciones", "21/10/2026", "Completada", 100, "Media"),
                        new EmployeeActivityItem("ACT-132", "Taller de refinamiento de historias de usuario", "Capacitación en criterios de aceptación ágiles", "Formación", "29/10/2026", "En progreso", 50, "Normal")
                )
        ));

        // 14. Operaciones - Valeria Castro (Operations Manager - Con licencia)
        mockEmployees.add(new EmployeeItem(
                "EMP-1014", "Valeria Castro", "Operations Manager", "Operaciones",
                EmployeeItem.WorkStatus.LICENCIA, "v.castro@website.pe", "+51 991 223 388",
                "VC", 0xFFBB75FF, 84, 8, 7, 1,
                "01/07/2020", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Presencial", "Con licencia",
                "-2 pp por período de licencia", "7 de 8 procesos concluidos",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 86, "Bueno"),
                        new EvaluationHistoryItem("Ago", 85, "Bueno"),
                        new EvaluationHistoryItem("Set", 84, "Bueno"),
                        new EvaluationHistoryItem("Oct", 84, "Bueno")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-045", "Nivel de Servicio Operativo (SLA)", "Cumplimiento general de SLAs institucionales", "Porcentaje", "95%", "93%", "%", 93, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO),
                        new EmployeeKpiItem("KPI-046", "Eficiencia Presupuestal", "Desviación del presupuesto de operaciones", "Porcentaje", "< 5%", "3.2%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-047", "Continuidad Operativa", "Planes de contingencia aprobados por auditoría", "Porcentaje", "100%", "100%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-133", "Revisión presupuestal de licencias corporativas", "Control de costos de herramientas Cloud", "Gestión", "10/11/2026", "Pendiente", 0, "Alta"),
                        new EmployeeActivityItem("ACT-134", "Aprobación de cronograma de continuidad", "Validación con gerencia general", "Operaciones", "15/10/2026", "Completada", 100, "Media")
                )
        ));

        // 15. Operaciones - Ricardo Alarcón (Logistics)
        mockEmployees.add(new EmployeeItem(
                "EMP-1015", "Ricardo Alarcón", "Logistics & Facilities Coordinator", "Operaciones",
                EmployeeItem.WorkStatus.ACTIVO, "r.alarcon@website.pe", "+51 998 776 221",
                "RA", 0xFFD97706, 91, 14, 13, 1,
                "01/09/2022", "Sede Surco", "Plazo Indeterminado",
                "08:00 - 17:00", "Presencial", "Excelente",
                "+3 pp frente al período anterior", "13 de 14 entregas conformes",
                3, 2, 1,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 86, "Bueno"),
                        new EvaluationHistoryItem("Ago", 88, "Bueno"),
                        new EvaluationHistoryItem("Set", 90, "Excelente"),
                        new EvaluationHistoryItem("Oct", 91, "Excelente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-048", "Atención de Requerimientos de Sede", "Tiempo de entrega de equipamiento tecnológico", "Tiempo", "48 hrs", "32 hrs", "hrs", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-049", "Exactitud de Inventario Físico", "Concordancia entre activo registrado y físico", "Porcentaje", "98%", "99.1%", "%", 99, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-050", "Plan de Mantenimiento Preventivo", "Estaciones de trabajo con mantenimiento al día", "Porcentaje", "90%", "86%", "%", 86, true, EmployeeKpiItem.KpiStatus.EN_PROGRESO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-135", "Inventario físico de equipos asignados a Personal", "Verificación de laptops y periféricos", "Logística", "29/10/2026", "En progreso", 65, "Alta"),
                        new EmployeeActivityItem("ACT-136", "Mantenimiento preventivo de estaciones de trabajo", "Limpieza técnica y reemplazo de insumos", "Servicios", "20/10/2026", "Completada", 100, "Media")
                )
        ));

        // 16. Seguridad - Fernando Alva (Cybersecurity Specialist)
        mockEmployees.add(new EmployeeItem(
                "EMP-1016", "Fernando Alva", "Cybersecurity Specialist", "Seguridad",
                EmployeeItem.WorkStatus.ACTIVO, "f.alva@website.pe", "+51 966 887 744",
                "FA", 0xFFEF4444, 93, 14, 13, 1,
                "15/01/2022", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Híbrida (3x2)", "Sobresaliente",
                "+4 pp frente al período anterior", "13 de 14 controles aprobados",
                3, 3, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 87, "Bueno"),
                        new EvaluationHistoryItem("Ago", 89, "Bueno"),
                        new EvaluationHistoryItem("Set", 91, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 93, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-051", "Tiempo de Respuesta a Incidentes (MTTR)", "Detección y neutralización de intentos de intrusión", "Tiempo", "30 min", "18 min", "min", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-052", "Puntuación de Hardening de Red", "Score de seguridad en análisis CIS Benchmarks", "Puntos", "90 / 100", "94 / 100", "pts", 94, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-053", "Remediación de Vulnerabilidades Críticas", "Parcheo en menos de 72 horas desde publicación", "Porcentaje", "95%", "98%", "%", 98, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-137", "Análisis de vulnerabilidades y hardening de red", "Escaneo periódico de puertos y certificados SSL", "Seguridad", "25/10/2026", "En progreso", 80, "Alta"),
                        new EmployeeActivityItem("ACT-138", "Simulacro de respuesta a incidentes de acceso", "Ejercicio de contención ante phishing interno", "Ciberseguridad", "19/10/2026", "Completada", 100, "Alta")
                )
        ));

        // 17. Seguridad - Lucía Villavicencio (Compliance Analyst)
        mockEmployees.add(new EmployeeItem(
                "EMP-1017", "Lucía Villavicencio", "SecOps & Compliance Analyst", "Seguridad",
                EmployeeItem.WorkStatus.ACTIVO, "l.villavicencio@website.pe", "+51 961 443 889",
                "LV", 0xFFDC2626, 95, 12, 11, 1,
                "01/02/2023", "Sede Central - San Isidro", "Plazo Indeterminado",
                "09:00 - 18:00", "Remota", "Sobresaliente",
                "+6 pp frente al período anterior", "11 de 12 auditorías conformes",
                3, 3, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 88, "Bueno"),
                        new EvaluationHistoryItem("Ago", 90, "Sobresaliente"),
                        new EvaluationHistoryItem("Set", 92, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 95, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-054", "Cumplimiento Normativo ISO 27001", "Porcentaje de controles de seguridad implementados", "Porcentaje", "95%", "97%", "%", 97, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-055", "Rotación Periódica de Credenciales", "Renovación de tokens de API y claves maestras", "Porcentaje", "100%", "100%", "%", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-056", "Auditoría de Permisos y Roles de Acceso", "Revisión de accesos de administrador y colaboradores", "Porcentaje", "95%", "98%", "%", 98, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-139", "Auditoría de cumplimiento normativo ISO 27001", "Preparación de evidencias para recertificación", "Compliance", "28/10/2026", "En progreso", 75, "Alta"),
                        new EmployeeActivityItem("ACT-140", "Rotación periódica de credenciales de servicio", "Actualización de secrets en AWS Secrets Manager", "Seguridad", "22/10/2026", "Completada", 100, "Alta")
                )
        ));

        // 18. Talento Humano - Daniela Espinoza (People Partner)
        mockEmployees.add(new EmployeeItem(
                "EMP-1018", "Daniela Espinoza", "Talent & People Partner", "Talento Humano",
                EmployeeItem.WorkStatus.ACTIVO, "d.espinoza@website.pe", "+51 990 112 334",
                "DE", 0xFF9333EA, 97, 16, 15, 1,
                "10/05/2021", "Sede Central - San Isidro", "Plazo Indeterminado",
                "08:30 - 17:30", "Híbrida (3x2)", "Desempeño Sobresaliente",
                "+8 pp frente al período anterior", "15 de 16 programas completados",
                4, 4, 0,
                Arrays.asList(
                        new EvaluationHistoryItem("Jul", 91, "Sobresaliente"),
                        new EvaluationHistoryItem("Ago", 93, "Sobresaliente"),
                        new EvaluationHistoryItem("Set", 95, "Sobresaliente"),
                        new EvaluationHistoryItem("Oct", 97, "Sobresaliente")
                ),
                Arrays.asList(
                        new EmployeeKpiItem("KPI-057", "Tiempo Medio de Cobertura de Vacantes", "Días transcurridos desde solicitud hasta contratación", "Tiempo", "30 días", "21 días", "días", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-058", "Satisfacción de Clima y Cultura", "Índice de eNPS interno entre los colaboradores", "Puntos", "75 pts", "84 pts", "pts", 100, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-059", "Cumplimiento del Plan de Capacitación", "Colaboradores con cursos obligatorios completados", "Porcentaje", "90%", "96%", "%", 96, true, EmployeeKpiItem.KpiStatus.CUMPLIDO),
                        new EmployeeKpiItem("KPI-060", "Retención de Talento Clave", "Porcentaje de retención en roles técnicos e intermedios", "Porcentaje", "92%", "96.5%", "%", 96, true, EmployeeKpiItem.KpiStatus.CUMPLIDO)
                ),
                Arrays.asList(
                        new EmployeeActivityItem("ACT-141", "Programa de integración para nuevas contrataciones", "Onboarding estructurado y asignación de mentores", "Gestión Humana", "27/10/2026", "En progreso", 85, "Alta"),
                        new EmployeeActivityItem("ACT-142", "Consolidado de asistencia y permisos autorizados", "Conciliación de marcaciones y justificaciones", "Laboral", "23/10/2026", "Completada", 100, "Media"),
                        new EmployeeActivityItem("ACT-143", "Encuesta de clima laboral Q4", "Lanzamiento y seguimiento de participación", "Cultura", "15/10/2026", "Completada", 100, "Alta")
                )
        ));
    }

    @Override
    public List<EmployeeItem> getAllEmployees() {
        return Collections.unmodifiableList(mockEmployees);
    }

    @Override
    public EmployeeItem getEmployeeById(String id) {
        if (id == null) return null;
        for (EmployeeItem item : mockEmployees) {
            if (item.getId().equalsIgnoreCase(id.trim())) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<DepartmentSummary> getDepartmentSummaries() {
        Map<String, int[]> map = new LinkedHashMap<>();
        for (EmployeeItem item : mockEmployees) {
            String dept = item.getDepartment();
            int[] counts = map.computeIfAbsent(dept, k -> new int[2]);
            counts[0]++; // Total en el departamento
            if (item.getWorkStatus() == EmployeeItem.WorkStatus.ACTIVO) {
                counts[1]++; // Activos en el departamento
            }
        }

        List<DepartmentSummary> summaries = new ArrayList<>();
        summaries.add(new DepartmentSummary("Todos", mockEmployees.size(), countActive()));
        for (Map.Entry<String, int[]> entry : map.entrySet()) {
            summaries.add(new DepartmentSummary(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }
        return summaries;
    }

    private int countActive() {
        int count = 0;
        for (EmployeeItem item : mockEmployees) {
            if (item.getWorkStatus() == EmployeeItem.WorkStatus.ACTIVO) {
                count++;
            }
        }
        return count;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String nfd = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT).trim();
    }

    @Override
    public List<EmployeeItem> filterEmployees(String query, String department, EmployeeItem.WorkStatus status) {
        String cleanQuery = normalize(query);
        boolean filterDept = (department != null && !department.isEmpty() && !"Todos".equalsIgnoreCase(department));
        String normDept = normalize(department);

        List<EmployeeItem> result = new ArrayList<>();
        for (EmployeeItem item : mockEmployees) {
            // Filtro por departamento
            if (filterDept && !normalize(item.getDepartment()).equalsIgnoreCase(normDept)) {
                continue;
            }

            // Filtro por estado
            if (status != null && item.getWorkStatus() != status) {
                continue;
            }

            // Búsqueda insensible a mayúsculas y tildes (nombre, rol o código de colaborador)
            if (!cleanQuery.isEmpty()) {
                boolean matchName = normalize(item.getName()).contains(cleanQuery);
                boolean matchRole = normalize(item.getRole()).contains(cleanQuery);
                boolean matchCode = normalize(item.getId()).contains(cleanQuery);
                if (!matchName && !matchRole && !matchCode) {
                    continue;
                }
            }

            result.add(item);
        }
        return result;
    }

    @Override
    public int getActiveCount() {
        return countActive();
    }

    @Override
    public int getVacationCount() {
        int count = 0;
        for (EmployeeItem item : mockEmployees) {
            if (item.getWorkStatus() == EmployeeItem.WorkStatus.VACACIONES) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTrainingAndLeaveCount() {
        int count = 0;
        for (EmployeeItem item : mockEmployees) {
            if (item.getWorkStatus() == EmployeeItem.WorkStatus.CAPACITACION
                    || item.getWorkStatus() == EmployeeItem.WorkStatus.LICENCIA) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTotalCount() {
        return mockEmployees.size();
    }
}
