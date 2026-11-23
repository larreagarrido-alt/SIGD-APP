package pe.net.ws.kpiweb;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.features.auth.FaceTiming;
import pe.net.ws.kpiweb.navigation.*;

/** Post-authentication entry only. Real dashboards are registered in DashboardRouter. */
public class MainActivity extends AppCompatActivity {
    private SessionManager session;
    private SessionEntryCoordinator entry;
    private long profileRequestStarted;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);session=new SessionManager(this);
        if(!session.isLoggedIn()){AccountNavigation.login(this,false);return;}
        setContentView(R.layout.activity_main);
        findViewById(R.id.entry_retry).setOnClickListener(v->resolveDestination());
        findViewById(R.id.entry_account).setOnClickListener(v->AccountNavigation.openAccount(this));
        findViewById(R.id.btn_logout).setOnClickListener(v->AccountNavigation.signOut(this));
        entry=new SessionEntryCoordinator(new SessionEntryCoordinator.Listener(){
            public void verified(LoginResponseData profile){
                FaceTiming.log(MainActivity.this,"profile_server",profileRequestStarted);
                long faceStarted=getIntent().getLongExtra(AccountNavigation.FACE_LOGIN_STARTED_NANOS,0);
                if(faceStarted!=0){FaceTiming.log(MainActivity.this,"face_login_total",faceStarted);getIntent().removeExtra(AccountNavigation.FACE_LOGIN_STARTED_NANOS);}
                LoginResponseData.WorkerInfo worker=profile.getWorker();
                if(worker!=null&&worker.getIdTrabajador()>0){
                    String name=worker.getNombreCompleto();
                    session.saveSession(session.getAccessToken(),"",profile.getUser().getIdUsuario(),
                        profile.getUser().getUsername(),profile.getUser().getIdRol(),worker.getIdTrabajador(),
                        name==null||name.isEmpty()?profile.getUser().getUsername():name,
                        worker.getCargoNombre(),profile.getUser().getRolNombre(),worker.getAgenciaNombre(),worker.getTipo());
                    if(worker.getIdCargo()!=null)session.updateVerifiedKpiLaborFields(
                        worker.getIdTrabajador(),worker.getIdCargo(),worker.getCargoNombre(),worker.getTipo());
                    session.saveWorkerContact(worker.getEmail(),worker.getFotoPerfil(),worker.getEstado());
                    session.saveVerifiedDni(worker.getIdTrabajador(),worker.getDni());
                    if(worker.getSaldoPuntos()!=null)session.savePointsBalance(worker.getSaldoPuntos());
                }
                DashboardRouter.Destination target=DashboardRouter.resolveHome(profile);
                pe.net.ws.kpiweb.features.kpi.sync.KpiFormRecovery.refresh(MainActivity.this, null);
                try{
                    if(target==null && DashboardRouter.requiresAdministrativeGrant(profile)) {
                        showUnavailable(R.string.nav_admin_grant_missing);
                        return;
                    }
                    if(target==null)AccountNavigation.openAccount(MainActivity.this,true);
                    else startActivity(new Intent(MainActivity.this,target.activity()));
                    finish();
                }catch(ActivityNotFoundException missing){showUnavailable();}
            }
            public void unauthorized(){FaceTiming.log(MainActivity.this,"profile_server",profileRequestStarted);session.clearActiveSession();AccountNavigation.login(MainActivity.this,false);}
            public void forbidden(){FaceTiming.log(MainActivity.this,"profile_server",profileRequestStarted);showUnavailable(R.string.nav_access_forbidden);}
            public void unavailable(){FaceTiming.log(MainActivity.this,"profile_server",profileRequestStarted);showUnavailable();}
        });
    }
    @Override protected void onResume(){super.onResume();if(entry!=null)resolveDestination();}
    private void resolveDestination(){
        if(!session.isLoggedIn()){AccountNavigation.login(this,false);return;}
        String token=session.getAccessToken();
        findViewById(R.id.entry_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.entry_retry).setEnabled(false);
        ((TextView)findViewById(R.id.entry_status)).setText(R.string.nav_verifying_session);
        profileRequestStarted=FaceTiming.now();
        entry.verify(ApiClient.getService().getProfile(session.getBearerToken()),session.getUserId(),
            ()->!isFinishing()&&!isDestroyed()&&token.equals(session.getAccessToken()));
    }
    private void showUnavailable(){showUnavailable(R.string.nav_session_unavailable);}
    private void showUnavailable(int message){
        findViewById(R.id.entry_progress).setVisibility(View.GONE);
        findViewById(R.id.entry_retry).setEnabled(true);
        ((TextView)findViewById(R.id.entry_status)).setText(message);
    }
    @Override protected void onStop(){if(entry!=null)entry.cancel();super.onStop();}
}
