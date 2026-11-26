<?php
declare(strict_types=1);
// Only synthetic source mutations by the verified local lab administrator.
if(!isset($admin,$db,$source,$app)||!preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source))throw new RuntimeException('Owned laboratory required');
use WebSitePlus\Employees\LaborObservationService;
use WebSitePlus\Auth\TokenService;
$admin->exec("INSERT INTO `$source`.cargos(id_cargo,nombre,sueldo_base) VALUES(104,'Synthetic new job',0)");
$admin->exec("INSERT INTO `$source`.trabajadores(id_trabajador,dni,nombres,apellidos,fecha_nacimiento,id_cargo,id_agencia,tipo,fecha_inicio,estado) VALUES(104,'00000104','Observed','Synthetic','2000-01-01',1,1,'Practicante','2026-01-01','Activo'),(105,'00000105','Inactive','Synthetic','2000-01-01',1,1,'Colaborador','2026-01-01','Inactivo')");
$observationHash=password_hash('Synthetic-only-123!',PASSWORD_DEFAULT);
$admin->prepare("INSERT INTO `$source`.usuarios(id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password) VALUES(104,'observation-owner',?,5,104,0),(105,'observation-alias',?,5,104,0)")->execute([$observationHash,$observationHash]);
$readObservation=static function(int $worker)use($db):array{
    return [
        $db->query('SELECT * FROM wp_situacion_laboral WHERE id_trabajador='.$worker)->fetch(),
        $db->query('SELECT * FROM wp_eventos_laborales WHERE id_trabajador='.$worker.' ORDER BY id_evento')->fetchAll(),
        $db->query('SELECT * FROM wp_periodos_laborales WHERE id_trabajador='.$worker.' ORDER BY id_periodo')->fetchAll(),
    ];
};
$baseline=LaborObservationService::observe(104);
check($baseline['eventos']===1,'Initial observation records baseline, not a reconstructed employment period');
check($readObservation(104)[2]===[],'Initial source start date never invents historical periods');
$initial=$readObservation(104);$repeat=LaborObservationService::observe(104);
check($repeat['eventos']===0&&$readObservation(104)===$initial,'Unchanged source leaves snapshot timestamps and immutable history identical');
LaborObservationService::observe(105);
check($db->query('SELECT situacion FROM wp_situacion_laboral WHERE id_trabajador=105')->fetchColumn()==='VIGENTE','Initially inactive collaborator remains an observed restriction, not a termination');
$activeRoster=api('/personal',null,$employeeAdmin);
$activeIds=array_map('intval',array_column($activeRoster['data']['empleados']??[],'id_trabajador'));
check($activeRoster['code']===200 && in_array(104,$activeIds,true) && !in_array(105,$activeIds,true),
    'Personal shows active attendance workers and hides inactive workers');
// The first observation itself must be idempotent when no complementary row exists yet.
$admin->exec("INSERT INTO `$source`.trabajadores(id_trabajador,dni,nombres,apellidos,fecha_nacimiento,id_cargo,id_agencia,tipo,fecha_inicio,estado) VALUES(106,'00000106','Concurrent','Baseline','2000-01-01',1,1,'Practicante','2026-01-01','Activo')");
$children=[];
for($i=0;$i<4;$i++){
    $pipes=[];$p=proc_open([PHP_BINARY,__DIR__.'/labor_transition_process.php',$path,'106','2','observe'],[1=>['pipe','w'],2=>['file',$dir.'/observation-baseline-'.$i.'.log','a']],$pipes);
    if(!is_resource($p))throw new RuntimeException('Cannot launch baseline race');$children[]=[$p,$pipes[1]];
}
foreach($children as [$p,$output]){$r=json_decode(stream_get_contents($output),true);fclose($output);$exit=proc_close($p);check($exit===0&&($r['status']??0)===200,'Concurrent first observation completes with a shared initialized row');}
check(count($readObservation(106)[1])===1&&$readObservation(106)[2]===[],'Concurrent first observations append exactly one baseline and no invented periods');
$db->exec('INSERT INTO wp_periodos_laborales(id_trabajador,tipo,id_cargo,id_agencia,fecha_inicio,motivo_inicio) VALUES(104,"Practicante",1,1,"2026-01-01","CARGA_INICIAL")');
$originalPeriods=$readObservation(104)[2];
$a=TokenService::createSession(104,null,'PASSWORD','127.0.0.1',$observationHash);
$b=TokenService::createSession(105,null,'PASSWORD','127.0.0.1',$observationHash);
$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo',tipo='Colaborador',id_cargo=104,id_agencia=2 WHERE id_trabajador=104");
$observed=LaborObservationService::observe(104);$snapshot=$readObservation(104);
check(!in_array(104,array_map('intval',array_column(api('/personal',null,$employeeAdmin)['data']['empleados']??[],'id_trabajador')),true),
    'Inactivation in Asistencias removes a worker from Personal without deleting their account');
