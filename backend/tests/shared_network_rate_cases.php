<?php
declare(strict_types=1);
use WebSitePlus\Auth\RateLimiter;
// Included only after integration.php has created an isolated synthetic database.
if (!isset($db, $source, $suffix) || !preg_match('/^audit[a-f0-9]{6}_asistencia_test$/D', $source)) {
    throw new RuntimeException('Isolated harness required');
}

$db->exec('UPDATE wp_intentos_login SET creado_en=DATE_SUB(NOW(),INTERVAL 1 DAY)');
$sharedIp='127.0.0.1';
for($i=0;$i<5;$i++)RateLimiter::recordAttempt('synthetic-worker-a',$sharedIp,false,'SYNTHETIC_BAD_PASSWORD');
check(!RateLimiter::checkLoginAttempts('synthetic-worker-a',$sharedIp),'Five failures still block the same worker');
check(RateLimiter::checkLoginAttempts('synthetic-worker-b',$sharedIp),'Another worker at the same office IP stays available');
for($i=0;$i<5;$i++)RateLimiter::recordAttempt('synthetic-worker-b',$sharedIp,true);
check(RateLimiter::checkLoginAttempts('synthetic-worker-b',$sharedIp),'Successful logins do not consume failed-attempt budget');
for($i=0;$i<55;$i++)RateLimiter::recordAttempt('synthetic-spray-'.$i,$sharedIp,false,'SYNTHETIC_SPRAY');
check(!RateLimiter::checkLoginAttempts('synthetic-worker-c',$sharedIp),'High-volume IP spray is still blocked');
