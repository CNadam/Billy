package com.vibin.billy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Fetches songs' data and puts that in a Listview. This goes inside the Viewpager.
 */
public class SongsFragment extends ListFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    ArrayList<ProcessingTask.BillyData> mData, mDataLite;
    String[] billySong, billyArtist, result;
    String jsonMdata, billboardResponse;
    LinearLayout spinner;
    View v;
    boolean spinnerVisible, pulltorefresh, isHot100;
    CustomBaseAdapter customBaseAdapter;
    CustomDatabaseAdapter customDatabaseAdapter;
    SwingBottomInAnimationAdapter swingBottomInAnimationAdapter;
    RequestQueue req;
    BillyApplication billyapp;
    ProcessingTask ft;
    String uri, searchparam, table_name, rssurl;
    ImageLoader imgload;
    SwipeRefreshLayout swipelayout;
    int mIndex, billySize, onlyOnce, position;
    final long ANIMATION_DELAY = 200;
    final long ANIMATION_DURATION = 350;

    private String tag = SongsFragment.class.getSimpleName(); // Tag is not final, because it's dynamic

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        position = getArguments().getInt("position");
        tag = tag.substring(0, tag.length() - 1) + Integer.toString(position);
        billyapp = (BillyApplication) getActivity().getApplication();
        billySize = billyapp.getBillySize();
        imgload = billyapp.getImageLoader();
        req = billyapp.getRequestQueue();

        rssurl = getResources().getStringArray(R.array.url)[position];
        table_name = getResources().getStringArray(R.array.table)[position];
        if (table_name.equals("MostPopular")) {
            isHot100 = true;
            billySize = 100;
        }

        billySong = new String[billySize];
        billyArtist = new String[billySize];
        ft = new ProcessingTask(billySize, getActivity());
        mData = new ArrayList<ProcessingTask.BillyData>(billySize);
        mDataLite = new ArrayList<ProcessingTask.BillyData>(billyapp.getBillySize());
        initializeArraylistitems();

        customDatabaseAdapter = new CustomDatabaseAdapter(getActivity());
        setHasOptionsMenu(true);

        if (!billyapp.isConnected()) {
            Log.d(tag, "No internet connection");
            jsonMdata = customDatabaseAdapter.getArrayList(table_name);
            //Log.d(tag, "jsonMdata is " + jsonMdata);
            if (jsonMdata != null) {
                Gson gson = new Gson();
                mData = gson.fromJson(jsonMdata, new TypeToken<ArrayList<ProcessingTask.BillyData>>() {
                }.getType());
            }
        }

        //Spawn requests only on new Instance
        if (savedInstanceState == null && billyapp.isConnected()) {
            performRequests();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_songs, container, false);
        if (jsonMdata == null && !billyapp.isConnected()) {
            v.findViewById(android.R.id.list).setVisibility(View.GONE);
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle("Connection error");
            alertDialogBuilder
                    .setMessage("Please connect to Internet.")
                    .setCancelable(false)
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            if (!((Activity) getActivity()).isFinishing()) {
                alertDialog.show();
            }
        }
        customBaseAdapter = new CustomBaseAdapter(getActivity(), mData, imgload);
        spinner = (LinearLayout) v.findViewById(R.id.spinner);
        if (spinnerVisible) {
            spinner.setVisibility(View.VISIBLE);
        } else {
            v.findViewById(android.R.id.list).setVisibility(View.VISIBLE);
        }
        mIndex = 0;
        swipelayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        swipelayout.setOnRefreshListener(this);
        swipelayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                R.color.billyred,
                android.R.color.holo_orange_light,
                R.color.billygreen);

        // Restore instance, on Orientation change
        if (savedInstanceState != null) {
            Log.d(tag, "savedInstanceState is not null");

            mData = savedInstanceState.getParcelableArrayList("MDATA");
            mDataLite = savedInstanceState.getParcelableArrayList("MDATALITE");
            billySong = savedInstanceState.getStringArray("BILLYSONG");
            billyArtist = savedInstanceState.getStringArray("BILLYARTIST");

            ft = new ProcessingTask(billySong, billyArtist, getActivity());
            if (mData == null) {
                Log.d(tag, "mData is null");
                initializeArraylistitems();
                performRequests();
            } else {
                Log.d(tag, "Oncreateview, mData size is " + mData.size());
                customBaseAdapter.updateArrayList(mData);
                customBaseAdapter.notifyDataSetChanged();
            }
        } else if (!billyapp.isConnected()) {
            Log.d(tag, "Oncreateview + no internet connection, mData size is " + mData.size());
            customBaseAdapter.updateArrayList(mData);
            customBaseAdapter.notifyDataSetChanged();
        }
        return v;
    }

    /**
     * Dynamically initialize {@code billyapp.getBillySize()} number of objects in ArrayList
     * Call this before loading additional data into ListView
     * <p/>
     * Create a lighter version of mData, by shallow-copying it to mDataLite
     */

    private void initializeArraylistitems() {
        if (mData.size() == billyapp.getBillySize()) {
            Log.d(tag, "mDataLite is same as mData");
            //mDataLite = mData;
            mDataLite = new ArrayList<ProcessingTask.BillyData>(mData);
            Log.d(tag, "intitialize, size of mDataLite is " + mDataLite.size() + " size of mData is " + mData.size());
        }
        int i = 0;
        while (i < billyapp.getBillySize()) {
            mData.add(new ProcessingTask.BillyData());
            i++;
        }
        Log.d(tag, "new size of mData is " + mData.size() + " and new size of mDataLite is " + mDataLite.size());
    }

    private void performRequests() {
        spinnerVisible = true;
        StringRequest stringreq = new StringRequest(Request.Method.GET, rssurl, billyComplete(), billyError());
        stringreq.setTag("billyreq");
        req.add(stringreq);
    }


    /**
     * Set adapter to list
     * If current screen is Hot 100, then attach a Button as footer view to ListView which loads additional data
     */
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

        if (isHot100) {
            LinearLayout layout = new LinearLayout(getActivity());
            layout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
            final Button loadMore = new Button(getActivity());
            loadMore.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            loadMore.setText("Load more...");
            final float scale = getResources().getDisplayMetrics().density;
            int padding_in_px = (int) (15 * scale + 0.5f);
            loadMore.setPadding(padding_in_px, 0, padding_in_px, 0);
            loadMore.getBackground().setColorFilter(0xffff0000, PorterDuff.Mode.SRC_ATOP);
            layout.addView(loadMore);
            getListView().addFooterView(layout);
            try {
                loadMore.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                            loadMore.getBackground().setColorFilter(0xffc40000, PorterDuff.Mode.SRC_ATOP);
                        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            loadMore.getBackground().setColorFilter(0xffff0000, PorterDuff.Mode.SRC_ATOP);
                            if (billyapp.isConnected()) {
                                Log.d(tag, "Size is " + mData.size());
                                if (mData.size() == 80) {
                                    getListView().removeFooterView(loadMore);
                                }
                                initializeArraylistitems();
                                customBaseAdapter.updateArrayList(mData);
                                customBaseAdapter.notifyDataSetChanged();

                                for (int i = 0; i < billyapp.getBillySize(); i++) {
                                    callitunes(mData.size() - billyapp.getBillySize() + i, false);
                                }
                            } else {
                                Toast.makeText(getActivity(), billyapp.getString(R.string.nointernet), Toast.LENGTH_LONG).show();
                            }
                        }
                        return true;
                    }
                });
            } catch (ArrayIndexOutOfBoundsException e) {
                getListView().removeFooterView(loadMore);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        swingBottomInAnimationAdapter.setShouldAnimate(false);
        //PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(myPrefListner);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(myPrefListner);
    }

    /**
     * Save mData to Bundle, to reuse on rotate
     * If connected to internet, write mDataLite to database, but only once per instance
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("MDATA", mData);
        outState.putStringArray("BILLYSONG", billySong);
        outState.putStringArray("BILLYARTIST", billyArtist);
        if (mDataLite != null) {
            outState.putParcelableArrayList("MDATALITE", mDataLite);
        }

        if (mData.size() > billyapp.getBillySize()) {
            boolean b = saveToDB(mDataLite);
            Log.d(tag, "mDataLite is saved to DB? " + b + " size is " + mDataLite.size() + " and mData size is " + mData.size());
        } else {
            boolean b = saveToDB(mData);
            Log.d(tag, "mData is saved to DB? " + b + " size is " + mData.size());
        }
        super.onSaveInstanceState(outState);
    }

    private boolean saveToDB(ArrayList<ProcessingTask.BillyData> list) {
        if (onlyOnce == 0 && billyapp.isConnected()) {
            if (checkArrayList(list)) {
                Gson gson = new Gson();
                String jsonMdata = gson.toJson(list);
                long yolo = customDatabaseAdapter.insertArrayList(jsonMdata, table_name);
                onlyOnce++;
                if (yolo != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Response listener for Billboard request
     * If response matches cached response, make no new requests, otherwise do.
     */
    private Response.Listener<String> billyComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                spinnerVisible = false;
                billboardResponse = response;
                spinner.setVisibility(View.GONE);
                v.findViewById(android.R.id.list).setVisibility(View.VISIBLE);
                try {
                    Cache.Entry entry = req.getCache().get(rssurl);
                    String data = new String(entry.data, "UTF-8");
                    String jsonMdata = customDatabaseAdapter.getArrayList(table_name);
                    if (pulltorefresh) {
                        Log.d(tag, "Pull to refresh");
                        handleXML(response, true);
                        pulltorefresh = false;
                        if (entry.data != null) {
                            if (!response.equalsIgnoreCase(data)) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.newsongs), Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getActivity(), getResources().getString(R.string.nonewsongs),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    } else if (entry.data == null || jsonMdata == null) {
                        Log.d(tag, "No cache/DB. Requests made.");
                        handleXML(response, true);
                    } else if (!response.equalsIgnoreCase(data) || jsonMdata.length() < 100) {
                        Log.d(tag, "New data available or DB is empty. Requests made.");
                        handleXML(response, true);
                    } else {
                        handleXML(response, false);
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
                } catch (NullPointerException e) {
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
     * Parses XML, populates {@code billySong, billyArtist}, spawns requests to iTunes
     *
     * @param response A String containing XML
     */
    private void handleXML(String response, boolean doItunesRequests) throws IOException, XmlPullParserException {
        billySong = ft.parseBillboard(response);
        billyArtist = ft.getArtists();
        if (doItunesRequests) {
            while (mIndex < billyapp.getBillySize()) {
                callitunes(mIndex, false);
                mIndex++;
            }
        }
    }

    /**
     * Spawn an iTunes request with song and artist Strings
     */
    private void callitunes(int i, boolean invalidateCache) {
        try {
            searchparam = ft.paramEncode(billyArtist[i]) + "+" + ft.paramEncode(billySong[i]);
            uri = getResources().getString(R.string.itunes, searchparam);
            Log.d(tag, uri);
            JsonObjectRequest jsonreq = new JsonObjectRequest(Request.Method.GET, uri, null, itunesComplete(), itunesError());
            if (invalidateCache) {
                //RequestQueue req = Volley.newRequestQueue(getActivity());
                req.getCache().remove(jsonreq.getCacheKey());
                //req.start();
            }
            req.add(jsonreq);
        } catch (NullPointerException e) {
            pulltorefresh = true;
            StringRequest stringreq = new StringRequest(Request.Method.GET, rssurl, billyComplete(), billyError());
            req.add(stringreq);
            e.printStackTrace();
        }
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
                        try {
                            // Log.d(tag, "Match is" +Integer.parseInt(result[4]));
                            mData.get(Integer.parseInt(result[4])).setItunes(result[0], result[1], result[2], result[3]);
                            customBaseAdapter.updateArrayList(mData);
                            customBaseAdapter.notifyDataSetChanged();
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Crashlytics.log(Log.ERROR, tag, "Result object from iTunes is null for this track. " + jsonObject);
                    }
                } catch (JSONException e) {
                    Log.d(tag, e + "");
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

    /**
     * Send all the meta-data to DetailView, as an intent
     */

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        try {
            if (billyapp.isConnected()) {
                ProcessingTask.BillyData billyData = mData.get(i);

                Intent myintent = new Intent(getActivity(), DetailView.class);
                if (billyData.artwork != null) {
                    myintent.putExtra("song", billyData.song);
                    myintent.putExtra("album", billyData.album);
                    myintent.putExtra("artist", billyData.artist);
                    myintent.putExtra("artwork", billyData.artwork);
                    myintent.putExtra("index", i);
                    startActivity(myintent);
                } else {
                    //Toast.makeText(getActivity(), "Tap the card to refresh", Toast.LENGTH_LONG).show();
                    callitunes(i, true);
                    customBaseAdapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getActivity(), billyapp.getString(R.string.nointernet), Toast.LENGTH_LONG).show();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            pulltorefresh = true;
            performRequests(); // mData is null, so perform all requests again and construct it
        }
    }

    /**
     * Pull to refresh, with animation. Using Google's SwipeRefreshLayout.
     */

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
        } else {
            Toast.makeText(getActivity(), billyapp.getString(R.string.nointernet), Toast.LENGTH_LONG).show();
            swipelayout.setRefreshing(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (billyapp.isConnected()) {
                    onRefresh();
                    swipelayout.setRefreshing(true);
                    return true;
                } else {
                    Toast.makeText(getActivity(), billyapp.getString(R.string.nointernet), Toast.LENGTH_LONG).show();
                    return false;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Cancel all on-going requests if screen is rotated
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        req.cancelAll("billyreq");
    }

    /**
     * Check if ArrayList stored in database has null values
     *
     * @return true, if it doesn't have null values
     */
    private boolean checkArrayList(ArrayList<ProcessingTask.BillyData> mData) {
        int i = 0;
        try {
            while (i < mData.size()) {
                if (mData.get(i) == null) {
                    return false;
                } else if (mData.get(i).artwork.isEmpty()) {
                    return false;
                }
                i++;
            }
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    /**
     * If compact cards preference is on, reload listview
     * If high resolution album art is on, update preference in Processing Task
     * spawn all iTunes requests again
     */

    SharedPreferences.OnSharedPreferenceChangeListener myPrefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("compactCards")) {
                ((ListView) v.findViewById(android.R.id.list)).setAdapter(customBaseAdapter);
            } else if (key.equals("albumArtQuality")) {
                ft.getArtworkUrlResolution();
                mIndex = 0;
                while (mIndex < billyapp.getBillySize()) {
                    callitunes(mIndex, false);
                    mIndex++;
                }
            }
        }
    };
}