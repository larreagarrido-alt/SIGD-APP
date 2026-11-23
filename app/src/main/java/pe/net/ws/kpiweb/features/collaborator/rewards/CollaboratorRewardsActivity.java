package pe.net.ws.kpiweb.features.collaborator.rewards;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorDataState;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorRankingPointsActivity;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.CatalogEmptyState;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.FilterMode;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.FilterState;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.IllustrationKind;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.RewardItem;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsCatalogHelper.SortMode;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsRankingDataSource.CollaboratorScoreSnapshot;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsRankingDataSource.RecognitionCardItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla "Premios y Beneficios" del Colaborador — Rediseño SIGD Dark Premium.
 *
 * Muestra exclusivamente los premios reales publicados por el servidor para trabajadores
 * autenticados (sin inventar registros, costos en puntos ni canjes simulados), con estados
 * diferenciados de carga, vacío, búsqueda, filtros reales y error.
 */
public class CollaboratorRewardsActivity extends AppCompatActivity {

    private static final long ANIM_DURATION_MS = 180L;

    private CollaboratorProfile activeProfile;
    private Call<ApiResponse<JsonObject>> catalogRequest;
    private Call<ApiResponse<JsonObject>> pointsRequest;

    private final List<RewardItem> publishedRewards = new ArrayList<>();
    private String searchQuery = "";

    private boolean catalogLoaded = false;
    private boolean catalogLoading = false;
    private String catalogErrorMessage = null;
    private boolean catalogErrorRetryable = false;
    private long catalogLoadedElapsed = 0L;
    private int confirmedPointsBalance = -1;

    private EditText etSearch;
    private View btnClearSearch;

    private View heroPointsPill;
    private TextView tvHeroPoints;
    private View stateLoading;
    private View stateEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private TextView btnEmptyAction;
    private LinearLayout gridContainer;
    private View endInfoCard;

