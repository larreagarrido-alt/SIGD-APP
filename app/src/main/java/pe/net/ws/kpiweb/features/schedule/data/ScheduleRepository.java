package pe.net.ws.kpiweb.features.schedule.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;

import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleProfile;
import pe.net.ws.kpiweb.features.schedule.model.ScheduleWorkday;
import pe.net.ws.kpiweb.features.schedule.model.Slot;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Motor único de horarios: todos los cargos se leen y se guardan desde aquí.
 *
 * El servidor publica las plantillas del cronograma. Asistencias determina qué turnos
 * de esta semana corresponden al trabajador; las ediciones del administrador se sincronizan.
 */
public final class ScheduleRepository {

    private static final String PREFS = "schedule_store";
    private static final String KEY_PROFILES = "profiles_json_v1";
    private static final Type LIST_TYPE = new TypeToken<List<ScheduleProfile>>() { }.getType();

    public interface Listener {
        void onSchedulesChanged();
    }

    public interface LoadCallback {
        void onComplete(boolean success);
    }

    private static ScheduleRepository instance;

    private final SharedPreferences prefs;
    private final Context context;
    private final Gson gson = new Gson();
    private final List<ScheduleProfile> profiles = new ArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService writes = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Integer, Long> revisions = new HashMap<>();
    private final Map<String, Long> remoteAlerts = new HashMap<>();
    private boolean remoteLoaded;
    private int loadedUserId;
    private String myTurno = "";
    private String myEntrada = "";
    private String mySalida = "";
    private final List<ScheduleWorkday> myCalendar = new ArrayList<>();
    private boolean myCalendarLoaded;
    private int version = 0;

