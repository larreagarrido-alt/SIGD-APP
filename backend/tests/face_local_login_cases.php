<?php
declare(strict_types=1);
// Signed protocol fixtures only: not optical PAD measurements. Synthetic isolated schemas required.
if(!isset($faceId,$facialPrivate,$proofFn)||!preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source))throw new RuntimeException('Isolated harness required');
$localPayload=fn(array $vector,array $pad=[])=>json_encode(['version'=>2,'descriptor'=>$vector,'pad'=>array_replace(['model'=>'MiniFASNetV1SE+V2-min','samples'=>7,'duration_ms'=>1600,'real_min'=>0.99],$pad)],JSON_THROW_ON_ERROR);
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$facialRowsBefore=(int)$db->query('SELECT COUNT(*) FROM wp_sesiones WHERE metodo="FACIAL"')->fetchColumn();
$disabledModes=['absent'=>null,'false'=>false,'null'=>null,'integer_zero'=>0,'integer_one'=>1,'string_true'=>'true','string_false'=>'false','string_one'=>'1','empty_array'=>[],'array_with_true'=>['enabled'=>true]];
try{
    foreach($disabledModes as $mode=>$value){
        $disabled=$cfg;
        if($mode==='absent')unset($disabled['security']['face_local_pad_login_enabled']);
        else $disabled['security']['face_local_pad_login_enabled']=$value;
        file_put_contents($path,'<?php return '.var_export($disabled,true).';');
        $health=api('/health');
        check($health['code']===200&&$health['data']['facial_sessions_enabled']===false&&$health['data']['facial_security_profile']==='DISABLED'
            &&$health['data']['build']==='face-self-service-20261022-v014'&&$health['data']['facial_activation_policy']==='SELF_SERVICE_EXPLICIT_GLOBAL_POLICY','Health fails closed for facial setting: '.$mode);
        $c=$challengeFn();check($c['facial_login_enabled']===false,'Facial challenge advertises disabled policy: '.$mode);
        $disabledResult=api('/auth/face/device/verify',$proofFn($c,$localPayload($syntheticDescriptor),$facialPrivate));
        check($disabledResult['code']===200&&$disabledResult['data']['identity_match']===true&&$disabledResult['data']['device_signature_valid']===true
            &&$disabledResult['data']['status']==='FACE_LOGIN_DISABLED'&&$disabledResult['data']['session_issued']===false&&$disabledResult['data']['auth']===null,'Signed valid V2 proof cannot create facial session: '.$mode);
    }
    check((int)$db->query('SELECT COUNT(*) FROM wp_sesiones WHERE metodo="FACIAL"')->fetchColumn()===$facialRowsBefore,'Every disabled configuration creates zero FACIAL rows');
}finally{file_put_contents($path,'<?php return '.var_export($cfg,true).';');}
// Explicit opt-in only in this isolated random synthetic harness; never touches private real config.
$policyKey=$db->query('SELECT * FROM wp_claves_faciales WHERE id_clave='.(int)$faceId)->fetch();
$enabled=labFaceSelfServiceConfig($cfg);
file_put_contents($path,'<?php return '.var_export($enabled,true).';');
check(api('/health')['data']['facial_sessions_enabled']===true&&api('/health')['data']['facial_self_service_enabled']===true,'Global self-service explicitly enabled without any account/device allowlist');
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$refused=[];
$bad=$enabled;$bad['security']['face_self_service']['enabled']=false;$refused['disabled']=$bad;
$bad=$enabled;$bad['security']['face_self_service']['local_pad_risk_accepted']=false;$refused['no_approval']=$bad;
$bad=$enabled;$bad['security']['face_self_service']['activation_id']='';$refused['missing_activation']=$bad;
$bad=$enabled;$bad['security']['face_self_service']['enabled']='true';$refused['string_true']=$bad;
$bad=$enabled;unset($bad['security']['face_self_service']);$refused['missing_policy']=$bad;
$bad=$enabled;$bad['security']['face_pilot']['enabled']=true;$refused['legacy_self-service_conflict']=$bad;
$bad=$enabled;$bad['security']['face_local_pad_login_enabled']=true;$refused['general_switch_true']=$bad;
$startFaceRows=(int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE metodo='FACIAL'")->fetchColumn();
try{
    foreach($refused as $case=>$config){
        file_put_contents($path,'<?php return '.var_export($config,true).';');
        $c=$challengeFn();check($c['facial_self_service_authorized']===false,'Self-service challenge refuses unapproved scope: '.$case);
        $denied=api('/auth/face/device/verify',$proofFn($c,$localPayload($syntheticDescriptor),$facialPrivate));
        check($denied['code']===200&&$denied['data']['session_issued']===false&&$denied['data']['auth']===null,'Even valid signed identity proof cannot issue outside self-service: '.$case);
    }
    check((int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE metodo='FACIAL'")->fetchColumn()===$startFaceRows,'All out-of-scope HTTP attempts create zero FACIAL sessions');
}finally{file_put_contents($path,'<?php return '.var_export($enabled,true).';');}
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$c=$challengeFn();$tampered=$proofFn($c,$localPayload($syntheticDescriptor),$facialPrivate);$tampered['face_payload']=str_replace('0.99','0.98',$tampered['face_payload']);
check(api('/auth/face/device/verify',$tampered)['code']===401,'Changing local PAD metadata invalidates protected signature');
// Invalid local evidence cannot create a session, even with a valid device signature.
foreach([['samples'=>4],['duration_ms'=>799],['real_min'=>0.94],['real_min'=>'0.99'],['model'=>'unknown']] as $bad){
    $c=$challengeFn();check(api('/auth/face/device/verify',$proofFn($c,$localPayload($syntheticDescriptor,$bad),$facialPrivate))['code']===422,'Incomplete local PAD envelope rejected: '.array_key_first($bad));
}
$c=$challengeFn();$validLocal=$proofFn($c,$localPayload($syntheticDescriptor),$facialPrivate);
$login=api('/auth/face/device/verify',$validLocal);
check($login['code']===200&&$login['data']['session_issued']===true&&$login['data']['security_profile']==='SELF_SERVICE_LOCAL_PAD'&&$login['data']['liveness_verified']===false,'Explicit scoped self-service creates real lab session without claiming independent PAD');
$faceToken=$login['data']['auth']['access_token'];$faceRefresh=$login['data']['auth']['refresh_token'];
$boundRow=$db->query('SELECT * FROM wp_sesiones ORDER BY id_sesion DESC LIMIT 1')->fetch();
check(preg_match('/^FS1\|[a-f0-9]{64}\|'.(int)$faceId.'$/D',$boundRow['user_agent'])===1,'Real self-service session stores account-device-key approval binding in existing server-owned metadata');
check(strtotime($boundRow['access_expira_en'])-time()<=900&&strtotime($boundRow['refresh_expira_en'])-time()<=3600,'Self-service access and refresh lifetimes capped without changing password/V011 lifetimes');
$audit=$db->query("SELECT detalles FROM wp_auditoria WHERE accion='login_facial_autoservicio' ORDER BY id_auditoria DESC LIMIT 1")->fetchColumn();
$auditFields=json_decode($audit,true);$auditNames=array_keys($auditFields);sort($auditNames);
check($auditNames===['activation_id','security_profile','server_pad_verified','session_issued']&&$auditFields['server_pad_verified']===false,'Self-service audit records approval and outcome only, no biometric/signature/token fields');
try{
    foreach($refused as $case=>$config){
        file_put_contents($path,'<?php return '.var_export($config,true).';');
        check(api('/auth/me',null,$faceToken)['code']===401&&api('/auth/refresh',['refresh_token'=>$faceRefresh])['code']===401,'Existing self-service access and refresh denied after policy withdrawal: '.$case);
    }
    $newEpoch=$enabled;$newEpoch['security']['face_self_service']['activation_id']='different-activation';file_put_contents($path,'<?php return '.var_export($newEpoch,true).';');
    check(api('/auth/me',null,$faceToken)['code']===401,'Reactivation with new epoch cannot revive earlier access token');
}finally{file_put_contents($path,'<?php return '.var_export($enabled,true).';');}
$db->exec("INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,alcance,efecto,motivo) SELECT 1,id_permiso,'PROPIO','DENEGAR','PILOT_SESSION_DENIAL' FROM wp_permisos WHERE codigo='auth.perfil'");
try{check(api('/auth/me',null,$faceToken)['code']===401&&api('/auth/refresh',['refresh_token'=>$faceRefresh])['code']===401,'Revoking own-profile permission also denies self-service access and refresh');}
finally{$db->exec("DELETE FROM wp_usuario_permisos WHERE motivo='PILOT_SESSION_DENIAL'");}
check($login['data']['user']['username']==='fixture'&&$login['data']['profile']['user']['id_usuario']===1,'Facial session belongs exclusively to linked account');
$profile=api('/auth/me',null,$faceToken);
check($profile['code']===200&&$profile['data']['user']['id_usuario']===1,'Facial access token opens authenticated profile of correct worker');
$sessionRow=$db->query('SELECT metodo,id_usuario,id_acceso_android FROM wp_sesiones ORDER BY id_sesion DESC LIMIT 1')->fetch();
check($sessionRow['metodo']==='FACIAL'&&(int)$sessionRow['id_usuario']===1&&(int)$sessionRow['id_acceso_android']===$activeAccessId,'Facial session distinguished from strong V011 biometrics and bound to authorized device');
check(api('/auth/biometria/revocar',['id_acceso'=>$activeAccessId],$faceToken)['code']===403,'Weaker facial session cannot authorize V011 device unlink');
check(api('/auth/face/device/verify',$validLocal)['code']===401,'Successful facial login cannot replay nonce or create second session');
$c=$challengeFn();check(api('/auth/face/device/verify',$proofFn($c,$localPayload($foreign),$facialPrivate))['error_code']==='FACE_MISMATCH','PAD envelope cannot bypass other persons face mismatch');
$disabled=$cfg;$disabled['security']['face_local_pad_login_enabled']=false;
file_put_contents($path,'<?php return '.var_export($disabled,true).';');
check(api('/auth/me',null,$faceToken)['code']===401,'Disabled switch blocks existing FACIAL access token');
check(api('/auth/refresh',['refresh_token'=>$faceRefresh])['code']===401,'Disabled switch blocks existing FACIAL refresh token');
check(api('/auth/me',null,$facePassword)['code']===200,'Facial switch does not block PASSWORD session');
check(api('/auth/me',null,$biometricEnrollToken)['code']===200,'Facial switch does not block V011 BIOMETRIA session');
file_put_contents($path,'<?php return '.var_export($enabled,true).';');
$rotatedFace=api('/auth/refresh',['refresh_token'=>$faceRefresh]);
check($rotatedFace['code']===200&&api('/auth/me',null,$rotatedFace['data']['access_token'])['code']===200,'Facial session supports normal refresh');
$refreshedRow=$db->query('SELECT access_expira_en,refresh_expira_en FROM wp_sesiones WHERE id_sesion='.(int)$boundRow['id_sesion'])->fetch();
check($rotatedFace['data']['expires_in']<=900&&strtotime($refreshedRow['access_expira_en'])-time()<=900&&strtotime($refreshedRow['refresh_expira_en'])-time()<=3600,'Refreshing self-service session retains both bounded lifetimes');
$faceTokenForRotation=$rotatedFace['data']['access_token'];
$c=$challengeFn();$secondFace=api('/auth/face/device/verify',$proofFn($c,$localPayload($syntheticDescriptor),$facialPrivate));
check($secondFace['code']===200&&api('/auth/logout',[],$secondFace['data']['auth']['access_token'])['code']===200&&api('/auth/me',null,$secondFace['data']['auth']['access_token'])['code']===401,'Facial logout revokes session without removing device binding');
$newFaceSessions=2;
