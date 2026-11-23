<?php
declare(strict_types=1);
// Private owned-lab worker only; never a web endpoint or a production utility.
if(PHP_SAPI!=='cli')exit(1);
date_default_timezone_set('America/Lima');
require dirname(__DIR__).'/src/Core/Autoloader.php';WebSitePlus\Core\Autoloader::register();
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
try{
    $norm=static fn(string $s):string=>strtolower(str_replace('\\','/',rtrim($s,'/\\')));
    $config=realpath($argv[1]??'');$storage=realpath(dirname(__DIR__).'/storage');
    if(!$config||!$storage||!str_starts_with($norm($config),$norm($storage).'/tests-'))throw new RuntimeException('Owned config required');
    Config::load($config);
    if(!preg_match('/^audit[a-f0-9]{6}_websiteplus_test$/D',(string)Config::get('database.database'))
        ||Config::get('database.host')!=='127.0.0.1'||(int)Config::get('database.port')!==13317)throw new RuntimeException('Owned lab required');
    $db=Database::getConnection();$actual=(string)$db->query('SELECT @@datadir')->fetchColumn();$expected=(string)getenv('WSP_TEST_DATA_DIR');
    if(!$expected||$norm($actual)!==$norm($expected))throw new RuntimeException('Unexpected datadir');
    $worker=(int)($argv[2]??0);$actor=(int)($argv[3]??0);
    $st=$db->prepare('SELECT id_usuario,id_rol,id_trabajador FROM usuarios WHERE id_usuario=?');$st->execute([$actor]);$user=$st->fetch();
    if(!$user||!WebSitePlus\Auth\Authorization::allows($user,'empleados.administrar',$worker))throw new RuntimeException('Fixture actor unauthorized');
    if(($argv[4]??'transition')==='observe'){
        $result=WebSitePlus\Employees\LaborObservationService::observe($worker);
    }else{
        WebSitePlus\Employees\LaborTransitionService::apply($worker,'DESVINCULACION','Synthetic concurrency only',date('Y-m-d'),$actor);
    }
    echo json_encode(['status'=>200]);
}catch(WebSitePlus\Core\HttpException $e){echo json_encode(['status'=>$e->status]);}
catch(Throwable $e){echo json_encode(['status'=>500,'failure_class'=>get_class($e)]);exit(1);}
