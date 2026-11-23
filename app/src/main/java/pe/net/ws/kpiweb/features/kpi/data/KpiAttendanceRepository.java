package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Locale;
import pe.net.ws.kpiweb.core.session.SessionManager;

/**
 * Turnos KPI recibidos del servidor de Asistencias para la semana y cuenta actuales.
 * Ante falta de datos no presume asistencia.
 */
public class KpiAttendanceRepository {

    private static final String PREF_NAME = "kpi_attendance_db_prefs_v6";
    private static final String KEY_ATTENDANCE_DATA = "attendance_days_json";

    public static final String SHIFT_MORNING = "MANANA";
    public static final String SHIFT_AFTERNOON = "TARDE";

    private static volatile KpiAttendanceRepository instance;
    private final Context prefsContext;
    private final SharedPreferences prefs;
    private final Map<Integer, AttendanceDay> dayMap = new HashMap<>();

    public static class AttendanceDay implements Serializable {
        public int dayIndex; // 0=Lunes, 1=Martes, 2=Miércoles, 3=Jueves, 4=Viernes, 5=Sábado
        public String dayName;
        public boolean morningAttended;
        public String morningHours;
        public String morningCheckIn;
        public String morningCheckOut;

        public boolean afternoonAttended;
        public boolean onApprovedLeave;
        public String afternoonHours;
        public String afternoonCheckIn;
        public String afternoonCheckOut;

        public AttendanceDay(int dayIndex, String dayName,
                             boolean morningAttended, String morningHours, String morningCheckIn, String morningCheckOut,
                             boolean afternoonAttended, String afternoonHours, String afternoonCheckIn, String afternoonCheckOut) {
            this.dayIndex = dayIndex;
            this.dayName = dayName;
            this.morningAttended = morningAttended;
            this.morningHours = morningHours;
            this.morningCheckIn = morningCheckIn;
            this.morningCheckOut = morningCheckOut;
            this.afternoonAttended = afternoonAttended;
            this.afternoonHours = afternoonHours;
            this.afternoonCheckIn = afternoonCheckIn;
            this.afternoonCheckOut = afternoonCheckOut;
        }

        public String getSummaryLabel() {
            if (morningAttended && afternoonAttended) {
                return "Turnos programados: Mañana y Tarde";
            } else if (morningAttended) {
                return "Turno programado: Mañana (" + morningHours + ")";
            } else if (afternoonAttended) {
                return "Turno programado: Tarde (" + afternoonHours + ")";
            } else {
                return "No hay un turno KPI exigible confirmado para este día";
            }
        }
    }

