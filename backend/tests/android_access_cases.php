<?php
// Included only by integration.php, after random synthetic schemas have been created.
if (!isset($db,$source,$suffix) || !preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source)) { throw new RuntimeException('Isolated harness required'); }
// Earlier cases intentionally triggered lockouts; start this independent fixture window cleanly.
$db->exec('UPDATE wp_intentos_login SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$db->exec("DELETE rp FROM wp_rol_permisos rp JOIN wp_permisos p USING(id_permiso) WHERE p.codigo='auth.dispositivos.recuperar'");
function androidKey(): array {
    return labEcKey();
}
function androidSignature(array $challenge, mixed $key): string {
    openssl_sign($challenge['message'],$sig,$key,OPENSSL_ALGO_SHA256);return base64_encode($sig);
}
$login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);$bearer=$login['data']['auth']['access_token'];
$otherLogin=api('/auth/login',['username'=>'fixture-admin','password'=>'Synthetic-only-123!']);$otherBearer=$otherLogin['data']['auth']['access_token'];
[$androidKey,$androidPublic]=androidKey();[$foreignKey,$foreignPublic]=androidKey();
$uid='11111111-1111-4111-8111-111111111111';$otherUid='22222222-2222-4222-8222-222222222222';
$registration=['device_uid'=>$uid,'clave_publica'=>$androidPublic,'nombre'=>'Synthetic Android','id_usuario'=>2];
check(api('/auth/biometria/registro',$registration)['code']===401,'Android enrollment requires authenticated account');
check(api('/auth/biometria/registro',array_replace($registration,['clave_publica'=>'invalid']),$bearer)['code']===422,'Android enrollment rejects invalid key');
$start=api('/auth/biometria/registro',$registration,$bearer);check($start['code']===200,'Android creates pending registration');
$id=$start['data']['id_acceso'];$identity=['id_acceso'=>$id,'device_uid'=>$uid];
check((int)$db->query('SELECT id_usuario FROM wp_accesos_android WHERE id_acceso='.$id)->fetchColumn()===1,'Android ignores supplied account ID');
check(api('/auth/biometria/desafio',$identity)['error_code']==='REGISTRATION_INCOMPLETE','Android pending key cannot login');
$proof=$identity+['challenge_id'=>$start['data']['challenge_id'],'firma_base64'=>androidSignature($start['data'],$androidKey)];
check(api('/auth/biometria/confirmar',$proof,$otherBearer)['code']===403,'Android enrollment proof bound to password session owner');
check(api('/auth/biometria/confirmar',$proof,$bearer)['code']===200,'Android signed enrollment activates credential');
$secondStart=api('/auth/biometria/registro',array_replace($registration,['device_uid'=>$otherUid,'clave_publica'=>$foreignPublic]),$bearer);
check($secondStart['code']===200,'Android second device may register with fresh account password');
$secondIdentity=['id_acceso'=>$secondStart['data']['id_acceso'],'device_uid'=>$otherUid];
check(api('/auth/biometria/confirmar',$secondIdentity+['challenge_id'=>$secondStart['data']['challenge_id'],
    'firma_base64'=>androidSignature($secondStart['data'],$foreignKey)],$bearer)['code']===200,
    'Android reinstall replacement proves new key while first device remains active');
check((int)$db->query('SELECT COUNT(*) FROM wp_accesos_android WHERE id_usuario=1 AND estado="ACTIVO"')->fetchColumn()===2,
    'Android password recovery does not revoke another active device');
