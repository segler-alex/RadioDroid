package net.programmierecke.radiodroid2;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TimePicker;

import net.programmierecke.radiodroid2.adapters.ItemAdapterRadioAlarm;
import net.programmierecke.radiodroid2.data.DataRadioStationAlarm;
import net.programmierecke.radiodroid2.interfaces.IChanged;

public class FragmentAlarm extends Fragment implements TimePickerDialog.OnTimeSetListener, IChanged {
    private ListView lv;
    private RadioAlarmManager ram;
    private ItemAdapterRadioAlarm adapterRadioAlarm;

    public FragmentAlarm() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ram = new RadioAlarmManager(getActivity().getApplicationContext(),this);
        View view = inflater.inflate(R.layout.layout_alarms, container, false);

        adapterRadioAlarm = new ItemAdapterRadioAlarm(getActivity());
        lv = (ListView)view.findViewById(R.id.listViewAlarms);
        lv.setAdapter(adapterRadioAlarm);
        lv.setClickable(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object anObject = parent.getItemAtPosition(position);
                if (anObject instanceof DataRadioStationAlarm) {
                    ClickOnItem((DataRadioStationAlarm) anObject);
                }
            }
        });

        RefreshList();

        view.invalidate();

        return view;
    }

    private void RefreshList() {
        adapterRadioAlarm.clear();
        for(DataRadioStationAlarm alarm: ram.getList()){
            adapterRadioAlarm.add(alarm);
        }
    }

    DataRadioStationAlarm clickedAlarm = null;
    private void ClickOnItem(DataRadioStationAlarm anObject) {
        clickedAlarm = anObject;
        TimePickerFragment newFragment = new TimePickerFragment();
        newFragment.setCallback(this);
        newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        ram.changeTime(clickedAlarm.id,hourOfDay,minute);
        RefreshList();
        view.invalidate();
    }

    @Override
    public void onChanged() {
        ram.load();
        RefreshList();
    }
}