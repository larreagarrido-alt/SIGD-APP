package pe.net.ws.kpiweb.features.collaborator.common;

import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.settings.data.SettingsPreferences;

/**
 * Utilidad centralizada para aplicar el tema de WEBSITE (Modo Claro, Modo Oscuro o Sistema)
 * y sincronizar el color y contraste de las barras del sistema en la experiencia del Colaborador.
 */
public final class CollaboratorThemeHelper {

    private static Typeface readableFont;

    private CollaboratorThemeHelper() {
    }

    /**
     * Aplica la preferencia de tema persistida antes o durante la creación de la Activity.
     */
    public static void applySavedTheme(AppCompatActivity activity) {
        SettingsPreferences.getInstance(activity).applyCurrentTheme();
    }

    /**
     * Sincroniza los colores de la barra de estado y barra de navegación con la paleta activa
     * (Modo Claro o Modo Oscuro) garantizando legibilidad de los iconos del sistema.
     */
    public static void setupSystemBars(AppCompatActivity activity) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        int mainBgColor = activity.getColor(R.color.collab_bg_main);
        int bottomNavColor = activity.getColor(R.color.collab_bg_bottom_nav);

        window.setStatusBarColor(mainBgColor);
        window.setNavigationBarColor(bottomNavColor);

        int nightModeFlags = activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        boolean isLightMode = (nightModeFlags != Configuration.UI_MODE_NIGHT_YES);

        View decorView = window.getDecorView();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, decorView);
        controller.setAppearanceLightStatusBars(isLightMode);
        controller.setAppearanceLightNavigationBars(isLightMode);

        String activityName = activity.getClass().getName();
        boolean collaboratorScreen = activityName.contains(".features.collaborator.")
                && !activityName.contains(".features.collaborator.kpi.");
        boolean collaboratorSharedScreen = "COLABORADOR".equals(activity.getIntent()
                .getStringExtra("extra_user_role"));
        if (collaboratorScreen || collaboratorSharedScreen) {
            applyReadableTypeface(activity.findViewById(android.R.id.content));
        }
    }

    public static void applyReadableTypeface(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            Typeface current = textView.getTypeface();
            int style = current == null ? Typeface.NORMAL : current.getStyle();
            textView.setTypeface(readableTypeface(style));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyReadableTypeface(group.getChildAt(i));
            }
        }
    }

    private static synchronized Typeface getReadableFont() {
        if (readableFont != null) return readableFont;
        for (String path : new String[]{"/system/fonts/Roboto-Regular.ttf", "/system/fonts/RobotoStatic-Regular.ttf"}) {
            try {
                readableFont = Typeface.createFromFile(path);
                return readableFont;
            } catch (RuntimeException ignored) {
                // Continue with the next installed Android font.
            }
        }
        readableFont = Typeface.create("sans-serif", Typeface.NORMAL);
        return readableFont;
    }

    public static Typeface readableTypeface(int style) {
        return Typeface.create(getReadableFont(), style);
    }
}
