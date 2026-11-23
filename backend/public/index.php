<?php
declare(strict_types=1);

/**
 * WebSite+ — Front Controller / API Gateway
 */

// Install before loading private files; bootstrap failures must not expose paths or PHP errors.
ini_set('display_errors', '0');
set_exception_handler(static function (Throwable $e): void {
    $expected = $e instanceof \WebSitePlus\Core\HttpException;
    if (!$expected) {
        // No exception values, credentials or stack traces in the public response or this log.
        error_log('WebSitePlus failure: ' . get_class($e) . ' at ' . basename($e->getFile()) . ':' . $e->getLine());
    }
    $status = $expected ? $e->status : 500;
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    header('X-Content-Type-Options: nosniff');
    header('X-Frame-Options: DENY');
    echo json_encode([
        'status' => 'error',
        'code' => $status,
        'error_code' => $expected ? $e->errorCode : 'INTERNAL_SERVER_ERROR',
        'message' => $expected ? $e->getMessage() : 'Error interno del servidor',
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
});

$autoloadLocal = dirname(__DIR__) . '/src/Core/Autoloader.php';
$autoloadServidor = dirname(__DIR__, 2) . '/websiteplus_backend/src/Core/Autoloader.php';

$autoload = is_file($autoloadLocal) ? $autoloadLocal : $autoloadServidor;
if (!is_readable($autoload)) {
    throw new RuntimeException('Private backend bootstrap unavailable');
}
require_once $autoload;
\WebSitePlus\Core\Autoloader::register();

use WebSitePlus\Auth\TokenService;
use WebSitePlus\Controllers\ActividadesController;
use WebSitePlus\Controllers\AttendanceController;
use WebSitePlus\Controllers\AuthController;
use WebSitePlus\Controllers\DashboardController;
use WebSitePlus\Controllers\KpiController;
use WebSitePlus\Controllers\KpiTemplateController;
use WebSitePlus\Controllers\PersonalController;
use WebSitePlus\Controllers\PremiosController;
use WebSitePlus\Controllers\PremiosPublicacionesController;
use WebSitePlus\Controllers\ScheduleController;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

date_default_timezone_set('America/Lima');
if (Config::get('app.env') === 'production' && ($_SERVER['HTTPS'] ?? '') !== 'on') {
    Response::error('HTTPS requerido', 426, 'HTTPS_REQUIRED');
}
$request = new Request();
$method = $request->getMethod();
$path = $request->getPath();

// Cabeceras CORS
header('Cache-Control: no-store');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With, X-Device-Uid');

if ($method === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Normalizar prefijo /api/v1 si existe
$route = $path;
if (str_starts_with($route, '/api/v1')) {
    $route = substr($route, 7) ?: '/';
} elseif (str_starts_with($route, '/api')) {
    $route = substr($route, 4) ?: '/';
}

// ---------------------------------------------------------------------
// Helper de Autenticación
// ---------------------------------------------------------------------
$authenticate = function () use ($request): array {
    $token = $request->getBearerToken();
    if (!$token) {
        Response::error('Token de autorizacion ausente o formato Bearer invalido', 401, 'UNAUTHORIZED');
    }
    $session = TokenService::validateAccessToken($token);
    if (!$session) {
        Response::error('Token invalido, expirado o sesion revocada', 401, 'INVALID_TOKEN');
    }
    return $session;
};

// ---------------------------------------------------------------------
// Router RESTful
// ---------------------------------------------------------------------

if ($method === 'GET' && $route === '/asistencias/mias') {
    (new AttendanceController())->mine($request, $authenticate());
}

// Ping de salud
if ($method === 'GET' && ($route === '/' || $route === '/health')) {
    // Liveness/config policy only: never autoload facial services, query DB or call PAD.
    // A mixed deployment or stale facial class must not break password/huella health.
    // Config indication only; actual authorization requires active account/device/key in the DB.
    $facePolicy=Config::get('security.face_self_service');
    $facialSessionsEnabled = is_array($facePolicy)&&($facePolicy['enabled']??null)===true
        &&($facePolicy['local_pad_risk_accepted']??null)===true
        &&is_string($facePolicy['activation_id']??null)&&preg_match('/^[A-Za-z0-9_-]{1,64}$/D',$facePolicy['activation_id'])===1
        &&Config::get('security.face_local_pad_login_enabled',false)===false&&Config::get('security.face_pilot.enabled',false)===false;
    Response::success([
        'api'       => 'WebSite+ REST API',
        'version'   => '1.0.0',
        'build'     => 'face-self-service-20261022-v014',
        'facial_self_service_enabled' => $facialSessionsEnabled,
        'facial_sessions_enabled' => $facialSessionsEnabled,
        'facial_security_profile' => $facialSessionsEnabled ? 'SELF_SERVICE_LOCAL_PAD' : 'DISABLED',
        'facial_activation_policy' => 'SELF_SERVICE_EXPLICIT_GLOBAL_POLICY',
        'pad_validated_for_sessions' => false,
        'status'    => 'running',
        'timestamp' => date('c'),
    ]);
}

// --- Rutas Públicas de Auth ---
if ($method==='POST' && str_starts_with($route,'/auth/face/device/')) {
    $controller=new \WebSitePlus\Controllers\FaceCredentialController();
    if($route==='/auth/face/device/status'){$controller->status($request,$authenticate());}
    if($route==='/auth/face/device/register'){$controller->begin($request,$authenticate());}
    if($route==='/auth/face/device/confirm'){$controller->confirm($request,$authenticate());}
    if($route==='/auth/face/device/revoke-challenge'){$controller->revokeChallenge($request,$authenticate());}
    if($route==='/auth/face/device/revoke'){$controller->revoke($request,$authenticate());}
    if($route==='/auth/face/device/challenge'){$controller->challenge($request);}
    if($route==='/auth/face/device/verify'){$controller->verify($request);}
}

if ($method === 'POST' && $route === '/auth/login') {
    (new AuthController())->login($request);
}

if ($method === 'POST' && $route === '/auth/refresh') {
    (new AuthController())->refresh($request);
}

if ($method === 'POST' && $route === '/auth/desafio') {
    $session = $authenticate();
    \WebSitePlus\Auth\Authorization::require($session, 'kpi.entregar', (int) $session['id_trabajador']);
    (new AuthController())->requestChallenge($request, $session);
}

// --- Rutas Protegidas de Auth ---
if ($method === 'GET' && $route === '/auth/seguridad/dispositivos') {
    (new \WebSitePlus\Controllers\SecurityDevicesController())->status($request, $authenticate());
}
if ($method === 'POST' && $route === '/auth/seguridad/revocar') {
    (new \WebSitePlus\Controllers\SecurityDevicesController())->revoke($request, $authenticate());
}
if ($method === 'POST' && $route === '/auth/seguridad/huella/desafio') {
    (new \WebSitePlus\Controllers\SecurityDevicesController())->fingerprintRecoveryBegin($request, $authenticate());
}
if ($method === 'POST' && $route === '/auth/seguridad/huella/confirmar') {
    (new \WebSitePlus\Controllers\SecurityDevicesController())->fingerprintRecoveryConfirm($request, $authenticate());
}
if ($method === 'POST' && str_starts_with($route, '/auth/biometria/')) {
    $action = substr($route, strlen('/auth/biometria/'));
    $controller = new \WebSitePlus\Controllers\AndroidAccessController();
    if ($action === 'desafio') { $controller->challenge($request); }
    if ($action === 'login') { $controller->login($request); }
    if ($action === 'registro') { $controller->register($request, $authenticate()); }
    if ($action === 'confirmar') { $controller->confirm($request, $authenticate()); }
    if ($action === 'revocar') { $controller->revoke($request, $authenticate()); }
    if ($action === 'recuperar') { $controller->recover($request, $authenticate()); }
}

if ($method === 'POST' && str_starts_with($route, '/auth/face/enrollment/')) {
    $session=$authenticate();
    $controller=new \WebSitePlus\Controllers\FaceEnrollmentController();
    if ($route==='/auth/face/enrollment/status') { $controller->status($request,$session); }
    if ($route==='/auth/face/enrollment/challenge') { $controller->challenge($request,$session); }
    if ($route==='/auth/face/enrollment/confirm') { $controller->confirm($request,$session); }
}

if ($method === 'POST' && str_starts_with($route, '/auth/face/')) {
    $action = substr($route, strlen('/auth/face/'));
    $controller = new \WebSitePlus\Controllers\FaceAuthController();
    if ($action === 'challenge') { $controller->challenge($request); }
    if ($action === 'verify') { $controller->verify($request); }
}

if ($method === 'POST' && $route === '/auth/logout') {
    $session = $authenticate();
    (new AuthController())->logout($request, $session);
}

if ($method === 'GET' && $route === '/auth/me') {
    $session = $authenticate();
    (new AuthController())->me($request, $session);
}

// Horarios compartidos por cargo. El editor y las alertas corresponden al Administrador.
if ($route === '/kpi/plantillas' && $method === 'GET')
    (new KpiTemplateController())->read($request, $authenticate());
if ($route === '/kpi/plantillas' && $method === 'PUT')
    (new KpiTemplateController())->save($request, $authenticate());
if ($route === '/kpi/plantillas/alertas' && $method === 'GET')
    (new KpiTemplateController())->alerts($request, $authenticate());
if ($method === 'PATCH' && preg_match('#^/kpi/plantillas/alertas/(\d+)/atender$#D', $route, $m))
    (new KpiTemplateController())->acknowledge($request, $authenticate(), (int)$m[1]);

if ($method === 'GET' && $route === '/horarios/mi-horario') {
    (new ScheduleController())->mine($request, $authenticate());
}
if ($method === 'GET' && $route === '/horarios') {
    (new ScheduleController())->list($request, $authenticate());
}
if ($method === 'GET' && $route === '/horarios/cargos') {
    (new ScheduleController())->cargos($request, $authenticate());
}
if ($method === 'GET' && $route === '/horarios/plantillas') {
    (new ScheduleController())->templates($request, $authenticate());
}
if ($method === 'GET' && $route === '/horarios/trabajadores') {
    (new ScheduleController())->workers($request, $authenticate());
}
if ($method === 'POST' && preg_match('#^/horarios/cargos/(\d+)/plantillas/([a-z0-9_]+)$#D', $route, $m)) {
    (new ScheduleController())->assignTemplate($request, $authenticate(), (int)$m[1], $m[2]);
}
if ($method === 'PUT' && preg_match('#^/horarios/cargos/(\d+)$#D', $route, $m)) {
    (new ScheduleController())->save($request, $authenticate(), (int)$m[1]);
}
if ($method === 'PUT' && preg_match('#^/horarios/turnos/(\d+)$#D', $route, $m)) {
    (new ScheduleController())->saveShift($request, $authenticate(), (int)$m[1]);
}
if ($method === 'GET' && $route === '/horarios/alertas') {
    (new ScheduleController())->alerts($request, $authenticate());
}
if ($method === 'PATCH' && preg_match('#^/horarios/alertas/(\d+)/atender$#D', $route, $m)) {
    (new ScheduleController())->acknowledge($request, $authenticate(), (int)$m[1]);
}

// --- Personal y Estados Laborales ---
if ($method === 'GET' && $route === '/personal') {
    $session = $authenticate();
    (new PersonalController())->index($request, $session);
}

if ($method === 'GET' && preg_match('#^/personal/(\d+)/foto$#D', $route, $matches)) {
    (new PersonalController())->photo($request, $authenticate(), (int)$matches[1]);
    exit;
}

if (preg_match('#^/personal/(\d+)$#', $route, $matches)) {
    $workerId = (int) $matches[1];
    $session = $authenticate();
    if ($method === 'GET') {
        (new PersonalController())->show($request, $session, $workerId);
    }
}

if (preg_match('#^/personal/(\d+)/transicion$#', $route, $matches) && $method === 'POST') {
    $workerId = (int) $matches[1];
    $session = $authenticate();
    (new PersonalController())->registrarTransicion($request, $session, $workerId);
}

// --- Módulo KPI ---
if ($method === 'GET' && $route === '/notificaciones') {
    (new \WebSitePlus\Controllers\NotificationsController())->own($request, $authenticate());
}
if ($method === 'GET' && $route === '/bienestar/tests') {
    (new \WebSitePlus\Controllers\WellnessController())->tests($request, $authenticate());
}
if ($method === 'GET' && $route === '/bienestar/actividades') {
    (new \WebSitePlus\Controllers\WellnessController())->activities($request, $authenticate());
}
if ($method === 'GET' && $route === '/bienestar/campanas')
    (new \WebSitePlus\Controllers\WellnessController())->campaigns($request,$authenticate());
if ($method === 'GET' && preg_match('#^/bienestar/campanas/(\d+)$#',$route,$matches))
    (new \WebSitePlus\Controllers\WellnessController())->campaignQuestions($request,$authenticate(),(int)$matches[1]);
if ($method === 'GET' && $route === '/bienestar/mis-respuestas') {
    (new \WebSitePlus\Controllers\WellnessController())->own($request, $authenticate());
}
if ($method === 'GET' && preg_match('#^/bienestar/mis-respuestas/(\d+)$#', $route, $matches)) {
    (new \WebSitePlus\Controllers\WellnessController())->ownDetail($request, $authenticate(), (int) $matches[1]);
}
if ($method === 'POST' && $route === '/bienestar/respuestas') {
    (new \WebSitePlus\Controllers\WellnessController())->submit($request, $authenticate());
}
if ($method === 'GET' && $route === '/bienestar/admin/resumen')
    (new \WebSitePlus\Controllers\WellnessAdminController())->overview($request, $authenticate());
if ($method === 'GET' && $route === '/bienestar/admin/campanas')
    (new \WebSitePlus\Controllers\WellnessAdminController())->campaigns($request, $authenticate());
if ($method === 'POST' && $route === '/bienestar/admin/campanas')
    (new \WebSitePlus\Controllers\WellnessAdminController())->createCampaign($request, $authenticate());
if ($method === 'PUT' && preg_match('#^/bienestar/admin/campanas/(\d+)$#', $route, $matches))
    (new \WebSitePlus\Controllers\WellnessAdminController())->updateCampaign($request, $authenticate(), (int)$matches[1]);
if ($method === 'GET' && $route === '/bienestar/admin/cuestionario')
    (new \WebSitePlus\Controllers\WellnessAdminController())->questionnaire($request, $authenticate());
if ($method === 'GET' && $route === '/bienestar/admin/planes')
    (new \WebSitePlus\Controllers\WellnessAdminController())->plans($request, $authenticate());
if ($method === 'POST' && $route === '/bienestar/admin/planes')
    (new \WebSitePlus\Controllers\WellnessAdminController())->savePlan($request, $authenticate());
if ($method === 'PUT' && preg_match('#^/bienestar/admin/planes/(\d+)$#', $route, $matches))
    (new \WebSitePlus\Controllers\WellnessAdminController())->savePlan($request, $authenticate(), (int)$matches[1]);
if ($method === 'POST' && $route === '/bienestar/admin/asistente')
    (new \WebSitePlus\Controllers\WellnessAdminController())->assistant($request, $authenticate());
if ($method === 'GET' && preg_match('#^/bienestar/admin/confidencial/(\d+)$#', $route, $matches))
    (new \WebSitePlus\Controllers\WellnessAdminController())->privateReview($request, $authenticate(), (int)$matches[1]);
if ($method === 'PUT' && preg_match('#^/bienestar/admin/confidencial/(\d+)/observacion$#', $route, $matches))
    (new \WebSitePlus\Controllers\WellnessAdminController())->privateObservation($request, $authenticate(), (int)$matches[1]);
if ($method === 'POST' && $route === '/notificaciones/marcar-todas') {
    (new \WebSitePlus\Controllers\NotificationsController())->readAll($request, $authenticate());
}
if ($method === 'POST' && preg_match('#^/notificaciones/(\d+)/lectura$#', $route, $matches)) {
    (new \WebSitePlus\Controllers\NotificationsController())->read($request, $authenticate(), (int) $matches[1]);
}
if ($method === 'POST' && $route === '/kpi/registros/sync') {
    (new \WebSitePlus\Controllers\KpiSyncController())->sync($request, $authenticate());
}
if ($method === 'POST' && $route === '/kpi/formularios/sync') {
    (new \WebSitePlus\Controllers\KpiFormsController())->sync($request, $authenticate());
}
if ($method === 'GET' && $route === '/kpi/formularios/permitidos') {
    (new \WebSitePlus\Controllers\KpiFormsController())->allowed($request, $authenticate());
}
if ($method === 'GET' && $route === '/kpi/formularios') {
    (new \WebSitePlus\Controllers\KpiFormsController())->list($request, $authenticate());
}
if ($method === 'GET' && $route === '/kpi/configuracion/borradores') {
    (new \WebSitePlus\Controllers\KpiDraftsController())->list($request, $authenticate());
}
if ($method === 'POST' && $route === '/kpi/configuracion/borradores') {
    (new \WebSitePlus\Controllers\KpiDraftsController())->save($request, $authenticate());
}
if ($method === 'GET' && $route === '/kpi/registros') {
    (new \WebSitePlus\Controllers\KpiSyncController())->ownRecords($request, $authenticate());
}
if ($method === 'GET' && $route === '/kpi/mis-kpis') {
    $session = $authenticate();
    (new KpiController())->misKpis($request, $session);
}

if (preg_match('#^/kpi/evaluaciones/(\d+)$#', $route, $matches) && $method === 'GET') {
    $evalId = (int) $matches[1];
    $session = $authenticate();
    (new KpiController())->detalle($request, $session, $evalId);
}

if (preg_match('#^/kpi/evaluaciones/(\d+)/borrador$#', $route, $matches) && $method === 'POST') {
    $evalId = (int) $matches[1];
    $session = $authenticate();
    (new KpiController())->guardarBorrador($request, $session, $evalId);
}

if (preg_match('#^/kpi/evaluaciones/(\d+)/entregar$#', $route, $matches) && $method === 'POST') {
    $evalId = (int) $matches[1];
    $session = $authenticate();
    (new KpiController())->confirmarEntrega($request, $session, $evalId);
}

if (preg_match('#^/kpi/evaluaciones/(\d+)/calificar$#', $route, $matches) && $method === 'POST') {
    $evalId = (int) $matches[1];
    $session = $authenticate();
    (new KpiController())->calificar($request, $session, $evalId);
}

// Official KPI rules, assignment and report review. No score is created by a raw form upload.
if ($route === '/kpi/oficial/definiciones' && $method === 'GET')
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->definitions($request, $authenticate());
if ($route === '/kpi/oficial/definiciones' && $method === 'POST')
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->createDefinition($request, $authenticate());
if ($method === 'POST' && preg_match('#^/kpi/oficial/definiciones/(\d+)/aprobar$#', $route, $matches))
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->approveDefinition($request, $authenticate(), (int)$matches[1]);
if ($route === '/kpi/oficial/asignaciones' && $method === 'POST')
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->assign($request, $authenticate());
if ($route === '/kpi/oficial/reportes' && $method === 'GET')
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->received($request, $authenticate());
if ($method === 'POST' && preg_match('#^/kpi/oficial/reportes/(\d+)/evaluar$#', $route, $matches))
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->review($request, $authenticate(), (int)$matches[1]);
if ($route === '/ranking/politicas' && $method === 'POST')
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->policy($request, $authenticate());
if ($route === '/ranking/cierres' && $method === 'POST')
    (new \WebSitePlus\Controllers\KpiOfficialAdminController())->close($request, $authenticate());

// --- Actividades Laborales ---
if ($route === '/actividades') {
    $session = $authenticate();
    if ($method === 'GET') {
        (new ActividadesController())->index($request, $session);
    } elseif ($method === 'POST') {
        (new ActividadesController())->store($request, $session);
    }
}

// --- Premios y Puntos ---
if ($method === 'GET' && $route === '/premios') {
    $session = $authenticate();
    (new PremiosPublicacionesController())->catalog($request, $session);
}
if ($method === 'GET' && $route === '/premios/admin') {
    (new PremiosPublicacionesController())->adminList($request, $authenticate());
}
if ($method === 'POST' && $route === '/premios/admin') {
    (new PremiosPublicacionesController())->save($request, $authenticate());
}
if (preg_match('#^/premios/admin/(\d+)$#', $route, $rewardMatch)) {
    $controller = new PremiosPublicacionesController();
    if ($method === 'PUT') $controller->save($request, $authenticate(), (int)$rewardMatch[1]);
    if ($method === 'DELETE') $controller->remove($request, $authenticate(), (int)$rewardMatch[1]);
}
if ($method === 'POST' && preg_match('#^/premios/admin/(\d+)/imagen$#', $route, $rewardMatch)) {
    (new PremiosPublicacionesController())->upload($request, $authenticate(), (int)$rewardMatch[1]);
}
if ($method === 'GET' && preg_match('#^/premios/(\d+)/imagen$#', $route, $rewardMatch)) {
    (new PremiosPublicacionesController())->image($request, $authenticate(), (int)$rewardMatch[1]);
}
if ($method === 'GET' && preg_match('#^/premios/(\d+)$#', $route, $rewardMatch)) {
    (new PremiosPublicacionesController())->detail($request, $authenticate(), (int)$rewardMatch[1]);
}

if ($method === 'GET' && $route === '/premios/mis-puntos') {
    $session = $authenticate();
    (new PremiosController())->misPuntos($request, $session);
}

if ($method === 'GET' && $route === '/ranking') {
    (new \WebSitePlus\Controllers\RankingController())->latest($request, $authenticate());
}
if ($method === 'GET' && $route === '/ranking/periodos') {
    (new \WebSitePlus\Controllers\RankingController())->periods($request, $authenticate());
}
if ($method === 'GET' && preg_match('#^/ranking/(\d+)$#', $route, $matches)) {
    (new \WebSitePlus\Controllers\RankingController())->detail($request, $authenticate(), (int) $matches[1]);
}

if ($method === 'POST' && $route === '/premios/canjear') {
    $session = $authenticate();
    (new PremiosController())->solicitarCanje($request, $session);
}

// --- Dashboard Resumen ---
if ($method === 'GET' && $route === '/dashboard/resumen') {
    $session = $authenticate();
    (new DashboardController())->resumen($request, $session);
}

// Ruta no encontrada
Response::error("Ruta no encontrada: {$method} {$path}", 404, 'NOT_FOUND');
