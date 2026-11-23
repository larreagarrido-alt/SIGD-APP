package pe.net.ws.kpiweb.features.schedule.adapter;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleLevel;

public class LevelScheduleAdapter extends RecyclerView.Adapter<LevelScheduleAdapter.Holder> {

    private final List<ScheduleLevel> items;

    public LevelScheduleAdapter(List<ScheduleLevel> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.schedule_item_level, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ScheduleLevel l = items.get(position);
        int color;
        try {
            color = android.graphics.Color.parseColor(l.colorHex);
        } catch (RuntimeException e) {
            color = ContextCompat.getColor(h.itemView.getContext(), R.color.sch_purple_700);
        }
        h.range.setText(l.range);
        h.desc.setText(l.description);
        h.name.setText(l.level);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(color);
        pill.setCornerRadius(100f * h.itemView.getResources().getDisplayMetrics().density);
        h.name.setBackground(pill);
        h.bar.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name, range, desc;
        final View bar;

        Holder(View v) {
            super(v);
            name = v.findViewById(R.id.schLevelName);
            range = v.findViewById(R.id.schLevelRange);
            desc = v.findViewById(R.id.schLevelDesc);
            bar = v.findViewById(R.id.schLevelBar);
        }
    }
}
