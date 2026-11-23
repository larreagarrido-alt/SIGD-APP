<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Notifications always belong to the worker in the authenticated session. */
final class NotificationsController
{
    private static function worker(array $session): int
    {
        $worker = (int) ($session['id_trabajador'] ?? 0);
        if ($worker <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'auth.perfil', $worker);
        return $worker;
    }

    public function own(Request $request, array $session): void
    {
        $worker = self::worker($session);
        $db = Database::getConnection();
        $query = $db->prepare('SELECT id_notificacion, tipo, titulo, mensaje, referencia_tipo, referencia_id, leida, creado_en FROM wp_notificaciones WHERE id_trabajador=? ORDER BY creado_en DESC, id_notificacion DESC LIMIT 100');
        $query->execute([$worker]);
        Response::success(['id_trabajador' => $worker, 'notificaciones' => $query->fetchAll(PDO::FETCH_ASSOC)]);
    }

    public function read(Request $request, array $session, int $id): void
    {
        $worker = self::worker($session);
        if ($id <= 0 || !is_bool($request->input('leida'))) {
            Response::error('Notificacion o estado invalido', 422, 'VALIDATION_FAILED');
        }
        $read = $request->input('leida');
        $db = Database::getConnection();
        $query = $db->prepare('UPDATE wp_notificaciones SET leida=?, fecha_leida=IF(?=1,NOW(),NULL) WHERE id_notificacion=? AND id_trabajador=?');
        $query->execute([$read ? 1 : 0, $read ? 1 : 0, $id, $worker]);
        $exists = $db->prepare('SELECT 1 FROM wp_notificaciones WHERE id_notificacion=? AND id_trabajador=?');
        $exists->execute([$id, $worker]);
        if (!$exists->fetchColumn()) Response::error('Notificacion no encontrada', 404, 'NOT_FOUND');
        Response::success(['id_notificacion' => $id, 'leida' => $read]);
    }

    public function readAll(Request $request, array $session): void
    {
        $worker = self::worker($session);
        $db = Database::getConnection();
        $query = $db->prepare('UPDATE wp_notificaciones SET leida=1, fecha_leida=NOW() WHERE id_trabajador=? AND leida=0');
        $query->execute([$worker]);
        Response::success(['id_trabajador' => $worker, 'actualizadas' => $query->rowCount()]);
    }
}
