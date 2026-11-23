package pe.net.ws.kpiweb.features.kpi.export;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Gestor oficial de impresión y detección de impresoras nativo para el SIGD.
 * Se comunica directamente con el PrintManager de Android para detectar impresoras
 * de red (WiFi), impresoras vinculadas por Bluetooth y servicios de impresión locales.
 */
public class KpiPrintManager {

    public static void printView(Activity activity, View viewToPrint, String jobName) {
        if (activity == null || viewToPrint == null) {
            return;
        }

        PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        if (printManager == null) {
            Toast.makeText(activity, "El servicio de impresión no está disponible en este dispositivo.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String cleanJobName = (jobName != null && !jobName.trim().isEmpty())
                ? jobName.trim().replace(" ", "_")
                : "Reporte_Semanal_SIGD";

        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setColorMode(PrintAttributes.COLOR_MODE_COLOR);
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);

        printManager.print(cleanJobName, new PrintDocumentAdapter() {
            private PrintedPdfDocument printedPdfDocument;

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal,
                                 LayoutResultCallback callback, Bundle metadata) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                printedPdfDocument = new PrintedPdfDocument(activity, newAttributes);
                PrintDocumentInfo info = new PrintDocumentInfo.Builder(cleanJobName + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build();

                callback.onLayoutFinished(info, !newAttributes.equals(oldAttributes));
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal,
                                WriteResultCallback callback) {
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    if (printedPdfDocument != null) {
                        printedPdfDocument.close();
                        printedPdfDocument = null;
                    }
                    return;
                }

                PdfDocument.Page page = printedPdfDocument.startPage(0);
                Canvas canvas = page.getCanvas();

                // Medidas del papel impreso (en puntos de impresión)
                int pageWidth = page.getInfo().getPageWidth();
                int pageHeight = page.getInfo().getPageHeight();

                // Capturar el contenido del View
                int viewWidth = viewToPrint.getWidth();
                int viewHeight = viewToPrint.getHeight();

                if (viewWidth <= 0 || viewHeight <= 0) {
                    viewToPrint.measure(
                            View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    viewWidth = viewToPrint.getMeasuredWidth();
                    viewHeight = viewToPrint.getMeasuredHeight();
                    viewToPrint.layout(0, 0, viewWidth, viewHeight);
                }

                // Escalar proporcionadamente la hoja completa al ancho de la hoja A4 (con márgenes de 16pt)
                int marginH = 18;
                int marginV = 20;
                int usableWidth = pageWidth - (marginH * 2);
                int usableHeight = pageHeight - (marginV * 2);

                float scaleX = (float) usableWidth / (float) viewWidth;
                float scaleY = (float) usableHeight / (float) viewHeight;
                float scale = Math.min(scaleX, scaleY);

                canvas.save();
                canvas.translate(marginH, marginV);
                canvas.scale(scale, scale);
                viewToPrint.draw(canvas);
                canvas.restore();

                printedPdfDocument.finishPage(page);

                try {
                    FileOutputStream fos = new FileOutputStream(destination.getFileDescriptor());
                    printedPdfDocument.writeTo(fos);
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException e) {
                    callback.onWriteFailed(e.toString());
                } finally {
                    if (printedPdfDocument != null) {
                        printedPdfDocument.close();
                        printedPdfDocument = null;
                    }
                }
            }
        }, builder.build());
    }
}

