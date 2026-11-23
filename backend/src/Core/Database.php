<?php
declare(strict_types=1);

namespace WebSitePlus\Core;

use PDO;
use RuntimeException;
use Throwable;

final class Database
{
    private static ?PDO $pdo = null;
    private static ?PDO $attendance = null;

    public static function getAttendanceConnection(): PDO
    {
        return self::$attendance ??= self::connect('attendance');
    }

    private static function connect(string $section): PDO
    {
        $cfg = Config::get($section);
        if (!is_array($cfg) || empty($cfg['username']) || empty($cfg['database'])) {
            throw new RuntimeException('Configure las conexiones antes de iniciar la API');
        }
        $app = Config::get('database.database');
        $source = Config::get('attendance.database');
        if (Config::get('database.host') !== Config::get('attendance.host') || Config::get('database.port') !== Config::get('attendance.port')) {
            throw new RuntimeException('Las vistas de integracion requieren ambas bases en el mismo servidor');
        }
        if ($app === $source || !preg_match('/^(?:[a-zA-Z0-9]+_)?websiteplus(?:_test)?$/D', (string) $app)) {
            throw new RuntimeException('La base destino debe ser websiteplus y distinta de asistencia');
        }
        $pdo = new PDO(sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $cfg['host'], $cfg['port'], $cfg['database']), $cfg['username'], $cfg['password'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
            PDO::MYSQL_ATTR_MULTI_STATEMENTS => false,
        ]);
        $pdo->exec("SET time_zone = '-05:00'");
        return $pdo;
    }

    public static function getConnection(): PDO
    {
        if (self::$pdo !== null) {
            return self::$pdo;
        }

        return self::$pdo = self::connect('database');
    }

    public static function setConnection(?PDO $pdo): void
    {
        self::$pdo = $pdo;
    }

    /**
     * Ejecuta un callback dentro de una transacción ACID con rollback automático ante errores.
     */
    public static function transaction(callable $callback): mixed
    {
        $pdo = self::getConnection();
        $isNested = $pdo->inTransaction();

        if (!$isNested) {
            $pdo->beginTransaction();
        }

        try {
            $result = $callback($pdo);
            if (!$isNested) {
                $pdo->commit();
            }
            return $result;
        } catch (Throwable $e) {
            if (!$isNested && $pdo->inTransaction()) {
                $pdo->rollBack();
            }
            throw $e;
        }
    }
}
