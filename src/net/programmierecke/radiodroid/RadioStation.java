package net.programmierecke.radiodroid;

public class RadioStation {
	public RadioStation() {
	}

	public String ID;
	public String Name;
	public String StreamUrl;
	public String HomePageUrl;
	public String Country;
	public String TagsAll;
	public String Language;
	public int ClickCount;
	public int Votes;

	@Override
	public String toString() {
		return this.Name + " - (" + Votes + ")";
	}
}
