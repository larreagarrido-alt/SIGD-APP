<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Auth\KpiLaborCalendar;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;
use WebSitePlus\Kpi\KpiFormMeasurement;
use WebSitePlus\Kpi\KpiFormCompletion;
use WebSitePlus\Kpi\KpiOfficialScore;
use WebSitePlus\Kpi\KpiPointsPolicy;

/** Uses the existing definition, assignment, evaluation, review and ranking tables. */
final class KpiOfficialAdminController
{
    private static function denied(string $message, int $status = 422): never
    {
        throw new HttpException($message, $status, $status === 409 ? 'CONFLICT' : 'VALIDATION_FAILED');
    }

    public function definitions(Request $request, array $session): void
    {
        Authorization::require($session, 'kpi.administrar');
        $rows = Database::getConnection()->query('SELECT id_kpi,codigo,version,id_categoria,nombre,descripcion,
            tipo_metrica,unidad_medida,meta_esperada,peso,puntos_base,periodicidad,id_cargo_objetivo,
            responsable_revision,activo,regla_aprobada_por,regla_aprobada_en
            FROM wp_kpi_definiciones ORDER BY id_kpi DESC')->fetchAll(PDO::FETCH_ASSOC);
        Response::success(['definiciones'=>$rows]);
    }

    public function createDefinition(Request $request, array $session): void
    {
        Authorization::require($session, 'kpi.administrar');
        $code=trim((string)$request->input('codigo',''));
        $name=trim((string)$request->input('nombre',''));
        $description=trim((string)$request->input('descripcion',''));
        $unit=trim((string)$request->input('unidad_medida',''));
        $kind=(string)$request->input('tipo_metrica','');
        $period=(string)$request->input('periodicidad','');
        $category=filter_var($request->input('id_categoria'), FILTER_VALIDATE_INT);
        $cargo=filter_var($request->input('id_cargo_objetivo'), FILTER_VALIDATE_INT);
        $reviewer=filter_var($request->input('responsable_revision'), FILTER_VALIDATE_INT);
        $meta=self::positive($request->input('meta_esperada'));
        $weight=self::positive($request->input('peso'));
        $points=filter_var($request->input('puntos_base'), FILTER_VALIDATE_INT);
        if (!preg_match('/^[A-Z0-9_.-]{3,50}$/D',$code) || mb_strlen($name)<3 || mb_strlen($name)>150
            || $description==='' || mb_strlen($description)>10000 || $unit==='' || mb_strlen($unit)>30
            || !in_array($kind,['NUMERICO','PORCENTAJE','BOOLEANO','ESCALA_1_5'],true)
            || !in_array($period,['DIARIO','SEMANAL','MENSUAL'],true)
            || !$category || !$cargo || !$reviewer || $meta===null || $weight===null
            || $weight>100 || !is_int($points) || $points<0 || $points>100000)
            self::denied('Complete todos los valores oficiales del indicador');
        $db=Database::getConnection();
        if ($kind!=='NUMERICO') self::denied('La puntuacion automatica requiere un indicador numerico');
        if (!KpiFormMeasurement::supportsUnit($unit))
            self::denied('La unidad no corresponde a una cantidad medible de los formularios');
        $cat=$db->prepare('SELECT 1 FROM wp_kpi_categorias WHERE id_categoria=? AND activo=1');
        $cat->execute([$category]); if (!$cat->fetchColumn()) self::denied('Categoria invalida');
        $staff=Database::getAttendanceConnection();
        $c=$staff->prepare('SELECT 1 FROM cargos WHERE id_cargo=?'); $c->execute([$cargo]);
        if (!$c->fetchColumn()) self::denied('Cargo inexistente');
        $u=$staff->prepare('SELECT 1 FROM usuarios WHERE id_usuario=?'); $u->execute([$reviewer]);
        if (!$u->fetchColumn()) self::denied('Responsable inexistente');
        $st=$db->prepare('INSERT INTO wp_kpi_definiciones
            (codigo,id_categoria,nombre,descripcion,tipo_metrica,unidad_medida,meta_esperada,peso,puntos_base,
             periodicidad,aplica_a,id_cargo_objetivo,responsable_revision,activo,creado_por)
             VALUES (?,?,?,?,?,?,?,?,?, ?,"CARGO",?,?,0,?)');
        try { $st->execute([$code,$category,$name,$description,$kind,$unit,$meta,$weight,$points,
            $period,$cargo,$reviewer,(int)$session['id_usuario']]); }
        catch (\PDOException $e) { if ($e->getCode()==='23000') self::denied('Codigo KPI duplicado',409); throw $e; }
        Response::success(['id_kpi'=>(int)$db->lastInsertId(),'estado'=>'PENDIENTE_APROBACION']);
    }

    public function approveDefinition(Request $request, array $session, int $id): void
    {
        Authorization::require($session, 'kpi.administrar');
        Database::transaction(function (PDO $db) use ($id,$session): void {
            $st=$db->prepare('UPDATE wp_kpi_definiciones SET activo=1,regla_aprobada_por=?,regla_aprobada_en=NOW()
                WHERE id_kpi=? AND regla_aprobada_en IS NULL AND responsable_revision IS NOT NULL');
            $st->execute([(int)$session['id_usuario'],$id]);
            if ($st->rowCount()!==1) self::denied('Indicador inexistente o ya aprobado',409);
            $cargo=$db->prepare('SELECT id_cargo_objetivo FROM wp_kpi_definiciones WHERE id_kpi=?');
            $cargo->execute([$id]);
            $idCargo=(int)$cargo->fetchColumn();
            $alert=$db->prepare('INSERT INTO wp_horarios_alertas (id_cargo,revision)
                SELECT id_cargo,revision FROM wp_horarios_cargo WHERE id_cargo=?
                ON DUPLICATE KEY UPDATE estado="PENDIENTE",creada_en=NOW(),atendida_por=NULL,atendida_en=NULL');
            $alert->execute([$idCargo]);
        });
        Response::success(['id_kpi'=>$id,'estado'=>'APROBADO']);
    }

    public function assign(Request $request, array $session): void
    {
        Authorization::require($session, 'kpi.administrar');
        $worker=filter_var($request->input('id_trabajador'), FILTER_VALIDATE_INT);
        $kpi=filter_var($request->input('id_kpi'), FILTER_VALIDATE_INT);
        $start=(string)$request->input('fecha_inicio','');
        if (!$worker || !$kpi || !self::day($start)) self::denied('Asignacion invalida');
        $staff=Database::getAttendanceConnection()->prepare('SELECT id_cargo FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
        $staff->execute([$worker]); $cargo=(int)$staff->fetchColumn();
        $db=Database::getConnection();
        $st=$db->prepare('SELECT id_cargo_objetivo FROM wp_kpi_definiciones WHERE id_kpi=? AND activo=1 AND regla_aprobada_en IS NOT NULL');
        $st->execute([$kpi]); $target=(int)$st->fetchColumn();
        if (!$cargo || !$target || $cargo!==$target) self::denied('Indicador no aprobado para el cargo');
        $st=$db->prepare('INSERT INTO wp_kpi_asignaciones (id_trabajador,id_kpi,fecha_inicio,asignado_por) VALUES (?,?,?,?)');
        try { $st->execute([$worker,$kpi,$start,(int)$session['id_usuario']]); }
        catch (\PDOException $e) { if ($e->getCode()==='23000') self::denied('Asignacion duplicada',409); throw $e; }
        Response::success(['id_asignacion'=>(int)$db->lastInsertId()]);
    }

    public function received(Request $request, array $session): void
    {
        if (!isset(Authorization::permissions($session)['kpi.revisar']))
            Response::error('No autorizado para revisar reportes',403,'FORBIDDEN');
        $db=Database::getConnection();
        $rows=$db->query('SELECT f.id_detalle,f.id_trabajador,f.id_cargo,f.tipo_formulario,f.revision,
            f.payload_json,
            f.actualizado_en,COUNT(l.id_evaluacion) AS evaluaciones_vinculadas,
            MAX(e.id_evaluacion) AS id_evaluacion,MAX(e.estado) AS estado_evaluacion,
            MAX(e.porcentaje_cumplimiento) AS porcentaje_cumplimiento,
            MAX(e.puntos_otorgados) AS puntos_otorgados,
            (SELECT v.nombre_completo FROM vw_wp_empleados_estado v
                WHERE v.id_trabajador=f.id_trabajador LIMIT 1) AS nombre_completo
            FROM wp_kpi_formularios_detalle f
            LEFT JOIN wp_kpi_reporte_evaluaciones l ON l.id_detalle=f.id_detalle
            LEFT JOIN wp_kpi_evaluaciones e ON e.id_evaluacion=l.id_evaluacion
            GROUP BY f.id_detalle ORDER BY f.actualizado_en DESC LIMIT 200')->fetchAll(PDO::FETCH_ASSOC);
        $rows=array_values(array_filter($rows,fn(array $row): bool =>
            Authorization::allows($session,'kpi.revisar',(int)$row['id_trabajador'])));
        $approved=$db->query('SELECT id_cargo_objetivo,periodicidad FROM wp_kpi_definiciones
            WHERE activo=1 AND regla_aprobada_en IS NOT NULL AND tipo_metrica="PORCENTAJE"
              AND unidad_medida="CUMPLIMIENTO"')->fetchAll(PDO::FETCH_ASSOC);
        $rules=[];
        foreach ($approved as $rule) $rules[(int)$rule['id_cargo_objetivo']]=(string)$rule['periodicidad'];
        $coverageCache=[];
        foreach ($rows as &$row) {
            $type=(string)$row['tipo_formulario'];
            $period=KpiOfficialScore::period($type);
            $payload=json_decode((string)$row['payload_json'],true);
            unset($row['payload_json']);
            if (!is_array($payload) || $period===null || ($rules[(int)$row['id_cargo']]??null)!==$period) continue;
            $units=KpiFormCompletion::units($type,$payload);
            if (!$units) continue;
            $day=substr(max(array_keys($units)),0,10);
            $from=$period==='DIARIO' ? $day : date('Y-m-d',strtotime($day.' -'.((int)date('N',strtotime($day))-1).' days'));
            $to=$period==='DIARIO' ? $day : date('Y-m-d',strtotime($from.' +6 days'));
            $key=$row['id_trabajador'].':'.$from.':'.$to;
            try {
                $coverageCache[$key]??=KpiLaborCalendar::periodCoverage((int)$row['id_trabajador'],$from,$to);
                $scorePayload=$payload;
                if ($type==='VENTAS') [$scorePayload]=self::salesWeek($db,$row+['payload_json'=>json_encode($payload)],$day);
                $score=KpiOfficialScore::percentage($type,$scorePayload,$day,$coverageCache[$key]);
                if ($score!==null) $row['porcentaje_estimado']=$score;
            } catch (\Throwable $ignored) {
                // A report can still be inspected when Asistencias has not settled its calendar.
            }
        }
        unset($row);
        Response::success(['reportes'=>$rows]);
    }

    public function review(Request $request, array $session, int $report): void
    {
        $note=trim((string)$request->input('observacion',''));
        if (mb_strlen($note)>5000)
            self::denied('Revision incompleta');
        $db=Database::getConnection();
        $st=$db->prepare('SELECT * FROM wp_kpi_formularios_detalle WHERE id_detalle=?');
        $st->execute([$report]); $form=$st->fetch(PDO::FETCH_ASSOC);
        if (!$form) self::denied('Reporte inexistente');
        Authorization::require($session,'kpi.revisar',(int)$form['id_trabajador']);
        $payload=json_decode((string)$form['payload_json'],true);
        if (!is_array($payload)) self::denied('Formulario invalido');
        $units=KpiFormCompletion::units((string)$form['tipo_formulario'],$payload);
        if (!$units) self::denied('El formulario no tiene una jornada confirmada');
        $days=array_map(static fn(string $unit): string => substr($unit,0,10),array_keys($units));
        $day=max($days);
        if (!self::day($day) || $day>date('Y-m-d')) self::denied('Fecha del reporte invalida');
        $links=[$form];
        if ($form['tipo_formulario']==='VENTAS') {
            [$payload,$links]=self::salesWeek($db,$form,$day);
        }
        $rules=$db->prepare('SELECT d.* FROM wp_kpi_definiciones d WHERE d.id_cargo_objetivo=?
            AND d.activo=1 AND d.regla_aprobada_en IS NOT NULL ORDER BY d.id_kpi');
        $rules->execute([(int)$form['id_cargo']]);
        $ruleRows=$rules->fetchAll(PDO::FETCH_ASSOC);
        if (!$ruleRows) {
            self::provisionMappedCargo($db,$form,$session);
            $rules->execute([(int)$form['id_cargo']]);
            $ruleRows=$rules->fetchAll(PDO::FETCH_ASSOC);
        }
        $matches=[];
        foreach ($ruleRows as $rule) {
            $automatic=$rule['tipo_metrica']==='PORCENTAJE'
                && $rule['unidad_medida']==='CUMPLIMIENTO'
                && KpiOfficialScore::period((string)$form['tipo_formulario'])===$rule['periodicidad'];
            $numeric=$rule['tipo_metrica']==='NUMERICO'
                && KpiFormMeasurement::supports((string)$form['tipo_formulario'],(string)$rule['unidad_medida'])
                && KpiFormMeasurement::value((string)$form['tipo_formulario'],$payload,
                    (string)$rule['unidad_medida'],$day,(string)$rule['periodicidad'])!==null;
            if ($automatic || $numeric) $matches[]=$rule;
        }
        if (!$matches) self::denied('Este cargo aun no tiene una meta oficial compatible con el formulario',409);
        if (count($matches)>1) self::denied('Hay varias metas oficiales para este reporte; falta configurar su evaluacion conjunta',409);
        $row=array_merge($form,$matches[0]);
        $kpi=(int)$row['id_kpi'];
        if ((int)$row['id_trabajador']===(int)$session['id_trabajador'] || (int)$row['responsable_revision']!==(int)$session['id_usuario'])
            Response::error('Responsable de revision no autorizado',403,'FORBIDDEN');
        if (!(int)$row['activo'] || !$row['regla_aprobada_en'] || (int)$row['id_cargo']!==(int)$row['id_cargo_objetivo'])
            self::denied('Indicador no aprobado para el cargo');
        if (!in_array($row['tipo_metrica'],['NUMERICO','PORCENTAJE'],true))
            self::denied('El indicador requiere una medicion automatica del formulario');
        $period=$row['periodicidad'];
        $key=match($period) {
            'DIARIO'=>$day,
            'SEMANAL'=>date('o-\WW',strtotime($day)),
            'MENSUAL'=>substr($day,0,7),
            default=>null,
        };
        if (!$key) self::denied('Periodicidad no compatible');
        $range=match($period) {
            'DIARIO'=>[$day,$day],
            'SEMANAL'=>[date('Y-m-d',strtotime($day.' -'.((int)date('N',strtotime($day))-1).' days')),
                date('Y-m-d',strtotime($day.' +'.(7-(int)date('N',strtotime($day))).' days'))],
            'MENSUAL'=>[substr($day,0,7).'-01',date('Y-m-t',strtotime($day))],
        };
        $coverage=KpiLaborCalendar::periodCoverage((int)$row['id_trabajador'],$range[0],$range[1]);
        if (!$coverage['tipo_historico_confirmado'] || $coverage['incidencias']>0
            || $coverage['turnos_programados']===0 || $coverage['turnos_exigibles']===0)
            self::denied('Asistencias aun no confirma turnos KPI exigibles para este periodo',409);
        if ($row['tipo_metrica']==='PORCENTAJE' && $period==='SEMANAL' && $range[1]>=date('Y-m-d'))
            self::denied('La semana aun no ha terminado; no se puede cerrar su resultado',409);
        $value=$row['tipo_metrica']==='PORCENTAJE'
            ? KpiOfficialScore::percentage((string)$row['tipo_formulario'],$payload,$day,$coverage)
            : KpiFormMeasurement::value((string)$row['tipo_formulario'],$payload,
                (string)$row['unidad_medida'],$day,(string)$period);
        if ($value===null) self::denied('El formulario no contiene una medicion confirmada para este indicador y periodo');
        $effectiveMeta=$row['tipo_metrica']==='PORCENTAJE' ? (float)$row['meta_esperada']
            : max(0.01,round((float)$row['meta_esperada']
                *$coverage['turnos_exigibles']/$coverage['turnos_programados'],2));
        $percentage=min(150.0,round($value/$effectiveMeta*100,2));
        $projectedPoints=KpiPointsPolicy::forPercentage($percentage);
        $closed=$db->prepare('SELECT 1 FROM wp_ranking_cierres WHERE periodo_clave=?');
        $closed->execute([substr($day,0,7)]);
        if ($closed->fetchColumn()) self::denied('El mes ya esta cerrado',409);
        $assignment=$db->prepare('SELECT id_asignacion FROM wp_kpi_asignaciones WHERE id_trabajador=? AND id_kpi=?
            AND activo=1 AND fecha_inicio<=? AND (fecha_fin IS NULL OR fecha_fin>=?) ORDER BY fecha_inicio DESC LIMIT 1');
        $assignment->execute([(int)$row['id_trabajador'],$kpi,$day,$day]);
        $assignmentId=$assignment->fetchColumn();
        if (!$assignmentId) {
            $staff=Database::getAttendanceConnection()->prepare('SELECT id_cargo FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
            $staff->execute([(int)$row['id_trabajador']]);
            if ((int)$staff->fetchColumn()!==(int)$row['id_cargo_objetivo'])
                self::denied('El cargo actual no coincide con la regla aprobada; revise el historial laboral');
            $auto=$db->prepare('INSERT IGNORE INTO wp_kpi_asignaciones (id_trabajador,id_kpi,fecha_inicio,asignado_por) VALUES (?,?,?,?)');
            $auto->execute([(int)$row['id_trabajador'],$kpi,$day,(int)$session['id_usuario']]);
            $assignment->execute([(int)$row['id_trabajador'],$kpi,$day,$day]);
            $assignmentId=$assignment->fetchColumn();
            if (!$assignmentId) self::denied('No se pudo registrar la asignacion del cargo');
        }
        $end=match($period) {
            'DIARIO'=>$day.' 23:59:59',
            'SEMANAL'=>date('Y-m-d 23:59:59',strtotime($day.' +'.(7-(int)date('N',strtotime($day))).' days')),
            'MENSUAL'=>date('Y-m-t 23:59:59',strtotime($day)),
        };
        $id=Database::transaction(function(PDO $pdo) use($row,$report,$kpi,$value,$note,$day,$period,$key,$end,$assignmentId,$session,$links): int {
            $lock=$pdo->prepare('SELECT payload_sha256 FROM wp_kpi_formularios_detalle WHERE id_detalle=? FOR UPDATE');
            foreach ($links as $source) {
                $lock->execute([(int)$source['id_detalle']]);
                if ($lock->fetchColumn()!==$source['payload_sha256']) self::denied('Reporte modificado durante la revision',409);
            }
            $st=$pdo->prepare('INSERT INTO wp_kpi_evaluaciones
                (id_asignacion,id_trabajador,id_kpi,periodo_tipo,periodo_clave,fecha_limite,estado,
                 meta_snapshot,peso_snapshot,puntos_base_snapshot,valor_reportado,comentarios_entrega,fecha_entrega)
                VALUES (?,?,?,?,?,? ,"Entregado",?,?,?,?,?,NOW())');
            try { $st->execute([(int)$assignmentId,(int)$row['id_trabajador'],$kpi,$period,$key,$end,
                $row['meta_esperada'],$row['peso'],$row['puntos_base'],$value,$note]); }
            catch (\PDOException $e) { if ($e->getCode()==='23000') self::denied('Evaluacion duplicada para este periodo',409); throw $e; }
            $id=(int)$pdo->lastInsertId();
            $link=$pdo->prepare('INSERT INTO wp_kpi_reporte_evaluaciones (id_detalle,id_evaluacion,payload_sha256,fecha_evaluada,vinculado_por) VALUES (?,?,?,?,?)');
            foreach ($links as $source) {
                $sourcePayload=json_decode((string)$source['payload_json'],true);
                $sourceUnits=is_array($sourcePayload) ? KpiFormCompletion::units((string)$source['tipo_formulario'],$sourcePayload) : [];
                $sourceDay=$sourceUnits ? substr(max(array_keys($sourceUnits)),0,10) : $day;
                $link->execute([(int)$source['id_detalle'],$id,$source['payload_sha256'],$sourceDay,(int)$session['id_usuario']]);
            }
            return $id;
        });
        Response::success(['id_evaluacion'=>$id,'estado'=>'Entregado','valor_calculado'=>$value,
            'unidad_medida'=>$row['unidad_medida'],'meta_aprobada'=>$row['meta_esperada'],
            'meta_efectiva'=>$effectiveMeta,'porcentaje_estimado'=>$percentage,
            'puntos_si_aprueba'=>$projectedPoints,
            'requiere_aprobacion'=>true]);
    }

    public function policy(Request $request, array $session): void
    {
        Authorization::require($session,'ranking.cerrar');
        $month=(string)$request->input('periodo_clave','');
        $formula=(string)$request->input('formula','');
        $reason=trim((string)$request->input('justificacion',''));
        $cross=$request->input('comparacion_cargos_aprobada');
        if (!self::month($month) || $formula!=='PROMEDIO_PONDERADO' || $cross!==true || mb_strlen($reason)<20)
            self::denied('La comparacion entre cargos y su formula requieren aprobacion explicita');
        $db=Database::getConnection();
        $st=$db->prepare('INSERT INTO wp_ranking_politicas
            (periodo_clave,formula,comparacion_cargos_aprobada,justificacion,aprobado_por) VALUES (?,"PROMEDIO_PONDERADO",1,?,?)');
        try { $st->execute([$month,$reason,(int)$session['id_usuario']]); }
        catch (\PDOException $e) { if ($e->getCode()==='23000') self::denied('Politica ya aprobada para el periodo',409); throw $e; }
        Response::success(['id_politica'=>(int)$db->lastInsertId(),'periodo_clave'=>$month]);
    }

    public function close(Request $request, array $session): void
    {
        Authorization::require($session,'ranking.cerrar');
        $month=(string)$request->input('periodo_clave','');
        if (!self::month($month)) self::denied('Mes invalido');
        if ($month>=date('Y-m')) self::denied('Solo puede cerrarse un mes concluido');
        $result=Database::transaction(function(PDO $db) use($month,$session): array {
            $p=$db->prepare('SELECT * FROM wp_ranking_politicas WHERE periodo_clave=? FOR UPDATE');
            $p->execute([$month]); $policy=$p->fetch(PDO::FETCH_ASSOC);
            if (!$policy || !(int)$policy['comparacion_cargos_aprobada']) self::denied('Falta aprobar la regla de comparacion entre cargos',409);
            $existing=$db->prepare('SELECT 1 FROM wp_ranking_cierres WHERE periodo_clave=?');
            $existing->execute([$month]); if ($existing->fetchColumn()) self::denied('Mes ya cerrado',409);
            $rows=$db->prepare('SELECT e.id_evaluacion,e.id_trabajador,e.estado,e.porcentaje_cumplimiento,
                e.puntos_otorgados,e.peso_snapshot,e.fecha_calificacion,v.sources_ok,v.cargo_del_reporte
                FROM wp_kpi_evaluaciones e JOIN (
                    SELECT l.id_evaluacion,MIN(l.payload_sha256=f.payload_sha256) AS sources_ok,
                           MIN(f.id_cargo) AS cargo_del_reporte
                    FROM wp_kpi_reporte_evaluaciones l
                    JOIN wp_kpi_formularios_detalle f ON f.id_detalle=l.id_detalle
                    GROUP BY l.id_evaluacion
                ) v ON v.id_evaluacion=e.id_evaluacion
                WHERE e.fecha_limite>=? AND e.fecha_limite<? ORDER BY e.id_trabajador,e.id_evaluacion FOR UPDATE');
            $rows->execute([$month.'-01 00:00:00',date('Y-m-d 00:00:00',strtotime($month.'-01 +1 month'))]);
            $evaluations=$rows->fetchAll(PDO::FETCH_ASSOC);
            if (!$evaluations) self::denied('No existen evaluaciones oficiales vinculadas a reportes',409);
            $persons=[];
            $approvedCount=0;
            foreach ($evaluations as $e) {
                // Gerencia conserva su evaluación propia, pero no condiciona el ranking del equipo.
                if ((int)$e['cargo_del_reporte']===1) continue;
                // Un reporte observado no genera puntos y no impide cerrar el mes.
                if ($e['estado']==='Observado') continue;
                if ($e['estado']!=='Aprobado' || $e['porcentaje_cumplimiento']===null || $e['puntos_otorgados']===null
                    || $e['fecha_calificacion']===null || !(int)$e['sources_ok'])
                    self::denied('Hay evaluaciones pendientes o reportes modificados',409);
                $approvedCount++;
                $worker=(int)$e['id_trabajador'];
                $weight=(float)$e['peso_snapshot'];
                if ($weight<=0) self::denied('Peso no valido',409);
                $persons[$worker]??=['weight'=>0.0,'weighted'=>0.0,'points'=>0,'count'=>0];
                $persons[$worker]['weight']+=$weight;
                $persons[$worker]['weighted']+=$weight*(float)$e['porcentaje_cumplimiento'];
                $persons[$worker]['points']+=(int)$e['puntos_otorgados'];
                $persons[$worker]['count']++;
            }
            $rank=[];
            foreach ($persons as $worker=>$v) $rank[]=['worker'=>$worker,'percent'=>round($v['weighted']/$v['weight'],2),
                'points'=>$v['points'],'count'=>$v['count']];
            // Un mes con solo evaluaciones de Gerencia se cierra sin participantes.
            // Gerencia conserva su evaluación, pero nunca ocupa un puesto.
            usort($rank,fn($a,$b)=>$b['percent']<=>$a['percent'] ?: $b['points']<=>$a['points'] ?: $a['worker']<=>$b['worker']);
            $digest=hash('sha256',json_encode($evaluations,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES));
            $c=$db->prepare('INSERT INTO wp_ranking_cierres
                (periodo_clave,id_politica,evaluaciones_aprobadas,participantes,origen_sha256,cerrado_por)
                VALUES (?,?,?,?,?,?)');
            $c->execute([$month,(int)$policy['id_politica'],$approvedCount,count($rank),$digest,(int)$session['id_usuario']]);
            $insert=$db->prepare('INSERT INTO wp_ranking_historico
                (periodo_tipo,periodo_clave,id_trabajador,puntos_kpi_periodo,porcentaje_cumplimiento,
                 kpis_asignados,kpis_aprobados,puesto,medalla,cerrado_por) VALUES ("MENSUAL",?,?,?,?,?,?,?,?,?)');
            $previous=null; $position=0;
            foreach ($rank as $index=>$r) {
                $key=$r['percent'].':'.$r['points'];
                if ($key!==$previous) $position=$index+1;
                $previous=$key;
                $medal=match($position){1=>'ORO',2=>'PLATA',3=>'BRONCE',default=>'NINGUNA'};
                $insert->execute([$month,$r['worker'],$r['points'],$r['percent'],$r['count'],$r['count'],
                    $position,$medal,(int)$session['id_usuario']]);
            }
            return ['periodo_clave'=>$month,'participantes'=>count($rank),'evaluaciones_aprobadas'=>$approvedCount,
                'origen_sha256'=>$digest];
        });
        Response::success($result);
    }

    private static function salesWeek(PDO $db, array $selected, string $day): array
    {
        $week=date('o-\WW',strtotime($day));
        $st=$db->prepare('SELECT * FROM wp_kpi_formularios_detalle
            WHERE id_trabajador=? AND id_cargo=? AND tipo_formulario="VENTAS"
            ORDER BY id_detalle DESC LIMIT 500');
        $st->execute([(int)$selected['id_trabajador'],(int)$selected['id_cargo']]);
        $byDay=[];
        foreach ($st->fetchAll(PDO::FETCH_ASSOC) as $source) {
            $data=json_decode((string)$source['payload_json'],true);
            if (!is_array($data)) continue;
            $units=KpiFormCompletion::units('VENTAS',$data);
            if (count($units)!==1) continue;
            $date=substr((string)array_key_first($units),0,10);
            if (date('o-\WW',strtotime($date))!==$week || isset($byDay[$date])) continue;
            $byDay[$date]=['source'=>$source,'payload'=>$data];
        }
        if (!isset($byDay[$day]) || (int)$byDay[$day]['source']['id_detalle']!==(int)$selected['id_detalle'])
            self::denied('Abre el ultimo reporte guardado de ese dia',409);
        ksort($byDay);
        return [['days'=>array_column(array_values($byDay),'payload')],
            array_column(array_values($byDay),'source')];
    }

    private static function provisionMappedCargo(PDO $db, array $form, array $session): void
    {
        $type=(string)$form['tipo_formulario'];
        $period=KpiOfficialScore::period($type);
        if ($period===null) return;
        $mapping=$db->prepare('SELECT tipo_formulario FROM wp_kpi_formulario_cargos
            WHERE id_cargo=? AND aprobado_en IS NOT NULL');
        $mapping->execute([(int)$form['id_cargo']]);
        $mapped=$mapping->fetchAll(PDO::FETCH_COLUMN);
        if ($mapped!==[$type]) return;
        $staff=Database::getAttendanceConnection()->prepare('SELECT id_cargo FROM trabajadores
            WHERE id_trabajador=? AND estado="Activo"');
        $staff->execute([(int)$form['id_trabajador']]);
        if ((int)$staff->fetchColumn()!==(int)$form['id_cargo']) return;
        $code='SIGD-OFICIAL-CARGO-'.(int)$form['id_cargo'];
        $insert=$db->prepare('INSERT IGNORE INTO wp_kpi_definiciones
            (codigo,id_categoria,nombre,descripcion,tipo_metrica,unidad_medida,meta_esperada,
             peso,puntos_base,periodicidad,aplica_a,id_cargo_objetivo,responsable_revision,
             activo,creado_por,regla_aprobada_por,regla_aprobada_en)
             VALUES (?,1,?,"Meta oficial del cronograma y formulario confirmado", "PORCENTAJE",
                     "CUMPLIMIENTO",100,1,20,?,"CARGO",?,?,1,?,?,NOW())');
        $insert->execute([$code,'Cumplimiento oficial - '.$type,$period,(int)$form['id_cargo'],
            (int)$session['id_usuario'],(int)$session['id_usuario'],
            (int)$session['id_usuario']]);
    }

    private static function positive(mixed $v): ?float
    { $n=self::nonnegative($v); return $n!==null && $n>0 ? $n : null; }
    private static function nonnegative(mixed $v): ?float
    { return (is_int($v)||is_float($v)||is_string($v)) && is_numeric($v) && (float)$v>=0 && (float)$v<=99999999 ? (float)$v : null; }
    private static function day(string $v): bool
    { $d=\DateTimeImmutable::createFromFormat('!Y-m-d',$v); return $d!==false && $d->format('Y-m-d')===$v; }
    private static function month(string $v): bool
    { return preg_match('/^20\d{2}-(0[1-9]|1[0-2])$/D',$v)===1; }
}
