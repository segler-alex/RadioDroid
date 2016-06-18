package net.programmierecke.radiodroid2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.data.DataRecording;

public class ItemAdapterRecordings extends ArrayAdapter<DataRecording> {
	private Context context;
	private int resourceId;

	public ItemAdapterRecordings(Context context, int resourceId) {
		super(context, resourceId);
		this.resourceId = resourceId;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		DataRecording aData = getItem(position);

		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(resourceId, null);
		}

		TextView aTextViewTop = (TextView) v.findViewById(R.id.textViewTop);
		TextView aTextViewBottom = (TextView) v.findViewById(R.id.textViewBottom);
		if (aTextViewTop != null) {
			aTextViewTop.setText("" + aData.Name);
		}
		if (aTextViewBottom != null) {
			aTextViewBottom.setText("" + aData.Time);
		}

		return v;
	}
}
