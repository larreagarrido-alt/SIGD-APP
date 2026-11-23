package pe.net.ws.kpiweb.features.admin.dashboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.rewards.RewardImageLoader;
import pe.net.ws.kpiweb.features.collaborator.rewards.RewardImageViewer;

/** Optimized adapter for administrative reward publications. */
final class AdminRewardAdapter extends RecyclerView.Adapter<AdminRewardAdapter.Holder> {
    interface Listener {
        void onEdit(JsonObject item);
        void onToggleState(JsonObject item, String nextState);
        void onDelete(JsonObject item);
    }

    private final Activity activity;
    private final List<JsonObject> items = new ArrayList<>();
    private final Listener listener;
    private String bearerToken = "";

    AdminRewardAdapter(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    void submitList(List<JsonObject> newItems, String bearer) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        bearerToken = bearer != null ? bearer : "";
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_reward, parent, false);
        AdminTypography.apply(parent.getContext(), view);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Context ctx = holder.itemView.getContext();
        JsonObject item = items.get(position);

        String title = AdminRewardFormatter.safeString(item, "titulo");
        String description = AdminRewardFormatter.safeString(item, "descripcion");
        String effectiveState = AdminRewardFormatter.safeString(item, "estado_efectivo");
        String pubState = AdminRewardFormatter.safeString(item, "estado_publicacion");
        String start = AdminRewardFormatter.safeString(item, "inicio_en");
        String end = AdminRewardFormatter.safeString(item, "fin_en");
        String imagePath = AdminRewardFormatter.safeString(item, "imagen_url");
        int pointsRequired = AdminRewardFormatter.safeInt(item, "puntos_requeridos");

        if (!imagePath.isEmpty()) {
            holder.imageWrap.setVisibility(View.VISIBLE);
            holder.image.setImageDrawable(null);
            RewardImageLoader.load(activity, holder.image, null, imagePath, bearerToken);
            holder.imageWrap.setContentDescription("Ampliar imagen de " + title);
            holder.imageWrap.setOnClickListener(v -> RewardImageViewer.show(activity, imagePath, bearerToken, null));
        } else {
            holder.imageWrap.setVisibility(View.GONE);
            holder.imageWrap.setOnClickListener(null);
        }

        holder.title.setText(title.isEmpty() ? "Sin título" : title);
        holder.state.setText(AdminRewardFormatter.formatStateLabel(effectiveState));
        applyStatePill(ctx, holder.state, effectiveState);

        if (holder.pointsWrap != null && holder.points != null) {
            if (pointsRequired > 0) {
                holder.points.setText(NumberFormat.getIntegerInstance(Locale.US).format(pointsRequired) + " pts");
                holder.pointsWrap.setVisibility(View.VISIBLE);
            } else {
                holder.pointsWrap.setVisibility(View.GONE);
            }
        }

        holder.description.setText(description);
        holder.description.setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);

        if (start.isEmpty() && end.isEmpty()) {
            holder.dates.setText("Sin programación definida");
        } else {
            holder.dates.setText("Inicio: " + AdminRewardFormatter.formatCompactDateTime(start)
                    + "  ·  Vence: " + AdminRewardFormatter.formatCompactDateTime(end));
        }

        boolean isDraft = "BORRADOR".equals(pubState);
        boolean isDeactivated = "DESACTIVADO".equals(pubState);
        String nextToggleState = isDeactivated ? "PUBLICADO" : "DESACTIVADO";
        String toggleLabel = isDeactivated ? "Reactivar" : "Desactivar";

        holder.editBtn.setOnClickListener(v -> listener.onEdit(item));
        if (isDraft) {
            holder.toggleBtn.setVisibility(View.GONE);
        } else {
            holder.toggleBtn.setVisibility(View.VISIBLE);
            holder.toggleBtn.setText(toggleLabel);
            holder.toggleBtn.setOnClickListener(v -> listener.onToggleState(item, nextToggleState));
        }

        holder.moreBtn.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(ctx, v);
            menu.getMenu().add(Menu.NONE, 1, 1, "Editar / renovar");
            if (!isDraft) {
                menu.getMenu().add(Menu.NONE, 2, 2, toggleLabel);
            }
            menu.getMenu().add(Menu.NONE, 3, 3, "Eliminar");
            menu.setOnMenuItemClickListener(mi -> {
                if (mi.getItemId() == 1) {
                    listener.onEdit(item);
                    return true;
                } else if (mi.getItemId() == 2) {
                    listener.onToggleState(item, nextToggleState);
                    return true;
                } else if (mi.getItemId() == 3) {
                    listener.onDelete(item);
                    return true;
                }
                return false;
            });
            menu.show();
        });
    }

    private void applyStatePill(Context ctx, TextView badge, String state) {
        int color = stateColor(ctx, state);
        badge.setTextColor(color);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(99f * ctx.getResources().getDisplayMetrics().density);
        bg.setColor(Color.argb(34, Color.red(color), Color.green(color), Color.blue(color)));
        bg.setStroke(Math.max(1, Math.round(ctx.getResources().getDisplayMetrics().density)),
                Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)));
        badge.setBackground(bg);
    }

    private int stateColor(Context ctx, String state) {
        if ("ACTIVO".equals(state)) return androidx.core.content.ContextCompat.getColor(ctx, R.color.admin_status_delivered);
        if ("PROGRAMADO".equals(state)) return androidx.core.content.ContextCompat.getColor(ctx, R.color.admin_purple_accent);
        if ("VENCIDO".equals(state)) return androidx.core.content.ContextCompat.getColor(ctx, R.color.admin_status_pending);
        if ("BORRADOR".equals(state)) return androidx.core.content.ContextCompat.getColor(ctx, R.color.admin_cyan_brain);
        if ("DESACTIVADO".equals(state)) return androidx.core.content.ContextCompat.getColor(ctx, R.color.admin_status_overdue);
        return androidx.core.content.ContextCompat.getColor(ctx, R.color.admin_text_secondary);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View imageWrap;
        final ImageView image;
        final TextView title;
        final TextView state;
        final View pointsWrap;
        final TextView points;
        final TextView description;
        final TextView dates;
        final TextView editBtn;
        final TextView toggleBtn;
        final View moreBtn;

        Holder(@NonNull View itemView) {
            super(itemView);
            imageWrap = itemView.findViewById(R.id.reward_item_image_wrap);
            image = itemView.findViewById(R.id.reward_item_image);
            title = itemView.findViewById(R.id.reward_item_title);
            state = itemView.findViewById(R.id.reward_item_state);
            pointsWrap = itemView.findViewById(R.id.reward_item_points_wrap);
            points = itemView.findViewById(R.id.reward_item_points);
            description = itemView.findViewById(R.id.reward_item_description);
            dates = itemView.findViewById(R.id.reward_item_dates);
            editBtn = itemView.findViewById(R.id.reward_item_btn_edit);
            toggleBtn = itemView.findViewById(R.id.reward_item_btn_toggle);
            moreBtn = itemView.findViewById(R.id.reward_item_more);
        }
    }
}
