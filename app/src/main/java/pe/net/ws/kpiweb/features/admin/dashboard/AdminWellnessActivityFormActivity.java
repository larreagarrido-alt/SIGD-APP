package pe.net.ws.kpiweb.features.admin.dashboard;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ExecutionStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.FormInput;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.MonthlyCategory;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.PublicationStatus;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.ValidationResult;
import pe.net.ws.kpiweb.features.admin.dashboard.AdminWellnessActivityHelper.VisibilityScope;
import pe.net.ws.kpiweb.features.admin.personal.data.PersonalRemoteMapper;
import pe.net.ws.kpiweb.features.admin.personal.model.EmployeeItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Formulario completo y moderno para crear o editar una Actividad de bienestar
 * (Centro de gestión -> Bienestar organizacional -> Evaluación mensual).
 * Conecta directamente con POST/PUT bienestar/admin/planes y directorio real de Personal.
 */
public final class AdminWellnessActivityFormActivity extends AppCompatActivity {

    public static final String EXTRA_PLAN_JSON = "extra_plan_json";
    public static final String EXTRA_AGENCIES_JSON = "extra_agencies_json";
    public static final String EXTRA_CAMPAIGNS_JSON = "extra_campaigns_json";
    public static final String EXTRA_RESULT_MESSAGE = "extra_result_message";

    private final List<Call<ApiResponse<JsonObject>>> activeCalls = new ArrayList<>();
    private final List<EmployeeItem> directory = new ArrayList<>();
    private final Set<Integer> selectedAgencyIds = new LinkedHashSet<>();
    private final Set<Long> selectedCampaignIds = new LinkedHashSet<>();
    private final Set<String> selectedCollaboratorIds = new LinkedHashSet<>();

    private JsonArray availableAgencies = new JsonArray();
    private JsonArray availableCampaigns = new JsonArray();

    private EditText inputName;
    private EditText inputObjective;
    private EditText inputNotes;
    private TextView tvCategory;
    private TextView tvResponsible;
    private TextView tvDate;
    private TextView tvStartTime;
    private TextView tvEndTime;
    private TextView errorBanner;
    private LinearLayout visibilityOptionsContainer;
    private LinearLayout audiencePickerBox;
    private TextView audienceSummaryTv;
    private TextView btnSelectAudience;
    private LinearLayout statusSelectorContainer;
    private TextView btnDraft;
    private TextView btnPublish;

    private long editingPlanId;
    private String selectedCategoryCode = "GENERAL";
    private String selectedResponsible = "";
    private String selectedDate = "";
    private String selectedStartTime = "";
    private String selectedEndTime = "";
    private ExecutionStatus selectedExecutionStatus = ExecutionStatus.PENDIENTE;
    private VisibilityScope selectedVisibility = VisibilityScope.SOLO_ADMIN;
    private boolean submitting;
    private boolean directoryLoaded;

