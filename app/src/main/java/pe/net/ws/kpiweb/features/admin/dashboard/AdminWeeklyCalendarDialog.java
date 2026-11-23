package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import pe.net.ws.kpiweb.R;

/**
 * Selector semanal interactivo y moderno para el Dashboard del Administrador.
 * Adapta automáticamente sus colores al Modo Claro y Modo Oscuro de SIGD.
 */
public final class AdminWeeklyCalendarDialog {

    public interface OnWeekSelectedListener {
        void onWeekSelected(int year, int month, int dayOfMonth);
    }

    private static final String[] MONTHS_ES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private static final String[] WEEKDAYS_ES = {"L", "M", "X", "J", "V", "S", "D"};

    private AdminWeeklyCalendarDialog() { }

    public static void show(Context context, Calendar initialDate, OnWeekSelectedListener listener) {
        View root = LayoutInflater.from(context).inflate(R.layout.admin_dialog_weekly_calendar, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        final Calendar today = Calendar.getInstance();
        clearTime(today);

        final Calendar selected = Calendar.getInstance();
        if (initialDate != null) {
            selected.setTimeInMillis(initialDate.getTimeInMillis());
        }
        clearTime(selected);

        final Calendar displayedMonth = (Calendar) selected.clone();
        displayedMonth.set(Calendar.DAY_OF_MONTH, 1);

        TextView tvSelectedWeek = root.findViewById(R.id.cal_tv_selected_week);
        TextView tvSelectedRange = root.findViewById(R.id.cal_tv_selected_range);
        TextView tvMonthYear = root.findViewById(R.id.cal_tv_month_year);
        ImageButton btnPrevMonth = root.findViewById(R.id.cal_btn_prev_month);
        ImageButton btnNextMonth = root.findViewById(R.id.cal_btn_next_month);
        View btnThisWeek = root.findViewById(R.id.cal_btn_this_week);
        LinearLayout weekdaysRow = root.findViewById(R.id.cal_weekdays_row);
        LinearLayout daysGrid = root.findViewById(R.id.cal_days_grid);
        View btnCancel = root.findViewById(R.id.cal_btn_cancel);
        View btnApply = root.findViewById(R.id.cal_btn_apply);

        int textMuted = ContextCompat.getColor(context, R.color.admin_text_muted);
        for (String wd : WEEKDAYS_ES) {
            TextView tv = new TextView(context);
            tv.setText(wd);
            tv.setTextColor(textMuted);
            tv.setTextSize(11.5f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            weekdaysRow.addView(tv, new LinearLayout.LayoutParams(0, dp(context, 26), 1f));
        }

        final Runnable[] renderRef = new Runnable[1];
        renderRef[0] = () -> {
            tvSelectedWeek.setText(AdminDashboardPeriod.formatWeekNumberLabel(selected));
            tvSelectedRange.setText(AdminDashboardPeriod.formatWeekRangeLong(selected));
            tvMonthYear.setText(MONTHS_ES[displayedMonth.get(Calendar.MONTH)] + " "
                    + displayedMonth.get(Calendar.YEAR));

            boolean canGoNextMonth = displayedMonth.get(Calendar.YEAR) < today.get(Calendar.YEAR)
                    || (displayedMonth.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && displayedMonth.get(Calendar.MONTH) < today.get(Calendar.MONTH));
            btnNextMonth.setEnabled(canGoNextMonth);
            btnNextMonth.setAlpha(canGoNextMonth ? 1f : 0.35f);

            renderDaysGrid(context, daysGrid, displayedMonth, selected, today, day -> {
                selected.setTimeInMillis(day.getTimeInMillis());
                renderRef[0].run();
            });
        };

        btnPrevMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, -1);
            renderRef[0].run();
        });
        btnNextMonth.setOnClickListener(v -> {
            displayedMonth.add(Calendar.MONTH, 1);
            renderRef[0].run();
        });
        btnThisWeek.setOnClickListener(v -> {
            selected.setTimeInMillis(today.getTimeInMillis());
            displayedMonth.setTimeInMillis(today.getTimeInMillis());
            displayedMonth.set(Calendar.DAY_OF_MONTH, 1);
            renderRef[0].run();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onWeekSelected(
                        selected.get(Calendar.YEAR),
                        selected.get(Calendar.MONTH),
                        selected.get(Calendar.DAY_OF_MONTH)
                );
            }
        });

        renderRef[0].run();
        dialog.show();
    }

    private interface OnDayClick {
        void onDayClick(Calendar day);
    }

    private static void renderDaysGrid(Context context, LinearLayout grid, Calendar monthStart,
                                       Calendar selected, Calendar today, OnDayClick onDayClick) {
        grid.removeAllViews();

        int textPrimary = ContextCompat.getColor(context, R.color.admin_text_primary);
        int textMuted = ContextCompat.getColor(context, R.color.admin_text_muted);
        int purplePrimary = ContextCompat.getColor(context, R.color.admin_purple_primary);
        int weekBandBg = ContextCompat.getColor(context, R.color.admin_surface_tertiary);

        String selectedWeekKey = AdminDashboardPeriod.fromSelectedDate(selected);
        int targetMonth = monthStart.get(Calendar.MONTH);

        GregorianCalendar cursor = new GregorianCalendar(monthStart.getTimeZone(), Locale.ROOT);
        cursor.setTimeInMillis(monthStart.getTimeInMillis());
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        int dow = cursor.get(Calendar.DAY_OF_WEEK);
        int offset = dow == Calendar.SUNDAY ? 6 : (dow - Calendar.MONDAY);
        cursor.add(Calendar.DAY_OF_MONTH, -offset);

        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            LinearLayout weekRow = new LinearLayout(context);
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setWeightSum(7f);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.topMargin = dp(context, 4);

            Calendar rowMid = (Calendar) cursor.clone();
            rowMid.add(Calendar.DAY_OF_MONTH, 3);
            boolean isSelectedWeekRow = selectedWeekKey.equals(AdminDashboardPeriod.fromSelectedDate(rowMid));
            if (isSelectedWeekRow) {
                GradientDrawable band = new GradientDrawable();
                band.setColor(weekBandBg);
                band.setCornerRadius(dp(context, 12));
                weekRow.setBackground(band);
            }

            boolean rowHasCurrentMonthDay = false;
            for (int col = 0; col < 7; col++) {
                final Calendar cellDay = (Calendar) cursor.clone();
                clearTime(cellDay);
                boolean inMonth = cellDay.get(Calendar.MONTH) == targetMonth;
                if (inMonth) rowHasCurrentMonthDay = true;
                boolean isFuture = cellDay.after(today);
                boolean isToday = sameDay(cellDay, today);
                boolean isSelectedDay = sameDay(cellDay, selected);

                TextView tv = new TextView(context);
                tv.setText(String.valueOf(cellDay.get(Calendar.DAY_OF_MONTH)));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(13f);

                if (isSelectedDay) {
                    GradientDrawable selBg = new GradientDrawable();
                    selBg.setShape(GradientDrawable.OVAL);
                    selBg.setColor(purplePrimary);
                    tv.setBackground(selBg);
                    tv.setTextColor(Color.WHITE);
                    tv.setTypeface(null, Typeface.BOLD);
                } else if (isToday) {
                    GradientDrawable todayBg = new GradientDrawable();
                    todayBg.setShape(GradientDrawable.OVAL);
                    todayBg.setColor(Color.TRANSPARENT);
                    todayBg.setStroke(dp(context, 2), purplePrimary);
                    tv.setBackground(todayBg);
                    tv.setTextColor(purplePrimary);
                    tv.setTypeface(null, Typeface.BOLD);
                } else {
                    tv.setTextColor(inMonth ? textPrimary : textMuted);
                    if (!inMonth) tv.setAlpha(0.45f);
                }

                if (isFuture) {
                    tv.setEnabled(false);
                    tv.setAlpha(0.28f);
                } else {
                    tv.setClickable(true);
                    tv.setFocusable(true);
                    tv.setOnClickListener(v -> onDayClick.onDayClick(cellDay));
                }

                LinearLayout cellWrap = new LinearLayout(context);
                cellWrap.setGravity(Gravity.CENTER);
                cellWrap.addView(tv, new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)));
                weekRow.addView(cellWrap, new LinearLayout.LayoutParams(0, dp(context, 40), 1f));

                cursor.add(Calendar.DAY_OF_MONTH, 1);
            }

            if (rowIdx < 5 || rowHasCurrentMonthDay) {
                grid.addView(weekRow, rowLp);
            }
        }
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
