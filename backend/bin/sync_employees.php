<?php
declare(strict_types=1);
// Optional manual/scheduled CLI observer. Not an HTTP endpoint. Never writes attendance.
if(PHP_SAPI!=='cli'){http_response_code(404);exit;}
require dirname(__DIR__).'/src/Core/Autoloader.php';
\WebSitePlus\Core\Autoloader::register();
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Employees\LaborObservationService;
if(empty($argv[1])||!is_file($argv[1])){fwrite(STDERR,"Uso: php sync_employees.php /ruta/privada/config.php\n");exit(1);}
try{
    Config::load($argv[1]);date_default_timezone_set('America/Lima');
    $last=0;$total=0;$events=0;$revoked=0;
    do{
        $st=Database::getAttendanceConnection()->prepare('SELECT id_trabajador FROM trabajadores WHERE id_trabajador>? ORDER BY id_trabajador LIMIT 100');
        $st->execute([$last]);$ids=$st->fetchAll(PDO::FETCH_COLUMN);
        foreach($ids as $id){$result=LaborObservationService::observe((int)$id);$last=(int)$id;++$total;$events+=$result['eventos'];$revoked+=$result['sesiones_revocadas'];}
    }while(count($ids)===100);
    echo json_encode(['trabajadores_observados'=>$total,'eventos'=>$events,'sesiones_revocadas'=>$revoked]).PHP_EOL;
}catch(Throwable $e){fwrite(STDERR,"Sincronizacion incompleta. Cada trabajador confirmado conserva su transaccion; revisar conexiones y volver a ejecutar.\n");exit(1);}
