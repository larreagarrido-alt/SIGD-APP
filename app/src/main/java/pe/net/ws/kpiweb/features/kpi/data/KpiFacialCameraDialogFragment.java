package pe.net.ws.kpiweb.features.kpi.data;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.features.kpi.ui.KpiThemeManager;

/**
 * Diálogo de validación biométrica Facial con activación real de la cámara
 * frontal (Camera2 + TextureView).
 * Se enciende en vivo cuando el usuario elige la opción "Facial" al guardar su
 * KPI diario.
 */
public class KpiFacialCameraDialogFragment extends DialogFragment {

    private static final String ARG_DAY_TITLE = "arg_day_title";
    private static final String ARG_IS_REMEMBERED = "arg_is_remembered";

    public interface FacialResultListener {
        void onVerified();

        void onSwitchToFingerprint();

        void onCancelled();

        void onCameraUnavailable(String message);
    }

    private FacialResultListener resultListener;
    private String dayTitle = "Guardar KPI Diario";
    private boolean isRemembered = false;

    // UI
    private FrameLayout cameraRingContainer;
    private GradientDrawable ringBorderDrawable;
    private TextureView textureView;
    private LinearLayout llPermissionOverlay;
    private TextView tvStatusBadge;
    private ProgressBar pbFaceScan;
    private TextView btnValidateNow;

    // Camera2
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Surface activePreviewSurface;
    private Size previewSize = new Size(640, 480);
    private int sensorOrientation = 270;
    private int supportedFaceDetectMode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;

