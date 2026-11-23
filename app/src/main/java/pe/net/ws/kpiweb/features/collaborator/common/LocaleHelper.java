package pe.net.ws.kpiweb.features.collaborator.common;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

import pe.net.ws.kpiweb.features.settings.data.SettingsPreferences;

/**
 * Gestor oficial de internacionalización e idioma para WEBSITE.
 * Soporta Español (es), English (en) y Português (pt), persistiendo
 * la selección y actualizando el contexto de recursos de la aplicación.
 */
public final class LocaleHelper {

    public static final String LANG_ES = "es";
    public static final String LANG_EN = "en";
    public static final String LANG_PT = "pt";

    private LocaleHelper() {}

    public static Context onAttach(Context context) {
        String lang = getPersistedLanguage(context);
        return setLocale(context, lang);
    }

    public static String getPersistedLanguage(Context context) {
        SettingsPreferences prefs = SettingsPreferences.getInstance(context);
        String lang = prefs.getLanguageCode();
        return (lang != null && !lang.isEmpty()) ? lang : LANG_ES;
    }

    public static Context setLocale(Context context, String languageCode) {
        String cleanCode = languageCode;
        if (!LANG_ES.equals(cleanCode) && !LANG_EN.equals(cleanCode) && !LANG_PT.equals(cleanCode)) {
            cleanCode = LANG_ES;
        }

        SettingsPreferences.getInstance(context).setLanguageCode(cleanCode);

        Locale locale = new Locale(cleanCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(locale));
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }

    public static String getLanguageDisplayName(String code) {
        if (LANG_EN.equalsIgnoreCase(code)) {
            return "English (US)";
        } else if (LANG_PT.equalsIgnoreCase(code)) {
            return "Português (Brasil)";
        } else {
            return "Español (Predeterminado)";
        }
    }
}

