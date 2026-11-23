<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;

use PDO;
use PDOException;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;

/** Account-authorized profile storage only. No facial login or source attendance writes. */
final class FaceEnrollmentService
{
    public static function status(array $session,int $id,string $uid): array
    {
        $device=self::device($session,$id,$uid);
        $reference=FaceReferenceService::describe((int)$session['id_usuario'],(int)$device['id_trabajador']);
        if($reference['reference_available'] || $reference['migration_v012_required']) {
            $access=FaceReferenceService::hasTable('wp_claves_faciales')?FaceCredentialService::status($session,$id,$uid):[];
            return $reference+['registered'=>$reference['reference_available'],'facial_login_enabled'=>$reference['reference_available']&&($access['facial_login_enabled']??false),
                'recent_password'=>$session['metodo']==='PASSWORD' && strtotime($session['creada_en'])>=time()-300];
        }
        $st=Database::getConnection()->prepare('SELECT id_rostro,estado,creado_en FROM wp_rostros_cuenta WHERE id_usuario=?');
        $st->execute([$session['id_usuario']]);$row=$st->fetch();
        return $reference+['registered'=>(bool)$row,'creado_en'=>$row['creado_en']??null,'facial_login_enabled'=>false,
            'recent_password'=>$session['metodo']==='PASSWORD' && strtotime($session['creada_en'])>=time()-300];
    }

    public static function challenge(array $session,int $id,string $uid,string $digest,string $ip): array
    {
        AndroidAccessService::freshPassword($session);
        $device=self::device($session,$id,$uid);
        self::enrollmentNeeded($session,$device);
        if(!preg_match('/^[a-f0-9]{64}$/D',$digest))throw new HttpException('Resumen facial invalido',422,'VALIDATION_FAILED');
        self::limit($session,$ip);self::unregistered((int)$session['id_usuario']);
        return ChallengeService::createChallenge((int)$session['id_usuario'],null,'REGISTRO_DISPOSITIVO',$ip,
            'rostro_registro',$id,self::digest($session,$device,$digest));
    }

    public static function confirm(array $session,int $id,string $uid,int $challenge,string $signature,string $payload,string $ip): array
    {
        AndroidAccessService::freshPassword($session);
        self::validateSamples($payload);self::limit($session,$ip);
        try {
            $saved=Database::transaction(function(PDO $db)use($session,$id,$uid,$challenge,$signature,$payload){
                $lock=$db->prepare('SELECT id_acceso FROM wp_accesos_android WHERE id_acceso=? FOR UPDATE');$lock->execute([$id]);
                $device=self::device($session,$id,$uid);
                self::enrollmentNeeded($session,$device);
                self::unregistered((int)$session['id_usuario']);
                $st=$db->prepare('SELECT * FROM wp_sesiones WHERE id_sesion=? FOR UPDATE');$st->execute([$session['id_sesion']]);$current=$st->fetch();
                if(!$current || $current['revocada_en']!==null || strtotime($current['access_expira_en'])<=time()
                    || !hash_equals($device['credencial_hmac'],$current['huella_credencial']))throw new HttpException('Sesion no autorizada',401,'INVALID_TOKEN');
                $st=$db->prepare('SELECT * FROM wp_desafios WHERE id_desafio=? FOR UPDATE');$st->execute([$challenge]);$c=$st->fetch();
                if(!$c || $c['proposito']!=='REGISTRO_DISPOSITIVO' || $c['referencia_tipo']!=='rostro_registro'
                    || (int)$c['referencia_id']!==$id || (int)$c['id_usuario']!==(int)$session['id_usuario']
                    || $c['resultado']!=='PENDIENTE' || $c['usado_en']!==null || strtotime($c['expira_en'])<=time()
                    || !hash_equals((string)$c['hash_contenido'],self::digest($session,$device,hash('sha256',$payload))))return null;
                $raw=strlen($signature)<=256?base64_decode($signature,true):false;
                $valid=$raw!==false && @openssl_verify($c['mensaje'],$raw,$device['clave_publica'],OPENSSL_ALGO_SHA256)===1;
                $st=$db->prepare('UPDATE wp_desafios SET usado_en=NOW(),resultado=? WHERE id_desafio=? AND usado_en IS NULL AND expira_en>NOW()');
                $st->execute([$valid?'VALIDO':'FIRMA_INVALIDA',$challenge]);
                if(!$valid || $st->rowCount()!==1)return null;
                $iv=random_bytes(12);$tag='';
                $aad='WSP_FACE_V1:'.$session['id_usuario'].':'.$id;
                $encrypted=openssl_encrypt($payload,'aes-256-gcm',self::key(),OPENSSL_RAW_DATA,$iv,$tag,$aad,16);
                if($encrypted===false)throw new \RuntimeException('Face profile encryption failed');
                $st=$db->prepare('INSERT INTO wp_rostros_cuenta (id_usuario,id_acceso_android,id_sesion_registro,modelo,muestras,perfil_cifrado,iv_base64,tag_base64) VALUES (?,?,?,?,3,?,?,?)');
                $st->execute([$session['id_usuario'],$id,$session['id_sesion'],'face-api.js-0.22.2-128d',base64_encode($encrypted),base64_encode($iv),base64_encode($tag)]);
                return ['registered'=>true,'estado'=>'PENDIENTE_VALIDACION','facial_login_enabled'=>false];
            });
        } catch(PDOException $e) {
            if($e->getCode()==='23000')throw new HttpException('La cuenta ya tiene un rostro registrado. No se sustituyo.',409,'FACE_ALREADY_REGISTERED');
            throw $e;
        }
        if($saved===null){RateLimiter::recordAttempt('face-enroll:'.$session['id_usuario'],$ip,false,'INVALID_SIGNATURE');throw new HttpException('Firma o desafio no autorizado',401,'INVALID_SIGNATURE');}
        AuditService::log('auth','rostro_guardado_pendiente','EXITO',(int)$session['id_usuario'],$session['username'],null,'wp_rostros_cuenta',null,['facial_login_enabled'=>false],$ip);
        return $saved;
    }