    private KpiAttendanceRepository(Context context) {
        this.prefsContext = context.getApplicationContext();
        this.prefs = prefsContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized KpiAttendanceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new KpiAttendanceRepository(context);
        }
        return instance;
    }

    /** Kept for old callers; never invent a shift. */
    public synchronized void seedDefaultDatabaseAttendance() {
        dayMap.clear();
    }

    public synchronized boolean applyServerCalendar(JSONObject calendar, int workerId) {
        if (calendar == null || workerId <= 0 || workerId != new SessionManager(prefsContext).getWorkerId()
                || !currentMonday().equals(calendar.optString("semana_desde"))) return false;
        JSONArray days = calendar.optJSONArray("dias");
        if (days == null || days.length() != 6) return false;
        Map<Integer, AttendanceDay> updated = new HashMap<>();
        for (int i = 0; i < 6; i++) {
            JSONObject row = days.optJSONObject(i);
            if (row == null || !row.has("manana") || !row.has("tarde")) return false;
            AttendanceDay scheduled = new AttendanceDay(i, row.optString("dia", "Día " + (i + 1)),
                    row.optBoolean("manana"), "Turno mañana", "--:--", "--:--",
                    row.optBoolean("tarde"), "Turno tarde", "--:--", "--:--");
            scheduled.onApprovedLeave = row.optBoolean("permiso_aprobado", false);
            updated.put(i, scheduled);
        }
        dayMap.clear(); dayMap.putAll(updated);
        prefs.edit().putInt("owner_worker",workerId).putString("week_start",currentMonday())
                .putString("synced_date",currentDateLima()).apply();
        saveToPrefs();
        return true;
    }

    public synchronized AttendanceDay getDayAttendance(int dayIndex0To5) {
        if (prefs.getInt("owner_worker",-1) != new SessionManager(prefsContext).getWorkerId()
                || !currentMonday().equals(prefs.getString("week_start",null))
                || !currentDateLima().equals(prefs.getString("synced_date",null))) dayMap.clear();
        if (!dayMap.containsKey(dayIndex0To5)) {
            return new AttendanceDay(dayIndex0To5, "Día " + dayIndex0To5, false, "", "--:--", "--:--", false, "", "--:--", "--:--");
        }
        return dayMap.get(dayIndex0To5);
    }

    public synchronized AttendanceDay getDayAttendance(Calendar cal) {
        if (cal == null) return getDayAttendance(0);
        Calendar selectedMonday = (Calendar) cal.clone();
        selectedMonday.add(Calendar.DAY_OF_MONTH,-((selectedMonday.get(Calendar.DAY_OF_WEEK)+5)%7));
        SimpleDateFormat selectedFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        selectedFormat.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        if (!currentMonday().equals(selectedFormat.format(selectedMonday.getTime())))
            return getDayAttendance(-1);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int idx = 0;
        switch (dow) {
            case Calendar.MONDAY: idx = 0; break;
            case Calendar.TUESDAY: idx = 1; break;
            case Calendar.WEDNESDAY: idx = 2; break;
            case Calendar.THURSDAY: idx = 3; break;
            case Calendar.FRIDAY: idx = 4; break;
            case Calendar.SATURDAY: idx = 5; break;
            case Calendar.SUNDAY: idx = -1; break;
        }
        return getDayAttendance(idx);
    }

    public synchronized boolean hasMorning(int dayIndex) {
        AttendanceDay d = getDayAttendance(dayIndex);
        return d != null && d.morningAttended;
    }

    public synchronized boolean hasAfternoon(int dayIndex) {
        AttendanceDay d = getDayAttendance(dayIndex);
        return d != null && d.afternoonAttended;
    }

    public synchronized boolean hasBothShifts(int dayIndex) {
        AttendanceDay d = getDayAttendance(dayIndex);
        return d != null && d.morningAttended && d.afternoonAttended;
    }

    public synchronized void resetToDefaults() {
        dayMap.clear();
        prefs.edit().remove("owner_worker").remove("week_start").remove("synced_date").apply();
        saveToPrefs();
    }

    private synchronized void saveToPrefs() {
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < 6; i++) {
                AttendanceDay d = dayMap.get(i);
                if (d != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("dayIndex", d.dayIndex);
                    obj.put("dayName", d.dayName);
                    obj.put("morningAttended", d.morningAttended);
                    obj.put("morningHours", d.morningHours);
                    obj.put("morningCheckIn", d.morningCheckIn);
                    obj.put("morningCheckOut", d.morningCheckOut);
                    obj.put("afternoonAttended", d.afternoonAttended);
                    obj.put("onApprovedLeave", d.onApprovedLeave);
                    obj.put("afternoonHours", d.afternoonHours);
                    obj.put("afternoonCheckIn", d.afternoonCheckIn);
                    obj.put("afternoonCheckOut", d.afternoonCheckOut);
                    arr.put(obj);
                }
            }
            prefs.edit().putString(KEY_ATTENDANCE_DATA, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadFromPrefs() {
        if (prefs.getInt("owner_worker",-1) != new SessionManager(prefsContext).getWorkerId()
                || !currentMonday().equals(prefs.getString("week_start",null))
                || !currentDateLima().equals(prefs.getString("synced_date",null))) return;
        String json = prefs.getString(KEY_ATTENDANCE_DATA, null);
        if (json == null) return;
        try {
            JSONArray arr = new JSONArray(json);
            dayMap.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                AttendanceDay d = new AttendanceDay(
                        obj.getInt("dayIndex"),
                        obj.getString("dayName"),
                        obj.getBoolean("morningAttended"),
                        obj.optString("morningHours", "08:00 AM - 01:30 PM"),
                        obj.optString("morningCheckIn", "--:--"),
                        obj.optString("morningCheckOut", "--:--"),
                        obj.getBoolean("afternoonAttended"),
                        obj.optString("afternoonHours", "03:30 PM - 06:30 PM"),
                        obj.optString("afternoonCheckIn", "--:--"),
                        obj.optString("afternoonCheckOut", "--:--")
                );
                d.onApprovedLeave = obj.optBoolean("onApprovedLeave", false);
                dayMap.put(d.dayIndex, d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String currentMonday() {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        now.add(Calendar.DAY_OF_MONTH,-((now.get(Calendar.DAY_OF_WEEK)+5)%7));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return format.format(now.getTime());
    }

    private static String currentDateLima() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return format.format(new java.util.Date());
    }
}

