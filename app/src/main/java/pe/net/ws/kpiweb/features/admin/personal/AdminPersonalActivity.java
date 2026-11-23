package pe.net.ws.kpiweb.features.admin.personal;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalUiHelper.SortOption;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalUiHelper.Stats;
import pe.net.ws.kpiweb.features.admin.personal.adapter.PersonalAdapter;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRemoteMapper;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRepository;
import pe.net.ws.kpiweb.features.admin.personal.model.DepartmentSummary;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem.WorkStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla principal del módulo Personal (Vista Administrador — SIGD).
 * Buscador inmediato bajo el encabezado, filtros de estado y agencia con
 * contadores dinámicos
 * integrados dentro de cada chip y tarjetas de colaboradores con acceso a
 * Reportes y Perfil.
 */
public class AdminPersonalActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_REPORTS = "pe.net.ws.kpiweb.OPEN_ADMIN_REPORTS";

    private PersonalRepository repository;
    private PersonalAdapter adapter;

    private NestedScrollView scrollView;
    private ImageButton btnBack;
    private View searchContainer;
    private ImageView ivSearchIcon;
    private EditText etSearch;
    private ImageButton btnClearSearch;

    // Secciones de Filtros separadas (Estado y Agencia) con contadores inline
    private View filtersSection;
    private View agencySection;
    private LinearLayout layoutStatusChips;
    private LinearLayout layoutDeptChips;

    // Resultados y Ordenamiento
    private TextView tvResultsCount;
    private TextView btnResetQuick;
    private View btnSort;
    private TextView tvSortLabel;

    // Listado, carga y estado vacío
    private RecyclerView rvEmployees;
    private View layoutLoading;
    private View layoutEmpty;
    private View btnEmptyReset;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;

    private Call<ApiResponse<JsonObject>> pendingPage;
    private BottomSheetDialog sortSheetDialog;
    private int loadGeneration;
    private boolean loading;
    private boolean dataLoaded;
    private String loadError;
    private boolean reloadOnResume;
    private boolean openReportsAfterLoad;
    private boolean reportsSelectionMode;

    // Estado actual de filtros y orden
    private String selectedDepartment = "Todos";
    private WorkStatus selectedStatus = null; // null = Todos
    private String currentSearchQuery = "";
    private SortOption selectedSort = SortOption.ORIGINAL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_personal);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        repository = PersonalRepository.getInstance();
        repository.useLiveData();
        reportsSelectionMode = getIntent().getBooleanExtra(EXTRA_OPEN_REPORTS, false);

        initViews();
        setupHeader();
        setupSearch();
        setupStatusChips();
        setupDepartmentChips();
        setupRecyclerView();
        setupBottomNavigation();

        updateSortLabel();
        applyFilters();
        AdminPersonalTypography.apply(findViewById(android.R.id.content));
        startPersonalLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (reloadOnResume) {
            reloadOnResume = false;
            startPersonalLoad();
        }
    }

    @Override
    protected void onStop() {
        loadGeneration++;
        if (pendingPage != null)
            pendingPage.cancel();
        pendingPage = null;
        if (sortSheetDialog != null && sortSheetDialog.isShowing()) {
            sortSheetDialog.dismiss();
        }
        reloadOnResume = true;
        super.onStop();
    }

    private void startPersonalLoad() {
        if (pendingPage != null)
            pendingPage.cancel();
        pendingPage = null;
        int generation = ++loadGeneration;
        String bearer = new SessionManager(this).getBearerToken();
        if (bearer.isEmpty()) {
            showLoadError("Inicia sesión para consultar Personal");
            return;
        }
        loading = true;
        loadError = null;
        applyFilters();
        loadPersonalPage(new ArrayList<>(), 0, generation, bearer);
    }

    private void loadPersonalPage(List<EmployeeItem> collected, int offset,
            int generation, String bearer) {
        pendingPage = ApiClient.getService().getPersonal(bearer, 100, offset);
        pendingPage.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null)
                    response.errorBody().close();
                if (!isCurrentLoad(call, generation, bearer))
                    return;
                pendingPage = null;
                ApiResponse<JsonObject> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    showLoadError(response.code() == 401 ? "La sesión venció. Vuelve a iniciar sesión."
                            : response.code() == 403
                                    ? "Tu cuenta no tiene el permiso empleados.ver para consultar Personal."
                                    : "No se pudo consultar Personal (HTTP " + response.code() + ")");
                    return;
                }
                JsonObject data = body.getData();
                try {
                    JsonArray rows = data.getAsJsonArray("empleados");
                    if (rows == null) {
                        showLoadError("La respuesta de Personal está incompleta");
                        return;
                    }
                    for (JsonElement row : rows) {
                        if (row.isJsonObject()) {
                            collected.add(PersonalRemoteMapper.employee(row.getAsJsonObject()));
                        }
                    }
                    JsonObject pagination = data.getAsJsonObject("paginacion");
                    boolean hasMore = pagination != null && pagination.has("hay_mas")
                            && pagination.get("hay_mas").getAsBoolean();
                    if (hasMore && (rows.size() == 0 || offset + rows.size() > 100000)) {
                        showLoadError("La paginación de Personal está incompleta");
                    } else if (hasMore) {
                        loadPersonalPage(collected, offset + rows.size(), generation, bearer);
                    } else {
                        repository.replaceLiveEmployees(collected);
                        dataLoaded = true;
                        loading = false;
                        loadError = null;
                        setupDepartmentChips();
                        applyFilters();
                        if (openReportsAfterLoad) {
                            openReportsAfterLoad = false;
                            showReportsPicker();
                        }
                    }
                } catch (RuntimeException malformed) {
                    showLoadError("No se pudo interpretar la lista de Personal");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!isCurrentLoad(call, generation, bearer))
                    return;
                pendingPage = null;
                showLoadError("No hay conexión con Personal");
            }
        });
    }

    private boolean isCurrentLoad(Call<ApiResponse<JsonObject>> call, int generation, String bearer) {
        return !isFinishing() && !isDestroyed() && generation == loadGeneration
                && pendingPage == call && !call.isCanceled()
                && bearer.equals(new SessionManager(this).getBearerToken());
    }

    private void showLoadError(String message) {
        loading = false;
        loadError = message;
        applyFilters();
    }

    private void initViews() {
        scrollView = findViewById(R.id.personal_scroll);
        btnBack = findViewById(R.id.personal_btn_back);
        searchContainer = findViewById(R.id.personal_search_container);
        ivSearchIcon = findViewById(R.id.personal_iv_search_icon);
        etSearch = findViewById(R.id.personal_et_search);
        btnClearSearch = findViewById(R.id.personal_btn_clear_search);

        filtersSection = findViewById(R.id.personal_filters_section);
        agencySection = findViewById(R.id.personal_agency_section);
        layoutStatusChips = findViewById(R.id.personal_layout_status_chips);
        layoutDeptChips = findViewById(R.id.personal_layout_dept_chips);

        tvResultsCount = findViewById(R.id.personal_tv_results_count);
        btnResetQuick = findViewById(R.id.personal_btn_reset_quick);
        btnSort = findViewById(R.id.personal_btn_sort);
        tvSortLabel = findViewById(R.id.personal_tv_sort_label);

        rvEmployees = findViewById(R.id.personal_rv_employees);
        layoutLoading = findViewById(R.id.personal_layout_loading);
        layoutEmpty = findViewById(R.id.personal_layout_empty);
        btnEmptyReset = findViewById(R.id.personal_btn_empty_reset);
        tvEmptyTitle = findViewById(R.id.personal_tv_empty_title);
        tvEmptyDesc = findViewById(R.id.personal_tv_empty_desc);
    }

    private void setupHeader() {
        btnBack.setOnClickListener(v -> finish());
        updateHeaderMode();
    }

    private void updateHeaderMode() {
        TextView tvTitle = findViewById(R.id.personal_tv_header_title);
        TextView tvSubtitle = findViewById(R.id.personal_tv_header_subtitle);
        if (tvTitle != null) {
            tvTitle.setText(reportsSelectionMode ? "Reportes KPI" : getString(R.string.personal_title));
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText(reportsSelectionMode
                    ? "Toca Reportes en la tarjeta del trabajador"
                    : getString(R.string.personal_subtitle));
        }
        if (adapter != null) {
            adapter.setHighlightReportsAction(reportsSelectionMode);
        }
    }

    private void selectStatus(@Nullable WorkStatus status) {
        selectedStatus = status;
        applyFilters();
    }

    private void setupSearch() {
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (searchContainer != null)
                searchContainer.setActivated(hasFocus);
            if (ivSearchIcon != null) {
                ImageViewCompat.setImageTintList(ivSearchIcon, ColorStateList.valueOf(
                        hasFocus ? 0xFFC69BF5 : 0xFFC3AED3));
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = (s != null) ? s.toString() : "";
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            currentSearchQuery = "";
            applyFilters();
        });

        btnResetQuick.setOnClickListener(v -> {
            if (loadError != null)
                startPersonalLoad();
            else
                resetAllFilters();
        });

        btnEmptyReset.setOnClickListener(v -> {
            if (loadError != null)
                startPersonalLoad();
            else
                resetAllFilters();
        });

        attachPressFeedback(btnSort);
        btnSort.setOnClickListener(v -> showSortSheet());
    }

    private void setupStatusChips() {
        layoutStatusChips.removeAllViews();
        addStatusChip("Todos", null, R.drawable.admin_ic_users_round, 0xFFC69BF5);
        addStatusChip("Activos", WorkStatus.ACTIVO, R.drawable.personal_ic_user_round_check, 0xFF57D6A0);
        addStatusChip("Inactivos", WorkStatus.INACTIVO, R.drawable.personal_ic_user_round_x, 0xFFF28C9B);
    }

    private void addStatusChip(String label, @Nullable WorkStatus status, @DrawableRes int iconRes, int accentColor) {
        LinearLayout chip = createCounterChip(label, 0, iconRes, accentColor);
        styleCounterChip(chip, selectedStatus == status, accentColor, 0);
        chip.setOnClickListener(v -> selectStatus(status));
        layoutStatusChips.addView(chip);
    }

    private void updateStatusChipsVisuals(Stats counts) {
        WorkStatus[] ordered = new WorkStatus[] { null, WorkStatus.ACTIVO, WorkStatus.INACTIVO };
        int[] accents = new int[] { 0xFFC69BF5, 0xFF57D6A0, 0xFFF28C9B };
        int[] values = new int[] { counts.total, counts.active, counts.inactive };
        for (int i = 0; i < ordered.length && i < layoutStatusChips.getChildCount(); i++) {
            View child = layoutStatusChips.getChildAt(i);
            if (child instanceof LinearLayout) {
                styleCounterChip((LinearLayout) child, selectedStatus == ordered[i], accents[i], values[i]);
            }
        }
    }

    private void setupDepartmentChips() {
        layoutDeptChips.removeAllViews();
        List<DepartmentSummary> summaries = repository.getDepartmentSummaries();
        if (summaries.isEmpty()) {
            agencySection.setVisibility(View.GONE);
            return;
        }
        agencySection.setVisibility(View.VISIBLE);

        for (DepartmentSummary dept : summaries) {
            String displayName = AdminPersonalUiHelper.formatAgencyDisplayName(dept.getDepartmentName());
            boolean isSelected = dept.getDepartmentName().equalsIgnoreCase(selectedDepartment);
            LinearLayout chip = createCounterChip(displayName, dept.getTotalCount(),
                    R.drawable.personal_ic_building_2, 0xFFC69BF5);
            styleCounterChip(chip, isSelected, 0xFFC69BF5, dept.getTotalCount());
            chip.setOnClickListener(v -> {
                selectedDepartment = dept.getDepartmentName();
                applyFilters();
            });
            layoutDeptChips.addView(chip);
        }
    }

    private void updateDepartmentChipsVisuals(Map<String, Integer> dynamicCounts) {
        int count = layoutDeptChips.getChildCount();
        List<DepartmentSummary> summaries = repository.getDepartmentSummaries();
        for (int i = 0; i < count && i < summaries.size(); i++) {
            View child = layoutDeptChips.getChildAt(i);
            if (child instanceof LinearLayout) {
                DepartmentSummary summary = summaries.get(i);
                boolean isSelected = summary.getDepartmentName().equalsIgnoreCase(selectedDepartment);
                int chipCount = AdminPersonalUiHelper.agencyCountFor(dynamicCounts, summary.getDepartmentName());
                styleCounterChip((LinearLayout) child, isSelected, 0xFFC69BF5, chipCount);
            }
        }
    }

    private LinearLayout createCounterChip(String label, int initialCount,
            @DrawableRes int iconRes, int accentColor) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setMinimumHeight(dpToPx(38));
        chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(10), dpToPx(6));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(38));
        params.setMarginEnd(dpToPx(8));
        chip.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dpToPx(14), dpToPx(14));
        iconLp.setMarginEnd(dpToPx(6));
        icon.setLayoutParams(iconLp);
        icon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(accentColor));
        chip.addView(icon);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12.5f);
        tvLabel.setSingleLine(true);
        tvLabel.setTypeface(AdminPersonalTypography.readable(Typeface.BOLD));
        chip.addView(tvLabel);

        TextView tvCount = new TextView(this);
        LinearLayout.LayoutParams countLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        countLp.setMarginStart(dpToPx(7));
        tvCount.setLayoutParams(countLp);
        tvCount.setMinWidth(dpToPx(22));
        tvCount.setGravity(Gravity.CENTER);
        tvCount.setPadding(dpToPx(7), dpToPx(2), dpToPx(7), dpToPx(2));
        tvCount.setText(String.valueOf(initialCount));
        tvCount.setTextSize(11f);
        tvCount.setSingleLine(true);
        tvCount.setTypeface(AdminPersonalTypography.readable(Typeface.BOLD));
        chip.addView(tvCount);

        attachPressFeedback(chip);
        return chip;
    }

    private void styleCounterChip(LinearLayout chip, boolean isSelected, int accentColor, int countValue) {
        chip.setBackgroundResource(isSelected
                ? R.drawable.personal_bg_chip_selected_premium
                : R.drawable.personal_bg_chip_unselected_premium);

        if (chip.getChildCount() >= 3) {
            View iconView = chip.getChildAt(0);
            View labelView = chip.getChildAt(1);
            View countView = chip.getChildAt(2);
            if (iconView instanceof ImageView) {
                ImageViewCompat.setImageTintList((ImageView) iconView, ColorStateList.valueOf(
                        isSelected ? Color.WHITE : accentColor));
            }
            if (labelView instanceof TextView) {
                TextView tv = (TextView) labelView;
                tv.setTextColor(isSelected ? Color.WHITE : ContextCompat.getColor(this, R.color.personal_text_primary));
                tv.setTypeface(AdminPersonalTypography.readable(isSelected ? Typeface.BOLD : Typeface.NORMAL));
            }
            if (countView instanceof TextView) {
                TextView tvCount = (TextView) countView;
                tvCount.setText(String.valueOf(Math.max(0, countValue)));
                tvCount.setBackgroundResource(isSelected
                        ? R.drawable.personal_bg_chip_count_selected
                        : R.drawable.personal_bg_chip_count_unselected);
                tvCount.setTextColor(isSelected ? Color.WHITE : accentColor);
            }
        }
    }

    private void setupRecyclerView() {
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        rvEmployees.setItemAnimator(null);
        adapter = new PersonalAdapter();
        adapter.setHighlightReportsAction(reportsSelectionMode);
        adapter.setOnEmployeeClickListener(item -> {
            Intent intent = new Intent(this, AdminEmployeeDetailActivity.class);
            intent.putExtra(AdminEmployeeDetailActivity.EXTRA_EMPLOYEE_ID, item.getId());
            startActivity(intent);
        });
        adapter.setOnReportClickListener(item -> {
            try {
                pe.net.ws.kpiweb.features.kpi.ui.KpiServerReportBrowser.show(this,
                        Integer.parseInt(item.getId()));
            } catch (NumberFormatException ignored) {
            }
        });
        rvEmployees.setAdapter(adapter);
    }

    private void showSortSheet() {
        if (sortSheetDialog != null && sortSheetDialog.isShowing()) {
            sortSheetDialog.dismiss();
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        sortSheetDialog = dialog;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(ContextCompat.getColor(this, R.color.personal_card_bg));
        container.setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(24));

        TextView title = new TextView(this);
        title.setText("Ordenar colaboradores");
        title.setTextColor(ContextCompat.getColor(this, R.color.personal_text_primary));
        title.setTextSize(17f);
        title.setTypeface(AdminPersonalTypography.readable(Typeface.BOLD));
        container.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Aplica el criterio sobre los colaboradores cargados");
        subtitle.setTextColor(ContextCompat.getColor(this, R.color.personal_text_secondary));
        subtitle.setTextSize(12.5f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dpToPx(3);
        subLp.bottomMargin = dpToPx(14);
        container.addView(subtitle, subLp);

        for (SortOption option : SortOption.values()) {
            boolean active = option == selectedSort;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setClickable(true);
            row.setFocusable(true);
            row.setPadding(dpToPx(14), dpToPx(13), dpToPx(14), dpToPx(13));
            row.setBackgroundResource(active
                    ? R.drawable.personal_bg_stat_card
                    : R.drawable.personal_bg_card_premium);
            row.setSelected(active);

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dpToPx(8);

            TextView label = new TextView(this);
            label.setText(option.sheetLabel);
            label.setTextColor(ContextCompat.getColor(this,
                    active ? R.color.personal_text_primary : R.color.personal_text_secondary));
            label.setTextSize(14f);
            label.setTypeface(AdminPersonalTypography.readable(active ? Typeface.BOLD : Typeface.NORMAL));
            label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(label);

            if (active) {
                TextView badge = new TextView(this);
                badge.setText("Activo");
                badge.setTextColor(ContextCompat.getColor(this, R.color.personal_purple_accent));
                badge.setTextSize(11.5f);
                badge.setTypeface(AdminPersonalTypography.readable(Typeface.BOLD));
                row.addView(badge);
            }

            row.setOnClickListener(v -> {
                selectedSort = option;
                updateSortLabel();
                applyFilters();
                dialog.dismiss();
            });
            container.addView(row, rowLp);
        }

        dialog.setContentView(container);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void updateSortLabel() {
        if (tvSortLabel != null) {
            tvSortLabel.setText(selectedSort.buttonLabel);
        }
        if (btnSort != null) {
            boolean custom = selectedSort != SortOption.ORIGINAL;
            btnSort.setBackgroundResource(custom
                    ? R.drawable.personal_bg_chip_selected_premium
                    : R.drawable.personal_bg_chip_unselected_premium);
        }
    }

    private void applyFilters() {
        if (!dataLoaded && loading) {
            filtersSection.setVisibility(View.GONE);
            agencySection.setVisibility(View.GONE);
            btnSort.setVisibility(View.GONE);
            etSearch.setEnabled(false);
            etSearch.setAlpha(0.55f);
            adapter.submitList(new ArrayList<>());
            rvEmployees.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.VISIBLE);
            tvResultsCount.setText("Consultando colaboradores…");
            btnResetQuick.setVisibility(View.GONE);
            return;
        }

        if (!dataLoaded && loadError != null) {
            filtersSection.setVisibility(View.GONE);
            agencySection.setVisibility(View.GONE);
            btnSort.setVisibility(View.GONE);
            etSearch.setEnabled(false);
            etSearch.setAlpha(0.55f);
            adapter.submitList(new ArrayList<>());
            rvEmployees.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvResultsCount.setText("No se pudo cargar Personal");
            tvEmptyTitle.setText("Consulta no disponible");
            tvEmptyDesc.setText(loadError);
            ((TextView) btnEmptyReset).setText("Reintentar");
            btnEmptyReset.setVisibility(View.VISIBLE);
            btnResetQuick.setVisibility(View.GONE);
            return;
        }

        layoutLoading.setVisibility(View.GONE);
        filtersSection.setVisibility(View.VISIBLE);
        agencySection.setVisibility(repository.getDepartmentSummaries().isEmpty() ? View.GONE : View.VISIBLE);
        btnSort.setVisibility(View.VISIBLE);
        etSearch.setEnabled(true);
        etSearch.setAlpha(1f);

        // Contadores dinámicos de estado: respetan búsqueda y agencia, sin aplicar el
        // filtro de estado
        List<EmployeeItem> baseForStatus = repository.filter(currentSearchQuery, selectedDepartment, null);
        Stats statusCounts = AdminPersonalUiHelper.computeStats(baseForStatus);
        updateStatusChipsVisuals(statusCounts);

        // Contadores dinámicos de agencia: respetan búsqueda y estado, sin aplicar el
        // filtro de agencia
        List<EmployeeItem> baseForAgency = repository.filter(currentSearchQuery, "Todos", selectedStatus);
        Map<String, Integer> agencyCounts = AdminPersonalUiHelper.countByAgency(baseForAgency);
        updateDepartmentChipsVisuals(agencyCounts);

        // Listado final aplicando búsqueda + agencia + estado + orden seleccionado
        List<EmployeeItem> filtered = repository.filter(currentSearchQuery, selectedDepartment, selectedStatus);
        List<EmployeeItem> sorted = AdminPersonalUiHelper.sortEmployees(filtered, selectedSort);
        adapter.submitList(sorted);

        int total = repository.getTotalCount();
        int showing = sorted.size();

        boolean hasActiveFilters = (!currentSearchQuery.isEmpty()
                || !"Todos".equalsIgnoreCase(selectedDepartment)
                || selectedStatus != null);

        String countLabel = AdminPersonalUiHelper.formatResultsCount(showing, total, hasActiveFilters);
        tvResultsCount.setText((loadError != null ? "Última consulta · "
                : loading ? "Actualizando · " : "") + countLabel);

        btnResetQuick.setText(loadError == null ? "Limpiar filtros" : "Reintentar");
        btnResetQuick.setVisibility(hasActiveFilters || loadError != null ? View.VISIBLE : View.GONE);

        if (showing == 0) {
            rvEmployees.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            if (total == 0) {
                tvEmptyTitle.setText("Aún no hay colaboradores registrados");
                tvEmptyDesc.setText("Cuando existan colaboradores autorizados en tu alcance, aparecerán aquí.");
                btnEmptyReset.setVisibility(View.GONE);
            } else {
                tvEmptyTitle.setText("No encontramos colaboradores");
                tvEmptyDesc.setText("Prueba con otra búsqueda o ajusta los filtros");
                ((TextView) btnEmptyReset).setText("Limpiar filtros");
                btnEmptyReset.setVisibility(hasActiveFilters ? View.VISIBLE : View.GONE);
            }
        } else {
            rvEmployees.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            if (!isReduceMotionEnabled()) {
                rvEmployees.setAlpha(0.92f);
                rvEmployees.animate().alpha(1f).setDuration(160L).start();
            }
        }
    }

    private void resetAllFilters() {
        selectedDepartment = "Todos";
        selectedStatus = null;
        currentSearchQuery = "";
        selectedSort = SortOption.ORIGINAL;
        etSearch.setText("");
        updateSortLabel();
        applyFilters();
    }

    private void showReportsPicker() {
        if (!dataLoaded) {
            if (!loading)
                startPersonalLoad();
            Toast.makeText(this, "Cargando trabajadores autorizados para consultar reportes", Toast.LENGTH_SHORT)
                    .show();
            openReportsAfterLoad = true;
            return;
        }
        if (scrollView != null)
            scrollView.smoothScrollTo(0, 0);
        Toast.makeText(this, "Toca Reportes en la tarjeta del colaborador para abrir sus reportes recibidos.",
                Toast.LENGTH_LONG).show();
    }

    private void setupBottomNavigation() {
        ViewGroup tabStaff = findViewById(R.id.personal_nav_tab_staff);
        if (tabStaff != null) {
            ImageView icStaff = findViewById(R.id.personal_nav_ic_staff);
            if (icStaff != null) {
                icStaff.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.personal_purple_accent)));
            }
            TextView tvStaff = findViewById(R.id.personal_nav_tv_staff);
            if (tvStaff != null) {
                tvStaff.setTextColor(ContextCompat.getColor(this, R.color.personal_text_primary));
                tvStaff.setTypeface(null, Typeface.BOLD);
            }
            View indicator = findViewById(R.id.personal_nav_indicator_staff);
            if (indicator != null)
                indicator.setVisibility(View.VISIBLE);

            tabStaff.setOnClickListener(v -> {
                reportsSelectionMode = false;
                updateHeaderMode();
                if (scrollView != null)
                    scrollView.smoothScrollTo(0, 0);
            });
        }

        View tabReports = findViewById(R.id.personal_nav_tab_reports);
        if (tabReports != null) {
            tabReports.setOnClickListener(v -> {
                Intent reports = new Intent(this,
                        pe.net.ws.kpiweb.features.admin.dashboard.AdminKpiOfficialActivity.class);
                reports.putExtra(
                        pe.net.ws.kpiweb.features.admin.dashboard.AdminKpiOfficialActivity.EXTRA_REPORTS_CENTER,
                        true);
                startActivity(reports);
            });
        }

        View tabHome = findViewById(R.id.personal_nav_tab_home);
        if (tabHome != null) {
            tabHome.setOnClickListener(v -> {
                Intent intent = new Intent(this,
                        pe.net.ws.kpiweb.features.admin.dashboard.AdminDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        View tabNotifs = findViewById(R.id.personal_nav_tab_notifs);
        if (tabNotifs != null) {
            tabNotifs.setOnClickListener(v -> startActivity(new Intent(this,
                    pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsActivity.class)));
        }

        View tabMenu = findViewById(R.id.personal_nav_tab_menu);
        if (tabMenu != null) {
            tabMenu.setOnClickListener(v -> startActivity(new Intent(this,
                    pe.net.ws.kpiweb.features.admin.dashboard.AdminModulesMenuActivity.class)));
        }
    }

    private void attachPressFeedback(View view) {
        if (view == null || isReduceMotionEnabled())
            return;
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.975f).scaleY(0.975f).setDuration(110L).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(130L).start();
            }
            return false;
        });
    }

    private boolean isReduceMotionEnabled() {
        try {
            return Settings.Global.getFloat(
                    getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
