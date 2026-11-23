<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use WebSitePlus\Core\HttpException;

/** Parses signed CLIENT evidence. This does not run PAD or attest camera provenance. */
final class LocalFaceProof
{
    public static function decode(string $payload): array
    {
        $v=strlen($payload)<=10000?json_decode($payload):null;
        if(is_array($v)) {
            if(!FaceReferenceService::valid($v))self::invalid();
            return ['vector'=>$v,'local_pad'=>false]; // V013 diagnostic compatibility, never creates sessions.
        }
        if(!is_object($v)||($v->version??null)!==2||!FaceReferenceService::valid($v->descriptor??null))self::invalid();
        $p=$v->pad??null;
        if(!is_object($p)||($p->model??null)!=='MiniFASNetV1SE+V2-min'
            ||!is_int($p->samples??null)||$p->samples<5||$p->samples>200
            ||!is_int($p->duration_ms??null)||$p->duration_ms<800||$p->duration_ms>15000
            ||(!is_int($p->real_min??null)&&!is_float($p->real_min??null))
            ||!is_finite((float)$p->real_min)||$p->real_min<0.95||$p->real_min>1)self::invalid();
        return ['vector'=>$v->descriptor,'local_pad'=>true];
    }
    private static function invalid(): never {throw new HttpException('Captura facial o comprobacion local incompleta',422,'FACE_CAPTURE_INVALID');}
}
