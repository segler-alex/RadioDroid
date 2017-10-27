package net.programmierecke.radiodroid2;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CountryFlagsLoader {
    private static final CountryFlagsLoader ourInstance = new CountryFlagsLoader();

    public static CountryFlagsLoader getInstance() {
        return ourInstance;
    }

    private CountryFlagsLoader() {
    }

    private class Country {
        private String name;
        private String code;

        public String getName() {
            return name;
        }

        public String getCode() {
            return code;
        }
    }

    private Map<String, String> countryToCode = new HashMap<>();

    public void load(Context context) {
        Resources resources = context.getResources();
        final InputStream inputStream = resources.openRawResource(R.raw.countries);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Gson gson = new Gson();
        Type collectionType = new TypeToken<Collection<Country>>() {
        }.getType();
        Collection<Country> countries = gson.fromJson(reader, collectionType);

        for (Country country : countries) {
            countryToCode.put(country.getName().toLowerCase(Locale.ENGLISH), country.getCode());
        }
    }

    public Drawable getFlag(Context context, String countryName) {
        String countryCode = countryToCode.get(countryName.toLowerCase(Locale.ENGLISH));

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
