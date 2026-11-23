package pe.net.ws.kpiweb.features.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.about.CollaboratorAboutActivity;
import pe.net.ws.kpiweb.features.collaborator.about.SigdCreditsHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.privacy.CollaboratorPrivacyActivity;
import pe.net.ws.kpiweb.features.settings.data.SettingsPreferences;

/**
 * Actividad principal de Configuración General (WEBSITE).
 * Soporta cambio real de apariencia (Modo claro, Modo oscuro y Utilizar configuración del sistema)
 * conservando la identidad morada, control funcional de recordatorios de KPI y avisos,
 * y enlaces directos a Privacidad y permisos y Acerca de WEBSITE.
 */
public class SettingsActivity extends AppCompatActivity {

    private SettingsPreferences prefs;

    private View optThemeDark;
    private View optThemeLight;
    private View optThemeSystem;
    private ImageView checkDark;
    private ImageView checkLight;
    private ImageView checkSystem;

    private SwitchMaterial switchMaster;
    private SwitchMaterial switchReminders;
    private SwitchMaterial switchKpi;
    private SwitchMaterial switchNews;
    private SwitchMaterial switchAlerts;
    private SwitchMaterial switchReduceAnim;
    private SwitchMaterial switchHighContrast;

    private TextView tvCurrentLanguage;
    private TextView tvTextScaleValue;
    private TextView tvVersion;
    private boolean isBindingViews = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        if ("COLABORADOR".equals(getIntent().getStringExtra(
                pe.net.ws.kpiweb.features.account.AccountActivity.EXTRA_USER_ROLE))) {
            setTheme(R.style.Theme_KPIWeb_Collaborator);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity_main);

        prefs = SettingsPreferences.getInstance(this);
        CollaboratorThemeHelper.setupSystemBars(this);

        initViews();
        findViewById(R.id.settings_security_devices).setOnClickListener(v ->
                startActivity(new Intent(this, SecurityDevicesActivity.class)));
        loadSavedPreferences();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void initViews() {
        View btnBack = findViewById(R.id.settings_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        optThemeDark = findViewById(R.id.settings_opt_theme_dark);
        optThemeLight = findViewById(R.id.settings_opt_theme_light);
        optThemeSystem = findViewById(R.id.settings_opt_theme_system);
        checkDark = findViewById(R.id.settings_ic_check_dark);
        checkLight = findViewById(R.id.settings_ic_check_light);
        checkSystem = findViewById(R.id.settings_ic_check_system);

        switchMaster = findViewById(R.id.settings_switch_notif_master);
        switchReminders = findViewById(R.id.settings_switch_notif_reminders);
        switchKpi = findViewById(R.id.settings_switch_notif_kpi);
        switchNews = findViewById(R.id.settings_switch_notif_news);
        switchAlerts = findViewById(R.id.settings_switch_notif_alerts);

        switchReduceAnim = findViewById(R.id.settings_switch_reduce_anim);
        switchHighContrast = findViewById(R.id.settings_switch_high_contrast);

        tvCurrentLanguage = findViewById(R.id.settings_tv_current_language);
        tvTextScaleValue = findViewById(R.id.settings_tv_text_scale_value);
        tvVersion = findViewById(R.id.settings_tv_version);

        populateDynamicAppVersion();
    }

    private void populateDynamicAppVersion() {
        if (tvVersion == null) return;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName != null ? pInfo.versionName : "";
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? pInfo.getLongVersionCode()
                    : pInfo.versionCode;
            tvVersion.setText("Versión " + SigdCreditsHelper.formatInstalledVersion(versionName, versionCode));
        } catch (Exception e) {
            tvVersion.setText("Versión " + SigdCreditsHelper.formatInstalledVersion("", 0L));
        }
    }

    private void loadSavedPreferences() {
        isBindingViews = true;

        updateThemeSelectionUI(prefs.getThemeMode());

        boolean masterEnabled = prefs.isMasterNotifEnabled();
        if (switchMaster != null) switchMaster.setChecked(masterEnabled);
        if (switchReminders != null) switchReminders.setChecked(prefs.isRemindersEnabled());
        if (switchKpi != null) switchKpi.setChecked(prefs.isKpiNotifEnabled());
        if (switchNews != null) switchNews.setChecked(prefs.isNewsEnabled());
        if (switchAlerts != null) switchAlerts.setChecked(prefs.isAlertsEnabled());
        updateChildSwitchesEnabledState(masterEnabled);

        if (switchReduceAnim != null) switchReduceAnim.setChecked(prefs.isReduceAnimations());
        if (switchHighContrast != null) switchHighContrast.setChecked(prefs.isHighContrast());

        updateLanguageDisplay(prefs.getLanguageCode());
        updateTextScaleDisplay(prefs.getTextScale());

        isBindingViews = false;
    }