check($observed['eventos']===4&&$observed['sesiones_revocadas']===2,'Inactivity plus type, job and agency changes append four events and revoke both accounts');
check($snapshot[0]['situacion']==='VIGENTE'&&$snapshot[0]['ultimo_estado_visto']==='Inactivo','Temporary inactivity never changes complementary termination status');
check($snapshot[2]===$originalPeriods,'Source changes preserve period values and dates without fabricated boundaries');
$lastEvents=array_slice($snapshot[1],-4);
check(array_column($lastEvents,'tipo_evento')===['INACTIVACION','CONVERSION_TIPO','CAMBIO_CARGO','CAMBIO_AGENCIA'],'Observed history distinguishes all source fields');
check(count(array_filter($lastEvents,static fn($e)=>$e['origen']==='SINCRONIZACION'&&$e['id_periodo']===null&&$e['id_usuario_responsable']===null&&str_contains($e['motivo'],'fecha efectiva y motivo originales no disponibles')))===4,'Observation events explicitly identify unknown effective dates and never pretend to be period transitions');
check(api('/auth/login',['username'=>'observation-owner','password'=>'Synthetic-only-123!'])['code']===403,'Inactive observed worker cannot start a password session');
check(api('/auth/me',null,$a['access_token'])['code']===401&&api('/auth/refresh',['refresh_token'=>$a['refresh_token']])['code']===401,'Observed inactivity blocks existing access and refresh');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=104");
$return=LaborObservationService::observe(104);
check(in_array(104,array_map('intval',array_column(api('/personal',null,$employeeAdmin)['data']['empleados']??[],'id_trabajador')),true),
    'Reactivation in Asistencias restores the worker to Personal automatically');
