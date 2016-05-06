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
import android.widget.TextView;

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
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item_alarm, null);
		}

		TextView tvStation = (TextView) v.findViewById(R.id.textViewStation);
		TextView tvTime = (TextView) v.findViewById(R.id.textViewTime);
		SwitchCompat s = (SwitchCompat)v.findViewById(R.id.switch1);
		ImageButton b = (ImageButton) v.findViewById(R.id.buttonDelete);

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
					Log.w("ALARM","new state:"+isChecked);
					RadioAlarmManager ram = new RadioAlarmManager(getContext().getApplicationContext(),null);
					ram.setEnabled(aData.id, isChecked);
				}
			});
		}

		return v;
	}
}
