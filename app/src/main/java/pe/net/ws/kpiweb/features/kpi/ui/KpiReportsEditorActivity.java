package pe.net.ws.kpiweb.features.kpi.ui;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;

/** Acceso administrativo al editor compartido de fichas visuales del servidor. */
public final class KpiReportsEditorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle state) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(state);
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getRoleId() != 1) {
            finish();
            return;
        }
        FrameLayout root = new FrameLayout(this);
        root.setId(R.id.main);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        setContentView(root);
        ViewCompat.requestApplyInsets(root);
        if (state == null) getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, new KpiReportsEditorFragment()).commit();
    }
}
