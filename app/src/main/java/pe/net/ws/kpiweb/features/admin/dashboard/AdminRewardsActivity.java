package pe.net.ws.kpiweb.features.admin.dashboard;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.collaborator.rewards.RewardImageLoader;
import pe.net.ws.kpiweb.features.collaborator.rewards.RewardImageViewer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Administrative publications, independent of KPI scoring or point redemption.
 */
public final class AdminRewardsActivity extends AppCompatActivity {
    private static final String[] FILTER_STATES = {
            "TODOS", "ACTIVO", "PROGRAMADO", "VENCIDO", "BORRADOR", "DESACTIVADO"
    };
    private static final int MAX_IMAGE_BYTES = 5242880;

    private final List<JsonObject> rewards = new ArrayList<>();
    private TextView totalCount, activeCount, scheduledCount, expiredCount, draftCount, listBadge;
    private ProgressBar summaryLoading;
    private ImageView refreshIcon, searchIcon, emptyIcon, emptyIllustration;
    private View searchContainer, searchClear, emptyCard, emptyIconBadge;
    private View statActiveCard, statScheduledCard, statExpiredCard, statDraftCard;
    private TextView emptyTitle, emptySubtitle, emptyAction;
    private LinearLayout filters;
    private EditText search;
    private RecyclerView recycler;
    private AdminRewardAdapter adapter;

    private String selectedState = "TODOS";
    private String token = "";
    private int userId;
    private boolean hasLoadedOnce = false;
    private String loadErrorMessage;
    private Call<ApiResponse<JsonObject>> pending;

    // Estado activo del formulario modal
    private Dialog activeFormDialog;
    private boolean formSubmitting = false;
    private Uri chosenImage;
    private String existingImageUrl = "";
    private String formStartValue = "";
    private String formEndValue = "";
    private ImageView formPreview;
    private View formPreviewRow, formRemoveImageBtn, formErrorBanner;
    private TextView formImageStatus, formImageMeta, formErrorText;

