package com.vibin.billy.swipeable;

import android.content.Context;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;


/**
 * @author Created by Saketme on 5/25/14.
 *         Contains base methods and fields for  and {@link ActivitySwipeDismissListener}
 */
public abstract class SwipeListener {

    // debug
    private String TAG = SwipeListener.class.getSimpleName();

    // Cached ViewConfiguration and system-wide constant values
    public final int SLOP;
    public final int MIN_FLING_VEL;
    public final int MAX_FLING_VEL;
    public final int FLING_DISTANCE;
    public final int DIRECTION_CHANGE_DISTANCE;

    // gesture
    private SwipeDirection mSwipeDirection = SwipeDirection.RIGHT;

    // constants and dimensions
    private static final int MIN_DISTANCE_FOR_FLING = 20; // dips
    private static final int MIN_DISTANCE_FOR_DIRECTION_CHANGE = 15; // dips

    // flags
    private boolean mScrollingCacheEnabled;
    private static final boolean USE_CACHE = false;
    //private boolean mSwipeDisabled = false;

    /**
     * Defines the direction in which the swipe to perform an action can be done.
     * Like, swipe to dismiss an activity OR swipe to reveal navigation drawer
     */
    public enum SwipeDirection {
        /**
         * NOT IN USE.
         * The user can swipe the activity / nav drawer into both directions (left and right)
         */
        BOTH,
        /**
         * The user can only swipe the activity / nav darawer to the left.
         */
        LEFT,
        /**
         * * The user can only swipe the activity / nav darawer  to the right to close it.
         */
        RIGHT
    }

    public SwipeListener(Context context, ViewConfiguration vc) {

        //Log.i(TAG, "SwipeListener()");

        SLOP = vc.getScaledTouchSlop();
        MIN_FLING_VEL = vc.getScaledMinimumFlingVelocity();
        MAX_FLING_VEL = vc.getScaledMaximumFlingVelocity();

        final float density = context.getResources().getDisplayMetrics().density;
        FLING_DISTANCE = (int) (MIN_DISTANCE_FOR_FLING * density);
        DIRECTION_CHANGE_DISTANCE = (int) (MIN_DISTANCE_FOR_DIRECTION_CHANGE * density);

    }

/* GETTERS & SETTERS */

    /**
     * Override this to specify the maximum animation duration.
     * Used for dynamically calculating duration of View animations depending upon the distance
     * that has to be traveled by the View.
     */
    abstract public long getMaximumAnimDuration();

    /**
     * Sets the directions in which the activity can be swiped to delete.
     * By default this is set to {@link SwipeDirection#RIGHT}.
     *
     * @param direction The direction to limit the swipe to.
     */
    public void setSwipeDirection(SwipeDirection direction) {
        //Log.i(TAG, "Setting direction to: " + direction);
        mSwipeDirection = direction;
    }

    /**
     * Enable/disable swipe.
     */
    /*public void setSwipeDisabled(boolean disabled) {
        this.mSwipeDisabled = disabled;
    }*/
    public SwipeDirection getSwipeDirection() {
        return mSwipeDirection;
    }

    public boolean isSwipeLeftDirection() {
        return mSwipeDirection == SwipeDirection.LEFT;
    }

    public boolean isSwipeRightDirection() {
        return mSwipeDirection == SwipeDirection.RIGHT;
    }

    /**
     * A hardware layer can be used to cache a complex view tree into a
     * texture and reduce the complexity of drawing operations. For instance,
     * when animating a complex view tree with a translation, a hardware layer can
     * be used to render the view tree only once.
     */
    public void manageLayers(final View view, boolean enableHardware) {
        //ViewUtils.manageLayers(view, enableHardware);
//        if(enableHardware) {
        //           view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //     }
    }

    /*
    * setScrollingCacheEnabled(true);
    * */
    private void setScrollingCacheEnabled(ViewGroup viewGroupiew, boolean enabled) {
        if (mScrollingCacheEnabled != enabled) {
            mScrollingCacheEnabled = enabled;
            if (USE_CACHE) {
                final int size = viewGroupiew.getChildCount();
                for (int i = 0; i < size; ++i) {
                    final View child = viewGroupiew.getChildAt(i);
                    if (child.getVisibility() != View.GONE) {
                        child.setDrawingCacheEnabled(enabled);
                    }
                }
            }
        }
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel.
    protected long getDynamicDurationTime(View view, float deltaX) {

        long fullDuration = getMaximumAnimDuration();
//          long fullDuration = 10000;
        float distanceRatio = Math.abs(deltaX) / view.getWidth();
        float dynDur;

        // bhaad me jaye dynamic stuff, I'm hardcoding it
        if (distanceRatio >= 0.9f)
            dynDur = fullDuration * 0.6f;
        else if (distanceRatio >= 0.7f)
            dynDur = fullDuration * 0.50f;
        else if (distanceRatio >= 0.4f)
            dynDur = fullDuration * 0.45f;
        else if (distanceRatio >= 0.2f)
            dynDur = fullDuration * 0.4f;
        else
            dynDur = fullDuration * 0.35f;

        //Log.i(TAG, "Dynamic duration using interpolator: " + dynDur + ", distanceRatio: " + distanceRatio);

        return (long) dynDur;

    }

    /**
     * Checks whether the delta of a swipe indicates, that the swipe is in the
     * correct direction, regarding the direction set via
     * {@link #setSwipeDirection(SwipeDirection)}
     *
     * @param deltaX The delta of x coordinate of the swipe.
     * @return Whether the delta of a swipe is in the right direction.
     */
    public boolean isDirectionValid(float deltaX) {

        switch (mSwipeDirection) {
            default:
            case BOTH:
                return true;
            case LEFT:
                return deltaX < 0;
            case RIGHT:
                return deltaX > 0;
        }

    }

}