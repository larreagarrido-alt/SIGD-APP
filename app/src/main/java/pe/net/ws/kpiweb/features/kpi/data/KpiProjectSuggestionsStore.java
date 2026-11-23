package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Almacén ligero local (SharedPreferences, CERO Base de Datos externa) para recordar
 * los nombres de proyectos que escribe el Programador Junior y mostrarlos como sugerencia
 * automática al escribir las primeras letras (ej. "co" -> "Control de asistencia").
 */
public final class KpiProjectSuggestionsStore {

    private static final String PREF_NAME = "kpi_programmer_projects_v1";
    private static final String KEY_PROJECTS = "saved_projects";

    private KpiProjectSuggestionsStore() {}

    public static List<String> getSuggestions(Context context) {
        Set<String> unique = new LinkedHashSet<>();
        if (context == null) return new ArrayList<>(unique);

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PROJECTS, null);
        if (raw != null && !raw.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    String val = arr.optString(i, "").trim();
                    if (!val.isEmpty()) {
                        unique.add(val);
                    }
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(unique);
    }

    public static void saveProject(Context context, String projectName) {
        if (context == null || projectName == null) return;
        String clean = projectName.trim();
        if (clean.length() < 2) return;

        List<String> existing = getSuggestions(context);
        Set<String> updated = new LinkedHashSet<>();
        updated.add(clean); // El más reciente primero
        for (String item : existing) {
            if (!item.equalsIgnoreCase(clean)) {
                updated.add(item);
            }
        }

        JSONArray arr = new JSONArray();
        for (String s : updated) {
            arr.put(s);
        }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROJECTS, arr.toString())
                .apply();
    }
}