    private boolean hasRequestedPermissionOnce = false;
    private boolean cameraStreamActive = false;
    private int frameCounter = 0;
    private int scanProgress = 0;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isAdded() || finished.get())
                    return;
                if (isGranted) {
                    showCameraModeUi();
                    startBackgroundThread();
                    if (textureView != null && textureView.isAvailable()) {
                        openFrontCamera(textureView.getWidth(), textureView.getHeight());
                    }
                } else {
                    showPermissionDeniedUi();
                }
            });

    public static KpiFacialCameraDialogFragment newInstance(String dayTitle, boolean isRemembered) {
        KpiFacialCameraDialogFragment fragment = new KpiFacialCameraDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY_TITLE, dayTitle);
        args.putBoolean(ARG_IS_REMEMBERED, isRemembered);
        fragment.setArguments(args);
        return fragment;
    }

    public void setFacialResultListener(FacialResultListener listener) {
        this.resultListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            dayTitle = getArguments().getString(ARG_DAY_TITLE, dayTitle);
            isRemembered = getArguments().getBoolean(ARG_IS_REMEMBERED, false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        boolean isDark = KpiThemeManager.isDarkMode(context);

        int primaryColor = ContextCompat.getColor(context, R.color.kpi_primary);
        int textPrimary = ContextCompat.getColor(context, R.color.kpi_text_primary);
        int textSecondary = ContextCompat.getColor(context, R.color.kpi_text_secondary);
        int borderNeutral = ContextCompat.getColor(context, R.color.kpi_border);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(context, 20);
        root.setPadding(pad, pad, pad, pad);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setCornerRadius(dp(context, 24));
        cardBg.setColor(isDark ? Color.parseColor("#151E36") : Color.WHITE);
        cardBg.setStroke(dp(context, 1), borderNeutral);
        root.setBackground(cardBg);

        // Título superior
        TextView tvTitle = new TextView(context);
        tvTitle.setText("Validación Biométrica Facial");
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setTextColor(textPrimary);
        tvTitle.setGravity(Gravity.CENTER);
        root.addView(tvTitle);

        TextView tvSubtitle = new TextView(context);
        tvSubtitle.setText(dayTitle);
        tvSubtitle.setTextSize(12f);
        tvSubtitle.setTextColor(textSecondary);
        tvSubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(context, 3);
        subLp.bottomMargin = dp(context, 14);
        tvSubtitle.setLayoutParams(subLp);
        root.addView(tvSubtitle);

        // Anillo circular de cámara frontal en vivo (236dp)
        int ringSize = dp(context, 236);
        int camSize = dp(context, 222);

        cameraRingContainer = new FrameLayout(context);
        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(ringSize, ringSize);
        cameraRingContainer.setLayoutParams(ringLp);

        ringBorderDrawable = new GradientDrawable();
        ringBorderDrawable.setShape(GradientDrawable.OVAL);
        ringBorderDrawable.setColor(isDark ? Color.parseColor("#0B1120") : Color.parseColor("#FAF5FF"));
        ringBorderDrawable.setStroke(dp(context, 4), primaryColor);
        cameraRingContainer.setBackground(ringBorderDrawable);

        textureView = new TextureView(context);
        FrameLayout.LayoutParams texLp = new FrameLayout.LayoutParams(camSize, camSize, Gravity.CENTER);
        textureView.setLayoutParams(texLp);
        textureView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        textureView.setClipToOutline(true);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        cameraRingContainer.addView(textureView);

        // Overlay en caso falte permiso de cámara
        llPermissionOverlay = new LinearLayout(context);
        llPermissionOverlay.setOrientation(LinearLayout.VERTICAL);
        llPermissionOverlay.setGravity(Gravity.CENTER);
        llPermissionOverlay.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        llPermissionOverlay.setVisibility(View.GONE);
        FrameLayout.LayoutParams permLp = new FrameLayout.LayoutParams(camSize, camSize, Gravity.CENTER);
        llPermissionOverlay.setLayoutParams(permLp);

        ImageView ivPermIcon = new ImageView(context);
        ivPermIcon.setImageResource(R.drawable.kpi_ic_camera_pro);
        ivPermIcon.setColorFilter(primaryColor);
        LinearLayout.LayoutParams ivPermLp = new LinearLayout.LayoutParams(dp(context, 36), dp(context, 36));
        ivPermIcon.setLayoutParams(ivPermLp);
        llPermissionOverlay.addView(ivPermIcon);

        TextView tvPermMsg = new TextView(context);
        tvPermMsg.setText("Permite el uso de la cámara frontal para validar tu rostro");
        tvPermMsg.setTextSize(12f);
        tvPermMsg.setTextColor(textPrimary);
        tvPermMsg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pmLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pmLp.topMargin = dp(context, 6);
        pmLp.bottomMargin = dp(context, 10);
        tvPermMsg.setLayoutParams(pmLp);
        llPermissionOverlay.addView(tvPermMsg);

        TextView btnGrantCam = new TextView(context);
        btnGrantCam.setText("Activar Cámara");
        btnGrantCam.setTextSize(12f);
        btnGrantCam.setTypeface(Typeface.DEFAULT_BOLD);
        btnGrantCam.setTextColor(Color.WHITE);
        btnGrantCam.setGravity(Gravity.CENTER);
        btnGrantCam.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8));
        btnGrantCam.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnGrantCam.setOnClickListener(v -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        llPermissionOverlay.addView(btnGrantCam);

        cameraRingContainer.addView(llPermissionOverlay);
        root.addView(cameraRingContainer);

        // Estado en vivo del escaneo
        tvStatusBadge = new TextView(context);
        tvStatusBadge.setText("Encendiendo cámara frontal...");
        tvStatusBadge.setTextSize(12.5f);
        tvStatusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvStatusBadge.setTextColor(primaryColor);
        tvStatusBadge.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(context, 14);
        tvStatusBadge.setLayoutParams(stLp);
        root.addView(tvStatusBadge);

        // Barra horizontal de progreso de escaneo facial
        pbFaceScan = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        pbFaceScan.setMax(100);
        pbFaceScan.setProgress(0);
        pbFaceScan.setProgressTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        pbFaceScan.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(borderNeutral));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 8));
        pbLp.topMargin = dp(context, 8);
        pbFaceScan.setLayoutParams(pbLp);
        root.addView(pbFaceScan);

        // Botón principal: Capturar y Validar Rostro ahora
        btnValidateNow = new TextView(context);
        btnValidateNow.setText("Capturar y Validar Rostro");
        btnValidateNow.setGravity(Gravity.CENTER);
        btnValidateNow.setTextSize(13.5f);
        btnValidateNow.setTypeface(Typeface.DEFAULT_BOLD);
        btnValidateNow.setTextColor(Color.WHITE);
        btnValidateNow.setBackgroundResource(R.drawable.kpi_bg_btn_elegant);
        btnValidateNow.setAlpha(0.5f);
        btnValidateNow.setEnabled(false);
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48));
        valLp.topMargin = dp(context, 14);
        btnValidateNow.setLayoutParams(valLp);
        btnValidateNow.setOnClickListener(v -> {
            if (cameraStreamActive) {
                completeFacialVerification();
            }
        });
        root.addView(btnValidateNow);

        // Fila inferior: Cambiar a Huella | Cancelar
        LinearLayout bottomRow = new LinearLayout(context);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams botLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        botLp.topMargin = dp(context, 10);
        bottomRow.setLayoutParams(botLp);

        TextView btnSwitchFingerprint = new TextView(context);
        btnSwitchFingerprint.setText(isRemembered ? "Cambiar método" : "Usar Huella");
        btnSwitchFingerprint.setGravity(Gravity.CENTER);
        btnSwitchFingerprint.setTextSize(12f);
        btnSwitchFingerprint.setTypeface(Typeface.DEFAULT_BOLD);
        btnSwitchFingerprint.setTextColor(primaryColor);
        btnSwitchFingerprint.setBackgroundResource(R.drawable.kpi_bg_goal_card_selected);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(0, dp(context, 42), 1.15f);
        swLp.rightMargin = dp(context, 8);
        btnSwitchFingerprint.setLayoutParams(swLp);
        btnSwitchFingerprint.setOnClickListener(v -> {
            if (finished.compareAndSet(false, true)) {
                closeCameraAndStopThread();
                dismissAllowingStateLoss();
                if (resultListener != null) {
                    resultListener.onSwitchToFingerprint();
                }
            }
        });
        bottomRow.addView(btnSwitchFingerprint);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("Cancelar");
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextSize(12f);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setTextColor(textSecondary);
        btnCancel.setBackgroundResource(R.drawable.kpi_designer_bg_day_unselected);
        LinearLayout.LayoutParams canLp = new LinearLayout.LayoutParams(0, dp(context, 42), 0.85f);
        btnCancel.setLayoutParams(canLp);
        btnCancel.setOnClickListener(v -> {
            if (finished.compareAndSet(false, true)) {
                closeCameraAndStopThread();
                dismissAllowingStateLoss();
                if (resultListener != null) {
                    resultListener.onCancelled();
                }
            }
        });
        bottomRow.addView(btnCancel);

        root.addView(bottomRow);
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (finished.get())
            return;

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCameraModeUi();
            startBackgroundThread();
            if (textureView != null && textureView.isAvailable()) {
                openFrontCamera(textureView.getWidth(), textureView.getHeight());
            }
        } else {
            if (!hasRequestedPermissionOnce) {
                hasRequestedPermissionOnce = true;
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                showPermissionDeniedUi();
            }
        }
    }

    @Override
    public void onPause() {
        closeCameraAndStopThread();
        super.onPause();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        closeCameraAndStopThread();
        super.onDismiss(dialog);
        if (finished.compareAndSet(false, true) && resultListener != null) {
            resultListener.onCancelled();
        }
    }

    private void showCameraModeUi() {
        if (llPermissionOverlay != null)
            llPermissionOverlay.setVisibility(View.GONE);
        if (textureView != null)
            textureView.setVisibility(View.VISIBLE);
    }

    private void showPermissionDeniedUi() {
        if (textureView != null)
            textureView.setVisibility(View.INVISIBLE);
        if (llPermissionOverlay != null)
            llPermissionOverlay.setVisibility(View.VISIBLE);
        if (tvStatusBadge != null) {
            tvStatusBadge.setText("Concede permiso de cámara o cambia a Huella");
        }
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            if (isAdded() && !finished.get()
                    && ContextCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openFrontCamera(width, height);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            configureCenterCropTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // Stream de video en vivo activo
        }
    };

    @SuppressLint("MissingPermission")
    private void openFrontCamera(int viewWidth, int viewHeight) {
        if (!isAdded() || finished.get() || cameraDevice != null)
            return;
        Context context = getContext();
        if (context == null)
            return;

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            notifyCameraUnavailable("No se pudo acceder al servicio de cámara del dispositivo.");
            return;
        }

        try {
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length == 0) {
                notifyCameraUnavailable("Este dispositivo no cuenta con cámara disponible.");
                return;
            }

            String targetCameraId = cameraIds[0];
            for (String id : cameraIds) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    targetCameraId = id;
                    break;
                }
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(targetCameraId);
            Integer sensorRot = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorOrientation = (sensorRot != null) ? sensorRot : 270;

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                if (sizes != null && sizes.length > 0) {
                    previewSize = chooseOptimalPreviewSize(sizes);
                }
            }

            supportedFaceDetectMode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;
            int[] faceModes = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
            if (faceModes != null) {
                for (int mode : faceModes) {
                    if (mode == CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL) {
                        supportedFaceDetectMode = mode;
                        break;
                    } else if (mode == CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                        supportedFaceDetectMode = mode;
                    }
                }
            }

            mainHandler.post(() -> configureCenterCropTransform(viewWidth, viewHeight));

            if (backgroundHandler == null) {
                startBackgroundThread();
            }
            manager.openCamera(targetCameraId, cameraStateCallback, backgroundHandler);

        } catch (CameraAccessException | SecurityException | IllegalArgumentException e) {
            notifyCameraUnavailable("No fue posible encender la cámara frontal: " + e.getMessage());
        }
    }

    private Size chooseOptimalPreviewSize(Size[] choices) {
        Size best = choices[0];
        int bestDiff = Integer.MAX_VALUE;
        for (Size option : choices) {
            int w = option.getWidth();
            int h = option.getHeight();
            if (w <= 1280 && h <= 1280 && w >= 480 && h >= 480) {
                int diff = Math.abs(w - 960) + Math.abs(h - 720);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = option;
                }
            }
        }
        return best;
    }

    private void configureCenterCropTransform(int viewWidth, int viewHeight) {
        if (textureView == null || previewSize == null || viewWidth <= 0 || viewHeight <= 0 || !isAdded()) {
            return;
        }
        int displayRotation = Surface.ROTATION_0;
        if (getActivity() != null && getActivity().getWindowManager() != null) {
            displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        }
        int displayDegrees = 0;
        if (displayRotation == Surface.ROTATION_90)
            displayDegrees = 90;
        else if (displayRotation == Surface.ROTATION_180)
            displayDegrees = 180;
        else if (displayRotation == Surface.ROTATION_270)
            displayDegrees = 270;

        int totalRotation = (sensorOrientation - displayDegrees + 360) % 360;
        boolean swapped = (totalRotation == 90 || totalRotation == 270);

        int effectiveW = swapped ? previewSize.getHeight() : previewSize.getWidth();
        int effectiveH = swapped ? previewSize.getWidth() : previewSize.getHeight();
        if (effectiveW <= 0 || effectiveH <= 0)
            return;

        float defaultScaleX = (float) viewWidth / effectiveW;
        float defaultScaleY = (float) viewHeight / effectiveH;
        float maxScale = Math.max(defaultScaleX, defaultScaleY);

        Matrix matrix = new Matrix();
        matrix.setScale(
                maxScale / defaultScaleX,
                maxScale / defaultScaleY,
                viewWidth / 2f,
                viewHeight / 2f);
        textureView.setTransform(matrix);
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            if (finished.get() || !isAdded()) {
                camera.close();
                return;
            }
            cameraDevice = camera;
            startCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            notifyCameraUnavailable("La cámara frontal está siendo ocupada por otra aplicación.");
        }
    };

    private void startCameraPreviewSession() {
        if (cameraDevice == null || textureView == null || !textureView.isAvailable() || finished.get()) {
            return;
        }
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            if (surfaceTexture == null)
                return;

            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            if (activePreviewSurface != null) {
                try {
                    activePreviewSurface.release();
                } catch (Exception ignored) {
                }
            }
            activePreviewSurface = new Surface(surfaceTexture);

            final CaptureRequest.Builder previewRequestBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(activePreviewSurface);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);

            if (supportedFaceDetectMode != CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF) {
                previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, supportedFaceDetectMode);
            }

            cameraDevice.createCaptureSession(
                    Collections.singletonList(activePreviewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null || finished.get()) {
                                session.close();
                                return;
                            }
                            captureSession = session;
                            try {
                                captureSession.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        captureCallback,
                                        backgroundHandler);
                                mainHandler.post(() -> {
                                    if (!isAdded() || finished.get())
                                        return;
                                    cameraStreamActive = true;
                                    btnValidateNow.setEnabled(true);
                                    btnValidateNow.setAlpha(1.0f);
                                    tvStatusBadge.setText("Mira a la cámara dentro del círculo...");
                                });
                            } catch (CameraAccessException | IllegalStateException ignored) {
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            notifyCameraUnavailable("No se pudo iniciar la vista previa de la cámara frontal.");
                        }
                    },
                    backgroundHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            notifyCameraUnavailable("Error iniciando cámara frontal: " + e.getMessage());
        }
    }

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request,
                @NonNull TotalCaptureResult result) {
            if (finished.get())
                return;

            frameCounter++;
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            final boolean hardwareFaceDetected = (faces != null && faces.length > 0);

            if (hardwareFaceDetected) {
                scanProgress = Math.min(100, scanProgress + 4);
            } else {
                scanProgress = Math.max(0, scanProgress - 2);
            }

            final int currentProgress = scanProgress;
            mainHandler.post(() -> {
                if (!isAdded() || finished.get())
                    return;
                pbFaceScan.setProgress(currentProgress);

                if (hardwareFaceDetected) {
                    ringBorderDrawable.setStroke(dp(requireContext(), 4), Color.parseColor("#10B981"));
                    tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
                    tvStatusBadge.setText("Rostro detectado · Escaneando (" + currentProgress + "%)");
                } else {
                    int primaryColor = ContextCompat.getColor(requireContext(), R.color.kpi_primary);
                    ringBorderDrawable.setStroke(dp(requireContext(), 4), primaryColor);
                    tvStatusBadge.setTextColor(primaryColor);
                    if (supportedFaceDetectMode == CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF) {
                        tvStatusBadge.setText("Cámara activa · Pulsa 'Capturar y Validar Rostro'");
                    } else {
                        tvStatusBadge.setText("Ubica tu rostro en el círculo o pulsa Capturar");
                    }
                }

                if (currentProgress >= 100) {
                    completeFacialVerification();
                }
            });
        }
    };

    private void completeFacialVerification() {
        if (!finished.compareAndSet(false, true))
            return;

        if (isAdded()) {
            pbFaceScan.setProgress(100);
            ringBorderDrawable.setStroke(dp(requireContext(), 5), Color.parseColor("#10B981"));
            tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
            tvStatusBadge.setText("✓ Rostro verificado correctamente");
            btnValidateNow.setEnabled(false);
        }

        // Apagar inmediatamente el hardware de cámara para apagar el punto verde de
        // Android
        closeCameraAndStopThread();

        mainHandler.postDelayed(() -> {
            dismissAllowingStateLoss();
            if (resultListener != null) {
                resultListener.onVerified();
            }
        }, 260);
    }

    private void notifyCameraUnavailable(String message) {
        mainHandler.post(() -> {
            if (finished.compareAndSet(false, true)) {
                closeCameraAndStopThread();
                dismissAllowingStateLoss();
                if (resultListener != null) {
                    resultListener.onCameraUnavailable(message);
                }
            }
        });
    }

    private void startBackgroundThread() {
        if (backgroundThread != null)
            return;
        backgroundThread = new HandlerThread("KpiFacialCamThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void closeCameraAndStopThread() {
        cameraStreamActive = false;
        if (captureSession != null) {
            try {
                captureSession.stopRepeating();
            } catch (Exception ignored) {
            }
            try {
                captureSession.close();
            } catch (Exception ignored) {
            }
            captureSession = null;
        }
        if (cameraDevice != null) {
            try {
                cameraDevice.close();
            } catch (Exception ignored) {
            }
            cameraDevice = null;
        }
        if (activePreviewSurface != null) {
            try {
                activePreviewSurface.release();
            } catch (Exception ignored) {
            }
            activePreviewSurface = null;
        }
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join(150);
            } catch (InterruptedException ignored) {
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
