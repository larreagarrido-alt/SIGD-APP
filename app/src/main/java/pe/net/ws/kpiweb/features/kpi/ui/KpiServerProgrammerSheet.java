package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;

import pe.net.ws.kpiweb.R;

/** Fills the teammate's original Programación sheet from one verified server snapshot. */
final class KpiServerProgrammerSheet {
    private KpiServerProgrammerSheet() {}

    static View create(Activity activity, JsonObject report) {
        View sheet = activity.getLayoutInflater().inflate(R.layout.kpi_view_official_programmer_sheet, null, false);
        replaceStaticLabels(sheet, value(report, "rol"));
        hide(sheet, R.id.llProgDaySelectorBar);
        hide(sheet, R.id.llProgDividerScissor);
        boolean afternoon = value(report, "turno").toLowerCase(Locale.ROOT).contains("tarde");
        hide(sheet, afternoon ? R.id.llProgShiftMorningContainer : R.id.llProgShiftAfternoonContainer);
        String prefix = afternoon ? "T" : "M";
        set(sheet, R.id.tvProgSelectedDayLabel, value(report, "fecha"));
        set(sheet, afternoon ? R.id.tvProgT_Fecha : R.id.tvProgM_Fecha, value(report, "fecha"));
        set(sheet, afternoon ? R.id.tvProgT_Practicante : R.id.tvProgM_Practicante, value(report, "practicante"));
        set(sheet, afternoon ? R.id.tvProgT_Supervisor : R.id.tvProgM_Supervisor, value(report, "supervisor"));
        set(sheet, afternoon ? R.id.tvProgT_Turno : R.id.tvProgM_Turno, value(report, "turno"));
        set(sheet, afternoon ? R.id.tvProgT_ResAsignadas : R.id.tvProgM_ResAsignadas, value(report, "tareasAsignadas"));
        set(sheet, afternoon ? R.id.tvProgT_ResCompletadas : R.id.tvProgM_ResCompletadas, value(report, "tareasCompletadas"));
        set(sheet, afternoon ? R.id.tvProgT_ResPendientes : R.id.tvProgM_ResPendientes, value(report, "tareasPendientes"));
        set(sheet, afternoon ? R.id.tvProgT_ResFuncionalidades : R.id.tvProgM_ResFuncionalidades, value(report, "funcionalidadesDesarrolladas"));
        set(sheet, afternoon ? R.id.tvProgT_ResIncidencias : R.id.tvProgM_ResIncidencias, value(report, "bugsDetectados"));
        set(sheet, afternoon ? R.id.tvProgT_ResCumplimiento : R.id.tvProgM_ResCumplimiento,
                value(report, "cumplimientoMeta") + " % (cálculo del formulario)");
        set(sheet, afternoon ? R.id.tvProgT_ScorePill : R.id.tvProgM_ScorePill,
                "PUNTAJE LOCAL: " + value(report, "puntaje") + " / 20");
        set(sheet, afternoon ? R.id.tvProgT_IncidenciaCheck : R.id.tvProgM_IncidenciaCheck,
                booleanValue(report, "huboDificultad") ? "Sí" : "No");
        set(sheet, afternoon ? R.id.tvProgT_IncidenciaDetalle : R.id.tvProgM_IncidenciaDetalle,
                value(report, "detalleDificultad"));
        set(sheet, afternoon ? R.id.tvProgT_FirmaPracticante : R.id.tvProgM_FirmaPracticante,
                value(report, "firmaPracticante"));
        set(sheet, afternoon ? R.id.tvProgT_FirmaSupervisor : R.id.tvProgM_FirmaSupervisor,
                value(report, "firmaSupervisor"));
        fillActivities(activity, sheet.findViewById(afternoon ? R.id.llProgT_ActsContainer : R.id.llProgM_ActsContainer), report);
        fillCriteria(activity, sheet.findViewById(afternoon ? R.id.llProgT_EvalCriteriaContainer : R.id.llProgM_EvalCriteriaContainer), report);
        LinearLayout root = (LinearLayout) sheet;
        addDetail(activity, root, "Observación del supervisor", value(report, "observacionSupervisor"));
        addDetail(activity, root, "Salida a campo", booleanValue(report, "esSalidaCampo") ? "Sí" : "No");
        if (booleanValue(report, "esSalidaCampo")) {
            addDetail(activity, root, "Motivo de salida", value(report, "motivoSalidaCampo"));
            addDetail(activity, root, "Horario de campo", value(report, "horaInicioCampo") + " – " + value(report, "horaFinCampo"));
        }
        if (report.has("metas") && report.get("metas").isJsonArray()) {
            addDetail(activity, root, "Metas registradas", "");
            for (JsonElement goal : report.getAsJsonArray("metas"))
                if (goal.isJsonObject()) {
                    JsonObject item = goal.getAsJsonObject();
                    addDetail(activity, root, value(item, "categoria"), value(item, "metaTexto"));
                }
        }
        addDetail(activity, root, "Estado", "Reporte recibido; evaluación oficial pendiente");
        return sheet;
    }

