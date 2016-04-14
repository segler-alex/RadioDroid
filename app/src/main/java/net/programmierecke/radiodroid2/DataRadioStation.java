package net.programmierecke.radiodroid2;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataRadioStation {
	public DataRadioStation() {
	}

	public String ID;
	public String Name;
	//public String StreamUrl;
	public String HomePageUrl;
	public String IconUrl;
	public String Country;
	public String State;
	public String TagsAll;
	public String Language;
	public int ClickCount;
	public int Votes;

	public String getShortDetails() {
		List<String> aList = new ArrayList<String>();
		if (Country != null) {
			if (!Country.trim().equals(""))
				aList.add(Country);
		}
		if (State != null) {
			if (!State.trim().equals(""))
				aList.add(State);
		}
		if (Language != null) {
			if (!Language.trim().equals(""))
				aList.add(Language);
		}
		if (TagsAll != null) {
			for (String aPart : TagsAll.split(",")) {
				if (!aPart.trim().equals(""))
					aList.add(aPart);
			}
		}
		return TextUtils.join(", ", aList);
	}

	public static DataRadioStation[] DecodeJson(String result) {
		List<DataRadioStation> aList = new ArrayList<DataRadioStation>();
		if (result != null) {
			if (TextUtils.isGraphic(result)) {
				try {
					JSONArray jsonArray = new JSONArray(result);
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject anObject = jsonArray.getJSONObject(i);

						DataRadioStation aStation = new DataRadioStation();
						aStation.ID = anObject.getString("id");
						aStation.Name = anObject.getString("name");
						//aStation.StreamUrl = anObject.getString("url");
						aStation.Votes = anObject.getInt("votes");
						aStation.HomePageUrl = anObject.getString("homepage");
						aStation.TagsAll = anObject.getString("tags");
						aStation.Country = anObject.getString("country");
						aStation.State = anObject.getString("state");
						aStation.IconUrl = anObject.getString("favicon");
						aStation.Language = anObject.getString("language");

						aList.add(aStation);
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return aList.toArray(new DataRadioStation[0]);
	}
}
