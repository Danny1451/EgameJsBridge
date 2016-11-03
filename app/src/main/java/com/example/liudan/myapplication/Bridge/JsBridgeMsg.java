package com.example.liudan.myapplication.Bridge;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by liudan on 16/6/23.
 */
public class JsBridgeMsg {
    public String callbackId; //callbackId
    public String responseId; //responseId
    public Object responseData; //responseData
    public Object data; //data of message
    public String handlerName; //name of handler

    private final static String CALLBACK_ID_STR = "callbackId";
    private final static String RESPONSE_ID_STR = "responseId";
    private final static String RESPONSE_DATA_STR = "responseData";
    private final static String DATA_STR = "data";
    private final static String HANDLER_NAME_STR = "handlerName";

    public String toJson(){
        JSONObject jo = new JSONObject();

        try {
            jo.put(CALLBACK_ID_STR, callbackId);
            jo.put(DATA_STR, data);
            jo.put(HANDLER_NAME_STR, handlerName);
            jo.put(RESPONSE_DATA_STR, responseData);
            jo.put(RESPONSE_ID_STR, responseId);
            return jo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static JsBridgeMsg toObject(String jsonStr) {
        JsBridgeMsg m =  new JsBridgeMsg();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            m.handlerName = jsonObject.has(HANDLER_NAME_STR) ? jsonObject.getString(HANDLER_NAME_STR) : null;
            m.callbackId = jsonObject.has(CALLBACK_ID_STR) ? jsonObject.getString(CALLBACK_ID_STR) : null;
            m.responseData = jsonObject.has(RESPONSE_DATA_STR) ? jsonObject.getString(RESPONSE_DATA_STR) : null;
            m.responseId = jsonObject.has(RESPONSE_ID_STR) ? jsonObject.getString(RESPONSE_ID_STR) : null ;
            m.data = jsonObject.has(DATA_STR) ? jsonObject.getString(DATA_STR) : null;
            return m;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return m;
    }
}
