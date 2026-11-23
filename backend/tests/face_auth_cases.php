<?php
declare(strict_types=1);

// Included by integration.php
if (!isset($db, $source, $suffix)) { throw new RuntimeException('Isolated harness required'); }

use WebSitePlus\Core\Config;

$faceSessionCountBefore = (int) $db->query('SELECT COUNT(*) FROM wp_sesiones')->fetchColumn();

// Helper to generate EC P-256 keys portably
$cnf = __DIR__.'/fixtures/openssl.cnf';
$createEcKey = function() use ($cnf): array {
    $args = ['private_key_type' => OPENSSL_KEYTYPE_EC, 'curve_name' => 'prime256v1'];
    if ($cnf) { $args['config'] = $cnf; }
    $key = openssl_pkey_new($args);
    if (!$key) { throw new RuntimeException('Cannot create synthetic EC key'); }
    $details = openssl_pkey_get_details($key);
    return [$key, $details['key']];
};

[$devicePrivateKey, $devicePublicKeyPem] = $createEcKey();
$derPublicKey = base64_decode(preg_replace('/-----[^-]+-----|\s/', '', $devicePublicKeyPem), true);
$deviceKeyFingerprint = hash('sha256', $derPublicKey);

[$pendingPrivateKey, $pendingPublicKeyPem] = $createEcKey();
$pendingDer = base64_decode(preg_replace('/-----[^-]+-----|\s/', '', $pendingPublicKeyPem), true);
$pendingFingerprint = hash('sha256', $pendingDer);

[$revokedPrivateKey, $revokedPublicKeyPem] = $createEcKey();
$revokedDer = base64_decode(preg_replace('/-----[^-]+-----|\s/', '', $revokedPublicKeyPem), true);
$revokedFingerprint = hash('sha256', $revokedDer);

[$otherPrivateKey, $otherPublicKeyPem] = $createEcKey();
$otherDer = base64_decode(preg_replace('/-----[^-]+-----|\s/', '', $otherPublicKeyPem), true);
$otherFingerprint = hash('sha256', $otherDer);

[$foreignPrivateKey, $foreignPublicKeyPem] = $createEcKey();

// Helper function to sign a challenge message
$signMessage = function(string $message, mixed $privateKey): string {
    $rawSig = '';
    openssl_sign($message, $rawSig, $privateKey, OPENSSL_ALGO_SHA256);
    return base64_encode($rawSig);
};

// 1. Prepare synthetic worker with valid 128D face descriptor in attendance DB
$syntheticDescriptor = array_fill(0, 128, 0.05);
$norm = sqrt(array_sum(array_map(fn($v) => $v * $v, $syntheticDescriptor)));
$syntheticDescriptor = array_map(fn($v) => $v / $norm, $syntheticDescriptor);
$syntheticDescriptorJson = json_encode($syntheticDescriptor);

// Update fixture worker with descriptor in source attendance DB
$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor = ? WHERE id_trabajador = 1")->execute([$syntheticDescriptorJson]);

// Ensure worker 2 (admin) has NO descriptor
$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor = NULL WHERE id_trabajador = 2")->execute();

// Resolve fixture user details dynamically
$fixtureUser = $db->query("SELECT id_usuario, password FROM usuarios WHERE username = 'fixture'")->fetch();
$fixtureUserId = (int) $fixtureUser['id_usuario'];
$fixturePasswordHmac = hash_hmac('sha256', (string) $fixtureUser['password'], (string) Config::get('security.jwt_secret'));

$adminUser = $db->query("SELECT id_usuario, password FROM usuarios WHERE username = 'fixture-admin'")->fetch();
$adminUserId = (int) $adminUser['id_usuario'];

// Setup authorized active device for 'fixture' in wp_accesos_android using dedicated test UUIDs
$activeDeviceUid = '55555555-5555-4555-8555-555555555555';
$pendingDeviceUid = '66666666-6666-4666-8666-666666666666';
$revokedDeviceUid = '77777777-7777-4777-8777-777777777777';
$otherUserDeviceUid = '88888888-8888-4888-8888-888888888888';
$unknownDeviceUid = '99999999-9999-4999-8999-999999999999';

// Ensure prior rows with these specific test UUIDs don't block
$db->exec("DELETE FROM wp_desafios WHERE referencia_tipo = 'AUTH_FACIAL'");
$db->exec("DELETE FROM wp_accesos_android WHERE device_uid IN ('$activeDeviceUid', '$pendingDeviceUid', '$revokedDeviceUid', '$otherUserDeviceUid')");

