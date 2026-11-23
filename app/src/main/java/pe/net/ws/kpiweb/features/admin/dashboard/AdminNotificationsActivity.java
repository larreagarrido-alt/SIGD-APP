package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.AdminNoticeItem;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.AdminTarget;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.FilterTab;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminNotificationsUiHelper.VisualMeta;
import pe.net.ws.kpiweb.features.admin.personal.AdminPersonalActivity;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.EmptyCopy;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Centro de Notificaciones del Administrador — unificado visualmente con el Colaborador SIGD.
 * Mantiene los avisos personales del servidor estrictamente separados del resumen operativo
 * (formularios KPI recibidos y conteos del período).
 */
public final class AdminNotificationsActivity extends AppCompatActivity {

    private FilterTab selectedFilter = FilterTab.TODOS;
    private final List<AdminNoticeItem> allNotices = new ArrayList<>();

    private TextView tvUnreadBadge;
    private View headerBellDot;
    private View btnMarkAllRead;
    private TextView tvTotalCount;
    private TextView chipAll;
    private TextView chipUnread;
    private TextView chipImportant;

    private View loadingContainer;
    private View errorCard;
    private TextView tvErrorTitle;
    private TextView tvErrorDetail;
    private View btnRetryNotices;
    private RecyclerView recyclerView;
    private AdminNotificationAdapter adapter;
    private View emptyCard;
    private TextView tvEmptyTitle;
    private TextView tvEmptyDetail;
    private TextView tvEmptyStatusChip;
    private View infoSection;

    private TextView weekChip;
    private TextView periodLabel;
    private View summaryErrorCard;
    private TextView summaryErrorTitle;
    private TextView summaryErrorDetail;
    private View summaryRetryBtn;
    private View formsCard;
    private TextView formsCount;
    private LinearLayout summaryExtraContainer;

    private Call<ApiResponse<JsonObject>> summaryRequest;
    private Call<ApiResponse<JsonObject>> ownRequest;

