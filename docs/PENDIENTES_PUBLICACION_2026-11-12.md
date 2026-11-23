# Pendientes para autorizar la publicación de KPIWeb

Estado al 12-11-2026, rama `jair-integracion`. La autenticación y las cuentas del sistema de asistencia se conservan. El titular aportó resultados de consultas de solo lectura hechas en phpMyAdmin; esta revisión no consultó ni modificó cPanel.

| Comprobación | Evidencia disponible | Pendiente para autorizar |
| --- | --- | --- |
| Paquete PHP completo | V6 incluye ocho controladores, router y los tres servicios Auth requeridos | Comparar checksum de cada archivo con V6 antes de copiar; publicar como unidad |
| Esquema V015 | Producción: `wp_kpi_registros_android` no existe y V015 no figura; `id_kpi` de definiciones es `INT UNSIGNED` en la exportación | Confirmar motor, PK/tipo y estado inmediatamente antes de instalación; decidir si se publicará infraestructura KPI vacía |
| Esquema V016 | Producción: `uk_wpandroid_usuario` y `uk_wpandroid_celular` presentes, sin `huella_habilitada` ni V016 | Respaldar y ensayar en staging antes de cualquier cambio aprobado |
| Esquema V017 | Producción: `uk_face_user` presente, sin `activo_acceso_android` ni V017 | Respaldar y ensayar en staging; comprobar físicamente la tabla de V013 y el estado V014 |
| V012 y rostro complementario | Producción: V012 ausente de `wp_migraciones` y `wp_rostros_cuenta` inexistente; la copia anterior de estructura difería | Mantener V012 fuera de v6. Decidir por separado si se necesita captura complementaria y cómo validar ese rostro antes de habilitar login |
| Sesiones | Producción: `wp_sesiones` InnoDB; `metodo` admite `PASSWORD`, `BIOMETRIA` y `FACIAL`, sin NULL | Reproducir exactamente en staging y ensayar sesiones por método/dispositivo |
| KPI oficiales | Producción: 0 definiciones, 0 asignaciones y 0 evaluaciones | Empresa debe definir/aprobar los 13 cruces cargo↔definición↔asignación; no declarar entrega KPI operativa |
| Autenticación y permisos | Pruebas locales con cuentas sintéticas y servicios PHP publicados | Verificar en staging empresarial `/auth/login`, `/auth/me`, `id_trabajador`, cargo, rol, agencia y permisos de cuentas existentes autorizadas |
| Biometría | Pruebas HTTP/SQL sintéticas cubren huella, rostro, dos dispositivos, sesiones y reintento | Pruebas físicas de claves de Android Keystore, cámara, desconexión y reinstalación según la lista adjunta |
| Reversión | V016/V017 `down` tienen condiciones que evitan perder estados activos | Ensayar respaldo y reversión **en staging**, preservando sesiones y vínculos; no forzar un `down` con cambios de datos |
| Pruebas de V6 | 583 comprobaciones PHP/HTTP/MySQL y 15 validaciones pasaron con los 18 archivos exactos sobre staging local aislado; V015/V016/V017 aplicadas allí | Repetir en staging de la empresa **sin V012 ni `wp_rostros_cuenta`** y en teléfonos; el ensayo anterior no acredita esta variante exacta |

Consultas preparadas: [CONSULTAS_SOLO_LECTURA_CPANEL_2026-11-12.sql](CONSULTAS_SOLO_LECTURA_CPANEL_2026-11-12.sql). Pruebas físicas: [PRUEBAS_FISICAS_BIOMETRIA_2026-11-12.md](PRUEBAS_FISICAS_BIOMETRIA_2026-11-12.md). Matriz de formularios: [KPI_FORMULARIOS_PUBLICACION_2026-11-12.md](KPI_FORMULARIOS_PUBLICACION_2026-11-12.md).

Plan actualizado con la ausencia física confirmada de V012 y los recuentos actuales: [PLAN_PUBLICACION_CONFIRMADO_2026-11-12.md](PLAN_PUBLICACION_CONFIRMADO_2026-11-12.md).
