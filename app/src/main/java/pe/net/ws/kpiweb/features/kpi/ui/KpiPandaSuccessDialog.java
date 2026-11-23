package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import pe.net.ws.kpiweb.R;

/** Confirmation of local capture; server receipt is checked separately by sync. */
public final class KpiPandaSuccessDialog {

    private KpiPandaSuccessDialog() {}

    public static void show(@NonNull Context context,
                            @NonNull String roleName,
                            int compliancePercent) {
        String lower = roleName.toLowerCase();
        boolean isPracticante = lower.contains("junior")
                || lower.contains("practicante")
                || roleName.equalsIgnoreCase("Programador");
        int pts = Math.max(10, compliancePercent);
        show(context, roleName, isPracticante, "Jornada Registrada", compliancePercent, pts, null);
    }

    public static void show(@NonNull Context context,
                            @NonNull String roleName,
                            boolean isPracticanteMode,
                            @NonNull String periodLabel,
                            int compliancePercent,
                            int pointsEarned,
                            Runnable onDismissAction) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        boolean isDark = KpiThemeManager.isDarkMode(context);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(context, 24);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(context, 24));
        cardBg.setColor(isDark ? Color.parseColor("#151E36") : Color.WHITE);
        cardBg.setStroke(dp(context, 2),
                isDark
                        ? (isPracticanteMode ? Color.parseColor("#38BDF8") : Color.parseColor("#6366F1"))
                        : (isPracticanteMode ? Color.parseColor("#D946EF") : Color.parseColor("#A21CAF")));
        card.setBackground(cardBg);

        // Insignia superior del modo (Colaborador Elegante vs Practicante Eléctrico)
        TextView tvModeBadge = new TextView(context);
        tvModeBadge.setText("FICHA REGISTRADA");
        tvModeBadge.setTextSize(11f);
        tvModeBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvModeBadge.setTextColor(isDark
                ? (isPracticanteMode ? Color.parseColor("#7DD3FC") : Color.parseColor("#A5B4FC"))
                : (isPracticanteMode ? Color.parseColor("#C026D3") : Color.parseColor("#86198F")));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(dp(context, 999));
        badgeBg.setColor(isPracticanteMode
                ? Color.parseColor(isDark ? "#164E63" : "#CFFAFE")
                : Color.parseColor(isDark ? "#451A03" : "#FEF3C7"));
        tvModeBadge.setBackground(badgeBg);
        tvModeBadge.setPadding(dp(context, 14), dp(context, 6), dp(context, 14), dp(context, 6));
        card.addView(tvModeBadge);

        // Título Principal
        TextView tvTitle = new TextView(context);
        tvTitle.setText("Ficha guardada");
        tvTitle.setTextSize(22f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(isDark ? Color.parseColor("#F8FAFC") : Color.parseColor("#0F172A"));
        tvTitle.setGravity(Gravity.CENTER);
        card.addView(tvTitle);

        // El guardado local no acredita una evaluación ni puntos oficiales.
        TextView tvRelief = new TextView(context);
        tvRelief.setText("Consulta el estado del servidor");
        tvRelief.setTextSize(16f);
        tvRelief.setTypeface(Typeface.DEFAULT_BOLD);
        tvRelief.setTextColor(Color.parseColor("#16A34A"));
        tvRelief.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams reliefParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reliefParams.topMargin = dp(context, 4);
        tvRelief.setLayoutParams(reliefParams);
        card.addView(tvRelief);

        // Resumen del envío
        TextView tvBody = new TextView(context);
        tvBody.setText("La ficha de " + roleName + " (" + periodLabel + ") se guardó en el dispositivo. " +
                "Cuando el servidor confirme su recepción, podrás verla en Reportes.");
        tvBody.setTextSize(13f);
        tvBody.setTextColor(isDark ? Color.parseColor("#CBD5E1") : Color.parseColor("#334155"));
        tvBody.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = dp(context, 12);
        bodyParams.bottomMargin = dp(context, 18);
        tvBody.setLayoutParams(bodyParams);
        card.addView(tvBody);

        // Botón Cerrar
        TextView btnOk = new TextView(context);
        btnOk.setText("Entendido");
        btnOk.setTextSize(14f);
        btnOk.setTypeface(Typeface.DEFAULT_BOLD);
        btnOk.setTextColor(Color.WHITE);
        btnOk.setGravity(Gravity.CENTER);
        btnOk.setBackgroundResource(isPracticanteMode
                ? R.drawable.kpi_bg_btn_electric
                : R.drawable.kpi_bg_btn_elegant);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48));
        btnOk.setLayoutParams(btnParams);
        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDismissAction != null) {
                onDismissAction.run();
            }
        });
        card.addView(btnOk);

        dialog.setContentView(card);
        if (dialog.getWindow() != null) {
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}

