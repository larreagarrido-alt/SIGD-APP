package pe.net.ws.kpiweb.features.kpi.designer.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Configuración Maestra Unificada por Cargo y Categoría de KPI (SIGD 2026).
 *
 * Clasificación Oficial:
 * 1. ⚡ PRACTICANTES (Estilo Eléctrico / Energía):
 *    - Programador Junior
 *    - Diseñador Gráfico Junior
 *    - + Nuevos Practicantes agregados de cualquier otra área.
 *
 * 2. 👔 COLABORADORES (Estilo Elegante Ejecutivo):
 *    - Asesor de Ventas
 *    - Diseñador Gráfico
 *    - Diseñador Gráfico Senior
 *    - + Nuevos Colaboradores agregados de cualquier otra área.
 */
public class KpiRoleProfileConfig {

    private static final String PREF_CUSTOM_ROLES = "kpi_custom_roles_registry_v1";
    private static final String KEY_ROLES_JSON = "key_roles_json";
    private static final List<CustomRoleEntry> sCustomRoles = new ArrayList<>();
    private static boolean sLoaded = false;

    public static class CustomRoleEntry {
        public final String roleName;
        public final String areaName;
        public final boolean isPracticante;
        public final String metric1Title;
        public final String metric2Title;
        public final String metric3Title;
        public final int goal1;
        public final int goal2;
        public final int goal3;

        public CustomRoleEntry(String roleName, String areaName, boolean isPracticante,
                               String metric1Title, String metric2Title, String metric3Title,
                               int goal1, int goal2, int goal3) {
            this.roleName = roleName;
            this.areaName = areaName;
            this.isPracticante = isPracticante;
            this.metric1Title = metric1Title;
            this.metric2Title = metric2Title;
            this.metric3Title = metric3Title;
            this.goal1 = Math.max(1, goal1);
            this.goal2 = Math.max(1, goal2);
            this.goal3 = Math.max(1, goal3);
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("roleName", roleName);
                obj.put("areaName", areaName);
                obj.put("isPracticante", isPracticante);
                obj.put("metric1Title", metric1Title);
                obj.put("metric2Title", metric2Title);
                obj.put("metric3Title", metric3Title);
                obj.put("goal1", goal1);
                obj.put("goal2", goal2);
                obj.put("goal3", goal3);
            } catch (Exception ignored) {
            }
            return obj;
        }

