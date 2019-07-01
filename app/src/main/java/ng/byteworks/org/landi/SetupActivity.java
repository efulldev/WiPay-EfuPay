package ng.byteworks.org.landi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import ng.byteworks.org.landi.utils.mainDatabase;


public class SetupActivity extends AppCompatActivity {

    private static SharedPreferences sharedPref;
    private static mainDatabase mDatabase;
    private static SharedPreferences.Editor mEditor;

    private String userPermission;
    private static Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = sharedPref.edit();
        mDatabase = new mainDatabase(getApplicationContext());

        Toolbar toolbar = (Toolbar) findViewById(R.id.setupToolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

//        get bundles from previous activity
//        use data to set visibility of options which should be
//        accessible to users based on their previledge
        Intent intent = getIntent();
        final String permission = intent.getStringExtra("permission");
        userPermission = permission;
//disable some functions based on permission level
        showSettingsButtons(userPermission);
//        change receipt logo
        Button chgLogo = (Button) findViewById(R.id.changeRecLogo);
        chgLogo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (userPermission.equals("admin")) {
                    Intent intent = new Intent(SetupActivity.this, ReceiptLogoActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
                }
            }
        });
//open EPMS Admin Config
        Button admin = (Button) findViewById(R.id.admin);
        admin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (userPermission.equals("admin")) {
                    Intent intent = new Intent(SetupActivity.this, com.arke.sdk.view.EPMSAdminActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
                }
            }
        });

        //open EPMS Admin Config
        Button ota = (Button) findViewById(R.id.ota);
        ota.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                checkOsVersion();
            }
        });

//sync terminal
        Button syncTerm = (Button) findViewById(R.id.syncTermBtn);
        syncTerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncTerminal();
            }
        });

//    end of day
        Button EOD = (Button) findViewById(R.id.eod);
        EOD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endOfDayConfirm(view);
            }
        });
//        print eod receipt
//        Button EODPrint = (Button) findViewById(R.id.eodPrintBtn);
//        EODPrint.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                printEodReceiptNow(view);
//            }
//        });


        //        open TID config setting dialog
        Button tidBtn = (Button) findViewById(R.id.terminalIdBtn);
        tidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openTidDialog(view);
            }
        });


//        open host config setting dialog
        Button hostConf = (Button) findViewById(R.id.hostConfigBtn);
        hostConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openHostConfDialog(view, permission);
            }
        });

