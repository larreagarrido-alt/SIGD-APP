<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;
use WebSitePlus\Auth\FaceCredentialService as Face;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
final class FaceCredentialController {
    public function status(Request $r,array $s): void {Response::success(Face::status($s,(int)$r->input('id_acceso',0),(string)$r->input('device_uid','')));}
    public function begin(Request $r,array $s): void {Response::success(Face::begin($s,(int)$r->input('id_acceso',0),(string)$r->input('device_uid',''),(string)$r->input('clave_publica',''),$r->getIp()));}
    public function confirm(Request $r,array $s): void {Response::success(Face::confirm($s,(int)$r->input('id_clave',0),(string)$r->input('device_uid',''),(int)$r->input('challenge_id',0),(string)$r->input('firma_base64',''),$r->getIp()));}
    public function revokeChallenge(Request $r,array $s): void {Response::success(Face::revokeChallenge($s,(int)$r->input('id_clave',0),(string)$r->input('device_uid',''),$r->getIp()));}
    public function revoke(Request $r,array $s): void {Response::success(Face::revoke($s,(int)$r->input('id_clave',0),(string)$r->input('device_uid',''),(int)$r->input('challenge_id',0),(string)$r->input('firma_base64',''),$r->getIp()));}
    public function challenge(Request $r): void {Response::success(Face::challenge((int)$r->input('id_clave',0),(string)$r->input('device_uid',''),$r->getIp()));}
    public function verify(Request $r): void {Response::success(Face::verify((int)$r->input('id_clave',0),(string)$r->input('device_uid',''),(int)$r->input('challenge_id',0),(string)$r->input('nonce',''),(string)$r->input('firma_base64',''),(string)$r->input('face_payload',''),$r->getIp()));}
}
