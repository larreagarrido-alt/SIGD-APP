<?php
declare(strict_types=1);

/**
 * WebSite+ — Plantilla de Configuración
 *
 * INSTRUCCIONES:
 * 1. Copia este archivo como `config.php` en esta misma carpeta (backend/config/config.php).
 * 2. `config.php` está protegido por .gitignore y NUNCA se sube a GitHub.
 * 3. En cPanel, pon los datos de tu base MySQL de cPanel.
 * 4. Genera claves secretas seguras (de al menos 32 caracteres aleatorios).
 */

return [
    'app' => [
        'name'        => 'WebSite+ API',
        'env'         => 'production', // 'development' o 'production'
        'debug'       => false,          // En 'production', cambiar a false
        'url'         => 'http://localhost/KPIWeb/backend/public',
        'timezone'    => 'America/Lima',
        'storage_dir' => dirname(__DIR__) . '/storage',
    ],

    // Base de datos MariaDB / MySQL en cPanel o XAMPP
    'database' => [
        'host'     => '127.0.0.1',
        'port'     => 3306,
        'database' => 'websiteplus', // Nunca apuntar esta conexion a asistencia
        'username' => '',
        'password' => '',
        'charset'  => 'utf8mb4',
    ],

    'attendance' => [
        'host' => '127.0.0.1', 'port' => 3306,
        'database' => 'wsnet_bd_asistencia', 'username' => '', 'password' => '',
    ],

    // Seguridad, sesiones y tokens
    'security' => [
        // Flujo antiguo bloqueado. La activacion usa exclusivamente face_self_service.
        // PAD del cliente no es PAD verificado por el servidor ni equivale a huella fuerte.
        'face_local_pad_login_enabled' => false,
        // Activacion empresarial unica, nunca por trabajador. Requiere aceptacion del PAD local.
        'face_self_service' => [
            'enabled' => false,
            'local_pad_risk_accepted' => false,
            'activation_id' => '', // Valor nuevo por cada reactivacion global.
        ],
        // Piloto retirado; no se utilizan listas manuales de trabajadores ni hashes.
        'face_pilot' => [
            'enabled' => false,
        ],
        // Secreto criptográfico para firma de tokens (HMAC-SHA256). Generar con bin2hex(random_bytes(32))
        'jwt_secret'          => '',
        'access_ttl_seconds'  => 3600,       // 1 hora
        'refresh_ttl_seconds' => 2592000,    // 30 días
        'challenge_ttl_sec'   => 180,        // 3 minutos para responder desafío biométrico
        'max_login_attempts'  => 5,          // Intentos fallidos antes de bloqueo
        'lockout_minutes'     => 15,         // Minutos de bloqueo tras exceder intentos
        'max_ip_failed_attempts_per_minute' => 60, // Cortafuegos de volumen; no mezcla 5 fallos de trabajadores distintos
        'max_ip_challenges_per_minute' => 120, // Cortafuegos de volumen para redes compartidas
    ],

    // CORS para permitir acceso desde Android y PWA web
    'cors' => [
        'allowed_origins' => ['*'], // En producción, restringir a dominios autorizados
        'allowed_methods' => ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
        'allowed_headers' => ['Content-Type', 'Authorization', 'X-Requested-With', 'X-Device-Uid'],
    ],

    'premios' => [
        'max_image_bytes' => 5242880, // 5 MB; PHP upload_max_filesize/post_max_size must also permit this.
    ],

    // Proveedor de Inteligencia Artificial (futuro chatbot)
    'ai_provider' => [
        'enabled'      => false,
        'provider'     => 'gemini', // o 'openai'
        'api_key'      => '',       // NUNCA colocar en Android
        'model'        => 'gemini-1.5-flash',
        'max_tokens'   => 500,
        'daily_limit'  => 50,       // Mensajes por usuario al día
    ],
];
