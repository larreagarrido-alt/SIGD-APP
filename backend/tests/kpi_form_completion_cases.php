<?php
declare(strict_types=1);
require dirname(__DIR__).'/src/Kpi/KpiFormCompletion.php';
use WebSitePlus\Kpi\KpiFormCompletion as Completion;

function checkCompletion(bool $ok, string $message): void {
    if (!$ok) throw new RuntimeException($message);
}
$calendar=['dias'=>[
    ['fecha'=>'2026-11-23','manana'=>true,'tarde'=>true],
    ['fecha'=>'2026-11-24','manana'=>true,'tarde'=>false,'permiso_aprobado'=>true],
    ['fecha'=>'2026-11-25','manana'=>false,'tarde'=>true],
]];
checkCompletion(Completion::pending($calendar,[],'2026-11-23')===2,'two required shifts');
checkCompletion(Completion::pending($calendar,['2026-11-23:MANANA'=>true],'2026-11-23')===1,'one shift completed');
checkCompletion(Completion::pending($calendar,['2026-11-23:DIA'=>true],'2026-11-25')===1,'daily report covers its day; leave excluded');
checkCompletion(Completion::units('DISENADOR_JUNIOR',['date'=>'23/11/2026','shift'=>'Mañana','biometricVerified'=>true])===['2026-11-23:MANANA'=>true],'junior verified shift');
checkCompletion(Completion::units('DISENADOR_JUNIOR',['date'=>'23/11/2026','shift'=>'Mañana','biometricVerified'=>false])===[],'junior draft excluded');
checkCompletion(Completion::units('COMMUNITY',['days'=>[['date'=>'2026-11-23','registered'=>true],['date'=>'2026-11-24','registered'=>false]]])===['2026-11-23:DIA'=>true],'weekly completed day');
checkCompletion(Completion::units('VENTAS',['fullDateFormatted'=>'Lunes, 23 de noviembre de 2026','saved'=>true])===['2026-11-23:DIA'=>true],'sales saved day');
checkCompletion(Completion::units('VENTAS',['fullDateFormatted'=>'2026-11-23','saved'=>false])===[],'sales draft excluded');
echo "KPI completion cases passed\n";
