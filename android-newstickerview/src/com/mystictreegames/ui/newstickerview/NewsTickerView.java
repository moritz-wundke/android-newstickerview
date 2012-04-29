package com.mystictreegames.ui.newstickerview;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The NewsTickerView is a custom {@link TestView} that has automarquee and features a list of news
 * to display. A news is an instance of {@link NewsHolder} which implements the {@link Parcelable}
 * interface so it can be exchanged easily.
 * <p>
 * The NewsTickerView comes with a time-line so the user knows when the news will change, it also features
 * simple method to set it as loading by changing the visibility of a referenced view.
 * <p>
 * Clicking on the ticker will open the assigned URL which can be a simple URI for the intent system. Swiping
 * will change to the next news.
 * <p>
 * <b>TODO:</b>
 * <ul>
 * <li>News counter: add a page like or bullet list type feature so the user knows how many news and in which one they are</li>
 * </ul>
 * @author Moritz 'Moss' Wundke (b.thax.dcg@gmail.com)
 *
 */
public class NewsTickerView extends TextView implements OnTouchListener {
	public static final String TAG = "NewsTickerView";
	
	/** Our news list */
	private List<NewsHolder> mNewsList = null;
	
	public static final int DEFAULT_UPDATE_RATE = 10;
	public static final int MIN_UPDATE_RATE = 2;
	
	public static final int FADE_ANIMATION_RATE = 33;
	public static final float FADE_ANIMATION_RATE_SECS = FADE_ANIMATION_RATE/1000.f;
	public static final int FADE_TIME = 1;
	
	/** Current applied alpha value */
	private float mFadeAlpha = 1;

	/** THe index that points to the current news */
	private int mIndex = -1;
	
	/** Loading text */
	private String mLoadingText = "";
	
	/** Error message */
	private String mLoadingErrorText = "";
	
	/** No news text */
	private String mNoNewsText = "";
	
	/** Controls whether we has been detached */
	private boolean bDetached = false;
	
	/** Last registered motion event */
	private int mLastMotionEvent = -1;
	
	/** The news animation handler that will swap to the next news time by time */
	private NewsTickerHandler mNewsTickerHandler;
	
	/** Fade in/out handler */
	private FadeAnimationHandler mFadeAnimationHandler;
	
	/** Will go from 0 to 1, used to handle the time left until the next news swap */
	private float mTimeLeft = 0;
	
	/** Current time the news will be active */
	private long mTimeToHandle;
	
	/** Time we started showing the current news */
	private long mStartTime;
	
	/** Paint instance used to draw a thin line tat indicates when the news get's swapped */
	private Paint mTimeLeftPaint = new Paint();
	
	/** ARGB color for the time line */
	private int mTimeLineColor = 0xFFCCCCCC;
	
	/** Width of the time line */
	private int mTimeLineWidth = 2;
	
	/** Enable/Disable the TimeLine */
	private boolean bEnableTimeLine = true;
	
	/** ResId of the ImageView that will be used to mark the loading process */
	private int mLoadingImageViewId = 0;
	
	/** Reference to the loading image view */
	private ImageView mLoadingImageView = null;
	
	/** Reference to the animation of the ImageView if any */
	private Animation mLoadingImageAnimation = null;
	
	/** Flag used to mak that a loading error occurred */
	private boolean bLoadingError;
	
	/** Flag used to control whether we are loading news or not */
	private boolean bIsLoadingNews;
	
	public NewsTickerView(Context context) {
		super(context);
		
		setOnTouchListener(this);
		init();
	}
	
