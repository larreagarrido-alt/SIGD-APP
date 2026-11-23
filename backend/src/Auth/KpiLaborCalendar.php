<?php
declare(strict_types=1);
namespace WebSitePlus\Auth;

use DateTimeImmutable;
use PDO;
use WebSitePlus\Core\Database;

/** Read-only KPI eligibility from the attendance system. */
final class KpiLaborCalendar
{
    private const DAYS = ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];

    public static function currentWeek(int $worker): array
    {
        $week = self::weekForDate($worker, new DateTimeImmutable('today'));
        $week['dias'] = array_slice($week['dias'], 0, 6);
        return $week;
    }

    public static function periodCoverage(int $worker, string $start, string $end): array
    {
        $first=DateTimeImmutable::createFromFormat('!Y-m-d',$start);
        $last=DateTimeImmutable::createFromFormat('!Y-m-d',$end);
        if (!$first || !$last || $first->format('Y-m-d')!==$start || $last->format('Y-m-d')!==$end
            || $first>$last || $first->diff($last)->days>370) throw new \InvalidArgumentException('Periodo KPI inválido');
        $scheduled=$eligible=$incidents=0;
        $typeHistoryConfirmed=true;
        $evidence=[];
        for ($week=$first->modify('monday this week'); $week<=$last; $week=$week->modify('+7 days')) {
            foreach (self::weekForDate($worker,$week)['dias'] as $day) {
                if ($day['fecha']<$start || $day['fecha']>$end) continue;
                $scheduled+=$day['turnos_programados'];
                $eligible+=$day['turnos_exigibles'];
                if ($day['estado']==='INCIDENCIA') $incidents++;
                if (!$day['tipo_historico_confirmado']) $typeHistoryConfirmed=false;
                $evidence[]=$day;
            }
        }
        return ['turnos_programados'=>$scheduled,'turnos_exigibles'=>$eligible,'incidencias'=>$incidents,
            'tipo_historico_confirmado'=>$typeHistoryConfirmed,
            'dias'=>$evidence,
            'origen_sha256'=>hash('sha256',json_encode($evidence,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES))];
    }

    private static function weekForDate(int $worker, DateTimeImmutable $day): array
    {
        $monday = $day->modify('monday this week');
        $source = Database::getAttendanceConnection();
        $workerType = $source->prepare('SELECT tipo FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
        $workerType->execute([$worker]);
        $currentType = $workerType->fetchColumn();
        $practiceDays = [];
        $practice = $source->prepare('SELECT dia,asiste,asiste_manana,asiste_tarde,hora_inicio,hora_fin,hora_inicio_tarde,hora_fin_tarde FROM horario_practicantes WHERE id_trabajador=?');
        $practice->execute([$worker]);
        foreach ($practice->fetchAll(PDO::FETCH_ASSOC) as $row) $practiceDays[self::dayKey((string)$row['dia'])] = $row;
        $typeHistory = $source->prepare('SELECT tipo FROM tipo_vigencias WHERE id_trabajador=? AND vigente_desde<=? ORDER BY vigente_desde DESC LIMIT 1');
        $history = $source->prepare('SELECT horario_json FROM horario_vigencias WHERE id_trabajador=? AND vigente_desde<=? ORDER BY vigente_desde DESC LIMIT 1');
        $assigned = $source->prepare('SELECT ht.dias_asistencia,hd.dia_semana,hd.hora_entrada,hd.hora_salida,hd.inicio_refrigerio,hd.fin_refrigerio,hd.es_laborable
            FROM horarios_trabajadores ht JOIN temporadas_horario th ON th.id_temporada=ht.id_temporada
            JOIN horario_detalles hd ON hd.id_horario=ht.id_horario
            WHERE ht.id_trabajador=? AND th.fecha_inicio<=? AND th.fecha_fin>=?
            ORDER BY th.fecha_inicio DESC,ht.id_horario_trabajador DESC,hd.id_horario_detalle DESC');
        $comp = $source->prepare('SELECT detalles FROM consumos_extras WHERE id_trabajador=? AND tipo_consumo="Permiso" AND fecha_permiso=?');
        $absence = $source->prepare('SELECT 1 FROM faltas WHERE id_trabajador=? AND fecha_falta=? LIMIT 1');
        $justified = $source->prepare('SELECT 1 FROM justificaciones WHERE id_trabajador=? AND fecha_referencia=? AND tipo_justificacion="Falta" AND estado_aprobacion="Aprobada" LIMIT 1');
        $holiday = $source->prepare('SELECT 1 FROM feriados WHERE fecha=? AND estado=1 AND excluir_evaluacion=1 AND tipo_jornada="no_laborable" LIMIT 1');
        $days = [];
        for ($index=0; $index<7; $index++) {
            $date = $monday->modify('+'.$index.' days')->format('Y-m-d');
            $name = self::DAYS[$index];
            $typeHistory->execute([$worker,$date]);
            $recordedType=$typeHistory->fetchColumn();
            $confirmedType=$recordedType!==false;
            // A direct edit outside the audited web flow must not silently keep
            // yesterday's type for today's or a future official calculation.
            if ($confirmedType && $date>=date('Y-m-d') && $currentType!==false
                && $recordedType!==$currentType) $confirmedType=false;
            $isPractitioner=($confirmedType?$recordedType:$currentType)==='Practicante';
            $morning = $afternoon = false;
            $morningWindow = $afternoonWindow = null;
            $practiceRow = $isPractitioner ? ($practiceDays[self::dayKey($name)] ?? null) : null;
            $historicSchedule=false;
            if ($isPractitioner && $date < date('Y-m-d')) {
                $history->execute([$worker,$date]);
                $snapshot=$history->fetchColumn();
                if ($snapshot!==false) {
                    $historicSchedule=true;
                    $practiceRow=null;
                    $saved=json_decode((string)$snapshot,true);
                    if (is_array($saved)) foreach ($saved as $historicRow) {
                        if (is_array($historicRow) && isset($historicRow['dia'])
                            && self::dayKey((string)$historicRow['dia'])===self::dayKey($name)) {
                            $practiceRow=$historicRow;
                            break;
                        }
                    }
                }
            }
            if ($practiceRow) {
                $morning = (bool)$practiceRow['asiste'] && (bool)$practiceRow['asiste_manana'];
                $afternoon = (bool)$practiceRow['asiste'] && (bool)$practiceRow['asiste_tarde'];
                if ($morning) $morningWindow = [substr((string)($practiceRow['hora_inicio'] ?: '08:00'),0,5),substr((string)($practiceRow['hora_fin'] ?: '13:30'),0,5)];
                if ($afternoon) $afternoonWindow = [substr((string)($practiceRow['hora_inicio_tarde'] ?: '15:30'),0,5),substr((string)($practiceRow['hora_fin_tarde'] ?: '18:30'),0,5)];
            } elseif (!$isPractitioner && !$historicSchedule) {
                $assigned->execute([$worker,$date,$date]);
                foreach ($assigned->fetchAll(PDO::FETCH_ASSOC) as $row) {
                    if (self::dayKey((string)$row['dia_semana'])!==self::dayKey($name)) continue;
                    // Asistencias can restrict a seasonal schedule to selected weekdays.
                    // The generic horario_detalles row alone is not an obligation.
                    if (trim((string)($row['dias_asistencia'] ?? '')) !== '') {
                        $selected = array_map(self::dayKey(...), explode(',', (string)$row['dias_asistencia']));
                        if (!in_array(self::dayKey($name), $selected, true)) break;
                    }
                    if ((bool)$row['es_laborable']) {
                        $start = substr((string)$row['hora_entrada'],0,5);
                        $end = substr((string)$row['hora_salida'],0,5);
                        if ($start!=='' && $end!=='' && $start<$end) {
                            $breakStart=substr((string)($row['inicio_refrigerio'] ?: '13:30'),0,5);
                            $breakEnd=substr((string)($row['fin_refrigerio'] ?: '15:30'),0,5);
                            if ($start<$breakStart) { $morning=true; $morningWindow=[$start,min($end,$breakStart)]; }
                            if ($end>$breakEnd) { $afternoon=true; $afternoonWindow=[max($start,$breakEnd),$end]; }
                            if (!$morning && !$afternoon) {
                                if ($start<'14:00') { $morning=true; $morningWindow=[$start,$end]; }
                                else { $afternoon=true; $afternoonWindow=[$start,$end]; }
                            }
                        }
                    }
                    break;
                }
            }
            $planned=(int)$morning+(int)$afternoon;
            $leave = LaborPolicy::approvedLeaveOnDate($worker,$date);
            if ($leave) {
                $days[] = ['fecha'=>$date,'dia'=>$name,'manana'=>false,'tarde'=>false,
                    'tipo_historico_confirmado'=>$confirmedType,
                    'estado'=>'NO_EXIGIBLE','motivo'=>'Permiso o vacaciones aprobadas',
                    'permiso_aprobado'=>true,
                    'turnos_programados'=>$planned,'turnos_exigibles'=>0];
                continue;
            }
            $holiday->execute([$date]);
            if ($holiday->fetchColumn()) {
                $days[] = ['fecha'=>$date,'dia'=>$name,'manana'=>false,'tarde'=>false,
                    'tipo_historico_confirmado'=>$confirmedType,
                    'estado'=>'NO_EXIGIBLE','motivo'=>'Feriado no laborable',
                    'turnos_programados'=>$planned,'turnos_exigibles'=>0];
                continue;
            }
            $comp->execute([$worker,$date]);
            $compensations = $comp->fetchAll(PDO::FETCH_COLUMN);
            foreach ($compensations as $details) {
                if (!preg_match('/(\d{2}:\d{2})\s+a\s+(\d{2}:\d{2})/i',(string)$details,$m)) continue;
                if ($morning && $morningWindow && $m[1]<=$morningWindow[0] && $m[2]>=$morningWindow[1]) $morning=false;
                if ($afternoon && $afternoonWindow && $m[1]<=$afternoonWindow[0] && $m[2]>=$afternoonWindow[1]) $afternoon=false;
            }
            $absence->execute([$worker,$date]);
            $hasAbsence = (bool)$absence->fetchColumn();
            if ($hasAbsence && $planned > 0) {
                $justified->execute([$worker,$date]);
                $approved = (bool)$justified->fetchColumn();
                if ($approved && !$compensations) {
                    $days[]=['fecha'=>$date,'dia'=>$name,'manana'=>false,'tarde'=>false,
                        'tipo_historico_confirmado'=>$confirmedType,
                        'estado'=>'NO_EXIGIBLE','motivo'=>'Ausencia justificada y aprobada',
                        'turnos_programados'=>$planned,'turnos_exigibles'=>0];
                    continue;
                }
                if (!$approved && !$compensations) {
                    $days[]=['fecha'=>$date,'dia'=>$name,'manana'=>false,'tarde'=>false,
                        'tipo_historico_confirmado'=>$confirmedType,
                        'estado'=>'INCIDENCIA','motivo'=>'Falta registrada en Asistencias; pendiente de revisión',
                        'turnos_programados'=>$planned,'turnos_exigibles'=>0];
                    continue;
                }
            }
            $days[]=['fecha'=>$date,'dia'=>$name,'manana'=>$morning,'tarde'=>$afternoon,
                'tipo_historico_confirmado'=>$confirmedType,
                'estado'=>($morning||$afternoon)?'EXIGIBLE':'NO_EXIGIBLE',
                'motivo'=>($morning||$afternoon)?null:($practiceRow?'Sin turno o descanso compensatorio':'Sin turno asignado o descanso compensatorio'),
                'turnos_programados'=>$planned,'turnos_exigibles'=>(int)$morning+(int)$afternoon];
        }
        return ['semana_desde'=>$monday->format('Y-m-d'),'dias'=>$days];
    }

    private static function dayKey(string $day): string
    {
        return mb_strtolower(strtr(trim($day),['Á'=>'A','É'=>'E','Í'=>'I','Ó'=>'O','Ú'=>'U','á'=>'a','é'=>'e','í'=>'i','ó'=>'o','ú'=>'u']), 'UTF-8');
    }
}
