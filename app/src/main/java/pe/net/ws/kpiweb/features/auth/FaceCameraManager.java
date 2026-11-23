package pe.net.ws.kpiweb.features.auth;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Native Camera2 hardware manager for enterprise facial verification.
 * Provides real front-facing camera preview, hardware facial detection,
 * framing/illumination quality checks, and clean lifecycle management.
 */
public class FaceCameraManager implements FaceAuthCoordinator.CameraBridge {

    public interface QualityListener {
        void onFaceCountChanged(int faceCount);
        void onIlluminationChanged(boolean adequate);
        default void onActionChanged(String action) {}
        default void onPreviewReady() {}
        default void onExtractorReady() {}
        default void onLandmarks(float[] normalizedPoints) {}
        default void onEnrollmentProgress(int samples) {}
        default void onPadResult(double real,double spoof,double millis) {}
    }
    public interface EnrollmentCallback {
        void onSamples(String payload);
        void onError(FaceAuthCoordinator.CaptureResult.Status status);
    }
    public interface LoginCallback {
        void onDescriptor(String descriptor);
        void onError(FaceAuthCoordinator.CaptureResult.Status status);
    }

    private final Context context;
    private final TextureView textureView;
    private final QualityListener qualityListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Surface previewSurface;
    private CaptureRequest.Builder previewRequestBuilder;
    private Size previewSize;
    private int sensorOrientation;
    private int configuredDisplayRotation=-1;
    private long firstPreviewFrameAt;
    private volatile boolean exposureConverged;
    private long cameraOpenStarted;
    private long frameAnalysisStarted;
    private long loginAttemptStartedNanos;
    private long loginCaptureStartedNanos;
    private boolean firstValidImageLogged,firstFaceLogged,faceModelsReadyLogged,padPassedLogged;
    private int loggedSamples;
    private int previewFrames;

    private LocalFaceExtractor extractor;
    private FaceMotionGate motion;
    private String[] challengeActions;
    private long captureStarted;
    private boolean extractorReadyReported;
    private volatile int extractedDimension;
    private volatile FaceAuthCoordinator.CaptureResult.Status captureFailure;
    private volatile boolean captureCompleted;
    private double lastEyeRatio,lastYaw,lastInferenceMillis;
    private double minimumEyeRatio=Double.POSITIVE_INFINITY;
    private FaceAuthCoordinator.CaptureCallback activeCallback;
    private EnrollmentCallback enrollmentCallback;
    private LoginCallback loginCallback;
    private FaceEnrollmentSamples enrollmentSamples;
    private LoginFaceSamples loginSamples;
    private LocalPadAnalyzer padAnalyzer;
    private PadTemporalGate padGate;
    private int captureGeneration;
    private Bitmap pendingPadFrame;
    /** Enables actual offline model screening only for the facial login capture. */
    public void enableLocalPad(){enableLocalPad(null);}
    /** Prepared models may be transferred from the login screen; no prior PAD result is reused. */
    public void enableLocalPad(@Nullable LocalPadAnalyzer prepared){
        if(padAnalyzer==null)padAnalyzer=prepared!=null?prepared:new LocalPadAnalyzer(context);
        padGate=new PadTemporalGate();padAnalyzer.prepare();
    }
    /** Snapshot before stopping the camera; evidence is signed together with the server nonce. */
    public String loginPayload(String descriptor){if(padAnalyzer==null||padGate==null)throw new IllegalStateException("PAD not enabled");return padGate.signedCapture(descriptor);}

    public void setExtractor(LocalFaceExtractor value) {extractor=value;}
    public void setLoginAttemptStarted(long startedNanos){loginAttemptStartedNanos=startedNanos;}
    boolean isPreviewRunning() {return isPreviewActive;}
    int extractedDimension() {return extractedDimension;}
    FaceAuthCoordinator.CaptureResult.Status captureFailure() {return captureFailure;}
    boolean captureCompleted() {return captureCompleted;}
    String diagnosticProgress() {return "action="+(motion==null?"NONE":motion.action())+" samples="+(motion==null?0:motion.samples())+" failure="+captureFailure+" ear="+lastEyeRatio+" min_ear="+minimumEyeRatio+" yaw="+lastYaw+" inference_ms="+lastInferenceMillis;}
    @Override public void configureChallenge(pe.net.ws.kpiweb.features.auth.models.FaceChallengeResponse challenge) {
        challengeActions=challenge.actions;
    }

