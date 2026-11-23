package pe.net.ws.kpiweb.features.kpi.designer.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiCustomKeypadDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;

/**
 * Formulario de KPI Semanal (Lunes a Sábado) para Diseñador Gráfico:
 * - Abre posicionado por defecto en el día real actual (ej. Miércoles si hoy es Miércoles).
 * - Permite retroceder libremente a cualquier día anterior pendiente para llenarlo.
 * - Una vez enviado un día con biometría, queda en modo solo lectura con opacidad (0.45f)
 *   y al tocarlo o presionar continuar lleva al siguiente día pendiente con scroll hacia arriba.
 * - Al enviar el KPI del Sábado o completar la semana del Colaborador, reproduce el tono de 2 segundos.
 */
public class KpiDesignerFormFragment extends Fragment {

    private View rootScrollView;

    // Cabecera de Rol y Estilo
    private TextView tvFormRoleHeaderTitle;
    private TextView tvFormRoleHeaderSubtitle;
    private TextView tvFormStyleBadge;

    // Selector de Fecha y Barra de Días (Lunes a Sábado)
    private View btnSelectDateDesigner;
    private TextView tvFormSelectedDate;
    private View btnResetDateToday;

    private LinearLayout btnDayLun, btnDayMar, btnDayMie, btnDayJue, btnDayVie, btnDaySab;
    private TextView tvDayLabelLun, tvDayLabelMar, tvDayLabelMie, tvDayLabelJue, tvDayLabelVie, tvDayLabelSab;
    private TextView tvDayStatusLun, tvDayStatusMar, tvDayStatusMie, tvDayStatusJue, tvDayStatusVie, tvDayStatusSab;

    // Banner Informativo de Estado
    private View containerDayLockBanner;
    private TextView tvLockBannerTitle;
    private TextView tvLockBannerSubtitle;
    private View btnUnlockRecord;

    // Datos de Modalidad
    private TextView tvFormDayName;
    private TextView tvFormModality;
    private TextView tvFormDailyGoal;

    // Títulos dinámicos de los 3 indicadores
    private TextView tvTitleDesigns;
    private TextView tvTitleVideos;
    private TextView tvTitlePhotos;

    // Steppers / Cajas Numéricas
    private View containerStepperDesigns;
    private TextView tvMetaDesignsSubtitle;
    private TextView tvDesignsCount;
    private ImageButton btnMinusDesigns;
    private ImageButton btnPlusDesigns;

    private View containerStepperVideos;
    private TextView tvMetaVideosSubtitle;
    private TextView tvVideosCount;
    private ImageButton btnMinusVideos;
    private ImageButton btnPlusVideos;

    private View containerStepperPhotos;
    private TextView tvMetaPhotosSubtitle;
    private TextView tvPhotosCount;
    private ImageButton btnMinusPhotos;
    private ImageButton btnPlusPhotos;

    // Observaciones
    private EditText etDailyObservations;
    private TextView tvCharCounter;

    // Botones de Acción
    private View btnSaveDailyDraft;
    private View btnSaveDailyProduction;
    private ImageView ivSaveButtonIcon;
    private TextView tvSaveButtonTitle;
    private TextView tvSaveButtonSubtitle;

    // Estado interno
    private final Calendar currentSelectedCalendar = Calendar.getInstance();
    private boolean isDayLocked = false;

    private int countDesigns = 0;
    private int countVideos = 0;
    private int countPhotos = 0;

    private int goalDesigns = 2;
    private int goalVideos = 2;
    private int goalPhotos = 0;

    private KpiRoleProfileConfig activeProfileConfig;

    public static KpiDesignerFormFragment newInstance() {
        return new KpiDesignerFormFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_designer_fragment_form, container, false);
        this.rootScrollView = root;
        KpiDesignerMemoryRepository.init(requireContext());

        initViews(root);
        refreshRoleAndStyleConfig();
        setupDayStripListeners();
        setupSteppers();
        setupDatePicker();
        setupObservationsCounter();
        setupFinalSubmitButton();

        Calendar defaultOpenDay = Calendar.getInstance();
        if (defaultOpenDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            defaultOpenDay.add(Calendar.DAY_OF_MONTH, -1); // Sábado de la semana actual
        }
        loadCalendarDate(defaultOpenDay);

