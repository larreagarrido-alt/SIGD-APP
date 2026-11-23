<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;

use PDO;
use PDOException;
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;

/** Login credentials only. Does not authorize a device for KPI signatures. */
final class AndroidAccessService
{
    public static function freshPassword(array $session): void
    {
        if ($session['metodo'] !== 'PASSWORD' || strtotime($session['creada_en']) < time()-300) {
            throw new HttpException('Vuelve a ingresar con tu contrasena para registrar un celular',403,'FRESH_PASSWORD_REQUIRED');
        }
    }

    public static function begin(array $session, string $uid, string $pem, string $name, string $ip): array
    {
        self::freshPassword($session);
        if (!preg_match('/^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$/D',$uid) || strlen($pem)>1500) {
            throw new HttpException('Identificador o clave invalidos',422,'VALIDATION_FAILED');
        }
        $key=@openssl_pkey_get_public($pem);
        $details=$key ? openssl_pkey_get_details($key) : false;
        if (!$details || $details['type']!==OPENSSL_KEYTYPE_EC || ($details['ec']['curve_name']??'')!=='prime256v1') {
            throw new HttpException('Se requiere una clave EC P-256',422,'VALIDATION_FAILED');
        }
        $pem=$details['key'];
        $der=base64_decode(preg_replace('/-----[^-]+-----|\s/','',$pem),true);
        $fingerprint=hash('sha256',$der);
        self::limitChallenges((int)$session['id_usuario'],$ip);
        return self::transaction(function(PDO $db) use($session,$uid,$pem,$name,$fingerprint,$ip) {
            self::assertUnlinked($db,(int)$session['id_usuario'],$uid);
            $st=$db->prepare('INSERT INTO wp_accesos_android (id_usuario,device_uid,nombre,clave_publica,huella_clave,credencial_hmac,id_sesion_registro) VALUES (?,?,?,?,?,?,?)');
            $st->execute([$session['id_usuario'],$uid,mb_substr(trim($name)?:'Android',0,100),$pem,$fingerprint,$session['huella_credencial'],$session['id_sesion']]);
            $id=(int)$db->lastInsertId();
            return ['id_acceso'=>$id]+ChallengeService::createChallenge((int)$session['id_usuario'],null,'REGISTRO_DISPOSITIVO',$ip,'android_acceso',$id,$fingerprint);
        });
    }

    private static function assertUnlinked(PDO $db,int $user,string $uid): void
    {
        $st=$db->prepare('SELECT id_usuario FROM wp_accesos_android WHERE estado="ACTIVO" AND device_uid=? FOR UPDATE');
        $st->execute([$uid]);
        if ($st->fetch()) { throw new HttpException('Este celular ya tiene una vinculacion activa. Desvincula su acceso anterior o solicita recuperacion.',409,'ACCOUNT_ALREADY_LINKED'); }
    }

    public static function confirm(array $session,int $id,string $uid,int $challenge,string $signature,string $ip): array
    {
        self::freshPassword($session);
        $result=self::transaction(function(PDO $db) use($session,$id,$uid,$challenge,$signature) {
            $row=self::find($id,$uid,true);
            if (!$row || $row['estado']!=='PENDIENTE' || (int)$row['id_usuario']!==(int)$session['id_usuario']
                || (int)$row['id_sesion_registro']!==(int)$session['id_sesion']
                || !hash_equals($row['credencial_hmac'],$session['huella_credencial'])) {
                throw new HttpException('Registro no autorizado',403,'DEVICE_NOT_AUTHORIZED');
            }
            self::assertUnlinked($db,(int)$session['id_usuario'],$uid);
            if (!self::consume($row,$challenge,$signature,'REGISTRO_DISPOSITIVO')) { return null; }
            $st=$db->prepare('UPDATE wp_accesos_android SET estado="ACTIVO",verificado_en=NOW(),ultimo_uso_en=NOW() WHERE id_acceso=?');$st->execute([$id]);
            $st=$db->prepare('UPDATE wp_sesiones SET id_acceso_android=? WHERE id_sesion=?');$st->execute([$id,$session['id_sesion']]);
            return ['id_acceso'=>$id,'device_uid'=>$uid,'username'=>$session['username']];
        });
        if (!$result) { self::failed($id,$ip); }
        AuditService::log('auth','celular_vinculado','EXITO',(int)$session['id_usuario'],$session['username'],null,'wp_accesos_android',$id,null,$ip);
        return $result;
    }

    public static function challenge(int $id,string $uid,string $ip): array
    {
        self::limitAttempts($id,$ip);
        $row=self::find($id,$uid);
        self::allowed($row);
        self::limitChallenges((int)$row['id_usuario'],$ip);
        return ChallengeService::createChallenge((int)$row['id_usuario'],null,'LOGIN',$ip,'android_acceso',$id,$row['huella_clave']);
    }

