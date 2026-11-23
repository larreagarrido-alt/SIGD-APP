package pe.net.ws.kpiweb.features.admin.personal;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Keeps Personal readable on devices that replace the default system font. */
public final class AdminPersonalTypography {
    private static Typeface base;

    private AdminPersonalTypography() {}

    public static void apply(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            Typeface current = text.getTypeface();
            text.setTypeface(readable(current == null ? Typeface.NORMAL : current.getStyle()));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i));
        }
    }

    public static synchronized Typeface readable(int style) {
        if (base == null) {
            for (String path : new String[]{"/system/fonts/Roboto-Regular.ttf", "/system/fonts/RobotoStatic-Regular.ttf"}) {
                try {
                    base = Typeface.createFromFile(path);
                    break;
                } catch (RuntimeException ignored) {
                    // Use the next available Android font.
                }
            }
            if (base == null) base = Typeface.create("sans-serif", Typeface.NORMAL);
        }
        return Typeface.create(base, style);
    }
}
