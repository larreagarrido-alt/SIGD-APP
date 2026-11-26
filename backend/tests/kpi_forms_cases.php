<?php
declare(strict_types=1);
// Included by integration.php only after it verifies an isolated synthetic server.
$formTypes = ['VENTAS','DISENADOR','DISENADOR_JUNIOR','DISENADOR_SENIOR','GERENCIA',
    'PROGRAMACION','ADMINISTRACION','ASISTENCIA_ADMIN','CONTABILIDAD','COMMUNITY',
    'FOTOGRAFIA','TRAFFICKER','VIDEO'];
$sample = ['tipo_formulario'=>'VENTAS','registro_uid'=>'fixture-preflight','revision'=>1,
    'payload'=>['fecha'=>'2026-11-12','observaciones'=>'synthetic','actividades'=>[['cliente'=>'fixture','cantidad'=>2]]]];
$firstAllowed=api('/kpi/formularios/permitidos',null,$token);
check(($firstAllowed['data']['tipos_formulario']??null)===[],
    'worker sees no form before the cargo mapping is approved');
check(api('/kpi/formularios/sync',['records'=>[$sample]],$token)['code']===422,
    'KPI form refuses a cargo mapping that has not been approved');
$proposed = [1=>'GERENCIA',3=>'DISENADOR',4=>'PROGRAMACION',5=>'VENTAS',
    6=>'PROGRAMACION',7=>'DISENADOR_JUNIOR',13=>'COMMUNITY',14=>'DISENADOR_SENIOR',
    15=>'COMMUNITY',16=>'COMMUNITY',17=>'VIDEO'];
$cargoNames=[1=>'Gerente General',2=>'Jefe de Agencia',3=>'Creativa/Diseño',
    4=>'Programador Web',5=>'Ejecutivo Comercial',6=>'Programador Junior',
    7=>'Diseñador Junior',13=>'Community Manager',14=>'Diseñador Senior',
    15=>'Community Manager Junior',16=>'Community Manager Senior',17=>'Creativo / Videógrafo'];
$stageMap = $db->prepare('INSERT INTO wp_kpi_formulario_cargos (id_cargo,tipo_formulario,aprobado_por,aprobado_en) VALUES (?,?,NULL,NOW())');
foreach ($proposed as $cargo => $type) $stageMap->execute([$cargo,$type]);
$switchCargo=$admin->prepare("UPDATE `$source`.trabajadores SET id_cargo=? WHERE id_trabajador=1");
foreach ($proposed as $cargo => $type) {
    $switchCargo->execute([$cargo]);
    check((api('/auth/me',null,$token)['data']['worker']['cargo_nombre']??null)===$cargoNames[$cargo],
        'auth/me preserves exact attendance cargo name for ID '.$cargo);
    check((api('/auth/me',null,$token)['data']['worker']['id_cargo']??null)===$cargo,
        'auth/me preserves exact attendance cargo ID '.$cargo);
    check((api('/kpi/formularios/permitidos',null,$token)['data']['tipos_formulario']??null)===[$type],
        'staging proposal resolves real cargo ID '.$cargo.' to '.$type);
}
$switchCargo->execute([2]);
check((api('/auth/me',null,$token)['data']['worker']['cargo_nombre']??null)===$cargoNames[2],
    'auth/me preserves exact Jefe de Agencia name');
check((api('/kpi/formularios/permitidos',null,$token)['data']['tipos_formulario']??null)===[],
    'Jefe de Agencia cargo 2 has no automatic KPI form');
$switchCargo->execute([1]);
$db->exec("DELETE FROM wp_rol_permisos WHERE id_rol=5 AND id_permiso=(SELECT id_permiso FROM wp_permisos WHERE codigo='kpi.entregar')");
check(api('/kpi/formularios/permitidos',null,$token)['code']===403,
    'cargo mapping alone does not bypass effective KPI permissions');
$db->exec("INSERT INTO wp_rol_permisos SELECT 5,id_permiso,'PROPIO' FROM wp_permisos WHERE codigo='kpi.entregar'");
check((api('/kpi/formularios/permitidos',null,$token)['data']['tipos_formulario']??null)===['GERENCIA'],
    'restored effective permission exposes only approved cargo mapping');
$mapping = $db->prepare('INSERT IGNORE INTO wp_kpi_formulario_cargos (id_cargo,tipo_formulario,aprobado_por,aprobado_en) VALUES (1,?,2,NOW())');
foreach ($formTypes as $type) $mapping->execute([$type]);
$sortedTypes=$formTypes; sort($sortedTypes);
check((api('/kpi/formularios/permitidos',null,$token)['data']['tipos_formulario']??null)===$sortedTypes,
    'authorized worker reads only forms approved for current cargo');
