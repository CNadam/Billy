package com.vibin.billy;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.view.CardView;

public class BillyCard extends Card {
    ArrayList<Fragment1.BillyData> data;
    int position;

    public BillyCard(Context context, ArrayList<Fragment1.BillyData> data, int position) {
        this(context, R.layout.main_content_inner_layout);
        this.data = data;
        this.position = position;
    }

    public BillyCard(Context context, int innerLayout) {
        super(context, innerLayout);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
       // CardView cv  = (CardView) view.findViewById(R.id.cardid);

        //modify textviews
        TextView t1 = (TextView) view.findViewById(R.id.artist);
        TextView t2 = (TextView) view.findViewById(R.id.album);
        //View v = cv.findViewById(R.id.card_header_layout);
        //TextView t3 = (TextView) v.findViewById(R.id.song);

        t1.setText(data.get(position).artist);
        t2.setText(data.get(position).album);
        //t3.setText(data.get(position).song);
    }


}
