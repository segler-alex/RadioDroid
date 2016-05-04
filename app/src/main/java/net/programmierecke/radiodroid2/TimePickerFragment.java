package net.programmierecke.radiodroid2;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    TimePickerDialog.OnTimeSetListener callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    public void setCallback(TimePickerDialog.OnTimeSetListener callback) {
        this.callback = callback;
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // this is needed, because on some devices onTimeSet is called twice!!
        if (callback != null){
            callback.onTimeSet(view,hourOfDay,minute);
            callback = null;
        }
    }
}