foreach ($formTypes as $i => $type) {
    $record = ['tipo_formulario'=>$type,'registro_uid'=>'fixture-form-'.$i,'revision'=>1,
        'payload'=>['fecha'=>'2026-11-12','campo'=>'synthetic-'.$type,
            'detalles'=>[['actividad'=>'fixture','observacion'=>'retained','conteo'=>$i]]]];
    $saved = api('/kpi/formularios/sync',['records'=>[$record]],$token);
    $read = api('/kpi/formularios?tipo_formulario='.$type,null,$token);
    check($saved['code']===200 && ($saved['data']['id_trabajador']??null)===1
        && $read['code']===200 && ($read['data']['id_trabajador']??null)===1
        && ($read['data']['registros'][0]['payload']??null)===$record['payload'],
        'KPI full form '.$type.' persists and reads all nested fields for its worker');
}
check((api('/kpi/formularios',null,$token)['data']['total']??null)===13,
    'KPI dashboard source counts only the 13 reports actually stored');
check((int)$db->query("SELECT COUNT(*) FROM wp_notificaciones WHERE id_trabajador=2 AND referencia_tipo='kpi_formulario'")->fetchColumn()===1,
    'administrator receives a notice for a completed daily KPI, not unfinished weekly drafts');
$currentWeekReport=['tipo_formulario'=>'GERENCIA','registro_uid'=>'fixture-current-week-dashboard',
    'revision'=>1,'payload'=>['days'=>[['date'=>date('Y-m-d'),'isRegistered'=>true]]]];
check(api('/kpi/formularios/sync',['records'=>[$currentWeekReport]],$token)['code']===200,
    'current-week completed report reaches the server');
$currentWeekAllowed=api('/kpi/formularios/permitidos',null,$token);
check(in_array('GERENCIA',$currentWeekAllowed['data']['tipos_con_entrega_semana']??[],true),
    'dashboard can distinguish a delivered KPI this week from an older report');
check(is_int($currentWeekAllowed['data']['turnos_kpi_pendientes']??null)
    && array_key_exists('proxima_entrega_kpi',$currentWeekAllowed['data']??[]),
    'dashboard receives the labor-calendar pending count and next due date');
$currentWeekId=(int)$db->query("SELECT id_detalle FROM wp_kpi_formularios_detalle WHERE registro_uid='fixture-current-week-dashboard'")->fetchColumn();
$db->exec("DELETE FROM wp_notificaciones WHERE referencia_tipo='kpi_formulario' AND referencia_id=".$currentWeekId);
$db->exec("DELETE FROM wp_kpi_formularios_detalle WHERE id_detalle=".$currentWeekId);
$page1=api('/kpi/formularios?limit=5&after_id=0',null,$token);
$cursor=(int)($page1['data']['next_after_id']??0);
$page2=api('/kpi/formularios?limit=5&after_id='.$cursor,null,$token);
check($page1['code']===200 && count($page1['data']['registros']??[])===5
    && ($page1['data']['has_more']??false)===true && $cursor>0
    && $page2['code']===200 && count($page2['data']['registros']??[])===5
    && (int)($page2['data']['registros'][0]['id_detalle']??0)>$cursor,
    'KPI history pages all reports without truncating after 100');
$one=api('/kpi/formularios?tipo_formulario=VENTAS&registro_uid=fixture-form-0',null,$token);
check($one['code']===200 && ($one['data']['total']??null)===1
    && ($one['data']['registros'][0]['registro_uid']??null)==='fixture-form-0',
    'KPI outbox can confirm a specific report beyond the first history page');
check(api('/kpi/formularios?limit=101',null,$token)['code']===422,
    'KPI history rejects invalid pagination');
check(api('/kpi/formularios?id_trabajador=2',null,$token)['code']===403,
    'KPI form reader cannot inspect another worker');
$unknown=$sample;$unknown['tipo_formulario']='UNAPPROVED';
check(api('/kpi/formularios/sync',['records'=>[$unknown]],$token)['code']===422,
    'KPI form rejects a client invented form type');
check(api('/kpi/formularios/sync',['records'=>[$sample]],$token)['code']===200,
    'KPI form accepted after the synthetic cargo mapping was approved');
check(api('/kpi/formularios/sync',['records'=>[$sample]],$token)['code']===200,
    'KPI form same revision and payload is idempotent');
$noticeCount=(int)$db->query("SELECT COUNT(*) FROM wp_notificaciones WHERE id_trabajador=2 AND referencia_tipo='kpi_formulario'")->fetchColumn();
check(api('/kpi/formularios/sync',['records'=>[$sample]],$token)['code']===200
    && (int)$db->query("SELECT COUNT(*) FROM wp_notificaciones WHERE id_trabajador=2 AND referencia_tipo='kpi_formulario'")->fetchColumn()===$noticeCount,
    'idempotent KPI retry does not duplicate administrator notice');
$conflict=$sample;$conflict['payload']['observaciones']='different';
check(api('/kpi/formularios/sync',['records'=>[$conflict]],$token)['code']===409,
    'KPI form same revision cannot silently overwrite content');
