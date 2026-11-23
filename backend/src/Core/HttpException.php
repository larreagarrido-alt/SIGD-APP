<?php
declare(strict_types=1);
namespace WebSitePlus\Core;
final class HttpException extends \RuntimeException
{
    public function __construct(string $message, public readonly int $status = 409,
                                public readonly string $errorCode = 'STATE_CONFLICT')
    {
        parent::__construct($message);
    }
}
