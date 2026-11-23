package pe.net.ws.kpiweb.features.collaborator.attendance;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorDataState;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla "Mis asistencias" del Colaborador.
 * Implementa la interfaz para relacionar la asistencia del colaborador
 * con el sistema anterior según el contrato {@link AttendanceServiceContract}.
 * Muestra porcentaje mensual, asistencias, tardanzas, faltas, justificaciones,
 * horas de entrada/salida y estado de sincronización.
 */
public class CollaboratorAttendanceActivity extends AppCompatActivity {

    public static final String EXTRA_RETURN_ADMIN = "pe.net.ws.kpiweb.RETURN_ADMIN_SCHEDULE";
    public static final String EXTRA_SECTION_MODE = "pe.net.ws.kpiweb.ATTENDANCE_SECTION_MODE";
    public static final String MODE_SCHEDULE = "SCHEDULE";
    public static final String MODE_ATTENDANCE = "ATTENDANCE";

    private CollaboratorAttendanceRepository repository;
    private CollaboratorProfile activeProfile;
    private String selectedMonth;

    private TextView tvSubtitle;
    private TextView tvTitle;
    private TextView tvPercentage;
    private TextView tvSummaryNote;
    private TextView statAttended;
    private TextView statTardiness;
    private TextView statAbsence;
    private TextView statJustified;

