<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;
use WebSitePlus\Auth\AndroidAccessService as Access;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
final class AndroidAccessController
{
    public function register(Request $r,array $s): void {
        Response::success(Access::begin($s,(string)$r->input('device_uid',''),(string)$r->input('clave_publica',''),(string)$r->input('nombre','Android'),$r->getIp()));
    }
    public function confirm(Request $r,array $s): void {
        Response::success(Access::confirm($s,(int)$r->input('id_acceso',0),(string)$r->input('device_uid',''),(int)$r->input('challenge_id',0),(string)$r->input('firma_base64',''),$r->getIp()));
    }
    public function challenge(Request $r): void {
        Response::success(Access::challenge((int)$r->input('id_acceso',0),(string)$r->input('device_uid',''),$r->getIp()));
    }
    public function login(Request $r): void {
        Response::success(Access::login((int)$r->input('id_acceso',0),(string)$r->input('device_uid',''),(int)$r->input('challenge_id',0),(string)$r->input('firma_base64',''),$r->getIp()));
    }
    public function revoke(Request $r,array $s): void {
        Access::revoke($s,(int)$r->input('id_acceso',0),false,'Titular confirma con su clave Android',$r->getIp());Response::success(null,'Celular desvinculado');
    }
    public function recover(Request $r,array $s): void {
        Access::revoke($s,(int)$r->input('id_acceso',0),true,(string)$r->input('motivo',''),$r->getIp());Response::success(null,'Recuperacion registrada');
    }
}
