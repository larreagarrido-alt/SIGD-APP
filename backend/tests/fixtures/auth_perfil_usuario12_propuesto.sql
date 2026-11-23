-- PROPUESTA PARA REVISION. NO EJECUTADO EN CPANEL.
-- Solo tras autorizacion: una excepcion individual; no modifica el rol 6.
-- No sustituye una excepcion existente, aunque sea DENEGAR.
USE `synthetic_websiteplus_test`;

START TRANSACTION;
INSERT INTO wp_usuario_permisos
    (id_usuario, id_permiso, efecto, alcance, motivo)
SELECT u.id_usuario, p.id_permiso, 'PERMITIR', 'PROPIO',
       'REVISION_WSP_20261021_U12_AUTH_PERFIL_PROPIO'
FROM usuarios u
JOIN trabajadores t ON t.id_trabajador = u.id_trabajador
JOIN wp_permisos p ON p.id_permiso = 1 AND p.codigo = 'auth.perfil'
WHERE u.id_usuario = 12
  AND u.username = 'fixture-grant-u12'
  AND u.id_rol = 6
  AND t.estado = 'Activo'
  AND NOT EXISTS (
      SELECT 1 FROM wp_usuario_permisos up
      WHERE up.id_usuario = u.id_usuario AND up.id_permiso = p.id_permiso
  );
SELECT ROW_COUNT() AS permisos_insertados;
COMMIT;

SELECT up.id_usuario, p.codigo, up.efecto, up.alcance, up.motivo
FROM wp_usuario_permisos up
JOIN wp_permisos p ON p.id_permiso = up.id_permiso
WHERE up.id_usuario = 12 AND p.id_permiso = 1 AND p.codigo = 'auth.perfil';
