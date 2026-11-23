package pe.net.ws.kpiweb.features.kpi.junior.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.notification.KpiReminderScheduler;
import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorActivity;
import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorDayRecord;
import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorProductionGoal;
import pe.net.ws.kpiweb.features.kpi.junior.ui.view.KpiJuniorWeeklyChartView;
import pe.net.ws.kpiweb.features.kpi.ui.KpiCustomKeypadDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiRoleSelectorFragment;

/**
 * Módulo Propio e Independiente del Diseñador Gráfico Junior (⚡ Practicante).
 * Conserva todas sus secciones originales y aplica el flujo semanal
 * Lunes–Sábado:
 * abre en el día real actual, permite retroceder a días previos pendientes,
 * bloquea con opacidad
 * en modo solo lectura los días ya enviados, y avanza automáticamente con
 * scroll hacia arriba.
 */
public class KpiJuniorContainerFragment extends Fragment {

    private static final String[] SHORT_DAYS = { "LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB" };
    private static final String[] FULL_DAYS = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado" };
    private static final int[] CAL_DAYS = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    };

    private final Calendar selectedCal = Calendar.getInstance();
    private KpiJuniorDayRecord currentRecord;

    private TextView tabFormBtn, tabReportBtn;
    private ScrollView formScroll;
    private View reportScroll;

    // Vistas Formulario Junior
    private TextView tvSelectedDate;
    private TextView tvReadOnlyNotice;
    private final TextView[] dayPills = new TextView[6];
    private String currentShiftTag = "MANANA";
    private TextView tvAttendanceNotice;
    private TextView tvShiftMorningPill, tvShiftAfternoonPill;
    private Spinner spShift;
    private LinearLayout goalSectionCard;
    private final LinearLayout[] goalCards = new LinearLayout[3];
    private LinearLayout actSectionCard;
    private LinearLayout activitiesListContainer;
    private TextView btnAddPiece;
    private LinearLayout evalFieldsContainer;
    private TextView tvLiveMetricsSummary;
    private TextView tvScoreBadge;
    private CheckBox cbGoalMet, cbOnTime, cbBriefing, cbQuality, cbFiles, cbComm;
    private TextView tvObservationsBox;
    private TextView btnSubmitFinalAction;

    // Vistas Reporte Junior
    private KpiJuniorWeeklyChartView weeklyChartView;
    private TextView tvWeeklySummaryStats;
    private LinearLayout weeklyBreakdownContainer;

    public static KpiJuniorContainerFragment newInstance() {
        return new KpiJuniorContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        KpiDefaultSessionManager.getInstance().setActiveRoleAndMode("Diseñador Gráfico Junior", true);
        KpiJuniorMemoryRepository.init(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_light_bg));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildHeader());

        FrameLayout contentFrame = new FrameLayout(requireContext());
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        formScroll = buildFormTab();
        contentFrame.addView(formScroll);
        root.addView(contentFrame);

        Calendar openCal = Calendar.getInstance();
        if (openCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            openCal.add(Calendar.DAY_OF_MONTH, -1);
        }
        int todayIdx = getTodayWeekDayIndex();
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay todayAtt = attRepo.getDayAttendance(todayIdx);
        if (todayAtt == null || (!todayAtt.morningAttended && !todayAtt.afternoonAttended)) {
            for (int i = todayIdx - 1; i >= 0; i--) {
                KpiAttendanceRepository.AttendanceDay a = attRepo.getDayAttendance(i);
                if (a != null && (a.morningAttended || a.afternoonAttended)) {
                    openCal = getWeekDayCalendar(i);
                    break;
                }
            }
        }
        loadDateRecord(openCal);
        return root;
    }

    private void scrollFormToTop() {
        if (formScroll != null) {
            formScroll.post(() -> formScroll.smoothScrollTo(0, 0));
        }
    }

    private Calendar getWeekDayCalendar(int dayIndex0To5) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int curr = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (curr == Calendar.SUNDAY) ? 6 : (curr - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff + dayIndex0To5);
        return cal;
    }

    private boolean isWeekDaySent(int dayIndex0To5) {
        if (dayIndex0To5 > getTodayWeekDayIndex())
            return false;
        SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String dStr = dfShort.format(getWeekDayCalendar(dayIndex0To5).getTime());
        if (getContext() == null)
            return false;
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(dayIndex0To5);
        if (att == null)
            return false;
        if (!att.morningAttended && !att.afternoonAttended)
            return false;

        boolean morningOk = !att.morningAttended;
        if (att.morningAttended) {
            KpiJuniorDayRecord mr = KpiJuniorMemoryRepository.getInstance().getShiftRecord(dStr, "MANANA");
            morningOk = mr != null && mr.isBiometricVerified();
        }

        boolean afternoonOk = !att.afternoonAttended;
        if (att.afternoonAttended) {
            KpiJuniorDayRecord ar = KpiJuniorMemoryRepository.getInstance().getShiftRecord(dStr, "TARDE");
            afternoonOk = ar != null && ar.isBiometricVerified();
        }

        return morningOk && afternoonOk;
    }

    private int getTodayWeekDayIndex() {
        Calendar now = Calendar.getInstance();
        switch (now.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            default:
                return 5;
        }
    }

    private int getSelectedWeekDayIndex() {
        int dow = selectedCal.get(Calendar.DAY_OF_WEEK);
        for (int i = 0; i < CAL_DAYS.length; i++) {
            if (CAL_DAYS[i] == dow)
                return i;
        }
        return 5;
    }

    private int getNextUnsentWeekDayIndexAfter(int fromIdx) {
        int todayIdx = getTodayWeekDayIndex();
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        for (int i = 0; i <= todayIdx; i++) {
            if (i == fromIdx)
                continue;
            KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(i);
            if (att != null && (att.morningAttended || att.afternoonAttended) && !isWeekDaySent(i)) {
                return i;
            }
        }
        return -1;
    }

    private void jumpToNextPendingJuniorDay() {
        int nextIdx = getNextUnsentWeekDayIndexAfter(getSelectedWeekDayIndex());
        if (nextIdx != -1) {
            loadDateRecord(getWeekDayCalendar(nextIdx));
            scrollFormToTop();
            Toast.makeText(getContext(),
                    "➔ Cargando " + FULL_DAYS[nextIdx] + " (pendiente de llenar)",
                    Toast.LENGTH_SHORT).show();
        } else {
            scrollFormToTop();
            Toast.makeText(getContext(),
                    "✓ Todos los días de la semana ya fueron enviados",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundResource(R.drawable.kpi_bg_header_electric);
        header.setPadding(dp(16), dp(16), dp(16), dp(14));

        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnBack = new TextView(requireContext());
        btnBack.setText("←");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(20f);
        btnBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnBack.setGravity(Gravity.CENTER);
        btnBack.setMinimumWidth(dp(44));
        btnBack.setMinimumHeight(dp(44));
        btnBack.setOnClickListener(v -> navigateBack());
        if (!(requireActivity() instanceof pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity)) topRow.addView(btnBack);

        LinearLayout titleCol = new LinearLayout(requireContext());
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = dp(8);
        titleCol.setLayoutParams(colLp);

        TextView tvBadge = new TextView(requireContext());
        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance()
                .getAuthenticatedUserName();
        String prefix = "PRACTICANTE: ";
        tvBadge.setText((authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+"))
                ? prefix + authName.toUpperCase(java.util.Locale.getDefault()) + " · JUNIOR"
                : "PRACTICANTE · DISEÑADOR GRÁFICO JUNIOR");
        tvBadge.setTextColor(Color.WHITE);
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvBadge);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("Ficha Diaria de Piezas y Evaluación");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        titleCol.addView(tvTitle);

        topRow.addView(titleCol);

        TextView btnAlarmTools = new TextView(requireContext());
        btnAlarmTools.setText("🔔 Alarmas / BD");
        btnAlarmTools.setTextSize(11f);
        btnAlarmTools.setTypeface(Typeface.DEFAULT_BOLD);
        btnAlarmTools.setTextColor(Color.WHITE);
        btnAlarmTools.setGravity(Gravity.CENTER);
        btnAlarmTools.setBackgroundResource(R.drawable.kpi_bg_translucent_pill);
        btnAlarmTools.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams almPillLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        almPillLp.rightMargin = dp(6);
        btnAlarmTools.setLayoutParams(almPillLp);
        btnAlarmTools.setOnClickListener(v -> showAlarmAndAttendanceDialog());
        topRow.addView(btnAlarmTools);

        TextView btnGoToReport = new TextView(requireContext());
        btnGoToReport.setText("Reporte");
        btnGoToReport.setTextSize(12f);
        btnGoToReport.setTypeface(Typeface.DEFAULT_BOLD);
        btnGoToReport.setTextColor(Color.WHITE);
        btnGoToReport.setGravity(Gravity.CENTER);
        btnGoToReport.setBackgroundResource(R.drawable.kpi_bg_translucent_pill);
        btnGoToReport.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnGoToReport.setOnClickListener(v -> {
            if (getActivity() != null) {
                String collabName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance()
                        .getAuthenticatedUserName();
                String cargoName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance()
                        .getAuthenticatedRole();
                if (cargoName == null || cargoName.trim().isEmpty()) {
                    cargoName = "Diseñador Gráfico Junior";
                }
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main,
                                pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment.newInstanceExclusive(cargoName,
                                        collabName))
                        .addToBackStack(null)
                        .commit();
            }
        });
        topRow.addView(btnGoToReport);

        header.addView(topRow);
        return header;
    }

    private ScrollView buildFormTab() {
        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(28));

        // 1. Selector de Fecha y Lunes a Sábado
        LinearLayout dateCard = createSectionCard();
        LinearLayout dateRow = new LinearLayout(requireContext());
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);

        tvSelectedDate = new TextView(requireContext());
        tvSelectedDate.setTextSize(14f);
        tvSelectedDate.setTypeface(Typeface.DEFAULT_BOLD);
        tvSelectedDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tvSelectedDate.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvSelectedDate.setBackgroundResource(R.drawable.kpi_junior_bg_input);
        tvSelectedDate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvSelectedDate.setOnClickListener(v -> openDatePicker());
        dateRow.addView(tvSelectedDate);

        TextView btnToday = new TextView(requireContext());
        btnToday.setText("HOY");
        btnToday.setTextSize(12f);
        btnToday.setTypeface(Typeface.DEFAULT_BOLD);
        btnToday.setGravity(Gravity.CENTER);
        btnToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        btnToday.setBackgroundResource(R.drawable.kpi_bg_goal_card_selected);
        LinearLayout.LayoutParams todayLp = new LinearLayout.LayoutParams(dp(64), dp(44));
        todayLp.leftMargin = dp(8);
        btnToday.setLayoutParams(todayLp);
        btnToday.setOnClickListener(v -> {
            Calendar t = Calendar.getInstance();
            if (t.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                t.add(Calendar.DAY_OF_MONTH, -1);
            loadDateRecord(t);
            scrollFormToTop();
        });
        dateRow.addView(btnToday);
        dateCard.addView(dateRow);

        // Barra Lunes a Sábado
        LinearLayout weekRow = new LinearLayout(requireContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams weekLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weekLp.topMargin = dp(10);
        weekRow.setLayoutParams(weekLp);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            final int targetDow = CAL_DAYS[i];
            TextView pill = new TextView(requireContext());
            pill.setText(SHORT_DAYS[i]);
            pill.setGravity(Gravity.CENTER);
            pill.setTextSize(12f);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i < 5)
                pLp.rightMargin = dp(4);
            pill.setLayoutParams(pLp);
            pill.setOnClickListener(v -> {
                int todayIdx = getTodayWeekDayIndex();
                KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
                KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(idx);
                boolean hasAtt = (att != null && (att.morningAttended || att.afternoonAttended));

                if (!hasAtt) {
                    Toast.makeText(getContext(),
                            "Sin turno de asistencia en BD para " + FULL_DAYS[idx] + " (Día no programado)",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (idx > todayIdx) {
                    Toast.makeText(getContext(),
                            "El día " + FULL_DAYS[idx]
                                    + " aún no ha comenzado. Solo puedes registrar hasta el día de hoy.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                jumpToWeekDay(targetDow);
                scrollFormToTop();
            });
            dayPills[i] = pill;
            weekRow.addView(pill);
        }
        dateCard.addView(weekRow);

        tvReadOnlyNotice = new TextView(requireContext());
        tvReadOnlyNotice.setTextSize(12f);
        tvReadOnlyNotice.setTypeface(Typeface.DEFAULT_BOLD);
        tvReadOnlyNotice.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
        tvReadOnlyNotice.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
        tvReadOnlyNotice.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams roLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        roLp.topMargin = dp(10);
        tvReadOnlyNotice.setLayoutParams(roLp);
        tvReadOnlyNotice.setVisibility(View.GONE);
        tvReadOnlyNotice.setOnClickListener(v -> jumpToNextPendingJuniorDay());
        dateCard.addView(tvReadOnlyNotice);

        // 1.1 Asistencia en BD (Sincronizada con base de datos MySQL)
        tvAttendanceNotice = new TextView(requireContext());
        tvAttendanceNotice.setTextSize(11.5f);
        tvAttendanceNotice.setTypeface(Typeface.DEFAULT_BOLD);
        tvAttendanceNotice.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        tvAttendanceNotice.setBackgroundResource(R.drawable.kpi_bg_goal_card_selected);
        tvAttendanceNotice.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams attLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        attLp.topMargin = dp(10);
        tvAttendanceNotice.setLayoutParams(attLp);
        dateCard.addView(tvAttendanceNotice);

        // 1.2 Selector de Doble Turno del Practicante (Mañana y Tarde)
        LinearLayout shiftTabsRow = new LinearLayout(requireContext());
        shiftTabsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams shiftTabsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shiftTabsLp.topMargin = dp(8);
        shiftTabsRow.setLayoutParams(shiftTabsLp);

        tvShiftMorningPill = new TextView(requireContext());
        tvShiftMorningPill.setText("Turno Mañana");
        tvShiftMorningPill.setTextSize(11f);
        tvShiftMorningPill.setIncludeFontPadding(false);
        tvShiftMorningPill.setTypeface(Typeface.DEFAULT_BOLD);
        tvShiftMorningPill.setGravity(Gravity.CENTER);
        tvShiftMorningPill.setPadding(dp(8), dp(8), dp(8), dp(8));
        tvShiftMorningPill.setMinimumHeight(dp(54));
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        smLp.rightMargin = dp(4);
        tvShiftMorningPill.setLayoutParams(smLp);
        tvShiftMorningPill.setOnClickListener(v -> {
            if (!"MANANA".equals(currentShiftTag)) {
                persistCurrentJuniorRecord(currentRecord != null && currentRecord.isBiometricVerified());
                currentShiftTag = "MANANA";
                loadDateRecord(selectedCal);
                scrollFormToTop();
            }
        });
        shiftTabsRow.addView(tvShiftMorningPill);

        tvShiftAfternoonPill = new TextView(requireContext());
        tvShiftAfternoonPill.setText("Turno Tarde");
        tvShiftAfternoonPill.setTextSize(11f);
        tvShiftAfternoonPill.setIncludeFontPadding(false);
        tvShiftAfternoonPill.setTypeface(Typeface.DEFAULT_BOLD);
        tvShiftAfternoonPill.setGravity(Gravity.CENTER);
        tvShiftAfternoonPill.setPadding(dp(8), dp(8), dp(8), dp(8));
        tvShiftAfternoonPill.setMinimumHeight(dp(54));
        LinearLayout.LayoutParams saLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        saLp.leftMargin = dp(4);
        tvShiftAfternoonPill.setLayoutParams(saLp);
        tvShiftAfternoonPill.setOnClickListener(v -> {
            if (!"TARDE".equals(currentShiftTag)) {
                persistCurrentJuniorRecord(currentRecord != null && currentRecord.isBiometricVerified());
                currentShiftTag = "TARDE";
                loadDateRecord(selectedCal);
                scrollFormToTop();
            }
        });
        shiftTabsRow.addView(tvShiftAfternoonPill);
        dateCard.addView(shiftTabsRow);

        spShift = new Spinner(requireContext());
        String[] shifts = { "Turno Mañana", "Turno Tarde" };
        spShift.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, shifts));
        spShift.setVisibility(View.GONE);
        dateCard.addView(spShift);

        body.addView(dateCard);

        // 2. Modalidad de Meta Diaria Propia del Junior (Estilo Programador Jr:
        // horizontal cards)
        goalSectionCard = createSectionCard();
        LinearLayout headerMetaRow = new LinearLayout(requireContext());
        headerMetaRow.setOrientation(LinearLayout.HORIZONTAL);
        headerMetaRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView tvMetaTitle = createSectionTitle("1. MODALIDAD DE META DIARIA (DISEÑADOR JR)");
        tvMetaTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerMetaRow.addView(tvMetaTitle);
        TextView tvDesliza = new TextView(requireContext());
        tvDesliza.setText("Desliza →");
        tvDesliza.setTextSize(11f);
        tvDesliza.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        headerMetaRow.addView(tvDesliza);
        goalSectionCard.addView(headerMetaRow);

        android.widget.HorizontalScrollView hsvGoals = new android.widget.HorizontalScrollView(requireContext());
        hsvGoals.setHorizontalScrollBarEnabled(false);
        hsvGoals.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.topMargin = dp(8);
        hsvGoals.setLayoutParams(hsvLp);

        LinearLayout llGoalsRow = new LinearLayout(requireContext());
        llGoalsRow.setOrientation(LinearLayout.HORIZONTAL);

        KpiJuniorProductionGoal[] goals = KpiJuniorProductionGoal.values();
        int[] goalResIcons = { R.drawable.kpi_ic_palette, R.drawable.kpi_ic_video, R.drawable.kpi_ic_target };
        for (int i = 0; i < goals.length; i++) {
            final KpiJuniorProductionGoal g = goals[i];
            final int resIcon = goalResIcons[i];

            LinearLayout opt = new LinearLayout(requireContext());
            opt.setOrientation(LinearLayout.HORIZONTAL);
            opt.setGravity(Gravity.CENTER_VERTICAL);
            opt.setPadding(dp(12), dp(8), dp(12), dp(8));
            opt.setMinimumWidth(dp(130));
            LinearLayout.LayoutParams optLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0)
                optLp.leftMargin = dp(8);
            opt.setLayoutParams(optLp);

            // Columna izquierda: Icono arriba y Nombre abajo
            LinearLayout leftCol = new LinearLayout(requireContext());
            leftCol.setOrientation(LinearLayout.VERTICAL);
            leftCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            android.widget.ImageView ivIcon = new android.widget.ImageView(requireContext());
            ivIcon.setImageResource(resIcon);
            ivIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(22), dp(22));
            ivLp.bottomMargin = dp(4);
            leftCol.addView(ivIcon, ivLp);

            TextView tvGTitle = new TextView(requireContext());
            tvGTitle.setText(g.getTitle());
            tvGTitle.setTextSize(11.5f);
            tvGTitle.setTypeface(Typeface.DEFAULT_BOLD);
            tvGTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            leftCol.addView(tvGTitle);
            opt.addView(leftCol);

            // Costado derecho: Número de la meta
            TextView tvGTarget = new TextView(requireContext());
            tvGTarget.setText(String.valueOf(g.getTargetTotalPieces()));
            tvGTarget.setTextSize(18f);
            tvGTarget.setTypeface(Typeface.DEFAULT_BOLD);
            tvGTarget.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
            tvGTarget.setPadding(dp(10), 0, 0, 0);
            opt.addView(tvGTarget);

            opt.setOnClickListener(v -> {
                if (currentRecord != null && currentRecord.isBiometricVerified()) {
                    jumpToNextPendingJuniorDay();
                    return;
                }
                if (currentRecord != null) {
                    currentRecord.setGoal(g);
                    currentRecord.recalculateMetrics();
                    refreshGoalSelectionUi();
                    refreshLiveSummaryUi();
                }
            });
            goalCards[i] = opt;
            llGoalsRow.addView(opt);
        }
        hsvGoals.addView(llGoalsRow);
        goalSectionCard.addView(hsvGoals);
        body.addView(goalSectionCard);

        // 3. Lista de Piezas / Actividades Realizadas por el Junior
        actSectionCard = createSectionCard();
        actSectionCard.addView(createSectionTitle("2. REGISTRO DE PIEZAS Y ACTIVIDADES DEL DÍA"));

        activitiesListContainer = new LinearLayout(requireContext());
        activitiesListContainer.setOrientation(LinearLayout.VERTICAL);
        actSectionCard.addView(activitiesListContainer);

        btnAddPiece = new TextView(requireContext());
        btnAddPiece.setText("Agregar Pieza Gráfica o Video Realizado");
        btnAddPiece.setGravity(Gravity.CENTER);
        btnAddPiece.setTextSize(13f);
        btnAddPiece.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddPiece.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        btnAddPiece.setBackgroundResource(R.drawable.kpi_bg_goal_card_selected);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        addLp.topMargin = dp(10);
        btnAddPiece.setLayoutParams(addLp);
        btnAddPiece.setOnClickListener(v -> {
            if (currentRecord != null && currentRecord.isBiometricVerified()) {
                jumpToNextPendingJuniorDay();
                return;
            }
            showAddOrEditActivityDialog(null);
        });
        actSectionCard.addView(btnAddPiece);

        body.addView(actSectionCard);

        // 4. Resumen en Vivo + 6 Criterios de Evaluación Rápida Automática
        LinearLayout evalCard = createSectionCard();
        evalFieldsContainer = new LinearLayout(requireContext());
        evalFieldsContainer.setOrientation(LinearLayout.VERTICAL);
        evalFieldsContainer.addView(createSectionTitle("3. CUMPLIMIENTO Y EVALUACIÓN AUTOMÁTICA"));

        TextView tvEvalSub = new TextView(requireContext());
        tvEvalSub.setText("Calculado en tiempo real según las piezas y actividades registradas:");
        tvEvalSub.setTextSize(11f);
        tvEvalSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        tvEvalSub.setPadding(0, 0, 0, dp(4));
        evalFieldsContainer.addView(tvEvalSub);

        tvLiveMetricsSummary = new TextView(requireContext());
        tvLiveMetricsSummary.setTextSize(13f);
        tvLiveMetricsSummary.setTypeface(Typeface.DEFAULT_BOLD);
        tvLiveMetricsSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        tvLiveMetricsSummary.setPadding(0, dp(6), 0, dp(4));
        evalFieldsContainer.addView(tvLiveMetricsSummary);

        tvScoreBadge = new TextView(requireContext());
        tvScoreBadge.setTextSize(13f);
        tvScoreBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvScoreBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
        tvScoreBadge.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
        tvScoreBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        evalFieldsContainer.addView(tvScoreBadge);

        cbGoalMet = createEvalCheck("Cumplió la meta diaria de piezas según modalidad");
        cbOnTime = createEvalCheck("Entregó todas las piezas registradas (sin pendientes)");
        cbBriefing = createEvalCheck("Produjo los formatos requeridos (posts/videos)");
        cbQuality = createEvalCheck("Calidad óptima verificada (sin exceso de correcciones)");
        cbFiles = createEvalCheck("Piezas terminadas y ordenadas");
        cbComm = createEvalCheck("Sin incidencias ni bloqueos reportados");

        evalFieldsContainer.addView(cbGoalMet);
        evalFieldsContainer.addView(cbOnTime);
        evalFieldsContainer.addView(cbBriefing);
        evalFieldsContainer.addView(cbQuality);
        evalFieldsContainer.addView(cbFiles);
        evalFieldsContainer.addView(cbComm);

        TextView tvObsLabel = createSectionTitle("4. OBSERVACIONES / INCIDENCIAS DEL PRACTICANTE");
        LinearLayout.LayoutParams obsLblLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        obsLblLp.topMargin = dp(12);
        tvObsLabel.setLayoutParams(obsLblLp);
        evalFieldsContainer.addView(tvObsLabel);

        tvObservationsBox = new TextView(requireContext());
        tvObservationsBox.setHint("Toca para anotar observaciones o aprendizajes del día...");
        tvObservationsBox.setTextSize(12f);
        tvObservationsBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tvObservationsBox.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_gray_light));
        tvObservationsBox.setBackgroundResource(R.drawable.kpi_junior_bg_input);
        tvObservationsBox.setPadding(dp(12), dp(12), dp(12), dp(12));
        tvObservationsBox.setMinimumHeight(dp(56));
        LinearLayout.LayoutParams obsBoxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        obsBoxLp.topMargin = dp(6);
        tvObservationsBox.setLayoutParams(obsBoxLp);
        tvObservationsBox.setOnClickListener(v -> {
            if (currentRecord != null && currentRecord.isBiometricVerified()) {
                jumpToNextPendingJuniorDay();
                return;
            }
            KpiCustomKeypadDialog.showText(
                    requireContext(),
                    "Observaciones · Diseñador Jr",
                    "Comentarios o incidencias del día",
                    tvObservationsBox.getText().toString(),
                    text -> {
                        tvObservationsBox.setText(text);
                        if (currentRecord != null)
                            currentRecord.setSupervisorObservation(text);
                    });
        });
        evalFieldsContainer.addView(tvObservationsBox);
        evalCard.addView(evalFieldsContainer);

        btnSubmitFinalAction = new TextView(requireContext());
        btnSubmitFinalAction.setText("Enviar Ficha del Día");
        btnSubmitFinalAction.setGravity(Gravity.CENTER);
        btnSubmitFinalAction.setTextSize(14f);
        btnSubmitFinalAction.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitFinalAction.setTextColor(Color.WHITE);
        btnSubmitFinalAction.setBackgroundResource(R.drawable.kpi_bg_btn_electric);
        LinearLayout.LayoutParams finalLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        finalLp.topMargin = dp(14);
        btnSubmitFinalAction.setLayoutParams(finalLp);
        btnSubmitFinalAction.setOnClickListener(v -> {
            if (currentRecord != null && currentRecord.isBiometricVerified()) {
                jumpToNextPendingJuniorDay();
            } else {
                submitFinalWithBiometrics();
            }
        });
        evalCard.addView(btnSubmitFinalAction);

        body.addView(evalCard);
        sv.addView(body);
        return sv;
    }

    private View buildReportTab() {
        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(28));

        LinearLayout chartCard = createSectionCard();
        chartCard.addView(createSectionTitle("PRODUCCIÓN SEMANAL · POSTS VS VIDEOS"));

        tvWeeklySummaryStats = new TextView(requireContext());
        tvWeeklySummaryStats.setTextSize(13f);
        tvWeeklySummaryStats.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        tvWeeklySummaryStats.setPadding(0, dp(4), 0, dp(8));
        chartCard.addView(tvWeeklySummaryStats);

        weeklyChartView = new KpiJuniorWeeklyChartView(requireContext());
        weeklyChartView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        chartCard.addView(weeklyChartView);
        body.addView(chartCard);

        LinearLayout detailCard = createSectionCard();
        detailCard.addView(createSectionTitle("HISTORIAL DE CALIFICACIONES DEL PRACTICANTE"));
        weeklyBreakdownContainer = new LinearLayout(requireContext());
        weeklyBreakdownContainer.setOrientation(LinearLayout.VERTICAL);
        detailCard.addView(weeklyBreakdownContainer);
        body.addView(detailCard);

        sv.addView(body);
        return sv;
    }

    private CheckBox createEvalCheck(String label) {
        CheckBox cb = new CheckBox(requireContext());
        cb.setText(label);
        cb.setTextSize(12f);
        cb.setClickable(false);
        cb.setFocusable(false);
        cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        return cb;
    }

    private void syncChecksToRecord() {
        if (currentRecord == null)
            return;
        currentRecord.recalculateMetrics();
    }

    private void loadDateRecord(Calendar cal) {
        selectedCal.setTime(cal.getTime());
        SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        SimpleDateFormat dfFull = new SimpleDateFormat("EEEE dd/MM/yyyy", new Locale("es", "ES"));
        String shortDate = dfShort.format(selectedCal.getTime());
        String fullText = dfFull.format(selectedCal.getTime());
        tvSelectedDate.setText(Character.toUpperCase(fullText.charAt(0)) + fullText.substring(1));

        int todayIdx = getTodayWeekDayIndex();
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        int dow = selectedCal.get(Calendar.DAY_OF_WEEK);

        for (int i = 0; i < 6; i++) {
            KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(i);
            boolean hasAtt = (att != null && (att.morningAttended || att.afternoonAttended));
            boolean isFuture = (i > todayIdx);
            boolean selected = (dow == CAL_DAYS[i]);
            boolean sent = isWeekDaySent(i);

            if (!hasAtt) {
                // Sin asistencia en BD (Lunes y Martes) -> PLOMO TOTAL
                dayPills[i].setText(SHORT_DAYS[i]);
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                dayPills[i].setTextColor(Color.parseColor("#94A3B8"));
                dayPills[i].setAlpha(0.38f);
            } else if (isFuture) {
                // Día futuro que aún no llega -> PLOMO BLOQUEADO
                dayPills[i].setText(SHORT_DAYS[i]);
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                dayPills[i].setTextColor(Color.parseColor("#94A3B8"));
                dayPills[i].setAlpha(0.38f);
            } else if (selected) {
                dayPills[i].setText(sent ? SHORT_DAYS[i] + " ✓" : SHORT_DAYS[i]);
                dayPills[i].setBackgroundResource(R.drawable.kpi_bg_btn_electric);
                dayPills[i].setTextColor(Color.WHITE);
                dayPills[i].setAlpha(1.0f);
            } else if (sent) {
                dayPills[i].setText(SHORT_DAYS[i] + " ✓");
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
                dayPills[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
                dayPills[i].setAlpha(0.70f);
            } else {
                dayPills[i].setText(SHORT_DAYS[i]);
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                dayPills[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
                dayPills[i].setAlpha(1.0f);
            }
        }

        // Consultar Asistencia en BD
        int selDayIdx = getSelectedWeekDayIndex();
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(selDayIdx);

        if (att != null) {
            tvAttendanceNotice.setText(att.getSummaryLabel());
            if (att.morningAttended && !att.afternoonAttended) {
                currentShiftTag = "MANANA";
            } else if (!att.morningAttended && att.afternoonAttended) {
                currentShiftTag = "TARDE";
            }
        }

        updateShiftPillsUi(att, shortDate);

        KpiJuniorDayRecord existing = KpiJuniorMemoryRepository.getInstance().getShiftRecord(shortDate,
                currentShiftTag);
        pe.net.ws.kpiweb.core.session.SessionManager session = new pe.net.ws.kpiweb.core.session.SessionManager(requireContext());
        String dbFullName = session.getFullName();
        String authUser = (dbFullName != null && !dbFullName.trim().isEmpty())
                ? dbFullName.trim()
                : KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        if (authUser == null || authUser.trim().isEmpty() || "Colaborador WebSite+".equals(authUser)) {
            authUser = !session.getUsername().isEmpty() ? session.getUsername() : "Practicante Diseñador Junior";
        }

        if (existing != null) {
            currentRecord = existing;
            if (authUser != null && !authUser.trim().isEmpty() && (currentRecord.getUserName() == null
                    || currentRecord.getUserName().isEmpty() || currentRecord.getUserName().equals("Oso"))) {
                currentRecord.setUserName(authUser);
            }
            if (currentRecord.getSupervisor() == null || currentRecord.getSupervisor().isEmpty() || "Carlos Ruiz".equals(currentRecord.getSupervisor())) {
                currentRecord.setSupervisor("Lic. Jefferson Cueva (Supervisor Creativo)");
            }
        } else {
            currentRecord = new KpiJuniorDayRecord();
            currentRecord.setDate(shortDate);
            currentRecord.setUserName((authUser != null && !authUser.trim().isEmpty()) ? authUser : "Practicante Diseñador Junior");
            currentRecord.setSupervisor("Lic. Jefferson Cueva (Supervisor Creativo)");
            currentRecord.setShift("MANANA".equals(currentShiftTag)
                    ? "Turno Mañana (08:00 AM - 01:30 PM)"
                    : "Turno Tarde (03:30 PM - 06:30 PM)");
            currentRecord.setGoal(KpiJuniorProductionGoal.PRODUCCION_MIXTA);
            currentRecord.setActivities(new ArrayList<>());
            currentRecord.recalculateMetrics();
        }

        boolean hasAttendance = (att != null && (att.morningAttended || att.afternoonAttended));
        boolean isFuture = (selDayIdx > todayIdx);
        boolean locked = currentRecord.isBiometricVerified() || isFuture || !hasAttendance;

        cbGoalMet.setChecked(Boolean.TRUE.equals(currentRecord.getEvalGoalMet()));
        cbOnTime.setChecked(Boolean.TRUE.equals(currentRecord.getEvalDeliveredOnTime()));
        cbBriefing.setChecked(Boolean.TRUE.equals(currentRecord.getEvalBriefingMet()));
        cbQuality.setChecked(Boolean.TRUE.equals(currentRecord.getEvalQualityAdequate()));
        cbFiles.setChecked(Boolean.TRUE.equals(currentRecord.getEvalFilesOrganized()));
        cbComm.setChecked(Boolean.TRUE.equals(currentRecord.getEvalCommunication()));
        tvObservationsBox.setText(
                currentRecord.getSupervisorObservation() != null ? currentRecord.getSupervisorObservation() : "");

        cbGoalMet.setEnabled(!locked);
        cbOnTime.setEnabled(!locked);
        cbBriefing.setEnabled(!locked);
        cbQuality.setEnabled(!locked);
        cbFiles.setEnabled(!locked);
        cbComm.setEnabled(!locked);

        float alpha = locked ? 0.45f : 1.0f;
        goalSectionCard.setAlpha(alpha);
        actSectionCard.setAlpha(alpha);
        evalFieldsContainer.setAlpha(alpha);

        if (btnAddPiece != null) {
            btnAddPiece.setVisibility(locked ? View.GONE : View.VISIBLE);
            btnAddPiece.setEnabled(!locked);
        }

        String shiftName = "MANANA".equals(currentShiftTag) ? "Turno Mañana" : "Turno Tarde";
        if (!hasAttendance) {
            tvReadOnlyNotice.setVisibility(View.VISIBLE);
            tvReadOnlyNotice.setText("Sin turno de asistencia en base de datos para " + FULL_DAYS[selDayIdx] + ".");
            btnSubmitFinalAction.setEnabled(false);
            btnSubmitFinalAction.setAlpha(0.45f);
            btnSubmitFinalAction.setText("Sin Asistencia en BD (" + FULL_DAYS[selDayIdx] + ")");
        } else if (isFuture) {
            tvReadOnlyNotice.setVisibility(View.VISIBLE);
            tvReadOnlyNotice
                    .setText("Día futuro (" + FULL_DAYS[selDayIdx] + ") bloqueado: aún no comienza la jornada.");
            btnSubmitFinalAction.setEnabled(false);
            btnSubmitFinalAction.setAlpha(0.45f);
            btnSubmitFinalAction.setText("Día Futuro Bloqueado");
        } else if (currentRecord.isBiometricVerified()) {
            tvReadOnlyNotice.setVisibility(View.VISIBLE);
            tvReadOnlyNotice.setText(FULL_DAYS[selDayIdx] + " · " + shiftName + " ya enviado (Solo lectura).");
            btnSubmitFinalAction.setEnabled(true);
            btnSubmitFinalAction.setAlpha(1.0f);

            // Si el otro turno está pendiente en el mismo día, permitir pasar a él
            if (att != null && att.morningAttended && att.afternoonAttended) {
                String otherTag = "MANANA".equals(currentShiftTag) ? "TARDE" : "MANANA";
                KpiJuniorDayRecord other = KpiJuniorMemoryRepository.getInstance().getShiftRecord(shortDate, otherTag);
                if (other == null || !other.isBiometricVerified()) {
                    btnSubmitFinalAction
                            .setText("Pasar a " + ("TARDE".equals(otherTag) ? "Turno Tarde" : "Turno Mañana"));
                    btnSubmitFinalAction.setOnClickListener(v -> {
                        currentShiftTag = otherTag;
                        loadDateRecord(selectedCal);
                        scrollFormToTop();
                    });
                    refreshGoalSelectionUi();
                    renderActivitiesList();
                    refreshLiveSummaryUi();
                    return;
                }
            }

            int nextIdx = getNextUnsentWeekDayIndexAfter(selDayIdx);
            if (nextIdx != -1) {
                btnSubmitFinalAction.setText("Ir al siguiente día pendiente (" + FULL_DAYS[nextIdx] + ")");
                btnSubmitFinalAction.setOnClickListener(v -> jumpToNextPendingJuniorDay());
            } else {
                btnSubmitFinalAction.setEnabled(false);
                btnSubmitFinalAction.setAlpha(0.55f);
                btnSubmitFinalAction.setText("Semana Completa Enviada (Solo lectura)");
            }
        } else {
            tvReadOnlyNotice.setVisibility(View.GONE);
            btnSubmitFinalAction.setEnabled(true);
            btnSubmitFinalAction.setAlpha(1.0f);
            btnSubmitFinalAction.setText("Enviar Ficha de " + FULL_DAYS[selDayIdx] + " (" + shiftName + ")");
            btnSubmitFinalAction.setOnClickListener(v -> submitFinalWithBiometrics());
        }

        refreshGoalSelectionUi();
        renderActivitiesList();
        refreshLiveSummaryUi();
    }

    private void updateShiftPillsUi(KpiAttendanceRepository.AttendanceDay att, String shortDate) {
        if (tvShiftMorningPill == null || tvShiftAfternoonPill == null)
            return;

        boolean mAtt = att == null || att.morningAttended;
        boolean aAtt = att == null || att.afternoonAttended;

        KpiJuniorDayRecord mRec = KpiJuniorMemoryRepository.getInstance().getShiftRecord(shortDate, "MANANA");
        boolean mSent = mRec != null && mRec.isBiometricVerified();

        KpiJuniorDayRecord aRec = KpiJuniorMemoryRepository.getInstance().getShiftRecord(shortDate, "TARDE");
        boolean aSent = aRec != null && aRec.isBiometricVerified();

        // 1. Pastilla Mañana: se oculta si en BD no tuvo turno mañana
        if (!mAtt) {
            tvShiftMorningPill.setVisibility(View.GONE);
        } else {
            tvShiftMorningPill.setVisibility(View.VISIBLE);
            tvShiftMorningPill.setEnabled(true);
            tvShiftMorningPill.setAlpha(1.0f);
            String label = "Turno Mañana (08:00 - 13:30)" + (mSent ? " ✓" : "");
            tvShiftMorningPill.setText(label);
            if ("MANANA".equals(currentShiftTag)) {
                tvShiftMorningPill.setBackgroundResource(R.drawable.kpi_bg_btn_electric);
                tvShiftMorningPill.setTextColor(Color.WHITE);
            } else if (mSent) {
                tvShiftMorningPill.setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
                tvShiftMorningPill.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
            } else {
                tvShiftMorningPill.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                tvShiftMorningPill.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            }
        }

        // 2. Pastilla Tarde: se oculta si en BD no tuvo turno tarde
        if (!aAtt) {
            tvShiftAfternoonPill.setVisibility(View.GONE);
        } else {
            tvShiftAfternoonPill.setVisibility(View.VISIBLE);
            tvShiftAfternoonPill.setEnabled(true);
            tvShiftAfternoonPill.setAlpha(1.0f);
            String label = "Turno Tarde (15:30 - 18:30)" + (aSent ? " ✓" : "");
            tvShiftAfternoonPill.setText(label);
            if ("TARDE".equals(currentShiftTag)) {
                tvShiftAfternoonPill.setBackgroundResource(R.drawable.kpi_bg_btn_electric);
                tvShiftAfternoonPill.setTextColor(Color.WHITE);
            } else if (aSent) {
                tvShiftAfternoonPill.setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
                tvShiftAfternoonPill.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
            } else {
                tvShiftAfternoonPill.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                tvShiftAfternoonPill.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            }
        }
    }

    private void refreshGoalSelectionUi() {
        KpiJuniorProductionGoal[] goals = KpiJuniorProductionGoal.values();
        for (int i = 0; i < goals.length; i++) {
            boolean active = (currentRecord != null && currentRecord.getGoal() == goals[i]);
            goalCards[i].setBackgroundResource(active
                    ? R.drawable.kpi_bg_goal_card_selected
                    : R.drawable.kpi_designer_bg_day_unselected);
        }
    }

    private void renderActivitiesList() {
        activitiesListContainer.removeAllViews();
        if (currentRecord == null || currentRecord.getActivities() == null)
            return;
        final boolean locked = currentRecord.isBiometricVerified();

        List<KpiJuniorActivity> list = currentRecord.getActivities();
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            final KpiJuniorActivity item = list.get(i);

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.topMargin = dp(6);
            row.setLayoutParams(rLp);

            android.widget.ImageView ivAct = new android.widget.ImageView(requireContext());
            ivAct.setImageResource(item.getType() == KpiJuniorActivity.Type.VIDEO
                    ? R.drawable.kpi_ic_video
                    : R.drawable.kpi_ic_palette);
            ivAct.setColorFilter(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(22), dp(22));
            ivLp.rightMargin = dp(8);
            row.addView(ivAct, ivLp);

            LinearLayout infoCol = new LinearLayout(requireContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvMain = new TextView(requireContext());
            tvMain.setText(item.getActivityPiece() + " · " + item.getClientProject());
            tvMain.setTextSize(13f);
            tvMain.setTypeface(Typeface.DEFAULT_BOLD);
            tvMain.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            infoCol.addView(tvMain);

            TextView tvSub = new TextView(requireContext());
            tvSub.setText(item.getType().getDisplayName()
                    + " | Horario: " + (item.getStartTime().isEmpty() ? "08:00 am" : item.getStartTime())
                    + " - " + (item.getEndTime().isEmpty() ? "09:30 am" : item.getEndTime())
                    + " | Correcciones: " + item.getCorrectionsCount());
            tvSub.setTextSize(11f);
            tvSub.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
            infoCol.addView(tvSub);
            row.addView(infoCol);

            TextView tvStatus = new TextView(requireContext());
            boolean delivered = item.getStatus() == KpiJuniorActivity.Status.ENTREGADO;
            tvStatus.setText(delivered ? "Entregado" : "Pendiente");
            tvStatus.setTextSize(11f);
            tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
            tvStatus.setPadding(dp(8), dp(6), dp(8), dp(6));
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                    delivered ? R.color.kpi_success_text : R.color.kpi_warning_text));
            if (!locked) {
                tvStatus.setOnClickListener(v -> {
                    item.setStatus(delivered ? KpiJuniorActivity.Status.PENDIENTE : KpiJuniorActivity.Status.ENTREGADO);
                    currentRecord.recalculateMetrics();
                    renderActivitiesList();
                    refreshLiveSummaryUi();
                });
            }
            row.addView(tvStatus);

            if (!locked) {
                TextView btnDel = new TextView(requireContext());
                btnDel.setText(" ✕ ");
                btnDel.setTextSize(14f);
                btnDel.setTypeface(Typeface.DEFAULT_BOLD);
                btnDel.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_danger));
                btnDel.setOnClickListener(v -> {
                    list.remove(idx);
                    currentRecord.recalculateMetrics();
                    renderActivitiesList();
                    refreshLiveSummaryUi();
                });
                row.addView(btnDel);
            } else {
                row.setOnClickListener(v -> jumpToNextPendingJuniorDay());
            }

            activitiesListContainer.addView(row);
        }
    }

    private void showAddOrEditActivityDialog(@Nullable KpiJuniorActivity existing) {
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(12), dp(20), dp(4));

        EditText etClient = new EditText(requireContext());
        etClient.setHint("Cliente o Proyecto (ej. Clínica Dental, WebSite+)");
        form.addView(etClient);

        EditText etPiece = new EditText(requireContext());
        etPiece.setHint("Pieza realizada (ej. Flyer promo, Edición Reel)");
        form.addView(etPiece);

        Spinner spType = new Spinner(requireContext());
        spType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, new String[] { "Post Gráfico", "Video / Reel" }));
        form.addView(spType);

        EditText etHours = new EditText(requireContext());
        etHours.setHint("Horario (ej. 08:00 am - 09:30 am o 03:30 pm - 05:00 pm)");
        etHours.setText("08:00 am - 09:30 am");
        form.addView(etHours);

        new AlertDialog.Builder(requireContext())
                .setTitle("Registrar Pieza · Diseñador Jr")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Agregar", (d, w) -> {
                    String client = etClient.getText().toString().trim();
                    String piece = etPiece.getText().toString().trim();
                    if (client.isEmpty())
                        client = "Proyecto Interno";
                    if (piece.isEmpty())
                        piece = "Pieza creativa";
                    KpiJuniorActivity.Type type = spType.getSelectedItemPosition() == 1
                            ? KpiJuniorActivity.Type.VIDEO
                            : KpiJuniorActivity.Type.POST;
                    String[] parts = etHours.getText().toString().split("-");
                    String start = parts.length > 0 ? parts[0].trim() : "08:00 am";
                    String end = parts.length > 1 ? parts[1].trim() : "09:30 am";

                    int nextOrder = currentRecord.getActivities().size() + 1;
                    KpiJuniorActivity act = new KpiJuniorActivity(
                            nextOrder, client, piece, type, start, end,
                            KpiJuniorActivity.Status.ENTREGADO, 0);
                    currentRecord.getActivities().add(act);
                    currentRecord.recalculateMetrics();
                    renderActivitiesList();
                    refreshLiveSummaryUi();
                })
                .show();
    }

    private void refreshLiveSummaryUi() {
        if (currentRecord == null)
            return;
        currentRecord.recalculateMetrics();

        cbGoalMet.setChecked(Boolean.TRUE.equals(currentRecord.getEvalGoalMet()));
        cbOnTime.setChecked(Boolean.TRUE.equals(currentRecord.getEvalDeliveredOnTime()));
        cbBriefing.setChecked(Boolean.TRUE.equals(currentRecord.getEvalBriefingMet()));
        cbQuality.setChecked(Boolean.TRUE.equals(currentRecord.getEvalQualityAdequate()));
        cbFiles.setChecked(Boolean.TRUE.equals(currentRecord.getEvalFilesOrganized()));
        cbComm.setChecked(Boolean.TRUE.equals(currentRecord.getEvalCommunication()));

        tvLiveMetricsSummary.setText(String.format(Locale.getDefault(),
                "Posts: %d | Videos: %d | Entregados: %d/%d (%d%%)",
                currentRecord.getPostsCompleted(),
                currentRecord.getVideosCompleted(),
                currentRecord.getDeliveredPieces(),
                currentRecord.getTargetPieces(),
                currentRecord.getGoalCompliancePercentage()));

        tvScoreBadge.setText(String.format(Locale.getDefault(),
                "Puntaje: %d/100 pts · Nota Practicante: %.1f / 20 (%s)",
                currentRecord.getTotalScore(),
                currentRecord.getFinalGrade(),
                currentRecord.getProductivityLevel()));
    }

    private void persistCurrentJuniorRecord(boolean verified) {
        if (currentRecord == null)
            return;
        syncChecksToRecord();
        currentRecord.setShift("MANANA".equals(currentShiftTag)
                ? "Turno Mañana (08:00 AM - 01:30 PM)"
                : "Turno Tarde (03:30 PM - 06:30 PM)");
        currentRecord.setBiometricVerified(verified);
        if (verified) {
            currentRecord.setVerificationTimestamp(System.currentTimeMillis());
        }
        currentRecord.recalculateMetrics();
        KpiJuniorMemoryRepository.getInstance().saveShiftRecord(currentRecord.getDate(), currentShiftTag,
                currentRecord);

        if (verified && getContext() != null) {
            String evalCode = "MANANA".equals(currentShiftTag) ? "KPI-JR-DISENO-M" : "KPI-JR-DISENO-T";
            String evalTitle = "Evaluación Diaria · Diseñador Gráfico Junior ("
                    + ("MANANA".equals(currentShiftTag) ? "Turno Mañana" : "Turno Tarde") + ")";
            KpiCustomRepository.getInstance(requireContext()).saveRoleDailyEvaluation(
                    "Diseñador Gráfico Junior",
                    currentRecord.getDate(),
                    evalCode,
                    evalTitle,
                    currentRecord.getDeliveredPieces(),
                    currentRecord.getTargetPieces(),
                    currentRecord.getTotalScore(),
                    false,
                    currentRecord.getSupervisorObservation());
        }
    }

    private void submitFinalWithBiometrics() {
        final int dayIdx = getSelectedWeekDayIndex();
        final String dayName = FULL_DAYS[dayIdx];
        String shiftName = "MANANA".equals(currentShiftTag) ? "Turno Mañana" : "Turno Tarde";
        KpiBiometricHelper.authenticate(this, "Enviar Ficha de " + dayName + " (" + shiftName + ")",
                new KpiBiometricHelper.BiometricCallback() {
                    @Override
                    public void onSuccess() {
                        onJuniorDayVerified(dayIdx, dayName);
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) {
                        if (errorCode == -1) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Confirmar Envío · Diseñador Jr")
                                    .setMessage(
                                            "¿Confirmas el envío de tu ficha de " + dayName + " (" + shiftName + ")?")
                                    .setNegativeButton("Cancelar", null)
                                    .setPositiveButton("Confirmar", (d, w) -> onJuniorDayVerified(dayIdx, dayName))
                                    .show();
                        } else {
                            Toast.makeText(getContext(), "Verificación biométrica cancelada", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailed() {
                        Toast.makeText(getContext(), "Biometría no reconocida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onJuniorDayVerified(int justSentIdx, String sentDayName) {
        // Tono de éxito de 2 segundos que suena inmediatamente al colocar la huella
        pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper.playDailySuccess(requireContext());

        persistCurrentJuniorRecord(true);
        int score = currentRecord != null ? currentRecord.getTotalScore() : 100;

        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(justSentIdx);
        SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String shortDate = dfShort.format(selectedCal.getTime());

        // Si el día tenía doble turno en BD y el otro turno aún está pendiente:
        if (att != null && att.morningAttended && att.afternoonAttended) {
            String otherTag = "MANANA".equals(currentShiftTag) ? "TARDE" : "MANANA";
            KpiJuniorDayRecord other = KpiJuniorMemoryRepository.getInstance().getShiftRecord(shortDate, otherTag);
            if (other == null || !other.isBiometricVerified()) {
                currentShiftTag = otherTag;
                loadDateRecord(selectedCal);
                scrollFormToTop();
                Toast.makeText(getContext(),
                        "✓ Turno registrado con éxito. Ahora procede con el "
                                + ("TARDE".equals(otherTag) ? "Turno Tarde" : "Turno Mañana") + ".",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        int nextIdx = getNextUnsentWeekDayIndexAfter(justSentIdx);
        if (nextIdx == -1 || justSentIdx == 5) {
            loadDateRecord(selectedCal);
            scrollFormToTop();
            KpiPandaSuccessDialog.show(requireContext(), "Diseñador Gráfico Junior", true,
                    nextIdx == -1 ? "Semana Completa" : sentDayName,
                    score, Math.max(10, score), () -> {
                        if (nextIdx != -1) {
                            loadDateRecord(getWeekDayCalendar(nextIdx));
                            scrollFormToTop();
                        }
                    });
        } else {
            loadDateRecord(getWeekDayCalendar(nextIdx));
            scrollFormToTop();
            Toast.makeText(getContext(),
                    "✓ " + sentDayName + " completado · Ahora en " + FULL_DAYS[nextIdx],
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showAlarmAndAttendanceDialog() {
        String[] options = {
                "🔔 Probar Alarma 2:00 PM (Turno Mañana) [Notif + Sonido]",
                "🔔 Probar Alarma 6:40 PM (Turno Tarde) [Notif + Sonido]",
                "🔔 Probar Alarma Sábado 8:00 PM [SONIDO DOBLE]",
                "🔔 Probar Alarma 8:00 PM Diario [Notif + Sonido]"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Alarmas & Asistencia en BD (MySQL)")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            KpiReminderScheduler.triggerInstant2PmReminder(requireContext());
                            break;
                        case 1:
                            KpiReminderScheduler.triggerInstant640PmReminder(requireContext());
                            break;
                        case 2:
                            KpiReminderScheduler.triggerInstantSaturday8PmDoubleReminder(requireContext());
                            break;
                        case 3:
                            KpiReminderScheduler.triggerInstantReminderCheck(requireContext());
                            break;
                    }
                })
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void refreshWeeklyReportUi() {
        List<KpiJuniorDayRecord> all = KpiJuniorMemoryRepository.getInstance().getAllRecords();
        List<KpiJuniorWeeklyChartView.DayData> chartItems = new ArrayList<>();
        weeklyBreakdownContainer.removeAllViews();

        int totalPosts = 0;
        int totalVideos = 0;
        double sumGrades = 0;

        int start = Math.max(0, all.size() - 6);
        List<KpiJuniorDayRecord> recent = all.subList(start, all.size());

        for (KpiJuniorDayRecord r : recent) {
            String shortLbl = r.getDate() != null && r.getDate().length() >= 5 ? r.getDate().substring(0, 5)
                    : r.getDate();
            chartItems.add(new KpiJuniorWeeklyChartView.DayData("Día\n" + shortLbl, r.getPostsCompleted(),
                    r.getVideosCompleted()));
            totalPosts += r.getPostsCompleted();
            totalVideos += r.getVideosCompleted();
            sumGrades += r.getFinalGrade();

            TextView row = new TextView(requireContext());
            row.setText(String.format(Locale.getDefault(),
                    "%s · %d Posts + %d Videos · Nota: %.1f/20 (%s)",
                    r.getDate(), r.getPostsCompleted(), r.getVideosCompleted(), r.getFinalGrade(),
                    r.getProductivityLevel()));
            row.setTextSize(12f);
            row.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            row.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            row.setLayoutParams(lp);
            weeklyBreakdownContainer.addView(row);
        }

        if (!chartItems.isEmpty()) {
            weeklyChartView.setData(chartItems);
        }
        double avgGrade = recent.isEmpty() ? 0 : (sumGrades / recent.size());
        tvWeeklySummaryStats.setText(String.format(Locale.getDefault(),
                "Total acumulado: %d Posts · %d Videos · Promedio Vigesimal: %.1f / 20",
                totalPosts, totalVideos, avgGrade));
    }

    private void jumpToWeekDay(int targetDow) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int curr = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (curr == Calendar.SUNDAY) ? 6 : (curr - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff + (targetDow - Calendar.MONDAY));
        loadDateRecord(cal);
    }

    private void openDatePicker() {
        new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            Calendar c = Calendar.getInstance();
            c.set(y, m, d);
            loadDateRecord(c);
            scrollFormToTop();
        }, selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), selectedCal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private LinearLayout createSectionCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.kpi_bg_card_white);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createSectionTitle(String title) {
        TextView tv = new TextView(requireContext());
        tv.setText(title);
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        return tv;
    }

    private void navigateBack() {
        if (getActivity() == null)
            return;
        if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getActivity().getSupportFragmentManager().popBackStack();
        } else {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, KpiRoleSelectorFragment.newInstance())
                    .commit();
        }
    }

    private int dp(int val) {
        return Math.round(val * requireContext().getResources().getDisplayMetrics().density);
    }
}
