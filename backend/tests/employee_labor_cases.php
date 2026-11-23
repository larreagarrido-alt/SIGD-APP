<?php
declare(strict_types=1);
// Owned random schemas only. All workers, signatures and sessions below are synthetic fixtures.
if(!isset($admin,$db,$source,$app)||!preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source))throw new RuntimeException('Owned employee laboratory required');
use WebSitePlus\Auth\TokenService;
$employeeConfig=labFaceSelfServiceConfig($cfg);
file_put_contents($path,'<?php return '.var_export($employeeConfig,true).';');
$db->exec('UPDATE wp_intentos_login SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$db->exec('UPDATE wp_desafios SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$today=date('Y-m-d');$past=date('Y-m-d',strtotime('-10 days'));
$admin->exec("INSERT INTO `$source`.agencias(id_agencia,nombre) VALUES(2,'Other synthetic agency')");
$insertWorker=$admin->prepare("INSERT INTO `$source`.trabajadores(id_trabajador,dni,nombres,apellidos,fecha_nacimiento,id_cargo,id_agencia,tipo,fecha_inicio,estado,id_supervisor,rostro_descriptor) VALUES(?,?,?,'Employee lab','2000-01-01',1,?,'Practicante',?,'Activo',?,?)");
foreach([[4,1,null],[5,1,4],[6,1,null],[7,2,null]] as [$id,$agency,$supervisor])$insertWorker->execute([$id,'0000000'.$id,'Synthetic '.$id,$agency,$past,$supervisor,$syntheticDescriptorJson]);
$pwd=password_hash('Synthetic-only-123!',PASSWORD_DEFAULT);
$admin->prepare("INSERT INTO `$source`.usuarios(id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password) VALUES(4,'employee-owner',?,5,4,0),(8,'employee-no-worker',?,1,NULL,0)")->execute([$pwd,$pwd]);
$permId=(int)$db->query("SELECT id_permiso FROM wp_permisos WHERE codigo='empleados.ver'")->fetchColumn();
$setScope=function(string $scope,string $effect='PERMITIR')use($db,$permId){$db->prepare('INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,alcance,efecto,motivo) VALUES(4,?,?,?,"Synthetic employee scope") ON DUPLICATE KEY UPDATE alcance=VALUES(alcance),efecto=VALUES(efecto)')->execute([$permId,$scope,$effect]);};
$ownerPassword=api('/auth/login',['username'=>'employee-owner','password'=>'Synthetic-only-123!'])['data']['auth']['access_token'];
$employeeAdmin=api('/auth/login',['username'=>'fixture-admin','password'=>'Synthetic-only-123!'])['data']['auth']['access_token'];
$ids=static fn(array $response):array=>array_values(array_unique(array_map('intval',array_column($response['data']['empleados'],'id_trabajador'))));
$sourceBefore=$admin->query("SELECT * FROM `$source`.trabajadores WHERE id_trabajador=4")->fetch(PDO::FETCH_ASSOC);
$kpiBefore=$db->query('SELECT * FROM wp_kpi_evaluaciones ORDER BY id_evaluacion')->fetchAll();
$revisionsBefore=$db->query('SELECT * FROM wp_kpi_revisiones ORDER BY id_revision')->fetchAll();
try{
    $setScope('PROPIO');
    $list=api('/personal',null,$ownerPassword);check($list['code']===200&&$ids($list)===[4],'Employee list PROPIO returns only own worker');
    check(api('/personal/4',null,$ownerPassword)['code']===200&&api('/personal/5',null,$ownerPassword)['code']===403,'Own detail allowed and another worker denied');
    $setScope('EQUIPO');$list=api('/personal',null,$ownerPassword);$visible=$ids($list);sort($visible);
    check($list['code']===200&&$visible===[4,5],'Employee list EQUIPO returns self and direct reports only');
    check(api('/personal/5',null,$ownerPassword)['code']===200&&api('/personal/6',null,$ownerPassword)['code']===403,'Team list and detail scopes agree');
    $setScope('AGENCIA');$list=api('/personal',null,$ownerPassword);$visible=$ids($list);
    check($list['code']===200&&in_array(4,$visible,true)&&in_array(6,$visible,true)&&!in_array(7,$visible,true),'Agency scope includes own agency and excludes another agency');
    $setScope('TODOS');check(in_array(7,$ids(api('/personal',null,$ownerPassword)),true),'Explicit TODOS scope lists other agencies');
    $setScope('PROPIO','DENEGAR');check(api('/personal',null,$ownerPassword)['code']===403&&api('/personal/4',null,$ownerPassword)['code']===403,'Individual denial overrides employee list and own detail');
    $setScope('PROPIO');
    $transition=['tipo_evento'=>'DESVINCULACION','motivo'=>'Synthetic labor test','fecha_efectiva'=>$today];
    check(api('/personal/4/transicion',$transition,$ownerPassword)['code']===403,'Employee read permission never grants transition administration');
    foreach([['motivo'=>'   '],['motivo'=>str_repeat('a',501)],['fecha_efectiva'=>'2026-02-30'],['fecha_efectiva'=>date('Y-m-d',strtotime('+1 day'))],['tipo_evento'=>'CONVERSION_TIPO']] as $invalid){
        check(api('/personal/4/transicion',array_replace($transition,$invalid),$employeeAdmin)['code']===422,'Transition validation rejects '.array_key_first($invalid));
    }
    check(api('/personal/999999/transicion',$transition,$employeeAdmin)['code']===404,'Nonexistent employee cannot acquire complementary state');
    // Attendance reads: current, pending, rejected, partial and externally changed permits.
    $leaveInsert=$admin->prepare("INSERT INTO `$source`.permisos_laborales(id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,motivo,estado,procesado_asistencia) VALUES(?,4,'Vacaciones',?,?,? ,?,0)");
    foreach([[40,'Aprobado'],[41,'Pendiente'],[42,'Rechazado']] as [$id,$state])$leaveInsert->execute([$id,$today,$today,'Synthetic only',$state]);
    $leavesBefore=$admin->query("SELECT * FROM `$source`.permisos_laborales WHERE id_trabajador=4 ORDER BY id_permiso_laboral")->fetchAll();
    $detail=api('/personal/4',null,$ownerPassword);
    check($detail['code']===200&&count($detail['data']['permisos_laborales'])===3&&$detail['data']['empleado']['condicion_operativa']==='DE_VACACIONES_O_PERMISO','Employee detail reads approved leave and retains pending/rejected history');
    check($admin->query("SELECT * FROM `$source`.permisos_laborales WHERE id_trabajador=4 ORDER BY id_permiso_laboral")->fetchAll()===$leavesBefore,'Reading employee permits never changes attendance processing flags');
    $admin->exec("UPDATE `$source`.permisos_laborales SET estado='Rechazado' WHERE id_permiso_laboral=40");
    $detail=api('/personal/4',null,$ownerPassword);
    check($detail['data']['empleado']['condicion_operativa']==='OPERATIVO'&&count($detail['data']['permisos_laborales'])===3,'Externally changed approval is reflected without cached app copies');
    $admin->exec("UPDATE `$source`.permisos_laborales SET estado='Aprobado' WHERE id_permiso_laboral=40");
    // A real V011 protocol registration and independent facial key, solely in this lab.
    $laborUid='aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';[$laborGuardian,$laborGuardianPublic]=androidKey();
    $g=api('/auth/biometria/registro',['device_uid'=>$laborUid,'clave_publica'=>$laborGuardianPublic,'nombre'=>'Synthetic employee phone'],$ownerPassword)['data'];$laborAndroid=(int)$g['id_acceso'];
    check(api('/auth/biometria/confirmar',['id_acceso'=>$laborAndroid,'device_uid'=>$laborUid,'challenge_id'=>$g['challenge_id'],'firma_base64'=>androidSignature($g,$laborGuardian)],$ownerPassword)['code']===200,'Employee fixture registers guardian through existing V011 API');
    [$laborFace,$laborFacePublic]=androidKey();$f=api('/auth/face/device/register',['id_acceso'=>$laborAndroid,'device_uid'=>$laborUid,'clave_publica'=>$laborFacePublic],$ownerPassword)['data'];$laborFaceId=(int)$f['id_clave'];
    check(api('/auth/face/device/confirm',['id_clave'=>$laborFaceId,'device_uid'=>$laborUid,'challenge_id'=>$f['challenge_id'],'firma_base64'=>androidSignature($f,$laborGuardian)],$ownerPassword)['data']['active'],'Employee fixture authorizes facial key without changing its authentication mechanism');
    $issue=function(string $method)use($laborUid,$laborAndroid,$laborGuardian,$laborFace,$laborFaceId,$syntheticDescriptor,$localPayload):array {
        if($method==='PASSWORD')return api('/auth/login',['username'=>'employee-owner','password'=>'Synthetic-only-123!'])['data']['auth'];
        if($method==='BIOMETRIA'){$c=api('/auth/biometria/desafio',['id_acceso'=>$laborAndroid,'device_uid'=>$laborUid])['data'];return api('/auth/biometria/login',['id_acceso'=>$laborAndroid,'device_uid'=>$laborUid,'challenge_id'=>$c['challenge_id'],'firma_base64'=>androidSignature($c,$laborGuardian)])['data'];}
        $identity=['id_clave'=>$laborFaceId,'device_uid'=>$laborUid];$c=api('/auth/face/device/challenge',$identity)['data'];$payload=$localPayload($syntheticDescriptor);
        openssl_sign($c['message']."\nFACE_PAYLOAD_SHA256:".hash('sha256',$payload),$signature,$laborFace,OPENSSL_ALGO_SHA256);
        return api('/auth/face/device/verify',$identity+['challenge_id'=>$c['challenge_id'],'nonce'=>$c['nonce'],'firma_base64'=>base64_encode($signature),'face_payload'=>$payload])['data']['auth'];
    };
    $restoreState=function()use($admin,$source,$db){$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=4");$db->exec("UPDATE wp_situacion_laboral SET situacion='VIGENTE' WHERE id_trabajador=4");};
    // The missing complementary row means VIGENTE, as before. Expired access must not mask refresh checks.
    foreach(['PASSWORD','BIOMETRIA','FACIAL'] as $method){
        $pair=$issue($method);$db->prepare('UPDATE wp_sesiones SET access_expira_en=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE access_hash=?')->execute([hash('sha256',$pair['access_token'])]);
        $fresh=api('/auth/refresh',['refresh_token'=>$pair['refresh_token']]);
        check($fresh['code']===200&&api('/auth/me',null,$fresh['data']['access_token'])['data']['user']['id_usuario']===4,'Expired access permits valid refresh while worker is active/on approved leave: '.$method);
        foreach(['ASISTENCIA_INACTIVO','DESVINCULADO'] as $state){
            $pair=$issue($method);$db->prepare('UPDATE wp_sesiones SET access_expira_en=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE access_hash=?')->execute([hash('sha256',$pair['access_token'])]);
            if($state==='ASISTENCIA_INACTIVO')$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=4");
            else $db->exec("INSERT INTO wp_situacion_laboral(id_trabajador,situacion,ultimo_estado_visto,ultimo_tipo_visto) VALUES(4,'DESVINCULADO','Activo','Practicante') ON DUPLICATE KEY UPDATE situacion='DESVINCULADO'");
            check(api('/auth/refresh',['refresh_token'=>$pair['refresh_token']])['code']===401,'Refresh directly rejects labor state without preceding access request: '.$method.' '.$state);
            $restoreState();
            check(api('/auth/refresh',['refresh_token'=>$pair['refresh_token']])['code']===401,'Reactivation cannot resurrect rejected refresh: '.$method.' '.$state);
        }
    }
    $noWorker=api('/auth/login',['username'=>'employee-no-worker','password'=>'Synthetic-only-123!'])['data']['auth'];
    check(api('/auth/me',null,$noWorker['access_token'])['code']===200&&api('/auth/refresh',['refresh_token'=>$noWorker['refresh_token']])['code']===200,'Administrative account without worker/type retains existing access and refresh policy');
    // Two accounts for the same worker must both be revoked, not only fetchColumn()'s first user.
    $admin->prepare("INSERT INTO `$source`.usuarios(id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password) VALUES(9,'employee-alias',?,5,4,0)")->execute([$pwd]);
    $pairs=[];foreach(['PASSWORD','BIOMETRIA','FACIAL'] as $method)$pairs[$method]=$issue($method);
    $pairs['SECOND_ACCOUNT']=api('/auth/login',['username'=>'employee-alias','password'=>'Synthetic-only-123!'])['data']['auth'];
    $st=$db->prepare('INSERT INTO wp_periodos_laborales(id_trabajador,tipo,id_cargo,id_agencia,fecha_inicio,fecha_fin,motivo_inicio,motivo_fin) VALUES(4,"Practicante",1,1,?,?,"CARGA_INICIAL","FIN_PRACTICAS")');
    $st->execute([date('Y-m-d',strtotime('-50 days')),date('Y-m-d',strtotime('-20 days'))]);$historicalId=(int)$db->lastInsertId();
    $historical=$db->query('SELECT * FROM wp_periodos_laborales WHERE id_periodo='.$historicalId)->fetch();
    $db->prepare('INSERT INTO wp_periodos_laborales(id_trabajador,tipo,id_cargo,id_agencia,fecha_inicio,motivo_inicio) VALUES(4,"Practicante",1,1,?,"CARGA_INICIAL")')->execute([$past]);$openId=(int)$db->lastInsertId();
    $closed=api('/personal/4/transicion',$transition,$employeeAdmin);check($closed['code']===200,'Employment termination commits complementary state and history');
    check((int)$db->query('SELECT COUNT(*) FROM wp_sesiones WHERE revocada_en IS NULL AND id_usuario IN (SELECT id_usuario FROM usuarios WHERE id_trabajador=4)')->fetchColumn()===0,'Termination immediately revokes every linked account before any protected access/refresh request');
    $event=$db->query('SELECT * FROM wp_eventos_laborales WHERE id_evento='.(int)$closed['data']['id_evento'])->fetch();
    check($event['valor_anterior']==='VIGENTE'&&$event['valor_nuevo']==='DESVINCULADO'&&(int)$event['id_periodo']===$openId,'Termination history stores actual prior complementary state and linked period');
    $period=$db->query('SELECT * FROM wp_periodos_laborales WHERE id_periodo='.$openId)->fetch();
    check($period['fecha_fin']===$today&&$period['motivo_fin']==='DESVINCULACION','Termination closes only current open employment period');
    foreach($pairs as $name=>$pair)if($name!=='SECOND_ACCOUNT')check(api('/auth/me',null,$pair['access_token'])['code']===401&&api('/auth/refresh',['refresh_token'=>$pair['refresh_token']])['code']===401,'Termination blocks existing access and refresh including all linked accounts: '.$name);
    $eventCount=(int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales WHERE id_trabajador=4')->fetchColumn();
    check(api('/personal/4/transicion',$transition,$employeeAdmin)['code']===409&&(int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales WHERE id_trabajador=4')->fetchColumn()===$eventCount,'Repeated termination is conflict, not duplicate history');
    $reentry=array_replace($transition,['tipo_evento'=>'REINGRESO']);
    $admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=4");
    check(api('/personal/4/transicion',$reentry,$employeeAdmin)['code']===409,'Reentry cannot override attendance inactivity');
    $admin->exec("UPDATE `$source`.trabajadores SET estado='Activo',tipo='Colaborador' WHERE id_trabajador=4");
    $returned=api('/personal/4/transicion',$reentry,$employeeAdmin);check($returned['code']===200,'Reentry requires prior termination and active attendance');
    $newPeriod=(int)$returned['data']['id_periodo'];$period=$db->query('SELECT * FROM wp_periodos_laborales WHERE id_periodo='.$newPeriod)->fetch();
    check($period['tipo']==='Colaborador'&&$period['fecha_fin']===null&&$period['motivo_inicio']==='REINGRESO','Reentry opens new period using current attendance type without rewriting former type');
    check($db->query('SELECT * FROM wp_periodos_laborales WHERE id_periodo='.$historicalId)->fetch()===$historical,'Already closed employment history remains byte-for-byte unchanged');
    check(api('/personal/4/transicion',$reentry,$employeeAdmin)['code']===409,'Repeated reentry cannot create another open period');
    foreach($pairs as $name=>$pair)check(api('/auth/refresh',['refresh_token'=>$pair['refresh_token']])['code']===401,'Reentry cannot restore tokens from terminated relationship: '.$name);
    $reactivate=array_replace($transition,['tipo_evento'=>'REACTIVACION']);
    check(api('/personal/4/transicion',$reactivate,$employeeAdmin)['code']===409,'Reactivation without observed prior inactivity is rejected');
    $db->exec("UPDATE wp_situacion_laboral SET ultimo_estado_visto='Inactivo' WHERE id_trabajador=4");
    $reactivated=api('/personal/4/transicion',$reactivate,$employeeAdmin);
    check($reactivated['code']===200&&(int)$reactivated['data']['id_periodo']===$newPeriod&&(int)$db->query('SELECT COUNT(*) FROM wp_periodos_laborales WHERE id_trabajador=4 AND fecha_fin IS NULL')->fetchColumn()===1,'Observed attendance return records reactivation without inventing pause dates or extra periods');
    $sourceAfter=$admin->query("SELECT * FROM `$source`.trabajadores WHERE id_trabajador=4")->fetch(PDO::FETCH_ASSOC);
    foreach(['estado','tipo'] as $changedByFixture)$sourceAfter[$changedByFixture]=$sourceBefore[$changedByFixture];
    check($sourceAfter===$sourceBefore,'Employee APIs never overwrite attendance worker fields (fixture state/type changes excluded)');
    check($db->query('SELECT * FROM wp_kpi_evaluaciones ORDER BY id_evaluacion')->fetchAll()===$kpiBefore&&$db->query('SELECT * FROM wp_kpi_revisiones ORDER BY id_revision')->fetchAll()===$revisionsBefore,'Employee operations never modify KPI evaluations or revisions');
    $db->prepare('UPDATE wp_periodos_laborales SET fecha_inicio=? WHERE id_periodo=?')->execute([date('Y-m-d',strtotime('+1 day')),$newPeriod]);
    try{
        check(api('/personal/4/transicion',$transition,$employeeAdmin)['code']===409,'Termination rejects a period whose start is after its requested closure');
        check($db->query('SELECT situacion FROM wp_situacion_laboral WHERE id_trabajador=4')->fetchColumn()==='VIGENTE'&&$db->query('SELECT fecha_fin FROM wp_periodos_laborales WHERE id_periodo='.$newPeriod)->fetchColumn()===null,'Invalid period chronology cannot partially terminate employment');
    }finally{$db->prepare('UPDATE wp_periodos_laborales SET fecha_inicio=? WHERE id_periodo=?')->execute([$today,$newPeriod]);}
    // Failure after state/period writes must roll back the full lifecycle operation.
    $rollbackToken=$issue('PASSWORD');$beforeEvents=(int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales WHERE id_trabajador=4')->fetchColumn();
    $admin->exec("CREATE TRIGGER `$app`.lab_employee_event_failure BEFORE INSERT ON `$app`.wp_eventos_laborales FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='SYNTHETIC_EMPLOYEE_ROLLBACK'");
    try{
        check(api('/personal/4/transicion',$transition,$employeeAdmin)['code']===500,'Injected event failure returns sanitized server error');
        check($db->query('SELECT situacion FROM wp_situacion_laboral WHERE id_trabajador=4')->fetchColumn()==='VIGENTE'&&$db->query('SELECT fecha_fin FROM wp_periodos_laborales WHERE id_periodo='.$newPeriod)->fetchColumn()===null,'Transition failure rolls back complementary state and period closure');
        check((int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales WHERE id_trabajador=4')->fetchColumn()===$beforeEvents&&api('/auth/me',null,$rollbackToken['access_token'])['code']===200,'Failed transition neither appends history nor revokes valid session');
    }finally{$admin->exec("DROP TRIGGER `$app`.lab_employee_event_failure");}
    // Separate PHP processes exercise database locking; the PHP HTTP lab server is single-threaded.
    $countBeforeRace=(int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales WHERE id_trabajador=4')->fetchColumn();
    $children=[];
    for($i=0;$i<4;$i++){
        $pipes=[];$process=proc_open([PHP_BINARY,__DIR__.'/labor_transition_process.php',$path,'4','2'],[1=>['pipe','w'],2=>['file',$dir.'/employee-race-'.$i.'.log','a']],$pipes);
        if(!is_resource($process))throw new RuntimeException('Cannot start employee concurrency test');
        $children[]=[$process,$pipes[1]];
    }
    $statuses=[];foreach($children as [$process,$output]){$out=stream_get_contents($output);fclose($output);$exit=proc_close($process);$r=json_decode($out,true);check($exit===0&&is_array($r),'Concurrent labor process finishes with sanitized result');$statuses[]=$r['status'];}
    sort($statuses);check($statuses===[200,409,409,409],'Four concurrent termination requests produce one success and three conflicts');
    check((int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales WHERE id_trabajador=4')->fetchColumn()===$countBeforeRace+1,'Concurrent termination appends exactly one immutable history event');
}finally{file_put_contents($path,'<?php return '.var_export($cfg,true).';');}