    public static function login(int $id,string $uid,int $challenge,string $signature,string $ip): array
    {
        self::limitAttempts($id,$ip);
        $tokens=self::transaction(function(PDO $db) use($id,$uid,$challenge,$signature,$ip) {
            $row=self::find($id,$uid,true);
            self::allowed($row);
            if (!self::consume($row,$challenge,$signature,'LOGIN')) { return null; }
            $st=$db->prepare('UPDATE wp_accesos_android SET ultimo_uso_en=NOW() WHERE id_acceso=?');$st->execute([$id]);
            return TokenService::createSession((int)$row['id_usuario'],null,'BIOMETRIA',$ip,$row['password'],'ANDROID',$id);
        });
        if (!$tokens) { self::failed($id,$ip); }
        RateLimiter::recordAttempt('android:'.$id,$ip,true);
        AuditService::log('auth','login_clave_android','EXITO',null,null,null,'wp_accesos_android',$id,null,$ip);
        return $tokens;
    }

    private static function fingerprintRecoveryHash(array $session,array $row): string
    {
        return hash('sha256',implode('|',['FINGERPRINT_RECOVERY_V1',(int)$session['id_sesion'],
            (int)$row['id_acceso'],$row['device_uid'],$row['huella_clave'],$row['credencial_hmac']]));
    }

    private static function priorFingerprintRecovery(PDO $db,array $session,array $row): bool
    {
        $st=$db->prepare('SELECT 1 FROM wp_desafios WHERE id_usuario=? AND referencia_tipo="android_fingerprint_recovery" AND referencia_id=? AND hash_contenido=? AND resultado="VALIDO" AND usado_en IS NOT NULL LIMIT 1');
        $st->execute([$session['id_usuario'],$row['id_acceso'],self::fingerprintRecoveryHash($session,$row)]);
        return (bool)$st->fetchColumn();
    }

    /** A recent password authorizes the request; the existing private key must sign the challenge. */
    public static function fingerprintRecoveryBegin(array $session,int $id,string $uid,string $ip): array
    {
        self::freshPassword($session);
        self::limitAttempts($id,$ip);
        $row=self::find($id,$uid);
        if(!$row||$row['estado']!=='ACTIVO'||(int)$row['id_usuario']!==(int)$session['id_usuario']
            ||!hash_equals($row['credencial_hmac'],$session['huella_credencial']))
            throw new HttpException('Dispositivo no autorizado para recuperar huella',403,'DEVICE_NOT_AUTHORIZED');
        if((int)$row['huella_habilitada']!==0
            && !((int)$row['huella_habilitada']===1 && self::priorFingerprintRecovery(Database::getConnection(),$session,$row)))
            throw new HttpException('La huella ya esta habilitada',409,'FINGERPRINT_ALREADY_ENABLED');
        self::limitChallenges((int)$session['id_usuario'],$ip);
        return ['id_acceso'=>$id]+ChallengeService::createChallenge((int)$session['id_usuario'],null,
            'REGISTRO_DISPOSITIVO',$ip,'android_fingerprint_recovery',$id,
            self::fingerprintRecoveryHash($session,$row));
    }