    private final Handler catalogTimer = new Handler(Looper.getMainLooper());
    private final Runnable refreshCatalog = new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()
                    && activeProfile != null
                    && CollaboratorDataState.isRealWorker(activeProfile)) {
                loadRealRewards(false);
                catalogTimer.postDelayed(this, 60000L);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_rewards);
        CollaboratorThemeHelper.setupSystemBars(this);

        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();

        bindViews();
        setupInteractions();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.MENU);
        CollaboratorThemeHelper.applyReadableTypeface(findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        refreshUI();
        catalogTimer.removeCallbacks(refreshCatalog);
        if (activeProfile != null && CollaboratorDataState.isRealWorker(activeProfile)) {
            catalogTimer.postDelayed(refreshCatalog, 60000L);
        }
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.MENU);
    }

    @Override
    protected void onPause() {
        catalogTimer.removeCallbacks(refreshCatalog);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        catalogTimer.removeCallbacks(refreshCatalog);
        if (catalogRequest != null) catalogRequest.cancel();
        if (pointsRequest != null) pointsRequest.cancel();
        super.onDestroy();
    }

    public static String resolveTierName(int points) {
        return CollaboratorRewardsRankingDataSource.resolveTierName(points);
    }

    private void bindViews() {
        etSearch = findViewById(R.id.collab_rewards_et_search);
        btnClearSearch = findViewById(R.id.collab_rewards_btn_clear_search);

        heroPointsPill = findViewById(R.id.collab_rewards_hero_points_pill);
        tvHeroPoints = findViewById(R.id.collab_rewards_tv_hero_points);
        stateLoading = findViewById(R.id.collab_rewards_state_loading);
        stateEmpty = findViewById(R.id.collab_rewards_state_empty);
        tvEmptyTitle = findViewById(R.id.collab_rewards_empty_title);
        tvEmptySubtitle = findViewById(R.id.collab_rewards_empty_subtitle);
        btnEmptyAction = findViewById(R.id.collab_rewards_empty_btn);
        gridContainer = findViewById(R.id.collab_rewards_grid_container);
        endInfoCard = findViewById(R.id.collab_rewards_end_info_card);
    }

    private void setupInteractions() {
        View btnBack = findViewById(R.id.collab_rewards_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnGoRanking = findViewById(R.id.collab_rewards_btn_go_ranking);
        if (btnGoRanking != null) {
            attachPressFeedback(btnGoRanking);
            btnGoRanking.setOnClickListener(v ->
                    startActivity(new Intent(this, CollaboratorRankingPointsActivity.class)));
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s == null ? "" : s.toString().trim();
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    renderCurrentCatalog();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearch != null) etSearch.setText("");
            });
        }
    }

    private void refreshUI() {
        if (activeProfile == null) return;
        if (CollaboratorDataState.isRealWorker(activeProfile)) {
            SessionManager session = new SessionManager(this);
            Integer cachedPoints = session.getPointsBalance();
            if (cachedPoints != null) {
                confirmedPointsBalance = cachedPoints;
                updateHeroPointsBadge(confirmedPointsBalance);
            }
            loadRealRewards(!catalogLoaded);
            return;
        }
        renderDemoFallback(activeProfile.getId());
    }

    private void loadRealRewards(boolean showLoadingState) {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) {
            catalogLoading = false;
            catalogLoaded = false;
            catalogErrorMessage = "Inicia sesión para consultar los premios publicados.";
            catalogErrorRetryable = false;
            renderCurrentCatalog();
            return;
        }

        if (catalogRequest != null) catalogRequest.cancel();
        if (pointsRequest != null) pointsRequest.cancel();

        final int workerId = session.getWorkerId();
        final String token = session.getAccessToken();

        if (showLoadingState) {
            catalogLoading = true;
            catalogErrorMessage = null;
            renderCurrentCatalog();
        }

        catalogRequest = ApiClient.getService().getPublishedRewards(session.getBearerToken());
        catalogRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!currentRequest(call, token, workerId)) return;
                catalogLoading = false;
                ApiResponse<JsonObject> body = response.body();
                JsonObject data = body == null ? null : body.getData();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || data == null) {
                    catalogErrorMessage = response.code() == 403
                            ? "Tu cuenta no tiene permiso para consultar reconocimientos."
                            : "No se pudo consultar el catálogo en este momento.";
                    catalogErrorRetryable = response.code() != 403;
                    renderCurrentCatalog();
                    return;
                }

                publishedRewards.clear();
                JsonArray items = data.has("premios") && data.get("premios").isJsonArray()
                        ? data.getAsJsonArray("premios")
                        : new JsonArray();
                publishedRewards.addAll(CollaboratorRewardsCatalogHelper.parseRewards(items));
                catalogLoadedElapsed = SystemClock.elapsedRealtime();
                catalogLoaded = true;
                catalogErrorMessage = null;
                catalogErrorRetryable = false;

                long nextRefresh = 60000L;
                for (RewardItem item : publishedRewards) {
                    if (item.secondsRemaining >= 0) {
                        nextRefresh = Math.min(nextRefresh, Math.max(1000L, item.secondsRemaining * 1000L));
                    }
                }
                catalogTimer.removeCallbacks(refreshCatalog);
                catalogTimer.postDelayed(refreshCatalog, nextRefresh);

                renderCurrentCatalog();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!currentRequest(call, token, workerId)) return;
                catalogLoading = false;
                catalogErrorMessage = "Sin conexión con el catálogo de reconocimientos.";
                catalogErrorRetryable = true;
                renderCurrentCatalog();
            }
        });

        pointsRequest = ApiClient.getService().getOwnPoints(session.getBearerToken());
        pointsRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!currentRequest(call, token, workerId)) return;
                ApiResponse<JsonObject> body = response.body();
                JsonObject data = body == null ? null : body.getData();
                if (response.isSuccessful() && body != null && body.isSuccessful() && data != null
                        && getInt(data, "id_trabajador", -1) == workerId) {
                    int points = getInt(data, "saldo_actual", 0);
                    confirmedPointsBalance = points;
                    session.savePointsBalance(points);
                    updateHeroPointsBadge(points);
                    if (catalogLoaded) {
                        renderCurrentCatalog();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                // Keep previously confirmed balance if available; never fabricate points.
            }
        });
    }

    private void updateHeroPointsBadge(int points) {
        if (heroPointsPill == null || tvHeroPoints == null) return;
        if (points < 0) {
            heroPointsPill.setVisibility(View.GONE);
            return;
        }
        tvHeroPoints.setText("Tus puntos: " + CollaboratorRewardsCatalogHelper.formatPointsBadge(points));
        heroPointsPill.setVisibility(View.VISIBLE);
    }

    private FilterState currentFilterState() {
        return new FilterState(searchQuery, FilterMode.ALL, "", SortMode.DEFAULT);
    }

    private void renderCurrentCatalog() {
        if (activeProfile != null && !CollaboratorDataState.isRealWorker(activeProfile)) {
            renderDemoFallback(activeProfile.getId());
            return;
        }

        // 1. Estado de carga (nunca mostrar "No hay premios" mientras carga)
        if (catalogLoading) {
            setSectionVisibility(true, false, false, false);
            return;
        }

        // 2. Estado de error / sin conexión / sin permiso
        if (catalogErrorMessage != null) {
            setSectionVisibility(false, true, false, false);
            if (tvEmptyTitle != null) {
                tvEmptyTitle.setText(catalogErrorRetryable
                        ? "No se pudo cargar el catálogo"
                        : "Acceso no disponible");
            }
            if (tvEmptySubtitle != null) {
                tvEmptySubtitle.setText(catalogErrorMessage);
            }
            if (btnEmptyAction != null) {
                if (catalogErrorRetryable) {
                    btnEmptyAction.setText("Reintentar");
                    btnEmptyAction.setVisibility(View.VISIBLE);
                    btnEmptyAction.setOnClickListener(v -> loadRealRewards(true));
                } else {
                    btnEmptyAction.setVisibility(View.GONE);
                }
            }
            return;
        }

        long elapsedMs = catalogLoaded ? Math.max(0L, SystemClock.elapsedRealtime() - catalogLoadedElapsed) : 0L;
        List<RewardItem> activeItems = CollaboratorRewardsCatalogHelper.filterActive(publishedRewards, elapsedMs);
        FilterState state = currentFilterState();
        int userPoints = Math.max(0, confirmedPointsBalance);
        List<RewardItem> displayed = CollaboratorRewardsCatalogHelper.applySearchAndFilters(
                activeItems, state, userPoints, elapsedMs);

        CatalogEmptyState emptyState = CollaboratorRewardsCatalogHelper.resolveEmptyState(
                activeItems.size(), displayed.size(), state);

        // 3. Estados vacíos diferenciados
        if (emptyState != CatalogEmptyState.NONE) {
            setSectionVisibility(false, true, false, false);
            bindEmptyState(emptyState);
            return;
        }

        // 4. Cuadrícula de premios reales + tarjeta informativa final
        setSectionVisibility(false, false, true, true);
        populateRewardsGrid(displayed, elapsedMs);
    }

    private void bindEmptyState(CatalogEmptyState emptyState) {
        if (tvEmptyTitle == null || tvEmptySubtitle == null || btnEmptyAction == null) return;
        if (emptyState == CatalogEmptyState.NO_REGISTERED_REWARDS) {
            tvEmptyTitle.setText("Aún no hay premios disponibles");
            tvEmptySubtitle.setText("Sigue participando y acumulando puntos para futuras recompensas");
            btnEmptyAction.setVisibility(View.GONE);
        } else {
            tvEmptyTitle.setText("Sin resultados para tu búsqueda");
            tvEmptySubtitle.setText("No encontramos premios que coincidan con \"" + searchQuery + "\".");
            btnEmptyAction.setText("Limpiar búsqueda");
            btnEmptyAction.setVisibility(View.VISIBLE);
            btnEmptyAction.setOnClickListener(v -> {
                if (etSearch != null) etSearch.setText("");
            });
        }
    }

    private void populateRewardsGrid(List<RewardItem> items, long elapsedMs) {
        if (gridContainer == null) return;
        gridContainer.removeAllViews();

        int columns = shouldUseTwoColumns() ? 2 : 1;
        LayoutInflater inflater = LayoutInflater.from(this);
        String bearer = new SessionManager(this).getBearerToken();
        boolean animate = !isReduceMotionEnabled();

        int cardIndex = 0;
        for (int i = 0; i < items.size(); i += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setBaselineAligned(false);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(12);
            gridContainer.addView(row, rowLp);

            for (int col = 0; col < columns; col++) {
                int idx = i + col;
                if (idx < items.size()) {
                    RewardItem item = items.get(idx);
                    View card = inflater.inflate(R.layout.collaborator_item_reward_card, row, false);
                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                    if (columns == 2 && col == 0) {
                        cardLp.setMarginEnd(dp(6));
                    } else if (columns == 2 && col == 1) {
                        cardLp.setMarginStart(dp(6));
                    }
                    bindRewardCard(card, item, elapsedMs, bearer);
                    row.addView(card, cardLp);

                    if (animate) {
                        card.setAlpha(0f);
                        card.setTranslationY(dp(10));
                        card.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setStartDelay(Math.min(cardIndex * 35L, 210L))
                                .setDuration(ANIM_DURATION_MS)
                                .start();
                    }
                    cardIndex++;
                } else {
                    Space spacer = new Space(this);
                    LinearLayout.LayoutParams spaceLp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                    spaceLp.setMarginStart(dp(6));
                    row.addView(spacer, spaceLp);
                }
            }
        }

        CollaboratorThemeHelper.applyReadableTypeface(gridContainer);
    }

    private void bindRewardCard(View card, RewardItem item, long elapsedMs, String bearer) {
        ImageView ivFallback = card.findViewById(R.id.reward_card_iv_fallback);
        ImageView ivPhoto = card.findViewById(R.id.reward_card_iv_photo);
        View pointsPill = card.findViewById(R.id.reward_card_points_pill);
        TextView tvPoints = card.findViewById(R.id.reward_card_tv_points);
        TextView tvStatusTag = card.findViewById(R.id.reward_card_tv_status_tag);
        TextView tvTitle = card.findViewById(R.id.reward_card_tv_title);
        TextView tvDesc = card.findViewById(R.id.reward_card_tv_desc);
        TextView tvMeta = card.findViewById(R.id.reward_card_tv_meta);
        View btnAction = card.findViewById(R.id.reward_card_btn_action);
        TextView tvActionLabel = card.findViewById(R.id.reward_card_tv_action_label);

        if (ivFallback != null) {
            ivFallback.setVisibility(View.VISIBLE);
            ivFallback.setImageResource(illustrationResFor(
                    CollaboratorRewardsCatalogHelper.resolveFallbackIllustration(item.title, item.category)));
        }
        if (ivPhoto != null) {
            ivPhoto.setImageDrawable(null);
            if (item.hasRealImage()) {
                RewardImageLoader.load(this, ivPhoto, ivFallback, item.imageUrl, bearer);
                ivPhoto.setOnClickListener(v -> RewardImageViewer.show(this, item.imageUrl, bearer, null));
            } else {
                ivPhoto.setOnClickListener(null);
            }
        }

        if (pointsPill != null && tvPoints != null) {
            if (item.hasPointsCost()) {
                tvPoints.setText(CollaboratorRewardsCatalogHelper.formatPointsBadge(item.pointsRequired));
                pointsPill.setVisibility(View.VISIBLE);
            } else {
                pointsPill.setVisibility(View.GONE);
            }
        }

        int leftSec = item.effectiveSecondsRemaining(elapsedMs);
        String validity = CollaboratorRewardsCatalogHelper.formatValidityLabel(leftSec, item.endDate);
        if (tvStatusTag != null) {
            tvStatusTag.setText(validity);
            tvStatusTag.setVisibility(View.VISIBLE);
        }

        if (tvTitle != null) {
            tvTitle.setText(item.title.isEmpty() ? "Reconocimiento publicado" : item.title);
        }
        if (tvDesc != null) {
            tvDesc.setText(item.description.isEmpty()
                    ? "Consulta los detalles vigentes de este reconocimiento."
                    : item.description);
        }
        if (tvMeta != null) {
            if (!item.category.isEmpty()) {
                tvMeta.setText(item.category);
                tvMeta.setVisibility(View.VISIBLE);
            } else {
                tvMeta.setVisibility(View.GONE);
            }
        }

        // El catálogo de publicaciones de SIGD (PremiosPublicacionesController) no expone canje directo
        // ni descuenta puntos; por transparencia mostramos "Ver detalle" conectado a GET /premios/{id}.
        if (tvActionLabel != null) {
            tvActionLabel.setText("Ver detalle");
        }

        View.OnClickListener openDetail = v -> {
            if (activeProfile != null && !CollaboratorDataState.isRealWorker(activeProfile)) {
                CollaboratorBannerHelper.showPremiumModal(
                        this,
                        "Premio y Reconocimiento",
                        item.title.isEmpty() ? "Reconocimiento" : item.title,
                        item.description,
                        "Cerrar",
                        null,
                        null
                );
            } else {
                openRealRewardDetail(item.id);
            }
        };
        attachPressFeedback(card);
        card.setOnClickListener(openDetail);
        if (btnAction != null) {
            attachPressFeedback(btnAction);
            btnAction.setOnClickListener(openDetail);
        }
    }

    private void openRealRewardDetail(int prizeId) {
        SessionManager current = new SessionManager(this);
        if (!current.isLoggedIn() || prizeId <= 0) return;
        String expected = current.getAccessToken();
        ApiClient.getService().getRewardDetail(current.getBearerToken(), prizeId)
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        if (!expected.equals(new SessionManager(CollaboratorRewardsActivity.this).getAccessToken())
                                || isFinishing() || isDestroyed()) return;
                        ApiResponse<JsonObject> result = response.body();
                        if (!response.isSuccessful() || result == null || !result.isSuccessful() || result.getData() == null) {
                            loadRealRewards(false);
                            return;
                        }
                        RewardItem live = CollaboratorRewardsCatalogHelper.parseReward(result.getData());
                        StringBuilder detail = new StringBuilder();
                        if (!live.description.isEmpty()) {
                            detail.append(live.description);
                        }
                        if (!live.endDate.isEmpty()) {
                            if (detail.length() > 0) detail.append("\n\n");
                            detail.append("Vigencia: disponible hasta ").append(live.endDate);
                        }
                        if (live.hasPointsCost()) {
                            if (detail.length() > 0) detail.append("\n");
                            detail.append("Puntaje de referencia: ")
                                    .append(CollaboratorRewardsCatalogHelper.formatPointsBadge(live.pointsRequired));
                        }
                        CollaboratorBannerHelper.showPremiumModal(
                                CollaboratorRewardsActivity.this,
                                "Premio y Reconocimiento",
                                live.title.isEmpty() ? "Reconocimiento publicado" : live.title,
                                detail.toString(),
                                "Cerrar",
                                null,
                                null
                        );
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                        if (isFinishing() || isDestroyed()) return;
                        CollaboratorBannerHelper.showPremiumModal(
                                CollaboratorRewardsActivity.this,
                                "Sin conexión",
                                "No se pudo abrir el detalle",
                                "Verifica tu conexión e intenta consultar el reconocimiento nuevamente.",
                                "Entendido",
                                null,
                                null
                        );
                    }
                });
    }

    /**
     * Modo demostrativo local (solo cuando el perfil activo no es trabajador real del servidor).
     */
    private void renderDemoFallback(String collabId) {
        CollaboratorScoreSnapshot snap = CollaboratorRewardsRankingDataSource.getSnapshotForCollaborator(collabId);
        confirmedPointsBalance = snap.totalPoints;
        updateHeroPointsBadge(snap.totalPoints);

        List<RecognitionCardItem> demoRecognitions =
                CollaboratorRewardsRankingDataSource.getRecognitionsForCollaborator(collabId);
        List<RewardItem> mapped = new ArrayList<>();
        int idx = 1;
        for (RecognitionCardItem rec : demoRecognitions) {
            mapped.add(new RewardItem(
                    idx++,
                    rec.title,
                    rec.description,
                    "",
                    rec.requiredTier,
                    0,
                    -1,
                    "",
                    rec.status.getLabel()
            ));
        }

        FilterState state = currentFilterState();
        List<RewardItem> filtered = CollaboratorRewardsCatalogHelper.applySearchAndFilters(
                mapped, state, snap.totalPoints, 0L);

        CatalogEmptyState emptyState = CollaboratorRewardsCatalogHelper.resolveEmptyState(
                mapped.size(), filtered.size(), state);
        if (emptyState != CatalogEmptyState.NONE) {
            setSectionVisibility(false, true, false, false);
            bindEmptyState(emptyState);
            return;
        }

        setSectionVisibility(false, false, true, true);
        populateRewardsGrid(filtered, 0L);
    }

    private void setSectionVisibility(boolean loading, boolean empty, boolean grid, boolean endCard) {
        if (stateLoading != null) stateLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (stateEmpty != null) stateEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (gridContainer != null) gridContainer.setVisibility(grid ? View.VISIBLE : View.GONE);
        if (endInfoCard != null) endInfoCard.setVisibility(endCard ? View.VISIBLE : View.GONE);
    }

    @DrawableRes
    private int illustrationResFor(IllustrationKind kind) {
        if (kind == IllustrationKind.CINEMA) return R.drawable.collab_ill_reward_cinema;
        if (kind == IllustrationKind.COFFEE) return R.drawable.collab_ill_reward_coffee;
        if (kind == IllustrationKind.VOUCHER) return R.drawable.collab_ill_reward_voucher;
        if (kind == IllustrationKind.TECH) return R.drawable.collab_ill_reward_tech;
        return R.drawable.collab_ill_reward_generic_gift;
    }

    private boolean shouldUseTwoColumns() {
        float widthDp = getResources().getDisplayMetrics().widthPixels
                / getResources().getDisplayMetrics().density;
        return widthDp >= 330f;
    }

    private void attachPressFeedback(View view) {
        if (view == null || isReduceMotionEnabled()) return;
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(110L).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(130L).start();
            }
            return false;
        });
    }

    private boolean isReduceMotionEnabled() {
        try {
            float scale = Settings.Global.getFloat(
                    getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return scale == 0f;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean currentRequest(Call<?> call, String token, int workerId) {
        SessionManager now = new SessionManager(this);
        return !isFinishing() && !isDestroyed() && !call.isCanceled()
                && now.getWorkerId() == workerId
                && token.equals(now.getAccessToken());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
