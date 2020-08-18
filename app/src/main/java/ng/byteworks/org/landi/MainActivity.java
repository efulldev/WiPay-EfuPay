package ng.byteworks.org.landi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.arke.sdk.util.data.BytesUtil;
import com.arke.sdk.util.epms.Constant;
import com.arke.sdk.util.epms.SqliteDatabase;
import com.arke.sdk.util.epms.Transaction;
import com.arke.sdk.view.EPMSAdminActivity;

import ng.byteworks.org.landi.utils.ActionCompleteCallback;
import ng.byteworks.org.landi.utils.Controller;
import ng.byteworks.org.landi.utils.MiscFunctions;
import ng.byteworks.org.landi.utils.TransactionResponse;
import ng.byteworks.org.landi.utils.mainDatabase;
import ng.byteworks.org.landi.utils.redundantDatabase;

import static ng.byteworks.org.landi.SetupActivity.encodeUrlEscaped;

public class MainActivity extends AppCompatActivity {

    private static SharedPreferences sharedPref;
    private mainDatabase mDatabase;
    private SqliteDatabase epmsDatabase;
    private redundantDatabase mRedDatabase;
    private SharedPreferences.Editor mEditor;

    private Integer transType;
    private Integer batchNo;
    private Integer seqNo;
    private Integer amount;
    private String rrn_text = "";
    private static Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = sharedPref.edit();
        mDatabase = new mainDatabase(getApplicationContext());
        epmsDatabase = new SqliteDatabase(getApplicationContext());
        mRedDatabase = new redundantDatabase(getApplicationContext());
        context = getApplicationContext();

        TextView bizText = (TextView)findViewById(R.id.bizNameTextView);
        String bizName = this.sharedPref.getString("businessName", "");
        bizText.setText(bizName);

        String path = this.sharedPref.getString("headerlogo", "not set");
        Log.d("M/A: Logo Path", path);


//        get cloud uri and set new one if none exists
        String uri = this.sharedPref.getString("cloudDBUri", "");

        if(uri.length() == 0){
            mEditor.putString("cloudDBUri", "http://terminal.efulltech.com.ng/api");
            mEditor.commit();
        }

        checkOsVersion();

//make payment
        Button purchase = (Button) findViewById(R.id.purchase);
        purchase.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                getBatchSeqNos();
                Intent intent = new Intent(MainActivity.this, com.arke.sdk.view.EPMSActivity.class);
                intent.putExtra("trantype", "" + 1);
                intent.putExtra("batchno", "" + batchNo);
                intent.putExtra("seqno", "" + seqNo);
                intent.putExtra("amount", "");
                startActivityForResult(intent, 0);
            }
        });


//        open preferences page
        Button setup = (Button) findViewById(R.id.setup);
        setup.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                confirmPinThenSettingsPage(v);
            }
        });

