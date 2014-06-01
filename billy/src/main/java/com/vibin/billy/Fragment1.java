package com.vibin.billy;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.zip.Inflater;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;


public class Fragment1 extends ListFragment {
    ViewGroup mContainer;
    ArrayList<Card> cards = new ArrayList<Card>();
    private static final String TAG = "Fragment1";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContainer = container;
        return inflater.inflate(R.layout.fragment_1, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Card card1 = new Card(this.getActivity()); //This is the first card
        Card card2 = new Card(this.getActivity()); //This is the second card
        initCards(card1);
        initCards(card2);

    }

    private void initCards(Card card) {
        card = new Card(this.getActivity());

        setStuff(card, R.drawable.ram, "Daft Punk", "RAM", "Get Lucky"); //add info to the card
        cards.add(card);

        CardArrayAdapter mCardArrayAdapter = new CardArrayAdapter(getActivity(),cards); //adapter for the ArrayList

        CardListView listView = (CardListView) getActivity().findViewById(R.id.list_cards); //attach the listview to the layout
        if (listView!=null){
            listView.setAdapter(mCardArrayAdapter);
        }
    }


    public void setStuff(Card card, int artwork, String artist, String album, String song) {

        LayoutInflater mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mInflater.inflate(R.layout.cardview_layout,mContainer,false);
        CardView cv = (CardView) view.findViewById(R.id.cardid);

        //create a CardHeader
        CustomHeader header = new CustomHeader(getActivity());
        card.addCardHeader(header);

        //creates a thumbnail image
        CustomThumbCard thumb = new CustomThumbCard(getActivity());
        thumb.setDrawableResource(artwork);
        card.addCardThumbnail(thumb);

        //modify textviews
        TextView t1 = (TextView) cv.findViewById(R.id.artist);
        TextView t2 = (TextView) cv.findViewById(R.id.album);
        View v = cv.findViewById(R.id.card_header_layout);
        TextView t3 = (TextView) v.findViewById(R.id.song);
        t1.setText(artist);
        t2.setText(album);
        t3.setText(song);

        cv.setCard(card); //set cardview to card

    }
}