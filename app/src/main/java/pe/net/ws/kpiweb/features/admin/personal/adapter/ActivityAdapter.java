package pe.net.ws.kpiweb.features.admin.personal.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalTypography;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeActivityItem;

/**
 * Adaptador para las actividades asignadas al colaborador.
 * Aplica colores semánticos (Verde, Ámbar, Rojo, Violeta) según el estado de la tarea.
 */
public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private final List<EmployeeActivityItem> items = new ArrayList<>();

    public void submitList(List<EmployeeActivityItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.personal_item_activity, parent, false);
        AdminPersonalTypography.apply(view);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvCategory;
        private final TextView tvStatus;
        private final TextView tvDesc;
        private final TextView tvDeadline;
        private final TextView tvProgressVal;
        private final ProgressBar pbProgress;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_activity_icon);
            tvTitle = itemView.findViewById(R.id.tv_activity_title);
            tvCategory = itemView.findViewById(R.id.tv_activity_category);
            tvStatus = itemView.findViewById(R.id.tv_activity_status);
            tvDesc = itemView.findViewById(R.id.tv_activity_desc);
            tvDeadline = itemView.findViewById(R.id.tv_activity_deadline);
            tvProgressVal = itemView.findViewById(R.id.tv_activity_progress_val);
            pbProgress = itemView.findViewById(R.id.pb_activity_progress);
        }

        public void bind(EmployeeActivityItem item) {
            tvTitle.setText(item.getTitle());
            tvCategory.setText(item.getCategory());
            tvDesc.setText(item.getDescription() != null ? item.getDescription() : item.getTitle());
            tvDeadline.setText("Límite: " + item.getDeadline());

            String status = item.getStatus() != null ? item.getStatus() : "Pendiente";
            tvStatus.setText(status);

            int colorText;
            int colorBg;

            if ("Completada".equalsIgnoreCase(status)) {
                colorText = 0xFF10B981; // Verde
                colorBg = 0x2610B981;
            } else if ("En progreso".equalsIgnoreCase(status)) {
                colorText = 0xFFF59E0B; // Ámbar
                colorBg = 0x26F59E0B;
            } else if ("Vencida".equalsIgnoreCase(status)) {
                colorText = 0xFFEF4444; // Rojo
                colorBg = 0x26EF4444;
            } else {
                colorText = 0xFFBB75FF; // Violeta
                colorBg = 0x268738F5;
            }

            tvStatus.setTextColor(colorText);
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(colorBg));

            int progress = item.getProgressPercent();
            tvProgressVal.setText(progress + "%");
            pbProgress.setProgress(progress);

            if ("Completada".equalsIgnoreCase(status)) {
                ivIcon.setImageResource(R.drawable.personal_ic_check_circle);
            } else {
                ivIcon.setImageResource(R.drawable.personal_ic_task);
            }
        }
    }
}
