package mi5.pushtest;


        import java.io.IOException;

        import android.app.Activity;
        import android.app.ProgressDialog;
        import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.text.TextUtils;
        import android.view.View;
        import android.widget.EditText;
        import android.widget.Toast;

        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.GooglePlayServicesUtil;
        import com.google.android.gms.gcm.GoogleCloudMessaging;
        import com.loopj.android.http.AsyncHttpClient;
        import com.loopj.android.http.AsyncHttpResponseHandler;
        import com.loopj.android.http.RequestParams;

        import org.apache.http.conn.scheme.Scheme;
        import org.apache.http.conn.ssl.SSLSocketFactory;

        import javax.net.ssl.SSLContext;

public class MainActivity extends Activity {
    ProgressDialog prgDialog;
    RequestParams params = new RequestParams();
    GoogleCloudMessaging gcmObj;
    Context applicationContext;
    String regId = "";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    AsyncTask<Void, Void, String> createRegIdTask;

    public static final String REG_ID = "regId";
    public static final String EMAIL_ID = "eMailId";
    public static final String UPSTREAM_MESSAGE_ID = "upstreamMessageId";
    EditText emailET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applicationContext = getApplicationContext();
        emailET = (EditText) findViewById(R.id.email);

        prgDialog = new ProgressDialog(this);
        // Set Progress Dialog Text
        prgDialog.setMessage("Please wait...");
        // Set Cancelable as False
        prgDialog.setCancelable(false);

        SharedPreferences prefs = getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);
        String registrationId = prefs.getString(REG_ID, "");
        if (!TextUtils.isEmpty(registrationId)) {
            Intent i = new Intent(applicationContext, HomeActivity.class);
            i.putExtra("regId", registrationId);
            startActivity(i);
            finish();
        }
    }

    public void RegisterUser(View view) {
        String emailID = emailET.getText().toString();

        if (!TextUtils.isEmpty(emailID) && Utility.validate(emailID)) {
            if (checkPlayServices()) {
                registerInBackground(emailID);
            }
        }
        // When Email is invalid
        else {
            Toast.makeText(applicationContext, "Please enter valid email",
                    Toast.LENGTH_LONG).show();
        }

    }

    private void registerInBackground(final String emailID) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcmObj == null) {
                        gcmObj = GoogleCloudMessaging
                                .getInstance(applicationContext);
                    }
                    regId = gcmObj
                            .register(ApplicationConstants.GOOGLE_PROJ_ID);
                    msg = "Registration ID :" + regId;

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                if (!TextUtils.isEmpty(regId)) {
                    storeRegIdinSharedPref(applicationContext, regId, emailID);
                    Toast.makeText(
                            applicationContext,
                            "Registered with GCM Server successfully.\n\n"
                                    + msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            applicationContext,
                            "Reg ID Creation Failed.\n\nEither you haven't enabled Internet or GCM server is busy right now. Make sure you enabled Internet and try registering again after some time."
                                    + msg, Toast.LENGTH_LONG).show();
                }
            }
        }.execute(null, null, null);
    }

    private void storeRegIdinSharedPref(Context context, String regId,
                                        String emailID) {
        SharedPreferences prefs = getSharedPreferences("UserDetails",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(REG_ID, regId);
        editor.putString(EMAIL_ID, emailID);
        editor.commit();
        storeRegIdInServer();

    }

    private int getNewUpstreamMessageId() {
        SharedPreferences prefs = getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
        int id = prefs.getInt(UPSTREAM_MESSAGE_ID, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(UPSTREAM_MESSAGE_ID, ++id);
        editor.commit();
        return id;
    }

    private void storeRegIdInServer() {
        try {
            Bundle data = new Bundle();
            data.putString(ApplicationConstants.UPSTREAM_REGID_KEY, regId);
            gcmObj.send(ApplicationConstants.GOOGLE_PROJ_ID + "@gcm.googleapis.com", String.valueOf(getNewUpstreamMessageId()), data);
            Toast.makeText(this, "Sent regId to backend via upstream message.", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(applicationContext,
                    HomeActivity.class);
            i.putExtra("regId", regId);
            startActivity(i);
            finish();
        }
        catch (IOException ex) {
            Toast.makeText(this, "Sending regId to backend via upstream failed!", Toast.LENGTH_LONG).show();
            regId = "";
            storeRegIdinSharedPref(this, "", "");
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(
                        applicationContext,
                        "This device doesn't support Play services, App will not work normally",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        } else {
            Toast.makeText(
                    applicationContext,
                    "This device supports Play services, App will work normally",
                    Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }
}