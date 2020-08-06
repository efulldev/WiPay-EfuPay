package ng.byteworks.org.landi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ActivationActivity extends AppCompatActivity {

    private Button act_btn;
    private EditText act_code_text;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        act_btn = findViewById(R.id.act_button);
        act_code_text = findViewById(R.id.act_code_editText);

        act_btn.setOnClickListener(v->{
            String code = act_code_text.getText().toString();
            if(code.isEmpty()){
                act_code_text.setError("Kindly Input a valid activation code");
            }else{
                checkTerminalAccess(code);
            }
        });
    }

    private void checkTerminalAccess(String code) {
        String tid = this.sharedPref.getString("terminalid", null);
        String mid = this.sharedPref.getString("merchantid", null);
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
        String url = _uri + "/checkTerminalAccess?terminal=" + tid + "&merchant=" + mid + "&activation_code="+code;
        Log.d("Activating Terminal ", url);
        Toast.makeText(ActivationActivity.this, "Validating Terminal... Please wait", Toast.LENGTH_SHORT).show();

        RequestQueue requestQueue = Volley.newRequestQueue(ActivationActivity.this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Update Response", response);
                        if (response.equals("true")) {
                            // grant user access to the app
                            Intent intent=new Intent(ActivationActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(ActivationActivity.this, "Activation Failed", Toast.LENGTH_LONG).show();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Term. Access Error", error.toString());
                        Toast.makeText(ActivationActivity.this, error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
