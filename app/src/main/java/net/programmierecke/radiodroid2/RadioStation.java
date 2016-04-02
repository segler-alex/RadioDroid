package net.programmierecke.radiodroid2;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

public class RadioStation {
	public RadioStation() {
	}

	public String ID;
	public String Name;
	public String StreamUrl;
	public String HomePageUrl;
	public String IconUrl;
	public String Country;
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
}