    public static function fingerprintRecoveryConfirm(array $session,int $id,string $uid,int $challenge,string $signature,string $ip): array
    {
        self::freshPassword($session);
        self::limitAttempts($id,$ip);
        $result=self::transaction(function(PDO $db)use($session,$id,$uid,$challenge,$signature):?array{
            $row=self::find($id,$uid,true);
            if(!$row||$row['estado']!=='ACTIVO'||(int)$row['id_usuario']!==(int)$session['id_usuario']
                ||!hash_equals($row['credencial_hmac'],$session['huella_credencial']))
                throw new HttpException('Dispositivo no autorizado para recuperar huella',403,'DEVICE_NOT_AUTHORIZED');
            $alreadyConfirmed=(int)$row['huella_habilitada']===1
                && self::priorFingerprintRecovery($db,$session,$row);
            if((int)$row['huella_habilitada']!==0 && !$alreadyConfirmed)
                throw new HttpException('La huella ya esta habilitada',409,'FINGERPRINT_ALREADY_ENABLED');
            $st=$db->prepare('SELECT * FROM wp_desafios WHERE id_desafio=? FOR UPDATE');$st->execute([$challenge]);$c=$st->fetch();
            if(!$c||$c['proposito']!=='REGISTRO_DISPOSITIVO'||$c['referencia_tipo']!=='android_fingerprint_recovery'
                ||(int)$c['referencia_id']!==$id||(int)$c['id_usuario']!==(int)$session['id_usuario']
                ||$c['resultado']!=='PENDIENTE'||$c['usado_en']!==null||strtotime($c['expira_en'])<=time()
                ||!hash_equals((string)$c['hash_contenido'],self::fingerprintRecoveryHash($session,$row)))
                return null;
            $raw=strlen($signature)<=256?base64_decode($signature,true):false;
            $valid=$raw!==false&&@openssl_verify($c['mensaje'],$raw,$row['clave_publica'],OPENSSL_ALGO_SHA256)===1;
            $mark=$db->prepare('UPDATE wp_desafios SET usado_en=NOW(),resultado=? WHERE id_desafio=? AND usado_en IS NULL AND expira_en>NOW()');
            $mark->execute([$valid?'VALIDO':'FIRMA_INVALIDA',$challenge]);
            if(!$valid||$mark->rowCount()!==1)return null;
            if(!$alreadyConfirmed){
                $update=$db->prepare('UPDATE wp_accesos_android SET huella_habilitada=1 WHERE id_acceso=? AND id_usuario=? AND estado="ACTIVO" AND huella_habilitada=0');
                $update->execute([$id,$session['id_usuario']]);
                if($update->rowCount()!==1)throw new HttpException('No se confirmo la actualizacion de huella',409,'RECOVERY_STATE_CHANGED');
            }
            return ['id_acceso'=>$id,'huella_habilitada'=>true,'credential_updated'=>!$alreadyConfirmed];
        });
        if(!$result)self::failed($id,$ip);
        AuditService::log('auth','huella_recuperada','EXITO',(int)$session['id_usuario'],$session['username'],
            (int)$session['id_trabajador'],'wp_accesos_android',$id,null,$ip);
        return $result;
    }

    private static function find(int $id,string $uid,bool $lock=false): ?array
    {
        if ($lock) {
            $st=Database::getConnection()->prepare('SELECT id_acceso FROM wp_accesos_android WHERE id_acceso=? AND device_uid=? FOR UPDATE');$st->execute([$id,$uid]);
        }
        // Lock only WebSite+ rows; the attendance identity remains read-only.
        $st=Database::getConnection()->prepare('SELECT a.*,u.password,u.id_trabajador,t.estado AS estado_trabajador,sl.situacion FROM wp_accesos_android a JOIN usuarios u ON u.id_usuario=a.id_usuario LEFT JOIN trabajadores t ON t.id_trabajador=u.id_trabajador LEFT JOIN wp_situacion_laboral sl ON sl.id_trabajador=u.id_trabajador WHERE a.id_acceso=? AND a.device_uid=?');
        $st->execute([$id,$uid]);return $st->fetch()?:null;
    }

    private static function allowed(?array $row): void
    {
        if ($row && $row['estado']==='PENDIENTE') { throw new HttpException('Registro sin completar. Valida tu cuenta para repetir la vinculacion.',401,'REGISTRATION_INCOMPLETE'); }
        // Estados definitivos: la clave local nunca podra volver a usarse (no existe transicion REVOCADO->ACTIVO).
        // Solo se informa a quien ya conoce id_acceso + device_uid (UUID aleatorio de la instalacion).
        if (!$row) { throw new HttpException('Este celular no tiene una vinculacion registrada. Ingresa con tu contrasena para vincularlo.',401,'DEVICE_NOT_FOUND'); }
        if ($row['estado']==='REVOCADO') { throw new HttpException('La vinculacion de este celular fue revocada. Ingresa con tu contrasena para vincularlo de nuevo.',401,'DEVICE_REVOKED'); }
        if ($row['estado']!=='ACTIVO' || (int)$row['huella_habilitada']!==1
            || ($row['id_trabajador']!==null && ($row['estado_trabajador']!=='Activo' || ($row['situacion']??'VIGENTE')!=='VIGENTE'))
            || !hash_equals($row['credencial_hmac'],hash_hmac('sha256',$row['password'],(string)Config::get('security.jwt_secret')))) {
            throw new HttpException('Celular no autorizado. Valida tu cuenta para recuperar el acceso.',401,'DEVICE_NOT_AUTHORIZED');
        }
    }

