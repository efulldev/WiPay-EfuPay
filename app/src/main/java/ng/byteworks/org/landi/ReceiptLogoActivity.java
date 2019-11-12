package ng.byteworks.org.landi;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import ng.byteworks.org.landi.utils.mainDatabase;

import static android.graphics.BitmapFactory.*;

public class ReceiptLogoActivity extends AppCompatActivity {

    private static final int IMAGE_GALLERY_REQUEST = 20;
    private ImageView recImagePrev;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private String TAG;
    private mainDatabase mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_logo);
        Toolbar toolbar = (Toolbar) findViewById(R.id.recLogoToolbar);
        setSupportActionBar(toolbar);
        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mDatabase = new mainDatabase(getApplicationContext());

        // create a folder to store logo
        createFolder("/EfuPay/assets/logo");
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();

        recImagePrev = (ImageView) findViewById(R.id.recImagePreview);
        String path = settings.getString("headerlogo", "");
        File logoTemp = new File(path);

        //  display previously set logo
        if(path.length() > 0 && logoTemp.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(path);

          recImagePrev.setImageBitmap(bitmap);
            Log.d("Logo Path Exists", path);
        }else{
            Log.e("Logo Path", "Invalid/Null image path");
        }
    }

    public void createFolder(String fname) {
        String myfolder = Environment.getExternalStorageDirectory() + "/" + fname;
        File f = new File(myfolder);
        if (!f.exists()) {
            if (!f.mkdirs()) {
                editor.putString("storagePath", myfolder);
                editor.commit();
                Log.d("Storage path created", myfolder);
            }
        }
    }

    public void fetchFromTerminalManager(View view){
//           get bank code
        String bankCode = settings.getString("bankCode", "");
        if(bankCode.length() == 0){
            Toast.makeText(this, "Please specify a CBN Bank Code", Toast.LENGTH_SHORT).show();
        }else{
//            send bank code to terminal manager to fetch the bank's logo url

            String _uri = settings.getString("cloudDBUri", "http://192.168.8.101/api/");
            String url = _uri + "/getBankLogo/"+bankCode;
            Log.d("fetching bank logo url ", url);
            Toast.makeText(this, "Fetching logo URI", Toast.LENGTH_SHORT).show();

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            StringRequest stringRequest = new StringRequest(
                    Request.Method.GET,
                    url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("Fetch logo response", response);
                            if (response.equals("false")) {
                                Toast.makeText(ReceiptLogoActivity.this, "Could not find image resource", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("Dnld","Initiating logo download");
                                downloadFile(response);
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Fetch logo Error", error.toString());
                            Toast.makeText(ReceiptLogoActivity.this, "Communication Error", Toast.LENGTH_LONG).show();
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

    public void loadFileFromUrlDialog(View view) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View promptView = layoutInflater.inflate(R.layout.cloud_database_url_dialog, null);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setView(promptView);

            final EditText uri = (EditText) promptView.findViewById(R.id.cloudDBUri);
            final TextView textView = (TextView) promptView.findViewById(R.id.cloudDBTextView);
            textView.setText("Enter Image resource URL");

            // setup a dialog window
            alertDialogBuilder.setCancelable(false)
                    .setPositiveButton("Download Image", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //download image
                            String downloadUrl = uri.getText().toString();
                            if(downloadUrl.length() == 0){
                                Toast.makeText(ReceiptLogoActivity.this, "Image resource location not defined", Toast.LENGTH_SHORT).show();
                            }else{
                                downloadFile(downloadUrl);
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


    public void downloadFile(String DownloadUrl) {
        Toast.makeText(this, "Downloading File", Toast.LENGTH_SHORT).show();
//        delete previous logo file
        File prevLogo = new File("/storage/emulated/0/EfuPay/assets/logo/ic_header_logo.bmp");
        if(prevLogo.exists()){
            prevLogo.delete();
            editor.putString("headerlogo", "");
            editor.commit();
        }
//        String DownloadUrl = "https://thenationonlineng.net/wp-content/uploads/2019/03/First-Bank.jpg";
        DownloadManager.Request request1 = new DownloadManager.Request(Uri.parse(DownloadUrl));
        request1.setDescription("Downloading Header Logo");   //appears the same in Notification bar while downloading
        request1.setTitle("ic_header_logo.bmp");
        request1.setVisibleInDownloadsUi(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request1.allowScanningByMediaScanner();
            request1.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        }
        request1.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request1.setDestinationInExternalPublicDir("/EfuPay/assets/logo", "ic_header_logo.bmp");

        DownloadManager manager1 = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Long reference  = manager1.enqueue(request1);
        Log.d("Download Reference", reference.toString());
        if (DownloadManager.STATUS_SUCCESSFUL == 8) {
            String path = "/storage/emulated/0/EfuPay/assets/logo/ic_header_logo.bmp";
            saveImagePath(path);
            finish();
        }
    }

    public void pickLogoFromLocalStorage(View view){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//        absolute file path;
        String pictureDirectoryPathAbs = pictureDirectory.getAbsolutePath();
        Uri data = Uri.parse(pictureDirectoryPathAbs);
        photoPickerIntent.setDataAndType(data, "image/*");
        startActivityForResult(photoPickerIntent, IMAGE_GALLERY_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_GALLERY_REQUEST){
                Uri imageUri = data.getData();
                InputStream inputStream;
                try {
                    inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap image = decodeStream(inputStream);
//                    get real path of the image from the URI
                    String realPath = getRealPathFromURI(this, imageUri);
//                    set the image src to the selected image file from storage
                    recImagePrev.setImageBitmap(image);
//                    pathPlaceholder.setText(realPath);
//                    save the real image path to share preferences
                    saveImagePath(realPath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error reading Image File", Toast.LENGTH_SHORT);
                }
            }
        }
    }

    public void saveImagePath(String headerlogo) {
        editor.putString("headerlogo", headerlogo);
        editor.commit();
        Log.d("Receipt Logo", headerlogo);
        Toast.makeText(this, "Receipt Logo changed", Toast.LENGTH_SHORT).show();
    }

    private String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            TAG = "Path Error";
            Log.e(TAG, "getRealPathFromURI Exception : " + e.toString());
            Toast.makeText(this, "Path Error", Toast.LENGTH_SHORT).show();
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }
}
