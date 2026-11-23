package pe.net.ws.kpiweb.features.collaborator.attendance;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio desacoplado de Asistencias para el Colaborador.
 * Conforme al contrato {@link AttendanceServiceContract}, proporciona datos de demostración
 * contextualizados para los 3 perfiles oficiales de prueba (Lucía, Carlos, Ana),
 * claramente identificados como modo demostración mientras se habilita el endpoint real.
 */
public class CollaboratorAttendanceRepository implements AttendanceServiceContract {

    private static CollaboratorAttendanceRepository instance;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private CollaboratorAttendanceRepository() {}

    public static synchronized CollaboratorAttendanceRepository getInstance() {
        if (instance == null) {
            instance = new CollaboratorAttendanceRepository();
        }
        return instance;
    }

    @Override
    public void getAttendanceSummary(String collaboratorId, String monthYear, AttendanceCallback<AttendanceSummary> callback) {
        handler.postDelayed(() -> {
            boolean isPrevMonth = "2026-09".equals(monthYear);
            AttendanceSummary summary;
            if ("collab_02".equals(collaboratorId)) { // Carlos Mendoza
                summary = isPrevMonth
                        ? new AttendanceSummary("Septiembre 2026", 22, 21, 1, 0, 0, 95)
                        : new AttendanceSummary("Octubre 2026", 21, 20, 1, 0, 0, 95);
            } else if ("collab_03".equals(collaboratorId)) { // Ana Torres
                summary = isPrevMonth
                        ? new AttendanceSummary("Septiembre 2026", 22, 22, 0, 0, 0, 100)
                        : new AttendanceSummary("Octubre 2026", 21, 21, 0, 0, 0, 100);
            } else { // Lucía Gómez (Predeterminado)
                summary = isPrevMonth
                        ? new AttendanceSummary("Septiembre 2026", 22, 22, 0, 0, 0, 100)
                        : new AttendanceSummary("Octubre 2026", 21, 20, 1, 0, 0, 96);
            }
            callback.onSuccess(summary);
        }, 300);
    }

    @Override
    public void getDailyRecords(String collaboratorId, String monthYear, AttendanceCallback<List<AttendanceRecord>> callback) {
        handler.postDelayed(() -> {
            List<AttendanceRecord> records = new ArrayList<>();
            boolean isPrevMonth = "2026-09".equals(monthYear);

            if (isPrevMonth) {
                records.add(new AttendanceRecord("rec_s01", "30 Sep 2026", "Miércoles", "08:55", "18:05", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_s02", "29 Sep 2026", "Martes", "08:58", "18:00", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_s03", "28 Sep 2026", "Lunes", "08:50", "18:10", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_s04", "25 Sep 2026", "Viernes", "09:00", "18:00", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_s05", "24 Sep 2026", "Jueves", "08:52", "18:02", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
            } else {
                // Octubre 2026
                records.add(new AttendanceRecord("rec_01", "29 Oct 2026", "Jueves", "08:55", "18:05", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_02", "28 Oct 2026", "Miércoles", "08:58", "18:00", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_03", "27 Oct 2026", "Martes", "08:50", "18:10", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_04", "26 Oct 2026", "Lunes", "09:12", "18:15", AttendanceRecord.AttendanceStatus.TARDANZA, "Tráfico en vía principal reportado"));
                records.add(new AttendanceRecord("rec_05", "23 Oct 2026", "Viernes", "08:54", "18:00", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_06", "22 Oct 2026", "Jueves", "08:48", "18:05", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_07", "21 Oct 2026", "Miércoles", "08:50", "18:02", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_08", "20 Oct 2026", "Martes", "08:56", "18:00", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_09", "19 Oct 2026", "Lunes", "08:52", "18:10", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
                records.add(new AttendanceRecord("rec_10", "16 Oct 2026", "Viernes", "08:57", "18:00", AttendanceRecord.AttendanceStatus.ASISTENCIA, null));
            }

            callback.onSuccess(records);
        }, 350);
    }
}