        return root;
    }

    private void initViews(View root) {
        tvFormRoleHeaderTitle = root.findViewById(R.id.tvFormRoleHeaderTitle);
        tvFormRoleHeaderSubtitle = root.findViewById(R.id.tvFormRoleHeaderSubtitle);
        tvFormStyleBadge = root.findViewById(R.id.tvFormStyleBadge);

        btnSelectDateDesigner = root.findViewById(R.id.btnSelectDateDesigner);
        tvFormSelectedDate = root.findViewById(R.id.tvFormSelectedDate);
        btnResetDateToday = root.findViewById(R.id.btnResetDateToday);

        btnDayLun = root.findViewById(R.id.btnDayLun);
        btnDayMar = root.findViewById(R.id.btnDayMar);
        btnDayMie = root.findViewById(R.id.btnDayMie);
        btnDayJue = root.findViewById(R.id.btnDayJue);
        btnDayVie = root.findViewById(R.id.btnDayVie);
        btnDaySab = root.findViewById(R.id.btnDaySab);

        tvDayLabelLun = root.findViewById(R.id.tvDayLabelLun);
        tvDayLabelMar = root.findViewById(R.id.tvDayLabelMar);
        tvDayLabelMie = root.findViewById(R.id.tvDayLabelMie);
        tvDayLabelJue = root.findViewById(R.id.tvDayLabelJue);
        tvDayLabelVie = root.findViewById(R.id.tvDayLabelVie);
        tvDayLabelSab = root.findViewById(R.id.tvDayLabelSab);

        tvDayStatusLun = root.findViewById(R.id.tvDayStatusLun);
        tvDayStatusMar = root.findViewById(R.id.tvDayStatusMar);
        tvDayStatusMie = root.findViewById(R.id.tvDayStatusMie);
        tvDayStatusJue = root.findViewById(R.id.tvDayStatusJue);
        tvDayStatusVie = root.findViewById(R.id.tvDayStatusVie);
        tvDayStatusSab = root.findViewById(R.id.tvDayStatusSab);

        containerDayLockBanner = root.findViewById(R.id.containerDayLockBanner);
        tvLockBannerTitle = root.findViewById(R.id.tvLockBannerTitle);
        tvLockBannerSubtitle = root.findViewById(R.id.tvLockBannerSubtitle);
        btnUnlockRecord = root.findViewById(R.id.btnUnlockRecord);
        if (btnUnlockRecord != null) {
            btnUnlockRecord.setVisibility(View.GONE);
        }

        tvFormDayName = root.findViewById(R.id.tvFormDayName);
        tvFormModality = root.findViewById(R.id.tvFormModality);
        tvFormDailyGoal = root.findViewById(R.id.tvFormDailyGoal);

        tvTitleDesigns = root.findViewById(R.id.tvTitleDesigns);
        tvTitleVideos = root.findViewById(R.id.tvTitleVideos);
        tvTitlePhotos = root.findViewById(R.id.tvTitlePhotos);

        containerStepperDesigns = root.findViewById(R.id.containerStepperDesigns);
        tvMetaDesignsSubtitle = root.findViewById(R.id.tvMetaDesignsSubtitle);
        tvDesignsCount = root.findViewById(R.id.tvDesignsCount);
        btnMinusDesigns = root.findViewById(R.id.btnMinusDesigns);
        btnPlusDesigns = root.findViewById(R.id.btnPlusDesigns);

        containerStepperVideos = root.findViewById(R.id.containerStepperVideos);
        tvMetaVideosSubtitle = root.findViewById(R.id.tvMetaVideosSubtitle);
        tvVideosCount = root.findViewById(R.id.tvVideosCount);
        btnMinusVideos = root.findViewById(R.id.btnMinusVideos);
        btnPlusVideos = root.findViewById(R.id.btnPlusVideos);

        containerStepperPhotos = root.findViewById(R.id.containerStepperPhotos);
        tvMetaPhotosSubtitle = root.findViewById(R.id.tvMetaPhotosSubtitle);
        tvPhotosCount = root.findViewById(R.id.tvPhotosCount);
        btnMinusPhotos = root.findViewById(R.id.btnMinusPhotos);
        btnPlusPhotos = root.findViewById(R.id.btnPlusPhotos);

        etDailyObservations = root.findViewById(R.id.etDailyObservations);
        tvCharCounter = root.findViewById(R.id.tvCharCounter);

        btnSaveDailyDraft = root.findViewById(R.id.btnSaveDailyDraft);
        if (btnSaveDailyDraft != null) {
            btnSaveDailyDraft.setVisibility(View.GONE);
        }

        btnSaveDailyProduction = root.findViewById(R.id.btnSaveDailyProduction);
        ivSaveButtonIcon = root.findViewById(R.id.ivSaveButtonIcon);
        tvSaveButtonTitle = root.findViewById(R.id.tvSaveButtonTitle);
        tvSaveButtonSubtitle = root.findViewById(R.id.tvSaveButtonSubtitle);
    }

    private void scrollToTop() {
        if (rootScrollView instanceof NestedScrollView) {
            rootScrollView.post(() -> ((NestedScrollView) rootScrollView).smoothScrollTo(0, 0));
        } else if (rootScrollView != null) {
            rootScrollView.post(() -> rootScrollView.scrollTo(0, 0));
        }
    }

    private void jumpToNextPendingDay() {
        String currentDay = tvFormDayName.getText().toString();
        String nextMissing = KpiDesignerMemoryRepository.getInstance().getNextUnlockedDayNameAfter(currentDay);
        if (nextMissing != null) {
            jumpToDayInCurrentWeek(getCalendarDayFromName(nextMissing));
            scrollToTop();
            Toast.makeText(getContext(), "➔ Cargando " + nextMissing + " (pendiente de llenar)", Toast.LENGTH_SHORT).show();
        } else {
            scrollToTop();
            Toast.makeText(getContext(), "✓ Todos los días de la semana ya fueron enviados", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshRoleAndStyleConfig() {
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        boolean isPracticante = KpiDefaultSessionManager.getInstance().isPracticanteMode();
        activeProfileConfig = KpiRoleProfileConfig.forRole(activeRole, isPracticante);

        if (tvFormRoleHeaderTitle != null) {
            tvFormRoleHeaderTitle.setText(activeProfileConfig.getFormHeaderTitle());
        }
        if (tvFormRoleHeaderSubtitle != null) {
            String authName = KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String userPrefix = isPracticante ? "Practicante: " : "Colaborador: ";
            String displayName = (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+"))
                    ? userPrefix + authName
                    : activeProfileConfig.getFormHeaderSubtitle();
            tvFormRoleHeaderSubtitle.setText(displayName);
        }
        if (tvTitleDesigns != null) {
            tvTitleDesigns.setText(activeProfileConfig.getMetric1Title());
        }
        if (tvTitleVideos != null) {
            tvTitleVideos.setText(activeProfileConfig.getMetric2Title());
        }
        if (tvTitlePhotos != null) {
            tvTitlePhotos.setText(activeProfileConfig.getMetric3Title());
        }

        if (tvFormStyleBadge != null) {
            tvFormStyleBadge.setText(isPracticante ? "PRACTICANTE" : "COLABORADOR");
            tvFormStyleBadge.setBackgroundResource(isPracticante
                    ? R.drawable.kpi_bg_card_electric_accent
                    : R.drawable.kpi_bg_card_elegant_accent);
            tvFormStyleBadge.setTextColor(ContextCompat.getColor(requireContext(),
                    isPracticante ? R.color.kpi_info_text : R.color.kpi_warning_text));
        }
    }

    private void setupDayStripListeners() {
        btnDayLun.setOnClickListener(v -> onDayPillTapped(Calendar.MONDAY));
        btnDayMar.setOnClickListener(v -> onDayPillTapped(Calendar.TUESDAY));
        btnDayMie.setOnClickListener(v -> onDayPillTapped(Calendar.WEDNESDAY));
        btnDayJue.setOnClickListener(v -> onDayPillTapped(Calendar.THURSDAY));
        btnDayVie.setOnClickListener(v -> onDayPillTapped(Calendar.FRIDAY));
        btnDaySab.setOnClickListener(v -> onDayPillTapped(Calendar.SATURDAY));
    }

    private void onDayPillTapped(int targetDow) {
        int currentDow = currentSelectedCalendar.get(Calendar.DAY_OF_WEEK);
        if (targetDow == currentDow && isDayLocked) {
            jumpToNextPendingDay();
        } else {
            jumpToDayInCurrentWeek(targetDow);
            scrollToTop();
        }
    }

    private void jumpToDayInCurrentWeek(int targetDayOfWeek) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);

        int currentDow = cal.get(Calendar.DAY_OF_WEEK);
        int diffToMonday = (currentDow == Calendar.SUNDAY) ? 6 : (currentDow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diffToMonday);

        int targetOffset = (targetDayOfWeek == Calendar.SUNDAY) ? 5 : (targetDayOfWeek - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, targetOffset);

        loadCalendarDate(cal);
    }

    private void setupDatePicker() {
        btnSelectDateDesigner.setOnClickListener(v -> {
            int y = currentSelectedCalendar.get(Calendar.YEAR);
            int m = currentSelectedCalendar.get(Calendar.MONTH);
            int d = currentSelectedCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog picker = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, dayOfMonth);
                loadCalendarDate(sel);
                scrollToTop();
            }, y, m, d);
            picker.show();
        });

        btnResetDateToday.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            if (today.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                today.add(Calendar.DAY_OF_MONTH, -1);
            }
            loadCalendarDate(today);
            scrollToTop();
            Toast.makeText(getContext(), "Posicionado en el día actual", Toast.LENGTH_SHORT).show();
        });
    }

    public void loadCalendarDate(Calendar cal) {
        currentSelectedCalendar.setTime(cal.getTime());

        SimpleDateFormat dfShort = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat dfFull = new SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));

        String shortDate = dfShort.format(currentSelectedCalendar.getTime());
        String fullDate = capitalize(dfFull.format(currentSelectedCalendar.getTime()));
        tvFormSelectedDate.setText(fullDate);

        int dow = currentSelectedCalendar.get(Calendar.DAY_OF_WEEK);
        String dayName = getDayNameFromCalendar(dow);
        tvFormDayName.setText(dayName);

        updateDayStripStatus(dow);

        KpiDesignerMemoryRepository repo = KpiDesignerMemoryRepository.getInstance();
        KpiDesignerDayRecord existingRecord = repo.getRecordForDayOfWeek(dayName);
        if (existingRecord == null) {
            existingRecord = repo.getRecordForDate(shortDate);
        }

        if (existingRecord != null && existingRecord.isLocked()) {
            applyLockedState(existingRecord);
        } else {
            applyActiveState(dow, existingRecord);
        }
    }

    private void applyLockedState(KpiDesignerDayRecord record) {
        isDayLocked = false; // Permitir siempre agregar extras de producción

        containerDayLockBanner.setVisibility(View.VISIBLE);
        int totalComp = record.getTotalCompleted();
        int totalGoal = record.getTotalGoal();
        if (totalComp > totalGoal) {
            tvLockBannerTitle.setText("★ " + record.getDayOfWeek() + " con entregas extras (" + totalComp + "/" + totalGoal + ")");
            tvLockBannerSubtitle.setText("Producción extra activa (" + record.getCompliancePercentage() + "%). Puedes seguir sumando más entregas.");
        } else {
            tvLockBannerTitle.setText("✓ " + record.getDayOfWeek() + " registrado (" + totalComp + "/" + totalGoal + ")");
            tvLockBannerSubtitle.setText("Meta alcanzada. Cualquier entrega adicional sumará automáticamente como extra.");
        }
        containerDayLockBanner.setOnClickListener(null);

        tvFormModality.setText(record.getModality());
        tvFormDailyGoal.setText(record.getDailyGoalDescription());

        goalDesigns = record.getDesignsGoal();
        goalVideos = record.getVideosGoal();
        goalPhotos = record.getPhotosGoal();

        countDesigns = record.getDesignsCompleted();
        countVideos = record.getVideosCompleted();
        countPhotos = record.getPhotosCompleted();

        tvDesignsCount.setText(String.valueOf(countDesigns));
        tvVideosCount.setText(String.valueOf(countVideos));
        tvPhotosCount.setText(String.valueOf(countPhotos));

        // Steppers SIEMPRE habilitados para permitir registrar extras
        btnMinusDesigns.setEnabled(true);
        btnPlusDesigns.setEnabled(true);
        btnMinusVideos.setEnabled(true);
        btnPlusVideos.setEnabled(true);
        btnMinusPhotos.setEnabled(true);
        btnPlusPhotos.setEnabled(true);

        containerStepperDesigns.setAlpha(1.0f);
        containerStepperVideos.setAlpha(1.0f);
        containerStepperPhotos.setAlpha(1.0f);

        etDailyObservations.setText(record.getObservations() != null ? record.getObservations() : "");
        etDailyObservations.setAlpha(1.0f);

        if (btnSaveDailyDraft != null) {
            btnSaveDailyDraft.setVisibility(View.GONE);
        }
        if (btnUnlockRecord != null) {
            btnUnlockRecord.setVisibility(View.GONE);
        }

        btnSaveDailyProduction.setEnabled(true);
        btnSaveDailyProduction.setAlpha(1.0f);
        boolean isPracticante = activeProfileConfig != null && activeProfileConfig.isPracticanteMode();
        btnSaveDailyProduction.setBackgroundResource(isPracticante
                ? R.drawable.kpi_bg_btn_electric
                : R.drawable.kpi_bg_btn_elegant);
        tvSaveButtonTitle.setText("✓ Guardar / Actualizar Día (con extras)");
        tvSaveButtonTitle.setTextColor(Color.WHITE);
        tvSaveButtonSubtitle.setText("Toca para registrar o actualizar entregas extras");
        tvSaveButtonSubtitle.setTextColor(Color.parseColor("#E0E7FF"));
        ivSaveButtonIcon.setColorFilter(Color.WHITE);
    }

    private void applyActiveState(int dow, @Nullable KpiDesignerDayRecord savedDraft) {
        isDayLocked = false;
        if (btnUnlockRecord != null) {
            btnUnlockRecord.setVisibility(View.GONE);
        }
        if (btnSaveDailyDraft != null) {
            btnSaveDailyDraft.setVisibility(View.GONE);
        }
        containerDayLockBanner.setVisibility(View.GONE);
        containerDayLockBanner.setOnClickListener(null);

        etDailyObservations.setEnabled(true);
        etDailyObservations.setAlpha(1.0f);

        boolean isPracticante = activeProfileConfig != null && activeProfileConfig.isPracticanteMode();
        String dayLabel = getDayNameFromCalendar(dow);
        boolean isSaturday = (dow == Calendar.SATURDAY);

        btnSaveDailyProduction.setEnabled(true);
        btnSaveDailyProduction.setAlpha(1.0f);
        btnSaveDailyProduction.setBackgroundResource(isPracticante
                ? R.drawable.kpi_bg_btn_electric
                : R.drawable.kpi_bg_btn_elegant);
        tvSaveButtonTitle.setText(isSaturday
                ? "Enviar KPI Semanal (Sábado)"
                : "Enviar KPI de " + dayLabel);
        tvSaveButtonTitle.setTextColor(Color.WHITE);
        tvSaveButtonSubtitle.setText("Valida con tu biométrico y avanza automáticamente al siguiente día");
        tvSaveButtonSubtitle.setTextColor(Color.parseColor("#E0E7FF"));
        ivSaveButtonIcon.setColorFilter(Color.WHITE);

        if (dow == Calendar.SUNDAY) {
            tvFormModality.setText("Día de descanso");
            tvFormDailyGoal.setText("Sin meta programada");
            goalDesigns = 0;
            goalVideos = 0;
            goalPhotos = 0;
            countDesigns = 0;
            countVideos = 0;
            countPhotos = 0;
            etDailyObservations.setText("");
            setStepperControls(false, "No laborable", false, "No laborable", false, "No laborable");
        } else {
            KpiRoleProfileConfig.DayGoalSpec spec = activeProfileConfig.getDayGoalSpec(dow);
            tvFormModality.setText(spec.modality);
            tvFormDailyGoal.setText(spec.goalSummary);
            goalDesigns = spec.goal1;
            goalVideos = spec.goal2;
            goalPhotos = spec.goal3;

            if (savedDraft != null) {
                countDesigns = savedDraft.getDesignsCompleted();
                countVideos = savedDraft.getVideosCompleted();
                countPhotos = savedDraft.getPhotosCompleted();
                etDailyObservations.setText(savedDraft.getObservations() != null ? savedDraft.getObservations() : "");
            } else {
                countDesigns = spec.goal1;
                countVideos = spec.goal2;
                countPhotos = spec.goal3;
                etDailyObservations.setText("");
            }

            setStepperControls(spec.active1, spec.sub1, spec.active2, spec.sub2, spec.active3, spec.sub3);
        }

        tvDesignsCount.setText(String.valueOf(countDesigns));
        tvVideosCount.setText(String.valueOf(countVideos));
        tvPhotosCount.setText(String.valueOf(countPhotos));
    }

    private void updateDayStripStatus(int selectedDow) {
        KpiDesignerMemoryRepository repo = KpiDesignerMemoryRepository.getInstance();
        Calendar todayCal = Calendar.getInstance();
        int todayDow = todayCal.get(Calendar.DAY_OF_WEEK);

        updateSingleDayButton(btnDayLun, tvDayLabelLun, tvDayStatusLun, Calendar.MONDAY, selectedDow, todayDow, repo.isDayRecorded("LUNES"));
        updateSingleDayButton(btnDayMar, tvDayLabelMar, tvDayStatusMar, Calendar.TUESDAY, selectedDow, todayDow, repo.isDayRecorded("MARTES"));
        updateSingleDayButton(btnDayMie, tvDayLabelMie, tvDayStatusMie, Calendar.WEDNESDAY, selectedDow, todayDow, repo.isDayRecorded("MIÉRCOLES"));
        updateSingleDayButton(btnDayJue, tvDayLabelJue, tvDayStatusJue, Calendar.THURSDAY, selectedDow, todayDow, repo.isDayRecorded("JUEVES"));
        updateSingleDayButton(btnDayVie, tvDayLabelVie, tvDayStatusVie, Calendar.FRIDAY, selectedDow, todayDow, repo.isDayRecorded("VIERNES"));
        updateSingleDayButton(btnDaySab, tvDayLabelSab, tvDayStatusSab, Calendar.SATURDAY, selectedDow, todayDow, repo.isDayRecorded("SÁBADO"));
    }

    private void updateSingleDayButton(LinearLayout btn, TextView tvLabel, TextView tvStatus,
                                       int targetDow, int selectedDow, int todayDow, boolean isRecorded) {
        boolean isSelected = (targetDow == selectedDow);
        boolean isToday = (targetDow == todayDow);
        boolean isPracticante = activeProfileConfig != null && activeProfileConfig.isPracticanteMode();

        if (isSelected) {
            btn.setBackgroundResource(isPracticante
                    ? R.drawable.kpi_bg_btn_electric
                    : R.drawable.kpi_bg_btn_elegant);
            btn.setAlpha(1.0f);
            tvLabel.setTextColor(Color.WHITE);
            tvStatus.setTextColor(Color.parseColor("#E0E7FF"));
        } else if (isRecorded) {
            btn.setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
            btn.setAlpha(0.55f);
            tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
        } else {
            btn.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
            btn.setAlpha(1.0f);
            tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        }

        if (isRecorded) {
            tvStatus.setText("✓");
        } else if (isToday) {
            tvStatus.setText("HOY");
        } else {
            tvStatus.setText("•");
        }
    }

    private String getDayNameFromCalendar(int dow) {
        switch (dow) {
            case Calendar.MONDAY: return "LUNES";
            case Calendar.TUESDAY: return "MARTES";
            case Calendar.WEDNESDAY: return "MIÉRCOLES";
            case Calendar.THURSDAY: return "JUEVES";
            case Calendar.FRIDAY: return "VIERNES";
            case Calendar.SATURDAY: return "SÁBADO";
            default: return "DOMINGO";
        }
    }

    private int getCalendarDayFromName(String dayName) {
        if (dayName == null) return Calendar.MONDAY;
        switch (dayName.toUpperCase(Locale.ROOT)) {
            case "LUNES": return Calendar.MONDAY;
            case "MARTES": return Calendar.TUESDAY;
            case "MIÉRCOLES":
            case "MIERCOLES": return Calendar.WEDNESDAY;
            case "JUEVES": return Calendar.THURSDAY;
            case "VIERNES": return Calendar.FRIDAY;
            case "SÁBADO":
            case "SABADO": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    private void setStepperControls(boolean enableDesigns, String descDesigns,
                                    boolean enableVideos, String descVideos,
                                    boolean enablePhotos, String descPhotos) {
        tvMetaDesignsSubtitle.setText(descDesigns);
        btnMinusDesigns.setEnabled(true);
        btnPlusDesigns.setEnabled(true);
        tvDesignsCount.setEnabled(true);
        containerStepperDesigns.setAlpha(1.0f);

        tvMetaVideosSubtitle.setText(descVideos);
        btnMinusVideos.setEnabled(true);
        btnPlusVideos.setEnabled(true);
        tvVideosCount.setEnabled(true);
        containerStepperVideos.setAlpha(1.0f);

        tvMetaPhotosSubtitle.setText(descPhotos);
        btnMinusPhotos.setEnabled(true);
        btnPlusPhotos.setEnabled(true);
        tvPhotosCount.setEnabled(true);
        containerStepperPhotos.setAlpha(1.0f);
    }

    private void setupSteppers() {
        // Controles de incremento y decremento directo (sin bloquear por meta)
        btnMinusDesigns.setOnClickListener(v -> {
            if (countDesigns > 0) {
                countDesigns--;
                tvDesignsCount.setText(String.valueOf(countDesigns));
            }
        });
        btnPlusDesigns.setOnClickListener(v -> {
            countDesigns++;
            tvDesignsCount.setText(String.valueOf(countDesigns));
        });

        btnMinusVideos.setOnClickListener(v -> {
            if (countVideos > 0) {
                countVideos--;
                tvVideosCount.setText(String.valueOf(countVideos));
            }
        });
        btnPlusVideos.setOnClickListener(v -> {
            countVideos++;
            tvVideosCount.setText(String.valueOf(countVideos));
        });

        btnMinusPhotos.setOnClickListener(v -> {
            if (countPhotos > 0) {
                countPhotos--;
                tvPhotosCount.setText(String.valueOf(countPhotos));
            }
        });
        btnPlusPhotos.setOnClickListener(v -> {
            countPhotos++;
            tvPhotosCount.setText(String.valueOf(countPhotos));
        });

        // Apertura de teclado numérico táctil personalizado para ingreso directo
        View.OnClickListener openDesignsPad = v -> {
            KpiCustomKeypadDialog.showNumeric(
                    requireContext(),
                    tvTitleDesigns.getText().toString(),
                    tvMetaDesignsSubtitle.getText().toString(),
                    countDesigns,
                    goalDesigns,
                    value -> {
                        countDesigns = value;
                        tvDesignsCount.setText(String.valueOf(countDesigns));
                    }
            );
        };
        tvDesignsCount.setOnClickListener(openDesignsPad);
        containerStepperDesigns.setOnClickListener(openDesignsPad);

        View.OnClickListener openVideosPad = v -> {
            KpiCustomKeypadDialog.showNumeric(
                    requireContext(),
                    tvTitleVideos.getText().toString(),
                    tvMetaVideosSubtitle.getText().toString(),
                    countVideos,
                    goalVideos,
                    value -> {
                        countVideos = value;
                        tvVideosCount.setText(String.valueOf(countVideos));
                    }
            );
        };
        tvVideosCount.setOnClickListener(openVideosPad);
        containerStepperVideos.setOnClickListener(openVideosPad);

        View.OnClickListener openPhotosPad = v -> {
            KpiCustomKeypadDialog.showNumeric(
                    requireContext(),
                    tvTitlePhotos.getText().toString(),
                    tvMetaPhotosSubtitle.getText().toString(),
                    countPhotos,
                    goalPhotos,
                    value -> {
                        countPhotos = value;
                        tvPhotosCount.setText(String.valueOf(countPhotos));
                    }
            );
        };
        tvPhotosCount.setOnClickListener(openPhotosPad);
        containerStepperPhotos.setOnClickListener(openPhotosPad);
    }

    private void setupObservationsCounter() {
        etDailyObservations.setShowSoftInputOnFocus(false);
        etDailyObservations.setOnClickListener(v -> {
            KpiCustomKeypadDialog.showText(
                    requireContext(),
                    "Observaciones del día (" + activeProfileConfig.getRoleName() + ")",
                    "Máximo 200 caracteres",
                    etDailyObservations.getText().toString(),
                    text -> etDailyObservations.setText(text)
            );
        });
        etDailyObservations.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s != null ? s.length() : 0;
                tvCharCounter.setText(len + "/200");
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFinalSubmitButton() {
        btnSaveDailyProduction.setOnClickListener(v -> {
            String day = tvFormDayName.getText().toString();
            if ("DOMINGO".equalsIgnoreCase(day)) {
                Toast.makeText(getContext(), "El domingo no tiene meta programada.", Toast.LENGTH_SHORT).show();
                return;
            }

            requestBiometricAndSubmitDay();
        });
    }

    private void requestBiometricAndSubmitDay() {
        String roleName = activeProfileConfig != null ? activeProfileConfig.getRoleName() : "Diseñador Gráfico";
        String dayName = tvFormDayName.getText().toString();
        KpiBiometricHelper.authenticate(this, "Enviar KPI de " + dayName + " · " + roleName, new KpiBiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                finalizeDayAndAdvance();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                if (errorCode == -1 || errorMessage.contains("Ajustes de Seguridad") || errorMessage.contains("sensor biométrico")) {
                    showFallbackBiometricDialog(errorMessage);
                } else {
                    Toast.makeText(getContext(), "Verificación biométrica cancelada", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed() {
                Toast.makeText(getContext(), "Biometría no reconocida. Intenta nuevamente.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private KpiDesignerDayRecord saveCurrentDayInternal(boolean lockImmediately) {
        String shortDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentSelectedCalendar.getTime());
        String day = tvFormDayName.getText().toString();
        String modality = tvFormModality.getText().toString();
        String goal = tvFormDailyGoal.getText().toString();
        String obs = etDailyObservations.getText().toString().trim();

        KpiDesignerDayRecord record = new KpiDesignerDayRecord(
                shortDate, day, modality, goal,
                goalDesigns, goalVideos, goalPhotos,
                countDesigns, countVideos, countPhotos,
                obs, lockImmediately);
        record.setLocked(lockImmediately);

        KpiDesignerMemoryRepository.getInstance().saveDayRecord(record);

        String roleName = activeProfileConfig != null ? activeProfileConfig.getRoleName() : "Diseñador Gráfico";
        KpiCustomRepository.getInstance(requireContext()).saveRoleDailyEvaluation(
                roleName,
                shortDate,
                "KPI-" + roleName.substring(0, Math.min(3, roleName.length())).toUpperCase(Locale.ROOT) + "-01",
                activeProfileConfig != null ? activeProfileConfig.getFormHeaderTitle() + " (" + modality + ")" : modality,
                record.getTotalCompleted(),
                Math.max(1, goalDesigns + goalVideos + goalPhotos),
                record.getCompliancePercentage(),
                false,
                obs
        );
        return record;
    }

    private void finalizeDayAndAdvance() {
        String sentDayName = tvFormDayName.getText().toString();
        int sentDow = currentSelectedCalendar.get(Calendar.DAY_OF_WEEK);
        KpiDesignerDayRecord lastRecord = saveCurrentDayInternal(true);

        KpiDesignerMemoryRepository repo = KpiDesignerMemoryRepository.getInstance();
        boolean allSixLocked = repo.areAllSixDaysLocked();
        String nextMissing = repo.getNextUnlockedDayNameAfter(sentDayName);

        String roleName = activeProfileConfig != null ? activeProfileConfig.getRoleName() : "Diseñador Gráfico";
        boolean isPracticante = activeProfileConfig != null && activeProfileConfig.isPracticanteMode();

        if (allSixLocked) {
            repo.submitFinalWeekReport();
        }

        boolean isWeeklySaturdayOrComplete = (sentDow == Calendar.SATURDAY || allSixLocked);
        if (!isPracticante) {
            if (isWeeklySaturdayOrComplete) {
                KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());
            } else {
                KpiSoundHelper.playDailySuccess(requireContext());
            }
        }
// sound handled above
//

        if (isWeeklySaturdayOrComplete) {
            int compliance = lastRecord.getCompliancePercentage();
            int points = compliance >= 90 ? 20 : (compliance >= 80 ? 17 : (compliance >= 60 ? 14 : (compliance >= 40 ? 12 : 10)));
            loadCalendarDate(currentSelectedCalendar);
            scrollToTop();
            KpiPandaSuccessDialog.show(
                    requireContext(),
                    roleName,
                    isPracticante,
                    allSixLocked ? "KPI Semanal Completo" : sentDayName,
                    compliance,
                    points,
                    () -> {
                        if (nextMissing != null) {
                            jumpToDayInCurrentWeek(getCalendarDayFromName(nextMissing));
                            scrollToTop();
                        }
                    }
            );
        } else if (nextMissing != null) {
            jumpToDayInCurrentWeek(getCalendarDayFromName(nextMissing));
            scrollToTop();
            Toast.makeText(getContext(),
                    "✓ " + sentDayName + " enviado · Ahora en " + nextMissing,
                    Toast.LENGTH_SHORT).show();
        } else {
            loadCalendarDate(currentSelectedCalendar);
            scrollToTop();
        }
    }

    private void showFallbackBiometricDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Validación de Identidad")
                .setMessage(message + "\n\n¿Deseas confirmar tu identidad y enviar tu KPI?")
                .setPositiveButton("Confirmar y Enviar", (dialog, which) -> finalizeDayAndAdvance())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase(Locale.getDefault()) + str.substring(1);
    }
}
