<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Auth\KpiLaborCalendar;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Shared schedules keyed by the real cargo in Asistencias. No demo seed is published. */
final class ScheduleController
{
    private static function admin(array $session): void
    {
        if ((int)($session['id_rol'] ?? 0) !== 1) Response::error('Solo Administrador', 403, 'FORBIDDEN');
        Authorization::require($session, 'dashboard.admin');
    }

    private static function cargoName(int $cargo): ?string
    {
        $st = Database::getAttendanceConnection()->prepare('SELECT nombre FROM cargos WHERE id_cargo=?');
        $st->execute([$cargo]);
        $name = $st->fetchColumn();
        return $name === false ? null : (string)$name;
    }

    public function mine(Request $request, array $session): void
    {
        $worker = (int)($session['id_trabajador'] ?? 0);
        if ($worker <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        $st = Database::getAttendanceConnection()->prepare('SELECT t.id_cargo,c.nombre AS cargo_nombre FROM trabajadores t JOIN cargos c ON c.id_cargo=t.id_cargo WHERE t.id_trabajador=? AND t.estado="Activo"');
        $st->execute([$worker]);
        $workerRow = $st->fetch();
        $cargo = (int)($workerRow['id_cargo'] ?? 0);
        if ($cargo <= 0) Response::error('Cargo activo no disponible', 403, 'FORBIDDEN');
        $db = Database::getConnection();
        $st = $db->prepare('SELECT id_cargo,perfil_json,revision,publicado,actualizado_en FROM wp_horarios_cargo WHERE id_cargo=?');
        $st->execute([$cargo]);
        $row = $st->fetch();
        if ($row && !(bool)$row['publicado']) {
            $row = null;
        } elseif (!$row) {
            // Future cargo: use a confirmed template only when its name matches exactly.
            $template = $db->prepare('SELECT perfil_json FROM wp_horarios_plantillas WHERE JSON_UNQUOTE(JSON_EXTRACT(perfil_json,"$.roleName")) = ? LIMIT 2');
            $template->execute([$workerRow['cargo_nombre']]);
            $matches = $template->fetchAll();
            if (count($matches) === 1) {
                $profile = json_decode($matches[0]['perfil_json']);
                $profile->id = 'cargo_' . $cargo;
                $profile->roleName = $workerRow['cargo_nombre'];
                $profile->generated = false;
                $row = ['id_cargo'=>$cargo, 'perfil_json'=>json_encode($profile, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
                    'revision'=>0, 'actualizado_en'=>null];
            }
        }
        $shift = $db->prepare('SELECT turno,TIME_FORMAT(hora_entrada,"%H:%i") AS hora_entrada,TIME_FORMAT(hora_salida,"%H:%i") AS hora_salida FROM wp_horarios_turnos WHERE id_trabajador=?');
        $shift->execute([$worker]);
        Response::success(['horario'=>$row ? [
            'id_cargo'=>(int)$row['id_cargo'], 'perfil'=>json_decode($row['perfil_json']),
            'revision'=>(int)$row['revision'], 'actualizado_en'=>$row['actualizado_en'],
        ] : null, 'turno'=>$shift->fetch() ?: null,
            'calendario_laboral'=>KpiLaborCalendar::currentWeek($worker)]);
    }

    public function list(Request $request, array $session): void
    {
        self::admin($session);
        $rows = Database::getConnection()->query('SELECT id_cargo,perfil_json,revision,publicado,actualizado_en FROM wp_horarios_cargo ORDER BY id_cargo')->fetchAll();
        Response::success(array_map(static fn(array $r): array => [
            'id_cargo'=>(int)$r['id_cargo'], 'perfil'=>json_decode($r['perfil_json']),
            'revision'=>(int)$r['revision'], 'publicado'=>(bool)$r['publicado'],
            'actualizado_en'=>$r['actualizado_en'],
        ], $rows));
    }

    public function cargos(Request $request, array $session): void
    {
        self::admin($session);
        $cargos = Database::getAttendanceConnection()->query('SELECT id_cargo,nombre FROM cargos ORDER BY id_cargo')->fetchAll();
        $rows = Database::getConnection()->query('SELECT id_cargo,revision,publicado FROM wp_horarios_cargo')->fetchAll();
        $status = [];
        foreach ($rows as $row) $status[(int)$row['id_cargo']] = ['revision'=>(int)$row['revision'],'publicado'=>(bool)$row['publicado']];
        Response::success(array_map(static fn(array $r): array => [
            'id_cargo'=>(int)$r['id_cargo'], 'nombre'=>$r['nombre'],
            'horario'=>$status[(int)$r['id_cargo']] ?? null,
        ], $cargos));
    }

    public function templates(Request $request, array $session): void
    {
        self::admin($session);
        $rows = Database::getConnection()->query('SELECT id_plantilla,perfil_json FROM wp_horarios_plantillas ORDER BY id_plantilla')->fetchAll();
        Response::success(array_map(static fn(array $r): array => [
            'id_plantilla'=>$r['id_plantilla'], 'perfil'=>json_decode($r['perfil_json']),
        ], $rows));
    }

    public function workers(Request $request, array $session): void
    {
        self::admin($session);
        $staff = Database::getAttendanceConnection()->query('SELECT t.id_trabajador,t.nombres,t.apellidos,t.id_cargo,c.nombre AS cargo_nombre
            FROM trabajadores t JOIN cargos c ON c.id_cargo=t.id_cargo WHERE t.estado="Activo" ORDER BY t.apellidos,t.nombres LIMIT 500')->fetchAll();
        $shifts = Database::getConnection()->query('SELECT id_trabajador,turno FROM wp_horarios_turnos')->fetchAll();
        $byWorker = [];
        foreach ($shifts as $shift) $byWorker[(int)$shift['id_trabajador']] = $shift['turno'];
        Response::success(array_map(static fn(array $r): array => [
            'id_trabajador'=>(int)$r['id_trabajador'],
            'nombre'=>trim($r['nombres'] . ' ' . $r['apellidos']),
            'id_cargo'=>(int)$r['id_cargo'], 'cargo'=>$r['cargo_nombre'],
            'turno'=>$byWorker[(int)$r['id_trabajador']] ?? null,
        ], $staff));
    }

    public function assignTemplate(Request $request, array $session, int $cargo, string $templateId): void
    {
        self::admin($session);
        if ($cargo <= 0 || !preg_match('/^[a-z0-9_]{2,60}$/D', $templateId))
            Response::error('Cargo o plantilla invalida', 422, 'VALIDATION_FAILED');
        $staff = Database::getAttendanceConnection()->prepare('SELECT nombre FROM cargos WHERE id_cargo=?');
        $staff->execute([$cargo]);
        $cargoName = $staff->fetchColumn();
        if ($cargoName === false) Response::error('Cargo inexistente', 422, 'INVALID_CARGO');
        $db = Database::getConnection();
        $st = $db->prepare('SELECT perfil_json FROM wp_horarios_plantillas WHERE id_plantilla=?');
        $st->execute([$templateId]);
        $raw = $st->fetchColumn();
        if ($raw === false) Response::error('Plantilla inexistente', 422, 'INVALID_TEMPLATE');
        $profile = json_decode($raw);
        if (!is_object($profile)) Response::error('Plantilla invalida', 500, 'INVALID_TEMPLATE');
        $profile->id = 'cargo_' . $cargo;
        $profile->roleName = $cargoName;
        $profile->subtitle = $cargoName . ' · Horario de trabajo';
        $profile->generated = false;
        $json = json_encode($profile, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        try {
            $st = $db->prepare('INSERT INTO wp_horarios_cargo (id_cargo,perfil_json,revision,publicado,actualizado_por) VALUES (?,?,1,1,?)');
            $st->execute([$cargo,$json,(int)$session['id_usuario']]);
        } catch (\PDOException $error) {
            if ($error->getCode() === '23000') Response::error('El cargo ya tiene horario', 409, 'SCHEDULE_EXISTS');
            throw $error;
        }
        Response::success(['id_cargo'=>$cargo,'id_plantilla'=>$templateId,'revision'=>1,'publicado'=>true]);
    }

    public function save(Request $request, array $session, int $cargo): void
    {
        self::admin($session);
        $cargoName = $cargo <= 0 ? null : self::cargoName($cargo);
        if ($cargoName === null) Response::error('Cargo inexistente', 422, 'INVALID_CARGO');
        $body = $request->getBody();
        $profile = $body['perfil'] ?? null;
        $revision = $body['revision'] ?? null;
        $published = $body['publicado'] ?? null;
        if (!is_array($profile) || array_is_list($profile) || !is_int($revision) || $revision < 0
            || !is_bool($published) || !is_string($profile['roleName'] ?? null)
            || trim($profile['roleName']) === '' || !is_array($profile['slots'] ?? null)) {
            Response::error('Horario o revision invalidos', 422, 'VALIDATION_FAILED');
        }
        $profile['id'] = 'cargo_' . $cargo;
        $profile['roleName'] = $cargoName;
        // JSON objects with numeric day keys must stay objects. PHP otherwise
        // turns {"0":"...","1":"..."} into a list, which Android cannot read.
        foreach ($profile['slots'] as &$slot) {
            if (is_array($slot) && is_array($slot['items'] ?? null)) {
                $slot['items'] = (object)$slot['items'];
            }
        }
        unset($slot);
        $json = json_encode($profile, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if ($json === false || strlen($json) > 262144) Response::error('Horario demasiado grande', 422, 'VALIDATION_FAILED');
        $newRevision = Database::transaction(function (PDO $db) use ($cargo,$revision,$published,$json,$session): int {
            $st = $db->prepare('SELECT revision FROM wp_horarios_cargo WHERE id_cargo=? FOR UPDATE');
            $st->execute([$cargo]);
            $current = $st->fetchColumn();
            if (($current === false ? 0 : (int)$current) !== $revision) {
                throw new HttpException('El horario cambio en otro dispositivo', 409, 'STALE_SCHEDULE');
            }
            $next = $revision + 1;
            if ($current === false) {
                $st = $db->prepare('INSERT INTO wp_horarios_cargo (id_cargo,perfil_json,revision,publicado,actualizado_por) VALUES (?,?,?,?,?)');
                $st->execute([$cargo,$json,$next,(int)$published,(int)$session['id_usuario']]);
            } else {
                $st = $db->prepare('UPDATE wp_horarios_cargo SET perfil_json=?,revision=?,publicado=?,actualizado_por=? WHERE id_cargo=?');
                $st->execute([$json,$next,(int)$published,(int)$session['id_usuario'],$cargo]);
            }
            $st = $db->prepare('INSERT INTO wp_horarios_alertas (id_cargo,revision) VALUES (?,?)');
            $st->execute([$cargo,$next]);
            return $next;
        });
        Response::success(['id_cargo'=>$cargo,'revision'=>$newRevision,'publicado'=>$published]);
    }

    public function saveShift(Request $request, array $session, int $worker): void
    {
        self::admin($session);
        if ($worker <= 0) Response::error('Trabajador invalido', 422, 'VALIDATION_FAILED');
        $st = Database::getAttendanceConnection()->prepare('SELECT 1 FROM trabajadores WHERE id_trabajador=?');
        $st->execute([$worker]);
        if (!$st->fetchColumn()) Response::error('Trabajador inexistente', 422, 'VALIDATION_FAILED');
        $turno = $request->getBody()['turno'] ?? null;
        $hours = ['MANANA'=>['08:00','13:30'], 'TARDE'=>['15:30','18:30'], 'COMPLETO'=>['08:00','18:30']];
        if (!is_string($turno) || !isset($hours[$turno])) Response::error('Turno invalido', 422, 'VALIDATION_FAILED');
        [$entrada,$salida] = $hours[$turno];
        $st = Database::getConnection()->prepare('INSERT INTO wp_horarios_turnos (id_trabajador,turno,hora_entrada,hora_salida,actualizado_por) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE turno=VALUES(turno),hora_entrada=VALUES(hora_entrada),hora_salida=VALUES(hora_salida),actualizado_por=VALUES(actualizado_por)');
        $st->execute([$worker,$turno,$entrada,$salida,(int)$session['id_usuario']]);
        Response::success(['id_trabajador'=>$worker,'turno'=>$turno,'hora_entrada'=>$entrada,'hora_salida'=>$salida]);
    }

    public function alerts(Request $request, array $session): void
    {
        self::admin($session);
        $rows = Database::getConnection()->query('SELECT id_alerta,id_cargo,revision,creada_en FROM wp_horarios_alertas WHERE estado="PENDIENTE" ORDER BY id_alerta DESC LIMIT 100')->fetchAll();
        Response::success($rows);
    }

    public function acknowledge(Request $request, array $session, int $id): void
    {
        self::admin($session);
        if ($id <= 0) Response::error('Alerta invalida', 422, 'VALIDATION_FAILED');
        $st = Database::getConnection()->prepare('UPDATE wp_horarios_alertas a JOIN wp_horarios_cargo h ON h.id_cargo=a.id_cargo AND h.revision=a.revision SET a.estado="ATENDIDA",a.atendida_por=?,a.atendida_en=NOW() WHERE a.id_alerta=? AND a.estado="PENDIENTE" AND h.publicado=1');
        $st->execute([(int)$session['id_usuario'],$id]);
        if ($st->rowCount() !== 1) Response::error('Publica el horario vigente antes de atender la alerta', 409, 'SCHEDULE_NOT_PUBLISHED');
        Response::success(['id_alerta'=>$id,'estado'=>'ATENDIDA']);
    }
}
