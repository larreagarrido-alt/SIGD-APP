<?php
declare(strict_types=1);
namespace WebSitePlus\Kpi;

/** Extracts reported quantities. Goals, percentages and points in the phone payload are ignored. */
final class KpiFormMeasurement
{
    private const FIELDS = [
        'PROGRAMACION' => ['TAREAS'=>'tareasCompletadas', 'ACTIVIDADES'=>'tareasCompletadas'],
        'VENTAS' => ['LLAMADAS'=>'llamadas', 'VISITAS'=>'visitas', 'COTIZACIONES'=>'cotizaciones',
            'SEGUIMIENTOS'=>'seguimiento', 'PROSPECTOS'=>'prospectos', 'CLIENTES'=>'clientes',
            'ACTIVIDADES'=>['llamadas','visitas','cotizaciones','seguimiento','prospectos','clientes']],
        'DISENADOR_JUNIOR' => ['PIEZAS'=>'deliveredPieces', 'ACTIVIDADES'=>'deliveredPieces'],
        'DISENADOR' => ['DISENOS'=>'designsCompleted', 'VIDEOS'=>'videosCompleted',
            'FOTOS'=>'photosCompleted', 'ACTIVIDADES'=>['designsCompleted','videosCompleted','photosCompleted']],
        'DISENADOR_SENIOR' => ['DISENOS'=>'designsCount', 'VIDEOS'=>'videosCount',
            'SESIONES'=>'photoSessionsCount', 'ACTIVIDADES'=>['designsCount','videosCount','photoSessionsCount','otherActivitiesCount']],
        'ADMINISTRACION' => ['GESTIONES'=>'gestionesCount', 'SEGUIMIENTOS'=>'seguimientosCount',
            'REUNIONES'=>'reunionesCount', 'ACTIVIDADES'=>['gestionesCount','seguimientosCount','reunionesCount']],
        'ASISTENCIA_ADMIN' => ['GESTIONES'=>'gestionesCount', 'DOCUMENTOS'=>'documentosCount',
            'COORDINACIONES'=>'coordinacionesCount', 'ACTIVIDADES'=>['gestionesCount','documentosCount','coordinacionesCount']],
        'CONTABILIDAD' => ['REGISTROS'=>'registrosCount', 'CONCILIACIONES'=>'conciliacionesCount',
            'COMPROBANTES'=>'comprobantesCount', 'ACTIVIDADES'=>['registrosCount','conciliacionesCount','comprobantesCount']],
        'COMMUNITY' => ['PUBLICACIONES'=>'publicacionesCount', 'INTERACCIONES'=>'interaccionesCount',
            'ACTIVIDADES'=>['publicacionesCount','interaccionesCount','otrasActividadesCount']],
        'FOTOGRAFIA' => ['SESIONES'=>'sesionesCount', 'FOTOS'=>'fotosCapturadasCount',
            'FOTOS_EDITADAS'=>'fotosEditadasCount', 'ACTIVIDADES'=>['sesionesCount','fotosCapturadasCount','fotosEditadasCount']],
        'TRAFFICKER' => ['CAMPANAS'=>'campanasCount', 'AJUSTES'=>'ajustesCount',
            'REPORTES'=>'reportesCount', 'ACTIVIDADES'=>['campanasCount','ajustesCount','reportesCount']],
        'VIDEO' => ['VIDEOS'=>'videosGrabadosCount', 'VIDEOS_EDITADOS'=>'videosEditadosCount',
            'AJUSTES'=>'ajustesCorreccionesCount', 'ACTIVIDADES'=>['videosGrabadosCount','videosEditadosCount','ajustesCorreccionesCount']],
        'GERENCIA' => ['ASUNTOS'=>'priorityCount'],
    ];

    public static function supports(string $type, string $unit): bool
    {
        return isset(self::FIELDS[$type][strtoupper(trim($unit))]);
    }

    public static function supportsUnit(string $unit): bool
    {
        foreach (array_keys(self::FIELDS) as $type)
            if (self::supports($type,$unit)) return true;
        return false;
    }

    public static function value(string $type, array $payload, string $unit, string $day, string $period): ?float
    {
        $fields=self::FIELDS[$type][strtoupper(trim($unit))]??null;
        if ($fields===null) return null;
        $fields=(array)$fields;
        $units=KpiFormCompletion::units($type,$payload);
        $rows=isset($payload['days']) && is_array($payload['days']) ? $payload['days'] : [$payload];
        $total=0.0;
        $matched=false;
        foreach ($rows as $row) {
            if (!is_array($row)) continue;
            $date=self::date((string)($row['date']??$row['fecha']??$row['fullDateFormatted']??''));
            if ($date===null || !self::inPeriod($date,$day,$period)) continue;
            $completed=false;
            foreach ($units as $key=>$_) if (str_starts_with($key,$date.':')) { $completed=true; break; }
            if (!$completed) continue;
            foreach ($fields as $field) {
                $number=$row[$field]??null;
                if (!is_int($number) && !is_float($number)) return null;
                if (!is_finite((float)$number) || $number<0 || $number>99999999) return null;
                $total+=(float)$number;
            }
            $matched=true;
        }
        return $matched && $total<=99999999 ? $total : null;
    }

    private static function date(string $raw): ?string
    {
        if (preg_match('/^\d{4}-\d{2}-\d{2}$/D',$raw)) $date=$raw;
        elseif (preg_match('/^(\d{2})\s*\/\s*(\d{2})\s*\/\s*(\d{4})$/D',$raw,$m)) $date=$m[3].'-'.$m[2].'-'.$m[1];
        elseif (preg_match('/^[^,]+,\s*(\d{1,2}) de ([[:alpha:]áéíóúñ]+) de (\d{4})$/iuD',$raw,$m)) {
            $months=['enero'=>1,'febrero'=>2,'marzo'=>3,'abril'=>4,'mayo'=>5,'junio'=>6,
                'julio'=>7,'agosto'=>8,'septiembre'=>9,'setiembre'=>9,'octubre'=>10,'noviembre'=>11,'diciembre'=>12];
            $month=$months[mb_strtolower($m[2])]??0;
            if (!$month) return null;
            $date=sprintf('%04d-%02d-%02d',(int)$m[3],$month,(int)$m[1]);
        } else return null;
        $parsed=\DateTimeImmutable::createFromFormat('!Y-m-d',$date);
        return $parsed && $parsed->format('Y-m-d')===$date ? $date : null;
    }

    private static function inPeriod(string $date, string $day, string $period): bool
    {
        return match($period) {
            'DIARIO'=>$date===$day,
            'SEMANAL'=>date('o-\WW',strtotime($date))===date('o-\WW',strtotime($day)),
            'MENSUAL'=>substr($date,0,7)===substr($day,0,7),
            default=>false,
        };
    }
}