    private TextView tabCurrentMonth;
    private TextView tabPrevMonth;
    private LinearLayout containerRecords;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_attendance);

        CollaboratorThemeHelper.setupSystemBars(this);
        repository = CollaboratorAttendanceRepository.getInstance();
        selectedMonth = monthOffset(0);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();

        bindViews();
        updateTabsUI();
        setupListeners();
        loadAttendanceData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        loadAttendanceData();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.collab_att_tv_title);
        tvSubtitle = findViewById(R.id.collab_att_tv_subtitle);
        tvPercentage = findViewById(R.id.collab_att_tv_percentage);
        tvSummaryNote = findViewById(R.id.collab_att_tv_summary_note);
        statAttended = findViewById(R.id.collab_att_stat_attended);
        statTardiness = findViewById(R.id.collab_att_stat_tardiness);
        statAbsence = findViewById(R.id.collab_att_stat_absence);
        statJustified = findViewById(R.id.collab_att_stat_justified);

        tabCurrentMonth = findViewById(R.id.collab_att_tab_month_current);
        tabPrevMonth = findViewById(R.id.collab_att_tab_month_prev);
        containerRecords = findViewById(R.id.collab_att_container_records);
        progressBar = findViewById(R.id.collab_att_progress);

        if (tvSubtitle != null && activeProfile != null) {
            tvSubtitle.setText("Registro laboral y puntualidad • " + activeProfile.getFullName());
        }
    }

    private void setupListeners() {
        View btnBack = findViewById(R.id.collab_att_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnRefresh = findViewById(R.id.collab_att_btn_refresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                CollaboratorBannerHelper.showBanner(
                        this,
                        "Sincronizando asistencias",
                        "Consultando registros del sistema anterior...",
                        R.drawable.collab_ic_refresh
                );
                loadAttendanceData();
            });
        }

        if (tabCurrentMonth != null) {
            tabCurrentMonth.setOnClickListener(v -> {
                selectedMonth = monthOffset(0);
                updateTabsUI();
                loadAttendanceData();
            });
        }

        if (tabPrevMonth != null) {
            tabPrevMonth.setOnClickListener(v -> {
                selectedMonth = monthOffset(-1);
                updateTabsUI();
                loadAttendanceData();
            });
        }
    }

    private void updateTabsUI() {
        boolean isCurrent = monthOffset(0).equals(selectedMonth);
        if (tabCurrentMonth != null) {
            tabCurrentMonth.setText(monthLabel(0));
            tabCurrentMonth.setBackgroundResource(isCurrent ? R.drawable.collab_bg_chip_selected : R.drawable.collab_bg_chip_unselected);
            tabCurrentMonth.setTextColor(isCurrent ? getColor(android.R.color.white) : getColor(R.color.collab_text_secondary));
        }
        if (tabPrevMonth != null) {
            tabPrevMonth.setText(monthLabel(-1));
            tabPrevMonth.setBackgroundResource(!isCurrent ? R.drawable.collab_bg_chip_selected : R.drawable.collab_bg_chip_unselected);
            tabPrevMonth.setTextColor(!isCurrent ? getColor(android.R.color.white) : getColor(R.color.collab_text_secondary));
        }
    }

    private void loadAttendanceData() {
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn() && session.getWorkerId() > 0) {
            if (tvTitle != null) tvTitle.setText("Mi asistencia");
            if (tvSubtitle != null) tvSubtitle.setText("Registro laboral y puntualidad • " + session.getFullName());
            loadRealAttendance();
            return;
        }
        pe.net.ws.kpiweb.navigation.AccountNavigation.login(this, false);
        finish();
    }

    private static String monthOffset(int offset) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        calendar.add(Calendar.MONTH, offset);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return format.format(calendar.getTime());
    }

    private static String monthLabel(int offset) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"));
        calendar.add(Calendar.MONTH, offset);
        SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE"));
        format.setTimeZone(TimeZone.getTimeZone("America/Lima"));
        return format.format(calendar.getTime());
    }

    private static String value(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : "";
    }

    private static int count(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsInt() : 0;
    }

    private static boolean isTrue(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() && row.get(key).getAsBoolean();
    }

    private void loadRealAttendance() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() < 1) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        final String requestedMonth = selectedMonth;
        ApiClient.getService().getOwnAttendance(session.getBearerToken(), requestedMonth)
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override public void onResponse(Call<ApiResponse<JsonObject>> call,
                                                     Response<ApiResponse<JsonObject>> response) {
                        if (!requestedMonth.equals(selectedMonth) || isFinishing()) return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        ApiResponse<JsonObject> body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                            showRealError(); return;
                        }
                        JsonObject data = body.getData();
                        JsonObject summary = data.getAsJsonObject("resumen");
                        if (summary != null) {
                            if (tvPercentage != null) tvPercentage.setText("—");
                            if (tvSummaryNote != null) tvSummaryNote.setText("Marcaciones registradas en " + (requestedMonth.equals(monthOffset(0)) ? monthLabel(0) : monthLabel(-1)) + ". Sin porcentaje estimado.");
                            if (statAttended != null) statAttended.setText(String.valueOf(count(summary, "dias_con_marcacion")));
                            if (statTardiness != null) statTardiness.setText(String.valueOf(count(summary, "dias_con_tardanza")));
                            if (statAbsence != null) statAbsence.setText(String.valueOf(count(summary, "dias_con_falta")));
                            if (statJustified != null) statJustified.setText(String.valueOf(count(summary, "dias_justificados")));
                        }
                        List<AttendanceRecord> records = new ArrayList<>();
                        JsonArray rows = data.getAsJsonArray("registros");
                        if (rows != null) for (int i = 0; i < rows.size(); i++) {
                            JsonObject row = rows.get(i).getAsJsonObject();
                            String date = value(row, "fecha");
                            Date parsed;
                            try { parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(date); }
                            catch (Exception ignored) { continue; }
                            AttendanceRecord.AttendanceStatus status = isTrue(row, "justificada")
                                    ? AttendanceRecord.AttendanceStatus.JUSTIFICADA
                                    : isTrue(row, "falta") ? AttendanceRecord.AttendanceStatus.FALTA
                                    : isTrue(row, "tardanza") ? AttendanceRecord.AttendanceStatus.TARDANZA
                                    : AttendanceRecord.AttendanceStatus.ASISTENCIA;
                            records.add(new AttendanceRecord(date,
                                    new SimpleDateFormat("dd MMM yyyy", new Locale("es", "PE")).format(parsed),
                                    new SimpleDateFormat("EEEE", new Locale("es", "PE")).format(parsed),
                                    shortTime(value(row, "entrada")), shortTime(value(row, "salida")),
                                    status, null));
                        }
                        renderRecords(records);
                    }
                    @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                        if (!requestedMonth.equals(selectedMonth) || isFinishing()) return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        showRealError();
                    }
                });
    }

    private static String shortTime(String time) { return time.length() >= 5 ? time.substring(0, 5) : "—"; }

    private void showRealError() {
        if (tvPercentage != null) tvPercentage.setText("—");
        if (tvSummaryNote != null) tvSummaryNote.setText("No se pudieron consultar las marcaciones del servidor.");
        renderRecords(new ArrayList<>());
    }

    private void loadDailyRecords(String collabId) {
        repository.getDailyRecords(collabId, selectedMonth, new AttendanceServiceContract.AttendanceCallback<List<AttendanceRecord>>() {
            @Override
            public void onSuccess(List<AttendanceRecord> records) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                renderRecords(records);
            }

            @Override
            public void onError(String errorMessage) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                CollaboratorBannerHelper.showBanner(CollaboratorAttendanceActivity.this, "Error de registros", errorMessage, R.drawable.collab_ic_info_circle);
            }
        });
    }

    private void renderSummary(AttendanceSummary summary) {
        if (summary == null) return;
        if (tvPercentage != null) tvPercentage.setText(summary.getFormattedPercentage());
        if (tvSummaryNote != null) tvSummaryNote.setText(summary.getTotalWorkDays() + " días laborales en " + summary.getMonthName());
        if (statAttended != null) statAttended.setText(String.valueOf(summary.getAttendedDays()));
        if (statTardiness != null) statTardiness.setText(String.valueOf(summary.getTardinessDays()));
        if (statAbsence != null) statAbsence.setText(String.valueOf(summary.getAbsenceDays()));
        if (statJustified != null) statJustified.setText(String.valueOf(summary.getJustifiedDays()));
    }

    private void renderRecords(List<AttendanceRecord> records) {
        if (containerRecords == null) return;
        containerRecords.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int pad14 = (int) (14 * density);
        int margin8 = (int) (8 * density);

        if (records == null || records.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No hay registros de marcación disponibles en este período.");
            emptyView.setTextColor(getColor(R.color.collab_text_muted));
            emptyView.setTextSize(13f);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(pad14, pad14 * 2, pad14, pad14 * 2);
            containerRecords.addView(emptyView);
            return;
        }

        for (AttendanceRecord record : records) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.collab_bg_card);
            card.setPadding(pad14, pad14, pad14, pad14);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = margin8;
            card.setLayoutParams(cardLp);

            // Fila 1: Fecha + Pill de Estado
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout dateCol = new LinearLayout(this);
            dateCol.setOrientation(LinearLayout.VERTICAL);
            dateCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDate = new TextView(this);
            tvDate.setText(record.getDateDisplay());
            tvDate.setTextColor(getColor(R.color.collab_text_primary));
            tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            tvDate.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            dateCol.addView(tvDate);

            TextView tvDay = new TextView(this);
            tvDay.setText(record.getDayOfWeek());
            tvDay.setTextColor(getColor(R.color.collab_text_secondary));
            tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            dateCol.addView(tvDay);

            topRow.addView(dateCol);

            TextView tvStatusPill = new TextView(this);
            tvStatusPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvStatusPill.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvStatusPill.setPadding((int) (9 * density), (int) (3 * density), (int) (9 * density), (int) (3 * density));

            switch (record.getStatus()) {
                case TARDANZA:
                    tvStatusPill.setText("Tardanza");
                    tvStatusPill.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                    tvStatusPill.setTextColor(getColor(R.color.collab_status_pending));
                    break;
                case FALTA:
                    tvStatusPill.setText("Falta");
                    tvStatusPill.setBackgroundResource(R.drawable.collab_bg_pill_red);
                    tvStatusPill.setTextColor(getColor(R.color.collab_status_overdue));
                    break;
                case JUSTIFICADA:
                    tvStatusPill.setText("Justificada");
                    tvStatusPill.setBackgroundResource(R.drawable.collab_bg_pill_cyan);
                    tvStatusPill.setTextColor(getColor(R.color.collab_cyan_accent));
                    break;
                case ASISTENCIA:
                default:
                    tvStatusPill.setText("Puntual");
                    tvStatusPill.setBackgroundResource(R.drawable.collab_bg_pill_green);
                    tvStatusPill.setTextColor(getColor(R.color.collab_status_completed));
                    break;
            }
            topRow.addView(tvStatusPill);

            card.addView(topRow);

            // Fila 2: Horarios de Entrada y Salida
            LinearLayout hoursRow = new LinearLayout(this);
            hoursRow.setOrientation(LinearLayout.HORIZONTAL);
            hoursRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hLp.topMargin = (int) (8 * density);
            hoursRow.setLayoutParams(hLp);

            TextView tvEntry = new TextView(this);
            tvEntry.setText("Entrada: " + record.getCheckInTime());
            tvEntry.setTextColor(getColor(R.color.collab_text_secondary));
            tvEntry.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            tvEntry.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            hoursRow.addView(tvEntry);

            TextView tvExit = new TextView(this);
            tvExit.setText("Salida: " + record.getCheckOutTime());
            tvExit.setTextColor(getColor(R.color.collab_text_secondary));
            tvExit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            hoursRow.addView(tvExit);

            card.addView(hoursRow);

            // Fila 3 (Opcional): Justificación
            if (record.hasJustification()) {
                TextView tvJust = new TextView(this);
                tvJust.setText("Motivo / Justificación: " + record.getJustificationNote());
                tvJust.setTextColor(getColor(R.color.collab_purple_accent));
                tvJust.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                LinearLayout.LayoutParams jLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                jLp.topMargin = (int) (6 * density);
                tvJust.setLayoutParams(jLp);
                card.addView(tvJust);
            }

            containerRecords.addView(card);
        }
    }
}

