package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.AdminNoticeItem;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.AdminTarget;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.VisualMeta;

/**
 * Adaptador de notificaciones reales del Administrador que reutiliza exactamente
 * la tarjeta visual del Centro de Notificaciones del Colaborador (collaborator_item_notification).
 */
public final class AdminNotificationAdapter extends RecyclerView.Adapter<AdminNotificationAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(AdminNoticeItem item);
        void onActionClick(AdminNoticeItem item, AdminTarget target);
        void onToggleReadClick(AdminNoticeItem item);
    }

    private final Context context;
    private final Listener listener;
    private final List<AdminNoticeItem> items = new ArrayList<>();

    public AdminNotificationAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<AdminNoticeItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.collaborator_item_notification, parent, false);
        AdminTypography.apply(context, view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final TextView dayHeader;
        final FrameLayout iconBox;
        final ImageView iconIv;
        final TextView typeBadge;
        final TextView tvDate;
        final View unreadDot;
        final TextView tvTitle;
        final ImageView chevron;
        final TextView tvMsg;
        final TextView btnAction;
        final TextView btnMarkRead;
        final View deleteSep;
        final TextView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.collab_item_notif_root);
            dayHeader = itemView.findViewById(R.id.collab_item_notif_day_header);
            iconBox = itemView.findViewById(R.id.collab_item_notif_icon_box);
            iconIv = itemView.findViewById(R.id.collab_item_notif_icon);
            typeBadge = itemView.findViewById(R.id.collab_item_notif_type_badge);
            tvDate = itemView.findViewById(R.id.collab_item_notif_date);
            unreadDot = itemView.findViewById(R.id.collab_item_notif_unread_dot);
            tvTitle = itemView.findViewById(R.id.collab_item_notif_title);
            chevron = itemView.findViewById(R.id.collab_item_notif_chevron);
            tvMsg = itemView.findViewById(R.id.collab_item_notif_message);
            btnAction = itemView.findViewById(R.id.collab_item_notif_btn_action);
            btnMarkRead = itemView.findViewById(R.id.collab_item_notif_btn_mark_read);
            deleteSep = itemView.findViewById(R.id.collab_item_notif_delete_sep);
            btnDelete = itemView.findViewById(R.id.collab_item_notif_btn_delete);
        }

        void bind(AdminNoticeItem item, int position) {
            if (item == null) return;
            VisualMeta meta = AdminNotificationsUiHelper.resolveVisualMeta(item.tipo);

            if (dayHeader != null) {
                String group = dayGroup(item.sortTimestamp);
                if (group.isEmpty()) {
                    dayHeader.setVisibility(View.GONE);
                } else {
                    boolean show = position == 0 || !group.equals(dayGroup(items.get(position - 1).sortTimestamp));
                    dayHeader.setVisibility(show ? View.VISIBLE : View.GONE);
                    dayHeader.setText(group);
                }
            }

            if (root != null) {
                root.setBackgroundResource(item.leida
                        ? R.drawable.collab_bg_notif_card_read
                        : R.drawable.collab_bg_notif_card_unread);
                root.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(item);
                });
            }

            if (unreadDot != null) {
                unreadDot.setVisibility(item.leida ? View.GONE : View.VISIBLE);
            }

            if (typeBadge != null) {
                typeBadge.setText(meta.badgeLabel);
            }

            if (tvDate != null) {
                tvDate.setText(item.creadoEn);
                tvDate.setVisibility(item.creadoEn.isEmpty() ? View.GONE : View.VISIBLE);
            }

            if (tvTitle != null) {
                tvTitle.setText(item.titulo.isEmpty() ? meta.badgeLabel : item.titulo);
                tvTitle.setAlpha(item.leida ? 0.88f : 1.0f);
            }

            if (tvMsg != null) {
                tvMsg.setText(item.mensaje);
                tvMsg.setVisibility(item.mensaje.isEmpty() ? View.GONE : View.VISIBLE);
            }

            boolean hasAction = meta.target != AdminTarget.NONE;
            if (chevron != null) {
                chevron.setVisibility(hasAction ? View.VISIBLE : View.GONE);
            }
            if (btnAction != null) {
                if (hasAction) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText(meta.target.actionLabel + " →");
                    btnAction.setOnClickListener(v -> {
                        if (listener != null) listener.onActionClick(item, meta.target);
                    });
                } else {
                    btnAction.setVisibility(View.GONE);
                    btnAction.setOnClickListener(null);
                }
            }

            if (btnMarkRead != null) {
                btnMarkRead.setText(item.leida ? "Marcar no leída" : "Marcar leída");
                btnMarkRead.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleReadClick(item);
                });
            }

            if (deleteSep != null) deleteSep.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            if (lp != null) {
                int margin = (int) (10 * context.getResources().getDisplayMetrics().density);
                lp.topMargin = position > 0 ? margin : 0;
                itemView.setLayoutParams(lp);
            }

            styleIcon(meta);
        }

        private void styleIcon(VisualMeta meta) {
            if (iconBox == null || iconIv == null || typeBadge == null) return;
            switch (meta.iconKind) {
                case FILE_CHECK:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);
                    iconIv.setImageResource(R.drawable.admin_ic_file_check_2);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    break;
                case CLIPBOARD_CHECK:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);
                    iconIv.setImageResource(R.drawable.collab_ic_notif_clipboard_check);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    break;
                case CALENDAR_CLOCK:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_amber);
                    iconIv.setImageResource(R.drawable.collab_ic_notif_calendar_clock);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_prog_gold));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_prog_gold));
                    break;
                case ALARM_CLOCK:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_red);
                    iconIv.setImageResource(R.drawable.collab_ic_notif_alarm_clock);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_status_overdue));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_red);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_status_overdue));
                    break;
                case SHIELD_CHECK:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_green);
                    iconIv.setImageResource(R.drawable.admin_ic_shield_check);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_notif_positive));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_notif_positive));
                    break;
                case USERS_ROUND:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_cyan);
                    iconIv.setImageResource(R.drawable.admin_ic_users_round);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_cyan_accent));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_cyan);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_cyan_accent));
                    break;
                case GIFT:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_amber);
                    iconIv.setImageResource(R.drawable.collab_ic_gift);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_prog_gold));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_prog_gold));
                    break;
                case BELL:
                default:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_lilac);
                    iconIv.setImageResource(R.drawable.collab_ic_nav_bell);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    break;
            }
        }
    }

    private static String dayGroup(long timestamp) {
        if (timestamp <= 0L) return "";
        Calendar now = Calendar.getInstance();
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(timestamp);
        if (now.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) return "Hoy";
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == day.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) return "Ayer";
        return "Anteriores";
    }
}
