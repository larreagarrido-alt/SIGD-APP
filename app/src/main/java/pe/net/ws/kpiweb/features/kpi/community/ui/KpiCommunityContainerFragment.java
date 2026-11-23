package pe.net.ws.kpiweb.features.kpi.community.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.community.data.KpiCommunityStorageManager;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityDayRecord;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityWeekReport;
import pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment;
import pe.net.ws.kpiweb.features.kpi.ui.KpiRoleSelectorFragment;

/**
 * Formulario de Registro Diario del cargo COMMUNITY MANAGER (SIGD 2026).
 * Conectado dinámicamente a la sesión/base de datos real del colaborador y al
 * Centro de Reportes Semanales (KpiReportsHubFragment).
 */
public class KpiCommunityContainerFragment extends Fragment {

    private KpiCommunityStorageManager storage;
    private int currentDayIndex = 0;
    private boolean isUpdatingUi = false;

    private final List<TextView> dayTabs = new ArrayList<>();
    private TextView tvHeaderCollabName;
    private TextView tvHeaderWeekRange;
    private boolean isMorningShift = true;
    private TextView btnShiftMorning;
    private TextView btnShiftAfternoon;

    private LinearLayout formCardContainer;
    private TextView tvDayTitle;
    private TextView tvMainActivityBadge;
    private TextView tvDailyGoalBadge;
    private TextView tvLockStatusBanner;

    private EditText etPublicaciones;
    private EditText etInteracciones;
    private EditText etOtrasActividades;
    private EditText etDayObservations;

    private TextView tvDailyTotalValue;
    private TextView tvDailyComplianceValue;

    private TextView tvWeeklySumPub;
    private TextView tvWeeklySumInt;
    private TextView tvWeeklySumOtr;
    private TextView tvWeeklySumTotal;
    private TextView tvWeeklyGeneralCompliance;
    private TextView tvWeeklyKpiLevelBadge;

    private LinearLayout weeklySummaryCard;
    private LinearLayout saturdayNoticeCard;
    private final CheckBox[] cbActions = new CheckBox[6];
    private CheckBox cbOtherActivities;
    private EditText etGeneralObservations;
    private Button btnSaveDay;

    public static KpiCommunityContainerFragment newInstance() {
        return new KpiCommunityContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        storage = KpiCommunityStorageManager.getInstance(requireContext());
        currentDayIndex = storage.getTodayDayIndex();

        ScrollView rootScroll = new ScrollView(requireContext());
        rootScroll.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootScroll.setBackgroundColor(Color.parseColor("#F8FAFC"));
        rootScroll.setFillViewport(true);

        LinearLayout mainCol = new LinearLayout(requireContext());
        mainCol.setOrientation(LinearLayout.VERTICAL);
        mainCol.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootScroll.addView(mainCol);

        // Header de borde a borde (ancho 100%, sin márgenes laterales)
        buildExecutiveHeaderCard(mainCol);

        // Contenedor de contenido con márgenes cómodos (16dp)
        LinearLayout contentContainer = new LinearLayout(requireContext());
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        contentContainer.setPadding(pad, dp(14), pad, dp(32));
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainCol.addView(contentContainer);

        buildDayTabsBar(contentContainer);
        buildDailyCaptureCard(contentContainer);
        buildSaturdayNoticeCard(contentContainer);
        buildWeeklySummaryAndActionsCard(contentContainer);

        loadDayData(currentDayIndex);
        return rootScroll;
    }

