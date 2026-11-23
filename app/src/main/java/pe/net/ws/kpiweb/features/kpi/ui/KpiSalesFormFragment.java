package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiMockDataFactory;
import pe.net.ws.kpiweb.features.kpi.model.KpiProductionGoal;
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesActivityItem;
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesReport;

/**
 * Fragmento para el formulario diario de evaluación KPI de Asesor de Ventas.
 */
public class KpiSalesFormFragment extends Fragment implements KpiSalesActivityAdapter.OnSalesDataChangedListener {

    private KpiSalesReport currentReport;
    private KpiSalesActivityAdapter adapter;

    // Vistas Header & Barra en vivo
    private LinearLayout btnSalesBack;
    private TextView tvSalesLivePercent;
    private ProgressBar pbSalesLiveProgress;
    private TextView tvSalesLiveSummary;

    // Sección 01
    private LinearLayout llSalesDatePicker;
    private TextView tvSalesDate;
    private EditText etSalesAdvisorName;
    private Spinner spSalesSupervisor;
    private Spinner spSalesTurno;
    private SwitchMaterial swSalesCampo;

    // Sección 02
    private LinearLayout llSalesGoalsContainer;

    // Sección 03
    private Button btnSalesAddCampo;
    private Button btnSalesAddGestion;
    private RecyclerView rvSalesActivities;

    // Sección 04
    private LinearLayout llSalesSummaryRows;

    // Botones inferiores
    private Button btnSalesClear;
    private Button btnSalesSave;

