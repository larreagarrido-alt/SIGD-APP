package pe.net.ws.kpiweb.features.admin.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;

/** Optimized adapter for displaying KPI reports received for administrative review. */
final class AdminKpiReportAdapter extends RecyclerView.Adapter<AdminKpiReportAdapter.Holder> {
    interface OnReportClickListener {
        void onReportClick(int reportId);
    }

    private final List<JsonObject> items = new ArrayList<>();
    private final OnReportClickListener listener;

    AdminKpiReportAdapter(OnReportClickListener listener) {
        this.listener = listener;
    }

    void submitList(List<JsonObject> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_kpi_report, parent, false);
        AdminTypography.apply(parent.getContext(), view);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        JsonObject item = items.get(position);
        int reportId = AdminKpiOfficialFormatter.safeInt(item, "id_detalle");
        int workerId = AdminKpiOfficialFormatter.safeInt(item, "id_trabajador");
        int evals = AdminKpiOfficialFormatter.safeInt(item, "evaluaciones_vinculadas");
        String area = AdminKpiOfficialFormatter.formatFormType(
                AdminKpiOfficialFormatter.safeString(item, "tipo_formulario"));
        String worker = AdminKpiOfficialFormatter.formatWorkerLabel(
                AdminKpiOfficialFormatter.safeString(item, "nombre_completo"), workerId);
        String state = AdminKpiOfficialFormatter.safeString(item, "estado_evaluacion");
        String percent = AdminKpiOfficialFormatter.safeString(item, "porcentaje_cumplimiento");
        if (percent.isEmpty()) percent = AdminKpiOfficialFormatter.safeString(item, "porcentaje_estimado");
        String evalText = evals == 0 ? "Por calcular" :
                (state.isEmpty() ? "En revisión" : state)
                + (percent.isEmpty() ? "" : " · " + percent + " %");
        if (evals == 0 && !percent.isEmpty()) evalText = "Calculado · " + percent + " %";

        holder.title.setText(area + " · Reporte #" + reportId);
        holder.subtitle.setText(worker);
        holder.evals.setText(evalText);
        boolean approved = state.equalsIgnoreCase("APROBADA")
                || state.equalsIgnoreCase("APROBADO");
        boolean observed = state.equalsIgnoreCase("OBSERVADA")
                || state.equalsIgnoreCase("OBSERVADO");
        holder.evals.setBackgroundResource(approved
                ? R.drawable.admin_bg_kpi_badge_approved
                : observed ? R.drawable.admin_bg_kpi_badge_pending
                : evals > 0 ? R.drawable.admin_bg_ranking_mini_chip
                : R.drawable.admin_bg_kpi_eval_pill_neutral);
        holder.evals.setTextColor(holder.itemView.getContext().getColor(approved
                ? R.color.admin_status_delivered
                : observed ? R.color.admin_status_pending
                : evals > 0 ? R.color.admin_purple_accent : R.color.admin_text_secondary));
        holder.evals.setCompoundDrawablesWithIntrinsicBounds(approved
                ? R.drawable.admin_ic_check : R.drawable.admin_ic_info_circle, 0, 0, 0);
        holder.evals.setCompoundDrawablePadding(4);
        android.graphics.drawable.Drawable statusIcon = holder.evals.getCompoundDrawables()[0];
        if (statusIcon != null) statusIcon.setTint(holder.evals.getCurrentTextColor());

        boolean hasAction = listener != null && reportId > 0;
        holder.actionRow.setVisibility(hasAction ? View.VISIBLE : View.GONE);
        holder.itemView.setClickable(hasAction);
        holder.itemView.setFocusable(hasAction);
        holder.itemView.setContentDescription(
                "Reporte número " + reportId + ", " + area + ", " + worker + ", " + evalText
                        + (hasAction ? ". Ver detalle" : ""));
        holder.itemView.setOnClickListener(hasAction ? v -> listener.onReportClick(reportId) : null);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView evals;
        final View actionRow;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.kpi_report_title);
            subtitle = itemView.findViewById(R.id.kpi_report_subtitle);
            evals = itemView.findViewById(R.id.kpi_report_evals);
            actionRow = itemView.findViewById(R.id.kpi_report_action_row);
        }
    }
}
