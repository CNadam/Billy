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
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class Fragment3 extends Fragment {

    ArrayList<FetchTask.BillyData> mData;
    String[] billySong, result;
    ListView lv;
    View v;
    CustomBaseAdapter customBaseAdapter;
    RequestQueue req;
    BillyApplication billyapp;
    FetchTask ft;
    String uri;
    String searchparam = "searchparam";
    String rssurl;
    int mIndex, billySize;
    ImageLoader imgload;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        billyapp = (BillyApplication) getActivity().getApplication();
        imgload = billyapp.getImageLoader();
        billySize = billyapp.getBillySize();
        mData = new ArrayList<FetchTask.BillyData>(billySize);
        while (mData.size() < billySize) {
            mData.add(new FetchTask.BillyData());
        }

        //Spawn requests only on new Instance
        if(savedInstanceState == null) {
            rssurl = getResources().getStringArray(R.array.url)[2];
            billySong = new String[billySize];
            req = billyapp.getRequestQueue();
            ft = new FetchTask(billySize);

            // Get Billboard XML
            StringRequest stringreq = new StringRequest(Request.Method.GET, rssurl, billyComplete(), billyError());
            req.add(stringreq);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Log.d(getClass().getName(), "Size of mdata in fragment3 " + mData.size());
        v = inflater.inflate(R.layout.fragment_3, container, false);
        lv = (ListView) v.findViewById(R.id.listView);
        customBaseAdapter = new CustomBaseAdapter(getActivity(), mData, imgload);
        lv.setAdapter(customBaseAdapter);
        mIndex = 0;

        // Restore instance, on Orientation change
        if(savedInstanceState != null)
        {
            mData = savedInstanceState.getParcelableArrayList("MDATA");
            customBaseAdapter.updateArrayList(mData);
            customBaseAdapter.notifyDataSetChanged();
        }
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(getClass().getName(), "Saving the instance state! Frag 3");
        outState.putParcelableArrayList("MDATA",mData);
        super.onSaveInstanceState(outState);
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
     *
     * @param response A String containing XML
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void handleXML(String response) throws IOException, XmlPullParserException {
        billySong = ft.parseBillboard(response);

        while (mIndex < billySong.length) {
            //Log.d(getClass().getName(), "Size of billysong is "+ billySong.length);
            //Log.d(getClass().getName(), "mIndex in fragment3 "+ mIndex);
            //Log.d(getClass().getName(), "billysong in fragment3 "+ billySong[mIndex]);
            //searchparam = billySong[mIndex].replaceAll(" ", "+"); // Put Billboard song as iTunes search parameter
            searchparam = ft.paramEncode(billySong, mIndex); // Put Billboard song as iTunes search parameter
            Log.d(getClass().getName(), searchparam);
            uri = getResources().getStringArray(R.array.url)[4] + searchparam + getResources().getStringArray(R.array.url)[5];
            Log.d(getClass().getName(), uri);
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
                    if (result != null) {
                        //Log.d(getClass().getName(), "Match is" +Integer.parseInt(result[4]));
                        mData.get(Integer.parseInt(result[4])).setItunes(result[1], result[0], result[2], result[3]);

                        customBaseAdapter.updateArrayList(mData);
                        customBaseAdapter.notifyDataSetChanged();
                    }
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