//        open key comp config setting dialog
        Button keyConf = (Button) findViewById(R.id.keyConfigBtn);
        keyConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openKeyCompConfDialog(view, permission);
            }
        });

        //print transaction history
        Button prinTransHistBtn = (Button) findViewById(R.id.prinTransHistBtn);
        prinTransHistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String headerLogoPath = sharedPref.getString("headerlogo", null);
                try {
                    com.arke.sdk.view.EPMSAdminActivity.printTransactionHistoryReceipt(SetupActivity.this, headerLogoPath);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        //close batch
        Button closeBatchBtn = (Button) findViewById(R.id.closeBatchBtn);
        closeBatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Integer batchNo = sharedPref.getInt(getString(R.string.batch_no), 1);
                Integer newBatchNo = batchNo + 1;
                mEditor.putInt(getString(R.string.batch_no), newBatchNo);
                mEditor.apply();
                com.arke.sdk.view.EPMSAdminActivity.closeBatch(SetupActivity.this);
            }
        });

        //reprint receipt
        Button reprintReceiptBtn = (Button) findViewById(R.id.reprintReceiptBtn);
        reprintReceiptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String headerLogoPath = sharedPref.getString("headerlogo", null);
                com.arke.sdk.view.EPMSAdminActivity.reprintReceipt(SetupActivity.this, SetupActivity.this, headerLogoPath);
            }
        });

        //clear database
        Button clearDBBtn = (Button) findViewById(R.id.clearDBBtn);
        clearDBBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDatabase.deleteEftTransaction();
                com.arke.sdk.view.EPMSAdminActivity.clearDatabase(SetupActivity.this);
            }
        });
    }

    private void showSettingsButtons(String userPermission) {
        Button opPinBtn = (Button)findViewById(R.id.chgOpPinBtn);
        Button supPinBtn = (Button)findViewById(R.id.chgSupPinBtn);
        Button adPinBtn = (Button)findViewById(R.id.chgAdPinBtn);
        Button cloudLink = (Button)findViewById(R.id.cloudLink);
        Button chngBizMailBtn = (Button)findViewById(R.id.chngBizMailBtn);
        Button closeBatchBtn = (Button)findViewById(R.id.closeBatchBtn);
        Button clearDBBtn = (Button)findViewById(R.id.clearDBBtn);
        Button prinTransHistBtn = (Button)findViewById(R.id.prinTransHistBtn);
        Button printEODRec = (Button)findViewById(R.id.printEODRec);
        View epmsConfig = (View)findViewById(R.id.epmsConfigInc);
        switch (userPermission){
            case "operator":
                adPinBtn.setVisibility(View.GONE);
                supPinBtn.setVisibility(View.GONE);
                epmsConfig.setVisibility(View.GONE);
                cloudLink.setVisibility(View.GONE);
                chngBizMailBtn.setVisibility(View.GONE);
                closeBatchBtn.setVisibility(View.GONE);
                clearDBBtn.setVisibility(View.GONE);
                printEODRec.setVisibility(View.GONE);
                break;
            case "supervisor":
                adPinBtn.setVisibility(View.GONE);
                epmsConfig.setVisibility(View.GONE);
                cloudLink.setVisibility(View.GONE);
                break;
            default:
//              admin
                break;
        }
        Toast.makeText(SetupActivity.this, "Access granted, Permission Level: "+userPermission.toUpperCase(), Toast.LENGTH_SHORT).show();
    }


    private void endOfDayConfirm(View view) {
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.confirmation_dialog_layout, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final TextView message = (TextView) promptView.findViewById(R.id.confirmation_textView);
            message.setText("This operation will wipe all transaction history from the device." +
                    "\nDo you wish to proceed anyway?");
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Proceed Anyway", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            endOfDayInitiate();
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

    private void endOfDayInitiate() {
//                check if business email is set
        String email = this.sharedPref.getString("businessEmail", "");
        if(email.length() == 0){
            Toast.makeText(SetupActivity.this, "Set Business E-mail", Toast.LENGTH_SHORT).show();
        }else {
            if (mDatabase.listTransactions().size() > 0) {
//                    mDatabase.deleteEftTransaction();
                Integer batchNo = sharedPref.getInt(getString(R.string.batch_no), 1);
                Integer newBatchNo = batchNo + 1;
                mEditor.putInt(getString(R.string.batch_no), newBatchNo);
                mEditor.putInt(getString(R.string.seq_no), 1);
                mEditor.commit();
//                instruct the server to send a report of the day's transactions to the merchant's email
                serverSendSummary(batchNo);
                Toast.makeText(SetupActivity.this, "End of day initiated : Batch No.:" + newBatchNo, Toast.LENGTH_LONG).show();
//                    delete eft table
//                mDatabase.deleteEftTransaction();
            } else {
                Toast.makeText(SetupActivity.this, "No transaction has been carried out since previous End of Day", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void serverSendSummary(Integer batchNo) {
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
        String email = sharedPref.getString("businessEmail", "");
        String tid = sharedPref.getString("terminalid", "");
        String mid = sharedPref.getString("merchantid", "");
        String urlPath = _uri + "/sendEodSummaryToMerchant?terminalId="+tid+"&merchantId="+mid+"&batchNo="+batchNo+"&email="+email;
        getServerResponse(urlPath);
    }

    private void checkOsVersion() {
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
        String packageName =  BuildConfig.APPLICATION_ID;
        String versionName = BuildConfig.VERSION_NAME;
        String url = _uri + "/checkOsVersion?package=" + packageName + "&version=" + versionName;

        Log.d("Checking OS Version ", url);
        Toast.makeText(SetupActivity.this, "Checking for updates", Toast.LENGTH_SHORT).show();

        RequestQueue requestQueue = Volley.newRequestQueue(SetupActivity.this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Update Response", response);
                        if (response.equals("true")) {
                            Toast.makeText(SetupActivity.this, "Your app is up to date", Toast.LENGTH_SHORT).show();
                        } else {
//                            navigate to update page
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(response)));
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Cloud DB Error", error.toString());
                        Toast.makeText(SetupActivity.this, "Communication Error", Toast.LENGTH_LONG).show();
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

    private void syncTerminal() {
//        if (!userPermission.equals("admin")) {
//            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
//        } else {
            String tid = sharedPref.getString("terminalid", "");
            String mid = sharedPref.getString("merchantid", "");
            String ptsp = sharedPref.getString("ptspId", "");
            String bank = sharedPref.getString("bankCode", "");
            String deviceModel = android.os.Build.MODEL;
            String versionName = BuildConfig.VERSION_NAME;
            String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
            String bizName = sharedPref.getString("businessName", "NOT SET");
            String bizEmail = sharedPref.getString("businessEmail", "NOT SET");
            String urlPath = _uri + "/syncTerminal?business_name="+bizName+"&business_email="+bizEmail+"&terminal_id=" + tid + "&merchant_id=" + mid + "&ptsp_id=" + ptsp + "&bank_id=" + bank + "&device_model=" + deviceModel + "&appVersion=" + versionName;
            getServerResponse(urlPath);
            Toast.makeText(SetupActivity.this, "Terminal sync initiated", Toast.LENGTH_SHORT).show();
//        }
    }

    private void getServerResponse(String url) {
        String escapedUrl = encodeUrlEscaped(url);
        Log.d("Cloud DB URI", escapedUrl);
        RequestQueue requestQueue = Volley.newRequestQueue(SetupActivity.this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                escapedUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Cloud DB Response", response);
                        Toast.makeText(SetupActivity.this, "Completed", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Cloud DB Error", error.toString());
                        Toast.makeText(SetupActivity.this, "Communication Error", Toast.LENGTH_LONG).show();
                    }
                }
        );
//      set retry policy to determine how long volley should wait before resending a failed request
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        add stringRequest to the queue
        requestQueue.add(stringRequest);
    }

    public static String encodeUrlEscaped(String urlStr) {
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            Log.e("Malformed Url", e.toString());
            e.printStackTrace();
        }
        URI uri = null;
        try {
            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (URISyntaxException e) {
            Log.e("URI Exception", e.toString());
            e.printStackTrace();
        }
        try {
            url = uri.toURL();
        } catch (MalformedURLException e) {
            Log.e("Malformed Url", e.toString());
            e.printStackTrace();
        }
        Log.d("Escaped URL", url.toString());
        return url.toString();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private void openKeyCompConfDialog(View view, String permission) {
        if (!permission.equals("admin")) {
            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
        } else {
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.key_component_edit_layout, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText key1 = (EditText) promptView.findViewById(R.id.keyomp1TextField);
            final EditText key2 = (EditText) promptView.findViewById(R.id.keyComp2TextField);

            final String _key1 = this.sharedPref.getString("key1", "");
            final String _key2 = this.sharedPref.getString("key2", "");

            key1.setText(_key1);
            key2.setText(_key2);

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
//                                save the new pin
                            mEditor.putString("key1", key1.getText().toString());
                            mEditor.putString("key2", key2.getText().toString());
                            mEditor.commit();
                            Toast.makeText(getApplicationContext(), "Key components have been changed successfully", Toast.LENGTH_LONG).show();
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
    }

    private void openHostConfDialog(View view, String permission) {
        if (!permission.equals("admin")) {
            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
        }else {
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.host_config_edit_layout, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText hostIp = (EditText) promptView.findViewById(R.id.hostIpTextField);
            final EditText hostPort = (EditText) promptView.findViewById(R.id.hostPortTextField);
            final EditText protocol = (EditText) promptView.findViewById(R.id.protocolTextField);
            final EditText callHome = (EditText) promptView.findViewById(R.id.callHomeTimerTextField);

            final String _hostip = this.sharedPref.getString("hostip", "");
            final String _hostport = this.sharedPref.getString("hostport", "");
            final String _protocol = this.sharedPref.getString("protocol", "");
            final String _callHome = this.sharedPref.getString("callhome", "");

            hostIp.setText(_hostip);
            hostPort.setText(_hostport);
            protocol.setText(_protocol);
            callHome.setText(_callHome);

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
//                                save the new pin
                            mEditor.putString("hostip", hostIp.getText().toString());
                            mEditor.putString("hostport", hostPort.getText().toString());
                            mEditor.putString("protocol", protocol.getText().toString());
                            mEditor.putString("callhome", callHome.getText().toString());
                            mEditor.commit();
                            Toast.makeText(getApplicationContext(), "Host Configuration has been changed successfully", Toast.LENGTH_LONG).show();
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
    }

    public void openTidDialog(View view) {
        if (!userPermission.equals("admin")) {
            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
        }else {
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.terminal_id_edit_layout, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText termId = (EditText) promptView.findViewById(R.id.terminalIdTextField);
            final EditText merId = (EditText) promptView.findViewById(R.id.merchantIdTextField);
            final EditText bankCode = (EditText) promptView.findViewById(R.id.bankCodeTextField);
            final EditText ptspId = (EditText) promptView.findViewById(R.id.ptspIdTextField);

            final String tid = this.sharedPref.getString("terminalid", "");
            final String mid = this.sharedPref.getString("merchantid", "");
            final String bCode = this.sharedPref.getString("bankCode", "");
            final String ptsp_id = this.sharedPref.getString("ptspId", "");

//        initialize text fields in dialog
            termId.setText(tid);
            merId.setText(mid);
            bankCode.setText(bCode);
            ptspId.setText(ptsp_id);

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Save Changes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
//                                save the new pin
                            mEditor.putString("terminalid", termId.getText().toString());
                            mEditor.putString("merchantid", merId.getText().toString());
                            mEditor.putString("bankCode", bankCode.getText().toString());
                            mEditor.putString("ptspId", ptspId.getText().toString());
                            mEditor.commit();
                            Toast.makeText(getApplicationContext(), "Terminal Identification has been changed successfully", Toast.LENGTH_SHORT).show();
                            syncTerminal();
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
    }

    //print eod receipt
    public void printEodReceiptNow(View view){
        String headerLogoPath = sharedPref.getString("headerlogo", null);
        try {
            com.arke.sdk.view.EPMSAdminActivity.printEODReceipt(SetupActivity.this, headerLogoPath);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    //    change operator pin
    public void changePinDialog(View view) {
        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
        View promptView = layoutInflater.inflate(R.layout.change_pin_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText oldPin = (EditText) promptView.findViewById(R.id.oldPinTextField);
        final EditText newPin = (EditText) promptView.findViewById(R.id.newPinTextField);
        final EditText newPin2 = (EditText) promptView.findViewById(R.id.confirmNewPinTextField);

        final String savedPin = this.sharedPref.getString("opPin", "1234");
        final String supPin = this.sharedPref.getString("supPin", "0000");
        final String adminPin = this.sharedPref.getString("adminPin", "260089");
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Change Operator PIN", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        check if new pin has not been used by other users
                        if(!newPin.getText().toString().equals(supPin) && !newPin.getText().toString().equals(adminPin)){
//                        check if old pin match what is saved
                            if(savedPin.toString().equals(oldPin.getText().toString()) || supPin.toString().equals(oldPin.getText().toString()) || adminPin.toString().equals(oldPin.getText().toString()) ){
//                            check if both new pins are exact
                                if(newPin.getText().toString().equals(newPin2.getText().toString())){
//                                save the new pin
                                    mEditor.putString("opPin", newPin.getText().toString());
                                    mEditor.commit();
                                    Toast.makeText(getApplicationContext(), "Operator PIN has been changed successfully", Toast.LENGTH_LONG).show();
                                }else{
//                                alert the user pins do not match
                                    Snackbar.make(view, "Sorry, New PINs do not match. Kindly try again.", Snackbar.LENGTH_LONG).show();
                                }
                            }else{
//                            alert the user invalid pin
                                Snackbar.make(view, "Invalid PIN! Please try again", Snackbar.LENGTH_LONG).show();
                            }
                        }else{
                            Toast.makeText(SetupActivity.this, "Please specify a different PIN", Toast.LENGTH_SHORT).show();
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

//    change supervisor pin
    public void changeSupPinDialog(View view) {
        if (userPermission.equals("admin") || userPermission.equals("supervisor")) {
        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
        View promptView = layoutInflater.inflate(R.layout.change_pin_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText oldPin = (EditText) promptView.findViewById(R.id.oldPinTextField);
        final EditText newPin = (EditText) promptView.findViewById(R.id.newPinTextField);
        final EditText newPin2 = (EditText) promptView.findViewById(R.id.confirmNewPinTextField);

        final String savedPin = this.sharedPref.getString("supPin", "0000");
        final String adminPin = this.sharedPref.getString("adminPin", "260089");
        final String opPin = this.sharedPref.getString("opPin", "1234");
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Change Supervisor PIN", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(!newPin.getText().toString().equals(opPin) && !newPin.getText().toString().equals(adminPin)){
//                        check if old pin match what is saved
                            if(savedPin.equals(oldPin.getText().toString()) || adminPin.equals(oldPin.getText().toString())){
//                            check if both new pins are exact
                                if(newPin.getText().toString().equals(newPin2.getText().toString())){
//                                save the new pin
                                    mEditor.putString("supPin", newPin.getText().toString());
                                    mEditor.commit();
                                    Toast.makeText(getApplicationContext(), "Supervisor PIN has been changed successfully", Toast.LENGTH_LONG).show();
                                }else{
//                                alert the user pins do not match
                                    Snackbar.make(view, "Sorry, New PINs do not match. Kindly try again.", Snackbar.LENGTH_LONG).show();
                                }
                            }else{
//                            alert the user invalid pin
                                Snackbar.make(view, "Invalid PIN! Please try again", Snackbar.LENGTH_LONG).show();
                            }
                        }else{
                            Toast.makeText(SetupActivity.this, "Please specify a different PIN", Toast.LENGTH_SHORT).show();
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
        }else{
            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
        }
    }


    //    change opperator pin
    public void changeAdminPinDialog(View view) {
        if (userPermission.equals("admin")) {
            // get prompts.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.change_pin_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText oldPin = (EditText) promptView.findViewById(R.id.oldPinTextField);
            final EditText newPin = (EditText) promptView.findViewById(R.id.newPinTextField);
            final EditText newPin2 = (EditText) promptView.findViewById(R.id.confirmNewPinTextField);

            final String savedPin = this.sharedPref.getString("adminPin", "260089");
            final String supPin = this.sharedPref.getString("supPin", "260089");
            final String opPin = this.sharedPref.getString("opPin", "260089");
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Change Admin PIN", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(!newPin.getText().toString().equals(supPin) && !newPin.getText().toString().equals(opPin)){
//                        check if old pin match what is saved
                                if (savedPin.toString().equals(oldPin.getText().toString())) {
//                            check if both new pins are exact
                                    if (newPin.getText().toString().equals(newPin2.getText().toString())) {
//                                save the new pin
                                        mEditor.putString("adminPin", newPin.getText().toString());
                                        mEditor.commit();
                                        Toast.makeText(getApplicationContext(), "Admin PIN has been changed successfully", Toast.LENGTH_LONG).show();
                                    } else {
//                                alert the user pins do not match
                                        Snackbar.make(view, "Sorry, New PINs do not match. Kindly try again.", Snackbar.LENGTH_LONG).show();
                                    }
                                } else {
//                            alert the user invalid pin
                                    Snackbar.make(view, "Invalid PIN! Please try again", Snackbar.LENGTH_LONG).show();
                                }
                            }else{
                                Toast.makeText(SetupActivity.this, "Please specify a different PIN", Toast.LENGTH_SHORT).show();
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
        else{
            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
        }
    }

//    change cloud db uri
    public void showInputDialog(View view) {
        if (userPermission.equals("admin")) {
            // get prompts.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.cloud_database_url_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText cloudDBUri = (EditText) promptView.findViewById(R.id.cloudDBUri);
//        initialize text
            String uri = this.sharedPref.getString("cloudDBUri", "");
            cloudDBUri.setText(uri);
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
//                        save new cloud database url
                            mEditor.putString("cloudDBUri", cloudDBUri.getText().toString());
                            mEditor.commit();
                            Toast.makeText(getApplicationContext(), "Cloud Database URI has been set successfully", Toast.LENGTH_LONG).show();
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
        else{
            Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_LONG).show();
        }
    }

//edit business name
    public void editBusinessName(View view) {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
        View promptView = layoutInflater.inflate(R.layout.edit_business_name_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText bizName = (EditText) promptView.findViewById(R.id.businessNameTextField);
//        initialize text
        String name = this.sharedPref.getString("businessName", "");
        bizName.setText(name);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        save new cloud database url
                        mEditor.putString("businessName", bizName.getText().toString());
                        mEditor.commit();
                        Toast.makeText(getApplicationContext(), "Your Business' name has been set successfully", Toast.LENGTH_LONG).show();
                        syncTerminal();
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


    //edit business emial
    public void editBusinessEmail(View view) {
            if (userPermission.equals("admin") || userPermission.equals("supervisor")) {
            // get prompts.xml view
            LayoutInflater layoutInflater = LayoutInflater.from(SetupActivity.this);
            View promptView = layoutInflater.inflate(R.layout.edit_business_email_layout_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetupActivity.this);
            alertDialogBuilder.setView(promptView);

            final EditText email = (EditText) promptView.findViewById(R.id.businessEmailTextField);
    //        initialize text
            String _email = this.sharedPref.getString("businessEmail", "");
            email.setText(_email);
            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
    //                        save new cloud database url
                            mEditor.putString("businessEmail", email.getText().toString());
                            mEditor.commit();
                            Toast.makeText(getApplicationContext(), "Your Business' e-mail has been set successfully", Toast.LENGTH_LONG).show();
                            syncTerminal();
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
        }else{
                Toast.makeText(getApplicationContext(), "You do not have access to this feature", Toast.LENGTH_SHORT).show();
            }
    }
}
