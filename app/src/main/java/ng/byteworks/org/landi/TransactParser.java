package ng.byteworks.org.landi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.arke.sdk.util.epms.SqliteDatabase;
import com.arke.sdk.util.epms.Transaction;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import ng.byteworks.org.landi.utils.ActionCompleteCallback;
import ng.byteworks.org.landi.utils.Controller;
import ng.byteworks.org.landi.utils.TransactionCompleteCallback;
import ng.byteworks.org.landi.utils.TransactionResponse;
import ng.byteworks.org.landi.utils.mainDatabase;
import ng.byteworks.org.landi.utils.redundantDatabase;
import ng.byteworks.org.landi.utils.redundantTransaction;
import com.arke.sdk.view.EPMSAdminActivity;
import static ng.byteworks.org.landi.SetupActivity.encodeUrlEscaped;


public class TransactParser extends AppCompatActivity {

    private static final String TAG = "TransactParser";
    private static Context context;
    private Timer respTimer;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor mEditor;
    private Button confirmPaymentBtn;

    private Integer transType;
    private Integer batchNo;
    private Integer seqNo;
    private Integer amount;
    private String details = "";

    private String packageName = "NOT SET";
    private String appName = "NOT SET";
    private String secretKey = null;
    private String publicKey = null;

    private mainDatabase mDatabase;
    private SqliteDatabase epmsDatabase;
    private redundantDatabase mRedDatabase;
    private TextView amountText;
    private TextView procText;

    public final static String EXTRA_PURCHASE_ACTION = "makePayment";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transact_parser);
        respTimer = new Timer();
        context = getApplicationContext();
        confirmPaymentBtn = findViewById(R.id.confirmPaymentBtn);
        amountText = findViewById(R.id.amountView);
        procText = findViewById(R.id.textView2);


