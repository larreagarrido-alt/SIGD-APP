package pe.net.ws.kpiweb.features.settings.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Gestor persistente de preferencias de usuario de WEBSITE.
 * Almacena y aplica de forma segura las configuraciones de:
 * - Apariencia (Modo oscuro por defecto, claro, seguir sistema)
 * - Notificaciones (general, recordatorios, avisos KPI, novedades, alertas)
 * - Idioma (Español / Inglés)
 * - Accesibilidad (Escala de texto, animaciones, contraste)
 */
public class SettingsPreferences {

    private static final String PREF_NAME = "pe.net.ws.kpiweb.settings_prefs";

    // Keys
    public static final String KEY_THEME = "pref_theme_mode";
    public static final String KEY_NOTIF_MASTER = "pref_notif_master";
    public static final String KEY_NOTIF_REMINDERS = "pref_notif_reminders";
    public static final String KEY_NOTIF_KPI = "pref_notif_kpi";
    public static final String KEY_NOTIF_NEWS = "pref_notif_news";
    public static final String KEY_NOTIF_ALERTS = "pref_notif_alerts";
    public static final String KEY_LANGUAGE = "pref_language_code";
    public static final String KEY_TEXT_SCALE = "pref_text_scale";
    public static final String KEY_REDUCE_ANIM = "pref_reduce_anim";
    public static final String KEY_HIGH_CONTRAST = "pref_high_contrast";

    // Values
    public static final String THEME_DARK = "DARK";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_SYSTEM = "SYSTEM";

    public static final String SCALE_NORMAL = "NORMAL";
    public static final String SCALE_MEDIUM = "MEDIUM";
    public static final String SCALE_LARGE = "LARGE";

    private static SettingsPreferences instance;
    private final SharedPreferences prefs;

    private SettingsPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsPreferences(context);
        }
        return instance;
    }

    // --- APARIENCIA ---
    public String getThemeMode() {
        return prefs.getString(KEY_THEME, THEME_DARK);
    }

    public void setThemeMode(String themeMode) {
        prefs.edit().putString(KEY_THEME, themeMode).apply();
        applyTheme(themeMode);
    }

    public void applyCurrentTheme() {
        applyTheme(getThemeMode());
    }

    public static void applyTheme(String themeMode) {
        int targetMode;
        if (THEME_LIGHT.equalsIgnoreCase(themeMode)) {
            targetMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (THEME_SYSTEM.equalsIgnoreCase(themeMode)) {
            targetMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else {
            targetMode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    // --- NOTIFICACIONES ---
    public boolean isMasterNotifEnabled() {
        return prefs.getBoolean(KEY_NOTIF_MASTER, true);
    }

    public void setMasterNotifEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_MASTER, enabled).apply();
    }

    public boolean isRemindersEnabled() {
        return prefs.getBoolean(KEY_NOTIF_REMINDERS, true);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_REMINDERS, enabled).apply();
    }

    public boolean isKpiNotifEnabled() {
        return prefs.getBoolean(KEY_NOTIF_KPI, true);
    }

    public void setKpiNotifEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_KPI, enabled).apply();
    }

    public boolean isNewsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_NEWS, true);
    }

    public void setNewsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_NEWS, enabled).apply();
    }

    public boolean isAlertsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_ALERTS, true);
    }

    public void setAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_ALERTS, enabled).apply();
    }

    // --- IDIOMA ---
    public String getLanguageCode() {
        return prefs.getString(KEY_LANGUAGE, "es");
    }

    public void setLanguageCode(String code) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply();
    }

    // --- ACCESIBILIDAD ---
    public String getTextScale() {
        return prefs.getString(KEY_TEXT_SCALE, SCALE_NORMAL);
    }

    public void setTextScale(String scale) {
        prefs.edit().putString(KEY_TEXT_SCALE, scale).apply();
    }

    public boolean isReduceAnimations() {
        return prefs.getBoolean(KEY_REDUCE_ANIM, false);
    }

    public void setReduceAnimations(boolean reduce) {
        prefs.edit().putBoolean(KEY_REDUCE_ANIM, reduce).apply();
    }

    public boolean isHighContrast() {
        return prefs.getBoolean(KEY_HIGH_CONTRAST, false);
    }

    public void setHighContrast(boolean highContrast) {
        prefs.edit().putBoolean(KEY_HIGH_CONTRAST, highContrast).apply();
    }
}
