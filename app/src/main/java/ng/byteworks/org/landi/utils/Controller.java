package ng.byteworks.org.landi.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.arke.sdk.util.data.BytesUtil;
import com.arke.sdk.util.epms.Transaction;

import static ng.byteworks.org.landi.SetupActivity.encodeUrlEscaped;

public class Controller {
    private Context context;
    private static SharedPreferences sharedPref;
    private SharedPreferences.Editor mEditor;


    public Controller(Context mContext) {
        this.context = mContext;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mEditor = sharedPref.edit();
    }


    //send transaction data to server
    public void sendTransaction(TransactionResponse response, ActionCompleteCallback callback){
        Transaction transaction = response.getTransaction();
        String details = response.getDetails();
        String tid = sharedPref.getString("terminalid", "");
        String _uri = sharedPref.getString("cloudDBUri", "http://192.168.8.101/api/");
        String bizName = sharedPref.getString("businessName", "NOT SET");
        final String bankCode = sharedPref.getString("bankCode", "null");
        final String ptsp_id = sharedPref.getString("ptspId", "null");
        final String trans_ref_no = tid + "-" + transaction.getRefno() + "-" + transaction.getDate();
        final String cardHolderName = new String(BytesUtil.hexString2ByteArray(transaction.getCardholdername()));

        String urlPath = _uri+"/newTransaction/?amt="+transaction.getAmount()+"&terId="+tid+
                "&merId="+transaction.getMarchantid()+
                "&merchant_name="+transaction.getMarchant()+"&business_name="+bizName+
                "&batchNo="+transaction.getBatchno()+
                "&seqNo="+transaction.getSeqno()+"&rrn="+transaction.getRefno()+
                "&respCode="+transaction.getResponsecode()+"&transType="+transaction.getTransname()+
                "&currency="+transaction.getCurrencycode()+"&stan="+transaction.getStan()+
                "&status="+transaction.getStatus()+"&details="+details+
                "&respMsg="+transaction.getResponsemessage()+"&dateTime="+transaction.getDatetime()
                +"&bankCode="+bankCode+"&ptspId="+ptsp_id+"&MaskedPAN="+ MiscFunctions.getMaskPAN(transaction.getTrack2())
                +"&CardNo="+transaction.getTrack2()+"&CardScheme="+MiscFunctions.CardScheme(transaction)+"&CardExpiryMonth="+transaction.getExpiry().substring(2, 4)
                +"&CardExpiryYear="+transaction.getExpiry().substring(0, 2)+"&CustomerName="+cardHolderName
                +"&TransactionReference=" + trans_ref_no + "&Nuban=" + null + "&AccountType=" + transaction.getFromac() + "&AuthCode=" + transaction.getAuthid();

        getServerResponse(urlPath, callback);

    }

    public void getServerResponse(String url, ActionCompleteCallback callback){
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
                        Toast.makeText(context, "Completed", Toast.LENGTH_SHORT).show();
                        if(callback != null){
                            callback.done("success");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Cloud DB Error", error.toString());
                        Toast.makeText(context, "Communication Error", Toast.LENGTH_LONG).show();
                        if(callback != null){
                            callback.done("failed");
                        }
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
