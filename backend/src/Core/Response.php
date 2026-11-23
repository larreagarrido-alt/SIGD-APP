<?php
declare(strict_types=1);

namespace WebSitePlus\Core;

final class Response
{
    public static function json(array $data, int $status = 200, array $headers = []): void
    {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');

        // Headers CORS y de seguridad
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('X-XSS-Protection: 1; mode=block');

        foreach ($headers as $k => $v) {
            header("{$k}: {$v}");
        }

        echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        exit;
    }

    public static function success(mixed $data = null, string $message = 'Operacion exitosa', int $status = 200): void
    {
        $response = [
            'status'  => 'success',
            'code'    => $status,
            'message' => $message,
            'data'    => $data,
        ];
        self::json($response, $status);
    }

    public static function error(string $message, int $status = 400, ?string $errorCode = null, mixed $errors = null): void
    {
        $response = [
            'status'     => 'error',
            'code'       => $status,
            'error_code' => $errorCode ?? 'ERROR_' . $status,
            'message'    => $message,
        ];

        if ($errors !== null) {
            $response['errors'] = $errors;
        }

        self::json($response, $status);
    }
}
