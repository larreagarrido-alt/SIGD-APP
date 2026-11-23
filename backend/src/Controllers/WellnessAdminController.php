<?php
declare(strict_types=1);
namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Organizational data only. Private answers require a separate, explicit grant. */
final class WellnessAdminController
{
    private const DIMENSIONS = ['AUTOACEPTACION','RELACIONES_POSITIVAS','AUTONOMIA',
        'DOMINIO_ENTORNO','PROPOSITO_VIDA','CRECIMIENTO_PERSONAL'];
    private const PLAN_CATEGORIES = ['GENERAL','CARGA_DE_TAREAS','COMUNICACION',
        'APOYO_DEL_EQUIPO','AMBIENTE_Y_RECURSOS','TRABAJO_Y_DESCANSO',
        'APRENDIZAJE_Y_RECONOCIMIENTO','CLARIDAD_DE_RESPONSABILIDADES'];
    private const MIN_GROUP = 5;

    private static function manage(array $session): void
    {
        Authorization::require($session, 'bienestar.administrar');
    }

    private static function aggregate(array $session): void
    {
        Authorization::require($session, 'bienestar.ver_agregado');
    }

    private static function fail(string $message, int $status = 422): never
    {
        Response::error($message, $status, $status === 403 ? 'FORBIDDEN' : 'VALIDATION_FAILED');
    }

    private static function validDateTime(string $value): bool
    {
        $date = \DateTimeImmutable::createFromFormat('!Y-m-d H:i:s', $value);
        return $date !== false && $date->format('Y-m-d H:i:s') === $value;
    }

    private static function effective(array $campaign): string
    {
        if ($campaign['estado'] === 'BORRADOR') return 'BORRADOR';
        if ($campaign['estado'] === 'DESACTIVADA') return 'DESACTIVADA';
        if ($campaign['estado'] === 'CERRADA') return 'FINALIZADA';
        $now = date('Y-m-d H:i:s');
        if ($campaign['inicio_en'] > $now) return 'PROGRAMADA';
        return $campaign['fin_en'] < $now ? 'FINALIZADA' : 'ACTIVA';
    }

