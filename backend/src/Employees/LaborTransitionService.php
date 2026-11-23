<?php
declare(strict_types=1);
namespace WebSitePlus\Employees;

use DateTimeImmutable;
use PDO;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;

/** Serializes WebSite+ employment changes. Attendance is read only; events are append only. */
final class LaborTransitionService
{
    public static function validate(string $event,string $reason,string $date): void {
        $parsed=DateTimeImmutable::createFromFormat('!Y-m-d',$date);
        if(!in_array($event,['REINGRESO','DESVINCULACION','REACTIVACION'],true)
            ||trim($reason)===''||mb_strlen(trim($reason))>500||!$parsed||$parsed->format('Y-m-d')!==$date
            ||$date!==date('Y-m-d'))throw new HttpException('Evento, motivo o fecha efectiva invalidos; solo se admiten cambios del dia actual',422,'VALIDATION_FAILED');
    }
    public static function apply(int $workerId,string $event,string $reason,string $date,int $actor): array {
        self::validate($event,$reason,$date);$reason=trim($reason);
        return Database::transaction(function(PDO $db)use($workerId,$event,$reason,$date,$actor){
            [$worker,$previous,$created]=LaborObservationService::lock($db,$workerId);
            $old=$previous['situacion'];
            if(($event==='DESVINCULACION'&&$old!=='VIGENTE')
                ||($event==='REINGRESO'&&($old!=='DESVINCULADO'||$worker['estado']!=='Activo'))
                ||($event==='REACTIVACION'&&($old!=='VIGENTE'||$worker['estado']!=='Activo'||$previous['ultimo_estado_visto']!=='Inactivo')))
                throw new HttpException('La transicion no corresponde al estado actual; no se duplico el historial',409,'LABOR_STATE_CONFLICT');
            // Do not swallow other observed changes when an explicit transition advances the snapshot.
            // A manual reactivation itself records the state edge once, with its supplied reason.
            LaborObservationService::record($db,$worker,$previous,$created,$event==='REACTIVACION');
            $st=$db->prepare('SELECT id_periodo,fecha_inicio FROM wp_periodos_laborales WHERE id_trabajador=? AND fecha_fin IS NULL FOR UPDATE');$st->execute([$workerId]);$open=$st->fetch();
            $period=$open?(int)$open['id_periodo']:null;
            if($event==='DESVINCULACION'&&$open){
                if($open['fecha_inicio']>$date)throw new HttpException('El periodo comienza despues de la fecha efectiva',409,'LABOR_PERIOD_CONFLICT');
                $db->prepare('UPDATE wp_periodos_laborales SET fecha_fin=?,motivo_fin="DESVINCULACION" WHERE id_periodo=? AND fecha_fin IS NULL')->execute([$date,$period]);
            }
            if($event==='REINGRESO'){
                if($open)throw new HttpException('El trabajador desvinculado conserva un periodo abierto; requiere revision',409,'LABOR_PERIOD_CONFLICT');
                $type=in_array($worker['tipo'],['Practicante','Colaborador'],true)?$worker['tipo']:'Sin tipo';
                $st=$db->prepare('INSERT INTO wp_periodos_laborales(id_trabajador,tipo,id_cargo,id_agencia,fecha_inicio,motivo_inicio,registrado_por) VALUES(?,?,?,?,?,"REINGRESO",?)');
                $st->execute([$workerId,$type,$worker['id_cargo'],$worker['id_agencia'],$date,$actor]);$period=(int)$db->lastInsertId();
            }
            // Reactivation records an already observed return in attendance. It does not invent pause dates.
            $next=$event==='DESVINCULACION'?'DESVINCULADO':'VIGENTE';
            $st=$db->prepare('UPDATE wp_situacion_laboral SET situacion=?,desvinculado_en=?,motivo_situacion=?,ultimo_estado_visto=?,ultimo_tipo_visto=?,ultimo_cargo_visto=?,ultima_agencia_vista=?,sincronizado_en=NOW(),actualizado_por=?,actualizado_en=NOW() WHERE id_trabajador=?');
            $st->execute([$next,$next==='DESVINCULADO'?$date:null,$reason,$worker['estado'],$worker['tipo']??'',$worker['id_cargo'],$worker['id_agencia'],$actor,$workerId]);
            $st=$db->prepare('INSERT INTO wp_eventos_laborales(id_trabajador,tipo_evento,valor_anterior,valor_nuevo,fecha_efectiva,motivo,origen,id_periodo,id_usuario_responsable) VALUES(?,?,?,?,?,?,"API",?,?)');
            $st->execute([$workerId,$event,$event==='REACTIVACION'?'Inactivo':$old,$event==='REACTIVACION'?'Activo':$next,$date,$reason,$period,$actor]);$id=(int)$db->lastInsertId();
            if($event==='DESVINCULACION'){
                // A worker may have more than one account. Revoke all methods for all linked users.
                $st=$db->prepare('UPDATE wp_sesiones SET revocada_en=NOW(),motivo_revocacion="DESVINCULACION_LABORAL" WHERE revocada_en IS NULL AND id_usuario IN (SELECT id_usuario FROM usuarios WHERE id_trabajador=?)');
                $st->execute([$workerId]);
            }
            return ['id_evento'=>$id,'id_trabajador'=>$workerId,'situacion'=>$next,'id_periodo'=>$period];
        });
    }
}
