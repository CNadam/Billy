package com.vibin.billy.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.vibin.billy.BillyApplication;
import com.vibin.billy.BillyItem;
import com.vibin.billy.R;

import java.util.ArrayList;

public class BaseAdapter extends android.widget.BaseAdapter {

    private static final String TAG = BaseAdapter.class.getSimpleName();
    Context c;
    ArrayList<BillyItem> mData;
    ImageLoader imgload;
    SharedPreferences sharedPref;
    BillyApplication billyapp;
    @LayoutRes
    int resource;

    public BaseAdapter(Context c, ArrayList<BillyItem> arrayList, ImageLoader imgload) {
        this.c = c;
        mData = arrayList;
        this.imgload = imgload;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        billyapp = BillyApplication.getInstance();
        checkCompactCards();
        PreferenceManager.getDefaultSharedPreferences(c).registerOnSharedPreferenceChangeListener(myPrefListner);
    }

    SharedPreferences.OnSharedPreferenceChangeListener myPrefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            checkCompactCards();
        }
    };

    /**
     * Switch to smaller layout for cards in the Listview
     */

    private void checkCompactCards() {
        if (sharedPref.getBoolean("compactCards", true)) {
            resource = c.getResources().getIdentifier("single_row_compact", "layout", c.getPackageName());
        } else {
            resource = c.getResources().getIdentifier("single_row", "layout", c.getPackageName());
        }
    }

    public void updateArrayList(ArrayList<BillyItem> arraylist) {
        mData = arraylist;
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

    static class MyViewHolder {
        TextView album, artist, song, rank;
        NetworkImageView artwork;

        MyViewHolder(View row) {
            album = (TextView) row.findViewById(R.id.album);
            artist = (TextView) row.findViewById(R.id.artist);
            song = (TextView) row.findViewById(R.id.song);
            rank = (TextView) row.findViewById(R.id.rank);
            song.setMaxLines(2);
            album.setMaxLines(1);
            song.setEllipsize(TextUtils.TruncateAt.END);
            album.setEllipsize(TextUtils.TruncateAt.END);
            artwork = (NetworkImageView) row.findViewById(R.id.artwork);
        }
    }


    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View row = convertView;
        MyViewHolder holder;
        if (row == null) {
            LayoutInflater lif = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = lif.inflate(resource, viewGroup, false);
            holder = new MyViewHolder(row);

            row.setTag(holder);
        } else {
            holder = (MyViewHolder) row.getTag();
        }

        BillyItem b = mData.get(i);

        //Log.d(TAG, i + " " + temp.album + " " + temp.artist + " " + temp.song + " " + temp.artwork);

        if (b.getRank() != 0) {
            holder.rank.setText(b.getRank() + "");
            holder.rank.setVisibility(View.VISIBLE);
        }
        holder.album.setText(b.getAlbum());
        holder.artist.setText(b.getArtist());
        holder.song.setText(b.getSong());
        holder.artwork.setImageUrl(b.getArtwork(), imgload);

        return row;
    }
}
