package pe.net.ws.kpiweb.features.help;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.account.AccountActivity;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsActivity;
import pe.net.ws.kpiweb.features.settings.SettingsActivity;

/**
 * Actividad de Ayuda y Soporte orientada al Colaborador (WEBSITE).
 * Ofrece preguntas frecuentes, instrucciones de uso, ayuda para Mis KPI, Notificaciones
 * y Configuración, además de un espacio de contacto con
 * "Correo de soporte por definir".
 */
public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_activity_main);

        CollaboratorThemeHelper.setupSystemBars(this);
        setupHeaderActions();
        setupFaqAccordion();
        setupGuidesActions();
        setupSearchFilter();
        setupSupportContactActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
    }

    private void setupHeaderActions() {
        View btnBack = findViewById(R.id.help_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupFaqAccordion() {
        bindFaqToggle(R.id.help_faq_header_1, R.id.help_faq_body_1, R.id.help_faq_icon_1);
        bindFaqToggle(R.id.help_faq_header_2, R.id.help_faq_body_2, R.id.help_faq_icon_2);
        bindFaqToggle(R.id.help_faq_header_3, R.id.help_faq_body_3, R.id.help_faq_icon_3);
        bindFaqToggle(R.id.help_faq_header_4, R.id.help_faq_body_4, R.id.help_faq_icon_4);
    }

    private void bindFaqToggle(int headerId, int bodyId, int iconId) {
        View header = findViewById(headerId);
        View body = findViewById(bodyId);
        ImageView icon = findViewById(iconId);

        if (header != null && body != null) {
            header.setOnClickListener(v -> {
                boolean isVisible = body.getVisibility() == View.VISIBLE;
                body.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                if (icon != null) {
                    icon.animate().rotation(isVisible ? 0f : 180f).setDuration(180).start();
                }
            });
        }
    }

    private void setupGuidesActions() {
        // Guía 1: Mis KPI
        View guide1 = findViewById(R.id.help_row_guide_1);
        if (guide1 != null) {
            guide1.setOnClickListener(v -> CollaboratorBannerHelper.showPremiumModal(
                    this,
                    "GUÍA DE USO • WEBSITE",
                    getString(R.string.help_guide_title1),
                    "1. Toca el botón central 'KPI' en la barra de navegación inferior.\n\n"
                            + "2. Revisa el contador de KPI asignados, completados y pendientes.\n\n"
                            + "3. Verifica la fecha de corte de tu próxima entrega para priorizar tus actividades del período.",
                    "Ir a Mis KPI",
                    () -> CollaboratorKpiIntegrationActivity.openKpiSection(this),
                    "Cerrar"
            ));
        }

        // Guía 2: Notificaciones
        View guide2 = findViewById(R.id.help_row_guide_2);
        if (guide2 != null) {
            guide2.setOnClickListener(v -> CollaboratorBannerHelper.showPremiumModal(
                    this,
                    "GUÍA DE USO • WEBSITE",
                    getString(R.string.help_guide_title2),
                    "1. Ingresa a 'Notificaciones' desde la barra inferior.\n\n"
                            + "2. Filtra tus avisos por categoría: Todas, KPI, Actividades, Evaluación o General.\n\n"
                            + "3. Pulsa 'Probar alerta' para verificar los recordatorios locales en tu dispositivo o toca un aviso para ir al módulo relacionado.",
                    "Ir a Notificaciones",
                    () -> startActivity(new Intent(this, CollaboratorNotificationsActivity.class)),
                    "Cerrar"
            ));
        }

        // Guía 3: Configuración
        View guide3 = findViewById(R.id.help_row_guide_3);
        if (guide3 != null) {
            guide3.setOnClickListener(v -> CollaboratorBannerHelper.showPremiumModal(
                    this,
                    "GUÍA DE USO • WEBSITE",
                    getString(R.string.help_guide_title3),
                    "1. Desde Menú > Configuración puedes alternar entre Modo claro, Modo oscuro y Configuración del sistema.\n\n"
                            + "2. Activa o desactiva los interruptores de recordatorios de KPI y avisos generales.\n\n"
                            + "3. En Privacidad y permisos puedes revisar los permisos del sistema y la información guardada en el dispositivo.",
                    "Ir a Configuración",
                    () -> {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
                        startActivity(intent);
                    },
                    "Cerrar"
            ));
        }
    }

    private void setupSearchFilter() {
        EditText etSearch = findViewById(R.id.help_et_search);
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFaqByQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterFaqByQuery(String rawQuery) {
        String query = rawQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            setFaqExpanded(R.id.help_faq_body_1, R.id.help_faq_icon_1, false);
            setFaqExpanded(R.id.help_faq_body_2, R.id.help_faq_icon_2, false);
            setFaqExpanded(R.id.help_faq_body_3, R.id.help_faq_icon_3, false);
            setFaqExpanded(R.id.help_faq_body_4, R.id.help_faq_icon_4, false);
            return;
        }

        matchAndExpand(query, R.string.help_faq_q1, R.string.help_faq_a1, R.id.help_faq_body_1, R.id.help_faq_icon_1);
        matchAndExpand(query, R.string.help_faq_q2, R.string.help_faq_a2, R.id.help_faq_body_2, R.id.help_faq_icon_2);
        matchAndExpand(query, R.string.help_faq_q3, R.string.help_faq_a3, R.id.help_faq_body_3, R.id.help_faq_icon_3);
        matchAndExpand(query, R.string.help_faq_q4, R.string.help_faq_a4, R.id.help_faq_body_4, R.id.help_faq_icon_4);
    }

    private void matchAndExpand(String query, int qRes, int aRes, int bodyId, int iconId) {
        String qText = getString(qRes).toLowerCase(Locale.ROOT);
        String aText = getString(aRes).toLowerCase(Locale.ROOT);
        boolean matches = qText.contains(query) || aText.contains(query);
        setFaqExpanded(bodyId, iconId, matches);
    }

    private void setFaqExpanded(int bodyId, int iconId, boolean expanded) {
        View body = findViewById(bodyId);
        ImageView icon = findViewById(iconId);
        if (body != null) {
            body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
        if (icon != null) {
            icon.setRotation(expanded ? 180f : 0f);
        }
    }

    private void setupSupportContactActions() {
        // Fila de correo: informa que el correo oficial está por definir (sin inventar direcciones)
        View rowEmail = findViewById(R.id.help_row_contact_email);
        if (rowEmail != null) {
            rowEmail.setOnClickListener(v -> CollaboratorBannerHelper.showBanner(
                    this,
                    "Correo de soporte por definir",
                    "La dirección de correo institucional de soporte será confirmada próximamente.",
                    R.drawable.collab_ic_help_support
            ));
        }

    }
}