    private void buildSaturdayNoticeCard(LinearLayout parent) {
        saturdayNoticeCard = new LinearLayout(requireContext());
        saturdayNoticeCard.setOrientation(LinearLayout.VERTICAL);
        saturdayNoticeCard.setBackgroundResource(R.drawable.kpi_bg_card_elegant_accent);
        saturdayNoticeCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        saturdayNoticeCard.setLayoutParams(lp);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("📅 CIERRE SEMANAL (HABILITADO LOS SÁBADOS)");
        tvTitle.setTextSize(12.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#334155"));
        saturdayNoticeCard.addView(tvTitle);

        TextView tvDesc = new TextView(requireContext());
        tvDesc.setText("Las secciones \"2. Resumen Semanal de Producción\", \"3. Acciones del Community Manager\" y \"5. Observaciones Generales\" corresponden al balance final y se completan los días SÁBADO.");
        tvDesc.setTextSize(12f);
        tvDesc.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = dp(4);
        tvDesc.setLayoutParams(dLp);
        saturdayNoticeCard.addView(tvDesc);

        TextView btnToggle = new TextView(requireContext());
        btnToggle.setText("👁️ Ver / Editar Cierre Semanal");
        btnToggle.setTextColor(Color.parseColor("#4C1D95"));
        btnToggle.setTextSize(12f);
        btnToggle.setTypeface(null, Typeface.BOLD);
        btnToggle.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable tgBg = new GradientDrawable();
        tgBg.setColor(Color.parseColor("#EDE9FE"));
        tgBg.setCornerRadius(dp(16));
        btnToggle.setBackground(tgBg);
        LinearLayout.LayoutParams tgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tgLp.topMargin = dp(8);
        btnToggle.setLayoutParams(tgLp);
        btnToggle.setOnClickListener(v -> {
            if (weeklySummaryCard != null) {
                boolean isVisible = weeklySummaryCard.getVisibility() == View.VISIBLE;
                weeklySummaryCard.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                btnToggle.setText(isVisible ? "👁️ Ver / Editar Cierre Semanal" : "▲ Ocultar Cierre Semanal");
            }
        });
        saturdayNoticeCard.addView(btnToggle);

        parent.addView(saturdayNoticeCard);
    }

    private void buildExecutiveHeaderCard(LinearLayout parent) {
        Context ctx = requireContext();
        LinearLayout headerCard = new LinearLayout(ctx);
        headerCard.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#311068"), Color.parseColor("#5B21B6")}
        );
        float r = dp(24);
        headerBg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        headerCard.setBackground(headerBg);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            headerCard.setElevation(dp(4));
        }

