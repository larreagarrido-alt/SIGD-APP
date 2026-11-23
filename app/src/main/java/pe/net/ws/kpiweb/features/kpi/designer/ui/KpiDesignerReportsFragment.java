package pe.net.ws.kpiweb.features.kpi.designer.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerHistorySummary;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;
import pe.net.ws.kpiweb.features.kpi.designer.ui.adapter.KpiDesignerHistoryAdapter;
import pe.net.ws.kpiweb.features.kpi.designer.ui.view.KpiDesignerDailyBarChartView;
import pe.net.ws.kpiweb.features.kpi.designer.ui.view.KpiDesignerEvolutionChartView;

/**
 * Fragmento de Reportes para el Diseñador Gráfico.
 * Contiene exclusivamente 2 pestañas:
 * 1. Semanal: Progreso de la semana actual con burbujas, resumen y gráfico de barras diarias.
 * 2. Historial: Estadísticas consolidadas, gráfico de evolución y listado de reportes históricos.
 */
public class KpiDesignerReportsFragment extends Fragment {

    private LinearLayout tabDesignerWeekly;
    private ImageView ivTabWeekly;
    private TextView tvTabWeekly;

    private LinearLayout tabDesignerHistory;
    private ImageView ivTabHistory;
    private TextView tvTabHistory;

    // Contenedores de vistas
    private View containerDesignerWeeklyTab;
    private View containerDesignerHistoryTab;

    // Elementos de la Pestaña Semanal
    private TextView tvCurrentWeekSelector;
    private TextView tvDayBubbleLun;
    private TextView tvDayBubbleMar;
    private TextView tvDayBubbleMie;
    private TextView tvDayBubbleJue;
    private TextView tvDayBubbleVie;
    private TextView tvDayBubbleSab;

    private TextView tvWeeklyProgressPercent;
    private TextView tvWeeklyActivitiesRatio;
    private TextView tvWeeklyLevelBadge;
    private TextView tvWeeklyLevelDesc;

    private TextView tvResumenDesigns;
    private TextView tvResumenVideos;
    private TextView tvResumenPhotos;
    private TextView tvResumenTotal;

    private KpiDesignerDailyBarChartView chartWeeklyDailyProduction;
    private View btnViewWeeklyReportDetail;

    // Elementos de la Pestaña Historial
    private TextView tvHistoryStatDesigns;
    private TextView tvHistoryStatVideos;
    private TextView tvHistoryStatPhotos;
    private TextView tvHistoryStatCompliance;
    private KpiDesignerEvolutionChartView chartHistoryEvolution;
    private RecyclerView rvHistoryReports;
    private KpiDesignerHistoryAdapter historyAdapter;

    private int activeTab = 0; // 0 = Semanal, 1 = Historial

    public static KpiDesignerReportsFragment newInstance() {
        return new KpiDesignerReportsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_designer_fragment_reports, container, false);
        initViews(root);
        setupTabs();
        setupHistoryRecyclerView();

        populateWeeklyTab();
        populateHistoryTab();

