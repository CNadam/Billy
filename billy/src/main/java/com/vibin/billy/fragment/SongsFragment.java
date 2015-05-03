package com.vibin.billy.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vibin.billy.BillyApplication;
import com.vibin.billy.BillyItem;
import com.vibin.billy.R;
import com.vibin.billy.activity.DetailView;
import com.vibin.billy.adapter.BaseAdapter;
import com.vibin.billy.adapter.DatabaseAdapter;
import com.vibin.billy.http.JsonObjectRequest;
import com.vibin.billy.http.StringRequest;
import com.vibin.billy.util.ProcessingTask;
import com.vibin.billy.util.SwingBottomInAnimationAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Heart of the app
 * Fetches songs' data and puts that in a Listview. This goes inside a Viewpager.
 */
public class SongsFragment extends ListFragment implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    ArrayList<BillyItem> mData, mDataLite;
    String[] billySong, billyArtist;
    String jsonMdata, cacheData = "";
    LinearLayout spinner;
    View v;
    boolean spinnerVisible, pulltorefresh;
    BaseAdapter baseAdapter;
    DatabaseAdapter databaseAdapter;
    SwingBottomInAnimationAdapter swingBottomInAdapter;
    RequestQueue req;
    BillyApplication billyapp;
    ProcessingTask ft;
    String tableName, rssurl;
    ImageLoader imgload;
    SwipeRefreshLayout swipelayout;
    StringRequest stringreq;
    int mIndex, billySize, onlyOnce;
    final long ANIMATION_DELAY = 200;
    final long ANIMATION_DURATION = 350;

    private String tag = SongsFragment.class.getSimpleName(); // Tag is not final, because it's dynamic

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int position = getArguments().getInt("position");
        tag = tag.substring(0, tag.length() - 1) + Integer.toString(position);
        billyapp = (BillyApplication) getActivity().getApplication();
        imgload = billyapp.getImageLoader();
        req = billyapp.getRequestQueue();

        rssurl = getResources().getStringArray(R.array.url)[position];
        tableName = getResources().getStringArray(R.array.table)[position];

        /**
         * Number of elements in Hot100 chart is 100
         * RnB 15, rest all 20
         */

        if (tableName.equals("MostPopular")) {
            billySize = 100;
        } else if (tableName.equals("RnB")) {
            billySize = 15;
        } else {
            billySize = 20;
        }

        billySong = new String[billySize];
        billyArtist = new String[billySize];
        ft = new ProcessingTask(billySize, getActivity());
        mData = new ArrayList<BillyItem>(billySize);
        mDataLite = new ArrayList<BillyItem>(billyapp.getMinBillySize(billySize));
        initializeArraylistitems();

        databaseAdapter = new DatabaseAdapter(getActivity());
        setHasOptionsMenu(true);

        /**
         * Populate mData beforehand if there's no connection
         */
        if (!billyapp.isConnected()) {
            Log.d(tag, "No internet connection");
            jsonMdata = databaseAdapter.getArrayList(tableName);
            if (jsonMdata != null) {
                Gson gson = new Gson();
                mData = gson.fromJson(jsonMdata, new TypeToken<ArrayList<BillyItem>>() {
                }.getType());
            }
        }

        /**
         * This has to only be in OnCreate and not OnCreateView. Because OnCreateView gets called everytime
         * you switch between Fragments inside a ViewPager.
         */
        if (savedInstanceState == null && billyapp.isConnected()) {
            try {
                performRequests(false);
            } catch (UnsupportedEncodingException e) {
                Log.d(tag, e.toString());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.d(tag, "oncreateview");
        v = inflater.inflate(R.layout.fragment_songs, container, false);

        if (jsonMdata == null && !billyapp.isConnected()) {
            nothingToShow();
        }

        baseAdapter = new BaseAdapter(getActivity(), mData, imgload);
        spinner = (LinearLayout) v.findViewById(R.id.spinner);

        /**
         * Show Spinner or ListView (both hidden by default)
         */

        if (spinnerVisible) {
            spinner.setVisibility(View.VISIBLE);
        } else {
            v.findViewById(android.R.id.list).setVisibility(View.VISIBLE);
        }
        mIndex = 0;
        swipelayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        swipelayout.setOnRefreshListener(this);
        swipelayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                R.color.billy,
                android.R.color.holo_orange_light,
                R.color.green);
        swipelayout.setSize(SwipeRefreshLayout.LARGE);
        //swipelayout.setRefreshing(true);

        /**
         * Restore instance, on Orientation change
         * if instance values are null, spawn requests again
         */

        if (savedInstanceState != null) {
            //Log.d(tag, "savedInstanceState is not null");

            mData = savedInstanceState.getParcelableArrayList("MDATA");
            mDataLite = savedInstanceState.getParcelableArrayList("MDATALITE");
            billySong = savedInstanceState.getStringArray("BILLYSONG");
            billyArtist = savedInstanceState.getStringArray("BILLYARTIST");

            ft.setBillySong(billySong);
            ft.setBillyArtist(billyArtist);
            if (!checkArrayList(mData)) {
                Log.d(tag, "mData or its objects are null");
//                initializeArraylistitems();
                try {
                    performRequests(false);
                } catch (UnsupportedEncodingException e) {
                    Log.d(tag, e.toString());
                }
            } else {
                Log.d(tag, "All songs have loaded, mData size is " + mData.size());
                updateList();
            }
        } else if (!billyapp.isConnected()) {
            Log.d(tag, "Oncreateview + no network, mData size is " + mData.size());
            updateList();
        }
        return v;
    }

    /**
     * When DB is empty and User isn't connected to Internet
     * Show an Alert Dialog and exit app
     * <p/>
     * Make sure to call {@link android.content.DialogInterface#cancel()}, before calling {@link android.app.Activity#finish()}
     */

    private void nothingToShow() {
        final Context c = getActivity();
        v.findViewById(android.R.id.list).setVisibility(View.GONE);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(c);
        alertDialogBuilder.setTitle("No connection");
        alertDialogBuilder
                .setMessage("Please connect to Internet.")
                .setCancelable(false)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        ((Activity) c).finish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        if (!(getActivity()).isFinishing()) {
            alertDialog.show();
        }
    }

    /**
     * Dynamically initialize {@link com.vibin.billy.BillyApplication#getMinBillySize(int)} number of objects in ArrayList
     * Call this before loading additional data into ListView
     * <p/>
     * Create a lighter version of {@link #mData}, by shallow-copying it to mDataLite
     */

    private void initializeArraylistitems() {
        if (mData.size() == billyapp.getMinBillySize(billySize)) {
            //Log.d(tag, "mDataLite is same as mData");
            mDataLite = new ArrayList<BillyItem>(mData);
            //Log.d(tag, "intitialize, size of mDataLite is " + mDataLite.size() + " size of mData is " + mData.size());
        }
        int i = 0;
        while (i < billyapp.getMinBillySize(billySize)) {
            mData.add(new BillyItem());
            i++;
        }
        Log.d(tag, "new size of mData is " + mData.size() + " and new size of mDataLite is " + mDataLite.size());
    }

    /**
     * Spawn Billboard request
     * Make sure to get cached version of Billboard response before making a new request, for comparing later
     */
    private void performRequests(boolean invalidateCache) throws UnsupportedEncodingException {
        stringreq = new StringRequest(rssurl, billyComplete(), billyError());
        spinnerVisible = true;
        Cache.Entry entry = req.getCache().get(rssurl);
        if (entry != null) {
            if (entry.data != null) {
                cacheData = new String(entry.data, "UTF-8");
                //Log.d(tag, "data is " + cacheData.substring(7500,8000)+" isExpired "+entry.isExpired()+" refreshNeeded "+entry.refreshNeeded()+" ttl "+entry.ttl+" softtl "+entry.softTtl);
            }
        }
        stringreq.setTag(this);

        if (invalidateCache) {
            req.getCache().invalidate(stringreq.getCacheKey(), true);
        }
        req.add(stringreq);
    }


    public void updateList() {
        baseAdapter.updateArrayList(mData);
        baseAdapter.notifyDataSetChanged();
    }

    /**
     * Set adapter to list
     * If current screen is Hot 100, then attach a Button as footer view to ListView which loads additional data
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(baseAdapter);

        // Assign the ListView to the AnimationAdapter and vice versa
        swingBottomInAdapter = new SwingBottomInAnimationAdapter(baseAdapter, ANIMATION_DELAY, ANIMATION_DURATION);
        swingBottomInAdapter.setAbsListView(getListView());
        getListView().setAdapter(swingBottomInAdapter);
        swingBottomInAdapter.setShouldAnimate(false);
        getListView().setOnItemClickListener(this);

        if (tableName.equals("MostPopular")) {
            final LinearLayout layout = new LinearLayout(getActivity());
            layout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
            final Button loadMore = new Button(getActivity());
            loadMore.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            loadMore.setText(billyapp.getString(R.string.loadmore));
            layout.addView(loadMore);
            getListView().addFooterView(layout);

            /**
             * mData keeps increasing till {@value billySize} elements (100, in case of Hot100)
             * Footer view removed when mData hits billySize - getMinBillySize(billySize) elements
             */
            loadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (billyapp.isConnected()) {
                        Log.d(tag, "Size is " + mData.size());
                        if (mData.size() == 40) {
                            getListView().setFastScrollEnabled(true);
                        }

                        if (mData.size() >= billySize - billyapp.getMinBillySize(billySize)) {
                            getListView().removeFooterView(loadMore);
                            loadMore.setVisibility(View.GONE); // just a bit precautionary
                        }
                        if (mData.size() < billySize) {
                            initializeArraylistitems();
                            baseAdapter.updateArrayList(mData);
                            baseAdapter.notifyDataSetChanged();

                            for (int i = 0; i < billyapp.getMinBillySize(billySize); i++) {
                                try {
                                    callitunes(mData.size() - billyapp.getMinBillySize(billySize) + i, true);
                                } catch (UnsupportedEncodingException e) {
                                    Log.d(tag, e.toString());
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getActivity(), billyapp.getString(R.string.nointernet), Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        swingBottomInAdapter.setShouldAnimate(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(myPrefListener);
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

        if (mData.size() > billyapp.getMinBillySize(billySize)) {
            boolean b = saveToDB(mDataLite);
            Log.d(tag, "mDataLite is saved to DB? " + b + " size is " + mDataLite.size() + " and mData size is " + mData.size());
        } else {
            boolean b = saveToDB(mData);
            Log.d(tag, "mData is saved to DB? " + b + " size is " + mData.size());
        }
        super.onSaveInstanceState(outState);
    }

    private boolean saveToDB(ArrayList<BillyItem> list) {
        if (onlyOnce == 0 && billyapp.isConnected()) {
            if (checkArrayList(list)) {
                Gson gson = new Gson();
                String jsonMdata = gson.toJson(list);
                long yolo = databaseAdapter.insertArrayList(jsonMdata, tableName);
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
                boolean cacheHit = stringreq.isCacheHit();
                Log.d(tag, "cacheHit? " + cacheHit);
                spinnerVisible = false;
                spinner.setVisibility(View.GONE);
                v.findViewById(android.R.id.list).setVisibility(View.VISIBLE);
                try {
                    String jsonMdata = databaseAdapter.getArrayList(tableName);
                    if (pulltorefresh) {
                        Log.d(tag, "Pull to refresh");
                        mIndex = 0;
                        handleXML(response, true);
                        pulltorefresh = false;
                    } else if (jsonMdata == null) {
                        Log.d(tag, "DB empty. Requests made.");
                        handleXML(response, true);
                    } else if (!cacheHit && !response.equals(cacheData)) {
                        Log.d(tag, "New data available. Requests made.");
                        //Toast.makeText(getActivity(), "New data available. Requests made.",
                        //       Toast.LENGTH_LONG).show();
                        handleXML(response, true);
                    } else {
                        handleXML(response, false);
                        Log.d(tag, "Cache and response are equal, no requests made");
                        Gson gson = new Gson();
                        mData = gson.fromJson(jsonMdata, new TypeToken<ArrayList<BillyItem>>() {
                        }.getType());
                        baseAdapter.updateArrayList(mData);
                        baseAdapter.notifyDataSetChanged();
                    }
                } catch (IOException e) {
                    Log.d(tag, e.toString());
                } catch (XmlPullParserException e) {
                    Log.d(tag, e.toString());
                } catch (NullPointerException e) {
                    Log.d(tag, e.toString());
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
        ft.parseBillboard(response);
        billySong = ft.getBillySong();
        billyArtist = ft.getBillyArtist();
        if (doItunesRequests) {
            while (mIndex < billyapp.getMinBillySize(billySize)) {
                callitunes(mIndex, true);
                mIndex++;
            }
        }
    }

    /**
     * Spawn an iTunes request with song and artist Strings
     */
    private void callitunes(int i, boolean simpleParams) throws UnsupportedEncodingException {
        try {
            if (isAdded()) {
                String searchparam;
                if (simpleParams) {
                    searchparam = billyapp.UTF8(ft.getSimpleString(billySong[i])) + "+" + billyapp.UTF8(ft.getSimpleString(billyArtist[i]));
                } else {
                    Log.d(tag, "trying normal params");
                    searchparam = billyapp.UTF8(billySong[i]) + "+" + billyapp.UTF8(billyArtist[i]);
                String searchparam = "";

                switch (method) {
                    case SIMPLE:
                        searchparam = billyapp.UTF8(ft.getSimpleString(billySong[i])) + "+" + billyapp.UTF8(ft.getSimpleString(billyArtist[i]));
                        createItunesRequest(searchparam, i, method.getValue());
                        break;
                    case NORMAL:
                        Log.d(tag, "trying normal params");
                        searchparam = billyapp.UTF8(billySong[i]) + "+" + billyapp.UTF8(billyArtist[i]);
                        createItunesRequest(searchparam, i, method.getValue());
                        break;
                    case ELIM:
                        JsonArrayRequest me = new JsonArrayRequest(getResources().getString(R.string.mistake), meComplete(i), meError());
                        me.setTag(this);
                        req.add(me);
                        break;
                }
                int simple = simpleParams ? 1 : 0;
                String uri = getResources().getString(R.string.itunes, searchparam) + "&id=" + i + "&simple=" + simple;
                Log.d(tag, uri);
                JsonObjectRequest jsonreq = new JsonObjectRequest(uri, null, itunesComplete(), itunesError());
                jsonreq.setTag(this);
                req.add(jsonreq);
/*                if (invalidateCache) {
                    req.getCache().invalidate(jsonreq.getCacheKey(), true);
                }*/
            }
        } catch (NullPointerException e) {
            Log.d(tag, e.toString());
            onRefresh(); // Simulate a pull to refresh
        }
    }

    private Response.Listener<JSONArray> meComplete(final int i) {
        return new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    for (int z = 0; z < response.length(); z++) {
                        JSONObject obj = response.getJSONObject(z);
                        if (obj.getString("actual").equals(billySong[i])) {
                            billySong[i] = obj.getString("correctedSong");
                            billyArtist[i] = obj.getString("correctedArtist");
                            ft.setBillySong(billySong);
                            ft.setBillyArtist(billyArtist);
                            callitunes(i, ItunesParamType.NORMAL);
                        }
                    }
                } catch (JSONException e) {

                } catch (UnsupportedEncodingException e1) {
                }
            }
        };
    }

    private Response.ErrorListener meError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d(tag, volleyError.toString());
            }
        };
    }

    private void createItunesRequest(String searchparam, int i, int method) {
        String uri = getResources().getString(R.string.itunes, searchparam) + "&id=" + i + "&method=" + method;
        Log.d(tag, uri);
        JsonObjectRequest jsonreq = new JsonObjectRequest(uri, null, itunesComplete(), itunesError());
        jsonreq.setTag(this);
        req.add(jsonreq);
    }

    /**
     * Parses JSONObject, populates {@code mData}, notifies the BaseAdapter
     * <p/>
     * Re-requests iTunes with no parameter simplification, if result is null
     */
    private Response.Listener<JSONObject> itunesComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    String url = jsonObject.getString("url");
                    int id = Integer.parseInt(url.substring(url.indexOf("id=") + 3, url.lastIndexOf("&")));
                    int simpleParam = Integer.parseInt(url.substring(url.length() - 1));
                    String[] result = ft.parseItunes(jsonObject, id);

                    if (result != null) {
                        try {
                            mData.get(id).setItunes(result[0], result[1], result[2], result[3]);
                            baseAdapter.updateArrayList(mData);
                            baseAdapter.notifyDataSetChanged();
                        } catch (NullPointerException e) {
                            Log.d(tag, e.toString());
                        }
                    } else if (simpleParam == 1) {
                        Log.e(tag, "Result object is null with simple params. " + url);
                        callitunes(id, false);
                    } else {
                        callitunes(id, ItunesParamType.NORMAL);
                    } else if (method == 1) {
                        Crashlytics.log(Log.ERROR, tag, "Result object is null with normal params. " + url);
                        callitunes(id, ItunesParamType.ELIM);
                    } else {
                        Log.e(tag, "Result object is null with elim params. " + url);
                    }
                } catch (JSONException e) {
                    Log.d(tag, e + "");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
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
                BillyItem b = mData.get(i);
                b.setSimpleSong(ft.getSimpleString(billySong[i]));
                b.setSimpleArtist(ft.getSimpleString(billyArtist[i]));

                Intent myintent = new Intent(getActivity(), DetailView.class);
                if (b.getArtwork() != null) {
                    myintent.putExtra("item", b);
                    startActivity(myintent);
                } else {
                    callitunes(i, true);
                    baseAdapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(getActivity(), billyapp.getString(R.string.nointernet), Toast.LENGTH_LONG).show();
            }
        } catch (NullPointerException e) {
            Log.d(tag, e.toString());
            pulltorefresh = true;
            try {
                performRequests(false); // mData is null, so perform all requests again and construct it
            } catch (UnsupportedEncodingException e1) {
                Log.d(tag, e1.toString());
            }
        } catch (UnsupportedEncodingException e) {
            Log.d(tag, e.toString());
        }
    }

    /**
     * Pull to refresh, with animation. Using Google's SwipeRefreshLayout.
     * Invalidates cache
     */

    @Override
    public void onRefresh() {
        if (billyapp.isConnected()) {
            pulltorefresh = true;
            swingBottomInAdapter.reset();
            try {
                performRequests(true);
            } catch (UnsupportedEncodingException ignored) {
            }
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
     * Cancel all on-going requests if screen is rotated, or fragment gets destroyed
     * <p/>
     * Note that FragmentStatePagerAdapter kills every fragment once you leave it (unlike other PagerAdapters)
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        req.cancelAll(this);
    }

    /**
     * Check if an ArrayList has null values
     *
     * @return true, if it doesn't have null values
     */
    private boolean checkArrayList(ArrayList<BillyItem> mData) {
        int i = 0;
        try {
            for (BillyItem b : mData) {
                if (b == null) {
                    i++;
                } else if (b.getArtwork() == null) {
                    i++;
                }
            }
        } catch (NullPointerException e) {
            return false;
        }
        return i < 4;
    }

    /**
     * If compact cards preference is on, reload listview
     * If album art preference is changed, update it in Processing Task
     * spawn all iTunes requests again
     */

    SharedPreferences.OnSharedPreferenceChangeListener myPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("compactCards")) {
                Log.d(tag, "compact cards preference changed");
                ((ListView) v.findViewById(android.R.id.list)).setAdapter(baseAdapter);
            } else if (key.equals("albumArtQuality")) {
                if (ft.refreshArtworkUrlResolution()) {
                    Log.i(tag, "album art quality preference changed");
                    mIndex = 0;
                    while (mIndex < billyapp.getMinBillySize(billySize)) {
                        try {
                            callitunes(mIndex, true);
                        } catch (UnsupportedEncodingException e) {
                            Log.d(tag, e.toString());
                        }
                        mIndex++;
                    }
                }
            }
        }
    };
}