package com.vibin.billy;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;

class CustomBaseAdapter extends BaseAdapter {
    Context c;
    ArrayList<FetchTask.BillyData> mData;
    ImageLoader imgload;

    CustomBaseAdapter(Context c, ArrayList<FetchTask.BillyData> arrayList, ImageLoader imgload) {
        this.c = c;
        Log.d(getClass().getName(), "This is the adapter constructor");
        mData = arrayList;
        this.imgload = imgload;
    }

    public void updateArrayList(ArrayList<FetchTask.BillyData> arraylist) {
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
        TextView album, artist, song;
        NetworkImageView artwork;

        MyViewHolder(View row) {
            album = (TextView) row.findViewById(R.id.album);
            artist = (TextView) row.findViewById(R.id.artist);
            song = (TextView) row.findViewById(R.id.song);
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
            row = lif.inflate(R.layout.single_row, viewGroup, false);
            holder = new MyViewHolder(row);

            row.setTag(holder);
        } else {
            holder = (MyViewHolder) row.getTag();
        }

        FetchTask.BillyData temp = mData.get(i);

        //Log.d(getClass().getName(), i + " " + temp.album + " " + temp.artist + " " + temp.song + " " + temp.artwork);

        // Set everything
        holder.album.setText(temp.album);
        holder.artist.setText(temp.artist);
        holder.song.setText(temp.song);
        holder.artwork.setImageUrl(temp.artwork, imgload);

        return row;
    }
}
