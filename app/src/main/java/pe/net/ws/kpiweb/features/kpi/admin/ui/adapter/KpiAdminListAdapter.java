package pe.net.ws.kpiweb.features.kpi.admin.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;

/**
 * Adaptador para listar los KPIs configurados en el panel administrativo.
 */
public class KpiAdminListAdapter extends RecyclerView.Adapter<KpiAdminListAdapter.KpiViewHolder> {

    public interface OnKpiActionListener {
        void onKpiClicked(KpiDefinition kpi);
        void onKpiStatusToggled(KpiDefinition kpi, boolean isChecked);
    }

    private final List<KpiDefinition> fullList = new ArrayList<>();
    private final List<KpiDefinition> filteredList = new ArrayList<>();
    private final OnKpiActionListener listener;

    private String currentAreaFilter = "TODOS";
    private String currentSearchQuery = "";

    public KpiAdminListAdapter(OnKpiActionListener listener) {
        this.listener = listener;
    }

    public void updateData(List<KpiDefinition> newList) {
        fullList.clear();
        if (newList != null) {
            fullList.addAll(newList);
        }
        applyFilters();
    }

    public void setAreaFilter(String area) {
        this.currentAreaFilter = (area != null) ? area.toUpperCase(Locale.ROOT) : "TODOS";
        applyFilters();
    }

    public void setSearchQuery(String query) {
        this.currentSearchQuery = (query != null) ? query.toLowerCase(Locale.ROOT).trim() : "";
        applyFilters();
    }

    private void applyFilters() {
        filteredList.clear();
        for (KpiDefinition k : fullList) {
            boolean areaMatch = currentAreaFilter.equals("TODOS") ||
                    (k.getArea() != null && k.getArea().toUpperCase(Locale.ROOT).contains(currentAreaFilter));

            boolean searchMatch = currentSearchQuery.isEmpty() ||
                    (k.getName() != null && k.getName().toLowerCase(Locale.ROOT).contains(currentSearchQuery)) ||
                    (k.getRole() != null && k.getRole().toLowerCase(Locale.ROOT).contains(currentSearchQuery));

            if (areaMatch && searchMatch) {
                filteredList.add(k);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KpiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.kpi_admin_item_card, parent, false);
        return new KpiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KpiViewHolder holder, int position) {
        KpiDefinition kpi = filteredList.get(position);
        holder.bind(kpi, listener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class KpiViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCardArea;
        private final TextView tvCardRole;
        private final TextView tvCardVersion;
        private final SwitchMaterial swCardStatus;
        private final TextView tvCardKpiName;
        private final TextView tvCardKpiDesc;
        private final TextView tvCardTarget;
        private final TextView tvCardWeight;
        private final TextView tvCardTypePolarity;
        private final ImageView btnCardEdit;

        public KpiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardArea = itemView.findViewById(R.id.tvCardArea);
            tvCardRole = itemView.findViewById(R.id.tvCardRole);
            tvCardVersion = itemView.findViewById(R.id.tvCardVersion);
            swCardStatus = itemView.findViewById(R.id.swCardStatus);
            tvCardKpiName = itemView.findViewById(R.id.tvCardKpiName);
            tvCardKpiDesc = itemView.findViewById(R.id.tvCardKpiDesc);
            tvCardTarget = itemView.findViewById(R.id.tvCardTarget);
            tvCardWeight = itemView.findViewById(R.id.tvCardWeight);
            tvCardTypePolarity = itemView.findViewById(R.id.tvCardTypePolarity);
            btnCardEdit = itemView.findViewById(R.id.btnCardEdit);
        }

        public void bind(KpiDefinition kpi, OnKpiActionListener listener) {
            tvCardArea.setText(kpi.getArea() != null ? kpi.getArea().toUpperCase(Locale.ROOT) : "GENERAL");
            tvCardRole.setText(kpi.getRole() != null ? kpi.getRole() : "Todos");
            tvCardVersion.setText("v" + kpi.getVersion());
            tvCardKpiName.setText(kpi.getName());
            tvCardKpiDesc.setText(kpi.getDescription() != null ? kpi.getDescription() : "");

            String targetStr = (kpi.getTargetValue() == (long) kpi.getTargetValue()) ?
                    String.format(Locale.getDefault(), "%d %s", (long) kpi.getTargetValue(), kpi.getUnit()) :
                    String.format(Locale.getDefault(), "%.1f %s", kpi.getTargetValue(), kpi.getUnit());
            tvCardTarget.setText(targetStr);

            tvCardWeight.setText(String.format(Locale.getDefault(), "%.0f%%", kpi.getWeight()));

            String polarityIcon = (kpi.getPolarity() == KpiPolarity.MAYOR_ES_MEJOR) ? "▲ Mayor" : "▼ Menor";
            String typeName = (kpi.getMeasurementType() != null) ? kpi.getMeasurementType().getDisplayName() : "Cantidad";
            tvCardTypePolarity.setText(typeName + " (" + polarityIcon + ")");

            swCardStatus.setOnCheckedChangeListener(null);
            swCardStatus.setChecked(kpi.isActive());
            swCardStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onKpiStatusToggled(kpi, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onKpiClicked(kpi);
            });

            btnCardEdit.setOnClickListener(v -> {
                if (listener != null) listener.onKpiClicked(kpi);
            });
        }
    }
}
