package pe.net.ws.kpiweb.features.schedule.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Franja horaria. items: índice de día (0=Lun ... 5=Sáb) -> actividad. Vacío = no se muestra ese día. */
public class Slot {

    public enum Kind { ENTRY, EXIT, BREAK, OFF, NORMAL }

    private static final Pattern RANGE =
            Pattern.compile("^\\s*(\\d{1,2}):(\\d{2})\\s*(?:[-–]\\s*(\\d{1,2}):(\\d{2}))?\\s*$");

    public String label;
    public int startMin;
    public int endMin;
    public Kind kind;
    public Map<Integer, String> items = new HashMap<>();

    public Slot() {
        this("", 0, 0, Kind.NORMAL);
    }

    public Slot(String label, int startMin, int endMin, Kind kind) {
        this.label = label;
        this.startMin = startMin;
        this.endMin = endMin;
        this.kind = kind;
    }

    public Slot day(int dayIndex, String text) {
        items.put(dayIndex, text);
        return this;
    }

    public Slot days(String text, int... dayIndexes) {
        for (int d : dayIndexes) {
            items.put(d, text);
        }
        return this;
    }

    public String itemFor(int dayIndex) {
        String v = items.get(dayIndex);
        return v == null ? "" : v;
    }

    public boolean isVisibleOn(int dayIndex) {
        return !itemFor(dayIndex).trim().isEmpty();
    }

    /**
     * Interpreta un texto "8:30 - 9:30" (o solo "13:30") y actualiza etiqueta e intervalo.
     * Devuelve false si el formato no es válido.
     */
    public boolean applyLabel(String text) {
        if (text == null) return false;
        Matcher m = RANGE.matcher(text);
        if (!m.matches()) return false;
        int sh = Integer.parseInt(m.group(1));
        int sm = Integer.parseInt(m.group(2));
        if (sh > 23 || sm > 59) return false;
        int start = sh * 60 + sm;
        int end;
        if (m.group(3) != null) {
            int eh = Integer.parseInt(m.group(3));
            int em = Integer.parseInt(m.group(4));
            if (eh > 23 || em > 59) return false;
            end = eh * 60 + em;
            if (end <= start) return false;
            end = end - 1;
        } else {
            end = Math.min(start + 14, 24 * 60 - 1);
        }
        this.startMin = start;
        this.endMin = end;
        this.label = text.trim().replaceAll("\\s*[-–]\\s*", " - ");
        return true;
    }

    public static String fmt(int min) {
        return String.format(Locale.getDefault(), "%d:%02d", min / 60, min % 60);
    }
}