	public NewsTickerView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.preferenceStyle);
	}
	
	public NewsTickerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.NewsTickerView, defStyle, 0);		
		mTimeLineColor = attributes.getColor(R.styleable.NewsTickerView_timeLineColor, mTimeLineColor);
		mTimeLineWidth = attributes.getInt(R.styleable.NewsTickerView_timeLineWidth, mTimeLineWidth);
		bEnableTimeLine = attributes.getBoolean(R.styleable.NewsTickerView_enableTimeLine, bEnableTimeLine);
		mLoadingImageViewId = attributes.getResourceId(R.styleable.NewsTickerView_loadingAnimationImageView, 0);
		mLoadingText = attributes.getString(R.styleable.NewsTickerView_loadingText);
		if ( mLoadingText == null )
			mLoadingText = getResources().getString(R.string.newsticker_loadingtext);
		mLoadingErrorText = attributes.getString(R.styleable.NewsTickerView_loadingErrorText);
		if ( mLoadingErrorText == null )
			mLoadingErrorText = getResources().getString(R.string.newsticker_loadingerrortext);
		mNoNewsText = attributes.getString(R.styleable.NewsTickerView_noNewsText);
		if ( mNoNewsText == null )
			mNoNewsText = getResources().getString(R.string.newsticker_nonewstext);
		attributes.recycle();
		
		setOnTouchListener(this);
		init();
	}
	
	private void init() {
		// Get rid of any text that could be in here. We will set the news for it
		setText("");
		mNewsTickerHandler = new NewsTickerHandler();
		mFadeAnimationHandler = new FadeAnimationHandler();
		mTimeLeftPaint.setColor(mTimeLineColor);
		mTimeLeftPaint.setStrokeWidth(mTimeLineWidth);
	}
	
	/** Set the res id of the view that will be used as the loading image */
	public void setLoadingImageView(int resId) {
		mLoadingImageViewId = resId;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
	    if(!(state instanceof SavedState)) {
	      super.onRestoreInstanceState(state);
	      return;
	    }
	    SavedState ss = (SavedState)state;
	    super.onRestoreInstanceState(ss.getSuperState());
	    
	    // Minus one because we will change the news when we are attached to the parent layout
	    this.mIndex = ss.mNewsIndex-1;
	}
	
	@Override
	public Parcelable onSaveInstanceState() {
	    Parcelable superState = super.onSaveInstanceState();
	    SavedState ss = new SavedState(superState);
	    ss.mNewsIndex = this.mIndex;

	    return ss;
	}
	
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
	}
	
	/** Try to cache the loading image */
	private void cacheLoadingImage() {
		// Now get image view
		try {
			View parent = (View)getParent();
	        if (parent != null) {
	        	mLoadingImageView = (ImageView) parent.findViewById(mLoadingImageViewId);
	        	mLoadingImageAnimation = mLoadingImageView.getAnimation();
	        }
		} catch (Exception e) {
    		e.printStackTrace();
    		mLoadingImageView = null;
    	}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		// Start ticking :D
		changeNews();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		// Once detached stop all handlers
		synchronized (this) {
			bDetached=true;
			mNewsTickerHandler.removeMessages(0);
			mFadeAnimationHandler.removeMessages(FadeAnimationHandler.FADE_IN_CODE);
			mFadeAnimationHandler.removeMessages(FadeAnimationHandler.FADE_OUT_CODE);
			
			// Release cached images
			if ( mLoadingImageView != null ) {
				mLoadingImageView.setBackgroundDrawable(null);
				mLoadingImageView.clearAnimation();
				mLoadingImageView = null;
				mLoadingImageAnimation = null;
			}
		}
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction,
			Rect previouslyFocusedRect) {
		if ( focused )
			super.onFocusChanged(focused, direction, previouslyFocusedRect);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if ( hasWindowFocus )
			super.onWindowFocusChanged(hasWindowFocus);
	}
	
	@Override
	public boolean isFocused() {
		return true;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean bResult = false;
		if ( !bIsLoadingNews ) {
			int action = event.getAction();
			switch(action)
			{
				case MotionEvent.ACTION_DOWN: 
				case MotionEvent.ACTION_MOVE:
					mLastMotionEvent = action;
					break;
				case MotionEvent.ACTION_UP:
					if ( mLastMotionEvent == MotionEvent.ACTION_DOWN ) {
						// TODO: If there where a loading error launch the loading error delegate!
						// bLoadingError
						// Get the right URL and fire the intent
						if (mNewsList != null && mIndex >= 0 && mIndex < mNewsList.size() ) {
							String link = mNewsList.get(mIndex).mLink;
							if ( link != null && link.length() > 0 ) {
								Intent i = new Intent(Intent.ACTION_VIEW);
								i.setData(Uri.parse(link));
								getContext().startActivity(i);
							}
						}
						bResult = true;
					} else if ( mLastMotionEvent == MotionEvent.ACTION_MOVE ) {
						changeNews();
						bResult = true;
					}
					mLastMotionEvent = -1;
					break;
			}
		}
		return bResult;
	}
	
	/**
	 * Check if we should show the timeline or not
	 */
	public boolean showTimeLine() {
		return bEnableTimeLine && (mNewsList != null && mNewsList.size() > 0);
	}
	
	/**
	 * Called when we are about to start any news fetching, either from the net or from 
	 * our cached version.
	 */
	public void onStartLoading() {
		bIsLoadingNews = true;
		if (mLoadingImageView == null) {
			cacheLoadingImage();
		}
		if ( mLoadingImageView != null ) {
			// Clear text before
			setText("");
			mLoadingImageView.setVisibility(VISIBLE);
			if ( mLoadingImageAnimation != null )
				mLoadingImageView.startAnimation(mLoadingImageAnimation);
		} else {
			setText(mLoadingText);
		}
		invalidate();
	}
	
	/**
	 * Called once we finished loading the news. This get's called also when the fetching fails.
	 */
	public void onStopLoading() {
		bIsLoadingNews = false;
		if (mLoadingImageView == null) {
			cacheLoadingImage();
		}
		if ( mLoadingImageView != null ) {
			mLoadingImageView.setVisibility(INVISIBLE);
			mLoadingImageView.clearAnimation();
		}
	}
	
	/** Set the news :D */
	public void setNews(List<NewsHolder> newsList) {
		mNewsList = newsList;
		bIsLoadingNews = false;
		bLoadingError = false;
		changeNews();
	}
	
	/** We got some news */
	public boolean hasNews() {
		return mNewsList != null && mNewsList.size() > 0;
	}
	
	/** Called when no news could be found. */
	public void onNoNewsFound() {
		bLoadingError = true;
		bIsLoadingNews = false;
		setText(this.mNoNewsText);
	}
	
	/** Called when we failed to load the news. */
	public void onNewsLoadingFailed( String errorText ) {
		bLoadingError = true;
		bIsLoadingNews = false;
		setText(errorText != "" ? errorText: this.mLoadingErrorText);
		onStopLoading();
	}
	
	/** Invoke default loading error */
	public void onNewsLoadingFailed() {
		onNewsLoadingFailed("");
	}
	
	/** Was there a loading error? */
	public boolean HasLoadingError() {
		return bLoadingError;
	}
	
	/**
	 * Change to next news
	 */
	public void changeNews() {
		// Once detached stop all handlers
		synchronized (this) {
			if ( !bDetached && !bIsLoadingNews && mNewsList != null && mNewsList.size() > 0 ) {
				// Update index
				mIndex++;		
				mIndex = mIndex % mNewsList.size();
				
				// Get the news
				NewsHolder news = mNewsList.get(mIndex);					
				if ( news != null ) {					
					// Set text
					setText(news.mNews);
					
					// Start fade-in
					mFadeAnimationHandler.startFadeIn();
					
					// Start time handling too
					if ( showTimeLine() ) {
						mTimeToHandle = (long) ((news.mTime+FADE_TIME*2)*1000);
						mStartTime = System.currentTimeMillis();
					}
					onStopLoading();
				} else {
					// Failed to set news. This should never happen!
					Log.e(TAG, "News loading finished but no news found for index("+mIndex+"), news size("+mNewsList.size()+")");
					onNewsLoadingFailed();
				}
			}
		}
	}
	
	/** Called once we faded the news in */
	public void fadedIn() {
		synchronized (this) {
			if ( !bDetached ) {
				mFadeAlpha = 1.f;
				updateTextAlpha();
				
				// Set new time
				NewsHolder news = mNewsList.get(mIndex);					
				if ( news != null ) {	
					mNewsTickerHandler.sleep((news.mTime-FADE_TIME)*1000);
				}
			}
		}
	}
	
	/** Called once the fade out has been finished */
	public void fadedOut() {
		synchronized (this) {
			if ( !bDetached ) {
				changeNews();
			}
		}
	}
	
	/** Update text with current alpha value */
	protected void updateTextAlpha() {
		NewsTickerView.this.setTextColor(Utils.combineColor(NewsTickerView.this.getCurrentTextColor(),NewsTickerView.this.mFadeAlpha));
	}
	
	@Override
	public void setHorizontalFadingEdgeEnabled(boolean horizontalFadingEdgeEnabled) {
		if ( showTimeLine() ) {
			// If we show the time line always show the horizontal fading edge
			super.setHorizontalFadingEdgeEnabled(true);
		} else {
			super.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled);
		}
	};
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		super.onDraw(canvas);
		
		if ( showTimeLine() ) {
			setHorizontalFadingEdgeEnabled(true);
			final long currentTime = System.currentTimeMillis();
			if ( currentTime <= mStartTime + mTimeToHandle ) {
				mTimeLeft = 1-Utils.clamp((currentTime-mStartTime)/(float)mTimeToHandle,0,1);
				if ( mTimeLeft > 0 ) {				    
					// Get all data we need
					final int right = getRight();
			        final int bottom = getBottom();
			        final int top = getTop();
			        final int left = getLeft();
			        final int height = getHeight();
			        final int paddingLeft = getPaddingLeft();			        
			        final int strokeWidth = (int) (mTimeLeftPaint.getStrokeWidth()*0.5f);			        
			        final int compoundPaddingLeft = getCompoundPaddingLeft();
			        final int compoundPaddingTop = getCompoundPaddingTop();
			        final int compoundPaddingRight = getCompoundPaddingRight();
			        final int compoundPaddingBottom = getCompoundPaddingBottom();
			        final int scrollX = getScrollX();
			        final int scrollY = getScrollY();
			        
			        // Compute the them with it's padding
			        int vspace = bottom - top - compoundPaddingBottom - compoundPaddingTop;
		            int hspace = right - left - compoundPaddingRight - compoundPaddingLeft;

	                canvas.save();
	                // Translate it taking the scrollx and scrolly into account, they are used when centering the text and such
	                // If we do not translate the canvas when the view is centered we would be drawing outside
	                canvas.translate(scrollX + paddingLeft, scrollY + compoundPaddingTop + (vspace - height) / 2);
	                canvas.drawLine(left, vspace-strokeWidth, hspace*mTimeLeft, vspace-strokeWidth, mTimeLeftPaint);
	                canvas.restore();
					
	                // Push changes to device
					invalidate();
				}
			}
		}
	}
	
	/**
	 * Inner class used to make a fixed timed animation of the curl effect.
	 */
	class NewsTickerHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			synchronized (this) {
				if ( !bDetached ) {
					mFadeAnimationHandler.startFadeOut();
				}
			}
		}

		public void sleep(long millis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), millis);
		}
	}
	
	/**
	 * Handler used to fade out and in.
	 */
	class FadeAnimationHandler extends Handler {
		private static final int FADE_OUT_CODE = 0;
		private static final int FADE_IN_CODE = 1;
		
		@Override
		public void handleMessage(Message msg) {
			synchronized (this) {
				if ( !bDetached ) {
					int direction = (msg.what == FADE_IN_CODE)?1:-1;
					NewsTickerView.this.mFadeAlpha += (FADE_ANIMATION_RATE_SECS/(float)FADE_TIME)*direction;
					NewsTickerView.this.updateTextAlpha();
					
					if ( NewsTickerView.this.mFadeAlpha <= 0.f && (msg.what == FADE_OUT_CODE) ) {
						NewsTickerView.this.fadedOut();
					} else if ( NewsTickerView.this.mFadeAlpha >= 1.f && (msg.what == FADE_IN_CODE) ) {
						NewsTickerView.this.fadedIn();
					} else {
						sleep(msg.what);
					}
				}
			}
		}
		
		public void startFadeIn() {
			NewsTickerView.this.mFadeAlpha = 0;
			NewsTickerView.this.updateTextAlpha();
			startAnimation(FADE_IN_CODE);
		}
		
		public void startFadeOut() {
			NewsTickerView.this.mFadeAlpha = 1;
			NewsTickerView.this.updateTextAlpha();
			startAnimation(FADE_OUT_CODE);
		}
		
		/**
		 * Start the fade animation
		 */
		private void startAnimation(int code) {
			this.removeMessages(FADE_OUT_CODE);
			this.removeMessages(FADE_IN_CODE);
			sendMessage(obtainMessage(code));
		}
		
		/**
		 * Sleep and then start again
		 */
		public void sleep(int code) {
			this.removeMessages(FADE_OUT_CODE);
			this.removeMessages(FADE_IN_CODE);
			sendMessageDelayed(obtainMessage(code), FADE_ANIMATION_RATE);
		}
	}
	
	/**
	 * Saved state that holds all what the ticked needs between state changes
	 * @author Moss
	 *
	 */
	static class SavedState extends BaseSavedState {
		public int mNewsIndex;
		SavedState(Parcelable superState) {
			super(superState);
		}
		
		private SavedState(Parcel in) {
			super(in);
			this.mNewsIndex = in.readInt();
		}
		
		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(this.mNewsIndex);
		}
		
		// required field that makes Parcelables from a Parcel
		public static final Parcelable.Creator<SavedState> CREATOR =
			new Parcelable.Creator<SavedState>() {
				public SavedState createFromParcel(Parcel in) {
					return new SavedState(in);
				}
				public SavedState[] newArray(int size) {
					return new SavedState[size];
				}
		};
	}
}
