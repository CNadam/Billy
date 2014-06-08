package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;

import java.util.ArrayList;


public class Fragment1 extends Fragment {
    ArrayList<CustomFragmentAdapter.BillyData> data;
    String[] song;
    String[] album;
    String[] artist;
    int[] artwork;
    ListView lv;
    View v;
    ImageLoader imgload;
    RequestQueue req;

    private static final String TAG = "Fragment1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        data = getArguments().getParcelableArrayList("KEY_MDATA");
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

    class MyCustomAdapter extends BaseAdapter {
        Context c;
        MyCustomAdapter(Context c) {
            int i;
            this.c = c;

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
            return data.size();
        }

        @Override
        public Object getItem(int i) {
            return data.get(i);
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
            ImageView artwork = (ImageView) row.findViewById(R.id.artwork);

            //put an object item from ArrayList in BillyData object
            CustomFragmentAdapter.BillyData temp = data.get(i);

            //set everything
            album.setText(temp.album);
            artist.setText(temp.artist);
            song.setText(temp.song);

            artwork.setImageResource(R.drawable.ram);

            return row;
        }
    }
}