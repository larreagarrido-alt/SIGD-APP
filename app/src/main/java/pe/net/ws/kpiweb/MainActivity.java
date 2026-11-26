package pe.net.ws.kpiweb;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.FaceTiming;
import pe.net.ws.kpiweb.features.auth.SigdSplashBackdropView;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.navigation.*;

/** Post-authentication entry only. Real dashboards are registered in DashboardRouter. */
public class MainActivity extends AppCompatActivity {
    private SessionManager session;
    private SessionEntryCoordinator entry;
    private long profileRequestStarted;
    private SigdSplashBackdropView backdrop;
    private View loadingGroup;
    private View errorGroup;
    private ObjectAnimator beamAnimator;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);session=new SessionManager(this);
        if(!session.isLoggedIn()){AccountNavigation.login(this,false);return;}
        setContentView(R.layout.activity_main);
        View root=findViewById(R.id.main);
        new WindowInsetsControllerCompat(getWindow(),root).setAppearanceLightStatusBars(false);
        new WindowInsetsControllerCompat(getWindow(),root).setAppearanceLightNavigationBars(false);
        backdrop=findViewById(R.id.entry_backdrop);
        loadingGroup=findViewById(R.id.entry_loading_group);
        errorGroup=findViewById(R.id.entry_error_group);
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
                    session.saveSupervisorName(worker.getSupervisorNombre());
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
                    stopTransitionMotion();
                    if(target==null)AccountNavigation.openAccount(MainActivity.this,true);
                    else startActivity(new Intent(MainActivity.this,target.activity()));
                    overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
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
        if(loadingGroup!=null)loadingGroup.setVisibility(View.VISIBLE);
        if(errorGroup!=null)errorGroup.setVisibility(View.GONE);
        findViewById(R.id.entry_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.entry_retry).setEnabled(false);
        startTransitionMotion();
        profileRequestStarted=FaceTiming.now();
        int user=session.getUserId();
        entry.verify(ApiClient.getService().getProfile(session.getBearerToken()),user,
            ()->!isFinishing()&&!isDestroyed()&&session.isLoggedIn()&&user==session.getUserId());
    }

    private void startTransitionMotion(){
        if(backdrop!=null)backdrop.start();
        View beam=findViewById(R.id.entry_loader_beam);
        if(beam!=null&&beamAnimator==null){
            float d=getResources().getDisplayMetrics().density;
            beam.setTranslationX(-52f*d);
            beamAnimator=ObjectAnimator.ofFloat(beam,View.TRANSLATION_X,-52f*d,116f*d);
            beamAnimator.setDuration(860L);
            beamAnimator.setRepeatCount(ValueAnimator.INFINITE);
            beamAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            beamAnimator.start();
        }
    }

    private void stopTransitionMotion(){
        if(backdrop!=null)backdrop.stop();
        if(beamAnimator!=null){beamAnimator.cancel();beamAnimator=null;}
    }

    private void showUnavailable(){showUnavailable(R.string.nav_session_unavailable);}
    private void showUnavailable(int message){
        stopTransitionMotion();
        if(loadingGroup!=null)loadingGroup.setVisibility(View.GONE);
        if(errorGroup!=null)errorGroup.setVisibility(View.VISIBLE);
        findViewById(R.id.entry_progress).setVisibility(View.GONE);
        findViewById(R.id.entry_retry).setEnabled(true);
        ((TextView)findViewById(R.id.entry_status)).setText(message);
    }

    @Override protected void onStop(){stopTransitionMotion();if(entry!=null)entry.cancel();super.onStop();}
}