//        get parameters from the calling activity
        Intent intent = getIntent();
        if(null != intent) {
            String action = intent.getStringExtra("action");
            sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mEditor = sharedPref.edit();
            mDatabase = new mainDatabase(getApplicationContext());
            epmsDatabase = new SqliteDatabase(getApplicationContext());
            mRedDatabase = new redundantDatabase(getApplicationContext());

            confirmPaymentBtn.setEnabled(false);
            confirmPaymentBtn.setText("Validating...");
            confirmPaymentBtn.setBackgroundColor(getResources().getColor(R.color.border_gray_color));
            confirmPaymentBtn.setTextColor(getResources().getColor(R.color.grey_transparent));
            procText.setVisibility(View.GONE);

            this.packageName = intent.getStringExtra("domainName");
            this.appName = intent.getStringExtra("appName");
            this.secretKey = /*"EFU-SEC-2d092d44744024d46749a0195f0040a8";*/intent.getStringExtra("secretKey");
            this.publicKey = /*"EFU-PUB-988c414f5c0f8633e274b88cf39b631a";*/intent.getStringExtra("publicKey");
            this.details = intent.getStringExtra("payDetails");
            Log.d("KEYS=>", this.secretKey+" "+this.publicKey);

            // check if client app is valid and has permission to carry out transaction
            checkClientAppPerm((res) -> {
                if(res.equals("true")) {

                    confirmPaymentBtn.setEnabled(true);
                    confirmPaymentBtn.setText("Proceed");
                    confirmPaymentBtn.setBackgroundColor(getResources().getColor(R.color.holo_orange_dark));
                    confirmPaymentBtn.setTextColor(getResources().getColor(R.color.white));
                    procText.setVisibility(View.VISIBLE);

                    switch (action) {
                        case EXTRA_PURCHASE_ACTION:
                            getBatchSeqNos();
                            this.transType = intent.getIntExtra("transType", 1);
                            this.amount = intent.getIntExtra("amount", 0);
                            final String formattedString = "You are about to be debited " + formatAmount(this.amount) + " from your bank account.";
                            amountText.setText(formattedString);
                            break;
                        default:
                            break;
                    }
                }else{
                    amountText.setText("Permission Denied\nYou are Unauthorized to perform this operation");
                    confirmPaymentBtn.setEnabled(false);
                    confirmPaymentBtn.setVisibility(View.GONE);
                    Toast.makeText(context, "You are Unauthorized to perform this operation", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkClientAppPerm( ActionCompleteCallback callback) {
        amountText.setText("Please wait...");
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api");
        _uri = _uri + "/validateClient?secret_key="+this.secretKey+"&public_key="+this.publicKey+"&package_name="+this.packageName;
        getServerResponse(_uri, (response) -> {
            callback.done(response);
        });
    }

    private static void getServerResponse(String url, ActionCompleteCallback callback){
        String escapedUrl = encodeUrlEscaped(url);
        Log.d("Cloud DB URI", escapedUrl);
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                escapedUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Cloud DB Response", response);
                        callback.done(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Cloud DB Error", error.toString());
                        Toast.makeText(context, "Communication Error", Toast.LENGTH_LONG).show();
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

    public void getBatchSeqNos(){
        Integer batchNo = sharedPref.getInt(getString(R.string.batch_no), 1);
        Integer seqNo = sharedPref.getInt(getString(R.string.seq_no), 1);
        this.batchNo = batchNo;
        this.seqNo = seqNo;
    }

    public void closeActivity(View view){
        Intent _data = new Intent();
        _data.putExtra("response", "Operation cancelled by the user");
        setResult(Activity.RESULT_CANCELED, _data);
        finish();
    }

    public void makePayment(View view) {
        Intent intent = new Intent(this, com.arke.sdk.view.EPMSActivity.class);
        intent.putExtra("trantype", "" + this.transType);
        intent.putExtra("batchno", "" + this.batchNo);
        intent.putExtra("seqno", "" + this.seqNo);
        intent.putExtra("amount", ""+this.amount);
        startActivityForResult(intent, 0);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check that it is the EPMSActivity with an OK result
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                // get String data from Intent
                Transaction newTransaction = (Transaction) data.getSerializableExtra("response");
                Log.d("TransactParser", "stan = " + newTransaction.getStan());
                if(newTransaction.getTranstype() == 1) {
                    mDatabase.saveEftTransaction(newTransaction);
                    mDatabase.saveTransactionOrigin(newTransaction.getRefno(), ""+appName);
                    epmsDatabase.saveEftTransaction(newTransaction);
                }

                String headerLogoPath = sharedPref.getString("headerlogo", null);
                if(headerLogoPath != null){
                    try {
                        EPMSAdminActivity.printReceipt(newTransaction, this, headerLogoPath);
                    } catch (Exception e) {
                        Log.e("MainActivity", e.getLocalizedMessage());
                    }
                }else{
                    Toast.makeText(this, "Please configure receipt logo", Toast.LENGTH_SHORT).show();
                }
                if (newTransaction.getMode() == com.arke.sdk.util.epms.Constant.CHIP) {
                    EPMSAdminActivity.removeCard(newTransaction, this, TransactParser.this);
                }
                // send transaction data to Efull Terminal Manager Server
                TransactionResponse response = new TransactionResponse();
                response.setDetails(this.details);
                response.setTransaction(newTransaction);
                Controller controller = new Controller(this);
                controller.sendTransaction(response, (res)-> {
                    Log.d("SEND TRANS", res);
                    TransactParser.completeTransaction(newTransaction, (done)-> {
                        returnResposeToApp(newTransaction);
                    } );
                });
            }
        }

    }

    private static void completeTransaction(Transaction newTransaction, TransactionCompleteCallback transactionCompleteCallback) {
        transactionCompleteCallback.done(newTransaction);
    }


    public void returnResposeToApp(Transaction newTransaction){
        // format amount
        Double amt = Double.parseDouble(newTransaction.getAmount());
        // return result to the calling activity
        Intent _data = new Intent();
        _data.putExtra("response", newTransaction.getResponsemessage());
        _data.putExtra("responseCode", newTransaction.getResponsecode());
        _data.putExtra("amount", formatAmount(amt));
        _data.putExtra("refNo", newTransaction.getRefno());
        _data.putExtra("batchNo", newTransaction.getBatchno());
        _data.putExtra("seqNo", newTransaction.getSeqno());
        _data.putExtra("dateTime", newTransaction.getDatetime());
        // update seqno and batchno
        Integer seqNo = sharedPref.getInt(getString(R.string.seq_no), 1);
        Integer newSeqNo = seqNo + 1;
        mEditor.putInt(getString(R.string.seq_no), newSeqNo);
        mEditor.apply();
        setResult(RESULT_OK, _data);
        finish();
    }

    public static String formatAmount(double totalAuthAmount) {
        String Currency = ""+Html.fromHtml("&#8358;");
        String Separator = ",";
        Boolean Spacing = false;
        Boolean Delimiter = false;
        Boolean Decimals = true;
        String currencyFormat = "";
        if (Spacing) {
            if (Delimiter) {
                currencyFormat = Currency + ". ";
            } else {
                currencyFormat = Currency + " ";
            }
        } else if (Delimiter) {
            currencyFormat = Currency + ".";
        } else {
            currencyFormat = Currency;
        }

        String tformatted = NumberFormat.getCurrencyInstance().format(totalAuthAmount / 100.0D).replace(NumberFormat.getCurrencyInstance().getCurrency().getSymbol(), currencyFormat);
        return tformatted;
    }
}
