# HTTP 500 al guardar Bienestar — diagnóstico controlado

## Evidencia observada

- El teléfono recibió HTTP 500 al enviar una evaluación semanal. La consulta agregada de `wp_bienestar_evaluaciones` para `DANI_SEMANAL` y `DANI_MENSUAL` devolvió cero filas después del intento. No hay confirmación de persistencia.
- La base publicada registra V021, dos cuestionarios, 16 preguntas y 80 opciones. Los roles 1, 5 y 6 tienen `bienestar.responder` con alcance `PROPIO`, según las capturas de phpMyAdmin.
- La copia SQL descargada, restaurada en MariaDB aislado, guardó seis respuestas sintéticas con el mismo controlador. Esto **no** reproduce todavía el fallo del servidor publicado.
- Con `ANSI_QUOTES` forzado en el laboratorio, la escritura falla y hace rollback; el controlador devuelve la referencia segura `WELLNESS_WRITE_1054_L...`. La consulta de producción informó `ANSI_QUOTES=0` en el modo global, así que esa reproducción no identifica la causa real.

## Diagnóstico publicado pendiente

`WellnessController.php` ahora convierte errores inesperados de la transacción en una referencia `WELLNESS_WRITE_<código SQL>_L<línea>`. El mensaje no incluye respuestas, identidades, SQL ni credenciales. Los errores de validación y autorización siguen conservando su código HTTP original.

El APK muestra esa referencia solamente si recibe HTTP 500 de Bienestar. En la copia aislada se verificó un guardado exitoso y el rollback ante un error de SQL. Además pasaron 692 comprobaciones PHP/HTTP/MySQL del laboratorio completo, 112 con V012 ausente, 15 validaciones previas y 171 pruebas Android; `:app:assembleDebug` compiló y el APK se instaló con `adb install -r` sin borrar datos.

## Siguiente comprobación

1. Reemplazar únicamente `websiteplus_backend/src/Controllers/WellnessController.php` con `Descargas/WellnessController_DIAGNOSTICO_20261114.php`, conservando el archivo anterior.
2. Reintentar una sola vez desde una cuenta propia y autorizada. Comunicar únicamente la referencia `WELLNESS_WRITE_...` o el resultado de guardado, nunca las respuestas.
3. Si sigue fallando, corregir la operación identificada y verificar con una consulta agregada de solo lectura que el intento no dejó una evaluación parcial.

## Causa identificada y corrección posterior

El segundo intento devolvió `WELLNESS_WRITE_1142_L117`. El código 1142 indica permiso MySQL denegado y la línea 117 correspondía a `DELETE FROM wp_bienestar_respuestas`. La consulta agregada previa mostró cero evaluaciones, por lo que el primer intento no dejó respuestas guardadas.

El controlador corregido elimina el `DELETE` innecesario. Una evaluación nueva se crea como `COMPLETADA` dentro de la misma transacción que inserta todas sus respuestas; si cualquier inserción falla, todo se revierte. Si encuentra una evaluación previa no completada con respuestas, la conserva y devuelve 409 `PREVIOUS_ANSWERS`, sin borrarlas ni sobrescribirlas. Una evaluación previa sin respuestas se puede completar. La app distingue el conflicto de respuestas previas del intento duplicado de un período ya completado.

Se comprobó en la copia SQL aislada con un usuario MySQL sintético sin privilegio `DELETE`: seis respuestas persistieron y la evaluación quedó completada. Otro caso sintético confirmó 409 y conservación de una respuesta previa. Las suites posteriores a la corrección pasaron 692 comprobaciones completas, 112 con V012 ausente y 15 validaciones; 171 pruebas Android y `:app:assembleDebug` pasaron. El APK corregido se instaló con `adb install -r` sin borrar datos.

El archivo publicado por el usuario en cPanel fue `Descargas/WellnessController_CORREGIDO_20261114.php`, con destino `websiteplus_backend/src/Controllers/WellnessController.php`. La corrección no requirió SQL nuevo. La API pública continuó respondiendo HTTP 200 en `/health`.

**Verificación física posterior:** con una cuenta propia autorizada, el usuario confirmó que la evaluación semanal se guardó y luego apareció en «Ver mi historial» del mismo teléfono. No se recibieron ni inspeccionaron las respuestas. Esto confirma el recorrido de escritura y lectura de Bienestar semanal en producción para esa cuenta; no constituye una prueba física de la evaluación mensual ni de otras cuentas.
