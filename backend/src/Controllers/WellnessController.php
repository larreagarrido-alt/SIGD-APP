<?php
declare(strict_types=1);

namespace WebSitePlus\Controllers;

use PDO;
use WebSitePlus\Auth\AuditService;
use WebSitePlus\Auth\Authorization;
use WebSitePlus\Core\Database;
use WebSitePlus\Core\HttpException;
use WebSitePlus\Core\Request;
use WebSitePlus\Core\Response;

/** Own-worker questionnaires only. Scores and clinical interpretations are not inferred here. */
final class WellnessController
{
    /** Activities published for this active worker; no private wellbeing answers are returned. */
    public function activities(Request $request, array $session): void
    {
        $worker=self::worker($session);
        $role=(int)($session['id_rol'] ?? 0);
        $employee=Database::getAttendanceConnection()->prepare(
            'SELECT id_agencia FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
        $employee->execute([$worker]);
        $agency=$employee->fetchColumn();
        if ($agency===false || !in_array($role,[5,6],true)) Response::success(['actividades'=>[]]);
        $db=Database::getConnection();
        $rows=$db->query('SELECT id_plan,nombre,objetivo,dimension,responsable,fecha_programada,
            TIME_FORMAT(hora_inicio,"%H:%i") AS hora_inicio,
            TIME_FORMAT(hora_fin,"%H:%i") AS hora_fin,
            estado,estado_publicacion,visibilidad,alcance_resumen,
            agencias_json,campanas_json,trabajadores_json,observaciones
            FROM wp_bienestar_planes WHERE estado_publicacion="PUBLICADA"
            AND visibilidad!="SOLO_ADMIN" ORDER BY fecha_programada DESC,id_plan DESC LIMIT 200')
            ->fetchAll(PDO::FETCH_ASSOC);
        $campaignQuery=$db->prepare('SELECT id_campana FROM wp_bienestar_campanas
            WHERE estado="PUBLICADA" AND inicio_en<=NOW() AND fin_en>=NOW()
            AND audiencia IN ("AMBOS",?) AND (id_agencia IS NULL OR id_agencia=?)');
        $campaignQuery->execute([$role===5?'COLABORADOR':'PRACTICANTE',$agency]);
        $campaignIds=array_map('intval',$campaignQuery->fetchAll(PDO::FETCH_COLUMN));
        $visible=[];
        foreach ($rows as $row) {
            $scope=$row['visibilidad'];
            $allowed=$scope==='TODO_EQUIPO';
            if ($scope==='COLABORADORES') {
                $allowed=in_array($worker,json_decode((string)$row['trabajadores_json'],true) ?: [],true);
            } elseif ($scope==='AGENCIA_CAMPANA') {
                $agencies=json_decode((string)$row['agencias_json'],true) ?: [];
                $campaigns=json_decode((string)$row['campanas_json'],true) ?: [];
                $allowed=in_array((int)$agency,$agencies,true)
                    || (bool)array_intersect($campaignIds,$campaigns);
            }
            if (!$allowed) continue;
            $row['publicada']=true;
            unset($row['agencias_json'],$row['campanas_json'],$row['trabajadores_json']);
            $visible[]=$row;
        }
        Response::success(['actividades'=>$visible]);
    }

    private static function worker(array $session): int
    {
        $worker = (int) ($session['id_trabajador'] ?? 0);
        if ($worker <= 0) Response::error('Trabajador no vinculado', 403, 'FORBIDDEN');
        Authorization::require($session, 'bienestar.responder', $worker);
        return $worker;
    }

    public function tests(Request $request, array $session): void
    {
        self::worker($session);
        $db = Database::getConnection();
        $tests = $db->query('SELECT id_test, codigo, titulo, descripcion, version, tipo, periodicidad FROM wp_bienestar_tests t WHERE activo=1 AND NOT EXISTS (SELECT 1 FROM wp_bienestar_ryff_instrumentos i WHERE i.id_test=t.id_test) ORDER BY id_test')->fetchAll(PDO::FETCH_ASSOC);
        $questions = $db->prepare('SELECT id_pregunta, orden, enunciado, tipo_respuesta FROM wp_bienestar_preguntas WHERE id_test=? ORDER BY orden,id_pregunta');
        $options = $db->prepare('SELECT id_opcion, orden, texto FROM wp_bienestar_opciones WHERE id_pregunta=? ORDER BY orden,id_opcion');
        foreach ($tests as &$test) {
            $questions->execute([(int) $test['id_test']]);
            $test['preguntas'] = $questions->fetchAll(PDO::FETCH_ASSOC);
            foreach ($test['preguntas'] as &$question) {
                $options->execute([(int) $question['id_pregunta']]);
                $question['opciones'] = $options->fetchAll(PDO::FETCH_ASSOC);
            }
            unset($question);
        }
        unset($test);
        Response::success(['tests' => $tests]);
    }

    /** Published Ryff campaigns visible to the signed-in worker only. */
    public function campaigns(Request $request, array $session): void
    {
        $worker=self::worker($session);
        $role=(int)($session['id_rol'] ?? 0);
        if (!in_array($role,[5,6],true)) Response::success(['campanas'=>[]]);
        $st=Database::getAttendanceConnection()->prepare('SELECT id_agencia FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
        $st->execute([$worker]); $agency=$st->fetchColumn();
        if ($agency===false) Response::success(['campanas'=>[]]);
        $db=Database::getConnection();
        $st=$db->prepare('SELECT c.id_campana,c.id_test,c.titulo,c.descripcion,c.inicio_en,c.fin_en,
            t.titulo AS cuestionario,i.version_instrumento FROM wp_bienestar_campanas c
            JOIN wp_bienestar_tests t ON t.id_test=c.id_test AND t.activo=1
            JOIN wp_bienestar_ryff_instrumentos i ON i.id_test=c.id_test
            WHERE c.estado="PUBLICADA" AND c.inicio_en<=NOW() AND c.fin_en>=NOW()
              AND c.audiencia IN ("AMBOS",?) AND (c.id_agencia IS NULL OR c.id_agencia=?)
              AND NOT EXISTS (SELECT 1 FROM wp_bienestar_participaciones p WHERE p.id_campana=c.id_campana AND p.id_trabajador=?)
            ORDER BY c.fin_en LIMIT 20');
        $st->execute([$role===5?'COLABORADOR':'PRACTICANTE',$agency,$worker]);
        Response::success(['campanas'=>$st->fetchAll(PDO::FETCH_ASSOC),
            'aviso'=>'Participación voluntaria. Las respuestas son privadas y solo se usan para resultados grupales anónimos.']);
    }

    public function campaignQuestions(Request $request, array $session, int $campaignId): void
    {
        $worker=self::worker($session);
        $role=(int)($session['id_rol'] ?? 0);
        $employee=Database::getAttendanceConnection()->prepare('SELECT id_agencia FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
        $employee->execute([$worker]); $agency=$employee->fetchColumn();
        $db=Database::getConnection();
        $st=$db->prepare('SELECT c.id_campana,c.id_test,c.titulo,c.descripcion,c.audiencia,c.id_agencia,
            i.version_instrumento FROM wp_bienestar_campanas c
            JOIN wp_bienestar_tests t ON t.id_test=c.id_test AND t.activo=1
            JOIN wp_bienestar_ryff_instrumentos i ON i.id_test=t.id_test
            WHERE c.id_campana=? AND c.estado="PUBLICADA" AND c.inicio_en<=NOW() AND c.fin_en>=NOW()');
        $st->execute([$campaignId]); $campaign=$st->fetch(PDO::FETCH_ASSOC);
        if (!$campaign || $agency===false || !in_array($role,[5,6],true)
            || !in_array($campaign['audiencia'],['AMBOS',$role===5?'COLABORADOR':'PRACTICANTE'],true)
            || ($campaign['id_agencia']!==null && (int)$campaign['id_agencia']!==(int)$agency))
            Response::error('Campaña no disponible',403,'FORBIDDEN');
        $done=$db->prepare('SELECT 1 FROM wp_bienestar_participaciones WHERE id_campana=? AND id_trabajador=?');
        $done->execute([$campaignId,$worker]);
        if ($done->fetchColumn()) Response::error('Campaña ya respondida',409,'ALREADY_COMPLETED');
        $st=$db->prepare('SELECT id_pregunta,orden,enunciado,tipo_respuesta FROM wp_bienestar_preguntas
            WHERE id_test=? ORDER BY orden,id_pregunta');
        $st->execute([(int)$campaign['id_test']]);
        header('Cache-Control: no-store');
        Response::success(['campana'=>['id_campana'=>$campaignId,'id_test'=>(int)$campaign['id_test'],
            'titulo'=>$campaign['titulo'],'descripcion'=>$campaign['descripcion'],
            'version_instrumento'=>$campaign['version_instrumento']],
            'preguntas'=>$st->fetchAll(PDO::FETCH_ASSOC),
            'aviso'=>'Participación voluntaria. Tus respuestas se guardan en el servidor y se muestran al equipo solo de forma grupal anónima cuando hay suficientes participantes. No afectan KPI, premios ni decisiones laborales.']);
    }

    public function own(Request $request, array $session): void
    {
        $worker = self::worker($session);
        $db = Database::getConnection();
        $query = $db->prepare('SELECT e.id_evaluacion_bienestar, e.id_test, t.titulo, e.periodo_clave, e.estado, e.fecha_completada FROM wp_bienestar_evaluaciones e JOIN wp_bienestar_tests t ON t.id_test=e.id_test WHERE e.id_trabajador=? ORDER BY e.creado_en DESC,e.id_evaluacion_bienestar DESC LIMIT 50');
        $query->execute([$worker]);
        Response::success(['id_trabajador' => $worker, 'evaluaciones' => $query->fetchAll(PDO::FETCH_ASSOC)]);
    }

    /** Private answers for the signed-in worker only; never expose another worker's health data. */
    public function ownDetail(Request $request, array $session, int $evaluationId): void
    {
        $worker = self::worker($session);
        if ($evaluationId <= 0) Response::error('Evaluacion no encontrada', 404, 'NOT_FOUND');
        $db = Database::getConnection();
        $query = $db->prepare('SELECT e.id_evaluacion_bienestar,e.id_test,t.codigo,t.titulo,e.periodo_clave,e.estado,e.fecha_completada FROM wp_bienestar_evaluaciones e JOIN wp_bienestar_tests t ON t.id_test=e.id_test WHERE e.id_evaluacion_bienestar=? AND e.id_trabajador=?');
        $query->execute([$evaluationId, $worker]);
        $evaluation = $query->fetch(PDO::FETCH_ASSOC);
        if (!$evaluation) Response::error('Evaluacion no encontrada', 404, 'NOT_FOUND');
        $answers = $db->prepare('SELECT q.id_pregunta,q.orden,q.enunciado,r.valor_numerico,r.texto_respuesta,o.orden AS opcion_orden,o.texto AS opcion_texto FROM wp_bienestar_respuestas r JOIN wp_bienestar_preguntas q ON q.id_pregunta=r.id_pregunta LEFT JOIN wp_bienestar_opciones o ON o.id_opcion=r.id_opcion_seleccionada WHERE r.id_evaluacion_bienestar=? ORDER BY q.orden,q.id_pregunta');
        $answers->execute([$evaluationId]);
        header('Cache-Control: no-store');
        Response::success(['id_trabajador' => $worker, 'evaluacion' => $evaluation,
            'respuestas' => $answers->fetchAll(PDO::FETCH_ASSOC)]);
    }

    public function submit(Request $request, array $session): void
    {
        $worker = self::worker($session);
        $body = $request->getBody();
        $testId = $body['id_test'] ?? null;
        $answers = $body['respuestas'] ?? null;
        $campaignId=$body['id_campana'] ?? null;
        $consent=$body['consentimiento_informado'] ?? false;
        if (!is_int($testId) || $testId <= 0 || !is_array($answers) || !array_is_list($answers)
            || count($answers) < 1 || count($answers) > 100) {
            Response::error('Cuestionario o respuestas invalidos', 422, 'VALIDATION_FAILED');
        }
        try {
        $result = Database::transaction(function (PDO $db) use ($worker, $testId, $answers, $campaignId, $consent, $session): array {
            $testQuery = $db->prepare('SELECT periodicidad FROM wp_bienestar_tests WHERE id_test=? AND activo=1');
            $testQuery->execute([$testId]);
            $periodicity = $testQuery->fetchColumn();
            if (!$periodicity) throw new HttpException('Cuestionario no disponible', 404, 'NOT_FOUND');
            $instrument=$db->prepare('SELECT version_instrumento FROM wp_bienestar_ryff_instrumentos WHERE id_test=?');
            $instrument->execute([$testId]); $instrumentVersion=$instrument->fetchColumn();
            $isRyff=$instrumentVersion!==false;
            if ($isRyff) {
                if (!is_int($campaignId) || $campaignId<=0 || $consent!==true)
                    throw new HttpException('Campaña y consentimiento requeridos',422,'CONSENT_REQUIRED');
                $campaign=$db->prepare('SELECT id_agencia,audiencia FROM wp_bienestar_campanas
                    WHERE id_campana=? AND id_test=? AND estado="PUBLICADA" AND inicio_en<=NOW() AND fin_en>=NOW() FOR UPDATE');
                $campaign->execute([$campaignId,$testId]); $campaignRow=$campaign->fetch(PDO::FETCH_ASSOC);
                if (!$campaignRow) throw new HttpException('Campaña fuera de vigencia',403,'FORBIDDEN');
                $role=(int)($session['id_rol'] ?? 0);
                if (!in_array($role,[5,6],true) || !in_array($campaignRow['audiencia'],['AMBOS',$role===5?'COLABORADOR':'PRACTICANTE'],true))
                    throw new HttpException('Cuenta no elegible',403,'FORBIDDEN');
                $employee=Database::getAttendanceConnection()->prepare('SELECT id_agencia FROM trabajadores WHERE id_trabajador=? AND estado="Activo"');
                $employee->execute([$worker]); $agency=$employee->fetchColumn();
                if ($agency===false || ($campaignRow['id_agencia']!==null && (int)$agency!==(int)$campaignRow['id_agencia']))
                    throw new HttpException('Cuenta no elegible',403,'FORBIDDEN');
            } elseif ($campaignId!==null) {
                throw new HttpException('Campaña incompatible',422,'VALIDATION_FAILED');
            }
            $year = date('Y'); $month = (int) date('n');
            $period = match ($periodicity) {
                'SEMANAL' => sprintf('%04d-W%02d', (int) date('o'), (int) date('W')),
                'MENSUAL' => date('Y-m'),
                'TRIMESTRAL' => $year . '-Q' . (int) ceil($month / 3),
                'SEMESTRAL' => $year . '-H' . (int) ceil($month / 6),
                'LIBRE' => date('Y-m-d'),
                default => throw new HttpException('Periodicidad no admitida', 422, 'VALIDATION_FAILED'),
            };
            if ($isRyff) $period='C'.$campaignId;
            $questions = $db->prepare('SELECT id_pregunta,tipo_respuesta FROM wp_bienestar_preguntas WHERE id_test=?');
            $questions->execute([$testId]);
            $allowed = [];
            foreach ($questions as $question) $allowed[(int) $question['id_pregunta']] = $question['tipo_respuesta'];
            if (!$allowed || count($answers) !== count($allowed)) throw new HttpException('Responde todas las preguntas oficiales', 422, 'VALIDATION_FAILED');
            $normalized = [];
            $option = $db->prepare('SELECT 1 FROM wp_bienestar_opciones WHERE id_opcion=? AND id_pregunta=?');
            foreach ($answers as $answer) {
                if (!is_array($answer) || !is_int($answer['id_pregunta'] ?? null)) throw new HttpException('Respuesta invalida', 422, 'VALIDATION_FAILED');
                $id = $answer['id_pregunta'];
                if (!isset($allowed[$id]) || isset($normalized[$id])) throw new HttpException('Pregunta ajena o duplicada', 422, 'VALIDATION_FAILED');
                $type = $allowed[$id]; $number = null; $choice = null; $text = null;
                if ($type === 'ESCALA_1_5' || $type === 'SI_NO') {
                    $number = $answer['valor_numerico'] ?? null;
                    if (!is_int($number) || ($type === 'ESCALA_1_5' && ($number < 1 || $number > 5))
                        || ($type === 'SI_NO' && !in_array($number, [0, 1], true))) throw new HttpException('Escala invalida', 422, 'VALIDATION_FAILED');
                } elseif ($type === 'OPCION_MULTIPLE') {
                    $choice = $answer['id_opcion_seleccionada'] ?? null;
                    if (!is_int($choice) || $choice <= 0) throw new HttpException('Opcion invalida', 422, 'VALIDATION_FAILED');
                    $option->execute([$choice, $id]);
                    if (!$option->fetchColumn()) throw new HttpException('Opcion ajena', 422, 'VALIDATION_FAILED');
                } elseif ($type === 'TEXTO_LIBRE') {
                    $text = $answer['texto_respuesta'] ?? null;
                    if (!is_string($text) || trim($text) === '' || mb_strlen($text) > 2000)
                        throw new HttpException('Texto invalido', 422, 'VALIDATION_FAILED');
                    $text = trim($text);
                } else throw new HttpException('Tipo de pregunta no admitido', 422, 'VALIDATION_FAILED');
                $normalized[$id] = [$number, $choice, $text];
            }
            $existing = $db->prepare('SELECT id_evaluacion_bienestar,estado FROM wp_bienestar_evaluaciones WHERE id_test=? AND id_trabajador=? AND periodo_clave=? FOR UPDATE');
            $existing->execute([$testId, $worker, $period]);
            $row = $existing->fetch(PDO::FETCH_ASSOC);
            if ($row && $row['estado'] === 'COMPLETADA') throw new HttpException('Cuestionario ya completado en este periodo', 409, 'ALREADY_COMPLETED');
            if ($row) {
                $evaluation = (int) $row['id_evaluacion_bienestar'];
                $previous = $db->prepare('SELECT COUNT(*) FROM wp_bienestar_respuestas WHERE id_evaluacion_bienestar=?');
                $previous->execute([$evaluation]);
                if ((int) $previous->fetchColumn() > 0) {
                    // Preserve any earlier answers until a deliberate recovery flow is authorized.
                    throw new HttpException('Evaluacion previa pendiente con respuestas; solicita revision', 409, 'PREVIOUS_ANSWERS');
                }
            } else {
                $insert = $db->prepare("INSERT INTO wp_bienestar_evaluaciones (id_test,id_trabajador,periodo_clave,estado,fecha_completada) VALUES (?,?,?,'COMPLETADA',NOW())");
                $insert->execute([$testId, $worker, $period]); $evaluation = (int) $db->lastInsertId();
            }
            $save = $db->prepare('INSERT INTO wp_bienestar_respuestas (id_evaluacion_bienestar,id_pregunta,valor_numerico,id_opcion_seleccionada,texto_respuesta) VALUES (?,?,?,?,?)');
            foreach ($normalized as $question => [$number, $choice, $text]) $save->execute([$evaluation, $question, $number, $choice, $text]);
            if ($row) {
                $db->prepare("UPDATE wp_bienestar_evaluaciones SET estado='COMPLETADA',fecha_completada=NOW(),puntaje_total=NULL,nivel_alerta=NULL,recomendacion_sistema=NULL WHERE id_evaluacion_bienestar=?")->execute([$evaluation]);
            }
            if ($isRyff) {
                $link=$db->prepare('INSERT INTO wp_bienestar_participaciones
                    (id_campana,id_evaluacion_bienestar,id_trabajador,consentimiento_version,consentido_en)
                    VALUES (?,?,?,?,NOW())');
                $link->execute([$campaignId,$evaluation,$worker,'RYFF-'.$instrumentVersion]);
            }
            return ['id_evaluacion_bienestar' => $evaluation, 'periodo_clave' => $period, 'respuestas_guardadas' => count($normalized)];
        });
        } catch (HttpException $e) {
            throw $e;
        } catch (\Throwable $e) {
            // The transaction has already rolled back. Return only a short reference,
            // never the SQL, answers, identity or exception message.
            $driverCode = $e instanceof \PDOException ? (int) ($e->errorInfo[1] ?? 0) : 0;
            $reference = 'WELLNESS_WRITE_' . $driverCode . '_L' . $e->getLine();
            error_log('Wellness submit failure: ' . $reference);
            Response::error('No se pudieron guardar las respuestas', 500, $reference);
        }
        AuditService::log('bienestar', 'respuesta_titular', 'EXITO', (int) $session['id_usuario'],
            $session['username'] ?? null, $worker, 'wp_bienestar_evaluaciones', $result['id_evaluacion_bienestar'],
            ['cantidad' => $result['respuestas_guardadas']], $request->getIp());
        Response::success($result, 'Respuestas guardadas');
    }
}
