package pe.net.ws.kpiweb.features.auth;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
public class PadImagePreprocessorTest {
    @Test public void originalModelUsesRgbChannelsAndByteScale(){
        int[] red=new int[16*16];Arrays.fill(red,0xffff0000);float[] tensor=PadImagePreprocessor.tensor(red,16,16,new int[]{0,0,16,16});
        assertEquals((255-151.2405)/63.0105,tensor[0],1e-6);assertEquals(-119.595/56.457,tensor[16384],1e-6);assertEquals(-107.8395/55.0035,tensor[32768],1e-6);
        for(int i=0;i<16384;i++)assertEquals(tensor[0],tensor[i],0);
    }
    @Test public void cropExcludesPixelsOutsideFace(){int[] p=new int[64*64];Arrays.fill(p,0xffffffff);for(int y=16;y<48;y++)for(int x=16;x<48;x++)p[y*64+x]=0xff000000;float[] t=PadImagePreprocessor.tensor(p,64,64,new int[]{16,16,48,48});assertEquals(-151.2405/63.0105,t[0],1e-6);}
    @Test public void normalizedFaceBoundsExpandAndClip(){assertArrayEquals(new int[]{0,0,100,100},PadImagePreprocessor.crop(100,100,0,0,1,1));}
    @Test(expected=IllegalArgumentException.class) public void invalidFaceBoundsRejected(){PadImagePreprocessor.crop(100,100,Double.NaN,0,.5,.5);}
    @Test(expected=IllegalArgumentException.class) public void tinyCropRejected(){PadImagePreprocessor.crop(100,100,.4,.4,.05,.05);}
    @Test(expected=IllegalArgumentException.class) public void outOfBoundsPixelsRejected(){PadImagePreprocessor.tensor(new int[100],10,10,new int[]{0,0,11,10});}
    @Test public void miniFasUsesUnnormalizedBgrByteScale(){int[] p=new int[64*64];Arrays.fill(p,0xffff0000);float[] t=PadImagePreprocessor.miniFas(p,64,64,new double[]{.25,.25,.5,.5},2.7);assertEquals(0,t[0],0);assertEquals(0,t[6400],0);assertEquals(255,t[12800],0);}
}