    private final ActivityResultLauncher<String> chooseImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null)
                    return;
                String mime = getContentResolver().getType(uri);
                if (mime == null
                        || !(mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp"))) {
                    showFormError("Formato no admitido. Selecciona una imagen JPG, PNG o WebP.");
                    return;
                }
                long size = measureUriBytes(uri);
                if (size <= 0 || size > MAX_IMAGE_BYTES) {
                    showFormError("La imagen supera el límite de 5 MB o no se pudo leer.");
                    return;
                }
                chosenImage = uri;
                hideFormError();
                bindFormImagePreview(uri, size);
            });

    @Override
    protected void onCreate(@Nullable Bundle state) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(state);
        setContentView(R.layout.admin_activity_rewards);
        AdminBottomNavigation.attach(this, 4);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        totalCount = findViewById(R.id.rewards_total_count);
        activeCount = findViewById(R.id.rewards_count_active);
        scheduledCount = findViewById(R.id.rewards_count_scheduled);
        expiredCount = findViewById(R.id.rewards_count_expired);
        draftCount = findViewById(R.id.rewards_count_draft);
        summaryLoading = findViewById(R.id.rewards_summary_loading);
        listBadge = findViewById(R.id.rewards_list_badge);

        refreshIcon = findViewById(R.id.rewards_refresh_icon);
        searchContainer = findViewById(R.id.rewards_search_container);
        searchIcon = findViewById(R.id.rewards_search_icon);
        search = findViewById(R.id.rewards_search);
        searchClear = findViewById(R.id.rewards_search_clear);
        filters = findViewById(R.id.rewards_filters);
        recycler = findViewById(R.id.rewards_recycler);

        emptyCard = findViewById(R.id.rewards_empty_card);
        emptyIllustration = findViewById(R.id.rewards_empty_illustration);
        emptyIconBadge = findViewById(R.id.rewards_empty_icon_badge);
        emptyIcon = findViewById(R.id.rewards_empty_icon);
        emptyTitle = findViewById(R.id.rewards_empty_title);
        emptySubtitle = findViewById(R.id.rewards_empty_subtitle);
        emptyAction = findViewById(R.id.rewards_empty_action);

        statActiveCard = findViewById(R.id.rewards_stat_active);
        statScheduledCard = findViewById(R.id.rewards_stat_scheduled);
        statExpiredCard = findViewById(R.id.rewards_stat_expired);
        statDraftCard = findViewById(R.id.rewards_stat_draft);

        adapter = new AdminRewardAdapter(this, new AdminRewardAdapter.Listener() {
            @Override
            public void onEdit(JsonObject item) {
                openForm(item);
            }

            @Override
            public void onToggleState(JsonObject item, String nextState) {
                changeState(item, nextState);
            }

            @Override
            public void onDelete(JsonObject item) {
                confirmDelete(item);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setItemAnimator(null);
        recycler.setAdapter(adapter);

        findViewById(R.id.rewards_back).setOnClickListener(v -> finish());
        findViewById(R.id.rewards_refresh).setOnClickListener(v -> {
            if (refreshIcon != null && !isReduceMotionEnabled()) {
                refreshIcon.animate().rotationBy(360f).setDuration(420L).start();
            }
            load();
        });
        View addBtn = findViewById(R.id.rewards_btn_add);
        attachPressFeedback(addBtn);
        addBtn.setOnClickListener(v -> openForm(null));

        statActiveCard.setOnClickListener(v -> toggleStatFilter("ACTIVO"));
        statScheduledCard.setOnClickListener(v -> toggleStatFilter("PROGRAMADO"));
        statExpiredCard.setOnClickListener(v -> toggleStatFilter("VENCIDO"));
        statDraftCard.setOnClickListener(v -> toggleStatFilter("BORRADOR"));

        View heroBanner = findViewById(R.id.rewards_hero_banner);
        if (heroBanner != null && !isReduceMotionEnabled()) {
            heroBanner.setAlpha(0f);
            heroBanner.setTranslationY(dp(8));
            heroBanner.animate().alpha(1f).translationY(0f).setDuration(200L).start();
        }

        setupSearch();
        renderFilters();
        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    @Override
    protected void onStop() {
        if (pending != null) {
            pending.cancel();
            pending = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (activeFormDialog != null && activeFormDialog.isShowing()) {
            activeFormDialog.dismiss();
        }
        super.onDestroy();
    }

    private void setupSearch() {
        searchClear.setOnClickListener(v -> search.setText(""));
        search.setOnFocusChangeListener((v, hasFocus) -> {
            searchContainer.setActivated(hasFocus);
            ImageViewCompat.setImageTintList(searchIcon, ColorStateList.valueOf(
                    getColor(hasFocus ? R.color.admin_purple_accent : R.color.admin_text_muted)));
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                render();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void toggleStatFilter(String state) {
        selectedState = state.equals(selectedState) ? "TODOS" : state;
        renderFilters();
        render();
    }

    private boolean sameAccount() {
        SessionManager session = new SessionManager(this);
        return !isFinishing() && !isDestroyed() && session.isLoggedIn()
                && userId == session.getUserId() && token.equals(session.getAccessToken());
    }

    private void load() {
        if (pending != null)
            pending.cancel();
        loadErrorMessage = null;
        summaryLoading.setVisibility(View.VISIBLE);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            summaryLoading.setVisibility(View.GONE);
            loadErrorMessage = "Inicia sesión para gestionar premios.";
            render();
            return;
        }
        token = session.getAccessToken();
        userId = session.getUserId();
        pending = ApiClient.getService().getAdminRewards(session.getBearerToken());
        pending.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null)
                    response.errorBody().close();
                if (call != pending || call.isCanceled() || !sameAccount())
                    return;
                pending = null;
                summaryLoading.setVisibility(View.GONE);
                hasLoadedOnce = true;

                ApiResponse<JsonObject> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    loadErrorMessage = response.code() == 403 ? "Tu cuenta no tiene permiso premios.administrar."
                            : response.code() == 401 ? "Sesión vencida. Vuelve a iniciar sesión."
                                    : "No se pudieron cargar las publicaciones.";
                    render();
                    return;
                }
                rewards.clear();
                JsonArray list = body.getData().getAsJsonArray("premios");
                if (list != null) {
                    for (JsonElement item : list) {
                        if (item.isJsonObject())
                            rewards.add(item.getAsJsonObject());
                    }
                }
                render();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call == pending && !call.isCanceled() && sameAccount()) {
                    pending = null;
                    summaryLoading.setVisibility(View.GONE);
                    hasLoadedOnce = true;
                    loadErrorMessage = "Sin conexión con el servidor.";
                    render();
                }
            }
        });
    }

    private void renderFilters() {
        filters.removeAllViews();
        for (String state : FILTER_STATES) {
            boolean selected = state.equals(selectedState);
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setBackgroundResource(selected
                    ? R.drawable.admin_bg_rewards_chip_active
                    : R.drawable.admin_bg_rewards_chip_inactive);
            chip.setMinimumHeight(dp(38));
            chip.setPadding(dp(12), dp(7), dp(14), dp(7));
            chip.setClickable(true);
            chip.setFocusable(true);

            ImageView icon = new ImageView(this);
            icon.setImageResource(filterIconRes(state));
            icon.setColorFilter(selected ? Color.WHITE : filterIconTint(state));
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(14), dp(14));
            iconLp.setMarginEnd(dp(6));
            chip.addView(icon, iconLp);

            TextView label = new TextView(this);
            label.setText(AdminRewardFormatter.formatFilterLabel(state));
            label.setSingleLine(true);
            label.setTextSize(12.5f);
            label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            label.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
            chip.addView(label);

            chip.setOnClickListener(v -> {
                if (selectedState.equals(state))
                    return;
                selectedState = state;
                renderFilters();
                render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            filters.addView(chip, lp);
        }
        syncStatCardsSelection();
        AdminTypography.apply(this, filters);
    }

    private void syncStatCardsSelection() {
        if (statActiveCard != null)
            statActiveCard.setSelected("ACTIVO".equals(selectedState));
        if (statScheduledCard != null)
            statScheduledCard.setSelected("PROGRAMADO".equals(selectedState));
        if (statExpiredCard != null)
            statExpiredCard.setSelected("VENCIDO".equals(selectedState));
        if (statDraftCard != null)
            statDraftCard.setSelected("BORRADOR".equals(selectedState));
    }

    private int filterIconRes(String state) {
        if ("ACTIVO".equals(state))
            return R.drawable.collaborator_ic_check_circle;
        if ("PROGRAMADO".equals(state))
            return R.drawable.admin_ic_calendar;
        if ("VENCIDO".equals(state))
            return R.drawable.collaborator_ic_clock;
        if ("BORRADOR".equals(state))
            return R.drawable.collaborator_ic_document;
        if ("DESACTIVADO".equals(state))
            return R.drawable.collab_ic_close;
        return R.drawable.admin_ic_rewards_layers;
    }

    private int filterIconTint(String state) {
        if ("ACTIVO".equals(state))
            return getColor(R.color.admin_status_delivered);
        if ("PROGRAMADO".equals(state))
            return getColor(R.color.admin_purple_accent);
        if ("VENCIDO".equals(state))
            return getColor(R.color.admin_status_pending);
        if ("BORRADOR".equals(state))
            return getColor(R.color.admin_cyan_brain);
        if ("DESACTIVADO".equals(state))
            return getColor(R.color.admin_status_overdue);
        return getColor(R.color.admin_text_secondary);
    }

    private void render() {
        int active = 0, scheduled = 0, expired = 0, draft = 0;
        for (JsonObject row : rewards) {
            String effective = value(row, "estado_efectivo");
            if ("ACTIVO".equals(effective))
                active++;
            else if ("PROGRAMADO".equals(effective))
                scheduled++;
            else if ("VENCIDO".equals(effective))
                expired++;
            else if ("BORRADOR".equals(effective))
                draft++;
        }
        totalCount.setText(AdminRewardFormatter.formatTotalBadge(rewards.size()));
        activeCount.setText(String.valueOf(active));
        scheduledCount.setText(String.valueOf(scheduled));
        expiredCount.setText(String.valueOf(expired));
        draftCount.setText(String.valueOf(draft));
        syncStatCardsSelection();

        String query = search.getText().toString().trim();
        List<JsonObject> filtered = new ArrayList<>();
        for (JsonObject row : rewards) {
            if (!"TODOS".equals(selectedState) && !selectedState.equals(value(row, "estado_efectivo")))
                continue;
            if (!AdminRewardFormatter.matchesSearch(row, query))
                continue;
            filtered.add(row);
        }

        boolean hasFilters = !"TODOS".equals(selectedState) || !query.isEmpty();
        listBadge.setText(AdminRewardFormatter.formatRewardsCount(filtered.size(), rewards.size(), hasFilters));

        if (loadErrorMessage != null && rewards.isEmpty()) {
            adapter.submitList(new ArrayList<>(), bearer());
            recycler.setVisibility(View.GONE);
            showEmptyState(false, R.drawable.admin_ic_alert_triangle,
                    "No se pudieron cargar los premios",
                    loadErrorMessage,
                    "Reintentar",
                    this::load);
            return;
        }

        if (rewards.isEmpty()) {
            adapter.submitList(new ArrayList<>(), bearer());
            recycler.setVisibility(View.GONE);
            if (hasLoadedOnce) {
                showEmptyState(true, 0,
                        "Aún no hay premios",
                        "Publica la primera recompensa para compartirla con tu equipo",
                        null,
                        null);
            } else {
                emptyCard.setVisibility(View.GONE);
            }
            return;
        }

        if (filtered.isEmpty()) {
            adapter.submitList(new ArrayList<>(), bearer());
            recycler.setVisibility(View.GONE);
            String desc = !query.isEmpty()
                    ? "No encontramos publicaciones que coincidan con \"" + query + "\"."
                    : "No hay publicaciones en el estado \"" + AdminRewardFormatter.formatFilterLabel(selectedState)
                            + "\".";
            showEmptyState(false, R.drawable.admin_ic_search,
                    "No encontramos premios con esos criterios",
                    desc,
                    "Limpiar filtros",
                    () -> {
                        selectedState = "TODOS";
                        search.setText("");
                        renderFilters();
                        render();
                    });
            return;
        }

        emptyCard.setVisibility(View.GONE);
        recycler.setVisibility(View.VISIBLE);
        adapter.submitList(filtered, bearer());
    }

    private void showEmptyState(boolean useGiftIllustration, int iconRes, String title, String subtitle,
            @Nullable String actionLabel, @Nullable Runnable action) {
        if (emptyIllustration != null && emptyIconBadge != null) {
            if (useGiftIllustration) {
                emptyIllustration.setVisibility(View.VISIBLE);
                emptyIconBadge.setVisibility(View.GONE);
            } else {
                emptyIllustration.setVisibility(View.GONE);
                emptyIconBadge.setVisibility(View.VISIBLE);
                if (iconRes != 0)
                    emptyIcon.setImageResource(iconRes);
            }
        }
        emptyTitle.setText(title);
        emptySubtitle.setText(subtitle);
        if (actionLabel != null && !actionLabel.trim().isEmpty() && action != null) {
            emptyAction.setText(actionLabel);
            emptyAction.setOnClickListener(v -> action.run());
            emptyAction.setVisibility(View.VISIBLE);
        } else {
            emptyAction.setText("");
            emptyAction.setOnClickListener(null);
            emptyAction.setVisibility(View.GONE);
        }
        emptyCard.setVisibility(View.VISIBLE);
    }

    private void attachPressFeedback(View view) {
        if (view == null || isReduceMotionEnabled())
            return;
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

    private void confirmDelete(JsonObject item) {
        int id = number(item, "id_premio");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Eliminar publicación")
                .setMessage("Se retirará del catálogo y se conservará su historial.")
                .setPositiveButton("Eliminar", (d, w) -> delete(id))
                .setNegativeButton("Cancelar", null)
                .create();
        styleAlertDialog(dialog, true);
        dialog.show();
    }

    private void openForm(@Nullable JsonObject item) {
        if (activeFormDialog != null && activeFormDialog.isShowing()) {
            activeFormDialog.dismiss();
        }
        chosenImage = null;
        formSubmitting = false;
        existingImageUrl = item != null ? value(item, "imagen_url") : "";

        final String initialTitle = value(item, "titulo");
        final String initialDesc = value(item, "descripcion");
        final String initialStart = value(item, "inicio_en");
        final String initialEnd = value(item, "fin_en");
        formStartValue = initialStart;
        formEndValue = initialEnd;

        View view = LayoutInflater.from(this).inflate(R.layout.admin_dialog_reward_form, null);
        TextView heading = view.findViewById(R.id.reward_form_heading);
        View closeBtn = view.findViewById(R.id.reward_form_close);
        NestedScrollView formScroll = view.findViewById(R.id.reward_form_scroll);
        formErrorBanner = view.findViewById(R.id.reward_form_error_banner);
        formErrorText = view.findViewById(R.id.reward_form_error_text);

        EditText titleInput = view.findViewById(R.id.reward_form_title);
        EditText descInput = view.findViewById(R.id.reward_form_description);

        View startPicker = view.findViewById(R.id.reward_form_start_picker);
        TextView startDateText = view.findViewById(R.id.reward_form_start_date);
        TextView startTimeText = view.findViewById(R.id.reward_form_start_time);
        View startClear = view.findViewById(R.id.reward_form_start_clear);

        View endPicker = view.findViewById(R.id.reward_form_end_picker);
        TextView endDateText = view.findViewById(R.id.reward_form_end_date);
        TextView endTimeText = view.findViewById(R.id.reward_form_end_time);
        View endClear = view.findViewById(R.id.reward_form_end_clear);

        View uploadZone = view.findViewById(R.id.reward_form_upload_zone);
        formPreviewRow = view.findViewById(R.id.reward_form_preview_row);
        formPreview = view.findViewById(R.id.reward_form_preview);
        formPreview.setOnClickListener(v -> RewardImageViewer.show(this,
                chosenImage == null ? existingImageUrl : "", bearer(), formPreview.getDrawable(), chosenImage));
        formImageStatus = view.findViewById(R.id.reward_form_image_status);
        formImageMeta = view.findViewById(R.id.reward_form_image_meta);
        formRemoveImageBtn = view.findViewById(R.id.reward_form_remove_image);

        View draftBtn = view.findViewById(R.id.reward_form_btn_draft);
        View publishBtn = view.findViewById(R.id.reward_form_btn_publish);
        ProgressBar publishProgress = view.findViewById(R.id.reward_form_publish_progress);
        ImageView publishIcon = view.findViewById(R.id.reward_form_publish_icon);
        TextView publishLabel = view.findViewById(R.id.reward_form_publish_label);
        View cancelBtn = view.findViewById(R.id.reward_form_btn_cancel);

        heading.setText(item == null ? "Nuevo premio" : "Editar premio");
        titleInput.setText(initialTitle);
        descInput.setText(initialDesc);

        Runnable syncDateLabels = () -> {
            updateDateCard(formStartValue, startDateText, startTimeText, startClear);
            updateDateCard(formEndValue, endDateText, endTimeText, endClear);
        };
        syncDateLabels.run();

        startPicker.setOnClickListener(v -> pickDateTime(formStartValue, selected -> {
            formStartValue = selected;
            hideFormError();
            syncDateLabels.run();
        }));
        startClear.setOnClickListener(v -> {
            formStartValue = "";
            syncDateLabels.run();
        });

        endPicker.setOnClickListener(v -> pickDateTime(formEndValue, selected -> {
            formEndValue = selected;
            hideFormError();
            syncDateLabels.run();
        }));
        endClear.setOnClickListener(v -> {
            formEndValue = "";
            syncDateLabels.run();
        });

        if (!existingImageUrl.isEmpty()) {
            formPreviewRow.setVisibility(View.VISIBLE);
            formImageStatus.setText("Imagen actual publicada");
            formImageMeta.setText("Toca arriba para reemplazarla");
            formRemoveImageBtn.setVisibility(View.GONE);
            RewardImageLoader.load(this, formPreview, null, existingImageUrl, bearer());
        } else {
            formPreviewRow.setVisibility(View.GONE);
        }

        uploadZone.setOnClickListener(v -> {
            if (!formSubmitting)
                chooseImage.launch("image/*");
        });
        formRemoveImageBtn.setOnClickListener(v -> {
            if (formSubmitting)
                return;
            chosenImage = null;
            if (!existingImageUrl.isEmpty()) {
                formPreviewRow.setVisibility(View.VISIBLE);
                formImageStatus.setText("Imagen actual publicada");
                formImageMeta.setText("Toca arriba para reemplazarla");
                formRemoveImageBtn.setVisibility(View.GONE);
                RewardImageLoader.load(this, formPreview, null, existingImageUrl, bearer());
            } else {
                formPreview.setImageDrawable(null);
                formPreviewRow.setVisibility(View.GONE);
            }
        });

        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getColor(R.color.admin_bg_main));
            window.setNavigationBarColor(getColor(R.color.admin_bg_drawer));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        Runnable attemptClose = () -> {
            if (formSubmitting)
                return;
            boolean dirty = !titleInput.getText().toString().trim().equals(initialTitle)
                    || !descInput.getText().toString().trim().equals(initialDesc)
                    || !formStartValue.equals(initialStart)
                    || !formEndValue.equals(initialEnd)
                    || chosenImage != null;
            if (!dirty) {
                dialog.dismiss();
                return;
            }
            AlertDialog discard = new AlertDialog.Builder(this)
                    .setTitle("Descartar cambios")
                    .setMessage("Tienes cambios sin guardar. ¿Deseas salir sin guardarlos?")
                    .setPositiveButton("Descartar", (d, w) -> dialog.dismiss())
                    .setNegativeButton("Seguir editando", null)
                    .create();
            styleAlertDialog(discard, true);
            discard.show();
        };

        dialog.setCancelable(false);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                attemptClose.run();
                return true;
            }
            return false;
        });

        closeBtn.setOnClickListener(v -> attemptClose.run());
        cancelBtn.setOnClickListener(v -> attemptClose.run());

        FormSubmitter submitter = state -> {
            if (formSubmitting)
                return;
            hideFormError();
            String t = titleInput.getText().toString().trim();
            String d = descInput.getText().toString().trim();
            String from = formStartValue.trim();
            String until = formEndValue.trim();

            if (t.length() < 3) {
                showFormError("El título debe tener al menos 3 caracteres.");
                titleInput.requestFocus();
                formScroll.smoothScrollTo(0, 0);
                return;
            }
            if (d.isEmpty()) {
                showFormError("Completa la descripción de la publicación.");
                descInput.requestFocus();
                formScroll.smoothScrollTo(0, 0);
                return;
            }
            boolean hasStart = !from.isEmpty();
            boolean hasEnd = !until.isEmpty();
            if (hasStart != hasEnd) {
                showFormError("Debes indicar tanto la fecha de inicio como la de vencimiento.");
                formScroll.smoothScrollTo(0, 0);
                return;
            }
            if (hasStart && (!AdminRewardFormatter.isValidBackendDateTime(from)
                    || !AdminRewardFormatter.isValidBackendDateTime(until))) {
                showFormError("Las fechas seleccionadas no tienen un formato válido.");
                formScroll.smoothScrollTo(0, 0);
                return;
            }
            if (hasStart && !AdminRewardFormatter.isEndAfterStart(from, until)) {
                showFormError("La fecha de vencimiento debe ser posterior a la fecha de inicio.");
                formScroll.smoothScrollTo(0, 0);
                return;
            }
            if ("PUBLICADO".equals(state)) {
                if (!hasStart) {
                    showFormError("Selecciona fecha y hora de inicio y vencimiento antes de publicar.");
                    formScroll.smoothScrollTo(0, 0);
                    return;
                }
                if (item == null && chosenImage == null) {
                    showFormError("Adjunta una imagen JPG, PNG o WebP antes de publicar.");
                    formScroll.smoothScrollTo(0, 0);
                    return;
                }
                if (item != null && existingImageUrl.isEmpty() && chosenImage == null) {
                    showFormError("Adjunta una imagen JPG, PNG o WebP antes de publicar.");
                    formScroll.smoothScrollTo(0, 0);
                    return;
                }
            }

            formSubmitting = true;
            draftBtn.setEnabled(false);
            draftBtn.setAlpha(0.5f);
            publishBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            cancelBtn.setAlpha(0.5f);
            publishProgress.setVisibility(View.VISIBLE);
            publishIcon.setVisibility(View.GONE);
            publishLabel.setText("PUBLICADO".equals(state) ? "Publicando…" : "Guardando…");

            submitForm(item, t, d, from, until, state, () -> {
                formSubmitting = false;
                if (dialog.isShowing())
                    dialog.dismiss();
                load();
            }, errorMessage -> {
                formSubmitting = false;
                draftBtn.setEnabled(true);
                draftBtn.setAlpha(1f);
                publishBtn.setEnabled(true);
                cancelBtn.setEnabled(true);
                cancelBtn.setAlpha(1f);
                publishProgress.setVisibility(View.GONE);
                publishIcon.setVisibility(View.VISIBLE);
                publishLabel.setText("Publicar");
                showFormError(errorMessage);
                formScroll.smoothScrollTo(0, 0);
            });
        };

        draftBtn.setOnClickListener(v -> submitter.run("BORRADOR"));
        publishBtn.setOnClickListener(v -> submitter.run("PUBLICADO"));

        AdminTypography.apply(this, view);
        activeFormDialog = dialog;
        dialog.show();
    }

    private interface FormSubmitter {
        void run(String targetState);
    }

    private interface DateTimePicked {
        void onPicked(String backendValue);
    }

    private interface ErrorCallback {
        void onError(String message);
    }

    private void updateDateCard(String value, TextView dateView, TextView timeView, View clearBtn) {
        if (value == null || value.trim().isEmpty()) {
            dateView.setText("Seleccionar fecha");
            dateView.setTextColor(getColor(R.color.admin_text_secondary));
            timeView.setText("Toca para elegir hora");
            clearBtn.setVisibility(View.GONE);
        } else {
            dateView.setText(AdminRewardFormatter.formatDisplayDate(value));
            dateView.setTextColor(getColor(R.color.admin_text_primary));
            String time = AdminRewardFormatter.formatDisplayTime(value);
            timeView.setText(time.isEmpty() ? "" : time + " h");
            clearBtn.setVisibility(View.VISIBLE);
        }
    }

    private void pickDateTime(String currentValue, DateTimePicked callback) {
        Calendar cal = Calendar.getInstance();
        int[] parts = AdminRewardFormatter.parseDateTimeParts(currentValue);
        int year = parts != null ? parts[0] : cal.get(Calendar.YEAR);
        int month = parts != null ? parts[1] - 1 : cal.get(Calendar.MONTH);
        int day = parts != null ? parts[2] : cal.get(Calendar.DAY_OF_MONTH);
        int hour = parts != null ? parts[3] : cal.get(Calendar.HOUR_OF_DAY);
        int minute = parts != null ? parts[4] : cal.get(Calendar.MINUTE);

        DatePickerDialog datePicker = new DatePickerDialog(this, R.style.Theme_KPIWeb_DatePicker,
                (dpView, selectedYear, selectedMonth, selectedDay) -> {
                    TimePickerDialog timePicker = new TimePickerDialog(this, R.style.Theme_KPIWeb_DatePicker,
                            (tpView, selectedHour, selectedMinute) -> callback.onPicked(
                                    AdminRewardFormatter.formatBackendDateTime(
                                            selectedYear, selectedMonth + 1, selectedDay, selectedHour,
                                            selectedMinute)),
                            hour, minute, true);
                    timePicker.show();
                }, year, month, day);
        datePicker.show();
    }

    private long measureUriBytes(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null)
                return -1;
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMAGE_BYTES)
                    return total;
            }
            return total;
        } catch (Exception e) {
            return -1;
        }
    }

    private void bindFormImagePreview(Uri uri, long sizeBytes) {
        if (formPreview == null || formPreviewRow == null)
            return;
        formPreview.setImageDrawable(null);
        RewardImageLoader.loadLocal(this, formPreview, uri, 800);
        formPreviewRow.setVisibility(View.VISIBLE);
        formImageStatus.setText("Imagen lista para subir");
        long kb = Math.max(1, sizeBytes / 1024);
        formImageMeta.setText(kb >= 1024
                ? String.format(java.util.Locale.getDefault(), "%.1f MB · Toca la imagen para ampliarla", kb / 1024f)
                : kb + " KB · Toca la imagen para ampliarla");
        formRemoveImageBtn.setVisibility(View.VISIBLE);
    }

    private void showFormError(String message) {
        if (formErrorBanner != null && formErrorText != null) {
            formErrorText.setText(message);
            formErrorBanner.setVisibility(View.VISIBLE);
        } else {
            toast(message);
        }
    }

    private void hideFormError() {
        if (formErrorBanner != null)
            formErrorBanner.setVisibility(View.GONE);
    }

    private void submitForm(@Nullable JsonObject item, String title, String description,
            String from, String until, String targetState,
            Runnable onComplete, ErrorCallback onError) {
        JsonObject body = new JsonObject();
        body.addProperty("titulo", title);
        body.addProperty("descripcion", description);
        body.addProperty("inicio_en", from);
        body.addProperty("fin_en", until);
        body.addProperty("estado_publicacion", targetState);

        int id = item == null ? 0 : number(item, "id_premio");
        Uri image = chosenImage;
        if (id == 0) {
            body.addProperty("estado_publicacion", "BORRADOR");
            perform(ApiClient.getService().createReward(bearer(), body), data -> {
                int created = number(data, "id_premio");
                if (created <= 0) {
                    onError.onError("No se recibió el identificador de la publicación.");
                    return;
                }
                afterSave(created, image, "PUBLICADO".equals(targetState) ? bodyWithState(body, targetState) : null,
                        onComplete, onError);
            }, onError);
        } else if (image != null) {
            afterSave(id, image, body, onComplete, onError);
        } else {
            perform(ApiClient.getService().updateReward(bearer(), id, body), data -> onComplete.run(), onError);
        }
    }

    private JsonObject bodyWithState(JsonObject body, String state) {
        JsonObject copy = body.deepCopy();
        copy.addProperty("estado_publicacion", state);
        return copy;
    }

    private void afterSave(int id, @Nullable Uri image, @Nullable JsonObject update,
            Runnable onComplete, ErrorCallback onError) {
        if (image == null) {
            if (update != null) {
                perform(ApiClient.getService().updateReward(bearer(), id, update), data -> onComplete.run(), onError);
            } else {
                onComplete.run();
            }
            return;
        }
        byte[] bytes;
        try (InputStream input = getContentResolver().openInputStream(image);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null)
                throw new IllegalArgumentException();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_IMAGE_BYTES)
                    throw new IllegalArgumentException();
                output.write(buffer, 0, count);
            }
            bytes = output.toByteArray();
        } catch (Exception error) {
            onError.onError("Imagen inválida o mayor de 5 MB. El borrador permanece guardado.");
            load();
            return;
        }
        String mime = getContentResolver().getType(image);
        if (mime == null || !(mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp"))) {
            onError.onError("Selecciona una imagen JPG, PNG o WebP.");
            load();
            return;
        }
        MultipartBody.Part part = MultipartBody.Part.createFormData(
                "imagen", "premio", RequestBody.create(MediaType.parse(mime), bytes));
        perform(ApiClient.getService().uploadRewardImage(bearer(), id, part), data -> {
            if (update != null) {
                perform(ApiClient.getService().updateReward(bearer(), id, update), saved -> onComplete.run(), onError);
            } else {
                onComplete.run();
            }
        }, onError);
    }

    private void changeState(JsonObject item, String state) {
        JsonObject body = new JsonObject();
        for (String field : new String[] { "titulo", "descripcion", "inicio_en", "fin_en" }) {
            body.addProperty(field, value(item, field));
        }
        body.addProperty("estado_publicacion", state);
        perform(ApiClient.getService().updateReward(bearer(), number(item, "id_premio"), body),
                data -> load(), this::toast);
    }

    private void delete(int id) {
        perform(ApiClient.getService().deleteReward(bearer(), id), data -> load(), this::toast);
    }

    private interface Done {
        void accept(JsonObject data);
    }

    private void perform(Call<ApiResponse<JsonObject>> call, Done done, ErrorCallback onError) {
        String expected = new SessionManager(this).getAccessToken();
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null)
                    response.errorBody().close();
                if (!expected.equals(new SessionManager(AdminRewardsActivity.this).getAccessToken()) || isFinishing())
                    return;
                ApiResponse<JsonObject> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful()) {
                    String msg = response.code() == 403 ? "No tienes permiso para modificar premios."
                            : response.code() == 401 ? "Sesión vencida. Vuelve a iniciar sesión."
                                    : "No se pudo guardar la publicación (HTTP " + response.code() + ").";
                    onError.onError(msg);
                    return;
                }
                done.accept(body.getData());
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                if (!isFinishing())
                    onError.onError("Sin conexión. Inténtalo nuevamente.");
            }
        });
    }

    private void styleAlertDialog(AlertDialog dialog, boolean destructive) {
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.color.admin_bg_drawer);
            }
            TextView titleView = dialog.findViewById(androidx.appcompat.R.id.alertTitle);
            if (titleView != null) {
                titleView.setTextColor(getColor(R.color.admin_text_primary));
                titleView.setTypeface(Typeface.DEFAULT_BOLD);
            }
            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextColor(getColor(R.color.admin_text_secondary));
            }
            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        getColor(destructive ? R.color.admin_danger_text : R.color.admin_purple_accent));
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.admin_text_secondary));
            }
        });
    }

    private String bearer() {
        return new SessionManager(this).getBearerToken();
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String value(JsonObject row, String key) {
        return AdminRewardFormatter.safeString(row, key);
    }

    private static int number(JsonObject row, String key) {
        return AdminRewardFormatter.safeInt(row, key);
    }
}
