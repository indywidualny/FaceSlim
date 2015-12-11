package org.indywidualni.fblite.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.util.Log;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.service.NotificationsService;

public class NotificationsSettingsFragment extends PreferenceFragment {

    private static Context context;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;
    private TrayAppPreferences trayPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.notifications_preferences);

        context = MyApplication.getContextOfApplication();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        trayPreferences = new TrayAppPreferences(context);

        // default value for interval_pref preference summary
        ListPreference lp = (ListPreference) findPreference("interval_pref");
        String temp1 = getString(R.string.interval_pref_description).replace("%s", "");
        String temp2 = lp.getSummary().toString();
        if (temp1.equals(temp2))
            lp.setValueIndex(2);

        // listener for changing preferences (works after the value change)
        prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // service intent (start, stop)
                final Intent intent = new Intent(context, NotificationsService.class);

                switch (key) {
                    case "interval_pref":
                        // update Tray Preference before restarting the service
                        trayPreferences.put("interval_pref", Integer.parseInt(preferences.getString("interval_pref", "1800000")));
                        // restart the service after time interval change
                        if (prefs.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                    case "ringtone":
                        trayPreferences.put("ringtone", preferences.getString("ringtone", "content://settings/system/notification_sound"));
                        break;
                    case "vibrate":
                        trayPreferences.put("vibrate", preferences.getBoolean("vibrate", false));
                        break;
                    case "led_light":
                        trayPreferences.put("led_light", preferences.getBoolean("led_light", false));
                        break;
                    case "notifications_everywhere":
                        trayPreferences.put("notifications_everywhere", preferences.getBoolean("notifications_everywhere", true));
                        break;
                }

                // what's going on, dude?
                Log.v("SharedPreferenceChange", key + " changed in NotificationsSettingsFragment");
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        // register the listener
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister the listener
        preferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        // update ringtone preference summary
        String ringtoneString = preferences.getString("ringtone", "content://settings/system/notification_sound");
        Uri ringtoneUri = Uri.parse(ringtoneString);
        String name;

        try {
            Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            name = ringtone.getTitle(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            name = "Default";
        }

        if ("".equals(ringtoneString))
            name = getString(R.string.silent);

        RingtonePreference rp = (RingtonePreference) findPreference("ringtone");
        rp.setSummary(getString(R.string.notification_sound_description) + name);
    }

}