package com.vibin.billy.swipeable;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ViewGroup;

/**
 * Created by Saketme on 11/7/13. PRAISE LORD DUARTE!
 * <p/>
 * The sueprhero version of an Activity. It can be closed by performing a swipe gesture (customizable to LEFT or RIGHT)
 * {@link #enableSwipeToDismiss()}
 */
public class SwipeableActivity extends FragmentActivity { // Extends Activity by default

    // debug
    private static final String TAG = SwipeableActivity.class.getSimpleName();

    // top most-level swipeable ViewGroup
    private SwipeDismissViewGroup mSwipeDismissViewGroup;

    /**
     * Enables Swipe to Dismiss on this Activity.
     * <p/>
     * For now, this is a permanent action and cannot be disabled later. We can make toggable
     * if required by calling {@link SwipeDismissViewGroup#setSwipeDisabled(boolean)}
     * <p/>
     * Don't forget to make apply this theme on your Activity in Manifest.XML:
     * <p/>
     * android:theme="@style/AppTheme.SwipeableActivity.Light"
     * (or .Dark)
     */
    public void enableSwipeToDismiss() {
        Log.d(TAG, "Swipe to dismiss enabled. ");

        /**
         * ----------
         * WORKING
         * ----------
         *
         * STEP 1.
         * We first need to ensure that the swipe gesture is accessible from anywhere,
         * including the action bar.
         *
         * For achieving this, we find the root View inside this activity's window
         * i.e., the Decor View. The DecorView is the view that actually holds the window's
         * background drawable. More info here:
         * http://android-developers.blogspot.in/2009/03/window-backgrounds-ui-speed.html
         *
         * The DecorView has one child ViewGroup that holds the ActionBar and a "content"
         * ViewGroup where all the Activity layout is present. (android.R.id.content)
         *
         * Next, the decor view's top-level ViewGroup is removed and added to our
         * {@link com.vibin.billy.SwipeDismissViewGroup}
         *
         * STEP 2.
         * The second step is related to how Android handles touch events. To put in
         * simple words, the touch events start at the Activity at dispatchTouchEven()
         * and flows down through views in its layout until consumed by a View.
         *
         * In our case, we don't want the Views inside SwipeDismissViewGroup to receive any touch
         * events as soon as the user starts swiping horizontally.
         *
         * To achieve this, we override SwipeDismissViewGroup's onInterceptTouchEvent() method
         * and detect horizontal swipes. When a match is found, the flow of touch events to
         * its child Views are blocked until the user lifts his finger.
         *
         * Take a look yourself:
         * {@link com.vibin.billy.SwipeDismissViewGroup#onInterceptTouchEvent(android.view.MotionEvent)}
         *
         * */
        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
        if (decorChild == null) return;  // this should never be true. just a pre-cautionary step.

        // save ActionBar themes that have transparent assets
        //decorChild.setBackgroundResource(background);

        // detach decorChild
        decor.removeView(decorChild);

        // create an overlay ImageView to be used for showing background dim
        //final OverlayImageViewCreator creator = OverlayImageViewCreator.createDimmed(this);
        //decor.addView(creator.overlayImageView, 0, creator.layoutParams);

        // add it to swipeable viewgroup
        mSwipeDismissViewGroup = new SwipeDismissViewGroup(this);
        mSwipeDismissViewGroup.setContent(decorChild);
        mSwipeDismissViewGroup.setSwipeListener(new SwipeDismissViewGroup.SwipeListener() {
            @Override
            public void onDismissed() {

                if (!isFinishing())
                    onActivitySwipeDismissed();
            }

            @Override
            public void onSlide(float slideOffset) {

                onActivitySlide(slideOffset);

                /*if(slideOffset == 0f)
                    creator.overlayImageView.setVisibility(View.INVISIBLE);
                else
                    creator.overlayImageView.setVisibility(View.VISIBLE);

                creator.overlayImageView.setAlpha(1 - slideOffset);*/

            }
        });

        //mSwipeDismissViewGroup.setBackgrounDimOverlay(creator.overlayImageView);

        // and add swipeable viewgroup to this window's decor
        decor.addView(mSwipeDismissViewGroup, 0);   // make position 1 if dim overlay is also present

    }

    /**
     * Override this if you'd like to be notified when the activity has been
     * swiped successfully and is about to be finished. Don't forget to call super on this.
     * <p/>
     * Of course you can also override onStop, onDestroy and other lifecycle methods.
     * Still, this method might come of use sometime.
     */
    protected void onActivitySwipeDismissed() {
        finish();
        overridePendingTransition(0, 0);
    }

    /**
     * Called when swipe-to-dismiss is enabled by calling enableSwipeToDismiss() and the user
     * is sliding the Activity.
     */
    public void onActivitySlide(float slideOffset) {
        // this space for rent
    }

}