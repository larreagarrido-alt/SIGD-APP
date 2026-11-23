<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Auth\LaborPolicy;
use WebSitePlus\Auth\KpiLaborCalendar;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
use WebSitePlus\Kpi\KpiFormCompletion;

/** Preserves raw form reports. Official KPI evaluations remain a separate workflow. */
final class KpiFormsController
{
    private const TYPES = ['VENTAS','DISENADOR','DISENADOR_JUNIOR','DISENADOR_SENIOR',
        'GERENCIA','PROGRAMACION','ADMINISTRACION','ASISTENCIA_ADMIN','CONTABILIDAD',
        'COMMUNITY','FOTOGRAFIA','TRAFFICKER','VIDEO'];

    /** Only mappings approved for the authenticated worker's current labor cargo. */
    public function allowed(Request $request, array $session): void
    {
        $worker = (int) ($session['id_trabajador'] ?? 0);
        if ($worker <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'kpi.ver', $worker);
        Authorization::require($session, 'kpi.entregar', $worker);
        $attendance = Database::getAttendanceConnection()->prepare(
            'SELECT t.id_cargo,t.tipo,c.nombre AS cargo_nombre FROM trabajadores t JOIN cargos c ON c.id_cargo=t.id_cargo WHERE t.id_trabajador=? AND t.estado="Activo"');
        $attendance->execute([$worker]);
        $labor = $attendance->fetch(PDO::FETCH_ASSOC);
        $cargo = (int) ($labor['id_cargo'] ?? 0);
        if ($cargo <= 0) Response::error('Cargo activo no disponible', 403, 'FORBIDDEN');
        $st = Database::getConnection()->prepare(
            'SELECT tipo_formulario FROM wp_kpi_formulario_cargos WHERE id_cargo=? AND aprobado_en IS NOT NULL ORDER BY tipo_formulario');
        $st->execute([$cargo]);
        $types = array_values(array_intersect($st->fetchAll(PDO::FETCH_COLUMN), self::TYPES));
        // The current labor cargo is the assignment source; no administrator enters worker IDs.
        if ($types) self::syncApprovedAssignment($worker,$cargo,$types);
        $leave = LaborPolicy::approvedLeaveOnDate($worker, date('Y-m-d'));
        $calendar=KpiLaborCalendar::currentWeek($worker);
        $completed=[];
        $reports=Database::getConnection()->prepare('SELECT tipo_formulario,payload_json FROM wp_kpi_formularios_detalle WHERE id_trabajador=? AND actualizado_en>=? ORDER BY id_detalle DESC LIMIT 200');
        $reports->execute([$worker,$calendar['semana_desde']]);
        foreach ($reports as $report) {
            if (!in_array($report['tipo_formulario'],$types,true)) continue;
            $payload=json_decode((string)$report['payload_json'],true);
            if (is_array($payload)) $completed+=KpiFormCompletion::units((string)$report['tipo_formulario'],$payload);
        }
        Response::success(['id_trabajador'=>$worker,'id_cargo'=>$cargo,
            'cargo_nombre'=>(string)$labor['cargo_nombre'],'tipo_trabajador'=>(string)$labor['tipo'],
            'tipos_formulario'=>$types,
            'calendario_laboral'=>$calendar,
            'turnos_kpi_pendientes'=>$types ? KpiFormCompletion::pending($calendar,$completed,date('Y-m-d')) : 0,
            'ausencia_aprobada'=>$leave ? [
                'tipo'=>(string)$leave['tipo_permiso'],
                'fecha_inicio'=>(string)$leave['fecha_inicio'],
                'fecha_fin'=>(string)$leave['fecha_fin'],
            ] : null]);
    }

