package pe.net.ws.kpiweb.features.kpi.accountant.ui;

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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import pe.net.ws.kpiweb.features.kpi.accountant.data.KpiAccountantStorageManager;
import pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantDayRecord;
import pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantWeekReport;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment;
import pe.net.ws.kpiweb.features.kpi.ui.KpiRoleSelectorFragment;

/**
 * Formulario de Registro Diario del cargo CONTADOR (SIGD 2026).
 * Conectado dinámicamente a la sesión/base de datos real del colaborador y al
 * Centro de Reportes Semanales (KpiReportsHubFragment).
 */
public class KpiAccountantContainerFragment extends Fragment {

    private KpiAccountantStorageManager storage;
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

    private EditText etRegistros;
    private EditText etConciliaciones;
    private EditText etComprobantes;
    private EditText etDayObservations;

    private TextView tvDailyTotalValue;
    private TextView tvDailyComplianceValue;

    private TextView tvWeeklySumReg;
    private TextView tvWeeklySumConc;
    private TextView tvWeeklySumComp;
    private TextView tvWeeklySumTotal;
    private TextView tvWeeklyKpiLevelBadge;

    private LinearLayout weeklySummaryCard;
    private LinearLayout saturdayNoticeCard;
    private final CheckBox[] cbActions = new CheckBox[6];
    private CheckBox cbOtherActivities;
    private EditText etGeneralObservations;
    private Button btnSaveDay;

    public static KpiAccountantContainerFragment newInstance() {
        return new KpiAccountantContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        storage = KpiAccountantStorageManager.getInstance(requireContext());
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
        tvDesc.setText("Las secciones \"2. Resumen Semanal Acumulado\", \"3. Acciones del Contador\" y \"5. Observaciones Generales\" corresponden al balance final y se completan los días SÁBADO.");
        tvDesc.setTextSize(12f);
        tvDesc.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = dp(4);
        tvDesc.setLayoutParams(dLp);
        saturdayNoticeCard.addView(tvDesc);

        parent.addView(saturdayNoticeCard);
    }

