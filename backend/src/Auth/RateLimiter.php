<?php
declare(strict_types=1);

namespace WebSitePlus\Auth;

use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;

final class RateLimiter
{
    public static function checkLoginAttempts(string $username, string $ip): bool
    {
        $db = Database::getConnection();
        $maxAttempts = (int) Config::get('security.max_login_attempts', 5);
        $lockoutMinutes = (int) Config::get('security.lockout_minutes', 15);

        // A shared office/NAT address must not let one worker's mistakes lock out every worker.
        // Keep the existing per-account limit, and retain a separate high-volume IP abuse guard.
        $stmt = $db->prepare('SELECT COUNT(*) FROM wp_intentos_login WHERE username=? AND exito=0 AND creado_en>DATE_SUB(NOW(),INTERVAL ? MINUTE)');
        $stmt->execute([$username, $lockoutMinutes]);
        if ((int)$stmt->fetchColumn() >= $maxAttempts) return false;

        $ipLimit = (int)Config::get('security.max_ip_failed_attempts_per_minute', 60);
        $stmt = $db->prepare('SELECT COUNT(*) FROM wp_intentos_login WHERE ip=? AND exito=0 AND creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE)');
        $stmt->execute([$ip]);
        return (int)$stmt->fetchColumn() < $ipLimit;
    }

    public static function recordAttempt(string $username, string $ip, bool $success, ?string $reason = null): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('
            INSERT INTO wp_intentos_login (username, ip, exito, motivo, creado_en)
            VALUES (?, ?, ?, ?, NOW())
        ');
        $stmt->execute([$username, $ip, $success ? 1 : 0, $reason]);
    }
}
