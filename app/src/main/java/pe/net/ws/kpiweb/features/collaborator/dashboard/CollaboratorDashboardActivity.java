package pe.net.ws.kpiweb.features.collaborator.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.gson.JsonObject;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.account.AccountActivity;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.data.CollaboratorMockDataSource;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorActivityItem;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorPerformanceSummary;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorPersonalStats;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsActivity;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorRankingPointsActivity;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsActivity;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessActivity;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignment;
import pe.net.ws.kpiweb.features.kpi.remote.KpiServerAssignments;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad Principal del Dashboard del Colaborador (WEBSITE).
 * Presenta una estructura limpia sin menú hamburguesa superior, barra de navegación
 * inferior de 5 opciones (INICIO | BIENESTAR | KPI | NOTIFICACIONES | MENÚ) y datos dinámicos
 * diferenciados por colaborador (Lucía Gómez, Carlos Mendoza y Elena Rojas).
 */
public class CollaboratorDashboardActivity extends AppCompatActivity {

    private CollaboratorProfile currentProfile;
    private Call<ApiResponse<LoginResponseData>> profileRequest;
    private Call<ApiResponse<KpiServerAssignments>> kpiRequest;
    private Call<ApiResponse<JsonObject>> formRequest;
    private Call<ApiResponse<JsonObject>> allowedFormsRequest;
    private Call<ApiResponse<JsonObject>> pointsRequest;
    private Call<ApiResponse<JsonObject>> attendanceRequest;
    private KpiServerAssignments latestKpis;
    private int latestKpisWorkerId;
    private Integer latestRawFormCount;
    private int latestRawFormsWorkerId;
    private int latestAllowedFormCount;
    private int latestAllowedFormsWorkerId;
    private String latestAllowedFormsPeriod;
    private int latestSubmittedFormTypes;
    private int latestPendingShifts;
    private String latestFormDueDate;
    private Integer latestBalance;
    private int latestPointsWorkerId;
    private Integer latestAttendanceDays;
    private int latestAttendanceWorkerId;
    private String latestAttendanceMonth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_dashboard);

        CollaboratorThemeHelper.setupSystemBars(this);

        currentProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();

        setupHeaderInteractions();
        setupQuickActions();
        findViewById(R.id.collab_tv_stat_kpi_sub).setOnClickListener(v ->
                pe.net.ws.kpiweb.features.kpi.ui.KpiServerReportBrowser.show(this,
                        new SessionManager(this).getWorkerId()));
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.HOME);

        bindCollaboratorProfile(currentProfile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager activeSession = new SessionManager(this);
        if (activeSession.isLoggedIn() && activeSession.getWorkerId() > 0) {
            pe.net.ws.kpiweb.features.kpi.notification.KpiReminderScheduler.scheduleAllReminders(this);
            pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.retry(this);
        }
        CollaboratorThemeHelper.setupSystemBars(this);
        currentProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        bindCollaboratorProfile(currentProfile);
        if (latestKpis != null && latestKpisWorkerId == new SessionManager(this).getWorkerId()
                && pe.net.ws.kpiweb.core.time.IsoWeekPeriod.currentInLima().equals(latestKpis.getPeriod()))
            renderRealKpis(latestKpis);
        if (latestBalance != null && latestPointsWorkerId == new SessionManager(this).getWorkerId()) renderRealPoints(latestBalance);
        refreshServerData();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.HOME);
    }

    private void setupHeaderInteractions() {
        View btnProfileSwitch = findViewById(R.id.collab_btn_profile_switcher);
        if (btnProfileSwitch != null) {
            // Comportamiento unificado de usuario final:
            // Al pulsar el avatar superior derecho, SIEMPRE abre "Mi cuenta"
            btnProfileSwitch.setOnClickListener(v -> {
                Intent intent = new Intent(this, AccountActivity.class);
                intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
                startActivity(intent);
            });

            // En MODO DESARROLLO (Debug build / FLAG_DEBUGGABLE):
            // Una pulsación larga (Long-click) abre un diálogo modal discreto
            // para alternar entre perfiles de prueba, sin superponer tarjetas en el Dashboard.
            // En MODO PRODUCCIÓN (Release):
            // Deshabilitado por completo; el colaborador solo accede a Mi cuenta.
            if (isDebugBuild() && !new pe.net.ws.kpiweb.core.session.SessionManager(this).isLoggedIn()) {
                btnProfileSwitch.setOnLongClickListener(v -> {
                    showDevProfileSwitcherDialog();
                    return true;
                });
            } else {
                btnProfileSwitch.setOnLongClickListener(null);
            }
        }
    }

    private boolean isDebugBuild() {
        return (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * Selector discreto de desarrollo para alternar entre colaboradores de prueba.
     * Exclusivo de builds de desarrollo (BuildConfig.DEBUG).
     */
    private void showDevProfileSwitcherDialog() {
        String[] profiles = new String[]{
                "Lucía Gómez (Diseño y Producto)",
                "Carlos Mendoza (Área Comercial)",
                "Elena Rojas (Atención al Cliente)"
        };
        CollaboratorMockDataSource.DevProfileIndex[] indices = new CollaboratorMockDataSource.DevProfileIndex[]{
                CollaboratorMockDataSource.DevProfileIndex.DISENO,
                CollaboratorMockDataSource.DevProfileIndex.VENTAS,
                CollaboratorMockDataSource.DevProfileIndex.ATENCION_CLIENTE
        };

        CollaboratorMockDataSource.DevProfileIndex currentIndex = CollaboratorSessionManager.getInstance(this).getActiveProfileIndex();
        int selected = 0;
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] == currentIndex) {
                selected = i;
                break;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🛠️ Selector de Pruebas (Debug)")
                .setSingleChoiceItems(profiles, selected, (dialog, which) -> {
                    CollaboratorSessionManager.getInstance(this).setActiveProfileIndex(indices[which]);
                    currentProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
                    bindCollaboratorProfile(currentProfile);
                    CollaboratorBottomNavHelper.updateUnreadBadge(this);
                    dialog.dismiss();
                    CollaboratorBannerHelper.showSuccess(this, "Perfil de prueba cambiado a " + currentProfile.getFirstName());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void bindCollaboratorProfile(CollaboratorProfile profile) {
        if (profile == null) return;
        boolean realProfile = profile.getId() != null && profile.getId().matches("[0-9]+");

        // La identidad aparece compacta en el encabezado; el detalle laboral vive en Mi cuenta.
        TextView tvGreeting = findViewById(R.id.collab_tv_greeting);
        TextView tvFullName = findViewById(R.id.collab_tv_full_name);
        TextView tvSubtitle = findViewById(R.id.collab_tv_job_subtitle);
        TextView tvInitials = findViewById(R.id.collab_tv_avatar_initials);
        TextView tvProfileName = findViewById(R.id.collab_tv_profile_name);
        TextView tvWorkerType = findViewById(R.id.collab_tv_role_badge);
        TextView tvEvalStatusBadge = findViewById(R.id.collab_tv_eval_status_badge);

        String rawFirstName = profile.getFirstName() == null ? "" : profile.getFirstName().trim();
        String firstName = rawFirstName.isEmpty() ? "" : rawFirstName.split("\\s+")[0];

        if (tvGreeting != null) {
            tvGreeting.setText(firstName.isEmpty()
                    ? "Hola"
                    : getString(R.string.collab_greeting_prefix, firstName));
        }
        if (tvProfileName != null) {
            tvProfileName.setText(firstName.isEmpty() ? "Perfil" : firstName);
        }
        if (tvFullName != null) {
            tvFullName.setText("Este es tu resumen de hoy");
        }
        if (tvSubtitle != null) {
            tvSubtitle.setVisibility(View.GONE);
        }
        if (tvInitials != null) {
            tvInitials.setText(profile.getAvatarInitials());
        }
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorProfilePhotoLoader.load(
                this, findViewById(R.id.collab_iv_avatar_photo), tvInitials,
                new pe.net.ws.kpiweb.core.session.SessionManager(this).getWorkerPhoto());
        if (tvWorkerType != null) {
            String workerType = new pe.net.ws.kpiweb.core.session.SessionManager(this).getWorkerTipo();
            tvWorkerType.setText(workerType == null || workerType.trim().isEmpty()
                    ? "Colaborador" : workerType.trim());
        }

        // 2. Tarjeta Hero de Rendimiento Personal y Estado de Evaluación
        CollaboratorPerformanceSummary hero = profile.getPerformanceSummary();
        if (hero != null) {
            TextView tvPeriod = findViewById(R.id.collab_tv_hero_period);
            TextView tvHeroTitle = findViewById(R.id.collab_tv_hero_title);
            TextView tvHeroSubtitle = findViewById(R.id.collab_tv_hero_subtitle);
            TextView tvHeroDeliveries = findViewById(R.id.collab_tv_hero_deliveries);
            TextView tvHeroScore = findViewById(R.id.collab_tv_hero_score);
            TextView tvHeroScoreLabel = findViewById(R.id.collab_tv_hero_score_label);

            if (tvPeriod != null) {
                tvPeriod.setText(getString(R.string.collab_hero_badge_title) + " • " + hero.getPeriodLabel());
            }
            if (tvHeroTitle != null) {
                tvHeroTitle.setText(hero.getHeadline());
            }
            if (tvHeroSubtitle != null) {
                tvHeroSubtitle.setText(hero.getComparisonTrend());
            }
            if (tvHeroDeliveries != null) {
                tvHeroDeliveries.setText(hero.getSummaryFooter());
            }

            if (hero.hasScore() && hero.getScorePercentage() != null) {
                if (tvHeroScore != null) {
                    tvHeroScore.setText(hero.getScorePercentage() + "%");
                    tvHeroScore.setTextSize(20f);
                }
                if (tvHeroScoreLabel != null) {
                    tvHeroScoreLabel.setText("Cumplimiento");
                }
                if (tvEvalStatusBadge != null) {
                    tvEvalStatusBadge.setText("Evaluación activa");
                    tvEvalStatusBadge.setBackgroundResource(R.drawable.collab_bg_pill_green);
                    tvEvalStatusBadge.setTextColor(getColor(R.color.collab_status_completed));
                }
            } else {
                // Cuando aún no existe puntaje disponible: muestra "Pendiente de evaluación"
                if (tvHeroScore != null) {
                    tvHeroScore.setText("Pendiente");
                    tvHeroScore.setTextSize(12f);
                }
                if (tvHeroScoreLabel != null) {
                    tvHeroScoreLabel.setText("de evaluación");
                }
                if (tvEvalStatusBadge != null) {
                    tvEvalStatusBadge.setText(getString(R.string.collab_hero_pending_eval));
                    tvEvalStatusBadge.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                    tvEvalStatusBadge.setTextColor(getColor(R.color.collab_status_pending));
                }
            }
        }

        // 3. Resumen de KPI (KPI asignados, KPI completados, KPI pendientes, Próxima fecha de entrega)
        TextView tvKpiAssigned = findViewById(R.id.collab_tv_kpi_assigned_count);
        TextView tvKpiCompleted = findViewById(R.id.collab_tv_kpi_completed_count);
        TextView tvKpiPending = findViewById(R.id.collab_tv_kpi_pending_count);
        TextView tvKpiNextDue = findViewById(R.id.collab_tv_kpi_next_due_summary);

        if (tvKpiAssigned != null) {
            tvKpiAssigned.setText(realProfile && profile.getKpis().isEmpty() ? "—" : String.valueOf(profile.getAssignedKpisCount()));
        }
        if (tvKpiCompleted != null) {
            tvKpiCompleted.setText(realProfile && profile.getKpis().isEmpty() ? "—" : String.valueOf(profile.getCompletedKpisCount()));
        }
        if (tvKpiPending != null) {
            tvKpiPending.setText(realProfile && profile.getKpis().isEmpty() ? "—" : String.valueOf(profile.getPendingKpisCount()));
        }
        if (tvKpiNextDue != null) {
            tvKpiNextDue.setText(profile.getNextDeliveryDateDisplay());
        }

        View kpiSummaryCard = findViewById(R.id.collab_card_kpi_summary_strip);
        if (kpiSummaryCard != null) {
            kpiSummaryCard.setOnClickListener(v -> CollaboratorKpiIntegrationActivity.openKpiSection(this));
        }

        // 4. Cuatro Tarjetas de Estadísticas Personales
        CollaboratorPersonalStats stats = profile.getStats();
        if (stats != null) {
            setText(R.id.collab_tv_stat_points_value, stats.getPointsDisplay());
            setText(R.id.collab_tv_stat_points_trend, stats.getPointsTrend());
            setText(R.id.collab_tv_stat_ranking_value, stats.getRankingDisplay());
            setText(R.id.collab_tv_stat_ranking_trend, stats.getRankingTrend());
            setText(R.id.collab_tv_stat_kpi_value, stats.getKpiDeliveredDisplay());
            setText(R.id.collab_tv_stat_kpi_sub, stats.getKpiDeliveredSub());
            setText(R.id.collab_tv_stat_attendance_value, stats.getAttendanceDisplay());
            setText(R.id.collab_tv_stat_attendance_badge, stats.getAttendanceBadge());

            ProgressBar pbKpi = findViewById(R.id.collab_pb_stat_kpi);
            if (pbKpi != null) {
                pbKpi.setProgress(stats.getKpiProgressPercent());
            }
        }
        if (latestAttendanceDays != null && latestAttendanceWorkerId == new SessionManager(this).getWorkerId()
                && currentMonth().equals(latestAttendanceMonth)) {
            renderRealAttendance(latestAttendanceDays);
        }

        // 5. Próxima Entrega / Actividad Destacada
        CollaboratorActivityItem nextAct = profile.getNextActivity();
        if (nextAct != null) {
            setText(R.id.collab_tv_next_due_date, nextAct.getTimeLabel());
            setText(R.id.collab_tv_next_title, nextAct.getTitle());
            setText(R.id.collab_tv_next_subtitle, nextAct.getCategory());

            TextView btnNextAction = findViewById(R.id.collab_btn_next_action);
            if (btnNextAction != null) {
                btnNextAction.setText(nextAct.getActionButtonLabel() != null
                        ? nextAct.getActionButtonLabel()
                        : getString(R.string.collab_hero_btn_view_kpis));
                btnNextAction.setOnClickListener(v -> CollaboratorKpiIntegrationActivity.openKpiSection(this));
            }
        } else {
            setText(R.id.collab_tv_next_due_date, realProfile ? "Por consultar" : "Al día");
            setText(R.id.collab_tv_next_title, realProfile ? "Entregas por consultar" : getString(R.string.collab_next_delivery_empty_title));
            setText(R.id.collab_tv_next_subtitle, realProfile
                    ? "Aún no se han consultado tus fechas de entrega reales."
                    : getString(R.string.collab_next_delivery_empty_sub));
        }

        // 6. Actividades Recientes
        bindRecentActivities(profile.getRecentActivities());
    }

    private void refreshServerData() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) return;
        final int workerId = session.getWorkerId();
        final int userId = session.getUserId();
        final String token = session.getAccessToken();
        if (latestRawFormsWorkerId != workerId) latestRawFormCount = null;
        if (latestAllowedFormsWorkerId != workerId) latestAllowedFormCount = 0;
        if (profileRequest != null) profileRequest.cancel();
        profileRequest = ApiClient.getService().getProfile(session.getBearerToken());
        profileRequest.enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override public void onResponse(Call<ApiResponse<LoginResponseData>> call, Response<ApiResponse<LoginResponseData>> response) {
                if (!isCurrent(call, token)) return;
                ApiResponse<LoginResponseData> body = response.body();
                LoginResponseData data = body == null ? null : body.getData();
                if (response.isSuccessful() && body != null && body.isSuccessful() && data != null
                        && data.getUser() != null && data.getWorker() != null
                        && data.getUser().getIdUsuario() == userId
                        && data.getWorker().getIdTrabajador() == workerId) {
                    LoginResponseData.WorkerInfo worker = data.getWorker();
                    String verifiedName = worker.getNombreCompleto();
                    if (verifiedName == null || verifiedName.trim().isEmpty()) verifiedName = session.getFullName();
                    session.updateVerifiedWorkerDetails(workerId, verifiedName,
                            worker.getCargoNombre(), worker.getAgenciaNombre(), worker.getTipo(),
                            worker.getEmail(), worker.getFotoPerfil(), worker.getEstado());
                    currentProfile = CollaboratorSessionManager.getInstance(CollaboratorDashboardActivity.this).getActiveProfile();
                    bindCollaboratorProfile(currentProfile);
                    if (latestKpis != null && latestKpisWorkerId == workerId) renderRealKpis(latestKpis);
                    if (latestBalance != null && latestPointsWorkerId == workerId) renderRealPoints(latestBalance);
                } else {
                    Log.w("CollabDashboard", "profile_http=" + response.code());
                    showConnectionState(response.code() == 401 ? "Tu sesión venció. Vuelve a iniciar sesión."
                            : "No se pudo verificar tu perfil en WEBSITE.");
                }
            }
            @Override public void onFailure(Call<ApiResponse<LoginResponseData>> call, Throwable error) {
                if (isCurrent(call, token)) showConnectionState("Sin conexión. Se muestran datos de la última sesión.");
            }
        });

        if (kpiRequest != null) kpiRequest.cancel();
        String period = pe.net.ws.kpiweb.core.time.IsoWeekPeriod.currentInLima();
        kpiRequest = ApiClient.getService().getMisKpis(session.getBearerToken(), period);
        kpiRequest.enqueue(new Callback<ApiResponse<KpiServerAssignments>>() {
            @Override public void onResponse(Call<ApiResponse<KpiServerAssignments>> call, Response<ApiResponse<KpiServerAssignments>> response) {
                if (!isCurrent(call, token)) return;
                ApiResponse<KpiServerAssignments> body = response.body();
                KpiServerAssignments data = body == null ? null : body.getData();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || data == null || data.getWorkerId() != workerId) {
                    Log.w("CollabDashboard", "kpi_http=" + response.code());
                    showConnectionState(response.code() == 401 ? "Tu sesión venció. Vuelve a iniciar sesión."
                            : response.code() == 403 ? "Tu cuenta no tiene permiso para consultar sus KPI."
                            : "No se pudieron consultar tus KPI.");
                    return;
                }
                latestKpis = data;
                latestKpisWorkerId = workerId;
                renderRealKpis(data);
            }
            @Override public void onFailure(Call<ApiResponse<KpiServerAssignments>> call, Throwable error) {
                if (isCurrent(call, token)) showConnectionState("Sin conexión con tus KPI. Inténtalo de nuevo.");
            }
        });

        if (formRequest != null) formRequest.cancel();
        formRequest = ApiClient.getService().getKpiForms(session.getBearerToken(), "");
        formRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!isCurrent(call, token)) return;
                JsonObject data = response.body() == null ? null : response.body().getData();
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || data == null || !data.has("id_trabajador") || !data.has("total")
                        || data.get("id_trabajador").getAsInt() != workerId) return;
                latestRawFormCount = data.get("total").getAsInt();
                latestRawFormsWorkerId = workerId;
                if (latestKpis != null && latestKpisWorkerId == workerId) renderRealKpis(latestKpis);
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) { }
        });

        if (allowedFormsRequest != null) allowedFormsRequest.cancel();
        allowedFormsRequest = ApiClient.getService().getAllowedKpiForms(session.getBearerToken());
        allowedFormsRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call,
                                             Response<ApiResponse<JsonObject>> response) {
                if (!isCurrent(call, token)) return;
                JsonObject data = response.body() == null ? null : response.body().getData();
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || data == null || !data.has("id_trabajador")
                        || data.get("id_trabajador").getAsInt() != workerId
                        || !data.has("tipos_formulario") || !data.get("tipos_formulario").isJsonArray()) return;
                latestAllowedFormCount = data.getAsJsonArray("tipos_formulario").size();
                latestSubmittedFormTypes = data.has("tipos_con_entrega_semana")
                        && data.get("tipos_con_entrega_semana").isJsonArray()
                        ? data.getAsJsonArray("tipos_con_entrega_semana").size() : 0;
                latestPendingShifts = data.has("turnos_kpi_pendientes")
                        && !data.get("turnos_kpi_pendientes").isJsonNull()
                        ? data.get("turnos_kpi_pendientes").getAsInt() : 0;
                latestFormDueDate = data.has("proxima_entrega_kpi")
                        && !data.get("proxima_entrega_kpi").isJsonNull()
                        ? data.get("proxima_entrega_kpi").getAsString() : null;
                latestAllowedFormsWorkerId = workerId;
                latestAllowedFormsPeriod = period;
                if (latestKpis != null && latestKpisWorkerId == workerId) renderRealKpis(latestKpis);
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) { }
        });

        if (pointsRequest != null) pointsRequest.cancel();
        pointsRequest = ApiClient.getService().getOwnPoints(session.getBearerToken());
        pointsRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!isCurrent(call, token)) return;
                ApiResponse<JsonObject> body = response.body();
                JsonObject data = body == null ? null : body.getData();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || data == null) return;
                try {
                    if (data.get("id_trabajador").getAsInt() != workerId) return;
                    int balance = data.get("saldo_actual").getAsInt();
                    session.savePointsBalance(balance);
                    latestBalance = balance;
                    latestPointsWorkerId = workerId;
                    renderRealPoints(balance);
                } catch (RuntimeException ignored) { }
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) { }
        });

        if (attendanceRequest != null) attendanceRequest.cancel();
        final String attendanceMonth = currentMonth();
        attendanceRequest = ApiClient.getService().getOwnAttendance(session.getBearerToken(), attendanceMonth);
        attendanceRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!isCurrent(call, token)) return;
                ApiResponse<JsonObject> body = response.body();
                JsonObject data = body == null ? null : body.getData();
                JsonObject summary = data == null ? null : data.getAsJsonObject("resumen");
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || summary == null
                        || !summary.has("dias_con_marcacion")) {
                    setText(R.id.collab_tv_stat_attendance_badge, "No disponible");
                    return;
                }
                latestAttendanceDays = summary.get("dias_con_marcacion").getAsInt();
                latestAttendanceWorkerId = workerId;
                latestAttendanceMonth = attendanceMonth;
                renderRealAttendance(latestAttendanceDays);
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (isCurrent(call, token)) setText(R.id.collab_tv_stat_attendance_badge, "Sin conexión");
            }
        });
    }

    private void renderRealAttendance(int days) {
        setText(R.id.collab_tv_stat_attendance_value, String.valueOf(days));
        setText(R.id.collab_tv_stat_attendance_badge, days == 1 ? "Día con marcación este mes" : "Días con marcación este mes");
    }

    private static String currentMonth() {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM", Locale.ROOT);
        format.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));
        return format.format(new Date());
    }

    private void renderRealPoints(int balance) {
        setText(R.id.collab_tv_stat_points_value, String.valueOf(balance));
        setText(R.id.collab_tv_stat_points_trend, "Saldo confirmado");
    }

    private void showConnectionState(String message) {
        TextView status = findViewById(R.id.collab_tv_job_subtitle);
        if (status != null) {
            status.setText(message);
            status.setVisibility(View.VISIBLE);
        }
    }

    private void renderRealKpis(KpiServerAssignments data) {
        if (!pe.net.ws.kpiweb.core.time.IsoWeekPeriod.currentInLima().equals(data.getPeriod())) return;
        boolean allowedCurrent = latestAllowedFormsWorkerId == data.getWorkerId()
                && data.getPeriod().equals(latestAllowedFormsPeriod);
        int assigned = Math.max(data.getAssignments().size(),
                allowedCurrent ? latestAllowedFormCount : 0);
        int done = 0;
                KpiServerAssignment next = null;
                for (KpiServerAssignment item : data.getAssignments()) {
                    String status = item.getStatus() == null ? "" : item.getStatus();
                    boolean completed = "Aprobado".equalsIgnoreCase(status) || "Entregado".equalsIgnoreCase(status);
                    boolean active = !"Cancelado".equalsIgnoreCase(status) && !"No exigible".equalsIgnoreCase(status);
                    if (completed) done++;
                    if (active && !completed && item.getDueAt() != null && !item.getDueAt().isEmpty()
                            && (next == null || item.getDueAt().compareTo(next.getDueAt()) < 0)) next = item;
                }
                int received = allowedCurrent
                        ? latestSubmittedFormTypes : 0;
                int delivered = Math.min(assigned, Math.max(done, received));
                setText(R.id.collab_tv_kpi_assigned_count, String.valueOf(assigned));
                setText(R.id.collab_tv_kpi_completed_count, String.valueOf(delivered));
                setText(R.id.collab_tv_kpi_pending_count,
                        allowedCurrent
                                ? String.valueOf(latestPendingShifts) : "—");
                setText(R.id.collab_tv_stat_kpi_value, delivered + "/" + assigned);
                setText(R.id.collab_tv_stat_kpi_sub, "Período " + data.getPeriod()
                        + (received > done ? " · Reporte recibido, pendiente de aprobación" : ""));
                ProgressBar progress = findViewById(R.id.collab_pb_stat_kpi);
                if (progress != null) progress.setProgress(assigned == 0 ? 0 : delivered * 100 / assigned);
                String due = next != null ? next.getDueAt()
                        : allowedCurrent ? latestFormDueDate : null;
                setText(R.id.collab_tv_kpi_next_due_summary, due == null ? "Sin entregas pendientes" : due);
                setText(R.id.collab_tv_next_due_date, due == null ? "Al día" : due);
                setText(R.id.collab_tv_next_title, next != null ? next.getName()
                        : due == null ? "Sin próximas entregas" : "Próximo reporte KPI");
                setText(R.id.collab_tv_next_subtitle, next != null ? "KPI asignado · " + next.getStatus()
                        : due == null ? "No hay turno exigible pendiente en este período"
                        : "Según tu horario vigente en Asistencias");
    }

    private boolean isCurrent(Call<?> call, String expectedToken) {
        return !isFinishing() && !isDestroyed() && !call.isCanceled()
                && expectedToken.equals(new SessionManager(this).getAccessToken());
    }

    @Override protected void onDestroy() {
        if (profileRequest != null) profileRequest.cancel();
        if (kpiRequest != null) kpiRequest.cancel();
        if (formRequest != null) formRequest.cancel();
        if (allowedFormsRequest != null) allowedFormsRequest.cancel();
        if (pointsRequest != null) pointsRequest.cancel();
        if (attendanceRequest != null) attendanceRequest.cancel();
        super.onDestroy();
    }

    private void bindRecentActivities(List<CollaboratorActivityItem> activities) {
        LinearLayout container = findViewById(R.id.collab_container_recent_activities);
        if (container == null) return;

        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (activities == null || activities.isEmpty()) {
            TextView emptyTv = new TextView(this);
            boolean realProfile = currentProfile != null && currentProfile.getId() != null
                    && currentProfile.getId().matches("[0-9]+");
            emptyTv.setText(realProfile ? "Aún no se han consultado tus actividades reales." : getString(R.string.collab_recent_empty));
            emptyTv.setTextColor(getColor(R.color.collab_text_muted));
            emptyTv.setTextSize(13f);
            container.addView(emptyTv);
            return;
        }

        for (int i = 0; i < activities.size(); i++) {
            CollaboratorActivityItem item = activities.get(i);
            View row = inflater.inflate(R.layout.collaborator_item_recent_activity, container, false);

            TextView tvTitle = row.findViewById(R.id.collab_tv_item_title);
            TextView tvSub = row.findViewById(R.id.collab_tv_item_subtitle);
            TextView tvTime = row.findViewById(R.id.collab_tv_item_time);
            FrameLayout flIcon = row.findViewById(R.id.collab_fl_item_icon);
            ImageView ivIcon = row.findViewById(R.id.collab_iv_item_icon);

            if (tvTitle != null) tvTitle.setText(item.getTitle());
            if (tvSub != null) tvSub.setText(item.getCategory());
            if (tvTime != null) tvTime.setText(item.getTimeLabel());

            if (flIcon != null && ivIcon != null) {
                switch (item.getType()) {
                    case ENTREGA:
                        flIcon.setBackgroundResource(R.drawable.collab_bg_icon_badge_green);
                        ivIcon.setImageResource(R.drawable.collaborator_ic_check_circle);
                        ivIcon.setColorFilter(getColor(R.color.collab_status_completed));
                        break;
                    case ANUNCIO:
                        flIcon.setBackgroundResource(R.drawable.collab_bg_icon_badge_amber);
                        ivIcon.setImageResource(R.drawable.collab_ic_nav_bell);
                        ivIcon.setColorFilter(getColor(R.color.collab_status_pending));
                        break;
                    case EVALUACION:
                    case OBJETIVO:
                    case REVISION:
                    default:
                        flIcon.setBackgroundResource(R.drawable.collab_bg_icon_badge_purple);
                        ivIcon.setImageResource(R.drawable.collaborator_ic_star);
                        ivIcon.setColorFilter(getColor(R.color.collab_purple_accent));
                        break;
                }
            }

            if (i > 0) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
                if (lp != null) {
                    lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
                    row.setLayoutParams(lp);
                }
            }

            row.setOnClickListener(v -> {
                if (item.getType() == CollaboratorActivityItem.ActivityType.ANUNCIO) {
                    startActivity(new Intent(this, CollaboratorNotificationsActivity.class));
                } else {
                    CollaboratorKpiIntegrationActivity.openKpiSection(this);
                }
            });

            container.addView(row);
        }
    }

    private void setupQuickActions() {
        bindClick(R.id.collab_btn_hero_view_kpis, () ->
                CollaboratorKpiIntegrationActivity.openKpiSection(this));
        bindClick(R.id.collab_qa_btn_deliver_kpi, () ->
                startActivity(new Intent(this, pe.net.ws.kpiweb.features.collaborator.attendance.CollaboratorAttendanceActivity.class)));
        bindClick(R.id.collab_qa_btn_wellness, () ->
                startActivity(new Intent(this, CollaboratorRankingPointsActivity.class)));
        bindClick(R.id.collab_qa_btn_attendance, () ->
                startActivity(new Intent(this, CollaboratorRewardsActivity.class)));

        bindClick(R.id.collab_card_stat_points, () -> {
            Intent intent = new Intent(this, CollaboratorRankingPointsActivity.class);
            intent.putExtra(CollaboratorRankingPointsActivity.EXTRA_INITIAL_TAB,
                    CollaboratorRankingPointsActivity.TAB_POINTS);
            startActivity(intent);
        });
        bindClick(R.id.collab_card_stat_ranking, () -> {
            Intent intent = new Intent(this, CollaboratorRankingPointsActivity.class);
            intent.putExtra(CollaboratorRankingPointsActivity.EXTRA_INITIAL_TAB,
                    CollaboratorRankingPointsActivity.TAB_RANKING);
            startActivity(intent);
        });
        bindClick(R.id.collab_card_stat_kpis, () ->
                CollaboratorKpiIntegrationActivity.openKpiSection(this));
        bindClick(R.id.collab_card_stat_attendance, () ->
                startActivity(new Intent(this, pe.net.ws.kpiweb.features.collaborator.attendance.CollaboratorAttendanceActivity.class)));
        bindClick(R.id.collab_btn_recent_view_all, () ->
                CollaboratorKpiIntegrationActivity.openKpiSection(this));
    }

    private void bindClick(int viewId, Runnable action) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(view -> action.run());
        }
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) {
            tv.setText(text);
        }
    }
}
