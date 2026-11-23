<?php
declare(strict_types=1);

namespace WebSitePlus\Core;

final class Config
{
    private static ?array $data = null;

    public static function get(string $key, mixed $default = null): mixed
    {
        if (self::$data === null) {
            self::load();
        }

        $segments = explode('.', $key);
        $current = self::$data;

        foreach ($segments as $segment) {
            if (!is_array($current) || !array_key_exists($segment, $current)) {
                return $default;
            }
            $current = $current[$segment];
        }

        return $current;
    }

    public static function set(string $key, mixed $value): void
    {
        if (self::$data === null) {
            self::load();
        }
        $segments = explode('.', $key);
        $current = &self::$data;
        foreach ($segments as $segment) {
            if (!isset($current[$segment]) || !is_array($current[$segment])) {
                $current[$segment] = [];
            }
            $current = &$current[$segment];
        }
        $current = $value;
    }

    public static function load(?string $customPath = null): void
    {
        $baseDir = dirname(__DIR__, 2) . '/config';
        $realConfig = $customPath ?? (getenv('WSP_CONFIG_PATH') ?: ($baseDir . '/config.php'));
        $exampleConfig = $baseDir . '/config.example.php';

        if (file_exists($realConfig)) {
            self::$data = require $realConfig;
        } elseif (file_exists($exampleConfig)) {
            self::$data = require $exampleConfig;
        } else {
            self::$data = [];
        }
    }
}
