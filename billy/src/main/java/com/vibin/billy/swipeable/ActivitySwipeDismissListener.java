package com.vibin.billy.swipeable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;

/**
 * Created by Saketme on 2/13/14.
 * Gesturelistener for closing an activity by swiping it to the left or right.
 */
public final class ActivitySwipeDismissListener extends SwipeListener
        implements View.OnTouchListener {

    // debug
    private String TAG = ActivitySwipeDismissListener.class.getSimpleName();

    // Cached stuff and system-wide constant values
    private ViewGroup mRootView;
    private float mDismissSlopPercent;

    // Transient properties
    private float mDownX;
    private float mDownTranslationX;                    // translationX of mRootView when ACTION_DOWN is received
    private boolean mSwiping;
    private boolean mFirstAnimation;
    private boolean mSwipeDisabled;
    private VelocityTracker mVelocityTracker;
    private final ViewPropertyAnimator mActivityAnimation;
    private ObjectAnimator mDrawerAnimator;             // for animating the activity's closing and opening

    // for ensuring that the user did not swipe back upwards at the last moment
    private float mLastRawX = -1;
    private boolean mSwipingToRight = false;

    // activity states
    public static final int STATE_DISMISSED = 0;
    public static final int STATE_NORMAL = 1;
    public static final int STATE_DRAGGING = 2;
    private int mActivityState = STATE_NORMAL;

    // gesture
    //private SwipeDirection getSwipeDirection() = SwipeDirection.RIGHT;

    // flags
    //private boolean mOngoingAnimation = false;

    /**
     * The callback interface used by {@link ActivitySwipeDismissListener}
     * to inform its client about a successful dismissal of the activity
     */
    public interface SwipeListener {

        /**
         * Called when the user has dismissed the activity
         */
        void onDismiss();

        /**
         * Called when the user is swiping the activity
         */
        void onSlide(float slideOffset);

    }

    private SwipeListener mListener;

    /**
     * Constructs a new swipe-to-dismiss touch listener for the given activity
     *
     * @param rootView           Root view of the activity which should be dismissable.
     * @param dismissSlopPercent Percentage of width after which the activity can be dismissed.
     *                           For example, a 0.5f slop would allow the activity to dismissed
     */
    public ActivitySwipeDismissListener(Context context, ViewGroup rootView, ViewConfiguration vc,
                                        float dismissSlopPercent, SwipeListener dismissCallback) {
        super(context, vc);

        //Log.i(TAG, "ActivitySwipeListener()");

        mDismissSlopPercent = dismissSlopPercent;
        mListener = dismissCallback;

        mRootView = rootView;
        mActivityAnimation = rootView.animate();
        rootView.setOnTouchListener(this);

    }

/* GETTERS & SETTERS */

    @Override
    public long getMaximumAnimDuration() {
        return 1400;    // ms
    }

    /**
     * Sets the directions in which the activity can be swiped to delete.
     * By default this is set to {@link SwipeDirection#RIGHT}.
     *
     * @param direction The direction to limit the swipe to.
     */
    public void setSwipeDirection(SwipeDirection direction) {
        super.setSwipeDirection(direction);
        mSwipingToRight = getSwipeDirection() == SwipeDirection.RIGHT;
    }

    /**
     * Enable/disable swipe.
     */
    public void setSwipeDisabled(boolean disabled) {
        this.mSwipeDisabled = disabled;
    }

    public boolean isActivityDismissed() {
        return mActivityState == STATE_DISMISSED;
    }

    public boolean isActivityBeingDragged() {
        return mActivityState == STATE_DRAGGING;
    }

    public boolean isActivityInNormalState() {
        return mActivityState == STATE_NORMAL;
    }

    public int getActivityState() {
        return mActivityState;
    }

/* TOUCH */

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if (mSwipeDisabled) return false;

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {

                mDownX = motionEvent.getRawX();
                mDownTranslationX = mRootView.getTranslationX();

                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(motionEvent);

                // stop any ongoing animation as soon as the user places his finger
                stopOngoingAnimation();

                return true;
            }

            case MotionEvent.ACTION_UP:

                if (mVelocityTracker == null) {
                    break;
                }

                // distance moved
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaXAbs = Math.abs(deltaX);
                boolean openDrawerRight = deltaX > 0;

                // velocity of movement
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityXAbs = Math.abs(mVelocityTracker.getXVelocity());

                // in case the user, at the last moment, dropped his decision of
                // dismissing this activity
                boolean dismiss = mSwipingToRight == openDrawerRight
                        && (deltaXAbs > mRootView.getWidth() * mDismissSlopPercent
                        && mSwiping || MIN_FLING_VEL * 2 <= velocityXAbs
                        && velocityXAbs <= MAX_FLING_VEL && mSwiping
                        && deltaXAbs > FLING_DISTANCE);

                /*
                LONGER VERSION:

                if(mSwipingToRight != openDrawerRight){
                    dismiss = false;
                    //Log.w(TAG, "STOPP!");

                }else{

                    // dismiss it only if the view was being swiped
                    if (deltaXAbs > mRootView.getWidth() * mDismissSlopPercent && mSwiping) {

                        dismiss = true;

                        //Log.i(TAG, "Should be dismissed. dismissRight: " + dismissRight);

                    }

                    // or, flinged
                    else if (MIN_FLING_VEL *2 <= velocityXAbs && velocityXAbs <= MAX_FLING_VEL
                            && mSwiping && deltaXAbs > FLING_DISTANCE) {


                        // velocityYAbs*2 < velocityXAbs will be true because we've already
                        // verifying it in SwipeDismissViewGroup's onInterceptTouchEvent()

                    dismiss = true;

                    //Log.i(TAG, "Flinged. Right: " + dismissRight);

                    }

                }

                */

                // animation time
                if (dismiss)    // animate dismissal
                    animateActivityDismissal(deltaX);

                else            // reset positions
                    animateResettingOfActivity();

            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker = null;
                mDownX = 0;
                mSwiping = false;
                mDownTranslationX = 0;
                manageLayers(mRootView, false);
                break;

            case MotionEvent.ACTION_MOVE: {

                if (mVelocityTracker == null) break;

                mVelocityTracker.addMovement(motionEvent);
                float x = motionEvent.getRawX();
                float deltaMoveX = x - mDownX;

                // no need to check against mSlop or diagonal movements. We're already doing that
                // while intercepting touch
                mSwiping = true;

                // for ensuring that the user was indeed swiping down before dismissing
                mSwipingToRight = !(motionEvent.getRawX() < mLastRawX && mLastRawX != -1);

                /*
                * LONGER VERSION:

                if (motionEvent.getRawX() < mLastRawX && mLastRawX != -1)
                    mSwipingToRight = false;
                else
                    mSwipingToRight = true;

                */

                // update after every few pixels movement
                if (Math.abs(motionEvent.getRawX() - mLastRawX) > DIRECTION_CHANGE_DISTANCE) {
                    mLastRawX = motionEvent.getRawX();
                }

                if (mSwiping) {

                    // set state
                    mActivityState = STATE_DRAGGING;

                    manageLayers(mRootView, true);
                    view.setTranslationX(translateWithinBounds(deltaMoveX));
                    manageLayers(mRootView, false);

                    // and callback
                    sendSlidingCallback();

                    return true;
                }

                break;
            }
        }
        return false;
    }

