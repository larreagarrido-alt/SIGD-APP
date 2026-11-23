<?php
declare(strict_types=1);
namespace WebSitePlus\Kpi;

/** Recomputes the percentage from raw form counts and the report's official goal revision. */
final class KpiOfficialScore
{
    private const SALES_UNITS = ['LLAMADAS','VISITAS','COTIZACIONES','SEGUIMIENTOS','PROSPECTOS','CLIENTES'];
    private const PROGRAM_KINDS = [
        1=>'Desarrollo',2=>'Corrección',3=>'Mejora',
        4=>'Maquetación',5=>'Integración / API',6=>'Mantenimiento Web',
    ];

    public static function period(string $type): ?string
    {
        return match($type) {
            'PROGRAMACION','DISENADOR_JUNIOR'=>'DIARIO',
            'VENTAS','DISENADOR','DISENADOR_SENIOR','ADMINISTRACION','ASISTENCIA_ADMIN',
            'CONTABILIDAD','COMMUNITY','FOTOGRAFIA','TRAFFICKER','VIDEO','GERENCIA'=>'SEMANAL',
            default=>null,
        };
    }

    public static function percentage(string $type, array $payload, string $day, array $coverage, array $goals=[]): ?float
    {
        $period=self::period($type);
        if ($period===null || empty($coverage['dias'])) return null;
        if (!$goals) $goals=KpiOfficialGoals::defaults($type);
        $scheduled=(int)($coverage['turnos_programados']??0);
        $eligible=(int)($coverage['turnos_exigibles']??0);
        if ($scheduled<=0 || $eligible<=0) return null;
        if ($type==='PROGRAMACION') return self::programmer($payload,$day,$coverage,$goals);
        if ($type==='DISENADOR_JUNIOR') return self::junior($payload,$coverage,$goals);
        if ($type==='VENTAS') {
            $total=0.0;
            foreach (self::SALES_UNITS as $index=>$unit) {
                $goal=$goals[$index]??0;
                if ($goal<=0) return null;
                $actual=KpiFormMeasurement::value($type,$payload,$unit,$day,$period);
                if ($actual===null) return null;
                $effective=$goal*$eligible/$scheduled;
                $total+=min(100.0,$actual/$effective*100.0);
            }
            return round($total/count(self::SALES_UNITS),2);
        }
        if ($type==='GERENCIA') {
            $done=0;
            $eligibleDays=[];
            foreach ($coverage['dias'] as $calendarDay)
                if (!empty($calendarDay['turnos_exigibles'])) $eligibleDays[(string)$calendarDay['fecha']]=true;
            foreach (($payload['days']??[]) as $row)
                if (is_array($row) && !empty($row['isRegistered'])
                    && isset($eligibleDays[(string)($row['date']??'')])
                    && trim((string)($row['trackingPerformed']??''))!=='') $done++;
            $target=count($eligibleDays)*(($goals[0]??6)/6);
            return $target>0 ? round(min(150.0,$done/$target*100.0),2) : null;
        }
        $actual=KpiFormMeasurement::value($type,$payload,'ACTIVIDADES',$day,$period);
        if ($actual===null) return null;
        if (in_array($type,['DISENADOR','DISENADOR_SENIOR','VIDEO'],true)) {
            $target=($goals[0]??0)*$eligible/$scheduled;
        } else {
            if (count($goals)!==6) return null;
            $target=0.0;
            foreach ($coverage['dias'] as $calendarDay) {
                $index=(int)date('N',strtotime((string)$calendarDay['fecha']))-1;
                if ($index<0 || $index>5 || empty($calendarDay['turnos_programados'])) continue;
                $target+=$goals[$index]*(int)$calendarDay['turnos_exigibles']/(int)$calendarDay['turnos_programados'];
            }
        }
        return $target>0 ? round(min(150.0,$actual/$target*100.0),2) : null;
    }

    private static function programmer(array $payload, string $day, array $coverage, array $goals): ?float
    {
        $index=(int)date('N',strtotime($day));
        $type=self::PROGRAM_KINDS[$index]??null;
        $goal=$goals[$index-1]??0;
        if ($type===null || !isset($payload['actividades']) || !is_array($payload['actividades'])) return null;
        $done=0;
        foreach ($payload['actividades'] as $item) {
            if (!is_array($item) || mb_strtolower(trim((string)($item['estado']??'')))!=='entregado') continue;
            $kind=mb_strtolower(trim((string)($item['tipo']??'')));
            $wanted=mb_strtolower($type);
            if ($kind===$wanted || ($index===4 && str_contains($kind,'maquetaci'))
                || ($index===5 && str_contains($kind,'api'))
                || ($index===6 && str_contains($kind,'mantenim'))) $done++;
        }
        $target=$goal*(int)$coverage['turnos_exigibles']/(int)$coverage['turnos_programados'];
        return round(min(150.0,$done/$target*100.0),2);
    }

    private static function junior(array $payload, array $coverage, array $goals): ?float
    {
        $goals=match((string)($payload['goal']??'')) {
            'DISENO_GRAFICO'=>['POST'=>$goals[0]??0],
            'PRODUCCION_AUDIOVISUAL'=>['VIDEO'=>$goals[1]??0],
            'PRODUCCION_MIXTA'=>['POST'=>$goals[2]??0,'VIDEO'=>$goals[3]??0],
            default=>null,
        };
        if ($goals===null || !isset($payload['activities']) || !is_array($payload['activities'])) return null;
        $counts=['POST'=>0,'VIDEO'=>0];
        foreach ($payload['activities'] as $item) {
            if (!is_array($item) || ($item['status']??null)!=='ENTREGADO') continue;
            $kind=(string)($item['type']??'');
            if (isset($counts[$kind])) $counts[$kind]++;
        }
        $actual=0;
        foreach ($goals as $kind=>$goal) $actual+=min($goal,$counts[$kind]);
        $target=array_sum($goals)*(int)$coverage['turnos_exigibles']/(int)$coverage['turnos_programados'];
        return round(min(150.0,$actual/$target*100.0),2);
    }
}
