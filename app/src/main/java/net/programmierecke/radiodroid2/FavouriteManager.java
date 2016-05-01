package net.programmierecke.radiodroid2;

import android.content.Context;

public class FavouriteManager extends StationSaveManager{
    @Override
    protected String getSaveId(){
        return "favourites";
    }

    public FavouriteManager(Context ctx) {
        super(ctx);
    }
}
