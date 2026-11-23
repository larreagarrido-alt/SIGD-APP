<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Shared visual templates. Official KPI definitions and submitted reports stay separate. */
final class KpiTemplateController
{
    private static function admin(array $session): void
    {
        if ((int)($session['id_rol'] ?? 0) !== 1) Response::error('Solo Administrador', 403, 'FORBIDDEN');
        Authorization::require($session, 'kpi.administrar');
    }

    public function read(Request $request, array $session): void
    {
        if (!Authorization::allows($session, 'kpi.ver', (int)($session['id_trabajador'] ?? 0))
            && !Authorization::allows($session, 'kpi.administrar')) {
            Response::error('No autorizado para consultar plantillas KPI', 403, 'FORBIDDEN');
        }
        $row = Database::getConnection()->query('SELECT revision,estado_json,actualizado_en FROM wp_kpi_plantillas_estado WHERE id=1')->fetch();
        if (!$row) Response::error('Plantillas no inicializadas', 500, 'TEMPLATES_NOT_INITIALIZED');
        Response::success([
            'revision'=>(int)$row['revision'],
            'estado'=>json_decode($row['estado_json']),
            'actualizado_en'=>$row['actualizado_en'],
        ]);
    }

    public function save(Request $request, array $session): void
    {
        self::admin($session);
        $body = $request->getBody();
        $revision = $body['revision'] ?? null;
        $cargoKey = $body['cargo_key'] ?? null;
        $state = $body['estado'] ?? null;
        if (!is_int($revision) || $revision < 0 || !is_string($cargoKey)
            || !preg_match('/^[A-Z0-9_]{1,80}$/D', $cargoKey)
            || !is_array($state) || ($state !== [] && array_is_list($state)) || count($state) > 1500) {
            Response::error('Plantilla o revision invalida', 422, 'VALIDATION_FAILED');
        }
        $prefixes = ['overrides_', 'sem_', 'inst_global_', 'inst_cargo_', 'custom_role_', 'custom_roles_list_json'];
        foreach ($state as $key=>$value) {
            $validKey = is_string($key) && strlen($key) <= 160
                && preg_match('/^[a-zA-Z0-9_]+$/D', $key);
            $allowed = false;
            foreach ($prefixes as $prefix) if (str_starts_with((string)$key, $prefix)) { $allowed = true; break; }
            if (!$validKey || !$allowed || !is_string($value) || strlen($value) > 750000) {
                Response::error('Campo de plantilla invalido', 422, 'VALIDATION_FAILED');
            }
        }
        $json = json_encode((object)$state, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if ($json === false || strlen($json) > 900000) Response::error('Plantilla demasiado grande', 413, 'TEMPLATE_TOO_LARGE');
        $next = Database::transaction(static function(PDO $db) use ($revision,$cargoKey,$json,$session): int {
            $st = $db->query('SELECT revision FROM wp_kpi_plantillas_estado WHERE id=1 FOR UPDATE');
            $current = $st->fetchColumn();
            if ($current === false || (int)$current !== $revision) {
                throw new HttpException('Plantilla modificada en otro dispositivo', 409, 'STALE_TEMPLATE');
            }
            $next = $revision + 1;
            $st = $db->prepare('UPDATE wp_kpi_plantillas_estado SET revision=?,estado_json=?,actualizado_por=? WHERE id=1');
            $st->execute([$next,$json,(int)$session['id_usuario']]);
            $st = $db->prepare('INSERT INTO wp_kpi_plantillas_alertas (cargo_key,revision) VALUES (?,?)');
            $st->execute([$cargoKey,$next]);
            return $next;
        });
        Response::success(['revision'=>$next,'cargo_key'=>$cargoKey]);
    }

    public function alerts(Request $request, array $session): void
    {
        self::admin($session);
        $rows = Database::getConnection()->query('SELECT id_alerta,cargo_key,revision,creada_en
            FROM wp_kpi_plantillas_alertas WHERE estado="PENDIENTE" ORDER BY id_alerta DESC LIMIT 100')->fetchAll();
        Response::success($rows);
    }

    public function acknowledge(Request $request, array $session, int $id): void
    {
        self::admin($session);
        $st = Database::getConnection()->prepare('UPDATE wp_kpi_plantillas_alertas SET estado="ATENDIDA",atendida_por=?,atendida_en=NOW()
            WHERE id_alerta=? AND estado="PENDIENTE"');
        $st->execute([(int)$session['id_usuario'],$id]);
        if ($st->rowCount() !== 1) Response::error('Alerta no disponible', 404, 'NOT_FOUND');
        Response::success(['id_alerta'=>$id,'estado'=>'ATENDIDA']);
    }
}
