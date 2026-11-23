package pe.net.ws.kpiweb.features.collaborator.notifications;

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
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.NotificationType;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.IconKind;

/**
 * Adaptador oficial del Centro de Notificaciones SIGD del Colaborador.
 */
public class CollaboratorNotificationAdapter extends RecyclerView.Adapter<CollaboratorNotificationAdapter.ViewHolder> {

    public interface OnNotificationActionListener {
        void onItemClick(CollaboratorNotificationItem item);
        void onActionModuleClick(CollaboratorNotificationItem item);
        void onToggleReadState(CollaboratorNotificationItem item);
        void onDeleteClick(CollaboratorNotificationItem item, int position);
    }

    private final Context context;
    private final List<CollaboratorNotificationItem> items = new ArrayList<>();
    private final OnNotificationActionListener listener;
    private boolean allowLocalDelete = false;

    public CollaboratorNotificationAdapter(Context context, OnNotificationActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setAllowLocalDelete(boolean allowLocalDelete) {
        if (this.allowLocalDelete != allowLocalDelete) {
            this.allowLocalDelete = allowLocalDelete;
            notifyDataSetChanged();
        }
    }

    public void setItems(List<CollaboratorNotificationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<CollaboratorNotificationItem> getItems() {
        return items;
    }

    public CollaboratorNotificationItem getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void restoreItem(int position, CollaboratorNotificationItem item) {
        if (item == null) return;
        int targetPos = Math.max(0, Math.min(position, items.size()));
        items.add(targetPos, item);
        notifyItemInserted(targetPos);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.collaborator_item_notification, parent, false);
        CollaboratorThemeHelper.applyReadableTypeface(view);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CollaboratorNotificationItem item = items.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final FrameLayout iconBox;
        final ImageView iconIv;
        final TextView typeBadge;
        final TextView tvDate;
        final View unreadDot;
        final TextView tvTitle;
        final TextView tvMsg;
        final TextView btnAction;
        final TextView btnMarkRead;
        final View deleteSep;
        final TextView btnDelete;
        final TextView dayHeader;

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
            tvMsg = itemView.findViewById(R.id.collab_item_notif_message);
            btnAction = itemView.findViewById(R.id.collab_item_notif_btn_action);
            btnMarkRead = itemView.findViewById(R.id.collab_item_notif_btn_mark_read);
            deleteSep = itemView.findViewById(R.id.collab_item_notif_delete_sep);
            btnDelete = itemView.findViewById(R.id.collab_item_notif_btn_delete);
        }

        void bind(CollaboratorNotificationItem item, int position) {
            if (item == null) return;
            if (dayHeader != null) {
                String group = dayGroup(item.getSortTimestamp());
                boolean show = position == 0 || !group.equals(dayGroup(items.get(position - 1).getSortTimestamp()));
                dayHeader.setVisibility(show ? View.VISIBLE : View.GONE);
                dayHeader.setText(group);
            }

            if (root != null) {
                root.setBackgroundResource(item.isRead()
                        ? R.drawable.collab_bg_notif_card_read
                        : R.drawable.collab_bg_notif_card_unread);
                root.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(item);
                });
            }
            if (unreadDot != null) {
                unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
            }

            if (typeBadge != null) {
                typeBadge.setText(item.getType().getLabel());
            }
            if (tvDate != null) {
                tvDate.setText(item.getDateTimeDisplay());
            }
            if (tvTitle != null) {
                tvTitle.setText(item.getTitle());
                tvTitle.setAlpha(item.isRead() ? 0.88f : 1.0f);
            }
            if (tvMsg != null) {
                tvMsg.setText(item.getMessage());
            }

            if (btnMarkRead != null) {
                btnMarkRead.setText(item.isRead() ? "Marcar no leída" : "Marcar leída");
                btnMarkRead.setOnClickListener(v -> {
                    if (listener != null) listener.onToggleReadState(item);
                });
            }

            if (deleteSep != null) {
                deleteSep.setVisibility(allowLocalDelete ? View.VISIBLE : View.GONE);
            }
            if (btnDelete != null) {
                btnDelete.setVisibility(allowLocalDelete ? View.VISIBLE : View.GONE);
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(item, getBindingAdapterPosition());
                });
            }

            if (btnAction != null) {
                btnAction.setText(item.getTargetModule().getActionLabel() + " →");
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onActionModuleClick(item);
                });
            }

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            if (lp != null) {
                int margin = (int) (10 * context.getResources().getDisplayMetrics().density);
                lp.topMargin = (position > 0) ? margin : 0;
                itemView.setLayoutParams(lp);
            }

            styleNotificationIcon(item.getType());
        }

        private void styleNotificationIcon(NotificationType type) {
            if (iconBox == null || iconIv == null || typeBadge == null) return;
            IconKind kind = CollaboratorNotificationsUiHelper.resolveIconKind(type);
            switch (kind) {
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
                case GIFT:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_amber);
                    iconIv.setImageResource(R.drawable.collab_ic_gift);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_prog_gold));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_prog_gold));
                    break;
                case AWARD:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_green);
                    iconIv.setImageResource(R.drawable.collab_ic_notif_award);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_notif_positive));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_notif_positive));
                    break;
                case HEART_HANDSHAKE:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);
                    iconIv.setImageResource(R.drawable.collab_ic_notif_heart_handshake);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_notif_violet_soft));
                    break;
                case MEGAPHONE:
                    iconBox.setBackgroundResource(R.drawable.collab_bg_icon_badge_cyan);
                    iconIv.setImageResource(R.drawable.collab_ic_notif_megaphone);
                    iconIv.setColorFilter(ContextCompat.getColor(context, R.color.collab_cyan_accent));
                    typeBadge.setBackgroundResource(R.drawable.collab_bg_pill_cyan);
                    typeBadge.setTextColor(ContextCompat.getColor(context, R.color.collab_cyan_accent));
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
