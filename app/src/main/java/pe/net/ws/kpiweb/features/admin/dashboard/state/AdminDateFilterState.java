package pe.net.ws.kpiweb.features.admin.dashboard.state;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Estado centralizado de filtros y navegación por fechas.
 * Maneja los modos Día, Semana, Mes y Personalizado con cálculo dinámico en hora local.
 */
public class AdminDateFilterState {

    public interface OnFilterChangedListener {
        void onFilterChanged(AdminDateFilterState state);
    }

    private AdminFilterMode mode;
    private final Calendar referenceCalendar;
    private Calendar customStartCalendar;
    private Calendar customEndCalendar;
    private OnFilterChangedListener listener;

    private final Locale localeEs = new Locale("es", "ES");

    public AdminDateFilterState() {
        this.mode = AdminFilterMode.DIA;
        this.referenceCalendar = Calendar.getInstance();
        this.referenceCalendar.setFirstDayOfWeek(Calendar.MONDAY);
    }

    public void setOnFilterChangedListener(OnFilterChangedListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onFilterChanged(this);
        }
    }

    public AdminFilterMode getMode() {
        return mode;
    }

    public void setMode(AdminFilterMode newMode) {
        if (this.mode != newMode) {
            this.mode = newMode;
            notifyListener();
        }
    }

    public boolean isNavigationArrowsEnabled() {
        return mode != AdminFilterMode.PERSONALIZADO;
    }

    public void next() {
        switch (mode) {
            case DIA:
                referenceCalendar.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case SEMANA:
                referenceCalendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MES:
                referenceCalendar.add(Calendar.MONTH, 1);
                break;
            case PERSONALIZADO:
                // Flechas deshabilitadas para rango personalizado para evitar ambigüedades
                return;
        }
        notifyListener();
    }

    public void previous() {
        switch (mode) {
            case DIA:
                referenceCalendar.add(Calendar.DAY_OF_MONTH, -1);
                break;
            case SEMANA:
                referenceCalendar.add(Calendar.WEEK_OF_YEAR, -1);
                break;
            case MES:
                referenceCalendar.add(Calendar.MONTH, -1);
                break;
            case PERSONALIZADO:
                return;
        }
        notifyListener();
    }

    public void applyToday() {
        this.mode = AdminFilterMode.DIA;
        this.referenceCalendar.setTime(new Date());
        notifyListener();
    }

    public void applyYesterday() {
        this.mode = AdminFilterMode.DIA;
        this.referenceCalendar.setTime(new Date());
        this.referenceCalendar.add(Calendar.DAY_OF_MONTH, -1);
        notifyListener();
    }

    public void applyThisWeek() {
        this.mode = AdminFilterMode.SEMANA;
        this.referenceCalendar.setTime(new Date());
        notifyListener();
    }

    public void applyLastWeek() {
        this.mode = AdminFilterMode.SEMANA;
        this.referenceCalendar.setTime(new Date());
        this.referenceCalendar.add(Calendar.WEEK_OF_YEAR, -1);
        notifyListener();
    }

    public void applyThisMonth() {
        this.mode = AdminFilterMode.MES;
        this.referenceCalendar.setTime(new Date());
        notifyListener();
    }

    public void applyLastMonth() {
        this.mode = AdminFilterMode.MES;
        this.referenceCalendar.setTime(new Date());
        this.referenceCalendar.add(Calendar.MONTH, -1);
        notifyListener();
    }

    public void applySingleDate(int year, int month, int dayOfMonth) {
        this.mode = AdminFilterMode.DIA;
        this.referenceCalendar.set(Calendar.YEAR, year);
        this.referenceCalendar.set(Calendar.MONTH, month);
        this.referenceCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        notifyListener();
    }

    public void applyWeekDate(int year, int month, int dayOfMonth) {
        this.mode = AdminFilterMode.SEMANA;
        this.referenceCalendar.set(year, month, dayOfMonth);
        notifyListener();
    }

    public void applyCustomRange(Calendar start, Calendar end) {
        this.mode = AdminFilterMode.PERSONALIZADO;
        this.customStartCalendar = (Calendar) start.clone();
        this.customEndCalendar = (Calendar) end.clone();
        notifyListener();
    }

    public Calendar getStartDate() {
        Calendar start = Calendar.getInstance();
        start.setFirstDayOfWeek(Calendar.MONDAY);
        switch (mode) {
            case DIA:
                start.setTime(referenceCalendar.getTime());
                resetToStartOfDay(start);
                return start;
            case SEMANA:
                start.setTime(referenceCalendar.getTime());
                start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                resetToStartOfDay(start);
                return start;
            case MES:
                start.setTime(referenceCalendar.getTime());
                start.set(Calendar.DAY_OF_MONTH, 1);
                resetToStartOfDay(start);
                return start;
            case PERSONALIZADO:
                if (customStartCalendar != null) {
                    start.setTime(customStartCalendar.getTime());
                }
                resetToStartOfDay(start);
                return start;
        }
        return start;
    }

    public Calendar getEndDate() {
        Calendar end = Calendar.getInstance();
        end.setFirstDayOfWeek(Calendar.MONDAY);
        switch (mode) {
            case DIA:
                end.setTime(referenceCalendar.getTime());
                resetToEndOfDay(end);
                return end;
            case SEMANA:
                Calendar s = getStartDate();
                end.setTime(s.getTime());
                end.add(Calendar.DAY_OF_MONTH, 6);
                resetToEndOfDay(end);
                return end;
            case MES:
                end.setTime(referenceCalendar.getTime());
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                resetToEndOfDay(end);
                return end;
            case PERSONALIZADO:
                if (customEndCalendar != null) {
                    end.setTime(customEndCalendar.getTime());
                }
                resetToEndOfDay(end);
                return end;
        }
        return end;
    }

    public String getFormattedDisplay() {
        switch (mode) {
            case DIA: {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, d 'de' MMMM", localeEs);
                String formatted = sdf.format(referenceCalendar.getTime());
                return capitalizeFirst(formatted);
            }
            case SEMANA: {
                Calendar start = getStartDate();
                Calendar end = getEndDate();
                SimpleDateFormat sdfDayMonth = new SimpleDateFormat("d MMM", localeEs);
                SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", localeEs);
                return sdfDayMonth.format(start.getTime()) + " - " + sdfDayMonth.format(end.getTime()) + " " + sdfYear.format(end.getTime());
            }
            case MES: {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", localeEs);
                return capitalizeFirst(sdf.format(referenceCalendar.getTime()));
            }
            case PERSONALIZADO: {
                Calendar start = getStartDate();
                Calendar end = getEndDate();
                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy", localeEs);
                return sdf.format(start.getTime()) + " - " + sdf.format(end.getTime());
            }
            default:
                return "";
        }
    }

    public Calendar getReferenceCalendar() {
        return (Calendar) referenceCalendar.clone();
    }

    private void resetToStartOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void resetToEndOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
