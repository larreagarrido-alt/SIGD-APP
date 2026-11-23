package pe.net.ws.kpiweb.features.kpi.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.data.KpiCollaboratorsRepository;
import pe.net.ws.kpiweb.features.kpi.data.KpiEditorStorageManager;
import pe.net.ws.kpiweb.features.kpi.data.KpiOfficialTemplateEngine;
import pe.net.ws.kpiweb.features.kpi.data.KpiTemplateSync;
import pe.net.ws.kpiweb.features.kpi.data.KpiScoreGoalConfig;
import pe.net.ws.kpiweb.features.kpi.model.KpiCollaboratorProfile;
import pe.net.ws.kpiweb.features.kpi.notification.KpiSoundHelper;
import pe.net.ws.kpiweb.features.kpi.ui.view.KpiZoomableSheetLayout;

/**
 * Editor Maestro Visual (WYSIWYG) de Plantillas de Reporte y Metas KPI (SIGD 2026).
 *
 * Muestra la hoja oficial del reporte de cada uno de los 13 cargos exactamente como es,
 * limpia de datos de usuario ("—"), y permite tocar literalmente cualquier elemento
 * de la hoja (encabezados, área, supervisor, actividades diarias de Lunes a Sábado,
 * metas diarias, metas semanales, criterios de evaluación, porcentajes de escala,
 * observaciones y el correo/redes al pie de página) para modificarlo en vivo.
 *
 * Al guardar, sincroniza los cambios automáticamente con el Reporte Oficial de la semana
 * actual y con el Formulario KPI que llena el colaborador (sin alterar semanas pasadas).
 */
public class KpiReportsEditorFragment extends Fragment {

    private static final String[] OFFICIAL_CARGOS = {
            "Administrador",
            "Asistente Administrativo",
            "Contador",
            "Community Manager",
            "Diseñador Gráfico Senior",
            "Diseñador Gráfico",
            "Asesor de Ventas",
            "Fotógrafo",
            "Trafficker Digital",
            "Videógrafo",
            "Gerente General",
            "Programador Junior",
            "Diseñador Gráfico Junior"
    };

    private Spinner spEditorRoleSelector;
    private TextView tvEditorCargoLabel;
    private TextView tvEditorModifiedCount;
    private TextView btnEditorToggleHighlight;
    private LinearLayout llEditorShiftToggle;
    private TextView btnEditorShiftMorning;
    private TextView btnEditorShiftAfternoon;
    private ScrollView svEditorSheetScroll;
    private KpiZoomableSheetLayout zoomEditorSheetLayout;
    private FrameLayout flEditorSheetContainer;
    private TextView btnEditorZoomOut;
    private TextView tvEditorZoomPercent;
    private TextView btnEditorZoomIn;
    private TextView btnEditorSaveChanges;
    private TextView btnEditorReset;

    private final List<String> availableCargos = new ArrayList<>();
    private String currentCargo = "Administrador";
    private String pendingLogoCargo = "Administrador";
    private int selectedShiftIndex = 0;
    private boolean highlightEditable = true;

