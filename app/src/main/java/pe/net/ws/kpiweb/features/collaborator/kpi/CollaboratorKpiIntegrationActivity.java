package pe.net.ws.kpiweb.features.collaborator.kpi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorKpiItem;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.auth.KpiDefaultSessionManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.junior.data.KpiJuniorMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.notification.KpiReminderScheduler;
import pe.net.ws.kpiweb.features.kpi.ui.KpiNavigationHelper;
import pe.net.ws.kpiweb.features.kpi.ui.KpiThemeContrastApplier;

/**
 * Anfitrión integrado oficial para "Mis KPI".
 * Aloja el módulo real de Fragments desarrollado por Joel (KpiNavigationHelper):
 * - KpiSalesContainerFragment (Ventas)
 * - KpiDesignerContainerFragment (Diseñador Gráfico / Creativo)
 * - KpiJuniorContainerFragment (Diseñador Junior)
 * - KpiSeniorContainerFragment (Diseñador Senior)
 * - KpiFormFragment (Programador Junior)
 * - KpiDynamicCaptureFragment (Fotógrafo / Otros cargos)
 * 
 * Conserva intacta la identidad morada de WEBSITE, la cabecera con retorno,
 * y la barra de navegación inferior (INICIO | IA | KPI | NOTIFICACIONES | MENÚ).
 * No modifica ni altera las fórmulas de cálculo ni los motores matemáticos de Joel.
 */
public class CollaboratorKpiIntegrationActivity extends AppCompatActivity {
    public static final String EXTRA_RETURN_ADMIN = "pe.net.ws.kpiweb.RETURN_ADMIN_KPI";
    private String approvedFormType;
    private boolean returnToAdmin;

