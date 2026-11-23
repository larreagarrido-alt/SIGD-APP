package pe.net.ws.kpiweb.features.kpi.designer.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;

/**
 * Adaptador para el listado de reportes en la pestaña Historial del Diseñador Gráfico.
 */
public class KpiDesignerHistoryAdapter extends RecyclerView.Adapter<KpiDesignerHistoryAdapter.ViewHolder> {

    public interface OnWeekClickListener {
        void onWeekClick(KpiDesignerWeekReport report);
    }

    private final List<KpiDesignerWeekReport> items = new ArrayList<>();
    private final OnWeekClickListener listener;

    public KpiDesignerHistoryAdapter(OnWeekClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<KpiDesignerWeekReport> list) {
        this.items.clear();
        if (list != null) {
            this.items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kpi_designer_item_history_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvHistoryWeekTitle;
        private final TextView tvHistoryDateRange;
        private final TextView tvHistoryActivitiesCount;
        private final TextView tvHistoryPercentBadge;
        private final TextView tvHistoryLevelBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryWeekTitle = itemView.findViewById(R.id.tvHistoryWeekTitle);
            tvHistoryDateRange = itemView.findViewById(R.id.tvHistoryDateRange);
            tvHistoryActivitiesCount = itemView.findViewById(R.id.tvHistoryActivitiesCount);
            tvHistoryPercentBadge = itemView.findViewById(R.id.tvHistoryPercentBadge);
            tvHistoryLevelBadge = itemView.findViewById(R.id.tvHistoryLevelBadge);
        }

        void bind(KpiDesignerWeekReport item) {
            tvHistoryWeekTitle.setText(item.getWeekId());
            tvHistoryDateRange.setText(item.getDateRange());
            tvHistoryActivitiesCount.setText(item.getTotalActivities() + " de " + KpiDesignerWeekReport.WEEKLY_GOAL_TOTAL + " actividades");
            tvHistoryPercentBadge.setText(item.getTotalCompliancePercentage() + "%");

            String level = item.getKpiLevel() != null ? item.getKpiLevel() : "BUENO";
            tvHistoryLevelBadge.setText(level);

            // Colores según nivel (compatibles con Modo Claro y Oscuro)
            if ("EXCELENTE".equals(level)) {
                tvHistoryLevelBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_green_bg));
                tvHistoryLevelBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_green_text));
            } else if ("MUY BUENO".equals(level) || "BUENO".equals(level)) {
                tvHistoryLevelBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_yellow_bg));
                tvHistoryLevelBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_yellow_text));
            } else if ("REGULAR".equals(level)) {
                tvHistoryLevelBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_orange_bg));
                tvHistoryLevelBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_senior_badge_orange_text));
            } else {
                tvHistoryLevelBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_cat_correccion_bg));
                tvHistoryLevelBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.kpi_danger));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWeekClick(item);
                }
            });
        }
    }
}