$insSt = $db->prepare("
    INSERT INTO wp_accesos_android (id_usuario, device_uid, nombre, clave_publica, huella_clave, credencial_hmac, id_sesion_registro, estado)
    VALUES (?, ?, 'Test Pixel', ?, ?, ?, (SELECT MIN(id_sesion) FROM wp_sesiones), 'ACTIVO')
");
$insSt->execute([$fixtureUserId, $activeDeviceUid, $devicePublicKeyPem, $deviceKeyFingerprint, $fixturePasswordHmac]);
$activeAccessId = (int) $db->lastInsertId();

// Device in PENDIENTE state
$db->prepare("
    INSERT INTO wp_accesos_android (id_usuario, device_uid, nombre, clave_publica, huella_clave, credencial_hmac, id_sesion_registro, estado)
    VALUES (?, ?, 'Pending Phone', ?, ?, ?, (SELECT MIN(id_sesion) FROM wp_sesiones), 'PENDIENTE')
")->execute([$fixtureUserId, $pendingDeviceUid, $pendingPublicKeyPem, $pendingFingerprint, $fixturePasswordHmac]);

// Device in REVOCADO state
$db->prepare("
    INSERT INTO wp_accesos_android (id_usuario, device_uid, nombre, clave_publica, huella_clave, credencial_hmac, id_sesion_registro, estado)
    VALUES (?, ?, 'Revoked Phone', ?, ?, ?, (SELECT MIN(id_sesion) FROM wp_sesiones), 'REVOCADO')
")->execute([$fixtureUserId, $revokedDeviceUid, $revokedPublicKeyPem, $revokedFingerprint, $fixturePasswordHmac]);

// Device belonging to ANOTHER user
$db->prepare("
    INSERT INTO wp_accesos_android (id_usuario, device_uid, nombre, clave_publica, huella_clave, credencial_hmac, id_sesion_registro, estado)
    VALUES (?, ?, 'Admin Phone', ?, ?, ?, (SELECT MIN(id_sesion) FROM wp_sesiones), 'ACTIVO')
")->execute([$adminUserId, $otherUserDeviceUid, $otherPublicKeyPem, $otherFingerprint, $fixturePasswordHmac]);

// Reset login attempts before starting
$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');

// -----------------------------------------------------------------------------
// TEST SUITE: CHALLENGE PHASE
// -----------------------------------------------------------------------------

// 2. Challenge requires username
$res = api('/auth/face/challenge', ['username' => '', 'device_uid' => $activeDeviceUid]);
check($res['code'] === 422, 'Face challenge requires username');

// 3. Challenge requires device_uid (ERROR B Remediation)
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => '']);
check($res['code'] === 422 && $res['error_code'] === 'VALIDATION_FAILED', 'Face challenge strictly requires non-empty device_uid');

// 4. Malformed device_uid rejected
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => 'not-a-valid-uuid']);
check($res['code'] === 422 && $res['error_code'] === 'VALIDATION_FAILED', 'Malformed device_uid format rejected');

// 5. Unknown device UID rejected (DEVICE_NOT_FOUND)
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $unknownDeviceUid]);
check($res['code'] === 401 && $res['error_code'] === 'DEVICE_NOT_FOUND', 'Unregistered device rejected under V011 policy');

// 6. Device of another user rejected (ACCOUNT_ALREADY_LINKED)
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $otherUserDeviceUid]);
check($res['code'] === 409 && $res['error_code'] === 'ACCOUNT_ALREADY_LINKED', 'Device linked to another account rejected');

// 7. Pending device rejected (REGISTRATION_INCOMPLETE)
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $pendingDeviceUid]);
check($res['code'] === 401 && $res['error_code'] === 'REGISTRATION_INCOMPLETE', 'Pending device rejected for facial challenge');

// 8. Revoked device rejected (DEVICE_NOT_AUTHORIZED)
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $revokedDeviceUid]);
check($res['code'] === 403 && $res['error_code'] === 'DEVICE_NOT_AUTHORIZED', 'Revoked device cannot initiate face auth');

// 9. Worker without descriptor rejected
$res = api('/auth/face/challenge', ['username' => 'fixture-admin', 'device_uid' => $otherUserDeviceUid]);
check($res['code'] === 422 && $res['error_code'] === 'WORKER_WITHOUT_FACE_RECORD', 'Worker without face record rejected');

