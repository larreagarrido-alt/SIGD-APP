package pe.net.ws.kpiweb.features.kpi.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport;

/**
 * Adaptador para listar los registros diarios de actividades (Historial KPI).
 */
public class KpiListAdapter extends RecyclerView.Adapter<KpiListAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(KpiDailyReport report);
    }

    private final Context context;
    private final List<KpiDailyReport> reportList;
    private final OnReportClickListener listener;

    public KpiListAdapter(Context context, List<KpiDailyReport> reportList, OnReportClickListener listener) {
        this.context = context;
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.kpi_item_list_card, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        KpiDailyReport report = reportList.get(position);
        holder.bind(report);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvListCardDate;
        TextView tvListCardProductivity;
        TextView tvListCardPracticante;
        TextView tvListCardTurno;
        TextView tvListCardActivitiesCount;
        TextView tvListCardBugsCount;

        ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvListCardDate = itemView.findViewById(R.id.tvListCardDate);
            tvListCardProductivity = itemView.findViewById(R.id.tvListCardProductivity);
            tvListCardPracticante = itemView.findViewById(R.id.tvListCardPracticante);
            tvListCardTurno = itemView.findViewById(R.id.tvListCardTurno);
            tvListCardActivitiesCount = itemView.findViewById(R.id.tvListCardActivitiesCount);
            tvListCardBugsCount = itemView.findViewById(R.id.tvListCardBugsCount);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onReportClick(reportList.get(getAdapterPosition()));
                }
            });
        }

        void bind(KpiDailyReport report) {
            tvListCardDate.setText(report.getFecha());
            tvListCardProductivity.setText(report.getNivelProductividad() + " (" + report.getCumplimientoMeta() + "%)");
            tvListCardPracticante.setText(report.getPracticante());
            tvListCardTurno.setText("Turno: " + report.getTurno());
            tvListCardActivitiesCount.setText(report.getTareasAsignadas() + " Actividades (" + report.getTareasCompletadas() + " Entregadas)");
            tvListCardBugsCount.setText(report.getBugsDetectados() + " Bugs");
        }
    }
}
