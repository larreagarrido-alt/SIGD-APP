package pe.net.ws.kpiweb.features.auth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Bundled inference only. Never receives tokens, user names or server descriptors. */
public final class LocalFaceExtractor {
    public interface Callback {void onFrame(JSONObject frame); void onFailure();}
    public static final String ORIGIN = "https://appassets.androidplatform.net";
    private final WebView view;
    private final Context context;
    private final long modelsStarted;
    private long pageStarted;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService encoder=java.util.concurrent.Executors.newSingleThreadExecutor();
    private Callback pending;
    private boolean ready, destroyed, failed, processing;
    private int request;
    private int firstRequest;
    private long firstDispatchStarted;
    private final Runnable timeout = () -> fail();
    private final Runnable modelsTimeout=()->{if(!ready && !destroyed){failed=true;fail();}};

    @SuppressLint("SetJavaScriptEnabled")
    public LocalFaceExtractor(Context context, WebView view) {
        this.view=view;this.context=context.getApplicationContext();modelsStarted=FaceTiming.now();
        WebSettings s=view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false); s.setAllowUniversalAccessFromFileURLs(false);
        s.setDomStorageEnabled(false); s.setDatabaseEnabled(false);
        s.setGeolocationEnabled(false); s.setBlockNetworkLoads(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        view.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView web, String url, android.graphics.Bitmap icon) {
                pageStarted=FaceTiming.now();
            }
            @Override public void onPageFinished(WebView web, String url) {
                FaceTiming.log(LocalFaceExtractor.this.context,"face_webview_page",pageStarted);
            }
            @androidx.annotation.RequiresApi(26)
            @Override public boolean onRenderProcessGone(WebView web, android.webkit.RenderProcessGoneDetail detail) {
                FaceTiming.event(LocalFaceExtractor.this.context,"face_renderer_gone");
                ready=false;failed=true;fail();return true;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u=r.getUrl();
                if ("GET".equals(r.getMethod()) && "https".equals(u.getScheme())
                        && "appassets.androidplatform.net".equals(u.getHost()) && u.getPort()==-1
                        && u.getQuery()==null && isBundledPath(u.getPath())) {
                    String path=u.getPath().substring(1);
                    String mime=path.endsWith(".html")?"text/html":path.endsWith(".js")?"text/javascript":path.endsWith(".json")?"application/json":"application/octet-stream";
                    try {return new WebResourceResponse(mime,"UTF-8",context.getAssets().open(path));}
                    catch(IOException ignored) { /* fail closed */ }
                }
                return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),
                        new ByteArrayInputStream(new byte[0]));
            }
        });
        view.addJavascriptInterface(new Object() {
            @JavascriptInterface public void result(String payload) {
                if(payload==null || payload.length()>24000) {main.post(()->fail());return;}
                main.post(()->{
                    if(destroyed)return;
                    try {
                        JSONObject object=new JSONObject(payload);
                        if("ready".equals(object.optString("kind"))) {
                            main.removeCallbacks(modelsTimeout);ready=true;
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_page_to_js_init",object.optDouble("page_to_init_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_script_resource",object.optDouble("script_resource_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_model_resource",object.optDouble("model_resource_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_detector_load",object.optDouble("detector_load_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_landmarks_load",object.optDouble("landmarks_load_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_descriptor_load",object.optDouble("descriptor_load_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_model_load",object.optDouble("load_ms",Double.NaN));
                            FaceTiming.log(LocalFaceExtractor.this.context,"face_models",modelsStarted);
                            return;
                        }
                        if("error".equals(object.optString("kind")) && !object.has("id")) {failed=true;fail();return;}
                        if(object.optInt("id",-1)!=request || pending==null)return;
                        processing=false;
                        if(!"frame".equals(object.optString("kind"))) {fail();return;}
                        if(request==firstRequest) {
                            FaceTiming.log(LocalFaceExtractor.this.context,"face_first_webview_roundtrip",firstDispatchStarted);
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_first_image_decode",object.optDouble("decode_ms",Double.NaN));
                            FaceTiming.logMillis(LocalFaceExtractor.this.context,"face_first_network_inference",object.optDouble("network_ms",Double.NaN));
                        }
                        Callback callback=pending;pending=null;main.removeCallbacks(timeout);
                        callback.onFrame(object);
                    } catch(Exception ignored) {fail();}
                });
            }
        },"FaceNative");
        main.postDelayed(modelsTimeout,45000);
        view.loadUrl(ORIGIN+"/face/engine.html");
    }

    static boolean isBundledPath(String path) {
        return path!=null && path.matches("/face/(engine\\.(html|js)|face-api\\.min\\.js|models/(ssd_mobilenetv1_model|face_landmark_68_model|face_recognition_model)(-weights_manifest\\.json|-shard[12]))");
    }
    public boolean isReady() {return ready && !destroyed && !failed;}
    public boolean isReusable() {return isReady() && !processing;}
    public boolean canRetain() {return !destroyed && !failed && !processing;}
    public boolean hasFailed() {return failed;}
    public void extract(Bitmap frame, Callback callback) {
        if(!isReady() || pending!=null) {callback.onFailure();return;}
        // The caller may recycle its bitmap as soon as extract returns.
        Bitmap owned=frame.copy(Bitmap.Config.ARGB_8888,false);
        if(owned==null){callback.onFailure();return;}
        pending=callback;processing=true;int id=++request;
        if(firstRequest==0)firstRequest=id;
        main.postDelayed(timeout,20000);
        encoder.execute(()->{
            long encodeStarted=FaceTiming.now();
            try {
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                if(!owned.compress(Bitmap.CompressFormat.JPEG,92,out))throw new IllegalStateException("Encoding failed");
                String image="data:image/jpeg;base64,"+Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
                String script="FaceEngine.process("+id+","+JSONObject.quote(image)+")";
                if(id==firstRequest)FaceTiming.log(context,"face_first_jpeg_encode",encodeStarted);
                main.post(()->{
                    if(!destroyed && id==request && pending!=null){
                        if(id==firstRequest)firstDispatchStarted=FaceTiming.now();
                        view.evaluateJavascript(script,null);
                    }
                });
            } catch(RuntimeException failure) {
                main.post(()->{if(!destroyed && id==request)fail();});
            } finally {owned.recycle();}
        });
    }
    private void fail() {
        failed=true;
        main.removeCallbacks(timeout);Callback callback=pending;pending=null;processing=false;
        if(callback!=null && !destroyed)callback.onFailure();
    }
    public void cancel() {request++;pending=null;main.removeCallbacks(timeout);}
    public void destroy() {
        destroyed=true;cancel();view.removeJavascriptInterface("FaceNative");
        main.removeCallbacks(modelsTimeout);
        encoder.shutdown();
        view.stopLoading();view.destroy();
    }
}
