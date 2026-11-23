package pe.net.ws.kpiweb.features.admin.personal.adapter;

import android.content.Context;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalTypography;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalUiHelper;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader;

/**
 * Adapter premium para la lista de colaboradores en el módulo Personal.
 * Presenta avatar con indicador de estado, identidad legible con soporte para
 * nombres largos,
 * badge de estado real y acciones directas a Reportes y Ver perfil.
 */
public class PersonalAdapter extends RecyclerView.Adapter<PersonalAdapter.EmployeeViewHolder> {

    public interface OnEmployeeClickListener {
        void onEmployeeClick(EmployeeItem item);
    }

    public interface OnReportClickListener {
        void onReportClick(EmployeeItem item);
    }

    private final List<EmployeeItem> items = new ArrayList<>();
    private OnEmployeeClickListener listener;
    private OnReportClickListener reportListener;
    private boolean highlightReportsAction = false;

    public void setOnEmployeeClickListener(OnEmployeeClickListener listener) {
        this.listener = listener;
    }

    public void setOnReportClickListener(OnReportClickListener listener) {
        this.reportListener = listener;
    }

    public void setHighlightReportsAction(boolean highlight) {
        if (this.highlightReportsAction != highlight) {
            this.highlightReportsAction = highlight;
            notifyDataSetChanged();
        }
    }

    public void submitList(List<EmployeeItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.personal_item_collaborator, parent, false);
        AdminPersonalTypography.apply(view);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        EmployeeItem item = items.get(position);
        holder.bind(item, listener, reportListener, highlightReportsAction);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {

        private final FrameLayout avatarContainer;
        private final View statusDot;
        private final TextView tvInitials;
        private final ImageView ivPhoto;
        private final TextView tvName;
        private final TextView tvStatus;
        private final View roleRow;
        private final TextView tvRole;
        private final View metaRow;
        private final TextView tvMeta;
        private final View kpiRow;
        private final TextView tvKpiBadge;
        private final TextView tvActivities;
        private final View btnReports;
        private final View btnProfile;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarContainer = itemView.findViewById(R.id.item_personal_avatar_container);
            statusDot = itemView.findViewById(R.id.item_personal_status_dot);
            tvInitials = itemView.findViewById(R.id.item_personal_tv_initials);
            ivPhoto = itemView.findViewById(R.id.item_personal_iv_photo);
            tvName = itemView.findViewById(R.id.item_personal_tv_name);
            tvStatus = itemView.findViewById(R.id.item_personal_tv_status);
            roleRow = itemView.findViewById(R.id.item_personal_role_row);
            tvRole = itemView.findViewById(R.id.item_personal_tv_role);
            metaRow = itemView.findViewById(R.id.item_personal_meta_row);
            tvMeta = itemView.findViewById(R.id.item_personal_tv_meta);
            kpiRow = itemView.findViewById(R.id.item_personal_kpi_row);
            tvKpiBadge = itemView.findViewById(R.id.item_personal_tv_kpi_badge);
            tvActivities = itemView.findViewById(R.id.item_personal_tv_activities);
            btnReports = itemView.findViewById(R.id.item_personal_btn_reports);
            btnProfile = itemView.findViewById(R.id.item_personal_btn_profile);
        }

