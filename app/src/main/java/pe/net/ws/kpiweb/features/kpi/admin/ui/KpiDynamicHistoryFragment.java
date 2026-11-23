package pe.net.ws.kpiweb.features.kpi.admin.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import pe.net.ws.kpiweb.features.kpi.sync.KpiSyncManager;
import pe.net.ws.kpiweb.features.kpi.admin.ui.adapter.KpiDynamicHistoryAdapter;
import pe.net.ws.kpiweb.features.kpi.export.KpiCsvExporter;
import pe.net.ws.kpiweb.features.kpi.export.KpiPdfExporter;

/**
 * Pantalla de Historial de Entregas y Auditoría del Formulario Dinámico.
 * Permite filtrar por puesto, auditar registros congelados y exportar a PDF y CSV.
 */
public class KpiDynamicHistoryFragment extends Fragment {

    private static final String ARG_FILTER_ROLE = "arg_filter_role";

    private String currentRoleFilter = "Todos";
    private ImageView btnHistBack, btnHistExportPdf, btnHistExportCsv;
    private Spinner spHistFilterRole;
    private TextView tvHistTotalCount, tvHistAverageCompliance, tvHistEmptyNotice, tvHistSyncStatus;
    private View btnHistSync;
    private RecyclerView rvDynamicHistory;
    private KpiDynamicHistoryAdapter adapter;

    private final String[] FILTER_ROLES = {
            "Todos",
            "Asesor de Ventas",
            "Diseñador Gráfico",
            "Diseñador Gráfico Junior",
            "Diseñador Gráfico Senior",
            "Programador"
    };

    public static KpiDynamicHistoryFragment newInstance(@Nullable String initialRole) {
        KpiDynamicHistoryFragment fragment = new KpiDynamicHistoryFragment();
        Bundle args = new Bundle();
        if (initialRole != null) {
            args.putString(ARG_FILTER_ROLE, initialRole);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getString(ARG_FILTER_ROLE) != null) {
            currentRoleFilter = getArguments().getString(ARG_FILTER_ROLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_fragment_dynamic_history, container, false);
        initViews(root);
        setupFilterSpinner();
        setupRecyclerView();
        setupListeners();
        loadHistoryData();
        return root;
    }

    private void initViews(View root) {
        btnHistBack = root.findViewById(R.id.btnHistBack);
        btnHistExportPdf = root.findViewById(R.id.btnHistExportPdf);
        btnHistExportCsv = root.findViewById(R.id.btnHistExportCsv);
        spHistFilterRole = root.findViewById(R.id.spHistFilterRole);
        tvHistTotalCount = root.findViewById(R.id.tvHistTotalCount);
        tvHistAverageCompliance = root.findViewById(R.id.tvHistAverageCompliance);
        tvHistEmptyNotice = root.findViewById(R.id.tvHistEmptyNotice);
        tvHistSyncStatus = root.findViewById(R.id.tvHistSyncStatus);
        btnHistSync = root.findViewById(R.id.btnHistSync);
        rvDynamicHistory = root.findViewById(R.id.rvDynamicHistory);
    }

    private void setupFilterSpinner() {
        if (getContext() == null) return;
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, FILTER_ROLES);
        spHistFilterRole.setAdapter(roleAdapter);

        for (int i = 0; i < FILTER_ROLES.length; i++) {
            if (FILTER_ROLES[i].equalsIgnoreCase(currentRoleFilter)) {
                spHistFilterRole.setSelection(i);
                break;
            }
        }

        spHistFilterRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentRoleFilter = FILTER_ROLES[position];
                loadHistoryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        rvDynamicHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KpiDynamicHistoryAdapter();
        rvDynamicHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnHistSync.setOnClickListener(v -> {
            btnHistSync.setEnabled(false);
            tvHistSyncStatus.setText("Enviando registros pendientes…");
            KpiSyncManager.getInstance(requireContext()).syncPendingRecords(new KpiSyncManager.SyncCallback() {
                @Override public void onSyncSuccess(int count) {
                    if (!isAdded() || getView() == null) return;
                    btnHistSync.setEnabled(true);
                    tvHistSyncStatus.setText(count + " confirmados por el servidor; "
                            + KpiSyncManager.getInstance(requireContext()).getPendingSyncCount() + " pendientes.");
                    loadHistoryData();
                }
                @Override public void onSyncError(String message) {
                    if (!isAdded() || getView() == null) return;
                    btnHistSync.setEnabled(true);
                    tvHistSyncStatus.setText(message);
                }
            });
        });
        btnHistBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnHistExportPdf.setOnClickListener(v -> {
            List<KpiRecordEntry> list = getFilteredEntries();
            if (list.isEmpty()) {
                Toast.makeText(getContext(), "No hay registros para exportar a PDF.", Toast.LENGTH_SHORT).show();
                return;
            }
            KpiPdfExporter.exportEntriesToPdf(requireContext(), list, currentRoleFilter);
        });

        btnHistExportCsv.setOnClickListener(v -> {
            List<KpiRecordEntry> list = getFilteredEntries();
            if (list.isEmpty()) {
                Toast.makeText(getContext(), "No hay registros para exportar a CSV.", Toast.LENGTH_SHORT).show();
                return;
            }
            KpiCsvExporter.exportEntriesToCsv(requireContext(), list, currentRoleFilter);
        });
    }

    private List<KpiRecordEntry> getFilteredEntries() {
        if (getContext() == null) return new ArrayList<>();
        List<KpiRecordEntry> all = KpiCustomRepository.getInstance(requireContext()).getAllRecordEntries();
        List<KpiRecordEntry> filtered = new ArrayList<>();

        for (KpiRecordEntry e : all) {
            if ("Todos".equalsIgnoreCase(currentRoleFilter) || currentRoleFilter.equalsIgnoreCase(e.getRole())) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    private void loadHistoryData() {
        if (tvHistSyncStatus != null && tvHistSyncStatus.getText().length() == 0)
            tvHistSyncStatus.setText(KpiSyncManager.getInstance(requireContext()).getPendingSyncCount() + " registros pendientes de confirmación.");
        List<KpiRecordEntry> list = getFilteredEntries();
        adapter.setEntries(list);

        if (list.isEmpty()) {
            tvHistEmptyNotice.setVisibility(View.VISIBLE);
            tvHistTotalCount.setText("0 registros");
            tvHistAverageCompliance.setText("0.0%");
        } else {
            tvHistEmptyNotice.setVisibility(View.GONE);
            tvHistTotalCount.setText(list.size() + " registros");

            double sumCompliance = 0.0;
            for (KpiRecordEntry e : list) {
                sumCompliance += e.getComputedCompliance();
            }
            double avg = sumCompliance / list.size();
            tvHistAverageCompliance.setText(String.format(Locale.getDefault(), "%.1f%%", avg));
        }
    }
}
