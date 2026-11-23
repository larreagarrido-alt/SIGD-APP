<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
use WebSitePlus\Kpi\KpiMonthlyAutoClose;

/** Current-month ranking uses approved evaluations; finished months use archived snapshots. */
final class RankingController
{
    private static function access(array $session): array
    {
        $worker = (int) ($session['id_trabajador'] ?? 0);
        if ($worker <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        $scope = Authorization::permissions($session)['ranking.ver'] ?? null;
        if (!in_array($scope, ['PROPIO', 'EQUIPO', 'AGENCIA', 'TODOS'], true))
            Response::error('No autorizado para este recurso', 403, 'FORBIDDEN');
        $db = Database::getConnection();
        $visibility = $db->query("SELECT valor FROM wp_configuracion WHERE clave='ranking_visibilidad'")->fetchColumn();
        if (!in_array($visibility, ['EMPRESA', 'AGENCIA'], true)) $visibility = 'PROPIO';
        return [$db, $worker, $scope, $visibility];
    }

    private static function restrict(string &$sql, array &$params, array $session, int $worker,
                                     string $scope, string $visibility): void
    {
        if ($visibility === 'PROPIO' || $scope === 'PROPIO') {
            $sql .= ' AND r.id_trabajador=?';
            $params[] = $worker;
        } elseif ($scope === 'EQUIPO') {
            $sql .= ' AND (r.id_trabajador=? OR t.id_supervisor=?)';
            array_push($params, $worker, $worker);
        }
        if ($visibility === 'AGENCIA' || $scope === 'AGENCIA') {
            $agency = (int) ($session['id_agencia'] ?? 0);
            if ($agency <= 0) Response::error('Agencia no vinculada', 403, 'FORBIDDEN');
            $sql .= ' AND t.id_agencia=?';
            $params[] = $agency;
        }
    }

    private static function type(Request $request): string
    {
        $type = $request->getQuery('periodo_tipo', 'MENSUAL');
        if (!is_string($type) || !in_array($type, ['MENSUAL', 'TRIMESTRAL', 'ANUAL'], true))
            Response::error('Periodo no disponible para el ranking cerrado', 422, 'VALIDATION_FAILED');
        return $type;
    }

    public function periods(Request $request, array $session): void
    {
        [$db] = self::access($session);
        KpiMonthlyAutoClose::run();
        $rows=$db->query('SELECT periodo_clave,cerrado_en,participantes,evaluaciones_aprobadas
            FROM wp_ranking_cierres ORDER BY periodo_clave DESC LIMIT 24')->fetchAll(PDO::FETCH_ASSOC);
        Response::success(['periodos'=>$rows]);
    }

    public function latest(Request $request, array $session): void
    {
        [$db, $worker, $scope, $visibility] = self::access($session);
        KpiMonthlyAutoClose::run();
        $type = self::type($request);
        $selected=$request->getQuery('periodo_clave');
        if ($selected!==null && (!is_string($selected)
            || !preg_match('/^20\d{2}-(0[1-9]|1[0-2])$/D',$selected)))
            Response::error('Mes invalido',422,'VALIDATION_FAILED');
        $currentMonth=date('Y-m');
        if ($type==='MENSUAL' && ($selected===null || $selected===$currentMonth)) {
            self::liveMonth($db,$session,$worker,$scope,$visibility,$currentMonth);
            return;
        }
        $periods = $db->prepare('SELECT periodo_clave FROM wp_ranking_historico WHERE periodo_tipo=? GROUP BY periodo_clave ORDER BY periodo_clave DESC LIMIT 2');
        $periods->execute([$type]);
        $keys = $periods->fetchAll(PDO::FETCH_COLUMN);
        if ($selected!==null) {
            $keys = [$selected];
            $prior=$db->prepare('SELECT MAX(periodo_clave) FROM wp_ranking_historico
                WHERE periodo_tipo=? AND periodo_clave<?');
            $prior->execute([$type,$selected]);
            $priorKey=$prior->fetchColumn();
            if ($priorKey) $keys[]=$priorKey;
        }
        if (!$keys) Response::success(['periodo_tipo' => $type, 'periodo_clave' => null,
            'visibilidad' => $visibility, 'id_trabajador' => $worker, 'ranking' => [],
            'resumen' => ['participantes' => 0, 'promedio_cumplimiento' => null, 'evaluaciones_completadas' => 0]]);
        $sql = 'SELECT r.id_trabajador,r.puesto,r.puntos_kpi_periodo,r.porcentaje_cumplimiento,
                       r.kpis_asignados,r.kpis_aprobados,r.medalla,
                       CONCAT(t.nombres," ",t.apellidos) AS nombre_completo,
                       t.tipo,t.id_agencia,a.nombre AS agencia_nombre,t.id_cargo,c.nombre AS cargo_nombre
                FROM wp_ranking_historico r JOIN trabajadores t ON t.id_trabajador=r.id_trabajador
                LEFT JOIN cargos c ON c.id_cargo=t.id_cargo
                LEFT JOIN agencias a ON a.id_agencia=t.id_agencia
                WHERE r.periodo_tipo=? AND r.periodo_clave=?';
        $params = [$type, $keys[0]];
        self::restrict($sql, $params, $session, $worker, $scope, $visibility);
        $sql .= ' ORDER BY r.puesto ASC,r.id_trabajador ASC';
        $st = $db->prepare($sql);
        $st->execute($params);
        $rows = $st->fetchAll(PDO::FETCH_ASSOC);
        $previous = [];
        if (count($keys) > 1 && $rows) {
            $oldSql = 'SELECT r.id_trabajador,r.puesto FROM wp_ranking_historico r JOIN trabajadores t ON t.id_trabajador=r.id_trabajador WHERE r.periodo_tipo=? AND r.periodo_clave=?';
            $oldParams = [$type, $keys[1]];
            self::restrict($oldSql, $oldParams, $session, $worker, $scope, $visibility);
            $old = $db->prepare($oldSql);
            $old->execute($oldParams);
            foreach ($old as $row) $previous[(int) $row['id_trabajador']] = (int) $row['puesto'];
        }
        $sum = 0.0;
        $approved = 0;
        foreach ($rows as &$row) {
            $row['puesto_anterior'] = $previous[(int) $row['id_trabajador']] ?? null;
            $sum += (float) $row['porcentaje_cumplimiento'];
            $approved += (int) $row['kpis_aprobados'];
        }
        unset($row);
        Response::success(['periodo_tipo' => $type, 'periodo_clave' => $keys[0],
            'visibilidad' => $visibility, 'id_trabajador' => $worker, 'ranking' => $rows,
            'resumen' => ['participantes' => count($rows),
                'promedio_cumplimiento' => $rows ? round($sum / count($rows), 2) : null,
                'evaluaciones_completadas' => $approved]]);
    }

    /** Approved evaluations form the current month's ranking without carrying over prior points. */
    private static function liveMonth(PDO $db, array $session, int $worker,
                                      string $scope, string $visibility, string $month): void
    {
        $start=$month.'-01 00:00:00';
        $end=date('Y-m-d 00:00:00',strtotime($start.' +1 month'));
        $st=$db->prepare('SELECT e.id_trabajador,
                   ROUND(SUM(e.porcentaje_cumplimiento*e.peso_snapshot)/SUM(e.peso_snapshot),2) AS porcentaje_cumplimiento,
                   SUM(e.puntos_otorgados) AS puntos_kpi_periodo,COUNT(*) AS kpis_aprobados,
                   CONCAT(t.nombres," ",t.apellidos) AS nombre_completo,t.tipo,t.id_agencia,
                   t.id_supervisor,a.nombre AS agencia_nombre,t.id_cargo,c.nombre AS cargo_nombre
            FROM wp_kpi_evaluaciones e
            JOIN trabajadores t ON t.id_trabajador=e.id_trabajador
            LEFT JOIN cargos c ON c.id_cargo=t.id_cargo
            LEFT JOIN agencias a ON a.id_agencia=t.id_agencia
            WHERE e.estado="Aprobado" AND e.fecha_calificacion IS NOT NULL
              AND e.porcentaje_cumplimiento IS NOT NULL AND e.puntos_otorgados IS NOT NULL
              AND e.peso_snapshot>0 AND e.fecha_limite>=? AND e.fecha_limite<?
              AND EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l
                  JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                  WHERE l.id_evaluacion=e.id_evaluacion AND f.id_cargo<>1
                    AND l.payload_sha256=f.payload_sha256)
              AND NOT EXISTS (SELECT 1 FROM wp_kpi_reporte_evaluaciones l
                  JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                  WHERE l.id_evaluacion=e.id_evaluacion AND l.payload_sha256<>f.payload_sha256)
            GROUP BY e.id_trabajador,t.nombres,t.apellidos,t.tipo,t.id_agencia,
                     t.id_supervisor,a.nombre,t.id_cargo,c.nombre');
        $st->execute([$start,$end]);
        $all=$st->fetchAll(PDO::FETCH_ASSOC);
        usort($all,static fn(array $a,array $b): int =>
            (float)$b['porcentaje_cumplimiento']<=>(float)$a['porcentaje_cumplimiento']
            ?: (int)$b['puntos_kpi_periodo']<=>(int)$a['puntos_kpi_periodo']
            ?: (int)$a['id_trabajador']<=>(int)$b['id_trabajador']);
        $previous=null; $position=0; $visible=[]; $totalPercent=0.0; $approved=0;
        foreach ($all as $index=>$row) {
            $key=$row['porcentaje_cumplimiento'].':'.$row['puntos_kpi_periodo'];
            if ($key!==$previous) $position=$index+1;
            $previous=$key;
            $id=(int)$row['id_trabajador'];
            if (($visibility==='PROPIO' || $scope==='PROPIO') && $id!==$worker) continue;
            if ($scope==='EQUIPO' && $id!==$worker && (int)$row['id_supervisor']!==$worker) continue;
            if ($visibility==='AGENCIA' || $scope==='AGENCIA') {
                $agency=(int)($session['id_agencia']??0);
                if ($agency<=0) Response::error('Agencia no vinculada',403,'FORBIDDEN');
                if ((int)$row['id_agencia']!==$agency) continue;
            }
            $row['puesto']=$position;
            $row['medalla']=match($position){1=>'ORO',2=>'PLATA',3=>'BRONCE',default=>'NINGUNA'};
            $row['kpis_asignados']=$row['kpis_aprobados'];
            $row['puesto_anterior']=null;
            unset($row['id_supervisor']);
            $totalPercent+=(float)$row['porcentaje_cumplimiento'];
            $approved+=(int)$row['kpis_aprobados'];
            $visible[]=$row;
        }
        Response::success(['periodo_tipo'=>'MENSUAL','periodo_clave'=>$month,
            'visibilidad'=>$visibility,'id_trabajador'=>$worker,'ranking'=>$visible,
            'resumen'=>['participantes'=>count($visible),
                'promedio_cumplimiento'=>$visible ? round($totalPercent/count($visible),2) : null,
                'evaluaciones_completadas'=>$approved]]);
    }

    public function detail(Request $request, array $session, int $target): void
    {
        [$db, $worker, $scope, $visibility] = self::access($session);
        if ($target <= 0 || !Authorization::allows($session, 'ranking.ver', $target))
            Response::error('No autorizado para este trabajador', 403, 'FORBIDDEN');
        $type = self::type($request);
        $sql = 'SELECT r.periodo_clave,r.puesto,r.puntos_kpi_periodo,r.porcentaje_cumplimiento,
                       r.kpis_asignados,r.kpis_aprobados,r.medalla
                FROM wp_ranking_historico r JOIN trabajadores t ON t.id_trabajador=r.id_trabajador
                WHERE r.periodo_tipo=? AND r.id_trabajador=?';
        $params = [$type, $target];
        self::restrict($sql, $params, $session, $worker, $scope, $visibility);
        $sql .= ' ORDER BY r.cerrado_en DESC,r.id_ranking DESC LIMIT 12';
        $st = $db->prepare($sql);
        $st->execute($params);
        $history = $st->fetchAll(PDO::FETCH_ASSOC);
        if (!$history) Response::error('Ranking no encontrado', 404, 'NOT_FOUND');
        $evaluations = [];
        if (Authorization::allows($session, 'kpi.ver', $target)) {
            $q = $db->prepare("SELECT e.periodo_clave,d.codigo,d.nombre,e.estado,e.porcentaje_cumplimiento,e.puntos_otorgados
                               FROM wp_kpi_evaluaciones e JOIN wp_kpi_definiciones d ON d.id_kpi=e.id_kpi
                               WHERE e.id_trabajador=? AND e.estado='Aprobado'
                               ORDER BY e.fecha_calificacion DESC,e.id_evaluacion DESC LIMIT 30");
            $q->execute([$target]);
            $evaluations = $q->fetchAll(PDO::FETCH_ASSOC);
        }
        Response::success(['id_trabajador' => $target, 'periodo_tipo' => $type,
            'evolucion' => $history, 'evaluaciones' => $evaluations]);
    }
}
