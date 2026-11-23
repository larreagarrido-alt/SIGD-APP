# Metas KPI encontradas en los ZIP del equipo — 18/11/2026

## Qué contiene realmente `KPI-Web.zip`

Fuente: `KPI-Web/app/src/main/java/pe/net/ws/kpiweb/features/kpi/admin/data/KpiCustomRepository.java`, método `seedDefaultCorporateKpis()` (líneas 380–405). Son 16 definiciones iniciales guardadas por la app en preferencias locales. La misma clase crea registros históricos `REC-SEED-*` (líneas 431–435). **No son filas de `wp_kpi_definiciones`, no son evaluaciones aprobadas ni prueban aprobación empresarial.**

| Cargo escrito en el ZIP | Código | Meta | Peso |
| --- | --- | ---: | ---: |
| Asesor de Ventas | KPI-COM-01 llamadas | 15 | 25 % |
| Asesor de Ventas | KPI-COM-02 visitas | 3 | 35 % |
| Asesor de Ventas | KPI-COM-03 propuestas | 2 | 20 % |
| Asesor de Ventas | KPI-COM-04 cierres | 1 | 20 % |
| Diseñador Gráfico | KPI-DIS-01 diseños | 6 | 50 % |
| Diseñador Gráfico | KPI-DIS-02 videos | 2 | 35 % |
| Diseñador Gráfico | KPI-DIS-03 sesiones | 1 | 15 % |
| Diseñador Gráfico Junior | KPI-JUN-01 posts | 5 | 50 % |
| Diseñador Gráfico Junior | KPI-JUN-02 adaptaciones | 3 | 30 % |
| Diseñador Gráfico Junior | KPI-JUN-03 briefing | 100 % | 20 % |
| Diseñador Gráfico Senior | KPI-SEN-01 entregables | 4 | 40 % |
| Diseñador Gráfico Senior | KPI-SEN-02 colaboradores | 2 | 30 % |
| Diseñador Gráfico Senior | KPI-SEN-03 entregas | 80 % | 30 % |
| Programador | KPI-PROG-01 tareas | 2 | 40 % |
| Programador | KPI-PROG-02 incidencias | 3 | 35 % |
| Programador | KPI-PROG-03 bugs | 0 | 25 % |

Todas estas semillas tienen frecuencia `Diaria` en `addSeed()` (línea 425). No hay semillas equivalentes para los otros formularios y cargos reales, incluidos Gerente General, Community Manager y Creativo / Videógrafo. Los nombres del ZIP tampoco identifican por sí solos si Programador Web y Programador Junior, o Diseñador Junior y Senior, deben compartir reglas.

## Contradicciones que impiden publicar puntuaciones automáticamente

- `KPI-Web.zip/docs/MANUAL_INTEGRACION_MODULO_KPI.md`, líneas 110–115: Excelente desde 95 %, Muy Bueno desde 90 %, Bueno desde 80 %, Regular desde 70 %, Deficiente por debajo de 70 %; 20/17/14/12/10 puntos.
- El motor en **ese mismo** `KPI-Web.zip`, `KpiCalculationEngine.java`, líneas 109–127, y su `KpiScoreLedger.java`, líneas 17–23: Excelente desde 90 %, Muy Bueno desde 80 %, Bueno desde 60 %, Regular desde 40 %.
- `KPI-Web(dani).zip` contiene otra versión de `KpiCalculationEngine.java`, líneas 109–127, con los umbrales 95/90/80/70 del manual. No resuelve cuál versión aprobó WEBSITE.
- El informe de Diseñador Gráfico en el ZIP habla de una meta **semanal** de 25 actividades (línea 127), mientras las 16 semillas anteriores son diarias. Las metas de Ventas del informe son semanales (100 llamadas, 60 visitas, 30 cotizaciones, 50 seguimientos, 15 prospectos, 3 cierres; líneas 128–133), distintas de las cuatro semillas diarias. No se pueden cargar ambas como una sola regla.
- `KPI-Web/database/migrations/V009__datos_iniciales.up.sql` inserta categorías y configuración general, pero no definiciones ni asignaciones de KPI. Su comentario exige verificar los roles reales antes de conceder permisos (líneas 40–55).

## Decisión operativa

Los formularios completos y sus reportes confirmados pueden seguir funcionando. El ranking oficial requiere registrar en el nuevo panel administrativo una regla aprobada y vigente para cada cargo/indicador, su período, fórmula, meta, peso y puntos base; luego asignar, evaluar, aprobar y cerrar. Las semillas del ZIP sirven como **propuesta para revisión** y no se insertan en producción. El cierre entre cargos requiere también aprobar expresamente la política de comparación.

Última evidencia de producción comunicada por captura: 10 formularios confirmados, 0 KPI definidos, 0 evaluaciones aprobadas y 0 filas de ranking. No se ha verificado un respaldo SQL nuevo del 18/11: el archivo local `Downloads/wsnet_websiteplus.sql` tiene fecha 14/11/2026 12:43. Tampoco se ha confirmado un staging de cPanel. Este documento no autoriza migraciones ni concesiones de permisos.

## Nueva tabla enviada por el usuario el 18/11

La imagen `codex-clipboard-f3b38618-435f-46ff-b099-329666a6b8cd.png` corresponde al formulario **VIDEO / Creativo-Videógrafo**: el texto de los cinco niveles coincide con `KpiVideographerContainerFragment.java`, líneas 791–811. Regla mostrada: Excelente 90–100 % y ≥10 actividades semanales; Muy Bueno 80–89 % y 9; Bueno 60–79 % y 7–8; Regular 40–59 % y 6; Deficiente <40 % y ≤5.

El código calcula `pctWeek` como promedio de cuatro porcentajes individuales con metas 3 videos grabados, 4 editados, 3 ajustes y 3 entregas (líneas 779–789). El 18/11 el usuario confirmó que **manda el porcentaje calculado** cuando contradice el número de actividades. Se corrigió la clasificación visual de VIDEO para usar solo `pctWeek`; el total de actividades permanece como dato informativo. Se quitaron los rangos de actividades de las etiquetas de nivel porque podrían contradecir el porcentaje. Las cuatro submetas suman 13 aunque el total de la tabla indica ≥10; no se cambian sin aprobación específica. Esta decisión no define puntos ni reglas para los demás cargos y no inserta configuraciones oficiales en producción.

El usuario precisó después que **solo los niveles porcentuales** son comunes a todos los cargos: Excelente ≥90 %, Muy Bueno ≥80 %, Bueno ≥60 %, Regular ≥40 %, Deficiente <40 %. No aprobó una meta universal de 10 actividades. Se mantuvieron las metas de producción de cada pantalla. Las etiquetas semanales de ADMINISTRACION, ASISTENCIA_ADMIN, CONTABILIDAD, FOTOGRAFIA y VIDEO ahora se clasifican por el porcentaje que ya calculan, sin atajos por recuento. COMMUNITY calcula su nivel de productividad con porcentaje sobre su meta semanal existente de 65 actividades; ya no muestra «Excelente» como texto inicial antes de cargar datos. El motor KPI general ya tenía estos cinco umbrales. Esta regla compartida de **nivel** no concede automáticamente puntos ni aprueba metas, pesos o evaluaciones oficiales.
