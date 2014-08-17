package com.vibin.billy;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class NotifyingImageButton extends ImageButton {
    private OnLayoutChangedListener layoutChangedListener;

    public NotifyingImageButton(Context context) {
        super(context);
    }

    public NotifyingImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingImageButton(Context context, AttributeSet attrs, int defStyle, OnLayoutChangedListener layoutChangedListener) {
        super(context, attrs, defStyle);
        this.layoutChangedListener = layoutChangedListener;
    }

    public void setLayoutChangedListener(OnLayoutChangedListener layoutChangedListener)
    {
        this.layoutChangedListener = layoutChangedListener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(layoutChangedListener != null)
        {
            layoutChangedListener.onLayout(changed, left, top, right, bottom);
        }
    }

    public interface OnLayoutChangedListener
    {
        void onLayout(boolean changed, int l, int t, int r, int b);
    }
}
