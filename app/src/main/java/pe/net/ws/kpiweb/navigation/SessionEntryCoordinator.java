package pe.net.ws.kpiweb.navigation;

import java.util.function.Supplier;
import pe.net.ws.kpiweb.core.network.ApiResponse;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Verifies the current session before selecting a module; no login, token issuance or biometric writes. */
public final class SessionEntryCoordinator {
    public interface Listener {
        void verified(LoginResponseData profile);
        void unauthorized();
        void forbidden();
        void unavailable();
    }
    private final Listener listener;
    private Call<ApiResponse<LoginResponseData>> pending;
    private int generation;
    public SessionEntryCoordinator(Listener listener){this.listener=listener;}
    public void verify(Call<ApiResponse<LoginResponseData>> call,int expectedUserId,Supplier<Boolean> stillCurrent){
        cancel();int run=generation;pending=call;
        call.enqueue(new Callback<ApiResponse<LoginResponseData>>(){
            private boolean current(){return run==generation&&!call.isCanceled()&&stillCurrent.get();}
            public void onResponse(Call<ApiResponse<LoginResponseData>> c,Response<ApiResponse<LoginResponseData>> r){
                if(r.errorBody()!=null)r.errorBody().close();
                if(!current())return;pending=null;
                if(r.code()==401){listener.unauthorized();return;}
                if(r.code()==403){listener.forbidden();return;}
                ApiResponse<LoginResponseData> body=r.body();
                if(!r.isSuccessful()||body==null||!body.isSuccessful()||body.getData()==null){listener.unavailable();return;}
                LoginResponseData profile=body.getData();
                if(profile.getUser()==null||profile.getUser().getIdUsuario()<=0||profile.getUser().getIdUsuario()!=expectedUserId){listener.unauthorized();return;}
                if(profile.getPermissions()==null){listener.unavailable();return;}
                listener.verified(profile);
            }
            public void onFailure(Call<ApiResponse<LoginResponseData>> c,Throwable failure){if(current()){pending=null;listener.unavailable();}}
        });
    }
    public void cancel(){generation++;if(pending!=null)pending.cancel();pending=null;}
}
