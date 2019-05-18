package net.programmierecke.radiodroid2.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

/**
 * Android doesn't provide a way to have integer preferences. This is a quick hack to have them.
 * User can enter anything in the text edit but only valid integer will be saved.
 */
public class IntEditTextPreference extends EditTextPreference {
    private int value = 0;
    private String summaryFormat;

    public IntEditTextPreference(Context context) {
        super(context);
    }

    public IntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        summaryFormat = getSummary().toString();
    }

    public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        summaryFormat = getSummary().toString();
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        if (defaultValue == null) {
            value = getPersistedInt(0);
        } else {
            Integer defaultInt = parseInteger((String) defaultValue);
            value = defaultInt != null ? defaultInt : 0;
        }

        if (summaryFormat != null) {
            setSummary(String.format(summaryFormat, value));
        }
    }

    @Override
    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();
        Integer currentValue = parseInteger(text);
        if (currentValue != null) {
            value = currentValue;
            persistInt(value);

            if (summaryFormat != null) {
                setSummary(String.format(summaryFormat, value));
            }
        }

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    @Override
    public String getText() {
        return Integer.toString(value);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(value));
    }

    private static Integer parseInteger(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
