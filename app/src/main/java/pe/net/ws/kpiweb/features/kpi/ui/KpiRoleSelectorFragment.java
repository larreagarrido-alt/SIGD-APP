package pe.net.ws.kpiweb.features.kpi.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;

/** Compatibility destination for legacy KPI form back buttons. */
public final class KpiRoleSelectorFragment extends Fragment {
    public static KpiRoleSelectorFragment newInstance() { return new KpiRoleSelectorFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(0xFF090419);
        return root;
    }

    @Override public void onResume() {
        super.onResume();
        if (requireActivity() instanceof CollaboratorKpiIntegrationActivity) {
            requireActivity().finish();
        }
    }
}
