package com.programmierecke.radiodroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RadioItemBigAdapter extends ArrayAdapter<RadioStation> {

	public RadioItemBigAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);

		itsContext = context;
	}

	Context itsContext;

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) itsContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item_big, null);
		}
		RadioStation aStation = getItem(position);
		if (aStation != null) {
			TextView tt = (TextView) v.findViewById(R.id.textViewLeft);
			TextView bt = (TextView) v.findViewById(R.id.textViewRight);
			if (tt != null) {
				tt.setText("" + aStation.Name);
			}
			if (bt != null) {
				bt.setText("" + aStation.Votes);
			}
		}
		return v;
	}
}
