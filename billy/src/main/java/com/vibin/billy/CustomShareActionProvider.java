package com.vibin.billy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Method;

public class CustomShareActionProvider extends ShareActionProvider {
    Context c;

    public CustomShareActionProvider(Context context) {
        super(context);
        this.c = context;
    }

    @Override
    public View onCreateActionView() {
        View view = super.onCreateActionView();
        try {
            Drawable icon = c.getResources().getDrawable(R.drawable.share_holo_light);
            Method method = view.getClass().getMethod("setExpandActivityOverflowButtonDrawable", Drawable.class);
            method.invoke(view, icon);
        } catch (Exception e) {
            Log.e("MyShareActionProvider", "onCreateActionView", e);
        }

        return view;
    }
}
