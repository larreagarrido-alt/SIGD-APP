package pe.net.ws.kpiweb.features.schedule.ui;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;

/** Entry point reachable from the actual administrator and worker menus. */
public final class ScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_EDITOR = "schedule_editor";

    @Override
    protected void onCreate(@Nullable Bundle state) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(state);
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) { finish(); return; }
        FrameLayout root = new FrameLayout(this);
        root.setId(R.id.main);
        root.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        root.setBackgroundColor(0xFF6400BA);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        setContentView(root);
        ViewCompat.requestApplyInsets(root);
        if (state == null) {
            boolean editor = getIntent().getBooleanExtra(EXTRA_EDITOR, false) && session.getRoleId() == 1;
            Fragment fragment = editor ? ScheduleSheetFragment.newInstance() : ScheduleFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.main, fragment).commit();
        }
    }
}
