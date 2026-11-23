package pe.net.ws.kpiweb.features.auth;

import android.content.SharedPreferences;

/** Separate preferences: only stage/status/codes, never tokens, keys, signatures or faces. */
public final class FaceRegistrationAttempt {
    private final SharedPreferences prefs;
    public FaceRegistrationAttempt(SharedPreferences prefs){this.prefs=prefs;}
    public boolean unresolved(){return prefs.contains("stage");}
    public boolean dispatched(FaceRegistrationRequest.Stage stage){
        return record(new FaceRegistrationRequest.Diagnostic(stage,0,"","PETICION_SIN_CONFIRMAR",true));
    }
    public boolean record(FaceRegistrationRequest.Diagnostic diagnostic){
        return prefs.edit().putString("stage",diagnostic.stage.name()).putInt("http",diagnostic.http)
            .putString("backend_code",diagnostic.backendCode).putString("local_code",diagnostic.localCode).commit();
    }
    public boolean resolved(){return prefs.edit().clear().commit();}
    public FaceRegistrationRequest.Diagnostic diagnostic(){
        FaceRegistrationRequest.Stage stage;
        try{stage=FaceRegistrationRequest.Stage.valueOf(prefs.getString("stage","REGISTER"));}
        catch(IllegalArgumentException invalid){stage=FaceRegistrationRequest.Stage.REGISTER;}
        return new FaceRegistrationRequest.Diagnostic(stage,prefs.getInt("http",0),prefs.getString("backend_code",""),prefs.getString("local_code",""),true);
    }
}
