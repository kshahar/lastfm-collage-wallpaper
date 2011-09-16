package org.kwimbo.lastfm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class CollageWallpaperSettings extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences sharedPreferences;
    boolean regenerated = false;
    boolean isFirstTime = false;
    boolean isDirty = false;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(CollageWallpaper.SHARED_PREFS_NAME);
        addPreferencesFromResource(com.kwimbo.lastfm.R.xml.preferences);

        sharedPreferences = getSharedPreferences(CollageWallpaper.SHARED_PREFS_NAME, MODE_PRIVATE);

        Preference regenerateNow = getPreferenceManager().findPreference("regenerate_now");
        if (regenerateNow == null) return;
        regenerateNow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                regenerateNow();
                return true;
            }
        });
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        isDirty = true;
        if (D.DBG) Log.d(D.TAG, "Preferences changed");
    }

    @Override
    protected void onStart() {
        super.onStart();

        regenerated = false;

        isFirstTime = sharedPreferences.getBoolean("is_first_time", true);
        if (isFirstTime) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("is_first_time", false);
            editor.commit();
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        if (D.DBG) Log.d(D.TAG, String.format("onStop %b %b %b", regenerated, isFirstTime, isDirty));

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        boolean shouldRegenerate =
                isFirstTime ||
                (!regenerated && isDirty);

        if (shouldRegenerate) {
            regenerateNow();
        }

        super.onStop();
    }

    private void regenerateNow() {
        regenerated = true;
        Intent intent = new Intent(Intent.ACTION_RUN);
        sendBroadcast(intent);
        if (D.DBG) Log.i(D.TAG, "Regenerate now");
        Toast.makeText(this, "Creating image...", Toast.LENGTH_SHORT).show();
    }
}