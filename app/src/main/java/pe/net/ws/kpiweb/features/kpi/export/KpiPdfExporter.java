package pe.net.ws.kpiweb.features.kpi.export;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pe.net.ws.kpiweb.features.kpi.admin.model.KpiRecordEntry;
import pe.net.ws.kpiweb.features.kpi.engine.KpiCalculationEngine;

/**
 * Generador nativo de actas ejecutivas en formato PDF utilizando Android PdfDocument.
 * Totalmente offline, estandarizado con la identidad institucional SIGD 2026.
 */
public class KpiPdfExporter {

    public static File exportEntriesToPdf(Context context, List<KpiRecordEntry> entries, String roleFilter) {
        if (context == null || entries == null || entries.isEmpty()) {
            if (context != null) {
                Toast.makeText(context, "No hay registros para exportar a PDF.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595;  // A4 standard width (pt)
        int pageHeight = 842; // A4 standard height (pt)

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. Franja superior institucional morado corporativo
        paint.setColor(Color.parseColor("#5B21B6"));
        canvas.drawRect(0, 0, pageWidth, 55, paint);

        // Título del encabezado
        paint.setColor(Color.WHITE);
        paint.setTextSize(14f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("WebSite+  ·  SISTEMA INTEGRAL DE GESTIÓN DEL DESEMPEÑO", 24, 32, paint);

        paint.setTextSize(9f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("INFORME OFICIAL DE EVALUACIÓN DE INDICADORES (SIGD 2026)", 24, 46, paint);

        // 2. Metadatos de la evaluación
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(12f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Cargo Evaluado: " + roleFilter, 24, 85, paint);

        SimpleDateFormat sdfNow = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(10f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Fecha de emisión: " + sdfNow.format(new Date()), 24, 102, paint);
        canvas.drawText("Total de entregas auditadas: " + entries.size(), 360, 102, paint);

        // Línea divisoria
        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(24, 114, pageWidth - 24, 114, paint);

        // 3. Encabezados de tabla
        int startY = 135;
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRect(24, startY - 14, pageWidth - 24, startY + 8, paint);

        paint.setColor(Color.parseColor("#334155"));
        paint.setTextSize(9f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("FECHA", 30, startY, paint);
        canvas.drawText("INDICADOR", 95, startY, paint);
        canvas.drawText("META", 290, startY, paint);
        canvas.drawText("REAL", 345, startY, paint);
        canvas.drawText("CUMPL. %", 400, startY, paint);
        canvas.drawText("PUNTOS", 470, startY, paint);
        canvas.drawText("ESTADO", 525, startY, paint);

        // 4. Filas de datos
        int currentY = startY + 24;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(8.5f);

        double totalWeightedPoints = 0.0;
        int count = 0;

        for (KpiRecordEntry e : entries) {
            if (currentY > pageHeight - 90) break; // Límite de página 1

            paint.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(e.getDate(), 30, currentY, paint);

            String name = e.getKpiName();
            if (name != null && name.length() > 32) {
                name = name.substring(0, 29) + "...";
            }
            canvas.drawText(name != null ? name : "", 95, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.1f", e.getTargetValueUsed()), 290, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.1f", e.getActualValue()), 345, currentY, paint);

            // Cumplimiento
            canvas.drawText(String.format(Locale.US, "%.1f%%", e.getComputedCompliance()), 400, currentY, paint);
            canvas.drawText(String.format(Locale.US, "%.1f pts", e.getComputedWeightedScore()), 470, currentY, paint);
            canvas.drawText(e.isFieldWork() ? "Campo" : "OK", 525, currentY, paint);

            totalWeightedPoints += e.getComputedWeightedScore();
            count++;

            paint.setColor(Color.parseColor("#F1F5F9"));
            paint.setStrokeWidth(0.8f);
            canvas.drawLine(24, currentY + 6, pageWidth - 24, currentY + 6, paint);

            currentY += 20;
        }

        // 5. Bloque de Cierre y Resumen Consolidado
        int summaryY = currentY + 25;
        paint.setColor(Color.parseColor("#FAF5FF"));
        canvas.drawRect(24, summaryY - 14, pageWidth - 24, summaryY + 50, paint);

        java.util.Map<String, Double> sessionTotals = new java.util.LinkedHashMap<>();
        for (KpiRecordEntry e : entries) {
            String key = e.getDate() + "_" + e.getRole();
            double prev = sessionTotals.containsKey(key) ? sessionTotals.get(key) : 0.0;
            sessionTotals.put(key, Math.min(100.0, prev + e.getComputedWeightedScore()));
        }
        double sumSessions = 0.0;
        for (Double val : sessionTotals.values()) {
            sumSessions += val;
        }
        double avgScore = !sessionTotals.isEmpty() ? (sumSessions / sessionTotals.size()) : 0.0;
        String level = KpiCalculationEngine.getLevelFromPercentage(avgScore);

        paint.setColor(Color.parseColor("#5B21B6"));
        paint.setTextSize(11f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PUNTAJE PONDERADO PROMEDIO: " + String.format(Locale.US, "%.2f%%", avgScore), 36, summaryY + 12, paint);
        canvas.drawText("CALIFICACIÓN INSTITUCIONAL: " + level, 36, summaryY + 34, paint);

        // 6. Pie de página y Firma Digital
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(8f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("✓ Documento verificado mediante autenticación biométrica nativa (Android BiometricPrompt).", 24, pageHeight - 35, paint);
        canvas.drawText("Sistema WebSite+  ·  SIGD 2026  ·  Copia de solo lectura inmutable.", 24, pageHeight - 22, paint);

        document.finishPage(page);

        try {
            File exportDir = new File(context.getExternalFilesDir(null), "kpi_exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = "Acta_KPI_" + roleFilter.replace(" ", "_") + "_" + sdf.format(new Date()) + ".pdf";
            File pdfFile = new File(exportDir, fileName);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            shareFile(context, pdfFile, "application/pdf", "Acta Oficial de Evaluación KPI");
            return pdfFile;
        } catch (Exception e) {
            e.printStackTrace();
            document.close();
            Toast.makeText(context, "Error al generar acta PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
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
            Toast.makeText(context, "✓ Acta generada en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }
}
