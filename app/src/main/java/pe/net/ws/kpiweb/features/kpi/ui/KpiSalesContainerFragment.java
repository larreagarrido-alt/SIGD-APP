package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.data.KpiRouteDbHelper;
import pe.net.ws.kpiweb.features.kpi.data.KpiSalesWeekManager;
import pe.net.ws.kpiweb.features.kpi.model.KpiRouteItem;
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesDay;

/**
 * Contenedor principal reestructurado para el rol Asesor de Ventas.
 * 
 * Arquitectura de 3 Pestañas Superiores:
 * 1. Actividades Diarias (Ficha KPI con 6 indicadores, desbloqueo de Lun a Sáb y verificación biométrica real).
 * 2. Hoja de Ruta (Gestión manual de visitas comerciales, llamadas y seguimientos con dirección opcional).
 * 3. Reportes (Pantalla única con Bloque 1: Consolidado KPI y Bloque 2: Consolidado Hoja de Ruta).
 */
public class KpiSalesContainerFragment extends Fragment {

    public static final int TAB_DAILY_KPI = 0;
    public static final int TAB_ROUTE = 1;
    public static final int TAB_REPORTS = 2;

    private KpiSalesWeekManager weekManager;
    private KpiRouteDbHelper routeDbHelper;
    private int currentTab = TAB_DAILY_KPI;
    private int selectedDayIndex = 0;
    private String currentRouteFilter = "Todos";

    // Contenedores Master
    private FrameLayout flSalesContent;
    private LinearLayout llFloatingToast;
    private TextView tvToastMessage;
    private View btnContainerBack;
    private TextView btnSalesGoToReport;
    private TextView tvHeaderGreeting;
    private TextView tvHeaderSubtitle;

    // Pestañas Superiores (3 Tabs)
    private LinearLayout tabTopDailyKpi, tabTopRoute, tabTopReports;
    private ImageView ivTabTopDailyKpi, ivTabTopRoute, ivTabTopReports;
    private TextView tvTabTopDailyKpi, tvTabTopRoute, tvTabTopReports;

    // Sub-vistas de las 3 pestañas
    private View viewDailyKpi;
    private View viewRoute;
    private View viewReports;

    // Componentes Pestaña 1: Actividades Diarias (KPI)
    private LinearLayout llDayTabsContainer;
    private TextView tvDayNameTitle, tvDayFullDate, tvDayStatusBadge;
    private LinearLayout llDayLockedBanner;
    private TextView tvDayLockedMessage;
    private LinearLayout llMetasContentContainer;

    // 6 Steppers
    private TextView tvValueLlamadas, tvCardRatioLlamadas, tvCardPercentLlamadas;
    private ProgressBar pbCardLlamadas;
    private TextView tvValueVisitas, tvCardRatioVisitas, tvCardPercentVisitas;
    private ProgressBar pbCardVisitas;
    private TextView tvValueCotiz, tvCardRatioCotiz, tvCardPercentCotiz;
    private ProgressBar pbCardCotiz;
    private TextView tvValueSeguimiento, tvCardRatioSeguimiento, tvCardPercentSeguimiento;
    private ProgressBar pbCardSeguimiento;
    private TextView tvValueProspectos, tvCardRatioProspectos, tvCardPercentProspectos;
    private ProgressBar pbCardProspectos;
    private TextView tvValueClientes, tvCardRatioClientes, tvCardPercentClientes;
    private ProgressBar pbCardClientes;

    private EditText etDayObservaciones;
    private TextView tvObsCounter;
    private TextView tvLockExplanation;
    private Button btnSaveDayAction;
    private final java.util.List<View> stepperButtons = new java.util.ArrayList<>();

    // Componentes Pestaña 2: Hoja de Ruta
    private TextView tvRouteStatTotal, tvRouteStatVisits, tvRouteStatCalls, tvRouteStatClosed;
    private Button btnRouteAddNew;
    private LinearLayout llRouteCardsContainer, llRouteEmptyState;
    private TextView tvEmptyStateExplanation;
    private TextView btnFilterAll, btnFilterMon, btnFilterTue, btnFilterWed, btnFilterThu, btnFilterFri, btnFilterSat;

    // Componentes Pestaña 3: Reportes (2 Bloques)
    private LinearLayout llReportKpiLocked, llReportKpiUnlocked;
    private TextView tvReportKpiLockedDays, tvReportKpiGlobalPercent;
    private LinearLayout llReportKpiIndicatorsTable, llReportKpiDailyBreakdown;
    private Button btnReportGoToKpi, btnReportCopyKpi, btnReportResetWeek;
    private TextView tvReportRouteCountTitle;
    private Button btnReportCopyRoute;
    private LinearLayout llReportRouteItemsContainer, llReportRouteEmptyState;

    public static KpiSalesContainerFragment newInstance() {
        return new KpiSalesContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.sales_fragment_container, container, false);
        weekManager = KpiSalesWeekManager.getInstance(requireContext());
        routeDbHelper = KpiRouteDbHelper.getInstance(requireContext());
        selectedDayIndex = weekManager.getTodayDayIndex();

        initMasterViews(root, inflater);
        setupTopTabs();
        setupMasterListeners();

        KpiThemeContrastApplier.apply(root);
        KpiThemeContrastApplier.apply(viewDailyKpi);
        KpiThemeContrastApplier.apply(viewRoute);
        KpiThemeContrastApplier.apply(viewReports);

