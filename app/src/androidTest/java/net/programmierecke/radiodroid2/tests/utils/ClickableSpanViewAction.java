package net.programmierecke.radiodroid2.tests.utils;

import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.instanceOf;

public class ClickableSpanViewAction implements ViewAction {
    private int spanIndex = -1;
    private CharSequence textToClick = null;

    public static ViewAction clickClickableSpan(final CharSequence textToClick) {
        return new ClickableSpanViewAction(textToClick);
    }

    public static ViewAction clickClickableSpan(int spanIndex) {
        return new ClickableSpanViewAction(spanIndex);
    }

    public ClickableSpanViewAction(int spanIndex) {
        this.spanIndex = spanIndex;
    }

    public ClickableSpanViewAction(CharSequence textToClick) {
        this.textToClick = textToClick;
    }

    @Override
    public Matcher<View> getConstraints() {
        return instanceOf(TextView.class);
    }

    @Override
    public String getDescription() {
        return "clicking on a ClickableSpan";
    }

    @Override
    public void perform(UiController uiController, View view) {
        TextView textView = (TextView) view;
        Spannable spannableString = (Spannable) textView.getText();
        ClickableSpan spanToLocate;
        if (spannableString.length() == 0) {
            throw new NoMatchingViewException.Builder()
                    .includeViewHierarchy(true)
                    .withRootView(textView)
                    .build();
        }

        ClickableSpan[] spans = spannableString.getSpans(0, spannableString.length(), ClickableSpan.class);

        if (spans.length > 0) {
            if (spanIndex >= spans.length) {
                throw new NoMatchingViewException.Builder()
                        .includeViewHierarchy(true)
                        .withRootView(textView)
                        .build();
            } else if (spanIndex >= 0) {
                spanToLocate = spans[spanIndex];
                spanToLocate.onClick(textView);
                return;
            }

            for (ClickableSpan span : spans) {
                int start = spannableString.getSpanStart(span);
                int end = spannableString.getSpanEnd(span);
                CharSequence sequence = spannableString.subSequence(start, end);
                if (textToClick.toString().equals(sequence.toString())) {
                    spanToLocate = span;
                    spanToLocate.onClick(textView);
                    return;
                }
            }
        }

        throw new NoMatchingViewException.Builder()
                .includeViewHierarchy(true)
                .withRootView(textView)
                .build();

    }

}
