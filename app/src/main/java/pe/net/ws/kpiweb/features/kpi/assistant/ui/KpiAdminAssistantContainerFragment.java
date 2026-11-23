package pe.net.ws.kpiweb.features.kpi.assistant.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.assistant.data.KpiAdminAssistantStorageManager;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantDayRecord;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantWeekReport;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiBiometricHelper;
import pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiPandaSuccessDialog;
import pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment;

/**
 * Formulario ejecutivo diario para el cargo de ASISTENTE ADMINISTRATIVO (SIGD 2026).
 * Conectado dinámicamente con los colaboradores reales de la base de datos:
 * - Selección de día de Lunes a Sábado + Turno Mañana / Tarde.
 * - Bloque 1: Control Diario de Producción (Gestiones Realizadas, Documentos Registrados,
 *   Coordinaciones Internas, Total Actividades en vivo, % Cumplimiento y Observaciones).
 * - Bloque 2 & 3: Resumen Semanal en vivo sobre la meta de 72 actividades (Nivel KPI:
 *   Excelente / Muy Bueno / Bueno / Regular / Deficiente) + Checklist de Acciones del cargo.
 * - Bloque 5: Observaciones Generales de cierre semanal.
 * - Confirmación biométrica con bloqueo de solo lectura (0.45f) y sincronización inmediata con Reportes SIGD.
 */
public class KpiAdminAssistantContainerFragment extends Fragment {

    private KpiAdminAssistantStorageManager storage;
    private int selectedDayIndex = 0;
    private boolean suppressWatchers = false;

    private final TextView[] dayTabPills = new TextView[6];

    private TextView tvHeaderCollab;
    private TextView tvHeaderWeekRange;
    private TextView tvDayStatusBanner;

    private TextView btnShiftMorning;
    private TextView btnShiftAfternoon;
    private boolean isMorningShift = true;

    // Bloque 1: Control Diario de Producción
    private TextView tvB1DayTitle;
    private TextView tvB1ActivityName;
    private TextView tvB1GoalText;
    private EditText etB1Gestiones;
    private EditText etB1Documentos;
    private EditText etB1Coordinaciones;
    private TextView tvB1LiveTotalAct;
    private TextView tvB1LiveComplianceBadge;
    private EditText etB1Observations;

    // Aviso de Cierre Semanal (solo visible de Lunes a Viernes)
    private LinearLayout cardSaturdayNoticeBanner;

    // Bloque 2 & 3: Resumen Semanal en Vivo y Acciones (solo visible los Sábados)
    private LinearLayout cardBlock2And3;
    private TextView tvWeeklySummaryTotals;
    private TextView tvWeeklyKpiLevelBadge;
    private final CheckBox[] cbActions = new CheckBox[6];
    private CheckBox cbOtherActivities;

    // Bloque 5: Observaciones Generales (solo visible los Sábados)
    private LinearLayout cardBlock5;
    private EditText etGeneralObservations;

    private TextView btnSaveDayRecord;

    public static KpiAdminAssistantContainerFragment newInstance() {
        return new KpiAdminAssistantContainerFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        storage = KpiAdminAssistantStorageManager.getInstance(ctx);
        selectedDayIndex = storage.getTodayDayIndex();

        ScrollView rootScroll = new ScrollView(ctx);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(Color.parseColor("#F8FAFC"));

        LinearLayout mainCol = new LinearLayout(ctx);
        mainCol.setOrientation(LinearLayout.VERTICAL);
        mainCol.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rootScroll.addView(mainCol);

        // Header de borde a borde (ancho 100%, sin márgenes laterales)
        buildHeaderCard(ctx, mainCol);

        // Contenedor de contenido con márgenes cómodos (16dp)
        LinearLayout contentContainer = new LinearLayout(ctx);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        contentContainer.setPadding(pad, dp(14), pad, dp(32));
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainCol.addView(contentContainer);

        buildDaySelectorStrip(ctx, contentContainer);
        buildBlock1ProductionCard(ctx, contentContainer);
        buildSaturdayNoticeCard(ctx, contentContainer);
        buildBlock2And3SummaryActionsCard(ctx, contentContainer);
        buildBlock5ObservationsCard(ctx, contentContainer);
        buildActionButtons(ctx, contentContainer);

        loadDayIntoForm(selectedDayIndex);
        return rootScroll;
    }

    private String resolveActiveCollaboratorName(Context ctx) {
        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(ctx);
        repo.syncWithSession();
        List<KpiCollaboratorProfile> list = repo.getCollaboratorsForRole("Asistente Administrativo");
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
        return "Colaborador (Asistente Administrativo)";
    }

