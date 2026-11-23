package pe.net.ws.kpiweb.features.collaborator.about;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import pe.net.ws.kpiweb.R;

/**
 * Fuente única de identidad institucional, versión real y créditos compartidos
 * entre Administrador y Colaborador para SIGD by website.
 */
public final class SigdCreditsHelper {

    public static final String APP_NAME = "SIGD";
    public static final String APP_DESCRIPTIVE_NAME = "Sistema Integral de Gestión de Desempeño";
    public static final String DEVELOPER_NAME = "Website";
    public static final String COPYRIGHT_YEAR = "2026";
    public static final String CREDIT_SIGNATURE = "2026-jadiel";
    public static final String DEVELOPED_BY_LABEL = "Desarrollado por Website";
    public static final String COPYRIGHT_LINE = "© 2026 WEBSITE · Todos los derechos reservados";
    public static final String FOOTER_META_LINE = "2026-jadiel";

    private SigdCreditsHelper() { }

    /**
     * Formatea la versión real instalada desde los metadatos del paquete Android.
     * Nunca usa firmas de créditos ni textos de prueba como número de versión.
     */
    public static String formatInstalledVersion(String versionName, long versionCode) {
        String cleanName = versionName == null ? "" : versionName.trim();
        if (cleanName.isEmpty() && versionCode <= 0) {
            return "No disponible";
        }
        if (cleanName.isEmpty()) {
            return "Build " + versionCode;
        }
        if (versionCode > 0) {
            return cleanName + " (Build " + versionCode + ")";
        }
        return cleanName;
    }

    /**
     * Aplica la misma identidad de créditos en el footer compartido debajo de Cerrar sesión
     * tanto en el menú de Administrador como en el menú de Colaborador, adaptado a tema claro y oscuro.
     */
    public static void bindMenuFooter(Context context, View root, boolean alwaysDarkSurface) {
        if (context == null || root == null) return;
        ImageView ivLogo = root.findViewById(R.id.sigd_credits_footer_logo);
        TextView tvDev = root.findViewById(R.id.sigd_credits_footer_dev);
        TextView tvMeta = root.findViewById(R.id.sigd_credits_footer_meta);

        boolean darkSurface = alwaysDarkSurface || isNightMode(context);
        if (ivLogo != null) {
            ivLogo.setImageResource(darkSurface
                    ? R.drawable.collab_logo_website_white
                    : R.drawable.collab_logo_website_purple);
        }
        if (tvDev != null) {
            tvDev.setText(DEVELOPED_BY_LABEL);
            tvDev.setTextColor(darkSurface ? Color.parseColor("#BCA6DA") : Color.parseColor("#665575"));
        }
        if (tvMeta != null) {
            tvMeta.setText(FOOTER_META_LINE);
            tvMeta.setTextColor(darkSurface ? Color.parseColor("#8E78AE") : Color.parseColor("#7E6B92"));
        }
    }

    public static boolean isNightMode(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }
}
