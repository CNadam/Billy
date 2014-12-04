package com.vibin.billy.swipeable;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

/**
 * Created by Saketme on 1/22/14.
 * Common stuff for animation:
 * <p/>
 * 1. Animate transition of images in an ImageView.
 * {@link #animateTransitionOfImage(ImageView, Drawable, boolean, int, ImageView.ScaleType)}
 */
public class AnimationUtils {

    /**
     * Interpolators
     */
    public static final Interpolator EASE_ACCELERATE_DEACELERATE_INTERPOLATOR = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    public static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    public static final Interpolator DECELERATE_INTERPOLATOR_FAST = new DecelerateInterpolator(2);

    /**
     * Animation durations
     */
    public static final int SUPER_QUICK_ANIM_DURATION = 100;
    public static final int QUICK_ANIM_DURATION = 200;
    public static final int SHORT_ANIM_DURATION = 300;
    public static final int MEDIUM_ANIM_DURATION = 500;
    public static final int LONG_ANIM_DURATION = 700;

    /**
     * Animates the changing of a ImageView (or its subclasses like ImageButton)'s photo.
     *
     * @param crossFadeEnabled Enables or disables the cross fade of the drawables. When cross fade
     *                         is disabled, the first drawable is always drawn opaque. With cross
     *                         fade enabled, the first drawable is drawn with the opposite alpha of
     *                         the second drawable.
     * @param animDuration     Duration for which this animation should run.
     */
    public static void animateTransitionOfImage(ImageView imageView, Drawable newDrawable,
                                                boolean crossFadeEnabled, int animDuration,
                                                final ImageView.ScaleType imageViewScaleType) {

        // TransitionDrawable doesn't accept null Drawables
        if (imageView.getDrawable() == null)
            imageView.setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (newDrawable == null)
            newDrawable = new ColorDrawable(Color.TRANSPARENT);

        TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{
                imageView.getDrawable(),
                newDrawable
        });

        imageView.setImageDrawable(transitionDrawable);
        imageView.setScaleType(imageViewScaleType);
        transitionDrawable.setCrossFadeEnabled(crossFadeEnabled);
        transitionDrawable.startTransition(animDuration);
    }

    /**
     * Same as previous method, but uses default parameters that are good enough for general
     * cases in our app.
     *
     * @return Duration of animation for which this animation will run
     */
    public static long animateTransitionOfImage(ImageView imageView, Drawable newDrawable) {
        animateTransitionOfImage(imageView, newDrawable, true,
                AnimationUtils.QUICK_ANIM_DURATION, ImageView.ScaleType.CENTER_CROP);

        return AnimationUtils.QUICK_ANIM_DURATION;
    }

}
