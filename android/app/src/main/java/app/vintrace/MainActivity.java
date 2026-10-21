package app.vintrace;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        WebView.setWebContentsDebuggingEnabled((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0B1020);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // NHTSA's firewall rejects some device user agents (e.g. emulators); use a plain one
        s.setUserAgentString("Mozilla/5.0 (Linux; Android) VinTrace/1.0");
        s.setAllowFileAccess(true);
        // The page is a local asset that calls the NHTSA APIs directly
        s.setAllowUniversalAccessFromFileURLs(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if ("file".equals(uri.getScheme())) return false;
                // Outside links (NICB, NHTSA) open in the phone's browser
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                Log.i("VinTrace", m.message() + " (" + m.sourceId() + ":" + m.lineNumber() + ")");
                return true;
            }
        });
        // Lets the page's "Print report" button open Android's print / save-as-PDF screen
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void print(final String title) {
                runOnUiThread(() -> {
                    PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                    String job = (title == null || title.isEmpty()) ? "VinTrace report" : title;
                    pm.print(job, web.createPrintDocumentAdapter(job), new PrintAttributes.Builder().build());
                });
            }
        }, "AndroidPrint");
        setContentView(web);
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
