package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

/** Numeric goals used by the server scorer. Visual sheet labels remain independent. */
public final class KpiScoreGoalConfig {
    private static final String PREF = "kpi_official_sheet_templates_v1";

    private KpiScoreGoalConfig() {}

    public static String key(String cargo) {
        return "sem_" + KpiOfficialTemplateEngine.normalizeCargoKey(cargo) + "_official_goals_json";
    }

    public static String[] labels(String cargo) {
        switch (KpiOfficialTemplateEngine.normalizeCargoKey(cargo)) {
            case "ASESOR_DE_VENTAS": return new String[]{"Llamadas semanales", "Visitas semanales", "Cotizaciones semanales", "Seguimientos semanales", "Prospectos semanales", "Clientes semanales"};
            case "DISENADOR_GRAFICO":
            case "DISENADOR_GRAFICO_SENIOR":
            case "VIDEOGRAFO": return new String[]{"Actividades semanales"};
            case "DISENADOR_GRAFICO_JUNIOR": return new String[]{"Posts en diseño gráfico", "Videos en audiovisual", "Posts en modalidad mixta", "Videos en modalidad mixta"};
            case "GERENTE_GENERAL": return new String[]{"Días de seguimiento por semana completa"};
            default: return new String[]{"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
        }
    }

    public static int[] defaults(String cargo) {
        switch (KpiOfficialTemplateEngine.normalizeCargoKey(cargo)) {
            case "ADMINISTRADOR": return new int[]{4,4,4,4,4,2};
            case "ASISTENTE_ADMINISTRATIVO": return new int[]{23,11,11,9,14,4};
            case "CONTADOR": return new int[]{45,17,24,12,8,12};
            case "COMMUNITY_MANAGER": return new int[]{11,14,13,12,9,6};
            case "FOTOGRAFO": return new int[]{3,3,3,3,3,2};
            case "TRAFFICKER_DIGITAL": return new int[]{1,1,1,1,1,1};
            case "VIDEOGRAFO": return new int[]{10};
            case "DISENADOR_GRAFICO": return new int[]{25};
            case "DISENADOR_GRAFICO_SENIOR": return new int[]{24};
            case "ASESOR_DE_VENTAS": return new int[]{100,60,30,50,15,3};
            case "PROGRAMADOR_JUNIOR": return new int[]{1,2,2,1,1,2};
            case "DISENADOR_GRAFICO_JUNIOR": return new int[]{4,2,2,1};
            case "GERENTE_GENERAL": return new int[]{6};
            default: return new int[0];
        }
    }

    public static int[] read(Context context, String cargo) {
        int[] fallback = defaults(cargo);
        String raw = preferences(context).getString(key(cargo), null);
        if (raw == null || fallback.length == 0) return fallback;
        try {
            JSONArray values = new JSONArray(raw);
            if (values.length() != fallback.length) return fallback;
            int[] parsed = new int[fallback.length];
            for (int i = 0; i < parsed.length; i++) {
                if (!(values.get(i) instanceof Integer)) return fallback;
                parsed[i] = values.getInt(i);
                if (parsed[i] < 1 || parsed[i] > 100000) return fallback;
            }
            return parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static String encode(int[] goals) {
        JSONArray array = new JSONArray();
        for (int goal : goals) array.put(goal);
        return array.toString();
    }
}
