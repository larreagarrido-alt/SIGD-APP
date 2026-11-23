package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository;
import pe.net.ws.kpiweb.features.kpi.data.KpiMockDataFactory;
import pe.net.ws.kpiweb.features.kpi.model.KpiActivityItem;
import pe.net.ws.kpiweb.features.kpi.model.KpiDailyReport;
import pe.net.ws.kpiweb.features.kpi.model.KpiProductionGoal;

/**
 * Formulario especializado de Programador Junior (Practicante):
 * - Flujo semanal Lunes–Sábado: abre posicionado en el día real actual, permite retroceder
 *   libremente a días anteriores pendientes, bloquea en solo lectura con opacidad (0.45f)
 *   los días ya enviados, y avanza automáticamente al siguiente día pendiente con scroll arriba.
 * - Meta diaria de producción en cuadros pequeños con scroll horizontal dentro del bloque EN VIVO.
 * - Proyecto editable con sugerencias guardadas y horas en "--:--" por defecto.
 * - Salida a campo activable que permite bloquear/limpiar las actividades de programación.
 */
public class KpiFormFragment extends Fragment implements KpiActivityAdapter.OnActivityDataChangedListener {

    public static final String PREF_PROGRAMMER_WEEK = "kpi_weekly_prog_prefs_v5";
    private static final String KEY_WEEK_ID = "active_week_id";
    private static final String[] SHORT_DAYS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};
    private static final String[] FULL_DAYS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    private KpiDailyReport currentReport;
    private boolean programmingLockedByCampo = false;
    private boolean suppressCampoDialog = false;
    private int selectedDayIndex = 0;
    private boolean isCurrentDaySent = false;

    private NestedScrollView nsvProgrammerForm;
    private LinearLayout llProgrammerFormContent;
    private LinearLayout weekStripCard;
    private final TextView[] dayPills = new TextView[6];
    private TextView tvProgrammerReadOnlyBanner;
    private TextView tvHeaderShiftBadge;
    private TextView tvShiftMorningPill;
    private TextView tvShiftAfternoonPill;
    private int currentShiftIndex = 0;

    // Vistas principales (Bloque EN VIVO + Metas horizontales)
    private TextView tvLivePercent;
    private ProgressBar pbLiveProgress;
    private TextView tvLiveSummary;
    private LinearLayout llGoalsContainer;

    // Sección 01: Datos generales
    private LinearLayout llDatePicker;
    private TextView tvFormDate;
    private EditText etPracticante;
    private Spinner spSupervisor;
    private Spinner spTurno;

    // Modalidad Salida a Campo / Comisión Externa
    private SwitchMaterial swSalidaCampo;
    private LinearLayout llCampoDetalleContainer;
    private LinearLayout llCampoStartTime;
    private TextView tvCampoStartTime;
    private LinearLayout llCampoEndTime;
    private TextView tvCampoEndTime;
    private EditText etMotivoCampo;
    private View cardActivitiesLockedByCampo;
    private Button btnUnlockProgrammingActivities;

    // Sección 02: Registro de Actividades
    private LinearLayout llProgrammingActivitiesBlock;
    private RecyclerView rvActivities;
    private KpiActivityAdapter activityAdapter;
    private Button btnAddActivity;

    // Sección 03: Resumen de Producción
    private LinearLayout llSummaryRows;
    private TextView tvSummaryMetaPercent;

    // Secciones técnicas (04 Evaluación Rápida + 05 Incidencias)
    private LinearLayout llProgrammingExtraSections;
    private LinearLayout llEvaluationCriteria;
    private RadioGroup rgDificultad;
    private EditText etDetalleDificultad;

    // Sección 06: Supervisor y firmas
    private TextView tvSupervisorObservation;
    private TextView tvSignaturePracticante;
    private TextView tvSignatureSupervisor;

    // Sección 07: Nivel de Productividad
    private RadioButton rbNivelExcelente;
    private RadioButton rbNivelMuyBueno;
    private RadioButton rbNivelBueno;
    private RadioButton rbNivelRegular;
    private RadioButton rbNivelDeficiente;

    // Botones de acción
    private View btnProgrammerBack;
    private View btnProgrammerGoToReport;
    private Button btnClearForm;
    private Button btnSaveForm;

    public static KpiFormFragment newInstance() {
        return new KpiFormFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_fragment_form, container, false);
        ensureWeekPrefsFresh();
        selectedDayIndex = getTodayWeekDayIndex();
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay todayAtt = attRepo.getDayAttendance(selectedDayIndex);
        if (todayAtt == null || (!todayAtt.morningAttended && !todayAtt.afternoonAttended)) {
            for (int i = selectedDayIndex - 1; i >= 0; i--) {
                KpiAttendanceRepository.AttendanceDay a = attRepo.getDayAttendance(i);
                if (a != null && (a.morningAttended || a.afternoonAttended)) {
                    selectedDayIndex = i;
                    break;
                }
            }
        }
        initViews(root);
        injectWeekDayStrip();
        setupSpinnersAndStaticSections(root);
        setupListeners();
        loadSelectedDayData();
        KpiThemeContrastApplier.apply(root);
        pe.net.ws.kpiweb.features.kpi.data.KpiTemplateSync.get(requireContext()).refresh((ok, message) -> {
            if (ok && isAdded() && getView() == root) {
                pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine eng =
                        pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine.getInstance(requireContext());
                eng.applyTemplateToFormView(requireContext(), "Programador Junior", root);
                poblarMetasProduccionCompactas();
                poblarEvaluacionRapida();
            }
        });
        return root;
    }

    private SharedPreferences getWeekPrefs() {
        if (getContext() == null) return null;
        return requireContext().getSharedPreferences(PREF_PROGRAMMER_WEEK, Context.MODE_PRIVATE);
    }

    private void ensureWeekPrefsFresh() {
        Calendar now = Calendar.getInstance();
        now.setFirstDayOfWeek(Calendar.MONDAY);
        String weekKey = now.get(Calendar.YEAR) + "-W" + now.get(Calendar.WEEK_OF_YEAR);
        SharedPreferences prefs = getWeekPrefs();
        if (prefs == null) return;
        String curKey = prefs.getString(KEY_WEEK_ID, "");
        if (curKey.isEmpty()) {
            prefs.edit().putString(KEY_WEEK_ID, weekKey).apply();
        } else if (!weekKey.equals(curKey)) {
            // Cambio de semana dominical real
            prefs.edit().clear().putString(KEY_WEEK_ID, weekKey).apply();
        }
    }

    private int getTodayWeekDayIndex() {
        Calendar now = Calendar.getInstance();
        switch (now.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            default: return 5;
        }
    }

    private Calendar getWeekDayCalendar(int dayIndex0To5) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff + dayIndex0To5);
        return cal;
    }

    private String getFormattedDateForDay(int dayIndex0To5) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd / MM / yyyy", Locale.getDefault());
        return sdf.format(getWeekDayCalendar(dayIndex0To5).getTime());
    }

    private boolean isDaySent(int dayIndex0To5) {
        if (getContext() == null) return false;
        int todayIdx = getTodayWeekDayIndex();
        if (dayIndex0To5 > todayIdx) return false;
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(dayIndex0To5);
        if (att == null) return false;
        if (!att.morningAttended && !att.afternoonAttended) return false;

        SharedPreferences prefs = getWeekPrefs();
        if (prefs == null) return false;
        boolean morningOk = !att.morningAttended || prefs.getBoolean("day_" + dayIndex0To5 + "_shift_0_sent", false);
        boolean afternoonOk = !att.afternoonAttended || prefs.getBoolean("day_" + dayIndex0To5 + "_shift_1_sent", false);
        return morningOk && afternoonOk;
    }

    private int getNextUnsentDayIndexAfter(int fromIdx) {
        int todayIdx = getTodayWeekDayIndex();
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        for (int i = 0; i <= todayIdx; i++) {
            if (i == fromIdx) continue;
            KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(i);
            if (att != null && (att.morningAttended || att.afternoonAttended) && !isDaySent(i)) {
                return i;
            }
        }
        return -1;
    }

    private void scrollFormToTop() {
        if (nsvProgrammerForm != null) {
            nsvProgrammerForm.post(() -> nsvProgrammerForm.smoothScrollTo(0, 0));
        }
    }

    private void jumpToNextPendingProgrammerDay() {
        int nextIdx = getNextUnsentDayIndexAfter(selectedDayIndex);
        if (nextIdx != -1) {
            selectedDayIndex = nextIdx;
            loadSelectedDayData();
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

    private void initViews(View root) {
        nsvProgrammerForm = root.findViewById(R.id.nsvProgrammerForm);
        llProgrammerFormContent = root.findViewById(R.id.llProgrammerFormContent);
        tvHeaderShiftBadge = root.findViewById(R.id.tvHeaderShiftBadge);

        tvLivePercent = root.findViewById(R.id.tvLivePercent);
        pbLiveProgress = root.findViewById(R.id.pbLiveProgress);
        tvLiveSummary = root.findViewById(R.id.tvLiveSummary);
        llGoalsContainer = root.findViewById(R.id.llGoalsContainer);

        llDatePicker = root.findViewById(R.id.llDatePicker);
        tvFormDate = root.findViewById(R.id.tvFormDate);
        etPracticante = root.findViewById(R.id.etPracticante);
        spSupervisor = root.findViewById(R.id.spSupervisor);
        spTurno = root.findViewById(R.id.spTurno);

        swSalidaCampo = root.findViewById(R.id.swSalidaCampo);
        llCampoDetalleContainer = root.findViewById(R.id.llCampoDetalleContainer);
        llCampoStartTime = root.findViewById(R.id.llCampoStartTime);
        tvCampoStartTime = root.findViewById(R.id.tvCampoStartTime);
        llCampoEndTime = root.findViewById(R.id.llCampoEndTime);
        tvCampoEndTime = root.findViewById(R.id.tvCampoEndTime);
        etMotivoCampo = root.findViewById(R.id.etMotivoCampo);
        cardActivitiesLockedByCampo = root.findViewById(R.id.cardActivitiesLockedByCampo);
        btnUnlockProgrammingActivities = root.findViewById(R.id.btnUnlockProgrammingActivities);

        llProgrammingActivitiesBlock = root.findViewById(R.id.llProgrammingActivitiesBlock);
        rvActivities = root.findViewById(R.id.rvActivities);
        btnAddActivity = root.findViewById(R.id.btnAddActivity);

        llSummaryRows = root.findViewById(R.id.llSummaryRows);
        tvSummaryMetaPercent = root.findViewById(R.id.tvSummaryMetaPercent);

        llProgrammingExtraSections = root.findViewById(R.id.llProgrammingExtraSections);
        llEvaluationCriteria = root.findViewById(R.id.llEvaluationCriteria);
        rgDificultad = root.findViewById(R.id.rgDificultad);
        etDetalleDificultad = root.findViewById(R.id.etDetalleDificultad);

        tvSupervisorObservation = root.findViewById(R.id.tvSupervisorObservation);
        tvSignaturePracticante = root.findViewById(R.id.tvSignaturePracticante);
        tvSignatureSupervisor = root.findViewById(R.id.tvSignatureSupervisor);

        rbNivelExcelente = root.findViewById(R.id.rbNivelExcelente);
        rbNivelMuyBueno = root.findViewById(R.id.rbNivelMuyBueno);
        rbNivelBueno = root.findViewById(R.id.rbNivelBueno);
        rbNivelRegular = root.findViewById(R.id.rbNivelRegular);
        rbNivelDeficiente = root.findViewById(R.id.rbNivelDeficiente);

        btnProgrammerBack = root.findViewById(R.id.btnProgrammerBack);
        btnProgrammerGoToReport = root.findViewById(R.id.btnProgrammerGoToReport);
        btnClearForm = root.findViewById(R.id.btnClearForm);
        btnSaveForm = root.findViewById(R.id.btnSaveForm);

        rvActivities.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void injectWeekDayStrip() {
        if (llProgrammerFormContent == null) return;
        float d = getResources().getDisplayMetrics().density;

        weekStripCard = new LinearLayout(requireContext());
        weekStripCard.setOrientation(LinearLayout.VERTICAL);
        weekStripCard.setBackgroundResource(R.drawable.kpi_bg_card_white);
        int pad = Math.round(12 * d);
        weekStripCard.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = Math.round(14 * d);
        weekStripCard.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            TextView pill = new TextView(requireContext());
            pill.setText(SHORT_DAYS[i]);
            pill.setGravity(Gravity.CENTER);
            pill.setTextSize(12f);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, Math.round(42 * d), 1f);
            if (i < 5) pLp.rightMargin = Math.round(4 * d);
            pill.setLayoutParams(pLp);
            pill.setOnClickListener(v -> {
                int todayIdx = getTodayWeekDayIndex();
                KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
                KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(idx);
                boolean hasAtt = (att != null && (att.morningAttended || att.afternoonAttended));

                if (!hasAtt) {
                    Toast.makeText(getContext(),
                            "Sin asistencia en BD para " + FULL_DAYS[idx] + " (Día no programado)",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (idx > todayIdx) {
                    Toast.makeText(getContext(),
                            "El día " + FULL_DAYS[idx] + " aún no ha comenzado. Solo puedes registrar hasta el día de hoy.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedDayIndex = idx;
                loadSelectedDayData();
                scrollFormToTop();
            });
            dayPills[i] = pill;
            row.addView(pill);
        }
        weekStripCard.addView(row);

        // Selector de Doble Turno del Practicante (Mañana y Tarde) - Idéntico a Diseñador Junior
        LinearLayout shiftTabsRow = new LinearLayout(requireContext());
        shiftTabsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams shiftTabsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shiftTabsLp.topMargin = Math.round(8 * d);
        shiftTabsRow.setLayoutParams(shiftTabsLp);

        tvShiftMorningPill = new TextView(requireContext());
        tvShiftMorningPill.setText("Turno Mañana");
        tvShiftMorningPill.setTextSize(11f);
        tvShiftMorningPill.setIncludeFontPadding(false);
        tvShiftMorningPill.setTypeface(Typeface.DEFAULT_BOLD);
        tvShiftMorningPill.setGravity(Gravity.CENTER);
        tvShiftMorningPill.setPadding(Math.round(8 * d), Math.round(8 * d), Math.round(8 * d), Math.round(8 * d));
        tvShiftMorningPill.setMinimumHeight(Math.round(54 * d));
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        smLp.rightMargin = Math.round(4 * d);
        tvShiftMorningPill.setLayoutParams(smLp);
        tvShiftMorningPill.setOnClickListener(v -> {
            if (currentShiftIndex != 0) {
                saveCurrentDraftToJson();
                currentShiftIndex = 0;
                if (spTurno != null) spTurno.setSelection(0);
                loadShiftData(0);
                refreshShiftTabsUi();
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
        tvShiftAfternoonPill.setPadding(Math.round(8 * d), Math.round(8 * d), Math.round(8 * d), Math.round(8 * d));
        tvShiftAfternoonPill.setMinimumHeight(Math.round(54 * d));
        LinearLayout.LayoutParams saLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        saLp.leftMargin = Math.round(4 * d);
        tvShiftAfternoonPill.setLayoutParams(saLp);
        tvShiftAfternoonPill.setOnClickListener(v -> {
            if (currentShiftIndex != 1) {
                saveCurrentDraftToJson();
                currentShiftIndex = 1;
                if (spTurno != null) spTurno.setSelection(1);
                loadShiftData(1);
                refreshShiftTabsUi();
                scrollFormToTop();
            }
        });
        shiftTabsRow.addView(tvShiftAfternoonPill);
        weekStripCard.addView(shiftTabsRow);

        tvProgrammerReadOnlyBanner = new TextView(requireContext());
        tvProgrammerReadOnlyBanner.setTextSize(12f);
        tvProgrammerReadOnlyBanner.setTypeface(Typeface.DEFAULT_BOLD);
        tvProgrammerReadOnlyBanner.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
        tvProgrammerReadOnlyBanner.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
        int bPad = Math.round(10 * d);
        tvProgrammerReadOnlyBanner.setPadding(bPad, Math.round(7 * d), bPad, Math.round(7 * d));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = Math.round(8 * d);
        tvProgrammerReadOnlyBanner.setLayoutParams(bLp);
        tvProgrammerReadOnlyBanner.setVisibility(View.GONE);
        weekStripCard.addView(tvProgrammerReadOnlyBanner);

        llProgrammerFormContent.addView(weekStripCard, 0);
    }

    private void refreshWeekDayStripUi() {
        int todayIdx = getTodayWeekDayIndex();
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());

        for (int i = 0; i < 6; i++) {
            KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(i);
            boolean hasAtt = (att != null && (att.morningAttended || att.afternoonAttended));
            boolean isFuture = (i > todayIdx);
            boolean sent = isDaySent(i);
            boolean sel = (i == selectedDayIndex);

            if (!hasAtt) {
                // Sin asistencia en base de datos (Lunes y Martes) -> PLOMO TOTAL
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
            } else if (sel) {
                // Día seleccionado
                dayPills[i].setText(sent ? SHORT_DAYS[i] + " ✓" : SHORT_DAYS[i]);
                dayPills[i].setBackgroundResource(R.drawable.kpi_bg_btn_electric);
                dayPills[i].setTextColor(Color.WHITE);
                dayPills[i].setAlpha(1.0f);
            } else if (sent) {
                // Ya enviado
                dayPills[i].setText(SHORT_DAYS[i] + " ✓");
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
                dayPills[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
                dayPills[i].setAlpha(0.70f);
            } else {
                // Pendiente de llenar (con asistencia en BD)
                dayPills[i].setText(SHORT_DAYS[i]);
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                dayPills[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
                dayPills[i].setAlpha(1.0f);
            }
        }
        refreshShiftTabsUi();
    }

    private void saveCurrentDraftToJson() {
        try {
            JSONObject obj = buildCurrentReportJson();
            SharedPreferences prefs = getWeekPrefs();
            if (prefs != null) {
                prefs.edit()
                        .putString("day_" + selectedDayIndex + "_shift_" + currentShiftIndex + "_json", obj.toString())
                        .apply();
            }
        } catch (Exception ignored) {}
    }

    private void refreshShiftTabsUi() {
        if (tvShiftMorningPill == null || tvShiftAfternoonPill == null || getContext() == null) return;
        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(selectedDayIndex);
        boolean mAtt = (att != null && att.morningAttended);
        boolean aAtt = (att != null && att.afternoonAttended);

        SharedPreferences p = getWeekPrefs();
        boolean mSent = p != null && p.getBoolean("day_" + selectedDayIndex + "_shift_0_sent", false);
        boolean aSent = p != null && p.getBoolean("day_" + selectedDayIndex + "_shift_1_sent", false);

        // Pastilla Mañana: se oculta si en BD no tuvo turno mañana
        if (!mAtt && (att != null && (att.morningAttended || att.afternoonAttended))) {
            tvShiftMorningPill.setVisibility(View.GONE);
        } else {
            tvShiftMorningPill.setVisibility(View.VISIBLE);
            tvShiftMorningPill.setEnabled(true);
            tvShiftMorningPill.setAlpha(1.0f);
            String label = "Turno Mañana (08:00 - 13:30)" + (mSent ? " ✓" : "");
            tvShiftMorningPill.setText(label);
            if (currentShiftIndex == 0) {
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

        // Pastilla Tarde: se oculta si en BD no tuvo turno tarde
        if (!aAtt && (att != null && (att.morningAttended || att.afternoonAttended))) {
            tvShiftAfternoonPill.setVisibility(View.GONE);
        } else {
            tvShiftAfternoonPill.setVisibility(View.VISIBLE);
            tvShiftAfternoonPill.setEnabled(true);
            tvShiftAfternoonPill.setAlpha(1.0f);
            String label = "Turno Tarde (15:30 - 18:30)" + (aSent ? " ✓" : "");
            tvShiftAfternoonPill.setText(label);
            if (currentShiftIndex == 1) {
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

    private void setupSpinnersAndStaticSections(View root) {
        pe.net.ws.kpiweb.core.session.SessionManager session = new pe.net.ws.kpiweb.core.session.SessionManager(requireContext());
        boolean realAccount = session.isLoggedIn() && session.getUserId() > 0 && session.getWorkerId() > 0;
        // The original mock report contains activities, scores, signatures and names.
        // None of those may become part of a real worker's submitted snapshot.
        this.currentReport = realAccount ? KpiMockDataFactory.createBlankProgrammerTemplate()
                : KpiMockDataFactory.createMockReport();
        if (realAccount) {
            currentReport.setRol(session.getCargoName());
            TextView roleBadge = root.findViewById(R.id.tvHeaderRole);
            if (roleBadge != null && session.getCargoName() != null)
                roleBadge.setText(session.getCargoName().toUpperCase(java.util.Locale.ROOT));
        }
        String dbFullName = session.getFullName();
        String dbDni = session.getDni();
        String authUser = (dbFullName != null && !dbFullName.trim().isEmpty())
                ? dbFullName.trim()
                : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        if (authUser == null || authUser.isEmpty() || authUser.equals("Colaborador WebSite+")) {
            authUser = !session.getUsername().isEmpty() ? session.getUsername()
                    : (realAccount ? "" : "Practicante Programador WebSite+");
        }

        currentReport.setPracticante(authUser);
        // A name supplied by /auth/me identifies the author; it is not a signature.
        currentReport.setFirmaPracticante(realAccount ? "" : authUser);

        if (etPracticante != null) {
            String label = authUser + (dbDni != null && !dbDni.trim().isEmpty() ? " · DNI " + dbDni.trim() : "");
            etPracticante.setText(label);
            etPracticante.setEnabled(false);
            etPracticante.setFocusable(false);
        }

        pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository collabRepo =
                pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository.getInstance(requireContext());
        pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile prof = collabRepo.findByName(authUser);
        String dbSupervisor = realAccount ? "" : (prof != null && prof.getSupervisor() != null && !prof.getSupervisor().trim().isEmpty()
                && !prof.getSupervisor().contains("Carlos Ramírez"))
                ? prof.getSupervisor().trim()
                : "Ing. Jair Valdera (Supervisor de TI)";

        String[] supervisores = {dbSupervisor};
        ArrayAdapter<String> adapterSup = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, supervisores);
        spSupervisor.setAdapter(adapterSup);
        currentReport.setSupervisor(dbSupervisor);
        currentReport.setFirmaSupervisor(realAccount ? "" : "Ing. Jair Valdera");

        String[] turnos = {"Turno Mañana (08:00 AM - 01:30 PM)", "Turno Tarde (03:30 PM - 06:30 PM)"};
        ArrayAdapter<String> adapterTurnos = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, turnos);
        spTurno.setAdapter(adapterTurnos);

        poblarMetasProduccionCompactas();
        poblarEvaluacionRapida();

        activityAdapter = new KpiActivityAdapter(requireContext(), currentReport.getActividades(), this);
        rvActivities.setAdapter(activityAdapter);

        tvSupervisorObservation.setText(currentReport.getObservacionSupervisor());
        tvSignaturePracticante.setText(realAccount ? "Responsable: " + authUser : authUser);
        tvSignatureSupervisor.setText(realAccount ? "Supervisor pendiente" : currentReport.getFirmaSupervisor());
    }

    private void loadSelectedDayData() {
        ensureWeekPrefsFresh();
        String dateFormatted = getFormattedDateForDay(selectedDayIndex);
        currentReport.setFecha(dateFormatted);
        tvFormDate.setText(dateFormatted);

        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(selectedDayIndex);

        int shiftIdx = 0;
        if (att != null) {
            if (att.morningAttended && !att.afternoonAttended) {
                shiftIdx = 0;
            } else if (!att.morningAttended && att.afternoonAttended) {
                shiftIdx = 1;
            } else if (att.morningAttended && att.afternoonAttended) {
                SharedPreferences p = getWeekPrefs();
                boolean morningSent = p != null && p.getBoolean("day_" + selectedDayIndex + "_shift_0_sent", false);
                boolean afternoonSent = p != null && p.getBoolean("day_" + selectedDayIndex + "_shift_1_sent", false);
                shiftIdx = (morningSent && !afternoonSent) ? 1 : 0;
            }
        }
        if (spTurno != null) {
            spTurno.setSelection(shiftIdx);
            spTurno.setEnabled(false);
        }
        currentShiftIndex = shiftIdx;
        refreshShiftTabsUi();

        String shiftLabel = (shiftIdx == 0) ? "TURNO MAÑANA (08:00 AM - 01:30 PM)" : "TURNO TARDE (03:30 PM - 06:30 PM)";
        if (tvHeaderShiftBadge != null) {
            tvHeaderShiftBadge.setText(shiftLabel);
        }

        SharedPreferences prefs = getWeekPrefs();
        isCurrentDaySent = prefs != null && prefs.getBoolean("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_sent", false);
        String savedJson = prefs.getString("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_json",
                prefs.getString("day_" + selectedDayIndex + "_json", null));

        currentReport.getActividades().clear();

        if (savedJson != null) {
            try {
                JSONObject obj = new JSONObject(savedJson);
                etPracticante.setText(obj.optString("practicante", currentReport.getPracticante()));
                int turnoIdx = obj.optInt("turnoIdx", shiftIdx);
                if (spTurno != null && spTurno.isEnabled()) spTurno.setSelection(turnoIdx);
                boolean isCampo = obj.optBoolean("esSalidaCampo", false);
                String hIni = obj.optString("horaInicioCampo", "--:--");
                String hFin = obj.optString("horaFinCampo", "--:--");
                String mot = obj.optString("motivoCampo", "");
                boolean lockProg = obj.optBoolean("lockProg", false);

                currentReport.setEsSalidaCampo(isCampo);
                currentReport.setHoraInicioCampo(hIni);
                currentReport.setHoraFinCampo(hFin);
                currentReport.setMotivoSalidaCampo(mot);

                suppressCampoDialog = true;
                swSalidaCampo.setChecked(isCampo);
                suppressCampoDialog = false;
                llCampoDetalleContainer.setVisibility(isCampo ? View.VISIBLE : View.GONE);
                tvCampoStartTime.setText(hIni);
                tvCampoEndTime.setText(hFin);
                etMotivoCampo.setText(mot);
                applyProgrammingLockState(lockProg);

                JSONArray arr = obj.optJSONArray("acts");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject a = arr.optJSONObject(i);
                        if (a != null) {
                            currentReport.getActividades().add(new KpiActivityItem(
                                    "ACT-" + i,
                                    String.format(Locale.getDefault(), "%02d", i + 1),
                                    a.optString("proyecto", ""),
                                    a.optString("tipo", "Desarrollo"),
                                    a.optString("descripcion", ""),
                                    a.optString("horaInicio", "--:--"),
                                    a.optString("horaFin", "--:--"),
                                    a.optString("estado", "Entregado"),
                                    a.optInt("bugs", 0),
                                    a.optString("commit", "")
                            ));
                        }
                    }
                }

                JSONObject critObj = obj.optJSONObject("criterios");
                if (critObj != null && currentReport.getCriteriosEvaluacion() != null) {
                    java.util.Iterator<String> it = critObj.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        currentReport.getCriteriosEvaluacion().put(k, critObj.optBoolean(k, true));
                    }
                }
                boolean huboDif = obj.optBoolean("huboDificultad", false);
                String detDif = obj.optString("detalleDificultad", "");
                currentReport.setHuboDificultad(huboDif);
                currentReport.setDetalleDificultad(detDif);
                if (rgDificultad != null) {
                    rgDificultad.check(huboDif ? R.id.rbDificultadSi : R.id.rbDificultadNo);
                }
                if (etDetalleDificultad != null) {
                    etDetalleDificultad.setText(detDif);
                }
                poblarEvaluacionRapida();
            } catch (Exception ignored) {}
        } else {
            poblarEvaluacionRapida();
            etPracticante.setText(currentReport.getPracticante());
            currentReport.setTurno(shiftIdx == 1 ? "Turno Tarde (03:30 PM - 06:30 PM)" : "Turno Mañana (08:00 AM - 01:30 PM)");
            currentReport.setEsSalidaCampo(false);
            currentReport.setHoraInicioCampo("--:--");
            currentReport.setHoraFinCampo("--:--");
            currentReport.setMotivoSalidaCampo("");

            suppressCampoDialog = true;
            swSalidaCampo.setChecked(false);
            suppressCampoDialog = false;
            llCampoDetalleContainer.setVisibility(View.GONE);
            tvCampoStartTime.setText("--:--");
            tvCampoEndTime.setText("--:--");
            etMotivoCampo.setText("");
            applyProgrammingLockState(false);

            currentReport.getActividades().clear();
            etDetalleDificultad.setText("");
        }

        activityAdapter.setReadOnly(isCurrentDaySent);
        refreshWeekDayStripUi();
        applyDayReadOnlyVisuals();
        recalcularVistasMetricas();
    }

    private void loadShiftData(int shiftIdx) {
        currentShiftIndex = shiftIdx;
        refreshShiftTabsUi();
        String shiftLabel = (shiftIdx == 0) ? "TURNO MAÑANA (08:00 AM - 01:30 PM)" : "TURNO TARDE (03:30 PM - 06:30 PM)";
        if (tvHeaderShiftBadge != null) {
            tvHeaderShiftBadge.setText(shiftLabel);
        }

        SharedPreferences prefs = getWeekPrefs();
        isCurrentDaySent = prefs.getBoolean("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_sent", false);
        String savedJson = prefs.getString("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_json", null);

        currentReport.getActividades().clear();

        if (savedJson != null) {
            try {
                JSONObject obj = new JSONObject(savedJson);
                etPracticante.setText(obj.optString("practicante", currentReport.getPracticante()));
                boolean isCampo = obj.optBoolean("esSalidaCampo", false);
                String hIni = obj.optString("horaInicioCampo", "--:--");
                String hFin = obj.optString("horaFinCampo", "--:--");
                String mot = obj.optString("motivoCampo", "");
                boolean lockProg = obj.optBoolean("lockProg", false);

                currentReport.setEsSalidaCampo(isCampo);
                currentReport.setHoraInicioCampo(hIni);
                currentReport.setHoraFinCampo(hFin);
                currentReport.setMotivoSalidaCampo(mot);

                suppressCampoDialog = true;
                swSalidaCampo.setChecked(isCampo);
                suppressCampoDialog = false;
                llCampoDetalleContainer.setVisibility(isCampo ? View.VISIBLE : View.GONE);
                tvCampoStartTime.setText(hIni);
                tvCampoEndTime.setText(hFin);
                etMotivoCampo.setText(mot);
                applyProgrammingLockState(lockProg);

                JSONArray arr = obj.optJSONArray("acts");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject a = arr.optJSONObject(i);
                        if (a != null) {
                            currentReport.getActividades().add(new KpiActivityItem(
                                    "ACT-" + i,
                                    String.format(Locale.getDefault(), "%02d", i + 1),
                                    a.optString("proyecto", ""),
                                    a.optString("tipo", "Desarrollo"),
                                    a.optString("descripcion", ""),
                                    a.optString("horaInicio", "--:--"),
                                    a.optString("horaFin", "--:--"),
                                    a.optString("estado", "Entregado"),
                                    a.optInt("bugs", 0),
                                    a.optString("commit", "")
                            ));
                        }
                    }
                }

                JSONObject critObj = obj.optJSONObject("criterios");
                if (critObj != null && currentReport.getCriteriosEvaluacion() != null) {
                    java.util.Iterator<String> it = critObj.keys();
                    while (it.hasNext()) {
                        String k = it.next();
                        currentReport.getCriteriosEvaluacion().put(k, critObj.optBoolean(k, true));
                    }
                }
                boolean huboDif = obj.optBoolean("huboDificultad", false);
                String detDif = obj.optString("detalleDificultad", "");
                currentReport.setHuboDificultad(huboDif);
                currentReport.setDetalleDificultad(detDif);
                if (rgDificultad != null) {
                    rgDificultad.check(huboDif ? R.id.rbDificultadSi : R.id.rbDificultadNo);
                }
                if (etDetalleDificultad != null) {
                    etDetalleDificultad.setText(detDif);
                }
                poblarEvaluacionRapida();
            } catch (Exception ignored) {}
        } else {
            poblarEvaluacionRapida();
            currentReport.setEsSalidaCampo(false);
            currentReport.setHoraInicioCampo("--:--");
            currentReport.setHoraFinCampo("--:--");
            currentReport.setMotivoSalidaCampo("");
            suppressCampoDialog = true;
            swSalidaCampo.setChecked(false);
            suppressCampoDialog = false;
            llCampoDetalleContainer.setVisibility(View.GONE);
            tvCampoStartTime.setText("--:--");
            tvCampoEndTime.setText("--:--");
            etMotivoCampo.setText("");
            applyProgrammingLockState(false);
            currentReport.getActividades().clear();
        }

        if (activityAdapter != null) {
            activityAdapter.setReadOnly(isCurrentDaySent);
            activityAdapter.notifyDataSetChanged();
        }
        applyDayReadOnlyVisuals();
        recalcularVistasMetricas();
    }

    private void applyDayReadOnlyVisuals() {
        int todayIdx = getTodayWeekDayIndex();
        boolean isFuture = selectedDayIndex > todayIdx;

        int curShift = (spTurno != null) ? spTurno.getSelectedItemPosition() : 0;
        if (curShift < 0) curShift = 0;
        String shiftShort = (curShift == 0) ? "Turno Mañana" : "Turno Tarde";
        String shiftLabel = (curShift == 0) ? "TURNO MAÑANA (08:00 AM - 01:30 PM)" : "TURNO TARDE (03:30 PM - 06:30 PM)";
        if (tvHeaderShiftBadge != null) {
            tvHeaderShiftBadge.setText(shiftLabel);
        }

        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(selectedDayIndex);
        boolean hasAttendance = (att != null && (att.morningAttended || att.afternoonAttended));

        boolean ro = isCurrentDaySent || isFuture || !hasAttendance;
        etPracticante.setEnabled(!ro);
        spSupervisor.setEnabled(!ro);
        spTurno.setEnabled(!ro && att != null && att.morningAttended && att.afternoonAttended);
        swSalidaCampo.setEnabled(!ro);
        if (llCampoStartTime != null) llCampoStartTime.setEnabled(!ro);
        if (llCampoEndTime != null) llCampoEndTime.setEnabled(!ro);
        etMotivoCampo.setEnabled(!ro);
        btnAddActivity.setEnabled(!ro);
        btnAddActivity.setVisibility(ro ? View.GONE : View.VISIBLE);
        etDetalleDificultad.setEnabled(!ro);
        if (rgDificultad != null) {
            for (int i = 0; i < rgDificultad.getChildCount(); i++) {
                rgDificultad.getChildAt(i).setEnabled(!ro);
            }
        }

        float alpha = ro ? 0.45f : 1.0f;
        if (llProgrammerFormContent != null) {
            for (int i = 0; i < llProgrammerFormContent.getChildCount(); i++) {
                View child = llProgrammerFormContent.getChildAt(i);
                if (child != weekStripCard && child.findViewById(R.id.btnSaveForm) == null && child != btnSaveForm) {
                    child.setAlpha(alpha);
                }
            }
        }

        btnClearForm.setVisibility(ro ? View.GONE : View.VISIBLE);

        if (!hasAttendance) {
            tvProgrammerReadOnlyBanner.setVisibility(View.VISIBLE);
            tvProgrammerReadOnlyBanner.setText("Sin turno de asistencia en base de datos para " + FULL_DAYS[selectedDayIndex] + ".");
            btnSaveForm.setEnabled(false);
            btnSaveForm.setAlpha(0.45f);
            btnSaveForm.setText("Sin Asistencia en BD (" + FULL_DAYS[selectedDayIndex] + ")");
        } else if (isFuture) {
            tvProgrammerReadOnlyBanner.setVisibility(View.VISIBLE);
            tvProgrammerReadOnlyBanner.setText("Día futuro (" + FULL_DAYS[selectedDayIndex] + ") bloqueado: aún no comienza la jornada.");
            btnSaveForm.setEnabled(false);
            btnSaveForm.setAlpha(0.45f);
            btnSaveForm.setText("Día Futuro Bloqueado");
        } else if (isCurrentDaySent) {
            tvProgrammerReadOnlyBanner.setVisibility(View.VISIBLE);
            tvProgrammerReadOnlyBanner.setText("🔒 Ficha de " + FULL_DAYS[selectedDayIndex] + " (" + shiftShort + ") enviada y sellada con huella (Solo lectura)");
            tvProgrammerReadOnlyBanner.setOnClickListener(null);
            tvProgrammerReadOnlyBanner.setClickable(false);

            // Si el otro turno del mismo día aún está pendiente, permitir pasar a él:
            if (att != null && att.morningAttended && att.afternoonAttended) {
                int otherShift = (curShift == 0) ? 1 : 0;
                boolean otherSent = getWeekPrefs().getBoolean("day_" + selectedDayIndex + "_shift_" + otherShift + "_sent", false);
                if (!otherSent) {
                    btnSaveForm.setEnabled(true);
                    btnSaveForm.setAlpha(1.0f);
                    btnSaveForm.setText("Pasar a " + (otherShift == 1 ? "Turno Tarde" : "Turno Mañana"));
                    btnSaveForm.setOnClickListener(v -> {
                        if (spTurno != null) spTurno.setSelection(otherShift);
                        loadShiftData(otherShift);
                        scrollFormToTop();
                    });
                    return;
                }
            }

            int nextIdx = getNextUnsentDayIndexAfter(selectedDayIndex);
            if (nextIdx != -1) {
                btnSaveForm.setEnabled(true);
                btnSaveForm.setAlpha(1.0f);
                btnSaveForm.setText("Ir al siguiente día pendiente (" + FULL_DAYS[nextIdx] + ")");
                btnSaveForm.setOnClickListener(v -> jumpToNextPendingProgrammerDay());
            } else {
                btnSaveForm.setEnabled(false);
                btnSaveForm.setAlpha(0.45f);
                btnSaveForm.setText("Jornada Enviada y Sellada con Huella ✓");
                btnSaveForm.setOnClickListener(null);
            }
        } else {
            tvProgrammerReadOnlyBanner.setVisibility(View.GONE);
            btnSaveForm.setEnabled(true);
            btnSaveForm.setAlpha(1.0f);
            btnSaveForm.setText("Enviar Ficha de " + FULL_DAYS[selectedDayIndex] + " (" + shiftShort + ") con Huella");
            btnSaveForm.setOnClickListener(v -> submitProgrammerWithBiometrics());
        }
    }

    private JSONObject buildCurrentReportJson() throws Exception {
        JSONObject obj = new JSONObject();
        if (etPracticante != null) {
            obj.put("practicante", etPracticante.getText().toString().trim());
        } else {
            obj.put("practicante", currentReport.getPracticante());
        }
        int shiftIdx = (spTurno != null) ? spTurno.getSelectedItemPosition() : 0;
        obj.put("turnoIdx", shiftIdx);
        obj.put("esSalidaCampo", currentReport.isEsSalidaCampo());
        obj.put("horaInicioCampo", currentReport.getHoraInicioCampo());
        obj.put("horaFinCampo", currentReport.getHoraFinCampo());
        obj.put("motivoCampo", currentReport.getMotivoSalidaCampo());
        obj.put("lockProg", programmingLockedByCampo);

        JSONArray arr = new JSONArray();
        for (KpiActivityItem act : currentReport.getActividades()) {
            JSONObject a = new JSONObject();
            a.put("proyecto", act.getProyecto());
            a.put("tipo", act.getTipo());
            a.put("descripcion", act.getDescripcion());
            a.put("horaInicio", act.getHoraInicio());
            a.put("horaFin", act.getHoraFin());
            a.put("estado", act.getEstado());
            a.put("bugs", act.getBugs());
            a.put("commit", act.getCommitHash());
            arr.put(a);
        }
        obj.put("acts", arr);

        // Sección 04 / 05: Criterios de Evaluación Rápida del Día
        JSONObject evalObj = new JSONObject();
        if (currentReport.getCriteriosEvaluacion() != null) {
            for (Map.Entry<String, Boolean> entry : currentReport.getCriteriosEvaluacion().entrySet()) {
                evalObj.put(entry.getKey(), entry.getValue());
            }
        }
        obj.put("criterios", evalObj);

        // Sección 05: Incidencias del Día
        boolean dif = (rgDificultad != null && rgDificultad.getCheckedRadioButtonId() == R.id.rbDificultadSi)
                || currentReport.isHuboDificultad();
        String det = (etDetalleDificultad != null) ? etDetalleDificultad.getText().toString().trim()
                : currentReport.getDetalleDificultad();
        obj.put("huboDificultad", dif);
        obj.put("detalleDificultad", det != null ? det : "");

        obj.put("observacionSupervisor", currentReport.getObservacionSupervisor());
        obj.put("firmaPracticante", currentReport.getFirmaPracticante());
        obj.put("firmaSupervisor", currentReport.getFirmaSupervisor());
        obj.put("cumplimientoMeta", currentReport.getCumplimientoMeta());
        return obj;
    }

    private void saveCurrentDraft() {
        if (getContext() == null || currentReport == null) return;
        try {
            JSONObject obj = buildCurrentReportJson();
            int shiftIdx = (spTurno != null) ? spTurno.getSelectedItemPosition() : 0;
            SharedPreferences prefs = getWeekPrefs();
            if (prefs != null) {
                prefs.edit()
                        .putString("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_json", obj.toString())
                        .putString("day_" + selectedDayIndex + "_json", obj.toString())
                        .apply();
            }
        } catch (Exception ignored) {}
    }

    private void persistCurrentDayToWeekPrefs() {
        try {
            JSONObject obj = buildCurrentReportJson();
            int shiftIdx = currentShiftIndex;
            SharedPreferences prefs = getWeekPrefs();
            if (prefs != null) {
                KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
                KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(selectedDayIndex);
                boolean hasDoubleShift = (att != null && att.morningAttended && att.afternoonAttended);

                SharedPreferences.Editor editor = prefs.edit()
                        .putBoolean("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_sent", true)
                        .putString("day_" + selectedDayIndex + "_shift_" + shiftIdx + "_json", obj.toString())
                        .putString("day_" + selectedDayIndex + "_json", obj.toString());

                if (!hasDoubleShift) {
                    editor.putBoolean("day_" + selectedDayIndex + "_sent", true);
                } else {
                    int other = (shiftIdx == 0) ? 1 : 0;
                    boolean otherSent = prefs.getBoolean("day_" + selectedDayIndex + "_shift_" + other + "_sent", false);
                    if (otherSent) {
                        editor.putBoolean("day_" + selectedDayIndex + "_sent", true);
                    }
                }
                editor.apply();
            }
        } catch (Exception ignored) {}
    }

    private KpiActivityItem createBlankProgrammingActivity(int index) {
        String formatted = String.format(Locale.getDefault(), "%02d", index);
        return new KpiActivityItem(
                "ACT-" + System.currentTimeMillis() + "-" + index,
                formatted,
                "",
                "Desarrollo",
                "",
                "--:--",
                "--:--",
                "Pendiente",
                0,
                ""
        );
    }

    private void poblarMetasProduccionCompactas() {
        llGoalsContainer.removeAllViews();
        if (currentReport.getMetas().isEmpty()) {
            TextView pending = new TextView(requireContext());
            pending.setText("Metas oficiales pendientes de configuración");
            pending.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            pending.setTextSize(12);
            llGoalsContainer.addView(pending);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(getContext());

        pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine template =
                pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine.getInstance(requireContext());
        int goalIndex = 0;

        for (KpiProductionGoal goal : currentReport.getMetas()) {
            View card = inflater.inflate(R.layout.kpi_item_goal_row, llGoalsContainer, false);
            TextView tvIcon = card.findViewById(R.id.tvGoalIcon);
            TextView tvCat = card.findViewById(R.id.tvGoalCategory);
            TextView tvTarget = card.findViewById(R.id.tvGoalTarget);

            String iconSymbol;
            String shortName;
            String compactNumber;

            switch (goal.getTipoIcono()) {
                case "dev":
                    iconSymbol = "< >";
                    shortName = "Desarrollo";
                    compactNumber = "1";
                    break;
                case "bug":
                    iconSymbol = "FIX";
                    shortName = "Corrección";
                    compactNumber = "2-4";
                    break;
                case "improve":
                    iconSymbol = "OPT";
                    shortName = "Mejoras";
                    compactNumber = "2-4";
                    break;
                case "layout":
                    iconSymbol = "UI";
                    shortName = "Maquetación";
                    compactNumber = "1";
                    break;
                case "api":
                    iconSymbol = "API";
                    shortName = "Integr. API";
                    compactNumber = "1";
                    break;
                default:
                    iconSymbol = "DEV";
                    shortName = "Mantenim.";
                    compactNumber = "2-4";
                    break;
            }

            if (goalIndex < 3) {
                String edited = template.getSemanticField("Programador Junior", "prog_goal_title_" + goalIndex, null);
                if (edited != null && !edited.isEmpty()) shortName = edited;
            }
            goalIndex++;
            tvIcon.setText(iconSymbol);
            tvIcon.setTextColor(ContextCompat.getColor(requireContext(), goal.getColorResId()));
            tvCat.setText(shortName);
            tvTarget.setText(goal.getMetaTexto());

            llGoalsContainer.addView(card);
        }
    }

    private void poblarEvaluacionRapida() {
        llEvaluationCriteria.removeAllViews();
        pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine template =
                pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine.getInstance(requireContext());
        int criterionIndex = 0;

        for (Map.Entry<String, Boolean> entry : currentReport.getCriteriosEvaluacion().entrySet()) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            TextView tvCriterio = new TextView(getContext());
            tvCriterio.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tvCriterio.setText(template.getSemanticField("Programador Junior",
                    "eval_criterion_" + criterionIndex++, entry.getKey()));
            tvCriterio.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            tvCriterio.setTextSize(12);

            CheckBox cbCumple = new CheckBox(getContext());
            cbCumple.setText("Cumple");
            cbCumple.setChecked(entry.getValue());
            cbCumple.setEnabled(!isCurrentDaySent);
            cbCumple.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_secondary));
            cbCumple.setOnCheckedChangeListener((btn, isChecked) -> {
                entry.setValue(isChecked);
                saveCurrentDraft();
            });

            row.addView(tvCriterio);
            row.addView(cbCumple);
            llEvaluationCriteria.addView(row);
        }
    }

    private void applyProgrammingLockState(boolean lockProgramming) {
        this.programmingLockedByCampo = lockProgramming;
        if (cardActivitiesLockedByCampo != null) {
            cardActivitiesLockedByCampo.setVisibility(lockProgramming ? View.VISIBLE : View.GONE);
        }
        if (llProgrammingActivitiesBlock != null) {
            llProgrammingActivitiesBlock.setVisibility(lockProgramming ? View.GONE : View.VISIBLE);
        }
        if (llProgrammingExtraSections != null) {
            llProgrammingExtraSections.setVisibility(lockProgramming ? View.GONE : View.VISIBLE);
        }
    }

    private void recalcularVistasMetricas() {
        currentReport.recalcularResumen();

        int porcentaje = currentReport.getCumplimientoMeta();
        pbLiveProgress.setProgress(porcentaje);

        if (currentReport.isEsSalidaCampo() && programmingLockedByCampo) {
            tvLivePercent.setText("100 % (JUSTIFICADO)");
            String hIni = currentReport.getHoraInicioCampo() != null ? currentReport.getHoraInicioCampo() : "--:--";
            String hFin = currentReport.getHoraFinCampo() != null ? currentReport.getHoraFinCampo() : "--:--";
            tvLiveSummary.setText("SALIDA A CAMPO (" + hIni + " - " + hFin + ") · KPI PROTEGIDO AL 100%");
        } else if (currentReport.isEsSalidaCampo()) {
            tvLivePercent.setText("100 % (JUSTIFICADO)");
            tvLiveSummary.setText("SALIDA A CAMPO + " + currentReport.getTareasAsignadas() + " ACTIVIDAD(ES) TÉCNICAS · KPI 100%");
        } else {
            tvLivePercent.setText(porcentaje + " %");
            tvLiveSummary.setText(currentReport.getTareasAsignadas() + " ACTIVIDADES · "
                    + currentReport.getTareasCompletadas() + " COMPLETADAS · "
                    + currentReport.getTareasPendientes() + " PENDIENTES");
        }

        llSummaryRows.removeAllViews();
        if (currentReport.isEsSalidaCampo()) {
            agregarFilaResumen("Modalidad del turno", "Salida a campo / Labor externa");
            String hIni = currentReport.getHoraInicioCampo() != null ? currentReport.getHoraInicioCampo() : "--:--";
            String hFin = currentReport.getHoraFinCampo() != null ? currentReport.getHoraFinCampo() : "--:--";
            agregarFilaResumen("Horario salida a campo", hIni + " a " + hFin);
            String justif = currentReport.getMotivoSalidaCampo();
            agregarFilaResumen("Justificación", (justif != null && !justif.trim().isEmpty()) ? justif.trim() : "Pendiente de escribir");
            agregarFilaResumen("Protección de KPI", "100% JUSTIFICADO (Sin penalización)");
        }

        if (!programmingLockedByCampo) {
            agregarFilaResumen("Tareas registradas", String.valueOf(currentReport.getTareasAsignadas()));
            agregarFilaResumen("Tareas entregadas", String.valueOf(Math.max(0, currentReport.getTareasCompletadas())));
            agregarFilaResumen("Tareas pendientes", String.valueOf(currentReport.getTareasPendientes()));
            agregarFilaResumen("Funcionalidades desarrolladas", String.valueOf(currentReport.getFuncionalidadesDesarrolladas()));
            agregarFilaResumen("Incidencias corregidas", String.valueOf(currentReport.getIncidenciasCorregidas()));
            if (currentReport.getCountMejora() > 0) {
                agregarFilaResumen("Mejoras / Cambios", String.valueOf(currentReport.getCountMejora()));
            }
            if (currentReport.getCountMaquetacion() > 0) {
                agregarFilaResumen("Maquetación / Frontend", String.valueOf(currentReport.getCountMaquetacion()));
            }
            if (currentReport.getCountApi() > 0) {
                agregarFilaResumen("Integración / API", String.valueOf(currentReport.getCountApi()));
            }
            if (currentReport.getCountMantenimiento() > 0) {
                agregarFilaResumen("Mantenimiento Web", String.valueOf(currentReport.getCountMantenimiento()));
            }
            agregarFilaResumen("Bugs detectados", String.valueOf(currentReport.getBugsDetectados()));
        }

        tvSummaryMetaPercent.setText(porcentaje + " %");
        actualizarNivelProductividad(currentReport.getNivelProductividad());
    }

    private void agregarFilaResumen(String indicador, String resultado) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvInd = new TextView(getContext());
        tvInd.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        tvInd.setText(indicador);
        tvInd.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        tvInd.setTextSize(12);

        TextView tvRes = new TextView(getContext());
        tvRes.setText(resultado);
        tvRes.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tvRes.setTextSize(12);
        tvRes.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvInd);
        row.addView(tvRes);
        llSummaryRows.addView(row);
    }

    private void actualizarNivelProductividad(String nivel) {
        rbNivelExcelente.setChecked("EXCELENTE".equalsIgnoreCase(nivel));
        rbNivelMuyBueno.setChecked("MUY BUENO".equalsIgnoreCase(nivel));
        rbNivelBueno.setChecked("BUENO".equalsIgnoreCase(nivel));
        rbNivelRegular.setChecked("REGULAR".equalsIgnoreCase(nivel));
        rbNivelDeficiente.setChecked("DEFICIENTE".equalsIgnoreCase(nivel));
    }

    private void showCampoTimePicker(boolean isStart) {
        if (isCurrentDaySent) return;
        String currentText = isStart ? currentReport.getHoraInicioCampo() : currentReport.getHoraFinCampo();
        boolean isTarde = spTurno != null && spTurno.getSelectedItemPosition() == 1;
        int defaultHour = isTarde ? (isStart ? 15 : 18) : (isStart ? 8 : 13);
        int defaultMinute = isTarde ? 30 : (isStart ? 0 : 30);

        if (currentText != null && currentText.contains(":") && !currentText.contains("--")) {
            try {
                String[] parts = currentText.trim().split("[: ]");
                if (parts.length >= 2) {
                    int h = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    boolean isPm = currentText.toLowerCase().contains("pm");
                    if (isPm && h < 12) h += 12;
                    if (!isPm && h == 12) h = 0;
                    defaultHour = h;
                    defaultMinute = m;
                }
            } catch (Exception ignored) {}
        }

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String amPm = hourOfDay >= 12 ? "pm" : "am";
            int hour12 = hourOfDay % 12;
            if (hour12 == 0) hour12 = 12;
            String formatted = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);

            if (isStart) {
                currentReport.setHoraInicioCampo(formatted);
                tvCampoStartTime.setText(formatted);
            } else {
                currentReport.setHoraFinCampo(formatted);
                tvCampoEndTime.setText(formatted);
            }
            recalcularVistasMetricas();
        }, defaultHour, defaultMinute, false);

        dialog.show();
    }

    private void setupListeners() {
        if (btnProgrammerBack != null) {
            btnProgrammerBack.setOnClickListener(v -> {
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        if (btnProgrammerGoToReport != null) {
            btnProgrammerGoToReport.setOnClickListener(v -> {
                if (getActivity() != null) {
                    String collabName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance()
                            .getAuthenticatedUserName();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main, pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment.newInstanceExclusive("Programador Junior", collabName))
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        View.OnClickListener dateClickListener = v -> {
            if (isCurrentDaySent) {
                jumpToNextPendingProgrammerDay();
                return;
            }
            Calendar calendar = getWeekDayCalendar(selectedDayIndex);
            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                String formattedDate = String.format(Locale.getDefault(), "%02d / %02d / %04d", dayOfMonth, month + 1, year);
                tvFormDate.setText(formattedDate);
                currentReport.setFecha(formattedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        };

        if (llDatePicker != null) llDatePicker.setOnClickListener(dateClickListener);
        tvFormDate.setOnClickListener(dateClickListener);

        spTurno.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean isAfternoon = (position == 1);
                if (activityAdapter != null) {
                    activityAdapter.setAfternoonShift(isAfternoon);
                }
                currentReport.setTurno(isAfternoon
                        ? "Turno Tarde (2:00 pm - 6:40 pm)"
                        : "Turno Mañana (8:00 am - 1:30 pm)");
                loadShiftData(position);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        View.OnClickListener campoStartClick = v -> showCampoTimePicker(true);
        if (llCampoStartTime != null) llCampoStartTime.setOnClickListener(campoStartClick);
        if (tvCampoStartTime != null) tvCampoStartTime.setOnClickListener(campoStartClick);

        View.OnClickListener campoEndClick = v -> showCampoTimePicker(false);
        if (llCampoEndTime != null) llCampoEndTime.setOnClickListener(campoEndClick);
        if (tvCampoEndTime != null) tvCampoEndTime.setOnClickListener(campoEndClick);

        swSalidaCampo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isCurrentDaySent) return;
            currentReport.setEsSalidaCampo(isChecked);
            llCampoDetalleContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (suppressCampoDialog) {
                recalcularVistasMetricas();
                return;
            }

            if (isChecked) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Salida a campo activada")
                        .setMessage("Se limpiarán las actividades técnicas porque saliste a realizar labores de la empresa fuera de tu área (tu KPI queda en 100% sin penalización).\n\n¿Deseas de igual manera agregar actividades de programación a tu KPI?")
                        .setCancelable(false)
                        .setPositiveButton("No, solo Salida a campo", (d, w) -> {
                            currentReport.getActividades().clear();
                            activityAdapter.notifyDataSetChanged();
                            applyProgrammingLockState(true);
                            recalcularVistasMetricas();
                        })
                        .setNegativeButton("Sí, agregar programación", (d, w) -> {
                            currentReport.getActividades().clear();
                            currentReport.getActividades().add(createBlankProgrammingActivity(1));
                            activityAdapter.notifyDataSetChanged();
                            applyProgrammingLockState(false);
                            recalcularVistasMetricas();
                        })
                        .show();
            } else {
                applyProgrammingLockState(false);
                if (currentReport.getActividades().isEmpty()) {
                    currentReport.getActividades().add(createBlankProgrammingActivity(1));
                    activityAdapter.notifyDataSetChanged();
                }
                recalcularVistasMetricas();
            }
        });

        if (btnUnlockProgrammingActivities != null) {
            btnUnlockProgrammingActivities.setOnClickListener(v -> {
                if (isCurrentDaySent) return;
                applyProgrammingLockState(false);
                if (currentReport.getActividades().isEmpty()) {
                    currentReport.getActividades().add(createBlankProgrammingActivity(1));
                    activityAdapter.notifyDataSetChanged();
                }
                recalcularVistasMetricas();
            });
        }

        etMotivoCampo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentReport.setMotivoSalidaCampo(s.toString());
                recalcularVistasMetricas();
            }
            @Override public void afterTextChanged(Editable s) {
                saveCurrentDraft();
            }
        });

        if (rgDificultad != null) {
            rgDificultad.setOnCheckedChangeListener((group, checkedId) -> {
                currentReport.setHuboDificultad(checkedId == R.id.rbDificultadSi);
                saveCurrentDraft();
            });
        }

        if (etDetalleDificultad != null) {
            etDetalleDificultad.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentReport.setDetalleDificultad(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {
                    saveCurrentDraft();
                }
            });
        }

        btnAddActivity.setOnClickListener(v -> {
            if (isCurrentDaySent) return;
            int nuevoNumero = currentReport.getActividades().size() + 1;
            KpiActivityItem nuevaActividad = createBlankProgrammingActivity(nuevoNumero);
            currentReport.getActividades().add(nuevaActividad);
            activityAdapter.notifyItemInserted(currentReport.getActividades().size() - 1);
            recalcularVistasMetricas();
            saveCurrentDraft();
        });

        btnClearForm.setOnClickListener(v -> {
            if (isCurrentDaySent) return;
            suppressCampoDialog = true;
            swSalidaCampo.setChecked(false);
            suppressCampoDialog = false;
            llCampoDetalleContainer.setVisibility(View.GONE);
            currentReport.setEsSalidaCampo(false);
            currentReport.setHoraInicioCampo("--:--");
            currentReport.setHoraFinCampo("--:--");
            currentReport.setMotivoSalidaCampo("");
            tvCampoStartTime.setText("--:--");
            tvCampoEndTime.setText("--:--");
            etMotivoCampo.setText("");
            applyProgrammingLockState(false);

            currentReport.getActividades().clear();
            activityAdapter.notifyDataSetChanged();
            recalcularVistasMetricas();
            saveCurrentDraft();
            Toast.makeText(getContext(), "Formulario reiniciado en cero", Toast.LENGTH_SHORT).show();
        });

        btnSaveForm.setOnClickListener(v -> submitProgrammerWithBiometrics());
    }

    private void submitProgrammerWithBiometrics() {
        if (isCurrentDaySent) {
            int nextIdx = getNextUnsentDayIndexAfter(selectedDayIndex);
            if (nextIdx != -1) {
                jumpToNextPendingProgrammerDay();
            }
            return;
        }

        if (currentReport.isEsSalidaCampo()) {
            if ("--:--".equals(currentReport.getHoraInicioCampo()) || "--:--".equals(currentReport.getHoraFinCampo())) {
                Toast.makeText(getContext(), "Selecciona la Hora inicio y Hora fin de tu Salida a campo", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentReport.getMotivoSalidaCampo() == null || currentReport.getMotivoSalidaCampo().trim().isEmpty()) {
                etMotivoCampo.setError("Escribe tu justificación");
                Toast.makeText(getContext(), "Escribe la justificación de tu Salida a campo", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final int pct = currentReport.getCumplimientoMeta();
        final String fecha = tvFormDate != null ? tvFormDate.getText().toString() : "Hoy";
        final String obs = currentReport.isEsSalidaCampo()
                ? ("Salida a campo (" + currentReport.getHoraInicioCampo() + " - " + currentReport.getHoraFinCampo() + "): " + currentReport.getMotivoSalidaCampo())
                : "";

        int cShift = currentShiftIndex;
        String promptShift = (cShift == 1) ? "Turno Tarde" : "Turno Mañana";

        pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper.authenticate(this,
                "Enviar KPI de " + FULL_DAYS[selectedDayIndex] + " (" + promptShift + ") · Programador Jr",
                new pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper.BiometricCallback() {
                    @Override
                    public void onSuccess() {
                        persistProgrammerEvaluation(fecha, pct, obs);
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) {
                        if (errorCode == -1) {
                            persistProgrammerEvaluation(fecha, pct, obs);
                        } else {
                            Toast.makeText(getContext(), "Verificación biométrica cancelada", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailed() {
                        Toast.makeText(getContext(), "Biometría no reconocida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void persistProgrammerEvaluation(String fecha, int pct, String obs) {
        if (getContext() == null) return;
        pe.net.ws.kpiweb.core.session.SessionManager session =
                new pe.net.ws.kpiweb.core.session.SessionManager(requireContext());
        boolean realAccount = session.isLoggedIn();

        final int justSentIdx = selectedDayIndex;
        final String sentDayName = FULL_DAYS[justSentIdx];

        for (KpiActivityItem act : currentReport.getActividades()) {
            if (act.getProyecto() != null && !act.getProyecto().trim().isEmpty()) {
                pe.net.ws.kpiweb.features.kpi.data.KpiProjectSuggestionsStore.saveProject(
                        requireContext(), act.getProyecto());
            }
        }

        persistCurrentDayToWeekPrefs();
        pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.enqueue(
                requireContext(), "PROGRAMACION", fecha + ":" + currentShiftIndex, currentReport);

        if (!realAccount) {
            // The legacy summary uses a sample code and target; never register it
            // as a real worker's KPI evaluation without an official definition.
            pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository.getInstance(requireContext())
                    .saveRoleDailyEvaluation("Programador", fecha, "KPI-PROG-01",
                            "Evaluación Diaria · Programador Junior (" + sentDayName + ")",
                            currentReport.isEsSalidaCampo() ? 4.0 : currentReport.getActividades().size(),
                            4.0, pct, currentReport.isEsSalidaCampo(), obs);
        }

        KpiAttendanceRepository attRepo = KpiAttendanceRepository.getInstance(requireContext());
        KpiAttendanceRepository.AttendanceDay att = attRepo.getDayAttendance(justSentIdx);
        int currentShift = currentShiftIndex;

        if (att != null && att.morningAttended && att.afternoonAttended) {
            int otherShift = (currentShift == 0) ? 1 : 0;
            boolean otherSent = getWeekPrefs().getBoolean("day_" + justSentIdx + "_shift_" + otherShift + "_sent", false);
            if (!otherSent) {
                currentShiftIndex = otherShift;
                if (spTurno != null) {
                    spTurno.setSelection(otherShift);
                }
                loadShiftData(otherShift);
                refreshShiftTabsUi();
                scrollFormToTop();
                if (realAccount) showRealProgrammerCapture(sentDayName, pct);
                else Toast.makeText(requireContext(),
                        "✓ Turno guardado. Ahora puedes registrar el " + (otherShift == 1 ? "Turno Tarde" : "Turno Mañana") + ".",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        refreshShiftTabsUi();

        int nextIdx = getNextUnsentDayIndexAfter(justSentIdx);
        if (nextIdx == -1 || justSentIdx == 5) {
            loadSelectedDayData();
            scrollFormToTop();
            if (realAccount) {
                showRealProgrammerCapture(sentDayName, pct);
                return;
            }
            KpiPandaSuccessDialog.show(requireContext(), "Programador Junior", true,
                    nextIdx == -1 ? "Semana Completa" : sentDayName,
                    pct, Math.max(10, pct), () -> {
                        if (nextIdx != -1) {
                            selectedDayIndex = nextIdx;
                            loadSelectedDayData();
                            scrollFormToTop();
                        }
                    });
        } else {
            selectedDayIndex = nextIdx;
            loadSelectedDayData();
            scrollFormToTop();
            if (realAccount) showRealProgrammerCapture(sentDayName, pct);
            else Toast.makeText(requireContext(),
                    "✓ " + sentDayName + " enviado · Ahora en " + FULL_DAYS[nextIdx], Toast.LENGTH_LONG).show();
        }
    }

    private void showRealProgrammerCapture(String day, int percent) {
        pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper.playDailySuccess(requireContext());
        KpiPandaSuccessDialog.show(requireContext(), "Programador Junior", true,
                day, percent, 0, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentDraft();
    }

    @Override
    public void onDataChanged() {
        recalcularVistasMetricas();
        saveCurrentDraft();
    }

    @Override
    public void onItemDeleted(int position) {
        if (isCurrentDaySent) return;
        if (position >= 0 && position < currentReport.getActividades().size()) {
            currentReport.getActividades().remove(position);
            activityAdapter.notifyItemRemoved(position);
            activityAdapter.notifyItemRangeChanged(position, currentReport.getActividades().size());
            recalcularVistasMetricas();
            saveCurrentDraft();
        }
    }
}
