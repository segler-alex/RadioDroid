package net.programmierecke.radiodroid2.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.MediaSessionCallback;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import jp.wasabeef.picasso.transformations.CropSquareTransformation;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;
import okhttp3.OkHttpClient;

import static net.programmierecke.radiodroid2.Utils.resourceToUri;

public class DataRadioStation {
	static final String TAG = "DATAStation";

	public DataRadioStation() {
	}

	public String Name;
	public String StationUuid="";
	public String ChangeUuid="";
	public String StreamUrl;
	public String HomePageUrl;
	public String IconUrl;
	public String Country;
	public String State;
	public String TagsAll;
	public String Language;
	public int ClickCount;
	public int ClickTrend;
	public int Votes;
	public int Bitrate;
	public String Codec;
	public boolean Working = true;
	public boolean Hls = false;

	@Deprecated
	public String StationId = "";

	public String getShortDetails(Context ctx) {
		List<String> aList = new ArrayList<String>();
		if (!Working){
			aList.add(ctx.getResources().getString(R.string.station_detail_broken));
		}
		if (Bitrate > 0){
			aList.add(ctx.getResources().getString(R.string.station_detail_bitrate, Bitrate));
		}
		if (State != null) {
			if (!State.trim().equals(""))
				aList.add(State);
		}
		if (Language != null) {
			if (!Language.trim().equals(""))
				aList.add(Language);
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
							aStation.Name = anObject.getString("name");
							aStation.StreamUrl = "";
							if (anObject.has("url")) {
								aStation.StreamUrl = anObject.getString("url");
							}
							if (anObject.has("stationuuid")) {
								aStation.StationUuid = anObject.getString("stationuuid");
							}
							if (!aStation.hasValidUuid()) {
								aStation.StationId = anObject.getString("id");
							}
							if (anObject.has("changeuuid")) {
								aStation.ChangeUuid = anObject.getString("changeuuid");
							}
							aStation.Votes = anObject.getInt("votes");
							aStation.HomePageUrl = anObject.getString("homepage");
							aStation.TagsAll = anObject.getString("tags");
							aStation.Country = anObject.getString("country");
							aStation.State = anObject.getString("state");
							aStation.IconUrl = anObject.getString("favicon");
							aStation.Language = anObject.getString("language");
							aStation.ClickCount = anObject.getInt("clickcount");
							if (anObject.has("clicktrend")) {
								aStation.ClickTrend = anObject.getInt("clicktrend");
							}
							if (anObject.has("bitrate")) {
								aStation.Bitrate = anObject.getInt("bitrate");
							}
							if (anObject.has("codec")) {
								aStation.Codec = anObject.getString("codec");
							}
							if (anObject.has("lastcheckok")){
								aStation.Working = anObject.getInt("lastcheckok") != 0;
							}
							if (anObject.has("hls")){
								aStation.Hls = anObject.getInt("hls") != 0;
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
					aStation.Name = anObject.getString("name");
					aStation.StreamUrl = "";
					if (anObject.has("url")) {
						aStation.StreamUrl = anObject.getString("url");
					}
					if (anObject.has("stationuuid")) {
						aStation.StationUuid = anObject.getString("stationuuid");
					}
					if (!aStation.hasValidUuid()) {
						aStation.StationId = anObject.getString("id");
					}
					if (anObject.has("changeuuid")) {
						aStation.ChangeUuid = anObject.getString("changeuuid");
					}
					aStation.Votes = anObject.getInt("votes");
					aStation.HomePageUrl = anObject.getString("homepage");
					aStation.TagsAll = anObject.getString("tags");
					aStation.Country = anObject.getString("country");
					aStation.State = anObject.getString("state");
					aStation.IconUrl = anObject.getString("favicon");
					aStation.Language = anObject.getString("language");
					aStation.ClickCount = anObject.getInt("clickcount");
					if (anObject.has("clicktrend")) {
						aStation.ClickTrend = anObject.getInt("clicktrend");
					}
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
			if (TextUtils.isEmpty(StationUuid))
				obj.put("id",StationId);
			else
				obj.put("stationuuid",StationUuid);
			obj.put("changeuuid",ChangeUuid);
			obj.put("name",Name);
			obj.put("homepage",HomePageUrl);
			obj.put("url",StreamUrl);
			obj.put("favicon",IconUrl);
			obj.put("country",Country);
			obj.put("state",State);
			obj.put("tags",TagsAll);
			obj.put("language",Language);
			obj.put("clickcount",ClickCount);
			obj.put("clicktrend",ClickTrend);
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

	public boolean refresh(final OkHttpClient httpClient, final Context context) {
		boolean success = false;
		DataRadioStation refreshedStation = (!TextUtils.isEmpty(StationUuid) ? Utils.getStationByUuid(httpClient, context, StationUuid) : Utils.getStationById(httpClient, context, StationId));

		if (refreshedStation != null && refreshedStation.hasValidUuid()) {
			copyPropertiesFrom(refreshedStation);
			success = true;
		}
		return success;
	}

	public boolean hasValidUuid() {
		return !TextUtils.isEmpty(StationUuid);
	}

	public void copyPropertiesFrom(DataRadioStation station) {
		StationUuid = station.StationUuid;
		StationId = station.StationId;
		ChangeUuid = station.ChangeUuid;
		Name = station.Name;
		HomePageUrl = station.HomePageUrl;
		StreamUrl = station.StreamUrl;
		IconUrl = station.IconUrl;
		Country = station.Country;
		State = station.State;
		TagsAll = station.TagsAll;
		Language = station.Language;
		ClickCount = station.ClickCount;
		ClickTrend = station.ClickTrend;
		Votes = station.Votes;
		Bitrate = station.Bitrate;
		Codec = station.Codec;
		Working = station.Working;
	}

	public interface ShortcutReadyListener {
		void onShortcutReadyListener(ShortcutInfo shortcutInfo);
	}

    public void prepareShortcut(Context ctx, ShortcutReadyListener cb) {
        Picasso.get()
                .load((TextUtils.isEmpty(IconUrl) ? resourceToUri(ctx.getResources(), R.drawable.ic_launcher).toString() : IconUrl))
                .error(R.drawable.ic_launcher)
                .transform(Utils.useCircularIcons(ctx) ? new CropCircleTransformation() : new CropSquareTransformation())
                .transform(new RoundedCornersTransformation(12, 2, RoundedCornersTransformation.CornerType.ALL))
                .into(new RadioIconTarget(ctx, this, cb));
    }

    class RadioIconTarget implements Target {
        DataRadioStation station;
        Context ctx;
        ShortcutReadyListener cb;

        RadioIconTarget(Context ctx, DataRadioStation station, ShortcutReadyListener cb) {
            super();
            this.ctx = ctx;
            this.station = station;
            this.cb = cb;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (Build.VERSION.SDK_INT >= 25) {
                Intent playByUUIDintent = new Intent(MediaSessionCallback.ACTION_PLAY_STATION_BY_UUID, null, ctx, ActivityMain.class)
                        .putExtra(MediaSessionCallback.EXTRA_STATION_UUID, station.StationUuid);
                ShortcutInfo shortcut = new ShortcutInfo.Builder(ctx.getApplicationContext(), ctx.getPackageName() + "/" + station.StationUuid)
                        .setShortLabel(station.Name)
                        .setIcon(Icon.createWithBitmap(bitmap))
                        .setIntent(playByUUIDintent)
                        .build();
                cb.onShortcutReadyListener(shortcut);
            }
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            onBitmapLoaded(((BitmapDrawable) errorDrawable).getBitmap(), null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    }
}
