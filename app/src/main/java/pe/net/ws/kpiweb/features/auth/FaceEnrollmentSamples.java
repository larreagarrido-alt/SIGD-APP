package pe.net.ws.kpiweb.features.auth;

import java.util.ArrayList;
import java.util.List;

/** Three consistent frontal model outputs. This is profile collection, not liveness proof. */
final class FaceEnrollmentSamples {
    private final List<double[]> samples=new ArrayList<>();
    private long last=-1;
    void reset(){samples.clear();last=-1;}
    boolean matchesCurrent(double ear,double yaw,double[] vector){
        if(samples.size()!=3 || !validFace(ear,yaw,vector))return false;
        for(double[] reference:samples)if(distance(reference,vector)>.58)return false;
        return true;
    }
    boolean observe(long time,double ear,double yaw,double[] vector) {
        if(!validFace(ear,yaw,vector) || (last>=0 && time-last<600))return false;
        for(double[] reference:samples)if(distance(reference,vector)>.58){reset();break;}
        samples.add(vector.clone());last=time;return samples.size()==3;
    }
    private static boolean validFace(double ear,double yaw,double[] vector){
        if(vector==null || vector.length!=128 || !Double.isFinite(ear) || !Double.isFinite(yaw)
                || ear<.20 || Math.abs(yaw)>=.18)return false;
        double norm=0;for(double value:vector){if(!Double.isFinite(value)||Math.abs(value)>10)return false;norm+=value*value;}
        return norm>=.01 && norm<=100;
    }
    private static double distance(double[] first,double[] second){
        double sum=0;for(int i=0;i<128;i++)sum+=Math.pow(first[i]-second[i],2);
        return Math.sqrt(sum);
    }
    int size(){return samples.size();}
    List<double[]> values(){List<double[]> copy=new ArrayList<>();for(double[] v:samples)copy.add(v.clone());return copy;}
}
