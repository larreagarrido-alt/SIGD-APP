<?php
declare(strict_types=1);

namespace WebSitePlus\Core;

/**
 * Autoloader PSR-4 autocontenido para WebSite+.
 * No requiere Composer en cPanel; carga clases bajo el namespace WebSitePlus\
 */
final class Autoloader
{
    private static bool $registered = false;

    public static function register(): void
    {
        if (self::$registered) {
            return;
        }

        spl_autoload_register(function (string $class): void {
            $prefix = 'WebSitePlus\\';
            $baseDir = dirname(__DIR__) . DIRECTORY_SEPARATOR;

            $len = strlen($prefix);
            if (strncmp($prefix, $class, $len) !== 0) {
                return;
            }

            $relativeClass = substr($class, $len);
            $file = $baseDir . str_replace('\\', DIRECTORY_SEPARATOR, $relativeClass) . '.php';

            if (file_exists($file)) {
                require $file;
            }
        });

        self::$registered = true;
    }
}
