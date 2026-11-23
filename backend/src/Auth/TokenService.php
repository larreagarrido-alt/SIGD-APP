<?php
declare(strict_types=1);

namespace WebSitePlus\Auth;

use PDO;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;

final class TokenService
{
    /**
     * Genera un par de tokens (Access y Refresh), registra la sesión en wp_sesiones y devuelve los tokens planos.
     */
    public static function createSession(int $userId, ?int $deviceId, string $method, string $ip, string $passwordHash, string $client = 'ANDROID', ?int $androidAccessId = null, ?int $faceKeyId = null): array
    {
        $facePolicy=null;
        if ($method === 'FACIAL') {
            if(!FaceAccessPolicy::settings())throw new \WebSitePlus\Core\HttpException('El inicio de sesion facial esta deshabilitado',403,'FACE_LOGIN_DISABLED');
            $facePolicy=FaceAccessPolicy::context($userId,$androidAccessId??0,$faceKeyId??0);
            if(!$facePolicy)throw new \WebSitePlus\Core\HttpException('Acceso facial no autorizado',403,'FACE_ACCESS_NOT_AUTHORIZED');
        }
        $db = Database::getConnection();

        $secret = (string) Config::get('security.jwt_secret', '');
        $accessTtl = (int) Config::get('security.access_ttl_seconds', 3600);
        $refreshTtl = (int) Config::get('security.refresh_ttl_seconds', 2592000);
        if($facePolicy){$accessTtl=min(900,$accessTtl);$refreshTtl=min(3600,$refreshTtl);}

        if (strlen($secret) < 64) { throw new \RuntimeException('Configure un secreto aleatorio de 64 caracteres'); }

        // Generar tokens criptográficamente seguros
        $rawAccess = bin2hex(random_bytes(32));
        $rawRefresh = bin2hex(random_bytes(32));

        $accessHash = hash('sha256', $rawAccess);
        $refreshHash = hash('sha256', $rawRefresh);

        // Huella de credencial: si la contraseña cambia en la tabla usuarios, la sesión queda invalidada
        $credentialFingerprint = hash_hmac('sha256', $passwordHash, $secret);

        $now = time();
        $accessExpiresAt = date('Y-m-d H:i:s', $now + $accessTtl);
        $refreshExpiresAt = date('Y-m-d H:i:s', $now + $refreshTtl);

        $stmt = $db->prepare('
            INSERT INTO wp_sesiones (
                id_usuario, id_dispositivo, metodo, cliente,
                access_hash, access_expira_en, refresh_hash, refresh_expira_en,
                huella_credencial, ip, id_acceso_android, user_agent, creada_en, ultimo_uso_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        ');
        $stmt->execute([
            $userId,
            $deviceId,
            $method,
            $client,
            $accessHash,
            $accessExpiresAt,
            $refreshHash,
            $refreshExpiresAt,
            $credentialFingerprint,
            $ip,
            $androidAccessId,
            $facePolicy['tag']??null,
        ]);

        $sessionId = (int) $db->lastInsertId();

        return [
            'session_id'   => $sessionId,
            'access_token' => $rawAccess,
            'refresh_token'=> $rawRefresh,
            'expires_in'   => $accessTtl,
            'token_type'   => 'Bearer',
        ];
    }

    /**
     * Valida un access token y devuelve los datos de la sesión y del usuario.
     */
    public static function validateAccessToken(string $rawToken): ?array
    {
        $db = Database::getConnection();
        $accessHash = hash('sha256', $rawToken);

        $stmt = $db->prepare('
            SELECT s.*, u.username, u.password AS current_pwd_hash, u.id_rol, u.id_trabajador, u.debe_cambiar_password, t.id_cargo, t.id_agencia,
                   t.estado AS estado_trabajador, t.tipo AS tipo_trabajador,
                   r.nombre AS rol_nombre,
                   c.nombre AS cargo_nombre,
                   sl.situacion AS situacion_websiteplus
            FROM wp_sesiones s
            JOIN usuarios u ON s.id_usuario = u.id_usuario
            LEFT JOIN trabajadores t ON u.id_trabajador = t.id_trabajador
            LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
            LEFT JOIN roles r ON u.id_rol = r.id_rol
            LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador = t.id_trabajador
            WHERE s.access_hash = ?
              AND s.revocada_en IS NULL
              AND s.access_expira_en > NOW()
            LIMIT 1
        ');
        $stmt->execute([$accessHash]);
        $row = $stmt->fetch();

        if (!$row) {
            return null;
        }

        if (!self::sessionAllowed($row)) { return null; }
        // Validar si la contraseña del usuario cambió después de crear la sesión
        $secret = (string) Config::get('security.jwt_secret', '');
        $currentFingerprint = hash_hmac('sha256', $row['current_pwd_hash'], $secret);
        if (!hash_equals($row['huella_credencial'], $currentFingerprint)) {
            // Contraseña modificada -> revocar sesión
            self::revokeSession((int) $row['id_sesion'], 'CAMBIO_PASSWORD');
            return null;
        }

        // Actualizar último uso
        $update = $db->prepare('UPDATE wp_sesiones SET ultimo_uso_en = NOW() WHERE id_sesion = ?');
        $update->execute([$row['id_sesion']]);

        return $row;
    }

    /**
     * Rota el refresh token y emite un nuevo access token.
     */
    public static function rotateRefreshToken(string $rawRefreshToken, string $ip): ?array
    {
        $db = Database::getConnection();
        $refreshHash = hash('sha256', $rawRefreshToken);

        // Buscar sesión con ese refresh token
        $stmt = $db->prepare('
            SELECT s.*, u.password AS current_pwd_hash
            FROM wp_sesiones s
            JOIN usuarios u ON s.id_usuario = u.id_usuario
            WHERE s.refresh_hash = ?
              AND s.revocada_en IS NULL
              AND s.refresh_expira_en > NOW()
            LIMIT 1
        ');
        $stmt->execute([$refreshHash]);
        $row = $stmt->fetch();

        if (!$row) {
            // Detectar posible ataque de reutilización de refresh ya rotado
            $stReused = $db->prepare('SELECT id_sesion FROM wp_sesiones WHERE refresh_anterior_hash = ? LIMIT 1');
            $stReused->execute([$refreshHash]);
            if ($stReused->fetch()) {
                // Posible token robado: revocar de inmediato
                $rev = $db->prepare('UPDATE wp_sesiones SET revocada_en = NOW(), motivo_revocacion = "TOKEN_REUTILIZADO" WHERE refresh_anterior_hash = ?');
                $rev->execute([$refreshHash]);
            }
            return null;
        }

        $accessTtl = (int) Config::get('security.access_ttl_seconds', 3600);
        $refreshTtl = (int) Config::get('security.refresh_ttl_seconds', 2592000);

        if (!self::sessionAllowed($row)) { return null; }

        if($row['metodo']==='FACIAL'){
            $facePolicy=FaceAccessPolicy::settings();if(!$facePolicy)return null;
            $accessTtl=min(900,$accessTtl);$refreshTtl=min(3600,$refreshTtl);
        }

        $newRawAccess = bin2hex(random_bytes(32));
        $newRawRefresh = bin2hex(random_bytes(32));

        $newAccessHash = hash('sha256', $newRawAccess);
        $newRefreshHash = hash('sha256', $newRawRefresh);

        $now = time();
        $newAccessExpires = date('Y-m-d H:i:s', $now + $accessTtl);
        $newRefreshExpires = date('Y-m-d H:i:s', $now + $refreshTtl);

        $upd = $db->prepare('
            UPDATE wp_sesiones SET
                access_hash = ?,
                access_expira_en = ?,
                refresh_anterior_hash = refresh_hash,
                refresh_hash = ?,
                refresh_expira_en = ?,
                ip = ?,
                ultimo_uso_en = NOW()
            WHERE id_sesion = ? AND refresh_hash = ? AND revocada_en IS NULL
        ');
        $upd->execute([
            $newAccessHash,
            $newAccessExpires,
            $newRefreshHash,
            $newRefreshExpires,
            $ip,
            $row['id_sesion'], $refreshHash
        ]);
        if ($upd->rowCount() !== 1) { return null; }

        return [
            'access_token' => $newRawAccess,
            'refresh_token'=> $newRawRefresh,
            'expires_in'   => $accessTtl,
            'token_type'   => 'Bearer',
        ];
    }

    private static function sessionAllowed(array $session): bool
    {
        $db = Database::getConnection();
        $st = $db->prepare('SELECT u.password, u.id_trabajador, t.estado, sl.situacion FROM usuarios u LEFT JOIN trabajadores t ON t.id_trabajador=u.id_trabajador LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador=u.id_trabajador WHERE u.id_usuario=?');
        $st->execute([$session['id_usuario']]);
        $u = $st->fetch();
        if($u&&$u['id_trabajador']!==null&&($u['estado']==='Inactivo'||$u['situacion']==='DESVINCULADO')){
            // Persist the observed restriction and revoke ALL accounts of this worker, regardless
            // of which account happened to present the first access/refresh token.
            \WebSitePlus\Employees\LaborObservationService::observe((int)$u['id_trabajador']);
        }
        $valid = $u && ($u['id_trabajador'] === null || ($u['estado'] === 'Activo' && ($u['situacion'] ?? 'VIGENTE') === 'VIGENTE'));
        $valid = $valid && hash_equals($session['huella_credencial'], hash_hmac('sha256', $u['password'], (string) Config::get('security.jwt_secret')));
        if ($valid && !empty($session['id_acceso_android'])) {
            $st = $db->prepare('SELECT 1 FROM wp_accesos_android WHERE id_acceso=? AND id_usuario=? AND estado="ACTIVO" AND credencial_hmac=?');
            $st->execute([$session['id_acceso_android'], $session['id_usuario'], $session['huella_credencial']]);
            if (!$st->fetchColumn()) {
                self::revokeSession((int) $session['id_sesion'], 'CELULAR_REVOCADO');
                return false;
            }
        }
        if ($valid && $session['id_dispositivo']) {
            $st = $db->prepare('SELECT 1 FROM wp_dispositivos WHERE id_dispositivo=? AND id_usuario=? AND estado="ACTIVO"');
            $st->execute([$session['id_dispositivo'], $session['id_usuario']]);
            $valid = (bool) $st->fetchColumn();
        }
        if (!$valid) { self::revokeAllUserSessions((int) $session['id_usuario'], 'ESTADO_O_CREDENCIAL_INVALIDA'); }
        // Preserve account/device revocation above before applying the facial-only policy.
        if ($valid && ($session['metodo'] ?? '') === 'FACIAL' && !FaceAccessPolicy::sessionAllowed($session)) {return false;}
        return $valid;
    }
    public static function revokeSession(int $sessionId, string $reason = 'LOGOUT'): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE wp_sesiones SET revocada_en = NOW(), motivo_revocacion = ? WHERE id_sesion = ? AND revocada_en IS NULL');
        $stmt->execute([$reason, $sessionId]);
    }

    public static function revokeAllUserSessions(int $userId, string $reason = 'REVOCACION_TOTAL'): void
    {
        $db = Database::getConnection();
        $stmt = $db->prepare('UPDATE wp_sesiones SET revocada_en = NOW(), motivo_revocacion = ? WHERE id_usuario = ? AND revocada_en IS NULL');
        $stmt->execute([$reason, $userId]);
    }
}
