package pe.net.ws.kpiweb.features.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class FacePreviewGeometryTest {
    @Test public void front270PortraitDoesNotRotateSensorTwice() {
        float[] m=FacePreviewGeometry.matrix(270,0,1280,720,720,960);
        assertEquals(0,m[1],.0001);assertEquals(0,m[3],.0001);
        assertTrue(m[0]>0);assertTrue(m[4]>0);
        // A vertical feature remains vertical and upright.
        assertEquals(0,m[1]*100,.0001);assertTrue(m[4]*100>0);
    }
    @Test public void front90PortraitAlsoRemainsUpright() {
        float[] m=FacePreviewGeometry.matrix(90,0,1280,720,720,960);
        assertEquals(0,m[1],.0001);assertEquals(0,m[3],.0001);
    }
    @Test public void everyDisplayRotationKeepsCenterAndAspect() {
        for(int sensor:new int[]{0,90,180,270})for(int display:new int[]{0,90,180,270}) {
            float[] m=FacePreviewGeometry.matrix(sensor,display,1280,720,720,960);
            assertEquals(360,m[0]*360+m[1]*480+m[2],.001);
            assertEquals(480,m[3]*360+m[4]*480+m[5],.001);
            float nativeW=sensor%180==0?1280:720,nativeH=sensor%180==0?720:1280;
            double xPixel=Math.hypot(m[0]*720/nativeW,m[3]*720/nativeW);
            double yPixel=Math.hypot(m[1]*960/nativeH,m[4]*960/nativeH);
            assertEquals(xPixel,yPixel,.0001);
        }
    }
    @Test public void displayRotationIsCompensatedWithoutExtraSensorRotation() {
        float[] m=FacePreviewGeometry.matrix(270,90,1280,720,960,720);
        assertEquals(0,m[0],.0001);assertEquals(0,m[4],.0001);
        assertTrue(m[1]>0);assertTrue(m[3]<0);
    }
    @Test public void upsideDownDisplayIsCompensated() {
        float[] m=FacePreviewGeometry.matrix(270,180,1280,720,720,960);
        assertTrue(m[0]<0);assertTrue(m[4]<0);
    }
    @Test(expected=IllegalArgumentException.class) public void zeroSizeCannotProduceInvalidMatrix() {
        FacePreviewGeometry.matrix(270,0,1280,720,0,960);
    }
}
