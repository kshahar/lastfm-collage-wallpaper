package org.kwimbo.lastfm;

import java.util.Date;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

public class UpdateScheduler {
    static final String nextUpdatePreference = "next_update";
    Runnable runnable;
    SharedPreferences preferences;
    Handler handler = new Handler();

    public UpdateScheduler(Runnable runnable, SharedPreferences preferences) {
        this.runnable = runnable;
        this.preferences = preferences;
    }

    public void scheduleFromPreferences() {
        long time = preferences.getLong(nextUpdatePreference, 0);
        if (time > 0) {
            Date date = new Date(time);
            reschedule(date);
        }
    }

    public void rescheduleIn(long milliseconds) {
        Date now = new Date();
        Date date = new Date(now.getTime() + milliseconds);
        reschedule(date);
    }

    public void reschedule(Date date) {
        handler.removeCallbacks(runnable);

        Date now = new Date();
        long timeMilliseconds = 0;
        if (now.before(date)) {
            timeMilliseconds = date.getTime() - now.getTime();
        }

        if (D.DBG) Log.i(D.TAG, String.format("Next update in %d milliseconds", timeMilliseconds));
        handler.postDelayed(runnable, timeMilliseconds);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(nextUpdatePreference, date.getTime());
        editor.commit();
    }
}