    private static function instrumentReady(PDO $db, int $testId): bool
    {
        $st = $db->prepare('SELECT t.codigo,i.escala_min,i.escala_max,COUNT(q.id_pregunta) AS items,
            COUNT(DISTINCT q.dimension_ryff) AS dimensiones,
            SUM(CASE WHEN q.dimension_ryff IS NULL OR q.tipo_respuesta!="ESCALA_1_5" THEN 1 ELSE 0 END) AS invalidos
            FROM wp_bienestar_tests t JOIN wp_bienestar_ryff_instrumentos i ON i.id_test=t.id_test
            LEFT JOIN wp_bienestar_preguntas q ON q.id_test=t.id_test
            WHERE t.id_test=? AND t.activo=1 GROUP BY t.id_test,i.escala_min,i.escala_max');
        $st->execute([$testId]); $row=$st->fetch(PDO::FETCH_ASSOC);
        return $row && str_starts_with((string)$row['codigo'],'RYFF_')
            && (int)$row['escala_min']===1 && (int)$row['escala_max']===5
            && (int)$row['items']>=18 && (int)$row['dimensiones']===6 && (int)$row['invalidos']===0;
    }

    private static function eligible(array $campaign, ?int $agencyFilter=null): array
    {
        if ($agencyFilter!==null && $campaign['id_agencia']!==null
            && (int)$campaign['id_agencia']!==$agencyFilter) return [];
        $sql='SELECT DISTINCT t.id_trabajador FROM trabajadores t JOIN usuarios u ON u.id_trabajador=t.id_trabajador
            WHERE t.estado="Activo" AND u.id_rol IN (5,6)';
        if ($campaign['audiencia']==='COLABORADOR') $sql.=' AND u.id_rol=5';
        if ($campaign['audiencia']==='PRACTICANTE') $sql.=' AND u.id_rol=6';
        $args=[];
        $agency=$agencyFilter ?? ($campaign['id_agencia']===null?null:(int)$campaign['id_agencia']);
        if ($agency!==null) { $sql.=' AND t.id_agencia=?'; $args[]=$agency; }
        $st=Database::getAttendanceConnection()->prepare($sql); $st->execute($args);
        return array_map('intval',$st->fetchAll(PDO::FETCH_COLUMN));
    }

    private static function campaign(PDO $db, int $id): array
    {
        $st=$db->prepare('SELECT * FROM wp_bienestar_campanas WHERE id_campana=?');
        $st->execute([$id]); $row=$st->fetch(PDO::FETCH_ASSOC);
        if (!$row) self::fail('Campaña inexistente',404);
        return $row;
    }

    public function overview(Request $request, array $session): void
    {
        self::aggregate($session);
        $db=Database::getConnection();
        $month=(string)$request->getQuery('mes',date('Y-m'));
        if (!preg_match('/^\d{4}-(0[1-9]|1[0-2])$/D',$month)) self::fail('Mes inválido');
        $agencyRaw=$request->getQuery('id_agencia',0);
        $agency=filter_var($agencyRaw,FILTER_VALIDATE_INT);
        if($agency===false || $agency<0)self::fail('Agencia inválida');
        $agency=$agency===0?null:$agency;
        $agencies=Database::getAttendanceConnection()->query('SELECT id_agencia,nombre FROM agencias ORDER BY nombre')
            ->fetchAll(PDO::FETCH_ASSOC);
        if($agency!==null && !in_array($agency,array_map(fn($a)=>(int)$a['id_agencia'],$agencies),true))
            self::fail('Agencia inexistente',404);
        $st=$db->prepare('SELECT * FROM wp_bienestar_campanas WHERE DATE_FORMAT(inicio_en,"%Y-%m")=? ORDER BY inicio_en DESC');
        $st->execute([$month]); $campaigns=$st->fetchAll(PDO::FETCH_ASSOC);
        $selected=(int)$request->getQuery('id_campana',0);
        if ($selected>0) {
            $campaigns=array_values(array_filter($campaigns,fn($c)=>(int)$c['id_campana']===$selected));
            if (!$campaigns) self::fail('Campaña fuera del período',404);
        }
        $eligible=[]; $completed=[]; $active=0; $campaignIds=[];
        foreach ($campaigns as $campaign) {
            if ($campaign['estado']==='BORRADOR') continue;
            $campaignIds[]=(int)$campaign['id_campana'];
            if (self::effective($campaign)==='ACTIVA') $active++;
            $campaignEligible=array_fill_keys(self::eligible($campaign,$agency),true);
            foreach (array_keys($campaignEligible) as $worker) $eligible[$worker]=true;
            $st=$db->prepare('SELECT p.id_trabajador FROM wp_bienestar_participaciones p
                JOIN wp_bienestar_evaluaciones e ON e.id_evaluacion_bienestar=p.id_evaluacion_bienestar
                WHERE p.id_campana=? AND p.retirado_en IS NULL AND e.estado="COMPLETADA"');
            $st->execute([(int)$campaign['id_campana']]);
            foreach ($st->fetchAll(PDO::FETCH_COLUMN) as $worker)
                if(isset($campaignEligible[(int)$worker]))$completed[(int)$worker]=true;
        }
        $denominator=count($eligible); $numerator=count($completed);
        $dimensions=$denominator>=self::MIN_GROUP
            ? self::dimensionAverages($db,$campaignIds,array_keys($eligible)) : [];
        $trend=[];
        for($i=5;$i>=0;$i--) {
            $trendMonth=(new \DateTimeImmutable($month.'-01'))->modify('-'.$i.' months')->format('Y-m');
            $trend[]=$trendMonth===$month
                ? ['mes'=>$month,'participacion_porcentaje'=>$denominator>=self::MIN_GROUP && $denominator
                    ? round(100*$numerator/$denominator,1):null]
                : self::participationForMonth($db,$trendMonth,$agency);
        }
        $comparisons=[];
        if($agency===null)foreach($agencies as $a){
            $item=self::participationForMonth($db,$month,(int)$a['id_agencia']);
            if($item['participacion_porcentaje']!==null)$comparisons[]=[
                'id_agencia'=>(int)$a['id_agencia'],'agencia'=>$a['nombre'],
                'participacion_porcentaje'=>$item['participacion_porcentaje']];
        }
        Response::success(['mes'=>$month,'participantes_elegibles'=>$denominator>=self::MIN_GROUP?$denominator:null,
            'cuestionarios_completados'=>$denominator>=self::MIN_GROUP?$numerator:null,
            'participacion_porcentaje'=>$denominator>=self::MIN_GROUP && $denominator
                ? round(100*$numerator/$denominator,1) : null,
            'campanas_activas'=>$active,'dimensiones'=>$dimensions,
            'tendencia_participacion'=>$trend,
            'agencias'=>$agencies,'id_agencia'=>$agency,'comparacion_agencias'=>$comparisons,
            'minimo_anonimato'=>self::MIN_GROUP,
            'resultados_disponibles'=>count($dimensions)===6,
            'nota'=>'Participación no es nivel de bienestar. Sin resultados individuales.']);
    }

    private static function participationForMonth(PDO $db, string $month, ?int $agency=null): array
    {
        $st=$db->prepare('SELECT * FROM wp_bienestar_campanas
            WHERE DATE_FORMAT(inicio_en,"%Y-%m")=? AND estado!="BORRADOR"');
        $st->execute([$month]); $eligible=[]; $completed=[];
        foreach($st->fetchAll(PDO::FETCH_ASSOC) as $campaign) {
            $campaignEligible=array_fill_keys(self::eligible($campaign,$agency),true);
            foreach(array_keys($campaignEligible) as $id) $eligible[$id]=true;
            $done=$db->prepare('SELECT DISTINCT p.id_trabajador FROM wp_bienestar_participaciones p
                JOIN wp_bienestar_evaluaciones e ON e.id_evaluacion_bienestar=p.id_evaluacion_bienestar
                WHERE p.id_campana=? AND p.retirado_en IS NULL AND e.estado="COMPLETADA"');
            $done->execute([(int)$campaign['id_campana']]);
            foreach($done->fetchAll(PDO::FETCH_COLUMN) as $id)
                if(isset($campaignEligible[(int)$id]))$completed[(int)$id]=true;
        }
        $n=count($eligible);
        return ['mes'=>$month,'participacion_porcentaje'=>$n>=self::MIN_GROUP
            ? round(100*count($completed)/$n,1):null];
    }

    private static function dimensionAverages(PDO $db, array $campaignIds, array $workerIds): array
    {
        if (!$campaignIds || count($workerIds)<self::MIN_GROUP) return [];
        $placeholders=implode(',',array_fill(0,count($campaignIds),'?'));
        $workers=implode(',',array_fill(0,count($workerIds),'?'));
        $st=$db->prepare('SELECT q.dimension_ryff AS dimension,COUNT(DISTINCT p.id_trabajador) AS participantes,
            ROUND(AVG(CASE WHEN q.es_inversa=1 THEN 6-r.valor_numerico ELSE r.valor_numerico END),2) AS promedio
            FROM wp_bienestar_participaciones p
            JOIN wp_bienestar_evaluaciones e ON e.id_evaluacion_bienestar=p.id_evaluacion_bienestar AND e.estado="COMPLETADA"
            JOIN wp_bienestar_respuestas r ON r.id_evaluacion_bienestar=e.id_evaluacion_bienestar
            JOIN wp_bienestar_preguntas q ON q.id_pregunta=r.id_pregunta
            WHERE p.id_campana IN ('.$placeholders.') AND p.id_trabajador IN ('.$workers.')
                AND p.retirado_en IS NULL AND q.dimension_ryff IS NOT NULL
                AND r.valor_numerico BETWEEN 1 AND 5
            GROUP BY q.dimension_ryff HAVING participantes>=? ORDER BY q.dimension_ryff');
        $st->execute([...$campaignIds,...$workerIds,self::MIN_GROUP]);
        $rows=$st->fetchAll(PDO::FETCH_ASSOC);
        return count($rows)===6 ? $rows : [];
    }

    public function campaigns(Request $request, array $session): void
    {
        self::manage($session);
        $rows=Database::getConnection()->query('SELECT c.id_campana,c.id_test,c.titulo,c.descripcion,c.audiencia,
            c.id_agencia,c.inicio_en,c.fin_en,c.estado,t.titulo AS cuestionario,t.version
            FROM wp_bienestar_campanas c LEFT JOIN wp_bienestar_tests t ON t.id_test=c.id_test
            ORDER BY c.creado_en DESC,c.id_campana DESC LIMIT 100')->fetchAll(PDO::FETCH_ASSOC);
        foreach ($rows as &$row) $row['estado_efectivo']=self::effective($row);
        Response::success(['campanas'=>$rows]);
    }

    public function createCampaign(Request $request, array $session): void
    {
        self::manage($session);
        $title=trim((string)$request->input('titulo',''));
        $description=trim((string)$request->input('descripcion',''));
        $audience=(string)$request->input('audiencia','AMBOS');
        $start=(string)$request->input('inicio_en',''); $end=(string)$request->input('fin_en','');
        $agency=$request->input('id_agencia'); $agency=$agency===null || $agency==='' ? null : filter_var($agency,FILTER_VALIDATE_INT);
        if (mb_strlen($title)<3 || mb_strlen($title)>150 || $description==='' || mb_strlen($description)>3000
            || !in_array($audience,['COLABORADOR','PRACTICANTE','AMBOS'],true)
            || !self::validDateTime($start) || !self::validDateTime($end) || $start >= $end
            || $agency===false || ($agency!==null && $agency<=0)) self::fail('Datos de campaña inválidos');
        if ($agency!==null) {
            $st=Database::getAttendanceConnection()->prepare('SELECT 1 FROM agencias WHERE id_agencia=?');
            $st->execute([$agency]); if (!$st->fetchColumn()) self::fail('Agencia inexistente');
        }
        $db=Database::getConnection();
        $st=$db->prepare('INSERT INTO wp_bienestar_campanas
            (titulo,descripcion,audiencia,id_agencia,inicio_en,fin_en,creado_por) VALUES (?,?,?,?,?,?,?)');
        $st->execute([$title,$description,$audience,$agency,$start,$end,(int)$session['id_usuario']]);
        $id=(int)$db->lastInsertId();
        AuditService::log('bienestar','campana_borrador','EXITO',(int)$session['id_usuario'],null,null,'wp_bienestar_campanas',$id);
        Response::success(['id_campana'=>$id,'estado'=>'BORRADOR']);
    }

    public function updateCampaign(Request $request, array $session, int $id): void
    {
        self::manage($session);
        $db=Database::getConnection(); $campaign=self::campaign($db,$id);
        if ($campaign['estado']==='CERRADA') self::fail('Campaña cerrada',409);
        $state=(string)$request->input('estado',$campaign['estado']);
        if (!in_array($state,['BORRADOR','PUBLICADA','DESACTIVADA','CERRADA'],true)) self::fail('Estado inválido');
        $test=$request->input('id_test',$campaign['id_test']);
        $test=$test===null || $test==='' ? null : filter_var($test,FILTER_VALIDATE_INT);
        if ($test===false || ($test!==null && $test<=0)) self::fail('Test inválido');
        if ($state==='PUBLICADA' && (!$test || !self::instrumentReady($db,(int)$test)))
            self::fail('Ryff validado y autorizado todavía no disponible',409);
        $start=(string)$request->input('inicio_en',$campaign['inicio_en']);
        $end=(string)$request->input('fin_en',$campaign['fin_en']);
        if (!self::validDateTime($start) || !self::validDateTime($end) || $start >= $end)
            self::fail('Vigencia inválida');
        $st=$db->prepare('UPDATE wp_bienestar_campanas SET id_test=?,inicio_en=?,fin_en=?,estado=? WHERE id_campana=?');
        $st->execute([$test,$start,$end,$state,$id]);
        AuditService::log('bienestar','campana_estado','EXITO',(int)$session['id_usuario'],null,null,'wp_bienestar_campanas',$id,['estado'=>$state]);
        Response::success(['id_campana'=>$id,'estado'=>$state]);
    }

    public function questionnaire(Request $request, array $session): void
    {
        self::manage($session);
        $db=Database::getConnection();
        $st=$db->query('SELECT t.id_test,t.codigo,t.titulo,t.descripcion,t.version,t.periodicidad,
            i.version_instrumento,i.escala_min,i.escala_max,i.procedencia,i.validado_por,i.validado_en
            FROM wp_bienestar_tests t JOIN wp_bienestar_ryff_instrumentos i ON i.id_test=t.id_test
            ORDER BY i.validado_en DESC LIMIT 1');
        $instrument=$st->fetch(PDO::FETCH_ASSOC);
        if (!$instrument) Response::success(['instrumento'=>null,'preguntas'=>[],
            'estado'=>'PENDIENTE_VALIDACION','mensaje'=>'No existe una versión Ryff validada en el servidor.']);
        $questions=$db->prepare('SELECT q.id_pregunta,q.orden,q.enunciado,q.tipo_respuesta,
            q.dimension_ryff,q.es_inversa FROM wp_bienestar_preguntas q WHERE q.id_test=? ORDER BY q.orden');
        $questions->execute([(int)$instrument['id_test']]);
        $campaigns=$db->prepare('SELECT titulo,inicio_en,fin_en,estado FROM wp_bienestar_campanas
            WHERE id_test=? ORDER BY inicio_en DESC LIMIT 20');
        $campaigns->execute([(int)$instrument['id_test']]);
        Response::success(['instrumento'=>$instrument,'preguntas'=>$questions->fetchAll(PDO::FETCH_ASSOC),
            'campanas'=>$campaigns->fetchAll(PDO::FETCH_ASSOC),
            'estado'=>self::instrumentReady($db,(int)$instrument['id_test'])?'VALIDADO':'INCOMPLETO']);
    }

    public function plans(Request $request, array $session): void
    {
        self::manage($session);
        $rows=Database::getConnection()->query('SELECT id_plan,nombre,objetivo,dimension,responsable,
            fecha_programada,TIME_FORMAT(hora_inicio,"%H:%i") AS hora_inicio,
            TIME_FORMAT(hora_fin,"%H:%i") AS hora_fin,estado,estado_publicacion,visibilidad,
            alcance_resumen,agencias_json,campanas_json,trabajadores_json,observaciones FROM wp_bienestar_planes
            ORDER BY fecha_programada DESC,id_plan DESC LIMIT 200')->fetchAll(PDO::FETCH_ASSOC);
        foreach ($rows as &$row) self::publicPlanFields($row);
        unset($row);
        Response::success(['planes'=>$rows]);
    }

    private static function ids(mixed $value): array
    {
        if ($value===null) return [];
        if (!is_array($value) || count($value)>200) self::fail('Destinatarios inválidos');
        $ids=[];
        foreach ($value as $raw) {
            if (!is_int($raw) && !is_string($raw)) self::fail('Destinatarios inválidos');
            $id=filter_var($raw,FILTER_VALIDATE_INT);
            if ($id===false || $id<=0) self::fail('Destinatarios inválidos');
            $ids[]=$id;
        }
        return array_values(array_unique($ids));
    }

    private static function publicPlanFields(array &$row): void
    {
        $row['publicada']=$row['estado_publicacion']==='PUBLICADA';
        foreach (['agencias','campanas','trabajadores'] as $field) {
            $row[$field.'_ids']=json_decode((string)($row[$field.'_json'] ?? '[]'),true) ?: [];
            unset($row[$field.'_json']);
        }
    }

    public function savePlan(Request $request, array $session, ?int $id=null): void
    {
        self::manage($session);
        $name=trim((string)$request->input('nombre',''));
        $goal=trim((string)$request->input('objetivo',''));
        $dimension=(string)$request->input('dimension','GENERAL');
        $owner=trim((string)$request->input('responsable',''));
        $day=(string)$request->input('fecha_programada','');
        $state=(string)$request->input('estado','PENDIENTE');
        $notes=trim((string)$request->input('observaciones',''));
        $body=$request->getBody();
        $publication=(string)$request->input('estado_publicacion','BORRADOR');
        $publish=$publication==='PUBLICADA';
        $visibility=(string)$request->input('visibilidad','SOLO_ADMIN');
        $summary=trim((string)$request->input('alcance_resumen',''));
        $start=trim((string)$request->input('hora_inicio',''));
        $end=trim((string)$request->input('hora_fin',''));
        $agencies=self::ids($body['agencias_ids'] ?? null);
        $campaigns=self::ids($body['campanas_ids'] ?? null);
        $workers=self::ids($body['trabajadores_ids'] ?? null);
        if (!$publish) $visibility='SOLO_ADMIN';
        if (!in_array($publication,['BORRADOR','PUBLICADA'],true) || mb_strlen($summary)>255
            || !in_array($visibility,['SOLO_ADMIN','TODO_EQUIPO','AGENCIA_CAMPANA','COLABORADORES'],true)
            || ($publish && $visibility==='SOLO_ADMIN')
            || ($visibility==='AGENCIA_CAMPANA' && !$agencies && !$campaigns)
            || ($visibility==='COLABORADORES' && !$workers)
            || ($start!=='' && !preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/D',$start))
            || ($end!=='' && !preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/D',$end))
            || (($start==='')!==($end==='')) || ($start!=='' && $start>=$end)) self::fail('Publicación inválida');
        if ($visibility!=='AGENCIA_CAMPANA') { $agencies=[]; $campaigns=[]; }
        if ($visibility!=='COLABORADORES') $workers=[];
        if (mb_strlen($name)<3 || mb_strlen($name)>150 || $goal==='' || mb_strlen($goal)>3000
            || !in_array($dimension,[...self::DIMENSIONS,...self::PLAN_CATEGORIES],true)
            || $owner==='' || mb_strlen($owner)>150 || !preg_match('/^\d{4}-\d{2}-\d{2}$/D',$day)
            || !checkdate((int)substr($day,5,2),(int)substr($day,8,2),(int)substr($day,0,4))
            || !in_array($state,['PENDIENTE','EN_PROCESO','COMPLETADA'],true)
            || mb_strlen($notes)>5000) self::fail('Plan incompleto');
        $db=Database::getConnection();
        if ($id===null) {
            $st=$db->prepare('INSERT INTO wp_bienestar_planes
                (nombre,objetivo,dimension,responsable,fecha_programada,hora_inicio,hora_fin,estado,
                 estado_publicacion,visibilidad,alcance_resumen,agencias_json,campanas_json,trabajadores_json,
                 observaciones,creado_por) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)');
            $st->execute([$name,$goal,$dimension,$owner,$day,$start?:null,$end?:null,$state,
                $publish?'PUBLICADA':'BORRADOR',$visibility,
                $summary,
                json_encode($agencies),json_encode($campaigns),json_encode($workers),$notes,(int)$session['id_usuario']]);
            $id=(int)$db->lastInsertId();
        } else {
            $st=$db->prepare('UPDATE wp_bienestar_planes SET nombre=?,objetivo=?,dimension=?,responsable=?,
                fecha_programada=?,hora_inicio=?,hora_fin=?,estado=?,estado_publicacion=?,visibilidad=?,
                alcance_resumen=?,agencias_json=?,campanas_json=?,trabajadores_json=?,observaciones=? WHERE id_plan=?');
            $st->execute([$name,$goal,$dimension,$owner,$day,$start?:null,$end?:null,$state,
                $publish?'PUBLICADA':'BORRADOR',$visibility,
                $summary,
                json_encode($agencies),json_encode($campaigns),json_encode($workers),$notes,$id]);
            if ($st->rowCount()===0) {
                $check=$db->prepare('SELECT 1 FROM wp_bienestar_planes WHERE id_plan=?');
                $check->execute([$id]); if (!$check->fetchColumn()) self::fail('Plan inexistente',404);
            }
        }
        AuditService::log('bienestar','plan_guardado','EXITO',(int)$session['id_usuario'],null,null,'wp_bienestar_planes',$id);
        Response::success(['id_plan'=>$id,'estado'=>$state,'estado_publicacion'=>$publish?'PUBLICADA':'BORRADOR',
            'publicada'=>$publish,'visibilidad'=>$visibility]);
    }

    public function assistant(Request $request, array $session): void
    {
        self::aggregate($session);
        $question=mb_strtolower(trim((string)$request->input('pregunta','')));
        if ($question==='' || mb_strlen($question)>600) self::fail('Pregunta inválida');
        if (preg_match('/suicid|hacerme da[nñ]o|hacerle da[nñ]o|en peligro|emergencia|autolesi[oó]n/u',$question)) {
            Response::success(['tipo'=>'REGLAS_LOCALES','tema'=>'SEGURIDAD_INMEDIATA',
                'respuesta'=>'Si alguien está en peligro inmediato, contacta a los servicios de emergencia locales y busca acompañamiento presencial de una persona de confianza. Evita tratarlo como un asunto de rendimiento laboral.',
                'aviso'=>'Orientación general; no reemplaza a un profesional de salud mental. No se guardó la pregunta ni se enviaron alertas individuales.']);
        }
        $topic='GENERAL';
        if (str_contains($question,'relacion') || str_contains($question,'comunic')) $topic='RELACIONES_POSITIVAS';
        elseif (str_contains($question,'carga') || str_contains($question,'organiza') || str_contains($question,'entorno')) $topic='DOMINIO_ENTORNO';
        elseif (str_contains($question,'descans') || str_contains($question,'pausa')) $topic='GENERAL';
        $text=match($topic) {
            'RELACIONES_POSITIVAS'=>'Propón espacios voluntarios de conversación y acuerdos claros de comunicación. Revisa con el equipo qué obstáculos cotidianos pueden resolverse.',
            'DOMINIO_ENTORNO'=>'Revisa la distribución de tareas, prioridades y recursos con el equipo. Prueba cambios pequeños y consulta después si facilitaron el trabajo.',
            default=>'Considera pausas breves, claridad de horarios y espacios voluntarios de escucha. Para una situación personal, busca apoyo profesional de confianza.',
        };
        Response::success(['tipo'=>'REGLAS_LOCALES','tema'=>$topic,'respuesta'=>$text,
            'aviso'=>'Orientación general; no reemplaza a un profesional de salud mental. No se analizan respuestas individuales ni se llama a IA externa.']);
    }

    public function privateReview(Request $request, array $session, int $evaluationId): void
    {
        $db=Database::getConnection();
        $st=$db->prepare('SELECT e.id_evaluacion_bienestar,e.id_trabajador,e.periodo_clave,t.titulo,
            a.finalidad,a.observaciones_privadas,a.observado_en FROM wp_bienestar_evaluaciones e
            JOIN wp_bienestar_tests t ON t.id_test=e.id_test
            JOIN wp_bienestar_ryff_instrumentos i ON i.id_test=e.id_test
            JOIN wp_bienestar_participaciones p ON p.id_evaluacion_bienestar=e.id_evaluacion_bienestar
                AND p.id_trabajador=e.id_trabajador AND p.retirado_en IS NULL
                AND p.revision_privada_consentida_en IS NOT NULL
            JOIN wp_bienestar_accesos_privados a ON a.id_evaluacion_bienestar=e.id_evaluacion_bienestar
                AND a.id_usuario_autorizado=? AND a.revocado_en IS NULL AND a.vence_en>NOW()
                AND a.finalidad=p.revision_privada_finalidad
            WHERE e.id_evaluacion_bienestar=? AND e.estado="COMPLETADA"');
        $st->execute([(int)$session['id_usuario'],$evaluationId]); $row=$st->fetch(PDO::FETCH_ASSOC);
        if (!$row || !Authorization::allows($session,'bienestar.ver_privado',(int)$row['id_trabajador'])) {
            AuditService::log('bienestar','revision_privada','DENEGADO',(int)$session['id_usuario'],null,null,
                'wp_bienestar_evaluaciones',$evaluationId);
            self::fail('Revisión confidencial no autorizada',403);
        }
        $answers=$db->prepare('SELECT q.orden,q.enunciado,q.dimension_ryff,q.es_inversa,r.valor_numerico
            FROM wp_bienestar_respuestas r JOIN wp_bienestar_preguntas q ON q.id_pregunta=r.id_pregunta
            WHERE r.id_evaluacion_bienestar=? ORDER BY q.orden');
        $answers->execute([$evaluationId]);
        AuditService::log('bienestar','revision_privada','EXITO',(int)$session['id_usuario'],null,
            (int)$row['id_trabajador'],'wp_bienestar_evaluaciones',$evaluationId);
        header('Cache-Control: no-store');
        Response::success(['evaluacion'=>$row,'respuestas'=>$answers->fetchAll(PDO::FETCH_ASSOC)]);
    }

    public function privateObservation(Request $request, array $session, int $evaluationId): void
    {
        $note=trim((string)$request->input('observacion',''));
        if ($note==='' || mb_strlen($note)>5000) self::fail('Observación inválida');
        $db=Database::getConnection();
        $st=$db->prepare('SELECT e.id_trabajador FROM wp_bienestar_evaluaciones e
            JOIN wp_bienestar_ryff_instrumentos i ON i.id_test=e.id_test
            JOIN wp_bienestar_participaciones p ON p.id_evaluacion_bienestar=e.id_evaluacion_bienestar
                AND p.retirado_en IS NULL AND p.revision_privada_consentida_en IS NOT NULL
            JOIN wp_bienestar_accesos_privados a ON a.id_evaluacion_bienestar=e.id_evaluacion_bienestar
                AND a.id_usuario_autorizado=? AND a.revocado_en IS NULL AND a.vence_en>NOW()
                AND a.finalidad=p.revision_privada_finalidad
            WHERE e.id_evaluacion_bienestar=? AND e.estado="COMPLETADA"');
        $st->execute([(int)$session['id_usuario'],$evaluationId]); $worker=(int)$st->fetchColumn();
        if (!$worker || !Authorization::allows($session,'bienestar.ver_privado',$worker)) {
            AuditService::log('bienestar','observacion_privada','DENEGADO',(int)$session['id_usuario'],null,null,
                'wp_bienestar_evaluaciones',$evaluationId);
            self::fail('Revisión confidencial no autorizada',403);
        }
        $st=$db->prepare('UPDATE wp_bienestar_accesos_privados SET observaciones_privadas=?,observado_en=NOW()
            WHERE id_evaluacion_bienestar=? AND id_usuario_autorizado=? AND revocado_en IS NULL AND vence_en>NOW()');
        $st->execute([$note,$evaluationId,(int)$session['id_usuario']]);
        AuditService::log('bienestar','observacion_privada','EXITO',(int)$session['id_usuario'],null,$worker,
            'wp_bienestar_evaluaciones',$evaluationId);
        Response::success(['guardado'=>true]);
    }
}
