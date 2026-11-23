package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.core.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import pe.net.ws.kpiweb.features.kpi.ui.view.KpiZoomableSheetLayout;

/** Read-only detail browser attached to the existing dashboard actions. */
public final class KpiServerReportBrowser {
    private KpiServerReportBrowser() {}

    public static void show(Activity activity, int workerId) {
        show(activity, workerId, 0);
    }

    public static void show(Activity activity, int workerId, int reportId) {
        show(activity, workerId, reportId, null);
    }

    public static void show(Activity activity, int workerId, int reportId, Runnable reviewAction) {
        show(activity, workerId, reportId, "", reviewAction);
    }

    public static void show(Activity activity, int workerId, int reportId,
                            String calculatedPercent, Runnable reviewAction) {
        SessionManager session = new SessionManager(activity);
        if (!session.isLoggedIn() || workerId <= 0) return;
        int user = session.getUserId();
        String token = session.getAccessToken();
        AlertDialog loading = new AlertDialog.Builder(activity).setTitle("Reportes KPI")
                .setMessage("Consultando reportes confirmados en el servidor…")
                .setNegativeButton("Cerrar", null).show();
        load(activity, loading, user, workerId, token, 0, new ArrayList<>(), reportId,
                calculatedPercent, reviewAction);
    }

