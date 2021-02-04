package net.programmierecke.radiodroid2.players;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Locale;

public class TextToSpeechPlayer {
    private static final String TAG = "TextToSpeechPlayer";
    private static Locale FALLBACK_LOCALE = Locale.US;
    private TextToSpeech tts = null;
    private Locale localeForTtsMessages;
    private boolean ttsIsSupported = true;
    private Context ctx;

    public TextToSpeechPlayer(@NonNull Context context, int testString_res_id) {
        ctx = context;
        Locale currentLocale = context.getResources().getConfiguration().locale;
        localeForTtsMessages = resStringIsLocalized(context, currentLocale, testString_res_id) ?
                currentLocale : FALLBACK_LOCALE;

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(localeForTtsMessages);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    if (localeForTtsMessages.equals(FALLBACK_LOCALE)) {
                        ttsIsSupported = false;
                        Log.e(TAG, "Fallback locale does not support tts.");
                    } else {
                        localeForTtsMessages = FALLBACK_LOCALE;
                        result = tts.setLanguage(localeForTtsMessages);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            ttsIsSupported = false;
                            Log.e(TAG, "Neither current locale nor fallback locale support tts.");
                        }
                    }
                }
            } else {
                Log.e(TAG, "Tts is not supported on this device.");
            }
        });
    }

    public void speak(int stringResId) {
        if (ttsIsSupported)
            speak(getResStringByLocale(ctx, stringResId, localeForTtsMessages));
    }

    public void speak(String string) {
        if (ttsIsSupported)
            tts.speak(string, TextToSpeech.QUEUE_ADD, null);
    }

    public void stop() {
        if (ttsIsSupported)
            tts.stop();
    }

    public static boolean resStringIsLocalized(Context context, Locale locale, int stringResId) {
        String stringWithCurrentLocale = getResStringByLocale(context, stringResId, locale);
        String stringWithDefaultLocale = getResStringByLocale(context, stringResId, FALLBACK_LOCALE);
        return locale.equals(FALLBACK_LOCALE) ||
               !stringWithCurrentLocale.equals(stringWithDefaultLocale);
    }

    // See https://stackoverflow.com/questions/17771531/android-how-to-get-string-in-specific-locale-without-changing-the-current-local
    @NonNull
    public static String getResStringByLocale(Context context, int resId, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            return getResStringByLocalePlus17(context, resId, locale);
        else
            return getResStringByLocaleBefore17(context, resId, locale);
    }

    @NonNull
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static String getResStringByLocalePlus17(Context context, int resId, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration).getResources().getString(resId);
    }

    private static String getResStringByLocaleBefore17(Context context, int resId, Locale locale) {
        Resources currentResources = context.getResources();
        AssetManager assets = currentResources.getAssets();
        DisplayMetrics metrics = currentResources.getDisplayMetrics();
        Configuration config = new Configuration(currentResources.getConfiguration());
        Locale.setDefault(locale);
        config.locale = locale;
        /*
         * Note: This (temporarily) changes the devices locale! TODO find a
         * better way to get the string in the specific locale
         */
        Resources defaultLocaleResources = new Resources(assets, metrics, config);
        String string = defaultLocaleResources.getString(resId);
        // Restore device-specific locale
        new Resources(assets, metrics, currentResources.getConfiguration());
        return string;
    }
}