    private static void fillActivities(Activity activity, LinearLayout holder, JsonObject report) {
        if (holder == null) return;
        holder.removeAllViews();
        JsonArray rows = report.has("actividades") && report.get("actividades").isJsonArray()
                ? report.getAsJsonArray("actividades") : new JsonArray();
        if (rows.size() == 0) { addDetail(activity, holder, "Actividades", "Sin actividades registradas"); return; }
        for (JsonElement element : rows) {
            if (!element.isJsonObject()) continue;
            JsonObject activityRow = element.getAsJsonObject();
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(100f);
            row.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.round(24 * activity.getResources().getDisplayMetrics().density)));
            addCell(activity, row, value(activityRow, "numero"), 6);
            addCell(activity, row, value(activityRow, "proyecto"), 22);
            addCell(activity, row, value(activityRow, "tipo"), 14);
            addCell(activity, row, value(activityRow, "descripcion"), 24);
            addCell(activity, row, value(activityRow, "horaInicio"), 8);
            addCell(activity, row, value(activityRow, "horaFin"), 8);
            addCell(activity, row, value(activityRow, "estado"), 10);
            addCell(activity, row, value(activityRow, "commitHash"), 8);
            holder.addView(row);
        }
    }

    private static void addCell(Activity activity, LinearLayout row, String value, int weight) {
        TextView cell = new TextView(activity);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, -1, weight));
        cell.setBackgroundResource(R.drawable.kpi_sigd_cell_data);
        cell.setText(value);
        cell.setTextSize(6.5f);
        cell.setTextColor(Color.rgb(30, 27, 75));
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setSingleLine(true);
        cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.setPadding(2, 0, 2, 0);
        row.addView(cell);
    }

    private static void fillCriteria(Activity activity, LinearLayout holder, JsonObject report) {
        if (holder == null) return;
        holder.removeAllViews();
        if (!report.has("criteriosEvaluacion") || !report.get("criteriosEvaluacion").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : report.getAsJsonObject("criteriosEvaluacion").entrySet())
            addDetail(activity, holder, entry.getKey(), entry.getValue().isJsonNull() ? "—"
                    : entry.getValue().getAsBoolean() ? "Sí" : "No");
    }

    private static void addDetail(Activity activity, LinearLayout holder, String title, String value) {
        TextView line = new TextView(activity);
        line.setText((title.isEmpty() ? "Dato" : title) + ": " + (value.isEmpty() ? "—" : value));
        line.setTextColor(Color.rgb(30, 27, 75));
        line.setTextSize(11);
        line.setGravity(Gravity.START);
        line.setPadding(8, 6, 8, 6);
        holder.addView(line);
    }

    private static String value(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "—" : element.getAsString();
    }

    private static boolean booleanValue(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    private static void set(View root, int id, String value) {
        TextView text = root.findViewById(id);
        if (text != null) text.setText(value);
    }

    private static void hide(View root, int id) {
        View target = root.findViewById(id);
        if (target != null) target.setVisibility(View.GONE);
    }

    private static void replaceStaticLabels(View view, String role) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            String original = text.getText().toString();
            if (original.contains("REPORTE DIARIO OFICIAL SIGD"))
                text.setText(original.replace("REPORTE DIARIO OFICIAL SIGD", "REPORTE DIARIO RECIBIDO"));
            if (original.equals("PRACTICANTE DE SOFTWARE Y PROGRAMACIÓN WEB"))
                text.setText(role);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++)
                replaceStaticLabels(group.getChildAt(index), role);
        }
    }
}
