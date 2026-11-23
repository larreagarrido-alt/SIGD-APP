<?php
declare(strict_types=1);
if(PHP_SAPI!=='cli')exit(1);
date_default_timezone_set('America/Lima');
require dirname(__DIR__).'/src/Core/Autoloader.php';WebSitePlus\Core\Autoloader::register();
use WebSitePlus\Employees\LaborTransitionService as Labor;
use WebSitePlus\Core\HttpException;
$n=0;$today=date('Y-m-d');
foreach(['DESVINCULACION','REINGRESO','REACTIVACION'] as $event){Labor::validate($event,'Authorized reason',$today);$n++;}
Labor::validate('DESVINCULACION',str_repeat('ñ',500),$today);$n++;
foreach([
 ['DESVINCULACION','   ',$today],['DESVINCULACION',str_repeat('a',501),$today],['CONVERSION_TIPO','Reason',$today],
 ['INACTIVACION','Reason',$today],['','Reason',$today],['DESVINCULACION','Reason','2026-02-30'],
 ['DESVINCULACION','Reason','2026-13-01'],['DESVINCULACION','Reason',date('Y-m-d',strtotime('-1 day'))],
 ['DESVINCULACION','Reason',date('Y-m-d',strtotime('+1 day'))],['DESVINCULACION','Reason',$today.' 12:00:00'],
 ['DESVINCULACION','Reason','']
] as [$event,$reason,$date]){
    $blocked=false;try{Labor::validate($event,$reason,$date);}catch(HttpException $e){$blocked=$e->status===422&&$e->errorCode==='VALIDATION_FAILED';}
    if(!$blocked)throw new RuntimeException('Invalid transition accepted');$n++;
}
echo "RESULT: $n validation tests passed; no database access\n";