    private final ActivityResultLauncher<String[]> logoImagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onLogoImageSelected);

    public static KpiReportsEditorFragment newInstance() {
        return new KpiReportsEditorFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.kpi_fragment_reports_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spEditorRoleSelector = view.findViewById(R.id.spEditorRoleSelector);
        tvEditorCargoLabel = view.findViewById(R.id.tvEditorCargoLabel);
        tvEditorModifiedCount = view.findViewById(R.id.tvEditorModifiedCount);
        btnEditorToggleHighlight = view.findViewById(R.id.btnEditorToggleHighlight);
        llEditorShiftToggle = view.findViewById(R.id.llEditorShiftToggle);
        btnEditorShiftMorning = view.findViewById(R.id.btnEditorShiftMorning);
        btnEditorShiftAfternoon = view.findViewById(R.id.btnEditorShiftAfternoon);
        svEditorSheetScroll = view.findViewById(R.id.svEditorSheetScroll);
        zoomEditorSheetLayout = view.findViewById(R.id.zoomEditorSheetLayout);
        flEditorSheetContainer = view.findViewById(R.id.flEditorSheetContainer);
        btnEditorZoomOut = view.findViewById(R.id.btnEditorZoomOut);
        tvEditorZoomPercent = view.findViewById(R.id.tvEditorZoomPercent);
        btnEditorZoomIn = view.findViewById(R.id.btnEditorZoomIn);
        btnEditorSaveChanges = view.findViewById(R.id.btnEditorSaveChanges);
        btnEditorReset = view.findViewById(R.id.btnEditorReset);

        setupZoomControls();

        View btnBack = view.findViewById(R.id.btnEditorBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else requireActivity().finish();
            });
        }

        setupShiftAndGuideButtons();

        if (btnEditorReset != null) {
            btnEditorReset.setOnClickListener(this::showHeaderMoreOptionsMenu);
        }

        if (btnEditorSaveChanges != null) {
            btnEditorSaveChanges.setOnClickListener(v -> saveAndSyncCurrentCargoTemplate());
        }

        KpiOfficialTemplateEngine.getInstance(requireContext()).setLogoPickRequestListener(cargo -> {
            pendingLogoCargo = cargo != null ? cargo : currentCargo;
            // Opción 3: Prioriza archivos PNG, permitiendo también JPG/WEBP para convertirlos automáticamente a PNG transparente
            logoImagePickerLauncher.launch(new String[]{"image/png", "image/webp", "image/jpeg", "image/*"});
        });

        // The server is authoritative. Keep the editor closed until the revision is known.
        if (btnEditorSaveChanges != null) btnEditorSaveChanges.setEnabled(false);
        if (btnEditorReset != null) btnEditorReset.setEnabled(false);
        if (flEditorSheetContainer != null) flEditorSheetContainer.setVisibility(View.INVISIBLE);
        if (tvEditorModifiedCount != null) tvEditorModifiedCount.setText("Cargando plantilla del servidor...");
        KpiTemplateSync.get(requireContext()).refresh((ok, message) -> {
            if (!isAdded() || getView() != view) return;
            if (!ok) {
                if (tvEditorModifiedCount != null) tvEditorModifiedCount.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                return;
            }
            setupCargosSpinner();
            updateShiftToggleUi();
            renderEditableOfficialSheet();
            if (flEditorSheetContainer != null) flEditorSheetContainer.setVisibility(View.VISIBLE);
            if (btnEditorSaveChanges != null) btnEditorSaveChanges.setEnabled(true);
            if (btnEditorReset != null) btnEditorReset.setEnabled(true);
        });
    }

    private void onLogoImageSelected(@Nullable Uri uri) {
        if (uri == null || getContext() == null) return;
        Context appContext = requireContext().getApplicationContext();
        final String targetCargo = pendingLogoCargo != null ? pendingLogoCargo : currentCargo;
        new Thread(() -> {
            String savedPath = copySelectedLogoToInternalStorage(uri, appContext);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (savedPath == null) {
                    Toast.makeText(requireContext(), "No se pudo procesar la imagen seleccionada", Toast.LENGTH_SHORT).show();
                    return;
                }
                showLogoScope(targetCargo, savedPath);
            });
        }, "kpi-logo-import").start();
    }

    private void showLogoScope(String targetCargo, String savedPath) {
        KpiOfficialTemplateEngine engine = KpiOfficialTemplateEngine.getInstance(requireContext());
        engine.showInstitutionalScopeDialog(
                requireContext(),
                targetCargo,
                "Logo Institucional en formato PNG",
                "Imagen convertida y optimizada a PNG de alta nitidez",
                applyToAll -> {
                    engine.saveInstitutionalLogoImage(targetCargo, savedPath, applyToAll);
                    renderEditableOfficialSheet();
                    Toast.makeText(
                            requireContext(),
                            applyToAll
                                    ? "✓ Logo PNG aplicado en TODOS los reportes KPI (13 cargos)"
                                    : "✓ Logo PNG aplicado solo en el reporte de " + targetCargo,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    @Nullable
    private String copySelectedLogoToInternalStorage(@NonNull Uri uri, @NonNull Context context) {
        try {
            File dir = new File(context.getFilesDir(), "kpi_logos");
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            InputStream in = context.getContentResolver().openInputStream(uri);
            if (in == null) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap decoded = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            if (decoded == null) return null;

            // Escalar proporcionalmente para que luzca nítido y ligero en el encabezado
            int maxW = 520;
            int maxH = 240;
            int w = decoded.getWidth();
            int h = decoded.getHeight();
            if (w > maxW || h > maxH) {
                float ratio = Math.min((float) maxW / w, (float) maxH / h);
                int nw = Math.max(1, Math.round(w * ratio));
                int nh = Math.max(1, Math.round(h * ratio));
                decoded = Bitmap.createScaledBitmap(decoded, nw, nh, true);
            }

            // Convertir siempre a Bitmap ARGB_8888 con soporte de transparencia PNG
            Bitmap pngReady = ensureTransparentPngBitmap(decoded);

            File outFile = new File(dir, "logo_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(outFile);
            pngReady.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Garantiza que cualquier imagen seleccionada quede en formato ARGB_8888 con setHasAlpha(true)
     * para que nunca se guarde con fondo negro y tome siempre el color real de la hoja.
     */
    @NonNull
    private Bitmap ensureTransparentPngBitmap(@NonNull Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.setHasAlpha(true);

        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);

        // 1. Si ya trae transparencia nativa real, aseguramos que los píxeles transparentes no tengan RGB negro
        boolean hasNativeTransparency = false;
        for (int i = 0; i < pixels.length; i++) {
            int a = Color.alpha(pixels[i]);
            if (a < 250) {
                hasNativeTransparency = true;
                if (a <= 8) {
                    pixels[i] = 0x00FFFFFF;
                }
            }
        }
        if (hasNativeTransparency) {
            out.setPixels(pixels, 0, width, 0, 0, width, height);
            return out;
        }

        // 2. Detectar si el fondo en las esquinas es blanco o negro (ej. PNG convertido por galería/WhatsApp)
        int c1 = pixels[0];
        int c2 = pixels[width - 1];
        int c3 = pixels[(height - 1) * width];
        int c4 = pixels[(height - 1) * width + (width - 1)];

        boolean whiteCorners = isNearWhite(c1) || isNearWhite(c2) || isNearWhite(c3) || isNearWhite(c4);
        boolean blackCorners = isNearBlack(c1) && isNearBlack(c2) && (isNearBlack(c3) || isNearBlack(c4));

        if (whiteCorners) {
            for (int i = 0; i < pixels.length; i++) {
                int px = pixels[i];
                int r = Color.red(px);
                int g = Color.green(px);
                int b = Color.blue(px);
                if (r >= 244 && g >= 244 && b >= 244) {
                    pixels[i] = 0x00FFFFFF;
                } else if (r >= 232 && g >= 232 && b >= 232) {
                    int avg = (r + g + b) / 3;
                    int newAlpha = Math.max(0, Math.min(255, (244 - avg) * 21));
                    pixels[i] = Color.argb(newAlpha, r, g, b);
                }
            }
        } else if (blackCorners) {
            for (int i = 0; i < pixels.length; i++) {
                int px = pixels[i];
                int r = Color.red(px);
                int g = Color.green(px);
                int b = Color.blue(px);
                if (r <= 14 && g <= 14 && b <= 14) {
                    pixels[i] = 0x00FFFFFF;
                } else if (r <= 28 && g <= 28 && b <= 28) {
                    int avg = (r + g + b) / 3;
                    int newAlpha = Math.max(0, Math.min(255, (avg - 14) * 18));
                    pixels[i] = Color.argb(newAlpha, r, g, b);
                }
            }
        }

        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private boolean isNearWhite(int color) {
        return Color.red(color) >= 240 && Color.green(color) >= 240 && Color.blue(color) >= 240;
    }

    private boolean isNearBlack(int color) {
        return Color.red(color) <= 16 && Color.green(color) <= 16 && Color.blue(color) <= 16;
    }

    private void setupCargosSpinner() {
        availableCargos.clear();
        availableCargos.addAll(Arrays.asList(OFFICIAL_CARGOS));

        // Agregar cargos personalizados creados desde "+ Nuevo KPI"
        List<String> customRoles = KpiOfficialTemplateEngine.getInstance(requireContext()).getCustomRoleNames();
        for (String cr : customRoles) {
            boolean exists = false;
            for (String c : availableCargos) {
                if (c.equalsIgnoreCase(cr.trim())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                availableCargos.add(cr.trim());
            }
        }

        // Agregar cargos personalizados que existan en el repositorio de colaboradores
        List<KpiCollaboratorProfile> profiles = KpiCollaboratorsRepository.getInstance(requireContext()).getAllCollaborators();
        for (KpiCollaboratorProfile p : profiles) {
            if (p.getRole() != null && !p.getRole().trim().isEmpty()) {
                boolean exists = false;
                for (String c : availableCargos) {
                    if (c.equalsIgnoreCase(p.getRole().trim())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    availableCargos.add(p.getRole().trim());
                }
            }
        }

        if (tvEditorCargoLabel != null) {
            int extraCount = Math.max(0, availableCargos.size() - OFFICIAL_CARGOS.length);
            tvEditorCargoLabel.setText(extraCount > 0
                    ? "CARGO A EDITAR (13 OFICIALES + " + extraCount + (extraCount == 1 ? " PERSONALIZADO)" : " PERSONALIZADOS)")
                    : "CARGO A EDITAR (13 CARGOS OFICIALES)");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                availableCargos
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEditorRoleSelector.setAdapter(adapter);

        int currentIdx = 0;
        for (int i = 0; i < availableCargos.size(); i++) {
            if (availableCargos.get(i).equalsIgnoreCase(currentCargo)) {
                currentIdx = i;
                break;
            }
        }
        spEditorRoleSelector.setSelection(currentIdx);

        spEditorRoleSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCargo = availableCargos.get(position);
                selectedShiftIndex = 0;
                if (zoomEditorSheetLayout != null) {
                    zoomEditorSheetLayout.resetZoom();
                }
                updateShiftToggleUi();
                renderEditableOfficialSheet();
                if (svEditorSheetScroll != null) {
                    svEditorSheetScroll.post(() -> svEditorSheetScroll.scrollTo(0, 0));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupZoomControls() {
        if (zoomEditorSheetLayout == null) return;
        zoomEditorSheetLayout.setOnZoomChangeListener((scale, percent) -> {
            if (tvEditorZoomPercent != null) {
                tvEditorZoomPercent.setText(percent + "%");
            }
        });
        if (btnEditorZoomIn != null) {
            btnEditorZoomIn.setOnClickListener(v -> zoomEditorSheetLayout.zoomIn());
        }
        if (btnEditorZoomOut != null) {
            btnEditorZoomOut.setOnClickListener(v -> zoomEditorSheetLayout.zoomOut());
        }
        if (tvEditorZoomPercent != null) {
            tvEditorZoomPercent.setOnClickListener(v -> {
                zoomEditorSheetLayout.resetZoom();
                Toast.makeText(requireContext(), "Zoom restablecido al 100%", Toast.LENGTH_SHORT).show();
            });
        }
        View fitPage = requireView().findViewById(R.id.btnEditorFitPage);
        if (fitPage != null) fitPage.setOnClickListener(v -> zoomEditorSheetLayout.fitToPage());
    }

    private void setupShiftAndGuideButtons() {
        if (btnEditorShiftMorning != null) {
            btnEditorShiftMorning.setOnClickListener(v -> {
                if (selectedShiftIndex != 0) {
                    selectedShiftIndex = 0;
                    updateShiftToggleUi();
                    renderEditableOfficialSheet();
                }
            });
        }
        if (btnEditorShiftAfternoon != null) {
            btnEditorShiftAfternoon.setOnClickListener(v -> {
                if (selectedShiftIndex != 1) {
                    selectedShiftIndex = 1;
                    updateShiftToggleUi();
                    renderEditableOfficialSheet();
                }
            });
        }
        if (btnEditorToggleHighlight != null) {
            btnEditorToggleHighlight.setOnClickListener(v -> {
                highlightEditable = !highlightEditable;
                btnEditorToggleHighlight.setText(highlightEditable ? "✎ Guías: ON" : "✎ Guías: OFF");
                btnEditorToggleHighlight.setTextColor(highlightEditable
                        ? ContextCompat.getColor(requireContext(), R.color.kpi_primary)
                        : Color.parseColor("#64748B"));
                renderEditableOfficialSheet();
            });
        }
    }

    private void updateShiftToggleUi() {
        KpiOfficialTemplateEngine engine = KpiOfficialTemplateEngine.getInstance(requireContext());
        boolean isDoubleShift = engine.isDoubleShiftJuniorCargo(currentCargo);
        if (llEditorShiftToggle != null) {
            llEditorShiftToggle.setVisibility(isDoubleShift ? View.VISIBLE : View.GONE);
        }
        if (isDoubleShift && btnEditorShiftMorning != null && btnEditorShiftAfternoon != null) {
            if (selectedShiftIndex == 0) {
                btnEditorShiftMorning.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                btnEditorShiftMorning.setTextColor(Color.WHITE);
                btnEditorShiftAfternoon.setBackgroundResource(android.R.color.transparent);
                btnEditorShiftAfternoon.setTextColor(Color.parseColor("#64748B"));
            } else {
                btnEditorShiftAfternoon.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
                btnEditorShiftAfternoon.setTextColor(Color.WHITE);
                btnEditorShiftMorning.setBackgroundResource(android.R.color.transparent);
                btnEditorShiftMorning.setTextColor(Color.parseColor("#64748B"));
            }
        }
    }

    private void renderEditableOfficialSheet() {
        if (flEditorSheetContainer == null || getContext() == null) return;
        flEditorSheetContainer.removeAllViews();

        KpiOfficialTemplateEngine engine = KpiOfficialTemplateEngine.getInstance(requireContext());
        View sheet = engine.inflateCleanTemplateForEditor(
                requireContext(), currentCargo, flEditorSheetContainer, selectedShiftIndex
        );

        engine.attachInteractiveEditor(
                requireContext(),
                currentCargo,
                sheet,
                highlightEditable,
                this::updateModifiedBadgeCount
        );

        flEditorSheetContainer.addView(sheet);
        updateModifiedBadgeCount();
    }

    private void updateModifiedBadgeCount() {
        if (tvEditorModifiedCount == null || getContext() == null) return;
        int count = KpiOfficialTemplateEngine.getInstance(requireContext())
                .getActiveChangesCount(currentCargo);
        if (count == 0) {
            tvEditorModifiedCount.setText("Plantilla Oficial");
            tvEditorModifiedCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.kpi_primary));
        } else {
            tvEditorModifiedCount.setText(count + (count == 1 ? " cambio activo" : " cambios activos"));
            tvEditorModifiedCount.setTextColor(Color.parseColor("#15803D"));
        }
    }

    private void saveAndSyncCurrentCargoTemplate() {
        if (btnEditorSaveChanges != null) btnEditorSaveChanges.setEnabled(false);
        KpiTemplateSync.get(requireContext()).save(currentCargo, (ok, message) -> {
            if (!isAdded()) return;
            if (btnEditorSaveChanges != null) btnEditorSaveChanges.setEnabled(true);
            if (ok) {
                KpiOfficialTemplateEngine.getInstance(requireContext())
                        .syncTemplateToRoleForms(requireContext(), currentCargo);
                KpiSoundHelper.playDailySuccess(requireContext());
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void showHeaderMoreOptionsMenu(View anchor) {
        LinearLayout menuCard = new LinearLayout(requireContext());
        menuCard.setOrientation(LinearLayout.VERTICAL);
        menuCard.setPadding(dp(6), dp(8), dp(6), dp(8));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(14));
        cardBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        menuCard.setBackground(cardBg);

        final PopupWindow popupWindow = new PopupWindow(
                menuCard,
                dp(215),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dp(10));

        // 1: Restaurar KPI
        menuCard.addView(createMenuOptionRow(
                R.drawable.kpi_ic_menu_restore,
                "Restaurar KPI",
                Color.parseColor("#1E293B"),
                v -> {
                    popupWindow.dismiss();
                    confirmResetTemplate();
                }
        ));

        menuCard.addView(createMenuDivider());

        menuCard.addView(createMenuOptionRow(
                R.drawable.kpi_ic_menu_new_kpi,
                "Metas del cálculo oficial",
                Color.parseColor("#4F46E5"),
                v -> { popupWindow.dismiss(); showOfficialGoalsDialog(); }
        ));

        menuCard.addView(createMenuDivider());

        // 2: Eliminar KPI (con ícono de basurita)
        menuCard.addView(createMenuOptionRow(
                R.drawable.kpi_ic_trash,
                "Eliminar KPI",
                Color.parseColor("#DC2626"),
                v -> {
                    popupWindow.dismiss();
                    handleDeleteCurrentKpi();
                }
        ));

        menuCard.addView(createMenuDivider());

        // 3: Nuevo KPI (sin el símbolo +, con su ícono propio)
        menuCard.addView(createMenuOptionRow(
                R.drawable.kpi_ic_menu_new_kpi,
                "Nuevo KPI",
                Color.parseColor("#4F46E5"),
                v -> {
                    popupWindow.dismiss();
                    showCreateNewKpiStep1Dialog();
                }
        ));

        popupWindow.showAsDropDown(anchor, -dp(165), dp(6));
    }

    private void showOfficialGoalsDialog() {
        int[] current = KpiScoreGoalConfig.read(requireContext(), currentCargo);
        if (current.length == 0) {
            Toast.makeText(requireContext(), "Este cargo aún no tiene una fórmula oficial", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = KpiScoreGoalConfig.labels(currentCargo);
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(8), dp(20), dp(8));
        scroll.addView(body);
        EditText[] fields = new EditText[current.length];
        for (int i = 0; i < current.length; i++) {
            TextView label = new TextView(requireContext());
            label.setText(labels[i]);
            label.setTextColor(Color.BLACK);
            body.addView(label);
            EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText(String.valueOf(current[i]));
            body.addView(input);
            fields[i] = input;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Metas numéricas de " + currentCargo)
                .setMessage("Estas cantidades determinan el porcentaje oficial de los reportes nuevos. Ajusta también los textos de la lámina si corresponde.")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar en servidor", (dialog, which) -> {
                    int[] next = new int[fields.length];
                    try {
                        for (int i = 0; i < fields.length; i++) {
                            next[i] = Integer.parseInt(fields[i].getText().toString().trim());
                            if (next[i] < 1 || next[i] > 100000) throw new NumberFormatException();
                        }
                    } catch (NumberFormatException error) {
                        Toast.makeText(requireContext(), "Cada meta debe estar entre 1 y 100000", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SharedPreferences prefs = KpiScoreGoalConfig.preferences(requireContext());
                    String key = KpiScoreGoalConfig.key(currentCargo);
                    String previous = prefs.getString(key, null);
                    prefs.edit().putString(key, KpiScoreGoalConfig.encode(next)).apply();
                    KpiTemplateSync.get(requireContext()).save(currentCargo, (ok, message) -> {
                        if (!ok) {
                            SharedPreferences.Editor restore = prefs.edit();
                            if (previous == null) restore.remove(key); else restore.putString(key, previous);
                            restore.apply();
                        }
                        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    });
                }).show();
    }

    private View createMenuOptionRow(int iconResId, String label, int textColor, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(12), dp(11), dp(14), dp(11));

        TypedValue outValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)) {
            row.setBackgroundResource(outValue.resourceId);
        }

        ImageView ivIcon = new ImageView(requireContext());
        ivIcon.setImageResource(iconResId);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(20), dp(20));
        iconLp.rightMargin = dp(12);
        ivIcon.setLayoutParams(iconLp);
        row.addView(ivIcon);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextColor(textColor);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tvLabel.setTypeface(null, Typeface.BOLD);
        row.addView(tvLabel);

        row.setOnClickListener(listener);
        return row;
    }

    private View createMenuDivider() {
        View div = new View(requireContext());
        div.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.leftMargin = dp(8);
        lp.rightMargin = dp(8);
        div.setLayoutParams(lp);
        return div;
    }

    private boolean isOfficialFactoryCargo(String cargoName) {
        if (cargoName == null) return false;
        for (String official : OFFICIAL_CARGOS) {
            if (official.equalsIgnoreCase(cargoName.trim())) {
                return true;
            }
        }
        return false;
    }

    private void handleDeleteCurrentKpi() {
        if (isOfficialFactoryCargo(currentCargo)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Cargo Oficial del Sistema")
                    .setMessage("El cargo \"" + currentCargo + "\" es uno de los 13 cargos oficiales de fábrica y no se puede borrar para no desconfigurar la empresa.\n\nRecuerda que puedes usar \"Restaurar KPI\" si deseas regresarlo a su estado original limpio.")
                    .setPositiveButton("Restaurar KPI", (d, w) -> confirmResetTemplate())
                    .setNegativeButton("Entendido", null)
                    .show();
            return;
        }

        final String cargoToDelete = currentCargo;
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar KPI")
                .setMessage("¿Estás seguro de que deseas eliminar permanentemente el KPI \"" + cargoToDelete + "\" del editor y del panel general de KPIs?")
                .setPositiveButton("Sí, eliminar", (d, w) -> {
                    KpiOfficialTemplateEngine engine = KpiOfficialTemplateEngine.getInstance(requireContext());
                    engine.deleteCustomKpiRole(cargoToDelete);

                    currentCargo = OFFICIAL_CARGOS[0];
                    selectedShiftIndex = 0;
                    setupCargosSpinner();
                    updateShiftToggleUi();
                    renderEditableOfficialSheet();

                    Toast.makeText(
                            requireContext(),
                            "KPI \"" + cargoToDelete + "\" eliminado del sistema y del panel general",
                            Toast.LENGTH_LONG
                    ).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Paso 1 de "+ Nuevo KPI": Pregunta el nombre del KPI (para qué rol/cargo).
     */
    private void showCreateNewKpiStep1Dialog() {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, dp(18), pad, dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        root.setBackground(bg);

        TextView tvStep = new TextView(requireContext());
        tvStep.setText("PASO 1 DE 2 • CREAR NUEVO KPI");
        tvStep.setTextColor(Color.parseColor("#4F46E5"));
        tvStep.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvStep.setTypeface(null, Typeface.BOLD);
        root.addView(tvStep);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("¿Cuál es el nombre del KPI / Rol?");
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(4);
        root.addView(tvTitle, tLp);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText("Ingresa el nombre del cargo para el que se generará la nueva hoja de reporte KPI.");
        tvSub.setTextColor(Color.parseColor("#64748B"));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(4);
        sLp.bottomMargin = dp(12);
        root.addView(tvSub, sLp);

        final EditText etRoleName = new EditText(requireContext());
        etRoleName.setHint("Ej: Copywriter, Jefe de Marketing...");
        etRoleName.setTextColor(Color.parseColor("#0F172A"));
        etRoleName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        etRoleName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        int etPad = dp(12);
        etRoleName.setPadding(etPad, etPad, etPad, etPad);
        GradientDrawable etBg = new GradientDrawable();
        etBg.setColor(Color.parseColor("#F8FAFC"));
        etBg.setStroke(dp(1), Color.parseColor("#6366F1"));
        etBg.setCornerRadius(dp(10));
        etRoleName.setBackground(etBg);
        root.addView(etRoleName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout actionsRow = new LinearLayout(requireContext());
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(14);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .create();

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("Cancelar");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setPadding(dp(14), dp(10), dp(14), dp(10));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        actionsRow.addView(btnCancel);

        TextView btnNext = new TextView(requireContext());
        btnNext.setText("Siguiente");
        btnNext.setTextColor(Color.WHITE);
        btnNext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnNext.setTypeface(null, Typeface.BOLD);
        btnNext.setPadding(dp(18), dp(10), dp(18), dp(10));
        btnNext.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnNext.setOnClickListener(v -> {
            String roleName = etRoleName.getText().toString().trim();
            if (roleName.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor ingresa el nombre del cargo", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            showCreateNewKpiStep2Dialog(roleName);
        });
        actionsRow.addView(btnNext);
        root.addView(actionsRow, actLp);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    /**
     * Paso 2 de "+ Nuevo KPI": Pregunta si el rol es Jefe o Colaborador y genera la plantilla
     * con la estructura correspondiente (Jefe = Diseñador Gráfico Senior | Colaborador = Fotógrafo).
     */
    private void showCreateNewKpiStep2Dialog(final String roleName) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, dp(18), pad, dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        root.setBackground(bg);

        TextView tvStep = new TextView(requireContext());
        tvStep.setText("PASO 2 DE 2 • RANGO DEL CARGO");
        tvStep.setTextColor(Color.parseColor("#4F46E5"));
        tvStep.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        tvStep.setTypeface(null, Typeface.BOLD);
        root.addView(tvStep);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("¿Es Jefe o Colaborador?");
        tvTitle.setTextColor(Color.parseColor("#0F172A"));
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.topMargin = dp(4);
        root.addView(tvTitle, tLp);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText("Selecciona el rango para \"" + roleName + "\". La estructura de la hoja se creará según tu elección:");
        tvSub.setTextColor(Color.parseColor("#475569"));
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.topMargin = dp(6);
        sLp.bottomMargin = dp(14);
        root.addView(tvSub, sLp);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .create();

        // Opción 1: JEFE (estructura tipo Diseñador Gráfico Senior)
        TextView btnJefe = new TextView(requireContext());
        btnJefe.setText("Jefe (Estructura tipo Diseñador Gráfico Senior)");
        btnJefe.setTextColor(Color.WHITE);
        btnJefe.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnJefe.setTypeface(null, Typeface.BOLD);
        btnJefe.setGravity(Gravity.CENTER);
        btnJefe.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnJefe.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnJefe.setOnClickListener(v -> {
            dialog.dismiss();
            finalizeCreateCustomKpi(roleName, true);
        });
        root.addView(btnJefe, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Opción 2: COLABORADOR (estructura tipo Fotógrafo)
        TextView btnCollab = new TextView(requireContext());
        btnCollab.setText("Colaborador (Estructura tipo Fotógrafo)");
        btnCollab.setTextColor(Color.parseColor("#1E1B4B"));
        btnCollab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btnCollab.setTypeface(null, Typeface.BOLD);
        btnCollab.setGravity(Gravity.CENTER);
        btnCollab.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable colBg = new GradientDrawable();
        colBg.setColor(Color.parseColor("#EEF2FF"));
        colBg.setStroke(dp(1), Color.parseColor("#6366F1"));
        colBg.setCornerRadius(dp(10));
        btnCollab.setBackground(colBg);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colLp.topMargin = dp(8);
        btnCollab.setOnClickListener(v -> {
            dialog.dismiss();
            finalizeCreateCustomKpi(roleName, false);
        });
        root.addView(btnCollab, colLp);

        TextView btnCancel = new TextView(requireContext());
        btnCancel.setText("Cancelar");
        btnCancel.setTextColor(Color.parseColor("#64748B"));
        btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(dp(10), dp(10), dp(10), dp(6));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = dp(6);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnCancel, cLp);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void finalizeCreateCustomKpi(String roleName, boolean isJefe) {
        KpiOfficialTemplateEngine engine = KpiOfficialTemplateEngine.getInstance(requireContext());
        engine.registerCustomKpiRole(roleName, isJefe);
        engine.resetCargoTemplate(roleName);
        pe.net.ws.kpiweb.features.schedule.data.ScheduleRepository.getInstance(requireContext())
                .createBlank(roleName);

        setupCargosSpinner();
        int idx = -1;
        for (int i = 0; i < availableCargos.size(); i++) {
            if (availableCargos.get(i).equalsIgnoreCase(roleName.trim())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            currentCargo = availableCargos.get(idx);
            spEditorRoleSelector.setSelection(idx);
            renderEditableOfficialSheet();
        }
        Toast.makeText(
                requireContext(),
                "Nuevo KPI \"" + roleName + "\" (" + (isJefe ? "Jefe" : "Colaborador") + ") creado y listo para configurar",
                Toast.LENGTH_LONG
        ).show();
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, requireContext().getResources().getDisplayMetrics()));
    }

    private void confirmResetTemplate() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Restaurar Plantilla Original")
                .setMessage("¿Deseas restaurar todos los textos, metas, porcentajes y pie de página de \"" + currentCargo + "\" a sus valores originales de fábrica?")
                .setPositiveButton("Sí, restaurar", (d, w) -> {
                    KpiOfficialTemplateEngine engine = KpiOfficialTemplateEngine.getInstance(requireContext());
                    engine.resetCargoTemplate(currentCargo);
                    renderEditableOfficialSheet();
                    Toast.makeText(requireContext(), "Plantilla restaurada a valores originales", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
