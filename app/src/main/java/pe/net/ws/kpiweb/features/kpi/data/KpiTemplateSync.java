package pe.net.ws.kpiweb.features.kpi.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Server is authoritative for editable visual KPI templates; reports remain independent. */
public final class KpiTemplateSync {
    private static final String PREF_NAME = "kpi_official_sheet_templates_v1";
    private static KpiTemplateSync instance;
    private final Context context;
    private final SharedPreferences prefs;
    private int loadedUser;
    private long revision = -1;

    public interface Result {
        void complete(boolean ok, String message);
    }

    private KpiTemplateSync(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized KpiTemplateSync get(Context context) {
        if (instance == null) instance = new KpiTemplateSync(context);
        return instance;
    }

    private static boolean isTemplateKey(String key) {
        return key.startsWith("overrides_") || key.startsWith("sem_")
                || key.startsWith("inst_global_") || key.startsWith("inst_cargo_")
                || key.startsWith("custom_role_") || key.equals("custom_roles_list_json");
    }

    public void refresh(Result result) {
        SessionManager session = new SessionManager(context);
        final int user = session.getUserId();
        final String bearer = session.getBearerToken();
        if (user <= 0 || bearer.isEmpty()) {
            result.complete(false, "Inicia sesión para cargar las plantillas KPI.");
            return;
        }
        ApiClient.getKpiTemplateService().read(bearer).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (new SessionManager(context).getUserId() != user) return;
                ApiResponse<JsonObject> envelope = response.body();
                if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful() || envelope.getData() == null) {
                    result.complete(false, "No se pudieron cargar las plantillas del servidor (HTTP " + response.code() + ").");
                    return;
                }
                try {
                    JsonObject data = envelope.getData();
                    long nextRevision = data.get("revision").getAsLong();
                    JsonObject state = data.getAsJsonObject("estado");
                    if (state == null) throw new IllegalStateException("Estado vacío");
                    SharedPreferences.Editor edit = prefs.edit();
                    // Preserve pre-integration edits on the administrator's device until they
                    // are reviewed and explicitly saved to a brand-new server state.
                    boolean preserveAdminLocal = nextRevision == 0 && state.size() == 0
                            && new SessionManager(context).getRoleId() == 1;
                    if (!preserveAdminLocal) {
                        for (String key : prefs.getAll().keySet()) if (isTemplateKey(key)) edit.remove(key);
                    }
                    for (Map.Entry<String, JsonElement> item : state.entrySet()) {
                        if (!isTemplateKey(item.getKey()) || !item.getValue().isJsonPrimitive()) continue;
                        String value = item.getValue().getAsString();
                        if (item.getKey().endsWith("inst_logo_image_path") && value.startsWith("data:image/png;base64,")) {
                            byte[] bytes = Base64.decode(value.substring(22), Base64.DEFAULT);
                            if (bytes.length > 750000) throw new IllegalStateException("Logo demasiado grande");
                            File dir = new File(context.getFilesDir(), "kpi_logos");
                            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Sin espacio para logo");
                            File image = new File(dir, "shared_" + Integer.toHexString(item.getKey().hashCode()) + "_" + nextRevision + ".png");
                            try (FileOutputStream out = new FileOutputStream(image)) { out.write(bytes); }
                            value = image.getAbsolutePath();
                        }
                        edit.putString(item.getKey(), value);
                    }
                    if (!edit.commit()) throw new IllegalStateException("No se pudo guardar la plantilla local");
                    loadedUser = user;
                    revision = nextRevision;
                    result.complete(true, "Plantillas actualizadas");
                } catch (Exception e) {
                    result.complete(false, "La plantilla del servidor no se pudo leer.");
                }
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                result.complete(false, "Sin conexión para cargar plantillas KPI.");
            }
        });
    }

    public void save(String cargo, Result result) {
        SessionManager session = new SessionManager(context);
        int user = session.getUserId();
        if (loadedUser != user || revision < 0) {
            result.complete(false, "Recarga las plantillas del servidor antes de guardar.");
            return;
        }
        JsonObject state = new JsonObject();
        try {
            for (Map.Entry<String, ?> item : prefs.getAll().entrySet()) {
                if (!isTemplateKey(item.getKey()) || !(item.getValue() instanceof String)) continue;
                String value = (String)item.getValue();
                if (item.getKey().endsWith("inst_logo_image_path") && !value.isEmpty() && !value.startsWith("data:image/png;base64,")) {
                    ByteArrayOutputStream memory = new ByteArrayOutputStream();
                    try (FileInputStream input = new FileInputStream(value)) {
                        byte[] chunk = new byte[8192];
                        int count;
                        while ((count = input.read(chunk)) != -1) {
                            memory.write(chunk, 0, count);
                            if (memory.size() > 500000) throw new IllegalStateException("El logo supera 500 KB");
                        }
                    }
                    byte[] bytes = memory.toByteArray();
                    value = "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                }
                state.addProperty(item.getKey(), value);
            }
        } catch (Exception e) {
            result.complete(false, "No se pudo leer el logo. Selecciona otra imagen PNG.");
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("revision", revision);
        body.addProperty("cargo_key", KpiOfficialTemplateEngine.normalizeCargoKey(cargo).toUpperCase(Locale.ROOT));
        body.add("estado", state);
        if (body.toString().length() > 900000) {
            result.complete(false, "La plantilla supera el tamaño permitido.");
            return;
        }
        ApiClient.getKpiTemplateService().save(session.getBearerToken(), body).enqueue(new Callback<ApiResponse<JsonObject>>() {
            @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                if (new SessionManager(context).getUserId() != user) return;
                ApiResponse<JsonObject> envelope = response.body();
                if (response.code() == 409) {
                    result.complete(false, "Otro dispositivo cambió la plantilla. Sal y vuelve a abrir el editor.");
                } else if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful() || envelope.getData() == null) {
                    result.complete(false, "No se guardó en el servidor (HTTP " + response.code() + ").");
                } else {
                    revision = envelope.getData().get("revision").getAsLong();
                    result.complete(true, "Plantilla guardada en el servidor.");
                }
            }
            @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable error) {
                result.complete(false, "Sin conexión. La plantilla no se guardó en el servidor.");
            }
        });
    }
}