        switchTab(0);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (activeTab == 0) {
            populateWeeklyTab();
        } else {
            populateHistoryTab();
        }
    }

    private void initViews(View root) {
        tabDesignerWeekly = root.findViewById(R.id.tabDesignerWeekly);
        ivTabWeekly = root.findViewById(R.id.ivTabWeekly);
        tvTabWeekly = root.findViewById(R.id.tvTabWeekly);

        tabDesignerHistory = root.findViewById(R.id.tabDesignerHistory);
        ivTabHistory = root.findViewById(R.id.ivTabHistory);
        tvTabHistory = root.findViewById(R.id.tvTabHistory);

        containerDesignerWeeklyTab = root.findViewById(R.id.containerDesignerWeeklyTab);
        containerDesignerHistoryTab = root.findViewById(R.id.containerDesignerHistoryTab);

        // Semanal
        tvCurrentWeekSelector = root.findViewById(R.id.tvCurrentWeekSelector);
        tvDayBubbleLun = root.findViewById(R.id.tvDayBubbleLun);
        tvDayBubbleMar = root.findViewById(R.id.tvDayBubbleMar);
        tvDayBubbleMie = root.findViewById(R.id.tvDayBubbleMie);
        tvDayBubbleJue = root.findViewById(R.id.tvDayBubbleJue);
        tvDayBubbleVie = root.findViewById(R.id.tvDayBubbleVie);
        tvDayBubbleSab = root.findViewById(R.id.tvDayBubbleSab);

        tvWeeklyProgressPercent = root.findViewById(R.id.tvWeeklyProgressPercent);
        tvWeeklyActivitiesRatio = root.findViewById(R.id.tvWeeklyActivitiesRatio);
        tvWeeklyLevelBadge = root.findViewById(R.id.tvWeeklyLevelBadge);
        tvWeeklyLevelDesc = root.findViewById(R.id.tvWeeklyLevelDesc);

        tvResumenDesigns = root.findViewById(R.id.tvResumenDesigns);
        tvResumenVideos = root.findViewById(R.id.tvResumenVideos);
        tvResumenPhotos = root.findViewById(R.id.tvResumenPhotos);
        tvResumenTotal = root.findViewById(R.id.tvResumenTotal);

        chartWeeklyDailyProduction = root.findViewById(R.id.chartWeeklyDailyProduction);
        btnViewWeeklyReportDetail = root.findViewById(R.id.btnViewWeeklyReportDetail);
        if (btnViewWeeklyReportDetail != null) {
            btnViewWeeklyReportDetail.setOnClickListener(v -> {
                KpiDesignerWeekReport cur = KpiDesignerMemoryRepository.getInstance().getCurrentWeekReport();
                if (cur != null) {
                    openWeekReportDetail(cur.getWeekId());
                }
            });
        }

        // Historial
        tvHistoryStatDesigns = root.findViewById(R.id.tvHistoryStatDesigns);
        tvHistoryStatVideos = root.findViewById(R.id.tvHistoryStatVideos);
        tvHistoryStatPhotos = root.findViewById(R.id.tvHistoryStatPhotos);
        tvHistoryStatCompliance = root.findViewById(R.id.tvHistoryStatCompliance);
        chartHistoryEvolution = root.findViewById(R.id.chartHistoryEvolution);
        rvHistoryReports = root.findViewById(R.id.rvHistoryReports);
    }

    private void setupTabs() {
        tabDesignerWeekly.setOnClickListener(v -> switchTab(0));
        tabDesignerHistory.setOnClickListener(v -> switchTab(1));
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.kpi_primary);

        if (tab == 0) {
            // Activar Semanal
            tabDesignerWeekly.setBackgroundResource(R.drawable.kpi_bg_tab_active_purple);
            tvTabWeekly.setTextColor(Color.WHITE);
            ivTabWeekly.setColorFilter(Color.WHITE);

            tabDesignerHistory.setBackgroundResource(android.R.color.transparent);
            tvTabHistory.setTextColor(primaryColor);
            ivTabHistory.setColorFilter(primaryColor);

            containerDesignerWeeklyTab.setVisibility(View.VISIBLE);
            containerDesignerHistoryTab.setVisibility(View.GONE);

            populateWeeklyTab();
        } else {
            // Activar Historial
            tabDesignerHistory.setBackgroundResource(R.drawable.kpi_bg_tab_active_purple);
            tvTabHistory.setTextColor(Color.WHITE);
            ivTabHistory.setColorFilter(Color.WHITE);

            tabDesignerWeekly.setBackgroundResource(android.R.color.transparent);
            tvTabWeekly.setTextColor(primaryColor);
            ivTabWeekly.setColorFilter(primaryColor);

            containerDesignerWeeklyTab.setVisibility(View.GONE);
            containerDesignerHistoryTab.setVisibility(View.VISIBLE);

            populateHistoryTab();
        }
    }

    private void populateWeeklyTab() {
        KpiDesignerWeekReport report = KpiDesignerMemoryRepository.getInstance().getCurrentWeekReport();
        if (report == null) return;

        tvCurrentWeekSelector.setText(report.getDateRangeFull());

        // Actualizar burbujas
        List<KpiDesignerDayRecord> days = report.getDays();
        boolean hasLun = false, hasMar = false, hasMie = false, hasJue = false, hasVie = false, hasSab = false;

        for (KpiDesignerDayRecord d : days) {
            String day = d.getDayOfWeek();
            if ("LUNES".equalsIgnoreCase(day)) hasLun = true;
            else if ("MARTES".equalsIgnoreCase(day)) hasMar = true;
            else if ("MIÉRCOLES".equalsIgnoreCase(day)) hasMie = true;
            else if ("JUEVES".equalsIgnoreCase(day)) hasJue = true;
            else if ("VIERNES".equalsIgnoreCase(day)) hasVie = true;
            else if ("SÁBADO".equalsIgnoreCase(day)) hasSab = true;
        }

        setBubbleStyle(tvDayBubbleLun, hasLun);
        setBubbleStyle(tvDayBubbleMar, hasMar);
        setBubbleStyle(tvDayBubbleMie, hasMie);
        setBubbleStyle(tvDayBubbleJue, hasJue);
        setBubbleStyle(tvDayBubbleVie, hasVie);
        setBubbleStyle(tvDayBubbleSab, hasSab);

        // Métricas
        tvWeeklyProgressPercent.setText(report.getTotalCompliancePercentage() + "%");
        tvWeeklyActivitiesRatio.setText(report.getTotalActivities() + " de " + KpiDesignerWeekReport.WEEKLY_GOAL_TOTAL + " actividades");
        tvWeeklyLevelBadge.setText("★ " + report.getKpiLevel());
        tvWeeklyLevelDesc.setText(report.getKpiLevelDescription());

        // Resumen
        tvResumenDesigns.setText(report.getTotalDesigns() + " / " + KpiDesignerWeekReport.WEEKLY_GOAL_DESIGNS + "   " + report.getDesignsPercentage() + "%");
        tvResumenVideos.setText(report.getTotalVideos() + " / " + KpiDesignerWeekReport.WEEKLY_GOAL_VIDEOS + "   " + report.getVideosPercentage() + "%");
        tvResumenPhotos.setText(report.getTotalPhotos() + " / " + KpiDesignerWeekReport.WEEKLY_GOAL_PHOTOS + "   " + report.getPhotosPercentage() + "%");
        tvResumenTotal.setText(report.getTotalActivities() + " / " + KpiDesignerWeekReport.WEEKLY_GOAL_TOTAL + "   " + report.getTotalCompliancePercentage() + "%");

        // Gráfico de barras
        chartWeeklyDailyProduction.setDaysFromRecords(days);
    }

    private void setBubbleStyle(TextView view, boolean completed) {
        if (completed) {
            view.setText("✓");
            view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_status_entregado_bg));
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_status_entregado));
        } else {
            view.setText("○");
            view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_surface_variant));
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        }
    }

    private void setupHistoryRecyclerView() {
        rvHistoryReports.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new KpiDesignerHistoryAdapter(report -> {
            // Abrir el detalle completo de la semana seleccionada (Semana 5, Semana 4, etc.)
            openWeekReportDetail(report.getWeekId());
        });
        rvHistoryReports.setAdapter(historyAdapter);
    }

    private void populateHistoryTab() {
        KpiDesignerHistorySummary summary = KpiDesignerMemoryRepository.getInstance().getHistorySummary();
        if (summary != null) {
            tvHistoryStatDesigns.setText(String.valueOf(summary.getTotalDesigns()));
            tvHistoryStatVideos.setText(String.valueOf(summary.getTotalVideos()));
            tvHistoryStatPhotos.setText(String.valueOf(summary.getTotalPhotos()));
            tvHistoryStatCompliance.setText(summary.getAverageCompliance() + "%");
            chartHistoryEvolution.setDataPoints(summary.getEvolutionPoints());
        }

        List<KpiDesignerWeekReport> list = KpiDesignerMemoryRepository.getInstance().getHistoryList();
        historyAdapter.setItems(list);
    }

    private void openWeekReportDetail(String weekId) {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.flDesignerContent, KpiDesignerReportDetailFragment.newInstance(weekId))
                    .addToBackStack("history_detail")
                    .commit();
        }
    }
}