        int pSide = dp(18);
        headerCard.setPadding(pSide, dp(22), pSide, dp(20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerCard.setLayoutParams(lp);

        // Fila 1: Botón Volver + Spacer + Botón Reporte
        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnBack = new TextView(ctx);
        btnBack.setText("← Volver");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(13f);
        btnBack.setTypeface(null, Typeface.BOLD);
        btnBack.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(Color.parseColor("#33FFFFFF"));
        backBg.setCornerRadius(dp(20));
        btnBack.setBackground(backBg);
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main, KpiRoleSelectorFragment.newInstance())
                        .commit();
            }
        });
        if (!(requireActivity() instanceof pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity)) topRow.addView(btnBack);

        View spacer = new View(ctx);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(spacer);

        TextView btnOpenReport = new TextView(ctx);
        btnOpenReport.setText("📊 Reporte");
        btnOpenReport.setTextColor(Color.WHITE);
        btnOpenReport.setTextSize(13f);
        btnOpenReport.setTypeface(null, Typeface.BOLD);
        btnOpenReport.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable repBg = new GradientDrawable();
        repBg.setColor(Color.parseColor("#40FFFFFF"));
        repBg.setStroke(dp(1), Color.parseColor("#80FFFFFF"));
        repBg.setCornerRadius(dp(20));
        btnOpenReport.setBackground(repBg);
        btnOpenReport.setOnClickListener(v -> openOfficialReportHub());
        topRow.addView(btnOpenReport);

        headerCard.addView(topRow);

        // Fila 2: Badge del Área
        TextView tvRoleBadge = new TextView(ctx);
        tvRoleBadge.setText("ÁREA MARKETING DIGITAL · GESTIÓN DE COMUNIDADES SIGD");
        tvRoleBadge.setTextColor(Color.parseColor("#DDD6FE"));
        tvRoleBadge.setTextSize(11f);
        tvRoleBadge.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = dp(14);
        tvRoleBadge.setLayoutParams(badgeLp);
        headerCard.addView(tvRoleBadge);

        // Fila 3: Título Principal
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("COMMUNITY MANAGER");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        headerCard.addView(tvTitle);

        TextView tvSlogan = new TextView(ctx);
        tvSlogan.setText(">>> GESTIONA REDES • PUBLICA • INTERACTÚA • ANALIZA RESULTADOS");
        tvSlogan.setTextColor(Color.parseColor("#C7D2FE"));
        tvSlogan.setTextSize(11f);
        tvSlogan.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams slLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slLp.topMargin = dp(2);
        tvSlogan.setLayoutParams(slLp);
        headerCard.addView(tvSlogan);

        // Fila 4: Card de Colaborador y Periodo
        LinearLayout collabCard = new LinearLayout(ctx);
        collabCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable collabBg = new GradientDrawable();
        collabBg.setColor(Color.parseColor("#20FFFFFF"));
        collabBg.setCornerRadius(dp(14));
        collabCard.setBackground(collabBg);
        collabCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccLp.topMargin = dp(12);
        collabCard.setLayoutParams(ccLp);

        tvHeaderCollabName = new TextView(ctx);
        tvHeaderCollabName.setTextColor(Color.WHITE);
        tvHeaderCollabName.setTextSize(13f);
        tvHeaderCollabName.setTypeface(null, Typeface.BOLD);
        collabCard.addView(tvHeaderCollabName);

        tvHeaderWeekRange = new TextView(ctx);
        tvHeaderWeekRange.setTextColor(Color.parseColor("#E0E7FF"));
        tvHeaderWeekRange.setTextSize(11.5f);
        collabCard.addView(tvHeaderWeekRange);

        // Fila de Turno (Mañana / Tarde)
        LinearLayout shiftRow = new LinearLayout(ctx);
        shiftRow.setOrientation(LinearLayout.HORIZONTAL);
        shiftRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.topMargin = dp(8);
        shiftRow.setLayoutParams(srLp);

        TextView tvShiftLabel = new TextView(ctx);
        tvShiftLabel.setText("Turno: ");
        tvShiftLabel.setTextColor(Color.parseColor("#E0E7FF"));
        tvShiftLabel.setTextSize(12f);
        tvShiftLabel.setTypeface(null, Typeface.BOLD);
        shiftRow.addView(tvShiftLabel);

        btnShiftMorning = createShiftPill(ctx, "☀️ Turno mañana", true);
        btnShiftAfternoon = createShiftPill(ctx, "🌙 Turno tarde", false);

        btnShiftMorning.setOnClickListener(v -> updateShiftSelection(true));
        btnShiftAfternoon.setOnClickListener(v -> updateShiftSelection(false));

        shiftRow.addView(btnShiftMorning);
        shiftRow.addView(btnShiftAfternoon);
        collabCard.addView(shiftRow);

        headerCard.addView(collabCard);
        parent.addView(headerCard);
    }

    private TextView createShiftPill(Context ctx, String text, boolean isMorning) {
        TextView pill = new TextView(ctx);
        pill.setText(text);
        pill.setTextSize(11.5f);
        pill.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(6);
        pill.setLayoutParams(lp);
        applyShiftPillStyle(pill, false);
        return pill;
    }

    private void applyShiftPillStyle(TextView pill, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        if (selected) {
            gd.setColor(Color.WHITE);
            pill.setTextColor(Color.parseColor("#4C1D95"));
            pill.setTypeface(null, Typeface.BOLD);
        } else {
            gd.setColor(Color.parseColor("#25FFFFFF"));
            pill.setTextColor(Color.parseColor("#E0E7FF"));
            pill.setTypeface(null, Typeface.NORMAL);
        }
        gd.setCornerRadius(dp(12));
        pill.setBackground(gd);
    }

    private void updateShiftSelection(boolean morning) {
        this.isMorningShift = morning;
        applyShiftPillStyle(btnShiftMorning, morning);
        applyShiftPillStyle(btnShiftAfternoon, !morning);
        storage.saveShift(morning);
    }

    private void buildDayTabsBar(LinearLayout parent) {
        Context ctx = requireContext();
        LinearLayout tabsBar = new LinearLayout(ctx);
        tabsBar.setOrientation(LinearLayout.HORIZONTAL);
        tabsBar.setWeightSum(6);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        tabsBar.setLayoutParams(lp);

        dayTabs.clear();
        for (int i = 0; i < 6; i++) {
            final int index = i;
            TextView tab = new TextView(ctx);
            tab.setText(KpiCommunityStorageManager.DAY_SHORTS[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(12.5f);
            tab.setPadding(0, dp(10), 0, dp(10));
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) tLp.leftMargin = dp(4);
            tab.setLayoutParams(tLp);

            tab.setOnClickListener(v -> selectDayTab(index));
            dayTabs.add(tab);
            tabsBar.addView(tab);
        }
        parent.addView(tabsBar);
    }

    private void selectDayTab(int index) {
        this.currentDayIndex = index;
        for (int i = 0; i < dayTabs.size(); i++) {
            TextView tab = dayTabs.get(i);
            GradientDrawable gd = new GradientDrawable();
            if (i == index) {
                gd.setColor(Color.parseColor("#4C1D95"));
                tab.setTextColor(Color.WHITE);
                tab.setTypeface(null, Typeface.BOLD);
            } else {
                gd.setColor(Color.WHITE);
                tab.setTextColor(Color.parseColor("#64748B"));
                tab.setTypeface(null, Typeface.NORMAL);
                gd.setStroke(dp(1), Color.parseColor("#E2E8F0"));
            }
            gd.setCornerRadius(dp(10));
            tab.setBackground(gd);
        }
        loadDayData(index);
    }

    private void buildDailyCaptureCard(LinearLayout parent) {
        Context ctx = requireContext();
        formCardContainer = new LinearLayout(ctx);
        formCardContainer.setOrientation(LinearLayout.VERTICAL);
        formCardContainer.setBackgroundResource(R.drawable.kpi_bg_card_white);
        formCardContainer.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        formCardContainer.setLayoutParams(lp);

        // Banner de estado Bloqueado / Registrado
        tvLockStatusBanner = new TextView(ctx);
        tvLockStatusBanner.setTextSize(11.5f);
        tvLockStatusBanner.setGravity(Gravity.CENTER);
        tvLockStatusBanner.setPadding(dp(10), dp(6), dp(10), dp(6));
        tvLockStatusBanner.setVisibility(View.GONE);
        formCardContainer.addView(tvLockStatusBanner);

        // Título del Día
        tvDayTitle = new TextView(ctx);
        tvDayTitle.setTextSize(17f);
        tvDayTitle.setTypeface(null, Typeface.BOLD);
        tvDayTitle.setTextColor(Color.parseColor("#0F172A"));
        formCardContainer.addView(tvDayTitle);

        // Badge Actividad Principal
        tvMainActivityBadge = new TextView(ctx);
        tvMainActivityBadge.setTextSize(12f);
        tvMainActivityBadge.setTextColor(Color.parseColor("#4C1D95"));
        tvMainActivityBadge.setTypeface(null, Typeface.BOLD);
        tvMainActivityBadge.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable actBg = new GradientDrawable();
        actBg.setColor(Color.parseColor("#F5F3FF"));
        actBg.setCornerRadius(dp(8));
        tvMainActivityBadge.setBackground(actBg);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(6);
        tvMainActivityBadge.setLayoutParams(actLp);
        formCardContainer.addView(tvMainActivityBadge);

        // Meta Diaria Badge
        tvDailyGoalBadge = new TextView(ctx);
        tvDailyGoalBadge.setTextSize(11.5f);
        tvDailyGoalBadge.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams dgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dgLp.topMargin = dp(4);
        tvDailyGoalBadge.setLayoutParams(dgLp);
        formCardContainer.addView(tvDailyGoalBadge);

        // Separador
        View divider = new View(ctx);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divLp.topMargin = dp(12);
        divLp.bottomMargin = dp(12);
        divider.setLayoutParams(divLp);
        formCardContainer.addView(divider);

        // 3 Filas de Métricas con Stepper
        etPublicaciones = createMetricInputRow(formCardContainer, "📢 Publicaciones realizadas", "Post, Carrusel o Historia");
        etInteracciones = createMetricInputRow(formCardContainer, "💬 Interacciones atendidas", "Mensajes, comentarios, consultas y respuestas");
        etOtrasActividades = createMetricInputRow(formCardContainer, "📋 Otras actividades", "Coordinaciones, monitoreo y soporte");

        // Fila Resumen Diario (Total Actividades + % Cumplimiento)
        LinearLayout sumRow = new LinearLayout(ctx);
        sumRow.setOrientation(LinearLayout.HORIZONTAL);
        sumRow.setGravity(Gravity.CENTER_VERTICAL);
        sumRow.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable sumBg = new GradientDrawable();
        sumBg.setColor(Color.parseColor("#F8FAFC"));
        sumBg.setCornerRadius(dp(10));
        sumRow.setBackground(sumBg);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.topMargin = dp(12);
        sumRow.setLayoutParams(srLp);

        TextView tvTotLabel = new TextView(ctx);
        tvTotLabel.setText("Total Actividades: ");
        tvTotLabel.setTextColor(Color.parseColor("#334155"));
        tvTotLabel.setTextSize(12.5f);
        tvTotLabel.setTypeface(null, Typeface.BOLD);
        sumRow.addView(tvTotLabel);

        tvDailyTotalValue = new TextView(ctx);
        tvDailyTotalValue.setText("0");
        tvDailyTotalValue.setTextColor(Color.parseColor("#4C1D95"));
        tvDailyTotalValue.setTextSize(15f);
        tvDailyTotalValue.setTypeface(null, Typeface.BOLD);
        sumRow.addView(tvDailyTotalValue);

        View sumSpacer = new View(ctx);
        sumSpacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        sumRow.addView(sumSpacer);

        tvDailyComplianceValue = new TextView(ctx);
        tvDailyComplianceValue.setText("100%");
        tvDailyComplianceValue.setTextColor(Color.WHITE);
        tvDailyComplianceValue.setTextSize(12f);
        tvDailyComplianceValue.setTypeface(null, Typeface.BOLD);
        tvDailyComplianceValue.setPadding(dp(12), dp(5), dp(12), dp(5));
        GradientDrawable compBg = new GradientDrawable();
        compBg.setColor(Color.parseColor("#16A34A"));
        compBg.setCornerRadius(dp(12));
        tvDailyComplianceValue.setBackground(compBg);
        sumRow.addView(tvDailyComplianceValue);

        formCardContainer.addView(sumRow);

        // Observaciones del Día
        TextView tvObsLabel = new TextView(ctx);
        tvObsLabel.setText("Observaciones del Día:");
        tvObsLabel.setTextColor(Color.parseColor("#475569"));
        tvObsLabel.setTextSize(12f);
        tvObsLabel.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams olLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olLp.topMargin = dp(14);
        tvObsLabel.setLayoutParams(olLp);
        formCardContainer.addView(tvObsLabel);

        etDayObservations = new EditText(ctx);
        etDayObservations.setHint("Ej. Publicación de contenidos en Facebook e Instagram.");
        etDayObservations.setTextSize(12.5f);
        etDayObservations.setTextColor(Color.parseColor("#0F172A"));
        etDayObservations.setHintTextColor(Color.parseColor("#94A3B8"));
        etDayObservations.setBackgroundResource(R.drawable.kpi_bg_card_white);
        etDayObservations.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        oLp.topMargin = dp(6);
        etDayObservations.setLayoutParams(oLp);
        formCardContainer.addView(etDayObservations);

        // Botón Guardar
        btnSaveDay = new Button(ctx);
        btnSaveDay.setText("💾 Guardar Registro del Día");
        btnSaveDay.setTextColor(Color.WHITE);
        btnSaveDay.setTextSize(14f);
        btnSaveDay.setTypeface(null, Typeface.BOLD);
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#4C1D95"));
        btnBg.setCornerRadius(dp(12));
        btnSaveDay.setBackground(btnBg);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        bLp.topMargin = dp(16);
        btnSaveDay.setLayoutParams(bLp);
        btnSaveDay.setOnClickListener(v -> saveCurrentDayData());
        formCardContainer.addView(btnSaveDay);

        parent.addView(formCardContainer);
    }

    private EditText createMetricInputRow(LinearLayout parent, String title, String subtitle) {
        Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = dp(10);
        row.setLayoutParams(rLp);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textCol.setLayoutParams(tcLp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTextSize(13f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        textCol.addView(tvTitle);

        TextView tvSub = new TextView(ctx);
        tvSub.setText(subtitle);
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setTextSize(11f);
        textCol.addView(tvSub);

        row.addView(textCol);

        // Stepper: [-]  [EditText]  [+]
        LinearLayout stepper = new LinearLayout(ctx);
        stepper.setOrientation(LinearLayout.HORIZONTAL);
        stepper.setGravity(Gravity.CENTER_VERTICAL);

        EditText etValue = new EditText(ctx);
        etValue.setInputType(InputType.TYPE_CLASS_NUMBER);
        etValue.setText("0");
        etValue.setGravity(Gravity.CENTER);
        etValue.setTextSize(14f);
        etValue.setTypeface(null, Typeface.BOLD);
        etValue.setTextColor(Color.parseColor("#0F172A"));
        etValue.setBackgroundResource(R.drawable.kpi_bg_card_white);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(dp(44), dp(38));
        etLp.setMargins(dp(6), 0, dp(6), 0);
        etValue.setLayoutParams(etLp);

        TextView btnMinus = createStepperButton(ctx, "−", () -> {
            int val = parseInt(etValue.getText().toString());
            if (val > 0) etValue.setText(String.valueOf(val - 1));
        });

        TextView btnPlus = createStepperButton(ctx, "+", () -> {
            int val = parseInt(etValue.getText().toString());
            etValue.setText(String.valueOf(val + 1));
        });

        etValue.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingUi) recalculateDailyTotals();
            }
        });

        stepper.addView(btnMinus);
        stepper.addView(etValue);
        stepper.addView(btnPlus);

        row.addView(stepper);
        parent.addView(row);
        return etValue;
    }

    private TextView createStepperButton(Context ctx, String text, Runnable onClick) {
        TextView btn = new TextView(ctx);
        btn.setText(text);
        btn.setGravity(Gravity.CENTER);
        btn.setTextSize(18f);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(Color.parseColor("#4C1D95"));
        btn.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#EDE9FE"));
        bg.setCornerRadius(dp(8));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    private void buildWeeklySummaryAndActionsCard(LinearLayout parent) {
        Context ctx = requireContext();
        weeklySummaryCard = new LinearLayout(ctx);
        weeklySummaryCard.setOrientation(LinearLayout.VERTICAL);
        weeklySummaryCard.setBackgroundResource(R.drawable.kpi_bg_card_white);
        weeklySummaryCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        weeklySummaryCard.setLayoutParams(lp);

        // Título Bloque 2
        TextView tvSec2 = new TextView(ctx);
        tvSec2.setText("2. RESUMEN SEMANAL DE PRODUCCIÓN");
        tvSec2.setTextSize(14f);
        tvSec2.setTypeface(null, Typeface.BOLD);
        tvSec2.setTextColor(Color.parseColor("#311068"));
        weeklySummaryCard.addView(tvSec2);

        // Card Acumulados
        LinearLayout grid = new LinearLayout(ctx);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(12), dp(10), dp(12), dp(10));
        grid.setBackgroundResource(R.drawable.kpi_bg_card_elegant_accent);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gLp.topMargin = dp(8);
        grid.setLayoutParams(gLp);

        tvWeeklySumPub = createSummaryMetricRow(grid, "📢 Publicaciones (Meta ≥ 10):", "9 (90%)");
        tvWeeklySumInt = createSummaryMetricRow(grid, "💬 Interacciones atendidas (Meta ≥ 50):", "50 (100%)");
        tvWeeklySumOtr = createSummaryMetricRow(grid, "📋 Otras actividades:", "6");
        tvWeeklySumTotal = createSummaryMetricRow(grid, "⭐ Total Actividades Semanal:", "65");
        tvWeeklyGeneralCompliance = createSummaryMetricRow(grid, "📊 Cumplimiento General (4 indicadores):", "98%");

        weeklySummaryCard.addView(grid);

        // Bloque 3: Acciones que realiza el Community Manager
        TextView tvSec3 = new TextView(ctx);
        tvSec3.setText("3. ACCIONES QUE REALIZA EL COMMUNITY MANAGER");
        tvSec3.setTextSize(14f);
        tvSec3.setTypeface(null, Typeface.BOLD);
        tvSec3.setTextColor(Color.parseColor("#311068"));
        LinearLayout.LayoutParams s3Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        s3Lp.topMargin = dp(18);
        tvSec3.setLayoutParams(s3Lp);
        weeklySummaryCard.addView(tvSec3);

        String[] actionLabels = {
                "Gestionar y mantener activas las redes sociales.",
                "Programar y publicar contenidos según el calendario.",
                "Atender y responder mensajes, comentarios y consultas.",
                "Realizar seguimiento de métricas (alcance, interacción, etc.).",
                "Elaborar el reporte semanal de resultados.",
                "Coordinar la presencia digital de la marca."
        };

        for (int i = 0; i < 6; i++) {
            cbActions[i] = new CheckBox(ctx);
            cbActions[i].setText(actionLabels[i]);
            cbActions[i].setChecked(true);
            cbActions[i].setTextColor(Color.parseColor("#0F172A"));
            cbActions[i].setTextSize(12.5f);
            weeklySummaryCard.addView(cbActions[i]);
        }

        cbOtherActivities = new CheckBox(ctx);
        cbOtherActivities.setText("Otras actividades asignadas.");
        cbOtherActivities.setChecked(false);
        cbOtherActivities.setTextColor(Color.parseColor("#64748B"));
        cbOtherActivities.setTextSize(12.5f);
        weeklySummaryCard.addView(cbOtherActivities);

        // Bloque 4: Nivel KPI de Productividad
        TextView tvSec4 = new TextView(ctx);
        tvSec4.setText("4. EVALUACIÓN KPI - PRODUCTIVIDAD");
        tvSec4.setTextSize(14f);
        tvSec4.setTypeface(null, Typeface.BOLD);
        tvSec4.setTextColor(Color.parseColor("#311068"));
        LinearLayout.LayoutParams s4Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        s4Lp.topMargin = dp(18);
        tvSec4.setLayoutParams(s4Lp);
        weeklySummaryCard.addView(tvSec4);

        tvWeeklyKpiLevelBadge = new TextView(ctx);
        tvWeeklyKpiLevelBadge.setText("Nivel KPI · pendiente de actividades");
        tvWeeklyKpiLevelBadge.setTextSize(12f);
        tvWeeklyKpiLevelBadge.setTextColor(Color.parseColor("#15803D"));
        tvWeeklyKpiLevelBadge.setTypeface(null, Typeface.BOLD);
        tvWeeklyKpiLevelBadge.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable lvlBg = new GradientDrawable();
        lvlBg.setColor(Color.parseColor("#DCFCE7"));
        lvlBg.setCornerRadius(dp(8));
        tvWeeklyKpiLevelBadge.setBackground(lvlBg);
        LinearLayout.LayoutParams lvlLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lvlLp.topMargin = dp(6);
        tvWeeklyKpiLevelBadge.setLayoutParams(lvlLp);
        weeklySummaryCard.addView(tvWeeklyKpiLevelBadge);

        // Bloque 5: Observaciones Generales
        TextView tvSec5 = new TextView(ctx);
        tvSec5.setText("5. OBSERVACIONES GENERALES");
        tvSec5.setTextSize(14f);
        tvSec5.setTypeface(null, Typeface.BOLD);
        tvSec5.setTextColor(Color.parseColor("#311068"));
        LinearLayout.LayoutParams s5Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        s5Lp.topMargin = dp(18);
        tvSec5.setLayoutParams(s5Lp);
        weeklySummaryCard.addView(tvSec5);

        etGeneralObservations = new EditText(ctx);
        etGeneralObservations.setText("Se cumplió con la gestión de redes sociales, la publicación de 9 contenidos, la atención de 50 interacciones y la entrega del reporte semanal, alcanzando un 98% de cumplimiento general.");
        etGeneralObservations.setTextSize(12.5f);
        etGeneralObservations.setTextColor(Color.parseColor("#0F172A"));
        etGeneralObservations.setBackgroundResource(R.drawable.kpi_bg_card_white);
        etGeneralObservations.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams genLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        genLp.topMargin = dp(6);
        etGeneralObservations.setLayoutParams(genLp);
        weeklySummaryCard.addView(etGeneralObservations);

        // Botón Guardar Cierre Semanal
        Button btnSaveSaturday = new Button(ctx);
        btnSaveSaturday.setText("🔒 Guardar Cierre Semanal");
        btnSaveSaturday.setTextColor(Color.WHITE);
        btnSaveSaturday.setTextSize(14f);
        btnSaveSaturday.setTypeface(null, Typeface.BOLD);
        GradientDrawable satBg = new GradientDrawable();
        satBg.setColor(Color.parseColor("#311068"));
        satBg.setCornerRadius(dp(12));
        btnSaveSaturday.setBackground(satBg);
        LinearLayout.LayoutParams satLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        satLp.topMargin = dp(16);
        btnSaveSaturday.setLayoutParams(satLp);
        btnSaveSaturday.setOnClickListener(v -> saveSaturdayData());
        weeklySummaryCard.addView(btnSaveSaturday);

        parent.addView(weeklySummaryCard);
    }

    private TextView createSummaryMetricRow(LinearLayout parent, String label, String defaultValue) {
        Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(3);
        row.setLayoutParams(lp);

        TextView tvLbl = new TextView(ctx);
        tvLbl.setText(label);
        tvLbl.setTextColor(Color.parseColor("#334155"));
        tvLbl.setTextSize(12f);
        LinearLayout.LayoutParams lmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvLbl.setLayoutParams(lmLp);
        row.addView(tvLbl);

        TextView tvVal = new TextView(ctx);
        tvVal.setText(defaultValue);
        tvVal.setTextColor(Color.parseColor("#0F172A"));
        tvVal.setTextSize(12f);
        tvVal.setTypeface(null, Typeface.BOLD);
        row.addView(tvVal);

        parent.addView(row);
        return tvVal;
    }

    private void loadDayData(int dayIndex) {
        isUpdatingUi = true;
        KpiCommunityWeekReport week = storage.getCurrentWeekReport();
        if (week == null || week.getDays() == null || dayIndex < 0 || dayIndex >= week.getDays().size()) {
            isUpdatingUi = false;
            return;
        }

        // Datos del Colaborador
        String collabName = "NICOL RAMOS";
        String weekRange = week.getDateRange();
        tvHeaderCollabName.setText("Colaboradora: " + collabName + " · Área: MARKETING DIGITAL");
        tvHeaderWeekRange.setText(week.getWeekId() + " · " + weekRange);
        updateShiftSelection(week.isMorningShift());

        KpiCommunityDayRecord rec = week.getDays().get(dayIndex);
        tvDayTitle.setText(rec.getDayName().toUpperCase(Locale.getDefault()) + " (" + rec.getDate() + ")");
        tvMainActivityBadge.setText("Modalidad: " + rec.getMainActivity().replace("\n", " "));
        tvDailyGoalBadge.setText("Meta esperada: " + rec.getDailyGoalLabel() + " · Total base: " + rec.getDailyExpectedTotal() + " actividades");

        etPublicaciones.setText(String.valueOf(rec.getPublicacionesCount()));
        etInteracciones.setText(String.valueOf(rec.getInteraccionesCount()));
        etOtrasActividades.setText(String.valueOf(rec.getOtrasActividadesCount()));
        etDayObservations.setText(rec.getObservations());

        // CheckBoxes de Acciones
        cbActions[0].setChecked(week.isAction1GestionarRedes());
        cbActions[1].setChecked(week.isAction2ProgramarContenidos());
        cbActions[2].setChecked(week.isAction3AtenderMensajes());
        cbActions[3].setChecked(week.isAction4SeguimientoMetricas());
        cbActions[4].setChecked(week.isAction5ElaborarReporte());
        cbActions[5].setChecked(week.isAction6CoordinarPresencia());
        cbOtherActivities.setChecked(week.isOtherActivitiesChecked());
        etGeneralObservations.setText(week.getGeneralObservations());

        isUpdatingUi = false;
        recalculateDailyTotals();
        updateWeeklySummaryDisplay(week);

        // Control de visibilidad del Cierre Semanal
        boolean isSaturday = (dayIndex == 5) || (storage.getTodayDayIndex() == 5);
        if (saturdayNoticeCard != null) {
            saturdayNoticeCard.setVisibility(isSaturday ? View.GONE : View.VISIBLE);
        }
        if (weeklySummaryCard != null) {
            weeklySummaryCard.setVisibility(isSaturday ? View.VISIBLE : View.GONE);
        }
    }

    private void recalculateDailyTotals() {
        int pub = parseInt(etPublicaciones.getText().toString());
        int inter = parseInt(etInteracciones.getText().toString());
        int otr = parseInt(etOtrasActividades.getText().toString());
        int total = pub + inter + otr;

        tvDailyTotalValue.setText(String.valueOf(total));

        KpiCommunityWeekReport week = storage.getCurrentWeekReport();
        int expected = (week != null && week.getDays() != null && currentDayIndex < week.getDays().size())
                ? week.getDays().get(currentDayIndex).getDailyExpectedTotal() : 1;
        int pct = expected > 0 ? Math.min(100, Math.round((total * 100f) / expected)) : 100;
        tvDailyComplianceValue.setText(pct + "%");
    }

    private void updateWeeklySummaryDisplay(KpiCommunityWeekReport week) {
        if (week == null) return;
        int totPub = week.getTotalPublicaciones();
        int totInt = week.getTotalInteracciones();
        int totOtr = week.getTotalOtrasActividades();
        int grandTotal = week.getTotalActivities();
        int genPct = week.getGeneralCompliancePercentage();

        tvWeeklySumPub.setText(totPub + " (" + week.getPublicacionesCompliance() + "%)");
        tvWeeklySumInt.setText(totInt + " (" + week.getInteraccionesCompliance() + "%)");
        tvWeeklySumOtr.setText(String.valueOf(totOtr));
        tvWeeklySumTotal.setText(String.valueOf(grandTotal));
        tvWeeklyGeneralCompliance.setText(genPct + "%");

        String lvl = week.getProductivityLevel();
        String desc = week.getProductivityDescription();
        tvWeeklyKpiLevelBadge.setText("⭐ " + lvl + " (" + week.getProductivityPercentage() + "%) · " + grandTotal + " actividades\n" + desc);
    }

    private void saveCurrentDayData() {
        int pub = parseInt(etPublicaciones.getText().toString());
        int inter = parseInt(etInteracciones.getText().toString());
        int otr = parseInt(etOtrasActividades.getText().toString());
        String obs = etDayObservations.getText().toString().trim();

        storage.saveDayRecord(currentDayIndex, pub, inter, otr, obs);
        KpiCommunityWeekReport week = storage.getCurrentWeekReport();
        updateWeeklySummaryDisplay(week);

        KpiSoundHelper.playDailySuccess(requireContext());
        Toast.makeText(requireContext(), "✅ Registro diario de Community Manager guardado correctamente", Toast.LENGTH_SHORT).show();
    }

    private void saveSaturdayData() {
        boolean a1 = cbActions[0].isChecked();
        boolean a2 = cbActions[1].isChecked();
        boolean a3 = cbActions[2].isChecked();
        boolean a4 = cbActions[3].isChecked();
        boolean a5 = cbActions[4].isChecked();
        boolean a6 = cbActions[5].isChecked();
        boolean other = cbOtherActivities.isChecked();
        String genObs = etGeneralObservations.getText().toString().trim();

        storage.saveSaturdayClosing(a1, a2, a3, a4, a5, a6, other, genObs);
        KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());

        KpiCommunityWeekReport week = storage.getCurrentWeekReport();
        KpiPandaSuccessDialog.show(
                requireContext(),
                "Community Manager",
                false,
                "Cierre Semanal Completo",
                week.getGeneralCompliancePercentage(),
                week.getTotalActivities(),
                this::openOfficialReportHub
        );
    }

    private void openOfficialReportHub() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main, KpiReportsHubFragment.newInstanceExclusive("Community Manager",
                            pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName()))
                    .addToBackStack(null)
                    .commit();
        }
    }

    private int parseInt(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