        public void bind(EmployeeItem item, OnEmployeeClickListener listener,
                OnReportClickListener reportListener, boolean highlightReports) {
            Context ctx = itemView.getContext();
            float density = ctx.getResources().getDisplayMetrics().density;

            tvName.setText(item.getName());
            tvInitials.setText(item.getAvatarInitials());
            if (ctx instanceof Activity) {
                int workerId;
                try { workerId = Integer.parseInt(item.getId()); }
                catch (NumberFormatException ignored) { workerId = 0; }
                CollaboratorProfilePhotoLoader.load((Activity) ctx, ivPhoto, tvInitials,
                        item.getPhotoUrl(), workerId);
            }

            String role = item.getRole() != null ? item.getRole().trim() : "";
            if (role.isEmpty()) {
                roleRow.setVisibility(View.GONE);
            } else {
                roleRow.setVisibility(View.VISIBLE);
                tvRole.setText(role);
            }

            String dept = item.getDepartment() != null ? item.getDepartment().trim() : "";
            String id = item.getId() != null ? item.getId().trim() : "";
            String metaText;
            if (!dept.isEmpty() && !id.isEmpty()) {
                metaText = dept + " · #" + id;
            } else if (!dept.isEmpty()) {
                metaText = dept;
            } else if (!id.isEmpty()) {
                metaText = "Código #" + id;
            } else {
                metaText = "";
            }
            if (metaText.isEmpty()) {
                metaRow.setVisibility(View.GONE);
            } else {
                metaRow.setVisibility(View.VISIBLE);
                tvMeta.setText(metaText);
            }

            // Avatar circular con borde violeta sutil y tono cohesivo SIGD
            int avatarColor = AdminPersonalUiHelper.avatarColorFor(item.getName(), item.getId());
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(avatarColor);
            avatarBg.setStroke(Math.round(1.5f * density), 0x66D4A9FF);
            avatarContainer.setBackground(avatarBg);

            // Resumen opcional de KPI y actividades
            boolean hasKpi = item.getKpiSummaryScore() > 0;
            boolean hasActivities = item.getAssignedActivities() > 0;
            if (hasKpi || hasActivities) {
                kpiRow.setVisibility(View.VISIBLE);
                if (hasKpi) {
                    tvKpiBadge.setVisibility(View.VISIBLE);
                    tvKpiBadge.setText(item.getKpiSummaryScore() + "% KPI");
                } else {
                    tvKpiBadge.setVisibility(View.GONE);
                }
                if (hasActivities) {
                    tvActivities.setVisibility(View.VISIBLE);
                    tvActivities.setText(ctx.getString(
                            R.string.personal_card_activities_summary,
                            item.getCompletedActivities(),
                            item.getAssignedActivities()));
                } else {
                    tvActivities.setVisibility(View.GONE);
                }
            } else {
                kpiRow.setVisibility(View.GONE);
            }

            // Estado laboral real con pill diferenciada e indicador circular en avatar
            EmployeeItem.WorkStatus status = item.getWorkStatus();
            int dotColor;
            switch (status) {
                case ACTIVO:
                    tvStatus.setText("Activo");
                    tvStatus.setBackgroundResource(R.drawable.personal_bg_status_active_pill);
                    dotColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_status_delivered);
                    tvStatus.setTextColor(dotColor);
                    break;
                case INACTIVO:
                    tvStatus.setText("Inactivo");
                    tvStatus.setBackgroundResource(R.drawable.personal_bg_status_inactive_pill);
                    dotColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_semantic_red);
                    tvStatus.setTextColor(dotColor);
                    break;
                case LICENCIA:
                case CAPACITACION:
                    tvStatus.setText("Permiso");
                    tvStatus.setBackgroundResource(R.drawable.personal_bg_status_leave_pill);
                    dotColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_cyan_brain);
                    tvStatus.setTextColor(dotColor);
                    break;
                case VACACIONES:
                    tvStatus.setText("Vacaciones");
                    tvStatus.setBackgroundResource(R.drawable.personal_bg_status_vacation);
                    dotColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_status_pending);
                    tvStatus.setTextColor(dotColor);
                    break;
                case SIN_DATOS:
                default:
                    tvStatus.setText(status != null ? status.getDisplayName() : "Sin estado");
                    tvStatus.setBackgroundResource(R.drawable.personal_bg_chip_unselected_premium);
                    tvStatus.setTextColor(
                            androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_text_secondary));
                    dotColor = androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_text_muted);
                    break;
            }

            GradientDrawable dotDrawable = new GradientDrawable();
            dotDrawable.setShape(GradientDrawable.OVAL);
            dotDrawable.setColor(dotColor);
            dotDrawable.setStroke(Math.round(2f * density),
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.personal_card_bg));
            statusDot.setBackground(dotDrawable);

            // Jerarquía visual de botones reales (Reportes / Ver perfil)
            btnReports.setBackgroundResource(highlightReports
                    ? R.drawable.personal_bg_btn_primary_premium
                    : R.drawable.personal_bg_btn_secondary_premium);
            btnProfile.setBackgroundResource(highlightReports
                    ? R.drawable.personal_bg_btn_secondary_premium
                    : R.drawable.personal_bg_btn_primary_premium);

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onEmployeeClick(item);
            });
            btnProfile.setOnClickListener(v -> {
                if (listener != null)
                    listener.onEmployeeClick(item);
            });
            btnReports.setOnClickListener(v -> {
                if (reportListener != null)
                    reportListener.onReportClick(item);
            });
        }
    }
}
