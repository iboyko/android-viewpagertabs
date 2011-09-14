package com.astuetz.viewpagertabs;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class ViewPagerTabs extends RelativeLayout implements OnPageChangeListener {
	
	@SuppressWarnings("unused")
	private static final String TAG = "ViewPagerTabs";
	
	
	// Scrolling direction
	private enum Direction {
		Left, Right, Center
	}
	
	// Some values for creating shadows to the left and right of the screen
	private static final int SHADOW_WIDTH = 40;
	private static final int SHADOW_DARK_ALPHA = 0xDD;
	
	// Context.
	private Context mContext;
	
	// The referenced {@link ViewPager}
	private ViewPager mPager;
	
	// The current position
	private int mPosition = 0;
	
	// The offset at which tabs are going to
	// be moved, if they are outside the screen
	private int mOutsideOffset = -1;
	
	// Used for initial positioning of the tabs
	private boolean isFirstMeasurement = true;
	
	
	// These values will be passed to every child ({@link ViewPagerTab})
	
	private int mBackgroundColor = 0xFF3B3B3B;
	private int mBackgroundColorPressed = 0x9943797F;
	
	private int mTextColor = 0xFF999999;
	private int mTextColorCenter = 0xFF91A438;
	
	private int mLineColor = 0xFF91A438;
	private int mLineHeight = 3;
	
	
	// These values are needed here
	
	private int mTabPaddingLeft = 25;
	private int mTabPaddingTop = 5;
	private int mTabPaddingRight = 25;
	private int mTabPaddingBottom = 10;
	
	private float mTextSize = 14;
	
	
	
	public ViewPagerTabs(Context context) {
		this(context, null);
	}
	
	public ViewPagerTabs(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public ViewPagerTabs(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		mContext = context;
		
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerTabs, defStyle, 0);
		
		mBackgroundColor = a.getColor(R.styleable.ViewPagerTabs_backgroundColor, mBackgroundColor);
		mBackgroundColorPressed = a.getColor(R.styleable.ViewPagerTabs_backgroundColorPressed, mBackgroundColorPressed);
		
		mTextColor = a.getColor(R.styleable.ViewPagerTabs_textColor, mTextColor);
		mTextColorCenter = a.getColor(R.styleable.ViewPagerTabs_textColorCenter, mTextColorCenter);
		
		mLineColor = a.getColor(R.styleable.ViewPagerTabs_lineColor, mLineColor);
		mLineHeight = a.getDimensionPixelSize(R.styleable.ViewPagerTabs_lineHeight, mLineHeight);
		
		mTextSize = a.getDimension(R.styleable.ViewPagerTabs_textSize, mTextSize);
		
		mTabPaddingLeft = a.getDimensionPixelSize(R.styleable.ViewPagerTabs_tabPaddingLeft, mTabPaddingLeft);
		mTabPaddingTop = a.getDimensionPixelSize(R.styleable.ViewPagerTabs_tabPaddingTop, mTabPaddingTop);
		mTabPaddingRight = a.getDimensionPixelSize(R.styleable.ViewPagerTabs_tabPaddingRight, mTabPaddingRight);
		mTabPaddingBottom = a.getDimensionPixelSize(R.styleable.ViewPagerTabs_tabPaddingBottom, mTabPaddingBottom);
		
		mOutsideOffset = a.getDimensionPixelSize(R.styleable.ViewPagerTabs_outsideOffset, mOutsideOffset);
		
		a.recycle();
		
		this.setBackgroundColor(mBackgroundColor);
	}
	
	
	/**
	 * Binds the {@link ViewPager} to this instance
	 * 
	 * @param pager
	 *          An instance of {@link ViewPager}
	 */
	public void setViewPager(ViewPager pager) {
		
		if (!(pager.getAdapter() instanceof ViewPagerTabProvider)) {
			throw new IllegalStateException("The pager's adapter has to implement ViewPagerTabProvider.");
		}
		
		this.mPager = pager;
		mPager.setCurrentItem(this.mPosition);
		mPager.setOnPageChangeListener(this);
		
		for (int i = 0; i < pager.getAdapter().getCount(); i++) {
			addTab(i, ((ViewPagerTabProvider) pager.getAdapter()).getTitle(i));
		}
		
		applyStyles();
		
		calculateNewPositions(true);
		this.requestLayout();
	}
	
	/**
	 * Binds the {@link ViewPager} to this instance and sets the current position
	 * 
	 * @param pager
	 *          An instance of {@link ViewPager}
	 * @param position
	 *          Initial position of the {@link ViewPager}
	 */
	public void setViewPager(ViewPager pager, int position) {
		this.mPosition = position;
		setViewPager(pager);
	}
	
	
	/**
	 * Adds a new {@link ViewPagerTab} to the layout
	 * 
	 * @param index
	 *          The index from the Pagers adapter
	 * @param title
	 *          The title which should be used
	 */
	public void addTab(int index, String title) {
		ViewPagerTab tab = new ViewPagerTab(mContext);
		tab.setText(title);
		tab.setIndex(index);
		addView(tab);
		
		tab.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mPager.setCurrentItem(((ViewPagerTab) v).getIndex());
			}
		});
	}
	
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		final View c = getChildAt(0);
		final TextView tab = (TextView) c;
		tab.measure(0, 0);
		
		setMeasuredDimension(resolveSize(0, widthMeasureSpec),
		    resolveSize(tab.getMeasuredHeight() + this.getPaddingTop() + this.getPaddingBottom(), heightMeasureSpec));
		
		// first time measuring, set outside offset (if not set manually),
		// measure children and calculate initial positions
		if (isFirstMeasurement) {
			isFirstMeasurement = false;
			if (mOutsideOffset < 0) mOutsideOffset = getMeasuredWidth();
			
			measureChildren();
			
			calculateNewPositions(true);
		}
		
	}
	
	
	private void measureChildren() {
		final int count = this.getChildCount();
		
		for (int i = 0; i < count; i++) {
			LayoutParams layoutParams = (LayoutParams) getChildAt(i).getLayoutParams();
			final int widthSpec = MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY);
			final int heightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY);
			getChildAt(i).measure(widthSpec, heightSpec);
		}
	}
	
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
		final int count = getChildCount();
		
		final int center = this.getMeasuredWidth() / 2;
		
		// At this position, the centered tab will be highlighted via
		// ViewPagerTab.setCenterPercent(int percent)
		final int highlightOffset = this.getMeasuredWidth() / 5;
		
		// lay out each tab
		for (int i = 0; i < count; i++) {
			
			final ViewPagerTab tab = (ViewPagerTab) getChildAt(i);
			
			final int tabCenter = tab.layoutPos + tab.getMeasuredWidth() / 2;
			int diff = Math.abs(center - tabCenter);
			
			if (diff <= highlightOffset) {
				final int x0 = 0;
				final int x1 = highlightOffset;
				final int y0 = 0;
				final int y1 = 100;
				final int y = (int) (y0 + (y1 * (diff - x0) - y0 * (diff - x0)) / (x1 - x0));
				tab.setCenterPercent(100 - y);
			} else {
				tab.setCenterPercent(0);
			}
			
			tab.layout(tab.layoutPos, this.getPaddingTop(), tab.layoutPos + tab.getMeasuredWidth(), this.getPaddingTop()
			    + tab.getMeasuredHeight());
			
		}
		
	}
	
	
	/**
	 * This method calculates the previous, current and next position for each tab
	 * 
	 * @param firstTime
	 *          If true, all tabs will be aligned at their initial position
	 */
	private void calculateNewPositions(boolean firstTime) {
		
		final int pos = mPosition;
		final int count = getChildCount();
		
		
		// left outside, will not come to screen
		for (int i = 0; i < pos - 2; i++) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(i);
			
			t.currentPos = leftOutside(t);
			t.prevPos = t.currentPos;
			t.nextPos = t.currentPos;
		}
		
		// left outside, may come to screen
		if (pos > 1) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(pos - 2);
			
			t.currentPos = leftOutside(t);
			t.prevPos = t.currentPos;
			t.nextPos = left(t);
		}
		
		// left
		if (pos > 0) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(pos - 1);
			
			t.currentPos = left(t);
			t.prevPos = leftOutside(t);
			t.nextPos = center(t);
		}
		
		// center
		if (count > 0) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(pos);
			
			t.currentPos = center(t);
			t.prevPos = left(t);
			t.nextPos = right(t);
		}
		
		// right
		if (pos < count - 1) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(pos + 1);
			
			t.currentPos = right(t);
			t.prevPos = center(t);
			t.nextPos = rightOutside(t);
		}
		
		// right outside, may come to screen
		if (pos < count - 2) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(pos + 2);
			
			t.currentPos = rightOutside(t);
			t.prevPos = right(t);
			t.nextPos = t.currentPos;
		}
		
		// right outside, will not come to screen
		for (int i = pos + 3; i < count; i++) {
			ViewPagerTab t = (ViewPagerTab) getChildAt(i);
			
			t.currentPos = rightOutside(t);
			t.prevPos = t.currentPos;
			t.nextPos = t.currentPos;
		}
		
		
		// Prevent tabs from overlapping
		// TODO: make this better?
		{
			ViewPagerTab leftOutside = pos > 1 ? (ViewPagerTab) getChildAt(pos - 2) : null;
			ViewPagerTab left = pos > 0 ? (ViewPagerTab) getChildAt(pos - 1) : null;
			ViewPagerTab center = (ViewPagerTab) getChildAt(pos);
			ViewPagerTab right = pos < getChildCount() - 1 ? (ViewPagerTab) getChildAt(pos + 1) : null;
			ViewPagerTab rightOutside = pos < getChildCount() - 2 ? (ViewPagerTab) getChildAt(pos + 2) : null;
			
			if (leftOutside != null) {
				if (leftOutside.nextPos + leftOutside.getMeasuredWidth() >= left.nextPos) {
					leftOutside.nextPos = left.nextPos - leftOutside.getMeasuredWidth();
				}
			}
			
			if (left != null) {
				if (left.currentPos + left.getMeasuredWidth() >= center.currentPos) {
					left.currentPos = center.currentPos - left.getMeasuredWidth();
				}
				if (center.nextPos <= left.nextPos + left.getMeasuredWidth()) {
					center.nextPos = left.nextPos + left.getMeasuredWidth();
				}
			}
			
			if (right != null) {
				if (center.prevPos + center.getMeasuredWidth() >= right.prevPos) {
					center.prevPos = right.prevPos - center.getMeasuredWidth();
				}
				if (right.currentPos <= center.currentPos + center.getMeasuredWidth()) {
					right.currentPos = center.currentPos + center.getMeasuredWidth();
				}
			}
			
			if (rightOutside != null) {
				if (rightOutside.prevPos <= right.prevPos + right.getMeasuredWidth()) {
					rightOutside.prevPos = right.prevPos + right.getMeasuredWidth();
				}
			}
		}
		
		
		// Set initial positions for each tab
		if (firstTime) {
			for (int i = 0; i < count; i++) {
				ViewPagerTab t = (ViewPagerTab) getChildAt(i);
				t.layoutPos = t.currentPos;
			}
		}
		
		
	}
	
	
	private int leftOutside(ViewPagerTab tab) {
		final int width = tab.getMeasuredWidth();
		return width * (-1) - mOutsideOffset;
	}
	
	private int left(ViewPagerTab tab) {
		return 0 - tab.getPaddingLeft();
	}
	
	private int center(ViewPagerTab tab) {
		final int width = tab.getMeasuredWidth();
		return this.getMeasuredWidth() / 2 - width / 2;
	}
	
	private int right(ViewPagerTab tab) {
		final int width = tab.getMeasuredWidth();
		return this.getMeasuredWidth() - width + tab.getPaddingRight();
	}
	
	private int rightOutside(ViewPagerTab tab) {
		return this.getMeasuredWidth() + mOutsideOffset;
	}
	
	
	
	@Override
	public void onPageScrollStateChanged(int state) {
	}
	
	/**
	 * At this point the scrolling direction is determined and every child is
	 * interpolated to its previous or next position
	 */
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		
		final int count = getChildCount();
		
		final int currentScrollX = mPosition * mPager.getWidth();
		
		Direction dir = Direction.Center;
		
		float x = 0.0f;
		
		if (mPager.getScrollX() < currentScrollX) {
			dir = Direction.Left;
			x = 1 - positionOffset;
		} else if (mPager.getScrollX() > currentScrollX) {
			dir = Direction.Right;
			x = positionOffset;
		}
		
		for (int i = 0; i < count; i++) {
			ViewPagerTab tab = (ViewPagerTab) getChildAt(i);
			
			final float x0 = 0.0f;
			final float x1 = 1.0f;
			
			final float y0 = tab.currentPos;
			float y1 = 0.0f;
			
			if (dir == Direction.Left)
				y1 = tab.nextPos;
			else if (dir == Direction.Right)
				y1 = tab.prevPos;
			else
				y1 = tab.currentPos;
			
			final int y = (int) (y0 + (y1 * (x - x0) - y0 * (x - x0)) / (x1 - x0));
			
			tab.layoutPos = y;
		}
		
		this.requestLayout();
	}
	
	@Override
	public void onPageSelected(int position) {
		mPosition = position;
		calculateNewPositions(false);
		this.requestLayout();
	}
	
	
	
	private Paint mShadowLeftPaint = new Paint();
	private Paint mShadowRightPaint = new Paint();
	
	/**
	 * Here the shadows ("translucent fading close to the edges") are drawn above
	 * the child views
	 */
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		
		int shadowDark = Color.argb(SHADOW_DARK_ALPHA, Color.red(mBackgroundColor), Color.green(mBackgroundColor),
		    Color.blue(mBackgroundColor));
		int shadowLight = Color.argb(0x00, Color.red(mBackgroundColor), Color.green(mBackgroundColor),
		    Color.blue(mBackgroundColor));
		
		
		final Shader shadowLeftShader = new LinearGradient(0, 0, SHADOW_WIDTH, 0, shadowDark, shadowLight,
		    Shader.TileMode.MIRROR);
		final Shader shadowRightShader = new LinearGradient(this.getWidth() - SHADOW_WIDTH, 0, this.getWidth(), 0,
		    shadowLight, shadowDark,
		    Shader.TileMode.MIRROR);
		
		final Paint shadowLeftPaint = mShadowLeftPaint;
		shadowLeftPaint.setShader(shadowLeftShader);
		shadowLeftPaint.setAntiAlias(true);
		
		final Paint shadowRightPaint = mShadowRightPaint;
		shadowRightPaint.setShader(shadowRightShader);
		shadowRightPaint.setAntiAlias(true);
		
		
		canvas.drawRect(0, 0, SHADOW_WIDTH, this.getHeight(), shadowLeftPaint);
		canvas.drawRect(this.getWidth() - SHADOW_WIDTH, 0, this.getWidth(), this.getHeight(), shadowRightPaint);
	}
	
	
	
	/*
	 * Public property access
	 */
	
	public void setTabPaddingLeft(int padding) {
		this.mTabPaddingLeft = padding;
		applyStyles();
	}
	
	public void setTabPaddingTop(int padding) {
		this.mTabPaddingTop = padding;
		applyStyles();
	}
	
	public void setTabPaddingRight(int padding) {
		this.mTabPaddingRight = padding;
		applyStyles();
	}
	
	public void setTabPaddingBottom(int padding) {
		this.mTabPaddingBottom = padding;
		applyStyles();
	}
	
	public void setTabPadding(int left, int top, int right, int bottom) {
		this.mTabPaddingLeft = left;
		this.mTabPaddingTop = top;
		this.mTabPaddingRight = right;
		this.mTabPaddingBottom = bottom;
		applyStyles();
	}
	
	public void setBackgroundColor(int color) {
		this.mBackgroundColor = color;
		applyStyles();
	}
	
	public void setBackgroundColorPressed(int color) {
		this.mBackgroundColorPressed = color;
		applyStyles();
	}
	
	public void setTextSize(float size) {
		this.mTextSize = size;
		applyStyles();
	}
	
	public void setTextColor(int color) {
		this.mTextColor = color;
		applyStyles();
	}
	
	public void setTextColorCenter(int color) {
		this.mTextColorCenter = color;
		applyStyles();
	}
	
	public void setLineColor(int color) {
		this.mLineColor = color;
		applyStyles();
	}
	
	public void setLineHeight(int height) {
		this.mLineHeight = height;
		applyStyles();
	}
	
	public void setOutsideOffset(int offset) {
		this.mOutsideOffset = offset;
		applyStyles();
	}
	
	
	/**
	 * 
	 */
	private void applyStyles() {
		
		final int count = getChildCount();
		
		for (int i = 0; i < count; i++) {
			
			ViewPagerTab tab = (ViewPagerTab) getChildAt(i);
			
			tab.setPadding(mTabPaddingLeft, mTabPaddingTop, mTabPaddingRight, mLineHeight + mTabPaddingBottom - 4);
			tab.setTextColors(mTextColor, mTextColorCenter);
			tab.setLineColors(mBackgroundColor, mLineColor);
			tab.setLineHeight(mLineHeight);
			tab.setBackgroundColorPressed(mBackgroundColorPressed);
			tab.setTextSize(mTextSize);
			
		}
		
		super.setBackgroundColor(mBackgroundColor);
		
		measureChildren();
		
		calculateNewPositions(true);
		this.requestLayout();
		
	}
	
	
	/**
	 * Call this method if the titles in the pager's adapter were changed
	 */
	public void refreshTitles() {
		final int count = getChildCount();
		
		for (int i = 0; i < count; i++) {
			ViewPagerTab tab = (ViewPagerTab) getChildAt(i);
			tab.setText(((ViewPagerTabProvider) mPager.getAdapter()).getTitle(i));
		}
		
		measureChildren();
		
		calculateNewPositions(true);
		this.requestLayout();
	}
	
	
	
	/*
	 * state handling
	 */
	
	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState(savedState.getSuperState());
		mPosition = savedState.position;
		mBackgroundColor = savedState.backgroundColor;
		mBackgroundColorPressed = savedState.backgroundColorPressed;
		mTextColor = savedState.textColor;
		mTextColorCenter = savedState.textColorCenter;
		mLineColor = savedState.lineColor;
		mLineHeight = savedState.lineHeight;
		mTabPaddingLeft = savedState.tabPaddingLeft;
		mTabPaddingTop = savedState.tabPaddingTop;
		mTabPaddingRight = savedState.tabPaddingRight;
		mTabPaddingBottom = savedState.tabPaddingBottom;
		mTextSize = savedState.textSize;
		applyStyles();
	}
	
	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.position = mPosition;
		savedState.backgroundColor = mBackgroundColor;
		savedState.backgroundColorPressed = mBackgroundColorPressed;
		savedState.textColor = mTextColor;
		savedState.textColorCenter = mTextColorCenter;
		savedState.lineColor = mLineColor;
		savedState.lineHeight = mLineHeight;
		savedState.tabPaddingLeft = mTabPaddingLeft;
		savedState.tabPaddingTop = mTabPaddingTop;
		savedState.tabPaddingRight = mTabPaddingRight;
		savedState.tabPaddingBottom = mTabPaddingBottom;
		savedState.textSize = mTextSize;
		return savedState;
	}
	
	
	/**
	 * This holds our state
	 *
	 */
	static class SavedState extends BaseSavedState {
		int position;
		int backgroundColor;
		int backgroundColorPressed;
		int textColor;
		int textColorCenter;
		int lineColor;
		int lineHeight;
		int tabPaddingLeft;
		int tabPaddingTop;
		int tabPaddingRight;
		int tabPaddingBottom;
		float textSize;
		
		public SavedState(Parcelable superState) {
			super(superState);
		}
		
		private SavedState(Parcel in) {
			super(in);
			position = in.readInt();
			backgroundColor = in.readInt();
			backgroundColorPressed = in.readInt();
			textColor = in.readInt();
			textColorCenter = in.readInt();
			lineColor = in.readInt();
			lineHeight = in.readInt();
			tabPaddingLeft = in.readInt();
			tabPaddingTop = in.readInt();
			tabPaddingRight = in.readInt();
			tabPaddingBottom = in.readInt();
			textSize = in.readFloat();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(position);
			dest.writeInt(backgroundColor);
			dest.writeInt(backgroundColorPressed);
			dest.writeInt(textColor);
			dest.writeInt(textColorCenter);
			dest.writeInt(lineColor);
			dest.writeInt(lineHeight);
			dest.writeInt(tabPaddingLeft);
			dest.writeInt(tabPaddingTop);
			dest.writeInt(tabPaddingRight);
			dest.writeInt(tabPaddingBottom);
			dest.writeFloat(textSize);
		}
		
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}
			
			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
	
}
