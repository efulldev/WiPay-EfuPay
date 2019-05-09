package ng.byteworks.org.landi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

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
        bizText.setText(bizName);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        },5000);
    }
}
