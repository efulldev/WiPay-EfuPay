package ng.byteworks.org.landi;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.arke.sdk.util.data.BytesUtil;
import com.arke.sdk.util.epms.Transaction;
import ng.byteworks.org.landi.utils.mainDatabase;
import ng.byteworks.org.landi.utils.redundantDatabase;

import static ng.byteworks.org.landi.TransactParser.formatAmount;


public class ThisTransaction extends AppCompatActivity {

    private String refno;

    private mainDatabase mDatabase;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor mEditor;
    private redundantDatabase mRedDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_this_transaction);

        Toolbar toolbar = (Toolbar) findViewById(R.id.thisTransactToolbar);
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mDatabase = new mainDatabase(getApplicationContext());
        mRedDatabase = new redundantDatabase(getApplicationContext());
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = sharedPref.edit();

        //get extras from intent starter
        Intent intent = getIntent();
        this.refno = intent.getStringExtra("refno");
//        get transaction data from sqlite database
        Transaction _transaction = mDatabase.getEftTransaction(this.refno);
//        variables and constants
        final TextView pan = (TextView) findViewById(R.id.pan);
        final TextView transtype = (TextView) findViewById(R.id.transtype);
        final TextView response = (TextView) findViewById(R.id.responseMsg);
        final TextView name = (TextView) findViewById(R.id.cardHolderName);
        final TextView cardType = (TextView) findViewById(R.id.cardType);
        final TextView expiry = (TextView) findViewById(R.id.expiryDate);
        final TextView amount = (TextView) findViewById(R.id.amountText_2);
        final TextView rrn = (TextView) findViewById(R.id.rrnText);
        final TextView dateTime = (TextView) findViewById(R.id.transDateTime);
        final TextView originApp = (TextView) findViewById(R.id.originAppText);

        Integer _panLength = _transaction.getPan().length();
        String _panText = _transaction.getPan().substring(0, 6) + "XXXXX" + _transaction.getPan().substring(12, _panLength);
        String cardHolderHame = new String(BytesUtil.hexString2ByteArray(_transaction.getCardholdername()));
        String _trDateTime = _transaction.getDate()+" "+_transaction.getTime();
        String _expText = _transaction.getExpiry().substring(2, 4)+"/"+_transaction.getExpiry().substring(0, 2);
//        set PAN
        pan.setText(_panText);
//        set trans type
        transtype.setText(_transaction.getTransname());
//        set response message
        response.setText(_transaction.getResponsemessage());
//        set card holder name
        name.setText(cardHolderHame);
//        set ref no (RRN)
        rrn.setText(_transaction.getRefno());
    //  set amount
        Float _amount = Float.parseFloat(_transaction.getAmount());
        String _amountText = formatAmount(_amount);
        amount.setText(_amountText);
//        set card type
        String _cardType = new String(BytesUtil.hexString2ByteArray(_transaction.getLabel()));
        cardType.setText(_cardType);
        expiry.setText(_expText);
        dateTime.setText(_trDateTime);
        originApp.setText(mDatabase.getTransactionOrigin(this.refno));
    }

//
//    public void reversal(View view){
//        Transaction _transaction = mDatabase.getEftTransaction(this.refno);
//        Intent reversal = new Intent(ThisTransaction.this, com.arke.sdk.view.EPMSActivity.class);
//        reversal.putExtra("trantype", ""+4);
//        reversal.putExtra("fromac", ""+_transaction.getFromac());
//        reversal.putExtra("toac", ""+_transaction.getToac());
//        reversal.putExtra("mti", ""+_transaction.getMti());
//        reversal.putExtra("datetime", ""+_transaction.getDatetime());
//        reversal.putExtra("refno", ""+_transaction.getRefno());
//        reversal.putExtra("amount", ""+_transaction.getAmount());
//        reversal.putExtra("cashback", ""+_transaction.getCashback());
//        startActivityForResult(reversal, 0);
//    }
//
//    public void refund(View view){
//        Transaction _transaction = mDatabase.getEftTransaction(this.refno);
//        Intent refund = new Intent(ThisTransaction.this, com.arke.sdk.view.EPMSActivity.class);
//        refund.putExtra("trantype", ""+5);
//        refund.putExtra("fromac", ""+_transaction.getFromac());
//        refund.putExtra("toac", ""+_transaction.getToac());
//        refund.putExtra("mti", ""+_transaction.getMti());
//        refund.putExtra("datetime", ""+_transaction.getDatetime());
//        refund.putExtra("refno", ""+_transaction.getRefno());
//        refund.putExtra("amount", ""+_transaction.getAmount());
//        refund.putExtra("cashback", ""+_transaction.getCashback());
//        startActivityForResult(refund, 0);
//    }


    public void printMerchantReceipt(View view) {
        Transaction _transaction = mDatabase.getEftTransaction(this.refno);
        String headerLogoPath = sharedPref.getString("headerlogo", null);
        if(headerLogoPath != null){
            try {
                com.arke.sdk.view.EPMSAdminActivity.reprint(_transaction, ThisTransaction.this, headerLogoPath);
            } catch (Exception e) {
                Log.e("MainActivity", e.getLocalizedMessage());
            }
        }else{
            Toast.makeText(ThisTransaction.this, "Please configure receipt logo", Toast.LENGTH_SHORT).show();
        }
        Log.d("Position", this.refno);
    }

//    end activity when back button is pressed on the toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
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
                mDatabase.saveEftTransaction(newTransaction);
                mRedDatabase.saveEftTransaction(newTransaction);//stores another copy of the transaction for redundancy and record persistence
                mDatabase.saveTransactionOrigin(newTransaction.getRefno(), "Efull Pay Terminal");
                String headerLogoPath = sharedPref.getString("headerlogo", null);
                if(headerLogoPath != null){
                    try {
                        com.arke.sdk.view.EPMSAdminActivity.printReceipt(newTransaction, ThisTransaction.this, headerLogoPath);
                    } catch (Exception e) {
                        Log.e("MainActivity", e.getLocalizedMessage());
                    }
                }else{
                    Toast.makeText(ThisTransaction.this, "Please configure receipt logo", Toast.LENGTH_SHORT).show();
                }
                if (newTransaction.getMode() == com.arke.sdk.util.epms.Constant.CHIP) {
                    com.arke.sdk.view.EPMSAdminActivity.removeCard(newTransaction, ThisTransaction.this,
                            ThisTransaction.this);
                }
            }
        }
    }
}
