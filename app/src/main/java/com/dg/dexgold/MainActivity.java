package com.dg.dexgold;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    // உங்கள் இணையதள முகவரி
    String websiteURL = "https://myexamfixer.blogspot.com/"; 
    private WebView webview;
    SwipeRefreshLayout mySwipeRefreshLayout;
    
    // இமேஜ் அப்லோடுக்காக
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // இணைய இணைப்பு சோதித்தல்
        if (!CheckNetwork.isInternetAvailable(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Please check your mobile data or Wifi.")
                    .setPositiveButton("Ok", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
        } else {
            initWebView();
        }

        // Swipe to Refresh வசதி
        mySwipeRefreshLayout = findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(() -> webview.reload());
        
        // தேவையான அனுமதிகளைக் கேட்டல் (Permissions)
        checkAppPermissions();
    }

    private void initWebView() {
        webview = findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setDatabaseEnabled(true);
        webview.getSettings().setAllowContentAccess(true);
        webview.setWebViewClient(new WebViewClientDemo());

        // இமேஜ் அப்லோட் லாஜிக் (கேலரி திறக்க)
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

        // 100% கிராஷ் ஆகாத டவுன்லோட் வசதி (Browser Fallback)
        webview.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                Toast.makeText(MainActivity.this, "Downloading in Browser...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error: Cannot download this file", Toast.LENGTH_SHORT).show();
            }
        });

        webview.loadUrl(websiteURL);
    }

    private void checkAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, 
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 1);
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
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("EXIT")
                    .setMessage("Are you sure you want to close the app?")
                    .setPositiveButton("Yes", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
        }
    }
}

class CheckNetwork {
    public static boolean isInternetAvailable(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
