package pe.net.ws.kpiweb.features.auth;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.*;

/** Single-flight caller owns capture sequencing; this worker takes/recycles each supplied bitmap. */
public final class LocalPadAnalyzer implements AutoCloseable {
    public interface Callback{void result(PadScore score);void failed();}
    private final Context context;private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private volatile boolean closed;private MiniFasPadClassifier model;private boolean preparationFailed;
    public LocalPadAnalyzer(Context context){this.context=context.getApplicationContext();}
    /** Queue model loading before the first frame, on the same worker used for inference. */
    public void prepare(){
        if(closed)return;
        try{worker.execute(()->{
            if(closed||model!=null)return;
            long started=FaceTiming.now();
            try{model=new MiniFasPadClassifier(context);FaceTiming.log(context,"pad_models",started);}
            catch(Exception|LinkageError invalid){
                preparationFailed=true;
            }
        });}catch(RejectedExecutionException ignored){}
    }
    public void analyze(Bitmap capture,double[] box,Callback callback){
        if(closed){capture.recycle();return;}
        try{worker.execute(()->{
            try{
                if(closed)return;if(preparationFailed)throw new IllegalStateException("PAD model unavailable");
                if(model==null){long started=FaceTiming.now();model=new MiniFasPadClassifier(context);FaceTiming.log(context,"pad_models",started);}
                PadScore score=model.score(capture,box).score;
                main.post(()->{if(!closed)callback.result(score);});
            }catch(Exception|LinkageError invalid){main.post(()->{if(!closed)callback.failed();});}
            finally{capture.recycle();}
        });}catch(RejectedExecutionException stopped){capture.recycle();}
    }
    @Override public void close(){
        if(closed)return;closed=true;
        worker.execute(()->{if(model!=null)try{model.close();}catch(Exception ignored){}model=null;});worker.shutdown();
    }
}