/* ANIMATION */

    private void animateActivityDismissal(float deltaX) {

        // this flag prevents the onAnimationEnd method in our listener to
        // be called twice. It usually happens whena a row is cleared
        // in the GridView
        mFirstAnimation = true;

        float distance = mRootView.getWidth() - mRootView.getTranslationX();
        long dynamicDuration = getDynamicDurationTime(mRootView, distance);
        boolean dismissRight = getSwipeDirection() == SwipeDirection.RIGHT;

        // for dismissing a view, the direction of fling/swipe should match the set
        // swipe direction
        if (deltaX > 0 != dismissRight) {
            return;
        }

        mDrawerAnimator = ObjectAnimator.ofFloat(
                mRootView,
                "translationX",
                mRootView.getTranslationX(),
                dismissRight ? mRootView.getWidth() : -mRootView.getWidth()
        );
        mDrawerAnimator.setStartDelay(0);
        mDrawerAnimator.setInterpolator(AnimationUtils.EASE_ACCELERATE_DEACELERATE_INTERPOLATOR);
//        mDrawerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mDrawerAnimator.setDuration(dynamicDuration);
        mDrawerAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                /**
                 * Changing layer type to hardware for better animation.
                 * See doc of {@link SwipeListener#manageLayers(android.view.View, boolean)}
                 * for more info
                 * */
                manageLayers(mRootView, true);

                // set state
                mActivityState = STATE_DRAGGING;

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                // change layer type back to NONE when this animation is complete
                manageLayers(mRootView, false);

                // set state
                mActivityState = STATE_DISMISSED;

            }

        });
        mDrawerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sendSlidingCallback();
            }
        });
        mDrawerAnimator.start();

        // Android takes some time to close an Activity after finish() has been called.
        // As a workaround, we're calling finish beforehand so that this animation
        // and finish() happen around the same time.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Log.i(TAG, "ACTION_UP -> onAnimationEnd");
                sendDismissCallback();
            }
        }, (long) (dynamicDuration * 0.6));

    }

    private void animateResettingOfActivity() {

        long dynamicDuration = getDynamicDurationTime(mRootView, mRootView.getTranslationX());

        mDrawerAnimator = ObjectAnimator.ofFloat(
                mRootView,
                "translationX",
                mRootView.getTranslationX(),
                0f
        );
        mDrawerAnimator.setStartDelay(0);
        mDrawerAnimator.setInterpolator(AnimationUtils.EASE_ACCELERATE_DEACELERATE_INTERPOLATOR);
//        mDrawerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mDrawerAnimator.setDuration(dynamicDuration);
        mDrawerAnimator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);

                /**
                 * Changing layer type to hardware for better animation. See doc of
                 * {@link ActivitySwipeDismissListener#manageLayers(mRootView, boolean)}
                 * for more info.
                 * */
                manageLayers(mRootView, true);

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                // change layer type back to NONE when this animation is complete
                manageLayers(mRootView, false);

                // set state
                mActivityState = STATE_NORMAL;

            }

        });
        mDrawerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sendSlidingCallback();
            }
        });
        mDrawerAnimator.start();

    }

    // immediately stops any on-going animation
    private void stopOngoingAnimation() {
        if (mDrawerAnimator != null) {
            mDrawerAnimator.removeAllUpdateListeners();
            mDrawerAnimator.cancel();
        }
    }

    public float translateWithinBounds(float deltaX) {

        float targetX = mDownTranslationX + deltaX;

        //Log.i(TAG, "Activity deltaX: " + deltaX + ", targetX: " + targetX);

        if (getSwipeDirection() == SwipeDirection.RIGHT) {
            if (targetX < 0)
                targetX = 0;
        } else if (getSwipeDirection() == SwipeDirection.LEFT) {
            if (targetX > 0)
                targetX = 0;
        }

        return targetX;
    }

/* CALLBACKS */

    private void sendDismissCallback() {
        // Fire the dismiss callback when the activity has been successfully swiped

        // dismiss this item
        mListener.onDismiss();
    }

    private void sendSlidingCallback() {

        int width = mRootView.getWidth();

        if (mListener == null || width == 0) return;

        float translationXAbs = Math.abs(mRootView.getTranslationX());
        float slideOffset = (translationXAbs / width);

        //Log.i(TAG, "slideOffset: " + slideOffset);

        //slideOffset = Math.min(1f, Math.max(0f, slideOffset));

        //Log.i(TAG, "Slide offset: " + slideOffset);
        mListener.onSlide(slideOffset);

    }

}