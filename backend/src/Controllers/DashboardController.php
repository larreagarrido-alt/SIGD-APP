<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

final class DashboardController
{
    /**
     * GET /api/v1/dashboard/resumen
     * Resumen integral para la pantalla de Administración y Bienestar de tu amiga.
     */
    public function resumen(Request $request, array $session): void
    {
        Authorization::require($session, 'dashboard.admin');
        \WebSitePlus\Auth\LaborPolicy::syncLeaves();
        $db = Database::getConnection();
        $periodo = $request->getQuery('periodo', date('o-\WW')); // Año ISO y semana ISO
        $hoy = date('Y-m-d');
        $inicioSemana = date('Y-m-d', strtotime('monday this week'));

        // 1. Métricas de Personal (desde vista analítica)
        $stPersonal = $db->query('
            SELECT
                COUNT(*) AS total_empleados,
                SUM(CASE WHEN tipo_trabajador = "Colaborador" AND condicion_operativa = "OPERATIVO" THEN 1 ELSE 0 END) AS colaboradores_activos,
                SUM(CASE WHEN tipo_trabajador = "Practicante" AND condicion_operativa = "OPERATIVO" THEN 1 ELSE 0 END) AS practicantes_activos,
                SUM(CASE WHEN condicion_operativa = "INACTIVO" THEN 1 ELSE 0 END) AS personal_inactivo,
                SUM(CASE WHEN condicion_operativa = "DE_VACACIONES_O_PERMISO" THEN 1 ELSE 0 END) AS personal_de_permiso,
                SUM(CASE WHEN condicion_operativa = "DESVINCULADO" THEN 1 ELSE 0 END) AS desvinculados
            FROM vw_wp_empleados_estado
        ');
        $personal = $stPersonal->fetch();

        // 2. Métricas de KPI en el periodo
        $stKpi = $db->prepare('
            SELECT
                COUNT(*) AS kpis_totales,
                COALESCE(SUM(CASE WHEN estado = "Aprobado" THEN 1 ELSE 0 END), 0) AS kpis_aprobados,
                COALESCE(SUM(CASE WHEN estado = "Entregado" THEN 1 ELSE 0 END), 0) AS kpis_entregados,
                COALESCE(SUM(CASE WHEN estado = "Pendiente" THEN 1 ELSE 0 END), 0) AS kpis_pendientes,
                COALESCE(SUM(CASE WHEN estado = "Vencido" THEN 1 ELSE 0 END), 0) AS kpis_vencidos,
                COALESCE(SUM(CASE WHEN estado = "No exigible" THEN 1 ELSE 0 END), 0) AS kpis_no_exigibles,
                ROUND(AVG(CASE WHEN estado NOT IN ("No exigible", "Cancelado") THEN COALESCE(porcentaje_cumplimiento, 0) ELSE NULL END), 2) AS promedio_cumplimiento
            FROM wp_kpi_evaluaciones
            WHERE periodo_clave = ?
        ');
        $stKpi->execute([$periodo]);
        $kpiMetricas = $stKpi->fetch();
        // Raw reports are shown separately; they are not approved KPI evaluations or points.
        $formulariosRecibidos = (int) $db->query('SELECT COUNT(*) FROM wp_kpi_formularios_detalle')->fetchColumn();

        // 3. Métricas de Actividades (Hoy y Semana)
        $stAct = $db->prepare('
            SELECT
                COALESCE(SUM(CASE WHEN fecha_actividad = ? THEN 1 ELSE 0 END), 0) AS actividades_hoy,
                COALESCE(SUM(CASE WHEN fecha_actividad >= ? THEN 1 ELSE 0 END), 0) AS actividades_esta_semana,
                COALESCE(SUM(CASE WHEN estado = "Registrada" THEN 1 ELSE 0 END), 0) AS actividades_pendientes_revision
            FROM wp_actividades
        ');
        $stAct->execute([$hoy, $inicioSemana]);
        $actividades = $stAct->fetch();

        // 4. Ranking Top 5 (por puntos de KPI aprobados en el mes)
        $periodoMes = date('Y-m');
        $stRank = $db->prepare('
            SELECT
                t.id_trabajador,
                CONCAT(t.nombres, " ", t.apellidos) AS nombre_completo,
                t.foto_perfil,
                c.nombre AS cargo_nombre,
                COALESCE(SUM(e.puntos_otorgados), 0) AS puntos_mes,
                ROUND(AVG(COALESCE(e.porcentaje_cumplimiento, 0)), 1) AS cumplimiento_promedio
            FROM trabajadores t
            JOIN cargos c ON t.id_cargo = c.id_cargo
            LEFT JOIN wp_kpi_evaluaciones e ON t.id_trabajador = e.id_trabajador AND e.estado = "Aprobado" AND e.fecha_calificacion >= ? AND e.fecha_calificacion < ?
            WHERE t.estado = "Activo"
            GROUP BY t.id_trabajador, t.nombres, t.apellidos, t.foto_perfil, c.nombre
            ORDER BY puntos_mes DESC, cumplimiento_promedio DESC
            LIMIT 5
        ');
        $stRank->execute([$periodoMes . '-01', date('Y-m-01', strtotime('first day of next month'))]);
        $topRanking = $stRank->fetchAll();

        Response::success([
            'periodo_consultado' => $periodo,
            'personal'           => $personal,
            'kpi'                => $kpiMetricas,
            'formularios_recibidos_total' => $formulariosRecibidos,
            'actividades'        => $actividades,
            'top_ranking'        => $topRanking,
        ]);
    }
}