    private static void load(Activity activity, AlertDialog loading, int user, int worker, String token,
                             long after, List<JsonObject> all, int reportId,
                             String calculatedPercent, Runnable reviewAction) {
        ApiClient.getService().getKpiFormsWorkerPage("Bearer " + token, worker, after, 100)
                .enqueue(new Callback<ApiResponse<JsonObject>>() {
                    @Override public void onResponse(Call<ApiResponse<JsonObject>> call, Response<ApiResponse<JsonObject>> response) {
                        if (response.errorBody() != null) response.errorBody().close();
                        if (!sameSession(activity, user, token) || !loading.isShowing()) return;
                        if (response.code() == 401) {
                            new SessionManager(activity).clearActiveSession();
                            fail(loading, "La sesión venció. Inicia sesión nuevamente."); return;
                        }
                        if (response.code() == 403) { fail(loading, "No tienes permiso para consultar estos reportes."); return; }
                        ApiResponse<JsonObject> envelope = response.body();
                        JsonObject data = envelope == null ? null : envelope.getData();
                        if (!response.isSuccessful() || envelope == null || !envelope.isSuccessful()
                                || data == null || !data.has("id_trabajador")
                                || data.get("id_trabajador").getAsInt() != worker
                                || !data.has("registros") || !data.get("registros").isJsonArray()) {
                            fail(loading, "No se pudo consultar el historial en el servidor."); return;
                        }
                        JsonArray rows = data.getAsJsonArray("registros");
                        for (JsonElement element : rows) if (element.isJsonObject()) all.add(element.getAsJsonObject());
                        long next = data.has("next_after_id") ? data.get("next_after_id").getAsLong() : after;
                        boolean more = data.has("has_more") && data.get("has_more").getAsBoolean();
                        if (more && rows.size() > 0 && next > after) load(activity, loading, user, worker, token, next, all, reportId, calculatedPercent, reviewAction);
                        else if (more) fail(loading, "La consulta quedó incompleta. Reintenta.");
                        else {
                            loading.dismiss();
                            if (reportId > 0) {
                                for (JsonObject report : all) {
                                    if (report.has("id_detalle") && report.get("id_detalle").getAsInt() == reportId) {
                                        detail(activity, user, token, report, calculatedPercent, reviewAction);
                                        return;
                                    }
                                }
                                new AlertDialog.Builder(activity).setTitle("Reporte no disponible")
                                        .setMessage("El formulario ya no está disponible para esta cuenta.")
                                        .setPositiveButton("Cerrar", null).show();
                            } else list(activity, user, token, all);
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<JsonObject>> call, Throwable failure) {
                        if (loading.isShowing()) fail(loading, "Sin conexión. Los reportes del servidor no están disponibles.");
                    }
                });
    }

    private static void list(Activity activity, int user, String token, List<JsonObject> reports) {
        if (!sameSession(activity, user, token)) return;
        if (reports.isEmpty()) {
            new AlertDialog.Builder(activity).setTitle("Reportes KPI")
                    .setMessage("Aún no hay formularios confirmados en MySQL para este trabajador.")
                    .setPositiveButton("Cerrar", null).show();
            return;
        }
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));
        content.setBackgroundColor(Color.rgb(12, 5, 28));
        TextView heading = addLine(activity, content, "Reportes recibidos del servidor", 18, true, 0);
        heading.setTextColor(Color.WHITE);
        addLine(activity, content, "Selecciona un formulario para ver su ficha y los campos guardados.", 13, false, 0)
                .setTextColor(Color.rgb(195, 181, 221));
        AlertDialog[] dialog = new AlertDialog[1];
        for (int i = 0; i < reports.size(); i++) {
            JsonObject row = reports.get(i);
            JsonObject payload = row.has("payload") && row.get("payload").isJsonObject() ? row.getAsJsonObject("payload") : null;
            String date = first(payload, "fecha", "date", "dateRange");
            String label = value(row,"tipo_formulario") + (date.isEmpty() ? "" : " · " + date)
                    + "\nRevisión " + value(row,"revision") + " · Confirmado en MySQL";
            TextView card = addLine(activity, content, label, 15, false, 0);
            card.setTextColor(Color.WHITE);
            card.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
            params.topMargin = dp(activity, 12);
            card.setLayoutParams(params);
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.rgb(35, 14, 65));
            background.setCornerRadius(dp(activity, 12));
            background.setStroke(dp(activity, 1), Color.rgb(115, 66, 181));
            card.setBackground(background);
            card.setOnClickListener(v -> {
                if (dialog[0] != null) dialog[0].dismiss();
                detail(activity, user, token, row, "", null);
            });
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        dialog[0] = new AlertDialog.Builder(activity).setView(scroll)
                .setNegativeButton("Cerrar", null).show();
    }

    private static void detail(Activity activity, int user, String token, JsonObject row,
                               String calculatedPercent, Runnable reviewAction) {
        if (!sameSession(activity, user, token)) return;
        JsonObject payload = row.has("payload") && row.get("payload").isJsonObject()
                ? row.getAsJsonObject("payload") : null;
        if (payload != null && "PROGRAMACION".equals(value(row, "tipo_formulario"))) {
            showZoomReport(activity, "Programación · reporte recibido",
                    KpiServerProgrammerSheet.create(activity, payload), calculatedPercent, reviewAction);
            return;
        }
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, 16);
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundColor(Color.WHITE);
        TextView header = addLine(activity, content, "SIGD  ·  REPORTE KPI\n"
                + value(row, "tipo_formulario").replace('_', ' '), 21, true, 0);
        header.setTextColor(Color.WHITE);
        header.setPadding(dp(activity, 16), dp(activity, 18), dp(activity, 16), dp(activity, 18));
        GradientDrawable headerBackground = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(7, 24, 105), Color.rgb(99, 24, 203)});
        headerBackground.setCornerRadius(dp(activity, 12));
        header.setBackground(headerBackground);
        TextView source = addLine(activity, content, "Confirmado en el servidor · revisión "
                + value(row, "revision"), 12, false, 0);
        source.setTextColor(Color.rgb(75, 65, 112));
        if (calculatedPercent != null && !calculatedPercent.isEmpty()) {
            TextView score = addLine(activity, content,
                    "CUMPLIMIENTO SEGÚN METAS DEL CARGO  ·  " + calculatedPercent + " %",
                    16, true, 0);
            score.setTextColor(Color.rgb(16, 103, 61));
            score.setPadding(dp(activity, 12), dp(activity, 14), dp(activity, 12), dp(activity, 14));
        }
        if (payload == null) addLine(activity, content, "El contenido del reporte no pudo interpretarse.", 15, false, 0);
        else for (java.util.Map.Entry<String, JsonElement> field : payload.entrySet()) {
            LinearLayout section = new LinearLayout(activity);
            section.setOrientation(LinearLayout.VERTICAL);
            section.setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 12));
            GradientDrawable sectionBackground = new GradientDrawable();
            sectionBackground.setColor(Color.rgb(250, 249, 255));
            sectionBackground.setCornerRadius(dp(activity, 10));
            sectionBackground.setStroke(dp(activity, 1), Color.rgb(205, 194, 229));
            section.setBackground(sectionBackground);
            LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
            sectionParams.topMargin = dp(activity, 12);
            content.addView(section, sectionParams);
            renderField(activity, section, field.getKey(), field.getValue(), 0);
        }
        showZoomReport(activity, value(row,"tipo_formulario") + " · reporte recibido", content,
                calculatedPercent, reviewAction);
    }

    private static void showZoomReport(Activity activity, String title, View sheet,
                                       String calculatedPercent, Runnable reviewAction) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Color.rgb(12, 5, 28));
        if (calculatedPercent != null && !calculatedPercent.isEmpty()) {
            TextView score = new TextView(activity);
            score.setText("Cumplimiento calculado con las metas del cargo: " + calculatedPercent + " %");
            score.setTextColor(Color.WHITE);
            score.setTextSize(15);
            score.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 8));
            panel.addView(score);
        }
        LinearLayout controls = new LinearLayout(activity);
        controls.setGravity(android.view.Gravity.CENTER);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        TextView minus = zoomControl(activity, "−");
        TextView percent = zoomControl(activity, "100%");
        TextView plus = zoomControl(activity, "+");
        controls.addView(minus);
        controls.addView(percent);
        controls.addView(plus);
        panel.addView(controls, new LinearLayout.LayoutParams(-1, dp(activity, 48)));
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        KpiZoomableSheetLayout zoom = new KpiZoomableSheetLayout(activity);
        zoom.addView(sheet, new FrameLayout.LayoutParams(-1, -2));
        zoom.setOnZoomChangeListener((scale, value) -> percent.setText(value + "%"));
        minus.setOnClickListener(v -> zoom.zoomOut());
        plus.setOnClickListener(v -> zoom.zoomIn());
        percent.setOnClickListener(v -> zoom.resetZoom());
        scroll.addView(zoom, new ScrollView.LayoutParams(-1, -2));
        int height = Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.70f);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, height));
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(title)
                .setView(panel).setNegativeButton("Volver", null);
        if (reviewAction != null) builder.setPositiveButton("Calcular y decidir", (d, which) -> reviewAction.run());
        AlertDialog dialog = builder.show();
        Window window = dialog.getWindow();
        if (window != null) window.setLayout(
                Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.96f),
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static TextView zoomControl(Activity activity, String label) {
        TextView button = new TextView(activity);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(18);
        button.setGravity(android.view.Gravity.CENTER);
        button.setContentDescription(label.equals("+") ? "Ampliar reporte"
                : label.equals("−") ? "Reducir reporte" : "Restablecer zoom");
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 76), -1));
        return button;
    }

    private static void renderField(Activity activity, LinearLayout parent, String key,
                                    JsonElement value, int depth) {
        String label = label(key);
        if (value == null || value.isJsonNull()) {
            addLine(activity, parent, label + ": —", 15, false, depth);
        } else if (value.isJsonObject()) {
            addLine(activity, parent, label, 16, true, depth);
            for (java.util.Map.Entry<String, JsonElement> child : value.getAsJsonObject().entrySet())
                renderField(activity, parent, child.getKey(), child.getValue(), depth + 1);
        } else if (value.isJsonArray()) {
            JsonArray items = value.getAsJsonArray();
            addLine(activity, parent, label + (items.size() == 0 ? ": sin registros" : " (" + items.size() + ")"),
                    16, true, depth);
            for (int i = 0; i < items.size(); i++)
                renderField(activity, parent, "Registro " + (i + 1), items.get(i), depth + 1);
        } else {
            String text = value.getAsJsonPrimitive().isBoolean()
                    ? (value.getAsBoolean() ? "Sí" : "No") : value.getAsString();
            addLine(activity, parent, label + ": " + (text.isEmpty() ? "—" : text),
                    15, false, depth);
        }
    }

    private static TextView addLine(Activity activity, LinearLayout parent, String text,
                                int size, boolean heading, int depth) {
        TextView line = new TextView(activity);
        line.setText(text);
        line.setTextSize(size);
        line.setTextColor(heading ? Color.rgb(72, 35, 134) : Color.rgb(39, 32, 49));
        if (heading) line.setTypeface(null, Typeface.BOLD);
        line.setTextIsSelectable(!heading);
        line.setPadding(dp(activity, Math.min(depth, 4) * 12), dp(activity, heading ? 14 : 4), 0, dp(activity, 4));
        parent.addView(line, new LinearLayout.LayoutParams(-1, -2));
        return line;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static String label(String key) {
        String spaced = key.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2").trim();
        return spaced.isEmpty() ? "Campo" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private static String first(JsonObject data, String... fields) {
        if (data != null) for (String field : fields) if (data.has(field) && !data.get(field).isJsonNull()) return data.get(field).getAsString();
        return "";
    }
    private static String value(JsonObject data, String field) {
        return data.has(field) && !data.get(field).isJsonNull() ? data.get(field).getAsString() : "";
    }
    private static boolean sameSession(Context context, int user, String token) {
        SessionManager current = new SessionManager(context);
        return current.isLoggedIn() && current.getUserId() == user && token.equals(current.getAccessToken());
    }
    private static void fail(AlertDialog loading, String message) {
        loading.setMessage(message);
    }
}
