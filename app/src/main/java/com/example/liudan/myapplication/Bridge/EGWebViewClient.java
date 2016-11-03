package com.example.liudan.myapplication.Bridge;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.os.Build;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liudan on 16/6/23.
 */
public class EGWebViewClient extends WebViewClient{

    private static boolean logging = false;
    private static final String kTag = "EGWebBridge";
    private static final String kInterface = kTag + "Interface";
    private static final String kCustomProtocolScheme = "egamescheme";
    private static final String kQueueHasMessage = "__EGAME_QUEUE_MESSAGE__";
    private static final String kBridgeLoaded = "__BRIDGE_LOADED__";


    private WebView webView;

    private ArrayList<JsBridgeMsg> startupMessageQueue = null; //消息的队列
    private Map<String, JsResponseCallback> responseCallbacks = null; //回调的map
    private Map<String, JsBridgeHandler> messageHandlers = null; //handler的map
    private long uniqueId = 0; //唯一的消息id

    private JsBridgeHandler messageHandler;

    private MyJavascriptInterface myInterface = new MyJavascriptInterface();


    public EGWebViewClient(WebView webView){
        this(webView, null);
    }

    public EGWebViewClient(WebView webView , JsBridgeHandler messageHandler){
        this.webView = webView;
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.addJavascriptInterface(myInterface, kInterface);
        this.startupMessageQueue = new ArrayList<JsBridgeMsg>();
        this.messageHandlers = new HashMap<String,JsBridgeHandler>();
        this.responseCallbacks = new HashMap<String, JsResponseCallback>();
        this.messageHandler = messageHandler;
    }

    public void reset(){

    }

    public void enableLogging() {
        logging = true;
    }

    ///调用Js的相关方法
    public void callHandler(String handlerName){

        this.callHandler(handlerName, null, null);
    }

    public void callHandler(String handlerName, Object data) {

        this.callHandler(handlerName, data, null);
    }

    public void callHandler(String handlerName, Object data, JsResponseCallback callback) {

        this.sendData(handlerName, data, callback);
    }

    public String callSynHandler(String handlerName, Object data){



        callHandler(handlerName, data, new JsResponseCallback() {
            @Override
            public void callback(Object responseData) {

            }
        });


    }




    public void send(Object data){
        this.send(data, null);
    }

    public void send(Object data , JsResponseCallback callback){
        this.sendData(null, data, callback);
    }

    ///接收来自Js的消息

    public void registerHandler(String handlerName, JsBridgeHandler handler){

        if (TextUtils.isEmpty(handlerName) || handler == null){
            return;
        }
        messageHandlers.put(handlerName, handler);

    }

    ///清空mess的队列
    private void flushMessageQueue(){
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {

            String script = "WebViewJavascriptBridge._fetchQueue()";

            executeJavascript(script, new JavascriptCallback() {
                @Override
                public void onReceiveValue(String responseData) {


                    if (TextUtils.isEmpty(responseData)){
                        return;
                    }

                    //解析来自js的消息
                    try {

                        JSONArray messages = new JSONArray(responseData);
                        for (int i = 0 ; i < messages.length(); i++){
                            JSONObject jo = messages.getJSONObject(i);

                            log("RCVD" ,jo);

                            JsBridgeMsg msg = JsBridgeMsg.toObject(jo.toString());

                            if (!TextUtils.isEmpty(msg.responseId)){

                                //若该条消息是回调消息 则 回调
                                JsResponseCallback responseCallback = responseCallbacks.remove(msg.responseId);
                                if (responseCallback != null){

                                    responseCallback.callback(msg.responseData);

                                }

                            }else {

                                //如果不是回调消息的话 应该
                                JsResponseCallback responseCallback = null;
                                final String callBackId = msg.callbackId;
                                if (!TextUtils.isEmpty(callBackId)){

                                    responseCallback = new JsResponseCallback() {

                                        @Override
                                        public void callback(Object responseData) {
                                            JsBridgeMsg message = new JsBridgeMsg();
                                            message.responseId = callBackId;
                                            message.responseData = responseData;
                                            queueMessage(message);

                                        }
                                    };

                                }

                                JsBridgeHandler handler;
                                if (!TextUtils.isEmpty(msg.handlerName)){
                                    handler = messageHandlers.get(msg.handlerName);
                                }else {
                                    //没有handler的话 回调默认的handler 名字
                                    handler = messageHandler;
                                }

                                if (handler != null){
                                    handler.request(msg.data, responseCallback);
                                }
                            }

                        }

                    }catch (JSONException e){
                        e.printStackTrace();

                    }


                }
            });


        }
    }