    private void buildHeaderCard(Context ctx, LinearLayout parent) {
        LinearLayout headerCard = new LinearLayout(ctx);
        headerCard.setOrientation(LinearLayout.VERTICAL);

        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#4C1D95"), Color.parseColor("#6D28D9")}
        );
        float r = dp(24);
        headerBg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        headerCard.setBackground(headerBg);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            headerCard.setElevation(dp(4));
        }

        int pSide = dp(18);
        headerCard.setPadding(pSide, dp(22), pSide, dp(20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerCard.setLayoutParams(lp);

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView btnBack = new TextView(ctx);
        btnBack.setText("← Volver");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTextSize(13f);
        btnBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnBack.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(Color.parseColor("#33FFFFFF"));
        backBg.setCornerRadius(dp(20));
        btnBack.setBackground(backBg);
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
        if (!(requireActivity() instanceof pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity)) topRow.addView(btnBack);

        View spacer = new View(ctx);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topRow.addView(spacer);

        TextView btnOpenReport = new TextView(ctx);
        btnOpenReport.setText("📊 Reporte");
        btnOpenReport.setTextColor(Color.WHITE);
        btnOpenReport.setTextSize(13f);
        btnOpenReport.setTypeface(Typeface.DEFAULT_BOLD);
        btnOpenReport.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable repBg = new GradientDrawable();
        repBg.setColor(Color.parseColor("#40FFFFFF"));
        repBg.setStroke(dp(1), Color.parseColor("#80FFFFFF"));
        repBg.setCornerRadius(dp(20));
        btnOpenReport.setBackground(repBg);
        btnOpenReport.setOnClickListener(v -> openOfficialReportSheet());
        topRow.addView(btnOpenReport);

        headerCard.addView(topRow);

        TextView tvBadge = new TextView(ctx);
        tvBadge.setText("ÁREA ADMINISTRACIÓN · SOPORTE OPERATIVO SIGD");
        tvBadge.setTextColor(Color.parseColor("#DDD6FE"));
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.topMargin = dp(14);
        tvBadge.setLayoutParams(badgeLp);
        headerCard.addView(tvBadge);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("Asistente Administrativo");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(2);
        tvTitle.setLayoutParams(titleLp);
        headerCard.addView(tvTitle);

        TextView tvSub = new TextView(ctx);
        tvSub.setText(">>> ORGANIZA • REGISTRA • COORDINA • APOYA • REPORTA");
        tvSub.setTextColor(Color.parseColor("#E0E7FF"));
        tvSub.setTextSize(11f);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(2);
        tvSub.setLayoutParams(subLp);
        headerCard.addView(tvSub);

        String collabName = resolveActiveCollaboratorName(ctx);
        tvHeaderCollab = new TextView(ctx);
        tvHeaderCollab.setText("👤 Colaborador: " + collabName.toUpperCase(Locale.getDefault()));
        tvHeaderCollab.setTextColor(Color.WHITE);
        tvHeaderCollab.setTextSize(12.5f);
        tvHeaderCollab.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colLp.topMargin = dp(10);
        tvHeaderCollab.setLayoutParams(colLp);
        headerCard.addView(tvHeaderCollab);

        KpiAdminAssistantWeekReport currentWeek = storage.getCurrentWeek();
        tvHeaderWeekRange = new TextView(ctx);
        String rangeText = currentWeek != null
                ? (currentWeek.getWeekId() + " · " + currentWeek.getDateRange())
                : "Semana Activa";
        tvHeaderWeekRange.setText("Jefe: ADMINISTRACIÓN GENERAL  •  " + rangeText);
        tvHeaderWeekRange.setTextColor(Color.parseColor("#C7D2FE"));
        tvHeaderWeekRange.setTextSize(11.5f);
        LinearLayout.LayoutParams rangeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rangeLp.topMargin = dp(3);
        tvHeaderWeekRange.setLayoutParams(rangeLp);
        headerCard.addView(tvHeaderWeekRange);

        // Selector de Turno (Mañana / Tarde)
        LinearLayout shiftRow = new LinearLayout(ctx);
        shiftRow.setOrientation(LinearLayout.HORIZONTAL);
        shiftRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams shiftLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shiftLp.topMargin = dp(12);
        shiftRow.setLayoutParams(shiftLp);

        TextView tvShiftLabel = new TextView(ctx);
        tvShiftLabel.setText("Turno:");
        tvShiftLabel.setTextColor(Color.WHITE);
        tvShiftLabel.setTextSize(12f);
        tvShiftLabel.setTypeface(Typeface.DEFAULT_BOLD);
        shiftRow.addView(tvShiftLabel);

        btnShiftMorning = new TextView(ctx);
        btnShiftMorning.setText("☑ Turno mañana");
        btnShiftMorning.setTextSize(11.5f);
        btnShiftMorning.setTypeface(Typeface.DEFAULT_BOLD);
        btnShiftMorning.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.leftMargin = dp(8);
        btnShiftMorning.setLayoutParams(mLp);
        btnShiftMorning.setOnClickListener(v -> {
            isMorningShift = true;
            updateShiftButtonsUI();
        });
        shiftRow.addView(btnShiftMorning);

        btnShiftAfternoon = new TextView(ctx);
        btnShiftAfternoon.setText("☐ Turno tarde");
        btnShiftAfternoon.setTextSize(11.5f);
        btnShiftAfternoon.setTypeface(Typeface.DEFAULT_BOLD);
        btnShiftAfternoon.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        aLp.leftMargin = dp(8);
        btnShiftAfternoon.setLayoutParams(aLp);
        btnShiftAfternoon.setOnClickListener(v -> {
            isMorningShift = false;
            updateShiftButtonsUI();
        });
        shiftRow.addView(btnShiftAfternoon);

        headerCard.addView(shiftRow);
        parent.addView(headerCard);
    }

    private void updateShiftButtonsUI() {
        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setColor(Color.WHITE);
        activeBg.setCornerRadius(dp(14));

        GradientDrawable inactiveBg = new GradientDrawable();
        inactiveBg.setColor(Color.parseColor("#26FFFFFF"));
        inactiveBg.setCornerRadius(dp(14));

        if (isMorningShift) {
            btnShiftMorning.setText("☑ Turno mañana");
            btnShiftMorning.setTextColor(Color.parseColor("#1E1B4B"));
            btnShiftMorning.setBackground(activeBg);

            btnShiftAfternoon.setText("☐ Turno tarde");
            btnShiftAfternoon.setTextColor(Color.WHITE);
            btnShiftAfternoon.setBackground(inactiveBg);
        } else {
            btnShiftMorning.setText("☐ Turno mañana");
            btnShiftMorning.setTextColor(Color.WHITE);
            btnShiftMorning.setBackground(inactiveBg);

            btnShiftAfternoon.setText("☑ Turno tarde");
            btnShiftAfternoon.setTextColor(Color.parseColor("#1E1B4B"));
            btnShiftAfternoon.setBackground(activeBg);
        }
    }

    private void buildDaySelectorStrip(Context ctx, LinearLayout parent) {
        LinearLayout stripCard = createSectionCard(ctx);

        TextView tvSelectLabel = new TextView(ctx);
        tvSelectLabel.setText("SELECCIONA EL DÍA DE PRODUCCIÓN (LUNES A SÁBADO)");
        tvSelectLabel.setTextSize(11f);
        tvSelectLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvSelectLabel.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_secondary));
        stripCard.addView(tvSelectLabel);

        LinearLayout pillsRow = new LinearLayout(ctx);
        pillsRow.setOrientation(LinearLayout.HORIZONTAL);
        pillsRow.setWeightSum(6f);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(8);
        pillsRow.setLayoutParams(rowLp);

        for (int i = 0; i < 6; i++) {
            final int dayIdx = i;
            TextView pill = new TextView(ctx);
            pill.setGravity(Gravity.CENTER);
            pill.setTextSize(11.5f);
            pill.setTypeface(Typeface.DEFAULT_BOLD);
            pill.setPadding(dp(4), dp(10), dp(4), dp(10));
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < 5) pLp.rightMargin = dp(5);
            pill.setLayoutParams(pLp);
            pill.setOnClickListener(v -> loadDayIntoForm(dayIdx));
            dayTabPills[i] = pill;
            pillsRow.addView(pill);
        }
        stripCard.addView(pillsRow);

        tvDayStatusBanner = new TextView(ctx);
        tvDayStatusBanner.setTextSize(12f);
        tvDayStatusBanner.setTypeface(Typeface.DEFAULT_BOLD);
        tvDayStatusBanner.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams banLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        banLp.topMargin = dp(10);
        tvDayStatusBanner.setLayoutParams(banLp);
        stripCard.addView(tvDayStatusBanner);

        parent.addView(stripCard);
    }

    private void buildBlock1ProductionCard(Context ctx, LinearLayout parent) {
        LinearLayout card = createSectionCard(ctx);

        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = createNumberBadge(ctx, "1");
        headerRow.addView(badge);

        tvB1DayTitle = new TextView(ctx);
        tvB1DayTitle.setText("CONTROL DIARIO DE PRODUCCIÓN");
        tvB1DayTitle.setTextSize(14f);
        tvB1DayTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvB1DayTitle.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_primary));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.leftMargin = dp(10);
        tvB1DayTitle.setLayoutParams(tLp);
        headerRow.addView(tvB1DayTitle);

        tvB1LiveComplianceBadge = new TextView(ctx);
        tvB1LiveComplianceBadge.setText("0%");
        tvB1LiveComplianceBadge.setTextSize(12f);
        tvB1LiveComplianceBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvB1LiveComplianceBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        headerRow.addView(tvB1LiveComplianceBadge);

        card.addView(headerRow);

        // Info box: Modalidad / Actividad Principal y Meta Diaria
        LinearLayout infoBox = new LinearLayout(ctx);
        infoBox.setOrientation(LinearLayout.VERTICAL);
        infoBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#EEF2FF"));
        infoBg.setCornerRadius(dp(10));
        infoBg.setStroke(dp(1), Color.parseColor("#C7D2FE"));
        infoBox.setBackground(infoBg);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.topMargin = dp(10);
        infoBox.setLayoutParams(infoLp);

        tvB1ActivityName = new TextView(ctx);
        tvB1ActivityName.setTextSize(13.5f);
        tvB1ActivityName.setTypeface(Typeface.DEFAULT_BOLD);
        tvB1ActivityName.setTextColor(Color.parseColor("#1E1B4B"));
        infoBox.addView(tvB1ActivityName);

        tvB1GoalText = new TextView(ctx);
        tvB1GoalText.setTextSize(12f);
        tvB1GoalText.setTextColor(Color.parseColor("#3730A3"));
        infoBox.addView(tvB1GoalText);

        card.addView(infoBox);

        // Disposición VERTICAL de las 3 métricas: nombres completos sin abreviar y su cuadrito al costado
        LinearLayout metricsContainer = new LinearLayout(ctx);
        metricsContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mcLp.topMargin = dp(12);
        metricsContainer.setLayoutParams(mcLp);

        // Fila 1: Gestiones realizadas
        etB1Gestiones = createNumericBox(ctx, "6");
        View rowGest = createMetricRow(ctx,
                "Gestiones realizadas",
                "Seguimiento a requerimientos, compras y servicios",
                etB1Gestiones
        );
        metricsContainer.addView(rowGest);

        // Fila 2: Documentos registrados
        etB1Documentos = createNumericBox(ctx, "3");
        View rowDoc = createMetricRow(ctx,
                "Documentos registrados",
                "Control, archivo y actualización de formatos",
                etB1Documentos
        );
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = dp(8);
        rowDoc.setLayoutParams(dLp);
        metricsContainer.addView(rowDoc);

        // Fila 3: Coordinaciones internas
        etB1Coordinaciones = createNumericBox(ctx, "2");
        View rowCoord = createMetricRow(ctx,
                "Coordinaciones internas",
                "Reuniones, apoyo logístico y enlace entre áreas",
                etB1Coordinaciones
        );
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = dp(8);
        rowCoord.setLayoutParams(cLp);
        metricsContainer.addView(rowCoord);

        card.addView(metricsContainer);

        // Barra de Total Actividades del Día debajo de las 3 cajitas
        tvB1LiveTotalAct = new TextView(ctx);
        tvB1LiveTotalAct.setText("Total Actividades del Día: 0  (Gestiones: 0 + Documentos: 0 + Coordinaciones: 0)");
        tvB1LiveTotalAct.setTextSize(12.5f);
        tvB1LiveTotalAct.setTypeface(Typeface.DEFAULT_BOLD);
        tvB1LiveTotalAct.setTextColor(Color.parseColor("#1E1B4B"));
        tvB1LiveTotalAct.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable totBg = new GradientDrawable();
        totBg.setColor(Color.parseColor("#EEF2FF"));
        totBg.setCornerRadius(dp(10));
        totBg.setStroke(dp(1), Color.parseColor("#C7D2FE"));
        tvB1LiveTotalAct.setBackground(totBg);
        LinearLayout.LayoutParams totLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        totLp.topMargin = dp(12);
        tvB1LiveTotalAct.setLayoutParams(totLp);
        card.addView(tvB1LiveTotalAct);

        card.addView(createFieldLabel(ctx, "Observaciones del Día"));
        etB1Observations = createInputBox(ctx,
                "Ej: Registro de solicitudes y archivado digital / Coordinación de reuniones internas.",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        card.addView(etB1Observations);

        TextWatcher liveWatcher = new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressWatchers) return;
                refreshLiveCalculations();
            }
        };
        etB1Gestiones.addTextChangedListener(liveWatcher);
        etB1Documentos.addTextChangedListener(liveWatcher);
        etB1Coordinaciones.addTextChangedListener(liveWatcher);

        parent.addView(card);
    }

    private View createMetricRow(Context ctx, String title, String subtitle, EditText inputView) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(Color.parseColor("#F8FAFC"));
        rowBg.setCornerRadius(dp(12));
        rowBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        row.setBackground(rowBg);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.rightMargin = dp(12);
        textCol.setLayoutParams(tLp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText(title);
        tvTitle.setTextSize(13.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor("#1E293B"));
        textCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(ctx);
            tvSub.setText(subtitle);
            tvSub.setTextSize(11f);
            tvSub.setTextColor(Color.parseColor("#64748B"));
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sLp.topMargin = dp(2);
            tvSub.setLayoutParams(sLp);
            textCol.addView(tvSub);
        }
        row.addView(textCol);
        row.addView(inputView);
        return row;
    }

    private EditText createNumericBox(Context ctx, String hint) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(16.5f);
        et.setTypeface(Typeface.DEFAULT_BOLD);
        et.setTextColor(Color.parseColor("#1E1B4B"));
        et.setHintTextColor(Color.parseColor("#94A3B8"));
        et.setGravity(Gravity.CENTER);

        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(Color.WHITE);
        etBg.setCornerRadius(dp(10));
        etBg.setStroke(dp(1.5f), Color.parseColor("#CBD5E1"));
        et.setBackground(etBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(76), dp(44));
        et.setLayoutParams(lp);
        return et;
    }

    private void buildSaturdayNoticeCard(Context ctx, LinearLayout parent) {
        cardSaturdayNoticeBanner = new LinearLayout(ctx);
        cardSaturdayNoticeBanner.setOrientation(LinearLayout.VERTICAL);
        cardSaturdayNoticeBanner.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F1F5F9"));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.parseColor("#CBD5E1"));
        cardSaturdayNoticeBanner.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        cardSaturdayNoticeBanner.setLayoutParams(lp);

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("📅 CIERRE SEMANAL (HABILITADO LOS SÁBADOS)");
        tvTitle.setTextSize(12.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(Color.parseColor("#334155"));
        cardSaturdayNoticeBanner.addView(tvTitle);

        TextView tvDesc = new TextView(ctx);
        tvDesc.setText("Las secciones \"2. Resumen Semanal y Acciones\" y \"5. Observaciones Generales\" corresponden al balance final de la semana y se completan los días SÁBADO.");
        tvDesc.setTextSize(12f);
        tvDesc.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = dp(4);
        tvDesc.setLayoutParams(dLp);
        cardSaturdayNoticeBanner.addView(tvDesc);

        parent.addView(cardSaturdayNoticeBanner);
    }

    private void buildBlock2And3SummaryActionsCard(Context ctx, LinearLayout parent) {
        cardBlock2And3 = createSectionCard(ctx);

        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.addView(createNumberBadge(ctx, "2"));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("RESUMEN SEMANAL Y ACCIONES DEL CARGO");
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_primary));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tLp.leftMargin = dp(10);
        tvTitle.setLayoutParams(tLp);
        headerRow.addView(tvTitle);

        tvWeeklyKpiLevelBadge = new TextView(ctx);
        tvWeeklyKpiLevelBadge.setText("★ EXCELENTE");
        tvWeeklyKpiLevelBadge.setTextSize(11f);
        tvWeeklyKpiLevelBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvWeeklyKpiLevelBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        headerRow.addView(tvWeeklyKpiLevelBadge);

        cardBlock2And3.addView(headerRow);

        tvWeeklySummaryTotals = new TextView(ctx);
        tvWeeklySummaryTotals.setTextSize(12f);
        tvWeeklySummaryTotals.setTextColor(Color.parseColor("#334155"));
        tvWeeklySummaryTotals.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable sumBg = new GradientDrawable();
        sumBg.setColor(Color.parseColor("#F8FAFC"));
        sumBg.setCornerRadius(dp(10));
        sumBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        tvWeeklySummaryTotals.setBackground(sumBg);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(10);
        tvWeeklySummaryTotals.setLayoutParams(sLp);
        cardBlock2And3.addView(tvWeeklySummaryTotals);

        TextView tvActionsTitle = createFieldLabel(ctx, "3. Acciones que realiza el Asistente Administrativo (Checklist SIGD)");
        cardBlock2And3.addView(tvActionsTitle);

        String[] fixedActions = {
                "Registro y control de documentos.",
                "Coordinación de actividades y reuniones.",
                "Seguimiento de requerimientos (compras, servicios, recursos).",
                "Actualización de formatos y registros internos.",
                "Elaboración de reportes e informes de gestión.",
                "Apoyo logístico y administrativo al equipo."
        };

        for (int i = 0; i < fixedActions.length; i++) {
            CheckBox cb = new CheckBox(ctx);
            cb.setText(fixedActions[i]);
            cb.setTextSize(12f);
            cb.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_primary));
            cb.setChecked(true);
            cb.setPadding(dp(4), dp(2), dp(4), dp(2));
            cbActions[i] = cb;
            cardBlock2And3.addView(cb);
        }

        cbOtherActivities = new CheckBox(ctx);
        cbOtherActivities.setText("Otras actividades asignadas.");
        cbOtherActivities.setTextSize(12f);
        cbOtherActivities.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_primary));
        cardBlock2And3.addView(cbOtherActivities);

        parent.addView(cardBlock2And3);
    }

    private void buildBlock5ObservationsCard(Context ctx, LinearLayout parent) {
        cardBlock5 = createSectionCard(ctx);

        LinearLayout headerRow = new LinearLayout(ctx);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.addView(createNumberBadge(ctx, "5"));

        TextView tvTitle = new TextView(ctx);
        tvTitle.setText("OBSERVACIONES GENERALES (CIERRE SEMANAL)");
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_primary));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.leftMargin = dp(10);
        tvTitle.setLayoutParams(tLp);
        headerRow.addView(tvTitle);
        cardBlock5.addView(headerRow);

        cardBlock5.addView(createFieldLabel(ctx, "Síntesis del desempeño semanal de Asistencia Administrativa"));
        etGeneralObservations = createInputBox(ctx,
                "Se cumplió con la gestión administrativa, el registro de documentos y la coordinación de actividades...",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etGeneralObservations.setMinLines(3);
        cardBlock5.addView(etGeneralObservations);

        parent.addView(cardBlock5);
    }

    private void buildActionButtons(Context ctx, LinearLayout parent) {
        btnSaveDayRecord = new TextView(ctx);
        btnSaveDayRecord.setText("✓  Confirmar y Registrar Día (Huella / PIN)");
        btnSaveDayRecord.setGravity(Gravity.CENTER);
        btnSaveDayRecord.setTextColor(Color.WHITE);
        btnSaveDayRecord.setTextSize(15f);
        btnSaveDayRecord.setTypeface(Typeface.DEFAULT_BOLD);
        btnSaveDayRecord.setPadding(dp(16), dp(15), dp(16), dp(15));
        btnSaveDayRecord.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(6);
        btnSaveDayRecord.setLayoutParams(btnLp);
        btnSaveDayRecord.setOnClickListener(v -> handleConfirmAndSaveDay());
        parent.addView(btnSaveDayRecord);
    }

    private void loadDayIntoForm(int dayIndex) {
        selectedDayIndex = Math.max(0, Math.min(5, dayIndex));
        KpiAdminAssistantWeekReport week = storage.getCurrentWeek();
        if (week == null || week.getDays() == null || week.getDays().size() <= selectedDayIndex) {
            return;
        }

        KpiAdminAssistantDayRecord rec = week.getDays().get(selectedDayIndex);
        suppressWatchers = true;

        isMorningShift = week.isMorningShift();
        updateShiftButtonsUI();

        tvB1DayTitle.setText("CONTROL DIARIO · " + rec.getDayName().toUpperCase(Locale.getDefault()));
        tvB1ActivityName.setText("Actividad Principal: " + rec.getMainActivity().replace("\n", " "));
        tvB1GoalText.setText("Meta Diaria (por turno): " + rec.getDailyGoalLabel().replace("\n", " ")
                + "  (Esperado: " + rec.getDailyExpectedTotal() + " act.)");

        int[] defaultGest = {12, 5, 6, 3, 7, 1};
        int[] defaultDoc = {8, 2, 3, 4, 5, 2};
        int[] defaultCoord = {3, 4, 2, 2, 2, 1};

        if (rec.isRegistered()) {
            etB1Gestiones.setText(String.valueOf(rec.getGestionesCount()));
            etB1Documentos.setText(String.valueOf(rec.getDocumentosCount()));
            etB1Coordinaciones.setText(String.valueOf(rec.getCoordinacionesCount()));
        } else {
            etB1Gestiones.setText(String.valueOf(defaultGest[selectedDayIndex]));
            etB1Documentos.setText(String.valueOf(defaultDoc[selectedDayIndex]));
            etB1Coordinaciones.setText(String.valueOf(defaultCoord[selectedDayIndex]));
        }
        etB1Observations.setText(rec.getObservations());

        cbOtherActivities.setChecked(week.isOtherActivitiesChecked());
        etGeneralObservations.setText(week.getGeneralObservations());

        suppressWatchers = false;

        boolean isSaturday = (selectedDayIndex == 5);
        if (cardBlock2And3 != null) {
            cardBlock2And3.setVisibility(isSaturday ? View.VISIBLE : View.GONE);
        }
        if (cardBlock5 != null) {
            cardBlock5.setVisibility(isSaturday ? View.VISIBLE : View.GONE);
        }
        if (cardSaturdayNoticeBanner != null) {
            cardSaturdayNoticeBanner.setVisibility(isSaturday ? View.GONE : View.VISIBLE);
        }

        refreshDayTabsVisuals(week.getDays());
        applyLockState(rec);
        refreshLiveCalculations();
    }

    private void refreshLiveCalculations() {
        KpiAdminAssistantWeekReport week = storage.getCurrentWeek();
        int expectedToday = KpiAdminAssistantStorageManager.DEFAULT_DAILY_EXPECTED_TOTALS[selectedDayIndex];
        int g = parseIntSafe(etB1Gestiones.getText().toString(), 0);
        int d = parseIntSafe(etB1Documentos.getText().toString(), 0);
        int c = parseIntSafe(etB1Coordinaciones.getText().toString(), 0);
        int todayTotal = g + d + c;
        int todayPct = expectedToday > 0 ? Math.min(100, Math.round((todayTotal * 100f) / expectedToday)) : 100;

        tvB1LiveTotalAct.setText("Total Actividades del Día: " + todayTotal
                + "  (Gestiones: " + g + " + Documentos: " + d + " + Coordinaciones: " + c + ")");
        stylePercentageBadge(tvB1LiveComplianceBadge, todayPct);

        // Resumen Semanal acumulado sobre la meta de 72 actividades
        int sumGest = 0;
        int sumDoc = 0;
        int sumCoord = 0;
        if (week != null && week.getDays() != null) {
            List<KpiAdminAssistantDayRecord> days = week.getDays();
            for (int i = 0; i < days.size(); i++) {
                if (i == selectedDayIndex) {
                    sumGest += g;
                    sumDoc += d;
                    sumCoord += c;
                } else if (days.get(i).isRegistered()) {
                    sumGest += days.get(i).getGestionesCount();
                    sumDoc += days.get(i).getDocumentosCount();
                    sumCoord += days.get(i).getCoordinacionesCount();
                }
            }
        }
        int totalWeeklyAct = sumGest + sumDoc + sumCoord;
        int weeklyPct = Math.min(100, Math.round((totalWeeklyAct * 100f) / 72f));

        tvWeeklySummaryTotals.setText(
                "Acumulado Semanal:  Gestiones: " + sumGest
                        + "  |  Documentos: " + sumDoc
                        + "  |  Coordinaciones: " + sumCoord
                        + "\nTotal Semanal: " + totalWeeklyAct + " / 72 actividades (" + weeklyPct + "% cumplimiento)"
        );

        styleKpiLevelBadge(tvWeeklyKpiLevelBadge, weeklyPct);
    }

    private void styleKpiLevelBadge(TextView badge, int pct) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (pct >= 90) {
            badge.setText("★ EXCELENTE (" + pct + "%)");
            bg.setColor(Color.parseColor("#DCFCE7"));
            badge.setTextColor(Color.parseColor("#15803D"));
        } else if (pct >= 80) {
            badge.setText("★ MUY BUENO (" + pct + "%)");
            bg.setColor(Color.parseColor("#DBEAFE"));
            badge.setTextColor(Color.parseColor("#1D4ED8"));
        } else if (pct >= 60) {
            badge.setText("★ BUENO (" + pct + "%)");
            bg.setColor(Color.parseColor("#FEF9C3"));
            badge.setTextColor(Color.parseColor("#A16207"));
        } else if (pct >= 40) {
            badge.setText("★ REGULAR (" + pct + "%)");
            bg.setColor(Color.parseColor("#FFEDD5"));
            badge.setTextColor(Color.parseColor("#C2410C"));
        } else {
            badge.setText("★ DEFICIENTE (" + pct + "%)");
            bg.setColor(Color.parseColor("#FEE2E2"));
            badge.setTextColor(Color.parseColor("#B91C1C"));
        }
        badge.setBackground(bg);
    }

    private void refreshDayTabsVisuals(List<KpiAdminAssistantDayRecord> days) {
        for (int i = 0; i < 6; i++) {
            TextView pill = dayTabPills[i];
            if (pill == null) continue;
            KpiAdminAssistantDayRecord d = (days != null && i < days.size()) ? days.get(i) : null;
            boolean locked = d != null && d.isLocked();
            boolean selected = (i == selectedDayIndex);

            String label = KpiAdminAssistantStorageManager.DAY_SHORTS[i] + (locked ? " ✓" : "");
            pill.setText(label);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(10));
            if (selected) {
                bg.setColor(Color.parseColor("#2E2A72"));
                pill.setTextColor(Color.WHITE);
            } else if (locked) {
                bg.setColor(Color.parseColor("#DCFCE7"));
                bg.setStroke(dp(1), Color.parseColor("#86EFAC"));
                pill.setTextColor(Color.parseColor("#166534"));
            } else {
                bg.setColor(Color.parseColor("#F1F5F9"));
                pill.setTextColor(Color.parseColor("#475569"));
            }
            pill.setBackground(bg);
        }
    }

    private void applyLockState(KpiAdminAssistantDayRecord rec) {
        boolean locked = rec.isLocked();
        GradientDrawable banBg = new GradientDrawable();
        banBg.setCornerRadius(dp(10));

        if (locked) {
            banBg.setColor(Color.parseColor("#DCFCE7"));
            banBg.setStroke(dp(1), Color.parseColor("#86EFAC"));
            tvDayStatusBanner.setBackground(banBg);
            tvDayStatusBanner.setTextColor(Color.parseColor("#166534"));
            tvDayStatusBanner.setText("🔒 " + rec.getDayName() + " (" + rec.getDate()
                    + ") YA REGISTRADO Y REFLEJADO EN EL REPORTE SIGD ("
                    + rec.getTotalActivities() + " actividades · " + rec.getCompliancePercentage() + "%).");
        } else {
            banBg.setColor(Color.parseColor("#FEF3C7"));
            banBg.setStroke(dp(1), Color.parseColor("#FDE68A"));
            tvDayStatusBanner.setBackground(banBg);
            tvDayStatusBanner.setTextColor(Color.parseColor("#92400E"));
            tvDayStatusBanner.setText("⏳ " + rec.getDayName() + " (" + rec.getDate()
                    + ") pendiente de confirmación. En el reporte se muestra \"—\" hasta que lo registres.");
        }

        boolean editable = !locked;
        float alpha = locked ? 0.45f : 1.0f;

        etB1Gestiones.setEnabled(editable);
        etB1Gestiones.setAlpha(alpha);
        etB1Documentos.setEnabled(editable);
        etB1Documentos.setAlpha(alpha);
        etB1Coordinaciones.setEnabled(editable);
        etB1Coordinaciones.setAlpha(alpha);
        etB1Observations.setEnabled(editable);
        etB1Observations.setAlpha(alpha);

        for (CheckBox cb : cbActions) {
            if (cb != null) {
                cb.setEnabled(editable);
                cb.setAlpha(alpha);
            }
        }
        if (cbOtherActivities != null) {
            cbOtherActivities.setEnabled(editable);
            cbOtherActivities.setAlpha(alpha);
        }

        boolean isSaturday = (selectedDayIndex == 5);
        btnSaveDayRecord.setEnabled(editable);
        btnSaveDayRecord.setAlpha(locked ? 0.5f : 1.0f);
        btnSaveDayRecord.setText(locked
                ? "🔒  Día Registrado (Solo Lectura)"
                : (isSaturday
                        ? "✓  Guardar Sábado y Cerrar Semana (Huella / PIN)"
                        : "✓  Confirmar y Registrar " + rec.getDayName() + " (Huella / PIN)"));
    }

    private void handleConfirmAndSaveDay() {
        KpiAdminAssistantWeekReport week = storage.getCurrentWeek();
        if (week == null || week.getDays() == null || week.getDays().size() <= selectedDayIndex) {
            return;
        }
        KpiAdminAssistantDayRecord rec = week.getDays().get(selectedDayIndex);
        if (rec.isLocked()) {
            Toast.makeText(getContext(), "Este día ya fue registrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        int gest = parseIntSafe(etB1Gestiones.getText().toString(), 0);
        int doc = parseIntSafe(etB1Documentos.getText().toString(), 0);
        int coord = parseIntSafe(etB1Coordinaciones.getText().toString(), 0);
        int total = gest + doc + coord;

        String summaryMsg = "Día: " + rec.getDayName() + " (" + rec.getDate() + ")\n"
                + "• Actividad: " + rec.getMainActivity().replace("\n", " ") + "\n"
                + "• Gestiones realizadas: " + gest + "\n"
                + "• Documentos registrados: " + doc + "\n"
                + "• Coordinaciones internas: " + coord + "\n"
                + "• Total actividades del día: " + total + "\n\n"
                + "Al confirmar con tu huella o PIN, este día quedará bloqueado y se reflejará automáticamente en el Reporte Semanal de Asistente Administrativo.";

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Registro Diario · Asistente Administrativo")
                .setMessage(summaryMsg)
                .setNegativeButton("Revisar", null)
                .setPositiveButton("Autenticar y Guardar", (d, w) -> {
                    KpiBiometricHelper.authenticate(
                            KpiAdminAssistantContainerFragment.this,
                            "Confirmar KPI de " + rec.getDayName() + " · Asistente Administrativo",
                            new KpiBiometricHelper.BiometricCallback() {
                                @Override
                                public void onSuccess() {
                                    commitDaySave(rec);
                                }

                                @Override
                                public void onError(int errorCode, String errorMessage) {
                                    if (errorCode == -1) {
                                        commitDaySave(rec);
                                    } else {
                                        Toast.makeText(getContext(), "Verificación biométrica cancelada", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailed() {
                                    Toast.makeText(getContext(), "Biometría no reconocida", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                })
                .show();
    }

    private void commitDaySave(KpiAdminAssistantDayRecord rec) {
        int gest = parseIntSafe(etB1Gestiones.getText().toString(), 0);
        int doc = parseIntSafe(etB1Documentos.getText().toString(), 0);
        int coord = parseIntSafe(etB1Coordinaciones.getText().toString(), 0);
        String obs = etB1Observations.getText().toString().trim();
        if (obs.isEmpty()) {
            obs = "Cumplimiento de meta diaria de soporte administrativo.";
        }

        rec.setGestionesCount(gest);
        rec.setDocumentosCount(doc);
        rec.setCoordinacionesCount(coord);
        rec.setObservations(obs);
        rec.setRegistered(true);
        rec.setLocked(true);

        storage.saveDayRecord(rec);
        KpiAdminAssistantWeekReport week = storage.getCurrentWeek();
        if (selectedDayIndex == 5) {
            storage.saveWeekClosingData(
                    isMorningShift,
                    cbOtherActivities != null && cbOtherActivities.isChecked(),
                    etGeneralObservations != null ? etGeneralObservations.getText().toString() : ""
            );
        } else {
            storage.saveWeekClosingData(
                    isMorningShift,
                    week != null && week.isOtherActivitiesChecked(),
                    week != null ? week.getGeneralObservations() : ""
            );
        }

        boolean isSaturday = (selectedDayIndex == 5);
        boolean allComplete = storage.areAllCurrentWeekDaysLocked();
        int nextDay = storage.getNextUnlockedDayIndexAfter(selectedDayIndex);
        if (isSaturday || allComplete) {
            KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());
            loadDayIntoForm(selectedDayIndex);
            KpiPandaSuccessDialog.show(requireContext(), "Asistente Administrativo", false,
                    allComplete ? "KPI Semanal Completo" : rec.getDayName(),
                    rec.getCompliancePercentage(), Math.max(10, rec.getTotalActivities()), () -> {
                        if (nextDay >= 0 && nextDay != selectedDayIndex) {
                            loadDayIntoForm(nextDay);
                        }
                    });
        } else {
            KpiSoundHelper.playDailySuccess(requireContext());
            Toast.makeText(getContext(),
                    "✓ " + rec.getDayName() + " guardado y actualizado en el Reporte Semanal.",
                    Toast.LENGTH_SHORT).show();
            if (nextDay >= 0 && nextDay != selectedDayIndex) {
                loadDayIntoForm(nextDay);
            } else {
                loadDayIntoForm(selectedDayIndex);
            }
        }
    }

    private void openOfficialReportSheet() {
        if (selectedDayIndex == 5) {
            storage.saveWeekClosingData(
                    isMorningShift,
                    cbOtherActivities != null && cbOtherActivities.isChecked(),
                    etGeneralObservations != null ? etGeneralObservations.getText().toString() : ""
            );
        }
        if (getActivity() != null) {
            String collabName = resolveActiveCollaboratorName(requireContext());
            Fragment hub = KpiReportsHubFragment.newInstanceExclusive("Asistente Administrativo", collabName);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main, hub)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private LinearLayout createSectionCard(Context ctx) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.kpi_bg_card_elegant_accent);
        int p = dp(16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createNumberBadge(Context ctx, String number) {
        TextView tv = new TextView(ctx);
        tv.setText(number);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2E2A72"));
        bg.setCornerRadius(dp(8));
        tv.setBackground(bg);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(26), dp(26)));
        return tv;
    }

    private TextView createFieldLabel(Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(11.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_secondary));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText createInputBox(Context ctx, String hint, int inputType) {
        EditText et = new EditText(ctx);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextSize(13.5f);
        et.setTextColor(ContextCompat.getColor(ctx, R.color.kpi_text_primary));
        et.setHintTextColor(Color.parseColor("#94A3B8"));
        et.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        return et;
    }

    private void stylePercentageBadge(TextView badge, int pct) {
        badge.setText(pct + "%");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (pct >= 90) {
            bg.setColor(Color.parseColor("#DCFCE7"));
            badge.setTextColor(Color.parseColor("#15803D"));
        } else if (pct >= 75) {
            bg.setColor(Color.parseColor("#FEF9C3"));
            badge.setTextColor(Color.parseColor("#A16207"));
        } else {
            bg.setColor(Color.parseColor("#FEE2E2"));
            badge.setTextColor(Color.parseColor("#B91C1C"));
        }
        badge.setBackground(bg);
    }

    private int parseIntSafe(String raw, int fallback) {
        if (raw == null || raw.trim().isEmpty()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
