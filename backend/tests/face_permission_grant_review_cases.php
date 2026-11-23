<?php
declare(strict_types=1);
// Test the proposed SQL only against owned random schemas and wholly synthetic workers.
if(!isset($admin,$db,$app,$source)||!preg_match('/^audit[a-f0-9]{6}_websiteplus_test$/D',$app)
    ||!preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D',$source))throw new RuntimeException('Synthetic owned harness required');
$admin->exec("INSERT INTO `$source`.roles(id_rol,nombre) VALUES(6,'Synthetic permission review role')");
$admin->exec("INSERT INTO `$source`.trabajadores(id_trabajador,dni,nombres,apellidos,fecha_nacimiento,id_cargo,id_agencia,tipo,fecha_inicio,estado) VALUES(12,'00000012','Synthetic','Permission Review','2000-01-01',1,1,'Colaborador','2026-01-01','Activo')");
$admin->prepare("INSERT INTO `$source`.usuarios(id_usuario,username,password,id_rol,id_trabajador,debe_cambiar_password) VALUES(12,'fixture-grant-u12',?,6,12,0)")->execute([password_hash('Synthetic-only-grant-123!',PASSWORD_DEFAULT)]);
$user=['id_usuario'=>12,'id_rol'=>6,'id_trabajador'=>12];
$rolesBefore=$db->query('SELECT * FROM wp_rol_permisos ORDER BY id_rol,id_permiso')->fetchAll();
$othersBefore=$db->query('SELECT * FROM wp_usuario_permisos WHERE id_usuario<>12 ORDER BY id_usuario,id_permiso')->fetchAll();
$run=static function(string $file)use($admin,$app):int{
    $sql=file_get_contents(__DIR__.'/fixtures/'.$file);
    $sql=str_replace('USE `synthetic_websiteplus_test`;','USE `'.$app.'`;',$sql);
    $affected=-1;
    foreach(WebSitePlus\Core\SqlScript::statements($sql) as $statement){
        $st=$admin->query($statement);
        if($st->columnCount()>0){foreach($st->fetchAll(PDO::FETCH_ASSOC) as $row){if(isset($row['permisos_insertados']))$affected=(int)$row['permisos_insertados'];if(isset($row['permisos_retirados']))$affected=(int)$row['permisos_retirados'];}}
        $st->closeCursor();
    }
    return $affected;
};
$grant='auth_perfil_usuario12_propuesto.sql';$revert='auth_perfil_usuario12_reversion.sql';
check(!WebSitePlus\Auth\Authorization::allows($user,'auth.perfil',12),'Review fixture mirrors missing auth.perfil for user12 role6');
$db->exec("INSERT INTO wp_usuario_permisos(id_usuario,id_permiso,efecto,alcance,motivo) SELECT 12,id_permiso,'DENEGAR','PROPIO','Synthetic preexisting refusal' FROM wp_permisos WHERE codigo='auth.perfil'");
check($run($grant)===0,'Proposed grant refuses to overwrite existing individual DENEGAR');
check(!$run($revert)&&!WebSitePlus\Auth\Authorization::allows($user,'auth.perfil',12),'Reversal cannot delete a preexisting denial');
$db->exec("DELETE FROM wp_usuario_permisos WHERE id_usuario=12 AND motivo='Synthetic preexisting refusal'");
check($run($grant)===1,'Exact proposed guarded INSERT creates one individual grant in synthetic schema');
$effective=WebSitePlus\Auth\Authorization::permissions($user);
check(count($effective)===2 && ($effective['auth.perfil']??null)==='PROPIO'
    && ($effective['bienestar.responder']??null)==='PROPIO',
    'Individual profile grant preserves the reviewed own-wellness role permission without administrative access');
check(WebSitePlus\Auth\Authorization::allows($user,'auth.perfil',12),'PROPIO authorizes own worker profile/security');
check(!WebSitePlus\Auth\Authorization::allows($user,'auth.perfil',1),'PROPIO refuses another worker even in the same agency');
check($db->query('SELECT * FROM wp_rol_permisos ORDER BY id_rol,id_permiso')->fetchAll()===$rolesBefore,'Proposed SQL never changes role6 or any role grant');
check($db->query('SELECT * FROM wp_usuario_permisos WHERE id_usuario<>12 ORDER BY id_usuario,id_permiso')->fetchAll()===$othersBefore,'Proposed SQL never changes other individual accounts');
check($run($grant)===0,'Repeated proposal inserts zero rows without overwriting existing grant');
check($run($revert)===1&&!WebSitePlus\Auth\Authorization::allows($user,'auth.perfil',12),'Specific reversal removes only the proposal and restores absence of permission');
check($run($revert)===0,'Repeated reversal removes zero additional rows');
$admin->exec("UPDATE `$source`.usuarios SET id_rol=5 WHERE id_usuario=12");
try{check($run($grant)===0,'Proposal refuses a user whose role no longer matches reviewed role6');}
finally{$admin->exec("UPDATE `$source`.usuarios SET id_rol=6 WHERE id_usuario=12");}
check(!WebSitePlus\Auth\FaceCredentialService::localLoginEnabled(),'Permission proposal never enables FACIAL sessions');
