<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AndroidAccessService;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Self-service management of the existing Android and facial credentials. */
final class SecurityDevicesController
{
    private static function owner(array $session): int
    {
        $user = (int) ($session['id_usuario'] ?? 0);
        $worker = (int) ($session['id_trabajador'] ?? 0);
        if ($user <= 0 || $worker <= 0) Response::error('Cuenta sin trabajador vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'auth.perfil', $worker);
        return $user;
    }

    public function status(Request $request, array $session): void
    {
        $user = self::owner($session);
        $db = Database::getConnection();
        $android = $db->prepare('SELECT id_acceso, nombre, huella_habilitada, creado_en, ultimo_uso_en FROM wp_accesos_android WHERE id_usuario=? AND estado="ACTIVO" ORDER BY id_acceso DESC');
        $android->execute([$user]);
        $face = $db->prepare('SELECT id_clave, id_acceso_android, confirmado_en FROM wp_claves_faciales WHERE id_usuario=? AND estado="ACTIVO" ORDER BY id_clave DESC');
        $face->execute([$user]);
        Response::success(['id_usuario' => $user, 'android' => $android->fetchAll(PDO::FETCH_ASSOC),
            'rostro' => $face->fetchAll(PDO::FETCH_ASSOC),
            'requiere_password_reciente' => $session['metodo'] !== 'PASSWORD' || strtotime((string) $session['creada_en']) < time() - 300]);
    }

    public function revoke(Request $request, array $session): void
    {
        $user = self::owner($session);
        AndroidAccessService::freshPassword($session);
        $method = $request->input('metodo');
        if (!is_string($method) || !in_array($method, ['HUELLA', 'ROSTRO', 'AMBOS'], true)) {
            Response::error('Metodo invalido', 422, 'VALIDATION_FAILED');
        }
        $requestedId = $request->input('id_acceso');
        if ($requestedId !== null && (!is_int($requestedId) || $requestedId <= 0)) {
            Response::error('Dispositivo invalido', 422, 'VALIDATION_FAILED');
        }
        $result = Database::transaction(function (PDO $db) use ($user, $method, $requestedId): array {
            $android = $db->prepare('SELECT id_acceso, huella_habilitada FROM wp_accesos_android WHERE id_usuario=? AND estado="ACTIVO" FOR UPDATE');
            $android->execute([$user]);
            $accesses = [];
            foreach ($android->fetchAll(PDO::FETCH_ASSOC) as $row) $accesses[(int)$row['id_acceso']] = (int)$row['huella_habilitada'];
            if ($requestedId === null) {
                if (count($accesses) > 1) throw new HttpException('Selecciona un dispositivo concreto', 422, 'DEVICE_SELECTION_REQUIRED');
                $id = $accesses ? (int)array_key_first($accesses) : 0;
            } else $id = $requestedId;
            if ($id <= 0 || !array_key_exists($id, $accesses)) throw new HttpException('Dispositivo no encontrado', 404, 'NOT_FOUND');
            $face = $db->prepare('SELECT id_clave FROM wp_claves_faciales WHERE id_usuario=? AND id_acceso_android=? AND estado="ACTIVO" FOR UPDATE');
            $face->execute([$user, $id]);
            $faceIds = array_map('intval', $face->fetchAll(PDO::FETCH_COLUMN));
            if ($method === 'HUELLA' && $accesses[$id] !== 1) throw new HttpException('Huella ya desvinculada', 404, 'NOT_FOUND');
            if ($method === 'ROSTRO' && !$faceIds) throw new HttpException('Rostro no vinculado', 404, 'NOT_FOUND');
            if ($method !== 'HUELLA' && $faceIds) {
                $db->prepare('UPDATE wp_claves_faciales SET estado="REVOCADO",revocado_en=NOW() WHERE id_usuario=? AND id_acceso_android=? AND estado="ACTIVO"')->execute([$user, $id]);
                $db->prepare('UPDATE wp_sesiones SET revocada_en=NOW(),motivo_revocacion="CLAVE_FACIAL_REVOCADA" WHERE id_usuario=? AND id_acceso_android=? AND metodo="FACIAL" AND revocada_en IS NULL')->execute([$user, $id]);
            }
            if ($method === 'HUELLA') {
                // Keep the active Android guardian: facial keys are bound to its ID and HMAC.
                $db->prepare('UPDATE wp_accesos_android SET huella_habilitada=0 WHERE id_usuario=? AND id_acceso=? AND estado="ACTIVO"')->execute([$user, $id]);
                $db->prepare('UPDATE wp_sesiones SET revocada_en=NOW(),motivo_revocacion="HUELLA_DESVINCULADA" WHERE id_usuario=? AND id_acceso_android=? AND metodo="BIOMETRIA" AND revocada_en IS NULL')->execute([$user, $id]);
            } elseif ($method === 'AMBOS') {
                $db->prepare('UPDATE wp_accesos_android SET estado="REVOCADO",revocado_en=NOW() WHERE id_usuario=? AND id_acceso=? AND estado="ACTIVO"')->execute([$user, $id]);
                $db->prepare('UPDATE wp_sesiones SET revocada_en=NOW(),motivo_revocacion="CELULAR_REVOCADO" WHERE id_usuario=? AND id_acceso_android=? AND revocada_en IS NULL')->execute([$user, $id]);
            }
            return ['id_acceso' => $id, 'huella_revocada' => $method !== 'ROSTRO',
                'rostro_revocado' => $method !== 'HUELLA' && (bool)$faceIds,
                'dispositivo_revocado' => $method === 'AMBOS'];
        });
        AuditService::log('auth', 'autogestion_biometria', 'EXITO', $user, $session['username'] ?? null,
            (int) $session['id_trabajador'], 'wp_accesos_android', $result['id_acceso'], ['metodo' => $method] + $result, $request->getIp());
        Response::success($result, 'Vinculacion revocada');
    }

    public function fingerprintRecoveryBegin(Request $request, array $session): void
    {
        self::owner($session);
        Response::success(AndroidAccessService::fingerprintRecoveryBegin($session,
            (int)$request->input('id_acceso',0),(string)$request->input('device_uid',''),$request->getIp()));
    }

    public function fingerprintRecoveryConfirm(Request $request, array $session): void
    {
        self::owner($session);
        Response::success(AndroidAccessService::fingerprintRecoveryConfirm($session,
            (int)$request->input('id_acceso',0),(string)$request->input('device_uid',''),
            (int)$request->input('challenge_id',0),(string)$request->input('firma_base64',''),$request->getIp()));
    }

}
