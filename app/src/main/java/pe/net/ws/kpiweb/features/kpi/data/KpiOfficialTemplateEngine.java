package pe.net.ws.kpiweb.features.kpi.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.accountant.data.KpiAccountantStorageManager;
import pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantDayRecord;
import pe.net.ws.kpiweb.features.kpi.accountant.model.KpiAccountantWeekReport;
import pe.net.ws.kpiweb.features.kpi.administrator.data.KpiAdministratorStorageManager;
import pe.net.ws.kpiweb.features.kpi.administrator.model.KpiAdministratorDayRecord;
import pe.net.ws.kpiweb.features.kpi.administrator.model.KpiAdministratorWeekReport;
import pe.net.ws.kpiweb.features.kpi.assistant.data.KpiAdminAssistantStorageManager;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantDayRecord;
import pe.net.ws.kpiweb.features.kpi.assistant.model.KpiAdminAssistantWeekReport;
import pe.net.ws.kpiweb.features.kpi.community.data.KpiCommunityStorageManager;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityDayRecord;
import pe.net.ws.kpiweb.features.kpi.community.model.KpiCommunityWeekReport;
import pe.net.ws.kpiweb.features.kpi.designer.data.KpiDesignerMemoryRepository;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerDayRecord;
import pe.net.ws.kpiweb.features.kpi.designer.model.KpiDesignerWeekReport;
import pe.net.ws.kpiweb.features.kpi.manager.data.KpiGeneralManagerStorageManager;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerDayRecord;
import pe.net.ws.kpiweb.features.kpi.manager.model.KpiGeneralManagerWeekReport;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.model.KpiProductionGoal;
import pe.net.ws.kpiweb.features.kpi.photographer.data.KpiPhotographerStorageManager;
import pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerDayRecord;
import pe.net.ws.kpiweb.features.kpi.photographer.model.KpiPhotographerWeekReport;
import pe.net.ws.kpiweb.features.kpi.senior.data.KpiSeniorStorageManager;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorDayRecord;
import pe.net.ws.kpiweb.features.kpi.senior.model.KpiSeniorWeekReport;
import pe.net.ws.kpiweb.features.kpi.trafficker.data.KpiTraffickerStorageManager;
import pe.net.ws.kpiweb.features.kpi.trafficker.model.KpiTraffickerDayRecord;
import pe.net.ws.kpiweb.features.kpi.trafficker.model.KpiTraffickerWeekReport;
import pe.net.ws.kpiweb.features.kpi.ui.KpiCustomKeypadDialog;
import pe.net.ws.kpiweb.features.kpi.videographer.data.KpiVideographerStorageManager;
import pe.net.ws.kpiweb.features.kpi.videographer.model.KpiVideographerDayRecord;
import pe.net.ws.kpiweb.features.kpi.videographer.model.KpiVideographerWeekReport;

/**
 * Motor Maestro de Plantillas Visuales WYSIWYG SIGD 2026.
 * Permite editar directamente sobre la hoja oficial del reporte (sin datos de usuario)
 * literalmente cualquier texto, encabezado, meta diaria/semanal, porcentaje, criterio,
 * acción, observación o pie de página (correo, web, redes), para TODOS los cargos.
 *
 * Sincroniza los cambios automáticamente con:
 * 1. El Reporte Oficial de la Semana Actual (sin alterar semanas pasadas/históricas).
 * 2. El Formulario KPI que llena el colaborador en su jornada diaria (días abiertos/nuevos).
 */
public class KpiOfficialTemplateEngine {

    private static final String PREF_NAME = "kpi_official_sheet_templates_v1";
    private static final String TAG_DYNAMIC_CONTACT_FOOTER = "kpi_dynamic_contact_footer_bar";
    private static final String TAG_ORIGINAL_TEXT = "kpi_original_template_text";
    private static final String TAG_CUSTOM_LOGO_IMAGE = "kpi_custom_logo_imageview";

    // Claves institucionales compartidas (img1: Logo de cabecera | img2: Pie de página)
    public static final String INST_LOGO_IMAGE_PATH = "inst_logo_image_path";
    public static final String INST_LOGO_TITLE = "inst_logo_title";
    public static final String INST_LOGO_SUBTITLE = "inst_logo_subtitle";
    public static final String INST_FOOTER_WEB = "inst_footer_web";
    public static final String INST_FOOTER_EMAIL = "inst_footer_email";
    public static final String INST_FOOTER_SOCIAL = "inst_footer_social";
    public static final String INST_FOOTER_SIGD_TITLE = "inst_footer_sigd_title";
    public static final String INST_FOOTER_SIGD_SUB = "inst_footer_sigd_sub";
    public static final String INST_FOOTER_SLOGAN = "inst_footer_slogan";
    public static final String INST_FOOTER_RIGHT = "inst_footer_right";

    private static final String[] ALL_INSTITUTIONAL_KEYS = {
            INST_LOGO_IMAGE_PATH, INST_LOGO_TITLE, INST_LOGO_SUBTITLE,
            INST_FOOTER_WEB, INST_FOOTER_EMAIL, INST_FOOTER_SOCIAL,
            INST_FOOTER_SIGD_TITLE, INST_FOOTER_SIGD_SUB, INST_FOOTER_SLOGAN, INST_FOOTER_RIGHT
    };

    public interface LogoPickRequestListener {
        void onRequestPickLogoImage(String cargo);
    }

    private LogoPickRequestListener logoPickRequestListener;

    public void setLogoPickRequestListener(LogoPickRequestListener listener) {
        this.logoPickRequestListener = listener;
    }

    private static volatile KpiOfficialTemplateEngine instance;
    private final Context appContext;
    private final SharedPreferences prefs;

