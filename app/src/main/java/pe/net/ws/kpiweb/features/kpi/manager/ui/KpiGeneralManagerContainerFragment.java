package pe.net.ws.kpiweb.features.kpi.manager.ui;

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
import pe.net.ws.kpiweb.features.kpi.manager.data.KpiGeneralManagerStorageManager;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerDayRecord;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerWeekReport;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiCustomKeypadDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment;
import pe.net.ws.kpiweb.features.kpi.ui.KpiRoleSelectorFragment;

/**
 * Módulo Ejecutivo del GERENTE GENERAL (👔 Colaborador / Conectado al Rol ADMIN).
 * Implementa el flujo estándar de Colaboradores (Lunes a Sábado, 1 jornada diaria):
 * - Bloque 1: Control Diario de Gestión (Propia)
 * - Bloque 2: Supervisión de las Áreas de la Empresa
 * - Rutina de Gestión (08:00 a.m. – 01:30 p.m.)
 * - Cierre Semanal (Resumen, Logros, Dificultades y Plan de Mejora)
 * - Validación biométrica con huella, bloqueo en modo solo lectura (0.45f) y sincronización
 *   instantánea con la hoja oficial del Centro de Reportes Semanales (SIGD).
 */
public class KpiGeneralManagerContainerFragment extends Fragment {

    private static final String[] SHORT_LABELS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB"};

    private KpiGeneralManagerStorageManager storageManager;
    private int selectedDayIndex = 0;
    private KpiGeneralManagerDayRecord activeDay;

    private ScrollView formScroll;
    private final TextView[] dayPills = new TextView[6];
    private TextView tvActiveDayTitle;
    private TextView tvReadOnlyDayNotice;

    private LinearLayout block1Card;
    private LinearLayout block2Card;
    private LinearLayout blockSummaryCard;

    // Bloque 1: Control diario de gestión (propia)
    private TextView tvB1IndicatorBadge;
    private TextView tvB1GoalBadge;
    private TextView tvB1TrackingVal;
    private TextView tvB1OwnPctVal;
    private TextView tvB1OwnObsVal;

    // Bloque 2: Supervisión de las áreas de la empresa
    private TextView tvB2SupervisedAreaVal;
    private TextView tvB2AreaPctVal;
    private TextView tvB2AreaObsVal;
    private TextView tvB2AreaActionsVal;

    // Bloque Resumen y Cierre Semanal (Secciones 3, 5, 6 y 7)
    private TextView tvWeeklyIndicatorsVal;
    private TextView tvWeeklyIssuesVal;
    private TextView tvAchievementsVal;
    private TextView tvDifficultiesVal;
    private TextView tvImprovementPlanVal;

    private TextView btnSubmitManagerDay;

