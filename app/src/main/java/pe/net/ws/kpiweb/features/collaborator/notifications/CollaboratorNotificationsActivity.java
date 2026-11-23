package pe.net.ws.kpiweb.features.collaborator.notifications;

import android.Manifest;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.account.AccountActivity;
import pe.net.ws.kpiweb.features.account.model.UserRole;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorDataState;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.CollaboratorDashboardActivity;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import pe.net.ws.kpiweb.features.collaborator.kpi.CollaboratorKpiIntegrationActivity;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.NotificationType;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationItem.TargetModule;
import pe.net.ws.kpiweb.features.collaborator.notifications.CollaboratorNotificationsUiHelper.EmptyCopy;
import pe.net.ws.kpiweb.features.collaborator.ranking.CollaboratorRankingPointsActivity;
import pe.net.ws.kpiweb.features.collaborator.rewards.CollaboratorRewardsActivity;
import pe.net.ws.kpiweb.features.collaborator.wellness.CollaboratorWellnessActivity;
import pe.net.ws.kpiweb.features.settings.SettingsActivity;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Centro de Notificaciones Premium del Colaborador — SIGD by website.
 * Conserva exactamente los endpoints, permisos, acciones de lectura y navegación existentes,
 * diferenciando estados de carga, error, bandeja vacía y filtros reales.
 */
public class CollaboratorNotificationsActivity extends AppCompatActivity {

    private static final int REQ_POST_NOTIFICATIONS = 4102;

    private String selectedCategory = "TODAS";
    private CollaboratorProfile activeProfile;
    private CollaboratorNotificationRepository repository;

    private RecyclerView recyclerView;
    private CollaboratorNotificationAdapter adapter;
    private TextView tvBadge;
    private View headerBellDot;
    private TextView tvTotalCount;
    private View loadingContainer;
    private View errorCard;
    private TextView tvErrorTitle;
    private TextView tvErrorDetail;
    private View btnRetry;
    private View emptyCard;
    private TextView tvEmptyTitle;
    private TextView tvEmpty;
    private TextView tvEmptyStatusChip;
    private View infoSection;
    private View swipeHint;

