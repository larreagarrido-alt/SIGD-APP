package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.kpi.model.KpiSalesDay;

/**
 * Gestor semanal de métricas comerciales para el cargo Asesor de Ventas.
 * Mantiene la persistencia local de los 6 días reales de la semana en curso (Lunes a Sábado):
 * - Abre posicionado por defecto en el día real actual.
 * - Permite navegar libremente a días anteriores sin bloquear días posteriores.
 * - Una vez enviado un día con biometría, queda en modo solo lectura con opacidad.
 */
public class KpiSalesWeekManager {

    private static final String KEY_WEEK_ID = "active_week_id";
    private static KpiSalesWeekManager instance;

    // Metas semanales fijadas
    public static final int GOAL_LLAMADAS = 100;
    public static final int GOAL_VISITAS = 60;
    public static final int GOAL_COTIZACIONES = 30;
    public static final int GOAL_SEGUIMIENTO = 50;
    public static final int GOAL_PROSPECTOS = 15;
    public static final int GOAL_CLIENTES = 3;

    private final SharedPreferences prefs;
    private final Context appContext;
    private final List<KpiSalesDay> days;
    private String activeWeekRangeLabel = "";
    private String currentUserId = "";

    private KpiSalesWeekManager(Context context, String userId) {
        this.appContext = context.getApplicationContext();
        this.currentUserId = (userId != null && !userId.trim().isEmpty()) ? userId.trim() : "EMP-DEFAULT";
        String sanitized = currentUserId.replaceAll("[^a-zA-Z0-9_]", "_");
        this.prefs = context.getApplicationContext().getSharedPreferences("kpi_sales_week_prefs_" + sanitized, Context.MODE_PRIVATE);
        this.days = new ArrayList<>();
        ensureCurrentWeekAndInitDays();
        loadFromPrefs();
    }

    public static synchronized KpiSalesWeekManager getInstance(Context context) {
        String activeUid = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserId();
        pe.net.ws.kpiweb.core.session.SessionManager serverSession = new pe.net.ws.kpiweb.core.session.SessionManager(context);
        if (serverSession.isLoggedIn() && serverSession.getUserId() > 0) activeUid = "user_" + serverSession.getUserId();
        if (activeUid == null || activeUid.trim().isEmpty()) {
            activeUid = "EMP-DEFAULT";
        }
        if (instance == null || !activeUid.equals(instance.currentUserId)) {
            instance = new KpiSalesWeekManager(context, activeUid);
        }
        return instance;
    }

