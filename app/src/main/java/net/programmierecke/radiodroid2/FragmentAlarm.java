package net.programmierecke.radiodroid2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class FragmentAlarm extends Fragment {
    private ListView lv;
    private RadioAlarmManager ram;
    private ItemAdapterRadioAlarm adapterRadioAlarm;

    public FragmentAlarm() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ram = new RadioAlarmManager(getActivity().getApplicationContext());
        View view = inflater.inflate(R.layout.layout_alarms, container, false);

        adapterRadioAlarm = new ItemAdapterRadioAlarm(getActivity());
        lv = (ListView)view.findViewById(R.id.listViewAlarms);
        lv.setAdapter(adapterRadioAlarm);

        adapterRadioAlarm.clear();
        for(DataRadioStationAlarm alarm: ram.getList()){
            adapterRadioAlarm.add(alarm);
        }

        view.invalidate();

        return view;
    }
}