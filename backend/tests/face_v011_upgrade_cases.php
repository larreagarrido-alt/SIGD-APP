<?php
declare(strict_types=1);
// Exact combined import, populated V011-shaped synthetic tables, no production connection.
if(!isset($admin,$app,$suffix)||!preg_match('/^audit[a-f0-9]{6}_websiteplus_test$/D',$app))throw new RuntimeException('Synthetic harness required');
$upgradeLab='upgrade'.$suffix.'_websiteplus_test';
$admin->exec("CREATE DATABASE `$upgradeLab` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
foreach(['wp_migraciones','wp_accesos_android','wp_sesiones'] as $table)$admin->exec("CREATE TABLE `$upgradeLab`.`$table` LIKE `$app`.`$table`");
$admin->exec("ALTER TABLE `$upgradeLab`.wp_sesiones MODIFY metodo ENUM('PASSWORD','BIOMETRIA') NOT NULL");
$admin->exec("INSERT INTO `$upgradeLab`.wp_migraciones SELECT * FROM `$app`.wp_migraciones WHERE CAST(SUBSTRING(version,2) AS UNSIGNED)<=11");
$columns=$admin->query("SELECT COLUMN_NAME FROM information_schema.columns WHERE table_schema='$app' AND table_name='wp_accesos_android' AND EXTRA NOT LIKE '%GENERATED%' ORDER BY ORDINAL_POSITION")->fetchAll(PDO::FETCH_COLUMN);
$columns=implode(',',array_map(static fn(string $name):string=>'`'.$name.'`',$columns));
$admin->exec("INSERT INTO `$upgradeLab`.wp_accesos_android ($columns) SELECT $columns FROM `$app`.wp_accesos_android");
$admin->exec("INSERT INTO `$upgradeLab`.wp_sesiones SELECT * FROM `$app`.wp_sesiones WHERE metodo IN ('PASSWORD','BIOMETRIA')");
$beforeAndroid=$admin->query("SELECT * FROM `$upgradeLab`.wp_accesos_android ORDER BY id_acceso")->fetchAll(PDO::FETCH_ASSOC);
$beforeSessions=$admin->query("SELECT * FROM `$upgradeLab`.wp_sesiones ORDER BY id_sesion")->fetchAll(PDO::FETCH_ASSOC);
$import=file_get_contents(dirname(__DIR__,2).'/database/actualizacion_wsnet_websiteplus_V011_a_V014.sql');
$apply=function(string $schema)use($admin,$import){
    $sql=str_replace('wsnet_websiteplus',$schema,$import);
    foreach(WebSitePlus\Core\SqlScript::statements($sql) as $statement){
        $cursor=$admin->query($statement);
        if($cursor!==false)$cursor->closeCursor();
    }
};
$apply($upgradeLab);
foreach(['V013','V014'] as $version){
    $actual=$admin->query("SELECT checksum FROM `$upgradeLab`.wp_migraciones WHERE version='$version'")->fetchColumn();
    check($actual===hash_file('sha256',glob(dirname(__DIR__,2).'/database/migrations/'.$version.'__*.up.sql')[0]),'Combined V011 upgrade installs exact '.$version.' checksum');
}
check($admin->query("SELECT * FROM `$upgradeLab`.wp_accesos_android ORDER BY id_acceso")->fetchAll(PDO::FETCH_ASSOC)===$beforeAndroid,'Combined upgrade preserves every V011 credential row');
check($admin->query("SELECT * FROM `$upgradeLab`.wp_sesiones ORDER BY id_sesion")->fetchAll(PDO::FETCH_ASSOC)===$beforeSessions,'Combined upgrade preserves existing password and fingerprint sessions');
check((int)$admin->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$upgradeLab' AND table_name='wp_rostros_cuenta'")->fetchColumn()===0
    &&(int)$admin->query("SELECT COUNT(*) FROM `$upgradeLab`.wp_migraciones WHERE version='V012'")->fetchColumn()===0,'Combined upgrade does not install optional V012');
$apply($upgradeLab);
check($admin->query("SELECT * FROM `$upgradeLab`.wp_sesiones ORDER BY id_sesion")->fetchAll(PDO::FETCH_ASSOC)===$beforeSessions,'Repeated combined import preserves existing sessions');
$future="enum('PASSWORD','BIOMETRIA','FACIAL','FUTURE_METHOD')";
$admin->exec("ALTER TABLE `$upgradeLab`.wp_sesiones MODIFY metodo $future NOT NULL");
$admin->exec("INSERT INTO `$upgradeLab`.wp_migraciones(version,descripcion) VALUES('V015','Synthetic future schema')");
$apply($upgradeLab);
check($admin->query("SELECT COLUMN_TYPE FROM information_schema.columns WHERE table_schema='$upgradeLab' AND table_name='wp_sesiones' AND column_name='metodo'")->fetchColumn()===$future,'Combined upgrade refuses to downgrade a future schema');
$emptyLab='empty'.$suffix.'_websiteplus_test';
$admin->exec("CREATE DATABASE `$emptyLab` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
$admin->exec("CREATE TABLE `$emptyLab`.wp_migraciones LIKE `$app`.wp_migraciones");
$apply($emptyLab);
check((int)$admin->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$emptyLab'")->fetchColumn()===1
    &&(int)$admin->query("SELECT COUNT(*) FROM `$emptyLab`.wp_migraciones")->fetchColumn()===0,'Combined upgrade does not install over a schema without V011');
$admin->exec("USE `$app`");
