package com.vibin.billy;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FetchTask extends AsyncTask<String, Void, String>{
    String result;
    private OnTaskCompleted listener;
    private static final String TAG = "FetchTask";
    public FetchTask(OnTaskCompleted listener) {
        super();
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... uri) {

        HttpClient hc = new DefaultHttpClient();
        HttpGet hg = new HttpGet(uri[0]);
        try {
           HttpResponse hr = hc.execute(hg);
           result =  convertInputStreamToString(hr.getEntity().getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String convertInputStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line, result = "";
        try {
            while((line = bufferedReader.readLine()) != null) {
                result += line;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        try {
            listener.onTaskCompleted(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
