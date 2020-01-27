package net.programmierecke.radiodroid2.data;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataCategory implements Comparable<DataCategory>{
    public String Name = "";
    public int UsedCount = 0;
    public String Label = null;
    public Drawable Icon = null;

    public String getSortField() {
        if (Label != null){
            return Label;
        }else{
            return Name;
        }
    }

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

    @Override
    public int compareTo(DataCategory o) {
        return getSortField().compareToIgnoreCase(o.getSortField());
    }
}
