package pe.net.ws.kpiweb.features.kpi.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository;
import pe.net.ws.kpiweb.features.kpi.data.KpiRouteDbHelper;
import pe.net.ws.kpiweb.features.kpi.data.KpiSalesWeekManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiSundayArchivalHelper;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;
import pe.net.ws.kpiweb.features.kpi.export.KpiPrintManager;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.model.KpiRouteItem;
import pe.net.ws.kpiweb.features.kpi.ui.view.KpiZoomableSheetLayout;
import pe.net.ws.kpiweb.features.kpi.model.KpiSalesDay;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.senior.data.KpiSeniorStorageManager;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorDayRecord;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorWeekReport;
import pe.net.ws.kpiweb.features.kpi.community.data.KpiCommunityStorageManager;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityDayRecord;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityWeekReport;
import pe.net.ws.kpiweb.features.kpi.administrator.data.KpiAdministratorStorageManager;
import pe.net.ws.kpiweb.features.kpi.administrator.model.KpiAdministratorDayRecord;
import pe.net.ws.kpiweb.features.kpi.administrator.model.KpiAdministratorWeekReport;
import pe.net.ws.kpiweb.features.kpi.accountant.data.KpiAccountantStorageManager;
import pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantDayRecord;
import pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantWeekReport;
import pe.net.ws.kpiweb.features.kpi.assistant.data.KpiAdminAssistantStorageManager;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantDayRecord;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantWeekReport;
import pe.net.ws.kpiweb.features.kpi.photographer.data.KpiPhotographerStorageManager;
import pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerDayRecord;
import pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerWeekReport;
import pe.net.ws.kpiweb.features.kpi.videographer.data.KpiVideographerStorageManager;
import pe.net.ws.kpiweb.features.kpi.videographer.model.KpiVideographerDayRecord;
import pe.net.ws.kpiweb.features.kpi.videographer.model.KpiVideographerWeekReport;
import pe.net.ws.kpiweb.features.kpi.trafficker.data.KpiTraffickerStorageManager;
import pe.net.ws.kpiweb.features.kpi.trafficker.model.KpiTraffickerDayRecord;
import pe.net.ws.kpiweb.features.kpi.trafficker.model.KpiTraffickerWeekReport;
import pe.net.ws.kpiweb.features.kpi.manager.data.KpiGeneralManagerStorageManager;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerDayRecord;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerWeekReport;
import pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorDayRecord;

/**
 * Apartado Centralizado e Independiente de Reportes Semanales (SIGD 2026).
 * Modulo desacoplado de los formularios de registro diario.
 *
 * LÓGICA DE CONTINUIDAD Y PERSISTENCIA REAL:
 * - Consulta directamente los repositorios y bases de datos locales:
 *   1. Diseñador Gráfico Senior -> KpiSeniorStorageManager
 *   2. Diseñador Gráfico -> KpiDesignerMemoryRepository
 *   3. Asesor de Ventas -> KpiSalesWeekManager & KpiRouteDbHelper (SQLite)
 * - Los reportes solo muestran datos en los días que efectivamente hayan sido registrados.
 * - Los días pendientes de llenado muestran guión "—".
 * - Los totales y porcentajes se calculan dinámicamente según el avance real acumulado.
 */
public class KpiReportsHubFragment extends Fragment {

    private static final String ARG_INITIAL_ROLE = "arg_initial_role";
    private static final String ARG_INITIAL_COLLAB = "arg_initial_collab";

    private Spinner spnReportCargo;
    private Spinner spnReportCollaborator;
    private Spinner spnReportWeek;
    private TextView btnViewModeFilled;
    private TextView btnViewModeBlank;
    private FrameLayout flReportSheetContainer;
    private KpiZoomableSheetLayout zoomReportSheetLayout;
    private ScrollView svReportsContentScroll;
    private View btnHubPrint;
    private TextView btnHubSendReport;
    private View btnHubOpenHistory;

    private int selectedShiftIndex = 0; // 0 = Turno Mañana, 1 = Turno Tarde
    private boolean isBlankMode = false;
    public static final String ARG_EXCLUSIVE_USER = "arg_exclusive_user";
    private boolean isExclusiveUserMode = false;

    private String selectedCargo = "Diseñador Gráfico Senior";
    private String selectedCollab = "";
    private String selectedWeek = "Semana Actual (En Curso)";

    public static KpiReportsHubFragment newInstance() {
        return new KpiReportsHubFragment();
    }

