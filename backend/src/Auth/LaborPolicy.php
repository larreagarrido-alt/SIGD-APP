<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;
use DateTimeImmutable;
use WebSitePlus\Core\Database;
final class LaborPolicy
{
    public static function approvedLeaveOnDate(int $worker, string $date): ?array
    {
        if ($worker <= 0 || !preg_match('/^\d{4}-\d{2}-\d{2}$/D', $date)) { return null; }
        $leave = Database::getAttendanceConnection()->prepare(
            'SELECT id_permiso_laboral,tipo_permiso,fecha_inicio,fecha_fin FROM permisos_laborales
             WHERE id_trabajador=? AND estado="Aprobado" AND fecha_inicio<=? AND fecha_fin>=?
             ORDER BY fecha_inicio DESC,id_permiso_laboral DESC LIMIT 1');
        $leave->execute([$worker,$date,$date]);
        $row = $leave->fetch(\PDO::FETCH_ASSOC);
        return $row ?: null;
    }

    public static function period(array $evaluation): ?array
    {
        $key=$evaluation['periodo_clave'];
        if ($evaluation['periodo_tipo']==='DIARIO' && preg_match('/^\d{4}-\d{2}-\d{2}$/D',$key)) { return [$key,$key]; }
        if ($evaluation['periodo_tipo']==='MENSUAL' && preg_match('/^\d{4}-\d{2}$/D',$key)) { $d=new DateTimeImmutable($key.'-01'); return [$d->format('Y-m-d'),$d->format('Y-m-t')]; }
        if ($evaluation['periodo_tipo']==='SEMANAL' && preg_match('/^(\d{4})-W(\d{2})$/D',$key,$m)) { $d=(new DateTimeImmutable())->setISODate((int)$m[1],(int)$m[2]); return [$d->format('Y-m-d'),$d->modify('+6 days')->format('Y-m-d')]; }
        return null;
    }
    public static function syncLeaves(?int $worker=null): void
    {
        $db=Database::getConnection();
        $sql='SELECT id_evaluacion,id_trabajador,periodo_tipo,periodo_clave FROM wp_kpi_evaluaciones WHERE estado IN ("Pendiente","En progreso","Vencido","Observado")';
        $st=$db->prepare($sql.($worker===null?'':' AND id_trabajador=?'));
        $st->execute($worker===null?[]:[$worker]);
        $leave=Database::getAttendanceConnection()->prepare('SELECT id_permiso_laboral,fecha_inicio,fecha_fin FROM permisos_laborales WHERE id_trabajador=? AND estado="Aprobado" AND fecha_inicio<=? AND fecha_fin>=? LIMIT 1');
        $comp=Database::getAttendanceConnection()->prepare('SELECT 1 FROM consumos_extras WHERE id_trabajador=? AND tipo_consumo="Permiso" AND fecha_permiso BETWEEN ? AND ? LIMIT 1');
        foreach($st as $evaluation) {
            $period=self::period($evaluation);
            if (!$period) { continue; } // Custom periods require explicit reviewed exemption.
            $leave->execute([$evaluation['id_trabajador'],$period[1],$period[0]]);
            $row=$leave->fetch();
            if (!$row) {
                $comp->execute([$evaluation['id_trabajador'],$period[0],$period[1]]);
                if (!$comp->fetchColumn()) continue;
            }
            $coverage=KpiLaborCalendar::periodCoverage((int)$evaluation['id_trabajador'],$period[0],$period[1]);
            if (!$coverage['tipo_historico_confirmado']) continue;
            if ($coverage['turnos_programados']===0 || $coverage['turnos_exigibles']>0 || $coverage['incidencias']>0) continue;
            $reason=$row ? 'Permiso aprobado #'.$row['id_permiso_laboral'].'; sin turnos exigibles en el periodo'
                : 'Descansos compensatorios aprobados; sin turnos exigibles en el periodo';
            Database::transaction(function($pdo) use($evaluation,$reason) {
                $lock=$pdo->prepare('SELECT estado FROM wp_kpi_evaluaciones WHERE id_evaluacion=? FOR UPDATE');
                $lock->execute([$evaluation['id_evaluacion']]); $old=$lock->fetchColumn();
                if (!in_array($old,['Pendiente','En progreso','Vencido','Observado'],true)) { return; }
                $update=$pdo->prepare('UPDATE wp_kpi_evaluaciones SET estado="No exigible",motivo_no_exigible=? WHERE id_evaluacion=?');
                $update->execute([$reason,$evaluation['id_evaluacion']]);
                $history=$pdo->prepare('INSERT INTO wp_kpi_revisiones (id_evaluacion,estado_anterior,estado_nuevo,motivo) VALUES (?,?,"No exigible",?)');
                $history->execute([$evaluation['id_evaluacion'],$old,$reason]);
            });
        }
    }
}
