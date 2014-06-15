package com.vibin.billy;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;


public class Fragment1 extends Fragment {
    ArrayList<FetchTask.BillyData> mData;
    String[] billySong,result;
    ListView lv;
    View v;
    MyCustomAdapter myCustomAdapter;
    RequestQueue req;
    BillyApplication billyapp;
    FetchTask ft;
    String uri, searchparam;
    String rssurl;
    int mIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mData = new ArrayList<FetchTask.BillyData>(24);
        while (mData.size() < 24) {mData.add(new FetchTask.BillyData());}

        rssurl = getResources().getStringArray(R.array.url)[0];
        billySong = new String[24];
        billyapp= (BillyApplication) getActivity().getApplication();
        req = billyapp.getRequestQueue();

        ft = new FetchTask();

        // Get Billboard XML
        StringRequest stringreq = new StringRequest(Request.Method.GET, rssurl, billyComplete(), billyError());

        // Cache for images
        //imgload = new ImageLoader(req, ImageCacheManager.getInstance().getImageLoader());
        req.add(stringreq);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "Size of mdata in fragment1 "+mData.size());
        v = inflater.inflate(R.layout.fragment_1, container, false);
        lv = (ListView) v.findViewById(R.id.listView);
        myCustomAdapter = new MyCustomAdapter(getActivity(),mData);
        lv.setAdapter(myCustomAdapter);
        mIndex = 0;
        return v;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // Listeners for Billboard request
    private Response.Listener<String> billyComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    handleXML(response);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener billyError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(getClass().getName(), volleyError.toString());
            }
        };
    }

    /**
     * Parses XML, populates billySong, spawns requests to iTunes
     * @param response A String containing XML
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void handleXML(String response) throws IOException, XmlPullParserException {
            billySong = ft.parseBillboard(response);

            while (mIndex < billySong.length) {
                searchparam = billySong[mIndex].replaceAll(" ", "+"); // Put Billboard song as iTunes search parameter
                uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
                JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, uri, null, itunesComplete(), itunesError());
                req.add(jsonreq);
                mIndex++;
            }
            Log.d(getClass().getName(), "itunes requests complete!");

    }


    /**
     * Parses JSONObject, populates {@code mData}, notifies the BaseAdapter
     */
    private Response.Listener<JSONObject> itunesComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    result = ft.parseItunes(jsonObject);
                    Log.d(getClass().getName(), "Match is" +Integer.parseInt(result[4]));
                    mData.get(Integer.parseInt(result[4])).setItunes(result[1], result[0], result[2], result[3]);

                    myCustomAdapter.updateArrayList(mData);
                    myCustomAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    Log.d(getClass().getName(), e + "mIndex is" + mIndex);
                }
            }
        };
    }

    private Response.ErrorListener itunesError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(getClass().getName(), volleyError.toString());
            }
        };
    }
}