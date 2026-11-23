package pe.net.ws.kpiweb.features.auth;
import org.junit.Test;
import static org.junit.Assert.*;
public class FaceEnrollmentSamplesTest {
    private double[] vector(){double[] v=new double[128];v[0]=1;return v;}
    @Test public void requiresThreeFrontalSamplesSpacedApart(){
        FaceEnrollmentSamples samples=new FaceEnrollmentSamples();double[] v=vector();
        assertFalse(samples.observe(0,.3,0,v));assertFalse(samples.observe(100,.3,0,v));assertEquals(1,samples.size());
        assertFalse(samples.observe(600,.3,0,v));assertTrue(samples.observe(1200,.3,0,v));assertEquals(3,samples.size());
    }
    @Test public void closedEyesPoseAndInvalidVectorsCannotEnroll(){
        FaceEnrollmentSamples samples=new FaceEnrollmentSamples();double[] v=vector();
        assertFalse(samples.observe(0,.1,0,v));assertFalse(samples.observe(600,.3,.3,v));
        v[0]=Double.NaN;assertFalse(samples.observe(1200,.3,0,v));
        assertFalse(samples.observe(1800,.3,0,new double[128]));assertEquals(0,samples.size());
    }
    @Test public void anotherFaceCannotBeMixedIntoExistingProfile(){
        FaceEnrollmentSamples samples=new FaceEnrollmentSamples();double[] v=vector();
        samples.observe(0,.3,0,v);samples.observe(600,.3,0,v);
        double[] other=vector();other[0]=0;other[1]=1;
        assertFalse(samples.observe(1200,.3,0,other));assertEquals(1,samples.size());
        assertArrayEquals(other,samples.values().get(0),0);
    }
    @Test public void capturedVectorsAreDefensivelyCopied(){
        FaceEnrollmentSamples samples=new FaceEnrollmentSamples();double[] v=vector();samples.observe(0,.3,0,v);
        v[0]=5;assertEquals(1,samples.values().get(0)[0],0);
        samples.values().get(0)[0]=7;assertEquals(1,samples.values().get(0)[0],0);
    }
}
