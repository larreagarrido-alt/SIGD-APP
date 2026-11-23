<?php
declare(strict_types=1);
function labEcKey(): array
{
    $key=openssl_pkey_new(['private_key_type'=>OPENSSL_KEYTYPE_EC,'curve_name'=>'prime256v1',
        'config'=>__DIR__.'/fixtures/openssl.cnf']);
    if(!$key)throw new RuntimeException('Cannot create synthetic EC key');
    return [$key,openssl_pkey_get_details($key)['key']];
}
