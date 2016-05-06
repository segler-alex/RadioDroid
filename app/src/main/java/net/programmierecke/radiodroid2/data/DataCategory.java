package net.programmierecke.radiodroid2.data;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataCategory {
    public String Name = "";
    public int UsedCount = 0;

    public static DataCategory[] DecodeJson(String result) {
        List<DataCategory> aList = new ArrayList<DataCategory>();
        if (result != null) {
            if (TextUtils.isGraphic(result)) {
                try {
                    JSONArray jsonArray = new JSONArray(result);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject anObject = jsonArray.getJSONObject(i);

                        DataCategory aData = new DataCategory();
                        aData.Name = anObject.getString("name");
                        aData.UsedCount = anObject.getInt("stationcount");

                        aList.add(aData);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        return aList.toArray(new DataCategory[0]);
    }
}
