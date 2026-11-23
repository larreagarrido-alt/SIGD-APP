package pe.net.ws.kpiweb.features.schedule.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.schedule.data.ScheduleData;
import pe.net.ws.kpiweb.features.schedule.data.ScheduleRepository;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleProfile;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ficha oficial del horario con diseño corporativo de lámina editable directamente sobre la hoja,
 * con zoom interactivo, desplazamiento libre y soporte vertical u horizontal.
 */
public class ScheduleSheetFragment extends Fragment implements ScheduleRepository.Listener {

    private static final String ARG_PROFILE = "profile_id";

    private static final int PURPLE_DARK = Color.parseColor("#581C87");
    private static final int PURPLE_MAIN = Color.parseColor("#6D28D9");
    private static final int PURPLE_BRIGHT = Color.parseColor("#8B5CF6");
    private static final int PURPLE_PILL = Color.parseColor("#E9D5FF");
    private static final int PURPLE_SOFT_BG = Color.parseColor("#F5F0FF");
    private static final int PURPLE_BREAK_BG = Color.parseColor("#EFE6FF");
    private static final int GRID_LINE = Color.parseColor("#D8B4FE");
    private static final int INK_DARK = Color.parseColor("#1E1B4B");
    private static final int INK_MUTED = Color.parseColor("#4C1D95");

    private ScheduleRepository repo;
    private ScheduleProfile profile;
    private ZoomPanLayout zoom;
    private PosterSheetLayout sheetHolder;
    private TextView cargoBtn;
    private LinearLayout alertBar;
    private TextView alertText;
    private float density;

    public static ScheduleSheetFragment newInstance() {
        return new ScheduleSheetFragment();
    }

    public static ScheduleSheetFragment newInstance(String profileId) {
        ScheduleSheetFragment f = new ScheduleSheetFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PROFILE, profileId);
        f.setArguments(b);
        return f;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private TextView text(String s, float sp, int color, int typefaceStyle) {
        TextView t = new TextView(requireContext());
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (typefaceStyle != Typeface.NORMAL) t.setTypeface(Typeface.DEFAULT, typefaceStyle);
        return t;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        return text(s, sp, color, bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    /** Botón tipo chip estilizado con icono vectorial y elevación sutil para la barra de herramientas. */
    private LinearLayout actionChip(int iconRes, @Nullable String label, int bgColor, int strokeColor, int fgColor) {
        LinearLayout chip = new LinearLayout(requireContext());
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(label == null ? 10 : 12), 0, dp(label == null ? 10 : 13), 0);
        chip.setElevation(dp(2));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), strokeColor);
        chip.setBackground(bg);

        ImageView iv = new ImageView(requireContext());
        iv.setImageResource(iconRes);
        iv.setColorFilter(fgColor);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(dp(16), dp(16));
        if (label != null && !label.isEmpty()) iLp.setMarginEnd(dp(6));
        chip.addView(iv, iLp);

