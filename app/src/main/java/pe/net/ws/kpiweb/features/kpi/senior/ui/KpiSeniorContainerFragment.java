package pe.net.ws.kpiweb.features.kpi.senior.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.senior.data.KpiSeniorStorageManager;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorDayRecord;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorIndicator;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorWeekReport;
import pe.net.ws.kpiweb.features.kpi.senior.ui.view.KpiSeniorDonutProgressView;
import pe.net.ws.kpiweb.features.kpi.ui.KpiCustomKeypadDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiRoleSelectorFragment;

/**
 * Módulo Propio e Independiente del Diseñador Gráfico Senior (👔 Colaborador).
 * Conserva sus 2 Bloques Reales:
 * - Bloque 1: Mi Producción Diaria (Diseños, Videos, Sesiones, Otras, Descripción).
 * - Bloque 2: Supervisión de Mi Equipo de Diseño (Senior/Junior supervisados, % Equipo, Observaciones, Acciones).
 * - Pestaña 2: Reporte Semanal con KpiSeniorDonutProgressView + 6 Indicadores Ponderados (25/25/20/15/10/5%).
 * - Flujo semanal Lunes a Sábado: abre en el día actual real, permite retroceder a días previos pendientes,
 *   muestra con opacidad en solo lectura los días ya enviados, y avanza automáticamente con scroll hacia arriba.
 */
public class KpiSeniorContainerFragment extends Fragment {