//transaction history
        Button history = (Button) findViewById(R.id.history);
        history.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TransactionHistory.class);
                intent.putExtra("dbType", "mainDB");
                startActivity(intent);
            }
        });

    }

    public void checkOsVersion() {
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
        String packageName =  BuildConfig.APPLICATION_ID;
        String versionName = BuildConfig.VERSION_NAME;
        String tid = this.sharedPref.getString("terminalid", "");
        String url = _uri + "/checkOsVersion?terminalId="+tid+"&package="+packageName+"&version="+versionName;

        Log.d("Checking OS Version ", url);
        Toast.makeText(MainActivity.this, "Checking for updates", Toast.LENGTH_SHORT).show();

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Update Response", response);
                        if(response.equals("true")){
                            Toast.makeText(MainActivity.this, "Your app is up to date", Toast.LENGTH_SHORT).show();
                        }else{
//                            navigate to update page
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(response)));
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Cloud DB Error", error.toString());
                        Toast.makeText(MainActivity.this, "Timeout, Please check your internet connection", Toast.LENGTH_LONG).show();
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


    protected void confirmPinThenSettingsPage(View view) {
        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.admin_pin_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText uPin = (EditText) promptView.findViewById(R.id.adminPinTextField);
        final String savedPin = this.sharedPref.getString("opPin", "1234");
        final String savedSupPin = this.sharedPref.getString("supPin", "0000");
        final String savedAdminPin = this.sharedPref.getString("adminPin", "260089");
        // setup a dialog window
         alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String pin = uPin.getText().toString();
                        String permission = "";
                        if(savedPin.equals(pin) || savedSupPin.equals(pin) || savedAdminPin.equals(pin)){
                            if(savedAdminPin.equals(pin)){
                                permission = "admin";
                            }else if(savedSupPin.equals(pin)){
                                permission = "supervisor";
                            }else{
                                permission = "operator";
                            }
//                            display setupActivity page
                            Intent intent = new Intent(MainActivity.this, SetupActivity.class);
                            intent.putExtra("permission", permission);
                            startActivity(intent);
                        }else{
//                            alert invalid PIN
                            Snackbar.make(view, "Invalid PIN! Please try again", Snackbar.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void getBatchSeqNos(){
        Integer batchNo = sharedPref.getInt(getString(R.string.batch_no), 1);
        Integer seqNo = sharedPref.getInt(getString(R.string.seq_no), 1);
        this.batchNo = batchNo;
        this.seqNo = seqNo;
    }

    //print eod receipt
    public void printEodReceiptNow(View view){
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.admin_pin_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText uPin = (EditText) promptView.findViewById(R.id.adminPinTextField);
        final String savedPin = this.sharedPref.getString("opPin", "1234");
        final String savedSupPin = this.sharedPref.getString("supPin", "0000");
        final String savedAdminPin = this.sharedPref.getString("adminPin", "260089");
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String pin = uPin.getText().toString();
                        String permission = "";
                        if(savedPin.equals(pin) || savedSupPin.equals(pin) || savedAdminPin.equals(pin)){
                            Boolean permissionGranted = false;
                            if(savedAdminPin.equals(pin)){
                                permission = "admin";
                                permissionGranted = true;
                            }else if(savedSupPin.equals(pin)){
                                permission = "supervisor";
                                permissionGranted = true;
                            }else{
                                permission = "operator";
                                permissionGranted = false;
                            }
//                           print end of day receipt if permission is granted
                            if(permissionGranted){
                                String headerLogoPath = sharedPref.getString("headerlogo", null);
                                try {
                                    com.arke.sdk.view.EPMSAdminActivity.printEODReceipt(MainActivity.this, headerLogoPath);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                            }
                        }else{
//                            alert invalid PIN
                            Snackbar.make(view, "Invalid PIN! Please try again", Snackbar.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up Button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check that it is the EPMSActivity with an OK result
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                // get String data from Intent
                Transaction newTransaction = (Transaction) data
                        .getSerializableExtra("response");
                //save transaction locally
                if(newTransaction.getTranstype() == 1) {
                    mDatabase.saveEftTransaction(newTransaction);
                    mDatabase.saveTransactionOrigin(newTransaction.getRefno(), "WiPay EfuPay");
                    epmsDatabase.saveEftTransaction(newTransaction);
                }

                String headerLogoPath = sharedPref.getString("headerlogo", null);
                if(headerLogoPath != null){
                    try {
                        EPMSAdminActivity.printReceipt(newTransaction, MainActivity.this, headerLogoPath);
                    } catch (Exception e) {
                        Log.e("MainActivity", e.getLocalizedMessage());
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Please configure receipt logo", Toast.LENGTH_SHORT).show();
                }

                if (newTransaction.getMode() == Constant.CHIP) {
                    EPMSAdminActivity.removeCard(newTransaction, MainActivity.this,
                            MainActivity.this);
                }

//                 send transaction data to Efull Terminal Manager Server
                    TransactionResponse response = new TransactionResponse();
                    response.setTransaction(newTransaction);
                    Controller controller = new Controller(context);
                    controller.sendTransaction(response, (res)-> {
                        Log.d("SEND TRANS", res);
                    });

//                update seqNo
                    Integer seqNo = sharedPref.getInt(getString(R.string.seq_no), 1);
                    Integer newSeqNo = seqNo + 1;
                    mEditor.putInt(getString(R.string.seq_no), newSeqNo);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    mEditor.apply();
                }
            }
        }
    }


    //send transaction data to server
//    public static void sendTransaction(TransactionResponse response, ActionCompleteCallback callback){
//        Transaction transaction = response.getTransaction();
//        String details = response.getDetails();
//        String tid = sharedPref.getString("terminalid", "");
//        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
//        String bizName = sharedPref.getString("businessName", "NOT SET");
//        final String bankCode = sharedPref.getString("bankCode", "null");
//        final String ptsp_id = sharedPref.getString("ptspId", "null");
//        final String trans_ref_no = tid + "-" + transaction.getRefno() + "-" + transaction.getDate();
//        final String cardHolderName = new String(BytesUtil.hexString2ByteArray(transaction.getCardholdername()));
//
//        String urlPath = _uri+"/newTransaction/?amt="+transaction.getAmount()+"&terId="+tid+
//                "&merId="+transaction.getMarchantid()+
//                "&merchant_name="+transaction.getMarchant()+"&business_name="+bizName+
//                "&batchNo="+transaction.getBatchno()+
//                "&seqNo="+transaction.getSeqno()+"&rrn="+transaction.getRefno()+
//                "&respCode="+transaction.getResponsecode()+"&transType="+transaction.getTransname()+
//                "&currency="+transaction.getCurrencycode()+"&stan="+transaction.getStan()+
//                "&status="+transaction.getStatus()+"&details="+details+
//                "&respMsg="+transaction.getResponsemessage()+"&dateTime="+transaction.getDatetime()
//                +"&bankCode="+bankCode+"&ptspId="+ptsp_id+"&MaskedPAN="+ MiscFunctions.getMaskPAN(transaction.getTrack2())
//                +"&CardNo="+transaction.getTrack2()+"&CardScheme="+MiscFunctions.CardScheme(transaction)+"&CardExpiryMonth="+transaction.getExpiry().substring(2, 4)
//                +"&CardExpiryYear="+transaction.getExpiry().substring(0, 2)+"&CustomerName="+cardHolderName
//                +"&TransactionReference=" + trans_ref_no + "&Nuban=" + null + "&AccountType=" + transaction.getFromac() + "&AuthCode=" + transaction.getAuthid();
//
//        getServerResponse(urlPath, callback);
//
//    }
//
//    public static void getServerResponse(String url, ActionCompleteCallback callback){
//        String escapedUrl = encodeUrlEscaped(url);
//        Log.d("Cloud DB URI", escapedUrl);
//        RequestQueue requestQueue = Volley.newRequestQueue(context);
//        StringRequest stringRequest = new StringRequest(
//                Request.Method.GET,
//                escapedUrl,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        Log.d("Cloud DB Response", response);
//                        Toast.makeText(context, "Completed", Toast.LENGTH_SHORT).show();
//                        if(callback != null){
//                            callback.done("success");
//                        }
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        Log.e("Cloud DB Error", error.toString());
//                        Toast.makeText(context, "Communication Error", Toast.LENGTH_LONG).show();
//                        if(callback != null){
//                            callback.done("failed");
//                        }
//                    }
//                }
//        );
////      set retry policy to determine how long volley should wait before resending a failed request
//        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
//                30000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
////        add jsonObjectRequest to the queue
//        requestQueue.add(stringRequest);
//    }
}
