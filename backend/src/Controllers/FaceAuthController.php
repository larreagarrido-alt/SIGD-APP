<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use WebSitePlus\Auth\FaceAuthService;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

final class FaceAuthController
{
    public function challenge(Request $request): void
    {
        $username = (string) $request->input('username', '');
        $deviceUid = (string) $request->input('device_uid', '');
        $data = FaceAuthService::challenge($username, $deviceUid, $request->getIp());
        Response::success($data, 'Desafio facial generado');
    }

    public function verify(Request $request): void
    {
        $body = $request->getBody();
        $username = (string) ($body['username'] ?? '');
        $deviceUid = (string) ($body['device_uid'] ?? '');
        $challengeId = (int) ($body['challenge_id'] ?? 0);
        $nonce = (string) ($body['nonce'] ?? '');
        $signature = (string) ($body['signature'] ?? $body['firma_base64'] ?? '');
        $faceData = $body['face_data'] ?? null;

        $data = FaceAuthService::verify($username, $deviceUid, $challengeId, $nonce, $signature, $faceData, $request->getIp());
        Response::success($data, 'Evaluacion facial de prueba sin autenticacion');
    }
}
