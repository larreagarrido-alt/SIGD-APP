<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use DateTimeImmutable;
use PDO;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Read-only attendance records for the authenticated worker. */
final class AttendanceController
{
    public function mine(Request $request, array $session): void
    {
        $worker=(int)($session['id_trabajador']??0);
        if ($worker<1) throw new HttpException('Cuenta sin trabajador vinculado',403,'FORBIDDEN');
        $month=(string)$request->getQuery('mes',date('Y-m'));
        $first=DateTimeImmutable::createFromFormat('!Y-m-d',$month.'-01');
        if (!$first || $first->format('Y-m')!==$month || $first>new DateTimeImmutable('first day of next month'))
            throw new HttpException('Mes inválido',422,'VALIDATION_FAILED');
        $start=$first->format('Y-m-d');
        $end=$first->format('Y-m-t');
        $db=Database::getAttendanceConnection();
        $st=$db->prepare('SELECT fecha,MIN(hora_entrada) entrada,MAX(hora_salida) salida,
            MAX(CASE WHEN estado_asistencia="Tardanza" THEN 1 ELSE 0 END) tardanza
            FROM asistencias WHERE id_trabajador=? AND fecha BETWEEN ? AND ?
            GROUP BY fecha ORDER BY fecha DESC');
        $st->execute([$worker,$start,$end]);
        $days=[];
        foreach ($st->fetchAll(PDO::FETCH_ASSOC) as $row) {
            $date=(string)$row['fecha'];
            $days[$date]=['fecha'=>$date,'entrada'=>$row['entrada'],'salida'=>$row['salida'],
                'tardanza'=>(bool)$row['tardanza'],'falta'=>false,'justificada'=>false];
        }
        $missing=$db->prepare('SELECT DISTINCT fecha_falta FROM faltas
            WHERE id_trabajador=? AND fecha_falta BETWEEN ? AND ?');
        $missing->execute([$worker,$start,$end]);
        foreach ($missing->fetchAll(PDO::FETCH_COLUMN) as $date) {
            $date=(string)$date;
            $days[$date]??=['fecha'=>$date,'entrada'=>null,'salida'=>null,
                'tardanza'=>false,'falta'=>false,'justificada'=>false];
            $days[$date]['falta']=true;
        }
        $approved=$db->prepare('SELECT DISTINCT fecha_referencia FROM justificaciones
            WHERE id_trabajador=? AND fecha_referencia BETWEEN ? AND ? AND estado_aprobacion="Aprobada"');
        $approved->execute([$worker,$start,$end]);
        foreach ($approved->fetchAll(PDO::FETCH_COLUMN) as $date) {
            $date=(string)$date;
            if (isset($days[$date])) $days[$date]['justificada']=true;
        }
        krsort($days);
        $records=array_values($days);
        Response::success(['mes'=>$month,'registros'=>$records,'resumen'=>[
            'dias_con_marcacion'=>count(array_filter($records,static fn($r)=>$r['entrada']!==null)),
            'dias_con_tardanza'=>count(array_filter($records,static fn($r)=>$r['tardanza'])),
            'dias_con_falta'=>count(array_filter($records,static fn($r)=>$r['falta'])),
            'dias_justificados'=>count(array_filter($records,static fn($r)=>$r['justificada'])),
        ]]);
    }
}
