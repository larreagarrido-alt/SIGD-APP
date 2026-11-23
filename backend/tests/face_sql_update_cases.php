<?php
declare(strict_types=1);
// Test the exact cPanel import files in a NEW synthetic schema, never a real database.
if(!isset($admin,$suffix)||!preg_match('/^[a-f0-9]{6}$/D',$suffix))throw new RuntimeException('Isolated harness required');
$sqlLab='sql'.$suffix.'_websiteplus_test';$admin->exec("CREATE DATABASE `$sqlLab` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
$admin->exec("USE `$sqlLab`");
$admin->exec('CREATE TABLE wp_accesos_android(id_acceso INT UNSIGNED PRIMARY KEY) ENGINE=InnoDB');
$admin->exec("CREATE TABLE wp_sesiones(id_sesion BIGINT UNSIGNED PRIMARY KEY,metodo ENUM('PASSWORD','BIOMETRIA') NOT NULL) ENGINE=InnoDB");
$admin->exec('CREATE TABLE wp_migraciones(version VARCHAR(10) PRIMARY KEY,descripcion VARCHAR(150),checksum CHAR(64)) ENGINE=InnoDB');
foreach(['V013','V012','V014'] as $version){
    $script=file_get_contents(dirname(__DIR__,2).'/database/actualizacion_wsnet_websiteplus_'.$version.'.sql');
    $script=str_replace('USE `wsnet_websiteplus`;','USE `'.$sqlLab.'`;',$script);
    foreach(WebSitePlus\Core\SqlScript::statements($script) as $statement)$admin->exec($statement);
    $expected=hash_file('sha256',glob(dirname(__DIR__,2).'/database/migrations/'.$version.'__*.up.sql')[0]);
    check($admin->query("SELECT checksum FROM wp_migraciones WHERE version='$version'")->fetchColumn()===$expected,'Exact cPanel '.$version.' import installs matching checksum');
    if($version==='V013')check((int)$admin->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$sqlLab' AND table_name='wp_rostros_cuenta'")->fetchColumn()===0,'Case A V013 import needs no V012 enrollment table');
    // Exact repeated import is idempotent on this newly verified schema.
    foreach(WebSitePlus\Core\SqlScript::statements($script) as $statement)$admin->exec($statement);
}
$admin->exec("USE `$source`");
