<?php
declare(strict_types=1);
namespace WebSitePlus\Employees;

use PDO;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;

/** Current observations, not reconstructed effective employment dates. Attendance is SELECT only. */
final class LaborObservationService
{
    public static function observe(int $workerId): array
    {
        return Database::transaction(function(PDO $db) use($workerId): array {
            [$worker,$previous,$created]=self::lock($db,$workerId);
            return self::record($db,$worker,$previous,$created);
        });
    }

    /** Shared serialization point for observations and explicit employment transitions. */
    public static function lock(PDO $db,int $workerId): array
    {
        $read=Database::getAttendanceConnection()->prepare('SELECT id_trabajador,estado,tipo,id_cargo,id_agencia FROM trabajadores WHERE id_trabajador=?');
        $read->execute([$workerId]);$seed=$read->fetch();
        if(!$seed)throw new HttpException('Trabajador no encontrado',404,'NOT_FOUND');
        self::validate($seed);
        $insert=$db->prepare('INSERT INTO wp_situacion_laboral(id_trabajador,ultimo_estado_visto,ultimo_tipo_visto,ultimo_cargo_visto,ultima_agencia_vista) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE id_trabajador=VALUES(id_trabajador)');
        $insert->execute([$workerId,$seed['estado'],$seed['tipo']??'',$seed['id_cargo'],$seed['id_agencia']]);
        $created=$insert->rowCount()===1;
        $lock=$db->prepare('SELECT * FROM wp_situacion_laboral WHERE id_trabajador=? FOR UPDATE');
        $lock->execute([$workerId]);$previous=$lock->fetch();
        // Re-read AFTER the lock through the SELECT-only autocommit connection. A waiting request
        // must not replay a stale source snapshot from its own REPEATABLE READ transaction.
        $read->execute([$workerId]);$worker=$read->fetch();
        if(!$worker)throw new HttpException('Trabajador no encontrado',404,'NOT_FOUND');
        self::validate($worker);
        return [$worker,$previous,$created];
    }

    private static function validate(array $worker): void
    {
        if(!in_array($worker['estado'],['Activo','Inactivo'],true))
            throw new HttpException('Estado de asistencia no valido',409,'LABOR_STATE_CONFLICT');
    }

    /** Caller holds the complementary row lock. Does not create, close or rewrite periods. */
    public static function record(PDO $db,array $worker,array $previous,bool $created,bool $manualReactivation=false): array
    {
        $workerId=(int)$worker['id_trabajador'];$changes=[];
        if($created)$changes[]=['CARGA_INICIAL',null,
            'estado='.$worker['estado'].';tipo='.($worker['tipo']??'').';cargo='.($worker['id_cargo']??'NULL').';agencia='.($worker['id_agencia']??'NULL')];
        $fields=[
            ['estado','ultimo_estado_visto',$worker['estado']==='Activo'?'REACTIVACION':'INACTIVACION'],
            ['tipo','ultimo_tipo_visto','CONVERSION_TIPO'],
            ['id_cargo','ultimo_cargo_visto','CAMBIO_CARGO'],
            ['id_agencia','ultima_agencia_vista','CAMBIO_AGENCIA'],
        ];
        foreach($fields as [$field,$snapshot,$event]){
            $old=$previous[$snapshot];$new=$worker[$field];
            if($field==='tipo'){$old=$old??'';$new=$new??'';}
            if(($old===null?null:(string)$old)!==($new===null?null:(string)$new)
                &&!($manualReactivation&&$field==='estado'))$changes[]=[$event,$old,$new];
        }
        if($changes){
            $event=$db->prepare('INSERT INTO wp_eventos_laborales(id_trabajador,tipo_evento,valor_anterior,valor_nuevo,fecha_efectiva,motivo,origen) VALUES(?,?,?,?,CURRENT_DATE(),?,"SINCRONIZACION")');
            foreach($changes as [$type,$old,$new])$event->execute([$workerId,$type,$old,$new,
                'Observado en asistencia en la fecha de este registro; fecha efectiva y motivo originales no disponibles. No modifica periodos ni implica desvinculacion o reingreso.']);
            // Preserve the explicit situation, reason, actor and termination date.
            $update=$db->prepare('UPDATE wp_situacion_laboral SET ultimo_estado_visto=?,ultimo_tipo_visto=?,ultimo_cargo_visto=?,ultima_agencia_vista=?,sincronizado_en=NOW() WHERE id_trabajador=?');
            $update->execute([$manualReactivation?$previous['ultimo_estado_visto']:$worker['estado'],$worker['tipo']??'',$worker['id_cargo'],$worker['id_agencia'],$workerId]);
        }
        $revoked=0;
        if($worker['estado']==='Inactivo'||$previous['situacion']==='DESVINCULADO'){
            $revoke=$db->prepare('UPDATE wp_sesiones SET revocada_en=NOW(),motivo_revocacion=? WHERE revocada_en IS NULL AND id_usuario IN (SELECT id_usuario FROM usuarios WHERE id_trabajador=?)');
            $revoke->execute([$previous['situacion']==='DESVINCULADO'?'DESVINCULACION_LABORAL':'INACTIVIDAD_ASISTENCIA',$workerId]);
            $revoked=$revoke->rowCount();
        }
        return ['id_trabajador'=>$workerId,'eventos'=>count($changes),'sesiones_revocadas'=>$revoked];
    }
}
