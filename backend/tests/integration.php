<?php
declare(strict_types=1);
// Synthetic, isolated integration harness. Never opens the original attendance database.
if (PHP_SAPI !== 'cli') { exit(1); }
date_default_timezone_set('America/Lima');
require __DIR__.'/face_self_service_lab_helpers.php';
require __DIR__.'/test_support.php';
require dirname(__DIR__) . '/src/Core/Autoloader.php';
WebSitePlus\Core\Autoloader::register();
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
use WebSitePlus\Auth\TokenService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Auth\ChallengeService;
$testPort=(int)(getenv('WSP_TEST_PORT')?:13317);
$withoutV012=getenv('WSP_TEST_NO_V012')==='1';
if($testPort<1024 || $testPort>65535 || $testPort===3306) { throw new RuntimeException('Use a separate local laboratory port, not 3306'); }
$admin = new PDO('mysql:host=127.0.0.1;port='.$testPort.';charset=utf8mb4', getenv('WSP_TEST_ADMIN') ?: 'root', getenv('WSP_TEST_PASSWORD') ?: '', [PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
// Verify the actual server is the owned lab BEFORE creating any schema or account.
$storage=realpath(dirname(__DIR__).'/storage');
$expectedData=realpath(getenv('WSP_TEST_DATA_DIR')?:dirname(__DIR__).'/storage/mysql-integral-20261017');
$actualData=realpath((string)$admin->query('SELECT @@datadir')->fetchColumn());
$norm=static function(string $p):string{$p=rtrim(str_replace('\\','/',$p),'/');return PHP_OS_FAMILY==='Windows'?strtolower($p):$p;};
if(!$storage||!$expectedData||!$actualData||!str_starts_with($norm($expectedData),$norm($storage).'/')
    ||!str_starts_with(basename($expectedData),'mysql-')||$norm($actualData)!==$norm($expectedData)){
    throw new RuntimeException('Refusing non-laboratory server; no schema or account has been created');
}
$suffix = bin2hex(random_bytes(3));
$app = 'audit'.$suffix.'_websiteplus_test';
$source = 'audit'.$suffix.'_asistencia_test';
$runtime = 'wsp'.$suffix;
$password = bin2hex(random_bytes(24));
$admin->exec("CREATE DATABASE `$app` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
$admin->exec("CREATE DATABASE `$source` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
$admin->exec("USE `$source`");
$schema = file_get_contents(__DIR__.'/fixtures/attendance_schema.sql');
// Attendance source schema used by authentication, labor state and KPI calendar.
foreach (['agencias','cargos','roles','trabajadores','usuarios','permisos_laborales',
    'horario_practicantes','horario_vigencias','temporadas_horario','horarios_trabajadores','tipo_vigencias','horario_detalles',
    'consumos_extras','feriados','faltas','asistencias','justificaciones'] as $table) {
    preg_match('/CREATE TABLE `'. $table .'` \(.*?; /s', $schema.' ', $unused);
    if (!preg_match('/CREATE TABLE `'. $table .'` \(.*?;/s', $schema, $m)) { throw new RuntimeException('Missing fixture schema'); }
    $admin->exec($m[0]);
}
$admin->exec("INSERT INTO roles (id_rol,nombre) VALUES (5,'Colaborador'),(1,'Administrador'),(6,'Practicante')");
$admin->exec("INSERT INTO agencias (id_agencia,nombre) VALUES (1,'Synthetic agency')");
$admin->exec("INSERT INTO cargos (id_cargo,nombre,sueldo_base) VALUES (1,'Gerente General',0)");
$admin->exec("INSERT INTO cargos (id_cargo,nombre,sueldo_base) VALUES
    (2,'Jefe de Agencia',0),(3,'Creativa/Diseño',0),(4,'Programador Web',0),
    (5,'Ejecutivo Comercial',0),(6,'Programador Junior',0),(7,'Diseñador Junior',0),
    (13,'Community Manager',0),(14,'Diseñador Senior',0),
    (15,'Community Manager Junior',0),(16,'Community Manager Senior',0),
    (17,'Creativo / Videógrafo',0)");
$admin->exec("INSERT INTO trabajadores (id_trabajador,dni,nombres,apellidos,fecha_nacimiento,id_cargo,id_agencia,tipo,fecha_inicio,estado) VALUES (1,'00000001','Fixture','One','2000-01-01',1,1,'Practicante','2026-01-01','Activo'),(2,'00000002','Fixture','Two','2000-01-01',1,1,'Colaborador','2026-01-01','Activo')");
$admin->exec("INSERT INTO tipo_vigencias (id_trabajador,vigente_desde,tipo,id_cargo,origen) VALUES (1,'2026-01-01','Practicante',1,'BASELINE'),(2,'2026-01-01','Colaborador',1,'BASELINE')");
$schedule=$admin->prepare('INSERT INTO horario_practicantes (id_trabajador,dia,asiste,asiste_manana,asiste_tarde,hora_inicio,hora_fin,hora_inicio_tarde,hora_fin_tarde) VALUES (?,?,1,1,1,"08:00","13:30","15:30","18:30")');
foreach ([1,2] as $worker) foreach (['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'] as $day) $schedule->execute([$worker,$day]);
$hash = password_hash('Synthetic-only-123!', PASSWORD_DEFAULT);
$st=$admin->prepare('INSERT INTO usuarios (id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password) VALUES (1,"fixture",?,5,1,0),(2,"fixture-admin",?,1,2,0)');
$st->execute([$hash,$hash]);
$admin->exec("CREATE USER '$runtime'@'127.0.0.1' IDENTIFIED BY '$password'");
$admin->exec("GRANT SELECT ON `$source`.* TO '$runtime'@'127.0.0.1'");
$admin->exec("GRANT SELECT, INSERT, UPDATE, DELETE ON `$app`.* TO '$runtime'@'127.0.0.1'");
$cfg = require dirname(__DIR__).'/config/config.example.php';
$cfg['app']['env'] = 'testing';
// Explicit opt-in for a physical synthetic laboratory run only. Default integration starts OFF.
$cfg['security']['face_local_pad_login_enabled'] = false;
$cfg['database'] = ['host'=>'127.0.0.1','port'=>$testPort,'database'=>$app,'username'=>getenv('WSP_TEST_ADMIN') ?: 'root','password'=>getenv('WSP_TEST_PASSWORD') ?: ''];
$cfg['attendance'] = ['host'=>'127.0.0.1','port'=>$testPort,'database'=>$source,'username'=>$runtime,'password'=>$password];
$cfg['security']['jwt_secret'] = bin2hex(random_bytes(32));
$dir = dirname(__DIR__).'/storage/tests-'.$suffix;
mkdir($dir,0700,true);
$path = $dir.'/config.php';
file_put_contents($path, '<?php return '.var_export($cfg,true).';');
function migrate(string $path): string {
    $p=proc_open([PHP_BINARY,dirname(__DIR__).'/bin/migrate.php',$path],[1=>['pipe','w'],2=>['pipe','w']],$pipes);
    $out=stream_get_contents($pipes[1]); $err=stream_get_contents($pipes[2]);
    fclose($pipes[1]); fclose($pipes[2]); $code=proc_close($p);
    if ($code !== 0) { throw new RuntimeException($out.$err); }
    return $out;
}
$passed=0;
function check(bool $ok,string $name): void { global $passed; if (!$ok) { throw new RuntimeException('FAIL '.$name); } ++$passed; echo "PASS $name\n"; }
echo "Isolated schemas: $app / $source\n";
// cPanel PHP-FPM: getallheaders() omits Authorization; .htaccess exposes it as REDIRECT_HTTP_AUTHORIZATION.
$fakeBearer = str_repeat('ab', 32);
$_SERVER['REDIRECT_HTTP_AUTHORIZATION'] = 'Bearer '.$fakeBearer;
check((new WebSitePlus\Core\Request())->getBearerToken() === $fakeBearer, 'Authorization read from REDIRECT_HTTP_AUTHORIZATION (cPanel FPM)');
// Caso exacto reportado: HTTP_AUTHORIZATION vacio no debe impedir leer REDIRECT_HTTP_AUTHORIZATION
$_SERVER['HTTP_AUTHORIZATION'] = '';
check((new WebSitePlus\Core\Request())->getBearerToken() === $fakeBearer, 'Empty HTTP_AUTHORIZATION does not mask valid REDIRECT_HTTP_AUTHORIZATION');
unset($_SERVER['REDIRECT_HTTP_AUTHORIZATION']);
$_SERVER['HTTP_AUTHORIZATION'] = 'Bearer '.$fakeBearer;
check((new WebSitePlus\Core\Request())->getBearerToken() === $fakeBearer, 'Authorization read from HTTP_AUTHORIZATION');
unset($_SERVER['HTTP_AUTHORIZATION']);
check((new WebSitePlus\Core\Request())->getBearerToken() === null, 'missing Authorization stays rejected');
echo migrate($path);
check(substr_count(migrate($path),'unchanged') === count(glob(dirname(__DIR__,2).'/database/migrations/*.up.sql'))-($withoutV012?1:0), 'migration rerun checksums');
$exportTarget='';
if (!$withoutV012) {
$exportTarget='export'.$suffix.'_websiteplus_test';
$bundle=$dir.'/installation.sql';
$process=proc_open([PHP_BINARY,dirname(__DIR__).'/bin/export_sql.php',$exportTarget,$source,$bundle],[1=>['pipe','w'],2=>['pipe','w']],$exportPipes);
$output=stream_get_contents($exportPipes[1]).stream_get_contents($exportPipes[2]);
fclose($exportPipes[1]);fclose($exportPipes[2]);check(proc_close($process)===0,'offline SQL export');
$admin->exec("CREATE DATABASE `$exportTarget` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
foreach(WebSitePlus\Core\SqlScript::statements(file_get_contents($bundle)) as $statement) { $admin->exec($statement); }
check((int)$admin->query('SELECT COUNT(*) FROM wp_migraciones WHERE checksum IS NOT NULL')->fetchColumn()===count(glob(dirname(__DIR__,2).'/database/migrations/*.up.sql')),'exported SQL installs with checksums');
}
$admin->exec("USE `$source`");
$cfg['database']['username']=$runtime; $cfg['database']['password']=$password;
file_put_contents($path,'<?php return '.var_export($cfg,true).';');
Config::load($path); $db=Database::getConnection();
check((int)$db->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'")->fetchColumn() >= ($withoutV012?51:52)
    && (int)$db->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('wp_bienestar_campanas','wp_bienestar_planes','wp_kpi_plantillas_estado','wp_kpi_plantillas_alertas')")->fetchColumn()===4,
    'expected isolated application tables including wellness and shared KPI templates');
check(str_contains((string)$db->query("SELECT COLUMN_TYPE FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wp_bienestar_tests' AND column_name='periodicidad'")->fetchColumn(),'SEMANAL'),'V020 weekly period exists with or without V012');
$officialSeed=file_get_contents(dirname(__DIR__,2).'/database/seeds/30_APLICAR_METAS_KPI_OFICIALES_20261122.sql');
foreach (WebSitePlus\Core\SqlScript::statements($officialSeed) as $statement) $db->exec($statement);
check((int)$db->query("SELECT COUNT(*) FROM wp_kpi_definiciones WHERE codigo LIKE 'SIGD-OFICIAL-CARGO-%' AND tipo_metrica='PORCENTAJE' AND meta_esperada=100 AND regla_aprobada_en IS NOT NULL")->fetchColumn()===11,
    'official seed loads all eleven mapped cargos with approved automatic percentages');
foreach (WebSitePlus\Core\SqlScript::statements($officialSeed) as $statement) $db->exec($statement);
check((int)$db->query("SELECT COUNT(*) FROM wp_kpi_definiciones WHERE codigo LIKE 'SIGD-OFICIAL-CARGO-%'")->fetchColumn()===11,
    'official seed can be retried without duplicate rules');
$assignmentSeed=str_replace('wsnet_bd_asistencia',$source,
    file_get_contents(dirname(__DIR__,2).'/database/seeds/32_ASIGNAR_KPI_ACTIVOS_20261122.sql'));
$db->exec("INSERT INTO wp_kpi_formulario_cargos (id_cargo,tipo_formulario,aprobado_en)
    VALUES (1,'GERENCIA',NOW())");
foreach (WebSitePlus\Core\SqlScript::statements($assignmentSeed) as $statement)
    if (str_starts_with(ltrim($statement),'INSERT IGNORE')) $db->exec($statement);
check((int)$db->query('SELECT COUNT(*) FROM wp_kpi_asignaciones WHERE activo=1')->fetchColumn()>=1,
    'approved cargo metas are assigned to existing active workers without manual IDs');
foreach (WebSitePlus\Core\SqlScript::statements($assignmentSeed) as $statement)
    if (str_starts_with(ltrim($statement),'INSERT IGNORE')) $db->exec($statement);
$db->exec('DELETE FROM wp_kpi_asignaciones');
$db->exec("DELETE FROM wp_kpi_formulario_cargos WHERE id_cargo=1 AND tipo_formulario='GERENCIA'");
$db->exec("DELETE FROM wp_kpi_definiciones WHERE codigo LIKE 'SIGD-OFICIAL-CARGO-%'");
$db->exec("DELETE FROM wp_migraciones WHERE version='V030'");
check((int)$admin->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$source' AND table_name LIKE 'wp\\_%'")->fetchColumn()===0,'no application table in source');
try { $db->exec("UPDATE usuarios SET username='forbidden' WHERE id_usuario=1"); check(false,'source writes denied'); } catch (PDOException $e) { check((int)$e->errorInfo[1]===1142 || (int)$e->errorInfo[1]===1356,'source writes denied by database grants'); }
$db->exec("INSERT INTO wp_rol_permisos SELECT 5,id_permiso,'PROPIO' FROM wp_permisos WHERE codigo IN ('auth.perfil','kpi.ver','kpi.entregar','actividad.ver','actividad.crear','premios.ver','premios.canjear','ranking.ver')");
$db->exec("INSERT INTO wp_rol_permisos SELECT 1,id_permiso,'TODOS' FROM wp_permisos WHERE codigo NOT IN ('bienestar.ver_privado','bienestar.responder')");
$grantSql=str_replace(['wsnet_websiteplus','wsnet_bd_asistencia'],[$app,$source],file_get_contents(dirname(__DIR__,2).'/database/proposals/bienestar_responder_roles_1_5_6.sql'));
foreach(WebSitePlus\Core\SqlScript::statements($grantSql) as $grantStatement) {
    if(str_starts_with($grantStatement,'INSERT INTO')) $db->exec($grantStatement);
}
check((int)$db->query("SELECT COUNT(*) FROM wp_rol_permisos rp JOIN wp_permisos p ON p.id_permiso=rp.id_permiso WHERE p.codigo='bienestar.responder' AND rp.id_rol IN (1,5,6) AND rp.alcance='PROPIO'")->fetchColumn()===3,'reviewed wellness grant gives own scope to existing roles 1, 5 and 6');
check((int)$db->query("SELECT COUNT(*) FROM wp_rol_permisos rp JOIN wp_permisos p ON p.id_permiso=rp.id_permiso WHERE p.codigo='bienestar.ver_privado'")->fetchColumn()===0,'wellness grant never exposes private answers');
foreach(WebSitePlus\Core\SqlScript::statements($grantSql) as $grantStatement) {
    if(str_starts_with($grantStatement,'INSERT INTO')) check($db->exec($grantStatement)===0,'wellness role grant is idempotent');
}
$tokens=TokenService::createSession(1,null,'PASSWORD','127.0.0.1',$hash);
$user=TokenService::validateAccessToken($tokens['access_token']);
check($user!==null,'access token resolves existing identity');
check(($user['cargo_nombre'] ?? null)==='Gerente General','authenticated session preserves exact labor role name from existing view');
check(!Authorization::allows($user,'dashboard.admin'),'worker cannot access administration');
check(!Authorization::allows($user,'kpi.ver',2),'worker cannot access another worker');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=1");
check(TokenService::validateAccessToken($tokens['access_token'])===null,'inactive access revoked');
check(TokenService::rotateRefreshToken($tokens['refresh_token'],'127.0.0.1')===null,'inactive refresh denied');
$admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=1");
check(TokenService::validateAccessToken($tokens['access_token'])===null,'reactivation does not resurrect revoked token');
$tokens=TokenService::createSession(1,null,'PASSWORD','127.0.0.1',$hash);
check(TokenService::validateAccessToken($tokens['access_token'])['id_trabajador']===1,'reactivation preserves identity');
$admin->exec("UPDATE `$source`.trabajadores SET tipo='Colaborador' WHERE id_trabajador=1");
check(TokenService::validateAccessToken($tokens['access_token'])['tipo_trabajador']==='Colaborador','conversion reflected without duplicate identity');
$admin->exec("UPDATE `$source`.trabajadores SET tipo='Practicante' WHERE id_trabajador=1");
$rotated=TokenService::rotateRefreshToken($tokens['refresh_token'],'127.0.0.1');
check($rotated!==null && TokenService::validateAccessToken($tokens['access_token'])===null,'refresh rotation invalidates old access');
check(TokenService::rotateRefreshToken($tokens['refresh_token'],'127.0.0.1')===null && TokenService::validateAccessToken($rotated['access_token'])===null,'refresh replay revokes session');
$tokens=TokenService::createSession(1,null,'PASSWORD','127.0.0.1',$hash);
$admin->exec("UPDATE `$source`.usuarios SET password='changed' WHERE id_usuario=1");
check(TokenService::rotateRefreshToken($tokens['refresh_token'],'127.0.0.1')===null,'password change prevents refresh');
$st=$admin->prepare("UPDATE `$source`.usuarios SET password=? WHERE id_usuario=1"); $st->execute([$hash]);
// HTTP checks execute the actual front controller and controllers.
$port=19000+random_int(0,5000); putenv('WSP_CONFIG_PATH='.$path);
$server=proc_open([PHP_BINARY,'-S','127.0.0.1:'.$port,'-t',dirname(__DIR__).'/public',dirname(__DIR__).'/public/index.php'],[0=>['pipe','r'],1=>['file',$dir.'/http.log','a'],2=>['file',$dir.'/http.log','a']],$pipes);
function api(string $route,?array $body=null,?string $token=null): array {
 global $port;
 $headers="Content-Type: application/json\r\nX-Device-Uid: synthetic-device\r\n".($token ? "Authorization: Bearer $token\r\n" : '');
 $ctx=stream_context_create(['http'=>['method'=>$body===null?'GET':'POST','header'=>$headers,'content'=>$body===null?'':json_encode((object)$body),'ignore_errors'=>true,'timeout'=>5]]);
 $raw=file_get_contents("http://127.0.0.1:$port/api/v1$route",false,$ctx);
 $value=json_decode($raw,true,512,JSON_THROW_ON_ERROR); return $value;
}
function apiMethod(string $method,string $route,?array $body=null,?string $token=null): array {
 global $port;
 $headers="Content-Type: application/json\r\n".($token ? "Authorization: Bearer $token\r\n" : '');
 $ctx=stream_context_create(['http'=>['method'=>$method,'header'=>$headers,'content'=>$body===null?'':json_encode((object)$body),'ignore_errors'=>true,'timeout'=>5]]);
 return json_decode(file_get_contents("http://127.0.0.1:$port/api/v1$route",false,$ctx),true,512,JSON_THROW_ON_ERROR);
}
function apiUpload(string $route,string $bytes,string $mime,?string $token): array {
 global $port;
 $boundary='TEST'.bin2hex(random_bytes(8));
 $payload="--$boundary\r\nContent-Disposition: form-data; name=\"imagen\"; filename=\"synthetic.png\"\r\nContent-Type: $mime\r\n\r\n".$bytes."\r\n--$boundary--\r\n";
 $headers="Content-Type: multipart/form-data; boundary=$boundary\r\n".($token ? "Authorization: Bearer $token\r\n" : '');
 $ctx=stream_context_create(['http'=>['method'=>'POST','header'=>$headers,'content'=>$payload,'ignore_errors'=>true,'timeout'=>5]]);
 return json_decode(file_get_contents("http://127.0.0.1:$port/api/v1$route",false,$ctx),true,512,JSON_THROW_ON_ERROR);
}
try {
 for($i=0;$i<50;$i++){ $socket=@fsockopen('127.0.0.1',$port,$errno,$errstr,.1); if($socket){fclose($socket);break;} usleep(100000); }
 check(api('/health')['code']===200,'health JSON');
 check(api('/auth/login',['username'=>'fixture','password'=>'wrong'])['code']===401,'HTTP incorrect password');
 $login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);
 check($login['code']===200,'HTTP shared credential login');
 $token=$login['data']['auth']['access_token'];
 $profile=api('/auth/me',null,$token);
check($profile['code']===200 && ($profile['data']['worker']['cargo_nombre'] ?? null)==='Gerente General',
       'HTTP existing account retains current cargo name');
 check(($profile['data']['worker']['id_cargo'] ?? null)===1
       && ($profile['data']['worker']['id_agencia'] ?? null)===1
       && ($profile['data']['worker']['id_trabajador'] ?? null)===1,
       'HTTP auth/me returns worker, cargo and agency IDs for Android navigation');
 check(in_array('kpi.ver',$profile['data']['permissions'] ?? [],true)
       && !in_array('dashboard.admin',$profile['data']['permissions'] ?? [],true),
       'HTTP effective permissions are resolved from the existing identity');
 require __DIR__.'/kpi_forms_cases.php';
 if ($withoutV012) {
     check((int)$db->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='wp_rostros_cuenta'")->fetchColumn()===0,
         'production-shaped laboratory has no wp_rostros_cuenta');
     $versions=$db->query("SELECT version FROM wp_migraciones WHERE version IN ('V012','V015','V016','V017','V018','V019') ORDER BY version")->fetchAll(PDO::FETCH_COLUMN);
     check($versions===['V015','V016','V017','V018','V019'],
         'production-shaped laboratory omits V012 and applies V015 through V019');
     $requiredColumns=$db->query("SELECT CONCAT(table_name,'.',column_name) FROM information_schema.columns WHERE table_schema=DATABASE() AND ((table_name='wp_accesos_android' AND column_name='huella_habilitada') OR (table_name='wp_claves_faciales' AND column_name='activo_acceso_android') OR (table_name='wp_kpi_registros_android' AND column_name='id_trabajador')) ORDER BY table_name,column_name")->fetchAll(PDO::FETCH_COLUMN);
     check(count($requiredColumns)===3,
         'V015 to V017 provide the expected KPI and biometric columns without V012');
     $methodType=(string)$db->query("SELECT COLUMN_TYPE FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='wp_sesiones' AND column_name='metodo'")->fetchColumn();
     check(str_contains($methodType,"'PASSWORD'") && str_contains($methodType,"'BIOMETRIA'") && str_contains($methodType,"'FACIAL'"),
         'production-shaped session enum retains all three authentication methods');
     $faceWithout=\WebSitePlus\Auth\FaceReferenceService::describe(1,1);
     check($faceWithout['migration_v012_required']===true && $faceWithout['reference_available']===false,
         'face without attendance descriptor is explicitly limited when V012 is absent');
     $syntheticFace=json_encode(array_fill(0,128,0.05),JSON_THROW_ON_ERROR);
     $setFace=$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor=? WHERE id_trabajador=1");
     $setFace->execute([$syntheticFace]);
     $faceWith=\WebSitePlus\Auth\FaceReferenceService::describe(1,1);
     check($faceWith['reference_source']==='ASISTENCIA' && $faceWith['reference_available']===true
         && $faceWith['migration_v012_required']===false,
         'valid attendance face reference remains available without V012');
     $admin->exec("UPDATE `$source`.trabajadores SET rostro_descriptor=NULL WHERE id_trabajador=1");
     check(api('/auth/face/enrollment/status',[],null)['code']===401,
         'facial enrollment remains protected by session without V012');
     echo "RESULT: $passed passed with V012 and wp_rostros_cuenta absent. Synthetic schemas retained; no production access.\n";
     return;
 }
 $db->exec("INSERT INTO wp_notificaciones (id_notificacion,id_trabajador,tipo,titulo,mensaje) VALUES (1001,1,'AVISO_GENERAL','Fixture','Own synthetic notice'),(1002,2,'AVISO_GENERAL','Fixture','Other synthetic notice')");
 check(api('/notificaciones',null,$token)['code']===200 && count(api('/notificaciones',null,$token)['data']['notificaciones'])===1,'HTTP notifications are scoped to signed-in worker');
 check(api('/notificaciones')['code']===401,'HTTP notifications require session');
 check(api('/notificaciones/1002/lectura',['leida'=>true],$token)['code']===404,'HTTP cannot update another worker notification');
 check(api('/notificaciones/1001/lectura',['leida'=>true],$token)['code']===200,'HTTP own notification read saved');
 check((int)$db->query('SELECT leida FROM wp_notificaciones WHERE id_notificacion=1001')->fetchColumn()===1,'notification read persisted');
 check(api('/notificaciones/marcar-todas',[],$token)['code']===200,'HTTP read-all available for own worker');
 $db->exec("INSERT INTO wp_bienestar_tests (id_test,codigo,titulo,descripcion,tipo,periodicidad) VALUES (101,'SYNTHETIC','Synthetic wellness','Isolated fixture','GENERAL','MENSUAL')");
 $db->exec("INSERT INTO wp_bienestar_preguntas (id_pregunta,id_test,orden,enunciado,tipo_respuesta) VALUES (101,101,1,'Synthetic scale','ESCALA_1_5'),(102,101,2,'Synthetic yes or no','SI_NO')");
 check(api('/bienestar/tests')['code']===401,'HTTP wellness catalog requires session');
 check(count(api('/bienestar/tests',null,$token)['data']['tests'])===3,'HTTP wellness catalog reads stored questions and Dani questionnaires');
 check(api('/bienestar/respuestas',['id_test'=>101,'respuestas'=>[['id_pregunta'=>101,'valor_numerico'=>3]]],$token)['code']===422,'HTTP wellness rejects incomplete answers');
 $wellness=['id_test'=>101,'respuestas'=>[['id_pregunta'=>101,'valor_numerico'=>3],['id_pregunta'=>102,'valor_numerico'=>1]]];
 check(api('/bienestar/respuestas',$wellness,$token)['code']===200,'HTTP wellness saves complete own response');
 check((int)$db->query('SELECT id_trabajador FROM wp_bienestar_evaluaciones WHERE id_test=101')->fetchColumn()===1,'wellness row belongs to authenticated worker');
 check((int)$db->query('SELECT COUNT(*) FROM wp_bienestar_respuestas')->fetchColumn()===2,'wellness answers persisted in isolated MySQL');
 $ownWellnessId=(int)$db->query('SELECT id_evaluacion_bienestar FROM wp_bienestar_evaluaciones WHERE id_test=101 AND id_trabajador=1')->fetchColumn();
 check(api('/bienestar/mis-respuestas/'.$ownWellnessId)['code']===401,'HTTP private wellness detail requires session');
 $ownWellnessDetail=api('/bienestar/mis-respuestas/'.$ownWellnessId,null,$token);
 check($ownWellnessDetail['code']===200 && count($ownWellnessDetail['data']['respuestas'])===2
     && (int)$ownWellnessDetail['data']['id_trabajador']===1,'HTTP returns complete answers only to their worker');
 check(api('/bienestar/respuestas',$wellness,$token)['code']===409,'HTTP wellness rejects duplicate period submission');
 check(count(api('/bienestar/mis-respuestas',null,$token)['data']['evaluaciones'])===1,'HTTP wellness history scoped to worker');
 $wellnessOther=api('/auth/login',['username'=>'fixture-admin','password'=>'Synthetic-only-123!'])['data']['auth']['access_token'];
 check(count(api('/bienestar/mis-respuestas',null,$wellnessOther)['data']['evaluaciones'])===0,'HTTP wellness history does not leak to another worker');
 check(api('/bienestar/mis-respuestas/'.$ownWellnessId,null,$wellnessOther)['code']===404,
     'HTTP private wellness detail hides another worker evaluation');
 $db->exec("INSERT INTO wp_bienestar_tests (id_test,codigo,titulo,descripcion,tipo,periodicidad) VALUES (102,'SYNTHETIC_WEEKLY','Synthetic weekly wellness','Isolated fixture','GENERAL','SEMANAL')");
 $db->exec("INSERT INTO wp_bienestar_preguntas (id_pregunta,id_test,orden,enunciado,tipo_respuesta) VALUES (103,102,1,'Synthetic weekly scale','ESCALA_1_5')");
 $weekly=['id_test'=>102,'respuestas'=>[['id_pregunta'=>103,'valor_numerico'=>4]]];
 check(api('/bienestar/respuestas',$weekly,$token)['code']===200,'HTTP weekly wellness saves own answers');
 check((string)$db->query('SELECT periodo_clave FROM wp_bienestar_evaluaciones WHERE id_test=102')->fetchColumn()===sprintf('%04d-W%02d',(int)date('o'),(int)date('W')),'weekly period uses ISO week year');
 check(api('/bienestar/respuestas',$weekly,$token)['code']===409,'HTTP weekly wellness rejects duplicate in same week');
 check(count(api('/bienestar/mis-respuestas',null,$wellnessOther)['data']['evaluaciones'])===0,'weekly history remains worker-scoped');
 $daniCatalog=api('/bienestar/tests',null,$token);
 $daniTests=[];
 foreach($daniCatalog['data']['tests'] as $test) $daniTests[$test['codigo']]=$test;
 check(isset($daniTests['DANI_SEMANAL'],$daniTests['DANI_MENSUAL'])
     && count($daniTests['DANI_SEMANAL']['preguntas'])===6
     && count($daniTests['DANI_MENSUAL']['preguntas'])===10,'HTTP reads both Dani questionnaires and all 16 questions');
 foreach(['DANI_SEMANAL'=>6,'DANI_MENSUAL'=>10] as $code=>$expected) {
     $answers=[];
     foreach($daniTests[$code]['preguntas'] as $question) {
         check(count($question['opciones'])===5,'Dani question has five stored answer choices');
         $answers[]=['id_pregunta'=>(int)$question['id_pregunta'],
             'id_opcion_seleccionada'=>(int)$question['opciones'][0]['id_opcion']];
     }
     $submission=['id_test'=>(int)$daniTests[$code]['id_test'],'respuestas'=>$answers];
     check(api('/bienestar/respuestas',$submission,$token)['code']===200,'HTTP saves all Dani '.$code.' answers');
     $saved=$db->prepare('SELECT COUNT(*) FROM wp_bienestar_respuestas r JOIN wp_bienestar_evaluaciones e ON e.id_evaluacion_bienestar=r.id_evaluacion_bienestar WHERE e.id_test=? AND e.id_trabajador=1');
     $saved->execute([(int)$daniTests[$code]['id_test']]);
     check((int)$saved->fetchColumn()===$expected,'MySQL persists all Dani '.$code.' answers for signed-in worker');
     check(api('/bienestar/respuestas',$submission,$token)['code']===409,'Dani '.$code.' duplicate period rejected');
 }
 check(count(api('/bienestar/mis-respuestas',null,$wellnessOther)['data']['evaluaciones'])===0,'Dani answers remain private from another worker');
 check(api('/bienestar/admin/resumen?mes='.date('Y-m'),null,$token)['code']===403,'worker cannot read organizational wellness');
 check(api('/bienestar/admin/resumen?mes='.date('Y-m'),null,$wellnessOther)['code']===200,'authorized admin sees organizational wellness');
 $planBase=['nombre'=>'Laboratory wellbeing activity','objetivo'=>'Synthetic objective',
     'dimension'=>'COMUNICACION','responsable'=>'Laboratory team',
     'fecha_programada'=>date('Y-m-d'),'hora_inicio'=>'08:00','hora_fin'=>'09:00',
     'estado'=>'PENDIENTE','observaciones'=>'Synthetic only'];
 $draftPlan=api('/bienestar/admin/planes',$planBase,$wellnessOther);
 check($draftPlan['code']===200 && $draftPlan['data']['estado_publicacion']==='BORRADOR',
     'wellbeing activity starts as an unpublished draft');
 check(count(api('/bienestar/actividades',null,$token)['data']['actividades'])===0,
     'worker does not see an unpublished activity');
 $planId=(int)$draftPlan['data']['id_plan'];
 $publishedPlan=apiMethod('PUT','/bienestar/admin/planes/'.$planId,
     $planBase+['estado_publicacion'=>'PUBLICADA','visibilidad'=>'COLABORADORES',
         'trabajadores_ids'=>[1]],$wellnessOther);
 check($publishedPlan['code']===200 && $publishedPlan['data']['publicada']===true,
     'administrator can publish for one worker');
 check(count(api('/bienestar/actividades',null,$token)['data']['actividades'])===1,
     'target worker sees the published activity');
 check(count(api('/bienestar/actividades',null,$wellnessOther)['data']['actividades'])===0,
     'administrator own account does not impersonate a worker');
 check(apiMethod('PUT','/bienestar/admin/planes/'.$planId,
     $planBase+['estado_publicacion'=>'PUBLICADA','visibilidad'=>'COLABORADORES',
         'trabajadores_ids'=>[]],$wellnessOther)['code']===422,
     'empty targeted audience cannot be published');
 check(api('/bienestar/admin/confidencial/'.$ownWellnessId,null,$wellnessOther)['code']===403,'general admin cannot read private wellness answers');
 $futureStart=date('Y-m-d H:i:s',time()-3600); $futureEnd=date('Y-m-d H:i:s',time()+86400);
 $draft=api('/bienestar/admin/campanas',['titulo'=>'Synthetic Ryff campaign','descripcion'=>'Voluntary laboratory fixture',
     'audiencia'=>'COLABORADOR','inicio_en'=>$futureStart,'fin_en'=>$futureEnd],$wellnessOther);
 check($draft['code']===200 && $draft['data']['estado']==='BORRADOR','admin creates draft campaign');
 $campaignId=(int)$draft['data']['id_campana'];
 check(apiMethod('PUT','/bienestar/admin/campanas/'.$campaignId,['id_test'=>101,'estado'=>'PUBLICADA'],$wellnessOther)['code']===409,
     'unvalidated questionnaire cannot be published as Ryff');
 check(count(api('/bienestar/campanas',null,$token)['data']['campanas'])===0,'draft campaign does not invite workers');
 $db->exec("INSERT INTO wp_bienestar_tests (id_test,codigo,titulo,descripcion,tipo,periodicidad) VALUES (103,'RYFF_SYNTHETIC','Synthetic Ryff','Not for production','GENERAL','LIBRE')");
 $db->exec("INSERT INTO wp_bienestar_ryff_instrumentos (id_test,version_instrumento,escala_min,escala_max,procedencia,validado_por,uso_autorizado_por,validado_en) VALUES (103,'synthetic',1,5,'test only','synthetic validator','lab',NOW())");
 $dims=['AUTOACEPTACION','RELACIONES_POSITIVAS','AUTONOMIA','DOMINIO_ENTORNO','PROPOSITO_VIDA','CRECIMIENTO_PERSONAL'];
 $question=$db->prepare('INSERT INTO wp_bienestar_preguntas (id_test,orden,enunciado,tipo_respuesta,es_inversa,dimension_ryff) VALUES (103,?,"Synthetic item","ESCALA_1_5",?,?)');
 for($i=1;$i<=18;$i++) $question->execute([$i,$i%2,$dims[($i-1)%6]]);
 check(apiMethod('PUT','/bienestar/admin/campanas/'.$campaignId,['id_test'=>103,'estado'=>'PUBLICADA'],$wellnessOther)['code']===200,
     'validated synthetic instrument can be published');
 $invite=api('/bienestar/campanas',null,$token);
 check($invite['code']===200 && count($invite['data']['campanas'])===1,'eligible worker sees published voluntary campaign');
 $form=api('/bienestar/campanas/'.$campaignId,null,$token);
 check($form['code']===200 && count($form['data']['preguntas'])===18,'worker sees 18 synthetic questions');
 $responses=[];foreach($form['data']['preguntas'] as $item)$responses[]=['id_pregunta'=>(int)$item['id_pregunta'],'valor_numerico'=>3];
 $ryffSubmission=['id_test'=>103,'id_campana'=>$campaignId,'respuestas'=>$responses];
 check(api('/bienestar/respuestas',$ryffSubmission,$token)['code']===422,'Ryff rejects submission without explicit consent');
 $ryffSubmission['consentimiento_informado']=true;
 check(api('/bienestar/respuestas',$ryffSubmission,$token)['code']===200,'consented synthetic Ryff submission persists');
 check((int)$db->query('SELECT COUNT(*) FROM wp_bienestar_participaciones')->fetchColumn()===1,'participation stores consent and evaluation link');
 $ryffEval=(int)$db->query('SELECT id_evaluacion_bienestar FROM wp_bienestar_participaciones LIMIT 1')->fetchColumn();
 $db->exec("INSERT INTO wp_rol_permisos SELECT 1,id_permiso,'TODOS' FROM wp_permisos WHERE codigo='bienestar.ver_privado'");
 $db->exec("INSERT INTO wp_bienestar_accesos_privados (id_evaluacion_bienestar,id_usuario_autorizado,finalidad,autorizado_por,autorizado_en,vence_en) VALUES ($ryffEval,2,'Synthetic purpose',2,NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY))");
 check(api('/bienestar/admin/confidencial/'.$ryffEval,null,$wellnessOther)['code']===403,
     'private Ryff review remains denied without separate individual consent');
 check(count(api('/bienestar/campanas',null,$token)['data']['campanas'])===0,'completed campaign no longer invites worker');
 $scheduled=api('/bienestar/admin/campanas',['titulo'=>'Future synthetic campaign','descripcion'=>'Future lab window',
     'audiencia'=>'PRACTICANTE','inicio_en'=>date('Y-m-d H:i:s',time()+86400),
     'fin_en'=>date('Y-m-d H:i:s',time()+172800)],$wellnessOther);
 check($scheduled['code']===200,'admin can schedule a future draft');
 $scheduledId=(int)$scheduled['data']['id_campana'];
 check(apiMethod('PUT','/bienestar/admin/campanas/'.$scheduledId,['id_test'=>103,'estado'=>'PUBLICADA'],$wellnessOther)['code']===200,
     'future campaign can be published but remains scheduled');
 check(api('/bienestar/campanas/'.$scheduledId,null,$token)['code']===403,
     'worker cannot open future or ineligible campaign');
 check(api('/bienestar/respuestas',['id_test'=>103,'id_campana'=>$scheduledId,
     'consentimiento_informado'=>true,'respuestas'=>$responses],$token)['code']===403,
     'submission is rejected outside campaign window');
 check(count(api('/bienestar/admin/resumen?mes='.date('Y-m'),null,$wellnessOther)['data']['dimensiones'])===0,
     'small Ryff group never exposes dimension means');
 $filtered=api('/bienestar/admin/resumen?mes='.date('Y-m').'&id_agencia=1&id_campana='.$campaignId,null,$wellnessOther);
 check($filtered['code']===200 && $filtered['data']['participantes_elegibles']===null
     && count($filtered['data']['dimensiones'])===0,'agency and campaign filters suppress small cohorts');
 $plan=api('/bienestar/admin/planes',['nombre'=>'Synthetic workshop','objetivo'=>'Improve team communication',
     'dimension'=>'RELACIONES_POSITIVAS','responsable'=>'Fixture','fecha_programada'=>date('Y-m-d'),'estado'=>'PENDIENTE'],
     $wellnessOther);
 check($plan['code']===200 && count(api('/bienestar/admin/planes',null,$wellnessOther)['data']['planes'])===2,
     'admin can create and read organizational improvement plan');
 check(apiMethod('PUT','/bienestar/admin/planes/'.$plan['data']['id_plan'],[
     'nombre'=>'Synthetic workshop','objetivo'=>'Improve team communication',
     'dimension'=>'RELACIONES_POSITIVAS','responsable'=>'Fixture',
     'fecha_programada'=>date('Y-m-d'),'estado'=>'EN_PROCESO','observaciones'=>'Lab only'],
     $wellnessOther)['code']===200,'admin can update improvement plan');
 check(api('/bienestar/admin/asistente',['pregunta'=>'Cómo mejorar la comunicación?'],$wellnessOther)['data']['tipo']==='REGLAS_LOCALES',
     'assistant returns transparent structured guidance');
 check(api('/bienestar/admin/asistente',['pregunta'=>'Alguien está en peligro inmediato'],$wellnessOther)['data']['tema']==='SEGURIDAD_INMEDIATA',
     'assistant routes immediate danger to emergency guidance without private data');
 check(api('/ranking')['code']===401,'HTTP ranking requires session');
 check(count(api('/ranking',null,$token)['data']['ranking'])===0,'HTTP ranking does not invent current standings');
 check(api('/ranking?periodo_tipo=SEMANAL',null,$token)['code']===422,'HTTP ranking rejects unsupported weekly snapshots');
 check(count(api('/ranking?periodo_tipo=TRIMESTRAL',null,$token)['data']['ranking'])===0,'HTTP ranking returns empty state for a period without a close');
 $admin->exec("UPDATE `$source`.trabajadores SET id_cargo=3 WHERE id_trabajador IN (1,2)");
 $db->exec("INSERT INTO wp_ranking_historico (periodo_tipo,periodo_clave,id_trabajador,puntos_kpi_periodo,puesto) VALUES ('MENSUAL','2026-10',1,20,2),('MENSUAL','2026-10',2,30,1)");
 check(count(api('/ranking?periodo_clave=2026-10',null,$token)['data']['ranking'])===1,'HTTP own-scope ranking hides other workers');
 $companyRanking=api('/ranking?periodo_clave=2026-10',null,$wellnessOther);
 check(count($companyRanking['data']['ranking'])===2,'HTTP admin ranking shows closed stored snapshot');
 check(($companyRanking['data']['resumen']['participantes'] ?? null)===2,'HTTP ranking summary counts authorized participants');
 check((int)$companyRanking['data']['ranking'][0]['id_trabajador']===2,'HTTP ranking orders by closed position');
 check(api('/ranking/2',null,$token)['code']===403,'HTTP ranking detail blocks other worker for own scope');
 check(count(api('/ranking/1',null,$wellnessOther)['data']['evolucion'])===1,'HTTP admin detail reads authorized history');
 $db->exec("INSERT INTO wp_ranking_historico (periodo_tipo,periodo_clave,id_trabajador,puntos_kpi_periodo,puesto,cerrado_en) VALUES ('MENSUAL','2026-09',1,10,1,'2026-10-01 00:00:00'),('MENSUAL','2026-09',2,10,2,'2026-10-01 00:00:00')");
 $withPrevious=api('/ranking?periodo_clave=2026-10',null,$wellnessOther)['data']['ranking'];
 check((int)$withPrevious[0]['puesto_anterior']===2 && (int)$withPrevious[1]['puesto_anterior']===1,
       'HTTP ranking compares positions only with a closed prior period');
 $db->exec("UPDATE wp_ranking_historico SET puesto=1 WHERE periodo_clave='2026-10' AND id_trabajador=1");
 $tied=api('/ranking?periodo_clave=2026-10',null,$wellnessOther)['data']['ranking'];
 check((int)$tied[0]['id_trabajador']===1 && (int)$tied[1]['id_trabajador']===2,
       'HTTP tied ranking has stable worker-id ordering');
 $db->exec("UPDATE wp_configuracion SET valor='AGENCIA' WHERE clave='ranking_visibilidad'");
 $admin->exec("INSERT INTO `$source`.agencias (id_agencia,nombre) VALUES (2,'Other synthetic agency')");
 $admin->exec("UPDATE `$source`.trabajadores SET id_agencia=2 WHERE id_trabajador=2");
 check(count(api('/ranking?periodo_clave=2026-10',null,$wellnessOther)['data']['ranking'])===1,'HTTP agency visibility excludes other agency');
 $admin->exec("UPDATE `$source`.trabajadores SET id_agencia=1 WHERE id_trabajador=2");
 $db->exec("UPDATE wp_configuracion SET valor='UNRECOGNIZED' WHERE clave='ranking_visibilidad'");
 check(count(api('/ranking?periodo_clave=2026-10',null,$token)['data']['ranking'])===1,'HTTP unknown ranking policy restricts to own row');
 $db->exec("UPDATE wp_configuracion SET valor='EMPRESA' WHERE clave='ranking_visibilidad'");
 $admin->exec("UPDATE `$source`.trabajadores SET id_cargo=1 WHERE id_trabajador IN (1,2)");
 check(api('/premios/admin',null,$token)['code']===403,'worker cannot administer prize publications');
 $prizeDraft=['titulo'=>'Synthetic recognition','descripcion'=>'Synthetic institutional recognition',
     'inicio_en'=>'2026-01-01 00:00:00','fin_en'=>'2030-01-01 23:59:59','estado_publicacion'=>'BORRADOR'];
 $created=api('/premios/admin',$prizeDraft,$wellnessOther);
 check($created['code']===200 && (int)$created['data']['id_premio']>0,'admin creates prize draft without points');
 $prizeId=(int)$created['data']['id_premio'];
 check(count(api('/premios',null,$token)['data']['premios'])===0,'draft stays hidden from worker catalog');
 check(apiMethod('PUT','/premios/admin/'.$prizeId,array_replace($prizeDraft,['estado_publicacion'=>'PUBLICADO']),$wellnessOther)['code']===422,
       'publication requires a validated image');
 $png=base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/QWQAAAAASUVORK5CYII=',true);
 check(apiUpload('/premios/admin/'.$prizeId.'/imagen','not-an-image','image/png',$wellnessOther)['code']===422,
       'invalid image content rejected despite claimed MIME');
 check(apiUpload('/premios/admin/'.$prizeId.'/imagen',$png,'image/png',$wellnessOther)['code']===200,
       'admin uploads validated PNG outside public directory');
 check(apiMethod('PUT','/premios/admin/'.$prizeId,array_replace($prizeDraft,['estado_publicacion'=>'PUBLICADO']),$wellnessOther)['code']===200,
       'admin publishes imaged prize');
 check(count(api('/premios',null,$token)['data']['premios'])===1,'worker sees published prize independently of KPI points');
 $imageContext=stream_context_create(['http'=>['header'=>"Authorization: Bearer $token\r\n",'ignore_errors'=>true]]);
 $served=file_get_contents("http://127.0.0.1:$port/api/v1/premios/$prizeId/imagen",false,$imageContext);
 check($served===$png && str_contains($http_response_header[0],'200'),
       'worker retrieves exact validated prize image through authenticated API');
 check(count(api('/premios',null,$wellnessOther)['data']['premios'])===1,'collaborator sees active prize');
 check(apiUpload('/premios/admin/'.$prizeId.'/imagen',$png,'image/png',$token)['code']===403,
       'worker cannot replace prize image');
 $jpeg=file_get_contents(dirname(__DIR__,2).'/app/src/main/res/drawable/collab_logo_website_official.jpg');
 check(apiUpload('/premios/admin/'.$prizeId.'/imagen',$jpeg,'image/jpeg',$wellnessOther)['code']===200,
       'admin replaces image with validated JPEG');
 $served=file_get_contents("http://127.0.0.1:$port/api/v1/premios/$prizeId/imagen",false,$imageContext);
 check($served===$jpeg,'worker receives replacement JPEG from server');
 check(apiMethod('PUT','/premios/admin/'.$prizeId,array_replace($prizeDraft,
       ['descripcion'=>'Edited synthetic institutional recognition','estado_publicacion'=>'PUBLICADO']),$wellnessOther)['code']===200,
       'admin edits published prize description');
 check(apiMethod('PUT','/premios/admin/'.$prizeId,array_replace($prizeDraft,
       ['estado_publicacion'=>'DESACTIVADO']),$wellnessOther)['code']===200
       && count(api('/premios',null,$token)['data']['premios'])===0,'deactivation hides prize immediately');
 check(apiMethod('PUT','/premios/admin/'.$prizeId,array_replace($prizeDraft,
       ['estado_publicacion'=>'PUBLICADO']),$wellnessOther)['code']===200
       && count(api('/premios',null,$token)['data']['premios'])===1,'reactivation restores valid prize');
 check((int)$db->query('SELECT COUNT(*) FROM wp_puntos_ledger')->fetchColumn()===0,'prize publication never awards points');
 $db->exec('UPDATE wp_premios SET inicio_en=DATE_ADD(NOW(),INTERVAL 1 DAY) WHERE id_premio='.$prizeId);
 check(count(api('/premios',null,$token)['data']['premios'])===0,'scheduled prize remains hidden by server clock');
 $db->exec('UPDATE wp_premios SET inicio_en=DATE_SUB(NOW(),INTERVAL 2 DAY),fin_en=DATE_SUB(NOW(),INTERVAL 1 DAY) WHERE id_premio='.$prizeId);
 check(count(api('/premios',null,$token)['data']['premios'])===0,'expired prize disappears automatically');
 $db->exec('UPDATE wp_premios SET fin_en=DATE_ADD(NOW(),INTERVAL 1 DAY) WHERE id_premio='.$prizeId);
 check(count(api('/premios',null,$token)['data']['premios'])===1,'renewed prize returns to worker catalog');
 check(apiMethod('DELETE','/premios/admin/'.$prizeId,null,$wellnessOther)['code']===200,'admin soft deletes prize');
 check(count(api('/premios',null,$token)['data']['premios'])===0,'deleted prize stays hidden');
 check((int)$db->query('SELECT COUNT(*) FROM wp_premios WHERE id_premio='.$prizeId)->fetchColumn()===1,
       'soft delete preserves prize record for audit');
 $db->exec("INSERT INTO wp_kpi_definiciones (id_kpi,codigo,id_categoria,nombre,descripcion,requiere_firma) VALUES (1,'SYNTHETIC',1,'Fixture','Fixture',1)");
 // Earlier authenticated reads can materialize cargo assignments automatically.
 $db->exec('DELETE FROM wp_kpi_asignaciones');
 $db->exec("INSERT INTO wp_kpi_asignaciones (id_asignacion,id_trabajador,id_kpi,fecha_inicio) VALUES (1,1,1,'2026-01-01'),(2,2,1,'2026-01-01')");
 $db->exec("INSERT INTO wp_kpi_evaluaciones (id_evaluacion,id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,meta_snapshot,peso_snapshot,puntos_base_snapshot) VALUES (1,1,1,1,'DIARIO','2026-10-15','2026-10-15 23:59:59',10,1,100),(2,2,2,1,'DIARIO','2026-10-15','2026-10-15 23:59:59',10,1,100),(3,1,1,1,'SEMANAL','2026-W43','2026-10-25 23:59:59',10,1,100)");
 $capture=['recordId'=>'REC-SYNTHETIC-1','kpiId'=>'SYNTHETIC','employeeId'=>'1','date'=>'2026-10-15','actualValue'=>8,'targetValueUsed'=>10,'weightUsed'=>100];
 check(api('/kpi/registros/sync',['records'=>[array_replace($capture,['targetValueUsed'=>11])]],$token)['code']===422,'HTTP KPI capture rejects client target differing from official snapshot');
 check(api('/kpi/registros/sync',['records'=>[$capture]],$token)['code']===200,'HTTP KPI capture accepts assigned official target');
 check((float)$db->query('SELECT peso_usado FROM wp_kpi_registros_android WHERE record_id="REC-SYNTHETIC-1"')->fetchColumn()===1.0,'KPI capture stores server weight instead of display percentage');
 check(api('/kpi/registros/sync',['records'=>[$capture]],$token)['data']['inserted']===0,'HTTP KPI capture retry is idempotent');
 check(api('/kpi/registros/sync',['records'=>[array_replace($capture,['recordId'=>'REC-SYNTHETIC-2','employeeId'=>'2'])]],$token)['code']===422,'HTTP KPI capture rejects another worker identity');
 check(count(api('/kpi/registros',null,$token)['data']['registros'])===1,'HTTP KPI capture history belongs to signed-in worker');
 check(api('/kpi/evaluaciones/2',null,$token)['code']===403,'HTTP KPI read ownership');
 check(api('/kpi/evaluaciones/1',null,$token)['code']===200,'HTTP own KPI detail available');
 check(api('/kpi/evaluaciones/2/borrador',['valor_reportado'=>5],$token)['code']===404,'HTTP KPI write ownership');
 check(api('/kpi/evaluaciones/1/calificar',['estado'=>'Aprobado'],$token)['code']===403,'HTTP worker cannot grade');
 check(api('/kpi/evaluaciones/1/entregar',['valor_reportado'=>10,'biometria'=>true],$token)['code']===422,'boolean biometric rejected');
 check(api('/kpi/evaluaciones/1/borrador',['valor_reportado'=>-10],$token)['code']===422,'negative value rejected');
 check(api('/kpi/evaluaciones/1/borrador',['valor_reportado'=>10,'comentarios_entrega'=>''],$token)['code']===200,'HTTP valid KPI draft available');
 [$key]=labEcKey();
 if (!$key) { throw new RuntimeException('Cannot create test signing key'); }
 $public=openssl_pkey_get_details($key)['key'];
 $st=$db->prepare('INSERT INTO wp_dispositivos (id_usuario,device_uid,nombre,clave_publica,huella_clave,estado) VALUES (1,"synthetic-device","Fixture",?,?,"ACTIVO")');$st->execute([$public,hash('sha256',$public)]);
 $device=(int)$db->lastInsertId();
 $digest=hash('sha256','EVAL:1:VAL:10:COMS:');
 $challenge=api('/auth/desafio',['id_usuario'=>2,'proposito'=>'ENTREGA_KPI','device_uid'=>'synthetic-device','referencia_tipo'=>'kpi_evaluacion','referencia_id'=>1,'hash_contenido'=>$digest],$token)['data'];
 openssl_sign($challenge['message'],$signature,$key,OPENSSL_ALGO_SHA256);
 $submission=['valor_reportado'=>10,'id_desafio'=>$challenge['challenge_id'],'firma_base64'=>base64_encode($signature)];
 check(api('/kpi/evaluaciones/1/entregar',array_replace($submission,['valor_reportado'=>11]),$token)['code']===401,'signed payload alteration rejected');
 check(api('/kpi/evaluaciones/1/entregar',$submission,$token)['code']===200,'valid EC signed submission accepted');
 check(api('/kpi/evaluaciones/1/entregar',$submission,$token)['code']===400,'duplicate submission rejected');
 check(!ChallengeService::verifyChallenge($challenge['challenge_id'],base64_encode($signature),1,$device,1,$digest),'challenge replay rejected');
 $adminLogin=api('/auth/login',['username'=>'fixture-admin','password'=>'Synthetic-only-123!']);$adminToken=$adminLogin['data']['auth']['access_token'];
 $adminSession=TokenService::validateAccessToken($adminToken);
 $securityUid='88888888-8888-4888-8888-888888888888';
 $securityPublic=$public;
 $securityHash=hash('sha256',$securityPublic);
 $securityCredential=hash_hmac('sha256',$hash,$cfg['security']['jwt_secret']);
 $st=$db->prepare('INSERT INTO wp_accesos_android (id_usuario,device_uid,nombre,clave_publica,huella_clave,credencial_hmac,id_sesion_registro,estado) VALUES (2,?,?,?,?,?,? ,"ACTIVO")');
 $st->execute([$securityUid,'Synthetic security device',$securityPublic,$securityHash,$securityCredential,$adminSession['id_sesion']]);
 $securityAccess=(int)$db->lastInsertId();
 $st=$db->prepare('INSERT INTO wp_claves_faciales (id_usuario,id_acceso_android,id_sesion_registro,clave_publica,huella_clave,credencial_hmac,estado) VALUES (2,?,?,?,?,?,"ACTIVO")');
 $st->execute([$securityAccess,$adminSession['id_sesion'],$securityPublic,hash('sha256','synthetic-face-security'),$securityCredential]);
 $secondUid='99999999-9999-4999-8999-999999999999';
 $st=$db->prepare('INSERT INTO wp_accesos_android (id_usuario,device_uid,nombre,clave_publica,huella_clave,credencial_hmac,id_sesion_registro,estado) VALUES (2,?,?,?,?,?,? ,"ACTIVO")');
 $st->execute([$secondUid,'Synthetic second device',$securityPublic,hash('sha256','synthetic-second-device'),$securityCredential,$adminSession['id_sesion']]);
 $secondAccess=(int)$db->lastInsertId();
 $st=$db->prepare('INSERT INTO wp_claves_faciales (id_usuario,id_acceso_android,id_sesion_registro,clave_publica,huella_clave,credencial_hmac,estado) VALUES (2,?,?,?,?,?,"ACTIVO")');
 $st->execute([$secondAccess,$adminSession['id_sesion'],$securityPublic,hash('sha256','synthetic-second-face'),$securityCredential]);
 try {
     $st->execute([$secondAccess,$adminSession['id_sesion'],$securityPublic,hash('sha256','synthetic-duplicate-face-on-device'),$securityCredential]);
     check(false,'V017 permits at most one active facial key per device');
 } catch (PDOException $uniqueFace) {
     check($uniqueFace->getCode()==='23000','V017 permits at most one active facial key per device');
 }
 $st=$db->prepare('INSERT INTO wp_sesiones (id_usuario,metodo,cliente,access_hash,access_expira_en,refresh_hash,refresh_expira_en,huella_credencial,ip,id_acceso_android,creada_en,ultimo_uso_en) VALUES (2,?,"ANDROID",?,DATE_ADD(NOW(),INTERVAL 1 HOUR),?,DATE_ADD(NOW(),INTERVAL 1 DAY),?,"127.0.0.1",?,NOW(),NOW())');
 foreach ([[$securityAccess,'BIOMETRIA'],[$securityAccess,'FACIAL'],[$secondAccess,'BIOMETRIA'],[$secondAccess,'FACIAL']] as $i=>$fixture) {
     $st->execute([$fixture[1],hash('sha256','biometric-access-'.$i),hash('sha256','biometric-refresh-'.$i),$securityCredential,$fixture[0]]);
 }
 check(api('/auth/seguridad/dispositivos',null,$adminToken)['code']===200,'HTTP own biometric device status available');
 check(count(api('/auth/seguridad/dispositivos',null,$adminToken)['data']['android'])===2,'HTTP status lists two devices');
 check(count(api('/auth/seguridad/dispositivos',null,$adminToken)['data']['rostro'])===2,'HTTP status lists two active facial keys on separate devices');
 check(count(api('/auth/seguridad/dispositivos',null,$token)['data']['android'])===0,'HTTP biometric status excludes another account');
 check(api('/auth/seguridad/dispositivos')['code']===401,'HTTP biometric status requires session');
 check(api('/auth/seguridad/revocar',['metodo'=>'INVALIDO'],$adminToken)['code']===422,'HTTP biometric revoke validates method');
 check(api('/auth/seguridad/revocar',['metodo'=>'ROSTRO'],$adminToken)['error_code']==='DEVICE_SELECTION_REQUIRED','HTTP multiple devices require selection');
 check(api('/auth/seguridad/revocar',['metodo'=>'ROSTRO','id_acceso'=>$securityAccess],$token)['code']===404,'HTTP cannot revoke another account face');
 check(api('/auth/seguridad/revocar',['metodo'=>'HUELLA','id_acceso'=>$securityAccess])['code']===401,'HTTP revoke requires session');
 check(api('/auth/seguridad/huella/desafio',['id_acceso'=>$securityAccess,'device_uid'=>$securityUid],$adminToken)['error_code']==='FINGERPRINT_ALREADY_ENABLED',
       'enabled fingerprint is not treated as a pending recovery');
 check(api('/auth/seguridad/revocar',['metodo'=>'HUELLA','id_acceso'=>$secondAccess],$adminToken)['code']===200,'HTTP fingerprint-only revocation on selected device');
 check((int)$db->query("SELECT huella_habilitada FROM wp_accesos_android WHERE id_acceso=$secondAccess")->fetchColumn()===0
       && (int)$db->query("SELECT huella_habilitada FROM wp_accesos_android WHERE id_acceso=$securityAccess")->fetchColumn()===1,'fingerprint revocation leaves other device untouched');
 check((int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE id_acceso_android=$secondAccess AND metodo='BIOMETRIA' AND revocada_en IS NOT NULL")->fetchColumn()===1,'selected fingerprint session revoked');
 check(api('/auth/seguridad/revocar',['metodo'=>'HUELLA','id_acceso'=>$securityAccess],$adminToken)['code']===200,'HTTP fingerprint-only revocation preserves facial guardian');
 check((int)$db->query("SELECT COUNT(*) FROM wp_claves_faciales WHERE id_acceso_android=$securityAccess AND estado='ACTIVO'")->fetchColumn()===1
       && (int)$db->query("SELECT COUNT(*) FROM wp_accesos_android WHERE id_acceso=$securityAccess AND estado='ACTIVO'")->fetchColumn()===1,'face key and guardian remain active after fingerprint revocation');
 check((int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE id_acceso_android=$securityAccess AND metodo='FACIAL' AND revocada_en IS NULL")->fetchColumn()===1,'facial session survives fingerprint revocation');
 $recoveryIdentity=['id_acceso'=>$securityAccess,'device_uid'=>$securityUid];
 check(api('/auth/seguridad/huella/desafio',$recoveryIdentity)['code']===401,'fingerprint recovery requires session');
 check(api('/auth/seguridad/huella/desafio',$recoveryIdentity,$token)['code']===403,'fingerprint recovery rejects another account');
 $recovery=api('/auth/seguridad/huella/desafio',$recoveryIdentity,$adminToken);
 check($recovery['code']===200,'fresh password starts same-device fingerprint recovery');
 [$foreignRecoveryKey]=labEcKey();
 openssl_sign($recovery['data']['message'],$wrongRecoverySignature,$foreignRecoveryKey,OPENSSL_ALGO_SHA256);
 $recoveryProof=$recoveryIdentity+['challenge_id'=>$recovery['data']['challenge_id'],'firma_base64'=>base64_encode($wrongRecoverySignature)];
 check(api('/auth/seguridad/huella/confirmar',$recoveryProof,$adminToken)['code']===401
       && (int)$db->query("SELECT huella_habilitada FROM wp_accesos_android WHERE id_acceso=$securityAccess")->fetchColumn()===0,
       'foreign key cannot recover disabled fingerprint');
 $recovery=api('/auth/seguridad/huella/desafio',$recoveryIdentity,$adminToken);
 openssl_sign($recovery['data']['message'],$validRecoverySignature,$key,OPENSSL_ALGO_SHA256);
 $recoveryProof=$recoveryIdentity+['challenge_id'=>$recovery['data']['challenge_id'],'firma_base64'=>base64_encode($validRecoverySignature)];
 $confirmedRecovery=api('/auth/seguridad/huella/confirmar',$recoveryProof,$adminToken);
 check($confirmedRecovery['code']===200 && $confirmedRecovery['data']['credential_updated']===true
       && (int)$db->query("SELECT huella_habilitada FROM wp_accesos_android WHERE id_acceso=$securityAccess")->fetchColumn()===1,
       'recovery reports update only after target fingerprint flag persisted');
 check(api('/auth/seguridad/huella/confirmar',$recoveryProof,$adminToken)['code']===401,'fingerprint recovery challenge cannot be replayed');
 $retryRecovery=api('/auth/seguridad/huella/desafio',$recoveryIdentity,$adminToken);
 openssl_sign($retryRecovery['data']['message'],$retrySignature,$key,OPENSSL_ALGO_SHA256);
 $retryResult=api('/auth/seguridad/huella/confirmar',$recoveryIdentity+['challenge_id'=>$retryRecovery['data']['challenge_id'],
       'firma_base64'=>base64_encode($retrySignature)],$adminToken);
 check($retryResult['code']===200 && $retryResult['data']['credential_updated']===false
       && $retryResult['data']['huella_habilitada']===true,
       'signed recovery retry reports prior confirmation without claiming a second update');
 $separatePassword=TokenService::createSession(2,null,'PASSWORD','127.0.0.1',$hash)['access_token'];
 check(api('/auth/seguridad/huella/desafio',$recoveryIdentity,$separatePassword)['error_code']==='FINGERPRINT_ALREADY_ENABLED',
       'different password session cannot claim an already enabled fingerprint as its recovery');
 check((int)$db->query("SELECT COUNT(*) FROM wp_claves_faciales WHERE id_acceso_android=$securityAccess AND estado='ACTIVO'")->fetchColumn()===1
       && (int)$db->query("SELECT huella_habilitada FROM wp_accesos_android WHERE id_acceso=$secondAccess")->fetchColumn()===0,
       'recovery preserves face and other-device fingerprint state');
 check((int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE id_acceso_android=$securityAccess AND metodo='BIOMETRIA' AND revocada_en IS NULL")->fetchColumn()===0,
       'fingerprint recovery does not restore revoked sessions');
 check(api('/auth/seguridad/revocar',['metodo'=>'ROSTRO','id_acceso'=>$securityAccess],$adminToken)['code']===200,'HTTP face-only revocation');
 check((int)$db->query("SELECT COUNT(*) FROM wp_claves_faciales WHERE id_acceso_android=$securityAccess AND estado='ACTIVO'")->fetchColumn()===0
       && (int)$db->query("SELECT huella_habilitada FROM wp_accesos_android WHERE id_acceso=$securityAccess")->fetchColumn()===1,'face revocation preserves fingerprint');
 check((int)$db->query("SELECT COUNT(*) FROM wp_claves_faciales WHERE id_acceso_android=$secondAccess AND estado='ACTIVO'")->fetchColumn()===1
       && (int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE id_acceso_android=$secondAccess AND metodo='FACIAL' AND revocada_en IS NULL")->fetchColumn()===1,
       'face-only revocation preserves face and session on another device');
 check((int)$db->query("SELECT COUNT(*) FROM wp_sesiones WHERE id_acceso_android=$securityAccess AND metodo='FACIAL' AND revocada_en IS NOT NULL")->fetchColumn()===1,'selected facial session revoked');
 check(api('/auth/seguridad/revocar',['metodo'=>'AMBOS','id_acceso'=>$securityAccess],$adminToken)['code']===200,'HTTP both methods revoke selected device');
 check(api('/auth/seguridad/huella/desafio',$recoveryIdentity,$adminToken)['code']===403,'revoked Android key cannot be recovered');
 check((int)$db->query("SELECT COUNT(*) FROM wp_accesos_android WHERE id_acceso=$securityAccess AND estado='REVOCADO'")->fetchColumn()===1
       && (int)$db->query("SELECT COUNT(*) FROM wp_accesos_android WHERE id_acceso=$secondAccess AND estado='ACTIVO'")->fetchColumn()===1,'both-method revocation leaves other device active');
 $db->prepare('UPDATE wp_sesiones SET creada_en=DATE_SUB(NOW(),INTERVAL 10 MINUTE) WHERE id_sesion=?')->execute([$adminSession['id_sesion']]);
 check(api('/auth/seguridad/revocar',['metodo'=>'AMBOS','id_acceso'=>$secondAccess],$adminToken)['code']===403,'stale password session cannot revoke another device');
 $db->prepare('UPDATE wp_sesiones SET creada_en=NOW() WHERE id_sesion=?')->execute([$adminSession['id_sesion']]);
 check(api('/auth/login',['username'=>'fixture-admin','password'=>'Synthetic-only-123!'])['code']===200,'password login survives biometric revocation');
 $db->exec('DELETE FROM wp_claves_faciales WHERE id_usuario=2');
 $db->exec("DELETE FROM wp_sesiones WHERE id_acceso_android IN ($securityAccess,$secondAccess)");
 $db->exec('DELETE FROM wp_accesos_android WHERE id_usuario=2');
 check(api('/dashboard/resumen',null,$adminToken)['code']===200,'HTTP admin dashboard available');
 $emptyDashboard=api('/dashboard/resumen?periodo=2099-W52',null,$adminToken);
 check($emptyDashboard['code']===200
       && isset($emptyDashboard['data']['kpi']['kpis_vencidos'])
       && (int)$emptyDashboard['data']['kpi']['kpis_vencidos']===0
       && isset($emptyDashboard['data']['kpi']['kpis_pendientes'])
       && (int)$emptyDashboard['data']['kpi']['kpis_pendientes']===0
       && isset($emptyDashboard['data']['actividades']['actividades_pendientes_revision']),
       'empty dashboard KPI period returns numeric zero counts for notices');
 check(api('/personal',null,$adminToken)['code']===200,'HTTP admin personnel list available');
 check(api('/personal/1',null,$adminToken)['code']===200,'HTTP admin personnel detail available');
 check(api('/personal/1/foto')['code']===401,'profile photo requires authentication');
 check(api('/personal/1/foto',null,$adminToken)['code']===404,
       'profile photo endpoint has no public fallback or synthetic personal image');
 check(api('/kpi/evaluaciones/1/calificar',['estado'=>'Aprobado','puntos_otorgados'=>99999],$adminToken)['code']===200,'authorized server-side grading');
 check((int)$db->query('SELECT SUM(puntos) FROM wp_puntos_ledger WHERE id_trabajador=1')->fetchColumn()===20,'client score ignored; approved tier controls points');
 foreach ([0=>10,39=>10,40=>12,59=>12,60=>14,79=>14,80=>17,89=>17,90=>20,150=>20] as $percentage=>$points)
     check(\WebSitePlus\Kpi\KpiPointsPolicy::forPercentage((float)$percentage)===$points,
           'approved KPI points at '.$percentage.' percent');
 check(api('/kpi/evaluaciones/1/calificar',['estado'=>'Aprobado'],$adminToken)['code']>=400 && (int)$db->query('SELECT COUNT(*) FROM wp_puntos_ledger WHERE id_trabajador=1')->fetchColumn()===1,'repeated grading does not duplicate credits');
 check(api('/ranking/cierres',['periodo_clave'=>'2026-08'],$token)['code']===403,'worker cannot close official ranking');
 check(api('/ranking/cierres',['periodo_clave'=>'2026-08'],$adminToken)['code']===409,'ranking close requires approved cross-cargo policy');
 $rule=['codigo'=>'SYNTH-ADMIN','nombre'=>'Synthetic approved metric','descripcion'=>'Synthetic review measurement',
     'id_categoria'=>1,'id_cargo_objetivo'=>1,'responsable_revision'=>2,'tipo_metrica'=>'NUMERICO',
     'unidad_medida'=>'ASUNTOS','meta_esperada'=>10,'peso'=>2,'puntos_base'=>50,'periodicidad'=>'MENSUAL'];
 check(api('/kpi/oficial/definiciones',$rule,$token)['code']===403,'worker cannot configure official KPI');
 check(api('/kpi/oficial/definiciones',array_diff_key($rule,['peso'=>true]),$adminToken)['code']===422,
       'official KPI requires explicitly supplied weight');
 $defined=api('/kpi/oficial/definiciones',$rule,$adminToken);
 check($defined['code']===200,'admin stores official KPI rule pending approval');
 $official=(int)$defined['data']['id_kpi'];
 check(api('/kpi/oficial/asignaciones',['id_trabajador'=>1,'id_kpi'=>$official,'fecha_inicio'=>'2026-08-01'],$adminToken)['code']===422,
       'unapproved KPI cannot be assigned');
 check(api('/kpi/oficial/definiciones/'.$official.'/aprobar',[],$adminToken)['code']===200,'authorized rule approval');
 check(api('/kpi/oficial/asignaciones',['id_trabajador'=>1,'id_kpi'=>$official,'fecha_inicio'=>'2026-08-01'],$adminToken)['code']===200,
       'approved KPI explicitly assigned to worker');
 $db->exec('UPDATE wp_kpi_asignaciones SET activo=0 WHERE id_kpi='.$official);
 $syntheticPayload=json_encode(['days'=>[['date'=>'2026-08-15','isRegistered'=>true,
     'priorityCount'=>8,'ownCompliancePct'=>100]],'fakePercentage'=>100]);
 $st=$db->prepare('INSERT INTO wp_kpi_formularios_detalle
     (id_trabajador,id_usuario,id_cargo,tipo_formulario,registro_uid,revision,payload_json,payload_sha256,creado_en)
     VALUES (1,1,1,"GERENCIA","synthetic-reviewed-report",1,?,?,"2026-08-15 12:00:00")');
 $st->execute([$syntheticPayload,hash('sha256',$syntheticPayload)]);
 $report=(int)$db->lastInsertId();
 $received=api('/kpi/oficial/reportes',null,$adminToken);
 check($received['code']===200 && count(array_filter($received['data']['reportes']??[],
     static fn(array $item): bool => (int)($item['id_detalle']??0)===$report))===1,
     'admin receives the confirmed report before review');
 check(api('/kpi/oficial/reportes/'.$report.'/evaluar',['observacion'=>'Synthetic verified measurement'],$token)['code']===403,
       'worker cannot review own report');
 $reviewed=api('/kpi/oficial/reportes/'.$report.'/evaluar',[
     'observacion'=>'Synthetic verified measurement'],$adminToken);
 check($reviewed['code']===200 && (float)$reviewed['data']['valor_calculado']===8.0
     && (float)$reviewed['data']['porcentaje_estimado']===80.0
     && (int)$reviewed['data']['puntos_si_aprueba']===17,
     'authorized reviewer links raw form with server measured value');
 check((int)$db->query('SELECT COUNT(*) FROM wp_kpi_asignaciones WHERE id_kpi='.$official.' AND activo=1')->fetchColumn()===1,
     'approved KPI assignment follows the current attendance cargo automatically');
 $reviewedId=(int)$reviewed['data']['id_evaluacion'];
 check(api('/ranking/cierres',['periodo_clave'=>'2026-08'],$adminToken)['code']===409,
       'unapproved official evaluation never creates ranking');
 $originalHash=(string)$db->query('SELECT payload_sha256 FROM wp_kpi_formularios_detalle WHERE id_detalle='.$report)->fetchColumn();
 $db->exec("UPDATE wp_kpi_formularios_detalle SET payload_sha256=REPEAT('a',64) WHERE id_detalle=".$report);
 check(api('/kpi/evaluaciones/'.$reviewedId.'/calificar',['estado'=>'Aprobado'],$adminToken)['code']===409,
       'changed raw form cannot be approved against stale review');
 $db->prepare('UPDATE wp_kpi_formularios_detalle SET payload_sha256=? WHERE id_detalle=?')->execute([$originalHash,$report]);
 check(api('/kpi/evaluaciones/'.$reviewedId.'/calificar',['estado'=>'Aprobado'],$adminToken)['code']===200,
       'authorized official KPI approval after review');
 check(api('/ranking/cierres',['periodo_clave'=>'2026-08'],$adminToken)['code']===409,
       'approved evaluation still requires explicit comparison policy');
 check(api('/ranking/politicas',['periodo_clave'=>'2026-08','formula'=>'PROMEDIO_PONDERADO',
     'comparacion_cargos_aprobada'=>true,'justificacion'=>'Synthetic approval of weighted comparison across cargos'], $adminToken)['code']===200,
       'explicit synthetic cross-cargo ranking rule recorded');
 $closed=api('/ranking/cierres',['periodo_clave'=>'2026-08'],$adminToken);
 check($closed['code']===200 && (int)$closed['data']['participantes']===0,'manager KPI is excluded from ranking');
 check(api('/ranking/cierres',['periodo_clave'=>'2026-08'],$adminToken)['code']===409,'monthly close cannot repeat');
 check((int)$db->query("SELECT COUNT(*) FROM wp_ranking_historico WHERE periodo_clave='2026-08'")->fetchColumn()===0,
       'manager monthly snapshot is absent from ranking');
 check(api('/ranking?periodo_tipo=MENSUAL&periodo_clave=2026-08',null,$adminToken)['data']['periodo_clave']==='2026-08',
       'closed historical month can be selected');
 check(count(api('/ranking/periodos',null,$adminToken)['data']['periodos'])===1,
       'server lists actual monthly closes');
 check(api('/kpi/oficial/reportes/'.$report.'/evaluar',[
       'observacion'=>'Synthetic repeat after close'],$adminToken)['code']===409,
       'closed month refuses new reviews');
 $db->exec('UPDATE wp_kpi_definiciones SET activo=0 WHERE id_kpi='.$official);
 $db->exec("INSERT INTO wp_kpi_definiciones
     (codigo,id_categoria,nombre,descripcion,tipo_metrica,unidad_medida,meta_esperada,peso,puntos_base,
      periodicidad,aplica_a,id_cargo_objetivo,responsable_revision,activo,regla_aprobada_por,regla_aprobada_en)
     VALUES ('SYNTH-AUTO',1,'Automatic manager KPI','Synthetic weekly goal','PORCENTAJE','CUMPLIMIENTO',100,1,20,
             'SEMANAL','CARGO',1,2,1,2,NOW())");
 $autoDays=[];
 for ($i=0;$i<6;$i++) $autoDays[]=['date'=>date('Y-m-d',strtotime('2026-07-20 +'.$i.' days')),
     'isRegistered'=>true,'trackingPerformed'=>'Synthetic recorded activity','ownCompliancePct'=>100];
 $autoPayload=json_encode(['days'=>$autoDays,'fakePercentage'=>0]);
 $st=$db->prepare('INSERT INTO wp_kpi_formularios_detalle
     (id_trabajador,id_usuario,id_cargo,tipo_formulario,registro_uid,revision,payload_json,payload_sha256,creado_en)
     VALUES (1,1,1,"GERENCIA","synthetic-auto-report",1,?,?,"2026-07-25 12:00:00")');
 $st->execute([$autoPayload,hash('sha256',$autoPayload)]);
 $autoReport=(int)$db->lastInsertId();
 $autoReview=api('/kpi/oficial/reportes/'.$autoReport.'/evaluar',[],$adminToken);
 check($autoReview['code']===200 && (float)$autoReview['data']['valor_calculado']===100.0
     && (float)$autoReview['data']['porcentaje_estimado']===100.0
     && (int)$autoReview['data']['puntos_si_aprueba']===20,
     'approved cargo rule computes one hundred percent without a numeric ID or phone percentage');
 $db->exec('DELETE FROM wp_kpi_reporte_evaluaciones WHERE id_evaluacion='.(int)$autoReview['data']['id_evaluacion']);
 $db->exec('DELETE FROM wp_kpi_evaluaciones WHERE id_evaluacion='.(int)$autoReview['data']['id_evaluacion']);
 $db->exec("DELETE FROM wp_kpi_asignaciones WHERE id_kpi=(SELECT id_kpi FROM wp_kpi_definiciones WHERE codigo='SYNTH-AUTO')");
 $db->exec("DELETE FROM wp_kpi_definiciones WHERE codigo='SYNTH-AUTO'");
 $db->exec('DELETE FROM wp_kpi_formularios_detalle WHERE id_detalle='.$autoReport);
 $db->exec('UPDATE wp_kpi_definiciones SET activo=1 WHERE id_kpi='.$official);
 $admin->exec("UPDATE `$source`.trabajadores SET id_cargo=5 WHERE id_trabajador=1");
 $db->exec("INSERT INTO wp_kpi_definiciones
     (codigo,id_categoria,nombre,descripcion,tipo_metrica,unidad_medida,meta_esperada,peso,puntos_base,
      periodicidad,aplica_a,id_cargo_objetivo,responsable_revision,activo,regla_aprobada_por,regla_aprobada_en)
     VALUES ('SYNTH-SALES',1,'Automatic sales KPI','Synthetic weekly sales goal','PORCENTAJE','CUMPLIMIENTO',100,1,20,
             'SEMANAL','CARGO',5,2,1,2,NOW())");
 $salesKpi=(int)$db->query("SELECT id_kpi FROM wp_kpi_definiciones WHERE codigo='SYNTH-SALES'")->fetchColumn();
 $salesIds=[];
 $salesNames=['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'];
 for ($i=0;$i<6;$i++) {
     $salesPayload=json_encode(['fullDateFormatted'=>$salesNames[$i].', '.(20+$i).' de julio de 2026',
         'saved'=>true,'llamadas'=>$i===0?100:0,'visitas'=>$i===0?60:0,
         'cotizaciones'=>$i===0?30:0,'seguimiento'=>$i===0?50:0,
         'prospectos'=>$i===0?15:0,'clientes'=>$i===0?3:0]);
     $st=$db->prepare('INSERT INTO wp_kpi_formularios_detalle
         (id_trabajador,id_usuario,id_cargo,tipo_formulario,registro_uid,revision,payload_json,payload_sha256,creado_en)
         VALUES (1,1,5,"VENTAS",?,1,?,? ,"2026-07-25 12:00:00")');
     $st->execute(['synthetic-sales-'.$i,$salesPayload,hash('sha256',$salesPayload)]);
     $salesIds[]=(int)$db->lastInsertId();
 }
 $salesReview=api('/kpi/oficial/reportes/'.end($salesIds).'/evaluar',[],$adminToken);
 check($salesReview['code']===200 && (float)$salesReview['data']['porcentaje_estimado']===100.0
     && (int)$salesReview['data']['puntos_si_aprueba']===20,
     'six daily sales forms produce one weekly official percentage');
 $salesEval=(int)$salesReview['data']['id_evaluacion'];
 check((int)$db->query('SELECT COUNT(*) FROM wp_kpi_reporte_evaluaciones WHERE id_evaluacion='.$salesEval)->fetchColumn()===6,
     'sales evaluation preserves all six source report hashes');
 $salesHash=(string)$db->query('SELECT payload_sha256 FROM wp_kpi_formularios_detalle WHERE id_detalle='.$salesIds[1])->fetchColumn();
 $db->exec("UPDATE wp_kpi_formularios_detalle SET payload_sha256=REPEAT('a',64) WHERE id_detalle=".$salesIds[1]);
 check(api('/kpi/evaluaciones/'.$salesEval.'/calificar',['estado'=>'Aprobado'],$adminToken)['code']===409,
     'changed sales report cannot be approved from a stale weekly calculation');
 $db->prepare('UPDATE wp_kpi_formularios_detalle SET payload_sha256=? WHERE id_detalle=?')->execute([$salesHash,$salesIds[1]]);
 check(api('/kpi/evaluaciones/'.$salesEval.'/calificar',['estado'=>'Aprobado'],$adminToken)['code']===200,
     'all six unchanged sales reports may be approved once');
 $db->exec('DELETE FROM wp_puntos_ledger WHERE referencia_tipo="kpi_evaluacion" AND referencia_id='.$salesEval);
 $db->exec('DELETE FROM wp_kpi_revisiones WHERE id_evaluacion='.$salesEval);
 $db->exec('DELETE FROM wp_kpi_reporte_evaluaciones WHERE id_evaluacion='.$salesEval);
 $db->exec('DELETE FROM wp_kpi_evaluaciones WHERE id_evaluacion='.$salesEval);
 $db->exec('DELETE FROM wp_kpi_asignaciones WHERE id_kpi='.$salesKpi);
 $db->exec('DELETE FROM wp_kpi_definiciones WHERE id_kpi='.$salesKpi);
 $db->exec('DELETE FROM wp_kpi_formularios_detalle WHERE id_detalle IN ('.implode(',',$salesIds).')');
 $admin->exec("UPDATE `$source`.trabajadores SET id_cargo=1 WHERE id_trabajador=1");
 $db->exec("INSERT INTO wp_premios (id_premio,titulo,descripcion,puntos_requeridos,stock_disponible) VALUES (1000,'Fixture','Fixture',25,3)");
 check(api('/premios',null,$token)['code']===200,'HTTP reward catalog available');
 $redeem=['id_premio'=>1000,'solicitud_id'=>'synthetic-request-0001'];
 check(api('/premios/canjear',$redeem,$token)['code']===200 && api('/premios/canjear',$redeem,$token)['code']===200,'redemption retry accepted idempotently');
 check((int)$db->query('SELECT COUNT(*) FROM wp_canjes')->fetchColumn()===1,'one redemption per request');
 check(api('/premios/mis-puntos',null,$token)['code']===200,'HTTP own points available');
 check(api('/premios/canjear',array_replace($redeem,['solicitud_id'=>'synthetic-request-0002']),$token)['code']===400,'insufficient balance rejected');
 $admin->exec("INSERT INTO `$source`.permisos_laborales (id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,estado) VALUES (1,1,'Vacaciones','2026-10-19','2026-10-25','Aprobado')");
 check(api('/kpi/mis-kpis',null,$token)['code']===200,'KPI leave synchronization');
 check($db->query('SELECT estado FROM wp_kpi_evaluaciones WHERE id_evaluacion=3')->fetchColumn()==='No exigible','approved leave excludes KPI noncompliance');
 check((int)$db->query('SELECT COUNT(*) FROM wp_kpi_revisiones WHERE id_evaluacion=3')->fetchColumn()===1,'leave exemption history retained');
 check(\WebSitePlus\Auth\LaborPolicy::approvedLeaveOnDate(1,'2026-10-19') !== null
       && \WebSitePlus\Auth\LaborPolicy::approvedLeaveOnDate(1,'2026-10-26') === null,
       'approved vacation applies from first through last date only');
 $monday=(new DateTimeImmutable('today'))->modify('monday this week');
 $weekStart=$monday->format('Y-m-d');$vacationEnd=$monday->modify('+20 days')->format('Y-m-d');
 $st=$admin->prepare("INSERT INTO `$source`.permisos_laborales (id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,estado) VALUES (100,1,'Vacaciones',?,?,'Aprobado')");
 $st->execute([$weekStart,$vacationEnd]);
 $calendar=api('/kpi/formularios/permitidos',null,$token);
 check($calendar['code']===200 && count($calendar['data']['calendario_laboral']['dias']??[])===6
       && count(array_filter($calendar['data']['calendario_laboral']['dias'],fn($d)=>$d['manana']||$d['tarde']))===0,
       'three-week approved vacation removes all KPI shifts this week');
 $admin->exec("DELETE FROM `$source`.permisos_laborales WHERE id_permiso_laboral=100");
 $st=$admin->prepare("INSERT INTO `$source`.consumos_extras (id_trabajador,tipo_consumo,fecha_permiso,detalles) VALUES (1,'Permiso',?,'15:30 a 18:30')");
 $st->execute([$monday->modify('+1 day')->format('Y-m-d')]);
 $calendar=api('/kpi/formularios/permitidos',null,$token);
 $tuesday=$calendar['data']['calendario_laboral']['dias'][1]??[];
 check($calendar['code']===200 && ($tuesday['manana']??false)===true && ($tuesday['tarde']??true)===false,
       'approved overtime compensation excuses only the afternoon shift');
 $admin->exec("DELETE FROM `$source`.consumos_extras WHERE id_trabajador=1");
 $previousMonday=$monday->modify('-7 days')->format('Y-m-d');
 $previousTuesday=$monday->modify('-6 days')->format('Y-m-d');
 $snapshot=json_encode([['dia'=>'Lunes','asiste'=>1,'asiste_manana'=>1,'asiste_tarde'=>1,
     'hora_inicio'=>'08:00','hora_fin'=>'13:30','hora_inicio_tarde'=>'15:30','hora_fin_tarde'=>'18:30'],
     ['dia'=>'Martes','asiste'=>0,'asiste_manana'=>0,'asiste_tarde'=>0]],JSON_UNESCAPED_UNICODE);
 $st=$admin->prepare("INSERT INTO `$source`.horario_vigencias (id_trabajador,vigente_desde,horario_json) VALUES (1,?,?)");
 $st->execute([$previousMonday,$snapshot]);
 $historicCoverage=\WebSitePlus\Auth\KpiLaborCalendar::periodCoverage(1,$previousMonday,$previousTuesday);
 check($historicCoverage['turnos_programados']===2 && $historicCoverage['turnos_exigibles']===2,
       'past KPI period uses attendance schedule effective on those dates');
 $admin->exec("DELETE FROM `$source`.horario_vigencias WHERE id_trabajador=1");
 $seasonStart=$monday->modify('-30 days')->format('Y-m-d');
 $seasonEnd=$monday->modify('+30 days')->format('Y-m-d');
 $st=$admin->prepare("INSERT INTO `$source`.temporadas_horario (id_temporada,fecha_inicio,fecha_fin) VALUES (50,?,?)");
 $st->execute([$seasonStart,$seasonEnd]);
 $admin->exec("INSERT INTO `$source`.horarios_trabajadores (id_horario_trabajador,id_trabajador,id_temporada,id_horario) VALUES (50,1,50,50)");
 $admin->exec("INSERT INTO `$source`.horario_detalles (id_horario,dia_semana,hora_entrada,hora_salida,inicio_refrigerio,fin_refrigerio,es_laborable) VALUES
    (50,'Lunes','08:00','13:30','13:30','15:30',1),
    (50,'Martes','15:30','18:30','13:30','15:30',1)");
 $practiceCalendar=api('/kpi/formularios/permitidos',null,$token);
 check(($practiceCalendar['data']['calendario_laboral']['dias'][0]['turnos_exigibles']??0)===2,
       'practitioner uses current practice schedule before conversion');
 $admin->exec("UPDATE `$source`.trabajadores SET tipo='Colaborador',id_cargo=4 WHERE id_trabajador=1");
 $st=$admin->prepare("INSERT INTO `$source`.tipo_vigencias (id_trabajador,vigente_desde,tipo,id_cargo,origen) VALUES (1,?,'Colaborador',4,'EDICION_ADMIN') ON DUPLICATE KEY UPDATE tipo=VALUES(tipo),id_cargo=VALUES(id_cargo),origen=VALUES(origen)");
 $st->execute([$weekStart]);
 $converted=api('/kpi/formularios/permitidos',null,$token);
 $convertedDays=$converted['data']['calendario_laboral']['dias']??[];
 check($converted['code']===200 && ($converted['data']['tipo_trabajador']??null)==='Colaborador'
       && ($converted['data']['id_cargo']??0)===4
       && ($converted['data']['cargo_nombre']??null)==='Programador Web'
       && ($converted['data']['tipos_formulario']??[])===['PROGRAMACION'],
       'conversion immediately returns current attendance type, cargo and authorized form');
 check(($convertedDays[0]['manana']??false)===true && ($convertedDays[0]['tarde']??true)===false
       && ($convertedDays[1]['manana']??true)===false && ($convertedDays[1]['tarde']??false)===true
       && ($convertedDays[2]['turnos_exigibles']??-1)===0,
       'converted collaborator uses seasonal worker schedule, never stale practitioner rows');
 $earlier=\WebSitePlus\Auth\KpiLaborCalendar::periodCoverage(1,$previousMonday,$previousTuesday);
 check($earlier['tipo_historico_confirmado'] && $earlier['turnos_programados']===4,
       'past practitioner week retains its historical type after conversion');
 $unverified=\WebSitePlus\Auth\KpiLaborCalendar::periodCoverage(1,'2025-12-29','2025-12-29');
 check($unverified['tipo_historico_confirmado']===false,
       'dates before the first recorded labor type cannot be treated as verified');
 $admin->exec("UPDATE `$source`.horarios_trabajadores SET dias_asistencia='Lunes' WHERE id_horario_trabajador=50");
 $restricted=api('/kpi/formularios/permitidos',null,$token);
 check(($restricted['data']['calendario_laboral']['dias'][0]['turnos_exigibles']??0)===1
       && ($restricted['data']['calendario_laboral']['dias'][1]['turnos_exigibles']??-1)===0,
       'collaborator schedule respects dias_asistencia override from Asistencias');
 $admin->exec("UPDATE `$source`.trabajadores SET tipo='Practicante',id_cargo=1 WHERE id_trabajador=1");
 $st=$admin->prepare("UPDATE `$source`.tipo_vigencias SET tipo='Practicante',id_cargo=1 WHERE id_trabajador=1 AND vigente_desde=?");
 $st->execute([$weekStart]);
 $returned=api('/kpi/formularios/permitidos',null,$token);
 check($returned['code']===200 && ($returned['data']['tipo_trabajador']??null)==='Practicante'
       && ($returned['data']['id_cargo']??0)===1
       && ($returned['data']['calendario_laboral']['dias'][0]['turnos_exigibles']??0)===2,
       'return to practitioner reads current practice schedule rather than stale collaborator season');
 $admin->exec("DELETE FROM `$source`.horario_practicantes WHERE id_trabajador=1 AND dia='Lunes'");
 $missingPractice=api('/kpi/formularios/permitidos',null,$token);
 check(($missingPractice['data']['calendario_laboral']['dias'][0]['turnos_exigibles']??-1)===0,
       'missing practitioner weekday never borrows old collaborator season');
 $schedule->execute([1,'Lunes']);
 $admin->exec("DELETE FROM `$source`.horarios_trabajadores WHERE id_trabajador=1 AND id_temporada=50");
 $admin->exec("DELETE FROM `$source`.horario_detalles WHERE id_horario=50");
 $admin->exec("DELETE FROM `$source`.temporadas_horario WHERE id_temporada=50");
 $db->exec("INSERT INTO wp_kpi_evaluaciones (id_evaluacion,id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,meta_snapshot,peso_snapshot,puntos_base_snapshot) VALUES (900001,1,1,1,'SEMANAL','2026-W44','2026-11-01 23:59:59',10,1,100)");
 $admin->exec("INSERT INTO `$source`.permisos_laborales (id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,estado) VALUES (2,1,'Permiso','2026-10-27','2026-10-27','Aprobado')");
 check(api('/kpi/mis-kpis',null,$token)['code']===200
       && $db->query('SELECT estado FROM wp_kpi_evaluaciones WHERE id_evaluacion=900001')->fetchColumn()==='Pendiente',
       'one approved day does not exempt a whole weekly KPI');
 $admin->exec("DELETE FROM `$source`.permisos_laborales WHERE id_permiso_laboral=2");
 $db->exec('DELETE FROM wp_kpi_evaluaciones WHERE id_evaluacion=900001');
 $db->exec("INSERT INTO wp_usuario_permisos (id_usuario,id_permiso,alcance,efecto,motivo) SELECT 1,id_permiso,'PROPIO','DENEGAR','Synthetic deny fixture' FROM wp_permisos WHERE codigo='kpi.ver'");
 check(api('/kpi/mis-kpis',null,$token)['code']===403,'explicit deny overrides role grant');
 check(api('/auth/me',null,$token)['code']===200,'HTTP profile JSON');
 $admin->exec("INSERT INTO `$source`.asistencias (id_trabajador,fecha,hora_entrada,hora_salida,estado_asistencia)
     VALUES (1,'2026-11-20','08:03:00','13:31:00','Tardanza'),(2,'2026-11-20','09:00:00','17:00:00','Puntual')");
 $ownAttendance=api('/asistencias/mias?mes=2026-11',null,$token);
 check($ownAttendance['code']===200
       && $ownAttendance['data']['resumen']['dias_con_marcacion']===1
       && $ownAttendance['data']['resumen']['dias_con_tardanza']===1
       && count($ownAttendance['data']['registros'])===1
       && $ownAttendance['data']['registros'][0]['entrada']==='08:03:00',
       'own attendance reads only source records for the authenticated worker');
 check(api('/asistencias/mias?mes=2026-13',null,$token)['code']===422,
       'own attendance rejects an invalid month');
 check(api('/dashboard/resumen',null,$token)['code']===403,'HTTP dashboard forbidden');
 check(api('/personal/2',null,$token)['code']===403,'HTTP other worker forbidden');
 check(api('/actividades',['titulo'=>'Synthetic','descripcion'=>'Fixture only'],$token)['code']===201,'HTTP creates app activity');
 $admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=1");
 check(api('/actividades',['titulo'=>'Blocked','descripcion'=>'Fixture'],$token)['code']===401,'HTTP inactive protected write denied');
 check(api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!'])['code']===403,'HTTP inactive login denied');
 $admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=1");
 $login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);$token=$login['data']['auth']['access_token'];
 check(count(api('/actividades',null,$token)['data']['actividades'])===1,'reactivation retains prior activity history');
 check(api('/auth/logout',[],$token)['code']===200 && api('/auth/me',null,$token)['code']===401,'HTTP logout revokes access');
 $login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);
 $refreshed=api('/auth/refresh',['refresh_token'=>$login['data']['auth']['refresh_token']]);
 check($refreshed['code']===200 && api('/auth/me',null,$refreshed['data']['access_token'])['code']===200,'HTTP refresh endpoint available');
 $transition=['tipo_evento'=>'DESVINCULACION','motivo'=>'Synthetic transition only','fecha_efectiva'=>date('Y-m-d')];
 check(api('/personal/1/transicion',$transition,$adminToken)['code']===200,'HTTP termination transition available');
 $transition['tipo_evento']='REINGRESO';
 check(api('/personal/1/transicion',$transition,$adminToken)['code']===200,'HTTP reentry transition available');
 $login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);$token=$login['data']['auth']['access_token'];
 $db->exec('UPDATE wp_dispositivos SET estado="REVOCADO" WHERE id_usuario=1');
 check(api('/auth/me',null,$token)['code']===401,'revoked device invalidates attached session');
 check(api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!'])['code']===403,'revoked device cannot login under same UID');
 $db->exec('UPDATE wp_dispositivos SET estado="ACTIVO" WHERE id_usuario=1');
 $login=api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!']);$token=$login['data']['auth']['access_token'];
 $db->exec('UPDATE wp_situacion_laboral SET situacion="DESVINCULADO" WHERE id_trabajador=1');
 check(api('/auth/me',null,$token)['code']===401 && api('/auth/login',['username'=>'fixture','password'=>'Synthetic-only-123!'])['code']===403,'termination revokes access and login');
 check((int)$db->query('SELECT COUNT(*) FROM wp_actividades WHERE id_trabajador=1')->fetchColumn()===1 && (int)$db->query('SELECT COUNT(*) FROM wp_kpi_evaluaciones WHERE id_trabajador=1')->fetchColumn()===3,'termination retains activity and KPI history');
 $db->exec('UPDATE wp_situacion_laboral SET situacion="VIGENTE" WHERE id_trabajador=1');
 $weekDay=new DateTimeImmutable('today');
 $weekMonday=$weekDay->modify('monday this week')->format('Y-m-d');
 $weekSaturday=$weekDay->modify('saturday this week')->format('Y-m-d');
 $weekKey=$weekDay->format('o-\\WW');
 $st=$admin->prepare("INSERT INTO `$source`.permisos_laborales (id_permiso_laboral,id_trabajador,tipo_permiso,fecha_inicio,fecha_fin,estado) VALUES (101,1,'Vacaciones',?,?,'Aprobado')");
 $st->execute([$weekMonday,$weekMonday]);
 $st=$db->prepare('INSERT INTO wp_kpi_evaluaciones (id_evaluacion,id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,estado,meta_snapshot,peso_snapshot,puntos_base_snapshot,valor_reportado) VALUES (900002,1,1,1,"SEMANAL",?,? ,"Entregado",10,1,100,5)');
 $st->execute([$weekKey,$weekSaturday.' 23:59:59']);
 $rated=api('/kpi/evaluaciones/900002/calificar',['estado'=>'Observado','observaciones_supervisor'=>'Synthetic attendance adjustment'],$adminToken);
 $adjusted=$db->query('SELECT meta_efectiva_snapshot,turnos_programados_snapshot,turnos_exigibles_snapshot,porcentaje_cumplimiento FROM wp_kpi_evaluaciones WHERE id_evaluacion=900002')->fetch(PDO::FETCH_ASSOC);
 check($rated['code']===200 && (int)$adjusted['turnos_programados_snapshot']===12
       && (int)$adjusted['turnos_exigibles_snapshot']===10
       && (float)$adjusted['meta_efectiva_snapshot']===8.33
       && (float)$adjusted['porcentaje_cumplimiento']===60.02,
       'weekly numeric KPI target is prorated by eligible shifts, with frozen evidence');
 $db->exec('INSERT INTO wp_kpi_evaluaciones (id_evaluacion,id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,estado,meta_snapshot,peso_snapshot,puntos_base_snapshot,valor_reportado)
     VALUES (900003,1,1,1,"SEMANAL","2025-W52","2025-12-28 23:59:59","Entregado",10,1,100,5)');
 $historicalGrade=api('/kpi/evaluaciones/900003/calificar',['estado'=>'Aprobado'],$adminToken);
 check($historicalGrade['code']===409 && ($historicalGrade['error_code']??'')==='LABOR_HISTORY_UNVERIFIED'
       && $db->query('SELECT estado FROM wp_kpi_evaluaciones WHERE id_evaluacion=900003')->fetchColumn()==='Entregado',
       'official grading refuses to infer labor type before recorded history');
 $admin->exec("DELETE FROM `$source`.permisos_laborales WHERE id_permiso_laboral=101");
 require __DIR__.'/android_access_cases.php';
 require __DIR__.'/face_auth_cases.php';
 require __DIR__.'/face_enrollment_cases.php';
 require __DIR__.'/face_permission_grant_review_cases.php';
 require __DIR__.'/face_registration_diagnostics_cases.php';
 require __DIR__.'/face_device_cases.php';
 require __DIR__.'/face_self_service_cases.php';
 require __DIR__.'/face_sql_update_cases.php';
 require __DIR__.'/face_v011_upgrade_cases.php';
 require __DIR__.'/employee_labor_cases.php';
 require __DIR__.'/employee_observation_cases.php';
 require __DIR__.'/employee_final_cases.php';
 // A user created later in Asistencias must appear without copying the account into WebSite+.
 $admin->exec("INSERT INTO `$source`.trabajadores
     (id_trabajador,dni,nombres,apellidos,fecha_nacimiento,id_cargo,id_agencia,tipo,fecha_inicio,estado)
     VALUES (900,'00000900','New','Fixture','2000-01-01',4,1,'Colaborador','2026-11-18','Activo')");
 $newAccount=$admin->prepare("INSERT INTO `$source`.usuarios
     (id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password)
     VALUES (900,'new-fixture',?,5,900,0)");
 $newAccount->execute([$hash]);
 $newLogin=api('/auth/login',['username'=>'new-fixture','password'=>'Synthetic-only-123!']);
 check($newLogin['code']===200 && (int)$newLogin['data']['worker']['id_trabajador']===900,
     'new attendance account is immediately usable without app account duplication');
 $newToken=$newLogin['data']['auth']['access_token'];
 $admin->exec("UPDATE `$source`.trabajadores SET estado='Inactivo' WHERE id_trabajador=900");
 check(api('/auth/me',null,$newToken)['code']===401
     && api('/auth/login',['username'=>'new-fixture','password'=>'Synthetic-only-123!'])['code']===403,
     'attendance inactivation blocks existing session and new login');
 $admin->exec("UPDATE `$source`.trabajadores SET estado='Activo' WHERE id_trabajador=900");
 check(api('/auth/login',['username'=>'new-fixture','password'=>'Synthetic-only-123!'])['code']===200,
     'attendance reactivation restores login without recreating app account');
 // The active month starts with its own approved points, while finished months are archived.
 $monthNow=date('Y-m');
 $db->prepare('INSERT INTO wp_kpi_asignaciones (id_trabajador,id_kpi,fecha_inicio,asignado_por)
     VALUES (900,1,?,2)')->execute([$monthNow.'-01']);
 $liveAssignment=(int)$db->lastInsertId();
 $insertForm=$db->prepare('INSERT INTO wp_kpi_formularios_detalle
     (id_trabajador,id_usuario,id_cargo,tipo_formulario,registro_uid,revision,payload_json,payload_sha256,creado_en)
     VALUES (900,900,4,"PROGRAMACION",?,1,"{}",?,?)');
 $insertEvaluation=$db->prepare('INSERT INTO wp_kpi_evaluaciones
     (id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,estado,
      meta_snapshot,peso_snapshot,puntos_base_snapshot,valor_reportado,
      porcentaje_cumplimiento,puntos_otorgados,fecha_calificacion)
     VALUES (?,900,1,"MENSUAL",?,? ,"Aprobado",100,1,20,50,50,12,NOW())');
 $linkEvaluation=$db->prepare('INSERT INTO wp_kpi_reporte_evaluaciones
     (id_detalle,id_evaluacion,payload_sha256,fecha_evaluada,vinculado_por)
     VALUES (?,?,?,?,2)');
 $payloadHash=hash('sha256','{}');
 $insertForm->execute(['live-rank-synthetic',$payloadHash,date('Y-m-d H:i:s')]);
 $liveForm=(int)$db->lastInsertId();
 $insertEvaluation->execute([$liveAssignment,$monthNow,date('Y-m-t 23:59:59')]);
 $linkEvaluation->execute([$liveForm,(int)$db->lastInsertId(),$payloadHash,date('Y-m-d')]);
 $liveRank=api('/ranking',null,$adminToken);
 check($liveRank['code']===200 && $liveRank['data']['periodo_clave']===$monthNow
       && count($liveRank['data']['ranking'])===1
       && (int)$liveRank['data']['ranking'][0]['id_trabajador']===900
       && (int)$liveRank['data']['ranking'][0]['puntos_kpi_periodo']===12,
       'current month ranking uses only its own approved KPI points');
 $archiveMonth='2024-06';
 $insertForm->execute(['pending-rank-synthetic',$payloadHash,'2024-05-10 12:00:00']);
 $pendingForm=(int)$db->lastInsertId();
 $db->prepare('INSERT INTO wp_kpi_evaluaciones
     (id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,estado,
      meta_snapshot,peso_snapshot,puntos_base_snapshot,valor_reportado)
     VALUES (?,900,1,"MENSUAL","2024-05","2024-05-31 23:59:59","Entregado",100,1,20,50)')
     ->execute([$liveAssignment]);
 $linkEvaluation->execute([$pendingForm,(int)$db->lastInsertId(),$payloadHash,'2024-05-10']);
 $insertForm->execute(['archive-rank-synthetic',$payloadHash,'2024-06-10 12:00:00']);
 $archiveForm=(int)$db->lastInsertId();
 $insertEvaluation->execute([$liveAssignment,$archiveMonth,'2024-06-30 23:59:59']);
 $linkEvaluation->execute([$archiveForm,(int)$db->lastInsertId(),$payloadHash,'2024-06-10']);
 api('/ranking/periodos',null,$adminToken);
 check((int)$db->query("SELECT COUNT(*) FROM wp_ranking_cierres WHERE periodo_clave='2024-06'")->fetchColumn()===1
       && (int)$db->query("SELECT COUNT(*) FROM wp_ranking_historico WHERE periodo_clave='2024-06' AND id_trabajador=900")->fetchColumn()===1,
       'finished month with resolved reports is archived automatically and keeps its history');
 check((int)$db->query("SELECT COUNT(*) FROM wp_ranking_cierres WHERE periodo_clave='2024-05'")->fetchColumn()===0,
       'automatic closure waits for pending KPI review');
 require __DIR__.'/shared_network_rate_cases.php';
 if(getenv('WSP_NATIVE_PUBLIC'))require __DIR__.'/face_native_fixture.php';
} finally { proc_terminate($server); proc_close($server); }
echo "RESULT: $passed passed. Synthetic schemas retained for inspection; no production access.\n";
