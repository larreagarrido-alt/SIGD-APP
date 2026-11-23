package pe.net.ws.kpiweb.features.kpi.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;

/**
 * Generador nativo de archivos CSV para auditoría y análisis en Excel de las entregas KPI.
 * Totalmente autónomo y offline; no depende de MySQL ni PHP.
 */
public class KpiCsvExporter {

    public static File exportEntriesToCsv(Context context, List<KpiRecordEntry> entries, String roleFilter) {
        if (context == null || entries == null || entries.isEmpty()) {
            if (context != null) {
                Toast.makeText(context, "No hay registros para exportar a CSV.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        try {
            File exportDir = new File(context.getExternalFilesDir(null), "kpi_exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = "KPI_Report_" + roleFilter.replace(" ", "_") + "_" + sdf.format(new Date()) + ".csv";
            File csvFile = new File(exportDir, fileName);

            FileOutputStream fos = new FileOutputStream(csvFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            // UTF-8 BOM para que Excel en Windows reconozca tildes y caracteres especiales
            writer.write("\ufeff");

            // Encabezados de auditoría
            writer.write("ID Entrega,Fecha,Puesto,Colaborador,Indicador,Versión KPI,Meta Usada,Valor Registrado,Cumplimiento %,Puntos Ponderados,Huella Digital,Salida Terreno,Motivo Terreno,Estado\n");

            for (KpiRecordEntry e : entries) {
                String line = String.format(Locale.US,
                        "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,%.2f,%.2f,\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        clean(e.getRecordId()),
                        clean(e.getDate()),
                        clean(e.getRole()),
                        clean(e.getEmployeeId()),
                        clean(e.getKpiName()),
                        e.getKpiVersionUsed(),
                        e.getTargetValueUsed(),
                        e.getActualValue(),
                        e.getComputedCompliance(),
                        e.getComputedWeightedScore(),
                        e.isBiometricVerified() ? "SI" : "NO",
                        e.isFieldWork() ? "SI" : "NO",
                        clean(e.getFieldWorkReason()),
                        clean(e.getDeliveryStatus())
                );
                writer.write(line);
            }

            writer.flush();
            writer.close();
            fos.close();

            shareFile(context, csvFile, "text/csv", "Exportación de KPIs en CSV");
            return csvFile;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al generar archivo CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    private static void shareFile(Context context, File file, String mimeType, String title) {
        try {
            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, title));
        } catch (Exception e) {
            Toast.makeText(context, "✓ Archivo generado en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}
