package pe.net.ws.kpiweb.features.auth;
import android.content.Context;
import android.graphics.Bitmap;
import ai.onnxruntime.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.security.MessageDigest;
import java.util.Collections;

/** Real offline MiniFAS inference. Conservative minimum of two live probabilities, not proof of camera origin. */
public final class MiniFasPadClassifier implements AutoCloseable {
    private final OrtEnvironment environment=OrtEnvironment.getEnvironment();
    private final OrtSession first,second;
    public static final class Result {
        public final double v1Real,v2Real;public final PadScore score;
        Result(double a,double b,double millis){v1Real=a;v2Real=b;double real=Math.min(a,b);score=new PadScore(real,1-real,millis);}
    }
    public MiniFasPadClassifier(Context context)throws Exception{
        first=load(context,"MiniFASNetV1SE.onnx","ebab7f90c7833fbccd46d3a555410e78d969db5438e169b6524be444862b3676");
        try{second=load(context,"MiniFASNetV2.onnx","b32929adc2d9c34b9486f8c4c7bc97c1b69bc0ea9befefc380e4faae4e463907");}
        catch(Exception invalid){first.close();throw invalid;}
    }
    private OrtSession load(Context context,String name,String expected)throws Exception{
        byte[] data;try(InputStream in=context.getAssets().open("face/pad/"+name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);data=out.toByteArray();
        }
        StringBuilder hash=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(data))hash.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
        if(!expected.equals(hash.toString()))throw new SecurityException("MiniFAS integrity failure");
        try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
            options.setIntraOpNumThreads(2);options.setInterOpNumThreads(1);
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);return environment.createSession(data,options);
        }
    }
    public static float[] input(Bitmap image,double[] box,double scale){int w=image.getWidth(),h=image.getHeight();int[] p=new int[w*h];image.getPixels(p,0,w,0,0,w,h);return PadImagePreprocessor.miniFas(p,w,h,box,scale);}
    private double probability(OrtSession session,float[] input)throws Exception{
        try(OnnxTensor tensor=OnnxTensor.createTensor(environment,FloatBuffer.wrap(input),new long[]{1,3,80,80});
            OrtSession.Result result=session.run(Collections.singletonMap("input",tensor))){
            float[][] logits=(float[][])result.get(0).getValue();if(logits.length!=1||logits[0].length!=3)throw new IllegalStateException("MiniFAS shape");
            double max=Math.max(logits[0][0],Math.max(logits[0][1],logits[0][2])),sum=0;
            for(float logit:logits[0]){if(!Float.isFinite(logit))throw new IllegalStateException("Nonfinite MiniFAS logits");sum+=Math.exp(logit-max);}
            return Math.exp(logits[0][1]-max)/sum;
        }
    }
    public synchronized Result score(Bitmap image,double[] box)throws Exception{
        long start=System.nanoTime();
        int width=image.getWidth(),height=image.getHeight();
        int[] pixels=new int[width*height];
        image.getPixels(pixels,0,width,0,0,width,height);
        double a=probability(first,PadImagePreprocessor.miniFas(pixels,width,height,box,4.0));
        double b=probability(second,PadImagePreprocessor.miniFas(pixels,width,height,box,2.7));
        return new Result(a,b,(System.nanoTime()-start)/1_000_000.0);
    }
    @Override public synchronized void close()throws OrtException{try{first.close();}finally{second.close();}}
}
