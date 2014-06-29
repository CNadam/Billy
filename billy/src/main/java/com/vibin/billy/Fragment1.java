package com.vibin.billy;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;


public class Fragment1 extends ListFragment implements AdapterView.OnItemClickListener {
    ArrayList<ProcessingTask.BillyData> mData;
    String[] billySong, result;
    ListView lv;
    View v;
    CustomBaseAdapter customBaseAdapter;
    CustomDatabaseAdapter customDatabaseAdapter;
    RequestQueue req;
    BillyApplication billyapp;
    ProcessingTask ft;
    String uri, searchparam;
    String rssurl;
    ImageLoader imgload;
    int mIndex, billySize, onlyOnce;

    private static final String TAG = Fragment1.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billyapp = (BillyApplication) getActivity().getApplication();
        billySize = billyapp.getBillySize();
        imgload = billyapp.getImageLoader();
        mData = new ArrayList<ProcessingTask.BillyData>(billySize);
        while (mData.size() < billySize) {
            mData.add(new ProcessingTask.BillyData());
        }
        customDatabaseAdapter = new CustomDatabaseAdapter(getActivity());

        if (!billyapp.isConnected()) {
            Log.d(TAG, "No internet connection");
            String jsonMdata = customDatabaseAdapter.getArrayList("MostPopular");
            Log.d(TAG, "jsonMdata is " + jsonMdata);
            Gson gson = new Gson();
            mData = gson.fromJson(jsonMdata, new TypeToken<ArrayList<ProcessingTask.BillyData>>() {
            }.getType());
        }

        //Spawn requests only on new Instance
        if (savedInstanceState == null && billyapp.isConnected()) {
            // Log.d(TAG, "Instance is null");
            rssurl = getResources().getStringArray(R.array.url)[0];
            billySong = new String[billySize];
            req = billyapp.getRequestQueue();
            ft = new ProcessingTask(billySize);

            // Get Billboard XML
            StringRequest stringreq = new StringRequest(Request.Method.GET, rssurl, billyComplete(), billyError());
            req.add(stringreq);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_1, container, false);
        customBaseAdapter = new CustomBaseAdapter(getActivity(), mData, imgload);
        mIndex = 0;

        // Restore instance, on Orientation change
        if (savedInstanceState != null) {
            Log.d(TAG, "Instance is not NULL!");
            mData = savedInstanceState.getParcelableArrayList("MDATA");
            customBaseAdapter.updateArrayList(mData);
            customBaseAdapter.notifyDataSetChanged();
        } else if (!billyapp.isConnected()) {
            customBaseAdapter.updateArrayList(mData);
            customBaseAdapter.notifyDataSetChanged();
        }
        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(customBaseAdapter);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Saving the instance state!");
        outState.putParcelableArrayList("MDATA", mData);

        // If connected to internet, write to database, but only once
        if (onlyOnce == 0 && billyapp.isConnected()) {
            Gson gson = new Gson();
            String jsonMdata = gson.toJson(mData);
            long yolo = customDatabaseAdapter.insertArrayList(jsonMdata, "MostPopular");
            onlyOnce++;
            Log.d(TAG, "Arraylist is serialized and yolo is " + yolo);
        }
        super.onSaveInstanceState(outState);
    }


    // Listeners for Billboard request
    private Response.Listener<String> billyComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Cache.Entry entry = req.getCache().get(rssurl);
                    String data = new String(entry.data, "UTF-8");
                    String jsonMdata = customDatabaseAdapter.getArrayList("MostPopular");
                    if (entry == null || jsonMdata == null) {
                        Log.d(getClass().getSimpleName(), "No cache/DB. Requests made.");
                        handleXML(response);
                    } else if (!response.equalsIgnoreCase(data) || customDatabaseAdapter.getArrayList("MostPopular").length() < 100) {
                        Log.d(getClass().getSimpleName(), "New data available or DB is empty. Requests made.");
                        handleXML(response);
                    } else {
                        Log.d(getClass().getName(), "Strings are equal, no requests made");
                        jsonMdata = customDatabaseAdapter.getArrayList("MostPopular");
                        Gson gson = new Gson();
                        mData = gson.fromJson(jsonMdata, new TypeToken<ArrayList<ProcessingTask.BillyData>>() {
                        }.getType());
                        customBaseAdapter.updateArrayList(mData);
                        customBaseAdapter.notifyDataSetChanged();
                    }
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
                Log.d(TAG, volleyError.toString());
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
            //searchparam = billySong[mIndex].replaceAll(" ", "+"); // Put Billboard song as iTunes search parameter
            searchparam = ft.paramEncode(billySong, mIndex); // Put Billboard song as iTunes search parameter
            uri = getResources().getStringArray(R.array.url)[4] + searchparam + getResources().getStringArray(R.array.url)[5];
            //Log.d(TAG, uri);
            JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, uri, null, itunesComplete(), itunesError());
            req.add(jsonreq);
            mIndex++;
        }
        //Log.d(TAG, "itunes requests complete!");
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
                        // Log.d(TAG, "Match is" +Integer.parseInt(result[4]));
                        mData.get(Integer.parseInt(result[4])).setItunes(result[0], result[1], result[2], result[3]);

                        customBaseAdapter.updateArrayList(mData);
                        customBaseAdapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    Log.d(TAG, e + "mIndex is" + mIndex);
                }
            }
        };
    }

    private Response.ErrorListener itunesError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(TAG, volleyError.toString());
            }
        };
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TextView song = (TextView) view.findViewById(R.id.song);
        TextView album = (TextView) view.findViewById(R.id.album);
        TextView artist = (TextView) view.findViewById(R.id.artist);

        Intent myintent = new Intent(getActivity(), DetailView.class);
        myintent.putExtra("song", song.getText().toString());
        myintent.putExtra("album", album.getText().toString());
        myintent.putExtra("artist", artist.getText().toString());
        startActivity(myintent);
    }
}