<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\ChallengeService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

final class KpiController
{
    /**
     * GET /api/v1/kpi/mis-kpis
     * Lista evaluaciones asignadas al trabajador autenticado para un periodo (o periodo actual).
     */
    public function misKpis(Request $request, array $session): void
    {
        Authorization::require($session, 'kpi.ver', (int) $session['id_trabajador']);
        \WebSitePlus\Auth\LaborPolicy::syncLeaves((int) $session['id_trabajador']);
        $workerId = $session['id_trabajador'];
        if (!$workerId) {
            Response::error('Este usuario no tiene un trabajador vinculado', 403, 'FORBIDDEN');
        }

        $periodo = $request->getQuery('periodo', date('o-\WW')); // Año ISO y semana ISO
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT e.*, d.codigo AS kpi_codigo, d.version AS kpi_version,
                   d.nombre AS kpi_nombre, d.descripcion AS kpi_descripcion,
                   d.tipo_metrica, d.unidad_medida, d.requiere_evidencia, d.requiere_firma,
                   cat.nombre AS categoria_nombre, cat.icono AS categoria_icono
            FROM wp_kpi_evaluaciones e
            JOIN wp_kpi_definiciones d ON e.id_kpi = d.id_kpi
            JOIN wp_kpi_categorias cat ON d.id_categoria = cat.id_categoria
            WHERE e.id_trabajador = ?
              AND (e.periodo_clave = ? OR ? = "")
            ORDER BY e.fecha_limite ASC
        ');
        $stmt->execute([$workerId, $periodo, $periodo]);
        $kpis = $stmt->fetchAll();

        Response::success([
            'id_trabajador' => (int) $workerId,
            'periodo'       => $periodo,
            'total'         => count($kpis),
            'evaluaciones'  => $kpis,
        ]);
    }

