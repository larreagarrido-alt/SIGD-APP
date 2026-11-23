package pe.net.ws.kpiweb.features.admin.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.gson.JsonObject;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Dedicated administrative form for creating an official KPI definition. */
public final class AdminKpiConfigureActivity extends AppCompatActivity {
    public static final String EXTRA_RESULT_MESSAGE = "kpi_cfg_result_message";
    private static final long ANIM_MS = 180L;

    private NestedScrollView scrollView;
    private View errorBanner;
    private TextView errorText;

    private EditText codigoInput, nombreInput, descripcionInput;
    private EditText unidadInput, metaInput, pesoInput, puntosInput;
    private EditText categoriaInput, cargoInput, responsableInput;

    private View metricNumerico, metricPorcentaje, metricBooleano, metricEscala;
    private View metricNumericoCheck, metricPorcentajeCheck, metricBooleanoCheck, metricEscalaCheck;
    private TextView metricNumericoText, metricPorcentajeText, metricBooleanoText, metricEscalaText;

    private FrameLayout periodSegmented;
    private View periodIndicator;
    private TextView periodDiaria, periodSemanal, periodMensual;

    private View cancelBtn, saveBtn;
    private ProgressBar saveProgress;
    private ImageView saveIcon;
    private TextView saveLabel;

    private String selectedMetric = "NUMERICO";
    private String selectedPeriod = "MENSUAL";
    private boolean submitting = false;

    private Call<ApiResponse<JsonObject>> saveCall;
    private String token;
    private int userId;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        SessionManager session = new SessionManager(this);
        token = session.getBearerToken();
        userId = session.getUserId();

        setContentView(R.layout.admin_activity_kpi_configure);
        getWindow().setStatusBarColor(getColor(R.color.admin_bg_main));
        getWindow().setNavigationBarColor(getColor(R.color.admin_bg_drawer));

        scrollView = findViewById(R.id.kpi_cfg_scroll);
        errorBanner = findViewById(R.id.kpi_cfg_error_banner);
        errorText = findViewById(R.id.kpi_cfg_error_text);

        codigoInput = findViewById(R.id.kpi_cfg_codigo);
        nombreInput = findViewById(R.id.kpi_cfg_nombre);
        descripcionInput = findViewById(R.id.kpi_cfg_descripcion);
        unidadInput = findViewById(R.id.kpi_cfg_unidad);
        metaInput = findViewById(R.id.kpi_cfg_meta);
        pesoInput = findViewById(R.id.kpi_cfg_peso);
        puntosInput = findViewById(R.id.kpi_cfg_puntos);
        categoriaInput = findViewById(R.id.kpi_cfg_categoria);
        cargoInput = findViewById(R.id.kpi_cfg_cargo);
        responsableInput = findViewById(R.id.kpi_cfg_responsable);

        metricNumerico = findViewById(R.id.kpi_cfg_metric_numerico);
        metricPorcentaje = findViewById(R.id.kpi_cfg_metric_porcentaje);
        metricBooleano = findViewById(R.id.kpi_cfg_metric_booleano);
        metricEscala = findViewById(R.id.kpi_cfg_metric_escala);

        metricNumericoCheck = findViewById(R.id.kpi_cfg_metric_numerico_check);
        metricPorcentajeCheck = findViewById(R.id.kpi_cfg_metric_porcentaje_check);
        metricBooleanoCheck = findViewById(R.id.kpi_cfg_metric_booleano_check);
        metricEscalaCheck = findViewById(R.id.kpi_cfg_metric_escala_check);

        metricNumericoText = findViewById(R.id.kpi_cfg_metric_numerico_text);
        metricPorcentajeText = findViewById(R.id.kpi_cfg_metric_porcentaje_text);
        metricBooleanoText = findViewById(R.id.kpi_cfg_metric_booleano_text);
        metricEscalaText = findViewById(R.id.kpi_cfg_metric_escala_text);

        periodSegmented = findViewById(R.id.kpi_cfg_period_segmented);
        periodIndicator = findViewById(R.id.kpi_cfg_period_indicator);
        periodDiaria = findViewById(R.id.kpi_cfg_period_diaria);
        periodSemanal = findViewById(R.id.kpi_cfg_period_semanal);
        periodMensual = findViewById(R.id.kpi_cfg_period_mensual);

        cancelBtn = findViewById(R.id.kpi_cfg_btn_cancel);
        saveBtn = findViewById(R.id.kpi_cfg_btn_save);
        saveProgress = findViewById(R.id.kpi_cfg_save_progress);
        saveIcon = findViewById(R.id.kpi_cfg_save_icon);
        saveLabel = findViewById(R.id.kpi_cfg_save_label);

