package com.vibin.billy.swipeable;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

public class SwipeDismissViewGroup extends ViewGroup {

    // debug
    private static final String TAG = SwipeDismissViewGroup.class.getSimpleName();

    // gestures
    private ActivitySwipeDismissListener mActivitySwipeDismissListener;
    private int mSlop;

    // cached stuff
    private View mContent;              // activity view
    private Rect mEdgeRect;
    private ImageView mBgDimOverlay;

    // transient properties
    private float mLastDownX, mLastDownY;
    private boolean mEdgeSwiping;
    private boolean mInterceptTouch;
    private boolean mSetupDone;

    /* Touch modes */
    // NOT IN USE

    // Allows the Swipeable Activity to be opened with a swipe gesture on the screen's margin
    public static final int TOUCHMODE_MARGIN = 0;

    // Allows the Swipeable Activity to be opened with a swipe gesture anywhere on the screen
    public static final int TOUCHMODE_FULLSCREEN = 1;

    protected int mTouchMode = TOUCHMODE_FULLSCREEN;

    // flags
    private boolean mSwipeDisabled;

    /**
     * Callbacks related to this swipeable ViewGroup
     */
    public interface SwipeListener {
        void onDismissed();

        void onSlide(float slideOffset);
    }

    private SwipeListener mListener;

    public SwipeDismissViewGroup(Context context) {
        this(context, null);
        init(context);
    }

    public SwipeDismissViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

        if (mSetupDone) return;
        mSetupDone = true;

        setWillNotDraw(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);

        setupTouchListener();

        /*
        * Swipe to dismiss direction should be set according to the user's way of holding
        * this phone. If he/she is left handed, then use LEFT to RIGHT for swiping,
        * else RIGHT to LEFT.
        * */
//        boolean userLeftHanded = USettings.getBool(context, USettings.IS_USER_LEFT_HANDED, true);
        boolean userLeftHanded = true;

        // for tracking swipes from the edge

        WindowDimens dimens = WindowUtils.getSimpleWindowDimens(getContext());
        //ViewGroup.LayoutParams dimens = getRootView().getLayoutParams();
        int edgeMargin = (int) (dimens.width * 0.1f);

        if (userLeftHanded) {
            setSwipeDirection(ActivitySwipeDismissListener.SwipeDirection.RIGHT);
            mEdgeRect = new Rect(0, 0, edgeMargin, dimens.height);
        } else {
            setSwipeDirection(ActivitySwipeDismissListener.SwipeDirection.LEFT);
            mEdgeRect = new Rect(dimens.width - edgeMargin, 0, dimens.width, dimens.height);
        }

        //Log.i(TAG, "Edge margin rect: " + mEdgeRect.flattenToString());

    }

    private void setupTouchListener() {

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mSlop = vc.getScaledTouchSlop();

        mActivitySwipeDismissListener = new ActivitySwipeDismissListener(
                getContext(),
                this,
                vc,
                0.3f,
                new ActivitySwipeDismissListener.SwipeListener() {
                    @Override
                    public void onDismiss() {
                        mListener.onDismissed();
                    }

                    @Override
                    public void onSlide(float slideOffset) {
                        mListener.onSlide(slideOffset);
                    }

                }
        );

    }

    public void setSwipeListener(SwipeListener l) {
        mListener = l;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = getDefaultSize(0, widthMeasureSpec);
        int height = getDefaultSize(0, heightMeasureSpec);
        setMeasuredDimension(width, height);

        final int contentWidth = getChildMeasureSpec(widthMeasureSpec, 0, width);
        final int contentHeight = getChildMeasureSpec(heightMeasureSpec, 0, height);
        mContent.measure(contentWidth, contentHeight);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Make sure scroll position is set correctly.
        /*if (w != oldw) {
            // [ChrisJ] - This fixes the onConfiguration change for orientation issue..
			// maybe worth having a look why the recomputeScroll pos is screwing
			// up?
			completeScroll();
			scrollTo(getDestScrollX(mCurItem), getScrollY());
		}*/
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        mContent.layout(0, 0, width, height);
    }

