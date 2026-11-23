<?php
declare(strict_types=1);
use WebSitePlus\Auth\TokenService;
if(!isset($activeAccessId,$devicePrivateKey)||!preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source))throw new RuntimeException('Isolated harness required');
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$db->exec('UPDATE wp_intentos_login SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor=? WHERE id_trabajador=1")->execute([$syntheticDescriptorJson]);
$facePassword=TokenService::createSession(1,null,'PASSWORD','127.0.0.1',$fixtureUser['password'])['access_token'];
$sessionCount=(int)$db->query('SELECT COUNT(*) FROM wp_sesiones')->fetchColumn();
$guardianBefore=$db->query('SELECT * FROM wp_accesos_android ORDER BY id_acceso')->fetchAll();
[$facialPrivate,$facialPublic]=androidKey();
$keyBody=['id_acceso'=>$activeAccessId,'device_uid'=>$activeDeviceUid,'clave_publica'=>$facialPublic,'id_usuario'=>2];
check(api('/auth/face/device/register',$keyBody)['code']===401,'Face key registration requires session');
check(api('/auth/face/device/register',$keyBody,$otherEnrollToken)['code']===403,'Face key registration requires matching V011 owner');
check(api('/auth/face/device/register',array_replace($keyBody,['clave_publica'=>'invalid']),$facePassword)['code']===422,'Face key registration requires P256 public key');
$begin=api('/auth/face/device/register',$keyBody,$facePassword)['data'];$faceId=$begin['id_clave'];
$confirm=['id_clave'=>$faceId,'device_uid'=>$activeDeviceUid,'challenge_id'=>$begin['challenge_id'],'firma_base64'=>androidSignature($begin,$facialPrivate)];
check(api('/auth/face/device/confirm',$confirm,$facePassword)['code']===401,'Face key cannot authorize itself instead of V011 guardian');
$begin=api('/auth/face/device/register',$keyBody,$facePassword)['data'];
$confirm['challenge_id']=$begin['challenge_id'];$confirm['firma_base64']=androidSignature($begin,$devicePrivateKey);
check(api('/auth/face/device/confirm',$confirm,$facePassword)['data']['active']===true,'Face key authorized by fresh password and V011 signature');
check(api('/auth/face/device/confirm',$confirm,$facePassword)['code']===403,'Face key confirmation cannot replay');
check(api('/auth/face/device/register',$keyBody,$facePassword)['data']['already_active']===true,'Face key registration heals lost active response without replacement');
$secondOwnUid='66666666-6666-4666-8666-666666666666';
[$secondOwnGuardian,$secondOwnPublic]=androidKey();
$secondOwnBegin=api('/auth/biometria/registro',['device_uid'=>$secondOwnUid,'clave_publica'=>$secondOwnPublic,'nombre'=>'Second device, same account'],$facePassword);
check($secondOwnBegin['code']===200,'same account may register a second Android device');
$secondOwnAccess=$secondOwnBegin['data']['id_acceso'];
check(api('/auth/biometria/confirmar',['id_acceso'=>$secondOwnAccess,'device_uid'=>$secondOwnUid,
    'challenge_id'=>$secondOwnBegin['data']['challenge_id'],'firma_base64'=>androidSignature($secondOwnBegin['data'],$secondOwnGuardian)],$facePassword)['code']===200,
    'second Android device requires its own signed possession proof');
[$secondOwnFaceKey,$secondOwnFacePublic]=androidKey();
$secondOwnFaceBegin=api('/auth/face/device/register',['id_acceso'=>$secondOwnAccess,'device_uid'=>$secondOwnUid,'clave_publica'=>$secondOwnFacePublic],$facePassword);
check($secondOwnFaceBegin['code']===200,'same account begins a second facial key on a distinct device');
$secondOwnFaceId=$secondOwnFaceBegin['data']['id_clave'];
check(api('/auth/face/device/confirm',['id_clave'=>$secondOwnFaceId,'device_uid'=>$secondOwnUid,
    'challenge_id'=>$secondOwnFaceBegin['data']['challenge_id'],'firma_base64'=>androidSignature($secondOwnFaceBegin['data'],$secondOwnGuardian)],$facePassword)['code']===200,
    'second facial key confirms with its own Android guardian');
check((int)$db->query('SELECT COUNT(*) FROM wp_claves_faciales WHERE id_usuario=1 AND estado="ACTIVO"')->fetchColumn()===2,
    'second facial confirmation preserves first device facial key');
$secondOwnRevoke=api('/auth/face/device/revoke-challenge',['id_clave'=>$secondOwnFaceId,'device_uid'=>$secondOwnUid],$facePassword);
check(api('/auth/face/device/revoke',['id_clave'=>$secondOwnFaceId,'device_uid'=>$secondOwnUid,
    'challenge_id'=>$secondOwnRevoke['data']['challenge_id'],'firma_base64'=>androidSignature($secondOwnRevoke['data'],$secondOwnGuardian)],$facePassword)['code']===200
    && (int)$db->query("SELECT COUNT(*) FROM wp_claves_faciales WHERE id_clave=$faceId AND estado='ACTIVO'")->fetchColumn()===1,
    'second facial revocation preserves first device facial key');
$db->prepare('DELETE FROM wp_claves_faciales WHERE id_acceso_android=?')->execute([$secondOwnAccess]);
$db->prepare('UPDATE wp_sesiones SET id_acceso_android=NULL WHERE id_acceso_android=?')->execute([$secondOwnAccess]);
$db->prepare('DELETE FROM wp_accesos_android WHERE id_acceso=?')->execute([$secondOwnAccess]);
$db->prepare('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE referencia_id IN (?,?)')->execute([$secondOwnAccess,$secondOwnFaceId]);
$identity=['id_clave'=>$faceId,'device_uid'=>$activeDeviceUid,'username'=>'fixture-admin'];
$challengeFn=fn()=>api('/auth/face/device/challenge',$identity)['data'];
$proofFn=function(array $c,string $payload,mixed $private)use($faceId,$activeDeviceUid){$signed=$c['message']."\nFACE_PAYLOAD_SHA256:".hash('sha256',$payload);openssl_sign($signed,$sig,$private,OPENSSL_ALGO_SHA256);return ['id_clave'=>$faceId,'device_uid'=>$activeDeviceUid,'challenge_id'=>$c['challenge_id'],'nonce'=>$c['nonce'],'firma_base64'=>base64_encode($sig),'face_payload'=>$payload];};
check(api('/auth/face/device/challenge',array_replace($identity,['device_uid'=>$unknownDeviceUid]))['code']===403,'Face challenge requires credential and matching installation, not UUID alone');
// Temporarily hide V012 only in this synthetic schema; restore even if an assertion fails.
$admin->exec("RENAME TABLE `$app`.wp_rostros_cuenta TO `$app`.lab_rostros_held");
try{check(api('/auth/face/device/challenge',$identity)['data']['reference_source']==='ASISTENCIA','Case A works without V012 table');}
finally{$admin->exec("RENAME TABLE `$app`.lab_rostros_held TO `$app`.wp_rostros_cuenta");}
$c=$challengeFn();$proof=$proofFn($c,$syntheticDescriptorJson,$facialPrivate);$proof['liveness_verified']=true;$proof['pad_verified']=true;
$evaluated=api('/auth/face/device/verify',$proof);
check($evaluated['code']===200&&$evaluated['data']['identity_match']===true&&$evaluated['data']['device_signature_valid']===true,'Independent facial key signs exact payload and verifies 1to1 attendance identity');
check($evaluated['data']['user']['username']==='fixture'&&$evaluated['data']['session_issued']===false&&$evaluated['data']['liveness_verified']===false&&$evaluated['data']['auth']===null,'Client PAD claims never enable sessions or change bound account');
check(api('/auth/face/device/verify',$proof)['code']===401,'Independent facial challenge cannot replay');
// These are untrusted request fields, NOT a simulated PAD verifier or a PAD evaluation.
$untrustedPad=['provider'=>'AWS_REKOGNITION','SessionId'=>'11111111-1111-4111-8111-111111111111','Status'=>'SUCCEEDED','Confidence'=>100];
$c=$challengeFn();$untrusted=$proofFn($c,$syntheticDescriptorJson,$facialPrivate);$untrusted['pad_receipt']=$untrustedPad;
$blocked=api('/auth/face/device/verify',$untrusted);
check($blocked['code']===200&&in_array($blocked['data']['status'],['PAD_REQUIRED','FACE_LOGIN_DISABLED'],true)&&$blocked['data']['liveness_verified']===false&&$blocked['data']['session_issued']===false&&$blocked['data']['auth']===null,'Client-shaped external provider receipt cannot authorize a facial session');
$c=$challengeFn();$wrongNonce=$proofFn($c,$syntheticDescriptorJson,$facialPrivate);$wrongNonce['nonce'].='A';
check(api('/auth/face/device/verify',$wrongNonce)['error_code']==='CHALLENGE_EXPIRED_OR_REUSED','Signed facial payload cannot bypass a mismatching server nonce');
$c=$challengeFn();$proof=$proofFn($c,$syntheticDescriptorJson,$devicePrivateKey);
check(api('/auth/face/device/verify',$proof)['code']===401,'V011 private key cannot impersonate separately registered facial key');
$c=$challengeFn();$proof=$proofFn($c,$syntheticDescriptorJson,$facialPrivate);$changed=$syntheticDescriptor;$changed[0]+=.01;$proof['face_payload']=json_encode($changed);
check(api('/auth/face/device/verify',$proof)['code']===401,'Altered face payload invalidates signature');
$c=$challengeFn();$foreign=array_fill(0,128,0);$foreign[0]=1;
check(api('/auth/face/device/verify',$proofFn($c,json_encode($foreign),$facialPrivate))['error_code']==='FACE_MISMATCH','Foreign face rejected for linked account');
$c=$challengeFn();check(api('/auth/face/device/verify',$proofFn($c,json_encode(array_fill(0,128,0)),$facialPrivate))['code']===422,'Zero forged descriptor rejected');
$db->exec('UPDATE wp_intentos_login SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$admin->exec("UPDATE `$source`.trabajadores SET rostro_descriptor='incompatible' WHERE id_trabajador=1");
check(api('/auth/face/device/challenge',$identity)['error_code']==='FACE_PROFILE_PENDING','Case B pending profile never becomes an authorized reference');
$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor=? WHERE id_trabajador=1")->execute([$syntheticDescriptorJson]);
$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=1");
check(api('/auth/face/device/challenge',$identity)['code']===403,'Facial key cannot bypass inactive employment');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=1");
$db->exec("INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,alcance,efecto,motivo) SELECT 1,id_permiso,'PROPIO','DENEGAR','FACE_TEST' FROM wp_permisos WHERE codigo='auth.perfil'");
check(api('/auth/face/device/challenge',$identity)['code']===403,'Facial key respects explicit access permission denial');
$db->exec("DELETE FROM wp_usuario_permisos WHERE motivo='FACE_TEST'");
try{
require __DIR__.'/face_local_login_cases.php';
[$rotatedPrivate,$rotatedPublic]=androidKey();$rotatedBody=array_replace($keyBody,['clave_publica'=>$rotatedPublic]);
$rotate=api('/auth/face/device/register',$rotatedBody,$facePassword)['data'];
check(api('/auth/face/device/confirm',['id_clave'=>$rotate['id_clave'],'device_uid'=>$activeDeviceUid,'challenge_id'=>$rotate['challenge_id'],'firma_base64'=>androidSignature($rotate,$devicePrivateKey)],$facePassword)['data']['active']===true,'Facial key rotation requires V011 proof');
check(api('/auth/face/device/challenge',$identity)['error_code']==='FACE_KEY_NOT_AUTHORIZED','Rotated former facial key stops working');
check(api('/auth/me',null,$faceTokenForRotation)['code']===401,'Rotating facial credential revokes its prior facial sessions');
$revoke=['id_clave'=>$rotate['id_clave'],'device_uid'=>$activeDeviceUid];
$r=api('/auth/face/device/revoke-challenge',$revoke,$facePassword)['data'];
check(api('/auth/face/device/revoke',$revoke+['challenge_id'=>$r['challenge_id'],'firma_base64'=>androidSignature($r,$devicePrivateKey)],$facePassword)['data']['revoked']===true,'Facial key revocation uses guardian signature');
check(api('/auth/face/device/challenge',$revoke)['error_code']==='FACE_KEY_NOT_AUTHORIZED','Revoked facial key rejected');
check((int)$db->query('SELECT COUNT(*) FROM wp_sesiones')->fetchColumn()===$sessionCount+$newFaceSessions,'Only two explicit successful local-PAD logins create sessions');
check($db->query('SELECT * FROM wp_accesos_android ORDER BY id_acceso')->fetchAll()===$guardianBefore,'Independent facial key leaves V011 entirely unchanged');
}finally{file_put_contents($path,'<?php return '.var_export($cfg,true).';');}
check(api('/health')['data']['facial_sessions_enabled']===($cfg['security']['face_local_pad_login_enabled']===true),'Facial tests restore the original isolated configuration');
