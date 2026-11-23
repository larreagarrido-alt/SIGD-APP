package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import pe.net.ws.kpiweb.R;

/** Uses the bundled readable font when the device replaces Android's sans-serif. */
final class AdminTypography {
    private AdminTypography() { }

    static void apply(Context context, View root) {
        Typeface base = ResourcesCompat.getFont(context, R.font.admin_roboto);
        if (base != null) applyToView(root, base);
    }

    private static void applyToView(View view, Typeface base) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            Typeface current = text.getTypeface();
            text.setTypeface(Typeface.create(base, current == null ? Typeface.NORMAL : current.getStyle()));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applyToView(group.getChildAt(i), base);
        }
    }
}
