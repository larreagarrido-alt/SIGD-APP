package pe.net.ws.kpiweb.features.admin.personal.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalTypography;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeKpiItem;

/**
 * Adaptador para el listado de indicadores KPI asignados al colaborador.
 */
public class KpiAdapter extends RecyclerView.Adapter<KpiAdapter.KpiViewHolder> {

    private final List<EmployeeKpiItem> items = new ArrayList<>();

    public void submitList(List<EmployeeKpiItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KpiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.personal_item_kpi, parent, false);
        AdminPersonalTypography.apply(view);
        return new KpiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KpiViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class KpiViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvStatus;
        private final TextView tvDescription;
        private final TextView tvType;
        private final TextView tvTarget;
        private final TextView tvActual;
        private final ProgressBar pbProgress;

        public KpiViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_kpi_name);
            tvStatus = itemView.findViewById(R.id.tv_kpi_status);
            tvDescription = itemView.findViewById(R.id.tv_kpi_description);
            tvType = itemView.findViewById(R.id.tv_kpi_type);
            tvTarget = itemView.findViewById(R.id.tv_kpi_target);
            tvActual = itemView.findViewById(R.id.tv_kpi_actual);
            pbProgress = itemView.findViewById(R.id.pb_kpi_progress);
        }

        public void bind(EmployeeKpiItem item) {
            tvName.setText(item.getName());
            tvDescription.setText(item.getDescription());
            tvType.setText(item.getMeasurementType());
            tvTarget.setText("Meta: " + item.getTargetValue());
            tvActual.setText(item.getActualValue());

            EmployeeKpiItem.KpiStatus status = item.getStatus();
            tvStatus.setText(status.getDisplayName());
            tvStatus.setTextColor(status.getColorHex());
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(status.getBgTintHex()));

            if (item.isShowProgressBar()) {
                pbProgress.setVisibility(View.VISIBLE);
                pbProgress.setProgress(item.getProgressPercent());
            } else {
                pbProgress.setVisibility(View.GONE);
            }
        }
    }
}
