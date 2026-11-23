<?php
declare(strict_types=1);
require dirname(__DIR__).'/src/Kpi/KpiFormCompletion.php';
require dirname(__DIR__).'/src/Kpi/KpiFormMeasurement.php';
require dirname(__DIR__).'/src/Kpi/KpiOfficialScore.php';
use WebSitePlus\Kpi\KpiOfficialScore as Score;

function checkScore(bool $ok, string $case): void {
    if (!$ok) throw new RuntimeException($case);
}
$days=[];
for ($i=0;$i<6;$i++) $days[]=['fecha'=>date('Y-m-d',strtotime('2026-11-23 +'.$i.' days')),
    'turnos_programados'=>1,'turnos_exigibles'=>1];
$week=['dias'=>$days,'turnos_programados'=>6,'turnos_exigibles'=>6];
$daily=['dias'=>[$days[0]],'turnos_programados'=>1,'turnos_exigibles'=>1];
$program=['fecha'=>'2026-11-23','turno'=>'Mañana','actividades'=>[
    ['tipo'=>'Desarrollo','estado'=>'Entregado'],['tipo'=>'Corrección','estado'=>'Entregado']]];
checkScore(Score::percentage('PROGRAMACION',$program,'2026-11-23',$daily)===100.0,
    'Programmer Monday uses one delivered functionality, not all tasks');
$junior=['date'=>'2026-11-23','shift'=>'Mañana','biometricVerified'=>true,
    'goal'=>'PRODUCCION_AUDIOVISUAL','activities'=>[
        ['type'=>'VIDEO','status'=>'ENTREGADO'],['type'=>'VIDEO','status'=>'PENDIENTE']]];
checkScore(Score::percentage('DISENADOR_JUNIOR',$junior,'2026-11-23',$daily)===50.0,
    'One of two official videos is 50 percent');
$community=[];
foreach ([11,14,13,12,9,6] as $i=>$count) $community[]=['date'=>$days[$i]['fecha'],'registered'=>true,
    'publicacionesCount'=>$count,'interaccionesCount'=>0,'otrasActividadesCount'=>0];
checkScore(Score::percentage('COMMUNITY',['days'=>$community],'2026-11-28',$week)===100.0,
    'Community six distinct daily targets sum to 65');
$partial=$week;
for ($i=2;$i<6;$i++) $partial['dias'][$i]['turnos_exigibles']=0;
$partial['turnos_exigibles']=2;
checkScore(Score::percentage('COMMUNITY',['days'=>array_slice($community,0,2)],'2026-11-28',$partial)===100.0,
    'Approved leave removes four days from the expected Community goal');
$sales=['days'=>[['fullDateFormatted'=>'Lunes, 23 de noviembre de 2026','saved'=>true,
    'llamadas'=>100,'visitas'=>60,'cotizaciones'=>30,'seguimiento'=>50,'prospectos'=>15,'clientes'=>3]]];
checkScore(Score::percentage('VENTAS',$sales,'2026-11-28',$week)===100.0,
    'Sales uses six weekly goals, not sum of unrelated units');
$weeklyCases=[
    'ADMINISTRACION'=>[[4,4,4,4,4,2],['gestionesCount','seguimientosCount','reunionesCount']],
    'ASISTENCIA_ADMIN'=>[[23,11,11,9,14,4],['gestionesCount','documentosCount','coordinacionesCount']],
    'CONTABILIDAD'=>[[45,17,24,12,8,12],['registrosCount','conciliacionesCount','comprobantesCount']],
    'FOTOGRAFIA'=>[[3,3,3,3,3,2],['sesionesCount','fotosCapturadasCount','fotosEditadasCount']],
    'TRAFFICKER'=>[[1,1,1,1,1,1],['campanasCount','ajustesCount','reportesCount']],
    'VIDEO'=>[[10,0,0,0,0,0],['videosGrabadosCount','videosEditadosCount','ajustesCorreccionesCount']],
    'DISENADOR'=>[[25,0,0,0,0,0],['designsCompleted','videosCompleted','photosCompleted']],
    'DISENADOR_SENIOR'=>[[24,0,0,0,0,0],['designsCount','videosCount','photoSessionsCount','otherActivitiesCount']],
];
foreach ($weeklyCases as $type=>[$goals,$fields]) {
    $rows=[];
    foreach ($goals as $i=>$goal) {
        $row=['date'=>$days[$i]['fecha']];
        $row[$type==='DISENADOR'?'biometricVerified':
            ($type==='DISENADOR_SENIOR'?'isRegistered':'registered')]=true;
        foreach ($fields as $field) $row[$field]=0;
        $row[$fields[0]]=$goal;
        $rows[]=$row;
    }
    checkScore(Score::percentage($type,['days'=>$rows],'2026-11-28',$week)===100.0,
        $type.' calculates its own form counts against its official weekly goal');
}
$manager=['days'=>array_map(static fn(array $d): array => ['date'=>$d['fecha'],
    'isRegistered'=>true,'trackingPerformed'=>'Activity'], $days)];
checkScore(Score::percentage('GERENCIA',$manager,'2026-11-28',$week)===100.0,
    'Manager own KPI remains measurable without joining ranking');
checkScore(Score::period('GERENCIA')==='SEMANAL' && Score::period('DISENADOR_JUNIOR')==='DIARIO',
    'Distinct periods preserved');
echo "Official KPI score cases passed\n";
