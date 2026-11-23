<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Response;
final class Authorization
{
    public static function permissions(array $user): array
    {
        $db = Database::getConnection();
        $st = $db->prepare('SELECT p.codigo, rp.alcance FROM wp_rol_permisos rp JOIN wp_permisos p USING (id_permiso) WHERE rp.id_rol = ?');
        $st->execute([$user['id_rol']]);
        $permissions = $st->fetchAll(\PDO::FETCH_KEY_PAIR);
        $st = $db->prepare('SELECT p.codigo, up.alcance, up.efecto FROM wp_usuario_permisos up JOIN wp_permisos p USING (id_permiso) WHERE up.id_usuario = ?');
        $st->execute([$user['id_usuario']]);
        foreach ($st as $row) {
            if ($row['efecto'] === 'PERMITIR') { $permissions[$row['codigo']] = $row['alcance']; }
            else { unset($permissions[$row['codigo']]); }
        }
        return $permissions;
    }
    public static function allows(array $user, string $code, ?int $worker = null): bool
    {
        $scope = self::permissions($user)[$code] ?? null;
        if ($scope === 'TODOS') { return true; }
        if ($scope === null || !$worker) { return false; }
        if ($worker === (int) $user['id_trabajador']) { return true; }
        $st = Database::getAttendanceConnection()->prepare('SELECT id_agencia, id_supervisor FROM trabajadores WHERE id_trabajador = ?');
        $st->execute([$worker]);
        $target = $st->fetch();
        if (!$target) { return false; }
        if ($scope === 'EQUIPO') { return !empty($user['id_trabajador']) && (int) $target['id_supervisor'] === (int) $user['id_trabajador']; }
        return $scope === 'AGENCIA' && !empty($user['id_agencia']) && (int) $target['id_agencia'] === (int) $user['id_agencia'];
    }
    public static function require(array $user, string $code, ?int $worker = null): void
    {
        if (!self::allows($user, $code, $worker)) { Response::error('No autorizado para este recurso', 403, 'FORBIDDEN'); }
    }
}
