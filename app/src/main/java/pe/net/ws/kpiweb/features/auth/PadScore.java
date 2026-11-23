package pe.net.ws.kpiweb.features.auth;

/** Result from the two active MiniFAS face presentation checks. */
final class PadScore {
    final double real;
    final double spoof;
    final double millis;

    PadScore(double real, double spoof, double millis) {
        this.real = real;
        this.spoof = spoof;
        this.millis = millis;
    }
}
