package com.vibin.billy;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class NotifyingImageView extends ImageView {
    private OnLayoutChangedListener layoutChangedListener;

    public NotifyingImageView(Context context) {
        super(context);
    }

    public NotifyingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyingImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
