package net.programmierecke.radiodroid2.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import net.programmierecke.radiodroid2.R;

import java.util.List;

public class TagsView extends AppCompatTextView {
    public interface TagSelectionCallback {
        void onTagSelected(String tag);
    }

    // Credits to Nachos source
    private class RoundedBackgroundSpan extends ReplacementSpan {
        private int mHeight;
        private int mCornerRadius;
        private int mTextHorizontalPadding;
        private int mTextVerticalMargin;
        private int mBackgroundColor;
        private int mTextColor;

        RoundedBackgroundSpan(int mHeight, int mCornerRadius, int mTextHorizontalPadding,
                              int mTextVerticalMargin, int mBackgroundColor, int mTextColor) {
            super();
            this.mHeight = mHeight;
            this.mCornerRadius = mCornerRadius;
            this.mTextHorizontalPadding = mTextHorizontalPadding;
            this.mTextVerticalMargin = mTextVerticalMargin;
            this.mBackgroundColor = mBackgroundColor;
            this.mTextColor = mTextColor;
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            top += ((bottom - top) / 2) - (mHeight / 2);
            bottom = top + mHeight;

            Paint.FontMetrics fm = paint.getFontMetrics();

            float adjustedY = top + ((mHeight / 2) + ((-fm.top - fm.bottom) / 2));

            RectF rect = new RectF(x, top, x + measureText(paint, text, start, end) + 2 * mTextHorizontalPadding, bottom);
            paint.setColor(mBackgroundColor);
            canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, paint);
            paint.setColor(mTextColor);
            canvas.drawText(text, start, end, x + mTextHorizontalPadding, adjustedY, paint);
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm != null) {
                paint.getFontMetricsInt(fm);
                int textHeight = fm.descent - fm.ascent;

                int spaceBetweenTopAndText = (mHeight - textHeight) / 2;

                int textTop = fm.top;
                int bkgTop = fm.top - spaceBetweenTopAndText;

                int textBottom = fm.bottom;
                int bkgBottom = fm.bottom + spaceBetweenTopAndText;

                // Text may be bigger than given height
                int topOfContent = Math.min(textTop, bkgTop);
                int bottomOfContent = Math.max(textBottom, bkgBottom);

                int topOfContentWithPadding = topOfContent - mTextVerticalMargin;
                int bottomOfContentWithPadding = bottomOfContent + mTextVerticalMargin;

                fm.ascent = topOfContentWithPadding;
                fm.descent = bottomOfContentWithPadding;
                fm.top = topOfContentWithPadding;
                fm.bottom = bottomOfContentWithPadding;
            }

            return Math.round(paint.measureText(text, start, end)) + mTextHorizontalPadding * 2;
        }

        private float measureText(Paint paint, CharSequence text, int start, int end) {
            return paint.measureText(text, start, end);
        }
    }

    private int mTagBackgroundColor = Color.RED;
    private int mCornerRadius = 16;
    private int mTagHeight = 20;
    private int mTextHorizontalPadding = 8;
    private int mTextVerticalMargin = 4;
    private TagSelectionCallback mTagSelectionCallback;


    public TagsView(Context context) {
        super(context);
        init(null, 0);
    }

    public TagsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TagsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.TagsView, defStyle, 0);

        mTagBackgroundColor = a.getColor(R.styleable.TagsView_tagBackgroundColor, mTagBackgroundColor);
        mCornerRadius = a.getDimensionPixelSize(R.styleable.TagsView_cornerRadius, mCornerRadius);
        mTagHeight = a.getDimensionPixelSize(R.styleable.TagsView_tagHeight, mTagHeight);
        mTextHorizontalPadding = a.getDimensionPixelSize(R.styleable.TagsView_textHorizontalPadding, mTextHorizontalPadding);
        mTextVerticalMargin = a.getDimensionPixelSize(R.styleable.TagsView_textVerticalMargin, mTextVerticalMargin);

        a.recycle();
    }

    public void setTags(List<String> tags) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

        final String spacing = "  ";
        for (final String tag : tags) {
            String tagWithBufferSpace = tag + spacing;
            stringBuilder.append(tagWithBufferSpace);

            RoundedBackgroundSpan span = new RoundedBackgroundSpan(mTagHeight, mCornerRadius,
                    mTextHorizontalPadding, mTextVerticalMargin, mTagBackgroundColor, getCurrentTextColor());
            final int start = stringBuilder.length() - tagWithBufferSpace.length();
            final int end = stringBuilder.length() - spacing.length();
            stringBuilder.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    if (mTagSelectionCallback != null) {
                        mTagSelectionCallback.onTagSelected(tag);
                    }
                }
            };
            stringBuilder.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        setText(stringBuilder);
        setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setTagSelectionCallback(TagSelectionCallback tagSelectionCallback) {
        mTagSelectionCallback = tagSelectionCallback;
    }
}
