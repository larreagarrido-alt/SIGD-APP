package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminRankingSummaryHelper.EmptyKind;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminRankingSummaryHelper.SummaryStats;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Read-only presentation of the server's closed and authorized ranking snapshots. */
public final class AdminRankingActivity extends AppCompatActivity {
    private static final long ANIM_DURATION_MS = 200L;

    private ViewGroup rootContent;
    private LinearLayout podium, list;
    private LinearLayout filterContent, agencySection, statusCard;
    private ChipGroup types, agencies;
    private FrameLayout frequencySegmented;
    private View frequencyIndicator, filterToggle, searchContainer, searchClear;
    private View clearFiltersBar, clearFiltersBtn;
    private View statusLoading, statusTrophy, statusIconBadge;
    private ImageView periodChevron, filterChevron, refreshIcon, searchIcon, statusIcon;
    private TextView statusTitle, statusMessage, statusPill, statusAction;
    private TextView periodLabel, podiumTitle, listTitle, filterToggleLabel;
    private TextView summaryPeriodBadge, statCollaborators, statEvaluated, statClassified, summaryFooter;
    private TextView freqSemanal, freqMensual;
    private EditText search;

    private Call<ApiResponse<JsonObject>> pending;
    private Call<ApiResponse<JsonObject>> detailPending;
    private Call<ApiResponse<JsonObject>> monthsPending;

    private final List<JsonObject> source = new ArrayList<>();
    private final List<JsonObject> closedPeriods = new ArrayList<>();
    private final Map<Integer, String> agencyNames = new LinkedHashMap<>();

    private String periodType = "MENSUAL";
    private String monthFilter;
    private String loadedPeriodKey = "";
    private String typeFilter = "Todos";
    private int agencyFilter = 0;
    private boolean filtersExpanded = false;
    private String originalToken = "";
    private int originalUser;
    private boolean loaded;

    private BottomSheetDialog periodSheetDialog;
    private View periodSheetRoot;

    @Override protected void onCreate(@Nullable Bundle state) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(state);
        setContentView(R.layout.admin_activity_ranking);
        AdminBottomNavigation.attach(this, 4);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        rootContent = findViewById(R.id.ranking_root_content);
        summaryPeriodBadge = findViewById(R.id.ranking_summary_period_badge);
        statCollaborators = findViewById(R.id.ranking_stat_collaborators);
        statEvaluated = findViewById(R.id.ranking_stat_evaluated);
        statClassified = findViewById(R.id.ranking_stat_classified);
        summaryFooter = findViewById(R.id.ranking_summary_footer);

        types = findViewById(R.id.ranking_type_filters);
        agencies = findViewById(R.id.ranking_agency_filters);
        agencySection = findViewById(R.id.ranking_agency_section);
        podium = findViewById(R.id.ranking_podium);
        list = findViewById(R.id.ranking_list);

        statusCard = findViewById(R.id.ranking_status_card);
        statusLoading = findViewById(R.id.ranking_status_loading);
        statusTrophy = findViewById(R.id.ranking_status_trophy);
        statusIconBadge = findViewById(R.id.ranking_status_icon_badge);
        statusIcon = findViewById(R.id.ranking_status_icon);
        statusTitle = findViewById(R.id.ranking_status_title);
        statusMessage = findViewById(R.id.ranking_status_message);
        statusPill = findViewById(R.id.ranking_status_pill);
        statusAction = findViewById(R.id.ranking_status_action);

        periodLabel = findViewById(R.id.ranking_period_label);
        podiumTitle = findViewById(R.id.ranking_podium_title);
        listTitle = findViewById(R.id.ranking_list_title);
        searchContainer = findViewById(R.id.ranking_search_container);
        searchIcon = findViewById(R.id.ranking_search_icon);
        search = findViewById(R.id.ranking_search);
        searchClear = findViewById(R.id.ranking_search_clear);

        periodChevron = findViewById(R.id.ranking_period_chevron);
        filterToggle = findViewById(R.id.ranking_filter_toggle);
        filterToggleLabel = findViewById(R.id.ranking_filter_toggle_label);
        filterChevron = findViewById(R.id.ranking_filter_chevron);
        filterContent = findViewById(R.id.ranking_filter_content);
        clearFiltersBar = findViewById(R.id.ranking_clear_filters_bar);
        clearFiltersBtn = findViewById(R.id.ranking_clear_filters_btn);
        refreshIcon = findViewById(R.id.ranking_refresh_icon);

        frequencySegmented = findViewById(R.id.ranking_frequency_segmented);
        frequencyIndicator = findViewById(R.id.ranking_frequency_indicator);
        freqSemanal = findViewById(R.id.ranking_freq_semanal);
        freqMensual = findViewById(R.id.ranking_freq_mensual);

        findViewById(R.id.ranking_back).setOnClickListener(v -> finish());
        findViewById(R.id.ranking_refresh).setOnClickListener(v -> {
            if (refreshIcon != null && !isReduceMotionEnabled()) {
                refreshIcon.animate().rotationBy(360f).setDuration(420L).start();
            }
            load();
            loadMonths();
        });

        View periodCard = findViewById(R.id.ranking_period_card);
        attachPressFeedback(periodCard);
        periodCard.setOnClickListener(v -> showPeriodSelectorSheet());

