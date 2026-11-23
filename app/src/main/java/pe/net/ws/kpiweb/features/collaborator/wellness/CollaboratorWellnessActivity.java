package pe.net.ws.kpiweb.features.collaborator.wellness;

import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBannerHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorBottomNavHelper;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorSessionManager;
import pe.net.ws.kpiweb.features.collaborator.common.CollaboratorThemeHelper;
import pe.net.ws.kpiweb.features.collaborator.dashboard.model.CollaboratorProfile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Recorridos locales de demostración para el bienestar del colaborador.
 * La evaluación mensual contiene preguntas originales; Ryff solo muestra ejemplos visuales
 * en debug. Se conserva la evaluación semanal anterior y sus datos locales.
 * Ningún resultado se envía al servidor ni afecta KPI, premios o ranking.
 */
public class CollaboratorWellnessActivity extends AppCompatActivity {

    public static final String EXTRA_RETURN_ADMIN = "pe.net.ws.kpiweb.RETURN_ADMIN_WELLNESS";
    private static final String PREFS_WELLNESS = "website_collab_wellness_v2_prefs";

    private enum ScreenPhase {
        OVERVIEW,
        INTRO,
        STEP_BY_STEP,
        REVIEW,
        COMPLETED,
        RESULT,
        EVOLUTION
    }

    private enum EvaluationType {
        WEEKLY("Evaluación semanal", 6),
        MONTHLY("Evaluación mensual", 10),
        RYFF_DEMO("Ryff — ejemplo visual", 3);

        final String title;
        final int totalQuestions;

        EvaluationType(String title, int totalQuestions) {
            this.title = title;
            this.totalQuestions = totalQuestions;
        }
    }

    public static final class MoodOption {
        public final int scoreValue; // 5..1
        public final String label;
        public final String subtitle;
        public final String hexColor;
        public final int iconRes;

        public MoodOption(int scoreValue, String label, String subtitle, String hexColor, int iconRes) {
            this.scoreValue = scoreValue;
            this.label = label;
            this.subtitle = subtitle;
            this.hexColor = hexColor;
            this.iconRes = iconRes;
        }
    }

    private static final MoodOption[] MOOD_OPTIONS = new MoodOption[]{
            new MoodOption(5, "Muy bien", "Con gran energía, claridad y bienestar positivo",
                    "#69D9AB", R.drawable.collab_ic_mood_very_good),
            new MoodOption(4, "Bien", "Tranquilo/a, enfocado/a y con buen ritmo laboral",
                    "#A8DBA8", R.drawable.collab_ic_mood_good),
            new MoodOption(3, "Neutral", "Estable, con momentos normales de exigencia",
                    "#C69BFF", R.drawable.collab_ic_mood_neutral),
            new MoodOption(2, "Mal", "Con cansancio acumulado o presión percibida",
                    "#F3BA7D", R.drawable.collab_ic_mood_bad),
            new MoodOption(1, "Muy mal", "Con sobrecarga alta; requiero pausa o apoyo",
                    "#F0A0AD", R.drawable.collab_ic_mood_very_bad)
    };

    private static final MoodOption[] MONTHLY_MOOD_OPTIONS = new MoodOption[]{
            new MoodOption(5, "Muy mal", "Me costó mucho este mes", "#F0A0AD", R.drawable.collab_ic_mood_very_bad),
            new MoodOption(4, "Mal", "Hubo dificultades", "#F3BA7D", R.drawable.collab_ic_mood_bad),
            new MoodOption(3, "Regular", "Tuve experiencias mixtas", "#C69BFF", R.drawable.collab_ic_mood_neutral),
            new MoodOption(2, "Bien", "Me sentí bien", "#A8DBA8", R.drawable.collab_ic_mood_good),
            new MoodOption(1, "Muy bien", "Me sentí muy bien", "#69D9AB", R.drawable.collab_ic_mood_very_good)
    };

    private static final String[] WEEKLY_DIMENSIONS = new String[]{
            "Estado de ánimo",
            "Motivación",
            "Carga laboral percibida",
            "Cansancio",
            "Apoyo recibido",
            "Satisfacción general de la semana"
    };

    private static final String[] WEEKLY_QUESTIONS = new String[]{
            "¿Cómo describirías tu estado de ánimo general durante esta semana?",
            "¿Qué nivel de motivación sentiste frente a tus actividades de la semana?",
            "¿Cómo percibiste el equilibrio y manejo de tu carga laboral esta semana?",
            "¿Cómo te sentiste respecto a tu energía y nivel de descanso frente al cansancio?",
            "¿Qué tan respaldado/a te sentiste por tu equipo y coordinación esta semana?",
            "¿Cuál es tu grado de satisfacción general al cerrar esta semana?"
    };

    private static final String[] MONTHLY_DIMENSIONS = new String[]{
            "Experiencia de trabajo", "Carga de tareas", "Responsabilidades",
            "Comunicación", "Apoyo del equipo", "Trabajo y descanso",
            "Recursos", "Aprendizaje", "Reconocimiento", "Balance del mes"
    };

    private static final String[] MONTHLY_QUESTIONS = new String[]{
            "¿Cómo te sentiste con tu experiencia de trabajo este mes?",
            "¿Cómo te sentiste con la cantidad de tareas que realizaste este mes?",
            "¿Cómo te sentiste con la claridad de tus responsabilidades este mes?",
            "¿Cómo te sentiste con la comunicación con tus compañeros este mes?",
            "¿Cómo te sentiste con el apoyo del equipo cuando lo necesitaste?",
            "¿Cómo te sentiste con el tiempo disponible para descansar fuera del trabajo?",
            "¿Cómo te sentiste con los recursos disponibles para tus tareas?",
            "¿Cómo te sentiste con las oportunidades de aprender este mes?",
            "¿Cómo te sentiste con el reconocimiento de tu esfuerzo este mes?",
            "En general, ¿cómo describirías tu experiencia laboral de este mes?"
    };

    private static final String[] MONTHLY_OPTIONS = {"Muy bien", "Bien", "Regular", "Mal", "Muy mal"};
    private static final String[] RYFF_OPTIONS = {"Totalmente en desacuerdo", "En desacuerdo", "Neutral", "De acuerdo", "Totalmente de acuerdo"};
    private static final String[] RYFF_DIMENSIONS = {"Autoaceptación", "Relaciones positivas", "Propósito en la vida"};
    private static final String[] RYFF_SIX_DIMENSIONS = {
            "Autoaceptación", "Relaciones positivas", "Autonomía", "Dominio del entorno",
            "Crecimiento personal", "Propósito en la vida"
    };
    private static final String[] RYFF_EXAMPLE_QUESTIONS = {
            "Ejemplo visual — Me resulta fácil reconocer cualidades personales que valoro.",
            "Ejemplo visual — Siento que puedo construir relaciones de apoyo con otras personas.",
            "Ejemplo visual — Tengo metas personales que me ayudan a orientar mis decisiones."
    };
    private static final long RYFF_DEMO_WINDOW_MS = 7L * 24 * 60 * 60 * 1000;

    private CollaboratorProfile activeProfile;
    private SharedPreferences prefs;
    private WellnessRecommendationRepository recommendationRepository;
    private OfficialWellnessScreen officialScreen;

