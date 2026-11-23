package pe.net.ws.kpiweb.features.auth;
/** Conservative local screening only. Threshold is provisional, never a production PAD certificate. */
public final class PadTemporalGate {
    public enum State{COLLECTING,PASS,REJECT}
    private int good,bad;private long first=-1,lastObserved=-1,firstGood=-1,lastGood=-1;private double minReal=1;private boolean passed,rejected;
    /** Losing the face/quality discards the streak without extending this capture's time budget. */
    public void interrupt(){good=0;bad=0;firstGood=-1;lastGood=-1;minReal=1;passed=false;}
    private State reject(){interrupt();rejected=true;return State.REJECT;}
    public State observe(long millis,double real,double spoof){
        if(rejected)return State.REJECT;
        passed=false;
        if(millis<0||!Double.isFinite(real+spoof)||real<0||real>1||spoof<0||spoof>1||Math.abs(real+spoof-1)>.001)return reject();
        if(first<0)first=millis;
        if(millis<=lastObserved||millis-first>15000)return reject();
        lastObserved=millis;
        if(real>=.95){if(good==0){firstGood=millis;minReal=1;}good++;bad=0;lastGood=millis;minReal=Math.min(minReal,real);}
        else{good=0;firstGood=-1;lastGood=-1;minReal=1;bad=spoof>=.5?bad+1:0;}
        if(bad>=3)return reject();
        passed=good>=5&&millis-firstGood>=800;
        return passed?State.PASS:State.COLLECTING;
    }
    public String signedCapture(String descriptor){
        if(!passed||good<5||lastGood-firstGood<800)throw new IllegalStateException("PAD capture incomplete");
        com.google.gson.JsonObject payload=new com.google.gson.JsonObject();payload.addProperty("version",2);
        payload.add("descriptor",com.google.gson.JsonParser.parseString(descriptor));
        com.google.gson.JsonObject pad=new com.google.gson.JsonObject();pad.addProperty("model","MiniFASNetV1SE+V2-min");
        pad.addProperty("samples",good);pad.addProperty("duration_ms",lastGood-firstGood);pad.addProperty("real_min",minReal);
        payload.add("pad",pad);return payload.toString();
    }
}
