package pe.net.ws.kpiweb.features.auth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/** Actual model landmarks. No synthetic scan animation or software compositing layer. */
public class FaceOverlayView extends View {
    private final Paint scrim=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dots=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval=new RectF();
    private final Path mask=new Path();
    private float[] landmarks=new float[0];
    private boolean scanning;
    private boolean minimalGuidance;
    public FaceOverlayView(Context context){super(context);init();}
    public FaceOverlayView(Context context,@Nullable AttributeSet attrs){super(context,attrs);init();}
    public FaceOverlayView(Context context,@Nullable AttributeSet attrs,int style){super(context,attrs,style);init();}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
    private void init(){
        scrim.setColor(Color.argb(155,18,4,40));
        border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(dp(2));border.setColor(0xffa855f7);
        dots.setStyle(Paint.Style.FILL);dots.setColor(0xff66ffdc);
        mask.setFillType(Path.FillType.EVEN_ODD);
    }
    public void setScanning(boolean value){
        if(scanning==value)return;scanning=value;
        if(!value)landmarks=new float[0];invalidate();
    }
    public boolean isScanning(){return scanning;}
    /** Login only: keep the camera unobstructed; enrollment retains its framing oval. */
    public void setMinimalGuidance(boolean value){minimalGuidance=value;invalidate();}
    public void setLandmarks(float[] value){
        if(value==null || value.length!=136){landmarks=new float[0];invalidate();return;}
        for(float coordinate:value)if(!Float.isFinite(coordinate))return;
        landmarks=value.clone();invalidate();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        float width=Math.min(Math.min(w*.82f,h*.9f/1.35f),dp(280));
        float height=width*1.35f;
        oval.set((w-width)/2,(h-height)/2,(w+width)/2,(h+height)/2);
        mask.reset();mask.setFillType(Path.FillType.EVEN_ODD);
        mask.addRect(0,0,w,h,Path.Direction.CW);mask.addOval(oval,Path.Direction.CW);
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(minimalGuidance){
            float inset=dp(16),length=dp(18),right=getWidth()-inset,bottom=getHeight()-inset;
            border.setColor(scanning?0xff66ffdc:0xffc9a7ee);
            canvas.drawLine(inset,inset,inset+length,inset,border);
            canvas.drawLine(inset,inset,inset,inset+length,border);
            canvas.drawLine(right-length,inset,right,inset,border);
            canvas.drawLine(right,inset,right,inset+length,border);
            canvas.drawLine(inset,bottom-length,inset,bottom,border);
            canvas.drawLine(inset,bottom,inset+length,bottom,border);
            canvas.drawLine(right-length,bottom,right,bottom,border);
            canvas.drawLine(right,bottom-length,right,bottom,border);
            return;
        }
        border.setColor(0xffa855f7);
        canvas.drawPath(mask,scrim);canvas.drawOval(oval,border);
        for(int i=0;i<landmarks.length;i+=2){
            float x=landmarks[i],y=landmarks[i+1];
            if(x>=0 && x<=1 && y>=0 && y<=1)canvas.drawCircle(x*getWidth(),y*getHeight(),dp(1.7f),dots);
        }
    }
}