        if (label != null && !label.isEmpty()) {
            chip.addView(text(label, 12f, fgColor, true));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);
        return chip;
    }

    private View dockDivider() {
        View v = new View(requireContext());
        v.setBackgroundColor(Color.parseColor("#E2E8F0"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(22));
        lp.setMarginEnd(dp(8));
        v.setLayoutParams(lp);
        return v;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        density = getResources().getDisplayMetrics().density;
        repo = ScheduleRepository.getInstance(requireContext());
        if (new SessionManager(requireContext()).getRoleId() != 1) {
            TextView blocked = new TextView(requireContext());
            blocked.setText("Tu cuenta no puede editar horarios.");
            blocked.setGravity(Gravity.CENTER);
            blocked.setTextColor(INK_DARK);
            return blocked;
        }
        if (!repo.isRemoteLoaded()) {
            TextView loading = text("Cargando horarios del servidor...", 15f, INK_DARK, false);
            loading.setGravity(Gravity.CENTER);
            loading.setOnClickListener(v -> requestRemote(loading));
            requestRemote(loading);
            return loading;
        }
        String id = savedInstanceState != null
                ? savedInstanceState.getString(ARG_PROFILE)
                : (getArguments() != null ? getArguments().getString(ARG_PROFILE) : null);
        if (id != null) {
            profile = repo.get(id);
        } else {
            ScheduleProfile mine = repo.findByRoleName(new SessionManager(requireContext()).getCargoName());
            profile = mine != null ? mine : repo.get(null);
        }
        if (profile == null) {
            TextView empty = text("No hay horarios publicados para editar.", 15f, INK_DARK, false);
            empty.setGravity(Gravity.CENTER);
            return empty;
        }

        boolean darkUi = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor(darkUi ? "#11091F" : "#F8F5FC"));
        root.setClickable(true);
        root.setFocusable(true);

        // ---- barra superior corporativa
        LinearLayout top = new LinearLayout(requireContext());
        top.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable topBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#4C1D95"), PURPLE_MAIN, Color.parseColor("#7C3AED")});
        top.setBackground(topBg);
        top.setElevation(dp(4));
        top.setPadding(dp(12), dp(8), dp(12), dp(10));

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = new ImageView(requireContext());
        back.setImageResource(R.drawable.kpi_ic_arrow_back_bold);
        back.setColorFilter(Color.WHITE);
        back.setPadding(dp(6), dp(6), dp(6), dp(6));
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        back.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) getParentFragmentManager().popBackStack();
            else requireActivity().finish();
        });
        titleRow.addView(back);

        TextView title = text("Ficha de Horario", 17.5f, Color.WHITE, true);
        title.setPadding(dp(6), 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Selector de cargo translúcido en el encabezado
        LinearLayout cargoChip = new LinearLayout(requireContext());
        cargoChip.setOrientation(LinearLayout.HORIZONTAL);
        cargoChip.setGravity(Gravity.CENTER_VERTICAL);
        cargoChip.setPadding(dp(12), dp(6), dp(8), dp(6));
        GradientDrawable cargoBg = new GradientDrawable();
        cargoBg.setColor(Color.parseColor("#33FFFFFF"));
        cargoBg.setCornerRadius(dp(18));
        cargoBg.setStroke(dp(1), Color.parseColor("#66FFFFFF"));
        cargoChip.setBackground(cargoBg);
        ImageView cIc = new ImageView(requireContext());
        cIc.setImageResource(R.drawable.sch_ic_schedule);
        cIc.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams cIcLp = new LinearLayout.LayoutParams(dp(16), dp(16));
        cIcLp.setMarginEnd(dp(6));
        cargoChip.addView(cIc, cIcLp);
        cargoBtn = text("Cargo", 12.5f, Color.WHITE, true);
        cargoBtn.setMaxLines(1);
        cargoChip.addView(cargoBtn);
        ImageView cArr = new ImageView(requireContext());
        cArr.setImageResource(R.drawable.sch_ic_expand);
        cArr.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams cArrLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        cArrLp.setMarginStart(dp(4));
        cargoChip.addView(cArr, cArrLp);
        cargoChip.setOnClickListener(v -> showCargoPicker());
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccLp.setMarginEnd(dp(4));
        titleRow.addView(cargoChip, ccLp);

        ImageView more = new ImageView(requireContext());
        more.setImageResource(R.drawable.sch_ic_more);
        more.setColorFilter(Color.WHITE);
        more.setPadding(dp(8), dp(8), dp(8), dp(8));
        more.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        more.setOnClickListener(this::showMenu);
        titleRow.addView(more);
        top.addView(titleRow);
        root.addView(top);

        // ---- dock blanco elevado con botones estilizados (zoom, giro, agregar y eliminar)
        HorizontalScrollView toolsScroll = new HorizontalScrollView(requireContext());
        toolsScroll.setHorizontalScrollBarEnabled(false);
        toolsScroll.setBackgroundColor(Color.parseColor(darkUi ? "#251630" : "#FFFFFF"));
        toolsScroll.setElevation(dp(3));
        LinearLayout tools = new LinearLayout(requireContext());
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(12), dp(8), dp(12), dp(8));

        final int neutralBg = Color.parseColor(darkUi ? "#302044" : "#F8F5FC");
        final int neutralStroke = Color.parseColor(darkUi ? "#674182" : "#D9C7EB");
        final int neutralFg = Color.parseColor(darkUi ? "#F5EBFC" : "#3A2750");

        final int addBg = Color.parseColor(darkUi ? "#40255C" : "#F3E8FF");
        final int addStroke = Color.parseColor(darkUi ? "#8655AB" : "#C4B5FD");
        final int addFg = Color.parseColor(darkUi ? "#E9D5FF" : "#5B159D");

        final int delBg = Color.parseColor("#FEF2F2");
        final int delStroke = Color.parseColor("#FECACA");
        final int delFg = Color.parseColor("#B91C1C");

        LinearLayout zoomOut = actionChip(R.drawable.kpi_ic_minus, null, neutralBg, neutralStroke, neutralFg);
        zoomOut.setOnClickListener(v -> zoom.zoomBy(0.8f));
        tools.addView(zoomOut);

        LinearLayout fit = actionChip(R.drawable.kpi_ic_refresh, "Ajustar", neutralBg, neutralStroke, neutralFg);
        fit.setOnClickListener(v -> zoom.resetZoom());
        tools.addView(fit);

        LinearLayout fitPage = actionChip(R.drawable.sch_ic_schedule, "Página", neutralBg, neutralStroke, neutralFg);
        fitPage.setOnClickListener(v -> zoom.fitToPage());
        tools.addView(fitPage);

        LinearLayout zoomIn = actionChip(R.drawable.kpi_ic_plus, null, neutralBg, neutralStroke, neutralFg);
        zoomIn.setOnClickListener(v -> zoom.zoomBy(1.25f));
        tools.addView(zoomIn);

        LinearLayout rotate = actionChip(R.drawable.kpi_ic_refresh, "Girar", neutralBg, neutralStroke, PURPLE_MAIN);
        rotate.setOnClickListener(v -> toggleOrientation());
        tools.addView(rotate);

        tools.addView(dockDivider());

        LinearLayout addRow = actionChip(R.drawable.kpi_ic_plus, "Fila", addBg, addStroke, addFg);
        addRow.setOnClickListener(v -> addSlot());
        tools.addView(addRow);

        LinearLayout delRow = actionChip(R.drawable.kpi_ic_trash, "Eliminar fila", delBg, delStroke, delFg);
        delRow.setOnClickListener(v -> deleteRowPicker());
        tools.addView(delRow);

        tools.addView(dockDivider());

        LinearLayout addGoal = actionChip(R.drawable.sch_ic_target, "Meta", addBg, addStroke, addFg);
        addGoal.setOnClickListener(v -> editGoal(null));
        tools.addView(addGoal);

        LinearLayout delGoal = actionChip(R.drawable.kpi_ic_trash, "Eliminar meta", delBg, delStroke, delFg);
        delGoal.setOnClickListener(v -> deleteGoalPicker());
        tools.addView(delGoal);

        toolsScroll.addView(tools);
        root.addView(toolsScroll);

        // ---- alerta de cambio de metas en el Editor de KPI
        alertBar = new LinearLayout(requireContext());
        alertBar.setOrientation(LinearLayout.HORIZONTAL);
        alertBar.setGravity(Gravity.CENTER_VERTICAL);
        alertBar.setBackgroundColor(Color.parseColor("#FEF3C7"));
        alertBar.setPadding(dp(12), dp(9), dp(10), dp(9));

        ImageView alertIcon = new ImageView(requireContext());
        alertIcon.setImageResource(R.drawable.kpi_ic_warning);
        alertIcon.setColorFilter(Color.parseColor("#B45309"));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
        iconLp.setMarginEnd(dp(10));
        alertBar.addView(alertIcon, iconLp);

        alertText = text("", 12f, Color.parseColor("#92400E"), true);
        alertBar.addView(alertText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView ok = text("Ya ajuste", 12f, Color.WHITE, true);
        ok.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable okBg = new GradientDrawable();
        okBg.setColor(Color.parseColor("#D97706"));
        okBg.setCornerRadius(dp(8));
        ok.setBackground(okBg);
        ok.setOnClickListener(v -> repo.dismissAlert(profile.id));
        alertBar.addView(ok);
        alertBar.setVisibility(View.GONE);
        root.addView(alertBar);

        // ---- lienzo con zoom y lámina corporativa
        zoom = new ZoomPanLayout(requireContext());
        FrameLayout canvasWrap = new FrameLayout(requireContext());
        canvasWrap.setPadding(dp(12), dp(12), dp(12), dp(20));

        sheetHolder = new PosterSheetLayout(requireContext());
        sheetHolder.setOrientation(LinearLayout.VERTICAL);
        sheetHolder.setPadding(dp(22), dp(18), dp(22), dp(22));
        sheetHolder.setElevation(dp(6));
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(Color.WHITE);
        sheetBg.setCornerRadius(dp(16));
        sheetHolder.setBackground(sheetBg);
        sheetHolder.setClipToOutline(true);
        sheetHolder.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        canvasWrap.addView(sheetHolder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        zoom.addView(canvasWrap, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(zoom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        bind();
        return root;
    }

    private void requestRemote(TextView loading) {
        loading.setText("Cargando horarios del servidor...");
        repo.refreshRemote(success -> {
            if (!isAdded()) return;
            if (success) {
                String id = getArguments() == null ? null : getArguments().getString(ARG_PROFILE);
                ScheduleSheetFragment fresh = id == null ? newInstance() : newInstance(id);
                getParentFragmentManager().beginTransaction().replace(R.id.main, fresh).commit();
            } else {
                loading.setText("No se pudieron cargar los horarios. Toca para reintentar.");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (profile != null) repo.addListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (profile != null) repo.removeListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (profile != null) outState.putString(ARG_PROFILE, profile.id);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (zoom != null) zoom.post(() -> zoom.fitToWidth());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Activity a = getActivity();
        if (a != null) a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public void onSchedulesChanged() {
        if (!isAdded() || profile == null) return;
        ScheduleProfile fresh = repo.get(profile.id);
        if (fresh != null) profile = fresh;
        bind();
    }

    private void toggleOrientation() {
        Activity a = requireActivity();
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        a.setRequestedOrientation(land ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void showCargoPicker() {
        final List<ScheduleProfile> all = repo.getAll();
        String[] names = new String[all.size()];
        int checked = 0;
        for (int i = 0; i < all.size(); i++) {
            names[i] = all.get(i).roleName + (repo.hasAlert(all.get(i).id) ? "  (revisar)" : "");
            if (all.get(i).id.equals(profile.id)) checked = i;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Elegir cargo")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    profile = all.get(which);
                    bind();
                    zoom.post(() -> zoom.resetZoom());
                    d.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showMenu(View anchor) {
        if (repo.isRemoteLoaded()) {
            PopupMenu remoteMenu = new PopupMenu(requireContext(), anchor);
            remoteMenu.getMenu().add(0, 11, 0, "Asignar plantilla a un cargo nuevo");
            remoteMenu.getMenu().add(0, 12, 1, "Asignar turno a un trabajador");
            remoteMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 11) chooseUnassignedCargo();
                else if (item.getItemId() == 12) chooseWorkerShift();
                return true;
            });
            remoteMenu.show();
            return;
        }
        PopupMenu pm = new PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, "Nuevo horario");
        pm.getMenu().add(0, 2, 1, "Restaurar horario");
        pm.getMenu().add(0, 3, 2, "Eliminar horario");
        pm.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    newSchedule();
                    return true;
                case 2:
                    confirm("Restaurar horario", "Se devuelve " + profile.roleName + " a su ficha original.", () -> {
                        if (!repo.restoreDefault(profile.id)) {
                            toast("Este horario no tiene version original.");
                        }
                    });
                    return true;
                case 3:
                    if (profile.factory) {
                        toast("Los cargos oficiales no se eliminan; usa Restaurar horario.");
                    } else {
                        confirm("Eliminar horario", "Se elimina el horario de " + profile.roleName + ".", () -> {
                            String id = profile.id;
                            ScheduleProfile next = repo.getAll().isEmpty() ? null : repo.getAll().get(0);
                            if (repo.delete(id)) {
                                profile = next != null && !next.id.equals(id) ? repo.get(next.id) : repo.get(null);
                                bind();
                            }
                        });
                    }
                    return true;
                default:
                    return false;
            }
        });
        pm.show();
    }

    private void chooseWorkerShift() {
        String bearer = new SessionManager(requireContext()).getBearerToken();
        ApiClient.getScheduleService().workers(bearer).enqueue(new Callback<ApiResponse<JsonArray>>() {
            @Override public void onResponse(Call<ApiResponse<JsonArray>> call, Response<ApiResponse<JsonArray>> response) {
                if (!isAdded()) return;
                ApiResponse<JsonArray> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    toast("No se pudo cargar el personal activo.");
                    return;
                }
                List<Integer> ids = new java.util.ArrayList<>();
                List<String> names = new java.util.ArrayList<>();
                for (JsonElement element : body.getData()) {
                    JsonObject item = element.getAsJsonObject();
                    ids.add(item.get("id_trabajador").getAsInt());
                    String turno = item.has("turno") && !item.get("turno").isJsonNull()
                            ? item.get("turno").getAsString() : "sin turno";
                    names.add(item.get("nombre").getAsString() + " · "
                            + item.get("cargo").getAsString() + " · " + turno);
                }
                if (ids.isEmpty()) { toast("No hay trabajadores activos."); return; }
                new AlertDialog.Builder(requireContext()).setTitle("Trabajador activo")
                        .setItems(names.toArray(new String[0]), (dialog, which) ->
                                chooseShift(ids.get(which), bearer))
                        .setNegativeButton(android.R.string.cancel, null).show();
            }
            @Override public void onFailure(Call<ApiResponse<JsonArray>> call, Throwable error) {
                if (isAdded()) toast("No se pudo cargar el personal activo.");
            }
        });
    }

    private void chooseShift(int workerId, String bearer) {
        String[] options = {"Mañana · 08:00 a 13:30", "Tarde · 15:30 a 18:30", "Completo · 08:00 a 18:30"};
        String[] codes = {"MANANA", "TARDE", "COMPLETO"};
        new AlertDialog.Builder(requireContext()).setTitle("Turno laboral")
                .setItems(options, (dialog, which) -> {
                    JsonObject body = new JsonObject();
                    body.addProperty("turno", codes[which]);
                    ApiClient.getScheduleService().shift(bearer, workerId, body)
                            .enqueue(new Callback<ApiResponse<JsonObject>>() {
                                @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                                    if (!isAdded()) return;
                                    ApiResponse<JsonObject> result = response.body();
                                    toast(response.isSuccessful() && result != null && result.isSuccessful()
                                            ? "Turno guardado en el servidor." : "No se pudo guardar el turno.");
                                }
                                @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                                    if (isAdded()) toast("No se pudo guardar el turno.");
                                }
                            });
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void chooseUnassignedCargo() {
        String bearer = new SessionManager(requireContext()).getBearerToken();
        ApiClient.getScheduleService().cargos(bearer).enqueue(new Callback<ApiResponse<JsonArray>>() {
            @Override public void onResponse(Call<ApiResponse<JsonArray>> call, Response<ApiResponse<JsonArray>> response) {
                if (!isAdded()) return;
                ApiResponse<JsonArray> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    toast("No se pudieron cargar los cargos.");
                    return;
                }
                List<Integer> ids = new java.util.ArrayList<>();
                List<String> names = new java.util.ArrayList<>();
                for (JsonElement element : body.getData()) {
                    JsonObject item = element.getAsJsonObject();
                    if (item.has("horario") && !item.get("horario").isJsonNull()) continue;
                    ids.add(item.get("id_cargo").getAsInt());
                    names.add(item.get("nombre").getAsString());
                }
                if (ids.isEmpty()) {
                    toast("Todos los cargos actuales ya tienen horario.");
                    return;
                }
                new AlertDialog.Builder(requireContext()).setTitle("Cargo de Asistencias")
                        .setItems(names.toArray(new String[0]), (dialog, which) ->
                                chooseTemplate(ids.get(which), names.get(which), bearer))
                        .setNegativeButton(android.R.string.cancel, null).show();
            }
            @Override public void onFailure(Call<ApiResponse<JsonArray>> call, Throwable error) {
                if (isAdded()) toast("No se pudieron cargar los cargos.");
            }
        });
    }

    private void chooseTemplate(int cargoId, String cargoName, String bearer) {
        ApiClient.getScheduleService().templates(bearer).enqueue(new Callback<ApiResponse<JsonArray>>() {
            @Override public void onResponse(Call<ApiResponse<JsonArray>> call, Response<ApiResponse<JsonArray>> response) {
                if (!isAdded()) return;
                ApiResponse<JsonArray> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    toast("No se pudieron cargar las plantillas.");
                    return;
                }
                List<String> ids = new java.util.ArrayList<>();
                List<String> names = new java.util.ArrayList<>();
                for (JsonElement element : body.getData()) {
                    JsonObject item = element.getAsJsonObject();
                    ids.add(item.get("id_plantilla").getAsString());
                    names.add(item.getAsJsonObject("perfil").get("roleName").getAsString());
                }
                new AlertDialog.Builder(requireContext()).setTitle("Plantilla para " + cargoName)
                        .setItems(names.toArray(new String[0]), (dialog, which) ->
                                confirm("Asignar horario", "¿Usar " + names.get(which) + " para " + cargoName + "?",
                                        () -> assignTemplate(cargoId, ids.get(which), bearer)))
                        .setNegativeButton(android.R.string.cancel, null).show();
            }
            @Override public void onFailure(Call<ApiResponse<JsonArray>> call, Throwable error) {
                if (isAdded()) toast("No se pudieron cargar las plantillas.");
            }
        });
    }

    private void assignTemplate(int cargoId, String templateId, String bearer) {
        ApiClient.getScheduleService().assign(bearer, cargoId, templateId)
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                        if (!isAdded()) return;
                        ApiResponse<JsonObject> body = response.body();
                        if (!response.isSuccessful() || body == null || !body.isSuccessful()) {
                            toast("No se pudo asignar el horario.");
                            return;
                        }
                        repo.refreshRemote(success -> {
                            if (!isAdded()) return;
                            if (!success) { toast("Asignado; vuelve a abrir el editor para verlo."); return; }
                            ScheduleProfile assigned = repo.get("cargo_" + cargoId);
                            if (assigned != null) { profile = assigned; bind(); }
                            toast("Horario asignado en el servidor.");
                        });
                    }
                    @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                        if (isAdded()) toast("No se pudo asignar el horario.");
                    }
                });
    }

    private void newSchedule() {
        final EditText input = new EditText(requireContext());
        input.setHint("Nombre del cargo");
        new AlertDialog.Builder(requireContext())
                .setTitle("Nuevo horario (ficha vacia)")
                .setView(pad(input))
                .setPositiveButton("Crear", (d, w) -> {
                    ScheduleProfile p = repo.createBlank(input.getText().toString());
                    if (p == null) {
                        toast("Nombre vacio o ya existente.");
                    } else {
                        profile = p;
                        bind();
                        zoom.post(() -> zoom.resetZoom());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirm(String title, String msg, Runnable onYes) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Aceptar", (d, w) -> onYes.run())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private View pad(View v) {
        FrameLayout f = new FrameLayout(requireContext());
        f.setPadding(dp(20), dp(8), dp(20), 0);
        f.addView(v);
        return f;
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    private int roleIconRes(String roleName) {
        String k = roleName == null ? "" : roleName.toUpperCase(Locale.ROOT);
        if (k.contains("VIDEO")) return R.drawable.kpi_ic_video;
        if (k.contains("FOTO")) return R.drawable.kpi_ic_camera_pro;
        if (k.contains("SENIOR")) return R.drawable.kpi_ic_senior_crown;
        if (k.contains("DISE")) return R.drawable.kpi_ic_design_pen;
        if (k.contains("PROGRAM")) return R.drawable.kpi_ic_code;
        if (k.contains("TRAFFICK")) return R.drawable.kpi_ic_trending_up;
        if (k.contains("VENTA")) return R.drawable.kpi_ic_sales_briefcase;
        if (k.contains("CONTAD")) return R.drawable.kpi_ic_chart;
        if (k.contains("COMMUNITY")) return R.drawable.kpi_ic_users;
        if (k.contains("GERENTE") || k.contains("ADMIN")) return R.drawable.kpi_ic_briefcase;
        return R.drawable.sch_ic_schedule;
    }

    // ------------------------------------------------------------------------ construcción visual de la lámina

    private void bind() {
        cargoBtn.setText(profile.roleName);
        if (repo.hasAlert(profile.id)) {
            alertText.setText("Las metas de " + profile.roleName
                    + " cambiaron en el Editor de KPI. Revisa y ajusta este horario.");
            alertBar.setVisibility(View.VISIBLE);
        } else {
            alertBar.setVisibility(View.GONE);
        }
        buildPosterSheet();
    }

    private void buildPosterSheet() {
        sheetHolder.removeAllViews();
        final int hourW = 124;
        final int dayW = 152;
        final int days = ScheduleData.DAYS.length;
        final int totalTableW = hourW + dayW * days; // 1036dp -> proporción exacta de lámina horizontal

        // ==================== 1. ENCABEZADO CORPORATIVO ====================
        LinearLayout headerBlock = new LinearLayout(requireContext());
        headerBlock.setOrientation(LinearLayout.HORIZONTAL);
        headerBlock.setGravity(Gravity.CENTER_VERTICAL);
        headerBlock.setLayoutParams(new LinearLayout.LayoutParams(dp(totalTableW), ViewGroup.LayoutParams.WRAP_CONTENT));
        headerBlock.setPadding(dp(8), dp(2), dp(8), dp(14));

        // Icono izquierdo sobre la onda morada
        FrameLayout leftIconWrap = new FrameLayout(requireContext());
        leftIconWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(92), dp(72)));
        ImageView leftIcon = new ImageView(requireContext());
        leftIcon.setImageResource(roleIconRes(profile.roleName));
        leftIcon.setColorFilter(Color.WHITE);
        FrameLayout.LayoutParams liLp = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER);
        leftIconWrap.addView(leftIcon, liLp);
        headerBlock.addView(leftIconWrap);

        // Bloque central: Título + Pastilla de Cargo + Pastilla de Pilares
        LinearLayout centerTitles = new LinearLayout(requireContext());
        centerTitles.setOrientation(LinearLayout.VERTICAL);
        centerTitles.setGravity(Gravity.CENTER_HORIZONTAL);
        centerTitles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView mainTitle = text("HORARIO DE ACTIVIDADES DIARIAS", 24f, PURPLE_DARK, true);
        mainTitle.setLetterSpacing(0.03f);
        mainTitle.setGravity(Gravity.CENTER);
        centerTitles.addView(mainTitle);

        // Fila: ---- [ CARGO ] ----
        LinearLayout roleRow = new LinearLayout(requireContext());
        roleRow.setOrientation(LinearLayout.HORIZONTAL);
        roleRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams roleRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        roleRowLp.topMargin = dp(4);
        roleRow.setLayoutParams(roleRowLp);

        View leftLine = new View(requireContext());
        leftLine.setBackgroundColor(PURPLE_MAIN);
        roleRow.addView(leftLine, new LinearLayout.LayoutParams(dp(90), dp(2)));

        TextView rolePill = text(profile.roleName.toUpperCase(Locale.ROOT), 17f, PURPLE_DARK, true);
        rolePill.setGravity(Gravity.CENTER);
        rolePill.setPadding(dp(28), dp(3), dp(28), dp(3));
        GradientDrawable rolePillBg = new GradientDrawable();
        rolePillBg.setColor(PURPLE_PILL);
        rolePillBg.setCornerRadius(dp(18));
        rolePill.setBackground(rolePillBg);
        LinearLayout.LayoutParams rpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rpLp.setMarginStart(dp(12));
        rpLp.setMarginEnd(dp(12));
        roleRow.addView(rolePill, rpLp);

        View rightLine = new View(requireContext());
        rightLine.setBackgroundColor(PURPLE_MAIN);
        roleRow.addView(rightLine, new LinearLayout.LayoutParams(dp(90), dp(2)));
        centerTitles.addView(roleRow);

        // Pastilla de pilares / subtítulo
        String pillarsText = buildPillarsBannerText();
        TextView pillarsPill = text(pillarsText, 12.5f, PURPLE_DARK, true);
        pillarsPill.setGravity(Gravity.CENTER);
        pillarsPill.setPadding(dp(26), dp(5), dp(26), dp(5));
        GradientDrawable pillarsBg = new GradientDrawable();
        pillarsBg.setColor(PURPLE_PILL);
        pillarsBg.setCornerRadius(dp(16));
        pillarsPill.setBackground(pillarsBg);
        LinearLayout.LayoutParams ppLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ppLp.topMargin = dp(8);
        centerTitles.addView(pillarsPill, ppLp);

        headerBlock.addView(centerTitles);

        // Icono derecho corporativo
        FrameLayout rightIconWrap = new FrameLayout(requireContext());
        rightIconWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(92), dp(72)));
        GradientDrawable rightCircle = new GradientDrawable();
        rightCircle.setShape(GradientDrawable.OVAL);
        rightCircle.setColor(PURPLE_PILL);
        rightCircle.setStroke(dp(2), PURPLE_MAIN);
        ImageView rightIcon = new ImageView(requireContext());
        rightIcon.setImageResource(R.drawable.sch_ic_target);
        rightIcon.setColorFilter(PURPLE_DARK);
        rightIcon.setBackground(rightCircle);
        rightIcon.setPadding(dp(11), dp(11), dp(11), dp(11));
        rightIconWrap.addView(rightIcon, new FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER));
        headerBlock.addView(rightIconWrap);

        sheetHolder.addView(headerBlock);

        // ==================== 2. TABLA PRINCIPAL CON BORDE REDONDEADO ====================
        LinearLayout tableCard = new LinearLayout(requireContext());
        tableCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable tableOutline = new GradientDrawable();
        tableOutline.setColor(Color.WHITE);
        tableOutline.setCornerRadius(dp(14));
        tableOutline.setStroke(dp(2), PURPLE_MAIN);
        tableCard.setBackground(tableOutline);
        tableCard.setClipToOutline(true);
        tableCard.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        tableCard.setLayoutParams(new LinearLayout.LayoutParams(dp(totalTableW), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Fila de encabezados con degradado morado
        LinearLayout headerRow = new LinearLayout(requireContext());
        GradientDrawable hdrGrad = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{PURPLE_DARK, PURPLE_MAIN, PURPLE_DARK});
        headerRow.setBackground(hdrGrad);

        // Celda HORA con icono de reloj
        LinearLayout hourHdr = new LinearLayout(requireContext());
        hourHdr.setOrientation(LinearLayout.HORIZONTAL);
        hourHdr.setGravity(Gravity.CENTER);
        hourHdr.setPadding(dp(6), dp(10), dp(6), dp(10));
        hourHdr.setLayoutParams(new LinearLayout.LayoutParams(dp(hourW), dp(44)));
        ImageView clockIc = new ImageView(requireContext());
        clockIc.setImageResource(R.drawable.kpi_ic_clock);
        clockIc.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams ckLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        ckLp.setMarginEnd(dp(6));
        hourHdr.addView(clockIc, ckLp);
        hourHdr.addView(text("HORA", 13.5f, Color.WHITE, true));
        headerRow.addView(hourHdr);

        for (int d = 0; d < days; d++) {
            View vDiv = new View(requireContext());
            vDiv.setBackgroundColor(Color.parseColor("#A855F7"));
            headerRow.addView(vDiv, new LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT));

            TextView dh = text(ScheduleData.DAYS[d].toUpperCase(Locale.ROOT), 13.5f, Color.WHITE, true);
            dh.setGravity(Gravity.CENTER);
            dh.setLayoutParams(new LinearLayout.LayoutParams(dp(dayW) - dp(1), dp(44)));
            headerRow.addView(dh);
        }
        tableCard.addView(headerRow);

        // Filas de franjas horarias
        for (int rowIdx = 0; rowIdx < profile.slots.size(); rowIdx++) {
            final Slot slot = profile.slots.get(rowIdx);
            boolean isBreak = slot.kind == Slot.Kind.BREAK;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);

            int hourBgColor = isBreak ? PURPLE_BREAK_BG : PURPLE_SOFT_BG;
            TextView hourCell = gridCell(slot.label, hourW, hourBgColor, PURPLE_DARK, Typeface.BOLD);
            hourCell.setOnClickListener(v -> editHour(slot));
            hourCell.setOnLongClickListener(v -> {
                confirm("Eliminar fila", "Se elimina la fila " + slot.label + ".", () -> {
                    profile.slots.remove(slot);
                    repo.save(profile);
                });
                return true;
            });
            row.addView(hourCell);

            for (int d = 0; d < days; d++) {
                final int day = d;
                String cellText = slot.itemFor(d);
                if (isBreak) {
                    LinearLayout breakCell = new LinearLayout(requireContext());
                    breakCell.setOrientation(LinearLayout.HORIZONTAL);
                    breakCell.setGravity(Gravity.CENTER);
                    breakCell.setPadding(dp(6), dp(8), dp(6), dp(8));
                    breakCell.setMinimumHeight(dp(40));
                    GradientDrawable bBg = new GradientDrawable();
                    bBg.setColor(PURPLE_BREAK_BG);
                    bBg.setStroke(Math.max(1, dp(0.6f)), GRID_LINE);
                    breakCell.setBackground(bBg);
                    breakCell.setLayoutParams(new LinearLayout.LayoutParams(dp(dayW), ViewGroup.LayoutParams.MATCH_PARENT));

                    ImageView cup = new ImageView(requireContext());
                    cup.setImageResource(R.drawable.sch_ic_breakfast);
                    cup.setColorFilter(PURPLE_MAIN);
                    LinearLayout.LayoutParams cupLp = new LinearLayout.LayoutParams(dp(18), dp(18));
                    cupLp.setMarginEnd(dp(6));
                    breakCell.addView(cup, cupLp);

                    String bLabel = cellText.isEmpty() ? "BREAK" : cellText.toUpperCase(Locale.ROOT);
                    breakCell.addView(text(bLabel, 12f, PURPLE_DARK, Typeface.BOLD_ITALIC));
                    breakCell.setOnClickListener(v -> editCell(slot, day));
                    row.addView(breakCell);
                } else {
                    int bgColor = rowCellBg(slot.kind);
                    int txColor = slot.kind == Slot.Kind.OFF ? Color.parseColor("#6B7280") : INK_DARK;
                    int style = (slot.kind == Slot.Kind.ENTRY || slot.kind == Slot.Kind.EXIT)
                            ? Typeface.BOLD : Typeface.NORMAL;
                    TextView c = gridCell(cellText, dayW, bgColor, txColor, style);
                    c.setOnClickListener(v -> editCell(slot, day));
                    row.addView(c);
                }
            }
            tableCard.addView(row);
        }
        sheetHolder.addView(tableCard);

        // ==================== 3. TARJETA DE METAS PRINCIPALES / KPIs ====================
        LinearLayout goalsCard = new LinearLayout(requireContext());
        goalsCard.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable goalsCardBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{PURPLE_DARK, PURPLE_MAIN});
        goalsCardBg.setCornerRadius(dp(18));
        goalsCard.setBackground(goalsCardBg);
        goalsCard.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams gcLp = new LinearLayout.LayoutParams(
                dp(totalTableW), ViewGroup.LayoutParams.WRAP_CONTENT);
        gcLp.topMargin = dp(18);
        goalsCard.setLayoutParams(gcLp);

        // Panel morado izquierdo (Icono + Título apilado)
        LinearLayout goalsLeft = new LinearLayout(requireContext());
        goalsLeft.setOrientation(LinearLayout.HORIZONTAL);
        goalsLeft.setGravity(Gravity.CENTER);
        goalsLeft.setPadding(dp(18), dp(16), dp(16), dp(16));
        goalsLeft.setLayoutParams(new LinearLayout.LayoutParams(dp(248), ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView goalBadgeIcon = new ImageView(requireContext());
        goalBadgeIcon.setImageResource(R.drawable.sch_ic_target);
        goalBadgeIcon.setColorFilter(Color.WHITE);
        LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        gbLp.setMarginEnd(dp(12));
        goalsLeft.addView(goalBadgeIcon, gbLp);

        String leftHeading = profile.goalsTabLabel != null && profile.goalsTabLabel.toUpperCase(Locale.ROOT).contains("KPI")
                ? "METAS\nPRINCIPALES\nY KPIs"
                : "METAS\nPRINCIPALES\nPOR DÍA";
        TextView goalsLabel = text(leftHeading, 15f, Color.WHITE, true);
        goalsLabel.setLineSpacing(dp(2), 1f);
        goalsLeft.addView(goalsLabel);
        goalsCard.addView(goalsLeft);

        // Panel blanco derecho con tabla de metas
        LinearLayout goalsRight = new LinearLayout(requireContext());
        goalsRight.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rightBg = new GradientDrawable();
        rightBg.setColor(Color.WHITE);
        rightBg.setCornerRadius(dp(14));
        goalsRight.setBackground(rightBg);
        goalsRight.setPadding(dp(16), dp(8), dp(16), dp(8));
        goalsRight.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (profile.goals.isEmpty()) {
            TextView empty = text("Sin metas configuradas. Toca \"+ Meta\" arriba para agregar.", 12.5f, INK_MUTED, Typeface.ITALIC);
            empty.setPadding(dp(8), dp(14), dp(8), dp(14));
            goalsRight.addView(empty);
        } else {
            for (int i = 0; i < profile.goals.size(); i++) {
                final ScheduleKpi k = profile.goals.get(i);
                boolean master = ScheduleRepository.MASTER_CHIP.equals(k.chipText);
                LinearLayout gr = new LinearLayout(requireContext());
                gr.setOrientation(LinearLayout.HORIZONTAL);
                gr.setGravity(Gravity.CENTER_VERTICAL);
                gr.setPadding(dp(6), dp(7), dp(6), dp(7));
                if (master) gr.setBackgroundColor(Color.parseColor("#FAF5FF"));

                String chip = k.chipText != null ? k.chipText : (k.weekly ? "SEMANAL" : "MENSUAL");
                TextView colTag = text(chip.toUpperCase(Locale.ROOT), 11.5f, PURPLE_DARK, true);
                colTag.setLayoutParams(new LinearLayout.LayoutParams(dp(116), ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView colName = text(k.name, 12.5f, INK_DARK, Typeface.ITALIC);
                colName.setPadding(dp(8), 0, dp(10), 0);
                colName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView colTarget = text(k.target, 12.5f, PURPLE_DARK, Typeface.BOLD_ITALIC);
                colTarget.setGravity(Gravity.END);
                colTarget.setLayoutParams(new LinearLayout.LayoutParams(dp(205), ViewGroup.LayoutParams.WRAP_CONTENT));

                gr.addView(colTag);
                gr.addView(colName);
                gr.addView(colTarget);
                gr.setOnClickListener(v -> editGoal(k));
                goalsRight.addView(gr);

                if (i < profile.goals.size() - 1) {
                    View div = new View(requireContext());
                    div.setBackgroundColor(GRID_LINE);
                    goalsRight.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                }
            }
        }
        goalsCard.addView(goalsRight);
        sheetHolder.addView(goalsCard);

        // ==================== 4. PIE DE LÁMINA (PASTILLA MORADA) ====================
        LinearLayout footerRow = new LinearLayout(requireContext());
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams frLp = new LinearLayout.LayoutParams(
                dp(totalTableW), ViewGroup.LayoutParams.WRAP_CONTENT);
        frLp.topMargin = dp(16);
        footerRow.setLayoutParams(frLp);

        View fLeft = new View(requireContext());
        fLeft.setBackgroundColor(PURPLE_MAIN);
        footerRow.addView(fLeft, new LinearLayout.LayoutParams(dp(120), dp(2.5f)));

        String footerText = (profile.motto != null && !profile.motto.trim().isEmpty())
                ? profile.motto.trim()
                : ("Total semanal: " + profile.goals.size() + " metas principales  |  Toca cualquier celda para editar");
        TextView footerPill = text(footerText, 13.5f, Color.WHITE, Typeface.BOLD_ITALIC);
        footerPill.setGravity(Gravity.CENTER);
        footerPill.setPadding(dp(28), dp(7), dp(28), dp(7));
        GradientDrawable fpBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{PURPLE_MAIN, PURPLE_DARK});
        fpBg.setCornerRadius(dp(20));
        footerPill.setBackground(fpBg);
        LinearLayout.LayoutParams fpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fpLp.setMarginStart(dp(16));
        fpLp.setMarginEnd(dp(16));
        footerRow.addView(footerPill, fpLp);

        View fRight = new View(requireContext());
        fRight.setBackgroundColor(PURPLE_MAIN);
        footerRow.addView(fRight, new LinearLayout.LayoutParams(dp(120), dp(2.5f)));
        sheetHolder.addView(footerRow);
    }

    private String buildPillarsBannerText() {
        if (profile.pillars != null && profile.pillars.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < profile.pillars.length; i++) {
                if (i > 0) sb.append("   •   ");
                sb.append(profile.pillars[i].toUpperCase(Locale.ROOT));
            }
            return sb.toString();
        }
        if (profile.subtitle != null && !profile.subtitle.trim().isEmpty()) {
            return profile.subtitle.trim().toUpperCase(Locale.ROOT);
        }
        return "PLANIFICACIÓN   •   EJECUCIÓN   •   CONTROL   •   SEGUIMIENTO   •   ENTREGA";
    }

    private int rowCellBg(Slot.Kind k) {
        switch (k) {
            case ENTRY:
                return Color.parseColor("#F3FBF7");
            case EXIT:
                return Color.parseColor("#FDF4F5");
            case OFF:
                return Color.parseColor("#F3F4F6");
            default:
                return Color.WHITE;
        }
    }

    private TextView gridCell(String s, int widthDp, int bgColor, int textColor, int typefaceStyle) {
        TextView t = text(s, 11.8f, textColor, typefaceStyle);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(7), dp(7), dp(7), dp(7));
        t.setMinHeight(dp(40));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setStroke(Math.max(1, dp(0.6f)), GRID_LINE);
        t.setBackground(bg);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(widthDp), ViewGroup.LayoutParams.MATCH_PARENT));
        return t;
    }

    // ------------------------------------------------------------------------ edición directa

    private void editCell(final Slot slot, final int day) {
        final EditText input = new EditText(requireContext());
        input.setText(slot.itemFor(day));
        input.setHint("Actividad (vacio = sin actividad)");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        new AlertDialog.Builder(requireContext())
                .setTitle(ScheduleData.DAYS[day] + "  " + slot.label)
                .setView(pad(input))
                .setPositiveButton("Guardar", (d, w) -> {
                    String v = input.getText().toString().trim();
                    if (v.isEmpty()) {
                        slot.items.remove(day);
                    } else {
                        slot.items.put(day, v);
                    }
                    repo.save(profile);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void editHour(final Slot slot) {
        final EditText input = new EditText(requireContext());
        input.setText(slot.label);
        input.setHint("08:00 - 09:00");
        new AlertDialog.Builder(requireContext())
                .setTitle("Hora de la fila")
                .setView(pad(input))
                .setPositiveButton("Guardar", (d, w) -> {
                    if (slot.applyLabel(input.getText().toString())) {
                        repo.save(profile);
                    } else {
                        toast("Formato invalido. Usa 08:00 - 09:00");
                    }
                })
                .setNeutralButton("Eliminar fila", (d, w) -> {
                    confirm("Eliminar fila", "Se elimina la fila " + slot.label + ".", () -> {
                        profile.slots.remove(slot);
                        repo.save(profile);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteRowPicker() {
        if (profile.slots.isEmpty()) {
            toast("No hay filas para eliminar.");
            return;
        }
        String[] labels = new String[profile.slots.size()];
        for (int i = 0; i < profile.slots.size(); i++) {
            Slot s = profile.slots.get(i);
            String preview = s.itemFor(0);
            labels[i] = preview.isEmpty() ? s.label : (s.label + "  -  " + preview);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona la fila a eliminar")
                .setItems(labels, (d, which) -> {
                    final Slot target = profile.slots.get(which);
                    confirm("Eliminar fila", "Se elimina la fila " + target.label + ".", () -> {
                        profile.slots.remove(target);
                        repo.save(profile);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteGoalPicker() {
        if (profile.goals.isEmpty()) {
            toast("No hay metas para eliminar.");
            return;
        }
        String[] labels = new String[profile.goals.size()];
        for (int i = 0; i < profile.goals.size(); i++) {
            ScheduleKpi g = profile.goals.get(i);
            labels[i] = g.name + (g.target != null && !g.target.isEmpty() ? " (" + g.target + ")" : "");
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona la meta a eliminar")
                .setItems(labels, (d, which) -> {
                    final ScheduleKpi target = profile.goals.get(which);
                    confirm("Eliminar meta", "Se elimina la meta \"" + target.name + "\".", () -> {
                        profile.goals.remove(target);
                        repo.save(profile);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addSlot() {
        Slot s = new Slot("08:00 - 09:00", 480, 539, Slot.Kind.NORMAL);
        if (!profile.slots.isEmpty()) {
            Slot last = profile.slots.get(profile.slots.size() - 1);
            int start = Math.min(last.endMin + 1, 23 * 60);
            s.applyLabel(Slot.fmt(start) + " - " + Slot.fmt(Math.min(start + 60, 24 * 60 - 1)));
        }
        profile.slots.add(s);
        repo.save(profile);
        toast("Fila agregada. Toca la hora para ajustarla.");
    }

    private void editGoal(@Nullable final ScheduleKpi goal) {
        final boolean isNew = goal == null;
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText name = new EditText(requireContext());
        name.setHint("Meta / indicador");
        final EditText target = new EditText(requireContext());
        target.setHint("Objetivo (ej. 1 video >= 3 min)");
        final EditText period = new EditText(requireContext());
        period.setHint("Periodo o dia (ej. Lunes / Semanal)");
        if (!isNew) {
            name.setText(goal.name);
            target.setText(goal.target);
            period.setText(goal.chipText != null ? goal.chipText : (goal.weekly ? "Semanal" : "Mensual"));
        }
        box.addView(name);
        box.addView(target);
        box.addView(period);
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext())
                .setTitle(isNew ? "Nueva meta" : "Editar meta")
                .setView(pad(box))
                .setPositiveButton("Guardar", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        toast("Escribe el nombre de la meta.");
                        return;
                    }
                    String per = period.getText().toString().trim();
                    ScheduleKpi k = isNew ? new ScheduleKpi() : goal;
                    k.name = n;
                    k.target = target.getText().toString().trim();
                    k.weekly = !per.equalsIgnoreCase("Mensual");
                    k.chipText = per.isEmpty() ? null : per;
                    if (isNew) profile.goals.add(k);
                    repo.save(profile);
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (!isNew) {
            b.setNeutralButton("Eliminar", (d, w) -> {
                profile.goals.remove(goal);
                repo.save(profile);
            });
        }
        b.show();
    }

    /**
     * Contenedor de lámina que dibuja las ondas decorativas moradas en las esquinas
     * (idéntico al diseño corporativo de las fichas oficiales, nítido a cualquier nivel de zoom).
     */
    private static final class PosterSheetLayout extends LinearLayout {
        private final Paint waveDark = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint waveSoft = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        PosterSheetLayout(Context context) {
            super(context);
            setWillNotDraw(false);
            waveSoft.setColor(Color.parseColor("#E9D5FF"));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            waveDark.setShader(new LinearGradient(0, 0, w * 0.25f, h * 0.25f,
                    PURPLE_DARK, PURPLE_BRIGHT, Shader.TileMode.CLAMP));

            // Onda suave superior izquierda
            path.reset();
            path.moveTo(0, 0);
            path.lineTo(w * 0.23f, 0);
            path.cubicTo(w * 0.19f, h * 0.12f, w * 0.08f, h * 0.10f, 0, h * 0.16f);
            path.close();
            canvas.drawPath(path, waveSoft);

            // Onda principal superior izquierda
            path.reset();
            path.moveTo(0, 0);
            path.lineTo(w * 0.19f, 0);
            path.cubicTo(w * 0.16f, h * 0.10f, w * 0.06f, h * 0.08f, 0, h * 0.13f);
            path.close();
            canvas.drawPath(path, waveDark);

            // Onda suave superior derecha
            path.reset();
            path.moveTo(w, 0);
            path.lineTo(w * 0.78f, 0);
            path.cubicTo(w * 0.83f, h * 0.09f, w * 0.93f, h * 0.11f, w, h * 0.16f);
            path.close();
            canvas.drawPath(path, waveSoft);

            // Onda suave inferior izquierda
            path.reset();
            path.moveTo(0, h);
            path.lineTo(0, h * 0.88f);
            path.cubicTo(w * 0.08f, h * 0.92f, w * 0.16f, h * 0.96f, w * 0.24f, h);
            path.close();
            canvas.drawPath(path, waveSoft);

            // Onda principal inferior izquierda
            path.reset();
            path.moveTo(0, h);
            path.lineTo(0, h * 0.92f);
            path.cubicTo(w * 0.07f, h * 0.95f, w * 0.13f, h * 0.98f, w * 0.18f, h);
            path.close();
            canvas.drawPath(path, waveDark);

            // Onda suave inferior derecha
            path.reset();
            path.moveTo(w, h);
            path.lineTo(w, h * 0.83f);
            path.cubicTo(w * 0.95f, h * 0.91f, w * 0.88f, h * 0.96f, w * 0.78f, h);
            path.close();
            canvas.drawPath(path, waveSoft);

            // Onda principal inferior derecha
            path.reset();
            path.moveTo(w, h);
            path.lineTo(w, h * 0.89f);
            path.cubicTo(w * 0.95f, h * 0.94f, w * 0.90f, h * 0.98f, w * 0.84f, h);
            path.close();
            canvas.drawPath(path, waveDark);
        }
    }
}