$update=$sample;$update['revision']=2;$update['payload']['observaciones']='updated';
check(api('/kpi/formularios/sync',['records'=>[$update]],$token)['code']===200,
    'KPI form higher revision accepted');
$latest = api('/kpi/formularios?tipo_formulario=VENTAS',null,$token);
$updatedRow = null;
foreach (($latest['data']['registros'] ?? []) as $row) if (($row['registro_uid'] ?? null)==='fixture-preflight') $updatedRow=$row;
check(($updatedRow['payload']['observaciones']??null)==='updated' && ($updatedRow['revision']??null)===2,
    'KPI form higher revision survives server read');
check(api('/kpi/formularios/sync',['records'=>[$sample]],$token)['code']===409,
    'KPI form rejects stale offline revision');
$switchCargo->execute([5]);
$fromOldCargo=$update;
$fromOldCargo['revision']=3;
check(api('/kpi/formularios/sync',['records'=>[$fromOldCargo]],$token)['code']===409,
    'promotion never rewrites a report under its former cargo');
$newCargoReport=$update;
$newCargoReport['registro_uid']='fixture-after-cargo-change';
$newCargoReport['revision']=1;
check(api('/kpi/formularios/sync',['records'=>[$newCargoReport]],$token)['code']===200,
    'new cargo can save a separate report with the same form type');
$mixedCargo=api('/kpi/formularios?tipo_formulario=VENTAS',null,$token);
$cargoByUid=[];
foreach (($mixedCargo['data']['registros']??[]) as $row) $cargoByUid[$row['registro_uid']]=(int)($row['id_cargo']??0);
check(($cargoByUid['fixture-preflight']??0)===1 && ($cargoByUid['fixture-after-cargo-change']??0)===5,
    'report history retains both source cargo IDs after a change');
$db->exec('DELETE FROM wp_kpi_formularios_detalle WHERE registro_uid="fixture-after-cargo-change"');
$switchCargo->execute([1]);
check(api('/kpi/formularios',null,null)['code']===401
    && api('/kpi/formularios/sync',['records'=>[$sample]],null)['code']===401,
    'KPI form read and write require authenticated session');
$draft=['borrador_uid'=>'fixture-draft-1','revision'=>1,
    'payload'=>['id'=>'fixture-draft-1','name'=>'Synthetic draft','targetValue'=>5,'weight'=>20,
        'activities'=>[['observation'=>'synthetic nested value']]]];
$draftAdmin=api('/auth/login',['username'=>'fixture-admin','password'=>'Synthetic-only-123!']);
$draftToken=$draftAdmin['data']['auth']['access_token']??null;
check(in_array('dashboard.admin',api('/auth/me',null,$draftToken)['data']['permissions']??[],true),
    'synthetic administrator receives dashboard grant from existing role permission table');
$db->exec("DELETE FROM wp_rol_permisos WHERE id_rol=1 AND id_permiso=(SELECT id_permiso FROM wp_permisos WHERE codigo='dashboard.admin')");
check(!in_array('dashboard.admin',api('/auth/me',null,$draftToken)['data']['permissions']??[],true)
    && api('/dashboard/resumen',null,$draftToken)['code']===403,
    'missing administrator grant is visible in auth/me and blocks dashboard endpoint');
$db->exec("INSERT INTO wp_rol_permisos SELECT 1,id_permiso,'TODOS' FROM wp_permisos WHERE codigo='dashboard.admin'");
check(in_array('dashboard.admin',api('/auth/me',null,$draftToken)['data']['permissions']??[],true),
    'restoring only the authorized synthetic role grant restores dashboard routing signal');
check(api('/kpi/formularios?id_trabajador=1',null,$draftToken)['code']===200
    && api('/kpi/formularios?id_trabajador=2',null,$token)['code']===403,
    'admin sees authorized worker reports while a worker cannot cross account scope');
check(api('/kpi/configuracion/borradores',$draft,$token)['code']===403
    && api('/kpi/configuracion/borradores',null,$token)['code']===403,
    'KPI administrative drafts require kpi.administrar');
$draftSaved=api('/kpi/configuracion/borradores',$draft,$draftToken);
$draftRead=api('/kpi/configuracion/borradores',null,$draftToken);
check($draftSaved['code']===200 && $draftRead['code']===200
    && ($draftRead['data']['borradores'][0]['payload']??null)===$draft['payload'],
    'KPI private administrative draft persists and reads all nested fields');
check(api('/kpi/configuracion/borradores',$draft,$draftToken)['code']===200,
    'KPI draft retry is idempotent');
$draftConflict=$draft; $draftConflict['payload']['name']='Different';
check(api('/kpi/configuracion/borradores',$draftConflict,$draftToken)['code']===409,
    'KPI draft equal revision cannot overwrite different contents');
check(api('/kpi/configuracion/borradores',null,null)['code']===401,
    'KPI draft read requires session');
