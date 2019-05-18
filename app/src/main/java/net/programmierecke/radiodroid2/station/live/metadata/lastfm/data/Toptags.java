
package net.programmierecke.radiodroid2.station.live.metadata.lastfm.data;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Toptags {

    @SerializedName("tag")
    @Expose
    private List<Tag> tag = null;

    public List<Tag> getTag() {
        return tag;
    }

    public void setTag(List<Tag> tag) {
        this.tag = tag;
    }

}
