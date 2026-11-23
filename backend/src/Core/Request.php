<?php
declare(strict_types=1);

namespace WebSitePlus\Core;

final class Request
{
    private string $method;
    private string $path;
    private array $headers;
    private array $queryParams;
    private array $body;
    private string $ip;

    public function __construct()
    {
        $this->method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');
        
        $uri = $_SERVER['REQUEST_URI'] ?? '/';
        $parsed = parse_url($uri);
        $this->path = $parsed['path'] ?? '/';
        $this->queryParams = $_GET;

        // Limpiar subcarpeta si se invoca en subdirectorio
        $scriptName = dirname($_SERVER['SCRIPT_NAME'] ?? '');
        if ($scriptName !== '/' && $scriptName !== '\\' && str_starts_with($this->path, $scriptName)) {
            $this->path = substr($this->path, strlen($scriptName));
            if ($this->path === '' || $this->path === false) {
                $this->path = '/';
            }
        }

        // Obtener cabeceras
        $this->headers = [];
        if (function_exists('getallheaders')) {
            foreach (getallheaders() as $k => $v) {
                $this->headers[strtolower($k)] = $v;
            }
        } else {
            foreach ($_SERVER as $k => $v) {
                if (str_starts_with($k, 'HTTP_')) {
                    $header = strtolower(str_replace('_', '-', substr($k, 5)));
                    $this->headers[$header] = $v;
                }
            }
        }
        // En cPanel (PHP-FPM/CGI) Authorization puede no aparecer en getallheaders(),
        // o venir vacio en HTTP_AUTHORIZATION mientras el valor real esta en REDIRECT_HTTP_AUTHORIZATION.
        $currentAuth = trim((string)($this->headers['authorization'] ?? ''));
        if ($currentAuth === '') {
            $candidates = [
                $_SERVER['HTTP_AUTHORIZATION'] ?? null,
                $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null,
                $_SERVER['REDIRECT_REDIRECT_HTTP_AUTHORIZATION'] ?? null,
            ];
            foreach ($candidates as $cand) {
                if (is_string($cand) && trim($cand) !== '') {
                    $this->headers['authorization'] = trim($cand);
                    break;
                }
            }
        }

        // IP del cliente (con soporte de proxies si se requiere)
        $this->ip = $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';

        // Parsear JSON body
        $this->body = [];
        $raw = file_get_contents('php://input');
        if (!empty($raw)) {
            if (strlen($raw) > 1048576) { Response::error('Cuerpo demasiado grande', 413); }
            $json = json_decode($raw, true);
            if (json_last_error() !== JSON_ERROR_NONE || !is_array($json) || !str_starts_with(ltrim($raw), '{')) {
                Response::error('Se requiere un objeto JSON valido', 400, 'INVALID_JSON');
            }
            $this->body = $json;
        }
        if ($this->method === 'POST' && empty($this->body) && !empty($_POST)) {
            $this->body = $_POST;
        }
    }

    public function getMethod(): string { return $this->method; }
    public function getPath(): string { return rtrim($this->path, '/') ?: '/'; }
    public function getIp(): string { return $this->ip; }
    public function getQueryParams(): array { return $this->queryParams; }
    public function getQuery(string $key, mixed $default = null): mixed
    {
        $value=$this->queryParams[$key] ?? $default;
        if (is_array($value)) { Response::error('Parametro invalido: '.$key,422); }
        return $value;
    }
    public function getBody(): array { return $this->body; }
    public function input(string $key, mixed $default = null): mixed
    {
        $value=$this->body[$key] ?? $default;
        if (is_array($value)) { Response::error('Parametro invalido: '.$key,422); }
        return $value;
    }

    public function getHeader(string $name): ?string
    {
        return $this->headers[strtolower($name)] ?? null;
    }

    public function getBearerToken(): ?string
    {
        $auth = $this->getHeader('authorization');
        if ($auth && preg_match('/^Bearer\s+([a-f0-9]{64})$/iD', trim($auth), $matches)) {
            return $matches[1];
        }
        return null;
    }

    public function getDeviceUid(): ?string
    {
        return $this->getHeader('x-device-uid');
    }
}
