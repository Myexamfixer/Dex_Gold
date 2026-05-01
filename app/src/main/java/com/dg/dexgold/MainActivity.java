package com.dg.dexgold;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog; // Loading க்காக
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler; // தாமதப்படுத்த (Delay)
import android.util.Base64;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    String websiteURL = "https://myexamfixer.blogspot.com/"; 
    private WebView webview;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;
    private String tempUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkStartupPermission();

        if (!CheckNetwork.isInternetAvailable(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet")
                    .setMessage("Please check your connection.")
                    .setPositiveButton("Ok", (dialog, which) -> finish()).show();
        } else {
            initWebView();
        }

        mySwipeRefreshLayout = findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(() -> webview.reload());
    }

    private void checkStartupPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 101);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            }
        }
    }

    private void initWebView() {
        webview = findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setDatabaseEnabled(true);
        webview.setWebViewClient(new WebViewClientDemo());

        webview.setWebChromeClient(new WebChromeClient() {
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mUploadMessage != null) mUploadMessage.onReceiveValue(null);
                mUploadMessage = filePathCallback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "Select Image"), FILECHOOSER_RESULTCODE);
                return true;
            }
        });

        webview.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            tempUrl = url;
            processDownload();
        });

        webview.loadUrl(websiteURL);
    }

    private void processDownload() {
        if (tempUrl.startsWith("data:")) {
            // Loading திரையைக் காட்டி இமேஜைச் சேமிக்கும்
            downloadBase64Image(tempUrl);
        } else {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(tempUrl));
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadBase64Image(String base64String) {
        // 2. Loading டயலாக் உருவாக்குதல்
        final ProgressDialog pd = new ProgressDialog(MainActivity.this);
        pd.setMessage("Optimizing & Saving Image...");
        pd.setCancelable(false);
        pd.show();

        // ஒரு சிறிய Delay (3 வினாடிகள்) - விளம்பரம் காட்ட அல்லது லோடிங் தெரிய
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String imageData = base64String.substring(base64String.indexOf(",") + 1);
                    byte[] decodedBytes = Base64.decode(imageData, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    String fileName = "MyExam_" + System.currentTimeMillis() + ".jpg";
                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(path, fileName);

                    OutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(file));
                    sendBroadcast(mediaScanIntent);

                    pd.dismiss(); // லோடிங் முடிந்தது
                    Toast.makeText(MainActivity.this, "Image Saved to Gallery!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Download Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        }, 3000); // 3000ms = 3 வினாடிகள் லோடிங் ஆகும்
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILECHOOSER_RESULTCODE && mUploadMessage != null) {
            Uri[] results = (resultCode == RESULT_OK && intent != null) ? new Uri[]{intent.getData()} : null;
            mUploadMessage.onReceiveValue(results);
            mUploadMessage = null;
        }
    }

    private class WebViewClientDemo extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mySwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) webview.goBack();
        else finish();
    }
}

class CheckNetwork {
    public static boolean isInternetAvailable(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
