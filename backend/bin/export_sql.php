<?php
declare(strict_types=1);
// Offline bundle for a NEW EMPTY database. Existing installations must use migrate.php.
if(PHP_SAPI!=='cli') { exit(1); }
require dirname(__DIR__).'/src/Core/Autoloader.php';
WebSitePlus\Core\Autoloader::register();
$target=$argv[1]??''; $source=$argv[2]??''; $output=$argv[3]??'';
if(!preg_match('/^(?:[a-zA-Z0-9]+_)?websiteplus(?:_test)?$/D',$target) || $target===$source || !$output) {
    throw new RuntimeException('Usage: php export_sql.php account_websiteplus account_attendance output.sql');
}
$sql="-- NEW EMPTY DATABASE ONLY. Existing installations: use migrate.php.\nUSE `$target`;\n";
foreach(WebSitePlus\Core\AttendanceViews::statements($source) as $view) { $sql.=$view.";\n"; }
foreach(glob(dirname(__DIR__,2).'/database/migrations/*.up.sql') as $file) {
    $sql.=str_replace('USE `websiteplus`;',"USE `$target`;",file_get_contents($file))."\n";
    $version=substr(basename($file),0,4);$hash=hash_file('sha256',$file);
    $sql.="UPDATE wp_migraciones SET checksum='$hash' WHERE version='$version';\n";
}
if(file_exists($output)) { throw new RuntimeException('Output already exists; use a new filename'); }
if(file_put_contents($output,$sql)===false) { throw new RuntimeException('Cannot write bundle'); }
echo "Generated $output for $target\n";