    private volatile int currentFaceCount = 0;
    private volatile Rect lastFaceBounds = null;
    private final java.util.List<Rect> recentFaceBounds = new java.util.ArrayList<>();
    private volatile boolean isPreviewActive = false;
    private volatile boolean isCapturing = false;

    public FaceCameraManager(@NonNull Context context,
                             @NonNull TextureView textureView,
                             @Nullable QualityListener qualityListener) {
        this.context = context.getApplicationContext();
        this.textureView = textureView;
        this.qualityListener = qualityListener;
    }

    @Override
    public boolean isCameraAvailable() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return false;
        try {
            for (String id : manager.getCameraIdList()) {
                Integer facing=manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if(facing!=null && facing==CameraCharacteristics.LENS_FACING_FRONT)return true;
            }
            return false;
        } catch (CameraAccessException e) {
            return false;
        }
    }

    @Override
    public boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public synchronized void startPreview() {
        if (isPreviewActive || cameraThread!=null) return;
        firstPreviewFrameAt=0;previewFrames=0;exposureConverged=false;
        firstValidImageLogged=false;firstFaceLogged=false;faceModelsReadyLogged=false;
        startBackgroundThread();

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface,int width,int height) {
                openCamera(width,height);
            }
            @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface,int width,int height) {
                configureTransform();
            }
            @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                closeCamera();return true;
            }
            @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                if(firstPreviewFrameAt==0){firstPreviewFrameAt=android.os.SystemClock.elapsedRealtime();FaceTiming.log(context,"first_preview_frame",cameraOpenStarted);FaceTiming.log(context,"tap_to_first_preview_frame",loginAttemptStartedNanos);}
                previewFrames++;
                int rotation=textureView.getDisplay()==null?0:textureView.getDisplay().getRotation()*90;
                if(rotation!=configuredDisplayRotation)configureTransform();
            }
        });
        if(textureView.isAvailable())openCamera(textureView.getWidth(),textureView.getHeight());

    }

    @Override
    public synchronized void stopPreview() {
        closeCamera();
    }

    private void startBackgroundThread() {
        if (cameraThread == null) {
            cameraThread = new HandlerThread("FaceCameraThread");
            cameraThread.start();
            cameraHandler = new Handler(cameraThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (cameraThread != null) {
            cameraThread.quitSafely();
            try {
                // Camera teardown is often requested by the UI during authentication.
                // Never wait for the camera thread on the main thread.
                if(Looper.myLooper()!=Looper.getMainLooper() && Thread.currentThread()!=cameraThread)cameraThread.join(500);
            } catch (InterruptedException ignored) {}
            cameraThread = null;
            cameraHandler = null;
        }
    }

    private void openCamera(int width, int height) {
        if (!hasCameraPermission()) return;
        cameraOpenStarted=FaceTiming.now();

        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return;

        try {
            // Find front-facing camera
            String frontCameraId = null;
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(id);
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = id;
                    break;
                }
            }

            if (frontCameraId == null) return;
            this.cameraId = frontCameraId;

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Integer orientation=characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            sensorOrientation=orientation==null?0:orientation;
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
            }

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    FaceTiming.log(context,"camera_open",cameraOpenStarted);
                    FaceTiming.log(context,"tap_to_camera_open",loginAttemptStartedNanos);
                    synchronized (FaceCameraManager.this) {
                        if(cameraThread==null) {camera.close();return;}
                        cameraDevice = camera;
                        createCameraPreviewSession();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    synchronized (FaceCameraManager.this) {
                        camera.close();
                        cameraDevice = null;
                        isPreviewActive = false;
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    synchronized (FaceCameraManager.this) {
                        camera.close();
                        cameraDevice = null;
                        isPreviewActive = false;
                    }
                }
            }, cameraHandler);

        } catch (SecurityException | CameraAccessException e) {
            closeCamera();
        }
    }

    private void createCameraPreviewSession() {
        if (cameraDevice == null || previewSize == null || !textureView.isAvailable()) return;

        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) return;

            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            mainHandler.post(this::configureTransform);
            Surface surface = new Surface(texture);
            previewSurface = surface;

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            // Configure hardware face detection mode
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager != null) {
                CameraCharacteristics chars = manager.getCameraCharacteristics(cameraId);
                int[] faceModes = chars.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                if (faceModes != null && faceModes.length > 0) {
                    int selectedMode = CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_OFF;
                    for (int mode : faceModes) {
                        if (mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL) {
                            selectedMode = mode;
                            break;
                        } else if (mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE) {
                            selectedMode = mode;
                        }
                    }
                    if (selectedMode != CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_OFF) {
                        previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, selectedMode);
                    }
                }
            }

            // Autofocus and autoexposure
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            cameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            synchronized (FaceCameraManager.this) {
                                if (cameraDevice == null) return;
                                captureSession = session;
                                try {
                                    captureSession.setRepeatingRequest(previewRequestBuilder.build(),
                                            new CameraCaptureSession.CaptureCallback() {
                                                @Override
                                                public void onCaptureCompleted(@NonNull CameraCaptureSession s,
                                                                               @NonNull CaptureRequest req,
                                                                               @NonNull TotalCaptureResult result) {
                                                    processFrameMetadata(result);
                                                }
                                            }, cameraHandler);
                                    isPreviewActive = true;
                                    if(qualityListener!=null)mainHandler.post(()->qualityListener.onPreviewReady());
                                } catch (CameraAccessException | IllegalStateException ignored) {}
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            isPreviewActive = false;
                        }
                    }, cameraHandler);

        } catch (CameraAccessException | IllegalStateException ignored) {}
    }

    private void processFrameMetadata(TotalCaptureResult result) {
        Integer aeState=result.get(CaptureResult.CONTROL_AE_STATE);
        if(aeState!=null && aeState==CaptureResult.CONTROL_AE_STATE_CONVERGED)exposureConverged=true;
        Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
        int faceCount = (faces != null) ? faces.length : 0;
        this.currentFaceCount = faceCount;
        if (faceCount == 1 && faces != null) {
            this.lastFaceBounds = faces[0].getBounds();
            synchronized (recentFaceBounds) {
                recentFaceBounds.add(new Rect(faces[0].getBounds()));
                if (recentFaceBounds.size() > 10) {
                    recentFaceBounds.remove(0);
                }
            }
        } else {
            this.lastFaceBounds = null;
        }

        // Hardware metadata is optional. Only model results update the visible face markers.
    }

    @Override
    public void captureAndProcess(FaceAuthCoordinator.CaptureCallback callback) {
        ++captureGeneration;if(padAnalyzer!=null)padGate=new PadTemporalGate();
        enrollmentCallback=null;loginCallback=null;enrollmentSamples=null;loginSamples=null;
        activeCallback=callback;
        isCapturing=true;
        captureStarted=android.os.SystemClock.elapsedRealtime();
        extractorReadyReported=false;
        extractedDimension=0;
        captureFailure=null;
        captureCompleted=false;
        minimumEyeRatio=Double.POSITIVE_INFINITY;
        try {motion=new FaceMotionGate(challengeActions,captureStarted);}
        catch(IllegalArgumentException invalid) {finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);return;}
        mainHandler.post(this::nextFrame);
    }

    public void captureForEnrollment(EnrollmentCallback callback) {
        ++captureGeneration;if(padAnalyzer!=null)padGate=new PadTemporalGate();
        activeCallback=null;loginCallback=null;loginSamples=null;enrollmentCallback=callback;enrollmentSamples=new FaceEnrollmentSamples();
        isCapturing=true;captureStarted=android.os.SystemClock.elapsedRealtime();
        extractorReadyReported=false;extractedDimension=0;captureFailure=null;captureCompleted=false;
        mainHandler.post(this::nextFrame);
    }

    /** Login retains the same three consistent frames; it emits only the final descriptor. */
    public void captureForLogin(LoginCallback callback) {
        ++captureGeneration;if(padAnalyzer!=null)padGate=new PadTemporalGate();
        activeCallback=null;enrollmentCallback=null;loginCallback=callback;enrollmentSamples=null;loginSamples=new LoginFaceSamples();
        isCapturing=true;captureStarted=android.os.SystemClock.elapsedRealtime();
        loginCaptureStartedNanos=FaceTiming.now();loggedSamples=0;padPassedLogged=false;
        extractorReadyReported=false;extractedDimension=0;captureFailure=null;captureCompleted=false;
        if(padAnalyzer==null || padGate==null){finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);return;}
        mainHandler.post(this::nextFrame);
    }

    private void nextFrame() {
        if(!isCapturing)return;
        long now=android.os.SystemClock.elapsedRealtime();
        if(now-captureStarted>90000) {finishCapture(FaceAuthCoordinator.CaptureResult.Status.TIMEOUT);return;}
        if(extractor==null) {finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);return;}
        if(extractor.hasFailed()) {finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);return;}
        if(!isPreviewActive || !extractor.isReady()) {mainHandler.postDelayed(this::nextFrame,250);return;}
        if(loginCallback!=null && !faceModelsReadyLogged){faceModelsReadyLogged=true;FaceTiming.log(context,"tap_to_face_models_ready",loginAttemptStartedNanos);}
        // Require rendered frames. Use camera AE convergence when available, with the original
        // one-second fallback for devices that omit it; luminance, PAD and identity gates remain.
        if(previewFrames<3 || firstPreviewFrameAt==0 || (!exposureConverged && now-firstPreviewFrameAt<1000)) {mainHandler.postDelayed(this::nextFrame,100);return;}
        if(!extractorReadyReported) {extractorReadyReported=true;FaceTiming.log(context,"analysis_ready",cameraOpenStarted);if(qualityListener!=null)qualityListener.onExtractorReady();}
        int width=480;
        int height=Math.max(1,Math.round(textureView.getHeight()*width/(float)Math.max(1,textureView.getWidth())));
        Bitmap frame=textureView.getBitmap(width,height);
        if(frame==null) {mainHandler.postDelayed(this::nextFrame,250);return;}
        float light=computeAverageLuminance(frame);
        if(qualityListener!=null)qualityListener.onIlluminationChanged(light>=30 && light<=230);
        if(light<30 || light>230) {frame.recycle();finishCapture(FaceAuthCoordinator.CaptureResult.Status.LOW_QUALITY);return;}
        if(loginCallback!=null && !firstValidImageLogged){firstValidImageLogged=true;FaceTiming.log(context,"tap_to_first_valid_image",loginAttemptStartedNanos);}
        final int run=captureGeneration;
        frameAnalysisStarted=FaceTiming.now();
        // The extractor owns its copy. Keep the original for PAD instead of copying it twice.
        final Bitmap padFrame=padAnalyzer==null?null:frame;
        pendingPadFrame=padFrame;
        extractor.extract(frame,new LocalFaceExtractor.Callback(){
            public void onFailure(){
                if(pendingPadFrame==padFrame)pendingPadFrame=null;
                if(padFrame!=null&&!padFrame.isRecycled())padFrame.recycle();
                if(isCapturing&&run==captureGeneration)finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);
            }
            public void onFrame(org.json.JSONObject result){
                FaceTiming.log(context,"face_detection",frameAnalysisStarted);
                FaceTiming.logMillis(context,"face_js_decode_and_inference",result.optDouble("ms",Double.NaN));
                if(loginCallback!=null && !firstFaceLogged && result.optInt("count",0)==1
                        && result.optDouble("score",0)>=.7 && result.optDouble("faceRatio",0)>=.2){
                    firstFaceLogged=true;FaceTiming.log(context,"tap_to_first_face",loginAttemptStartedNanos);
                }
                if(pendingPadFrame==padFrame)pendingPadFrame=null;
                if(!isCapturing||run!=captureGeneration){if(padFrame!=null)padFrame.recycle();return;}
                if(padAnalyzer==null){handleExtractedFrame(result,null);return;}
                if(result.optInt("count",0)!=1||result.optDouble("score",0)<.7||result.optDouble("faceRatio",0)<.2){
                    if(padFrame!=null)padFrame.recycle();
                    padGate.interrupt();if(loginSamples!=null)loginSamples.interrupt();handleExtractedFrame(result,null);return;
                }
                try{
                    org.json.JSONArray b=result.getJSONArray("box");if(b.length()!=4||padFrame==null)throw new IllegalArgumentException();
                    double[] box={b.getDouble(0),b.getDouble(1),b.getDouble(2),b.getDouble(3)};
                    long padStarted=FaceTiming.now();
                    padAnalyzer.analyze(padFrame,box,new LocalPadAnalyzer.Callback(){
                        public void result(PadScore score){FaceTiming.log(context,"local_pad",padStarted);FaceTiming.logMillis(context,"pad_model_inference",score.millis);if(isCapturing&&run==captureGeneration)handleExtractedFrame(result,score);}
                        public void failed(){if(isCapturing&&run==captureGeneration)finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);}
                    });
                }catch(Exception invalid){
                    if(padFrame!=null&&!padFrame.isRecycled())padFrame.recycle();
                    finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);
                }
            }
        });
        if(padFrame==null)frame.recycle();
    }
    private void handleExtractedFrame(org.json.JSONObject result,PadScore padScore){
                if(!isCapturing)return;
                int count=result.optInt("count",0);
                if(qualityListener!=null)qualityListener.onFaceCountChanged(count);
                if(count>1) {finishCapture(FaceAuthCoordinator.CaptureResult.Status.MULTIPLE_FACES);return;}
                if(count==0) {
                    if(qualityListener!=null)qualityListener.onLandmarks(new float[0]);
                    queueNextFrame(150);return;
                }
                try {
                    org.json.JSONArray landmarks=result.optJSONArray("landmarks");
                    if(qualityListener!=null && landmarks!=null && landmarks.length()==68) {
                        float[] points=new float[136];
                        for(int i=0;i<68;i++) {
                            points[i*2]=(float)landmarks.getJSONArray(i).getDouble(0);
                            points[i*2+1]=(float)landmarks.getJSONArray(i).getDouble(1);
                        }
                        qualityListener.onLandmarks(points);
                    }
                    org.json.JSONArray values=result.getJSONArray("descriptor");
                    if(values.length()!=128)throw new IllegalArgumentException("Invalid descriptor");
                    double[] vector=new double[128];
                    for(int i=0;i<128;i++) {
                        vector[i]=values.getDouble(i);
                        if(!Double.isFinite(vector[i]))throw new IllegalArgumentException("Nonfinite descriptor");
                    }
                    extractedDimension=vector.length;
                    if(result.getDouble("score")<.7 || result.getDouble("faceRatio")<.2) {
                        queueNextFrame(150);return;
                    }
                PadTemporalGate.State padState=null;
                if(padAnalyzer!=null){
                    if(padScore==null){finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);return;}
                    if(qualityListener!=null)qualityListener.onPadResult(padScore.real,padScore.spoof,padScore.millis);
                    padState=padGate.observe(android.os.SystemClock.elapsedRealtime(),padScore.real,padScore.spoof);
                    if(padState==PadTemporalGate.State.REJECT){finishCapture(FaceAuthCoordinator.CaptureResult.Status.SPOOF_DETECTED);return;}
                    if(loginSamples!=null && padScore.real<.95){loginSamples.interrupt();loggedSamples=0;queueNextFrame(150);return;}
                    if(loginSamples==null && padState!=PadTemporalGate.State.PASS){mainHandler.postDelayed(FaceCameraManager.this::nextFrame,150);return;}
                    if(padState==PadTemporalGate.State.PASS && loginCallback!=null && !padPassedLogged){padPassedLogged=true;FaceTiming.log(context,"pad_pass",loginCaptureStartedNanos);}
                }
                    lastEyeRatio=result.getDouble("ear");lastYaw=result.getDouble("yaw");lastInferenceMillis=result.getDouble("ms");
                    minimumEyeRatio=Math.min(minimumEyeRatio,lastEyeRatio);
                    if(loginSamples!=null){
                        boolean ready=loginSamples.observe(android.os.SystemClock.elapsedRealtime(),lastEyeRatio,lastYaw,vector,
                                padScore.real>=.95,padState==PadTemporalGate.State.PASS);
                        if(loginSamples.size()<loggedSamples)loggedSamples=0;
                        if(loginSamples.size()>loggedSamples){loggedSamples=loginSamples.size();FaceTiming.log(context,"sample_"+loggedSamples,loginCaptureStartedNanos);}
                        if(ready){
                            FaceTiming.log(context,"samples_complete",loginCaptureStartedNanos);
                            LoginCallback callback=loginCallback;loginCallback=null;loginSamples=null;isCapturing=false;
                            org.json.JSONArray frontal=new org.json.JSONArray();
                            for(double component:vector)frontal.put(component);
                            callback.onDescriptor(frontal.toString());
                        }else queueNextFrame(150);
                        return;
                    }
                    if(enrollmentSamples!=null) {
                        boolean ready=enrollmentSamples.observe(android.os.SystemClock.elapsedRealtime(),lastEyeRatio,lastYaw,vector);
                        if(qualityListener!=null)qualityListener.onEnrollmentProgress(enrollmentSamples.size());
                        if(ready) {
                            org.json.JSONArray samples=new org.json.JSONArray();
                            for(double[] sample:enrollmentSamples.values()) {
                                org.json.JSONArray encoded=new org.json.JSONArray();for(double v:sample)encoded.put(v);samples.put(encoded);
                            }
                            EnrollmentCallback callback=enrollmentCallback;enrollmentCallback=null;isCapturing=false;
                            if(callback!=null)callback.onSamples(samples.toString());
                        } else mainHandler.postDelayed(FaceCameraManager.this::nextFrame,150);
                        return;
                    }
                    boolean passed=motion.observe(android.os.SystemClock.elapsedRealtime(),lastEyeRatio,lastYaw,vector);
                    if(qualityListener!=null)qualityListener.onActionChanged(motion.action());
                    if(passed) {
                        captureCompleted=true;
                        FaceAuthCoordinator.CaptureCallback callback=activeCallback;
                        activeCallback=null;isCapturing=false;
                        org.json.JSONArray frontal=new org.json.JSONArray();
                        for(double component:motion.completedDescriptor())frontal.put(component);
                        callback.onResult(FaceAuthCoordinator.CaptureResult.success(frontal.toString()));
                    } else mainHandler.postDelayed(FaceCameraManager.this::nextFrame,100);
                } catch(Exception invalid) {
                    // Category only: never log the frame, descriptor or account information.
                    android.util.Log.w("FaceCapture", "frame_rejected="+invalid.getClass().getSimpleName());
                    finishCapture(FaceAuthCoordinator.CaptureResult.Status.EXTRACTOR_UNAVAILABLE);
                }
    }

    private void queueNextFrame(long otherCaptureDelayMs) {
        // Inference itself spaces login frames; PAD still requires five good observations
        // across at least 800 ms, so an extra timer only adds latency.
        if(loginSamples!=null)mainHandler.post(this::nextFrame);
        else mainHandler.postDelayed(this::nextFrame,otherCaptureDelayMs);
    }
    private void finishCapture(FaceAuthCoordinator.CaptureResult.Status status) {
        ++captureGeneration;
        android.util.Log.i("FaceCapture","capture_stopped="+status.name());
        captureFailure=status;
        FaceAuthCoordinator.CaptureCallback callback=activeCallback;activeCallback=null;isCapturing=false;
        EnrollmentCallback enrollment=enrollmentCallback;enrollmentCallback=null;
        LoginCallback login=loginCallback;loginCallback=null;
        if(callback!=null)callback.onResult(FaceAuthCoordinator.CaptureResult.error(status));
        if(enrollment!=null)enrollment.onError(status);
        if(login!=null)login.onError(status);
    }

    private float computeAverageLuminance(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int stepX = Math.max(1, width / 20);
        int stepY = Math.max(1, height / 20);

        long totalLum = 0;
        int sampleCount = 0;

        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                // Standard Rec. 601 luminance formula
                int lum = (299 * r + 587 * g + 114 * b) / 1000;
                totalLum += lum;
                sampleCount++;
            }
        }

        return sampleCount > 0 ? (float) totalLum / sampleCount : 128f;
    }

    private void configureTransform() {
        if(previewSize==null || textureView.getWidth()==0 || textureView.getHeight()==0)return;
        int rotation=textureView.getDisplay()==null?0:textureView.getDisplay().getRotation()*90;
        configuredDisplayRotation=rotation;
        android.graphics.Matrix matrix=new android.graphics.Matrix();
        matrix.setValues(FacePreviewGeometry.matrix(sensorOrientation,rotation,previewSize.getWidth(),previewSize.getHeight(),
                textureView.getWidth(),textureView.getHeight()));
        textureView.setTransform(matrix);
    }

    public synchronized void closeCamera() {
        ++captureGeneration;
        if(pendingPadFrame!=null&&!pendingPadFrame.isRecycled())pendingPadFrame.recycle();pendingPadFrame=null;
        if(padAnalyzer!=null){padAnalyzer.close();padAnalyzer=null;}
        isPreviewActive = false;
        configuredDisplayRotation=-1;
        isCapturing=false;activeCallback=null;enrollmentCallback=null;loginCallback=null;enrollmentSamples=null;loginSamples=null;
        mainHandler.removeCallbacksAndMessages(null);
        if(extractor!=null)extractor.cancel();
        textureView.setSurfaceTextureListener(null);
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.close();
                captureSession = null;
            }
        } catch (Exception ignored) {}

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if(previewSurface!=null) {previewSurface.release();previewSurface=null;}

        stopBackgroundThread();
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        return Arrays.stream(choices)
                .filter(s -> s.getWidth() >= width && s.getHeight() >= height)
                .min(Comparator.comparingInt(s -> s.getWidth() * s.getHeight()))
                .orElse(choices[0]);
    }
}