    public static KpiSalesFormFragment newInstance() {
        return new KpiSalesFormFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_fragment_sales_form, container, false);
        initViews(root);
        loadMockData();
        setupListeners();
        return root;
    }

    private void initViews(View root) {
        btnSalesBack = root.findViewById(R.id.btnSalesBack);
        tvSalesLivePercent = root.findViewById(R.id.tvSalesLivePercent);
        pbSalesLiveProgress = root.findViewById(R.id.pbSalesLiveProgress);
        tvSalesLiveSummary = root.findViewById(R.id.tvSalesLiveSummary);

        llSalesDatePicker = root.findViewById(R.id.llSalesDatePicker);
        tvSalesDate = root.findViewById(R.id.tvSalesDate);
        etSalesAdvisorName = root.findViewById(R.id.etSalesAdvisorName);
        spSalesSupervisor = root.findViewById(R.id.spSalesSupervisor);
        spSalesTurno = root.findViewById(R.id.spSalesTurno);
        swSalesCampo = root.findViewById(R.id.swSalesCampo);

        llSalesGoalsContainer = root.findViewById(R.id.llSalesGoalsContainer);

        btnSalesAddCampo = root.findViewById(R.id.btnSalesAddCampo);
        btnSalesAddGestion = root.findViewById(R.id.btnSalesAddGestion);
        rvSalesActivities = root.findViewById(R.id.rvSalesActivities);
        rvSalesActivities.setLayoutManager(new LinearLayoutManager(getContext()));

        llSalesSummaryRows = root.findViewById(R.id.llSalesSummaryRows);

        btnSalesClear = root.findViewById(R.id.btnSalesClear);
        btnSalesSave = root.findViewById(R.id.btnSalesSave);
    }

    private void loadMockData() {
        this.currentReport = KpiMockDataFactory.createSalesMockReport();

        // 01 Datos generales
        tvSalesDate.setText(currentReport.getFecha());
        etSalesAdvisorName.setText(currentReport.getAsesor());

        String[] supervisores = {"Carlos Ramírez", "María José Sánchez"};
        ArrayAdapter<String> supAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, supervisores);
        spSalesSupervisor.setAdapter(supAdapter);

        String[] turnos = {"Turno Mañana", "Turno Tarde"};
        ArrayAdapter<String> turnoAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, turnos);
        spSalesTurno.setAdapter(turnoAdapter);

        swSalesCampo.setChecked(currentReport.isEsSalidaCampo());

        // 02 Metas comerciales
        poblarMetas();

        // 03 Gestiones
        adapter = new KpiSalesActivityAdapter(requireContext(), currentReport.getGestiones(), this);
        rvSalesActivities.setAdapter(adapter);

        // 04 Resumen
        recalcularVistasMetricas();
    }

    private void poblarMetas() {
        llSalesGoalsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (KpiProductionGoal goal : currentReport.getMetas()) {
            View row = inflater.inflate(R.layout.kpi_item_goal_row, llSalesGoalsContainer, false);
            TextView tvIcon = row.findViewById(R.id.tvGoalIcon);
            TextView tvCat = row.findViewById(R.id.tvGoalCategory);
            TextView tvTarget = row.findViewById(R.id.tvGoalTarget);

            tvCat.setText(goal.getCategoria());
            tvTarget.setText(goal.getMetaTexto() + " ⌄");
            tvIcon.setText("•");
            tvIcon.setTextColor(ContextCompat.getColor(requireContext(), goal.getColorResId()));

            llSalesGoalsContainer.addView(row);
        }
    }

    private void recalcularVistasMetricas() {
        currentReport.recalcularResumen();

        int porcentaje = currentReport.getCumplimientoMeta();
        pbSalesLiveProgress.setProgress(porcentaje);

        if (currentReport.isEsSalidaCampo()) {
            tvSalesLivePercent.setText("100 % (JUSTIFICADO)");
            tvSalesLiveSummary.setText("● JORNADA COMPLETA EN TERRENO · JUSTIFICADA ANTE ADMIN");
        } else if (currentReport.getVisitasCampoJustificadas() > 0) {
            tvSalesLivePercent.setText(porcentaje + " % (JUSTIFICADO)");
            tvSalesLiveSummary.setText(currentReport.getGestionesAsignadas() + " GESTIONES · "
                    + currentReport.getGestionesConcretadas() + " CONCRETADAS · "
                    + currentReport.getVisitasCampoJustificadas() + " EN TERRENO (JUSTIFICADAS)");
        } else {
            tvSalesLivePercent.setText(porcentaje + " %");
            tvSalesLiveSummary.setText(currentReport.getGestionesAsignadas() + " GESTIONES · "
                    + currentReport.getGestionesConcretadas() + " CONCRETADAS · "
                    + currentReport.getGestionesPendientes() + " EN SEGUIMIENTO");
        }

        // 04 Tabla Resumen
        llSalesSummaryRows.removeAllViews();
        if (currentReport.isEsSalidaCampo()) {
            agregarFilaResumen("Modalidad de trabajo", "Jornada en terreno / Visitas comerciales");
            agregarFilaResumen("Estado de justificación", "AUTORIZADO (Sin penalizar KPI · 100%)");
        }
        agregarFilaResumen("Gestiones comerciales asignadas", String.valueOf(currentReport.getGestionesAsignadas()));
        agregarFilaResumen("Gestiones concretadas en oficina", String.valueOf(currentReport.getGestionesConcretadas()));
        if (currentReport.getVisitasCampoJustificadas() > 0) {
            agregarFilaResumen("Visitas comerciales en terreno", currentReport.getVisitasCampoJustificadas() + " (Acreditadas ante Admin)");
        }
        agregarFilaResumen("Gestiones en seguimiento", String.valueOf(currentReport.getGestionesPendientes()));
        agregarFilaResumen("Prospectos contactados", String.valueOf(currentReport.getProspectosContactados()));
        agregarFilaResumen("Cotizaciones formales emitidas", String.valueOf(currentReport.getCotizacionesEmitidas()));
        agregarFilaResumen("Monto total cotizado", String.format(Locale.getDefault(), "S/. %.2f", currentReport.getTotalCotizado()));
        agregarFilaResumen("Ventas cerradas concretadas", String.valueOf(currentReport.getVentasCerradas()));
        agregarFilaResumen("Monto total recaudado en ventas", String.format(Locale.getDefault(), "S/. %.2f", currentReport.getTotalVendido()));
        agregarFilaResumen("Cumplimiento comercial de meta", porcentaje + " %");
        agregarFilaResumen("Nivel de productividad", currentReport.getNivelProductividad() + " (" + currentReport.getPuntaje() + " pts)");
    }

    private void agregarFilaResumen(String indicador, String resultado) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvInd = new TextView(getContext());
        tvInd.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tvInd.setText(indicador);
        tvInd.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        tvInd.setTextSize(12);

        TextView tvRes = new TextView(getContext());
        tvRes.setText(resultado);
        tvRes.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tvRes.setTextSize(12);
        tvRes.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvInd);
        row.addView(tvRes);
        llSalesSummaryRows.addView(row);
    }

    private void setupListeners() {
        // Volver al Panel
        btnSalesBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Selector nativo de Fecha
        View.OnClickListener dateClick = v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog d = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                String f = String.format(Locale.getDefault(), "%02d / %02d / %04d", dayOfMonth, month + 1, year);
                tvSalesDate.setText(f);
                currentReport.setFecha(f);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            d.show();
        };
        llSalesDatePicker.setOnClickListener(dateClick);
        tvSalesDate.setOnClickListener(dateClick);

        // Switch Salida a Terreno
        swSalesCampo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentReport.setEsSalidaCampo(isChecked);
            recalcularVistasMetricas();
        });

        // + Visita en terreno
        btnSalesAddCampo.setOnClickListener(v -> {
            int n = currentReport.getGestiones().size() + 1;
            String formatted = String.format(Locale.getDefault(), "%02d", n);
            KpiSalesActivityItem item = new KpiSalesActivityItem(
                    "GES-" + System.currentTimeMillis(),
                    formatted,
                    "",
                    "Visita en terreno",
                    "Visita presencial a sede de cliente para demostración de servicios WebSite+",
                    "03:30 pm",
                    "06:30 pm",
                    "Salida a campo",
                    0.0
            );
            currentReport.getGestiones().add(item);
            adapter.notifyItemInserted(currentReport.getGestiones().size() - 1);
            recalcularVistasMetricas();
            Toast.makeText(getContext(), "Visita en terreno agregada (Justificada ante Admin)", Toast.LENGTH_SHORT).show();
        });

        // + Gestión comercial
        btnSalesAddGestion.setOnClickListener(v -> {
            int n = currentReport.getGestiones().size() + 1;
            String formatted = String.format(Locale.getDefault(), "%02d", n);
            KpiSalesActivityItem item = new KpiSalesActivityItem(
                    "GES-" + System.currentTimeMillis(),
                    formatted,
                    "",
                    "Prospección",
                    "",
                    "03:30 pm",
                    "04:15 pm",
                    "Concretado",
                    0.0
            );
            currentReport.getGestiones().add(item);
            adapter.notifyItemInserted(currentReport.getGestiones().size() - 1);
            recalcularVistasMetricas();
        });

        // Limpiar
        btnSalesClear.setOnClickListener(v -> {
            currentReport.getGestiones().clear();
            adapter.notifyDataSetChanged();
            recalcularVistasMetricas();
            Toast.makeText(getContext(), "Formulario comercial reiniciado", Toast.LENGTH_SHORT).show();
        });

        // Guardar
        btnSalesSave.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Esta vista comercial de ejemplo no guarda datos. Usa el formulario principal de Ventas.",
                    Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDataChanged() {
        recalcularVistasMetricas();
    }

    @Override
    public void onItemDeleted(int position) {
        if (position >= 0 && position < currentReport.getGestiones().size()) {
            currentReport.getGestiones().remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, currentReport.getGestiones().size());
            recalcularVistasMetricas();
        }
    }
}
