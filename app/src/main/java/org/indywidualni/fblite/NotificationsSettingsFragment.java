package org.indywidualni.fblite;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.util.Log;

public class NotificationsSettingsFragment extends PreferenceFragment {

    private static Context context;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefListner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notifications_preferences);

        context = MyApplication.getContextOfApplication();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // listener for changing preferences (works after the value change)
        myPrefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

                switch (key) {
                    case "interval_pref":
                        Log.v("SettingsFragment", "interval_pref changed");
                        // restart service after time interval change
                        if (NotificationsService.isRunning) {
                            Intent intent = new Intent(context, NotificationsService.class);
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                }

            }
        };

        // register the listener above
        preferences.registerOnSharedPreferenceChangeListener(myPrefListner);
    }

    @Override
    public void onResume () {
        super.onResume();

        // update ringtone preference summary
        String ringtoneString = preferences.getString("ringtone", "content://settings/system/notification_sound");
        Uri ringtoneUri = Uri.parse(ringtoneString);
        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
        String name = ringtone.getTitle(context);
        if ("".equals(ringtoneString))
            name = getString(R.string.silent);
        RingtonePreference rp = (RingtonePreference) findPreference("ringtone");
        rp.setSummary(getString(R.string.notification_sound_description) + name);
    }

}