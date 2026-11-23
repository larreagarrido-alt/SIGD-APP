package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import pe.net.ws.kpiweb.R;

/**
 * Teclado personalizado interno (Numérico 123 y de Letras ABC) que reemplaza
 * tanto a los antiguos controles [- 0 +] como al teclado tradicional del sistema Android.
 */
public class KpiCustomKeypadDialog {

    public interface OnNumberConfirmedListener {
        void onConfirmed(int value);
    }

    public interface OnTextConfirmedListener {
        void onConfirmed(String text);
    }

    public static void showNumeric(Context context,
                                   String title,
                                   String subtitle,
                                   int currentValue,
                                   int goalHint,
                                   OnNumberConfirmedListener listener) {
        showInternal(context, title, subtitle, String.valueOf(Math.max(0, currentValue)),
                goalHint, true, true, valueStr -> {
                    int parsed = 0;
                    try {
                        parsed = Integer.parseInt(valueStr.replaceAll("[^0-9]", ""));
                    } catch (Exception ignored) {
                    }
                    if (listener != null) {
                        listener.onConfirmed(Math.max(0, parsed));
                    }
                });
    }

    public static void showText(Context context,
                                String title,
                                String subtitle,
                                String currentText,
                                OnTextConfirmedListener listener) {
        showInternal(context, title, subtitle, currentText != null ? currentText : "",
                0, false, false, valueStr -> {
                    if (listener != null) {
                        listener.onConfirmed(valueStr != null ? valueStr.trim() : "");
                    }
                });
    }

