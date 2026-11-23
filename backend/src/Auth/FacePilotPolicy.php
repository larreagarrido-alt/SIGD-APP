<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
/** Retired pilot: compatibility shim; mixed deployments must fail closed. */
final class FacePilotPolicy
{
    public static function settings(): ?array {return null;}
    public static function grant(array $key,array $device): ?array {return null;}
    public static function context(int $user,int $android,int $key): ?array {return null;}
    public static function sessionAllowed(array $session): bool {return false;}
}