    private KpiOfficialTemplateEngine(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized KpiOfficialTemplateEngine getInstance(Context context) {
        if (instance == null) {
            instance = new KpiOfficialTemplateEngine(context);
        }
        return instance;
    }

    public static String normalizeCargoKey(String cargo) {
        if (cargo == null || cargo.trim().isEmpty()) return "PROGRAMADOR_JUNIOR";
        String clean = Normalizer.normalize(cargo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT);
        if ("ASISTENTE ADMINISTRATIVO".equals(clean)) return "ASISTENTE_ADMINISTRATIVO";
        if ("ADMINISTRADOR".equals(clean)) return "ADMINISTRADOR";
        if ("CONTADOR".equals(clean)) return "CONTADOR";
        if ("COMMUNITY MANAGER".equals(clean)) return "COMMUNITY_MANAGER";
        if ("DISENADOR GRAFICO SENIOR".equals(clean) || "DISENADOR SENIOR".equals(clean)) return "DISENADOR_GRAFICO_SENIOR";
        if ("DISENADOR GRAFICO JUNIOR".equals(clean) || "DISENADOR JUNIOR".equals(clean)) return "DISENADOR_GRAFICO_JUNIOR";
        if ("DISENADOR GRAFICO".equals(clean)) return "DISENADOR_GRAFICO";
        if ("ASESOR DE VENTAS".equals(clean) || "EJECUTIVO DE VENTAS".equals(clean)) return "ASESOR_DE_VENTAS";
        if ("FOTOGRAFO".equals(clean)) return "FOTOGRAFO";
        if ("TRAFFICKER DIGITAL".equals(clean) || "TRAFFICKER".equals(clean)) return "TRAFFICKER_DIGITAL";
        if ("VIDEOGRAFO".equals(clean)) return "VIDEOGRAFO";
        if ("GERENTE GENERAL".equals(clean)) return "GERENTE_GENERAL";
        if ("PROGRAMADOR JUNIOR".equals(clean) || "PROGRAMADOR".equals(clean)) return "PROGRAMADOR_JUNIOR";

        if (instance != null && instance.isCustomRoleKey(clean.replaceAll("[^A-Z0-9_]", "_"))) {
            return "CUSTOM_" + clean.replaceAll("[^A-Z0-9_]", "_");
        }

        if (clean.contains("ASISTENTE") && clean.contains("ADMIN")) return "ASISTENTE_ADMINISTRATIVO";
        if (clean.contains("ADMINISTRADOR")) return "ADMINISTRADOR";
        if (clean.contains("CONTADOR")) return "CONTADOR";
        if (clean.contains("COMMUNITY")) return "COMMUNITY_MANAGER";
        if (clean.contains("SENIOR")) return "DISENADOR_GRAFICO_SENIOR";
        if (clean.contains("DISENADOR") && clean.contains("JUNIOR")) return "DISENADOR_GRAFICO_JUNIOR";
        if (clean.contains("DISENADOR")) return "DISENADOR_GRAFICO";
        if (clean.contains("VENTA") || clean.contains("ASESOR") || clean.contains("COMERCIAL")) return "ASESOR_DE_VENTAS";
        if (clean.contains("FOTOGRAFO")) return "FOTOGRAFO";
        if (clean.contains("TRAFFICKER") || clean.contains("PAUTA")) return "TRAFFICKER_DIGITAL";
        if (clean.contains("VIDEOGRAFO")) return "VIDEOGRAFO";
        if (clean.contains("GERENTE")) return "GERENTE_GENERAL";
        if (clean.contains("PROGRAMADOR")) return "PROGRAMADOR_JUNIOR";
        return clean.replaceAll("[^A-Z0-9_]", "_");
    }

    private static String toRawRoleToken(String cargo) {
        if (cargo == null) return "";
        return Normalizer.normalize(cargo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]", "_");
    }

    private boolean isCustomRoleKey(String rawToken) {
        return prefs.contains("custom_role_rank_" + rawToken);
    }

    public static boolean isCustomRoleStatic(String cargo) {
        return instance != null && instance.isCustomRole(cargo);
    }

    public static boolean isCustomRoleJefeStatic(String cargo) {
        return instance != null && instance.isCustomRoleJefe(cargo);
    }

    public boolean isCustomRole(String cargo) {
        return isCustomRoleKey(toRawRoleToken(cargo));
    }

    public boolean isCustomRoleJefe(String cargo) {
        String rank = prefs.getString("custom_role_rank_" + toRawRoleToken(cargo), "COLABORADOR");
        return "JEFE".equalsIgnoreCase(rank);
    }

    public void registerCustomKpiRole(String cargoName, boolean isJefe) {
        if (cargoName == null || cargoName.trim().isEmpty()) return;
        String trimmed = cargoName.trim();
        String token = toRawRoleToken(trimmed);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("custom_role_rank_" + token, isJefe ? "JEFE" : "COLABORADOR");
        ed.putString("custom_role_display_" + token, trimmed);

        List<String> current = getCustomRoleNames();
        boolean found = false;
        for (String c : current) {
            if (c.equalsIgnoreCase(trimmed)) {
                found = true;
                break;
            }
        }
        if (!found) {
            current.add(trimmed);
            JSONArray arr = new JSONArray();
            for (String c : current) arr.put(c);
            ed.putString("custom_roles_list_json", arr.toString());
        }
        ed.apply();

        // Sincronizar con el registro de cargos personalizados del panel principal
        pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.addCustomRole(
                appContext,
                new pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.CustomRoleEntry(
                        trimmed,
                        isJefe ? "Jefatura / Dirección" : "Gestión Operativa",
                        false,
                        "Indicador 1",
                        "Indicador 2",
                        "Indicador 3",
                        3, 2, 1
                )
        );
    }

    public boolean deleteCustomKpiRole(String cargoName) {
        if (cargoName == null || cargoName.trim().isEmpty()) return false;
        String trimmed = cargoName.trim();
        String token = toRawRoleToken(trimmed);
        String normKey = normalizeCargoKey(trimmed);

        List<String> current = getCustomRoleNames();
        boolean removed = false;
        for (int i = current.size() - 1; i >= 0; i--) {
            if (current.get(i).equalsIgnoreCase(trimmed)) {
                current.remove(i);
                removed = true;
            }
        }

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove("custom_role_rank_" + token);
        ed.remove("custom_role_display_" + token);
        ed.remove("overrides_" + normKey);
        ed.remove("overrides_CUSTOM_" + token);

        for (String k : prefs.getAll().keySet()) {
            if (k.startsWith("sem_" + normKey + "_")
                    || k.startsWith("sem_CUSTOM_" + token + "_")
                    || k.startsWith("inst_cargo_" + normKey + "_")
                    || k.startsWith("inst_cargo_CUSTOM_" + token + "_")) {
                ed.remove(k);
            }
        }

        JSONArray arr = new JSONArray();
        for (String c : current) arr.put(c);
        ed.putString("custom_roles_list_json", arr.toString());
        ed.apply();

        pe.net.ws.kpiweb.features.kpi.designer.model.KpiRoleProfileConfig.removeCustomRole(appContext, trimmed);
        return removed;
    }

    public List<String> getCustomRoleNames() {
        List<String> list = new ArrayList<>();
        String json = prefs.getString("custom_roles_list_json", null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    String item = arr.optString(i, "").trim();
                    if (!item.isEmpty()) list.add(item);
                }
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    public int getLayoutResForCargo(String cargo) {
        if (isCustomRole(cargo)) {
            return isCustomRoleJefe(cargo)
                    ? R.layout.kpi_view_official_senior_sheet
                    : R.layout.kpi_view_official_photographer_sheet;
        }
        String key = normalizeCargoKey(cargo);
        switch (key) {
            case "ADMINISTRADOR":
                return R.layout.kpi_view_official_administrator_sheet;
            case "ASISTENTE_ADMINISTRATIVO":
                return R.layout.kpi_view_official_admin_assistant_sheet;
            case "CONTADOR":
                return R.layout.kpi_view_official_accountant_sheet;
            case "COMMUNITY_MANAGER":
                return R.layout.kpi_view_official_community_sheet;
            case "DISENADOR_GRAFICO_SENIOR":
                return R.layout.kpi_view_official_senior_sheet;
            case "DISENADOR_GRAFICO":
                return R.layout.kpi_view_official_designer_sheet;
            case "ASESOR_DE_VENTAS":
                return R.layout.kpi_view_official_sales_sheet;
            case "FOTOGRAFO":
                return R.layout.kpi_view_official_photographer_sheet;
            case "TRAFFICKER_DIGITAL":
                return R.layout.kpi_view_official_trafficker_sheet;
            case "VIDEOGRAFO":
                return R.layout.kpi_view_official_videographer_sheet;
            case "GERENTE_GENERAL":
                return R.layout.kpi_view_official_general_manager_sheet;
            case "DISENADOR_GRAFICO_JUNIOR":
                return R.layout.kpi_view_official_junior_designer_sheet;
            case "PROGRAMADOR_JUNIOR":
                return R.layout.kpi_view_official_programmer_sheet;
            default:
                return R.layout.kpi_view_official_photographer_sheet;
        }
    }

    public boolean isDoubleShiftJuniorCargo(String cargo) {
        String key = normalizeCargoKey(cargo);
        return "PROGRAMADOR_JUNIOR".equals(key) || "DISENADOR_GRAFICO_JUNIOR".equals(key);
    }

    // =========================================================================
    // 1. PERSISTENCIA DE OVERRIDES VISUALES Y CAMPOS SEMÁNTICOS POR CARGO
    // =========================================================================

    public Map<String, String> getViewOverrides(String cargo) {
        String key = "overrides_" + normalizeCargoKey(cargo);
        String jsonStr = prefs.getString(key, null);
        Map<String, String> map = new LinkedHashMap<>();
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONObject obj = new JSONObject(jsonStr);
                Iterator<String> it = obj.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    map.put(k, obj.optString(k, ""));
                }
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    public void saveViewOverride(String cargo, String viewKey, String newText) {
        Map<String, String> map = getViewOverrides(cargo);
        map.put(viewKey, newText != null ? newText : "");
        persistViewOverrides(cargo, map);
    }

    public void removeViewOverride(String cargo, String viewKey) {
        Map<String, String> map = getViewOverrides(cargo);
        map.remove(viewKey);
        persistViewOverrides(cargo, map);
    }

    private void persistViewOverrides(String cargo, Map<String, String> map) {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, String> e : map.entrySet()) {
                obj.put(e.getKey(), e.getValue());
            }
            prefs.edit().putString("overrides_" + normalizeCargoKey(cargo), obj.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void saveSemanticField(String cargo, String fieldKey, String value) {
        prefs.edit().putString("sem_" + normalizeCargoKey(cargo) + "_" + fieldKey, value != null ? value : "").apply();
    }

    public String getSemanticField(String cargo, String fieldKey, String defaultValue) {
        return prefs.getString("sem_" + normalizeCargoKey(cargo) + "_" + fieldKey, defaultValue);
    }

    public boolean hasCustomizations(String cargo) {
        return getActiveChangesCount(cargo) > 0;
    }

    public int getActiveChangesCount(String cargo) {
        int count = getViewOverrides(cargo).size();
        for (String instKey : ALL_INSTITUTIONAL_KEYS) {
            String val = getInstitutionalValue(cargo, instKey);
            if (val != null && !val.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Guarda un valor institucional (Logo de img1 o Pie de página de img2) ya sea
     * para TODOS los KPIs (los 13 cargos) o únicamente para el cargo actual.
     */
    public void saveInstitutionalValue(String cargo, String instKey, String value, boolean applyToAllCargos) {
        SharedPreferences.Editor ed = prefs.edit();
        String safeVal = value != null ? value : "";
        if (applyToAllCargos) {
            ed.putString("inst_global_" + instKey, safeVal);
            // Limpiar overrides individuales de este campo institucional en todos los cargos para que hereden el global
            for (String k : prefs.getAll().keySet()) {
                if (k.startsWith("inst_cargo_") && k.endsWith("_" + instKey)) {
                    ed.remove(k);
                }
            }
        } else {
            ed.putString("inst_cargo_" + normalizeCargoKey(cargo) + "_" + instKey, safeVal);
        }
        ed.apply();
    }

    public String getInstitutionalValue(String cargo, String instKey) {
        String norm = normalizeCargoKey(cargo);
        String cargoKey = "inst_cargo_" + norm + "_" + instKey;
        if (prefs.contains(cargoKey)) {
            return prefs.getString(cargoKey, "");
        }
        return prefs.getString("inst_global_" + instKey, null);
    }

    public void saveInstitutionalLogoImage(String cargo, String imageFilePath, boolean applyToAllCargos) {
        saveInstitutionalValue(cargo, INST_LOGO_IMAGE_PATH, imageFilePath != null ? imageFilePath : "", applyToAllCargos);
    }

    public String getInstitutionalLogoImagePath(String cargo) {
        return getInstitutionalValue(cargo, INST_LOGO_IMAGE_PATH);
    }

    public void resetCargoTemplate(String cargo) {
        String norm = normalizeCargoKey(cargo);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove("overrides_" + norm);
        for (String k : prefs.getAll().keySet()) {
            if (k.startsWith("sem_" + norm + "_") || k.startsWith("inst_cargo_" + norm + "_")) {
                ed.remove(k);
            }
        }
        ed.apply();
    }

    // =========================================================================
    // 2. INFLADO Y PREPARACIÓN DE HOJA EN MODO PLANTILLA LIMPIA (EDITOR)
    // =========================================================================

    public View inflateCleanTemplateForEditor(Context context, String cargo, ViewGroup parent, int selectedShiftIndex) {
        LayoutInflater inflater = LayoutInflater.from(context);
        int layoutRes = getLayoutResForCargo(cargo);
        View sheet = inflater.inflate(layoutRes, parent, false);

        prepareCleanTemplateState(context, cargo, sheet, selectedShiftIndex);
        ensureInstitutionalContactFooter(context, sheet);
        captureOriginalTexts(sheet, sheet);
        applySavedOverridesToSheet(cargo, sheet, true);
        applyInstitutionalLogoAndFooterToSheet(context, cargo, sheet);

        return sheet;
    }

    private void prepareCleanTemplateState(Context context, String cargo, View sheet, int selectedShiftIndex) {
        String key = normalizeCargoKey(cargo);

        if ("PROGRAMADOR_JUNIOR".equals(key)) {
            View llMorning = sheet.findViewById(R.id.llProgShiftMorningContainer);
            View llAfternoon = sheet.findViewById(R.id.llProgShiftAfternoonContainer);
            View llDivider = sheet.findViewById(R.id.llProgDividerScissor);
            if (llDivider != null) llDivider.setVisibility(View.GONE);
            if (llMorning != null) llMorning.setVisibility(selectedShiftIndex == 0 ? View.VISIBLE : View.GONE);
            if (llAfternoon != null) llAfternoon.setVisibility(selectedShiftIndex == 0 ? View.GONE : View.VISIBLE);

            setTextIfPresent(sheet, R.id.tvProgM_Practicante, "— (Nombre del usuario)");
            setTextIfPresent(sheet, R.id.tvProgT_Practicante, "— (Nombre del usuario)");
            setTextIfPresent(sheet, R.id.tvProgM_Fecha, "DD/MM/2026");
            setTextIfPresent(sheet, R.id.tvProgT_Fecha, "DD/MM/2026");

            int[] userCells = {
                    R.id.tvProgM_ResAsignadas, R.id.tvProgM_ResCompletadas, R.id.tvProgM_ResPendientes,
                    R.id.tvProgM_ResFuncionalidades, R.id.tvProgM_ResIncidencias, R.id.tvProgM_ResCumplimiento,
                    R.id.tvProgT_ResAsignadas, R.id.tvProgT_ResCompletadas, R.id.tvProgT_ResPendientes,
                    R.id.tvProgT_ResFuncionalidades, R.id.tvProgT_ResIncidencias, R.id.tvProgT_ResCumplimiento
            };
            for (int id : userCells) setTextIfPresent(sheet, id, "—");
            setTextIfPresent(sheet, R.id.tvProgM_ScorePill, "NOTA: -- / 20");
            setTextIfPresent(sheet, R.id.tvProgT_ScorePill, "NOTA: -- / 20");

            populateCleanActivityPlaceholder((LinearLayout) sheet.findViewById(R.id.llProgM_ActsContainer));
            populateCleanActivityPlaceholder((LinearLayout) sheet.findViewById(R.id.llProgT_ActsContainer));

            String[] defaultProgCriteria = {
                    "1. Desarrollo y arquitectura de módulos",
                    "2. Calidad de código y orden en commits",
                    "3. Cumplimiento de funcionalidades asignadas",
                    "4. Resolución de bugs y proactividad"
            };
            populateCriteriaRows((LinearLayout) sheet.findViewById(R.id.llProgM_EvalCriteriaContainer), cargo, defaultProgCriteria, false);
            populateCriteriaRows((LinearLayout) sheet.findViewById(R.id.llProgT_EvalCriteriaContainer), cargo, defaultProgCriteria, false);
            return;
        }

        if ("DISENADOR_GRAFICO_JUNIOR".equals(key)) {
            View llMorning = sheet.findViewById(R.id.llDesShiftMorningContainer);
            View llAfternoon = sheet.findViewById(R.id.llDesShiftAfternoonContainer);
            View llDivider = sheet.findViewById(R.id.llDesDividerScissor);
            if (llDivider != null) llDivider.setVisibility(View.GONE);
            if (llMorning != null) llMorning.setVisibility(selectedShiftIndex == 0 ? View.VISIBLE : View.GONE);
            if (llAfternoon != null) llAfternoon.setVisibility(selectedShiftIndex == 0 ? View.GONE : View.VISIBLE);

            setTextIfPresent(sheet, R.id.tvDesM_Practicante, "— (Nombre del usuario)");
            setTextIfPresent(sheet, R.id.tvDesT_Practicante, "— (Nombre del usuario)");
            setTextIfPresent(sheet, R.id.tvDesM_Fecha, "DD/MM/2026");
            setTextIfPresent(sheet, R.id.tvDesT_Fecha, "DD/MM/2026");

            int[] userCells = {
                    R.id.tvDesM_ResPosts, R.id.tvDesM_ResVideos, R.id.tvDesM_ResTotalPiezas, R.id.tvDesM_ResMeta,
                    R.id.tvDesM_ResEntregadas, R.id.tvDesM_ResCorrecciones, R.id.tvDesM_ResCumplimiento,
                    R.id.tvDesT_ResPosts, R.id.tvDesT_ResVideos, R.id.tvDesT_ResTotalPiezas, R.id.tvDesT_ResMeta,
                    R.id.tvDesT_ResEntregadas, R.id.tvDesT_ResCorrecciones, R.id.tvDesT_ResCumplimiento
            };
            for (int id : userCells) setTextIfPresent(sheet, id, "—");
            setTextIfPresent(sheet, R.id.tvDesM_ScorePill, "NOTA: -- / 20");
            setTextIfPresent(sheet, R.id.tvDesT_ScorePill, "NOTA: -- / 20");

            populateCleanActivityPlaceholder((LinearLayout) sheet.findViewById(R.id.llDesM_ActsContainer));
            populateCleanActivityPlaceholder((LinearLayout) sheet.findViewById(R.id.llDesT_ActsContainer));

            String[] defaultDesCriteria = {
                    "1. Composición visual y balance gráfico",
                    "2. Aplicación rigurosa de manual de marca",
                    "3. Cumplimiento de metas y entregables",
                    "4. Cero correcciones críticas y proactividad"
            };
            populateCriteriaRows((LinearLayout) sheet.findViewById(R.id.llDesM_EvalCriteriaContainer), cargo, defaultDesCriteria, false);
            populateCriteriaRows((LinearLayout) sheet.findViewById(R.id.llDesT_EvalCriteriaContainer), cargo, defaultDesCriteria, false);
            return;
        }

        if ("ASESOR_DE_VENTAS".equals(key)) {
            TextView btnGoals = sheet.findViewById(R.id.btnSalesTabKpiGoals);
            TextView btnRoute = sheet.findViewById(R.id.btnSalesTabRoute);
            View llGoals = sheet.findViewById(R.id.llSalesContainerKpiGoals);
            View llRoute = sheet.findViewById(R.id.llSalesContainerRoute);
            if (btnGoals != null && btnRoute != null && llGoals != null && llRoute != null) {
                btnGoals.setOnClickListener(v -> {
                    llGoals.setVisibility(View.VISIBLE);
                    llRoute.setVisibility(View.GONE);
                    btnGoals.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                    btnGoals.setTextColor(Color.WHITE);
                    btnRoute.setBackgroundResource(android.R.color.transparent);
                    btnRoute.setTextColor(Color.parseColor("#64748B"));
                });
                btnRoute.setOnClickListener(v -> {
                    llGoals.setVisibility(View.GONE);
                    llRoute.setVisibility(View.VISIBLE);
                    btnRoute.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                    btnRoute.setTextColor(Color.WHITE);
                    btnGoals.setBackgroundResource(android.R.color.transparent);
                    btnGoals.setTextColor(Color.parseColor("#64748B"));
                });
            }
            setTextIfPresent(sheet, R.id.tvSalesCollabName, "EJECUTIVO:  — (Automático)");
            setTextIfPresent(sheet, R.id.tvRouteCollabName, "EJECUTIVO DE VENTAS:  — (Automático)");
        }

        // En cualquier hoja semanal de colaborador, limpiamos todas las celdas de conteo del usuario a "—"
        clearUserNumericCellsInCollaboratorSheet(context, sheet);

        // Si es un Nuevo KPI personalizado creado por el usuario, preparamos su estructura en blanco
        if (isCustomRole(cargo)) {
            prepareBlankCustomRoleSheet(context, cargo, sheet, isCustomRoleJefe(cargo));
        }
    }

    /**
     * Prepara la plantilla predeterminada en blanco para un Nuevo KPI creado por el usuario:
     * - Si es Jefe: estructura tipo Diseñador Gráfico Senior.
     * - Si es Colaborador: estructura tipo Fotógrafo.
     * Deja únicamente el cargo, los encabezados de secciones 1, 2, 3 ("ACCIONES QUE REALIZA EL <CARGO>"),
     * en la 4 solo Nivel KPI y % Cumplimiento, y en la 5 Observaciones vacías listas para llenar.
     */
    public void prepareBlankCustomRoleSheet(Context context, String cargo, View sheet, boolean isJefe) {
        if (sheet == null || cargo == null) return;
        String upperCargo = cargo.trim().toUpperCase(Locale.getDefault());

        if (!isJefe) {
            // Estructura COLABORADOR (basada en Fotógrafo)
            replaceTextInHierarchy(sheet, "FOTÓGRAFO", upperCargo);
            replaceTextInHierarchy(sheet, "»» PLANIFICA • CAPTURA • EDITA • ENTREGA RESULTADOS", "(Vacío)");
            replaceTextInHierarchy(sheet, "📷 Captura, edita\ny produce\ncontenido visual", "(Vacío)");

            setTextIfPresent(sheet, R.id.tvPhoCollabRole, "Cargo:  " + upperCargo);
            setTextIfPresent(sheet, R.id.tvPhoCollabArea, "Área:  (Vacío)");
            setTextIfPresent(sheet, R.id.tvPhoCollabSupervisor, "Responsable de evaluación:  (Vacío)");
            setTextIfPresent(sheet, R.id.tvPhoCollabBoss, "Jefe inmediato:  (Vacío)");

            // Sección 1: Control Diario (encabezados genéricos y filas de Lunes a Sábado vacías)
            replaceTextInHierarchy(sheet, "SESIONES\nREALIZADAS", "INDICADOR 1");
            replaceTextInHierarchy(sheet, "FOTOS\nCAPTURADAS", "INDICADOR 2");
            replaceTextInHierarchy(sheet, "FOTOS\nEDITADAS", "INDICADOR 3");
            clearDayRowsActivitiesAndMetas(sheet);
            replaceTextInHierarchy(sheet, "≥ 1 sesiones\n(≥ 140 fotos)", "(Vacío)");
            replaceTextInHierarchy(sheet, "📷 Sesión: Toma fotográfica (planificada o de cliente).", "(Vacío)");
            replaceTextInHierarchy(sheet, "💻 Edición: Selección, retoque y exportación de fotos.", "(Vacío)");

            // Sección 2: Resumen Semanal de Producción (solo campos, sin registros)
            String[] phoSummaryLabels = {
                    "Sesiones realizadas", "≥ 5",
                    "Fotos capturadas", "≥ 140",
                    "Fotos editadas", "≥ 120",
                    "Entregas de material", "4 indicadores"
            };
            for (String s : phoSummaryLabels) {
                replaceTextInHierarchy(sheet, s, "(Vacío)");
            }

            // Sección 3: Acciones que realiza el <CARGO> (con registros vacíos)
            replaceTextInHierarchy(sheet, "ACCIONES QUE REALIZA EL FOTÓGRAFO", "ACCIONES QUE REALIZA EL " + upperCargo);
            replaceTextInHierarchy(sheet, "ACCIONES QUE REALIZA EL " + upperCargo + " ", "ACCIONES QUE REALIZA EL " + upperCargo);
            String[] phoActions = {
                    "☑  Planificación de sesiones según cronograma.",
                    "☑  Toma fotográfica (producto, servicio o cliente).",
                    "☑  Selección y edición del material.",
                    "☑  Entrega de fotos en la carpeta del proyecto.",
                    "☑  Respaldo y organización del archivo.",
                    "☑  Coordinación con diseño y marketing.",
                    "☐  Otras actividades asignadas."
            };
            for (String act : phoActions) {
                replaceTextInHierarchy(sheet, act, "☐  (Vacío)");
            }

            // Sección 4: Evaluación KPI - Productividad (conservar Nivel KPI y % Cumplimiento; vaciar Producción y Descripción)
            int[] levelRowIds = {
                    R.id.rowPhoLevelExcelente, R.id.rowPhoLevelMuyBueno,
                    R.id.rowPhoLevelBueno, R.id.rowPhoLevelRegular, R.id.rowPhoLevelDeficiente
            };
            for (int rowId : levelRowIds) {
                View row = sheet.findViewById(rowId);
                if (row instanceof ViewGroup) {
                    ViewGroup vgRow = (ViewGroup) row;
                    if (vgRow.getChildCount() >= 4) {
                        if (vgRow.getChildAt(2) instanceof TextView) {
                            ((TextView) vgRow.getChildAt(2)).setText("(Vacío)");
                        }
                        if (vgRow.getChildAt(3) instanceof TextView) {
                            ((TextView) vgRow.getChildAt(3)).setText("(Vacío)");
                        }
                    }
                }
            }

            // Sección 5: Observaciones Generales vacío
            setTextIfPresent(sheet, R.id.tvPhoGeneralObservations, "(Vacío)");
        } else {
            // Estructura JEFE (basada en Diseñador Gráfico Senior)
            replaceTextInHierarchy(sheet, "DISEÑADOR GRÁFICO SENIOR", upperCargo);
            replaceTextInHierarchy(sheet, ">>> PRODUCE • SUPERVISA • COORDINA • GARANTIZA CALIDAD", "(Vacío)");
            replaceTextInHierarchy(sheet, "Creatividad\nque genera\nresultados", "(Vacío)");

            setTextIfPresent(sheet, R.id.tvSheetCollabRole, "Cargo: " + cargo.trim() + " (Jefatura)");
            setTextIfPresent(sheet, R.id.tvSheetCollabArea, "Área: (Vacío)");
            setTextIfPresent(sheet, R.id.tvSheetCollabSupervisor, "Evaluador: (Vacío)");
            setTextIfPresent(sheet, R.id.tvSheetCollabBoss, "Jefe: (Vacío)");

            // Sección 1: Control Diario (actividades y metas diarias vacías)
            replaceTextInHierarchy(sheet, "Audiov. + diseño", "(Vacío)");
            replaceTextInHierarchy(sheet, "Diseño + sesión", "(Vacío)");
            replaceTextInHierarchy(sheet, "4 vid + 2 dis", "(Vacío)");
            replaceTextInHierarchy(sheet, "2 vid + 1 dis", "(Vacío)");
            replaceTextInHierarchy(sheet, "2 dis + 1 ses", "(Vacío)");

            // Sección 2 / 3: Acciones que realiza el <CARGO> (registros vacíos)
            replaceTextInHierarchy(sheet, "RUTINA (08:00 - 01:30)", "ACCIONES QUE REALIZA EL " + upperCargo);
            String[] srRoutines = {
                    "08:00 - 08:15  Ingreso y organización",
                    "08:15 - 09:00  Revisión briefs",
                    "09:00 - 10:00  Producción creativa",
                    "10:00 - 10:30  Revisión y ajustes",
                    "10:30 - 12:00  Producción creativa",
                    "12:00 - 01:00  Supervisión de equipo",
                    "01:00 - 01:30  Entrega y reportes"
            };
            for (String r : srRoutines) {
                replaceTextInHierarchy(sheet, r, "☐  (Vacío)");
            }

            // Sección 3: Resumen Semanal de Producción (solo campos, sin registros)
            replaceTextInHierarchy(sheet, "Diseños POST", "(Vacío)");
            replaceTextInHierarchy(sheet, "Videos", "(Vacío)");
            replaceTextInHierarchy(sheet, "Sesiones foto", "(Vacío)");
            replaceTextInHierarchy(sheet, "10", "(Vacío)");
            replaceTextInHierarchy(sheet, "12", "(Vacío)");
            replaceTextInHierarchy(sheet, "02", "(Vacío)");
            replaceTextInHierarchy(sheet, "24", "(Vacío)");

            // Sección 4: Indicadores Clave (conservar % pesos y dejar campos listos)
            String[] srInds = {"Calidad prod.", "Productividad", "Supervisión", "Entregas", "Cero errores", "Liderazgo"};
            for (String ind : srInds) {
                replaceTextInHierarchy(sheet, ind, "(Vacío)");
            }

            // Sección 5: Observaciones / Logros / Dificultades / Plan Mejora vacíos
            setTextIfPresent(sheet, R.id.tvSheetAchievements, "(Vacío)");
            setTextIfPresent(sheet, R.id.tvSheetDifficulties, "(Vacío)");
            setTextIfPresent(sheet, R.id.tvSheetImprovementPlan, "(Vacío)");
        }
    }

    private void clearDayRowsActivitiesAndMetas(View current) {
        if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            int dayIdx = detectDayRowIndex(vg);
            if (dayIdx >= 0 && vg.getChildCount() >= 3) {
                if (vg.getChildAt(1) instanceof TextView) {
                    ((TextView) vg.getChildAt(1)).setText("(Vacío)");
                }
                if (vg.getChildAt(2) instanceof TextView) {
                    ((TextView) vg.getChildAt(2)).setText("(Vacío)");
                }
                return;
            }
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearDayRowsActivitiesAndMetas(vg.getChildAt(i));
            }
        }
    }

    private void clearUserNumericCellsInCollaboratorSheet(Context context, View root) {
        if (root == null) return;
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            String idName = getIdEntryName(context, tv);
            if (isUserGeneratedDataCellId(idName)) {
                if (idName.contains("CollabName")) {
                    tv.setText("Colaborador:  — (Automático)");
                } else {
                    tv.setText("—");
                }
            }
        } else if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearUserNumericCellsInCollaboratorSheet(context, vg.getChildAt(i));
            }
        }
    }

    public boolean isUserGeneratedDataCellId(String idName) {
        if (idName == null || idName.isEmpty()) return false;
        if (idName.contains("CollabName") || idName.endsWith("_Practicante") || idName.endsWith("_Fecha")) {
            return true;
        }
        // Celdas diarias y sumatorias llenadas por el usuario en todos los cargos:
        String[] userCellPatterns = {
                "B1Rev_", "B1Gest_", "B1Seg_", "B1Reu_", "B1Inf_", "B1Tot_", "B1Pct_", "B1Obs_",
                "B1Sum", "B2PlanRes", "B2PlanPct", "B2SupRes", "B2SupPct", "B2CoordRes", "B2CoordPct",
                "B2RepRes", "B2RepPct", "B2TotalPct",
                "B1Reg_", "B1Conc_", "B1Comp_", "B2RegRes", "B2RegPct", "B2ConcRes", "B2ConcPct",
                "B1Doc_", "B1Coord_", "B2GestRes", "B2GestPct", "B2DocRes", "B2DocPct",
                "B1Ses_", "B1Cap_", "B1Edit_", "B2SesRes", "B2SesPct", "B2CapRes", "B2CapPct", "B2EditRes", "B2EditPct", "B2EntRes", "B2EntPct",
                "B1Grab_", "B1Aju_", "B2GrabRes", "B2GrabPct", "B2AjuRes", "B2AjuPct",
                "B1Cam_", "B1Rep_", "B2CamRes", "B2CamPct", "B2AbRes", "B2AbPct",
                "GmB1Track_", "GmB1Pct_", "GmB1Obs_", "GmB2Pct_", "GmB2Obs_", "GmB2Act_",
                "GmB1Total", "GmB2Avg", "GmB3IndRes", "GmB3IndPct", "GmB3IssRes", "GmB3IssPct",
                "GmB3AreasRes", "GmB3AreasPct", "GmB3RescRes", "GmB3RescPct", "GmB3SigdRes", "GmB3SigdPct", "GmB3GrandTotalPct", "GmB4FinalScore",
                "CommPub", "CommInt", "CommOtr", "CommTot", "CommPct", "CommObs",
                "CommTotal", "CommGrandTotal", "CommRes", "CommGeneralPct",
                "B1Designs_", "B1Videos_", "B1Photos_", "B1Others_", "B1Total_",
                "B2Sr_", "B2Jr_", "B2TeamPct_", "B2Obs_", "B2Act_", "B1TotalWeekly", "B2AverageWeekly",
                "B3DesignsRes", "B3DesignsPct", "B3VideosRes", "B3VideosPct", "B3PhotosRes", "B3PhotosPct", "B3TotalRes", "B3TotalPct",
                "B4QualityRes", "B4QualityPond", "B4ProdRes", "B4ProdPond", "B4SupRes", "B4SupPond",
                "B4DelivRes", "B4DelivPond", "B4OrthoRes", "B4OrthoPond", "B4LeaderRes", "B4LeaderPond", "B4FinalScore",
                "DgDis_", "DgVid_", "DgTot_", "DgPct_", "DgObs_", "DgRes",
                "SalesCalls_", "SalesVisits_", "SalesQuotes_", "SalesFollowups_", "SalesProspects_", "SalesClients_", "SalesObs_",
                "SalesTot", "SalesCard", "RouteTot",
                "ResAsignadas", "ResCompletadas", "ResPendientes", "ResFuncionalidades", "ResIncidencias", "ResCumplimiento",
                "ResPosts", "ResVideos", "ResTotal", "ResEntregadas", "ResCorrecciones", "ScorePill"
        };
        for (String pat : userCellPatterns) {
            if (idName.contains(pat)) return true;
        }
        return false;
    }

    /**
     * Garantiza que TODAS las hojas tengan en la parte inferior la barra institucional
     * con web, correo (hola@website.pe) y redes (@website.creativo) para que el usuario
     * pueda editar el correo y datos de pie de página en cualquier cargo.
     */
    public void ensureInstitutionalContactFooter(Context context, View sheetRoot) {
        if (!(sheetRoot instanceof ViewGroup)) return;
        ViewGroup rootGroup = (ViewGroup) sheetRoot;

        // Verificar si el XML ya contiene el correo hola@website.pe o el footer dinámico
        if (containsTextRecursive(rootGroup, "hola@website") || rootGroup.findViewWithTag(TAG_DYNAMIC_CONTACT_FOOTER) != null) {
            return;
        }

        LinearLayout contactFooter = new LinearLayout(context);
        contactFooter.setTag(TAG_DYNAMIC_CONTACT_FOOTER);
        contactFooter.setOrientation(LinearLayout.HORIZONTAL);
        contactFooter.setGravity(Gravity.CENTER_VERTICAL);
        contactFooter.setBackgroundColor(Color.parseColor("#EEF2FF"));
        contactFooter.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 22));
        lp.topMargin = dp(context, 2);
        contactFooter.setLayoutParams(lp);