    private static void showInternal(Context context,
                                     String title,
                                     String subtitle,
                                     String initialValue,
                                     int goalHint,
                                     boolean startNumeric,
                                     boolean numericOnlyField,
                                     OnTextConfirmedListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int colorCardBg = ContextCompat.getColor(context, R.color.kpi_card_bg);
        int colorSurfaceVariant = ContextCompat.getColor(context, R.color.kpi_surface_variant);
        int colorBorder = ContextCompat.getColor(context, R.color.kpi_border);
        int colorTextPrimary = ContextCompat.getColor(context, R.color.kpi_text_primary);
        int colorTextSecondary = ContextCompat.getColor(context, R.color.kpi_text_secondary);
        int colorPrimary = ContextCompat.getColor(context, R.color.kpi_primary);

        final StringBuilder buffer = new StringBuilder(initialValue != null ? initialValue : "");
        final boolean[] firstNumericKeystroke = {startNumeric};
        final boolean[] isUppercase = {true};
        final boolean[] isNumericMode = {startNumeric};

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 10));

        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(colorCardBg);
        float topRadius = dp(context, 20);
        sheetBg.setCornerRadii(new float[]{topRadius, topRadius, topRadius, topRadius, 0, 0, 0, 0});
        sheetBg.setStroke(dp(context, 1), colorBorder);
        root.setBackground(sheetBg);

        // Barra superior (Handle)
        View handle = new View(context);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(context, 38), dp(context, 4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(context, 6);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(colorBorder);
        handleBg.setCornerRadius(dp(context, 4));
        handle.setBackground(handleBg);
        root.addView(handle, handleParams);

        // Cabecera: Título + Botón Cerrar
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextColor(colorTextPrimary);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        titleCol.addView(tvTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(context);
            tvSub.setText(subtitle);
            tvSub.setTextColor(colorTextSecondary);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            titleCol.addView(tvSub);
        }
        headerRow.addView(titleCol, titleColParams);

        TextView btnClose = new TextView(context);
        btnClose.setText("✕");
        btnClose.setTextColor(colorTextSecondary);
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnClose.setTypeface(null, Typeface.BOLD);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setBackground(createKeyDrawable(context, colorSurfaceVariant, colorBorder, 8));
        btnClose.setOnClickListener(v -> dialog.dismiss());
        headerRow.addView(btnClose, new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)));

        root.addView(headerRow);

        // Caja de visualización del valor en vivo
        LinearLayout displayBox = new LinearLayout(context);
        displayBox.setOrientation(LinearLayout.HORIZONTAL);
        displayBox.setGravity(Gravity.CENTER_VERTICAL);
        displayBox.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));
        displayBox.setBackgroundResource(R.drawable.kpi_bg_goal_card_selected);
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        displayParams.topMargin = dp(context, 6);
        displayParams.bottomMargin = dp(context, 6);

        TextView tvDisplayValue = new TextView(context);
        tvDisplayValue.setText(buffer.length() == 0 ? (startNumeric ? "0" : "Escribe aquí...") : buffer.toString());
        tvDisplayValue.setTextColor(colorTextPrimary);
        tvDisplayValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, startNumeric ? 20 : 14);
        tvDisplayValue.setTypeface(null, Typeface.BOLD);
        tvDisplayValue.setGravity(startNumeric ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);
        displayBox.addView(tvDisplayValue, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnClearAll = new TextView(context);
        btnClearAll.setText("Limpiar");
        btnClearAll.setTextColor(colorPrimary);
        btnClearAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnClearAll.setTypeface(null, Typeface.BOLD);
        btnClearAll.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
        btnClearAll.setOnClickListener(v -> {
            buffer.setLength(0);
            firstNumericKeystroke[0] = false;
            tvDisplayValue.setText(isNumericMode[0] ? "0" : "");
        });
        displayBox.addView(btnClearAll);
        root.addView(displayBox, displayParams);

        // Selector de Modo: [ 🔢 Teclado Numérico (123) | 🔤 Teclado de Letras (ABC) ]
        LinearLayout modeSwitchRow = new LinearLayout(context);
        modeSwitchRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams modeSwitchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 32));
        modeSwitchParams.bottomMargin = dp(context, 6);

        TextView tabNumeric = new TextView(context);
        tabNumeric.setText("Teclado Numérico");
        tabNumeric.setGravity(Gravity.CENTER);
        tabNumeric.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tabNumeric.setTypeface(null, Typeface.BOLD);

        TextView tabLetters = new TextView(context);
        tabLetters.setText("Teclado de Letras");
        tabLetters.setGravity(Gravity.CENTER);
        tabLetters.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tabLetters.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams tabLeftParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        tabLeftParams.rightMargin = dp(context, 4);
        LinearLayout.LayoutParams tabRightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        tabRightParams.leftMargin = dp(context, 4);

        modeSwitchRow.addView(tabNumeric, tabLeftParams);
        modeSwitchRow.addView(tabLetters, tabRightParams);
        root.addView(modeSwitchRow, modeSwitchParams);

        // Contenedores de ambos teclados
        LinearLayout numericPadContainer = new LinearLayout(context);
        numericPadContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout letterPadContainer = new LinearLayout(context);
        letterPadContainer.setOrientation(LinearLayout.VERTICAL);

        Runnable updateDisplay = () -> {
            if (buffer.length() == 0) {
                tvDisplayValue.setText(isNumericMode[0] ? "0" : "");
            } else {
                tvDisplayValue.setText(buffer.toString());
            }
        };

        Runnable refreshTabsVisual = () -> {
            if (isNumericMode[0]) {
                tabNumeric.setBackground(createKeyDrawable(context, colorPrimary, colorPrimary, 10));
                tabNumeric.setTextColor(Color.WHITE);
                tabLetters.setBackground(createKeyDrawable(context, colorSurfaceVariant, colorBorder, 10));
                tabLetters.setTextColor(colorTextSecondary);
                numericPadContainer.setVisibility(View.VISIBLE);
                letterPadContainer.setVisibility(View.GONE);
            } else {
                tabLetters.setBackground(createKeyDrawable(context, colorPrimary, colorPrimary, 10));
                tabLetters.setTextColor(Color.WHITE);
                tabNumeric.setBackground(createKeyDrawable(context, colorSurfaceVariant, colorBorder, 10));
                tabNumeric.setTextColor(colorTextSecondary);
                numericPadContainer.setVisibility(View.GONE);
                letterPadContainer.setVisibility(View.VISIBLE);
            }
        };

        tabNumeric.setOnClickListener(v -> {
            isNumericMode[0] = true;
            refreshTabsVisual.run();
        });

        tabLetters.setOnClickListener(v -> {
            isNumericMode[0] = false;
            refreshTabsVisual.run();
        });

        // =====================================================
        // CONSTRUIR TECLADO NUMÉRICO (123)
        // =====================================================
        LinearLayout quickNumRow = new LinearLayout(context);
        quickNumRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams quickNumParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 30));
        quickNumParams.bottomMargin = dp(context, 5);

        int[] presets = goalHint > 0 ? new int[]{0, 1, 2, goalHint, 6} : new int[]{0, 1, 2, 4, 6};
        for (int i = 0; i < presets.length; i++) {
            final int val = presets[i];
            String chipText = (i == 3 && goalHint > 0) ? ("Meta (" + val + ")") : String.valueOf(val);
            TextView chip = new TextView(context);
            chip.setText(chipText);
            chip.setGravity(Gravity.CENTER);
            chip.setTextColor(colorPrimary);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setBackground(createKeyDrawable(context, colorSurfaceVariant, colorBorder, 8));
            chip.setOnClickListener(v -> {
                buffer.setLength(0);
                buffer.append(val);
                firstNumericKeystroke[0] = false;
                updateDisplay.run();
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i < presets.length - 1) cp.rightMargin = dp(context, 6);
            quickNumRow.addView(chip, cp);
        }
        numericPadContainer.addView(quickNumRow, quickNumParams);

        String[][] numRows = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"C", "0", "⌫"}
        };

        for (String[] rowKeys : numRows) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 40));
            rp.bottomMargin = dp(context, 4);

            for (int i = 0; i < rowKeys.length; i++) {
                final String key = rowKeys[i];
                TextView btnKey = new TextView(context);
                btnKey.setText(key);
                btnKey.setGravity(Gravity.CENTER);
                btnKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                btnKey.setTypeface(null, Typeface.BOLD);

                boolean isAction = "C".equals(key) || "⌫".equals(key);
                btnKey.setTextColor(isAction ? colorPrimary : colorTextPrimary);
                btnKey.setBackground(createKeyDrawable(context,
                        isAction ? colorSurfaceVariant : colorCardBg, colorBorder, 8));

                btnKey.setOnClickListener(v -> {
                    if ("C".equals(key)) {
                        buffer.setLength(0);
                        firstNumericKeystroke[0] = false;
                    } else if ("⌫".equals(key)) {
                        if (buffer.length() > 0) {
                            buffer.deleteCharAt(buffer.length() - 1);
                        }
                        firstNumericKeystroke[0] = false;
                    } else {
                        if (firstNumericKeystroke[0]) {
                            buffer.setLength(0);
                            firstNumericKeystroke[0] = false;
                        }
                        if (buffer.length() < (numericOnlyField ? 4 : 200)) {
                            if (numericOnlyField && "0".equals(buffer.toString())) {
                                buffer.setLength(0);
                            }
                            buffer.append(key);
                        }
                    }
                    updateDisplay.run();
                });

                LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (i < rowKeys.length - 1) kp.rightMargin = dp(context, 6);
                row.addView(btnKey, kp);
            }
            numericPadContainer.addView(row, rp);
        }

        // =====================================================
        // CONSTRUIR TECLADO DE LETRAS (ABC - QWERTY ESPAÑOL)
        // =====================================================
        HorizontalScrollView phraseScroll = new HorizontalScrollView(context);
        phraseScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout phraseRow = new LinearLayout(context);
        phraseRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] quickPhrases = {
                "Trabajo fluido",
                "Meta cumplida",
                "Avance de campaña",
                "Revisión aprobada"
        };
        for (String phrase : quickPhrases) {
            TextView chip = new TextView(context);
            chip.setText("+ " + phrase);
            chip.setTextColor(colorPrimary);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));
            chip.setBackground(createKeyDrawable(context, colorSurfaceVariant, colorBorder, 8));
            chip.setOnClickListener(v -> {
                if (numericOnlyField) {
                    isNumericMode[0] = true;
                    refreshTabsVisual.run();
                    return;
                }
                if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) != ' ') {
                    buffer.append(". ");
                }
                buffer.append(phrase);
                updateDisplay.run();
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.rightMargin = dp(context, 6);
            phraseRow.addView(chip, cp);
        }
        phraseScroll.addView(phraseRow);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.bottomMargin = dp(context, 5);
        letterPadContainer.addView(phraseScroll, scrollParams);

        String[][] alphaRows = {
                {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
                {"A", "S", "D", "F", "G", "H", "J", "K", "L", "Ñ"},
                {"⇧", "Z", "X", "C", "V", "B", "N", "M", "⌫"}
        };

        final java.util.List<TextView> letterButtons = new java.util.ArrayList<>();

        for (String[] rowKeys : alphaRows) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 36));
            rp.bottomMargin = dp(context, 4);

            for (int i = 0; i < rowKeys.length; i++) {
                final String key = rowKeys[i];
                TextView btnKey = new TextView(context);
                btnKey.setGravity(Gravity.CENTER);
                btnKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                btnKey.setTypeface(null, Typeface.BOLD);

                boolean isSpecial = "⇧".equals(key) || "⌫".equals(key);
                btnKey.setText(key);
                btnKey.setTextColor(isSpecial ? colorPrimary : colorTextPrimary);
                btnKey.setBackground(createKeyDrawable(context,
                        isSpecial ? colorSurfaceVariant : colorCardBg, colorBorder, 8));

                if (!isSpecial) {
                    letterButtons.add(btnKey);
                }

                btnKey.setOnClickListener(v -> {
                    if ("⇧".equals(key)) {
                        isUppercase[0] = !isUppercase[0];
                        for (TextView lb : letterButtons) {
                            String t = lb.getText().toString();
                            lb.setText(isUppercase[0] ? t.toUpperCase() : t.toLowerCase());
                        }
                    } else if ("⌫".equals(key)) {
                        if (buffer.length() > 0) {
                            buffer.deleteCharAt(buffer.length() - 1);
                        }
                    } else {
                        if (!numericOnlyField && buffer.length() < 200) {
                            String ch = isUppercase[0] ? key.toUpperCase() : key.toLowerCase();
                            buffer.append(ch);
                            if (isUppercase[0] && buffer.length() == 1) {
                                isUppercase[0] = false;
                                for (TextView lb : letterButtons) {
                                    lb.setText(lb.getText().toString().toLowerCase());
                                }
                            }
                        }
                    }
                    updateDisplay.run();
                });

                float weight = isSpecial ? 1.4f : 1f;
                LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
                if (i < rowKeys.length - 1) kp.rightMargin = dp(context, 4);
                row.addView(btnKey, kp);
            }
            letterPadContainer.addView(row, rp);
        }

        // Fila inferior de barra espaciadora
        LinearLayout spaceRow = new LinearLayout(context);
        spaceRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams spaceRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 36));
        spaceRowParams.bottomMargin = dp(context, 4);

        TextView btnToNum = new TextView(context);
        btnToNum.setText("123");
        btnToNum.setGravity(Gravity.CENTER);
        btnToNum.setTextColor(colorPrimary);
        btnToNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnToNum.setTypeface(null, Typeface.BOLD);
        btnToNum.setBackground(createKeyDrawable(context, colorSurfaceVariant, colorBorder, 8));
        btnToNum.setOnClickListener(v -> {
            isNumericMode[0] = true;
            refreshTabsVisual.run();
        });

        TextView btnComma = new TextView(context);
        btnComma.setText(",");
        btnComma.setGravity(Gravity.CENTER);
        btnComma.setTextColor(colorTextPrimary);
        btnComma.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnComma.setTypeface(null, Typeface.BOLD);
        btnComma.setBackground(createKeyDrawable(context, colorCardBg, colorBorder, 8));
        btnComma.setOnClickListener(v -> {
            if (!numericOnlyField && buffer.length() < 200) {
                buffer.append(", ");
                updateDisplay.run();
            }
        });

        TextView btnSpace = new TextView(context);
        btnSpace.setText("ESPACIO");
        btnSpace.setGravity(Gravity.CENTER);
        btnSpace.setTextColor(colorTextPrimary);
        btnSpace.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnSpace.setTypeface(null, Typeface.BOLD);
        btnSpace.setBackground(createKeyDrawable(context, colorCardBg, colorBorder, 8));
        btnSpace.setOnClickListener(v -> {
            if (!numericOnlyField && buffer.length() < 200) {
                buffer.append(" ");
                updateDisplay.run();
            }
        });

        TextView btnDot = new TextView(context);
        btnDot.setText(".");
        btnDot.setGravity(Gravity.CENTER);
        btnDot.setTextColor(colorTextPrimary);
        btnDot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        btnDot.setTypeface(null, Typeface.BOLD);
        btnDot.setBackground(createKeyDrawable(context, colorCardBg, colorBorder, 8));
        btnDot.setOnClickListener(v -> {
            if (!numericOnlyField && buffer.length() < 200) {
                buffer.append(". ");
                updateDisplay.run();
            }
        });

        LinearLayout.LayoutParams p123 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.3f);
        p123.rightMargin = dp(context, 4);
        LinearLayout.LayoutParams pComma = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.9f);
        pComma.rightMargin = dp(context, 4);
        LinearLayout.LayoutParams pSpace = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3.8f);
        pSpace.rightMargin = dp(context, 4);
        LinearLayout.LayoutParams pDot = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.9f);

        spaceRow.addView(btnToNum, p123);
        spaceRow.addView(btnComma, pComma);
        spaceRow.addView(btnSpace, pSpace);
        spaceRow.addView(btnDot, pDot);
        letterPadContainer.addView(spaceRow, spaceRowParams);

        root.addView(numericPadContainer);
        root.addView(letterPadContainer);

        refreshTabsVisual.run();

        // Botón Confirmar / Aplicar
        TextView btnConfirm = new TextView(context);
        btnConfirm.setText("Confirmar y Aplicar");
        btnConfirm.setGravity(Gravity.CENTER);
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnConfirm.setTypeface(null, Typeface.BOLD);
        btnConfirm.setBackgroundResource(R.drawable.kpi_bg_gradient_btn);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 40));
        confirmParams.topMargin = dp(context, 5);
        btnConfirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirmed(buffer.toString());
            }
            dialog.dismiss();
        });
        root.addView(btnConfirm, confirmParams);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        dialog.show();
    }

    private static GradientDrawable createKeyDrawable(Context context, int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(context, radiusDp));
        gd.setStroke(dp(context, 1), strokeColor);
        return gd;
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }
}

