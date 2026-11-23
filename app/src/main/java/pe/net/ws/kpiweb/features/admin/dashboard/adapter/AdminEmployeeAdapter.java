package pe.net.ws.kpiweb.features.admin.dashboard.adapter;

import android.content.Context;
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
import pe.net.ws.kpiweb.features.admin.dashboard.model.AdminEmployeeKpiItem;

/**
 * Adaptador para la lista de colaboradores en el dashboard administrativo.
 */
public class AdminEmployeeAdapter extends RecyclerView.Adapter<AdminEmployeeAdapter.EmployeeViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(AdminEmployeeKpiItem item);
    }

    private final List<AdminEmployeeKpiItem> items = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setItems(List<AdminEmployeeKpiItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        AdminEmployeeKpiItem item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvRole;
        private final TextView tvStatus;
        private final TextView tvAssigned;
        private final TextView tvDelivered;
        private final TextView tvPending;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.admin_tv_employee_initials);
            tvName = itemView.findViewById(R.id.admin_tv_employee_name);
            tvRole = itemView.findViewById(R.id.admin_tv_employee_role);
            tvStatus = itemView.findViewById(R.id.admin_tv_employee_status);
            tvAssigned = itemView.findViewById(R.id.admin_tv_emp_assigned_val);
            tvDelivered = itemView.findViewById(R.id.admin_tv_emp_delivered_val);
            tvPending = itemView.findViewById(R.id.admin_tv_emp_pending_val);
        }

        public void bind(AdminEmployeeKpiItem item) {
            Context context = itemView.getContext();
            tvName.setText(item.getName());
            tvRole.setText(item.getRole());
            tvAssigned.setText(String.valueOf(item.getAssigned()));
            tvDelivered.setText(String.valueOf(item.getDelivered()));
            tvPending.setText(String.valueOf(item.getPending()));

            // Obtener iniciales
            String initials = getInitials(item.getName());
            tvInitials.setText(initials);

            // Estado y color
            tvStatus.setText(item.getStatusLabel());
            switch (item.getStatusType()) {
                case ON_TRACK:
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_delivered));
                    break;
                case PENDING:
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_pending));
                    break;
                case AT_RISK:
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.admin_status_overdue));
                    break;
            }
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "US";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (parts[0].length() >= 2) {
                return parts[0].substring(0, 2).toUpperCase();
            }
            return parts[0].toUpperCase();
        }
    }
}