        TextView tvWeb = new TextView(context);
        tvWeb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvWeb.setText("🌐 www.website.pe");
        tvWeb.setTextColor(Color.parseColor("#1E1B4B"));
        tvWeb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6.5f);
        tvWeb.setTypeface(null, Typeface.BOLD);

        TextView tvEmail = new TextView(context);
        tvEmail.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvEmail.setGravity(Gravity.CENTER);
        tvEmail.setText("✉ hola@website.pe");
        tvEmail.setTextColor(Color.parseColor("#1E1B4B"));
        tvEmail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6.5f);
        tvEmail.setTypeface(null, Typeface.BOLD);

        TextView tvSocial = new TextView(context);
        tvSocial.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvSocial.setGravity(Gravity.END);
        tvSocial.setText("📸 @website.creativo");
        tvSocial.setTextColor(Color.parseColor("#1E1B4B"));
        tvSocial.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6.5f);
        tvSocial.setTypeface(null, Typeface.BOLD);

        contactFooter.addView(tvWeb);
        contactFooter.addView(tvEmail);
        contactFooter.addView(tvSocial);
        rootGroup.addView(contactFooter);
    }

    private boolean containsTextRecursive(View view, String needle) {
        if (view instanceof TextView) {
            CharSequence cs = ((TextView) view).getText();
            if (cs != null && cs.toString().toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (containsTextRecursive(vg.getChildAt(i), needle)) return true;
            }
        }
        return false;
    }

    private void populateCleanActivityPlaceholder(LinearLayout container) {
        if (container == null) return;
        container.removeAllViews();
        Context ctx = container.getContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 22)));
        row.setGravity(Gravity.CENTER);
        row.setBackgroundResource(R.drawable.kpi_sigd_cell_data);

        TextView tv = new TextView(ctx);
        tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tv.setGravity(Gravity.CENTER);
        tv.setText("— Espacio para las actividades que registra el usuario en su formulario —");
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6.5f);
        tv.setTypeface(null, Typeface.ITALIC);
        row.addView(tv);
        container.addView(row);
    }

    public void populateCriteriaRows(LinearLayout container, String cargo, String[] defaultCriteria, boolean hasScore) {
        if (container == null) return;
        container.removeAllViews();
        Context ctx = container.getContext();
        for (int i = 0; i < defaultCriteria.length; i++) {
            String savedCrit = getSemanticField(cargo, "eval_criterion_" + i, defaultCriteria[i]);
            String savedPts = getSemanticField(cargo, "eval_criterion_pts_" + i, "5 pts");

            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 4), dp(ctx, 2), dp(ctx, 4), dp(ctx, 2));

            TextView tvCheck = new TextView(ctx);
            tvCheck.setText(hasScore ? "[✓] " : "[ ] ");
            tvCheck.setTextColor(hasScore ? Color.parseColor("#16A34A") : Color.parseColor("#94A3B8"));
            tvCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, 7f);
            tvCheck.setTypeface(null, Typeface.BOLD);
            row.addView(tvCheck);

            TextView tvText = new TextView(ctx);
            tvText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvText.setText(savedCrit);
            tvText.setTextColor(hasScore ? Color.parseColor("#1E1B4B") : Color.parseColor("#64748B"));
            tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6.5f);
            row.addView(tvText);

            TextView tvPts = new TextView(ctx);
            tvPts.setText(hasScore ? ("5 / " + savedPts) : ("0 / " + savedPts));
            tvPts.setTextColor(hasScore ? Color.parseColor("#4338CA") : Color.parseColor("#94A3B8"));
            tvPts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6.5f);
            tvPts.setTypeface(null, hasScore ? Typeface.BOLD : Typeface.NORMAL);
            row.addView(tvPts);

            container.addView(row);
        }
    }

    // =========================================================================
    // 3. MODO INTERACTIVO WYSIWYG EN EL EDITOR (TOCAR CUALQUIER ELEMENTO)
    // =========================================================================

    public void attachInteractiveEditor(Context context,
                                        String cargo,
                                        View sheetRoot,
                                        boolean highlightEditable,
                                        Runnable onAnyChangeCallback) {
        attachInteractiveRecursive(context, cargo, sheetRoot, sheetRoot, highlightEditable, onAnyChangeCallback);
    }

    private void attachInteractiveRecursive(Context context,
                                            String cargo,
                                            View sheetRoot,
                                            View current,
                                            boolean highlightEditable,
                                            Runnable onAnyChangeCallback) {
        if (current == null) return;

        // Si es la imagen del logo personalizado (img1), al tocarla abre el editor de Logo
        if (current instanceof ImageView && TAG_CUSTOM_LOGO_IMAGE.equals(current.getTag())) {
            current.setClickable(true);
            current.setFocusable(true);
            if (highlightEditable) {
                GradientDrawable hintBg = new GradientDrawable();
                hintBg.setColor(Color.parseColor("#124F46E5"));
                hintBg.setStroke(dp(context, 1.5f), Color.parseColor("#4F46E5"), dp(context, 3), dp(context, 2));
                hintBg.setCornerRadius(dp(context, 4));
                current.setBackground(hintBg);
            } else {
                current.setBackground(null);
            }
            current.setOnClickListener(v -> showLogoEditorDialog(
                    context, cargo, sheetRoot, highlightEditable, onAnyChangeCallback
            ));
            return;
        }

        if (current instanceof TextView && !(current instanceof EditText)) {
            TextView tv = (TextView) current;
            String idName = getIdEntryName(context, tv);

            // No interceptar los botones de navegación de pestañas internas
            if ("btnSalesTabKpiGoals".equals(idName) || "btnSalesTabRoute".equals(idName)
                    || idName.startsWith("btnProgDay") || idName.startsWith("btnDesDay")) {
                return;
            }

            String text = tv.getText() != null ? tv.getText().toString().trim() : "";

            // Celdas numéricas de usuario ("—") muestran aviso informativo al tocarlas
            if (isUserGeneratedDataCellId(idName) || "—".equals(text)
                    || text.contains("Espacio para las actividades que registra el usuario")
                    || "[ ]".equals(text) || "[✓]".equals(text)) {
                tv.setOnClickListener(v -> Toast.makeText(
                        context,
                        "ℹ Esta celda se llena automáticamente con los datos que registra el usuario en su formulario KPI.",
                        Toast.LENGTH_SHORT
                ).show());
                return;
            }

            // Verificar si es parte del Logo Institucional (img1: "website+" o "Agencia Creativa")
            final String instKey = getInstitutionalKeyForTextView(tv);
            if (INST_LOGO_TITLE.equals(instKey) || INST_LOGO_SUBTITLE.equals(instKey)) {
                boolean isLogoMod = getInstitutionalValue(cargo, INST_LOGO_IMAGE_PATH) != null
                        || getInstitutionalValue(cargo, INST_LOGO_TITLE) != null
                        || getInstitutionalValue(cargo, INST_LOGO_SUBTITLE) != null;
                applyEditableHighlight(context, tv, highlightEditable, isLogoMod);
                tv.setClickable(true);
                tv.setFocusable(true);
                tv.setOnClickListener(v -> showLogoEditorDialog(
                        context, cargo, sheetRoot, highlightEditable, onAnyChangeCallback
                ));
                return;
            }

            // Es un elemento de la plantilla (título, meta, actividad, criterio, porcentaje, correo, etc.)
            final String viewKey = getViewKey(context, sheetRoot, tv);
            boolean isModified = getViewOverrides(cargo).containsKey(viewKey)
                    || (instKey != null && getInstitutionalValue(cargo, instKey) != null);
            applyEditableHighlight(context, tv, highlightEditable, isModified);

            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(v -> showElementEditorDialog(
                    context, cargo, sheetRoot, tv, viewKey, highlightEditable, onAnyChangeCallback
            ));
            return;
        }

        if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            for (int i = 0; i < vg.getChildCount(); i++) {
                attachInteractiveRecursive(context, cargo, sheetRoot, vg.getChildAt(i), highlightEditable, onAnyChangeCallback);
            }
        }
    }

    private void applyEditableHighlight(Context context, TextView tv, boolean highlightEditable, boolean isModified) {
        if (!highlightEditable && !isModified) {
            return;
        }
        // Si el TextView tiene un color de fondo oscuro (#2E2A72, #1E1B4B, #4C1D95, etc.), mantenemos su legibilidad
        int textColor = tv.getCurrentTextColor();
        boolean isLightText = Color.red(textColor) > 200 && Color.green(textColor) > 200 && Color.blue(textColor) > 200;

        if (isModified && !isLightText) {
            GradientDrawable modBg = new GradientDrawable();
            modBg.setColor(Color.parseColor("#EEF2FF"));
            modBg.setStroke(dp(context, 1), Color.parseColor("#4F46E5"));
            modBg.setCornerRadius(dp(context, 3));
            tv.setBackground(modBg);
        } else if (highlightEditable && !isLightText && tv.getBackground() == null) {
            GradientDrawable hintBg = new GradientDrawable();
            hintBg.setColor(Color.parseColor("#084F46E5"));
            hintBg.setStroke(dp(context, 1), Color.parseColor("#336366F1"), dp(context, 3), dp(context, 2));
            hintBg.setCornerRadius(dp(context, 3));
            tv.setBackground(hintBg);
        }
    }

    private void showElementEditorDialog(Context context,
                                         String cargo,
                                         View sheetRoot,
                                         TextView targetTv,
                                         String viewKey,
                                         boolean highlightEditable,
                                         Runnable onAnyChangeCallback) {
        String currentText = targetTv.getText() != null ? targetTv.getText().toString() : "";
        Object origObj = targetTv.getTag(R.id.llOfficialSheetRoot);
        final String originalText = origObj instanceof String ? (String) origObj : currentText;

        String semanticDescription = describeElementContext(context, targetTv);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 18);
        root.setPadding(pad, dp(context, 16), pad, dp(context, 12));
        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(Color.WHITE);
        dialogBg.setCornerRadius(dp(context, 18));
        root.setBackground(dialogBg);

        // Encabezado
        TextView tvBadge = new TextView(context);
        tvBadge.setText("✎ EDITAR ELEMENTO DE LA HOJA OFICIAL (" + cargo.toUpperCase(Locale.getDefault()) + ")");
        tvBadge.setTextColor(Color.parseColor("#4F46E5"));
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        root.addView(tvBadge);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(semanticDescription);
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(context, 4);
        root.addView(tvTitle, titleLp);

        TextView tvSub = new TextView(context);
        tvSub.setText("Se actualizará en el Reporte Oficial actual y en el Formulario del usuario (sin alterar semanas pasadas).");
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(context, 2);
        subLp.bottomMargin = dp(context, 10);
        root.addView(tvSub, subLp);

        // Campo de texto editable (si dice "(Vacío)", se abre limpio para escribir directo)
        final EditText etValue = new EditText(context);
        String initialEditValue = currentText;
        if ("(Vacío)".equalsIgnoreCase(currentText.trim())) {
            initialEditValue = "";
        } else if ("☐  (Vacío)".equalsIgnoreCase(currentText.trim()) || "☑  (Vacío)".equalsIgnoreCase(currentText.trim())) {
            initialEditValue = "☑  ";
        } else if (currentText.endsWith("(Vacío)")) {
            initialEditValue = currentText.replace("(Vacío)", "").trim() + " ";
        }
        etValue.setHint("Escribe aquí el contenido para este campo...");
        etValue.setText(initialEditValue);
        etValue.setTextColor(Color.parseColor("#0F172A"));
        etValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        etValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etValue.setMinLines(currentText.contains("\n") || currentText.length() > 45 ? 3 : 1);
        etValue.setMaxLines(6);
        etValue.setGravity(Gravity.TOP | Gravity.START);
        int etPad = dp(context, 12);
        etValue.setPadding(etPad, etPad, etPad, etPad);
        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(Color.parseColor("#F8FAFC"));
        etBg.setStroke(dp(context, 1.5f), Color.parseColor("#6366F1"));
        etBg.setCornerRadius(dp(context, 10));
        etValue.setBackground(etBg);
        etValue.setSelection(etValue.getText().length());
        root.addView(etValue, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Barra de símbolos rápidos (para correos, metas ≥, %, check, etc.)
        HorizontalScrollView symScroll = new HorizontalScrollView(context);
        symScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout symRow = new LinearLayout(context);
        symRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] symbols = {"≥ ", "≤ ", "%", " – ", "@", "✉ ", "🌐 ", "📸 ", "☑  ", "☐  ", "★ ", "• "};
        for (String sym : symbols) {
            TextView chip = new TextView(context);
            chip.setText(sym.trim());
            chip.setTextColor(Color.parseColor("#3730A3"));
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(Color.parseColor("#EEF2FF"));
            chipBg.setCornerRadius(dp(context, 8));
            chipBg.setStroke(dp(context, 1), Color.parseColor("#C7D2FE"));
            chip.setBackground(chipBg);
            chip.setOnClickListener(v -> {
                int start = Math.max(etValue.getSelectionStart(), 0);
                int end = Math.max(etValue.getSelectionEnd(), 0);
                etValue.getText().replace(Math.min(start, end), Math.max(start, end), sym, 0, sym.length());
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.rightMargin = dp(context, 6);
            symRow.addView(chip, cp);
        }
        symScroll.addView(symRow);
        LinearLayout.LayoutParams symScrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        symScrollLp.topMargin = dp(context, 8);
        root.addView(symScroll, symScrollLp);

        // Fila de herramientas rápidas: Teclado Interno SIGD + Restaurar original
        LinearLayout toolsRow = new LinearLayout(context);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams toolsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toolsLp.topMargin = dp(context, 8);

        TextView btnCustomKeypad = new TextView(context);
        btnCustomKeypad.setText("⌨ Usar Teclado Interno SIGD");
        btnCustomKeypad.setTextColor(Color.parseColor("#4F46E5"));
        btnCustomKeypad.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnCustomKeypad.setTypeface(null, Typeface.BOLD);
        btnCustomKeypad.setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));
        btnCustomKeypad.setOnClickListener(v -> KpiCustomKeypadDialog.showText(
                context,
                semanticDescription,
                "Editar texto de la plantilla",
                etValue.getText().toString(),
                newText -> {
                    etValue.setText(newText);
                    etValue.setSelection(etValue.getText().length());
                }
        ));
        toolsRow.addView(btnCustomKeypad, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnRestoreCell = new TextView(context);
        btnRestoreCell.setText("↺ Valor original");
        btnRestoreCell.setTextColor(Color.parseColor("#64748B"));
        btnRestoreCell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnRestoreCell.setTypeface(null, Typeface.BOLD);
        btnRestoreCell.setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));
        btnRestoreCell.setOnClickListener(v -> {
            etValue.setText(originalText);
            etValue.setSelection(etValue.getText().length());
        });
        toolsRow.addView(btnRestoreCell);
        root.addView(toolsRow, toolsLp);

        // Botones de acción (Cancelar / Aplicar)
        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(context, 14);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cancelar");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setPadding(dp(context, 14), dp(context, 10), dp(context, 14), dp(context, 10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        actionsRow.addView(btnCancel);

        TextView btnApply = new TextView(context);
        btnApply.setText("✓ Aplicar Cambio");
        btnApply.setTextColor(Color.WHITE);
        btnApply.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnApply.setTypeface(null, Typeface.BOLD);
        btnApply.setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 10));
        btnApply.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnApply.setOnClickListener(v -> {
            String updated = etValue.getText().toString().trim();
            if (updated.isEmpty()) {
                updated = originalText;
            }
            final String finalUpdated = updated;
            final String instKey = getInstitutionalKeyForTextView(targetTv);

            dialog.dismiss();

            // Si es un apartado institucional (img1 o img2: pie de página / correo / web / redes / SIGD),
            // preguntamos si desea cambiarlo en TODOS los KPIs o solo en este cargo.
            if (instKey != null) {
                showInstitutionalScopeDialog(
                        context,
                        cargo,
                        semanticDescription,
                        finalUpdated,
                        applyToAll -> {
                            saveInstitutionalValue(cargo, instKey, finalUpdated, applyToAll);
                            if (!applyToAll) {
                                saveViewOverride(cargo, viewKey, finalUpdated);
                            } else {
                                removeViewOverride(cargo, viewKey);
                            }
                            applyInstitutionalLogoAndFooterToSheet(context, cargo, sheetRoot);
                            applyEditableHighlight(context, targetTv, highlightEditable, true);
                            if (onAnyChangeCallback != null) {
                                onAnyChangeCallback.run();
                            }
                            Toast.makeText(
                                    context,
                                    applyToAll
                                            ? "✓ Actualizado en TODOS los reportes KPI (13 cargos)"
                                            : "✓ Actualizado solo en el reporte de " + cargo,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
                return;
            }

            targetTv.setText(finalUpdated);
            saveViewOverride(cargo, viewKey, finalUpdated);
            extractAndSaveSemanticFieldFromEdit(context, cargo, sheetRoot, targetTv, finalUpdated);
            syncTemplateToRoleForms(context, cargo);
            applyEditableHighlight(context, targetTv, highlightEditable, true);
            if (onAnyChangeCallback != null) {
                onAnyChangeCallback.run();
            }
        });
        actionsRow.addView(btnApply);
        root.addView(actionsRow, actLp);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    public interface ScopeDecisionCallback {
        void onScopeSelected(boolean applyToAllCargos);
    }

    /**
     * Muestra el diálogo que pregunta al usuario si el cambio en el Logo (img1) o Pie de Página (img2)
     * debe aplicarse a TODOS los KPIs (los 13 cargos) o únicamente al cargo actual.
     */
    public void showInstitutionalScopeDialog(Context context,
                                             String cargo,
                                             String elementTitle,
                                             String previewValue,
                                             ScopeDecisionCallback callback) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 18);
        root.setPadding(pad, dp(context, 18), pad, dp(context, 14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(context, 18));
        root.setBackground(bg);

        TextView tvBadge = new TextView(context);
        tvBadge.setText("⚡ ALCANCE DEL CAMBIO INSTITUCIONAL");
        tvBadge.setTextColor(Color.parseColor("#4F46E5"));
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        root.addView(tvBadge);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("¿Cambiar en todos los KPIs o solo en este?");
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(context, 4);
        root.addView(tvTitle, tLp);

        TextView tvDesc = new TextView(context);
        tvDesc.setText("Estás modificando \"" + elementTitle + "\""
                + (previewValue != null && !previewValue.isEmpty() ? ":\n\"" + previewValue + "\"" : ".")
                + "\n\nElige dónde deseas aplicar este cambio:");
        tvDesc.setTextColor(Color.parseColor("#475569"));
        tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dLp.topMargin = dp(context, 8);
        dLp.bottomMargin = dp(context, 14);
        root.addView(tvDesc, dLp);

        AlertDialog scopeDialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();

        // Opción 1: Aplicar en TODOS los KPIs (los 13 cargos)
        TextView btnAll = new TextView(context);
        btnAll.setText("🌐 Cambiar en TODOS los KPIs (Los 13 cargos)");
        btnAll.setTextColor(Color.WHITE);
        btnAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnAll.setTypeface(null, Typeface.BOLD);
        btnAll.setGravity(Gravity.CENTER);
        btnAll.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        btnAll.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnAll.setOnClickListener(v -> {
            scopeDialog.dismiss();
            if (callback != null) callback.onScopeSelected(true);
        });
        root.addView(btnAll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Opción 2: Aplicar SOLO en este cargo
        TextView btnOnlyThis = new TextView(context);
        btnOnlyThis.setText("📌 Cambiar SOLO en este KPI (" + cargo + ")");
        btnOnlyThis.setTextColor(Color.parseColor("#1E1B4B"));
        btnOnlyThis.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnOnlyThis.setTypeface(null, Typeface.BOLD);
        btnOnlyThis.setGravity(Gravity.CENTER);
        btnOnlyThis.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        GradientDrawable onlyBg = new GradientDrawable();
        onlyBg.setColor(Color.parseColor("#EEF2FF"));
        onlyBg.setStroke(dp(context, 1.5f), Color.parseColor("#6366F1"));
        onlyBg.setCornerRadius(dp(context, 10));
        btnOnlyThis.setBackground(onlyBg);
        LinearLayout.LayoutParams onlyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        onlyLp.topMargin = dp(context, 8);
        btnOnlyThis.setOnClickListener(v -> {
            scopeDialog.dismiss();
            if (callback != null) callback.onScopeSelected(false);
        });
        root.addView(btnOnlyThis, onlyLp);

        // Cancelar
        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cancelar");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 6));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = dp(context, 6);
        btnCancel.setOnClickListener(v -> scopeDialog.dismiss());
        root.addView(btnCancel, cLp);

        if (scopeDialog.getWindow() != null) {
            scopeDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        scopeDialog.show();
    }

    /**
     * Diálogo dedicado para el apartado del Logo Institucional (img1):
     * Permite subir una foto desde la galería para ponerla como logo, restaurar al texto,
     * o editar el texto del logo, preguntando siempre si aplica a todos los KPIs o solo a este.
     */
    private void showLogoEditorDialog(Context context,
                                      String cargo,
                                      View sheetRoot,
                                      boolean highlightEditable,
                                      Runnable onAnyChangeCallback) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 18);
        root.setPadding(pad, dp(context, 16), pad, dp(context, 14));
        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(Color.WHITE);
        dialogBg.setCornerRadius(dp(context, 18));
        root.setBackground(dialogBg);

        TextView tvBadge = new TextView(context);
        tvBadge.setText("🖼 LOGO INSTITUCIONAL DEL REPORTE (" + cargo.toUpperCase(Locale.getDefault()) + ")");
        tvBadge.setTextColor(Color.parseColor("#4F46E5"));
        tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvBadge.setTypeface(null, Typeface.BOLD);
        root.addView(tvBadge);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("Personalizar Logo de Cabecera");
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(context, 4);
        root.addView(tvTitle, titleLp);

        TextView tvSub = new TextView(context);
        tvSub.setText("Prioriza archivos PNG transparentes o convierte cualquier foto de tu galería automáticamente a PNG limpio.");
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(context, 2);
        subLp.bottomMargin = dp(context, 12);
        root.addView(tvSub, subLp);

        // Vista previa si ya hay una foto cargada
        String currentLogoPath = getInstitutionalLogoImagePath(cargo);
        if (currentLogoPath != null && !currentLogoPath.isEmpty() && new File(currentLogoPath).exists()) {
            LinearLayout previewBox = new LinearLayout(context);
            previewBox.setOrientation(LinearLayout.VERTICAL);
            previewBox.setGravity(Gravity.CENTER);
            previewBox.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
            GradientDrawable prevBg = new GradientDrawable();
            prevBg.setColor(Color.parseColor("#F8FAFC"));
            prevBg.setStroke(dp(context, 1), Color.parseColor("#CBD5E1"));
            prevBg.setCornerRadius(dp(context, 10));
            previewBox.setBackground(prevBg);

            ImageView ivPreview = new ImageView(context);
            ivPreview.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 120), dp(context, 52)));
            ivPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ivPreview.setForceDarkAllowed(false);
            }
            Bitmap bmp = loadCleanLogoBitmap(currentLogoPath);
            if (bmp != null) {
                ivPreview.setImageBitmap(bmp);
            }
            previewBox.addView(ivPreview);

            TextView tvPrevLabel = new TextView(context);
            tvPrevLabel.setText("Logo PNG actual activo");
            tvPrevLabel.setTextColor(Color.parseColor("#475569"));
            tvPrevLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            previewBox.addView(tvPrevLabel);

            LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            prevLp.bottomMargin = dp(context, 10);
            root.addView(previewBox, prevLp);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(root)
                .create();

        // Botón principal: SUBIR FOTO / LOGO PNG DESDE GALERÍA
        TextView btnUploadPhoto = new TextView(context);
        btnUploadPhoto.setText("📷 Subir Logo PNG (o Convertir Foto a PNG)");
        btnUploadPhoto.setTextColor(Color.WHITE);
        btnUploadPhoto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        btnUploadPhoto.setTypeface(null, Typeface.BOLD);
        btnUploadPhoto.setGravity(Gravity.CENTER);
        btnUploadPhoto.setPadding(dp(context, 16), dp(context, 12), dp(context, 16), dp(context, 12));
        btnUploadPhoto.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnUploadPhoto.setOnClickListener(v -> {
            dialog.dismiss();
            if (logoPickRequestListener != null) {
                logoPickRequestListener.onRequestPickLogoImage(cargo);
            } else {
                Toast.makeText(context, "Selector de imágenes no disponible en esta vista", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnUploadPhoto, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Si hay foto activa, botón para quitar la foto y volver al logo de texto
        if (currentLogoPath != null && !currentLogoPath.isEmpty()) {
            TextView btnRemovePhoto = new TextView(context);
            btnRemovePhoto.setText("🗑 Quitar foto y usar texto del logo");
            btnRemovePhoto.setTextColor(Color.parseColor("#DC2626"));
            btnRemovePhoto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            btnRemovePhoto.setTypeface(null, Typeface.BOLD);
            btnRemovePhoto.setGravity(Gravity.CENTER);
            btnRemovePhoto.setPadding(dp(context, 12), dp(context, 9), dp(context, 12), dp(context, 9));
            GradientDrawable remBg = new GradientDrawable();
            remBg.setColor(Color.parseColor("#FEF2F2"));
            remBg.setStroke(dp(context, 1), Color.parseColor("#FECACA"));
            remBg.setCornerRadius(dp(context, 8));
            btnRemovePhoto.setBackground(remBg);
            LinearLayout.LayoutParams remLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            remLp.topMargin = dp(context, 8);
            btnRemovePhoto.setOnClickListener(v -> {
                dialog.dismiss();
                showInstitutionalScopeDialog(
                        context,
                        cargo,
                        "Restaurar Logo de Texto",
                        "website+ / Agencia Creativa",
                        applyToAll -> {
                            saveInstitutionalLogoImage(cargo, "", applyToAll);
                            applyInstitutionalLogoAndFooterToSheet(context, cargo, sheetRoot);
                            attachInteractiveEditor(context, cargo, sheetRoot, highlightEditable, onAnyChangeCallback);
                            if (onAnyChangeCallback != null) onAnyChangeCallback.run();
                        }
                );
            });
            root.addView(btnRemovePhoto, remLp);
        }

        // Sección opcional: Editar el texto del logo ("website+" / "Agencia Creativa")
        TextView tvOrText = new TextView(context);
        tvOrText.setText("O EDITAR EL TEXTO DEL LOGO:");
        tvOrText.setTextColor(Color.parseColor("#64748B"));
        tvOrText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        tvOrText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams orLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        orLp.topMargin = dp(context, 14);
        orLp.bottomMargin = dp(context, 6);
        root.addView(tvOrText, orLp);

        String currentMainTitle = getInstitutionalValue(cargo, INST_LOGO_TITLE);
        if (currentMainTitle == null || currentMainTitle.isEmpty()) currentMainTitle = "website+";
        String currentSubTitle = getInstitutionalValue(cargo, INST_LOGO_SUBTITLE);
        if (currentSubTitle == null || currentSubTitle.isEmpty()) currentSubTitle = "Agencia Creativa";

        final EditText etLogoTitle = new EditText(context);
        etLogoTitle.setHint("Texto principal (ej. website+)");
        etLogoTitle.setText(currentMainTitle);
        etLogoTitle.setTextColor(Color.parseColor("#0F172A"));
        etLogoTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        int etPad = dp(context, 10);
        etLogoTitle.setPadding(etPad, etPad, etPad, etPad);
        GradientDrawable etBg1 = new GradientDrawable();
        etBg1.setColor(Color.parseColor("#F8FAFC"));
        etBg1.setStroke(dp(context, 1), Color.parseColor("#CBD5E1"));
        etBg1.setCornerRadius(dp(context, 8));
        etLogoTitle.setBackground(etBg1);
        root.addView(etLogoTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText etLogoSub = new EditText(context);
        etLogoSub.setHint("Subtítulo (ej. Agencia Creativa)");
        etLogoSub.setText(currentSubTitle);
        etLogoSub.setTextColor(Color.parseColor("#0F172A"));
        etLogoSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        etLogoSub.setPadding(etPad, etPad, etPad, etPad);
        GradientDrawable etBg2 = new GradientDrawable();
        etBg2.setColor(Color.parseColor("#F8FAFC"));
        etBg2.setStroke(dp(context, 1), Color.parseColor("#CBD5E1"));
        etBg2.setCornerRadius(dp(context, 8));
        etLogoSub.setBackground(etBg2);
        LinearLayout.LayoutParams subEtLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subEtLp.topMargin = dp(context, 6);
        root.addView(etLogoSub, subEtLp);

        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(context, 12);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cerrar");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        actionsRow.addView(btnCancel);

        TextView btnSaveText = new TextView(context);
        btnSaveText.setText("✓ Usar este Texto");
        btnSaveText.setTextColor(Color.parseColor("#4F46E5"));
        btnSaveText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        btnSaveText.setTypeface(null, Typeface.BOLD);
        btnSaveText.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8));
        GradientDrawable saveTxtBg = new GradientDrawable();
        saveTxtBg.setColor(Color.parseColor("#EEF2FF"));
        saveTxtBg.setStroke(dp(context, 1), Color.parseColor("#6366F1"));
        saveTxtBg.setCornerRadius(dp(context, 8));
        btnSaveText.setBackground(saveTxtBg);
        btnSaveText.setOnClickListener(v -> {
            String newMain = etLogoTitle.getText().toString().trim();
            String newSub = etLogoSub.getText().toString().trim();
            if (newMain.isEmpty()) newMain = "website+";
            if (newSub.isEmpty()) newSub = "Agencia Creativa";
            final String fMain = newMain;
            final String fSub = newSub;
            dialog.dismiss();

            showInstitutionalScopeDialog(
                    context,
                    cargo,
                    "Texto del Logo Institucional",
                    fMain + " • " + fSub,
                    applyToAll -> {
                        saveInstitutionalLogoImage(cargo, "", applyToAll);
                        saveInstitutionalValue(cargo, INST_LOGO_TITLE, fMain, applyToAll);
                        saveInstitutionalValue(cargo, INST_LOGO_SUBTITLE, fSub, applyToAll);
                        applyInstitutionalLogoAndFooterToSheet(context, cargo, sheetRoot);
                        attachInteractiveEditor(context, cargo, sheetRoot, highlightEditable, onAnyChangeCallback);
                        if (onAnyChangeCallback != null) onAnyChangeCallback.run();
                        Toast.makeText(
                                context,
                                applyToAll
                                        ? "✓ Logo actualizado en TODOS los KPIs (13 cargos)"
                                        : "✓ Logo actualizado solo en " + cargo,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
            );
        });
        actionsRow.addView(btnSaveText);
        root.addView(actionsRow, actLp);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private String describeElementContext(Context context, TextView tv) {
        String idName = getIdEntryName(context, tv);
        String text = tv.getText() != null ? tv.getText().toString().replace("\n", " ").trim() : "";

        if (text.contains("@") || text.contains("www.") || text.contains("✉") || text.contains("🌐") || text.contains("📸")) {
            return "Pie de Página / Contacto Institucional";
        }
        if (idName.contains("Supervisor") || text.toLowerCase(Locale.ROOT).startsWith("responsable") || text.toLowerCase(Locale.ROOT).startsWith("evaluador")) {
            return "Responsable de Evaluación / Supervisor";
        }
        if (idName.contains("Area") || text.toLowerCase(Locale.ROOT).startsWith("área:")) {
            return "Área Asignada del Cargo";
        }
        if (idName.contains("Boss") || text.toLowerCase(Locale.ROOT).startsWith("jefe")) {
            return "Jefe Inmediato";
        }
        if (idName.contains("Role") || text.toLowerCase(Locale.ROOT).startsWith("cargo:")) {
            return "Nombre / Subtítulo del Cargo";
        }
        if (idName.contains("Shift") || text.toLowerCase(Locale.ROOT).startsWith("turno:")) {
            return "Configuración de Turno / Horario";
        }
        if (idName.contains("GeneralObservations") || idName.contains("Achievements") || idName.contains("Difficulties") || idName.contains("ImprovementPlan")) {
            return "Plantilla de Observaciones / Notas Semanales";
        }

        // Verificar si está en una fila de día (Lunes a Sábado)
        if (tv.getParent() instanceof ViewGroup) {
            ViewGroup row = (ViewGroup) tv.getParent();
            int dayIdx = detectDayRowIndex(row);
            if (dayIdx >= 0) {
                int childIdx = row.indexOfChild(tv);
                String[] dayNames = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
                if (childIdx == 1) {
                    return "Actividad / Modalidad Principal (" + dayNames[dayIdx] + ")";
                } else if (childIdx == 2) {
                    return "Meta Diaria Exigida (" + dayNames[dayIdx] + ")";
                }
            }
        }

        if (text.startsWith("☑") || text.startsWith("☐")) {
            return "Acción / Criterio de Verificación del Cargo";
        }
        if (text.contains("%")) {
            return "Rango de Porcentaje / Escala de Evaluación KPI";
        }
        if (text.contains("≥") || text.contains("≤")) {
            return "Meta de Producción Esperada";
        }
        return "Texto / Meta de la Plantilla Oficial";
    }

    // =========================================================================
    // 4. EXTRACCIÓN SEMÁNTICA AUTOMÁTICA Y SINCRONIZACIÓN CON FORMULARIOS
    // =========================================================================

    private void extractAndSaveSemanticFieldFromEdit(Context context,
                                                     String cargo,
                                                     View sheetRoot,
                                                     TextView editedTv,
                                                     String newText) {
        String idName = getIdEntryName(context, editedTv);

        // 1. Metadatos de cabecera (Área, Evaluador/Supervisor, Jefe inmediato, Turno)
        if (idName.contains("Area") || newText.toLowerCase(Locale.ROOT).startsWith("área:")) {
            String val = stripPrefix(newText, ":");
            saveSemanticField(cargo, "header_area", val);
            updateCollaboratorProfileField(context, cargo, "area", val);
        } else if (idName.contains("Supervisor") || newText.toLowerCase(Locale.ROOT).startsWith("responsable") || newText.toLowerCase(Locale.ROOT).startsWith("evaluador")) {
            String val = stripPrefix(newText, ":");
            saveSemanticField(cargo, "header_supervisor", val);
            updateCollaboratorProfileField(context, cargo, "supervisor", val);
        } else if (idName.contains("Boss") || newText.toLowerCase(Locale.ROOT).startsWith("jefe")) {
            String val = stripPrefix(newText, ":");
            saveSemanticField(cargo, "header_boss", val);
            updateCollaboratorProfileField(context, cargo, "boss", val);
        } else if (idName.contains("GeneralObservations")) {
            saveSemanticField(cargo, "general_observations", newText);
        }

        // 2. Filas diarias de Lunes a Sábado (Actividad Principal y Meta Diaria)
        if (editedTv.getParent() instanceof ViewGroup) {
            ViewGroup parentRow = (ViewGroup) editedTv.getParent();
            int dayIdx = detectDayRowIndex(parentRow);
            if (dayIdx >= 0) {
                int childIdx = parentRow.indexOfChild(editedTv);
                if (childIdx == 1) {
                    saveSemanticField(cargo, "day_activity_" + dayIdx, newText);
                } else if (childIdx == 2) {
                    saveSemanticField(cargo, "day_goal_label_" + dayIdx, newText);
                    int parsedGoal = parseExpectedNumberFromGoalText(newText, -1);
                    if (parsedGoal > 0) {
                        saveSemanticField(cargo, "day_expected_total_" + dayIdx, String.valueOf(parsedGoal));
                    }
                }
            }

            // 3. Encabezados de las 3 columnas de métricas diarias
            if (isTableHeaderRow(parentRow)) {
                int childIdx = parentRow.indexOfChild(editedTv);
                if (childIdx >= 3 && childIdx <= 5) {
                    saveSemanticField(cargo, "metric_header_" + (childIdx - 3), newText.replace("\n", " ").trim());
                }
            }

            // 4. Checklist de Acciones (Sección 3)
            if (newText.startsWith("☑") || newText.startsWith("☐") || editedTv.getId() != View.NO_ID && idName.startsWith("cb")) {
                int actionIdx = parentRow.indexOfChild(editedTv);
                if (actionIdx >= 0 && actionIdx < 7) {
                    String cleanAction = newText.replace("☑", "").replace("☐", "").trim();
                    saveSemanticField(cargo, "action_item_" + actionIdx, cleanAction);
                }
            }

            // 5. Criterios de Evaluación de Practicantes / Juniors (Sección 5)
            View evalM1 = sheetRoot.findViewById(R.id.llProgM_EvalCriteriaContainer);
            View evalT1 = sheetRoot.findViewById(R.id.llProgT_EvalCriteriaContainer);
            View evalM2 = sheetRoot.findViewById(R.id.llDesM_EvalCriteriaContainer);
            View evalT2 = sheetRoot.findViewById(R.id.llDesT_EvalCriteriaContainer);
            if (parentRow.getParent() == evalM1 || parentRow.getParent() == evalT1
                    || parentRow.getParent() == evalM2 || parentRow.getParent() == evalT2) {
                ViewGroup criteriaContainer = (ViewGroup) parentRow.getParent();
                int critIdx = criteriaContainer.indexOfChild(parentRow);
                if (critIdx >= 0 && critIdx < 6) {
                    if (parentRow.indexOfChild(editedTv) == 1) {
                        saveSemanticField(cargo, "eval_criterion_" + critIdx, newText);
                    } else if (parentRow.indexOfChild(editedTv) == 2) {
                        saveSemanticField(cargo, "eval_criterion_pts_" + critIdx, newText.replace("0 / ", "").replace("5 / ", "").trim());
                    }
                }
            }
        }

        // 6. Escanear tarjetas de metas de Programador Junior, Diseñador Junior y Asesor de Ventas
        extractSpecialRoleGoalsFromSheet(cargo, sheetRoot);
    }

    private void extractSpecialRoleGoalsFromSheet(String cargo, View sheetRoot) {
        String key = normalizeCargoKey(cargo);
        if ("PROGRAMADOR_JUNIOR".equals(key)) {
            // Extraer las 3 tarjetas de metas de producción de Turno Mañana (hijo índice 3 de llProgShiftMorningContainer)
            View morningContainer = sheetRoot.findViewById(R.id.llProgShiftMorningContainer);
            if (morningContainer instanceof ViewGroup && ((ViewGroup) morningContainer).getChildCount() > 3) {
                View cardsRow = ((ViewGroup) morningContainer).getChildAt(3);
                if (cardsRow instanceof ViewGroup) {
                    ViewGroup vgCards = (ViewGroup) cardsRow;
                    for (int i = 0; i < Math.min(3, vgCards.getChildCount()); i++) {
                        View card = vgCards.getChildAt(i);
                        if (card instanceof ViewGroup && ((ViewGroup) card).getChildCount() >= 2) {
                            View v0 = ((ViewGroup) card).getChildAt(0);
                            View v1 = ((ViewGroup) card).getChildAt(1);
                            if (v0 instanceof TextView && v1 instanceof TextView) {
                                String mainTitle = ((TextView) v0).getText().toString().trim();
                                String subDesc = ((TextView) v1).getText().toString().trim();
                                saveSemanticField(cargo, "prog_goal_title_" + i, mainTitle);
                                saveSemanticField(cargo, "prog_goal_desc_" + i, subDesc);
                            }
                        }
                    }
                }
            }

            // Extraer criterios de evaluación
            ViewGroup evalContainer = sheetRoot.findViewById(R.id.llProgM_EvalCriteriaContainer);
            if (evalContainer != null && evalContainer.getChildCount() > 0) {
                for (int i = 0; i < evalContainer.getChildCount(); i++) {
                    View row = evalContainer.getChildAt(i);
                    if (row instanceof ViewGroup && ((ViewGroup) row).getChildCount() >= 2) {
                        View tv = ((ViewGroup) row).getChildAt(1);
                        if (tv instanceof TextView) {
                            String cText = ((TextView) tv).getText().toString().trim();
                            saveSemanticField(cargo, "eval_criterion_" + i, cText);
                        }
                    }
                }
            }
        } else if ("DISENADOR_GRAFICO_JUNIOR".equals(key)) {
            View morningContainer = sheetRoot.findViewById(R.id.llDesShiftMorningContainer);
            if (morningContainer instanceof ViewGroup && ((ViewGroup) morningContainer).getChildCount() > 3) {
                View cardsRow = ((ViewGroup) morningContainer).getChildAt(3);
                if (cardsRow instanceof ViewGroup) {
                    ViewGroup vgCards = (ViewGroup) cardsRow;
                    for (int i = 0; i < Math.min(3, vgCards.getChildCount()); i++) {
                        View card = vgCards.getChildAt(i);
                        if (card instanceof ViewGroup && ((ViewGroup) card).getChildCount() >= 2) {
                            View v0 = ((ViewGroup) card).getChildAt(0);
                            View v1 = ((ViewGroup) card).getChildAt(1);
                            if (v0 instanceof TextView && v1 instanceof TextView) {
                                saveSemanticField(cargo, "jr_des_goal_title_" + i, ((TextView) v0).getText().toString().trim());
                                saveSemanticField(cargo, "jr_des_goal_desc_" + i, ((TextView) v1).getText().toString().trim());
                            }
                        }
                    }
                }
            }
        } else if ("ASESOR_DE_VENTAS".equals(key)) {
            View goalsContainer = sheetRoot.findViewById(R.id.llSalesContainerKpiGoals);
            if (goalsContainer instanceof ViewGroup && ((ViewGroup) goalsContainer).getChildCount() > 2) {
                View cardsRow = ((ViewGroup) goalsContainer).getChildAt(2);
                if (cardsRow instanceof ViewGroup) {
                    ViewGroup vgCards = (ViewGroup) cardsRow;
                    for (int i = 0; i < Math.min(6, vgCards.getChildCount()); i++) {
                        View card = vgCards.getChildAt(i);
                        if (card instanceof ViewGroup && ((ViewGroup) card).getChildCount() >= 2) {
                            View v0 = ((ViewGroup) card).getChildAt(0);
                            View v1 = ((ViewGroup) card).getChildAt(1);
                            if (v0 instanceof TextView && v1 instanceof TextView) {
                                saveSemanticField(cargo, "sales_goal_num_" + i, ((TextView) v0).getText().toString().trim());
                                saveSemanticField(cargo, "sales_goal_label_" + i, ((TextView) v1).getText().toString().trim());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sincroniza la plantilla activa del cargo con los gestores de almacenamiento de los
     * formularios de usuario (SOLO en la semana en curso y en días NO bloqueados/enviados,
     * protegiendo las semanas anteriores e históricas).
     */
    public void syncTemplateToRoleForms(Context context, String cargo) {
        String key = normalizeCargoKey(cargo);

        switch (key) {
            case "ADMINISTRADOR": {
                KpiAdministratorStorageManager sm = KpiAdministratorStorageManager.getInstance(context);
                KpiAdministratorWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiAdministratorDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                            sm.saveDayRecord(d);
                        }
                    }
                }
                break;
            }
            case "CONTADOR": {
                KpiAccountantStorageManager sm = KpiAccountantStorageManager.getInstance(context);
                KpiAccountantWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiAccountantDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                            sm.saveDayRecord(d);
                        }
                    }
                }
                break;
            }
            case "ASISTENTE_ADMINISTRATIVO": {
                KpiAdminAssistantStorageManager sm = KpiAdminAssistantStorageManager.getInstance(context);
                KpiAdminAssistantWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiAdminAssistantDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                            sm.saveDayRecord(d);
                        }
                    }
                }
                break;
            }
            case "COMMUNITY_MANAGER": {
                KpiCommunityStorageManager sm = KpiCommunityStorageManager.getInstance(context);
                KpiCommunityWeekReport week = sm.getCurrentWeekReport();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiCommunityDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                        }
                    }
                    sm.saveShift(week.isMorningShift());
                }
                break;
            }
            case "FOTOGRAFO": {
                KpiPhotographerStorageManager sm = KpiPhotographerStorageManager.getInstance(context);
                KpiPhotographerWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiPhotographerDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                            sm.saveDayRecord(d);
                        }
                    }
                }
                break;
            }
            case "VIDEOGRAFO": {
                KpiVideographerStorageManager sm = KpiVideographerStorageManager.getInstance(context);
                KpiVideographerWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiVideographerDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                            sm.saveDayRecord(d);
                        }
                    }
                }
                break;
            }
            case "TRAFFICKER_DIGITAL": {
                KpiTraffickerStorageManager sm = KpiTraffickerStorageManager.getInstance(context);
                KpiTraffickerWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    String obs = getSemanticField(cargo, "general_observations", null);
                    if (obs != null && !obs.isEmpty()) week.setGeneralObservations(obs);
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiTraffickerDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainActivity(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalLabel(goal);
                            if (exp != null && !exp.isEmpty()) d.setDailyExpectedTotal(parseSafeInt(exp, d.getDailyExpectedTotal()));
                            sm.saveDayRecord(d);
                        }
                    }
                }
                break;
            }
            case "GERENTE_GENERAL": {
                KpiGeneralManagerStorageManager sm = KpiGeneralManagerStorageManager.getInstance(context);
                KpiGeneralManagerWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiGeneralManagerDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            if (act != null && !act.isEmpty()) d.setMainIndicator(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalText(goal);
                        }
                    }
                    sm.saveToPrefs();
                }
                break;
            }
            case "DISENADOR_GRAFICO": {
                KpiDesignerMemoryRepository repo = KpiDesignerMemoryRepository.getInstance(context);
                KpiDesignerWeekReport week = repo.getCurrentWeekReport();
                if (week != null && week.getDays() != null) {
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiDesignerDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String act = getSemanticField(cargo, "day_activity_" + i, null);
                            String goal = getSemanticField(cargo, "day_goal_label_" + i, null);
                            if (act != null && !act.isEmpty()) d.setModality(act);
                            if (goal != null && !goal.isEmpty()) d.setDailyGoalDescription(goal);
                        }
                    }
                }
                break;
            }
            case "DISENADOR_GRAFICO_SENIOR": {
                KpiSeniorStorageManager sm = KpiSeniorStorageManager.getInstance(context);
                KpiSeniorWeekReport week = sm.getCurrentWeek();
                if (week != null && week.getDays() != null) {
                    for (int i = 0; i < Math.min(6, week.getDays().size()); i++) {
                        KpiSeniorDayRecord d = week.getDays().get(i);
                        if (!d.isLocked()) {
                            String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                            if (exp != null && !exp.isEmpty()) {
                                d.setDailyGoal(parseSafeInt(exp, d.getDailyGoal()));
                            }
                        }
                    }
                    sm.saveToPrefs();
                }
                break;
            }
        }

        // El Editor de KPI es el principal: sincronizar metas hacia el Horario del cargo
        // y activar la alerta para que el Licenciado revise/ajuste las horas del horario.
        syncMasterGoalsToSchedule(context, cargo);
    }

    private void syncMasterGoalsToSchedule(Context context, String cargo) {
        try {
            String[] days = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
            List<pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi> masterGoals = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                String act = getSemanticField(cargo, "day_activity_" + i, null);
                String goalLabel = getSemanticField(cargo, "day_goal_label_" + i, null);
                String exp = getSemanticField(cargo, "day_expected_total_" + i, null);
                if ((goalLabel != null && !goalLabel.trim().isEmpty())
                        || (exp != null && !exp.trim().isEmpty())
                        || (act != null && !act.trim().isEmpty())) {
                    String title = days[i] + (act != null && !act.trim().isEmpty()
                            ? " - " + act.trim().replace("\n", " ") : " - Meta KPI");
                    String target;
                    if (goalLabel != null && !goalLabel.trim().isEmpty()) {
                        target = goalLabel.trim();
                    } else if (exp != null && !exp.trim().isEmpty()) {
                        target = exp.trim() + " por dia";
                    } else {
                        target = "Actualizada desde Editor KPI";
                    }
                    masterGoals.add(new pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi(
                            title, target, false,
                            pe.net.ws.kpiweb.features.schedule.data.ScheduleRepository.MASTER_CHIP));
                }
            }
            for (int i = 0; i < 3; i++) {
                String jrGoal = getSemanticField(cargo, "jr_des_goal_title_" + i, null);
                if (jrGoal != null && !jrGoal.trim().isEmpty()) {
                    masterGoals.add(new pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi(
                            "Meta creativa " + (i + 1), jrGoal.trim(), true,
                            pe.net.ws.kpiweb.features.schedule.data.ScheduleRepository.MASTER_CHIP));
                }
            }
            boolean hasAnyOverrides = getActiveChangesCount(cargo) > 0;
            pe.net.ws.kpiweb.features.schedule.data.ScheduleRepository
                    .getInstance(context)
                    .applyMasterKpiList(cargo, masterGoals, hasAnyOverrides || !masterGoals.isEmpty());
        } catch (Exception ignored) {
        }
    }

    /**
     * Aplica automáticamente las acciones de checklist, metas de tarjetas y textos personalizados
     * de la plantilla oficial sobre el árbol visual del formulario del colaborador.
     */
    public void applyTemplateToFormView(Context context, String cargo, View formRoot) {
        if (formRoot == null) return;
        List<CheckBox> checkBoxes = new ArrayList<>();
        collectCheckBoxes(formRoot, checkBoxes);

        String key = normalizeCargoKey(cargo);
        if ("DISENADOR_GRAFICO_JUNIOR".equals(key)) {
            for (int i = 0; i < Math.min(4, checkBoxes.size()); i++) {
                String crit = getSemanticField(cargo, "eval_criterion_" + i, null);
                if (crit != null && !crit.isEmpty()) {
                    checkBoxes.get(i).setText(crit.replaceFirst("^\\d+\\.\\s*", ""));
                }
            }
            String[] defaultGoalTitles = {"4 POSTS", "2 VIDEOS", "2 POSTS + 1 VIDEO"};
            for (int i = 0; i < 3; i++) {
                String customTitle = getSemanticField(cargo, "jr_des_goal_title_" + i, null);
                if (customTitle != null && !customTitle.isEmpty()) {
                    replaceTextInHierarchy(formRoot, defaultGoalTitles[i], customTitle);
                }
            }
        } else {
            for (int i = 0; i < Math.min(7, checkBoxes.size()); i++) {
                String customAction = getSemanticField(cargo, "action_item_" + i, null);
                if (customAction != null && !customAction.isEmpty()) {
                    checkBoxes.get(i).setText(customAction);
                }
            }
        }
    }

    /** Refresh shared edits before applying them to a newly opened, unsent form. */
    public void refreshAndApplyTemplateToFormView(Context context, String cargo, View formRoot) {
        KpiTemplateSync.get(context).refresh((ok, message) -> {
            if (ok && formRoot.isAttachedToWindow()) {
                syncTemplateToRoleForms(context, cargo);
                applyTemplateToFormView(context, cargo, formRoot);
            }
        });
    }

    private void collectCheckBoxes(View current, List<CheckBox> out) {
        if (current instanceof CheckBox) {
            out.add((CheckBox) current);
        } else if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            for (int i = 0; i < vg.getChildCount(); i++) {
                collectCheckBoxes(vg.getChildAt(i), out);
            }
        }
    }

    private void replaceTextInHierarchy(View current, String oldExactText, String newText) {
        if (current instanceof TextView && !(current instanceof EditText)) {
            TextView tv = (TextView) current;
            String t = tv.getText() != null ? tv.getText().toString().trim() : "";
            if (t.equalsIgnoreCase(oldExactText)) {
                tv.setText(newText);
            }
        } else if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            for (int i = 0; i < vg.getChildCount(); i++) {
                replaceTextInHierarchy(vg.getChildAt(i), oldExactText, newText);
            }
        }
    }

    /**
     * Aplica las etiquetas, checklists y encabezados editados en la plantilla directamente
     * sobre la vista del Formulario KPI del colaborador.
     */
    public void applyTemplateToCollaboratorFormUi(String cargo,
                                                  CheckBox[] cbActions,
                                                  TextView tvActivityBadge,
                                                  TextView tvGoalBadge,
                                                  int dayIndex) {
        if (cbActions != null) {
            for (int i = 0; i < cbActions.length; i++) {
                if (cbActions[i] != null) {
                    String customAction = getSemanticField(cargo, "action_item_" + i, null);
                    if (customAction != null && !customAction.isEmpty()) {
                        cbActions[i].setText(customAction);
                    }
                }
            }
        }
        if (dayIndex >= 0 && dayIndex < 6) {
            String customAct = getSemanticField(cargo, "day_activity_" + dayIndex, null);
            if (customAct != null && !customAct.isEmpty() && tvActivityBadge != null) {
                tvActivityBadge.setText(customAct.replace("\n", " "));
            }
            String customGoal = getSemanticField(cargo, "day_goal_label_" + dayIndex, null);
            if (customGoal != null && !customGoal.isEmpty() && tvGoalBadge != null) {
                tvGoalBadge.setText("Meta diaria exigida: " + customGoal.replace("\n", " "));
            }
        }
    }

    // =========================================================================
    // 5. APLICACIÓN DE LA PLANTILLA AL REPORTE OFICIAL (SEMANA EN CURSO)
    // =========================================================================

    public void applyTemplateToReportSheet(Context context, String cargo, View sheetRoot, boolean isHistoricalWeek) {
        if (sheetRoot == null) return;
        if (isCustomRole(cargo)) {
            clearUserNumericCellsInCollaboratorSheet(context, sheetRoot);
            prepareBlankCustomRoleSheet(context, cargo, sheetRoot, isCustomRoleJefe(cargo));
        }
        ensureInstitutionalContactFooter(context, sheetRoot);
        captureOriginalTexts(sheetRoot, sheetRoot);

        // REGLA CLAVE DEL USUARIO: Las semanas anteriores/históricas NO se cambian.
        if (isHistoricalWeek) {
            return;
        }

        applySavedOverridesToSheet(cargo, sheetRoot, false);
        applyInstitutionalLogoAndFooterToSheet(context, cargo, sheetRoot);
    }

    /**
     * Aplica en la hoja del reporte tanto el Logo Institucional (img1: foto o texto)
     * como el Pie de Página Institucional (img2: web, correo, redes, SIGD), ya sea
     * que se hayan configurado para todos los KPIs o solo para el cargo actual.
     */
    public void applyInstitutionalLogoAndFooterToSheet(Context context, String cargo, View sheetRoot) {
        if (sheetRoot == null) return;
        String logoPath = getInstitutionalLogoImagePath(cargo);
        Bitmap logoBitmap = loadCleanLogoBitmap(logoPath);
        applyInstitutionalRecursive(context, cargo, sheetRoot, logoBitmap);
    }

    /**
     * Carga el logo en ARGB_8888 con canal alfa activo y limpia cualquier fondo negro residual
     * (causado por conversión sin alfa o por imágenes guardadas con fondo negro) para que el logo
     * tome exactamente el color de fondo de la hoja oficial.
     */
    private Bitmap loadCleanLogoBitmap(String logoPath) {
        if (logoPath == null || logoPath.isEmpty()) return null;
        File f = new File(logoPath);
        if (!f.exists()) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap raw = BitmapFactory.decodeFile(logoPath, opts);
            if (raw == null) return null;

            int w = raw.getWidth();
            int h = raw.getHeight();
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            out.setHasAlpha(true);

            int[] pixels = new int[w * h];
            raw.getPixels(pixels, 0, w, 0, 0, w, h);

            // Verificar si las esquinas quedaron negras (#000000) o blancas para dejarlas 100% transparentes
            int c1 = pixels[0];
            int c2 = pixels[w - 1];
            int c3 = pixels[(h - 1) * w];
            int c4 = pixels[(h - 1) * w + (w - 1)];

            boolean blackBg = isCornerBlack(c1) && isCornerBlack(c2) && (isCornerBlack(c3) || isCornerBlack(c4));
            boolean whiteBg = isCornerWhite(c1) && isCornerWhite(c2) && (isCornerWhite(c3) || isCornerWhite(c4));

            if (blackBg) {
                for (int i = 0; i < pixels.length; i++) {
                    int px = pixels[i];
                    int r = Color.red(px);
                    int g = Color.green(px);
                    int b = Color.blue(px);
                    if (r <= 14 && g <= 14 && b <= 14) {
                        pixels[i] = 0x00FFFFFF;
                    } else if (r <= 28 && g <= 28 && b <= 28) {
                        int avg = (r + g + b) / 3;
                        int newAlpha = Math.max(0, Math.min(255, (avg - 14) * 18));
                        pixels[i] = Color.argb(newAlpha, r, g, b);
                    }
                }
            } else if (whiteBg) {
                for (int i = 0; i < pixels.length; i++) {
                    int px = pixels[i];
                    int r = Color.red(px);
                    int g = Color.green(px);
                    int b = Color.blue(px);
                    if (r >= 244 && g >= 244 && b >= 244) {
                        pixels[i] = 0x00FFFFFF;
                    }
                }
            }

            out.setPixels(pixels, 0, w, 0, 0, w, h);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCornerBlack(int c) {
        return Color.alpha(c) > 200 && Color.red(c) <= 16 && Color.green(c) <= 16 && Color.blue(c) <= 16;
    }

    private boolean isCornerWhite(int c) {
        return Color.alpha(c) > 200 && Color.red(c) >= 244 && Color.green(c) >= 244 && Color.blue(c) >= 244;
    }

    private void applyInstitutionalRecursive(Context context, String cargo, View current, Bitmap logoBitmap) {
        if (current == null) return;

        // 1. Detectar contenedores de Logo (LinearLayout que contiene "website+" y "Agencia Creativa")
        if (current instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) current;
            TextView tvLogoTitle = null;
            TextView tvLogoSub = null;
            for (int i = 0; i < ll.getChildCount(); i++) {
                View ch = ll.getChildAt(i);
                if (ch instanceof TextView) {
                    TextView tv = (TextView) ch;
                    String instKey = getInstitutionalKeyForTextView(tv);
                    if (INST_LOGO_TITLE.equals(instKey)) tvLogoTitle = tv;
                    else if (INST_LOGO_SUBTITLE.equals(instKey)) tvLogoSub = tv;
                }
            }
            if (tvLogoTitle != null && tvLogoSub != null) {
                ImageView ivLogo = ll.findViewWithTag(TAG_CUSTOM_LOGO_IMAGE);
                if (ivLogo == null) {
                    ivLogo = new ImageView(context);
                    ivLogo.setTag(TAG_CUSTOM_LOGO_IMAGE);
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(context, 76), dp(context, 34));
                    ivLp.bottomMargin = dp(context, 1);
                    ivLogo.setLayoutParams(ivLp);
                    ivLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    ivLogo.setAdjustViewBounds(true);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        ivLogo.setForceDarkAllowed(false);
                    }
                    ll.addView(ivLogo, 0);
                }

                if (logoBitmap != null) {
                    if (ll.getTag(R.id.llOfficialSheetRoot) == null && ll.getBackground() != null) {
                        ll.setTag(R.id.llOfficialSheetRoot, ll.getBackground());
                    }
                    // Si el contenedor del logo tenía fondo morado (#2E2A72) sobre cabecera blanca,
                    // lo dejamos transparente para que el PNG transparente luzca limpio como en img1
                    if (ll.getChildCount() <= 3 && ll.getBackground() != null) {
                        ll.setBackgroundColor(Color.TRANSPARENT);
                    }
                    ivLogo.setImageBitmap(logoBitmap);
                    ivLogo.setVisibility(View.VISIBLE);
                    tvLogoTitle.setVisibility(View.GONE);
                    tvLogoSub.setVisibility(View.GONE);
                } else {
                    Object savedBg = ll.getTag(R.id.llOfficialSheetRoot);
                    if (savedBg instanceof android.graphics.drawable.Drawable) {
                        ll.setBackground((android.graphics.drawable.Drawable) savedBg);
                    }
                    ivLogo.setVisibility(View.GONE);
                    tvLogoTitle.setVisibility(View.VISIBLE);
                    tvLogoSub.setVisibility(View.VISIBLE);

                    String customTitle = getInstitutionalValue(cargo, INST_LOGO_TITLE);
                    if (customTitle != null && !customTitle.isEmpty()) {
                        tvLogoTitle.setText(customTitle);
                    } else {
                        Object orig1 = tvLogoTitle.getTag(R.id.llOfficialSheetRoot);
                        if (orig1 instanceof String) tvLogoTitle.setText((String) orig1);
                    }

                    String customSub = getInstitutionalValue(cargo, INST_LOGO_SUBTITLE);
                    if (customSub != null && !customSub.isEmpty()) {
                        tvLogoSub.setText(customSub);
                    } else {
                        Object orig2 = tvLogoSub.getTag(R.id.llOfficialSheetRoot);
                        if (orig2 instanceof String) tvLogoSub.setText((String) orig2);
                    }
                }
            }
        }

        // 2. Aplicar textos institucionales del Pie de Página (img2: web, correo, redes, SIGD)
        if (current instanceof TextView && !(current instanceof EditText)) {
            TextView tv = (TextView) current;
            String instKey = getInstitutionalKeyForTextView(tv);
            if (instKey != null && !INST_LOGO_TITLE.equals(instKey) && !INST_LOGO_SUBTITLE.equals(instKey)) {
                String customVal = getInstitutionalValue(cargo, instKey);
                if (customVal != null && !customVal.isEmpty()) {
                    tv.setText(customVal);
                }
            }
            return;
        }

        if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyInstitutionalRecursive(context, cargo, vg.getChildAt(i), logoBitmap);
            }
        }
    }

    public String getInstitutionalKeyForTextView(TextView tv) {
        if (tv == null) return null;
        Object origObj = tv.getTag(R.id.llOfficialSheetRoot);
        String orig = origObj instanceof String
                ? ((String) origObj).trim()
                : (tv.getText() != null ? tv.getText().toString().trim() : "");
        if (orig.isEmpty()) return null;

        if ("website+".equalsIgnoreCase(orig)) return INST_LOGO_TITLE;
        if ("Agencia Creativa".equalsIgnoreCase(orig)) return INST_LOGO_SUBTITLE;

        String lower = orig.toLowerCase(Locale.ROOT);
        if (orig.startsWith("🌐") || lower.contains("www.website.pe")) return INST_FOOTER_WEB;
        if (orig.startsWith("✉") || lower.contains("hola@website")) return INST_FOOTER_EMAIL;
        if (orig.startsWith("📸") || lower.contains("@website.creativo")) return INST_FOOTER_SOCIAL;

        if ("SIGD".equals(orig)) return INST_FOOTER_SIGD_TITLE;
        if (lower.contains("sistema integral") && lower.contains("desempeño")) return INST_FOOTER_SIGD_SUB;
        if (orig.startsWith("TU TALENTO ")) return INST_FOOTER_SLOGAN;
        if (lower.contains("más ideas") || ">>>>>".equals(orig)) return INST_FOOTER_RIGHT;

        return null;
    }

    public void applySavedOverridesToSheet(String cargo, View sheetRoot, boolean isCleanEditorMode) {
        Map<String, String> overrides = getViewOverrides(cargo);
        if (overrides.isEmpty()) return;
        applyOverridesRecursive(appContext, sheetRoot, sheetRoot, overrides, isCleanEditorMode);
    }

    private void applyOverridesRecursive(Context context,
                                         View sheetRoot,
                                         View current,
                                         Map<String, String> overrides,
                                         boolean isCleanEditorMode) {
        if (current == null) return;

        if (current instanceof TextView && !(current instanceof EditText)) {
            TextView tv = (TextView) current;
            String idName = getIdEntryName(context, tv);
            if (!isCleanEditorMode && isUserGeneratedDataCellId(idName)) {
                return;
            }
            String key = getViewKey(context, sheetRoot, tv);
            if (overrides.containsKey(key)) {
                tv.setText(overrides.get(key));
            }
            return;
        }

        if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyOverridesRecursive(context, sheetRoot, vg.getChildAt(i), overrides, isCleanEditorMode);
            }
        }
    }

    // =========================================================================
    // 6. UTILIDADES DE IDENTIFICACIÓN DETERMINISTA DE VISTAS Y PARSEO
    // =========================================================================

    private void captureOriginalTexts(View sheetRoot, View current) {
        if (current == null) return;
        if (current instanceof TextView) {
            TextView tv = (TextView) current;
            if (tv.getTag(R.id.llOfficialSheetRoot) == null) {
                tv.setTag(R.id.llOfficialSheetRoot, tv.getText() != null ? tv.getText().toString() : "");
            }
        } else if (current instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) current;
            for (int i = 0; i < vg.getChildCount(); i++) {
                captureOriginalTexts(sheetRoot, vg.getChildAt(i));
            }
        }
    }

    public String getViewKey(Context context, View sheetRoot, View target) {
        String idName = getIdEntryName(context, target);
        String treePath = buildTreePath(sheetRoot, target);
        if (!idName.isEmpty()) {
            return "id:" + idName + "@" + treePath;
        }
        return "path:" + treePath;
    }

    private String buildTreePath(View root, View target) {
        List<Integer> indices = new ArrayList<>();
        View curr = target;
        while (curr != null && curr != root && curr.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) curr.getParent();
            indices.add(0, parent.indexOfChild(curr));
            curr = parent;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indices.size(); i++) {
            if (i > 0) sb.append('/');
            sb.append(indices.get(i));
        }
        return sb.toString();
    }

    private String getIdEntryName(Context context, View view) {
        if (view == null || view.getId() == View.NO_ID) return "";
        try {
            return context.getResources().getResourceEntryName(view.getId());
        } catch (Exception e) {
            return "";
        }
    }

    private int detectDayRowIndex(ViewGroup row) {
        if (row == null || row.getChildCount() < 3) return -1;
        View first = row.getChildAt(0);
        if (!(first instanceof TextView)) return -1;
        String t = ((TextView) first).getText() != null
                ? ((TextView) first).getText().toString().trim().toUpperCase(Locale.ROOT) : "";
        if (t.startsWith("LUNES") || "LUN".equals(t)) return 0;
        if (t.startsWith("MARTES") || "MAR".equals(t)) return 1;
        if (t.startsWith("MIÉRCOLES") || t.startsWith("MIERCOLES") || "MIÉ".equals(t)) return 2;
        if (t.startsWith("JUEVES") || "JUE".equals(t)) return 3;
        if (t.startsWith("VIERNES") || "VIE".equals(t)) return 4;
        if (t.startsWith("SÁBADO") || t.startsWith("SABADO") || "SÁB".equals(t)) return 5;
        return -1;
    }

    private boolean isTableHeaderRow(ViewGroup row) {
        if (row == null || row.getChildCount() < 4) return false;
        View first = row.getChildAt(0);
        if (!(first instanceof TextView)) return false;
        String t = ((TextView) first).getText() != null
                ? ((TextView) first).getText().toString().trim().toUpperCase(Locale.ROOT) : "";
        return "DÍA".equals(t) || "DIA".equals(t);
    }

    private int parseExpectedNumberFromGoalText(String text, int fallback) {
        if (text == null || text.isEmpty()) return fallback;
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        int sum = 0;
        boolean found = false;
        while (m.find()) {
            try {
                sum += Integer.parseInt(m.group(1));
                found = true;
                // Si no hay un '+' en el texto (ej. "02 videos + 02 diseños"), tomamos el primer número (ej. "1 - 2 publicaciones" -> 1 o 2)
                if (!text.contains("+")) {
                    return Integer.parseInt(m.group(1));
                }
            } catch (Exception ignored) {
            }
        }
        return found && sum > 0 ? sum : fallback;
    }

    private int parseSafeInt(String val, int fallback) {
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String stripPrefix(String text, String delimiter) {
        if (text == null) return "";
        int idx = text.indexOf(delimiter);
        if (idx >= 0 && idx + 1 < text.length()) {
            return text.substring(idx + 1).trim();
        }
        return text.trim();
    }

    private void updateCollaboratorProfileField(Context context, String cargo, String fieldType, String newValue) {
        if (newValue == null || newValue.isEmpty()) return;
        KpiCollaboratorsRepository repo = KpiCollaboratorsRepository.getInstance(context);
        List<KpiCollaboratorProfile> all = repo.getAllCollaborators();
        String normCargo = normalizeCargoKey(cargo);
        for (KpiCollaboratorProfile p : all) {
            if (normalizeCargoKey(p.getRole()).equals(normCargo)) {
                if ("area".equals(fieldType)) p.setArea(newValue);
                else if ("supervisor".equals(fieldType)) p.setSupervisor(newValue);
                else if ("boss".equals(fieldType)) p.setImmediateBoss(newValue);
            }
        }
    }

    private void setTextIfPresent(View root, int viewId, String text) {
        View v = root.findViewById(viewId);
        if (v instanceof TextView) {
            ((TextView) v).setText(text);
        }
    }

    private static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }
}
