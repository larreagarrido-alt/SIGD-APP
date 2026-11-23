package pe.net.ws.kpiweb.features.auth;

/** TextureView already applies SENSOR_ORIENTATION. Only compensate display rotation here. */
final class FacePreviewGeometry {
    static float[] matrix(int sensorDegrees,int displayDegrees,int bufferWidth,int bufferHeight,int viewWidth,int viewHeight) {
        if(bufferWidth<=0 || bufferHeight<=0 || viewWidth<=0 || viewHeight<=0
                || sensorDegrees%90!=0 || displayDegrees%90!=0)throw new IllegalArgumentException("Invalid preview geometry");
        boolean sensorSwapped=Math.floorMod(sensorDegrees,180)!=0;
        float nativeWidth=sensorSwapped?bufferHeight:bufferWidth;
        float nativeHeight=sensorSwapped?bufferWidth:bufferHeight;
        boolean displaySwapped=Math.floorMod(displayDegrees,180)!=0;
        float finalWidth=displaySwapped?nativeHeight:nativeWidth;
        float finalHeight=displaySwapped?nativeWidth:nativeHeight;
        float crop=Math.max(viewWidth/finalWidth,viewHeight/finalHeight);
        double radians=Math.toRadians(-displayDegrees);
        float cos=(float)Math.cos(radians),sin=(float)Math.sin(radians);
        float a=cos*crop*nativeWidth/viewWidth,b=-sin*crop*nativeHeight/viewHeight;
        float c=sin*crop*nativeWidth/viewWidth,d=cos*crop*nativeHeight/viewHeight;
        float x=viewWidth/2f,y=viewHeight/2f;
        return new float[]{a,b,x-a*x-b*y,c,d,y-c*x-d*y,0,0,1};
    }
}
