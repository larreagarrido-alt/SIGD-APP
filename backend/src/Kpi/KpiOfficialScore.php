<?php
declare(strict_types=1);
namespace WebSitePlus\Kpi;

/** Recomputes the percentage from raw form counts and the goals shipped with each role. */
final class KpiOfficialScore
{
    private const DAILY_TOTALS = [
        'ADMINISTRACION'=>[4,4,4,4,4,2],
        'ASISTENCIA_ADMIN'=>[23,11,11,9,14,4],
        'CONTABILIDAD'=>[45,17,24,12,8,12],
        'COMMUNITY'=>[11,14,13,12,9,6],
        'FOTOGRAFIA'=>[3,3,3,3,3,2],
        'TRAFFICKER'=>[1,1,1,1,1,1],
        'VIDEO'=>[1,1,1,1,1,1],
    ];
    private const WEEKLY_TOTALS = ['DISENADOR'=>25,'DISENADOR_SENIOR'=>24,'VIDEO'=>10];
    private const SALES_GOALS = ['LLAMADAS'=>100,'VISITAS'=>60,'COTIZACIONES'=>30,
        'SEGUIMIENTOS'=>50,'PROSPECTOS'=>15,'CLIENTES'=>3];
    private const PROGRAM_GOALS = [
        1=>['Desarrollo',1],2=>['Corrección',2],3=>['Mejora',2],
        4=>['Maquetación',1],5=>['Integración / API',1],6=>['Mantenimiento Web',2],
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

    public static function percentage(string $type, array $payload, string $day, array $coverage): ?float
    {
        $period=self::period($type);
        if ($period===null || empty($coverage['dias'])) return null;
        $scheduled=(int)($coverage['turnos_programados']??0);
        $eligible=(int)($coverage['turnos_exigibles']??0);
        if ($scheduled<=0 || $eligible<=0) return null;
        if ($type==='PROGRAMACION') return self::programmer($payload,$day,$coverage);
        if ($type==='DISENADOR_JUNIOR') return self::junior($payload,$coverage);
        if ($type==='VENTAS') {
            $total=0.0;
            foreach (self::SALES_GOALS as $unit=>$goal) {
                $actual=KpiFormMeasurement::value($type,$payload,$unit,$day,$period);
                if ($actual===null) return null;
                $effective=$goal*$eligible/$scheduled;
                $total+=min(100.0,$actual/$effective*100.0);
            }
            return round($total/count(self::SALES_GOALS),2);
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
            return round(min(150.0,$done/max(1,count($eligibleDays))*100.0),2);
        }
        $actual=KpiFormMeasurement::value($type,$payload,'ACTIVIDADES',$day,$period);
        if ($actual===null) return null;
        if (isset(self::WEEKLY_TOTALS[$type])) {
            $target=self::WEEKLY_TOTALS[$type]*$eligible/$scheduled;
        } else {
            $goals=self::DAILY_TOTALS[$type]??null;
            if ($goals===null) return null;
            $target=0.0;
            foreach ($coverage['dias'] as $calendarDay) {
                $index=(int)date('N',strtotime((string)$calendarDay['fecha']))-1;
                if ($index<0 || $index>5 || empty($calendarDay['turnos_programados'])) continue;
                $target+=$goals[$index]*(int)$calendarDay['turnos_exigibles']/(int)$calendarDay['turnos_programados'];
            }
        }
        return $target>0 ? round(min(150.0,$actual/$target*100.0),2) : null;
    }

    private static function programmer(array $payload, string $day, array $coverage): ?float
    {
        $index=(int)date('N',strtotime($day));
        [$type,$goal]=self::PROGRAM_GOALS[$index]??[null,null];
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

    private static function junior(array $payload, array $coverage): ?float
    {
        $goals=match((string)($payload['goal']??'')) {
            'DISENO_GRAFICO'=>['POST'=>4],
            'PRODUCCION_AUDIOVISUAL'=>['VIDEO'=>2],
            'PRODUCCION_MIXTA'=>['POST'=>2,'VIDEO'=>1],
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
