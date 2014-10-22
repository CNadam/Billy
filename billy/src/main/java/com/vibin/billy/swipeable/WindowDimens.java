package com.vibin.billy.swipeable;

/**
 * Created by Saketme on 5/25/2014.
 * POJO for holding dimensions of either the available window or full display.
 * <p/>
 * USED BY
 * ---------
 * <p/>
 * 1. WindowUtils
 */
public class WindowDimens {

    public int width;
    public int height;
    public int statusBarHeight;

    //NOTE When in landscape mode, ignore Nav bar height
    public int navBarHeight;

    public WindowDimens(int width, int height, int statusBarHeight, int navBarHeight) {
        this.width = width;
        this.height = height;
        this.statusBarHeight = statusBarHeight;
        this.navBarHeight = navBarHeight;
    }

}
