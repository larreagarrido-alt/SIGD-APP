package pe.net.ws.kpiweb.features.schedule.adapter;

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

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.schedule.data.ScheduleData;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleProfile;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleWorkday;
import pe.net.ws.kpiweb.features.schedule.model.Slot;

/** Línea de tiempo del día: cabecera, franjas, frase motivacional y pilares. */
public class SlotTimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int T_HEADER = 0, T_SLOT = 1, T_QUOTE = 2, T_PILLARS = 3;

    private final ScheduleProfile profile;
    private final List<Slot> allSlots;
    private final List<Slot> visible = new ArrayList<>();
    private int dayIndex = 0;
    private boolean today = false;
    private int nowMin = -1;
    private int nowPosition = -1;
    private boolean personalCalendarRequired;
    private ScheduleWorkday personalDay;

    public SlotTimelineAdapter(ScheduleProfile profile) {
        this.profile = profile;
        this.allSlots = profile.slots;
    }

    public void setPersonalDay(boolean required, ScheduleWorkday day) {
        personalCalendarRequired = required;
        personalDay = day;
    }

    /** Cambia de día y/o actualiza la hora. nowMin se ignora si el día no es hoy. */
    public void update(int dayIndex, boolean isToday, int nowMin) {
        this.dayIndex = dayIndex;
        this.today = isToday;
        this.nowMin = nowMin;
        visible.clear();
        for (Slot s : allSlots) {
            if (s.isVisibleOn(dayIndex) && allowedByPersonalDay(s)) {
                visible.add(s);
            }
        }
        nowPosition = -1;
        if (today) {
            for (int i = 0; i < visible.size(); i++) {
                Slot s = visible.get(i);
                if (s.kind == Slot.Kind.OFF) {
                    continue;
                }
                int next = i + 1 < visible.size() ? visible.get(i + 1).startMin : s.endMin + 1;
                if (nowMin >= s.startMin && nowMin < next) {
                    nowPosition = i;
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    private boolean allowedByPersonalDay(Slot slot) {
        if (!personalCalendarRequired) return true;
        if (personalDay == null || !personalDay.hasShift()) return false;
        if (personalDay.manana && personalDay.tarde) return true;
        if (slot.kind == Slot.Kind.BREAK || slot.kind == Slot.Kind.OFF) return false;
        return personalDay.manana ? slot.startMin < 14 * 60 : slot.startMin >= 14 * 60;
    }

    /** Posición del adaptador (incluye cabecera) de la franja actual, o -1. */
    public int getNowAdapterPosition() {
        return nowPosition < 0 ? -1 : nowPosition + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return T_HEADER;
        if (position <= visible.size()) return T_SLOT;
        if (profile.hasQuote() && position == visible.size() + 1) return T_QUOTE;
        return T_PILLARS;
    }

    @Override
    public int getItemCount() {
        return visible.size() + (profile.hasQuote() ? 3 : 2);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case T_HEADER:
                return new HeaderHolder(inf.inflate(R.layout.schedule_item_header, parent, false));
            case T_SLOT:
                return new SlotHolder(inf.inflate(R.layout.schedule_item_slot, parent, false));
            case T_QUOTE:
                return new StaticHolder(inf.inflate(R.layout.schedule_item_quote, parent, false));
            default:
                return new StaticHolder(inf.inflate(R.layout.schedule_item_pillars, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Context c = holder.itemView.getContext();
        if (holder instanceof HeaderHolder) {
            bindHeader((HeaderHolder) holder, c);
        } else if (holder instanceof SlotHolder) {
            bindSlot((SlotHolder) holder, c, position - 1);
        } else if (getItemViewType(position) == T_QUOTE) {
            ((TextView) holder.itemView.findViewById(R.id.schQuoteText)).setText(profile.quote);
        } else {
            bindPillars(holder.itemView);
        }
    }

    private void bindPillars(View v) {
        int[] ids = {R.id.schPillar1, R.id.schPillar2, R.id.schPillar3, R.id.schPillar4, R.id.schPillar5};
        View box5 = v.findViewById(R.id.schPillarBox5);
        if (box5 != null) {
            box5.setVisibility(profile.pillars.length >= 5 ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < ids.length && i < profile.pillars.length; i++) {
            ((TextView) v.findViewById(ids[i])).setText(profile.pillars[i]);
        }
        TextView motto = v.findViewById(R.id.schMotto);
        motto.setText(profile.motto);
        motto.setVisibility(profile.motto == null || profile.motto.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String endLabel(Slot s) {
        int i = s.label.indexOf(" - ");
        return i >= 0 ? s.label.substring(i + 3) : s.label;
    }

    private void bindHeader(HeaderHolder h, Context c) {
        h.title.setText(ScheduleData.DAYS_FULL[dayIndex]);
        h.today.setVisibility(today ? View.VISIBLE : View.GONE);
        h.note.setVisibility(profile.generated ? View.VISIBLE : View.GONE);
        if (personalCalendarRequired && personalDay == null) {
            h.sub.setText("No se pudo verificar tu jornada en Asistencias. Actualiza la pantalla.");
            return;
        }
        if (personalCalendarRequired && !personalDay.hasShift()) {
            String reason = personalDay.motivo;
            h.sub.setText(reason == null || reason.trim().isEmpty()
                    ? "Sin turno exigible para este día" : reason);
            return;
        }
        int count = 0;
        Slot first = null;
        Slot last = null;
        for (Slot s : visible) {
            if (s.kind == Slot.Kind.BREAK || s.kind == Slot.Kind.OFF) continue;
            count++;
            if (first == null) first = s;
            last = s;
        }
        if (first == null) {
            h.sub.setText(R.string.sch_day_off_summary);
        } else {
            h.sub.setText(c.getString(R.string.sch_day_summary,
                    Slot.fmt(first.startMin), endLabel(last), count));
        }
    }

    private void bindSlot(SlotHolder h, Context c, int index) {
        Slot s = visible.get(index);
        boolean isNow = index == nowPosition;
        String text = s.itemFor(dayIndex);

        h.time.setText(s.label);
        h.activity.setText(text.toUpperCase(java.util.Locale.getDefault()));
        h.now.setVisibility(isNow ? View.VISIBLE : View.GONE);
        h.line.setVisibility(View.VISIBLE);

        boolean amber = isNow || s.kind == Slot.Kind.BREAK;
        h.card.setStrokeColor(ContextCompat.getColor(c,
                amber ? R.color.sch_amber : R.color.sch_line));
        h.card.setStrokeWidth(dp(c, amber ? 1.5f : 0.75f));
        h.card.setCardBackgroundColor(ContextCompat.getColor(c,
                s.kind == Slot.Kind.BREAK ? R.color.sch_amber_soft : R.color.sch_card));
        h.activity.setGravity(s.kind == Slot.Kind.BREAK
                ? android.view.Gravity.CENTER_HORIZONTAL : android.view.Gravity.START);
        h.time.setGravity(s.kind == Slot.Kind.BREAK
                ? android.view.Gravity.CENTER_HORIZONTAL : android.view.Gravity.START);
        h.activity.setTextColor(ContextCompat.getColor(c,
                s.kind == Slot.Kind.BREAK ? R.color.sch_amber_dark : R.color.sch_text));

        int dotBg;
        int icon;
        int tint = android.graphics.Color.WHITE;
        if (isNow) {
            dotBg = R.drawable.sch_bg_dot_now;
            tint = ContextCompat.getColor(c, R.color.sch_amber_dark);
            icon = R.drawable.sch_ic_schedule;
        } else if (s.kind == Slot.Kind.ENTRY) {
            dotBg = R.drawable.sch_bg_dot_entry;
            tint = ContextCompat.getColor(c, R.color.sch_purple_700);
            icon = R.drawable.sch_ic_login;
        } else if (s.kind == Slot.Kind.EXIT) {
            dotBg = R.drawable.sch_bg_dot;
            icon = R.drawable.sch_ic_logout;
        } else if (s.kind == Slot.Kind.BREAK) {
            dotBg = R.drawable.sch_bg_dot_break;
            tint = ContextCompat.getColor(c, R.color.sch_amber_dark);
            icon = R.drawable.sch_ic_breakfast;
        } else if (s.kind == Slot.Kind.OFF) {
            dotBg = R.drawable.sch_bg_dot_entry;
            tint = ContextCompat.getColor(c, R.color.sch_purple_700);
            icon = R.drawable.sch_ic_home;
        } else {
            dotBg = R.drawable.sch_bg_dot;
            icon = R.drawable.sch_ic_schedule;
        }
        h.dot.setBackgroundResource(dotBg);
        h.dotIcon.setImageResource(icon);
        h.dotIcon.setColorFilter(tint);
    }

    private static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title, sub, today, note;

        HeaderHolder(View v) {
            super(v);
            title = v.findViewById(R.id.schHeadTitle);
            sub = v.findViewById(R.id.schHeadSub);
            today = v.findViewById(R.id.schHeadToday);
            note = v.findViewById(R.id.schHeadNote);
        }
    }

    static class SlotHolder extends RecyclerView.ViewHolder {
        final TextView time, activity, now;
        final MaterialCardView card;
        final FrameLayout dot;
        final ImageView dotIcon;
        final View line;

        SlotHolder(View v) {
            super(v);
            time = v.findViewById(R.id.schTime);
            activity = v.findViewById(R.id.schActivity);
            now = v.findViewById(R.id.schNow);
            card = v.findViewById(R.id.schCard);
            dot = v.findViewById(R.id.schDot);
            dotIcon = v.findViewById(R.id.schDotIcon);
            line = v.findViewById(R.id.schLine);
        }
    }

    static class StaticHolder extends RecyclerView.ViewHolder {
        StaticHolder(View v) {
            super(v);
        }
    }
}
