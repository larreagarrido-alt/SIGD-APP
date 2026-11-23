<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use PDO;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;

/** Account/device facial access. LOCAL_PAD trusts signed Android screening, not server PAD. */
final class FaceCredentialService
{
    private static function schema(): void {
        if(!FaceReferenceService::hasTable('wp_claves_faciales'))throw new HttpException('Instala V013 para autorizar la clave facial',503,'FACE_KEY_SCHEMA_REQUIRED');
    }
    private static function rate(string $key,string $ip): void {
        if(!RateLimiter::checkLoginAttempts($key,$ip))throw new HttpException('Demasiados intentos',429,'TOO_MANY_ATTEMPTS');
        $id=(int)substr($key,strpos($key,':')+1);
        if(str_starts_with($key,'face-device:')){
            $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_desafios WHERE referencia_tipo="face_login_device" AND referencia_id=? AND creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE)');$st->execute([$id]);
        }else{
            $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_desafios WHERE id_usuario=? AND referencia_tipo IN ("face_key_register","face_key_revoke") AND creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE)');$st->execute([$id]);
        }
        if((int)$st->fetchColumn()>=60)throw new HttpException('Espera un minuto',429,'TOO_MANY_ATTEMPTS');
        $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_desafios WHERE ip=? AND creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE)');$st->execute([$ip]);
        if((int)$st->fetchColumn()>=(int)Config::get('security.max_ip_challenges_per_minute',120))throw new HttpException('Espera un minuto',429,'TOO_MANY_ATTEMPTS');
    }
    public static function device(int $id,string $uid,?int $user=null): array {
        if($id<=0||!preg_match('/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/D',$uid))throw new HttpException('Dispositivo invalido',422,'VALIDATION_FAILED');
        $st=Database::getConnection()->prepare('SELECT a.*,u.username,u.password,u.id_rol,u.id_trabajador,t.estado AS estado_trabajador,sl.situacion FROM wp_accesos_android a JOIN usuarios u ON u.id_usuario=a.id_usuario LEFT JOIN trabajadores t ON t.id_trabajador=u.id_trabajador LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador=u.id_trabajador WHERE a.id_acceso=? AND a.device_uid=?');$st->execute([$id,$uid]);$a=$st->fetch();
        if(!$a||$a['estado']!=='ACTIVO'||($user!==null&&(int)$a['id_usuario']!==$user)||$a['id_trabajador']===null||$a['estado_trabajador']!=='Activo'||($a['situacion']??'VIGENTE')!=='VIGENTE'
            ||!hash_equals($a['credencial_hmac'],hash_hmac('sha256',$a['password'],(string)Config::get('security.jwt_secret'))))throw new HttpException('Celular o cuenta no autorizados',403,'DEVICE_NOT_AUTHORIZED');
        if(!Authorization::allows($a,'auth.perfil',(int)$a['id_trabajador']))throw new HttpException('Permiso de acceso revocado',403,'FORBIDDEN');
        return $a;
    }
    private static function key(int $id,string $uid): array {
        self::schema();$st=Database::getConnection()->prepare('SELECT * FROM wp_claves_faciales WHERE id_clave=?');$st->execute([$id]);$k=$st->fetch();
        if(!$k||$k['estado']!=='ACTIVO')throw new HttpException('Autoriza de nuevo la clave facial desde tu cuenta',401,'FACE_KEY_NOT_AUTHORIZED');
        $a=self::device((int)$k['id_acceso_android'],$uid,(int)$k['id_usuario']);
        if(!hash_equals($k['credencial_hmac'],$a['credencial_hmac']))throw new HttpException('La credencial de la cuenta cambio',401,'FACE_KEY_NOT_AUTHORIZED');
        return [$k,$a];
    }
    /** Historical unscoped switch remains blocked; self-service uses FaceAccessPolicy. */
    public static function localLoginEnabled(): bool {return false;}
    public static function status(array $s,int $android,string $uid): array {
        self::schema();$a=self::device($android,$uid,(int)$s['id_usuario']);
        $st=Database::getConnection()->prepare('SELECT * FROM wp_claves_faciales WHERE id_usuario=? AND id_acceso_android=? AND estado="ACTIVO" ORDER BY id_clave DESC LIMIT 1');
        $st->execute([$s['id_usuario'],$android]);$k=$st->fetch();
        $ref=FaceReferenceService::describe((int)$s['id_usuario'],(int)$a['id_trabajador']);
        $enabled=$k&&$ref['reference_available']&&FaceAccessPolicy::grant($k,$a)!==null;
        return ['id_clave'=>$k?(int)$k['id_clave']:0,'active'=>(bool)$k,'facial_login_enabled'=>$enabled,
            'facial_self_service_authorized'=>$enabled,'self_service_available'=>FaceAccessPolicy::settings()!==null,
            'recent_password'=>$s['metodo']==='PASSWORD'&&strtotime($s['creada_en'])>=time()-300,
            'security_profile'=>$enabled?'SELF_SERVICE_LOCAL_PAD':'DISABLED','session_issued'=>false];
    }
    private static function revokeFaceSessions(PDO $db,int $user,int $android): void {
        $db->prepare('UPDATE wp_sesiones SET revocada_en=COALESCE(revocada_en,NOW()),motivo_revocacion="CLAVE_FACIAL_REVOCADA" WHERE id_usuario=? AND id_acceso_android=? AND metodo="FACIAL" AND revocada_en IS NULL')->execute([$user,$android]);
    }
    public static function begin(array $s,int $android,string $uid,string $pem,string $ip): array {
        self::schema();AndroidAccessService::freshPassword($s);self::rate('face-key:'.$s['id_usuario'],$ip);
        $a=self::device($android,$uid,(int)$s['id_usuario']);
        FaceReferenceService::resolve((int)$s['id_usuario'],(int)$a['id_trabajador']);
        if(strlen($pem)>1500)throw new HttpException('Clave invalida',422,'VALIDATION_FAILED');
        $p=@openssl_pkey_get_public($pem);$d=$p?openssl_pkey_get_details($p):false;
        if(!$d||$d['type']!==OPENSSL_KEYTYPE_EC||($d['ec']['curve_name']??'')!=='prime256v1')throw new HttpException('Se requiere EC P-256',422,'VALIDATION_FAILED');
        $pem=$d['key'];$fp=hash('sha256',base64_decode(preg_replace('/-----[^-]+-----|\s/','',$pem),true));
        return Database::transaction(function(PDO $db)use($s,$a,$pem,$fp,$ip){
            $st=$db->prepare('SELECT * FROM wp_claves_faciales WHERE huella_clave=? FOR UPDATE');$st->execute([$fp]);$k=$st->fetch();
            if($k&&((int)$k['id_usuario']!==(int)$s['id_usuario']||(int)$k['id_acceso_android']!==(int)$a['id_acceso']||$k['estado']==='REVOCADO'))throw new HttpException('Clave no reutilizable',409,'FACE_KEY_CONFLICT');
            if($k&&$k['estado']==='ACTIVO'&&hash_equals($k['credencial_hmac'],$a['credencial_hmac']))return ['id_clave'=>(int)$k['id_clave'],'already_active'=>true,'active'=>true,'facial_login_enabled'=>FaceAccessPolicy::grant($k,$a)!==null,'session_issued'=>false];
            if(!$k){$st=$db->prepare('INSERT INTO wp_claves_faciales(id_usuario,id_acceso_android,id_sesion_registro,clave_publica,huella_clave,credencial_hmac) VALUES(?,?,?,?,?,?)');$st->execute([$s['id_usuario'],$a['id_acceso'],$s['id_sesion'],$pem,$fp,$a['credencial_hmac']]);$id=(int)$db->lastInsertId();}
            else{$id=(int)$k['id_clave'];$db->prepare('UPDATE wp_claves_faciales SET estado="PENDIENTE",id_sesion_registro=?,credencial_hmac=? WHERE id_clave=?')->execute([$s['id_sesion'],$a['credencial_hmac'],$id]);}
            return ['id_clave'=>$id,'already_active'=>false]+ChallengeService::createChallenge((int)$s['id_usuario'],null,'REGISTRO_DISPOSITIVO',$ip,'face_key_register',$id,self::registrationHash($s,$id,$fp,$a['huella_clave']));
        });
    }
    private static function registrationHash(array $s,int $id,string $fp,string $guardian): string {return hash('sha256',implode(':',['FACE_KEY_V1',$s['id_sesion'],$s['id_usuario'],$id,$fp,$guardian]));}
    private static function consume(int $challenge,int $user,int $id,string $scope,string $hash,string $signature,string $public): bool {
        $db=Database::getConnection();$st=$db->prepare('SELECT * FROM wp_desafios WHERE id_desafio=? FOR UPDATE');$st->execute([$challenge]);$c=$st->fetch();
        if(!$c||$c['proposito']!=='REGISTRO_DISPOSITIVO'||$c['referencia_tipo']!==$scope||(int)$c['referencia_id']!==$id||(int)$c['id_usuario']!==$user||$c['usado_en']!==null||$c['resultado']!=='PENDIENTE'||strtotime($c['expira_en'])<=time()||!hash_equals((string)$c['hash_contenido'],$hash))return false;
        $raw=strlen($signature)<=256?base64_decode($signature,true):false;$valid=$raw!==false&&@openssl_verify($c['mensaje'],$raw,$public,OPENSSL_ALGO_SHA256)===1;
        $st=$db->prepare('UPDATE wp_desafios SET resultado=?,usado_en=NOW() WHERE id_desafio=? AND usado_en IS NULL AND expira_en>NOW()');$st->execute([$valid?'VALIDO':'FIRMA_INVALIDA',$challenge]);return $valid&&$st->rowCount()===1;
    }
    public static function confirm(array $s,int $id,string $uid,int $challenge,string $signature,string $ip): array {
        self::schema();AndroidAccessService::freshPassword($s);self::rate('face-key:'.$s['id_usuario'],$ip);
        $ok=Database::transaction(function(PDO $db)use($s,$id,$uid,$challenge,$signature){
            $st=$db->prepare('SELECT * FROM wp_claves_faciales WHERE id_clave=? FOR UPDATE');$st->execute([$id]);$k=$st->fetch();
            if(!$k||$k['estado']!=='PENDIENTE'||(int)$k['id_usuario']!==(int)$s['id_usuario']||(int)$k['id_sesion_registro']!==(int)$s['id_sesion'])throw new HttpException('Registro no autorizado',403,'DEVICE_NOT_AUTHORIZED');
            $a=self::device((int)$k['id_acceso_android'],$uid,(int)$s['id_usuario']);
            if(!hash_equals($k['credencial_hmac'],$a['credencial_hmac']))return false;
            if(!self::consume($challenge,(int)$s['id_usuario'],$id,'face_key_register',self::registrationHash($s,$id,$k['huella_clave'],$a['huella_clave']),$signature,$a['clave_publica']))return false;
            $db->prepare('UPDATE wp_claves_faciales SET estado="REVOCADO",revocado_en=NOW() WHERE id_usuario=? AND id_acceso_android=? AND estado="ACTIVO"')->execute([$s['id_usuario'],$a['id_acceso']]);
            self::revokeFaceSessions($db,(int)$s['id_usuario'],(int)$a['id_acceso']);
            $db->prepare('UPDATE wp_claves_faciales SET estado="ACTIVO",confirmado_en=NOW() WHERE id_clave=?')->execute([$id]);return true;
        });
        if(!$ok){RateLimiter::recordAttempt('face-key:'.$s['id_usuario'],$ip,false,'INVALID_SIGNATURE');throw new HttpException('Comprobacion de clave invalida',401,'INVALID_SIGNATURE');}
        AuditService::log('auth','clave_facial_autorizada','EXITO',(int)$s['id_usuario'],$s['username'],null,'wp_claves_faciales',$id,['session_issued'=>false],$ip);
        return ['id_clave'=>$id,'active'=>true,'facial_login_enabled'=>FaceAccessPolicy::context((int)$s['id_usuario'],(int)self::key($id,$uid)[0]['id_acceso_android'],$id)!==null,
            'security_profile'=>FaceAccessPolicy::settings()?'SELF_SERVICE_LOCAL_PAD':'DISABLED','session_issued'=>false];
    }
    public static function revokeChallenge(array $s,int $id,string $uid,string $ip): array {
        self::rate('face-key:'.$s['id_usuario'],$ip);
        AndroidAccessService::freshPassword($s);[$k,$a]=self::key($id,$uid);
        if((int)$k['id_usuario']!==(int)$s['id_usuario'])throw new HttpException('Cuenta ajena',403,'FORBIDDEN');
        return ChallengeService::createChallenge((int)$s['id_usuario'],null,'REGISTRO_DISPOSITIVO',$ip,'face_key_revoke',$id,self::registrationHash($s,$id,$k['huella_clave'],$a['huella_clave']));
    }
    public static function revoke(array $s,int $id,string $uid,int $challenge,string $signature,string $ip): array {
        self::rate('face-key:'.$s['id_usuario'],$ip);
        AndroidAccessService::freshPassword($s);[$k,$a]=self::key($id,$uid);
        if((int)$k['id_usuario']!==(int)$s['id_usuario'])throw new HttpException('Cuenta ajena',403,'FORBIDDEN');
        $ok=Database::transaction(function(PDO $db)use($s,$id,$challenge,$signature,$k,$a){
            if(!self::consume($challenge,(int)$s['id_usuario'],$id,'face_key_revoke',self::registrationHash($s,$id,$k['huella_clave'],$a['huella_clave']),$signature,$a['clave_publica']))return false;
            $db->prepare('UPDATE wp_claves_faciales SET estado="REVOCADO",revocado_en=NOW() WHERE id_clave=?')->execute([$id]);
            self::revokeFaceSessions($db,(int)$s['id_usuario'],(int)$a['id_acceso']);return true;
        });
        if(!$ok)throw new HttpException('Firma invalida',401,'INVALID_SIGNATURE');
        AuditService::log('auth','clave_facial_revocada','EXITO',(int)$s['id_usuario'],$s['username'],null,'wp_claves_faciales',$id,null,$ip);return ['revoked'=>true];
    }
    public static function challenge(int $id,string $uid,string $ip): array {
        self::rate('face-device:'.$id,$ip);[$k,$a]=self::key($id,$uid);
        $ref=FaceReferenceService::resolve((int)$k['id_usuario'],(int)$a['id_trabajador']);
        $facePolicy=FaceAccessPolicy::grant($k,$a);
        return ChallengeService::createChallenge((int)$k['id_usuario'],null,'LOGIN',$ip,'face_login_device',$id,$k['huella_clave'])+
            ['reference_source'=>$ref['source'],'facial_login_enabled'=>$facePolicy!==null,'facial_self_service_authorized'=>$facePolicy!==null,'security_profile'=>$facePolicy?'SELF_SERVICE_LOCAL_PAD':'DISABLED','liveness_required'=>true];
    }
    public static function verify(int $id,string $uid,int $challenge,string $nonce,string $signature,string $payload,string $ip): array {
        self::rate('face-device:'.$id,$ip);$proof=LocalFaceProof::decode($payload);$v=$proof['vector'];
        $result=Database::transaction(function(PDO $db)use($id,$uid,$challenge,$nonce,$signature,$payload,$ip,$proof,$v){
        // Lock WebSite+ device/face rows only. Attendance remains read-only.
        $st=$db->prepare('SELECT id_acceso_android FROM wp_claves_faciales WHERE id_clave=?');$st->execute([$id]);$android=$st->fetchColumn();
        if($android){$st=$db->prepare('SELECT id_acceso FROM wp_accesos_android WHERE id_acceso=? FOR UPDATE');$st->execute([$android]);}
        $st=$db->prepare('SELECT id_clave FROM wp_claves_faciales WHERE id_clave=? FOR UPDATE');$st->execute([$id]);
        [$k,$a]=self::key($id,$uid);
        $db=Database::getConnection();$st=$db->prepare('SELECT * FROM wp_desafios WHERE id_desafio=?');$st->execute([$challenge]);$c=$st->fetch();
        if(!$c||$c['proposito']!=='LOGIN'||$c['referencia_tipo']!=='face_login_device'||(int)$c['referencia_id']!==$id||(int)$c['id_usuario']!==(int)$k['id_usuario']||$c['usado_en']!==null||$c['resultado']!=='PENDIENTE'||strtotime($c['expira_en'])<=time()||!hash_equals($c['nonce'],$nonce)||!hash_equals($c['hash_contenido'],$k['huella_clave']))throw new HttpException('Desafio no valido',401,'CHALLENGE_EXPIRED_OR_REUSED');
        $raw=strlen($signature)<=256?base64_decode($signature,true):false;
        $signed=$c['mensaje']."\nFACE_PAYLOAD_SHA256:".hash('sha256',$payload);
        $valid=$raw!==false&&@openssl_verify($signed,$raw,$k['clave_publica'],OPENSSL_ALGO_SHA256)===1;
        $st=$db->prepare('UPDATE wp_desafios SET resultado=?,usado_en=NOW() WHERE id_desafio=? AND usado_en IS NULL AND resultado="PENDIENTE" AND expira_en>NOW()');$st->execute([$valid?'VALIDO':'FIRMA_INVALIDA',$challenge]);
        if(!$valid||$st->rowCount()!==1)return ['_failure'=>'INVALID_SIGNATURE'];
        // Resolve AFTER signature; never download an authoritative descriptor to the client.
        $ref=FaceReferenceService::resolve((int)$k['id_usuario'],(int)$a['id_trabajador']);
        if(FaceAuthService::euclideanDistance($v,$ref['vector'])>.58)return ['_failure'=>'FACE_MISMATCH'];
        $facePolicy=FaceAccessPolicy::grant($k,$a);
        if($proof['local_pad']&&$facePolicy!==null) {
            $schema=$db->query("SELECT COLUMN_TYPE FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wp_sesiones' AND column_name='metodo'")->fetchColumn();
            if(!is_string($schema)||!str_contains($schema,"'FACIAL'"))throw new HttpException('Importa la actualizacion V014 de WebSite+',503,'FACE_SESSION_SCHEMA_REQUIRED');
            $auth=TokenService::createSession((int)$k['id_usuario'],null,'FACIAL',$ip,$a['password'],'ANDROID',(int)$a['id_acceso'],$id);
            AuditService::log('auth','login_facial_autoservicio','EXITO',(int)$k['id_usuario'],$a['username'],null,'wp_claves_faciales',$id,['security_profile'=>'SELF_SERVICE_LOCAL_PAD','activation_id'=>$facePolicy['activation_id'],'server_pad_verified'=>false,'session_issued'=>true],$ip);
            $user=['id_usuario'=>(int)$k['id_usuario'],'username'=>$a['username'],'id_rol'=>(int)$a['id_rol']];
            return ['identity_match'=>true,'device_signature_valid'=>true,'liveness_verified'=>false,'local_pad_accepted'=>true,
                'security_profile'=>'SELF_SERVICE_LOCAL_PAD','facial_self_service_authorized'=>true,'session_issued'=>true,'reference_source'=>$ref['source'],'status'=>'AUTHENTICATED_SELF_SERVICE_LOCAL_PAD','auth'=>$auth,'user'=>$user,
                'profile'=>['user'=>$user,'worker'=>['id_trabajador'=>(int)$a['id_trabajador']]]];
        }
        // Presence/PAD is not established by a client embedding, signature or gesture flag.
        AuditService::log('auth','coincidencia_facial_sin_sesion','DENEGADO',(int)$k['id_usuario'],$a['username'],null,'wp_claves_faciales',$id,['pad_verified'=>false,'session_issued'=>false],$ip);
        return ['identity_match'=>true,'device_signature_valid'=>true,'liveness_verified'=>false,'session_issued'=>false,'facial_login_enabled'=>false,'facial_self_service_authorized'=>false,'reference_source'=>$ref['source'],
            'status'=>'FACE_LOGIN_DISABLED','auth'=>null,'user'=>['id_usuario'=>(int)$k['id_usuario'],'username'=>$a['username']]];
        });
        if(isset($result['_failure'])){
            RateLimiter::recordAttempt('face-device:'.$id,$ip,false,$result['_failure']);
            throw new HttpException($result['_failure']==='FACE_MISMATCH'?'El rostro no coincide con tu cuenta':'Firma o captura alterada',401,$result['_failure']);
        }
        if($result['session_issued'])RateLimiter::recordAttempt('face-device:'.$id,$ip,true);
        return $result;
    }
}
