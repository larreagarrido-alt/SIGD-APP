package pe.net.ws.kpiweb;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import pe.net.ws.kpiweb.core.session.SessionManager;
import pe.net.ws.kpiweb.features.auth.FaceEnrollmentActivity;
import pe.net.ws.kpiweb.features.auth.models.LoginResponseData;
import pe.net.ws.kpiweb.core.network.ApiClient;
import pe.net.ws.kpiweb.navigation.AccountNavigation;
import pe.net.ws.kpiweb.navigation.SessionEntryCoordinator;

/** Account/security screen, never a dashboard. Available from all future modules. */
public class AccountActivity extends AppCompatActivity {
    private SessionManager session;
    private SessionEntryCoordinator entry;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);session=new SessionManager(this);
        if(!session.isLoggedIn()){AccountNavigation.login(this,false);return;}
        setContentView(R.layout.activity_account);
        TextView welcome=findViewById(R.id.tv_welcome_name);
        String name=session.getFullName().isEmpty()?session.getUsername():session.getFullName();
        welcome.setText(getString(R.string.nav_account_welcome,name));
        findViewById(R.id.account_dashboard_notice).setVisibility(getIntent().getBooleanExtra(AccountNavigation.DASHBOARD_UNAVAILABLE,false)?View.VISIBLE:View.GONE);
        findViewById(R.id.btn_face_enrollment).setOnClickListener(v->startActivity(new Intent(this,FaceEnrollmentActivity.class)));
        findViewById(R.id.btn_logout).setOnClickListener(v->AccountNavigation.signOut(this));
        findViewById(R.id.account_retry).setOnClickListener(v->verifyAccount());
        entry=new SessionEntryCoordinator(new SessionEntryCoordinator.Listener(){
            public void verified(LoginResponseData profile){
                findViewById(R.id.account_progress).setVisibility(View.GONE);
                findViewById(R.id.account_status).setVisibility(View.GONE);
                findViewById(R.id.account_retry).setVisibility(View.GONE);
                findViewById(R.id.btn_face_enrollment).setEnabled(true);
            }
            public void unauthorized(){session.clearActiveSession();AccountNavigation.login(AccountActivity.this,false);}
            public void forbidden(){showVerificationError(R.string.nav_access_forbidden);}
            public void unavailable(){showVerificationError(R.string.nav_account_unavailable);}
        });
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.account_root),(view,insets)->{
            androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;
        });
    }
    @Override protected void onResume(){super.onResume();if(entry!=null)verifyAccount();}
    private void verifyAccount(){
        if(!session.isLoggedIn()){AccountNavigation.login(this,false);return;}
        String token=session.getAccessToken();
        findViewById(R.id.account_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.account_status).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.account_status)).setText(R.string.nav_verifying_session);
        findViewById(R.id.account_retry).setEnabled(false);
        findViewById(R.id.account_retry).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_face_enrollment).setEnabled(false);
        entry.verify(ApiClient.getService().getProfile(session.getBearerToken()),session.getUserId(),
            ()->!isFinishing()&&!isDestroyed()&&token.equals(session.getAccessToken()));
    }
    private void showVerificationError(int message){
        findViewById(R.id.account_progress).setVisibility(View.GONE);
        findViewById(R.id.account_retry).setEnabled(true);
        ((TextView)findViewById(R.id.account_status)).setText(message);
    }
    @Override protected void onStop(){if(entry!=null)entry.cancel();super.onStop();}
}