// 10. Inactive account rejected
$admin->prepare("UPDATE `$source`.trabajadores SET estado = 'Inactivo' WHERE id_trabajador = 1")->execute();
$inactiveChallenge = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
check($inactiveChallenge['code'] === 403 && $inactiveChallenge['error_code'] === 'ACCOUNT_INACTIVE', 'Inactive worker rejected for face auth');
$admin->prepare("UPDATE `$source`.trabajadores SET estado = 'Activo' WHERE id_trabajador = 1")->execute();

// 11. Valid challenge issued with active authorized device
$res = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
check($res['code'] === 200 && !empty($res['data']['nonce']) && !empty($res['data']['message']), 'Valid face challenge issued for active device');
$challengeId = $res['data']['challenge_id'];
$nonce = $res['data']['nonce'];
$challengeMessage = $res['data']['message'];

// Verify challenge isolation in DB: must have proposito = 'LOGIN' and referencia_tipo = 'AUTH_FACIAL'
$stCheckCh = $db->prepare('SELECT * FROM wp_desafios WHERE id_desafio = ?');
$stCheckCh->execute([$challengeId]);
$dbChRow = $stCheckCh->fetch();
check($dbChRow['proposito'] === 'LOGIN' && $dbChRow['referencia_tipo'] === 'AUTH_FACIAL', 'Challenge strictly isolated to AUTH_FACIAL');
check((int) $dbChRow['referencia_id'] === $activeAccessId, 'Challenge bound to device access id');
check($dbChRow['hash_contenido'] === $deviceKeyFingerprint, 'Challenge bound to device key fingerprint');

// -----------------------------------------------------------------------------
// TEST SUITE: VERIFICATION PHASE & CRYPTOGRAPHIC CHECKS
// -----------------------------------------------------------------------------

// Sign valid challenge with authorized device key
$validSignature = $signMessage($challengeMessage, $devicePrivateKey);

// Sign valid challenge with foreign device key (unauthorized key)
$foreignSignature = $signMessage($challengeMessage, $foreignPrivateKey);

// 12. Verification requires signature
$res = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $challengeId,
    'nonce'        => $nonce,
    'signature'    => '',
    'face_data'    => $syntheticDescriptor,
]);
check($res['code'] === 422 && $res['error_code'] === 'VALIDATION_FAILED', 'Verification requires device cryptographic signature');

// 13. Invalid / malformed signature rejected
$res = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $challengeId,
    'nonce'        => $nonce,
    'signature'    => 'invalid_base64_or_tampered_signature',
    'face_data'    => $syntheticDescriptor,
]);
check($res['code'] === 401 && $res['error_code'] === 'INVALID_SIGNATURE', 'Malformed signature rejected with INVALID_SIGNATURE');

// 14. Signature from unauthorized key rejected
$resForeignCh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$foreignSigOnFresh = $signMessage($resForeignCh['data']['message'], $foreignPrivateKey);
$res = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resForeignCh['data']['challenge_id'],
    'nonce'        => $resForeignCh['data']['nonce'],
    'signature'    => $foreignSigOnFresh,
    'face_data'    => $syntheticDescriptor,
]);
check($res['code'] === 401 && $res['error_code'] === 'INVALID_SIGNATURE', 'Signature from foreign private key rejected');

// 15. Cross-purpose challenge isolation check (Challenge from KPI or normal login rejected)
$foreignChallenge = \WebSitePlus\Auth\ChallengeService::createChallenge($fixtureUserId, null, 'LOGIN', '127.0.0.1', 'android_acceso', $activeAccessId, $deviceKeyFingerprint);
$foreignChSig = $signMessage($foreignChallenge['message'], $devicePrivateKey);
$res = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $foreignChallenge['challenge_id'],
    'nonce'        => $foreignChallenge['nonce'],
    'signature'    => $foreignChSig,
    'face_data'    => $syntheticDescriptor,
]);
check($res['code'] === 401 && $res['error_code'] === 'CHALLENGE_EXPIRED_OR_REUSED', 'Fingerprint/KPI challenge rejected for facial authentication');

// 16. Vector schema validation: length must be 128
$resFresh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$sigFresh = $signMessage($resFresh['data']['message'], $devicePrivateKey);
$res = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => [0.1, 0.2, 0.3], // only 3 elements
]);
check($res['code'] === 422 && $res['error_code'] === 'VALIDATION_FAILED', 'Malformed vector with incorrect dimensions (<128) rejected');

