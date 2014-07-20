package com.vibin.billy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

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


public class SongsFragment extends ListFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    ArrayList<ProcessingTask.BillyData> mData;
    String[] billySong, result;
    View v;
    CustomBaseAdapter customBaseAdapter;
    CustomDatabaseAdapter customDatabaseAdapter;
    SwingBottomInAnimationAdapter swingBottomInAnimationAdapter;
    RequestQueue req;
    BillyApplication billyapp;
    ProcessingTask ft;
    String uri, searchparam,table_name,rssurl;
    ImageLoader imgload;
    SwipeRefreshLayout swipelayout;
    int mIndex, billySize, onlyOnce, position;
    boolean pulltorefresh;
    final long ANIMATION_DELAY = 200;
    final long ANIMATION_DURATION = 350;

    private String tag = SongsFragment.class.getSimpleName(); // Tag is not final
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        position = getArguments().getInt("position");
        tag = tag.substring(0,tag.length()-1)+Integer.toString(position);
        billyapp = (BillyApplication) getActivity().getApplication();
        billySize = billyapp.getBillySize();
        imgload = billyapp.getImageLoader();
        mData = new ArrayList<ProcessingTask.BillyData>(billySize);
        while (mData.size() < billySize) {
            mData.add(new ProcessingTask.BillyData());
        }
        customDatabaseAdapter = new CustomDatabaseAdapter(getActivity());
        setHasOptionsMenu(true);

        table_name = getResources().getStringArray(R.array.table)[position];
        if (!billyapp.isConnected()) {
            Log.d(tag, "No internet connection");
            String jsonMdata = customDatabaseAdapter.getArrayList(table_name);
            Log.d(tag, "jsonMdata is " + jsonMdata);
            Gson gson = new Gson();
            mData = gson.fromJson(jsonMdata, new TypeToken<ArrayList<ProcessingTask.BillyData>>() {
            }.getType());
        }

        //Spawn requests only on new Instance
        if (savedInstanceState == null && billyapp.isConnected()) {
            // Log.d(tag, "Instance is null");
            rssurl = getResources().getStringArray(R.array.url)[position];
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
        v = inflater.inflate(R.layout.fragment_songs, container, false);
        customBaseAdapter = new CustomBaseAdapter(getActivity(), mData, imgload);
        mIndex = 0;
        swipelayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        swipelayout.setOnRefreshListener(this);
        swipelayout.setColorScheme(android.R.color.holo_blue_bright,
                R.color.billyred,
                android.R.color.holo_orange_light,
                R.color.billygreen);

        // Restore instance, on Orientation change
        if (savedInstanceState != null) {
            Log.d(tag, "Instance is not NULL!");
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

        swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(customBaseAdapter, ANIMATION_DELAY, ANIMATION_DURATION);

        // Assign the ListView to the AnimationAdapter and vice versa
        swingBottomInAnimationAdapter.setAbsListView(getListView());
        getListView().setAdapter(swingBottomInAnimationAdapter);
        swingBottomInAnimationAdapter.setShouldAnimate(false);

        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        swingBottomInAnimationAdapter.setShouldAnimate(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(tag, "Saving the instance state!");
        outState.putParcelableArrayList("MDATA", mData);

        // If connected to internet, write to database, but only once
        if (onlyOnce == 0 && billyapp.isConnected()) {
            Gson gson = new Gson();
            String jsonMdata = gson.toJson(mData);
            long yolo = customDatabaseAdapter.insertArrayList(jsonMdata, table_name);
            onlyOnce++;
            Log.d(tag, "Arraylist is serialized and yolo is " + yolo);
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
                    String jsonMdata = customDatabaseAdapter.getArrayList(table_name);
                    if(pulltorefresh)
                    {
                        Log.d(tag, "Pull to refresh");
                        if(!response.equalsIgnoreCase(data)){Toast.makeText(getActivity(),"New songs have loaded",Toast.LENGTH_LONG).show();}
                        handleXML(response);
                        pulltorefresh = false; // Resetting
                    }
                    else if (entry == null || jsonMdata == null) {
                        Log.d(tag, "No cache/DB. Requests made.");
                        handleXML(response);
                    } else if (!response.equalsIgnoreCase(data) || jsonMdata.length() < 100) {
                        Log.d(tag, "New data available or DB is empty. Requests made.");
                        handleXML(response);
                    } else {
                        Log.d(tag, "Cache and response are equal, no requests made");
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
                Log.d(tag, volleyError.toString());
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
            searchparam = ft.paramEncode(billySong[mIndex]);
            uri = getResources().getString(R.string.itunes) + searchparam + getResources().getString(R.string.itunes_params);
            //Log.d(tag, uri);
            JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, uri, null, itunesComplete(), itunesError());
            req.add(jsonreq);
            mIndex++;
        }
        //Log.d(tag, "itunes requests complete!");
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
                        // Log.d(tag, "Match is" +Integer.parseInt(result[4]));
                        mData.get(Integer.parseInt(result[4])).setItunes(result[0], result[1], result[2], result[3]);
                        customBaseAdapter.updateArrayList(mData);
                        customBaseAdapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    Log.d(tag, e + "mIndex is" + mIndex);
                }
            }
        };
    }

    private Response.ErrorListener itunesError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(tag, volleyError.toString());
            }
        };
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (billyapp.isConnected()) {
            ProcessingTask.BillyData billyData = mData.get(i);

            Intent myintent = new Intent(getActivity(), DetailView.class);
            myintent.putExtra("song", billyData.song);
            myintent.putExtra("album", billyData.album);
            myintent.putExtra("artist", billyData.artist);
            myintent.putExtra("artwork", billyData.artwork);
            myintent.putExtra("index",i);
            startActivity(myintent);
        }
        else{
            Toast.makeText(getActivity(),"Please connect to Internet",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRefresh() {
        if (billyapp.isConnected()) {
            pulltorefresh = true;
            swingBottomInAnimationAdapter.reset();
            StringRequest stringreq = new StringRequest(Request.Method.GET, rssurl, billyComplete(), billyError());
            req.add(stringreq);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    swipelayout.setRefreshing(false);
                }
            }, 2500);
        }
        else{
            Toast.makeText(getActivity(),"Please connect to Internet",Toast.LENGTH_LONG).show();
            swipelayout.setRefreshing(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.refresh:
                if (billyapp.isConnected()) {
                    onRefresh();
                    swipelayout.setRefreshing(true);
                    return true;
                }
                else
                {
                    Toast.makeText(getActivity(),"Please connect to Internet",Toast.LENGTH_LONG).show();
                    return false;
                }
        }
        return super.onOptionsItemSelected(item);
    }
}