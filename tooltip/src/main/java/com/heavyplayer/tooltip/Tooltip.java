package com.heavyplayer.tooltip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Tooltip extends ViewGroup {
    // TODO: Most of these could/should be settable.
    private static final int TEXT_SIZE_SP = 15;
    private static final int ARROW_SIDE_SIZE_DP = 10;
    private static final int ROUNDED_CORNERS_RADII_DP = 4;
    private static final int PADDING_VERTICAL_DP = 7;
    private static final int PADDING_HORIZONTAL_DP = 14;

    private final UpdateWindowListener UPDATE_WINDOW_LISTENER = new UpdateWindowListener();

    private Activity mActivity;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private LayoutInflater mLayoutInflater;
    private boolean mVisible;

    private Rect mTarget;
    private View mTargetView;
    private Integer mTargetX, mTargetY;
    private Menu mMenu;
    private int mMenuItemId;

    private int mGravity = Gravity.TOP;
    private Point mDisplaySize = new Point();
    private Point mWindowPosition = new Point();

    private boolean mIsShown = false;
    private boolean mIsDismissed = false;

    private ArrowView mArrowView;
    private BalloonView mBalloonView;

    private int mColor = Color.WHITE;
    private CharSequence mText;
    private int mTextColor = Color.BLACK;

    private OnShowListener mOnShowListener;
    private OnClickListener mOnClickListener;
    private OnDismissListener mOnDismissListener;

    public Tooltip(Activity activity) {
        super(activity);

        mActivity = activity;
        mWindowManager = (WindowManager)activity.getSystemService(Activity.WINDOW_SERVICE);
        mLayoutInflater = (LayoutInflater)activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    public void show() {
        // If show(), it's no longer dismissed.
        mIsDismissed = false;

        // Make sure there are no views (in case show() is called twice).
        removeAllViews();

        // Create and add inner views.
        mArrowView = new ArrowView(mActivity);
        addView(mArrowView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mBalloonView = new BalloonView(mActivity);
        addView(mBalloonView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        // TODO: Configure the views here instead of having the values read from super (this).

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mOnClickListener != null)
                    mOnClickListener.onClick(Tooltip.this);
                else
                    dismiss();
            }
        };
        mArrowView.setOnClickListener(onClickListener);
        mBalloonView.setOnClickListener(onClickListener);

        locateTarget(new OnTargetExtractedListener() {
            @Override
            public void onTargetExtracted(boolean immediate, boolean visible, boolean changed) {
                if(mIsDismissed)
                    return;

                calculateGravity();
                calculateWindowPosition();

                mWindowLayoutParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        mWindowPosition.x,
                        mWindowPosition.y,
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
                mWindowLayoutParams.windowAnimations = R.style.TooltipAnimation;
                mWindowLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
                mWindowManager.addView(Tooltip.this, mWindowLayoutParams);

                if(visible) {
                    mVisible = true;
                } else {
                    setVisibility(View.GONE);
                    mVisible = false;
                }

                if(mTargetView != null)
                    mTargetView.getViewTreeObserver().addOnPreDrawListener(UPDATE_WINDOW_LISTENER);

                mIsShown = true;

                if(mOnShowListener != null)
                    mOnShowListener.onShow(Tooltip.this);
            }
        });
    }

    public void dismiss() {
        if(mIsShown) {
            if(mTargetView != null)
                mTargetView.getViewTreeObserver().removeOnPreDrawListener(UPDATE_WINDOW_LISTENER);

            mWindowLayoutParams.windowAnimations = R.style.TooltipAnimation;
            mWindowManager.updateViewLayout(this, mWindowLayoutParams);
            mWindowManager.removeView(this);

            mIsShown = false;

            if(mOnDismissListener != null)
                mOnDismissListener.onDismiss(this);
        }

        mIsDismissed = true;
    }

    private void updateWindow() {
        locateTarget(new OnTargetExtractedListener() {
            @Override
            public void onTargetExtracted(boolean immediate, boolean visible, boolean changed) {
                if(changed && mIsShown && !mIsDismissed) {
                    calculateWindowPosition();

                    if(mVisible && !visible) {
                        mWindowLayoutParams.windowAnimations = R.style.TooltipAnimation;
                        setVisibility(View.GONE);
                        mVisible = false;
                    }
                    else if(!mVisible && visible) {
                        // Don't use the WindowManager's animation since it doesn't onLayout() until it ends.
                        Animation inAnim = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_in);
                        mArrowView.startAnimation(inAnim);
                        mBalloonView.startAnimation(inAnim);

                        mWindowLayoutParams.windowAnimations = 0;
                        setVisibility(View.VISIBLE);
                        mVisible = true;
                    }

                    mWindowLayoutParams.x = mWindowPosition.x;
                    mWindowLayoutParams.y = mWindowPosition.y;

                    mWindowManager.updateViewLayout(Tooltip.this, mWindowLayoutParams);
                }
            }
        });
    }

    /**
     * Set the view which is targeted by this tooltip.
     */
    public void setTarget(View targetView) {
        mTargetView = targetView;
    }

    /**
     * Set the coordinates which are targeted by this tooltip.
     *
     * @param targetX the X center coordinate
     * @param targetY the Y center coordinate
     */
    public void setTarget(int targetX, int targetY) {
        mTargetX = targetX;
        mTargetY = targetY;
    }

    /**
     * Set the menu item targeted by this tooltip.
     *
     * Only visible Action Bar menu items are supported. When used with collapsed
     * menu items, or the old bottom menus, the behaviour is undefined.
     */
    public void setTarget(Menu menu, int menuItemId) {
        MenuItem item = menu.findItem(menuItemId);
        View actionView = item.getActionView();
        if(actionView != null) {
            setTarget(actionView);
        }
        else {
            mMenu = menu;
            mMenuItemId = menuItemId;
        }
    }

    /**
     * Set the tooltip's color.
     */
    public void setColor(int color) {
        mColor = color;
    }

    /**
     * Set the text that appears in the tooltip.
     */
    public void setText(CharSequence text) {
        mText = text;
    }

    /**
     * Set the tooltip's text color.
     */
    public void setTextColor(int textColor) {
        mTextColor = textColor;
    }

    /**
     * Set the listener to be invoked when the tooltip is shown.
     */
    public void setOnShowListener(OnShowListener listener) {
        mOnShowListener = listener;
    }

    /**
     * Set the listener to be invoked when the tooltip is shown. By default, when a tooltip is tapped, it's dismissed.
     */
    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    /**
     * Set the listener to be invoked when the tooltip is dismissed.
     */
    public void setOnDismissListener(OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    @SuppressLint("NewApi")
    private void locateTarget(final OnTargetExtractedListener onTargetExtractedListener) {
        final Rect previousTarget = mTarget;

        if(mTargetX != null && mTargetY != null) {
            mTarget = new Rect(mTargetX, mTargetY, mTargetX, mTargetY);

            if(onTargetExtractedListener != null) {
                calculateDisplaySize();
                onTargetExtractedListener.onTargetExtracted(
                        true,
                        ((mTarget.right >= 0 || mTarget.left <= mDisplaySize.x) && (mTarget.top >= 0 || mTarget.bottom <= mDisplaySize.y)),
                        !mTarget.equals(previousTarget)
                );
            }
        }
        else if(mTargetView != null) {
            locateTargetByView(mTargetView);

            if(onTargetExtractedListener != null) {
                Rect rect = new Rect();
                onTargetExtractedListener.onTargetExtracted(
                        true,
                        mTargetView.getLocalVisibleRect(rect) && mTargetView.isShown(),
                        !mTarget.equals(previousTarget)
                );
            }
        }
        else if(mMenu != null) {
            final MenuItem item = mMenu.findItem(mMenuItemId);
            if(item != null) {
                final ViewGroup actionView = getActionView(item.getIcon());

                if(actionView != null) {
                    item.setActionView(actionView);
                    actionView.post(new Runnable() {
                        @Override
                        public void run() {
                            locateTargetByView(actionView);

                            if(onTargetExtractedListener != null)
                                onTargetExtractedListener.onTargetExtracted(
                                        false,
                                        actionView.getLocalVisibleRect(new Rect()) && actionView.isShown(),
                                        !mTarget.equals(previousTarget)
                                );

                            item.setActionView(null);
                        }
                    });
                }
            }
        }
        else if(onTargetExtractedListener != null) {
            onTargetExtractedListener.onTargetExtracted(true, false, previousTarget != null);
        }
    }

    private ViewGroup getActionView(Drawable icon) {
        ViewGroup actionView = (ViewGroup)mLayoutInflater.inflate(R.layout.ab_placeholder_item,
                                                                  new LinearLayout(mActivity), false);
        if(actionView != null) {
            ImageView iconView = (ImageView)actionView.getChildAt(0);
            if(iconView != null)
                iconView.setImageDrawable(icon);
        }
        return actionView;
    }

    private void calculateGravity() {
        if(mTarget == null)
            throw new IllegalStateException("You must set some target.");

        calculateDisplaySize();

        // Multiply each space by the opposite action to uniform the scale.
        int leftSpace = mTarget.left * mDisplaySize.y;
        int topSpace = mTarget.top * mDisplaySize.x;
        int rightSpace = (mDisplaySize.x - mTarget.right) * mDisplaySize.y;
        int bottomSpace = (mDisplaySize.y - mTarget.bottom) * mDisplaySize.x;

        int mostSpacious = Math.max(leftSpace, Math.max(topSpace, Math.max(rightSpace, bottomSpace)));

        // Prefer TOP or BOTTOM by allowing them to be up to 30% smaller.
        if(topSpace == mostSpacious || (bottomSpace != mostSpacious && topSpace > 0.7 * mostSpacious))
            mGravity = Gravity.TOP;
        else if(bottomSpace == mostSpacious || (topSpace != mostSpacious && bottomSpace > 0.7 * mostSpacious))
            mGravity = Gravity.BOTTOM;
        else if(leftSpace == mostSpacious)
            mGravity = Gravity.LEFT;
        else if(rightSpace == mostSpacious)
            mGravity = Gravity.RIGHT;
    }

    private void calculateWindowPosition() {
        calculateDisplaySize();

        ensureChildrenMeasured();
        int arrowWidth = mArrowView.getMeasuredWidth();
        int arrowHeight = mArrowView.getMeasuredHeight();
        int balloonWidth = mBalloonView.getMeasuredWidth();
        int balloonHeight = mBalloonView.getMeasuredHeight();

        int targetCenterX = getTargetVisibleCenterX();
        int targetCenterY = getTargetVisibleCenterY();
        int targetWidth = getTargetVisibleWidth();
        int targetHeight = getTargetVisibleHeight();

        // Set common properties.
        switch(mGravity) {
            case Gravity.TOP:
            case Gravity.BOTTOM:
                mWindowPosition.x = targetCenterX - balloonWidth / 2;
                break;

            case Gravity.LEFT:
            case Gravity.RIGHT:
                mWindowPosition.y = targetCenterY - balloonHeight / 2;
                break;
        }

        // Set individual properties.
        switch(mGravity) {
            case Gravity.TOP:
                mWindowPosition.y = mTarget.top - balloonHeight - Math.min(arrowHeight / 2, targetHeight / 2);
                break;

            case Gravity.BOTTOM:
                mWindowPosition.y = mTarget.bottom - Math.min(arrowHeight / 2, targetHeight / 2);
                break;

            case Gravity.LEFT:
                mWindowPosition.x = mTarget.left - balloonWidth - Math.min(arrowWidth / 2, targetWidth / 2);
                break;

            case Gravity.RIGHT:
                mWindowPosition.x = mTarget.right - Math.min(arrowWidth / 2, targetWidth / 2);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ensureChildrenMeasured();
        int arrowWidth = mArrowView.getMeasuredWidth();
        int arrowHeight = mArrowView.getMeasuredHeight();
        int balloonWidth = mBalloonView.getMeasuredWidth();
        int balloonHeight = mBalloonView.getMeasuredHeight();

        switch(mGravity) {
            case Gravity.TOP:
            case Gravity.BOTTOM:
                setMeasuredDimension(balloonWidth, arrowHeight + balloonHeight);
                break;

            case Gravity.LEFT:
            case Gravity.RIGHT:
                setMeasuredDimension(arrowWidth + balloonWidth, balloonHeight);
                break;

            default:
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                break;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        calculateDisplaySize();

        int width = right - left;
        int height = bottom - top;

        ensureChildrenMeasured();
        int arrowWidth = mArrowView.getMeasuredWidth();
        int arrowHeight = mArrowView.getMeasuredHeight();
        int balloonWidth = mBalloonView.getMeasuredWidth();
        int balloonHeight = mBalloonView.getMeasuredHeight();

        // Set the positions.
        int arrowLeft = trim(0, mWindowPosition.x + width - mDisplaySize.x, mWindowPosition.x);
        int arrowTop = trim(0, mWindowPosition.y + height - mDisplaySize.y, mWindowPosition.y);
        int balloonLeft = arrowLeft;
        int balloonTop = arrowTop;

        // Position arrow and balloon with respect to each other.
        switch(mGravity) {
            case Gravity.TOP:
                arrowLeft += width / 2 - arrowWidth / 2;
                arrowTop += balloonHeight;
                break;

            case Gravity.BOTTOM:
                arrowLeft += width / 2 - arrowWidth / 2;
                balloonTop += arrowHeight;
                break;

            case Gravity.LEFT:
                arrowLeft += balloonWidth;
                arrowTop += height / 2 - arrowHeight / 2;
                break;

            case Gravity.RIGHT:
                arrowTop += height / 2 - arrowHeight / 2;
                balloonLeft += arrowWidth;
                break;
        }

        // Don't let the balloon be out of the screen when there's no need.
        switch(mGravity) {
            case Gravity.TOP:
            case Gravity.BOTTOM:
                if(balloonLeft < 0)
                    balloonLeft = Math.min(0, balloonLeft + (balloonWidth / 2 - arrowWidth + getRoundedCornersRadii()));
                else if(mWindowPosition.x > mDisplaySize.x - balloonWidth)
                    balloonLeft = Math.max(0, (mWindowPosition.x - (balloonWidth / 2 - arrowWidth + getRoundedCornersRadii())) - (mDisplaySize.x - balloonWidth));
                break;

            case Gravity.LEFT:
            case Gravity.RIGHT:
                if(balloonTop < 0)
                    balloonTop = Math.min(0, balloonTop + (balloonHeight / 2 - arrowHeight + getRoundedCornersRadii()));
                else if(mWindowPosition.y > mDisplaySize.y - balloonHeight)
                    balloonTop = Math.max(0, (mWindowPosition.y - (balloonHeight / 2 - arrowHeight + getRoundedCornersRadii())) - (mDisplaySize.y - balloonHeight));
                break;
        }

        // Lay out the views.
        mArrowView.layout(arrowLeft, arrowTop, arrowLeft + arrowWidth, arrowTop + arrowHeight);
        mBalloonView.layout(balloonLeft, balloonTop, balloonLeft + balloonWidth, balloonTop + balloonHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // The OnClickListener is set on the child views, so all touch events that reach this view should be sent to the
        // underlying activity. Before we do that, we need to adjust the coordinates.
        event.offsetLocation(trim(mWindowPosition.x, 0, mDisplaySize.x), trim(mWindowPosition.y, 0, mDisplaySize.y));
        mActivity.dispatchTouchEvent(event);
        return false;
    }

    private void ensureChildrenMeasured() {
        int arrowWidth = mArrowView.getMeasuredWidth();
        int arrowHeight = mArrowView.getMeasuredHeight();

        if(arrowWidth == 0 || arrowHeight == 0) {
            int arrowWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mDisplaySize.x, MeasureSpec.AT_MOST);
            int arrowHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mDisplaySize.y, MeasureSpec.AT_MOST);
            measureChild(mArrowView, arrowWidthMeasureSpec, arrowHeightMeasureSpec);
        }

        int balloonWidth = mBalloonView.getMeasuredWidth();
        int balloonHeight = mBalloonView.getMeasuredHeight();

        if(balloonWidth == 0 || balloonHeight == 0) {
            int balloonWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mDisplaySize.x - arrowWidth, MeasureSpec.AT_MOST);
            int balloonHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mDisplaySize.y - arrowHeight,
                                                                       MeasureSpec.AT_MOST);
            measureChild(mBalloonView, balloonWidthMeasureSpec, balloonHeightMeasureSpec);
        }
    }

    // TODO: Cache these values.
    private int getArrowSideSize() {
        int px = dpToPx(ARROW_SIDE_SIZE_DP);
        return (px & 1) == 0 ? px : px -1; // Ensure it's even.
    }

    private int getRoundedCornersRadii() {
        return dpToPx(ROUNDED_CORNERS_RADII_DP);
    }

    private int getPaddingVertical() {
        return dpToPx(PADDING_VERTICAL_DP);
    }

    private int getPaddingHorizontal() {
        return dpToPx(PADDING_HORIZONTAL_DP);
    }

    private int getTargetVisibleCenterX() {
        return (trim(mTarget.right, 0, mDisplaySize.x) + trim(mTarget.left, 0, mDisplaySize.x)) >> 1;
    }

    private int getTargetVisibleCenterY() {
        return (trim(mTarget.bottom, 0, mDisplaySize.y) + trim(mTarget.top, 0, mDisplaySize.y)) >> 1;
    }

    private int getTargetVisibleWidth() {
        return trim(mTarget.right, 0, mDisplaySize.x) - trim(mTarget.left, 0, mDisplaySize.x);
    }

    private int getTargetVisibleHeight() {
        return trim(mTarget.bottom, 0, mDisplaySize.y) - trim(mTarget.top, 0, mDisplaySize.y);
    }

    private int dpToPx(int dpValue) {
        return (int)TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dpValue, mActivity.getResources().getDisplayMetrics());
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void calculateDisplaySize() {
        View decorView = mActivity.getWindow().getDecorView();
        int[] position = new int[2];
        decorView.getLocationOnScreen(position);

        mDisplaySize.x = decorView.getWidth() - position[0];
        mDisplaySize.y = decorView.getHeight() - position[1];
    }

    private void locateTargetByView(View view) {
        int[] position = new int[2];
        view.getLocationInWindow(position);

        mTarget = new Rect();
        mTarget.left = position[0];
        mTarget.top = position[1];
        mTarget.right = mTarget.left + view.getWidth();
        mTarget.bottom = mTarget.top + view.getHeight();
    }

    private int trim(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public interface OnShowListener {
        public void onShow(Tooltip tooltip);
    }

    public interface OnClickListener {
        public void onClick(Tooltip tooltip);
    }

    public interface OnDismissListener {
        public void onDismiss(Tooltip tooltip);
    }

    private class BalloonView extends TextView {
        private BalloonView(Context context) {
            super(context);

            setText(mText);
            setTextColor(mTextColor);
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP);
            setTypeface(null, Typeface.BOLD);
            setSingleLine(false);
            setGravity(Gravity.CENTER);
            setPadding(getPaddingHorizontal(), getPaddingVertical(), getPaddingHorizontal(), getPaddingVertical());
            setBackground(new ShapeDrawable(new ColoredRoundRectShape(getRoundedCornersRadii())));
        }

        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void setBackground(Drawable background) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                super.setBackground(background);
            else
                super.setBackgroundDrawable(background);
        }

        private class ColoredRoundRectShape extends RoundRectShape {
            private Paint mPaint;

            private ColoredRoundRectShape(float radii) {
                super(new float[]{radii, radii, radii, radii, radii, radii, radii, radii}, null, null);

                mPaint = new Paint();
                mPaint.setAntiAlias(true);
                mPaint.setColor(mColor);
            }

            @Override
            public void draw(Canvas canvas, Paint paint) {
                super.draw(canvas, mPaint);
            }
        }
    }

    private class ArrowView extends View {
        private Paint mPaint;
        private Path mPath;

        public ArrowView(Context context) {
            super(context);
        }

        private void ensurePaint() {
            if(mPaint == null) {
                mPaint = new Paint();
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setAntiAlias(true);
                mPaint.setColor(mColor);
            }
        }

        private void ensurePath() {
            if(mPath == null) {
                mPath = new Path();
                mPath.setFillType(Path.FillType.EVEN_ODD);

                int otherSideSize = getOtherSideSize();
                mPath.moveTo(0, 0);
                mPath.lineTo(otherSideSize / 2, getArrowSideSize());
                mPath.lineTo(otherSideSize, 0);

                mPath.close();
            }
        }

        private int getOtherSideSize() {
            return getArrowSideSize() * 2;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            switch(mGravity) {
                case Gravity.TOP:
                case Gravity.BOTTOM:
                    setMeasuredDimension(getOtherSideSize(), getArrowSideSize());
                    break;

                case Gravity.LEFT:
                case Gravity.RIGHT:
                    setMeasuredDimension(getArrowSideSize(), getOtherSideSize());
                    break;

                default:
                    setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
                    break;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            ensurePaint();
            ensurePath();

            int count = canvas.save();
            switch(mGravity) {
                case Gravity.BOTTOM:
                    canvas.rotate(180, getOtherSideSize() / 2, getArrowSideSize() / 2);
                    break;

                case Gravity.LEFT:
                    canvas.rotate(-90, getOtherSideSize() / 2, getOtherSideSize() / 2);
                    break;

                case Gravity.RIGHT:
                    canvas.rotate(90, getArrowSideSize() / 2, getArrowSideSize() / 2);
                    break;

                default:
                    break;
            }
            canvas.drawPath(mPath, mPaint);
            canvas.restoreToCount(count);
        }
    }

    private interface OnTargetExtractedListener {
        /**
         * @param immediate true if we were able to extract the target location immediately, false if not
         * @param visible true if the target is currently visible on screen, false if not
         * @param changed true if the target changed its location, false if not
         */
        void onTargetExtracted(boolean immediate, boolean visible, boolean changed);
    }

    private class UpdateWindowListener implements ViewTreeObserver.OnPreDrawListener {
        @Override
        public boolean onPreDraw() {
            updateWindow();
            return true;
        }
    }
}
