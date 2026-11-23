package pe.net.ws.kpiweb.features.kpi.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiMockDataFactory;
import pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport;

/**
 * Fragmento para visualizar la lista o historial de registros diarios de KPIs.
 */
public class KpiListFragment extends Fragment implements KpiListAdapter.OnReportClickListener {

    private RecyclerView rvKpiReportList;
    private ExtendedFloatingActionButton fabNewReport;
    private KpiListAdapter listAdapter;
    private List<KpiDailyReport> reportList;

    public static KpiListFragment newInstance() {
        return new KpiListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_fragment_list, container, false);
        initViews(root);
        loadData();
        return root;
    }

    private void initViews(View root) {
        rvKpiReportList = root.findViewById(R.id.rvKpiReportList);
        fabNewReport = root.findViewById(R.id.fabNewReport);

        rvKpiReportList.setLayoutManager(new LinearLayoutManager(getContext()));

        fabNewReport.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(android.R.id.content, KpiFormFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void loadData() {
        reportList = KpiMockDataFactory.createMockReportList();
        listAdapter = new KpiListAdapter(requireContext(), reportList, this);
        rvKpiReportList.setAdapter(listAdapter);
    }

    @Override
    public void onReportClick(KpiDailyReport report) {
        Toast.makeText(getContext(), "Abriendo reporte del " + report.getFecha(), Toast.LENGTH_SHORT).show();
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, KpiFormFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
        }
    }
}
