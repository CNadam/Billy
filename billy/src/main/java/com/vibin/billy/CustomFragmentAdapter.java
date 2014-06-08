package com.vibin.billy;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.viewpagerindicator.IconPagerAdapter;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class CustomFragmentAdapter extends FragmentPagerAdapter implements IconPagerAdapter {

    private static final String TAG = "CustomFragmentAdapter";
    protected static final String[] CONTENT = new String[]{
            "Most Popular", "Pop", "Rock", "Dance"
    };
    private int mCount = CONTENT.length;
    String searchparam = "dark+horse";
    Context c;
    Cache cache;
    RequestQueue req;
    ArrayList<BillyData> mData;
    ImageLoader.ImageCache imgcache;
    Bundle b1;
    String KEY_MDATA ="KEY_MDATA";
    String uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
    String rssurl ="http://www1.billboard.com/rss/charts/hot-100";
    int mIndex;

    public CustomFragmentAdapter(FragmentManager fm, Context c) throws JSONException {
        super(fm);
        this.c = c;

        req = Volley.newRequestQueue(c);

        int memClass = ((ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
        int cacheSize = 1024*1024*memClass/8;

        // Get Billboard XML
        StringRequest stringreq = new StringRequest(Request.Method.GET,rssurl,billyComplete(),billyError());


        //SimpleXmlRequest<Note> simplereq = new SimpleXmlRequest<Note>(Request.Method.GET,rssurl,Note.class,simpleComplete(),simpleError());


        req.add(stringreq);
        //req.add(simplereq);
    }


    private Response.Listener<String> billyComplete() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
              Log.d(getClass().getName(), response.substring(0,50));
              handleXML(response);
            }
        };
    }

    private Response.ErrorListener billyError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(c,volleyError.toString(),Toast.LENGTH_LONG).show();
                Log.d(getClass().getName(), volleyError.toString());
            }
        };
    }


    public class BillyData implements Parcelable{
        String song, album, artist, artwork;
        BillyData(String song) {
            this.song = song;
        }

        public BillyData(Parcel in) {
            readFromParcel(in);
        }

        private void readFromParcel(Parcel in) {
            this.song = in.readString();
            this.album = in.readString();
            this.artist = in.readString();
            this.artwork = in.readString();

        }

        public void setItunes(String album, String artist, String artwork){
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
        }

        public final Parcelable.Creator<BillyData> CREATOR = new Parcelable.Creator<BillyData>() {
            public BillyData createFromParcel(Parcel in) {
                return new BillyData(in);
            }

            public BillyData[] newArray(int size) {
                return new BillyData[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(song);
            parcel.writeString(album);
            parcel.writeString(artist);
            parcel.writeString(artwork);
        }
    }

    // Parse the XML as InputStream
    private void handleXML(String response) {
        mData = new ArrayList<BillyData>();
        try {
            InputStream in = null;
            try {
                in = IOUtils.toInputStream(response, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            XmlPullParserFactory xmlppf = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlppf.newPullParser();
            parser.setInput(in,null);

            int event = parser.getEventType();
            int i = 0;
            while(event != XmlPullParser.END_DOCUMENT)
            {
                String name = parser.getName();

                if (parser.getLineNumber() <= 300) {
                    switch(event)
                    {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.END_TAG:
                        if(name.equals("title")){
                            Log.d(getClass().getName(), "Song name is "+parser.getText());

                            BillyData obj = new BillyData(cleanSongTitle(parser.getText()));
                            mData.add(i,obj);
                            i++;
                        }
                        break;
                    }

                    try {
                        event = parser.next();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            Log.d(getClass().getName(), "Parsing is done!");
            mData.remove(0); // Remove first object as it doesn't contain song

            //Query iTunes for each Billboard song
            Log.d(getClass().getName(), "Size of arraylist is "+mData.size());
            while(mIndex <= mData.size())
            {
                searchparam = cleanSongTitle(mData.get(mIndex).song); // Put Billboard song as iTunes search parameter
                JsonObjectRequest jsonreq =  new JsonObjectRequest(Request.Method.GET,uri,null, itunesComplete(), itunesError());
                req.add(jsonreq);
            }
            Log.d(getClass().getName(), "itunes requests complete!");

            b1.putParcelableArrayList(KEY_MDATA, mData);

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    // Manipulating the Billboard Song string
    private String cleanSongTitle(String text) {
         return  text.substring(text.indexOf(":")+2, text.indexOf(",")-1).replaceAll(" ","+");
    }


    private Response.Listener<JSONObject> itunesComplete() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    String artistName,collectionName,artworkUrl;
                    JSONArray mJsonArray = jsonObject.getJSONArray("results");
                    artistName = mJsonArray.getJSONObject(0).getString("artistName");
                    collectionName = mJsonArray.getJSONObject(0).getString("collectionName");
                    artworkUrl = mJsonArray.getJSONObject(0).getString("artworkUrl100");
                    Log.d(getClass().getName(), artistName+ " "+ collectionName +" "+ artworkUrl);

                    mData.get(mIndex).setItunes(collectionName, artistName, artworkUrl);
                    mIndex++;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private Response.ErrorListener itunesError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(c,volleyError.toString(),Toast.LENGTH_LONG).show();
                Log.d(getClass().getName(), volleyError.toString());
            }
        };
    }


     public CustomFragmentAdapter(android.support.v4.app.Fragment fragment) {
        super(fragment.getChildFragmentManager());

    }


    @Override
    public int getIconResId(int index) {
        return 0;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment1();
        switch (position) {
            case 0:
                fragment.setArguments(b1);
                fragment = new Fragment1();
                Log.d(TAG, "fragment 1 is called");
                break;
            case 1:
                fragment = new Fragment2();
                Log.d(TAG, "fragment 2 is called");
                break;
            case 2:
                fragment = new Fragment3();
                Log.d(TAG, "fragment 3 is called");
                break;
            case 3:
                fragment = new Fragment4();
                Log.d(TAG, "fragment 4 is called");
                break;
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        switch (position) {
            case 0:
                title = "Most Popular";
                break;
            case 1:
                title = "Pop";
                break;
            case 2:
                title = "Rock";
                break;
            case 3:
                title = "Dance";
                break;
        }

        return title;
    }

    public void setCount(int count) {
        if (count > 0 && count < 10) {
            mCount = count;
            notifyDataSetChanged();
        }
    }
}
