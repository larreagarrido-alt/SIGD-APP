package pe.net.ws.kpiweb.navigation;

import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Collections;
import pe.net.ws.kpiweb.AccountActivity;
import pe.net.ws.kpiweb.MainActivity;
import pe.net.ws.kpiweb.R;
import pe.net.ws.kpiweb.core.network.*;
import pe.net.ws.kpiweb.core.session.*;
import pe.net.ws.kpiweb.features.auth.LoginActivity;
import retrofit2.*;

/** Shared account entry/logout for future modules. Does not decide dashboard permissions. */
public final class AccountNavigation {
    public static final String DASHBOARD_UNAVAILABLE="dashboard_unavailable";
    public static final String FACE_LOGIN_STARTED_NANOS="face_login_started_nanos";
    private AccountNavigation(){}
    public static void afterAuthentication(AppCompatActivity activity){
        afterAuthentication(activity,0);
    }
    public static void afterAuthentication(AppCompatActivity activity,long faceStartedNanos){
        Intent intent=new Intent(activity,MainActivity.class);
        if(faceStartedNanos!=0)intent.putExtra(FACE_LOGIN_STARTED_NANOS,faceStartedNanos);
        activity.startActivity(intent);activity.finish();
    }
    public static void openAccount(Context context){openAccount(context,false);}
    public static void openAccount(Context context,boolean noDashboard){
        Intent intent=new Intent(context,AccountActivity.class).putExtra(DASHBOARD_UNAVAILABLE,noDashboard);
        if(!(context instanceof android.app.Activity))intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    public static void login(AppCompatActivity activity,boolean forget){
        activity.startActivity(new Intent(activity,LoginActivity.class).putExtra("forget_android_access",forget)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));activity.finish();
    }
    public static void signOut(AppCompatActivity activity){
        SessionManager session=new SessionManager(activity);
        if(new AndroidDeviceStore(activity).hasCredential()){
            new MaterialAlertDialogBuilder(activity).setTitle(R.string.auth_sign_out).setMessage(R.string.auth_logout_keep_message)
                .setPositiveButton(R.string.auth_sign_out,(d,w)->finishSignOut(activity,session,false,false))
                .setNeutralButton(R.string.auth_forget_device,(d,w)->finishSignOut(activity,session,true,false))
                .setNegativeButton(R.string.auth_cancel,null).show();
        }else finishSignOut(activity,session,false,true);
    }
    /** Use after a module has already shown its own sign-out confirmation. */
    public static void signOutConfirmed(AppCompatActivity activity){
        finishSignOut(activity,new SessionManager(activity),false,false);
    }
    private static void finishSignOut(AppCompatActivity activity,SessionManager session,boolean forget,boolean clearAll){
        ApiClient.getService().logout(session.getBearerToken(),Collections.emptyMap()).enqueue(new Callback<ApiResponse<Object>>(){
            public void onResponse(Call<ApiResponse<Object>> c,Response<ApiResponse<Object>> r){}
            public void onFailure(Call<ApiResponse<Object>> c,Throwable failure){}
        });
        if(clearAll)session.clear();else session.clearActiveSession();
        login(activity,forget);
    }
}