    //重写跳转的方法 过滤掉 交互的 方法
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        if (view != webView){
            return true;
        }

        log(" should Load",url);

        //防止编码不正确
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //判断是否是约定好的开头网页
        if (url.startsWith(kCustomProtocolScheme)){

            if (url.indexOf(kBridgeLoaded) > 0){
                injectJavascriptFile();
            }else if (url.indexOf(kQueueHasMessage) > 0){
                flushMessageQueue();
            }else{
                logUnknowMessage(url);
            }

            return true;
        }


        return super.shouldOverrideUrlLoading(view, url);
    }

    ///注入js文件
    private void injectJavascriptFile(){
        try {
            InputStream is = webView.getContext().getAssets()
                    .open("WebViewJavascriptBridge.js.txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String js = new String(buffer);
            executeJavascript(js);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    ///未知消息
    private void logUnknowMessage(String msg){
        log("WARNING",msg );
    }

    @Override
    public void onPageFinished(WebView view, String url) {




        if (startupMessageQueue != null){
            for (JsBridgeMsg m : startupMessageQueue){
                dispatchMessage(m);
            }
            startupMessageQueue = null;
        }

        super.onPageFinished(view, url);

    }

    //向js发送消息
    private void sendData(String handlerName, Object data, JsResponseCallback callback){

        if (data == null && TextUtils.isEmpty(handlerName)){
            return;
        }

        JsBridgeMsg msg = new JsBridgeMsg();

        if (data!= null){
            msg.data = data;
        }

        if (callback != null){
            String callBackId = "objc_cb_" + (++uniqueId);
            responseCallbacks.put(callBackId,callback);
            msg.callbackId = callBackId;
        }
        if (!TextUtils.isEmpty(handlerName)){

            msg.handlerName = handlerName;

        }

        queueMessage(msg);


    }

    //消息加入到队列中
    private void queueMessage(JsBridgeMsg msg){
        if (startupMessageQueue != null){
            startupMessageQueue.add(msg);
        }else {
            dispatchMessage(msg);
        }
    }

    //向js发送消息
    private void dispatchMessage(JsBridgeMsg msg){

        String messageJson = msg.toJson();


        messageJson.toString()
                .replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"")
                .replaceAll("\'", "\\\\\'").replaceAll("\n", "\\\\\n")
                .replaceAll("\r", "\\\\\r").replaceAll("\f", "\\\\\f");
        log("SEND", messageJson);
        String javascriptCommand = "WebViewJavascriptBridge._handleMessageFromObjC('" + messageJson + "');";

        //判断是否在主线程
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {

            executeJavascript(javascriptCommand);
        }else {
            log("SEND", "not in the main thread");
        }

    }



    public void executeJavascript(String script) {
        executeJavascript(script, null);
    }

    public void executeJavascript(final String script,
                                  final JavascriptCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
//                    log("RECEIVE", "msg =" + value + " js = " + script);

                    if (callback != null) {
                        if (value != null && value.startsWith("\"")
                                && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1)
                                    .replaceAll("\\\\", "");
                        }
                        callback.onReceiveValue(value);
                    }
                }
            });
        } else {
            if (callback != null) {
                myInterface.addCallback(++uniqueId + "", callback);
                webView.loadUrl("javascript:window." + kInterface
                        + ".onResultForScript(" + uniqueId + "," + script + ")");
            } else {
                webView.loadUrl("javascript:" + script);
            }
        }
    }


    //日志方法
    void log(String action, Object json) {
        if (!logging)
            return;
        String jsonString = String.valueOf(json);
        if (jsonString.length() > 500) {
            Log.i(kTag, action + ": " + jsonString.substring(0, 500) + " [...]");

        } else {
            Log.i(kTag, action + ": " + jsonString);
        }
    }

    private class MyJavascriptInterface {
        Map<String, JavascriptCallback> map = new HashMap<String, JavascriptCallback>();

        public void addCallback(String key, JavascriptCallback callback) {
            map.put(key, callback);
        }

        @JavascriptInterface
        public void onResultForScript(String key, String value) {
            Log.i(kTag, "onResultForScript: " + value);
            JavascriptCallback callback = map.remove(key);
            if (callback != null)
                callback.onReceiveValue(value);
        }
    }

    public interface JavascriptCallback {
        public void onReceiveValue(String value);
    };


}
