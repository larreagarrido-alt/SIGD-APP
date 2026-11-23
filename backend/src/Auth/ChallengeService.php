<?php
declare(strict_types=1);

namespace WebSitePlus\Auth;

use RuntimeException;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;

final class ChallengeService
{
    /**
     * Genera un desafío criptográfico de un solo uso con ventana de expiración corta (3 min).
     */
    public static function createChallenge(
        int $userId,
        ?int $deviceId,
        string $purpose,
        string $ip,
        ?string $refType = null,
        ?int $refId = null,
        ?string $contentHash = null
    ): array {
        $db = Database::getConnection();
        $ttl = (int) Config::get('security.challenge_ttl_sec', 180);

        // 32 bytes de entropía en base64url
        $nonce = rtrim(strtr(base64_encode(random_bytes(32)), '+/', '-_'), '=');
        $expiresAt = date('Y-m-d H:i:s', time() + $ttl);

        $message = sprintf(
            'WEBSITEPLUS_AUTH:%s:%d:%s:%s:%s',
            $purpose,
            $userId,
            $nonce,
            $contentHash ?? 'NONE',
            $expiresAt
        );

        $stmt = $db->prepare('
            INSERT INTO wp_desafios (
                id_usuario, id_dispositivo, proposito, referencia_tipo, referencia_id,
                nonce, hash_contenido, mensaje, expira_en, resultado, ip, creado_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, "PENDIENTE", ?, NOW())
        ');
        $stmt->execute([
            $userId,
            $deviceId,
            $purpose,
            $refType,
            $refId,
            $nonce,
            $contentHash,
            $message,
            $expiresAt,
            $ip,
        ]);

        $challengeId = (int) $db->lastInsertId();

        return [
            'challenge_id' => $challengeId,
            'nonce'        => $nonce,
            'message'      => $message,
            'expires_at'   => $expiresAt,
            'expires_in'   => $ttl,
        ];
    }

    /**
     * Verifica la firma del desafío y lo marca como usado inmediatamente.
     */
    public static function verifyChallenge(int $challengeId, string $signatureBase64, int $userId, int $deviceId, int $evalId, string $digest): bool
    {
        $db = Database::getConnection();

        $stmt = $db->prepare('
            SELECT d.*, dev.clave_publica, dev.estado AS estado_disp
            FROM wp_desafios d
            LEFT JOIN wp_dispositivos dev ON d.id_dispositivo = dev.id_dispositivo
            WHERE d.id_desafio = ?
            LIMIT 1
        ');
        $stmt->execute([$challengeId]);
        $row = $stmt->fetch();

        if (!$row) {
            return false;
        }
        if ((int) $row['id_usuario'] !== $userId || (int) $row['id_dispositivo'] !== $deviceId
            || $row['estado_disp'] !== 'ACTIVO' || $row['proposito'] !== 'ENTREGA_KPI'
            || $row['referencia_tipo'] !== 'kpi_evaluacion' || (int) $row['referencia_id'] !== $evalId
            || !hash_equals((string) $row['hash_contenido'], $digest)) { return false; }

        // Rechazar si ya fue usado o venció
        if ($row['resultado'] !== 'PENDIENTE' || $row['usado_en'] !== null) {
            return false;
        }
        if (strtotime($row['expira_en']) < time()) {
            $upd = $db->prepare('UPDATE wp_desafios SET resultado = "VENCIDO" WHERE id_desafio = ?');
            $upd->execute([$challengeId]);
            return false;
        }

        $pem = $row['clave_publica'];
        if (empty($pem)) {
            return false;
        }

        // Decodificar firma (Android Keystore emite DER encoded ECDSA o R||S)
        $rawSignature = base64_decode($signatureBase64, true);
        if ($rawSignature === false) {
            return false;
        }

        // Verificar criptográficamente con OpenSSL
        $verify = @openssl_verify($row['mensaje'], $rawSignature, $pem, OPENSSL_ALGO_SHA256);
        $isValid = ($verify === 1);

        $resultStr = $isValid ? 'VALIDO' : 'FIRMA_INVALIDA';
        $upd = $db->prepare('UPDATE wp_desafios SET resultado = ?, usado_en = NOW() WHERE id_desafio = ? AND resultado = "PENDIENTE" AND usado_en IS NULL AND expira_en > NOW()');
        $upd->execute([$resultStr, $challengeId]);

        return $isValid && $upd->rowCount() === 1;
    }
}
