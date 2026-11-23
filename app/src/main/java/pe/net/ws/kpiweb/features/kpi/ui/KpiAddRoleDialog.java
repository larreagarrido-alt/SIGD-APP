package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.admin.data.KpiCustomRepository;
import pe.net.ws.kpiweb.features.kpi.admin.model.KpiDefinition;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig;
import pe.net.ws.kpiweb.features.kpi.engine.KpiMeasurementType;
import pe.net.ws.kpiweb.features.kpi.engine.KpiPolarity;

/**
 * Creador Dinámico del Licenciado para configurar nuevos cargos con sus PROPIOS KPIs,
 * diferenciado según sea:
 * - 👔 COLABORADOR (producción autónoma, entregables finales, salidas a campo/locación).
 * - ⚡ PRACTICANTE (metas formativas graduales, piezas de apoyo y supervisión).
 */
public final class KpiAddRoleDialog {

    public interface OnRoleCreatedListener {
        void onRoleCreated(KpiRoleProfileConfig.CustomRoleEntry entry);
    }

    private KpiAddRoleDialog() {}

    public static void show(@NonNull Context context,
                            boolean defaultIsPracticante,
                            @NonNull OnRoleCreatedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        boolean isDark = KpiThemeManager.isDarkMode(context);
        final boolean[] isPracticanteSelected = {defaultIsPracticante};

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 18);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(context, 22));
        cardBg.setColor(ContextCompat.getColor(context, R.color.kpi_card_bg));
        cardBg.setStroke(dp(context, 2), ContextCompat.getColor(context, R.color.kpi_primary));
        root.setBackground(cardBg);
        scroll.addView(root);

        TextView tvTitle = new TextView(context);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_primary));
        root.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setTextSize(12f);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_secondary));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(context, 4);
        subLp.bottomMargin = dp(context, 12);
        tvSub.setLayoutParams(subLp);
        root.addView(tvSub);

        // Selector Superior: Practicante vs Colaborador
        LinearLayout rowType = new LinearLayout(context);
        rowType.setOrientation(LinearLayout.HORIZONTAL);
        rowType.setWeightSum(2f);

        TextView btnTypePracticante = new TextView(context);
        btnTypePracticante.setText("Modo Practicante\n(Formativo / Apoyo)");
        btnTypePracticante.setGravity(Gravity.CENTER);
        btnTypePracticante.setTextSize(12f);
        btnTypePracticante.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, dp(context, 48), 1f);
        lpLeft.rightMargin = dp(context, 6);
        btnTypePracticante.setLayoutParams(lpLeft);

        TextView btnTypeColaborador = new TextView(context);
        btnTypeColaborador.setText("Modo Colaborador\n(Producción / Campo)");
        btnTypeColaborador.setGravity(Gravity.CENTER);
        btnTypeColaborador.setTextSize(12f);
        btnTypeColaborador.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(0, dp(context, 48), 1f);
        lpRight.leftMargin = dp(context, 6);
        btnTypeColaborador.setLayoutParams(lpRight);

        rowType.addView(btnTypePracticante);
        rowType.addView(btnTypeColaborador);
        root.addView(rowType);

        // Barra de Plantillas Rápidas para el Licenciado
        TextView tvPresetsLbl = new TextView(context);
        tvPresetsLbl.setText("PLANTILLAS SUGERIDAS (1 TOQUE):");
        tvPresetsLbl.setTextSize(11f);
        tvPresetsLbl.setTypeface(Typeface.DEFAULT_BOLD);
        tvPresetsLbl.setTextColor(ContextCompat.getColor(context, R.color.kpi_primary));
        LinearLayout.LayoutParams plLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plLp.topMargin = dp(context, 10);
        plLp.bottomMargin = dp(context, 4);
        tvPresetsLbl.setLayoutParams(plLp);
        root.addView(tvPresetsLbl);

        HorizontalScrollView hsvPresets = new HorizontalScrollView(context);
        hsvPresets.setHorizontalScrollBarEnabled(false);
        LinearLayout presetsContainer = new LinearLayout(context);
        presetsContainer.setOrientation(LinearLayout.HORIZONTAL);
        hsvPresets.addView(presetsContainer);
        root.addView(hsvPresets);

        // Campos de Cargo y Área
        EditText etRoleName = createTextInput(context, root, "Cargo a Crear", "");
        EditText etAreaName = createTextInput(context, root, "Área / Departamento", "");

        // 3 Filas de KPI Propio (Nombre del KPI + Meta numérica)
        TextView tvKpisHeader = new TextView(context);
        tvKpisHeader.setText("DEFINE SUS PROPIOS KPIS Y METAS DIARIAS:");
        tvKpisHeader.setTextSize(11f);
        tvKpisHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvKpisHeader.setTextColor(ContextCompat.getColor(context, R.color.kpi_primary));
        LinearLayout.LayoutParams khLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        khLp.topMargin = dp(context, 12);
        tvKpisHeader.setLayoutParams(khLp);
        root.addView(tvKpisHeader);

        EditText[] kpi1 = createKpiRow(context, root, "KPI 1");
        EditText[] kpi2 = createKpiRow(context, root, "KPI 2");
        EditText[] kpi3 = createKpiRow(context, root, "KPI 3");

        Runnable applyModeDefaults = () -> {
            presetsContainer.removeAllViews();
            if (isPracticanteSelected[0]) {
                tvTitle.setText("Configurar Nuevo Practicante");
                tvSub.setText("Adaptado a practicantes: metas graduales de apoyo, tareas formativas y entregas supervisadas.");
                btnTypePracticante.setBackgroundResource(R.drawable.kpi_bg_btn_electric);
                btnTypePracticante.setTextColor(Color.WHITE);
                btnTypeColaborador.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                btnTypeColaborador.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_primary));

                etRoleName.setText("Practicante de Sistemas");
                etAreaName.setText("Sistemas e Informática");
                kpi1[0].setText("Mapeo y documentación de procesos"); kpi1[1].setText("4");
                kpi2[0].setText("Soporte y pruebas de software"); kpi2[1].setText("10");
                kpi3[0].setText("Apoyo en incidencias técnicas"); kpi3[1].setText("3");

                addPresetChip(context, presetsContainer, "Pract. Sistemas", () -> {
                    etRoleName.setText("Practicante de Sistemas");
                    etAreaName.setText("Sistemas e Informática");
                    kpi1[0].setText("Mapeo y documentación de procesos"); kpi1[1].setText("4");
                    kpi2[0].setText("Soporte y pruebas de software"); kpi2[1].setText("10");
                    kpi3[0].setText("Apoyo en incidencias técnicas"); kpi3[1].setText("3");
                });
                addPresetChip(context, presetsContainer, "Pract. Audiovisual", () -> {
                    etRoleName.setText("Practicante Audiovisual");
                    etAreaName.setText("Audiovisual");
                    kpi1[0].setText("Corte inicial y subtitulado de Reels"); kpi1[1].setText("3");
                    kpi2[0].setText("Organización de clips y audio"); kpi2[1].setText("10");
                    kpi3[0].setText("Apoyo técnico en grabación"); kpi3[1].setText("1");
                });
            } else {
                tvTitle.setText("Configurar Nuevo Colaborador");
                tvSub.setText("Adaptado a colaboradores titulares: metas de producción final y SLA de calidad.");
                btnTypeColaborador.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                btnTypeColaborador.setTextColor(Color.WHITE);
                btnTypePracticante.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
                btnTypePracticante.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_primary));

                etRoleName.setText("Editor Audiovisual");
                etAreaName.setText("Audiovisual");
                kpi1[0].setText("Reels dinámicos editados y sonorizados"); kpi1[1].setText("4");
                kpi2[0].setText("Videos corporativos / horizontales"); kpi2[1].setText("1");
                kpi3[0].setText("Entregas aprobadas sin reproceso"); kpi3[1].setText("4");

                addPresetChip(context, presetsContainer, "Editor Audiovisual", () -> {
                    etRoleName.setText("Editor Audiovisual");
                    etAreaName.setText("Audiovisual");
                    kpi1[0].setText("Reels dinámicos editados y sonorizados"); kpi1[1].setText("4");
                    kpi2[0].setText("Videos corporativos / horizontales"); kpi2[1].setText("1");
                    kpi3[0].setText("Entregas aprobadas sin reproceso"); kpi3[1].setText("4");
                });
                addPresetChip(context, presetsContainer, "Community Manager", () -> {
                    etRoleName.setText("Community Manager");
                    etAreaName.setText("Marketing Digital");
                    kpi1[0].setText("Cuentas gestionadas con parrilla al día"); kpi1[1].setText("8");
                    kpi2[0].setText("Campañas / Copys estratégicos redactados"); kpi2[1].setText("6");
                    kpi3[0].setText("Leads / Mensajes respondidos en SLA"); kpi3[1].setText("30");
                });
            }
        };
        applyModeDefaults.run();

        btnTypePracticante.setOnClickListener(v -> {
            isPracticanteSelected[0] = true;
            applyModeDefaults.run();
        });
        btnTypeColaborador.setOnClickListener(v -> {
            isPracticanteSelected[0] = false;
            applyModeDefaults.run();
        });

        // Botones inferiores
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(context, 16);
        actions.setLayoutParams(actLp);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cancelar");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextSize(13f);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_secondary));
        btnCancel.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        actions.addView(btnCancel);

        TextView btnSave = new TextView(context);
        btnSave.setText("Crear Cargo y sus KPIs");
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setTextSize(13f);
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setTextColor(Color.WHITE);
        btnSave.setBackgroundResource(R.drawable.kpi_bg_gradient_btn);
        btnSave.setPadding(dp(context, 18), dp(context, 12), dp(context, 18), dp(context, 12));
        btnSave.setOnClickListener(v -> {
            String role = etRoleName.getText().toString().trim();
            String area = etAreaName.getText().toString().trim();
            if (role.isEmpty()) {
                Toast.makeText(context, "Escribe el nombre del cargo", Toast.LENGTH_SHORT).show();
                return;
            }
            if (area.isEmpty()) area = "General";

            String m1 = kpi1[0].getText().toString().trim();
            String m2 = kpi2[0].getText().toString().trim();
            String m3 = kpi3[0].getText().toString().trim();
            int g1 = parseGoal(kpi1[1].getText().toString(), 4);
            int g2 = parseGoal(kpi2[1].getText().toString(), 2);
            int g3 = parseGoal(kpi3[1].getText().toString(), 2);

            if (m1.isEmpty()) m1 = "Indicador principal de " + role;
            if (m2.isEmpty()) m2 = "Entregables operativos";
            if (m3.isEmpty()) m3 = "Calidad y cumplimiento";

            KpiRoleProfileConfig.CustomRoleEntry entry = new KpiRoleProfileConfig.CustomRoleEntry(
                    role, area, isPracticanteSelected[0], m1, m2, m3, g1, g2, g3);
            KpiRoleProfileConfig.addCustomRole(context, entry);

            // Registrar sus KPIs reales en KpiCustomRepository si aún no tiene indicadores activos
            KpiCustomRepository repo = KpiCustomRepository.getInstance(context);
            List<KpiDefinition> existing = repo.getActiveKpisForRole(role);
            boolean hasDedicated = false;
            for (KpiDefinition kd : existing) {
                if (role.equalsIgnoreCase(kd.getRole())) {
                    hasDedicated = true;
                    break;
                }
            }
            if (!hasDedicated) {
                saveCustomKpiDef(repo, role, area, m1, g1, 40.0, isPracticanteSelected[0]);
                saveCustomKpiDef(repo, role, area, m2, g2, 35.0, isPracticanteSelected[0]);
                saveCustomKpiDef(repo, role, area, m3, g3, 25.0, isPracticanteSelected[0]);
            }

            dialog.dismiss();
            listener.onRoleCreated(entry);
        });
        actions.addView(btnSave);
        root.addView(actions);

        dialog.setContentView(scroll);
        if (dialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.94f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static void saveCustomKpiDef(KpiCustomRepository repo, String role, String area,
                                         String name, int target, double weight, boolean isPracticante) {
        KpiDefinition def = new KpiDefinition(
                null, name, area, role,
                KpiMeasurementType.CANTIDAD,
                isPracticante ? "tareas/piezas" : "entregables",
                target, weight, KpiPolarity.MAYOR_ES_MEJOR
        );
        def.setDescription((isPracticante ? "Meta formativa de practicante · " : "Meta titular de colaborador · ") + role);
        def.setObjective("Evaluación personalizada configurada por Gerencia.");
        def.setResponsible(role);
        def.setFrequency("Diaria");
        def.setActive(true);
        repo.saveOrUpdateKpi(def);
    }

    private static void addPresetChip(Context context, LinearLayout parent, String label, Runnable action) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(11f);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setTextColor(ContextCompat.getColor(context, R.color.kpi_primary));
        chip.setBackgroundResource(R.drawable.kpi_bg_goal_card_selected);
        chip.setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(context, 6);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> action.run());
        parent.addView(chip);
    }

    private static EditText createTextInput(Context context, LinearLayout parent, String label, String value) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setTextSize(11f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_secondary));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(context, 8);
        tv.setLayoutParams(tLp);
        parent.addView(tv);

        EditText et = new EditText(context);
        et.setText(value);
        et.setTextSize(13f);
        et.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_primary));
        et.setBackgroundResource(R.drawable.kpi_junior_bg_input);
        et.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        parent.addView(et);
        return et;
    }

    private static EditText[] createKpiRow(Context context, LinearLayout parent, String placeholder) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = dp(context, 6);
        row.setLayoutParams(rLp);

        EditText etName = new EditText(context);
        etName.setHint(placeholder + " (Nombre del indicador)");
        etName.setTextSize(12f);
        etName.setTextColor(ContextCompat.getColor(context, R.color.kpi_text_primary));
        etName.setBackgroundResource(R.drawable.kpi_junior_bg_input);
        etName.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nLp.rightMargin = dp(context, 6);
        etName.setLayoutParams(nLp);

        EditText etGoal = new EditText(context);
        etGoal.setHint("Meta");
        etGoal.setInputType(InputType.TYPE_CLASS_NUMBER);
        etGoal.setGravity(Gravity.CENTER);
        etGoal.setTextSize(13f);
        etGoal.setTypeface(Typeface.DEFAULT_BOLD);
        etGoal.setTextColor(ContextCompat.getColor(context, R.color.kpi_primary));
        etGoal.setBackgroundResource(R.drawable.kpi_senior_bg_input);
        etGoal.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        etGoal.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 62), ViewGroup.LayoutParams.WRAP_CONTENT));

        row.addView(etName);
        row.addView(etGoal);
        parent.addView(row);
        return new EditText[]{etName, etGoal};
    }

    private static int parseGoal(String raw, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int dp(Context context, int val) {
        return Math.round(val * context.getResources().getDisplayMetrics().density);
    }
}