        // Carga inicial directa en Actividades Diarias (sin dashboard propio)
        switchTab(TAB_DAILY_KPI);
        return root;
    }

    private void initMasterViews(View root, LayoutInflater inflater) {
        flSalesContent = root.findViewById(R.id.flSalesContent);
        llFloatingToast = root.findViewById(R.id.llFloatingToast);
        tvToastMessage = root.findViewById(R.id.tvToastMessage);
        btnContainerBack = root.findViewById(R.id.btnContainerBack);
        btnSalesGoToReport = root.findViewById(R.id.btnSalesGoToReport);
        tvHeaderGreeting = root.findViewById(R.id.tvHeaderGreeting);
        tvHeaderSubtitle = root.findViewById(R.id.tvHeaderSubtitle);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        boolean isPracticante = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String userPrefix = isPracticante ? "Practicante: " : "Colaborador: ";
        if (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")) {
            if (tvHeaderGreeting != null) {
                String firstName = authName.trim().split("\\s+")[0];
                tvHeaderGreeting.setText("¡Hola, " + firstName + "!");
            }
            if (tvHeaderSubtitle != null) {
                tvHeaderSubtitle.setText(userPrefix + authName + " · Asesor Comercial");
            }
            TextView tvHeaderAvatar = root.findViewById(R.id.tvHeaderAvatar);
            if (tvHeaderAvatar != null) {
                String[] parts = authName.trim().split("\\s+");
                String initials = parts.length > 1 ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase(java.util.Locale.ROOT)
                        : ("" + authName.charAt(0)).toUpperCase(java.util.Locale.ROOT);
                tvHeaderAvatar.setText(initials);
            }
        }

        // Pestañas Superiores
        tabTopDailyKpi = root.findViewById(R.id.tabTopDailyKpi);
        tabTopRoute = root.findViewById(R.id.tabTopRoute);
        tabTopReports = null;

        ivTabTopDailyKpi = root.findViewById(R.id.ivTabTopDailyKpi);
        ivTabTopRoute = root.findViewById(R.id.ivTabTopRoute);
        ivTabTopReports = null;

        tvTabTopDailyKpi = root.findViewById(R.id.tvTabTopDailyKpi);
        tvTabTopRoute = root.findViewById(R.id.tvTabTopRoute);
        tvTabTopReports = null;

        // Inflar las 3 sub-vistas
        viewDailyKpi = inflater.inflate(R.layout.sales_view_daily_kpi, flSalesContent, false);
        viewRoute = inflater.inflate(R.layout.sales_view_route, flSalesContent, false);
        viewReports = inflater.inflate(R.layout.sales_view_reports, flSalesContent, false);

        initDailyKpiViews();
        initRouteViews();
        initReportsViews();
    }

    private void setupTopTabs() {
        if (tabTopDailyKpi != null) tabTopDailyKpi.setOnClickListener(v -> switchTab(TAB_DAILY_KPI));
        if (tabTopRoute != null) tabTopRoute.setOnClickListener(v -> switchTab(TAB_ROUTE));
        if (tabTopReports != null) tabTopReports.setOnClickListener(v -> switchTab(TAB_REPORTS));
    }

    private void setupMasterListeners() {
        btnContainerBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        if (btnSalesGoToReport != null) {
            btnSalesGoToReport.setOnClickListener(v -> openReportsHub());
        }
    }

    private void openReportsHub() {
        if (getActivity() != null) {
            String collabName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String cargoName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
            if (cargoName == null || cargoName.trim().isEmpty()) {
                cargoName = "Asesor de Ventas";
            }
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment.newInstanceExclusive(cargoName, collabName))
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void switchTab(int tab) {
        currentTab = tab;
        flSalesContent.removeAllViews();
        resetTopTabStyles();

        int colorActiveText = ContextCompat.getColor(requireContext(), R.color.kpi_primary);
        int colorInactiveText = 0xFFDDD6FE;

        switch (tab) {
            case TAB_DAILY_KPI:
                tabTopDailyKpi.setBackgroundResource(R.drawable.sales_bg_top_tab_active);
                ivTabTopDailyKpi.setColorFilter(colorActiveText);
                tvTabTopDailyKpi.setTextColor(colorActiveText);
                updateDailyKpiDayTabs();
                updateDailyKpiViews();
                flSalesContent.addView(viewDailyKpi);
                break;

            case TAB_ROUTE:
                tabTopRoute.setBackgroundResource(R.drawable.sales_bg_top_tab_active);
                ivTabTopRoute.setColorFilter(colorActiveText);
                tvTabTopRoute.setTextColor(colorActiveText);
                refreshRouteData();
                flSalesContent.addView(viewRoute);
                break;

            case TAB_REPORTS:
                if (tabTopReports != null) {
                    tabTopReports.setBackgroundResource(R.drawable.sales_bg_top_tab_active);
                    ivTabTopReports.setColorFilter(colorActiveText);
                    tvTabTopReports.setTextColor(colorActiveText);
                }
                refreshReportsData();
                flSalesContent.addView(viewReports);
                break;
        }
    }

    private void resetTopTabStyles() {
        int colorInactive = 0xFFDDD6FE;
        if (tabTopDailyKpi != null) tabTopDailyKpi.setBackgroundResource(android.R.color.transparent);
        if (tabTopRoute != null) tabTopRoute.setBackgroundResource(android.R.color.transparent);
        if (tabTopReports != null) tabTopReports.setBackgroundResource(android.R.color.transparent);

        if (ivTabTopDailyKpi != null) ivTabTopDailyKpi.setColorFilter(colorInactive);
        if (ivTabTopRoute != null) ivTabTopRoute.setColorFilter(colorInactive);
        if (ivTabTopReports != null) ivTabTopReports.setColorFilter(colorInactive);

        if (tvTabTopDailyKpi != null) tvTabTopDailyKpi.setTextColor(colorInactive);
        if (tvTabTopRoute != null) tvTabTopRoute.setTextColor(colorInactive);
        if (tvTabTopReports != null) tvTabTopReports.setTextColor(colorInactive);
    }

    // =========================================================================
    // 1. LÓGICA DE ACTIVIDADES DIARIAS (KPI) CON AUTENTICACIÓN BIOMÉTRICA
    // =========================================================================

    private void initDailyKpiViews() {
        llDayTabsContainer = viewDailyKpi.findViewById(R.id.llDayTabsContainer);
        tvDayNameTitle = viewDailyKpi.findViewById(R.id.tvDayNameTitle);
        tvDayFullDate = viewDailyKpi.findViewById(R.id.tvDayFullDate);
        tvDayStatusBadge = viewDailyKpi.findViewById(R.id.tvDayStatusBadge);
        llDayLockedBanner = viewDailyKpi.findViewById(R.id.llDayLockedBanner);
        tvDayLockedMessage = viewDailyKpi.findViewById(R.id.tvDayLockedMessage);
        llMetasContentContainer = viewDailyKpi.findViewById(R.id.llMetasContentContainer);

        // Indicador 1: Llamadas
        tvValueLlamadas = viewDailyKpi.findViewById(R.id.tvValueLlamadas);
        tvCardRatioLlamadas = viewDailyKpi.findViewById(R.id.tvCardRatioLlamadas);
        tvCardPercentLlamadas = viewDailyKpi.findViewById(R.id.tvCardPercentLlamadas);
        pbCardLlamadas = viewDailyKpi.findViewById(R.id.pbCardLlamadas);
        setupNumericInput(tvValueLlamadas, 1, "Llamadas comerciales", "Meta semanal sugerida: 100", 17);

        // Indicador 2: Visitas
        tvValueVisitas = viewDailyKpi.findViewById(R.id.tvValueVisitas);
        tvCardRatioVisitas = viewDailyKpi.findViewById(R.id.tvCardRatioVisitas);
        tvCardPercentVisitas = viewDailyKpi.findViewById(R.id.tvCardPercentVisitas);
        pbCardVisitas = viewDailyKpi.findViewById(R.id.pbCardVisitas);
        setupNumericInput(tvValueVisitas, 2, "Nuevas visitas a empresas", "Meta semanal sugerida: 60", 10);

        // Indicador 3: Cotizaciones
        tvValueCotiz = viewDailyKpi.findViewById(R.id.tvValueCotiz);
        tvCardRatioCotiz = viewDailyKpi.findViewById(R.id.tvCardRatioCotiz);
        tvCardPercentCotiz = viewDailyKpi.findViewById(R.id.tvCardPercentCotiz);
        pbCardCotiz = viewDailyKpi.findViewById(R.id.pbCardCotiz);
        setupNumericInput(tvValueCotiz, 3, "Cotizaciones enviadas", "Meta semanal sugerida: 30", 5);

        // Indicador 4: Seguimiento
        tvValueSeguimiento = viewDailyKpi.findViewById(R.id.tvValueSeguimiento);
        tvCardRatioSeguimiento = viewDailyKpi.findViewById(R.id.tvCardRatioSeguimiento);
        tvCardPercentSeguimiento = viewDailyKpi.findViewById(R.id.tvCardPercentSeguimiento);
        pbCardSeguimiento = viewDailyKpi.findViewById(R.id.pbCardSeguimiento);
        setupNumericInput(tvValueSeguimiento, 4, "Empresas en seguimiento", "Meta semanal sugerida: 50", 8);

        // Indicador 5: Prospectos
        tvValueProspectos = viewDailyKpi.findViewById(R.id.tvValueProspectos);
        tvCardRatioProspectos = viewDailyKpi.findViewById(R.id.tvCardRatioProspectos);
        tvCardPercentProspectos = viewDailyKpi.findViewById(R.id.tvCardPercentProspectos);
        pbCardProspectos = viewDailyKpi.findViewById(R.id.pbCardProspectos);
        setupNumericInput(tvValueProspectos, 5, "Nuevos prospectos", "Meta semanal sugerida: 15", 3);

        // Indicador 6: Clientes
        tvValueClientes = viewDailyKpi.findViewById(R.id.tvValueClientes);
        tvCardRatioClientes = viewDailyKpi.findViewById(R.id.tvCardRatioClientes);
        tvCardPercentClientes = viewDailyKpi.findViewById(R.id.tvCardPercentClientes);
        pbCardClientes = viewDailyKpi.findViewById(R.id.pbCardClientes);
        setupNumericInput(tvValueClientes, 6, "Nuevos clientes (Cierres)", "Meta semanal sugerida: 3", 1);

        // Observaciones y Guardar
        etDayObservaciones = viewDailyKpi.findViewById(R.id.etDayObservaciones);
        tvObsCounter = viewDailyKpi.findViewById(R.id.tvObsCounter);
        tvLockExplanation = viewDailyKpi.findViewById(R.id.tvLockExplanation);
        btnSaveDayAction = viewDailyKpi.findViewById(R.id.btnSaveDayAction);

        etDayObservaciones.setFocusable(false);
        etDayObservaciones.setClickable(true);
        etDayObservaciones.setOnClickListener(v -> {
            KpiSalesDay day = weekManager.getDay(selectedDayIndex);
            if (day.isSaved()) {
                jumpToNextPendingSalesDay();
                return;
            }
            KpiCustomKeypadDialog.showText(requireContext(),
                    "Observaciones · Asesor de Ventas",
                    "Anota detalles de tus visitas o llamadas",
                    day.getObservaciones(),
                    text -> {
                        etDayObservaciones.setText(text);
                        day.setObservaciones(text);
                    });
        });

        etDayObservaciones.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                tvObsCounter.setText(length + " / 400");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Al presionar Enviar Día -> Si ya fue enviado avanza al siguiente día pendiente; si no, pide biometría
        btnSaveDayAction.setOnClickListener(v -> {
            KpiSalesDay current = weekManager.getDay(selectedDayIndex);
            if (current.isSaved()) {
                jumpToNextPendingSalesDay();
            } else {
                initiateBiometricSave();
            }
        });
    }

    private void scrollDailyKpiToTop() {
        if (viewDailyKpi instanceof androidx.core.widget.NestedScrollView) {
            viewDailyKpi.post(() -> ((androidx.core.widget.NestedScrollView) viewDailyKpi).smoothScrollTo(0, 0));
        } else if (viewDailyKpi != null) {
            viewDailyKpi.post(() -> viewDailyKpi.scrollTo(0, 0));
        }
    }

    private void jumpToNextPendingSalesDay() {
        int nextIdx = weekManager.getNextUnsavedDayIndexAfter(selectedDayIndex);
        if (nextIdx != -1) {
            selectedDayIndex = nextIdx;
            updateDailyKpiDayTabs();
            updateDailyKpiViews();
            scrollDailyKpiToTop();
            showToastBanner("➔ Cargando " + weekManager.getDay(nextIdx).getDayName() + " (pendiente de llenar)");
        } else {
            scrollDailyKpiToTop();
            showToastBanner("✓ Todos los días de la semana ya fueron enviados");
        }
    }

    private void setupNumericInput(TextView box, int indicatorId, String title, String subtitle, int goalHint) {
        stepperButtons.add(box);
        box.setOnClickListener(v -> {
            KpiSalesDay day = weekManager.getDay(selectedDayIndex);
            if (day.isSaved()) {
                jumpToNextPendingSalesDay();
                return;
            }
            int currentVal = 0;
            switch (indicatorId) {
                case 1: currentVal = day.getLlamadas(); break;
                case 2: currentVal = day.getVisitas(); break;
                case 3: currentVal = day.getCotizaciones(); break;
                case 4: currentVal = day.getSeguimiento(); break;
                case 5: currentVal = day.getProspectos(); break;
                case 6: currentVal = day.getClientes(); break;
            }
            KpiCustomKeypadDialog.showNumeric(requireContext(), title, subtitle, currentVal, goalHint, value -> {
                switch (indicatorId) {
                    case 1: day.setLlamadas(value); break;
                    case 2: day.setVisitas(value); break;
                    case 3: day.setCotizaciones(value); break;
                    case 4: day.setSeguimiento(value); break;
                    case 5: day.setProspectos(value); break;
                    case 6: day.setClientes(value); break;
                }
                updateDailyKpiViews();
            });
        });
    }

    private void updateDailyKpiDayTabs() {
        llDayTabsContainer.removeAllViews();
        List<KpiSalesDay> days = weekManager.getDays();
        int todayIdx = weekManager.getTodayDayIndex();
        float density = getResources().getDisplayMetrics().density;
        int marginPx = (int) (3 * density);
        int padY = (int) (8 * density);
        int minHeightPx = (int) (58 * density);

        for (int i = 0; i < days.size(); i++) {
            final int index = i;
            KpiSalesDay day = days.get(i);
            boolean isSelected = (i == selectedDayIndex);
            boolean isSaved = day.isSaved();

            LinearLayout tab = new LinearLayout(getContext());
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(android.view.Gravity.CENTER);
            tab.setPadding(0, padY, 0, padY);
            tab.setMinimumHeight(minHeightPx);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(marginPx, 0, marginPx, 0);
            tab.setLayoutParams(params);

            if (isSelected) {
                tab.setBackgroundResource(R.drawable.kpi_bg_day_tab_active);
                tab.setElevation(4f * density);
                tab.setAlpha(1.0f);
            } else if (isSaved) {
                tab.setBackgroundResource(R.drawable.kpi_bg_day_tab_locked);
                tab.setElevation(0f);
                tab.setAlpha(0.55f);
            } else {
                tab.setBackgroundResource(R.drawable.kpi_bg_day_tab_inactive);
                tab.setElevation(2f * density);
                tab.setAlpha(1.0f);
            }

            TextView tvName = new TextView(getContext());
            tvName.setText(day.getShortName());
            tvName.setGravity(android.view.Gravity.CENTER);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
            tvName.setTextSize(12);
            tvName.setSingleLine(true);
            tvName.setTextColor(isSelected ? 0xFFFFFFFF : (isSaved ? 0xFF64748B : 0xFF0F172A));

            TextView tvNum = new TextView(getContext());
            tvNum.setText(String.valueOf(day.getDayNumber()));
            tvNum.setGravity(android.view.Gravity.CENTER);
            tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
            tvNum.setTextSize(14);
            tvNum.setSingleLine(true);
            tvNum.setTextColor(isSelected ? 0xFFFFFFFF : (isSaved ? 0xFF64748B : 0xFF4F46E5));

            TextView tvSub = new TextView(getContext());
            tvSub.setGravity(android.view.Gravity.CENTER);
            tvSub.setTextSize(9f);
            tvSub.setTypeface(null, android.graphics.Typeface.BOLD);
            tvSub.setSingleLine(true);

            if (isSelected) {
                tvSub.setText(isSaved ? "✓ VISTO" : "ACTIVO");
                tvSub.setTextColor(0xFFFFFFFF);
            } else if (isSaved) {
                tvSub.setText("✓ ENVIADO");
                tvSub.setTextColor(0xFF059669);
            } else if (i == todayIdx) {
                tvSub.setText("HOY");
                tvSub.setTextColor(0xFF4F46E5);
            } else {
                tvSub.setText("PEND.");
                tvSub.setTextColor(0xFF4F46E5);
            }

            tab.addView(tvName);
            tab.addView(tvNum);
            tab.addView(tvSub);

            tab.setOnClickListener(v -> {
                if (isSaved && index == selectedDayIndex) {
                    jumpToNextPendingSalesDay();
                } else {
                    selectedDayIndex = index;
                    updateDailyKpiDayTabs();
                    updateDailyKpiViews();
                    scrollDailyKpiToTop();
                }
            });

            llDayTabsContainer.addView(tab);
        }
    }

    private void updateDailyKpiViews() {
        KpiSalesDay day = weekManager.getDay(selectedDayIndex);
        boolean isSaved = day.isSaved();

        tvDayNameTitle.setText(day.getDayName());
        tvDayFullDate.setText(day.getFullDateFormatted());
        llMetasContentContainer.setVisibility(View.VISIBLE);

        // Stepper 1: Llamadas
        int valLlamadas = day.getLlamadas();
        tvValueLlamadas.setText(String.valueOf(valLlamadas));
        int totLlamadas = weekManager.getTotalLlamadas();
        tvCardRatioLlamadas.setText(totLlamadas + " / " + KpiSalesWeekManager.GOAL_LLAMADAS + " sem");
        int pLlamadas = Math.min(100, (totLlamadas * 100) / KpiSalesWeekManager.GOAL_LLAMADAS);
        pbCardLlamadas.setProgress(pLlamadas);
        tvCardPercentLlamadas.setText(pLlamadas + "%");

        // Stepper 2: Visitas
        int valVisitas = day.getVisitas();
        tvValueVisitas.setText(String.valueOf(valVisitas));
        int totVisitas = weekManager.getTotalVisitas();
        tvCardRatioVisitas.setText(totVisitas + " / " + KpiSalesWeekManager.GOAL_VISITAS + " sem");
        int pVisitas = Math.min(100, (totVisitas * 100) / KpiSalesWeekManager.GOAL_VISITAS);
        pbCardVisitas.setProgress(pVisitas);
        tvCardPercentVisitas.setText(pVisitas + "%");

        // Stepper 3: Cotizaciones
        int valCotiz = day.getCotizaciones();
        tvValueCotiz.setText(String.valueOf(valCotiz));
        int totCotiz = weekManager.getTotalCotizaciones();
        tvCardRatioCotiz.setText(totCotiz + " / " + KpiSalesWeekManager.GOAL_COTIZACIONES + " sem");
        int pCotiz = Math.min(100, (totCotiz * 100) / KpiSalesWeekManager.GOAL_COTIZACIONES);
        pbCardCotiz.setProgress(pCotiz);
        tvCardPercentCotiz.setText(pCotiz + "%");

        // Stepper 4: Seguimiento
        int valSeg = day.getSeguimiento();
        tvValueSeguimiento.setText(String.valueOf(valSeg));
        int totSeg = weekManager.getTotalSeguimiento();
        tvCardRatioSeguimiento.setText(totSeg + " / " + KpiSalesWeekManager.GOAL_SEGUIMIENTO + " sem");
        int pSeg = Math.min(100, (totSeg * 100) / KpiSalesWeekManager.GOAL_SEGUIMIENTO);
        pbCardSeguimiento.setProgress(pSeg);
        tvCardPercentSeguimiento.setText(pSeg + "%");

        // Stepper 5: Prospectos
        int valProsp = day.getProspectos();
        tvValueProspectos.setText(String.valueOf(valProsp));
        int totProsp = weekManager.getTotalProspectos();
        tvCardRatioProspectos.setText(totProsp + " / " + KpiSalesWeekManager.GOAL_PROSPECTOS + " sem");
        int pProsp = Math.min(100, (totProsp * 100) / KpiSalesWeekManager.GOAL_PROSPECTOS);
        pbCardProspectos.setProgress(pProsp);
        tvCardPercentProspectos.setText(pProsp + "%");

        // Stepper 6: Clientes
        int valCli = day.getClientes();
        tvValueClientes.setText(String.valueOf(valCli));
        int totCli = weekManager.getTotalClientes();
        tvCardRatioClientes.setText(totCli + " / " + KpiSalesWeekManager.GOAL_CLIENTES + " sem");
        int pCli = Math.min(100, (totCli * 100) / KpiSalesWeekManager.GOAL_CLIENTES);
        pbCardClientes.setProgress(pCli);
        tvCardPercentClientes.setText(pCli + "%");

        // Observaciones
        if (!etDayObservaciones.getText().toString().equals(day.getObservaciones())) {
            etDayObservaciones.setText(day.getObservaciones());
        }

        int saved = weekManager.getSavedDaysCount();
        tvLockExplanation.setText("Días enviados: " + saved + " de 6. Puedes retroceder a llenar días previos pendientes; los días enviados quedan en solo lectura.");

        for (View btn : stepperButtons) {
            btn.setEnabled(!isSaved);
        }
        etDayObservaciones.setEnabled(!isSaved);

        float contentAlpha = isSaved ? 0.45f : 1.0f;
        for (int c = 0; c < llMetasContentContainer.getChildCount(); c++) {
            View child = llMetasContentContainer.getChildAt(c);
            if (child != btnSaveDayAction) {
                child.setAlpha(contentAlpha);
                if (isSaved) {
                    child.setOnClickListener(v -> jumpToNextPendingSalesDay());
                } else {
                    child.setOnClickListener(null);
                    child.setClickable(false);
                }
            }
        }

        if (isSaved) {
            int nextIdx = weekManager.getNextUnsavedDayIndexAfter(selectedDayIndex);
            tvDayStatusBadge.setText("✓ Enviado " + day.getSavedAt() + " (Solo lectura)");
            tvDayStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_secondary));

            llDayLockedBanner.setVisibility(View.VISIBLE);
            if (nextIdx != -1) {
                String nextName = weekManager.getDay(nextIdx).getDayName();
                tvDayLockedMessage.setText(day.getDayName() + " ya fue enviado (solo visualización). Toca aquí para ir al siguiente día pendiente: " + nextName + ".");
                llDayLockedBanner.setOnClickListener(v -> jumpToNextPendingSalesDay());
                btnSaveDayAction.setEnabled(true);
                btnSaveDayAction.setAlpha(1.0f);
                btnSaveDayAction.setText("➔ Ir al siguiente día pendiente (" + nextName + ")");
            } else {
                tvDayLockedMessage.setText("✓ Semana completa enviada. Estás visualizando " + day.getDayName() + " en modo solo lectura.");
                llDayLockedBanner.setOnClickListener(null);
                btnSaveDayAction.setEnabled(false);
                btnSaveDayAction.setAlpha(0.55f);
                btnSaveDayAction.setText("✓ KPI Semanal Completo (Solo lectura)");
            }
        } else {
            llDayLockedBanner.setVisibility(View.GONE);
            llDayLockedBanner.setOnClickListener(null);
            tvDayStatusBadge.setText("En edición");
            tvDayStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
            btnSaveDayAction.setEnabled(true);
            btnSaveDayAction.setAlpha(1.0f);
            btnSaveDayAction.setText(selectedDayIndex == 5
                    ? "Enviar KPI Semanal (Sábado)"
                    : "Enviar KPI de " + day.getDayName());
        }
    }

    /**
     * Inicia el proceso de guardado protegido por autenticación biométrica real (Huella o Facial).
     */
    private void initiateBiometricSave() {
        KpiSalesDay day = weekManager.getDay(selectedDayIndex);
        if (day.isSaved()) {
            jumpToNextPendingSalesDay();
            return;
        }

        KpiBiometricHelper.authenticate(this, day.getDayName(), new KpiBiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                saveCurrentDayData();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                if (errorCode == -1) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirmación de Cierre · Asesor de Ventas")
                            .setMessage("Sensor biométrico no enrolado en este equipo (" + errorMessage + "). ¿Deseas confirmar y guardar la ficha KPI de " + day.getDayName() + "?")
                            .setPositiveButton("Confirmar y Guardar", (d, w) -> saveCurrentDayData())
                            .setNegativeButton("Cancelar", null)
                            .show();
                } else {
                    Toast.makeText(getContext(), "Autenticación biométrica cancelada. Los datos siguen en borrador.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailed() {
                Toast.makeText(getContext(), "Biometría no reconocida. Intenta nuevamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCurrentDayData() {
        final int justSavedIndex = selectedDayIndex;
        KpiSalesDay day = weekManager.getDay(justSavedIndex);
        String obs = etDayObservaciones.getText().toString().trim();

        weekManager.saveDayData(
                justSavedIndex,
                day.getLlamadas(),
                day.getVisitas(),
                day.getCotizaciones(),
                day.getSeguimiento(),
                day.getProspectos(),
                day.getClientes(),
                obs
        );

        int compliancePct = weekManager.getOverallCompliancePercent();
        pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository.getInstance(requireContext())
                .saveRoleDailyEvaluation("Asesor de Ventas", day.getFullDateFormatted(), "KPI-VENTAS-01",
                        "Evaluación Comercial y Hoja de Ruta · " + day.getDayName(),
                        day.getVisitas() + day.getClientes(), 6.0, compliancePct,
                        day.getVisitas() > 0, obs);

        boolean isWeeklySaturdayOrComplete = (justSavedIndex == 5 || weekManager.isWeekComplete());
        if (isWeeklySaturdayOrComplete) {
            pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper
                    .playCollaboratorWeekendSuccess(requireContext());
        } else {
            pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper.playDailySuccess(requireContext());
        }

        int nextIdx = weekManager.getNextUnsavedDayIndexAfter(justSavedIndex);
        if (weekManager.isWeekComplete()) {
            updateDailyKpiDayTabs();
            updateDailyKpiViews();
            scrollDailyKpiToTop();
            showCelebrationDialog();
        } else if (justSavedIndex == 5) {
            KpiPandaSuccessDialog.show(requireContext(), "Asesor de Ventas", false,
                    "KPI Semanal · Sábado", compliancePct, Math.max(10, compliancePct), () -> {
                        if (nextIdx != -1) {
                            selectedDayIndex = nextIdx;
                            updateDailyKpiDayTabs();
                            updateDailyKpiViews();
                            scrollDailyKpiToTop();
                        }
                    });
        } else if (nextIdx != -1) {
            selectedDayIndex = nextIdx;
            updateDailyKpiDayTabs();
            updateDailyKpiViews();
            scrollDailyKpiToTop();
            showToastBanner("✓ " + day.getDayName() + " enviado · Ahora en " + weekManager.getDay(nextIdx).getDayName());
        }
    }

    private void showCelebrationDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.kpi_dialog_sales_celebration, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvCelebrationPercent = dialogView.findViewById(R.id.tvCelebrationPercent);
        tvCelebrationPercent.setText(weekManager.getOverallCompliancePercent() + "%");

        dialogView.findViewById(R.id.btnCelebrationViewSummary).setOnClickListener(v -> {
            dialog.dismiss();
            switchTab(TAB_REPORTS);
        });

        dialogView.findViewById(R.id.btnCelebrationKeepEditing).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================================
    // 2. LÓGICA DE HOJA DE RUTA COMERCIAL (MANUAL)
    // =========================================================================

    private void initRouteViews() {
        tvRouteStatTotal = viewRoute.findViewById(R.id.tvRouteStatTotal);
        tvRouteStatVisits = viewRoute.findViewById(R.id.tvRouteStatVisits);
        tvRouteStatCalls = viewRoute.findViewById(R.id.tvRouteStatCalls);
        tvRouteStatClosed = viewRoute.findViewById(R.id.tvRouteStatClosed);
        btnRouteAddNew = viewRoute.findViewById(R.id.btnRouteAddNew);
        llRouteCardsContainer = viewRoute.findViewById(R.id.llRouteCardsContainer);
        llRouteEmptyState = viewRoute.findViewById(R.id.llRouteEmptyState);
        tvEmptyStateExplanation = viewRoute.findViewById(R.id.tvEmptyStateExplanation);

        btnFilterAll = viewRoute.findViewById(R.id.btnFilterAll);
        btnFilterMon = viewRoute.findViewById(R.id.btnFilterMon);
        btnFilterTue = viewRoute.findViewById(R.id.btnFilterTue);
        btnFilterWed = viewRoute.findViewById(R.id.btnFilterWed);
        btnFilterThu = viewRoute.findViewById(R.id.btnFilterThu);
        btnFilterFri = viewRoute.findViewById(R.id.btnFilterFri);
        btnFilterSat = viewRoute.findViewById(R.id.btnFilterSat);

        setupRouteFilters();

        btnRouteAddNew.setOnClickListener(v -> showRouteEntryDialog(null));
    }

    private void setupRouteFilters() {
        View.OnClickListener listener = v -> {
            resetFilterButtons();
            TextView btn = (TextView) v;
            btn.setBackgroundResource(R.drawable.sales_bg_top_tab_active);
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));

            currentRouteFilter = btn.getText().toString();
            refreshRouteCards();
        };

        btnFilterAll.setOnClickListener(listener);
        btnFilterMon.setOnClickListener(listener);
        btnFilterTue.setOnClickListener(listener);
        btnFilterWed.setOnClickListener(listener);
        btnFilterThu.setOnClickListener(listener);
        btnFilterFri.setOnClickListener(listener);
        btnFilterSat.setOnClickListener(listener);
    }

    private void resetFilterButtons() {
        int colorInactive = ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary);
        TextView[] buttons = {btnFilterAll, btnFilterMon, btnFilterTue, btnFilterWed, btnFilterThu, btnFilterFri, btnFilterSat};
        for (TextView btn : buttons) {
            btn.setBackgroundResource(R.drawable.kpi_bg_day_tab_inactive);
            btn.setTextColor(colorInactive);
        }
    }

    private void refreshRouteData() {
        // Actualizar estadísticas globales de ruta
        List<KpiRouteItem> all = routeDbHelper.getAllItems();
        int total = all.size();
        int visits = 0;
        int calls = 0;
        int closed = 0;

        for (KpiRouteItem item : all) {
            String type = item.getActivityType() != null ? item.getActivityType().toLowerCase() : "";
            if (type.contains("visita")) visits++;
            else if (type.contains("llamada") || type.contains("seguimiento")) calls++;

            String res = item.getCommercialResult() != null ? item.getCommercialResult().toLowerCase() : "";
            if (res.contains("cerrad") || res.contains("venta")) closed++;
        }

        tvRouteStatTotal.setText(String.valueOf(total));
        tvRouteStatVisits.setText(String.valueOf(visits));
        tvRouteStatCalls.setText(String.valueOf(calls));
        tvRouteStatClosed.setText(String.valueOf(closed));

        refreshRouteCards();
    }

    private void refreshRouteCards() {
        llRouteCardsContainer.removeAllViews();
        List<KpiRouteItem> list = routeDbHelper.getItemsByDay(currentRouteFilter);

        if (list.isEmpty()) {
            llRouteEmptyState.setVisibility(View.VISIBLE);
            tvEmptyStateExplanation.setText("No hay actividades comerciales registradas para "
                    + (currentRouteFilter.equalsIgnoreCase("Todos") ? "ningún día" : currentRouteFilter)
                    + ".\nPresiona '+ Registrar Actividad de Ruta' para agregar una.");
            return;
        }

        llRouteEmptyState.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (KpiRouteItem item : list) {
            View card = inflater.inflate(R.layout.sales_item_route_card, llRouteCardsContainer, false);

            TextView tvRouteType = card.findViewById(R.id.tvRouteType);
            ImageView ivRouteTypeIcon = card.findViewById(R.id.ivRouteTypeIcon);
            TextView tvRouteTimeRange = card.findViewById(R.id.tvRouteTimeRange);
            TextView tvRouteBusinessName = card.findViewById(R.id.tvRouteBusinessName);
            TextView tvRouteContact = card.findViewById(R.id.tvRouteContact);
            TextView tvRoutePhone = card.findViewById(R.id.tvRoutePhone);
            TextView tvRouteManualAddress = card.findViewById(R.id.tvRouteManualAddress);
            LinearLayout llRouteAddressContainer = card.findViewById(R.id.llRouteAddressContainer);
            TextView tvRouteResult = card.findViewById(R.id.tvRouteResult);
            TextView tvRouteDateBadge = card.findViewById(R.id.tvRouteDateBadge);
            TextView tvRouteObservations = card.findViewById(R.id.tvRouteObservations);
            ImageButton btnRouteEdit = card.findViewById(R.id.btnRouteEdit);
            ImageButton btnRouteDelete = card.findViewById(R.id.btnRouteDelete);

            tvRouteType.setText(item.getActivityType());
            if (item.getActivityType().contains("Llamada")) {
                ivRouteTypeIcon.setImageResource(R.drawable.kpi_ic_phone);
            } else if (item.getActivityType().contains("Seguimiento")) {
                ivRouteTypeIcon.setImageResource(R.drawable.kpi_ic_link);
            } else {
                ivRouteTypeIcon.setImageResource(R.drawable.kpi_ic_walk);
            }

            String start = item.getStartTime() != null && !item.getStartTime().isEmpty() ? item.getStartTime() : "--:--";
            String end = item.getEndTime() != null && !item.getEndTime().isEmpty() ? item.getEndTime() : "--:--";
            tvRouteTimeRange.setText(start + " - " + end);

            tvRouteBusinessName.setText(item.getBusinessName());
            tvRouteContact.setText(item.getContactPerson() != null && !item.getContactPerson().isEmpty() ? item.getContactPerson() : "Sin contacto");
            tvRoutePhone.setText(item.getPhone() != null && !item.getPhone().isEmpty() ? item.getPhone() : "Sin teléfono");

            if (item.getManualAddress() != null && !item.getManualAddress().trim().isEmpty()) {
                llRouteAddressContainer.setVisibility(View.VISIBLE);
                tvRouteManualAddress.setText(item.getManualAddress());
            } else {
                llRouteAddressContainer.setVisibility(View.GONE);
            }

            tvRouteResult.setText(item.getCommercialResult());
            tvRouteDateBadge.setText(item.getDayName() != null ? item.getDayName() : "");

            if (item.getObservations() != null && !item.getObservations().trim().isEmpty()) {
                tvRouteObservations.setVisibility(View.VISIBLE);
                tvRouteObservations.setText("❝ " + item.getObservations() + " ❞");
            } else {
                tvRouteObservations.setVisibility(View.GONE);
            }

            btnRouteEdit.setOnClickListener(v -> showRouteEntryDialog(item));
            btnRouteDelete.setOnClickListener(v -> showDeleteRouteConfirm(item));

            llRouteCardsContainer.addView(card);
        }
    }

    private void showDeleteRouteConfirm(KpiRouteItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Eliminar actividad?")
                .setMessage("¿Deseas eliminar la actividad en \"" + item.getBusinessName() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    routeDbHelper.deleteItem(item.getId());
                    refreshRouteData();
                    showToastBanner("Actividad eliminada");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showRouteEntryDialog(@Nullable KpiRouteItem itemToEdit) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.sales_dialog_route_entry, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        Spinner spActivityType = dialogView.findViewById(R.id.spActivityType);
        Spinner spDayName = dialogView.findViewById(R.id.spDayName);
        EditText etBusinessName = dialogView.findViewById(R.id.etBusinessName);
        EditText etContactPerson = dialogView.findViewById(R.id.etContactPerson);
        EditText etPhone = dialogView.findViewById(R.id.etPhone);
        EditText etManualAddress = dialogView.findViewById(R.id.etManualAddress);
        EditText etStartTime = dialogView.findViewById(R.id.etStartTime);
        EditText etEndTime = dialogView.findViewById(R.id.etEndTime);
        Spinner spCommercialResult = dialogView.findViewById(R.id.spCommercialResult);
        EditText etRouteObservations = dialogView.findViewById(R.id.etRouteObservations);
        Button btnCancelDialog = dialogView.findViewById(R.id.btnCancelDialog);
        Button btnSaveRouteDialog = dialogView.findViewById(R.id.btnSaveRouteDialog);
        ImageButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        // Opciones de tipo de actividad
        String[] types = {"Visita presencial", "Llamada telefónica", "Seguimiento comercial"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spActivityType.setAdapter(typeAdapter);

        // Opciones de día
        String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, days);
        spDayName.setAdapter(dayAdapter);

        // Opciones de resultado comercial
        String[] results = {"Interesado", "Venta Cerrada", "En Seguimiento", "No Interesado", "Reprogramado"};
        ArrayAdapter<String> resultAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, results);
        spCommercialResult.setAdapter(resultAdapter);

        // Configurar selectores de hora
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        // Precarga en caso de edición
        boolean isEdit = (itemToEdit != null);
        if (isEdit) {
            tvDialogTitle.setText("Editar Actividad");
            btnSaveRouteDialog.setText("Actualizar");

            for (int i = 0; i < types.length; i++) {
                if (types[i].equalsIgnoreCase(itemToEdit.getActivityType())) spActivityType.setSelection(i);
            }
            for (int i = 0; i < days.length; i++) {
                if (days[i].equalsIgnoreCase(itemToEdit.getDayName())) spDayName.setSelection(i);
            }
            for (int i = 0; i < results.length; i++) {
                if (results[i].equalsIgnoreCase(itemToEdit.getCommercialResult())) spCommercialResult.setSelection(i);
            }

            etBusinessName.setText(itemToEdit.getBusinessName());
            etContactPerson.setText(itemToEdit.getContactPerson());
            etPhone.setText(itemToEdit.getPhone());
            etManualAddress.setText(itemToEdit.getManualAddress());
            etStartTime.setText(itemToEdit.getStartTime());
            etEndTime.setText(itemToEdit.getEndTime());
            etRouteObservations.setText(itemToEdit.getObservations());
        } else {
            // Asignar el día actual por defecto (horario de entrada 8:00 AM)
            spDayName.setSelection(Math.min(selectedDayIndex, 5));
            etStartTime.setText("08:00 AM");
            etEndTime.setText("08:45 AM");
        }

        btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        btnCancelDialog.setOnClickListener(v -> dialog.dismiss());

        btnSaveRouteDialog.setOnClickListener(v -> {
            String business = etBusinessName.getText().toString().trim();
            if (business.isEmpty()) {
                etBusinessName.setError("Escribe el nombre del negocio");
                etBusinessName.requestFocus();
                return;
            }

            KpiRouteItem item = isEdit ? itemToEdit : new KpiRouteItem();
            item.setActivityType(spActivityType.getSelectedItem().toString());
            item.setDayName(spDayName.getSelectedItem().toString());
            item.setBusinessName(business);
            item.setContactPerson(etContactPerson.getText().toString().trim());
            item.setPhone(etPhone.getText().toString().trim());
            item.setManualAddress(etManualAddress.getText().toString().trim());
            item.setStartTime(etStartTime.getText().toString().trim());
            item.setEndTime(etEndTime.getText().toString().trim());
            item.setCommercialResult(spCommercialResult.getSelectedItem().toString());
            item.setObservations(etRouteObservations.getText().toString().trim());

            if (isEdit) {
                routeDbHelper.updateItem(item);
                showToastBanner("Actividad actualizada ✓");
            } else {
                routeDbHelper.insertItem(item);
                showToastBanner("Actividad registrada en Hoja de Ruta ✓");
            }

            dialog.dismiss();
            refreshRouteData();
        });

        dialog.show();
    }

    private void showTimePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
            String amPm = hourOfDay >= 12 ? "PM" : "AM";
            int hour12 = hourOfDay % 12;
            if (hour12 == 0) hour12 = 12;
            target.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minuteOfHour, amPm));
        }, hour, minute, false);
        timePickerDialog.show();
    }

    // =========================================================================
    // 3. LÓGICA DE REPORTES (PANTALLA ÚNICA CON 2 BLOQUES: KPI Y HOJA DE RUTA)
    // =========================================================================

    private void initReportsViews() {
        llReportKpiLocked = viewReports.findViewById(R.id.llReportKpiLocked);
        llReportKpiUnlocked = viewReports.findViewById(R.id.llReportKpiUnlocked);
        tvReportKpiLockedDays = viewReports.findViewById(R.id.tvReportKpiLockedDays);
        tvReportKpiGlobalPercent = viewReports.findViewById(R.id.tvReportKpiGlobalPercent);
        llReportKpiIndicatorsTable = viewReports.findViewById(R.id.llReportKpiIndicatorsTable);
        llReportKpiDailyBreakdown = viewReports.findViewById(R.id.llReportKpiDailyBreakdown);
        btnReportGoToKpi = viewReports.findViewById(R.id.btnReportGoToKpi);
        btnReportCopyKpi = viewReports.findViewById(R.id.btnReportCopyKpi);
        btnReportResetWeek = viewReports.findViewById(R.id.btnReportResetWeek);

        tvReportRouteCountTitle = viewReports.findViewById(R.id.tvReportRouteCountTitle);
        btnReportCopyRoute = viewReports.findViewById(R.id.btnReportCopyRoute);
        llReportRouteItemsContainer = viewReports.findViewById(R.id.llReportRouteItemsContainer);
        llReportRouteEmptyState = viewReports.findViewById(R.id.llReportRouteEmptyState);

        btnReportGoToKpi.setOnClickListener(v -> switchTab(TAB_DAILY_KPI));
        btnReportCopyKpi.setOnClickListener(v -> copySummaryToClipboard());
        btnReportResetWeek.setOnClickListener(v -> showResetConfirmationDialog());
        btnReportCopyRoute.setOnClickListener(v -> copyRouteReportToClipboard());
    }

    private void refreshReportsData() {
        // --- BLOQUE 1: KPI ---
        boolean isKpiComplete = weekManager.isWeekComplete();
        int savedCount = weekManager.getSavedDaysCount();

        if (!isKpiComplete) {
            llReportKpiLocked.setVisibility(View.VISIBLE);
            llReportKpiUnlocked.setVisibility(View.GONE);
            tvReportKpiLockedDays.setText("Has completado " + savedCount + " de 6 días de la semana.\nEl consolidado semanal de KPI se desbloquea al guardar los 6 días.");
        } else {
            llReportKpiLocked.setVisibility(View.GONE);
            llReportKpiUnlocked.setVisibility(View.VISIBLE);

            int percent = weekManager.getOverallCompliancePercent();
            tvReportKpiGlobalPercent.setText(percent + "%");

            // Tabla 6 Indicadores
            llReportKpiIndicatorsTable.removeAllViews();
            addReportKpiIndicatorRow("Llamadas comerciales", weekManager.getTotalLlamadas(), KpiSalesWeekManager.GOAL_LLAMADAS);
            addReportKpiIndicatorRow("Nuevas visitas a empresas", weekManager.getTotalVisitas(), KpiSalesWeekManager.GOAL_VISITAS);
            addReportKpiIndicatorRow("Cotizaciones enviadas", weekManager.getTotalCotizaciones(), KpiSalesWeekManager.GOAL_COTIZACIONES);
            addReportKpiIndicatorRow("Empresas en seguimiento", weekManager.getTotalSeguimiento(), KpiSalesWeekManager.GOAL_SEGUIMIENTO);
            addReportKpiIndicatorRow("Nuevos prospectos", weekManager.getTotalProspectos(), KpiSalesWeekManager.GOAL_PROSPECTOS);
            addReportKpiIndicatorRow("Nuevos clientes cerrados", weekManager.getTotalClientes(), KpiSalesWeekManager.GOAL_CLIENTES);

            // Desglose Diario Lun a Sáb
            llReportKpiDailyBreakdown.removeAllViews();
            List<KpiSalesDay> days = weekManager.getDays();
            for (KpiSalesDay d : days) {
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(0, 10, 0, 10);

                TextView tvDayTitle = new TextView(getContext());
                tvDayTitle.setText(d.getDayName() + " " + d.getDayNumber() + "/10 · " + d.getTotalActividadesDia() + " actividades");
                tvDayTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvDayTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
                tvDayTitle.setTextSize(13);

                TextView tvDayDet = new TextView(getContext());
                String det = String.format(Locale.getDefault(),
                        "%d llamadas · %d visitas · %d cotiz. · %d seg. · %d prosp. · %d clientes",
                        d.getLlamadas(), d.getVisitas(), d.getCotizaciones(),
                        d.getSeguimiento(), d.getProspectos(), d.getClientes());
                tvDayDet.setText(det);
                tvDayDet.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
                tvDayDet.setTextSize(11);

                row.addView(tvDayTitle);
                row.addView(tvDayDet);

                if (!d.getObservaciones().isEmpty()) {
                    TextView tvObs = new TextView(getContext());
                    tvObs.setText("❝ " + d.getObservaciones() + " ❞");
                    tvObs.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
                    tvObs.setTextSize(10.5f);
                    tvObs.setPadding(0, 4, 0, 0);
                    row.addView(tvObs);
                }

                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_border));
                divider.setPadding(0, 8, 0, 0);

                llReportKpiDailyBreakdown.addView(row);
                llReportKpiDailyBreakdown.addView(divider);
            }
        }

        // --- BLOQUE 2: HOJA DE RUTA ---
        List<KpiRouteItem> routeList = routeDbHelper.getAllItems();
        tvReportRouteCountTitle.setText(routeList.size() + " Actividades Comerciales Registradas");
        llReportRouteItemsContainer.removeAllViews();

        if (routeList.isEmpty()) {
            llReportRouteEmptyState.setVisibility(View.VISIBLE);
        } else {
            llReportRouteEmptyState.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (KpiRouteItem item : routeList) {
                View card = inflater.inflate(R.layout.sales_item_route_card, llReportRouteItemsContainer, false);

                TextView tvRouteType = card.findViewById(R.id.tvRouteType);
                ImageView ivRouteTypeIcon = card.findViewById(R.id.ivRouteTypeIcon);
                TextView tvRouteTimeRange = card.findViewById(R.id.tvRouteTimeRange);
                TextView tvRouteBusinessName = card.findViewById(R.id.tvRouteBusinessName);
                TextView tvRouteContact = card.findViewById(R.id.tvRouteContact);
                TextView tvRoutePhone = card.findViewById(R.id.tvRoutePhone);
                TextView tvRouteManualAddress = card.findViewById(R.id.tvRouteManualAddress);
                LinearLayout llRouteAddressContainer = card.findViewById(R.id.llRouteAddressContainer);
                TextView tvRouteResult = card.findViewById(R.id.tvRouteResult);
                TextView tvRouteDateBadge = card.findViewById(R.id.tvRouteDateBadge);
                TextView tvRouteObservations = card.findViewById(R.id.tvRouteObservations);
                ImageButton btnRouteEdit = card.findViewById(R.id.btnRouteEdit);
                ImageButton btnRouteDelete = card.findViewById(R.id.btnRouteDelete);

                // En el reporte ocultamos los botones de editar/borrar para vista ejecutiva
                btnRouteEdit.setVisibility(View.GONE);
                btnRouteDelete.setVisibility(View.GONE);

                tvRouteType.setText(item.getActivityType());
                if (item.getActivityType().contains("Llamada")) {
                    ivRouteTypeIcon.setImageResource(R.drawable.kpi_ic_phone);
                } else if (item.getActivityType().contains("Seguimiento")) {
                    ivRouteTypeIcon.setImageResource(R.drawable.kpi_ic_link);
                } else {
                    ivRouteTypeIcon.setImageResource(R.drawable.kpi_ic_walk);
                }

                String start = item.getStartTime() != null ? item.getStartTime() : "--:--";
                String end = item.getEndTime() != null ? item.getEndTime() : "--:--";
                tvRouteTimeRange.setText(start + " - " + end);

                tvRouteBusinessName.setText(item.getBusinessName());
                tvRouteContact.setText(item.getContactPerson() != null && !item.getContactPerson().isEmpty() ? item.getContactPerson() : "Sin contacto");
                tvRoutePhone.setText(item.getPhone() != null && !item.getPhone().isEmpty() ? item.getPhone() : "Sin teléfono");

                if (item.getManualAddress() != null && !item.getManualAddress().trim().isEmpty()) {
                    llRouteAddressContainer.setVisibility(View.VISIBLE);
                    tvRouteManualAddress.setText(item.getManualAddress());
                } else {
                    llRouteAddressContainer.setVisibility(View.GONE);
                }

                tvRouteResult.setText(item.getCommercialResult());
                tvRouteDateBadge.setText(item.getDayName() != null ? item.getDayName() : "");

                if (item.getObservations() != null && !item.getObservations().trim().isEmpty()) {
                    tvRouteObservations.setVisibility(View.VISIBLE);
                    tvRouteObservations.setText("❝ " + item.getObservations() + " ❞");
                } else {
                    tvRouteObservations.setVisibility(View.GONE);
                }

                llReportRouteItemsContainer.addView(card);
            }
        }
    }

    private void addReportKpiIndicatorRow(String label, int total, int goal) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 10, 0, 10);

        TextView tvLabel = new TextView(getContext());
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tvLabel.setTextSize(12.5f);

        int p = Math.min(150, (total * 100) / goal);
        TextView tvRatio = new TextView(getContext());
        tvRatio.setText(total + " / " + goal + " (" + p + "%)");
        tvRatio.setTypeface(null, android.graphics.Typeface.BOLD);
        tvRatio.setTextColor(ContextCompat.getColor(requireContext(), p >= 100 ? R.color.kpi_secondary : R.color.kpi_primary));
        tvRatio.setTextSize(12.5f);

        row.addView(tvLabel);
        row.addView(tvRatio);
        llReportKpiIndicatorsTable.addView(row);

        View div = new View(getContext());
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_border));
        llReportKpiIndicatorsTable.addView(div);
    }

    private void copySummaryToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Resumen Semanal KPI", weekManager.generateClipboardSummary());
            clipboard.setPrimaryClip(clip);
            showToastBanner("Informe KPI copiado al portapapeles");
            Toast.makeText(getContext(), "✓ Informe KPI copiado al portapapeles", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyRouteReportToClipboard() {
        List<KpiRouteItem> list = routeDbHelper.getAllItems();
        StringBuilder sb = new StringBuilder();
        sb.append("HOJA DE RUTA COMERCIAL — WEBSITE+\n");
        sb.append("Total Actividades: ").append(list.size()).append("\n\n");

        for (KpiRouteItem item : list) {
            sb.append("• [").append(item.getDayName()).append("] ").append(item.getActivityType()).append("\n");
            sb.append("  Negocio: ").append(item.getBusinessName()).append("\n");
            if (item.getContactPerson() != null && !item.getContactPerson().isEmpty()) {
                sb.append("  Contacto: ").append(item.getContactPerson()).append(" (").append(item.getPhone()).append(")\n");
            }
            if (item.getManualAddress() != null && !item.getManualAddress().isEmpty()) {
                sb.append("  Dirección: ").append(item.getManualAddress()).append("\n");
            }
            sb.append("  Horario: ").append(item.getStartTime()).append(" - ").append(item.getEndTime()).append("\n");
            sb.append("  Resultado: ").append(item.getCommercialResult()).append("\n");
            if (item.getObservations() != null && !item.getObservations().isEmpty()) {
                sb.append("  Obs: ").append(item.getObservations()).append("\n");
            }
            sb.append("\n");
        }

        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Hoja de Ruta Comercial", sb.toString());
            clipboard.setPrimaryClip(clip);
            showToastBanner("Hoja de Ruta copiada al portapapeles");
            Toast.makeText(getContext(), "✓ Hoja de Ruta copiada al portapapeles", Toast.LENGTH_SHORT).show();
        }
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Reiniciar la semana?")
                .setMessage("Se borrarán los avances registrados de los 6 días de KPI para que puedas volver a probar desde cero.")
                .setPositiveButton("Sí, reiniciar", (dialog, which) -> {
                    weekManager.resetWeek();
                    selectedDayIndex = weekManager.getTodayDayIndex();
                    switchTab(TAB_DAILY_KPI);
                    scrollDailyKpiToTop();
                    showToastBanner("Semana reiniciada");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showToastBanner(String message) {
        tvToastMessage.setText(message);
        llFloatingToast.setVisibility(View.VISIBLE);
        llFloatingToast.setAlpha(0f);
        llFloatingToast.animate().alpha(1f).setDuration(250).withEndAction(() -> {
            llFloatingToast.postDelayed(() -> {
                if (isAdded()) {
                    llFloatingToast.animate().alpha(0f).setDuration(250).withEndAction(() -> {
                        llFloatingToast.setVisibility(View.GONE);
                    }).start();
                }
            }, 2500);
        }).start();
    }
}
