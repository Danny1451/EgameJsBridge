package com.example.liudan.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.example.liudan.myapplication.Bridge.EGWebViewClient;
import com.example.liudan.myapplication.Bridge.JsResponseCallback;
import com.example.liudan.myapplication.Bridge.JsBridgeHandler;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    WebView webView;
    Button btnCallHandler;
    Button btnReload;

    MyWebViewClient client;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView)findViewById(R.id.webView);
        btnCallHandler = (Button)findViewById(R.id.btnCallHandler);
        btnReload = (Button)findViewById(R.id.btnReload);

        client = new MyWebViewClient(webView);
        client.enableLogging();
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(client);

        webView.loadUrl("file:///android_asset/ExampleApp.html");


        btnReload.setOnClickListener(this);
        btnCallHandler.setOnClickListener(this);
    }


    class MyWebViewClient extends EGWebViewClient {
        public MyWebViewClient(WebView webView) {

            // support js send
            super(webView, new JsBridgeHandler() {

                @Override
                public void request(Object data, JsResponseCallback callback) {
                    Toast.makeText(MainActivity.this, "ObjC Received message from JS:" + data, Toast.LENGTH_LONG).show();
                    callback.callback("Response for message from ObjC!");
                }
            });

            enableLogging();

            registerHandler("testObjcCallback", new JsBridgeHandler() {

                @Override
                public void request(Object data, JsResponseCallback callback) {
                    Toast.makeText(MainActivity.this, "testObjcCallback called:" + data, Toast.LENGTH_LONG).show();
                    callback.callback("Android 收到了你的消息!");
                }
            });

            send("A string sent from ObjC before Webview has loaded.", new JsResponseCallback() {

                @Override
                public void callback(Object data) {
                    Toast.makeText(MainActivity.this, "ObjC got response! :" + data, Toast.LENGTH_LONG).show();
                }
            });

            try {
                callHandler("testJavascriptHandler", new JSONObject("{\"foo\":\"before ready\" }"),new JsResponseCallback() {

                    @Override
                    public void callback(Object data) {
                        Toast.makeText(MainActivity.this, "ObjC call testJavascriptHandler got response! :" + data, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

    }
    @Override
    public void onClick(View v) {
        if (btnCallHandler.equals(v)) {


                client.callHandler("testJavascriptHandler", "你好 我是android", new JsResponseCallback() {
                    @Override
                    public void callback(Object responseData) {
                        Toast.makeText(MainActivity.this, "testJavascriptHandler responded: " + responseData, Toast.LENGTH_LONG).show();
                    }
                });


        }

        if (btnReload.equals(v)){

            webView.reload();
        }

    }
}
