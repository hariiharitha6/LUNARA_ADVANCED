package com.example.lunara;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

/**
 * Helper to apply locale without full app restart.
 * Usage: call LocaleHelper.setLocale(context, "ml") then recreate() the activity.
 */
public class LocaleHelper {

    private static final String PREF_NAME = "app_settings";
    private static final String KEY_LANG  = "language";

    public static Context setLocale(Context context, String languageCode) {
        persist(context, languageCode);
        return updateResources(context, languageCode);
    }

    public static Context onAttach(Context context) {
        String lang = getPersistedLanguage(context);
        return updateResources(context, lang);
    }

    public static String getPersistedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "en");
    }

    private static void persist(Context context, String language) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANG, language).apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}
