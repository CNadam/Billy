package com.vibin.billy;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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


public class Fragment1 extends Fragment {
    ArrayList<BillyData> mData;
    String[] song;
    String[] album;
    String[] artist;
    int[] artwork;
    ListView lv;
    View v;
    ImageLoader imgload;
    RequestQueue req;
    String searchparam = "dark+horse";
    String uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
    String rssurl ="http://www1.billboard.com/rss/charts/hot-100";
    int mIndex = 0;
    int nIndex = 0;

    private static final String TAG = "Fragment1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mData = new ArrayList<BillyData>(25);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_1, container, false);
        lv = (ListView) v.findViewById(R.id.listView);
        lv.setAdapter(new MyCustomAdapter(getActivity()));
        Log.d(getClass().getName(), "On create view yaha hai bc");
        return v;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    //contains data to be put into ArrayList
 /*   class BillyData {
        String song, album, artist;
        int artwork;
        BillyData(String song, String album, String artist, int artwork) {
            this.song = song;
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
        }
    }*/

    public class BillyData {
        String song, album, artist, artwork;
        BillyData(String song) {
            this.song = song;
        }

        public void setItunes(String album, String artist, String artwork, String song){
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
            this.song = song;
        }
    }

    class MyCustomAdapter extends BaseAdapter {
        Context c;
        MyCustomAdapter(Context c) {
            int i;
            this.c = c;

            req = Volley.newRequestQueue(c);

            // Get Billboard XML
            StringRequest stringreq = new StringRequest(Request.Method.GET,rssurl,billyComplete(),billyError());

            imgload = new ImageLoader(req, new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);
                public void putBitmap(String url, Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }
                public Bitmap getBitmap(String url) {
                    return mCache.get(url);
                }
            });

            req.add(stringreq);
/*
            //create data and put inside ArrayList
            song = new String[]{"Get Lucky", "All Of Me", "Mirrors", "Fourth song", "Five song"};
            album = new String[]{"Random Access Memories", "Love In The Future", "The 20/20 Experience", "Fourth album", "Fifth album"};
            artist = new String[]{"Daft Punk", "John Legend", "Justin Timberlake", "New artist", "Iggy"};
            artwork = new int[]{R.drawable.ram, R.drawable.allofme, R.drawable.mirrors, R.drawable.allofme, R.drawable.allofme};
*/
/*
            data = new ArrayList<CustomFragmentAdapter.BillyData>();
            for (i = 0; i < song.length; i++) {
                BillyData obj = new BillyData(song[i], album[i], artist[i], artwork[i]);
                data.add(obj);
            }
*/
        }

        private Response.Listener<String> billyComplete() {
            return new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        handleXML(response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
        }


        private Response.ErrorListener billyError() {
            return new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Toast.makeText(c, volleyError.toString(), Toast.LENGTH_LONG).show();
                    Log.d(getClass().getName(), volleyError.toString());
                }
            };
        }


        // Parse the XML as InputStream
        private void handleXML(String response) throws IOException {
            mData = new ArrayList<BillyData>(100);
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

                    if (parser.getLineNumber() < 200 && event == XmlPullParser.START_TAG) {
                        if(name.equals("title")){

                            if(parser.next() == XmlPullParser.TEXT) {
                                if (i != 0) {
                                    String title = parser.getText();

                                    BillyData obj = new BillyData(cleanSongTitle(parser.getText()));
                                    mData.add(i-1, obj);
                                }
                                i++;
                            }
                        }

                    }
                    else if(parser.getLineNumber() >= 200) {
                        Log.d(getClass().getName(), "Parsed 200 lines!");
                        break;
                    }
                    try {
                        event = parser.next();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                //Query iTunes for each Billboard song
                Log.d(getClass().getName(), "Parsing is done and size of arraylist is "+mData.size());
                while(mIndex < mData.size())
                {

                    Log.d(getClass().getName(), mData.get(mIndex).song);
                    searchparam = mData.get(mIndex).song.replaceAll(" ","+"); // Put Billboard song as iTunes search parameter
                    uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
                    JsonObjectRequest jsonreq =  new JsonObjectRequest(Request.Method.GET,uri,null, itunesComplete(), itunesError());
                    req.add(jsonreq);
                    Log.d(getClass().getName(), "number is "+req.getSequenceNumber());
                    mIndex++;
                }
                Log.d(getClass().getName(), "itunes requests complete!");

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }

        // Manipulating the Billboard Song string
        private String cleanSongTitle(String text) {
            return  text.substring(text.indexOf(":") + 2, text.indexOf(","));
        }


        private Response.Listener<JSONObject> itunesComplete() {
            return new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    try {
                        String trackName,artistName,collectionName,artworkUrl;
                        JSONArray mJsonArray = jsonObject.getJSONArray("results");
                        artistName = mJsonArray.getJSONObject(0).getString("artistName");
                        collectionName = mJsonArray.getJSONObject(0).getString("collectionName");
                        artworkUrl = mJsonArray.getJSONObject(0).getString("artworkUrl100");
                        trackName = mJsonArray.getJSONObject(0).getString("trackName");
                       // Log.d(getClass().getName(), artistName+ " "+ collectionName +" "+ artworkUrl);

                        String newUrl = artworkUrl.replaceAll("100x100","600x600");
//                        Log.d(getClass().getName(), ""+artworkUrl.lastIndexOf("100x100"));
                        mData.get(nIndex).setItunes(collectionName, artistName, newUrl, trackName);
                        Log.d(getClass().getName(), nIndex + " " + mData.get(nIndex).song + mData.get(nIndex).artist + mData.get(nIndex).album + mData.get(nIndex).song);
                        nIndex++;
                        notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.d(getClass().getName(), e+ "mIndex is" + mIndex);
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


        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int i) {
            return mData.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater lif = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = lif.inflate(R.layout.single_row, viewGroup, false);

            //find everything
            TextView album = (TextView) row.findViewById(R.id.album);
            TextView artist = (TextView) row.findViewById(R.id.artist);
            TextView song = (TextView) row.findViewById(R.id.song);
            NetworkImageView artwork = (NetworkImageView) row.findViewById(R.id.artwork);

            //put an object item from ArrayList in BillyData object
            BillyData temp = mData.get(i);

            //set everything
            album.setText(temp.album);
            artist.setText(temp.artist);
            song.setText(temp.song);
            artwork.setImageUrl(temp.artwork,imgload);

         //   Log.d(getClass().getName(), temp.album + temp.artist + temp.song + temp.artwork);
            return row;
        }
    }
}