<?php
declare(strict_types=1);
require dirname(__DIR__).'/src/Kpi/KpiFormCompletion.php';
require dirname(__DIR__).'/src/Kpi/KpiFormMeasurement.php';
require dirname(__DIR__).'/src/Kpi/KpiPointsPolicy.php';
use WebSitePlus\Kpi\KpiFormMeasurement as Measure;
use WebSitePlus\Kpi\KpiPointsPolicy as Points;

function checkMeasure(bool $ok, string $why): void {
    if (!$ok) throw new RuntimeException($why);
}
$designer=['days'=>[['date'=>'2026-11-23','biometricVerified'=>true,
    'designsCompleted'=>0,'videosCompleted'=>2,'photosCompleted'=>0,'videosGoal'=>4],
    ['date'=>'2026-11-24','biometricVerified'=>false,
    'designsCompleted'=>0,'videosCompleted'=>50,'photosCompleted'=>0,'videosGoal'=>4]]];
$value=Measure::value('DISENADOR',$designer,'VIDEOS','2026-11-23','SEMANAL');
checkMeasure($value===2.0,'two recorded videos; exclude draft');
checkMeasure(round($value/4*100)===50.0,'two of four is 50 percent');
checkMeasure(Points::forPercentage(50)===12,'regular level awards 12 after approval');
checkMeasure(Measure::value('DISENADOR',$designer,'VIDEOS','2026-11-30','SEMANAL')===null,'wrong week');
checkMeasure(Measure::value('GERENCIA',['days'=>[['date'=>'2026-11-23','isRegistered'=>true,
    'ownCompliancePct'=>100,'priorityCount'=>1]]],'PORCENTAJE','2026-11-23','SEMANAL')===null,
    'self reported percentage cannot award points');
checkMeasure(Measure::value('VIDEO',['days'=>[['date'=>'2026-11-23','registered'=>true,
    'videosGrabadosCount'=>2,'videosEditadosCount'=>1,'ajustesCorreccionesCount'=>0,
    'dailyExpectedTotal'=>1]]],'ACTIVIDADES','2026-11-23','SEMANAL')===3.0,
    'video activity count ignores editable phone goal');
checkMeasure(Measure::value('VENTAS',['fullDateFormatted'=>'Lunes, 23 de noviembre de 2026','saved'=>true,
    'llamadas'=>2,'visitas'=>0,'cotizaciones'=>0,'seguimiento'=>0,'prospectos'=>0,'clientes'=>0],
    'LLAMADAS','2026-11-23','DIARIO')===2.0,'sales date and count');
checkMeasure(Measure::value('PROGRAMACION',
    ['fecha'=>'25 / 11 / 2026','turno'=>'Mañana','tareasCompletadas'=>2],
    'TAREAS','2026-11-23','SEMANAL')===2.0,
    'Android programmer date with spaces contributes to official KPI measurement');
$cases=[
    ['PROGRAMACION',['fecha'=>'2026-11-23','turno'=>'Mañana','tareasCompletadas'=>2,'cumplimientoMeta'=>100],'TAREAS'],
    ['DISENADOR_JUNIOR',['date'=>'2026-11-23','shift'=>'Mañana','biometricVerified'=>true,'deliveredPieces'=>2,'goalCompliancePercentage'=>100],'PIEZAS'],
    ['DISENADOR_SENIOR',['days'=>[['date'=>'2026-11-23','isRegistered'=>true,'videosCount'=>2,'teamCompliancePercentage'=>100]]],'VIDEOS'],
    ['ADMINISTRACION',['days'=>[['date'=>'2026-11-23','registered'=>true,'gestionesCount'=>2]]],'GESTIONES'],
    ['ASISTENCIA_ADMIN',['days'=>[['date'=>'2026-11-23','registered'=>true,'documentosCount'=>2]]],'DOCUMENTOS'],
    ['CONTABILIDAD',['days'=>[['date'=>'2026-11-23','registered'=>true,'registrosCount'=>2]]],'REGISTROS'],
    ['COMMUNITY',['days'=>[['date'=>'2026-11-23','registered'=>true,'publicacionesCount'=>2]]],'PUBLICACIONES'],
    ['FOTOGRAFIA',['days'=>[['date'=>'2026-11-23','registered'=>true,'sesionesCount'=>2]]],'SESIONES'],
    ['TRAFFICKER',['days'=>[['date'=>'2026-11-23','registered'=>true,'campanasCount'=>2]]],'CAMPANAS'],
    ['VIDEO',['days'=>[['date'=>'2026-11-23','registered'=>true,'videosEditadosCount'=>2]]],'VIDEOS_EDITADOS'],
    ['GERENCIA',['days'=>[['date'=>'2026-11-23','isRegistered'=>true,'priorityCount'=>2,'ownCompliancePct'=>100]]],'ASUNTOS'],
];
foreach ($cases as [$type,$payload,$unit])
    checkMeasure(Measure::value($type,$payload,$unit,'2026-11-23','SEMANAL')===2.0,$type.' count');
echo "KPI measurement cases passed\n";