    private boolean noticesLoading = false;
    private boolean noticesLoadedOnce = false;
    private String noticesErrorTitle = null;
    private String noticesErrorDetail = null;
    private boolean noticesErrorRetryable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_notifications);
        AdminBottomNavigation.attach(this, 3);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        bindViews();
        setupListeners();
        updateChipStyles();
        AdminTypography.apply(this, findViewById(android.R.id.content));
        renderNoticesSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAll();
    }

    @Override
    protected void onStop() {
        if (summaryRequest != null) summaryRequest.cancel();
        if (ownRequest != null) ownRequest.cancel();
        summaryRequest = null;
        ownRequest = null;
        super.onStop();
    }

    private void bindViews() {
        tvUnreadBadge = findViewById(R.id.admin_notifs_tv_unread_badge);
        headerBellDot = findViewById(R.id.admin_notifs_header_bell_dot);
        btnMarkAllRead = findViewById(R.id.admin_notifs_btn_mark_all_read);
        tvTotalCount = findViewById(R.id.admin_notifs_tv_total_count);
        chipAll = findViewById(R.id.admin_notif_chip_all);
        chipUnread = findViewById(R.id.admin_notif_chip_unread);
        chipImportant = findViewById(R.id.admin_notif_chip_important);

        loadingContainer = findViewById(R.id.admin_notifs_loading_container);
        errorCard = findViewById(R.id.admin_notifs_error_card);
        tvErrorTitle = findViewById(R.id.admin_notifs_error_title);
        tvErrorDetail = findViewById(R.id.admin_notifs_error_detail);
        btnRetryNotices = findViewById(R.id.admin_notifs_btn_retry);
        recyclerView = findViewById(R.id.admin_notifs_recycler_view);
        emptyCard = findViewById(R.id.admin_notifs_empty_card);
        tvEmptyTitle = findViewById(R.id.admin_notifs_empty_title);
        tvEmptyDetail = findViewById(R.id.admin_notifs_empty_detail);
        tvEmptyStatusChip = findViewById(R.id.admin_notifs_empty_status_chip);
        infoSection = findViewById(R.id.admin_notifs_info_section);

        weekChip = findViewById(R.id.admin_notifications_week_chip);
        periodLabel = findViewById(R.id.admin_notifications_period);
        summaryErrorCard = findViewById(R.id.admin_summary_error_card);
        summaryErrorTitle = findViewById(R.id.admin_summary_error_title);
        summaryErrorDetail = findViewById(R.id.admin_summary_error_detail);
        summaryRetryBtn = findViewById(R.id.admin_summary_btn_retry);
        formsCard = findViewById(R.id.admin_summary_forms_card);
        formsCount = findViewById(R.id.admin_summary_forms_count);
        summaryExtraContainer = findViewById(R.id.admin_notifications_summary_extra);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationAdapter(this, new AdminNotificationAdapter.Listener() {
            @Override
            public void onItemClick(AdminNoticeItem item) {
                showNoticeDetailDialog(item);
            }

            @Override
            public void onActionClick(AdminNoticeItem item, AdminTarget target) {
                if (!item.leida) setRemoteRead(item, true);
                navigateToAdminTarget(target);
            }

            @Override
            public void onToggleReadClick(AdminNoticeItem item) {
                setRemoteRead(item, !item.leida);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.admin_notifications_back).setOnClickListener(v -> finish());

        chipAll.setOnClickListener(v -> selectFilter(FilterTab.TODOS));
        chipUnread.setOnClickListener(v -> selectFilter(FilterTab.NO_LEIDOS));
        chipImportant.setOnClickListener(v -> selectFilter(FilterTab.IMPORTANTES));

        btnMarkAllRead.setOnClickListener(v -> markAllRemoteRead());
        btnRetryNotices.setOnClickListener(v -> loadOwnNotifications(true));
        summaryRetryBtn.setOnClickListener(v -> loadOperationalSummary());

        formsCard.setOnClickListener(v -> openPersonalReports());
    }

    private void selectFilter(FilterTab filter) {
        selectedFilter = filter;
        updateChipStyles();
        renderNoticesSection();
    }

    private void updateChipStyles() {
        styleChip(chipAll, selectedFilter == FilterTab.TODOS);
        styleChip(chipUnread, selectedFilter == FilterTab.NO_LEIDOS);
        styleChip(chipImportant, selectedFilter == FilterTab.IMPORTANTES);
    }

    private void styleChip(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundResource(selected
                ? R.drawable.collab_bg_notif_chip_selected
                : R.drawable.collab_bg_notif_chip_unselected);
        chip.setTextColor(selected
                ? Color.WHITE
                : getColor(R.color.collab_notif_text_secondary));
    }

    private void loadAll() {
        loadOwnNotifications(!noticesLoadedOnce);
        loadOperationalSummary();
    }

    private void loadOwnNotifications(boolean showSkeleton) {
        if (ownRequest != null) ownRequest.cancel();
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() <= 0 || session.getWorkerId() <= 0) {
            noticesLoading = false;
            noticesErrorTitle = "Inicia sesión";
            noticesErrorDetail = "Se necesita una sesión activa vinculada a tu cuenta para consultar tus avisos.";
            noticesErrorRetryable = false;
            allNotices.clear();
            renderNoticesSection();
            return;
        }

        final String expectedToken = session.getAccessToken();
        final int workerId = session.getWorkerId();

        if (showSkeleton) {
            noticesLoading = true;
            noticesErrorTitle = null;
            noticesErrorDetail = null;
            renderNoticesSection();
        }

        ownRequest = ApiClient.getService().getOwnNotifications("Bearer " + expectedToken);
        ownRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (!isCurrentOwn(call, expectedToken, workerId)) return;
                ownRequest = null;
                noticesLoading = false;

                if (response.code() == 401) {
                    noticesErrorTitle = "Sesión vencida";
                    noticesErrorDetail = "Vuelve a iniciar sesión para consultar tus notificaciones.";
                    noticesErrorRetryable = false;
                    renderNoticesSection();
                    return;
                }
                if (response.code() == 403) {
                    noticesErrorTitle = "Acceso restringido";
                    noticesErrorDetail = "Tu cuenta no tiene permiso para consultar estas notificaciones.";
                    noticesErrorRetryable = false;
                    renderNoticesSection();
                    return;
                }

                ApiResponse<JsonObject> envelope = response.body();
                JsonObject data = envelope == null ? null : envelope.getData();
                Integer returnedWorker = data == null ? null : count(data, "id_trabajador");
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || data == null || returnedWorker == null || returnedWorker != workerId
                        || !data.has("notificaciones") || !data.get("notificaciones").isJsonArray()) {
                    noticesErrorTitle = "No se pudieron cargar tus avisos";
                    noticesErrorDetail = "El servidor no devolvió las notificaciones de esta cuenta (HTTP "
                            + response.code() + ").";
                    noticesErrorRetryable = true;
                    renderNoticesSection();
                    return;
                }

                JsonArray rows = data.getAsJsonArray("notificaciones");
                allNotices.clear();
                allNotices.addAll(AdminNotificationsUiHelper.parseNotifications(rows));
                noticesLoadedOnce = true;
                noticesErrorTitle = null;
                noticesErrorDetail = null;
                noticesErrorRetryable = false;
                renderNoticesSection();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!isCurrentOwn(call, expectedToken, workerId)) return;
                ownRequest = null;
                noticesLoading = false;
                noticesErrorTitle = "Sin conexión";
                noticesErrorDetail = "No pudimos conectarnos al servidor para consultar tus notificaciones.";
                noticesErrorRetryable = true;
                renderNoticesSection();
            }
        });
    }

    private void loadOperationalSummary() {
        if (summaryRequest != null) summaryRequest.cancel();
        String week = AdminDashboardPeriod.fromSelectedDate(Calendar.getInstance());
        weekChip.setText("Semana " + week);
        periodLabel.setText("Consultando resumen del período…");
        summaryErrorCard.setVisibility(View.GONE);
        summaryExtraContainer.removeAllViews();

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getUserId() <= 0) {
            formsCard.setVisibility(View.GONE);
            showSummaryError("Sesión requerida", "Inicia sesión para ver el resumen operativo.", false);
            return;
        }

        final String expectedToken = session.getAccessToken();
        summaryRequest = ApiClient.getService().getDashboardResumen(session.getBearerToken(), week);
        summaryRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (!isCurrentSummary(call, expectedToken)) return;
                summaryRequest = null;

                if (response.code() == 403) {
                    formsCard.setVisibility(View.GONE);
                    showSummaryError("Acceso restringido",
                            "Tu cuenta no tiene el permiso dashboard.admin para consultar el resumen operativo.", false);
                    return;
                }
                if (response.code() == 401) {
                    formsCard.setVisibility(View.GONE);
                    showSummaryError("Sesión vencida", "Vuelve a iniciar sesión para ver el resumen.", false);
                    return;
                }

                ApiResponse<JsonObject> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                        || envelope.getData() == null) {
                    formsCard.setVisibility(View.GONE);
                    showSummaryError("Resumen no disponible",
                            "El servidor no devolvió el resumen operativo (HTTP " + response.code() + ").", true);
                    return;
                }

                renderOperationalSummary(envelope.getData(), week);
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!isCurrentSummary(call, expectedToken)) return;
                summaryRequest = null;
                formsCard.setVisibility(View.GONE);
                showSummaryError("Sin conexión", "No se pudo consultar el resumen operativo del servidor.", true);
            }
        });
    }

    private void renderOperationalSummary(JsonObject data, String week) {
        summaryExtraContainer.removeAllViews();
        if (!data.has("periodo_consultado") || data.get("periodo_consultado").isJsonNull()
                || !week.equals(data.get("periodo_consultado").getAsString())) {
            formsCard.setVisibility(View.GONE);
            showSummaryError("Período incorrecto", "El servidor devolvió datos de otra semana.", true);
            return;
        }

        JsonObject kpi = data.has("kpi") && data.get("kpi").isJsonObject()
                ? data.getAsJsonObject("kpi") : null;
        JsonObject activities = data.has("actividades") && data.get("actividades").isJsonObject()
                ? data.getAsJsonObject("actividades") : null;
        if (kpi == null || activities == null) {
            formsCard.setVisibility(View.GONE);
            showSummaryError("Resumen incompleto", "Faltan conteos del servidor para esta semana.", true);
            return;
        }

        Integer overdue = count(kpi, "kpis_vencidos");
        Integer pending = count(kpi, "kpis_pendientes");
        Integer review = count(activities, "actividades_pendientes_revision");
        Integer received = count(data, "formularios_recibidos_total");
        if (overdue == null || pending == null || review == null) {
            formsCard.setVisibility(View.GONE);
            showSummaryError("Resumen incompleto", "El servidor no informó todos los conteos.", true);
            return;
        }

        summaryErrorCard.setVisibility(View.GONE);
        String estadoPeriodo = (overdue > 0 || pending > 0 || review > 0)
                ? "Con seguimiento activo"
                : "Período al día";
        periodLabel.setText(estadoPeriodo + " · fuente: WEBSITE");

        if (received != null) {
            formsCard.setVisibility(View.VISIBLE);
            formsCount.setText(String.valueOf(received));
        } else {
            formsCard.setVisibility(View.GONE);
        }

        if (overdue > 0) {
            addOperationalRow("KPI vencidos", "Período " + week, overdue,
                    R.drawable.collab_ic_notif_alarm_clock,
                    R.drawable.collab_bg_icon_badge_red,
                    R.color.collab_status_overdue);
        }
        if (pending > 0) {
            addOperationalRow("KPI pendientes", "Período " + week, pending,
                    R.drawable.collab_ic_notif_calendar_clock,
                    R.drawable.collab_bg_icon_badge_amber,
                    R.color.collab_prog_gold);
        }
        if (review > 0) {
            addOperationalRow("Actividades por revisar", "Total actual del servidor, sin filtro semanal", review,
                    R.drawable.collab_ic_notif_clipboard_check,
                    R.drawable.collab_bg_icon_badge_purple,
                    R.color.collab_notif_violet_soft);
        }
    }

    private void addOperationalRow(String title, String detail, int value,
                                   @DrawableRes int iconRes,
                                   @DrawableRes int iconBgRes,
                                   int tintColorRes) {
        View row = getLayoutInflater().inflate(R.layout.admin_item_notification_summary, summaryExtraContainer, false);
        row.setBackgroundResource(R.drawable.collab_bg_notif_card_read);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        row.setLayoutParams(lp);

        ImageView icon = row.findViewById(R.id.admin_notification_icon);
        if (icon != null) {
            icon.setImageResource(iconRes);
            icon.setColorFilter(ContextCompat.getColor(this, tintColorRes));
            if (icon.getParent() instanceof FrameLayout) {
                ((View) icon.getParent()).setBackgroundResource(iconBgRes);
            }
        }
        TextView tvTitle = row.findViewById(R.id.admin_notification_title);
        TextView tvDetail = row.findViewById(R.id.admin_notification_detail);
        TextView tvCount = row.findViewById(R.id.admin_notification_count);
        if (tvTitle != null) {
            tvTitle.setText(title);
            tvTitle.setTextColor(getColor(R.color.collab_text_primary));
        }
        if (tvDetail != null) {
            tvDetail.setText(detail);
            tvDetail.setTextColor(getColor(R.color.collab_notif_text_secondary));
        }
        if (tvCount != null) {
            tvCount.setText(String.valueOf(value));
            tvCount.setTextColor(ContextCompat.getColor(this, tintColorRes));
        }

        AdminTypography.apply(this, row);
        summaryExtraContainer.addView(row);
    }

    private void showSummaryError(String title, String detail, boolean canRetry) {
        periodLabel.setText("Resumen no disponible por ahora");
        summaryErrorCard.setVisibility(View.VISIBLE);
        summaryErrorTitle.setText(title);
        summaryErrorDetail.setText(detail);
        summaryRetryBtn.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private void renderNoticesSection() {
        int unreadCount = AdminNotificationsUiHelper.countUnread(allNotices);
        List<AdminNoticeItem> visible = AdminNotificationsUiHelper.filterNotices(allNotices, selectedFilter);

        updateHeaderStatus(unreadCount, noticesLoading);

        if (noticesLoading) {
            tvTotalCount.setText("Consultando…");
            setInboxVisibility(true, false, false, false, false);
            return;
        }

        if (noticesErrorTitle != null) {
            tvTotalCount.setText(AdminNotificationsUiHelper.formatSectionCount(0));
            tvErrorTitle.setText(noticesErrorTitle);
            tvErrorDetail.setText(noticesErrorDetail);
            btnRetryNotices.setVisibility(noticesErrorRetryable ? View.VISIBLE : View.GONE);
            setInboxVisibility(false, true, false, false, false);
            return;
        }

        tvTotalCount.setText(AdminNotificationsUiHelper.formatSectionCount(visible.size()));
        adapter.setItems(visible);

        if (visible.isEmpty()) {
            EmptyCopy copy = AdminNotificationsUiHelper.resolveEmptyCopy(selectedFilter, allNotices.size());
            tvEmptyTitle.setText(copy.title);
            tvEmptyDetail.setText(copy.description);
            tvEmptyStatusChip.setText(copy.statusChip);
            if (copy.positiveTone) {
                tvEmptyStatusChip.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
                tvEmptyStatusChip.setTextColor(getColor(R.color.collab_notif_positive));
            } else {
                tvEmptyStatusChip.setBackgroundResource(R.drawable.collab_bg_notif_chip_unselected);
                tvEmptyStatusChip.setTextColor(getColor(R.color.collab_notif_text_secondary));
            }
            boolean showInfo = allNotices.isEmpty() && selectedFilter == FilterTab.TODOS;
            setInboxVisibility(false, false, false, true, showInfo);
        } else {
            setInboxVisibility(false, false, true, false, false);
        }
    }

    private void updateHeaderStatus(int unreadCount, boolean loading) {
        btnMarkAllRead.setVisibility(!loading && unreadCount > 0 ? View.VISIBLE : View.GONE);
        headerBellDot.setVisibility(!loading && unreadCount > 0 ? View.VISIBLE : View.GONE);

        if (loading) {
            tvUnreadBadge.setText("Actualizando…");
            tvUnreadBadge.setBackgroundResource(R.drawable.collab_bg_notif_chip_unselected);
            tvUnreadBadge.setTextColor(getColor(R.color.collab_notif_text_secondary));
        } else if (unreadCount > 0) {
            tvUnreadBadge.setText(AdminNotificationsUiHelper.formatHeaderPendingStatus(unreadCount));
            tvUnreadBadge.setBackgroundResource(R.drawable.collab_bg_notif_chip_selected);
            tvUnreadBadge.setTextColor(Color.WHITE);
        } else {
            tvUnreadBadge.setText(AdminNotificationsUiHelper.formatHeaderPendingStatus(0));
            tvUnreadBadge.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
            tvUnreadBadge.setTextColor(getColor(R.color.collab_notif_positive));
        }
    }

    private void setInboxVisibility(boolean loading, boolean error, boolean list, boolean empty, boolean info) {
        loadingContainer.setVisibility(loading ? View.VISIBLE : View.GONE);
        errorCard.setVisibility(error ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(list ? View.VISIBLE : View.GONE);
        emptyCard.setVisibility(empty ? View.VISIBLE : View.GONE);
        infoSection.setVisibility(info ? View.VISIBLE : View.GONE);
    }

    private void showNoticeDetailDialog(AdminNoticeItem item) {
        if (!item.leida) setRemoteRead(item, true);
        VisualMeta meta = AdminNotificationsUiHelper.resolveVisualMeta(item.tipo);
        String body = item.mensaje;
        if (!item.creadoEn.isEmpty()) {
            body = body.isEmpty() ? item.creadoEn : body + "\n\n" + item.creadoEn;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(item.titulo.isEmpty() ? meta.badgeLabel : item.titulo)
                .setMessage(body)
                .setNegativeButton("Cerrar", null);
        if (meta.target != AdminTarget.NONE) {
            builder.setPositiveButton(meta.target.actionLabel, (d, which) -> navigateToAdminTarget(meta.target));
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> styleDialog(dialog));
        dialog.show();
    }

    private void styleDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.admin_bg_drawer);
        }
        TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
        if (titleView != null) {
            titleView.setTextColor(getColor(R.color.admin_text_primary));
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
        }
        TextView msgView = dialog.findViewById(android.R.id.message);
        if (msgView != null) {
            msgView.setTextColor(getColor(R.color.collab_notif_text_secondary));
        }
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.collab_notif_violet_soft));
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.collab_notif_text_secondary));
        }
    }

    private void setRemoteRead(AdminNoticeItem item, boolean read) {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) return;
        final String expectedToken = session.getAccessToken();
        ApiClient.getService().setNotificationRead(
                "Bearer " + expectedToken,
                item.id,
                Collections.<String, Object>singletonMap("leida", read)
        ).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (isFinishing() || isDestroyed()
                        || !expectedToken.equals(new SessionManager(AdminNotificationsActivity.this).getAccessToken())) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                    item.leida = read;
                    renderNoticesSection();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            "No se pudo guardar el estado de lectura en el servidor.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                Snackbar.make(findViewById(android.R.id.content),
                        "Sin conexión: no se actualizó la lectura.",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllRemoteRead() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || session.getWorkerId() <= 0) return;
        final String expectedToken = session.getAccessToken();
        ApiClient.getService().markAllNotificationsRead(
                "Bearer " + expectedToken,
                Collections.emptyMap()
        ).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (isFinishing() || isDestroyed()
                        || !expectedToken.equals(new SessionManager(AdminNotificationsActivity.this).getAccessToken())) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                    for (AdminNoticeItem item : allNotices) item.leida = true;
                    renderNoticesSection();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            "No se pudieron marcar los avisos como leídos.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (isFinishing() || isDestroyed()) return;
                Snackbar.make(findViewById(android.R.id.content),
                        "Sin conexión: siguen pendientes de lectura.",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToAdminTarget(AdminTarget target) {
        if (target == null) return;
        switch (target) {
            case PERSONAL_REPORTS:
                openPersonalReports();
                break;
            case KPI_OFFICIAL:
                Intent reports = new Intent(this, AdminKpiOfficialActivity.class);
                reports.putExtra(AdminKpiOfficialActivity.EXTRA_REPORTS_CENTER, true);
                startActivity(reports);
                break;
            case REWARDS:
                startActivity(new Intent(this, AdminRewardsActivity.class));
                break;
            case PERSONAL:
                startActivity(new Intent(this, AdminPersonalActivity.class));
                break;
            case NONE:
            default:
                break;
        }
    }

    private void openPersonalReports() {
        Intent reports = new Intent(this, AdminPersonalActivity.class);
        reports.putExtra(AdminPersonalActivity.EXTRA_OPEN_REPORTS, true);
        startActivity(reports);
    }

    private boolean isCurrentSummary(Call<ApiResponse<JsonObject>> call, String expectedToken) {
        return call == summaryRequest && !call.isCanceled() && !isFinishing() && !isDestroyed()
                && expectedToken.equals(new SessionManager(this).getAccessToken());
    }

    private boolean isCurrentOwn(Call<ApiResponse<JsonObject>> call, String expectedToken, int workerId) {
        SessionManager current = new SessionManager(this);
        return call == ownRequest && !call.isCanceled() && !isFinishing() && !isDestroyed()
                && current.isLoggedIn() && current.getWorkerId() == workerId
                && expectedToken.equals(current.getAccessToken());
    }

    private static Integer count(JsonObject data, String key) {
        try {
            if (!data.has(key) || data.get(key).isJsonNull()) return null;
            int value = data.get(key).getAsInt();
            return value >= 0 ? value : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
