<?php
declare(strict_types=1);

namespace WebSitePlus\Auth;

use WebSitePlus\Core\Database;

final class AuditService
{
    public static function log(
        string $module,
        string $action,
        string $result = 'EXITO',
        ?int $userId = null,
        ?string $username = null,
        ?int $targetWorkerId = null,
        ?string $resource = null,
        ?int $resourceId = null,
        ?array $details = null,
        ?string $ip = null
    ): void {
        try {
            $db = Database::getConnection();
            $clientIp = $ip ?? ($_SERVER['REMOTE_ADDR'] ?? '127.0.0.1');

            // Sanitizar detalles: nunca registrar contraseñas ni tokens
            if ($details !== null) {
                unset($details['password'], $details['access_token'], $details['refresh_token'], $details['clave_publica']);
            }
            $detailsJson = $details ? json_encode($details, JSON_UNESCAPED_UNICODE) : null;

            $stmt = $db->prepare('
                INSERT INTO wp_auditoria (
                    fecha_hora, id_usuario, username, id_trabajador_afectado,
                    ip, modulo, accion, recurso, id_recurso, resultado, detalles
                ) VALUES (NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ');
            $stmt->execute([
                $userId,
                $username,
                $targetWorkerId,
                $clientIp,
                $module,
                $action,
                $resource,
                $resourceId,
                $result,
                $detailsJson
            ]);
        } catch (\Throwable $e) {
            // La auditoría no debe romper la transacción de negocio principal si falla
            error_log('Error registrando auditoria: ' . $e->getMessage());
        }
    }
}
