package pe.net.ws.kpiweb.features.auth;

/** RGB/NCHW preprocessing of the ORIGINAL ONNX anti-spoof-mn3 model, not the OpenVINO BGR IR.
 * Independent implementation of Keys cubic resize (a=-0.75), replicated border and uint8 rounding.
 * No session decision is made here. */
public final class PadImagePreprocessor {
    public static final int SIZE=128;
    private static final double[] MEAN={151.2405,119.5950,107.8395},STD={63.0105,56.4570,55.0035};
    private PadImagePreprocessor(){}
    public static int[] crop(int width,int height,double x,double y,double w,double h){
        if(width<2||height<2||!Double.isFinite(x+y+w+h)||w<=0||h<=0||x<0||y<0||x+w>1.001||y+h>1.001)throw new IllegalArgumentException("Invalid face bounds");
        // Same expansion factors as the original author's face detector wrapper.
        double cx=(x+w/2)*width,cy=(y+h/2)*height;
        int left=Math.max(0,(int)Math.floor(cx-w*width*1.1/2));
        int top=Math.max(0,(int)Math.floor(cy-h*height*1.05/2));
        int right=Math.min(width,(int)Math.ceil(cx+w*width*1.1/2));
        int bottom=Math.min(height,(int)Math.ceil(cy+h*height*1.05/2));
        if(right-left<16||bottom-top<16)throw new IllegalArgumentException("Face crop too small");
        return new int[]{left,top,right,bottom};
    }
    private static double cubic(double distance){
        double x=Math.abs(distance),a=-.75;
        if(x<=1)return (a+2)*x*x*x-(a+3)*x*x+1;
        if(x<2)return a*x*x*x-5*a*x*x+8*a*x-4*a;
        return 0;
    }
    /** MiniFAS original ONNX input: expanded inclusive crop, linear resize, BGR bytes, no normalization. */
    public static float[] miniFas(int[] pixels,int width,int height,double[] box,double scale){
        if(pixels.length!=width*height||box.length!=4||!Double.isFinite(box[0]+box[1]+box[2]+box[3]+scale)||box[0]<0||box[1]<0||box[2]<=0||box[3]<=0||scale<1)throw new IllegalArgumentException("Invalid MiniFAS input");
        int x=(int)(box[0]*width),y=(int)(box[1]*height),bw=(int)(box[2]*width),bh=(int)(box[3]*height);
        if(bw<16||bh<16||x+bw>width+1||y+bh>height+1)throw new IllegalArgumentException("Invalid MiniFAS bounds");
        scale=Math.min(scale,Math.min((height-1)/(double)bh,(width-1)/(double)bw));
        int l=Math.max(0,(int)(x+bw/2.0-bw*scale/2)),t=Math.max(0,(int)(y+bh/2.0-bh*scale/2));
        int r=Math.min(width-1,(int)(x+bw/2.0+bw*scale/2)),b=Math.min(height-1,(int)(y+bh/2.0+bh*scale/2));
        int w=r-l+1,h=b-t+1;float[] out=new float[3*80*80];
        for(int dy=0;dy<80;dy++)for(int dx=0;dx<80;dx++){
            double sx=(dx+.5)*w/80-.5,sy=(dy+.5)*h/80-.5;int ix=(int)Math.floor(sx),iy=(int)Math.floor(sy);double fx=sx-ix,fy=sy-iy;
            for(int c=0;c<3;c++){
                double value=0;for(int ky=0;ky<=1;ky++)for(int kx=0;kx<=1;kx++){
                    int px=l+Math.max(0,Math.min(w-1,ix+kx)),py=t+Math.max(0,Math.min(h-1,iy+ky));
                    value+=((pixels[py*width+px]>>(8*c))&255)*(kx==0?1-fx:fx)*(ky==0?1-fy:fy);
                }
                out[c*6400+dy*80+dx]=(float)Math.max(0,Math.min(255,Math.round(value)));
            }
        }
        return out;
    }
    public static float[] tensor(int[] argb,int width,int height,int[] rect){
        if(width<1||height<1||argb.length!=width*height||rect.length!=4||rect[0]<0||rect[1]<0||rect[2]>width||rect[3]>height||rect[2]<=rect[0]||rect[3]<=rect[1])throw new IllegalArgumentException("Invalid pixels/crop");
        int rw=rect[2]-rect[0],rh=rect[3]-rect[1];float[] out=new float[3*SIZE*SIZE];
        for(int dy=0;dy<SIZE;dy++)for(int dx=0;dx<SIZE;dx++){
            double sx=(dx+.5)*rw/SIZE-.5,sy=(dy+.5)*rh/SIZE-.5;int ix=(int)Math.floor(sx),iy=(int)Math.floor(sy);
            double[] rgb=new double[3];
            for(int ky=-1;ky<=2;ky++)for(int kx=-1;kx<=2;kx++){
                int px=rect[0]+Math.max(0,Math.min(rw-1,ix+kx)),py=rect[1]+Math.max(0,Math.min(rh-1,iy+ky));
                double weight=cubic(sx-ix-kx)*cubic(sy-iy-ky);int pixel=argb[py*width+px];
                rgb[0]+=weight*((pixel>>16)&255);rgb[1]+=weight*((pixel>>8)&255);rgb[2]+=weight*(pixel&255);
            }
            for(int c=0;c<3;c++){double value=Math.max(0,Math.min(255,Math.round(rgb[c])));out[c*SIZE*SIZE+dy*SIZE+dx]=(float)((value-MEAN[c])/STD[c]);}
        }
        return out;
    }
}
