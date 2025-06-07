package com.example.inforingkas.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemePreferenceHelper {
    private final SharedPreferences sharedPreferences;

    public ThemePreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setThemeMode(String themeMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.KEY_THEME_MODE, themeMode);
        editor.apply();
        applyTheme(themeMode);
    }

    public String getThemeMode() {
        // Default to system theme or light theme if not set
        return sharedPreferences.getString(Constants.KEY_THEME_MODE, Constants.THEME_SYSTEM);
    }

    public static void applyTheme(String themeMode) {
        switch (themeMode) {
            case Constants.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Constants.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case Constants.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // Helper for last fetch date
    public void setLastFetchDateTerkini(String date) { // date in YYYY-MM-DD format
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.KEY_LAST_FETCH_DATE_TERKINI, date);
        editor.apply();
    }

    public String getLastFetchDateTerkini() {
        return sharedPreferences.getString(Constants.KEY_LAST_FETCH_DATE_TERKINI, "");
    }
}