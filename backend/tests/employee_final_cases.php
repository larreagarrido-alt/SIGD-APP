<?php
declare(strict_types=1);
if(!isset($admin,$db,$source,$app)||!preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source))throw new RuntimeException('Owned final employee lab required');
// A missing employee identity must not turn sentinel supervisor=0 into an authorized team.
$admin->exec("UPDATE `$source`.trabajadores SET id_supervisor=0 WHERE id_trabajador=7");
$db->prepare('INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,alcance,efecto,motivo) VALUES(8,?,"EQUIPO","PERMITIR","Synthetic no-worker team")')->execute([$permId]);
$noWorkerAuth=api('/auth/login',['username'=>'employee-no-worker','password'=>'Synthetic-only-123!'])['data']['auth'];
try{
    $emptyTeam=api('/personal',null,$noWorkerAuth['access_token']);
    check($emptyTeam['code']===200&&$emptyTeam['data']['empleados']===[],'EQUIPO without associated worker never exposes supervisor-zero employees');
    check(api('/personal/7',null,$noWorkerAuth['access_token'])['code']===403,'No-worker team list and detail have consistent authorization');
}finally{
    $db->prepare('DELETE FROM wp_usuario_permisos WHERE id_usuario=8 AND id_permiso=?')->execute([$permId]);
    $admin->exec("UPDATE `$source`.trabajadores SET id_supervisor=NULL WHERE id_trabajador=7");
}
$eventCount=(int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales')->fetchColumn();
foreach([
    ['tipo_evento'=>[],'motivo'=>'Synthetic'],
    ['tipo_evento'=>'DESVINCULACION','motivo'=>['unexpected'=>'array']],
    ['tipo_evento'=>'DESVINCULACION','motivo'=>123],
    ['tipo_evento'=>'DESVINCULACION','motivo'=>true],
    ['tipo_evento'=>'DESVINCULACION','motivo'=>'Synthetic','fecha_efectiva'=>[]],
    ['tipo_evento'=>'DESVINCULACION','motivo'=>'Synthetic','fecha_efectiva'=>null],
] as $invalid){
    $res=api('/personal/104/transicion',$invalid,$employeeAdmin);
    check($res['code']===422&&$res['error_code']==='VALIDATION_FAILED','Malformed JSON field types return safe validation error, never stored Array or coerced text');
}
check((int)$db->query('SELECT COUNT(*) FROM wp_eventos_laborales')->fetchColumn()===$eventCount,'Rejected transition types cause no observation or history side effects');
foreach(['tipo=Administrador','estado=Desvinculado','id_cargo=-1','id_cargo=1.5','limit=0','limit=101','offset=-1','offset=100001','q='.str_repeat('a',151),'q%5B%5D=unexpected'] as $query){
    $res=api('/personal?'.$query,null,$employeeAdmin);
    check($res['code']===422,'Invalid employee filter or excessive query is rejected before synchronization: '.strtok($query,'='));
}
$first=api('/personal?limit=1&offset=0',null,$employeeAdmin);
$second=api('/personal?limit=1&offset=1',null,$employeeAdmin);
$all=api('/personal?limit=100&offset=0',null,$employeeAdmin);
check($first['code']===200&&$first['data']['total']===1&&$first['data']['paginacion']===['limit'=>1,'offset'=>0,'hay_mas'=>true],'Bounded pagination preserves legacy total and employees fields and adds continuation metadata');
check($first['data']['empleados'][0]===$all['data']['empleados'][0]&&$second['data']['empleados'][0]===$all['data']['empleados'][1],'Stable ordering makes adjacent pages consistent even with multiple source accounts');
$end=api('/personal?offset=100000',null,$employeeAdmin);
check($end['data']['empleados']===[]&&$end['data']['paginacion']['hay_mas']===false,'Empty final page reports no further records');
$savedDni=$admin->query("SELECT dni FROM `$source`.trabajadores WHERE id_trabajador=7")->fetchColumn();
$admin->exec("UPDATE `$source`.trabajadores SET dni='11111111' WHERE id_trabajador=7");
try{
    $zero=api('/personal?q=0',null,$employeeAdmin);
    check($zero['code']===200&&!in_array(7,array_map('intval',array_column($zero['data']['empleados'],'id_trabajador')),true),'Search string zero is a real filter rather than an empty PHP value');
}finally{$admin->prepare("UPDATE `$source`.trabajadores SET dni=? WHERE id_trabajador=7")->execute([$savedDni]);}
// Existing source leave types cover approved justified absence; do not calculate unexplained absence.
$admin->exec("INSERT INTO `$source`.permisos_laborales(id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,motivo,estado,procesado_asistencia) VALUES(120,105,'Ausencia justificada',CURRENT_DATE(),CURRENT_DATE(),'Synthetic authorized absence','Aprobado',0)");
$absence=api('/personal/105',null,$employeeAdmin);
check($absence['code']===200&&$absence['data']['permisos_laborales'][0]['tipo_permiso']==='Ausencia justificada','Employee detail exposes source authorized absence without inventing an absence calculation');
check($absence['data']['empleado']['condicion_operativa']==='INACTIVO','Approved absence does not override actual source inactivity');
// Histories use deterministic descending identity when several events share a second/day.
$history=api('/personal/104',null,$employeeAdmin)['data']['eventos_transicion'];$ordered=true;
for($i=1;$i<count($history);$i++){
    if($history[$i-1]['fecha_efectiva']===$history[$i]['fecha_efectiva']&&$history[$i-1]['creado_en']===$history[$i]['creado_en']
        &&(int)$history[$i-1]['id_evento']<(int)$history[$i]['id_evento'])$ordered=false;
}
check($ordered,'History ordering is deterministic for events created during the same second');