// 17. Vector schema validation: must be finite numeric values (no NaN, Inf, strings)
$invalidVector = array_fill(0, 128, 0.05);
$invalidVector[10] = 'not_a_number';
$resFresh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$sigFresh = $signMessage($resFresh['data']['message'], $devicePrivateKey);
$res = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => $invalidVector,
]);
check($res['code'] === 422 && $res['error_code'] === 'VALIDATION_FAILED', 'Vector containing non-numeric values rejected');

// 18. Non-matching face descriptor (distance > 0.58) rejected (FACE_MISMATCH)
$mismatchedDescriptor = array_fill(0, 128, -0.05);
$mNorm = sqrt(array_sum(array_map(fn($v) => $v * $v, $mismatchedDescriptor)));
$mismatchedDescriptor = array_map(fn($v) => $v / $mNorm, $mismatchedDescriptor);

$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');
$resFresh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$sigFresh = $signMessage($resFresh['data']['message'], $devicePrivateKey);
$verifyFail = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => $mismatchedDescriptor,
]);
check($verifyFail['code'] === 401 && $verifyFail['error_code'] === 'FACE_MISMATCH', 'Non-matching face descriptor rejected (FACE_MISMATCH)');
// Privacy verification: Ensure detailed distance is NOT revealed in error message
check(strpos($verifyFail['message'] ?? '', 'distancia') === false, 'Detailed distance is NOT exposed in user-facing error response');

// 19. Challenge reuse rejected
$verifyReplay = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => $syntheticDescriptor,
]);
check($verifyReplay['code'] === 401 && $verifyReplay['error_code'] === 'CHALLENGE_EXPIRED_OR_REUSED', 'Reused face challenge rejected');

// 20. Matching descriptor (distance < 0.58) with valid signature in PREVIEW / RESTRICTED mode (default safety)
$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');
$resFresh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$sigFresh = $signMessage($resFresh['data']['message'], $devicePrivateKey);

$matchingPerturbed = $syntheticDescriptor;
$matchingPerturbed[0] += 0.02;
$matchingPerturbed[1] -= 0.02;

$verifyControlled = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => $matchingPerturbed,
]);
check($verifyControlled['code'] === 200, 'Matching descriptor and valid signature successfully verify in test mode');
check($verifyControlled['data']['verified'] === false && $verifyControlled['data']['descriptor_match'] === true && $verifyControlled['data']['liveness_verified'] === false, 'Preview match never claims verified identity or liveness');
check($verifyControlled['data']['session_issued'] === false, 'SAFETY RULE ENFORCED: Real session issuance is gated/disabled by default');
check($verifyControlled['data']['mode'] === 'PREVIEW_SECURITY_RESTRICTED', 'Response indicates PREVIEW_SECURITY_RESTRICTED mode');

// 21. Bloqueo incondicional: con face_auth_issuance_enabled = true, NO se emite sesion
$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');
$cfgRuntime = require $path;
$cfgRuntime['security']['face_auth_issuance_enabled'] = true;
$cfgRuntime['security']['face_auth_engine_certified'] = false;
file_put_contents($path, '<?php return ' . var_export($cfgRuntime, true) . ';');

// Independent account identity tests. Synthetic vectors only; never source worker data.
$foreignDescriptor = array_fill(0, 128, -1 / sqrt(128));
$admin->prepare("UPDATE `$source`.trabajadores SET rostro_descriptor = ? WHERE id_trabajador = 2")
    ->execute([json_encode($foreignDescriptor)]);
