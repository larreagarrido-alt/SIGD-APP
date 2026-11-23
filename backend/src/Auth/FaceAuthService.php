<?php
declare(strict_types=1);

namespace WebSitePlus\Auth;

use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Response;

final class FaceAuthService
{
    private const MATCH_THRESHOLD = 0.58;

    public static function challenge(string $username, string $deviceUid, string $ip): array
    {
        $username = trim($username);
        $deviceUid = trim($deviceUid);

        if (empty($username)) {
            Response::error('Usuario es obligatorio', 422, 'VALIDATION_FAILED');
        }

        // ERROR B Remediation: device_uid is mandatory and must be valid UUID
        if (empty($deviceUid) || !preg_match('/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/D', $deviceUid)) {
            Response::error('Identificador de dispositivo es obligatorio y debe tener formato valido', 422, 'VALIDATION_FAILED');
        }

        if (!RateLimiter::checkLoginAttempts($username, $ip)) {
            AuditService::log('auth', 'face_bloqueado', 'DENEGADO', null, $username, null, 'usuarios', null, ['motivo' => 'Exceso de intentos'], $ip);
            Response::error('Demasiados intentos fallidos. Espere unos minutos.', 429, 'TOO_MANY_ATTEMPTS');
        }

        $db = Database::getConnection();

        // 1. Buscar usuario y trabajador vinculado
        $stmt = $db->prepare('
            SELECT u.id_usuario, u.username, u.password, u.id_trabajador,
                   t.nombres, t.apellidos, t.estado AS estado_trabajador,
                   sl.situacion AS situacion_websiteplus
            FROM usuarios u
            LEFT JOIN trabajadores t ON u.id_trabajador = t.id_trabajador
            LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador = t.id_trabajador
            WHERE u.username = ?
            LIMIT 1
        ');
        $stmt->execute([$username]);
        $user = $stmt->fetch();

        if (!$user) {
            Response::error('Usuario no encontrado', 404, 'USER_NOT_FOUND');
        }

        // 2. Verificar estado laboral
        $esInactivo = ($user['estado_trabajador'] !== 'Activo');
        $esDesvinculado = (($user['situacion_websiteplus'] ?? 'VIGENTE') !== 'VIGENTE');
        if ($user['id_trabajador'] === null || $esInactivo || $esDesvinculado) {
            Response::error('Acceso suspendido: Su cuenta o estado laboral se encuentra inactivo.', 403, 'ACCOUNT_INACTIVE');
        }

        // 3. Consultar descriptor facial directamente de la base de asistencia (solo lectura autorizada)
        $attDb = Database::getAttendanceConnection();
        $stDesc = $attDb->prepare('SELECT rostro_descriptor FROM trabajadores WHERE id_trabajador = ? AND estado = "Activo" LIMIT 1');
        $stDesc->execute([(int) $user['id_trabajador']]);
        $descriptor = trim((string) ($stDesc->fetchColumn() ?: ''));
        if (empty($descriptor)) {
            Response::error('El trabajador no tiene registrado un descriptor facial en el sistema de asistencia.', 422, 'WORKER_WITHOUT_FACE_RECORD');
        }

        // 4. Verificar politica de dispositivo V011: exigir vinculación válida y ACTIVA
        $stDev = $db->prepare('SELECT * FROM wp_accesos_android WHERE device_uid = ? ORDER BY (estado = \'ACTIVO\') DESC, id_acceso DESC LIMIT 1');
        $stDev->execute([$deviceUid]);
        $deviceRow = $stDev->fetch();

        if (!$deviceRow) {
            Response::error('Este celular no tiene una vinculacion registrada. Ingrese con contrasena para vincularlo.', 401, 'DEVICE_NOT_FOUND');
        }

        if ((int) $deviceRow['id_usuario'] !== (int) $user['id_usuario']) {
            Response::error('Este dispositivo esta vinculado a otra cuenta.', 409, 'ACCOUNT_ALREADY_LINKED');
        }

        if ($deviceRow['estado'] === 'PENDIENTE') {
            Response::error('Registro sin completar. Valide su cuenta para repetir la vinculacion.', 401, 'REGISTRATION_INCOMPLETE');
        }

        if ($deviceRow['estado'] === 'REVOCADO') {
            Response::error('El acceso de este dispositivo ha sido revocado. Ingrese con contrasena para vincularlo de nuevo.', 403, 'DEVICE_NOT_AUTHORIZED');
        }

        if ($deviceRow['estado'] !== 'ACTIVO') {
            Response::error('Dispositivo no autorizado.', 403, 'DEVICE_NOT_AUTHORIZED');
        }

        $expectedHmac = hash_hmac('sha256', (string) $user['password'], (string) Config::get('security.jwt_secret'));
        if (!hash_equals((string) $deviceRow['credencial_hmac'], $expectedHmac)) {
            Response::error('Celular no autorizado. Valide su cuenta con contrasena para actualizar la vinculacion.', 401, 'DEVICE_NOT_AUTHORIZED');
        }

        // 5. Generar desafio anti-replay aislado con proposito LOGIN y referencia_tipo AUTH_FACIAL
        $challenge = ChallengeService::createChallenge(
            (int) $user['id_usuario'],
            null,
            'LOGIN',
            $ip,
            'AUTH_FACIAL',
            (int) $deviceRow['id_acceso'],
            $deviceRow['huella_clave']
        );

        $actions = ['BLINK', 'TURN_LEFT', 'TURN_RIGHT'];
        for ($i = count($actions)-1; $i > 0; --$i) {
            $j = random_int(0, $i); [$actions[$i], $actions[$j]] = [$actions[$j], $actions[$i]];
        }

        return [
            'actions' => $actions,
            'challenge_id' => $challenge['challenge_id'],
            'nonce'        => $challenge['nonce'],
            'message'      => $challenge['message'],
            'action'       => 'BLINK',
            'guide'        => 'Centra tu rostro en el recuadro y parpadea suavemente para confirmar presencia real.',
        ];
    }

    public static function verify(
        string $username,
        string $deviceUid,
        int $challengeId,
        string $nonce,
        string $signature,
        mixed $faceData,
        string $ip
    ): array {
        $username = trim($username);
        $deviceUid = trim($deviceUid);
        $signature = trim($signature);
        $nonce = trim($nonce);

        if (empty($username) || $challengeId <= 0 || empty($nonce)) {
            Response::error('Datos de verificacion incompletos', 422, 'VALIDATION_FAILED');
        }

        if (empty($deviceUid) || !preg_match('/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/D', $deviceUid)) {
            Response::error('Identificador de dispositivo es obligatorio y debe tener formato valido', 422, 'VALIDATION_FAILED');
        }

        // Exigir firma criptográfica del dispositivo
        if (empty($signature)) {
            Response::error('Firma criptografica de dispositivo es obligatoria', 422, 'VALIDATION_FAILED');
        }

        if (!RateLimiter::checkLoginAttempts($username, $ip)) {
            AuditService::log('auth', 'face_bloqueado', 'DENEGADO', null, $username, null, 'usuarios', null, ['motivo' => 'Exceso de intentos'], $ip);
            Response::error('Demasiados intentos fallidos. Espere unos minutos.', 429, 'TOO_MANY_ATTEMPTS');
        }

        $db = Database::getConnection();

        // 1. Validar dispositivo V011
        $stDev = $db->prepare('SELECT * FROM wp_accesos_android WHERE device_uid = ? ORDER BY (estado = \'ACTIVO\') DESC, id_acceso DESC LIMIT 1');
        $stDev->execute([$deviceUid]);
        $deviceRow = $stDev->fetch();

        if (!$deviceRow) {
            Response::error('Este celular no tiene una vinculacion registrada.', 401, 'DEVICE_NOT_FOUND');
        }
        if ($deviceRow['estado'] === 'PENDIENTE') {
            Response::error('Vinculacion incompleta.', 401, 'REGISTRATION_INCOMPLETE');
        }
        if ($deviceRow['estado'] === 'REVOCADO') {
            Response::error('Dispositivo revocado.', 403, 'DEVICE_NOT_AUTHORIZED');
        }
        if ($deviceRow['estado'] !== 'ACTIVO') {
            Response::error('Dispositivo no autorizado.', 403, 'DEVICE_NOT_AUTHORIZED');
        }

        // 2. Validar desafio aislado para AUTH_FACIAL
        $stCh = $db->prepare('
            SELECT * FROM wp_desafios
            WHERE id_desafio = ?
              AND proposito = "LOGIN"
              AND referencia_tipo = "AUTH_FACIAL"
              AND referencia_id = ?
              AND resultado = "PENDIENTE"
              AND usado_en IS NULL
              AND expira_en > NOW()
            LIMIT 1
        ');
        $stCh->execute([$challengeId, (int) $deviceRow['id_acceso']]);
        $challenge = $stCh->fetch();

        if (!$challenge) {
            RateLimiter::recordAttempt($username, $ip, false, 'DESAFIO_VENCIDO');
            Response::error('Desafio vencido, reutilizado o no corresponde al contexto facial.', 401, 'CHALLENGE_EXPIRED_OR_REUSED');
        }

        // Validar nonce
        if (!hash_equals((string) $challenge['nonce'], $nonce)) {
            RateLimiter::recordAttempt($username, $ip, false, 'NONCE_INVALIDO');
            Response::error('Desafio no corresponde a la sesion de escaneo.', 401, 'CHALLENGE_EXPIRED_OR_REUSED');
        }

        // 3. Verificar firma criptográfica ECDSA P-256 usando clave pública registrada
        $rawSignature = (strlen($signature) <= 512) ? base64_decode($signature, true) : false;
        if ($rawSignature === false) {
            $db->prepare('UPDATE wp_desafios SET resultado = "FIRMA_INVALIDA", usado_en = NOW() WHERE id_desafio = ? AND resultado = "PENDIENTE" AND usado_en IS NULL')->execute([$challengeId]);
            RateLimiter::recordAttempt($username, $ip, false, 'FIRMA_INVALIDA');
            Response::error('Firma criptografica invalida o malformada.', 401, 'INVALID_SIGNATURE');
        }

        $verifySig = @openssl_verify($challenge['mensaje'], $rawSignature, $deviceRow['clave_publica'], OPENSSL_ALGO_SHA256);
        if ($verifySig !== 1) {
            $db->prepare('UPDATE wp_desafios SET resultado = "FIRMA_INVALIDA", usado_en = NOW() WHERE id_desafio = ? AND resultado = "PENDIENTE" AND usado_en IS NULL')->execute([$challengeId]);
            RateLimiter::recordAttempt($username, $ip, false, 'FIRMA_INVALIDA');
            Response::error('Firma criptografica rechazada: no coincide con la clave del dispositivo vinculado.', 401, 'INVALID_SIGNATURE');
        }

        // Marcar desafio consumido inmediatamente (anti-replay)
        $upd = $db->prepare('UPDATE wp_desafios SET resultado = "VALIDO", usado_en = NOW() WHERE id_desafio = ? AND resultado = "PENDIENTE" AND usado_en IS NULL AND expira_en > NOW()');
        $upd->execute([$challengeId]);
        if ($upd->rowCount() === 0) {
            RateLimiter::recordAttempt($username, $ip, false, 'DESAFIO_REUTILIZADO');
            Response::error('Desafio vencido o reutilizado.', 401, 'CHALLENGE_EXPIRED_OR_REUSED');
        }

        // 4. Buscar usuario completo y validar que coincida con el desafío y el dispositivo
        $stmt = $db->prepare('
            SELECT u.*, r.nombre AS rol_nombre,
                   t.nombres, t.apellidos, t.dni, t.email, t.estado AS estado_trabajador,
                   t.tipo AS tipo_trabajador, t.id_cargo, t.id_agencia, t.foto_perfil,
                   c.nombre AS cargo_nombre,
                   a.nombre AS agencia_nombre,
                   sl.situacion AS situacion_websiteplus
            FROM usuarios u
            JOIN roles r ON u.id_rol = r.id_rol
            LEFT JOIN trabajadores t ON u.id_trabajador = t.id_trabajador
            LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
            LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
            LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador = t.id_trabajador
            WHERE u.username = ?
            LIMIT 1
        ');
        $stmt->execute([$username]);
        $user = $stmt->fetch();

        if (!$user || (int) $user['id_usuario'] !== (int) $challenge['id_usuario'] || (int) $user['id_usuario'] !== (int) $deviceRow['id_usuario']) {
            RateLimiter::recordAttempt($username, $ip, false, 'USUARIO_INVALIDO');
            Response::error('Usuario no coincide con la vinculacion del dispositivo.', 403, 'UNAUTHORIZED');
        }

        $expectedHmac = hash_hmac('sha256', (string) $user['password'], (string) Config::get('security.jwt_secret'));
        if (!hash_equals((string) $deviceRow['credencial_hmac'], $expectedHmac)
            || !hash_equals((string) $deviceRow['huella_clave'], (string) $challenge['hash_contenido'])) {
            Response::error('La credencial cambio. Ingrese con contrasena.', 401, 'DEVICE_NOT_AUTHORIZED');
        }

        // 5. Verificar estado laboral
        $esInactivo = ($user['estado_trabajador'] !== 'Activo');
        $esDesvinculado = (($user['situacion_websiteplus'] ?? 'VIGENTE') !== 'VIGENTE');
        if ($user['id_trabajador'] === null || $esInactivo || $esDesvinculado) {
            RateLimiter::recordAttempt($username, $ip, false, 'TRABAJADOR_INACTIVO');
            Response::error('Acceso suspendido: Su cuenta o estado laboral se encuentra inactivo.', 403, 'ACCOUNT_INACTIVE');
        }

        // 6. Consultar descriptor facial directamente de la base de asistencia (solo lectura autorizada)
        $attDb = Database::getAttendanceConnection();
        $stDesc = $attDb->prepare('SELECT rostro_descriptor FROM trabajadores WHERE id_trabajador = ? AND estado = "Activo" LIMIT 1');
        $stDesc->execute([(int) $user['id_trabajador']]);
        $storedDescriptorRaw = trim((string) ($stDesc->fetchColumn() ?: ''));
        if (empty($storedDescriptorRaw)) {
            RateLimiter::recordAttempt($username, $ip, false, 'SIN_DESCRIPTOR');
            Response::error('El trabajador no tiene registrado un descriptor facial en el sistema de asistencia.', 422, 'WORKER_WITHOUT_FACE_RECORD');
        }

        $storedDescriptor = json_decode($storedDescriptorRaw, true);
        if (!self::validDescriptor($storedDescriptor)) {
            RateLimiter::recordAttempt($username, $ip, false, 'DESCRIPTOR_CORRUPTO');
            Response::error('Descriptor facial almacenado corrupto o no compatible.', 500, 'INVALID_STORED_DESCRIPTOR');
        }

        // 7. Validar estrictamente los 128 valores numéricos y finitos de entrada
        $inputDescriptor = is_string($faceData) ? json_decode($faceData, true) : $faceData;
        if (!self::validDescriptor($inputDescriptor)) {
            RateLimiter::recordAttempt($username, $ip, false, 'FACE_DATA_INVALIDA');
            Response::error('Datos de rostro invalidos o no compatibles con 128D.', 422, 'VALIDATION_FAILED');
        }

        foreach ($inputDescriptor as $val) {
            if (!is_numeric($val) || !is_finite((float) $val)) {
                RateLimiter::recordAttempt($username, $ip, false, 'FACE_DATA_VALORES_INVALIDOS');
                Response::error('El descriptor facial contiene valores numericos invalidos o no finitos.', 422, 'VALIDATION_FAILED');
            }
        }

        // 8. Comparar vector de entrada contra descriptor de asistencia
        $distance = self::euclideanDistance($storedDescriptor, $inputDescriptor);

        if ($distance > self::MATCH_THRESHOLD) {
            RateLimiter::recordAttempt($username, $ip, false, 'ROSTRO_NO_COINCIDE');
            AuditService::log('auth', 'face_mismatch', 'FALLO', (int) $user['id_usuario'], $username, (int) $user['id_trabajador'], 'trabajadores', (int) $user['id_trabajador'], ['distancia' => round($distance, 4)], $ip);
            // IMPORTANTE (Regla 8): No exponer distancia detallada al usuario
            Response::error('El rostro capturado no coincide con el registro del trabajador.', 401, 'FACE_MISMATCH');
        }

        // Actualizar último uso del dispositivo
        $db->prepare('UPDATE wp_accesos_android SET ultimo_uso_en = NOW() WHERE id_acceso = ?')->execute([(int) $deviceRow['id_acceso']]);

        // A preview match is not a successful login and must not reset failed login attempts.

        // 9. REGLA DE SEGURIDAD PRIORITARIA INCONDICIONAL:
        // Se bloquea DE FORMA INCONDICIONAL la emisión de sesiones de autenticación (tokens de acceso o refresco).
        // Ninguna combinación de configuraciones (sea face_auth_issuance_enabled, face_auth_engine_certified u otra)
        // puede emitir tokens a partir de vectores de punto flotante proporcionados por el cliente
        // mientras no exista un extractor real compatible, protección de vivacidad validada y un mecanismo confiable de verificación.
        AuditService::log('auth', 'face_preview_evaluated', 'DENEGADO', (int) $user['id_usuario'], $username, (int) $user['id_trabajador'], 'usuarios', (int) $user['id_usuario'], [
            'descriptor_match'     => true,
            'liveness_verified'    => false,
            'authenticated'        => false,
            'metodo'                => 'VALIDACION_DISPOSITIVO_Y_VECTOR_PRUEBA',
            'distancia'             => round($distance, 4),
            'dispositivo_uid'       => $deviceUid,
            'sesion_emitida'        => false,
            'bloqueo_incondicional' => true
        ], $ip);

        return [
            'verified'        => false,
            'descriptor_match' => true,
            'device_signature_valid' => true,
            'liveness_verified' => false,
            'session_issued'  => false,
            'mode'            => 'PREVIEW_SECURITY_RESTRICTED',
            'message'         => 'Coincidencia matematica de prueba y firma de dispositivo comprobadas. No se ha verificado vivacidad ni autenticado al usuario. No se emiten sesiones.',
            'device' => [
                'device_uid' => $deviceUid,
                'id_acceso'  => (int) $deviceRow['id_acceso'],
                'estado'     => $deviceRow['estado'],
            ],
            'user' => [
                'id_usuario'      => (int) $user['id_usuario'],
                'username'        => $user['username'],
                'nombre_completo' => trim(($user['nombres'] ?? '') . ' ' . ($user['apellidos'] ?? '')),
            ],
            'auth' => null,
        ];
    }

    public static function validDescriptor(mixed $vector): bool
    {
        if (!is_array($vector) || !array_is_list($vector) || count($vector) !== 128) { return false; }
        foreach ($vector as $value) {
            if ((!is_int($value) && !is_float($value)) || !is_finite((float) $value)) { return false; }
        }
        return true;
    }

    public static function euclideanDistance(array $v1, array $v2): float
    {
        if (!self::validDescriptor($v1) || !self::validDescriptor($v2)) {
            throw new \InvalidArgumentException('Expected two finite numeric 128D lists');
        }
        $sum = 0.0;
        $count = min(count($v1), count($v2));
        for ($i = 0; $i < $count; $i++) {
            $diff = (float) $v1[$i] - (float) $v2[$i];
            $sum += $diff * $diff;
        }
        return sqrt($sum);
    }
}
