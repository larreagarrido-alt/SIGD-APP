package pe.net.ws.kpiweb.features.auth;

/** Three unchanged frontal samples collected only from the current good PAD streak. */
final class LoginFaceSamples {
    private final FaceEnrollmentSamples samples=new FaceEnrollmentSamples();
    private boolean ready;

    void interrupt(){samples.reset();ready=false;}
    int size(){return samples.size();}

    boolean observe(long time,double ear,double yaw,double[] vector,boolean padGood,boolean padPassed){
        if(!padGood){interrupt();return false;}
        if(ready){
            if(!samples.matchesCurrent(ear,yaw,vector)){
                interrupt();samples.observe(time,ear,yaw,vector);return false;
            }
        } else ready=samples.observe(time,ear,yaw,vector);
        return ready && padPassed;
    }
}
