package pe.net.ws.kpiweb.features.kpi.admin.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.engine.KpiMeasurementType;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;

/**
 * Fragmento para crear y editar parámetros de KPIs por Gerencia.
 */
public class KpiAdminEditKpiFragment extends Fragment {

    private static final String ARG_KPI_ID = "arg_kpi_id";

    private String kpiId;
    private KpiDefinition currentKpi;

    private ImageView btnEditBack;
    private TextView tvEditTitle, tvEditSubtitle, tvEditVersionBadge;
    private EditText etEditKpiName, etEditKpiDesc, etEditKpiObjective;
    private Spinner spEditArea, spEditRole, spEditMeasurementType, spEditFrequency;
    private EditText etEditUnit, etEditTargetValue, etEditWeight;
    private RadioGroup rgPolarity;
    private RadioButton rbMayorEsMejor, rbMenorEsMejor;
    private SwitchMaterial swEditActive;
    private LinearLayout llVersionHistoryContainer;
    private TextView tvVersionHistoryList;
    private Button btnSaveKpiConfig;

    private static final String ARG_PRESELECTED_ROLE = "arg_preselected_role";

    private String preselectedRole;
    private List<String> dynamicAreas;
    private List<String> dynamicRoles;
    private final String[] FREQUENCIES = {"Diaria", "Semanal", "Mensual"};

    public static KpiAdminEditKpiFragment newInstance(@Nullable String kpiId) {
        return newInstance(kpiId, null);
    }

    public static KpiAdminEditKpiFragment newInstance(@Nullable String kpiId, @Nullable String targetRole) {
        KpiAdminEditKpiFragment fragment = new KpiAdminEditKpiFragment();
        Bundle args = new Bundle();
        if (kpiId != null) {
            args.putString(ARG_KPI_ID, kpiId);
        }
        if (targetRole != null) {
            args.putString(ARG_PRESELECTED_ROLE, targetRole);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            kpiId = getArguments().getString(ARG_KPI_ID);
            preselectedRole = getArguments().getString(ARG_PRESELECTED_ROLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_admin_fragment_edit_kpi, container, false);
        initViews(root);
        setupSpinners();
        loadKpiData();
        setupListeners();
        return root;
    }

    private void initViews(View root) {
        btnEditBack = root.findViewById(R.id.btnEditBack);
        tvEditTitle = root.findViewById(R.id.tvEditTitle);
        tvEditSubtitle = root.findViewById(R.id.tvEditSubtitle);
        tvEditVersionBadge = root.findViewById(R.id.tvEditVersionBadge);
        etEditKpiName = root.findViewById(R.id.etEditKpiName);
        etEditKpiDesc = root.findViewById(R.id.etEditKpiDesc);
        etEditKpiObjective = root.findViewById(R.id.etEditKpiObjective);
        spEditArea = root.findViewById(R.id.spEditArea);
        spEditRole = root.findViewById(R.id.spEditRole);
        spEditMeasurementType = root.findViewById(R.id.spEditMeasurementType);
        spEditFrequency = root.findViewById(R.id.spEditFrequency);
        etEditUnit = root.findViewById(R.id.etEditUnit);
        etEditTargetValue = root.findViewById(R.id.etEditTargetValue);
        etEditWeight = root.findViewById(R.id.etEditWeight);
        rgPolarity = root.findViewById(R.id.rgPolarity);
        rbMayorEsMejor = root.findViewById(R.id.rbMayorEsMejor);
        rbMenorEsMejor = root.findViewById(R.id.rbMenorEsMejor);
        swEditActive = root.findViewById(R.id.swEditActive);
        llVersionHistoryContainer = root.findViewById(R.id.llVersionHistoryContainer);
        tvVersionHistoryList = root.findViewById(R.id.tvVersionHistoryList);
        btnSaveKpiConfig = root.findViewById(R.id.btnSaveKpiConfig);
    }

    private void setupSpinners() {
        if (getContext() == null) return;

        KpiCustomRepository repo = KpiCustomRepository.getInstance(requireContext());
        dynamicAreas = repo.getAllConfiguredAreas(requireContext());
        dynamicRoles = repo.getAllConfiguredRoles(requireContext());
        if (!dynamicRoles.contains("Todos")) {
            dynamicRoles.add("Todos");
        }

        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, dynamicAreas);
        spEditArea.setAdapter(areaAdapter);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, dynamicRoles);
        spEditRole.setAdapter(roleAdapter);

