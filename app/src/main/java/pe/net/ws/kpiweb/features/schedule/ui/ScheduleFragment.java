package pe.net.ws.kpiweb.features.schedule.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.schedule.adapter.KpiScheduleAdapter;
import pe.net.ws.kpiweb.features.schedule.adapter.LevelScheduleAdapter;
import pe.net.ws.kpiweb.features.schedule.adapter.SlotTimelineAdapter;
import pe.net.ws.kpiweb.features.schedule.data.ScheduleData;
import pe.net.ws.kpiweb.features.schedule.data.ScheduleRepository;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleProfile;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleWorkday;

/**
 * Visor único de horarios: un solo diseño para todos los cargos. El filtro de cargo cambia los datos;
 * por defecto abre el cargo de la sesión. Se actualiza en tiempo real cuando el Editor guarda cambios.
 */
public class ScheduleFragment extends Fragment implements ScheduleRepository.Listener {

    private static final long REFRESH_MS = 30_000L;
    private static final String ARG_PROFILE = "profile_id";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            refreshTimeline(false);
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    private ScheduleRepository repo;
    private ScheduleProfile profile;
    private RecyclerView list;
    private TabLayout tabs;
    private TextView cargoName;
    private TextView subtitle;
    private View alertBanner;
    private TextView alertText;
    private View[] dayCells;
    private SlotTimelineAdapter timelineAdapter;
    private int selectedDay;
    private int todayIndex;
    private int currentTab = 0;
    private boolean rebuildingTabs = false;
    private boolean pendingReload = false;

    public static ScheduleFragment newInstance() {
        return new ScheduleFragment();
    }