    private static final String[] SHORT_LABELS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};

    private KpiSeniorStorageManager storageManager;
    private int selectedDayIndex = 0;
    private KpiSeniorDayRecord activeDay;

    private ScrollView formScroll;

    private final TextView[] dayPills = new TextView[6];
    private TextView tvActiveDayTitle;
    private TextView tvReadOnlyDayNotice;

    private LinearLayout block1Card;
    private LinearLayout block2FieldsContainer;
    private TextView btnSubmitSeniorAction;

    // Bloque 1: Producción propia
    private TextView tvDesignsVal, tvVideosVal, tvSessionsVal, tvOthersVal, tvProdDesc;

    // Bloque 2: Supervisión de equipo
    private TextView tvSupSeniorVal, tvSupJuniorVal, tvTeamPctVal, tvSupObs, tvSupActions;

    public static KpiSeniorContainerFragment newInstance() {
        return new KpiSeniorContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        KpiDefaultSessionManager.getInstance().setActiveRoleAndMode("Diseñador Gráfico Senior", false);
        storageManager = KpiSeniorStorageManager.getInstance(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_light_bg));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildHeader());

        FrameLayout content = new FrameLayout(requireContext());
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        formScroll = buildFormTab();
        content.addView(formScroll);
        root.addView(content);

        loadDayByIndex(storageManager.getTodayDayIndex());
        return root;
    }

    private void scrollFormToTop() {
        if (formScroll != null) {
            formScroll.post(() -> formScroll.smoothScrollTo(0, 0));
        }
    }

    private void jumpToNextPendingSeniorDay() {
        int nextIdx = storageManager.getNextUnlockedDayIndexAfter(selectedDayIndex);
        KpiSeniorWeekReport week = storageManager.getCurrentWeek();
        if (nextIdx != -1 && week != null && nextIdx < week.getDays().size()) {
            loadDayByIndex(nextIdx);
            scrollFormToTop();
            Toast.makeText(getContext(),
                    "➔ Cargando " + week.getDays().get(nextIdx).getDayName() + " (pendiente de llenar)",
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
        header.setBackgroundResource(R.drawable.kpi_bg_header_elegant);
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

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = dp(8);
        col.setLayoutParams(colLp);

        TextView tvSub = new TextView(requireContext());
        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        boolean isPrac = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String prefix = isPrac ? "PRACTICANTE: " : "COLABORADOR: ";
        tvSub.setText((authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+"))
                ? prefix + authName.toUpperCase(java.util.Locale.getDefault()) + " · SENIOR"
                : "COLABORADOR · DISEÑADOR GRÁFICO SENIOR");
        tvSub.setTextColor(Color.WHITE);
        tvSub.setTextSize(11f);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(tvSub);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("Producción Propia + Supervisión de Equipo");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(tvTitle);

        topRow.addView(col);

        TextView btnGoToReport = new TextView(requireContext());
        btnGoToReport.setText("Reporte");
        btnGoToReport.setTextSize(12f);
        btnGoToReport.setTypeface(Typeface.DEFAULT_BOLD);
        btnGoToReport.setTextColor(Color.WHITE);
        btnGoToReport.setGravity(Gravity.CENTER);
        btnGoToReport.setBackgroundResource(R.drawable.kpi_bg_translucent_pill);
        btnGoToReport.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnGoToReport.setOnClickListener(v -> openReportsHub());
        topRow.addView(btnGoToReport);

        header.addView(topRow);
        return header;
    }

    private void openReportsHub() {
        if (getActivity() != null) {
            String collabName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String cargoName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
            if (cargoName == null || cargoName.trim().isEmpty()) {
                cargoName = "Diseñador Gráfico Senior";
            }
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment.newInstanceExclusive(cargoName, collabName))
                    .addToBackStack(null)
                    .commit();
        }
    }

    private ScrollView buildFormTab() {
        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(28));

        // Selector de días Lun-Sáb
        LinearLayout dayCard = createCard();
        tvActiveDayTitle = new TextView(requireContext());
        tvActiveDayTitle.setTextSize(14f);
        tvActiveDayTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvActiveDayTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        dayCard.addView(tvActiveDayTitle);

        LinearLayout weekRow = new LinearLayout(requireContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wLp.topMargin = dp(10);
        weekRow.setLayoutParams(wLp);

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            TextView pill = new TextView(requireContext());
            pill.setText(SHORT_LABELS[i]);
            pill.setGravity(Gravity.CENTER);
            pill.setTextSize(12f);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            if (i < 5) pLp.rightMargin = dp(4);
            pill.setLayoutParams(pLp);
            pill.setOnClickListener(v -> {
                if (idx == selectedDayIndex && activeDay != null && activeDay.isLocked()) {
                    jumpToNextPendingSeniorDay();
                } else {
                    loadDayByIndex(idx);
                    scrollFormToTop();
                }
            });
            dayPills[i] = pill;
            weekRow.addView(pill);
        }
        dayCard.addView(weekRow);

        tvReadOnlyDayNotice = new TextView(requireContext());
        tvReadOnlyDayNotice.setTextSize(12f);
        tvReadOnlyDayNotice.setTypeface(Typeface.DEFAULT_BOLD);
        tvReadOnlyDayNotice.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
        tvReadOnlyDayNotice.setBackgroundResource(R.drawable.kpi_senior_bg_pill_green);
        tvReadOnlyDayNotice.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams roLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        roLp.topMargin = dp(10);
        tvReadOnlyDayNotice.setLayoutParams(roLp);
        tvReadOnlyDayNotice.setVisibility(View.GONE);
        tvReadOnlyDayNotice.setOnClickListener(v -> jumpToNextPendingSeniorDay());
        dayCard.addView(tvReadOnlyDayNotice);

        body.addView(dayCard);

        // BLOQUE 1: MI PRODUCCIÓN DIARIA
        block1Card = createCard();
        block1Card.addView(createTitle("BLOQUE 1 · MI PRODUCCIÓN DIARIA SENIOR"));

        tvDesignsVal = addNumericRow(block1Card, "Diseños de Campaña / Branding", "Meta sugerida: 2 piezas", 2, val -> {
            if (activeDay != null) activeDay.setDesignsCount(val);
        });
        tvVideosVal = addNumericRow(block1Card, "Edición / Dirección Audiovisual", "Meta sugerida: 2 videos", 2, val -> {
            if (activeDay != null) activeDay.setVideosCount(val);
        });
        tvSessionsVal = addNumericRow(block1Card, "Dirección en Sesión Fotográfica", "Sesiones dirigidas hoy", 1, val -> {
            if (activeDay != null) activeDay.setPhotoSessionsCount(val);
        });
        tvOthersVal = addNumericRow(block1Card, "Propuestas / Líneas Gráficas Nuevas", "Conceptos estratégicos", 1, val -> {
            if (activeDay != null) activeDay.setOtherActivitiesCount(val);
        });

        tvProdDesc = addTextBox(block1Card, "Descripción de producción propia del día...", "Producción Senior", text -> {
            if (activeDay != null) activeDay.setActivitiesDescription(text);
        });
        body.addView(block1Card);

        // BLOQUE 2: SUPERVISIÓN DE EQUIPO DE DISEÑO
        LinearLayout b2 = createCard();
        block2FieldsContainer = new LinearLayout(requireContext());
        block2FieldsContainer.setOrientation(LinearLayout.VERTICAL);
        block2FieldsContainer.addView(createTitle("BLOQUE 2 · SUPERVISIÓN DE EQUIPO DE DISEÑO"));

        tvSupSeniorVal = addNumericRow(block2FieldsContainer, "Diseñadores supervisados", "Colaboradores revisados", 2, val -> {
            if (activeDay != null) activeDay.setSupervisedSeniorCount(val);
        });
        tvSupJuniorVal = addNumericRow(block2FieldsContainer, "Practicantes Junior supervisados", "Practicantes guiados", 2, val -> {
            if (activeDay != null) activeDay.setSupervisedJuniorCount(val);
        });
        tvTeamPctVal = addNumericRow(block2FieldsContainer, "% Cumplimiento del Equipo (0-100)", "Porcentaje global del equipo hoy", 85, val -> {
            if (activeDay != null) activeDay.setTeamCompliancePercentage(Math.min(100, val));
        });

        tvSupObs = addTextBox(block2FieldsContainer, "Observaciones sobre el avance del equipo...", "Observaciones de Supervisión", text -> {
            if (activeDay != null) activeDay.setSupervisionObservations(text);
        });
        tvSupActions = addTextBox(block2FieldsContainer, "Acciones correctivas y feedback entregado...", "Feedback Aplicado", text -> {
            if (activeDay != null) activeDay.setSupervisionActions(text);
        });
        b2.addView(block2FieldsContainer);

        btnSubmitSeniorAction = new TextView(requireContext());
        btnSubmitSeniorAction.setText("Enviar KPI del Día");
        btnSubmitSeniorAction.setGravity(Gravity.CENTER);
        btnSubmitSeniorAction.setTextSize(14f);
        btnSubmitSeniorAction.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitSeniorAction.setTextColor(Color.WHITE);
        btnSubmitSeniorAction.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        fLp.topMargin = dp(14);
        btnSubmitSeniorAction.setLayoutParams(fLp);
        btnSubmitSeniorAction.setOnClickListener(v -> {
            if (activeDay != null && activeDay.isLocked()) {
                jumpToNextPendingSeniorDay();
            } else {
                submitSeniorDay();
            }
        });
        b2.addView(btnSubmitSeniorAction);

        body.addView(b2);
        sv.addView(body);
        return sv;
    }

    private interface IntConsumer {
        void accept(int val);
    }

    private interface StrConsumer {
        void accept(String val);
    }

    private TextView addNumericRow(LinearLayout parent, String title, String sub, int metaHint, IntConsumer onChange) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        row.setLayoutParams(lp);

        LinearLayout textCol = new LinearLayout(requireContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvT = new TextView(requireContext());
        tvT.setText(title);
        tvT.setTextSize(13f);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        tvT.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        textCol.addView(tvT);

        TextView tvS = new TextView(requireContext());
        tvS.setText(sub);
        tvS.setTextSize(11f);
        tvS.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        textCol.addView(tvS);
        row.addView(textCol);

        TextView box = new TextView(requireContext());
        box.setText("0");
        box.setGravity(Gravity.CENTER);
        box.setTextSize(16f);
        box.setTypeface(Typeface.DEFAULT_BOLD);
        box.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        box.setBackgroundResource(R.drawable.kpi_senior_bg_input);
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(68), dp(46)));

        View.OnClickListener openPad = v -> {
            if (activeDay != null && activeDay.isLocked()) {
                jumpToNextPendingSeniorDay();
                return;
            }
            int curr = 0;
            try { curr = Integer.parseInt(box.getText().toString().trim()); } catch (Exception ignored) {}
            KpiCustomKeypadDialog.showNumeric(requireContext(), title, sub, curr, metaHint, value -> {
                box.setText(String.valueOf(value));
                onChange.accept(value);
            });
        };
        box.setOnClickListener(openPad);
        row.setOnClickListener(openPad);

        row.addView(box);
        parent.addView(row);
        return box;
    }

    private TextView addTextBox(LinearLayout parent, String hint, String dialogTitle, StrConsumer onChange) {
        TextView box = new TextView(requireContext());
        box.setHint(hint);
        box.setTextSize(12f);
        box.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        box.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_gray_light));
        box.setBackgroundResource(R.drawable.kpi_junior_bg_input);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setMinimumHeight(dp(50));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        box.setLayoutParams(lp);
        box.setOnClickListener(v -> {
            if (activeDay != null && activeDay.isLocked()) {
                jumpToNextPendingSeniorDay();
                return;
            }
            KpiCustomKeypadDialog.showText(
                    requireContext(), dialogTitle, "Máximo 200 caracteres",
                    box.getText().toString(), text -> {
                        box.setText(text);
                        onChange.accept(text);
                    });
        });
        parent.addView(box);
        return box;
    }

    private void loadDayByIndex(int index) {
        KpiSeniorWeekReport week = storageManager.getCurrentWeek();
        if (week == null || week.getDays().isEmpty()) return;

        int safeIdx = Math.max(0, Math.min(index, week.getDays().size() - 1));
        selectedDayIndex = safeIdx;
        activeDay = week.getDays().get(safeIdx);
        boolean locked = activeDay.isLocked();

        for (int i = 0; i < 6 && i < week.getDays().size(); i++) {
            boolean sel = (i == safeIdx);
            boolean dayDone = week.getDays().get(i).isLocked();
            dayPills[i].setText(dayDone ? SHORT_LABELS[i] + " ✓" : SHORT_LABELS[i]);
            if (sel) {
                dayPills[i].setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                dayPills[i].setTextColor(Color.WHITE);
                dayPills[i].setAlpha(1.0f);
            } else if (dayDone) {
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_completed);
                dayPills[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_success_text));
                dayPills[i].setAlpha(0.55f);
            } else {
                dayPills[i].setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                dayPills[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
                dayPills[i].setAlpha(1.0f);
            }
        }

        tvActiveDayTitle.setText(activeDay.getDayName() + " · " + activeDay.getDate() + " (" + week.getWeekId() + ")");

        tvDesignsVal.setText(String.valueOf(activeDay.getDesignsCount()));
        tvVideosVal.setText(String.valueOf(activeDay.getVideosCount()));
        tvSessionsVal.setText(String.valueOf(activeDay.getPhotoSessionsCount()));
        tvOthersVal.setText(String.valueOf(activeDay.getOtherActivitiesCount()));
        tvProdDesc.setText(activeDay.getActivitiesDescription());

        tvSupSeniorVal.setText(String.valueOf(activeDay.getSupervisedSeniorCount()));
        tvSupJuniorVal.setText(String.valueOf(activeDay.getSupervisedJuniorCount()));
        tvTeamPctVal.setText(String.valueOf(activeDay.getTeamCompliancePercentage()));
        tvSupObs.setText(activeDay.getSupervisionObservations());
        tvSupActions.setText(activeDay.getSupervisionActions());

        // Opacidad y modo solo lectura si el día ya fue enviado
        float contentAlpha = locked ? 0.45f : 1.0f;
        block1Card.setAlpha(contentAlpha);
        block2FieldsContainer.setAlpha(contentAlpha);

        if (locked) {
            int nextIdx = storageManager.getNextUnlockedDayIndexAfter(safeIdx);
            tvReadOnlyDayNotice.setVisibility(View.VISIBLE);
            if (nextIdx != -1) {
                String nextName = week.getDays().get(nextIdx).getDayName();
                tvReadOnlyDayNotice.setText("✓ " + activeDay.getDayName() + " ya enviado (Solo lectura). Toca aquí para avanzar a " + nextName + ".");
                btnSubmitSeniorAction.setEnabled(true);
                btnSubmitSeniorAction.setAlpha(1.0f);
                btnSubmitSeniorAction.setText("Ir al siguiente día pendiente (" + nextName + ")");
            } else {
                tvReadOnlyDayNotice.setText("✓ Semana completa enviada y sellada (Solo visualización).");
                btnSubmitSeniorAction.setEnabled(false);
                btnSubmitSeniorAction.setAlpha(0.55f);
                btnSubmitSeniorAction.setText("KPI Semanal Completo (Solo lectura)");
            }
        } else {
            tvReadOnlyDayNotice.setVisibility(View.GONE);
            btnSubmitSeniorAction.setEnabled(true);
            btnSubmitSeniorAction.setAlpha(1.0f);
            btnSubmitSeniorAction.setText(safeIdx == 5
                    ? "Enviar KPI Semanal (Sábado)"
                    : "Enviar KPI de " + activeDay.getDayName());
        }
    }

    private void saveActiveDayLocked() {
        if (activeDay == null) return;
        storageManager.saveDayRecord(activeDay);
        if (getContext() != null) {
            KpiCustomRepository.getInstance(requireContext()).saveRoleDailyEvaluation(
                    "Diseñador Gráfico Senior",
                    activeDay.getDate(),
                    "KPI-SR-DISENO",
                    "Producción + Supervisión · Diseñador Senior",
                    activeDay.getTotalProduction(),
                    activeDay.getDailyGoal(),
                    activeDay.getDailyCompliancePercentage(),
                    false,
                    activeDay.getSupervisionObservations()
            );
        }
    }

    private void submitSeniorDay() {
        if (activeDay == null) return;
        String dayName = activeDay.getDayName();
        KpiBiometricHelper.authenticate(this, "Enviar KPI de " + dayName + " · Diseñador Senior", new KpiBiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                onSeniorDayBiometricConfirmed();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                if (errorCode == -1) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirmar Envío · Diseñador Senior")
                            .setMessage("¿Confirmas el envío de Producción y Supervisión de " + dayName + "?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Confirmar", (d, w) -> onSeniorDayBiometricConfirmed())
                            .show();
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

    private void onSeniorDayBiometricConfirmed() {
        final int justSentIdx = selectedDayIndex;
        final String sentDayName = activeDay != null ? activeDay.getDayName() : "Día";
        final int pct = activeDay != null ? activeDay.getDailyCompliancePercentage() : 100;

        saveActiveDayLocked();

        boolean allComplete = storageManager.areAllCurrentWeekDaysLocked();
        boolean isWeeklySaturdayOrComplete = (justSentIdx == 5 || allComplete);
        int nextIdx = storageManager.getNextUnlockedDayIndexAfter(justSentIdx);
        KpiSeniorWeekReport week = storageManager.getCurrentWeek();

        if (isWeeklySaturdayOrComplete) {
            KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());
        } else {
            KpiSoundHelper.playDailySuccess(requireContext());
        }

        if (isWeeklySaturdayOrComplete) {
        // sound handled above
            loadDayByIndex(justSentIdx);
            scrollFormToTop();
            KpiPandaSuccessDialog.show(requireContext(), "Diseñador Gráfico Senior", false,
                    allComplete ? "KPI Semanal Completo" : sentDayName,
                    pct, Math.max(10, pct), () -> {
                        if (nextIdx != -1) {
                            loadDayByIndex(nextIdx);
                            scrollFormToTop();
                        }
                    });
        } else if (nextIdx != -1 && week != null && nextIdx < week.getDays().size()) {
            String nextName = week.getDays().get(nextIdx).getDayName();
            loadDayByIndex(nextIdx);
            scrollFormToTop();
            Toast.makeText(getContext(),
                    "✓ " + sentDayName + " enviado · Ahora en " + nextName,
                    Toast.LENGTH_SHORT).show();
        } else {
            loadDayByIndex(justSentIdx);
            scrollFormToTop();
        }
    }

    private LinearLayout createCard() {
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

    private TextView createTitle(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        return tv;
    }

    private void navigateBack() {
        if (getActivity() == null) return;
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
