package ng.byteworks.org.landi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import ng.byteworks.org.landi.BuildConfig;

public class SplashActivity extends AppCompatActivity {

    Handler handler;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = sharedPref.edit();

        TextView textView = findViewById(R.id.appVersionText);
        textView.setText("v"+BuildConfig.VERSION_NAME);


        TextView bizText = (TextView)findViewById(R.id.bizNameTextViewSplash);
        String bizName = this.sharedPref.getString("businessName", "");
        String tid = this.sharedPref.getString("terminalid", null);
        String mid = this.sharedPref.getString("merchantid", null);
        bizText.setText(bizName);

            handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // if terminal has been configured, perform check with Efull servers,
                    // find out if the terminal has been deactivated
                    if(tid != null && mid != null) {
                        checkTerminalAccess(tid, mid);
                    }else {
                        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }, 5000);
    }

    private void checkTerminalAccess(String tid, String mid) {
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
        String url = _uri + "/checkTerminalAccess?terminal=" + tid + "&merchant=" + mid;
        Log.d("Checking Term. Access ", url);

        RequestQueue requestQueue = Volley.newRequestQueue(SplashActivity.this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Update Response", response);
                        if (response.equals("true")) {
                            // grant user access to the app
                            Intent intent=new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // navigate to Access Override Screen
                            Intent intent=new Intent(SplashActivity.this, ActivationActivity.class);
                            startActivity(intent);
                            finish();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Term. Access Error", error.toString());
                        Toast.makeText(SplashActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );

//      set retry policy to determine how long volley should wait before resending a failed request
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        add jsonObjectRequest to the queue
        requestQueue.add(stringRequest);
    }

}
