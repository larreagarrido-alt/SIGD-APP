<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;

/** Self-service account/device/key authorization. Local PAD is not server attestation. */
final class FaceAccessPolicy
{
    public static function settings(): ?array {
        $p=Config::get('security.face_self_service');
        // A stale deployment/config must not activate either historical login path.
        if(Config::get('security.face_local_pad_login_enabled',false)!==false
            ||Config::get('security.face_pilot.enabled',false)!==false
            ||!is_array($p)||($p['enabled']??null)!==true||($p['local_pad_risk_accepted']??null)!==true
            ||!is_string($p['activation_id']??null)||!preg_match('/^[A-Za-z0-9_-]{1,64}$/D',$p['activation_id']))return null;
        return $p;
    }
    public static function grant(array $key,array $device): ?array {
        $p=self::settings();if(!$p)return null;
        if(($key['estado']??null)!=='ACTIVO'||($device['estado']??null)!=='ACTIVO'
            ||(int)($key['id_clave']??0)<=0||(int)($key['id_usuario']??0)<=0
            ||(int)($key['id_acceso_android']??0)<=0
            ||(int)$key['id_usuario']!==(int)($device['id_usuario']??0)
            ||(int)$key['id_acceso_android']!==(int)($device['id_acceso']??0)
            ||!is_string($key['credencial_hmac']??null)||!is_string($device['credencial_hmac']??null)
            ||!hash_equals($key['credencial_hmac'],$device['credencial_hmac'])
            ||!preg_match('/^[a-f0-9]{64}$/D',(string)($key['huella_clave']??''))
            ||!is_string($device['device_uid']??null)||$device['device_uid']==='')return null;
        $binding=[$p['activation_id'],(int)$key['id_usuario'],(int)$device['id_acceso'],(int)$key['id_clave'],
            $key['huella_clave'],hash('sha256',$device['device_uid']),$key['credencial_hmac']];
        return ['tag'=>'FS1|'.hash('sha256',json_encode($binding,JSON_THROW_ON_ERROR)).'|'.$key['id_clave'],
            'activation_id'=>$p['activation_id']];
    }
    public static function context(int $user,int $android,int $key): ?array {
        if(!self::settings()||$user<=0||$android<=0||$key<=0)return null;
        $st=Database::getConnection()->prepare('SELECT k.*,a.device_uid,a.estado AS android_estado,a.credencial_hmac AS android_hmac,u.password,u.id_rol,u.id_trabajador,t.estado AS worker_estado,sl.situacion FROM wp_claves_faciales k JOIN wp_accesos_android a ON a.id_acceso=k.id_acceso_android AND a.id_usuario=k.id_usuario JOIN usuarios u ON u.id_usuario=k.id_usuario LEFT JOIN trabajadores t ON t.id_trabajador=u.id_trabajador LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador=t.id_trabajador WHERE k.id_clave=? AND k.id_usuario=? AND k.id_acceso_android=?');
        $st->execute([$key,$user,$android]);$k=$st->fetch();
        if(!$k||$k['id_trabajador']===null||$k['worker_estado']!=='Activo'||($k['situacion']??'VIGENTE')!=='VIGENTE'
            ||!hash_equals($k['android_hmac'],hash_hmac('sha256',$k['password'],(string)Config::get('security.jwt_secret')))
            ||!Authorization::allows($k,'auth.perfil',(int)$k['id_trabajador']))return null;
        return self::grant($k,['id_usuario'=>$user,'id_acceso'=>$android,'device_uid'=>$k['device_uid'],
            'estado'=>$k['android_estado'],'credencial_hmac'=>$k['android_hmac']]);
    }
    public static function sessionAllowed(array $session): bool {
        $tag=$session['user_agent']??'';
        if(!is_string($tag)||!preg_match('/^FS1\|[a-f0-9]{64}\|([1-9][0-9]*)$/D',$tag,$m))return false;
        $context=self::context((int)$session['id_usuario'],(int)($session['id_acceso_android']??0),(int)$m[1]);
        return $context!==null&&hash_equals($context['tag'],$tag);
    }
}
