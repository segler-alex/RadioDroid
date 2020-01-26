package net.programmierecke.radiodroid2.interfaces;

import net.programmierecke.radiodroid2.station.StationsFilter;

public interface IFragmentSearchable {
    void Search(StationsFilter.SearchStyle searchStyle, String query);
}
