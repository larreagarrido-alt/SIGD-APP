package pe.net.ws.kpiweb.features.collaborator.ranking;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorDataState;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorProgressSnapshot.EvalWindow;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorProgressSnapshot.LevelNode;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorProgressSnapshot.RankingRow;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsActivity;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsRankingDataSource;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsRankingDataSource.CollaboratorScoreSnapshot;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsRankingDataSource.LeaderboardEntry;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsRankingDataSource.PeriodFilter;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla "Mi progreso y reconocimientos" del Colaborador (SIGD by website).
 * Presenta nivel actual e insignia ilustrada, estadísticas clave, línea de niveles,
 * evaluación semanal/mensual con gráfico circular y ranking general con datos reales.
 */
public class CollaboratorRankingPointsActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";
    public static final String TAB_RANKING = "RANKING";
    public static final String TAB_POINTS = "POINTS";

    private CollaboratorProfile activeProfile;
    private boolean openRankingSectionOnStart;
    private EvalWindow selectedEvalWindow = EvalWindow.MONTH;
    private boolean rankingExpanded;

    private Call<ApiResponse<JsonObject>> ownPointsRequest;
    private Call<ApiResponse<JsonObject>> rankingRequest;
    private JsonObject ownPoints;
    private JsonObject closedRanking;
    private String rankingStatus = "Consultando clasificación cerrada…";
    private int ownPointsWorkerId;
    private boolean shieldAnimatedOnce;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_ranking_points);
        CollaboratorThemeHelper.setupSystemBars(this);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_INITIAL_TAB)) {
            String init = getIntent().getStringExtra(EXTRA_INITIAL_TAB);
            openRankingSectionOnStart = TAB_RANKING.equalsIgnoreCase(init);
            if (openRankingSectionOnStart) {
                rankingExpanded = true;
            }
        }

        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        setupInteractions();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.MENU);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        refreshUI();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.MENU);
        if (openRankingSectionOnStart) {
            openRankingSectionOnStart = false;
            scrollToRankingSection();
        }
    }

    @Override
    protected void onDestroy() {
        if (ownPointsRequest != null) ownPointsRequest.cancel();
        if (rankingRequest != null) rankingRequest.cancel();
        super.onDestroy();
    }

    private void setupInteractions() {
        View btnBack = findViewById(R.id.collab_ranking_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnGoRewards = findViewById(R.id.collab_ranking_btn_go_rewards);
        if (btnGoRewards != null) {
            btnGoRewards.setOnClickListener(v ->
                    startActivity(new Intent(this, CollaboratorRewardsActivity.class)));
        }

        View btnWeek = findViewById(R.id.collab_prog_btn_week);
        if (btnWeek != null) {
            btnWeek.setOnClickListener(v -> {
                if (selectedEvalWindow != EvalWindow.WEEK) {
                    selectedEvalWindow = EvalWindow.WEEK;
                    renderCurrentData();
                }
            });
        }

        View btnMonth = findViewById(R.id.collab_prog_btn_month);
        if (btnMonth != null) {
            btnMonth.setOnClickListener(v -> {
                if (selectedEvalWindow != EvalWindow.MONTH) {
                    selectedEvalWindow = EvalWindow.MONTH;
                    renderCurrentData();
                }
            });
        }

        View btnViewRanking = findViewById(R.id.collab_prog_btn_view_ranking);
        if (btnViewRanking != null) {
            btnViewRanking.setOnClickListener(v -> {
                rankingExpanded = !rankingExpanded;
                renderCurrentData();
                scrollToRankingSection();
            });
        }

        View btnRetry = findViewById(R.id.collab_prog_btn_retry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                showStatusBanner("Actualizando datos…", false);
                loadOwnPoints();
                loadClosedRanking();
            });
        }
    }

    private void refreshUI() {
        if (activeProfile == null) return;
        if (CollaboratorDataState.isRealWorker(activeProfile)) {
            int workerId = new SessionManager(this).getWorkerId();
            if (ownPointsWorkerId != workerId) {
                ownPoints = null;
                closedRanking = null;
                rankingStatus = "Consultando clasificación cerrada…";
            }
            setText(R.id.collab_ranking_tv_subtitle, "Tus puntos y progreso");
            if (ownPoints == null) {
                showStatusBanner("Consultando tus puntos y clasificación…", false);
                renderRealSnapshot();
            } else {
                hideStatusBanner();
                renderRealSnapshot();
            }
            loadOwnPoints();
            loadClosedRanking();
            return;
        }
        hideStatusBanner();
        renderPreviewSnapshot();
    }

    private void renderCurrentData() {
        if (activeProfile != null && CollaboratorDataState.isRealWorker(activeProfile)) {
            renderRealSnapshot();
        } else {
            renderPreviewSnapshot();
        }
    }

    private void loadOwnPoints() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) return;
        if (ownPointsRequest != null) ownPointsRequest.cancel();
        final int workerId = session.getWorkerId();
        final String token = session.getAccessToken();
        ownPointsRequest = ApiClient.getService().getOwnPoints(session.getBearerToken());
        ownPointsRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                SessionManager current = new SessionManager(CollaboratorRankingPointsActivity.this);
                if (call.isCanceled() || isFinishing() || isDestroyed() || current.getWorkerId() != workerId
                        || !token.equals(current.getAccessToken())) return;
                ApiResponse<JsonObject> envelope = response.body();
                JsonObject data = envelope == null ? null : envelope.getData();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful() || data == null
                        || intOrZero(data, "id_trabajador") != workerId) {
                    if (response.code() == 401) {
                        current.clearActiveSession();
                        AccountNavigation.login(CollaboratorRankingPointsActivity.this, false);
                        return;
                    }
                    if (ownPoints == null) {
                        showStatusBanner("No se pudieron consultar los puntos de tu cuenta.", true);
                    }
                    return;
                }
                ownPoints = data;
                ownPointsWorkerId = workerId;
                current.savePointsBalance(intOrZero(data, "saldo_actual"));
                hideStatusBanner();
                renderRealSnapshot();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!call.isCanceled() && !isFinishing() && ownPoints == null) {
                    showStatusBanner("Sin conexión. No se pudo actualizar tu historial de puntos.", true);
                }
            }
        });
    }

    private void loadClosedRanking() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) return;
        if (rankingRequest != null) rankingRequest.cancel();
        final int workerId = session.getWorkerId();
        final String token = session.getAccessToken();
        rankingRequest = ApiClient.getService().getLatestRanking(session.getBearerToken(), "MENSUAL");
        rankingRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                SessionManager current = new SessionManager(CollaboratorRankingPointsActivity.this);
                if (call.isCanceled() || isFinishing() || isDestroyed() || current.getWorkerId() != workerId
                        || !token.equals(current.getAccessToken())) return;
                ApiResponse<JsonObject> envelope = response.body();
                JsonObject data = envelope == null ? null : envelope.getData();
                if (response.isSuccessful() && envelope != null && envelope.isSuccessful() && data != null
                        && intOrZero(data, "id_trabajador") == workerId) {
                    closedRanking = data;
                    rankingStatus = "";
                } else {
                    closedRanking = null;
                    rankingStatus = response.code() == 403
                            ? "Tu cuenta no tiene permiso para ver la clasificación."
                            : response.code() == 401
                            ? "Sesión vencida. Vuelve a iniciar sesión."
                            : "No se pudo consultar la clasificación cerrada.";
                    if (response.code() == 401) {
                        current.clearActiveSession();
                        AccountNavigation.login(CollaboratorRankingPointsActivity.this, false);
                        return;
                    }
                }
                renderRealSnapshot();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!call.isCanceled() && !isFinishing()) {
                    rankingStatus = "Sin conexión para consultar la clasificación.";
                    renderRealSnapshot();
                }
            }
        });
    }

    private void renderRealSnapshot() {
        SessionManager session = new SessionManager(this);
        CollaboratorProgressSnapshot snap = CollaboratorProgressSnapshot.fromServerData(
                ownPoints,
                closedRanking,
                session.getWorkerId(),
                selectedEvalWindow,
                System.currentTimeMillis()
        );

        // 1. Tarjeta principal: Mi nivel actual (sin niveles inventados)
        if (snap.currentLevel != null) {
            setText(R.id.collab_prog_tv_badge_tag, "Nivel " + snap.currentLevel.number);
            setText(R.id.collab_prog_tv_level_name, snap.currentLevel.name + " — Nivel " + snap.currentLevel.number);
            if (snap.nextLevel != null) {
                int span = Math.max(1, snap.nextLevel.requiredPoints - snap.currentLevel.requiredPoints);
                int gained = Math.max(0, snap.totalPoints - snap.currentLevel.requiredPoints);
                int pct = Math.min(100, Math.round(gained * 100f / span));
                animateHorizontalProgress(findViewById(R.id.collab_prog_pb_level), pct);
                setText(R.id.collab_prog_tv_level_desc, "Avanza hacia " + snap.nextLevel.name + " completando tus actividades.");
                setText(R.id.collab_prog_tv_level_points,
                        CollaboratorProgressSnapshot.formatPoints(snap.totalPoints) + " / "
                                + CollaboratorProgressSnapshot.formatPoints(snap.nextLevel.requiredPoints) + " pts");
            } else {
                animateHorizontalProgress(findViewById(R.id.collab_prog_pb_level), 100);
                setText(R.id.collab_prog_tv_level_desc, "Has alcanzado el nivel más alto configurado.");
                setText(R.id.collab_prog_tv_level_points,
                        CollaboratorProgressSnapshot.formatPoints(snap.totalPoints) + " pts acumulados");
            }
        } else {
            boolean hasMedal = snap.medal != null && !snap.medal.isEmpty();
            setText(R.id.collab_prog_tv_badge_tag, hasMedal ? formatMedal(snap.medal) : "Sin nivel");
            setText(R.id.collab_prog_tv_level_name,
                    hasMedal ? "Reconocimiento " + formatMedal(snap.medal) : "Sin nivel asignado aún");
            setText(R.id.collab_prog_tv_level_desc,
                    snap.totalPoints > 0
                            ? "Tus puntos confirmados están activos. El sistema de niveles se mostrará aquí cuando sea habilitado."
                            : "Aún no registras puntos ni un nivel asignado. Completa tus actividades evaluadas para comenzar.");
            animateHorizontalProgress(findViewById(R.id.collab_prog_pb_level), 0);
            setText(R.id.collab_prog_tv_level_points,
                    CollaboratorProgressSnapshot.formatPoints(snap.totalPoints) + " pts acumulados");
        }
        animateShieldEntrance();

        // 2. Tres tarjetas de estadísticas
        setText(R.id.collab_prog_stat_points, CollaboratorProgressSnapshot.formatPoints(snap.totalPoints));
        setText(R.id.collab_prog_stat_rank, snap.rankPosition == null ? "—" : "#" + snap.rankPosition);
        setText(R.id.collab_prog_stat_org, snap.topPercentile == null ? "—" : "Top " + snap.topPercentile + "%");

        // 3. Línea visual de progreso por niveles
        renderLevelsRoadmap(snap.levels);

        // 4. Evaluación de puntos (Semana / Mes + Gráfico circular)
        updateEvalSelectorUI();
        String periodText;
        if (selectedEvalWindow == EvalWindow.WEEK) {
            periodText = "Últimos 7 días";
            setText(R.id.collab_prog_tv_eval_caption, "Progreso semanal");
        } else {
            String formattedMonth = CollaboratorProgressSnapshot.formatPeriodMonthLabel(snap.closedPeriodKey);
            periodText = formattedMonth.isEmpty() ? "Últimos 31 días" : formattedMonth;
            setText(R.id.collab_prog_tv_eval_caption, "Progreso mensual");
        }
        setText(R.id.collab_prog_tv_eval_period, periodText);

        CircularProgressIndicator circular = findViewById(R.id.collab_prog_circular_eval);
        if (snap.progressPercent != null) {
            animateCircularProgress(circular, snap.progressPercent);
            setText(R.id.collab_prog_tv_eval_pct, snap.progressPercent + "%");
            setText(R.id.collab_prog_tv_eval_ring_sub, "Cumplimiento");
        } else {
            animateCircularProgress(circular, 0);
            setText(R.id.collab_prog_tv_eval_pct, "—");
            setText(R.id.collab_prog_tv_eval_ring_sub, "Sin KPI aprobados");
        }

        setText(R.id.collab_prog_tv_eval_activities, String.valueOf(snap.activitiesCompleted));
        setText(R.id.collab_prog_tv_eval_points,
                (snap.pointsEarnedInPeriod > 0 ? "+" : "")
                        + CollaboratorProgressSnapshot.formatPoints(snap.pointsEarnedInPeriod) + " pts");
        setText(R.id.collab_prog_tv_eval_team_avg,
                snap.teamAverageCompliance == null
                        ? "Sin datos"
                        : String.format(Locale.US, "%.0f%%", snap.teamAverageCompliance));

        View evalEmpty = findViewById(R.id.collab_prog_tv_eval_empty);
        if (evalEmpty != null) {
            evalEmpty.setVisibility(snap.hasPeriodData ? View.GONE : View.VISIBLE);
        }

        // 5. Ranking general
        String formattedMonth = CollaboratorProgressSnapshot.formatPeriodMonthLabel(snap.closedPeriodKey);
        setText(R.id.collab_prog_tv_ranking_period,
                formattedMonth.isEmpty()
                        ? "Tu puesto en el ranking general"
                        : "Período: " + formattedMonth);

        String ownPhoto = session.isLoggedIn() ? session.getWorkerPhoto() : "";
        renderRankingRows(
                snap.visibleRankingRows(rankingExpanded),
                snap.rankingRows.size(),
                ownPhoto,
                closedRanking == null && !rankingStatus.isEmpty()
                        ? rankingStatus
                        : "Aún no hay KPI aprobados este mes."
        );
    }

    /**
     * Modo de vista previa local únicamente cuando no hay sesión real activa.
     */
    private void renderPreviewSnapshot() {
        if (activeProfile == null) return;
        String collabId = activeProfile.getId();
        CollaboratorScoreSnapshot snap = CollaboratorRewardsRankingDataSource.getSnapshotForCollaborator(collabId);

        setText(R.id.collab_ranking_tv_subtitle, "Tus puntos y progreso");
        setText(R.id.collab_prog_tv_badge_tag, snap.tierName);
        setText(R.id.collab_prog_tv_level_name, snap.tierName);
        setText(R.id.collab_prog_tv_level_desc, snap.comparisonSummaryText);
        animateHorizontalProgress(findViewById(R.id.collab_prog_pb_level), snap.tierProgressPercent);
        setText(R.id.collab_prog_tv_level_points,
                snap.pointsNeededForNextTier <= 0
                        ? snap.getFormattedTotalPoints() + " · Nivel máximo"
                        : snap.getFormattedTotalPoints() + " · Faltan "
                        + CollaboratorRewardsRankingDataSource.formatNumber(snap.pointsNeededForNextTier) + " pts");
        animateShieldEntrance();

        List<LeaderboardEntry> entries = CollaboratorRewardsRankingDataSource.getLeaderboard(
                selectedEvalWindow == EvalWindow.WEEK ? PeriodFilter.CURRENT_WEEK : PeriodFilter.CURRENT_MONTH);
        int totalParticipants = entries.size();
        int topPct = totalParticipants >= 2
                ? Math.max(1, Math.round(snap.rankPosition * 100f / totalParticipants)) : 100;

        setText(R.id.collab_prog_stat_points, CollaboratorRewardsRankingDataSource.formatNumber(snap.totalPoints));
        setText(R.id.collab_prog_stat_rank, "#" + snap.rankPosition);
        setText(R.id.collab_prog_stat_org, "Top " + topPct + "%");

        List<LevelNode> demoLevels = Arrays.asList(
                new LevelNode(1, "Bronce", 0, true, "Rango Bronce".equals(snap.tierName)),
                new LevelNode(2, "Plata", 500, snap.totalPoints >= 500, "Rango Plata".equals(snap.tierName)),
                new LevelNode(3, "Oro", 1000, snap.totalPoints >= 1000, "Rango Oro".equals(snap.tierName)),
                new LevelNode(4, "Platino", 1500, snap.totalPoints >= 1500, "Rango Platino".equals(snap.tierName))
        );
        renderLevelsRoadmap(demoLevels);

        updateEvalSelectorUI();
        boolean isWeek = selectedEvalWindow == EvalWindow.WEEK;
        setText(R.id.collab_prog_tv_eval_period, isWeek ? "Semana actual" : "Mes actual");
        setText(R.id.collab_prog_tv_eval_caption, isWeek ? "Progreso semanal" : "Progreso mensual");
        animateCircularProgress(findViewById(R.id.collab_prog_circular_eval), snap.tierProgressPercent);
        setText(R.id.collab_prog_tv_eval_pct, snap.tierProgressPercent + "%");
        setText(R.id.collab_prog_tv_eval_ring_sub, "Avance");
        setText(R.id.collab_prog_tv_eval_activities, String.valueOf(activeProfile.getCompletedKpisCount()));
        setText(R.id.collab_prog_tv_eval_points,
                "+" + CollaboratorRewardsRankingDataSource.formatNumber(
                        isWeek ? snap.weeklyGainedPoints : snap.totalPoints) + " pts");
        setText(R.id.collab_prog_tv_eval_team_avg, "Sin datos");
        View evalEmpty = findViewById(R.id.collab_prog_tv_eval_empty);
        if (evalEmpty != null) evalEmpty.setVisibility(View.GONE);

        List<RankingRow> rows = new ArrayList<>();
        for (LeaderboardEntry entry : entries) {
            rows.add(new RankingRow(
                    0,
                    entry.rank,
                    entry.fullName,
                    CollaboratorProgressSnapshot.initialsOf(entry.fullName),
                    entry.departmentAndTier,
                    "",
                    isWeek ? entry.weeklyPoints : entry.totalPoints,
                    entry.collabId.equals(collabId)
            ));
        }
        List<RankingRow> visible = (!rankingExpanded && rows.size() > 5) ? rows.subList(0, 5) : rows;
        renderRankingRows(visible, rows.size(), "", "Sin clasificación disponible.");
    }

    private void renderLevelsRoadmap(List<LevelNode> levels) {
        View scroll = findViewById(R.id.collab_prog_levels_scroll);
        LinearLayout container = findViewById(R.id.collab_prog_levels_container);
        View empty = findViewById(R.id.collab_prog_levels_empty);
        if (scroll == null || container == null || empty == null) return;

        if (levels == null || levels.isEmpty()) {
            scroll.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            return;
        }

        scroll.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        container.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        int activeLineColor = ContextCompat.getColor(this, R.color.collab_prog_violet_glow);
        int inactiveLineColor = ContextCompat.getColor(this, R.color.collab_prog_border_subtle);
        int goldColor = ContextCompat.getColor(this, R.color.collab_prog_gold);
        int lockedColor = ContextCompat.getColor(this, R.color.collab_prog_text_secondary);

        for (int i = 0; i < levels.size(); i++) {
            LevelNode node = levels.get(i);
            View item = inflater.inflate(R.layout.collaborator_item_progress_level_node, container, false);
            View lineLeft = item.findViewById(R.id.collab_level_line_left);
            View lineRight = item.findViewById(R.id.collab_level_line_right);
            FrameLayout circle = item.findViewById(R.id.collab_level_node_circle);
            ImageView icon = item.findViewById(R.id.collab_level_node_icon);
            TextView tvName = item.findViewById(R.id.collab_level_tv_name);
            TextView tvNum = item.findViewById(R.id.collab_level_tv_number);

            lineLeft.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            lineRight.setVisibility(i == levels.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            lineLeft.setBackgroundColor(node.completed ? activeLineColor : inactiveLineColor);
            boolean nextReached = (i + 1 < levels.size()) && levels.get(i + 1).completed;
            lineRight.setBackgroundColor(nextReached ? activeLineColor : inactiveLineColor);

            int circleDp = node.current ? 44 : 34;
            ViewGroup.LayoutParams lp = circle.getLayoutParams();
            lp.width = dp(circleDp);
            lp.height = dp(circleDp);
            circle.setLayoutParams(lp);

            if (node.current) {
                circle.setBackgroundResource(R.drawable.collab_bg_level_node_current);
                icon.setImageResource(R.drawable.collaborator_ic_star);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(goldColor));
                tvName.setTextColor(ContextCompat.getColor(this, R.color.collab_text_primary));
                tvNum.setTextColor(ContextCompat.getColor(this, R.color.collab_prog_violet_glow));
            } else if (node.completed) {
                circle.setBackgroundResource(R.drawable.collab_bg_level_node_done);
                icon.setImageResource(R.drawable.collaborator_ic_star);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(Color.WHITE));
                tvName.setTextColor(ContextCompat.getColor(this, R.color.collab_text_primary));
                tvNum.setTextColor(ContextCompat.getColor(this, R.color.collab_prog_text_secondary));
            } else {
                circle.setBackgroundResource(R.drawable.collab_bg_level_node_locked);
                icon.setImageResource(R.drawable.collab_ic_lock);
                ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(lockedColor));
                tvName.setTextColor(lockedColor);
                tvNum.setTextColor(lockedColor);
                item.setAlpha(0.76f);
            }

            tvName.setText(node.name);
            tvNum.setText("Nivel " + node.number);
            container.addView(item);
        }
        CollaboratorThemeHelper.applyReadableTypeface(container);
    }

    private void renderRankingRows(List<RankingRow> rows, int totalCount, String ownPhoto, String emptyMessage) {
        LinearLayout list = findViewById(R.id.collab_prog_ranking_list);
        View emptyBox = findViewById(R.id.collab_prog_ranking_empty);
        TextView btnView = findViewById(R.id.collab_prog_btn_view_ranking);
        if (list == null || emptyBox == null) return;

        list.removeAllViews();
        if (btnView != null) {
            if (totalCount > 5) {
                btnView.setVisibility(View.VISIBLE);
                btnView.setText(rankingExpanded ? "Ver menos" : "Ver ranking");
            } else {
                btnView.setVisibility(View.GONE);
            }
        }

        if (rows == null || rows.isEmpty()) {
            list.setVisibility(View.GONE);
            emptyBox.setVisibility(View.VISIBLE);
            setText(R.id.collab_prog_tv_ranking_empty_msg, emptyMessage);
            return;
        }

        list.setVisibility(View.VISIBLE);
        emptyBox.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (RankingRow row : rows) {
            View item = inflater.inflate(R.layout.collaborator_item_ranking_row, list, false);
            View root = item.findViewById(R.id.collab_ranking_row_root);
            FrameLayout posBadge = item.findViewById(R.id.collab_ranking_row_pos_badge);
            TextView tvPos = item.findViewById(R.id.collab_ranking_row_pos);
            ImageView ivPhoto = item.findViewById(R.id.collab_ranking_row_photo);
            TextView tvInitials = item.findViewById(R.id.collab_ranking_row_initials);
            TextView tvName = item.findViewById(R.id.collab_ranking_row_name);
            TextView tvMeta = item.findViewById(R.id.collab_ranking_row_meta);
            TextView tvPoints = item.findViewById(R.id.collab_ranking_row_points);

            root.setBackgroundResource(row.currentUser
                    ? R.drawable.collab_bg_ranking_row_self
                    : R.drawable.collab_bg_progress_stat_card);

            tvPos.setText("#" + row.position);
            if (row.position == 1) {
                posBadge.setBackgroundResource(R.drawable.collab_bg_icon_badge_amber);
                tvPos.setTextColor(ContextCompat.getColor(this, R.color.collab_prog_gold));
            } else if (row.position == 2) {
                posBadge.setBackgroundResource(R.drawable.collab_bg_icon_badge_cyan);
                tvPos.setTextColor(ContextCompat.getColor(this, R.color.collab_cyan_accent));
            } else if (row.position == 3) {
                posBadge.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);
                tvPos.setTextColor(ContextCompat.getColor(this, R.color.collab_prog_violet_glow));
            } else {
                posBadge.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);
                tvPos.setTextColor(ContextCompat.getColor(this, R.color.collab_prog_text_secondary));
            }

            tvInitials.setText(row.initials);
            if (row.currentUser && ownPhoto != null && !ownPhoto.trim().isEmpty()) {
                CollaboratorProfilePhotoLoader.load(this, ivPhoto, tvInitials, ownPhoto);
            } else {
                ivPhoto.setVisibility(View.GONE);
                tvInitials.setVisibility(View.VISIBLE);
            }

            tvName.setText(row.currentUser ? row.fullName + " (Tú)" : row.fullName);
            if (row.roleOrArea != null && !row.roleOrArea.isEmpty()) {
                tvMeta.setText(row.roleOrArea);
                tvMeta.setVisibility(View.VISIBLE);
            } else if (row.medal != null && !row.medal.isEmpty()) {
                tvMeta.setText("Medalla " + formatMedal(row.medal));
                tvMeta.setVisibility(View.VISIBLE);
            } else {
                tvMeta.setVisibility(View.GONE);
            }

            tvPoints.setText(CollaboratorProgressSnapshot.formatPoints(row.periodPoints) + " pts");
            tvPoints.setTextColor(ContextCompat.getColor(this,
                    row.currentUser ? R.color.collab_prog_violet_glow : R.color.collab_text_primary));

            list.addView(item);
        }
        CollaboratorThemeHelper.applyReadableTypeface(list);
    }

    private void updateEvalSelectorUI() {
        TextView btnWeek = findViewById(R.id.collab_prog_btn_week);
        TextView btnMonth = findViewById(R.id.collab_prog_btn_month);
        boolean isWeek = selectedEvalWindow == EvalWindow.WEEK;
        if (btnWeek != null) {
            btnWeek.setBackgroundResource(isWeek ? R.drawable.collab_bg_chip_selected : 0);
            btnWeek.setTextColor(isWeek ? Color.WHITE : ContextCompat.getColor(this, R.color.collab_prog_text_secondary));
        }
        if (btnMonth != null) {
            btnMonth.setBackgroundResource(!isWeek ? R.drawable.collab_bg_chip_selected : 0);
            btnMonth.setTextColor(!isWeek ? Color.WHITE : ContextCompat.getColor(this, R.color.collab_prog_text_secondary));
        }
    }

    private void showStatusBanner(String msg, boolean showRetry) {
        View banner = findViewById(R.id.collab_prog_banner_status);
        View retry = findViewById(R.id.collab_prog_btn_retry);
        if (banner != null) banner.setVisibility(View.VISIBLE);
        setText(R.id.collab_prog_tv_banner_msg, msg);
        if (retry != null) retry.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void hideStatusBanner() {
        View banner = findViewById(R.id.collab_prog_banner_status);
        if (banner != null) banner.setVisibility(View.GONE);
    }

    private void scrollToRankingSection() {
        ScrollView scroll = findViewById(R.id.collab_ranking_scroll);
        View rankingSection = findViewById(R.id.collab_prog_section_ranking);
        if (scroll != null && rankingSection != null) {
            scroll.post(() -> scroll.smoothScrollTo(0, rankingSection.getTop()));
        }
    }

    private void animateShieldEntrance() {
        if (shieldAnimatedOnce) return;
        shieldAnimatedOnce = true;
        View shield = findViewById(R.id.collab_prog_iv_shield);
        if (shield == null || !animationsEnabled()) return;
        shield.setAlpha(0.3f);
        shield.setScaleX(0.92f);
        shield.setScaleY(0.92f);
        shield.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(240L)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateHorizontalProgress(@Nullable ProgressBar bar, int target) {
        if (bar == null) return;
        int clamped = Math.max(0, Math.min(100, target));
        if (!animationsEnabled()) {
            bar.setProgress(clamped);
            return;
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(bar, "progress", bar.getProgress(), clamped);
        anim.setDuration(240L);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private void animateCircularProgress(@Nullable CircularProgressIndicator indicator, int target) {
        if (indicator == null) return;
        int clamped = Math.max(0, Math.min(100, target));
        if (!animationsEnabled()) {
            indicator.setProgressCompat(clamped, false);
            return;
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(indicator, "progress", indicator.getProgress(), clamped);
        anim.setDuration(260L);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private boolean animationsEnabled() {
        try {
            float scale = Settings.Global.getFloat(
                    getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f);
            return scale > 0f;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static String formatMedal(String medal) {
        if (medal == null || medal.trim().isEmpty()) return "";
        String lower = medal.trim().toLowerCase(new Locale("es", "ES"));
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static int intOrZero(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) {
            tv.setText(text);
        }
    }
}
