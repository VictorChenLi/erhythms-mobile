
package com.erhythms.main;

import java.util.Vector;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class HorizontalScrollInViewGroup extends ViewGroup {

	private boolean allowNext;
	private Scroller mScroller;
	private VelocityTracker mVelocityTracker;
	private int mCurScreen;
	private int mDefaultScreen = 0;
	private static final int TOUCH_STATE_REST = 0;
	private static final int TOUCH_STATE_SCROLLING = 1;
	private static final int SNAP_VELOCITY = 500;
	private int mTouchState = TOUCH_STATE_REST;
	private int mTouchSlop;
	private float mLastMotionX;
	private int sensitivity = 30;
	private boolean spring;
	private Vector<HorizontalScrollInViewGroupListener> listeners;
	
	//used for the screen changed listener
	private OnScreenChangedListener screenChangedCallBack;
	
	public HorizontalScrollInViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mScroller = new Scroller(context);
		setCurScreen(mDefaultScreen);
		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		listeners = new Vector<HorizontalScrollInViewGroupListener>();
		
		//initiate the screen change listener
		screenChangedCallBack = (OnScreenChangedListener)context;
		
	}
	
	
	// An interface that tells the activity the screen is switched
    public interface OnScreenChangedListener {
        public void onScreenChanged(int currentScreen);
    }
	
	
	/* (non-Javadoc)
	 * @see android.view.ViewGroup#onLayout(boolean, int, int, int, int)
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		int childLeft = 0;
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childWidth = childView.getMeasuredWidth();
				childView.layout(childLeft, 0, childLeft + childWidth,
						childView.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"HorizontalScrollInViewGroup only canmCurScreen run at EXACTLY mode!");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"HorizontalScrollInViewGroup only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		scrollTo(getCurScreen() * width, 0);
	}
	
	@Override
	public void computeScroll() {
		// TODO Auto-generated method stub
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		
		if (mVelocityTracker == null)
			mVelocityTracker = VelocityTracker.obtain();
		mVelocityTracker.addMovement(event);
		
		final int action = event.getAction();
		final float x = event.getX();
		
		
		switch (action) {
		
		case MotionEvent.ACTION_DOWN:
			
			if (!mScroller.isFinished())
				mScroller.abortAnimation();
			mLastMotionX = x;
			break;
			
		case MotionEvent.ACTION_MOVE:
			
			int deltaX = (int) (mLastMotionX - x);
			
			if (Math.abs(deltaX) > sensitivity) {
				if (spring) {
					scrollBy(deltaX, 0);
					mLastMotionX = x;
				} else {
					final int childCount = getChildCount();
					boolean max = getCurScreen() < childCount - 1;
					boolean min = getCurScreen() > 0;
					boolean canMove = deltaX > 0 ? (max ? allowNext : false)
							: (min ? true : false);
					
					Log.v("debugtag",getCurScreen()+".canMove="+canMove);
					
					if (canMove) {
						scrollBy(deltaX, 0);
						mLastMotionX = x;
					}
				}
			}
			
			break;
			
		case MotionEvent.ACTION_UP:
			
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000);
			int velocityX = (int) velocityTracker.getXVelocity();
			
			if (velocityX > SNAP_VELOCITY && getCurScreen() > 0) {
				
				// Fling enough to move left
				snapToScreen(getCurScreen() - 1);
				
			} else if (velocityX < -SNAP_VELOCITY && getCurScreen() < getChildCount() - 1 && allowNext) {
				// Fling enough to move right
				snapToScreen(getCurScreen() + 1);
			
			} else {
				
				snapToDestination();
			
			}
			
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			
			mTouchState = TOUCH_STATE_REST;
			break;
		
		case MotionEvent.ACTION_CANCEL:
		
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return true;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState != TOUCH_STATE_REST))
			return true;
		final float x = ev.getX();
		switch (action) {
		case MotionEvent.ACTION_MOVE:
			final int xDiff = (int) Math.abs(mLastMotionX - x);
			if (xDiff > mTouchSlop)
				mTouchState = TOUCH_STATE_SCROLLING;
			break;
		case MotionEvent.ACTION_DOWN:
			mLastMotionX = x;
			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			break;
		}
		return mTouchState != TOUCH_STATE_REST;
	}
	
	public void snapToDestination() {
		final int screenWidth = getWidth();
		final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
		snapToScreen(destScreen);
		
		//call back to let the activity know the current screen
		screenChangedCallBack.onScreenChanged(getCurScreen());
	}
	
	public void snapToScreen(int whichScreen) {
		// get the valid layout page
		int lastIndex = getCurScreen();
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		if (getScrollX() != (whichScreen * getWidth())) {

			final int delta = whichScreen * getWidth() - getScrollX();
			mScroller.startScroll(getScrollX(), 0, delta, 0,
					Math.abs(delta) * 1);
			setCurScreen(whichScreen);
			invalidate(); // Redraw the layout
		}
		for (HorizontalScrollInViewGroupListener listener : listeners)
			listener.turnPage(lastIndex, whichScreen);
		
		//call back to let the activity know the current screen
		screenChangedCallBack.onScreenChanged(getCurScreen());
	}
	
	
	public void setToScreen(int whichScreen) {
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		setCurScreen(whichScreen);
		scrollTo(whichScreen * getWidth(), 0);
		
		//call back to let the activity know the current screen
		screenChangedCallBack.onScreenChanged(getCurScreen());
	}

	public int getCurScreen() {
		return mCurScreen;
	}

	public void setCurScreen(int mCurScreen) {
		this.mCurScreen = mCurScreen;
	}

	public boolean isAllowNext() {
		return allowNext;
	}

	public void setAllowNext(boolean allowNext) {
		this.allowNext = allowNext;
	}
}
