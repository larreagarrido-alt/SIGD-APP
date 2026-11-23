<?php
declare(strict_types=1);
namespace WebSitePlus\Kpi;

use PDO;
use Throwable;
use WebSitePlus\Core\Database;

/** Archives finished KPI months when all received team reports have been resolved. */
final class KpiMonthlyAutoClose
{
    public static function run(): void
    {
        $db=Database::getConnection();
        $months=$db->prepare('SELECT DISTINCT DATE_FORMAT(e.fecha_limite,"%Y-%m") AS month_key
            FROM wp_kpi_evaluaciones e
            LEFT JOIN wp_ranking_cierres c ON c.periodo_clave=DATE_FORMAT(e.fecha_limite,"%Y-%m")
            WHERE e.fecha_limite<? AND c.id_cierre IS NULL
            ORDER BY month_key ASC LIMIT 12');
        $months->execute([date('Y-m-01 00:00:00')]);
        foreach ($months->fetchAll(PDO::FETCH_COLUMN) as $month) {
            try { self::closeIfReady((string)$month); }
            catch (Throwable $error) {
                // Ranking reads must remain available if archiving encounters a server problem.
                error_log('SIGD monthly ranking close failed: '.get_class($error));
            }
        }
    }

    private static function closeIfReady(string $month): void
    {
        Database::transaction(function (PDO $db) use ($month): void {
            $closed=$db->prepare('SELECT id_cierre FROM wp_ranking_cierres WHERE periodo_clave=? FOR UPDATE');
            $closed->execute([$month]);
            if ($closed->fetchColumn()) return;
            $start=$month.'-01 00:00:00';
            $end=date('Y-m-d 00:00:00',strtotime($start.' +1 month'));
            $unreviewed=$db->prepare('SELECT 1 FROM wp_kpi_formularios_detalle f
                WHERE f.creado_en>=? AND f.creado_en<? AND f.id_cargo<>1
                  AND NOT EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l
                                  WHERE l.id_detalle=f.id_detalle) LIMIT 1');
            $unreviewed->execute([$start,$end]);
            if ($unreviewed->fetchColumn()) return;
            $rows=$db->prepare('SELECT e.id_evaluacion,e.id_trabajador,e.estado,e.porcentaje_cumplimiento,
                e.puntos_otorgados,e.peso_snapshot,e.fecha_calificacion,v.sources_ok,v.cargo_del_reporte
                FROM wp_kpi_evaluaciones e JOIN (
                    SELECT l.id_evaluacion,MIN(l.payload_sha256=f.payload_sha256) AS sources_ok,
                           MIN(f.id_cargo) AS cargo_del_reporte
                    FROM wp_kpi_reporte_evaluaciones l
                    JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                    GROUP BY l.id_evaluacion
                ) v ON v.id_evaluacion=e.id_evaluacion
                WHERE e.fecha_limite>=? AND e.fecha_limite<?
                ORDER BY e.id_trabajador,e.id_evaluacion FOR UPDATE');
            $rows->execute([$start,$end]);
            $evaluations=$rows->fetchAll(PDO::FETCH_ASSOC);
            if (!$evaluations) return;
            $persons=[]; $approvedCount=0;
            foreach ($evaluations as $evaluation) {
                if ((int)$evaluation['cargo_del_reporte']===1 || $evaluation['estado']==='Observado') continue;
                if ($evaluation['estado']!=='Aprobado'
                    || $evaluation['porcentaje_cumplimiento']===null
                    || $evaluation['puntos_otorgados']===null
                    || $evaluation['fecha_calificacion']===null
                    || !(int)$evaluation['sources_ok']) return;
                $weight=(float)$evaluation['peso_snapshot'];
                if ($weight<=0) return;
                $id=(int)$evaluation['id_trabajador'];
                $persons[$id]??=['weight'=>0.0,'weighted'=>0.0,'points'=>0,'count'=>0];
                $persons[$id]['weight']+=$weight;
                $persons[$id]['weighted']+=$weight*(float)$evaluation['porcentaje_cumplimiento'];
                $persons[$id]['points']+=(int)$evaluation['puntos_otorgados'];
                $persons[$id]['count']++;
                $approvedCount++;
            }
            $rank=[];
            foreach ($persons as $id=>$values) {
                $rank[]=['worker'=>$id,
                    'percent'=>round($values['weighted']/$values['weight'],2),
                    'points'=>$values['points'],'count'=>$values['count']];
            }
            usort($rank,static fn(array $a,array $b): int =>
                $b['percent']<=>$a['percent'] ?: $b['points']<=>$a['points']
                ?: $a['worker']<=>$b['worker']);
            $policy=$db->prepare('INSERT IGNORE INTO wp_ranking_politicas
                (periodo_clave,formula,comparacion_cargos_aprobada,justificacion,aprobado_por)
                VALUES (?,"PROMEDIO_PONDERADO",1,?,0)');
            $policy->execute([$month,
                'Regla SIGD: comparar porcentajes calculados sobre las metas oficiales de cada cargo; desempatar por puntos aprobados.']);
            $policyId=$db->prepare('SELECT id_politica FROM wp_ranking_politicas
                WHERE periodo_clave=? AND comparacion_cargos_aprobada=1 FOR UPDATE');
            $policyId->execute([$month]);
            $idPolicy=(int)$policyId->fetchColumn();
            if ($idPolicy<=0) return;
            $digest=hash('sha256',json_encode($evaluations,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES));
            $insertClose=$db->prepare('INSERT INTO wp_ranking_cierres
                (periodo_clave,id_politica,evaluaciones_aprobadas,participantes,origen_sha256,cerrado_por)
                VALUES (?,?,?,?,?,0)');
            $insertClose->execute([$month,$idPolicy,$approvedCount,count($rank),$digest]);
            $insert=$db->prepare('INSERT INTO wp_ranking_historico
                (periodo_tipo,periodo_clave,id_trabajador,puntos_kpi_periodo,porcentaje_cumplimiento,
                 kpis_asignados,kpis_aprobados,puesto,medalla,cerrado_por)
                VALUES ("MENSUAL",?,?,?,?,?,?,?,?,0)');
            $previous=null; $position=0;
            foreach ($rank as $index=>$entry) {
                $key=$entry['percent'].':'.$entry['points'];
                if ($key!==$previous) $position=$index+1;
                $previous=$key;
                $medal=match($position){1=>'ORO',2=>'PLATA',3=>'BRONCE',default=>'NINGUNA'};
                $insert->execute([$month,$entry['worker'],$entry['points'],$entry['percent'],
                    $entry['count'],$entry['count'],$position,$medal]);
            }
        });
    }
}