    private void updateThemeSelectionUI(String selectedMode) {
        if (checkDark != null) {
            checkDark.setVisibility(SettingsPreferences.THEME_DARK.equalsIgnoreCase(selectedMode)
                    ? View.VISIBLE : View.GONE);
        }
        if (checkLight != null) {
            checkLight.setVisibility(SettingsPreferences.THEME_LIGHT.equalsIgnoreCase(selectedMode)
                    ? View.VISIBLE : View.GONE);
        }
        if (checkSystem != null) {
            checkSystem.setVisibility(SettingsPreferences.THEME_SYSTEM.equalsIgnoreCase(selectedMode)
                    ? View.VISIBLE : View.GONE);
        }
    }

    private void updateChildSwitchesEnabledState(boolean masterEnabled) {
        float alpha = masterEnabled ? 1.0f : 0.45f;
        setSwitchState(switchReminders, masterEnabled, alpha);
        setSwitchState(switchKpi, masterEnabled, alpha);
        setSwitchState(switchNews, masterEnabled, alpha);
        setSwitchState(switchAlerts, masterEnabled, alpha);
    }

    private void setSwitchState(SwitchMaterial sw, boolean enabled, float alpha) {
        if (sw != null) {
            sw.setEnabled(enabled);
            sw.setAlpha(alpha);
        }
    }