    public static void openKpiSection(Context context) {
        Intent intent = new Intent(context, CollaboratorKpiIntegrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) {
            pe.net.ws.kpiweb.navigation.AccountNavigation.login(this, false);
            finish();
            return;
        }
        setContentView(R.layout.collaborator_activity_kpi_integration);
        returnToAdmin = getIntent().getBooleanExtra(EXTRA_RETURN_ADMIN, false);

        CollaboratorThemeHelper.setupSystemBars(this);

        View btnBack = findViewById(R.id.collab_kpi_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnReturn = findViewById(R.id.collab_kpi_btn_return_dashboard);
        if (btnReturn != null) {
            btnReturn.setOnClickListener(v -> {
                Intent intent = new Intent(this, returnToAdmin
                        ? pe.net.ws.kpiweb.features.admin.dashboard.AdminDashboardActivity.class
                        : CollaboratorDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        initJoelKpiRepositories();
        loadAuthorizedForms(savedInstanceState);
        if (returnToAdmin) {
            View bottomNav = findViewById(R.id.collab_bottom_nav_root);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        } else CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.KPI);
    }

    private void showKpiStatus(String message, boolean retry) {
        FrameLayout host = findViewById(R.id.main);
        host.removeAllViews();
        TextView state = new TextView(this);
        state.setText(message + (retry ? "\n\nToca para reintentar." : ""));
        state.setGravity(Gravity.CENTER);
        state.setTextSize(16);
        state.setPadding(dp(24), dp(24), dp(24), dp(24));
        if (retry) state.setOnClickListener(v -> loadAuthorizedForms(null));
        host.addView(state, new FrameLayout.LayoutParams(-1, -1));
    }

    private void loadAuthorizedForms(@Nullable Bundle savedInstanceState) {
        SessionManager owner = new SessionManager(this);
        int user = owner.getUserId(), worker = owner.getWorkerId();
        String token = owner.getAccessToken();
        if (!owner.isLoggedIn() || user <= 0 || worker <= 0) {
            showKpiStatus("No hay un trabajador autenticado para consultar KPI.", false);
            return;
        }
        FrameLayout host = findViewById(R.id.main);
        host.removeAllViews();
        ProgressBar loading = new ProgressBar(this);
        host.addView(loading, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        pe.net.ws.kpiweb.core.network.ApiClient.getService().getAllowedKpiForms(owner.getBearerToken())
                .enqueue(new retrofit2.Callback<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>>() {
                    @Override public void onResponse(retrofit2.Call<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>> call,
                                                     retrofit2.Response<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        SessionManager current = new SessionManager(CollaboratorKpiIntegrationActivity.this);
                        if (isFinishing() || isDestroyed() || !current.isLoggedIn() || current.getUserId()!=user
                                || current.getWorkerId()!=worker || !token.equals(current.getAccessToken())) return;
                        if (response.code()==401) {
                            current.clearActiveSession();
                            pe.net.ws.kpiweb.navigation.AccountNavigation.login(CollaboratorKpiIntegrationActivity.this, false);
                            finish();
                            return;
                        }
                        if (response.code()==403) { showKpiStatus("Esta cuenta no tiene permiso para acceder a KPI.", false); return; }
                        pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject> envelope=response.body();
                        com.google.gson.JsonObject data=envelope==null?null:envelope.getData();
                        if (!response.isSuccessful() || envelope==null || !envelope.isSuccessful() || data==null
                                || !data.has("id_trabajador") || data.get("id_trabajador").getAsInt()!=worker
                                || !data.has("tipos_formulario") || !data.get("tipos_formulario").isJsonArray()
                                || !data.has("calendario_laboral") || !data.get("calendario_laboral").isJsonObject()) {
                            showKpiStatus("El servidor KPI no está disponible o necesita la actualización de formularios.", true); return;
                        }
                        try {
                            if (!data.has("cargo_nombre") || !data.has("tipo_trabajador")) {
                                showKpiStatus("Asistencias no confirmó el cargo actual. Reintenta.", true); return;
                            }
                            current.updateVerifiedKpiLaborFields(worker,
                                    data.get("id_cargo").getAsInt(),
                                    data.get("cargo_nombre").getAsString(),
                                    data.get("tipo_trabajador").getAsString());
                            org.json.JSONObject calendar = new org.json.JSONObject(data.getAsJsonObject("calendario_laboral").toString());
                            if (!pe.net.ws.kpiweb.features.kpi.data.KpiAttendanceRepository.getInstance(CollaboratorKpiIntegrationActivity.this)
                                    .applyServerCalendar(calendar, worker)) {
                                showKpiStatus("No se pudo comprobar el calendario de Asistencias. Reintenta.", true); return;
                            }
                        } catch (org.json.JSONException invalid) {
                            showKpiStatus("El calendario de Asistencias no es válido. Reintenta.", true); return;
                        }
                        if (data.has("ausencia_aprobada") && data.get("ausencia_aprobada").isJsonObject()) {
                            com.google.gson.JsonObject leave = data.getAsJsonObject("ausencia_aprobada");
                            String start = leave.has("fecha_inicio") ? leave.get("fecha_inicio").getAsString() : "";
                            String end = leave.has("fecha_fin") ? leave.get("fecha_fin").getAsString() : "";
                            showKpiStatus("Asistencias registra vacaciones o permiso aprobado del " + start
                                    + " al " + end + ". Hoy no tienes una ficha KPI exigible.", false);
                            return;
                        }
                        java.util.ArrayList<String> types=new java.util.ArrayList<>();
                        for (com.google.gson.JsonElement value : data.getAsJsonArray("tipos_formulario")) {
                            if (value.isJsonPrimitive() && fragmentForFormType(value.getAsString())!=null) types.add(value.getAsString());
                        }
                        if (types.isEmpty()) {
                            showKpiStatus("Aún no hay un formulario KPI aprobado para tu cargo. Consulta con administración.", false);
                            return;
                        }
                        Runnable open = () -> {
                            if (types.size()==1) { approvedFormType=types.get(0); loadJoelKpiFragment(savedInstanceState); }
                            else new androidx.appcompat.app.AlertDialog.Builder(CollaboratorKpiIntegrationActivity.this)
                                    .setTitle("Formularios autorizados para tu cargo")
                                    .setItems(types.toArray(new String[0]), (dialog, index) -> {
                                        approvedFormType=types.get(index); loadJoelKpiFragment(null);
                                    }).setCancelable(false).show();
                        };
                        pe.net.ws.kpiweb.features.kpi.sync.KpiFormRecovery.refresh(CollaboratorKpiIntegrationActivity.this,
                                (success, restored) -> { if (!isFinishing() && !isDestroyed()) open.run(); });
                    }
                    @Override public void onFailure(retrofit2.Call<pe.net.ws.kpiweb.core.network.ApiResponse<com.google.gson.JsonObject>> call, Throwable error) {
                        if (!isFinishing() && !isDestroyed()) showKpiStatus("Sin conexión para comprobar los formularios autorizados.", true);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        pe.net.ws.kpiweb.features.kpi.sync.KpiFormSyncManager.retry(this);
        CollaboratorThemeHelper.setupSystemBars(this);
        if (!returnToAdmin) CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.KPI);
    }

    private void initJoelKpiRepositories() {
        try {
            KpiDesignerMemoryRepository.init(this);
            KpiJuniorMemoryRepository.init(this);
            KpiReminderScheduler.scheduleAllReminders(this);
        } catch (Exception ignored) {
        }
    }

    private void loadJoelKpiFragment(@Nullable Bundle savedInstanceState) {
        FrameLayout container = findViewById(R.id.main);
        ScrollView fallbackScroll = findViewById(R.id.collab_kpi_fallback_scroll);
        TextView tvHeaderTitle = findViewById(R.id.collab_kpi_tv_header_title);

        // 1. Obtener la identidad activa: priorizar sesión oficial de Jair
        SessionManager session = new SessionManager(this);
        String role;
        String userId;
        String name;
        String area;
        String workerType;

        if (session.isLoggedIn()) {
            userId = String.valueOf(session.getUserId());
            name = session.getFullName();
            if (name == null || name.trim().isEmpty()) name = session.getUsername();
            role = session.getCargoName();
            if (role == null || role.trim().isEmpty()) role = session.getRolName();
            if (role == null || role.trim().isEmpty()) role = "Colaborador";
            area = session.getAgenciaName() != null ? session.getAgenciaName() : "Operaciones";
            workerType = session.getWorkerTipo() != null ? session.getWorkerTipo() : "Planilla";
        } else {
            // Modo desarrollo: utilizar perfil activo de CollaboratorSessionManager
            CollaboratorProfile profile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
            if (profile != null) {
                userId = profile.getId();
                name = profile.getFullName();
                role = profile.getJobTitle();
                area = profile.getDepartment();
                workerType = "Planilla";
            } else {
                userId = "collab_01";
                name = "Colaborador";
                role = "Diseñador Gráfico";
                area = "Diseño";
                workerType = "Planilla";
            }
        }

        // Sincronizar la sesión en el gestor de Joel
        try {
            KpiCollaboratorsRepository.getInstance(this).registerActiveDatabaseUser(userId, name, role, area, workerType);
            KpiDefaultSessionManager.getInstance().setSession(userId, name, role, workerType, false);
        } catch (Exception ignored) {
        }

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("KPI");
            TextView subtitle = findViewById(R.id.collab_kpi_tv_header_sub);
            if (subtitle != null) subtitle.setText(role);
        }

        // 2. Obtener el Fragment oficial correspondiente mediante el router de Joel
        Fragment kpiFragment = fragmentForFormType(approvedFormType);

        // 3. Montar el Fragment en el contenedor oficial
        if (kpiFragment != null && container != null) {
            if (fallbackScroll != null) fallbackScroll.setVisibility(View.GONE);
            container.setVisibility(View.VISIBLE);

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, kpiFragment)
                        .commit();
            }
        } else {
            if (container != null) container.setVisibility(View.VISIBLE);
            if (fallbackScroll != null) fallbackScroll.setVisibility(View.GONE);
            showKpiStatus("El formulario autorizado aún no está disponible en esta versión de Android.", false);
        }
    }

    private Fragment fragmentForFormType(String type) {
        if (type == null) return null;
        switch (type) {
            case "VENTAS": return pe.net.ws.kpiweb.features.kpi.ui.KpiSalesContainerFragment.newInstance();
            case "DISENADOR": return pe.net.ws.kpiweb.features.kpi.designer.ui.KpiDesignerContainerFragment.newInstance();
            case "DISENADOR_JUNIOR": return pe.net.ws.kpiweb.features.kpi.junior.ui.KpiJuniorContainerFragment.newInstance();
            case "DISENADOR_SENIOR": return pe.net.ws.kpiweb.features.kpi.senior.ui.KpiSeniorContainerFragment.newInstance();
            case "GERENCIA": return pe.net.ws.kpiweb.features.kpi.manager.ui.KpiGeneralManagerContainerFragment.newInstance();
            case "PROGRAMACION": return pe.net.ws.kpiweb.features.kpi.ui.KpiFormFragment.newInstance();
            case "ADMINISTRACION": return pe.net.ws.kpiweb.features.kpi.administrator.ui.KpiAdministratorContainerFragment.newInstance();
            case "ASISTENCIA_ADMIN": return pe.net.ws.kpiweb.features.kpi.assistant.ui.KpiAdminAssistantContainerFragment.newInstance();
            case "CONTABILIDAD": return pe.net.ws.kpiweb.features.kpi.accountant.ui.KpiAccountantContainerFragment.newInstance();
            case "COMMUNITY": return pe.net.ws.kpiweb.features.kpi.community.ui.KpiCommunityContainerFragment.newInstance();
            case "FOTOGRAFIA": return pe.net.ws.kpiweb.features.kpi.photographer.ui.KpiPhotographerContainerFragment.newInstance();
            case "TRAFFICKER": return pe.net.ws.kpiweb.features.kpi.trafficker.ui.KpiTraffickerContainerFragment.newInstance();
            case "VIDEO": return pe.net.ws.kpiweb.features.kpi.videographer.ui.KpiVideographerContainerFragment.newInstance();
            default: return null;
        }
    }

    private void bindCollaboratorKpiSummary() {
        CollaboratorProfile profile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        if (profile == null) return;

        TextView tvName = findViewById(R.id.collab_kpi_tv_collab_name);
        TextView tvRole = findViewById(R.id.collab_kpi_tv_collab_role);
        TextView tvAssigned = findViewById(R.id.collab_kpi_tv_assigned);
        TextView tvCompleted = findViewById(R.id.collab_kpi_tv_completed);
        TextView tvPending = findViewById(R.id.collab_kpi_tv_pending);

        if (tvName != null) tvName.setText(profile.getFullName());
        if (tvRole != null) tvRole.setText(profile.getJobTitle() + " • " + profile.getDepartment());
        if (tvAssigned != null) tvAssigned.setText(String.valueOf(profile.getAssignedKpisCount()));
        if (tvCompleted != null) tvCompleted.setText(String.valueOf(profile.getCompletedKpisCount()));
        if (tvPending != null) tvPending.setText(String.valueOf(profile.getPendingKpisCount()));

        LinearLayout listContainer = findViewById(R.id.collab_kpi_container_list);
        if (listContainer == null) return;
        listContainer.removeAllViews();

        List<CollaboratorKpiItem> kpis = profile.getKpis();
        if (kpis == null || kpis.isEmpty()) return;

        int pad14 = dp(14);
        int margin10 = dp(10);

        for (int i = 0; i < kpis.size(); i++) {
            CollaboratorKpiItem item = kpis.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.collab_bg_card);
            card.setPadding(pad14, pad14, pad14, pad14);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                cardLp.topMargin = margin10;
            }
            card.setLayoutParams(cardLp);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(item.getName());
            tvTitle.setTextColor(getColor(R.color.collab_text_primary));
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvTitle.setTypeface(tvTitle.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvTitle.setLayoutParams(titleLp);

            TextView tvBadge = new TextView(this);
            tvBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
            tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tvBadge.setTypeface(tvBadge.getTypeface(), android.graphics.Typeface.BOLD);

            switch (item.getStatus()) {
                case COMPLETADO:
                    tvBadge.setText("Completado");
                    tvBadge.setBackgroundResource(R.drawable.collab_bg_pill_green);
                    tvBadge.setTextColor(getColor(R.color.collab_status_completed));
                    break;
                case VENCIDO:
                    tvBadge.setText("Vencido");
                    tvBadge.setBackgroundResource(R.drawable.collab_bg_pill_red);
                    tvBadge.setTextColor(getColor(R.color.collab_status_overdue));
                    break;
                case PENDIENTE:
                    tvBadge.setText("Pendiente");
                    tvBadge.setBackgroundResource(R.drawable.collab_bg_pill_amber);
                    tvBadge.setTextColor(getColor(R.color.collab_status_pending));
                    break;
                case EN_CURSO:
                default:
                    tvBadge.setText("En curso");
                    tvBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
                    tvBadge.setTextColor(getColor(R.color.collab_purple_accent));
                    break;
            }

            topRow.addView(tvTitle);
            topRow.addView(tvBadge);
            card.addView(topRow);

            ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(item.getProgressPercentage());
            pb.setProgressDrawable(getDrawable(R.drawable.collab_bg_progress_horizontal));
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            pbLp.topMargin = dp(10);
            pb.setLayoutParams(pbLp);
            card.addView(pb);

            LinearLayout bottomRow = new LinearLayout(this);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams botLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            botLp.topMargin = dp(8);
            bottomRow.setLayoutParams(botLp);

            TextView tvProgress = new TextView(this);
            String progressText = formatValue(item.getCurrentValue()) + " / "
                    + formatValue(item.getTargetValue()) + " " + item.getUnit()
                    + " (" + item.getProgressPercentage() + "%)";
            tvProgress.setText(progressText);
            tvProgress.setTextColor(getColor(R.color.collab_text_secondary));
            tvProgress.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            bottomRow.addView(tvProgress, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDue = new TextView(this);
            tvDue.setText(item.getDueDate());
            tvDue.setTextColor(getColor(R.color.collab_text_muted));
            tvDue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            bottomRow.addView(tvDue);

            card.addView(bottomRow);
            listContainer.addView(card);
        }
    }

    private String formatValue(double val) {
        if (val == (long) val) {
            return String.valueOf((long) val);
        }
        return String.valueOf(val);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
