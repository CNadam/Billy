package com.vibin.billy;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.CardHeader;

public class CustomHeader extends CardHeader {

    public CustomHeader(Context context) {

        super(context, R.layout.custom_header_layout);
    }

}