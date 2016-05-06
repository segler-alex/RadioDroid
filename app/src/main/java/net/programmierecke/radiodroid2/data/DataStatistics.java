package net.programmierecke.radiodroid2.data;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataStatistics {
    public String Name = "";
    public String Value = "";

    public static DataStatistics[] DecodeJson(String result) {
        List<DataStatistics> aList = new ArrayList<DataStatistics>();
        if (result != null) {
            if (TextUtils.isGraphic(result)) {
                try {
                    JSONObject jsonObject = new JSONObject(result);

                    Iterator<?> keys = jsonObject.keys();
                    while( keys.hasNext() ) {
                        String key = (String)keys.next();

                        DataStatistics aData = new DataStatistics();
                        aData.Name = key;
                        aData.Value = jsonObject.getString(key);

                        aList.add(aData);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        return aList.toArray(new DataStatistics[0]);
    }
}
