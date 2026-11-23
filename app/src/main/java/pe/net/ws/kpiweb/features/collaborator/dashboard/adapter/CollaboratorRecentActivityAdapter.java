package pe.net.ws.kpiweb.features.collaborator.dashboard.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorActivityItem;

/**
 * Adaptador para el listado de Actividad Reciente del Colaborador.
 */
public class CollaboratorRecentActivityAdapter extends RecyclerView.Adapter<CollaboratorRecentActivityAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CollaboratorActivityItem item);
    }

    private final List<CollaboratorActivityItem> items = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public CollaboratorRecentActivityAdapter() {
    }

    public CollaboratorRecentActivityAdapter(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setItems(List<CollaboratorActivityItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.collaborator_item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CollaboratorActivityItem item = items.get(position);
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

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.collab_iv_item_icon);
            tvTitle = itemView.findViewById(R.id.collab_tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.collab_tv_item_subtitle);
            tvTime = itemView.findViewById(R.id.collab_tv_item_time);
        }

        public void bind(CollaboratorActivityItem item) {
            Context context = itemView.getContext();
            tvTitle.setText(item.getTitle());
            tvSubtitle.setText(item.getCategory());
            tvTime.setText(item.getTimeLabel());

            if (item.getType() == CollaboratorActivityItem.ActivityType.ENTREGA) {
                ivIcon.setImageResource(R.drawable.collaborator_ic_check_circle);
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.admin_status_delivered));
            } else if (item.getType() == CollaboratorActivityItem.ActivityType.ANUNCIO) {
                ivIcon.setImageResource(R.drawable.collaborator_ic_document);
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.admin_cyan_brain));
            } else {
                ivIcon.setImageResource(R.drawable.collaborator_ic_star);
                ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.admin_purple_accent));
            }
        }
    }
}
