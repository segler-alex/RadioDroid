package net.programmierecke.radiodroid2.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import net.programmierecke.radiodroid2.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataRadioStation {
	static final String TAG = "DATAStation";

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
	public int Bitrate;
	public String Codec;
	public boolean Working = true;

	public String getShortDetails(Context ctx) {
		List<String> aList = new ArrayList<String>();
		if (!Working){
			aList.add(ctx.getResources().getString(R.string.station_detail_broken));
		}
		if (Codec != null){
			if (!Codec.trim().equals("")){
				aList.add(Codec);
			}
		}
		if (Bitrate > 0){
			aList.add(ctx.getResources().getString(R.string.station_detail_bitrate, Bitrate));
		}
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
						try {
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
							aStation.ClickCount = anObject.getInt("clickcount");
							if (anObject.has("bitrate")) {
								aStation.Bitrate = anObject.getInt("bitrate");
							}
							if (anObject.has("codec")) {
								aStation.Codec = anObject.getString("codec");
							}
							if (anObject.has("lastcheckok")){
								aStation.Working = anObject.getInt("lastcheckok") != 0;
							}

							aList.add(aStation);
						}catch(Exception e){
							Log.e(TAG, "DecodeJson() #2 "+e);
						}
					}

				} catch (JSONException e) {
					Log.e(TAG, "DecodeJson() #1 "+e);
				}
			}
		}
		return aList.toArray(new DataRadioStation[0]);
	}

	public static DataRadioStation DecodeJsonSingle(String result) {
		if (result != null) {
			if (TextUtils.isGraphic(result)) {
				try {
					JSONObject anObject = new JSONObject(result);

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
					aStation.ClickCount = anObject.getInt("clickcount");
					if (anObject.has(("bitrate"))) {
						aStation.Bitrate = anObject.getInt("bitrate");
					}
					if (anObject.has("codec")) {
						aStation.Codec = anObject.getString("codec");
					}
					if (anObject.has("lastcheckok")){
						aStation.Working = anObject.getInt("lastcheckok") != 0;
					}

					return aStation;
				} catch (JSONException e) {
					Log.e(TAG, "DecodeJsonSingle() "+e);
				}
			}
		}
		return null;
	}

	public JSONObject toJson(){
		JSONObject obj = new JSONObject();
		try {
			obj.put("id",ID);
			obj.put("name",Name);
			obj.put("homepage",HomePageUrl);
			obj.put("favicon",IconUrl);
			obj.put("country",Country);
			obj.put("state",State);
			obj.put("tags",TagsAll);
			obj.put("language",Language);
			obj.put("clickcount",ClickCount);
			obj.put("votes",Votes);
			obj.put("bitrate",""+Bitrate);
			obj.put("codec",Codec);
			obj.put("lastcheckok",Working ? "1" : "0");
			return obj;
		} catch (JSONException e) {
			Log.e(TAG, "toJson() "+e);
		}

		return null;
	}
}
