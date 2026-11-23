package pe.net.ws.kpiweb.features.auth;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import java.util.concurrent.TimeUnit;

/** Stage durations only. Never include accounts, images, descriptors, or credentials. */
public final class FaceTiming {
    static final String TAG = "FaceTiming";
    private FaceTiming() {}
    public static long now() { return System.nanoTime(); }
    static void event(Context context, String stage) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            Log.d(TAG, stage);
    }
    public static void log(Context context, String stage, long started) {
        if (started==0 || (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return;
        Log.d(TAG, stage + "_ms=" + TimeUnit.NANOSECONDS.toMillis(now() - started));
    }
    static void logMillis(Context context,String stage,double millis) {
        if(!Double.isFinite(millis) || millis<0
                || (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
        Log.d(TAG,stage+"_ms="+Math.round(millis));
    }
}
