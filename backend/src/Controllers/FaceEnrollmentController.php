<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;
use WebSitePlus\Auth\FaceEnrollmentService as Enrollment;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
final class FaceEnrollmentController
{
    public function status(Request $r,array $s): void {
        Response::success(Enrollment::status($s,(int)$r->input('id_acceso',0),(string)$r->input('device_uid','')));
    }
    public function challenge(Request $r,array $s): void {
        Response::success(Enrollment::challenge($s,(int)$r->input('id_acceso',0),(string)$r->input('device_uid',''),(string)$r->input('perfil_sha256',''),$r->getIp()));
    }
    public function confirm(Request $r,array $s): void {
        Response::success(Enrollment::confirm($s,(int)$r->input('id_acceso',0),(string)$r->input('device_uid',''),(int)$r->input('challenge_id',0),(string)$r->input('firma_base64',''),(string)$r->input('perfil_json',''),$r->getIp()),'Rostro guardado; acceso facial pendiente de validacion');
    }
}
