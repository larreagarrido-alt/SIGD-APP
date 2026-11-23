package pe.net.ws.kpiweb.features.kpi.designer.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerHistorySummary;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;

/**
 * Repositorio exclusivo en memoria RAM para el Diseñador Gráfico.
 * Gestiona de forma dinámica la semana actual según el calendario del sistema:
 * - Reconoce el día real y calcula el rango de Lunes a Sábado de la semana en curso.
 * - Registra la producción día a día; los reportes semanales se van actualizando
 *   en tiempo real acumulando los días efectivamente registrados.
 * - Cada nueva semana calendario se reinicia automáticamente en un nuevo ciclo,
 *   archivando la semana previa en el historial.
 * - Cumple estrictamente la regla de NO usar bases de datos externas ni SQLite/Room.
 */
public class KpiDesignerMemoryRepository {

    private static final String PREF_NAME_DEFAULT = "kpi_designer_storage_v1";
    private static final String KEY_CURRENT_WEEK = "key_current_week";
    private static final String KEY_HISTORY = "key_history_weeks";
    private static android.content.Context sAppContext;

    private static volatile KpiDesignerMemoryRepository instance;
    private String currentUserId = "";

    private KpiDesignerWeekReport currentWeekReport;
    private String currentWeekKey; // Ej: "2026-W42"

    private final Map<String, KpiDesignerWeekReport> weekStore = new HashMap<>();
    private final List<KpiDesignerWeekReport> historyList = new ArrayList<>();
    private KpiDesignerHistorySummary historySummary;

    public static synchronized void init(android.content.Context context) {
        if (context != null) {
            sAppContext = context.getApplicationContext();
            if (instance != null) {
                instance.loadFromPrefs();
            }
        }
    }

    public static synchronized KpiDesignerMemoryRepository getInstance(android.content.Context context) {
        if (context != null) {
            sAppContext = context.getApplicationContext();
        }
        return getInstance();
    }

    private KpiDesignerMemoryRepository(String userId) {
        this.currentUserId = (userId != null && !userId.trim().isEmpty()) ? userId.trim() : "EMP-DEFAULT";
        loadFromPrefs();
        if (weekStore.isEmpty()) {
            if (sAppContext == null || !new pe.net.ws.kpiweb.core.session.SessionManager(sAppContext).isLoggedIn())
                seedInitialHistory();
            ensureCurrentWeekActive();
            saveToPrefs();
        } else {
            ensureCurrentWeekActive();
        }
    }

    public static synchronized KpiDesignerMemoryRepository getInstance() {
        String activeUserId = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserId();
        if (sAppContext != null) {
            pe.net.ws.kpiweb.core.session.SessionManager serverSession = new pe.net.ws.kpiweb.core.session.SessionManager(sAppContext);
            if (serverSession.isLoggedIn() && serverSession.getUserId() > 0)
                activeUserId = "user_" + serverSession.getUserId();
        }
        if (activeUserId == null || activeUserId.trim().isEmpty()) {
            activeUserId = "EMP-DEFAULT";
        }
        if (instance == null || !activeUserId.equals(instance.currentUserId)) {
            instance = new KpiDesignerMemoryRepository(activeUserId);
        }
        return instance;
    }

    private String getPrefName() {
        String sanitized = (currentUserId != null && !currentUserId.isEmpty())
                ? currentUserId.replaceAll("[^a-zA-Z0-9_]", "_") : "v1";
        return "kpi_designer_storage_" + sanitized;
    }