    public static KpiReportsHubFragment newInstance(String initialRole, String initialCollab) {
        KpiReportsHubFragment fragment = new KpiReportsHubFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_ROLE, initialRole);
        args.putString(ARG_INITIAL_COLLAB, initialCollab);
        fragment.setArguments(args);
        return fragment;
    }

    public static KpiReportsHubFragment newInstanceExclusive(String initialRole, String initialCollab) {
        KpiReportsHubFragment fragment = new KpiReportsHubFragment();
        Bundle args = new Bundle();
        String normRole = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.normalizeOperationalRole(initialRole);
        args.putString(ARG_INITIAL_ROLE, normRole);
        args.putString(ARG_INITIAL_COLLAB, initialCollab);
        args.putBoolean(ARG_EXCLUSIVE_USER, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String authRole = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        String authCollab = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        if (authRole != null && !authRole.trim().isEmpty()) {
            selectedCargo = authRole;
        }
        if (authCollab != null && !authCollab.trim().isEmpty() && !authCollab.equals("Colaborador WebSite+")) {
            selectedCollab = authCollab;
        }
        if (getArguments() != null) {
            String role = getArguments().getString(ARG_INITIAL_ROLE);
            if (role != null && !role.trim().isEmpty()) {
                selectedCargo = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.normalizeOperationalRole(role);
            }
            String collab = getArguments().getString(ARG_INITIAL_COLLAB);
            if (collab != null && !collab.trim().isEmpty()) {
                selectedCollab = collab;
            }
            isExclusiveUserMode = getArguments().getBoolean(ARG_EXCLUSIVE_USER, false);
        } else {
            selectedCargo = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.normalizeOperationalRole(selectedCargo);
        }

        View root = inflater.inflate(R.layout.kpi_fragment_reports_hub, container, false);
        initViews(root);
        setupSpinners();
        setupModeButtons();
        renderCurrentReport();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al regresar de cualquier formulario, refresca la lectura de la base de datos
        renderCurrentReport();
        pe.net.ws.kpiweb.features.kpi.sync.KpiFormRecovery.refresh(requireContext(), (success, restored) -> {
            if (isAdded() && success && restored > 0) renderCurrentReport();
        });
    }

    private void initViews(View root) {
        root.findViewById(R.id.btnReportsBack).setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        spnReportCargo = root.findViewById(R.id.spnReportCargo);
        spnReportCollaborator = root.findViewById(R.id.spnReportCollaborator);
        spnReportWeek = root.findViewById(R.id.spnReportWeek);
        btnViewModeFilled = root.findViewById(R.id.btnViewModeFilled);
        btnViewModeBlank = root.findViewById(R.id.btnViewModeBlank);
        flReportSheetContainer = root.findViewById(R.id.flReportSheetContainer);
        zoomReportSheetLayout = root.findViewById(R.id.zoomReportSheetLayout);
        TextView zoomPercent = root.findViewById(R.id.tvReportZoomPercent);
        TextView zoomIn = root.findViewById(R.id.btnReportZoomIn);
        TextView zoomOut = root.findViewById(R.id.btnReportZoomOut);
        if (zoomReportSheetLayout != null) {
            zoomReportSheetLayout.setOnZoomChangeListener((scale, percent) -> zoomPercent.setText(percent + "%"));
            zoomIn.setOnClickListener(v -> zoomReportSheetLayout.zoomIn());
            zoomOut.setOnClickListener(v -> zoomReportSheetLayout.zoomOut());
            zoomPercent.setOnClickListener(v -> zoomReportSheetLayout.resetZoom());
        }
        svReportsContentScroll = root.findViewById(R.id.svReportsContentScroll);

        btnHubPrint = root.findViewById(R.id.btnHubPrint);
        if (btnHubPrint != null) {
            btnHubPrint.setOnClickListener(v -> handlePrintReport());
        }

        btnHubSendReport = root.findViewById(R.id.btnHubSendReport);
        if (btnHubSendReport != null) {
            if (new pe.net.ws.kpiweb.core.session.SessionManager(requireContext()).isLoggedIn())
                btnHubSendReport.setText("Ver reportes confirmados");
            btnHubSendReport.setOnClickListener(v -> handleSendReport());
        }

        btnHubOpenHistory = root.findViewById(R.id.btnHubOpenHistory);
        if (btnHubOpenHistory != null) {
            btnHubOpenHistory.setOnClickListener(v -> showHistoryDialog());
        }
    }

    private void setupSpinners() {
        List<String> cargoList = new ArrayList<>();
        if (isExclusiveUserMode) {
            cargoList.add(selectedCargo);
        } else {
            cargoList.add("Administrador");
            cargoList.add("Asistente Administrativo");
            cargoList.add("Contador");
            cargoList.add("Community Manager");
            cargoList.add("Diseñador Gráfico Senior");
            cargoList.add("Diseñador Gráfico");
            cargoList.add("Asesor de Ventas");
            cargoList.add("Fotógrafo");
            cargoList.add("Trafficker Digital");
            cargoList.add("Videógrafo");
            cargoList.add("Gerente General");
            cargoList.add("Programador Junior");
            cargoList.add("Diseñador Gráfico Junior");

            if (selectedCargo != null && !selectedCargo.trim().isEmpty() && !cargoList.contains(selectedCargo)) {
                cargoList.add(0, selectedCargo);
            }
        }

        ArrayAdapter<String> cargoAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.kpi_item_spinner_hub_selected, cargoList);
        cargoAdapter.setDropDownViewResource(R.layout.kpi_item_spinner_hub_dropdown);
        spnReportCargo.setAdapter(cargoAdapter);

        int initCargoPos = cargoList.indexOf(selectedCargo);
        if (initCargoPos >= 0) {
            spnReportCargo.setSelection(initCargoPos);
        } else if (!cargoList.isEmpty()) {
            selectedCargo = cargoList.get(0);
            spnReportCargo.setSelection(0);
        }

        if (isExclusiveUserMode) {
            spnReportCargo.setEnabled(false);
            spnReportCargo.setClickable(false);
        } else {
            spnReportCargo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String newCargo = cargoList.get(position);
                    if (!newCargo.equalsIgnoreCase(selectedCargo)) {
                        selectedCargo = newCargo;
                        selectedCollab = ""; // Reiniciar colaborador para tomar el primero del nuevo cargo
                        updateCollaboratorSpinner();
                        if (!hasAfternoonShiftForCurrentCollab()) {
                            selectedShiftIndex = 0;
                        }
                        updateModeButtonStyles();
                        renderCurrentReport();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        List<String> weekList = new ArrayList<>();
        weekList.add("Semana Actual (En Curso)");
        weekList.add("Semana N.º 37 (Del 07/09/2026 al 12/09/2026)");
        weekList.add("Semana N.º 36 (Del 31/08/2026 al 05/09/2026)");
        weekList.add("Semana N.º 35 (Del 24/08/2026 al 29/08/2026)");
        weekList.add("Semana N.º 34 (Del 17/08/2026 al 22/08/2026)");
        weekList.add("Semana N.º 33 (Del 10/08/2026 al 15/08/2026)");
        weekList.add("Semana N.º 32 (Del 03/08/2026 al 08/08/2026)");

        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.kpi_item_spinner_hub_selected, weekList);
        weekAdapter.setDropDownViewResource(R.layout.kpi_item_spinner_hub_dropdown);
        spnReportWeek.setAdapter(weekAdapter);
        int initWeekPos = weekList.indexOf(selectedWeek);
        if (initWeekPos >= 0) spnReportWeek.setSelection(initWeekPos);

        spnReportWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWeek = weekList.get(position);
                renderCurrentReport();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateCollaboratorSpinner();
    }

    private void updateCollaboratorSpinner() {
        List<String> collabNames = new ArrayList<>();
        if (isExclusiveUserMode) {
            String activeAuthUser = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String nameToUse = (selectedCollab != null && !selectedCollab.isEmpty()) ? selectedCollab : activeAuthUser;
            if (nameToUse == null || nameToUse.isEmpty()) {
                nameToUse = "Colaborador (" + selectedCargo + ")";
            }
            collabNames.add(nameToUse);
            selectedCollab = nameToUse;
        } else {
            KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
            List<KpiCollaboratorProfile> profiles = repo.getCollaboratorsForRole(selectedCargo);
            for (KpiCollaboratorProfile p : profiles) {
                collabNames.add(p.getName());
            }
            if (collabNames.isEmpty()) {
                String activeAuthUser = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
                if (activeAuthUser != null && !activeAuthUser.isEmpty() && !activeAuthUser.equals("Colaborador WebSite+")) {
                    collabNames.add(activeAuthUser);
                } else {
                    collabNames.add("Colaborador (" + selectedCargo + ")");
                }
            }
        }

        ArrayAdapter<String> collabAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.kpi_item_spinner_hub_selected, collabNames);
        collabAdapter.setDropDownViewResource(R.layout.kpi_item_spinner_hub_dropdown);
        spnReportCollaborator.setAdapter(collabAdapter);

        int pos = (selectedCollab != null && !selectedCollab.isEmpty()) ? collabNames.indexOf(selectedCollab) : -1;
        if (pos >= 0) {
            spnReportCollaborator.setSelection(pos);
        } else {
            selectedCollab = collabNames.get(0);
            spnReportCollaborator.setSelection(0);
        }

        if (isExclusiveUserMode) {
            spnReportCollaborator.setEnabled(false);
            spnReportCollaborator.setClickable(false);
        } else {
            spnReportCollaborator.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedCollab = collabNames.get(position);
                    if (!hasAfternoonShiftForCurrentCollab()) {
                        selectedShiftIndex = 0;
                    }
                    updateModeButtonStyles();
                    renderCurrentReport();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private boolean hasAfternoonShiftForCurrentCollab() {
        if (selectedCollab == null || selectedCollab.isEmpty() || getContext() == null) return false;
        if (selectedCargo != null) {
            String c = selectedCargo.toLowerCase(Locale.getDefault());
            if (c.contains("junior") || c.contains("practicante") || c.contains("program")) {
                return true;
            }
        }
        pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository repo =
                pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository.getInstance(requireContext());
        pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile profile = repo.findByName(selectedCollab);
        if (profile != null) {
            if (profile.isPracticante()) return true;
            if (profile.getRole() != null) {
                String r = profile.getRole().toLowerCase(Locale.getDefault());
                if (r.contains("junior") || r.contains("practicante") || r.contains("program")) return true;
            }
            if (profile.getShift() != null) {
                String s = profile.getShift().toLowerCase(Locale.getDefault());
                return s.contains("tarde") || s.contains("completo") || s.contains("doble");
            }
        }
        return false;
    }

    private void applyStandardLevelHighlight(View sheet, int overallPct,
            int rowExcId, int rowMuyId, int rowBueId, int rowRegId, int rowDefId) {
        if (sheet == null) return;
        int[] rows = { rowExcId, rowMuyId, rowBueId, rowRegId, rowDefId };
        for (int id : rows) {
            View r = sheet.findViewById(id);
            if (r != null) {
                if (r instanceof TextView) {
                    r.setBackgroundColor(Color.parseColor("#F1F5F9"));
                } else {
                    r.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
        if (isBlankMode) return;

        if (overallPct >= 90) {
            View r = sheet.findViewById(rowExcId);
            if (r != null) r.setBackgroundColor(Color.parseColor("#DCFCE7"));
        } else if (overallPct >= 80) {
            View r = sheet.findViewById(rowMuyId);
            if (r != null) r.setBackgroundColor(Color.parseColor("#DBEAFE"));
        } else if (overallPct >= 60) {
            View r = sheet.findViewById(rowBueId);
            if (r != null) r.setBackgroundColor(Color.parseColor("#FEF9C3"));
        } else if (overallPct >= 40) {
            View r = sheet.findViewById(rowRegId);
            if (r != null) r.setBackgroundColor(Color.parseColor("#FFEDD5"));
        } else {
            View r = sheet.findViewById(rowDefId);
            if (r != null) r.setBackgroundColor(Color.parseColor("#FEE2E2"));
        }
    }

    private void setupModeButtons() {
        if (btnViewModeFilled != null) {
            btnViewModeFilled.setText("Turno Mañana");
            btnViewModeFilled.setOnClickListener(v -> {
                selectedShiftIndex = 0;
                isBlankMode = false;
                updateModeButtonStyles();
                renderCurrentReport();
            });
        }

        if (btnViewModeBlank != null) {
            btnViewModeBlank.setText("Turno Tarde");
            btnViewModeBlank.setOnClickListener(v -> {
                if (!hasAfternoonShiftForCurrentCollab()) {
                    Toast.makeText(getContext(), "Turno tarde no habilitado para este colaborador", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedShiftIndex = 1;
                isBlankMode = false;
                updateModeButtonStyles();
                renderCurrentReport();
            });
        }
        updateModeButtonStyles();
    }

    private void updateModeButtonStyles() {
        if (btnViewModeFilled == null || btnViewModeBlank == null || getContext() == null) return;
        boolean hasAfternoon = hasAfternoonShiftForCurrentCollab();
        btnViewModeBlank.setEnabled(hasAfternoon);
        btnViewModeBlank.setClickable(hasAfternoon);
        btnViewModeBlank.setAlpha(hasAfternoon ? 1.0f : 0.45f);

        if (selectedShiftIndex == 0 || !hasAfternoon) {
            selectedShiftIndex = 0;
            btnViewModeFilled.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
            btnViewModeFilled.setTextColor(Color.WHITE);
            btnViewModeBlank.setBackgroundResource(android.R.color.transparent);
            btnViewModeBlank.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        } else {
            btnViewModeBlank.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
            btnViewModeBlank.setTextColor(Color.WHITE);
            btnViewModeFilled.setBackgroundResource(android.R.color.transparent);
            btnViewModeFilled.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        }
    }

    private void renderCurrentReport() {
        if (flReportSheetContainer == null || getContext() == null) return;
        if (zoomReportSheetLayout != null) zoomReportSheetLayout.resetZoom();
        flReportSheetContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        String upperCargo = selectedCargo != null ? selectedCargo.trim().toUpperCase(Locale.ROOT)
                .replace("Ñ", "N")
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U") : "";
        if (upperCargo.contains("ASISTENTE") && upperCargo.contains("ADMIN")) {
            renderOfficialAdminAssistantSheet(inflater);
        } else if (upperCargo.contains("ADMINISTRADOR") || upperCargo.contains("ADMINISTRACION")) {
            renderOfficialAdministratorSheet(inflater);
        } else if (upperCargo.contains("CONTADOR") || upperCargo.contains("CONTAB")) {
            renderOfficialAccountantSheet(inflater);
        } else if (upperCargo.contains("COMMUNITY") || upperCargo.contains("REDES")) {
            renderOfficialCommunitySheet(inflater);
        } else if (upperCargo.contains("FOTOGRAFO") || upperCargo.contains("FOTOGRAFIA")) {
            renderOfficialPhotographerSheet(inflater);
        } else if (upperCargo.contains("VIDEOGRAFO") || upperCargo.contains("VIDEO")) {
            renderOfficialVideographerSheet(inflater);
        } else if (upperCargo.contains("TRAFFICKER") || upperCargo.contains("PAUTA")) {
            renderOfficialTraffickerSheet(inflater);
        } else if (upperCargo.contains("GERENTE") || upperCargo.contains("GERENCIA") || upperCargo.equals("ADMIN")) {
            renderOfficialGeneralManagerSheet(inflater);
        } else if (upperCargo.contains("PROGRAM") || upperCargo.contains("WEB") || upperCargo.contains("SISTEMA")) {
            renderOfficialProgrammerSheet(inflater);
        } else if (upperCargo.contains("JUNIOR") || upperCargo.contains("PRACTICANTE")) {
            if (upperCargo.contains("DISEN") || upperCargo.contains("GRAFIC")) {
                renderOfficialJuniorDesignerSheet(inflater);
            } else {
                renderOfficialJuniorGenericSheet(inflater);
            }
        } else if (upperCargo.contains("SENIOR")) {
            renderOfficialSeniorSheet(inflater);
        } else if (upperCargo.contains("DISEN") || upperCargo.contains("GRAFIC") || upperCargo.contains("CREATIV")) {
            renderOfficialDesignerSheet(inflater);
        } else if (upperCargo.contains("VENTA") || upperCargo.contains("COMERCIAL") || upperCargo.contains("ASESOR")) {
            renderOfficialSalesSheet(inflater);
        } else {
            renderOfficialJuniorGenericSheet(inflater);
        }
        // Current reports follow the shared template. Historical weeks keep their saved view.
        if (selectedWeek != null && selectedWeek.contains("Actual")
                && flReportSheetContainer.getChildCount() > 0) {
            final View currentSheet = flReportSheetContainer.getChildAt(0);
            final String cargo = selectedCargo;
            pe.net.ws.kpiweb.features.kpi.data.KpiTemplateSync.get(requireContext())
                    .refresh((ok, message) -> {
                        if (ok && isAdded() && flReportSheetContainer != null
                                && flReportSheetContainer.getChildCount() > 0
                                && flReportSheetContainer.getChildAt(0) == currentSheet
                                && cargo.equals(selectedCargo) && selectedWeek.contains("Actual")) {
                            pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine
                                    .getInstance(requireContext())
                                    .applyTemplateToReportSheet(requireContext(), cargo, currentSheet, false);
                        }
                    });
        }
    }

    // =========================================================================
    // 0A. REPORTE OFICIAL SIGD: ADMINISTRADOR
    // =========================================================================
    private void renderOfficialAdministratorSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_administrator_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvAdmCollabName = sheet.findViewById(R.id.tvAdmCollabName);
        TextView tvAdmCollabRole = sheet.findViewById(R.id.tvAdmCollabRole);
        TextView tvAdmCollabShift = sheet.findViewById(R.id.tvAdmCollabShift);
        TextView tvAdmCollabArea = sheet.findViewById(R.id.tvAdmCollabArea);
        TextView tvAdmCollabSupervisor = sheet.findViewById(R.id.tvAdmCollabSupervisor);
        TextView tvAdmCollabBoss = sheet.findViewById(R.id.tvAdmCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "ADMINISTRADOR"));

        String roleName = profile != null ? profile.getRole() : "ADMINISTRADOR";
        String areaName = profile != null ? profile.getArea() : "ADMINISTRACIÓN GENERAL";
        String supervisorName = profile != null ? profile.getSupervisor() : "GERENCIA GENERAL";
        String bossName = profile != null ? profile.getImmediateBoss() : "DIRECTORIO";

        KpiAdministratorStorageManager storage = KpiAdministratorStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiAdministratorWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        boolean morning = (week == null || week.isMorningShift());
        tvAdmCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        tvAdmCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        tvAdmCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        tvAdmCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        tvAdmCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        tvAdmCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvAdmWeekNumber = sheet.findViewById(R.id.tvAdmWeekNumber);
        TextView tvAdmDateFrom = sheet.findViewById(R.id.tvAdmDateFrom);
        TextView tvAdmDateTo = sheet.findViewById(R.id.tvAdmDateTo);

        if (week != null) {
            if (tvAdmWeekNumber != null) {
                tvAdmWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvAdmDateFrom != null && tvAdmDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvAdmDateFrom.setText("Del:  " + parts[0].trim());
                    tvAdmDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] gestIds = {R.id.tvAdmB1Gest_0, R.id.tvAdmB1Gest_1, R.id.tvAdmB1Gest_2, R.id.tvAdmB1Gest_3, R.id.tvAdmB1Gest_4, R.id.tvAdmB1Gest_5};
        int[] segIds = {R.id.tvAdmB1Seg_0, R.id.tvAdmB1Seg_1, R.id.tvAdmB1Seg_2, R.id.tvAdmB1Seg_3, R.id.tvAdmB1Seg_4, R.id.tvAdmB1Seg_5};
        int[] reuIds = {R.id.tvAdmB1Reu_0, R.id.tvAdmB1Reu_1, R.id.tvAdmB1Reu_2, R.id.tvAdmB1Reu_3, R.id.tvAdmB1Reu_4, R.id.tvAdmB1Reu_5};
        int[] totIds = {R.id.tvAdmB1Tot_0, R.id.tvAdmB1Tot_1, R.id.tvAdmB1Tot_2, R.id.tvAdmB1Tot_3, R.id.tvAdmB1Tot_4, R.id.tvAdmB1Tot_5};
        int[] pctIds = {R.id.tvAdmB1Pct_0, R.id.tvAdmB1Pct_1, R.id.tvAdmB1Pct_2, R.id.tvAdmB1Pct_3, R.id.tvAdmB1Pct_4, R.id.tvAdmB1Pct_5};
        int[] obsIds = {R.id.tvAdmB1Obs_0, R.id.tvAdmB1Obs_1, R.id.tvAdmB1Obs_2, R.id.tvAdmB1Obs_3, R.id.tvAdmB1Obs_4, R.id.tvAdmB1Obs_5};

        int sumGest = 0, sumSeg = 0, sumReu = 0, sumTot = 0, sumExp = 0;
        if (week != null && week.getDays() != null) {
            for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                KpiAdministratorDayRecord d = week.getDays().get(i);
                TextView tvG = sheet.findViewById(gestIds[i]);
                TextView tvS = sheet.findViewById(segIds[i]);
                TextView tvR = sheet.findViewById(reuIds[i]);
                TextView tvT = sheet.findViewById(totIds[i]);
                TextView tvP = sheet.findViewById(pctIds[i]);
                TextView tvO = sheet.findViewById(obsIds[i]);

                if (isBlankMode) {
                    if (tvG != null) tvG.setText("");
                    if (tvS != null) tvS.setText("");
                    if (tvR != null) tvR.setText("");
                    if (tvT != null) tvT.setText("");
                    if (tvP != null) tvP.setText("");
                    if (tvO != null) tvO.setText("");
                } else {
                    int g = d.getGestionesCount();
                    int s = d.getSeguimientosCount();
                    int r = d.getReunionesCount();
                    int t = d.getTotalActivities();
                    int p = d.getCompliancePercentage();
                    sumGest += g;
                    sumSeg += s;
                    sumReu += r;
                    sumTot += t;
                    sumExp += d.getDailyExpectedTotal();

                    if (tvG != null) tvG.setText(String.valueOf(g));
                    if (tvS != null) tvS.setText(String.valueOf(s));
                    if (tvR != null) tvR.setText(String.valueOf(r));
                    if (tvT != null) tvT.setText(String.valueOf(t));
                    if (tvP != null) tvP.setText(p + "%");
                    if (tvO != null) tvO.setText(d.getObservations());
                }
            }
        }

        TextView tvSumG = sheet.findViewById(R.id.tvAdmB1SumGest);
        TextView tvSumS = sheet.findViewById(R.id.tvAdmB1SumSeg);
        TextView tvSumR = sheet.findViewById(R.id.tvAdmB1SumReu);
        TextView tvSumT = sheet.findViewById(R.id.tvAdmB1SumTot);
        TextView tvSumP = sheet.findViewById(R.id.tvAdmB1SumPct);

        if (isBlankMode) {
            if (tvSumG != null) tvSumG.setText("");
            if (tvSumS != null) tvSumS.setText("");
            if (tvSumR != null) tvSumR.setText("");
            if (tvSumT != null) tvSumT.setText("");
            if (tvSumP != null) tvSumP.setText("");
        } else {
            if (tvSumG != null) tvSumG.setText(String.valueOf(sumGest));
            if (tvSumS != null) tvSumS.setText(String.valueOf(sumSeg));
            if (tvSumR != null) tvSumR.setText(String.valueOf(sumReu));
            if (tvSumT != null) tvSumT.setText(String.valueOf(sumTot));
            int overallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
            if (tvSumP != null) tvSumP.setText(overallPct + "%");
        }

        int admOverallPct = 0;
        if (!isBlankMode) {
            admOverallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
        }
        applyStandardLevelHighlight(sheet, admOverallPct,
                R.id.rowAdmLevelExcelente, R.id.rowAdmLevelMuyBueno, R.id.rowAdmLevelBueno,
                R.id.rowAdmLevelRegular, R.id.rowAdmLevelDeficiente);

        TextView tvObs = sheet.findViewById(R.id.tvAdmGeneralObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : (week != null ? week.getGeneralObservations() : ""));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0B. REPORTE OFICIAL SIGD: CONTADOR
    // =========================================================================
    private void renderOfficialAccountantSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_accountant_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvAccCollabName = sheet.findViewById(R.id.tvAccCollabName);
        TextView tvAccCollabRole = sheet.findViewById(R.id.tvAccCollabRole);
        TextView tvAccCollabShift = sheet.findViewById(R.id.tvAccCollabShift);
        TextView tvAccCollabArea = sheet.findViewById(R.id.tvAccCollabArea);
        TextView tvAccCollabSupervisor = sheet.findViewById(R.id.tvAccCollabSupervisor);
        TextView tvAccCollabBoss = sheet.findViewById(R.id.tvAccCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "CONTADOR"));

        String roleName = profile != null ? profile.getRole() : "CONTADOR";
        String areaName = profile != null ? profile.getArea() : "CONTABILIDAD Y FINANZAS";
        String supervisorName = profile != null ? profile.getSupervisor() : "GERENCIA GENERAL";
        String bossName = profile != null ? profile.getImmediateBoss() : "DIRECTORIO";

        KpiAccountantStorageManager storage = KpiAccountantStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiAccountantWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        boolean morning = (week == null || week.isMorningShift());
        tvAccCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        tvAccCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        tvAccCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        tvAccCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        tvAccCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        tvAccCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvAccWeekNumber = sheet.findViewById(R.id.tvAccWeekNumber);
        TextView tvAccDateFrom = sheet.findViewById(R.id.tvAccDateFrom);
        TextView tvAccDateTo = sheet.findViewById(R.id.tvAccDateTo);

        if (week != null) {
            if (tvAccWeekNumber != null) {
                tvAccWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvAccDateFrom != null && tvAccDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvAccDateFrom.setText("Del:  " + parts[0].trim());
                    tvAccDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] regIds = {R.id.tvAccB1Reg_0, R.id.tvAccB1Reg_1, R.id.tvAccB1Reg_2, R.id.tvAccB1Reg_3, R.id.tvAccB1Reg_4, R.id.tvAccB1Reg_5};
        int[] concIds = {R.id.tvAccB1Conc_0, R.id.tvAccB1Conc_1, R.id.tvAccB1Conc_2, R.id.tvAccB1Conc_3, R.id.tvAccB1Conc_4, R.id.tvAccB1Conc_5};
        int[] compIds = {R.id.tvAccB1Comp_0, R.id.tvAccB1Comp_1, R.id.tvAccB1Comp_2, R.id.tvAccB1Comp_3, R.id.tvAccB1Comp_4, R.id.tvAccB1Comp_5};
        int[] totIds = {R.id.tvAccB1Tot_0, R.id.tvAccB1Tot_1, R.id.tvAccB1Tot_2, R.id.tvAccB1Tot_3, R.id.tvAccB1Tot_4, R.id.tvAccB1Tot_5};
        int[] pctIds = {R.id.tvAccB1Pct_0, R.id.tvAccB1Pct_1, R.id.tvAccB1Pct_2, R.id.tvAccB1Pct_3, R.id.tvAccB1Pct_4, R.id.tvAccB1Pct_5};
        int[] obsIds = {R.id.tvAccB1Obs_0, R.id.tvAccB1Obs_1, R.id.tvAccB1Obs_2, R.id.tvAccB1Obs_3, R.id.tvAccB1Obs_4, R.id.tvAccB1Obs_5};

        int sumReg = 0, sumConc = 0, sumComp = 0, sumTot = 0, sumExp = 0;
        if (week != null && week.getDays() != null) {
            for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                KpiAccountantDayRecord d = week.getDays().get(i);
                TextView tvReg = sheet.findViewById(regIds[i]);
                TextView tvConc = sheet.findViewById(concIds[i]);
                TextView tvComp = sheet.findViewById(compIds[i]);
                TextView tvT = sheet.findViewById(totIds[i]);
                TextView tvP = sheet.findViewById(pctIds[i]);
                TextView tvO = sheet.findViewById(obsIds[i]);

                if (isBlankMode) {
                    if (tvReg != null) tvReg.setText("");
                    if (tvConc != null) tvConc.setText("");
                    if (tvComp != null) tvComp.setText("");
                    if (tvT != null) tvT.setText("");
                    if (tvP != null) tvP.setText("");
                    if (tvO != null) tvO.setText("");
                } else {
                    int r = d.getRegistrosCount();
                    int c = d.getConciliacionesCount();
                    int cp = d.getComprobantesCount();
                    int t = d.getTotalActivities();
                    int p = d.getCompliancePercentage();
                    sumReg += r;
                    sumConc += c;
                    sumComp += cp;
                    sumTot += t;
                    sumExp += d.getDailyExpectedTotal();

                    if (tvReg != null) tvReg.setText(String.valueOf(r));
                    if (tvConc != null) tvConc.setText(String.valueOf(c));
                    if (tvComp != null) tvComp.setText(String.valueOf(cp));
                    if (tvT != null) tvT.setText(String.valueOf(t));
                    if (tvP != null) tvP.setText(p + "%");
                    if (tvO != null) tvO.setText(d.getObservations());
                }
            }
        }

        TextView tvSumReg = sheet.findViewById(R.id.tvAccB1SumReg);
        TextView tvSumConc = sheet.findViewById(R.id.tvAccB1SumConc);
        TextView tvSumComp = sheet.findViewById(R.id.tvAccB1SumComp);
        TextView tvSumTot = sheet.findViewById(R.id.tvAccB1SumTot);
        TextView tvSumPct = sheet.findViewById(R.id.tvAccB1SumPct);

        if (isBlankMode) {
            if (tvSumReg != null) tvSumReg.setText("");
            if (tvSumConc != null) tvSumConc.setText("");
            if (tvSumComp != null) tvSumComp.setText("");
            if (tvSumTot != null) tvSumTot.setText("");
            if (tvSumPct != null) tvSumPct.setText("");
        } else {
            if (tvSumReg != null) tvSumReg.setText(String.valueOf(sumReg));
            if (tvSumConc != null) tvSumConc.setText(String.valueOf(sumConc));
            if (tvSumComp != null) tvSumComp.setText(String.valueOf(sumComp));
            if (tvSumTot != null) tvSumTot.setText(String.valueOf(sumTot));
            int overallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
            if (tvSumPct != null) tvSumPct.setText(overallPct + "%");
        }

        int accOverallPct = 0;
        if (!isBlankMode) {
            accOverallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
        }
        applyStandardLevelHighlight(sheet, accOverallPct,
                R.id.rowAccLevelExcelente, R.id.rowAccLevelMuyBueno, R.id.rowAccLevelBueno,
                R.id.rowAccLevelRegular, R.id.rowAccLevelDeficiente);

        TextView tvObs = sheet.findViewById(R.id.tvAccGeneralObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : (week != null ? week.getGeneralObservations() : ""));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0C. REPORTE OFICIAL SIGD: ASISTENTE ADMINISTRATIVO
    // =========================================================================
    private void renderOfficialAdminAssistantSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_admin_assistant_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvAstCollabName = sheet.findViewById(R.id.tvAstCollabName);
        TextView tvAstCollabRole = sheet.findViewById(R.id.tvAstCollabRole);
        TextView tvAstCollabShift = sheet.findViewById(R.id.tvAstCollabShift);
        TextView tvAstCollabArea = sheet.findViewById(R.id.tvAstCollabArea);
        TextView tvAstCollabSupervisor = sheet.findViewById(R.id.tvAstCollabSupervisor);
        TextView tvAstCollabBoss = sheet.findViewById(R.id.tvAstCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "ASISTENTE ADMINISTRATIVO"));

        String roleName = profile != null ? profile.getRole() : "ASISTENTE ADMINISTRATIVO";
        String areaName = profile != null ? profile.getArea() : "ADMINISTRACIÓN";
        String supervisorName = profile != null ? profile.getSupervisor() : "ADMINISTRADOR GENERAL";
        String bossName = profile != null ? profile.getImmediateBoss() : "GERENCIA GENERAL";

        KpiAdminAssistantStorageManager storage = KpiAdminAssistantStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiAdminAssistantWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        boolean morning = (week == null || week.isMorningShift());
        tvAstCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        tvAstCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        tvAstCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        tvAstCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        tvAstCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        tvAstCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvAstWeekNumber = sheet.findViewById(R.id.tvAstWeekNumber);
        TextView tvAstDateFrom = sheet.findViewById(R.id.tvAstDateFrom);
        TextView tvAstDateTo = sheet.findViewById(R.id.tvAstDateTo);

        if (week != null) {
            if (tvAstWeekNumber != null) {
                tvAstWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvAstDateFrom != null && tvAstDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvAstDateFrom.setText("Del:  " + parts[0].trim());
                    tvAstDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] gestIds = {R.id.tvAstB1Gest_0, R.id.tvAstB1Gest_1, R.id.tvAstB1Gest_2, R.id.tvAstB1Gest_3, R.id.tvAstB1Gest_4, R.id.tvAstB1Gest_5};
        int[] docIds = {R.id.tvAstB1Doc_0, R.id.tvAstB1Doc_1, R.id.tvAstB1Doc_2, R.id.tvAstB1Doc_3, R.id.tvAstB1Doc_4, R.id.tvAstB1Doc_5};
        int[] coordIds = {R.id.tvAstB1Coord_0, R.id.tvAstB1Coord_1, R.id.tvAstB1Coord_2, R.id.tvAstB1Coord_3, R.id.tvAstB1Coord_4, R.id.tvAstB1Coord_5};
        int[] totIds = {R.id.tvAstB1Tot_0, R.id.tvAstB1Tot_1, R.id.tvAstB1Tot_2, R.id.tvAstB1Tot_3, R.id.tvAstB1Tot_4, R.id.tvAstB1Tot_5};
        int[] pctIds = {R.id.tvAstB1Pct_0, R.id.tvAstB1Pct_1, R.id.tvAstB1Pct_2, R.id.tvAstB1Pct_3, R.id.tvAstB1Pct_4, R.id.tvAstB1Pct_5};
        int[] obsIds = {R.id.tvAstB1Obs_0, R.id.tvAstB1Obs_1, R.id.tvAstB1Obs_2, R.id.tvAstB1Obs_3, R.id.tvAstB1Obs_4, R.id.tvAstB1Obs_5};

        int sumGest = 0, sumDoc = 0, sumCoord = 0, sumTot = 0, sumExp = 0;
        if (week != null && week.getDays() != null) {
            for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                KpiAdminAssistantDayRecord d = week.getDays().get(i);
                TextView tvG = sheet.findViewById(gestIds[i]);
                TextView tvD = sheet.findViewById(docIds[i]);
                TextView tvC = sheet.findViewById(coordIds[i]);
                TextView tvT = sheet.findViewById(totIds[i]);
                TextView tvP = sheet.findViewById(pctIds[i]);
                TextView tvO = sheet.findViewById(obsIds[i]);

                if (isBlankMode) {
                    if (tvG != null) tvG.setText("");
                    if (tvD != null) tvD.setText("");
                    if (tvC != null) tvC.setText("");
                    if (tvT != null) tvT.setText("");
                    if (tvP != null) tvP.setText("");
                    if (tvO != null) tvO.setText("");
                } else {
                    int g = d.getGestionesCount();
                    int doc = d.getDocumentosCount();
                    int c = d.getCoordinacionesCount();
                    int t = d.getTotalActivities();
                    int p = d.getCompliancePercentage();
                    sumGest += g;
                    sumDoc += doc;
                    sumCoord += c;
                    sumTot += t;
                    sumExp += d.getDailyExpectedTotal();

                    if (tvG != null) tvG.setText(String.valueOf(g));
                    if (tvD != null) tvD.setText(String.valueOf(doc));
                    if (tvC != null) tvC.setText(String.valueOf(c));
                    if (tvT != null) tvT.setText(String.valueOf(t));
                    if (tvP != null) tvP.setText(p + "%");
                    if (tvO != null) tvO.setText(d.getObservations());
                }
            }
        }

        TextView tvSumG = sheet.findViewById(R.id.tvAstB1SumGest);
        TextView tvSumD = sheet.findViewById(R.id.tvAstB1SumDoc);
        TextView tvSumC = sheet.findViewById(R.id.tvAstB1SumCoord);
        TextView tvSumTot = sheet.findViewById(R.id.tvAstB1SumTot);
        TextView tvSumPct = sheet.findViewById(R.id.tvAstB1SumPct);

        if (isBlankMode) {
            if (tvSumG != null) tvSumG.setText("");
            if (tvSumD != null) tvSumD.setText("");
            if (tvSumC != null) tvSumC.setText("");
            if (tvSumTot != null) tvSumTot.setText("");
            if (tvSumPct != null) tvSumPct.setText("");
        } else {
            if (tvSumG != null) tvSumG.setText(String.valueOf(sumGest));
            if (tvSumD != null) tvSumD.setText(String.valueOf(sumDoc));
            if (tvSumC != null) tvSumC.setText(String.valueOf(sumCoord));
            if (tvSumTot != null) tvSumTot.setText(String.valueOf(sumTot));
            int overallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
            if (tvSumPct != null) tvSumPct.setText(overallPct + "%");
        }

        int astOverallPct = 0;
        if (!isBlankMode) {
            astOverallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
        }
        applyStandardLevelHighlight(sheet, astOverallPct,
                R.id.rowAstLevelExcelente, R.id.rowAstLevelMuyBueno, R.id.rowAstLevelBueno,
                R.id.rowAstLevelRegular, R.id.rowAstLevelDeficiente);

        TextView tvObs = sheet.findViewById(R.id.tvAstGeneralObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : (week != null ? week.getGeneralObservations() : ""));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0D. REPORTE OFICIAL SIGD: FOTÓGRAFO
    // =========================================================================
    private void renderOfficialPhotographerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_photographer_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvCollabName = sheet.findViewById(R.id.tvPhoCollabName);
        TextView tvCollabRole = sheet.findViewById(R.id.tvPhoCollabRole);
        TextView tvCollabShift = sheet.findViewById(R.id.tvPhoCollabShift);
        TextView tvCollabArea = sheet.findViewById(R.id.tvPhoCollabArea);
        TextView tvCollabSupervisor = sheet.findViewById(R.id.tvPhoCollabSupervisor);
        TextView tvCollabBoss = sheet.findViewById(R.id.tvPhoCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "FOTÓGRAFO"));

        String roleName = profile != null ? profile.getRole() : "FOTÓGRAFO";
        String areaName = profile != null ? profile.getArea() : "PRODUCCIÓN AUDIOVISUAL";
        String supervisorName = profile != null ? profile.getSupervisor() : "JEFFERSON CUEVA";
        String bossName = profile != null ? profile.getImmediateBoss() : "GERENCIA GENERAL";

        KpiPhotographerStorageManager storage = KpiPhotographerStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiPhotographerWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        boolean morning = (week == null || week.isMorningShift());
        if (tvCollabName != null) tvCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        if (tvCollabRole != null) tvCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        if (tvCollabShift != null) tvCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        if (tvCollabArea != null) tvCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        if (tvCollabSupervisor != null) tvCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        if (tvCollabBoss != null) tvCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvWeekNumber = sheet.findViewById(R.id.tvPhoWeekNumber);
        TextView tvDateFrom = sheet.findViewById(R.id.tvPhoDateFrom);
        TextView tvDateTo = sheet.findViewById(R.id.tvPhoDateTo);

        if (week != null) {
            if (tvWeekNumber != null) {
                tvWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvDateFrom != null && tvDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvDateFrom.setText("Del:  " + parts[0].trim());
                    tvDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] sesIds = {R.id.tvPhoB1Ses_0, R.id.tvPhoB1Ses_1, R.id.tvPhoB1Ses_2, R.id.tvPhoB1Ses_3, R.id.tvPhoB1Ses_4, R.id.tvPhoB1Ses_5};
        int[] capIds = {R.id.tvPhoB1Cap_0, R.id.tvPhoB1Cap_1, R.id.tvPhoB1Cap_2, R.id.tvPhoB1Cap_3, R.id.tvPhoB1Cap_4, R.id.tvPhoB1Cap_5};
        int[] editIds = {R.id.tvPhoB1Edit_0, R.id.tvPhoB1Edit_1, R.id.tvPhoB1Edit_2, R.id.tvPhoB1Edit_3, R.id.tvPhoB1Edit_4, R.id.tvPhoB1Edit_5};
        int[] totIds = {R.id.tvPhoB1Tot_0, R.id.tvPhoB1Tot_1, R.id.tvPhoB1Tot_2, R.id.tvPhoB1Tot_3, R.id.tvPhoB1Tot_4, R.id.tvPhoB1Tot_5};
        int[] pctIds = {R.id.tvPhoB1Pct_0, R.id.tvPhoB1Pct_1, R.id.tvPhoB1Pct_2, R.id.tvPhoB1Pct_3, R.id.tvPhoB1Pct_4, R.id.tvPhoB1Pct_5};
        int[] obsIds = {R.id.tvPhoB1Obs_0, R.id.tvPhoB1Obs_1, R.id.tvPhoB1Obs_2, R.id.tvPhoB1Obs_3, R.id.tvPhoB1Obs_4, R.id.tvPhoB1Obs_5};

        int sumSes = 0, sumCap = 0, sumEdit = 0, sumTot = 0, sumExp = 0;
        List<KpiPhotographerDayRecord> days = week != null ? week.getDays() : null;

        for (int i = 0; i < 6; i++) {
            TextView tvS = sheet.findViewById(sesIds[i]);
            TextView tvC = sheet.findViewById(capIds[i]);
            TextView tvE = sheet.findViewById(editIds[i]);
            TextView tvT = sheet.findViewById(totIds[i]);
            TextView tvP = sheet.findViewById(pctIds[i]);
            TextView tvO = sheet.findViewById(obsIds[i]);

            if (isBlankMode) {
                if (tvS != null) tvS.setText("—");
                if (tvC != null) tvC.setText("—");
                if (tvE != null) tvE.setText("—");
                if (tvT != null) tvT.setText("—");
                if (tvP != null) tvP.setText("—");
                if (tvO != null) tvO.setText("");
            } else {
                if (days != null && i < days.size()) {
                    KpiPhotographerDayRecord d = days.get(i);
                    int s = d.getSesionesCount();
                    int c = d.getFotosCapturadasCount();
                    int e = d.getFotosEditadasCount();
                    int t = d.getTotalActivities();
                    int p = d.getCompliancePercentage();
                    sumSes += s;
                    sumCap += c;
                    sumEdit += e;
                    sumTot += t;
                    sumExp += d.getDailyExpectedTotal();

                    if (tvS != null) tvS.setText(String.valueOf(s));
                    if (tvC != null) tvC.setText(String.valueOf(c));
                    if (tvE != null) tvE.setText(String.valueOf(e));
                    if (tvT != null) tvT.setText(String.valueOf(t));
                    if (tvP != null) tvP.setText(p + "%");
                    if (tvO != null) tvO.setText(d.getObservations());
                }
            }
        }

        TextView tvSumSes = sheet.findViewById(R.id.tvPhoB1SumSes);
        TextView tvSumCap = sheet.findViewById(R.id.tvPhoB1SumCap);
        TextView tvSumEdit = sheet.findViewById(R.id.tvPhoB1SumEdit);
        TextView tvSumTot = sheet.findViewById(R.id.tvPhoB1SumTot);
        TextView tvSumPct = sheet.findViewById(R.id.tvPhoB1SumPct);

        if (isBlankMode) {
            if (tvSumSes != null) tvSumSes.setText("");
            if (tvSumCap != null) tvSumCap.setText("");
            if (tvSumEdit != null) tvSumEdit.setText("");
            if (tvSumTot != null) tvSumTot.setText("");
            if (tvSumPct != null) tvSumPct.setText("");
        } else {
            if (tvSumSes != null) tvSumSes.setText(String.valueOf(sumSes));
            if (tvSumCap != null) tvSumCap.setText(String.valueOf(sumCap));
            if (tvSumEdit != null) tvSumEdit.setText(String.valueOf(sumEdit));
            if (tvSumTot != null) tvSumTot.setText(String.valueOf(sumTot));
            int overallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
            if (tvSumPct != null) tvSumPct.setText(overallPct + "%");
        }

        TextView tvB2SesRes = sheet.findViewById(R.id.tvPhoB2SesRes);
        TextView tvB2SesPct = sheet.findViewById(R.id.tvPhoB2SesPct);
        TextView tvB2CapRes = sheet.findViewById(R.id.tvPhoB2CapRes);
        TextView tvB2CapPct = sheet.findViewById(R.id.tvPhoB2CapPct);
        TextView tvB2EditRes = sheet.findViewById(R.id.tvPhoB2EditRes);
        TextView tvB2EditPct = sheet.findViewById(R.id.tvPhoB2EditPct);
        TextView tvB2EntRes = sheet.findViewById(R.id.tvPhoB2EntRes);
        TextView tvB2EntPct = sheet.findViewById(R.id.tvPhoB2EntPct);
        TextView tvB2TotalPct = sheet.findViewById(R.id.tvPhoB2TotalPct);

        if (isBlankMode) {
            if (tvB2SesRes != null) tvB2SesRes.setText("0");
            if (tvB2SesPct != null) tvB2SesPct.setText("0%");
            if (tvB2CapRes != null) tvB2CapRes.setText("0");
            if (tvB2CapPct != null) tvB2CapPct.setText("0%");
            if (tvB2EditRes != null) tvB2EditRes.setText("0");
            if (tvB2EditPct != null) tvB2EditPct.setText("0%");
            if (tvB2EntRes != null) tvB2EntRes.setText("0");
            if (tvB2EntPct != null) tvB2EntPct.setText("0%");
            if (tvB2TotalPct != null) tvB2TotalPct.setText("0%");
        } else {
            int sPct = Math.min(100, Math.round((sumSes * 100f) / 5));
            int cPct = Math.min(100, Math.round((sumCap * 100f) / 200));
            int ePct = Math.min(100, Math.round((sumEdit * 100f) / 160));
            int entPct = sumTot >= 15 ? 100 : Math.round((sumTot * 100f) / 17);
            int totEvalPct = Math.min(100, Math.round((sPct + cPct + ePct + entPct) / 4f));

            if (tvB2SesRes != null) tvB2SesRes.setText(String.valueOf(sumSes));
            if (tvB2SesPct != null) tvB2SesPct.setText(sPct + "%");
            if (tvB2CapRes != null) tvB2CapRes.setText(String.valueOf(sumCap));
            if (tvB2CapPct != null) tvB2CapPct.setText(cPct + "%");
            if (tvB2EditRes != null) tvB2EditRes.setText(String.valueOf(sumEdit));
            if (tvB2EditPct != null) tvB2EditPct.setText(ePct + "%");
            if (tvB2EntRes != null) tvB2EntRes.setText(String.valueOf(sumTot >= 15 ? 6 : 5));
            if (tvB2EntPct != null) tvB2EntPct.setText(entPct + "%");
            if (tvB2TotalPct != null) tvB2TotalPct.setText(totEvalPct + "%");
        }

        TextView tvOtherAct = sheet.findViewById(R.id.tvPhoB3OtherAct);
        if (tvOtherAct != null) {
            tvOtherAct.setText(!isBlankMode && week != null && week.isOtherActivitiesChecked()
                    ? "☑ Realización de otras actividades asignadas por jefatura inmediata"
                    : "☐ Realización de otras actividades asignadas por jefatura inmediata");
        }

        View rowExc = sheet.findViewById(R.id.rowPhoLevelExcelente);
        View rowMuy = sheet.findViewById(R.id.rowPhoLevelMuyBueno);
        View rowBue = sheet.findViewById(R.id.rowPhoLevelBueno);
        View rowReg = sheet.findViewById(R.id.rowPhoLevelRegular);
        View rowDef = sheet.findViewById(R.id.rowPhoLevelDeficiente);

        if (rowExc != null) rowExc.setBackgroundColor(Color.WHITE);
        if (rowMuy != null) rowMuy.setBackgroundColor(Color.WHITE);
        if (rowBue != null) rowBue.setBackgroundColor(Color.WHITE);
        if (rowReg != null) rowReg.setBackgroundColor(Color.WHITE);
        if (rowDef != null) rowDef.setBackgroundColor(Color.WHITE);

        if (!isBlankMode) {
            int score = sumTot >= 16 ? 100 : (sumTot * 100 / 17);
            if (score >= 90) {
                if (rowExc != null) rowExc.setBackgroundColor(Color.parseColor("#DCFCE7"));
            } else if (score >= 80) {
                if (rowMuy != null) rowMuy.setBackgroundColor(Color.parseColor("#DBEAFE"));
            } else if (score >= 60) {
                if (rowBue != null) rowBue.setBackgroundColor(Color.parseColor("#FEF9C3"));
            } else if (score >= 40) {
                if (rowReg != null) rowReg.setBackgroundColor(Color.parseColor("#FFEDD5"));
            } else {
                if (rowDef != null) rowDef.setBackgroundColor(Color.parseColor("#FEE2E2"));
            }
        }

        TextView tvObs = sheet.findViewById(R.id.tvPhoGeneralObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : (week != null ? week.getGeneralObservations() : ""));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0E. REPORTE OFICIAL SIGD: VIDEÓGRAFO
    // =========================================================================
    private void renderOfficialVideographerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_videographer_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvCollabName = sheet.findViewById(R.id.tvVidCollabName);
        TextView tvCollabRole = sheet.findViewById(R.id.tvVidCollabRole);
        TextView tvCollabShift = sheet.findViewById(R.id.tvVidCollabShift);
        TextView tvCollabArea = sheet.findViewById(R.id.tvVidCollabArea);
        TextView tvCollabSupervisor = sheet.findViewById(R.id.tvVidCollabSupervisor);
        TextView tvCollabBoss = sheet.findViewById(R.id.tvVidCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "VIDEÓGRAFO"));

        String roleName = profile != null ? profile.getRole() : "VIDEÓGRAFO";
        String areaName = profile != null ? profile.getArea() : "PRODUCCIÓN AUDIOVISUAL";
        String supervisorName = profile != null ? profile.getSupervisor() : "JEFFERSON CUEVA";
        String bossName = profile != null ? profile.getImmediateBoss() : "GERENCIA GENERAL";

        KpiVideographerStorageManager storage = KpiVideographerStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiVideographerWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        boolean morning = (week == null || week.isMorningShift());
        if (tvCollabName != null) tvCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        if (tvCollabRole != null) tvCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        if (tvCollabShift != null) tvCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        if (tvCollabArea != null) tvCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        if (tvCollabSupervisor != null) tvCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        if (tvCollabBoss != null) tvCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvWeekNumber = sheet.findViewById(R.id.tvVidWeekNumber);
        TextView tvDateFrom = sheet.findViewById(R.id.tvVidDateFrom);
        TextView tvDateTo = sheet.findViewById(R.id.tvVidDateTo);

        if (week != null) {
            if (tvWeekNumber != null) {
                tvWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvDateFrom != null && tvDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvDateFrom.setText("Del:  " + parts[0].trim());
                    tvDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] grabIds = {R.id.tvVidB1Grab_0, R.id.tvVidB1Grab_1, R.id.tvVidB1Grab_2, R.id.tvVidB1Grab_3, R.id.tvVidB1Grab_4, R.id.tvVidB1Grab_5};
        int[] editIds = {R.id.tvVidB1Edit_0, R.id.tvVidB1Edit_1, R.id.tvVidB1Edit_2, R.id.tvVidB1Edit_3, R.id.tvVidB1Edit_4, R.id.tvVidB1Edit_5};
        int[] ajuIds = {R.id.tvVidB1Aju_0, R.id.tvVidB1Aju_1, R.id.tvVidB1Aju_2, R.id.tvVidB1Aju_3, R.id.tvVidB1Aju_4, R.id.tvVidB1Aju_5};
        int[] totIds = {R.id.tvVidB1Tot_0, R.id.tvVidB1Tot_1, R.id.tvVidB1Tot_2, R.id.tvVidB1Tot_3, R.id.tvVidB1Tot_4, R.id.tvVidB1Tot_5};
        int[] pctIds = {R.id.tvVidB1Pct_0, R.id.tvVidB1Pct_1, R.id.tvVidB1Pct_2, R.id.tvVidB1Pct_3, R.id.tvVidB1Pct_4, R.id.tvVidB1Pct_5};
        int[] obsIds = {R.id.tvVidB1Obs_0, R.id.tvVidB1Obs_1, R.id.tvVidB1Obs_2, R.id.tvVidB1Obs_3, R.id.tvVidB1Obs_4, R.id.tvVidB1Obs_5};

        int sumGrab = 0, sumEdit = 0, sumAju = 0, sumTot = 0, sumExp = 0;
        List<KpiVideographerDayRecord> days = week != null ? week.getDays() : null;

        for (int i = 0; i < 6; i++) {
            TextView tvG = sheet.findViewById(grabIds[i]);
            TextView tvE = sheet.findViewById(editIds[i]);
            TextView tvA = sheet.findViewById(ajuIds[i]);
            TextView tvT = sheet.findViewById(totIds[i]);
            TextView tvP = sheet.findViewById(pctIds[i]);
            TextView tvO = sheet.findViewById(obsIds[i]);

            if (isBlankMode) {
                if (tvG != null) tvG.setText("—");
                if (tvE != null) tvE.setText("—");
                if (tvA != null) tvA.setText("—");
                if (tvT != null) tvT.setText("—");
                if (tvP != null) tvP.setText("—");
                if (tvO != null) tvO.setText("");
            } else {
                if (days != null && i < days.size()) {
                    KpiVideographerDayRecord d = days.get(i);
                    int g = d.getVideosGrabadosCount();
                    int e = d.getVideosEditadosCount();
                    int a = d.getAjustesCorreccionesCount();
                    int t = d.getTotalActivities();
                    int p = d.getCompliancePercentage();
                    sumGrab += g;
                    sumEdit += e;
                    sumAju += a;
                    sumTot += t;
                    sumExp += d.getDailyExpectedTotal();

                    if (tvG != null) tvG.setText(String.valueOf(g));
                    if (tvE != null) tvE.setText(String.valueOf(e));
                    if (tvA != null) tvA.setText(String.valueOf(a));
                    if (tvT != null) tvT.setText(String.valueOf(t));
                    if (tvP != null) tvP.setText(p + "%");
                    if (tvO != null) tvO.setText(d.getObservations());
                }
            }
        }

        TextView tvSumGrab = sheet.findViewById(R.id.tvVidB1SumGrab);
        TextView tvSumEdit = sheet.findViewById(R.id.tvVidB1SumEdit);
        TextView tvSumAju = sheet.findViewById(R.id.tvVidB1SumAju);
        TextView tvSumTot = sheet.findViewById(R.id.tvVidB1SumTot);
        TextView tvSumPct = sheet.findViewById(R.id.tvVidB1SumPct);

        if (isBlankMode) {
            if (tvSumGrab != null) tvSumGrab.setText("");
            if (tvSumEdit != null) tvSumEdit.setText("");
            if (tvSumAju != null) tvSumAju.setText("");
            if (tvSumTot != null) tvSumTot.setText("");
            if (tvSumPct != null) tvSumPct.setText("");
        } else {
            if (tvSumGrab != null) tvSumGrab.setText(String.valueOf(sumGrab));
            if (tvSumEdit != null) tvSumEdit.setText(String.valueOf(sumEdit));
            if (tvSumAju != null) tvSumAju.setText(String.valueOf(sumAju));
            if (tvSumTot != null) tvSumTot.setText(String.valueOf(sumTot));
            int overallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
            if (tvSumPct != null) tvSumPct.setText(overallPct + "%");
        }

        TextView tvB2GrabRes = sheet.findViewById(R.id.tvVidB2GrabRes);
        TextView tvB2GrabPct = sheet.findViewById(R.id.tvVidB2GrabPct);
        TextView tvB2EditRes = sheet.findViewById(R.id.tvVidB2EditRes);
        TextView tvB2EditPct = sheet.findViewById(R.id.tvVidB2EditPct);
        TextView tvB2AjuRes = sheet.findViewById(R.id.tvVidB2AjuRes);
        TextView tvB2AjuPct = sheet.findViewById(R.id.tvVidB2AjuPct);
        TextView tvB2EntRes = sheet.findViewById(R.id.tvVidB2EntRes);
        TextView tvB2EntPct = sheet.findViewById(R.id.tvVidB2EntPct);
        TextView tvB2TotalPct = sheet.findViewById(R.id.tvVidB2TotalPct);

        if (isBlankMode) {
            if (tvB2GrabRes != null) tvB2GrabRes.setText("0");
            if (tvB2GrabPct != null) tvB2GrabPct.setText("0%");
            if (tvB2EditRes != null) tvB2EditRes.setText("0");
            if (tvB2EditPct != null) tvB2EditPct.setText("0%");
            if (tvB2AjuRes != null) tvB2AjuRes.setText("0");
            if (tvB2AjuPct != null) tvB2AjuPct.setText("0%");
            if (tvB2EntRes != null) tvB2EntRes.setText("0");
            if (tvB2EntPct != null) tvB2EntPct.setText("0%");
            if (tvB2TotalPct != null) tvB2TotalPct.setText("0%");
        } else {
            int gPct = Math.min(100, Math.round((sumGrab * 100f) / 3));
            int ePct = Math.min(100, Math.round((sumEdit * 100f) / 4));
            int aPct = Math.min(100, Math.round((sumAju * 100f) / 3));
            int entPct = sumTot >= 8 ? 100 : Math.round((sumTot * 100f) / 10);
            int totEvalPct = Math.min(100, Math.round((gPct + ePct + aPct + entPct) / 4f));

            if (tvB2GrabRes != null) tvB2GrabRes.setText(String.valueOf(sumGrab));
            if (tvB2GrabPct != null) tvB2GrabPct.setText(gPct + "%");
            if (tvB2EditRes != null) tvB2EditRes.setText(String.valueOf(sumEdit));
            if (tvB2EditPct != null) tvB2EditPct.setText(ePct + "%");
            if (tvB2AjuRes != null) tvB2AjuRes.setText(String.valueOf(sumAju));
            if (tvB2AjuPct != null) tvB2AjuPct.setText(aPct + "%");
            if (tvB2EntRes != null) tvB2EntRes.setText(String.valueOf(sumTot >= 8 ? 3 : 2));
            if (tvB2EntPct != null) tvB2EntPct.setText(entPct + "%");
            if (tvB2TotalPct != null) tvB2TotalPct.setText(totEvalPct + "%");
        }

        TextView tvOtherAct = sheet.findViewById(R.id.tvVidB3OtherAct);
        if (tvOtherAct != null) {
            tvOtherAct.setText(!isBlankMode && week != null && week.isOtherActivitiesChecked()
                    ? "☑ Realización de otras actividades asignadas por jefatura inmediata"
                    : "☐ Realización de otras actividades asignadas por jefatura inmediata");
        }

        View rowExc = sheet.findViewById(R.id.rowVidLevelExcelente);
        View rowMuy = sheet.findViewById(R.id.rowVidLevelMuyBueno);
        View rowBue = sheet.findViewById(R.id.rowVidLevelBueno);
        View rowReg = sheet.findViewById(R.id.rowVidLevelRegular);
        View rowDef = sheet.findViewById(R.id.rowVidLevelDeficiente);

        if (rowExc != null) rowExc.setBackgroundColor(Color.WHITE);
        if (rowMuy != null) rowMuy.setBackgroundColor(Color.WHITE);
        if (rowBue != null) rowBue.setBackgroundColor(Color.WHITE);
        if (rowReg != null) rowReg.setBackgroundColor(Color.WHITE);
        if (rowDef != null) rowDef.setBackgroundColor(Color.WHITE);

        if (!isBlankMode) {
            int score = sumTot >= 10 ? 100 : (sumTot * 100 / 10);
            if (score >= 90) {
                if (rowExc != null) rowExc.setBackgroundColor(Color.parseColor("#DCFCE7"));
            } else if (score >= 80) {
                if (rowMuy != null) rowMuy.setBackgroundColor(Color.parseColor("#DBEAFE"));
            } else if (score >= 60) {
                if (rowBue != null) rowBue.setBackgroundColor(Color.parseColor("#FEF9C3"));
            } else if (score >= 40) {
                if (rowReg != null) rowReg.setBackgroundColor(Color.parseColor("#FFEDD5"));
            } else {
                if (rowDef != null) rowDef.setBackgroundColor(Color.parseColor("#FEE2E2"));
            }
        }

        TextView tvObs = sheet.findViewById(R.id.tvVidGeneralObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : (week != null ? week.getGeneralObservations() : ""));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0F. REPORTE OFICIAL SIGD: TRAFFICKER DIGITAL
    // =========================================================================
    private void renderOfficialTraffickerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_trafficker_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvCollabName = sheet.findViewById(R.id.tvTrafCollabName);
        TextView tvCollabRole = sheet.findViewById(R.id.tvTrafCollabRole);
        TextView tvCollabShift = sheet.findViewById(R.id.tvTrafCollabShift);
        TextView tvCollabArea = sheet.findViewById(R.id.tvTrafCollabArea);
        TextView tvCollabSupervisor = sheet.findViewById(R.id.tvTrafCollabSupervisor);
        TextView tvCollabBoss = sheet.findViewById(R.id.tvTrafCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "TRAFFICKER DIGITAL"));

        String roleName = profile != null ? profile.getRole() : "TRAFFICKER DIGITAL";
        String areaName = profile != null ? profile.getArea() : "MARKETING DIGITAL / PAUTA";
        String supervisorName = profile != null ? profile.getSupervisor() : "JEFFERSON CUEVA";
        String bossName = profile != null ? profile.getImmediateBoss() : "GERENCIA GENERAL";

        KpiTraffickerStorageManager storage = KpiTraffickerStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiTraffickerWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        boolean morning = (week == null || week.isMorningShift());
        if (tvCollabName != null) tvCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        if (tvCollabRole != null) tvCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        if (tvCollabShift != null) tvCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        if (tvCollabArea != null) tvCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        if (tvCollabSupervisor != null) tvCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        if (tvCollabBoss != null) tvCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvWeekNumber = sheet.findViewById(R.id.tvTrafWeekNumber);
        TextView tvDateFrom = sheet.findViewById(R.id.tvTrafDateFrom);
        TextView tvDateTo = sheet.findViewById(R.id.tvTrafDateTo);

        if (week != null) {
            if (tvWeekNumber != null) {
                tvWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvDateFrom != null && tvDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvDateFrom.setText("Del:  " + parts[0].trim());
                    tvDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] camIds = {R.id.tvTrafB1Cam_0, R.id.tvTrafB1Cam_1, R.id.tvTrafB1Cam_2, R.id.tvTrafB1Cam_3, R.id.tvTrafB1Cam_4, R.id.tvTrafB1Cam_5};
        int[] ajuIds = {R.id.tvTrafB1Aju_0, R.id.tvTrafB1Aju_1, R.id.tvTrafB1Aju_2, R.id.tvTrafB1Aju_3, R.id.tvTrafB1Aju_4, R.id.tvTrafB1Aju_5};
        int[] repIds = {R.id.tvTrafB1Rep_0, R.id.tvTrafB1Rep_1, R.id.tvTrafB1Rep_2, R.id.tvTrafB1Rep_3, R.id.tvTrafB1Rep_4, R.id.tvTrafB1Rep_5};
        int[] totIds = {R.id.tvTrafB1Tot_0, R.id.tvTrafB1Tot_1, R.id.tvTrafB1Tot_2, R.id.tvTrafB1Tot_3, R.id.tvTrafB1Tot_4, R.id.tvTrafB1Tot_5};
        int[] pctIds = {R.id.tvTrafB1Pct_0, R.id.tvTrafB1Pct_1, R.id.tvTrafB1Pct_2, R.id.tvTrafB1Pct_3, R.id.tvTrafB1Pct_4, R.id.tvTrafB1Pct_5};
        int[] obsIds = {R.id.tvTrafB1Obs_0, R.id.tvTrafB1Obs_1, R.id.tvTrafB1Obs_2, R.id.tvTrafB1Obs_3, R.id.tvTrafB1Obs_4, R.id.tvTrafB1Obs_5};

        int sumCam = 0, sumAju = 0, sumRep = 0, sumTot = 0, sumExp = 0;
        List<KpiTraffickerDayRecord> days = week != null ? week.getDays() : null;

        for (int i = 0; i < 6; i++) {
            TextView tvC = sheet.findViewById(camIds[i]);
            TextView tvA = sheet.findViewById(ajuIds[i]);
            TextView tvR = sheet.findViewById(repIds[i]);
            TextView tvT = sheet.findViewById(totIds[i]);
            TextView tvP = sheet.findViewById(pctIds[i]);
            TextView tvO = sheet.findViewById(obsIds[i]);

            if (isBlankMode) {
                if (tvC != null) tvC.setText("—");
                if (tvA != null) tvA.setText("—");
                if (tvR != null) tvR.setText("—");
                if (tvT != null) tvT.setText("—");
                if (tvP != null) tvP.setText("—");
                if (tvO != null) tvO.setText("");
            } else {
                if (days != null && i < days.size()) {
                    KpiTraffickerDayRecord d = days.get(i);
                    int c = d.getCampanasCount();
                    int a = d.getAjustesCount();
                    int r = d.getReportesCount();
                    int t = d.getTotalActivities();
                    int p = d.getCompliancePercentage();
                    sumCam += c;
                    sumAju += a;
                    sumRep += r;
                    sumTot += t;
                    sumExp += d.getDailyExpectedTotal();

                    if (tvC != null) tvC.setText(String.valueOf(c));
                    if (tvA != null) tvA.setText(String.valueOf(a));
                    if (tvR != null) tvR.setText(String.valueOf(r));
                    if (tvT != null) tvT.setText(String.valueOf(t));
                    if (tvP != null) tvP.setText(p + "%");
                    if (tvO != null) tvO.setText(d.getObservations());
                }
            }
        }

        TextView tvSumCam = sheet.findViewById(R.id.tvTrafB1SumCam);
        TextView tvSumAju = sheet.findViewById(R.id.tvTrafB1SumAju);
        TextView tvSumRep = sheet.findViewById(R.id.tvTrafB1SumRep);
        TextView tvSumTot = sheet.findViewById(R.id.tvTrafB1SumTot);
        TextView tvSumPct = sheet.findViewById(R.id.tvTrafB1SumPct);

        if (isBlankMode) {
            if (tvSumCam != null) tvSumCam.setText("");
            if (tvSumAju != null) tvSumAju.setText("");
            if (tvSumRep != null) tvSumRep.setText("");
            if (tvSumTot != null) tvSumTot.setText("");
            if (tvSumPct != null) tvSumPct.setText("");
        } else {
            if (tvSumCam != null) tvSumCam.setText(String.valueOf(sumCam));
            if (tvSumAju != null) tvSumAju.setText(String.valueOf(sumAju));
            if (tvSumRep != null) tvSumRep.setText(String.valueOf(sumRep));
            if (tvSumTot != null) tvSumTot.setText(String.valueOf(sumTot));
            int overallPct = sumExp > 0 ? Math.min(100, Math.round((sumTot * 100f) / sumExp)) : 100;
            if (tvSumPct != null) tvSumPct.setText(overallPct + "%");
        }

        TextView tvB2CamRes = sheet.findViewById(R.id.tvTrafB2CamRes);
        TextView tvB2CamPct = sheet.findViewById(R.id.tvTrafB2CamPct);
        TextView tvB2AjuRes = sheet.findViewById(R.id.tvTrafB2AjuRes);
        TextView tvB2AjuPct = sheet.findViewById(R.id.tvTrafB2AjuPct);
        TextView tvB2RepRes = sheet.findViewById(R.id.tvTrafB2RepRes);
        TextView tvB2RepPct = sheet.findViewById(R.id.tvTrafB2RepPct);
        TextView tvB2AbRes = sheet.findViewById(R.id.tvTrafB2AbRes);
        TextView tvB2AbPct = sheet.findViewById(R.id.tvTrafB2AbPct);
        TextView tvB2TotalPct = sheet.findViewById(R.id.tvTrafB2TotalPct);

        if (isBlankMode) {
            if (tvB2CamRes != null) tvB2CamRes.setText("0");
            if (tvB2CamPct != null) tvB2CamPct.setText("0%");
            if (tvB2AjuRes != null) tvB2AjuRes.setText("0");
            if (tvB2AjuPct != null) tvB2AjuPct.setText("0%");
            if (tvB2RepRes != null) tvB2RepRes.setText("0");
            if (tvB2RepPct != null) tvB2RepPct.setText("0%");
            if (tvB2AbRes != null) tvB2AbRes.setText("0");
            if (tvB2AbPct != null) tvB2AbPct.setText("0%");
            if (tvB2TotalPct != null) tvB2TotalPct.setText("0%");
        } else {
            int cPct = Math.min(100, Math.round((sumCam * 100f) / 2));
            int aPct = Math.min(100, Math.round((sumAju * 100f) / 5));
            int rPct = Math.min(100, Math.round((sumRep * 100f) / 4));
            int abPct = sumTot >= 9 ? 100 : Math.round((sumTot * 100f) / 11);
            int totEvalPct = Math.min(100, Math.round((cPct + aPct + rPct + abPct) / 4f));

            if (tvB2CamRes != null) tvB2CamRes.setText(String.valueOf(sumCam));
            if (tvB2CamPct != null) tvB2CamPct.setText(cPct + "%");
            if (tvB2AjuRes != null) tvB2AjuRes.setText(String.valueOf(sumAju));
            if (tvB2AjuPct != null) tvB2AjuPct.setText(aPct + "%");
            if (tvB2RepRes != null) tvB2RepRes.setText(String.valueOf(sumRep));
            if (tvB2RepPct != null) tvB2RepPct.setText(rPct + "%");
            if (tvB2AbRes != null) tvB2AbRes.setText(String.valueOf(sumTot >= 9 ? 1 : 0));
            if (tvB2AbPct != null) tvB2AbPct.setText(abPct + "%");
            if (tvB2TotalPct != null) tvB2TotalPct.setText(totEvalPct + "%");
        }

        TextView tvOtherAct = sheet.findViewById(R.id.tvTrafB3OtherAct);
        if (tvOtherAct != null) {
            tvOtherAct.setText(!isBlankMode && week != null && week.isOtherActivitiesChecked()
                    ? "☑ Realización de otras actividades asignadas por jefatura inmediata"
                    : "☐ Realización de otras actividades asignadas por jefatura inmediata");
        }

        View rowExc = sheet.findViewById(R.id.rowTrafLevelExcelente);
        View rowMuy = sheet.findViewById(R.id.rowTrafLevelMuyBueno);
        View rowBue = sheet.findViewById(R.id.rowTrafLevelBueno);
        View rowReg = sheet.findViewById(R.id.rowTrafLevelRegular);
        View rowDef = sheet.findViewById(R.id.rowTrafLevelDeficiente);

        if (rowExc != null) rowExc.setBackgroundColor(Color.WHITE);
        if (rowMuy != null) rowMuy.setBackgroundColor(Color.WHITE);
        if (rowBue != null) rowBue.setBackgroundColor(Color.WHITE);
        if (rowReg != null) rowReg.setBackgroundColor(Color.WHITE);
        if (rowDef != null) rowDef.setBackgroundColor(Color.WHITE);

        if (!isBlankMode) {
            int score = sumTot >= 11 ? 100 : (sumTot * 100 / 11);
            if (score >= 90) {
                if (rowExc != null) rowExc.setBackgroundColor(Color.parseColor("#DCFCE7"));
            } else if (score >= 80) {
                if (rowMuy != null) rowMuy.setBackgroundColor(Color.parseColor("#DBEAFE"));
            } else if (score >= 60) {
                if (rowBue != null) rowBue.setBackgroundColor(Color.parseColor("#FEF9C3"));
            } else if (score >= 40) {
                if (rowReg != null) rowReg.setBackgroundColor(Color.parseColor("#FFEDD5"));
            } else {
                if (rowDef != null) rowDef.setBackgroundColor(Color.parseColor("#FEE2E2"));
            }
        }

        TextView tvObs = sheet.findViewById(R.id.tvTrafGeneralObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : (week != null ? week.getGeneralObservations() : ""));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0G. REPORTE OFICIAL SIGD: GERENTE GENERAL (MANUEL CASTILLO)
    // =========================================================================
    private void renderOfficialGeneralManagerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_general_manager_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvCollabName = sheet.findViewById(R.id.tvGmCollabName);
        TextView tvCollabRole = sheet.findViewById(R.id.tvGmCollabRole);
        TextView tvCollabShift = sheet.findViewById(R.id.tvGmCollabShift);
        TextView tvCollabArea = sheet.findViewById(R.id.tvGmCollabArea);
        TextView tvCollabSupervisor = sheet.findViewById(R.id.tvGmCollabSupervisor);
        TextView tvCollabBoss = sheet.findViewById(R.id.tvGmCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "MANUEL CASTILLO"));

        String roleName = profile != null ? profile.getRole() : "GERENTE GENERAL";
        String areaName = profile != null ? profile.getArea() : "GERENCIA GENERAL / DIRECCIÓN EJECUTIVA";
        String supervisorName = profile != null ? profile.getSupervisor() : "DIRECTORIO";
        String bossName = profile != null ? profile.getImmediateBoss() : "JUNTA DIRECTIVA";

        KpiGeneralManagerStorageManager storage = KpiGeneralManagerStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiGeneralManagerWeekReport week = isWeek37
                ? storage.getWeekById("Semana N.º 37")
                : storage.getCurrentWeek();
        if (week == null) {
            week = storage.getCurrentWeek();
        }

        if (tvCollabName != null) tvCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        if (tvCollabRole != null) tvCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        if (tvCollabShift != null) tvCollabShift.setText("Turno:  ☑ Turno mañana   ☑ Turno tarde");
        if (tvCollabArea != null) tvCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        if (tvCollabSupervisor != null) tvCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        if (tvCollabBoss != null) tvCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvWeekNumber = sheet.findViewById(R.id.tvGmWeekNumber);
        TextView tvDateFrom = sheet.findViewById(R.id.tvGmDateFrom);
        TextView tvDateTo = sheet.findViewById(R.id.tvGmDateTo);

        if (week != null) {
            if (tvWeekNumber != null) {
                tvWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvDateFrom != null && tvDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvDateFrom.setText("Del:  " + parts[0].trim());
                    tvDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] b1TrackIds = {R.id.tvGmB1Track_0, R.id.tvGmB1Track_1, R.id.tvGmB1Track_2, R.id.tvGmB1Track_3, R.id.tvGmB1Track_4, R.id.tvGmB1Track_5};
        int[] b1PctIds = {R.id.tvGmB1Pct_0, R.id.tvGmB1Pct_1, R.id.tvGmB1Pct_2, R.id.tvGmB1Pct_3, R.id.tvGmB1Pct_4, R.id.tvGmB1Pct_5};
        int[] b1ObsIds = {R.id.tvGmB1Obs_0, R.id.tvGmB1Obs_1, R.id.tvGmB1Obs_2, R.id.tvGmB1Obs_3, R.id.tvGmB1Obs_4, R.id.tvGmB1Obs_5};

        int[] b2AreaIds = {R.id.tvGmB2Area_0, R.id.tvGmB2Area_1, R.id.tvGmB2Area_2, R.id.tvGmB2Area_3, R.id.tvGmB2Area_4, R.id.tvGmB2Area_5};
        int[] b2PctIds = {R.id.tvGmB2Pct_0, R.id.tvGmB2Pct_1, R.id.tvGmB2Pct_2, R.id.tvGmB2Pct_3, R.id.tvGmB2Pct_4, R.id.tvGmB2Pct_5};
        int[] b2ObsIds = {R.id.tvGmB2Obs_0, R.id.tvGmB2Obs_1, R.id.tvGmB2Obs_2, R.id.tvGmB2Obs_3, R.id.tvGmB2Obs_4, R.id.tvGmB2Obs_5};
        int[] b2ActIds = {R.id.tvGmB2Act_0, R.id.tvGmB2Act_1, R.id.tvGmB2Act_2, R.id.tvGmB2Act_3, R.id.tvGmB2Act_4, R.id.tvGmB2Act_5};

        List<KpiGeneralManagerDayRecord> days = week != null ? week.getDays() : null;
        int sumOwnPct = 0;
        int sumAreaPct = 0;
        int validDays = 0;

        for (int i = 0; i < 6; i++) {
            TextView tvTrack = sheet.findViewById(b1TrackIds[i]);
            TextView tvPct1 = sheet.findViewById(b1PctIds[i]);
            TextView tvObs1 = sheet.findViewById(b1ObsIds[i]);

            TextView tvArea = sheet.findViewById(b2AreaIds[i]);
            TextView tvPct2 = sheet.findViewById(b2PctIds[i]);
            TextView tvObs2 = sheet.findViewById(b2ObsIds[i]);
            TextView tvAct2 = sheet.findViewById(b2ActIds[i]);

            if (isBlankMode) {
                if (tvTrack != null) tvTrack.setText("");
                if (tvPct1 != null) tvPct1.setText("—");
                if (tvObs1 != null) tvObs1.setText("");
                if (tvArea != null) tvArea.setText("");
                if (tvPct2 != null) tvPct2.setText("—");
                if (tvObs2 != null) tvObs2.setText("");
                if (tvAct2 != null) tvAct2.setText("");
            } else {
                if (days != null && i < days.size()) {
                    KpiGeneralManagerDayRecord d = days.get(i);
                    validDays++;
                    sumOwnPct += d.getOwnCompliancePct();
                    sumAreaPct += d.getAreaCompliancePct();

                    if (tvTrack != null) tvTrack.setText(d.getTrackingPerformed());
                    if (tvPct1 != null) tvPct1.setText(d.getOwnCompliancePct() + "%");
                    if (tvObs1 != null) tvObs1.setText(d.getOwnObservations());

                    if (tvArea != null) tvArea.setText(d.getSupervisedArea());
                    if (tvPct2 != null) tvPct2.setText(d.getAreaCompliancePct() + "%");
                    if (tvObs2 != null) tvObs2.setText(d.getAreaObservations());
                    if (tvAct2 != null) tvAct2.setText(d.getAreaActions());
                }
            }
        }

        TextView tvB1TotalDone = sheet.findViewById(R.id.tvGmB1TotalDone);
        TextView tvB1TotalPct = sheet.findViewById(R.id.tvGmB1TotalPct);
        TextView tvB2AvgPct = sheet.findViewById(R.id.tvGmB2AvgPct);

        if (isBlankMode) {
            if (tvB1TotalDone != null) tvB1TotalDone.setText("");
            if (tvB1TotalPct != null) tvB1TotalPct.setText("");
            if (tvB2AvgPct != null) tvB2AvgPct.setText("");
        } else {
            int avgOwn = validDays > 0 ? (sumOwnPct / validDays) : 100;
            int avgArea = validDays > 0 ? (sumAreaPct / validDays) : 100;
            if (tvB1TotalDone != null) tvB1TotalDone.setText(String.valueOf(validDays));
            if (tvB1TotalPct != null) tvB1TotalPct.setText(avgOwn + "%");
            if (tvB2AvgPct != null) tvB2AvgPct.setText(avgArea + "%");
        }

        TextView tvB3IndRes = sheet.findViewById(R.id.tvGmB3IndRes);
        TextView tvB3IndPct = sheet.findViewById(R.id.tvGmB3IndPct);
        TextView tvB3IssRes = sheet.findViewById(R.id.tvGmB3IssRes);
        TextView tvB3IssPct = sheet.findViewById(R.id.tvGmB3IssPct);
        TextView tvB3AreasRes = sheet.findViewById(R.id.tvGmB3AreasRes);
        TextView tvB3AreasPct = sheet.findViewById(R.id.tvGmB3AreasPct);
        TextView tvB3RescRes = sheet.findViewById(R.id.tvGmB3RescRes);
        TextView tvB3RescPct = sheet.findViewById(R.id.tvGmB3RescPct);
        TextView tvB3SigdRes = sheet.findViewById(R.id.tvGmB3SigdRes);
        TextView tvB3SigdPct = sheet.findViewById(R.id.tvGmB3SigdPct);
        TextView tvB3GrandTotalPct = sheet.findViewById(R.id.tvGmB3GrandTotalPct);

        if (isBlankMode) {
            if (tvB3IndRes != null) tvB3IndRes.setText("0");
            if (tvB3IndPct != null) tvB3IndPct.setText("0%");
            if (tvB3IssRes != null) tvB3IssRes.setText("0");
            if (tvB3IssPct != null) tvB3IssPct.setText("0%");
            if (tvB3AreasRes != null) tvB3AreasRes.setText("0");
            if (tvB3AreasPct != null) tvB3AreasPct.setText("0%");
            if (tvB3RescRes != null) tvB3RescRes.setText("0");
            if (tvB3RescPct != null) tvB3RescPct.setText("0%");
            if (tvB3SigdRes != null) tvB3SigdRes.setText("0");
            if (tvB3SigdPct != null) tvB3SigdPct.setText("0%");
            if (tvB3GrandTotalPct != null) tvB3GrandTotalPct.setText("0%");
        } else {
            if (tvB3IndRes != null) tvB3IndRes.setText(String.valueOf(week != null ? week.getWeeklyIndicatorsReviewed() : 18));
            if (tvB3IndPct != null) tvB3IndPct.setText("100%");
            if (tvB3IssRes != null) tvB3IssRes.setText(String.valueOf(week != null ? week.getWeeklyIssuesResolved() : 11));
            if (tvB3IssPct != null) tvB3IssPct.setText("100%");
            if (tvB3AreasRes != null) tvB3AreasRes.setText("6");
            if (tvB3AreasPct != null) tvB3AreasPct.setText("100%");
            if (tvB3RescRes != null) tvB3RescRes.setText("1");
            if (tvB3RescPct != null) tvB3RescPct.setText("100%");
            if (tvB3SigdRes != null) tvB3SigdRes.setText("1");
            if (tvB3SigdPct != null) tvB3SigdPct.setText("100%");
            if (tvB3GrandTotalPct != null) tvB3GrandTotalPct.setText("100%");
        }

        TextView tvFinalScore = sheet.findViewById(R.id.tvGmB4FinalScore);
        if (tvFinalScore != null) {
            tvFinalScore.setText(isBlankMode ? "0 / 20" : "20.0 / 20");
        }

        TextView tvAch = sheet.findViewById(R.id.tvGmAchievements);
        TextView tvDiff = sheet.findViewById(R.id.tvGmDifficulties);
        TextView tvPlan = sheet.findViewById(R.id.tvGmImprovementPlan);

        if (isBlankMode) {
            if (tvAch != null) tvAch.setText("");
            if (tvDiff != null) tvDiff.setText("");
            if (tvPlan != null) tvPlan.setText("");
        } else {
            if (tvAch != null) tvAch.setText(week != null && !week.getAchievements().isEmpty()
                    ? week.getAchievements()
                    : "• Implementación del nuevo sistema SIGD y cumplimiento del 100% en supervisión semanal de todas las áreas operativas.");
            if (tvDiff != null) tvDiff.setText(week != null && !week.getDifficulties().isEmpty()
                    ? week.getDifficulties()
                    : "• Sobrecarga en solicitudes extraordinarias de clientes en horario vespertino.");
            if (tvPlan != null) tvPlan.setText(week != null && !week.getImprovementPlan().isEmpty()
                    ? week.getImprovementPlan()
                    : "• Automatizar reportes de cierre diario y optimizar la delegación con las jefaturas de área.");
        }

        flReportSheetContainer.addView(sheet);
    }

    private Calendar getWeekDayCalendar(int dayIndex0To5) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff + dayIndex0To5);
        return cal;
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

    // =========================================================================
    // 0I. REPORTE OFICIAL SIGD: PROGRAMADOR JUNIOR (DOBLE TURNO)
    // =========================================================================
    private void renderOfficialProgrammerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_programmer_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "PROGRAMADOR JUNIOR"));

        String roleName = profile != null ? profile.getRole() : "PROGRAMADOR JUNIOR";
        String supervisorName = profile != null ? profile.getSupervisor() : "LIC. JEFFERSON CUEVA";

        View llMorning = sheet.findViewById(R.id.llProgShiftMorningContainer);
        View llAfternoon = sheet.findViewById(R.id.llProgShiftAfternoonContainer);
        View llDivider = sheet.findViewById(R.id.llProgDividerScissor);
        if (llDivider != null) llDivider.setVisibility(View.GONE);

        if (selectedShiftIndex == 0) {
            if (llMorning != null) llMorning.setVisibility(View.VISIBLE);
            if (llAfternoon != null) llAfternoon.setVisibility(View.GONE);
        } else {
            if (llMorning != null) llMorning.setVisibility(View.GONE);
            if (llAfternoon != null) llAfternoon.setVisibility(View.VISIBLE);
        }

        TextView tvSelectedDayLabel = sheet.findViewById(R.id.tvProgSelectedDayLabel);

        TextView tvMFecha = sheet.findViewById(R.id.tvProgM_Fecha);
        TextView tvMPract = sheet.findViewById(R.id.tvProgM_Practicante);
        TextView tvMSuper = sheet.findViewById(R.id.tvProgM_Supervisor);
        TextView tvMTurno = sheet.findViewById(R.id.tvProgM_Turno);

        TextView tvMAsig = sheet.findViewById(R.id.tvProgM_ResAsignadas);
        TextView tvMComp = sheet.findViewById(R.id.tvProgM_ResCompletadas);
        TextView tvMPend = sheet.findViewById(R.id.tvProgM_ResPendientes);
        TextView tvMFunc = sheet.findViewById(R.id.tvProgM_ResFuncionalidades);
        TextView tvMInci = sheet.findViewById(R.id.tvProgM_ResIncidencias);
        TextView tvMCump = sheet.findViewById(R.id.tvProgM_ResCumplimiento);
        TextView tvMScore = sheet.findViewById(R.id.tvProgM_ScorePill);

        TextView tvTFecha = sheet.findViewById(R.id.tvProgT_Fecha);
        TextView tvTPract = sheet.findViewById(R.id.tvProgT_Practicante);
        TextView tvTSuper = sheet.findViewById(R.id.tvProgT_Supervisor);
        TextView tvTTurno = sheet.findViewById(R.id.tvProgT_Turno);

        TextView tvTAsig = sheet.findViewById(R.id.tvProgT_ResAsignadas);
        TextView tvTComp = sheet.findViewById(R.id.tvProgT_ResCompletadas);
        TextView tvTPend = sheet.findViewById(R.id.tvProgT_ResPendientes);
        TextView tvTFunc = sheet.findViewById(R.id.tvProgT_ResFuncionalidades);
        TextView tvTInci = sheet.findViewById(R.id.tvProgT_ResIncidencias);
        TextView tvTCump = sheet.findViewById(R.id.tvProgT_ResCumplimiento);
        TextView tvTScore = sheet.findViewById(R.id.tvProgT_ScorePill);

        final String[] dayLabels = {"Lunes 12/10/2026", "Martes 13/10/2026", "Miércoles 14/10/2026", "Jueves 15/10/2026", "Viernes 16/10/2026", "Sábado 17/10/2026"};
        final int[] btnDayIds = {R.id.btnProgDay0, R.id.btnProgDay1, R.id.btnProgDay2, R.id.btnProgDay3, R.id.btnProgDay4, R.id.btnProgDay5};
        final SharedPreferences progPrefs = requireContext().getSharedPreferences(pe.net.ws.kpiweb.features.kpi.ui.KpiFormFragment.PREF_PROGRAMMER_WEEK, Context.MODE_PRIVATE);
        final SharedPreferences progPrefsFallback = requireContext().getSharedPreferences("kpi_programmer_week_prefs", Context.MODE_PRIVATE);
        final int[] currentDayIdx = {0};

        Runnable updateDayUi = new Runnable() {
            @Override
            public void run() {
                int dIdx = currentDayIdx[0];
                String cleanDate = dayLabels[dIdx].replace("Lunes ", "").replace("Martes ", "").replace("Miércoles ", "").replace("Jueves ", "").replace("Viernes ", "").replace("Sábado ", "");

                if (tvSelectedDayLabel != null) tvSelectedDayLabel.setText(dayLabels[dIdx]);
                if (tvMFecha != null) tvMFecha.setText(cleanDate);
                if (tvTFecha != null) tvTFecha.setText(cleanDate);
                if (tvMPract != null) tvMPract.setText(collabName.toUpperCase(Locale.getDefault()));
                if (tvTPract != null) tvTPract.setText(collabName.toUpperCase(Locale.getDefault()));
                if (tvMSuper != null) tvMSuper.setText(supervisorName);
                if (tvTSuper != null) tvTSuper.setText(supervisorName);
                if (tvMTurno != null) tvMTurno.setText("Mañana (8:00 - 1:30)");
                if (tvTTurno != null) tvTTurno.setText("Tarde (3:30 - 6:30)");

                String mJson = progPrefs.getString("day_" + dIdx + "_shift_0_json", progPrefs.getString("day_" + dIdx + "_json",
                        progPrefsFallback.getString("day_" + dIdx + "_shift_0_json", progPrefsFallback.getString("day_" + dIdx + "_json", null))));
                boolean mSent = progPrefs.getBoolean("day_" + dIdx + "_shift_0_sent", progPrefs.getBoolean("day_" + dIdx + "_sent",
                        progPrefsFallback.getBoolean("day_" + dIdx + "_shift_0_sent", progPrefsFallback.getBoolean("day_" + dIdx + "_sent", false))));
                String aJson = progPrefs.getString("day_" + dIdx + "_shift_1_json", progPrefsFallback.getString("day_" + dIdx + "_shift_1_json", null));
                boolean aSent = progPrefs.getBoolean("day_" + dIdx + "_shift_1_sent", progPrefsFallback.getBoolean("day_" + dIdx + "_shift_1_sent", false));

                // --- TURNO MAÑANA ---
                int mAsig = 0, mComp = 0, mFunc = 0, mBugs = 0;
                org.json.JSONArray mActs = null;
                boolean mHasData = false;
                if (mJson != null) {
                    try {
                        org.json.JSONObject mObj = new org.json.JSONObject(mJson);
                        mActs = mObj.optJSONArray("acts");
                        mAsig = mActs != null ? mActs.length() : 0;
                        if (mActs != null) {
                            for (int k = 0; k < mActs.length(); k++) {
                                org.json.JSONObject a = mActs.getJSONObject(k);
                                String st = a.optString("estado", "");
                                if (st.contains("Complet") || st.contains("100%")) mComp++;
                                String tp = a.optString("tipo", "");
                                if (tp.contains("Func") || tp.contains("Mód")) mFunc++;
                                mBugs += a.optInt("bugs", 0);
                            }
                        }
                        mHasData = true;
                    } catch (Exception ignored) {}
                } else if (mSent) {
                    mHasData = true;
                }
                int mPend = Math.max(0, mAsig - mComp);
                int mPct = mAsig > 0 ? Math.min(100, Math.round((mComp * 100f) / mAsig)) : (mSent ? 100 : 0);
                int mScore = mHasData ? Math.min(20, Math.round((mPct * 20f) / 100f)) : 0;

                if (tvMAsig != null) tvMAsig.setText(String.valueOf(mAsig));
                if (tvMComp != null) tvMComp.setText(String.valueOf(mComp));
                if (tvMPend != null) tvMPend.setText(String.valueOf(mPend));
                if (tvMFunc != null) tvMFunc.setText(String.valueOf(mFunc));
                if (tvMInci != null) tvMInci.setText(String.valueOf(mBugs));
                if (tvMCump != null) tvMCump.setText(mHasData ? (mPct + " %") : "—");
                if (tvMScore != null) tvMScore.setText(mHasData ? String.format(Locale.getDefault(), "NOTA: %02d / 20", mScore) : "NOTA: -- / 20");

                LinearLayout llMActs = sheet.findViewById(R.id.llProgM_ActsContainer);
                populateProgrammerActivityRows(llMActs, mActs, true);
                LinearLayout llMEval = sheet.findViewById(R.id.llProgM_EvalCriteriaContainer);
                populateProgrammerCriteria(llMEval, mScore);
                if (mHasData) {
                    applyStandardLevelHighlight(sheet, mPct, R.id.tvProgM_NivelExc, R.id.tvProgM_NivelMuy, R.id.tvProgM_NivelBue, R.id.tvProgM_NivelReg, R.id.tvProgM_NivelDef);
                } else {
                    applyStandardLevelHighlight(sheet, -1, R.id.tvProgM_NivelExc, R.id.tvProgM_NivelMuy, R.id.tvProgM_NivelBue, R.id.tvProgM_NivelReg, R.id.tvProgM_NivelDef);
                }

                // --- TURNO TARDE ---
                int aAsig = 0, aComp = 0, aFunc = 0, aBugs = 0;
                org.json.JSONArray aActs = null;
                boolean aHasData = false;
                if (aJson != null) {
                    try {
                        org.json.JSONObject aObj = new org.json.JSONObject(aJson);
                        aActs = aObj.optJSONArray("acts");
                        aAsig = aActs != null ? aActs.length() : 0;
                        if (aActs != null) {
                            for (int k = 0; k < aActs.length(); k++) {
                                org.json.JSONObject a = aActs.getJSONObject(k);
                                String st = a.optString("estado", "");
                                if (st.contains("Complet") || st.contains("100%")) aComp++;
                                String tp = a.optString("tipo", "");
                                if (tp.contains("Func") || tp.contains("Mód")) aFunc++;
                                aBugs += a.optInt("bugs", 0);
                            }
                        }
                        aHasData = true;
                    } catch (Exception ignored) {}
                } else if (aSent) {
                    aHasData = true;
                }
                int aPend = Math.max(0, aAsig - aComp);
                int aPct = aAsig > 0 ? Math.min(100, Math.round((aComp * 100f) / aAsig)) : (aSent ? 100 : 0);
                int aScore = aHasData ? Math.min(20, Math.round((aPct * 20f) / 100f)) : 0;

                if (tvTAsig != null) tvTAsig.setText(String.valueOf(aAsig));
                if (tvTComp != null) tvTComp.setText(String.valueOf(aComp));
                if (tvTPend != null) tvTPend.setText(String.valueOf(aPend));
                if (tvTFunc != null) tvTFunc.setText(String.valueOf(aFunc));
                if (tvTInci != null) tvTInci.setText(String.valueOf(aBugs));
                if (tvTCump != null) tvTCump.setText(aHasData ? (aPct + " %") : "—");
                if (tvTScore != null) tvTScore.setText(aHasData ? String.format(Locale.getDefault(), "NOTA: %02d / 20", aScore) : "NOTA: -- / 20");

                LinearLayout llTActs = sheet.findViewById(R.id.llProgT_ActsContainer);
                populateProgrammerActivityRows(llTActs, aActs, false);
                LinearLayout llTEval = sheet.findViewById(R.id.llProgT_EvalCriteriaContainer);
                populateProgrammerCriteria(llTEval, aScore);
                if (aHasData) {
                    applyStandardLevelHighlight(sheet, aPct, R.id.tvProgT_NivelExc, R.id.tvProgT_NivelMuy, R.id.tvProgT_NivelBue, R.id.tvProgT_NivelReg, R.id.tvProgT_NivelDef);
                } else {
                    applyStandardLevelHighlight(sheet, -1, R.id.tvProgT_NivelExc, R.id.tvProgT_NivelMuy, R.id.tvProgT_NivelBue, R.id.tvProgT_NivelReg, R.id.tvProgT_NivelDef);
                }
            }
        };

        updateDayUi.run();

        for (int i = 0; i < btnDayIds.length; i++) {
            final int idx = i;
            TextView btn = sheet.findViewById(btnDayIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    currentDayIdx[0] = idx;
                    for (int j = 0; j < btnDayIds.length; j++) {
                        TextView b = sheet.findViewById(btnDayIds[j]);
                        if (b != null) {
                            b.setBackgroundResource(j == idx ? R.drawable.kpi_bg_btn_electric : R.drawable.kpi_designer_bg_day_unselected);
                            b.setTextColor(j == idx ? Color.WHITE : Color.parseColor("#1E1B4B"));
                        }
                    }
                    updateDayUi.run();
                    if (svReportsContentScroll != null) {
                        svReportsContentScroll.post(() -> svReportsContentScroll.scrollTo(0, 0));
                    }
                });
            }
        }

        flReportSheetContainer.addView(sheet);
        if (svReportsContentScroll != null) {
            svReportsContentScroll.post(() -> svReportsContentScroll.scrollTo(0, 0));
        }
    }

    // =========================================================================
    // 0J. REPORTE OFICIAL SIGD: DISEÑADOR GRÁFICO JUNIOR (DOBLE TURNO)
    // =========================================================================
    private void renderOfficialJuniorDesignerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_junior_designer_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "DISEÑADOR GRÁFICO JUNIOR"));

        String roleName = profile != null ? profile.getRole() : "DISEÑADOR GRÁFICO JUNIOR";
        String supervisorName = profile != null ? profile.getSupervisor() : "LIC. JEFFERSON CUEVA (SUPERVISOR CREATIVO)";

        View llMorning = sheet.findViewById(R.id.llDesShiftMorningContainer);
        View llAfternoon = sheet.findViewById(R.id.llDesShiftAfternoonContainer);
        View llDivider = sheet.findViewById(R.id.llDesDividerScissor);
        if (llDivider != null) llDivider.setVisibility(View.GONE);

        if (selectedShiftIndex == 0) {
            if (llMorning != null) llMorning.setVisibility(View.VISIBLE);
            if (llAfternoon != null) llAfternoon.setVisibility(View.GONE);
        } else {
            if (llMorning != null) llMorning.setVisibility(View.GONE);
            if (llAfternoon != null) llAfternoon.setVisibility(View.VISIBLE);
        }

        TextView tvSelectedDayLabel = sheet.findViewById(R.id.tvDesSelectedDayLabel);

        TextView tvMFecha = sheet.findViewById(R.id.tvDesM_Fecha);
        TextView tvMPract = sheet.findViewById(R.id.tvDesM_Practicante);
        TextView tvMSuper = sheet.findViewById(R.id.tvDesM_Supervisor);
        TextView tvMTurno = sheet.findViewById(R.id.tvDesM_Turno);

        TextView tvMPosts = sheet.findViewById(R.id.tvDesM_ResPosts);
        TextView tvMVideos = sheet.findViewById(R.id.tvDesM_ResVideos);
        TextView tvMTot = sheet.findViewById(R.id.tvDesM_ResTotalPiezas);
        TextView tvMMeta = sheet.findViewById(R.id.tvDesM_ResMeta);
        TextView tvMEnt = sheet.findViewById(R.id.tvDesM_ResEntregadas);
        TextView tvMCorr = sheet.findViewById(R.id.tvDesM_ResCorrecciones);
        TextView tvMCump = sheet.findViewById(R.id.tvDesM_ResCumplimiento);
        TextView tvMScore = sheet.findViewById(R.id.tvDesM_ScorePill);

        TextView tvTFecha = sheet.findViewById(R.id.tvDesT_Fecha);
        TextView tvTPract = sheet.findViewById(R.id.tvDesT_Practicante);
        TextView tvTSuper = sheet.findViewById(R.id.tvDesT_Supervisor);
        TextView tvTTurno = sheet.findViewById(R.id.tvDesT_Turno);

        TextView tvTPosts = sheet.findViewById(R.id.tvDesT_ResPosts);
        TextView tvTVideos = sheet.findViewById(R.id.tvDesT_ResVideos);
        TextView tvTTot = sheet.findViewById(R.id.tvDesT_ResTotalPiezas);
        TextView tvTMeta = sheet.findViewById(R.id.tvDesT_ResMeta);
        TextView tvTEnt = sheet.findViewById(R.id.tvDesT_ResEntregadas);
        TextView tvTCorr = sheet.findViewById(R.id.tvDesT_ResCorrecciones);
        TextView tvTCump = sheet.findViewById(R.id.tvDesT_ResCumplimiento);
        TextView tvTScore = sheet.findViewById(R.id.tvDesT_ScorePill);

        final String[] dayLabels = {"Lunes 12/10/2026", "Martes 13/10/2026", "Miércoles 14/10/2026", "Jueves 15/10/2026", "Viernes 16/10/2026", "Sábado 17/10/2026"};
        final int[] btnDayIds = {R.id.btnDesDay0, R.id.btnDesDay1, R.id.btnDesDay2, R.id.btnDesDay3, R.id.btnDesDay4, R.id.btnDesDay5};
        final int[] currentDayIdx = {0};

        Runnable updateDayUi = new Runnable() {
            @Override
            public void run() {
                int dIdx = currentDayIdx[0];
                String cleanDate = dayLabels[dIdx].replace("Lunes ", "").replace("Martes ", "").replace("Miércoles ", "").replace("Jueves ", "").replace("Viernes ", "").replace("Sábado ", "");

                if (tvSelectedDayLabel != null) tvSelectedDayLabel.setText(dayLabels[dIdx]);
                if (tvMFecha != null) tvMFecha.setText(cleanDate);
                if (tvTFecha != null) tvTFecha.setText(cleanDate);
                if (tvMPract != null) tvMPract.setText(collabName.toUpperCase(Locale.getDefault()));
                if (tvTPract != null) tvTPract.setText(collabName.toUpperCase(Locale.getDefault()));
                if (tvMSuper != null) tvMSuper.setText(supervisorName);
                if (tvTSuper != null) tvTSuper.setText(supervisorName);
                if (tvMTurno != null) tvMTurno.setText("Mañana (8:00 - 1:30)");
                if (tvTTurno != null) tvTTurno.setText("Tarde (3:30 - 6:30)");

                KpiJuniorMemoryRepository memRepo = KpiJuniorMemoryRepository.getInstance(requireContext());
                KpiJuniorDayRecord mRecord = memRepo.getShiftRecord(cleanDate, "MANANA");
                KpiJuniorDayRecord aRecord = memRepo.getShiftRecord(cleanDate, "TARDE");

                // --- TURNO MAÑANA ---
                boolean mHasData = (mRecord != null);
                int mPosts = mHasData ? mRecord.getPostsCompleted() : 0;
                int mVideos = mHasData ? mRecord.getVideosCompleted() : 0;
                int mTot = mHasData ? mRecord.getTotalPieces() : 0;
                int mMeta = mHasData ? mRecord.getTargetPieces() : 0;
                int mEnt = mHasData ? mRecord.getDeliveredPieces() : 0;
                int mCorr = mHasData ? mRecord.getTotalCorrections() : 0;
                int mCump = mHasData ? mRecord.getGoalCompliancePercentage() : 0;
                int mScore = mHasData ? (int) Math.round(mRecord.getFinalGrade()) : 0;

                if (tvMPosts != null) tvMPosts.setText(String.valueOf(mPosts));
                if (tvMVideos != null) tvMVideos.setText(String.valueOf(mVideos));
                if (tvMTot != null) tvMTot.setText(String.valueOf(mTot));
                if (tvMMeta != null) tvMMeta.setText(mHasData ? String.valueOf(mMeta) : "—");
                if (tvMEnt != null) tvMEnt.setText(String.valueOf(mEnt));
                if (tvMCorr != null) tvMCorr.setText(String.valueOf(mCorr));
                if (tvMCump != null) tvMCump.setText(mHasData ? (mCump + " %") : "—");
                if (tvMScore != null) tvMScore.setText(mHasData ? String.format(Locale.getDefault(), "NOTA: %02d / 20", mScore) : "NOTA: -- / 20");

                LinearLayout llMActs = sheet.findViewById(R.id.llDesM_ActsContainer);
                populateJuniorDesignerActivityRows(llMActs, mHasData ? mRecord.getActivities() : null, true);
                LinearLayout llMEval = sheet.findViewById(R.id.llDesM_EvalCriteriaContainer);
                populateJuniorDesignerCriteria(llMEval, mScore);
                if (mHasData) {
                    applyStandardLevelHighlight(sheet, mCump, R.id.tvDesM_NivelExc, R.id.tvDesM_NivelMuy, R.id.tvDesM_NivelBue, R.id.tvDesM_NivelReg, R.id.tvDesM_NivelDef);
                } else {
                    applyStandardLevelHighlight(sheet, -1, R.id.tvDesM_NivelExc, R.id.tvDesM_NivelMuy, R.id.tvDesM_NivelBue, R.id.tvDesM_NivelReg, R.id.tvDesM_NivelDef);
                }

                // --- TURNO TARDE ---
                boolean aHasData = (aRecord != null);
                int aPosts = aHasData ? aRecord.getPostsCompleted() : 0;
                int aVideos = aHasData ? aRecord.getVideosCompleted() : 0;
                int aTot = aHasData ? aRecord.getTotalPieces() : 0;
                int aMeta = aHasData ? aRecord.getTargetPieces() : 0;
                int aEnt = aHasData ? aRecord.getDeliveredPieces() : 0;
                int aCorr = aHasData ? aRecord.getTotalCorrections() : 0;
                int aCump = aHasData ? aRecord.getGoalCompliancePercentage() : 0;
                int aScore = aHasData ? (int) Math.round(aRecord.getFinalGrade()) : 0;

                if (tvTPosts != null) tvTPosts.setText(String.valueOf(aPosts));
                if (tvTVideos != null) tvTVideos.setText(String.valueOf(aVideos));
                if (tvTTot != null) tvTTot.setText(String.valueOf(aTot));
                if (tvTMeta != null) tvTMeta.setText(aHasData ? String.valueOf(aMeta) : "—");
                if (tvTEnt != null) tvTEnt.setText(String.valueOf(aEnt));
                if (tvTCorr != null) tvTCorr.setText(String.valueOf(aCorr));
                if (tvTCump != null) tvTCump.setText(aHasData ? (aCump + " %") : "—");
                if (tvTScore != null) tvTScore.setText(aHasData ? String.format(Locale.getDefault(), "NOTA: %02d / 20", aScore) : "NOTA: -- / 20");

                LinearLayout llTActs = sheet.findViewById(R.id.llDesT_ActsContainer);
                populateJuniorDesignerActivityRows(llTActs, aHasData ? aRecord.getActivities() : null, false);
                LinearLayout llTEval = sheet.findViewById(R.id.llDesT_EvalCriteriaContainer);
                populateJuniorDesignerCriteria(llTEval, aScore);
                if (aHasData) {
                    applyStandardLevelHighlight(sheet, aCump, R.id.tvDesT_NivelExc, R.id.tvDesT_NivelMuy, R.id.tvDesT_NivelBue, R.id.tvDesT_NivelReg, R.id.tvDesT_NivelDef);
                } else {
                    applyStandardLevelHighlight(sheet, -1, R.id.tvDesT_NivelExc, R.id.tvDesT_NivelMuy, R.id.tvDesT_NivelBue, R.id.tvDesT_NivelReg, R.id.tvDesT_NivelDef);
                }
            }
        };

        updateDayUi.run();

        for (int i = 0; i < btnDayIds.length; i++) {
            final int idx = i;
            TextView btn = sheet.findViewById(btnDayIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    currentDayIdx[0] = idx;
                    for (int j = 0; j < btnDayIds.length; j++) {
                        TextView b = sheet.findViewById(btnDayIds[j]);
                        if (b != null) {
                            b.setBackgroundResource(j == idx ? R.drawable.kpi_bg_btn_electric : R.drawable.kpi_designer_bg_day_unselected);
                            b.setTextColor(j == idx ? Color.WHITE : Color.parseColor("#1E1B4B"));
                        }
                    }
                    updateDayUi.run();
                    if (svReportsContentScroll != null) {
                        svReportsContentScroll.post(() -> svReportsContentScroll.scrollTo(0, 0));
                    }
                });
            }
        }

        flReportSheetContainer.addView(sheet);
        if (svReportsContentScroll != null) {
            svReportsContentScroll.post(() -> svReportsContentScroll.scrollTo(0, 0));
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE RENDERIZADO TABLAS SIGD PARA PRACTICANTES / JUNIORS
    // =========================================================================
    private void populateProgrammerActivityRows(LinearLayout container, org.json.JSONArray acts, boolean isMorning) {
        if (container == null || getContext() == null) return;
        container.removeAllViews();
        int count = acts != null ? acts.length() : 0;
        if (count == 0) {
            addActivityPlaceholderRow(container);
        } else {
            for (int i = 0; i < count; i++) {
                org.json.JSONObject obj = acts.optJSONObject(i);
                if (obj == null) continue;
                String n = String.valueOf(i + 1);
                String p = obj.optString("proyecto", "Website+");
                String t = obj.optString("tipo", "Desarrollo");
                String d = obj.optString("descripcion", obj.optString("actividad", "Funcionalidad"));
                String in = obj.optString("horaInicio", "--:--");
                String fi = obj.optString("horaFin", "--:--");
                String st = obj.optString("estado", "Completada");
                String cm = obj.optString("commit", "✓");
                addProgActivityRowView(container, n, p, t, d, in, fi, st, cm);
            }
        }
    }

    private void addActivityPlaceholderRow(LinearLayout container) {
        Context ctx = container.getContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 22)));
        row.setGravity(Gravity.CENTER);
        row.setBackgroundResource(R.drawable.kpi_sigd_cell_data);

        TextView tv = new TextView(ctx);
        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tv.setGravity(Gravity.CENTER);
        tv.setText("— Sin actividades registradas para este turno —");
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 6.5f);
        tv.setTypeface(null, Typeface.ITALIC);
        row.addView(tv);

        container.addView(row);
    }

    private void addProgActivityRowView(LinearLayout container, String n, String p, String t, String d, String in, String fi, String st, String cm) {
        Context ctx = container.getContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 20)));
        row.setWeightSum(100f);

        row.addView(createTableCell(ctx, n, 6f, true, Gravity.CENTER, "#1E1B4B"));
        row.addView(createTableCell(ctx, p, 22f, false, Gravity.CENTER_VERTICAL | Gravity.START, "#0F172A"));
        row.addView(createTableCell(ctx, t, 14f, false, Gravity.CENTER, "#475569"));
        row.addView(createTableCell(ctx, d, 24f, false, Gravity.CENTER_VERTICAL | Gravity.START, "#0F172A"));
        row.addView(createTableCell(ctx, in, 8f, false, Gravity.CENTER, "#475569"));
        row.addView(createTableCell(ctx, fi, 8f, false, Gravity.CENTER, "#475569"));
        int stColor = st.toLowerCase().contains("pend") ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A");
        row.addView(createTableCell(ctx, st, 10f, true, Gravity.CENTER, String.format("#%06X", (0xFFFFFF & stColor))));
        row.addView(createTableCell(ctx, cm, 8f, false, Gravity.CENTER, "#6366F1"));

        container.addView(row);
    }

    private void populateProgrammerCriteria(LinearLayout container, int score) {
        if (container == null || getContext() == null) return;
        container.removeAllViews();
        Context ctx = container.getContext();
        String[] criteria = {
                "1. Desarrollo y arquitectura de módulos",
                "2. Calidad de código y orden en commits",
                "3. Cumplimiento de funcionalidades asignadas",
                "4. Resolución de bugs y proactividad"
        };
        boolean hasScore = score > 0;
        for (String c : criteria) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(ctx, 4), dpToPx(ctx, 2), dpToPx(ctx, 4), dpToPx(ctx, 2));

            TextView tvCheck = new TextView(ctx);
            tvCheck.setText(hasScore ? "[✓] " : "[ ] ");
            tvCheck.setTextColor(hasScore ? Color.parseColor("#16A34A") : Color.parseColor("#94A3B8"));
            tvCheck.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7f);
            tvCheck.setTypeface(null, Typeface.BOLD);
            row.addView(tvCheck);

            TextView tvText = new TextView(ctx);
            tvText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvText.setText(c);
            tvText.setTextColor(hasScore ? Color.parseColor("#1E1B4B") : Color.parseColor("#64748B"));
            tvText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 6.5f);
            row.addView(tvText);

            TextView tvPts = new TextView(ctx);
            tvPts.setText(hasScore ? "5 / 5 pts" : "0 / 5 pts");
            tvPts.setTextColor(hasScore ? Color.parseColor("#4338CA") : Color.parseColor("#94A3B8"));
            tvPts.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 6.5f);
            tvPts.setTypeface(null, hasScore ? Typeface.BOLD : Typeface.NORMAL);
            row.addView(tvPts);

            container.addView(row);
        }
    }

    private void populateJuniorDesignerActivityRows(LinearLayout container, List<pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorActivity> acts, boolean isMorning) {
        if (container == null || getContext() == null) return;
        container.removeAllViews();
        int count = acts != null ? acts.size() : 0;
        if (count == 0) {
            addActivityPlaceholderRow(container);
        } else {
            for (int i = 0; i < count; i++) {
                pe.net.ws.kpiweb.features.kpi.junior.model.KpiJuniorActivity a = acts.get(i);
                if (a == null) continue;
                String n = String.valueOf(i + 1);
                String p = a.getClientProject() != null ? a.getClientProject() : "Website+";
                String t = a.getType() != null ? a.getType().getDisplayName() : "Post";
                String d = a.getActivityPiece() != null ? a.getActivityPiece() : "Pieza gráfica";
                String in = a.getStartTime() != null ? a.getStartTime() : "--:--";
                String fi = a.getEndTime() != null ? a.getEndTime() : "--:--";
                String st = a.getStatus() != null ? a.getStatus().getDisplayName() : "Completada";
                String co = String.valueOf(a.getCorrectionsCount());
                addJuniorDesignerActivityRowView(container, n, p, t, d, in, fi, st, co);
            }
        }
    }

    private void addJuniorDesignerActivityRowView(LinearLayout container, String n, String p, String t, String d, String in, String fi, String st, String co) {
        Context ctx = container.getContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 20)));
        row.setWeightSum(100f);

        row.addView(createTableCell(ctx, n, 6f, true, Gravity.CENTER, "#1E1B4B"));
        row.addView(createTableCell(ctx, p, 24f, false, Gravity.CENTER_VERTICAL | Gravity.START, "#0F172A"));
        row.addView(createTableCell(ctx, t, 14f, false, Gravity.CENTER, "#475569"));
        row.addView(createTableCell(ctx, d, 24f, false, Gravity.CENTER_VERTICAL | Gravity.START, "#0F172A"));
        row.addView(createTableCell(ctx, in, 8f, false, Gravity.CENTER, "#475569"));
        row.addView(createTableCell(ctx, fi, 8f, false, Gravity.CENTER, "#475569"));
        int stColor = st.toLowerCase().contains("pend") ? Color.parseColor("#DC2626") : Color.parseColor("#16A34A");
        row.addView(createTableCell(ctx, st, 10f, true, Gravity.CENTER, String.format("#%06X", (0xFFFFFF & stColor))));
        row.addView(createTableCell(ctx, co, 6f, false, Gravity.CENTER, "#0F172A"));

        container.addView(row);
    }

    private void populateJuniorDesignerCriteria(LinearLayout container, int score) {
        if (container == null || getContext() == null) return;
        container.removeAllViews();
        Context ctx = container.getContext();
        String[] criteria = {
                "1. Composición visual y balance gráfico",
                "2. Aplicación rigurosa de manual de marca",
                "3. Cumplimiento de metas y entregables",
                "4. Cero correcciones críticas y proactividad"
        };
        boolean hasScore = score > 0;
        for (String c : criteria) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(ctx, 4), dpToPx(ctx, 2), dpToPx(ctx, 4), dpToPx(ctx, 2));

            TextView tvCheck = new TextView(ctx);
            tvCheck.setText(hasScore ? "[✓] " : "[ ] ");
            tvCheck.setTextColor(hasScore ? Color.parseColor("#16A34A") : Color.parseColor("#94A3B8"));
            tvCheck.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7f);
            tvCheck.setTypeface(null, Typeface.BOLD);
            row.addView(tvCheck);

            TextView tvText = new TextView(ctx);
            tvText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvText.setText(c);
            tvText.setTextColor(hasScore ? Color.parseColor("#1E1B4B") : Color.parseColor("#64748B"));
            tvText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 6.5f);
            row.addView(tvText);

            TextView tvPts = new TextView(ctx);
            tvPts.setText(hasScore ? "5 / 5 pts" : "0 / 5 pts");
            tvPts.setTextColor(hasScore ? Color.parseColor("#4338CA") : Color.parseColor("#94A3B8"));
            tvPts.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 6.5f);
            tvPts.setTypeface(null, hasScore ? Typeface.BOLD : Typeface.NORMAL);
            row.addView(tvPts);

            container.addView(row);
        }
    }

    private TextView createTableCell(Context ctx, String text, float weight, boolean bold, int gravity, String colorHex) {
        TextView tv = new TextView(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        tv.setLayoutParams(lp);
        tv.setBackgroundResource(R.drawable.kpi_sigd_cell_data);
        tv.setText(text);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 6.5f);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        tv.setGravity(gravity);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setPadding(dpToPx(ctx, 2), 0, dpToPx(ctx, 2), 0);
        return tv;
    }

    private int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    // =========================================================================
    // 0K. REPORTE OFICIAL SIGD: PRACTICANTE / JUNIOR GENÉRICO (ASISTENCIA BD Y RENDIMIENTO)
    // =========================================================================
    private void renderOfficialJuniorGenericSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_junior_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "PRACTICANTE"));

        String roleName = (selectedCargo != null && !selectedCargo.isEmpty()) ? selectedCargo : (profile != null ? profile.getRole() : "PRACTICANTE");
        String supervisorName = profile != null ? profile.getSupervisor() : "LIC. MANUEL CASTILLO (DIRECCIÓN)";

        TextView tvTitle = sheet.findViewById(R.id.tvSheetJrRoleTitle);
        TextView tvWeekNumber = sheet.findViewById(R.id.tvSheetJrWeekNumber);
        TextView tvCollab = sheet.findViewById(R.id.tvSheetJrCollabName);
        TextView tvRole = sheet.findViewById(R.id.tvSheetJrCollabRole);
        TextView tvShift = sheet.findViewById(R.id.tvSheetJrCollabShift);
        TextView tvSupervisor = sheet.findViewById(R.id.tvSheetJrCollabSupervisor);

        if (tvTitle != null) tvTitle.setText(roleName.toUpperCase(Locale.getDefault()) + " — CONTROL DE DOBLE TURNO (SIGD 2026)");
        if (tvWeekNumber != null) {
            String wNum = "37";
            if (selectedWeek != null && selectedWeek.matches(".*\\d+.*")) {
                wNum = selectedWeek.replaceAll("\\D+", "");
            }
            tvWeekNumber.setText(wNum);
        }
        if (tvCollab != null) tvCollab.setText("Practicante:  " + collabName.toUpperCase(Locale.getDefault()));
        if (tvRole != null) tvRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        if (tvShift != null) tvShift.setText("Horarios:  Mañana (08:00 - 13:30) / Tarde (14:00 - 18:40)");
        if (tvSupervisor != null) tvSupervisor.setText("Evaluador:  " + supervisorName.toUpperCase(Locale.getDefault()));

        TextView tvAttendedShifts = sheet.findViewById(R.id.tvSheetJrAttendedShifts);
        TextView tvDeliveredTotal = sheet.findViewById(R.id.tvSheetJrDeliveredTotal);
        TextView tvWeeklyPct = sheet.findViewById(R.id.tvSheetJrWeeklyPct);
        TextView tvFinalScore = sheet.findViewById(R.id.tvSheetJrFinalScore);

        if (isBlankMode) {
            if (tvAttendedShifts != null) tvAttendedShifts.setText("0 / 12 turnos");
            if (tvDeliveredTotal != null) tvDeliveredTotal.setText("0 entregadas");
            if (tvWeeklyPct != null) tvWeeklyPct.setText("0 %");
            if (tvFinalScore != null) tvFinalScore.setText("00 / 20");
        } else {
            if (tvAttendedShifts != null) tvAttendedShifts.setText("11 / 12 turnos");
            if (tvDeliveredTotal != null) tvDeliveredTotal.setText("28 actividades");
            if (tvWeeklyPct != null) tvWeeklyPct.setText("95.8 %");
            if (tvFinalScore != null) tvFinalScore.setText("19.2 / 20");
        }

        LinearLayout llDaysTable = sheet.findViewById(R.id.llJuniorDaysTable);
        if (llDaysTable != null) {
            llDaysTable.removeAllViews();
            String[] days = {"Lunes 12/10", "Martes 13/10", "Miércoles 14/10", "Jueves 15/10", "Viernes 16/10", "Sábado 17/10"};
            String[] mornHours = {"5.5 hrs", "5.5 hrs", "5.5 hrs", "5.5 hrs", "5.5 hrs", "5.0 hrs"};
            String[] aftHours = {"4.5 hrs", "4.5 hrs", "4.5 hrs", "4.5 hrs", "4.5 hrs", "4.0 hrs"};
            String[] scores = {"20", "19", "20", "18", "20", "18"};

            for (int i = 0; i < days.length; i++) {
                View row = inflater.inflate(R.layout.kpi_item_official_junior_day_row, llDaysTable, false);
                TextView tvDay = row.findViewById(R.id.tvRowDayName);
                TextView tvAtt = row.findViewById(R.id.tvRowAttendanceStatus);
                TextView tvMornStat = row.findViewById(R.id.tvRowMorningStatus);
                TextView tvMornH = row.findViewById(R.id.tvRowMorningHours);
                TextView tvAftStat = row.findViewById(R.id.tvRowAfternoonStatus);
                TextView tvAftH = row.findViewById(R.id.tvRowAfternoonHours);
                TextView tvScore = row.findViewById(R.id.tvRowScore);

                if (tvDay != null) tvDay.setText(days[i]);

                if (isBlankMode) {
                    if (tvAtt != null) tvAtt.setText("—");
                    if (tvMornStat != null) tvMornStat.setText("—");
                    if (tvMornH != null) tvMornH.setText("0 hrs");
                    if (tvAftStat != null) tvAftStat.setText("—");
                    if (tvAftH != null) tvAftH.setText("0 hrs");
                    if (tvScore != null) tvScore.setText("—");
                } else {
                    if (tvAtt != null) tvAtt.setText("Presente");
                    if (tvMornStat != null) tvMornStat.setText("Cumplido");
                    if (tvMornH != null) tvMornH.setText(mornHours[i]);
                    if (tvAftStat != null) tvAftStat.setText("Cumplido");
                    if (tvAftH != null) tvAftH.setText(aftHours[i]);
                    if (tvScore != null) tvScore.setText(scores[i]);
                }
                llDaysTable.addView(row);
            }
        }

        TextView tvObs = sheet.findViewById(R.id.tvSheetJrObservations);
        if (tvObs != null) {
            tvObs.setText(isBlankMode ? "" : "Desempeño sobresaliente durante la semana. Cumplió con todas las tareas asignadas y demostró gran proactividad.");
        }

        TextView tvSig = sheet.findViewById(R.id.tvSheetJrSignaturePracticante);
        if (tvSig != null) {
            tvSig.setText("Firma Practicante:\n" + collabName.toUpperCase(Locale.getDefault()));
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 0H. REPORTE OFICIAL SIGD: COMMUNITY MANAGER (NICOL RAMOS)
    // =========================================================================
    private void renderOfficialCommunitySheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_community_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvCommCollabName = sheet.findViewById(R.id.tvCommCollabName);
        TextView tvCommCollabRole = sheet.findViewById(R.id.tvCommCollabRole);
        TextView tvCommCollabShift = sheet.findViewById(R.id.tvCommCollabShift);
        TextView tvCommCollabArea = sheet.findViewById(R.id.tvCommCollabArea);
        TextView tvCommCollabSupervisor = sheet.findViewById(R.id.tvCommCollabSupervisor);
        TextView tvCommCollabBoss = sheet.findViewById(R.id.tvCommCollabBoss);

        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String collabName = profile != null ? profile.getName()
                : ((selectedCollab != null && !selectedCollab.isEmpty())
                    ? selectedCollab
                    : (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+")
                        ? authName : "NICOL RAMOS"));

        String roleName = profile != null ? profile.getRole() : "COMMUNITY MANAGER";
        String areaName = profile != null ? profile.getArea() : "MARKETING DIGITAL";
        String supervisorName = profile != null ? profile.getSupervisor() : "MANUEL CASTILLO";
        String bossName = profile != null ? profile.getImmediateBoss() : "JEFFERSON CUEVA";

        KpiCommunityStorageManager storage = KpiCommunityStorageManager.getInstance(requireContext());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
        KpiCommunityWeekReport week = isWeek37
                ? storage.getWeekReport("Semana N.º 37")
                : storage.getCurrentWeekReport();
        if (week == null) {
            week = storage.getCurrentWeekReport();
        }

        boolean morning = (week == null || week.isMorningShift());
        tvCommCollabName.setText("Colaborador:  " + collabName.toUpperCase(Locale.getDefault()));
        tvCommCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        tvCommCollabShift.setText(morning
                ? "Turno:  ☑ Turno mañana   ☐ Turno tarde"
                : "Turno:  ☐ Turno mañana   ☑ Turno tarde");
        tvCommCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        tvCommCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        tvCommCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        TextView tvCommWeekNumber = sheet.findViewById(R.id.tvCommWeekNumber);
        TextView tvCommDateFrom = sheet.findViewById(R.id.tvCommDateFrom);
        TextView tvCommDateTo = sheet.findViewById(R.id.tvCommDateTo);

        if (week != null) {
            if (tvCommWeekNumber != null) {
                tvCommWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvCommDateFrom != null && tvCommDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange().replace("Del ", "").trim();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvCommDateFrom.setText("Del:  " + parts[0].trim());
                    tvCommDateTo.setText("al:   " + parts[1].trim());
                }
            }
        }

        int[] pubIds = {R.id.tvCommPub1, R.id.tvCommPub2, R.id.tvCommPub3, R.id.tvCommPub4, R.id.tvCommPub5, R.id.tvCommPub6};
        int[] intIds = {R.id.tvCommInt1, R.id.tvCommInt2, R.id.tvCommInt3, R.id.tvCommInt4, R.id.tvCommInt5, R.id.tvCommInt6};
        int[] otrIds = {R.id.tvCommOtr1, R.id.tvCommOtr2, R.id.tvCommOtr3, R.id.tvCommOtr4, R.id.tvCommOtr5, R.id.tvCommOtr6};
        int[] totIds = {R.id.tvCommTot1, R.id.tvCommTot2, R.id.tvCommTot3, R.id.tvCommTot4, R.id.tvCommTot5, R.id.tvCommTot6};
        int[] pctIds = {R.id.tvCommPct1, R.id.tvCommPct2, R.id.tvCommPct3, R.id.tvCommPct4, R.id.tvCommPct5, R.id.tvCommPct6};
        int[] obsIds = {R.id.tvCommObs1, R.id.tvCommObs2, R.id.tvCommObs3, R.id.tvCommObs4, R.id.tvCommObs5, R.id.tvCommObs6};

        TextView tvCommTotalPub = sheet.findViewById(R.id.tvCommTotalPub);
        TextView tvCommTotalInt = sheet.findViewById(R.id.tvCommTotalInt);
        TextView tvCommTotalOtr = sheet.findViewById(R.id.tvCommTotalOtr);
        TextView tvCommGrandTotal = sheet.findViewById(R.id.tvCommGrandTotal);
        TextView tvCommTotalPct = sheet.findViewById(R.id.tvCommTotalPct);

        TextView tvCommResPub = sheet.findViewById(R.id.tvCommResPub);
        TextView tvCommPctPub = sheet.findViewById(R.id.tvCommPctPub);
        TextView tvCommResInt = sheet.findViewById(R.id.tvCommResInt);
        TextView tvCommPctInt = sheet.findViewById(R.id.tvCommPctInt);
        TextView tvCommResMet = sheet.findViewById(R.id.tvCommResMet);
        TextView tvCommPctMet = sheet.findViewById(R.id.tvCommPctMet);
        TextView tvCommResRep = sheet.findViewById(R.id.tvCommResRep);
        TextView tvCommPctRep = sheet.findViewById(R.id.tvCommPctRep);
        TextView tvCommGeneralPct = sheet.findViewById(R.id.tvCommGeneralPct);

        TextView cbCommAction1 = sheet.findViewById(R.id.cbCommAction1);
        TextView cbCommAction2 = sheet.findViewById(R.id.cbCommAction2);
        TextView cbCommAction3 = sheet.findViewById(R.id.cbCommAction3);
        TextView cbCommAction4 = sheet.findViewById(R.id.cbCommAction4);
        TextView cbCommAction5 = sheet.findViewById(R.id.cbCommAction5);
        TextView cbCommAction6 = sheet.findViewById(R.id.cbCommAction6);
        TextView cbCommActionOther = sheet.findViewById(R.id.cbCommActionOther);

        View rowExcelente = sheet.findViewById(R.id.rowCommLevelExcelente);
        View rowMuyBueno = sheet.findViewById(R.id.rowCommLevelMuyBueno);
        View rowBueno = sheet.findViewById(R.id.rowCommLevelBueno);
        View rowRegular = sheet.findViewById(R.id.rowCommLevelRegular);
        View rowDeficiente = sheet.findViewById(R.id.rowCommLevelDeficiente);

        TextView ivCheckExcelente = sheet.findViewById(R.id.ivCommCheckExcelente);
        TextView ivCheckMuyBueno = sheet.findViewById(R.id.ivCommCheckMuyBueno);
        TextView ivCheckBueno = sheet.findViewById(R.id.ivCommCheckBueno);
        TextView ivCheckRegular = sheet.findViewById(R.id.ivCommCheckRegular);
        TextView ivCheckDeficiente = sheet.findViewById(R.id.ivCommCheckDeficiente);

        TextView tvCommGeneralObservations = sheet.findViewById(R.id.tvCommGeneralObservations);

        if (isBlankMode) {
            for (int i = 0; i < 6; i++) {
                ((TextView) sheet.findViewById(pubIds[i])).setText("—");
                ((TextView) sheet.findViewById(intIds[i])).setText("—");
                ((TextView) sheet.findViewById(otrIds[i])).setText("—");
                ((TextView) sheet.findViewById(totIds[i])).setText("—");
                ((TextView) sheet.findViewById(pctIds[i])).setText("—");
                ((TextView) sheet.findViewById(obsIds[i])).setText("");
            }
            tvCommTotalPub.setText("0");
            tvCommTotalInt.setText("0");
            tvCommTotalOtr.setText("0");
            tvCommGrandTotal.setText("0");
            tvCommTotalPct.setText("0%");

            tvCommResPub.setText("0");
            tvCommPctPub.setText("0%");
            tvCommResInt.setText("0");
            tvCommPctInt.setText("0%");
            tvCommResMet.setText("0");
            tvCommPctMet.setText("0%");
            tvCommResRep.setText("0");
            tvCommPctRep.setText("0%");
            tvCommGeneralPct.setText("0%");

            cbCommAction1.setText("☐ Gestionar y mantener activas las RR.SS.");
            cbCommAction2.setText("☐ Programar y publicar contenidos.");
            cbCommAction3.setText("☐ Atender mensajes, comentarios y dudas.");
            cbCommAction4.setText("☐ Seguimiento de métricas (alcance e int.).");
            cbCommAction5.setText("☐ Elaborar el reporte semanal de resultados.");
            cbCommAction6.setText("☐ Coordinar la presencia digital de marca.");
            cbCommActionOther.setText("☐ Otras actividades asignadas.");

            rowExcelente.setBackgroundColor(Color.WHITE);
            rowMuyBueno.setBackgroundColor(Color.WHITE);
            rowBueno.setBackgroundColor(Color.WHITE);
            rowRegular.setBackgroundColor(Color.WHITE);
            rowDeficiente.setBackgroundColor(Color.WHITE);

            if (ivCheckExcelente != null) ivCheckExcelente.setText("⭐ EXCELENTE");
            if (ivCheckMuyBueno != null) ivCheckMuyBueno.setText("⭐ MUY BUENO");
            if (ivCheckBueno != null) ivCheckBueno.setText("⭐ BUENO");
            if (ivCheckRegular != null) ivCheckRegular.setText("⭐ REGULAR");
            if (ivCheckDeficiente != null) ivCheckDeficiente.setText("⭐ DEFICIENTE");

            tvCommGeneralObservations.setText("");
        } else {
            int sumPub = 0, sumInt = 0, sumOtr = 0;
            List<KpiCommunityDayRecord> days = week != null ? week.getDays() : null;

            for (int i = 0; i < 6; i++) {
                TextView tvPub = sheet.findViewById(pubIds[i]);
                TextView tvInt = sheet.findViewById(intIds[i]);
                TextView tvOtr = sheet.findViewById(otrIds[i]);
                TextView tvTot = sheet.findViewById(totIds[i]);
                TextView tvPct = sheet.findViewById(pctIds[i]);
                TextView tvObs = sheet.findViewById(obsIds[i]);

                if (days != null && i < days.size()) {
                    KpiCommunityDayRecord d = days.get(i);
                    if (d.isRegistered() || isWeek37) {
                        tvPub.setText(String.valueOf(d.getPublicacionesCount()));
                        tvInt.setText(String.valueOf(d.getInteraccionesCount()));
                        tvOtr.setText(String.valueOf(d.getOtrasActividadesCount()));
                        tvTot.setText(String.valueOf(d.getTotalActivities()));
                        tvPct.setText(d.getCompliancePercentage() + "%");
                        tvObs.setText(d.getObservations());

                        sumPub += d.getPublicacionesCount();
                        sumInt += d.getInteraccionesCount();
                        sumOtr += d.getOtrasActividadesCount();
                    } else {
                        tvPub.setText("—");
                        tvInt.setText("—");
                        tvOtr.setText("—");
                        tvTot.setText("—");
                        tvPct.setText("—");
                        tvObs.setText("");
                    }
                }
            }

            int grandTot = sumPub + sumInt + sumOtr;
            tvCommTotalPub.setText(String.valueOf(sumPub));
            tvCommTotalInt.setText(String.valueOf(sumInt));
            tvCommTotalOtr.setText(String.valueOf(sumOtr));
            tvCommGrandTotal.setText(String.valueOf(grandTot));

            int pubPct = Math.min(100, Math.round((sumPub * 100f) / 10));
            int intPct = Math.min(100, Math.round((sumInt * 100f) / 50));
            int metPct = grandTot > 0 ? 100 : 0;
            int repPct = grandTot > 0 ? 100 : 0;
            int evalPct = grandTot > 0 ? Math.round((pubPct + intPct + metPct + repPct) / 4f) : 0;

            tvCommTotalPct.setText(evalPct >= 90 ? "100%" : evalPct + "%");

            tvCommResPub.setText(String.valueOf(sumPub));
            tvCommPctPub.setText(pubPct + "%");
            tvCommResInt.setText(String.valueOf(sumInt));
            tvCommPctInt.setText(intPct + "%");
            tvCommResMet.setText(grandTot > 0 ? "1" : "0");
            tvCommPctMet.setText(metPct + "%");
            tvCommResRep.setText(grandTot > 0 ? "1" : "0");
            tvCommPctRep.setText(repPct + "%");
            tvCommGeneralPct.setText(evalPct + "%");

            if (week != null) {
                cbCommAction1.setText(week.isAction1GestionarRedes() ? "☑ Gestionar y mantener activas las RR.SS." : "☐ Gestionar y mantener activas las RR.SS.");
                cbCommAction2.setText(week.isAction2ProgramarContenidos() ? "☑ Programar y publicar contenidos." : "☐ Programar y publicar contenidos.");
                cbCommAction3.setText(week.isAction3AtenderMensajes() ? "☑ Atender mensajes, comentarios y dudas." : "☐ Atender mensajes, comentarios y dudas.");
                cbCommAction4.setText(week.isAction4SeguimientoMetricas() ? "☑ Seguimiento de métricas (alcance e int.)." : "☐ Seguimiento de métricas (alcance e int.).");
                cbCommAction5.setText(week.isAction5ElaborarReporte() ? "☑ Elaborar el reporte semanal de resultados." : "☐ Elaborar el reporte semanal de resultados.");
                cbCommAction6.setText(week.isAction6CoordinarPresencia() ? "☑ Coordinar la presencia digital de marca." : "☐ Coordinar la presencia digital de marca.");
                cbCommActionOther.setText(week.isOtherActivitiesChecked() ? "☑ Otras actividades asignadas." : "☐ Otras actividades asignadas.");
            }

            rowExcelente.setBackgroundColor(Color.WHITE);
            rowMuyBueno.setBackgroundColor(Color.WHITE);
            rowBueno.setBackgroundColor(Color.WHITE);
            rowRegular.setBackgroundColor(Color.WHITE);
            rowDeficiente.setBackgroundColor(Color.WHITE);

            if (ivCheckExcelente != null) ivCheckExcelente.setText("⭐ EXCELENTE");
            if (ivCheckMuyBueno != null) ivCheckMuyBueno.setText("⭐ MUY BUENO");
            if (ivCheckBueno != null) ivCheckBueno.setText("⭐ BUENO");
            if (ivCheckRegular != null) ivCheckRegular.setText("⭐ REGULAR");
            if (ivCheckDeficiente != null) ivCheckDeficiente.setText("⭐ DEFICIENTE");

            if (grandTot >= 62 || evalPct >= 90) {
                rowExcelente.setBackgroundColor(Color.parseColor("#DCFCE7"));
                if (ivCheckExcelente != null) ivCheckExcelente.setText("✔ ⭐ EXCELENTE");
            } else if (grandTot >= 52 || evalPct >= 80) {
                rowMuyBueno.setBackgroundColor(Color.parseColor("#DBEAFE"));
                if (ivCheckMuyBueno != null) ivCheckMuyBueno.setText("✔ ⭐ MUY BUENO");
            } else if (grandTot >= 39 || evalPct >= 60) {
                rowBueno.setBackgroundColor(Color.parseColor("#FEF9C3"));
                if (ivCheckBueno != null) ivCheckBueno.setText("✔ ⭐ BUENO");
            } else if (grandTot >= 26 || evalPct >= 40) {
                rowRegular.setBackgroundColor(Color.parseColor("#FFEDD5"));
                if (ivCheckRegular != null) ivCheckRegular.setText("✔ ⭐ REGULAR");
            } else {
                rowDeficiente.setBackgroundColor(Color.parseColor("#FEE2E2"));
                if (ivCheckDeficiente != null) ivCheckDeficiente.setText("✔ ⭐ DEFICIENTE");
            }

            if (week != null && !week.getGeneralObservations().isEmpty()) {
                tvCommGeneralObservations.setText(week.getGeneralObservations());
            } else {
                tvCommGeneralObservations.setText("Se cumplió con la gestión de redes sociales, la publicación de " + sumPub + " contenidos, la atención de " + sumInt + " interacciones y la entrega del reporte semanal, alcanzando un " + evalPct + "% de cumplimiento general.");
            }
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 1. REPORTE OFICIAL SIGD: DISEÑADOR GRÁFICO SENIOR (JEFFERSON CUEVA)
    // =========================================================================
    private void renderOfficialSeniorSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_senior_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvSheetCollabName = sheet.findViewById(R.id.tvSheetCollabName);
        TextView tvSheetCollabRole = sheet.findViewById(R.id.tvSheetCollabRole);
        TextView tvSheetCollabShift = sheet.findViewById(R.id.tvSheetCollabShift);
        TextView tvSheetCollabArea = sheet.findViewById(R.id.tvSheetCollabArea);
        TextView tvSheetCollabSupervisor = sheet.findViewById(R.id.tvSheetCollabSupervisor);
        TextView tvSheetBoss = sheet.findViewById(R.id.tvSheetCollabBoss);

        String collabName = profile != null ? profile.getName() : ((selectedCollab != null && !selectedCollab.isEmpty()) ? selectedCollab : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName());
        String roleName = profile != null ? profile.getRole() : selectedCargo;
        String shiftName = profile != null ? profile.getShift() : "Mañana (08:00 a.m. - 01:30 p.m.)";
        String areaName = profile != null ? profile.getArea() : "Team Creativo";
        String supervisorName = profile != null ? profile.getSupervisor() : "Licenciado / Dirección";
        String bossName = profile != null ? profile.getImmediateBoss() : "Gerencia General";

        boolean isPracticante = profile != null ? profile.isPracticante() : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String prefix = isPracticante ? "Practicante:  " : "Colaborador:  ";
        tvSheetCollabName.setText(prefix + collabName.toUpperCase(Locale.getDefault()));
        tvSheetCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()) + " (Jefatura Área Creativa)");
        tvSheetCollabShift.setText("Turno:  " + shiftName);
        tvSheetCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        tvSheetCollabSupervisor.setText("Responsable de evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        tvSheetBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        KpiSeniorStorageManager storage = KpiSeniorStorageManager.getInstance(requireContext());
        KpiSeniorWeekReport week = getSelectedSeniorWeek(storage);

        TextView tvSheetWeekNumber = sheet.findViewById(R.id.tvSheetWeekNumber);
        TextView tvSheetDateFrom = sheet.findViewById(R.id.tvSheetDateFrom);
        TextView tvSheetDateTo = sheet.findViewById(R.id.tvSheetDateTo);
        if (week != null) {
            if (tvSheetWeekNumber != null) {
                tvSheetWeekNumber.setText(String.valueOf(week.getWeekNumber() > 0 ? week.getWeekNumber() : 37));
            }
            if (tvSheetDateFrom != null && tvSheetDateTo != null && week.getDateRange() != null) {
                String range = week.getDateRange();
                if (range.contains(" al ")) {
                    String[] parts = range.split(" al ");
                    tvSheetDateFrom.setText(parts[0].trim());
                    tvSheetDateTo.setText("al  " + parts[1].trim());
                }
            }
        }

        if (btnHubSendReport != null) {
            if (week != null && week.getStatus() != null && week.getStatus().contains("Enviado")) {
                btnHubSendReport.setText("Reporte Enviado ✓");
                btnHubSendReport.setAlpha(0.85f);
            } else {
                btnHubSendReport.setText("Enviar Reporte");
                btnHubSendReport.setAlpha(1.0f);
            }
        }

        int[] designsIds = {R.id.tvB1Designs_0, R.id.tvB1Designs_1, R.id.tvB1Designs_2, R.id.tvB1Designs_3, R.id.tvB1Designs_4, R.id.tvB1Designs_5};
        int[] videosIds = {R.id.tvB1Videos_0, R.id.tvB1Videos_1, R.id.tvB1Videos_2, R.id.tvB1Videos_3, R.id.tvB1Videos_4, R.id.tvB1Videos_5};
        int[] photosIds = {R.id.tvB1Photos_0, R.id.tvB1Photos_1, R.id.tvB1Photos_2, R.id.tvB1Photos_3, R.id.tvB1Photos_4, R.id.tvB1Photos_5};
        int[] othersIds = {R.id.tvB1Others_0, R.id.tvB1Others_1, R.id.tvB1Others_2, R.id.tvB1Others_3, R.id.tvB1Others_4, R.id.tvB1Others_5};
        int[] totalIds = {R.id.tvB1Total_0, R.id.tvB1Total_1, R.id.tvB1Total_2, R.id.tvB1Total_3, R.id.tvB1Total_4, R.id.tvB1Total_5};
        int[] pctIds = {R.id.tvB1Pct_0, R.id.tvB1Pct_1, R.id.tvB1Pct_2, R.id.tvB1Pct_3, R.id.tvB1Pct_4, R.id.tvB1Pct_5};
        int[] obsIds = {R.id.tvB1Obs_0, R.id.tvB1Obs_1, R.id.tvB1Obs_2, R.id.tvB1Obs_3, R.id.tvB1Obs_4, R.id.tvB1Obs_5};

        int[] b2SrIds = {R.id.tvB2Sr_0, R.id.tvB2Sr_1, R.id.tvB2Sr_2, R.id.tvB2Sr_3, R.id.tvB2Sr_4, R.id.tvB2Sr_5};
        int[] b2JrIds = {R.id.tvB2Jr_0, R.id.tvB2Jr_1, R.id.tvB2Jr_2, R.id.tvB2Jr_3, R.id.tvB2Jr_4, R.id.tvB2Jr_5};
        int[] b2PctIds = {R.id.tvB2TeamPct_0, R.id.tvB2TeamPct_1, R.id.tvB2TeamPct_2, R.id.tvB2TeamPct_3, R.id.tvB2TeamPct_4, R.id.tvB2TeamPct_5};
        int[] b2ObsIds = {R.id.tvB2Obs_0, R.id.tvB2Obs_1, R.id.tvB2Obs_2, R.id.tvB2Obs_3, R.id.tvB2Obs_4, R.id.tvB2Obs_5};
        int[] b2ActIds = {R.id.tvB2Act_0, R.id.tvB2Act_1, R.id.tvB2Act_2, R.id.tvB2Act_3, R.id.tvB2Act_4, R.id.tvB2Act_5};

        TextView tvB1TotalWeekly = sheet.findViewById(R.id.tvB1TotalWeekly);
        TextView tvB2AverageWeekly = sheet.findViewById(R.id.tvB2AverageWeekly);

        TextView tvB3DesignsRes = sheet.findViewById(R.id.tvB3DesignsRes);
        TextView tvB3DesignsPct = sheet.findViewById(R.id.tvB3DesignsPct);
        TextView tvB3VideosRes = sheet.findViewById(R.id.tvB3VideosRes);
        TextView tvB3VideosPct = sheet.findViewById(R.id.tvB3VideosPct);
        TextView tvB3PhotosRes = sheet.findViewById(R.id.tvB3PhotosRes);
        TextView tvB3PhotosPct = sheet.findViewById(R.id.tvB3PhotosPct);
        TextView tvB3TotalRes = sheet.findViewById(R.id.tvB3TotalRes);
        TextView tvB3TotalPct = sheet.findViewById(R.id.tvB3TotalPct);

        TextView tvB4QualityRes = sheet.findViewById(R.id.tvB4QualityRes);
        TextView tvB4QualityPond = sheet.findViewById(R.id.tvB4QualityPond);
        TextView tvB4ProdRes = sheet.findViewById(R.id.tvB4ProdRes);
        TextView tvB4ProdPond = sheet.findViewById(R.id.tvB4ProdPond);
        TextView tvB4SupRes = sheet.findViewById(R.id.tvB4SupRes);
        TextView tvB4SupPond = sheet.findViewById(R.id.tvB4SupPond);
        TextView tvB4DelivRes = sheet.findViewById(R.id.tvB4DelivRes);
        TextView tvB4DelivPond = sheet.findViewById(R.id.tvB4DelivPond);
        TextView tvB4OrthoRes = sheet.findViewById(R.id.tvB4OrthoRes);
        TextView tvB4OrthoPond = sheet.findViewById(R.id.tvB4OrthoPond);
        TextView tvB4LeaderRes = sheet.findViewById(R.id.tvB4LeaderRes);
        TextView tvB4LeaderPond = sheet.findViewById(R.id.tvB4LeaderPond);
        TextView tvB4FinalScore = sheet.findViewById(R.id.tvB4FinalScore);

        TextView tvSheetAchievements = sheet.findViewById(R.id.tvSheetAchievements);
        TextView tvSheetDifficulties = sheet.findViewById(R.id.tvSheetDifficulties);
        TextView tvSheetImprovementPlan = sheet.findViewById(R.id.tvSheetImprovementPlan);

        if (isBlankMode) {
            for (int i = 0; i < 6; i++) {
                ((TextView) sheet.findViewById(designsIds[i])).setText("—");
                ((TextView) sheet.findViewById(videosIds[i])).setText("—");
                ((TextView) sheet.findViewById(photosIds[i])).setText("—");
                ((TextView) sheet.findViewById(othersIds[i])).setText("—");
                ((TextView) sheet.findViewById(totalIds[i])).setText("—");
                ((TextView) sheet.findViewById(pctIds[i])).setText("—");
                ((TextView) sheet.findViewById(obsIds[i])).setText("—");

                ((TextView) sheet.findViewById(b2SrIds[i])).setText("—");
                ((TextView) sheet.findViewById(b2JrIds[i])).setText("—");
                ((TextView) sheet.findViewById(b2PctIds[i])).setText("—");
                ((TextView) sheet.findViewById(b2ObsIds[i])).setText("—");
                ((TextView) sheet.findViewById(b2ActIds[i])).setText("—");
            }

            tvB1TotalWeekly.setText("—");
            tvB2AverageWeekly.setText("—");

            tvB3DesignsRes.setText("—");
            tvB3DesignsPct.setText("—");
            tvB3VideosRes.setText("—");
            tvB3VideosPct.setText("—");
            tvB3PhotosRes.setText("—");
            tvB3PhotosPct.setText("—");
            tvB3TotalRes.setText("—");
            tvB3TotalPct.setText("—");

            tvB4QualityRes.setText("—");
            tvB4QualityPond.setText("—");
            tvB4ProdRes.setText("—");
            tvB4ProdPond.setText("—");
            tvB4SupRes.setText("—");
            tvB4SupPond.setText("—");
            tvB4DelivRes.setText("—");
            tvB4DelivPond.setText("—");
            tvB4OrthoRes.setText("—");
            tvB4OrthoPond.setText("—");
            tvB4LeaderRes.setText("—");
            tvB4LeaderPond.setText("—");
            tvB4FinalScore.setText("— %");

            tvSheetAchievements.setText("• \n• ");
            tvSheetDifficulties.setText("• \n• ");
            tvSheetImprovementPlan.setText("• \n• ");

        } else {
            // LECTURA DINÁMICA DE BASE DE DATOS / STORAGE:
            // Solo muestra datos para los días que realmente están registrados
            boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
            int totalDesigns = 0, totalVideos = 0, totalPhotos = 0, totalOthers = 0;
            int totalSum = 0;
            float teamPctSum = 0;
            int teamDaysCount = 0;
            int registeredDaysCount = 0;

            if (week != null && week.getDays() != null && week.getDays().size() >= 6) {
                for (int i = 0; i < 6; i++) {
                    KpiSeniorDayRecord d = week.getDays().get(i);
                    boolean isReg = d.isRegistered() || (isWeek37 && (d.getDesignsCount() + d.getVideosCount()) > 0);

                    if (isReg) {
                        registeredDaysCount++;
                        int designs = d.getDesignsCount();
                        int videos = d.getVideosCount();
                        int photos = d.getPhotoSessionsCount();
                        int others = d.getOtherActivitiesCount();
                        int dayTot = designs + videos + photos + others;
                        int dayPct = d.getDailyCompliancePercentage();

                        totalDesigns += designs;
                        totalVideos += videos;
                        totalPhotos += photos;
                        totalOthers += others;
                        totalSum += dayTot;

                        ((TextView) sheet.findViewById(designsIds[i])).setText(designs > 0 ? String.format(Locale.getDefault(), "%02d", designs) : "0");
                        ((TextView) sheet.findViewById(videosIds[i])).setText(videos > 0 ? String.format(Locale.getDefault(), "%02d", videos) : "0");
                        ((TextView) sheet.findViewById(photosIds[i])).setText(String.valueOf(photos));
                        ((TextView) sheet.findViewById(othersIds[i])).setText(String.valueOf(others));
                        ((TextView) sheet.findViewById(totalIds[i])).setText(String.format(Locale.getDefault(), "%02d", dayTot));
                        ((TextView) sheet.findViewById(pctIds[i])).setText(dayPct + "%");
                        ((TextView) sheet.findViewById(obsIds[i])).setText("N");

                        int sr = d.getSupervisedSeniorCount();
                        int jr = d.getSupervisedJuniorCount();
                        int tpct = d.getTeamCompliancePercentage();
                        teamPctSum += tpct;
                        teamDaysCount++;

                        ((TextView) sheet.findViewById(b2SrIds[i])).setText(String.valueOf(sr));
                        ((TextView) sheet.findViewById(b2JrIds[i])).setText(String.valueOf(jr));
                        ((TextView) sheet.findViewById(b2PctIds[i])).setText(tpct + "%");
                        ((TextView) sheet.findViewById(b2ObsIds[i])).setText(d.getSupervisionObservations().isEmpty() ? "Producción" : d.getSupervisionObservations());
                        ((TextView) sheet.findViewById(b2ActIds[i])).setText(d.getSupervisionActions().isEmpty() ? "Mejora" : d.getSupervisionActions());
                    } else {
                        // Día NO registrado aún -> Guión limpio
                        ((TextView) sheet.findViewById(designsIds[i])).setText("—");
                        ((TextView) sheet.findViewById(videosIds[i])).setText("—");
                        ((TextView) sheet.findViewById(photosIds[i])).setText("—");
                        ((TextView) sheet.findViewById(othersIds[i])).setText("—");
                        ((TextView) sheet.findViewById(totalIds[i])).setText("—");
                        ((TextView) sheet.findViewById(pctIds[i])).setText("—");
                        ((TextView) sheet.findViewById(obsIds[i])).setText("—");

                        ((TextView) sheet.findViewById(b2SrIds[i])).setText("—");
                        ((TextView) sheet.findViewById(b2JrIds[i])).setText("—");
                        ((TextView) sheet.findViewById(b2PctIds[i])).setText("—");
                        ((TextView) sheet.findViewById(b2ObsIds[i])).setText("—");
                        ((TextView) sheet.findViewById(b2ActIds[i])).setText("—");
                    }
                }
            }

            if (registeredDaysCount == 0) {
                tvB1TotalWeekly.setText("—");
                tvB2AverageWeekly.setText("—");
                tvB3DesignsRes.setText("—");
                tvB3DesignsPct.setText("— %");
                tvB3VideosRes.setText("—");
                tvB3VideosPct.setText("— %");
                tvB3PhotosRes.setText("—");
                tvB3PhotosPct.setText("— %");
                tvB3TotalRes.setText("—");
                tvB3TotalPct.setText("— %");

                tvB4QualityRes.setText("—");
                tvB4QualityPond.setText("—");
                tvB4ProdRes.setText("—");
                tvB4ProdPond.setText("—");
                tvB4SupRes.setText("—");
                tvB4SupPond.setText("—");
                tvB4DelivRes.setText("—");
                tvB4DelivPond.setText("—");
                tvB4OrthoRes.setText("—");
                tvB4OrthoPond.setText("—");
                tvB4LeaderRes.setText("—");
                tvB4LeaderPond.setText("—");
                tvB4FinalScore.setText("— %");

                tvSheetAchievements.setText("• Sin registros de producción");
                tvSheetDifficulties.setText("• Pendiente de inicio de jornada");
                tvSheetImprovementPlan.setText("• Registro diario continuo");
            } else {
                tvB1TotalWeekly.setText(String.format(Locale.getDefault(), "%02d", totalSum));
                float avgTeam = teamDaysCount > 0 ? (teamPctSum / teamDaysCount) : 0f;
                tvB2AverageWeekly.setText(String.format(Locale.getDefault(), "%.1f%%", avgTeam));

                tvB3DesignsRes.setText(String.format(Locale.getDefault(), "%02d", totalDesigns));
                float desPct = (totalDesigns * 100f) / 10f;
                tvB3DesignsPct.setText(String.format(Locale.getDefault(), "%.0f%%", desPct));

                tvB3VideosRes.setText(String.format(Locale.getDefault(), "%02d", totalVideos));
                float vidPct = (totalVideos * 100f) / 12f;
                tvB3VideosPct.setText(String.format(Locale.getDefault(), "%.1f%%", vidPct));

                tvB3PhotosRes.setText(String.format(Locale.getDefault(), "%02d", totalPhotos));
                float phoPct = (totalPhotos * 100f) / 2f;
                tvB3PhotosPct.setText(String.format(Locale.getDefault(), "%.0f%%", phoPct));

                tvB3TotalRes.setText(String.format(Locale.getDefault(), "%02d", totalSum));
                float totPct = (totalSum * 100f) / 24f;
                tvB3TotalPct.setText(String.format(Locale.getDefault(), "%.1f%%", totPct));

                tvB4QualityRes.setText("100%");
                tvB4QualityPond.setText("25%");

                float prodScore = Math.min(100f, vidPct > 0 ? vidPct : ((totalSum * 100f) / 24f));
                float prodPond = (25f * prodScore) / 100f;
                tvB4ProdRes.setText(String.format(Locale.getDefault(), "%.1f%%", prodScore));
                tvB4ProdPond.setText(String.format(Locale.getDefault(), "%.2f%%", prodPond));

                tvB4SupRes.setText(String.format(Locale.getDefault(), "%.0f%%", avgTeam > 0 ? avgTeam : 90f));
                float supPond = (20f * (avgTeam > 0 ? avgTeam : 90f)) / 100f;
                tvB4SupPond.setText(String.format(Locale.getDefault(), "%.1f%%", supPond));

                tvB4DelivRes.setText("80%");
                tvB4DelivPond.setText("12%");

                tvB4OrthoRes.setText("95%");
                tvB4OrthoPond.setText("9.5%");

                tvB4LeaderRes.setText("100%");
                tvB4LeaderPond.setText("5%");

                float finalAvg = 25f + prodPond + supPond + 12f + 9.5f + 5f;
                tvB4FinalScore.setText(String.format(Locale.getDefault(), "%.2f%%", finalAvg));

                tvSheetAchievements.setText("• " + registeredDaysCount + " días registrados en la semana\n• " + totalSum + " actividades completadas");
                tvSheetDifficulties.setText("• Ninguna incidencia crítica reportada");
                tvSheetImprovementPlan.setText("• Optimización de tiempos en renderizado");
            }
        }

        flReportSheetContainer.addView(sheet);
    }

    // =========================================================================
    // 2. REPORTE OFICIAL SIGD: DISEÑADOR GRÁFICO
    // =========================================================================
    private void renderOfficialDesignerSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_designer_sheet, flReportSheetContainer, false);

        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(requireContext());
        KpiCollaboratorProfile profile = repo.findByName(selectedCollab);

        TextView tvDgCollabName = sheet.findViewById(R.id.tvDgCollabName);
        TextView tvDgCollabRole = sheet.findViewById(R.id.tvDgCollabRole);
        TextView tvDgCollabShift = sheet.findViewById(R.id.tvDgCollabShift);
        TextView tvDgCollabArea = sheet.findViewById(R.id.tvDgCollabArea);
        TextView tvDgCollabSupervisor = sheet.findViewById(R.id.tvDgCollabSupervisor);
        TextView tvDgCollabBoss = sheet.findViewById(R.id.tvDgCollabBoss);

        String collabName = profile != null ? profile.getName() : ((selectedCollab != null && !selectedCollab.isEmpty()) ? selectedCollab : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName());
        String roleName = "Diseñador Gráfico";
        String areaName = profile != null ? profile.getArea() : "Team Creativo";
        String supervisorName = profile != null ? profile.getSupervisor() : "Licenciado / Dirección";
        String bossName = profile != null ? profile.getImmediateBoss() : "Gerencia General";

        boolean isPracticante = profile != null ? profile.isPracticante() : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String prefix = isPracticante ? "Practicante:  " : "Colaborador:  ";
        tvDgCollabName.setText(prefix + collabName.toUpperCase(Locale.getDefault()));
        tvDgCollabRole.setText("Cargo:  " + roleName.toUpperCase(Locale.getDefault()));
        tvDgCollabShift.setText("Turno:  ☑ Turno mañana    ☐ Turno tarde");
        tvDgCollabArea.setText("Área:  " + areaName.toUpperCase(Locale.getDefault()));
        tvDgCollabSupervisor.setText("Responsable evaluación:  " + supervisorName.toUpperCase(Locale.getDefault()));
        tvDgCollabBoss.setText("Jefe inmediato:  " + bossName.toUpperCase(Locale.getDefault()));

        int[] disIds = {R.id.tvDgDis_0, R.id.tvDgDis_1, R.id.tvDgDis_2, R.id.tvDgDis_3, R.id.tvDgDis_4, R.id.tvDgDis_5};
        int[] vidIds = {R.id.tvDgVid_0, R.id.tvDgVid_1, R.id.tvDgVid_2, R.id.tvDgVid_3, R.id.tvDgVid_4, R.id.tvDgVid_5};
        int[] totIds = {R.id.tvDgTot_0, R.id.tvDgTot_1, R.id.tvDgTot_2, R.id.tvDgTot_3, R.id.tvDgTot_4, R.id.tvDgTot_5};
        int[] pctIds = {R.id.tvDgPct_0, R.id.tvDgPct_1, R.id.tvDgPct_2, R.id.tvDgPct_3, R.id.tvDgPct_4, R.id.tvDgPct_5};
        int[] obsIds = {R.id.tvDgObs_0, R.id.tvDgObs_1, R.id.tvDgObs_2, R.id.tvDgObs_3, R.id.tvDgObs_4, R.id.tvDgObs_5};

        TextView tvDgDis_Total = sheet.findViewById(R.id.tvDgDis_Total);
        TextView tvDgVid_Total = sheet.findViewById(R.id.tvDgVid_Total);
        TextView tvDgTot_Total = sheet.findViewById(R.id.tvDgTot_Total);
        TextView tvDgPct_Total = sheet.findViewById(R.id.tvDgPct_Total);

        TextView tvDgResDesigns = sheet.findViewById(R.id.tvDgResDesigns);
        TextView tvDgPctDesigns = sheet.findViewById(R.id.tvDgPctDesigns);
        TextView tvDgResVideos = sheet.findViewById(R.id.tvDgResVideos);
        TextView tvDgPctVideos = sheet.findViewById(R.id.tvDgPctVideos);
        TextView tvDgResPhotos = sheet.findViewById(R.id.tvDgResPhotos);
        TextView tvDgPctPhotos = sheet.findViewById(R.id.tvDgPctPhotos);
        TextView tvDgResGrandTotal = sheet.findViewById(R.id.tvDgResGrandTotal);
        TextView tvDgPctGrandTotal = sheet.findViewById(R.id.tvDgPctGrandTotal);

        View rowDgLevelBueno = sheet.findViewById(R.id.rowDgLevelBueno);
        TextView tvDgGeneralObservations = sheet.findViewById(R.id.tvDgGeneralObservations);

        if (isBlankMode) {
            for (int i = 0; i < 6; i++) {
                ((TextView) sheet.findViewById(disIds[i])).setText("—");
                ((TextView) sheet.findViewById(vidIds[i])).setText("—");
                ((TextView) sheet.findViewById(totIds[i])).setText("—");
                ((TextView) sheet.findViewById(pctIds[i])).setText("—");
                ((TextView) sheet.findViewById(obsIds[i])).setText("—");
            }

            tvDgDis_Total.setText("—");
            tvDgVid_Total.setText("—");
            tvDgTot_Total.setText("—");
            tvDgPct_Total.setText("—");

            tvDgResDesigns.setText("—");
            tvDgPctDesigns.setText("— %");
            tvDgResVideos.setText("—");
            tvDgPctVideos.setText("— %");
            tvDgResPhotos.setText("—");
            tvDgPctPhotos.setText("— %");
            tvDgResGrandTotal.setText("—");
            tvDgPctGrandTotal.setText("— %");
            tvDgPctGrandTotal.setBackgroundResource(R.drawable.kpi_sheet_cell_data);

            clearDgLevelHighlights(sheet);
            if (tvDgGeneralObservations != null) {
                tvDgGeneralObservations.setText("");
            }
        } else {
            // LECTURA DINÁMICA DE BASE DE DATOS / MEMORY REPOSITORY:
            boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
            int totalDesigns = 0, totalVideos = 0, totalPhotos = 0, totalActivities = 0;
            int registeredDaysCount = 0;

            KpiDesignerMemoryRepository designerRepo = KpiDesignerMemoryRepository.getInstance(requireContext());
            KpiDesignerWeekReport week = isWeek37 ? designerRepo.getWeekReport("Semana 5") : designerRepo.getCurrentWeekReport();
            List<KpiDesignerDayRecord> days = (week != null) ? week.getDays() : null;

            int[] refDis = {6, 1, 7, 7, 6, 1};
            int[] refVid = {0, 0, 0, 0, 0, 0};
            int[] refTot = {6, 1, 7, 7, 6, 1};
            String[] refPct = {"100%", "25%", "100%", "100%", "100%", "100%"};
            String[] refObs = {
                    "Trabajar Posta Canal",
                    "Diseño WhatsApp + 2 carruseles post",
                    "Diseño Caleta",
                    "Campañas My.Cl + Artes Impresiones",
                    "",
                    "Sesión Don Hugo"
            };

            for (int i = 0; i < 6; i++) {
                boolean dayHasData = false;
                int dCount = 0, vCount = 0, totCount = 0;
                String pctStr = "";
                String obsStr = "";

                if (isWeek37) {
                    dayHasData = true;
                    dCount = refDis[i];
                    vCount = refVid[i];
                    totCount = refTot[i];
                    pctStr = refPct[i];
                    obsStr = refObs[i];
                } else if (days != null && i < days.size()) {
                    KpiDesignerDayRecord d = days.get(i);
                    if (d.isBiometricVerified() || d.isLocked() || d.getTotalCompleted() > 0) {
                        dayHasData = true;
                        dCount = d.getDesignsCompleted();
                        vCount = d.getVideosCompleted();
                        totCount = d.getTotalCompleted();
                        pctStr = d.getCompliancePercentage() + "%";
                        obsStr = d.getObservations().isEmpty() ? "" : d.getObservations();
                        totalPhotos += d.getPhotosCompleted();
                    }
                }

                if (dayHasData) {
                    registeredDaysCount++;
                    totalDesigns += dCount;
                    totalVideos += vCount;
                    totalActivities += totCount;

                    ((TextView) sheet.findViewById(disIds[i])).setText(String.valueOf(dCount));
                    ((TextView) sheet.findViewById(vidIds[i])).setText(String.valueOf(vCount));
                    ((TextView) sheet.findViewById(totIds[i])).setText(String.valueOf(totCount));
                    ((TextView) sheet.findViewById(pctIds[i])).setText(pctStr);
                    ((TextView) sheet.findViewById(obsIds[i])).setText(obsStr);
                } else {
                    // DÍA NO REGISTRADO -> "—"
                    ((TextView) sheet.findViewById(disIds[i])).setText("—");
                    ((TextView) sheet.findViewById(vidIds[i])).setText("—");
                    ((TextView) sheet.findViewById(totIds[i])).setText("—");
                    ((TextView) sheet.findViewById(pctIds[i])).setText("—");
                    ((TextView) sheet.findViewById(obsIds[i])).setText("—");
                }
            }

            if (registeredDaysCount == 0) {
                tvDgDis_Total.setText("—");
                tvDgVid_Total.setText("—");
                tvDgTot_Total.setText("—");
                tvDgPct_Total.setText("—");

                tvDgResDesigns.setText("—");
                tvDgPctDesigns.setText("— %");
                tvDgResVideos.setText("—");
                tvDgPctVideos.setText("— %");
                tvDgResPhotos.setText("—");
                tvDgPctPhotos.setText("— %");
                tvDgResGrandTotal.setText("—");
                tvDgPctGrandTotal.setText("— %");
                tvDgPctGrandTotal.setBackgroundResource(R.drawable.kpi_sheet_cell_data);

                clearDgLevelHighlights(sheet);
                if (tvDgGeneralObservations != null) {
                    tvDgGeneralObservations.setText("Pendiente de inicio de registros diarios de producción.");
                }
            } else {
                tvDgDis_Total.setText(String.valueOf(totalDesigns));
                tvDgVid_Total.setText(totalVideos > 0 ? String.valueOf(totalVideos) : "—");
                tvDgTot_Total.setText(String.valueOf(totalActivities));
                tvDgPct_Total.setText(String.format(Locale.getDefault(), "%.1f%%", (totalDesigns * 100f) / 18f));

                tvDgResDesigns.setText(String.valueOf(totalDesigns));
                tvDgPctDesigns.setText(String.format(Locale.getDefault(), "%.1f%%", (totalDesigns * 100f) / 18f));
                tvDgResVideos.setText(totalVideos > 0 ? String.valueOf(totalVideos) : "—");
                tvDgPctVideos.setText(String.format(Locale.getDefault(), "%.0f%%", (totalVideos * 100f) / 6f));
                tvDgResPhotos.setText(String.valueOf(isWeek37 ? 3 : totalPhotos));
                tvDgPctPhotos.setText(isWeek37 ? "300%" : String.format(Locale.getDefault(), "%.0f%%", (totalPhotos * 100f) / 1f));

                int grandTotal = isWeek37 ? 20 : totalActivities;
                tvDgResGrandTotal.setText(String.valueOf(grandTotal));
                int finalPct = Math.round((grandTotal * 100f) / 25f);
                tvDgPctGrandTotal.setText(finalPct + "%");
                tvDgPctGrandTotal.setBackgroundResource(R.drawable.kpi_sheet_cell_yellow);

                clearDgLevelHighlights(sheet);
                if (finalPct >= 95) {
                    sheet.findViewById(R.id.rowDgLevelExcelente).setBackgroundColor(Color.parseColor("#DCFCE7"));
                } else if (finalPct >= 90) {
                    sheet.findViewById(R.id.rowDgLevelMuyBueno).setBackgroundColor(Color.parseColor("#DBEAFE"));
                } else if (finalPct >= 80) {
                    if (rowDgLevelBueno != null) rowDgLevelBueno.setBackgroundColor(Color.parseColor("#FEF08A"));
                } else if (finalPct >= 70) {
                    sheet.findViewById(R.id.rowDgLevelRegular).setBackgroundColor(Color.parseColor("#FFEDD5"));
                } else {
                    sheet.findViewById(R.id.rowDgLevelDeficiente).setBackgroundColor(Color.parseColor("#FEE2E2"));
                }

                if (tvDgGeneralObservations != null) {
                    tvDgGeneralObservations.setText(isWeek37 ?
                            "NO CUMPLE CON LAS METAS DIARIAS ASIGNADOS. CUMPLIR Y RESPETAR LAS METAS DIARIAS ASIGNADAS." :
                            "Avance semanal registrado: " + registeredDaysCount + " de 6 días completados.");
                }
            }
        }

        flReportSheetContainer.addView(sheet);
    }

    private void clearDgLevelHighlights(View sheet) {
        if (sheet == null) return;
        int[] levelRows = {
                R.id.rowDgLevelExcelente,
                R.id.rowDgLevelMuyBueno,
                R.id.rowDgLevelBueno,
                R.id.rowDgLevelRegular,
                R.id.rowDgLevelDeficiente
        };
        for (int id : levelRows) {
            View r = sheet.findViewById(id);
            if (r != null) r.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // =========================================================================
    // 3. REPORTE OFICIAL: ASESOR DE VENTAS (METAS Y HOJA DE RUTA)
    // =========================================================================
    private void renderOfficialSalesSheet(LayoutInflater inflater) {
        View sheet = inflater.inflate(R.layout.kpi_view_official_sales_sheet, flReportSheetContainer, false);

        TextView btnSalesTabKpiGoals = sheet.findViewById(R.id.btnSalesTabKpiGoals);
        TextView btnSalesTabRoute = sheet.findViewById(R.id.btnSalesTabRoute);
        View llSalesContainerKpiGoals = sheet.findViewById(R.id.llSalesContainerKpiGoals);
        View llSalesContainerRoute = sheet.findViewById(R.id.llSalesContainerRoute);

        btnSalesTabKpiGoals.setOnClickListener(v -> {
            llSalesContainerKpiGoals.setVisibility(View.VISIBLE);
            llSalesContainerRoute.setVisibility(View.GONE);
            btnSalesTabKpiGoals.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
            btnSalesTabKpiGoals.setTextColor(Color.WHITE);
            btnSalesTabRoute.setBackgroundResource(android.R.color.transparent);
            btnSalesTabRoute.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        });

        btnSalesTabRoute.setOnClickListener(v -> {
            llSalesContainerKpiGoals.setVisibility(View.GONE);
            llSalesContainerRoute.setVisibility(View.VISIBLE);
            btnSalesTabRoute.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
            btnSalesTabRoute.setTextColor(Color.WHITE);
            btnSalesTabKpiGoals.setBackgroundResource(android.R.color.transparent);
            btnSalesTabKpiGoals.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_secondary));
        });

        TextView tvSalesCollabName = sheet.findViewById(R.id.tvSalesCollabName);
        TextView tvSalesWeekRange = sheet.findViewById(R.id.tvSalesWeekRange);
        TextView tvSalesZoneRoute = sheet.findViewById(R.id.tvSalesZoneRoute);
        TextView tvRouteCollabName = sheet.findViewById(R.id.tvRouteCollabName);
        TextView tvRouteDate = sheet.findViewById(R.id.tvRouteDate);
        TextView tvRouteZone = sheet.findViewById(R.id.tvRouteZone);

        String collab = (selectedCollab != null && !selectedCollab.isEmpty()) ? selectedCollab : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        if (collab == null || collab.isEmpty() || collab.equals("Colaborador WebSite+")) {
            collab = "Asesor Comercial";
        }
        boolean isSalesPracticante = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().isPracticanteMode();
        String salesPrefix = isSalesPracticante ? "PRACTICANTE: " : "EJECUTIVO: ";
        tvSalesCollabName.setText(salesPrefix + collab);

        KpiSalesWeekManager salesWeekMgr = KpiSalesWeekManager.getInstance(requireContext());
        String weekRangeLabel = salesWeekMgr.getActiveWeekRangeLabel();
        if (selectedWeek != null && !selectedWeek.isEmpty() && !selectedWeek.contains("Actual")) {
            weekRangeLabel = selectedWeek;
        }
        tvSalesWeekRange.setText("SEMANA: " + weekRangeLabel.toUpperCase(Locale.getDefault()));
        tvSalesZoneRoute.setText("ZONA / RUTA: Centro / Sin urgencias");
        tvRouteCollabName.setText((isSalesPracticante ? "PRACTICANTE DE VENTAS:  " : "EJECUTIVO DE VENTAS:  ") + collab);
        SimpleDateFormat todayFmt = new SimpleDateFormat("dd / MM / yyyy", Locale.getDefault());
        tvRouteDate.setText("FECHA:  " + todayFmt.format(new Date()));
        tvRouteZone.setText("ZONA / RUTA:  Centro Comercial / Sin urgencias");

        int[] callIds = {R.id.tvSalesCalls_0, R.id.tvSalesCalls_1, R.id.tvSalesCalls_2, R.id.tvSalesCalls_3, R.id.tvSalesCalls_4, R.id.tvSalesCalls_5};
        int[] visitIds = {R.id.tvSalesVisits_0, R.id.tvSalesVisits_1, R.id.tvSalesVisits_2, R.id.tvSalesVisits_3, R.id.tvSalesVisits_4, R.id.tvSalesVisits_5};
        int[] quoteIds = {R.id.tvSalesQuotes_0, R.id.tvSalesQuotes_1, R.id.tvSalesQuotes_2, R.id.tvSalesQuotes_3, R.id.tvSalesQuotes_4, R.id.tvSalesQuotes_5};
        int[] followIds = {R.id.tvSalesFollowups_0, R.id.tvSalesFollowups_1, R.id.tvSalesFollowups_2, R.id.tvSalesFollowups_3, R.id.tvSalesFollowups_4, R.id.tvSalesFollowups_5};
        int[] prospIds = {R.id.tvSalesProspects_0, R.id.tvSalesProspects_1, R.id.tvSalesProspects_2, R.id.tvSalesProspects_3, R.id.tvSalesProspects_4, R.id.tvSalesProspects_5};
        int[] clientIds = {R.id.tvSalesClients_0, R.id.tvSalesClients_1, R.id.tvSalesClients_2, R.id.tvSalesClients_3, R.id.tvSalesClients_4, R.id.tvSalesClients_5};
        int[] obsIds = {R.id.tvSalesObs_0, R.id.tvSalesObs_1, R.id.tvSalesObs_2, R.id.tvSalesObs_3, R.id.tvSalesObs_4, R.id.tvSalesObs_5};

        TextView tvSalesTotCalls = sheet.findViewById(R.id.tvSalesTotCalls);
        TextView tvSalesTotVisits = sheet.findViewById(R.id.tvSalesTotVisits);
        TextView tvSalesTotQuotes = sheet.findViewById(R.id.tvSalesTotQuotes);
        TextView tvSalesTotFollowups = sheet.findViewById(R.id.tvSalesTotFollowups);
        TextView tvSalesTotProspects = sheet.findViewById(R.id.tvSalesTotProspects);
        TextView tvSalesTotClients = sheet.findViewById(R.id.tvSalesTotClients);

        TextView tvSalesCardCallsObt = sheet.findViewById(R.id.tvSalesCardCallsObt);
        TextView tvSalesCardCallsPct = sheet.findViewById(R.id.tvSalesCardCallsPct);
        TextView tvSalesCardVisitsObt = sheet.findViewById(R.id.tvSalesCardVisitsObt);
        TextView tvSalesCardVisitsPct = sheet.findViewById(R.id.tvSalesCardVisitsPct);
        TextView tvSalesCardQuotesObt = sheet.findViewById(R.id.tvSalesCardQuotesObt);
        TextView tvSalesCardQuotesPct = sheet.findViewById(R.id.tvSalesCardQuotesPct);
        TextView tvSalesCardFollowupsObt = sheet.findViewById(R.id.tvSalesCardFollowupsObt);
        TextView tvSalesCardFollowupsPct = sheet.findViewById(R.id.tvSalesCardFollowupsPct);
        TextView tvSalesCardProspectsObt = sheet.findViewById(R.id.tvSalesCardProspectsObt);
        TextView tvSalesCardProspectsPct = sheet.findViewById(R.id.tvSalesCardProspectsPct);
        TextView tvSalesCardClientsObt = sheet.findViewById(R.id.tvSalesCardClientsObt);
        TextView tvSalesCardClientsPct = sheet.findViewById(R.id.tvSalesCardClientsPct);

        LinearLayout llRouteRowsContainer = sheet.findViewById(R.id.llRouteRowsContainer);
        TextView tvRouteTotCalls = sheet.findViewById(R.id.tvRouteTotCalls);
        TextView tvRouteTotVisits = sheet.findViewById(R.id.tvRouteTotVisits);
        TextView tvRouteTotQuotes = sheet.findViewById(R.id.tvRouteTotQuotes);
        TextView tvRouteTotFollowups = sheet.findViewById(R.id.tvRouteTotFollowups);
        TextView tvRouteTotProspects = sheet.findViewById(R.id.tvRouteTotProspects);

        if (isBlankMode) {
            for (int i = 0; i < 6; i++) {
                ((TextView) sheet.findViewById(callIds[i])).setText("—");
                ((TextView) sheet.findViewById(visitIds[i])).setText("—");
                ((TextView) sheet.findViewById(quoteIds[i])).setText("—");
                ((TextView) sheet.findViewById(followIds[i])).setText("—");
                ((TextView) sheet.findViewById(prospIds[i])).setText("—");
                ((TextView) sheet.findViewById(clientIds[i])).setText("—");
                ((TextView) sheet.findViewById(obsIds[i])).setText("—");
            }

            tvSalesTotCalls.setText("—");
            tvSalesTotVisits.setText("—");
            tvSalesTotQuotes.setText("—");
            tvSalesTotFollowups.setText("—");
            tvSalesTotProspects.setText("—");
            tvSalesTotClients.setText("—");

            tvSalesCardCallsObt.setText("—");
            tvSalesCardCallsPct.setText("— %");
            tvSalesCardVisitsObt.setText("—");
            tvSalesCardVisitsPct.setText("— %");
            tvSalesCardQuotesObt.setText("—");
            tvSalesCardQuotesPct.setText("— %");
            tvSalesCardFollowupsObt.setText("—");
            tvSalesCardFollowupsPct.setText("— %");
            tvSalesCardProspectsObt.setText("—");
            tvSalesCardProspectsPct.setText("— %");
            tvSalesCardClientsObt.setText("—");
            tvSalesCardClientsPct.setText("— %");

            tvRouteTotCalls.setText("—");
            tvRouteTotVisits.setText("—");
            tvRouteTotQuotes.setText("—");
            tvRouteTotFollowups.setText("—");
            tvRouteTotProspects.setText("—");

            populateRouteRows(llRouteRowsContainer, true);
        } else {
            // LECTURA DINÁMICA DE BASE DE DATOS LOCAL / KPISALESWEEKMANAGER:
            boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));
            int totalCalls = 0, totalVisits = 0, totalQuotes = 0, totalFollowups = 0, totalProspects = 0, totalClients = 0;
            int savedCount = 0;

            KpiSalesWeekManager salesManager = KpiSalesWeekManager.getInstance(requireContext());
            List<KpiSalesDay> salesDays = salesManager.getDays();

            int[] refCalls = {23, 27, 21, 17, 14, 0};
            int[] refVisits = {0, 5, 0, 1, 13, 8};
            int[] refQuotes = {0, 0, 0, 1, 3, 1};
            int[] refFollows = {4, 6, 2, 1, 5, 3};
            int[] refProspects = {4, 6, 2, 0, 3, 2};
            int[] refClients = {0, 0, 0, 0, 0, 0};

            for (int i = 0; i < 6; i++) {
                boolean dayHasData = false;
                int c = 0, v = 0, q = 0, f = 0, p = 0, cl = 0;
                String obs = "";

                if (isWeek37) {
                    dayHasData = true;
                    c = refCalls[i];
                    v = refVisits[i];
                    q = refQuotes[i];
                    f = refFollows[i];
                    p = refProspects[i];
                    cl = refClients[i];
                    obs = "✓";
                } else if (salesDays != null && i < salesDays.size()) {
                    KpiSalesDay d = salesDays.get(i);
                    if (d.isSaved()) {
                        dayHasData = true;
                        c = d.getLlamadas();
                        v = d.getVisitas();
                        q = d.getCotizaciones();
                        f = d.getSeguimiento();
                        p = d.getProspectos();
                        cl = d.getClientes();
                        obs = d.getObservaciones().isEmpty() ? "✓" : d.getObservaciones();
                    }
                }

                if (dayHasData) {
                    savedCount++;
                    totalCalls += c;
                    totalVisits += v;
                    totalQuotes += q;
                    totalFollowups += f;
                    totalProspects += p;
                    totalClients += cl;

                    ((TextView) sheet.findViewById(callIds[i])).setText(String.valueOf(c));
                    ((TextView) sheet.findViewById(visitIds[i])).setText(String.valueOf(v));
                    ((TextView) sheet.findViewById(quoteIds[i])).setText(String.valueOf(q));
                    ((TextView) sheet.findViewById(followIds[i])).setText(String.valueOf(f));
                    ((TextView) sheet.findViewById(prospIds[i])).setText(String.valueOf(p));
                    ((TextView) sheet.findViewById(clientIds[i])).setText(String.valueOf(cl));
                    ((TextView) sheet.findViewById(obsIds[i])).setText(obs);
                } else {
                    // DÍA NO REGISTRADO -> "—"
                    ((TextView) sheet.findViewById(callIds[i])).setText("—");
                    ((TextView) sheet.findViewById(visitIds[i])).setText("—");
                    ((TextView) sheet.findViewById(quoteIds[i])).setText("—");
                    ((TextView) sheet.findViewById(followIds[i])).setText("—");
                    ((TextView) sheet.findViewById(prospIds[i])).setText("—");
                    ((TextView) sheet.findViewById(clientIds[i])).setText("—");
                    ((TextView) sheet.findViewById(obsIds[i])).setText("—");
                }
            }

            if (savedCount == 0) {
                tvSalesTotCalls.setText("—");
                tvSalesTotVisits.setText("—");
                tvSalesTotQuotes.setText("—");
                tvSalesTotFollowups.setText("—");
                tvSalesTotProspects.setText("—");
                tvSalesTotClients.setText("—");

                tvSalesCardCallsObt.setText("—");
                tvSalesCardCallsPct.setText("— %");
                tvSalesCardVisitsObt.setText("—");
                tvSalesCardVisitsPct.setText("— %");
                tvSalesCardQuotesObt.setText("—");
                tvSalesCardQuotesPct.setText("— %");
                tvSalesCardFollowupsObt.setText("—");
                tvSalesCardFollowupsPct.setText("— %");
                tvSalesCardProspectsObt.setText("—");
                tvSalesCardProspectsPct.setText("— %");
                tvSalesCardClientsObt.setText("—");
                tvSalesCardClientsPct.setText("— %");

                tvRouteTotCalls.setText("—");
                tvRouteTotVisits.setText("—");
                tvRouteTotQuotes.setText("—");
                tvRouteTotFollowups.setText("—");
                tvRouteTotProspects.setText("—");
            } else {
                tvSalesTotCalls.setText(String.valueOf(totalCalls));
                tvSalesTotVisits.setText(String.valueOf(totalVisits));
                tvSalesTotQuotes.setText(String.valueOf(totalQuotes));
                tvSalesTotFollowups.setText(String.valueOf(totalFollowups));
                tvSalesTotProspects.setText(String.valueOf(totalProspects));
                tvSalesTotClients.setText(String.valueOf(totalClients));

                tvSalesCardCallsObt.setText(String.valueOf(totalCalls));
                tvSalesCardCallsPct.setText(String.format(Locale.getDefault(), "%.1f %%", (totalCalls * 100f) / 100f));

                tvSalesCardVisitsObt.setText(String.valueOf(totalVisits));
                tvSalesCardVisitsPct.setText(String.format(Locale.getDefault(), "%.1f %%", (totalVisits * 100f) / 60f));

                tvSalesCardQuotesObt.setText(String.valueOf(totalQuotes));
                tvSalesCardQuotesPct.setText(String.format(Locale.getDefault(), "%.1f %%", (totalQuotes * 100f) / 30f));

                tvSalesCardFollowupsObt.setText(String.valueOf(totalFollowups));
                tvSalesCardFollowupsPct.setText(String.format(Locale.getDefault(), "%.1f %%", (totalFollowups * 100f) / 50f));

                tvSalesCardProspectsObt.setText(String.valueOf(totalProspects));
                tvSalesCardProspectsPct.setText(String.format(Locale.getDefault(), "%.1f %%", (totalProspects * 100f) / 15f));

                tvSalesCardClientsObt.setText(String.valueOf(totalClients));
                tvSalesCardClientsPct.setText(String.format(Locale.getDefault(), "%.1f %%", (totalClients * 100f) / 3f));

                tvRouteTotCalls.setText(String.valueOf(totalCalls));
                tvRouteTotVisits.setText(String.valueOf(totalVisits));
                tvRouteTotQuotes.setText(String.valueOf(totalQuotes));
                tvRouteTotFollowups.setText(String.valueOf(totalFollowups));
                tvRouteTotProspects.setText(String.valueOf(totalProspects));
            }

            populateRouteRows(llRouteRowsContainer, false);
        }

        flReportSheetContainer.addView(sheet);
    }

    private void populateRouteRows(LinearLayout container, boolean isBlank) {
        if (container == null) return;
        container.removeAllViews();

        String routeCollab = (selectedCollab != null && !selectedCollab.isEmpty()) ? selectedCollab : pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        if (routeCollab == null || routeCollab.isEmpty() || routeCollab.equals("Colaborador WebSite+")) {
            routeCollab = "Asesor Comercial";
        }

        String[] hours = {
                "08:00 AM", "08:15 AM", "08:30 AM", "08:45 AM",
                "09:00 AM", "09:15 AM", "09:30 AM", "09:45 AM",
                "10:00 AM", "10:15 AM", "10:30 AM", "10:45 AM",
                "11:00 AM", "11:05 AM", "11:40 AM",
                "12:15 PM", "12:30 PM", "12:45 PM",
                "01:00 PM", "01:05 PM", "01:30 PM"
        };

        String[][] refEntries = {
                {"Corporación Andina", "Ing. Carlos Vega", "984123891", "Página Web", "Llamada inicial"},
                {"Llamada seguimiento", "Lic. Patricia Ruiz", "972134568", "E-commerce", "Enviará brief"},
                {"Policlínico San José", "Dra. Mercedes P.", "973412589", "Sistema Citas", "Reunión agendada"},
                {"Llamada prospección", "Marco Alarcón", "961234875", "Identidad Visual", "Interesado"},
                {"Inversiones El Sol", "Lic. Pedro Ramos", "951874236", "Publicidad Web", "Cotización pedida"},
                {"Llamada seguimiento", "Sra. Carmen Gil", "941258796", "Redes Sociales", "Llamar tarde"},
                {"Distribuidora Norte", "Mario Gutiérrez", "948123984", "Catálogo Web", "Seguimiento"},
                {"Llamada prospecto", "Hugo Benavides", "931254876", "Hosting + Dominio", "Contactado"},
                {"Ferretería Central", "Julia Mendoza", "963258741", "Tienda Virtual", "Prospecto calificado"},
                {"Llamada seguimiento", "Ing. Walter Vera", "921458763", "Web Corporativa", "Enviado correo"},
                {"Estudio Jurídico Díaz", "Dr. Fernando D.", "952147863", "Landing Page", "Interés alto"},
                {"Llamada prospecto", "Elena Morales", "914523698", "Branding Web", "Pedir logo"},
                {"Consultoría Andina", "César Prado", "987412365", "Auditoría SEO", "Cotización lista"},
                {"Llamada seguimiento", "Dra. Rosa Vidal", "978541236", "Diseño Web", "Revisión propuesta"},
                {"Agroexportadora Sur", "Ing. Raúl Flores", "965874123", "Portal B2B", "Seguimiento 2"},
                {"Llamada prospecto", "Víctor Quispe", "954123698", "Mantenimiento Web", "Agendado"},
                {"Restaurante Casona", "Roberto Ruiz", "987456123", "Menú Digital QR", "Negociación"},
                {"Llamada seguimiento", "Sra. Gloria Solís", "945123687", "Campaña Ads", "Confirmación"},
                {"Llamada prospecto", "David Navarro", "932145698", "Página Web", "Prospecto nuevo"},
                {"Llamada seguimiento", "Lic. Mónica Silva", "923145698", "Software Medida", "Cierre pendiente"},
                {"Cierre de Jornada", routeCollab, "Oficina", "Consolidado Diario", "23 llam. / 4 prosp."}
        };

        // Lectura de la base de datos local SQLite de rutas
        KpiRouteDbHelper routeDb = KpiRouteDbHelper.getInstance(requireContext());
        List<KpiRouteItem> realRouteItems = routeDb.getAllItems();
        boolean hasRealItems = (realRouteItems != null && !realRouteItems.isEmpty());
        boolean isWeek37 = (selectedWeek != null && selectedWeek.contains("37"));

        for (int i = 0; i < hours.length; i++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (18 * getResources().getDisplayMetrics().density)
            ));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(100f);

            TextView tvHour = createRouteCell(hours[i], 15f, true, Gravity.CENTER);
            row.addView(tvHour);

            String business = "—";
            String contact = "—";
            String phone = "—";
            String service = "—";
            String obs = "—";

            if (!isBlank) {
                if (hasRealItems && i < realRouteItems.size()) {
                    KpiRouteItem item = realRouteItems.get(i);
                    business = item.getBusinessName();
                    contact = item.getContactPerson();
                    phone = item.getPhone();
                    service = item.getActivityType();
                    obs = item.getObservations();
                } else if (isWeek37) {
                    business = refEntries[i][0];
                    contact = refEntries[i][1];
                    phone = refEntries[i][2];
                    service = refEntries[i][3];
                    obs = refEntries[i][4];
                }
            }

            TextView tvBusiness = createRouteCell(business, 22f, false, Gravity.CENTER_VERTICAL | Gravity.START);
            row.addView(tvBusiness);

            TextView tvContact = createRouteCell(contact, 18f, false, Gravity.CENTER_VERTICAL | Gravity.START);
            row.addView(tvContact);

            TextView tvPhone = createRouteCell(phone, 15f, false, Gravity.CENTER);
            row.addView(tvPhone);

            TextView tvService = createRouteCell(service, 15f, false, Gravity.CENTER);
            row.addView(tvService);

            TextView tvObs = createRouteCell(obs, 15f, false, Gravity.CENTER);
            row.addView(tvObs);

            container.addView(row);
        }
    }

    private TextView createRouteCell(String text, float weight, boolean isBold, int gravity) {
        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        tv.setLayoutParams(lp);
        tv.setBackgroundResource(R.drawable.kpi_sheet_cell_data);
        tv.setText(text);
        tv.setTextSize(5.5f);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tv.setGravity(gravity);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        int pad = (int) (2 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, 0, pad, 0);
        if (isBold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    // =========================================================================
    // 4. REPORTE GENERAL: OTROS CARGOS (PROGRAMADOR JR, DISEÑADOR JR)
    // =========================================================================
    private void renderGeneralRoleSheet(LayoutInflater inflater) {
        TextView tvGeneral = new TextView(requireContext());
        tvGeneral.setPadding(32, 48, 32, 48);
        tvGeneral.setTextSize(13f);
        tvGeneral.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_text_primary));
        tvGeneral.setBackgroundResource(R.drawable.kpi_bg_card_white);

        String modeText = isBlankMode ? "Formato de Plantilla Vacía" : "Datos Registrados";
        tvGeneral.setText("REPORTE SEMANAL DE RENDIMIENTO (SIGD)\n\n"
                + "Cargo: " + selectedCargo + "\n"
                + "Colaborador asignado: " + selectedCollab + "\n"
                + "Semana activa: " + selectedWeek + "\n"
                + "Estado de vista: " + modeText + "\n\n"
                + "Este cargo se encuentra conectado a la base de datos de colaboradores.\n"
                + "Las actividades registradas por día (Lunes a Sábado) se consolidan y calculan automáticamente.");

        flReportSheetContainer.addView(tvGeneral);
    }

    // =========================================================================
    // 5. ACCIONES Y FLUJO: IMPRESIÓN Y ENVÍO A CUENTA DE SUPERVISOR (LICENCIADO)
    // =========================================================================
    private KpiSeniorWeekReport getSelectedSeniorWeek(KpiSeniorStorageManager storage) {
        if (storage == null) return null;
        if (selectedWeek != null) {
            if (selectedWeek.contains("37")) {
                KpiSeniorWeekReport w = storage.getWeekById("Semana N.º 37");
                if (w != null) return w;
            } else if (selectedWeek.contains("36")) {
                KpiSeniorWeekReport w = storage.getWeekById("Semana N.º 36");
                if (w != null) return w;
            } else if (selectedWeek.contains("35")) {
                KpiSeniorWeekReport w = storage.getWeekById("Semana N.º 35");
                if (w != null) return w;
            } else if (selectedWeek.contains("34")) {
                KpiSeniorWeekReport w = storage.getWeekById("Semana N.º 34");
                if (w != null) return w;
            } else if (selectedWeek.contains("33")) {
                KpiSeniorWeekReport w = storage.getWeekById("Semana N.º 33");
                if (w != null) return w;
            } else if (selectedWeek.contains("32")) {
                KpiSeniorWeekReport w = storage.getWeekById("Semana N.º 32");
                if (w != null) return w;
            }
        }
        return storage.getCurrentWeek();
    }

    private void handlePrintReport() {
        if (new pe.net.ws.kpiweb.core.session.SessionManager(requireContext()).isLoggedIn()) {
            Toast.makeText(requireContext(), "La ficha imprimible aún no usa el reporte del servidor.", Toast.LENGTH_LONG).show();
            showOwnServerReports();
            return;
        }
        if (flReportSheetContainer == null || flReportSheetContainer.getChildCount() == 0) {
            Toast.makeText(requireContext(), "No hay ningún reporte cargado para imprimir", Toast.LENGTH_SHORT).show();
            return;
        }

        View sheetView = flReportSheetContainer.getChildAt(0);
        Toast.makeText(requireContext(), "Iniciando servicio de impresión... Detectando impresoras WiFi / Bluetooth", Toast.LENGTH_SHORT).show();

        String jobName = "Reporte_Semanal_SIGD_" + selectedCargo.replace(" ", "_") + "_" + selectedCollab.replace(" ", "_");
        KpiPrintManager.printView(requireActivity(), sheetView, jobName);
    }

    private void showOwnServerReports() {
        pe.net.ws.kpiweb.core.session.SessionManager session =
                new pe.net.ws.kpiweb.core.session.SessionManager(requireContext());
        if (session.getWorkerId() <= 0) {
            Toast.makeText(requireContext(), "No hay un trabajador vinculado a la sesión.", Toast.LENGTH_LONG).show();
            return;
        }
        pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.retry(requireContext());
        KpiServerReportBrowser.show(requireActivity(), session.getWorkerId());
    }

    private void handleSendReport() {
        if (new pe.net.ws.kpiweb.core.session.SessionManager(requireContext()).isLoggedIn()) {
            showOwnServerReports();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Envío de Reporte")
                .setMessage("¿Confirmas el envío oficial del Reporte Semanal (" + selectedWeek + ") correspondiente a "
                        + selectedCollab + " a la cuenta del Lic. Manuel Castillo (Jefatura de Evaluación)?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar Reporte", (dialog, which) -> submitReportToSupervisor())
                .show();
    }

    private void submitReportToSupervisor() {
        if ("Diseñador Gráfico Senior".equalsIgnoreCase(selectedCargo.trim())) {
            KpiSeniorStorageManager storage = KpiSeniorStorageManager.getInstance(requireContext());
            KpiSeniorWeekReport week = getSelectedSeniorWeek(storage);
            if (week != null) {
                week.setStatus("Enviado al Lic. Manuel Castillo");
            }
        }

        KpiSoundHelper.playCollaboratorWeekendSuccess(requireContext());

        KpiPandaSuccessDialog.show(
                requireContext(),
                selectedCargo,
                false,
                "Reporte Semanal Enviado",
                100,
                100,
                () -> {
                    if (btnHubSendReport != null) {
                        btnHubSendReport.setText("Reporte Enviado ✓");
                        btnHubSendReport.setAlpha(0.85f);
                    }
                    Toast.makeText(requireContext(), "Reporte registrado exitosamente en la cuenta del Lic. Manuel Castillo", Toast.LENGTH_LONG).show();
                }
        );
    }

    // =========================================================================
    // 6. APARTADO: HISTORIAL DE REPORTES SEMANALES ARCHIVADOS (DOMINGO 23:59)
    // =========================================================================

    private static class HistoryItemEntry {
        final String cargo;
        final String collab;
        final String weekTitle;
        final String status;
        final String area;

        HistoryItemEntry(String cargo, String collab, String weekTitle, String status, String area) {
            this.cargo = cargo;
            this.collab = collab;
            this.weekTitle = weekTitle;
            this.status = status;
            this.area = area;
        }
    }

    private void showHistoryDialog() {
        if (new pe.net.ws.kpiweb.core.session.SessionManager(requireContext()).isLoggedIn()) {
            showOwnServerReports();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.kpi_dialog_reports_history, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnHistoryClose).setOnClickListener(v -> dialog.dismiss());

        LinearLayout container = view.findViewById(R.id.llHistoryItemsContainer);
        populateHistoryList(container, dialog);

        view.findViewById(R.id.btnForceSundayArchival).setOnClickListener(v -> {
            boolean success = KpiSundayArchivalHelper.executeSundayCloseAndReset(requireContext(), true);
            if (success) {
                populateHistoryList(container, dialog);
                renderCurrentReport();
            }
        });

        dialog.show();
    }

    private void populateHistoryList(LinearLayout container, AlertDialog dialog) {
        if (container == null) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        List<HistoryItemEntry> history = new ArrayList<>();
        String authName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
        String authRole = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        String collabForHistory = (selectedCollab != null && !selectedCollab.isEmpty()) ? selectedCollab : authName;
        String cargoForHistory = (selectedCargo != null && !selectedCargo.isEmpty()) ? selectedCargo : authRole;
        if (collabForHistory == null || collabForHistory.isEmpty()) {
            collabForHistory = "Colaborador SIGD";
        }
        if (cargoForHistory == null || cargoForHistory.isEmpty()) {
            cargoForHistory = "Diseñador Gráfico";
        }

        // Semanas archivadas dinámicas para el colaborador y cargo activo
        history.add(new HistoryItemEntry(cargoForHistory, collabForHistory, "Semana N.º 37 (Del 07/09/2026 al 12/09/2026)", "Enviado al Licenciado ✓", "WebSite+ SIGD"));
        history.add(new HistoryItemEntry(cargoForHistory, collabForHistory, "Semana N.º 36 (Del 31/08/2026 al 05/09/2026)", "Archivado en Historial (Domingo 23:59) ✓", "WebSite+ SIGD"));
        history.add(new HistoryItemEntry(cargoForHistory, collabForHistory, "Semana N.º 35 (Del 24/08/2026 al 29/08/2026)", "Archivado en Historial (Domingo 23:59) ✓", "WebSite+ SIGD"));
        history.add(new HistoryItemEntry(cargoForHistory, collabForHistory, "Semana N.º 34 (Del 17/08/2026 al 22/08/2026)", "Archivado en Historial (Domingo 23:59) ✓", "WebSite+ SIGD"));
        history.add(new HistoryItemEntry(cargoForHistory, collabForHistory, "Semana N.º 33 (Del 10/08/2026 al 15/08/2026)", "Archivado en Historial (Domingo 23:59) ✓", "WebSite+ SIGD"));
        history.add(new HistoryItemEntry(cargoForHistory, collabForHistory, "Semana N.º 32 (Del 03/08/2026 al 08/08/2026)", "Archivado en Historial (Domingo 23:59) ✓", "WebSite+ SIGD"));

        for (HistoryItemEntry item : history) {
            View itemView = inflater.inflate(R.layout.kpi_item_history_week, container, false);

            TextView tvCargo = itemView.findViewById(R.id.tvHistoryCargoBadge);
            TextView tvStatus = itemView.findViewById(R.id.tvHistoryStatusBadge);
            TextView tvWeek = itemView.findViewById(R.id.tvHistoryWeekTitle);
            TextView tvCollab = itemView.findViewById(R.id.tvHistoryCollabName);
            View btnOpen = itemView.findViewById(R.id.btnHistoryOpenSheet);

            tvCargo.setText(item.cargo.toUpperCase(Locale.getDefault()));
            tvStatus.setText(item.status);
            tvWeek.setText(item.weekTitle);
            boolean isItemPracticante = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.isRoleNaturallyPracticante(item.cargo);
            String itemPrefix = isItemPracticante ? "Practicante: " : "Colaborador: ";
            tvCollab.setText(itemPrefix + item.collab + " · Área: " + item.area);

            View.OnClickListener clickListener = v -> {
                if (!isExclusiveUserMode) {
                    selectedCargo = item.cargo;
                    selectedCollab = item.collab;
                }
                selectedWeek = item.weekTitle;
                isBlankMode = false;
                updateModeButtonStyles();

                if (!isExclusiveUserMode && spnReportCargo != null && spnReportCargo.getAdapter() != null) {
                    @SuppressWarnings("unchecked")
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spnReportCargo.getAdapter();
                    int pos = adapter.getPosition(selectedCargo);
                    if (pos >= 0) spnReportCargo.setSelection(pos);
                }

                if (!isExclusiveUserMode) {
                    updateCollaboratorSpinner();
                }

                if (spnReportWeek != null && spnReportWeek.getAdapter() != null) {
                    @SuppressWarnings("unchecked")
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spnReportWeek.getAdapter();
                    int pos = adapter.getPosition(selectedWeek);
                    if (pos >= 0) spnReportWeek.setSelection(pos);
                }

                renderCurrentReport();
                dialog.dismiss();
                Toast.makeText(requireContext(), "Ficha oficial cargada: " + item.collab + " (" + item.weekTitle + ")", Toast.LENGTH_SHORT).show();
            };

            itemView.setOnClickListener(clickListener);
            if (btnOpen != null) {
                btnOpen.setOnClickListener(clickListener);
            }

            container.addView(itemView);
        }
    }
}
