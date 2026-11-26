<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\ChallengeService;
use WebSitePlus\Auth\RateLimiter;
use WebSitePlus\Auth\TokenService;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

final class AuthController
{
    /**
     * POST /api/v1/auth/login
     */
    public function login(Request $request): void
    {
        $username = trim((string) $request->input('username', ''));
        $password = (string) $request->input('password', '');
        $deviceUid = $request->getDeviceUid();
        $ip = $request->getIp();

        if (empty($username) || empty($password)) {
            Response::error('Usuario y contrasena son obligatorios', 422, 'VALIDATION_FAILED');
        }

        // 1. Validar intentos fallidos (Anti fuerza bruta)
        if (!RateLimiter::checkLoginAttempts($username, $ip)) {
            AuditService::log('auth', 'login_bloqueado', 'DENEGADO', null, $username, null, 'usuarios', null, ['motivo' => 'Exceso de intentos'], $ip);
            Response::error('Demasiados intentos fallidos. Cuenta bloqueada temporalmente por 15 minutos.', 429, 'TOO_MANY_ATTEMPTS');
        }

        $db = Database::getConnection();

        // 2. Buscar usuario en la base existente
        $stmt = $db->prepare('
            SELECT u.*, r.nombre AS rol_nombre,
                   t.nombres, t.apellidos, t.dni, t.email, t.estado AS estado_trabajador,
                   t.tipo AS tipo_trabajador, t.id_cargo, t.id_agencia, t.id_supervisor, t.foto_perfil,
                   CONCAT(supervisor.nombres, CHAR(32), supervisor.apellidos) AS supervisor_nombre,
                   c.nombre AS cargo_nombre,
                   a.nombre AS agencia_nombre,
                   sl.situacion AS situacion_websiteplus
            FROM usuarios u
            JOIN roles r ON u.id_rol = r.id_rol
            LEFT JOIN trabajadores t ON u.id_trabajador = t.id_trabajador
            LEFT JOIN trabajadores supervisor ON supervisor.id_trabajador = t.id_supervisor
            LEFT JOIN cargos c ON t.id_cargo = c.id_cargo
            LEFT JOIN agencias a ON t.id_agencia = a.id_agencia
            LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador = t.id_trabajador
            WHERE u.username = ?
            LIMIT 1
        ');
        $stmt->execute([$username]);
        $user = $stmt->fetch();

        // 3. Verificar contraseña con hash nativo bcrypt de asistencia
        if (!$user || !password_verify($password, $user['password'])) {
            RateLimiter::recordAttempt($username, $ip, false, 'CREDENCIALES_INVALIDAS');
            AuditService::log('auth', 'login_fallido', 'FALLO', $user ? (int) $user['id_usuario'] : null, $username, null, 'usuarios', null, null, $ip);
            Response::error('Usuario o contrasena incorrectos', 401, 'INVALID_CREDENTIALS');
        }

        // 4. Verificar estado laboral
        // Si tiene trabajador vinculado, revisar si está Inactivo o Desvinculado
        $esInactivo = ($user['estado_trabajador'] !== 'Activo');
        $esDesvinculado = (($user['situacion_websiteplus'] ?? 'VIGENTE') !== 'VIGENTE');

        if ($user['id_trabajador'] !== null && ($esInactivo || $esDesvinculado)) {
            RateLimiter::recordAttempt($username, $ip, false, 'TRABAJADOR_INACTIVO');
            AuditService::log('auth', 'login_denegado_inactivo', 'DENEGADO', (int) $user['id_usuario'], $username, (int) $user['id_trabajador'], 'trabajadores', (int) $user['id_trabajador'], null, $ip);
            Response::error('Acceso suspendido: Su cuenta o estado laboral se encuentra inactivo. Comuniquese con Administracion.', 403, 'ACCOUNT_INACTIVE');
        }

        if ((int)($user['debe_cambiar_password'] ?? 0) === 1) {
            Response::error('Cambia tu clave inicial en la web de Asistencias antes de entrar a SIGD.', 403, 'PASSWORD_CHANGE_REQUIRED');
        }

        // 5. Verificar si tiene dispositivo registrado activo
        $deviceId = null;
        if (!empty($deviceUid)) {
            $blocked=$db->prepare('SELECT estado FROM wp_dispositivos WHERE id_usuario=? AND device_uid=?');
            $blocked->execute([$user['id_usuario'],$deviceUid]);
            $deviceState=$blocked->fetchColumn();
            if ($deviceState && $deviceState !== 'ACTIVO') { Response::error('Dispositivo no autorizado',403,'DEVICE_NOT_AUTHORIZED'); }
            $stDev = $db->prepare('SELECT id_dispositivo FROM wp_dispositivos WHERE id_usuario = ? AND device_uid = ? AND estado = "ACTIVO" LIMIT 1');
            $stDev->execute([$user['id_usuario'], $deviceUid]);
            $deviceId = $stDev->fetchColumn() ?: null;
        }

        // 6. Registrar intento exitoso y crear sesión
        RateLimiter::recordAttempt($username, $ip, true);

        $sessionTokens = TokenService::createSession(
            (int) $user['id_usuario'],
            $deviceId ? (int) $deviceId : null,
            'PASSWORD',
            $ip,
            $user['password']
        );

        // 7. Cargar permisos efectivos del usuario
        $permissions = \WebSitePlus\Auth\Authorization::permissions($user);

        AuditService::log('auth', 'login_exitoso', 'EXITO', (int) $user['id_usuario'], $username, $user['id_trabajador'] ? (int) $user['id_trabajador'] : null, 'usuarios', (int) $user['id_usuario'], ['metodo' => 'PASSWORD'], $ip);

        // 8. Responder con datos completos
        Response::success([
            'auth' => $sessionTokens,
            'user' => [
                'id_usuario'             => (int) $user['id_usuario'],
                'username'               => $user['username'],
                'id_rol'                 => (int) $user['id_rol'],
                'rol_nombre'             => $user['rol_nombre'],
                'debe_cambiar_password'  => (bool) $user['debe_cambiar_password'],
            ],
            'worker' => $user['id_trabajador'] ? [
                'id_trabajador'      => (int) $user['id_trabajador'],
                'dni'                => $user['dni'],
                'nombres'            => $user['nombres'],
                'apellidos'          => $user['apellidos'],
                'nombre_completo'    => trim($user['nombres'] . ' ' . $user['apellidos']),
                'email'              => $user['email'],
                'tipo'               => $user['tipo_trabajador'],
                'estado'             => $user['estado_trabajador'],
                'id_cargo'           => $user['id_cargo'] ? (int) $user['id_cargo'] : null,
                'cargo_nombre'       => $user['cargo_nombre'],
                'id_agencia'         => $user['id_agencia'] ? (int) $user['id_agencia'] : null,
                'agencia_nombre'     => $user['agencia_nombre'],
                'foto_perfil'        => $user['foto_perfil'],
                'supervisor_nombre' => $user['supervisor_nombre'],
            ] : null,
            'permissions' => array_keys($permissions),
        ], 'Inicio de sesion exitoso');
    }

    /**
     * POST /api/v1/auth/refresh
     */
    public function refresh(Request $request): void
    {
        $refreshToken = (string) $request->input('refresh_token', '');
        if (empty($refreshToken)) {
            Response::error('refresh_token es requerido', 422, 'VALIDATION_FAILED');
        }

        $newTokens = TokenService::rotateRefreshToken($refreshToken, $request->getIp());
        if (!$newTokens) {
            Response::error('Token de refresco invalido, expirado o reutilizado', 401, 'INVALID_REFRESH_TOKEN');
        }

        Response::success($newTokens, 'Token actualizado');
    }

    /**
     * POST /api/v1/auth/logout
     */
    public function logout(Request $request, array $userSession): void
    {
        TokenService::revokeSession((int) $userSession['id_sesion'], 'LOGOUT_VOLUNTARIO');
        AuditService::log('auth', 'logout', 'EXITO', (int) $userSession['id_usuario'], $userSession['username'], null, 'wp_sesiones', (int) $userSession['id_sesion'], null, $request->getIp());
        Response::success(null, 'Sesion cerrada exitosamente');
    }

    /**
     * GET /api/v1/auth/me
     */
    public function me(Request $request, array $userSession): void
    {
        $db = Database::getConnection();

        // Consultar saldo de puntos si tiene trabajador
        $saldoPuntos = 0;
        if (!empty($userSession['id_trabajador'])) {
            $stP = $db->prepare('SELECT saldo_posterior FROM wp_puntos_ledger WHERE id_trabajador = ? ORDER BY id_movimiento DESC LIMIT 1');
            $stP->execute([$userSession['id_trabajador']]);
            $saldoPuntos = (int) ($stP->fetchColumn() ?: 0);
        }

        // Consultar permisos
        $permissions = \WebSitePlus\Auth\Authorization::permissions($userSession);

        // Dispositivo activo vinculado
        $stDev = $db->prepare('SELECT id_dispositivo, device_uid, nombre, modelo, estado FROM wp_dispositivos WHERE id_usuario = ? AND estado = "ACTIVO" LIMIT 1');
        $stDev->execute([$userSession['id_usuario']]);
        $device = $stDev->fetch();

        $workerProfile = null;
        $workerShift = null;
        if (!empty($userSession['id_trabajador'])) {
            $stWorker = $db->prepare('
                SELECT t.nombres, t.apellidos, t.dni, t.email, t.foto_perfil,
                       CONCAT(supervisor.nombres, CHAR(32), supervisor.apellidos) AS supervisor_nombre,
                       c.nombre AS cargo_nombre, a.nombre AS agencia_nombre
                FROM trabajadores t
                LEFT JOIN trabajadores supervisor ON supervisor.id_trabajador = t.id_supervisor
                LEFT JOIN cargos c ON c.id_cargo = t.id_cargo
                LEFT JOIN agencias a ON a.id_agencia = t.id_agencia
                WHERE t.id_trabajador = ? LIMIT 1
            ');
            $stWorker->execute([(int) $userSession['id_trabajador']]);
            $workerProfile = $stWorker->fetch() ?: null;
            $hasShifts = $db->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='wp_horarios_turnos'")->fetchColumn();
            if ((int)$hasShifts === 1) {
                $stShift = $db->prepare('SELECT turno,TIME_FORMAT(hora_entrada,"%H:%i") AS hora_entrada,TIME_FORMAT(hora_salida,"%H:%i") AS hora_salida FROM wp_horarios_turnos WHERE id_trabajador=?');
                $stShift->execute([(int)$userSession['id_trabajador']]);
                $workerShift = $stShift->fetch() ?: null;
            }
        }

        Response::success([
            'user' => [
                'id_usuario'            => (int) $userSession['id_usuario'],
                'username'              => $userSession['username'],
                'id_rol'                => (int) $userSession['id_rol'],
                'rol_nombre'            => $userSession['rol_nombre'],
                'debe_cambiar_password' => (bool) $userSession['debe_cambiar_password'],
            ],
            'worker' => $workerProfile ? [
                'id_trabajador'      => (int) $userSession['id_trabajador'],
                'dni'                => $workerProfile['dni'],
                'nombres'            => $workerProfile['nombres'],
                'apellidos'          => $workerProfile['apellidos'],
                'nombre_completo'    => trim($workerProfile['nombres'] . ' ' . $workerProfile['apellidos']),
                'email'              => $workerProfile['email'],
                'tipo'               => $userSession['tipo_trabajador'],
                'estado'             => $userSession['estado_trabajador'],
                'id_cargo'           => $userSession['id_cargo'] !== null ? (int) $userSession['id_cargo'] : null,
                'cargo_nombre'       => $workerProfile['cargo_nombre'],
                'id_agencia'         => $userSession['id_agencia'] !== null ? (int) $userSession['id_agencia'] : null,
                'agencia_nombre'     => $workerProfile['agencia_nombre'],
                'foto_perfil'        => $workerProfile['foto_perfil'],
                'supervisor_nombre'  => $workerProfile['supervisor_nombre'],
                'saldo_puntos'       => $saldoPuntos,
                'turno'              => $workerShift['turno'] ?? null,
                'hora_entrada'       => $workerShift['hora_entrada'] ?? null,
                'hora_salida'        => $workerShift['hora_salida'] ?? null,
            ] : null,
            'device'      => $device ?: null,
            'permissions' => array_keys($permissions),
        ]);
    }

    /**
     * POST /api/v1/auth/desafio
     * Solicita un nonce para firma biométrica (login o entrega de KPI)
     */
    public function requestChallenge(Request $request, array $session): void
    {
        $userId = (int) $session['id_usuario'];
        $purpose = (string) $request->input('proposito', 'LOGIN');
        $deviceUid = (string) $request->input('device_uid', '');
        $refType = $request->input('referencia_tipo');
        $refId = $request->input('referencia_id') ? (int) $request->input('referencia_id') : null;
        $contentHash = $request->input('hash_contenido');

        if ($userId <= 0 || !in_array($purpose, ['ENTREGA_KPI'], true)) {
            Response::error('Parametros de desafio invalidos', 422, 'VALIDATION_FAILED');
        }

        $db = Database::getConnection();
        $deviceId = null;
        if (!empty($deviceUid)) {
            $stDev = $db->prepare('SELECT id_dispositivo FROM wp_dispositivos WHERE id_usuario = ? AND device_uid = ? AND estado = "ACTIVO" LIMIT 1');
            $stDev->execute([$userId, $deviceUid]);
            $deviceId = $stDev->fetchColumn() ?: null;
        }

        if (!$deviceId || $refType !== 'kpi_evaluacion' || !$refId || !is_string($contentHash) || !preg_match('/^[a-f0-9]{64}$/D', $contentHash)) {
            Response::error('Desafio requiere dispositivo activo, evaluacion y SHA-256', 422);
        }
        $st = $db->prepare('SELECT 1 FROM wp_kpi_evaluaciones WHERE id_evaluacion=? AND id_trabajador=?');
        $st->execute([$refId, $session['id_trabajador']]);
        if (!$st->fetchColumn()) { Response::error('Evaluacion no autorizada', 403); }
        $challenge = ChallengeService::createChallenge(
            $userId,
            $deviceId ? (int) $deviceId : null,
            $purpose,
            $request->getIp(),
            $refType,
            $refId,
            $contentHash
        );

        Response::success($challenge, 'Desafio generado');
    }
}