    public static ScheduleFragment newInstance(String profileId) {
        ScheduleFragment f = new ScheduleFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PROFILE, profileId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.schedule_fragment, container, false);
        repo = ScheduleRepository.getInstance(requireContext());
        if (!repo.isRemoteLoaded()) {
            TextView loading = new TextView(requireContext());
            loading.setText("Cargando horario del servidor...");
            loading.setTextColor(0xFFFFFFFF);
            loading.setBackgroundColor(0xFF10051F);
            loading.setGravity(android.view.Gravity.CENTER);
            loading.setOnClickListener(v -> requestRemote(loading));
            requestRemote(loading);
            return loading;
        }
        profile = resolveInitialProfile();
        if (profile == null) {
            TextView empty = new TextView(requireContext());
            empty.setText("Todavía no hay un horario asignado a tu cargo.");
            empty.setTextColor(0xFFFFFFFF);
            empty.setBackgroundColor(0xFF10051F);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32, 32, 32, 32);
            return empty;
        }

        todayIndex = todayIndex();
        selectedDay = todayIndex >= 0 ? todayIndex : 0; // domingo -> lunes

        root.findViewById(R.id.schBtnBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
            else requireActivity().finish();
        });

        subtitle = root.findViewById(R.id.schSubtitle);
        cargoName = root.findViewById(R.id.schCargoName);
        alertBanner = root.findViewById(R.id.schAlertBanner);
        alertText = root.findViewById(R.id.schAlertText);
        if (alertBanner != null && new SessionManager(requireContext()).getRoleId() == 1) {
            alertBanner.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                    .replace(R.id.main, ScheduleSheetFragment.newInstance(profile.id))
                    .addToBackStack(null)
                    .commit());
        }
        root.findViewById(R.id.schCargoSelector).setOnClickListener(v -> {
            if (new SessionManager(requireContext()).getRoleId() == 1) showCargoPicker();
        });

        list = root.findViewById(R.id.schList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        buildDays((ViewGroup) root.findViewById(R.id.schDaysBar), inflater);

        tabs = root.findViewById(R.id.schTabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!rebuildingTabs) showTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        bindProfile(true);
        return root;
    }

    private void requestRemote(TextView loading) {
        loading.setText("Cargando horario del servidor...");
        repo.refreshRemote(success -> {
            if (!isAdded()) return;
            if (success) {
                ScheduleFragment fresh = getArguments() != null
                        ? newInstance(getArguments().getString(ARG_PROFILE)) : newInstance();
                getParentFragmentManager().beginTransaction().replace(R.id.main, fresh).commit();
            } else {
                loading.setText("No se pudo cargar el horario del servidor. Toca para reintentar.");
            }
        });
    }

    private ScheduleProfile resolveInitialProfile() {
        String id = getArguments() != null ? getArguments().getString(ARG_PROFILE) : null;
        SessionManager session = new SessionManager(requireContext());
        if (id != null && session.getRoleId() == 1) return repo.get(id);
        ScheduleProfile mine = repo.findByRoleName(new SessionManager(requireContext()).getCargoName());
        if (mine == null && new SessionManager(requireContext()).getRoleId() == 1) {
            mine = repo.findByRoleName(KpiDefaultSessionManager.getInstance().getAuthenticatedRole());
        }
        return mine != null ? mine : (session.getRoleId() == 1 ? repo.get(null) : null);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (profile == null) return;
        repo.addListener(this);
        if (pendingReload) {
            pendingReload = false;
            reloadFromRepository();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (profile != null) repo.removeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (profile == null) return;
        // Un cambio de jornada o cargo en Asistencias debe verse al volver a abrir el cronograma.
        repo.refreshRemote(success -> { });
        refreshTimeline(false);
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, REFRESH_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    @Override
    public void onSchedulesChanged() {
        if (isAdded() && profile != null) reloadFromRepository();
    }

    private void reloadFromRepository() {
        profile = repo.get(profile.id);
        if (profile == null) profile = resolveInitialProfile();
        if (profile == null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main, newInstance()).commit();
            return;
        }
        bindProfile(false);
    }

    /** Aplica los datos del cargo al diseño único. */
    private void bindProfile(boolean firstTime) {
        boolean personal = new SessionManager(requireContext()).getRoleId() != 1;
        subtitle.setText(personal
                ? profile.subtitle + "\nJornada personal según Asistencias"
                : profile.subtitle);
        cargoName.setText(profile.roleName);
        if (alertBanner != null && alertText != null) {
            if (new SessionManager(requireContext()).getRoleId() == 1 && repo.hasAlert(profile.id)) {
                alertText.setText("Metas KPI actualizadas para " + profile.roleName
                        + ". El Licenciado debe revisar este horario.");
                alertBanner.setVisibility(View.VISIBLE);
            } else {
                alertBanner.setVisibility(View.GONE);
            }
        }
        timelineAdapter = new SlotTimelineAdapter(profile);

        rebuildingTabs = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText(R.string.sch_tab_schedule));
        tabs.addTab(tabs.newTab().setText(profile.goalsTabLabel));
        if (profile.levels != null) {
            tabs.addTab(tabs.newTab().setText(R.string.sch_tab_evaluation));
        }
        if (currentTab >= tabs.getTabCount()) currentTab = 0;
        TabLayout.Tab sel = tabs.getTabAt(currentTab);
        if (sel != null) sel.select();
        rebuildingTabs = false;

        showTab(currentTab);
    }

    private void showCargoPicker() {
        final List<ScheduleProfile> all = repo.getAll();
        String[] names = new String[all.size()];
        int checked = 0;
        for (int i = 0; i < all.size(); i++) {
            names[i] = all.get(i).roleName;
            if (all.get(i).id.equals(profile.id)) checked = i;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sch_pick_cargo)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    profile = all.get(which);
                    bindProfile(false);
                    d.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int todayIndex() {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        return dow == Calendar.SUNDAY ? -1 : dow - Calendar.MONDAY;
    }

    private int nowMinutes() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    private void buildDays(ViewGroup bar, LayoutInflater inflater) {
        dayCells = new View[ScheduleData.DAYS.length];
        for (int i = 0; i < dayCells.length; i++) {
            View cell = inflater.inflate(R.layout.schedule_day_cell, bar, false);
            ((TextView) cell.findViewById(R.id.schDayText)).setText(ScheduleData.DAYS[i]);
            cell.findViewById(R.id.schDayDot).setVisibility(i == todayIndex ? View.VISIBLE : View.INVISIBLE);
            final int day = i;
            cell.setOnClickListener(v -> {
                selectedDay = day;
                styleDays();
                refreshTimeline(true);
            });
            bar.addView(cell);
            dayCells[i] = cell;
        }
        styleDays();
    }

    private void styleDays() {
        for (int i = 0; i < dayCells.length; i++) {
            boolean sel = i == selectedDay;
            TextView t = dayCells[i].findViewById(R.id.schDayText);
            dayCells[i].setBackgroundResource(sel ? R.drawable.sch_bg_day_selected : 0);
            t.setTextColor(ContextCompat.getColor(requireContext(),
                    sel ? android.R.color.white : R.color.sch_text_soft));
        }
    }

    private void showTab(int tab) {
        currentTab = tab;
        if (tab == 0) {
            list.setAdapter(timelineAdapter);
            refreshTimeline(true);
        } else if (tab == 1) {
            list.setAdapter(new KpiScheduleAdapter(profile.goals));
        } else if (profile.levels != null) {
            list.setAdapter(new LevelScheduleAdapter(profile.levels));
        }
    }

    /** Recalcula la lista; si scroll=true, lleva la vista a la actividad actual. */
    private void refreshTimeline(boolean scroll) {
        if (currentTab != 0 || list == null || timelineAdapter == null) return;
        boolean isToday = selectedDay == todayIndex;
        if (new SessionManager(requireContext()).getRoleId() != 1) {
            ScheduleWorkday day = repo.getOwnWorkday(selectedDay);
            timelineAdapter.setPersonalDay(true, day);
        }
        timelineAdapter.update(selectedDay, isToday, nowMinutes());
        if (scroll) {
            int pos = timelineAdapter.getNowAdapterPosition();
            list.scrollToPosition(Math.max(pos - 1, 0));
        }
    }
}
