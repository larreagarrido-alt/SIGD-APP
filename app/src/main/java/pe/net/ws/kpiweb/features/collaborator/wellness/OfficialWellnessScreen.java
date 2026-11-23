package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Official questionnaires are read from the authenticated API; demo answers are never submitted. */
final class OfficialWellnessScreen {
    private final AppCompatActivity activity;
    private final SessionManager session;
    private final LinearLayout content;
    private final LinearLayout dynamic;
    private final View overview;
    private final View evolution;
    private final View result;
    private final View step;
    private final View review;
    private final View stepFooter;
    private final String originalToken;
    private JsonObject monthlyTest;
    private JsonObject weeklyTest;
    private JsonArray historyRows = new JsonArray();
    private boolean submitting;
    private boolean formOpen;
    private JsonObject currentTest;
    private Long currentCampaignId;
    private JsonArray currentQuestions;
    private int[] selectedOptions;
    private int stepIndex;

    OfficialWellnessScreen(AppCompatActivity activity, SessionManager session) {
        this.activity = activity;
        this.session = session;
        originalToken = session.getBearerToken();
        // The real account uses Dani's original views, with server-owned data.
        activity.setContentView(R.layout.collaborator_activity_wellness);
        CollaboratorThemeHelper.setupSystemBars(activity);
        ScrollView scroll = activity.findViewById(R.id.collab_wellness_scroll);
        content = (LinearLayout) scroll.getChildAt(0);
        overview = activity.findViewById(R.id.collab_wellness_view_overview);
        evolution = activity.findViewById(R.id.collab_wellness_view_evolution);
        result = activity.findViewById(R.id.collab_wellness_view_result);
        step = activity.findViewById(R.id.collab_wellness_view_step);
        review = activity.findViewById(R.id.collab_wellness_view_review);
        stepFooter = activity.findViewById(R.id.collab_wellness_step_footer);
        dynamic = new LinearLayout(activity);
        dynamic.setOrientation(LinearLayout.VERTICAL);
        dynamic.setVisibility(View.GONE);
        content.addView(dynamic);
        activity.<TextView>findViewById(R.id.collab_wellness_privacy_notice).setText(
                "Tus respuestas se guardan en el servidor para tu cuenta. Solo tú puedes consultar este historial; no afecta tus KPI.");
        activity.findViewById(R.id.collab_wellness_btn_back_evaluations).setOnClickListener(v -> showOverview());
        activity.findViewById(R.id.collab_wellness_btn_hero_result).setOnClickListener(v -> openLatestMonthly());
        activity.findViewById(R.id.collab_wellness_btn_result_finish).setOnClickListener(v -> showOverview());
        activity.findViewById(R.id.collab_wellness_btn_open_ryff).setOnClickListener(v -> openRyffCampaigns());
        View hubSuggestions = activity.findViewById(R.id.collab_wellness_hub_suggestions);
        if (hubSuggestions != null) {
            hubSuggestions.setOnClickListener(v -> openMySuggestions());
        }
        View hubActivities = activity.findViewById(R.id.collab_wellness_hub_activities);
        if (hubActivities != null) {
            hubActivities.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, CollaboratorWellnessActivitiesActivity.class)));
        }
        View hubAssistant = activity.findViewById(R.id.collab_wellness_hub_assistant);
        if (hubAssistant != null) {
            hubAssistant.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, CollaboratorWellnessAssistantActivity.class)));
        }
        activity.findViewById(R.id.collab_wellness_ryff_chart_placeholder).setVisibility(View.GONE);
        activity.findViewById(R.id.collab_wellness_preferences_toggle).setVisibility(View.GONE);
        activity.findViewById(R.id.collab_wellness_btn_back).setOnClickListener(v -> goBack());
        View evoRetry = activity.findViewById(R.id.collab_wellness_evo_btn_retry);
        if (evoRetry != null) {
            evoRetry.setOnClickListener(v -> refreshHistory(true));
        }
        renderSupportResources();
        boolean returnToAdmin = activity.getIntent().getBooleanExtra(CollaboratorWellnessActivity.EXTRA_RETURN_ADMIN, false);
        if (returnToAdmin) {
            View bottomNav = activity.findViewById(R.id.collab_bottom_nav_root);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        } else {
            CollaboratorBottomNavHelper.setup(activity, CollaboratorBottomNavHelper.NavTab.WELLNESS);
        }
        CollaboratorThemeHelper.applyReadableTypeface(activity.findViewById(android.R.id.content));
    }

    void show() { showOverview(); load(); }

    private void page(View visible) {
        overview.setVisibility(visible == overview ? View.VISIBLE : View.GONE);
        evolution.setVisibility(visible == evolution ? View.VISIBLE : View.GONE);
        result.setVisibility(visible == result ? View.VISIBLE : View.GONE);
        step.setVisibility(visible == step ? View.VISIBLE : View.GONE);
        review.setVisibility(visible == review ? View.VISIBLE : View.GONE);
        stepFooter.setVisibility(visible == step ? View.VISIBLE : View.GONE);
        dynamic.setVisibility(visible == dynamic ? View.VISIBLE : View.GONE);
        ScrollView scroll = activity.findViewById(R.id.collab_wellness_scroll);
        scroll.scrollTo(0, 0);
    }

    private void showOverview() { page(overview); }

    void goBack() {
        if (formOpen && (dynamic.getVisibility() == View.VISIBLE ||
                step.getVisibility() == View.VISIBLE || review.getVisibility() == View.VISIBLE)) {
            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setMessage("¿Salir de la evaluación? Las respuestas sin enviar se perderán.")
                    .setNegativeButton("Continuar", null)
                    .setPositiveButton("Salir", (dialog, which) -> { formOpen = false; showOverview(); })
                    .show();
        } else if (result.getVisibility() == View.VISIBLE) history();
        else if (evolution.getVisibility() == View.VISIBLE || dynamic.getVisibility() == View.VISIBLE) showOverview();
        else activity.finish();
    }

    private boolean sameSession() { return originalToken.equals(session.getBearerToken()); }

    private TextView text(String value, int size) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
        view.setPadding(0, 8, 0, 12);
        dynamic.addView(view);
        return view;
    }

    private Button button(String value, Runnable action) {
        Button view = new Button(activity);
        view.setText(value);
        view.setAllCaps(false);
        view.setTextColor(Color.WHITE);
        view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(119, 46, 231)));
        view.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = 12;
        dynamic.addView(view, params);
        return view;
    }

    private void heading(String status) {
        dynamic.removeAllViews();
        page(dynamic);
        text(status, 17);
    }

    private void error(int code, String message) {
        if (code == 401) {
            session.clearActiveSession();
            AccountNavigation.login(activity, false);
            return;
        }
        heading(code == 403 ? "Tu cuenta no tiene permiso para responder cuestionarios." : message);
        button("Reintentar", this::load);
    }

    private void load() {
        showOverview();
        activity.<TextView>findViewById(R.id.collab_wellness_tv_status_title).setText("Cargando evaluaciones…");
        ApiClient.getService().getOfficialWellnessTests(originalToken).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!sameSession() || activity.isFinishing()) return;
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()) {
                    error(response.code(), "No se pudieron cargar los cuestionarios del servidor."); return;
                }
                JsonObject data = response.body().getData();
                JsonArray tests = data != null && data.has("tests") ? data.getAsJsonArray("tests") : new JsonArray();
                monthlyTest = null;
                weeklyTest = null;
                for (JsonElement entry : tests) {
                    JsonObject test = entry.getAsJsonObject();
                    if ("DANI_MENSUAL".equals(string(test, "codigo"))) monthlyTest = test;
                    if ("DANI_SEMANAL".equals(string(test, "codigo"))) weeklyTest = test;
                }
                TextView monthly = activity.findViewById(R.id.collab_wellness_btn_start_monthly);
                monthly.setText(monthlyTest == null ? "Evaluación mensual no disponible" : "Realizar evaluación");
                monthly.setOnClickListener(v -> { if (monthlyTest != null) {currentCampaignId=null;form(monthlyTest);} });
                TextView legacyWeekly = activity.findViewById(R.id.collab_wellness_btn_legacy_weekly);
                if (legacyWeekly != null) legacyWeekly.setVisibility(View.GONE);
                View weeklyCard = activity.findViewById(R.id.collab_wellness_weekly_card);
                TextView weeklyOverview = activity.findViewById(R.id.collab_wellness_btn_start_weekly);
                if (weeklyCard != null) {
                    weeklyCard.setVisibility(weeklyTest == null ? View.GONE : View.VISIBLE);
                }
                if (weeklyOverview != null) {
                    weeklyOverview.setVisibility(weeklyTest == null ? View.GONE : View.VISIBLE);
                    weeklyOverview.setText("Comenzar evaluación semanal");
                    weeklyOverview.setOnClickListener(v -> { if (weeklyTest != null) {currentCampaignId=null;form(weeklyTest);} });
                }
                refreshRyffAvailability();
                refreshHistory(false);
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (sameSession() && !activity.isFinishing()) error(0, "Sin conexión. Tus respuestas no se han enviado.");
            }
        });
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private void openMySuggestions() {
        JsonObject latest = latestMonthly();
        if (latest != null) {
            openDetail(latest);
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Sugerencias para ti")
                .setMessage("Completa tu evaluación mensual para desbloquear tu pantalla personalizada de sugerencias.")
                .setNegativeButton("Ahora no", null)
                .setPositiveButton("Comenzar evaluación", (d, w) -> {
                    if (monthlyTest != null) {
                        currentCampaignId = null;
                        form(monthlyTest);
                    } else {
                        Toast.makeText(activity, "Evaluación mensual no disponible por ahora.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void openRyffCampaigns() {
        ApiClient.getService().getOwnWellnessCampaigns(originalToken).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!sameSession() || activity.isFinishing()) return;
                if (!response.isSuccessful() || response.body()==null || !response.body().isSuccessful()) {
                    Toast.makeText(activity,"No se pudo consultar la invitación Ryff.",Toast.LENGTH_SHORT).show();return;
                }
                JsonObject data=response.body().getData();
                JsonArray rows=data!=null && data.has("campanas")?data.getAsJsonArray("campanas"):new JsonArray();
                if(rows.size()==0){new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setMessage("No hay una campaña Ryff validada y vigente para tu cuenta.")
                        .setPositiveButton("Cerrar",null).show();return;}
                String[] titles=new String[rows.size()];
                for(int i=0;i<rows.size();i++)titles[i]=string(rows.get(i).getAsJsonObject(),"titulo");
                new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Campañas voluntarias")
                        .setItems(titles,(d,which)->openRyffCampaign(rows.get(which).getAsJsonObject().get("id_campana").getAsLong()))
                        .setNegativeButton("Cancelar",null).show();
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if(sameSession() && !activity.isFinishing())Toast.makeText(activity,"Sin conexión al consultar campañas.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openRyffCampaign(long id) {
        ApiClient.getService().getOwnWellnessCampaign(originalToken,id).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!sameSession() || activity.isFinishing()) return;
                if (!response.isSuccessful() || response.body()==null || !response.body().isSuccessful()) {
                    Toast.makeText(activity,"La campaña ya no está disponible.",Toast.LENGTH_SHORT).show();return;}
                JsonObject data=response.body().getData();
                if(data==null || !data.has("campana") || !data.has("preguntas"))return;
                JsonObject campaign=data.getAsJsonObject("campana");
                JsonObject test=new JsonObject();test.addProperty("id_test",campaign.get("id_test").getAsInt());
                test.addProperty("titulo",string(campaign,"titulo"));
                test.add("preguntas",data.getAsJsonArray("preguntas"));
                new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle(string(campaign,"titulo"))
                        .setMessage(string(campaign,"descripcion")+"\n\n"+string(data,"aviso")+
                                "\n\nPuedes decidir no participar. Al continuar y enviar tus respuestas, confirmas tu consentimiento para esta campaña.")
                        .setNegativeButton("No participar",null)
                        .setPositiveButton("Participar",(d,w)->{currentCampaignId=id;form(test);}).show();
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if(sameSession() && !activity.isFinishing())Toast.makeText(activity,"Sin conexión al abrir la campaña.",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshRyffAvailability() {
        TextView badge = activity.findViewById(R.id.collab_wellness_badge_ryff_state);
        TextView deadline = activity.findViewById(R.id.collab_wellness_tv_ryff_deadline);
        TextView openBtn = activity.findViewById(R.id.collab_wellness_btn_open_ryff);
        if (openBtn == null) return;
        ApiClient.getService().getOwnWellnessCampaigns(originalToken).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override
            public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!sameSession() || activity.isFinishing()) return;
                JsonObject data = response.isSuccessful() && response.body() != null && response.body().isSuccessful()
                        ? response.body().getData() : null;
                JsonArray rows = data != null && data.has("campanas") && data.get("campanas").isJsonArray()
                        ? data.getAsJsonArray("campanas") : new JsonArray();
                if (rows.size() > 0) {
                    JsonObject first = rows.get(0).getAsJsonObject();
                    if (badge != null) badge.setText("Invitación activa");
                    if (deadline != null) {
                        String end = string(first, "fin_en");
                        deadline.setText(end.isEmpty() ? "Disponible para responder" : "Vigente hasta: " + end);
                        deadline.setVisibility(View.VISIBLE);
                    }
                    openBtn.setText("Comenzar evaluación de Ryff");
                    openBtn.setBackgroundResource(R.drawable.collab_bg_chip_selected);
                    openBtn.setTextColor(Color.WHITE);
                    openBtn.setClickable(true);
                    openBtn.setAlpha(1f);
                    openBtn.setOnClickListener(v -> openRyffCampaigns());
                } else {
                    if (badge != null) badge.setText("Solo por invitación");
                    if (deadline != null) {
                        deadline.setText("No tienes invitaciones Ryff vigentes en este momento.");
                        deadline.setVisibility(View.VISIBLE);
                    }
                    openBtn.setVisibility(View.GONE);
                }
                if (response.errorBody() != null) response.errorBody().close();
            }

            @Override
            public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!sameSession() || activity.isFinishing()) return;
                if (deadline != null) {
                    deadline.setText("Requiere autorización vigente para participar.");
                    deadline.setVisibility(View.VISIBLE);
                }
                openBtn.setVisibility(View.GONE);
            }
        });
    }

    private JsonObject latestByTest(JsonObject testObj) {
        if (testObj == null || !testObj.has("id_test")) return null;
        int id = testObj.get("id_test").getAsInt();
        for (JsonElement element : historyRows) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            if (row.has("id_test") && row.get("id_test").getAsInt() == id
                    && "COMPLETADA".equals(string(row, "estado"))) {
                return row;
            }
        }
        return null;
    }

    private JsonObject latestMonthly() {
        return latestByTest(monthlyTest);
    }

    private void renderHistory() {
        JsonObject latest = latestMonthly();
        JsonObject latestWeekly = latestByTest(weeklyTest);

        activity.<TextView>findViewById(R.id.collab_wellness_tv_status_title).setText(
                latest == null ? "Un espacio para ti" : "Tu resumen está listo");
        activity.<TextView>findViewById(R.id.collab_wellness_tv_status_desc).setText(
                latest == null ? "Completa una evaluación para ver tu historial." :
                        "Tu evaluación de " + WellnessEvolutionHelper.formatPeriodKey(string(latest, "periodo_clave"))
                                + " está guardada en el servidor.");
        activity.<TextView>findViewById(R.id.collab_wellness_tv_recommendations).setText(
                latest == null ? "Las sugerencias aparecen después de completar la evaluación mensual." :
                        "Consulta Mis sugerencias o abre tu evaluación mensual para revisar tus orientaciones.");
        // Evitar botón duplicado "Ver mi resultado" en el hero (ya existe en Mis sugerencias y Evaluación mensual)
        activity.findViewById(R.id.collab_wellness_btn_hero_result).setVisibility(View.GONE);

        // Tarjeta única de Evaluación mensual (estado + período/fecha + una sola acción principal)
        TextView monthlyPeriodTv = activity.findViewById(R.id.collab_wellness_tv_month_period);
        TextView monthlyBadge = activity.findViewById(R.id.collab_wellness_badge_monthly_state);
        TextView monthlyButton = activity.findViewById(R.id.collab_wellness_btn_start_monthly);
        if (latest != null) {
            monthlyBadge.setText("Completada");
            if (monthlyPeriodTv != null) {
                String pLabel = WellnessEvolutionHelper.formatPeriodKey(string(latest, "periodo_clave"));
                String doneDate = WellnessEvolutionHelper.formatCompletedDate(string(latest, "fecha_completada"));
                monthlyPeriodTv.setText(doneDate.isEmpty() ? pLabel : pLabel + " · " + doneDate);
            }
            monthlyButton.setText("Ver mi resultado");
            monthlyButton.setOnClickListener(v -> openDetail(latest));
        } else {
            monthlyBadge.setText(monthlyTest == null ? "No disponible" : "Disponible");
            if (monthlyPeriodTv != null) monthlyPeriodTv.setText("Mes actual • 3 a 5 minutos");
            monthlyButton.setText(monthlyTest == null ? "Evaluación mensual no disponible" : "Realizar evaluación");
            monthlyButton.setOnClickListener(v -> {
                if (monthlyTest != null) {
                    currentCampaignId = null;
                    form(monthlyTest);
                }
            });
        }

        // Tarjeta única de Evaluación semanal (sin botones duplicados)
        TextView legacyWeekly = activity.findViewById(R.id.collab_wellness_btn_legacy_weekly);
        if (legacyWeekly != null) legacyWeekly.setVisibility(View.GONE);
        View weeklyCard = activity.findViewById(R.id.collab_wellness_weekly_card);
        TextView weeklyBadge = activity.findViewById(R.id.collab_wellness_badge_weekly_state);
        TextView weeklyPeriod = activity.findViewById(R.id.collab_wellness_tv_weekly_period);
        TextView weeklyBtn = activity.findViewById(R.id.collab_wellness_btn_start_weekly);
        if (weeklyCard != null && weeklyTest != null) {
            weeklyCard.setVisibility(View.VISIBLE);
            if (latestWeekly != null) {
                if (weeklyBadge != null) weeklyBadge.setText("Completada");
                if (weeklyPeriod != null) {
                    weeklyPeriod.setText(WellnessEvolutionHelper.formatPeriodKey(string(latestWeekly, "periodo_clave")));
                }
                if (weeklyBtn != null) {
                    weeklyBtn.setText("Ver mi resultado");
                    weeklyBtn.setOnClickListener(v -> openDetail(latestWeekly));
                }
            } else {
                if (weeklyBadge != null) weeklyBadge.setText("Disponible");
                if (weeklyPeriod != null) weeklyPeriod.setText("Seguimiento breve de la semana");
                if (weeklyBtn != null) {
                    weeklyBtn.setText("Comenzar evaluación semanal");
                    weeklyBtn.setOnClickListener(v -> {
                        currentCampaignId = null;
                        form(weeklyTest);
                    });
                }
            }
        } else if (weeklyCard != null) {
            weeklyCard.setVisibility(View.GONE);
        }

        View loadingView = activity.findViewById(R.id.collab_wellness_evo_loading);
        View errorView = activity.findViewById(R.id.collab_wellness_evo_error);
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (errorView != null) errorView.setVisibility(View.GONE);

        int monthlyTestId = monthlyTest != null && monthlyTest.has("id_test")
                ? monthlyTest.get("id_test").getAsInt() : 0;
        List<WellnessEvolutionHelper.EvaluationRecord> records = WellnessEvolutionHelper.parseHistory(historyRows);
        WellnessEvolutionHelper.EvaluationRecord featured = WellnessEvolutionHelper.selectFeatured(records, monthlyTestId);
        List<WellnessEvolutionHelper.EvaluationRecord> historyWithoutDuplicate =
                WellnessEvolutionHelper.excludeFeatured(records, featured);

        renderFeaturedEvolutionCard(featured);
        bindEvolutionPrimaryAction(featured);
        renderEvolutionTimeline(historyWithoutDuplicate, featured != null);

        activity.findViewById(R.id.collab_wellness_evolution_record).setVisibility(View.GONE);
    }

    private void renderFeaturedEvolutionCard(WellnessEvolutionHelper.EvaluationRecord featured) {
        View card = activity.findViewById(R.id.collab_wellness_evo_featured_card);
        ImageView icon = activity.findViewById(R.id.collab_wellness_evo_featured_icon);
        TextView title = activity.findViewById(R.id.collab_wellness_evo_featured_title);
        TextView period = activity.findViewById(R.id.collab_wellness_evo_featured_period);
        TextView badge = activity.findViewById(R.id.collab_wellness_evo_featured_badge);
        TextView date = activity.findViewById(R.id.collab_wellness_evo_featured_date);
        View chevron = activity.findViewById(R.id.collab_wellness_evo_featured_chevron);
        if (card == null) return;
        card.setVisibility(View.VISIBLE);

        if (featured == null) {
            if (icon != null) icon.setImageResource(R.drawable.collab_ill_wellness_clipboard_pending);
            if (title != null) title.setText("Evaluación mensual");
            if (period != null) {
                period.setText(monthlyTest != null
                        ? "Aún sin registros guardados"
                        : "Sin invitación o evaluación activa");
            }
            if (badge != null) {
                badge.setText(monthlyTest != null ? "Disponible" : "No disponible");
                badge.setBackgroundResource(R.drawable.collab_bg_notif_chip_unselected);
                badge.setTextColor(ContextCompat.getColor(activity, R.color.collab_notif_violet_soft));
            }
            if (date != null) date.setVisibility(View.GONE);
            if (chevron != null) chevron.setVisibility(View.GONE);
            card.setOnClickListener(null);
            card.setClickable(false);
            return;
        }

        boolean completed = featured.statusTone == WellnessEvolutionHelper.StatusTone.COMPLETED;
        if (icon != null) {
            icon.setImageResource(completed
                    ? R.drawable.collab_ill_wellness_clipboard_done
                    : R.drawable.collab_ill_wellness_clipboard_pending);
        }
        if (title != null) title.setText(featured.title);
        if (period != null) period.setText(featured.periodDisplay);
        if (badge != null) {
            badge.setText(featured.statusDisplay);
            applyStatusBadgeStyle(badge, featured.statusTone);
        }
        if (date != null) {
            if (featured.hasCompletedDate()) {
                date.setText(featured.completedAtDisplay);
                date.setVisibility(View.VISIBLE);
            } else {
                date.setVisibility(View.GONE);
            }
        }
        if (featured.canOpenDetail()) {
            if (chevron != null) chevron.setVisibility(View.VISIBLE);
            card.setClickable(true);
            card.setOnClickListener(v -> openDetail(featured.rawRow));
        } else {
            if (chevron != null) chevron.setVisibility(View.GONE);
            card.setClickable(false);
            card.setOnClickListener(null);
        }
    }

    private void bindEvolutionPrimaryAction(WellnessEvolutionHelper.EvaluationRecord featured) {
        View primaryBtn = activity.findViewById(R.id.collab_wellness_evo_primary_cta);
        TextView primaryLabel = activity.findViewById(R.id.collab_wellness_evo_primary_cta_label);
        if (primaryBtn == null || primaryLabel == null) return;

        // Si ya existe una tarjeta destacada clicable para consultar el resultado, eliminar botón duplicado
        if (featured != null && featured.canOpenDetail()) {
            primaryBtn.setVisibility(View.GONE);
            primaryBtn.setOnClickListener(null);
            return;
        }
        if (monthlyTest != null) {
            primaryBtn.setVisibility(View.VISIBLE);
            primaryBtn.setEnabled(true);
            primaryBtn.setAlpha(1f);
            primaryLabel.setText("Realizar evaluación mensual");
            primaryBtn.setOnClickListener(v -> {
                currentCampaignId = null;
                form(monthlyTest);
            });
        } else {
            primaryBtn.setVisibility(View.GONE);
            primaryBtn.setOnClickListener(null);
        }
    }

    private void renderEvolutionTimeline(List<WellnessEvolutionHelper.EvaluationRecord> records, boolean hasFeaturedRecord) {
        LinearLayout months = activity.findViewById(R.id.collab_wellness_evolution_months);
        if (months == null) return;
        months.removeAllViews();

        if (records.isEmpty()) {
            LinearLayout empty = new LinearLayout(activity);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.collab_bg_notif_empty_card);
            int pad = Math.round(18f * activity.getResources().getDisplayMetrics().density);
            empty.setPadding(pad, pad, pad, pad);

            TextView emptyTitle = new TextView(activity);
            emptyTitle.setText(hasFeaturedRecord
                    ? "Sin períodos anteriores adicionales"
                    : "Aún no tienes evaluaciones guardadas");
            emptyTitle.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
            emptyTitle.setTextSize(14f);
            emptyTitle.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            emptyTitle.setGravity(Gravity.CENTER);
            empty.addView(emptyTitle);

            TextView emptyDesc = new TextView(activity);
            emptyDesc.setText(hasFeaturedRecord
                    ? "Tu evaluación más reciente ya aparece arriba. Los siguientes períodos completados se irán listando aquí."
                    : "Cuando completes tu primera evaluación en el servidor, aparecerá aquí ordenada cronológicamente.");
            emptyDesc.setTextColor(ContextCompat.getColor(activity, R.color.collab_prog_text_secondary));
            emptyDesc.setTextSize(12f);
            emptyDesc.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descLp.topMargin = Math.round(6f * activity.getResources().getDisplayMetrics().density);
            empty.addView(emptyDesc, descLp);

            months.addView(empty);
            CollaboratorThemeHelper.applyReadableTypeface(months);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < records.size(); i++) {
            WellnessEvolutionHelper.EvaluationRecord rec = records.get(i);
            View row = inflater.inflate(R.layout.collaborator_item_wellness_history, months, false);
            View dot = row.findViewById(R.id.wellness_hist_dot);
            View line = row.findViewById(R.id.wellness_hist_line);
            View card = row.findViewById(R.id.wellness_hist_card);
            TextView tvTitle = row.findViewById(R.id.wellness_hist_tv_title);
            TextView tvStatus = row.findViewById(R.id.wellness_hist_tv_status);
            TextView tvPeriod = row.findViewById(R.id.wellness_hist_tv_period);
            TextView tvDate = row.findViewById(R.id.wellness_hist_tv_date);
            View chevron = row.findViewById(R.id.wellness_hist_chevron);

            if (dot != null) {
                dot.setBackgroundResource(rec.statusTone == WellnessEvolutionHelper.StatusTone.COMPLETED
                        ? R.drawable.collab_bg_level_node_done
                        : R.drawable.collab_bg_level_node_current);
            }
            if (line != null) {
                line.setVisibility(i == records.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            }
            if (tvTitle != null) tvTitle.setText(rec.title);
            if (tvPeriod != null) tvPeriod.setText(rec.periodDisplay);
            if (tvStatus != null) {
                tvStatus.setText(rec.statusDisplay);
                applyStatusBadgeStyle(tvStatus, rec.statusTone);
            }
            if (tvDate != null) {
                if (rec.hasCompletedDate()) {
                    tvDate.setText(rec.completedAtDisplay);
                    tvDate.setVisibility(View.VISIBLE);
                } else {
                    tvDate.setVisibility(View.GONE);
                }
            }
            if (card != null) {
                if (rec.canOpenDetail()) {
                    if (chevron != null) chevron.setVisibility(View.VISIBLE);
                    card.setClickable(true);
                    card.setOnClickListener(v -> openDetail(rec.rawRow));
                } else {
                    if (chevron != null) chevron.setVisibility(View.GONE);
                    card.setClickable(false);
                }
            }
            months.addView(row);
        }
        CollaboratorThemeHelper.applyReadableTypeface(months);
    }

    private void renderSupportResources() {
        LinearLayout list = activity.findViewById(R.id.collab_wellness_evo_resources_list);
        View section = activity.findViewById(R.id.collab_wellness_evo_resources_section);
        if (list == null || section == null) return;
        List<WellnessKnowledgeCatalog.Source> sources = WellnessKnowledgeCatalog.sources();
        if (sources.isEmpty()) {
            section.setVisibility(View.GONE);
            return;
        }
        section.setVisibility(View.VISIBLE);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < sources.size(); i++) {
            WellnessKnowledgeCatalog.Source src = sources.get(i);
            View card = inflater.inflate(R.layout.collaborator_item_wellness_resource, list, false);
            ImageView icon = card.findViewById(R.id.wellness_resource_icon);
            TextView tvTopic = card.findViewById(R.id.wellness_resource_topic);
            TextView tvSource = card.findViewById(R.id.wellness_resource_source);
            if (icon != null) {
                icon.setImageResource(i % 2 == 0
                        ? R.drawable.collab_ic_notif_heart_handshake
                        : R.drawable.collab_ic_heart_pulse);
            }
            if (tvTopic != null) tvTopic.setText(src.topic);
            if (tvSource != null) tvSource.setText(src.institution + " · " + src.title);
            card.setOnClickListener(v -> openExternalResource(src.url));
            list.addView(card);
        }
        CollaboratorThemeHelper.applyReadableTypeface(list);
    }

    private void openExternalResource(String url) {
        if (url == null || !url.startsWith("https://")) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(activity, "No se encontró un navegador para abrir la guía.", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyStatusBadgeStyle(TextView badge, WellnessEvolutionHelper.StatusTone tone) {
        if (tone == WellnessEvolutionHelper.StatusTone.COMPLETED) {
            badge.setBackgroundResource(R.drawable.collab_bg_notif_status_positive);
            badge.setTextColor(ContextCompat.getColor(activity, R.color.collab_notif_positive));
        } else if (tone == WellnessEvolutionHelper.StatusTone.IN_PROGRESS) {
            badge.setBackgroundResource(R.drawable.collab_bg_pill_amber);
            badge.setTextColor(ContextCompat.getColor(activity, R.color.collab_status_pending));
        } else {
            badge.setBackgroundResource(R.drawable.collab_bg_notif_chip_unselected);
            badge.setTextColor(ContextCompat.getColor(activity, R.color.collab_notif_violet_soft));
        }
    }

    private void addHistoryText(LinearLayout parent, String value, Runnable click) {
        TextView item = new TextView(activity);
        item.setText(value);
        item.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
        item.setTextSize(15);
        item.setPadding(18, 18, 18, 18);
        item.setBackgroundResource(R.drawable.collab_bg_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = 12;
        parent.addView(item, params);
        if (click != null) item.setOnClickListener(v -> click.run());
    }

    private void openLatestMonthly() {
        JsonObject row = latestMonthly();
        if (row != null) openDetail(row);
    }

    private void openDetail(JsonObject row) {
        openDetail(row, false);
    }

    private void openSavedDetail(JsonObject receipt) {
        if (receipt == null || !receipt.has("id_evaluacion_bienestar")) {
            heading("Tus respuestas quedaron guardadas en el servidor.");
            button("Ver mi evolución", this::history);
            return;
        }
        JsonObject row = new JsonObject();
        row.add("id_evaluacion_bienestar", receipt.get("id_evaluacion_bienestar"));
        openDetail(row, true);
    }

    private void openDetail(JsonObject row, boolean freshlySaved) {
        int id = row.get("id_evaluacion_bienestar").getAsInt();
        heading(freshlySaved ? "Respuestas guardadas. Preparando tu resultado…" :
                "Cargando tus respuestas del servidor…");
        ApiClient.getService().getOwnWellnessDetail(originalToken, id).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!sameSession() || activity.isFinishing()) return;
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()) {
                    detailError(response.code(), row, freshlySaved); return;
                }
                JsonObject data = response.body().getData();
                JsonObject evaluation = data.getAsJsonObject("evaluacion");
                JsonArray answers = data.getAsJsonArray("respuestas");
                if (monthlyTest != null && evaluation.get("id_test").getAsInt() == monthlyTest.get("id_test").getAsInt()) {
                    showMonthlyResult(evaluation, answers);
                } else {
                    showAnswerDetail(evaluation, answers);
                }
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (sameSession() && !activity.isFinishing()) detailError(0, row, freshlySaved);
            }
        });
    }

    private void detailError(int code, JsonObject row, boolean freshlySaved) {
        if (code == 401) { error(401, "Sesión vencida"); return; }
        heading(freshlySaved ? "Tus respuestas se guardaron, pero no se pudo cargar el resultado." :
                "No se pudo cargar esta evaluación.");
        button("Reintentar", () -> openDetail(row, freshlySaved));
        button("Mi evolución", this::history);
    }

    private void showAnswerDetail(JsonObject evaluation, JsonArray answers) {
        LinearLayout record = activity.findViewById(R.id.collab_wellness_evolution_record);
        record.removeAllViews();
        float density = activity.getResources().getDisplayMetrics().density;
        TextView title = new TextView(activity);
        title.setText(string(evaluation, "titulo") + " · "
                + WellnessEvolutionHelper.formatPeriodKey(string(evaluation, "periodo_clave")));
        title.setTextSize(16);
        title.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        title.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
        title.setPadding(0, 0, 0, Math.round(10 * density));
        record.addView(title);
        for (JsonElement element : answers) {
            JsonObject answer = element.getAsJsonObject();
            LinearLayout itemBox = new LinearLayout(activity);
            itemBox.setOrientation(LinearLayout.VERTICAL);
            itemBox.setBackgroundResource(R.drawable.collab_bg_notif_card_read);
            int pad = Math.round(12 * density);
            itemBox.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Math.round(8 * density);

            TextView qTv = new TextView(activity);
            qTv.setText(string(answer, "enunciado"));
            qTv.setTextSize(13f);
            qTv.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
            itemBox.addView(qTv);

            TextView aTv = new TextView(activity);
            aTv.setText(answerLabel(answer));
            aTv.setTextSize(12.5f);
            aTv.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            aTv.setTextColor(ContextCompat.getColor(activity, R.color.collab_notif_violet_soft));
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            aLp.topMargin = Math.round(4 * density);
            itemBox.addView(aTv, aLp);

            record.addView(itemBox, lp);
        }
        CollaboratorThemeHelper.applyReadableTypeface(record);
        record.setVisibility(View.VISIBLE);
        page(evolution);
    }

    private static String answerLabel(JsonObject answer) {
        String choice = string(answer, "opcion_texto");
        if (!choice.isEmpty()) return choice;
        String free = string(answer, "texto_respuesta");
        return !free.isEmpty() ? free : string(answer, "valor_numerico");
    }

    static int monthlyScore(String label) {
        switch (label.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "muy bien": return 1;
            case "bien": return 2;
            case "regular": return 3;
            case "mal": return 4;
            case "muy mal": return 5;
            default: return 0;
        }
    }

    private void showMonthlyResult(JsonObject evaluation, JsonArray answers) {
        if (answers.size() != 10) { showAnswerDetail(evaluation, answers); return; }
        int[] values = new int[10];
        for (int i = 0; i < values.length; i++) {
            values[i] = monthlyScore(string(answers.get(i).getAsJsonObject(), "opcion_texto"));
            if (values[i] == 0) { showAnswerDetail(evaluation, answers); return; }
        }
        WellnessRecommendationRules.Result orientation = WellnessRecommendationRules.forMonthlyAnswers(values);
        activity.<TextView>findViewById(R.id.collab_wellness_result_greeting).setText("Tu orientación personal");
        TextView month = activity.findViewById(R.id.collab_wellness_result_month);
        month.setText(string(evaluation, "periodo_clave") + " · respuestas del servidor");
        month.setVisibility(View.VISIBLE);
        TextView summary = activity.findViewById(R.id.collab_wellness_result_summary);
        summary.setText(orientation.summary + " Esta orientación no es un diagnóstico.");
        summary.setVisibility(View.VISIBLE);
        activity.<TextView>findViewById(R.id.collab_wellness_result_positive).setText(orientation.positives);
        activity.<TextView>findViewById(R.id.collab_wellness_result_attention).setText(orientation.attention);
        activity.findViewById(R.id.collab_wellness_result_analysis).setVisibility(View.VISIBLE);
        LinearLayout recommendations = activity.findViewById(R.id.collab_wellness_result_recommendations);
        recommendations.removeAllViews();
        View recHeader = activity.findViewById(R.id.collab_wellness_result_rec_header);
        if (recHeader != null) {
            recHeader.setVisibility(orientation.recommendations.isEmpty() ? View.GONE : View.VISIBLE);
        }
        int recIndex = 0;
        for (WellnessRecommendationRules.Recommendation recommendation : orientation.recommendations) {
            WellnessRecommendationCardBinder.inflateAndBind(
                    recommendations,
                    recommendation,
                    recommendation.detail,
                    recIndex++
            );
        }
        CollaboratorThemeHelper.applyReadableTypeface(result);
        page(result);
    }

    private void history() {
        page(evolution);
        showEvolutionLoading();
        refreshHistory(true);
    }

    private void showEvolutionLoading() {
        View loadingView = activity.findViewById(R.id.collab_wellness_evo_loading);
        View errorView = activity.findViewById(R.id.collab_wellness_evo_error);
        View featured = activity.findViewById(R.id.collab_wellness_evo_featured_card);
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (errorView != null) errorView.setVisibility(View.GONE);
        if (featured != null && historyRows.size() == 0) featured.setVisibility(View.GONE);
    }

    private void showEvolutionError(String message) {
        View loadingView = activity.findViewById(R.id.collab_wellness_evo_loading);
        View errorView = activity.findViewById(R.id.collab_wellness_evo_error);
        TextView errorMsg = activity.findViewById(R.id.collab_wellness_evo_error_msg);
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        if (errorMsg != null) errorMsg.setText(message);
        if (errorView != null) errorView.setVisibility(View.VISIBLE);
    }

    private void refreshHistory(boolean showEvolution) {
        if (showEvolution) showEvolutionLoading();
        ApiClient.getService().getOwnWellnessHistory(originalToken).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (!sameSession() || activity.isFinishing()) return;
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccessful()) {
                    if (response.code() == 401) {
                        error(401, "Sesión vencida");
                    } else if (showEvolution) {
                        showEvolutionError(response.code() == 403
                                ? "Tu cuenta no tiene permiso para consultar este historial."
                                : "No se pudo cargar tu historial del servidor.");
                    } else {
                        error(response.code(), "No se pudo cargar el historial.");
                    }
                    return;
                }
                JsonObject data = response.body().getData();
                historyRows = data != null && data.has("evaluaciones") ? data.getAsJsonArray("evaluaciones") : new JsonArray();
                renderHistory();
                if (showEvolution) page(evolution);
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (!sameSession() || activity.isFinishing()) return;
                if (showEvolution) {
                    showEvolutionError("Sin conexión. No se pudo consultar tu historial de evaluaciones.");
                } else {
                    error(0, "Sin conexión. No se pudo cargar el historial.");
                }
            }
        });
    }

    private void startOriginalFlow(JsonObject test, JsonArray questions) {
        currentTest = test;
        currentQuestions = questions;
        selectedOptions = new int[questions.size()];
        stepIndex = 0;
        formOpen = true;
        activity.findViewById(R.id.collab_wellness_btn_save_draft).setVisibility(View.GONE);
        activity.findViewById(R.id.collab_wellness_btn_prev).setOnClickListener(v -> {
            if (stepIndex > 0) { stepIndex--; renderStep(); } else goBack();
        });
        activity.findViewById(R.id.collab_wellness_btn_next).setOnClickListener(v -> {
            if (selectedOptions[stepIndex] == 0) {
                Toast.makeText(activity, "Elige una respuesta para continuar.", Toast.LENGTH_SHORT).show(); return;
            }
            if (stepIndex + 1 < currentQuestions.size()) { stepIndex++; renderStep(); }
            else renderReview();
        });
        activity.findViewById(R.id.collab_wellness_btn_back_to_edit).setOnClickListener(v -> renderStep());
        activity.findViewById(R.id.collab_wellness_btn_cancel_review).setOnClickListener(v -> goBack());
        activity.findViewById(R.id.collab_wellness_btn_confirm_send).setOnClickListener(v -> submitOriginalFlow());
        renderStep();
    }

    private void renderStep() {
        JsonObject question = currentQuestions.get(stepIndex).getAsJsonObject();
        int total = currentQuestions.size();
        int current = stepIndex + 1;
        String testTitle = string(currentTest, "titulo");
        activity.<TextView>findViewById(R.id.collab_wellness_step_tv_type).setText(
                testTitle.isEmpty() ? "Evaluación semanal" : testTitle);
        activity.<TextView>findViewById(R.id.collab_wellness_step_tv_counter).setText(
                "Pregunta " + current + " de " + total);

        ProgressBar progress = activity.findViewById(R.id.collab_wellness_step_pb);
        int targetProgress = current * 100;
        progress.setMax(total * 100);
        if (WellnessOptionCardBinder.animationsEnabled(activity) && progress.getProgress() > 0) {
            ObjectAnimator anim = ObjectAnimator.ofInt(progress, "progress", progress.getProgress(), targetProgress);
            anim.setDuration(200L);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.start();
        } else {
            progress.setProgress(targetProgress);
        }

        TextView dimensionView = activity.findViewById(R.id.collab_wellness_step_tv_dimension);
        String dimension = string(question, "dimension");
        dimensionView.setText(dimension.isEmpty() ? "PREGUNTA " + current : dimension.toUpperCase(java.util.Locale.ROOT));
        dimensionView.setVisibility(View.VISIBLE);
        activity.<TextView>findViewById(R.id.collab_wellness_step_tv_question).setText(string(question, "enunciado"));

        LinearLayout options = activity.findViewById(R.id.collab_wellness_options_container);
        List<WellnessOptionCardBinder.OptionItem> items = new ArrayList<>();
        for (JsonElement element : question.getAsJsonArray("opciones")) {
            JsonObject option = element.getAsJsonObject();
            items.add(new WellnessOptionCardBinder.OptionItem(
                    option.get("id_opcion").getAsInt(),
                    string(option, "texto"),
                    string(option, "descripcion")
            ));
        }
        WellnessOptionCardBinder.bind(options, items, selectedOptions[stepIndex], optionId -> {
            selectedOptions[stepIndex] = optionId;
            updateStepNavigationButtons();
        });

        updateStepNavigationButtons();
        page(step);
    }

    private void updateStepNavigationButtons() {
        TextView btnPrev = activity.findViewById(R.id.collab_wellness_btn_prev);
        TextView btnNext = activity.findViewById(R.id.collab_wellness_btn_next);
        boolean hasSelection = selectedOptions != null
                && stepIndex >= 0
                && stepIndex < selectedOptions.length
                && selectedOptions[stepIndex] != 0;

        if (btnPrev != null) {
            btnPrev.setText("Anterior");
            btnPrev.setAlpha(stepIndex == 0 ? 0.65f : 1f);
        }
        if (btnNext != null) {
            btnNext.setText(stepIndex + 1 == currentQuestions.size() ? "Revisar respuestas" : "Siguiente");
            float density = activity.getResources().getDisplayMetrics().density;
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(14f * density);
            bg.setColor(ContextCompat.getColor(activity,
                    hasSelection ? R.color.collab_wellness_violet_primary : R.color.collab_wellness_btn_disabled_bg));
            btnNext.setBackground(bg);
            btnNext.setTextColor(hasSelection
                    ? Color.WHITE
                    : ContextCompat.getColor(activity, R.color.collab_wellness_btn_disabled_text));
        }
    }

    private void renderReview() {
        LinearLayout list = activity.findViewById(R.id.collab_wellness_review_list_container);
        list.removeAllViews();
        float density = activity.getResources().getDisplayMetrics().density;
        int pad14 = Math.round(14f * density);
        int pad12 = Math.round(12f * density);
        for (int i = 0; i < currentQuestions.size(); i++) {
            JsonObject question = currentQuestions.get(i).getAsJsonObject();
            int index = i;
            String selected = "";
            for (JsonElement element : question.getAsJsonArray("opciones")) {
                JsonObject option = element.getAsJsonObject();
                if (option.get("id_opcion").getAsInt() == selectedOptions[i]) selected = string(option, "texto");
            }
            WellnessOptionVisualResolver.OptionVisual visual = WellnessOptionVisualResolver.resolveSingle(selected);
            int accent = ContextCompat.getColor(activity, visual.accentColorRes);

            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.collab_bg_card);
            row.setPadding(pad14, pad12, pad14, pad12);
            row.setClickable(true);
            row.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Math.round(10f * density);
            row.setLayoutParams(lp);

            if (visual.iconRes != 0) {
                FrameLayout iconWrap = new FrameLayout(activity);
                int wrapSize = Math.round((visual.isIllustratedFace() ? 44f : 38f) * density);
                GradientDrawable wrapBg = new GradientDrawable();
                wrapBg.setShape(visual.isIllustratedFace() ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
                if (!visual.isIllustratedFace()) {
                    wrapBg.setCornerRadius(11f * density);
                }
                wrapBg.setColor(Color.argb(26, Color.red(accent), Color.green(accent), Color.blue(accent)));
                iconWrap.setBackground(wrapBg);
                ImageView iv = new ImageView(activity);
                int iconSize = Math.round((visual.isIllustratedFace() ? 40f : 26f) * density);
                iv.setImageResource(visual.iconRes);
                iv.setContentDescription(null);
                iconWrap.addView(iv, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
                row.addView(iconWrap, new LinearLayout.LayoutParams(wrapSize, wrapSize));
            }

            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            colLp.setMarginStart(visual.iconRes != 0 ? Math.round(12f * density) : 0);
            colLp.setMarginEnd(Math.round(8f * density));
            col.setLayoutParams(colLp);

            TextView qText = new TextView(activity);
            qText.setText((i + 1) + ". " + string(question, "enunciado"));
            qText.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
            qText.setTextSize(13.5f);
            qText.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            col.addView(qText);

            TextView ansText = new TextView(activity);
            ansText.setText(selected);
            ansText.setTextColor(accent);
            ansText.setTextSize(12.5f);
            ansText.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            LinearLayout.LayoutParams ansLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ansLp.topMargin = Math.round(4f * density);
            col.addView(ansText, ansLp);

            row.addView(col);

            TextView editBadge = new TextView(activity);
            editBadge.setText("Editar");
            editBadge.setTextColor(ContextCompat.getColor(activity, R.color.collab_purple_accent));
            editBadge.setTextSize(11.5f);
            editBadge.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
            editBadge.setBackgroundResource(R.drawable.collab_bg_pill_purple);
            editBadge.setPadding(Math.round(10f * density), Math.round(5f * density),
                    Math.round(10f * density), Math.round(5f * density));
            row.addView(editBadge);

            row.setOnClickListener(v -> { stepIndex = index; renderStep(); });
            list.addView(row);
        }
        page(review);
    }

    private void submitOriginalFlow() {
        if (submitting) return;
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < currentQuestions.size(); i++) {
            if (selectedOptions[i] == 0) { stepIndex = i; renderStep(); return; }
            Map<String, Object> answer = new HashMap<>();
            answer.put("id_pregunta", currentQuestions.get(i).getAsJsonObject().get("id_pregunta").getAsInt());
            answer.put("id_opcion_seleccionada", selectedOptions[i]);
            answers.add(answer);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("id_test", currentTest.get("id_test").getAsInt());
        body.put("respuestas", answers);
        submitting = true;
        activity.<TextView>findViewById(R.id.collab_wellness_btn_confirm_send).setText("Guardando en el servidor…");
        ApiClient.getService().submitWellnessAnswers(originalToken, body).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                submitting = false;
                if (!sameSession() || activity.isFinishing()) return;
                activity.<TextView>findViewById(R.id.collab_wellness_btn_confirm_send).setText("Finalizar evaluación");
                if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                    formOpen = false;
                    openSavedDetail(response.body().getData());
                } else if (response.code() == 401) error(401, "Sesión vencida");
                else {
                    String code = safeServerCode(response);
                    Toast.makeText(activity, response.code() == 409 ?
                            "Ya respondiste en este período. Revisa tu historial." :
                            "No se guardaron las respuestas (HTTP " + response.code() +
                                    (code.isEmpty() ? "" : ", " + code) + ").", Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                submitting = false;
                if (!sameSession() || activity.isFinishing()) return;
                activity.<TextView>findViewById(R.id.collab_wellness_btn_confirm_send).setText("Finalizar evaluación");
                Toast.makeText(activity, "Sin conexión. Tus respuestas siguen en esta pantalla.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void form(JsonObject test) {
        submitting = false;
        JsonArray questions = test.has("preguntas") ? test.getAsJsonArray("preguntas") : new JsonArray();
        boolean originalFlow = ("DANI_MENSUAL".equals(string(test, "codigo")) ||
                "DANI_SEMANAL".equals(string(test, "codigo"))) && questions.size() > 0;
        for (JsonElement element : questions) {
            JsonObject question = element.getAsJsonObject();
            if (!"OPCION_MULTIPLE".equals(string(question, "tipo_respuesta")) ||
                    !question.has("opciones") || question.getAsJsonArray("opciones").size() == 0) originalFlow = false;
        }
        if (originalFlow) { startOriginalFlow(test, questions); return; }
        formOpen = true;
        heading(string(test, "titulo") + " · Respuestas privadas");
        if (questions.size() == 0) { text("Este cuestionario aún no tiene preguntas oficiales.", 15); button("Volver", this::load); return; }
        List<Map<String, Object>> answers = new ArrayList<>();
        List<Runnable> collect = new ArrayList<>();
        for (JsonElement entry : questions) {
            JsonObject question = entry.getAsJsonObject();
            int questionId = question.get("id_pregunta").getAsInt();
            String type = string(question, "tipo_respuesta");
            text(string(question, "enunciado"), 17);
            Map<String, Object> answer = new HashMap<>();
            answer.put("id_pregunta", questionId);
            answers.add(answer);
            if ("TEXTO_LIBRE".equals(type)) {
                EditText input = new EditText(activity);
                input.setHint("Escribe tu respuesta");
                input.setTextColor(ContextCompat.getColor(activity, R.color.collab_text_primary));
                input.setHintTextColor(ContextCompat.getColor(activity, R.color.collab_text_muted));
                input.setBackgroundResource(R.drawable.collab_bg_input_box);
                int pad = Math.round(14f * activity.getResources().getDisplayMetrics().density);
                input.setPadding(pad, pad, pad, pad);
                LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                inputLp.bottomMargin = pad;
                dynamic.addView(input, inputLp);
                collect.add(() -> { if (!input.getText().toString().trim().isEmpty()) answer.put("texto_respuesta", input.getText().toString().trim()); });
            } else {
                LinearLayout optionGroup = new LinearLayout(activity);
                optionGroup.setOrientation(LinearLayout.VERTICAL);
                List<WellnessOptionCardBinder.OptionItem> items = new ArrayList<>();
                if ("OPCION_MULTIPLE".equals(type)) {
                    JsonArray options = question.getAsJsonArray("opciones");
                    for (JsonElement optionElement : options) {
                        JsonObject option = optionElement.getAsJsonObject();
                        items.add(new WellnessOptionCardBinder.OptionItem(
                                option.get("id_opcion").getAsInt(),
                                string(option, "texto"),
                                string(option, "descripcion")
                        ));
                    }
                    WellnessOptionCardBinder binder = WellnessOptionCardBinder.bind(optionGroup, items, Integer.MIN_VALUE, null);
                    collect.add(() -> {
                        int picked = binder.getSelectedOptionId();
                        if (picked != Integer.MIN_VALUE) answer.put("id_opcion_seleccionada", picked);
                    });
                } else {
                    int min = "SI_NO".equals(type) ? 0 : 1;
                    int max = "SI_NO".equals(type) ? 1 : 5;
                    for (int value = min; value <= max; value++) {
                        items.add(new WellnessOptionCardBinder.OptionItem(
                                value,
                                "SI_NO".equals(type) ? (value == 1 ? "Sí" : "No") : Integer.toString(value),
                                ""
                        ));
                    }
                    WellnessOptionCardBinder binder = WellnessOptionCardBinder.bind(optionGroup, items, Integer.MIN_VALUE, null);
                    collect.add(() -> {
                        int picked = binder.getSelectedOptionId();
                        if (picked != Integer.MIN_VALUE) answer.put("valor_numerico", picked);
                    });
                }
                LinearLayout.LayoutParams groupLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                groupLp.bottomMargin = Math.round(10f * activity.getResources().getDisplayMetrics().density);
                dynamic.addView(optionGroup, groupLp);
            }
        }
        TextView status = text("", 14);
        button("Guardar respuestas en el servidor", () -> {
            if (submitting) return;
            for (Runnable collector : collect) collector.run();
            for (Map<String, Object> answer : answers) if (answer.size() != 2) { status.setText("Responde todas las preguntas antes de guardar."); return; }
            submitting = true;
            status.setText("Guardando…");
            Map<String, Object> body = new HashMap<>();
            body.put("id_test", test.get("id_test").getAsInt());
            body.put("respuestas", answers);
            if(currentCampaignId!=null){body.put("id_campana",currentCampaignId.intValue());body.put("consentimiento_informado",true);}
            ApiClient.getService().submitWellnessAnswers(originalToken, body).enqueue(new Callback<ApiResponse<JsonObject>>() {
                @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                    submitting = false;
                    if (!sameSession() || activity.isFinishing()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccessful()) {
                        formOpen = false;
                        openSavedDetail(response.body().getData());
                    } else if (response.code() == 401) error(401, "Sesión vencida");
                    else {
                        String serverCode = safeServerCode(response);
                        if (response.code() == 409) {
                            status.setText("PREVIOUS_ANSWERS".equals(serverCode)
                                    ? "Hay una evaluación incompleta anterior. Tus respuestas previas se conservaron; solicita revisión."
                                    : "Ya respondiste este cuestionario en el período actual.");
                        } else {
                            String reference = serverCode.startsWith("WELLNESS_WRITE_") ? ", " + serverCode : "";
                            status.setText("No se guardaron las respuestas (HTTP " + response.code() + reference + "). Puedes reintentar.");
                        }
                    }
                }
                @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                    submitting = false;
                    if (sameSession() && !activity.isFinishing()) status.setText("Sin conexión. Las respuestas siguen en esta pantalla; reintenta cuando vuelva internet.");
                }
            });
        });
        button("Cancelar", this::goBack);
    }

    private static String safeServerCode(Response<?> response) {
        if ((response.code() != 500 && response.code() != 409) || response.errorBody() == null) return "";
        try {
            JsonObject error = JsonParser.parseString(response.errorBody().string()).getAsJsonObject();
            String code = error.get("error_code").getAsString();
            return code.matches("WELLNESS_WRITE_[0-9]{1,5}_L[0-9]{1,5}") || "PREVIOUS_ANSWERS".equals(code) ? code : "";
        } catch (Exception ignored) {
            return "";
        }
    }
}