    private ScreenPhase currentPhase = ScreenPhase.OVERVIEW;
    private EvaluationType activeEvalType = EvaluationType.MONTHLY;
    private int currentQuestionIndex = 0;

    // Respuestas en edición durante el flujo paso a paso (0 = sin seleccionar aún, 1..5 = seleccionado)
    private int[] draftAnswers = new int[10];
    private String resultMonthKey;
    private String resultMonthLabel;
    private boolean recommendationsVisible;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        CollaboratorThemeHelper.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        SessionManager realSession = new SessionManager(this);
        if (realSession.isLoggedIn() && realSession.getWorkerId() > 0) {
            officialScreen = new OfficialWellnessScreen(this, realSession);
            setupBackNavigationProtection();
            officialScreen.show();
            return;
        }
        if (!realSession.isLoggedIn() || realSession.getWorkerId() <= 0) {
            pe.net.ws.kpiweb.navigation.AccountNavigation.login(this, false);
            finish();
            return;
        }
        setContentView(R.layout.collaborator_activity_wellness);
        CollaboratorThemeHelper.setupSystemBars(this);

        prefs = getSharedPreferences(PREFS_WELLNESS, Context.MODE_PRIVATE);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        recommendationRepository = new WellnessRecommendationRepository(prefs, new SessionManager(this).getWorkerId());