/* GETTERS & SETTERS */

    public void setContent(View v) {
        if (mContent != null)
            this.removeView(mContent);
        mContent = v;
        addView(mContent);
    }

    public View getContent() {
        return mContent;
    }

    public boolean isActivityInNormalState() {
        return mActivitySwipeDismissListener == null
                || mActivitySwipeDismissListener.isActivityInNormalState();
    }

    /**
     * Enables / disables swipe.
     */
    public void setSwipeDisabled(boolean disabled) {
        mSwipeDisabled = disabled;
    }

    /**
     * Customize swipe gesture direction. LEFT or RIGHT
     */
    public void setSwipeDirection(ActivitySwipeDismissListener.SwipeDirection swipeDirection) {
        mActivitySwipeDismissListener.setSwipeDirection(swipeDirection);
    }

    /**
     * Not in use ATM.
     */
    public void setTouchMode(int i) {
        mTouchMode = i;
    }

    public void setBackgrounDimOverlay(ImageView overlayImageView) {
        mBgDimOverlay = overlayImageView;
    }

/* MAGIC HAPPENS HERE */

    /**
     * Intercepting touch events is difficult to understand. See onInterceptTouchEvent()'s
     * documentation for full info.
     * <p/>
     * In short, as long as this method returns false, the touch events are passed down
     * to its child Views.
     * <p/>
     * As soon as it returns true, Android will start passing all touch events to
     * {@link #onTouchEvent(android.view.MotionEvent)} and the child Views of this
     * ViewGroup will no longer receive any touch events UNTIL the user lifts his finger again
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {

        if (!isEnabled() || mSwipeDisabled)
            return false;

        switch (motionEvent.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                if (!thisTouchAllowed(motionEvent)) break;

                mLastDownX = motionEvent.getRawX();
                mLastDownY = motionEvent.getRawY();

                // forawrding this DOWN event to our gesture listener
                mActivitySwipeDismissListener.onTouch(this, motionEvent);

                // if touch started from the edge
                if (touchStartedFromEdge()) {
                    //mInterceptTouch = true;
                }

                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                mInterceptTouch = false;

                // and UP event too
                mActivitySwipeDismissListener.onTouch(this, motionEvent);

                break;

            case MotionEvent.ACTION_MOVE:

                float deltaX = motionEvent.getRawX() - mLastDownX;
                float deltaY = motionEvent.getRawY() - mLastDownY;
                float deltaXAbs = Math.abs(deltaX);
                float deltaYAbs = Math.abs(deltaY);

                // if the user is swiping in the allowed direction
                if (deltaXAbs > mSlop || deltaYAbs > mSlop) {

                    // determining if the swipe started from the window edge
                    if (touchStartedFromEdge()) {
                        mEdgeSwiping = true;
                    }

                    // CONDITIONS:
                    // 1. If swiping from edge, track all horizontal movements
                    // 2. If swiping from elsewhere, track pure horizontal movements only
                    //    (i.e., horizontal distance covered should be more than the vertical
                    //    distance covered)

                    float directionRatio = deltaXAbs / deltaYAbs;

                    // and is swiping horizontally (not even diagonally
                    // (deltaYAbs < deltaXAbs / 2)
                    //if(directionRatio > 1.5 || (mEdgeSwiping && directionRatio > 0.5f)){
                    if (mEdgeSwiping && directionRatio > 0.5f) {
                        //Log.i(TAG, "Swiping activity");
                        mInterceptTouch = true;
                    }

                }

                break;

        }

        /*Log.i(TAG, "doReturn: " + doReturn + ", mIsBeingDragged: " + mIsBeingDragged
                + ", mQuickReturn: " + mQuickReturn);*/

        return mInterceptTouch;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled() || mActivitySwipeDismissListener == null || !thisTouchAllowed(event))
            return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                //Log.d(TAG, "Finger lifted");

                // reset everything
                mLastDownX = 0;
                mLastDownY = 0;
                mEdgeSwiping = false;
                mInterceptTouch = false;

                break;
        }

        // forwarding it all to our gesture listener
        return mActivitySwipeDismissListener.onTouch(this, event);

    }

    /*
    * In case we change our minds in future and instead of allowing the
    * user to swipe anywhere on the screen, we want to allow only from display
    * margins, we'll use this
    * */
    private boolean thisTouchAllowed(MotionEvent event) {
        return true;
    }

    private boolean touchStartedFromEdge() {
        //return mEdgeRect.contains((int)mLastDownX, (int)mLastDownY);
        return true;
    }

}
