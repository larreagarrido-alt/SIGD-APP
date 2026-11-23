package pe.net.ws.kpiweb.features.auth;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.ResponseBody;
import pe.net.ws.kpiweb.features.auth.models.FaceDeviceResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** No logging, backend message display or retry. Unknown mutations remain unknown. */
public final class FaceRegistrationRequest {
    public enum Stage { REGISTER, CONFIRM }
    public static final class Diagnostic {
        public final Stage stage;
        public final int http;
        public final String backendCode;
        public final String localCode;
        public final boolean uncertain;
        public Diagnostic(Stage stage,int http,String backendCode,String localCode,boolean uncertain){
            this.stage=stage;this.http=http;
            this.backendCode=safeCode(backendCode);this.localCode=safeCode(localCode);this.uncertain=uncertain;
        }
        public String display(){
            String text="Etapa: "+(stage==Stage.REGISTER?"inicio del registro":"confirmación del registro")+".\nHTTP: "+(http==0?"sin respuesta":http)
                +". error_code: "+(backendCode.isEmpty()?"no disponible":backendCode)+".";
            if(!localCode.isEmpty())text+="\nDiagnóstico local: "+localCode+".";
            if("FRESH_PASSWORD_REQUIRED".equals(backendCode))text+="\nVuelve a ingresar con contraseña y autoriza antes de cinco minutos. La sesión por huella no permite registrar una clave facial.";
            if("INVALID_TOKEN".equals(backendCode)||"UNAUTHORIZED".equals(backendCode))text+="\nLa sesión no está autorizada. Vuelve a ingresar con contraseña.";
            if(uncertain)text+="\nResultado no confirmado. No repitas la autorización ni borres los datos de la app. Primero hay que comprobar el estado en el servidor.";
            return text;
        }
    }
    public interface Listener {
        void success(FaceDeviceResponse value,int http);
        void failure(Diagnostic diagnostic);
    }
    private FaceRegistrationRequest() {}
    private static String safeCode(String value){return value!=null&&value.matches("[A-Z][A-Z0-9_]{0,63}")?value:"";}
    public static void enqueue(Call<ResponseBody> call,Stage stage,Listener listener){
        call.enqueue(new Callback<ResponseBody>(){
            @Override public void onResponse(Call<ResponseBody> call,Response<ResponseBody> response){
                if(call.isCanceled()){
                    ResponseBody discarded=response.isSuccessful()?response.body():response.errorBody();
                    if(discarded!=null)discarded.close();return;
                }
                String code="";JsonObject json=null;
                try(ResponseBody body=response.isSuccessful()?response.body():response.errorBody()){
                    if(body!=null){
                        body.source().request(8193);
                        if(body.source().buffer().size()<=8192){
                            String raw=body.string();JsonElement parsed=JsonParser.parseString(raw);
                            if(parsed.isJsonObject()){
                                json=parsed.getAsJsonObject();JsonElement item=json.get("error_code");
                                if(item!=null&&item.isJsonPrimitive()&&item.getAsJsonPrimitive().isString())code=safeCode(item.getAsString());
                            }
                        }
                    }
                }catch(IOException|RuntimeException ignored){/* Never expose bodies or exception messages. */}
                FaceDeviceResponse value=null;
                if(response.isSuccessful()&&json!=null){
                    try {
                        if("success".equals(json.get("status").getAsString())&&json.has("data")&&json.get("data").isJsonObject()){
                            value=new Gson().fromJson(json.get("data"),FaceDeviceResponse.class);
                        }
                    }catch(RuntimeException ignored){}
                }
                if(value!=null){listener.success(value,response.code());return;}
                boolean uncertain=response.code()>=500||response.code()==408||response.isSuccessful()||response.code()<400||code.isEmpty();
                listener.failure(new Diagnostic(stage,response.code(),code,json==null?"RESPUESTA_NO_VALIDA":"",uncertain));
            }
            @Override public void onFailure(Call<ResponseBody> call,Throwable failure){
                if(!call.isCanceled())listener.failure(new Diagnostic(stage,0,"","CONEXION_SIN_RESPUESTA",true));
            }
        });
    }
}
