-- SOLO REVERSION DE LA CONCESION PROPUESTA, TRAS AUTORIZACION.
-- No borra excepciones con otro efecto, alcance o motivo.
USE `synthetic_websiteplus_test`;

START TRANSACTION;
DELETE up FROM wp_usuario_permisos up
JOIN wp_permisos p ON p.id_permiso = up.id_permiso
WHERE up.id_usuario = 12
  AND p.id_permiso = 1
  AND p.codigo = 'auth.perfil'
  AND up.efecto = 'PERMITIR'
  AND up.alcance = 'PROPIO'
  AND up.motivo = 'REVISION_WSP_20261021_U12_AUTH_PERFIL_PROPIO';
SELECT ROW_COUNT() AS permisos_retirados;
COMMIT;
