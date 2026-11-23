package pe.net.ws.kpiweb.features.auth;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Parses only server-provided Retry-After values; never estimates a lockout from local policy. */
final class RetryAfter {
    private RetryAfter() {}
    static long delayMillis(String header, long nowMillis) {
        if (header == null) return -1;
        String value = header.trim();
        if (value.matches("[0-9]{1,10}")) {
            try {return Math.multiplyExact(Long.parseLong(value), 1000L);}
            catch (ArithmeticException | NumberFormatException invalid) {return -1;}
        }
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        ParsePosition position = new ParsePosition(0);
        Date date = format.parse(value, position);
        if (date == null || position.getIndex() != value.length()) return -1;
        return Math.max(0, date.getTime() - nowMillis);
    }
}
