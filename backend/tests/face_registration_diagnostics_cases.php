<?php
declare(strict_types=1);
// Fault injection ONLY in the random owned laboratory, never production.
if(!isset($admin,$db,$app,$activeAccessId,$devicePrivateKey)||!preg_match('/^audit[a-f0-9]{6}_websiteplus_test$/D',$app))throw new RuntimeException('Owned synthetic harness required');
$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor=? WHERE id_trabajador=1")->execute([$syntheticDescriptorJson]);
$diagnosticGuardian=$db->query('SELECT * FROM wp_accesos_android ORDER BY id_acceso')->fetchAll();
$rowsBefore=(int)$db->query('SELECT COUNT(*) FROM wp_claves_faciales')->fetchColumn();
[$diagnosticPrivate,$diagnosticPublic]=androidKey();
$diagnosticBody=['id_acceso'=>$activeAccessId,'device_uid'=>$activeDeviceUid,'clave_publica'=>$diagnosticPublic];
$newPassword=WebSitePlus\Auth\TokenService::createSession(1,null,'PASSWORD','127.0.0.1',$fixtureUser['password'])['access_token'];
$oldPassword=WebSitePlus\Auth\TokenService::createSession(1,null,'PASSWORD','127.0.0.1',$fixtureUser['password'])['access_token'];
$db->prepare('UPDATE wp_sesiones SET creada_en=DATE_SUB(NOW(),INTERVAL 6 MINUTE) WHERE access_hash=?')->execute([hash('sha256',$oldPassword)]);
$fingerprintPassword=WebSitePlus\Auth\TokenService::createSession(1,null,'BIOMETRIA','127.0.0.1',$fixtureUser['password'],'ANDROID',$activeAccessId)['access_token'];
foreach([$oldPassword,$fingerprintPassword] as $token){
    $result=api('/auth/face/device/register',$diagnosticBody,$token);
    check($result['code']===403&&$result['error_code']==='FRESH_PASSWORD_REQUIRED','Registration begin refuses stale-password or V011 session with exact 403/FRESH_PASSWORD_REQUIRED');
    check((int)$db->query('SELECT COUNT(*) FROM wp_claves_faciales')->fetchColumn()===$rowsBefore,'Fresh-password refusal leaves facial credential table unchanged');
}
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$permissionId=(int)$db->query("SELECT id_permiso FROM wp_permisos WHERE codigo='auth.perfil'")->fetchColumn();
$roleScope=$db->query('SELECT alcance FROM wp_rol_permisos WHERE id_rol=5 AND id_permiso='.$permissionId)->fetchColumn();
if($permissionId<=0||$roleScope===false)throw new RuntimeException('Synthetic permission baseline required');
try {
    $db->exec('DELETE FROM wp_rol_permisos WHERE id_rol=5 AND id_permiso='.$permissionId);
    $result=api('/auth/face/device/register',$diagnosticBody,$newPassword);
    check($result['code']===403&&$result['error_code']==='FORBIDDEN','Exact reported REGISTER 403/FORBIDDEN reproduced when auth.perfil grant is absent');
    check((int)$db->query('SELECT COUNT(*) FROM wp_claves_faciales')->fetchColumn()===$rowsBefore,'Missing auth.perfil rejects before facial key INSERT');
}finally{$db->prepare('INSERT INTO wp_rol_permisos(id_rol,id_permiso,alcance) VALUES(5,?,?)')->execute([$permissionId,$roleScope]);}
try {
    $db->prepare("INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,alcance,efecto,motivo) VALUES(1,?,'PROPIO','DENEGAR','Synthetic permission diagnostic')")->execute([$permissionId]);
    $result=api('/auth/face/device/register',$diagnosticBody,$newPassword);
    check($result['code']===403&&$result['error_code']==='FORBIDDEN','Exact reported REGISTER 403/FORBIDDEN reproduced when individual DENEGAR overrides role grant');
    check((int)$db->query('SELECT COUNT(*) FROM wp_claves_faciales')->fetchColumn()===$rowsBefore,'Explicit auth.perfil denial leaves facial key table unchanged');
}finally{$db->prepare('DELETE FROM wp_usuario_permisos WHERE id_usuario=1 AND id_permiso=?')->execute([$permissionId]);}
$challengeRows=(int)$db->query('SELECT COUNT(*) FROM wp_desafios')->fetchColumn();
$admin->exec("CREATE TRIGGER `$app`.lab_fail_face_challenge BEFORE INSERT ON `$app`.wp_desafios FOR EACH ROW BEGIN IF NEW.referencia_tipo='face_key_register' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LAB_INJECTED_CHALLENGE_FAILURE'; END IF; END");
try {
    $result=api('/auth/face/device/register',$diagnosticBody,$newPassword);
    check($result['code']===500&&$result['error_code']==='INTERNAL_SERVER_ERROR','Injected begin challenge failure returns safe 500/INTERNAL_SERVER_ERROR');
    check(!str_contains(json_encode($result),'LAB_INJECTED')&&!str_contains(json_encode($result),'SQLSTATE'),'Storage failure never discloses SQL or injected detail to client');
    check((int)$db->query('SELECT COUNT(*) FROM wp_claves_faciales')->fetchColumn()===$rowsBefore,'Begin rolls back inserted facial key when challenge INSERT fails despite valid purpose ENUM');
    check((int)$db->query('SELECT COUNT(*) FROM wp_desafios')->fetchColumn()===$challengeRows,'Begin rollback also leaves challenge table unchanged');
}finally{$admin->exec("DROP TRIGGER `$app`.lab_fail_face_challenge");}
$result=api('/auth/face/device/register',$diagnosticBody,$newPassword);$registration=$result['data'];$diagnosticId=(int)$registration['id_clave'];
check($result['code']===200&&$db->query('SELECT estado FROM wp_claves_faciales WHERE id_clave='.$diagnosticId)->fetchColumn()==='PENDIENTE','Successful begin commits PENDIENTE before Android confirmation starts');
$confirmBody=['id_clave'=>$diagnosticId,'device_uid'=>$activeDeviceUid,'challenge_id'=>$registration['challenge_id'],'firma_base64'=>androidSignature($registration,$devicePrivateKey)];
$db->prepare('UPDATE wp_sesiones SET creada_en=DATE_SUB(NOW(),INTERVAL 6 MINUTE) WHERE access_hash=?')->execute([hash('sha256',$newPassword)]);
$result=api('/auth/face/device/confirm',$confirmBody,$newPassword);
check($result['code']===403&&$result['error_code']==='FRESH_PASSWORD_REQUIRED','Confirm separately enforces the same five-minute password window');
check($db->query('SELECT estado FROM wp_claves_faciales WHERE id_clave='.$diagnosticId)->fetchColumn()==='PENDIENTE','Confirm precondition failure retains previously committed PENDIENTE key');
$db->prepare('UPDATE wp_sesiones SET creada_en=NOW() WHERE access_hash=?')->execute([hash('sha256',$newPassword)]);
$admin->exec("CREATE TRIGGER `$app`.lab_fail_face_activate BEFORE UPDATE ON `$app`.wp_claves_faciales FOR EACH ROW BEGIN IF NEW.estado='ACTIVO' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='LAB_INJECTED_ACTIVATION_FAILURE'; END IF; END");
try {
    $result=api('/auth/face/device/confirm',$confirmBody,$newPassword);
    check($result['code']===500&&$result['error_code']==='INTERNAL_SERVER_ERROR','Injected confirm activation failure returns 500');
    check($db->query('SELECT estado FROM wp_claves_faciales WHERE id_clave='.$diagnosticId)->fetchColumn()==='PENDIENTE','Confirm transaction rollback does not delete begin key');
    $stored=$db->query('SELECT resultado,usado_en FROM wp_desafios WHERE id_desafio='.(int)$registration['challenge_id'])->fetch();
    check($stored['resultado']==='PENDIENTE'&&$stored['usado_en']===null,'Confirm rollback restores consumed challenge atomically');
}finally{$admin->exec("DROP TRIGGER `$app`.lab_fail_face_activate");}
// This explicit lab retry is justified by direct inspection of the owned transaction state;
// the application must NEVER infer this from a timeout or generic 500.
$result=api('/auth/face/device/confirm',$confirmBody,$newPassword);
check($result['code']===200&&$result['data']['active']===true&&$result['data']['facial_login_enabled']===false,'Known lab state can confirm key while facial sessions remain disabled');
check((int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE metodo='FACIAL'")->fetchColumn()===0,'Registration never issues FACIAL session');
check($db->query('SELECT * FROM wp_accesos_android ORDER BY id_acceso')->fetchAll()===$diagnosticGuardian,'Diagnostic fault injection never changes V011 guardian rows');
// Remove only the key/challenge fixtures introduced by this case in this random lab schema.
$db->prepare("DELETE FROM wp_desafios WHERE referencia_tipo='face_key_register' AND referencia_id=?")->execute([$diagnosticId]);
$db->prepare('DELETE FROM wp_claves_faciales WHERE id_clave=?')->execute([$diagnosticId]);
$db->exec('UPDATE wp_intentos_login SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
