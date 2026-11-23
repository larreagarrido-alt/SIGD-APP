package pe.net.ws.kpiweb.features.auth;

import java.util.Arrays;
import java.util.List;

/** Temporal landmark checks only. This is NOT a certified presentation attack detector. */
public final class FaceMotionGate {
    private final List<String> actions;
    private final long started;
    private long lastTime = -1;
    private long stageStarted;
    private int stage;
    private int samples;
    private int stableSamples;
    private boolean closed;
    private boolean eyesOpen;
    private double openBaseline;
    private double[] identity;
    // Same embedding distance used by attendance and the 1:1 backend, not a PAD score.
    static final double CONTINUITY_LIMIT=.58;
    private final java.util.ArrayList<double[]> baselineFrames=new java.util.ArrayList<>();
    private final java.util.ArrayList<Double> baselineEyes=new java.util.ArrayList<>();
    private final java.util.ArrayList<double[]> finalFrames=new java.util.ArrayList<>();
    private double[] completedDescriptor;
    private boolean reposition;
    private int repositionFrames;

    public FaceMotionGate(String[] actions, long started) {
        if (actions == null || actions.length != 3 || !Arrays.asList(actions).contains("BLINK")
                || !Arrays.asList(actions).contains("TURN_LEFT") || !Arrays.asList(actions).contains("TURN_RIGHT")) {
            throw new IllegalArgumentException("Three distinct facial actions required");
        }
        this.actions = Arrays.asList(actions.clone());
        this.started = started;
        stageStarted = started;
    }

    public boolean observe(long time, double ear, double yaw, double[] vector) {
        if (time <= lastTime || time - started > 90000 || vector == null || vector.length != 128
                || !Double.isFinite(ear) || !Double.isFinite(yaw)) return false;
        for (double v : vector) if (!Double.isFinite(v)) return false;
        lastTime = time;
        // First establish open eyes and a centered face across several actual frames.
        // Do not anchor continuity to the arbitrary first frame while the user is positioning.
        if (openBaseline == 0) {
            samples++;
            if (ear < .20 || Math.abs(yaw) >= .18) {
                stableSamples=0;samples=0;baselineFrames.clear();baselineEyes.clear();return false;
            }
            for(double[] reference:baselineFrames)if(distance(reference,vector)>CONTINUITY_LIMIT) {
                baselineFrames.clear();baselineEyes.clear();stableSamples=0;samples=1;break;
            }
            baselineFrames.add(vector.clone());baselineEyes.add(ear);
            if (++stableSamples >= 3) {
                identity=medoid(baselineFrames);
                java.util.Collections.sort(baselineEyes);openBaseline=baselineEyes.get(1);
                eyesOpen = true; stableSamples = 0; stageStarted = time;
            }
            return false;
        }
        if (distance(identity,vector) > CONTINUITY_LIMIT) {
            // Do not interpret an unusable pose as proof that the person changed.
            // Do not count it, learn a new reference, or retain a partial blink.
            reposition=true;repositionFrames=0;stableSamples=0;closed=false;finalFrames.clear();return false;
        }
        samples++;
        if(reposition) {
            repositionFrames=Math.abs(yaw)<.18 && ear>=openBaseline*.85?repositionFrames+1:0;
            if(repositionFrames>=3){reposition=false;stageStarted=time;}
            return false;
        }
        if (stage >= actions.size()) {
            if(Math.abs(yaw)>=.18 || ear<openBaseline*.85) {finalFrames.clear();return false;}
            finalFrames.add(vector.clone());
            if(finalFrames.size()>=3 && samples>=10 && time-started>=2000) {
                completedDescriptor=medoid(finalFrames);return true;
            }
            return false;
        }
        String action = actions.get(stage);
        boolean passed;
        if ("BLINK".equals(action)) {
            if (eyesOpen && ear < openBaseline * .65) closed = true;
            passed = closed && ear > openBaseline * .85 && time-stageStarted >= 400;
        } else {
            boolean positioned = "TURN_LEFT".equals(action) ? yaw < -.22 : yaw > .22;
            stableSamples = positioned ? stableSamples+1 : 0;
            passed = stableSamples >= 2 && time-stageStarted >= 400;
        }
        if (passed) {stage++; closed=false; stableSamples=0; stageStarted=time;}
        return false; // Always reacquire a frontal descriptor after the last head turn.
    }
    public String action() {return reposition?"REPOSITION":openBaseline == 0 ? "CENTER" : stage < actions.size() ?
        ("BLINK".equals(actions.get(stage)) && closed?"OPEN_EYES":actions.get(stage)) : completedDescriptor==null?"RETURN_CENTER":"DONE";}
    public int samples() {return samples;}
    public double[] completedDescriptor() {return completedDescriptor==null?null:completedDescriptor.clone();}
    private static double[] medoid(java.util.List<double[]> frames) {
        double best=Double.POSITIVE_INFINITY;double[] chosen=null;
        for(double[] candidate:frames) {
            double sum=0;for(double[] other:frames)sum+=distance(candidate,other);
            if(sum<best){best=sum;chosen=candidate;}
        }
        return chosen.clone(); // Keep an actual model output; never average or normalize embeddings.
    }
    private static double distance(double[] a,double[] b) {
        double sum=0;for(int i=0;i<128;i++)sum+=Math.pow(a[i]-b[i],2);return Math.sqrt(sum);
    }
}