    private static function device(array $session,int $id,string $uid): array
    {
        if($id<=0 || !preg_match('/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/D',$uid))throw new HttpException('Celular invalido',422,'VALIDATION_FAILED');
        $st=Database::getConnection()->prepare('SELECT a.*,u.password,u.id_trabajador,t.estado AS estado_trabajador,sl.situacion FROM wp_accesos_android a JOIN usuarios u ON u.id_usuario=a.id_usuario LEFT JOIN trabajadores t ON t.id_trabajador=u.id_trabajador LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador=u.id_trabajador WHERE a.id_acceso=? AND a.device_uid=?');
        $st->execute([$id,$uid]);$row=$st->fetch();
        if(!$row || $row['estado']!=='ACTIVO' || (int)$row['id_usuario']!==(int)$session['id_usuario']
            || $row['id_trabajador']===null || $row['estado_trabajador']!=='Activo' || ($row['situacion']??'VIGENTE')!=='VIGENTE'
            || !hash_equals($row['credencial_hmac'],hash_hmac('sha256',$row['password'],(string)Config::get('security.jwt_secret'))))throw new HttpException('El celular no esta autorizado para esta cuenta',403,'DEVICE_NOT_AUTHORIZED');
        return $row;
    }
    private static function unregistered(int $user): void
    {
        $st=Database::getConnection()->prepare('SELECT id_rostro FROM wp_rostros_cuenta WHERE id_usuario=?');$st->execute([$user]);
        if($st->fetch())throw new HttpException('Tu cuenta ya tiene un rostro registrado. No se sustituyo.',409,'FACE_ALREADY_REGISTERED');
    }
    private static function enrollmentNeeded(array $session,array $device): void {
        if(FaceReferenceService::attendance((int)$device['id_trabajador'])!==null)throw new HttpException('Tu cuenta ya tiene un rostro valido en asistencia; no necesitas registrarlo otra vez',409,'FACE_ALREADY_IN_ATTENDANCE');
        if(!FaceReferenceService::hasTable('wp_rostros_cuenta'))throw new HttpException('Instala V012 para el registro complementario',503,'FACIAL_SCHEMA_REQUIRED');
    }
    private static function digest(array $session,array $device,string $digest): string
    {
        return hash('sha256',implode(':',['WSP_FACE_ENROLL_V1',$session['id_sesion'],$session['id_usuario'],$device['id_acceso'],$device['device_uid'],$device['huella_clave'],$digest]));
    }
    private static function key(): string
    {
        $secret=(string)Config::get('security.jwt_secret','');
        if(strlen($secret)<32)throw new \RuntimeException('Face profile encryption key unavailable');
        return hash_hkdf('sha256',$secret,32,'websiteplus.face.enrollment.v1');
    }
    private static function limit(array $session,string $ip): void
    {
        if(!RateLimiter::checkLoginAttempts('face-enroll:'.$session['id_usuario'],$ip))throw new HttpException('Espera antes de reintentar',429,'TOO_MANY_ATTEMPTS');
        $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_desafios WHERE id_usuario=? AND referencia_tipo="rostro_registro" AND creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE)');$st->execute([$session['id_usuario']]);
        if((int)$st->fetchColumn()>=20)throw new HttpException('Espera antes de reintentar',429,'TOO_MANY_ATTEMPTS');
    }
    public static function validateSamples(string $payload): void
    {
        try {$samples=strlen($payload)<=16000?json_decode($payload,false,8,JSON_THROW_ON_ERROR):null;}
        catch(\JsonException $e){$samples=null;}
        if(!is_array($samples) || !array_is_list($samples) || count($samples)!==3)throw new HttpException('Se requieren tres capturas de rostro',422,'VALIDATION_FAILED');
        foreach($samples as $vector){
            if(!is_array($vector) || !array_is_list($vector) || count($vector)!==128)throw new HttpException('Perfil facial invalido',422,'VALIDATION_FAILED');
            $norm=0;foreach($vector as $v){if((!is_int($v)&&!is_float($v)) || !is_finite((float)$v) || abs($v)>10)throw new HttpException('Perfil facial invalido',422,'VALIDATION_FAILED');$norm+=$v*$v;}
            if($norm<.01 || $norm>100)throw new HttpException('Perfil facial invalido',422,'VALIDATION_FAILED');
        }
        for($a=0;$a<3;$a++)for($b=$a+1;$b<3;$b++){
            $distance=0;for($i=0;$i<128;$i++)$distance+=($samples[$a][$i]-$samples[$b][$i])**2;
            if(sqrt($distance)>.58)throw new HttpException('Las capturas no forman un perfil consistente',422,'INCONSISTENT_FACE');
        }
    }
}