    public static synchronized ScheduleRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ScheduleRepository(context.getApplicationContext());
        }
        return instance;
    }

    private ScheduleRepository(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public synchronized boolean isRemoteLoaded() {
        SessionManager session = new SessionManager(context);
        return remoteLoaded && loadedUserId == session.getUserId();
    }

    /** Server data is authoritative. A failed request never falls back to sample schedules. */
    public void refreshRemote(LoadCallback callback) {
        SessionManager session = new SessionManager(context);
        String bearer = session.getBearerToken();
        int userId = session.getUserId();
        boolean admin = session.getRoleId() == 1;
        if (bearer.isEmpty()) {
            callback.onComplete(false);
            return;
        }
        if (admin) {
            ApiClient.getScheduleService().all(bearer).enqueue(new Callback<ApiResponse<JsonArray>>() {
                @Override public void onResponse(Call<ApiResponse<JsonArray>> call, Response<ApiResponse<JsonArray>> response) {
                    ApiResponse<JsonArray> body = response.body();
                    if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                        callback.onComplete(false);
                        return;
                    }
                    List<ScheduleProfile> received = new ArrayList<>();
                    Map<Integer, Long> revs = new HashMap<>();
                    try {
                        for (JsonElement element : body.getData()) {
                            JsonObject row = element.getAsJsonObject();
                            int cargoId = row.get("id_cargo").getAsInt();
                            ScheduleProfile profile = gson.fromJson(row.get("perfil"), ScheduleProfile.class);
                            if (profile == null || profile.slots == null || profile.slots.isEmpty()) continue;
                            profile.useOfficialEvaluationLevels();
                            profile.id = "cargo_" + cargoId;
                            received.add(profile);
                            revs.put(cargoId, row.get("revision").getAsLong());
                        }
                    } catch (RuntimeException error) {
                        callback.onComplete(false);
                        return;
                    }
                    acceptRemote(userId, received, revs, null, null, callback);
                }
                @Override public void onFailure(Call<ApiResponse<JsonArray>> call, Throwable error) {
                    callback.onComplete(false);
                }
            });
        } else {
            ApiClient.getScheduleService().mine(bearer).enqueue(new Callback<ApiResponse<JsonObject>>() {
                @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                    ApiResponse<JsonObject> body = response.body();
                    if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null) {
                        callback.onComplete(false);
                        return;
                    }
                    List<ScheduleProfile> received = new ArrayList<>();
                    Map<Integer, Long> revs = new HashMap<>();
                    try {
                        JsonElement schedule = body.getData().get("horario");
                        JsonObject row = schedule != null && schedule.isJsonObject()
                                ? schedule.getAsJsonObject() : null;
                        if (row != null) {
                            int cargoId = row.get("id_cargo").getAsInt();
                            ScheduleProfile profile = gson.fromJson(row.get("perfil"), ScheduleProfile.class);
                            if (profile != null && profile.slots != null && !profile.slots.isEmpty()) {
                                profile.useOfficialEvaluationLevels();
                                profile.id = "cargo_" + cargoId;
                                received.add(profile);
                                revs.put(cargoId, row.get("revision").getAsLong());
                            }
                        }
                    } catch (RuntimeException error) {
                        callback.onComplete(false);
                        return;
                    }
                    JsonElement shift = body.getData().get("turno");
                    JsonElement calendar = body.getData().get("calendario_laboral");
                    acceptRemote(userId, received, revs,
                            shift != null && shift.isJsonObject() ? shift.getAsJsonObject() : null,
                            calendar != null && calendar.isJsonObject() ? calendar.getAsJsonObject() : null,
                            callback);
                }
                @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                    callback.onComplete(false);
                }
            });
        }
    }

    private void acceptRemote(int userId, List<ScheduleProfile> received,
                              Map<Integer, Long> revs, JsonObject shift, JsonObject calendar,
                              LoadCallback callback) {
        if (new SessionManager(context).getUserId() != userId) {
            callback.onComplete(false);
            return;
        }
        synchronized (this) {
            profiles.clear();
            profiles.addAll(received);
            revisions.clear();
            revisions.putAll(revs);
            remoteAlerts.clear();
            myTurno = shift == null ? "" : stringValue(shift, "turno");
            myEntrada = shift == null ? "" : stringValue(shift, "hora_entrada");
            mySalida = shift == null ? "" : stringValue(shift, "hora_salida");
            myCalendar.clear();
            myCalendarLoaded = false;
            if (calendar != null && calendar.has("dias") && calendar.get("dias").isJsonArray()) {
                for (JsonElement day : calendar.getAsJsonArray("dias")) {
                    if (day.isJsonObject()) {
                        ScheduleWorkday workday = gson.fromJson(day, ScheduleWorkday.class);
                        if (workday != null) myCalendar.add(workday);
                    }
                }
                myCalendarLoaded = myCalendar.size() == 6;
            }
            remoteLoaded = true;
            loadedUserId = userId;
            sortProfiles();
        }
        notifyChanged();
        if (new SessionManager(context).getRoleId() == 1) loadRemoteAlerts();
        callback.onComplete(true);
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    public synchronized String getOwnShiftLabel() {
        if (!isRemoteLoaded() || myTurno.isEmpty() || myEntrada.isEmpty() || mySalida.isEmpty()) return "";
        String label = "MANANA".equals(myTurno) ? "Mañana" : "TARDE".equals(myTurno) ? "Tarde" : "Completo";
        return label + " · " + myEntrada + "–" + mySalida;
    }

    public synchronized boolean isOwnCalendarLoaded() {
        return isRemoteLoaded() && myCalendarLoaded;
    }

    public synchronized ScheduleWorkday getOwnWorkday(int dayIndex) {
        if (!isOwnCalendarLoaded() || dayIndex < 0 || dayIndex >= myCalendar.size()) return null;
        return myCalendar.get(dayIndex);
    }

    private void loadRemoteAlerts() {
        SessionManager session = new SessionManager(context);
        int userId = session.getUserId();
        ApiClient.getScheduleService().alerts(session.getBearerToken()).enqueue(new Callback<ApiResponse<JsonArray>>() {
            @Override public void onResponse(Call<ApiResponse<JsonArray>> call, Response<ApiResponse<JsonArray>> response) {
                ApiResponse<JsonArray> body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccessful() || body.getData() == null
                        || new SessionManager(context).getUserId() != userId) return;
                Map<String, Long> received = new HashMap<>();
                try {
                    for (JsonElement element : body.getData()) {
                        JsonObject row = element.getAsJsonObject();
                        received.put("cargo_" + row.get("id_cargo").getAsInt(), row.get("id_alerta").getAsLong());
                    }
                } catch (RuntimeException invalid) { return; }
                synchronized (ScheduleRepository.this) {
                    remoteAlerts.clear();
                    remoteAlerts.putAll(received);
                }
                notifyChanged();
            }
            @Override public void onFailure(Call<ApiResponse<JsonArray>> call, Throwable error) { }
        });
    }

    private void load() {
        profiles.clear();
        String json = prefs.getString(KEY_PROFILES, null);
        if (json != null && !json.isEmpty()) {
            try {
                List<ScheduleProfile> stored = gson.fromJson(json, LIST_TYPE);
                if (stored != null) profiles.addAll(stored);
            } catch (RuntimeException ignored) {
                profiles.clear();
            }
        }
        // Garantiza que los 13 cargos oficiales existan (también tras actualizar la app).
        boolean changed = profiles.isEmpty();
        for (ScheduleProfile def : ScheduleData.defaultProfiles()) {
            if (find(def.id) == null) {
                profiles.add(def);
                changed = true;
            }
        }
        // Normaliza franjas antiguas (13:30-15:00 / 15:00-...) al horario real oficial (8:00-13:30 y 15:30-18:30)
        // sin perder ediciones personalizadas del usuario.
        for (ScheduleProfile p : profiles) {
            if (p.slots == null) continue;
            for (Slot s : p.slots) {
                if (s == null || s.label == null) continue;
                String lbl = s.label.trim();
                if ("13:30 - 15:00".equals(lbl)) {
                    s.applyLabel("13:30 - 15:30");
                    changed = true;
                } else if ("15:00 - 16:30".equals(lbl)) {
                    s.applyLabel("15:30 - 16:30");
                    changed = true;
                } else if ("15:00 - 17:00".equals(lbl)) {
                    s.applyLabel("15:30 - 17:00");
                    changed = true;
                }
            }
        }
        sortProfiles();
        if (changed) persist();
    }

    private void sortProfiles() {
        final List<String> order = new ArrayList<>();
        for (ScheduleProfile d : ScheduleData.defaultProfiles()) order.add(d.id);
        Collections.sort(profiles, new Comparator<ScheduleProfile>() {
            @Override
            public int compare(ScheduleProfile a, ScheduleProfile b) {
                int ia = order.indexOf(a.id);
                int ib = order.indexOf(b.id);
                if (ia < 0) ia = Integer.MAX_VALUE;
                if (ib < 0) ib = Integer.MAX_VALUE;
                return ia != ib ? Integer.compare(ia, ib) : a.roleName.compareToIgnoreCase(b.roleName);
            }
        });
    }

    private void persist() {
        if (remoteLoaded) return;
        prefs.edit().putString(KEY_PROFILES, gson.toJson(profiles, LIST_TYPE)).apply();
    }

    private void notifyChanged() {
        version++;
        for (Listener l : listeners) l.onSchedulesChanged();
    }

    private ScheduleProfile find(String id) {
        for (ScheduleProfile p : profiles) {
            if (p.id.equals(id)) return p;
        }
        return null;
    }

    // ----------------------------------------------------------------------------- lectura

    public synchronized List<ScheduleProfile> getAll() {
        return new ArrayList<>(profiles);
    }

    public synchronized ScheduleProfile get(String id) {
        ScheduleProfile p = id == null ? null : find(id);
        if (remoteLoaded && id != null) return p;
        return p != null ? p : (profiles.isEmpty() ? null : profiles.get(0));
    }

    /** Busca el horario del cargo indicado (nombre libre, p. ej. el cargo de la sesión). */
    public synchronized ScheduleProfile findByRoleName(String roleName) {
        if (roleName == null) return null;
        String key = normalize(roleName);
        for (ScheduleProfile p : profiles) {
            if (normalize(p.roleName).equals(key)) return p;
        }
        for (ScheduleProfile p : profiles) {
            String k = normalize(p.roleName);
            if (key.contains(k) || k.contains(key)) return p;
        }
        return null;
    }

    public int getVersion() {
        return version;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    // ----------------------------------------------------------------------------- escritura

    /** Guarda el perfil editado (se aplica de inmediato en todas las pantallas). */
    public synchronized void save(ScheduleProfile profile) {
        if (remoteLoaded && !isRemoteLoaded()) return;
        Collections.sort(profile.slots, new Comparator<Slot>() {
            @Override
            public int compare(Slot a, Slot b) {
                return Integer.compare(a.startMin, b.startMin);
            }
        });
        if (find(profile.id) == null) profiles.add(profile);
        sortProfiles();
        persist();
        notifyChanged();
        if (remoteLoaded && new SessionManager(context).getRoleId() == 1) {
            queueRemoteSave(profile);
        }
    }

    private void queueRemoteSave(ScheduleProfile profile) {
        final String snapshot = gson.toJson(profile);
        final SessionManager queuedSession = new SessionManager(context);
        final int queuedUserId = queuedSession.getUserId();
        final String queuedBearer = queuedSession.getBearerToken();
        final int cargoId;
        try {
            if (profile.id == null || !profile.id.startsWith("cargo_")) throw new NumberFormatException();
            cargoId = Integer.parseInt(profile.id.substring(6));
        } catch (NumberFormatException invalid) {
            mainHandler.post(() -> Toast.makeText(context, "Horario sin cargo de Asistencias", Toast.LENGTH_LONG).show());
            return;
        }
        writes.execute(() -> {
            if (new SessionManager(context).getUserId() != queuedUserId) return;
            long revision;
            synchronized (ScheduleRepository.this) {
                revision = revisions.containsKey(cargoId) ? revisions.get(cargoId) : 0;
            }
            JsonObject body = new JsonObject();
            body.add("perfil", com.google.gson.JsonParser.parseString(snapshot));
            body.addProperty("revision", revision);
            body.addProperty("publicado", true);
            try {
                Response<ApiResponse<JsonObject>> result = ApiClient.getScheduleService()
                        .save(queuedBearer, cargoId, body).execute();
                ApiResponse<JsonObject> receipt = result.body();
                if (!result.isSuccessful() || receipt == null || !receipt.isSuccessful() || receipt.getData() == null) {
                    throw new IllegalStateException("No se pudo confirmar el horario en el servidor");
                }
                long next = receipt.getData().get("revision").getAsLong();
                synchronized (ScheduleRepository.this) { revisions.put(cargoId, next); }
                mainHandler.post(() -> {
                    Toast.makeText(context, "Horario guardado en el servidor", Toast.LENGTH_SHORT).show();
                    loadRemoteAlerts();
                });
            } catch (Exception error) {
                mainHandler.post(() -> {
                    Toast.makeText(context, "No se guardó el horario en el servidor", Toast.LENGTH_LONG).show();
                    refreshRemote(success -> { });
                });
            }
        });
    }

    /** Crea un horario en blanco con la plantilla estándar. Devuelve null si el nombre ya existe. */
    public synchronized ScheduleProfile createBlank(String roleName) {
        String name = roleName == null ? "" : roleName.trim();
        if (name.isEmpty() || findByExactName(name) != null) return null;
        String id = "custom_" + normalize(name).toLowerCase(Locale.ROOT) + "_" + System.currentTimeMillis();
        ScheduleProfile p = ScheduleData.blankProfile(id, name);
        save(p);
        return p;
    }

    private ScheduleProfile findByExactName(String name) {
        for (ScheduleProfile p : profiles) {
            if (p.roleName.equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    /** Elimina un horario personalizado. Los cargos oficiales no se eliminan (se restauran). */
    public synchronized boolean delete(String id) {
        ScheduleProfile p = find(id);
        if (p == null || p.factory) return false;
        profiles.remove(p);
        persist();
        notifyChanged();
        return true;
    }

    /** Devuelve un cargo oficial a su horario original de fábrica. */
    public synchronized boolean restoreDefault(String id) {
        for (ScheduleProfile def : ScheduleData.defaultProfiles()) {
            if (def.id.equals(id)) {
                ScheduleProfile current = find(id);
                if (current != null) profiles.remove(current);
                profiles.add(def);
                sortProfiles();
                persist();
                notifyChanged();
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ Editor de KPI -> Horario

    public static final String MASTER_CHIP = "Editor KPI";
    private static final String KEY_ALERTS = "kpi_change_alerts_v1";

    /**
     * El Editor de KPI es el maestro: sus metas por día reemplazan las metas del horario del cargo
     * y se deja una alerta pendiente para que el Licenciado ajuste las franjas del horario.
     */
    public synchronized void applyMasterKpi(String cargo, String[] dayNames, String[] titles, int[] expected) {
        ScheduleProfile p = findByRoleName(cargo);
        if (p == null || dayNames == null) return;
        List<pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi> keep = new ArrayList<>();
        StringBuilder before = new StringBuilder();
        for (pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi k : p.goals) {
            if (MASTER_CHIP.equals(k.chipText)) {
                before.append(k.name).append('|').append(k.target).append(';');
            } else {
                keep.add(k);
            }
        }
        StringBuilder after = new StringBuilder();
        List<pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi> master = new ArrayList<>();
        for (int i = 0; i < dayNames.length; i++) {
            String name = dayNames[i] + " - " + (titles != null && i < titles.length ? titles[i] : "Meta del dia");
            String target = expected[i] + " por dia";
            master.add(new pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi(name, target, false, MASTER_CHIP));
            after.append(name).append('|').append(target).append(';');
        }
        if (before.toString().equals(after.toString())) return; // sin cambios reales
        keep.addAll(master);
        p.goals = keep;
        persist();
        java.util.Set<String> alerts = new java.util.HashSet<>(prefs.getStringSet(KEY_ALERTS, new java.util.HashSet<String>()));
        alerts.add(p.id);
        prefs.edit().putStringSet(KEY_ALERTS, alerts).apply();
        notifyChanged();
    }

    /**
     * Sincroniza metas maestras provenientes del Editor Oficial de KPI ({@code KpiOfficialTemplateEngine})
     * hacia el horario del cargo y activa la notificación pendiente para el Licenciado.
     */
    public synchronized void applyMasterKpiList(
            String cargo,
            List<pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi> incomingMasterGoals,
            boolean forceAlert) {
        ScheduleProfile p = findByRoleName(cargo);
        if (p == null) return;
        boolean updated = false;
        if (incomingMasterGoals != null && !incomingMasterGoals.isEmpty()) {
            List<pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi> keep = new ArrayList<>();
            for (pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi k : p.goals) {
                if (!MASTER_CHIP.equals(k.chipText)) {
                    keep.add(k);
                }
            }
            for (pe.net.ws.kpiweb.features.schedule.model.ScheduleKpi g : incomingMasterGoals) {
                g.chipText = MASTER_CHIP;
                keep.add(g);
            }
            p.goals = keep;
            updated = true;
        }
        if (updated || forceAlert) {
            persist();
            java.util.Set<String> alerts = new java.util.HashSet<>(
                    prefs.getStringSet(KEY_ALERTS, new java.util.HashSet<String>()));
            alerts.add(p.id);
            prefs.edit().putStringSet(KEY_ALERTS, alerts).apply();
            notifyChanged();
        }
    }

    /** Cargos cuyo horario debe revisarse porque cambiaron sus metas en el Editor de KPI. */
    public synchronized List<ScheduleProfile> getPendingAlerts() {
        List<ScheduleProfile> out = new ArrayList<>();
        if (remoteLoaded) {
            for (String id : remoteAlerts.keySet()) {
                ScheduleProfile p = find(id);
                if (p != null) out.add(p);
            }
            return out;
        }
        for (String id : prefs.getStringSet(KEY_ALERTS, new java.util.HashSet<String>())) {
            ScheduleProfile p = find(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    public synchronized boolean hasAlert(String id) {
        if (remoteLoaded) return remoteAlerts.containsKey(id);
        return prefs.getStringSet(KEY_ALERTS, new java.util.HashSet<String>()).contains(id);
    }

    /** El Licenciado confirma que ya ajustó el horario de este cargo. */
    public synchronized void dismissAlert(String id) {
        if (remoteLoaded) {
            Long alertId = remoteAlerts.get(id);
            if (alertId == null) return;
            SessionManager session = new SessionManager(context);
            ApiClient.getScheduleService().acknowledge(session.getBearerToken(), alertId)
                    .enqueue(new Callback<ApiResponse<JsonObject>>() {
                        @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                            ApiResponse<JsonObject> body = response.body();
                            if (response.isSuccessful() && body != null && body.isSuccessful()) {
                                synchronized (ScheduleRepository.this) { remoteAlerts.remove(id); }
                                notifyChanged();
                            } else {
                                Toast.makeText(context, "Guarda el horario antes de atender el aviso", Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                            Toast.makeText(context, "No se pudo atender el aviso", Toast.LENGTH_LONG).show();
                        }
                    });
            return;
        }
        java.util.Set<String> alerts = new java.util.HashSet<>(prefs.getStringSet(KEY_ALERTS, new java.util.HashSet<String>()));
        if (alerts.remove(id)) {
            prefs.edit().putStringSet(KEY_ALERTS, alerts).apply();
            notifyChanged();
        }
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
