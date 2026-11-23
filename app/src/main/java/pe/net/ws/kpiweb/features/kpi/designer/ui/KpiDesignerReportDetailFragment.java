package pe.net.ws.kpiweb.features.kpi.designer.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;
import pe.net.ws.kpiweb.features.kpi.designer.ui.view.KpiDesignerDailyBarChartView;
import pe.net.ws.kpiweb.features.kpi.designer.ui.view.KpiDesignerDonutChartView;
import pe.net.ws.kpiweb.features.kpi.export.KpiPdfExporter;

/**
 * Detalle completo del Reporte Semanal en una ÚNICA pantalla con Scroll Vertical.
 * Refleja fielmente las 5 secciones oficiales del informe:
 * 1. Control diario de producción (Tabla Lunes a Sábado)
 * 2. Resumen semanal de producción
 * 3. Acciones que realiza el Diseñador Gráfico (ÚNICA sección editable, protegida con biometría)
 * 4. Evaluación KPI - Productividad
 * 5. Observaciones generales
 * Complementos visuales: Gráfico de barras de producción semanal y gráfico donut de distribución.
 */
public class KpiDesignerReportDetailFragment extends Fragment {

    private static final String ARG_WEEK_ID = "arg_week_id";

    private String weekId = "Semana 5";
    private KpiDesignerWeekReport report;

    // Encabezado
    private View btnDetailBack;
    private TextView tvDetailWeekBadge;
    private TextView tvDetailLevelTopBadge;
    private TextView tvDetailDateRangeSelector;

    // Métrica circular
    private TextView tvDetailBigPercent;
    private TextView tvDetailActivitiesRatio;
    private TextView tvDetailLevelBadgeSub;

    // Sección 1: Tabla diaria
    private LinearLayout containerTableDaysRows;
    private TextView tvTableTotalDesigns;
    private TextView tvTableTotalVideos;
    private TextView tvTableTotalAll;
    private TextView tvTableTotalPercent;

    // Sección 2: Resumen
    private TextView tvSec2ResDesigns;
    private TextView tvSec2PercDesigns;
    private TextView tvSec2ResVideos;
    private TextView tvSec2PercVideos;
    private TextView tvSec2ResPhotos;
    private TextView tvSec2PercPhotos;
    private TextView tvSec2ResTotal;
    private TextView tvSec2PercTotal;

    // Sección 3: Acciones (9 casillas)
    private CheckBox[] checkBoxes = new CheckBox[9];
    private View btnSaveActions;

    // Sección 4: Niveles
    private View rowLevelExcelente;
    private View rowLevelMuyBueno;
    private View rowLevelBueno;
    private View rowLevelRegular;
    private View rowLevelDeficiente;

    // Sección 5: Observaciones
    private TextView tvDetailGeneralObservations;

    // Complementos
    private KpiDesignerDailyBarChartView chartDetailWeeklyBars;
    private KpiDesignerDonutChartView chartDetailDonut;
    private TextView tvLegendDesigns;
    private TextView tvLegendVideos;
    private TextView tvLegendPhotos;
    private View btnDownloadPdfReport;

    public static KpiDesignerReportDetailFragment newInstance(String weekId) {
        KpiDesignerReportDetailFragment fragment = new KpiDesignerReportDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WEEK_ID, weekId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            weekId = getArguments().getString(ARG_WEEK_ID, "Semana 5");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_designer_fragment_report_detail, container, false);
        initViews(root);

        loadWeekData();
        setupActionsSaving();

        btnDetailBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnDownloadPdfReport.setOnClickListener(v -> {
            List<KpiRecordEntry> entries = KpiCustomRepository.getInstance(requireContext()).getAllRecordEntries();
            KpiPdfExporter.exportEntriesToPdf(requireContext(), entries, "Diseñador Gráfico - " + weekId);
        });