        setupHeaderAndListeners();
        setupBackNavigationProtection();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.WELLNESS);
        showOverviewScreen();
        if (savedInstanceState != null) restoreScreenState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (officialScreen != null) { super.onSaveInstanceState(outState); return; }
        outState.putString("wellness_phase", currentPhase.name());
        outState.putString("wellness_type", activeEvalType.name());
        outState.putInt("wellness_question", currentQuestionIndex);
        outState.putIntArray("wellness_answers", draftAnswers);
        outState.putString("wellness_result_month_key", resultMonthKey);
        outState.putString("wellness_result_month_label", resultMonthLabel);
        outState.putBoolean("wellness_recommendations_visible", recommendationsVisible);
        super.onSaveInstanceState(outState);
    }

    private void restoreScreenState(Bundle savedState) {
        try {
            EvaluationType type = EvaluationType.valueOf(savedState.getString("wellness_type", "MONTHLY"));
            int[] answers = savedState.getIntArray("wellness_answers");
            if (answers != null && answers.length == type.totalQuestions) draftAnswers = answers;
            activeEvalType = type;
            currentQuestionIndex = Math.max(0, Math.min(type.totalQuestions - 1,
                    savedState.getInt("wellness_question", 0)));
            ScreenPhase phase = ScreenPhase.valueOf(savedState.getString("wellness_phase", "OVERVIEW"));
            if (type == EvaluationType.RYFF_DEMO && !isRyffDemoAvailable()) phase = ScreenPhase.OVERVIEW;
            if (phase == ScreenPhase.INTRO) showIntroScreen(type);
            else if (phase == ScreenPhase.STEP_BY_STEP) {
                currentPhase = phase;
                updatePhaseVisibility();
                renderCurrentQuestionStep();
            } else if (phase == ScreenPhase.REVIEW) showReviewScreen();
            else if (phase == ScreenPhase.COMPLETED) {
                currentPhase = phase;
                setVisible(R.id.collab_wellness_btn_open_result, type == EvaluationType.MONTHLY);
                updatePhaseVisibility();
            }
            else if (phase == ScreenPhase.RESULT) {
                boolean showRecommendations = savedState.getBoolean("wellness_recommendations_visible", false);
                showResultScreen(savedState.getString("wellness_result_month_key", monthKey()),
                        savedState.getString("wellness_result_month_label", monthLabel()));
                if (showRecommendations) showRecommendations();
            }
            else if (phase == ScreenPhase.EVOLUTION) showEvolutionScreen();
            else showOverviewScreen();
        } catch (IllegalArgumentException ignored) {
            showOverviewScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (officialScreen != null) return;
        CollaboratorThemeHelper.setupSystemBars(this);
        activeProfile = CollaboratorSessionManager.getInstance(this).getActiveProfile();
        CollaboratorBottomNavHelper.setup(this, CollaboratorBottomNavHelper.NavTab.WELLNESS);
        if (currentPhase == ScreenPhase.OVERVIEW) {
            refreshOverviewSummary();
        }
    }

    private void setupBackNavigationProtection() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExitOrBackRequest();
            }
        });
    }

    private void handleExitOrBackRequest() {
        if (officialScreen != null) { officialScreen.goBack(); return; }
        if (currentPhase == ScreenPhase.STEP_BY_STEP || currentPhase == ScreenPhase.REVIEW) {
            confirmDiscardEvaluation();
        } else if (currentPhase == ScreenPhase.COMPLETED || currentPhase == ScreenPhase.INTRO
                || currentPhase == ScreenPhase.EVOLUTION || currentPhase == ScreenPhase.RESULT) {
            showOverviewScreen();
        } else {
            finish();
        }
    }

    private void confirmDiscardEvaluation() {
        CollaboratorBannerHelper.showPremiumModal(
                this,
                "Evaluación en curso • WEBSITE",
                "¿Deseas salir de la evaluación actual?",
                "Si sales ahora, las respuestas que aún no has confirmado en este intento no se guardarán.",
                "Salir sin guardar",
                this::showOverviewScreen,
                "Continuar respondiendo"
        );
    }

    private void setupHeaderAndListeners() {
        View btnBack = findViewById(R.id.collab_wellness_btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleExitOrBackRequest());
        }

        View btnStartMonthly = findViewById(R.id.collab_wellness_btn_start_monthly);
        if (btnStartMonthly != null) {
            btnStartMonthly.setOnClickListener(v -> {
                if (isMonthlyCompleted()) showResultScreen(monthKey(), monthLabel());
                else showIntroScreen(EvaluationType.MONTHLY);
            });
        }
        View btnHeroResult = findViewById(R.id.collab_wellness_btn_hero_result);
        if (btnHeroResult != null) btnHeroResult.setOnClickListener(v -> {
            if (isMonthlyCompleted()) showResultScreen(monthKey(), monthLabel());
        });

        View btnRyff = findViewById(R.id.collab_wellness_btn_open_ryff);
        if (btnRyff != null) btnRyff.setOnClickListener(v -> {
            if (isRyffDemoAvailable()) showIntroScreen(EvaluationType.RYFF_DEMO);
        });

        View btnBackEvaluations = findViewById(R.id.collab_wellness_btn_back_evaluations);
        if (btnBackEvaluations != null) btnBackEvaluations.setOnClickListener(v -> showOverviewScreen());
        View btnLegacyWeekly = findViewById(R.id.collab_wellness_btn_legacy_weekly);
        if (btnLegacyWeekly != null) btnLegacyWeekly.setOnClickListener(v -> showIntroScreen(EvaluationType.WEEKLY));
        View btnIntroStart = findViewById(R.id.collab_wellness_intro_start);
        if (btnIntroStart != null) btnIntroStart.setOnClickListener(v -> startEvaluationFlow(activeEvalType));
        View btnIntroDecline = findViewById(R.id.collab_wellness_intro_decline);
        if (btnIntroDecline != null) btnIntroDecline.setOnClickListener(v -> showOverviewScreen());
        View btnSaveDraft = findViewById(R.id.collab_wellness_btn_save_draft);
        if (btnSaveDraft != null) btnSaveDraft.setOnClickListener(v -> saveDraft());

        View btnPrev = findViewById(R.id.collab_wellness_btn_prev);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentQuestionIndex > 0) {
                    currentQuestionIndex--;
                    renderCurrentQuestionStep();
                } else {
                    confirmDiscardEvaluation();
                }
            });
        }

        View btnNext = findViewById(R.id.collab_wellness_btn_next);
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (draftAnswers[currentQuestionIndex] <= 0) {
                    CollaboratorBannerHelper.showBanner(
                            this,
                            "Selecciona una respuesta",
                            "Elige una de las cinco opciones antes de avanzar.",
                            R.drawable.collab_ic_heart_pulse
                    );
                    return;
                }
                if (currentQuestionIndex < activeEvalType.totalQuestions - 1) {
                    currentQuestionIndex++;
                    renderCurrentQuestionStep();
                } else {
                    showReviewScreen();
                }
            });
        }

        View btnBackToEdit = findViewById(R.id.collab_wellness_btn_back_to_edit);
        if (btnBackToEdit != null) {
            btnBackToEdit.setOnClickListener(v -> {
                currentPhase = ScreenPhase.STEP_BY_STEP;
                updatePhaseVisibility();
                renderCurrentQuestionStep();
            });
        }

        View btnCancelReview = findViewById(R.id.collab_wellness_btn_cancel_review);
        if (btnCancelReview != null) {
            btnCancelReview.setOnClickListener(v -> confirmDiscardEvaluation());
        }

        View btnConfirmSend = findViewById(R.id.collab_wellness_btn_confirm_send);
        if (btnConfirmSend != null) {
            btnConfirmSend.setOnClickListener(v -> {
                for (int answer : draftAnswers) {
                    if (answer < 1 || answer > 5) {
                        CollaboratorBannerHelper.showBanner(this, "Faltan respuestas",
                                "Completa todas las preguntas antes de finalizar.",
                                R.drawable.collab_ic_info_circle);
                        return;
                    }
                }
                CollaboratorBannerHelper.showPremiumModal(this, "Confirmar evaluación",
                        "¿Deseas finalizar y guardar tus respuestas?",
                        "Esta demostración guarda el registro solo en este dispositivo. No se envía al administrador ni a una IA.",
                        "Guardar y finalizar", this::saveAndCompleteEvaluation,
                        "Seguir revisando");
            });
        }

        View btnFinishClose = findViewById(R.id.collab_wellness_btn_finish_close);
        if (btnFinishClose != null) {
            btnFinishClose.setOnClickListener(v -> showOverviewScreen());
        }
        View btnOpenResult = findViewById(R.id.collab_wellness_btn_open_result);
        if (btnOpenResult != null) btnOpenResult.setOnClickListener(v -> showResultScreen(monthKey(), monthLabel()));
        View btnShowRecommendations = findViewById(R.id.collab_wellness_btn_show_recommendations);
        if (btnShowRecommendations != null) btnShowRecommendations.setOnClickListener(v -> showRecommendations());
        View btnResultFinish = findViewById(R.id.collab_wellness_btn_result_finish);
        if (btnResultFinish != null) btnResultFinish.setOnClickListener(v -> showOverviewScreen());
        View preferencesToggle = findViewById(R.id.collab_wellness_preferences_toggle);
        View preferencesPanel = findViewById(R.id.collab_wellness_preferences_panel);
        preferencesToggle.setOnClickListener(v -> preferencesPanel.setVisibility(
                preferencesPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        Switch personalization = findViewById(R.id.collab_wellness_personalization_switch);
        personalization.setOnCheckedChangeListener((button, checked) -> {
            recommendationRepository.setEnabled(checked);
            if (currentPhase == ScreenPhase.RESULT) showResultScreen(resultMonthKey, resultMonthLabel);
        });
        findViewById(R.id.collab_wellness_clear_preferences).setOnClickListener(v -> {
            recommendationRepository.clear();
            CollaboratorBannerHelper.showBanner(this, "Preferencias borradas",
                    "Se eliminaron las valoraciones aprendidas de esta cuenta en el dispositivo.", R.drawable.collab_ic_info_circle);
            if (currentPhase == ScreenPhase.RESULT) showResultScreen(resultMonthKey, resultMonthLabel);
        });
    }

    private void showOverviewScreen() {
        currentPhase = ScreenPhase.OVERVIEW;
        updatePhaseVisibility();
        refreshOverviewSummary();
        scrollToTop();
    }

    private void showIntroScreen(EvaluationType type) {
        if (type == EvaluationType.RYFF_DEMO && !isRyffDemoAvailable()) return;
        activeEvalType = type;
        currentPhase = ScreenPhase.INTRO;
        updatePhaseVisibility();
        if (type == EvaluationType.RYFF_DEMO) {
            setText(R.id.collab_wellness_intro_title, "Conoce tu bienestar psicológico");
            setText(R.id.collab_wellness_intro_desc,
                    "Esta evaluación te invita a reflexionar sobre autoaceptación, relaciones, autonomía, dominio del entorno, crecimiento personal y propósito en la vida. "
                            + "La versión final de 18 ítems, su adaptación y puntuación requieren validación profesional. Los tres ítems siguientes son ejemplos visuales, no preguntas oficiales de Ryff.");
            String deadline = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "PE"))
                    .format(new Date(prefs.getLong(ryffPrefix() + "assigned_at", System.currentTimeMillis())
                            + RYFF_DEMO_WINDOW_MS));
            setText(R.id.collab_wellness_intro_meta,
                    "Invitación de prueba • 3 ejemplos visuales • Plazo: " + deadline);
        } else if (type == EvaluationType.MONTHLY) {
            setText(R.id.collab_wellness_intro_title, "Evaluación mensual");
            setText(R.id.collab_wellness_intro_desc,
                    "Reflexiona sobre tu experiencia laboral de " + monthLabel() + ". "
                            + "Son diez preguntas originales de demostración; no forman una escala psicológica validada.");
            setText(R.id.collab_wellness_intro_meta, "10 preguntas • 3 a 5 minutos • " + monthLabel());
        } else {
            setText(R.id.collab_wellness_intro_title, "Evaluación semanal anterior");
            setText(R.id.collab_wellness_intro_desc,
                    "Recorrido de demostración conservado de la versión anterior. Su promedio local no es una evaluación clínica.");
            setText(R.id.collab_wellness_intro_meta, "6 preguntas • Guardado local de prueba");
        }
        scrollToTop();
    }

    private void showEvolutionScreen() {
        currentPhase = ScreenPhase.EVOLUTION;
        updatePhaseVisibility();
        LinearLayout list = findViewById(R.id.collab_wellness_evolution_months);
        if (list != null) {
            list.removeAllViews();
            Calendar month = Calendar.getInstance();
            for (int i = 0; i < 6; i++) {
                String key = new SimpleDateFormat("yyyyMM", Locale.US).format(month.getTime());
                String label = new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE")).format(month.getTime());
                boolean done = prefs.getBoolean("monthly_v2_" + getCollabId() + "_" + key + "_completed", false);
                addMonthHistoryRow(list, key, label, done);
                month.add(Calendar.MONTH, -1);
            }
            CollaboratorThemeHelper.applyReadableTypeface(list);
        }
        LinearLayout record = findViewById(R.id.collab_wellness_evolution_record);
        if (record != null) record.setVisibility(View.GONE);
        boolean ryffDone = prefs.getBoolean("ryff_demo_" + getCollabId() + "_completed", false);
        setText(R.id.collab_wellness_evolution_ryff, ryffDone
                ? "Ryff (ejemplo visual): completaste tres ítems de prueba. Los resultados por dimensión estarán disponibles solo cuando exista una versión validada; no se han calculado puntuaciones."
                : "Ryff: espacio reservado para resultados personales por dimensión de una versión validada. No hay puntuaciones psicológicas en esta demostración.");
        renderRyffChartPlaceholder();
        setText(R.id.collab_wellness_btn_legacy_weekly, "Abrir evaluación semanal anterior (demo)");
        if (isMonthlyCompleted()) showMonthlyRecord(monthKey(), monthLabel());
        scrollToTop();
    }

    private void showResultScreen(String key, String label) {
        if (key == null || !key.matches("\\d{6}")) {
            showEvolutionScreen();
            return;
        }
        String prefix = "monthly_v2_" + getCollabId() + "_" + key + "_";
        if (!prefs.getBoolean(prefix + "completed", false)) {
            showEvolutionScreen();
            return;
        }
        int[] answers = new int[MONTHLY_QUESTIONS.length];
        for (int i = 0; i < answers.length; i++) answers[i] = prefs.getInt(prefix + "q_" + i, 0);
        WellnessRecommendationRules.Result result;
        try {
            result = WellnessRecommendationRules.forMonthlyAnswers(answers, recommendationRepository.preferences());
        } catch (IllegalArgumentException incomplete) {
            CollaboratorBannerHelper.showBanner(this, "Registro incompleto",
                    "No se puede generar una orientación sin las diez respuestas guardadas.",
                    R.drawable.collab_ic_info_circle);
            showEvolutionScreen();
            return;
        }
        resultMonthKey = key;
        resultMonthLabel = label == null ? monthLabel() : label;
        recommendationsVisible = true;
        currentPhase = ScreenPhase.RESULT;
        updatePhaseVisibility();
        SessionManager session = new SessionManager(this);
        String fullName = session.getFullName().trim();
        if (fullName.equalsIgnoreCase(session.getUsername())) fullName = "";
        String firstName = fullName.isEmpty() ? "" : fullName.split("\\s+")[0];
        setText(R.id.collab_wellness_result_greeting,
                firstName == null || firstName.trim().isEmpty() ? "Hola, este espacio es para ti"
                        : "Hola, " + firstName.trim());
        if (firstName.isEmpty()) loadVerifiedGreeting(session);
        setText(R.id.collab_wellness_result_month, "Resumen de " + resultMonthLabel);
        setText(R.id.collab_wellness_result_summary, result.summary);
        int previousOverall = previousMonthlyOverall(key);
        setVisible(R.id.collab_wellness_result_professional_help,
                WellnessResponseAnalyzer.shouldSuggestProfessionalSupport(answers[9], previousOverall));
        setText(R.id.collab_wellness_result_positive, result.positives);
        setText(R.id.collab_wellness_result_attention, result.attention);
        Switch personalization = findViewById(R.id.collab_wellness_personalization_switch);
        personalization.setOnCheckedChangeListener(null);
        personalization.setChecked(recommendationRepository.isEnabled());
        personalization.setEnabled(new SessionManager(this).getWorkerId() > 0);
        personalization.setOnCheckedChangeListener((button, checked) -> {
            recommendationRepository.setEnabled(checked);
            showResultScreen(resultMonthKey, resultMonthLabel);
        });
        LinearLayout areas = findViewById(R.id.collab_wellness_result_areas);
        areas.removeAllViews();
        addAreaCard(areas, "Carga de tareas", answers[1]);
        addAreaCard(areas, "Comunicación", answers[3]);
        addAreaCard(areas, "Apoyo del equipo", answers[4]);
        addAreaCard(areas, "Trabajo y descanso", answers[5]);
        LinearLayout recommendations = findViewById(R.id.collab_wellness_result_recommendations);
        recommendations.removeAllViews();
        recommendations.setVisibility(View.VISIBLE);
        View recHeader = findViewById(R.id.collab_wellness_result_rec_header);
        if (recHeader != null) {
            recHeader.setVisibility(result.recommendations.isEmpty() ? View.GONE : View.VISIBLE);
        }
        int recIndex = 0;
        for (WellnessRecommendationRules.Recommendation suggestion : result.recommendations) {
            addRecommendationCard(recommendations, suggestion, recIndex++);
        }
        CollaboratorThemeHelper.applyReadableTypeface(findViewById(R.id.collab_wellness_view_result));
        scrollToTop();
    }

    private void loadVerifiedGreeting(SessionManager session) {
        String bearer = session.getBearerToken();
        int workerId = session.getWorkerId();
        if (bearer.isEmpty() || workerId <= 0) return;
        ApiClient.getService().getProfile(bearer).enqueue(new Callback<ApiResponse<LoginResponseData>>() {
            @Override public void onResponse(Call<ApiResponse<LoginResponseData>> call,
                                             Response<ApiResponse<LoginResponseData>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (isFinishing() || isDestroyed() || currentPhase != ScreenPhase.RESULT
                        || !bearer.equals(new SessionManager(CollaboratorWellnessActivity.this).getBearerToken())) return;
                ApiResponse<LoginResponseData> body = response.body();
                LoginResponseData.WorkerInfo worker = body == null || body.getData() == null
                        ? null : body.getData().getWorker();
                if (!response.isSuccessful() || worker == null || worker.getIdTrabajador() != workerId) return;
                String name = worker.getNombreCompleto();
                if (name != null && !name.trim().isEmpty())
                    setText(R.id.collab_wellness_result_greeting, "Hola, " + name.trim().split("\\s+")[0]);
            }
            @Override public void onFailure(Call<ApiResponse<LoginResponseData>> call, Throwable error) {
                // The local result remains available without a network connection.
            }
        });
    }

    private void showRecommendations() {
        if (currentPhase != ScreenPhase.RESULT) return;
        recommendationsVisible = true;
        View recommendations = findViewById(R.id.collab_wellness_result_recommendations);
        recommendations.setVisibility(View.VISIBLE);
        ScrollView scroll = findViewById(R.id.collab_wellness_scroll);
        scroll.post(() -> scroll.smoothScrollTo(0, recommendations.getTop()));
    }

    private void addRecommendationHeading(LinearLayout container) {
        TextView heading = new TextView(this);
        heading.setText("Sugerencias para tu día a día");
        heading.setTextColor(getColor(R.color.collab_text_primary));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        heading.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        heading.setPadding(0, 0, 0, dp(12));
        container.addView(heading);
    }

    private void addAreaCard(LinearLayout container, String topic, int answer) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.collab_bg_card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(8);
        container.addView(card, cardParams);
        ImageView face = new ImageView(this);
        face.setImageResource(findMoodByScore(MONTHLY_MOOD_OPTIONS, answer).iconRes);
        face.setContentDescription(answerLabel(EvaluationType.MONTHLY, answer));
        card.addView(face, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView title = new TextView(this);
        title.setText(topic + "  ·  " + answerLabel(EvaluationType.MONTHLY, answer));
        title.setTextColor(getColor(R.color.collab_text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        title.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        titleParams.setMarginStart(dp(12));
        card.addView(title, titleParams);
    }

    private void addRecommendationCard(LinearLayout container,
                                       WellnessRecommendationRules.Recommendation suggestion,
                                       int index) {
        View card = WellnessRecommendationCardBinder.inflateAndBind(
                container, suggestion, suggestion.action, index);
        LinearLayout extraSlot = WellnessRecommendationCardBinder.extraSlot(card);
        if (extraSlot == null) return;
        extraSlot.setVisibility(View.VISIBLE);

        TextView detailsToggle = new TextView(this);
        detailsToggle.setText("Ver detalles");
        detailsToggle.setTextColor(getColor(R.color.collab_purple_accent));
        detailsToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        detailsToggle.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        detailsToggle.setPadding(0, dp(9), 0, dp(2));
        detailsToggle.setClickable(true);
        detailsToggle.setFocusable(true);
        extraSlot.addView(detailsToggle);
        LinearLayout extra = new LinearLayout(this);
        extra.setOrientation(LinearLayout.VERTICAL);
        extra.setVisibility(View.GONE);
        extraSlot.addView(extra);
        detailsToggle.setOnClickListener(v -> {
            boolean open = extra.getVisibility() != View.VISIBLE;
            extra.setVisibility(open ? View.VISIBLE : View.GONE);
            detailsToggle.setText(open ? "Ocultar detalles" : "Ver detalles");
        });

        TextView detail = new TextView(this);
        detail.setText(suggestion.detail);
        detail.setTextColor(getColor(R.color.collab_text_secondary));
        detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        detail.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.NORMAL));
        detail.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        detailParams.topMargin = dp(6);
        extra.addView(detail, detailParams);
        TextView reason = new TextView(this);
        reason.setText(suggestion.reason);
        reason.setTextColor(getColor(R.color.collab_purple_accent));
        reason.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        reason.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.NORMAL));
        LinearLayout.LayoutParams reasonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reasonParams.topMargin = dp(8);
        extra.addView(reason, reasonParams);
        TextView source = new TextView(this);
        source.setText("Fuente: " + suggestion.source.institution + " · " + suggestion.source.title);
        source.setTextColor(getColor(R.color.collab_text_muted));
        source.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        source.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.NORMAL));
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceParams.topMargin = dp(8);
        extra.addView(source, sourceParams);
        TextView sourceLink = new TextView(this);
        sourceLink.setText("¿En qué se basa esta recomendación?");
        sourceLink.setTextColor(getColor(R.color.collab_purple_accent));
        sourceLink.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sourceLink.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        sourceLink.setPadding(0, dp(7), 0, dp(5));
        sourceLink.setClickable(true);
        sourceLink.setFocusable(true);
        sourceLink.setContentDescription("Abrir fuente oficial: " + suggestion.source.institution
                + ", " + suggestion.source.title);
        sourceLink.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(suggestion.source.url)));
            } catch (ActivityNotFoundException unavailable) {
                CollaboratorBannerHelper.showBanner(this, "Enlace no disponible",
                        "No hay una aplicación para abrir la fuente oficial.", R.drawable.collab_ic_info_circle);
            }
        });
        extra.addView(sourceLink);
        TextView question = new TextView(this);
        question.setText("¿Te resultó útil?");
        question.setTextColor(getColor(R.color.collab_text_primary));
        question.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        question.setTypeface(CollaboratorThemeHelper.readableTypeface(Typeface.BOLD));
        LinearLayout.LayoutParams questionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        questionParams.topMargin = dp(12);
        extra.addView(question, questionParams);
        TextView yes = addRatingAction(extra, "Me resultó útil");
        TextView no = addRatingAction(extra, "No me resultó útil");
        int vote = recommendationRepository.vote(resultMonthKey, suggestion.id);
        if (vote == 1) yes.setText("✓ Me resultó útil");
        if (vote == -1) no.setText("✓ No me resultó útil");
        yes.setOnClickListener(v -> submitRating(suggestion, true, yes, no));
        no.setOnClickListener(v -> submitRating(suggestion, false, yes, no));
    }

    private TextView addRatingAction(LinearLayout container, String label) {
        TextView action = new TextView(this);
        action.setText(label);
        action.setTextColor(getColor(R.color.collab_purple_accent));
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        action.setPadding(0, dp(9), 0, dp(4));
        action.setClickable(true);
        action.setFocusable(true);
        container.addView(action);
        return action;
    }

    private void submitRating(WellnessRecommendationRules.Recommendation suggestion, boolean helpful,
                              TextView yes, TextView no) {
        if (!recommendationRepository.isEnabled()) {
            CollaboratorBannerHelper.showBanner(this, "Valoración opcional",
                    "Activa «Usar mis valoraciones» si deseas personalizar futuras sugerencias.",
                    R.drawable.collab_ic_info_circle);
            return;
        }
        if (recommendationRepository.rate(resultMonthKey, suggestion.id, helpful)) {
            yes.setText(helpful ? "✓ Me resultó útil" : "Me resultó útil");
            no.setText(helpful ? "No me resultó útil" : "✓ No me resultó útil");
            CollaboratorBannerHelper.showBanner(this, "Gracias por tu valoración",
                    "Se tendrá en cuenta en tus próximas sugerencias de este dispositivo.",
                    R.drawable.collab_ic_info_circle);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int previousMonthlyOverall(String key) {
        Calendar previous = Calendar.getInstance();
        previous.set(Integer.parseInt(key.substring(0, 4)), Integer.parseInt(key.substring(4, 6)) - 1, 1);
        previous.add(Calendar.MONTH, -1);
        String previousKey = new SimpleDateFormat("yyyyMM", Locale.US).format(previous.getTime());
        String prefix = "monthly_v2_" + getCollabId() + "_" + previousKey + "_";
        return prefs.getBoolean(prefix + "completed", false) ? prefs.getInt(prefix + "q_9", 0) : 0;
    }

    private void showMonthlyRecord(String key, String label) {
        LinearLayout record = findViewById(R.id.collab_wellness_evolution_record);
        if (record == null) return;
        record.removeAllViews();
        String prefix = "monthly_v2_" + getCollabId() + "_" + key + "_";
        if (!prefs.getBoolean(prefix + "completed", false)) {
            record.setVisibility(View.GONE);
            return;
        }
        record.setVisibility(View.VISIBLE);
        addRecordText(record, "Tu registro de " + label, true);
        addRecordText(record, "Respuestas de demostración guardadas solo en este dispositivo. No son puntuaciones psicológicas.", false);
        for (int i = 0; i < MONTHLY_QUESTIONS.length; i++) {
            int answer = prefs.getInt(prefix + "q_" + i, 0);
            addRecordText(record, (i + 1) + ". " + MONTHLY_QUESTIONS[i]
                    + "\n" + answerLabel(EvaluationType.MONTHLY, answer), false);
        }
        TextView openResult = new TextView(this);
        openResult.setText("Ver mi resultado orientativo");
        openResult.setGravity(Gravity.CENTER);
        openResult.setTextColor(Color.WHITE);
        openResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        openResult.setBackgroundResource(R.drawable.collab_bg_chip_selected);
        openResult.setMinimumHeight((int) (48 * getResources().getDisplayMetrics().density));
        openResult.setOnClickListener(v -> showResultScreen(key, label));
        record.addView(openResult);
        CollaboratorThemeHelper.applyReadableTypeface(record);
    }

    private void addRecordText(LinearLayout record, String text, boolean heading) {
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(getColor(heading ? R.color.collab_text_primary : R.color.collab_text_secondary));
        item.setTextSize(TypedValue.COMPLEX_UNIT_SP, heading ? 16 : 13);
        if (heading) item.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (record.getChildCount() > 0) params.topMargin = (int) (10 * getResources().getDisplayMetrics().density);
        item.setLayoutParams(params);
        record.addView(item);
    }

    private void renderRyffChartPlaceholder() {
        LinearLayout chart = findViewById(R.id.collab_wellness_ryff_chart_placeholder);
        if (chart == null) return;
        chart.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        TextView heading = new TextView(this);
        heading.setText("Dimensiones de Ryff • visualización futura");
        heading.setTextColor(getColor(R.color.collab_text_primary));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        chart.addView(heading);
        for (String dimension : RYFF_SIX_DIMENSIONS) {
            TextView label = new TextView(this);
            label.setText(dimension + "  •  pendiente");
            label.setTextColor(getColor(R.color.collab_text_secondary));
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = (int) (10 * density);
            label.setLayoutParams(labelParams);
            chart.addView(label);
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(0);
            bar.setProgressTintList(ColorStateList.valueOf(getColor(R.color.collab_border)));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (4 * density));
            barParams.topMargin = (int) (4 * density);
            bar.setLayoutParams(barParams);
            chart.addView(bar);
        }
        CollaboratorThemeHelper.applyReadableTypeface(chart);
    }

    private void addMonthHistoryRow(LinearLayout list, String key, String label, boolean completed) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.collab_bg_card);
        card.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = (int) (9 * density);
        card.setLayoutParams(params);
        TextView title = new TextView(this);
        title.setText(label.substring(0, 1).toUpperCase(new Locale("es", "PE")) + label.substring(1)
                + "  •  " + (completed ? "Completada en este dispositivo" : "Sin registro local"));
        title.setTextColor(getColor(R.color.collab_text_primary));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        card.addView(title);
        if (completed) {
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> showMonthlyRecord(key, label));
        }
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(completed ? 100 : 0);
        bar.setProgressTintList(ColorStateList.valueOf(getColor(R.color.collab_purple_primary)));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (5 * density));
        barParams.topMargin = (int) (10 * density);
        bar.setLayoutParams(barParams);
        card.addView(bar);
        list.addView(card);
    }

    private String monthKey() {
        return new SimpleDateFormat("yyyyMM", Locale.US).format(new Date());
    }

    private String monthLabel() {
        return new SimpleDateFormat("MMMM yyyy", new Locale("es", "PE")).format(new Date());
    }

    private String monthlyPrefix() {
        return "monthly_v2_" + getCollabId() + "_" + monthKey() + "_";
    }

    private boolean isMonthlyCompleted() {
        return prefs.getBoolean(monthlyPrefix() + "completed", false);
    }

    private String ryffPrefix() {
        return "ryff_demo_" + getCollabId() + "_";
    }

    private boolean isRyffDemoAvailable() {
        // No assignment endpoint exists yet. A debug build must not self-assign a psychological test.
        return false;
    }

    private void startEvaluationFlow(EvaluationType type) {
        if (type == EvaluationType.RYFF_DEMO && !isRyffDemoAvailable()) return;
        if (type == EvaluationType.MONTHLY && isMonthlyCompleted()) {
            showEvolutionScreen();
            return;
        }
        activeEvalType = type;
        currentQuestionIndex = 0;
        draftAnswers = new int[type.totalQuestions];

        String prefix = type == EvaluationType.WEEKLY ? "wk_" + getCollabId() + "_"
                : type == EvaluationType.MONTHLY ? monthlyPrefix() : ryffPrefix();
        if (prefs.getBoolean(prefix + "draft", false)
                || (type == EvaluationType.WEEKLY && prefs.getBoolean(prefix + "completed", false))) {
            for (int i = 0; i < type.totalQuestions; i++) {
                draftAnswers[i] = prefs.getInt(prefix + "q_" + i, 0);
            }
        }

        currentPhase = ScreenPhase.STEP_BY_STEP;
        updatePhaseVisibility();
        renderCurrentQuestionStep();
        scrollToTop();
    }

    private void saveDraft() {
        String prefix = activeEvalType == EvaluationType.WEEKLY ? "wk_" + getCollabId() + "_"
                : activeEvalType == EvaluationType.MONTHLY ? monthlyPrefix() : ryffPrefix();
        SharedPreferences.Editor editor = prefs.edit().putBoolean(prefix + "draft", true);
        for (int i = 0; i < draftAnswers.length; i++) editor.putInt(prefix + "q_" + i, draftAnswers[i]);
        editor.apply();
        CollaboratorBannerHelper.showBanner(this, "Borrador guardado",
                "Puedes continuar desde este dispositivo. No se envió al servidor.", R.drawable.collab_ic_heart_pulse);
        showOverviewScreen();
    }

    private void renderCurrentQuestionStep() {
        String[] dimensions = dimensionsFor(activeEvalType);
        String[] questions = questionsFor(activeEvalType);
        int total = activeEvalType.totalQuestions;
        int stepNum = currentQuestionIndex + 1;
        int pct = (int) ((stepNum * 100f) / total);

        setText(R.id.collab_wellness_step_tv_type, activeEvalType.title);
        setText(R.id.collab_wellness_step_tv_counter, "Pregunta " + stepNum + " de " + total);
        setText(R.id.collab_wellness_step_tv_dimension,
                (activeEvalType == EvaluationType.RYFF_DEMO ? "Ejemplo visual · " : "Tema · ")
                        + dimensions[currentQuestionIndex]);
        setText(R.id.collab_wellness_step_tv_question, questions[currentQuestionIndex]);

        ProgressBar pb = findViewById(R.id.collab_wellness_step_pb);
        if (pb != null) {
            pb.setMax(100);
            pb.setProgress(pct);
        }

        TextView btnPrev = findViewById(R.id.collab_wellness_btn_prev);
        if (btnPrev != null) {
            btnPrev.setText(currentQuestionIndex == 0 ? "Cancelar" : "Anterior");
        }

        updateDemoNextButtonState(total);

        if (activeEvalType == EvaluationType.RYFF_DEMO) renderSimpleOptionCards();
        else renderMoodOptionCards(activeEvalType == EvaluationType.MONTHLY ? MONTHLY_MOOD_OPTIONS : MOOD_OPTIONS);
        scrollToTop();
    }

    private void updateDemoNextButtonState(int total) {
        TextView btnNext = findViewById(R.id.collab_wellness_btn_next);
        if (btnNext == null) return;
        boolean hasSelection = draftAnswers[currentQuestionIndex] > 0;
        btnNext.setText(currentQuestionIndex == total - 1 ? "Revisar respuestas" : "Siguiente");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(14f * getResources().getDisplayMetrics().density);
        bg.setColor(getColor(hasSelection ? R.color.collab_wellness_violet_primary : R.color.collab_wellness_btn_disabled_bg));
        btnNext.setBackground(bg);
        btnNext.setTextColor(hasSelection ? Color.WHITE : getColor(R.color.collab_wellness_btn_disabled_text));
    }

    private String[] dimensionsFor(EvaluationType type) {
        return type == EvaluationType.WEEKLY ? WEEKLY_DIMENSIONS
                : type == EvaluationType.MONTHLY ? MONTHLY_DIMENSIONS : RYFF_DIMENSIONS;
    }

    private String[] questionsFor(EvaluationType type) {
        return type == EvaluationType.WEEKLY ? WEEKLY_QUESTIONS
                : type == EvaluationType.MONTHLY ? MONTHLY_QUESTIONS : RYFF_EXAMPLE_QUESTIONS;
    }

    private String answerLabel(EvaluationType type, int value) {
        if (type == EvaluationType.WEEKLY) return findMoodByScore(value).label;
        String[] options = type == EvaluationType.MONTHLY ? MONTHLY_OPTIONS : RYFF_OPTIONS;
        return value >= 1 && value <= options.length ? options[value - 1] : "Sin respuesta";
    }

    private void renderSimpleOptionCards() {
        LinearLayout container = findViewById(R.id.collab_wellness_options_container);
        if (container == null) return;
        String[] options = activeEvalType == EvaluationType.MONTHLY ? MONTHLY_OPTIONS : RYFF_OPTIONS;
        java.util.List<WellnessOptionCardBinder.OptionItem> items = new java.util.ArrayList<>(options.length);
        for (int i = 0; i < options.length; i++) {
            items.add(new WellnessOptionCardBinder.OptionItem(i + 1, options[i], ""));
        }
        WellnessOptionCardBinder.bind(container, items, draftAnswers[currentQuestionIndex], value -> {
            draftAnswers[currentQuestionIndex] = value;
            updateDemoNextButtonState(activeEvalType.totalQuestions);
        });
    }

    private void renderMoodOptionCards(MoodOption[] options) {
        LinearLayout container = findViewById(R.id.collab_wellness_options_container);
        if (container == null) return;
        java.util.List<WellnessOptionCardBinder.OptionItem> items = new java.util.ArrayList<>(options.length);
        for (MoodOption option : options) {
            items.add(new WellnessOptionCardBinder.OptionItem(option.scoreValue, option.label, option.subtitle));
        }
        WellnessOptionCardBinder.bind(container, items, draftAnswers[currentQuestionIndex], score -> {
            draftAnswers[currentQuestionIndex] = score;
            updateDemoNextButtonState(activeEvalType.totalQuestions);
        });
    }

    private void showReviewScreen() {
        currentPhase = ScreenPhase.REVIEW;
        updatePhaseVisibility();

        setText(R.id.collab_wellness_review_tv_badge,
                activeEvalType.title.toUpperCase(Locale.ROOT) + " • REVISIÓN PREVIA");
        setText(R.id.collab_wellness_review_tv_title,
                "Verifica tus " + activeEvalType.totalQuestions + " respuestas antes de confirmar");

        LinearLayout container = findViewById(R.id.collab_wellness_review_list_container);
        if (container != null) {
            container.removeAllViews();
            String[] dimensions = dimensionsFor(activeEvalType);
            String[] questions = questionsFor(activeEvalType);

            float density = getResources().getDisplayMetrics().density;
            int pad14 = (int) (14 * density);
            int margin10 = (int) (10 * density);

            for (int i = 0; i < activeEvalType.totalQuestions; i++) {
                final int stepIdx = i;
                MoodOption chosen = activeEvalType == EvaluationType.RYFF_DEMO ? null
                        : findMoodByScore(activeEvalType == EvaluationType.MONTHLY
                        ? MONTHLY_MOOD_OPTIONS : MOOD_OPTIONS, draftAnswers[i]);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackgroundResource(R.drawable.collab_bg_card);
                row.setPadding(pad14, (int) (12 * density), pad14, (int) (12 * density));
                row.setClickable(true);
                row.setFocusable(true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = margin10;
                row.setLayoutParams(lp);

                ImageView ivMood = new ImageView(this);
                int iconSize = (int) (32 * density);
                ivMood.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
                if (chosen != null) ivMood.setImageResource(chosen.iconRes);
                else ivMood.setVisibility(View.GONE);
                row.addView(ivMood);

                LinearLayout col = new LinearLayout(this);
                col.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                colLp.setMarginStart((int) (12 * density));
                colLp.setMarginEnd((int) (8 * density));
                col.setLayoutParams(colLp);

                TextView tvDim = new TextView(this);
                tvDim.setText((i + 1) + ". " + dimensions[i]);
                tvDim.setTextColor(getColor(R.color.collab_text_primary));
                tvDim.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                tvDim.setTypeface(Typeface.DEFAULT_BOLD);
                col.addView(tvDim);

                TextView tvQ = new TextView(this);
                tvQ.setText(questions[i]);
                tvQ.setTextColor(getColor(R.color.collab_text_secondary));
                tvQ.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                col.addView(tvQ);

                TextView tvAns = new TextView(this);
                tvAns.setText("Respuesta: " + answerLabel(activeEvalType, draftAnswers[i]));
                tvAns.setTextColor(chosen == null ? getColor(R.color.collab_purple_accent)
                        : Color.parseColor(chosen.hexColor));
                tvAns.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                tvAns.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams ansLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ansLp.topMargin = (int) (3 * density);
                tvAns.setLayoutParams(ansLp);
                col.addView(tvAns);

                row.addView(col);

                TextView btnEditItem = new TextView(this);
                btnEditItem.setText("Editar");
                btnEditItem.setTextColor(getColor(R.color.collab_purple_accent));
                btnEditItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
                btnEditItem.setTypeface(Typeface.DEFAULT_BOLD);
                btnEditItem.setBackgroundResource(R.drawable.collab_bg_pill_purple);
                btnEditItem.setPadding((int) (10 * density), (int) (5 * density), (int) (10 * density), (int) (5 * density));
                row.addView(btnEditItem);

                row.setOnClickListener(v -> {
                    currentQuestionIndex = stepIdx;
                    currentPhase = ScreenPhase.STEP_BY_STEP;
                    updatePhaseVisibility();
                    renderCurrentQuestionStep();
                });

                container.addView(row);
            }
            CollaboratorThemeHelper.applyReadableTypeface(container);
        }
        scrollToTop();
    }

    private void saveAndCompleteEvaluation() {
        if (activeEvalType != EvaluationType.WEEKLY) {
            if (activeEvalType == EvaluationType.RYFF_DEMO && !isRyffDemoAvailable()) {
                showOverviewScreen();
                return;
            }
            String prefix = activeEvalType == EvaluationType.MONTHLY ? monthlyPrefix() : ryffPrefix();
            String dateStr = new SimpleDateFormat("dd MMM yyyy • HH:mm", new Locale("es", "PE"))
                    .format(new Date());
            SharedPreferences.Editor editor = prefs.edit().putBoolean(prefix + "completed", true)
                    .putBoolean(prefix + "draft", false).putString(prefix + "date", dateStr);
            for (int i = 0; i < activeEvalType.totalQuestions; i++) {
                if (draftAnswers[i] < 1 || draftAnswers[i] > 5) return;
                editor.putInt(prefix + "q_" + i, draftAnswers[i]);
            }
            editor.apply();
            setVisible(R.id.collab_wellness_btn_open_result, activeEvalType == EvaluationType.MONTHLY);
            ImageView completedIcon = findViewById(R.id.collab_wellness_completed_iv_mood);
            if (completedIcon != null) completedIcon.setImageResource(R.drawable.collaborator_ic_check_circle);
            setText(R.id.collab_wellness_completed_tv_title, "¡Gracias por compartir tu experiencia!");
            setText(R.id.collab_wellness_completed_tv_summary,
                    activeEvalType.title + " • " + activeEvalType.totalQuestions
                            + " respuestas guardadas localmente el " + dateStr);
            currentPhase = ScreenPhase.COMPLETED;
            updatePhaseVisibility();
            scrollToTop();
            return;
        }
        String collabId = getCollabId();
        String prefix = (activeEvalType == EvaluationType.WEEKLY ? "wk_" : "mo_") + collabId + "_";
        String dateStr = new SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(new Date());

        int sum = 0;
        SharedPreferences.Editor ed = prefs.edit();
        for (int i = 0; i < activeEvalType.totalQuestions; i++) {
            int val = draftAnswers[i] > 0 ? draftAnswers[i] : 4;
            ed.putInt(prefix + "q_" + i, val);
            sum += val;
        }
        float avg = sum / (float) activeEvalType.totalQuestions;
        ed.putBoolean(prefix + "completed", true);
        ed.putString(prefix + "date", dateStr);
        ed.putFloat(prefix + "avg", avg);
        ed.putFloat("last_avg_" + collabId, avg);
        ed.putString("last_type_" + collabId, activeEvalType.title);
        ed.putString("last_date_" + collabId, dateStr);
        ed.apply();

        ImageView ivCompletedMood = findViewById(R.id.collab_wellness_completed_iv_mood);
        if (ivCompletedMood != null) {
            ivCompletedMood.setImageResource(R.drawable.collaborator_ic_check_circle);
        }

        setText(R.id.collab_wellness_completed_tv_summary,
                activeEvalType.title + " completada (" + activeEvalType.totalQuestions + "/"
                        + activeEvalType.totalQuestions + " respuestas) • " + dateStr);

        currentPhase = ScreenPhase.COMPLETED;
        setVisible(R.id.collab_wellness_btn_open_result, false);
        updatePhaseVisibility();
        scrollToTop();
    }

    private void refreshOverviewSummary() {
        if (activeProfile != null) {
            setText(R.id.collab_wellness_tv_subtitle,
                    "Tu bienestar, nuestra prioridad");
        }
        ImageView ivStatusMood = findViewById(R.id.collab_wellness_iv_status_mood);
        if (ivStatusMood != null) {
            ivStatusMood.setVisibility(View.VISIBLE);
            ivStatusMood.setImageResource(R.drawable.collab_avatar_webi_robot_transparent);
        }
        TextView tvStatusTitle = findViewById(R.id.collab_wellness_tv_status_title);
        TextView tvStatusDesc = findViewById(R.id.collab_wellness_tv_status_desc);
        TextView tvRecs = findViewById(R.id.collab_wellness_tv_recommendations);
        if (tvStatusTitle != null) {
            tvStatusTitle.setText(isMonthlyCompleted() ? "Tu resumen está listo" : "Un espacio para ti");
            tvStatusTitle.setTextColor(getColor(R.color.collab_text_primary));
        }
        if (tvStatusDesc != null) tvStatusDesc.setText(isMonthlyCompleted()
                ? "Ya puedes revisar la orientación de " + monthLabel() + " basada en tus respuestas."
                : "Reflexiona sobre tu experiencia laboral a tu ritmo.");
        if (tvRecs != null) tvRecs.setText(isMonthlyCompleted()
                ? "Websito te acompaña con recomendaciones locales dentro de Bienestar."
                : "Completa la evaluación mensual para ver tus recomendaciones aquí.");
        setVisible(R.id.collab_wellness_btn_hero_result, isMonthlyCompleted());

        setText(R.id.collab_wellness_tv_month_period, monthLabel() + " • 3 a 5 minutos");
        boolean monthlyDraft = prefs.getBoolean(monthlyPrefix() + "draft", false);
        setText(R.id.collab_wellness_badge_monthly_state,
                isMonthlyCompleted() ? "Completada" : monthlyDraft ? "En proceso" : "Disponible");
        setText(R.id.collab_wellness_btn_start_monthly,
                isMonthlyCompleted() ? "Ver mi resultado" : monthlyDraft ? "Continuar evaluación" : "Comenzar evaluación");

        boolean ryffCompleted = prefs.getBoolean(ryffPrefix() + "completed", false);
        boolean ryffAvailable = isRyffDemoAvailable();
        String ryffState = ryffCompleted ? "Ejemplo completado (demo)" : "Solo por invitación";
        setText(R.id.collab_wellness_badge_ryff_state, ryffState);
        TextView ryffDeadline = findViewById(R.id.collab_wellness_tv_ryff_deadline);
        if (ryffDeadline != null) {
            ryffDeadline.setVisibility(View.GONE);
        }
        setText(R.id.collab_wellness_btn_open_ryff,
                ryffAvailable ? "Ver invitación de prueba" : "Esta evaluación requiere invitación");
        View ryffButton = findViewById(R.id.collab_wellness_btn_open_ryff);
        if (ryffButton != null) {
            ryffButton.setEnabled(ryffAvailable);
            ryffButton.setAlpha(ryffAvailable ? 1f : 0.65f);
        }
    }

    private void updatePhaseVisibility() {
        setVisible(R.id.collab_wellness_view_overview, currentPhase == ScreenPhase.OVERVIEW);
        setVisible(R.id.collab_wellness_view_intro, currentPhase == ScreenPhase.INTRO);
        setVisible(R.id.collab_wellness_view_step, currentPhase == ScreenPhase.STEP_BY_STEP);
        setVisible(R.id.collab_wellness_view_review, currentPhase == ScreenPhase.REVIEW);
        setVisible(R.id.collab_wellness_view_completed, currentPhase == ScreenPhase.COMPLETED);
        setVisible(R.id.collab_wellness_view_result, currentPhase == ScreenPhase.RESULT);
        setVisible(R.id.collab_wellness_view_evolution, currentPhase == ScreenPhase.EVOLUTION);
        setVisible(R.id.collab_wellness_step_footer, currentPhase == ScreenPhase.STEP_BY_STEP);
    }

    private void setVisible(int viewId, boolean visible) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private MoodOption findMoodByScore(int score) {
        return findMoodByScore(MOOD_OPTIONS, score);
    }

    private MoodOption findMoodByScore(MoodOption[] options, int score) {
        for (MoodOption option : options) {
            if (option.scoreValue == score) return option;
        }
        return options[2];
    }

    private MoodOption resolveOverallMood(float avg) {
        int rounded = Math.max(1, Math.min(5, Math.round(avg)));
        return findMoodByScore(rounded);
    }

    private String getCollabId() {
        return activeProfile != null ? activeProfile.getId() : "anonymous";
    }

    private void scrollToTop() {
        ScrollView sv = findViewById(R.id.collab_wellness_scroll);
        if (sv != null) {
            sv.post(() -> sv.smoothScrollTo(0, 0));
        }
    }

    private void setText(int viewId, String text) {
        TextView tv = findViewById(viewId);
        if (tv != null && text != null) {
            tv.setText(text);
        }
    }
}
