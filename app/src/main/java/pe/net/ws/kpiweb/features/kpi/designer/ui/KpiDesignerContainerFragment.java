package pe.net.ws.kpiweb.features.kpi.designer.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig;
import pe.net.ws.kpiweb.features.kpi.notification.KpiReminderScheduler;

/**
 * Contenedor Maestro Unificado del Sistema KPI (SIGD 2026) - Diseñador Gráfico.
 * Soporta los 5 cargos operativos y los 2 estilos de KPI:
 * - COLABORADORES (Estilo Elegante Ejecutivo 👔)
 * - PRACTICANTES (Estilo Eléctrico / Energía ⚡)
 */
public class KpiDesignerContainerFragment extends Fragment {

    private static final String ARG_ROLE = "arg_role";
    private static final String ARG_PRACTICANTE = "arg_practicante";

    private View toolbarDesigner;
    private View btnDesignerBack;
    private TextView tvDesignerHeaderTitle;
    private TextView tvDesignerHeaderSubtitle;
    private View btnDesignerNotification;
    private TextView btnDesignerGoToReport;

    private int currentSection = 0; // 0 = KPI Diario, 1 = Reportes

    public static KpiDesignerContainerFragment newInstance() {
        return new KpiDesignerContainerFragment();
    }

    public static KpiDesignerContainerFragment newInstanceForRole(String roleName, boolean isPracticanteMode) {
        KpiDesignerContainerFragment fragment = new KpiDesignerContainerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, roleName);
        args.putBoolean(ARG_PRACTICANTE, isPracticanteMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null && getArguments().containsKey(ARG_ROLE)) {
            String role = getArguments().getString(ARG_ROLE, "Diseñador Gráfico");
            boolean isPracticante = getArguments().getBoolean(ARG_PRACTICANTE, false);
            KpiDefaultSessionManager.getInstance().setActiveRoleAndMode(role, isPracticante);
        }

        View root = inflater.inflate(R.layout.kpi_designer_fragment_container, container, false);
        initViews(root);
        applyRoleStyleToHeader();
        setupNavigation();

        if (savedInstanceState == null) {
            navigateToSection(0);
        }

        return root;
    }

    private void initViews(View root) {
        toolbarDesigner = root.findViewById(R.id.toolbarDesigner);
        btnDesignerBack = root.findViewById(R.id.btnDesignerBack);
        tvDesignerHeaderTitle = root.findViewById(R.id.tvDesignerHeaderTitle);
        tvDesignerHeaderSubtitle = root.findViewById(R.id.tvDesignerHeaderSubtitle);
        btnDesignerNotification = root.findViewById(R.id.btnDesignerNotification);
        btnDesignerGoToReport = root.findViewById(R.id.btnDesignerGoToReport);
    }

    public void applyRoleStyleToHeader() {
        String activeRole = KpiDefaultSessionManager.getInstance().getAuthenticatedRole();
        boolean isPracticante = KpiDefaultSessionManager.getInstance().isPracticanteMode();
        KpiRoleProfileConfig config = KpiRoleProfileConfig.forRole(activeRole, isPracticante);

        if (toolbarDesigner != null) {
            toolbarDesigner.setBackgroundResource(isPracticante
                    ? R.drawable.kpi_bg_header_electric
                    : R.drawable.kpi_bg_header_elegant);
        }

        if (tvDesignerHeaderSubtitle != null) {
            String authName = KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String prefix = isPracticante ? "Practicante: " : "Colaborador: ";
            String namePart = (authName != null && !authName.isEmpty() && !authName.equals("Colaborador WebSite+"))
                    ? prefix + authName : config.getRoleName();
            tvDesignerHeaderSubtitle.setText(namePart + " · " + (isPracticante ? "Estilo Eléctrico" : "Estilo Elegante"));
        }
    }

    private void setupNavigation() {
        if (btnDesignerBack != null) {
            btnDesignerBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Botón de campana: Dispara y verifica el recordatorio de las 8:00 PM en vivo
        if (btnDesignerNotification != null) {
            btnDesignerNotification.setOnClickListener(v ->
                    KpiReminderScheduler.triggerInstantReminderCheck(requireContext()));
        }

        // Botón directo de Reporte en cabecera
        if (btnDesignerGoToReport != null) {
            btnDesignerGoToReport.setOnClickListener(v -> openReportsHub());
        }
    }

    private void openReportsHub() {
        if (getActivity() != null) {
            String collabName = pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager.getInstance().getAuthenticatedUserName();
            String cargoName = "Diseñador Gráfico"; // Vinculado directamente como Diseñador Gráfico oficial
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, pe.net.ws.kpiweb.features.kpi.ui.KpiReportsHubFragment.newInstanceExclusive(cargoName, collabName))
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void navigateToSection(int section) {
        this.currentSection = section;

        Fragment targetFragment;
        if (section == 0) {
            if (tvDesignerHeaderTitle != null) tvDesignerHeaderTitle.setText("Registro diario");
            targetFragment = KpiDesignerFormFragment.newInstance();
        } else {
            if (tvDesignerHeaderTitle != null) tvDesignerHeaderTitle.setText("REPORTES SEMANALES");
            targetFragment = KpiDesignerReportsFragment.newInstance();
        }

        applyRoleStyleToHeader();

        getChildFragmentManager().beginTransaction()
                .replace(R.id.flDesignerContent, targetFragment)
                .commit();
    }
}
