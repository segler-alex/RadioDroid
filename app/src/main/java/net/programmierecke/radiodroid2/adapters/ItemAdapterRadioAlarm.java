package net.programmierecke.radiodroid2.adapters;

import android.content.Context;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.programmierecke.radiodroid2.BuildConfig;
import net.programmierecke.radiodroid2.data.DataRadioStationAlarm;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioAlarmManager;

import java.util.Locale;

public class ItemAdapterRadioAlarm extends ArrayAdapter<DataRadioStationAlarm> {
	private Context context;

	public ItemAdapterRadioAlarm(Context context) {
		super(context, R.layout.list_item_alarm);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final DataRadioStationAlarm aData = getItem(position);

		View v = convertView;
		LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (v == null) {
			v = vi.inflate(R.layout.list_item_alarm, null);
		}

		TextView tvStation = (TextView) v.findViewById(R.id.textViewStation);
		TextView tvTime = (TextView) v.findViewById(R.id.textViewTime);
		SwitchCompat s = (SwitchCompat)v.findViewById(R.id.switch1);
		ImageButton b = (ImageButton) v.findViewById(R.id.buttonDeleteAlarm);
		final ImageButton buttonRepeating = (ImageButton) v.findViewById(R.id.checkboxRepeating);
		final LinearLayout repeatDaysView = (LinearLayout) v.findViewById(R.id.repeatDaysView);

		if (repeatDaysView.getChildCount() < 1) {
			populateWeekDayButtons(aData, vi, repeatDaysView);
		}

		buttonRepeating.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				aData.repeating = !aData.repeating;
				RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(),null);
				ram.toggleRepeating(aData.id);
				repeatDaysView.setVisibility(aData.repeating ? View.VISIBLE : View.GONE);
			}
		});

		if (b != null){
			b.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(),null);
					ram.remove(aData.id);
				}
			});
		}
		if (tvStation != null) {
			tvStation.setText(aData.station.Name);
		}
		if (tvTime != null) {
			tvTime.setText(String.format(Locale.getDefault(),"%02d:%02d",aData.hour,aData.minute));
		}
		if (s != null){
			s.setChecked(aData.enabled);
			s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if(BuildConfig.DEBUG) { Log.d("ALARM","new state:"+isChecked); }
					RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(),null);
					ram.setEnabled(aData.id, isChecked);
				}
			});
		}
		repeatDaysView.setVisibility(aData.repeating ? View.VISIBLE : View.GONE);
		buttonRepeating.setContentDescription(this.context.getResources().getString(aData.repeating ? R.string.image_button_dont_repeat : R.string.image_button_repeat));
		return v;
	}

	private void populateWeekDayButtons(final DataRadioStationAlarm aData, LayoutInflater vi, LinearLayout repeatDays) {
		String[] mShortWeekDayStrings = this.context.getResources().getStringArray(R.array.weekdays);
		for (int i = 0; i < 7; i++) {
			final ViewGroup viewgroup = (ViewGroup) vi.inflate(R.layout.day_button,
					repeatDays, false);
			final ToggleButton button = (ToggleButton) viewgroup.getChildAt(0);

			repeatDays.addView(viewgroup);

			button.setId(i);
			button.setText(mShortWeekDayStrings[i]);
			button.setTextOn(mShortWeekDayStrings[i]);
			button.setTextOff(mShortWeekDayStrings[i]);
			button.setContentDescription(mShortWeekDayStrings[i]);
			if (aData.weekDays.contains(i)) {
				button.setChecked(true);
			}
			button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					int bid = button.getId();
					RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(),null);
					ram.changeWeekDays(aData.id, bid);
				}
			});
		}
	}
}
