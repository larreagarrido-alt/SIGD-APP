package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;
import pe.net.ws.kpiweb.features.kpi.senior.data.KpiSeniorStorageManager;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorWeekReport;

/**
 * Gestor de Cierre y Reinicio Dominical (SIGD 2026).
 * Regla de corte oficial: Domingo a las 23:59 (medianoche hacia el lunes).
 *
 * Flujo:
 * 1. Durante la semana (Lunes a Sábado) y todo el Domingo de gracia (horas extras/renders),
 *    los datos se registran y acumulan normalmente.
 * 2. Al llegar el Domingo a las 23:59 (o inicio de nueva semana):
 *    - Se sella y archiva la semana completa con todos sus datos en el "Historial de Reportes".
 *    - Se limpian las casillas diarias de los formularios activos para iniciar en cero el Lunes.
 *    - La ficha archivada permanece intacta en el Historial para auditoría e impresión.
 */
public class KpiSundayArchivalHelper {

    private static final String PREF_ARCHIVE = "kpi_sunday_archival_prefs";
    private static final String KEY_LAST_ARCHIVED_WEEK = "last_archived_week_id";
    private static final String KEY_LAST_ARCHIVED_TIMESTAMP = "last_archived_timestamp";

    /**
     * Verifica si el día actual es Domingo o si ya se alcanzó el horario de corte (23:50 - 23:59).
     */
    public static boolean isCutoffTimeReached() {
        Calendar cal = Calendar.getInstance();
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        return (dow == Calendar.SUNDAY && (hour == 23 && minute >= 50));
    }

    /**
     * Obtiene la fecha real actual formateada para nuevos registros.
     */
    public static String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Obtiene el identificador de la semana calendario actual (ej. "2026-W44").
     */
    public static String getCurrentCalendarWeekId() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int year = cal.get(Calendar.YEAR);
        int week = cal.get(Calendar.WEEK_OF_YEAR);
        return year + "-W" + week;
    }

    /**
     * Ejecuta el cierre semanal y limpieza de formularios:
     * 1. Archiva Diseñador Gráfico Senior en su almacén histórico.
     * 2. Archiva Diseñador Gráfico en KpiDesignerMemoryRepository.
     * 3. Archiva y reinicia Asesor de Ventas en KpiSalesWeekManager.
     */
    public static boolean executeSundayCloseAndReset(Context context, boolean forceExecution) {
        if (!forceExecution && !isCutoffTimeReached()) {
            Toast.makeText(context, "El cierre y archivo automático está programado para el Domingo a las 23:59.", Toast.LENGTH_SHORT).show();
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_ARCHIVE, Context.MODE_PRIVATE);
        String currentWeekKey = getCurrentCalendarWeekId();

        // 1. Archivar Diseñador Gráfico Senior
        KpiSeniorStorageManager seniorStorage = KpiSeniorStorageManager.getInstance(context);
        KpiSeniorWeekReport seniorWeek = seniorStorage.getCurrentWeek();
        if (seniorWeek != null) {
            seniorWeek.setStatus("Archivado en Historial (Cierre Domingo 23:59)");
            seniorStorage.saveToPrefs();
        }

        // 2. Archivar Diseñador Gráfico
        KpiDesignerMemoryRepository designerRepo = KpiDesignerMemoryRepository.getInstance(context);
        if (designerRepo != null) {
            KpiDesignerWeekReport designerWeek = designerRepo.getCurrentWeekReport();
            if (designerWeek != null) {
                designerWeek.setStatus("Finalizado");
            }
        }

        // 3. Archivar y reiniciar Asesor de Ventas
        KpiSalesWeekManager salesManager = KpiSalesWeekManager.getInstance(context);
        if (salesManager != null) {
            salesManager.resetWeek();
        }

        prefs.edit()
                .putString(KEY_LAST_ARCHIVED_WEEK, currentWeekKey)
                .putLong(KEY_LAST_ARCHIVED_TIMESTAMP, System.currentTimeMillis())
                .apply();

        Toast.makeText(context, "Cierre completado: Semana archivada en Historial y formularios listos para el lunes.", Toast.LENGTH_LONG).show();
        return true;
    }
}