    private String token;
    private int userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_wellness_activity_form);
        pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper.setupSystemBars(this);

        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        parseExtras();
        bindViews();
        bindEvents();
        renderVisibilityOptions();
        renderExecutionStatusSelector();
        updateAudienceSummaryUi();
        AdminTypography.apply(this, findViewById(android.R.id.content));
        loadDirectory(0);
    }

    private void parseExtras() {
        String agenciesJson = getIntent().getStringExtra(EXTRA_AGENCIES_JSON);
        if (agenciesJson != null && !agenciesJson.isEmpty()) {
            try {
                JsonElement el = JsonParser.parseString(agenciesJson);
                if (el.isJsonArray()) availableAgencies = el.getAsJsonArray();
            } catch (Exception ignored) {
            }
        }
        String campaignsJson = getIntent().getStringExtra(EXTRA_CAMPAIGNS_JSON);
        if (campaignsJson != null && !campaignsJson.isEmpty()) {
            try {
                JsonElement el = JsonParser.parseString(campaignsJson);
                if (el.isJsonArray()) availableCampaigns = el.getAsJsonArray();
            } catch (Exception ignored) {
            }
        }
    }

    private void bindViews() {
        inputName = findViewById(R.id.wellness_act_input_name);
        inputObjective = findViewById(R.id.wellness_act_input_objective);
        inputNotes = findViewById(R.id.wellness_act_input_notes);
        tvCategory = findViewById(R.id.wellness_act_tv_category);
        tvResponsible = findViewById(R.id.wellness_act_tv_responsible);
        tvDate = findViewById(R.id.wellness_act_tv_date);
        tvStartTime = findViewById(R.id.wellness_act_tv_start_time);
        tvEndTime = findViewById(R.id.wellness_act_tv_end_time);
        errorBanner = findViewById(R.id.wellness_act_error_banner);
        visibilityOptionsContainer = findViewById(R.id.wellness_act_visibility_options);
        audiencePickerBox = findViewById(R.id.wellness_act_audience_picker_box);
        audienceSummaryTv = findViewById(R.id.wellness_act_audience_summary);
        btnSelectAudience = findViewById(R.id.wellness_act_btn_select_audience);
        statusSelectorContainer = findViewById(R.id.wellness_act_status_selector);
        btnDraft = findViewById(R.id.wellness_act_btn_draft);
        btnPublish = findViewById(R.id.wellness_act_btn_publish);

        String planJson = getIntent().getStringExtra(EXTRA_PLAN_JSON);
        if (planJson != null && !planJson.isEmpty()) {
            try {
                JsonObject current = JsonParser.parseString(planJson).getAsJsonObject();
                editingPlanId = longValue(current, "id_plan");
                TextView headerTitle = findViewById(R.id.wellness_act_header_title);
                headerTitle.setText("Editar actividad de bienestar");
                inputName.setText(cleanString(current, "nombre"));
                inputObjective.setText(cleanString(current, "objetivo"));
                inputNotes.setText(cleanString(current, "observaciones"));

                String dim = cleanString(current, "dimension");
                if (!dim.isEmpty()) selectedCategoryCode = dim;
                tvCategory.setText(AdminWellnessActivityHelper.formatCategoryLabel(selectedCategoryCode));

                selectedResponsible = cleanString(current, "responsable");
                if (!selectedResponsible.isEmpty()) {
                    tvResponsible.setText(selectedResponsible);
                    tvResponsible.setTextColor(getColor(R.color.admin_text_primary));
                }

                selectedDate = cleanString(current, "fecha_programada");
                if (!selectedDate.isEmpty()) {
                    tvDate.setText(AdminWellnessActivityHelper.formatDateDisplay(selectedDate));
                    tvDate.setTextColor(getColor(R.color.admin_text_primary));
                }

                selectedStartTime = cleanString(current, "hora_inicio");
                if (!selectedStartTime.isEmpty()) {
                    tvStartTime.setText(selectedStartTime);
                    tvStartTime.setTextColor(getColor(R.color.admin_text_primary));
                }
                selectedEndTime = cleanString(current, "hora_fin");
                if (!selectedEndTime.isEmpty()) {
                    tvEndTime.setText(selectedEndTime);
                    tvEndTime.setTextColor(getColor(R.color.admin_text_primary));
                }

                selectedExecutionStatus = AdminWellnessActivityHelper.normalizeExecutionStatus(
                        cleanString(current, "estado"));
                selectedVisibility = AdminWellnessActivityHelper.normalizeVisibilityScope(
                        cleanString(current, "visibilidad"));
            } catch (Exception ignored) {
            }
        }
    }

    private void bindEvents() {
        findViewById(R.id.wellness_act_btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.wellness_act_pick_category).setOnClickListener(v -> chooseCategory());
        findViewById(R.id.wellness_act_pick_responsible).setOnClickListener(v -> chooseResponsible());
        findViewById(R.id.wellness_act_pick_date).setOnClickListener(v -> pickDate());
        findViewById(R.id.wellness_act_pick_start_time).setOnClickListener(v -> pickTime(true));
        findViewById(R.id.wellness_act_pick_end_time).setOnClickListener(v -> pickTime(false));
        btnSelectAudience.setOnClickListener(v -> openAudiencePicker());
        btnDraft.setOnClickListener(v -> submitActivity(false));
        btnPublish.setOnClickListener(v -> submitActivity(true));
    }

    private void chooseCategory() {
        List<MonthlyCategory> categories = AdminWellnessActivityHelper.monthlyCategories();
        String[] labels = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            labels[i] = categories.get(i).label;
        }
        new AlertDialog.Builder(this)
                .setTitle("Dimensión o área relacionada")
                .setItems(labels, (d, which) -> {
                    selectedCategoryCode = categories.get(which).code;
                    tvCategory.setText(categories.get(which).label);
                    hideError();
                })
                .show();
    }

    private void chooseResponsible() {
        if (!directoryLoaded && directory.isEmpty()) {
            Toast.makeText(this, "Cargando directorio de personal…", Toast.LENGTH_SHORT).show();
            loadDirectory(0);
            return;
        }
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(10), dp(16), dp(10));

        EditText search = new EditText(this);
        search.setHint("Buscar responsable por nombre o cargo…");
        container.addView(search);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(280));
        sLp.topMargin = dp(8);
        container.addView(scroll, sLp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Seleccionar responsable")
                .setView(container)
                .setNegativeButton("Cancelar", null)
                .create();

        Runnable render = () -> {
            list.removeAllViews();
            String q = search.getText().toString().trim();
            for (EmployeeItem emp : directory) {
                if (!AdminWellnessHelper.matchesCollaboratorQuery(emp.getName(), emp.getRole(), emp.getDepartment(), q)) {
                    continue;
                }
                TextView row = new TextView(this);
                row.setText(emp.getName() + "\n" + emp.getRole() + " · " + emp.getDepartment());
                row.setPadding(dp(10), dp(10), dp(10), dp(10));
                row.setTextSize(13f);
                row.setOnClickListener(v -> {
                    selectedResponsible = emp.getName();
                    tvResponsible.setText(selectedResponsible);
                    tvResponsible.setTextColor(getColor(R.color.admin_text_primary));
                    hideError();
                    dialog.dismiss();
                });
                list.addView(row);
            }
            if (list.getChildCount() == 0) {
                TextView empty = new TextView(this);
                empty.setText(directory.isEmpty()
                        ? "No hay personal disponible desde el servidor."
                        : "Sin coincidencias en el directorio.");
                empty.setPadding(dp(10), dp(12), dp(10), dp(12));
                list.addView(empty);
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        render.run();
        dialog.show();
    }

    private void pickDate() {
        Calendar cal = Calendar.getInstance();
        if (selectedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                cal.set(Calendar.YEAR, Integer.parseInt(selectedDate.substring(0, 4)));
                cal.set(Calendar.MONTH, Integer.parseInt(selectedDate.substring(5, 7)) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(selectedDate.substring(8, 10)));
            } catch (Exception ignored) {
            }
        }
        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            tvDate.setText(AdminWellnessActivityHelper.formatDateDisplay(selectedDate));
            tvDate.setTextColor(getColor(R.color.admin_text_primary));
            hideError();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        int hour = isStart ? 10 : 11;
        int min = 0;
        String current = isStart ? selectedStartTime : selectedEndTime;
        if (current.contains(":")) {
            try {
                String[] parts = current.split(":");
                hour = Integer.parseInt(parts[0]);
                min = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {
            }
        }
        new TimePickerDialog(this, (view, h, m) -> {
            String formatted = String.format(Locale.US, "%02d:%02d", h, m);
            if (isStart) {
                selectedStartTime = formatted;
                tvStartTime.setText(formatted);
                tvStartTime.setTextColor(getColor(R.color.admin_text_primary));
            } else {
                selectedEndTime = formatted;
                tvEndTime.setText(formatted);
                tvEndTime.setTextColor(getColor(R.color.admin_text_primary));
            }
            hideError();
        }, hour, min, true).show();
    }

    private void renderVisibilityOptions() {
        visibilityOptionsContainer.removeAllViews();
        VisibilityScope[] scopes = VisibilityScope.values();
        for (int i = 0; i < scopes.length; i++) {
            VisibilityScope scope = scopes[i];
            boolean selected = scope == selectedVisibility;
            TextView option = new TextView(this);
            option.setText(scope.displayName);
            option.setTextSize(12.5f);
            option.setTypeface(null, Typeface.BOLD);
            option.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
            option.setBackgroundResource(R.drawable.admin_bg_wellness_chip);
            option.setSelected(selected);
            option.setPadding(dp(12), dp(10), dp(12), dp(10));
            option.setOnClickListener(v -> {
                selectedVisibility = scope;
                renderVisibilityOptions();
                updateAudienceSummaryUi();
                hideError();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = dp(7);
            visibilityOptionsContainer.addView(option, lp);
        }
    }

    private void updateAudienceSummaryUi() {
        if (selectedVisibility == VisibilityScope.AGENCIA_CAMPANA) {
            audiencePickerBox.setVisibility(View.VISIBLE);
            String summary = AdminWellnessActivityHelper.formatAudienceSummary(
                    selectedVisibility, selectedAgencyIds.size(), selectedCampaignIds.size(), 0);
            audienceSummaryTv.setText("Alcance: " + summary);
            btnSelectAudience.setText("Seleccionar agencias o campañas");
        } else if (selectedVisibility == VisibilityScope.COLABORADORES) {
            audiencePickerBox.setVisibility(View.VISIBLE);
            String summary = AdminWellnessActivityHelper.formatAudienceSummary(
                    selectedVisibility, 0, 0, selectedCollaboratorIds.size());
            audienceSummaryTv.setText("Destinatarios: " + summary);
            btnSelectAudience.setText("Seleccionar colaboradores del directorio");
        } else {
            audiencePickerBox.setVisibility(View.GONE);
        }
    }

    private void openAudiencePicker() {
        if (selectedVisibility == VisibilityScope.AGENCIA_CAMPANA) {
            openAgenciesCampaignsPicker();
        } else if (selectedVisibility == VisibilityScope.COLABORADORES) {
            openCollaboratorsPicker();
        }
    }

    private void openAgenciesCampaignsPicker() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(16), dp(10));
        scroll.addView(box);

        TextView h1 = new TextView(this);
        h1.setText("Agencias reales");
        h1.setTypeface(null, Typeface.BOLD);
        box.addView(h1);

        if (availableAgencies.size() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No se recibieron agencias desde el servidor.");
            box.addView(empty);
        } else {
            for (JsonElement el : availableAgencies) {
                if (!el.isJsonObject()) continue;
                JsonObject row = el.getAsJsonObject();
                int id = (int) longValue(row, "id_agencia");
                String name = cleanString(row, "nombre");
                CheckBox cb = new CheckBox(this);
                cb.setText(name);
                cb.setChecked(selectedAgencyIds.contains(id));
                cb.setOnCheckedChangeListener((b, checked) -> {
                    if (checked) selectedAgencyIds.add(id);
                    else selectedAgencyIds.remove(id);
                });
                box.addView(cb);
            }
        }

        TextView h2 = new TextView(this);
        h2.setText("Campañas registradas");
        h2.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams h2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        h2Lp.topMargin = dp(12);
        box.addView(h2, h2Lp);

        if (availableCampaigns.size() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No hay campañas registradas.");
            box.addView(empty);
        } else {
            for (JsonElement el : availableCampaigns) {
                if (!el.isJsonObject()) continue;
                JsonObject row = el.getAsJsonObject();
                long id = longValue(row, "id_campana");
                String title = cleanString(row, "titulo");
                CheckBox cb = new CheckBox(this);
                cb.setText(title);
                cb.setChecked(selectedCampaignIds.contains(id));
                cb.setOnCheckedChangeListener((b, checked) -> {
                    if (checked) selectedCampaignIds.add(id);
                    else selectedCampaignIds.remove(id);
                });
                box.addView(cb);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Agencias y campañas destinatarias")
                .setView(scroll)
                .setPositiveButton("Aplicar", (d, w) -> updateAudienceSummaryUi())
                .show();
    }

    private void openCollaboratorsPicker() {
        if (!directoryLoaded && directory.isEmpty()) {
            Toast.makeText(this, "Cargando directorio de personal…", Toast.LENGTH_SHORT).show();
            loadDirectory(0);
            return;
        }
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(10), dp(16), dp(10));

        EditText search = new EditText(this);
        search.setHint("Buscar colaborador…");
        container.addView(search);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(280));
        sLp.topMargin = dp(8);
        container.addView(scroll, sLp);

        Runnable render = () -> {
            list.removeAllViews();
            String q = search.getText().toString().trim();
            for (EmployeeItem emp : directory) {
                if (!AdminWellnessHelper.matchesCollaboratorQuery(emp.getName(), emp.getRole(), emp.getDepartment(), q)) {
                    continue;
                }
                CheckBox cb = new CheckBox(this);
                cb.setText(emp.getName() + " (" + emp.getDepartment() + ")");
                cb.setChecked(selectedCollaboratorIds.contains(emp.getId()));
                cb.setOnCheckedChangeListener((b, checked) -> {
                    if (checked) selectedCollaboratorIds.add(emp.getId());
                    else selectedCollaboratorIds.remove(emp.getId());
                });
                list.addView(cb);
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        render.run();

        new AlertDialog.Builder(this)
                .setTitle("Colaboradores destinatarios")
                .setView(container)
                .setPositiveButton("Confirmar", (d, w) -> updateAudienceSummaryUi())
                .show();
    }

    private void renderExecutionStatusSelector() {
        statusSelectorContainer.removeAllViews();
        ExecutionStatus[] statuses = ExecutionStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            ExecutionStatus st = statuses[i];
            boolean selected = st == selectedExecutionStatus;
            TextView chip = new TextView(this);
            chip.setText(st.displayName);
            chip.setTextSize(12f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setGravity(Gravity.CENTER);
            chip.setBackgroundResource(R.drawable.admin_bg_wellness_chip);
            chip.setSelected(selected);
            chip.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
            chip.setPadding(dp(8), dp(10), dp(8), dp(10));
            chip.setOnClickListener(v -> {
                selectedExecutionStatus = st;
                renderExecutionStatusSelector();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(dp(8));
            statusSelectorContainer.addView(chip, lp);
        }
    }

    private void submitActivity(boolean publish) {
        if (submitting) return;
        FormInput formInput = new FormInput(
                inputName.getText().toString(),
                inputObjective.getText().toString(),
                selectedCategoryCode,
                selectedResponsible,
                selectedDate,
                selectedStartTime,
                selectedEndTime,
                selectedExecutionStatus,
                selectedVisibility,
                selectedAgencyIds.size(),
                selectedCampaignIds.size(),
                selectedCollaboratorIds.size()
        );
        ValidationResult validation = AdminWellnessActivityHelper.validate(formInput, publish);
        if (!validation.valid) {
            showError(validation.errorMessage);
            return;
        }
        hideError();
        setSubmitting(true);

        PublicationStatus pubStatus = publish ? PublicationStatus.PUBLICADA : PublicationStatus.BORRADOR;
        VisibilityScope effectiveVisibility = publish ? selectedVisibility : VisibilityScope.SOLO_ADMIN;
        String audienceSummary = AdminWellnessActivityHelper.formatAudienceSummary(
                effectiveVisibility,
                selectedAgencyIds.size(),
                selectedCampaignIds.size(),
                selectedCollaboratorIds.size()
        );

        JsonObject body = new JsonObject();
        body.addProperty("nombre", formInput.name);
        body.addProperty("objetivo", formInput.objective);
        body.addProperty("dimension", formInput.categoryCode);
        body.addProperty("responsable", formInput.responsible);
        body.addProperty("fecha_programada", formInput.scheduledDate);
        body.addProperty("estado", formInput.executionStatus.apiValue);
        body.addProperty("observaciones", inputNotes.getText().toString().trim());
        if (!formInput.startTime.isEmpty()) body.addProperty("hora_inicio", formInput.startTime);
        if (!formInput.endTime.isEmpty()) body.addProperty("hora_fin", formInput.endTime);
        body.addProperty("estado_publicacion", pubStatus.apiValue);
        body.addProperty("publicada", publish);
        body.addProperty("visibilidad", effectiveVisibility.apiValue);
        body.addProperty("alcance_resumen", audienceSummary);

        if (publish && selectedVisibility == VisibilityScope.AGENCIA_CAMPANA) {
            JsonArray agArr = new JsonArray();
            for (Integer id : selectedAgencyIds) agArr.add(id);
            JsonArray campArr = new JsonArray();
            for (Long id : selectedCampaignIds) campArr.add(id);
            body.add("agencias_ids", agArr);
            body.add("campanas_ids", campArr);
        } else if (publish && selectedVisibility == VisibilityScope.COLABORADORES) {
            JsonArray workers = new JsonArray();
            for (String id : selectedCollaboratorIds) {
                try {
                    workers.add(Integer.parseInt(id));
                } catch (NumberFormatException ignored) {
                    workers.add(id);
                }
            }
            body.add("trabajadores_ids", workers);
        }

        Call<ApiResponse<JsonObject>> call = editingPlanId <= 0
                ? ApiClient.getService().createAdminWellnessPlan(token, body)
                : ApiClient.getService().updateAdminWellnessPlan(token, editingPlanId, body);
        activeCalls.add(call);
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                activeCalls.remove(c);
                setSubmitting(false);
                if (!sameAccount() || isFinishing()) return;
                ApiResponse<JsonObject> resp = response.body();
                if (response.isSuccessful() && resp != null && resp.isSuccessful()) {
                    JsonObject savedData = resp.getData();
                    boolean serverConfirmedPublication = savedData != null
                            && ((savedData.has("publicada") && !savedData.get("publicada").isJsonNull()
                            && savedData.get("publicada").getAsBoolean())
                            || "PUBLICADA".equalsIgnoreCase(cleanString(savedData, "estado_publicacion")));

                    String resultMsg;
                    if (publish && !serverConfirmedPublication) {
                        resultMsg = "Actividad guardada en el servidor. Nota: la visibilidad hacia colaboradores requiere que el backend habilite publicación/destinatarios en bienestar/admin/planes y GET bienestar/actividades.";
                    } else if (publish) {
                        resultMsg = "Actividad publicada correctamente para los destinatarios autorizados.";
                    } else {
                        resultMsg = "Borrador guardado en el servidor (solo visible para administración).";
                    }
                    Intent out = new Intent();
                    out.putExtra(EXTRA_RESULT_MESSAGE, resultMsg);
                    setResult(RESULT_OK, out);
                    finish();
                } else {
                    String err = response.code() == 403
                            ? "Tu cuenta no tiene permiso para guardar actividades de bienestar."
                            : response.code() == 401
                            ? "Sesión vencida. Vuelve a iniciar sesión."
                            : "El servidor no aceptó la solicitud (HTTP " + response.code() + "). No se guardó ningún cambio.";
                    showError(err);
                }
                if (response.errorBody() != null) response.errorBody().close();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                activeCalls.remove(c);
                setSubmitting(false);
                if (sameAccount() && !isFinishing()) {
                    showError("Sin conexión con el servidor. No se guardó ni publicó la actividad.");
                }
            }
        });
    }

    private void loadDirectory(int offset) {
        Call<ApiResponse<JsonObject>> call = ApiClient.getService().getPersonal(token, 100, offset);
        activeCalls.add(call);
        call.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> c, Response<ApiResponse<JsonObject>> response) {
                activeCalls.remove(c);
                if (!sameAccount() || isFinishing()) return;
                ApiResponse<JsonObject> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                    directoryLoaded = true;
                    if (response.errorBody() != null) response.errorBody().close();
                    return;
                }
                JsonObject data = body.getData();
                JsonArray rows = data.has("empleados") && data.get("empleados").isJsonArray()
                        ? data.getAsJsonArray("empleados") : new JsonArray();
                for (JsonElement el : rows) {
                    if (el.isJsonObject()) {
                        EmployeeItem emp = PersonalRemoteMapper.employee(el.getAsJsonObject());
                        if (emp.getWorkStatus() != EmployeeItem.WorkStatus.INACTIVO) {
                            directory.add(emp);
                        }
                    }
                }
                JsonObject pag = data.has("paginacion") && data.get("paginacion").isJsonObject()
                        ? data.getAsJsonObject("paginacion") : null;
                boolean hasMore = pag != null && pag.has("hay_mas")
                        && pag.get("hay_mas").getAsBoolean() && rows.size() > 0 && offset < 2000;
                if (hasMore) {
                    loadDirectory(offset + rows.size());
                } else {
                    directoryLoaded = true;
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> c, Throwable error) {
                activeCalls.remove(c);
                directoryLoaded = true;
            }
        });
    }

    private void setSubmitting(boolean busy) {
        submitting = busy;
        btnDraft.setEnabled(!busy);
        btnPublish.setEnabled(!busy);
        btnDraft.setAlpha(busy ? 0.6f : 1f);
        btnPublish.setAlpha(busy ? 0.6f : 1f);
        btnPublish.setText(busy ? "Guardando…" : "Publicar actividad");
    }

    private void showError(String msg) {
        errorBanner.setText(msg);
        errorBanner.setVisibility(View.VISIBLE);
        ScrollView scroll = findViewById(R.id.wellness_act_scroll);
        if (scroll != null) scroll.smoothScrollTo(0, 0);
    }

    private void hideError() {
        errorBanner.setVisibility(View.GONE);
    }

    private boolean sameAccount() {
        SessionManager s = new SessionManager(this);
        return s.isLoggedIn() && s.getUserId() == userId && token.equals(s.getBearerToken());
    }

    @Override
    protected void onStop() {
        for (Call<ApiResponse<JsonObject>> c : activeCalls) c.cancel();
        activeCalls.clear();
        super.onStop();
    }

    private static String cleanString(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) return "";
        String val = o.get(key).getAsString().trim();
        return "—".equals(val) ? "" : val;
    }

    private static long longValue(JsonObject o, String key) {
        try {
            return o.get(key).getAsLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
