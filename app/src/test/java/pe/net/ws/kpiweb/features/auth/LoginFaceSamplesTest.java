package pe.net.ws.kpiweb.features.auth;

import static org.junit.Assert.*;
import org.junit.Test;

public class LoginFaceSamplesTest {
    private static double[] face(){double[] v=new double[128];v[0]=1;return v;}

    @Test public void threeConsistentSamplesAndFullPadCanFinishTogether(){
        LoginFaceSamples samples=new LoginFaceSamples();PadTemporalGate pad=new PadTemporalGate();double[] face=face();
        for(int i=0;i<4;i++){
            long time=i*300;
            assertFalse(samples.observe(time,.30,0,face,true,
                    pad.observe(time,.99,.01)==PadTemporalGate.State.PASS));
        }
        assertTrue(samples.observe(1200,.30,0,face,true,
                pad.observe(1200,.99,.01)==PadTemporalGate.State.PASS));
        assertEquals(3,samples.size());
    }

    @Test public void padInterruptionDiscardsEarlierSamples(){
        LoginFaceSamples samples=new LoginFaceSamples();double[] face=face();
        samples.observe(0,.30,0,face,true,false);
        samples.observe(600,.30,0,face,true,false);
        assertFalse(samples.observe(900,.30,0,face,false,false));
        assertEquals(0,samples.size());
        assertFalse(samples.observe(1200,.30,0,face,true,true));
        assertEquals(1,samples.size());
    }

    @Test public void differentFaceCannotFinishAfterPadPass(){
        LoginFaceSamples samples=new LoginFaceSamples();double[] face=face();
        samples.observe(0,.30,0,face,true,false);
        samples.observe(600,.30,0,face,true,false);
        samples.observe(1200,.30,0,face,true,false);
        double[] other=face();other[0]=0;other[1]=1;
        assertFalse(samples.observe(1500,.30,0,other,true,true));
        assertEquals(1,samples.size());
    }
}
