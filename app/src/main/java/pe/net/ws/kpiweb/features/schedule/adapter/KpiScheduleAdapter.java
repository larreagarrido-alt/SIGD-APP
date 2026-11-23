package pe.net.ws.kpiweb.features.schedule.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;

public class KpiScheduleAdapter extends RecyclerView.Adapter<KpiScheduleAdapter.Holder> {

    private final List<ScheduleKpi> items;

    public KpiScheduleAdapter(List<ScheduleKpi> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.schedule_item_kpi, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ScheduleKpi k = items.get(position);
        h.name.setText(k.name);
        h.target.setText(k.target);
        h.freq.setText(k.chipText != null ? k.chipText
                : h.itemView.getContext().getString(k.weekly ? R.string.sch_freq_weekly : R.string.sch_freq_monthly));
        h.freq.setBackgroundResource(k.weekly ? R.drawable.sch_bg_pill_weekly : R.drawable.sch_bg_pill_today);
        h.freq.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                k.weekly ? R.color.sch_purple_700 : R.color.sch_amber_dark));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name, target, freq;

        Holder(View v) {
            super(v);
            name = v.findViewById(R.id.schKpiName);
            target = v.findViewById(R.id.schKpiTarget);
            freq = v.findViewById(R.id.schKpiFreq);
        }
    }
}
