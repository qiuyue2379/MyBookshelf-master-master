package com.monke.monkeybook.help;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;


public class HttpHelper {
    private static final int REQUEST_TIMEOUT = 15 * 1000;
    private static final int SO_TIMEOUT = 15 * 1000;

    public static String sendHttpRequest(String pUrl) {
        String result;
        result = doGet(pUrl);
        return result;
    }
    public static String doGet(String url) {
        String result = null;
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {

            }
            result = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (IOException e) {
            //Log.i("GET", "Bad Request!");
        }
        return result;
    }
    public static String GetJsonListValue(String str) {
        String strError="";
        try {
            JSONObject json1 = new JSONObject(str);
            JSONObject tmp = json1.getJSONObject("d");
            return tmp.getString("a");
        } catch (Exception e) {
            strError=e.toString();
        }
        return strError;

    }

    public static JSONArray GetJsonValue(String str) {

        try {
            JSONArray json1 = new JSONArray(str);
            return json1;
        } catch (Exception e) {
            //strError=e.toString();
        }
        return null;
    }
}