check($return['eventos']===1&&$db->query('SELECT tipo_evento FROM wp_eventos_laborales WHERE id_trabajador=104 ORDER BY id_evento DESC LIMIT 1')->fetchColumn()==='REACTIVACION','Inactive to active records return only with a prior locked observation');
check(api('/auth/refresh',['refresh_token'=>$b['refresh_token']])['code']===401&&TokenService::validateAccessToken($b['access_token'])===null,'A second account never used during inactivity cannot resurrect its old tokens');
check(api('/auth/login',['username'=>'observation-owner','password'=>'Synthetic-only-123!'])['code']===200,'Reactivated worker can establish a new session');
$stable=$readObservation(104);LaborObservationService::observe(104);
check($readObservation(104)===$stable,'Repeated active observation cannot duplicate reactivation');
// An access/refresh rejection must persist revocation of every account, even without employee reads.
$a=TokenService::createSession(104,null,'PASSWORD','127.0.0.1',$observationHash);
$b=TokenService::createSession(105,null,'PASSWORD','127.0.0.1',$observationHash);
$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=104");
check(TokenService::rotateRefreshToken($a['refresh_token'],'127.0.0.1')===null,'Direct refresh observes external inactivity without a preceding employee request');
check((int)$db->query('SELECT COUNT(*) FROM wp_sesiones WHERE id_usuario IN(104,105) AND revocada_en IS NULL')->fetchColumn()===0,'Refresh restriction revokes all sessions of all accounts sharing that worker');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=104");
check(api('/personal/104',null,$employeeAdmin)['code']===200,'Authorized detail automatically observes source reactivation');
check(TokenService::rotateRefreshToken($b['refresh_token'],'127.0.0.1')===null,'Source reactivation never restores unpresented secondary refresh');
// Preserve the established leave policy; do not copy or mutate leave records.
$admin->exec("INSERT INTO `$source`.permisos_laborales(id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,motivo,estado,procesado_asistencia) VALUES(104,104,'Vacaciones',CURRENT_DATE(),CURRENT_DATE(),'Synthetic','Aprobado',0)");
$beforeLeave=$readObservation(104);$leaveSource=$admin->query("SELECT * FROM `$source`.permisos_laborales WHERE id_permiso_laboral=104")->fetchAll();
$detail=api('/personal/104',null,$employeeAdmin);
check($detail['data']['empleado']['condicion_operativa']==='DE_VACACIONES_O_PERMISO'&&$readObservation(104)===$beforeLeave,'Approved vacation is read live without inactivity, termination or redundant labor history');
check(count(array_filter($detail['data']['eventos_transicion'],static fn($e)=>$e['origen']==='SINCRONIZACION'&&$e['fecha_efectiva_confirmada']===false&&$e['observado_en']!==null))===count($detail['data']['eventos_transicion']),'API explicitly distinguishes observation time from an unconfirmed effective date without removing legacy fields');
check($admin->query("SELECT * FROM `$source`.permisos_laborales WHERE id_permiso_laboral=104")->fetchAll()===$leaveSource,'Observation never changes approved source leave or its processing flags');
$term=['tipo_evento'=>'DESVINCULACION','motivo'=>'Synthetic explicit termination'];
check(api('/personal/104/transicion',$term,$employeeAdmin)['code']===200,'Explicit termination remains independent of source vacation');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=104");LaborObservationService::observe(104);
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=104");LaborObservationService::observe(104);
check($db->query('SELECT situacion FROM wp_situacion_laboral WHERE id_trabajador=104')->fetchColumn()==='DESVINCULADO','Source return cannot automatically undo explicit termination or fabricate reentry');
check(api('/auth/login',['username'=>'observation-owner','password'=>'Synthetic-only-123!'])['code']===403,'Source active status alone cannot authorize a terminated account');
check(api('/personal/104/transicion',['tipo_evento'=>'REINGRESO','motivo'=>'Synthetic approved reentry'],$employeeAdmin)['code']===200,'Authorized explicit reentry still opens its own reviewed period');
// No scoped read may cause observation writes for employees outside the response.
$outside=$readObservation(7);$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=7");
$db->prepare('INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,alcance,efecto,motivo) VALUES(104,?,"PROPIO","PERMITIR","Synthetic observation scope")')->execute([$permId]);
$limited=api('/auth/login',['username'=>'observation-owner','password'=>'Synthetic-only-123!'])['data']['auth']['access_token'];
$ownList=api('/personal',null,$limited);
check($ownList['code']===200&&array_unique(array_column($ownList['data']['empleados'],'id_trabajador'))===[104],'Existing PROPIO list remains scoped during automatic observation');
check(api('/personal/7',null,$limited)['code']===403&&$readObservation(7)===$outside,'Out-of-scope detail and filtered list never synchronize unauthorized workers');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=7");
// Event failure rolls back the snapshot and revocation in the same transaction.
$rollbackSession=TokenService::createSession(104,null,'PASSWORD','127.0.0.1',$observationHash);
$beforeFailure=$readObservation(104);
$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=104");
$admin->exec("CREATE TRIGGER `$app`.lab_observation_failure BEFORE INSERT ON `$app`.wp_eventos_laborales FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='SYNTHETIC_OBSERVATION_FAILURE'");
try{
    $failed=false;try{LaborObservationService::observe(104);}catch(PDOException $e){$failed=true;}
    check($failed&&$readObservation(104)===$beforeFailure,'Injected observation error rolls back snapshot and append-only history');
    $rev=$db->prepare('SELECT revocada_en FROM wp_sesiones WHERE id_sesion=?');$rev->execute([$rollbackSession['session_id']]);
    check($rev->fetchColumn()===null,'Failed observation leaves no partially committed revocation');
    check(api('/personal/104',null,$employeeAdmin)['code']===500,'Observation failure returns a sanitized HTTP error instead of silent success');
}finally{$admin->exec("DROP TRIGGER `$app`.lab_observation_failure");}
$recovered=LaborObservationService::observe(104);
check($recovered['eventos']===1&&TokenService::validateAccessToken($rollbackSession['access_token'])===null,'Explicit retry after known rollback records exactly one change and enforces revocation');
// Independent PHP processes test actual InnoDB serialization (not single-threaded HTTP).
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo',tipo='Practicante',id_cargo=1,id_agencia=1 WHERE id_trabajador=104");
$eventsBefore=count($readObservation(104)[1]);$children=[];
for($i=0;$i<4;$i++){
    $pipes=[];$p=proc_open([PHP_BINARY,__DIR__.'/labor_transition_process.php',$path,'104','2','observe'],[1=>['pipe','w'],2=>['file',$dir.'/observation-race-'.$i.'.log','a']],$pipes);
    if(!is_resource($p))throw new RuntimeException('Cannot launch observation race');$children[]=[$p,$pipes[1]];
}
foreach($children as [$p,$output]){$r=json_decode(stream_get_contents($output),true);fclose($output);$exit=proc_close($p);check($exit===0&&($r['status']??0)===200,'Concurrent observation completes without conflicting history');}
check(count($readObservation(104)[1])===$eventsBefore+4,'Four concurrent observers append each of four changes exactly once');
$final=$readObservation(104);LaborObservationService::observe(104);
check($readObservation(104)===$final,'Post-concurrency synchronization is a complete no-op');
// A blocked observer must re-read source after acquiring the common lock.
$beforeWait=count($readObservation(104)[1]);
$db->beginTransaction();$db->query('SELECT * FROM wp_situacion_laboral WHERE id_trabajador=104 FOR UPDATE')->fetch();
$waitProcess=null;$waitPipes=[];
try{
    $admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=104");
    $waitProcess=proc_open([PHP_BINARY,__DIR__.'/labor_transition_process.php',$path,'104','2','observe'],[1=>['pipe','w'],2=>['file',$dir.'/observation-wait.log','a']],$waitPipes);
    if(!is_resource($waitProcess))throw new RuntimeException('Cannot launch locked observer');
    $deadline=microtime(true)+5;$waiting=false;
    $waitQuery=$admin->prepare('SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB=? AND INFO LIKE "INSERT INTO wp_situacion_laboral%"');
    do{$waitQuery->execute([$app]);$waiting=(int)$waitQuery->fetchColumn()>0;if(!$waiting)usleep(20000);}while(!$waiting&&microtime(true)<$deadline);
    check($waiting,'Concurrent observer demonstrably waits on the shared complementary lock');
    $admin->exec("UPDATE `$source`.trabajadores SET estado='Activo',id_cargo=104 WHERE id_trabajador=104");
    $db->commit();
}finally{if($db->inTransaction())$db->rollBack();}
$waitResult=json_decode(stream_get_contents($waitPipes[1]),true);fclose($waitPipes[1]);$waitExit=proc_close($waitProcess);
check($waitExit===0&&($waitResult['status']??0)===200,'Blocked observer completes after source changes and lock release');
$afterWait=$readObservation(104);
check(count($afterWait[1])===$beforeWait+1&&$afterWait[0]['ultimo_estado_visto']==='Activo'&&(int)$afterWait[0]['ultimo_cargo_visto']===104,'Observer uses fresh source after lock, never replays a stale inactive snapshot');
// A failure during final revocation must undo the earlier event and snapshot writes as well.
$lateSession=TokenService::createSession(104,null,'PASSWORD','127.0.0.1',$observationHash);
$beforeLateFailure=$readObservation(104);$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=104");
$admin->exec("CREATE TRIGGER `$app`.lab_observation_revoke_failure BEFORE UPDATE ON `$app`.wp_sesiones FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='SYNTHETIC_LATE_FAILURE'");
try{
    $failed=false;try{LaborObservationService::observe(104);}catch(PDOException $e){$failed=true;}
    check($failed&&$readObservation(104)===$beforeLateFailure,'Late revocation error rolls back already written observation events and snapshot');
}finally{$admin->exec("DROP TRIGGER `$app`.lab_observation_revoke_failure");}
LaborObservationService::observe(104);
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=104");LaborObservationService::observe(104);
check(TokenService::rotateRefreshToken($lateSession['refresh_token'],'127.0.0.1')===null,'Successful retry revokes sessions permanently before later reactivation');
// Scope-preserving filter composition is unchanged.
foreach(['/personal?tipo=Practicante&estado=Activo&id_cargo=104&q=Observed','/personal?tipo=Colaborador&q=Observed'] as $n=>$route){
    $filtered=api($route,null,$employeeAdmin);
    check($filtered['code']===200&&count(array_unique(array_column($filtered['data']['empleados'],'id_trabajador')))===($n===0?1:0),'Employee filters compose with permission scopes during observation');
}
// Manual termination and source observation share the SAME lock and cannot lose a field edge.
$beforeMixed=count($readObservation(104)[1]);$admin->exec("UPDATE `$source`.trabajadores SET id_agencia=2 WHERE id_trabajador=104");
$children=[];
foreach(['observe','transition','observe','transition'] as $i=>$mode){
    $pipes=[];$p=proc_open([PHP_BINARY,__DIR__.'/labor_transition_process.php',$path,'104','2',$mode],[1=>['pipe','w'],2=>['file',$dir.'/observation-mixed-'.$i.'.log','a']],$pipes);
    if(!is_resource($p))throw new RuntimeException('Cannot launch mixed race');$children[]=[$p,$pipes[1]];
}
$mixed=[];
foreach($children as [$p,$output]){$r=json_decode(stream_get_contents($output),true);fclose($output);$exit=proc_close($p);check($exit===0&&in_array($r['status']??0,[200,409],true),'Concurrent manual transition and observer return controlled results');$mixed[]=$r['status'];}
sort($mixed);$mixedState=$readObservation(104);
check($mixed===[200,200,200,409]&&count($mixedState[1])===$beforeMixed+2&&$mixedState[0]['situacion']==='DESVINCULADO'&&(int)$mixedState[0]['ultima_agencia_vista']===2,'Mixed race preserves one agency observation and one explicit termination without duplicates');
// Exercise the optional all-workers CLI with the same SELECT-only runtime user.
$sourceStable=$admin->query("SELECT * FROM `$source`.trabajadores ORDER BY id_trabajador")->fetchAll();
for($i=0;$i<2;$i++){
    $pipes=[];$p=proc_open([PHP_BINARY,dirname(__DIR__).'/bin/sync_employees.php',$path],[1=>['pipe','w'],2=>['pipe','w']],$pipes);
    $output=stream_get_contents($pipes[1]);$error=stream_get_contents($pipes[2]);fclose($pipes[1]);fclose($pipes[2]);$exit=proc_close($p);$summary=json_decode($output,true);
    check($exit===0&&is_array($summary)&&$summary['trabajadores_observados']>=8,'Optional CLI processes all workers using only authorized local runtime grants');
    if($i===1)check($summary['eventos']===0&&$summary['sesiones_revocadas']===0,'Repeated CLI scan produces zero events and zero revocations');
}
check($admin->query("SELECT * FROM `$source`.trabajadores ORDER BY id_trabajador")->fetchAll()===$sourceStable,'All observation entry points preserve attendance source byte for byte');
check($db->query('SELECT * FROM wp_kpi_evaluaciones ORDER BY id_evaluacion')->fetchAll()===$kpiBefore&&$db->query('SELECT * FROM wp_kpi_revisiones ORDER BY id_revision')->fetchAll()===$revisionsBefore,'Observer leaves KPI evaluations and calculations untouched');
