<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Private drafts only: never writes definitions, assignments, evaluations or permissions. */
final class KpiDraftsController
{
    public function list(Request $request, array $session): void
    {
        Authorization::require($session, 'kpi.administrar');
        $db = Database::getConnection();
        $st = $db->prepare('SELECT borrador_uid,revision,payload_json,payload_sha256,actualizado_en FROM wp_kpi_borradores_admin WHERE id_usuario=? ORDER BY actualizado_en DESC LIMIT 200');
        $st->execute([(int)$session['id_usuario']]);
        $items = [];
        foreach ($st as $row) {
            $row['payload'] = json_decode($row['payload_json'], true);
            unset($row['payload_json']);
            $items[] = $row;
        }
        Response::success(['id_usuario'=>(int)$session['id_usuario'], 'borradores'=>$items]);
    }

    public function save(Request $request, array $session): void
    {
        Authorization::require($session, 'kpi.administrar');
        $body = $request->getBody();
        $uid = $body['borrador_uid'] ?? null;
        $revision = $body['revision'] ?? null;
        $payload = $body['payload'] ?? null;
        if (!is_string($uid) || preg_match('/^[A-Za-z0-9_.:-]{1,100}$/D', $uid)!==1
            || !is_int($revision) || $revision < 1 || !is_array($payload) || array_is_list($payload)) {
            Response::error('Borrador inválido', 422, 'VALIDATION_FAILED');
        }
        $name = trim((string)($payload['name'] ?? ''));
        $target = $payload['targetValue'] ?? null;
        $weight = $payload['weight'] ?? null;
        if ($name === '' || strlen($name) > 450 || !is_numeric($target) || (float)$target <= 0
            || !is_numeric($weight) || (float)$weight < 1 || (float)$weight > 100) {
            Response::error('Nombre, meta o peso inválido', 422, 'VALIDATION_FAILED');
        }
        if (isset($payload['id']) && $payload['id'] !== $uid) Response::error('Identidad de borrador inválida', 422, 'VALIDATION_FAILED');
        $json = json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if ($json === false || strlen($json) > 65536) Response::error('Borrador demasiado grande', 422, 'VALIDATION_FAILED');
        $hash = hash('sha256', $json);
        Database::transaction(function (PDO $db) use ($session,$uid,$revision,$json,$hash): void {
            $user = (int)$session['id_usuario'];
            $st = $db->prepare('SELECT revision,payload_sha256 FROM wp_kpi_borradores_admin WHERE id_usuario=? AND borrador_uid=? FOR UPDATE');
            $st->execute([$user,$uid]);
            $old = $st->fetch();
            if ($old) {
                if ((int)$old['revision'] > $revision) throw new HttpException('Borrador anterior',409,'STALE_DRAFT');
                if ((int)$old['revision'] === $revision && !hash_equals((string)$old['payload_sha256'],$hash)) throw new HttpException('Borrador en conflicto',409,'DRAFT_CONFLICT');
                if ((int)$old['revision'] < $revision) {
                    $up = $db->prepare('UPDATE wp_kpi_borradores_admin SET revision=?,payload_json=?,payload_sha256=? WHERE id_usuario=? AND borrador_uid=? AND revision<?');
                    $up->execute([$revision,$json,$hash,$user,$uid,$revision]);
                    if ($up->rowCount()!==1) throw new HttpException('Borrador en conflicto',409,'DRAFT_CONFLICT');
                }
            } else {
                $ins = $db->prepare('INSERT INTO wp_kpi_borradores_admin (id_usuario,borrador_uid,revision,payload_json,payload_sha256) VALUES (?,?,?,?,?)');
                $ins->execute([$user,$uid,$revision,$json,$hash]);
            }
        });
        Response::success(['id_usuario'=>(int)$session['id_usuario'],'borrador_uid'=>$uid,'revision'=>$revision,'payload_sha256'=>$hash], 'Borrador persistido');
    }
}
