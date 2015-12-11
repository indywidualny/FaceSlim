package org.indywidualni.fblite.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import net.grandcentrix.tray.TrayAppPreferences;

import org.indywidualni.fblite.MyApplication;
import org.indywidualni.fblite.R;
import org.indywidualni.fblite.activity.MainActivity;
import org.indywidualni.fblite.service.NotificationsService;
import org.indywidualni.fblite.util.FileOperation;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    private static Context context;
    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener;
    private TrayAppPreferences trayPreferences;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // get context
        context = MyApplication.getContextOfApplication();

        // get Tray Preferences and Shared Preferences
        trayPreferences = new TrayAppPreferences(context);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // set onPreferenceClickListener for a few preferences
        Preference getFeedPref = findPreference("get_feed");
        Preference notificationsSettingsPref = findPreference("notifications_settings");
        Preference clearCachePref = findPreference("clear_cache");
        getFeedPref.setOnPreferenceClickListener(this);
        notificationsSettingsPref.setOnPreferenceClickListener(this);
        clearCachePref.setOnPreferenceClickListener(this);


        // listener for changing preferences (works after the value change)
        prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // service intent (start, stop)
                final Intent intent = new Intent(context, NotificationsService.class);

                switch (key) {
                    case "notifications_activated":
                        if (prefs.getBoolean("notifications_activated", false))
                            context.startService(intent);
                        else
                            context.stopService(intent);
                        break;
                    case "feed_url":
                        trayPreferences.put("feed_url", preferences.getString("feed_url", ""));
                        // remove saved date for fresh check
                        trayPreferences.put("saved_date", "");
                        // restart service
                        if (prefs.getBoolean("notifications_activated", false)) {
                            context.stopService(intent);
                            context.startService(intent);
                        }
                        break;
                    case "transparent_nav":
                    case "drawer_pos":
                    case "no_images":
                    case "hardware_acceleration":
                        relaunch();
                        break;
                }

                // what's going on, dude?
                Log.v("SharedPreferenceChange", key + " changed in SettingsFragment");
            }
        };
    }

    // a few preferences are just buttons to click on, let's make them work
    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        Log.v("OnPreferenceClick", key + " clicked in SettingsFragment");

        switch (key) {
            case "get_feed":
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.get_feed_link))));
                return true;
            case "notifications_settings":
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, 0)
                        .addToBackStack(null).replace(R.id.content_frame,
                        new NotificationsSettingsFragment()).commit();
                return true;
            case "clear_cache":
                // clear cache dirs
                FileOperation.deleteCache(getActivity().getApplicationContext());
                // restart the app (really ugly way of doing it but...)
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // register the listener
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        // unregister the listener
        preferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);
    }

    // relaunch the app
    private void relaunch() {
        // notify user about relaunching the app
        Toast.makeText(getActivity(), getString(R.string.applying_changes), Toast.LENGTH_SHORT).show();
        // sending intent to onNewIntent() of MainActivity
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("core_settings_changed", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}