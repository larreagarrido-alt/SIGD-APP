<?php
declare(strict_types=1);
namespace WebSitePlus\Kpi;

use PDO;
use WebSitePlus\Core\HttpException;

/** Numeric scoring goals, separate from the text and artwork of each sheet. */
final class KpiOfficialGoals
{
    private const ROLES = [
        'ADMINISTRADOR'=>['ADMINISTRACION',[4,4,4,4,4,2]],
        'ASISTENTE_ADMINISTRATIVO'=>['ASISTENCIA_ADMIN',[23,11,11,9,14,4]],
        'CONTADOR'=>['CONTABILIDAD',[45,17,24,12,8,12]],
        'COMMUNITY_MANAGER'=>['COMMUNITY',[11,14,13,12,9,6]],
        'FOTOGRAFO'=>['FOTOGRAFIA',[3,3,3,3,3,2]],
        'TRAFFICKER_DIGITAL'=>['TRAFFICKER',[1,1,1,1,1,1]],
        'VIDEOGRAFO'=>['VIDEO',[10]],
        'DISENADOR_GRAFICO'=>['DISENADOR',[25]],
        'DISENADOR_GRAFICO_SENIOR'=>['DISENADOR_SENIOR',[24]],
        'ASESOR_DE_VENTAS'=>['VENTAS',[100,60,30,50,15,3]],
        'PROGRAMADOR_JUNIOR'=>['PROGRAMACION',[1,2,2,1,1,2]],
        'DISENADOR_GRAFICO_JUNIOR'=>['DISENADOR_JUNIOR',[4,2,2,1]],
        'GERENTE_GENERAL'=>['GERENCIA',[6]],
    ];

    public static function defaults(string $type): array
    {
        foreach (self::ROLES as [$mapped,$goals]) if ($mapped===$type) return $goals;
        return [];
    }

    /** Validate every numeric configuration before the template revision is published. */
    public static function extract(array $state): array
    {
        $result=[];
        foreach ($state as $key=>$value) {
            if (!is_string($key) || !str_ends_with($key,'_official_goals_json')) continue;
            if (!preg_match('/^sem_([A-Z_]+)_official_goals_json$/D',$key,$match)
                || !isset(self::ROLES[$match[1]])) {
                throw new HttpException('Cargo de meta KPI invalido',422,'VALIDATION_FAILED');
            }
            $goals=json_decode((string)$value,true);
            $expected=self::ROLES[$match[1]][1];
            if (!is_array($goals) || !array_is_list($goals) || count($goals)!==count($expected))
                throw new HttpException('Lista de metas KPI incompleta',422,'VALIDATION_FAILED');
            foreach ($goals as $goal) {
                if (!is_int($goal) || $goal<1 || $goal>100000)
                    throw new HttpException('Meta KPI fuera de rango',422,'VALIDATION_FAILED');
            }
            $result[self::ROLES[$match[1]][0]]=$goals;
        }
        return $result;
    }

    public static function forReport(PDO $db, string $type, ?int $revision): array
    {
        $defaults=self::defaults($type);
        if ($revision===null || $revision===0 || !$defaults) return $defaults;
        $st=$db->prepare('SELECT metas_json FROM wp_kpi_metas_versiones WHERE revision=?');
        $st->execute([$revision]);
        $json=$st->fetchColumn();
        if (!is_string($json)) throw new HttpException('Revision de metas KPI no disponible',409,'GOALS_REVISION_MISSING');
        $all=json_decode($json,true);
        $goals=is_array($all) ? ($all[$type]??null) : null;
        return is_array($goals) && array_is_list($goals) && count($goals)===count($defaults)
            ? $goals : $defaults;
    }
}