    /**
     * GET /api/v1/kpi/evaluaciones/{id}
     * Detalle de una evaluación con criterios y evidencias.
     */
    public function detalle(Request $request, array $session, int $evalId): void
    {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT e.*, d.nombre AS kpi_nombre, d.descripcion AS kpi_descripcion,
                   d.tipo_metrica, d.unidad_medida, d.requiere_evidencia, d.requiere_firma,
                   t.nombres, t.apellidos
            FROM wp_kpi_evaluaciones e
            JOIN wp_kpi_definiciones d ON e.id_kpi = d.id_kpi
            JOIN trabajadores t ON e.id_trabajador = t.id_trabajador
            WHERE e.id_evaluacion = ?
            LIMIT 1
        ');
        $stmt->execute([$evalId]);
        $eval = $stmt->fetch();

        if (!$eval) {
            Response::error('Evaluacion no encontrada', 404, 'NOT_FOUND');
        }

        // Validar propiedad si es colaborador/practicante
        if (!Authorization::allows($session, 'kpi.ver', (int) $eval['id_trabajador'])) {
            Response::error('No tienes autorizacion para consultar esta evaluacion', 403, 'FORBIDDEN');
        }

        // Criterios / rúbrica
        $stCrit = $db->prepare('SELECT * FROM wp_kpi_criterios WHERE id_kpi = ? ORDER BY rango_min ASC');
        $stCrit->execute([$eval['id_kpi']]);
        $criterios = $stCrit->fetchAll();

        // Evidencias adjuntas
        $stEvid = $db->prepare('
            SELECT ev.id_evidencia, ev.descripcion, ev.subido_en,
                   a.nombre_original, a.mime, a.tamano_bytes, a.uuid
            FROM wp_kpi_evidencias ev
            JOIN wp_archivos a ON ev.id_archivo = a.id_archivo
            WHERE ev.id_evaluacion = ?
        ');
        $stEvid->execute([$evalId]);
        $evidencias = $stEvid->fetchAll();

        Response::success([
            'evaluacion' => $eval,
            'criterios'  => $criterios,
            'evidencias' => $evidencias,
        ]);
    }

    /**
     * POST /api/v1/kpi/evaluaciones/{id}/borrador
     * Guarda el avance o borrador sin cerrar la entrega.
     */
    public function guardarBorrador(Request $request, array $session, int $evalId): void
    {
        Authorization::require($session, 'kpi.entregar', (int) $session['id_trabajador']);
        \WebSitePlus\Auth\LaborPolicy::syncLeaves((int) $session['id_trabajador']);
        $workerId = (int) $session['id_trabajador'];
        $valor = $request->input('valor_reportado');
        if ($valor !== null && (!is_numeric($valor) || !is_finite((float) $valor) || (float) $valor < 0)) { Response::error('Valor invalido', 422); }
        $comentarios = $request->input('comentarios_entrega');

        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT id_evaluacion, estado, id_trabajador FROM wp_kpi_evaluaciones WHERE id_evaluacion = ? LIMIT 1');
        $stmt->execute([$evalId]);
        $eval = $stmt->fetch();

        if (!$eval || (int) $eval['id_trabajador'] !== $workerId) {
            Response::error('Evaluacion no encontrada o no autorizada', 404, 'NOT_FOUND');
        }

        if (in_array($eval['estado'], ['Entregado', 'Aprobado', 'No exigible', 'Cancelado'], true)) {
            Response::error('No se puede modificar una evaluacion en estado ' . $eval['estado'], 400, 'STATE_CONFLICT');
        }

        $upd = $db->prepare('
            UPDATE wp_kpi_evaluaciones SET
                valor_reportado = ?,
                comentarios_entrega = ?,
                estado = "En progreso",
                fecha_primer_borrador = COALESCE(fecha_primer_borrador, NOW()),
                actualizado_en = NOW()
            WHERE id_evaluacion = ? AND estado IN ("Pendiente","En progreso","Observado","Vencido")
        ');
        $upd->execute([$valor, $comentarios, $evalId]);
        if ($upd->rowCount() !== 1) { Response::error('Estado concurrente', 409); }

        Response::success(null, 'Borrador guardado exitosamente');
    }

    /**
     * POST /api/v1/kpi/evaluaciones/{id}/entregar
     * Confirma la entrega con verificación biométrica (firma criptográfica).
     */
    public function confirmarEntrega(Request $request, array $session, int $evalId): void
    {
        Authorization::require($session, 'kpi.entregar', (int) $session['id_trabajador']);
        \WebSitePlus\Auth\LaborPolicy::syncLeaves((int) $session['id_trabajador']);
        $workerId = (int) $session['id_trabajador'];
        $valor = $request->input('valor_reportado');
        $comentarios = (string) $request->input('comentarios_entrega', '');
        $challengeId = (int) $request->input('id_desafio', 0);
        $signatureBase64 = (string) $request->input('firma_base64', '');
        $deviceUid = (string) $request->getDeviceUid();

        if (!is_numeric($valor) || !is_finite((float) $valor) || (float) $valor < 0) {
            Response::error('El valor reportado es requerido', 422, 'VALIDATION_FAILED');
        }

        $db = Database::getConnection();
        $db->beginTransaction();

        $stmt = $db->prepare('
            SELECT e.*, d.requiere_firma, d.requiere_evidencia
            FROM wp_kpi_evaluaciones e
            JOIN wp_kpi_definiciones d ON e.id_kpi = d.id_kpi
            WHERE e.id_evaluacion = ?
            LIMIT 1 FOR UPDATE
        ');
        $stmt->execute([$evalId]);
        $eval = $stmt->fetch();

        if (!$eval || (int) $eval['id_trabajador'] !== $workerId) {
            Response::error('Evaluacion no encontrada o no autorizada', 404, 'NOT_FOUND');
        }

        if (in_array($eval['estado'], ['Entregado', 'Aprobado', 'No exigible', 'Cancelado'], true)) {
            Response::error('Esta evaluacion ya fue entregada o aprobada previamente', 400, 'ALREADY_SUBMITTED');
        }

        if ($eval['requiere_evidencia']) {
            $evidence=$db->prepare('SELECT COUNT(*) FROM wp_kpi_evidencias WHERE id_evaluacion=?');
            $evidence->execute([$evalId]);
            if (!(int)$evidence->fetchColumn()) { Response::error('Evidencia requerida',422,'EVIDENCE_REQUIRED'); }
        }
        // Digest del contenido de entrega para validar no-repudio
        $payloadDigest = hash('sha256', sprintf('EVAL:%d:VAL:%s:COMS:%s', $evalId, (string) $valor, $comentarios));

        // 1. Validar firma biométrica si el indicador lo exige
        $dispositivoId = null;
        if ((bool) $eval['requiere_firma']) {
            if ($challengeId <= 0 || empty($signatureBase64)) {
                Response::error('Esta entrega requiere confirmacion biometrica (id_desafio y firma_base64)', 422, 'BIOMETRIC_REQUIRED');
            }

            // Buscar dispositivo activo
            $stDev = $db->prepare('SELECT id_dispositivo, clave_publica FROM wp_dispositivos WHERE id_usuario = ? AND device_uid = ? AND estado = "ACTIVO" LIMIT 1');
            $stDev->execute([$session['id_usuario'], $deviceUid]);
            $device = $stDev->fetch();

            if (!$device) {
                Response::error('No tienes un dispositivo biometrico activo registrado para firmar', 403, 'DEVICE_NOT_AUTHORIZED');
            }
            $dispositivoId = (int) $device['id_dispositivo'];

            // Verificar firma criptográfica
            $valido = ChallengeService::verifyChallenge($challengeId, $signatureBase64, (int) $session['id_usuario'], $dispositivoId, $evalId, $payloadDigest);
            if (!$valido) {
                AuditService::log('kpi', 'entrega_firma_invalida', 'FALLO', (int) $session['id_usuario'], $session['username'], $workerId, 'wp_kpi_evaluaciones', $evalId);
                Response::error('Firma biometrica invalida o desafio vencido/reutilizado', 401, 'INVALID_SIGNATURE');
            }
        }

        // 2. Transacción de entrega
        Database::transaction(function (PDO $pdo) use ($evalId, $valor, $comentarios, $eval, $challengeId, $dispositivoId, $signatureBase64, $payloadDigest, $session) {
            // Actualizar evaluación a 'Entregado'
            $upd = $pdo->prepare('
                UPDATE wp_kpi_evaluaciones SET
                    valor_reportado = ?,
                    comentarios_entrega = ?,
                    estado = "Entregado",
                    fecha_entrega = NOW(),
                    actualizado_en = NOW()
                WHERE id_evaluacion = ? AND estado IN ("Pendiente","En progreso","Observado","Vencido")
            ');
            $upd->execute([$valor, $comentarios, $evalId]);
            if ($upd->rowCount() !== 1) { throw new \RuntimeException('Estado concurrente'); }

            // Guardar firma si hubo
            if ($dispositivoId && $challengeId > 0) {
                $stFirma = $pdo->prepare('
                    INSERT INTO wp_kpi_firmas (id_evaluacion, id_desafio, id_dispositivo, firma_base64, sha256_entrega, verificada, firmado_en)
                    VALUES (?, ?, ?, ?, ?, 1, NOW())
                ');
                $stFirma->execute([$evalId, $challengeId, $dispositivoId, $signatureBase64, $payloadDigest]);
            }

            // Historial de revisión
            $stRev = $pdo->prepare('
                INSERT INTO wp_kpi_revisiones (id_evaluacion, estado_anterior, estado_nuevo, motivo, observacion, id_usuario, creado_en)
                VALUES (?, ?, "Entregado", "Entrega confirmada por trabajador", ?, ?, NOW())
            ');
            $stRev->execute([$evalId, $eval['estado'], $comentarios, $session['id_usuario']]);
        });

        $db->commit();
        AuditService::log('kpi', 'entrega_confirmada', 'EXITO', (int) $session['id_usuario'], $session['username'], $workerId, 'wp_kpi_evaluaciones', $evalId, ['valor' => $valor]);

        Response::success(null, 'KPI entregado exitosamente');
    }

    /**
     * POST /api/v1/kpi/evaluaciones/{id}/calificar
     * Calificación por supervisor/admin (calcula cumplimiento y acredita puntos).
     */
    public function calificar(Request $request, array $session, int $evalId): void
    {
        $nuevoEstado = (string) $request->input('estado'); // 'Aprobado' u 'Observado'
        $observaciones = (string) $request->input('observaciones_supervisor', '');

        if (!in_array($nuevoEstado, ['Aprobado', 'Observado'], true)) {
            Response::error('Estado de calificacion invalido', 422, 'VALIDATION_FAILED');
        }

        $db = Database::getConnection();

        $stmt = $db->prepare('SELECT * FROM wp_kpi_evaluaciones WHERE id_evaluacion = ? LIMIT 1');
        $stmt->execute([$evalId]);
        $eval = $stmt->fetch();

        if (!$eval) {
            Response::error('Evaluacion no encontrada', 404, 'NOT_FOUND');
        }

        Authorization::require($session, 'kpi.revisar', (int) $eval['id_trabajador']);
        Database::transaction(function (PDO $pdo) use ($evalId, $eval, $nuevoEstado, $observaciones, $session) {
            $lock = $pdo->prepare('SELECT * FROM wp_kpi_evaluaciones WHERE id_evaluacion=? FOR UPDATE');
            $lock->execute([$evalId]); $eval = $lock->fetch();
            if ($eval['estado'] !== 'Entregado' || (int) $eval['id_trabajador'] === (int) $session['id_trabajador']) { throw new \WebSitePlus\Core\HttpException('Evaluacion no calificable'); }
            $closed = $pdo->prepare('SELECT 1 FROM wp_ranking_cierres WHERE periodo_clave=?');
            $closed->execute([substr((string)$eval['fecha_limite'],0,7)]);
            if ($closed->fetchColumn()) throw new \WebSitePlus\Core\HttpException('Periodo KPI ya cerrado',409,'PERIOD_CLOSED');
            $source = $pdo->prepare('SELECT l.payload_sha256,f.payload_sha256 AS actual_hash
                FROM wp_kpi_reporte_evaluaciones l JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                WHERE l.id_evaluacion=?');
            $source->execute([$evalId]);
            $linked = $source->fetchAll(PDO::FETCH_ASSOC);
            foreach ($linked as $sourceRow)
                if (!hash_equals((string)$sourceRow['payload_sha256'],(string)$sourceRow['actual_hash']))
                    throw new \WebSitePlus\Core\HttpException('El reporte fue modificado despues de la revision',409,'REPORT_CHANGED');
            $period = \WebSitePlus\Auth\LaborPolicy::period($eval);
            if (!$period) throw new \WebSitePlus\Core\HttpException('Periodo KPI sin calendario verificable',409,'CALENDAR_UNAVAILABLE');
            $coverage = \WebSitePlus\Auth\KpiLaborCalendar::periodCoverage((int)$eval['id_trabajador'],$period[0],$period[1]);
            if (!$coverage['tipo_historico_confirmado']) throw new \WebSitePlus\Core\HttpException('El tipo laboral de este periodo anterior no está acreditado en Asistencias',409,'LABOR_HISTORY_UNVERIFIED');
            if ($coverage['incidencias']>0) throw new \WebSitePlus\Core\HttpException('Hay faltas pendientes de revision en Asistencias',409,'ATTENDANCE_REVIEW_REQUIRED');
            if ($coverage['turnos_programados']===0) throw new \WebSitePlus\Core\HttpException('No hay turnos programados verificables',409,'CALENDAR_UNAVAILABLE');
            if ($coverage['turnos_exigibles']===0) throw new \WebSitePlus\Core\HttpException('El periodo no tiene turnos KPI exigibles',409,'KPI_NOT_REQUIRED');
            $metric=$pdo->prepare('SELECT tipo_metrica FROM wp_kpi_definiciones WHERE id_kpi=?');
            $metric->execute([(int)$eval['id_kpi']]);
            \WebSitePlus\Auth\PointsLock::acquire($pdo, (int) $eval['id_trabajador']);
            $meta = (float) $eval['meta_snapshot'];
            if ($metric->fetchColumn()==='NUMERICO' && $meta>0)
                $meta=max(0.01,round($meta*$coverage['turnos_exigibles']/$coverage['turnos_programados'],2));
            $valor = (float) $eval['valor_reportado'];
            // Calcular porcentaje en el servidor
            $pctCumplimiento = ($meta > 0) ? min(150.00, round(($valor / $meta) * 100, 2)) : 100.00;
            $puntosOtorgados = ($nuevoEstado === 'Aprobado')
                ? \WebSitePlus\Kpi\KpiPointsPolicy::forPercentage($pctCumplimiento) : 0;

            // Actualizar evaluación
            $upd = $pdo->prepare('
                UPDATE wp_kpi_evaluaciones SET
                    estado = ?,
                    meta_efectiva_snapshot = ?,
                    turnos_programados_snapshot = ?,
                    turnos_exigibles_snapshot = ?,
                    calendario_sha256 = ?,
                    porcentaje_cumplimiento = ?,
                    puntos_otorgados = ?,
                    calificado_por = ?,
                    fecha_calificacion = NOW(),
                    observaciones_supervisor = ?,
                    actualizado_en = NOW()
                WHERE id_evaluacion = ?
            ');
            $upd->execute([$nuevoEstado, $meta, $coverage['turnos_programados'], $coverage['turnos_exigibles'],
                $coverage['origen_sha256'], $pctCumplimiento, $puntosOtorgados, $session['id_usuario'], $observaciones, $evalId]);

            // Si es Aprobado y otorga puntos > 0, acreditar en wp_puntos_ledger
            if ($nuevoEstado === 'Aprobado' && $puntosOtorgados > 0) {
                // Obtener saldo actual
                $stSal = $pdo->prepare('SELECT saldo_posterior FROM wp_puntos_ledger WHERE id_trabajador = ? ORDER BY id_movimiento DESC LIMIT 1 FOR UPDATE');
                $stSal->execute([$eval['id_trabajador']]);
                $saldoAnt = (int) ($stSal->fetchColumn() ?: 0);
                $saldoPost = $saldoAnt + $puntosOtorgados;

                $stLedger = $pdo->prepare('
                    INSERT INTO wp_puntos_ledger (
                        id_trabajador, tipo_movimiento, puntos, saldo_anterior, saldo_posterior,
                        referencia_tipo, referencia_id, motivo, autorizado_por, creado_en
                    ) VALUES (?, "GANADO_KPI", ?, ?, ?, "kpi_evaluacion", ?, "KPI Aprobado", ?, NOW())
                ');
                $stLedger->execute([
                    $eval['id_trabajador'],
                    $puntosOtorgados,
                    $saldoAnt,
                    $saldoPost,
                    $evalId,
                    $session['id_usuario'],
                ]);
            }

            // Registrar revisión inmutable
            $stRev = $pdo->prepare('
                INSERT INTO wp_kpi_revisiones (id_evaluacion, estado_anterior, estado_nuevo, motivo, observacion, id_usuario, creado_en)
                VALUES (?, ?, ?, "Calificacion por supervisor", ?, ?, NOW())
            ');
            $stRev->execute([$evalId, $eval['estado'], $nuevoEstado, $observaciones, $session['id_usuario']]);
        });

        AuditService::log('kpi', 'evaluacion_calificada', 'EXITO', (int) $session['id_usuario'], $session['username'], (int) $eval['id_trabajador'], 'wp_kpi_evaluaciones', $evalId, ['estado' => $nuevoEstado]);

        Response::success(null, "Evaluacion {$nuevoEstado} exitosamente");
    }
}
