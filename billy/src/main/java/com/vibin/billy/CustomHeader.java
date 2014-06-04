package com.vibin.billy;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import it.gmariotti.cardslib.library.internal.CardHeader;

public class CustomHeader extends CardHeader {

    ArrayList<Fragment1.BillyData> data;
    int position;
    public CustomHeader(Context context, ArrayList<Fragment1.BillyData> data, int position) {
        super(context, R.layout.custom_header_layout);
        this.data = data;
        this.position = position;
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        super.setupInnerViewElements(parent, view);
        TextView t3 = (TextView) view.findViewById(R.id.song);
        t3.setText(data.get(position).song);
    }
}