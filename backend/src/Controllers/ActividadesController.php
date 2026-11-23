<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

final class ActividadesController
{
    /**
     * GET /api/v1/actividades
     * Filtra por día, semana, mes o rango personalizado.
     */
    public function index(Request $request, array $session): void
    {
        Authorization::require($session, 'actividad.ver', (int) $session['id_trabajador']);
        $db = Database::getConnection();

        $workerId = $request->getQuery('id_trabajador');
        $fecha = $request->getQuery('fecha');
        $fechaInicio = $request->getQuery('desde');
        $fechaFin = $request->getQuery('hasta');
        $estado = $request->getQuery('estado');

        // Si es rol colaborador o practicante, solo ver sus propias actividades
        if (!Authorization::allows($session, 'actividad.ver')) {
            Authorization::require($session, 'actividad.crear', (int) $session['id_trabajador']);
        $workerId = $session['id_trabajador'];
        }

        $sql = '
            SELECT a.*, CONCAT(t.nombres, " ", t.apellidos) AS trabajador_nombre,
                   c.nombre AS cargo_nombre,
                   e.id_kpi, d.nombre AS kpi_nombre
            FROM wp_actividades a
            JOIN trabajadores t ON a.id_trabajador = t.id_trabajador
            LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
            LEFT JOIN wp_kpi_evaluaciones e ON a.id_evaluacion_kpi = e.id_evaluacion
            LEFT JOIN wp_kpi_definiciones d ON e.id_kpi = d.id_kpi
            WHERE 1=1
        ';
        $params = [];

        if (!empty($workerId)) {
            $sql .= ' AND a.id_trabajador = ?';
            $params[] = (int) $workerId;
        }
        if (!empty($fecha)) {
            $sql .= ' AND a.fecha_actividad = ?';
            $params[] = $fecha;
        } elseif (!empty($fechaInicio) && !empty($fechaFin)) {
            $sql .= ' AND a.fecha_actividad BETWEEN ? AND ?';
            $params[] = $fechaInicio;
            $params[] = $fechaFin;
        }
        if (!empty($estado)) {
            $sql .= ' AND a.estado = ?';
            $params[] = $estado;
        }

        $sql .= ' ORDER BY a.fecha_actividad DESC, a.creado_en DESC LIMIT 100';

        $stmt = $db->prepare($sql);
        $stmt->execute($params);
        $actividades = $stmt->fetchAll();

        Response::success([
            'total'       => count($actividades),
            'actividades' => $actividades,
        ]);
    }

    /**
     * POST /api/v1/actividades
     * Registra una nueva actividad laboral.
     */
    public function store(Request $request, array $session): void
    {
        Authorization::require($session, 'actividad.crear', (int) $session['id_trabajador']);
        $workerId = $session['id_trabajador'];
        if (!$workerId) {
            Response::error('Usuario sin trabajador vinculado', 403, 'FORBIDDEN');
        }

        $titulo = trim((string) $request->input('titulo', ''));
        $descripcion = trim((string) $request->input('descripcion', ''));
        $fecha = (string) $request->input('fecha_actividad', date('Y-m-d'));
        $horaInicio = $request->input('hora_inicio');
        $horaFin = $request->input('hora_fin');
        $horas = $request->input('horas_dedicadas');
        $evalKpiId = $request->input('id_evaluacion_kpi');

        if (empty($titulo) || empty($descripcion)) {
            Response::error('Titulo y descripcion son requeridos', 422, 'VALIDATION_FAILED');
        }

        $db = Database::getConnection();

        if ($evalKpiId) {
            $check = $db->prepare('SELECT 1 FROM wp_kpi_evaluaciones WHERE id_evaluacion=? AND id_trabajador=?');
            $check->execute([$evalKpiId, $workerId]);
            if (!$check->fetchColumn()) { Response::error('Evaluacion no autorizada', 403); }
        }
        $stmt = $db->prepare('
            INSERT INTO wp_actividades (
                id_trabajador, id_evaluacion_kpi, titulo, descripcion,
                fecha_actividad, hora_inicio, hora_fin, horas_dedicadas,
                estado, creado_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, "Registrada", NOW())
        ');
        $stmt->execute([
            $workerId,
            $evalKpiId ? (int) $evalKpiId : null,
            $titulo,
            $descripcion,
            $fecha,
            $horaInicio,
            $horaFin,
            $horas,
        ]);

        $actId = (int) $db->lastInsertId();

        AuditService::log('actividad', 'actividad_creada', 'EXITO', (int) $session['id_usuario'], $session['username'], (int) $workerId, 'wp_actividades', $actId);

        Response::success(['id_actividad' => $actId], 'Actividad registrada exitosamente', 201);
    }
}