        if (preselectedRole != null) {
            setSpinnerSelection(spEditRole, preselectedRole);
        }

        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, FREQUENCIES);
        spEditFrequency.setAdapter(freqAdapter);

        KpiMeasurementType[] types = KpiMeasurementType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeNames);
        spEditMeasurementType.setAdapter(typeAdapter);
    }

    private void loadKpiData() {
        if (kpiId != null && getContext() != null) {
            currentKpi = KpiCustomRepository.getInstance(requireContext()).getKpiById(kpiId);
        }

        if (currentKpi != null) {
            tvEditTitle.setText("Editar KPI");
            tvEditSubtitle.setText(currentKpi.getName());
            tvEditVersionBadge.setText("Versión " + currentKpi.getVersion());
            etEditKpiName.setText(currentKpi.getName());
            etEditKpiDesc.setText(currentKpi.getDescription());
            etEditKpiObjective.setText(currentKpi.getObjective());

            setSpinnerSelection(spEditArea, currentKpi.getArea());
            setSpinnerSelection(spEditRole, currentKpi.getRole());
            if (currentKpi.getMeasurementType() != null) {
                setSpinnerSelection(spEditMeasurementType, currentKpi.getMeasurementType().getDisplayName());
            }
            setSpinnerSelection(spEditFrequency, currentKpi.getFrequency());

            etEditUnit.setText(currentKpi.getUnit());
            etEditTargetValue.setText(String.format(Locale.US, "%.1f", currentKpi.getTargetValue()));
            etEditWeight.setText(String.format(Locale.US, "%.0f", currentKpi.getWeight()));

            if (currentKpi.getPolarity() == KpiPolarity.MENOR_ES_MEJOR) {
                rbMenorEsMejor.setChecked(true);
            } else {
                rbMayorEsMejor.setChecked(true);
            }

            swEditActive.setChecked(currentKpi.isActive());

            // Versiones históricas si existen
            List<KpiDefinition> history = KpiCustomRepository.getInstance(requireContext()).getVersionHistoryForKpi(kpiId);
            if (history != null && !history.isEmpty()) {
                llVersionHistoryContainer.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                for (KpiDefinition v : history) {
                    sb.append("• Versión ").append(v.getVersion())
                            .append(": Meta ").append(v.getTargetValue()).append(" ").append(v.getUnit())
                            .append(" | Peso ").append(v.getWeight()).append("%\n");
                }
                tvVersionHistoryList.setText(sb.toString().trim());
            } else {
                llVersionHistoryContainer.setVisibility(View.GONE);
            }
        } else {
            tvEditTitle.setText("Crear Nuevo KPI");
            tvEditSubtitle.setText("Definir nuevo indicador paramétrico");
            tvEditVersionBadge.setText("Nueva Versión 1");
            etEditUnit.setText("unidades");
            etEditTargetValue.setText("10.0");
            etEditWeight.setText("20");
            llVersionHistoryContainer.setVisibility(View.GONE);
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner == null || value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupListeners() {
        btnEditBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnSaveKpiConfig.setOnClickListener(v -> saveKpi());
    }

    private void saveKpi() {
        String name = etEditKpiName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Por favor ingresa el nombre del KPI.", Toast.LENGTH_SHORT).show();
            etEditKpiName.requestFocus();
            return;
        }

        String targetStr = etEditTargetValue.getText().toString().trim();
        if (targetStr.isEmpty()) {
            Toast.makeText(getContext(), "Ingresa una meta numérica válida.", Toast.LENGTH_SHORT).show();
            etEditTargetValue.requestFocus();
            return;
        }

        double targetValue;
        try {
            targetValue = Double.parseDouble(targetStr);
            if (targetValue <= 0.0) {
                Toast.makeText(getContext(), "La meta debe ser un valor mayor a cero.", Toast.LENGTH_SHORT).show();
                etEditTargetValue.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Meta numérica no válida.", Toast.LENGTH_SHORT).show();
            return;
        }

        String weightStr = etEditWeight.getText().toString().trim();
        double weight = 20.0;
        if (!weightStr.isEmpty()) {
            try {
                weight = Double.parseDouble(weightStr);
                if (weight < 1.0 || weight > 100.0) {
                    Toast.makeText(getContext(), "El peso porcentual debe estar entre 1% y 100%.", Toast.LENGTH_SHORT).show();
                    etEditWeight.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Peso no válido.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (currentKpi == null) {
            currentKpi = new KpiDefinition();
        }

        currentKpi.setName(name);
        currentKpi.setDescription(etEditKpiDesc.getText().toString().trim());
        currentKpi.setObjective(etEditKpiObjective.getText().toString().trim());
        currentKpi.setArea(spEditArea.getSelectedItem().toString());
        currentKpi.setRole(spEditRole.getSelectedItem().toString());

        int typeIndex = spEditMeasurementType.getSelectedItemPosition();
        if (typeIndex >= 0 && typeIndex < KpiMeasurementType.values().length) {
            currentKpi.setMeasurementType(KpiMeasurementType.values()[typeIndex]);
        }

        currentKpi.setUnit(etEditUnit.getText().toString().trim());
        currentKpi.setTargetValue(targetValue);
        currentKpi.setFrequency(spEditFrequency.getSelectedItem().toString());
        currentKpi.setWeight(weight);

        currentKpi.setPolarity(rbMenorEsMejor.isChecked() ? KpiPolarity.MENOR_ES_MEJOR : KpiPolarity.MAYOR_ES_MEJOR);
        currentKpi.setActive(swEditActive.isChecked());

        KpiCustomRepository.getInstance(requireContext()).saveOrUpdateKpi(currentKpi);

        pe.net.ws.kpiweb.features.kpi.sync.KpiDraftSyncManager.enqueue(requireContext(), currentKpi);

        Toast.makeText(getContext(), "Borrador local v" + currentKpi.getVersion()
                + " en cola para confirmar en el servidor. Aún requiere aprobación oficial.", Toast.LENGTH_LONG).show();

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
