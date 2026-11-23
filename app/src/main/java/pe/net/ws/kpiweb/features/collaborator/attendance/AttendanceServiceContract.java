package pe.net.ws.kpiweb.features.collaborator.attendance;

import java.util.List;

/**
 * Contrato formal para la futura integración con el Sistema de Gestión de Asistencias anterior.
 *
 * <p>Define las especificaciones de interfaz para la conexión mediante API REST/JSON:
 * <ul>
 *   <li><b>GET /api/v1/attendance/summary</b>: Consulta métricas consolidadas (porcentaje, asistencias, tardanzas, faltas).</li>
 *   <li><b>GET /api/v1/attendance/records</b>: Lista cronológica de marcaciones de entrada y salida con estado y justificaciones.</li>
 *   <li><b>POST /api/v1/attendance/justifications</b>: Envío de justificaciones documentales por tardanza o ausencia.</li>
 * </ul>
 *
 * <p>Mientras el backend institucional conecte el servicio, el repositorio {@link CollaboratorAttendanceRepository}
 * implementa este contrato en modo demostración desacoplado para garantizar la estabilidad de la aplicación.
 */
public interface AttendanceServiceContract {

    interface AttendanceCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    /**
     * Obtiene el resumen mensual consolidado para un colaborador.
     * @param collaboratorId Identificador institucional del colaborador (ej. "collab_01")
     * @param monthYear Período en formato "yyyy-MM" (ej. "2026-10")
     * @param callback Retorno asíncrono con el resumen o error de conexión
     */
    void getAttendanceSummary(String collaboratorId, String monthYear, AttendanceCallback<AttendanceSummary> callback);

    /**
     * Obtiene los registros diarios de marcaciones para un colaborador en un período.
     * @param collaboratorId Identificador institucional del colaborador
     * @param monthYear Período en formato "yyyy-MM"
     * @param callback Retorno asíncrono con la lista de marcaciones o error de conexión
     */
    void getDailyRecords(String collaboratorId, String monthYear, AttendanceCallback<List<AttendanceRecord>> callback);
}