$db->prepare('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE referencia_tipo="android_acceso" AND referencia_id=?')->execute([$secondStart['data']['id_acceso']]);
$db->prepare('UPDATE wp_sesiones SET id_acceso_android=NULL WHERE id_acceso_android=?')->execute([$secondStart['data']['id_acceso']]);
$db->prepare('DELETE FROM wp_accesos_android WHERE id_acceso=?')->execute([$secondStart['data']['id_acceso']]);
check(api('/auth/biometria/registro',array_replace($registration,['clave_publica'=>$foreignPublic]),$otherBearer)['code']===409,'Android phone cannot register another account');
check(api('/auth/biometria/desafio',array_replace($identity,['device_uid'=>$otherUid]))['error_code']==='DEVICE_NOT_FOUND','Android another installation cannot request account challenge (DEVICE_NOT_FOUND)');
check(api('/auth/biometria/desafio',['id_acceso'=>999999,'device_uid'=>'33333333-3333-4333-8333-333333333333'])['error_code']==='DEVICE_NOT_FOUND','Android stale pre-V011 local credential reported as DEVICE_NOT_FOUND');
$challenge=api('/auth/biometria/desafio',$identity)['data'];
$wrong=$identity+['challenge_id'=>$challenge['challenge_id'],'firma_base64'=>androidSignature($challenge,$foreignKey)];
check(api('/auth/biometria/login',$wrong)['code']===401,'Android foreign private key rejected');
$correct=$identity+['challenge_id'=>$challenge['challenge_id'],'firma_base64'=>androidSignature($challenge,$androidKey)];
check(api('/auth/biometria/login',$correct)['code']===401,'Android failed challenge cannot be reused');
$challenge=api('/auth/biometria/desafio',$identity)['data'];$correct=$identity+['challenge_id'=>$challenge['challenge_id'],'firma_base64'=>androidSignature($challenge,$androidKey)];
$signedLogin=api('/auth/biometria/login',$correct);
check($signedLogin['code']===200,'Android signed login creates new session without username/password');
$androidToken=$signedLogin['data']['access_token'];
check(api('/auth/me',null,$androidToken)['data']['user']['id_usuario']===1,'Android signed session resolves registered account');
check(api('/auth/biometria/login',$correct)['code']===401,'Android successful signature replay rejected');
check(api('/auth/biometria/revocar',$identity,$bearer)['code']===403,'Android password session cannot unlink phone');
check(api('/auth/logout',[],$androidToken)['code']===200,'Android logout revokes session');
$challenge=api('/auth/biometria/desafio',$identity)['data'];
$signedLogin=api('/auth/biometria/login',$identity+['challenge_id'=>$challenge['challenge_id'],'firma_base64'=>androidSignature($challenge,$androidKey)]);
check($signedLogin['code']===200,'Android credential survives logout and creates another session');
$androidToken=$signedLogin['data']['access_token'];
check(api('/auth/biometria/recuperar',$identity+['motivo'=>'Synthetic recovery'],$otherBearer)['code']===403,'Android recovery requires separately granted privilege');
check(api('/auth/biometria/revocar',$identity,$androidToken)['code']===200,'Android owner can unlink after fresh signed login');
$revokedChallenge=api('/auth/biometria/desafio',$identity);
check($revokedChallenge['code']===401 && $revokedChallenge['error_code']==='DEVICE_REVOKED' && api('/auth/me',null,$androidToken)['code']===401,'Android unlinked key (DEVICE_REVOKED) and attached session denied');
$login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);$bearer=$login['data']['auth']['access_token'];
$next=api('/auth/biometria/registro',['device_uid'=>$otherUid,'clave_publica'=>$foreignPublic,'nombre'=>'Synthetic replacement'],$bearer);
check($next['code']===200,'Android replacement phone enrolls after prior unlink');
$nextId=$next['data']['id_acceso'];
$nextIdentity=['id_acceso'=>$nextId,'device_uid'=>$otherUid];
check(api('/auth/biometria/confirmar',$nextIdentity+['challenge_id'=>$next['data']['challenge_id'],'firma_base64'=>androidSignature($next['data'],$foreignKey)],$bearer)['code']===200,'Android replacement phone confirms possession');
$db->exec("INSERT INTO wp_usuario_permisos (id_usuario,id_permiso,alcance,efecto,motivo) SELECT 2,id_permiso,'TODOS','PERMITIR','Synthetic recovery fixture' FROM wp_permisos WHERE codigo='auth.dispositivos.recuperar'");
check(api('/auth/biometria/recuperar',$nextIdentity+['motivo'=>'Synthetic lost phone recovery'],$otherBearer)['code']===200,'Android explicitly authorized recovery revokes lost phone');
check((int)$db->query('SELECT COUNT(*) FROM wp_dispositivos')->fetchColumn()===1,'Android login enrollment does not approve KPI signing devices');
