package pe.net.ws.kpiweb.features.kpi.admin.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;

/**
 * Adaptador para listar las entregas auditadas e inmutables del formulario dinámico.
 */
public class KpiDynamicHistoryAdapter extends RecyclerView.Adapter<KpiDynamicHistoryAdapter.HistoryViewHolder> {

    private final List<KpiRecordEntry> entries = new ArrayList<>();

    public void setEntries(List<KpiRecordEntry> list) {
        entries.clear();
        if (list != null) {
            entries.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.kpi_dynamic_history_item, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        KpiRecordEntry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvHistKpiName;
        private final TextView tvHistComplianceBadge;
        private final TextView tvHistMetrics;
        private final TextView tvHistWeightScore;
        private final LinearLayout llHistFieldWorkNotice;
        private final TextView tvHistFieldWorkReason;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistKpiName = itemView.findViewById(R.id.tvHistKpiName);
            tvHistComplianceBadge = itemView.findViewById(R.id.tvHistComplianceBadge);
            tvHistMetrics = itemView.findViewById(R.id.tvHistMetrics);
            tvHistWeightScore = itemView.findViewById(R.id.tvHistWeightScore);
            llHistFieldWorkNotice = itemView.findViewById(R.id.llHistFieldWorkNotice);
            tvHistFieldWorkReason = itemView.findViewById(R.id.tvHistFieldWorkReason);
        }

        public void bind(KpiRecordEntry entry) {
            tvHistKpiName.setText(entry.getKpiName());
            tvHistComplianceBadge.setText(String.format(Locale.getDefault(), "%.1f%%", entry.getComputedCompliance()));

            if (entry.getComputedCompliance() >= 90.0) {
                tvHistComplianceBadge.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
                tvHistComplianceBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_green_text));
            } else if (entry.getComputedCompliance() >= 60.0) {
                tvHistComplianceBadge.setBackgroundResource(R.drawable.kpi_senior_bg_pill_yellow);
                tvHistComplianceBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_yellow_text));
            } else {
                tvHistComplianceBadge.setBackgroundResource(R.drawable.kpi_bg_stepper_btn);
                tvHistComplianceBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_danger));
            }

            tvHistMetrics.setText(String.format(Locale.getDefault(), "%s  ·  Registrado: %.1f  ·  Meta: %.1f (v%d)",
                    entry.getDate(), entry.getActualValue(), entry.getTargetValueUsed(), entry.getKpiVersionUsed()));

            tvHistWeightScore.setText(String.format(Locale.getDefault(), "Puntos: %.1f pts", entry.getComputedWeightedScore()));

            if (entry.isFieldWork()) {
                llHistFieldWorkNotice.setVisibility(View.VISIBLE);
                tvHistFieldWorkReason.setText(entry.getFieldWorkReason() != null ? entry.getFieldWorkReason() : "Jornada en terreno");
            } else {
                llHistFieldWorkNotice.setVisibility(View.GONE);
            }
        }
    }
}