        findViewById(R.id.kpi_cfg_back).setOnClickListener(v -> {
            if (!submitting) finish();
        });
        cancelBtn.setOnClickListener(v -> {
            if (!submitting) finish();
        });
        saveBtn.setOnClickListener(v -> attemptSave());

        setupMetricSelector();
        setupPeriodSelector();
        attachErrorClearWatchers();

        updateMetricUI();
        updatePeriodUI(false);
        AdminTypography.apply(this, findViewById(android.R.id.content));
    }

    @Override protected void onStop() {
        if (saveCall != null) {
            saveCall.cancel();
            saveCall = null;
        }
        super.onStop();
    }

    private void setupMetricSelector() {
        metricNumerico.setOnClickListener(v -> selectMetric("NUMERICO"));
        metricPorcentaje.setOnClickListener(v -> showError("La app calcula el porcentaje desde una cantidad y su meta.", null));
        metricBooleano.setOnClickListener(v -> showError("Para puntos oficiales elige una cantidad medible.", null));
        metricEscala.setOnClickListener(v -> showError("Para puntos oficiales elige una cantidad medible.", null));
        metricPorcentaje.setAlpha(0.45f);
        metricBooleano.setAlpha(0.45f);
        metricEscala.setAlpha(0.45f);
    }

    private void selectMetric(String metric) {
        if (submitting || selectedMetric.equals(metric)) return;
        selectedMetric = metric;
        updateMetricUI();
    }

    private void updateMetricUI() {
        styleMetricOption(metricNumerico, metricNumericoCheck, metricNumericoText, "NUMERICO".equals(selectedMetric));
        styleMetricOption(metricPorcentaje, metricPorcentajeCheck, metricPorcentajeText, "PORCENTAJE".equals(selectedMetric));
        styleMetricOption(metricBooleano, metricBooleanoCheck, metricBooleanoText, "BOOLEANO".equals(selectedMetric));
        styleMetricOption(metricEscala, metricEscalaCheck, metricEscalaText, "ESCALA_1_5".equals(selectedMetric));
    }

    private void styleMetricOption(View container, View check, TextView text, boolean selected) {
        container.setBackgroundResource(selected
                ? R.drawable.admin_bg_ranking_chip_active
                : R.drawable.admin_bg_ranking_chip_inactive);
        container.setSelected(selected);
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
        text.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
        text.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        AdminTypography.apply(this, text);
    }

    private void setupPeriodSelector() {
        periodDiaria.setOnClickListener(v -> selectPeriod("DIARIO"));
        periodSemanal.setOnClickListener(v -> selectPeriod("SEMANAL"));
        periodMensual.setOnClickListener(v -> selectPeriod("MENSUAL"));
        periodSegmented.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if ((r - l) != (or - ol) && (r - l) > 0) {
                positionPeriodIndicator(false);
            }
        });
    }

    private void selectPeriod(String period) {
        if (submitting || selectedPeriod.equals(period)) return;
        selectedPeriod = period;
        updatePeriodUI(true);
    }

    private void updatePeriodUI(boolean animate) {
        styleSegment(periodDiaria, "DIARIO".equals(selectedPeriod));
        styleSegment(periodSemanal, "SEMANAL".equals(selectedPeriod));
        styleSegment(periodMensual, "MENSUAL".equals(selectedPeriod));
        periodSegmented.post(() -> positionPeriodIndicator(animate));
    }

    private void styleSegment(TextView segment, boolean selected) {
        segment.setSelected(selected);
        segment.setTextColor(selected ? Color.WHITE : getColor(R.color.admin_text_secondary));
        segment.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        AdminTypography.apply(this, segment);
    }

    private void positionPeriodIndicator(boolean animate) {
        int innerWidth = periodSegmented.getWidth()
                - periodSegmented.getPaddingLeft()
                - periodSegmented.getPaddingRight();
        if (innerWidth <= 0) return;
        int segmentWidth = Math.round(innerWidth / 3f);
        int index = "DIARIO".equals(selectedPeriod) ? 0 : "SEMANAL".equals(selectedPeriod) ? 1 : 2;
        float targetX = index * (innerWidth / 3f);

        ViewGroup.LayoutParams lp = periodIndicator.getLayoutParams();
        if (lp.width != segmentWidth) {
            lp.width = segmentWidth;
            periodIndicator.setLayoutParams(lp);
        }
        if (animate) {
            periodIndicator.animate()
                    .translationX(targetX)
                    .setDuration(ANIM_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            periodIndicator.animate().cancel();
            periodIndicator.setTranslationX(targetX);
        }
    }

    private void attachErrorClearWatchers() {
        EditText[] all = {
                codigoInput, nombreInput, descripcionInput,
                unidadInput, metaInput, pesoInput, puntosInput,
                categoriaInput, cargoInput, responsableInput
        };
        for (EditText input : all) {
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (input.isActivated()) input.setActivated(false);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private boolean sameAccount() {
        SessionManager current = new SessionManager(this);
        return current.isLoggedIn() && current.getUserId() == userId && token.equals(current.getBearerToken());
    }

    private void showError(String message, @Nullable EditText focusField) {
        errorText.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
        if (focusField != null) {
            focusField.setActivated(true);
            focusField.setError(message);
            focusField.requestFocus();
        }
        scrollView.smoothScrollTo(0, 0);
    }

    private void setSubmitting(boolean active) {
        submitting = active;
        saveBtn.setEnabled(!active);
        cancelBtn.setEnabled(!active);
        cancelBtn.setAlpha(active ? 0.5f : 1f);
        saveProgress.setVisibility(active ? View.VISIBLE : View.GONE);
        saveIcon.setVisibility(active ? View.GONE : View.VISIBLE);
        saveLabel.setText(active ? "Guardando…" : "Guardar indicador");
    }

    private void attemptSave() {
        if (submitting) return;
        errorBanner.setVisibility(View.GONE);

        EditText[] required = {
                codigoInput, nombreInput, descripcionInput,
                unidadInput, metaInput, pesoInput, puntosInput,
                categoriaInput, cargoInput, responsableInput
        };
        for (EditText field : required) {
            field.setActivated(false);
            field.setError(null);
            if (field.getText().toString().trim().isEmpty()) {
                showError("Completa todos los campos obligatorios antes de guardar.", field);
                return;
            }
        }

        if (!sameAccount()) {
            showError("La cuenta cambió. Vuelve a iniciar sesión.", null);
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("codigo", codigoInput.getText().toString().trim());
        body.addProperty("nombre", nombreInput.getText().toString().trim());
        body.addProperty("descripcion", descripcionInput.getText().toString().trim());
        body.addProperty("id_categoria", categoriaInput.getText().toString().trim());
        body.addProperty("id_cargo_objetivo", cargoInput.getText().toString().trim());
        body.addProperty("responsable_revision", responsableInput.getText().toString().trim());
        body.addProperty("tipo_metrica", selectedMetric);
        body.addProperty("unidad_medida", unidadInput.getText().toString().trim());
        body.addProperty("meta_esperada", metaInput.getText().toString().trim());
        body.addProperty("peso", pesoInput.getText().toString().trim());
        body.addProperty("puntos_base", puntosInput.getText().toString().trim());
        body.addProperty("periodicidad", selectedPeriod);

        setSubmitting(true);
        saveCall = ApiClient.getService().createOfficialKpiDefinition(token, body);
        saveCall.enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (response.errorBody() != null) response.errorBody().close();
                if (call != saveCall || call.isCanceled() || isFinishing()) return;
                saveCall = null;
                setSubmitting(false);

                ApiResponse<JsonObject> result = response.body();
                if (response.isSuccessful() && result != null && result.isSuccessful() && result.getData() != null) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_RESULT_MESSAGE,
                            "Indicador guardado como pendiente. Apruébalo después de verificar sus reglas.");
                    setResult(RESULT_OK, data);
                    finish();
                    return;
                }

                String serverMsg = result != null && result.getMessage() != null && !result.getMessage().trim().isEmpty()
                        ? result.getMessage().trim()
                        : null;
                if (response.code() == 403) {
                    showError("Tu cuenta no tiene permiso para configurar indicadores oficiales.", null);
                } else if (response.code() == 401) {
                    showError("Sesión vencida. Vuelve a iniciar sesión.", null);
                } else if (serverMsg != null) {
                    showError(serverMsg, null);
                } else {
                    showError("No se pudo guardar el indicador (HTTP " + response.code() + "). Verifica los datos e intenta nuevamente.", null);
                }
            }

            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                if (call != saveCall || call.isCanceled() || isFinishing()) return;
                saveCall = null;
                setSubmitting(false);
                showError("Sin conexión con el servidor. Verifica tu red e intenta nuevamente.", null);
            }
        });
    }
}
