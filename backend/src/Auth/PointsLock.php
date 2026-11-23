<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use PDO;
final class PointsLock
{
    // Stable row exists before the first ledger entry; serializes balance changes.
    public static function acquire(PDO $db, int $worker): void
    {
        $st = $db->prepare('INSERT INTO wp_situacion_laboral (id_trabajador, ultimo_estado_visto, ultimo_tipo_visto) SELECT id_trabajador, estado, tipo FROM trabajadores WHERE id_trabajador=? ON DUPLICATE KEY UPDATE id_trabajador=VALUES(id_trabajador)');
        $st->execute([$worker]);
        $st = $db->prepare('SELECT id_trabajador FROM wp_situacion_laboral WHERE id_trabajador=? FOR UPDATE');
        $st->execute([$worker]);
        if (!$st->fetchColumn()) { throw new \RuntimeException('Trabajador inexistente'); }
    }
}
