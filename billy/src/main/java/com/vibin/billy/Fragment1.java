package com.vibin.billy;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Arrays;


public class Fragment1 extends Fragment {
    ArrayList<BillyData> mData;
    String[] billySong;
    ListView lv;
    View v;
    ImageLoader imgload;
    MyCustomAdapter myCustomAdapter;
    RequestQueue req;
    String searchparam;
    JSONArray mJsonArray;
    String uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
    String rssurl ="http://www1.billboard.com/rss/charts/hot-100";
    String trackName,artistName,collectionName,artworkUrl;
    int mIndex, nIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mData = new ArrayList<BillyData>(24);
        while(mData.size() < 24) {
            mData.add(new BillyData());
        }

        // Get Billboard XML
        billySong = new String[24];
        req = Volley.newRequestQueue(getActivity());
        StringRequest stringreq = new StringRequest(Request.Method.GET,rssurl,billyComplete(),billyError());

        // Cache for images
        imgload = new ImageLoader(req, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(24);
            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }
            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }
        });

        req.add(stringreq);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_1, container, false);
        lv = (ListView) v.findViewById(R.id.listView);
        myCustomAdapter = new MyCustomAdapter(getActivity());
        lv.setAdapter(myCustomAdapter);
        mIndex = nIndex = 0;
        return v;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public class BillyData {
        String song, album, artist, artwork;
        BillyData() {}
        BillyData(String album, String artist, String artwork, String song) {
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
            this.song = song;
        }
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
                }
            }
        };
    }

    private Response.ErrorListener billyError() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getActivity(), volleyError.toString(), Toast.LENGTH_LONG).show();
                Log.d(getClass().getName(), volleyError.toString());
            }
        };
    }


    // Parse the XML as InputStream
    private void handleXML(String response) throws IOException {
        try {
            InputStream in = null;
            in = IOUtils.toInputStream(response, "UTF-8");

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
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
                                billySong[i-1] = cleanSongTitle(parser.getText());
                                Log.d(getClass().getName(), billySong[i-1]);
                            }
                            i++;
                        }
                    }

                }
                else if(parser.getLineNumber() >= 200) {
                    Log.d(getClass().getName(), "Parsed 200 lines!");
                    break;
                }
                event = parser.next();
            }

            while(mIndex < billySong.length-1)
            {
                searchparam = billySong[mIndex].replaceAll(" ","+"); // Put Billboard song as iTunes search parameter
                uri = "http://itunes.apple.com/search?term=" + searchparam + "&limit=1";
                JsonObjectRequest jsonreq =  new JsonObjectRequest(Request.Method.GET,uri,null, itunesComplete(), itunesError());
                req.add(jsonreq);
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
                    mJsonArray = jsonObject.getJSONArray("results");
                    artistName = mJsonArray.getJSONObject(0).getString("artistName");
                    collectionName = mJsonArray.getJSONObject(0).getString("collectionName");
                    artworkUrl = mJsonArray.getJSONObject(0).getString("artworkUrl100").replaceAll("100x100","400x400");
                    trackName = mJsonArray.getJSONObject(0).getString("trackName");

                    String newTrack;
                    if(trackName.contains("("))
                    {
                        newTrack = capitalize(trackName.substring(0, trackName.indexOf("("))).trim();
                    }
                    else
                    {
                        newTrack = capitalize(trackName).trim();
                    }

                    int match = Arrays.asList(billySong).indexOf(newTrack);
                    Log.d(getClass().getName(), newTrack+":  match is "+match);
                    mData.add(match, new BillyData(collectionName, artistName, artworkUrl, newTrack));
                    //mData.add(match, obj);

                   // Log.d(getClass().getName(), nIndex + " " + mData.get(nIndex).song + mData.get(nIndex).artist + mData.get(nIndex).album + mData.get(nIndex).song);
                   // nIndex++;
                    myCustomAdapter.notifyDataSetChanged();
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
                Toast.makeText(getActivity(),volleyError.toString(),Toast.LENGTH_LONG).show();
                Log.d(getClass().getName(), volleyError.toString());
            }
        };
    }

    private String capitalize(String text) {
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        if (words[0].length() > 0) {
            sb.append(Character.toUpperCase(words[0].charAt(0)) + words[0].subSequence(1, words[0].length()).toString().toLowerCase());
            for (int i = 1; i < words.length; i++) {
                sb.append(" ");
                sb.append(Character.toUpperCase(words[i].charAt(0)) + words[i].subSequence(1, words[i].length()).toString().toLowerCase());
            }
        }
        //Log.d(getClass().getName(), "The capitalized string is"+sb.toString());
        return sb.toString();
    }


    //Adapter for the ListView
    class MyCustomAdapter extends BaseAdapter {
        Context c;

        MyCustomAdapter(Context c) {
            this.c = c;
            Log.d(getClass().getName(), "This is the adapter constructor!");

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

        class MyViewHolder{
            TextView album,artist,song;
            NetworkImageView artwork;
            MyViewHolder(View row){
                album = (TextView) row.findViewById(R.id.album);
                artist = (TextView) row.findViewById(R.id.artist);
                song = (TextView) row.findViewById(R.id.song);
                artwork = (NetworkImageView) row.findViewById(R.id.artwork);
            }
        }
        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View row =  convertView;
            MyViewHolder holder;
            if(row == null) {
                LayoutInflater lif = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = lif.inflate(R.layout.single_row, viewGroup, false);
                holder = new MyViewHolder(row);

                row.setTag(holder);
            }
            else{
                holder = (MyViewHolder) row.getTag();
            }

            //put an object item from ArrayList in BillyData object
            BillyData temp = mData.get(i);
            Log.d(getClass().getName(), i+ " "+temp.album + " "+temp.artist + " "+temp.song + " "+temp.artwork);

            //set everything
            holder.album.setText(temp.album);
            holder.artist.setText(temp.artist);
            holder.song.setText(temp.song);
            holder.artwork.setImageUrl(temp.artwork,imgload);


            return row;
        }
    }
}