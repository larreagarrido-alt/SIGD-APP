package pe.net.ws.kpiweb.features.auth;
import org.junit.Test;
import static org.junit.Assert.*;
public class PadTemporalGateTest {
    @Test public void duplicateTimestampIsRejectedEvenAfterFirstTimestamp(){PadTemporalGate g=new PadTemporalGate();g.observe(0,.99,.01);g.observe(200,.99,.01);assertEquals(PadTemporalGate.State.REJECT,g.observe(200,.99,.01));}
    @Test public void backwardsTimestampWithinWindowIsRejected(){PadTemporalGate g=new PadTemporalGate();g.observe(0,.99,.01);g.observe(400,.99,.01);assertEquals(PadTemporalGate.State.REJECT,g.observe(300,.99,.01));}
    @Test public void invalidSampleMakesRejectionTerminal(){PadTemporalGate g=new PadTemporalGate();g.observe(0,Double.NaN,.01);for(int i=1;i<7;i++)assertEquals(PadTemporalGate.State.REJECT,g.observe(i*250,.99,.01));}
    @Test public void spoofRejectionCannotLaterRecoverOnSameCapture(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<3;i++)g.observe(i*200,.01,.99);g.interrupt();assertEquals(PadTemporalGate.State.REJECT,g.observe(800,.99,.01));}
    @Test public void interruptionRequiresAnEntireNewStreak(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<4;i++)g.observe(i*200,.99,.01);g.interrupt();for(int i=0;i<4;i++)assertEquals(PadTemporalGate.State.COLLECTING,g.observe(800+i*200,.99,.01));assertEquals(PadTemporalGate.State.PASS,g.observe(1600,.99,.01));}
    @Test public void interruptionDoesNotRestartCaptureTimeBudget(){PadTemporalGate g=new PadTemporalGate();g.observe(0,.99,.01);g.interrupt();assertEquals(PadTemporalGate.State.REJECT,g.observe(15001,.99,.01));}
    @Test(expected=IllegalStateException.class) public void interruptionInvalidatesPreviouslyPassedProof(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<5;i++)g.observe(i*200,.99,.01);g.interrupt();g.signedCapture("[]");}
    @Test public void fiveHighScoresNeedElapsedTime(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<4;i++)assertEquals(PadTemporalGate.State.COLLECTING,g.observe(i*200,.99,.01));assertEquals(PadTemporalGate.State.PASS,g.observe(800,.99,.01));}
    @Test public void rapidRepeatedFramesAreNotEnough(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<6;i++)assertEquals(PadTemporalGate.State.COLLECTING,g.observe(i*10,.999,.001));}
    @Test public void threeSpoofScoresReject(){PadTemporalGate g=new PadTemporalGate();g.observe(0,.05,.95);g.observe(200,.04,.96);assertEquals(PadTemporalGate.State.REJECT,g.observe(400,.01,.99));}
    @Test public void uncertainFrameResetsConsecutiveAcceptance(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<4;i++)g.observe(i*200,.99,.01);g.observe(800,.8,.2);assertEquals(PadTemporalGate.State.COLLECTING,g.observe(1000,.99,.01));}
    @Test public void elapsedBudgetRejectsIndefiniteUncertainty(){PadTemporalGate g=new PadTemporalGate();g.observe(0,.8,.2);assertEquals(PadTemporalGate.State.REJECT,g.observe(15001,.99,.01));}
    @Test public void invalidProbabilitiesNeverPass(){assertEquals(PadTemporalGate.State.REJECT,new PadTemporalGate().observe(0,Double.NaN,.5));assertEquals(PadTemporalGate.State.REJECT,new PadTemporalGate().observe(0,1,1));}
    @Test public void backwardsClockRejects(){PadTemporalGate g=new PadTemporalGate();g.observe(1000,.99,.01);assertEquals(PadTemporalGate.State.REJECT,g.observe(999,.99,.01));}
    @Test(expected=IllegalStateException.class) public void unfinishedCaptureCannotProduceLoginProof(){new PadTemporalGate().signedCapture("[]");}
    @Test(expected=IllegalStateException.class) public void laterUncertaintyInvalidatesEarlierPass(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<5;i++)g.observe(i*250,.99,.01);g.observe(1500,.8,.2);g.signedCapture("[]");}
    @Test public void signedProofUsesActualStreakMinimumAndElapsedTime(){PadTemporalGate g=new PadTemporalGate();for(int i=0;i<5;i++)g.observe(i*250,.99-i*.001,.01+i*.001);com.google.gson.JsonObject p=com.google.gson.JsonParser.parseString(g.signedCapture("[0.1]")).getAsJsonObject();assertEquals(2,p.get("version").getAsInt());assertEquals(5,p.getAsJsonObject("pad").get("samples").getAsInt());assertEquals(1000,p.getAsJsonObject("pad").get("duration_ms").getAsLong());assertEquals(.986,p.getAsJsonObject("pad").get("real_min").getAsDouble(),.000001);}
}