$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');
$ownChallenge=api('/auth/face/challenge',['username'=>'fixture','device_uid'=>$activeDeviceUid]);
$ownSignature=$signMessage($ownChallenge['data']['message'],$devicePrivateKey);
$otherFace=api('/auth/face/verify',[
    'username'=>'fixture','device_uid'=>$activeDeviceUid,
    'challenge_id'=>$ownChallenge['data']['challenge_id'],'nonce'=>$ownChallenge['data']['nonce'],
    'signature'=>$ownSignature,'face_data'=>$foreignDescriptor,
]);
check($otherFace['code']===401 && $otherFace['error_code']==='FACE_MISMATCH', 'Another enrolled workers face cannot switch the bound account');
check(!isset($ownChallenge['data']['descriptor']) && !isset($ownChallenge['data']['rostro_descriptor']), 'Stored facial references never leave server');
$actions=$ownChallenge['data']['actions'];sort($actions);
check($actions===['BLINK','TURN_LEFT','TURN_RIGHT'], 'Server generates the complete temporal action sequence');
$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');
$wrongUserChallenge=api('/auth/face/challenge',['username'=>'fixture','device_uid'=>$activeDeviceUid]);
$wrongUser=api('/auth/face/verify',[
    'username'=>'fixture-admin','device_uid'=>$activeDeviceUid,
    'challenge_id'=>$wrongUserChallenge['data']['challenge_id'],'nonce'=>$wrongUserChallenge['data']['nonce'],
    'signature'=>$signMessage($wrongUserChallenge['data']['message'],$devicePrivateKey),'face_data'=>$foreignDescriptor,
]);
check($wrongUser['code']===403 && $wrongUser['error_code']==='UNAUTHORIZED', 'Changing username cannot reuse another accounts challenge');
check(!WebSitePlus\Auth\FaceAuthService::validDescriptor(array_fill(0,128,'0.1')), 'Numeric strings are not accepted as neural output');
check(!WebSitePlus\Auth\FaceAuthService::validDescriptor(array_fill(1,128,0.1)), 'Associative or shifted vectors are rejected');
check(!WebSitePlus\Auth\FaceAuthService::validDescriptor(array_fill(0,128,INF)), 'Nonfinite descriptors are rejected');
check(!WebSitePlus\Auth\FaceAuthService::validDescriptor(array_fill(0,128,NAN)), 'NaN descriptors are rejected');

$resFresh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$sigFresh = $signMessage($resFresh['data']['message'], $devicePrivateKey);

$verifyAccidental = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => $matchingPerturbed,
]);
check($verifyAccidental['code'] === 200 && $verifyAccidental['data']['session_issued'] === false, 'Accidental config activation blocked without certified engine');
check(empty($verifyAccidental['data']['auth']), 'Auth token payload is strictly null/empty under issuance_enabled');
check($verifyAccidental['data']['mode'] === 'PREVIEW_SECURITY_RESTRICTED', 'Remains in PREVIEW_SECURITY_RESTRICTED mode');

// 22. Bloqueo incondicional: INCLUSO con ambas banderas activadas (face_auth_issuance_enabled = true Y face_auth_engine_certified = true)
// NO se emite access token ni refresh token a partir de descriptores enviados por el cliente
$db->exec('UPDATE wp_intentos_login SET creado_en = DATE_SUB(NOW(), INTERVAL 1 DAY)');
$cfgRuntime['security']['face_auth_issuance_enabled'] = true;
$cfgRuntime['security']['face_auth_engine_certified'] = true;
file_put_contents($path, '<?php return ' . var_export($cfgRuntime, true) . ';');

$resFresh = api('/auth/face/challenge', ['username' => 'fixture', 'device_uid' => $activeDeviceUid]);
$sigFresh = $signMessage($resFresh['data']['message'], $devicePrivateKey);

$verifyBothFlags = api('/auth/face/verify', [
    'username'     => 'fixture',
    'device_uid'   => $activeDeviceUid,
    'challenge_id' => $resFresh['data']['challenge_id'],
    'nonce'        => $resFresh['data']['nonce'],
    'signature'    => $sigFresh,
    'face_data'    => $matchingPerturbed,
]);
check($verifyBothFlags['code'] === 200 && $verifyBothFlags['data']['session_issued'] === false, 'Even with BOTH flags true, session issuance remains unconditionally blocked');
check(empty($verifyBothFlags['data']['auth']), 'Neither access token nor refresh token is issued');
check($verifyBothFlags['data']['mode'] === 'PREVIEW_SECURITY_RESTRICTED', 'Mode strictly remains PREVIEW_SECURITY_RESTRICTED');

// Confirm protected endpoints reject unauthenticated access
$me = api('/auth/me', null, null);
check($me['code'] === 401, 'Protected resource safely inaccessible without valid session token');
check((int) $db->query('SELECT COUNT(*) FROM wp_sesiones')->fetchColumn() === $faceSessionCountBefore,
    'All facial previews and config flags create zero sessions in database');

// Reset config back to safe defaults (both false)
$cfgRuntime['security']['face_auth_issuance_enabled'] = false;
$cfgRuntime['security']['face_auth_engine_certified'] = false;
file_put_contents($path, '<?php return ' . var_export($cfgRuntime, true) . ';');
