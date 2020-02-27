package net.programmierecke.radiodroid2.station;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
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
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.service.MediaSessionCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import jp.wasabeef.picasso.transformations.CropSquareTransformation;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;
import okhttp3.OkHttpClient;

import static net.programmierecke.radiodroid2.Utils.resourceToUri;

public class DataRadioStation implements Parcelable {
	static final String TAG = "DATAStation";
	public static final int MAX_REFRESH_RETRIES = 16;

	public static final String RADIO_STATION_LOCAL_INFO_CHAGED = "net.programmierecke.radiodroid2.radiostation.changed";
	public static final String RADIO_STATION_UUID = "UUID";

    public DataRadioStation() {
	}

	public String Name;
	public String StationUuid="";
	public String ChangeUuid="";
	public String StreamUrl;
	public String HomePageUrl;
	public String IconUrl;
	public String Country;
	public String CountryCode;
	public String State;
	public String TagsAll;
	public String Language;
	public int ClickCount;
	public int ClickTrend;
	public int Votes;
	public int RefreshRetryCount;
	public int Bitrate;
	public String Codec;
	public boolean Working = true;
	public boolean Hls = false;

	public String playableUrl;

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

	public String getLongDetails(Context ctx) {
		List<String> aList = new ArrayList<>();
		if (!Working){
			aList.add(ctx.getResources().getString(R.string.station_detail_broken));
		}
		if (Bitrate > 0){
			aList.add(ctx.getResources().getString(R.string.station_detail_bitrate, Bitrate));
		}
		if(!TextUtils.isEmpty(Codec)) {
			aList.add(Codec);
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

	public boolean hasIcon() {
		return !TextUtils.isEmpty(IconUrl);
	}

	private void fixStationFields() {
		if (IconUrl == null || TextUtils.isEmpty(IconUrl.trim())) {
			IconUrl = "";
		}
	}

	public static List<DataRadioStation> DecodeJson(String result) {
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
							if (anObject.has("refreshretrycount")) {
								aStation.RefreshRetryCount = anObject.getInt("refreshretrycount");
							} else {
								aStation.RefreshRetryCount = 0;
							}
							aStation.HomePageUrl = anObject.getString("homepage");
							aStation.TagsAll = anObject.getString("tags");
							aStation.Country = anObject.getString("country");
							if (anObject.has("countrycode")) {
								aStation.CountryCode = anObject.getString("countrycode");
							}
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

							aStation.fixStationFields();

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
		return aList;
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
					if (anObject.has("refreshretrycount")) {
						aStation.RefreshRetryCount = anObject.getInt("refreshretrycount");
					} else {
						aStation.RefreshRetryCount = 0;
					}
					aStation.HomePageUrl = anObject.getString("homepage");
					aStation.TagsAll = anObject.getString("tags");
					aStation.Country = anObject.getString("country");
					if (anObject.has("countrycode")) {
						aStation.CountryCode = anObject.getString("countrycode");
					}
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

					aStation.fixStationFields();

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
			if (TextUtils.isEmpty(StationUuid)) {
				obj.put("id", StationId);
			} else {
				obj.put("stationuuid", StationUuid);
			}
			obj.put("changeuuid",ChangeUuid);
			obj.put("name",Name);
			obj.put("homepage",HomePageUrl);
			obj.put("url",StreamUrl);
			obj.put("favicon",IconUrl);
			obj.put("country",Country);
			obj.put("countrycode",CountryCode);
			obj.put("state",State);
			obj.put("tags",TagsAll);
			obj.put("language",Language);
			obj.put("clickcount",ClickCount);
			obj.put("clicktrend",ClickTrend);
			if (RefreshRetryCount > 0) {
				obj.put("refreshretrycount", RefreshRetryCount);
			}
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
			RefreshRetryCount = 0;
			success = true;
		} else if (Utils.hasAnyConnection(context)) {
			RefreshRetryCount++;
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
		CountryCode = station.CountryCode;
		State = station.State;
		TagsAll = station.TagsAll;
		Language = station.Language;
		ClickCount = station.ClickCount;
		ClickTrend = station.ClickTrend;
		Votes = station.Votes;
		RefreshRetryCount = station.RefreshRetryCount;
		Bitrate = station.Bitrate;
		Codec = station.Codec;
		Working = station.Working;
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.Name);
		dest.writeString(this.StationUuid);
		dest.writeString(this.ChangeUuid);
		dest.writeString(this.StreamUrl);
		dest.writeString(this.HomePageUrl);
		dest.writeString(this.IconUrl);
		dest.writeString(this.Country);
		dest.writeString(this.CountryCode);
		dest.writeString(this.State);
		dest.writeString(this.TagsAll);
		dest.writeString(this.Language);
		dest.writeInt(this.ClickCount);
		dest.writeInt(this.ClickTrend);
		dest.writeInt(this.Votes);
		dest.writeInt(this.Bitrate);
		dest.writeString(this.Codec);
		dest.writeByte(this.Working ? (byte) 1 : (byte) 0);
		dest.writeByte(this.Hls ? (byte) 1 : (byte) 0);
		dest.writeString(this.playableUrl);
		dest.writeString(this.StationId);
	}

	protected DataRadioStation(Parcel in) {
		this.Name = in.readString();
		this.StationUuid = in.readString();
		this.ChangeUuid = in.readString();
		this.StreamUrl = in.readString();
		this.HomePageUrl = in.readString();
		this.IconUrl = in.readString();
		this.Country = in.readString();
		this.CountryCode = in.readString();
		this.State = in.readString();
		this.TagsAll = in.readString();
		this.Language = in.readString();
		this.ClickCount = in.readInt();
		this.ClickTrend = in.readInt();
		this.Votes = in.readInt();
		this.Bitrate = in.readInt();
		this.Codec = in.readString();
		this.Working = in.readByte() != 0;
		this.Hls = in.readByte() != 0;
		this.playableUrl = in.readString();
		this.StationId = in.readString();
	}

	public static final Parcelable.Creator<DataRadioStation> CREATOR = new Parcelable.Creator<DataRadioStation>() {
		@Override
		public DataRadioStation createFromParcel(Parcel source) {
			return new DataRadioStation(source);
		}

		@Override
		public DataRadioStation[] newArray(int size) {
			return new DataRadioStation[size];
		}
	};

	public interface ShortcutReadyListener {
		void onShortcutReadyListener(ShortcutInfo shortcutInfo);
	}

    public void prepareShortcut(Context ctx, ShortcutReadyListener cb) {
        Picasso.get()
                .load((!hasIcon() ? resourceToUri(ctx.getResources(), R.drawable.ic_launcher).toString() : IconUrl))
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
