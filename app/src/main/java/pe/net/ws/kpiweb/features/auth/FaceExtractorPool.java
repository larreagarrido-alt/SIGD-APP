package pe.net.ws.kpiweb.features.auth;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.view.ViewGroup;
import android.webkit.WebView;

/** Keeps only loaded model weights in this process between login screens. UI thread only. */
final class FaceExtractorPool {
    static final class Entry {
        final WebView view;
        final LocalFaceExtractor extractor;
        Entry(WebView view, LocalFaceExtractor extractor) {
            this.view=view;this.extractor=extractor;
        }
    }

    private static Entry available;
    private FaceExtractorPool() {}

    static Entry take(Context activity) {
        Entry entry=available;
        available=null;
        if(entry==null){FaceTiming.event(activity,"face_pool_miss");return null;}
        if(!entry.extractor.canRetain()) {
            FaceTiming.event(activity,"face_pool_stale");
            entry.extractor.destroy();
            return null;
        }
        FaceTiming.event(activity,"face_pool_hit");
        ((MutableContextWrapper)entry.view.getContext()).setBaseContext(activity);
        return entry;
    }

    static void offer(Context application,WebView view,LocalFaceExtractor extractor) {
        if(view==null || extractor==null)return;
        extractor.cancel();
        ViewGroup parent=(ViewGroup)view.getParent();
        if(parent!=null)parent.removeView(view);
        if(!extractor.canRetain() || !(view.getContext() instanceof MutableContextWrapper)
                || available!=null) {
            FaceTiming.event(application,extractor.hasFailed()?"face_pool_rejected_failed":
                    !extractor.canRetain()?"face_pool_rejected_busy":
                    available!=null?"face_pool_rejected_occupied":"face_pool_rejected_context");
            extractor.destroy();
            return;
        }
        FaceTiming.event(application,extractor.isReady()?"face_pool_offered_ready":"face_pool_offered_loading");
        ((MutableContextWrapper)view.getContext()).setBaseContext(application.getApplicationContext());
        available=new Entry(view,extractor);
    }
}