    private void buildExecutiveHeaderCard(LinearLayout parent) {
        Context ctx = requireContext();
        LinearLayout headerCard = new LinearLayout(ctx);
        headerCard.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#4C1D95"), Color.parseColor("#6D28D9")}
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
        tvRoleBadge.setText("ÁREA ADMINISTRACIÓN Y FINANZAS · CONTROL SEMANAL SIGD");
        tvRoleBadge.setTextColor(Color.parseColor("#DDD6FE"));
        tvRoleBadge.setTextSize(11f);
        tvRoleBadge.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = dp(14);
        tvRoleBadge.setLayoutParams(badgeLp);
        headerCard.addView(tvRoleBadge);

        // Fila 3: Título del Cargo
        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("Contador");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(2);
        tvTitle.setLayoutParams(titleLp);
        headerCard.addView(tvTitle);

        // Fila 4: Slogan / Pilares
        TextView tvSub = new TextView(ctx);
        tvSub.setText(">>> REGISTRA • CONCILIA • CONTROLA • DECLARA • CIERRA");
        tvSub.setTextColor(Color.parseColor("#E0E7FF"));
        tvSub.setTextSize(11f);
        tvSub.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(2);
        tvSub.setLayoutParams(subLp);
        headerCard.addView(tvSub);

        // Fila 5: Colaborador
        String realUserName = resolveRealDatabaseCollaboratorName();
        tvHeaderCollabName = new TextView(ctx);
        tvHeaderCollabName.setText("👤 Colaborador: " + realUserName.toUpperCase(Locale.getDefault()));
        tvHeaderCollabName.setTextColor(Color.WHITE);
        tvHeaderCollabName.setTextSize(12.5f);
        tvHeaderCollabName.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colLp.topMargin = dp(10);
        tvHeaderCollabName.setLayoutParams(colLp);
        headerCard.addView(tvHeaderCollabName);

        // Fila 6: Jefe y Rango de Semana
        KpiAccountantWeekReport week = storage.getCurrentWeek();
        tvHeaderWeekRange = new TextView(ctx);
        String rangeText = week != null ? (week.getWeekId() + " · " + week.getDateRange()) : "Semana Activa";
        tvHeaderWeekRange.setText("Jefe: GERENCIA / DIRECCIÓN  •  " + rangeText);
        tvHeaderWeekRange.setTextColor(Color.parseColor("#C7D2FE"));
        tvHeaderWeekRange.setTextSize(11.5f);
        LinearLayout.LayoutParams rangeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rangeLp.topMargin = dp(3);
        tvHeaderWeekRange.setLayoutParams(rangeLp);
        headerCard.addView(tvHeaderWeekRange);

        // Fila 7: Selector de Turno (Píldoras Mañana / Tarde)
        LinearLayout shiftRow = new LinearLayout(ctx);
        shiftRow.setOrientation(LinearLayout.HORIZONTAL);
        shiftRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams shiftLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shiftLp.topMargin = dp(12);
        shiftRow.setLayoutParams(shiftLp);

        TextView tvShiftLabel = new TextView(ctx);
        tvShiftLabel.setText("Turno:");
        tvShiftLabel.setTextColor(Color.WHITE);
        tvShiftLabel.setTextSize(12f);
        tvShiftLabel.setTypeface(null, Typeface.BOLD);
        shiftRow.addView(tvShiftLabel);

        isMorningShift = week == null || week.isMorningShift();

        btnShiftMorning = new TextView(ctx);
        btnShiftMorning.setTextSize(11.5f);
        btnShiftMorning.setTypeface(null, Typeface.BOLD);
        btnShiftMorning.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.leftMargin = dp(8);
        btnShiftMorning.setLayoutParams(mLp);
        btnShiftMorning.setOnClickListener(v -> {
            isMorningShift = true;
            updateShiftButtonsUI();
            saveClosingMetadataSilently();
        });
        shiftRow.addView(btnShiftMorning);

        btnShiftAfternoon = new TextView(ctx);
        btnShiftAfternoon.setTextSize(11.5f);
        btnShiftAfternoon.setTypeface(null, Typeface.BOLD);
        btnShiftAfternoon.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.leftMargin = dp(8);
        btnShiftAfternoon.setLayoutParams(aLp);
        btnShiftAfternoon.setOnClickListener(v -> {
            isMorningShift = false;
            updateShiftButtonsUI();
            saveClosingMetadataSilently();
        });
        shiftRow.addView(btnShiftAfternoon);

        updateShiftButtonsUI();

        headerCard.addView(shiftRow);
        parent.addView(headerCard);
    }

    private void updateShiftButtonsUI() {
        if (getContext() == null || btnShiftMorning == null || btnShiftAfternoon == null) return;
        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setColor(Color.WHITE);
        activeBg.setCornerRadius(dp(14));

        GradientDrawable inactiveBg = new GradientDrawable();
        inactiveBg.setColor(Color.parseColor("#26FFFFFF"));
        inactiveBg.setCornerRadius(dp(14));

        if (isMorningShift) {
            btnShiftMorning.setText("☑ Turno mañana");
            btnShiftMorning.setTextColor(Color.parseColor("#4C1D95"));
            btnShiftMorning.setBackground(activeBg);

            btnShiftAfternoon.setText("☐ Turno tarde");
            btnShiftAfternoon.setTextColor(Color.WHITE);
            btnShiftAfternoon.setBackground(inactiveBg);
        } else {
            btnShiftMorning.setText("☐ Turno mañana");
            btnShiftMorning.setTextColor(Color.WHITE);
            btnShiftMorning.setBackground(inactiveBg);

            btnShiftAfternoon.setText("☑ Turno tarde");
            btnShiftAfternoon.setTextColor(Color.parseColor("#4C1D95"));
            btnShiftAfternoon.setBackground(activeBg);
        }
    }