    private static function syncApprovedAssignment(int $worker, int $cargo, array $types): void
    {
        $db=Database::getConnection();
        $find=$db->prepare('SELECT id_kpi FROM wp_kpi_definiciones WHERE id_cargo_objetivo=?
            AND codigo=? AND tipo_metrica="PORCENTAJE" AND unidad_medida="CUMPLIMIENTO"
            AND activo=1 AND regla_aprobada_en IS NOT NULL LIMIT 1');
        $exists=$db->prepare('SELECT 1 FROM wp_kpi_asignaciones WHERE id_trabajador=? AND id_kpi=?
            AND activo=1 AND fecha_inicio<=CURDATE() AND (fecha_fin IS NULL OR fecha_fin>=CURDATE()) LIMIT 1');
        $insert=$db->prepare('INSERT IGNORE INTO wp_kpi_asignaciones
            (id_trabajador,id_kpi,fecha_inicio,asignado_por) VALUES (?,?,CURDATE(),NULL)');
        foreach ($types as $type) {
            $find->execute([$cargo,'SIGD-OFICIAL-CARGO-'.$cargo]);
            $kpi=(int)$find->fetchColumn();
            if (!$kpi) continue;
            $exists->execute([$worker,$kpi]);
            if (!$exists->fetchColumn()) $insert->execute([$worker,$kpi]);
        }
    }

    public function sync(Request $request, array $session): void
    {
        $worker = (int) ($session['id_trabajador'] ?? 0);
        if ($worker <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'kpi.entregar', $worker);
        $attendance = Database::getAttendanceConnection()->prepare('SELECT id_cargo FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
        $attendance->execute([$worker]);
        $cargo = (int) $attendance->fetchColumn();
        if ($cargo <= 0) Response::error('Cargo activo no disponible', 403, 'FORBIDDEN');
        $records = $request->getBody()['records'] ?? null;
        if (!is_array($records) || !array_is_list($records) || count($records) < 1 || count($records) > 20) {
            Response::error('Se requieren entre 1 y 20 reportes', 422, 'VALIDATION_FAILED');
        }
        $rows = [];
        foreach ($records as $record) {
            if (!is_array($record)) Response::error('Reporte inválido', 422, 'VALIDATION_FAILED');
            $type = $record['tipo_formulario'] ?? null;
            $uid = $record['registro_uid'] ?? null;
            $revision = $record['revision'] ?? null;
            $payload = $record['payload'] ?? null;
            if (!is_string($type) || !in_array($type, self::TYPES, true)
                || !is_string($uid) || preg_match('/^[A-Za-z0-9_.:-]{1,100}$/D', $uid) !== 1
                || !is_int($revision) || $revision < 1
                || !is_array($payload) || array_is_list($payload)) {
                Response::error('Tipo, identidad, revisión o contenido inválido', 422, 'VALIDATION_FAILED');
            }
            $json = json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
            if ($json === false || strlen($json) > 262144) Response::error('Reporte demasiado grande', 422, 'VALIDATION_FAILED');
            $key = $type . ':' . $uid;
            if (isset($rows[$key])) Response::error('Reporte repetido en lote', 422, 'VALIDATION_FAILED');
            $rows[$key] = compact('type','uid','revision','json') + ['hash' => hash('sha256', $json)];
        }
        $accepted = Database::transaction(function (PDO $db) use ($rows, $worker, $cargo, $session): array {
            $metaRevision=(int)$db->query('SELECT revision FROM wp_kpi_plantillas_estado WHERE id=1 FOR UPDATE')->fetchColumn();
            $mapping = $db->prepare('SELECT 1 FROM wp_kpi_formulario_cargos WHERE id_cargo=? AND tipo_formulario=? AND aprobado_en IS NOT NULL');
            $select = $db->prepare('SELECT id_detalle,id_cargo,revision,payload_sha256,payload_json FROM wp_kpi_formularios_detalle WHERE id_trabajador=? AND tipo_formulario=? AND registro_uid=? FOR UPDATE');
            $insert = $db->prepare('INSERT INTO wp_kpi_formularios_detalle (id_trabajador,id_usuario,id_cargo,tipo_formulario,registro_uid,revision,meta_revision,payload_json,payload_sha256) VALUES (?,?,?,?,?,?,?,?,?)');
            $update = $db->prepare('UPDATE wp_kpi_formularios_detalle SET id_usuario=?,id_cargo=?,revision=?,meta_revision=?,payload_json=?,payload_sha256=? WHERE id_trabajador=? AND tipo_formulario=? AND registro_uid=? AND revision<?');
            $admins = $db->prepare('SELECT DISTINCT id_trabajador FROM usuarios WHERE id_rol=1 AND id_trabajador IS NOT NULL AND id_trabajador>0');
            $admins->execute();
            $adminWorkers = array_map('intval', $admins->fetchAll(PDO::FETCH_COLUMN));
            $notify = $db->prepare('INSERT INTO wp_notificaciones (id_trabajador,tipo,titulo,mensaje,referencia_tipo,referencia_id) VALUES (?,\'AVISO_GENERAL\',\'Formulario KPI recibido\',?,\'kpi_formulario\',?)');
            $ids = [];
            foreach ($rows as $key => $row) {
                $mapping->execute([$cargo,$row['type']]);
                if (!$mapping->fetchColumn()) throw new HttpException('Formulario sin correspondencia de cargo aprobada', 422, 'FORM_NOT_ASSIGNED');
                $select->execute([$worker,$row['type'],$row['uid']]);
                $old = $select->fetch();
                $changed = false;
                if ($old) {
                    if ((int)$old['id_cargo'] !== $cargo)
                        throw new HttpException('El reporte pertenece a un cargo anterior', 409, 'CARGO_CHANGED');
                    if ((int) $old['revision'] > $row['revision']) throw new HttpException('Revisión anterior', 409, 'STALE_REPORT');
                    if ((int) $old['revision'] === $row['revision']) {
                        if (!hash_equals((string) $old['payload_sha256'], $row['hash'])) throw new HttpException('Revisión con distinto contenido', 409, 'REPORT_CONFLICT');
                    } else {
                        $update->execute([(int)$session['id_usuario'],$cargo,$row['revision'],$metaRevision,$row['json'],$row['hash'],$worker,$row['type'],$row['uid'],$row['revision']]);
                        if ($update->rowCount() !== 1) throw new HttpException('Revisión concurrente', 409, 'REPORT_CONFLICT');
                        $changed = true;
                    }
                } else {
                    $insert->execute([$worker,(int)$session['id_usuario'],$cargo,$row['type'],$row['uid'],$row['revision'],$metaRevision,$row['json'],$row['hash']]);
                    $changed = true;
                }
                $oldPayload=$old?json_decode((string)$old['payload_json'],true):[];
                $newPayload=json_decode($row['json'],true);
                $before=KpiFormCompletion::units($row['type'],is_array($oldPayload)?$oldPayload:[]);
                $after=KpiFormCompletion::units($row['type'],is_array($newPayload)?$newPayload:[]);
                $newUnits=array_diff_key($after,$before);
                if ($changed && $newUnits) {
                    $reportId = $old ? (int)$old['id_detalle'] : (int)$db->lastInsertId();
                    foreach ($adminWorkers as $adminWorker) {
                        foreach (array_keys($newUnits) as $unit) {
                            $notify->execute([$adminWorker, 'El trabajador #'.$worker.' completó '.$row['type'].' del '.$unit.'.', $reportId]);
                        }
                    }
                }
                $ids[] = ['tipo_formulario'=>$row['type'],'registro_uid'=>$row['uid'],'revision'=>$row['revision'],'payload_sha256'=>$row['hash']];
            }
            return $ids;
        });
        Response::success(['id_trabajador'=>$worker,'accepted'=>$accepted], 'Reportes confirmados');
    }

    public function list(Request $request, array $session): void
    {
        $worker = (int) ($request->getQuery('id_trabajador', $session['id_trabajador'] ?? 0));
        if ($worker <= 0) Response::error('Trabajador inválido', 422, 'VALIDATION_FAILED');
        Authorization::require($session, 'kpi.ver', $worker);
        $type = $request->getQuery('tipo_formulario', '');
        if (!is_string($type) || ($type !== '' && !in_array($type, self::TYPES, true))) Response::error('Formulario inválido', 422, 'VALIDATION_FAILED');
        $uid = $request->getQuery('registro_uid', '');
        if (!is_string($uid) || ($uid !== '' && preg_match('/^[A-Za-z0-9_.:-]{1,100}$/D', $uid)!==1))
            Response::error('Identidad de reporte inválida', 422, 'VALIDATION_FAILED');
        $after = filter_var($request->getQuery('after_id', 0), FILTER_VALIDATE_INT);
        $limit = filter_var($request->getQuery('limit', 100), FILTER_VALIDATE_INT);
        if ($after === false || $after < 0 || $limit === false || $limit < 1 || $limit > 100) {
            Response::error('Paginación inválida', 422, 'VALIDATION_FAILED');
        }
        $sql = 'SELECT id_detalle,id_cargo,tipo_formulario,registro_uid,revision,payload_json,payload_sha256,actualizado_en FROM wp_kpi_formularios_detalle WHERE id_trabajador=?';
        $args = [$worker];
        if ($type !== '') { $sql .= ' AND tipo_formulario=?'; $args[] = $type; }
        if ($uid !== '') { $sql .= ' AND registro_uid=?'; $args[] = $uid; }
        $count = Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_kpi_formularios_detalle WHERE id_trabajador=?'
            . ($type !== '' ? ' AND tipo_formulario=?' : '') . ($uid !== '' ? ' AND registro_uid=?' : ''));
        $count->execute($args);
        $total = (int) $count->fetchColumn();
        $sql .= ' AND id_detalle > ' . (int) $after . ' ORDER BY id_detalle ASC LIMIT ' . (int) $limit;
        $st = Database::getConnection()->prepare($sql);
        $st->execute($args);
        $items = [];
        foreach ($st as $row) {
            $row['payload'] = json_decode($row['payload_json'], true);
            unset($row['payload_json']);
            $items[] = $row;
        }
        $lastId = $items ? (int) $items[count($items)-1]['id_detalle'] : (int) $after;
        Response::success(['id_trabajador'=>$worker,'total'=>$total,'registros'=>$items,
            'next_after_id'=>$lastId,'has_more'=>count($items)===(int)$limit]);
    }
}