    public static KpiGeneralManagerContainerFragment newInstance() {
        return new KpiGeneralManagerContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        KpiDefaultSessionManager.getInstance().setActiveRoleAndMode("Gerente General", false);
        storageManager = KpiGeneralManagerStorageManager.getInstance(requireContext());

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.kpi_light_bg));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildHeader());

        FrameLayout content = new FrameLayout(requireContext());
        content.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        formScroll = buildFormContent();
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

    private void jumpToNextPendingManagerDay() {
        int nextIdx = storageManager.getNextUnlockedDayIndexAfter(selectedDayIndex);
        KpiGeneralManagerWeekReport week = storageManager.getCurrentWeek();
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
        topRow.addView(btnBack);

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = dp(8);
        col.setLayoutParams(colLp);

        TextView tvSub = new TextView(requireContext());
        String displayName = resolveActiveManagerName().toUpperCase(Locale.getDefault());
        tvSub.setText("COLABORADOR: " + displayName + " · GERENTE GENERAL (ADMIN)");
        tvSub.setTextColor(Color.WHITE);
        tvSub.setTextSize(10.5f);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(tvSub);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("Control Diario de Gestión + Supervisión");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(16.5f);
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

    private String resolveActiveManagerName() {
        pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository repo =
                pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository.getInstance(requireContext());
        repo.syncWithSession();
        List<pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile> list =
                repo.getCollaboratorsForRole("Gerente General");
        if (list != null && !list.isEmpty()) {
            String n = list.get(0).getName();
            if (n != null && !n.trim().isEmpty()) {
                return n.trim();
            }
        }
        String authName = KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        if (authName != null && !authName.trim().isEmpty() && !authName.equals("Colaborador WebSite+")) {
            return authName.trim();
        }
        return "Colaborador (Gerente General)";
    }

    private void openReportsHub() {
        if (getActivity() != null) {
            String collabName = resolveActiveManagerName();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, KpiReportsHubFragment.newInstanceExclusive("Gerente General", collabName))
                    .addToBackStack(null)
                    .commit();
        }
    }

    private ScrollView buildFormContent() {
        ScrollView sv = new ScrollView(requireContext());
        sv.setFillViewport(true);

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(16), dp(16), dp(28));

        // 1. Selector de Días (Lunes a Sábado)
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
                    jumpToNextPendingManagerDay();
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
        tvReadOnlyDayNotice.setOnClickListener(v -> jumpToNextPendingManagerDay());
        dayCard.addView(tvReadOnlyDayNotice);

        body.addView(dayCard);

        // 2. BLOQUE 1 · CONTROL DIARIO DE GESTIÓN (PROPIA)
        block1Card = createCard();
        block1Card.addView(createTitle("1 · CONTROL DIARIO DE GESTIÓN (PROPIA)"));

        // Ficha informativa del indicador y meta del día
        LinearLayout goalBanner = new LinearLayout(requireContext());
        goalBanner.setOrientation(LinearLayout.VERTICAL);
        goalBanner.setBackgroundResource(R.drawable.kpi_bg_card_elegant_accent);
        goalBanner.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gbLp.topMargin = dp(8);
        goalBanner.setLayoutParams(gbLp);

        tvB1IndicatorBadge = new TextView(requireContext());
        tvB1IndicatorBadge.setTextSize(13.5f);
        tvB1IndicatorBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvB1IndicatorBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        goalBanner.addView(tvB1IndicatorBadge);

        tvB1GoalBadge = new TextView(requireContext());
        tvB1GoalBadge.setTextSize(12f);
        tvB1GoalBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        LinearLayout.LayoutParams goalSubLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        goalSubLp.topMargin = dp(2);
        tvB1GoalBadge.setLayoutParams(goalSubLp);
        goalBanner.addView(tvB1GoalBadge);
        block1Card.addView(goalBanner);

        tvB1TrackingVal = addLabeledTextBox(
                block1Card,
                "Seguimiento Realizado Hoy",
                "Ej. 3 indicadores / 2 asuntos / 4 áreas / Presupuesto y gastos",
                "Seguimiento Realizado",
                text -> {
                    if (activeDay != null) {
                        activeDay.setTrackingPerformed(text);
                        storageManager.saveToPrefs();
                    }
                }
        );

        tvB1OwnPctVal = addNumericRow(
                block1Card,
                "Resultado del Día (%)",
                "Porcentaje de cumplimiento de la meta propia (0-100%)",
                100,
                val -> {
                    if (activeDay != null) {
                        activeDay.setOwnCompliancePct(Math.min(100, val));
                        storageManager.saveToPrefs();
                    }
                }
        );

        tvB1OwnObsVal = addLabeledTextBox(
                block1Card,
                "Observaciones de Gestión Propia",
                "Ej. Revisión y alineación de metas...",
                "Observaciones (Gestión Propia)",
                text -> {
                    if (activeDay != null) {
                        activeDay.setOwnObservations(text);
                        storageManager.saveToPrefs();
                    }
                }
        );

        body.addView(block1Card);

        // 3. BLOQUE 2 · SUPERVISIÓN DE LAS ÁREAS DE LA EMPRESA
        block2Card = createCard();
        block2Card.addView(createTitle("2 · SUPERVISIÓN DE LAS ÁREAS DE LA EMPRESA"));

        tvB2SupervisedAreaVal = addLabeledTextBox(
                block2Card,
                "Área Supervisada del Día",
                "Ej. Marketing / Diseño, Producción Audiovisual, Comercial / Ventas...",
                "Área Supervisada",
                text -> {
                    if (activeDay != null) {
                        activeDay.setSupervisedArea(text);
                        storageManager.saveToPrefs();
                    }
                }
        );

        tvB2AreaPctVal = addNumericRow(
                block2Card,
                "Cumplimiento del Área (%)",
                "Porcentaje de cumplimiento del área supervisada hoy (0-100%)",
                100,
                val -> {
                    if (activeDay != null) {
                        activeDay.setAreaCompliancePct(Math.min(100, val));
                        storageManager.saveToPrefs();
                    }
                }
        );

        tvB2AreaObsVal = addLabeledTextBox(
                block2Card,
                "Principales Observaciones del Área",
                "Ej. Avance de campañas / Cumplimiento de cronograma...",
                "Principales Observaciones",
                text -> {
                    if (activeDay != null) {
                        activeDay.setAreaObservations(text);
                        storageManager.saveToPrefs();
                    }
                }
        );

        tvB2AreaActionsVal = addLabeledTextBox(
                block2Card,
                "Acciones Realizadas / Decisiones",
                "Ej. Ajuste de contenidos / Revisión y validación...",
                "Acciones Realizadas",
                text -> {
                    if (activeDay != null) {
                        activeDay.setAreaActions(text);
                        storageManager.saveToPrefs();
                    }
                }
        );

        body.addView(block2Card);

        // 4. RUTINA DE GESTIÓN (TURNO MAÑANA 08:00 a.m. – 01:30 p.m.)
        body.addView(buildRoutineCard());

        // 5. RESUMEN SEMANAL Y CIERRE EJECUTIVO (Secciones 3, 5, 6 y 7)
        blockSummaryCard = createCard();
        blockSummaryCard.addView(createTitle("3, 5, 6 Y 7 · RESUMEN Y CIERRE SEMANAL DE GERENCIA"));

        KpiGeneralManagerWeekReport currentWeek = storageManager.getCurrentWeek();

        tvWeeklyIndicatorsVal = addNumericRow(
                blockSummaryCard,
                "Indicadores prioritarios revisados (Semanal)",
                "Meta semanal propuesta: 12 – 18 indicadores",
                18,
                val -> {
                    KpiGeneralManagerWeekReport w = storageManager.getCurrentWeek();
                    if (w != null) {
                        w.setWeeklyIndicatorsReviewed(val);
                        storageManager.saveToPrefs();
                    }
                }
        );
        if (currentWeek != null) {
            tvWeeklyIndicatorsVal.setText(String.valueOf(currentWeek.getWeeklyIndicatorsReviewed()));
        }

        tvWeeklyIssuesVal = addNumericRow(
                blockSummaryCard,
                "Asuntos prioritarios resueltos (Semanal)",
                "Meta semanal propuesta: 6 – 12 asuntos",
                11,
                val -> {
                    KpiGeneralManagerWeekReport w = storageManager.getCurrentWeek();
                    if (w != null) {
                        w.setWeeklyIssuesResolved(val);
                        storageManager.saveToPrefs();
                    }
                }
        );
        if (currentWeek != null) {
            tvWeeklyIssuesVal.setText(String.valueOf(currentWeek.getWeeklyIssuesResolved()));
        }

        tvAchievementsVal = addLabeledTextBox(
                blockSummaryCard,
                "5 · Logros de la Semana",
                "• Cumplimiento del 100% de la gestión...",
                "Logros de la Semana",
                text -> {
                    KpiGeneralManagerWeekReport w = storageManager.getCurrentWeek();
                    if (w != null) {
                        w.setAchievements(text);
                        storageManager.saveToPrefs();
                    }
                }
        );
        if (currentWeek != null) {
            tvAchievementsVal.setText(currentWeek.getAchievements());
        }

        tvDifficultiesVal = addLabeledTextBox(
                blockSummaryCard,
                "6 · Dificultades / Incidencias",
                "• Menor alcance en leads...",
                "Dificultades / Incidencias",
                text -> {
                    KpiGeneralManagerWeekReport w = storageManager.getCurrentWeek();
                    if (w != null) {
                        w.setDifficulties(text);
                        storageManager.saveToPrefs();
                    }
                }
        );
        if (currentWeek != null) {
            tvDifficultiesVal.setText(currentWeek.getDifficulties());
        }

        tvImprovementPlanVal = addLabeledTextBox(
                blockSummaryCard,
                "7 · Plan de Mejora (Próxima Semana)",
                "• Fortalecer la generación de leads...",
                "Plan de Mejora",
                text -> {
                    KpiGeneralManagerWeekReport w = storageManager.getCurrentWeek();
                    if (w != null) {
                        w.setImprovementPlan(text);
                        storageManager.saveToPrefs();
                    }
                }
        );
        if (currentWeek != null) {
            tvImprovementPlanVal.setText(currentWeek.getImprovementPlan());
        }

        btnSubmitManagerDay = new TextView(requireContext());
        btnSubmitManagerDay.setText("Enviar KPI del Día");
        btnSubmitManagerDay.setGravity(Gravity.CENTER);
        btnSubmitManagerDay.setTextSize(14f);
        btnSubmitManagerDay.setTypeface(Typeface.DEFAULT_BOLD);
        btnSubmitManagerDay.setTextColor(Color.WHITE);
        btnSubmitManagerDay.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        fLp.topMargin = dp(16);
        btnSubmitManagerDay.setLayoutParams(fLp);
        btnSubmitManagerDay.setOnClickListener(v -> {
            if (activeDay != null && activeDay.isLocked()) {
                jumpToNextPendingManagerDay();
            } else {
                submitManagerDay();
            }
        });
        blockSummaryCard.addView(btnSubmitManagerDay);

        body.addView(blockSummaryCard);
        sv.addView(body);
        return sv;
    }

    private View buildRoutineCard() {
        LinearLayout card = createCard();
        card.addView(createTitle("RUTINA DE GESTIÓN · TURNO MAÑANA (08:00 a.m. – 01:30 p.m.)"));

        String[][] routineRows = {
                {"08:00 – 08:30", "Revisión de indicadores y agenda diaria"},
                {"08:30 – 09:30", "Reunión de coordinación con áreas"},
                {"09:30 – 10:30", "Seguimiento de proyectos y campañas"},
                {"10:30 – 12:00", "Revisión de recursos y presupuesto"},
                {"12:00 – 01:00", "Toma de decisiones y ajustes"},
                {"01:00 – 01:30", "Registro y cierre de avances del día"}
        };

        for (String[] r : routineRows) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
            row.setPadding(dp(10), dp(7), dp(10), dp(7));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            row.setLayoutParams(lp);

            TextView tvHour = new TextView(requireContext());
            tvHour.setText(r[0]);
            tvHour.setTextSize(11.5f);
            tvHour.setTypeface(Typeface.DEFAULT_BOLD);
            tvHour.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
            tvHour.setLayoutParams(new LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(tvHour);

            TextView tvAct = new TextView(requireContext());
            tvAct.setText(r[1]);
            tvAct.setTextSize(12f);
            tvAct.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
            tvAct.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tvAct);

            card.addView(row);
        }

        return card;
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
        box.setText(String.valueOf(metaHint));
        box.setGravity(Gravity.CENTER);
        box.setTextSize(16f);
        box.setTypeface(Typeface.DEFAULT_BOLD);
        box.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        box.setBackgroundResource(R.drawable.kpi_senior_bg_input);
        box.setLayoutParams(new LinearLayout.LayoutParams(dp(68), dp(46)));

        View.OnClickListener openPad = v -> {
            if (activeDay != null && activeDay.isLocked()) {
                jumpToNextPendingManagerDay();
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

    private TextView addLabeledTextBox(LinearLayout parent, String label, String hint, String dialogTitle, StrConsumer onChange) {
        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(12f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lblLp.topMargin = dp(10);
        tvLabel.setLayoutParams(lblLp);
        parent.addView(tvLabel);

        TextView box = new TextView(requireContext());
        box.setHint(hint);
        box.setTextSize(12.5f);
        box.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        box.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_gray_light));
        box.setBackgroundResource(R.drawable.kpi_junior_bg_input);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setMinimumHeight(dp(48));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        box.setLayoutParams(lp);
        box.setOnClickListener(v -> {
            if (activeDay != null && activeDay.isLocked()) {
                jumpToNextPendingManagerDay();
                return;
            }
            KpiCustomKeypadDialog.showText(
                    requireContext(), dialogTitle, "Ingresa el detalle de gestión",
                    box.getText().toString(), text -> {
                        box.setText(text);
                        onChange.accept(text);
                    });
        });
        parent.addView(box);
        return box;
    }

    private void loadDayByIndex(int index) {
        KpiGeneralManagerWeekReport week = storageManager.getCurrentWeek();
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

        tvB1IndicatorBadge.setText("Indicador Principal: " + activeDay.getMainIndicator());
        tvB1GoalBadge.setText("Meta del Día: " + activeDay.getDailyGoalText());

        String defaultTrackHint = KpiGeneralManagerStorageManager.DEFAULT_TRACKING_HINTS[safeIdx];
        tvB1TrackingVal.setHint("Ej. " + defaultTrackHint);
        tvB1TrackingVal.setText(activeDay.getTrackingPerformed());
        tvB1OwnPctVal.setText(String.valueOf(activeDay.getOwnCompliancePct()));
        tvB1OwnObsVal.setText(activeDay.getOwnObservations());

        tvB2SupervisedAreaVal.setText(activeDay.getSupervisedArea());
        tvB2AreaPctVal.setText(String.valueOf(activeDay.getAreaCompliancePct()));
        tvB2AreaObsVal.setText(activeDay.getAreaObservations());
        tvB2AreaActionsVal.setText(activeDay.getAreaActions());

        tvWeeklyIndicatorsVal.setText(String.valueOf(week.getWeeklyIndicatorsReviewed()));
        tvWeeklyIssuesVal.setText(String.valueOf(week.getWeeklyIssuesResolved()));
        tvAchievementsVal.setText(week.getAchievements());
        tvDifficultiesVal.setText(week.getDifficulties());
        tvImprovementPlanVal.setText(week.getImprovementPlan());

        float contentAlpha = locked ? 0.45f : 1.0f;
        block1Card.setAlpha(contentAlpha);
        block2Card.setAlpha(contentAlpha);

        if (locked) {
            int nextIdx = storageManager.getNextUnlockedDayIndexAfter(safeIdx);
            tvReadOnlyDayNotice.setVisibility(View.VISIBLE);
            if (nextIdx != -1) {
                String nextName = week.getDays().get(nextIdx).getDayName();
                tvReadOnlyDayNotice.setText("✓ " + activeDay.getDayName() + " ya enviado (Solo lectura). Toca aquí para avanzar a " + nextName + ".");
                btnSubmitManagerDay.setEnabled(true);
                btnSubmitManagerDay.setAlpha(1.0f);
                btnSubmitManagerDay.setText("Ir al siguiente día pendiente (" + nextName + ")");
            } else {
                tvReadOnlyDayNotice.setText("✓ Semana completa enviada y sellada (Solo visualización).");
                btnSubmitManagerDay.setEnabled(false);
                btnSubmitManagerDay.setAlpha(0.55f);
                btnSubmitManagerDay.setText("KPI Semanal Completo (Solo lectura)");
            }
        } else {
            tvReadOnlyDayNotice.setVisibility(View.GONE);
            btnSubmitManagerDay.setEnabled(true);
            btnSubmitManagerDay.setAlpha(1.0f);
            btnSubmitManagerDay.setText(safeIdx == 5
                    ? "Enviar KPI Semanal (Sábado)"
                    : "Enviar KPI de " + activeDay.getDayName());
        }
    }

    private void saveActiveDayLocked() {
        if (activeDay == null) return;
        storageManager.saveDayRecord(activeDay);

        if (getContext() != null) {
            KpiCustomRepository.getInstance(requireContext()).saveRoleDailyEvaluation(
                    "Gerente General",
                    activeDay.getDate(),
                    "KPI-GG-DIRECCION",
                    activeDay.getMainIndicator() + " · Supervisión: " + activeDay.getSupervisedArea(),
                    activeDay.getOwnCompliancePct(),
                    100,
                    (activeDay.getOwnCompliancePct() + activeDay.getAreaCompliancePct()) / 2.0,
                    false,
                    activeDay.getOwnObservations()
            );
        }
    }

    private void submitManagerDay() {
        if (activeDay == null) return;
        if (activeDay.getTrackingPerformed().trim().isEmpty()) {
            Toast.makeText(getContext(), "Describe el seguimiento realizado antes de enviar el KPI", Toast.LENGTH_LONG).show();
            return;
        }
        String dayName = activeDay.getDayName();
        KpiBiometricHelper.authenticate(this, "Enviar KPI de " + dayName + " · Gerente General", new KpiBiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                onManagerDayBiometricConfirmed();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                if (errorCode == -1) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirmar Envío · Gerente General")
                            .setMessage("¿Confirmas el envío de Gestión y Supervisión de " + dayName + "?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Confirmar", (d, w) -> onManagerDayBiometricConfirmed())
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

    private void onManagerDayBiometricConfirmed() {
        final int justSentIdx = selectedDayIndex;
        final String sentDayName = activeDay != null ? activeDay.getDayName() : "Día";
        final int pct = activeDay != null
                ? Math.round((activeDay.getOwnCompliancePct() + activeDay.getAreaCompliancePct()) / 2f)
                : 100;

        saveActiveDayLocked();

        boolean allComplete = storageManager.areAllCurrentWeekDaysLocked();
        boolean isWeeklySaturdayOrComplete = (justSentIdx == 5 || allComplete);
        int nextIdx = storageManager.getNextUnlockedDayIndexAfter(justSentIdx);
        KpiGeneralManagerWeekReport week = storageManager.getCurrentWeek();

        if (isWeeklySaturdayOrComplete) {
            KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());
            loadDayByIndex(justSentIdx);
            scrollFormToTop();
            KpiPandaSuccessDialog.show(requireContext(), "Gerente General", false,
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
            KpiSoundHelper.playDailySuccess(requireContext());
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
        if (getActivity() instanceof pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity) {
            getActivity().finish();
            return;
        }
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