        public static CustomRoleEntry fromJson(JSONObject obj) {
            if (obj == null) return null;
            return new CustomRoleEntry(
                    obj.optString("roleName", "Nuevo Cargo"),
                    obj.optString("areaName", "General"),
                    obj.optBoolean("isPracticante", true),
                    obj.optString("metric1Title", "Actividades principales"),
                    obj.optString("metric2Title", "Entregables secundarios"),
                    obj.optString("metric3Title", "Revisiones / Calidad"),
                    obj.optInt("goal1", 3),
                    obj.optInt("goal2", 2),
                    obj.optInt("goal3", 1)
            );
        }
    }

    public static synchronized List<CustomRoleEntry> getCustomRoles(Context context) {
        if (!sLoaded && context != null) {
            loadCustomRoles(context);
        }
        return new ArrayList<>(sCustomRoles);
    }

    public static synchronized void addCustomRole(Context context, CustomRoleEntry entry) {
        if (entry == null) return;
        if (!sLoaded && context != null) {
            loadCustomRoles(context);
        }
        for (int i = 0; i < sCustomRoles.size(); i++) {
            if (sCustomRoles.get(i).roleName.equalsIgnoreCase(entry.roleName)) {
                sCustomRoles.set(i, entry);
                saveCustomRoles(context);
                return;
            }
        }
        sCustomRoles.add(entry);
        saveCustomRoles(context);
    }

    public static synchronized void removeCustomRole(Context context, String roleName) {
        if (roleName == null) return;
        if (!sLoaded && context != null) {
            loadCustomRoles(context);
        }
        boolean removed = false;
        for (int i = sCustomRoles.size() - 1; i >= 0; i--) {
            CustomRoleEntry e = sCustomRoles.get(i);
            if (e != null && (e.roleName.equalsIgnoreCase(roleName)
                    || e.roleName.toLowerCase().contains("especialista"))) {
                sCustomRoles.remove(i);
                removed = true;
            }
        }
        if (removed && context != null) {
            saveCustomRoles(context);
        }
    }

    private static void loadCustomRoles(Context context) {
        sLoaded = true;
        if (context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_CUSTOM_ROLES, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_ROLES_JSON, null);
            if (raw != null) {
                JSONArray arr = new JSONArray(raw);
                sCustomRoles.clear();
                boolean needResave = false;
                for (int i = 0; i < arr.length(); i++) {
                    CustomRoleEntry e = CustomRoleEntry.fromJson(arr.optJSONObject(i));
                    if (e != null) {
                        String rLow = e.roleName != null ? e.roleName.toLowerCase() : "";
                        if (rLow.contains("especialista") || rLow.equals("especialista en marketing") || rLow.equals("especialista de marketing")) {
                            needResave = true;
                            continue;
                        }
                        sCustomRoles.add(e);
                    }
                }
                if (needResave) {
                    saveCustomRoles(context);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void saveCustomRoles(Context context) {
        if (context == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (CustomRoleEntry e : sCustomRoles) {
                arr.put(e.toJson());
            }
            context.getSharedPreferences(PREF_CUSTOM_ROLES, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ROLES_JSON, arr.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    public static class DayGoalSpec {
        public final String modality;
        public final String goalSummary;
        public final int goal1;
        public final int goal2;
        public final int goal3;
        public final boolean active1;
        public final boolean active2;
        public final boolean active3;
        public final String sub1;
        public final String sub2;
        public final String sub3;

        public DayGoalSpec(String modality, String goalSummary,
                           int goal1, boolean active1, String sub1,
                           int goal2, boolean active2, String sub2,
                           int goal3, boolean active3, String sub3) {
            this.modality = modality;
            this.goalSummary = goalSummary;
            this.goal1 = goal1;
            this.active1 = active1;
            this.sub1 = sub1;
            this.goal2 = goal2;
            this.active2 = active2;
            this.sub2 = sub2;
            this.goal3 = goal3;
            this.active3 = active3;
            this.sub3 = sub3;
        }
    }

    private final String roleName;
    private final boolean practicanteMode;
    private final String formHeaderTitle;
    private final String formHeaderSubtitle;
    private final String metric1Title;
    private final String metric2Title;
    private final String metric3Title;
    private final CustomRoleEntry customSpec;

    public KpiRoleProfileConfig(String roleName,
                                boolean practicanteMode,
                                String formHeaderTitle,
                                String formHeaderSubtitle,
                                String metric1Title,
                                String metric2Title,
                                String metric3Title) {
        this(roleName, practicanteMode, formHeaderTitle, formHeaderSubtitle,
                metric1Title, metric2Title, metric3Title, null);
    }

    public KpiRoleProfileConfig(String roleName,
                                boolean practicanteMode,
                                String formHeaderTitle,
                                String formHeaderSubtitle,
                                String metric1Title,
                                String metric2Title,
                                String metric3Title,
                                CustomRoleEntry customSpec) {
        this.roleName = roleName;
        this.practicanteMode = practicanteMode;
        this.formHeaderTitle = formHeaderTitle;
        this.formHeaderSubtitle = formHeaderSubtitle;
        this.metric1Title = metric1Title;
        this.metric2Title = metric2Title;
        this.metric3Title = metric3Title;
        this.customSpec = customSpec;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isPracticanteMode() {
        return practicanteMode;
    }

    public String getFormHeaderTitle() {
        return formHeaderTitle;
    }

    public String getFormHeaderSubtitle() {
        return formHeaderSubtitle;
    }

    public String getMetric1Title() {
        return metric1Title;
    }

    public String getMetric2Title() {
        return metric2Title;
    }

    public String getMetric3Title() {
        return metric3Title;
    }

    public static KpiRoleProfileConfig forRole(String rawRole, boolean isPracticanteOverride) {
        String r = rawRole != null ? rawRole.trim() : "Diseñador Gráfico";

        // 0. Verificar primero si es un cargo personalizado (Practicante o Colaborador de otra área)
        for (CustomRoleEntry entry : sCustomRoles) {
            if (entry.roleName.equalsIgnoreCase(r)) {
                return new KpiRoleProfileConfig(
                        entry.roleName,
                        isPracticanteOverride,
                        "KPI Diario · " + entry.areaName,
                        isPracticanteOverride
                                ? "Practicante (" + entry.areaName + ") · Estilo Eléctrico Lun a Sáb"
                                : "Colaborador (" + entry.areaName + ") · Estilo Elegante Lun a Sáb",
                        entry.metric1Title,
                        entry.metric2Title,
                        entry.metric3Title,
                        entry
                );
            }
        }

        String upper = r.toUpperCase();

        if (upper.contains("PROGRAMADOR") || upper.contains("SOFTWARE") || upper.contains("DESARROLLADOR")) {
            return new KpiRoleProfileConfig(
                    "Programador Junior",
                    isPracticanteOverride,
                    "KPI Diario · Programador Junior",
                    isPracticanteOverride
                            ? "Practicante Eléctrico · Desarrollo & Software Lun a Sáb"
                            : "Colaborador Elegante · Ingeniería de Software Lun a Sáb",
                    "Funcionalidades desarrolladas",
                    "Incidencias / Bugs corregidos",
                    "Despliegues / Commits QA"
            );
        }

        if (upper.contains("VENTA") || upper.contains("COMERCIAL") || upper.contains("ASESOR")) {
            return new KpiRoleProfileConfig(
                    "Asesor de Ventas",
                    isPracticanteOverride,
                    "KPI Diario · Gestión Comercial",
                    isPracticanteOverride
                            ? "Practicante Eléctrico · Prospección activa Lun a Sáb"
                            : "Colaborador Elegante · Ejecutivo Comercial Lun a Sáb",
                    "Prospectos contactados",
                    "Reuniones / Demos realizadas",
                    "Cierres / Propuestas enviadas"
            );
        }

        if (upper.contains("JUNIOR") || upper.contains("PRACTICANTE")) {
            return new KpiRoleProfileConfig(
                    "Diseñador Gráfico Junior",
                    isPracticanteOverride,
                    "KPI Diario · Diseñador Gráfico Junior",
                    isPracticanteOverride
                            ? "Practicante Eléctrico · Energía creativa Lun a Sáb"
                            : "Colaborador Elegante · Producción Junior Lun a Sáb",
                    "Piezas gráficas / Posts",
                    "Reels / Videos cortos",
                    "Piezas aprobadas sin corrección"
            );
        }

        if (upper.contains("SENIOR")) {
            return new KpiRoleProfileConfig(
                    "Diseñador Gráfico Senior",
                    isPracticanteOverride,
                    "KPI Diario · Dirección & Supervisión",
                    isPracticanteOverride
                            ? "Practicante Eléctrico · Liderazgo creativo Lun a Sáb"
                            : "Colaborador Elegante · Dirección Senior Lun a Sáb",
                    "Campañas / Diseños Senior",
                    "Videos Master / Sesiones",
                    "Piezas del equipo supervisadas"
            );
        }

        return new KpiRoleProfileConfig(
                "Diseñador Gráfico",
                isPracticanteOverride,
                "KPI Diario · Producción Creativa",
                isPracticanteOverride
                        ? "Practicante Eléctrico · Entregables dinámicos Lun a Sáb"
                        : "Colaborador Elegante · Control creativo Lun a Sáb",
                "Diseños gráficos",
                "Videos y Reels",
                "Sesiones fotográficas"
        );
    }

    public DayGoalSpec getDayGoalSpec(int dayOfWeek) {
        if (customSpec != null) {
            String summary = customSpec.goal1 + " " + customSpec.metric1Title.toLowerCase() +
                    " + " + customSpec.goal2 + " " + customSpec.metric2Title.toLowerCase();
            return new DayGoalSpec(
                    "Área " + customSpec.areaName + " (" + (customSpec.isPracticante ? "Practicante" : "Colaborador") + ")",
                    summary,
                    customSpec.goal1, true, "Meta: " + customSpec.goal1 + " entregables",
                    customSpec.goal2, true, "Meta: " + customSpec.goal2 + " entregables",
                    customSpec.goal3, true, "Meta: " + customSpec.goal3 + " entregables"
            );
        }

        String upper = roleName.toUpperCase();

        // 1. PROGRAMADOR JUNIOR (PRACTICANTE ⚡)
        if (upper.contains("PROGRAMADOR")) {
            if (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) {
                return new DayGoalSpec(
                        "Sprint Eléctrico · Backend & UI",
                        "03 funcionalidades + 02 bugs + 01 commit QA",
                        3, true, "Meta: 03 módulos/pantallas",
                        2, true, "Meta: 02 bugs resueltos",
                        1, true, "Meta: 01 entrega QA"
                );
            } else if (dayOfWeek == Calendar.SATURDAY) {
                return new DayGoalSpec(
                        "Cierre Técnico & Despliegue Semanal",
                        "01 funcionalidad + 01 bug + 02 despliegues",
                        1, true, "Meta: 01 funcionalidad",
                        1, true, "Meta: 01 bug resuelto",
                        2, true, "Meta: 02 despliegues"
                );
            } else {
                return new DayGoalSpec(
                        "Desarrollo Full-Stack + Soporte",
                        "02 funcionalidades + 02 bugs + 01 commit QA",
                        2, true, "Meta: 02 módulos/pantallas",
                        2, true, "Meta: 02 bugs resueltos",
                        1, true, "Meta: 01 entrega QA"
                );
            }
        }

        // 2. ASESOR DE VENTAS (COLABORADOR 👔)
        if (upper.contains("VENTA")) {
            if (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) {
                return new DayGoalSpec(
                        "Ruta Comercial & Cierre de Cuentas",
                        "06 prospectos + 03 demos + 02 cierres",
                        6, true, "Meta: 06 prospectos",
                        3, true, "Meta: 03 demos/reuniones",
                        2, true, "Meta: 02 cierres/propuestas"
                );
            } else if (dayOfWeek == Calendar.SATURDAY) {
                return new DayGoalSpec(
                        "Seguimiento Ejecutivo de Cartera",
                        "05 prospectos + 02 demos + 01 cierre",
                        5, true, "Meta: 05 prospectos",
                        2, true, "Meta: 02 demos/reuniones",
                        1, true, "Meta: 01 cierre/propuesta"
                );
            } else {
                return new DayGoalSpec(
                        "Prospección Activa + Presentaciones",
                        "08 prospectos + 02 demos + 01 cierre",
                        8, true, "Meta: 08 prospectos",
                        2, true, "Meta: 02 demos/reuniones",
                        1, true, "Meta: 01 cierre/propuesta"
                );
            }
        }

        // 3. DISEÑADOR GRÁFICO JUNIOR (PRACTICANTE ⚡)
        if (upper.contains("JUNIOR")) {
            if (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) {
                return new DayGoalSpec(
                        "A - Ráfaga de Diseño para Redes",
                        "04 piezas gráficas + 03 aprobadas al 1er intento",
                        4, true, "Meta: 04 posts/piezas",
                        0, true, "Meta: 00 reels (opcional)",
                        3, true, "Meta: 03 aprobadas sin cambio"
                );
            } else if (dayOfWeek == Calendar.SATURDAY) {
                return new DayGoalSpec(
                        "Cierre Semanal & Entrega de Editables",
                        "02 piezas + 01 reel + 02 aprobadas",
                        2, true, "Meta: 02 posts/piezas",
                        1, true, "Meta: 01 reel corto",
                        2, true, "Meta: 02 aprobadas sin cambio"
                );
            } else {
                return new DayGoalSpec(
                        "B - Producción Mixta (Posts + Reels)",
                        "03 piezas + 01 reel + 02 aprobadas",
                        3, true, "Meta: 03 posts/piezas",
                        1, true, "Meta: 01 reel corto",
                        2, true, "Meta: 02 aprobadas sin cambio"
                );
            }
        }

        // 4. DISEÑADOR GRÁFICO SENIOR (COLABORADOR 👔)
        if (upper.contains("SENIOR")) {
            if (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) {
                return new DayGoalSpec(
                        "Dirección de Campañas + Supervisión",
                        "03 diseños senior + 01 video + 03 supervisadas",
                        3, true, "Meta: 03 piezas/campañas",
                        1, true, "Meta: 01 video/sesión",
                        3, true, "Meta: 03 piezas supervisadas"
                );
            } else if (dayOfWeek == Calendar.SATURDAY) {
                return new DayGoalSpec(
                        "Auditoría Creativa & Cierre de Equipo",
                        "01 campaña + 01 sesión + 04 supervisadas",
                        1, true, "Meta: 01 campaña clave",
                        1, true, "Meta: 01 sesión/video",
                        4, true, "Meta: 04 piezas supervisadas"
                );
            } else {
                return new DayGoalSpec(
                        "Producción Senior + Feedback al Equipo",
                        "02 diseños senior + 01 video + 03 supervisadas",
                        2, true, "Meta: 02 piezas/campañas",
                        1, true, "Meta: 01 video/sesión",
                        3, true, "Meta: 03 piezas supervisadas"
                );
            }
        }

        // 5. DISEÑADOR GRÁFICO TITULAR (COLABORADOR 👔)
        if (dayOfWeek == Calendar.TUESDAY || dayOfWeek == Calendar.THURSDAY) {
            return new DayGoalSpec(
                    "A - Diseño gráfico",
                    "06 diseños (videos y fotos suman como extras)",
                    6, true, "Meta: 06 diseños",
                    0, true, "Meta: 00 (Extras habilitados)",
                    0, true, "Meta: 00 (Extras habilitados)"
            );
        } else if (dayOfWeek == Calendar.SATURDAY) {
            return new DayGoalSpec(
                    "Producción fotográfica",
                    "01 sesión fotográfica (diseños y videos suman como extras)",
                    0, true, "Meta: 00 (Extras habilitados)",
                    0, true, "Meta: 00 (Extras habilitados)",
                    1, true, "Meta: 01 sesión"
            );
        } else {
            return new DayGoalSpec(
                    "B - Audiovisual + Diseño",
                    "02 videos + 02 diseños (fotos suman como extras)",
                    2, true, "Meta: 02 diseños",
                    2, true, "Meta: 02 videos",
                    0, true, "Meta: 00 (Extras habilitados)"
            );
        }
    }
}
