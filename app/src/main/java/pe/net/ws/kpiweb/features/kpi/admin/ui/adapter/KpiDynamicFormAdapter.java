package pe.net.ws.kpiweb.features.kpi.admin.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.engine.KpiCalculationEngine;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;

/**
 * Adaptador dinámico que renderiza las cajas de entrada numérica
 * para los KPIs asignados a cualquier rol, sin botones + ni -.
 */
public class KpiDynamicFormAdapter extends RecyclerView.Adapter<KpiDynamicFormAdapter.DynamicViewHolder> {

    public interface OnDynamicDataChangedListener {
        void onDataChanged(double totalWeightedScore, String overallLevel);
    }

    private final List<KpiDefinition> kpiList = new ArrayList<>();
    private final Map<String, Double> actualValues = new LinkedHashMap<>();
    private final Map<String, Double> weightedScores = new LinkedHashMap<>();
    private final OnDynamicDataChangedListener listener;
    private boolean isReadOnly = false;

    public KpiDynamicFormAdapter(OnDynamicDataChangedListener listener) {
        this.listener = listener;
    }

    public void setKpiList(List<KpiDefinition> list) {
        kpiList.clear();
        actualValues.clear();
        weightedScores.clear();
        if (list != null) {
            kpiList.addAll(list);
            for (KpiDefinition k : kpiList) {
                actualValues.put(k.getId(), 0.0);
                weightedScores.put(k.getId(), 0.0);
            }
        }
        notifyDataSetChanged();
        recalculateTotals();
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        notifyDataSetChanged();
    }

    public void setActualValues(Map<String, Double> values) {
        if (values != null) {
            for (Map.Entry<String, Double> entry : values.entrySet()) {
                actualValues.put(entry.getKey(), entry.getValue());
                KpiDefinition kpi = findKpiById(entry.getKey());
                if (kpi != null) {
                    double weighted = KpiCalculationEngine.calculateWeightedScore(
                            KpiCalculationEngine.calculateCompliance(entry.getValue(), kpi.getTargetValue(), kpi.getMeasurementType(), kpi.getPolarity()),
                            kpi.getWeight()
                    );
                    weightedScores.put(kpi.getId(), weighted);
                }
            }
        }
        notifyDataSetChanged();
        recalculateTotals();
    }

    private KpiDefinition findKpiById(String id) {
        for (KpiDefinition k : kpiList) {
            if (k.getId() != null && k.getId().equals(id)) return k;
        }
        return null;
    }

    public Map<String, Double> getActualValues() {
        return new LinkedHashMap<>(actualValues);
    }

    public List<KpiDefinition> getKpiList() {
        return new ArrayList<>(kpiList);
    }

    private void recalculateTotals() {
        List<Double> scores = new ArrayList<>(weightedScores.values());
        double total = KpiCalculationEngine.calculateTotalWeightedScore(scores);
        String level = KpiCalculationEngine.getLevelFromPercentage(total);
        if (listener != null) {
            listener.onDataChanged(total, level);
        }
    }

    @NonNull
    @Override
    public DynamicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.kpi_dynamic_form_item, parent, false);
        return new DynamicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DynamicViewHolder holder, int position) {
        KpiDefinition kpi = kpiList.get(position);
        holder.bind(kpi);
    }

    @Override
    public int getItemCount() {
        return kpiList.size();
    }

    class DynamicViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDynKpiName;
        private final TextView tvDynWeightBadge;
        private final TextView tvDynTargetDescription;
        private final EditText etDynActualValue;
        private final TextView tvDynCompliancePercent;
        private final TextView tvDynLevelBadge;

        private TextWatcher currentTextWatcher;

        public DynamicViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDynKpiName = itemView.findViewById(R.id.tvDynKpiName);
            tvDynWeightBadge = itemView.findViewById(R.id.tvDynWeightBadge);
            tvDynTargetDescription = itemView.findViewById(R.id.tvDynTargetDescription);
            etDynActualValue = itemView.findViewById(R.id.etDynActualValue);
            tvDynCompliancePercent = itemView.findViewById(R.id.tvDynCompliancePercent);
            tvDynLevelBadge = itemView.findViewById(R.id.tvDynLevelBadge);
        }

        public void bind(KpiDefinition kpi) {
            tvDynKpiName.setText(kpi.getName());
            tvDynWeightBadge.setText(String.format(Locale.getDefault(), "Peso: %.0f%%", kpi.getWeight()));

            String polarityStr = (kpi.getPolarity() == KpiPolarity.MENOR_ES_MEJOR) ? " (Menor es mejor)" : "";
            tvDynTargetDescription.setText(String.format(Locale.getDefault(), "Meta programada: %.1f %s %s",
                    kpi.getTargetValue(), kpi.getUnit(), polarityStr));

            etDynActualValue.setEnabled(!isReadOnly);

            Double currentVal = actualValues.get(kpi.getId());
            if (currentTextWatcher != null) {
                etDynActualValue.removeTextChangedListener(currentTextWatcher);
            }

            if (currentVal != null && currentVal > 0.0) {
                etDynActualValue.setText(String.format(Locale.US, "%.1f", currentVal));
            } else {
                etDynActualValue.setText("");
            }

            updateMetrics(kpi, (currentVal != null) ? currentVal : 0.0);

            currentTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    double val = 0.0;
                    if (s != null && !s.toString().trim().isEmpty()) {
                        try {
                            val = Double.parseDouble(s.toString().trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    actualValues.put(kpi.getId(), val);
                    updateMetrics(kpi, val);
                    recalculateTotals();
                }
            };

            etDynActualValue.addTextChangedListener(currentTextWatcher);
        }

        private void updateMetrics(KpiDefinition kpi, double val) {
            KpiCalculationEngine.EvaluationResult result = KpiCalculationEngine.evaluateIndicator(
                    val, kpi.getTargetValue(), kpi.getWeight(), kpi.getMeasurementType(), kpi.getPolarity());

            weightedScores.put(kpi.getId(), result.weightedScore);

            tvDynCompliancePercent.setText(String.format(Locale.getDefault(), "%.1f%%", result.rawCompliancePercentage));
            tvDynCompliancePercent.setTextColor(result.colorHex);
            tvDynLevelBadge.setText(result.performanceLevel);
        }
    }
}