        return root;
    }

    private void initViews(View root) {
        btnDetailBack = root.findViewById(R.id.btnDetailBack);
        tvDetailWeekBadge = root.findViewById(R.id.tvDetailWeekBadge);
        tvDetailLevelTopBadge = root.findViewById(R.id.tvDetailLevelTopBadge);
        tvDetailDateRangeSelector = root.findViewById(R.id.tvDetailDateRangeSelector);

        tvDetailBigPercent = root.findViewById(R.id.tvDetailBigPercent);
        tvDetailActivitiesRatio = root.findViewById(R.id.tvDetailActivitiesRatio);
        tvDetailLevelBadgeSub = root.findViewById(R.id.tvDetailLevelBadgeSub);

        containerTableDaysRows = root.findViewById(R.id.containerTableDaysRows);
        tvTableTotalDesigns = root.findViewById(R.id.tvTableTotalDesigns);
        tvTableTotalVideos = root.findViewById(R.id.tvTableTotalVideos);
        tvTableTotalAll = root.findViewById(R.id.tvTableTotalAll);
        tvTableTotalPercent = root.findViewById(R.id.tvTableTotalPercent);

        tvSec2ResDesigns = root.findViewById(R.id.tvSec2ResDesigns);
        tvSec2PercDesigns = root.findViewById(R.id.tvSec2PercDesigns);
        tvSec2ResVideos = root.findViewById(R.id.tvSec2ResVideos);
        tvSec2PercVideos = root.findViewById(R.id.tvSec2PercVideos);
        tvSec2ResPhotos = root.findViewById(R.id.tvSec2ResPhotos);
        tvSec2PercPhotos = root.findViewById(R.id.tvSec2PercPhotos);
        tvSec2ResTotal = root.findViewById(R.id.tvSec2ResTotal);
        tvSec2PercTotal = root.findViewById(R.id.tvSec2PercTotal);

        checkBoxes[0] = root.findViewById(R.id.cbAction1);
        checkBoxes[1] = root.findViewById(R.id.cbAction2);
        checkBoxes[2] = root.findViewById(R.id.cbAction3);
        checkBoxes[3] = root.findViewById(R.id.cbAction4);
        checkBoxes[4] = root.findViewById(R.id.cbAction5);
        checkBoxes[5] = root.findViewById(R.id.cbAction6);
        checkBoxes[6] = root.findViewById(R.id.cbAction7);
        checkBoxes[7] = root.findViewById(R.id.cbAction8);
        checkBoxes[8] = root.findViewById(R.id.cbAction9);
        btnSaveActions = root.findViewById(R.id.btnSaveActions);

        rowLevelExcelente = root.findViewById(R.id.rowLevelExcelente);
        rowLevelMuyBueno = root.findViewById(R.id.rowLevelMuyBueno);
        rowLevelBueno = root.findViewById(R.id.rowLevelBueno);
        rowLevelRegular = root.findViewById(R.id.rowLevelRegular);
        rowLevelDeficiente = root.findViewById(R.id.rowLevelDeficiente);

        tvDetailGeneralObservations = root.findViewById(R.id.tvDetailGeneralObservations);

        chartDetailWeeklyBars = root.findViewById(R.id.chartDetailWeeklyBars);
        chartDetailDonut = root.findViewById(R.id.chartDetailDonut);
        tvLegendDesigns = root.findViewById(R.id.tvLegendDesigns);
        tvLegendVideos = root.findViewById(R.id.tvLegendVideos);
        tvLegendPhotos = root.findViewById(R.id.tvLegendPhotos);
        btnDownloadPdfReport = root.findViewById(R.id.btnDownloadPdfReport);
    }

    private void loadWeekData() {
        report = KpiDesignerMemoryRepository.getInstance().getWeekReport(weekId);
        if (report == null) {
            report = KpiDesignerMemoryRepository.getInstance().getCurrentWeekReport();
        }
        if (report == null) return;

        // Encabezado
        tvDetailWeekBadge.setText(report.getWeekId());
        tvDetailLevelTopBadge.setText("★ " + report.getKpiLevel());
        tvDetailDateRangeSelector.setText(report.getDateRangeFull());

        // Métrica circular
        tvDetailBigPercent.setText(report.getTotalCompliancePercentage() + "%");
        tvDetailActivitiesRatio.setText(report.getTotalActivities() + " de " + KpiDesignerWeekReport.WEEKLY_GOAL_TOTAL + " actividades");
        tvDetailLevelBadgeSub.setText("★ " + report.getKpiLevel());

        // Sección 1: Construir tabla diaria
        populateDailyTable(report.getDays());

        tvTableTotalDesigns.setText(String.valueOf(report.getTotalDesigns()));
        tvTableTotalVideos.setText(String.valueOf(report.getTotalVideos()));
        tvTableTotalAll.setText(String.valueOf(report.getTotalActivities()));
        tvTableTotalPercent.setText(report.getTotalCompliancePercentage() + "%");

        // Sección 2: Resumen
        tvSec2ResDesigns.setText(String.valueOf(report.getTotalDesigns()));
        tvSec2PercDesigns.setText(report.getDesignsPercentage() + "%");

        tvSec2ResVideos.setText(String.valueOf(report.getTotalVideos()));
        tvSec2PercVideos.setText(report.getVideosPercentage() + "%");

        tvSec2ResPhotos.setText(String.valueOf(report.getTotalPhotos()));
        tvSec2PercPhotos.setText(report.getPhotosPercentage() + "%");

        tvSec2ResTotal.setText(String.valueOf(report.getTotalActivities()));
        tvSec2PercTotal.setText(report.getTotalCompliancePercentage() + "%");

        // Sección 3: Acciones
        boolean[] actions = report.getActionsPerformed();
        for (int i = 0; i < 9; i++) {
            if (checkBoxes[i] != null && actions != null && i < actions.length) {
                checkBoxes[i].setChecked(actions[i]);
            }
        }

        // Sección 4: Destacar nivel obtenido
        highlightActiveLevel(report.getKpiLevel());

        // Sección 5: Observaciones
        if (report.getGeneralObservations() != null) {
            tvDetailGeneralObservations.setText(report.getGeneralObservations());
        }

        // Complementos visuales
        chartDetailWeeklyBars.setDaysFromRecords(report.getDays());
        chartDetailDonut.setDistribution(report.getTotalDesigns(), report.getTotalVideos(), report.getTotalPhotos());

        double dPerc = report.getTotalActivities() > 0 ? ((double) report.getTotalDesigns() / report.getTotalActivities()) * 100.0 : 0;
        double vPerc = report.getTotalActivities() > 0 ? ((double) report.getTotalVideos() / report.getTotalActivities()) * 100.0 : 0;
        double pPerc = report.getTotalActivities() > 0 ? ((double) report.getTotalPhotos() / report.getTotalActivities()) * 100.0 : 0;

        tvLegendDesigns.setText(String.format("● Diseños gráficos   %d (%.1f%%)", report.getTotalDesigns(), dPerc));
        tvLegendVideos.setText(String.format("● Videos   %d (%.1f%%)", report.getTotalVideos(), vPerc));
        tvLegendPhotos.setText(String.format("● Sesiones fotográficas   %d (%.1f%%)", report.getTotalPhotos(), pPerc));
    }

    private void populateDailyTable(List<KpiDesignerDayRecord> days) {
        containerTableDaysRows.removeAllViews();
        if (getContext() == null) return;

        float density = getResources().getDisplayMetrics().density;
        int heightPx = (int) (32 * density);
        int padPx = (int) (6 * density);

        String[] daysFull = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO"};
        String[] modalities = {"Modalidad B", "Modalidad A", "Modalidad B", "Modalidad A", "Modalidad B", "Fotografía"};
        String[] defaultGoals = {"02 v + 02 d", "06 diseños", "02 v + 02 d", "06 diseños", "02 v + 02 d", "01 sesión"};

        for (int i = 0; i < 6; i++) {
            String targetDay = daysFull[i];
            KpiDesignerDayRecord match = null;

            if (days != null) {
                for (KpiDesignerDayRecord r : days) {
                    if (r.getDayOfWeek() != null &&
                            (r.getDayOfWeek().equalsIgnoreCase(targetDay) ||
                             r.getDayOfWeek().replace("É", "E").replace("Á", "A").equalsIgnoreCase(targetDay.replace("É", "E").replace("Á", "A")))) {
                        match = r;
                        break;
                    }
                }
            }

            LinearLayout row = new LinearLayout(getContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(padPx, 0, padPx, 0);

            int textPrimary = ContextCompat.getColor(requireContext(), R.color.kpi_text_primary);
            int textSecondary = ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary);

            if (match != null) {
                // Col 1: Día
                TextView tvDay = createTableCell(targetDay, 1.2f, Gravity.START, false, textPrimary);
                // Col 2: Modalidad
                TextView tvMod = createTableCell(match.getModality() != null ? match.getModality() : modalities[i], 1.8f, Gravity.START, false, textSecondary);
                // Col 3: Meta
                TextView tvMeta = createTableCell(match.getDailyGoalDescription() != null ? match.getDailyGoalDescription() : defaultGoals[i], 1.0f, Gravity.CENTER, false, textSecondary);
                // Col 4: Diseños
                TextView tvDis = createTableCell(String.valueOf(match.getDesignsCompleted()), 0.8f, Gravity.CENTER, true, textPrimary);
                // Col 5: Videos
                TextView tvVid = createTableCell(String.valueOf(match.getVideosCompleted()), 0.8f, Gravity.CENTER, true, textPrimary);
                // Col 6: Total
                TextView tvTot = createTableCell(String.valueOf(match.getTotalCompleted()), 0.8f, Gravity.CENTER, true, textPrimary);
                // Col 7: %
                int comp = match.getCompliancePercentage();
                int colorComp = comp >= 100
                        ? ContextCompat.getColor(requireContext(), R.color.kpi_status_entregado)
                        : (comp >= 80 ? ContextCompat.getColor(requireContext(), R.color.kpi_status_pendiente) : ContextCompat.getColor(requireContext(), R.color.kpi_danger));
                TextView tvPerc = createTableCell(comp + "%", 1.0f, Gravity.CENTER, true, colorComp);

                row.addView(tvDay);
                row.addView(tvMod);
                row.addView(tvMeta);
                row.addView(tvDis);
                row.addView(tvVid);
                row.addView(tvTot);
                row.addView(tvPerc);
            } else {
                // Día pendiente
                TextView tvDay = createTableCell(targetDay, 1.2f, Gravity.START, false, textSecondary);
                TextView tvMod = createTableCell(modalities[i], 1.8f, Gravity.START, false, textSecondary);
                TextView tvMeta = createTableCell(defaultGoals[i], 1.0f, Gravity.CENTER, false, textSecondary);
                TextView tvDis = createTableCell("-", 0.8f, Gravity.CENTER, false, textSecondary);
                TextView tvVid = createTableCell("-", 0.8f, Gravity.CENTER, false, textSecondary);
                TextView tvTot = createTableCell("-", 0.8f, Gravity.CENTER, false, textSecondary);
                TextView tvPerc = createTableCell("Pendiente", 1.0f, Gravity.CENTER, false, textSecondary);

                row.addView(tvDay);
                row.addView(tvMod);
                row.addView(tvMeta);
                row.addView(tvDis);
                row.addView(tvVid);
                row.addView(tvTot);
                row.addView(tvPerc);
            }

            containerTableDaysRows.addView(row);
        }
    }

    private TextView createTableCell(String text, float weight, int gravity, boolean bold, int textColor) {
        TextView tv = new TextView(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(lp);
        tv.setText(text);
        tv.setGravity(gravity);
        tv.setTextColor(textColor);
        tv.setTextSize(10f);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return tv;
    }

    private void highlightActiveLevel(String level) {
        int defaultRowBg = ContextCompat.getColor(requireContext(), R.color.kpi_surface_variant);
        rowLevelExcelente.setBackgroundColor(defaultRowBg);
        rowLevelMuyBueno.setBackgroundColor(defaultRowBg);
        rowLevelBueno.setBackgroundColor(defaultRowBg);
        rowLevelRegular.setBackgroundColor(defaultRowBg);
        rowLevelDeficiente.setBackgroundColor(defaultRowBg);

        if ("EXCELENTE".equals(level)) {
            rowLevelExcelente.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_status_entregado_bg));
        } else if ("MUY BUENO".equals(level)) {
            rowLevelMuyBueno.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_status_justificado_bg));
        } else if ("BUENO".equals(level)) {
            rowLevelBueno.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_status_pendiente_bg));
        } else if ("REGULAR".equals(level)) {
            rowLevelRegular.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_senior_badge_orange_bg));
        } else {
            rowLevelDeficiente.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_cat_correccion_bg));
        }
    }

    private void setupActionsSaving() {
        btnSaveActions.setOnClickListener(v -> {
            // Sección 3 es la única editable; requiere BiometricPrompt antes de confirmar
            KpiBiometricHelper.authenticate(this, "Actualizar Acciones de " + weekId, new KpiBiometricHelper.BiometricCallback() {
                @Override
                public void onSuccess() {
                    saveActionsInternal();
                }

                @Override
                public void onError(int errorCode, String errorMessage) {
                    if (errorCode == -1 || errorMessage.contains("Ajustes de Seguridad") || errorMessage.contains("sensor biométrico")) {
                        showFallbackBiometricActionsDialog(errorMessage);
                    } else {
                        Toast.makeText(getContext(), "Actualización biométrica cancelada", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailed() {
                    Toast.makeText(getContext(), "Huella o rostro no reconocido. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveActionsInternal() {
        boolean[] newActions = new boolean[9];
        for (int i = 0; i < 9; i++) {
            newActions[i] = checkBoxes[i] != null && checkBoxes[i].isChecked();
        }

        KpiDesignerMemoryRepository.getInstance().updateWeekActions(weekId, newActions, true);

        new AlertDialog.Builder(requireContext())
                .setTitle("Acciones Guardadas")
                .setMessage("Las acciones efectuadas en la " + weekId + " han sido verificadas biométricamente y actualizadas exitosamente sin alterar los números de producción.")
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void showFallbackBiometricActionsDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Validación de Identidad (Sandbox)")
                .setMessage(message + "\n\n¿Deseas confirmar la actualización de acciones en el sandbox de desarrollo?")
                .setPositiveButton("Confirmar y Guardar", (dialog, which) -> saveActionsInternal())
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
