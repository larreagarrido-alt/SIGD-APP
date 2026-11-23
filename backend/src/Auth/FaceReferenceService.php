<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;

/** 1:1 reference selection. Attendance always wins; no global face search or source writes. */
final class FaceReferenceService
{
    public static function hasTable(string $table): bool {
        $st=Database::getConnection()->prepare('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?');
        $st->execute([$table]);return (int)$st->fetchColumn()===1;
    }
    public static function valid(mixed $v): bool {
        if(!FaceAuthService::validDescriptor($v))return false;
        $norm=0;foreach($v as $x){if(abs($x)>10)return false;$norm+=$x*$x;}
        return $norm>=.01 && $norm<=100;
    }
    public static function attendance(int $worker): ?array {
        $st=Database::getAttendanceConnection()->prepare('SELECT rostro_descriptor FROM trabajadores WHERE id_trabajador=? AND estado="Activo" LIMIT 1');
        $st->execute([$worker]);$raw=$st->fetchColumn();
        $v=is_string($raw)?json_decode($raw,false):null;return self::valid($v)?$v:null;
    }
    public static function describe(int $user,int $worker): array {
        if(self::attendance($worker)!==null)return ['reference_source'=>'ASISTENCIA','reference_available'=>true,'requires_enrollment'=>false,'migration_v012_required'=>false,'estado'=>'REFERENCIA_ASISTENCIA'];
        if(!self::hasTable('wp_rostros_cuenta'))return ['reference_source'=>'NINGUNA','reference_available'=>false,'requires_enrollment'=>true,'migration_v012_required'=>true,'estado'=>'SIN_REGISTRO'];
        $st=Database::getConnection()->prepare('SELECT estado FROM wp_rostros_cuenta WHERE id_usuario=?');$st->execute([$user]);$state=$st->fetchColumn();
        return ['reference_source'=>$state?'WEBSITEPLUS_PENDIENTE':'NINGUNA','reference_available'=>false,'requires_enrollment'=>!$state,'migration_v012_required'=>false,'estado'=>$state?:'SIN_REGISTRO'];
    }
    public static function resolve(int $user,int $worker): array {
        $vector=self::attendance($worker);if($vector!==null)return ['source'=>'ASISTENCIA','vector'=>$vector];
        $description=self::describe($user,$worker);
        throw new HttpException($description['requires_enrollment']?'Tu cuenta requiere registro facial autorizado':'Tu rostro requiere validacion empresarial antes de utilizarse',422,
            $description['requires_enrollment']?'FACE_ENROLLMENT_REQUIRED':'FACE_PROFILE_PENDING');
    }
}
