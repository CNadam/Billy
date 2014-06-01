package com.vibin.billy;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import it.gmariotti.cardslib.library.internal.CardThumbnail;

public class CustomThumbCard extends CardThumbnail {

    private static final String TAG =  CustomThumbCard.class.getName();
    public CustomThumbCard(Context context) {
        super(context);
    }


    @Override
    public void setupInnerViewElements(ViewGroup parent, View viewImage) {
        if (viewImage != null) {

            if (parent!=null && parent.getResources()!=null){
                DisplayMetrics metrics=parent.getResources().getDisplayMetrics();

                int base = 143;

                if (metrics!=null){
                    viewImage.getLayoutParams().width = (int)(base*metrics.density);
                    viewImage.getLayoutParams().height = (int)(base*metrics.density);
                }else{
                    Log.i(TAG, "Layout params are indeed null");
                    viewImage.getLayoutParams().width = 250;
                    viewImage.getLayoutParams().height = 250;
                }
            }
        }
    }

}