    private void ensureCurrentWeekAndInitDays() {
        Calendar now = Calendar.getInstance();
        now.setFirstDayOfWeek(Calendar.MONDAY);
        int year = now.get(Calendar.YEAR);
        int week = now.get(Calendar.WEEK_OF_YEAR);
        String currentWeekKey = year + "-W" + week;

        String storedWeekKey = prefs.getString(KEY_WEEK_ID, "");
        if (!currentWeekKey.equals(storedWeekKey)) {
            prefs.edit().clear().putString(KEY_WEEK_ID, currentWeekKey).apply();
        }

        Calendar monday = (Calendar) now.clone();
        int dow = monday.get(Calendar.DAY_OF_WEEK);
        int diffToMonday = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        monday.add(Calendar.DAY_OF_MONTH, -diffToMonday);

        String[] dayNames = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        String[] shortNames = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};
        SimpleDateFormat fullFmt = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        SimpleDateFormat rangeFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        days.clear();
        String startStr = "";
        String endStr = "";
        for (int i = 0; i < 6; i++) {
            Calendar dayCal = (Calendar) monday.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, i);
            int dayNum = dayCal.get(Calendar.DAY_OF_MONTH);
            String formatted = dayNames[i] + ", " + fullFmt.format(dayCal.getTime());
            days.add(new KpiSalesDay(i, dayNames[i], shortNames[i], dayNum, formatted));
            if (i == 0) startStr = rangeFmt.format(dayCal.getTime());
            if (i == 5) endStr = rangeFmt.format(dayCal.getTime());
        }
        activeWeekRangeLabel = "Del " + startStr + " al " + endStr;
    }

    private void loadFromPrefs() {
        for (int i = 0; i < days.size(); i++) {
            KpiSalesDay d = days.get(i);
            d.setSaved(prefs.getBoolean("day_" + i + "_saved", false));
            d.setSavedAt(prefs.getString("day_" + i + "_savedAt", ""));
            d.setObservaciones(prefs.getString("day_" + i + "_obs", ""));
            d.setLlamadas(prefs.getInt("day_" + i + "_llamadas", 0));
            d.setVisitas(prefs.getInt("day_" + i + "_visitas", 0));
            d.setCotizaciones(prefs.getInt("day_" + i + "_cotizaciones", 0));
            d.setSeguimiento(prefs.getInt("day_" + i + "_seguimiento", 0));
            d.setProspectos(prefs.getInt("day_" + i + "_prospectos", 0));
            d.setClientes(prefs.getInt("day_" + i + "_clientes", 0));
        }
    }

    public void persistDay(int index) {
        if (index < 0 || index >= days.size()) return;
        KpiSalesDay d = days.get(index);
        prefs.edit()
                .putBoolean("day_" + index + "_saved", d.isSaved())
                .putString("day_" + index + "_savedAt", d.getSavedAt())
                .putString("day_" + index + "_obs", d.getObservaciones())
                .putInt("day_" + index + "_llamadas", d.getLlamadas())
                .putInt("day_" + index + "_visitas", d.getVisitas())
                .putInt("day_" + index + "_cotizaciones", d.getCotizaciones())
                .putInt("day_" + index + "_seguimiento", d.getSeguimiento())
                .putInt("day_" + index + "_prospectos", d.getProspectos())
                .putInt("day_" + index + "_clientes", d.getClientes())
                .apply();
    }

    public String getActiveWeekRangeLabel() {
        return (activeWeekRangeLabel != null && !activeWeekRangeLabel.isEmpty()) ? activeWeekRangeLabel : "Del Lunes al Sábado";
    }

    public List<KpiSalesDay> getDays() {
        return days;
    }

    public KpiSalesDay getDay(int index) {
        if (index < 0 || index >= days.size()) return days.get(0);
        return days.get(index);
    }

    public int getSavedDaysCount() {
        int count = 0;
        for (KpiSalesDay d : days) {
            if (d.isSaved()) count++;
        }
        return count;
    }

    public boolean isWeekComplete() {
        return getSavedDaysCount() == 6;
    }

    /**
     * Retorna el índice del día actual real de la semana (0 = Lunes .. 5 = Sábado/Domingo).
     */
    public int getTodayDayIndex() {
        Calendar now = Calendar.getInstance();
        int dow = now.get(Calendar.DAY_OF_WEEK);
        switch (dow) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY:
            default: return 5;
        }
    }

    public int getFirstUnsavedDayIndex() {
        for (int i = 0; i < days.size(); i++) {
            if (!days.get(i).isSaved()) {
                return i;
            }
        }
        return 5;
    }

    /**
     * Busca el siguiente día pendiente de llenar después de {@code fromIndex}.
     * Si no hay posteriores pendientes, revisa días previos sin llenar.
     * Retorna -1 si los 6 días ya fueron enviados.
     */
    public int getNextUnsavedDayIndexAfter(int fromIndex) {
        for (int i = fromIndex + 1; i < days.size(); i++) {
            if (!days.get(i).isSaved()) {
                return i;
            }
        }
        for (int i = 0; i < fromIndex; i++) {
            if (!days.get(i).isSaved()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Todos los días de Lunes a Sábado están disponibles para abrirse (por ejemplo, abrir Miércoles
     * por defecto y poder retroceder libremente al Lunes o Martes para llenarlos).
     */
    public boolean isDayUnlocked(int dayIndex) {
        return dayIndex >= 0 && dayIndex < days.size();
    }

    public int getTotalLlamadas() {
        int sum = 0;
        for (KpiSalesDay d : days) sum += d.getLlamadas();
        return sum;
    }

    public int getTotalVisitas() {
        int sum = 0;
        for (KpiSalesDay d : days) sum += d.getVisitas();
        return sum;
    }

    public int getTotalCotizaciones() {
        int sum = 0;
        for (KpiSalesDay d : days) sum += d.getCotizaciones();
        return sum;
    }

    public int getTotalSeguimiento() {
        int sum = 0;
        for (KpiSalesDay d : days) sum += d.getSeguimiento();
        return sum;
    }

    public int getTotalProspectos() {
        int sum = 0;
        for (KpiSalesDay d : days) sum += d.getProspectos();
        return sum;
    }

    public int getTotalClientes() {
        int sum = 0;
        for (KpiSalesDay d : days) sum += d.getClientes();
        return sum;
    }

    public int getTotalActivities() {
        return getTotalLlamadas() + getTotalVisitas() + getTotalCotizaciones()
                + getTotalSeguimiento() + getTotalProspectos() + getTotalClientes();
    }

    public int getOverallCompliancePercent() {
        double pLlamadas = Math.min(150.0, (getTotalLlamadas() * 100.0) / GOAL_LLAMADAS);
        double pVisitas = Math.min(150.0, (getTotalVisitas() * 100.0) / GOAL_VISITAS);
        double pCotiz = Math.min(150.0, (getTotalCotizaciones() * 100.0) / GOAL_COTIZACIONES);
        double pSeg = Math.min(150.0, (getTotalSeguimiento() * 100.0) / GOAL_SEGUIMIENTO);
        double pProsp = Math.min(150.0, (getTotalProspectos() * 100.0) / GOAL_PROSPECTOS);
        double pCli = Math.min(150.0, (getTotalClientes() * 100.0) / GOAL_CLIENTES);

        double avg = (pLlamadas + pVisitas + pCotiz + pSeg + pProsp + pCli) / 6.0;
        return (int) Math.round(avg);
    }

    public void saveDayData(int index, int llamadas, int visitas, int cotizaciones, int seguimiento, int prospectos, int clientes, String obs) {
        if (index < 0 || index >= days.size()) return;
        KpiSalesDay d = days.get(index);
        d.setLlamadas(llamadas);
        d.setVisitas(visitas);
        d.setCotizaciones(cotizaciones);
        d.setSeguimiento(seguimiento);
        d.setProspectos(prospectos);
        d.setClientes(clientes);
        d.setObservaciones(obs);
        d.setSaved(true);
        if (d.getSavedAt().isEmpty()) {
            d.setSavedAt(new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
        }
        persistDay(index);
        pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                appContext, "VENTAS", activeWeekRangeLabel + ":" + index, d);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiSalesDay report = new com.google.gson.Gson().fromJson(payload, KpiSalesDay.class);
        if (report == null || report.getDayIndex() < 0 || report.getDayIndex() >= days.size()) return false;
        KpiSalesDay local = days.get(report.getDayIndex());
        if ((local.isSaved() && !allowReplace) || report.getFullDateFormatted() == null
                || !report.getFullDateFormatted().equals(local.getFullDateFormatted())) return false;
        days.set(report.getDayIndex(), report);
        persistDay(report.getDayIndex());
        return true;
    }

    public void resetWeek() {
        prefs.edit().clear().apply();
        ensureCurrentWeekAndInitDays();
    }

    public String generateClipboardSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("website ® · Agencia Creativa\n");
        sb.append("Resumen semanal ").append(activeWeekRangeLabel).append("\n");
        String advisor = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        sb.append("Asesor: ").append((advisor != null && !advisor.isEmpty()) ? advisor : "Asesor Comercial").append(" · Comercial\n\n");

        for (KpiSalesDay d : days) {
            sb.append("● ").append(d.getDayName()).append(" ").append(d.getDayNumber()).append(": ");
            if (d.isSaved()) {
                sb.append(d.getLlamadas()).append(" llamadas, ")
                        .append(d.getVisitas()).append(" visitas, ")
                        .append(d.getCotizaciones()).append(" cotiz., ")
                        .append(d.getSeguimiento()).append(" seg., ")
                        .append(d.getProspectos()).append(" prosp., ")
                        .append(d.getClientes()).append(" clientes.");
            } else {
                sb.append("Sin registrar.");
            }
            sb.append("\n");
        }

        sb.append("\nTOTALES ACUMULADOS:\n");
        sb.append("• Llamadas comerciales: ").append(getTotalLlamadas()).append(" / ").append(GOAL_LLAMADAS).append("\n");
        sb.append("• Nuevas visitas a empresas: ").append(getTotalVisitas()).append(" / ").append(GOAL_VISITAS).append("\n");
        sb.append("• Cotizaciones enviadas: ").append(getTotalCotizaciones()).append(" / ").append(GOAL_COTIZACIONES).append("\n");
        sb.append("• Empresas en seguimiento: ").append(getTotalSeguimiento()).append(" / ").append(GOAL_SEGUIMIENTO).append("\n");
        sb.append("• Nuevos prospectos: ").append(getTotalProspectos()).append(" / ").append(GOAL_PROSPECTOS).append("\n");
        sb.append("• Nuevos clientes: ").append(getTotalClientes()).append(" / ").append(GOAL_CLIENTES).append("\n");
        sb.append("Cumplimiento general: ").append(getOverallCompliancePercent()).append("%\n");

        return sb.toString();
    }
}
