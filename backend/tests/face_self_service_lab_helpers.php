<?php
declare(strict_types=1);
// Global policy in isolated lab only. No per-user or per-device authorization configuration.
function labFaceSelfServiceConfig(array $cfg):array {
    $cfg['security']['face_local_pad_login_enabled']=false;
    $cfg['security']['face_pilot']=['enabled'=>false];
    $cfg['security']['face_self_service']=['enabled'=>true,'local_pad_risk_accepted'=>true,'activation_id'=>'SYNTHETIC-LAB-SELF-SERVICE-1'];
    return $cfg;
}