    private static function consume(array $row,int $id,string $signature,string $purpose): bool
    {
        $db=Database::getConnection();
        $st=$db->prepare('SELECT * FROM wp_desafios WHERE id_desafio=? FOR UPDATE');$st->execute([$id]);$c=$st->fetch();
        if (!$c || $c['proposito']!==$purpose || (int)$c['id_usuario']!==(int)$row['id_usuario']
            || $c['referencia_tipo']!=='android_acceso' || (int)$c['referencia_id']!==(int)$row['id_acceso']
            || $c['resultado']!=='PENDIENTE' || $c['usado_en']!==null || strtotime($c['expira_en'])<=time()
            || !hash_equals((string)$c['hash_contenido'],$row['huella_clave'])) { return false; }
        $raw=strlen($signature)<=256 ? base64_decode($signature,true):false;
        $valid=$raw!==false && @openssl_verify($c['mensaje'],$raw,$row['clave_publica'],OPENSSL_ALGO_SHA256)===1;
        $st=$db->prepare('UPDATE wp_desafios SET usado_en=NOW(),resultado=? WHERE id_desafio=? AND usado_en IS NULL AND expira_en>NOW()');
        $st->execute([$valid?'VALIDO':'FIRMA_INVALIDA',$id]);
        return $valid && $st->rowCount()===1;
    }

    private static function limitAttempts(int $id,string $ip): void
    {
        if (!RateLimiter::checkLoginAttempts('android:'.$id,$ip)) { throw new HttpException('Demasiados intentos. Espera antes de reintentar.',429,'TOO_MANY_ATTEMPTS'); }
    }
    private static function failed(int $id,string $ip): never
    {
        RateLimiter::recordAttempt('android:'.$id,$ip,false,'FIRMA_INVALIDA');
        throw new HttpException('Comprobacion invalida, vencida o utilizada',401,'INVALID_SIGNATURE');
    }
    private static function limitChallenges(int $user,string $ip): void
    {
        $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_desafios WHERE creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE) AND id_usuario=?');$st->execute([$user]);
        if ((int)$st->fetchColumn()>=20) { throw new HttpException('Espera un minuto antes de reintentar',429,'TOO_MANY_ATTEMPTS'); }
        // A separate flood cap remains for an abusive IP; ordinary shared networks use per-user quotas.
        $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM wp_desafios WHERE creado_en>DATE_SUB(NOW(),INTERVAL 1 MINUTE) AND ip=?');$st->execute([$ip]);
        if ((int)$st->fetchColumn()>=(int)Config::get('security.max_ip_challenges_per_minute',120)) { throw new HttpException('Espera un minuto antes de reintentar',429,'TOO_MANY_ATTEMPTS'); }
    }
    private static function transaction(callable $fn): mixed
    {
        try { return Database::transaction($fn); }
        catch(PDOException $e) { if ($e->getCode()==='23000' || in_array((int)($e->errorInfo[1]??0),[1205,1213],true)) {
            throw new HttpException('La cuenta o celular ya tiene una vinculacion o registro en curso. Reintenta sin sustituir el acceso anterior.',409,'ACCOUNT_ALREADY_LINKED');
        } throw $e; }
    }

    public static function revoke(array $session,int $id,bool $recovery,string $reason,string $ip): void
    {
        if ($recovery) {
            self::freshPassword($session);
            if ((Authorization::permissions($session)['auth.dispositivos.recuperar']??'')!=='TODOS' || mb_strlen(trim($reason))<5 || mb_strlen($reason)>255) {
                throw new HttpException('Recuperacion requiere permiso autorizado y motivo',403,'FORBIDDEN');
            }
        } elseif ($session['metodo']!=='BIOMETRIA' || (int)($session['id_acceso_android']??0)!==$id || strtotime($session['creada_en'])<time()-300) {
            throw new HttpException('Confirma con la biometria del celular vinculado para desvincularlo',403,'BIOMETRIC_REAUTH_REQUIRED');
        }
        Database::transaction(function(PDO $db) use($session,$id,$recovery) {
            $st=$db->prepare('SELECT id_usuario FROM wp_accesos_android WHERE id_acceso=? FOR UPDATE');$st->execute([$id]);$owner=$st->fetchColumn();
            if (!$owner || (!$recovery && (int)$owner!==(int)$session['id_usuario'])) { throw new HttpException('Acceso no encontrado',404,'NOT_FOUND'); }
            $st=$db->prepare('UPDATE wp_claves_faciales SET estado="REVOCADO",revocado_en=NOW() WHERE id_acceso_android=? AND estado="ACTIVO"');$st->execute([$id]);
            $st=$db->prepare('UPDATE wp_accesos_android SET estado="REVOCADO",revocado_en=NOW() WHERE id_acceso=?');$st->execute([$id]);
            $st=$db->prepare('UPDATE wp_sesiones SET revocada_en=COALESCE(revocada_en,NOW()),motivo_revocacion="CELULAR_REVOCADO" WHERE id_acceso_android=?');$st->execute([$id]);
        });
        AuditService::log('auth',$recovery?'celular_recuperacion':'celular_desvinculado','EXITO',(int)$session['id_usuario'],$session['username'],null,'wp_accesos_android',$id,['motivo'=>$reason],$ip);
    }
}
