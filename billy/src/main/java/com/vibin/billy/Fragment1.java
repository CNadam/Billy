package com.vibin.billy;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;
import it.gmariotti.cardslib.library.view.CardView;


public class Fragment1 extends Fragment {
    ArrayList<BillyData> data;
    String[] song;
    String[] album;
    String[] artist;
    int[] artwork;
    CardView cv;
    View v;
    ArrayList<Card> cards = new ArrayList<Card>();
    private static final String TAG = "Fragment1";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         v =  inflater.inflate(R.layout.fragment_1, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        populateData();
        setAdapter();
    }

    private void setAdapter() {
        CardArrayAdapter mCardArrayAdapter = new CardArrayAdapter(getActivity(),cards); //adapter for the ArrayList

        CardListView listView = (CardListView) getActivity().findViewById(R.id.list_cards); //attach the listview to the layout
        if (listView!=null){
            listView.setAdapter(mCardArrayAdapter);
        }
    }

    class BillyData {
        String song, album, artist;
        int artwork;
        BillyData(String song, String album, String artist, int artwork) {
            this.song = song;
            this.album = album;
            this.artist = artist;
            this.artwork = artwork;
        }
    }

    private void populateData() {

        TextView tv = new TextView(getActivity());
        tv.setText("Some random text");
        LinearLayout ll = (LinearLayout) v.findViewById(R.id.what);
        ll.addView(tv);

        int i;
        song = new String[] {"Get Lucky","All Of Me","Mirrors"};
        album = new String[] {"Random Access Memories","Love In The Future","The 20/20 Experience"};
        artist = new String[] {"Daft Punk","John Legend","Justin Timberlake"};
        artwork = new int[] {R.drawable.ram,R.drawable.allofme,R.drawable.mirrors};
        data = new ArrayList<BillyData>();

        for(i=0;i<song.length;i++)
        {
            BillyData obj = new BillyData(song[i],album[i],artist[i],artwork[i]);
            data.add(obj);
            BillyCard card = new BillyCard(getActivity(),data,i);

            //create a CardHeader
            CustomHeader header = new CustomHeader(getActivity(), data, i);
            card.addCardHeader(header);

            //creates a thumbnail image
            CustomThumbCard thumb = new CustomThumbCard(getActivity());
            thumb.setDrawableResource(artwork[i]);
            card.addCardThumbnail(thumb);
            cards.add(card);
        }
    }
}