    private void buildDayTabsBar(LinearLayout parent) {
        LinearLayout tabsRow = new LinearLayout(requireContext());
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        tabsRow.setLayoutParams(lp);

        dayTabs.clear();
        for (int i = 0; i < 6; i++) {
            final int dayIdx = i;
            TextView tab = new TextView(requireContext());
            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i < 5) tabLp.rightMargin = dp(4);
            tab.setLayoutParams(tabLp);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(12f);
            tab.setTypeface(null, Typeface.BOLD);
            tab.setText(KpiAccountantStorageManager.DAY_SHORTS[i]);
            tab.setOnClickListener(v -> {
                currentDayIndex = dayIdx;
                loadDayData(currentDayIndex);
            });
            dayTabs.add(tab);
            tabsRow.addView(tab);
        }
        parent.addView(tabsRow);
    }

    private void buildDailyCaptureCard(LinearLayout parent) {
        formCardContainer = new LinearLayout(requireContext());
        formCardContainer.setOrientation(LinearLayout.VERTICAL);
        formCardContainer.setBackgroundResource(R.drawable.kpi_bg_card_elegant_accent);
        formCardContainer.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        formCardContainer.setLayoutParams(lp);

        tvLockStatusBanner = new TextView(requireContext());
        tvLockStatusBanner.setText("🔒 DÍA CONFIRMADO Y BLOQUEADO CON BIOMETRÍA (SOLO LECTURA)");
        tvLockStatusBanner.setTextColor(Color.parseColor("#15803D"));
        tvLockStatusBanner.setBackgroundColor(Color.parseColor("#DCFCE7"));
        tvLockStatusBanner.setPadding(dp(10), dp(6), dp(10), dp(6));
        tvLockStatusBanner.setTextSize(11f);
        tvLockStatusBanner.setTypeface(null, Typeface.BOLD);
        tvLockStatusBanner.setVisibility(View.GONE);
        formCardContainer.addView(tvLockStatusBanner);

        tvDayTitle = new TextView(requireContext());
        tvDayTitle.setTextColor(Color.parseColor("#1E1B4B"));
        tvDayTitle.setTextSize(16f);
        tvDayTitle.setTypeface(null, Typeface.BOLD);
        tvDayTitle.setPadding(0, dp(4), 0, dp(6));
        formCardContainer.addView(tvDayTitle);

        tvMainActivityBadge = new TextView(requireContext());
        tvMainActivityBadge.setTextColor(Color.parseColor("#4C1D95"));
        tvMainActivityBadge.setTextSize(13f);
        tvMainActivityBadge.setTypeface(null, Typeface.BOLD);
        formCardContainer.addView(tvMainActivityBadge);

        tvDailyGoalBadge = new TextView(requireContext());
        tvDailyGoalBadge.setTextColor(Color.parseColor("#047857"));
        tvDailyGoalBadge.setTextSize(12f);
        tvDailyGoalBadge.setTypeface(null, Typeface.BOLD);
        tvDailyGoalBadge.setPadding(0, dp(2), 0, dp(12));
        formCardContainer.addView(tvDailyGoalBadge);

        etRegistros = addNumericInputRow(formCardContainer,
                "1. Registros Contables (Compras / Ventas / Cuentas):", "Ej. 22");
        etConciliaciones = addNumericInputRow(formCardContainer,
                "2. Conciliaciones Bancarias (Cuentas / Movimientos):", "Ej. 1");
        etComprobantes = addNumericInputRow(formCardContainer,
                "3. Comprobantes Procesados (SUNAT / Facturación):", "Ej. 22");

        LinearLayout liveRow = new LinearLayout(requireContext());
        liveRow.setOrientation(LinearLayout.HORIZONTAL);
        liveRow.setBackgroundColor(Color.parseColor("#F1F5F9"));
        liveRow.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams liveLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        liveLp.topMargin = dp(6);
        liveLp.bottomMargin = dp(10);
        liveRow.setLayoutParams(liveLp);

        tvDailyTotalValue = new TextView(requireContext());
        tvDailyTotalValue.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvDailyTotalValue.setTextColor(Color.parseColor("#0F172A"));
        tvDailyTotalValue.setTextSize(13f);
        tvDailyTotalValue.setTypeface(null, Typeface.BOLD);

        tvDailyComplianceValue = new TextView(requireContext());
        tvDailyComplianceValue.setTextColor(Color.parseColor("#16A34A"));
        tvDailyComplianceValue.setTextSize(13f);
        tvDailyComplianceValue.setTypeface(null, Typeface.BOLD);

        liveRow.addView(tvDailyTotalValue);
        liveRow.addView(tvDailyComplianceValue);
        formCardContainer.addView(liveRow);

        TextView lblObs = new TextView(requireContext());
        lblObs.setText("Observaciones del Día:");
        lblObs.setTextColor(Color.parseColor("#334155"));
        lblObs.setTextSize(12f);
        lblObs.setTypeface(null, Typeface.BOLD);
        formCardContainer.addView(lblObs);

        etDayObservations = new EditText(requireContext());
        etDayObservations.setHint("Ej. Registro de comprobantes del inicio de semana.");
        etDayObservations.setTextSize(13f);
        etDayObservations.setTextColor(Color.parseColor("#0F172A"));
        etDayObservations.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        etDayObservations.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams obsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        obsLp.topMargin = dp(4);
        obsLp.bottomMargin = dp(14);
        etDayObservations.setLayoutParams(obsLp);
        formCardContainer.addView(etDayObservations);

        btnSaveDay = new Button(requireContext());
        btnSaveDay.setText("GUARDAR Y CONFIRMAR DÍA (BIOMETRÍA)");
        btnSaveDay.setTextColor(Color.WHITE);
        btnSaveDay.setTypeface(null, Typeface.BOLD);
        btnSaveDay.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnSaveDay.setOnClickListener(v -> onSaveDayClicked());
        formCardContainer.addView(btnSaveDay);

        parent.addView(formCardContainer);
    }

    private EditText addNumericInputRow(LinearLayout parent, String label, String hint) {
        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#334155"));
        tvLabel.setTextSize(12f);
        tvLabel.setTypeface(null, Typeface.BOLD);
        parent.addView(tvLabel);

        EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(hint);
        et.setTextSize(14f);
        et.setTextColor(Color.parseColor("#0F172A"));
        et.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(8);
        et.setLayoutParams(lp);

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingUi) {
                    recalculateLiveDayPreview();
                }
            }
        });
        parent.addView(et);
        return et;
    }

    private void buildWeeklySummaryAndActionsCard(LinearLayout parent) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.kpi_bg_card_elegant_accent);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(lp);
        weeklySummaryCard = card;

        TextView tvSec2Title = new TextView(requireContext());
        tvSec2Title.setText("2. RESUMEN SEMANAL ACUMULADO (META ≥ 118 ACTIVIDADES)");
        tvSec2Title.setTextColor(Color.parseColor("#4C1D95"));
        tvSec2Title.setTextSize(13f);
        tvSec2Title.setTypeface(null, Typeface.BOLD);
        card.addView(tvSec2Title);

        tvWeeklySumReg = createStatLine(card);
        tvWeeklySumConc = createStatLine(card);
        tvWeeklySumComp = createStatLine(card);
        tvWeeklySumTotal = createStatLine(card);

        tvWeeklyKpiLevelBadge = new TextView(requireContext());
        tvWeeklyKpiLevelBadge.setTextSize(12f);
        tvWeeklyKpiLevelBadge.setTypeface(null, Typeface.BOLD);
        tvWeeklyKpiLevelBadge.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = dp(6);
        badgeLp.bottomMargin = dp(12);
        tvWeeklyKpiLevelBadge.setLayoutParams(badgeLp);
        card.addView(tvWeeklyKpiLevelBadge);

        TextView tvSec3Title = new TextView(requireContext());
        tvSec3Title.setText("3. ACCIONES QUE REALIZA EL CONTADOR");
        tvSec3Title.setTextColor(Color.parseColor("#4C1D95"));
        tvSec3Title.setTextSize(13f);
        tvSec3Title.setTypeface(null, Typeface.BOLD);
        card.addView(tvSec3Title);

        String[] fixedActions = {
                "Registro de compras, ventas y gastos.",
                "Conciliación bancaria de cuentas.",
                "Control de cuentas por pagar y por cobrar.",
                "Elaboración de reportes financieros.",
                "Declaración de impuestos y obligaciones (SUNAT).",
                "Cierre y validación de la información contable."
        };
        for (int i = 0; i < fixedActions.length; i++) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(fixedActions[i]);
            cb.setTextColor(Color.parseColor("#1E293B"));
            cb.setTextSize(12f);
            cb.setChecked(true);
            cb.setPadding(0, dp(2), 0, dp(2));
            cbActions[i] = cb;
            card.addView(cb);
        }

        cbOtherActivities = new CheckBox(requireContext());
        cbOtherActivities.setText("Otras actividades asignadas.");
        cbOtherActivities.setTextColor(Color.parseColor("#1E293B"));
        cbOtherActivities.setTextSize(12f);
        cbOtherActivities.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUi) saveClosingMetadataSilently();
        });
        card.addView(cbOtherActivities);

        TextView tvSec5Title = new TextView(requireContext());
        tvSec5Title.setText("5. OBSERVACIONES GENERALES DE LA SEMANA");
        tvSec5Title.setTextColor(Color.parseColor("#4C1D95"));
        tvSec5Title.setTextSize(13f);
        tvSec5Title.setTypeface(null, Typeface.BOLD);
        tvSec5Title.setPadding(0, dp(10), 0, dp(4));
        card.addView(tvSec5Title);

        etGeneralObservations = new EditText(requireContext());
        etGeneralObservations.setTextSize(12f);
        etGeneralObservations.setTextColor(Color.parseColor("#0F172A"));
        etGeneralObservations.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        etGeneralObservations.setPadding(dp(12), dp(10), dp(12), dp(10));
        etGeneralObservations.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdatingUi) saveClosingMetadataSilently();
            }
        });
        card.addView(etGeneralObservations);

        parent.addView(card);
    }

    private TextView createStatLine(LinearLayout parent) {
        TextView tv = new TextView(requireContext());
        tv.setTextColor(Color.parseColor("#1E293B"));
        tv.setTextSize(12f);
        tv.setPadding(0, dp(3), 0, dp(3));
        parent.addView(tv);
        return tv;
    }

    private void loadDayData(int index) {
        isUpdatingUi = true;
        KpiAccountantWeekReport week = storage.getCurrentWeek();
        if (week == null || week.getDays() == null || index < 0 || index >= week.getDays().size()) {
            isUpdatingUi = false;
            return;
        }

        List<KpiAccountantDayRecord> days = week.getDays();
        for (int i = 0; i < dayTabs.size() && i < days.size(); i++) {
            TextView tab = dayTabs.get(i);
            KpiAccountantDayRecord d = days.get(i);
            String label = d.getDayShort() + (d.isLocked() ? " ✓" : "");
            tab.setText(label);
            if (i == index) {
                tab.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                tab.setTextColor(Color.WHITE);
            } else if (d.isLocked()) {
                tab.setBackgroundColor(Color.parseColor("#DCFCE7"));
                tab.setTextColor(Color.parseColor("#15803D"));
            } else {
                tab.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                tab.setTextColor(Color.parseColor("#475569"));
            }
        }

        KpiAccountantDayRecord current = days.get(index);
        tvDayTitle.setText("DÍA: " + current.getDayName().toUpperCase(Locale.getDefault()) + " (" + current.getDate() + ")");
        tvMainActivityBadge.setText("Modalidad / Actividad Principal: " + current.getMainActivity().replace("\n", " "));
        tvDailyGoalBadge.setText("Meta Diaria (por turno): " + current.getDailyGoalLabel().replace("\n", " ")
                + " • Esperado ref.: " + current.getDailyExpectedTotal() + " act.");

        if (current.isRegistered() || current.isLocked()) {
            etRegistros.setText(String.valueOf(current.getRegistrosCount()));
            etConciliaciones.setText(String.valueOf(current.getConciliacionesCount()));
            etComprobantes.setText(String.valueOf(current.getComprobantesCount()));
        } else {
            etRegistros.setText("");
            etConciliaciones.setText("");
            etComprobantes.setText("");
        }
        etDayObservations.setText(current.getObservations());

        cbOtherActivities.setChecked(week.isOtherActivitiesChecked());
        etGeneralObservations.setText(week.getGeneralObservations());

        boolean locked = current.isLocked();
        boolean isSaturday = (index == 5);

        if (weeklySummaryCard != null) {
            weeklySummaryCard.setVisibility(isSaturday ? View.VISIBLE : View.GONE);
        }
        if (saturdayNoticeCard != null) {
            saturdayNoticeCard.setVisibility(isSaturday ? View.GONE : View.VISIBLE);
        }

        tvLockStatusBanner.setVisibility(locked ? View.VISIBLE : View.GONE);
        etRegistros.setEnabled(!locked);
        etConciliaciones.setEnabled(!locked);
        etComprobantes.setEnabled(!locked);
        etDayObservations.setEnabled(!locked);
        btnSaveDay.setEnabled(!locked);
        btnSaveDay.setText(locked ? "DÍA CONFIRMADO Y BLOQUEADO ✓" :
                (isSaturday ? "GUARDAR SÁBADO Y CERRAR SEMANA (BIOMETRÍA)" :
                        "GUARDAR Y CONFIRMAR " + current.getDayName().toUpperCase(Locale.getDefault()) + " (BIOMETRÍA)"));
        formCardContainer.setAlpha(locked ? 0.45f : 1.0f);

        for (CheckBox cb : cbActions) {
            if (cb != null) cb.setEnabled(!locked && isSaturday);
        }
        if (cbOtherActivities != null) cbOtherActivities.setEnabled(!locked && isSaturday);
        if (etGeneralObservations != null) etGeneralObservations.setEnabled(!locked && isSaturday);

        isUpdatingUi = false;
        recalculateLiveDayPreview();
        refreshWeeklySummaryBanner();
    }

    private void recalculateLiveDayPreview() {
        KpiAccountantWeekReport week = storage.getCurrentWeek();
        if (week == null || week.getDays() == null || currentDayIndex >= week.getDays().size()) return;
        KpiAccountantDayRecord current = week.getDays().get(currentDayIndex);

        int r = parseIntSafe(etRegistros.getText().toString());
        int c = parseIntSafe(etConciliaciones.getText().toString());
        int comp = parseIntSafe(etComprobantes.getText().toString());
        int total = r + c + comp;
        int expected = Math.max(1, current.getDailyExpectedTotal());
        int pct = Math.min(100, Math.round((total * 100f) / expected));

        tvDailyTotalValue.setText("Total Actividades Hoy: " + total);
        tvDailyComplianceValue.setText("Cumplimiento: " + pct + "%");
    }

    private void refreshWeeklySummaryBanner() {
        KpiAccountantWeekReport week = storage.getCurrentWeek();
        if (week == null || week.getDays() == null) return;

        int sumReg = 0;
        int sumConc = 0;
        int sumComp = 0;
        int sumTot = 0;
        for (KpiAccountantDayRecord d : week.getDays()) {
            if (d.isRegistered() || d.isLocked()) {
                sumReg += d.getRegistrosCount();
                sumConc += d.getConciliacionesCount();
                sumComp += d.getComprobantesCount();
                sumTot += d.getTotalActivities();
            }
        }

        int pctReg = Math.min(100, Math.round((sumReg * 100f) / 50f));
        int pctConc = Math.min(100, Math.round((sumConc * 100f) / 2f));
        int pctComp = Math.min(100, Math.round((sumComp * 100f) / 50f));
        int pctWeek = Math.min(100, Math.round((sumTot * 100f) / 118f));

        tvWeeklySumReg.setText("• Registros contables: " + sumReg + " / meta ≥ 50 (" + pctReg + "%)");
        tvWeeklySumConc.setText("• Conciliaciones bancarias: " + sumConc + " / meta ≥ 2 (" + pctConc + "%)");
        tvWeeklySumComp.setText("• Comprobantes procesados: " + sumComp + " / meta ≥ 50 (" + pctComp + "%)");
        tvWeeklySumTotal.setText("• TOTAL SEMANAL: " + sumTot + " / 118 actividades (" + pctWeek + "%)");

        String level;
        int bgColor;
        int txtColor;
        if (pctWeek >= 90) {
            level = "NIVEL KPI: EXCELENTE (90% – 100%)";
            bgColor = Color.parseColor("#DCFCE7");
            txtColor = Color.parseColor("#15803D");
        } else if (pctWeek >= 80) {
            level = "NIVEL KPI: MUY BUENO (80% – 89%)";
            bgColor = Color.parseColor("#DBEAFE");
            txtColor = Color.parseColor("#1D4ED8");
        } else if (pctWeek >= 60) {
            level = "NIVEL KPI: BUENO (60% – 79%)";
            bgColor = Color.parseColor("#FEF9C3");
            txtColor = Color.parseColor("#A16207");
        } else if (pctWeek >= 40) {
            level = "NIVEL KPI: REGULAR (40% – 59%)";
            bgColor = Color.parseColor("#FFEDD5");
            txtColor = Color.parseColor("#C2410C");
        } else {
            level = "NIVEL KPI: DEFICIENTE (< 40%)";
            bgColor = Color.parseColor("#FEE2E2");
            txtColor = Color.parseColor("#B91C1C");
        }
        tvWeeklyKpiLevelBadge.setText(level);
        tvWeeklyKpiLevelBadge.setBackgroundColor(bgColor);
        tvWeeklyKpiLevelBadge.setTextColor(txtColor);
    }

    private void onSaveDayClicked() {
        KpiAccountantWeekReport week = storage.getCurrentWeek();
        if (week == null || week.getDays() == null || currentDayIndex >= week.getDays().size()) return;
        KpiAccountantDayRecord current = week.getDays().get(currentDayIndex);

        if (current.isLocked()) {
            Toast.makeText(requireContext(), "Este día ya fue bloqueado.", Toast.LENGTH_SHORT).show();
            return;
        }

        int r = parseIntSafe(etRegistros.getText().toString());
        int c = parseIntSafe(etConciliaciones.getText().toString());
        int comp = parseIntSafe(etComprobantes.getText().toString());
        int total = r + c + comp;
        String obs = etDayObservations.getText().toString().trim();

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Registro Diario • " + current.getDayName())
                .setMessage("Se registrarán:\n"
                        + "• Registros Contables: " + r + "\n"
                        + "• Conciliaciones Bancarias: " + c + "\n"
                        + "• Comprobantes Procesados: " + comp + "\n"
                        + "• Total Actividades: " + total + "\n\n"
                        + "Al confirmar con huella/credencial, este día quedará bloqueado en modo lectura.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Autenticar y Bloquear", (dialog, which) ->
                        KpiBiometricHelper.authenticate(
                                KpiAccountantContainerFragment.this,
                                "Confirmar KPI de " + current.getDayName() + " · Contador",
                                new KpiBiometricHelper.BiometricCallback() {
                                    @Override
                                    public void onSuccess() {
                                        commitDaySave(current, r, c, comp, obs);
                                    }

                                    @Override
                                    public void onError(int errorCode, String errorMessage) {
                                        if (errorCode == -1) {
                                            commitDaySave(current, r, c, comp, obs);
                                        } else {
                                            Toast.makeText(getContext(), "Verificación biométrica cancelada", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailed() {
                                        Toast.makeText(getContext(), "Biometría no reconocida", Toast.LENGTH_SHORT).show();
                                    }
                                }
                        )
                )
                .show();
    }

    private void commitDaySave(KpiAccountantDayRecord current, int r, int c, int comp, String obs) {
        current.setRegistrosCount(r);
        current.setConciliacionesCount(c);
        current.setComprobantesCount(comp);
        current.setObservations(obs.isEmpty() ? getDefaultObservationForDay(currentDayIndex) : obs);
        current.setRegistered(true);
        current.setLocked(true);

        storage.saveDayRecord(current);
        saveClosingMetadataSilently();

        boolean isSaturday = (currentDayIndex == 5);
        boolean allComplete = storage.areAllCurrentWeekDaysLocked();
        int nextIdx = storage.getNextUnlockedDayIndexAfter(currentDayIndex);

        if (isSaturday || allComplete) {
            storage.markCurrentWeekSubmitted();
            KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());
            loadDayData(currentDayIndex);
            KpiPandaSuccessDialog.show(requireContext(), "Contador", false,
                    allComplete ? "KPI Semanal Completo" : current.getDayName(),
                    current.getCompliancePercentage(), Math.max(10, current.getTotalActivities()), () -> {
                        if (nextIdx >= 0 && nextIdx != currentDayIndex) {
                            currentDayIndex = nextIdx;
                            loadDayData(currentDayIndex);
                        }
                    });
        } else {
            KpiSoundHelper.playDailySuccess(requireContext());
            Toast.makeText(requireContext(),
                    "✓ " + current.getDayName() + " guardado y actualizado en el Reporte Semanal.",
                    Toast.LENGTH_SHORT).show();
            if (nextIdx >= 0) {
                currentDayIndex = nextIdx;
            }
            loadDayData(currentDayIndex);
        }
    }

    private String getDefaultObservationForDay(int dayIdx) {
        switch (dayIdx) {
            case 0: return "Registro de comprobantes del inicio de semana.";
            case 1: return "Conciliación del banco principal.";
            case 2: return "Actualización de cuentas y vencimientos.";
            case 3: return "Reporte de estado de resultados (avance).";
            case 4: return "Declaración y revisión de impuestos.";
            default: return "Cierre contable semanal y respaldo.";
        }
    }

    private void saveClosingMetadataSilently() {
        if (currentDayIndex != 5) return;
        boolean morning = isMorningShift;
        boolean other = cbOtherActivities != null && cbOtherActivities.isChecked();
        String genObs = etGeneralObservations != null ? etGeneralObservations.getText().toString() : "";
        storage.saveWeekClosingData(morning, other, genObs);
    }

    private String resolveRealDatabaseCollaboratorName() {
        String authName = KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        List<KpiCollaboratorProfile> list = repo.getCollaboratorsForRole("Contador");
        if (list != null && !list.isEmpty()) {
            return list.get(0).getName();
        }
        if (authName != null && !authName.trim().isEmpty() && !authName.equals("Colaborador WebSite+")) {
            return authName.trim();
        }
        return "Colaborador (Contador)";
    }

    private void openOfficialReportHub() {
        saveClosingMetadataSilently();
        if (getActivity() != null) {
            String collabName = resolveRealDatabaseCollaboratorName();
            Fragment hub = KpiReportsHubFragment.newInstanceExclusive("Contador", collabName);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main, hub)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private int parseIntSafe(String val) {
        try {
            return Math.max(0, Integer.parseInt(val.trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
