package pe.net.ws.kpiweb.features.kpi.admin.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.admin.ui.adapter.KpiAdminListAdapter;

/**
 * Fragmento principal del Gestor de KPIs para Gerencia.
 * Muestra el catálogo completo, buscador, filtros por área y switches de activación.
 */
public class KpiAdminManageListFragment extends Fragment implements KpiAdminListAdapter.OnKpiActionListener {

    private ImageView btnAdminBack, btnQuickAddKpi;
    private EditText etAdminSearch;
    private TextView chipFilterAll, chipFilterSales, chipFilterDesign, chipFilterTech;
    private RecyclerView rvAdminKpis;
    private View btnCreateNewKpi;

    private KpiAdminListAdapter adapter;
    private TextView activeChip;

    public static KpiAdminManageListFragment newInstance() {
        return new KpiAdminManageListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_admin_fragment_manage_list, container, false);
        initViews(root);
        setupRecyclerView();
        setupListeners();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadKpis();
        pe.net.ws.kpiweb.features.kpi.sync.KpiDraftSyncManager.retry(requireContext());
        pe.net.ws.kpiweb.features.kpi.sync.KpiDraftSyncManager.refresh(requireContext(), () -> {
            if (isAdded()) loadKpis();
        });
    }

    private void initViews(View root) {
        btnAdminBack = root.findViewById(R.id.btnAdminBack);
        btnQuickAddKpi = root.findViewById(R.id.btnQuickAddKpi);
        etAdminSearch = root.findViewById(R.id.etAdminSearch);
        chipFilterAll = root.findViewById(R.id.chipFilterAll);
        chipFilterSales = root.findViewById(R.id.chipFilterSales);
        chipFilterDesign = root.findViewById(R.id.chipFilterDesign);
        chipFilterTech = root.findViewById(R.id.chipFilterTech);
        rvAdminKpis = root.findViewById(R.id.rvAdminKpis);
        btnCreateNewKpi = root.findViewById(R.id.btnCreateNewKpi);
        activeChip = chipFilterAll;
    }

    private void setupRecyclerView() {
        rvAdminKpis.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KpiAdminListAdapter(this);
        rvAdminKpis.setAdapter(adapter);
    }

    private void loadKpis() {
        if (getContext() != null) {
            List<KpiDefinition> list = KpiCustomRepository.getInstance(requireContext()).getAllKpis();
            adapter.updateData(list);
        }
    }

    private void setupListeners() {
        btnAdminBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        View.OnClickListener openCreate = v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main, KpiAdminEditKpiFragment.newInstance(null))
                        .addToBackStack(null)
                        .commit();
            }
        };

        btnQuickAddKpi.setOnClickListener(openCreate);
        btnCreateNewKpi.setOnClickListener(openCreate);

        etAdminSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearchQuery(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipFilterAll.setOnClickListener(v -> selectChip(chipFilterAll, "TODOS"));
        chipFilterSales.setOnClickListener(v -> selectChip(chipFilterSales, "COMERCIAL"));
        chipFilterDesign.setOnClickListener(v -> selectChip(chipFilterDesign, "DISEÑO"));
        chipFilterTech.setOnClickListener(v -> selectChip(chipFilterTech, "TECNOLOGÍA"));
    }

    private void selectChip(TextView chip, String area) {
        if (activeChip != null) {
            activeChip.setBackgroundResource(R.drawable.kpi_bg_tab_inactive_light);
            activeChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        }
        chip.setBackgroundResource(R.drawable.kpi_bg_tab_active_purple);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_on_primary));
        activeChip = chip;
        adapter.setAreaFilter(area);
    }

    @Override
    public void onKpiClicked(KpiDefinition kpi) {
        if (getActivity() != null && kpi != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main, KpiAdminEditKpiFragment.newInstance(kpi.getId()))
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onKpiStatusToggled(KpiDefinition kpi, boolean isChecked) {
        if (getContext() != null && kpi != null) {
            KpiCustomRepository.getInstance(requireContext()).toggleKpiStatus(kpi.getId());
            String status = isChecked ? "activado" : "desactivado";
            Toast.makeText(getContext(), "KPI " + status + ": " + kpi.getName(), Toast.LENGTH_SHORT).show();
        }
    }
}
