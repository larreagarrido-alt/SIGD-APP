package pe.net.ws.kpiweb.features.kpi.admin.ui;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import pe.net.ws.kpiweb.features.kpi.admin.ui.adapter.KpiDynamicFormAdapter;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.engine.KpiCalculationEngine;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;

/**
 * Formulario especializado por Cargo para nuevos Colaboradores o Practicantes creados.
 * Flujo semanal Lunes–Sábado:
 * - Abre posicionado por defecto en el día real actual.
 * - Permite retroceder libremente a llenar días anteriores pendientes.
 * - Los días ya enviados quedan en modo solo lectura con opacidad (0.45f).
 * - Al enviar un día avanza automáticamente al siguiente día pendiente con scroll hacia arriba.
 */
public class KpiDynamicCaptureFragment extends Fragment implements KpiDynamicFormAdapter.OnDynamicDataChangedListener {

    private static final String ARG_TARGET_ROLE = "arg_target_role";
    private static final String[] SHORT_DAYS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};
    private static final String[] FULL_DAYS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    private String targetRole = "Colaborador";
    private List<String> availableRoles;
    private double lastWeightedScore = 0.0;
    private int selectedDayIndex = 0;
    private boolean isCurrentDaySent = false;

    private ImageView btnDynBack, btnDynHistory;
    private TextView tvDynDateLabel, tvDynLockBadge, btnUnlockDynamicForTesting;
    private Spinner spDynSelectRole;
    private View cardFieldWorkContainer;
    private TextView tvDynLiveTotalPercent, tvDynLiveOverallLevel, tvDynLiveStatusDesc;
    private ProgressBar pbDynLiveProgress;
    private RecyclerView rvDynamicKpis;
    private Button btnSaveDynamicKpi;
    private com.google.android.material.switchmaterial.SwitchMaterial swFieldWorkJustified;
    private EditText etFieldWorkReason;
    private LinearLayout extraRoleHeaderHolder;
    private TextView tvReadOnlyDayBanner;

    private KpiDynamicFormAdapter adapter;

    public static KpiDynamicCaptureFragment newInstance(@Nullable String initialRole) {
        KpiDynamicCaptureFragment fragment = new KpiDynamicCaptureFragment();
        Bundle args = new Bundle();
        if (initialRole != null) {
            args.putString(ARG_TARGET_ROLE, initialRole);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getString(ARG_TARGET_ROLE) != null) {
            targetRole = getArguments().getString(ARG_TARGET_ROLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.kpi_fragment_dynamic_capture, container, false);
        selectedDayIndex = getTodayWeekDayIndex();
        initViews(root);
        injectLicenciadoQuickAddBar(root);
        setupRoleSpinner();
        setupRecyclerView();
        setupListeners();
        loadKpisForCurrentRole();
        return root;
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

    private String getDateStrForDay(int dayIndex0To5) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        return sdf.format(getWeekDayCalendar(dayIndex0To5).getTime());
    }

    private String getFullDateLabelForDay(int dayIndex0To5) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
        return FULL_DAYS[dayIndex0To5] + ", " + sdf.format(getWeekDayCalendar(dayIndex0To5).getTime());
    }

    private boolean isDaySent(int dayIndex0To5) {
        if (getContext() == null) return false;
        String dateStr = getDateStrForDay(dayIndex0To5);
        return !KpiCustomRepository.getInstance(requireContext())
                .getRecordEntriesByDateAndRole(dateStr, targetRole).isEmpty();
    }

    private int getNextUnsentDayIndexAfter(int fromIdx) {
        for (int i = fromIdx + 1; i < 6; i++) {
            if (!isDaySent(i)) return i;
        }
        for (int i = 0; i < fromIdx; i++) {
            if (!isDaySent(i)) return i;
        }
        return -1;
    }

    private void scrollListToTop() {
        if (rvDynamicKpis != null) {
            rvDynamicKpis.post(() -> rvDynamicKpis.smoothScrollToPosition(0));
        }
    }

    private void jumpToNextPendingDynamicDay() {
        int nextIdx = getNextUnsentDayIndexAfter(selectedDayIndex);
        if (nextIdx != -1) {
            selectedDayIndex = nextIdx;
            loadKpisForCurrentRole();
            scrollListToTop();
            Toast.makeText(getContext(),
                    "➔ Cargando " + FULL_DAYS[nextIdx] + " (pendiente de llenar)",
                    Toast.LENGTH_SHORT).show();
        } else {
            scrollListToTop();
            Toast.makeText(getContext(),
                    "✓ Todos los días de la semana ya fueron enviados",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews(View root) {
        btnDynBack = root.findViewById(R.id.btnDynBack);
        btnDynHistory = root.findViewById(R.id.btnDynHistory);
        tvDynDateLabel = root.findViewById(R.id.tvDynDateLabel);
        tvDynLockBadge = root.findViewById(R.id.tvDynLockBadge);
        btnUnlockDynamicForTesting = root.findViewById(R.id.btnUnlockDynamicForTesting);
        if (btnUnlockDynamicForTesting != null) {
            btnUnlockDynamicForTesting.setVisibility(View.GONE);
        }
        spDynSelectRole = root.findViewById(R.id.spDynSelectRole);
        cardFieldWorkContainer = root.findViewById(R.id.cardFieldWorkContainer);
        tvDynLiveTotalPercent = root.findViewById(R.id.tvDynLiveTotalPercent);
        tvDynLiveOverallLevel = root.findViewById(R.id.tvDynLiveOverallLevel);
        tvDynLiveStatusDesc = root.findViewById(R.id.tvDynLiveStatusDesc);
        pbDynLiveProgress = root.findViewById(R.id.pbDynLiveProgress);
        rvDynamicKpis = root.findViewById(R.id.rvDynamicKpis);
        btnSaveDynamicKpi = root.findViewById(R.id.btnSaveDynamicKpi);
        swFieldWorkJustified = root.findViewById(R.id.swFieldWorkJustified);
        etFieldWorkReason = root.findViewById(R.id.etFieldWorkReason);
    }

    private void injectLicenciadoQuickAddBar(View root) {
        if (!(root instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) root;

        extraRoleHeaderHolder = new LinearLayout(requireContext());
        extraRoleHeaderHolder.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(16);
        extraRoleHeaderHolder.setPadding(padH, dp(6), padH, dp(2));

        int insertIndex = Math.min(2, parent.getChildCount());
        parent.addView(extraRoleHeaderHolder, insertIndex);
    }

    private void refreshQuickAddBar() {
        if (extraRoleHeaderHolder == null || getContext() == null) return;
        extraRoleHeaderHolder.removeAllViews();

        boolean isPrac = KpiDefaultSessionManager.isRoleNaturallyPracticante(targetRole);

        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = dp(6);
        topRow.setLayoutParams(rLp);

        TextView tvModeBadge = new TextView(requireContext());
        tvModeBadge.setText(isPrac ? "PRACTICANTE" : "COLABORADOR");
        tvModeBadge.setTextSize(11f);
        tvModeBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvModeBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        tvModeBadge.setBackgroundResource(isPrac
                ? R.drawable.kpi_bg_card_electric_accent
                : R.drawable.kpi_bg_card_elegant_accent);
        tvModeBadge.setPadding(dp(10), dp(6), dp(10), dp(6));
        topRow.addView(tvModeBadge);

        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(spacer);

        TextView btnAddKpiToRole = new TextView(requireContext());
        btnAddKpiToRole.setText("➕ Agregar KPI a este cargo");
        btnAddKpiToRole.setTextSize(11f);
        btnAddKpiToRole.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddKpiToRole.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_on_primary));
        btnAddKpiToRole.setBackgroundResource(R.drawable.kpi_bg_gradient_btn);
        btnAddKpiToRole.setPadding(dp(12), dp(7), dp(12), dp(7));
        btnAddKpiToRole.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, KpiAdminEditKpiFragment.newInstance(null, targetRole))
                        .addToBackStack(null)
                        .commit();
            }
        });
        topRow.addView(btnAddKpiToRole);
        extraRoleHeaderHolder.addView(topRow);

        // Barra semanal Lunes a Sábado
        LinearLayout weekRow = new LinearLayout(requireContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wLp.bottomMargin = dp(4);
        weekRow.setLayoutParams(wLp);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            boolean sel = (i == selectedDayIndex);
            boolean sent = isDaySent(i);

            TextView pill = new TextView(requireContext());
            pill.setText(sent ? SHORT_DAYS[i] + " ✓" : SHORT_DAYS[i]);
            pill.setGravity(Gravity.CENTER);
            pill.setTextSize(11.5f);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
            if (i < 5) pLp.rightMargin = dp(4);
            pill.setLayoutParams(pLp);

            if (sel) {
                pill.setBackgroundResource(isPrac
                        ? R.drawable.kpi_bg_btn_electric
                        : R.drawable.kpi_bg_btn_elegant);
                pill.setTextColor(Color.WHITE);
                pill.setAlpha(1.0f);
            } else if (sent) {
                pill.setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
                pill.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
                pill.setAlpha(0.55f);
            } else {
                pill.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                pill.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
                pill.setAlpha(1.0f);
            }

            pill.setOnClickListener(v -> {
                if (idx == selectedDayIndex && sent) {
                    jumpToNextPendingDynamicDay();
                } else {
                    selectedDayIndex = idx;
                    loadKpisForCurrentRole();
                    scrollListToTop();
                }
            });
            weekRow.addView(pill);
        }
        extraRoleHeaderHolder.addView(weekRow);

        tvReadOnlyDayBanner = new TextView(requireContext());
        tvReadOnlyDayBanner.setTextSize(11.5f);
        tvReadOnlyDayBanner.setTypeface(Typeface.DEFAULT_BOLD);
        tvReadOnlyDayBanner.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
        tvReadOnlyDayBanner.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
        tvReadOnlyDayBanner.setPadding(dp(10), dp(6), dp(10), dp(6));
        tvReadOnlyDayBanner.setVisibility(View.GONE);
        tvReadOnlyDayBanner.setOnClickListener(v -> jumpToNextPendingDynamicDay());
        extraRoleHeaderHolder.addView(tvReadOnlyDayBanner);

        if (swFieldWorkJustified != null) {
            swFieldWorkJustified.setText(isPrac
                    ? "Comisión formativa / apoyo externo supervisado"
                    : "Cobertura en locación / sesión externa (Lugar y Hora)");
        }
        if (etFieldWorkReason != null) {
            etFieldWorkReason.setHint(isPrac
                    ? "Ej. Apoyo en sesión estudio central 08:00 AM - 01:30 PM..."
                    : "Ej. Locación Cliente / Restaurante Miraflores · 10:30 AM - 01:00 PM...");
        }
    }

    private void setupRoleSpinner() {
        if (getContext() == null) return;
        availableRoles = KpiCustomRepository.getInstance(requireContext()).getAllConfiguredRoles(requireContext());
        if (!availableRoles.contains(targetRole)) {
            availableRoles.add(0, targetRole);
        }

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, availableRoles);
        spDynSelectRole.setAdapter(roleAdapter);

        for (int i = 0; i < availableRoles.size(); i++) {
            if (availableRoles.get(i).equalsIgnoreCase(targetRole)) {
                spDynSelectRole.setSelection(i);
                break;
            }
        }

        spDynSelectRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetRole = availableRoles.get(position);
                selectedDayIndex = getTodayWeekDayIndex();
                loadKpisForCurrentRole();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        rvDynamicKpis.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new KpiDynamicFormAdapter(this);
        rvDynamicKpis.setAdapter(adapter);
    }

    private void loadKpisForCurrentRole() {
        if (getContext() == null) return;
        refreshQuickAddBar();

        tvDynDateLabel.setText(getFullDateLabelForDay(selectedDayIndex));

        KpiCustomRepository repo = KpiCustomRepository.getInstance(requireContext());
        List<KpiDefinition> list = repo.getActiveKpisForRole(targetRole);
        adapter.setKpiList(list);

        String dateStr = getDateStrForDay(selectedDayIndex);
        List<KpiRecordEntry> dayEntries = repo.getRecordEntriesByDateAndRole(dateStr, targetRole);
        isCurrentDaySent = !dayEntries.isEmpty();

        if (isCurrentDaySent) {
            Map<String, Double> savedValues = new LinkedHashMap<>();
            boolean isField = false;
            String fieldReason = "";
            for (KpiRecordEntry e : dayEntries) {
                savedValues.put(e.getKpiId(), e.getActualValue());
                if (e.isFieldWork()) {
                    isField = true;
                    fieldReason = e.getFieldWorkReason();
                }
            }
            adapter.setActualValues(savedValues);
            adapter.setReadOnly(true);

            rvDynamicKpis.setAlpha(0.45f);
            if (cardFieldWorkContainer != null) {
                cardFieldWorkContainer.setAlpha(0.45f);
            }

            if (swFieldWorkJustified != null) {
                swFieldWorkJustified.setChecked(isField);
                swFieldWorkJustified.setEnabled(false);
            }
            if (etFieldWorkReason != null) {
                etFieldWorkReason.setText(fieldReason);
                etFieldWorkReason.setVisibility(isField ? View.VISIBLE : View.GONE);
                etFieldWorkReason.setEnabled(false);
            }

            tvDynLockBadge.setText("✓ Solo lectura");
            tvDynLockBadge.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
            tvDynLockBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));

            int nextIdx = getNextUnsentDayIndexAfter(selectedDayIndex);
            if (tvReadOnlyDayBanner != null) {
                tvReadOnlyDayBanner.setVisibility(View.VISIBLE);
                tvReadOnlyDayBanner.setText(nextIdx != -1
                        ? "✓ " + FULL_DAYS[selectedDayIndex] + " ya enviado (Solo lectura). Toca para avanzar a " + FULL_DAYS[nextIdx] + "."
                        : "✓ Semana completa enviada y sellada (Solo visualización).");
            }

            if (nextIdx != -1) {
                btnSaveDynamicKpi.setEnabled(true);
                btnSaveDynamicKpi.setAlpha(1.0f);
                btnSaveDynamicKpi.setText("➔ Ir al siguiente día pendiente (" + FULL_DAYS[nextIdx] + ")");
                btnSaveDynamicKpi.setBackgroundTintList(null);
            } else {
                btnSaveDynamicKpi.setEnabled(false);
                btnSaveDynamicKpi.setAlpha(0.55f);
                btnSaveDynamicKpi.setText("✓ KPI Semanal Completo (Solo lectura)");
                btnSaveDynamicKpi.setBackgroundTintList(ColorStateList.valueOf(0xFF10B981));
            }
        } else {
            adapter.setReadOnly(false);
            rvDynamicKpis.setAlpha(1.0f);
            if (cardFieldWorkContainer != null) {
                cardFieldWorkContainer.setAlpha(1.0f);
            }
            if (tvReadOnlyDayBanner != null) {
                tvReadOnlyDayBanner.setVisibility(View.GONE);
            }

            if (swFieldWorkJustified != null) {
                swFieldWorkJustified.setChecked(false);
                swFieldWorkJustified.setEnabled(true);
            }
            if (etFieldWorkReason != null) {
                etFieldWorkReason.setText("");
                etFieldWorkReason.setVisibility(View.GONE);
                etFieldWorkReason.setEnabled(true);
            }

            tvDynLockBadge.setText("En edición");
            tvDynLockBadge.setBackgroundResource(R.drawable.kpi_senior_bg_pill_yellow);
            tvDynLockBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_warning_text));

            btnSaveDynamicKpi.setEnabled(true);
            btnSaveDynamicKpi.setAlpha(1.0f);
            btnSaveDynamicKpi.setText(selectedDayIndex == 5
                    ? "Enviar KPI Semanal (Sábado)"
                    : "Enviar KPI de " + FULL_DAYS[selectedDayIndex]);
            btnSaveDynamicKpi.setBackgroundTintList(null);
        }
    }

    private void setupListeners() {
        btnDynBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        if (btnDynHistory != null) {
            btnDynHistory.setOnClickListener(v -> openDynamicHistory());
        }

        if (swFieldWorkJustified != null) {
            swFieldWorkJustified.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (etFieldWorkReason != null && !isCurrentDaySent) {
                    etFieldWorkReason.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (isChecked) etFieldWorkReason.requestFocus();
                }
            });
        }

        btnSaveDynamicKpi.setOnClickListener(v -> {
            if (isCurrentDaySent) {
                jumpToNextPendingDynamicDay();
            } else {
                confirmSaveWithBiometrics();
            }
        });
    }

    private void openDynamicHistory() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main, KpiDynamicHistoryFragment.newInstance(targetRole))
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void confirmSaveWithBiometrics() {
        String dayName = FULL_DAYS[selectedDayIndex];
        KpiBiometricHelper.authenticate(this, "Confirmar KPI de " + dayName + " · " + targetRole, new KpiBiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                saveRecordsPermanently();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                if (errorCode == -1) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirmar Registro · " + targetRole)
                            .setMessage("¿Deseas confirmar y sellar tus KPIs de " + dayName + "?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Confirmar", (d, w) -> saveRecordsPermanently())
                            .show();
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

    private void saveRecordsPermanently() {
        final int justSentIdx = selectedDayIndex;
        final String sentDayName = FULL_DAYS[justSentIdx];
        String dateStr = getDateStrForDay(justSentIdx);

        Map<String, Double> actualValues = adapter.getActualValues();
        List<KpiDefinition> kpis = adapter.getKpiList();
        boolean isField = (swFieldWorkJustified != null && swFieldWorkJustified.isChecked());
        String fieldReason = (etFieldWorkReason != null) ? etFieldWorkReason.getText().toString().trim() : "";

        for (KpiDefinition kpi : kpis) {
            Double val = actualValues.get(kpi.getId());
            double actual = (val != null) ? val : 0.0;

            KpiCalculationEngine.EvaluationResult eval = KpiCalculationEngine.evaluateIndicator(
                    actual, kpi.getTargetValue(), kpi.getWeight(), kpi.getMeasurementType(), kpi.getPolarity());

            KpiRecordEntry entry = new KpiRecordEntry(
                    "REC-" + java.util.UUID.randomUUID(),
                    kpi.getId(),
                    kpi.getVersion(),
                    kpi.getName(),
                    targetRole,
                    "COLAB-ACTUAL",
                    dateStr,
                    actual,
                    kpi.getTargetValue(),
                    kpi.getWeight(),
                    eval.rawCompliancePercentage,
                    eval.weightedScore,
                    false
            );
            entry.setFieldWork(isField);
            entry.setFieldWorkReason(fieldReason);
            entry.setPeriod("Diaria");
            entry.setDeliveryStatus("Entregado");

            KpiCustomRepository.getInstance(requireContext()).saveRecordEntry(entry);
        }

        pe.net.ws.kpiweb.features.kpi.sync.KpiSyncManager.getInstance(requireContext())
                .syncPendingRecords(new pe.net.ws.kpiweb.features.kpi.sync.KpiSyncManager.SyncCallback() {
                    @Override public void onSyncSuccess(int count) {
                        if (isAdded()) Toast.makeText(requireContext(), count + " registros confirmados por el servidor.", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onSyncError(String message) {
                        if (isAdded()) Toast.makeText(requireContext(), "Guardado local. " + message, Toast.LENGTH_LONG).show();
                    }
                });

        int nextIdx = getNextUnsentDayIndexAfter(justSentIdx);
        boolean allSixSent = (nextIdx == -1);
        boolean isPrac = KpiDefaultSessionManager.isRoleNaturallyPracticante(targetRole);
        boolean isWeeklySaturdayOrComplete = (justSentIdx == 5 || allSixSent);

        if (!isPrac) {
            if (isWeeklySaturdayOrComplete) {
                KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());
            } else {
                KpiSoundHelper.playDailySuccess(requireContext());
            }
        }

        if (isWeeklySaturdayOrComplete) {
            loadKpisForCurrentRole();
            scrollListToTop();
            int scorePct = (int) Math.round(lastWeightedScore);
            KpiPandaSuccessDialog.show(requireContext(), targetRole, isPrac,
                    allSixSent ? "KPI semanal guardado localmente" : sentDayName,
                    scorePct, Math.max(10, scorePct), () -> {
                        if (nextIdx != -1) {
                            selectedDayIndex = nextIdx;
                            loadKpisForCurrentRole();
                            scrollListToTop();
                        }
                    });
        } else {
            selectedDayIndex = nextIdx;
            loadKpisForCurrentRole();
            scrollListToTop();
            Toast.makeText(getContext(),
                    "✓ " + sentDayName + " guardado localmente · Ahora en " + FULL_DAYS[nextIdx],
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDataChanged(double totalWeightedScore, String overallLevel) {
        lastWeightedScore = totalWeightedScore;
        tvDynLiveTotalPercent.setText(String.format(Locale.getDefault(), "%.1f%%", totalWeightedScore));
        pbDynLiveProgress.setProgress((int) Math.min(100.0, Math.round(totalWeightedScore)));

        tvDynLiveOverallLevel.setText(overallLevel);
        tvDynLiveOverallLevel.setTextColor(KpiCalculationEngine.getLevelColor(overallLevel));
        tvDynLiveStatusDesc.setText(KpiCalculationEngine.getLevelDescription(overallLevel));
    }

    private int dp(int val) {
        return Math.round(val * requireContext().getResources().getDisplayMetrics().density);
    }
}