    /**
     * Semanas históricas consolidadas previas (para consulta y auditoría de reportes).
     */
    private void seedInitialHistory() {
        KpiDesignerWeekReport s5 = new KpiDesignerWeekReport(
                "Semana 5", 5, "08/09/2026 - 13/09/2026", "Del 08 al 13 de septiembre de 2026", "Finalizado");

        List<KpiDesignerDayRecord> s5Days = new ArrayList<>();
        s5Days.add(new KpiDesignerDayRecord("08/09/2026", "LUNES", "Modalidad B (Audiovisual + Diseño)",
                "02 v + 02 d", 2, 2, 0, 2, 2, 0, "Entrega a tiempo", true));
        s5Days.add(new KpiDesignerDayRecord("09/09/2026", "MARTES", "Modalidad A (Diseño gráfico)",
                "06 diseños", 6, 0, 0, 6, 0, 0, "Campaña redes completada", true));
        s5Days.add(new KpiDesignerDayRecord("10/09/2026", "MIÉRCOLES", "Modalidad B (Audiovisual + Diseño)",
                "02 v + 02 d", 2, 2, 0, 3, 2, 0, "Superó la meta de diseños", true));
        s5Days.add(new KpiDesignerDayRecord("11/09/2026", "JUEVES", "Modalidad A (Diseño gráfico)",
                "06 diseños", 6, 0, 0, 5, 0, 0, "Ajuste de formatos para reels", true));
        s5Days.add(new KpiDesignerDayRecord("12/09/2026", "VIERNES", "Modalidad B (Audiovisual + Diseño)",
                "02 v + 02 d", 2, 2, 0, 2, 1, 0, "Video adicional en revisión", true));
        s5Days.add(new KpiDesignerDayRecord("13/09/2026", "SÁBADO", "Sesión fotográfica (Producción)",
                "01 sesión", 0, 0, 1, 0, 0, 1, "Sesión de producto en estudio", true));

        s5.setDays(s5Days);
        s5.setGeneralObservations("Semana con buen desempeño. Se cumplió con la mayoría de metas diarias asignadas.");
        s5.setActionsBiometricVerified(true);
        weekStore.put(s5.getWeekId(), s5);

        KpiDesignerWeekReport s4 = createMockWeek("Semana 4", 4, "01/09/2026 - 06/09/2026", "Del 01 al 06 de septiembre de 2026", 16, 5, 1);
        KpiDesignerWeekReport s3 = createMockWeek("Semana 3", 3, "25/08/2026 - 30/08/2026", "Del 25 al 30 de agosto de 2026", 14, 5, 1);
        KpiDesignerWeekReport s2 = createMockWeek("Semana 2", 2, "18/08/2026 - 23/08/2026", "Del 18 al 23 de agosto de 2026", 12, 4, 0);

        weekStore.put(s4.getWeekId(), s4);
        weekStore.put(s3.getWeekId(), s3);
        weekStore.put(s2.getWeekId(), s2);

        historyList.add(s5);
        historyList.add(s4);
        historyList.add(s3);
        historyList.add(s2);

        List<KpiDesignerHistorySummary.EvolutionPoint> evol = new ArrayList<>();
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S1", 50));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S2", 64));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S3", 80));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S4", 88));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S5", 96));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S6", 80));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S7", 85));
        evol.add(new KpiDesignerHistorySummary.EvolutionPoint("S8", 92));

        historySummary = new KpiDesignerHistorySummary(142, 52, 8, 87, evol);
    }

    private KpiDesignerWeekReport createMockWeek(String id, int num, String range, String fullRange,
                                                 int designs, int videos, int photos) {
        KpiDesignerWeekReport rep = new KpiDesignerWeekReport(id, num, range, fullRange, "Finalizado");
        List<KpiDesignerDayRecord> dList = new ArrayList<>();
        dList.add(new KpiDesignerDayRecord("01/09/2026", "LUNES", "Modalidad B", "02 v + 02 d", 2, 2, 0, 2, 1, 0, "", true));
        dList.add(new KpiDesignerDayRecord("02/09/2026", "MARTES", "Modalidad A", "06 diseños", 6, 0, 0, 5, 0, 0, "", true));
        dList.add(new KpiDesignerDayRecord("03/09/2026", "MIÉRCOLES", "Modalidad B", "02 v + 02 d", 2, 2, 0, 2, 1, 0, "", true));
        dList.add(new KpiDesignerDayRecord("04/09/2026", "JUEVES", "Modalidad A", "06 diseños", 6, 0, 0, 4, 0, 0, "", true));
        dList.add(new KpiDesignerDayRecord("05/09/2026", "VIERNES", "Modalidad B", "02 v + 02 d", 2, 2, 0, 2, 1, 0, "", true));
        dList.add(new KpiDesignerDayRecord("06/09/2026", "SÁBADO", "Fotografía", "01 sesión", 0, 0, 1, 0, 0, photos, "", true));
        rep.setDays(dList);
        rep.setGeneralObservations("Reporte finalizado para " + id);
        return rep;
    }

    /**
     * Valida y garantiza la semana actual en base a la fecha real del sistema.
     * Si la semana ha cambiado respecto al ciclo anterior, archiva la previa y reinicia una limpia.
     */
    public synchronized KpiDesignerWeekReport getCurrentWeekReport() {
        ensureCurrentWeekActive();
        return currentWeekReport;
    }

    private synchronized void ensureCurrentWeekActive() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int year = cal.get(Calendar.YEAR);
        int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
        String weekKey = year + "-W" + weekOfYear;

        if (currentWeekReport != null && weekKey.equals(currentWeekKey)) {
            return;
        }

        // Si ya existía una semana anterior con días registrados, archivamos en el historial
        if (currentWeekReport != null && currentWeekReport.getDays() != null && !currentWeekReport.getDays().isEmpty()) {
            if (!weekStore.containsKey(currentWeekReport.getWeekId())) {
                weekStore.put(currentWeekReport.getWeekId(), currentWeekReport);
            }
            if (!historyList.contains(currentWeekReport)) {
                historyList.add(0, currentWeekReport);
            }
        }

        // Calcular Lunes y Sábado de la semana real en curso
        Calendar weekCal = (Calendar) cal.clone();
        int dow = weekCal.get(Calendar.DAY_OF_WEEK);
        int diffToMonday = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        weekCal.add(Calendar.DAY_OF_MONTH, -diffToMonday);
        Date mondayDate = weekCal.getTime();

        weekCal.add(Calendar.DAY_OF_MONTH, 5);
        Date saturdayDate = weekCal.getTime();

        SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dfMonth = new SimpleDateFormat("dd 'de' MMMM", new Locale("es", "ES"));
        SimpleDateFormat dfYear = new SimpleDateFormat("yyyy", Locale.getDefault());

        String dateRange = dfShort.format(mondayDate) + " - " + dfShort.format(saturdayDate);
        String dateRangeFull = "Semana del " + dfMonth.format(mondayDate) + " al " + dfMonth.format(saturdayDate) + " de " + dfYear.format(saturdayDate);
        String weekId = "Semana " + weekOfYear;

        currentWeekReport = new KpiDesignerWeekReport(weekId, weekOfYear, dateRange, dateRangeFull, "En progreso");
        currentWeekReport.setDays(new ArrayList<>());
        this.currentWeekKey = weekKey;

        weekStore.put(weekId, currentWeekReport);
    }

    /**
     * Guarda o actualiza el registro diario de producción.
     * Inmediatamente recalcula las métricas de la semana para que los reportes
     * reflejen los días acumulados en tiempo real.
     */
    public synchronized void saveDayRecord(KpiDesignerDayRecord record) {
        if (record == null) return;
        ensureCurrentWeekActive();

        List<KpiDesignerDayRecord> currentDays = currentWeekReport.getDays();
        if (currentDays == null) {
            currentDays = new ArrayList<>();
        }

        boolean updated = false;
        for (int i = 0; i < currentDays.size(); i++) {
            KpiDesignerDayRecord existing = currentDays.get(i);
            if (existing.getDayOfWeek().equalsIgnoreCase(record.getDayOfWeek())
                    || (existing.getDate() != null && existing.getDate().equals(record.getDate()))) {
                currentDays.set(i, record);
                updated = true;
                break;
            }
        }

        if (!updated) {
            currentDays.add(record);
        }

        // Ordenar cronológicamente (Lunes a Sábado)
        Collections.sort(currentDays, (a, b) -> Integer.compare(getDayOrder(a.getDayOfWeek()), getDayOrder(b.getDayOfWeek())));

        currentWeekReport.setDays(currentDays);
        currentWeekReport.recalculateMetrics();
        saveToPrefs();
        syncReport(currentWeekReport);
    }

    /**
     * Sella y bloquea el reporte final de la semana cuando el usuario presiona
     * "Enviar Reporte Final" con verificación biométrica.
     */
    public synchronized void submitFinalWeekReport() {
        ensureCurrentWeekActive();
        if (currentWeekReport == null) return;
        List<KpiDesignerDayRecord> currentDays = currentWeekReport.getDays();
        if (currentDays != null) {
            for (KpiDesignerDayRecord d : currentDays) {
                d.setLocked(true);
                d.setBiometricVerified(true);
            }
        }
        currentWeekReport.setStatus("Finalizado");
        currentWeekReport.setActionsBiometricVerified(true);
        currentWeekReport.recalculateMetrics();
        weekStore.put(currentWeekReport.getWeekId(), currentWeekReport);
        if (!historyList.contains(currentWeekReport)) {
            historyList.add(0, currentWeekReport);
        }
        saveToPrefs();
        syncReport(currentWeekReport);
    }

    /**
     * Desbloquea los días de la semana en curso para permitir edición.
     */
    public synchronized void unlockCurrentWeekForEditing() {
        ensureCurrentWeekActive();
        if (currentWeekReport == null) return;
        List<KpiDesignerDayRecord> currentDays = currentWeekReport.getDays();
        if (currentDays != null) {
            for (KpiDesignerDayRecord d : currentDays) {
                d.setLocked(false);
            }
        }
        currentWeekReport.setStatus("En progreso");
        saveToPrefs();
        syncReport(currentWeekReport);
    }

    public synchronized boolean isCurrentWeekFinalSubmitted(String roleName) {
        ensureCurrentWeekActive();
        return currentWeekReport != null && "Finalizado".equalsIgnoreCase(currentWeekReport.getStatus());
    }

    /**
     * Calcula qué días desde el Lunes hasta el día actual aún no han sido marcados.
     * Usado tanto por la alerta previa al envío como por la Notificación de las 8:00 PM.
     */
    public synchronized List<String> getMissingDaysUpToToday(String roleName) {
        ensureCurrentWeekActive();
        List<String> missing = new ArrayList<>();
        String[] orderedDays = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};

        Calendar today = Calendar.getInstance();
        int dow = today.get(Calendar.DAY_OF_WEEK);
        int maxOrder;
        switch (dow) {
            case Calendar.MONDAY: maxOrder = 1; break;
            case Calendar.TUESDAY: maxOrder = 2; break;
            case Calendar.WEDNESDAY: maxOrder = 3; break;
            case Calendar.THURSDAY: maxOrder = 4; break;
            case Calendar.FRIDAY: maxOrder = 5; break;
            default: maxOrder = 6; break;
        }

        for (int i = 0; i < maxOrder && i < orderedDays.length; i++) {
            String dayName = orderedDays[i];
            if (getRecordForDayOfWeek(dayName) == null) {
                missing.add(dayName);
            }
        }
        return missing;
    }

    private int getDayOrder(String dayOfWeek) {
        if (dayOfWeek == null) return 99;
        switch (dayOfWeek.toUpperCase(Locale.ROOT)) {
            case "LUNES": return 1;
            case "MARTES": return 2;
            case "MIÉRCOLES":
            case "MIERCOLES": return 3;
            case "JUEVES": return 4;
            case "VIERNES": return 5;
            case "SÁBADO":
            case "SABADO": return 6;
            default: return 7;
        }
    }

    /**
     * Retorna el registro de un día de la semana si ya fue completado y guardado.
     */
    public synchronized KpiDesignerDayRecord getRecordForDayOfWeek(String dayOfWeek) {
        ensureCurrentWeekActive();
        if (currentWeekReport == null || currentWeekReport.getDays() == null) return null;
        for (KpiDesignerDayRecord d : currentWeekReport.getDays()) {
            if (d.getDayOfWeek().equalsIgnoreCase(dayOfWeek)) {
                return d;
            }
        }
        return null;
    }

    /**
     * Indica si un día de la semana actual ya fue enviado y bloqueado con biometría.
     */
    public synchronized boolean isDayRecorded(String dayOfWeek) {
        KpiDesignerDayRecord rec = getRecordForDayOfWeek(dayOfWeek);
        return rec != null && rec.isLocked();
    }

    public synchronized boolean areAllSixDaysLocked() {
        String[] orderedDays = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};
        for (String d : orderedDays) {
            if (!isDayRecorded(d)) return false;
        }
        return true;
    }

    /**
     * Retorna el nombre del siguiente día pendiente (no enviado) en la semana actual después de {@code currentDayName},
     * o {@code null} si los 6 días ya fueron enviados.
     */
    public synchronized String getNextUnlockedDayNameAfter(String currentDayName) {
        ensureCurrentWeekActive();
        String[] orderedDays = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};
        int fromIdx = 0;
        for (int i = 0; i < orderedDays.length; i++) {
            if (orderedDays[i].equalsIgnoreCase(currentDayName)) {
                fromIdx = i;
                break;
            }
        }
        for (int i = fromIdx + 1; i < orderedDays.length; i++) {
            if (!isDayRecorded(orderedDays[i])) {
                return orderedDays[i];
            }
        }
        for (int i = 0; i < fromIdx; i++) {
            if (!isDayRecorded(orderedDays[i])) {
                return orderedDays[i];
            }
        }
        return null;
    }

    /**
     * Retorna el registro por fecha exacta dd/MM/yyyy.
     */
    public synchronized KpiDesignerDayRecord getRecordForDate(String dateStr) {
        ensureCurrentWeekActive();
        if (currentWeekReport == null || currentWeekReport.getDays() == null || dateStr == null) return null;
        for (KpiDesignerDayRecord d : currentWeekReport.getDays()) {
            if (dateStr.equals(d.getDate())) {
                return d;
            }
        }
        return null;
    }

    /**
     * Reinicia los registros de la semana en curso (para pruebas de ciclo limpio).
     */
    public synchronized void resetCurrentWeek() {
        ensureCurrentWeekActive();
        currentWeekReport.setDays(new ArrayList<>());
        saveToPrefs();
    }

    public synchronized List<KpiDesignerWeekReport> getHistoryList() {
        return new ArrayList<>(historyList);
    }

    public synchronized KpiDesignerWeekReport getWeekReport(String weekId) {
        if (currentWeekReport != null && currentWeekReport.getWeekId().equalsIgnoreCase(weekId)) {
            return currentWeekReport;
        }
        return weekStore.get(weekId);
    }

    public synchronized boolean restoreFromServer(com.google.gson.JsonObject payload, boolean allowReplace) {
        KpiDesignerWeekReport report = new com.google.gson.Gson().fromJson(payload, KpiDesignerWeekReport.class);
        if (report == null || report.getWeekId() == null || report.getWeekId().isEmpty()) return false;
        KpiDesignerWeekReport local = weekStore.get(report.getWeekId());
        if (local != null && local.getDays() != null && !local.getDays().isEmpty() && !allowReplace) return false;
        weekStore.put(report.getWeekId(), report);
        if (currentWeekReport != null && report.getWeekId().equals(currentWeekReport.getWeekId())) currentWeekReport = report;
        else historyList.add(0, report);
        saveToPrefs();
        return true;
    }

    public synchronized void updateWeekActions(String weekId, boolean[] actions, boolean biometricVerified) {
        KpiDesignerWeekReport report = weekStore.get(weekId);
        if (report != null && actions != null) {
            report.setActionsPerformed(actions);
            report.setActionsBiometricVerified(biometricVerified);
            saveToPrefs();
            syncReport(report);
        }
    }

    private void syncReport(KpiDesignerWeekReport report) {
        if (sAppContext != null && report != null) pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                sAppContext, "DISENADOR", report.getWeekId(), report);
    }

    public synchronized KpiDesignerHistorySummary getHistorySummary() {
        return historySummary;
    }

    private synchronized void saveToPrefs() {
        if (sAppContext == null) return;
        try {
            android.content.SharedPreferences prefs = sAppContext.getSharedPreferences(getPrefName(), android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            if (currentWeekReport != null) {
                editor.putString(KEY_CURRENT_WEEK, currentWeekReport.toJson().toString());
            }
            org.json.JSONArray histArray = new org.json.JSONArray();
            for (KpiDesignerWeekReport r : historyList) {
                histArray.put(r.toJson());
            }
            editor.putString(KEY_HISTORY, histArray.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        if (sAppContext == null) return;
        try {
            android.content.SharedPreferences prefs = sAppContext.getSharedPreferences(getPrefName(), android.content.Context.MODE_PRIVATE);
            String curStr = prefs.getString(KEY_CURRENT_WEEK, null);
            if (curStr != null) {
                org.json.JSONObject obj = new org.json.JSONObject(curStr);
                currentWeekReport = KpiDesignerWeekReport.fromJson(obj);
                if (currentWeekReport != null) {
                    weekStore.put(currentWeekReport.getWeekId(), currentWeekReport);
                }
            }
            String histStr = prefs.getString(KEY_HISTORY, null);
            if (histStr != null) {
                org.json.JSONArray array = new org.json.JSONArray(histStr);
                historyList.clear();
                for (int i = 0; i < array.length(); i++) {
                    org.json.JSONObject obj = array.optJSONObject(i);
                    if (obj != null) {
                        KpiDesignerWeekReport r = KpiDesignerWeekReport.fromJson(obj);
                        if (r != null) {
                            historyList.add(r);
                            weekStore.put(r.getWeekId(), r);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
