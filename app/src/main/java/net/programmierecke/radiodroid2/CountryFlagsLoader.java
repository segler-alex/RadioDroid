package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.Locale;

public class CountryFlagsLoader {
    private static final CountryFlagsLoader ourInstance = new CountryFlagsLoader();

    public static CountryFlagsLoader getInstance() {
        return ourInstance;
    }

    private CountryFlagsLoader() {
    }

    public Drawable getFlag(Context context, String countryCode) {
        if (countryCode != null) {
            Resources resources = context.getResources();
            final String resourceName = "flag_" + countryCode.toLowerCase();
            final int resourceId = resources.getIdentifier(resourceName, "drawable",
                    context.getPackageName());
            if (resourceId != 0) {
                return resources.getDrawable(resourceId);
            }
        }
        return null;
    }
}
