<?php
declare(strict_types=1);
namespace WebSitePlus\Kpi;

/** Read only confirmation of daily form submissions; never awards points. */
final class KpiFormCompletion
{
    public static function units(string $type, array $payload): array
    {
        $units=[];
        if ($type==='PROGRAMACION') {
            self::add($units,$payload['fecha']??null,$payload['turno']??null);
        } elseif ($type==='VENTAS') {
            if (!empty($payload['saved'])) self::add($units,$payload['fullDateFormatted']??null,null);
            foreach (($payload['days']??[]) as $day)
                if (is_array($day) && !empty($day['saved']))
                    self::add($units,$day['fullDateFormatted']??null,null);
        } elseif ($type==='DISENADOR_JUNIOR') {
            if (!empty($payload['biometricVerified'])) self::add($units,$payload['date']??null,$payload['shift']??null);
        } else {
            foreach (($payload['days']??[]) as $day) {
                if (!is_array($day)) continue;
                $complete=match($type) {
                    'DISENADOR'=>!empty($day['biometricVerified']),
                    'DISENADOR_SENIOR','GERENCIA'=>!empty($day['isRegistered']),
                    default=>!empty($day['registered']),
                };
                if ($complete) self::add($units,$day['date']??null,null);
            }
        }
        ksort($units);
        return $units;
    }

    private static function add(array &$units, mixed $rawDate, mixed $rawShift): void
    {
        if (!is_string($rawDate)) return;
        $rawDate=trim($rawDate);
        if (preg_match('/^\d{4}-\d{2}-\d{2}$/D',$rawDate)) $date=$rawDate;
        elseif (preg_match('/^(\d{2})\s*\/\s*(\d{2})\s*\/\s*(\d{4})$/D',$rawDate,$m)) $date=$m[3].'-'.$m[2].'-'.$m[1];
        elseif (preg_match('/^[^,]+,\s*(\d{1,2}) de ([[:alpha:]áéíóúñ]+) de (\d{4})$/iuD',$rawDate,$m)) {
            $months=['enero'=>1,'febrero'=>2,'marzo'=>3,'abril'=>4,'mayo'=>5,'junio'=>6,
                'julio'=>7,'agosto'=>8,'septiembre'=>9,'setiembre'=>9,'octubre'=>10,'noviembre'=>11,'diciembre'=>12];
            $month=$months[mb_strtolower($m[2])]??0;
            if (!$month) return;
            $date=sprintf('%04d-%02d-%02d',(int)$m[3],$month,(int)$m[1]);
        }
        else return;
        $parsed=\DateTimeImmutable::createFromFormat('!Y-m-d',$date);
        if (!$parsed || $parsed->format('Y-m-d')!==$date) return;
        $shift=is_string($rawShift)?mb_strtoupper(trim($rawShift)):'';
        $shift=(str_contains($shift,'TARDE')||str_contains($shift,'AFTERNOON'))?'TARDE':
            ((str_contains($shift,'MAÑANA')||str_contains($shift,'MANANA')||str_contains($shift,'MORNING'))?'MANANA':'DIA');
        $units[$date.':'.$shift]=true;
    }

    public static function pending(array $calendar, array $completed, string $today): int
    {
        $count=0;
        foreach (($calendar['dias']??[]) as $day) {
            if (!is_array($day) || !isset($day['fecha']) || $day['fecha']>$today
                || !empty($day['permiso_aprobado'])) continue;
            $date=(string)$day['fecha'];
            if (!empty($completed[$date.':DIA'])) continue;
            if (!empty($day['manana']) && empty($completed[$date.':MANANA'])) $count++;
            if (!empty($day['tarde']) && empty($completed[$date.':TARDE'])) $count++;
        }
        return $count;
    }
}