    private void setupListeners() {
        if (optThemeDark != null) {
            optThemeDark.setOnClickListener(v -> selectThemeMode(SettingsPreferences.THEME_DARK));
        }
        if (optThemeLight != null) {
            optThemeLight.setOnClickListener(v -> selectThemeMode(SettingsPreferences.THEME_LIGHT));
        }
        if (optThemeSystem != null) {
            optThemeSystem.setOnClickListener(v -> selectThemeMode(SettingsPreferences.THEME_SYSTEM));
        }

        if (switchMaster != null) {
            switchMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setMasterNotifEnabled(isChecked);
                updateChildSwitchesEnabledState(isChecked);
                CollaboratorBannerHelper.showBanner(
                        this,
                        isChecked ? "Notificaciones generales activadas" : "Notificaciones generales silenciadas",
                        "Preferencia guardada localmente en WEBSITE.",
                        R.drawable.collab_ic_nav_bell
                );
            });
        }

        if (switchReminders != null) {
            switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setRemindersEnabled(isChecked);
                CollaboratorBannerHelper.showBanner(
                        this,
                        isChecked ? "Recordatorios de actividades activados" : "Recordatorios de actividades desactivados",
                        "Tus avisos locales se ajustaron a esta preferencia.",
                        R.drawable.collaborator_ic_clock
                );
            });
        }

        if (switchKpi != null) {
            switchKpi.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setKpiNotifEnabled(isChecked);
                CollaboratorBannerHelper.showBanner(
                        this,
                        isChecked ? "Recordatorios de KPI activados" : "Recordatorios de KPI desactivados",
                        "Preferencia de alertas de indicadores actualizada.",
                        R.drawable.collab_ic_nav_kpi
                );
            });
        }

        if (switchNews != null) {
            switchNews.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setNewsEnabled(isChecked);
                CollaboratorBannerHelper.showBanner(
                        this,
                        isChecked ? "Avisos generales activados" : "Avisos generales desactivados",
                        "Recibirás comunicados de WEBSITE según esta opción.",
                        R.drawable.collaborator_ic_star
                );
            });
        }

        if (switchAlerts != null) {
            switchAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setAlertsEnabled(isChecked);
                CollaboratorBannerHelper.showBanner(
                        this,
                        "Preferencia de alertas actualizada",
                        "Se guardó la configuración de alertas importantes.",
                        R.drawable.collab_ic_nav_bell
                );
            });
        }

        if (switchReduceAnim != null) {
            switchReduceAnim.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setReduceAnimations(isChecked);
            });
        }

        if (switchHighContrast != null) {
            switchHighContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isBindingViews) return;
                prefs.setHighContrast(isChecked);
            });
        }

        View rowLanguage = findViewById(R.id.settings_row_language);
        if (rowLanguage != null) {
            rowLanguage.setOnClickListener(v -> showLanguagePickerDialog());
        }

        View rowTextScale = findViewById(R.id.settings_row_text_scale);
        if (rowTextScale != null) {
            rowTextScale.setOnClickListener(v -> showTextScalePickerDialog());
        }

        // Enlaces a Privacidad y permisos
        Runnable openPrivacy = () -> startActivity(new Intent(this, CollaboratorPrivacyActivity.class));
        View rowPrivacyData = findViewById(R.id.settings_row_privacy_data);
        if (rowPrivacyData != null) rowPrivacyData.setOnClickListener(v -> openPrivacy.run());
        View rowPermissions = findViewById(R.id.settings_row_permissions);
        if (rowPermissions != null) rowPermissions.setOnClickListener(v -> openPrivacy.run());
        View rowPrivacyPolicy = findViewById(R.id.settings_row_privacy_policy);
        if (rowPrivacyPolicy != null) rowPrivacyPolicy.setOnClickListener(v -> openPrivacy.run());

        // Enlace a Acerca de WEBSITE
        if (tvVersion != null && tvVersion.getParent() instanceof View) {
            ((View) tvVersion.getParent()).setOnClickListener(v ->
                    startActivity(new Intent(this, CollaboratorAboutActivity.class)));
        }
    }

    private void selectThemeMode(String mode) {
        if (mode.equalsIgnoreCase(prefs.getThemeMode())) return;
        updateThemeSelectionUI(mode);
        prefs.setThemeMode(mode);
    }

    private void showLanguagePickerDialog() {
        String[] languages = new String[]{"Español (Predeterminado)", "English", "Português"};
        String[] codes = new String[]{
                pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.LANG_ES,
                pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.LANG_EN,
                pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.LANG_PT
        };
        String currentCode = pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.getPersistedLanguage(this);
        int selectedIndex = 0;
        if (pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.LANG_EN.equalsIgnoreCase(currentCode)) selectedIndex = 1;
        else if (pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.LANG_PT.equalsIgnoreCase(currentCode)) selectedIndex = 2;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Idioma de la aplicación")
                .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                    String chosenCode = codes[which];
                    dialog.dismiss();
                    pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.setLocale(SettingsActivity.this, chosenCode);
                    updateLanguageDisplay(chosenCode);
                    CollaboratorBannerHelper.showBanner(
                            SettingsActivity.this,
                            "Idioma actualizado",
                            "Idioma seleccionado: " + languages[which],
                            R.drawable.collab_ic_website_logo
                    );
                    recreate();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateLanguageDisplay(String code) {
        if (tvCurrentLanguage == null) return;
        tvCurrentLanguage.setText(pe.net.ws.kpiweb.features.collaborator.common.LocaleHelper.getLanguageDisplayName(code));
    }

    private void showTextScalePickerDialog() {
        String current = prefs.getTextScale();
        final String nextValue;
        final String nextLabel;
        if (SettingsPreferences.SCALE_NORMAL.equalsIgnoreCase(current)) {
            nextValue = SettingsPreferences.SCALE_MEDIUM;
            nextLabel = "Mediano";
        } else if (SettingsPreferences.SCALE_MEDIUM.equalsIgnoreCase(current)) {
            nextValue = SettingsPreferences.SCALE_LARGE;
            nextLabel = "Grande";
        } else {
            nextValue = SettingsPreferences.SCALE_NORMAL;
            nextLabel = "Normal (Predeterminado)";
        }

        CollaboratorBannerHelper.showPremiumModal(
                this,
                "ACCESIBILIDAD • WEBSITE",
                "Escala de texto de la interfaz",
                "¿Deseas cambiar el tamaño de texto de visualización a '" + nextLabel + "'?",
                "Aplicar " + nextLabel,
                () -> {
                    prefs.setTextScale(nextValue);
                    updateTextScaleDisplay(nextValue);
                    CollaboratorBannerHelper.showBanner(
                            this,
                            "Escala de texto actualizada",
                            "Tamaño seleccionado: " + nextLabel,
                            R.drawable.collab_ic_website_logo
                    );
                },
                "Cancelar"
        );
    }

    private void updateTextScaleDisplay(String scale) {
        if (tvTextScaleValue == null) return;
        if (SettingsPreferences.SCALE_MEDIUM.equalsIgnoreCase(scale)) {
            tvTextScaleValue.setText("Mediano");
        } else if (SettingsPreferences.SCALE_LARGE.equalsIgnoreCase(scale)) {
            tvTextScaleValue.setText("Grande");
        } else {
            tvTextScaleValue.setText("Normal (Predeterminado)");
        }
    }
}
