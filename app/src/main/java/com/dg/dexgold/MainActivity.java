package com.dg.dexgold;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    String websiteURL = "https://myexamfixer.blogspot.com/";
    private WebView webview;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private ValueCallback<Uri[]> mUploadMessage;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!CheckNetwork.isInternetAvailable(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet")
                    .setMessage("Please check your connection.")
                    .setPositiveButton("Ok", (dialog, which) -> finish()).show();
        } else {
            webview = findViewById(R.id.webView);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.getSettings().setDomStorageEnabled(true);
            webview.getSettings().setAllowFileAccess(true);
            webview.getSettings().setDatabaseEnabled(true);
            webview.setWebViewClient(new WebViewClientDemo());

            // Photo Upload logic
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

            // 100% No Crash Download Logic (Direct Browser Fallback)
            webview.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    Toast.makeText(MainActivity.this, "Downloading in Browser...", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Cannot download this file", Toast.LENGTH_SHORT).show();
                }
            });

            webview.loadUrl(websiteURL);
        }

        mySwipeRefreshLayout = findViewById(R.id.swipeContainer);
        mySwipeRefreshLayout.setOnRefreshListener(() -> webview.reload());
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