        filterToggle.setOnClickListener(v -> toggleFilterPanel());
        clearFiltersBtn.setOnClickListener(v -> clearFilters());

        setupFrequencySelector();
        setupSearch();

        filtersExpanded = false;
        filterContent.setVisibility(View.GONE);
        filterChevron.setRotation(0f);

        renderTypeChips();
        updateFrequencySelection(false);
        updateActiveFilterIndicators();
        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    @Override protected void onResume() {
        super.onResume();
        load();
        loadMonths();
    }

    @Override protected void onStop() {
        if (pending != null) pending.cancel();
        if (detailPending != null) detailPending.cancel();
        if (monthsPending != null) monthsPending.cancel();
        pending = null;
        detailPending = null;
        monthsPending = null;
        if (periodSheetDialog != null && periodSheetDialog.isShowing()) {
            periodSheetDialog.dismiss();
        }
        super.onStop();
    }

    private void setupSearch() {
        searchClear.setOnClickListener(v -> search.setText(""));
        search.setOnFocusChangeListener((v, hasFocus) -> {
            searchContainer.setActivated(hasFocus);
            ImageViewCompat.setImageTintList(searchIcon, ColorStateList.valueOf(
                    hasFocus ? 0xFFD4A9FF : 0xFFBCA6CC));
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasQuery = s != null && s.length() > 0;
                searchClear.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                render();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFrequencySelector() {
        freqSemanal.setOnClickListener(v -> Toast.makeText(this,
                "Aún no hay cierres semanales en el ranking del servidor.", Toast.LENGTH_LONG).show());
        freqMensual.setOnClickListener(v -> selectPeriod("MENSUAL"));
        frequencySegmented.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((right - left) != (oldRight - oldLeft) && (right - left) > 0) {
                positionFrequencyIndicator(false);
            }
        });
    }

    private void toggleFilterPanel() {
        if (!isReduceMotionEnabled()) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(ANIM_DURATION_MS);
            transition.setInterpolator(new DecelerateInterpolator());
            TransitionManager.beginDelayedTransition(rootContent, transition);
        }

        filtersExpanded = !filtersExpanded;
        filterContent.setVisibility(filtersExpanded ? View.VISIBLE : View.GONE);
        if (!isReduceMotionEnabled()) {
            filterChevron.animate()
                    .rotation(filtersExpanded ? 180f : 0f)
                    .setDuration(ANIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            filterChevron.setRotation(filtersExpanded ? 180f : 0f);
        }
        if (filtersExpanded) {
            frequencySegmented.post(() -> positionFrequencyIndicator(false));
        }
        updateActiveFilterIndicators();
    }

    private void updateFrequencySelection(boolean animate) {
        styleSegment(freqSemanal, false, true);
        styleSegment(freqMensual, "MENSUAL".equals(periodType), false);
        frequencySegmented.post(() -> positionFrequencyIndicator(animate && !isReduceMotionEnabled()));
    }

    private void styleSegment(TextView segment, boolean selected, boolean disabled) {
        segment.setSelected(selected);
        segment.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
        segment.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        segment.setAlpha(disabled ? 0.5f : 1f);
        AdminTypography.apply(this, segment);
    }

    private void positionFrequencyIndicator(boolean animate) {
        int innerWidth = frequencySegmented.getWidth()
                - frequencySegmented.getPaddingLeft()
                - frequencySegmented.getPaddingRight();
        if (innerWidth <= 0) return;
        int segmentCount = 2;
        int segmentWidth = Math.round(innerWidth / (float) segmentCount);
        int selectedIndex = "SEMANAL".equals(periodType) ? 0 : 1;
        float targetX = selectedIndex * (innerWidth / (float) segmentCount);

        ViewGroup.LayoutParams lp = frequencyIndicator.getLayoutParams();
        if (lp.width != segmentWidth) {
            lp.width = segmentWidth;
            frequencyIndicator.setLayoutParams(lp);
        }
        if (animate && frequencyIndicator.getVisibility() == View.VISIBLE && filterContent.getVisibility() == View.VISIBLE) {
            frequencyIndicator.animate()
                    .translationX(targetX)
                    .setDuration(ANIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            frequencyIndicator.animate().cancel();
            frequencyIndicator.setTranslationX(targetX);
        }
    }

    private boolean hasActiveFilters() {
        return !"MENSUAL".equals(periodType)
                || monthFilter != null
                || !"Todos".equals(typeFilter)
                || agencyFilter > 0;
    }

    private void clearFilters() {
        if (!hasActiveFilters()) return;
        boolean needsReload = !"MENSUAL".equals(periodType) || monthFilter != null;
        periodType = "MENSUAL";
        monthFilter = null;
        typeFilter = "Todos";
        agencyFilter = 0;

        if (!isReduceMotionEnabled()) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(ANIM_DURATION_MS);
            transition.setInterpolator(new DecelerateInterpolator());
            TransitionManager.beginDelayedTransition(rootContent, transition);
        }

        updateFrequencySelection(true);
        renderTypeChips();
        renderAgencyChips();
        updateActiveFilterIndicators();

        if (needsReload) {
            load();
        } else {
            render();
        }
    }

    private void updateActiveFilterIndicators() {
        boolean customFreq = !"MENSUAL".equals(periodType);
        boolean customAgency = agencyFilter > 0;

        // Evitar repetir "Mensual" innecesariamente cuando ya está indicado en el selector de período
        if (filterToggleLabel != null) {
            if (customFreq) {
                filterToggleLabel.setText(AdminRankingPeriodFormatter.frequencyLabel(periodType));
            } else if (customAgency) {
                String resolved = agencyNames.get(agencyFilter);
                filterToggleLabel.setText(resolved == null || resolved.trim().isEmpty()
                        ? "Agencia" : resolved.trim());
            } else {
                filterToggleLabel.setText("Frecuencia");
            }
        }
        filterToggle.setBackgroundResource((filtersExpanded || customFreq || customAgency)
                ? R.drawable.admin_bg_rewards_secondary_btn
                : R.drawable.admin_bg_rewards_chip_inactive);

        if (clearFiltersBar != null) {
            clearFiltersBar.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoadingStatus() {
        statusLoading.setVisibility(View.VISIBLE);
        statusTrophy.setVisibility(View.GONE);
        statusIconBadge.setVisibility(View.GONE);
        statusTitle.setText("Consultando resultados");
        statusMessage.setText("Cargando los KPI aprobados del período…");
        statusPill.setVisibility(View.GONE);
        statusAction.setVisibility(View.GONE);
        statusCard.setVisibility(View.VISIBLE);
    }

    private void showTrophyEmptyStatus(String title, String message, @Nullable String pillText) {
        statusLoading.setVisibility(View.GONE);
        statusTrophy.setVisibility(View.VISIBLE);
        statusIconBadge.setVisibility(View.GONE);
        statusTitle.setText(title);
        statusMessage.setText(message);
        if (pillText != null && !pillText.isEmpty()) {
            statusPill.setText(pillText);
            statusPill.setVisibility(View.VISIBLE);
        } else {
            statusPill.setVisibility(View.GONE);
        }
        statusAction.setVisibility(View.GONE);
        statusCard.setVisibility(View.VISIBLE);
    }

    private void showIconStatus(@DrawableRes int iconRes,
                                String title,
                                String message,
                                @Nullable String actionLabel,
                                @Nullable Runnable action) {
        statusLoading.setVisibility(View.GONE);
        statusTrophy.setVisibility(View.GONE);
        statusIconBadge.setVisibility(View.VISIBLE);
        statusIcon.setImageResource(iconRes);
        statusTitle.setText(title);
        statusMessage.setText(message);
        statusPill.setVisibility(View.GONE);
        if (actionLabel != null && action != null) {
            statusAction.setText(actionLabel);
            statusAction.setOnClickListener(v -> action.run());
            statusAction.setVisibility(View.VISIBLE);
        } else {
            statusAction.setVisibility(View.GONE);
        }
        statusCard.setVisibility(View.VISIBLE);
    }

    private void hideStatus() {
        statusCard.setVisibility(View.GONE);
    }

    private void showPeriodSelectorSheet() {
        if (periodSheetDialog != null && periodSheetDialog.isShowing()) {
            periodSheetDialog.dismiss();
        }
        periodSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.admin_dialog_ranking_period_sheet, null);
        periodSheetRoot = sheetView;
        periodSheetDialog.setContentView(sheetView);
        periodSheetDialog.setOnShowListener(d -> {
            View bottomSheet = periodSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        periodSheetDialog.setOnDismissListener(d -> {
            periodSheetRoot = null;
            if (!isReduceMotionEnabled()) {
                periodChevron.animate().rotation(0f).setDuration(ANIM_DURATION_MS).start();
            } else {
                periodChevron.setRotation(0f);
            }
        });

        sheetView.findViewById(R.id.ranking_sheet_close).setOnClickListener(v -> periodSheetDialog.dismiss());

        View latestAction = sheetView.findViewById(R.id.ranking_sheet_latest_action);
        TextView latestSubtitle = sheetView.findViewById(R.id.ranking_sheet_latest_subtitle);
        ImageView latestCheck = sheetView.findViewById(R.id.ranking_sheet_latest_check);
        boolean isLatestActive = monthFilter == null || !"MENSUAL".equals(periodType);
        latestAction.setBackgroundResource(isLatestActive
                ? R.drawable.admin_bg_ranking_sheet_item_active
                : R.drawable.admin_bg_ranking_sheet_item);
        latestCheck.setVisibility(isLatestActive ? View.VISIBLE : View.INVISIBLE);
        latestSubtitle.setText(AdminRankingPeriodFormatter.latestClosedSubtitle(periodType));
        latestAction.setOnClickListener(v -> {
            monthFilter = null;
            periodSheetDialog.dismiss();
            updateActiveFilterIndicators();
            load();
            loadMonths();
        });

        populatePeriodSheetList(sheetView);
        AdminTypography.apply(this, sheetView);

        if (!isReduceMotionEnabled()) {
            periodChevron.animate().rotation(180f).setDuration(ANIM_DURATION_MS).start();
        } else {
            periodChevron.setRotation(180f);
        }
        periodSheetDialog.show();
    }

    private void populatePeriodSheetList(View sheetView) {
        LinearLayout listContainer = sheetView.findViewById(R.id.ranking_sheet_periods_list);
        NestedScrollView scrollView = sheetView.findViewById(R.id.ranking_sheet_periods_scroll);
        TextView emptyView = sheetView.findViewById(R.id.ranking_sheet_empty);
        listContainer.removeAllViews();

        if (closedPeriods.isEmpty()) {
            emptyView.setText(monthsPending != null
                    ? "Cargando cierres mensuales disponibles…"
                    : "Aún no hay cierres mensuales anteriores registrados.");
            emptyView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);

        int limit = Math.min(closedPeriods.size(), 24);
        for (int i = 0; i < limit; i++) {
            JsonObject row = closedPeriods.get(i);
            String monthKey = str(row, "periodo_clave");
            if (monthKey.isEmpty()) continue;

            boolean selected = "MENSUAL".equals(periodType) && monthKey.equals(monthFilter);
            int participants = num(row, "participantes");
            int evaluations = num(row, "evaluaciones_aprobadas");

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setMinimumHeight(dp(54));
            item.setPadding(dp(12), dp(10), dp(14), dp(10));
            item.setClickable(true);
            item.setFocusable(true);
            item.setBackgroundResource(selected
                    ? R.drawable.admin_bg_ranking_sheet_item_active
                    : R.drawable.admin_bg_ranking_sheet_item);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.admin_ic_calendar);
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(
                    selected ? getColor(R.color.admin_purple_accent) : getColor(R.color.admin_text_secondary)));
            item.addView(icon, new LinearLayout.LayoutParams(dp(18), dp(18)));

            LinearLayout textStack = new LinearLayout(this);
            textStack.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams stackLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            stackLp.setMarginStart(dp(12));
            stackLp.setMarginEnd(dp(8));

            String formattedName = AdminRankingPeriodFormatter.formatPeriodKey(monthKey, "MENSUAL");
            TextView title = new TextView(this);
            title.setText(formattedName);
            title.setTextSize(14f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(getColor(R.color.admin_text_primary));
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            textStack.addView(title);

            StringBuilder meta = new StringBuilder(monthKey);
            if (participants > 0) {
                meta.append(" · ").append(participants)
                        .append(participants == 1 ? " participante" : " participantes");
            }
            if (evaluations > 0) {
                meta.append(" · ").append(evaluations)
                        .append(evaluations == 1 ? " evaluación" : " evaluaciones");
            }
            TextView subtitle = new TextView(this);
            subtitle.setText(meta.toString());
            subtitle.setTextSize(11.5f);
            subtitle.setTextColor(getColor(R.color.admin_text_secondary));
            subtitle.setSingleLine(true);
            subtitle.setEllipsize(TextUtils.TruncateAt.END);
            textStack.addView(subtitle);

            item.addView(textStack, stackLp);

            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.admin_ic_check);
            ImageViewCompat.setImageTintList(check, ColorStateList.valueOf(getColor(R.color.admin_purple_accent)));
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            item.addView(check, new LinearLayout.LayoutParams(dp(18), dp(18)));

            item.setContentDescription(formattedName + (selected ? ", seleccionado" : ""));
            item.setOnClickListener(v -> {
                monthFilter = monthKey;
                periodType = "MENSUAL";
                agencyFilter = 0;
                updateFrequencySelection(false);
                updateActiveFilterIndicators();
                if (periodSheetDialog != null && periodSheetDialog.isShowing()) {
                    periodSheetDialog.dismiss();
                }
                load();
            });

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            listContainer.addView(item, rowLp);
        }

        AdminTypography.apply(this, listContainer);
        scrollView.post(() -> {
            int maxPx = dp(280);
            ViewGroup.LayoutParams lp = scrollView.getLayoutParams();
            int desired = listContainer.getHeight() > maxPx ? maxPx : ViewGroup.LayoutParams.WRAP_CONTENT;
            if (lp.height != desired) {
                lp.height = desired;
                scrollView.setLayoutParams(lp);
            }
        });
    }

    private boolean sameAccount() {
        SessionManager now = new SessionManager(this);
        return now.isLoggedIn() && now.getUserId() == originalUser &&
                originalToken.equals(now.getAccessToken());
    }

    private void load() {
        if (pending != null) pending.cancel();
        if (detailPending != null) detailPending.cancel();
        pending = null;
        detailPending = null;
        source.clear();
        agencyNames.clear();
        loaded = false;
        clearResults();
        agencies.removeAllViews();
        agencySection.setVisibility(View.GONE);
        bindSummaryStats(new SummaryStats("—", "—", "—", "", false),
                monthFilter != null && "MENSUAL".equals(periodType)
                        ? AdminRankingPeriodFormatter.formatPeriodKey(monthFilter, periodType)
                        : "Consultando…");

        if (monthFilter != null && "MENSUAL".equals(periodType)) {
            periodLabel.setText(AdminRankingPeriodFormatter.formatPeriodKey(monthFilter, periodType));
        } else {
            periodLabel.setText("Consultando período…");
        }
        showLoadingStatus();

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() <= 0) {
            periodLabel.setText("Sesión requerida");
            summaryPeriodBadge.setText("Sin sesión");
            showIconStatus(R.drawable.admin_ic_info_circle,
                    "Sesión requerida",
                    "Inicia sesión para consultar el ranking.",
                    null, null);
            return;
        }
        originalUser = session.getUserId();
        originalToken = session.getAccessToken();
        pending = monthFilter == null || !"MENSUAL".equals(periodType)
                ? ApiClient.getService().getLatestRanking(session.getBearerToken(), periodType)
                : ApiClient.getService().getRankingMonth(session.getBearerToken(), periodType, monthFilter);
        pending.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != pending || call.isCanceled() || !sameAccount() || isFinishing()) return;
                pending = null;
                ApiResponse<JsonObject> body = response.body();
                if (response.code() == 401) {
                    periodLabel.setText("Sesión vencida");
                    summaryPeriodBadge.setText("Sesión vencida");
                    showIconStatus(R.drawable.admin_ic_info_circle,
                            "Sesión vencida",
                            "Vuelve a iniciar sesión para consultar el ranking.",
                            null, null);
                    return;
                }
                if (response.code() == 403) {
                    periodLabel.setText("Acceso restringido");
                    summaryPeriodBadge.setText("Sin permiso");
                    showIconStatus(R.drawable.admin_ic_info_circle,
                            "Acceso restringido",
                            "Tu cuenta no tiene permiso ranking.ver para consultar esta clasificación.",
                            null, null);
                    return;
                }
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    periodLabel.setText("No disponible");
                    summaryPeriodBadge.setText("Error");
                    showIconStatus(R.drawable.admin_ic_alert_triangle,
                            "No se pudo consultar el ranking",
                            "Ocurrió un problema al cargar los resultados del servidor.",
                            "Reintentar",
                            () -> { load(); loadMonths(); });
                    return;
                }
                JsonObject data = body.getData();
                JsonArray entries = array(data, "ranking");
                for (JsonElement element : entries) {
                    if (element.isJsonObject()) source.add(element.getAsJsonObject());
                }
                loaded = true;
                loadedPeriodKey = str(data, "periodo_clave");
                String formattedPeriod = AdminRankingPeriodFormatter.formatPeriodKey(loadedPeriodKey, periodType);
                periodLabel.setText(formattedPeriod);
                renderAgencyChips();
                updateActiveFilterIndicators();
                render();
            }

            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call != pending || call.isCanceled() || !sameAccount() || isFinishing()) return;
                pending = null;
                String fallbackPeriod = loadedPeriodKey.isEmpty()
                        ? "Sin conexión"
                        : AdminRankingPeriodFormatter.formatPeriodKey(loadedPeriodKey, periodType);
                periodLabel.setText(fallbackPeriod);
                summaryPeriodBadge.setText("Sin conexión");
                showIconStatus(R.drawable.admin_ic_alert_triangle,
                        "Sin conexión",
                        "No se pudo conectar con el servidor para consultar el ranking.",
                        "Reintentar",
                        () -> { load(); loadMonths(); });
            }
        });
    }

    private void bindSummaryStats(SummaryStats stats, String periodBadgeText) {
        if (summaryPeriodBadge != null) summaryPeriodBadge.setText(periodBadgeText);
        if (statCollaborators != null) statCollaborators.setText(stats.collaboratorsText);
        if (statEvaluated != null) statEvaluated.setText(stats.evaluatedText);
        if (statClassified != null) statClassified.setText(stats.classifiedText);
        if (summaryFooter != null) {
            if (stats.hasData && !stats.footerHighlight.isEmpty()) {
                summaryFooter.setText(stats.footerHighlight);
                summaryFooter.setVisibility(View.VISIBLE);
            } else {
                summaryFooter.setVisibility(View.GONE);
            }
        }
    }

    private void clearResults() {
        podium.removeAllViews();
        list.removeAllViews();
        podiumTitle.setVisibility(View.GONE);
        listTitle.setVisibility(View.GONE);
    }

    private void loadMonths() {
        if (monthsPending != null) monthsPending.cancel();
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) return;
        monthsPending = ApiClient.getService().getRankingPeriods(session.getBearerToken());
        monthsPending.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != monthsPending || !sameAccount() || isFinishing()) return;
                monthsPending = null;
                closedPeriods.clear();
                ApiResponse<JsonObject> body = response.body();
                if (response.isSuccessful() && body != null && body.isSuccessful() && body.getData() != null) {
                    JsonArray rows = body.getData().getAsJsonArray("periodos");
                    if (rows != null) {
                        for (int i = 0; i < rows.size() && i < 24; i++) {
                            if (rows.get(i).isJsonObject()) {
                                closedPeriods.add(rows.get(i).getAsJsonObject());
                            }
                        }
                    }
                }
                if (periodSheetDialog != null && periodSheetDialog.isShowing() && periodSheetRoot != null) {
                    populatePeriodSheetList(periodSheetRoot);
                }
            }

            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call == monthsPending) {
                    monthsPending = null;
                    if (periodSheetDialog != null && periodSheetDialog.isShowing() && periodSheetRoot != null) {
                        populatePeriodSheetList(periodSheetRoot);
                    }
                }
            }
        });
    }

    private void selectPeriod(String value) {
        boolean switchingToLatestSameType = periodType.equals(value) && monthFilter != null;
        if (periodType.equals(value) && !switchingToLatestSameType) return;
        periodType = value;
        monthFilter = null;
        agencyFilter = 0;
        updateFrequencySelection(true);
        updateActiveFilterIndicators();
        load();
    }

    private void renderTypeChips() {
        types.removeAllViews();
        addFilterChip(types, "Todos", R.drawable.admin_ic_team, "Todos".equals(typeFilter), () -> {
            if ("Todos".equals(typeFilter)) return;
            typeFilter = "Todos";
            renderTypeChips();
            updateActiveFilterIndicators();
            render();
        });
        addFilterChip(types, "Colaboradores", R.drawable.collab_ic_rec_users_round, "Colaborador".equals(typeFilter), () -> {
            if ("Colaborador".equals(typeFilter)) return;
            typeFilter = "Colaborador";
            renderTypeChips();
            updateActiveFilterIndicators();
            render();
        });
        addFilterChip(types, "Practicantes", R.drawable.admin_ic_intern, "Practicante".equals(typeFilter), () -> {
            if ("Practicante".equals(typeFilter)) return;
            typeFilter = "Practicante";
            renderTypeChips();
            updateActiveFilterIndicators();
            render();
        });
        AdminTypography.apply(this, types);
    }

    private void renderAgencyChips() {
        agencyNames.clear();
        for (JsonObject row : source) {
            int id = num(row, "id_agencia");
            if (id > 0) agencyNames.put(id, str(row, "agencia_nombre"));
        }
        agencies.removeAllViews();
        agencySection.setVisibility(agencyNames.size() > 1 ? View.VISIBLE : View.GONE);
        if (agencyNames.size() < 2) {
            agencyFilter = 0;
            return;
        }
        if (agencyFilter > 0 && !agencyNames.containsKey(agencyFilter)) {
            agencyFilter = 0;
        }
        addFilterChip(agencies, "Todas", R.drawable.admin_ic_building, agencyFilter == 0, () -> {
            if (agencyFilter == 0) return;
            agencyFilter = 0;
            renderAgencyChips();
            updateActiveFilterIndicators();
            render();
        });
        for (Map.Entry<Integer, String> agency : agencyNames.entrySet()) {
            int id = agency.getKey();
            String name = agency.getValue().isEmpty() ? "Ag. " + id : agency.getValue();
            addFilterChip(agencies, name, R.drawable.admin_ic_building, agencyFilter == id, () -> {
                if (agencyFilter == id) return;
                agencyFilter = id;
                renderAgencyChips();
                updateActiveFilterIndicators();
                render();
            });
        }
        AdminTypography.apply(this, agencies);
    }

    private void addFilterChip(ChipGroup parent, String label, @DrawableRes int iconRes,
                               boolean selected, Runnable action) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setSelected(selected);
        chip.setMinimumHeight(dp(38));
        chip.setPadding(dp(12), dp(7), dp(13), dp(7));
        chip.setBackgroundResource(selected
                ? R.drawable.admin_bg_rewards_chip_active
                : R.drawable.admin_bg_rewards_chip_inactive);
        chip.setContentDescription(label + (selected ? ", seleccionado" : ""));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(
                selected ? Color.WHITE : getColor(R.color.admin_text_secondary)));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(15), dp(15));
        iconLp.setMarginEnd(dp(6));
        chip.addView(icon, iconLp);

        TextView text = new TextView(this);
        text.setText(label);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setTextSize(12.5f);
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        text.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
        chip.addView(text);

        chip.setOnClickListener(v -> action.run());
        parent.addView(chip, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void render() {
        if (!loaded) return;
        clearResults();
        String rawQuery = search.getText().toString().trim();
        String query = rawQuery.toLowerCase(Locale.ROOT);
        List<JsonObject> visible = new ArrayList<>();
        for (JsonObject row : source) {
            if (!"Todos".equals(typeFilter) && !typeFilter.equalsIgnoreCase(str(row, "tipo"))) continue;
            if (agencyFilter > 0 && agencyFilter != num(row, "id_agencia")) continue;
            if (!str(row, "nombre_completo").toLowerCase(Locale.ROOT).contains(query)) continue;
            visible.add(row);
        }

        String periodBadge = AdminRankingPeriodFormatter.formatPeriodKey(loadedPeriodKey, periodType);
        SummaryStats stats = AdminRankingSummaryHelper.computeStats(loaded, loadedPeriodKey, visible);
        bindSummaryStats(stats, periodBadge);

        EmptyKind emptyKind = AdminRankingSummaryHelper.resolveEmptyKind(
                source.size(), visible.size(), loadedPeriodKey, rawQuery, hasActiveFilters());
        if (emptyKind != EmptyKind.NONE) {
            if (emptyKind == EmptyKind.NO_CLOSED_PERIOD) {
                showTrophyEmptyStatus(
                        "Aún no hay resultados",
                        "Cuando se complete un período de evaluación, podrás consultar aquí el ranking de tu equipo.",
                        "Esperando evaluaciones");
            } else if (emptyKind == EmptyKind.NO_EVALUATIONS) {
                boolean currentMonth = monthFilter == null && "MENSUAL".equals(periodType);
                showTrophyEmptyStatus(currentMonth
                                ? "El ranking de este mes empieza en cero"
                                : "Aún no hay resultados disponibles",
                        currentMonth
                                ? "Aparecerán resultados cuando se aprueben los KPI de " + periodBadge + "."
                                : "No se encontraron evaluaciones cerradas para " + periodBadge + ".",
                        "Esperando evaluaciones");
            } else if (emptyKind == EmptyKind.NO_SEARCH_RESULTS) {
                showIconStatus(R.drawable.admin_ic_search,
                        "No encontramos colaboradores con esa búsqueda",
                        "No hay resultados para \"" + rawQuery + "\" en este período.",
                        "Limpiar búsqueda",
                        () -> search.setText(""));
            } else {
                showIconStatus(R.drawable.admin_ic_sliders,
                        "No hay colaboradores con estos filtros",
                        "Ningún colaborador coincide con los filtros seleccionados.",
                        "Limpiar filtros",
                        this::clearFilters);
            }
            return;
        }

        hideStatus();

        podiumTitle.setVisibility(View.VISIBLE);
        listTitle.setVisibility(View.VISIBLE);
        renderPodium(visible);
        for (JsonObject row : visible) {
            addRow(row);
        }
        AdminTypography.apply(this, podium);
        AdminTypography.apply(this, list);
    }

    private void renderPodium(List<JsonObject> visible) {
        podium.removeAllViews();
        int count = Math.min(3, visible.size());
        if (count == 0) return;

        boolean animate = !isReduceMotionEnabled();
        if (count == 1) {
            addPodiumCard(visible.get(0), 1, 0xFFF2C36D, true, true, animate, 0);
            return;
        }
        if (count == 2) {
            addPodiumCard(visible.get(0), 1, 0xFFF2C36D, true, false, animate, 0);
            addPodiumCard(visible.get(1), 2, 0xFFC8CEDB, false, true, animate, 1);
            return;
        }
        // 3 puestos: 2.º izquierda · 1.º centro elevado · 3.º derecha
        addPodiumCard(visible.get(1), 2, 0xFFC8CEDB, false, false, animate, 1);
        addPodiumCard(visible.get(0), 1, 0xFFF2C36D, true, false, animate, 0);
        addPodiumCard(visible.get(2), 3, 0xFFD29972, false, true, animate, 2);
    }

    private void addPodiumCard(JsonObject row,
                               int rankOrder,
                               int medalColor,
                               boolean isFirst,
                               boolean isLast,
                               boolean animate,
                               int animIndex) {
        View card = LayoutInflater.from(this).inflate(R.layout.admin_item_ranking_podium, podium, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (!isLast) lp.setMarginEnd(dp(8));
        lp.topMargin = isFirst ? 0 : dp(16);
        card.setLayoutParams(lp);
        card.setMinimumHeight(dp(isFirst ? 176 : 158));

        if (isFirst) {
            GradientDrawable goldCardBg = new GradientDrawable();
            goldCardBg.setShape(GradientDrawable.RECTANGLE);
            goldCardBg.setCornerRadius(dp(18));
            goldCardBg.setColor(getColor(R.color.admin_surface_tertiary));
            goldCardBg.setStroke(dp(1), getColor(R.color.admin_status_pending));
            card.setBackground(goldCardBg);
        }

        ImageView medalIv = card.findViewById(R.id.podium_medal_icon);
        TextView rankTv = card.findViewById(R.id.podium_rank_label);
        TextView avatarTv = card.findViewById(R.id.podium_avatar);
        TextView nameTv = card.findViewById(R.id.podium_name);
        TextView percentTv = card.findViewById(R.id.podium_percent);
        TextView pointsTv = card.findViewById(R.id.podium_points);

        int rankNum = num(row, "puesto");
        String name = str(row, "nombre_completo");
        medalIv.setColorFilter(medalColor);
        rankTv.setText("#" + (rankNum > 0 ? rankNum : rankOrder));
        rankTv.setTextColor(medalColor);

        avatarTv.setText(initials(name));
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(0xFF472766);
        avatarBg.setStroke(dp(2), medalColor);
        avatarTv.setBackground(avatarBg);

        nameTv.setText(name);
        percentTv.setText(percent(row));
        pointsTv.setText(num(row, "puntos_kpi_periodo") + " pts");

        attachPressFeedback(card);
        card.setOnClickListener(v -> detail(row));
        podium.addView(card);

        if (animate) {
            card.setAlpha(0f);
            card.setTranslationY(dp(10));
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(animIndex * 50L)
                    .setDuration(ANIM_DURATION_MS)
                    .start();
        }
    }

    private void addRow(JsonObject row) {
        View card = LayoutInflater.from(this).inflate(R.layout.admin_item_ranking_row, list, false);
        TextView posTv = card.findViewById(R.id.ranking_row_position);
        TextView avatarTv = card.findViewById(R.id.ranking_row_avatar);
        TextView nameTv = card.findViewById(R.id.ranking_row_name);
        TextView metaTv = card.findViewById(R.id.ranking_row_meta);
        TextView percentTv = card.findViewById(R.id.ranking_row_percent);
        TextView pointsTv = card.findViewById(R.id.ranking_row_points);
        TextView deltaTv = card.findViewById(R.id.ranking_row_delta);
        FrameLayout bar = card.findViewById(R.id.ranking_row_bar);
        View barFill = card.findViewById(R.id.ranking_row_bar_fill);

        int pos = num(row, "puesto");
        String name = str(row, "nombre_completo");
        String cargo = str(row, "cargo_nombre");
        String tipo = str(row, "tipo");

        posTv.setText("#" + pos);
        posTv.setTextColor(pos == 1 ? getColor(R.color.admin_status_pending)
                : pos == 2 ? getColor(R.color.admin_cyan_brain)
                : pos == 3 ? 0xFFB45309
                : getColor(R.color.admin_purple_accent));

        avatarTv.setText(initials(name));
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(getColor(R.color.admin_purple_primary));
        avatarTv.setBackground(avatarBg);

        nameTv.setText(name);
        String meta = cargo.isEmpty() ? tipo : (tipo.isEmpty() ? cargo : cargo + " · " + tipo);
        metaTv.setText(meta);
        percentTv.setText(percent(row));
        pointsTv.setText(num(row, "puntos_kpi_periodo") + " pts");

        Integer old = nullableInt(row, "puesto_anterior");
        if (old != null) {
            String deltaText = old == pos ? "Sin cambio de posición"
                    : old > pos ? "Subió " + (old - pos) + " puesto(s)"
                    : "Bajó " + (pos - old) + " puesto(s)";
            deltaTv.setText(deltaText);
            deltaTv.setTextColor(old > pos ? getColor(R.color.admin_status_delivered) : getColor(R.color.admin_text_secondary));
            deltaTv.setVisibility(View.VISIBLE);
        } else {
            deltaTv.setVisibility(View.GONE);
        }

        float pct = Math.min(100f, Math.max(0f, decimal(row, "porcentaje_cumplimiento")));
        bar.post(() -> {
            ViewGroup.LayoutParams fillLp = barFill.getLayoutParams();
            fillLp.width = Math.round(bar.getWidth() * pct / 100f);
            barFill.setLayoutParams(fillLp);
        });

        attachPressFeedback(card);
        card.setOnClickListener(v -> detail(row));
        list.addView(card);
    }

    private void detail(JsonObject row) {
        if (detailPending != null) detailPending.cancel();
        int worker = num(row, "id_trabajador");
        SessionManager session = new SessionManager(this);
        if (!sameAccount()) return;
        detailPending = ApiClient.getService().getRankingDetail(session.getBearerToken(), worker, periodType);
        detailPending.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != detailPending || call.isCanceled() || !sameAccount() || isFinishing()) return;
                detailPending = null;
                ApiResponse<JsonObject> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()) {
                    Toast.makeText(AdminRankingActivity.this, response.code() == 403 ?
                            "No tienes permiso para ver este detalle." : "No se pudo abrir el detalle.", Toast.LENGTH_LONG).show();
                    return;
                }
                showDetail(row, envelope.getData());
            }

            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call == detailPending && !call.isCanceled() && sameAccount())
                    Toast.makeText(AdminRankingActivity.this, "Sin conexión para cargar el detalle.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDetail(JsonObject row, JsonObject data) {
        int bgMain = getColor(R.color.admin_bg_main);
        int textPrimary = getColor(R.color.admin_text_primary);
        int textSecondary = getColor(R.color.admin_text_secondary);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bgMain);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(24));
        scroll.addView(content);
        content.addView(labelText(str(row, "nombre_completo"), 20, textPrimary, true));
        content.addView(labelText(str(row, "cargo_nombre") + " · " + str(row, "tipo"), 13, textSecondary, false));
        section(content, "Evolución del ranking");
        for (JsonElement element : array(data, "evolucion")) {
            JsonObject item = element.getAsJsonObject();
            detailCard(content, str(item, "periodo_clave") + "   ·   puesto #" + num(item, "puesto"),
                    percent(item) + " de cumplimiento   ·   " + num(item, "puntos_kpi_periodo") + " puntos confirmados");
        }
        JsonArray evaluations = array(data, "evaluaciones");
        section(content, "KPI aprobados · " + evaluations.size());
        if (evaluations.size() == 0) {
            detailCard(content, "Sin desglose disponible", "Tu permiso de KPI o los datos aprobados no permiten mostrar más detalle.");
        } else for (JsonElement element : evaluations) {
            JsonObject item = element.getAsJsonObject();
            detailCard(content, str(item, "nombre") + " · " + str(item, "periodo_clave"),
                    percent(item) + " de cumplimiento   ·   " + num(item, "puntos_otorgados") + " puntos confirmados");
        }
        AdminTypography.apply(this, content);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll).setPositiveButton("Cerrar", null).create();
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(bgMain));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.admin_purple_accent));
    }

    private void section(LinearLayout parent, String label) {
        TextView title = labelText(label, 16, getColor(R.color.admin_text_primary), true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(22);
        parent.addView(title, p);
    }

    private void detailCard(LinearLayout parent, String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.admin_bg_ranking_surface_card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        parent.addView(card, p);
        card.addView(labelText(title, 14, getColor(R.color.admin_text_primary), true));
        card.addView(labelText(body, 12, getColor(R.color.admin_text_secondary), false));
    }

    private TextView labelText(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private void attachPressFeedback(View view) {
        if (view == null || isReduceMotionEnabled()) return;
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(110L).start();
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static int num(JsonObject o, String key) {
        try {
            return Integer.parseInt(str(o, key));
        } catch (Exception e) {
            return 0;
        }
    }

    private static Integer nullableInt(JsonObject o, String key) {
        try {
            return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static float decimal(JsonObject o, String key) {
        try {
            return Float.parseFloat(str(o, key));
        } catch (Exception e) {
            return 0;
        }
    }

    private static JsonArray array(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static String percent(JsonObject row) {
        return String.format(Locale.getDefault(), "%.1f%%", decimal(row, "porcentaje_cumplimiento"));
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts.length > 1 ? (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT) :
                name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