    private Call<ApiResponse<JsonObject>> notificationsRequest;
    private boolean remoteLoading = false;
    private boolean remoteLoadedOnce = false;
    private String remoteErrorTitle = null;
    private String remoteErrorDetail = null;
    private boolean remoteErrorRetryable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collaborator_activity_notifications);

        CollaboratorThemeHelper.setupSystemBars(this);

        repository = CollaboratorNotificationRepository.getInstance(this);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();

        bindViews();
        setupHeaderAndFilters();
        setupLocalReminderAction();
        setupRecyclerViewAndSwipe();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.NOTIFICATIONS);
        CollaboratorThemeHelper.applyReadableTypeface(findViewById(android.R.id.content));
        refreshNotificationsUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CollaboratorThemeHelper.setupSystemBars(this);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        if (adapter != null) {
            adapter.setAllowLocalDelete(!isRealWorker());
        }
        updateReminderStatusLabel();
        if (isRealWorker()) {
            loadRemoteNotifications(!remoteLoadedOnce);
        } else {
            refreshNotificationsUI();
        }
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.NOTIFICATIONS);
    }

    @Override
    protected void onStop() {
        if (notificationsRequest != null) notificationsRequest.cancel();
        notificationsRequest = null;
        super.onStop();
    }

    private void bindViews() {
        tvBadge = findViewById(R.id.collab_notifs_tv_unread_badge);
        headerBellDot = findViewById(R.id.collab_notifs_header_bell_dot);
        tvTotalCount = findViewById(R.id.collab_notifs_tv_total_count);
        loadingContainer = findViewById(R.id.collab_notifs_loading_container);
        errorCard = findViewById(R.id.collab_notifs_error_card);
        tvErrorTitle = findViewById(R.id.collab_notifs_error_title);
        tvErrorDetail = findViewById(R.id.collab_notifs_error_detail);
        btnRetry = findViewById(R.id.collab_notifs_btn_retry);
        emptyCard = findViewById(R.id.collab_notifs_empty_card);
        tvEmptyTitle = findViewById(R.id.collab_notifs_empty_title);
        tvEmpty = findViewById(R.id.collab_notifs_tv_empty);
        tvEmptyStatusChip = findViewById(R.id.collab_notifs_empty_status_chip);
        infoSection = findViewById(R.id.collab_notifs_info_section);
        swipeHint = findViewById(R.id.collab_notifs_swipe_hint);

        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadRemoteNotifications(true));
        }
    }

    private void setupHeaderAndFilters() {
        View btnMarkAll = findViewById(R.id.collab_notifs_btn_mark_all_read);
        if (btnMarkAll != null) {
            btnMarkAll.setOnClickListener(v -> {
                if (activeProfile == null) return;
                int unread = repository.getUnreadCount(activeProfile.getId());
                if (unread == 0) {
                    CollaboratorBannerHelper.showBanner(
                            this,
                            "Bandeja al día",
                            "Todas tus notificaciones ya se encuentran marcadas como leídas.",
                            R.drawable.collaborator_ic_check_circle
                    );
                    return;
                }
                if (isRealWorker()) {
                    markAllRemote();
                    return;
                }
                repository.markAllAsRead(activeProfile.getId());
                refreshNotificationsUI();
                CollaboratorBottomNavHelper.updateUnreadBadge(this);
                CollaboratorBannerHelper.showBanner(
                        this,
                        "Notificaciones actualizadas",
                        "Se marcaron todas tus notificaciones como leídas.",
                        R.drawable.collaborator_ic_check_circle
                );
            });
        }

        bindFilterChip(R.id.collab_notif_chip_all, "TODAS");
        bindFilterChip(R.id.collab_notif_chip_unread, "NO_LEIDAS");
        bindFilterChip(R.id.collab_notif_chip_kpi, "IMPORTANTES");
        updateChipStyles();
    }

    private void bindFilterChip(int chipId, String category) {
        View chip = findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(v -> {
                selectedCategory = category;
                updateChipStyles();
                refreshNotificationsUI();
            });
        }
    }

    private void updateChipStyles() {
        styleSingleChip(R.id.collab_notif_chip_all, "TODAS".equals(selectedCategory));
        styleSingleChip(R.id.collab_notif_chip_unread, "NO_LEIDAS".equals(selectedCategory));
        styleSingleChip(R.id.collab_notif_chip_kpi, "IMPORTANTES".equals(selectedCategory));
    }

    private void styleSingleChip(int viewId, boolean selected) {
        TextView chip = findViewById(viewId);
        if (chip == null) return;
        chip.setBackgroundResource(selected
                ? R.drawable.collab_bg_notif_chip_selected
                : R.drawable.collab_bg_notif_chip_unselected);
        chip.setTextColor(selected
                ? Color.WHITE
                : getColor(R.color.collab_notif_text_secondary));
    }

    private void setupLocalReminderAction() {
        updateReminderStatusLabel();
        View testCard = findViewById(R.id.collab_notifs_test_card);
        View btnTestReminder = findViewById(R.id.collab_notifs_btn_test_reminder);
        if (isRealWorker()) {
            if (testCard != null) testCard.setVisibility(View.GONE);
            return;
        }
        if (testCard != null) testCard.setVisibility(View.VISIBLE);
        if (btnTestReminder != null) {
            btnTestReminder.setOnClickListener(v -> triggerTestLocalReminder());
        }
    }

    private void updateReminderStatusLabel() {
        TextView tvStatus = findViewById(R.id.collab_notifs_tv_reminder_status);
        if (tvStatus == null) return;
        boolean allowed = CollaboratorNotificationHelper.areKpiRemindersAllowedBySettings(this);
        if (allowed) {
            tvStatus.setText("Recordatorios de KPI activos en Configuración");
            tvStatus.setTextColor(getColor(R.color.collab_notif_positive));
        } else {
            tvStatus.setText("Recordatorios desactivados en Configuración");
            tvStatus.setTextColor(getColor(R.color.collab_status_pending));
        }
    }

    private void triggerTestLocalReminder() {
        if (activeProfile == null) return;

        if (!CollaboratorNotificationHelper.areKpiRemindersAllowedBySettings(this)) {
            CollaboratorBannerHelper.showPremiumModal(
                    this,
                    "CONFIGURACIÓN DE AVISOS",
                    "Recordatorios desactivados",
                    "Has desactivado las alertas o recordatorios de KPI en Configuración. ¿Deseas abrir Configuración para activarlos?",
                    "Ir a Configuración",
                    () -> {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        intent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
                        startActivity(intent);
                    },
                    "Cancelar"
            );
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !CollaboratorNotificationHelper.hasNotificationPermission(this)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS);
            return;
        }

        emitLocalReminderNow();
    }

    private void emitLocalReminderNow() {
        if (activeProfile == null) return;
        CollaboratorNotificationItem created = repository.addLocalTestReminder(
                activeProfile.getId(), activeProfile.getFirstName());

        boolean systemPosted = CollaboratorNotificationHelper.triggerSystemReminderNotification(
                this, created.getTitle(), created.getMessage());

        refreshNotificationsUI();
        CollaboratorBottomNavHelper.updateUnreadBadge(this);

        if (systemPosted) {
            CollaboratorBannerHelper.showBanner(
                    this,
                    "Recordatorio emitido con éxito",
                    "Se envió la alerta a la barra del sistema y a tu panel interno de WEBSITE.",
                    R.drawable.collab_ic_nav_bell
            );
        } else {
            CollaboratorBannerHelper.showBanner(
                    this,
                    "Aviso interno registrado",
                    "El recordatorio se añadió en el primer lugar de tu bandeja de notificaciones.",
                    R.drawable.collab_ic_nav_bell
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            emitLocalReminderNow();
        }
    }

    private void setupRecyclerViewAndSwipe() {
        recyclerView = findViewById(R.id.collab_notifs_recycler_view);
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CollaboratorNotificationAdapter(this, new CollaboratorNotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onItemClick(CollaboratorNotificationItem item) {
                showNotificationDetailDialog(item);
            }

            @Override
            public void onActionModuleClick(CollaboratorNotificationItem item) {
                if (isRealWorker()) setRemoteRead(item, true);
                else repository.markAsRead(item.getId());
                CollaboratorBottomNavHelper.updateUnreadBadge(CollaboratorNotificationsActivity.this);
                navigateToTargetModule(item.getTargetModule());
            }

            @Override
            public void onToggleReadState(CollaboratorNotificationItem item) {
                if (isRealWorker()) {
                    setRemoteRead(item, !item.isRead());
                    return;
                }
                repository.setReadState(item.getId(), !item.isRead());
                refreshNotificationsUI();
                CollaboratorBottomNavHelper.updateUnreadBadge(CollaboratorNotificationsActivity.this);
            }

            @Override
            public void onDeleteClick(CollaboratorNotificationItem item, int position) {
                handleDeleteNotification(item, position);
            }
        });
        adapter.setAllowLocalDelete(!isRealWorker());
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            private final ColorDrawable bgDelete = new ColorDrawable(Color.parseColor("#B71C1C"));
            private final Drawable iconDelete = ContextCompat.getDrawable(CollaboratorNotificationsActivity.this, R.drawable.collab_ic_delete);

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return isRealWorker() ? 0 : super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position >= 0 && position < adapter.getItemCount()) {
                    CollaboratorNotificationItem item = adapter.getItem(position);
                    handleDeleteNotification(item, position);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                int iconMargin = (int) (16 * getResources().getDisplayMetrics().density);
                int intrinsicWidth = iconDelete != null ? iconDelete.getIntrinsicWidth() : 0;
                int intrinsicHeight = iconDelete != null ? iconDelete.getIntrinsicHeight() : 0;
                int iconTop = itemView.getTop() + (itemView.getHeight() - intrinsicHeight) / 2;
                int iconBottom = iconTop + intrinsicHeight;

                if (dX > 0) {
                    bgDelete.setBounds(itemView.getLeft(), itemView.getTop(), (int) (itemView.getLeft() + dX), itemView.getBottom());
                    bgDelete.draw(c);
                    if (iconDelete != null && dX > iconMargin + intrinsicWidth) {
                        int iconLeft = itemView.getLeft() + iconMargin;
                        iconDelete.setBounds(iconLeft, iconTop, iconLeft + intrinsicWidth, iconBottom);
                        iconDelete.draw(c);
                    }
                } else if (dX < 0) {
                    bgDelete.setBounds((int) (itemView.getRight() + dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    bgDelete.draw(c);
                    if (iconDelete != null && Math.abs(dX) > iconMargin + intrinsicWidth) {
                        int iconRight = itemView.getRight() - iconMargin;
                        iconDelete.setBounds(iconRight - intrinsicWidth, iconTop, iconRight, iconBottom);
                        iconDelete.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    private void handleDeleteNotification(CollaboratorNotificationItem item, int position) {
        if (item == null || adapter == null) return;
        if (isRealWorker()) {
            adapter.notifyItemChanged(position);
            Snackbar.make(findViewById(android.R.id.content),
                    "Las notificaciones del servidor no se eliminan desde esta pantalla.",
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        adapter.removeItem(position);
        repository.discardNotificationLocally(item.getId());
        refreshNotificationsUI();

        View rootLayout = findViewById(R.id.collab_notifs_root);
        if (rootLayout == null) rootLayout = findViewById(android.R.id.content);

        Snackbar.make(rootLayout, "Notificación eliminada", Snackbar.LENGTH_LONG)
                .setAction("DESHACER", v -> {
                    repository.restoreNotificationLocally(item.getId());
                    refreshNotificationsUI();
                })
                .setActionTextColor(ContextCompat.getColor(this, R.color.collab_notif_violet_soft))
                .show();
    }

    private void refreshNotificationsUI() {
        if (activeProfile == null) return;
        boolean realWorker = isRealWorker();

        List<CollaboratorNotificationItem> allItems = repository.getNotificationsForCollaborator(
                activeProfile.getId(), "TODAS");
        List<CollaboratorNotificationItem> visibleItems = repository.getNotificationsForCollaborator(
                activeProfile.getId(), selectedCategory);
        int unreadCount = repository.getUnreadCount(activeProfile.getId());

        updateHeaderStatus(unreadCount, realWorker && remoteLoading);

        if (tvTotalCount != null) {
            if (realWorker && remoteLoading) {
                tvTotalCount.setText("Consultando…");
            } else {
                tvTotalCount.setText(CollaboratorNotificationsUiHelper.formatSectionCount(visibleItems.size()));
            }
        }

        // 1. Estado de carga (skeleton)
        if (realWorker && remoteLoading) {
            setContentVisibility(true, false, false, false, false);
            if (swipeHint != null) swipeHint.setVisibility(View.GONE);
            CollaboratorBottomNavHelper.updateUnreadBadge(this);
            return;
        }

        // 2. Estado de error (nunca mostrar estado vacío cuando falló el servidor)
        if (realWorker && remoteErrorTitle != null) {
            setContentVisibility(false, true, false, false, false);
            if (tvErrorTitle != null) tvErrorTitle.setText(remoteErrorTitle);
            if (tvErrorDetail != null) tvErrorDetail.setText(remoteErrorDetail);
            if (btnRetry != null) btnRetry.setVisibility(remoteErrorRetryable ? View.VISIBLE : View.GONE);
            if (swipeHint != null) swipeHint.setVisibility(View.GONE);
            CollaboratorBottomNavHelper.updateUnreadBadge(this);
            return;
        }

        if (adapter != null) {
            adapter.setItems(visibleItems);
        }

        // 3. Estado vacío diferenciado por filtro
        if (visibleItems.isEmpty()) {
            EmptyCopy copy = CollaboratorNotificationsUiHelper.resolveEmptyCopy(selectedCategory, allItems.size());
            if (tvEmptyTitle != null) tvEmptyTitle.setText(copy.title);
            if (tvEmpty != null) tvEmpty.setText(copy.description);
            if (tvEmptyStatusChip != null) {
                tvEmptyStatusChip.setText(copy.statusChip);
                if (copy.positiveTone) {
                    tvEmptyStatusChip.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
                    tvEmptyStatusChip.setTextColor(getColor(R.color.collab_notif_positive));
                } else {
                    tvEmptyStatusChip.setBackgroundResource(R.drawable.collab_bg_notif_chip_unselected);
                    tvEmptyStatusChip.setTextColor(getColor(R.color.collab_notif_text_secondary));
                }
            }
            boolean showInfo = allItems.isEmpty() && "TODAS".equals(selectedCategory);
            setContentVisibility(false, false, false, true, showInfo);
            if (swipeHint != null) swipeHint.setVisibility(View.GONE);
        } else {
            // 4. Lista de notificaciones reales
            setContentVisibility(false, false, true, false, false);
            if (swipeHint != null) {
                swipeHint.setVisibility(!realWorker ? View.VISIBLE : View.GONE);
            }
        }

        CollaboratorBottomNavHelper.updateUnreadBadge(this);
    }

    private void updateHeaderStatus(int unreadCount, boolean loading) {
        View markAllButton = findViewById(R.id.collab_notifs_btn_mark_all_read);
        if (markAllButton != null) {
            markAllButton.setVisibility(!loading && unreadCount > 0 ? View.VISIBLE : View.GONE);
        }
        if (headerBellDot != null) {
            headerBellDot.setVisibility(!loading && unreadCount > 0 ? View.VISIBLE : View.GONE);
        }
        if (tvBadge != null) {
            if (loading) {
                tvBadge.setText("Actualizando…");
                tvBadge.setBackgroundResource(R.drawable.collab_bg_notif_chip_unselected);
                tvBadge.setTextColor(getColor(R.color.collab_notif_text_secondary));
            } else if (unreadCount > 0) {
                tvBadge.setText(CollaboratorNotificationsUiHelper.formatHeaderPendingStatus(unreadCount));
                tvBadge.setBackgroundResource(R.drawable.collab_bg_notif_chip_selected);
                tvBadge.setTextColor(Color.WHITE);
            } else {
                tvBadge.setText(CollaboratorNotificationsUiHelper.formatHeaderPendingStatus(0));
                tvBadge.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
                tvBadge.setTextColor(getColor(R.color.collab_notif_positive));
            }
        }
    }

    private void setContentVisibility(boolean loading, boolean error, boolean list, boolean empty, boolean info) {
        if (loadingContainer != null) loadingContainer.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (errorCard != null) errorCard.setVisibility(error ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(list ? View.VISIBLE : View.GONE);
        if (emptyCard != null) emptyCard.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (infoSection != null) infoSection.setVisibility(info ? View.VISIBLE : View.GONE);
    }

    private boolean isRealWorker() {
        return CollaboratorDataState.isRealWorker(activeProfile);
    }

    private void loadRemoteNotifications(boolean showLoadingSkeleton) {
        if (!isRealWorker()) return;
        if (notificationsRequest != null) notificationsRequest.cancel();
        SessionManager session = new SessionManager(this);
        final int workerId = session.getWorkerId();
        final String token = session.getAccessToken();

        if (showLoadingSkeleton) {
            remoteLoading = true;
            remoteErrorTitle = null;
            remoteErrorDetail = null;
            refreshNotificationsUI();
        }

        notificationsRequest = ApiClient.getService().getOwnNotifications(session.getBearerToken());
        notificationsRequest.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (call != notificationsRequest || isFinishing() || isDestroyed()) return;
                notificationsRequest = null;
                remoteLoading = false;
                if (!token.equals(new SessionManager(CollaboratorNotificationsActivity.this).getAccessToken())) return;
                if (response.code() == 401) {
                    session.clearActiveSession();
                    AccountNavigation.login(CollaboratorNotificationsActivity.this, false);
                    return;
                }
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()
                        || response.body().getData() == null) {
                    if (response.code() == 403) {
                        remoteErrorTitle = "Acceso restringido";
                        remoteErrorDetail = "Tu cuenta no tiene permiso para consultar notificaciones.";
                        remoteErrorRetryable = false;
                    } else if (response.code() == 404) {
                        remoteErrorTitle = "Servicio no disponible";
                        remoteErrorDetail = "La consulta de notificaciones no está disponible en este momento.";
                        remoteErrorRetryable = true;
                    } else {
                        remoteErrorTitle = "No se pudieron cargar tus avisos";
                        remoteErrorDetail = "Ocurrió un problema al consultar el servidor. Inténtalo de nuevo.";
                        remoteErrorRetryable = true;
                    }
                    refreshNotificationsUI();
                    return;
                }
                JsonObject data = response.body().getData();
                if (!data.has("id_trabajador") || data.get("id_trabajador").getAsInt() != workerId
                        || !data.has("notificaciones") || !data.get("notificaciones").isJsonArray()) {
                    remoteErrorTitle = "Datos no disponibles";
                    remoteErrorDetail = "La respuesta de notificaciones no corresponde a tu cuenta.";
                    remoteErrorRetryable = true;
                    refreshNotificationsUI();
                    return;
                }
                List<CollaboratorNotificationItem> items = new ArrayList<>();
                JsonArray rows = data.getAsJsonArray("notificaciones");
                for (JsonElement row : rows) {
                    if (!row.isJsonObject()) continue;
                    CollaboratorNotificationItem item = remoteItem(row.getAsJsonObject(), workerId);
                    if (item != null) items.add(item);
                }
                repository.replaceRemoteNotifications(workerId, items);
                remoteLoadedOnce = true;
                remoteErrorTitle = null;
                remoteErrorDetail = null;
                remoteErrorRetryable = false;
                refreshNotificationsUI();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call != notificationsRequest || call.isCanceled() || isFinishing() || isDestroyed()) return;
                notificationsRequest = null;
                remoteLoading = false;
                remoteErrorTitle = "Sin conexión";
                remoteErrorDetail = "No pudimos conectarnos al servidor para consultar tus notificaciones.";
                remoteErrorRetryable = true;
                refreshNotificationsUI();
            }
        });
    }

    private static String string(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : "";
    }

    private static CollaboratorNotificationItem remoteItem(JsonObject row, int workerId) {
        try {
            long id = row.get("id_notificacion").getAsLong();
            if (id <= 0) return null;
            String kind = string(row, "tipo");
            NotificationType type;
            TargetModule target;
            switch (kind) {
                case "KPI_POR_VENCER":
                    type = NotificationType.KPI_PROXIMO;
                    target = TargetModule.MIS_KPI;
                    break;
                case "KPI_VENCIDO":
                    type = NotificationType.KPI_VENCIDO;
                    target = TargetModule.MIS_KPI;
                    break;
                case "KPI_REVISADO":
                    type = NotificationType.RECORDATORIO_EVALUACION;
                    target = TargetModule.MIS_KPI;
                    break;
                case "PREMIO_DISPONIBLE":
                    type = NotificationType.PREMIO_DESBLOQUEADO;
                    target = TargetModule.PREMIOS;
                    break;
                case "CANJE_ACTUALIZADO":
                    type = NotificationType.AVISO_GENERAL;
                    target = TargetModule.PREMIOS;
                    break;
                default:
                    type = NotificationType.AVISO_GENERAL;
                    target = TargetModule.DASHBOARD;
            }
            String date = string(row, "creado_en");
            long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(date).getTime();
            boolean read = row.has("leida") && row.get("leida").getAsInt() == 1;
            return new CollaboratorNotificationItem(
                    String.valueOf(id),
                    String.valueOf(workerId),
                    type,
                    string(row, "titulo"),
                    string(row, "mensaje"),
                    date,
                    timestamp,
                    target,
                    read
            );
        } catch (Exception invalid) {
            return null;
        }
    }

    private void setRemoteRead(CollaboratorNotificationItem item, boolean read) {
        long id;
        try {
            id = Long.parseLong(item.getId());
        } catch (NumberFormatException invalid) {
            return;
        }
        SessionManager session = new SessionManager(this);
        ApiClient.getService().setNotificationRead(
                session.getBearerToken(),
                id,
                Collections.singletonMap("leida", read)
        ).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.code() == 401) {
                    session.clearActiveSession();
                    AccountNavigation.login(CollaboratorNotificationsActivity.this, false);
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                    repository.setReadState(item.getId(), read);
                    refreshNotificationsUI();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            "No se pudo guardar la lectura en el servidor.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Sin conexión: la lectura no se guardó.",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllRemote() {
        SessionManager session = new SessionManager(this);
        ApiClient.getService().markAllNotificationsRead(
                session.getBearerToken(),
                Collections.emptyMap()
        ).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.code() == 401) {
                    session.clearActiveSession();
                    AccountNavigation.login(CollaboratorNotificationsActivity.this, false);
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                    repository.markAllAsRead(activeProfile.getId());
                    refreshNotificationsUI();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            "No se pudieron actualizar las notificaciones.",
                            Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                Snackbar.make(findViewById(android.R.id.content),
                        "Sin conexión: siguen pendientes de lectura.",
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showNotificationDetailDialog(CollaboratorNotificationItem item) {
        if (!item.isRead()) {
            if (isRealWorker()) setRemoteRead(item, true);
            else {
                repository.markAsRead(item.getId());
                refreshNotificationsUI();
                CollaboratorBottomNavHelper.updateUnreadBadge(this);
            }
        }

        CollaboratorBannerHelper.showPremiumModal(
                this,
                item.getType().getLabel() + " • " + item.getDateTimeDisplay(),
                item.getTitle(),
                item.getMessage(),
                item.getTargetModule().getActionLabel(),
                () -> navigateToTargetModule(item.getTargetModule()),
                "Cerrar"
        );
    }

    private void navigateToTargetModule(TargetModule target) {
        switch (target) {
            case MIS_KPI:
                CollaboratorKpiIntegrationActivity.openKpiSection(this);
                break;
            case PREMIOS:
                startActivity(new Intent(this, CollaboratorRewardsActivity.class));
                break;
            case RANKING_PUNTOS:
                startActivity(new Intent(this, CollaboratorRankingPointsActivity.class));
                break;
            case BIENESTAR:
                startActivity(new Intent(this, CollaboratorWellnessActivity.class));
                break;
            case CONFIGURACION:
                Intent sIntent = new Intent(this, SettingsActivity.class);
                sIntent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
                startActivity(sIntent);
                break;
            case MI_CUENTA:
                Intent aIntent = new Intent(this, AccountActivity.class);
                aIntent.putExtra(AccountActivity.EXTRA_USER_ROLE, UserRole.COLABORADOR.name());
                startActivity(aIntent);
                break;
            case DASHBOARD:
            default:
                Intent dIntent = new Intent(this, CollaboratorDashboardActivity.class);
                dIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(dIntent);
                break;
        }
    }
}
