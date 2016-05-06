package net.programmierecke.radiodroid2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.programmierecke.radiodroid2.data.DataStatistics;
import net.programmierecke.radiodroid2.R;

public class ItemAdapterStatistics extends ArrayAdapter<DataStatistics> {
	private Context context;
	private int resourceId;

	public ItemAdapterStatistics(Context context, int resourceId) {
		super(context, resourceId);
		this.resourceId = resourceId;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		DataStatistics aData = getItem(position);

		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(resourceId, null);
		}

		TextView aTextViewTop = (TextView) v.findViewById(R.id.stats_name);
		TextView aTextViewBottom = (TextView) v.findViewById(R.id.stats_value);
		if (aTextViewTop != null) {
			aTextViewTop.setText("" + aData.Name);
		}
		if (aTextViewBottom != null) {
			aTextViewBottom.setText("" + aData.Value);
		}

		return v;
	}
}
