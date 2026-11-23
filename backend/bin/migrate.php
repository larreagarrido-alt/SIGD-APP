<?php
declare(strict_types=1);
// CLI only. Never import the reference schema in an existing attendance database.
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require dirname(__DIR__) . '/src/Core/Autoloader.php';
WebSitePlus\Core\Autoloader::register();
use WebSitePlus\Core\Config;
use WebSitePlus\Core\Database;
Config::load($argv[1] ?? null);
$db = Database::getConnection();
$source = Config::get('attendance.database');
if (!preg_match('/^[a-zA-Z0-9_]+$/D', $source)) { throw new RuntimeException('Invalid source schema'); }
$skipV012 = getenv('WSP_TEST_NO_V012') === '1';
if ($skipV012) {
    $dataDir = realpath((string)$db->query('SELECT @@datadir')->fetchColumn());
    $storage = realpath(dirname(__DIR__) . '/storage');
    $norm = static fn(string $p): string => strtolower(str_replace('\\', '/', rtrim($p, '\\/')));
    if (Config::get('app.env') !== 'testing' || (int)Config::get('database.port') !== 13317
        || !str_ends_with((string)Config::get('database.database'), '_websiteplus_test')
        || !str_ends_with($source, '_asistencia_test') || !$dataDir || !$storage
        || !str_starts_with($norm($dataDir), $norm($storage) . '/mysql-repro-')) {
        throw new RuntimeException('V012 omission is permitted only in the owned synthetic laboratory');
    }
}
if (!$db->query("SELECT GET_LOCK('websiteplus_migrations', 0)")->fetchColumn()) { throw new RuntimeException('Migration already running'); }
try {
    // Logical references: read-through views, no copied employees/passwords or cross-schema FKs.
    // INVOKER requires SELECT on source tables; never grant source writes to this account.
    foreach (\WebSitePlus\Core\AttendanceViews::statements($source) as $view) { $db->exec($view); }
    $db->exec('CREATE TABLE IF NOT EXISTS wp_migraciones (version VARCHAR(10) PRIMARY KEY, descripcion VARCHAR(150) NOT NULL, checksum CHAR(64) NULL, aplicada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB');
    foreach (glob(dirname(__DIR__, 2) . '/database/migrations/*.up.sql') as $file) {
        $version = substr(basename($file), 0, 4);
        if ($skipV012 && $version === 'V012') { echo "V012 absent in production-shaped laboratory\n"; continue; }
        $hash = hash_file('sha256', $file);
        $st = $db->prepare('SELECT checksum FROM wp_migraciones WHERE version = ?');
        $st->execute([$version]);
        $row = $st->fetch();
        if ($row) {
            if ($row['checksum'] !== $hash) { throw new RuntimeException("$version: checksum differs or legacy installation; review schema before adopting"); }
            echo "$version unchanged\n"; continue;
        }
        $sql = str_replace('USE `websiteplus`;', 'USE `' . Config::get('database.database') . '`;', file_get_contents($file));
        foreach (\WebSitePlus\Core\SqlScript::statements($sql) as $statement) {
            if (trim($statement) !== '') { $db->exec($statement); }
        }
        $st = $db->prepare('UPDATE wp_migraciones SET checksum = ? WHERE version = ?');
        $st->execute([$hash, $version]);
        echo "$version applied\n";
    }
} finally { $db->query("SELECT RELEASE_LOCK('websiteplus_migrations')"); }
